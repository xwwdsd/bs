-- 资讯表
CREATE TABLE IF NOT EXISTS sys_news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '标题',
    summary VARCHAR(500) COMMENT '摘要',
    content TEXT COMMENT '内容',
    cover_image VARCHAR(255) COMMENT '封面图',
    category VARCHAR(50) DEFAULT 'CS2' COMMENT '分类: CS2, DOTA2',
    author VARCHAR(100) DEFAULT '官方' COMMENT '作者',
    source VARCHAR(100) DEFAULT '原创' COMMENT '来源',
    views INT DEFAULT 0 COMMENT '浏览量',
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

-- 插入一些模拟数据
INSERT INTO sys_news (title, summary, cover_image, category, author, source, created_at) VALUES
('rain谈适应指挥位：暂停时间一开始，我都会忘记自己是指挥位', 'FaZe Clan的指挥rain在最近的采访中谈到了他适应指挥角色的过程...', 'https://img.buff.163.com/202403/blob_1709606400000.jpg', 'CS2', 'Gekyume13', 'BUFF', NOW()),
('官宣：9INE签下flayy替换shield', '波兰战队9INE宣布签下前ENCE Academy的狙击手flayy...', 'https://img.buff.163.com/202403/blob_1709606400001.jpg', 'CS2', 'Gekyume13', 'BUFF', NOW()),
('赛事主办方FISSURE取消三项赛事，原裂变天地深圳站取消', '由于不可抗力因素，FISSURE宣布取消原定于深圳举办的裂变天地赛事...', 'https://img.buff.163.com/202403/blob_1709606400002.jpg', 'CS2', 'Gekyume13', 'BUFF', NOW()),
('创意工坊优秀皮肤推荐：闪耀机甲AK Boost Protocol', '本周创意工坊优秀皮肤推荐...', 'https://img.buff.163.com/202403/blob_1709606400003.jpg', 'CS2', '小玥玥', 'BUFF', DATE_SUB(NOW(), INTERVAL 1 DAY));
