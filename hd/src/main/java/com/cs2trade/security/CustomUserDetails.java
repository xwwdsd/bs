package com.cs2trade.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 自定义用户详情类
 * 扩展Spring Security的UserDetails，添加用户ID字段
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户等级
     */
    private Integer userLevel;

    /**
     * 权限列表
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 账号是否未过期
     */
    private boolean accountNonExpired = true;

    /**
     * 账号是否未锁定
     */
    private boolean accountNonLocked = true;

    /**
     * 凭证是否未过期
     */
    private boolean credentialsNonExpired = true;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public CustomUserDetails(Long userId, String username, String password, String email, 
                             Integer userLevel, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.userLevel = userLevel;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
