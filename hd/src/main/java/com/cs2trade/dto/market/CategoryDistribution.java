package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CategoryDistribution implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;
    private Integer count;
    private BigDecimal value;
}
