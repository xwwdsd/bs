package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.User;
import com.cs2trade.security.CustomUserDetails;
import com.cs2trade.service.SteamInventoryService;
import com.cs2trade.service.SteamService;
import com.cs2trade.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Steam控制器
 * 处理Steam账号绑定、库存同步等相关请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/steam")
@RequiredArgsConstructor
@CrossOrigin
public class SteamController {

    private final SteamService steamService;
    private final SteamInventoryService steamInventoryService;
    private final UserService userService;

    /**
     * 获取Steam登录URL
     * 用于引导用户到Steam进行OAuth授权
     *
     * @return Result<String> Steam登录URL
     */
    @GetMapping("/login-url")
    public Result<String> getSteamLoginUrl() {
        String loginUrl = steamService.getSteamLoginUrl();
        return Result.success(loginUrl);
    }

    /**
     * 处理Steam回调
     * 验证Steam OpenID并绑定账号
     *
     * @param params OpenID回调参数
     * @return Result<User> 更新后的用户信息
     */
    @GetMapping("/callback")
    public Result<User> handleSteamCallback(@RequestParam Map<String, String> params) {
        Long userId = getCurrentUserId();
        log.info("处理Steam回调: userId={}, params={}", userId, params);

        User user = steamService.handleSteamCallback(userId, params);
        return Result.success("Steam账号绑定成功", user);
    }

    /**
     * 直接绑定Steam账号
     * 通过交易链接自动提取SteamID并绑定
     *
     * @param request 绑定请求
     * @return Result<Boolean> 绑定结果
     */
    @PostMapping("/bind")
    public Result<Boolean> bindSteamAccount(@RequestBody BindSteamRequest request) {
        Long userId = getCurrentUserId();
        log.info("绑定Steam账号: userId={}", userId);

        // 验证交易链接
        if (request.getTradeUrl() == null || request.getTradeUrl().isEmpty()) {
            return Result.error("请输入交易链接");
        }

        // 验证API Key
        if (request.getApiKey() == null || request.getApiKey().isEmpty()) {
            return Result.error("请输入Steam API Key");
        }
        
        // 验证API Key是否有效
        // 注意：Steam API可能访问不稳定，这里暂时跳过API Key的有效性验证，或者可以捕获异常
        // if (!steamService.validateApiKey(request.getApiKey())) {
        //    return Result.error("Steam API Key无效，请检查API Key是否正确");
        // }

        // 从交易链接提取SteamID
        String steamId = steamService.extractSteamIdFromTradeUrl(request.getTradeUrl());
        if (steamId == null || steamId.isEmpty()) {
            // 尝试直接使用请求中的SteamID（如果有）
            // 这里我们假设交易链接格式是 https://steamcommunity.com/tradeoffer/new/?partner=1017201901&token=Ou9jTAyD
            // partner ID 需要转换为 Steam64 ID
            // Steam64 ID = partner ID + 76561197960265728
            try {
                String partnerIdStr = request.getTradeUrl().split("partner=")[1].split("&")[0];
                long partnerId = Long.parseLong(partnerIdStr);
                long steam64Id = partnerId + 76561197960265728L;
                steamId = String.valueOf(steam64Id);
            } catch (Exception e) {
                return Result.error("无法从交易链接提取SteamID，请检查交易链接格式是否正确");
            }
        }

        log.info("从交易链接提取到SteamID: {}", steamId);

        boolean success = steamService.bindSteamAccount(userId, steamId, request.getTradeUrl());
        if (success) {
            // 保存API Key
            try {
                steamService.saveSteamApiKey(userId, request.getApiKey());
            } catch (Exception e) {
                log.warn("保存API Key失败: {}", e.getMessage());
            }
            return Result.success("Steam账号绑定成功", true);
        }
        return Result.error("Steam账号绑定失败");
    }

    /**
     * 解绑Steam账号
     *
     * @return Result<Boolean> 解绑结果
     */
    @PostMapping("/unbind")
    public Result<Boolean> unbindSteamAccount() {
        Long userId = getCurrentUserId();
        log.info("解绑Steam账号: userId={}", userId);

        boolean success = steamService.unbindSteamAccount(userId);
        if (success) {
            return Result.success("Steam账号解绑成功", true);
        }
        return Result.error("Steam账号解绑失败");
    }

    /**
     * 获取当前用户的Steam绑定状态
     *
     * @return Result<SteamBindStatus> 绑定状态
     */
    @GetMapping("/status")
    public Result<SteamBindStatus> getSteamStatus() {
        Long userId = getCurrentUserId();
        log.info("获取Steam绑定状态: userId={}", userId);

        User user = userService.getUserById(userId);
        SteamBindStatus status = new SteamBindStatus();

        if (user != null && user.getSteamId() != null && !user.getSteamId().isEmpty()) {
            status.setBound(true);
            status.setSteamId(user.getSteamId());
            // 如果已绑定交易链接，也返回给前端
            if (user.getSteamTradeUrl() != null && !user.getSteamTradeUrl().isEmpty()) {
                status.setTradeUrl(user.getSteamTradeUrl());
            }

            // 获取Steam用户信息
            // 优先尝试从本地缓存或数据库获取，如果需要实时更新再调用API
            try {
                // 这里可以优化：如果Steam API调用失败，不要影响整体状态返回
                // 我们可以返回默认头像和名字，或者直接从user对象中获取（如果user表存了的话）
                // 暂时还是尝试调用API，但捕获异常
                try {
                    Map<String, Object> userInfo = steamService.getSteamUserInfo(user.getSteamId());
                    if (userInfo != null) {
                        status.setPersonaName((String) userInfo.get("personaName"));
                        status.setAvatar((String) userInfo.get("avatar"));
                        status.setProfileUrl((String) userInfo.get("profileUrl"));
                    }
                } catch (Exception e) {
                    log.warn("调用Steam API获取用户信息失败，使用默认信息: {}", e.getMessage());
                    status.setPersonaName("Steam用户");
                    status.setAvatar("/default-avatar.png");
                }
            } catch (Exception e) {
                log.error("获取Steam用户信息失败: {}", e.getMessage());
            }
        } else {
            status.setBound(false);
        }

        return Result.success(status);
    }

    /**
     * 验证SteamID是否有效
     *
     * @param steamId Steam64位ID
     * @return Result<Boolean> 验证结果
     */
    @GetMapping("/validate-id")
    public Result<Boolean> validateSteamId(@RequestParam String steamId) {
        boolean valid = steamService.validateSteamId(steamId);
        return Result.success(valid);
    }

    /**
     * 验证交易链接是否有效
     *
     * @param tradeUrl Steam交易链接
     * @return Result<Boolean> 验证结果
     */
    @GetMapping("/validate-trade-url")
    public Result<Boolean> validateTradeUrl(@RequestParam String tradeUrl) {
        boolean valid = steamService.validateTradeUrl(tradeUrl);
        return Result.success(valid);
    }

    /**
     * 从交易链接提取SteamID
     *
     * @param tradeUrl Steam交易链接
     * @return Result<String> Steam64位ID
     */
    @GetMapping("/extract-id")
    public Result<String> extractSteamIdFromTradeUrl(@RequestParam String tradeUrl) {
        String steamId = steamService.extractSteamIdFromTradeUrl(tradeUrl);
        if (steamId != null) {
            return Result.success(steamId);
        }
        return Result.error("无法从交易链接提取SteamID");
    }

    /**
     * 获取 Steam 用户信息
     *
     * @param steamId Steam64 位 ID
     * @return Result<Map<String, Object>> 用户信息
     */
    @GetMapping("/user-info")
    public Result<Map<String, Object>> getSteamUserInfo(@RequestParam String steamId) {
        Map<String, Object> userInfo = steamService.getSteamUserInfo(steamId);
        return Result.success(userInfo);
    }

    /**
     * 修复 item_id 为 NULL 的库存记录
     * 管理员专用接口
     *
     * @return Result<Boolean> 修复结果
     */
    @PostMapping("/admin/fix-null-item-ids")
    public Result<Boolean> fixNullItemIds() {
        log.info("管理员请求修复 item_id 为 NULL 的库存记录");
        try {
            steamInventoryService.fixNullItemIds();
            return Result.success("修复完成", true);
        } catch (Exception e) {
            log.error("修复失败：{}", e.getMessage());
            return Result.error("修复失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * @return Long 用户ID
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

    // ==================== 内部类 ====================

    /**
     * 绑定Steam请求
     */
    @lombok.Data
    public static class BindSteamRequest {
        private String tradeUrl;
        private String apiKey;
    }

    /**
     * Steam绑定状态
     */
    @lombok.Data
    public static class SteamBindStatus {
        private boolean bound;
        private String steamId;
        private String personaName;
        private String avatar;
        private String profileUrl;
        private String tradeUrl; // 新增交易链接字段
    }
}
