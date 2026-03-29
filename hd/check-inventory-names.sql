-- 1. 查看 user_inventory 表中 item_id = 38 的记录的 name 字段
SELECT id, name, item_id 
FROM user_inventory 
WHERE item_id = 38
LIMIT 10;

-- 2. 查看 item 表中是否有能匹配的记录
SELECT id, name_cn, name, icon_url 
FROM item 
WHERE is_active = 1
LIMIT 20;

-- 3. 尝试模糊匹配（使用 LIKE）
SELECT ui.id, ui.name as inventory_name, i.id as item_id, i.name_cn as item_name
FROM user_inventory ui
LEFT JOIN item i ON (i.name_cn LIKE CONCAT('%', ui.name, '%') OR i.name LIKE CONCAT('%', ui.name, '%'))
WHERE ui.item_id = 38
LIMIT 10;
