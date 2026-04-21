package com.cs2trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 * 封装用户注册时提交的数据
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class RegisterRequest {

    /**
     * 用户名
     * 用于显示的名称，支持中文、英文、数字，长度2-20个字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @Pattern(regexp = "^[\u4e00-\u9fa5a-zA-Z0-9_]+$", message = "用户名只能包含中文、英文、数字和下划线")
    private String username;

    /**
     * 用户邮箱
     * 可选字段，用于登录和接收通知
     */
    private String email;

    /**
     * 登录密码
     * 长度6-20个字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /**
     * 确认密码
     * 必须与密码字段一致
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
