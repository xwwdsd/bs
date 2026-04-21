package com.cs2trade.mapper;

import com.cs2trade.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 饰品数据访问层接口
 * 定义对饰品表(item)的数据库操作方法
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface ItemMapper {

    /**
     * 根据ID查询饰品
     *
     * @param id 饰品ID
     * @return Item 饰品实体对象，不存在时返回null
     */
    Item selectById(@Param("id") Long id);

    /**
     * 根据游戏内物品ID查询饰品
     *
     * @param itemId 游戏内物品ID
     * @return Item 饰品实体对象，不存在时返回null
     */
    Item selectByItemId(@Param("itemId") String itemId);

    /**
     * 查询所有启用的饰品
     *
     * @return List<Item> 饰品列表
     */
    List<Item> selectAllActive();

    /**
     * 分页查询饰品列表
     *
     * @param category   分类筛选
     * @param exterior   外观筛选
     * @param quality    品质筛选
     * @param keyword    关键词搜索
     * @param sortField  排序字段
     * @param sortOrder  排序方式
     * @param offset     偏移量
     * @param limit      每页数量
     * @return List<Item> 饰品列表
     */
    List<Item> selectList(@Param("category") String category,
                          @Param("exterior") String exterior,
                          @Param("quality") String quality,
                          @Param("keyword") String keyword,
                          @Param("sortField") String sortField,
                          @Param("sortOrder") String sortOrder,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);

    /**
     * 查询符合条件的饰品总数
     *
     * @param category 分类筛选
     * @param exterior 外观筛选
     * @param quality  品质筛选
     * @param keyword  关键词搜索
     * @return int 总数
     */
    int selectCount(@Param("category") String category,
                    @Param("exterior") String exterior,
                    @Param("quality") String quality,
                    @Param("keyword") String keyword);

    /**
     * 插入新饰品
     *
     * @param item 饰品实体对象
     * @return int 影响的行数
     */
    int insert(Item item);

    /**
     * 根据ID更新饰品信息
     *
     * @param item 饰品实体对象
     * @return int 影响的行数
     */
    int updateById(Item item);

    @Update("""
            UPDATE item
            SET icon_url = #{iconUrl}, updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateIconUrl(@Param("id") Long id, @Param("iconUrl") String iconUrl);

    /**
     * 根据ID删除饰品
     *
     * @param id 饰品ID
     * @return int 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据分类查询饰品
     *
     * @param category 分类
     * @return List<Item> 饰品列表
     */
    List<Item> selectByCategory(@Param("category") String category);

    /**
     * 根据外观查询饰品
     *
     * @param exterior 外观
     * @return List<Item> 饰品列表
     */
    List<Item> selectByExterior(@Param("exterior") String exterior);

    /**
     * 根据品质查询饰品
     *
     * @param quality 品质
     * @return List<Item> 饰品列表
     */
    List<Item> selectByQuality(@Param("quality") String quality);

    /**
     * 搜索饰品（按名称）
     *
     * @param keyword 关键词
     * @return List<Item> 饰品列表
     */
    List<Item> searchByName(@Param("keyword") String keyword);

    /**
     * 获取所有分类
     *
     * @return List<String> 分类列表
     */
    List<String> selectAllCategories();

    /**
     * 获取所有外观类型
     *
     * @return List<String> 外观列表
     */
    List<String> selectAllExteriors();

    /**
     * 获取所有品质
     *
     * @return List<String> 品质列表
     */
    List<String> selectAllQualities();

    /**
     * 查询所有饰品（后台管理用）
     *
     * @return List<Item> 饰品列表
     */
    List<Item> selectAll();
}
