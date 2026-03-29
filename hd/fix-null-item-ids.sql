-- 修复 item_id 为 NULL 的库存记录
-- 这个脚本会查询所有 item_id 为 NULL 的记录，并根据 name 字段匹配 item 表中的数据

-- 1. 首先查看有多少条记录需要修复
SELECT COUNT(*) AS null_item_id_count 
FROM user_inventory 
WHERE item_id IS NULL;

-- 2. 查看这些记录的详细信息
SELECT id, name, icon_url, exterior 
FROM user_inventory 
WHERE item_id IS NULL
LIMIT 20;

-- 3. 查看 item 表中有哪些物品可以用于匹配
SELECT id, name_cn, name, icon_url 
FROM item 
WHERE is_active = 1
ORDER BY id;

-- 4. 更新匹配的物品 ID（示例：如果 name 包含'未知饰品'，设置为 item_id=38）
-- 注意：这个需要根据实际情况调整匹配逻辑
UPDATE user_inventory ui
SET item_id = (
    SELECT i.id 
    FROM item i 
    WHERE i.name_cn = ui.name OR i.name = ui.name
    LIMIT 1
)
WHERE ui.item_id IS NULL;

-- 5. 对于无法匹配的记录，设置为未知饰品 (id=38)
UPDATE user_inventory 
SET item_id = 38 
WHERE item_id IS NULL;

-- 6. 验证修复结果
SELECT COUNT(*) AS remaining_null_count 
FROM user_inventory 
WHERE item_id IS NULL;
