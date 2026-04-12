package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.config.SteamTradeBotProperties;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.User;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.util.SteamApiClient;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteamTradeMonitorService {

    private final RestTemplate restTemplate;
    private final SteamApiClient steamApiClient;
    private final UserInventoryMapper userInventoryMapper;
    private final UserMapper userMapper;
    private final SteamTradeBotProperties botProperties;

    public boolean isBotMonitoringEnabled() {
        return botProperties.isEnabled();
    }

    public SellerOfferDetectionResult detectSellerOffer(TradeOrder order) {
        if (order == null) {
            return SellerOfferDetectionResult.notFound("交易订单不存在");
        }

        User seller = userMapper.selectById(order.getSellerId());
        if (seller == null) {
            return SellerOfferDetectionResult.notFound("卖家账号不存在");
        }

        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer == null) {
            return SellerOfferDetectionResult.notFound("买家账号不存在");
        }

        if (seller.getSteamApiKey() == null || seller.getSteamApiKey().isBlank()) {
            return SellerOfferDetectionResult.notFound("卖家尚未配置 Steam API Key");
        }

        if (buyer.getSteamId() == null || buyer.getSteamId().isBlank()) {
            return SellerOfferDetectionResult.notFound("买家尚未绑定 Steam 账号");
        }

        UserInventory sourceInventory = userInventoryMapper.selectById(order.getInventoryId());
        if (sourceInventory == null) {
            return SellerOfferDetectionResult.notFound("库存快照不存在");
        }

        List<TradeOfferSummary> sentOffers = fetchSentTradeOffers(seller.getSteamApiKey());
        if (sentOffers.isEmpty()) {
            return SellerOfferDetectionResult.notFound("暂未检测到卖家发出的 Steam 报价");
        }

        String buyerAccountId = toAccountId32(buyer.getSteamId());
        TradeOfferSummary matchedOffer = sentOffers.stream()
                .filter(offer -> Objects.equals(offer.getAccountIdOther(), buyerAccountId))
                .filter(offer -> offerContainsInventoryItem(sourceInventory, offer))
                .max(Comparator.comparingLong(offer -> offer.getTimeUpdated() == null ? 0L : offer.getTimeUpdated()))
                .orElse(null);

        if (matchedOffer == null) {
            return SellerOfferDetectionResult.notFound("暂未检测到匹配这笔订单的 Steam 报价");
        }

        return SellerOfferDetectionResult.found(
                matchedOffer.getTradeOfferId(),
                matchedOffer.getTradeOfferUrl(),
                matchedOffer.getState(),
                matchedOffer.getStateText()
        );
    }

    public BotDeliveryCheckResult inspectSellerDelivery(TradeOrder order) {
        if (order == null || order.getTradeOfferId() == null || order.getTradeOfferId().isBlank()) {
            return BotDeliveryCheckResult.error("缺少 Steam 报价 ID");
        }

        User seller = userMapper.selectById(order.getSellerId());
        if (seller == null) {
            return BotDeliveryCheckResult.error("卖家账号不存在");
        }

        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer == null) {
            return BotDeliveryCheckResult.error("买家账号不存在");
        }

        if (seller.getSteamApiKey() == null || seller.getSteamApiKey().isBlank()) {
            return BotDeliveryCheckResult.error("卖家尚未配置 Steam API Key");
        }

        if (buyer.getSteamId() == null || buyer.getSteamId().isBlank()) {
            return BotDeliveryCheckResult.error("买家尚未绑定 Steam 账号");
        }

        TradeOfferSnapshot offerSnapshot = fetchTradeOffer(seller.getSteamApiKey(), order.getTradeOfferId());
        if (offerSnapshot == null) {
            return BotDeliveryCheckResult.error("查询 Steam 报价状态失败");
        }

        boolean inventoryMatched = false;
        String inventoryMessage = null;

        if (offerSnapshot.isAccepted()) {
            try {
                inventoryMatched = buyerInventoryHasMatchingItem(order, buyer.getSteamId());
                if (!inventoryMatched) {
                    inventoryMessage = "Steam 报价已接受，但买家库存中暂未检测到该物品";
                }
            } catch (Exception e) {
                inventoryMessage = "查询买家库存失败: " + e.getMessage();
                log.warn("查询买家库存失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }

        return BotDeliveryCheckResult.builder()
                .tradeOfferId(order.getTradeOfferId())
                .offerState(offerSnapshot.getState())
                .offerStateText(offerSnapshot.getStateText())
                .accepted(offerSnapshot.isAccepted())
                .terminalFailure(offerSnapshot.isTerminalFailure())
                .inventoryMatched(inventoryMatched)
                .errorMessage(firstNonBlank(inventoryMessage, offerSnapshot.getErrorMessage()))
                .build();
    }

    private TradeOfferSnapshot fetchTradeOffer(String apiKey, String tradeOfferId) {
        String url = "https://api.steampowered.com/IEconService/GetTradeOffer/v1/?key="
                + apiKey
                + "&tradeofferid="
                + tradeOfferId
                + "&language=schinese";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return TradeOfferSnapshot.error("查询 Steam 报价失败，HTTP " + response.getStatusCode().value());
            }

            JSONObject root = JSONObject.parseObject(response.getBody());
            JSONObject responseObj = root.getJSONObject("response");
            JSONObject offer = responseObj != null ? responseObj.getJSONObject("offer") : null;
            if (offer == null) {
                return TradeOfferSnapshot.error("Steam 报价返回为空");
            }

            Integer state = offer.getInteger("trade_offer_state");
            return TradeOfferSnapshot.builder()
                    .state(state)
                    .stateText(resolveTradeOfferStateText(state))
                    .accepted(Objects.equals(state, 3))
                    .terminalFailure(isTerminalFailureState(state))
                    .build();
        } catch (Exception e) {
            log.warn("查询 Steam 报价失败: tradeOfferId={}, error={}", tradeOfferId, e.getMessage());
            return TradeOfferSnapshot.error("查询 Steam 报价失败: " + e.getMessage());
        }
    }

    private List<TradeOfferSummary> fetchSentTradeOffers(String apiKey) {
        List<TradeOfferSummary> summaries = new ArrayList<>();
        String url = "https://api.steampowered.com/IEconService/GetTradeOffers/v1/?key="
                + apiKey
                + "&get_sent_offers=1&get_received_offers=0&active_only=1&historical_only=0&language=schinese";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("查询卖家发出报价失败: http={}", response.getStatusCode().value());
                return summaries;
            }

            JSONObject root = JSONObject.parseObject(response.getBody());
            JSONObject responseObj = root != null ? root.getJSONObject("response") : null;
            JSONArray sentOffers = responseObj != null ? responseObj.getJSONArray("trade_offers_sent") : null;
            if (sentOffers == null) {
                return summaries;
            }

            for (int i = 0; i < sentOffers.size(); i++) {
                JSONObject offer = sentOffers.getJSONObject(i);
                if (offer == null) {
                    continue;
                }

                JSONArray itemsToGiveJson = offer.getJSONArray("items_to_give");
                List<TradeOfferItemSnapshot> itemsToGive = new ArrayList<>();
                if (itemsToGiveJson != null) {
                    for (int j = 0; j < itemsToGiveJson.size(); j++) {
                        JSONObject item = itemsToGiveJson.getJSONObject(j);
                        if (item == null) {
                            continue;
                        }
                        itemsToGive.add(TradeOfferItemSnapshot.builder()
                                .assetId(item.getString("assetid"))
                                .classId(item.getString("classid"))
                                .instanceId(item.getString("instanceid"))
                                .build());
                    }
                }

                String tradeOfferId = offer.getString("tradeofferid");
                summaries.add(TradeOfferSummary.builder()
                        .tradeOfferId(tradeOfferId)
                        .tradeOfferUrl(tradeOfferId == null || tradeOfferId.isBlank()
                                ? null
                                : "https://steamcommunity.com/tradeoffer/" + tradeOfferId + "/")
                        .accountIdOther(offer.getString("accountid_other"))
                        .state(offer.getInteger("trade_offer_state"))
                        .stateText(resolveTradeOfferStateText(offer.getInteger("trade_offer_state")))
                        .timeUpdated(offer.getLong("time_updated"))
                        .itemsToGive(itemsToGive)
                        .build());
            }
        } catch (Exception e) {
            log.warn("查询卖家发出报价失败: {}", e.getMessage());
        }

        return summaries;
    }

    private boolean buyerInventoryHasMatchingItem(TradeOrder order, String buyerSteamId) {
        UserInventory sourceInventory = userInventoryMapper.selectById(order.getInventoryId());
        if (sourceInventory == null) {
            throw new IllegalStateException("库存快照不存在");
        }

        JSONObject inventoryJson = steamApiClient.getInventory(buyerSteamId);
        if (inventoryJson == null) {
            throw new IllegalStateException("买家库存返回为空");
        }

        List<InventoryFingerprint> buyerItems = parseInventoryFingerprints(inventoryJson);
        return buyerItems.stream().anyMatch(item -> matchesSourceInventory(sourceInventory, item));
    }

    private List<InventoryFingerprint> parseInventoryFingerprints(JSONObject inventoryJson) {
        List<InventoryFingerprint> fingerprints = new ArrayList<>();

        JSONArray assets = inventoryJson.getJSONArray("assets");
        JSONArray descriptions = inventoryJson.getJSONArray("descriptions");
        if (assets == null || descriptions == null) {
            return fingerprints;
        }

        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset == null) {
                continue;
            }

            String classId = asset.getString("classid");
            String instanceId = asset.getString("instanceid");
            JSONObject description = findDescription(descriptions, classId, instanceId);

            fingerprints.add(InventoryFingerprint.builder()
                    .classId(classId)
                    .instanceId(instanceId)
                    .name(description != null ? firstNonBlank(description.getString("name"), description.getString("market_hash_name")) : null)
                    .build());
        }

        return fingerprints;
    }

    private JSONObject findDescription(JSONArray descriptions, String classId, String instanceId) {
        for (int i = 0; i < descriptions.size(); i++) {
            JSONObject description = descriptions.getJSONObject(i);
            if (description == null) {
                continue;
            }

            if (Objects.equals(classId, description.getString("classid"))
                    && Objects.equals(instanceId, description.getString("instanceid"))) {
                return description;
            }
        }
        return null;
    }

    private boolean matchesSourceInventory(UserInventory source, InventoryFingerprint candidate) {
        if (source == null || candidate == null) {
            return false;
        }

        if (source.getClassId() != null && candidate.getClassId() != null
                && source.getInstanceId() != null && candidate.getInstanceId() != null
                && source.getClassId().equals(candidate.getClassId())
                && source.getInstanceId().equals(candidate.getInstanceId())) {
            return true;
        }

        return normalize(source.getName()).equals(normalize(candidate.getName()));
    }

    private boolean offerContainsInventoryItem(UserInventory sourceInventory, TradeOfferSummary offer) {
        if (sourceInventory == null || offer == null || offer.getItemsToGive() == null) {
            return false;
        }

        return offer.getItemsToGive().stream().anyMatch(item -> matchesSourceInventory(sourceInventory, item));
    }

    private boolean matchesSourceInventory(UserInventory source, TradeOfferItemSnapshot candidate) {
        if (source == null || candidate == null) {
            return false;
        }

        if (source.getAssetId() != null && candidate.getAssetId() != null
                && source.getAssetId().equals(candidate.getAssetId())) {
            return true;
        }

        return source.getClassId() != null
                && source.getInstanceId() != null
                && candidate.getClassId() != null
                && candidate.getInstanceId() != null
                && source.getClassId().equals(candidate.getClassId())
                && source.getInstanceId().equals(candidate.getInstanceId());
    }

    private String toAccountId32(String steamId64) {
        if (steamId64 == null || steamId64.isBlank()) {
            return "";
        }
        BigInteger steam64 = new BigInteger(steamId64);
        BigInteger base = new BigInteger("76561197960265728");
        return steam64.subtract(base).toString();
    }

    private boolean isTerminalFailureState(Integer state) {
        return Objects.equals(state, 5)
                || Objects.equals(state, 6)
                || Objects.equals(state, 7)
                || Objects.equals(state, 8)
                || Objects.equals(state, 10)
                || Objects.equals(state, 11);
    }

    private String resolveTradeOfferStateText(Integer state) {
        if (state == null) {
            return "未知";
        }

        return switch (state) {
            case 1 -> "无效";
            case 2 -> "进行中";
            case 3 -> "已接受";
            case 4 -> "已反报价";
            case 5 -> "已过期";
            case 6 -> "已取消";
            case 7 -> "已拒绝";
            case 8 -> "物品失效";
            case 9 -> "待手机确认";
            case 10 -> "已被二次验证取消";
            case 11 -> "交易保管中";
            default -> "未知";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Data
    @Builder
    private static class InventoryFingerprint {
        private String classId;
        private String instanceId;
        private String name;
    }

    @Data
    @Builder
    private static class TradeOfferItemSnapshot {
        private String assetId;
        private String classId;
        private String instanceId;
    }

    @Data
    @Builder
    private static class TradeOfferSummary {
        private String tradeOfferId;
        private String tradeOfferUrl;
        private String accountIdOther;
        private Integer state;
        private String stateText;
        private Long timeUpdated;
        private List<TradeOfferItemSnapshot> itemsToGive;
    }

    @Data
    @Builder
    private static class TradeOfferSnapshot {
        private Integer state;
        private String stateText;
        private boolean accepted;
        private boolean terminalFailure;
        private String errorMessage;

        static TradeOfferSnapshot error(String message) {
            return TradeOfferSnapshot.builder()
                    .stateText("查询失败")
                    .errorMessage(message)
                    .accepted(false)
                    .terminalFailure(false)
                    .build();
        }
    }

    @Data
    @Builder
    public static class BotDeliveryCheckResult {
        private String tradeOfferId;
        private Integer offerState;
        private String offerStateText;
        private boolean accepted;
        private boolean terminalFailure;
        private boolean inventoryMatched;
        private String errorMessage;

        public static BotDeliveryCheckResult error(String message) {
            return BotDeliveryCheckResult.builder()
                    .offerStateText("查询失败")
                    .errorMessage(message)
                    .accepted(false)
                    .terminalFailure(false)
                    .inventoryMatched(false)
                    .build();
        }
    }

    @Data
    @Builder
    public static class SellerOfferDetectionResult {
        private boolean found;
        private String tradeOfferId;
        private String tradeOfferUrl;
        private Integer offerState;
        private String offerStateText;
        private String errorMessage;

        public static SellerOfferDetectionResult found(String tradeOfferId, String tradeOfferUrl, Integer offerState, String offerStateText) {
            return SellerOfferDetectionResult.builder()
                    .found(true)
                    .tradeOfferId(tradeOfferId)
                    .tradeOfferUrl(tradeOfferUrl)
                    .offerState(offerState)
                    .offerStateText(offerStateText)
                    .build();
        }

        public static SellerOfferDetectionResult notFound(String errorMessage) {
            return SellerOfferDetectionResult.builder()
                    .found(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
