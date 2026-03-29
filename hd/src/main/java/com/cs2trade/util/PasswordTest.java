package com.cs2trade.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 输入的明文密码
        String inputPassword = "admin123";
        
        // 生成新的密码哈希
        String newHash = encoder.encode(inputPassword);
        System.out.println("admin123 的新哈希: " + newHash);
        
        // 验证新生成的哈希
        boolean matches = encoder.matches(inputPassword, newHash);
        System.out.println("验证结果: " + matches);
    }
}
