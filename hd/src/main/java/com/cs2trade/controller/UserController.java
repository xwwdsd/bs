package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.User;
import com.cs2trade.security.CustomUserDetails;
import com.cs2trade.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户个人信息相关的HTTP请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }

        throw new RuntimeException("获取用户ID失败");
    }

    /**
     * 获取用户个人信息
     */
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = getCurrentUserId();
        User user = userService.getUserById(userId);
        user.setPassword(null); // 不返回密码
        user.setSteamApiKey(null); // 不返回API Key
        return Result.success(user);
    }

    /**
     * 更新用户个人信息
     */
    @PostMapping("/update")
    public Result<User> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        log.info("更新用户信息: userId={}, nickname={}, email={}", userId, request.getNickname(), request.getEmail());
        
        User updatedUser = userService.updateProfile(userId, request.getNickname(), request.getAvatar(), request.getEmail());
        updatedUser.setPassword(null);
        updatedUser.setSteamApiKey(null);
        
        return Result.success(updatedUser);
    }

    /**
     * 更新交易链接
     */
    @PostMapping("/update-trade-url")
    public Result<Boolean> updateTradeUrl(@RequestBody UpdateTradeUrlRequest request) {
        Long userId = getCurrentUserId();
        String tradeUrl = request.getTradeUrl();
        
        log.info("更新交易链接: userId={}, url={}", userId, tradeUrl);
        
        if (tradeUrl == null || tradeUrl.isEmpty()) {
            return Result.error("交易链接不能为空");
        }
        
        // 简单验证格式
        if (!tradeUrl.contains("steamcommunity.com/tradeoffer/new")) {
            return Result.error("交易链接格式不正确");
        }
        
        boolean success = userService.updateSteamTradeUrl(userId, tradeUrl);
        return Result.success("交易链接更新成功", success);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = getCurrentUserId();
        log.info("修改密码: userId={}", userId);
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("两次输入的新密码不一致");
        }
        
        User user = userService.getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error("旧密码错误");
        }
        
        boolean success = userService.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
        return Result.success("密码修改成功", success);
    }

    /**
     * 获取用户支付账号信息
     */
    @GetMapping("/payment-accounts")
    public Result<PaymentAccountsResponse> getPaymentAccounts() {
        Long userId = getCurrentUserId();
        log.info("获取支付账号信息: userId={}", userId);
        
        User user = userService.getUserById(userId);
        PaymentAccountsResponse response = new PaymentAccountsResponse();
        response.setPhone(user.getPhone());
        response.setAlipay(user.getAlipayAccount());
        response.setAlipayRealName(user.getAlipayRealName());
        response.setBankCard(user.getBankCardNumber());
        response.setBankRealName(user.getBankRealName());
        response.setBankName(user.getBankName());
        response.setBankPhone(user.getBankPhone());
        
        return Result.success(response);
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    public Result<Boolean> bindPhone(@RequestBody BindPhoneRequest request) {
        Long userId = getCurrentUserId();
        log.info("绑定手机号: userId={}, phone={}", userId, request.getPhone());
        
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            return Result.error("手机号不能为空");
        }
        
        if (!request.getPhone().matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }
        
        boolean success = userService.bindPhone(userId, request.getPhone());
        return Result.success("绑定成功", success);
    }

    /**
     * 绑定支付宝
     */
    @PostMapping("/bind-alipay")
    public Result<Boolean> bindAlipay(@RequestBody BindAlipayRequest request) {
        Long userId = getCurrentUserId();
        log.info("绑定支付宝: userId={}, account={}", userId, request.getAccount());
        
        if (request.getAccount() == null || request.getAccount().isEmpty()) {
            return Result.error("支付宝账号不能为空");
        }
        
        boolean success = userService.bindAlipay(userId, request.getAccount(), request.getRealName());
        return Result.success("绑定成功", success);
    }

    /**
     * 绑定银行卡
     */
    @PostMapping("/bind-bank")
    public Result<Boolean> bindBank(@RequestBody BindBankRequest request) {
        Long userId = getCurrentUserId();
        log.info("绑定银行卡: userId={}, cardNumber={}", userId, request.getCardNumber());
        
        if (request.getCardNumber() == null || request.getCardNumber().isEmpty()) {
            return Result.error("银行卡号不能为空");
        }
        
        if (request.getRealName() == null || request.getRealName().isEmpty()) {
            return Result.error("持卡人姓名不能为空");
        }
        
        if (request.getBankName() == null || request.getBankName().isEmpty()) {
            return Result.error("开户银行不能为空");
        }
        
        boolean success = userService.bindBank(userId, request.getRealName(), request.getCardNumber(), 
                request.getBankName(), request.getPhone());
        return Result.success("绑定成功", success);
    }

    // ==================== 请求类 ====================

    @lombok.Data
    public static class UpdateProfileRequest {
        private String nickname;
        private String email;
        private String avatar;
    }

    @lombok.Data
    public static class UpdateTradeUrlRequest {
        private String tradeUrl;
    }
    
    @lombok.Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;
    }

    @lombok.Data
    public static class BindPhoneRequest {
        private String phone;
        private String code;
    }

    @lombok.Data
    public static class BindAlipayRequest {
        private String account;
        private String realName;
    }

    @lombok.Data
    public static class BindBankRequest {
        private String realName;
        private String cardNumber;
        private String bankName;
        private String phone;
    }

    @lombok.Data
    public static class PaymentAccountsResponse {
        private String phone;
        private String alipay;
        private String alipayRealName;
        private String bankCard;
        private String bankRealName;
        private String bankName;
        private String bankPhone;
    }
}
