-- 重置用户密码为 123456
-- 使用BCrypt加密，$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO 是 123456 的加密值
UPDATE cs2trade.sys_user 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO'
WHERE id = 2;

-- 验证更新
SELECT id, username, email FROM cs2trade.sys_user WHERE id = 2;
