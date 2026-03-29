package com.cs2trade.security;

import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT认证过滤器
 * 拦截所有请求，验证JWT令牌的有效性
 * 继承OncePerRequestFilter确保每个请求只过滤一次
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT工具类，用于Token的解析和验证
     */
    private final JwtUtils jwtUtils;

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * Token请求头名称，从配置文件读取
     */
    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    /**
     * Token前缀，从配置文件读取
     */
    @Value("${jwt.prefix:Bearer}")
    private String tokenPrefix;

    /**
     * 执行过滤逻辑
     * 从请求头中提取JWT令牌并验证
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头中提取JWT令牌
            String jwt = extractJwtFromRequest(request);

            // 检查令牌是否存在且有效
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                // 从Token中提取用户ID
                Long userId = jwtUtils.getUserIdFromToken(jwt);
                // 从Token中提取用户名
                String username = jwtUtils.getUsernameFromToken(jwt);

                // 从数据库查询用户详细信息
                User user = userMapper.selectById(userId);
                if (user != null) {
                    // 构建权限列表
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (user.getUserLevel() != null) {
                        if (user.getUserLevel() >= User.LEVEL_ADMIN) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                        if (user.getUserLevel() >= User.LEVEL_SUPER_ADMIN) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                        }
                    }

                    // 创建自定义用户详情对象
                    CustomUserDetails userDetails = new CustomUserDetails(
                            userId,
                            username,
                            "", // JWT认证不需要密码
                            user.getEmail(),
                            user.getUserLevel(),
                            authorities
                    );

                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, // 用户信息
                                    null, // 凭证（JWT认证不需要）
                                    userDetails.getAuthorities() // 权限列表
                            );

                    // 设置请求详情（IP地址、Session ID等）
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 将认证信息存入Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT认证成功，用户ID: {}, 用户名: {}", userId, username);
                }
            }
        } catch (Exception e) {
            // 认证失败，记录错误日志
            log.error("JWT认证失败: {}", e.getMessage());
            // 清除Security上下文中的认证信息
            SecurityContextHolder.clearContext();
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求头中提取JWT令牌
     * 支持标准的Bearer Token格式：Authorization: Bearer <token>
     * 
     * @param request HTTP请求对象
     * @return String JWT令牌字符串，如果不存在则返回null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // 获取Authorization请求头
        String bearerToken = request.getHeader(tokenHeader);

        // 检查请求头是否存在且以指定前缀开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix + " ")) {
            // 提取Token（去掉前缀和空格）
            return bearerToken.substring(tokenPrefix.length() + 1);
        }

        // 没有找到有效的Token
        return null;
    }

    /**
     * 判断是否应该跳过过滤
     * 某些路径（如登录、注册）不需要JWT认证
     * 
     * @param request HTTP请求对象
     * @return boolean 是否跳过过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 获取请求路径
        String path = request.getRequestURI();
        
        // 定义不需要JWT认证的路径
        // 登录、注册等公开接口
        return path.contains("/auth/login") 
            || path.contains("/auth/register")
            || path.contains("/auth/refresh")
            || path.startsWith("/api/public/");
    }
}
