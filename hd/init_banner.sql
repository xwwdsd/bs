CREATE TABLE IF NOT EXISTS sys_banner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT 'Banner Title',
    description VARCHAR(255) COMMENT 'Banner Description',
    image_url VARCHAR(255) NOT NULL COMMENT 'Image URL',
    link_url VARCHAR(255) COMMENT 'Link URL',
    status TINYINT DEFAULT 1 COMMENT 'Status: 0 Disabled, 1 Enabled',
    sort_order INT DEFAULT 0 COMMENT 'Sort Order',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Home Banner';

INSERT INTO sys_banner (title, description, image_url, sort_order) VALUES 
('网易UU远程', '手机随时随地畅玩CS2', '/uploads/banner1.jpg', 1);
