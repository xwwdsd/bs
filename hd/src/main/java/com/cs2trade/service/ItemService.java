package com.cs2trade.service;

import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.Item;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 饰品服务接口
 * 定义饰品相关的业务逻辑方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface ItemService {

    /**
     * 根据ID查询饰品
     *
     * @param id 饰品ID
     * @return Item 饰品实体对象
     */
    Item getItemById(Long id);

    /**
     * 分页查询饰品列表
     *
     * @param category  分类筛选
     * @param exterior  外观筛选
     * @param quality   品质筛选
     * @param keyword   关键词搜索
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @param page      页码
     * @param size      每页数量
     * @return PageResult<Item> 分页结果
     */
    PageResult<Item> getItemList(String category, String exterior, String quality,
                                  String keyword, String sortField, String sortOrder,
                                  Integer page, Integer size);

    /**
     * 获取所有启用的饰品
     *
     * @return List<Item> 饰品列表
     */
    List<Item> getAllActiveItems();

    /**
     * 根据分类查询饰品
     *
     * @param category 分类
     * @return List<Item> 饰品列表
     */
    List<Item> getItemsByCategory(String category);

    /**
     * 搜索饰品
     *
     * @param keyword 关键词
     * @return List<Item> 饰品列表
     */
    List<Item> searchItems(String keyword);

    /**
     * 获取所有分类
     *
     * @return List<String> 分类列表
     */
    List<String> getAllCategories();

    /**
     * 获取所有外观类型
     *
     * @return List<String> 外观列表
     */
    List<String> getAllExteriors();

    /**
     * 获取所有品质
     *
     * @return List<String> 品质列表
     */
    List<String> getAllQualities();

    /**
     * 添加饰品（管理员）
     *
     * @param item 饰品实体
     * @return Item 添加后的饰品
     */
    Item addItem(Item item);

    /**
     * 更新饰品（管理员）
     *
     * @param item 饰品实体
     * @return Item 更新后的饰品
     */
    Item updateItem(Item item);

    /**
     * 删除饰品（管理员）
     *
     * @param id 饰品 ID
     */
    void deleteItem(Long id);

    /**
     * 从 Steam API 同步饰品数据
     * 获取 CS2 所有饰品的信息并更新到数据库
     *
     * @return int 同步的饰品数量
     */
    int syncItemsFromSteam();

    /**
     * 异步从 Steam API 同步饰品数据
     *
     * @return CompletableFuture<Integer> 同步的饰品数量
     */
    CompletableFuture<Integer> syncItemsFromSteamAsync();

    /**
     * 获取 Steam 饰品同步任务状态
     *
     * @return Map<String, Object> 状态信息
     */
    Map<String, Object> getSteamSyncStatus();
}
