package com.cs2trade.service.impl;

import com.cs2trade.entity.*;
import com.cs2trade.mapper.*;
import com.cs2trade.service.TradeOrderService;
import com.cs2trade.service.WalletService;
import com.cs2trade.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 交易订单服务实现类
 * 实现交易订单相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper tradeOrderMapper;
    private final SellOrderMapper sellOrderMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final UserInventoryMapper userInventoryMapper;
    private final WalletService walletService;
    private final WebSocketService webSocketService;

    // 手续费率 5%
    private static final BigDecimal FEE_RATE = new BigDecimal("0.05");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long buyerId, Long sellOrderId) {
        log.info("创建订单: buyerId={}, sellOrderId={}", buyerId, sellOrderId);

        // 获取出售订单
        SellOrder sellOrder = sellOrderMapper.selectById(sellOrderId);
        if (sellOrder == null) {
            throw new RuntimeException("出售订单不存在");
        }

        // 检查订单状态
        if (sellOrder.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("该商品已不在售");
        }

        // 不能购买自己的商品
        if (sellOrder.getUserId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的商品");
        }

        // 创建交易订单
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(sellOrder.getUserId());
        order.setItemId(sellOrder.getItemId());
        order.setInventoryId(sellOrder.getInventoryId());
        order.setPrice(sellOrder.getPrice());

        // 计算手续费和实际到账金额
        BigDecimal fee = sellOrder.getPrice().multiply(FEE_RATE);
        BigDecimal actualAmount = sellOrder.getPrice().subtract(fee);
        order.setFee(fee);
        order.setActualAmount(actualAmount);

        order.setStatus(TradeOrder.STATUS_PENDING);

        // 保存订单
        tradeOrderMapper.insert(order);

        // 更新出售订单状态为交易中
        sellOrderMapper.updateStatus(sellOrderId, SellOrder.STATUS_TRADING);

        // 发送WebSocket通知给卖家
        Item item = itemMapper.selectById(sellOrder.getItemId());
        webSocketService.sendNewOrderNotification(order.getSellerId(), order.getId(), 
                item != null ? item.getNameCn() : "未知饰品");

        log.info("订单创建成功: orderNo={}", order.getOrderNo());
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

        // 验证买家
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 验证订单状态
        if (order.getStatus() != TradeOrder.STATUS_PENDING) {
            throw new RuntimeException("订单状态不正确");
        }

        // 检查买家余额
        Wallet buyerWallet = walletService.getWalletByUserId(buyerId);
        if (buyerWallet.getBalance().compareTo(order.getPrice()) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 扣除买家余额并记录流水
        boolean deductSuccess = walletService.pay(buyerId, order.getPrice(), "购买饰品: " + order.getOrderNo(), orderId, "TRADE_ORDER");
        if (!deductSuccess) {
            throw new RuntimeException("支付失败");
        }

        // 更新订单状态为待发货
        tradeOrderMapper.updatePaidStatus(orderId, TradeOrder.STATUS_PAID);

        // 发送WebSocket通知给卖家
        webSocketService.sendPaymentNotification(order.getSellerId(), orderId, 
                order.getPrice().toString());

        // 发送WebSocket通知给买家
        webSocketService.sendOrderStatusUpdate(buyerId, orderId, TradeOrder.STATUS_PAID, 
                "订单已支付，等待卖家发货");

        log.info("订单支付成功: orderId={}", orderId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shipOrder(Long orderId, Long sellerId, String tradeOfferId, String tradeOfferUrl) {
        log.info("发货: orderId={}, sellerId={}", orderId, sellerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证卖家
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 验证订单状态
        if (order.getStatus() != TradeOrder.STATUS_PAID) {
            throw new RuntimeException("订单状态不正确");
        }

        // 更新订单状态为待收货
        tradeOrderMapper.updateSentStatus(orderId, TradeOrder.STATUS_SENT, tradeOfferId, tradeOfferUrl);

        // 发送WebSocket通知给买家
        webSocketService.sendShipmentNotification(order.getBuyerId(), orderId, tradeOfferUrl);

        log.info("订单发货成功: orderId={}", orderId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReceipt(Long orderId, Long buyerId) {
        log.info("确认收货: orderId={}, buyerId={}", orderId, buyerId);

        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证买家
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 验证订单状态
        if (order.getStatus() != TradeOrder.STATUS_SENT) {
            throw new RuntimeException("订单状态不正确");
        }

        // 增加卖家余额
        boolean addSuccess = walletService.receive(order.getSellerId(), order.getActualAmount(), "出售饰品收入: " + order.getOrderNo(), orderId, "TRADE_ORDER");
        if (!addSuccess) {
            throw new RuntimeException("确认收货失败");
        }

        // 更新订单状态为已完成
        tradeOrderMapper.updateCompletedStatus(orderId, TradeOrder.STATUS_COMPLETED);

        // 更新出售订单状态为已售出
        sellOrderMapper.updateStatusByInventoryId(order.getInventoryId(), SellOrder.STATUS_SOLD);

        // 更新库存状态为已售出
        userInventoryMapper.updateStatus(order.getInventoryId(), UserInventory.STATUS_SOLD);

        // 发送WebSocket通知给买家和卖家
        webSocketService.sendOrderCompletedNotification(order.getBuyerId(), orderId, false);
        webSocketService.sendOrderCompletedNotification(order.getSellerId(), orderId, true);

        log.info("订单完成: orderId={}", orderId);
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

        // 验证权限（买家或卖家都可以取消）
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 验证订单状态（只有待确认和待发货状态可以取消）
        if (order.getStatus() != TradeOrder.STATUS_PENDING && order.getStatus() != TradeOrder.STATUS_PAID) {
            throw new RuntimeException("订单状态不正确，无法取消");
        }

        // 如果已支付，退款给买家
        if (order.getStatus() == TradeOrder.STATUS_PAID) {
            boolean refundSuccess = walletService.receive(order.getBuyerId(), order.getPrice(), "订单取消退款: " + order.getOrderNo(), orderId, "TRADE_ORDER");
            if (!refundSuccess) {
                throw new RuntimeException("退款失败");
            }
        }

        // 更新订单状态为已取消
        tradeOrderMapper.updateCancelledStatus(orderId, TradeOrder.STATUS_CANCELLED);

        // 恢复出售订单状态为在售
        sellOrderMapper.updateStatusByInventoryId(order.getInventoryId(), SellOrder.STATUS_ON_SALE);

        log.info("订单取消成功: orderId={}", orderId);
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

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "TD" + timestamp + random;
    }

    /**
     * 填充订单关联信息
     */
    private void enrichOrderInfo(TradeOrder order) {
        if (order.getItemId() != null) {
            order.setItem(itemMapper.selectById(order.getItemId()));
        }
        if (order.getBuyerId() != null) {
            order.setBuyer(userMapper.selectById(order.getBuyerId()));
        }
        if (order.getSellerId() != null) {
            order.setSeller(userMapper.selectById(order.getSellerId()));
        }
    }
}
