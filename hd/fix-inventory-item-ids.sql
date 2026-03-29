-- 修复 user_inventory 表中 item_id = 38 的记录
-- 这个脚本会将 item_id = 38 的记录根据名称重新匹配到正确的物品 ID

-- 1. 首先查看有多少条记录需要修复（item_id = 38）
SELECT COUNT(*) AS item_id_38_count 
FROM user_inventory 
WHERE item_id = 38;

-- 2. 查看这些记录的详细信息（前 20 条）
SELECT id, name, item_id 
FROM user_inventory 
WHERE item_id = 38
LIMIT 20;

-- 3. 查找"AK-47 | 精英行动"在 item 表中的 ID
SELECT id, name_cn, name, icon_url 
FROM item 
WHERE name_cn LIKE '%AK-47%' OR name LIKE '%AK-47%'
LIMIT 10;

-- 4. 查找"截短霰弹枪 | 旱地之花"在 item 表中的 ID
SELECT id, name_cn, name, icon_url 
FROM item 
WHERE name_cn LIKE '%截短%' OR name_cn LIKE '%旱地%' OR name LIKE '%Sawed-Off%'
LIMIT 10;

-- 5. 更新匹配的物品 ID（根据名称精确匹配）
-- 注意：这个 UPDATE 语句会根据 name 字段匹配 item 表的 name_cn
UPDATE user_inventory ui
INNER JOIN item i ON (ui.name = i.name_cn OR ui.name = i.name)
SET ui.item_id = i.id
WHERE ui.item_id = 38;

-- 6. 查看还有多少条记录未修复
SELECT COUNT(*) AS remaining_item_id_38_count 
FROM user_inventory 
WHERE item_id = 38;

-- 7. 查看剩余未匹配的记录名称
SELECT DISTINCT name 
FROM user_inventory 
WHERE item_id = 38
LIMIT 20;
