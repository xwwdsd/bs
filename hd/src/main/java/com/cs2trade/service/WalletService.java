package com.cs2trade.service;

import com.cs2trade.entity.Wallet;

import java.math.BigDecimal;

/**
 * 钱包服务接口
 * 定义钱包相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface WalletService {

    /**
     * 获取用户钱包
     *
     * @param userId 用户ID
     * @return Wallet 钱包信息
     */
    Wallet getWalletByUserId(Long userId);

    /**
     * 创建用户钱包
     *
     * @param userId 用户ID
     * @return Wallet 创建的钱包
     */
    Wallet createWallet(Long userId);

    /**
     * 增加余额
     *
     * @param userId 用户ID
     * @param amount 增加金额
     * @return boolean 是否成功
     */
    boolean addBalance(Long userId, BigDecimal amount);

    /**
     * 扣除余额
     *
     * @param userId 用户ID
     * @param amount 扣除金额
     * @return boolean 是否成功
     */
    boolean deductBalance(Long userId, BigDecimal amount);

    /**
     * 冻结金额
     *
     * @param userId 用户ID
     * @param amount 冻结金额
     * @return boolean 是否成功
     */
    boolean freezeAmount(Long userId, BigDecimal amount);

    /**
     * 解冻金额
     *
     * @param userId 用户ID
     * @param amount 解冻金额
     * @return boolean 是否成功
     */
    boolean unfreezeAmount(Long userId, BigDecimal amount);

    // ==================== 求购金相关方法 ====================

    /**
     * 增加求购金
     *
     * @param userId 用户ID
     * @param amount 增加金额
     * @return boolean 是否成功
     */
    boolean addBuyOrderAmount(Long userId, BigDecimal amount);

    /**
     * 扣除求购金
     *
     * @param userId 用户ID
     * @param amount 扣除金额
     * @return boolean 是否成功
     */
    boolean deductBuyOrderAmount(Long userId, BigDecimal amount);

    /**
     * 冻结求购金
     *
     * @param userId 用户ID
     * @param amount 冻结金额
     * @return boolean 是否成功
     */
    boolean freezeBuyOrderAmount(Long userId, BigDecimal amount);

    /**
     * 解冻求购金
     *
     * @param userId 用户ID
     * @param amount 解冻金额
     * @return boolean 是否成功
     */
    boolean unfreezeBuyOrderAmount(Long userId, BigDecimal amount);

    // ==================== 充值提现相关方法 ====================

    /**
     * 充值
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @param channel 充值渠道
     * @return boolean 是否成功
     */
    boolean recharge(Long userId, BigDecimal amount, String channel);

    /**
     * 提现
     *
     * @param userId 用户ID
     * @param amount 提现金额
     * @param account 到账账户
     * @param channel 提现渠道
     * @return boolean 是否成功
     */
    boolean withdraw(Long userId, BigDecimal amount, String account, String channel);

    /**
     * 支付（扣除余额并记录流水）
     *
     * @param userId 用户ID
     * @param amount 支付金额
     * @param description 描述
     * @param relatedOrderId 关联订单ID
     * @param relatedOrderType 关联订单类型
     * @return boolean 是否成功
     */
    boolean pay(Long userId, BigDecimal amount, String description, Long relatedOrderId, String relatedOrderType);

    /**
     * 收款（增加余额并记录流水）
     *
     * @param userId 用户ID
     * @param amount 收款金额
     * @param description 描述
     * @param relatedOrderId 关联订单ID
     * @param relatedOrderType 关联订单类型
     * @return boolean 是否成功
     */
    boolean receive(Long userId, BigDecimal amount, String description, Long relatedOrderId, String relatedOrderType);
}
