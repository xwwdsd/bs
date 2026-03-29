package com.cs2trade.service;

import com.cs2trade.entity.UserInventory;

import java.util.List;

/**
 * Steam库存服务接口
 * 用于同步和管理用户的Steam库存
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface SteamInventoryService {

    /**
     * 同步用户Steam库存
     * 从Steam API获取用户库存并更新到数据库
     *
     * @param userId 用户ID
     * @return List<UserInventory> 同步后的库存列表
     */
    List<UserInventory> syncInventory(Long userId);

    /**
     * 获取用户库存列表
     *
     * @param userId 用户ID
     * @return List<UserInventory> 库存列表
     */
    List<UserInventory> getUserInventory(Long userId);

    /**
     * 获取用户可交易的库存
     *
     * @param userId 用户ID
     * @return List<UserInventory> 可交易的库存列表
     */
    List<UserInventory> getMarketableInventory(Long userId);

    /**
     * 根据ID获取库存物品
     *
     * @param id 库存ID
     * @return UserInventory 库存物品
     */
    UserInventory getInventoryById(Long id);

    /**
     * 删除库存物品
     *
     * @param id 库存ID
     */
    void deleteInventory(Long id);

    /**
     * 清空用户库存
     *
     * @param userId 用户 ID
     */
    void clearUserInventory(Long userId);

    /**
     * 修复 item_id 为 NULL 的库存记录
     * 重新匹配物品并更新 item_id 字段
     */
    void fixNullItemIds();
}
