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
import com.cs2trade.service.TradeOrderService;
import com.cs2trade.service.WalletService;
import com.cs2trade.service.WebSocketService;
import com.cs2trade.service.impl.SteamTradeMonitorService.BotDeliveryCheckResult;
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
        log.info("创建订单: buyerId={}, sellOrderId={}", buyerId, sellOrderId);

        SellOrder sellOrder = sellOrderMapper.selectById(sellOrderId);
        if (sellOrder == null) {
            throw new RuntimeException("出售订单不存在");
        }
        if (sellOrder.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("该商品已不在售");
        }
        if (sellOrder.getUserId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的商品");
        }

        BigDecimal orderPrice = agreedPrice != null ? agreedPrice : sellOrder.getPrice();
        if (orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("鎴愪氦浠锋牸鏃犳晥");
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
        webSocketService.sendPaymentNotification(order.getSellerId(), orderId, order.getPrice().toString());
        webSocketService.sendOrderStatusUpdate(buyerId, orderId, TradeOrder.STATUS_PAID, "订单已支付，等待卖家发送 Steam 报价");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shipOrder(Long orderId, Long sellerId, String tradeOfferId, String tradeOfferUrl) {
        log.info("登记卖家报价: orderId={}, sellerId={}", orderId, sellerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() != TradeOrder.STATUS_PAID) {
            throw new RuntimeException("订单状态不正确");
        }
        if (tradeOfferId == null || tradeOfferId.isBlank()) {
            throw new RuntimeException("请输入卖家发出的 Steam 报价 ID");
        }

        tradeOrderMapper.registerBotOffer(
                orderId,
                TradeOrder.STATUS_OFFERING,
                tradeOfferId,
                tradeOfferUrl,
                TradeOrder.DELIVERY_STAGE_SELLER_OFFER_SENT
        );

        webSocketService.sendShipmentNotification(order.getBuyerId(), orderId, tradeOfferUrl);
        webSocketService.sendOrderStatusUpdate(order.getBuyerId(), orderId, TradeOrder.STATUS_OFFERING,
                "卖家已向你发送 Steam 报价，系统将自动检测报价状态和你的库存");
        webSocketService.sendOrderStatusUpdate(order.getSellerId(), orderId, TradeOrder.STATUS_OFFERING,
                "卖家报价已登记，系统正在自动检测报价状态");
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

        tradeOrderMapper.confirmSellerBotOffer(orderId, TradeOrder.DELIVERY_STAGE_SELLER_OFFER_SENT);
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

        inspectAndApplyDelivery(order);
        return getOrderById(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReceipt(Long orderId, Long buyerId) {
        log.info("买家手动确认收货: orderId={}, buyerId={}", orderId, buyerId);

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
    public void pollOfferingOrders() {
        List<TradeOrder> orders = tradeOrderMapper.selectByStatusForMonitoring(
                TradeOrder.STATUS_OFFERING,
                botProperties.getBatchSize()
        );
        for (TradeOrder order : orders) {
            try {
                inspectAndApplyDelivery(order);
            } catch (Exception e) {
                log.warn("自动检测卖家报价失败: orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
    }

    private void inspectAndApplyDelivery(TradeOrder order) {
        BotDeliveryCheckResult result = steamTradeMonitorService.inspectSellerDelivery(order);

        TradeOrder patch = new TradeOrder();
        patch.setId(order.getId());
        patch.setSteamOfferState(result.getOfferState());
        patch.setSteamOfferStateText(result.getOfferStateText());
        patch.setLastOfferCheckAt(LocalDateTime.now());
        patch.setMonitorErrorMessage(result.getErrorMessage());

        if (result.isAccepted() && result.isInventoryMatched()) {
            settleCompletedOrder(order);
            patch.setStatus(TradeOrder.STATUS_COMPLETED);
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_BUYER_RECEIVED);
            patch.setInventoryVerifiedAt(LocalDateTime.now());
            patch.setBotReceivedAt(LocalDateTime.now());
            patch.setCompletedAt(LocalDateTime.now());
            patch.setMonitorErrorMessage(null);
        } else if (result.isTerminalFailure()) {
            patch.setStatus(TradeOrder.STATUS_PAID);
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_NONE);
            patch.setMonitorErrorMessage(firstNonBlank(result.getErrorMessage(), "Steam 报价已结束，请重新发送报价"));
        } else if (result.isAccepted()) {
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_OFFER_ACCEPTED);
        } else {
            patch.setDeliveryStage(TradeOrder.DELIVERY_STAGE_SELLER_OFFER_SENT);
        }

        tradeOrderMapper.updateBotTracking(patch);

        if (result.isAccepted() && result.isInventoryMatched()) {
            webSocketService.sendOrderCompletedNotification(order.getBuyerId(), order.getId(), false);
            webSocketService.sendOrderCompletedNotification(order.getSellerId(), order.getId(), true);
        } else if (result.isTerminalFailure()) {
            webSocketService.sendOrderStatusUpdate(order.getSellerId(), order.getId(), TradeOrder.STATUS_PAID,
                    firstNonBlank(result.getErrorMessage(), "Steam 报价已结束，请重新发送报价"));
        }
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
    }

    private void enrichOrderInfo(TradeOrder order) {
        if (order.getItemId() != null) {
            order.setItem(itemMapper.selectById(order.getItemId()));
        }
        if (order.getInventoryId() != null) {
            try {
                order.setInventory(userInventoryMapper.selectById(order.getInventoryId()));
            } catch (Exception e) {
                log.debug("填充订单库存失败: orderId={}, inventoryId={}", order.getId(), order.getInventoryId());
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
