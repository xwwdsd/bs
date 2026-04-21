package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecentTradeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime completedAt;
    private BigDecimal price;
    private String source;
}
