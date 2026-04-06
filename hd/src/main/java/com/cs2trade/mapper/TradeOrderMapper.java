package com.cs2trade.mapper;

import com.cs2trade.entity.TradeOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 交易订单Mapper接口
 * 提供交易订单的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface TradeOrderMapper {

    /**
     * 根据ID查询订单
     *
     * @param id 订单ID
     * @return TradeOrder 订单信息
     */
    @Select("SELECT * FROM trade_order WHERE id = #{id}")
    TradeOrder selectById(Long id);

    /**
     * 根据订单编号查询订单
     *
     * @param orderNo 订单编号
     * @return TradeOrder 订单信息
     */
    @Select("SELECT * FROM trade_order WHERE order_no = #{orderNo}")
    TradeOrder selectByOrderNo(String orderNo);

    /**
     * 查询买家的所有订单
     *
     * @param buyerId 买家ID
     * @return List<TradeOrder> 订单列表
     */
    @Select("SELECT * FROM trade_order WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<TradeOrder> selectByBuyerId(Long buyerId);

    /**
     * 查询卖家的所有订单
     *
     * @param sellerId 卖家ID
     * @return List<TradeOrder> 订单列表
     */
    @Select("SELECT * FROM trade_order WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<TradeOrder> selectBySellerId(Long sellerId);

    /**
     * 查询用户的所有订单（作为买家或卖家）
     *
     * @param userId 用户ID
     * @return List<TradeOrder> 订单列表
     */
    @Select("SELECT * FROM trade_order WHERE buyer_id = #{userId} OR seller_id = #{userId} ORDER BY created_at DESC")
    List<TradeOrder> selectByUserId(Long userId);

    /**
     * 插入新订单
     *
     * @param order 订单信息
     * @return int 影响行数
     */
    @Insert("INSERT INTO trade_order (order_no, buyer_id, seller_id, item_id, inventory_id, price, fee, actual_amount, status, created_at, updated_at) " +
            "VALUES (#{orderNo}, #{buyerId}, #{sellerId}, #{itemId}, #{inventoryId}, #{price}, #{fee}, #{actualAmount}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TradeOrder order);

    /**
     * 更新订单状态
     *
     * @param id 订单ID
     * @param status 新状态
     * @return int 影响行数
     */
    @Update("UPDATE trade_order SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新订单支付信息
     *
     * @param id 订单ID
     * @param status 状态
     * @return int 影响行数
     */
    @Update("UPDATE trade_order SET status = #{status}, paid_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updatePaidStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新订单发货信息
     *
     * @param id 订单ID
     * @param status 状态
     * @param tradeOfferId Steam交易报价ID
     * @param tradeOfferUrl 交易报价链接
     * @return int 影响行数
     */
    @Update("UPDATE trade_order SET status = #{status}, trade_offer_id = #{tradeOfferId}, trade_offer_url = #{tradeOfferUrl}, sent_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateSentStatus(@Param("id") Long id, @Param("status") Integer status, 
                         @Param("tradeOfferId") String tradeOfferId, @Param("tradeOfferUrl") String tradeOfferUrl);

    /**
     * 更新订单完成信息
     *
     * @param id 订单ID
     * @param status 状态
     * @return int 影响行数
     */
    @Update("UPDATE trade_order SET status = #{status}, completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateCompletedStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新订单取消信息
     *
     * @param id 订单ID
     * @param status 状态
     * @return int 影响行数
     */
    @Update("UPDATE trade_order SET status = #{status}, cancelled_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateCancelledStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除订单
     *
     * @param id 订单ID
     * @return int 影响行数
     */
    @Delete("DELETE FROM trade_order WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 查询所有订单（MyBatis-Plus兼容）
     *
     * @param wrapper 查询条件
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> selectList(@Param("ew") Object wrapper);

    /**
     * 根据状态查询订单
     *
     * @param status 订单状态
     * @return List<TradeOrder> 订单列表
     */
    @Select("SELECT * FROM trade_order WHERE status = #{status} ORDER BY created_at DESC")
    List<TradeOrder> selectByStatus(@Param("status") Integer status);

    @Select("""
            SELECT *
            FROM trade_order
            WHERE status = #{status}
              AND trade_offer_id IS NOT NULL
            ORDER BY updated_at ASC
            LIMIT #{limit}
            """)
    List<TradeOrder> selectByStatusForMonitoring(@Param("status") Integer status, @Param("limit") Integer limit);

    @Update("""
            UPDATE trade_order
            SET status = #{status},
                trade_offer_id = #{tradeOfferId},
                trade_offer_url = #{tradeOfferUrl},
                delivery_stage = #{deliveryStage},
                bot_offer_dispatched_at = NOW(),
                seller_offer_confirmed_at = NOW(),
                steam_offer_state = NULL,
                steam_offer_state_text = NULL,
                last_offer_check_at = NULL,
                inventory_verified_at = NULL,
                bot_received_at = NULL,
                monitor_error_message = NULL,
                sent_at = NOW(),
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int registerBotOffer(@Param("id") Long id,
                         @Param("status") Integer status,
                         @Param("tradeOfferId") String tradeOfferId,
                         @Param("tradeOfferUrl") String tradeOfferUrl,
                         @Param("deliveryStage") String deliveryStage);

    @Update("""
            UPDATE trade_order
            SET seller_offer_confirmed_at = NOW(),
                delivery_stage = #{deliveryStage},
                monitor_error_message = NULL,
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int confirmSellerBotOffer(@Param("id") Long id, @Param("deliveryStage") String deliveryStage);

    @Update("""
            <script>
            UPDATE trade_order
            <set>
                <if test='status != null'>status = #{status},</if>
                <if test='steamOfferState != null'>steam_offer_state = #{steamOfferState},</if>
                <if test='steamOfferStateText != null'>steam_offer_state_text = #{steamOfferStateText},</if>
                <if test='deliveryStage != null'>delivery_stage = #{deliveryStage},</if>
                <if test='lastOfferCheckAt != null'>last_offer_check_at = #{lastOfferCheckAt},</if>
                <if test='inventoryVerifiedAt != null'>inventory_verified_at = #{inventoryVerifiedAt},</if>
                <if test='botReceivedAt != null'>bot_received_at = #{botReceivedAt},</if>
                <if test='completedAt != null'>completed_at = #{completedAt},</if>
                monitor_error_message = #{monitorErrorMessage},
                updated_at = NOW()
            </set>
            WHERE id = #{id}
            </script>
            """)
    int updateBotTracking(TradeOrder order);

    /**
     * 删除指定状态的交易订单
     *
     * @param status 订单状态
     * @return int 影响行数
     */
    @Delete("DELETE FROM trade_order WHERE status = #{status}")
    int deleteByStatus(@Param("status") Integer status);

    /**
     * 删除指定天数之前的已完成订单
     *
     * @param days 天数
     * @return int 影响行数
     */
    @Delete("DELETE FROM trade_order WHERE status = 4 AND completed_at < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOldCompletedOrders(@Param("days") int days);
}
