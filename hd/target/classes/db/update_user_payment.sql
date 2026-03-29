-- ========================================================
-- 更新用户表结构
-- 添加支付宝和银行卡绑定字段
-- ========================================================

-- 添加支付宝相关字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_account VARCHAR(64) DEFAULT NULL COMMENT '支付宝账号' AFTER is_real_name;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS alipay_real_name VARCHAR(32) DEFAULT NULL COMMENT '支付宝真实姓名' AFTER alipay_account;

-- 添加银行卡相关字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bank_card_number VARCHAR(32) DEFAULT NULL COMMENT '银行卡号' AFTER alipay_real_name;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bank_real_name VARCHAR(32) DEFAULT NULL COMMENT '银行卡真实姓名' AFTER bank_card_number;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bank_name VARCHAR(32) DEFAULT NULL COMMENT '开户银行' AFTER bank_real_name;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS bank_phone VARCHAR(16) DEFAULT NULL COMMENT '银行预留手机号' AFTER bank_name;

-- 显示更新后的表结构
DESCRIBE sys_user;
