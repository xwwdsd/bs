package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminResetController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/reset-admin-password")
    public Result<String> resetAdminPassword() {
        log.info("重置管理员密码请求");

        User admin = userMapper.selectByUsername("admin");
        if (admin == null) {
            admin = userMapper.selectByEmail("admin@cs2trade.com");
        }

        if (admin == null) {
            return Result.error("管理员账号不存在");
        }

        String newPassword = "admin123";
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(admin);

        log.info("管理员密码已重置: userId={}", admin.getId());
        return Result.success("管理员密码已重置为: admin123", "admin123");
    }
}
