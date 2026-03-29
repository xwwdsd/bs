package com.cs2trade.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户登录响应DTO
 * 封装用户登录成功后返回的数据
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 访问令牌(Access Token)
     * 用于后续的API请求认证，有效期较短
     */
    private String accessToken;

    /**
     * 刷新令牌(Refresh Token)
     * 用于在Access Token过期后获取新的Token，有效期较长
     */
    private String refreshToken;

    /**
     * Token类型，通常为"Bearer"
     */
    private String tokenType;

    /**
     * Access Token过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户基本信息
     */
    private UserInfo userInfo;

    /**
     * 用户基本信息内部类
     */
    @Data
    @Builder
    public static class UserInfo {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 用户邮箱
         */
        private String email;

        /**
         * 用户头像URL
         */
        private String avatar;

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
         * Steam是否已绑定
         */
        private Boolean steamBound;
    }
}
