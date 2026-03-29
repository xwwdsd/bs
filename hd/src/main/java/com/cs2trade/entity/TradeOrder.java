package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单实体类
 * 对应数据库表 trade_order
 * 存储实际成交的交易订单信息
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class TradeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 交易ID - 主键，自增
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 饰品ID
     */
    private Long itemId;

    /**
     * 库存ID
     */
    private Long inventoryId;

    /**
     * 成交价格
     */
    private BigDecimal price;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 实际到账金额
     */
    private BigDecimal actualAmount;

    /**
     * 订单状态
     * 0: 待确认
     * 1: 报价中
     * 2: 待发货
     * 3: 待收货
     * 4: 已完成
     * 5: 已取消
     * 6: 纠纷中
     */
    private Integer status;

    /**
     * Steam交易报价ID
     */
    private String tradeOfferId;

    /**
     * 交易报价链接
     */
    private String tradeOfferUrl;

    /**
     * 付款时间
     */
    private LocalDateTime paidAt;

    /**
     * 发货时间
     */
    private LocalDateTime sentAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 取消时间
     */
    private LocalDateTime cancelledAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ==================== 关联对象 ====================

    /**
     * 关联的饰品信息
     */
    private Item item;

    /**
     * 关联的买家信息
     */
    private User buyer;

    /**
     * 关联的卖家信息
     */
    private User seller;

    // ==================== 常量定义 ====================

    /** 状态 - 待确认 */
    public static final int STATUS_PENDING = 0;

    /** 状态 - 报价中 */
    public static final int STATUS_OFFERING = 1;

    /** 状态 - 待发货 */
    public static final int STATUS_PAID = 2;

    /** 状态 - 待收货 */
    public static final int STATUS_SENT = 3;

    /** 状态 - 已完成 */
    public static final int STATUS_COMPLETED = 4;

    /** 状态 - 已取消 */
    public static final int STATUS_CANCELLED = 5;

    /** 状态 - 纠纷中 */
    public static final int STATUS_DISPUTE = 6;

    /**
     * 获取状态文本
     */
    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING: return "待确认";
            case STATUS_OFFERING: return "报价中";
            case STATUS_PAID: return "待发货";
            case STATUS_SENT: return "待收货";
            case STATUS_COMPLETED: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            case STATUS_DISPUTE: return "纠纷中";
            default: return "未知";
        }
    }
}
