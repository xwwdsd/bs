package com.cs2trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 sys_user
 * 存储平台用户的基本信息
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID - 主键，自增
     */
    private Long id;

    /**
     * 用户名 - 用户自定义名称，用于显示
     */
    private String username;

    /**
     * 邮箱 - 用于登录和接收通知，唯一
     */
    private String email;

    /**
     * 手机号 - 用于绑定和验证
     */
    private String phone;

    /**
     * 密码 - BCrypt加密存储
     */
    @JsonIgnore
    private String password;

    /**
     * 头像URL - 用户头像图片地址
     */
    private String avatar;

    /**
     * Steam64ID - Steam平台用户唯一标识
     */
    private String steamId;

    /**
     * Steam交易链接 - 用于接收交易报价
     */
    private String steamTradeUrl;

    /**
     * Steam API Key - 用于库存同步等功能
     */
    @JsonIgnore
    private String steamApiKey;

    /**
     * 账号状态
     * 0: 禁用
     * 1: 正常
     * 2: 待验证
     */
    private Integer status;

    /**
     * 用户等级
     * 0: 普通用户
     * 1: VIP用户
     * 2: 管理员
     * 3: 超级管理员
     */
    private Integer userLevel;

    /**
     * 是否实名认证
     * 0: 未认证
     * 1: 已认证
     */
    private Integer isRealName;

    /**
     * 支付宝账号
     */
    private String alipayAccount;

    /**
     * 支付宝真实姓名
     */
    private String alipayRealName;

    /**
     * 银行卡号
     */
    private String bankCardNumber;

    /**
     * 银行卡真实姓名
     */
    private String bankRealName;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行预留手机号
     */
    private String bankPhone;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

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
     * 账号状态 - 禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 账号状态 - 正常
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 账号状态 - 待验证
     */
    public static final int STATUS_PENDING = 2;

    /**
     * 用户等级 - 普通用户
     */
    public static final int LEVEL_NORMAL = 0;

    /**
     * 用户等级 - VIP用户
     */
    public static final int LEVEL_VIP = 1;

    /**
     * 用户等级 - 管理员
     */
    public static final int LEVEL_ADMIN = 2;

    /**
     * 用户等级 - 超级管理员
     */
    public static final int LEVEL_SUPER_ADMIN = 3;

    /**
     * 实名认证状态 - 未认证
     */
    public static final int REAL_NAME_NO = 0;

    /**
     * 实名认证状态 - 已认证
     */
    public static final int REAL_NAME_YES = 1;
}
