package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饰品实体类
 * 对应数据库表 item
 * 存储CS2饰品的基础信息
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 饰品ID - 主键，自增
     */
    private Long id;

    /**
     * 游戏内物品ID
     */
    private String itemId;

    /**
     * 饰品英文名称
     */
    private String name;

    /**
     * 饰品中文名称
     */
    private String nameCn;

    /**
     * 饰品分类
     * weapon: 武器
     * knife: 刀具
     * glove: 手套
     * sticker: 贴纸
     * case: 箱子
     * other: 其他
     */
    private String category;

    /**
     * 子分类（如武器类型：步枪、狙击枪等）
     */
    private String subCategory;

    /**
     * 品质
     * covert: 隐秘
     * classified: 保密
     * restricted: 受限
     * mil-spec: 军规级
     * industrial: 工业级
     * consumer: 消费级
     */
    private String quality;

    /**
     * 稀有度
     */
    private String rarity;

    /**
     * 外观磨损类型
     * FN: 崭新出厂
     * MW: 略有磨损
     * FT: 久经沙场
     * WW: 破损不堪
     * BS: 战痕累累
     */
    private String exterior;

    /**
     * 饰品图标URL
     */
    private String iconUrl;

    /**
     * 检视链接模板
     */
    private String inspectLinkTemplate;

    /**
     * Steam市场链接
     */
    private String steamMarketUrl;

    /**
     * Buff参考价格
     */
    private BigDecimal buffPrice;

    /**
     * 是否启用
     * 0: 禁用
     * 1: 启用
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ==================== 常量定义 ====================

    /**
     * 分类 - 武器
     */
    public static final String CATEGORY_WEAPON = "weapon";

    /**
     * 分类 - 刀具
     */
    public static final String CATEGORY_KNIFE = "knife";

    /**
     * 分类 - 手套
     */
    public static final String CATEGORY_GLOVE = "glove";

    /**
     * 分类 - 贴纸
     */
    public static final String CATEGORY_STICKER = "sticker";

    /**
     * 分类 - 箱子
     */
    public static final String CATEGORY_CASE = "case";

    /**
     * 分类 - 其他
     */
    public static final String CATEGORY_OTHER = "other";

    /**
     * 品质 - 隐秘
     */
    public static final String QUALITY_COVERT = "covert";

    /**
     * 品质 - 保密
     */
    public static final String QUALITY_CLASSIFIED = "classified";

    /**
     * 品质 - 受限
     */
    public static final String QUALITY_RESTRICTED = "restricted";

    /**
     * 品质 - 军规级
     */
    public static final String QUALITY_MIL_SPEC = "mil-spec";

    /**
     * 品质 - 工业级
     */
    public static final String QUALITY_INDUSTRIAL = "industrial";

    /**
     * 品质 - 消费级
     */
    public static final String QUALITY_CONSUMER = "consumer";

    /**
     * 外观 - 崭新出厂
     */
    public static final String EXTERIOR_FN = "FN";

    /**
     * 外观 - 略有磨损
     */
    public static final String EXTERIOR_MW = "MW";

    /**
     * 外观 - 久经沙场
     */
    public static final String EXTERIOR_FT = "FT";

    /**
     * 外观 - 破损不堪
     */
    public static final String EXTERIOR_WW = "WW";

    /**
     * 外观 - 战痕累累
     */
    public static final String EXTERIOR_BS = "BS";

    /**
     * 状态 - 禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 状态 - 启用
     */
    public static final int STATUS_ENABLED = 1;
}
