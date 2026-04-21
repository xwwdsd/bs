package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradeDailyStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate tradeDate;
    private BigDecimal averagePrice;
    private Integer volume;
}
