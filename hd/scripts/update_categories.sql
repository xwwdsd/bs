-- ========================================================
-- 更新饰品分类数据 - 简化版
-- 说明：完善武器分类的子分类数据
-- 时间：2026-03-06
-- ========================================================

USE cs2trade;

-- 1. 匕首分类
UPDATE item SET category = 'knife', sub_category = 'butterfly' WHERE name LIKE '%Butterfly Knife%';
UPDATE item SET category = 'knife', sub_category = 'm9_bayonet' WHERE name LIKE '%M9 Bayonet%';
UPDATE item SET category = 'knife', sub_category = 'karambit' WHERE name LIKE '%Karambit%';
UPDATE item SET category = 'knife', sub_category = 'kukri' WHERE name LIKE '%Kukri%';
UPDATE item SET category = 'knife', sub_category = 'skeleton' WHERE name LIKE '%Skeleton%';
UPDATE item SET category = 'knife', sub_category = 'bayonet' WHERE name LIKE '%Bayonet%' AND name NOT LIKE '%M9%';
UPDATE item SET category = 'knife', sub_category = 'jagged' WHERE name LIKE '%Jagged%';
UPDATE item SET category = 'knife', sub_category = 'nomad' WHERE name LIKE '%Nomad%';
UPDATE item SET category = 'knife', sub_category = 'flip' WHERE name LIKE '%Flip Knife%';
UPDATE item SET category = 'knife', sub_category = 'stiletto' WHERE name LIKE '%Stiletto%';
UPDATE item SET category = 'knife', sub_category = 'navy' WHERE name LIKE '%Navy%';
UPDATE item SET category = 'knife', sub_category = 'bear' WHERE name LIKE '%Bear%';
UPDATE item SET category = 'knife', sub_category = 'huntsman' WHERE name LIKE '%Huntsman%';
UPDATE item SET category = 'knife', sub_category = 'cord' WHERE name LIKE '%Cord%';
UPDATE item SET category = 'knife', sub_category = 'survival' WHERE name LIKE '%Survival%';
UPDATE item SET category = 'knife', sub_category = 'falchion' WHERE name LIKE '%Falchion%';
UPDATE item SET category = 'knife', sub_category = 'shadow' WHERE name LIKE '%Shadow%';
UPDATE item SET category = 'knife', sub_category = 'bowie' WHERE name LIKE '%Bowie%';
UPDATE item SET category = 'knife', sub_category = 'gut' WHERE name LIKE '%Gut%';
UPDATE item SET category = 'knife', sub_category = 'push' WHERE name LIKE '%Push%';

-- 2. 手套分类
UPDATE item SET category = 'glove', sub_category = 'sport' WHERE name LIKE '%Sport Gloves%';
UPDATE item SET category = 'glove', sub_category = 'specialist' WHERE name LIKE '%Specialist Gloves%';
UPDATE item SET category = 'glove', sub_category = 'moto' WHERE name LIKE '%Moto Gloves%';
UPDATE item SET category = 'glove', sub_category = 'driver' WHERE name LIKE '%Driver Gloves%';
UPDATE item SET category = 'glove', sub_category = 'hand_wraps' WHERE name LIKE '%Hand Wraps%';
UPDATE item SET category = 'glove', sub_category = 'broken_fang' WHERE name LIKE '%Broken Fang%';
UPDATE item SET category = 'glove', sub_category = 'hydra' WHERE name LIKE '%Hydra%';
UPDATE item SET category = 'glove', sub_category = 'bloodhound' WHERE name LIKE '%Bloodhound%';

-- 3. 步枪分类
UPDATE item SET category = 'weapon', sub_category = 'ak47' WHERE name LIKE '%AK-47%';
UPDATE item SET category = 'weapon', sub_category = 'awp' WHERE name LIKE '%AWP%';
UPDATE item SET category = 'weapon', sub_category = 'm4a1_silencer' WHERE name LIKE '%M4A1-S%';
UPDATE item SET category = 'weapon', sub_category = 'm4a4' WHERE name LIKE '%M4A4%';
UPDATE item SET category = 'weapon', sub_category = 'aug' WHERE name LIKE '%AUG%';
UPDATE item SET category = 'weapon', sub_category = 'sg553' WHERE name LIKE '%SG 553%';
UPDATE item SET category = 'weapon', sub_category = 'famas' WHERE name LIKE '%FAMAS%';
UPDATE item SET category = 'weapon', sub_category = 'galil' WHERE name LIKE '%Galil%';
UPDATE item SET category = 'weapon', sub_category = 'ssg08' WHERE name LIKE '%SSG 08%';
UPDATE item SET category = 'weapon', sub_category = 'scar20' WHERE name LIKE '%SCAR-20%';
UPDATE item SET category = 'weapon', sub_category = 'g3sg1' WHERE name LIKE '%G3SG1%';

-- 4. 手枪分类
UPDATE item SET category = 'weapon', sub_category = 'deagle' WHERE name LIKE '%Desert Eagle%';
UPDATE item SET category = 'weapon', sub_category = 'usp_silencer' WHERE name LIKE '%USP-S%';
UPDATE item SET category = 'weapon', sub_category = 'glock18' WHERE name LIKE '%Glock-18%';
UPDATE item SET category = 'weapon', sub_category = 'p2000' WHERE name LIKE '%P2000%';
UPDATE item SET category = 'weapon', sub_category = 'p250' WHERE name LIKE '%P250%';
UPDATE item SET category = 'weapon', sub_category = 'fiveseven' WHERE name LIKE '%Five-SeveN%';
UPDATE item SET category = 'weapon', sub_category = 'r8' WHERE name LIKE '%R8%';
UPDATE item SET category = 'weapon', sub_category = 'tec9' WHERE name LIKE '%Tec-9%';
UPDATE item SET category = 'weapon', sub_category = 'elite' WHERE name LIKE '%Dual Berettas%';
UPDATE item SET category = 'weapon', sub_category = 'cz75' WHERE name LIKE '%CZ75%';
UPDATE item SET category = 'weapon', sub_category = 'zeus' WHERE name LIKE '%Zeus%';

-- 5. 微型冲锋枪分类
UPDATE item SET category = 'weapon', sub_category = 'mp9' WHERE name LIKE '%MP9%';
UPDATE item SET category = 'weapon', sub_category = 'mac10' WHERE name LIKE '%MAC-10%';
UPDATE item SET category = 'weapon', sub_category = 'ump45' WHERE name LIKE '%UMP-45%';
UPDATE item SET category = 'weapon', sub_category = 'p90' WHERE name LIKE '%P90%';
UPDATE item SET category = 'weapon', sub_category = 'mp7' WHERE name LIKE '%MP7%';
UPDATE item SET category = 'weapon', sub_category = 'bizon' WHERE name LIKE '%PP-Bizon%';
UPDATE item SET category = 'weapon', sub_category = 'mp5sd' WHERE name LIKE '%MP5-SD%';

-- 6. 霰弹枪分类
UPDATE item SET category = 'weapon', sub_category = 'xm1014' WHERE name LIKE '%XM1014%';
UPDATE item SET category = 'weapon', sub_category = 'mag7' WHERE name LIKE '%MAG-7%';
UPDATE item SET category = 'weapon', sub_category = 'sawedoff' WHERE name LIKE '%Sawed-Off%';
UPDATE item SET category = 'weapon', sub_category = 'nova' WHERE name LIKE '%Nova%';

-- 7. 机枪分类
UPDATE item SET category = 'weapon', sub_category = 'm249' WHERE name LIKE '%M249%';
UPDATE item SET category = 'weapon', sub_category = 'negev' WHERE name LIKE '%Negev%';

-- 8. 印花分类
UPDATE item SET category = 'sticker', sub_category = 'budapest2025' WHERE name LIKE '%Budapest 2025%';
UPDATE item SET category = 'sticker', sub_category = 'austin2025' WHERE name LIKE '%Austin 2025%';
UPDATE item SET category = 'sticker', sub_category = 'paris2023' WHERE name LIKE '%Paris 2023%';
UPDATE item SET category = 'sticker', sub_category = 'rio2022' WHERE name LIKE '%Rio 2022%';
UPDATE item SET category = 'sticker', sub_category = 'antwerp2022' WHERE name LIKE '%Antwerp 2022%';
UPDATE item SET category = 'sticker', sub_category = 'stockholm2021' WHERE name LIKE '%Stockholm 2021%';
UPDATE item SET category = 'sticker', sub_category = 'berlin2019' WHERE name LIKE '%Berlin 2019%';
UPDATE item SET category = 'sticker', sub_category = 'katowice2019' WHERE name LIKE '%Katowice 2019%';
UPDATE item SET category = 'sticker', sub_category = 'boston2018' WHERE name LIKE '%Boston 2018%';
UPDATE item SET category = 'sticker', sub_category = 'london2018' WHERE name LIKE '%London 2018%';
UPDATE item SET category = 'sticker', sub_category = 'krakow2017' WHERE name LIKE '%Krakow 2017%';
UPDATE item SET category = 'sticker', sub_category = 'atlanta2017' WHERE name LIKE '%Atlanta 2017%';
UPDATE item SET category = 'sticker', sub_category = 'cologne2016' WHERE name LIKE '%Cologne 2016%';
UPDATE item SET category = 'sticker', sub_category = 'columbus2016' WHERE name LIKE '%Columbus 2016%';
UPDATE item SET category = 'sticker', sub_category = 'katowice2015' WHERE name LIKE '%Katowice 2015%';
UPDATE item SET category = 'sticker', sub_category = 'cologne2015' WHERE name LIKE '%Cologne 2015%';
UPDATE item SET category = 'sticker', sub_category = 'katowice2014' WHERE name LIKE '%Katowice 2014%';
UPDATE item SET category = 'sticker', sub_category = 'cologne2014' WHERE name LIKE '%Cologne 2014%';

-- 9. 挂件分类
UPDATE item SET category = 'charm', sub_category = 'budapest2025_charm' WHERE name LIKE '%Budapest 2025%' AND name LIKE '%Charm%';
UPDATE item SET category = 'charm', sub_category = 'austin2025' WHERE name LIKE '%Austin 2025%' AND name LIKE '%Charm%';
UPDATE item SET category = 'charm', sub_category = 'dr_bomb' WHERE name LIKE '%Dr. Bomb%';
UPDATE item SET category = 'charm', sub_category = 'sleeve_rare' WHERE name LIKE '%Sleeve%';

-- 10. 探员分类
UPDATE item SET category = 'agent', sub_category = 'ct' WHERE name LIKE '%CT%' OR name LIKE '%反恐精英%' OR name LIKE '%SAS%' OR name LIKE '%GIGN%' OR name LIKE '%GSG-9%' OR name LIKE '%Spetsnaz%';
UPDATE item SET category = 'agent', sub_category = 't' WHERE name LIKE '%T%' OR name LIKE '%恐怖分子%' OR name LIKE '%Phoenix%' OR name LIKE '%Separatist%' OR name LIKE '%Elite Crew%' OR name LIKE '%Professional%';

-- 查询统计
SELECT '分类更新完成！' AS status;

SELECT 
    category AS '分类', 
    sub_category AS '子分类', 
    COUNT(*) AS '数量'
FROM item 
WHERE category IN ('knife', 'glove', 'weapon')
GROUP BY category, sub_category
ORDER BY category, sub_category;
