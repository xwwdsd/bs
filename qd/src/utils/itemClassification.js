const TOP_LEVEL_CATEGORY_MAP = {
  knife: 'knife',
  glove: 'glove',
  rifle: 'rifle',
  pistol: 'pistol',
  smg: 'smg',
  shotgun: 'shotgun',
  sniper_rifle: 'sniper_rifle',
  sniper: 'sniper_rifle',
  machinegun: 'machinegun',
  machine_gun: 'machinegun',
  sticker: 'sticker',
  charm: 'charm',
  agent: 'agent',
  other: 'other',
  graffiti: 'other',
  case: 'other',
  music: 'other',
  tool: 'other',
  pass: 'other',
  collectible: 'other'
}

const EXPLICIT_SUBCATEGORY_MAP = {
  graffiti: 'graffiti',
  case: 'case',
  music: 'music',
  tool: 'tool',
  pass: 'pass',
  collectible: 'collectible'
}

const WEAPON_RULES = [
  { category: 'knife', subCategory: 'butterfly', aliases: ['butterfly knife', '\u8774\u8776\u5200'] },
  { category: 'knife', subCategory: 'm9', aliases: ['m9 bayonet', 'm9 \u523a\u5200'] },
  { category: 'knife', subCategory: 'claw', aliases: ['karambit', '\u722a\u5b50\u5200', '\u722a\u5200'] },
  { category: 'knife', subCategory: 'karambit', aliases: ['kukri knife', '\u5ed3\u5c14\u5580\u5200', 'kukri'] },
  { category: 'knife', subCategory: 'skeleton', aliases: ['skeleton knife', '\u9ab7\u9ac5\u5315\u9996'] },
  { category: 'knife', subCategory: 'bayonet', aliases: ['bayonet', '\u523a\u5200'] },
  { category: 'knife', subCategory: 'serrated', aliases: ['talon knife', '\u952f\u9f7f\u722a\u5200'] },
  { category: 'knife', subCategory: 'nomad', aliases: ['nomad knife', '\u6d41\u6d6a\u8005\u5315\u9996'] },
  { category: 'knife', subCategory: 'flip', aliases: ['flip knife', '\u6298\u53e0\u5200'] },
  { category: 'knife', subCategory: 'short', aliases: ['stiletto knife', '\u77ed\u5251'] },
  { category: 'knife', subCategory: 'gut', aliases: ['gut knife', '\u6d77\u8c79\u77ed\u5200'] },
  { category: 'knife', subCategory: 'bear', aliases: ['ursus knife', '\u718a\u5200'] },
  { category: 'knife', subCategory: 'huntsman', aliases: ['huntsman knife', '\u730e\u6740\u8005\u5315\u9996'] },
  { category: 'knife', subCategory: 'cord', aliases: ['paracord knife', '\u7cfb\u7ef3\u5315\u9996'] },
  { category: 'knife', subCategory: 'survival', aliases: ['survival knife', '\u6c42\u751f\u5315\u9996'] },
  { category: 'knife', subCategory: 'bowie', aliases: ['bowie knife', '\u9c8d\u4f0a\u730e\u5200', '\u9c8d\u4f0a\u5200'] },
  { category: 'knife', subCategory: 'fangs', aliases: ['shadow daggers', '\u6697\u5f71\u53cc\u5315'] },
  { category: 'knife', subCategory: 'gut-knife', aliases: ['\u7a7f\u80a0\u5200'] },
  { category: 'knife', subCategory: 'navaja', aliases: ['navaja knife', '\u6298\u5200'] },
  { category: 'glove', subCategory: 'sport', aliases: ['sport gloves', '\u8fd0\u52a8\u624b\u5957'] },
  { category: 'glove', subCategory: 'specialist', aliases: ['specialist gloves', '\u4e13\u4e1a\u624b\u5957'] },
  { category: 'glove', subCategory: 'moto', aliases: ['moto gloves', '\u6469\u6258\u624b\u5957'] },
  { category: 'glove', subCategory: 'driver', aliases: ['driver gloves', '\u9a7e\u9a76\u624b\u5957'] },
  { category: 'glove', subCategory: 'hand', aliases: ['hand wraps', '\u624b\u90e8\u675f\u5e26'] },
  { category: 'glove', subCategory: 'hydra', aliases: ['hydra gloves', '\u4e5d\u5934\u86c7\u624b\u5957'] },
  { category: 'glove', subCategory: 'bloodhound', aliases: ['bloodhound gloves', '\u8840\u730e\u624b\u5957'] },
  { category: 'sniper_rifle', subCategory: 'awp', aliases: ['awp'] },
  { category: 'sniper_rifle', subCategory: 'ssg08', aliases: ['ssg 08', 'ssg08'] },
  { category: 'sniper_rifle', subCategory: 'scar20', aliases: ['scar 20', 'scar20', 'scar-20'] },
  { category: 'sniper_rifle', subCategory: 'g3sg1', aliases: ['g3sg1'] },
  { category: 'rifle', subCategory: 'ak47', aliases: ['ak 47', 'ak-47'] },
  { category: 'rifle', subCategory: 'm4a4', aliases: ['m4a4'] },
  { category: 'rifle', subCategory: 'm4a1s', aliases: ['m4a1-s', 'm4a1 s', 'm4a1\u6d88\u97f3\u7248'] },
  { category: 'rifle', subCategory: 'aug', aliases: ['aug'] },
  { category: 'rifle', subCategory: 'famas', aliases: ['famas', '\u6cd5\u739b\u65af'] },
  { category: 'rifle', subCategory: 'galil', aliases: ['galil ar', 'galil', '\u52a0\u5229\u5c14 ar', '\u52a0\u5229\u5c14ar'] },
  { category: 'rifle', subCategory: 'sg553', aliases: ['sg 553', 'sg553'] },
  { category: 'pistol', subCategory: 'deagle', aliases: ['desert eagle', '\u6c99\u6f20\u4e4b\u9e70'] },
  { category: 'pistol', subCategory: 'glock', aliases: ['glock 18', 'glock-18', '\u683c\u6d1b\u514b18', '\u683c\u6d1b\u514b 18'] },
  { category: 'pistol', subCategory: 'p250', aliases: ['p250'] },
  { category: 'pistol', subCategory: 'usp', aliases: ['usp-s', 'usp s', 'usp\u6d88\u97f3\u7248', 'usp \u6d88\u97f3\u7248'] },
  { category: 'pistol', subCategory: 'p2000', aliases: ['p2000'] },
  { category: 'pistol', subCategory: 'fn57', aliases: ['five seven', 'five-seven', 'fiveseven', 'fn57', 'fn 57'] },
  { category: 'pistol', subCategory: 'r8', aliases: ['r8 revolver', 'r8 \u5de6\u8f6e\u624b\u67aa'] },
  { category: 'pistol', subCategory: 'tec9', aliases: ['tec 9', 'tec-9'] },
  { category: 'pistol', subCategory: 'elite', aliases: ['dual berettas', '\u53cc\u6301\u8d1d\u745e\u5854'] },
  { category: 'pistol', subCategory: 'cz75a', aliases: ['cz75 auto', 'cz75-auto', 'cz75a', 'cz75 \u81ea\u52a8\u624b\u67aa'] },
  { category: 'smg', subCategory: 'mp9', aliases: ['mp9'] },
  { category: 'smg', subCategory: 'mac10', aliases: ['mac 10', 'mac-10'] },
  { category: 'smg', subCategory: 'ump45', aliases: ['ump 45', 'ump-45'] },
  { category: 'smg', subCategory: 'p90', aliases: ['p90'] },
  { category: 'smg', subCategory: 'mp7', aliases: ['mp7'] },
  { category: 'smg', subCategory: 'bizon', aliases: ['pp bizon', 'pp-bizon', '\u91ce\u725b\u51b2\u950b\u67aa', '\u91ce\u725b'] },
  { category: 'smg', subCategory: 'mp5sd', aliases: ['mp5 sd', 'mp5-sd', 'mp5sd'] },
  { category: 'shotgun', subCategory: 'mag7', aliases: ['mag 7', 'mag-7'] },
  { category: 'shotgun', subCategory: 'nova', aliases: ['nova', '\u65b0\u661f'] },
  { category: 'shotgun', subCategory: 'xm1014', aliases: ['xm1014'] },
  { category: 'shotgun', subCategory: 'sawedoff', aliases: ['sawed off', 'sawed-off', '\u622a\u77ed\u9730\u5f39\u67aa'] },
  { category: 'machinegun', subCategory: 'm249', aliases: ['m249'] },
  { category: 'machinegun', subCategory: 'negev', aliases: ['negev', '\u5185\u683c\u592b'] }
]

const TYPE_RULES = [
  { type: 'StarStatTrak', aliases: ['★ stattrak', 'stattrak ★', 'star stattrak', 'star_stattrak', 'starstattrak'] },
  { type: 'StatTrak', aliases: ['stattrak'] },
  { type: 'Souvenir', aliases: ['souvenir', '\u7eaa\u5ff5\u54c1'] },
  { type: 'Star', aliases: ['\u2605', 'star'] }
]

export const QUALITY_TEXT_MAP = {
  contraband: '\u8fdd\u7981',
  covert: '\u9690\u79d8',
  classified: '\u4fdd\u5bc6',
  restricted: '\u53d7\u9650',
  'mil-spec': '\u519b\u89c4\u7ea7',
  industrial: '\u5de5\u4e1a\u7ea7',
  consumer: '\u6d88\u8d39\u7ea7',
  extraordinary: '\u975e\u51e1',
  exotic: '\u5353\u8d8a',
  remarkable: '\u5947\u5f02',
  'high-grade': '\u9ad8\u7ea7',
  'normal-grade': '\u666e\u901a\u7ea7',
  'agent-grade': '\u63a2\u5458\u54c1\u8d28'
}

export const TYPE_TEXT_MAP = {
  Normal: '\u666e\u901a',
  Souvenir: '\u7eaa\u5ff5\u54c1',
  StatTrak: 'StatTrak\u2122',
  Star: '\u2605',
  StarStatTrak: '\u2605 StatTrak\u2122'
}

const SPECIAL_OTHER_SUBCATEGORY_TEXTS = {
  case: ['\u6b66\u5668\u7bb1', '\u6b66\u5668\u7bb1\u5b50', 'case', 'capsule', '\u80f6\u56ca'],
  music: ['\u97f3\u4e50\u76d2', 'music kit'],
  tool: ['\u6539\u540d\u5361', '\u94a5\u5319', '\u5de5\u5177', 'name tag', 'key'],
  pass: ['\u901a\u884c\u8bc1', 'viewer pass'],
  collectible: ['\u786c\u5e01', '\u7eaa\u5ff5\u5e01', '\u5fbd\u7ae0', 'coin', 'medal', 'diamond coin', '\u94bb\u77f3\u5e01']
}

const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFKC')
    .replace(/StatTrak(?:™)?/gi, ' stattrak ')
    .replace(/Souvenir/gi, ' souvenir ')
    .replace(/Five[- ]?SeveN/gi, ' five seven ')
    .replace(/USP-S/gi, ' usp-s ')
    .replace(/M4A1-S/gi, ' m4a1-s ')
    .replace(/PP-Bizon/gi, ' pp-bizon ')
    .replace(/CZ75-Auto/gi, ' cz75-auto ')
    .replace(/R8 Revolver/gi, ' r8 revolver ')
    .replace(/[|/\\()[\]{}【】<>：:，,。!?！？'"`~]/g, ' ')
    .replace(/[-_]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()

const normalizeKey = (value) =>
  String(value ?? '')
    .normalize('NFKC')
    .trim()
    .toLowerCase()
    .replace(/[-\s]+/g, '_')

const normalizeExplicitType = (value) => {
  const rawText = String(value ?? '').normalize('NFKC').trim()
  if (!rawText) {
    return ''
  }

  const normalizedText = normalizeText(rawText)
  const normalizedKey = normalizeKey(rawText)
  const hasStar = rawText.includes('\u2605') || normalizedText.includes('★') || /\bstar\b/.test(normalizedText)
  const hasStatTrak = normalizedText.includes('stattrak') || normalizedKey.includes('stattrak')

  if (hasStar && hasStatTrak) return 'StarStatTrak'
  if (hasStatTrak) return 'StatTrak'
  if (normalizedText.includes('souvenir') || normalizedKey === 'souvenir') return 'Souvenir'
  if (hasStar) return 'Star'
  if (['normal', 'ordinary', 'default', 'none'].includes(normalizedKey)) return 'Normal'

  return ''
}

const getNameLikeCandidateTexts = (source) => {
  if (!source) return []
  if (typeof source === 'string') return [source]

  return [
    source.name,
    source.nameCn,
    source.marketHashName,
    source.market_name,
    source.market_hash_name,
    source.inventory?.name,
    source.inventory?.nameCn,
    source.inventory?.marketHashName,
    source.inventory?.market_name,
    source.inventory?.item?.nameCn,
    source.inventory?.item?.name,
    source.item?.nameCn,
    source.item?.name,
    source.item?.marketHashName
  ].filter(Boolean)
}

const getCandidateTexts = (source, { includeDescriptions = true } = {}) => {
  const texts = getNameLikeCandidateTexts(source)

  if (!source || typeof source === 'string' || !includeDescriptions) {
    return texts
  }

  return texts.concat(
    [
      source.description,
      source.inventory?.description,
      source.item?.description
    ].filter(Boolean)
  )
}

const hasAnyAlias = (text, aliases) => aliases.some((alias) => text.includes(normalizeText(alias)))

const inferSpecialCategory = (texts) => {
  const normalizedTexts = texts.map(normalizeText)
  const combined = normalizedTexts.join(' ')
  const normalizedRawTexts = texts.map((text) => String(text ?? '').normalize('NFKC'))

  const isSticker = normalizedRawTexts.some((text) => /^\s*(\u5370\u82b1|sticker)\s*\|/i.test(text))
  if (isSticker) {
    const isMajor = [
      'major',
      '\u9526\u6807\u8d5b',
      'major championship',
      'shanghai',
      '\u4e0a\u6d77',
      'paris',
      '\u5df4\u9ece',
      'katowice',
      'cologne',
      'copenhagen',
      'rio',
      'stockholm',
      'antwerp',
      'blast'
    ].some((token) => combined.includes(normalizeText(token)))

    if (isMajor) {
      return { category: 'sticker', subCategory: 'major' }
    }

    const isCommunity = ['community', '\u793e\u533a'].some((token) => combined.includes(normalizeText(token)))
    return { category: 'sticker', subCategory: isCommunity ? 'community' : 'other' }
  }

  const isGraffiti = normalizedRawTexts.some((text) =>
    /^\s*(\u5c01\u88c5\u7684\u6d82\u9e26|\u6d82\u9e26|sealed graffiti|graffiti)\s*\|/i.test(text)
  )
  if (isGraffiti) {
    return { category: 'other', subCategory: 'graffiti' }
  }

  if (['\u6302\u4ef6', '\u540a\u5760', 'charm'].some((token) => combined.includes(normalizeText(token)))) {
    const subCategory = ['2024', '\u4e0a\u6d77\u9526\u6807\u8d5b', 'shanghai'].some((token) =>
      combined.includes(normalizeText(token))
    )
      ? '2024'
      : 'other'
    return { category: 'charm', subCategory }
  }

  for (const [subCategory, aliases] of Object.entries(SPECIAL_OTHER_SUBCATEGORY_TEXTS)) {
    if (hasAnyAlias(combined, aliases)) {
      return { category: 'other', subCategory }
    }
  }

  if (
    ['\u63a2\u5458', 'agent', '\u7279\u8b66', '\u53cd\u6050\u7cbe\u82f1', 'fbi', 'swat', 'sas'].some((token) =>
      combined.includes(normalizeText(token))
    )
  ) {
    return { category: 'agent', subCategory: 'ct' }
  }

  if (['\u6050\u6016\u5206\u5b50', '\u4e13\u4e1a\u4eba\u58eb', '\u51e4\u51f0\u6218\u58eb'].some((token) => combined.includes(normalizeText(token)))) {
    return { category: 'agent', subCategory: 't' }
  }

  return null
}

const inferWeaponCategory = (texts) => {
  for (const text of texts.map(normalizeText)) {
    if (!text) continue

    for (const rule of WEAPON_RULES) {
      if (hasAnyAlias(text, rule.aliases)) {
        return {
          category: rule.category,
          subCategory: rule.subCategory
        }
      }
    }
  }

  return null
}

const normalizeExplicitCategory = (value) => {
  const normalized = normalizeKey(value)
  return TOP_LEVEL_CATEGORY_MAP[normalized] || ''
}

const getExplicitCategory = (source) => {
  const candidates = [
    source?.item?.category,
    source?.inventory?.item?.category,
    source?.inventory?.category,
    source?.category
  ]

  for (const value of candidates) {
    const normalized = normalizeExplicitCategory(value)
    if (normalized && normalized !== 'weapon') {
      return normalized
    }
  }

  return ''
}

const getExplicitSubCategory = (source) => {
  const candidates = [
    source?.item?.subCategory,
    source?.item?.sub_category,
    source?.inventory?.item?.subCategory,
    source?.inventory?.item?.sub_category,
    source?.inventory?.subCategory,
    source?.inventory?.sub_category,
    source?.subCategory,
    source?.sub_category
  ]

  for (const value of candidates) {
    const normalized = normalizeKey(value)
    if (normalized) {
      return normalized
    }
  }

  const explicitCategory = normalizeKey(
    source?.item?.category || source?.inventory?.item?.category || source?.inventory?.category || source?.category
  )

  return EXPLICIT_SUBCATEGORY_MAP[explicitCategory] || ''
}

const getExplicitType = (source) => {
  const candidates = [
    source?.type,
    source?.inventory?.type,
    source?.item?.type,
    source?.inventory?.item?.type
  ]

  for (const value of candidates) {
    const normalized = normalizeExplicitType(value)
    if (normalized) {
      return normalized
    }
  }

  return ''
}

const inferClassification = (source) => {
  const texts = getNameLikeCandidateTexts(source)
  return inferSpecialCategory(texts) || inferWeaponCategory(texts)
}

export const resolveItemCategory = (source) => {
  const inferred = inferClassification(source)
  if (inferred?.category) {
    return inferred.category
  }

  const explicit = getExplicitCategory(source)
  return explicit || 'other'
}

export const resolveItemSubCategory = (source) => {
  const inferred = inferClassification(source)
  if (inferred?.subCategory) {
    return inferred.subCategory
  }

  return getExplicitSubCategory(source)
}

export const resolveItemType = (source) => {
  const explicit = getExplicitType(source)
  if (explicit) {
    return explicit
  }

  const texts = getNameLikeCandidateTexts(source)
  const combined = texts.map(normalizeText).join(' ')
  const original = texts.join(' ')
  const hasStar = original.includes('\u2605') || combined.includes('★') || /\bstar\b/.test(combined)
  const hasStatTrak = combined.includes('stattrak')

  if (!combined && !original) {
    return 'Normal'
  }

  if (hasStar && hasStatTrak) {
    return 'StarStatTrak'
  }

  for (const rule of TYPE_RULES) {
    if (hasAnyAlias(combined, rule.aliases)) {
      return rule.type
    }
  }

  return 'Normal'
}

export const normalizeQualityKey = (value) => {
  const normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[_\s-]/g, '')

  const map = {
    ancient: 'contraband',
    contraband: 'contraband',
    legendary: 'covert',
    covert: 'covert',
    mythical: 'classified',
    classified: 'classified',
    rare: 'restricted',
    restricted: 'restricted',
    uncommon: 'mil-spec',
    milspec: 'mil-spec',
    'mil-spec': 'mil-spec',
    milspecgrade: 'mil-spec',
    industrial: 'industrial',
    industrialgrade: 'industrial',
    consumer: 'consumer',
    consumergrade: 'consumer',
    common: 'consumer',
    immortal: 'extraordinary',
    extraordinary: 'extraordinary',
    exotic: 'exotic',
    remarkable: 'remarkable',
    highgrade: 'high-grade',
    superior: 'high-grade',
    basegrade: 'normal-grade',
    normalgrade: 'normal-grade',
    distinguished: 'agent-grade',
    exceptional: 'agent-grade',
    master: 'agent-grade',
    agentgrade: 'agent-grade'
  }

  return map[normalized] || normalized
}

const WEAPON_PAINT_QUALITY_KEYS = new Set(['consumer', 'industrial', 'mil-spec', 'restricted', 'classified', 'covert'])
const STRICT_NO_WEAR_QUALITY_CATEGORIES = new Set(['sticker', 'charm', 'agent'])
const STRICT_NO_WEAR_QUALITY_SUBCATEGORIES = new Set(['music', 'graffiti'])

const isQualityCompatibleWithCategory = (quality, category, subCategory) => {
  if (!quality) {
    return false
  }

  if (
    (STRICT_NO_WEAR_QUALITY_CATEGORIES.has(category) || STRICT_NO_WEAR_QUALITY_SUBCATEGORIES.has(subCategory)) &&
    WEAPON_PAINT_QUALITY_KEYS.has(quality)
  ) {
    return false
  }

  return true
}

const QUALITY_INFERENCE_RULES = [
  { key: 'contraband', aliases: ['违禁', 'contraband', 'ancient'] },
  { key: 'covert', aliases: ['隐秘', 'covert', 'legendary'] },
  { key: 'classified', aliases: ['保密', 'classified', 'mythical'] },
  { key: 'restricted', aliases: ['受限', 'restricted', 'rare'] },
  { key: 'mil-spec', aliases: ['军规级', 'mil-spec', 'mil spec', 'uncommon'] },
  { key: 'industrial', aliases: ['工业级', 'industrial', 'industrial grade'] },
  { key: 'consumer', aliases: ['消费级', 'consumer', 'consumer grade'] },
  { key: 'extraordinary', aliases: ['非凡', 'extraordinary', 'immortal'] },
  { key: 'exotic', aliases: ['卓越', 'exotic'] },
  { key: 'remarkable', aliases: ['奇异', 'remarkable'] },
  { key: 'high-grade', aliases: ['高级', 'high grade', 'high-grade', 'superior'] },
  { key: 'normal-grade', aliases: ['普通级', 'base grade', 'base-grade', 'normal grade', 'normal-grade'] },
  { key: 'agent-grade', aliases: ['探员品质', 'distinguished', 'exceptional', 'master', 'agent grade'] }
]

export const inferQualityFromTexts = (...values) => {
  const combined = values
    .flatMap((value) => (Array.isArray(value) ? value : [value]))
    .filter(Boolean)
    .map((value) => normalizeText(value))
    .join(' ')

  if (!combined) {
    return ''
  }

  for (const rule of QUALITY_INFERENCE_RULES) {
    if (rule.aliases.some((alias) => combined.includes(normalizeText(alias)))) {
      return rule.key
    }
  }

  return ''
}

export const resolveItemQuality = (source) => {
  const category = resolveItemCategory(source)
  const subCategory = resolveItemSubCategory(source)
  const metadataCandidates = [
    source?.item?.rarity,
    source?.item?.quality,
    source?.inventory?.item?.rarity,
    source?.inventory?.item?.quality,
    source?.rarity,
    source?.quality,
    source?.inventory?.rarity,
    source?.inventory?.quality
  ]

  for (const value of metadataCandidates) {
    const normalized = normalizeQualityKey(value)
    if (isQualityCompatibleWithCategory(normalized, category, subCategory)) {
      return normalized
    }
  }

  const inferred = inferQualityFromTexts(...getNameLikeCandidateTexts(source))
  if (isQualityCompatibleWithCategory(inferred, category, subCategory)) {
    return inferred
  }

  return ''
}

export const shouldUseNoPaintExterior = (source) => {
  const category = resolveItemCategory(source)
  const subCategory = resolveItemSubCategory(source)

  if (['sticker', 'charm', 'agent'].includes(category)) {
    return true
  }

  return ['graffiti', 'music', 'case', 'tool', 'pass', 'collectible'].includes(subCategory)
}
