-- CS2 饰品数据导入脚本
-- 执行前请确保已连接到 cs2trade 数据库

-- 清空现有数据（保留未知饰品）
DELETE FROM item WHERE id != 1;

-- 插入步枪类饰品
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_ak47_0', 'AK-47', 'AK-47', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_ak47.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_m4a1_0', 'M4A4', 'M4A4', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_m4a1.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_m4a1_silencer_0', 'M4A1-S', 'M4A1-S', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_m4a1_silencer.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_awp_0', 'AWP', 'AWP', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_awp.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_ssg08_0', 'SSG 08', 'SSG 08', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_ssg08.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_scar20_0', 'SCAR-20', 'SCAR-20', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_scar20.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_g3sg1_0', 'G3SG1', 'G3SG1', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_g3sg1.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_sg556_0', 'SG 553', 'SG 553', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_sg556.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_aug_0', 'AUG', 'AUG', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_aug.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_famas_0', 'FAMAS', 'FAMAS', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_famas.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_galilar_0', 'Galil AR', 'Galil AR', 'rifle', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_galilar.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 插入手枪类饰品
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_glock_0', 'Glock-18', 'Glock-18', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_glock.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_usp_silencer_0', 'USP-S', 'USP-S', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_usp_silencer.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_p250_0', 'P250', 'P250', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_p250.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_p2000_0', 'P2000', 'P2000', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_p2000.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_tec9_0', 'Tec-9', 'Tec-9', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_tec9.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_fiveseven_0', 'Five-SeveN', 'Five-SeveN', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_fiveseven.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_cz75a_0', 'CZ75-Auto', 'CZ75-Auto', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_cz75a.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_deagle_0', 'Desert Eagle', '沙漠之鹰', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_deagle.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_revolver_0', 'R8 Revolver', 'R8 左轮手枪', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_revolver.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_elite_0', 'Dual Berettas', '双持贝瑞塔', 'pistol', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_elite.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 插入微型冲锋枪类
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_mp9_0', 'MP9', 'MP9', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_mp9.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_mac10_0', 'MAC-10', 'MAC-10', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_mac10.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_mp7_0', 'MP7', 'MP7', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_mp7.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_mp5sd_0', 'MP5-SD', 'MP5-SD', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_mp5sd.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_ump45_0', 'UMP-45', 'UMP-45', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_ump45.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_p90_0', 'P90', 'P90', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_p90.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_bizon_0', 'PP-Bizon', 'PP-野牛', 'smg', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_bizon.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 插入霰弹枪类
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_nova_0', 'Nova', 'Nova', 'shotgun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_nova.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_xm1014_0', 'XM1014', 'XM1014', 'shotgun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_xm1014.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_sawedoff_0', 'Sawed-Off', '截短霰弹枪', 'shotgun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_sawedoff.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_mag7_0', 'MAG-7', 'MAG-7', 'shotgun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_mag7.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 插入机枪类
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_m249_0', 'M249', 'M249', 'machinegun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_m249.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_negev_0', 'Negev', 'Negev', 'machinegun', 'consumer', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_negev.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 插入匕首类（基础武器）
INSERT INTO item (item_id, name, name_cn, category, quality, exterior, icon_url, buff_price, is_active, created_at, updated_at) VALUES
('weapon_knife_0', 'Knife', '匕首', 'knife', 'extraordinary', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_knife.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW()),
('weapon_knife_t_0', 'Knife', '匕首', 'knife', 'extraordinary', null, 'https://steamcdn-a.akamaihd.net/apps/730/icons/econ/weapons/base_weapons/weapon_knife_t.a320f13a5bd9617b0f0c4a5e9e0c0b8e6f8c7e5e.png', 0.00, 1, NOW(), NOW());

-- 查看导入结果
SELECT category, COUNT(*) as count FROM item GROUP BY category;
