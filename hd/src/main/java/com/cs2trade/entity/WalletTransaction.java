package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包交易流水实体类
 * 对应数据库表 wallet_transaction
 * 存储用户钱包的所有交易记录
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class WalletTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易类型
     * 1: 充值
     * 2: 提现
     * 3: 收入
     * 4: 支出
     * 5: 冻结
     * 6: 解冻
     */
    private Integer type;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 交易后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 关联订单ID
     */
    private Long relatedOrderId;

    private String orderNo;

    /**
     * 关联订单类型
     */
    private String relatedOrderType;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ==================== 常量定义 ====================

    /** 交易类型 - 充值 */
    public static final int TYPE_RECHARGE = 1;

    /** 交易类型 - 提现 */
    public static final int TYPE_WITHDRAW = 2;

    /** 交易类型 - 收入 */
    public static final int TYPE_INCOME = 3;

    /** 交易类型 - 支出 */
    public static final int TYPE_EXPENSE = 4;

    /** 交易类型 - 冻结 */
    public static final int TYPE_FREEZE = 5;

    /** 交易类型 - 解冻 */
    public static final int TYPE_UNFREEZE = 6;

    /**
     * 获取交易类型文本
     */
    public String getTypeText() {
        switch (type) {
            case TYPE_RECHARGE: return "充值";
            case TYPE_WITHDRAW: return "提现";
            case TYPE_INCOME: return "收入";
            case TYPE_EXPENSE: return "支出";
            case TYPE_FREEZE: return "冻结";
            case TYPE_UNFREEZE: return "解冻";
            default: return "未知";
        }
    }

    /**
     * 是否为收入类型
     */
    public boolean isIncome() {
        return type == TYPE_RECHARGE || type == TYPE_INCOME || type == TYPE_UNFREEZE;
    }

    /**
     * 是否为支出类型
     */
    public boolean isExpense() {
        return type == TYPE_WITHDRAW || type == TYPE_EXPENSE || type == TYPE_FREEZE;
    }
}
