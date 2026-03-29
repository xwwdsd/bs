package com.cs2trade.controller;

import com.cs2trade.dto.LoginRequest;
import com.cs2trade.dto.LoginResponse;
import com.cs2trade.dto.RegisterRequest;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证控制器
 * 处理用户认证相关的请求，包括注册、登录等
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    /**
     * 用户服务
     */
    private final UserService userService;

    /**
     * 用户Mapper
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册接口
     * 接收用户注册信息，创建新用户账号
     * 
     * @param request 注册请求DTO，包含用户名、邮箱、密码等
     * @return Result 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求: email={}", request.getEmail());
        
        try {
            // 调用服务层处理注册逻辑
            User user = userService.register(request);
            
            log.info("注册成功: userId={}", user.getId());
            return Result.success("注册成功", null);
        } catch (RuntimeException e) {
            // 业务异常，返回错误信息
            log.warn("注册失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            // 系统异常
            log.error("注册发生异常", e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录接口
     * 验证用户凭据，返回JWT令牌
     * 支持用户名或邮箱登录
     *
     * @param request 登录请求DTO，包含账号（用户名/邮箱）和密码
     * @param httpRequest HTTP请求对象，用于获取客户端IP
     * @return Result 包含Token和用户信息的登录结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        log.info("收到登录请求: account={}", request.getAccount());
        
        try {
            // 获取客户端IP地址
            String ipAddress = getClientIpAddress(httpRequest);
            
            // 调用服务层处理登录逻辑
            LoginResponse response = userService.login(request, ipAddress);
            
            log.info("登录成功: account={}", request.getAccount());
            return Result.success("登录成功", response);
        } catch (RuntimeException e) {
            // 业务异常，返回错误信息
            log.warn("登录失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            // 系统异常
            log.error("登录发生异常", e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 检查邮箱是否已注册
     * 用于前端实时校验
     * 
     * @param email 邮箱地址
     * @return Result 检查结果
     */
    @GetMapping("/check-email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailExists(email);
        return Result.success(exists);
    }

    /**
     * 检查用户名是否已存在
     * 用于前端实时校验
     *
     * @param username 用户名
     * @return Result 检查结果
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.isUsernameExists(username);
        return Result.success(exists);
    }

    /**
     * 重置管理员密码（临时接口）
     * @return Result 重置结果
     */
    @GetMapping("/reset-admin")
    public Result<String> resetAdminPassword() {
        try {
            User user = userMapper.selectByUsername("admin");
            if (user == null) {
                return Result.error("管理员账号不存在");
            }
            String newPassword = passwordEncoder.encode("admin123");
            user.setPassword(newPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("管理员密码已重置: {}", newPassword);
            return Result.success("密码已重置为 admin123", newPassword);
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("重置失败");
        }
    }

    /**
     * 获取客户端IP地址
     * 处理代理服务器的情况
     * 
     * @param request HTTP请求对象
     * @return String 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果存在多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
