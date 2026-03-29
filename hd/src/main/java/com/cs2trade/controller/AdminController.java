package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.*;
import com.cs2trade.service.AdminService;
import com.cs2trade.service.SellOrderService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 处理后台管理相关的HTTP请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
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

    // ==================== 用户管理 ====================

    /**
     * 获取用户列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param role 角色筛选
     * @return Result<Map<String, Object>> 用户列表
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        log.info("获取用户列表: page={}, size={}, keyword={}, role={}", page, size, keyword, role);
        Map<String, Object> result = adminService.getUsers(page, size, keyword, role);
        return Result.success(result);
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param user 用户信息
     * @return Result<Boolean> 更新结果
     */
    @PutMapping("/users/{userId}")
    public Result<Boolean> updateUser(@PathVariable Long userId, @RequestBody User user) {
        log.info("更新用户信息: userId={}", userId);
        user.setId(userId);
        boolean success = adminService.updateUser(user);
        return Result.success("更新成功", success);
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return Result<User> 用户信息
     */
    @GetMapping("/users/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        log.info("获取用户详情: userId={}", userId);
        User user = adminService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return Result<Boolean> 更新结果
     */
    @PutMapping("/users/{userId}/status")
    public Result<Boolean> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        log.info("更新用户状态: userId={}, status={}", userId, status);
        boolean success = adminService.updateUserStatus(userId, status);
        return Result.success("更新成功", success);
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return Result<Boolean> 删除结果
     */
    @DeleteMapping("/users/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        log.info("删除用户: userId={}", userId);
        boolean success = adminService.deleteUser(userId);
        return Result.success("删除成功", success);
    }

    // ==================== 订单管理 ====================

    /**
     * 获取所有订单
     *
     * @return Result<List<TradeOrder>> 订单列表
     */
    @GetMapping("/orders")
    public Result<List<TradeOrder>> getAllOrders() {
        log.info("获取所有订单");
        List<TradeOrder> orders = adminService.getAllOrders();
        return Result.success(orders);
    }

    /**
     * 根据状态获取订单
     *
     * @param status 订单状态
     * @return Result<List<TradeOrder>> 订单列表
     */
    @GetMapping("/orders/by-status")
    public Result<List<TradeOrder>> getOrdersByStatus(@RequestParam Integer status) {
        log.info("根据状态获取订单: status={}", status);
        List<TradeOrder> orders = adminService.getOrdersByStatus(status);
        return Result.success(orders);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return Result<Boolean> 取消结果
     */
    @PostMapping("/orders/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        log.info("取消订单: orderId={}", orderId);
        boolean success = adminService.cancelOrder(orderId);
        return Result.success("订单已取消", success);
    }

    // ==================== 饰品管理 ====================

    /**
     * 获取所有饰品
     *
     * @return Result<List<Item>> 饰品列表
     */
    @GetMapping("/items")
    public Result<List<Item>> getAllItems() {
        log.info("获取所有饰品");
        List<Item> items = adminService.getAllItems();
        return Result.success(items);
    }

    /**
     * 添加饰品
     *
     * @param item 饰品信息
     * @return Result<Item> 添加的饰品
     */
    @PostMapping("/items")
    public Result<Item> addItem(@RequestBody Item item) {
        log.info("添加饰品: {}", item.getName());
        Item newItem = adminService.addItem(item);
        return Result.success("添加成功", newItem);
    }

    /**
     * 更新饰品
     *
     * @param itemId 饰品ID
     * @param item 饰品信息
     * @return Result<Item> 更新的饰品
     */
    @PutMapping("/items/{itemId}")
    public Result<Item> updateItem(@PathVariable Long itemId, @RequestBody Item item) {
        log.info("更新饰品: id={}", itemId);
        item.setId(itemId);
        Item updatedItem = adminService.updateItem(item);
        return Result.success("更新成功", updatedItem);
    }

    /**
     * 删除饰品
     *
     * @param itemId 饰品ID
     * @return Result<Boolean> 删除结果
     */
    @DeleteMapping("/items/{itemId}")
    public Result<Boolean> deleteItem(@PathVariable Long itemId) {
        log.info("删除饰品: itemId={}", itemId);
        boolean success = adminService.deleteItem(itemId);
        return Result.success("删除成功", success);
    }

    // ==================== 数据统计 ====================

    /**
     * 获取统计数据
     *
     * @return Result<Map<String, Object>> 统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        log.info("获取统计数据");
        Map<String, Object> stats = adminService.getStatistics();
        return Result.success(stats);
    }

    /**
     * 获取交易趋势
     *
     * @param days 天数
     * @return Result<Map<String, Object>> 趋势数据
     */
    @GetMapping("/statistics/trade-trend")
    public Result<Map<String, Object>> getTradeTrend(@RequestParam(defaultValue = "7") Integer days) {
        log.info("获取交易趋势: days={}", days);
        Map<String, Object> trend = adminService.getTradeTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取用户增长
     *
     * @param days 天数
     * @return Result<Map<String, Object>> 增长数据
     */
    @GetMapping("/statistics/user-growth")
    public Result<Map<String, Object>> getUserGrowth(@RequestParam(defaultValue = "7") Integer days) {
        log.info("获取用户增长: days={}", days);
        Map<String, Object> growth = adminService.getUserGrowth(days);
        return Result.success(growth);
    }

    // ==================== 资讯管理 ====================

    /**
     * 获取所有资讯
     */
    @GetMapping("/news")
    public Result<List<News>> getAllNews() {
        log.info("获取所有资讯");
        List<News> newsList = adminService.getAllNews();
        return Result.success(newsList);
    }

    /**
     * 审核资讯
     */
    @PutMapping("/news/{newsId}/audit")
    public Result<Boolean> auditNews(@PathVariable Long newsId, @RequestParam Integer status, @RequestParam(required = false) String reason) {
        log.info("审核资讯: id={}, status={}", newsId, status);
        boolean success = adminService.auditNews(newsId, status, reason);
        return Result.success("审核完成", success);
    }

    /**
     * 删除资讯
     */
    @DeleteMapping("/news/{newsId}")
    public Result<Boolean> deleteNews(@PathVariable Long newsId) {
        log.info("删除资讯: id={}", newsId);
        boolean success = adminService.deleteNews(newsId);
        return Result.success("删除成功", success);
    }

    // ==================== 玩家秀管理 ====================

    /**
     * 获取所有玩家秀
     */
    @GetMapping("/player-shows")
    public Result<List<PlayerShow>> getAllPlayerShows() {
        log.info("获取所有玩家秀");
        List<PlayerShow> shows = adminService.getAllPlayerShows();
        return Result.success(shows);
    }

    /**
     * 删除玩家秀
     */
    @DeleteMapping("/player-shows/{showId}")
    public Result<Boolean> deletePlayerShow(@PathVariable Long showId) {
        log.info("删除玩家秀: id={}", showId);
        boolean success = adminService.deletePlayerShow(showId);
        return Result.success("删除成功", success);
    }

    // ==================== 提现管理 ====================

    /**
     * 获取提现申请列表
     */
    @GetMapping("/withdrawals")
    public Result<List<Withdrawal>> getWithdrawals(@RequestParam(required = false) Integer status) {
        log.info("获取提现申请列表: status={}", status);
        List<Withdrawal> withdrawals = adminService.getWithdrawals(status);
        return Result.success(withdrawals);
    }

    /**
     * 审核提现申请
     */
    @PostMapping("/withdrawals/{withdrawalId}/audit")
    public Result<Boolean> auditWithdrawal(@PathVariable Long withdrawalId, @RequestParam Integer status, @RequestParam(required = false) String reason) {
        log.info("审核提现申请: id={}, status={}", withdrawalId, status);
        boolean success = adminService.auditWithdrawal(withdrawalId, status, reason);
        return Result.success("审核完成", success);
    }

    // ==================== 数据清理 ====================

    /**
     * 清理已取消的出售订单
     */
    @DeleteMapping("/clean/sell-orders/cancelled")
    public Result<Integer> cleanCancelledSellOrders() {
        log.info("清理已取消的出售订单");
        int count = adminService.cleanCancelledSellOrders();
        return Result.success("清理完成，共清理" + count + "条数据", count);
    }

    /**
     * 清理已取消的交易订单
     */
    @DeleteMapping("/clean/trade-orders/cancelled")
    public Result<Integer> cleanCancelledTradeOrders() {
        log.info("清理已取消的交易订单");
        int count = adminService.cleanCancelledTradeOrders();
        return Result.success("清理完成，共清理" + count + "条数据", count);
    }

    /**
     * 清理过期的出售订单
     */
    @DeleteMapping("/clean/sell-orders/expired")
    public Result<Integer> cleanExpiredSellOrders() {
        log.info("清理过期的出售订单");
        int count = adminService.cleanExpiredSellOrders();
        return Result.success("清理完成，共清理" + count + "条数据", count);
    }

    /**
     * 清理旧的已完成交易订单
     */
    @DeleteMapping("/clean/trade-orders/old-completed")
    public Result<Integer> cleanOldCompletedOrders(@RequestParam(defaultValue = "30") Integer days) {
        log.info("清理{}天前的已完成交易订单", days);
        int count = adminService.cleanOldCompletedOrders(days);
        return Result.success("清理完成，共清理" + count + "条数据", count);
    }

    /**
     * 清理已售出的库存记录
     */
    @DeleteMapping("/clean/inventory/sold")
    public Result<Integer> cleanSoldInventory() {
        log.info("清理已售出的库存记录");
        int count = adminService.cleanSoldInventory();
        return Result.success("清理完成，共清理" + count + "条数据", count);
    }

    /**
     * 一键清理所有无用数据
     */
    @DeleteMapping("/clean/all")
    public Result<Map<String, Integer>> cleanAllUselessData() {
        log.info("一键清理所有无用数据");
        Map<String, Integer> result = adminService.cleanAllUselessData();
        return Result.success("清理完成", result);
    }

    // ==================== 出售订单管理 ====================

    /**
     * 获取所有出售订单
     */
    @GetMapping("/sell-orders")
    public Result<List<SellOrder>> getAllSellOrders(@RequestParam(required = false) Integer status) {
        log.info("获取出售订单列表: status={}", status);
        List<SellOrder> orders;
        if (status != null) {
            orders = sellOrderService.getSellOrdersByStatus(status);
        } else {
            orders = sellOrderService.getAllSellOrders();
        }
        return Result.success(orders);
    }

    /**
     * 获取出售订单详情
     */
    @GetMapping("/sell-orders/{orderId}")
    public Result<SellOrder> getSellOrderById(@PathVariable Long orderId) {
        log.info("获取出售订单详情: orderId={}", orderId);
        SellOrder order = sellOrderService.getOrderById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 取消出售订单
     */
    @PostMapping("/sell-orders/{orderId}/cancel")
    public Result<Boolean> cancelSellOrder(@PathVariable Long orderId) {
        log.info("管理员取消出售订单: orderId={}", orderId);
        try {
            boolean success = sellOrderService.adminCancelSellOrder(orderId);
            return Result.success("取消成功", success);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
