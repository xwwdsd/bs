-- 资讯表
CREATE TABLE IF NOT EXISTS sys_news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '标题',
    summary VARCHAR(500) COMMENT '摘要',
    content TEXT COMMENT '内容',
    cover_image VARCHAR(255) COMMENT '封面图',
    category VARCHAR(50) DEFAULT 'CS2' COMMENT '分类: CS2, DOTA2',
    author VARCHAR(100) DEFAULT '官方' COMMENT '作者',
    user_id BIGINT COMMENT '用户ID',
    source VARCHAR(100) DEFAULT '原创' COMMENT '来源',
    views INT DEFAULT 0 COMMENT '浏览量',
    status INT DEFAULT 1 COMMENT '状态: 0=待审核, 1=已通过, 2=已拒绝, 3=草稿',
    reject_reason VARCHAR(500) COMMENT '拒绝原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯表';

-- 玩家秀表
CREATE TABLE IF NOT EXISTS sys_player_show (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    image_url VARCHAR(255) NOT NULL COMMENT '图片URL',
    description VARCHAR(500) COMMENT '描述',
    likes INT DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家秀表';

-- 玩家秀评论表
CREATE TABLE IF NOT EXISTS sys_player_show_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL COMMENT '秀ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家秀评论表';
