package com.cs2trade.service;

import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.SellOrder;
import java.math.BigDecimal;
import java.util.List;

/**
 * 出售订单服务接口
 * 定义出售订单相关的业务逻辑接口
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface SellOrderService {

    /**
     * 创建出售订单（上架）
     *
     * @param userId      用户ID
     * @param inventoryId 库存ID
     * @param price       出售价格
     * @return SellOrder 创建的订单
     */
    SellOrder createSellOrder(Long userId, Long inventoryId, BigDecimal price);

    /**
     * 创建出售订单（上架）- 通过AssetId
     *
     * @param userId  用户ID
     * @param assetId Steam Asset ID
     * @param price   出售价格
     * @return SellOrder 创建的订单
     */
    SellOrder createSellOrderByAssetId(Long userId, String assetId, BigDecimal price);

    /**
     * 取消出售订单（下架）
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return boolean 是否成功
     */
    boolean cancelSellOrder(Long orderId, Long userId);

    /**
     * 获取用户的出售订单
     *
     * @param userId 用户ID
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> getUserSellOrders(Long userId);

    /**
     * 获取指定饰品的在售订单
     *
     * @param itemId 饰品ID
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> getActiveOrdersByItemId(Long itemId);

    /**
     * 根据ID获取订单
     *
     * @param id 订单ID
     * @return SellOrder 订单信息
     */
    SellOrder getOrderById(Long id);

    /**
     * 获取所有出售订单
     *
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> getAllSellOrders();

    /**
     * 根据状态获取出售订单
     *
     * @param status 状态
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> getSellOrdersByStatus(Integer status);

    /**
     * 管理员取消出售订单
     *
     * @param orderId 订单ID
     * @return boolean 是否成功
     */
    boolean adminCancelSellOrder(Long orderId);

    /**
     * 获取在售市场列表（分页）
     *
     * @param category  分类
     * @param exterior  外观
     * @param quality   品质
     * @param keyword   关键词
     * @param minPrice  最低价格
     * @param maxPrice  最高价格
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @param page      页码
     * @param size      每页数量
     * @return PageResult<SellOrder> 分页结果
     */
    PageResult<SellOrder> getMarketList(String category, String exterior, String quality,
                                         String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                         String sortField, String sortOrder, Integer page, Integer size);
}
