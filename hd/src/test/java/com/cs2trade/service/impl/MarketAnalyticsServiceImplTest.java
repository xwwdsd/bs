package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.dto.market.ItemIconCandidate;
import com.cs2trade.dto.market.ItemMarketPanel;
import com.cs2trade.dto.market.ItemRecommendation;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.ItemMarketSnapshot;
import com.cs2trade.entity.ItemPriceHistory;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.mapper.FavoriteMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.ItemMarketSnapshotMapper;
import com.cs2trade.mapper.ItemPriceHistoryMapper;
import com.cs2trade.mapper.MarketAnalyticsMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.util.SteamApiClient;
import com.cs2trade.util.SteamMarketPageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        assertFalse(panel.getPriceTrend().get("all").isEmpty());
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
        assertEquals(new BigDecimal("69.71"), panel.getPriceTrend().get("all").getLast().getPrice());
    }

    @Test
    void getMarketPanelIgnoresAbnormalLowestSellPriceAndKeepsLowestSellVisible() {
        Long itemId = 10L;
        Item item = new Item();
        item.setId(itemId);
        item.setSteamReferencePrice(new BigDecimal("22.00"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("22.00"));
        snapshot.setLowestSellPrice(new BigDecimal("323.00"));
        snapshot.setSuggestedSellPrice(new BigDecimal("322.99"));
        snapshot.setSuggestedBuyPrice(new BigDecimal("306.84"));

        List<ItemPriceHistory> steamHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 20, 0, 0), "21.80"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 21, 0, 0), "22.00")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam")).thenReturn(steamHistory, steamHistory);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade")).thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId)).thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId)).thenReturn(new BigDecimal("323.00"));
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        assertEquals(new BigDecimal("323.00"), panel.getLowestSellPrice());
        assertEquals(new BigDecimal("22.00"), panel.getSuggestedSellPrice());
        assertEquals(new BigDecimal("20.90"), panel.getSuggestedBuyPrice());
        assertTrue(panel.getPricingBasis().contains("忽略异常挂单"));
    }

    @Test
    void getMarketPanelKeepsSuggestionAnchoredWhenLowestSellIsHigherThanReference() {
        Long itemId = 11L;
        Item item = new Item();
        item.setId(itemId);
        item.setSteamReferencePrice(new BigDecimal("100.00"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("100.00"));
        snapshot.setLowestSellPrice(new BigDecimal("120.00"));

        List<ItemPriceHistory> steamHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 20, 0, 0), "99.50"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 21, 0, 0), "100.00")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam")).thenReturn(steamHistory, steamHistory);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade")).thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId)).thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId)).thenReturn(new BigDecimal("120.00"));
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        assertEquals(new BigDecimal("100.00"), panel.getSuggestedSellPrice());
        assertEquals(new BigDecimal("95.00"), panel.getSuggestedBuyPrice());
        assertFalse(panel.getPricingBasis().contains("忽略异常挂单"));
    }

    @Test
    void getMarketPanelPrefersAverageTradeWhenLowestSellIsAbnormal() {
        Long itemId = 12L;
        Item item = new Item();
        item.setId(itemId);
        item.setSteamReferencePrice(new BigDecimal("22.00"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("22.00"));
        snapshot.setLowestSellPrice(new BigDecimal("323.00"));
        snapshot.setAvgTradePrice7d(new BigDecimal("28.50"));

        List<ItemPriceHistory> steamHistory = List.of(
                history(itemId, "steam", LocalDateTime.of(2026, 4, 20, 0, 0), "27.90"),
                history(itemId, "steam", LocalDateTime.of(2026, 4, 21, 0, 0), "28.50")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(item, item);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam")).thenReturn(steamHistory, steamHistory);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade")).thenReturn(List.of(), List.of());
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId)).thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId)).thenReturn(new BigDecimal("323.00"));
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        assertEquals(new BigDecimal("28.50"), panel.getSuggestedSellPrice());
        assertEquals(new BigDecimal("27.08"), panel.getSuggestedBuyPrice());
        assertTrue(panel.getPricingBasis().contains("近 7 日成交"));
    }

    @Test
    void getMarketPanelFallsBackToHighestBuyWhenNoOtherSignalsExist() {
        Long itemId = 13L;
        ItemMarketSnapshot snapshot = baseSnapshot(itemId, null);
        snapshot.setReferencePrice(null);
        snapshot.setLatestPrice(null);
        snapshot.setLowestSellPrice(null);
        snapshot.setAvgTradePrice7d(null);
        snapshot.setSuggestedSellPrice(null);
        snapshot.setSuggestedBuyPrice(null);

        List<ItemPriceHistory> localHistory = List.of(
                history(itemId, "local_trade", LocalDateTime.of(2026, 4, 20, 0, 0), "17.80"),
                history(itemId, "local_trade", LocalDateTime.of(2026, 4, 21, 0, 0), "18.10")
        );

        when(itemMarketSnapshotMapper.selectLatestByItemId(itemId)).thenReturn(snapshot, snapshot);
        when(itemMapper.selectById(itemId)).thenReturn(null, null);
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "steam")).thenReturn(List.of(), List.of());
        when(itemPriceHistoryMapper.selectByItemIdAndSource(itemId, "local_trade")).thenReturn(localHistory, localHistory);
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId)).thenReturn(new BigDecimal("18.00"), new BigDecimal("18.00"));
        when(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId)).thenReturn(BigDecimal.ZERO);
        when(marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10)).thenReturn(List.of());

        ItemMarketPanel panel = service.getMarketPanel(itemId);

        assertEquals(new BigDecimal("18.00"), panel.getSuggestedSellPrice());
        assertEquals(new BigDecimal("17.10"), panel.getSuggestedBuyPrice());
        assertTrue(panel.getPricingBasis().contains("最高求购价"));
    }

    @Test
    void getInventoryAnalysisUsesUnifiedPricingDecisionForRecommendations() {
        Long userId = 100L;
        Long itemId = 14L;

        Item item = new Item();
        item.setId(itemId);
        item.setNameCn("P250 | 二西莫夫 (久经沙场)");
        item.setSteamReferencePrice(new BigDecimal("22.00"));
        item.setCategory("weapon");

        UserInventory inventory = new UserInventory();
        inventory.setId(501L);
        inventory.setUserId(userId);
        inventory.setItemId(itemId);
        inventory.setName("P250 | Asiimov (Field-Tested)");
        inventory.setItem(item);
        inventory.setIconUrl("/inventory.png");
        inventory.setIsMarketable(UserInventory.IS_MARKETABLE);
        inventory.setStatus(UserInventory.STATUS_NORMAL);

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("22.00"));
        snapshot.setLowestSellPrice(new BigDecimal("323.00"));
        snapshot.setSuggestedSellPrice(new BigDecimal("322.99"));
        snapshot.setSuggestedBuyPrice(new BigDecimal("306.84"));

        when(userInventoryMapper.selectByUserId(userId)).thenReturn(List.of(inventory));
        when(itemMarketSnapshotMapper.selectLatestAll()).thenReturn(List.of(snapshot));
        when(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId)).thenReturn(BigDecimal.ZERO);

        var analysis = service.getInventoryAnalysis(userId);

        assertEquals(new BigDecimal("22.00"), analysis.getTotalValue());
        assertEquals(1, analysis.getRecommendations().size());
        assertEquals(new BigDecimal("22.00"), analysis.getRecommendations().getFirst().getSuggestedSellPrice());
    }

    @Test
    void getRecommendationsFallsBackToInventoryIcon() {
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

        List<ItemRecommendation> recommendations = service.getRecommendations(null, null, 6);

        assertEquals(1, recommendations.size());
        assertEquals(inventoryIcon, recommendations.get(0).getIconUrl());
        assertEquals("行情样本偏少", recommendations.get(0).getRecommendReason());
        verifyNoInteractions(steamApiClient);
    }

    @Test
    void getRecommendationsBackfillsMissingItemIconFromSteamSearch() {
        Long itemId = 5905L;
        Item item = new Item();
        item.setId(itemId);
        item.setItemId("P90 | Blind Spot (Well-Worn)");
        item.setName("P90 | 盲点 (破损不堪)");
        item.setNameCn("P90 | 盲点 (破损不堪)");
        item.setCategory("smg");
        item.setQuality("common");
        item.setIconUrl("https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps/730/app-icon.jpg");
        item.setSteamReferencePrice(new BigDecimal("13.01"));

        ItemMarketSnapshot snapshot = baseSnapshot(itemId, new BigDecimal("13.01"));

        JSONObject exactResult = new JSONObject();
        exactResult.put("name", "P90 | 盲点 (破损不堪)");
        exactResult.put("hash_name", "P90 | Blind Spot (Well-Worn)");
        exactResult.put("sell_price", 1647);
        JSONObject exactAssetDescription = new JSONObject();
        exactAssetDescription.put("market_hash_name", "P90 | Blind Spot (Well-Worn)");
        exactAssetDescription.put("icon_url", "blind-spot-icon");
        exactResult.put("asset_description", exactAssetDescription);

        JSONObject distractorResult = new JSONObject();
        distractorResult.put("name", "P90 | 盲点 (战痕累累)");
        distractorResult.put("hash_name", "P90 | Blind Spot (Battle-Scarred)");
        JSONObject distractorAssetDescription = new JSONObject();
        distractorAssetDescription.put("market_hash_name", "P90 | Blind Spot (Battle-Scarred)");
        distractorAssetDescription.put("icon_url", "battle-scarred-icon");
        distractorResult.put("asset_description", distractorAssetDescription);

        JSONArray results = new JSONArray();
        results.add(distractorResult);
        results.add(exactResult);

        JSONObject payload = new JSONObject();
        payload.put("success", true);
        payload.put("results", results);
        payload.put("total_count", 2);

        when(itemMapper.selectAllActive()).thenReturn(List.of(item));
        when(itemMarketSnapshotMapper.selectLatestAll()).thenReturn(List.of(snapshot));
        when(marketAnalyticsMapper.selectBestInventoryIconUrls(List.of(itemId))).thenReturn(List.of());
        when(steamApiClient.searchCsgoItems("P90 | Blind Spot (Well-Worn)", 10))
                .thenReturn(SteamMarketPageResult.success(200, payload));

        List<ItemRecommendation> recommendations = service.getRecommendations(null, null, 6);

        assertEquals(1, recommendations.size());
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/blind-spot-icon", recommendations.get(0).getIconUrl());

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        verify(steamApiClient, never()).searchCsgoItems("P90 | 盲点 (破损不堪)", 10);

        Item updated = itemCaptor.getValue();
        assertEquals(itemId, updated.getId());
        assertEquals("P90 | Blind Spot (Well-Worn)", updated.getSteamMarketHashName());
        assertEquals(
                "https://steamcommunity.com/market/listings/730/P90%20%7C%20Blind%20Spot%20%28Well-Worn%29",
                updated.getSteamMarketUrl()
        );
        assertEquals("https://steamcommunity-a.akamaihd.net/economy/image/blind-spot-icon", updated.getIconUrl());
        assertEquals(new BigDecimal("16.47"), updated.getSteamReferencePrice());
    }

    @Test
    void getRecommendationsUsesCachedSteamHistoryForHeatWhenSnapshotHeatIsMissing() {
        Long currentItemId = 7000L;
        Long candidateItemId = 7001L;

        Item currentItem = recommendationItem(currentItemId, "Tec-9 | Current", "25.00");
        Item candidateItem = recommendationItem(candidateItemId, "Tec-9 | Active", "25.00");
        candidateItem.setSubCategory("smg");
        candidateItem.setExterior("MW");

        ItemMarketSnapshot currentSnapshot = baseSnapshot(currentItemId, new BigDecimal("25.00"));
        ItemMarketSnapshot candidateSnapshot = baseSnapshot(candidateItemId, new BigDecimal("25.00"));
        candidateSnapshot.setHeatScore(0);

        ItemPriceHistory firstHistory = history(
                candidateItemId,
                "steam",
                LocalDateTime.now().minusDays(2),
                "24.00"
        );
        firstHistory.setVolume(8);
        ItemPriceHistory secondHistory = history(
                candidateItemId,
                "steam",
                LocalDateTime.now().minusDays(1),
                "25.00"
        );
        secondHistory.setVolume(9);

        when(itemMapper.selectAllActive()).thenReturn(List.of(currentItem, candidateItem));
        when(itemMarketSnapshotMapper.selectLatestAll()).thenReturn(List.of(currentSnapshot, candidateSnapshot));
        when(itemPriceHistoryMapper.selectByItemIdAndSource(candidateItemId, "steam"))
                .thenReturn(List.of(firstHistory, secondHistory));

        List<ItemRecommendation> recommendations = service.getRecommendations(null, currentItemId, 1);

        assertEquals(1, recommendations.size());
        assertEquals(candidateItemId, recommendations.getFirst().getItemId());
        assertTrue(recommendations.getFirst().getHeatScore() > 0);
        assertTrue(recommendations.getFirst().getRecommendScore() > 64);
    }

    private Item recommendationItem(Long itemId, String name, String price) {
        Item item = new Item();
        item.setId(itemId);
        item.setName(name);
        item.setNameCn(name);
        item.setCategory("weapon");
        item.setSubCategory("pistol");
        item.setQuality("mil-spec");
        item.setExterior("FN");
        item.setIconUrl("https://steamcommunity-a.akamaihd.net/economy/image/" + itemId);
        item.setSteamReferencePrice(new BigDecimal(price));
        return item;
    }

    private ItemMarketSnapshot baseSnapshot(Long itemId, BigDecimal referencePrice) {
        ItemMarketSnapshot snapshot = new ItemMarketSnapshot();
        snapshot.setItemId(itemId);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setReferencePrice(referencePrice);
        snapshot.setLatestPrice(referencePrice);
        snapshot.setSuggestedSellPrice(referencePrice);
        snapshot.setSuggestedBuyPrice(referencePrice != null
                ? referencePrice.multiply(new BigDecimal("0.95"))
                : null);
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
