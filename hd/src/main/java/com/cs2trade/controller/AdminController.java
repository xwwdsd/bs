package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.*;
import com.cs2trade.service.AdminService;
import com.cs2trade.service.MarketAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MarketAnalyticsService marketAnalyticsService;

    @GetMapping("/users")
    public Result<Map<String, Object>> getUsers(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(adminService.getUsers(page, size, keyword, role, status));
    }

    @GetMapping("/users/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = adminService.getUserById(userId);
        return user == null ? Result.error("用户不存在") : Result.success(user);
    }

    @GetMapping("/users/{userId}/overview")
    public Result<Map<String, Object>> getUserOverview(@PathVariable Long userId) {
        return Result.success(adminService.getUserOverview(userId));
    }

    @PutMapping("/users/{userId}")
    public Result<Boolean> updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setId(userId);
        return Result.success("更新成功", adminService.updateUser(user));
    }

    @PutMapping("/users/{userId}/status")
    public Result<Boolean> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        return Result.success("状态已更新", adminService.updateUserStatus(userId, status));
    }

    @DeleteMapping("/users/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        return Result.success("删除成功", adminService.deleteUser(userId));
    }

    @GetMapping("/items")
    public Result<Map<String, Object>> getItems(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "20") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) Integer isActive) {
        return Result.success(adminService.getItems(page, size, keyword, category, isActive));
    }

    @PostMapping("/items")
    public Result<Item> addItem(@RequestBody Item item) {
        return Result.success("添加成功", adminService.addItem(item));
    }

    @PutMapping("/items/{itemId}")
    public Result<Item> updateItem(@PathVariable Long itemId, @RequestBody Item item) {
        item.setId(itemId);
        return Result.success("更新成功", adminService.updateItem(item));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<Boolean> deleteItem(@PathVariable Long itemId) {
        return Result.success("删除成功", adminService.deleteItem(itemId));
    }

    @GetMapping("/inventory")
    public Result<Map<String, Object>> getInventories(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) Long itemId,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Integer isMarketable,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getInventories(page, size, userId, itemId, status, isMarketable, keyword));
    }

    @GetMapping("/inventory/{inventoryId}")
    public Result<UserInventory> getInventoryById(@PathVariable Long inventoryId) {
        UserInventory inventory = adminService.getInventoryById(inventoryId);
        return inventory == null ? Result.error("库存记录不存在") : Result.success(inventory);
    }

    @PutMapping("/inventory/{inventoryId}/item")
    public Result<Boolean> fixInventoryItemMapping(@PathVariable Long inventoryId, @RequestParam Long itemId) {
        return Result.success("映射已修复", adminService.fixInventoryItemMapping(inventoryId, itemId));
    }

    @DeleteMapping("/inventory/abnormal")
    public Result<Integer> cleanAbnormalInventories() {
        return Result.success("清理完成", adminService.cleanAbnormalInventories());
    }

    @GetMapping("/buy-orders")
    public Result<Map<String, Object>> getBuyOrders(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getBuyOrders(page, size, status, userId, keyword));
    }

    @GetMapping("/buy-orders/{orderId}")
    public Result<BuyOrder> getBuyOrderById(@PathVariable Long orderId) {
        BuyOrder order = adminService.getBuyOrderById(orderId);
        return order == null ? Result.error("求购单不存在") : Result.success(order);
    }

    @PostMapping("/buy-orders/{orderId}/cancel")
    public Result<Boolean> cancelBuyOrder(@PathVariable Long orderId) {
        return Result.success("求购单已取消", adminService.cancelBuyOrder(orderId));
    }

    @PostMapping("/buy-orders/expire")
    public Result<Integer> expireBuyOrders() {
        return Result.success("过期处理完成", adminService.expireBuyOrders());
    }

    @GetMapping("/sell-orders")
    public Result<Map<String, Object>> getSellOrders(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "20") Integer size,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getSellOrders(page, size, status, userId, keyword));
    }

    @GetMapping("/sell-orders/{orderId}")
    public Result<SellOrder> getSellOrderById(@PathVariable Long orderId) {
        SellOrder order = adminService.getSellOrderById(orderId);
        return order == null ? Result.error("出售单不存在") : Result.success(order);
    }

    @PostMapping("/sell-orders/{orderId}/cancel")
    public Result<Boolean> cancelSellOrder(@PathVariable Long orderId) {
        return Result.success("出售单已取消", adminService.cancelSellOrder(orderId));
    }

    @GetMapping("/orders")
    public Result<Map<String, Object>> getTradeOrders(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getTradeOrders(page, size, status, userId, keyword));
    }

    @GetMapping("/orders/by-status")
    public Result<List<TradeOrder>> getOrdersByStatus(@RequestParam Integer status) {
        return Result.success(adminService.getOrdersByStatus(status));
    }

    @GetMapping("/orders/{orderId}")
    public Result<TradeOrder> getTradeOrderById(@PathVariable Long orderId) {
        TradeOrder order = adminService.getOrderById(orderId);
        return order == null ? Result.error("交易订单不存在") : Result.success(order);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable Long orderId) {
        return Result.success("交易订单已取消", adminService.cancelOrder(orderId));
    }

    @GetMapping("/wallets")
    public Result<Map<String, Object>> getWallets(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getWallets(page, size, userId, keyword));
    }

    @GetMapping("/wallets/{userId}")
    public Result<Wallet> getWalletByUserId(@PathVariable Long userId) {
        Wallet wallet = adminService.getWalletByUserId(userId);
        return wallet == null ? Result.error("钱包不存在") : Result.success(wallet);
    }

    @GetMapping("/wallet-transactions")
    public Result<Map<String, Object>> getWalletTransactions(@RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "20") Integer size,
                                                             @RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) Integer type,
                                                             @RequestParam(required = false) String orderNo) {
        return Result.success(adminService.getWalletTransactions(page, size, userId, type, orderNo));
    }

    @GetMapping("/withdrawals")
    public Result<Map<String, Object>> getWithdrawals(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getWithdrawalsPage(page, size, status, userId, keyword));
    }

    @GetMapping("/withdrawals/{withdrawalId}")
    public Result<Withdrawal> getWithdrawalById(@PathVariable Long withdrawalId) {
        Withdrawal withdrawal = adminService.getWithdrawalById(withdrawalId);
        return withdrawal == null ? Result.error("提现记录不存在") : Result.success(withdrawal);
    }

    @PostMapping("/withdrawals/{withdrawalId}/audit")
    public Result<Boolean> auditWithdrawal(@PathVariable Long withdrawalId,
                                           @RequestParam Integer status,
                                           @RequestParam(required = false) String reason) {
        return Result.success("审核完成", adminService.auditWithdrawal(withdrawalId, status, reason));
    }

    @GetMapping("/favorites")
    public Result<Map<String, Object>> getFavorites(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) Integer type,
                                                    @RequestParam(required = false) Long targetId) {
        return Result.success(adminService.getFavorites(page, size, userId, type, targetId));
    }

    @DeleteMapping("/favorites/{favoriteId}")
    public Result<Boolean> deleteFavorite(@PathVariable Long favoriteId) {
        return Result.success("收藏记录已删除", adminService.deleteFavorite(favoriteId));
    }

    @GetMapping("/messages")
    public Result<Map<String, Object>> getMessages(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer size,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) Integer type,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getMessages(page, size, userId, type, status, keyword));
    }

    @PostMapping("/messages/{messageId}/read")
    public Result<Boolean> markMessageRead(@PathVariable Long messageId) {
        return Result.success("消息已标记为已读", adminService.markMessageRead(messageId));
    }

    @PostMapping("/messages/batch-delete")
    public Result<Boolean> deleteMessages(@RequestBody List<Long> ids) {
        adminService.deleteMessages(ids);
        return Result.success("消息已删除", true);
    }

    @PostMapping("/messages/system")
    public Result<Message> sendSystemMessage(@RequestBody Message message) {
        return Result.success("消息已发送", adminService.sendSystemMessage(message));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(adminService.getStatistics());
    }

    @PostMapping("/statistics/rebuild-market-data")
    public Result<String> rebuildMarketData(@RequestParam(required = false) Long itemId) {
        if (itemId == null) {
            marketAnalyticsService.rebuildAllMarketData();
            return Result.success("行情数据已重建", "all");
        }
        marketAnalyticsService.rebuildMarketData(itemId);
        return Result.success("行情数据已重建", String.valueOf(itemId));
    }

    @GetMapping("/statistics/trade-trend")
    public Result<Map<String, Object>> getTradeTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(adminService.getTradeTrend(days));
    }

    @GetMapping("/statistics/user-growth")
    public Result<Map<String, Object>> getUserGrowth(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(adminService.getUserGrowth(days));
    }

    @GetMapping("/news")
    public Result<List<News>> getAllNews() {
        return Result.success(adminService.getAllNews());
    }

    @PostMapping("/news")
    public Result<News> createNews(@RequestBody News news) {
        return Result.success("资讯已创建", adminService.createNews(news));
    }

    @PutMapping("/news/{newsId}")
    public Result<News> updateNews(@PathVariable Long newsId, @RequestBody News news) {
        return Result.success("资讯已更新", adminService.updateNews(newsId, news));
    }

    @PutMapping("/news/{newsId}/audit")
    public Result<Boolean> auditNews(@PathVariable Long newsId,
                                     @RequestParam Integer status,
                                     @RequestParam(required = false) String reason) {
        return Result.success("审核完成", adminService.auditNews(newsId, status, reason));
    }

    @DeleteMapping("/news/{newsId}")
    public Result<Boolean> deleteNews(@PathVariable Long newsId) {
        return Result.success("资讯已删除", adminService.deleteNews(newsId));
    }

    @GetMapping("/player-shows")
    public Result<Map<String, Object>> getPlayerShows(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getPlayerShows(page, size, userId, keyword));
    }

    @DeleteMapping("/player-shows/{showId}")
    public Result<Boolean> deletePlayerShow(@PathVariable Long showId) {
        return Result.success("玩家秀已删除", adminService.deletePlayerShow(showId));
    }

    @GetMapping("/player-show-comments")
    public Result<Map<String, Object>> getPlayerShowComments(@RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "20") Integer size,
                                                             @RequestParam(required = false) Long showId,
                                                             @RequestParam(required = false) Long userId,
                                                             @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getPlayerShowComments(page, size, showId, userId, keyword));
    }

    @DeleteMapping("/player-show-comments/{commentId}")
    public Result<Boolean> deletePlayerShowComment(@PathVariable Long commentId) {
        return Result.success("评论已删除", adminService.deletePlayerShowComment(commentId));
    }

    @GetMapping("/player-show-likes")
    public Result<Map<String, Object>> getPlayerShowLikes(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size,
                                                          @RequestParam(required = false) Long showId,
                                                          @RequestParam(required = false) Long userId) {
        return Result.success(adminService.getPlayerShowLikes(page, size, showId, userId));
    }

    @GetMapping("/sync/tasks")
    public Result<Map<String, Object>> getSteamSyncTasks(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "20") Integer size,
                                                         @RequestParam(required = false) String taskType,
                                                         @RequestParam(required = false) String status) {
        return Result.success(adminService.getSteamSyncTasks(page, size, taskType, status));
    }

    @GetMapping("/sync/tasks/latest")
    public Result<SteamSyncTask> getLatestSteamSyncTask(@RequestParam String taskType) {
        return Result.success(adminService.getLatestSteamSyncTask(taskType));
    }

    @DeleteMapping("/clean/sell-orders/cancelled")
    public Result<Integer> cleanCancelledSellOrders() {
        return Result.success("清理完成", adminService.cleanCancelledSellOrders());
    }

    @DeleteMapping("/clean/trade-orders/cancelled")
    public Result<Integer> cleanCancelledTradeOrders() {
        return Result.success("清理完成", adminService.cleanCancelledTradeOrders());
    }

    @DeleteMapping("/clean/sell-orders/expired")
    public Result<Integer> cleanExpiredSellOrders() {
        return Result.success("清理完成", adminService.cleanExpiredSellOrders());
    }

    @DeleteMapping("/clean/trade-orders/old-completed")
    public Result<Integer> cleanOldCompletedOrders(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success("清理完成", adminService.cleanOldCompletedOrders(days));
    }

    @DeleteMapping("/clean/inventory/sold")
    public Result<Integer> cleanSoldInventory() {
        return Result.success("清理完成", adminService.cleanSoldInventory());
    }

    @DeleteMapping("/clean/all")
    public Result<Map<String, Integer>> cleanAllUselessData() {
        return Result.success("清理完成", adminService.cleanAllUselessData());
    }
}
