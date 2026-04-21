UPDATE sys_user
SET email = NULL
WHERE TRIM(email) = '';

ALTER TABLE sys_user
    MODIFY COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址';
