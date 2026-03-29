package com.cs2trade.service.impl;

import com.cs2trade.dto.LoginRequest;
import com.cs2trade.dto.LoginResponse;
import com.cs2trade.dto.RegisterRequest;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.UserService;
import com.cs2trade.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 * 实现用户相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户数据访问层
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器，用于密码加密和验证
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT工具类，用于生成Token
     */
    private final JwtUtils jwtUtils;

    /**
     * Access Token过期时间（毫秒）
     */
    @Value("${jwt.access-token-expiration:7200000}")
    private long accessTokenExpiration;

    /**
     * 用户注册
     * 创建新用户账号，进行数据校验和密码加密
     *
     * @param request 注册请求DTO
     * @return User 注册成功的用户信息
     * @throws RuntimeException 当校验失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        log.info("用户注册请求: email={}", request.getEmail());

        // 1. 校验两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 2. 检查邮箱是否已注册
        if (isEmailExists(request.getEmail())) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 3. 检查用户名是否已存在
        if (isUsernameExists(request.getUsername())) {
            throw new RuntimeException("该用户名已被使用");
        }

        // 4. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // 使用BCrypt对密码进行加密
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // 设置默认头像（使用系统默认头像）
        user.setAvatar("/default-avatar.png");
        // 设置账号状态为正常
        user.setStatus(User.STATUS_ENABLED);
        // 设置用户等级为普通用户
        user.setUserLevel(User.LEVEL_NORMAL);
        // 设置未实名认证
        user.setIsRealName(User.REAL_NAME_NO);
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 5. 保存到数据库
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("注册失败，请稍后重试");
        }

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 用户登录
     * 验证用户凭据，生成JWT令牌，更新登录信息
     * 支持用户名或邮箱登录
     *
     * @param request   登录请求DTO
     * @param ipAddress 用户登录IP地址
     * @return LoginResponse 登录响应，包含Token和用户信息
     * @throws RuntimeException 当登录失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, String ipAddress) {
        log.info("用户登录请求: account={}", request.getAccount());

        // 1. 根据账号查询用户（支持用户名或邮箱）
        User user;
        if (request.getAccount().contains("@")) {
            // 包含@符号，按邮箱查询
            user = userMapper.selectByEmail(request.getAccount());
        } else {
            // 不包含@符号，按用户名查询
            user = userMapper.selectByUsername(request.getAccount());
        }
        if (user == null) {
            throw new RuntimeException("用户名/邮箱或密码错误");
        }

        // 2. 检查账号状态
        if (user.getStatus() == User.STATUS_DISABLED) {
            throw new RuntimeException("账号已被禁用，请联系客服");
        }
        if (user.getStatus() == User.STATUS_PENDING) {
            throw new RuntimeException("账号待验证，请查看邮箱完成验证");
        }

        // 3. 验证密码
        log.info("验证密码 - 输入密码长度: {}, 数据库密码: {}", 
                request.getPassword() != null ? request.getPassword().length() : 0,
                user.getPassword());
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("密码验证结果: {}", passwordMatches);
        if (!passwordMatches) {
            throw new RuntimeException("用户名/邮箱或密码错误");
        }

        // 4. 更新登录信息
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLoginInfo(user.getId(), now, ipAddress);

        // 5. 生成JWT令牌
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 6. 构建登录响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .userLevel(user.getUserLevel())
                .isRealName(user.getIsRealName())
                .steamBound(user.getSteamId() != null && !user.getSteamId().isEmpty())
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // 转换为秒
                .userInfo(userInfo)
                .build();

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return response;
    }

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return User 用户实体对象，不存在时返回null
     */
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 根据邮箱查询用户
     *
     * @param email 用户邮箱
     * @return User 用户实体对象，不存在时返回null
     */
    @Override
    public User getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /**
     * 检查邮箱是否已注册
     *
     * @param email 邮箱地址
     * @return boolean 是否已注册
     */
    @Override
    public boolean isEmailExists(String email) {
        return userMapper.countByEmail(email) > 0;
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return boolean 是否已存在
     */
    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Long userId, String nickname, String avatar) {
        User user = new User();
        user.setId(userId);
        if (nickname != null && !nickname.isEmpty()) {
            user.setUsername(nickname);
        }
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar);
        }
        
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSteamTradeUrl(Long userId, String tradeUrl) {
        User user = new User();
        user.setId(userId);
        user.setSteamTradeUrl(tradeUrl);
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, String newPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(newPassword);
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindAlipay(Long userId, String alipayAccount, String realName) {
        log.info("绑定支付宝: userId={}, account={}", userId, alipayAccount);
        User user = new User();
        user.setId(userId);
        user.setAlipayAccount(alipayAccount);
        user.setAlipayRealName(realName);
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindBank(Long userId, String realName, String cardNumber, String bankName, String phone) {
        log.info("绑定银行卡: userId={}, cardNumber={}", userId, cardNumber);
        User user = new User();
        user.setId(userId);
        user.setBankRealName(realName);
        user.setBankCardNumber(cardNumber);
        user.setBankName(bankName);
        user.setBankPhone(phone);
        return userMapper.updateById(user) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindPhone(Long userId, String phone) {
        log.info("绑定手机号: userId={}, phone={}", userId, phone);
        
        if (userMapper.countByPhone(phone) > 0) {
            User existingUser = userMapper.selectByPhone(phone);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("该手机号已被其他用户绑定");
            }
        }
        
        return userMapper.updatePhone(userId, phone) > 0;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        return userMapper.countByPhone(phone) > 0;
    }
}
