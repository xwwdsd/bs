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
                  <span>{{ TEXT.category }} | {{ displayCategoryLabel }}</span>
                  <span>{{ TEXT.type }} | {{ displayTypeText }}</span>
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
                      <em v-if="displayReferenceSourceText" class="reference-price-source">{{ displayReferenceSourceText }}</em>
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
                    <el-button type="danger" plain @click="openBuyOrderDialog">
                      {{ TEXT.requestBuy }}
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

        <section class="order-book-section">
          <div class="order-book-header">
            <div class="order-book-tabs">
              <button
                type="button"
                class="order-book-tab"
                :class="{ active: activeOrderTab === 'sell' }"
                @click="activeOrderTab = 'sell'"
              >
                {{ TEXT.sellTab }}
              </button>
              <button
                type="button"
                class="order-book-tab"
                :class="{ active: activeOrderTab === 'buy' }"
                @click="activeOrderTab = 'buy'"
              >
                {{ TEXT.buyTab }}
              </button>
              <button
                v-for="entry in marketPanelTabs"
                :key="entry.value"
                type="button"
                class="order-book-tab"
                :class="{ active: activeOrderTab === entry.value }"
                @click="selectMarketPanel(entry.value)"
              >
                {{ entry.label }}
              </button>
            </div>
            <span v-if="!isInsightTabActive" class="order-book-count">
              {{ activeOrderTab === 'sell' ? relatedOrders.length : buyOrders.length }}
              {{ activeOrderTab === 'sell' ? TEXT.saleRecordSuffix : TEXT.buyRecordSuffix }}
            </span>
          </div>

          <ItemSellOrdersSection
            v-if="activeOrderTab === 'sell'"
            class="orders-panel sell-orders-panel"
            hide-header
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

          <section v-else-if="activeOrderTab === 'buy'" class="orders-panel buy-orders-section">
            <div class="buy-orders-layout">
              <div class="buy-orders-table" v-loading="buyOrdersLoading">
                <el-empty v-if="!buyOrders.length && !buyOrdersLoading" :description="TEXT.noActiveSale" />
                <div v-for="order in buyOrders" v-else :key="order.id" class="buy-order-row">
                  <div class="buy-order-item">
                    <img :src="getBuyOrderItemIcon(order)" :alt="getItemName(order)" />
                    <div>
                      <strong>{{ getItemName(order) }}</strong>
                      <span>{{ TEXT.remainingQuantity }} {{ getRemainingQuantity(order) }}</span>
                    </div>
                  </div>
                  <div class="buy-order-buyer">
                    <span>{{ TEXT.buyer }}</span>
                    <strong>{{ order.user?.username || TEXT.unknownSeller }}</strong>
                  </div>
                  <div class="buy-order-price">
                    <span>{{ TEXT.price }}</span>
                    <strong>{{ TEXT.currency }} {{ formatPrice(order.price) }}</strong>
                  </div>
                  <el-button
                    type="primary"
                    plain
                    :disabled="order.userId === currentUserId"
                    @click="openRespondBuyDialog(order)"
                  >
                    {{ TEXT.sellToBuyer }}
                  </el-button>
                </div>
              </div>
            </div>
          </section>

          <section v-else-if="activeOrderTab === 'recommend'" class="orders-panel recommend-tab-section">
            <ItemRecommendationPanel
              class="detail-recommend-panel"
              :current-item-id="currentDisplayItemId"
            />
          </section>

          <section v-else-if="isMarketPanelActive" class="orders-panel market-tab-section">
            <ItemMarketPanel
              class="detail-market-panel"
              :item-id="currentDisplayItemId"
              :active-view="currentMarketPanel"
              :fallback-price="currentSellPrice"
              :highest-buy-price="highestBuyPrice"
            />
          </section>
        </section>
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

    <el-dialog v-model="showBuyOrderDialog" :title="TEXT.createBuyOrderTitle" width="420px">
      <el-form label-position="top">
        <el-form-item :label="TEXT.item">
          <div class="dialog-static-value">{{ displayName }}</div>
        </el-form-item>
        <el-form-item :label="TEXT.buyOrderPriceLabel">
          <el-input-number v-model="buyForm.price" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="TEXT.buyOrderQuantityLabel">
          <el-input-number v-model="buyForm.quantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBuyOrderDialog = false">{{ TEXT.cancel }}</el-button>
        <el-button type="primary" :loading="buyCreating" @click="submitBuyOrder">{{ TEXT.confirmBuyOrder }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRespondBuyDialog" :title="TEXT.respondBuyTitle" width="480px">
      <el-form label-position="top">
        <el-form-item :label="TEXT.buyOrderPriceLabel">
          <div class="dialog-static-value">{{ TEXT.currency }} {{ formatPrice(respondTargetOrder?.price) }}</div>
        </el-form-item>
        <el-form-item :label="TEXT.chooseInventoryForBuy">
          <el-select
            v-model="respondForm.inventoryId"
            :placeholder="TEXT.chooseInventoryPlaceholder"
            :loading="respondInventoryLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in matchingInventory"
              :key="item.id"
              :label="item.name || item.item?.nameCn || item.item?.name"
              :value="item.id"
            />
          </el-select>
          <p v-if="!matchingInventory.length && !respondInventoryLoading" class="form-help">{{ TEXT.noMatchingInventory }}</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRespondBuyDialog = false">{{ TEXT.cancel }}</el-button>
        <el-button
          type="primary"
          :loading="respondSubmitting"
          :disabled="!respondForm.inventoryId"
          @click="submitRespondBuyOrder"
        >
          {{ TEXT.confirmRespondBuy }}
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { createBuyOrder, getBuyOrdersByItemId, respondBuyOrder } from '@/api/buyOrder'
import { addFavorite, checkFavorite, removeFavorite } from '@/api/favorite'
import { getMarketableInventory } from '@/api/inventory'
import { getItemById, searchItems } from '@/api/item'
import { createBargain } from '@/api/message'
import { getSellOrderDetail, getSellOrdersByItemId } from '@/api/sellOrder'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import ItemBargainDialog from '@/views/items/components/ItemBargainDialog.vue'
import ItemMarketPanel from '@/views/items/components/ItemMarketPanel.vue'
import ItemRecommendationPanel from '@/views/items/components/ItemRecommendationPanel.vue'
import ItemSellOrdersSection from '@/views/items/components/ItemSellOrdersSection.vue'
import request from '@/utils/request'
import { resolveItemImageUrl } from '@/utils/itemImage'
import {
  resolveItemQuality,
  resolveItemCategory,
  resolveItemSubCategory,
  resolveItemType
} from '@/utils/itemClassification'
import {
  normalizeWearValue
} from '@/utils/itemExterior'
import { getItemDisplayModel } from '@/utils/itemDisplay'
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
const buyOrdersLoading = ref(false)
const favoriteLoading = ref(false)
const isFavorited = ref(false)
const selectedOrder = ref(null)
const itemDetail = ref(null)
const sellOrders = ref([])
const buyOrders = ref([])
const activeOrderTab = ref('sell')
const activeMarketPanel = ref('trend')
const relatedItems = ref([])
const showBargainDialog = ref(false)
const bargainSubmitting = ref(false)
const bargainTargetOrder = ref(null)
const bargainForm = ref({
  price: 0.01
})
const showBuyOrderDialog = ref(false)
const buyCreating = ref(false)
const buyForm = ref({
  price: 0.01,
  quantity: 1
})
const showRespondBuyDialog = ref(false)
const respondInventoryLoading = ref(false)
const respondSubmitting = ref(false)
const respondTargetOrder = ref(null)
const respondInventory = ref([])
const respondForm = ref({
  inventoryId: null
})
const marketPanelTabs = [
  { value: 'trend', label: '价格走势' },
  { value: 'trades', label: '成交记录' },
  { value: 'pricing', label: '定价建议' },
  { value: 'recommend', label: '推荐' }
]

const marketPanelValues = ['trend', 'trades', 'pricing']
const insightTabValues = marketPanelTabs.map((tab) => tab.value)
const isInsightTabActive = computed(() => insightTabValues.includes(activeOrderTab.value))
const isMarketPanelActive = computed(() => marketPanelValues.includes(activeOrderTab.value))
const currentMarketPanel = computed(() => (
  marketPanelValues.includes(activeOrderTab.value) ? activeOrderTab.value : activeMarketPanel.value
))
const selectMarketPanel = (panel) => {
  activeOrderTab.value = panel
  if (marketPanelValues.includes(panel)) {
    activeMarketPanel.value = panel
  }
}

const currentUserId = computed(() => userStore.userInfo?.userId || userStore.userInfo?.id || null)
const displaySource = computed(() => selectedOrder.value || (itemDetail.value ? { item: itemDetail.value } : null))
const displayModel = computed(() => getItemDisplayModel(displaySource.value))
const displayName = computed(() => getItemName(displaySource.value))
const displayIcon = computed(() => getItemIcon(displaySource.value))
const displayCategoryLabel = computed(() => displayModel.value.categoryLabel)
const displayTypeText = computed(() => displayModel.value.typeText)
const displayQualityText = computed(() => displayModel.value.qualityText)
const displayTradeNotice = computed(() => getTradeNotice(displaySource.value))
const displayReferencePrice = computed(() => getReferencePrice(displaySource.value))
const displayReferenceSourceText = computed(() => getReferencePriceSourceText(displaySource.value))
const currentSellPrice = computed(() => {
  const selectedPrice = Number(selectedOrder.value?.price)
  if (Number.isFinite(selectedPrice) && selectedPrice > 0) return selectedPrice

  const cheapestPrice = Number(getCheapestOrder(relatedOrders.value)?.price)
  if (Number.isFinite(cheapestPrice) && cheapestPrice > 0) return cheapestPrice

  return displayReferencePrice.value
})
const highestBuyPrice = computed(() => {
  const prices = buyOrders.value
    .map((order) => Number(order?.price))
    .filter((price) => Number.isFinite(price) && price > 0)
  return prices.length ? Math.max(...prices) : 0
})
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

const isUnknownItem = (item) => {
  if (!item) return false
  return String(item.itemId || '').toLowerCase() === 'unknown' ||
    String(item.name || '').toLowerCase() === 'unknown item' ||
    item.nameCn === TEXT.unknownItem
}

const normalizeResolvedItemId = (value) => {
  const normalized = String(value ?? '').trim()
  if (!normalized || normalized.toLowerCase() === 'unknown') return null
  return value
}

const getItemEntityId = (item) => {
  if (!item || isUnknownItem(item)) return null
  return normalizeResolvedItemId(item.id)
}

const getSourceItemId = (source, { allowRawFallback = true } = {}) => {
  if (!source) return null

  const current = buildSource(source)
  if (isUnknownItem(current.item) || isUnknownItem(current.inventory?.item)) return null

  const itemId = getItemEntityId(current.item) || getItemEntityId(current.inventory?.item)
  if (itemId) return itemId

  if (!allowRawFallback) return null
  return normalizeResolvedItemId(source.itemId ?? (current.inventory ? null : source.id))
}

const resolveCurrentItemId = () => (
  getSourceItemId({ item: itemDetail.value }, { allowRawFallback: false }) ||
  getSourceItemId(displaySource.value)
)

const currentDisplayItemId = computed(resolveCurrentItemId)
const favoriteTargetItemId = computed(resolveCurrentItemId)
const matchingInventory = computed(() => {
  const targetItemId = getSourceItemId(respondTargetOrder.value) || currentDisplayItemId.value
  return respondInventory.value.filter((item) => Number(item?.itemId) === Number(targetItemId) && Number(item?.status ?? 0) === 0)
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
  return resolveItemImageUrl(
    current.inventory?.iconUrlLarge,
    current.inventory?.iconUrl,
    current.item?.iconUrl
  )
}

const getBuyOrderItemIcon = (order) => resolveItemImageUrl(order?.item?.iconUrl, displayIcon.value)

const getItemCategory = (source) => resolveItemCategory(buildSource(source))
const getItemSubCategory = (source) => resolveItemSubCategory(buildSource(source))
const getSourceDisplayModel = (source) => getItemDisplayModel(buildSource(source))
const getItemType = (source) => getSourceDisplayModel(source).resolvedType || 'Normal'
const getItemExterior = (source) => getSourceDisplayModel(source).filterExterior
const getItemQuality = (source) => getSourceDisplayModel(source).resolvedQuality

const getReferencePriceInfo = (source) => {
  const current = buildSource(source)

  const candidate = [
    { price: current.item?.steamReferencePrice, source: 'steam' },
    { price: current.inventory?.item?.steamReferencePrice, source: 'steam' },
    { price: current.item?.buffPrice, source: 'buff' },
    { price: current.inventory?.item?.buffPrice, source: 'buff' },
    { price: current.item?.marketPrice, source: 'local' },
    { price: current.item?.lowestPrice, source: 'local' },
    { price: current.inventory?.marketPrice, source: 'local' }
  ]
    .map((entry) => ({
      price: Number(entry.price),
      source: entry.source
    }))
    .find((entry) => Number.isFinite(entry.price) && entry.price > 0)

  return candidate || { price: 0, source: '' }
}

const getReferencePrice = (source) => getReferencePriceInfo(source).price
const getReferencePriceSource = (source) => getReferencePriceInfo(source).source
const getReferencePriceSourceText = (source) => {
  const referenceSource = getReferencePriceSource(source)
  if (referenceSource === 'steam') return 'Steam参考价'
  if (referenceSource === 'buff') return 'Buff回退'
  if (referenceSource === 'local') return '站内参考'
  return ''
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
const getCollectionTypeLabel = (source) => getSourceDisplayModel(source).typeText || TEXT.normalType

const getWearNumber = (source) => {
  const current = getSourceDisplayModel(source)
  return normalizeWearValue(current.wearValue)
}

const getWearRange = (source) => {
  if (!getSourceDisplayModel(source).showWearModule) return null
  return WEAR_RANGE_MAP[getItemExterior(source)] || null
}

const getWearDisplay = (source) => {
  if (!getSourceDisplayModel(source).showWearModule) {
    return '-'
  }

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

const hasWearVisual = (source) =>
  getSourceDisplayModel(source).showWearModule && (getWearNumber(source) !== null || !!getWearRange(source))

const getWearPercent = (source) => {
  if (!getSourceDisplayModel(source).showWearModule) {
    return 0
  }

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

    const target = pickVariantItem(comparableItems, source, { type: currentType, exterior })
    const isActive = currentExterior === exterior
    const activePrice = currentSellPrice.value || getReferencePrice(source)

    entries.push({
      id: `exterior-${exterior}`,
      kind: 'exterior',
      label: getExteriorText(exterior),
      price: isActive ? activePrice : (prices.length ? Math.min(...prices) : null),
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

const fetchBuyOrders = async (itemId) => {
  buyOrders.value = []
  if (!itemId) return

  buyOrdersLoading.value = true
  try {
    const orders = await getBuyOrdersByItemId(itemId)
    buyOrders.value = Array.isArray(orders) ? orders : []
  } catch (error) {
    buyOrders.value = []
  } finally {
    buyOrdersLoading.value = false
  }
}

const getRemainingQuantity = (order) => {
  const quantity = Number(order?.quantity || 0)
  const filled = Number(order?.filledQuantity || 0)
  return Math.max(quantity - filled, 0)
}

const openBuyOrderDialog = () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  const targetItemId = currentDisplayItemId.value
  if (!targetItemId) {
    ElMessage.error(TEXT.notFound)
    return
  }

  const suggestedPrice = displayReferencePrice.value || Number(selectedOrder.value?.price || 0) || 0.01
  buyForm.value = {
    price: Number(Math.max(suggestedPrice, 0.01).toFixed(2)),
    quantity: 1
  }
  showBuyOrderDialog.value = true
}

const submitBuyOrder = async () => {
  const targetItemId = currentDisplayItemId.value
  const price = Number(buyForm.value.price)
  const quantity = Number(buyForm.value.quantity || 1)

  if (!targetItemId) {
    ElMessage.error(TEXT.notFound)
    return
  }
  if (!Number.isFinite(price) || price <= 0) {
    ElMessage.warning(TEXT.inputValidBargainPrice)
    return
  }

  buyCreating.value = true
  try {
    await createBuyOrder({
      itemId: targetItemId,
      price: Number(price.toFixed(2)),
      quantity
    })
    ElMessage.success(TEXT.buyOrderCreatedSuccess)
    showBuyOrderDialog.value = false
    await fetchBuyOrders(targetItemId)
  } catch (error) {
    ElMessage.error(error?.message || TEXT.createBuyOrderFailed)
  } finally {
    buyCreating.value = false
  }
}

const openRespondBuyDialog = async (order) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }
  if (!order || order.userId === currentUserId.value) return

  respondTargetOrder.value = order
  respondForm.value.inventoryId = null
  showRespondBuyDialog.value = true
  respondInventoryLoading.value = true
  try {
    const inventory = await getMarketableInventory()
    respondInventory.value = Array.isArray(inventory) ? inventory : []
    if (matchingInventory.value.length === 1) {
      respondForm.value.inventoryId = matchingInventory.value[0].id
    }
  } catch (error) {
    respondInventory.value = []
    ElMessage.error(error?.message || TEXT.noMatchingInventory)
  } finally {
    respondInventoryLoading.value = false
  }
}

const submitRespondBuyOrder = async () => {
  const order = respondTargetOrder.value
  if (!order?.id || !respondForm.value.inventoryId) return

  respondSubmitting.value = true
  try {
    await respondBuyOrder(order.id, { inventoryId: respondForm.value.inventoryId })
    ElMessage.success(TEXT.respondBuySuccess)
    showRespondBuyDialog.value = false
    await fetchBuyOrders(getSourceItemId(order) || currentDisplayItemId.value)
    router.push('/user/orders')
  } catch (error) {
    ElMessage.error(error?.message || TEXT.respondBuyFailed)
  } finally {
    respondSubmitting.value = false
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
  buyOrders.value = []
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

      const resolvedItemId = getSourceItemId({ ...order, item: itemDetail.value || order?.item })
      await fetchSellOrders(resolvedItemId)
      await fetchBuyOrders(resolvedItemId)

      if (!sellOrders.value.length && order) {
        sellOrders.value = [order]
      }

      await loadRelatedItems(order)
      return
    }

    if (route.params.id) {
      itemDetail.value = await getItemById(route.params.id)
      const resolvedItemId = getSourceItemId({ item: itemDetail.value })
      await fetchSellOrders(resolvedItemId)
      await fetchBuyOrders(resolvedItemId)
      selectedOrder.value = sellOrders.value[0] || null
      await loadRelatedItems(selectedOrder.value || { item: itemDetail.value })
    }
  } catch (error) {
    selectedOrder.value = null
    itemDetail.value = null
    sellOrders.value = []
    buyOrders.value = []
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
  width: clamp(1080px, 72vw, 1408px);
  max-width: calc(100% - 48px);
  margin: 0 auto;
  padding: 88px 0 40px;
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
  border-radius: 0;
  overflow: hidden;
  box-shadow: 0 24px 58px rgba(15, 23, 42, 0.16);
}

.hero-panel {
  position: relative;
  background:
    radial-gradient(circle at 15% 48%, rgba(59, 130, 246, 0.16), transparent 22%),
    linear-gradient(90deg, rgba(30, 41, 59, 0.86) 0%, rgba(27, 34, 43, 0.96) 32%, rgba(27, 34, 43, 0.98) 100%);
  border: 0;
  color: #f8fafc;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(240px, 330px) minmax(0, 1fr);
  align-items: center;
  gap: 44px;
  min-height: 272px;
  padding: 34px 48px 32px;
}

.media-panel {
  display: flex;
}

.media-stage {
  position: relative;
  width: 100%;
  min-height: 218px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  background: transparent;
  overflow: visible;
}

.media-stage img {
  max-width: 100%;
  max-height: 138px;
  object-fit: contain;
  filter: drop-shadow(0 16px 24px rgba(0, 0, 0, 0.35));
}

.media-badge-row {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.media-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 14px;
  border-radius: 0;
  background: rgba(100, 116, 139, 0.9);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.media-badge-secondary {
  background: rgba(15, 23, 42, 0.68);
  color: #ff8f1f;
}

.media-badge-FN {
  background: rgba(34, 197, 94, 0.88);
}

.media-badge-MW {
  background: rgba(132, 204, 22, 0.88);
}

.media-badge-FT {
  background: rgba(245, 158, 11, 0.9);
}

.media-badge-WW {
  background: rgba(148, 163, 184, 0.92);
}

.media-badge-BS {
  background: rgba(239, 68, 68, 0.88);
}

.media-badge-quality-contraband {
  background: rgba(245, 223, 77, 0.94);
  color: #1f2937;
}

.media-badge-quality-covert {
  background: rgba(244, 63, 94, 0.92);
}

.media-badge-quality-classified {
  background: rgba(192, 38, 211, 0.92);
}

.media-badge-quality-restricted {
  background: rgba(139, 92, 246, 0.92);
}

.media-badge-quality-mil-spec {
  background: rgba(59, 130, 246, 0.92);
}

.media-badge-quality-industrial {
  background: rgba(96, 165, 250, 0.92);
}

.media-badge-quality-consumer,
.media-badge-category-case {
  background: rgba(148, 163, 184, 0.92);
}

.media-badge-quality-extraordinary {
  background: rgba(249, 115, 22, 0.92);
}

.media-badge-quality-exotic,
.media-badge-category-graffiti,
.media-badge-category-collectible {
  background: rgba(168, 85, 247, 0.92);
}

.media-badge-quality-remarkable {
  background: rgba(236, 72, 153, 0.92);
}

.media-badge-quality-high-grade {
  background: rgba(79, 142, 247, 0.92);
}

.media-badge-quality-normal-grade {
  background: rgba(226, 232, 240, 0.96);
  color: #1f2937;
}

.media-badge-quality-agent-grade,
.media-badge-category-agent {
  background: rgba(34, 197, 94, 0.92);
}

.media-badge-category-sticker,
.media-badge-category-music {
  background: rgba(37, 99, 235, 0.92);
}

.media-badge-category-charm,
.media-badge-category-tool {
  background: rgba(15, 118, 110, 0.92);
}

.media-badge-category-pass {
  background: rgba(154, 52, 18, 0.92);
}

.media-badge-type-Souvenir {
  color: #fbbf24;
}

.media-badge-type-Star {
  color: #ffffff;
}

.media-badge-type-StarStatTrak {
  color: #ffb347;
}

.media-wear-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 2px;
}

.media-wear-text {
  display: inline-flex;
  padding: 3px 0 4px;
  background: rgba(15, 23, 42, 0.82);
  color: rgba(226, 232, 240, 0.86);
  font-size: 13px;
  line-height: 1.4;
}

.media-wear-scale {
  position: relative;
  height: 6px;
  background: linear-gradient(90deg, #22c55e 0%, #facc15 45%, #ef4444 100%);
}

.media-wear-marker {
  position: absolute;
  top: -5px;
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 10px solid #e2e8f0;
  transform: translateX(-50%);
}

.info-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 15px;
  min-width: 0;
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
  font-size: clamp(30px, 2.35vw, 40px);
  line-height: 1.12;
}

.base-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  width: min(100%, 520px);
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(245, 158, 11, 0.42);
  color: rgba(226, 232, 240, 0.88);
  font-size: 14px;
}

.notice-bar {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 840px;
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
  gap: 32px;
  margin-top: 10px;
}

.price-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.price-group {
  display: flex;
  align-items: baseline;
  gap: 14px;
  min-width: 0;
}

.price-group span {
  display: inline-flex;
  margin-bottom: 0;
  color: rgba(203, 213, 225, 0.78);
  font-size: 14px;
}

.price-group strong {
  color: #f59e0b;
  font-size: 28px;
  line-height: 1;
}

.reference-price-source {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.14);
  color: #f8d68a;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  line-height: 1.4;
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
  gap: 16px;
}

.hero-actions :deep(.el-button) {
  min-width: 126px;
  height: 42px;
  margin-left: 0;
  border-radius: 2px;
  font-weight: 700;
}

.order-book-section {
  margin-top: 28px;
  overflow: hidden;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}

.order-book-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 34px;
  background: #101521;
  color: #fff;
}

.order-book-tabs {
  display: flex;
  align-items: center;
  gap: 34px;
  min-height: 72px;
}

.order-book-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 72px;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(226, 232, 240, 0.74);
  cursor: pointer;
  font-size: 17px;
  font-weight: 800;
}

.order-book-tab.active {
  color: #ffffff;
}

.order-book-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  border-radius: 999px;
  background: #60a5fa;
}

.order-book-tab.active::before {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 0;
  height: 0;
  border-right: 6px solid transparent;
  border-bottom: 0;
  border-left: 6px solid transparent;
  border-top: 6px solid #60a5fa;
  transform: translate(-50%, 100%);
}

.order-book-count {
  color: #dbeafe;
  font-weight: 700;
}

.orders-panel {
  min-width: 0;
}

.sell-orders-panel {
  border-radius: 0;
  margin-top: 0;
  box-shadow: none;
}

.buy-orders-section {
  margin-top: 0;
  overflow: hidden;
  border-radius: 0;
  background: #ffffff;
}

.buy-orders-layout {
  display: block;
}

.buy-orders-table {
  min-height: 96px;
  padding: 18px 24px 22px;
}

.market-tab-section {
  background: #ffffff;
}

.recommend-tab-section {
  background: #ffffff;
}

.detail-market-panel {
  min-width: 0;
}

.detail-recommend-panel {
  min-width: 0;
}

.buy-order-row {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(130px, 0.6fr) minmax(120px, 0.5fr) auto;
  gap: 18px;
  align-items: center;
  padding: 16px;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  background: #fffaf0;
}

.buy-order-row + .buy-order-row {
  margin-top: 12px;
}

.buy-order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.buy-order-item img {
  width: 74px;
  height: 54px;
  object-fit: contain;
}

.buy-order-item strong,
.buy-order-buyer strong {
  display: block;
  overflow: hidden;
  color: #172033;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.buy-order-item span,
.buy-order-buyer span,
.buy-order-price span {
  display: block;
  margin-bottom: 4px;
  color: #94a3b8;
  font-size: 13px;
}

.buy-order-price strong {
  color: #f59e0b;
  font-size: 22px;
}

.buy-order-row :deep(.el-button) {
  width: 100%;
  height: 34px;
  border-radius: 10px;
  font-weight: 700;
}

.dialog-static-value {
  min-height: 36px;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #172033;
  font-weight: 700;
}

.form-help {
  margin: 8px 0 0;
  color: #f97316;
  font-size: 13px;
}

.favorite-action {
  border-color: rgba(245, 158, 11, 0.34);
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.18);
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
  min-width: 104px;
  height: 34px;
  margin-left: auto;
  border-radius: 0;
  font-weight: 700;
}

.price-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 30px;
  padding: 18px 48px 20px;
  border-top: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(15, 23, 42, 0.32);
}

.price-strip-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 150px;
  min-height: 42px;
  padding: 0 20px;
  border: 1px solid rgba(148, 163, 184, 0.46);
  border-radius: 0;
  color: #cbd5e1;
  background: rgba(15, 23, 42, 0.1);
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
  font-size: 14px;
}

.price-strip-item strong {
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 800;
}

.price-strip-item.active {
  border-color: rgba(96, 165, 250, 0.86);
  background: rgba(96, 165, 250, 0.28);
  color: #ffffff;
}

.price-strip-item.accent {
  border-color: rgba(148, 163, 184, 0.34);
}

@media (max-width: 1024px) {
  .detail-shell {
    width: min(calc(100% - 40px), 1020px);
  }

  .hero-main {
    grid-template-columns: 1fr;
    gap: 24px;
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

  .media-badge-row {
    left: 0;
    right: 0;
  }

  .price-strip {
    gap: 10px;
    padding: 12px 18px 16px;
  }

  .price-strip-item {
    min-width: 0;
    flex: 1 1 140px;
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

  .order-book-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 16px 18px;
  }

  .order-book-tabs {
    width: 100%;
    min-height: 42px;
    gap: 22px;
    overflow-x: auto;
  }

  .order-book-tab {
    min-height: 44px;
    font-size: 17px;
    white-space: nowrap;
  }

  .order-book-tab.active::after {
    bottom: 0;
  }

  .order-book-tab.active::before {
    display: none;
  }

  .buy-order-row {
    grid-template-columns: 1fr;
    align-items: start;
  }

}
</style>
