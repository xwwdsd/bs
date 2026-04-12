package com.cs2trade.mapper;

import com.cs2trade.entity.SellOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 出售订单数据访问层接口
 * 定义对出售订单表(sell_order)的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface SellOrderMapper {

    /**
     * 根据ID查询出售订单
     *
     * @param id 订单ID
     * @return SellOrder 订单实体对象
     */
    SellOrder selectById(@Param("id") Long id);

    /**
     * 查询所有出售订单
     *
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectAll();

    /**
     * 根据用户ID查询出售订单
     *
     * @param userId 用户ID
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据饰品ID查询出售订单
     *
     * @param itemId 饰品ID
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 根据状态查询出售订单
     *
     * @param status 状态
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectByStatus(@Param("status") Integer status);

    /**
     * 查询指定饰品的在售订单（按价格排序）
     *
     * @param itemId 饰品ID
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectActiveByItemId(@Param("itemId") Long itemId);

    /**
     * 插入新出售订单
     *
     * @param sellOrder 订单实体对象
     * @return int 影响的行数
     */
    int insert(SellOrder sellOrder);

    /**
     * 根据ID更新出售订单
     *
     * @param sellOrder 订单实体对象
     * @return int 影响的行数
     */
    int updateById(SellOrder sellOrder);

    /**
     * 根据ID删除出售订单
     *
     * @param id 订单ID
     * @return int 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 取消订单（更新状态为已取消）
     *
     * @param id 订单ID
     * @return int 影响的行数
     */
    int cancelOrder(@Param("id") Long id);

    /**
     * 完成订单（更新状态为已售出）
     *
     * @param id 订单ID
     * @return int 影响的行数
     */
    int completeOrder(@Param("id") Long id);

    /**
     * 更新订单状态
     *
     * @param id 订单ID
     * @param status 新状态
     * @return int 影响的行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据库存ID更新订单状态
     *
     * @param inventoryId 库存ID
     * @param status 新状态
     * @return int 影响的行数
     */
    int updateStatusByInventoryId(@Param("inventoryId") Long inventoryId, @Param("status") Integer status);

    /**
     * Update the item linked to an order.
     *
     * @param id     order id
     * @param itemId item id
     * @return affected rows
     */
    int updateItemId(@Param("id") Long id, @Param("itemId") Long itemId);

    /**
     * 删除指定状态的出售订单
     *
     * @param status 状态
     * @return int 影响的行数
     */
    int deleteByStatus(@Param("status") Integer status);

    /**
     * 删除过期的出售订单
     *
     * @return int 影响的行数
     */
    int deleteExpired();

    /**
     * 分页查询在售订单（带筛选条件）
     *
     * @param category  分类
     * @param exterior  外观
     * @param quality   品质
     * @param keyword   关键词
     * @param minPrice  最低价格
     * @param maxPrice  最高价格
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @param offset    偏移量
     * @param size      每页数量
     * @return List<SellOrder> 订单列表
     */
    List<SellOrder> selectMarketList(@Param("category") String category,
                                      @Param("exterior") String exterior,
                                      @Param("quality") String quality,
                                      @Param("keyword") String keyword,
                                      @Param("minPrice") java.math.BigDecimal minPrice,
                                      @Param("maxPrice") java.math.BigDecimal maxPrice,
                                      @Param("sortField") String sortField,
                                      @Param("sortOrder") String sortOrder,
                                      @Param("offset") Integer offset,
                                      @Param("size") Integer size);

    /**
     * 统计在售订单数量（带筛选条件）
     *
     * @param category 分类
     * @param exterior 外观
     * @param quality  品质
     * @param keyword  关键词
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return Long 总数
     */
    Long countMarketList(@Param("category") String category,
                         @Param("exterior") String exterior,
                         @Param("quality") String quality,
                         @Param("keyword") String keyword,
                         @Param("minPrice") java.math.BigDecimal minPrice,
                         @Param("maxPrice") java.math.BigDecimal maxPrice);
}
