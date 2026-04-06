package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.service.TradeOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/order")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    @PostMapping("/create")
    public Result<TradeOrder> createOrder(@RequestBody CreateOrderRequest request) {
        Long buyerId = getCurrentUserId();
        TradeOrder order = tradeOrderService.createOrder(buyerId, request.getSellOrderId());
        return Result.success("订单创建成功", order);
    }

    @PostMapping("/{orderId}/pay")
    public Result<Boolean> payOrder(@PathVariable Long orderId) {
        Long buyerId = getCurrentUserId();
        boolean success = tradeOrderService.payOrder(orderId, buyerId);
        return Result.success("支付成功", success);
    }

    @PostMapping("/{orderId}/ship")
    public Result<Boolean> shipOrder(@PathVariable Long orderId, @RequestBody ShipOrderRequest request) {
        Long sellerId = getCurrentUserId();
        boolean success = tradeOrderService.shipOrder(orderId, sellerId, request.getTradeOfferId(), request.getTradeOfferUrl());
        return Result.success("卖家报价已登记", success);
    }

    @PostMapping("/{orderId}/bot-confirm")
    public Result<Boolean> confirmBotOffer(@PathVariable Long orderId) {
        Long sellerId = getCurrentUserId();
        boolean success = tradeOrderService.confirmBotOffer(orderId, sellerId);
        return Result.success("已触发报价检测", success);
    }

    @PostMapping("/{orderId}/bot-check")
    public Result<TradeOrder> checkBotDelivery(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        TradeOrder order = tradeOrderService.checkBotDelivery(orderId, userId);
        return Result.success("报价检测已完成", order);
    }

    @PostMapping("/{orderId}/confirm")
    public Result<Boolean> confirmReceipt(@PathVariable Long orderId) {
        Long buyerId = getCurrentUserId();
        boolean success = tradeOrderService.confirmReceipt(orderId, buyerId);
        return Result.success("确认收货成功", success);
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        boolean success = tradeOrderService.cancelOrder(orderId, userId);
        return Result.success("订单取消成功", success);
    }

    @GetMapping("/{orderId}")
    public Result<TradeOrder> getOrder(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        TradeOrder order = tradeOrderService.getOrderById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error("无权查看该订单");
        }
        return Result.success(order);
    }

    @GetMapping("/my")
    public Result<List<TradeOrder>> getMyOrders() {
        Long userId = getCurrentUserId();
        return Result.success(tradeOrderService.getUserOrders(userId));
    }

    @GetMapping("/my/buy")
    public Result<List<TradeOrder>> getMyBuyOrders() {
        Long buyerId = getCurrentUserId();
        return Result.success(tradeOrderService.getBuyerOrders(buyerId));
    }

    @GetMapping("/my/sell")
    public Result<List<TradeOrder>> getMySellOrders() {
        Long sellerId = getCurrentUserId();
        return Result.success(tradeOrderService.getSellerOrders(sellerId));
    }

    @Data
    public static class CreateOrderRequest {
        private Long sellOrderId;
    }

    @Data
    public static class ShipOrderRequest {
        private String tradeOfferId;
        private String tradeOfferUrl;
    }
}
