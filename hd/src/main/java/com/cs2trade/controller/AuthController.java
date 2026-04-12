package com.cs2trade.controller;

import com.cs2trade.dto.LoginRequest;
import com.cs2trade.dto.LoginResponse;
import com.cs2trade.dto.RegisterRequest;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.User;
import com.cs2trade.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received: email={}", request.getEmail());

        try {
            User user = userService.register(request);
            log.info("Register succeeded: userId={}", user.getId());
            return Result.success("\u6ce8\u518c\u6210\u529f", null);
        } catch (RuntimeException e) {
            log.warn("Register failed: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Register failed unexpectedly", e);
            return Result.error("\u6ce8\u518c\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        log.info("Login request received: account={}", request.getAccount());

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            LoginResponse response = userService.login(request, ipAddress);

            log.info("Login succeeded: account={}", request.getAccount());
            return Result.success("\u767b\u5f55\u6210\u529f", response);
        } catch (RuntimeException e) {
            log.warn("Login failed: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Login failed unexpectedly", e);
            return Result.error("\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }
    }

    @GetMapping("/check-email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        return Result.success(userService.isEmailExists(email));
    }

    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(userService.isUsernameExists(username));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private boolean isBlankOrUnknown(String value) {
        return value == null || value.isEmpty() || "unknown".equalsIgnoreCase(value);
    }
}
