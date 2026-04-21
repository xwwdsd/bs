package com.cs2trade.mapper;

import com.cs2trade.entity.Wallet;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户钱包数据访问层接口
 * 定义对用户钱包表(user_wallet)的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface WalletMapper {

    /**
     * 根据ID查询钱包
     *
     * @param id 钱包ID
     * @return Wallet 钱包实体对象
     */
    @Select("SELECT * FROM user_wallet WHERE id = #{id}")
    Wallet selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询钱包
     *
     * @param userId 用户ID
     * @return Wallet 钱包实体对象
     */
    @Select("SELECT * FROM user_wallet WHERE user_id = #{userId}")
    Wallet selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_wallet ORDER BY updated_at DESC")
    List<Wallet> selectAll();

    /**
     * 插入新钱包
     *
     * @param wallet 钱包实体对象
     * @return int 影响的行数
     */
    @Insert("INSERT INTO user_wallet (user_id, balance, frozen_amount, buy_order_amount, frozen_buy_order_amount, total_recharge, total_withdraw, created_at, updated_at) VALUES (#{userId}, #{balance}, #{frozenAmount}, #{buyOrderAmount}, #{frozenBuyOrderAmount}, #{totalRecharge}, #{totalWithdraw}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Wallet wallet);

    /**
     * 更新钱包余额
     *
     * @param userId 用户ID
     * @param amount 变动金额（正数为增加，负数为减少）
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, updated_at = NOW() WHERE user_id = #{userId}")
    int updateBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 增加余额
     *
     * @param userId 用户ID
     * @param amount 增加金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, updated_at = NOW() WHERE user_id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣除余额
     *
     * @param userId 用户ID
     * @param amount 扣除金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET balance = balance - #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 冻结金额
     *
     * @param userId 用户ID
     * @param amount 冻结金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET balance = balance - #{amount}, frozen_amount = frozen_amount + #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND balance >= #{amount}")
    int freezeAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解冻金额
     *
     * @param userId 用户ID
     * @param amount 解冻金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET balance = balance + #{amount}, frozen_amount = frozen_amount - #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND frozen_amount >= #{amount}")
    int unfreezeAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // ==================== 求购金相关方法 ====================

    /**
     * 增加求购金
     *
     * @param userId 用户ID
     * @param amount 增加金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET buy_order_amount = buy_order_amount + #{amount}, updated_at = NOW() WHERE user_id = #{userId}")
    int addBuyOrderAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣除求购金
     *
     * @param userId 用户ID
     * @param amount 扣除金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET buy_order_amount = buy_order_amount - #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND buy_order_amount >= #{amount}")
    int deductBuyOrderAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 冻结求购金
     *
     * @param userId 用户ID
     * @param amount 冻结金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET frozen_buy_order_amount = frozen_buy_order_amount + #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND (buy_order_amount - frozen_buy_order_amount) >= #{amount}")
    int freezeBuyOrderAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解冻求购金
     *
     * @param userId 用户ID
     * @param amount 解冻金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET frozen_buy_order_amount = frozen_buy_order_amount - #{amount}, updated_at = NOW() WHERE user_id = #{userId} AND frozen_buy_order_amount >= #{amount}")
    int unfreezeBuyOrderAmount(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // ==================== 累计金额相关方法 ====================

    /**
     * 增加累计充值金额
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET total_recharge = total_recharge + #{amount}, updated_at = NOW() WHERE user_id = #{userId}")
    int addTotalRecharge(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 增加累计提现金额
     *
     * @param userId 用户ID
     * @param amount 提现金额
     * @return int 影响的行数
     */
    @Update("UPDATE user_wallet SET total_withdraw = total_withdraw + #{amount}, updated_at = NOW() WHERE user_id = #{userId}")
    int addTotalWithdraw(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
