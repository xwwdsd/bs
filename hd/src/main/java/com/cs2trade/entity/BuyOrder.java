package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 求购订单实体类
 * 对应数据库表 buy_order
 * 存储用户的求购订单信息
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class BuyOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 求购ID - 主键，自增
     */
    private Long id;

    /**
     * 用户ID - 关联sys_user表
     */
    private Long userId;

    /**
     * 饰品ID - 关联item表
     */
    private Long itemId;

    /**
     * 求购单价
     */
    private BigDecimal price;

    /**
     * 求购数量
     */
    private Integer quantity;

    /**
     * 已成交数量
     */
    private Integer filledQuantity;

    /**
     * 订单状态
     * 0: 已取消
     * 1: 进行中
     * 2: 已完成
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否自动接受
     * 0: 否
     * 1: 是
     */
    private Integer autoAccept;

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
     * 关联的用户信息
     */
    private User user;

    // ==================== 常量定义 ====================

    /**
     * 状态 - 已取消
     */
    public static final int STATUS_CANCELLED = 0;

    /**
     * 状态 - 进行中
     */
    public static final int STATUS_ACTIVE = 1;

    /**
     * 状态 - 已完成
     */
    public static final int STATUS_COMPLETED = 2;

    /**
     * 自动接受 - 否
     */
    public static final int AUTO_ACCEPT_NO = 0;

    /**
     * 自动接受 - 是
     */
    public static final int AUTO_ACCEPT_YES = 1;
}
