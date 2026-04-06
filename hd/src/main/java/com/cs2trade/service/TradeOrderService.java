package com.cs2trade.service;

import com.cs2trade.entity.TradeOrder;

import java.util.List;

/**
 * 交易订单服务接口
 * 定义交易订单相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface TradeOrderService {

    /**
     * 创建订单
     *
     * @param buyerId 买家ID
     * @param sellOrderId 出售订单ID
     * @return TradeOrder 创建的订单
     */
    TradeOrder createOrder(Long buyerId, Long sellOrderId);

    /**
     * 鍒涘缓璁㈠崟锛堝厑璁稿嵎鐢ㄦ寚瀹氭垚浜や环锛?
     */
    TradeOrder createOrder(Long buyerId, Long sellOrderId, java.math.BigDecimal agreedPrice);

    /**
     * 支付订单（模拟支付）
     *
     * @param orderId 订单ID
     * @param buyerId 买家ID
     * @return boolean 是否支付成功
     */
    boolean payOrder(Long orderId, Long buyerId);

    /**
     * 发货
     *
     * @param orderId 订单ID
     * @param sellerId 卖家ID
     * @param tradeOfferId Steam交易报价ID
     * @param tradeOfferUrl 交易报价链接
     * @return boolean 是否发货成功
     */
    boolean shipOrder(Long orderId, Long sellerId, String tradeOfferId, String tradeOfferUrl);

    /**
     * 卖家确认已接受机器人报价
     */
    boolean confirmBotOffer(Long orderId, Long sellerId);

    /**
     * 手动触发机器人收货检测
     */
    TradeOrder checkBotDelivery(Long orderId, Long userId);

    /**
     * 确认收货
     *
     * @param orderId 订单ID
     * @param buyerId 买家ID
     * @return boolean 是否确认成功
     */
    boolean confirmReceipt(Long orderId, Long buyerId);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return boolean 是否取消成功
     */
    boolean cancelOrder(Long orderId, Long userId);

    /**
     * 根据ID获取订单
     *
     * @param orderId 订单ID
     * @return TradeOrder 订单信息
     */
    TradeOrder getOrderById(Long orderId);

    /**
     * 获取用户的所有订单
     *
     * @param userId 用户ID
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> getUserOrders(Long userId);

    /**
     * 获取用户作为买家的订单
     *
     * @param buyerId 买家ID
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> getBuyerOrders(Long buyerId);

    /**
     * 获取用户作为卖家的订单
     *
     * @param sellerId 卖家ID
     * @return List<TradeOrder> 订单列表
     */
    List<TradeOrder> getSellerOrders(Long sellerId);
}
