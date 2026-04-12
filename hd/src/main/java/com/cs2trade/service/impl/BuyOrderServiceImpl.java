package com.cs2trade.service.impl;

import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.BuyOrder;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.BuyOrderMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.service.BuyOrderService;
import com.cs2trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyOrderServiceImpl implements BuyOrderService {

    private static final int DEFAULT_EXPIRE_DAYS = 7;

    private final BuyOrderMapper buyOrderMapper;
    private final ItemMapper itemMapper;
    private final UserInventoryMapper userInventoryMapper;
    private final SellOrderMapper sellOrderMapper;
    private final TradeOrderService tradeOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuyOrder createBuyOrder(Long userId, Long itemId, BigDecimal price, Integer quantity, Integer autoAccept) {
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        if (itemId == null) {
            throw new RuntimeException("请选择求购饰品");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("求购价格必须大于0");
        }

        int safeQuantity = quantity == null ? 1 : quantity;
        if (safeQuantity <= 0) {
            throw new RuntimeException("求购数量必须大于0");
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null || (item.getIsActive() != null && item.getIsActive() == 0)) {
            throw new RuntimeException("饰品不存在或已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        BuyOrder order = new BuyOrder();
        order.setUserId(userId);
        order.setItemId(itemId);
        order.setPrice(price);
        order.setQuantity(safeQuantity);
        order.setFilledQuantity(0);
        order.setStatus(BuyOrder.STATUS_ACTIVE);
        order.setAutoAccept(autoAccept != null && autoAccept == BuyOrder.AUTO_ACCEPT_NO
                ? BuyOrder.AUTO_ACCEPT_NO
                : BuyOrder.AUTO_ACCEPT_YES);
        order.setExpireTime(now.plusDays(DEFAULT_EXPIRE_DAYS));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        buyOrderMapper.insert(order);
        order.setItem(item);
        log.info("Created buy order: id={}, userId={}, itemId={}, price={}, quantity={}",
                order.getId(), userId, itemId, price, safeQuantity);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBuyOrder(Long orderId, Long userId) {
        BuyOrder order = requireOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消该求购单");
        }
        if (order.getStatus() != BuyOrder.STATUS_ACTIVE) {
            throw new RuntimeException("当前求购单状态不允许取消");
        }

        buyOrderMapper.updateStatus(orderId, BuyOrder.STATUS_CANCELLED);
        return true;
    }

    @Override
    public List<BuyOrder> getUserBuyOrders(Long userId) {
        buyOrderMapper.expireOldActiveOrders();
        return buyOrderMapper.selectByUserId(userId);
    }

    @Override
    public List<BuyOrder> getActiveOrdersByItemId(Long itemId) {
        buyOrderMapper.expireOldActiveOrders();
        return buyOrderMapper.selectActiveByItemId(itemId);
    }

    @Override
    public BuyOrder getOrderById(Long id) {
        BuyOrder order = buyOrderMapper.selectById(id);
        if (order != null) {
            enrichOrder(order);
        }
        return order;
    }

    @Override
    public PageResult<BuyOrder> getMarketList(String category, String exterior, String quality,
                                              String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                              String sortField, String sortOrder, Integer page, Integer size) {
        buyOrderMapper.expireOldActiveOrders();

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        List<BuyOrder> orders = buyOrderMapper.selectMarketList(
                category, exterior, quality, keyword, minPrice, maxPrice,
                sortField, sortOrder, offset, safeSize
        );
        Long total = buyOrderMapper.countMarketList(category, exterior, quality, keyword, minPrice, maxPrice);
        return PageResult.of(safePage, safeSize, total == null ? 0 : total, orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder respondToBuyOrder(Long sellerId, Long buyOrderId, Long inventoryId) {
        if (sellerId == null) {
            throw new RuntimeException("请先登录");
        }
        if (inventoryId == null) {
            throw new RuntimeException("请选择用于出售的库存饰品");
        }

        BuyOrder buyOrder = requireOrder(buyOrderId);
        if (buyOrder.getStatus() != BuyOrder.STATUS_ACTIVE) {
            throw new RuntimeException("该求购单已不可响应");
        }
        if (buyOrder.getExpireTime() != null && !buyOrder.getExpireTime().isAfter(LocalDateTime.now())) {
            buyOrderMapper.updateStatus(buyOrderId, BuyOrder.STATUS_CANCELLED);
            throw new RuntimeException("该求购单已过期");
        }
        if (buyOrder.getFilledQuantity() != null
                && buyOrder.getQuantity() != null
                && buyOrder.getFilledQuantity() >= buyOrder.getQuantity()) {
            buyOrderMapper.updateStatus(buyOrderId, BuyOrder.STATUS_COMPLETED);
            throw new RuntimeException("该求购单已完成");
        }
        if (buyOrder.getUserId().equals(sellerId)) {
            throw new RuntimeException("不能响应自己的求购单");
        }

        UserInventory inventory = userInventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            throw new RuntimeException("库存饰品不存在");
        }
        if (!inventory.getUserId().equals(sellerId)) {
            throw new RuntimeException("无权操作该库存饰品");
        }
        if (inventory.getStatus() == null || inventory.getStatus() != UserInventory.STATUS_NORMAL) {
            throw new RuntimeException("该库存饰品当前不可出售");
        }
        if (inventory.getIsMarketable() == null || inventory.getIsMarketable() != UserInventory.IS_MARKETABLE) {
            throw new RuntimeException("该库存饰品暂不可交易");
        }
        if (inventory.getItemId() == null || !inventory.getItemId().equals(buyOrder.getItemId())) {
            throw new RuntimeException("库存饰品与求购饰品不匹配");
        }

        int reserved = buyOrderMapper.incrementFilledQuantity(buyOrder.getId());
        if (reserved != 1) {
            throw new RuntimeException("\u8be5\u6c42\u8d2d\u5355\u5df2\u5b8c\u6210\u6216\u5df2\u88ab\u4ed6\u4eba\u54cd\u5e94");
        }

        LocalDateTime now = LocalDateTime.now();
        SellOrder sellOrder = new SellOrder();
        sellOrder.setUserId(sellerId);
        sellOrder.setItemId(buyOrder.getItemId());
        sellOrder.setInventoryId(inventoryId);
        sellOrder.setPrice(buyOrder.getPrice());
        sellOrder.setStatus(SellOrder.STATUS_ON_SALE);
        sellOrder.setIsResponseToBuy(SellOrder.RESPONSE_YES);
        sellOrder.setTargetBuyOrderId(buyOrder.getId());
        sellOrder.setExpireTime(now.plusDays(DEFAULT_EXPIRE_DAYS));
        sellOrder.setCreatedAt(now);
        sellOrder.setUpdatedAt(now);
        sellOrderMapper.insert(sellOrder);
        userInventoryMapper.updateStatus(inventoryId, UserInventory.STATUS_ON_SALE);

        TradeOrder tradeOrder = tradeOrderService.createOrder(buyOrder.getUserId(), sellOrder.getId(), buyOrder.getPrice());

        log.info("Responded to buy order: buyOrderId={}, sellOrderId={}, tradeOrderId={}",
                buyOrder.getId(), sellOrder.getId(), tradeOrder.getId());
        return tradeOrder;
    }

    private BuyOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("求购单ID不能为空");
        }
        BuyOrder order = buyOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("求购单不存在");
        }
        return order;
    }

    private void enrichOrder(BuyOrder order) {
        if (order.getItemId() != null) {
            order.setItem(itemMapper.selectById(order.getItemId()));
        }
    }
}
