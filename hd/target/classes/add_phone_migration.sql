-- 添加手机号字段到sys_user表
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号码' AFTER email;
