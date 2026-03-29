-- 为sys_news表添加status和reject_reason字段
-- status: 0=待审核, 1=已通过, 2=已拒绝, 3=草稿
-- reject_reason: 拒绝原因

ALTER TABLE sys_news 
ADD COLUMN status INT DEFAULT 1 COMMENT '状态: 0=待审核, 1=已通过, 2=已拒绝, 3=草稿',
ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因';

-- 将现有数据默认设为已通过状态
UPDATE sys_news SET status = 1 WHERE status IS NULL;
