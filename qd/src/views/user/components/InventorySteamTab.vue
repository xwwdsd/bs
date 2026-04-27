<template>
  <div class="steam-tab">
    <div class="analysis-overview" v-loading="analysisLoading">
      <article class="analysis-card analysis-card-accent">
        <span>资产总估值</span>
        <strong>¥ {{ displayTotalValue.toFixed(2) }}</strong>
      </article>
      <article class="analysis-card">
        <span>可出售数量</span>
        <strong>{{ sellableCount }}</strong>
      </article>
      <article class="analysis-card">
        <span>建议优先出售</span>
        <strong>{{ recommendedSellCount }}</strong>
      </article>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-top toolbar-top-right">
        <div class="toolbar-stats">
          <span>件数：<strong>{{ inventory.length }}</strong></span>
          <span>估值：<strong>¥ {{ totalValue.toFixed(2) }}</strong></span>
        </div>
      </div>

      <div class="toolbar-filters">
        <div class="filter-group">
          <el-select v-model="filters.quality" placeholder="品质" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in qualityOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.type" placeholder="类别" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in typeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.exterior" placeholder="外观" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in exteriorOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.sellableOnly" class="toolbar-select">
            <el-option label="显示全部" :value="false" />
            <el-option label="仅可出售" :value="true" />
          </el-select>
        </div>

        <div class="search-group">
          <el-input
            v-model="filters.search"
            placeholder="输入物品名称"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="toolbar-actions">
        <div class="sort-group">
          <button
            class="sort-button"
            :class="{ current: sortBy === 'time' }"
            @click="sortBy = 'time'"
          >
            时间
          </button>
          <button
            class="sort-button"
            :class="{ current: sortBy === 'price' }"
            @click="sortBy = 'price'"
          >
            价格
          </button>
          <button
            class="sort-button"
            :class="{ current: sortBy === 'wear' }"
            @click="sortBy = 'wear'"
          >
            磨损
          </button>
        </div>

        <div class="selection-summary">已选 {{ selectedCount }}/{{ filteredInventory.length || inventory.length }}</div>

        <div class="action-group">
          <el-button :loading="syncing" @click="handleRefresh">刷新</el-button>
          <el-button type="primary" :disabled="!hasSellableInView" @click="handleTopSell">出售</el-button>
        </div>
      </div>
    </div>

    <div class="content-card" v-loading="loading">
      <el-empty v-if="!filteredInventory.length && !loading" description="暂无库存数据" />

      <div v-else class="items-grid">
        <div
          v-for="item in filteredInventory"
          :key="item.id"
          class="item-card"
          :class="{ 'item-card-disabled': !canSellItem(item), 'item-card-selected': selectedInventoryId === item.id }"
          @click="selectInventory(item)"
        >
          <el-popover
            trigger="hover"
            placement="right-start"
            :width="360"
            :show-after="1500"
            :hide-after="120"
            popper-class="inventory-detail-popover"
          >
            <template #reference>
              <div class="card-image card-image-hoverable">
                <div class="image-top-row">
                  <span class="wear-tag" :class="getCardPrimaryBadgeClass(item)">{{ getCardPrimaryBadgeText(item) }}</span>
                  <span v-if="item.paintSeed !== null && item.paintSeed !== undefined" class="seed-tag">{{ item.paintSeed }}</span>
                  <span v-if="getCardSecondaryBadgeText(item)" class="type-tag" :class="getCardSecondaryBadgeClass(item)">
                    {{ getCardSecondaryBadgeText(item) }}
                  </span>
                </div>
                <img :src="item.iconUrl || item.item?.iconUrl || '/default-item.svg'" :alt="item.name || '饰品'" />
                <span class="note-tag">备注</span>
                <div v-if="shouldRenderWearPanel(item)" class="wear-panel">
                  <div class="wear-text">磨损: {{ getWearDisplay(item, 6, 2) }}</div>
                  <div class="wear-scale">
                    <span class="wear-marker" :style="{ left: `${getWearPercent(item)}%` }"></span>
                  </div>
                </div>
              </div>
            </template>
            <div class="detail-popover">
              <h5 class="detail-title">{{ item.name || item.item?.nameCn || item.item?.name || '未知饰品' }}</h5>
              <div class="detail-grid">
                <span class="detail-label">图案模板(paint seed)</span>
                <span class="detail-value">{{ formatDetailValue(item.paintSeed) }}</span>
                <span class="detail-label">皮肤编号(paint index)</span>
                <span class="detail-value">{{ formatDetailValue(item.paintIndex) }}</span>
                <span class="detail-label">磨损</span>
                <span class="detail-value">{{ getWearDisplay(item, 16, 6) }}</span>
                <span class="detail-label">品质</span>
                <span class="detail-value">{{ getDisplayRarityText(getInventoryRarity(item)) || '-' }}</span>
                <span class="detail-label">外观</span>
                <span class="detail-value">{{ getDisplayExterior(item) }}</span>
                <span class="detail-label">Asset ID</span>
                <span class="detail-value">{{ formatDetailValue(item.assetId) }}</span>
              </div>
              <p class="detail-description">{{ formatDetailValue(item.description) }}</p>
            </div>
          </el-popover>

          <div class="card-info">
            <h4 class="card-name" :title="item.name || item.item?.nameCn || item.item?.name || '未知饰品'">
              {{ item.name || item.item?.nameCn || item.item?.name || '未知饰品' }}
            </h4>
            <p class="card-subtitle">{{ getCardDisplaySubtitle(item) }}</p>
            <div class="card-price-row">
              <div class="price-meta">
                <p class="card-price">¥ {{ formatPrice(getInventoryReferencePrice(item)) }}</p>
                <span
                  v-if="getInventoryReferenceSourceText(item)"
                  class="reference-source-tag"
                  :class="`reference-source-tag-${getInventoryReferenceSource(item)}`"
                >
                  {{ getInventoryReferenceSourceText(item) }}
                </span>
                <span v-if="getCardStatusText(item)" class="card-status" :class="getCardStatusClass(item)">
                  {{ getCardStatusText(item) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="sellDialogVisible" title="创建出售订单" width="420px">
      <el-form label-position="top">
        <el-form-item label="饰品">
          <div class="dialog-name">{{ currentSellItem?.name || currentSellItem?.item?.nameCn || '-' }}</div>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="sellPrice" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sellDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sellLoading" @click="confirmSell">确认出售</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="marketSellDialogVisible"
      width="1060px"
      :show-close="false"
      :close-on-click-modal="false"
      class="market-sell-dialog"
      @closed="handleSellDialogClosed"
    >
      <div v-if="currentSellItem" class="buff-sell-dialog">
        <div class="buff-sell-header">
          <div class="buff-sell-header-side"></div>
          <div class="buff-sell-title-wrap">
            <h3 class="buff-sell-title">上架饰品 <span>(1件)</span></h3>
          </div>
          <div class="buff-sell-header-actions">
            <button
              type="button"
              class="buff-price-sync"
              :disabled="!sellReferencePrice"
              @click="applyReferencePrice"
            >
              一键定价
            </button>
            <button type="button" class="buff-close-button" @click="marketSellDialogVisible = false">&times;</button>
          </div>
        </div>

        <div class="buff-sell-table">
          <div class="buff-sell-table-head">
            <span class="buff-col-item">饰品</span>
            <span class="buff-col-market">市场价</span>
            <span class="buff-col-sale">出售价格</span>
            <span class="buff-col-actual">实收金额</span>
          </div>

          <div class="buff-sell-table-row">
            <div class="buff-item-cell">
              <div class="buff-item-preview">
                <img :src="currentSellItemImage" :alt="currentSellItemName" />
              </div>
              <div class="buff-item-copy">
                <h4 class="buff-item-name">{{ currentSellItemName }}</h4>
                <p class="buff-item-subtitle">{{ currentSellItemSubtitle }}</p>
                <div class="buff-item-meta">
                  <span>{{ currentSellItemRarity }}</span>
                  <span>{{ currentSellItemExterior }}</span>
                  <span v-if="currentSellItemWear">磨损: {{ currentSellItemWear }}</span>
                  <span v-if="currentSellItem?.assetId">Asset: {{ currentSellItem.assetId }}</span>
                </div>
              </div>
            </div>

            <div class="buff-market-cell">
              <strong class="buff-market-price">
                {{ sellReferencePrice ? `¥ ${formatPrice(sellReferencePrice)}` : '--' }}
              </strong>
            </div>

            <div class="buff-input-cell">
              <label class="buff-price-field" :class="{ 'is-focused': sellPriceFocused }">
                <span class="buff-price-prefix">¥</span>
                <input
                  v-model.number="sellPrice"
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputmode="decimal"
                  class="buff-price-input"
                  placeholder="出售价格"
                  @focus="sellPriceFocused = true"
                  @blur="handleSellPriceBlur"
                  @keyup.enter="confirmSell"
                />
              </label>
            </div>

            <div class="buff-actual-cell">
              <div class="buff-price-field buff-price-field-readonly">
                <span class="buff-price-prefix">¥</span>
                <span class="buff-price-readonly">
                  {{ sellPrice > 0 ? formatPrice(sellActualAmount) : '实收金额' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="buff-sell-footer">
          <div class="buff-sell-income">
            <span class="buff-sell-income-label">预计收入 |</span>
            <strong class="buff-sell-income-value">¥ {{ formatPrice(sellActualAmount) }}</strong>
            <span class="buff-sell-income-meta">(已扣除 ¥ {{ formatPrice(sellFeeAmount) }} 手续费)</span>
          </div>
            <p class="buff-sell-note">订单创建后买家付款并发送 Steam 报价，系统会自动检测交易状态。</p>
          <div class="buff-sell-actions">
            <button type="button" class="buff-footer-button buff-footer-button-ghost" @click="marketSellDialogVisible = false">
              取消
            </button>
            <button type="button" class="buff-footer-button buff-footer-button-primary" :disabled="sellLoading" @click="confirmSell">
              <span v-if="sellLoading">上架中...</span>
              <span v-else>上架</span>
            </button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createSellOrder } from '@/api/sellOrder'
import { getInventory, getInventoryAnalysis, syncInventory } from '@/api/inventory'
import {
  EXTERIOR_RANGE_MAP as WEAR_RANGE_MAP,
  extractWearFromText as extractWearFromMetadataText,
  normalizeWearValue
} from '@/utils/itemExterior'
import {
  QUALITY_TEXT_MAP,
  TYPE_TEXT_MAP,
  normalizeQualityKey,
  resolveItemQuality
} from '@/utils/itemClassification'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const loading = ref(false)
const syncing = ref(false)
const analysisLoading = ref(false)
const hasLoaded = ref(false)
const inventory = ref([])
const inventoryAnalysis = ref(null)
const sortBy = ref('time')
const sellDialogVisible = ref(false)
const marketSellDialogVisible = ref(false)
const currentSellItem = ref(null)
const sellPrice = ref(0)
const sellLoading = ref(false)
const sellPriceFocused = ref(false)
const selectedInventoryId = ref(null)
const SELL_ORDER_FEE_RATE = 0.05

const filters = ref({
  search: '',
  quality: '',
  type: '',
  exterior: '',
  sellableOnly: false
})

const qualityOptions = Object.entries(QUALITY_TEXT_MAP).map(([value, label]) => ({ value, label }))
const typeOptions = Object.entries(TYPE_TEXT_MAP).map(([value, label]) => ({ value, label }))
const exteriorOptions = [
  { value: 'FN', label: '崭新出厂' },
  { value: 'MW', label: '略有磨损' },
  { value: 'FT', label: '久经沙场' },
  { value: 'WW', label: '破损不堪' },
  { value: 'BS', label: '战痕累累' },
  { value: 'NoPaint', label: '无涂装' }
]

const toPositiveNumber = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

const getInventoryReferenceInfo = (item) => {
  if (hasTradeRestriction(item)) {
    return { price: 0, source: '' }
  }

  const candidate = [
    { price: item?.item?.steamReferencePrice, source: 'steam' },
    { price: item?.steamReferencePrice, source: 'steam' },
    { price: item?.item?.buffPrice, source: 'buff' },
    { price: item?.buffPrice, source: 'buff' },
    { price: item?.marketPrice, source: 'local' }
  ]
    .map((entry) => ({
      price: toPositiveNumber(entry.price),
      source: entry.source
    }))
    .find((entry) => entry.price !== null)

  return candidate || { price: null, source: '' }
}

const getInventoryReferencePrice = (item) => getInventoryReferenceInfo(item).price
const getInventoryReferenceSource = (item) => getInventoryReferenceInfo(item).source
const getInventoryReferenceSourceText = (item) => {
  const source = getInventoryReferenceSource(item)
  if (source === 'steam') return 'Steam参考价'
  if (source === 'buff') return 'Buff回退'
  if (source === 'local') return '站内参考'
  return ''
}

const totalValue = computed(() => inventory.value.reduce((sum, item) => sum + (getInventoryReferencePrice(item) || 0), 0))
const displayTotalValue = computed(() => Number(inventoryAnalysis.value?.totalValue ?? totalValue.value ?? 0))
const sellableCount = computed(() => Number(inventoryAnalysis.value?.sellableCount ?? 0))
const recommendedSellCount = computed(() => Number(inventoryAnalysis.value?.recommendedSellCount ?? 0))
const selectedCount = computed(() => (selectedInventoryId.value ? 1 : 0))
const sellReferencePrice = computed(() => getInventoryReferencePrice(currentSellItem.value))
const sellFeeAmount = computed(() => {
  const numeric = Number(sellPrice.value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric * SELL_ORDER_FEE_RATE : 0
})
const sellActualAmount = computed(() => {
  const numeric = Number(sellPrice.value)
  if (!Number.isFinite(numeric) || numeric <= 0) return 0
  return Math.max(numeric - sellFeeAmount.value, 0)
})
const currentSellItemName = computed(
  () => currentSellItem.value?.name || currentSellItem.value?.item?.nameCn || currentSellItem.value?.item?.name || '未知饰品'
)
const currentSellItemImage = computed(
  () => currentSellItem.value?.iconUrl || currentSellItem.value?.item?.iconUrl || '/default-item.svg'
)
const currentSellItemSubtitle = computed(() => {
  if (!currentSellItem.value) return '暂无饰品描述'
  return getCardDisplaySubtitle(currentSellItem.value) || '暂无饰品描述'
})
const currentSellItemRarity = computed(() => {
  if (!currentSellItem.value) return '未知品质'
  return getDisplayRarityText(getInventoryRarity(currentSellItem.value)) || '未知品质'
})
const currentSellItemExterior = computed(() => {
  if (!currentSellItem.value) return '未知外观'
  return getDisplayExterior(currentSellItem.value)
})
const currentSellItemWear = computed(() => {
  if (!currentSellItem.value || !shouldRenderWearPanel(currentSellItem.value)) return ''
  return getWearDisplay(currentSellItem.value, 6, 2)
})

const getInventoryRarity = (item) => resolveItemQuality(item)
const getInventoryExterior = (item) => getItemDisplayModel(item).filterExterior

const getDisplayRarityText = (rarity) => QUALITY_TEXT_MAP[rarity] || rarity || ''

const getDisplayExterior = (item) => {
  const map = {
    FN: '崭新出厂',
    MW: '略有磨损',
    FT: '久经沙场',
    WW: '破损不堪',
    BS: '战痕累累',
    NoPaint: '无涂装'
  }
  const exterior = getInventoryExterior(item)
  return map[exterior] || exterior || '未知外观'
}

const getExteriorCode = (item) => {
  const code = getInventoryExterior(item)
  return ['FN', 'MW', 'FT', 'WW', 'BS', 'NoPaint'].includes(code) ? code : 'UN'
}

const getCardDisplayModel = (item) => getItemDisplayModel(item)
const getCardPrimaryBadgeText = (item) => getCardDisplayModel(item).primaryBadge.text
const getCardPrimaryBadgeClass = (item) => {
  const badge = getCardDisplayModel(item).primaryBadge
  if (!badge) return 'wear-tag-UN'
  if (badge.kind === 'quality') return `wear-tag-quality-${badge.code}`
  if (badge.kind === 'category') return `wear-tag-category-${badge.code || 'other'}`
  return `wear-tag-${badge.code || 'UN'}`
}
const getCardSecondaryBadgeText = (item) => getCardDisplayModel(item).secondaryBadge?.text || ''
const getCardSecondaryBadgeClass = (item) => {
  const badge = getCardDisplayModel(item).secondaryBadge
  return badge ? `type-tag-${badge.code}` : ''
}
const getCardDisplaySubtitle = (item) => getCardDisplayModel(item).subtitle
const shouldRenderWearPanel = (item) => getCardDisplayModel(item).showWearModule
const getCardFilterExterior = (item) => getCardDisplayModel(item).filterExterior

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric >= 0 ? numeric.toFixed(2) : '--'
}

const formatWear = (value, digits = 3) => {
  if (value === null || value === undefined || value === '') return '-'
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(digits) : '-'
}

const getWearValue = (item) => {
  const candidates = [
    item?.paintWear,
    item?.paintwear,
    item?.wear,
    item?.item?.paintWear,
    item?.item?.paintwear,
    item?.item?.wear
  ]

  for (const raw of candidates) {
    const normalized = normalizeWearValue(raw)
    if (normalized !== null) {
      return normalized
    }
  }

  return extractWearFromMetadataText(item?.description)
}

const getWearRange = (item, digits = 2) => {
  const exterior = getExteriorCode(item)
  const range = WEAR_RANGE_MAP[exterior]
  if (!range) return '-'
  return `${range[0].toFixed(digits)}-${range[1].toFixed(digits)}`
}

const getWearDisplay = (item, exactDigits = 6, rangeDigits = 2) => {
  const wear = getWearValue(item)
  if (wear === null || wear === undefined || wear === '') {
    return getWearRange(item, rangeDigits)
  }
  return formatWear(wear, exactDigits)
}

const getWearPercent = (item) => {
  const exactWear = getWearValue(item)
  if (exactWear === null || exactWear === undefined || exactWear === '') {
    const exteriorMidpointMap = {
      FN: 0.035,
      MW: 0.11,
      FT: 0.265,
      WW: 0.415,
      BS: 0.725
    }
    const fallback = exteriorMidpointMap[getExteriorCode(item)]
    return Number.isFinite(fallback) ? fallback * 100 : 0
  }
  const numeric = Number(exactWear)
  const normalized = numeric > 1 && numeric <= 100 ? numeric / 100 : numeric
  return Math.max(0, Math.min(100, normalized * 100))
}

const formatDetailValue = (value) => {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text || '-'
}

const roundCurrency = (value) => Math.round(Number(value || 0) * 100) / 100

const normalizeSellPrice = (value, fallback = 0.01) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return roundCurrency(Math.max(fallback, 0.01))
  }
  return roundCurrency(Math.max(numeric, 0.01))
}

const isMarketableItem = (item) => {
  const value = item?.isMarketable
  return value === 1 || value === '1' || value === true
}

const getStatusCode = (item) => Number(item?.status ?? 0)

const hasExplicitTradeProtectionText = (text) => {
  const normalized = String(text || '').replace(/<[^>]+>/g, ' ').trim().toLowerCase()
  if (!normalized) return false
  if (
    /(受交易保护|交易保护|交易冷却|不可交易|不可出售|不可转让|不能被交易|不能被出售|不能被转让|trade hold|trade restricted|tradable after|tradeable after|cannot be traded|cannot be sold|cannot be marketed|cannot be transferred|not tradable|not marketable)/.test(
      normalized
    )
  ) {
    return true
  }
  return /(before|until|之前).*(cannot|not|不可|不能).*(trade|market|sell|transfer|交易|出售|转让)/.test(normalized)
}

const detectSteamRestriction = (item) => {
  if (item?.tradableAfter || item?.marketableAfter || item?.tradeLockUntil || item?.trade_lock_until) return true

  const reason = String(item?.marketableReason || '').trim()
  const ownerDescriptions = Array.isArray(item?.owner_descriptions)
    ? item.owner_descriptions
    : Array.isArray(item?.ownerDescriptions)
      ? item.ownerDescriptions
      : []
  const fraudWarnings = Array.isArray(item?.fraudwarnings)
    ? item.fraudwarnings
    : Array.isArray(item?.fraudWarnings)
      ? item.fraudWarnings
      : []
  const extraText = [...ownerDescriptions, ...fraudWarnings].map((v) => String(v || '').replace(/<[^>]+>/g, ' ')).join(' ')

  if (hasExplicitTradeProtectionText(extraText)) return true
  if (!reason) return false
  if (reason === '该物品存在 Steam 交易限制') {
    return false
  }

  return hasExplicitTradeProtectionText(reason)
}

const hasTradeRestriction = (item) => {
  if (detectSteamRestriction(item)) return true
  const reason = String(item?.marketableReason || '').trim()
  if (!reason || reason === '可出售' || reason === '该物品存在 Steam 交易限制') return false
  return /(不可|限制|受限|冷却|锁定|禁止)/.test(reason)
}

const canSellItem = (item) => isMarketableItem(item) && getStatusCode(item) === 0 && !hasTradeRestriction(item)

const getSellDisabledReason = (item) => {
  if (!item) return '-'
  if (detectSteamRestriction(item)) return '该物品存在 Steam 交易限制'
  if (hasTradeRestriction(item)) return item.marketableReason
  if (getStatusCode(item) !== 0) {
    const statusMap = {
      1: '该物品已售出',
      2: '该物品正在交易中',
      3: '该物品已在售'
    }
    return statusMap[getStatusCode(item)] || '该物品当前不可出售'
  }
  if (!isMarketableItem(item)) return '该物品当前不可上架出售'
  return '可出售'
}

const getSecondaryDisabledReason = (item) => {
  const reason = getSellDisabledReason(item)
  if (!reason) return ''
  if (detectSteamRestriction(item)) return ''
  return reason
}

const getCardStatusText = (item) => {
  if (!item) return ''
  if (detectSteamRestriction(item) || hasTradeRestriction(item)) return '无法出售'
  if (canSellItem(item)) return '可出售'

  const statusCode = getStatusCode(item)
  if (statusCode === 1) return '已售出'
  if (statusCode === 2) return '交易中'
  if (statusCode === 3) return '已在售'
  if (!isMarketableItem(item)) return '不可上架'

  return getSecondaryDisabledReason(item)
}

const getCardStatusClass = (item) => {
  if (!item) return ''
  if (canSellItem(item)) return 'card-status-sellable'
  if (detectSteamRestriction(item) || hasTradeRestriction(item) || !isMarketableItem(item)) {
    return 'card-status-disabled'
  }
  return 'card-status-muted'
}

const filteredInventory = computed(() => {
  const keyword = filters.value.search.trim().toLowerCase()
  const result = inventory.value.filter((item) => {
    const name = String(item.name || item.item?.nameCn || item.item?.name || '').toLowerCase()
    const displayModel = getCardDisplayModel(item)
    const exterior = getCardFilterExterior(item)
    const quality = displayModel.resolvedQuality
    const type = displayModel.resolvedType
    const matchKeyword = !keyword || name.includes(keyword)
    const matchQuality = !filters.value.quality || quality === normalizeQualityKey(filters.value.quality)
    const matchType = !filters.value.type || type === filters.value.type
    const matchExterior = !filters.value.exterior || exterior === filters.value.exterior
    const matchSellable = !filters.value.sellableOnly || canSellItem(item)
    return matchKeyword && matchQuality && matchType && matchExterior && matchSellable
  })

  return [...result].sort((a, b) => {
    if (sortBy.value === 'price') {
      return (getInventoryReferencePrice(b) || 0) - (getInventoryReferencePrice(a) || 0)
    }
    if (sortBy.value === 'wear') {
      const wearA = getWearValue(a)
      const wearB = getWearValue(b)
      if (wearA === null && wearB === null) return 0
      if (wearA === null) return 1
      if (wearB === null) return -1
      return wearA - wearB
    }
    return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
  })
})

const hasSellableInView = computed(() => filteredInventory.value.some((item) => canSellItem(item)))

const selectedSellItem = computed(() => {
  if (!selectedInventoryId.value) return null
  return filteredInventory.value.find((item) => item.id === selectedInventoryId.value) || null
})

const normalizeInventory = (payload) => {
  if (Array.isArray(payload)) return payload
  if (payload?.list) return payload.list
  if (payload?.data?.list) return payload.data.list
  return []
}

const fetchInventoryAnalysis = async () => {
  analysisLoading.value = true
  try {
    inventoryAnalysis.value = await getInventoryAnalysis()
  } catch (error) {
    inventoryAnalysis.value = null
  } finally {
    analysisLoading.value = false
  }
}

const fetchInventory = async () => {
  loading.value = true
  try {
    const payload = await getInventory()
    inventory.value = normalizeInventory(payload)
    hasLoaded.value = true
    await fetchInventoryAnalysis()
    if (!selectedInventoryId.value && inventory.value.length) {
      const firstSellable = inventory.value.find((item) => canSellItem(item))
      selectedInventoryId.value = (firstSellable || inventory.value[0]).id
    } else if (selectedInventoryId.value && !inventory.value.some((item) => item.id === selectedInventoryId.value)) {
      selectedInventoryId.value = null
    }
  } catch (error) {
    ElMessage.error(error?.message || '获取库存失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = async () => {
  syncing.value = true
  try {
    await syncInventory()
    ElMessage.success('库存刷新成功')
    await fetchInventory()
  } catch (error) {
    ElMessage.error(error?.message || '库存刷新失败')
  } finally {
    syncing.value = false
  }
}

const selectInventory = (item) => {
  selectedInventoryId.value = item?.id || null
}

const handleTopSell = () => {
  const fallback = filteredInventory.value.find((item) => canSellItem(item))
  const target = selectedSellItem.value && canSellItem(selectedSellItem.value) ? selectedSellItem.value : fallback
  if (!target) {
    ElMessage.warning('当前列表没有可出售的物品')
    return
  }
  selectedInventoryId.value = target.id
  openSellDialog(target)
}

const openSellDialog = (item) => {
  currentSellItem.value = item
  sellPrice.value = normalizeSellPrice(getInventoryReferencePrice(item) || 0.01)
  marketSellDialogVisible.value = true
}

const applyReferencePrice = () => {
  sellPrice.value = normalizeSellPrice(sellReferencePrice.value || 0.01)
}

const handleSellPriceBlur = () => {
  sellPriceFocused.value = false
  sellPrice.value = normalizeSellPrice(sellPrice.value, sellReferencePrice.value || 0.01)
}

const handleSellDialogClosed = () => {
  sellPriceFocused.value = false
  currentSellItem.value = null
}

const confirmSell = async () => {
  if (!currentSellItem.value) return
  const normalizedPrice = normalizeSellPrice(sellPrice.value, sellReferencePrice.value || 0.01)
  if (!Number.isFinite(normalizedPrice) || normalizedPrice <= 0) {
    ElMessage.warning('请输入正确的出售价格')
    return
  }

  sellPrice.value = normalizedPrice
  sellLoading.value = true
  try {
    await createSellOrder({
      inventoryId: currentSellItem.value.id,
      price: normalizedPrice
    })
    ElMessage.success('出售订单创建成功')
    marketSellDialogVisible.value = false
    sellDialogVisible.value = false
    await fetchInventory()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '创建出售订单失败')
  } finally {
    sellLoading.value = false
  }
}

const handleSearch = () => {
  filters.value.search = filters.value.search.trim()
}

watch(
  () => props.active,
  (active) => {
    if (active && !hasLoaded.value) {
      fetchInventory()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.steam-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.analysis-overview {
  display: grid;
  gap: 16px;
}

.analysis-overview {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.analysis-card,
.toolbar-card,
.content-card {
  border: 1px solid #e4e8f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.analysis-card {
  padding: 20px 22px;
}

.analysis-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.analysis-card strong {
  color: #0f172a;
  font-size: 28px;
  line-height: 1.2;
}

.analysis-card-accent {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(245, 158, 11, 0.14));
}

.toolbar-card {
  padding: 24px;
}

.toolbar-top,
.toolbar-filters,
.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-filters,
.toolbar-actions {
  margin-top: 18px;
}

.toolbar-top-right {
  justify-content: flex-end;
}

.toolbar-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #475569;
  font-size: 15px;
  white-space: nowrap;
}

.toolbar-stats strong {
  color: #f59e0b;
  font-size: 20px;
}

.filter-group,
.action-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 150px;
}

.search-group {
  min-width: 300px;
  flex: 1;
  max-width: 460px;
}

.search-input {
  width: 100%;
}

.sort-group {
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  border: 1px solid #d4d8e3;
  border-radius: 12px;
}

.sort-button {
  border: 0;
  border-right: 1px solid #d4d8e3;
  background: #fff;
  color: #64748b;
  padding: 12px 22px;
  font-size: 14px;
  cursor: pointer;
}

.sort-button:last-child {
  border-right: 0;
}

.sort-button.current {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}

.selection-summary {
  color: #64748b;
  font-size: 14px;
}

.content-card {
  padding: 24px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
}

.item-card {
  overflow: hidden;
  border: 1px solid #d7dce4;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.14);
}

.item-card-selected {
  border-color: #3f8fe8;
  box-shadow: 0 0 0 2px rgba(63, 143, 232, 0.24), 0 10px 20px rgba(15, 23, 42, 0.14);
}

.item-card-disabled {
  opacity: 0.94;
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 182px;
  padding: 16px 14px 52px;
  background: radial-gradient(circle at 50% 82%, rgba(255, 255, 255, 0.26), transparent 50%),
    linear-gradient(180deg, #5b606a 0%, #454a54 100%);
  border-bottom: 1px solid #2b3340;
}

.card-image-hoverable {
  cursor: pointer;
}

.card-image img {
  max-width: 100%;
  max-height: 108px;
  object-fit: contain;
  filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.35));
}

.image-top-row {
  position: absolute;
  left: 0;
  top: 0;
  display: flex;
  align-items: center;
  gap: 2px;
}

.wear-tag {
  padding: 4px 8px;
  background: #3f8f49;
  color: #fff;
  font-size: 14px;
  line-height: 1;
}

.wear-tag-FN {
  background: #329545;
}

.wear-tag-MW {
  background: #5d9545;
}

.wear-tag-FT {
  background: #efab3d;
}

.wear-tag-WW {
  background: #8c8f96;
}

.wear-tag-BS {
  background: #c85050;
}

.wear-tag-NoPaint {
  background: #f3f4f6;
  color: #334155;
}

.wear-tag-category-sticker,
.wear-tag-category-music {
  background: #2563eb;
}

.wear-tag-category-graffiti {
  background: #7c3aed;
}

.wear-tag-category-charm {
  background: #0f766e;
}

.wear-tag-category-agent {
  background: #16a34a;
}

.wear-tag-category-case {
  background: #475569;
}

.wear-tag-category-tool {
  background: #0f766e;
}

.wear-tag-category-pass {
  background: #7c2d12;
}

.wear-tag-category-collectible {
  background: #7c3aed;
}

.wear-tag-UN {
  background: #5d8a42;
}

.wear-tag-quality-contraband {
  background: #f5df4d;
  color: #1f2937;
}

.wear-tag-quality-covert {
  background: #d9485f;
}

.wear-tag-quality-classified {
  background: #c84cff;
}

.wear-tag-quality-restricted {
  background: #8b5cf6;
}

.wear-tag-quality-mil-spec {
  background: #3b82f6;
}

.wear-tag-quality-industrial {
  background: #60a5fa;
}

.wear-tag-quality-consumer {
  background: #94a3b8;
}

.wear-tag-quality-extraordinary {
  background: #f97316;
}

.wear-tag-quality-exotic {
  background: #a855f7;
}

.wear-tag-quality-remarkable {
  background: #ec4899;
}

.wear-tag-quality-high-grade {
  background: #4f8ef7;
}

.wear-tag-quality-normal-grade {
  background: #e5e7eb;
  color: #1f2937;
}

.wear-tag-quality-agent-grade {
  background: #22c55e;
}

.seed-tag {
  padding: 4px 7px;
  background: rgba(106, 112, 123, 0.92);
  color: #d5d8de;
  font-size: 12px;
  line-height: 1;
}

.type-tag {
  padding: 4px 8px;
  background: rgba(15, 21, 34, 0.95);
  color: #ff8f1f;
  font-size: 14px;
  line-height: 1;
}

.type-tag-Souvenir {
  color: #fbbf24;
}

.type-tag-Star {
  color: #f8fafc;
}

.type-tag-StarStatTrak {
  color: #ffb347;
}

.note-tag {
  position: absolute;
  left: 10px;
  bottom: 38px;
  padding: 4px 10px;
  background: rgba(186, 192, 202, 0.78);
  color: #f5f7fa;
  font-size: 14px;
}

.wear-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
}

.wear-text {
  padding: 1px 10px 2px;
  background: rgba(31, 40, 58, 0.82);
  color: #aab4c8;
  font-size: 13px;
  line-height: 1.35;
}

.wear-scale {
  position: relative;
  height: 6px;
  background: linear-gradient(90deg, #17a62a 0%, #17a62a 20%, #e0be4a 45%, #d3884a 68%, #be4343 100%);
}

.wear-marker {
  position: absolute;
  top: -3px;
  width: 2px;
  height: 12px;
  background: #f3f4f6;
  box-shadow: 0 0 4px rgba(255, 255, 255, 0.8);
  transform: translateX(-1px);
}

.card-info {
  padding: 12px;
}

.card-name {
  margin: 0;
  overflow: hidden;
  color: #122238;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.35;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.card-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
}

.card-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
}

.price-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-price {
  margin: 0;
  color: #ed9b00;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.reference-source-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.4;
  white-space: nowrap;
}

.reference-source-tag-steam {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
}

.reference-source-tag-buff {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.reference-source-tag-local {
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
}

.card-status {
  font-size: 12px;
  white-space: nowrap;
}

.card-status-sellable {
  color: #ef4444;
}

.card-status-disabled {
  color: #ef4444;
}

.card-status-muted {
  color: #a16207;
}

.dialog-name {
  color: #111827;
}

.market-sell-dialog :deep(.el-dialog) {
  overflow: hidden;
  border: 1px solid #d7dee9;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.28);
}

.market-sell-dialog :deep(.el-dialog__header) {
  display: none;
}

.market-sell-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.buff-sell-dialog {
  display: flex;
  flex-direction: column;
  min-height: 430px;
  background: #fff;
}

.buff-sell-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f6;
}

.buff-sell-header-side {
  min-height: 1px;
}

.buff-sell-title-wrap {
  text-align: center;
}

.buff-sell-title {
  margin: 0;
  color: #3b4a61;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.buff-sell-title span {
  color: #8a94a7;
  font-size: 14px;
  font-weight: 500;
}

.buff-sell-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.buff-price-sync {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #8da2c5;
  border-radius: 4px;
  background: #5b7398;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.buff-price-sync:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.buff-price-sync:not(:disabled):hover {
  transform: translateY(-1px);
}

.buff-close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: none;
  border: 0;
  background: transparent;
  color: #9ca3af;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s ease;
}

.buff-close-button:hover {
  color: #475569;
}

.buff-sell-table {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.buff-sell-table-head {
  display: grid;
  grid-template-columns: minmax(0, 2.9fr) minmax(120px, 0.95fr) minmax(190px, 1.15fr) minmax(190px, 1.15fr);
  align-items: center;
  padding: 0 26px;
  height: 58px;
  border-bottom: 1px solid #eef2f6;
  color: #8f98ab;
  font-size: 15px;
  background: #fbfbfc;
}

.buff-sell-table-row {
  display: grid;
  grid-template-columns: minmax(0, 2.9fr) minmax(120px, 0.95fr) minmax(190px, 1.15fr) minmax(190px, 1.15fr);
  align-items: center;
  gap: 22px;
  padding: 22px 26px 28px;
}

.buff-item-cell {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.buff-item-thumb {
  display: none;
}

.buff-item-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 104px;
  height: 78px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  background: linear-gradient(180deg, #727781 0%, #575c66 100%);
}

.buff-item-preview img {
  max-width: 88px;
  max-height: 60px;
  object-fit: contain;
  filter: drop-shadow(0 8px 16px rgba(15, 23, 42, 0.24));
}

.buff-item-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.buff-item-name {
  margin: 0;
  color: #2b3443;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.45;
  word-break: break-word;
}

.buff-item-subtitle {
  margin: 0;
  color: #778296;
  font-size: 13px;
  line-height: 1.4;
}

.buff-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  color: #7d8798;
  font-size: 12px;
}

.buff-market-cell,
.buff-input-cell,
.buff-actual-cell {
  display: flex;
  align-items: center;
}

.buff-market-price {
  color: #f59e0b;
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.buff-price-field {
  display: flex;
  align-items: center;
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e4e7ed;
  border-radius: 2px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.buff-price-field.is-focused {
  border-color: #b9c5d8;
  box-shadow: 0 0 0 3px rgba(91, 115, 152, 0.08);
}

.buff-price-prefix {
  color: #b8bec8;
  font-size: 18px;
  font-weight: 700;
}

.buff-price-input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: #374151;
  font-size: 18px;
  font-weight: 500;
}

.buff-price-input::placeholder,
.buff-price-readonly {
  color: #b8bec8;
}

.buff-price-field-readonly {
  justify-content: flex-start;
  gap: 8px;
  background: #fafafa;
}

.buff-price-readonly {
  font-size: 18px;
  font-weight: 500;
}

.buff-price-input::-webkit-outer-spin-button,
.buff-price-input::-webkit-inner-spin-button {
  margin: 0;
  -webkit-appearance: none;
}

.buff-price-input[type='number'] {
  appearance: textfield;
  -moz-appearance: textfield;
}

.buff-sell-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px 18px;
  padding: 18px 28px 20px;
  background: #32353b;
}

.buff-sell-income {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.buff-sell-income-label {
  color: #b7bfcb;
  font-size: 16px;
}

.buff-sell-income-value {
  color: #f59e0b;
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
}

.buff-sell-income-meta {
  color: #a9b1be;
  font-size: 14px;
}

.buff-sell-note {
  grid-column: 1 / 2;
  margin: 0;
  color: #f59e0b;
  font-size: 14px;
  line-height: 1.6;
}

.buff-sell-actions {
  grid-column: 2 / 3;
  grid-row: 1 / 3;
  display: flex;
  align-items: center;
  gap: 14px;
}

.buff-footer-button {
  min-width: 124px;
  height: 50px;
  border: 0;
  border-radius: 2px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.buff-footer-button:hover {
  transform: translateY(-1px);
}

.buff-footer-button-ghost {
  background: rgba(255, 255, 255, 0.1);
  color: #f8fafc;
}

.buff-footer-button-primary {
  background: #537bd7;
  color: #fff;
}

.buff-footer-button-primary:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.buff-col-item,
.buff-col-market,
.buff-col-sale,
.buff-col-actual {
  display: flex;
  align-items: center;
}

.buff-col-market,
.buff-col-sale,
.buff-col-actual {
  justify-content: center;
}

.buff-market-cell,
.buff-input-cell,
.buff-actual-cell {
  justify-content: center;
}

.detail-popover {
  color: #e5e9f1;
}

.detail-title {
  margin: 0 0 10px;
  color: #f8fafc;
  font-size: 16px;
  line-height: 1.4;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px 10px;
  margin-bottom: 10px;
}

.detail-label {
  color: #9ca8bb;
  font-size: 12px;
}

.detail-value {
  color: #f0f4fa;
  font-size: 12px;
  font-weight: 600;
  text-align: right;
}

.detail-description {
  margin: 0;
  color: #c8d1df;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
}

:deep(.inventory-detail-popover.el-popover),
:deep(.inventory-detail-popover.el-popper) {
  border: 1px solid #313c52;
  background: linear-gradient(180deg, #1f293a 0%, #151d2d 100%);
  box-shadow: 0 16px 36px rgba(3, 7, 18, 0.55);
}

:deep(.inventory-detail-popover.el-popper .el-popper__arrow::before) {
  border: 1px solid #313c52;
  background: #1f293a;
}

@media (max-width: 1200px) {
  .items-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .analysis-overview {
    grid-template-columns: 1fr;
  }

  .toolbar-top,
  .toolbar-filters,
  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-stats {
    justify-content: space-between;
  }

  .search-group {
    max-width: none;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 960px) {
  .buff-sell-header {
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 12px;
  }

  .buff-sell-header-side {
    display: none;
  }

  .buff-sell-header-actions {
    width: 100%;
    justify-content: center;
  }

  .buff-sell-table-head {
    display: none;
  }

  .buff-sell-table-row {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .buff-item-cell,
  .buff-market-cell,
  .buff-input-cell,
  .buff-actual-cell {
    justify-content: flex-start;
  }

  .buff-sell-footer {
    grid-template-columns: 1fr;
  }

  .buff-sell-actions {
    grid-column: auto;
    grid-row: auto;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .items-grid {
    grid-template-columns: 1fr;
  }

  .market-sell-dialog :deep(.el-dialog) {
    width: min(1060px, calc(100vw - 16px)) !important;
    margin: 0 auto;
  }

  .buff-sell-title {
    font-size: 20px;
  }

  .buff-sell-table-row {
    padding: 18px 16px 20px;
  }

  .buff-item-cell {
    grid-template-columns: 84px minmax(0, 1fr);
    gap: 12px;
  }

  .buff-item-preview {
    width: 84px;
    height: 68px;
  }

  .buff-item-preview img {
    max-width: 72px;
    max-height: 50px;
  }

  .buff-market-price {
    font-size: 20px;
  }

  .buff-price-field {
    height: 42px;
  }

  .buff-price-input,
  .buff-price-readonly {
    font-size: 16px;
  }

  .buff-sell-income {
    flex-wrap: wrap;
    gap: 6px 10px;
  }

  .buff-sell-income-value {
    font-size: 30px;
  }

  .buff-sell-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .buff-footer-button {
    width: 100%;
  }
}
</style>
