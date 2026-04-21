package com.cs2trade.service;

import com.cs2trade.dto.market.InventoryAnalysis;
import com.cs2trade.dto.market.ItemMarketPanel;
import com.cs2trade.dto.market.ItemRecommendation;

import java.util.List;

public interface MarketAnalyticsService {

    ItemMarketPanel getMarketPanel(Long itemId);

    InventoryAnalysis getInventoryAnalysis(Long userId);

    List<ItemRecommendation> getRecommendations(Long userId, int limit);

    void rebuildMarketData(Long itemId);

    void rebuildAllMarketData();
}
