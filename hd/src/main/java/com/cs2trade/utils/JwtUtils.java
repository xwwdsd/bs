package com.cs2trade.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JWT工具类
 * 用于生成、解析和验证JWT令牌
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Component
public class JwtUtils {

    private static final int HS256_MIN_KEY_BYTES = 32;
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=_-]+$");

    /**
     * JWT密钥 - 从配置文件读取
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Access Token过期时间(毫秒) - 从配置文件读取
     */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * Refresh Token过期时间(毫秒) - 从配置文件读取
     */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 获取JWT签名密钥
     * 使用HMAC-SHA256算法生成密钥
     * 
     * @return SecretKey 密钥对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = resolveSigningKeyBytes();
        validateSigningKeyLength(keyBytes);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void validateConfiguredSecret() {
        byte[] keyBytes = resolveSigningKeyBytes();
        validateSigningKeyLength(keyBytes);
    }

    private byte[] resolveSigningKeyBytes() {
        if (jwtSecret == null) {
            throw new IllegalStateException("JWT_SECRET is missing");
        }

        String normalized = jwtSecret.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET is blank");
        }

        if (looksLikeBase64(normalized)) {
            try {
                return Decoders.BASE64.decode(normalized);
            } catch (IllegalArgumentException ignored) {
                // Fall back to raw UTF-8 bytes when the configured value is not valid Base64.
            }
        }

        return normalized.getBytes(StandardCharsets.UTF_8);
    }

    private boolean looksLikeBase64(String value) {
        return value.length() >= 43 && BASE64_PATTERN.matcher(value).matches();
    }

    private void validateSigningKeyLength(byte[] keyBytes) {
        if (keyBytes.length < HS256_MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too short for HS256. Please use at least 32 bytes (256 bits), preferably a Base64-encoded random key."
            );
        }
    }

    /**
     * 生成Access Token
     * Access Token用于用户身份认证，有效期较短
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param email 用户邮箱
     * @return String JWT令牌字符串
     */
    public String generateAccessToken(Long userId, String username, String email) {
        // 创建Token中的附加信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email", email);
        claims.put("tokenType", "access");
        
        // 调用通用生成方法创建Token
        return generateToken(claims, accessTokenExpiration);
    }

    /**
     * 生成Refresh Token
     * Refresh Token用于刷新Access Token，有效期较长
     * 
     * @param userId 用户ID
     * @return String JWT令牌字符串
     */
    public String generateRefreshToken(Long userId) {
        // 创建Token中的附加信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "refresh");
        
        // 调用通用生成方法创建Token
        return generateToken(claims, refreshTokenExpiration);
    }

    /**
     * 通用Token生成方法
     * 
     * @param claims Token中包含的声明信息
     * @param expiration 过期时间(毫秒)
     * @return String JWT令牌字符串
     */
    private String generateToken(Map<String, Object> claims, long expiration) {
        // 获取当前时间
        Date now = new Date();
        // 计算过期时间
        Date expiryDate = new Date(now.getTime() + expiration);

        // 构建JWT令牌
        return Jwts.builder()
                // 设置自定义声明
                .claims(claims)
                // 设置签发时间
                .issuedAt(now)
                // 设置过期时间
                .expiration(expiryDate)
                // 设置签名算法和密钥
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                // 生成紧凑格式的JWT字符串
                .compact();
    }

    /**
     * 从Token中提取用户ID
     * 
     * @param token JWT令牌
     * @return Long 用户ID
     */
    public Long getUserIdFromToken(String token) {
        // 解析Token并获取声明
        Claims claims = parseToken(token);
        // 返回用户ID
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中提取用户名
     * 
     * @param token JWT令牌
     * @return String 用户名
     */
    public String getUsernameFromToken(String token) {
        // 解析Token并获取声明
        Claims claims = parseToken(token);
        // 返回用户名
        return claims.get("username", String.class);
    }

    /**
     * 从Token中提取Token类型
     * 
     * @param token JWT令牌
     * @return String Token类型(access/refresh)
     */
    public String getTokenTypeFromToken(String token) {
        // 解析Token并获取声明
        Claims claims = parseToken(token);
        // 返回Token类型
        return claims.get("tokenType", String.class);
    }

    /**
     * 解析Token获取所有声明
     * 
     * @param token JWT令牌
     * @return Claims 声明对象
     * @throws JwtException 解析失败时抛出异常
     */
    public Claims parseToken(String token) {
        try {
            // 使用JWT解析器解析Token
            JwtParser parser = Jwts.parser()
                    // 设置验证密钥
                    .verifyWith(getSigningKey())
                    .build();
            
            // 解析并返回声明
            return parser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            // Token已过期
            log.error("JWT Token已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            // 不支持的Token格式
            log.error("不支持的JWT Token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            // Token格式错误
            log.error("JWT Token格式错误: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            // 签名验证失败
            log.error("JWT签名验证失败: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            // Token为空或非法
            log.error("JWT Token为空或非法: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证Token是否有效
     * 
     * @param token JWT令牌
     * @return boolean 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析Token
            parseToken(token);
            // 解析成功则Token有效
            return true;
        } catch (Exception e) {
            // 解析失败则Token无效
            log.error("JWT Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token是否过期
     * 
     * @param token JWT令牌
     * @return boolean 是否已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            // 解析Token获取声明
            Claims claims = parseToken(token);
            // 获取过期时间
            Date expiration = claims.getExpiration();
            // 比较过期时间与当前时间
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            // Token已过期异常
            return true;
        } catch (Exception e) {
            // 其他异常视为过期
            log.error("检查Token过期状态时出错: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 获取Token剩余有效时间(毫秒)
     * 
     * @param token JWT令牌
     * @return long 剩余有效时间(毫秒)，如果已过期返回0
     */
    public long getExpirationTime(String token) {
        try {
            // 解析Token获取声明
            Claims claims = parseToken(token);
            // 获取过期时间
            Date expiration = claims.getExpiration();
            // 计算剩余时间
            long remaining = expiration.getTime() - System.currentTimeMillis();
            // 返回剩余时间(不小于0)
            return Math.max(remaining, 0);
        } catch (Exception e) {
            // 异常时返回0
            return 0;
        }
    }

    /**
     * 刷新Access Token
     * 使用Refresh Token生成新的Access Token
     * 
     * @param refreshToken 刷新令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param email 用户邮箱
     * @return String 新的Access Token
     */
    public String refreshAccessToken(String refreshToken, Long userId, String username, String email) {
        // 验证Refresh Token是否有效
        if (!validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token无效或已过期");
        }
        
        // 验证Token类型是否为refresh
        String tokenType = getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("Token类型不正确");
        }
        
        // 验证Token中的用户ID是否匹配
        Long tokenUserId = getUserIdFromToken(refreshToken);
        if (!tokenUserId.equals(userId)) {
            throw new RuntimeException("Token用户ID不匹配");
        }
        
        // 生成新的Access Token
        return generateAccessToken(userId, username, email);
    }
}
