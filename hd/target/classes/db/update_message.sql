-- ========================================================
-- 更新消息表结构
-- 添加新字段支持完整的消息功能
-- ========================================================

-- 添加标题字段
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS title VARCHAR(200) DEFAULT NULL COMMENT '消息标题' AFTER user_id;

-- 添加子类型字段
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS sub_type TINYINT DEFAULT NULL COMMENT '子类型' AFTER type;

-- 添加关联订单号
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS related_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联订单号' AFTER status;

-- 添加关联商品ID
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS related_item_id BIGINT DEFAULT NULL COMMENT '关联商品ID' AFTER related_order_no;

-- 添加商品名称
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS item_name VARCHAR(200) DEFAULT NULL COMMENT '商品名称' AFTER related_item_id;

-- 添加还价金额
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS bargain_price DECIMAL(18, 2) DEFAULT NULL COMMENT '还价金额' AFTER item_name;

-- 添加发送者ID
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS sender_id BIGINT DEFAULT NULL COMMENT '发送者ID' AFTER bargain_price;

-- 添加发送者名称
ALTER TABLE sys_message ADD COLUMN IF NOT EXISTS sender_name VARCHAR(64) DEFAULT NULL COMMENT '发送者名称' AFTER sender_id;

-- 更新表注释
ALTER TABLE sys_message COMMENT = '消息表 - 支持交易消息、系统消息、还价留言';

-- 显示更新后的表结构
DESCRIBE sys_message;
