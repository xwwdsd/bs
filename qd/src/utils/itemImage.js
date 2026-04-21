const DEFAULT_ITEM_IMAGE = '/default-item.svg'
const STEAM_IMAGE_BASE_URL = 'https://steamcommunity-a.akamaihd.net/economy/image/'

const ABSOLUTE_URL_PATTERN = /^(?:https?:)?\/\//i
const LOCAL_ASSET_PATTERN = /^(?:\/|\.\/|\.\.\/)/
const INLINE_ASSET_PATTERN = /^(?:data:|blob:)/i
const STEAM_APP_ICON_PATTERN = /\/steamcommunity\/public\/images\/apps\/730\//i

export const normalizeItemImageUrl = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''

  if (STEAM_APP_ICON_PATTERN.test(raw)) {
    return ''
  }

  if (ABSOLUTE_URL_PATTERN.test(raw) || INLINE_ASSET_PATTERN.test(raw) || LOCAL_ASSET_PATTERN.test(raw)) {
    return raw
  }

  if (raw.startsWith('economy/image/')) {
    return `${STEAM_IMAGE_BASE_URL}${raw.replace(/^economy\/image\//, '')}`
  }

  return `${STEAM_IMAGE_BASE_URL}${raw.replace(/^\/+/, '')}`
}

export const resolveItemImageUrl = (...candidates) => {
  for (const candidate of candidates) {
    const normalized = normalizeItemImageUrl(candidate)
    if (normalized) {
      return normalized
    }
  }

  return DEFAULT_ITEM_IMAGE
}

export { DEFAULT_ITEM_IMAGE }
