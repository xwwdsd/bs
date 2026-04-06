export const EXTERIOR_TEXT_MAP = {
  FN: '崭新出厂',
  MW: '略有磨损',
  FT: '久经沙场',
  WW: '破损不堪',
  BS: '战痕累累',
  NoPaint: '无涂装'
}

export const EXTERIOR_RANGE_MAP = {
  FN: [0, 0.07],
  MW: [0.07, 0.15],
  FT: [0.15, 0.38],
  WW: [0.38, 0.45],
  BS: [0.45, 1]
}

const EXTERIOR_ALIAS_MAP = {
  fn: 'FN',
  factory_new: 'FN',
  'factory new': 'FN',
  崭新出厂: 'FN',
  mw: 'MW',
  minimal_wear: 'MW',
  'minimal wear': 'MW',
  略有磨损: 'MW',
  ft: 'FT',
  'field-tested': 'FT',
  'field tested': 'FT',
  久经沙场: 'FT',
  ww: 'WW',
  'well-worn': 'WW',
  'well worn': 'WW',
  破损不堪: 'WW',
  bs: 'BS',
  'battle-scarred': 'BS',
  'battle scarred': 'BS',
  战痕累累: 'BS',
  nopaint: 'NoPaint',
  'no paint': 'NoPaint',
  无涂装: 'NoPaint'
}

const inferExteriorFromText = (...values) => {
  for (const raw of values) {
    const value = String(raw || '').trim()
    if (!value) continue

    const direct = EXTERIOR_ALIAS_MAP[value] || EXTERIOR_ALIAS_MAP[value.toLowerCase()]
    if (direct) {
      return direct
    }

    const lower = value.toLowerCase()
    if (value.includes('崭新出厂') || lower.includes('factory new')) return 'FN'
    if (value.includes('略有磨损') || lower.includes('minimal wear')) return 'MW'
    if (value.includes('久经沙场') || lower.includes('field-tested') || lower.includes('field tested')) return 'FT'
    if (value.includes('破损不堪') || lower.includes('well-worn') || lower.includes('well worn')) return 'WW'
    if (value.includes('战痕累累') || lower.includes('battle-scarred') || lower.includes('battle scarred')) return 'BS'
    if (value.includes('无涂装') || lower.includes('no paint')) return 'NoPaint'
  }

  return ''
}

export const normalizeWearValue = (value) => {
  if (value === null || value === undefined || value === '') return null
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return null
  return numeric > 1 && numeric <= 100 ? numeric / 100 : numeric
}

export const extractWearFromText = (text) => {
  const source = String(text || '')
  if (!source) return null
  const cleaned = source.replace(/<[^>]+>/g, ' ')
  const match = cleaned.match(/(?:磨损|float|paint wear)\s*[:：]?\s*([0-9]*\.?[0-9]+)/i)
  return normalizeWearValue(match?.[1])
}

export const getExteriorFromWear = (paintWear) => {
  const wear = normalizeWearValue(paintWear)
  if (wear === null || wear < 0) return ''
  if (wear <= 0.07) return 'FN'
  if (wear <= 0.15) return 'MW'
  if (wear <= 0.38) return 'FT'
  if (wear <= 0.45) return 'WW'
  if (wear <= 1) return 'BS'
  return ''
}

export const normalizeExterior = (value, ...fallbackTexts) => {
  const normalized = String(value || '').trim()
  if (normalized) {
    const mapped = EXTERIOR_ALIAS_MAP[normalized] || EXTERIOR_ALIAS_MAP[normalized.toLowerCase()]
    if (mapped) {
      return mapped
    }
  }

  const inferred = inferExteriorFromText(normalized, ...fallbackTexts)
  return inferred || normalized
}

export const resolveExterior = (paintWear, exterior, ...fallbackTexts) =>
  getExteriorFromWear(paintWear) || normalizeExterior(exterior, ...fallbackTexts)

export const getExteriorText = (value) => EXTERIOR_TEXT_MAP[value] || value || '未知外观'
