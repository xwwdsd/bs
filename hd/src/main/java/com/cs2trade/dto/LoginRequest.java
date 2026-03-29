package com.cs2trade.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求DTO
 * 封装用户登录时提交的数据
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class LoginRequest {

    /**
     * 登录账号（用户名或邮箱）
     * 支持用户名或邮箱登录
     */
    @NotBlank(message = "用户名/邮箱不能为空")
    private String account;

    /**
     * 登录密码
     * 必须是非空且长度在6-20个字符之间
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;
}
