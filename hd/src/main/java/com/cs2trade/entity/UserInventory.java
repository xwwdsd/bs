package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户库存实体类
 * 对应数据库表 user_inventory
 * 存储用户的Steam库存饰品信息
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class UserInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 库存ID - 主键，自增
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
     * Steam Asset ID
     */
    private String assetId;

    /**
     * Steam Class ID
     */
    private String classId;

    /**
     * Steam Instance ID
     */
    private String instanceId;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品图标URL
     */
    private String iconUrl;

    /**
     * 物品大图URL
     */
    private String iconUrlLarge;

    /**
     * 外观磨损
     */
    private String exterior;

    /**
     * 图案模板号
     */
    private Integer paintSeed;

    /**
     * 皮肤编号 / Finish Catalog
     */
    private Integer paintIndex;

    /**
     * 磨损度数值
     */
    private BigDecimal paintWear;

    /**
     * 检视链接
     */
    private String inspectUrl;

    /**
     * 物品描述
     */
    private String description;

    /**
     * 不可出售/不可交易的原因
     */
    private String marketableReason;

    /**
     * 前端展示用稀有度
     */
    private String rarity;

    /**
     * 前端展示用类型
     */
    private String type;

    /**
     * 贴纸/挂件等附加信息
     */
    private List<StickerInfo> stickers;

    /**
     * 是否可交易
     * 0: 否
     * 1: 是
     */
    private Integer isMarketable;

    /**
     * 市场价格
     */
    private BigDecimal marketPrice;

    /**
     * 获取时间
     */
    private LocalDateTime acquiredAt;

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

    // ==================== 常量定义 ====================

    /**
     * 是否可交易 - 否
     */
    public static final int NOT_MARKETABLE = 0;

    /**
     * 是否可交易 - 是
     */
    public static final int IS_MARKETABLE = 1;

    /**
     * 状态 - 正常
     */
    public static final int STATUS_NORMAL = 0;

    /**
     * 状态 - 已售出
     */
    public static final int STATUS_SOLD = 1;

    /**
     * 状态 - 交易中
     */
    public static final int STATUS_TRADING = 2;

    /**
     * 状态 - 在售
     */
    public static final int STATUS_ON_SALE = 3;

    /**
     * 库存状态
     * 0: 正常
     * 1: 已售出
     * 2: 交易中
     * 3: 在售
     */
    private Integer status;

    @Data
    public static class StickerInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String iconUrl;
        private BigDecimal wear;
    }
}
