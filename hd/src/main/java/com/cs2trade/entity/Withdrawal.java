package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Withdrawal implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private BigDecimal amount;
    private String bankName;
    private String bankAccount;
    private String accountName;
    private Integer status; // 0待审核 1已通过 2已拒绝
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
