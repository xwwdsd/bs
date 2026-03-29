package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.dto.request.ResetPasswordRequest;
import com.cs2trade.dto.request.SendResetEmailRequest;
import com.cs2trade.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody SendResetEmailRequest request) {
        log.info("忘记密码请求: email={}", request.getEmail());
        return passwordResetService.sendResetEmail(request.getEmail());
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("重置密码请求");
        return passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

    @GetMapping("/validate-reset-token")
    public Result<Boolean> validateToken(@RequestParam String token) {
        return passwordResetService.validateToken(token);
    }
}
