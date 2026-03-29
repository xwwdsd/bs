package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.service.TradeOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易订单控制器
 * 处理交易订单相关的HTTP请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/order")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return Result<TradeOrder> 创建的订单
     */
    @PostMapping("/create")
    public Result<TradeOrder> createOrder(@RequestBody CreateOrderRequest request) {
        Long buyerId = getCurrentUserId();
        log.info("创建订单: buyerId={}, sellOrderId={}", buyerId, request.getSellOrderId());

        TradeOrder order = tradeOrderService.createOrder(buyerId, request.getSellOrderId());
        return Result.success("订单创建成功", order);
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return Result<Boolean> 支付结果
     */
    @PostMapping("/{orderId}/pay")
    public Result<Boolean> payOrder(@PathVariable Long orderId) {
        Long buyerId = getCurrentUserId();
        log.info("支付订单: orderId={}, buyerId={}", orderId, buyerId);

        boolean success = tradeOrderService.payOrder(orderId, buyerId);
        return Result.success("支付成功", success);
    }

    /**
     * 发货
     *
     * @param orderId 订单ID
     * @param request 发货请求
     * @return Result<Boolean> 发货结果
     */
    @PostMapping("/{orderId}/ship")
    public Result<Boolean> shipOrder(@PathVariable Long orderId, @RequestBody ShipOrderRequest request) {
        Long sellerId = getCurrentUserId();
        log.info("发货: orderId={}, sellerId={}", orderId, sellerId);

        boolean success = tradeOrderService.shipOrder(orderId, sellerId, request.getTradeOfferId(), request.getTradeOfferUrl());
        return Result.success("发货成功", success);
    }

    /**
     * 确认收货
     *
     * @param orderId 订单ID
     * @return Result<Boolean> 确认结果
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Boolean> confirmReceipt(@PathVariable Long orderId) {
        Long buyerId = getCurrentUserId();
        log.info("确认收货: orderId={}, buyerId={}", orderId, buyerId);

        boolean success = tradeOrderService.confirmReceipt(orderId, buyerId);
        return Result.success("确认收货成功", success);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return Result<Boolean> 取消结果
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        log.info("取消订单: orderId={}, userId={}", orderId, userId);

        boolean success = tradeOrderService.cancelOrder(orderId, userId);
        return Result.success("订单取消成功", success);
    }

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return Result<TradeOrder> 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<TradeOrder> getOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        log.info("获取订单详情: orderId={}, userId={}", orderId, userId);

        TradeOrder order = tradeOrderService.getOrderById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 验证权限
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error("无权查看该订单");
        }

        return Result.success(order);
    }

    /**
     * 获取我的订单列表
     *
     * @return Result<List<TradeOrder>> 订单列表
     */
    @GetMapping("/my")
    public Result<List<TradeOrder>> getMyOrders() {
        Long userId = getCurrentUserId();
        log.info("获取我的订单: userId={}", userId);

        List<TradeOrder> orders = tradeOrderService.getUserOrders(userId);
        return Result.success(orders);
    }

    /**
     * 获取我购买的订单
     *
     * @return Result<List<TradeOrder>> 订单列表
     */
    @GetMapping("/my/buy")
    public Result<List<TradeOrder>> getMyBuyOrders() {
        Long buyerId = getCurrentUserId();
        log.info("获取我购买的订单: buyerId={}", buyerId);

        List<TradeOrder> orders = tradeOrderService.getBuyerOrders(buyerId);
        return Result.success(orders);
    }

    /**
     * 获取我出售的订单
     *
     * @return Result<List<TradeOrder>> 订单列表
     */
    @GetMapping("/my/sell")
    public Result<List<TradeOrder>> getMySellOrders() {
        Long sellerId = getCurrentUserId();
        log.info("获取我出售的订单: sellerId={}", sellerId);

        List<TradeOrder> orders = tradeOrderService.getSellerOrders(sellerId);
        return Result.success(orders);
    }

    // ==================== 请求类 ====================

    /**
     * 创建订单请求
     */
    @lombok.Data
    public static class CreateOrderRequest {
        private Long sellOrderId;
    }

    /**
     * 发货请求
     */
    @lombok.Data
    public static class ShipOrderRequest {
        private String tradeOfferId;
        private String tradeOfferUrl;
    }
}
