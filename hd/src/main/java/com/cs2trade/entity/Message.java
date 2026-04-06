package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消息实体类
 * 支持交易消息、系统消息、还价留言
 */
@Data
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int TYPE_TRADE = 1;
    public static final int TYPE_SYSTEM = 2;
    public static final int TYPE_BARGAIN = 3;
    public static final int BARGAIN_STATUS_PENDING = 1;
    public static final int BARGAIN_STATUS_ACCEPTED = 2;
    public static final int BARGAIN_STATUS_REJECTED = 3;

    private Long id;
    private Long userId;
    private String title;
    private String content;
    
    /**
     * 消息类型
     * 1: 交易消息
     * 2: 系统消息
     * 3: 还价留言
     */
    private Integer type;
    
    /**
     * 子类型
     * 交易消息: 1-购买 2-出售 3-完成 4-取消 5-退款
     * 系统消息: 1-重要 2-活动 3-通知 4-公告
     */
    private Integer subType;
    
    /**
     * 状态: 0-未读, 1-已读
     */
    private Integer status;
    
    /**
     * 关联订单号
     */
    private String relatedOrderNo;
    
    /**
     * 关联商品ID
     */
    private Long relatedItemId;
    private Long relatedSellOrderId;
    
    /**
     * 关联商品名称
     */
    private String itemName;
    
    /**
     * 还价金额（仅还价留言使用）
     */
    private BigDecimal bargainPrice;
    
    /**
     * 发送者ID（还价留言使用）
     */
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    private LocalDateTime createdAt;
}
