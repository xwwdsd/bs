package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包实体类
 * 对应数据库表 user_wallet
 * 存储用户的资金信息
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class Wallet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 钱包ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额（交易中）
     */
    private BigDecimal frozenAmount;

    /**
     * 求购金
     */
    private BigDecimal buyOrderAmount;

    /**
     * 冻结求购金
     */
    private BigDecimal frozenBuyOrderAmount;

    /**
     * 累计充值
     */
    private BigDecimal totalRecharge;

    /**
     * 累计提现
     */
    private BigDecimal totalWithdraw;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
