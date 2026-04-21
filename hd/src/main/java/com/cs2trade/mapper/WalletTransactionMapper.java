package com.cs2trade.mapper;

import com.cs2trade.entity.WalletTransaction;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 钱包交易流水Mapper接口
 * 提供钱包交易流水的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface WalletTransactionMapper {

    /**
     * 根据ID查询流水
     *
     * @param id 流水ID
     * @return WalletTransaction 流水信息
     */
    @Select("SELECT * FROM wallet_transaction WHERE id = #{id}")
    WalletTransaction selectById(Long id);

    /**
     * 查询用户的所有流水
     *
     * @param userId 用户ID
     * @return List<WalletTransaction> 流水列表
     */
    @Select("SELECT id, user_id, type, amount, balance AS balanceAfter, CAST(NULL AS SIGNED) AS relatedOrderId, order_no AS orderNo, remark AS description, created_at FROM wallet_transaction WHERE user_id = #{userId} ORDER BY created_at DESC")
    @Results(id = "walletTransactionMap", value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "type", column = "type"),
        @Result(property = "amount", column = "amount"),
        @Result(property = "balanceAfter", column = "balanceAfter"),
        @Result(property = "relatedOrderId", column = "relatedOrderId"),
        @Result(property = "orderNo", column = "orderNo"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<WalletTransaction> selectByUserId(Long userId);

    /**
     * 查询用户的流水（按类型筛选）
     *
     * @param userId 用户ID
     * @param type 交易类型
     * @return List<WalletTransaction> 流水列表
     */
    @Select("SELECT id, user_id, type, amount, balance AS balanceAfter, CAST(NULL AS SIGNED) AS relatedOrderId, order_no AS orderNo, remark AS description, created_at FROM wallet_transaction WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC")
    @ResultMap("walletTransactionMap")
    List<WalletTransaction> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    @Select("""
            <script>
            SELECT id, user_id, type, amount, balance AS balanceAfter,
                   CAST(NULL AS SIGNED) AS relatedOrderId, order_no AS orderNo, remark AS description, created_at
            FROM wallet_transaction
            <where>
                <if test='userId != null'>AND user_id = #{userId}</if>
                <if test='type != null'>AND type = #{type}</if>
                <if test='orderNo != null and orderNo != ""'>AND order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
            </where>
            ORDER BY created_at DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    @ResultMap("walletTransactionMap")
    List<WalletTransaction> selectAllForAdmin(@Param("userId") Long userId,
                                              @Param("type") Integer type,
                                              @Param("orderNo") String orderNo,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM wallet_transaction
            <where>
                <if test='userId != null'>AND user_id = #{userId}</if>
                <if test='type != null'>AND type = #{type}</if>
                <if test='orderNo != null and orderNo != ""'>AND order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
            </where>
            </script>
            """)
    long countAllForAdmin(@Param("userId") Long userId,
                          @Param("type") Integer type,
                          @Param("orderNo") String orderNo);

    /**
     * 插入新流水
     *
     * @param transaction 流水信息
     * @return int 影响行数
     */
    @Insert("INSERT INTO wallet_transaction (user_id, type, amount, balance, order_no, remark, created_at) " +
            "VALUES (#{userId}, #{type}, #{amount}, #{balanceAfter}, #{relatedOrderId}, #{description}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WalletTransaction transaction);

    /**
     * 删除流水
     *
     * @param id 流水ID
     * @return int 影响行数
     */
    @Delete("DELETE FROM wallet_transaction WHERE id = #{id}")
    int deleteById(Long id);
}
