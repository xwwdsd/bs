package com.cs2trade.service;

import com.cs2trade.dto.LoginRequest;
import com.cs2trade.dto.LoginResponse;
import com.cs2trade.dto.RegisterRequest;
import com.cs2trade.entity.User;

/**
 * 用户服务接口
 * 定义用户相关的业务逻辑方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface UserService {

    /**
     * 用户注册
     * 处理用户注册请求，创建新用户账号
     *
     * @param request 注册请求DTO，包含用户名、邮箱、密码等信息
     * @return User 注册成功的用户信息
     */
    User register(RegisterRequest request);

    /**
     * 用户登录
     * 验证用户凭据并返回登录令牌
     *
     * @param request 登录请求DTO，包含邮箱和密码
     * @param ipAddress 用户登录IP地址，用于记录登录信息
     * @return LoginResponse 登录响应，包含Token和用户信息
     */
    LoginResponse login(LoginRequest request, String ipAddress);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return User 用户实体对象，不存在时返回null
     */
    User getUserById(Long userId);

    /**
     * 根据邮箱查询用户
     *
     * @param email 用户邮箱
     * @return User 用户实体对象，不存在时返回null
     */
    User getUserByEmail(String email);

    /**
     * 检查邮箱是否已注册
     *
     * @param email 邮箱地址
     * @return boolean 是否已注册
     */
    boolean isEmailExists(String email);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return boolean 是否已存在
     */
    boolean isUsernameExists(String username);

    /**
     * 更新用户头像和昵称
     *
     * @param userId 用户ID
     * @param nickname 昵称
     * @param avatar 头像
     * @return User 更新后的用户
     */
    User updateProfile(Long userId, String nickname, String avatar);

    /**
     * 更新Steam交易链接
     *
     * @param userId 用户ID
     * @param tradeUrl 交易链接
     * @return boolean 是否成功
     */
    boolean updateSteamTradeUrl(Long userId, String tradeUrl);

    /**
     * 更新密码
     *
     * @param userId 用户ID
     * @param newPassword 新密码（已加密）
     * @return boolean 是否成功
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 绑定支付宝
     *
     * @param userId 用户ID
     * @param alipayAccount 支付宝账号
     * @param realName 真实姓名
     * @return boolean 是否成功
     */
    boolean bindAlipay(Long userId, String alipayAccount, String realName);

    /**
     * 绑定银行卡
     *
     * @param userId 用户ID
     * @param realName 持卡人姓名
     * @param cardNumber 银行卡号
     * @param bankName 开户银行
     * @param phone 预留手机号
     * @return boolean 是否成功
     */
    boolean bindBank(Long userId, String realName, String cardNumber, String bankName, String phone);

    /**
     * 绑定手机号
     *
     * @param userId 用户ID
     * @param phone 手机号
     * @return boolean 是否成功
     */
    boolean bindPhone(Long userId, String phone);

    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号
     * @return boolean 是否已存在
     */
    boolean isPhoneExists(String phone);
}
