-- 删除步枪类饰品
-- 根据截图中的武器名称删除

DELETE FROM item WHERE name IN (
    'AK-47',
    'M4A4',
    'M4A1-S',
    'AWP',
    'SSG 08',
    'SCAR-20',
    'G3SG1',
    'SG 553',
    'AUG',
    'FAMAS',
    'Galil AR'
);

-- 或者根据子分类删除
-- DELETE FROM item WHERE sub_category = 'rifle';
