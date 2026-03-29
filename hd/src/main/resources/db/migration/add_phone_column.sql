-- 添加手机号字段到sys_user表
-- 执行此脚本前请先备份数据库

-- 添加phone字段
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号码' AFTER email;

-- 添加唯一索引
ALTER TABLE sys_user ADD UNIQUE KEY uk_phone (phone);
