package com.cs2trade.controller;

import com.cs2trade.dto.PageResult;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.service.SellOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出售订单控制器
 * 处理出售订单相关的HTTP请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/sell-orders")
@RequiredArgsConstructor
public class SellOrderController {

    private final SellOrderService sellOrderService;
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
     * 创建出售订单
     * 支持两种方式:
     * 1. 通过 inventoryId (库存ID)
     * 2. 通过 assetId (Steam Asset ID)
     *
     * @param req 创建请求
     * @return Result<SellOrder> 创建的订单
     */
    @PostMapping
    public Result<SellOrder> createSellOrder(@RequestBody CreateSellOrderRequest req) {
        Long userId = getCurrentUserId();
        log.info("创建出售订单: userId={}, inventoryId={}, assetId={}, price={}", 
                userId, req.getInventoryId(), req.getAssetId(), req.getPrice());
        
        if (req.getInventoryId() == null && req.getAssetId() == null) {
            log.warn("inventoryId和assetId都为空，请求数据: {}", req);
            return Result.error("库存ID不能为空");
        }
        
        if (req.getPrice() == null || req.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.error("价格必须大于0");
        }
        
        try {
            SellOrder order;
            if (req.getInventoryId() != null) {
                order = sellOrderService.createSellOrder(userId, req.getInventoryId(), req.getPrice());
            } else {
                order = sellOrderService.createSellOrderByAssetId(userId, req.getAssetId(), req.getPrice());
            }
            return Result.success("上架成功", order);
        } catch (RuntimeException e) {
            log.warn("创建出售订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建出售订单异常", e);
            return Result.error("创建出售订单失败");
        }
    }

    /**
     * 取消出售订单
     *
     * @param id 订单ID
     * @return Result<Boolean> 取消结果
     */
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

    /**
     * 获取我的出售订单
     *
     * @return Result<List<SellOrder>> 订单列表
     */
    @GetMapping("/my")
    public Result<List<SellOrder>> getMySellOrders() {
        Long userId = getCurrentUserId();
        log.info("获取我的出售订单: userId={}", userId);

        List<SellOrder> orders = sellOrderService.getUserSellOrders(userId);
        return Result.success(orders);
    }

    /**
     * 获取指定饰品的在售订单
     *
     * @param itemId 饰品ID
     * @return Result<List<SellOrder>> 订单列表
     */
    @GetMapping("/item/{itemId}")
    public Result<List<SellOrder>> getActiveOrdersByItemId(@PathVariable Long itemId) {
        List<SellOrder> orders = sellOrderService.getActiveOrdersByItemId(itemId);
        return Result.success(orders);
    }

    /**
     * 获取在售市场列表（分页）
     *
     * @param category  分类
     * @param exterior  外观
     * @param quality   品质
     * @param keyword   关键词
     * @param minPrice  最低价格
     * @param maxPrice  最高价格
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @param page      页码
     * @param size      每页数量
     * @return Result<PageResult<SellOrder>> 分页结果
     */
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
                sortField, sortOrder, page, size);

        return Result.success(result);
    }

    // ==================== 请求类 ====================

    /**
     * 创建出售订单请求
     */
    @lombok.Data
    public static class CreateSellOrderRequest {
        private Long inventoryId;
        private String assetId;
        private BigDecimal price;
    }
}
