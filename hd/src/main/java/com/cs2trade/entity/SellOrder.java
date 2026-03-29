package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出售订单实体类
 * 对应数据库表 sell_order
 * 存储用户的出售(供应)订单信息
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class SellOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供应ID - 主键，自增
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
     * 库存ID - 关联user_inventory表
     */
    private Long inventoryId;

    /**
     * 出售价格
     */
    private BigDecimal price;

    /**
     * 订单状态
     * 0: 已取消
     * 1: 在售
     * 2: 交易中
     * 3: 已售出
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否响应求购
     * 0: 否
     * 1: 是
     */
    private Integer isResponseToBuy;

    /**
     * 目标求购订单ID
     */
    private Long targetBuyOrderId;

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

    /**
     * 关联的库存信息
     */
    private UserInventory inventory;

    // ==================== 常量定义 ====================

    /**
     * 状态 - 已取消
     */
    public static final int STATUS_CANCELLED = 0;

    /**
     * 状态 - 在售
     */
    public static final int STATUS_ON_SALE = 1;

    /**
     * 状态 - 交易中
     */
    public static final int STATUS_TRADING = 2;

    /**
     * 状态 - 已售出
     */
    public static final int STATUS_SOLD = 3;

    /**
     * 是否响应求购 - 否
     */
    public static final int RESPONSE_NO = 0;

    /**
     * 是否响应求购 - 是
     */
    public static final int RESPONSE_YES = 1;
}
