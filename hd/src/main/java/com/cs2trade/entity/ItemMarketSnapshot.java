package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ItemMarketSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long itemId;
    private LocalDate snapshotDate;
    private BigDecimal latestPrice;
    private BigDecimal referencePrice;
    private BigDecimal lowestSellPrice;
    private BigDecimal avgTradePrice7d;
    private Integer tradeCount7d;
    private Integer tradeCount30d;
    private Integer activeSellCount;
    private Integer favoriteCount;
    private BigDecimal priceChange7d;
    private BigDecimal priceChange30d;
    private Integer volatilityScore;
    private Integer liquidityScore;
    private Integer heatScore;
    private BigDecimal suggestedBuyPrice;
    private BigDecimal suggestedSellPrice;
    private LocalDateTime createdAt;
}
