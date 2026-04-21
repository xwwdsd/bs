package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ItemMarketPanel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, List<MarketTrendPoint>> priceTrend = new LinkedHashMap<>();
    private String trendSource;
    private List<RecentTradeRecord> recentTrades;
    private BigDecimal latestPrice;
    private BigDecimal referencePrice;
    private String referencePriceSource;
    private BigDecimal lowestSellPrice;
    private BigDecimal highestBuyPrice;
    private BigDecimal avgTradePrice7d;
    private BigDecimal priceChange7d;
    private BigDecimal priceChange30d;
    private Integer heatScore;
    private Integer liquidityScore;
    private Integer volatilityScore;
    private BigDecimal suggestedBuyPrice;
    private BigDecimal suggestedSellPrice;
    private String dataNote;
    private String pricingBasis;
}
