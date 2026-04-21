package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.security.CustomUserDetails;
import com.cs2trade.service.MarketAnalyticsService;
import com.cs2trade.service.SteamInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存控制器
 * 处理用户库存相关的请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
@CrossOrigin
public class InventoryController {

    private final SteamInventoryService steamInventoryService;
    private final MarketAnalyticsService marketAnalyticsService;

    /**
     * 获取当前用户的库存列表
     *
     * @return Result<List<UserInventory>> 库存列表
     */
    @GetMapping
    public Result<List<UserInventory>> getMyInventory() {
        Long userId = getCurrentUserId();
        log.info("获取用户库存: userId={}", userId);

        List<UserInventory> inventory = steamInventoryService.getUserInventory(userId);
        return Result.success(inventory);
    }

    /**
     * 同步Steam库存
     * 从Steam API获取最新库存数据
     *
     * @return Result<List<UserInventory>> 同步后的库存列表
     */
    @PostMapping("/sync")
    public Result<List<UserInventory>> syncInventory() {
        Long userId = getCurrentUserId();
        log.info("同步Steam库存: userId={}", userId);

        try {
            List<UserInventory> inventory = steamInventoryService.syncInventory(userId);
            return Result.success("库存同步成功", inventory);
        } catch (Exception e) {
            log.error("同步库存失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 获取可交易的库存
     *
     * @return Result<List<UserInventory>> 可交易的库存列表
     */
    @GetMapping("/marketable")
    public Result<List<UserInventory>> getMarketableInventory() {
        Long userId = getCurrentUserId();
        log.info("获取可交易库存: userId={}", userId);

        List<UserInventory> inventory = steamInventoryService.getMarketableInventory(userId);
        return Result.success(inventory);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return Long 用户ID
     */
    @GetMapping("/analysis")
    public Result<?> getInventoryAnalysis() {
        return Result.success(marketAnalyticsService.getInventoryAnalysis(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }

        // 从CustomUserDetails中获取用户ID
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }

        throw new RuntimeException("获取用户ID失败");
    }
}
