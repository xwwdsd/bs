package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ItemRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String name;
    private String nameCn;
    private String iconUrl;
    private String category;
    private String quality;
    private String exterior;
    private BigDecimal referencePrice;
    private Integer heatScore;
    private Integer recommendScore;
    private String recommendReason;
}
