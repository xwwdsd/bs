import {
  QUALITY_TEXT_MAP,
  TYPE_TEXT_MAP,
  resolveItemCategory,
  resolveItemQuality,
  resolveItemSubCategory,
  resolveItemType
} from '@/utils/itemClassification'
import {
  EXTERIOR_TEXT_MAP,
  extractWearFromText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

export const DISPLAY_FAMILY = {
  WEAR_BEARING: 'wear-bearing',
  QUALITY_NO_WEAR: 'quality-no-wear',
  CATEGORY_NO_WEAR: 'category-no-wear'
}

export const CATEGORY_TEXT_MAP = {
  rifle: '步枪',
  pistol: '手枪',
  smg: '冲锋枪',
  shotgun: '霰弹枪',
  sniper_rifle: '狙击步枪',
  machinegun: '机枪',
  knife: '刀具',
  glove: '手套',
  sticker: '印花',
  charm: '挂件',
  agent: '探员',
  other: '其他'
}

export const OTHER_SUBCATEGORY_TEXT_MAP = {
  graffiti: '涂鸦',
  case: '武器箱',
  music: '音乐盒',
  tool: '工具',
  pass: '通行证',
  collectible: '收藏品'
}

const WEAR_BEARING_CATEGORIES = new Set([
  'rifle',
  'pistol',
  'smg',
  'shotgun',
  'sniper_rifle',
  'machinegun',
  'knife',
  'glove'
])

const QUALITY_NO_WEAR_CATEGORIES = new Set(['sticker', 'charm', 'agent'])
const QUALITY_NO_WEAR_SUBCATEGORIES = new Set(['music', 'graffiti'])
const CATEGORY_NO_WEAR_SUBCATEGORIES = new Set(['case', 'tool', 'pass', 'collectible'])
const EXTERIOR_CODES = new Set(['FN', 'MW', 'FT', 'WW', 'BS'])

const getCandidateWear = (source) => {
  const candidates = [
    source?.paintWear,
    source?.paintwear,
    source?.wear,
    source?.inventory?.paintWear,
    source?.inventory?.paintwear,
    source?.inventory?.wear,
    source?.item?.paintWear,
    source?.item?.paintwear,
    source?.item?.wear
  ]

  for (const raw of candidates) {
    const normalized = normalizeWearValue(raw)
    if (normalized !== null) {
      return normalized
    }
  }

  return extractWearFromText(source?.description || source?.inventory?.description || source?.item?.description)
}

const getCandidateExterior = (source, wearValue) =>
  resolveExteriorCode(
    wearValue,
    source?.inventory?.exterior || source?.exterior || source?.inventory?.wearName || source?.item?.exterior,
    source?.name,
    source?.marketHashName,
    source?.inventory?.name,
    source?.inventory?.marketHashName,
    source?.item?.nameCn,
    source?.item?.name,
    source?.item?.marketHashName,
    source?.description,
    source?.inventory?.description,
    source?.item?.description
  )

export const getCategoryLabel = (category, subCategory = '') => {
  if (category === 'other' && OTHER_SUBCATEGORY_TEXT_MAP[subCategory]) {
    return OTHER_SUBCATEGORY_TEXT_MAP[subCategory]
  }

  return CATEGORY_TEXT_MAP[category] || CATEGORY_TEXT_MAP.other
}

export const getItemDisplayModel = (source) => {
  const resolvedCategory = resolveItemCategory(source)
  const resolvedSubCategory = resolveItemSubCategory(source)
  const resolvedType = resolveItemType(source) || 'Normal'
  const resolvedQuality = resolveItemQuality(source)
  const rawWearValue = getCandidateWear(source)
  const inferredExterior = getCandidateExterior(source, rawWearValue)
  const hasWearExterior = EXTERIOR_CODES.has(inferredExterior)

  let displayFamily = DISPLAY_FAMILY.CATEGORY_NO_WEAR

  if (WEAR_BEARING_CATEGORIES.has(resolvedCategory) || hasWearExterior) {
    displayFamily = DISPLAY_FAMILY.WEAR_BEARING
  } else if (
    QUALITY_NO_WEAR_CATEGORIES.has(resolvedCategory) ||
    QUALITY_NO_WEAR_SUBCATEGORIES.has(resolvedSubCategory) ||
    resolvedQuality
  ) {
    displayFamily = DISPLAY_FAMILY.QUALITY_NO_WEAR
  } else if (CATEGORY_NO_WEAR_SUBCATEGORIES.has(resolvedSubCategory)) {
    displayFamily = DISPLAY_FAMILY.CATEGORY_NO_WEAR
  }

  const showWearModule = displayFamily === DISPLAY_FAMILY.WEAR_BEARING
  const resolvedExterior = showWearModule ? inferredExterior : 'NoPaint'
  const categoryLabel = getCategoryLabel(resolvedCategory, resolvedSubCategory)
  const qualityText = QUALITY_TEXT_MAP[resolvedQuality] || resolvedQuality || categoryLabel
  const exteriorText = EXTERIOR_TEXT_MAP[resolvedExterior] || resolvedExterior || '未知外观'
  const typeText = TYPE_TEXT_MAP[resolvedType] || resolvedType || TYPE_TEXT_MAP.Normal

  let primaryBadge = {
    text: categoryLabel,
    kind: 'category',
    code: resolvedSubCategory || resolvedCategory || 'other'
  }

  if (displayFamily === DISPLAY_FAMILY.WEAR_BEARING) {
    primaryBadge = {
      text: exteriorText,
      kind: 'exterior',
      code: resolvedExterior || 'UN'
    }
  } else if (displayFamily === DISPLAY_FAMILY.QUALITY_NO_WEAR) {
    primaryBadge = {
      text: qualityText,
      kind: 'quality',
      code: resolvedQuality || 'unknown'
    }
  }

  const secondaryBadge =
    resolvedType && resolvedType !== 'Normal'
      ? {
          text: typeText,
          kind: 'type',
          code: resolvedType
        }
      : null

  let subtitle = categoryLabel
  if (displayFamily === DISPLAY_FAMILY.CATEGORY_NO_WEAR && resolvedType !== 'Normal') {
    subtitle = `${categoryLabel} · ${typeText}`
  }

  return {
    displayFamily,
    resolvedCategory,
    resolvedSubCategory,
    resolvedType,
    resolvedQuality,
    resolvedExterior,
    wearValue: rawWearValue,
    showWearModule,
    filterExterior: showWearModule ? resolvedExterior : 'NoPaint',
    categoryLabel,
    typeText,
    qualityText,
    exteriorText,
    subtitle,
    primaryBadge,
    secondaryBadge
  }
}
