package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.dto.market.CategoryDistribution;
import com.cs2trade.dto.market.InventoryAnalysis;
import com.cs2trade.dto.market.InventoryRecommendation;
import com.cs2trade.dto.market.ItemIconCandidate;
import com.cs2trade.dto.market.ItemMarketPanel;
import com.cs2trade.dto.market.ItemRecommendation;
import com.cs2trade.dto.market.MarketTrendPoint;
import com.cs2trade.dto.market.RecentTradeRecord;
import com.cs2trade.dto.market.TradeDailyStat;
import com.cs2trade.entity.Favorite;
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
import com.cs2trade.service.MarketAnalyticsService;
import com.cs2trade.util.SteamApiClient;
import com.cs2trade.util.SteamItemIdentityUtils;
import com.cs2trade.util.SteamMarketPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalyticsServiceImpl implements MarketAnalyticsService {

    private static final String SOURCE_STEAM = "steam";
    private static final String SOURCE_LOCAL_TRADE = "local_trade";
    private static final String SOURCE_REFERENCE = "reference";
    private static final String REFERENCE_SOURCE_STEAM = "steam";
    private static final String REFERENCE_SOURCE_BUFF = "buff";
    private static final String REFERENCE_SOURCE_LOCAL = "local";
    private static final String RANGE_LIFETIME = "lifetime";
    private static final String RANGE_DAILY = "daily";
    private static final String RANGE_WEEK = "week";
    private static final String RANGE_MONTH = "month";
    private static final BigDecimal BUY_DISCOUNT = new BigDecimal("0.95");
    private static final BigDecimal PRICE_UNDERCUT = new BigDecimal("0.01");
    private static final BigDecimal SNAPSHOT_PRICE_DIFF_TOLERANCE = new BigDecimal("0.005");
    private static final BigDecimal ABNORMAL_LOWEST_SELL_HIGH_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal ABNORMAL_LOWEST_SELL_LOW_MULTIPLIER = new BigDecimal("0.80");
    private static final int RECOMMENDATION_STEAM_SEARCH_LIMIT = 10;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern STEAM_TIME_PATTERN = Pattern.compile("([A-Za-z]{3})\\s+(\\d{1,2})\\s+(\\d{4})\\s+(\\d{1,2})");
    private static final String STEAM_APP_ICON_URL_FRAGMENT = "/steamcommunity/public/images/apps/730/";
    private static final DateTimeFormatter STEAM_HISTORY_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM d yyyy H")
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter TREND_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TREND_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final ItemMapper itemMapper;
    private final ItemPriceHistoryMapper itemPriceHistoryMapper;
    private final ItemMarketSnapshotMapper itemMarketSnapshotMapper;
    private final MarketAnalyticsMapper marketAnalyticsMapper;
    private final UserInventoryMapper userInventoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final SteamApiClient steamApiClient;

    @Override
    public ItemMarketPanel getMarketPanel(Long itemId) {
        if (itemId == null) {
            return new ItemMarketPanel();
        }

        ensureSnapshotFresh(itemId);

        Item item = itemMapper.selectById(itemId);
        ItemMarketSnapshot snapshot = itemMarketSnapshotMapper.selectLatestByItemId(itemId);
        List<ItemPriceHistory> steamHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_STEAM);
        if (steamHistory.size() < 2) {
            if (item != null) {
                refreshSteamHistory(item);
                steamHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_STEAM);
            }
        }
        List<ItemPriceHistory> localHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_LOCAL_TRADE);
        List<RecentTradeRecord> recentTrades = marketAnalyticsMapper.selectRecentCompletedTrades(itemId, 10);
        BigDecimal liveLowestSellPrice = positiveMoney(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId));
        BigDecimal highestBuyPrice = positiveMoney(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId));
        BigDecimal avgTradePrice7d = snapshot != null ? snapshot.getAvgTradePrice7d() : null;
        BigDecimal effectiveLowestSellPrice = firstPositiveMoney(
                liveLowestSellPrice,
                snapshot != null ? snapshot.getLowestSellPrice() : null
        );
        BigDecimal steamOverviewPrice = item != null && positiveMoney(item.getSteamReferencePrice()) == null
                ? fetchSteamReferencePrice(item)
                : null;
        ReferencePriceChoice referenceChoice = resolveReferencePriceChoice(
                item,
                steamOverviewPrice,
                effectiveLowestSellPrice,
                snapshot != null ? snapshot.getReferencePrice() : null,
                snapshot != null ? snapshot.getLatestPrice() : null,
                avgTradePrice7d,
                highestBuyPrice
        );
        PricingDecision pricingDecision = resolvePricingDecision(
                effectiveLowestSellPrice,
                avgTradePrice7d,
                highestBuyPrice,
                referenceChoice.price()
        );
        if (item != null && REFERENCE_SOURCE_STEAM.equals(referenceChoice.source())
                && shouldRefreshSteamHistoryForCurrencyMismatch(steamHistory, referenceChoice.price())) {
            log.warn("Detected possible USD Steam history for itemId={}, referencePrice={}, latestHistoryPrice={}",
                    itemId, referenceChoice.price(), lastPrice(steamHistory));
            refreshSteamHistory(item);
            List<ItemPriceHistory> refreshedSteamHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_STEAM);
            steamHistory = shouldRefreshSteamHistoryForCurrencyMismatch(refreshedSteamHistory, referenceChoice.price())
                    ? List.of()
                    : refreshedSteamHistory;
        }
        boolean useSteamTrend = steamHistory.size() >= 2;
        boolean useLocalTrend = !useSteamTrend && localHistory.size() >= 2;
        String trendSource = useSteamTrend ? SOURCE_STEAM : (useLocalTrend ? SOURCE_LOCAL_TRADE : SOURCE_REFERENCE);
        BigDecimal fallbackTrendPrice = referenceChoice.price();

        ItemMarketPanel panel = new ItemMarketPanel();
        panel.setTrendSource(trendSource);
        panel.setPriceTrend(useSteamTrend || useLocalTrend
                ? buildTrendRanges(useSteamTrend ? steamHistory : localHistory, useSteamTrend)
                : buildReferenceTrendRanges(fallbackTrendPrice));
        panel.setRecentTrades(recentTrades);
        panel.setReferencePrice(referenceChoice.price());
        panel.setReferencePriceSource(referenceChoice.source());
        panel.setHighestBuyPrice(highestBuyPrice);
        panel.setDataNote(resolveMarketPanelNote(trendSource, fallbackTrendPrice));
        panel.setPricingBasis(resolvePricingBasis(
                effectiveLowestSellPrice,
                avgTradePrice7d,
                highestBuyPrice,
                trendSource,
                referenceChoice,
                pricingDecision
        ));

        if (snapshot != null) {
            panel.setLatestPrice(firstPositiveMoney(
                    snapshot.getLatestPrice(),
                    effectiveLowestSellPrice,
                    avgTradePrice7d,
                    highestBuyPrice,
                    referenceChoice.price()
            ));
            panel.setLowestSellPrice(effectiveLowestSellPrice);
            panel.setAvgTradePrice7d(avgTradePrice7d);
            panel.setPriceChange7d(snapshot.getPriceChange7d());
            panel.setPriceChange30d(snapshot.getPriceChange30d());
            panel.setHeatScore(snapshot.getHeatScore());
            panel.setLiquidityScore(snapshot.getLiquidityScore());
            panel.setVolatilityScore(snapshot.getVolatilityScore());
            panel.setSuggestedBuyPrice(pricingDecision.suggestedBuyPrice());
            panel.setSuggestedSellPrice(pricingDecision.suggestedSellPrice());
        } else {
            panel.setLatestPrice(firstPositiveMoney(
                    effectiveLowestSellPrice,
                    avgTradePrice7d,
                    highestBuyPrice,
                    referenceChoice.price()
            ));
            panel.setLowestSellPrice(effectiveLowestSellPrice);
            panel.setAvgTradePrice7d(avgTradePrice7d);
            panel.setSuggestedBuyPrice(pricingDecision.suggestedBuyPrice());
            panel.setSuggestedSellPrice(pricingDecision.suggestedSellPrice());
        }

        return panel;
    }

    @Override
    public InventoryAnalysis getInventoryAnalysis(Long userId) {
        InventoryAnalysis analysis = new InventoryAnalysis();
        analysis.setTotalValue(ZERO_MONEY);
        analysis.setSellableCount(0);
        analysis.setRestrictedCount(0);
        analysis.setRecommendedSellCount(0);

        if (userId == null) {
            return analysis;
        }

        List<UserInventory> inventoryList = userInventoryMapper.selectByUserId(userId);
        Map<Long, ItemMarketSnapshot> snapshotMap = toSnapshotMap(loadLatestSnapshots());
        Map<String, CategoryDistribution> categoryMap = new LinkedHashMap<>();
        List<InventoryRecommendation> recommendations = new ArrayList<>();

        BigDecimal totalValue = ZERO_MONEY;
        int sellableCount = 0;
        int restrictedCount = 0;
        int recommendedSellCount = 0;

        for (UserInventory inventory : inventoryList) {
            Item item = inventory.getItem();
            ItemMarketSnapshot snapshot = snapshotMap.get(inventory.getItemId());
            BigDecimal referencePrice = resolveInventoryReferencePrice(inventory, snapshot);
            BigDecimal highestBuyPrice = positiveMoney(marketAnalyticsMapper.selectHighestActiveBuyPrice(inventory.getItemId()));
            PricingDecision pricingDecision = resolvePricingDecision(
                    snapshot != null ? snapshot.getLowestSellPrice() : null,
                    snapshot != null ? snapshot.getAvgTradePrice7d() : null,
                    highestBuyPrice,
                    referencePrice
            );
            totalValue = totalValue.add(defaultMoney(referencePrice));

            String category = firstNonBlank(item != null ? item.getCategory() : null, "other");
            CategoryDistribution distribution = categoryMap.computeIfAbsent(category, key -> {
                CategoryDistribution dto = new CategoryDistribution();
                dto.setCategory(key);
                dto.setCount(0);
                dto.setValue(ZERO_MONEY);
                return dto;
            });
            distribution.setCount(distribution.getCount() + 1);
            distribution.setValue(distribution.getValue().add(defaultMoney(referencePrice)));

            boolean sellable = canSellInventory(inventory);
            if (sellable) {
                sellableCount++;
            } else {
                restrictedCount++;
            }

            int sellPriorityScore = calculateSellPriorityScore(snapshot, sellable);
            if (sellable && sellPriorityScore >= 75) {
                recommendedSellCount++;
            }

            if (!sellable) {
                continue;
            }

            InventoryRecommendation recommendation = new InventoryRecommendation();
            recommendation.setInventoryId(inventory.getId());
            recommendation.setItemId(inventory.getItemId());
            recommendation.setName(firstNonBlank(
                    inventory.getName(),
                    item != null ? item.getNameCn() : null,
                    item != null ? item.getName() : null,
                    "未知饰品"
            ));
            recommendation.setIconUrl(firstNonBlank(
                    inventory.getIconUrl(),
                    item != null ? item.getIconUrl() : null,
                    "/default-item.svg"
            ));
            recommendation.setReferencePrice(referencePrice);
            recommendation.setSellPriorityScore(sellPriorityScore);
            recommendation.setActionLabel(resolveActionLabel(sellPriorityScore));
            recommendation.setReasonTags(resolveReasonTags(snapshot, true));
            recommendation.setSuggestedSellPrice(firstPositiveMoney(
                    pricingDecision.suggestedSellPrice(),
                    referencePrice
            ));
            recommendations.add(recommendation);
        }

        recommendations.sort(Comparator
                .comparing(InventoryRecommendation::getSellPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(InventoryRecommendation::getReferencePrice, Comparator.nullsLast(Comparator.reverseOrder())));

        List<CategoryDistribution> categoryDistribution = new ArrayList<>(categoryMap.values());
        categoryDistribution.sort(Comparator
                .comparing(CategoryDistribution::getValue, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CategoryDistribution::getCount, Comparator.nullsLast(Comparator.reverseOrder())));

        analysis.setTotalValue(totalValue);
        analysis.setSellableCount(sellableCount);
        analysis.setRestrictedCount(restrictedCount);
        analysis.setRecommendedSellCount(recommendedSellCount);
        analysis.setCategoryDistribution(categoryDistribution);
        analysis.setRecommendations(recommendations.size() > 6 ? recommendations.subList(0, 6) : recommendations);
        return analysis;
    }

    @Override
    public List<ItemRecommendation> getRecommendations(Long userId, Long currentItemId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        List<Item> activeItems = itemMapper.selectAllActive();
        if (activeItems.isEmpty()) {
            return List.of();
        }

        Map<Long, ItemMarketSnapshot> snapshotMap = toSnapshotMap(loadLatestSnapshots());

        if (snapshotMap.isEmpty()) {
            log.info("Recommendation snapshots unavailable, falling back to static item metadata");
        }

        if (currentItemId != null) {
            Item currentItem = activeItems.stream()
                    .filter(item -> item != null && Objects.equals(item.getId(), currentItemId))
                    .findFirst()
                    .orElseGet(() -> itemMapper.selectById(currentItemId));
            List<ItemRecommendation> contextualRecommendations = buildContextRecommendations(
                    currentItem,
                    activeItems,
                    snapshotMap,
                    safeLimit
            );
            if (!contextualRecommendations.isEmpty()) {
                return contextualRecommendations;
            }
        }

        if (userId == null) {
            return buildHotRecommendations(activeItems, snapshotMap, safeLimit);
        }

        List<Favorite> favorites = favoriteMapper.getFavorites(userId, 1);
        List<UserInventory> inventoryList = userInventoryMapper.selectByUserId(userId);
        Set<Long> excludedItemIds = new HashSet<>();
        Map<String, Integer> categoryWeights = new HashMap<>();
        Map<String, Integer> qualityWeights = new HashMap<>();
        Map<String, Integer> exteriorWeights = new HashMap<>();

        accumulateProfileFromFavorites(favorites, excludedItemIds, categoryWeights, qualityWeights, exteriorWeights);
        accumulateProfileFromInventory(inventoryList, excludedItemIds, categoryWeights, qualityWeights, exteriorWeights);

        List<String> topCategories = topKeys(categoryWeights, 2);
        List<String> topQualities = topKeys(qualityWeights, 2);
        List<String> topExteriors = topKeys(exteriorWeights, 1);

        if (topCategories.isEmpty() && topQualities.isEmpty() && topExteriors.isEmpty()) {
            return buildHotRecommendations(activeItems, snapshotMap, safeLimit);
        }

        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (Item item : activeItems) {
            if (item == null || excludedItemIds.contains(item.getId())) {
                continue;
            }

            ItemMarketSnapshot snapshot = snapshotMap.get(item.getId());
            int heatScore = snapshot != null ? defaultInt(snapshot.getHeatScore()) : 0;
            int categoryBonus = topCategories.contains(item.getCategory()) ? 25 : 0;
            int qualityBonus = topQualities.contains(item.getQuality()) ? 15 : 0;
            int exteriorBonus = topExteriors.contains(item.getExterior()) ? 10 : 0;
            int recommendScore = clampScore(Math.round(heatScore * 0.5f) + categoryBonus + qualityBonus + exteriorBonus);
            candidates.add(new RecommendationCandidate(
                    item,
                    snapshot,
                    recommendScore,
                    resolveRecommendationReason(item, categoryBonus, qualityBonus, exteriorBonus, heatScore),
                    heatScore,
                    50
            ));
        }

        candidates.sort(recommendationCandidateComparator());
        List<ItemRecommendation> recommendations = buildRecommendationsFromCandidates(candidates, safeLimit);
        return recommendations.isEmpty()
                ? buildHotRecommendations(activeItems, snapshotMap, safeLimit)
                : recommendations;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildMarketData(Long itemId) {
        if (itemId == null) {
            return;
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return;
        }

        refreshSteamHistory(item);
        refreshLocalTradeHistory(itemId);
        rebuildSnapshot(item);
    }

    @Override
    public void rebuildAllMarketData() {
        List<Item> items = itemMapper.selectAllActive();
        for (Item item : items) {
            try {
                rebuildMarketData(item.getId());
            } catch (Exception e) {
                log.warn("Rebuild market data failed for itemId={}: {}", item.getId(), e.getMessage());
            }
        }
    }

    private void ensureSnapshotFresh(Long itemId) {
        ItemMarketSnapshot latestSnapshot = itemMarketSnapshotMapper.selectLatestByItemId(itemId);
        if (latestSnapshot == null || !LocalDate.now().equals(latestSnapshot.getSnapshotDate())) {
            rebuildMarketData(itemId);
            return;
        }

        Item item = itemMapper.selectById(itemId);
        List<ItemPriceHistory> steamHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_STEAM);
        List<ItemPriceHistory> localHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_LOCAL_TRADE);
        BigDecimal highestBuyPrice = positiveMoney(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId));
        ReferencePriceChoice referenceChoice = resolveReferencePriceChoice(
                item,
                null,
                latestSnapshot.getReferencePrice(),
                latestSnapshot.getLatestPrice(),
                latestSnapshot.getLowestSellPrice(),
                latestSnapshot.getAvgTradePrice7d(),
                highestBuyPrice
        );

        boolean hasTrend = steamHistory.size() >= 2 || localHistory.size() >= 2;
        boolean hasReference = positiveMoney(referenceChoice.price()) != null;
        if (!hasTrend && !hasReference) {
            rebuildMarketData(itemId);
            return;
        }

        PricingDecision pricingDecision = resolvePricingDecision(
                latestSnapshot.getLowestSellPrice(),
                latestSnapshot.getAvgTradePrice7d(),
                highestBuyPrice,
                referenceChoice.price()
        );
        if (isSnapshotPricingStale(latestSnapshot, pricingDecision)) {
            if (item != null) {
                rebuildSnapshot(item);
            } else {
                rebuildMarketData(itemId);
            }
        }
    }

    private boolean isSnapshotPricingStale(ItemMarketSnapshot snapshot, PricingDecision pricingDecision) {
        if (snapshot == null || pricingDecision == null) {
            return false;
        }
        return moneyDiffers(snapshot.getSuggestedSellPrice(), pricingDecision.suggestedSellPrice())
                || moneyDiffers(snapshot.getSuggestedBuyPrice(), pricingDecision.suggestedBuyPrice());
    }

    private boolean moneyDiffers(BigDecimal storedPrice, BigDecimal freshPrice) {
        BigDecimal stored = positiveMoney(storedPrice);
        BigDecimal fresh = positiveMoney(freshPrice);
        if (stored == null || fresh == null) {
            return stored != fresh;
        }
        return stored.subtract(fresh).abs().compareTo(SNAPSHOT_PRICE_DIFF_TOLERANCE) > 0;
    }

    private void refreshSteamHistory(Item item) {
        String marketHashName = resolveMarketHashName(item);
        if (marketHashName == null || marketHashName.isBlank()) {
            return;
        }

        JSONObject payload = steamApiClient.getMarketPriceHistory(marketHashName);
        if (payload == null || !payload.getBooleanValue("success")) {
            return;
        }

        JSONArray prices = payload.getJSONArray("prices");
        if (prices == null || prices.isEmpty()) {
            return;
        }

        List<ItemPriceHistory> histories = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            JSONArray row = prices.getJSONArray(i);
            if (row == null || row.size() < 2) {
                continue;
            }

            LocalDateTime recordedAt = parseSteamHistoryTime(row.getString(0));
            BigDecimal price = parseMoney(row.get(1));
            Integer volume = parseInteger(row.size() > 2 ? row.get(2) : null);
            if (recordedAt == null || price == null) {
                continue;
            }

            ItemPriceHistory history = new ItemPriceHistory();
            history.setItemId(item.getId());
            history.setSource(SOURCE_STEAM);
            history.setRangeType(RANGE_LIFETIME);
            history.setRecordedAt(recordedAt);
            history.setPrice(price);
            history.setVolume(volume != null ? volume : 0);
            histories.add(history);
        }

        if (histories.isEmpty()) {
            return;
        }

        histories.sort(Comparator.comparing(ItemPriceHistory::getRecordedAt));
        itemPriceHistoryMapper.deleteByItemIdAndSource(item.getId(), SOURCE_STEAM);
        for (ItemPriceHistory history : histories) {
            itemPriceHistoryMapper.insert(history);
        }
    }

    private void refreshLocalTradeHistory(Long itemId) {
        List<TradeDailyStat> dailyStats = marketAnalyticsMapper.selectDailyCompletedStats(itemId);
        itemPriceHistoryMapper.deleteByItemIdAndSource(itemId, SOURCE_LOCAL_TRADE);

        for (TradeDailyStat stat : dailyStats) {
            if (stat == null || stat.getTradeDate() == null || stat.getAveragePrice() == null) {
                continue;
            }

            ItemPriceHistory history = new ItemPriceHistory();
            history.setItemId(itemId);
            history.setSource(SOURCE_LOCAL_TRADE);
            history.setRangeType(RANGE_DAILY);
            history.setRecordedAt(stat.getTradeDate().atStartOfDay());
            history.setPrice(scaleMoney(stat.getAveragePrice()));
            history.setVolume(defaultInt(stat.getVolume()));
            itemPriceHistoryMapper.insert(history);
        }
    }

    private void rebuildSnapshot(Item item) {
        Long itemId = item.getId();
        LocalDate today = LocalDate.now();
        itemMarketSnapshotMapper.deleteByItemIdAndSnapshotDate(itemId, today);

        List<ItemPriceHistory> steamHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_STEAM);
        List<ItemPriceHistory> localHistory = itemPriceHistoryMapper.selectByItemIdAndSource(itemId, SOURCE_LOCAL_TRADE);
        List<ItemPriceHistory> baselineHistory = steamHistory.size() >= 2 ? steamHistory : localHistory;

        BigDecimal lowestSellPrice = positiveMoney(marketAnalyticsMapper.selectLowestActiveSellPrice(itemId));
        BigDecimal highestBuyPrice = positiveMoney(marketAnalyticsMapper.selectHighestActiveBuyPrice(itemId));
        BigDecimal avgTradePrice7d = positiveMoney(marketAnalyticsMapper.selectAverageCompletedPriceWithinDays(itemId, 7));
        Integer tradeCount7d = defaultInt(marketAnalyticsMapper.countCompletedWithinDays(itemId, 7));
        Integer tradeCount30d = defaultInt(marketAnalyticsMapper.countCompletedWithinDays(itemId, 30));
        Integer activeSellCount = defaultInt(marketAnalyticsMapper.countActiveSellOrders(itemId));
        Integer favoriteCount = defaultInt(marketAnalyticsMapper.countFavorites(itemId));
        BigDecimal steamOverviewPrice = fetchSteamReferencePrice(item);
        ReferencePriceChoice referenceChoice = resolveReferencePriceChoice(
                item,
                steamOverviewPrice,
                lowestSellPrice,
                avgTradePrice7d,
                highestBuyPrice
        );
        BigDecimal referencePrice = referenceChoice.price();
        BigDecimal latestPrice = firstPositiveMoney(lastPrice(baselineHistory), lowestSellPrice, avgTradePrice7d, highestBuyPrice, referencePrice);
        PricingDecision pricingDecision = resolvePricingDecision(lowestSellPrice, avgTradePrice7d, highestBuyPrice, referencePrice);
        int historyHeatScore = calculateHistoryHeatScore(baselineHistory);
        int historyLiquidityScore = calculateHistoryLiquidityScore(baselineHistory);

        ItemMarketSnapshot snapshot = new ItemMarketSnapshot();
        snapshot.setItemId(itemId);
        snapshot.setSnapshotDate(today);
        snapshot.setLatestPrice(scaleMoney(latestPrice));
        snapshot.setReferencePrice(scaleMoney(referencePrice));
        snapshot.setLowestSellPrice(scaleMoney(lowestSellPrice));
        snapshot.setAvgTradePrice7d(scaleMoney(avgTradePrice7d));
        snapshot.setTradeCount7d(tradeCount7d);
        snapshot.setTradeCount30d(tradeCount30d);
        snapshot.setActiveSellCount(activeSellCount);
        snapshot.setFavoriteCount(favoriteCount);
        snapshot.setPriceChange7d(calculatePriceChange(baselineHistory, 7));
        snapshot.setPriceChange30d(calculatePriceChange(baselineHistory, 30));
        snapshot.setVolatilityScore(calculateVolatilityScore(baselineHistory));
        snapshot.setLiquidityScore(clampScore(historyLiquidityScore + tradeCount7d * 12 + activeSellCount * 4));
        snapshot.setHeatScore(clampScore(historyHeatScore + tradeCount7d * 10 + favoriteCount * 3 + activeSellCount * 2));
        snapshot.setSuggestedSellPrice(pricingDecision.suggestedSellPrice());
        snapshot.setSuggestedBuyPrice(pricingDecision.suggestedBuyPrice());
        itemMarketSnapshotMapper.insert(snapshot);
    }

    private Map<String, List<MarketTrendPoint>> buildTrendRanges(List<ItemPriceHistory> histories, boolean useSteam) {
        Map<String, List<MarketTrendPoint>> result = new LinkedHashMap<>();
        result.put("sevenDays", toTrendPoints(filterHistoryByDays(histories, 7), useSteam ? RANGE_WEEK : RANGE_DAILY));
        result.put("thirtyDays", toTrendPoints(filterHistoryByDays(histories, 30), useSteam ? RANGE_MONTH : RANGE_DAILY));
        result.put("all", toTrendPoints(histories, useSteam ? RANGE_LIFETIME : RANGE_DAILY));
        return result;
    }

    private Map<String, List<MarketTrendPoint>> buildReferenceTrendRanges(BigDecimal referencePrice) {
        Map<String, List<MarketTrendPoint>> result = new LinkedHashMap<>();
        BigDecimal price = positiveMoney(referencePrice);
        if (price == null) {
            result.put("sevenDays", List.of());
            result.put("thirtyDays", List.of());
            result.put("all", List.of());
            return result;
        }

        result.put("sevenDays", toTrendPoints(buildFlatTrendHistory(price, 7), SOURCE_REFERENCE));
        result.put("thirtyDays", toTrendPoints(buildFlatTrendHistory(price, 30), SOURCE_REFERENCE));
        result.put("all", toTrendPoints(buildFlatTrendHistory(price, 30), SOURCE_REFERENCE));
        return result;
    }

    private List<ItemPriceHistory> buildFlatTrendHistory(BigDecimal price, int days) {
        List<ItemPriceHistory> histories = new ArrayList<>();
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        for (int index = 0; index < days; index++) {
            ItemPriceHistory history = new ItemPriceHistory();
            history.setRecordedAt(start.plusDays(index));
            history.setPrice(scaleMoney(price));
            history.setVolume(0);
            histories.add(history);
        }
        return histories;
    }

    private List<MarketTrendPoint> toTrendPoints(List<ItemPriceHistory> histories, String rangeType) {
        List<MarketTrendPoint> points = new ArrayList<>();
        for (ItemPriceHistory history : histories) {
            if (history == null || history.getRecordedAt() == null || history.getPrice() == null) {
                continue;
            }

            MarketTrendPoint point = new MarketTrendPoint();
            point.setRecordedAt(history.getRecordedAt().format(TREND_TIME_FORMATTER));
            point.setLabel(history.getRecordedAt().format(TREND_DAY_FORMATTER));
            point.setPrice(scaleMoney(history.getPrice()));
            point.setVolume(defaultInt(history.getVolume()));
            points.add(point);
        }
        return points;
    }

    private BigDecimal resolveFallbackTrendPrice(ItemMarketSnapshot snapshot, Item item, BigDecimal highestBuyPrice) {
        return firstPositiveMoney(
                snapshot != null ? snapshot.getLatestPrice() : null,
                snapshot != null ? snapshot.getLowestSellPrice() : null,
                snapshot != null ? snapshot.getAvgTradePrice7d() : null,
                highestBuyPrice,
                item != null ? item.getSteamReferencePrice() : null,
                item != null ? item.getBuffPrice() : null
        );
    }

    private ReferencePriceChoice resolveReferencePriceChoice(Item item,
                                                             BigDecimal steamOverviewPrice,
                                                             BigDecimal... localCandidates) {
        BigDecimal steamPrice = firstPositiveMoney(
                item != null ? item.getSteamReferencePrice() : null,
                steamOverviewPrice
        );
        if (steamPrice != null) {
            return new ReferencePriceChoice(steamPrice, REFERENCE_SOURCE_STEAM);
        }

        BigDecimal buffPrice = firstPositiveMoney(item != null ? item.getBuffPrice() : null);
        if (buffPrice != null) {
            return new ReferencePriceChoice(buffPrice, REFERENCE_SOURCE_BUFF);
        }

        BigDecimal localPrice = firstPositiveMoney(localCandidates);
        if (localPrice != null) {
            return new ReferencePriceChoice(localPrice, REFERENCE_SOURCE_LOCAL);
        }

        return ReferencePriceChoice.empty();
    }

    private String resolveMarketPanelNote(String trendSource,
                                          BigDecimal fallbackTrendPrice) {
        if (SOURCE_STEAM.equals(trendSource)) {
            return "已展示 Steam 社区市场历史走势。";
        }
        if (SOURCE_LOCAL_TRADE.equals(trendSource)) {
            return "Steam 历史暂不可用，已回退到本站完成成交日均价。";
        }
        if (positiveMoney(fallbackTrendPrice) != null) {
            return "暂无可用 Steam 历史和本站成交趋势，已按当前参考价绘制保底趋势线。";
        }
        return "暂无可用 Steam 历史、本站成交趋势或参考价。";
    }

    private String resolvePricingBasis(BigDecimal lowestSellPrice,
                                       BigDecimal avgTradePrice7d,
                                       BigDecimal highestBuyPrice,
                                       String trendSource,
                                       ReferencePriceChoice referenceChoice,
                                       PricingDecision pricingDecision) {
        if (pricingDecision == null || positiveMoney(pricingDecision.suggestedSellPrice()) == null) {
            return positiveMoney(highestBuyPrice) != null
                    ? "暂无快照数据，先参考当前最高求购价。"
                    : "暂无可用价格源，建议等待 Steam 或本站成交数据刷新。";
        }

        boolean hasAvgTrade = positiveMoney(avgTradePrice7d) != null;
        boolean hasHighestBuy = positiveMoney(highestBuyPrice) != null;
        boolean hasReference = positiveMoney(referenceChoice.price()) != null;

        if (pricingDecision.ignoredLowestSell()) {
            if (hasAvgTrade && hasReference) {
                return "当前最低在售价明显偏离近 7 日成交价和参考价，已忽略异常挂单，建议卖价按近 7 日成交均价与参考价中的较高值估算，建议买价为卖价的 95%。";
            }
            if (hasAvgTrade) {
                return "当前最低在售价明显偏离近 7 日成交价，已忽略异常挂单，建议卖价参考近 7 日成交均价，建议买价为卖价的 95%。";
            }
            if (hasReference) {
                return "当前最低在售价明显偏离参考价，已忽略异常挂单并按参考价估算建议卖价，建议买价为卖价的 95%。";
            }
            if (hasHighestBuy) {
                return "当前最低在售价明显偏离最高求购价，已忽略异常挂单，建议卖价先参考当前最高求购价，建议买价为卖价的 95%。";
            }
        }

        if (pricingDecision.usedLowestSell() && hasAvgTrade) {
            return "当前最低在售价低于近 7 日成交锚点且未明显异常，建议卖价小幅低于最低在售以提高成交速度，建议买价为卖价的 95%。";
        }
        if (pricingDecision.usedLowestSell()) {
            return "当前最低在售价低于参考价且未明显异常，建议卖价小幅低于最低在售以提高成交速度，建议买价为卖价的 95%。";
        }
        if (hasAvgTrade) {
            return "暂无在售挂单，建议卖价参考近 7 日本站成交均价，建议买价为卖价的 95%。";
        }
        if (hasHighestBuy) {
            return "暂无在售和成交记录，建议价先参考当前最高求购价，等待成交后会自动切换到真实成交口径。";
        }
        if (hasReference) {
            return SOURCE_REFERENCE.equals(trendSource)
                    ? "暂无本站成交和 Steam 历史走势，建议卖价优先参考 Steam/Buff 参考价，避免被异常挂单影响。"
                    : "暂无本站成交，建议卖价优先参考 Steam/Buff 参考价，避免被异常挂单影响。";
        }
        return "暂无可用价格源，建议等待 Steam 或本站成交数据刷新。";
    }

    private String resolvePricingBasis(ItemMarketSnapshot snapshot,
                                       BigDecimal highestBuyPrice,
                                       String trendSource,
                                       ReferencePriceChoice referenceChoice) {
        if (snapshot == null) {
            return positiveMoney(highestBuyPrice) != null
                    ? "暂无快照数据，先参考当前最高求购价。"
                    : "暂无可用价格源，建议等待 Steam 或本站成交数据刷新。";
        }

        boolean hasLowestSell = positiveMoney(snapshot.getLowestSellPrice()) != null;
        boolean hasAvgTrade = positiveMoney(snapshot.getAvgTradePrice7d()) != null;
        boolean hasHighestBuy = positiveMoney(highestBuyPrice) != null;
        boolean hasReference = positiveMoney(snapshot.getReferencePrice()) != null;

        if (hasLowestSell && hasAvgTrade) {
            return "建议卖价优先参考近7日成交和参考价，最低在售只作为盘口辅助。";
        }
        if (hasLowestSell) {
            return "暂无近7日成交，建议卖价优先参考 Steam/Buff 参考价，最低在售只作为盘口辅助。";
        }
        if (hasAvgTrade) {
            return "暂无在售挂单，建议卖价参考近7日本站成交均价，建议买价为卖价的 95%。";
        }
        if (hasHighestBuy) {
            return "暂无在售和成交记录，建议价先参考当前最高求购价，等待成交后会自动切换到真实成交口径。";
        }
        if (hasReference) {
            return SOURCE_REFERENCE.equals(trendSource)
                    ? "暂无本站成交和 Steam 历史走势，建议价暂用 Steam/Buff 参考价兜底。"
                    : "暂无本站成交，建议价暂用 Steam/Buff 参考价兜底。";
        }
        return "暂无可用价格源，建议等待 Steam 或本站成交数据刷新。";
    }

    private List<ItemPriceHistory> filterHistoryByDays(List<ItemPriceHistory> histories, int days) {
        if (histories == null || histories.isEmpty()) {
            return List.of();
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<ItemPriceHistory> filtered = new ArrayList<>();
        for (ItemPriceHistory history : histories) {
            if (history != null && history.getRecordedAt() != null && !history.getRecordedAt().isBefore(cutoff)) {
                filtered.add(history);
            }
        }
        return filtered;
    }

    private int calculateSellPriorityScore(ItemMarketSnapshot snapshot, boolean sellable) {
        float heat = snapshot != null ? defaultInt(snapshot.getHeatScore()) : 0;
        float liquidity = snapshot != null ? defaultInt(snapshot.getLiquidityScore()) : 0;
        float volatility = snapshot != null ? defaultInt(snapshot.getVolatilityScore()) : 0;
        int score = Math.round((heat * 0.45f) + (liquidity * 0.35f) - (volatility * 0.20f) + (sellable ? 10 : 0));
        return clampScore(score);
    }

    private String resolveActionLabel(int score) {
        if (score >= 75) {
            return "建议优先出售";
        }
        if (score >= 55) {
            return "可择机出售";
        }
        return "建议持有观察";
    }

    private List<String> resolveReasonTags(ItemMarketSnapshot snapshot, boolean sellable) {
        List<String> tags = new ArrayList<>();
        if (snapshot != null && defaultInt(snapshot.getLiquidityScore()) >= 60) {
            tags.add("成交活跃");
        }
        if (snapshot != null && defaultInt(snapshot.getHeatScore()) >= 70) {
            tags.add("收藏热度高");
        }
        if (snapshot != null && defaultInt(snapshot.getVolatilityScore()) >= 45) {
            tags.add("波动偏大");
        }
        if (!sellable) {
            tags.add("受交易限制");
        }
        return tags.size() > 2 ? tags.subList(0, 2) : tags;
    }

    private List<ItemRecommendation> buildContextRecommendations(Item currentItem,
                                                                 List<Item> items,
                                                                 Map<Long, ItemMarketSnapshot> snapshotMap,
                                                                 int limit) {
        if (currentItem == null || currentItem.getId() == null) {
            return List.of();
        }

        ItemMarketSnapshot currentSnapshot = snapshotMap.get(currentItem.getId());
        BigDecimal currentReferencePrice = resolveRecommendationReferencePrice(currentItem, currentSnapshot);
        List<RecommendationCandidate> candidates = new ArrayList<>();

        for (Item item : items) {
            if (item == null || Objects.equals(item.getId(), currentItem.getId())) {
                continue;
            }

            ItemMarketSnapshot snapshot = snapshotMap.get(item.getId());
            boolean sameCategory = sameDimension(currentItem.getCategory(), item.getCategory());
            boolean sameSubCategory = sameDimension(currentItem.getSubCategory(), item.getSubCategory());
            boolean sameQuality = sameDimension(currentItem.getQuality(), item.getQuality());
            boolean sameExterior = sameDimension(currentItem.getExterior(), item.getExterior());
            int heatScore = snapshotHeatScore(snapshot);
            int priceBonus = calculateContextPriceBonus(
                    currentReferencePrice,
                    resolveRecommendationReferencePrice(item, snapshot)
            );

            if (!sameCategory && !sameSubCategory && !sameQuality && !sameExterior && priceBonus == 0 && heatScore < 35) {
                continue;
            }

            int recommendScore = clampScore(
                    (sameCategory ? 28 : 0)
                            + (sameSubCategory ? 26 : 0)
                            + (sameQuality ? 14 : 0)
                            + (sameExterior ? 8 : 0)
                            + priceBonus
                            + Math.round(heatScore * 0.35f)
            );

            candidates.add(new RecommendationCandidate(
                    item,
                    snapshot,
                    recommendScore,
                    resolveContextRecommendationReason(sameCategory, sameSubCategory, sameQuality, sameExterior, priceBonus, heatScore),
                    heatScore,
                    35
            ));
        }

        candidates.sort(recommendationCandidateComparator());
        return buildRecommendationsFromCandidates(candidates, limit);
    }

    private List<ItemRecommendation> buildHotRecommendations(List<Item> items,
                                                             Map<Long, ItemMarketSnapshot> snapshotMap,
                                                             int limit) {
        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (Item item : items) {
            if (item == null) {
                continue;
            }

            ItemMarketSnapshot snapshot = snapshotMap.get(item.getId());
            int heatScore = snapshotHeatScore(snapshot);
            candidates.add(new RecommendationCandidate(
                    item,
                    snapshot,
                    heatScore,
                    resolveHotRecommendationReason(heatScore),
                    heatScore,
                    100
            ));
        }

        candidates.sort(recommendationCandidateComparator());
        return buildRecommendationsFromCandidates(candidates, limit);
    }

    private List<ItemRecommendation> buildRecommendationsFromCandidates(List<RecommendationCandidate> candidates, int limit) {
        List<ItemRecommendation> recommendations = new ArrayList<>();
        int batchSize = Math.max(limit * 10, 60);
        Map<Long, SteamMetadataPatch> steamMetadataMap = new HashMap<>();
        Set<Long> attemptedSteamItemIds = new HashSet<>();
        for (int start = 0; start < candidates.size() && recommendations.size() < limit; start += batchSize) {
            int end = Math.min(start + batchSize, candidates.size());
            List<RecommendationCandidate> batch = candidates.subList(start, end);
            Map<Long, String> inventoryIconMap = loadInventoryIconMap(batch);

            for (RecommendationCandidate candidate : batch) {
                String iconUrl = resolveRecommendationIconUrl(
                        candidate.item(),
                        inventoryIconMap,
                        steamMetadataMap,
                        attemptedSteamItemIds
                );
                ItemRecommendation recommendation = buildItemRecommendation(candidate.item(), candidate.snapshot(), iconUrl);
                int resolvedHeatScore = resolveRecommendationHeatScore(
                        candidate.item(),
                        candidate.snapshot(),
                        candidate.heatScore()
                );
                recommendation.setHeatScore(resolvedHeatScore);
                recommendation.setRecommendScore(adjustRecommendationScore(
                        candidate.recommendScore(),
                        candidate.heatScore(),
                        resolvedHeatScore,
                        candidate.heatWeight()
                ));
                recommendation.setRecommendReason(resolveCandidateRecommendationReason(candidate, resolvedHeatScore));
                recommendations.add(recommendation);
                if (recommendations.size() >= limit) {
                    break;
                }
            }
        }
        return recommendations;
    }

    private String resolveCandidateRecommendationReason(RecommendationCandidate candidate, int resolvedHeatScore) {
        if (candidate != null && candidate.heatWeight() == 100) {
            return resolveHotRecommendationReason(resolvedHeatScore);
        }
        return firstNonBlank(candidate != null ? candidate.recommendReason() : null, resolveHotRecommendationReason(resolvedHeatScore));
    }

    private String resolveHotRecommendationReason(int heatScore) {
        int score = clampScore(heatScore);
        if (score >= 70) {
            return "近期热度较高";
        }
        if (score >= 35) {
            return "近期热度稳定";
        }
        if (score > 0) {
            return "有少量行情数据";
        }
        return "行情样本偏少";
    }

    private boolean sameDimension(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return !normalizedLeft.isEmpty() && normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private int calculateContextPriceBonus(BigDecimal currentPrice, BigDecimal candidatePrice) {
        BigDecimal safeCurrentPrice = positiveMoney(currentPrice);
        BigDecimal safeCandidatePrice = positiveMoney(candidatePrice);
        if (safeCurrentPrice == null || safeCandidatePrice == null) {
            return 0;
        }

        BigDecimal diffRatio = safeCandidatePrice.subtract(safeCurrentPrice)
                .abs()
                .divide(safeCurrentPrice, 4, RoundingMode.HALF_UP);

        if (diffRatio.compareTo(new BigDecimal("0.10")) <= 0) {
            return 22;
        }
        if (diffRatio.compareTo(new BigDecimal("0.25")) <= 0) {
            return 16;
        }
        if (diffRatio.compareTo(new BigDecimal("0.50")) <= 0) {
            return 10;
        }
        if (diffRatio.compareTo(BigDecimal.ONE) <= 0) {
            return 4;
        }
        return 0;
    }

    private String resolveContextRecommendationReason(boolean sameCategory,
                                                      boolean sameSubCategory,
                                                      boolean sameQuality,
                                                      boolean sameExterior,
                                                      int priceBonus,
                                                      int heatScore) {
        if (sameSubCategory && priceBonus >= 16) {
            return "同系列相近价位";
        }
        if (sameSubCategory && sameQuality) {
            return "同系列同品质";
        }
        if (sameCategory && sameQuality && priceBonus >= 10) {
            return "同类型同品质";
        }
        if (sameCategory && sameExterior) {
            return "同类型同磨损";
        }
        if (sameCategory && priceBonus >= 16) {
            return "同类型相近价位";
        }
        if (sameSubCategory) {
            return "同系列热门";
        }
        if (sameCategory) {
            return "同类型热门";
        }
        if (priceBonus >= 16) {
            return "相近价位推荐";
        }
        if (heatScore >= 65) {
            return "近期热度较高";
        }
        return "为你推荐";
    }

    private Comparator<RecommendationCandidate> recommendationCandidateComparator() {
        return Comparator
                .comparingInt(RecommendationCandidate::recommendScore)
                .reversed()
                .thenComparing(RecommendationCandidate::heatScore, Comparator.reverseOrder())
                .thenComparing(
                        candidate -> resolveRecommendationReferencePrice(candidate.item(), candidate.snapshot()),
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private ItemRecommendation buildItemRecommendation(Item item, ItemMarketSnapshot snapshot, String iconUrl) {
        ItemRecommendation recommendation = new ItemRecommendation();
        recommendation.setItemId(item.getId());
        recommendation.setName(item.getName());
        recommendation.setNameCn(item.getNameCn());
        recommendation.setIconUrl(iconUrl);
        recommendation.setCategory(item.getCategory());
        recommendation.setQuality(item.getQuality());
        recommendation.setExterior(item.getExterior());
        recommendation.setReferencePrice(resolveRecommendationReferencePrice(item, snapshot));
        recommendation.setHeatScore(snapshotHeatScore(snapshot));
        return recommendation;
    }

    private BigDecimal resolveRecommendationReferencePrice(Item item, ItemMarketSnapshot snapshot) {
        BigDecimal referencePrice = firstPositiveMoney(
                snapshot != null ? snapshot.getReferencePrice() : null,
                item != null ? item.getSteamReferencePrice() : null,
                item != null ? item.getBuffPrice() : null,
                snapshot != null ? snapshot.getLatestPrice() : null
        );
        PricingDecision pricingDecision = resolvePricingDecision(
                snapshot != null ? snapshot.getLowestSellPrice() : null,
                snapshot != null ? snapshot.getAvgTradePrice7d() : null,
                null,
                referencePrice
        );
        return firstPositiveMoney(pricingDecision.suggestedSellPrice(), referencePrice);
    }

    private int snapshotHeatScore(ItemMarketSnapshot snapshot) {
        return snapshot != null ? defaultInt(snapshot.getHeatScore()) : 0;
    }

    private int resolveRecommendationHeatScore(Item item, ItemMarketSnapshot snapshot, int candidateHeatScore) {
        int heatScore = clampScore(Math.max(snapshotHeatScore(snapshot), candidateHeatScore));
        if (heatScore > 0 || item == null || item.getId() == null) {
            return heatScore;
        }

        int steamHeatScore = calculateHistoryHeatScore(
                itemPriceHistoryMapper.selectByItemIdAndSource(item.getId(), SOURCE_STEAM)
        );
        if (steamHeatScore > 0) {
            return steamHeatScore;
        }

        return calculateHistoryHeatScore(
                itemPriceHistoryMapper.selectByItemIdAndSource(item.getId(), SOURCE_LOCAL_TRADE)
        );
    }

    private int adjustRecommendationScore(int recommendScore,
                                          int originalHeatScore,
                                          int resolvedHeatScore,
                                          int heatWeight) {
        int heatDelta = Math.round((resolvedHeatScore - originalHeatScore) * (heatWeight / 100f));
        return clampScore(recommendScore + heatDelta);
    }

    private Map<Long, String> loadInventoryIconMap(List<RecommendationCandidate> candidates) {
        List<Long> itemIds = new ArrayList<>();
        Set<Long> seenItemIds = new HashSet<>();
        for (RecommendationCandidate candidate : candidates) {
            Item item = candidate.item();
            if (item == null || item.getId() == null || !firstUsableIconUrl(item.getIconUrl()).isBlank()) {
                continue;
            }
            if (seenItemIds.add(item.getId())) {
                itemIds.add(item.getId());
            }
        }

        if (itemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> iconMap = new HashMap<>();
        for (ItemIconCandidate candidate : marketAnalyticsMapper.selectBestInventoryIconUrls(itemIds)) {
            if (candidate == null || candidate.getItemId() == null || iconMap.containsKey(candidate.getItemId())) {
                continue;
            }

            String iconUrl = firstUsableIconUrl(candidate.getIconUrl());
            if (!iconUrl.isBlank()) {
                iconMap.put(candidate.getItemId(), iconUrl);
            }
        }
        return iconMap;
    }

    private SteamMetadataPatch resolveRecommendationSteamMetadata(Item item) {
        String resolvedMarketHashName = resolveMarketHashName(item);
        Set<String> searchQueries = new LinkedHashSet<>();
        addSearchQuery(searchQueries, resolvedMarketHashName);
        addSearchQuery(searchQueries, item != null ? item.getItemId() : null);
        addSearchQuery(searchQueries, item != null ? item.getName() : null);
        addSearchQuery(searchQueries, item != null ? item.getNameCn() : null);

        for (String searchQuery : searchQueries) {
            try {
                SteamMarketPageResult result = steamApiClient.searchCsgoItems(searchQuery, RECOMMENDATION_STEAM_SEARCH_LIMIT);
                if (result == null || !result.isSuccess() || result.getPayload() == null) {
                    continue;
                }

                JSONObject matchedResult = findExactSteamSearchResult(result.getPayload().getJSONArray("results"), item);
                if (matchedResult == null) {
                    continue;
                }

                return buildSteamMetadataPatch(matchedResult);
            } catch (Exception e) {
                log.warn("Recommendation Steam metadata lookup failed: itemId={}, query={}, message={}",
                        item != null ? item.getId() : null, searchQuery, e.getMessage());
            }
        }
        return null;
    }

    private void addSearchQuery(Set<String> searchQueries, String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        searchQueries.add(query.trim());
    }

    private JSONObject findExactSteamSearchResult(JSONArray results, Item item) {
        if (results == null || results.isEmpty() || item == null) {
            return null;
        }

        Set<String> targetKeys = new LinkedHashSet<>();
        addNormalizedKey(targetKeys, resolveMarketHashName(item));
        addNormalizedKey(targetKeys, item.getSteamMarketHashName());
        addNormalizedKey(targetKeys, item.getItemId());
        addNormalizedKey(targetKeys, item.getName());
        addNormalizedKey(targetKeys, item.getNameCn());

        for (int index = 0; index < results.size(); index++) {
            JSONObject result = results.getJSONObject(index);
            if (result == null) {
                continue;
            }

            String hashName = extractSteamSearchHashName(result);
            String displayName = firstNonBlank(
                    result.getString("name"),
                    extractSteamSearchAssetField(result, "market_name")
            );
            String normalizedHashName = SteamItemIdentityUtils.normalizeLookupKey(hashName);
            String normalizedDisplayName = SteamItemIdentityUtils.normalizeLookupKey(displayName);
            if (targetKeys.contains(normalizedHashName) || targetKeys.contains(normalizedDisplayName)) {
                return result;
            }
        }
        return null;
    }

    private void addNormalizedKey(Set<String> targetKeys, String value) {
        String normalizedKey = SteamItemIdentityUtils.normalizeLookupKey(value);
        if (normalizedKey != null) {
            targetKeys.add(normalizedKey);
        }
    }

    private SteamMetadataPatch buildSteamMetadataPatch(JSONObject steamResult) {
        if (steamResult == null) {
            return null;
        }

        String marketHashName = extractSteamSearchHashName(steamResult);
        String steamMarketUrl = SteamItemIdentityUtils.buildSteamMarketUrl(marketHashName);
        String iconUrl = firstUsableIconUrl(
                SteamItemIdentityUtils.normalizeSteamIconUrl(extractSteamSearchAssetField(steamResult, "icon_url_large")),
                SteamItemIdentityUtils.normalizeSteamIconUrl(extractSteamSearchAssetField(steamResult, "icon_url")),
                SteamItemIdentityUtils.normalizeSteamIconUrl(steamResult.getString("icon_url"))
        );
        BigDecimal steamReferencePrice = parseSteamSearchPrice(steamResult);
        if (marketHashName.isBlank() && steamMarketUrl == null && iconUrl.isBlank() && steamReferencePrice == null) {
            return null;
        }
        return new SteamMetadataPatch(
                iconUrl.isBlank() ? null : iconUrl,
                marketHashName.isBlank() ? null : marketHashName,
                steamMarketUrl,
                steamReferencePrice
        );
    }

    private String extractSteamSearchHashName(JSONObject steamResult) {
        return firstNonBlank(
                steamResult != null ? steamResult.getString("market_hash_name") : null,
                steamResult != null ? steamResult.getString("hash_name") : null,
                extractSteamSearchAssetField(steamResult, "market_hash_name"),
                extractSteamSearchAssetField(steamResult, "market_name")
        );
    }

    private String extractSteamSearchAssetField(JSONObject steamResult, String fieldName) {
        if (steamResult == null || fieldName == null || fieldName.isBlank()) {
            return "";
        }

        JSONObject assetDescription = steamResult.getJSONObject("asset_description");
        if (assetDescription == null) {
            return "";
        }
        return firstNonBlank(assetDescription.getString(fieldName));
    }

    private BigDecimal parseSteamSearchPrice(JSONObject steamResult) {
        if (steamResult == null) {
            return null;
        }

        Object sellPriceRaw = steamResult.get("sell_price");
        if (sellPriceRaw instanceof Number sellPriceNumber && sellPriceNumber.longValue() > 0) {
            return scaleMoney(BigDecimal.valueOf(sellPriceNumber.longValue(), 2));
        }

        BigDecimal price = parseMoney(steamResult.get("sell_price_text"));
        if (price != null) {
            return price;
        }
        return parseMoney(steamResult.get("price"));
    }

    private void persistRecommendationSteamMetadata(Item item, SteamMetadataPatch metadataPatch) {
        if (item == null || item.getId() == null || metadataPatch == null || metadataPatch.isEmpty()) {
            return;
        }

        Item updatedItem = new Item();
        updatedItem.setId(item.getId());
        boolean dirty = false;

        if (metadataPatch.iconUrl() != null && !Objects.equals(item.getIconUrl(), metadataPatch.iconUrl())) {
            updatedItem.setIconUrl(metadataPatch.iconUrl());
            item.setIconUrl(metadataPatch.iconUrl());
            dirty = true;
        }
        if (metadataPatch.steamMarketHashName() != null
                && !Objects.equals(item.getSteamMarketHashName(), metadataPatch.steamMarketHashName())) {
            updatedItem.setSteamMarketHashName(metadataPatch.steamMarketHashName());
            item.setSteamMarketHashName(metadataPatch.steamMarketHashName());
            dirty = true;
        }
        if (metadataPatch.steamMarketUrl() != null
                && !Objects.equals(item.getSteamMarketUrl(), metadataPatch.steamMarketUrl())) {
            updatedItem.setSteamMarketUrl(metadataPatch.steamMarketUrl());
            item.setSteamMarketUrl(metadataPatch.steamMarketUrl());
            dirty = true;
        }
        if (metadataPatch.steamReferencePrice() != null
                && !Objects.equals(positiveMoney(item.getSteamReferencePrice()), positiveMoney(metadataPatch.steamReferencePrice()))) {
            BigDecimal scaledPrice = scaleMoney(metadataPatch.steamReferencePrice());
            updatedItem.setSteamReferencePrice(scaledPrice);
            updatedItem.setSteamReferenceCurrency("CNY");
            updatedItem.setSteamReferencePriceSource("steam_market_search");
            updatedItem.setSteamReferencePriceUpdatedAt(LocalDateTime.now());
            item.setSteamReferencePrice(scaledPrice);
            item.setSteamReferenceCurrency("CNY");
            item.setSteamReferencePriceSource("steam_market_search");
            item.setSteamReferencePriceUpdatedAt(updatedItem.getSteamReferencePriceUpdatedAt());
            dirty = true;
        }

        if (!dirty) {
            return;
        }

        try {
            itemMapper.updateById(updatedItem);
        } catch (Exception e) {
            log.warn("Failed to persist recommendation Steam metadata: itemId={}, message={}",
                    item.getId(), e.getMessage());
        }
    }

    private String resolveRecommendationIconUrl(Item item,
                                                Map<Long, String> inventoryIconMap,
                                                Map<Long, SteamMetadataPatch> steamMetadataMap,
                                                Set<Long> attemptedSteamItemIds) {
        if (item == null) {
            return "";
        }

        String itemIconUrl = firstUsableIconUrl(item.getIconUrl());
        if (!itemIconUrl.isBlank()) {
            return itemIconUrl;
        }

        if (item.getId() == null) {
            return "";
        }

        String inventoryIconUrl = firstUsableIconUrl(inventoryIconMap.get(item.getId()));
        if (!inventoryIconUrl.isBlank()) {
            return inventoryIconUrl;
        }

        if (item.getId() == null) {
            return "";
        }

        SteamMetadataPatch metadataPatch = steamMetadataMap.get(item.getId());
        if (metadataPatch == null && attemptedSteamItemIds.add(item.getId())) {
            metadataPatch = resolveRecommendationSteamMetadata(item);
            if (metadataPatch != null && !metadataPatch.isEmpty()) {
                persistRecommendationSteamMetadata(item, metadataPatch);
                steamMetadataMap.put(item.getId(), metadataPatch);
            }
        }
        if (metadataPatch == null) {
            return "";
        }
        return firstUsableIconUrl(metadataPatch.iconUrl());
    }

    private String firstUsableIconUrl(String... values) {
        for (String value : values) {
            if (isUsableIconUrl(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isUsableIconUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("default-item.svg")
                && !normalized.equals("default-item.png")
                && !normalized.endsWith("/default-item.svg")
                && !normalized.endsWith("/default-item.png")
                && !normalized.contains(STEAM_APP_ICON_URL_FRAGMENT);
    }

    private boolean hasRecommendationIcon(ItemRecommendation recommendation) {
        return recommendation != null && isUsableIconUrl(recommendation.getIconUrl());
    }

    private record RecommendationCandidate(
            Item item,
            ItemMarketSnapshot snapshot,
            int recommendScore,
            String recommendReason,
            int heatScore,
            int heatWeight
    ) {
    }

    private record PricingDecision(
            BigDecimal suggestedSellPrice,
            BigDecimal suggestedBuyPrice,
            BigDecimal anchorPrice,
            boolean usedLowestSell,
            boolean ignoredLowestSell
    ) {
    }

    private record SteamMetadataPatch(
            String iconUrl,
            String steamMarketHashName,
            String steamMarketUrl,
            BigDecimal steamReferencePrice
    ) {
        private boolean isEmpty() {
            return iconUrl == null
                    && steamMarketHashName == null
                    && steamMarketUrl == null
                    && steamReferencePrice == null;
        }
    }

    private void accumulateProfileFromFavorites(List<Favorite> favorites,
                                                Set<Long> excludedItemIds,
                                                Map<String, Integer> categoryWeights,
                                                Map<String, Integer> qualityWeights,
                                                Map<String, Integer> exteriorWeights) {
        for (Favorite favorite : favorites) {
            if (favorite == null || favorite.getItemId() == null) {
                continue;
            }
            excludedItemIds.add(favorite.getItemId());
            Item item = favorite.getItem();
            if (item == null) {
                continue;
            }
            incrementWeight(categoryWeights, item.getCategory());
            incrementWeight(qualityWeights, item.getQuality());
            incrementWeight(exteriorWeights, item.getExterior());
        }
    }

    private void accumulateProfileFromInventory(List<UserInventory> inventoryList,
                                                Set<Long> excludedItemIds,
                                                Map<String, Integer> categoryWeights,
                                                Map<String, Integer> qualityWeights,
                                                Map<String, Integer> exteriorWeights) {
        for (UserInventory inventory : inventoryList) {
            if (inventory == null || inventory.getItemId() == null) {
                continue;
            }
            excludedItemIds.add(inventory.getItemId());
            Item item = inventory.getItem();
            if (item == null) {
                continue;
            }
            incrementWeight(categoryWeights, item.getCategory());
            incrementWeight(qualityWeights, item.getQuality());
            incrementWeight(exteriorWeights, firstNonBlank(item.getExterior(), inventory.getExterior()));
        }
    }

    private void incrementWeight(Map<String, Integer> weightMap, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        weightMap.merge(key, 1, Integer::sum);
    }

    private List<String> topKeys(Map<String, Integer> weightMap, int limit) {
        return weightMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String resolveRecommendationReason(Item item,
                                               int categoryBonus,
                                               int qualityBonus,
                                               int exteriorBonus,
                                               int heatScore) {
        if (categoryBonus > 0) {
            return "与你常关注的" + firstNonBlank(item.getCategory(), "饰品类型") + "风格接近";
        }
        if (qualityBonus > 0) {
            return "与你偏好的品质区间相似";
        }
        if (exteriorBonus > 0) {
            return "与你常浏览的磨损区间相似";
        }
        if (heatScore >= 60) {
            return "近7日成交活跃";
        }
        return "近期热度稳定";
    }

    private Map<Long, ItemMarketSnapshot> toSnapshotMap(List<ItemMarketSnapshot> snapshots) {
        Map<Long, ItemMarketSnapshot> snapshotMap = new HashMap<>();
        for (ItemMarketSnapshot snapshot : snapshots) {
            if (snapshot != null && snapshot.getItemId() != null) {
                snapshotMap.put(snapshot.getItemId(), snapshot);
            }
        }
        return snapshotMap;
    }

    private List<ItemMarketSnapshot> loadLatestSnapshots() {
        return itemMarketSnapshotMapper.selectLatestAll();
    }

    private BigDecimal resolveInventoryReferencePrice(UserInventory inventory, ItemMarketSnapshot snapshot) {
        return firstPositiveMoney(
                snapshot != null ? snapshot.getReferencePrice() : null,
                inventory.getItem() != null ? inventory.getItem().getSteamReferencePrice() : null,
                inventory.getItem() != null ? inventory.getItem().getBuffPrice() : null,
                inventory.getMarketPrice(),
                snapshot != null ? snapshot.getLatestPrice() : null,
                snapshot != null ? snapshot.getLowestSellPrice() : null,
                snapshot != null ? snapshot.getAvgTradePrice7d() : null
        );
    }

    private boolean canSellInventory(UserInventory inventory) {
        if (inventory == null) {
            return false;
        }

        boolean marketable = Objects.equals(inventory.getIsMarketable(), UserInventory.IS_MARKETABLE);
        boolean normalStatus = Objects.equals(inventory.getStatus(), UserInventory.STATUS_NORMAL);
        String reason = firstNonBlank(inventory.getMarketableReason()).toLowerCase(Locale.ROOT);
        boolean blockedByReason = reason.contains("hold")
                || reason.contains("restricted")
                || reason.contains("cooldown")
                || reason.contains("不可交易")
                || reason.contains("限制")
                || reason.contains("冷却");
        return marketable && normalStatus && !blockedByReason;
    }

    private int calculateHistoryHeatScore(List<ItemPriceHistory> histories) {
        List<ItemPriceHistory> recent7d = filterHistoryByDays(histories, 7);
        List<ItemPriceHistory> recent30d = filterHistoryByDays(histories, 30);
        if (recent7d.isEmpty() && recent30d.isEmpty()) {
            return 0;
        }

        int volume7d = sumHistoryActivityUnits(recent7d);
        int volume30d = sumHistoryActivityUnits(recent30d);
        int tradingDays7d = countHistoryTradingDays(recent7d);
        int momentumScore = Math.min(14, calculatePriceChange(histories, 7)
                .abs()
                .setScale(0, RoundingMode.HALF_UP)
                .intValue());

        return clampScore(
                Math.min(56, volume7d * 4)
                        + Math.min(18, volume30d / 2)
                        + Math.min(12, tradingDays7d * 2)
                        + momentumScore
        );
    }

    private int calculateHistoryLiquidityScore(List<ItemPriceHistory> histories) {
        List<ItemPriceHistory> recent7d = filterHistoryByDays(histories, 7);
        List<ItemPriceHistory> recent30d = filterHistoryByDays(histories, 30);
        if (recent7d.isEmpty() && recent30d.isEmpty()) {
            return 0;
        }

        int volume7d = sumHistoryActivityUnits(recent7d);
        int volume30d = sumHistoryActivityUnits(recent30d);
        int tradingDays30d = countHistoryTradingDays(recent30d);

        return clampScore(
                Math.min(62, volume7d * 5)
                        + Math.min(26, volume30d)
                        + Math.min(12, tradingDays30d)
        );
    }

    private int sumHistoryActivityUnits(List<ItemPriceHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (ItemPriceHistory history : histories) {
            if (history == null) {
                continue;
            }

            int volume = defaultInt(history.getVolume());
            if (volume > 0) {
                total += volume;
            } else if (positiveMoney(history.getPrice()) != null) {
                total += 1;
            }
        }
        return total;
    }

    private int countHistoryTradingDays(List<ItemPriceHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return 0;
        }

        Set<LocalDate> tradingDays = new HashSet<>();
        for (ItemPriceHistory history : histories) {
            if (history != null && history.getRecordedAt() != null && historyActivityUnits(history) > 0) {
                tradingDays.add(history.getRecordedAt().toLocalDate());
            }
        }
        return tradingDays.size();
    }

    private int historyActivityUnits(ItemPriceHistory history) {
        if (history == null) {
            return 0;
        }

        int volume = defaultInt(history.getVolume());
        if (volume > 0) {
            return volume;
        }
        return positiveMoney(history.getPrice()) != null ? 1 : 0;
    }

    private Integer calculateVolatilityScore(List<ItemPriceHistory> histories) {
        List<ItemPriceHistory> recentHistory = filterHistoryByDays(histories, 7);
        if (recentHistory.size() < 3) {
            return 0;
        }

        double mean = 0;
        for (ItemPriceHistory history : recentHistory) {
            mean += history.getPrice().doubleValue();
        }
        mean = mean / recentHistory.size();
        if (mean <= 0) {
            return 0;
        }

        double variance = 0;
        for (ItemPriceHistory history : recentHistory) {
            double diff = history.getPrice().doubleValue() - mean;
            variance += diff * diff;
        }
        variance = variance / recentHistory.size();
        double stddev = Math.sqrt(variance);
        return clampScore((int) Math.round((stddev / mean) * 100));
    }

    private BigDecimal calculatePriceChange(List<ItemPriceHistory> histories, int days) {
        if (histories == null || histories.size() < 2) {
            return ZERO_MONEY;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        ItemPriceHistory latest = histories.get(histories.size() - 1);
        ItemPriceHistory baseline = null;
        for (ItemPriceHistory history : histories) {
            if (history.getRecordedAt() != null && !history.getRecordedAt().isBefore(cutoff)) {
                baseline = history;
                break;
            }
        }
        if (baseline == null) {
            baseline = histories.get(0);
        }
        if (baseline == null || baseline.getPrice() == null || baseline.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_MONEY;
        }

        return latest.getPrice()
                .subtract(baseline.getPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(baseline.getPrice(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal fetchSteamReferencePrice(Item item) {
        if (item == null) {
            return null;
        }

        String marketHashName = resolveMarketHashName(item);
        if (marketHashName == null || marketHashName.isBlank()) {
            return null;
        }

        JSONObject overview = steamApiClient.getMarketPriceOverview(marketHashName);
        if (overview == null || !overview.getBooleanValue("success")) {
            return null;
        }

        return firstPositiveMoney(
                parseMoney(overview.get("lowest_price")),
                parseMoney(overview.get("median_price"))
        );
    }

    private PricingDecision resolvePricingDecision(BigDecimal lowestSellPrice,
                                                   BigDecimal avgTradePrice7d,
                                                   BigDecimal highestBuyPrice,
                                                   BigDecimal referencePrice) {
        BigDecimal averageCandidate = positiveMoney(avgTradePrice7d);
        BigDecimal referenceCandidate = positiveMoney(referencePrice);
        BigDecimal buyCandidate = positiveMoney(highestBuyPrice);
        BigDecimal anchorPrice = firstPositiveMoney(averageCandidate, referenceCandidate, buyCandidate);

        BigDecimal trustedLowestCandidate = positiveMoney(lowestSellPrice);
        boolean ignoredLowestSell = false;
        if (trustedLowestCandidate != null && anchorPrice != null) {
            BigDecimal highLimit = scaleMoney(anchorPrice.multiply(ABNORMAL_LOWEST_SELL_HIGH_MULTIPLIER));
            BigDecimal lowLimit = scaleMoney(anchorPrice.multiply(ABNORMAL_LOWEST_SELL_LOW_MULTIPLIER));
            ignoredLowestSell = trustedLowestCandidate.compareTo(highLimit) > 0
                    || trustedLowestCandidate.compareTo(lowLimit) < 0;
            if (ignoredLowestSell) {
                trustedLowestCandidate = null;
            }
        }

        boolean usedLowestSell = false;
        BigDecimal undercutLowestSell = null;
        if (trustedLowestCandidate != null) {
            undercutLowestSell = trustedLowestCandidate.subtract(PRICE_UNDERCUT);
            if (undercutLowestSell.compareTo(PRICE_UNDERCUT) < 0) {
                undercutLowestSell = PRICE_UNDERCUT;
            }
        }

        BigDecimal suggestedSellCandidate;
        if (anchorPrice != null) {
            if (undercutLowestSell != null && undercutLowestSell.compareTo(anchorPrice) < 0) {
                suggestedSellCandidate = undercutLowestSell;
                usedLowestSell = true;
            } else {
                suggestedSellCandidate = anchorPrice;
            }
        } else {
            suggestedSellCandidate = undercutLowestSell;
            usedLowestSell = positiveMoney(undercutLowestSell) != null;
        }

        BigDecimal suggestedSellPrice = scaleMoney(suggestedSellCandidate);
        if (positiveMoney(suggestedSellPrice) == null) {
            suggestedSellPrice = ZERO_MONEY;
        }

        BigDecimal suggestedBuyPrice = positiveMoney(suggestedSellPrice) != null
                ? scaleMoney(suggestedSellPrice.multiply(BUY_DISCOUNT))
                : ZERO_MONEY;
        return new PricingDecision(
                suggestedSellPrice,
                suggestedBuyPrice,
                anchorPrice,
                usedLowestSell,
                ignoredLowestSell
        );
    }

    private BigDecimal calculateSuggestedSellPrice(BigDecimal lowestSellPrice,
                                                   BigDecimal avgTradePrice7d,
                                                   BigDecimal highestBuyPrice,
                                                   BigDecimal referencePrice) {
        return resolvePricingDecision(
                lowestSellPrice,
                avgTradePrice7d,
                highestBuyPrice,
                referencePrice
        ).suggestedSellPrice();
    }

    private BigDecimal calculateSuggestedBuyPrice(BigDecimal suggestedSellPrice, BigDecimal referencePrice) {
        BigDecimal sellPrice = firstPositiveMoney(suggestedSellPrice, referencePrice);
        if (sellPrice == null) {
            return ZERO_MONEY;
        }
        return scaleMoney(sellPrice.multiply(BUY_DISCOUNT));
    }

    private BigDecimal maxPositiveMoney(BigDecimal first, BigDecimal second) {
        BigDecimal a = positiveMoney(first);
        BigDecimal b = positiveMoney(second);
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.max(b);
    }

    private BigDecimal lastPrice(List<ItemPriceHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return null;
        }
        return histories.get(histories.size() - 1).getPrice();
    }

    private boolean shouldRefreshSteamHistoryForCurrencyMismatch(List<ItemPriceHistory> steamHistory,
                                                                 BigDecimal referencePrice) {
        BigDecimal latestHistoryPrice = positiveMoney(lastPrice(steamHistory));
        BigDecimal steamReferencePrice = positiveMoney(referencePrice);
        if (latestHistoryPrice == null || steamReferencePrice == null) {
            return false;
        }

        BigDecimal ratio = steamReferencePrice.divide(latestHistoryPrice, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(new BigDecimal("5.50")) >= 0
                && ratio.compareTo(new BigDecimal("8.50")) <= 0;
    }

    private String resolveMarketHashName(Item item) {
        if (item == null) {
            return "";
        }

        return firstNonBlank(
                normalizeMarketHashNameCandidate(item.getSteamMarketHashName(), item),
                extractMarketHashNameFromUrl(item.getSteamMarketUrl()),
                normalizeMarketHashNameCandidate(item.getItemId(), item)
        );
    }

    private String extractMarketHashNameFromUrl(String steamMarketUrl) {
        if (steamMarketUrl == null || steamMarketUrl.isBlank()) {
            return "";
        }

        String normalizedUrl = steamMarketUrl.trim();
        int markerIndex = normalizedUrl.indexOf("/market/listings/730/");
        if (markerIndex < 0) {
            return "";
        }

        String marketHashName = normalizedUrl.substring(markerIndex + "/market/listings/730/".length());
        int queryIndex = marketHashName.indexOf('?');
        if (queryIndex >= 0) {
            marketHashName = marketHashName.substring(0, queryIndex);
        }
        int hashIndex = marketHashName.indexOf('#');
        if (hashIndex >= 0) {
            marketHashName = marketHashName.substring(0, hashIndex);
        }

        try {
            return URLDecoder.decode(marketHashName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Unable to decode Steam market URL: {}", steamMarketUrl);
            return marketHashName;
        }
    }

    private String normalizeMarketHashNameCandidate(String rawName, Item item) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }

        String marketHashName = rawName.trim();
        if (isLikelyInternalItemId(marketHashName) || containsCjkCharacters(marketHashName) || marketHashName.indexOf('\uFFFD') >= 0) {
            return "";
        }

        if (hasStatTrak(item) && !containsStatTrak(marketHashName) && marketHashName.contains(" | ")) {
            marketHashName = "StatTrak\u2122 " + marketHashName;
        }

        String exterior = toSteamExteriorName(item.getExterior());
        if (!exterior.isBlank() && marketHashName.contains(" | ") && !marketHashName.contains("(")) {
            return marketHashName + " (" + exterior + ")";
        }

        return marketHashName;
    }

    private boolean isLikelyInternalItemId(String value) {
        return value != null && value.matches("^[a-z]+_[a-z0-9_]+_\\d+$");
    }

    private boolean containsCjkCharacters(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(index));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.HANGUL_SYLLABLES) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStatTrak(Item item) {
        return containsStatTrak(item.getSteamMarketHashName())
                || containsStatTrak(item.getItemId())
                || containsStatTrak(item.getName())
                || containsStatTrak(item.getNameCn());
    }

    private boolean containsStatTrak(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("stattrak");
    }

    private String toSteamExteriorName(String exterior) {
        if (exterior == null || exterior.isBlank()) {
            return "";
        }

        switch (exterior.trim().toUpperCase(Locale.ROOT)) {
            case "FN":
                return "Factory New";
            case "MW":
                return "Minimal Wear";
            case "FT":
                return "Field-Tested";
            case "WW":
                return "Well-Worn";
            case "BS":
                return "Battle-Scarred";
            default:
                return "";
        }
    }

    private LocalDateTime parseSteamHistoryTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.replace(",", " ").trim();
        Matcher matcher = STEAM_TIME_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        String parseText = matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3) + " " + matcher.group(4);
        try {
            return LocalDateTime.parse(parseText, STEAM_HISTORY_FORMATTER);
        } catch (Exception e) {
            log.debug("Unable to parse Steam history time: {}", raw);
            return null;
        }
    }

    private BigDecimal parseMoney(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return scaleMoney(BigDecimal.valueOf(number.doubleValue()));
        }

        Matcher matcher = PRICE_PATTERN.matcher(String.valueOf(raw).replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        return scaleMoney(new BigDecimal(matcher.group(1)));
    }

    private Integer parseInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }

        Matcher matcher = INTEGER_PATTERN.matcher(String.valueOf(raw).replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? scaleMoney(value) : ZERO_MONEY;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return scaleMoney(value);
    }

    private BigDecimal firstPositiveMoney(BigDecimal... candidates) {
        for (BigDecimal candidate : candidates) {
            BigDecimal money = positiveMoney(candidate);
            if (money != null) {
                return money;
            }
        }
        return null;
    }

    private static final class ReferencePriceChoice {
        private final BigDecimal price;
        private final String source;

        private ReferencePriceChoice(BigDecimal price, String source) {
            this.price = price;
            this.source = source;
        }

        private static ReferencePriceChoice empty() {
            return new ReferencePriceChoice(null, null);
        }

        private BigDecimal price() {
            return price;
        }

        private String source() {
            return source;
        }
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
