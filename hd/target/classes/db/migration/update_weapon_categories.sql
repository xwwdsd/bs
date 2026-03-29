-- ========================================================
-- 更新饰品分类数据
-- 说明：完善武器分类的子分类数据
-- 时间：2026-03-06
-- ========================================================

USE cs2trade;

-- ========================================================
-- 1. 更新匕首 (Knife) 分类
-- ========================================================

-- 更新蝴蝶刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'butterfly',
    name_cn = '蝴蝶刀'
WHERE name LIKE '%Butterfly Knife%' OR name LIKE '%蝴蝶刀%';

-- 更新 M9 刺刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'm9_bayonet',
    name_cn = 'M9 刺刀'
WHERE name LIKE '%M9 Bayonet%' OR name LIKE '%M9 刺刀%';

-- 更新爪子刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'karambit',
    name_cn = '爪子刀'
WHERE name LIKE '%Karambit%' OR name LIKE '%爪子刀%';

-- 更新 廓尔喀刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'kukri',
    name_cn = '廓尔喀刀'
WHERE name LIKE '%Kukri%' OR name LIKE '%廓尔喀%';

-- 更新 骷髅匕首
UPDATE item SET 
    category = 'knife',
    sub_category = 'skeleton',
    name_cn = '骷髅匕首'
WHERE name LIKE '%Skeleton%' OR name LIKE '%骷髅%';

-- 更新 刺刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'bayonet',
    name_cn = '刺刀'
WHERE name LIKE '%Bayonet%' AND name NOT LIKE '%M9%';

-- 更新 锯齿爪刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'jagged',
    name_cn = '锯齿爪刀'
WHERE name LIKE '%Jagged%' OR name LIKE '%锯齿%';

-- 更新 流浪者匕首
UPDATE item SET 
    category = 'knife',
    sub_category = 'nomad',
    name_cn = '流浪者匕首'
WHERE name LIKE '%Nomad%' OR name LIKE '%流浪者%';

-- 更新 折叠刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'flip',
    name_cn = '折叠刀'
WHERE name LIKE '%Flip Knife%' OR name LIKE '%折叠刀%';

-- 更新 短剑
UPDATE item SET 
    category = 'knife',
    sub_category = 'stiletto',
    name_cn = '短剑'
WHERE name LIKE '%Stiletto%' OR name LIKE '%短剑%';

-- 更新 海豹短刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'navy',
    name_cn = '海豹短刀'
WHERE name LIKE '%Navy%' OR name LIKE '%海豹%';

-- 更新 熊刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'bear',
    name_cn = '熊刀'
WHERE name LIKE '%Bear%' OR name LIKE '%熊刀%';

-- 更新 猎杀者匕首
UPDATE item SET 
    category = 'knife',
    sub_category = 'huntsman',
    name_cn = '猎杀者匕首'
WHERE name LIKE '%Huntsman%' OR name LIKE '%猎杀者%';

-- 更新 系绳匕首
UPDATE item SET 
    category = 'knife',
    sub_category = 'cord',
    name_cn = '系绳匕首'
WHERE name LIKE '%Cord%' OR name LIKE '%系绳%';

-- 更新 求生匕首
UPDATE item SET 
    category = 'knife',
    sub_category = 'survival',
    name_cn = '求生匕首'
WHERE name LIKE '%Survival%' OR name LIKE '%求生%';

-- 更新 弯刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'falchion',
    name_cn = '弯刀'
WHERE name LIKE '%Falchion%' OR name LIKE '%弯刀%';

-- 更新 暗影双匕
UPDATE item SET 
    category = 'knife',
    sub_category = 'shadow',
    name_cn = '暗影双匕'
WHERE name LIKE '%Shadow%' OR name LIKE '%暗影%';

-- 更新 鲍伊猎刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'bowie',
    name_cn = '鲍伊猎刀'
WHERE name LIKE '%Bowie%' OR name LIKE '%鲍伊%';

-- 更新 穿肠刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'gut',
    name_cn = '穿肠刀'
WHERE name LIKE '%Gut%' OR name LIKE '%穿肠%';

-- 更新 折刀
UPDATE item SET 
    category = 'knife',
    sub_category = 'push',
    name_cn = '折刀'
WHERE name LIKE '%Push%' OR name LIKE '%折刀%';

-- ========================================================
-- 2. 更新手套 (Gloves) 分类
-- ========================================================

-- 更新运动手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'sport',
    name_cn = '运动手套'
WHERE name LIKE '%Sport Gloves%' OR name LIKE '%运动手套%';

-- 更新专业手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'specialist',
    name_cn = '专业手套'
WHERE name LIKE '%Specialist Gloves%' OR name LIKE '%专业手套%';

-- 更新摩托手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'moto',
    name_cn = '摩托手套'
WHERE name LIKE '%Moto Gloves%' OR name LIKE '%摩托手套%';

-- 更新驾驶手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'driver',
    name_cn = '驾驶手套'
WHERE name LIKE '%Driver Gloves%' OR name LIKE '%驾驶手套%';

-- 更新手部束带
UPDATE item SET 
    category = 'glove',
    sub_category = 'hand_wraps',
    name_cn = '手部束带'
WHERE name LIKE '%Hand Wraps%' OR name LIKE '%手部束带%';

-- 更新狂牙手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'broken_fang',
    name_cn = '狂牙手套'
WHERE name LIKE '%Broken Fang%' OR name LIKE '%狂牙%';

-- 更新九头蛇手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'hydra',
    name_cn = '九头蛇手套'
WHERE name LIKE '%Hydra%' OR name LIKE '%九头蛇%';

-- 更新血猎手套
UPDATE item SET 
    category = 'glove',
    sub_category = 'bloodhound',
    name_cn = '血猎手套'
WHERE name LIKE '%Bloodhound%' OR name LIKE '%血猎%';

-- ========================================================
-- 3. 更新步枪 (Rifle) 分类
-- ========================================================

-- 更新 AK-47
UPDATE item SET 
    category = 'weapon',
    sub_category = 'ak47',
    name_cn = 'AK-47'
WHERE name LIKE '%AK-47%';

-- 更新 AWP
UPDATE item SET 
    category = 'weapon',
    sub_category = 'awp',
    name_cn = 'AWP'
WHERE name LIKE '%AWP%';

-- 更新 M4A1 消音版
UPDATE item SET 
    category = 'weapon',
    sub_category = 'm4a1_silencer',
    name_cn = 'M4A1 消音版'
WHERE name LIKE '%M4A1-S%';

-- 更新 M4A4
UPDATE item SET 
    category = 'weapon',
    sub_category = 'm4a4',
    name_cn = 'M4A4'
WHERE name LIKE '%M4A4%';

-- 更新 AUG
UPDATE item SET 
    category = 'weapon',
    sub_category = 'aug',
    name_cn = 'AUG'
WHERE name LIKE '%AUG%';

-- 更新 SG 553
UPDATE item SET 
    category = 'weapon',
    sub_category = 'sg553',
    name_cn = 'SG 553'
WHERE name LIKE '%SG 553%' OR name LIKE '%SG553%';

-- 更新 法玛斯
UPDATE item SET 
    category = 'weapon',
    sub_category = 'famas',
    name_cn = '法玛斯'
WHERE name LIKE '%FAMAS%' OR name LIKE '%法玛斯%';

-- 更新 加利尔 AR
UPDATE item SET 
    category = 'weapon',
    sub_category = 'galil',
    name_cn = '加利尔 AR'
WHERE name LIKE '%Galil%' OR name LIKE '%加利尔%';

-- 更新 SSG 08
UPDATE item SET 
    category = 'weapon',
    sub_category = 'ssg08',
    name_cn = 'SSG 08'
WHERE name LIKE '%SSG 08%' OR name LIKE '%SSG08%';

-- 更新 SCAR-20
UPDATE item SET 
    category = 'weapon',
    sub_category = 'scar20',
    name_cn = 'SCAR-20'
WHERE name LIKE '%SCAR-20%' OR name LIKE '%SCAR20%';

-- 更新 G3SG1
UPDATE item SET 
    category = 'weapon',
    sub_category = 'g3sg1',
    name_cn = 'G3SG1'
WHERE name LIKE '%G3SG1%';

-- ========================================================
-- 4. 更新手枪 (Pistol) 分类
-- ========================================================

-- 更新 沙漠之鹰
UPDATE item SET 
    category = 'weapon',
    sub_category = 'deagle',
    name_cn = '沙漠之鹰'
WHERE name LIKE '%Desert Eagle%' OR name LIKE '%沙漠之鹰%';

-- 更新 USP 消音版
UPDATE item SET 
    category = 'weapon',
    sub_category = 'usp_silencer',
    name_cn = 'USP 消音版'
WHERE name LIKE '%USP-S%' OR name LIKE '%USP 消音%';

-- 更新 格洛克 18 型
UPDATE item SET 
    category = 'weapon',
    sub_category = 'glock18',
    name_cn = '格洛克 18 型'
WHERE name LIKE '%Glock-18%' OR name LIKE '%格洛克 18%';

-- 更新 P2000
UPDATE item SET 
    category = 'weapon',
    sub_category = 'p2000',
    name_cn = 'P2000'
WHERE name LIKE '%P2000%';

-- 更新 P250
UPDATE item SET 
    category = 'weapon',
    sub_category = 'p250',
    name_cn = 'P250'
WHERE name LIKE '%P250%';

-- 更新 FN57
UPDATE item SET 
    category = 'weapon',
    sub_category = 'fiveseven',
    name_cn = 'FN57'
WHERE name LIKE '%Five-SeveN%' OR name LIKE '%FN57%';

-- 更新 R8 左轮手枪
UPDATE item SET 
    category = 'weapon',
    sub_category = 'r8',
    name_cn = 'R8 左轮手枪'
WHERE name LIKE '%R8%' OR name LIKE '%R8 左轮%';

-- 更新 Tec-9
UPDATE item SET 
    category = 'weapon',
    sub_category = 'tec9',
    name_cn = 'Tec-9'
WHERE name LIKE '%Tec-9%';

-- 更新 双持贝瑞塔
UPDATE item SET 
    category = 'weapon',
    sub_category = 'elite',
    name_cn = '双持贝瑞塔'
WHERE name LIKE '%Dual Berettas%' OR name LIKE '%双持贝瑞塔%';

-- 更新 CZ75 自动手枪
UPDATE item SET 
    category = 'weapon',
    sub_category = 'cz75',
    name_cn = 'CZ75 自动手枪'
WHERE name LIKE '%CZ75%' OR name LIKE '%CZ75 自动%';

-- 更新 电击枪
UPDATE item SET 
    category = 'weapon',
    sub_category = 'zeus',
    name_cn = '电击枪'
WHERE name LIKE '%Zeus%' OR name LIKE '%电击枪%';

-- ========================================================
-- 5. 更新微型冲锋枪 (SMG) 分类
-- ========================================================

-- 更新 MP9
UPDATE item SET 
    category = 'weapon',
    sub_category = 'mp9',
    name_cn = 'MP9'
WHERE name LIKE '%MP9%';

-- 更新 MAC-10
UPDATE item SET 
    category = 'weapon',
    sub_category = 'mac10',
    name_cn = 'MAC-10'
WHERE name LIKE '%MAC-10%' OR name LIKE '%MAC10%';

-- 更新 UMP-45
UPDATE item SET 
    category = 'weapon',
    sub_category = 'ump45',
    name_cn = 'UMP-45'
WHERE name LIKE '%UMP-45%' OR name LIKE '%UMP45%';

-- 更新 P90
UPDATE item SET 
    category = 'weapon',
    sub_category = 'p90',
    name_cn = 'P90'
WHERE name LIKE '%P90%';

-- 更新 MP7
UPDATE item SET 
    category = 'weapon',
    sub_category = 'mp7',
    name_cn = 'MP7'
WHERE name LIKE '%MP7%';

-- 更新 PP-野牛
UPDATE item SET 
    category = 'weapon',
    sub_category = 'bizon',
    name_cn = 'PP-野牛'
WHERE name LIKE '%PP-Bizon%' OR name LIKE '%野牛%';

-- 更新 MP5-SD
UPDATE item SET 
    category = 'weapon',
    sub_category = 'mp5sd',
    name_cn = 'MP5-SD'
WHERE name LIKE '%MP5-SD%' OR name LIKE '%MP5SD%';

-- ========================================================
-- 输出更新统计
-- ========================================================

SELECT '分类更新完成！' AS status;

SELECT category, sub_category, COUNT(*) as count 
FROM item 
WHERE category IN ('knife', 'glove', 'weapon')
GROUP BY category, sub_category
ORDER BY category, sub_category;
