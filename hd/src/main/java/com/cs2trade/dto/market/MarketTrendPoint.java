package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class MarketTrendPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordedAt;
    private String label;
    private BigDecimal price;
    private Integer volume;
}
