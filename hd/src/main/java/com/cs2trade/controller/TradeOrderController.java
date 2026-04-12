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
    public Result<TradeOrder> createOrder(@RequestBody CreateOrderRequest requestBody) {
        try {
            Long buyerId = getCurrentUserId();
            TradeOrder order = tradeOrderService.createOrder(buyerId, requestBody.getSellOrderId());
            return Result.success("订单创建成功", order);
        } catch (RuntimeException e) {
            log.warn("创建订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建订单异常", e);
            return Result.error("创建订单失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/pay")
    public Result<Boolean> payOrder(@PathVariable Long orderId) {
        try {
            Long buyerId = getCurrentUserId();
            boolean success = tradeOrderService.payOrder(orderId, buyerId);
            return Result.success("支付成功", success);
        } catch (RuntimeException e) {
            log.warn("支付订单失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("支付订单异常: orderId={}", orderId, e);
            return Result.error("支付失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/ship")
    public Result<Boolean> shipOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) ShipOrderRequest requestBody
    ) {
        try {
            Long sellerId = getCurrentUserId();
            String tradeOfferId = requestBody != null ? requestBody.getTradeOfferId() : null;
            String tradeOfferUrl = requestBody != null ? requestBody.getTradeOfferUrl() : null;
            boolean success = tradeOrderService.shipOrder(orderId, sellerId, tradeOfferId, tradeOfferUrl);
            return Result.success("已触发系统自动检测卖家发货", success);
        } catch (RuntimeException e) {
            log.warn("卖家发货失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("卖家发货异常: orderId={}", orderId, e);
            return Result.error("自动检测发货失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/bot-confirm")
    public Result<Boolean> confirmBotOffer(@PathVariable Long orderId) {
        try {
            Long sellerId = getCurrentUserId();
            boolean success = tradeOrderService.confirmBotOffer(orderId, sellerId);
            return Result.success("已触发报价检测", success);
        } catch (RuntimeException e) {
            log.warn("确认报价失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("确认报价异常: orderId={}", orderId, e);
            return Result.error("报价检测触发失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/bot-check")
    public Result<TradeOrder> checkBotDelivery(@PathVariable Long orderId) {
        try {
            Long userId = getCurrentUserId();
            TradeOrder order = tradeOrderService.checkBotDelivery(orderId, userId);
            return Result.success("报价检测已完成", order);
        } catch (RuntimeException e) {
            log.warn("检测报价失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("检测报价异常: orderId={}", orderId, e);
            return Result.error("检测报价失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/confirm")
    public Result<Boolean> confirmReceipt(@PathVariable Long orderId) {
        try {
            Long buyerId = getCurrentUserId();
            boolean success = tradeOrderService.confirmReceipt(orderId, buyerId);
            return Result.success("确认收货成功", success);
        } catch (RuntimeException e) {
            log.warn("确认收货失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("确认收货异常: orderId={}", orderId, e);
            return Result.error("确认收货失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        try {
            Long userId = getCurrentUserId();
            boolean success = tradeOrderService.cancelOrder(orderId, userId);
            return Result.success("订单取消成功", success);
        } catch (RuntimeException e) {
            log.warn("取消订单失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消订单异常: orderId={}", orderId, e);
            return Result.error("取消订单失败，请稍后重试");
        }
    }

    @GetMapping("/{orderId}")
    public Result<TradeOrder> getOrder(@PathVariable Long orderId) {
        try {
            Long userId = getCurrentUserId();
            TradeOrder order = tradeOrderService.getOrderById(orderId);
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                return Result.error("无权查看该订单");
            }
            return Result.success(order);
        } catch (RuntimeException e) {
            log.warn("获取订单详情失败: orderId={}, error={}", orderId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取订单详情异常: orderId={}", orderId, e);
            return Result.error("获取订单详情失败，请稍后重试");
        }
    }

    @GetMapping("/my")
    public Result<List<TradeOrder>> getMyOrders() {
        try {
            Long userId = getCurrentUserId();
            return Result.success(tradeOrderService.getUserOrders(userId));
        } catch (RuntimeException e) {
            log.warn("获取我的订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取我的订单异常", e);
            return Result.error("获取订单列表失败，请稍后重试");
        }
    }

    @GetMapping("/my/buy")
    public Result<List<TradeOrder>> getMyBuyOrders() {
        try {
            Long buyerId = getCurrentUserId();
            return Result.success(tradeOrderService.getBuyerOrders(buyerId));
        } catch (RuntimeException e) {
            log.warn("获取买入订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取买入订单异常", e);
            return Result.error("获取买入订单失败，请稍后重试");
        }
    }

    @GetMapping("/my/sell")
    public Result<List<TradeOrder>> getMySellOrders() {
        try {
            Long sellerId = getCurrentUserId();
            return Result.success(tradeOrderService.getSellerOrders(sellerId));
        } catch (RuntimeException e) {
            log.warn("获取卖出订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取卖出订单异常", e);
            return Result.error("获取卖出订单失败，请稍后重试");
        }
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
