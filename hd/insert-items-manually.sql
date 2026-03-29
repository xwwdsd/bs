-- 手动添加缺失的 CS2 饰品到 item 表
-- 注意：这些物品的 icon_url 使用的是 Steam CDN 的示例 URL，实际使用时需要替换为真实的图片 URL

-- AK-47 | 红线
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('AK-47 | Redline', 'AK-47 | Redline', 'AK-47 | 红线', 'rifle', 'classified', 'classified', 'FT', 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 100.00, 1, NOW(), NOW());

-- AWP | 黑色魅影
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('AWP | Atheris', 'AWP | Atheris', 'AWP | 黑色魅影', 'sniper_rifle', 'restricted', 'restricted', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 200.00, 1, NOW(), NOW());

-- M4A1 消音版 | 幽独
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('M4A1-S | Printstream', 'M4A1-S | Printstream', 'M4A1 消音版 | 幽独', 'rifle', 'covert', 'covert', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/i0CoZ81Ui0m-9KwlBY1L_18myuGuq1wfhWSaZgMttyVfPaERSR0Wqmu7LAocGIGz3UqlXOLrxM-vMGmW8VNxu5Dx60noTyL8ypexwjFS4_ega6F_H-OcDW-vzOFjvvVoRiegqhBzsmyWpYPwJiPTcFIoXpslROVftRK5kYblN7zq5VbX3YtMmH_8ji5MvX1qtu1XWPFxrvLJz1aW589-peo', 111.00, 1, NOW(), NOW());

-- MP9（StatTrak™） | 红宝石毒镖
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('MP9 | Ruby Poison Dart', 'MP9 | Ruby Poison Dart', 'MP9（StatTrak™） | 红宝石毒镖', 'smg', 'restricted', 'restricted', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/i0CoZ81Ui0m-9KwlBY1L_18myuGuq1wfhWSaZgMttyVfPaERSR0Wqmu7LAocGIGz3UqlXOLrxM-vMGmW8VNxu5Dx60noTyL8js_f_C9k4uL3V6pkNOKcCWKe_uJ_t-l9AXzhkEsm527Xy9r_JynEP1IiWJZ3FOYP4xTqmtznNO-34VCKiYJExSjgznQeVzj6siQ', 122.00, 1, NOW(), NOW());

-- 截短霰弹枪（纪念品） | 旱地之花
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('Sawed-Off | Wasteland Princess', 'Sawed-Off | Wasteland Princess', '截短霰弹枪（纪念品） | 旱地之花', 'shotgun', 'mil-spec', 'mil-spec', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/i0CoZ81Ui0m-9KwlBY1L_18myuGuq1wfhWSaZgMttyVfPaERSR0Wqmu7LAocGIGz3UqlXOLrxM-vMGmW8VNxu5Dx60noTyLin4Hl-S1d6c2mcZtpJeOWHHOvw-J5v-xWQiy3nAgq_W7Uytf6J3uQblUkDJZwQLMC4US-ktDkYuuwsQSP3YlByCv52ChL6yd1o7FVAWi_E24', 50.00, 1, NOW(), NOW());

-- 新星 | 沼泽草
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('Nova | Bloomstick', 'Nova | Bloomstick', '新星 | 沼泽草', 'shotgun', 'mil-spec', 'mil-spec', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 30.00, 1, NOW(), NOW());

-- P90 | 摧枯拉朽
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('P90 | Chopper King', 'P90 | Chopper King', 'P90 | 摧枯拉朽', 'smg', 'restricted', 'restricted', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 80.00, 1, NOW(), NOW());

-- 沙漠之鹰（StatTrak™） | 印花集
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('Desert Eagle | Printstream', 'Desert Eagle | Printstream', '沙漠之鹰 | 印花集', 'pistol', 'covert', 'covert', 'FN', 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 300.00, 1, NOW(), NOW());

-- 音乐盒 | Valve，反恐精英：全球攻势
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('Music Kit | Valve, CS:GO', 'Music Kit | Valve, CS:GO', '音乐盒 | Valve，反恐精英：全球攻势', 'music', 'rare', 'rare', NULL, 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 50.00, 1, NOW(), NOW());

-- 音乐盒 | 完美世界，诶嘿
INSERT INTO item (item_id, name, name_cn, category, quality, rarity, exterior, icon_url, buff_price, is_active, created_at, updated_at)
VALUES 
('Music Kit | Perfect World, Ai Hei', 'Music Kit | Perfect World, Ai Hei', '音乐盒 | 完美世界，诶嘿', 'music', 'rare', 'rare', NULL, 'https://steamcommunity-a.akamaihd.net/economy/image/-9a81dlWLwJ2UUGcVs_nsVtzdOEdtWtpKGPK4', 50.00, 1, NOW(), NOW());
