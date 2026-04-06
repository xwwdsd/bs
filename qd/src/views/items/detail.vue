<template>
  <div class="detail-page">
    <SiteHeader active="market" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="detail-shell" v-loading="loading">
      <template v-if="displaySource">
        <nav class="detail-breadcrumb" aria-label="breadcrumb">
          <router-link to="/items" class="breadcrumb-link">{{ TEXT.market }}</router-link>
          <span class="breadcrumb-separator">></span>
          <span class="breadcrumb-current">{{ displayName }}</span>
        </nav>

        <section class="detail-hero">
          <div class="hero-panel">
            <div class="hero-main">
              <div class="media-panel">
                <div class="media-stage">
                  <img :src="displayIcon" :alt="displayName" />
                </div>
              </div>

              <div class="info-panel">
                <div class="info-topbar">
                  <p class="eyebrow">{{ displayCategoryLabel }}</p>
                  <el-button
                    plain
                    class="favorite-action favorite-corner"
                    :class="{ active: isFavorited }"
                    :loading="favoriteLoading"
                    @click="toggleFavorite"
                  >
                    <el-icon>
                      <StarFilled v-if="isFavorited" />
                      <Star v-else />
                    </el-icon>
                    <span>{{ favoriteButtonText }}</span>
                  </el-button>
                </div>
                <h1>{{ displayName }}</h1>

                <div class="base-meta">
                  <span>{{ TEXT.quality }} | {{ displayQualityText }}</span>
                  <span>{{ TEXT.category }} | {{ displayCollectionTypeLabel }}</span>
                  <span>{{ TEXT.type }} | {{ displayCategoryLabel }}</span>
                </div>

                <div class="notice-bar">
                  <span class="notice-badge">{{ TEXT.notice }}</span>
                  <p>{{ displayTradeNotice }}</p>
                </div>

                <div class="trade-row">
                  <div class="price-summary">
                    <div class="price-group">
                      <span>{{ TEXT.referencePrice }}</span>
                      <strong>{{ TEXT.currency }} {{ formatPrice(displayReferencePrice) }}</strong>
                    </div>
                  </div>

                  <div class="hero-actions">
                    <el-button
                      v-if="selectedOrder"
                      type="success"
                      :disabled="selectedOrder.userId === currentUserId"
                      @click="handleBuy(selectedOrder)"
                    >
                      {{ TEXT.buyCurrentOrder }}
                    </el-button>
                    <el-button v-if="userStore.isLoggedIn" @click="goInventory">{{ TEXT.sellMine }}</el-button>
                  </div>
                </div>

              </div>
            </div>

            <div v-if="priceStripEntries.length" class="price-strip">
              <div
                v-for="entry in priceStripEntries"
                :key="entry.id"
                class="price-strip-item"
                :class="{ active: entry.active, accent: entry.kind !== 'exterior', clickable: entry.clickable }"
                @click="handlePriceStripClick(entry)"
              >
                <span>{{ getPriceStripEntryLabel(entry) }}</span>
                <strong v-if="entry.mode === 'switch'">{{ entry.actionText }}</strong>
                <strong v-else>{{ entry.priceText === '--' ? '--' : `${TEXT.currency} ${entry.priceText}` }}</strong>
              </div>
            </div>
          </div>
        </section>

        <ItemSellOrdersSection
          :loading="sellOrdersLoading"
          :orders="relatedOrders"
          :selected-order-id="selectedOrder?.id ?? null"
          :current-user-id="currentUserId"
          :text="TEXT"
          :can-bargain="canBargain"
          :format-date="formatDate"
          :format-price="formatPrice"
          :get-avatar-fallback="getAvatarFallback"
          :get-collection-type-label="getCollectionTypeLabel"
          :get-exterior-text="getExteriorText"
          :get-item-exterior="getItemExterior"
          :get-item-icon="getItemIcon"
          :get-item-name="getItemName"
          :get-order-status="getOrderStatus"
          :get-wear-display="getWearDisplay"
          :get-wear-percent="getWearPercent"
          :has-wear-visual="hasWearVisual"
          @select-order="goToOrder"
          @bargain="openBargainDialog"
          @buy="handleBuy"
        />
      </template>

      <el-empty v-else-if="!loading" :description="TEXT.notFound" />
    </main>

    <ItemBargainDialog
      v-model="showBargainDialog"
      :price="bargainForm.price"
      :text="TEXT"
      :target-name="bargainTargetName"
      :target-price-text="bargainTargetPriceText"
      :target-max-price="bargainTargetMaxPrice"
      :submitting="bargainSubmitting"
      @update:price="bargainForm.price = $event"
      @submit="submitBargain"
    />

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { addFavorite, checkFavorite, removeFavorite } from '@/api/favorite'
import { getItemById, searchItems } from '@/api/item'
import { createBargain } from '@/api/message'
import { getSellOrderDetail, getSellOrdersByItemId } from '@/api/sellOrder'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import ItemBargainDialog from '@/views/items/components/ItemBargainDialog.vue'
import ItemSellOrdersSection from '@/views/items/components/ItemSellOrdersSection.vue'
import request from '@/utils/request'
import {
  normalizeQualityKey,
  resolveItemCategory,
  resolveItemSubCategory,
  resolveItemType
} from '@/utils/itemClassification'
import {
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'
import {
  CATEGORY_TEXT_MAP,
  EXTERIOR_ORDER,
  EXTERIOR_TEXT_MAP,
  LEVEL_LABELS,
  OTHER_SUBCATEGORY_TEXT_MAP,
  QUALITY_TEXT_MAP,
  TEXT,
  TYPE_SWITCH_CODE,
  TYPE_TEXT_MAP,
  WEAR_RANGE_MAP
} from '@/views/items/detailConstants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showLoginModal = ref(false)
const loading = ref(false)
const sellOrdersLoading = ref(false)
const favoriteLoading = ref(false)
const isFavorited = ref(false)
const selectedOrder = ref(null)
const itemDetail = ref(null)
const sellOrders = ref([])
const relatedItems = ref([])
const showBargainDialog = ref(false)
const bargainSubmitting = ref(false)
const bargainTargetOrder = ref(null)
const bargainForm = ref({
  price: 0.01
})

const currentUserId = computed(() => userStore.userInfo?.userId || userStore.userInfo?.id || null)
const displaySource = computed(() => selectedOrder.value || (itemDetail.value ? { item: itemDetail.value } : null))
const displayName = computed(() => getItemName(displaySource.value))
const displayIcon = computed(() => getItemIcon(displaySource.value))
const displayCategoryLabel = computed(() => getItemCategoryLabel(displaySource.value))
const displayCollectionTypeLabel = computed(() => getCollectionTypeLabel(displaySource.value))
const displayQualityText = computed(() => getQualityText(getItemQuality(displaySource.value)))
const displayTradeNotice = computed(() => getTradeNotice(displaySource.value))
const displayReferencePrice = computed(() => getReferencePrice(displaySource.value))
const bargainTargetName = computed(() => getItemName(bargainTargetOrder.value))
const bargainTargetPrice = computed(() => Number(bargainTargetOrder.value?.price || 0))
const bargainTargetPriceText = computed(() => formatPrice(bargainTargetPrice.value))
const bargainTargetMaxPrice = computed(() => {
  const price = bargainTargetPrice.value
  if (!Number.isFinite(price) || price <= 0.01) return undefined
  return Number((price - 0.01).toFixed(2))
})

const buildSource = (source) => {
  if (!source) {
    return { item: null, inventory: null }
  }

  if (source.item || source.inventory) {
    return {
      ...source,
      item: source.item || null,
      inventory: source.inventory || null
    }
  }

  return {
    item: source,
    inventory: null
  }
}

const favoriteTargetItemId = computed(() => {
  const source = displaySource.value
  if (!source) return null
  return source.item?.id || source.itemId || itemDetail.value?.id || selectedOrder.value?.itemId || null
})

const favoriteButtonText = computed(() => (isFavorited.value ? TEXT.favorited : TEXT.favorite))

const formatPrice = (price) => {
  const numeric = Number(price)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : '0.00'
}

const formatDate = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleString('zh-CN')
}

const formatWear = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(6) : '-'
}

const normalizeExterior = (...values) => {
  const exteriorMap = {
    factory_new: 'FN',
    'factory new': 'FN',
    '\u5d2d\u65b0\u51fa\u5382': 'FN',
    minimal_wear: 'MW',
    'minimal wear': 'MW',
    '\u7565\u6709\u78e8\u635f': 'MW',
    'field-tested': 'FT',
    'field tested': 'FT',
    '\u4e45\u7ecf\u6c99\u573a': 'FT',
    'well-worn': 'WW',
    'well worn': 'WW',
    '\u7834\u635f\u4e0d\u582a': 'WW',
    'battle-scarred': 'BS',
    'battle scarred': 'BS',
    '\u6218\u75d5\u7d2f\u7d2f': 'BS',
    nopaint: 'NoPaint',
    '\u65e0\u6d82\u88c5': 'NoPaint'
  }

  for (const raw of values) {
    const normalized = String(raw || '').trim()
    if (!normalized) continue

    const direct = exteriorMap[normalized] || exteriorMap[normalized.toLowerCase()]
    if (direct) {
      return direct
    }

    const lower = normalized.toLowerCase()
    if (lower.includes('factory new')) return 'FN'
    if (lower.includes('minimal wear')) return 'MW'
    if (lower.includes('field-tested') || lower.includes('field tested')) return 'FT'
    if (lower.includes('well-worn') || lower.includes('well worn')) return 'WW'
    if (lower.includes('battle-scarred') || lower.includes('battle scarred')) return 'BS'
    if (normalized.includes('\u5d2d\u65b0\u51fa\u5382')) return 'FN'
    if (normalized.includes('\u7565\u6709\u78e8\u635f')) return 'MW'
    if (normalized.includes('\u4e45\u7ecf\u6c99\u573a')) return 'FT'
    if (normalized.includes('\u7834\u635f\u4e0d\u582a')) return 'WW'
    if (normalized.includes('\u6218\u75d5\u7d2f\u7d2f')) return 'BS'
  }

  return ''
}

const getItemName = (source) => {
  const current = buildSource(source)
  return (
    current.inventory?.name ||
    current.item?.nameCn ||
    current.item?.name ||
    current.nameCn ||
    current.name ||
    TEXT.unknownItem
  )
}

const getItemIcon = (source) => {
  const current = buildSource(source)
  return (
    current.inventory?.iconUrlLarge ||
    current.inventory?.iconUrl ||
    current.item?.iconUrl ||
    '/default-item.png'
  )
}

const getItemCategory = (source) => resolveItemCategory(buildSource(source))
const getItemSubCategory = (source) => resolveItemSubCategory(buildSource(source))
const getItemType = (source) => resolveItemType(buildSource(source)) || 'Normal'

const getItemExterior = (source) => {
  const current = buildSource(source)
  return resolveExteriorCode(
    getWearNumber(source),
    current.inventory?.exterior,
    current.inventory?.wearName,
    current.item?.exterior,
    current.inventory?.name,
    current.item?.nameCn,
    current.item?.name
  )
}

const getItemQuality = (source) => {
  const current = buildSource(source)
  return normalizeQualityKey(
    current.inventory?.rarity ||
      current.inventory?.item?.quality ||
      current.item?.quality ||
      current.item?.rarity ||
      current.inventory?.item?.rarity
  )
}

const getReferencePrice = (source) => {
  const current = buildSource(source)
  const candidates = [
    current.item?.buffPrice,
    current.item?.marketPrice,
    current.item?.lowestPrice,
    current.inventory?.marketPrice,
    current.price
  ]
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0)

  return candidates[0] || 0
}

const getCategoryText = (category, subCategory = '') => {
  if (category === 'other' && OTHER_SUBCATEGORY_TEXT_MAP[subCategory]) {
    return OTHER_SUBCATEGORY_TEXT_MAP[subCategory]
  }

  return CATEGORY_TEXT_MAP[category] || CATEGORY_TEXT_MAP.other
}

const getItemCategoryLabel = (source) => getCategoryText(getItemCategory(source), getItemSubCategory(source))
const getExteriorText = (value) => EXTERIOR_TEXT_MAP[value] || value || TEXT.unknownExterior
const getQualityText = (value) => QUALITY_TEXT_MAP[value] || value || TEXT.unknownQuality
const getCollectionTypeLabel = (source) => TYPE_TEXT_MAP[getItemType(source)] || TEXT.normalType

const getWearNumber = (source) => {
  const current = buildSource(source)
  return normalizeWearValue(current.inventory?.paintWear ?? current.paintWear)
}

const getWearRange = (source) => WEAR_RANGE_MAP[getItemExterior(source)] || null

const getWearDisplay = (source) => {
  const wear = getWearNumber(source)
  if (wear !== null) {
    return formatWear(wear)
  }

  const range = getWearRange(source)
  if (range) {
    return `${range[0].toFixed(2)} - ${range[1].toFixed(2)}`
  }

  return '-'
}

const hasWearVisual = (source) => getWearNumber(source) !== null || !!getWearRange(source)

const getWearPercent = (source) => {
  const wear = getWearNumber(source)
  if (wear !== null) {
    return Math.max(0, Math.min(wear, 1)) * 100
  }

  const range = getWearRange(source)
  if (!range) return 0
  return ((range[0] + range[1]) / 2) * 100
}

const getTradeNotice = (source) => {
  const current = buildSource(source)
  if (current.inventory?.isMarketable === 0 && current.inventory?.marketableReason) {
    return current.inventory.marketableReason
  }

  return TEXT.genericTradeNotice
}

const getOrderStatus = (order) => {
  if (!order) {
    return { tone: 'normal', text: TEXT.saleStatus }
  }

  if (order.inventory?.isMarketable === 0) {
    return { tone: 'warn', text: TEXT.restrictedStatus }
  }

  return { tone: 'normal', text: TEXT.saleStatus }
}

const getAvatarFallback = (user) => String(user?.username || 'U').trim().charAt(0).toUpperCase()
const getSellerLevelLabel = (user) => LEVEL_LABELS[user?.userLevel ?? 0] || '\u7528\u6237'

const stripDecorations = (name) =>
  String(name || '')
    .normalize('NFKC')
    .replace(/StatTrak(?:\u2122)?/gi, ' ')
    .replace(/Souvenir/gi, ' ')
    .replace(/\u7eaa\u5ff5\u54c1/g, ' ')
    .replace(/[\u2605\u2606]/g, ' ')
    .replace(/\((\u5d2d\u65b0\u51fa\u5382|\u7565\u6709\u78e8\u635f|\u4e45\u7ecf\u6c99\u573a|\u7834\u635f\u4e0d\u582a|\u6218\u75d5\u7d2f\u7d2f|Factory New|Minimal Wear|Field-Tested|Well-Worn|Battle-Scarred)\)/gi, ' ')
    .replace(/\uff08(\u5d2d\u65b0\u51fa\u5382|\u7565\u6709\u78e8\u635f|\u4e45\u7ecf\u6c99\u573a|\u7834\u635f\u4e0d\u582a|\u6218\u75d5\u7d2f\u7d2f)\uff09/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const parseComparableSignature = (source) => {
  const cleaned = stripDecorations(getItemName(source))
  const parts = cleaned
    .split('|')
    .map((part) => part.trim())
    .filter(Boolean)

  return {
    raw: cleaned.toLowerCase(),
    weapon: parts[0]?.toLowerCase() || '',
    skin: parts[1]?.toLowerCase() || ''
  }
}

const isComparableSource = (candidate, target) => {
  const current = parseComparableSignature(candidate)
  const base = parseComparableSignature(target)

  if (base.weapon && base.skin) {
    return current.weapon === base.weapon && current.skin === base.skin
  }

  if (base.weapon) {
    return current.weapon === base.weapon || current.raw.includes(base.raw) || base.raw.includes(current.raw)
  }

  return current.raw.includes(base.raw) || base.raw.includes(current.raw)
}

const isSameListingGroup = (candidate, target) => {
  if (!candidate || !target) return false
  if (!isComparableSource(candidate, target)) return false

  const candidateType = getItemType(candidate)
  const targetType = getItemType(target)
  if (candidateType !== targetType) return false

  const targetExterior = getItemExterior(target)
  if (!targetExterior) return true

  return getItemExterior(candidate) === targetExterior
}

const getSearchKeyword = (source) => {
  const signature = parseComparableSignature(source)
  if (signature.weapon && signature.skin) {
    return `${signature.weapon} ${signature.skin}`.trim()
  }

  return stripDecorations(getItemName(source))
}

const getSourceItemId = (source) => {
  const current = buildSource(source)
  return current.item?.id || source?.itemId || (current.inventory ? null : source?.id) || null
}

const getComparableItems = (items, source) => {
  const pool = []
  const seen = new Set()

  const append = (candidate) => {
    if (!candidate || !isComparableSource(candidate, source)) return

    const itemId = getSourceItemId(candidate)
    const key = itemId ? `item:${itemId}` : `name:${getItemName(candidate)}:${getItemType(candidate)}:${getItemExterior(candidate)}`
    if (seen.has(key)) return

    seen.add(key)
    pool.push(candidate)
  }

  items.forEach((item) => append(item))
  append(source)

  return pool
}

const sortVariantCandidates = (left, right) => {
  const leftPrice = getReferencePrice(left)
  const rightPrice = getReferencePrice(right)
  const leftHasPrice = Number.isFinite(leftPrice) && leftPrice > 0
  const rightHasPrice = Number.isFinite(rightPrice) && rightPrice > 0

  if (leftHasPrice !== rightHasPrice) {
    return leftHasPrice ? -1 : 1
  }

  if (leftHasPrice && rightHasPrice && leftPrice !== rightPrice) {
    return leftPrice - rightPrice
  }

  return Number(getSourceItemId(left) || 0) - Number(getSourceItemId(right) || 0)
}

const pickVariantItem = (items, source, { type = '', exterior = '', preferredExterior = '' } = {}) => {
  const candidates = getComparableItems(items, source).filter((item) => {
    if (type && getItemType(item) !== type) return false
    if (exterior && getItemExterior(item) !== exterior) return false
    return true
  })

  if (!candidates.length) {
    return null
  }

  return [...candidates].sort((left, right) => {
    if (preferredExterior) {
      const leftPreferred = getItemExterior(left) === preferredExterior
      const rightPreferred = getItemExterior(right) === preferredExterior
      if (leftPreferred !== rightPreferred) {
        return leftPreferred ? -1 : 1
      }
    }

    return sortVariantCandidates(left, right)
  })[0]
}

const getPriceStripTypeLabel = (type) => {
  if (type === 'StatTrak') return 'StatTrak\u2122'
  return TYPE_TEXT_MAP[type] || type
}

const getPriceStripEntryLabel = (entry) => {
  if (!entry || entry.kind !== 'type') return entry?.label || ''
  return getPriceStripTypeLabel(String(entry.id || '').replace(/^type-/, ''))
}

const buildPriceStripEntries = (items, source) => {
  if (!source) return []

  const entries = []
  const currentType = getItemType(source)
  const currentExterior = getItemExterior(source)
  const comparableItems = getComparableItems(items, source)

  EXTERIOR_ORDER.forEach((exterior) => {
    const prices = comparableItems
      .filter((item) => getItemType(item) === currentType && getItemExterior(item) === exterior)
      .map((item) => getReferencePrice(item))
      .filter((price) => Number.isFinite(price) && price > 0)

    const fallbackPrice = currentExterior === exterior ? getReferencePrice(source) || Number(selectedOrder.value?.price || 0) : null
    const target = pickVariantItem(comparableItems, source, { type: currentType, exterior })
    const isActive = currentExterior === exterior

    entries.push({
      id: `exterior-${exterior}`,
      kind: 'exterior',
      label: getExteriorText(exterior),
      price: prices.length ? Math.min(...prices) : fallbackPrice,
      active: isActive,
      clickable: !!getSourceItemId(target),
      mode: 'price',
      targetItemId: getSourceItemId(target)
    })
  })

  const hasNormalType = comparableItems.some((item) => getItemType(item) === 'Normal')
  const hasStatTrakType = comparableItems.some((item) => getItemType(item) === TYPE_SWITCH_CODE)
  const isCurrentStatTrak = currentType === TYPE_SWITCH_CODE
  const targetType = isCurrentStatTrak ? 'Normal' : TYPE_SWITCH_CODE
  const typeTarget = pickVariantItem(comparableItems, source, {
    type: targetType,
    preferredExterior: currentExterior
  })
  const shouldRenderTypeToggle = currentType === 'Normal' || currentType === TYPE_SWITCH_CODE || hasNormalType || hasStatTrakType

  if (shouldRenderTypeToggle) {
    const type = TYPE_SWITCH_CODE
    const target = typeTarget
    const isActive = isCurrentStatTrak
    entries.push({
      id: `type-${type}`,
      kind: 'type',
      label: getPriceStripTypeLabel(TYPE_SWITCH_CODE),
      price: null,
      active: isActive,
      clickable: !!getSourceItemId(target),
      mode: 'switch',
      actionText: isActive ? '\u5207\u6362\u5230\u666e\u901a' : '\u5207\u6362\u5230',
      targetItemId: getSourceItemId(target)
    })
  }

  return entries
    .filter((entry) => entry.mode === 'switch' || entry.price === null || Number.isFinite(Number(entry.price)))
    .map((entry) => ({
      ...entry,
      priceText: entry.price === null ? '--' : formatPrice(entry.price)
    }))
}

const priceStripEntries = computed(() => buildPriceStripEntries(relatedItems.value, displaySource.value))

const getCheapestOrder = (orders) =>
  [...orders].sort((left, right) => {
    const priceDiff = Number(left?.price || 0) - Number(right?.price || 0)
    if (priceDiff !== 0) return priceDiff
    return new Date(left?.createdAt || 0).getTime() - new Date(right?.createdAt || 0).getTime()
  })[0] || null

const relatedOrders = computed(() => {
  const source = displaySource.value
  if (!source) return []

  return [...sellOrders.value]
    .filter((order) => (selectedOrder.value ? isSameListingGroup(order, selectedOrder.value) : isComparableSource(order, source)))
    .sort((left, right) => {
      if (selectedOrder.value?.id === left.id) return -1
      if (selectedOrder.value?.id === right.id) return 1

      const priceDiff = Number(left.price || 0) - Number(right.price || 0)
      if (priceDiff !== 0) return priceDiff

      return new Date(left.createdAt || 0).getTime() - new Date(right.createdAt || 0).getTime()
    })
})

const loadRelatedItems = async (source) => {
  relatedItems.value = []
  if (!source) return

  try {
    const keyword = getSearchKeyword(source)
    if (!keyword) return

    const result = await searchItems(keyword)
    const list = Array.isArray(result) ? result : []
    relatedItems.value = list.filter((item) => isComparableSource(item, source))
  } catch (error) {
    relatedItems.value = []
  }
}

const fetchSellOrders = async (itemId) => {
  if (!itemId) {
    sellOrders.value = selectedOrder.value ? [selectedOrder.value] : []
    return
  }

  sellOrdersLoading.value = true
  try {
    const orders = await getSellOrdersByItemId(itemId)
    sellOrders.value = Array.isArray(orders) ? orders : []
  } finally {
    sellOrdersLoading.value = false
  }
}

const getSuggestedBargainPrice = (order) => {
  const currentPrice = Number(order?.price || 0)
  if (!Number.isFinite(currentPrice) || currentPrice <= 0.01) return 0.01

  const reduced = currentPrice >= 1 ? currentPrice - 1 : currentPrice * 0.95
  return Math.max(Number(reduced.toFixed(2)), 0.01)
}

const canBargain = (order) => {
  const currentPrice = Number(order?.price || 0)
  return Number.isFinite(currentPrice) && currentPrice > 0.01
}

const refreshFavoriteState = async () => {
  const targetItemId = favoriteTargetItemId.value
  if (!userStore.isLoggedIn || !targetItemId) {
    isFavorited.value = false
    return
  }

  try {
    const result = await checkFavorite(targetItemId, 1)
    isFavorited.value = result === true || result?.favorited === true
  } catch (error) {
    isFavorited.value = false
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  const targetItemId = favoriteTargetItemId.value
  if (!targetItemId || favoriteLoading.value) {
    return
  }

  favoriteLoading.value = true
  try {
    if (isFavorited.value) {
      await removeFavorite(targetItemId, 1)
      isFavorited.value = false
      ElMessage.success(TEXT.favoriteRemoved)
    } else {
      await addFavorite(targetItemId, 1)
      isFavorited.value = true
      ElMessage.success(TEXT.favoriteSuccess)
    }
  } catch (error) {
    ElMessage.error(error?.message || TEXT.favoriteFailed)
  } finally {
    favoriteLoading.value = false
  }
}

const openBargainDialog = (order) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!order || order.userId === currentUserId.value) {
    return
  }
  if (!canBargain(order)) {
    ElMessage.warning(TEXT.lowestPriceBargainDisabled)
    return
  }

  bargainTargetOrder.value = order
  bargainForm.value.price = getSuggestedBargainPrice(order)
  showBargainDialog.value = true
}

const submitBargain = async () => {
  const targetOrder = bargainTargetOrder.value
  const bargainPrice = Number(bargainForm.value.price)
  const currentPrice = Number(targetOrder?.price || 0)

  if (!targetOrder?.id) {
    ElMessage.error(TEXT.targetOrderMissing)
    return
  }
  if (!Number.isFinite(bargainPrice) || bargainPrice <= 0) {
    ElMessage.warning(TEXT.inputValidBargainPrice)
    return
  }
  if (Number.isFinite(currentPrice) && currentPrice > 0 && bargainPrice >= currentPrice) {
    ElMessage.warning(TEXT.bargainPriceTooHigh)
    return
  }

  bargainSubmitting.value = true
  try {
    await createBargain({
      sellOrderId: targetOrder.id,
      bargainPrice: Number(bargainPrice.toFixed(2))
    })
    ElMessage.success(TEXT.bargainSent)
    showBargainDialog.value = false
    bargainTargetOrder.value = null
  } catch (error) {
    ElMessage.error(error?.message || TEXT.sendBargainFailed)
  } finally {
    bargainSubmitting.value = false
  }
}

const goToItem = (itemId) => {
  if (!itemId || route.params.id === String(itemId)) return
  router.push(`/items/${itemId}`)
}

const handlePriceStripClick = async (entry) => {
  if (!entry?.clickable || entry.active) return

  const targetItemId = entry.targetItemId
  if (!targetItemId) return

  try {
    const orders = await getSellOrdersByItemId(targetItemId)
    const targetOrder = getCheapestOrder(Array.isArray(orders) ? orders : [])
    if (targetOrder?.id) {
      goToOrder(targetOrder.id)
      return
    }
  } catch (error) {
    // Fall back to the variant detail page when the order lookup fails.
  }

  goToItem(targetItemId)
}

const fetchItemContext = async () => {
  loading.value = true
  selectedOrder.value = null
  itemDetail.value = null
  sellOrders.value = []
  relatedItems.value = []

  try {
    if (route.params.orderId) {
      const order = await getSellOrderDetail(route.params.orderId)
      selectedOrder.value = order
      itemDetail.value = order?.item || null

      if (order?.itemId) {
        try {
          itemDetail.value = await getItemById(order.itemId)
        } catch (error) {
          itemDetail.value = order?.item || null
        }
      }

      await fetchSellOrders(order?.itemId)

      if (!sellOrders.value.length && order) {
        sellOrders.value = [order]
      }

      await loadRelatedItems(order)
      return
    }

    if (route.params.id) {
      itemDetail.value = await getItemById(route.params.id)
      await fetchSellOrders(route.params.id)
      selectedOrder.value = sellOrders.value[0] || null
      await loadRelatedItems(selectedOrder.value || { item: itemDetail.value })
    }
  } catch (error) {
    selectedOrder.value = null
    itemDetail.value = null
    sellOrders.value = []
    relatedItems.value = []
    ElMessage.error(error?.message || TEXT.fetchDetailFailed)
  } finally {
    loading.value = false
  }
}

const goToOrder = (orderId) => {
  if (!orderId || route.params.orderId === String(orderId)) return
  router.push(`/items/order/${orderId}`)
}

const goInventory = () => router.push('/user/inventory')

const handleBuy = async (order) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!order || order.userId === currentUserId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      TEXT.confirmBuyMessagePrefix + ' ' + TEXT.currency + ' ' + formatPrice(order.price) + ' ' + TEXT.confirmBuyMessageSuffix,
      TEXT.confirmBuyTitle,
      {
        confirmButtonText: TEXT.confirmLabel,
        cancelButtonText: TEXT.cancel,
        type: 'warning'
      }
    )

    await request.post('/v1/order/create', { sellOrderId: order.id })
    ElMessage.success(TEXT.orderCreatedSuccess)
    router.push('/user/orders')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || TEXT.buyFailed)
    }
  }
}

watch(
  () => route.fullPath,
  () => {
    fetchItemContext()
  },
  { immediate: true }
)

watch(
  [favoriteTargetItemId, () => userStore.isLoggedIn],
  () => {
    refreshFavoriteState()
  },
  { immediate: true }
)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.1), transparent 24%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.08), transparent 24%),
    #eef2f7;
}

.detail-shell {
  width: min(71%, 1080px);
  margin: 0 auto;
  padding: 88px 0 36px;
}

.detail-breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  color: #64748b;
  font-size: 15px;
}

.breadcrumb-link {
  color: #334155;
  text-decoration: none;
}

.breadcrumb-link:hover {
  color: #2563eb;
}

.breadcrumb-current {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-hero {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 26px 60px rgba(15, 23, 42, 0.12);
}

.hero-panel {
  background:
    radial-gradient(circle at 18% 45%, rgba(59, 130, 246, 0.16), transparent 26%),
    linear-gradient(180deg, #252a33 0%, #21262e 100%);
  color: #f8fafc;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 18px;
  padding: 18px 18px 14px;
}

.media-panel {
  display: flex;
}

.media-stage {
  width: 100%;
  min-height: 208px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background:
    radial-gradient(circle at center, rgba(59, 130, 246, 0.18), transparent 50%),
    linear-gradient(180deg, rgba(22, 28, 38, 0.98), rgba(31, 41, 55, 0.98));
  overflow: hidden;
}

.media-stage img {
  max-width: 100%;
  max-height: 162px;
  object-fit: contain;
  filter: drop-shadow(0 16px 24px rgba(0, 0, 0, 0.35));
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.eyebrow {
  margin: 0;
  color: #f59e0b;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.info-panel h1 {
  margin: 0;
  color: #ffffff;
  font-size: clamp(26px, 2.4vw, 40px);
  line-height: 1.12;
}

.base-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  color: rgba(226, 232, 240, 0.88);
  font-size: 14px;
}

.notice-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border-left: 3px solid rgba(245, 158, 11, 0.92);
  background: rgba(245, 158, 11, 0.08);
}

.notice-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(245, 158, 11, 0.82);
  color: #fbbf24;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.notice-bar p {
  margin: 0;
  color: rgba(248, 250, 252, 0.92);
  font-size: 13px;
  line-height: 1.55;
}

.trade-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 2px;
}

.price-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.price-group {
  min-width: 146px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.18);
}

.price-group span {
  display: block;
  margin-bottom: 5px;
  color: rgba(203, 213, 225, 0.78);
  font-size: 12px;
}

.price-group strong {
  color: #f8fafc;
  font-size: 22px;
  line-height: 1;
}

.price-group.highlight {
  border-color: rgba(245, 158, 11, 0.34);
  background: rgba(245, 158, 11, 0.1);
}

.price-group.highlight strong {
  color: #fbbf24;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.hero-actions :deep(.el-button) {
  min-width: 116px;
  height: 36px;
  margin-left: 0;
  border-radius: 10px;
  font-weight: 700;
}

.favorite-action {
  border-color: rgba(245, 158, 11, 0.34);
  color: #f8fafc;
  background: rgba(245, 158, 11, 0.08);
}

.favorite-action.active {
  border-color: rgba(245, 158, 11, 0.92);
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.14);
}

.favorite-action :deep(.el-icon) {
  margin-right: 6px;
}

.favorite-corner {
  min-width: 112px;
  height: 36px;
  margin-left: auto;
  border-radius: 10px;
  font-weight: 700;
}

.price-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 18px 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(15, 23, 42, 0.16);
}

.price-strip-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(96, 165, 250, 0.24);
  border-radius: 10px;
  color: #cbd5e1;
  background: rgba(255, 255, 255, 0.04);
}

.price-strip-item.clickable {
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.price-strip-item.clickable:hover {
  transform: translateY(-1px);
  border-color: rgba(96, 165, 250, 0.5);
  background: rgba(96, 165, 250, 0.12);
}

.price-strip-item span {
  font-size: 13px;
}

.price-strip-item strong {
  color: #e2e8f0;
  font-size: 13px;
}

.price-strip-item.active {
  border-color: rgba(96, 165, 250, 0.92);
  background: rgba(96, 165, 250, 0.22);
  color: #ffffff;
}

.price-strip-item.accent {
  border-color: rgba(148, 163, 184, 0.34);
}

@media (max-width: 1280px) {
  .detail-shell {
    width: min(80%, 1020px);
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .trade-row {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .detail-shell {
    width: min(100% - 20px, 720px);
    padding-top: 82px;
  }

  .detail-breadcrumb {
    font-size: 13px;
  }

  .hero-main {
    padding: 18px 18px 14px;
  }

  .info-topbar {
    flex-wrap: wrap;
  }

  .media-stage {
    min-height: 210px;
  }

  .media-stage img {
    max-height: 156px;
  }

  .price-strip {
    padding: 12px 18px 16px;
  }

  .info-panel h1 {
    font-size: 28px;
  }

  .base-meta {
    gap: 10px 16px;
    font-size: 13px;
  }

  .price-summary {
    flex-direction: column;
  }

  .price-group {
    min-width: 0;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }

}
</style>
