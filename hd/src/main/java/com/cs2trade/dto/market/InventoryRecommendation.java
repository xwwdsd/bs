package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long inventoryId;
    private Long itemId;
    private String name;
    private String iconUrl;
    private BigDecimal referencePrice;
    private Integer sellPriorityScore;
    private String actionLabel;
    private List<String> reasonTags = new ArrayList<>();
    private BigDecimal suggestedSellPrice;
}
