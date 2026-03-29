-- 消息表
CREATE TABLE IF NOT EXISTS sys_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content VARCHAR(500) NOT NULL COMMENT '消息内容',
    type TINYINT DEFAULT 1 COMMENT '类型: 1交易 2系统 3还价',
    status TINYINT DEFAULT 0 COMMENT '状态: 0未读 1已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 收藏表
CREATE TABLE IF NOT EXISTS sys_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    item_id BIGINT COMMENT '饰品ID',
    news_id BIGINT COMMENT '资讯ID',
    type TINYINT DEFAULT 1 COMMENT '类型: 1饰品 2资讯',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 玩家秀点赞记录表
CREATE TABLE IF NOT EXISTS player_show_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL COMMENT '玩家秀ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_show_user (show_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家秀点赞记录表';

-- 为sys_news添加审核相关字段 (如果已存在会报错，但配置了 continue-on-error=true 则会忽略)
ALTER TABLE sys_news ADD COLUMN user_id BIGINT COMMENT '用户ID';
ALTER TABLE sys_news ADD COLUMN status INT DEFAULT 1 COMMENT '状态: 0=待审核, 1=已通过, 2=已拒绝, 3=草稿';
ALTER TABLE sys_news ADD COLUMN reject_reason VARCHAR(500) COMMENT '拒绝原因';
