CREATE TABLE IF NOT EXISTS sys_banner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT 'Banner标题',
    description VARCHAR(255) COMMENT 'Banner描述',
    image_url VARCHAR(255) NOT NULL COMMENT '图片地址',
    link_url VARCHAR(255) COMMENT '跳转链接',
    status TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图';

CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token VARCHAR(64) NOT NULL COMMENT '重置令牌',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    used TINYINT DEFAULT 0 COMMENT '是否已使用: 0-未使用, 1-已使用',
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置令牌表';

CREATE TABLE IF NOT EXISTS withdrawal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    bank_name VARCHAR(100) NOT NULL COMMENT '银行名称',
    bank_account VARCHAR(50) NOT NULL COMMENT '银行账号',
    account_name VARCHAR(50) NOT NULL COMMENT '账户名',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    reason VARCHAR(255) COMMENT '拒绝原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    processed_at DATETIME COMMENT '处理时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';
