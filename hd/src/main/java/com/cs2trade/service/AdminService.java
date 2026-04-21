package com.cs2trade.service;

import com.cs2trade.entity.*;

import java.util.List;
import java.util.Map;

public interface AdminService {

    List<User> getAllUsers();

    Map<String, Object> getUsers(Integer page, Integer size, String keyword, String role, Integer status);

    Map<String, Object> getUserOverview(Long userId);

    boolean updateUser(User user);

    boolean updateUserStatus(Long userId, Integer status);

    User getUserById(Long userId);

    boolean deleteUser(Long userId);

    List<TradeOrder> getAllOrders();

    Map<String, Object> getTradeOrders(Integer page, Integer size, Integer status, Long userId, String keyword);

    List<TradeOrder> getOrdersByStatus(Integer status);

    TradeOrder getOrderById(Long orderId);

    boolean cancelOrder(Long orderId);

    List<Item> getAllItems();

    Map<String, Object> getItems(Integer page, Integer size, String keyword, String category, Integer isActive);

    Item addItem(Item item);

    Item updateItem(Item item);

    boolean deleteItem(Long itemId);

    Map<String, Object> getInventories(Integer page, Integer size, Long userId, Long itemId,
                                       Integer status, Integer isMarketable, String keyword);

    UserInventory getInventoryById(Long inventoryId);

    boolean fixInventoryItemMapping(Long inventoryId, Long itemId);

    int cleanAbnormalInventories();

    Map<String, Object> getBuyOrders(Integer page, Integer size, Integer status, Long userId, String keyword);

    BuyOrder getBuyOrderById(Long orderId);

    boolean cancelBuyOrder(Long orderId);

    int expireBuyOrders();

    Map<String, Object> getSellOrders(Integer page, Integer size, Integer status, Long userId, String keyword);

    SellOrder getSellOrderById(Long orderId);

    boolean cancelSellOrder(Long orderId);

    Map<String, Object> getWallets(Integer page, Integer size, Long userId, String keyword);

    Wallet getWalletByUserId(Long userId);

    Map<String, Object> getWalletTransactions(Integer page, Integer size, Long userId, Integer type, String orderNo);

    Map<String, Object> getWithdrawalsPage(Integer page, Integer size, Integer status, Long userId, String keyword);

    List<Withdrawal> getWithdrawals(Integer status);

    Withdrawal getWithdrawalById(Long withdrawalId);

    boolean auditWithdrawal(Long withdrawalId, Integer status, String reason);

    Map<String, Object> getFavorites(Integer page, Integer size, Long userId, Integer type, Long targetId);

    boolean deleteFavorite(Long favoriteId);

    Map<String, Object> getMessages(Integer page, Integer size, Long userId, Integer type, Integer status, String keyword);

    boolean markMessageRead(Long messageId);

    void deleteMessages(List<Long> ids);

    Message sendSystemMessage(Message message);

    Map<String, Object> getStatistics();

    Map<String, Object> getTradeTrend(Integer days);

    Map<String, Object> getUserGrowth(Integer days);

    List<Banner> getAllBanners();

    Banner addBanner(Banner banner);

    Banner updateBanner(Banner banner);

    boolean deleteBanner(Long bannerId);

    boolean updateBannerStatus(Long bannerId, Integer status);

    List<News> getAllNews();

    News createNews(News news);

    News updateNews(Long newsId, News news);

    boolean auditNews(Long newsId, Integer status, String reason);

    boolean deleteNews(Long newsId);

    List<PlayerShow> getAllPlayerShows();

    Map<String, Object> getPlayerShows(Integer page, Integer size, Long userId, String keyword);

    boolean deletePlayerShow(Long showId);

    Map<String, Object> getPlayerShowComments(Integer page, Integer size, Long showId, Long userId, String keyword);

    boolean deletePlayerShowComment(Long commentId);

    Map<String, Object> getPlayerShowLikes(Integer page, Integer size, Long showId, Long userId);

    Map<String, Object> getSteamSyncTasks(Integer page, Integer size, String taskType, String status);

    SteamSyncTask getLatestSteamSyncTask(String taskType);

    int cleanCancelledSellOrders();

    int cleanCancelledTradeOrders();

    int cleanExpiredSellOrders();

    int cleanOldCompletedOrders(int days);

    int cleanSoldInventory();

    Map<String, Integer> cleanAllUselessData();
}
