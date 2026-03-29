package com.cs2trade.mapper;

import com.cs2trade.entity.UserInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户库存数据访问层接口
 * 定义对用户库存表(user_inventory)的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface UserInventoryMapper {

    /**
     * 根据ID查询库存物品
     *
     * @param id 库存ID
     * @return UserInventory 库存实体对象
     */
    UserInventory selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询库存列表
     *
     * @param userId 用户ID
     * @return List<UserInventory> 库存列表
     */
    List<UserInventory> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询可交易的库存
     *
     * @param userId 用户ID
     * @return List<UserInventory> 可交易的库存列表
     */
    List<UserInventory> selectMarketableByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和Asset ID查询库存
     *
     * @param userId  用户ID
     * @param assetId Steam Asset ID
     * @return UserInventory 库存实体对象
     */
    UserInventory selectByUserIdAndAssetId(@Param("userId") Long userId, @Param("assetId") String assetId);

    /**
     * 插入新库存物品
     *
     * @param inventory 库存实体对象
     * @return int 影响的行数
     */
    int insert(UserInventory inventory);

    /**
     * 根据ID更新库存物品
     *
     * @param inventory 库存实体对象
     * @return int 影响的行数
     */
    int updateById(UserInventory inventory);

    /**
     * 根据ID删除库存物品
     *
     * @param id 库存ID
     * @return int 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID删除所有库存
     *
     * @param userId 用户ID
     * @return int 影响的行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除指定用户未被出售订单引用的本地库存快照
     *
     * @param userId 用户ID
     * @return int 影响的行数
     */
    int deleteUnreferencedByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID统计库存数量
     *
     * @param userId 用户ID
     * @return int 库存数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 更新库存状态
     *
     * @param id 库存ID
     * @param status 新状态
     * @return int 影响的行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除指定状态的库存
     *
     * @param status 状态
     * @return int 影响的行数
     */
    int deleteByStatus(@Param("status") Integer status);

    /**
     * 删除指定用户的所有已售出库存
     *
     * @param userId 用户 ID
     * @return int 影响的行数
     */
    int deleteSoldByUserId(@Param("userId") Long userId);

    /**
     * 查询所有 item_id 为 NULL 的库存记录
     *
     * @return List<UserInventory> item_id 为 NULL 的库存列表
     */
    List<UserInventory> selectWithNullItemId();
}
