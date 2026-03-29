/**
 * 饰品分类配置
 * 定义所有武器及其子分类
 */

// 分类配置
export const WEAPON_CATEGORIES = {
  // 1. 匕首分类
  knife: {
    id: 'knife',
    name: '匕首',
    icon: 'knife-icon',
    subCategories: [
      { id: 'butterfly', name: '蝴蝶刀' },
      { id: 'm9_bayonet', name: 'M9 刺刀' },
      { id: 'karambit', name: '爪子刀' },
      { id: 'kukri', name: '廓尔喀刀' },
      { id: 'skeleton', name: '骷髅匕首' },
      { id: 'bayonet', name: '刺刀' },
      { id: 'jagged', name: '锯齿爪刀' },
      { id: 'nomad', name: '流浪者匕首' },
      { id: 'flip', name: '折叠刀' },
      { id: 'stiletto', name: '短剑' },
      { id: 'navy', name: '海豹短刀' },
      { id: 'bear', name: '熊刀' },
      { id: 'huntsman', name: '猎杀者匕首' },
      { id: 'cord', name: '系绳匕首' },
      { id: 'survival', name: '求生匕首' },
      { id: 'falchion', name: '弯刀' },
      { id: 'shadow', name: '暗影双匕' },
      { id: 'bowie', name: '鲍伊猎刀' },
      { id: 'gut', name: '穿肠刀' },
      { id: 'push', name: '折刀' }
    ]
  },
  
  // 2. 手套分类
  glove: {
    id: 'glove',
    name: '手套',
    icon: 'glove-icon',
    subCategories: [
      { id: 'sport', name: '运动手套' },
      { id: 'specialist', name: '专业手套' },
      { id: 'moto', name: '摩托手套' },
      { id: 'driver', name: '驾驶手套' },
      { id: 'hand_wraps', name: '手部束带' },
      { id: 'broken_fang', name: '狂牙手套' },
      { id: 'hydra', name: '九头蛇手套' },
      { id: 'bloodhound', name: '血猎手套' }
    ]
  },
  
  // 3. 步枪分类
  rifle: {
    id: 'rifle',
    name: '步枪',
    icon: 'rifle-icon',
    subCategories: [
      { id: 'ak47', name: 'AK-47' },
      { id: 'awp', name: 'AWP' },
      { id: 'm4a1_silencer', name: 'M4A1 消音版' },
      { id: 'm4a4', name: 'M4A4' },
      { id: 'aug', name: 'AUG' },
      { id: 'sg553', name: 'SG 553' },
      { id: 'famas', name: '法玛斯' },
      { id: 'galil', name: '加利尔 AR' },
      { id: 'ssg08', name: 'SSG 08' },
      { id: 'scar20', name: 'SCAR-20' },
      { id: 'g3sg1', name: 'G3SG1' }
    ]
  },
  
  // 4. 手枪分类
  pistol: {
    id: 'pistol',
    name: '手枪',
    icon: 'pistol-icon',
    subCategories: [
      { id: 'deagle', name: '沙漠之鹰' },
      { id: 'usp_silencer', name: 'USP 消音版' },
      { id: 'glock18', name: '格洛克 18 型' },
      { id: 'p2000', name: 'P2000' },
      { id: 'p250', name: 'P250' },
      { id: 'fiveseven', name: 'FN57' },
      { id: 'r8', name: 'R8 左轮手枪' },
      { id: 'tec9', name: 'Tec-9' },
      { id: 'elite', name: '双持贝瑞塔' },
      { id: 'cz75', name: 'CZ75 自动手枪' },
      { id: 'zeus', name: '电击枪' }
    ]
  },
  
  // 5. 微型冲锋枪分类
  smg: {
    id: 'smg',
    name: '微型冲锋枪',
    icon: 'smg-icon',
    subCategories: [
      { id: 'mp9', name: 'MP9' },
      { id: 'mac10', name: 'MAC-10' },
      { id: 'ump45', name: 'UMP-45' },
      { id: 'p90', name: 'P90' },
      { id: 'mp7', name: 'MP7' },
      { id: 'bizon', name: 'PP-野牛' },
      { id: 'mp5sd', name: 'MP5-SD' }
    ]
  },
  
  // 6. 霰弹枪分类
  shotgun: {
    id: 'shotgun',
    name: '霰弹枪',
    icon: 'shotgun-icon',
    subCategories: [
      { id: 'xm1014', name: 'XM1014' },
      { id: 'mag7', name: 'MAG-7' },
      { id: 'sawedoff', name: '截短霰弹枪' },
      { id: 'nova', name: '新星' }
    ]
  },
  
  // 7. 机枪分类
  machinegun: {
    id: 'machinegun',
    name: '机枪',
    icon: 'machinegun-icon',
    subCategories: [
      { id: 'm249', name: 'M249' },
      { id: 'negev', name: '内格夫' }
    ]
  },
  
  // 8. 印花分类
  sticker: {
    id: 'sticker',
    name: '印花',
    icon: 'sticker-icon',
    subCategories: [
      { id: 'budapest2025', name: '25 布达佩斯' },
      { id: 'austin2025', name: '25 奥斯汀' },
      { id: 'element_hand', name: '元素手作' },
      { id: 'ambush', name: '伏击' },
      { id: 'rio2022', name: '22 里约' },
      { id: 'warroom', name: '作战室' },
      { id: 'surfshop', name: '激流冲浪店' },
      { id: 'poorly_drawn', name: 'Poorly Drawn' },
      { id: 'press', name: '压枪' },
      { id: 'glow', name: '光环' },
      { id: 'berlin2019', name: '19 年柏林' },
      { id: 'katowice2019', name: '19 年卡托' },
      { id: 'boston2018', name: '18 年波士顿' },
      { id: 'cologne2016', name: '16 年科隆' },
      { id: 'cologne2015', name: '15 年科隆' },
      { id: 'cologne2014', name: '14 年科隆' },
      { id: 'sticker_capsule_1', name: '1 号印花' },
      { id: 'sticker_capsule_2', name: '2 号印花' },
      { id: 'fuze', name: 'Enfu 印花' },
      { id: 'bright_china_1', name: '点亮中国 1 号' },
      { id: 'bright_china_2', name: '点亮中国 2 号' },
      { id: 'animal_fable', name: '动物寓言印花' },
      { id: 'slid3', name: 'Slid3 印花' },
      { id: 'candy_skull', name: '糖果脸谱印花' },
      { id: 'pinup', name: '海报女郎印花' },
      { id: 'team_role', name: '团队定位印花' },
      { id: 'community2025', name: '25 年社区印花' },
      { id: 'warhammer2025', name: '25 年战锤 40K' },
      { id: 'paris2023', name: '23 巴黎' },
      { id: 'anniversary10', name: '十周年' },
      { id: 'battlefield2042', name: '战地 2042' },
      { id: 'operation_riptide', name: '激流大行动' },
      { id: 'rmr2020', name: '2020RMR' },
      { id: 'warhammer2020', name: '20 年战锤 40K' },
      { id: 'broken_fang', name: '裂网大行动' },
      { id: 'chicken', name: '小鸡' },
      { id: 'candy_skull_2', name: '糖果脸谱 2' },
      { id: 'shanghai2024', name: '24 上海' },
      { id: 'character_hand', name: '角色手作' },
      { id: 'copenhagen2024', name: '24 哥本哈根' },
      { id: 'antwerp2022', name: '22 安特卫普' },
      { id: 'stockholm2021', name: '21 斯德哥尔摩' },
      { id: 'community2021', name: '2021 社区' },
      { id: 'broken_fang_operation', name: '狂牙大行动' },
      { id: 'half_life_alyx', name: '半衰期:Alyx' },
      { id: 'cs20', name: 'CS20' },
      { id: 'beast', name: '猛兽' },
      { id: 'london2018', name: '18 年伦敦' },
      { id: 'krakow2017', name: '17 年克拉科夫' },
      { id: 'atlanta2017', name: '17 年亚特兰大' },
      { id: 'columbus2016', name: '16 年哥伦布' },
      { id: 'dh2015', name: '15 年 DH' },
      { id: 'katowice2015', name: '15 年卡托维兹' },
      { id: 'dh2014', name: '14 年 DH' },
      { id: 'katowice2014', name: '14 年卡托维兹' },
      { id: 'community2018', name: '18 年社区印花' },
      { id: 'bright_china_sticker_1', name: '1 号社区印花' },
      { id: 'animal_fable_sticker', name: '动物寓言印花' },
      { id: 'slid3_sticker', name: 'Slid3 印花' },
      { id: 'candy_skull_sticker', name: '糖果脸谱印花' },
      { id: 'pinup_sticker', name: '海报女郎印花' },
      { id: 'team_role_sticker', name: '团队定位印花' },
      { id: 'other', name: '其他印花' },
      { id: 'segment', name: '段位印花' }
    ]
  },
  
  // 9. 挂件分类
  charm: {
    id: 'charm',
    name: '挂件',
    icon: 'charm-icon',
    subCategories: [
      { id: 'budapest2025_charm', name: '25 布达佩斯' },
      { id: 'dr_bomb', name: '爆炸博士' },
      { id: 'austin2025', name: '25 奥斯汀' },
      { id: 'sleeve_rare', name: '袖珍武器挂件' },
      { id: 'sticker_board', name: '印花板' },
      { id: 'community_charm', name: '迷链社区' },
      { id: 'chain_charm', name: '迷链挂件' }
    ]
  },
  
  // 10. 探员分类
  agent: {
    id: 'agent',
    name: '探员',
    icon: 'agent-icon',
    subCategories: [
      { id: 'ct', name: '反恐精英' },
      { id: 't', name: '恐怖分子' }
    ]
  },
  
  // 11. 其他分类
  other: {
    id: 'other',
    name: '其他',
    icon: 'other-icon',
    subCategories: [
      { id: 'case', name: '武器箱' },
      { id: 'music_kit', name: '音乐盒' },
      { id: 'tool', name: '工具' },
      { id: 'graffiti', name: '涂鸦' },
      { id: 'pass', name: '通行证' },
      { id: 'collectible', name: '收藏品' },
      { id: 'patch', name: '布章' },
      { id: 'gift', name: '礼物' }
    ]
  }
};

/**
 * 获取主分类列表
 */
export const getMainCategories = () => {
  return Object.values(WEAPON_CATEGORIES);
};

/**
 * 根据主分类 ID 获取子分类
 */
export const getSubCategories = (categoryId) => {
  const category = WEAPON_CATEGORIES[categoryId];
  return category ? category.subCategories : [];
};

/**
 * 分类映射关系（用于后端 category 字段）
 */
export const CATEGORY_MAPPING = {
  // 匕首映射
  butterfly: 'knife',
  m9_bayonet: 'knife',
  karambit: 'knife',
  kukri: 'knife',
  skeleton: 'knife',
  bayonet: 'knife',
  jagged: 'knife',
  nomad: 'knife',
  flip: 'knife',
  stiletto: 'knife',
  navy: 'knife',
  bear: 'knife',
  huntsman: 'knife',
  cord: 'knife',
  survival: 'knife',
  falchion: 'knife',
  shadow: 'knife',
  bowie: 'knife',
  gut: 'knife',
  push: 'knife',
  
  // 手套映射
  sport: 'glove',
  specialist: 'glove',
  moto: 'glove',
  driver: 'glove',
  hand_wraps: 'glove',
  broken_fang: 'glove',
  hydra: 'glove',
  bloodhound: 'glove',
  
  // 步枪、手枪、冲锋枪都映射到 weapon
  ak47: 'weapon',
  awp: 'weapon',
  m4a1_silencer: 'weapon',
  m4a4: 'weapon',
  aug: 'weapon',
  sg553: 'weapon',
  famas: 'weapon',
  galil: 'weapon',
  ssg08: 'weapon',
  scar20: 'weapon',
  g3sg1: 'weapon',
  deagle: 'weapon',
  usp_silencer: 'weapon',
  glock18: 'weapon',
  p2000: 'weapon',
  p250: 'weapon',
  fiveseven: 'weapon',
  r8: 'weapon',
  tec9: 'weapon',
  elite: 'weapon',
  cz75: 'weapon',
  zeus: 'weapon',
  mp9: 'weapon',
  mac10: 'weapon',
  ump45: 'weapon',
  p90: 'weapon',
  mp7: 'weapon',
  bizon: 'weapon',
  mp5sd: 'weapon'
};
