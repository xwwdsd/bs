package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ItemPriceHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long itemId;
    private String source;
    private BigDecimal price;
    private Integer volume;
    private LocalDateTime recordedAt;
    private String rangeType;
    private LocalDateTime createdAt;
}
