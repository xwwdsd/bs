package com.cs2trade.service;

import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.BuyOrder;
import com.cs2trade.entity.TradeOrder;

import java.math.BigDecimal;
import java.util.List;

public interface BuyOrderService {

    BuyOrder createBuyOrder(Long userId, Long itemId, BigDecimal price, Integer quantity, Integer autoAccept);

    boolean cancelBuyOrder(Long orderId, Long userId);

    List<BuyOrder> getUserBuyOrders(Long userId);

    List<BuyOrder> getActiveOrdersByItemId(Long itemId);

    BuyOrder getOrderById(Long id);

    PageResult<BuyOrder> getMarketList(String category, String exterior, String quality,
                                       String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                       String sortField, String sortOrder, Integer page, Integer size);

    TradeOrder respondToBuyOrder(Long sellerId, Long buyOrderId, Long inventoryId);
}
