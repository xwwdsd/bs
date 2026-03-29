package com.cs2trade.service.impl;

import com.cs2trade.entity.*;
import com.cs2trade.mapper.*;
import com.cs2trade.service.AdminService;
import com.cs2trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员服务实现类
 * 实现后台管理相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final ItemMapper itemMapper;
    private final TradeOrderService tradeOrderService;
    private final BannerMapper bannerMapper;
    private final NewsMapper newsMapper;
    private final PlayerShowMapper playerShowMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final SellOrderMapper sellOrderMapper;
    private final UserInventoryMapper userInventoryMapper;

    // ==================== 用户管理 ====================

    @Override
    public List<User> getAllUsers() {
        log.info("获取所有用户列表");
        return userMapper.selectList(null);
    }

    @Override
    public Map<String, Object> getUsers(Integer page, Integer size, String keyword, String role) {
        log.info("获取用户列表(分页): page={}, size={}, keyword={}, role={}", page, size, keyword, role);
        
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectByPage(keyword, role, offset, size);
        long total = userMapper.countByCondition(keyword, role);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return result;
    }

    @Override
    public User getUserById(Long userId) {
        log.info("获取用户详情: userId={}", userId);
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        log.info("更新用户信息: userId={}", user.getId());
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, Integer status) {
        log.info("更新用户状态: userId={}, status={}", userId, status);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        log.info("删除用户: userId={}", userId);
        return userMapper.deleteById(userId) > 0;
    }

    // ==================== 订单管理 ====================

    @Override
    public List<TradeOrder> getAllOrders() {
        log.info("获取所有订单");
        return tradeOrderMapper.selectList(null);
    }

    @Override
    public List<TradeOrder> getOrdersByStatus(Integer status) {
        log.info("根据状态获取订单: status={}", status);
        return tradeOrderMapper.selectByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId) {
        log.info("管理员取消订单: orderId={}", orderId);
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 调用订单服务取消订单
        return tradeOrderService.cancelOrder(orderId, order.getBuyerId());
    }

    // ==================== 饰品管理 ====================

    @Override
    public List<Item> getAllItems() {
        log.info("获取所有饰品");
        return itemMapper.selectAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Item addItem(Item item) {
        log.info("添加饰品: {}", item.getName());
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Item updateItem(Item item) {
        log.info("更新饰品: id={}", item.getId());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(Long itemId) {
        log.info("删除饰品: itemId={}", itemId);
        return itemMapper.deleteById(itemId) > 0;
    }

    // ==================== 数据统计 ====================

    @Override
    public Map<String, Object> getStatistics() {
        log.info("获取统计数据");
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        List<User> users = userMapper.selectList(null);
        stats.put("totalUsers", users.size());
        stats.put("activeUsers", users.stream().filter(u -> u.getStatus() == 1).count());

        // 订单统计
        List<TradeOrder> orders = tradeOrderMapper.selectList(null);
        stats.put("totalOrders", orders.size());
        stats.put("completedOrders", orders.stream().filter(o -> o.getStatus() == 4).count());
        stats.put("pendingOrders", orders.stream().filter(o -> o.getStatus() == 0 || o.getStatus() == 2).count());

        // 交易金额统计
        BigDecimal totalTradeAmount = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .map(TradeOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalTradeAmount", totalTradeAmount);

        // 饰品统计
        List<Item> items = itemMapper.selectAll();
        stats.put("totalItems", items.size());

        // 今日数据
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        long todayOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(todayStart))
                .count();
        stats.put("todayOrders", todayOrders);

        BigDecimal todayTradeAmount = orders.stream()
                .filter(o -> o.getStatus() == 4)
                .filter(o -> o.getCompletedAt() != null && o.getCompletedAt().isAfter(todayStart))
                .map(TradeOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayTradeAmount", todayTradeAmount);

        return stats;
    }

    @Override
    public Map<String, Object> getTradeTrend(Integer days) {
        log.info("获取交易趋势: days={}", days);
        Map<String, Object> result = new HashMap<>();

        List<String> dates = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date.format(formatter));

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            // 统计当日订单
            List<TradeOrder> dayOrders = tradeOrderMapper.selectList(null);
            long count = dayOrders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .filter(o -> !o.getCreatedAt().isBefore(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .count();
            orderCounts.add((int) count);

            // 统计当日交易额
            BigDecimal dayAmount = dayOrders.stream()
                    .filter(o -> o.getStatus() == 4)
                    .filter(o -> o.getCompletedAt() != null)
                    .filter(o -> !o.getCompletedAt().isBefore(dayStart) && o.getCompletedAt().isBefore(dayEnd))
                    .map(TradeOrder::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.add(dayAmount);
        }

        result.put("dates", dates);
        result.put("orderCounts", orderCounts);
        result.put("amounts", amounts);

        return result;
    }

    @Override
    public Map<String, Object> getUserGrowth(Integer days) {
        log.info("获取用户增长: days={}", days);
        Map<String, Object> result = new HashMap<>();

        List<String> dates = new ArrayList<>();
        List<Integer> newUsers = new ArrayList<>();
        List<Integer> totalUsers = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        List<User> allUsers = userMapper.selectList(null);

        int cumulativeUsers = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dates.add(date.format(formatter));

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            // 统计当日新增用户
            long count = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null)
                    .filter(u -> !u.getCreatedAt().isBefore(dayStart) && u.getCreatedAt().isBefore(dayEnd))
                    .count();
            newUsers.add((int) count);

            // 累计用户
            cumulativeUsers += count;
            totalUsers.add(cumulativeUsers);
        }

        result.put("dates", dates);
        result.put("newUsers", newUsers);
        result.put("totalUsers", totalUsers);

        return result;
    }

    // ==================== Banner管理 ====================

    @Override
    public List<Banner> getAllBanners() {
        return bannerMapper.selectAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Banner addBanner(Banner banner) {
        log.info("添加Banner: {}", banner.getTitle());
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Banner updateBanner(Banner banner) {
        banner.setUpdatedAt(LocalDateTime.now());
        bannerMapper.update(banner);
        return banner;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBanner(Long bannerId) {
        return bannerMapper.deleteById(bannerId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBannerStatus(Long bannerId, Integer status) {
        Banner banner = bannerMapper.selectById(bannerId);
        if (banner == null) {
            throw new RuntimeException("Banner不存在");
        }
        banner.setStatus(status);
        banner.setUpdatedAt(LocalDateTime.now());
        return bannerMapper.update(banner) > 0;
    }

    // ==================== 资讯管理 ====================

    @Override
    public List<News> getAllNews() {
        return newsMapper.selectAllForAdmin();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditNews(Long newsId, Integer status, String reason) {
        News news = newsMapper.selectById(newsId);
        if (news == null) {
            throw new RuntimeException("资讯不存在");
        }
        return newsMapper.updateStatus(newsId, status, reason) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNews(Long newsId) {
        return newsMapper.deleteById(newsId) > 0;
    }

    // ==================== 玩家秀管理 ====================

    @Override
    public List<PlayerShow> getAllPlayerShows() {
        return playerShowMapper.selectAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlayerShow(Long showId) {
        return playerShowMapper.deleteById(showId) > 0;
    }

    // ==================== 提现管理 ====================

    @Override
    public List<Withdrawal> getWithdrawals(Integer status) {
        if (status == null) {
            return withdrawalMapper.selectAll();
        }
        return withdrawalMapper.selectByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditWithdrawal(Long withdrawalId, Integer status, String reason) {
        Withdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new RuntimeException("提现记录不存在");
        }
        return withdrawalMapper.updateStatus(withdrawalId, status, reason) > 0;
    }

    // ==================== 数据清理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanCancelledSellOrders() {
        log.info("清理已取消的出售订单");
        return sellOrderMapper.deleteByStatus(SellOrder.STATUS_CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanCancelledTradeOrders() {
        log.info("清理已取消的交易订单");
        return tradeOrderMapper.deleteByStatus(TradeOrder.STATUS_CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredSellOrders() {
        log.info("清理过期的出售订单");
        return sellOrderMapper.deleteExpired();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanOldCompletedOrders(int days) {
        log.info("清理{}天前的已完成交易订单", days);
        return tradeOrderMapper.deleteOldCompletedOrders(days);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanSoldInventory() {
        log.info("清理已售出的库存记录");
        return userInventoryMapper.deleteByStatus(UserInventory.STATUS_SOLD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> cleanAllUselessData() {
        log.info("一键清理所有无用数据");
        Map<String, Integer> result = new HashMap<>();
        
        result.put("cancelledSellOrders", cleanCancelledSellOrders());
        result.put("cancelledTradeOrders", cleanCancelledTradeOrders());
        result.put("expiredSellOrders", cleanExpiredSellOrders());
        result.put("oldCompletedOrders", cleanOldCompletedOrders(30));
        result.put("soldInventory", cleanSoldInventory());
        
        int total = result.values().stream().mapToInt(Integer::intValue).sum();
        result.put("total", total);
        
        log.info("清理完成，共清理{}条数据", total);
        return result;
    }
}
