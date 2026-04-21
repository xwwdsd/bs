package com.cs2trade.controller;

import com.cs2trade.dto.PageResult;
import com.cs2trade.dto.Result;
import com.cs2trade.entity.Item;
import com.cs2trade.service.ItemService;
import com.cs2trade.service.MarketAnalyticsService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 饰品控制器
 * 处理饰品相关的请求，包括浏览、搜索、筛选等
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/items")
@RequiredArgsConstructor
@CrossOrigin
public class ItemController {

    private final ItemService itemService;
    private final MarketAnalyticsService marketAnalyticsService;
    private final JdbcTemplate jdbcTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    private Long getOptionalUserId() {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isBlank()) {
            return null;
        }

        try {
            return jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行分类数据更新
     *
     * @return Result<String> 执行结果
     */
    @PostMapping("/update-categories")
    public Result<String> updateCategories() {
        try {
            // 读取并执行 SQL 文件
            String sql = """
                UPDATE item SET 
                    category = 'knife',
                    sub_category = 'butterfly',
                    name_cn = '蝴蝶刀'
                WHERE name LIKE '%Butterfly Knife%';
                
                UPDATE item SET 
                    category = 'knife',
                    sub_category = 'm9_bayonet',
                    name_cn = 'M9 刺刀'
                WHERE name LIKE '%M9 Bayonet%';
                
                UPDATE item SET 
                    category = 'knife',
                    sub_category = 'karambit',
                    name_cn = '爪子刀'
                WHERE name LIKE '%Karambit%';
                
                UPDATE item SET 
                    category = 'glove',
                    sub_category = 'sport',
                    name_cn = '运动手套'
                WHERE name LIKE '%Sport Gloves%';
                
                UPDATE item SET 
                    category = 'glove',
                    sub_category = 'specialist',
                    name_cn = '专业手套'
                WHERE name LIKE '%Specialist Gloves%';
                
                UPDATE item SET 
                    category = 'weapon',
                    sub_category = 'ak47',
                    name_cn = 'AK-47'
                WHERE name LIKE '%AK-47%';
                
                UPDATE item SET 
                    category = 'weapon',
                    sub_category = 'awp',
                    name_cn = 'AWP'
                WHERE name LIKE '%AWP%';
                
                UPDATE item SET 
                    category = 'weapon',
                    sub_category = 'm4a1_silencer',
                    name_cn = 'M4A1 消音版'
                WHERE name LIKE '%M4A1-S%';
                
                UPDATE item SET 
                    category = 'weapon',
                    sub_category = 'mp9',
                    name_cn = 'MP9'
                WHERE name LIKE '%MP9%';
                
                UPDATE item SET 
                    category = 'weapon',
                    sub_category = 'deagle',
                    name_cn = '沙漠之鹰'
                WHERE name LIKE '%Desert Eagle%';
                """;
            
            jdbcTemplate.execute(sql);
            return Result.success("分类数据更新成功");
        } catch (Exception e) {
            log.error("更新分类数据失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 获取饰品列表（支持分页、筛选、搜索）
     *
     * @param category  分类筛选
     * @param exterior  外观筛选
     * @param quality   品质筛选
     * @param keyword   关键词搜索
     * @param sortField 排序字段
     * @param sortOrder 排序方式
     * @param page      页码
     * @param size      每页数量
     * @return Result<PageResult<Item>> 分页结果
     */
    @GetMapping
    public Result<PageResult<Item>> getItemList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String exterior,
            @RequestParam(required = false) String quality,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "created_at") String sortField,
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("获取饰品列表: category={}, exterior={}, quality={}, keyword={}, page={}, size={}",
                category, exterior, quality, keyword, page, size);

        PageResult<Item> result = itemService.getItemList(category, exterior, quality, keyword,
                sortField, sortOrder, page, size);

        return Result.success(result);
    }

    /**
     * 根据ID获取饰品详情
     *
     * @param id 饰品ID
     * @return Result<Item> 饰品详情
     */
    @GetMapping("/{id}")
    public Result<Item> getItemById(@PathVariable Long id) {
        log.info("获取饰品详情: id={}", id);

        Item item = itemService.getItemById(id);
        if (item == null) {
            return Result.error(404, "饰品不存在");
        }

        return Result.success(item);
    }

    /**
     * 获取所有启用的饰品
     *
     * @return Result<List<Item>> 饰品列表
     */
    @GetMapping("/{id}/market-panel")
    public Result<?> getMarketPanel(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        if (item == null) {
            return Result.error(404, "楗板搧涓嶅瓨鍦?");
        }
        return Result.success(marketAnalyticsService.getMarketPanel(id));
    }

    @GetMapping("/all")
    public Result<List<Item>> getAllActiveItems() {
        log.info("获取所有启用的饰品");

        List<Item> items = itemService.getAllActiveItems();
        return Result.success(items);
    }

    /**
     * 根据分类获取饰品
     *
     * @param category 分类
     * @return Result<List<Item>> 饰品列表
     */
    @GetMapping("/category/{category}")
    public Result<List<Item>> getItemsByCategory(@PathVariable String category) {
        log.info("根据分类获取饰品: category={}", category);

        List<Item> items = itemService.getItemsByCategory(category);
        return Result.success(items);
    }

    /**
     * 搜索饰品
     *
     * @param keyword 关键词
     * @return Result<List<Item>> 饰品列表
     */
    @GetMapping("/search")
    public Result<List<Item>> searchItems(@RequestParam String keyword) {
        log.info("搜索饰品: keyword={}", keyword);

        List<Item> items = itemService.searchItems(keyword);
        return Result.success(items);
    }

    /**
     * 获取所有分类
     *
     * @return Result<List<String>> 分类列表
     */
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        log.info("获取所有分类");

        List<String> categories = itemService.getAllCategories();
        return Result.success(categories);
    }

    /**
     * 获取所有外观类型
     *
     * @return Result<List<String>> 外观列表
     */
    @GetMapping("/exteriors")
    public Result<List<String>> getAllExteriors() {
        log.info("获取所有外观类型");

        List<String> exteriors = itemService.getAllExteriors();
        return Result.success(exteriors);
    }

    /**
     * 获取所有品质
     *
     * @return Result<List<String>> 品质列表
     */
    @GetMapping("/qualities")
    public Result<List<String>> getAllQualities() {
        log.info("获取所有品质");

        List<String> qualities = itemService.getAllQualities();
        return Result.success(qualities);
    }

    // ==================== 管理员接口 ====================

    /**
     * 添加饰品（管理员）
     *
     * @param item 饰品信息
     * @return Result<Item> 添加后的饰品
     */
    @GetMapping("/recommendations")
    public Result<?> getRecommendations(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(marketAnalyticsService.getRecommendations(getOptionalUserId(), limit == null ? 8 : limit));
    }

    @PostMapping
    public Result<Item> addItem(@RequestBody Item item) {
        log.info("添加饰品: name={}", item.getName());

        Item result = itemService.addItem(item);
        return Result.success("添加成功", result);
    }

    /**
     * 更新饰品（管理员）
     *
     * @param id   饰品ID
     * @param item 饰品信息
     * @return Result<Item> 更新后的饰品
     */
    @PutMapping("/{id}")
    public Result<Item> updateItem(@PathVariable Long id, @RequestBody Item item) {
        log.info("更新饰品: id={}", id);

        item.setId(id);
        Item result = itemService.updateItem(item);
        return Result.success("更新成功", result);
    }

    /**
     * 删除饰品（管理员）
     *
     * @param id 饰品 ID
     * @return Result<Void>
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        log.info("删除饰品：id={}", id);

        itemService.deleteItem(id);
        return Result.success("删除成功", null);
    }

    /**
     * 从 Steam API 同步饰品数据（管理员）- 异步方式
     *
     * @return Result<String> 任务已启动
     */
    @PostMapping("/sync-steam/async")
    public Result<String> syncItemsFromSteamAsync() {
        log.info("管理员请求从 Steam API 异步同步饰品数据");
        
        try {
            // 异步执行同步任务
            itemService.syncItemsFromSteamAsync();
            return Result.success("同步任务已启动，请在后台查看日志监控进度");
        } catch (Exception e) {
            log.error("启动 Steam 饰品同步失败", e);
            return Result.error("启动失败：" + e.getMessage());
        }
    }

    /**
     * 获取 Steam 饰品同步状态
     *
     * @return Result<Map<String, Object>> 同步状态
     */
    @GetMapping("/sync-steam/status")
    public Result<Map<String, Object>> getSteamSyncStatus() {
        return Result.success(itemService.getSteamSyncStatus());
    }

    /**
     * 从 Steam API 同步饰品数据（管理员）- 同步方式（已废弃，会超时）
     *
     * @return Result<Integer> 同步的饰品数量
     */
    @PostMapping("/sync-steam")
    @Deprecated
    public Result<Integer> syncItemsFromSteam() {
        log.info("管理员请求从 Steam API 同步饰品数据（同步方式）");

        try {
            int count = itemService.syncItemsFromSteam();
            return Result.success("同步完成，共更新 " + count + " 个饰品", count);
        } catch (Exception e) {
            log.error("同步 Steam 饰品失败", e);
            return Result.error("同步失败：" + e.getMessage());
        }
    }
}
