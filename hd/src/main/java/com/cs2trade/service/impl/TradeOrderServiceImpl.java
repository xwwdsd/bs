package com.cs2trade.service.impl;

import com.cs2trade.config.SteamTradeBotProperties;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.entity.Wallet;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.TradeOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.MessagePublishService;
import com.cs2trade.service.TradeOrderService;
import com.cs2trade.service.WalletService;
import com.cs2trade.service.WebSocketService;
import com.cs2trade.service.impl.SteamTradeMonitorService.BotDeliveryCheckResult;
import com.cs2trade.service.impl.SteamTradeMonitorService.TradeOfferDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.05");

    private final TradeOrderMapper tradeOrderMapper;
    private final SellOrderMapper sellOrderMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final UserInventoryMapper userInventoryMapper;
    private final WalletService walletService;
    private final MessagePublishService messagePublishService;
    private final WebSocketService webSocketService;
    private final SteamTradeMonitorService steamTradeMonitorService;
    private final SteamTradeBotProperties botProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long buyerId, Long sellOrderId) {
        return createOrder(buyerId, sellOrderId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long buyerId, Long sellOrderId, BigDecimal agreedPrice) {
        log.info("创建交易订单: buyerId={}, sellOrderId={}", buyerId, sellOrderId);

        SellOrder sellOrder = sellOrderMapper.selectById(sellOrderId);
        if (sellOrder == null) {
            throw new RuntimeException("出售订单不存在");
        }
        if (sellOrder.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("该物品当前不在售");
        }
        if (sellOrder.getUserId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的物品");
        }

        BigDecimal orderPrice = agreedPrice != null ? agreedPrice : sellOrder.getPrice();
        if (orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("订单价格无效");
        }

        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(sellOrder.getUserId());
        order.setItemId(sellOrder.getItemId());
        order.setInventoryId(sellOrder.getInventoryId());
        order.setPrice(orderPrice);

        BigDecimal fee = orderPrice.multiply(FEE_RATE);
        order.setFee(fee);
        order.setActualAmount(orderPrice.subtract(fee));
        order.setStatus(TradeOrder.STATUS_PENDING);
        order.setDeliveryStage(TradeOrder.DELIVERY_STAGE_NONE);

        tradeOrderMapper.insert(order);
        sellOrderMapper.updateStatus(sellOrderId, SellOrder.STATUS_TRADING);

        Item item = itemMapper.selectById(sellOrder.getItemId());
        sendOrderCreatedMessage(order, item);
        webSocketService.sendNewOrderNotification(
                order.getSellerId(),
                order.getId(),
                item != null ? firstNonBlank(item.getNameCn(), item.getName()) : "未知饰品"
        );

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, Long buyerId) {
        log.info("支付订单: orderId={}, buyerId={}", orderId, buyerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_PENDING) {
            throw new RuntimeException("订单状态不正确");
        }

        Wallet buyerWallet = walletService.getWalletByUserId(buyerId);
        if (buyerWallet.getBalance().compareTo(order.getPrice()) < 0) {
            throw new RuntimeException("余额不足");
        }

        boolean deductSuccess = walletService.pay(
                buyerId,
                order.getPrice(),
                "购买饰品: " + order.getOrderNo(),
                orderId,
                "TRADE_ORDER"
        );
        if (!deductSuccess) {
            throw new RuntimeException("支付失败");
        }

        tradeOrderMapper.updatePaidStatus(orderId, TradeOrder.STATUS_PAID);
        sendPaymentMessages(order);
        webSocketService.sendPaymentNotification(order.getSellerId(), orderId, order.getPrice().toString());
        webSocketService.sendOrderStatusUpdate(
                buyerId,
                orderId,
                TradeOrder.STATUS_PAID,
                "订单已支付，请在 Steam 向卖家发送报价，系统会自动检测报价"
        );
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shipOrder(Long orderId, Long buyerId, String tradeOfferId, String tradeOfferUrl) {
        log.info("检测买家报价: orderId={}, buyerId={}", orderId, buyerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_PAID) {
            throw new RuntimeException("订单状态不正确");
        }

        tradeOfferId = firstNonBlank(tradeOfferId, extractTradeOfferId(tradeOfferUrl));

        if (tradeOfferId == null || tradeOfferId.isBlank()) {
            TradeOfferDetectionResult detectedOffer = steamTradeMonitorService.detectBuyerOffer(order);
            if (!detectedOffer.isFound()) {
                throw new RuntimeException(firstNonBlank(
                        detectedOffer.getErrorMessage(),
                        "暂未检测到你发给卖家的 Steam 报价，请先在 Steam 中向卖家发送报价后稍后再试"
                ));
            }
            tradeOfferId = detectedOffer.getTradeOfferId();
            tradeOfferUrl = firstNonBlank(detectedOffer.getTradeOfferUrl(), tradeOfferUrl);
        } else {
            TradeOfferDetectionResult validatedOffer = steamTradeMonitorService.validateBuyerOfferById(order, tradeOfferId);
            if (!validatedOffer.isFound()) {
                throw new RuntimeException(firstNonBlank(
                        validatedOffer.getErrorMessage(),
                        "该 Steam 报价无法匹配当前订单"
                ));
            }
            tradeOfferUrl = firstNonBlank(
                    tradeOfferUrl,
                    validatedOffer.getTradeOfferUrl(),
                    "https://steamcommunity.com/tradeoffer/" + tradeOfferId + "/"
            );
        }

        registerBuyerOffer(order, tradeOfferId, tradeOfferUrl);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmBotOffer(Long orderId, Long sellerId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_OFFERING) {
            throw new RuntimeException("当前订单不处于报价检测阶段");
        }

        tradeOrderMapper.confirmSellerBotOffer(orderId, TradeOrder.DELIVERY_STAGE_BUYER_OFFER_SENT);
        inspectAndApplyDelivery(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder checkBotDelivery(Long orderId, Long userId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        if (order.getStatus() == TradeOrder.STATUS_PAID
                && (order.getTradeOfferId() == null || order.getTradeOfferId().isBlank())) {
            TradeOfferDetectionResult detectedOffer = steamTradeMonitorService.detectBuyerOffer(order);
            if (detectedOffer.isFound()) {
                registerBuyerOffer(order, detectedOffer.getTradeOfferId(), detectedOffer.getTradeOfferUrl());
                order = tradeOrderMapper.selectById(orderId);
            }
        }

        if (order.getTradeOfferId() == null || order.getTradeOfferId().isBlank()) {
            throw new RuntimeException("暂未检测到 Steam 报价，请先由买家在 Steam 向卖家发送报价后稍后再试");
        }

        inspectAndApplyDelivery(order);
        return getOrderById(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReceipt(Long orderId, Long buyerId) {
        log.info("买家确认收货: orderId={}, buyerId={}", orderId, buyerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_SENT) {
            throw new RuntimeException("订单状态不正确");
        }

        settleCompletedOrder(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        log.info("取消订单: orderId={}, userId={}", orderId, userId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_PENDING && order.getStatus() != TradeOrder.STATUS_PAID) {
            throw new RuntimeException("当前订单状态不允许取消");
        }

        if (order.getStatus() == TradeOrder.STATUS_PAID) {
            boolean refundSuccess = walletService.receive(
                    order.getBuyerId(),
                    order.getPrice(),
                    "订单取消退款: " + order.getOrderNo(),
                    orderId,
                    "TRADE_ORDER"
            );
            if (!refundSuccess) {
                throw new RuntimeException("退款失败");
            }
        }

        tradeOrderMapper.updateCancelledStatus(orderId, TradeOrder.STATUS_CANCELLED);
        sellOrderMapper.updateStatusByInventoryId(order.getInventoryId(), SellOrder.STATUS_ON_SALE);
        sendCancelledMessages(order);
        return true;
    }

    @Override
    public TradeOrder getOrderById(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order != null) {
            enrichOrderInfo(order);
        }
        return order;
    }

    @Override
    public List<TradeOrder> getUserOrders(Long userId) {
        List<TradeOrder> orders = tradeOrderMapper.selectByUserId(userId);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    public List<TradeOrder> getBuyerOrders(Long buyerId) {
        List<TradeOrder> orders = tradeOrderMapper.selectByBuyerId(buyerId);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    public List<TradeOrder> getSellerOrders(Long sellerId) {
        List<TradeOrder> orders = tradeOrderMapper.selectBySellerId(sellerId);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Transactional(rollbackFor = Exception.class)
    public void pollPendingShipmentOrders() {
        List<TradeOrder> orders = tradeOrderMapper.selectPendingShipmentForAutoDetection(
                TradeOrder.STATUS_PAID,
                botProperties.getBatchSize()
        );
        for (TradeOrder order : orders) {
            try {
                TradeOfferDetectionResult detectedOffer = steamTradeMonitorService.detectBuyerOffer(order);
                if (detectedOffer.isFound()) {
                    registerBuyerOffer(order, detectedOffer.getTradeOfferId(), detectedOffer.getTradeOfferUrl());
                }
            } catch (Exception e) {
                log.warn("自动检测买家报价失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void pollOfferingOrders() {
        List<TradeOrder> orders = tradeOrderMapper.selectByStatusForMonitoring(
                TradeOrder.STATUS_OFFERING,
                botProperties.getBatchSize()
        );
        for (TradeOrder order : orders) {
            try {
                inspectAndApplyDelivery(order);
            } catch (Exception e) {
                log.warn("自动检测报价状态失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
    }

    private void inspectAndApplyDelivery(TradeOrder order) {
        BotDeliveryCheckResult result = steamTradeMonitorService.inspectBuyerOfferDelivery(order);

        TradeOrder patch = new TradeOrder();
        patch.setId(order.getId());
        patch.setSteamOfferState(result.getOfferState());
        patch.setSteamOfferStateText(result.getOfferStateText());
        patch.setLastOfferCheckAt(LocalDateTime.now());
        patch.setMonitorErrorMessage(result.getErrorMessage());

        if (result.isAccepted() && result.isInventoryMatched()) {
            patch.setStatus(TradeOrder.STATUS_SENT);
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_BUYER_RECEIVED);
            patch.setInventoryVerifiedAt(LocalDateTime.now());
            patch.setBotReceivedAt(LocalDateTime.now());
            patch.setMonitorErrorMessage(null);
        } else if (result.isTerminalFailure()) {
            patch.setStatus(TradeOrder.STATUS_PAID);
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_NONE);
            patch.setMonitorErrorMessage(firstNonBlank(result.getErrorMessage(), "Steam 报价已结束，请重新发送报价"));
        } else if (result.isAccepted()) {
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_OFFER_ACCEPTED);
        } else {
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_BUYER_OFFER_SENT);
        }

        tradeOrderMapper.updateBotTracking(patch);

        if (result.isAccepted() && result.isInventoryMatched()) {
            if (order.getStatus() != TradeOrder.STATUS_SENT) {
                sendDeliveryConfirmedMessages(order);
            }
            webSocketService.sendOrderStatusUpdate(
                    order.getBuyerId(),
                    order.getId(),
                    TradeOrder.STATUS_SENT,
                    "Steam 鎶ヤ环宸茶鎺ュ彈锛岃涔板纭鏀惰揣"
            );
            webSocketService.sendOrderStatusUpdate(
                    order.getSellerId(),
                    order.getId(),
                    TradeOrder.STATUS_SENT,
                    "璁㈠崟宸茶繘鍏ュ緟鏀惰揣闃舵锛岀瓑寰呬拱瀹剁‘璁ゆ敹璐?"
            );
        } else if (result.isTerminalFailure()) {
            if (order.getStatus() != TradeOrder.STATUS_PAID || !TradeOrder.DELIVERY_STAGE_NONE.equals(order.getDeliveryStage())) {
                sendOfferFailedMessage(order, result.getErrorMessage());
            }
            webSocketService.sendOrderStatusUpdate(
                    order.getSellerId(),
                    order.getId(),
                    TradeOrder.STATUS_PAID,
                    firstNonBlank(result.getErrorMessage(), "Steam 报价已结束，请重新发送报价")
            );
        }
    }

    private void registerBuyerOffer(TradeOrder order, String tradeOfferId, String tradeOfferUrl) {
        tradeOrderMapper.registerBotOffer(
                order.getId(),
                TradeOrder.STATUS_OFFERING,
                tradeOfferId,
                tradeOfferUrl,
                TradeOrder.DELIVERY_STAGE_BUYER_OFFER_SENT
        );
        sendShipmentMessages(order);

        webSocketService.sendShipmentNotification(order.getBuyerId(), order.getId(), tradeOfferUrl);
        webSocketService.sendOrderStatusUpdate(
                order.getBuyerId(),
                order.getId(),
                TradeOrder.STATUS_OFFERING,
                "已检测到你发给卖家的 Steam 报价，系统将自动检测报价状态和你的库存"
        );
        webSocketService.sendOrderStatusUpdate(
                order.getSellerId(),
                order.getId(),
                TradeOrder.STATUS_OFFERING,
                "已检测到买家发来的 Steam 报价，系统正在自动跟踪报价状态"
        );
    }

    private void settleCompletedOrder(TradeOrder order) {
        boolean addSuccess = walletService.receive(
                order.getSellerId(),
                order.getActualAmount(),
                "出售饰品收入: " + order.getOrderNo(),
                order.getId(),
                "TRADE_ORDER"
        );
        if (!addSuccess) {
            throw new RuntimeException("卖家收款失败");
        }

        tradeOrderMapper.updateCompletedStatus(order.getId(), TradeOrder.STATUS_COMPLETED);
        sellOrderMapper.updateStatusByInventoryId(order.getInventoryId(), SellOrder.STATUS_SOLD);
        userInventoryMapper.updateStatus(order.getInventoryId(), UserInventory.STATUS_SOLD);
        sendCompletedMessages(order);
    }

    private void sendOrderCreatedMessage(TradeOrder order, Item item) {
        String itemName = resolveItemName(order, item);
        String buyerName = messagePublishService.resolveUsername(order.getBuyerId(), "买家");
        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                2,
                "收到新的交易订单",
                buyerName + " 已下单购买「" + itemName + "」，订单号 " + order.getOrderNo()
                        + "，成交价 " + messagePublishService.formatMoneyWithSymbol(order.getPrice())
                        + "。请等待买家付款。",
                order,
                order.getBuyerId(),
                buyerName
        );
    }

    private void sendPaymentMessages(TradeOrder order) {
        String itemName = resolveItemName(order);
        String buyerName = messagePublishService.resolveUsername(order.getBuyerId(), "买家");
        String sellerName = messagePublishService.resolveUsername(order.getSellerId(), "卖家");

        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                1,
                "订单支付成功",
                "你已完成订单 " + order.getOrderNo() + " 的支付，商品为「" + itemName + "」，金额 "
                        + messagePublishService.formatMoneyWithSymbol(order.getPrice()) + "。请在 Steam 向卖家发送报价，系统会自动检测报价状态。",
                order,
                order.getSellerId(),
                sellerName
        );

        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                2,
                "买家已付款，等待买家发送报价",
                buyerName + " 已完成订单 " + order.getOrderNo() + " 的支付，成交饰品为「" + itemName
                        + "」，金额 " + messagePublishService.formatMoneyWithSymbol(order.getPrice())
                        + "。请等待买家在 Steam 向你发送报价。",
                order,
                order.getBuyerId(),
                buyerName
        );
    }

    private void sendShipmentMessages(TradeOrder order) {
        String itemName = resolveItemName(order);
        String sellerName = messagePublishService.resolveUsername(order.getSellerId(), "卖家");

        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                1,
                "已检测到你的 Steam 报价",
                "订单 " + order.getOrderNo() + " 的「" + itemName
                        + "」已匹配到你发给卖家的 Steam 报价，系统正在自动检测报价状态和库存变更。",
                order,
                order.getSellerId(),
                sellerName
        );

        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                2,
                "买家报价已记录",
                "系统已记录买家对订单 " + order.getOrderNo() + " 发来的 Steam 报价，正在持续跟踪交易状态。",
                order
        );
    }

    private void sendDeliveryConfirmedMessages(TradeOrder order) {
        String itemName = resolveItemName(order);

        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                1,
                "系统已确认收货",
                "系统已检测到订单 " + order.getOrderNo() + " 的「" + itemName
                        + "」进入你的库存，请尽快确认收货以完成交易。",
                order
        );

        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                2,
                "买家已收到饰品",
                "系统已确认订单 " + order.getOrderNo() + " 的「" + itemName
                        + "」进入买家库存，等待买家确认收货后自动完成结算。",
                order
        );
    }

    private void sendOfferFailedMessage(TradeOrder order, String errorMessage) {
        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                1,
                "Steam 报价需要重新发送",
                "订单 " + order.getOrderNo() + " 的 Steam 报价状态异常。"
                        + firstNonBlank(errorMessage, "系统未能确认本次报价，请重新向卖家发送报价。"),
                order
        );
    }

    private void sendCompletedMessages(TradeOrder order) {
        String itemName = resolveItemName(order);

        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                3,
                "交易已完成",
                "订单 " + order.getOrderNo() + " 已完成，你已成功收到「" + itemName + "」。",
                order,
                order.getSellerId(),
                messagePublishService.resolveUsername(order.getSellerId(), "卖家")
        );

        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                3,
                "订单已完成结算",
                "订单 " + order.getOrderNo() + " 已完成，出售「" + itemName + "」的收入 "
                        + messagePublishService.formatMoneyWithSymbol(order.getActualAmount())
                        + " 已发放到你的钱包。",
                order
        );
    }

    private void sendCancelledMessages(TradeOrder order) {
        String itemName = resolveItemName(order);
        boolean refunded = order.getStatus() == TradeOrder.STATUS_PAID;

        messagePublishService.sendTradeMessage(
                order.getBuyerId(),
                refunded ? 5 : 4,
                refunded ? "订单已取消并退款" : "订单已取消",
                refunded
                        ? "订单 " + order.getOrderNo() + " 已取消，支付金额 "
                                + messagePublishService.formatMoneyWithSymbol(order.getPrice()) + " 已退回你的钱包。"
                        : "订单 " + order.getOrderNo() + " 已取消，未发生扣款。",
                order
        );

        messagePublishService.sendTradeMessage(
                order.getSellerId(),
                4,
                "交易订单已取消",
                "订单 " + order.getOrderNo() + " 已取消，饰品「" + itemName + "」已恢复为可出售状态。",
                order
        );
    }

    private String resolveItemName(TradeOrder order) {
        return resolveItemName(order, null);
    }

    private String resolveItemName(TradeOrder order, Item item) {
        if (item != null) {
            return firstNonBlank(item.getNameCn(), item.getName(), "这件饰品");
        }
        return messagePublishService.resolveItemName(order.getItemId());
    }

    private void enrichOrderInfo(TradeOrder order) {
        if (order.getItemId() != null) {
            order.setItem(itemMapper.selectById(order.getItemId()));
        }
        if (order.getInventoryId() != null) {
            try {
                order.setInventory(userInventoryMapper.selectById(order.getInventoryId()));
            } catch (Exception e) {
                log.debug("补充订单库存失败: orderId={}, inventoryId={}", order.getId(), order.getInventoryId());
            }
        }
        if (order.getBuyerId() != null) {
            order.setBuyer(userMapper.selectById(order.getBuyerId()));
        }
        if (order.getSellerId() != null) {
            order.setSeller(userMapper.selectById(order.getSellerId()));
        }
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "TD" + timestamp + random;
    }

    private String extractTradeOfferId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.matches("\\d+")) {
            return trimmed;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("/tradeoffer/(\\d+)")
                .matcher(trimmed);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
