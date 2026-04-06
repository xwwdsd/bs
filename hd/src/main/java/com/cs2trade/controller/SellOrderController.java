package com.cs2trade.controller;

import com.cs2trade.dto.PageResult;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.service.SellOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/sell-orders")
@RequiredArgsConstructor
public class SellOrderController {

    private final SellOrderService sellOrderService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    @PostMapping
    public Result<SellOrder> createSellOrder(@RequestBody CreateSellOrderRequest req) {
        Long userId = getCurrentUserId();
        log.info("创建出售订单: userId={}, inventoryId={}, assetId={}, price={}",
                userId, req.getInventoryId(), req.getAssetId(), req.getPrice());

        if (req.getInventoryId() == null && req.getAssetId() == null) {
            return Result.error("库存ID不能为空");
        }

        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("价格必须大于0");
        }

        try {
            SellOrder order = req.getInventoryId() != null
                    ? sellOrderService.createSellOrder(userId, req.getInventoryId(), req.getPrice())
                    : sellOrderService.createSellOrderByAssetId(userId, req.getAssetId(), req.getPrice());
            return Result.success("上架成功", order);
        } catch (RuntimeException e) {
            log.warn("创建出售订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建出售订单异常", e);
            return Result.error("创建出售订单失败");
        }
    }

    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancelSellOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        log.info("取消出售订单: id={}, userId={}", id, userId);

        try {
            boolean success = sellOrderService.cancelSellOrder(id, userId);
            return Result.success("取消成功", success);
        } catch (RuntimeException e) {
            log.warn("取消出售订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消出售订单异常", e);
            return Result.error("取消出售订单失败");
        }
    }

    @GetMapping("/my")
    public Result<List<SellOrder>> getMySellOrders() {
        Long userId = getCurrentUserId();
        log.info("获取我的出售订单: userId={}", userId);
        return Result.success(sellOrderService.getUserSellOrders(userId));
    }

    @GetMapping("/{id}")
    public Result<SellOrder> getSellOrderById(@PathVariable Long id) {
        log.info("获取出售订单详情: id={}", id);

        SellOrder order = sellOrderService.getOrderById(id);
        if (order == null) {
            return Result.error(404, "出售订单不存在");
        }

        return Result.success(order);
    }

    @GetMapping("/item/{itemId}")
    public Result<List<SellOrder>> getActiveOrdersByItemId(@PathVariable Long itemId) {
        return Result.success(sellOrderService.getActiveOrdersByItemId(itemId));
    }

    @GetMapping("/market")
    public Result<PageResult<SellOrder>> getMarketList(
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

        log.info("获取在售市场列表: category={}, exterior={}, quality={}, keyword={}, minPrice={}, maxPrice={}, page={}, size={}",
                category, exterior, quality, keyword, minPrice, maxPrice, page, size);

        PageResult<SellOrder> result = sellOrderService.getMarketList(
                category, exterior, quality, keyword, minPrice, maxPrice,
                sortField, sortOrder, page, size
        );

        return Result.success(result);
    }

    @Data
    public static class CreateSellOrderRequest {
        private Long inventoryId;
        private String assetId;
        private BigDecimal price;
    }
}
