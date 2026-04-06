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

import java.util.ArrayList;
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

    public BotDeliveryCheckResult inspectSellerDelivery(TradeOrder order) {
        if (order == null || order.getTradeOfferId() == null || order.getTradeOfferId().isBlank()) {
            return BotDeliveryCheckResult.error("Trade offer id is missing");
        }

        User seller = userMapper.selectById(order.getSellerId());
        if (seller == null) {
            return BotDeliveryCheckResult.error("Seller account is missing");
        }

        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer == null) {
            return BotDeliveryCheckResult.error("Buyer account is missing");
        }

        if (seller.getSteamApiKey() == null || seller.getSteamApiKey().isBlank()) {
            return BotDeliveryCheckResult.error("Seller Steam API Key is not configured");
        }

        if (buyer.getSteamId() == null || buyer.getSteamId().isBlank()) {
            return BotDeliveryCheckResult.error("Buyer Steam account is not bound");
        }

        TradeOfferSnapshot offerSnapshot = fetchTradeOffer(seller.getSteamApiKey(), order.getTradeOfferId());
        if (offerSnapshot == null) {
            return BotDeliveryCheckResult.error("Failed to query Steam trade offer status");
        }

        boolean inventoryMatched = false;
        String inventoryMessage = null;

        if (offerSnapshot.isAccepted()) {
            try {
                inventoryMatched = buyerInventoryHasMatchingItem(order, buyer.getSteamId());
                if (!inventoryMatched) {
                    inventoryMessage = "Trade offer accepted, but the buyer inventory does not show the item yet";
                }
            } catch (Exception e) {
                inventoryMessage = "Failed to query buyer inventory: " + e.getMessage();
                log.warn("Failed to query buyer inventory: orderId={}, error={}", order.getId(), e.getMessage());
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
                return TradeOfferSnapshot.error("Steam trade offer query failed with HTTP " + response.getStatusCode().value());
            }

            JSONObject root = JSONObject.parseObject(response.getBody());
            JSONObject responseObj = root.getJSONObject("response");
            JSONObject offer = responseObj != null ? responseObj.getJSONObject("offer") : null;
            if (offer == null) {
                return TradeOfferSnapshot.error("Steam trade offer response is empty");
            }

            Integer state = offer.getInteger("trade_offer_state");
            return TradeOfferSnapshot.builder()
                    .state(state)
                    .stateText(resolveTradeOfferStateText(state))
                    .accepted(Objects.equals(state, 3))
                    .terminalFailure(isTerminalFailureState(state))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to query Steam trade offer: tradeOfferId={}, error={}", tradeOfferId, e.getMessage());
            return TradeOfferSnapshot.error("Failed to query Steam trade offer: " + e.getMessage());
        }
    }

    private boolean buyerInventoryHasMatchingItem(TradeOrder order, String buyerSteamId) {
        UserInventory sourceInventory = userInventoryMapper.selectById(order.getInventoryId());
        if (sourceInventory == null) {
            throw new IllegalStateException("Source inventory snapshot is missing");
        }

        JSONObject inventoryJson = steamApiClient.getInventory(buyerSteamId);
        if (inventoryJson == null) {
            throw new IllegalStateException("Buyer inventory response is empty");
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
}
