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
import java.util.stream.Collectors;

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
    private final BuyOrderMapper buyOrderMapper;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final FavoriteMapper favoriteMapper;
    private final MessageMapper messageMapper;
    private final PlayerShowCommentMapper playerShowCommentMapper;
    private final PlayerShowLikeMapper playerShowLikeMapper;
    private final SteamSyncTaskMapper steamSyncTaskMapper;

    public List<User> getAllUsers() { return userMapper.selectList(null); }

    public Map<String, Object> getUsers(Integer page, Integer size, String keyword, String role, Integer status) {
        return pageOf(userMapper.selectAll().stream()
                .filter(u -> matchesUser(u, keyword, role, status))
                .collect(Collectors.toList()), page, size);
    }

    public Map<String, Object> getUserOverview(Long userId) {
        User user = requireUser(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", user);
        data.put("wallet", walletMapper.selectByUserId(userId));
        data.put("inventoryCount", userInventoryMapper.countByUserId(userId));
        data.put("inventories", userInventoryMapper.selectByUserId(userId).stream().limit(8).toList());
        data.put("buyOrders", buyOrderMapper.selectByUserId(userId).stream().limit(8).toList());
        data.put("sellOrders", sellOrderMapper.selectByUserId(userId).stream().limit(8).toList());
        data.put("tradeOrders", tradeOrderMapper.selectByUserId(userId).stream().limit(8).toList());
        data.put("favorites", favoriteMapper.selectAll(userId, null, null, 0, 8));
        data.put("messageCount", messageMapper.countMessages(userId, null));
        data.put("unreadMessages", messageMapper.countUnread(userId, null));
        data.put("playerShows", playerShowMapper.selectByUserId(userId).stream().limit(8).toList());
        return data;
    }

    public User getUserById(Long userId) { return userMapper.selectById(userId); }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        requireUser(user.getId());
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, Integer status) {
        User user = requireUser(userId);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        requireUser(userId);
        return userMapper.deleteById(userId) > 0;
    }

    public List<TradeOrder> getAllOrders() { return tradeOrderMapper.selectList(null); }

    public Map<String, Object> getTradeOrders(Integer page, Integer size, Integer status, Long userId, String keyword) {
        return pageOf(tradeOrderMapper.selectList(null).stream()
                .filter(o -> status == null || Objects.equals(o.getStatus(), status))
                .filter(o -> userId == null || Objects.equals(o.getBuyerId(), userId) || Objects.equals(o.getSellerId(), userId))
                .filter(o -> blank(keyword) || contains(o.getOrderNo(), keyword) || contains(o.getTradeOfferId(), keyword)
                        || contains(o.getDeliveryStage(), keyword) || Objects.toString(o.getId(), "").contains(keyword))
                .collect(Collectors.toList()), page, size);
    }

    public List<TradeOrder> getOrdersByStatus(Integer status) { return tradeOrderMapper.selectByStatus(status); }

    public TradeOrder getOrderById(Long orderId) { return tradeOrderService.getOrderById(orderId); }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        return tradeOrderService.cancelOrder(orderId, order.getBuyerId());
    }

    public List<Item> getAllItems() { return itemMapper.selectAll(); }

    public Map<String, Object> getItems(Integer page, Integer size, String keyword, String category, Integer isActive) {
        return pageOf(itemMapper.selectAll().stream()
                .filter(i -> blank(category) || eq(i.getCategory(), category) || eq(i.getSubCategory(), category))
                .filter(i -> isActive == null || Objects.equals(i.getIsActive(), isActive))
                .filter(i -> blank(keyword) || contains(i.getName(), keyword) || contains(i.getNameCn(), keyword)
                        || contains(i.getItemId(), keyword) || contains(i.getSteamMarketHashName(), keyword))
                .collect(Collectors.toList()), page, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public Item addItem(Item item) {
        if (item.getIsActive() == null) item.setIsActive(1);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        return item;
    }

    @Transactional(rollbackFor = Exception.class)
    public Item updateItem(Item item) {
        if (itemMapper.selectById(item.getId()) == null) throw new RuntimeException("饰品不存在");
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return itemMapper.selectById(item.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(Long itemId) { return itemMapper.deleteById(itemId) > 0; }

    public Map<String, Object> getInventories(Integer page, Integer size, Long userId, Long itemId,
                                              Integer status, Integer isMarketable, String keyword) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(userInventoryMapper.selectAllForAdmin(userId, itemId, status, isMarketable, keyword, offset, s),
                userInventoryMapper.countAllForAdmin(userId, itemId, status, isMarketable, keyword), p, s);
    }

    public UserInventory getInventoryById(Long inventoryId) { return userInventoryMapper.selectById(inventoryId); }

    @Transactional(rollbackFor = Exception.class)
    public boolean fixInventoryItemMapping(Long inventoryId, Long itemId) {
        UserInventory inventory = userInventoryMapper.selectById(inventoryId);
        if (inventory == null) throw new RuntimeException("库存记录不存在");
        if (itemMapper.selectById(itemId) == null) throw new RuntimeException("饰品不存在");
        inventory.setItemId(itemId);
        inventory.setUpdatedAt(LocalDateTime.now());
        return userInventoryMapper.updateById(inventory) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanAbnormalInventories() {
        int count = 0;
        for (UserInventory inventory : userInventoryMapper.selectWithNullItemId()) {
            count += userInventoryMapper.deleteById(inventory.getId());
        }
        return count;
    }

    public Map<String, Object> getBuyOrders(Integer page, Integer size, Integer status, Long userId, String keyword) {
        buyOrderMapper.expireOldActiveOrders();
        return pageOf(buyOrderMapper.selectAll().stream()
                .filter(o -> status == null || Objects.equals(o.getStatus(), status))
                .filter(o -> userId == null || Objects.equals(o.getUserId(), userId))
                .filter(o -> blank(keyword) || Objects.toString(o.getId(), "").contains(keyword)
                        || Objects.toString(o.getUserId(), "").contains(keyword)
                        || (o.getItem() != null && (contains(o.getItem().getName(), keyword) || contains(o.getItem().getNameCn(), keyword))))
                .collect(Collectors.toList()), page, size);
    }

    public BuyOrder getBuyOrderById(Long orderId) {
        BuyOrder order = buyOrderMapper.selectById(orderId);
        if (order != null && order.getItemId() != null) order.setItem(itemMapper.selectById(order.getItemId()));
        if (order != null && order.getUserId() != null) order.setUser(userMapper.selectById(order.getUserId()));
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBuyOrder(Long orderId) {
        BuyOrder order = buyOrderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("求购单不存在");
        if (!Objects.equals(order.getStatus(), BuyOrder.STATUS_ACTIVE)) throw new RuntimeException("当前求购单状态不允许取消");
        return buyOrderMapper.updateStatus(orderId, BuyOrder.STATUS_CANCELLED) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireBuyOrders() { return buyOrderMapper.expireOldActiveOrders(); }

    public Map<String, Object> getSellOrders(Integer page, Integer size, Integer status, Long userId, String keyword) {
        Map<String, Object> result = pageOf((status == null ? sellOrderMapper.selectAll() : sellOrderMapper.selectByStatus(status)).stream()
                .filter(o -> userId == null || Objects.equals(o.getUserId(), userId))
                .filter(o -> blank(keyword) || Objects.toString(o.getId(), "").contains(keyword)
                        || Objects.toString(o.getUserId(), "").contains(keyword)
                        || Objects.toString(o.getInventoryId(), "").contains(keyword)
                        || Objects.toString(o.getItemId(), "").contains(keyword))
                .collect(Collectors.toList()), page, size);
        ((List<?>) result.get("list")).forEach(o -> enrichSellOrder((SellOrder) o));
        return result;
    }

    public SellOrder getSellOrderById(Long orderId) {
        SellOrder order = sellOrderMapper.selectById(orderId);
        enrichSellOrder(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSellOrder(Long orderId) {
        SellOrder order = sellOrderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("出售单不存在");
        if (!Objects.equals(order.getStatus(), SellOrder.STATUS_ON_SALE)) throw new RuntimeException("当前出售单状态不允许取消");
        sellOrderMapper.updateStatus(orderId, SellOrder.STATUS_CANCELLED);
        if (order.getInventoryId() != null) userInventoryMapper.updateStatus(order.getInventoryId(), UserInventory.STATUS_NORMAL);
        return true;
    }

    public Map<String, Object> getWallets(Integer page, Integer size, Long userId, String keyword) {
        return pageOf(walletMapper.selectAll().stream()
                .filter(w -> userId == null || Objects.equals(w.getUserId(), userId))
                .filter(w -> matchesWallet(w, keyword))
                .collect(Collectors.toList()), page, size);
    }

    public Wallet getWalletByUserId(Long userId) {
        requireUser(userId);
        return walletMapper.selectByUserId(userId);
    }

    public Map<String, Object> getWalletTransactions(Integer page, Integer size, Long userId, Integer type, String orderNo) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(walletTransactionMapper.selectAllForAdmin(userId, type, orderNo, offset, s),
                walletTransactionMapper.countAllForAdmin(userId, type, orderNo), p, s);
    }

    public Map<String, Object> getWithdrawalsPage(Integer page, Integer size, Integer status, Long userId, String keyword) {
        return pageOf((status == null ? withdrawalMapper.selectAll() : withdrawalMapper.selectByStatus(status)).stream()
                .filter(w -> userId == null || Objects.equals(w.getUserId(), userId))
                .filter(w -> blank(keyword) || Objects.toString(w.getId(), "").contains(keyword)
                        || Objects.toString(w.getUserId(), "").contains(keyword)
                        || contains(w.getUsername(), keyword) || contains(w.getBankName(), keyword)
                        || contains(w.getAccountName(), keyword))
                .collect(Collectors.toList()), page, size);
    }

    public List<Withdrawal> getWithdrawals(Integer status) {
        return status == null ? withdrawalMapper.selectAll() : withdrawalMapper.selectByStatus(status);
    }

    public Withdrawal getWithdrawalById(Long withdrawalId) { return withdrawalMapper.selectById(withdrawalId); }

    @Transactional(rollbackFor = Exception.class)
    public boolean auditWithdrawal(Long withdrawalId, Integer status, String reason) {
        if (withdrawalMapper.selectById(withdrawalId) == null) throw new RuntimeException("提现记录不存在");
        return withdrawalMapper.updateStatus(withdrawalId, status, reason) > 0;
    }

    public Map<String, Object> getFavorites(Integer page, Integer size, Long userId, Integer type, Long targetId) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(favoriteMapper.selectAll(userId, type, targetId, offset, s),
                favoriteMapper.countAll(userId, type, targetId), p, s);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFavorite(Long favoriteId) { return favoriteMapper.deleteById(favoriteId) > 0; }

    public Map<String, Object> getMessages(Integer page, Integer size, Long userId, Integer type, Integer status, String keyword) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(messageMapper.selectAllForAdmin(userId, type, status, keyword, offset, s),
                messageMapper.countAllForAdmin(userId, type, status, keyword), p, s);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markMessageRead(Long messageId) {
        if (messageMapper.selectById(messageId) == null) throw new RuntimeException("消息不存在");
        messageMapper.markAsRead(messageId);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMessages(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) messageMapper.batchDelete(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public Message sendSystemMessage(Message message) {
        if (message.getUserId() == null) throw new RuntimeException("接收用户不能为空");
        requireUser(message.getUserId());
        if (blank(message.getTitle())) message.setTitle("系统通知");
        if (blank(message.getContent())) throw new RuntimeException("消息内容不能为空");
        message.setType(Message.TYPE_SYSTEM);
        message.setSubType(message.getSubType() == null ? 3 : message.getSubType());
        message.setStatus(0);
        message.setSenderName("系统");
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.createMessage(message);
        return message;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<User> users = userMapper.selectList(null);
        List<Item> items = itemMapper.selectAll();
        List<BuyOrder> buyOrders = buyOrderMapper.selectAll();
        List<SellOrder> sellOrders = sellOrderMapper.selectAll();
        List<TradeOrder> orders = tradeOrderMapper.selectList(null);
        stats.put("totalUsers", users.size());
        stats.put("activeUsers", users.stream().filter(u -> Objects.equals(u.getStatus(), User.STATUS_ENABLED)).count());
        stats.put("adminUsers", users.stream().filter(u -> u.getUserLevel() != null && u.getUserLevel() >= User.LEVEL_ADMIN).count());
        stats.put("totalItems", items.size());
        stats.put("activeItems", items.stream().filter(i -> Objects.equals(i.getIsActive(), 1)).count());
        stats.put("abnormalInventories", userInventoryMapper.selectWithNullItemId().size());
        stats.put("activeBuyOrders", buyOrders.stream().filter(o -> Objects.equals(o.getStatus(), BuyOrder.STATUS_ACTIVE)).count());
        stats.put("activeSellOrders", sellOrders.stream().filter(o -> Objects.equals(o.getStatus(), SellOrder.STATUS_ON_SALE)).count());
        stats.put("totalOrders", orders.size());
        stats.put("completedOrders", orders.stream().filter(o -> Objects.equals(o.getStatus(), TradeOrder.STATUS_COMPLETED)).count());
        stats.put("pendingOrders", orders.stream().filter(o -> Objects.equals(o.getStatus(), TradeOrder.STATUS_PENDING)
                || Objects.equals(o.getStatus(), TradeOrder.STATUS_PAID)).count());
        stats.put("pendingWithdrawals", withdrawalMapper.selectByStatus(0).size());
        stats.put("totalTradeAmount", orders.stream()
                .filter(o -> Objects.equals(o.getStatus(), TradeOrder.STATUS_COMPLETED))
                .map(TradeOrder::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("todayOrders", orders.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(todayStart)).count());
        stats.put("todayTradeAmount", orders.stream()
                .filter(o -> Objects.equals(o.getStatus(), TradeOrder.STATUS_COMPLETED))
                .filter(o -> o.getCompletedAt() != null && !o.getCompletedAt().isBefore(todayStart))
                .map(TradeOrder::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        return stats;
    }

    public Map<String, Object> getTradeTrend(Integer days) {
        int d = days == null || days < 1 ? 7 : Math.min(days, 60);
        List<TradeOrder> orders = tradeOrderMapper.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (LocalDate date = LocalDate.now().minusDays(d - 1L); !date.isAfter(LocalDate.now()); date = date.plusDays(1)) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            dates.add(date.format(formatter));
            orderCounts.add((int) orders.stream().filter(o -> o.getCreatedAt() != null)
                    .filter(o -> !o.getCreatedAt().isBefore(start) && o.getCreatedAt().isBefore(end)).count());
            amounts.add(orders.stream().filter(o -> Objects.equals(o.getStatus(), TradeOrder.STATUS_COMPLETED))
                    .filter(o -> o.getCompletedAt() != null)
                    .filter(o -> !o.getCompletedAt().isBefore(start) && o.getCompletedAt().isBefore(end))
                    .map(TradeOrder::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        result.put("dates", dates);
        result.put("orderCounts", orderCounts);
        result.put("amounts", amounts);
        return result;
    }

    public Map<String, Object> getUserGrowth(Integer days) {
        int d = days == null || days < 1 ? 7 : Math.min(days, 60);
        List<User> users = userMapper.selectList(null);
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> newUsers = new ArrayList<>();
        List<Integer> totalUsers = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (LocalDate date = LocalDate.now().minusDays(d - 1L); !date.isAfter(LocalDate.now()); date = date.plusDays(1)) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            dates.add(date.format(formatter));
            newUsers.add((int) users.stream().filter(u -> u.getCreatedAt() != null)
                    .filter(u -> !u.getCreatedAt().isBefore(start) && u.getCreatedAt().isBefore(end)).count());
            totalUsers.add((int) users.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isBefore(end)).count());
        }
        result.put("dates", dates);
        result.put("newUsers", newUsers);
        result.put("totalUsers", totalUsers);
        return result;
    }

    public List<Banner> getAllBanners() { return bannerMapper.selectAll(); }

    @Transactional(rollbackFor = Exception.class)
    public Banner addBanner(Banner banner) {
        if (banner.getStatus() == null) banner.setStatus(1);
        if (banner.getSortOrder() == null) banner.setSortOrder(0);
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        bannerMapper.insert(banner);
        return banner;
    }

    @Transactional(rollbackFor = Exception.class)
    public Banner updateBanner(Banner banner) {
        banner.setUpdatedAt(LocalDateTime.now());
        bannerMapper.update(banner);
        return bannerMapper.selectById(banner.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBanner(Long bannerId) { return bannerMapper.deleteById(bannerId) > 0; }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateBannerStatus(Long bannerId, Integer status) {
        Banner banner = bannerMapper.selectById(bannerId);
        if (banner == null) throw new RuntimeException("轮播图不存在");
        banner.setStatus(status);
        banner.setUpdatedAt(LocalDateTime.now());
        return bannerMapper.update(banner) > 0;
    }

    public List<News> getAllNews() { return newsMapper.selectAllForAdmin(); }

    @Transactional(rollbackFor = Exception.class)
    public News createNews(News news) {
        if (blank(news.getTitle())) throw new RuntimeException("资讯标题不能为空");
        if (news.getStatus() == null) news.setStatus(1);
        if (news.getViews() == null) news.setViews(0);
        if (blank(news.getAuthor())) news.setAuthor("管理员");
        if (blank(news.getSource())) news.setSource("后台发布");
        news.setCreatedAt(LocalDateTime.now());
        newsMapper.insert(news);
        return newsMapper.selectById(news.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public News updateNews(Long newsId, News news) {
        if (newsMapper.selectById(newsId) == null) throw new RuntimeException("资讯不存在");
        news.setId(newsId);
        newsMapper.updateByAdmin(news);
        return newsMapper.selectById(newsId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean auditNews(Long newsId, Integer status, String reason) {
        if (newsMapper.selectById(newsId) == null) throw new RuntimeException("资讯不存在");
        return newsMapper.updateStatus(newsId, status, reason) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNews(Long newsId) { return newsMapper.deleteById(newsId) > 0; }

    public List<PlayerShow> getAllPlayerShows() { return playerShowMapper.selectAll(); }

    public Map<String, Object> getPlayerShows(Integer page, Integer size, Long userId, String keyword) {
        return pageOf(playerShowMapper.selectAll().stream()
                .filter(s -> userId == null || Objects.equals(s.getUserId(), userId))
                .filter(s -> blank(keyword) || contains(s.getDescription(), keyword) || contains(s.getUsername(), keyword))
                .collect(Collectors.toList()), page, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlayerShow(Long showId) { return playerShowMapper.deleteById(showId) > 0; }

    public Map<String, Object> getPlayerShowComments(Integer page, Integer size, Long showId, Long userId, String keyword) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(playerShowCommentMapper.selectAllForAdmin(showId, userId, keyword, offset, s),
                playerShowCommentMapper.countAllForAdmin(showId, userId, keyword), p, s);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlayerShowComment(Long commentId) { return playerShowCommentMapper.deleteById(commentId) > 0; }

    public Map<String, Object> getPlayerShowLikes(Integer page, Integer size, Long showId, Long userId) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(playerShowLikeMapper.selectAll(showId, userId, offset, s),
                playerShowLikeMapper.countAll(showId, userId), p, s);
    }

    public Map<String, Object> getSteamSyncTasks(Integer page, Integer size, String taskType, String status) {
        int p = safePage(page), s = safeSize(size), offset = (p - 1) * s;
        return pageResult(steamSyncTaskMapper.selectAll(taskType, status, offset, s),
                steamSyncTaskMapper.countAll(taskType, status), p, s);
    }

    public SteamSyncTask getLatestSteamSyncTask(String taskType) {
        return steamSyncTaskMapper.selectLatestByTaskType(taskType);
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanCancelledSellOrders() { return sellOrderMapper.deleteByStatus(SellOrder.STATUS_CANCELLED); }

    @Transactional(rollbackFor = Exception.class)
    public int cleanCancelledTradeOrders() { return tradeOrderMapper.deleteByStatus(TradeOrder.STATUS_CANCELLED); }

    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredSellOrders() { return sellOrderMapper.deleteExpired(); }

    @Transactional(rollbackFor = Exception.class)
    public int cleanOldCompletedOrders(int days) { return tradeOrderMapper.deleteOldCompletedOrders(days); }

    @Transactional(rollbackFor = Exception.class)
    public int cleanSoldInventory() { return userInventoryMapper.deleteByStatus(UserInventory.STATUS_SOLD); }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> cleanAllUselessData() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("cancelledSellOrders", cleanCancelledSellOrders());
        result.put("cancelledTradeOrders", cleanCancelledTradeOrders());
        result.put("expiredSellOrders", cleanExpiredSellOrders());
        result.put("oldCompletedOrders", cleanOldCompletedOrders(30));
        result.put("soldInventory", cleanSoldInventory());
        result.put("abnormalInventory", cleanAbnormalInventories());
        result.put("total", result.values().stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }

    private void enrichSellOrder(SellOrder order) {
        if (order == null) return;
        if (order.getItemId() != null) order.setItem(itemMapper.selectById(order.getItemId()));
        if (order.getUserId() != null) order.setUser(userMapper.selectById(order.getUserId()));
        if (order.getInventoryId() != null) order.setInventory(userInventoryMapper.selectById(order.getInventoryId()));
    }

    private Map<String, Object> pageOf(List<?> source, Integer page, Integer size) {
        int p = safePage(page), s = safeSize(size);
        int from = Math.min((p - 1) * s, source.size());
        int to = Math.min(from + s, source.size());
        return pageResult(source.subList(from, to), source.size(), p, s);
    }

    private Map<String, Object> pageResult(List<?> list, long total, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private int safePage(Integer page) { return page == null || page < 1 ? 1 : page; }

    private int safeSize(Integer size) { return size == null || size < 1 ? 20 : Math.min(size, 100); }

    private boolean matchesUser(User user, String keyword, String role, Integer status) {
        if (status != null && !Objects.equals(user.getStatus(), status)) return false;
        if (!blank(role)) {
            boolean admin = "admin".equalsIgnoreCase(role);
            boolean normal = "user".equalsIgnoreCase(role) || "normal".equalsIgnoreCase(role);
            if (admin && (user.getUserLevel() == null || user.getUserLevel() < User.LEVEL_ADMIN)) return false;
            if (normal && user.getUserLevel() != null && user.getUserLevel() >= User.LEVEL_ADMIN) return false;
        }
        return blank(keyword) || contains(user.getUsername(), keyword) || contains(user.getEmail(), keyword)
                || contains(user.getPhone(), keyword) || contains(user.getSteamId(), keyword);
    }

    private boolean matchesWallet(Wallet wallet, String keyword) {
        if (blank(keyword)) return true;
        User user = wallet.getUserId() == null ? null : userMapper.selectById(wallet.getUserId());
        return Objects.toString(wallet.getUserId(), "").contains(keyword)
                || (user != null && (contains(user.getUsername(), keyword) || contains(user.getEmail(), keyword)));
    }

    private boolean contains(String value, String keyword) {
        return value != null && keyword != null
                && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean eq(String value, String expected) {
        return value != null && expected != null && value.equalsIgnoreCase(expected);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
