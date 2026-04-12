package com.cs2trade.service.impl;

import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.InspectMetadataService;
import com.cs2trade.service.SellOrderService;
import com.cs2trade.service.SteamInventoryService;
import com.cs2trade.util.SteamApiClient;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 出售订单服务实现类
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellOrderServiceImpl implements SellOrderService {

    private final SellOrderMapper sellOrderMapper;
    private final UserInventoryMapper userInventoryMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SteamInventoryService steamInventoryService;
    private final InspectMetadataService inspectMetadataService;
    private final SteamApiClient steamApiClient;

    private static final String STEAM_REFERENCE_CURRENCY = "CNY";
    private static final String STEAM_REFERENCE_PRICE_SOURCE = "steam_market_priceoverview";
    private static final Pattern PRICE_TEXT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SellOrder createSellOrder(Long userId, Long inventoryId, BigDecimal price) {
        log.info("创建出售订单: userId={}, inventoryId={}, price={}", userId, inventoryId, price);

        // 验证参数
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("价格必须大于0");
        }

        // 获取库存信息
        UserInventory inventory = userInventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            throw new RuntimeException("库存物品不存在");
        }

        // 验证所有权
        if (!inventory.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该物品");
        }

        // 验证物品状态
        if (inventory.getStatus() != UserInventory.STATUS_NORMAL) {
            throw new RuntimeException("物品状态不可出售(可能已在售或已锁定)");
        }

        // 验证是否可交易
        if (inventory.getIsMarketable() != UserInventory.IS_MARKETABLE) {
            throw new RuntimeException("该物品暂不可出售");
        }

        // 获取物品 ID（如果 inventory.itemId 为 null，尝试根据名称匹配）
        Long itemId = inventory.getItemId();
        if (itemId == null && inventory.getName() != null) {
            // 尝试根据名称匹配物品
            Item matchedItem = matchItemByName(inventory.getName());
            if (matchedItem != null) {
                itemId = matchedItem.getId();
                // 更新 inventory 的 item_id
                inventory.setItemId(itemId);
                userInventoryMapper.updateById(inventory);
                log.info("根据名称匹配到物品：inventoryId={}, name={}, itemId={}", inventoryId, inventory.getName(), itemId);
            } else {
                // 未匹配到，使用未知饰品 ID
                Item unknownItem = getOrCreateUnknownItem();
                itemId = unknownItem.getId();
                inventory.setItemId(itemId);
                userInventoryMapper.updateById(inventory);
                log.warn("未匹配到物品，使用未知饰品：inventoryId={}, name={}, itemId={}", inventoryId, inventory.getName(), itemId);
            }
        }

        // 创建出售订单
        SellOrder order = new SellOrder();
        order.setUserId(userId);
        order.setInventoryId(inventoryId);
        order.setItemId(itemId);
        order.setPrice(price);
        order.setStatus(SellOrder.STATUS_ON_SALE);
        order.setIsResponseToBuy(SellOrder.RESPONSE_NO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认过期时间（例如7天后）
        order.setExpireTime(LocalDateTime.now().plusDays(7));

        sellOrderMapper.insert(order);

        // 更新库存状态为在售
        userInventoryMapper.updateStatus(inventoryId, UserInventory.STATUS_ON_SALE);

        log.info("出售订单创建成功: id={}", order.getId());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SellOrder createSellOrderByAssetId(Long userId, String assetId, BigDecimal price) {
        log.info("创建出售订单(通过AssetId): userId={}, assetId={}, price={}", userId, assetId, price);

        // 验证参数
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("价格必须大于0");
        }

        // 获取库存信息
        UserInventory inventory = userInventoryMapper.selectByUserIdAndAssetId(userId, assetId);
        if (inventory == null) {
            log.info("本地库存快照缺失，先执行一次 Steam 同步: userId={}, assetId={}", userId, assetId);
            steamInventoryService.syncInventory(userId);
            inventory = userInventoryMapper.selectByUserIdAndAssetId(userId, assetId);
        }
        if (inventory == null) {
            throw new RuntimeException("库存物品不存在，请先同步 Steam 库存后重试");
        }

        // 验证所有权
        if (!inventory.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该物品");
        }

        // 验证物品状态
        if (inventory.getStatus() != UserInventory.STATUS_NORMAL) {
            throw new RuntimeException("物品状态不可出售(可能已在售或已锁定)");
        }

        // 验证是否可交易
        if (inventory.getIsMarketable() != UserInventory.IS_MARKETABLE) {
            throw new RuntimeException("该物品暂不可出售");
        }

        // 创建出售订单
        SellOrder order = new SellOrder();
        order.setUserId(userId);
        order.setInventoryId(inventory.getId());
        order.setItemId(inventory.getItemId());
        order.setPrice(price);
        order.setStatus(SellOrder.STATUS_ON_SALE);
        order.setIsResponseToBuy(SellOrder.RESPONSE_NO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认过期时间（例如7天后）
        order.setExpireTime(LocalDateTime.now().plusDays(7));

        sellOrderMapper.insert(order);

        // 更新库存状态为在售
        userInventoryMapper.updateStatus(inventory.getId(), UserInventory.STATUS_ON_SALE);

        log.info("出售订单创建成功: id={}", order.getId());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSellOrder(Long orderId, Long userId) {
        log.info("取消出售订单: orderId={}, userId={}", orderId, userId);

        SellOrder order = sellOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 验证权限
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 验证状态
        if (order.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("订单状态不可取消");
        }

        // 更新订单状态
        sellOrderMapper.updateStatus(orderId, SellOrder.STATUS_CANCELLED);

        // 恢复库存状态
        userInventoryMapper.updateStatus(order.getInventoryId(), UserInventory.STATUS_NORMAL);

        log.info("出售订单取消成功: id={}", orderId);
        return true;
    }

    @Override
    public List<SellOrder> getUserSellOrders(Long userId) {
        List<SellOrder> orders = sellOrderMapper.selectByUserId(userId);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    public List<SellOrder> getActiveOrdersByItemId(Long itemId) {
        List<SellOrder> orders = sellOrderMapper.selectActiveByItemId(itemId);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    public SellOrder getOrderById(Long id) {
        SellOrder order = sellOrderMapper.selectById(id);
        if (order != null) {
            enrichOrderInfo(order);
        }
        return order;
    }

    @Override
    public List<SellOrder> getAllSellOrders() {
        List<SellOrder> orders = sellOrderMapper.selectAll();
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    public List<SellOrder> getSellOrdersByStatus(Integer status) {
        List<SellOrder> orders = sellOrderMapper.selectByStatus(status);
        orders.forEach(this::enrichOrderInfo);
        return orders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminCancelSellOrder(Long orderId) {
        log.info("管理员取消出售订单: orderId={}", orderId);

        SellOrder order = sellOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("订单状态不可取消");
        }

        sellOrderMapper.updateStatus(orderId, SellOrder.STATUS_CANCELLED);

        if (order.getInventoryId() != null) {
            userInventoryMapper.updateStatus(order.getInventoryId(), UserInventory.STATUS_NORMAL);
        }

        log.info("管理员取消出售订单成功: id={}", orderId);
        return true;
    }

    /**
     * 填充关联信息
     */
    private void enrichOrderInfo(SellOrder order) {
        // 查询库存信息（优先使用库存中的原始数据）
        if (order.getInventoryId() != null) {
            try {
                UserInventory inventory = userInventoryMapper.selectById(order.getInventoryId());
                inspectMetadataService.repairAndPersist(inventory);
                order.setInventory(inventory);
            } catch (Exception e) {
                log.warn("获取库存信息失败: inventoryId={}", order.getInventoryId());
            }
        }

        // 查询饰品信息
        if (order.getItemId() != null) {
            try {
                Item item = itemMapper.selectById(order.getItemId());
                order.setItem(item);
            } catch (Exception e) {
                log.warn("获取饰品信息失败: itemId={}", order.getItemId());
            }
        }

        // 查询用户信息
        preferInventoryItem(order);

        if (order.getUserId() != null) {
            try {
                order.setUser(userMapper.selectById(order.getUserId()));
            } catch (Exception e) {
                log.warn("获取用户信息失败: userId={}", order.getUserId());
            }
        }
    }

    private void preferInventoryItem(SellOrder order) {
        if (order == null || order.getInventory() == null) {
            return;
        }

        UserInventory inventory = order.getInventory();
        Item inventoryItem = inventory.getItem();
        if (inventoryItem == null || isUnknownItem(inventoryItem)) {
            return;
        }

        Long resolvedItemId = inventory.getItemId() != null ? inventory.getItemId() : inventoryItem.getId();
        if (resolvedItemId == null) {
            return;
        }

        Item fullInventoryItem = itemMapper.selectById(resolvedItemId);
        if (fullInventoryItem != null) {
            inventoryItem = fullInventoryItem;
            inventory.setItem(fullInventoryItem);
        }
        ensureSteamReferencePrice(inventoryItem, inventory.getMarketHashName());

        Item orderItem = order.getItem();
        if (orderItem != null && !isUnknownItem(orderItem) && order.getItemId() != null
                && order.getItemId().equals(resolvedItemId)) {
            order.setItem(inventoryItem);
            return;
        }

        order.setItemId(resolvedItemId);
        order.setItem(inventoryItem);
        try {
            sellOrderMapper.updateItemId(order.getId(), resolvedItemId);
        } catch (Exception e) {
            log.warn("淇鍑哄敭璁㈠崟 item_id 澶辫触: orderId={}, itemId={}", order.getId(), inventory.getItemId());
        }
    }

    private boolean isUnknownItem(Item item) {
        return item == null
                || "unknown".equalsIgnoreCase(item.getItemId())
                || "Unknown Item".equalsIgnoreCase(item.getName());
    }

    private void ensureSteamReferencePrice(Item item, String marketHashName) {
        if (item == null || marketHashName == null || marketHashName.isBlank()) {
            return;
        }
        if (item.getSteamReferencePrice() != null && item.getSteamReferencePrice().compareTo(BigDecimal.ZERO) > 0) {
            return;
        }

        JSONObject overview = steamApiClient.getMarketPriceOverview(marketHashName);
        if (overview == null || !overview.getBooleanValue("success")) {
            return;
        }

        BigDecimal price = parsePriceText(overview.getString("lowest_price"), overview.getString("median_price"));
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        item.setSteamReferencePrice(price);
        item.setSteamReferenceCurrency(STEAM_REFERENCE_CURRENCY);
        item.setSteamReferencePriceSource(STEAM_REFERENCE_PRICE_SOURCE);
        item.setSteamReferencePriceUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    private BigDecimal parsePriceText(String... texts) {
        if (texts == null) {
            return null;
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }

            Matcher matcher = PRICE_TEXT_PATTERN.matcher(text.replace(",", ""));
            if (!matcher.find()) {
                continue;
            }

            try {
                BigDecimal value = new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Try the next candidate.
            }
        }

        return null;
    }

    @Override
    public PageResult<SellOrder> getMarketList(String category, String exterior, String quality,
                                                String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                                String sortField, String sortOrder, Integer page, Integer size) {
        log.info("获取在售市场列表: category={}, exterior={}, quality={}, keyword={}, minPrice={}, maxPrice={}, page={}, size={}",
                category, exterior, quality, keyword, minPrice, maxPrice, page, size);

        int offset = (page - 1) * size;
        
        List<SellOrder> orders = sellOrderMapper.selectMarketList(
                category, exterior, quality, keyword, minPrice, maxPrice,
                sortField, sortOrder, offset, size);
        
        Long total = sellOrderMapper.countMarketList(
                category, exterior, quality, keyword, minPrice, maxPrice);

        return PageResult.of(page, size, total, orders);
    }

    /**
     * 根据名称匹配物品
     */
    private Item matchItemByName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }

        // 获取所有物品
        List<Item> allItems = itemMapper.selectAllActive();
        
        // 直接匹配
        for (Item item : allItems) {
            if (itemName.equalsIgnoreCase(item.getNameCn()) || itemName.equalsIgnoreCase(item.getName())) {
                return item;
            }
        }
        
        // 模糊匹配
        for (Item item : allItems) {
            String cnName = item.getNameCn() != null ? item.getNameCn().toLowerCase() : "";
            String enName = item.getName() != null ? item.getName().toLowerCase() : "";
            String searchName = itemName.toLowerCase();
            
            if (cnName.contains(searchName) || enName.contains(searchName) ||
                searchName.contains(cnName) || searchName.contains(enName)) {
                return item;
            }
        }
        
        return null;
    }

    /**
     * 获取或创建未知饰品
     */
    private Item getOrCreateUnknownItem() {
        // 尝试查找已存在的未知饰品
        List<Item> items = itemMapper.selectAllActive();
        for (Item item : items) {
            if ("unknown".equals(item.getItemId()) || "未知饰品".equals(item.getNameCn())) {
                return item;
            }
        }
        
        // 创建新的未知饰品（理论上不会执行到这里，因为未知饰品应该已存在）
        Item unknownItem = new Item();
        unknownItem.setItemId("unknown");
        unknownItem.setName("Unknown Item");
        unknownItem.setNameCn("未知饰品");
        unknownItem.setCategory("other");
        unknownItem.setRarity("consumer");
        unknownItem.setBuffPrice(BigDecimal.ZERO);
        unknownItem.setIsActive(1);
        unknownItem.setIconUrl("/default-item.png");
        unknownItem.setCreatedAt(LocalDateTime.now());
        unknownItem.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(unknownItem);
        return unknownItem;
    }
}
