-- 密码重置令牌表
CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token VARCHAR(64) NOT NULL COMMENT '重置令牌',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    used TINYINT DEFAULT 0 COMMENT '是否已使用: 0-未使用, 1-已使用',
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';
