package com.cs2trade.controller;

import com.cs2trade.dto.PageResult;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.BuyOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.service.BuyOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/buy-orders")
@RequiredArgsConstructor
public class BuyOrderController {

    private final BuyOrderService buyOrderService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    @PostMapping
    public Result<BuyOrder> createBuyOrder(@RequestBody CreateBuyOrderRequest req) {
        Long userId = getCurrentUserId();
        try {
            BuyOrder order = buyOrderService.createBuyOrder(
                    userId,
                    req.getItemId(),
                    req.getPrice(),
                    req.getQuantity(),
                    req.getAutoAccept()
            );
            return Result.success("求购发布成功", order);
        } catch (RuntimeException e) {
            log.warn("Create buy order failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancelBuyOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        try {
            return Result.success("求购已取消", buyOrderService.cancelBuyOrder(id, userId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/respond")
    public Result<TradeOrder> respondToBuyOrder(@PathVariable Long id, @RequestBody RespondBuyOrderRequest req) {
        Long userId = getCurrentUserId();
        try {
            TradeOrder tradeOrder = buyOrderService.respondToBuyOrder(userId, id, req.getInventoryId());
            return Result.success("已响应求购，请前往订单继续处理", tradeOrder);
        } catch (RuntimeException e) {
            log.warn("Respond buy order failed: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<BuyOrder>> getMyBuyOrders() {
        Long userId = getCurrentUserId();
        return Result.success(buyOrderService.getUserBuyOrders(userId));
    }

    @GetMapping("/{id}")
    public Result<BuyOrder> getBuyOrderById(@PathVariable Long id) {
        BuyOrder order = buyOrderService.getOrderById(id);
        if (order == null) {
            return Result.error(404, "求购单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/item/{itemId}")
    public Result<List<BuyOrder>> getActiveOrdersByItemId(@PathVariable Long itemId) {
        return Result.success(buyOrderService.getActiveOrdersByItemId(itemId));
    }

    @GetMapping("/market")
    public Result<PageResult<BuyOrder>> getMarketList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String exterior,
            @RequestParam(required = false) String quality,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "created_at") String sortField,
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(buyOrderService.getMarketList(
                category, exterior, quality, keyword, minPrice, maxPrice, sortField, sortOrder, page, size
        ));
    }

    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    @Data
    public static class CreateBuyOrderRequest {
        private Long itemId;
        private BigDecimal price;
        private Integer quantity;
        private Integer autoAccept;
    }

    @Data
    public static class RespondBuyOrderRequest {
        private Long inventoryId;
    }
}
