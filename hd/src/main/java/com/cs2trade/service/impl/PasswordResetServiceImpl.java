package com.cs2trade.service.impl;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.PasswordResetToken;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.PasswordResetTokenMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@cs2trade.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> sendResetEmail(String email) {
        log.info("发送密码重置邮件: email={}", email);

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            log.warn("邮箱未注册: email={}", email);
            return Result.error("该邮箱未注册");
        }

        tokenMapper.invalidateByUserId(user.getId());

        String token = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setUsed(PasswordResetToken.USED_NO);
        tokenMapper.insert(resetToken);

        sendEmail(user.getEmail(), user.getUsername(), token);

        log.info("密码重置邮件已发送: userId={}", user.getId());
        return Result.success("重置邮件已发送，请查收", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetPassword(String token, String newPassword) {
        log.info("重置密码请求");

        PasswordResetToken resetToken = tokenMapper.selectByToken(token);
        
        if (resetToken == null) {
            return Result.error("无效的重置链接");
        }

        if (resetToken.getUsed() == PasswordResetToken.USED_YES) {
            return Result.error("该链接已使用");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Result.error("该链接已过期");
        }

        User user = userMapper.selectById(resetToken.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        resetToken.setUsed(PasswordResetToken.USED_YES);
        tokenMapper.updateById(resetToken);

        log.info("密码重置成功: userId={}", user.getId());
        return Result.success("密码重置成功", null);
    }

    @Override
    public Result<Boolean> validateToken(String token) {
        try {
            PasswordResetToken resetToken = tokenMapper.selectByToken(token);

            if (resetToken == null || resetToken.getUsed() == PasswordResetToken.USED_YES) {
                return Result.success(false);
            }

            return Result.success(!resetToken.getExpiresAt().isBefore(LocalDateTime.now()));
        } catch (Exception e) {
            log.error("验证token失败: {}", e.getMessage());
            return Result.success(false);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendEmail(String to, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("【CS2Trade】密码重置");

            String encodedToken = Base64.getEncoder().encodeToString(token.getBytes());
            String resetUrl = frontendUrl + "/reset-password?token=" + encodedToken;
            String content = String.format(
                "您好 %s，\n\n" +
                "您正在申请重置CS2Trade账号密码。\n\n" +
                "请点击以下链接重置密码（链接1小时内有效）：\n%s\n\n" +
                "如果您没有申请重置密码，请忽略此邮件。\n\n" +
                "CS2Trade团队",
                username, resetUrl
            );
            message.setText(content);

            mailSender.send(message);
            log.info("邮件发送成功: to={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }
}
