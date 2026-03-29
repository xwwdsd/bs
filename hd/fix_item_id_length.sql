-- ========================================================
-- 修复 item_id 字段长度问题
-- 解决 Steam market_hash_name 过长导致的数据截断错误
-- ========================================================

USE cs2_trade;

-- 修改 item 表的 item_id 字段长度从 VARCHAR(50) 到 VARCHAR(255)
ALTER TABLE item MODIFY COLUMN item_id VARCHAR(255) NOT NULL COMMENT '游戏内物品 ID（Steam market_hash_name）';

-- 验证修改结果
DESC item;
