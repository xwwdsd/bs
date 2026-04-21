package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryAnalysis implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal totalValue;
    private Integer sellableCount;
    private Integer restrictedCount;
    private Integer recommendedSellCount;
    private List<CategoryDistribution> categoryDistribution = new ArrayList<>();
    private List<InventoryRecommendation> recommendations = new ArrayList<>();
}
