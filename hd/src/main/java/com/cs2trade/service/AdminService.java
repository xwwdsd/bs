package com.cs2trade.service;

import com.cs2trade.entity.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口
 * 定义后台管理相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface AdminService {

    // ==================== 用户管理 ====================

    /**
     * 获取所有用户
     *
     * @return List<User> 用户列表
     */
    List<User> getAllUsers();

    /**
     * 获取用户列表（分页）
     *
     * @param page 页码
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param role 角色筛选
     * @return Map<String, Object> 用户列表和总数
     */
    Map<String, Object> getUsers(Integer page, Integer size, String keyword, String role);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return boolean 是否成功
     */
    boolean updateUser(User user);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return boolean 是否成功
     */
    boolean updateUserStatus(Long userId, Integer status);

    /**
     * 根据ID获取用户详情
     *
     * @param userId 用户ID
     * @return User 用户信息
     */
    User getUserById(Long userId);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return boolean 是否成功
     */
    boolean deleteUser(Long userId);

    // ==================== 订单管理 ====================

    /**
     * 获取所有交易订单
     *
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> getAllOrders();

    /**
     * 根据状态获取订单
     *
     * @param status 订单状态
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> getOrdersByStatus(Integer status);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return boolean 是否成功
     */
    boolean cancelOrder(Long orderId);

    // ==================== 饰品管理 ====================

    /**
     * 获取所有饰品
     *
     * @return List<Item> 饰品列表
     */
    List<Item> getAllItems();

    /**
     * 添加饰品
     *
     * @param item 饰品信息
     * @return Item 添加的饰品
     */
    Item addItem(Item item);

    /**
     * 更新饰品
     *
     * @param item 饰品信息
     * @return Item 更新的饰品
     */
    Item updateItem(Item item);

    /**
     * 删除饰品
     *
     * @param itemId 饰品ID
     * @return boolean 是否成功
     */
    boolean deleteItem(Long itemId);

    // ==================== 数据统计 ====================

    /**
     * 获取统计数据
     *
     * @return Map<String, Object> 统计数据
     */
    Map<String, Object> getStatistics();

    /**
     * 获取交易趋势数据
     *
     * @param days 天数
     * @return Map<String, Object> 趋势数据
     */
    Map<String, Object> getTradeTrend(Integer days);

    /**
     * 获取用户增长数据
     *
     * @param days 天数
     * @return Map<String, Object> 增长数据
     */
    Map<String, Object> getUserGrowth(Integer days);

    // ==================== Banner管理 ====================

    List<Banner> getAllBanners();

    Banner addBanner(Banner banner);

    Banner updateBanner(Banner banner);

    boolean deleteBanner(Long bannerId);

    boolean updateBannerStatus(Long bannerId, Integer status);

    // ==================== 资讯管理 ====================

    List<News> getAllNews();

    boolean auditNews(Long newsId, Integer status, String reason);

    boolean deleteNews(Long newsId);

    // ==================== 玩家秀管理 ====================

    List<PlayerShow> getAllPlayerShows();

    boolean deletePlayerShow(Long showId);

    // ==================== 提现管理 ====================

    List<Withdrawal> getWithdrawals(Integer status);

    boolean auditWithdrawal(Long withdrawalId, Integer status, String reason);

    // ==================== 数据清理 ====================

    /**
     * 清理已取消的出售订单
     *
     * @return int 清理数量
     */
    int cleanCancelledSellOrders();

    /**
     * 清理已取消的交易订单
     *
     * @return int 清理数量
     */
    int cleanCancelledTradeOrders();

    /**
     * 清理过期的出售订单
     *
     * @return int 清理数量
     */
    int cleanExpiredSellOrders();

    /**
     * 清理旧的已完成交易订单
     *
     * @param days 保留天数
     * @return int 清理数量
     */
    int cleanOldCompletedOrders(int days);

    /**
     * 清理已售出的库存记录
     *
     * @return int 清理数量
     */
    int cleanSoldInventory();

    /**
     * 一键清理所有无用数据
     *
     * @return Map<String, Integer> 各类数据清理数量
     */
    Map<String, Integer> cleanAllUselessData();
}
