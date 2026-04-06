-- ========================================================
-- CS2饰品交易平台 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ========================================================

-- 创建数据库(如果不存在)
CREATE DATABASE IF NOT EXISTS cs2trade
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE cs2trade;

-- ========================================================
-- 1. 用户相关表
-- ========================================================

-- --------------------------------------------------------
-- 表结构: sys_user (用户基础表)
-- 说明: 存储平台用户的基本信息
-- --------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    -- 主键ID，自增
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    
    -- 用户基本信息
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    
    -- Steam相关信息
    steam_id VARCHAR(50) DEFAULT NULL COMMENT 'Steam64ID',
    steam_trade_url VARCHAR(255) DEFAULT NULL COMMENT 'Steam交易链接',
    steam_api_key VARCHAR(50) DEFAULT NULL COMMENT 'Steam API Key',
    
    -- 账号状态
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账号状态: 0-禁用, 1-正常, 2-待验证',
    user_level TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户等级: 0-普通, 1-VIP, 2-管理员, 3-超级管理员',
    is_real_name TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否实名认证: 0-否, 1-是',
    
    -- 登录信息
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    
    -- 时间戳
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 主键约束
    PRIMARY KEY (id),
    
    -- 唯一索引
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_steam_id (steam_id),
    UNIQUE KEY uk_phone (phone),
    
    -- 普通索引
    KEY idx_status (status),
    KEY idx_user_level (user_level),
    KEY idx_created_at (created_at)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础表';

-- --------------------------------------------------------
-- 表结构: user_wallet (用户钱包表)
-- 说明: 存储用户的资金信息
-- --------------------------------------------------------
DROP TABLE IF EXISTS user_wallet;
CREATE TABLE user_wallet (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    frozen_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    total_recharge DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计充值',
    total_withdraw DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计提现',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    
    -- 外键约束
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户钱包表';

-- --------------------------------------------------------
-- 表结构: wallet_transaction (钱包流水表)
-- 说明: 记录用户钱包的所有资金变动
-- --------------------------------------------------------
DROP TABLE IF EXISTS wallet_transaction;
CREATE TABLE wallet_transaction (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    type TINYINT UNSIGNED NOT NULL COMMENT '交易类型: 1-充值, 2-提现, 3-收入, 4-支出, 5-冻结, 6-解冻',
    amount DECIMAL(18, 2) NOT NULL COMMENT '变动金额(正数增加,负数减少)',
    balance DECIMAL(18, 2) NOT NULL COMMENT '变动后余额',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_type (type),
    KEY idx_order_no (order_no),
    KEY idx_created_at (created_at),
    
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包流水表';

-- ========================================================
-- 2. 饰品相关表
-- ========================================================

-- --------------------------------------------------------
-- 表结构: item (饰品基础表)
-- 说明: 存储CS2饰品的基础信息
-- --------------------------------------------------------
DROP TABLE IF EXISTS item;
CREATE TABLE item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '饰品ID',
    item_id VARCHAR(50) NOT NULL COMMENT '游戏内物品ID',
    name VARCHAR(100) NOT NULL COMMENT '饰品英文名称',
    name_cn VARCHAR(100) DEFAULT NULL COMMENT '饰品中文名称',
    category VARCHAR(50) NOT NULL COMMENT '饰品分类: weapon/knife/glove/sticker/case/other',
    sub_category VARCHAR(50) DEFAULT NULL COMMENT '子分类(如武器类型)',
    quality VARCHAR(50) DEFAULT NULL COMMENT '品质: covert/classified/restricted/mil-spec/industrial/consumer',
    rarity VARCHAR(50) DEFAULT NULL COMMENT '稀有度',
    exterior VARCHAR(50) DEFAULT NULL COMMENT '外观磨损: FN/MW/FT/WW/BS',
    icon_url VARCHAR(255) DEFAULT NULL COMMENT '饰品图标URL',
    inspect_link_template VARCHAR(255) DEFAULT NULL COMMENT '检视链接模板',
    steam_market_url VARCHAR(255) DEFAULT NULL COMMENT 'Steam市场链接',
    buff_price DECIMAL(18, 2) DEFAULT NULL COMMENT 'Buff参考价格',
    is_active TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_id (item_id),
    KEY idx_category (category),
    KEY idx_sub_category (sub_category),
    KEY idx_quality (quality),
    KEY idx_exterior (exterior),
    KEY idx_is_active (is_active)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饰品基础表';

-- --------------------------------------------------------
-- 表结构: user_inventory (用户库存表)
-- 说明: 存储用户的Steam库存快照
-- --------------------------------------------------------
DROP TABLE IF EXISTS user_inventory;
CREATE TABLE user_inventory (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    item_id BIGINT UNSIGNED NOT NULL COMMENT '饰品ID',
    asset_id VARCHAR(50) NOT NULL COMMENT 'Steam Asset ID',
    class_id VARCHAR(50) NOT NULL COMMENT 'Steam Class ID',
    instance_id VARCHAR(50) NOT NULL COMMENT 'Steam Instance ID',
    name VARCHAR(100) NOT NULL COMMENT '物品名称',
    icon_url VARCHAR(255) DEFAULT NULL COMMENT '物品图标URL',
    icon_url_large VARCHAR(255) DEFAULT NULL COMMENT '物品大图URL',
    exterior VARCHAR(50) DEFAULT NULL COMMENT '外观磨损',
    paint_seed INT UNSIGNED DEFAULT NULL COMMENT '图案模板号',
    paint_index INT UNSIGNED DEFAULT NULL COMMENT '皮肤编号 / Finish Catalog',
    paint_wear DECIMAL(10, 8) DEFAULT NULL COMMENT '磨损度数值',
    inspect_url VARCHAR(1000) DEFAULT NULL COMMENT '检视链接',
    description TEXT DEFAULT NULL COMMENT '物品描述',
    marketable_reason VARCHAR(255) DEFAULT NULL COMMENT '不可出售原因',
    rarity VARCHAR(64) DEFAULT NULL COMMENT '前端展示用稀有度',
    type VARCHAR(64) DEFAULT NULL COMMENT '前端展示用类型',
    is_marketable TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否可交易: 0-否, 1-是',
    market_price DECIMAL(18, 2) DEFAULT NULL COMMENT '市场价格',
    acquired_at DATETIME DEFAULT NULL COMMENT '获取时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-待确认, 1-正常, 2-交易中, 3-已锁定',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_asset (user_id, asset_id),
    KEY idx_user_id (user_id),
    KEY idx_item_id (item_id),
    KEY idx_is_marketable (is_marketable),
    
    CONSTRAINT fk_inventory_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES item(id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户库存表';

-- ========================================================
-- 3. 交易相关表
-- ========================================================

-- --------------------------------------------------------
-- 表结构: buy_order (求购订单表)
-- 说明: 存储用户的求购订单
-- --------------------------------------------------------
DROP TABLE IF EXISTS buy_order;
CREATE TABLE buy_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '求购ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    item_id BIGINT UNSIGNED NOT NULL COMMENT '饰品ID',
    price DECIMAL(18, 2) NOT NULL COMMENT '求购单价',
    quantity INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '求购数量',
    filled_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已成交数量',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-已取消, 1-进行中, 2-已完成',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    auto_accept TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否自动接受: 0-否, 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_item_id (item_id),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time),
    
    CONSTRAINT fk_buy_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_buy_order_item FOREIGN KEY (item_id) REFERENCES item(id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求购订单表';

-- --------------------------------------------------------
-- 表结构: sell_order (供应订单表)
-- 说明: 存储用户的供应(出售)订单
-- --------------------------------------------------------
DROP TABLE IF EXISTS sell_order;
CREATE TABLE sell_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '供应ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    item_id BIGINT UNSIGNED NOT NULL COMMENT '饰品ID',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '库存ID',
    price DECIMAL(18, 2) NOT NULL COMMENT '出售价格',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-已取消, 1-在售, 2-交易中, 3-已售出',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    is_response_to_buy TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否响应求购: 0-否, 1-是',
    target_buy_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标求购订单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_item_id (item_id),
    KEY idx_status (status),
    KEY idx_expire_time (expire_time),
    
    CONSTRAINT fk_sell_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_sell_order_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_sell_order_inventory FOREIGN KEY (inventory_id) REFERENCES user_inventory(id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应订单表';

-- --------------------------------------------------------
-- 表结构: trade_order (交易订单表)
-- 说明: 存储实际成交的交易订单
-- --------------------------------------------------------
DROP TABLE IF EXISTS trade_order;
CREATE TABLE trade_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '交易ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    buyer_id BIGINT UNSIGNED NOT NULL COMMENT '买家ID',
    seller_id BIGINT UNSIGNED NOT NULL COMMENT '卖家ID',
    item_id BIGINT UNSIGNED NOT NULL COMMENT '饰品ID',
    inventory_id BIGINT UNSIGNED NOT NULL COMMENT '库存ID',
    price DECIMAL(18, 2) NOT NULL COMMENT '成交价格',
    fee DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
    actual_amount DECIMAL(18, 2) NOT NULL COMMENT '实际到账金额',
      status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0-待确认, 1-报价中, 2-待发货, 3-待收货, 4-已完成, 5-已取消, 6-纠纷中',
      trade_offer_id VARCHAR(100) DEFAULT NULL COMMENT 'Steam交易报价ID',
      trade_offer_url VARCHAR(255) DEFAULT NULL COMMENT '交易报价链接',
      delivery_stage VARCHAR(32) DEFAULT 'NONE' COMMENT '机器人交付阶段',
      steam_offer_state INT DEFAULT NULL COMMENT 'Steam报价状态码',
      steam_offer_state_text VARCHAR(64) DEFAULT NULL COMMENT 'Steam报价状态文本',
      bot_offer_dispatched_at DATETIME DEFAULT NULL COMMENT '机器人报价发出时间',
      seller_offer_confirmed_at DATETIME DEFAULT NULL COMMENT '卖家确认报价时间',
      last_offer_check_at DATETIME DEFAULT NULL COMMENT '最近一次检查报价时间',
      inventory_verified_at DATETIME DEFAULT NULL COMMENT '检测到对方库存出现饰品时间',
      bot_received_at DATETIME DEFAULT NULL COMMENT '机器人确认收到饰品时间',
      monitor_error_message VARCHAR(500) DEFAULT NULL COMMENT '监控错误信息',
      paid_at DATETIME DEFAULT NULL COMMENT '付款时间',
    sent_at DATETIME DEFAULT NULL COMMENT '发货时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    cancelled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_buyer_id (buyer_id),
    KEY idx_seller_id (seller_id),
    KEY idx_item_id (item_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    
    CONSTRAINT fk_trade_order_buyer FOREIGN KEY (buyer_id) REFERENCES sys_user(id),
    CONSTRAINT fk_trade_order_seller FOREIGN KEY (seller_id) REFERENCES sys_user(id),
    CONSTRAINT fk_trade_order_item FOREIGN KEY (item_id) REFERENCES item(id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单表';

-- ========================================================
-- 4. 支付相关表
-- ========================================================

-- --------------------------------------------------------
-- 表结构: recharge_order (充值记录表)
-- 说明: 存储用户的充值记录
-- --------------------------------------------------------
DROP TABLE IF EXISTS recharge_order;
CREATE TABLE recharge_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '充值ID',
    order_no VARCHAR(64) NOT NULL COMMENT '充值订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    amount DECIMAL(18, 2) NOT NULL COMMENT '充值金额',
    channel VARCHAR(50) NOT NULL COMMENT '充值渠道: alipay/wechat',
    channel_order_no VARCHAR(100) DEFAULT NULL COMMENT '渠道订单号',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    
    CONSTRAINT fk_recharge_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录表';

-- --------------------------------------------------------
-- 表结构: withdraw_order (提现记录表)
-- 说明: 存储用户的提现申请记录
-- --------------------------------------------------------
DROP TABLE IF EXISTS withdraw_order;
CREATE TABLE withdraw_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '提现ID',
    order_no VARCHAR(64) NOT NULL COMMENT '提现订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    amount DECIMAL(18, 2) NOT NULL COMMENT '提现金额',
    fee DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '手续费',
    actual_amount DECIMAL(18, 2) NOT NULL COMMENT '实际到账金额',
    bank_card_id BIGINT UNSIGNED NOT NULL COMMENT '银行卡ID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-审核通过, 2-审核拒绝, 3-已打款',
    audited_by BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
    audited_at DATETIME DEFAULT NULL COMMENT '审核时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    
    CONSTRAINT fk_withdraw_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现记录表';

-- ========================================================
-- 5. 初始化数据
-- ========================================================

-- 插入超级管理员账号
-- 密码: admin123 (BCrypt加密后的值)
INSERT INTO sys_user (username, email, password, status, user_level, is_real_name, created_at, updated_at) VALUES
('admin', 'admin@cs2trade.com', '$2a$10$7JB720yubVSOfvXZ/SCFWu6P7D5C/DRuAdpiPqQdQnGxN3zF2dK2i', 1, 3, 1, NOW(), NOW());

-- 为管理员创建钱包
INSERT INTO user_wallet (user_id, balance, frozen_amount, total_recharge, total_withdraw, created_at, updated_at) VALUES
(1, 0.00, 0.00, 0.00, 0.00, NOW(), NOW());

-- ========================================================
-- 完成
-- ========================================================
