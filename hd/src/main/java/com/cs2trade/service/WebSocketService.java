package com.cs2trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务类
 * 用于发送实时消息通知
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送订单状态更新通知
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param status 新状态
     * @param message 消息内容
     */
    public void sendOrderStatusUpdate(Long userId, Long orderId, Integer status, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_STATUS_UPDATE");
        payload.put("orderId", orderId);
        payload.put("status", status);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/user/" + userId + "/orders";
        messagingTemplate.convertAndSend(destination, payload);
        log.info("发送订单状态更新通知: userId={}, orderId={}, status={}", userId, orderId, status);
    }

    /**
     * 发送新订单通知给卖家
     *
     * @param sellerId 卖家ID
     * @param orderId 订单ID
     * @param itemName 商品名称
     */
    public void sendNewOrderNotification(Long sellerId, Long orderId, String itemName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NEW_ORDER");
        payload.put("orderId", orderId);
        payload.put("itemName", itemName);
        payload.put("message", "您有新的订单: " + itemName);
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/user/" + sellerId + "/orders";
        messagingTemplate.convertAndSend(destination, payload);
        log.info("发送新订单通知: sellerId={}, orderId={}", sellerId, orderId);
    }

    /**
     * 发送支付成功通知
     *
     * @param sellerId 卖家ID
     * @param orderId 订单ID
     * @param amount 金额
     */
    public void sendPaymentNotification(Long sellerId, Long orderId, String amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PAYMENT_RECEIVED");
        payload.put("orderId", orderId);
        payload.put("amount", amount);
        payload.put("message", "订单已支付，请尽快发货");
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/user/" + sellerId + "/orders";
        messagingTemplate.convertAndSend(destination, payload);
        log.info("发送支付通知: sellerId={}, orderId={}", sellerId, orderId);
    }

    /**
     * 发送发货通知给买家
     *
     * @param buyerId 买家ID
     * @param orderId 订单ID
     * @param tradeOfferUrl 交易报价链接
     */
    public void sendShipmentNotification(Long buyerId, Long orderId, String tradeOfferUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_SHIPPED");
        payload.put("orderId", orderId);
        payload.put("tradeOfferUrl", tradeOfferUrl);
        payload.put("message", "卖家已发货，请查看Steam交易报价");
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/user/" + buyerId + "/orders";
        messagingTemplate.convertAndSend(destination, payload);
        log.info("发送发货通知: buyerId={}, orderId={}", buyerId, orderId);
    }

    /**
     * 发送订单完成通知
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param isSeller 是否是卖家
     */
    public void sendOrderCompletedNotification(Long userId, Long orderId, boolean isSeller) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ORDER_COMPLETED");
        payload.put("orderId", orderId);
        payload.put("message", isSeller ? "订单已完成，款项已到账" : "订单已完成，感谢您的购买");
        payload.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/user/" + userId + "/orders";
        messagingTemplate.convertAndSend(destination, payload);
        log.info("发送订单完成通知: userId={}, orderId={}", userId, orderId);
    }
}
