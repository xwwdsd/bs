package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.dto.market.ItemIconCandidate;
import com.cs2trade.dto.market.ItemMarketPanel;
import com.cs2trade.dto.market.ItemRecommendation;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.ItemMarketSnapshot;
import com.cs2trade.entity.ItemPriceHistory;
import com.cs2trade.mapper.FavoriteMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.ItemMarketSnapshotMapper;
import com.cs2trade.mapper.ItemPriceHistoryMapper;
import com.cs2trade.mapper.MarketAnalyticsMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.util.SteamApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketAnalyticsServiceImplTest {

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemPriceHistoryMapper itemPriceHistoryMapper;

    @Mock
    private ItemMarketSnapshotMapper itemMarketSnapshotMapper;

    @Mock
    private MarketAnalyticsMapper marketAnalyticsMapper;

    @Mock
    private UserInventoryMapper userInventoryMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private SteamApiClient steamApiClient;

    private MarketAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MarketAnalyticsServiceImpl(
                itemMapper,
                itemPriceHistoryMapper,
                itemMarketSnapshotMapper,
                marketAnalyticsMapper,
                userInventoryMapper,
                favoriteMapper,
                steamApiClient
        );
    }

    @Test
    void getMarketPanelRefreshesSteamTrendUsingCanonicalMarketHashName() {
        Long itemId = 1L;
        Item item = new Item();
        item.setId(itemId);
        item.setItemId("legacy_item_id");
        item.setSteamMarketHashName("AK-47 | Redline (Field-Tested)");
        item.setSteamReferencePrice(new BigDecimal("12.00"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("12.00"));
        List<ItemPriceHistory> steamHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 10, 0, 0), "11.80"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 11, 0, 0), "12.10")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam"))
                .thenReturn(List.of(), List.of(), steamHistory);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade"))
                .thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId))
                .thenReturn(new BigDecimal("11.00"), new BigDecimal("11.00"));
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());
        when(steamApiClient.getMarketPriceHistory("AK-47 | Redline (Field-Tested)")).thenReturn(steamHistoryPayload());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        verify(steamApiClient).getMarketPriceHistory("AK-47 | Redline (Field-Tested)");
        verify(itemPriceHistoryMapper).deleteByItemIdAndSource(itemId, "steam");
        verify(itemPriceHistoryMapper, org.mockito.Mockito.times(2)).insert(any(ItemPriceHistory.class));
        assertEquals("steam", panel.getTrendSource());
        assertEquals("steam", panel.getReferencePriceSource());
        assertEquals(new BigDecimal("12.00"), panel.getReferencePrice());
        assertFalse(panel.getPriceTrend().get("sevenDays").isEmpty());
    }

    @Test
    void getMarketPanelFallsBackToBuffReferenceTrendWhenSteamHistoryIsUnavailable() {
        Long itemId = 2L;
        Item item = new Item();
        item.setId(itemId);
        item.setItemId("legacy_item_id");
        item.setBuffPrice(new BigDecimal("18.88"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("18.88"));

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam"))
                .thenReturn(List.of(), List.of(), List.of());
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade"))
                .thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        assertEquals("reference", panel.getTrendSource());
        assertEquals("buff", panel.getReferencePriceSource());
        assertEquals(new BigDecimal("18.88"), panel.getReferencePrice());
        assertNotNull(panel.getPriceTrend().get("sevenDays"));
        assertEquals(7, panel.getPriceTrend().get("sevenDays").size());
    }

    @Test
    void getMarketPanelRefreshesSteamHistoryWhenCachedHistoryLooksLikeUsd() {
        Long itemId = 4L;
        Item item = new Item();
        item.setId(itemId);
        item.setSteamMarketHashName("M4A1-S | Decimator (Field-Tested)");
        item.setSteamReferencePrice(new BigDecimal("69.71"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("69.71"));
        List<ItemPriceHistory> usdLikeHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 14, 0, 0), "9.88"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 15, 0, 0), "9.99")
        );
        List<ItemPriceHistory> cnyHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 14, 0, 0), "68.50"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 15, 0, 0), "69.71")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam"))
                .thenReturn(usdLikeHistory, usdLikeHistory, cnyHistory);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade"))
                .thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());
        when(steamApiClient.getMarketPriceHistory("M4A1-S | Decimator (Field-Tested)"))
                .thenReturn(steamHistoryPayload("68.50", "69.71"));

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        verify(steamApiClient).getMarketPriceHistory("M4A1-S | Decimator (Field-Tested)");
        assertEquals("steam", panel.getTrendSource());
        assertEquals(new BigDecimal("69.71"), panel.getPriceTrend().get("sevenDays").getLast().getPrice());
    }

    @Test
    void getRecommendationsFallsBackToInventoryIconAndBackfillsItem() {
        Long itemId = 3L;
        Item item = new Item();
        item.setId(itemId);
        item.setName("Sticker | Team LDLC.com | Katowice 2014");
        item.setNameCn("印花 | Team LDLC.com | 2014年卡托维兹锦标赛");
        item.setCategory("other");
        item.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/8dbc71957312bbd3baea65848b545be9eae2a355.jpg");

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("2032.62"));
        String inventoryIcon = "https://steamcommunity-a.akamaihd.net/economy/image/icon-hash";
        ItemIconCandidate iconCandidate = new ItemIconCandidate();
        iconCandidate.setItemId(itemId);
        iconCandidate.setIconUrl(inventoryIcon);

        when(itemMapper.selectAllActive()).thenReturn(List.of(item));
        when(itemMarketSnapshotMapper.selectLatestAll()).thenReturn(List.of(snapshot));
        when(marketAnalyticsMapper.selectBestInventoryIconUrls(List.of(itemId))).thenReturn(List.of(iconCandidate));

        List<ItemRecommendation> recommendations = service.getRecommendations(null, 6);

        assertEquals(1, recommendations.size());
        assertEquals(inventoryIcon, recommendations.get(0).getIconUrl());
        verify(itemMapper).updateIconUrl(itemId, inventoryIcon);
    }

    private ItemMarketSnapshot baseSnapshot(Long itemId, BigDecimal referencePrice) {
        ItemMarketSnapshot snapshot = new ItemMarketSnapshot();
        snapshot.setItemId(itemId);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setReferencePrice(referencePrice);
        snapshot.setLatestPrice(referencePrice);
        snapshot.setSuggestedSellPrice(referencePrice);
        snapshot.setSuggestedBuyPrice(referencePrice.multiply(new BigDecimal("0.95")));
        return snapshot;
    }

    private ItemPriceHistory history(Long itemId, String source, LocalDateTime recordedAt, String price) {
        ItemPriceHistory history = new ItemPriceHistory();
        history.setItemId(itemId);
        history.setSource(source);
        history.setRecordedAt(recordedAt);
        history.setRangeType("lifetime");
        history.setPrice(new BigDecimal(price));
        history.setVolume(1);
        return history;
    }

    private JSONObject steamHistoryPayload() {
        return steamHistoryPayload("11.80", "12.10");
    }

    private JSONObject steamHistoryPayload(String firstPrice, String secondPrice) {
        JSONArray prices = new JSONArray();
        prices.add(priceRow("Apr 10 2026 0", firstPrice, 2));
        prices.add(priceRow("Apr 11 2026 0", secondPrice, 3));

        JSONObject payload = new JSONObject();
        payload.put("success", true);
        payload.put("prices", prices);
        return payload;
    }

    private JSONArray priceRow(String time, String price, int volume) {
        JSONArray row = new JSONArray();
        row.add(time);
        row.add(price);
        row.add(volume);
        return row;
    }
}
