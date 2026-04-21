<template>
  <section id="sell-orders" class="sell-section" :class="{ compact }" v-loading="loading">
    <div v-if="!hideHeader" class="section-toolbar">
      <div class="section-tab active">{{ text.currentOnSale }}</div>
      <span class="section-count">{{ orders.length }} {{ text.saleRecordSuffix }}</span>
    </div>

    <el-empty v-if="!orders.length && !loading" :description="text.emptyListings" />

    <div v-else class="sell-table">
      <div class="sell-filter-bar">
        <div class="sell-filter-main">
          <el-popover
            v-model:visible="popoverState.exterior"
            trigger="click"
            placement="bottom-start"
            :width="188"
            :show-arrow="false"
            :popper-style="filterPopoverStyle"
            popper-class="sell-filter-popper"
          >
            <template #reference>
              <button
                type="button"
                class="filter-trigger"
                :class="{ active: popoverState.exterior, selected: !!filterState.exterior }"
              >
                <span>{{ exteriorTriggerLabel }}</span>
              </button>
            </template>

            <div class="filter-menu">
              <button
                type="button"
                class="filter-menu-item"
                :class="{ active: !filterState.exterior }"
                @click="selectExterior('')"
              >
                不限
              </button>
              <button
                v-for="option in exteriorOptions"
                :key="option.value"
                type="button"
                class="filter-menu-item"
                :class="{ active: filterState.exterior === option.value }"
                @click="selectExterior(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </el-popover>

          <el-popover
            v-model:visible="popoverState.paintSeed"
            trigger="click"
            placement="bottom-start"
            :width="228"
            :show-arrow="false"
            :popper-style="filterPopoverStyle"
            popper-class="sell-filter-popper"
            @show="syncPaintSeedDraft"
          >
            <template #reference>
              <button
                type="button"
                class="filter-trigger"
                :class="{ active: popoverState.paintSeed, selected: !!filterState.paintSeed }"
              >
                <span>{{ paintSeedTriggerLabel }}</span>
              </button>
            </template>

            <div class="seed-panel">
              <div class="seed-search-row">
                <input
                  v-model="paintSeedDraft"
                  type="text"
                  placeholder="请输入模板"
                  @keyup.enter="applyPaintSeed"
                />
                <button type="button" class="seed-search-button" @click="applyPaintSeed">
                  <el-icon><Search /></el-icon>
                </button>
              </div>
            </div>
          </el-popover>
        </div>

        <div class="sell-filter-side">
          <el-popover
            v-model:visible="popoverState.price"
            trigger="click"
            placement="bottom-end"
            :width="314"
            :show-arrow="false"
            :popper-style="filterPopoverStyle"
            popper-class="sell-filter-popper"
            @show="syncPriceDraft"
          >
            <template #reference>
              <button
                type="button"
                class="filter-trigger price-trigger"
                :class="{ active: popoverState.price, selected: hasPriceFilter }"
              >
                <span class="price-trigger-currency">{{ text.currency }}</span>
                <span>{{ priceTriggerMinLabel }}</span>
                <em>-</em>
                <span>{{ priceTriggerMaxLabel }}</span>
              </button>
            </template>

            <div class="price-panel">
              <div class="price-panel-row">
                <span>{{ text.currency }}</span>
                <input v-model="priceDraft.min" type="number" min="0" step="0.01" placeholder="最低价" />
                <em>-{{ text.currency }}</em>
                <input v-model="priceDraft.max" type="number" min="0" step="0.01" placeholder="最高价" />
              </div>
              <div class="price-panel-actions">
                <button type="button" class="panel-action" @click="clearPriceRange">清除</button>
                <button type="button" class="panel-action primary" @click="applyPriceRange">确定</button>
              </div>
            </div>
          </el-popover>

          <el-popover
            v-model:visible="popoverState.sort"
            trigger="click"
            placement="bottom-end"
            :width="188"
            :show-arrow="false"
            :popper-style="filterPopoverStyle"
            popper-class="sell-filter-popper"
          >
            <template #reference>
              <button
                type="button"
                class="filter-trigger sort-trigger"
                :class="{ active: popoverState.sort, selected: filterState.sort !== 'default' }"
              >
                <span>{{ sortTriggerLabel }}</span>
              </button>
            </template>

            <div class="filter-menu">
              <button
                v-for="option in sortOptions"
                :key="option.value"
                type="button"
                class="filter-menu-item"
                :class="{ active: filterState.sort === option.value }"
                @click="selectSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </el-popover>

          <button v-if="hasActiveFilters" type="button" class="filter-reset" @click="resetFilters">重置</button>
          <span class="filter-result">{{ displayOrders.length }} / {{ orders.length }} 条</span>
        </div>
      </div>

      <el-empty v-if="!displayOrders.length && !loading" description="暂无符合筛选条件的在售记录" />

      <template v-else>
        <div class="sell-table-head">
          <span>{{ text.item }}</span>
          <span>{{ text.wearLevel }}</span>
          <span>{{ text.seller }}</span>
          <span>{{ text.price }}</span>
          <span>{{ text.action }}</span>
        </div>

        <article
          v-for="order in displayOrders"
          :key="order.id"
          class="sell-row"
          :class="{ active: selectedOrderId === order.id }"
        >
          <div class="table-cell item-cell">
            <div class="item-thumb">
              <img :src="getItemIcon(order)" :alt="getItemName(order)" />
            </div>
            <div class="item-copy">
              <strong>{{ getItemName(order) }}</strong>
              <span>{{ getOrderSubtitle(order) }}</span>
            </div>
          </div>

          <div class="table-cell wear-cell" @click="$emit('select-order', order.id)">
            <p v-if="shouldShowWearInfo(order)" class="wear-value">{{ text.wear }}: {{ getWearDisplay(order) }}</p>
            <div v-if="shouldShowWearInfo(order) && hasWearVisual(order)" class="wear-bar">
              <span class="wear-marker" :style="{ left: `${getWearPercent(order)}%` }"></span>
            </div>
            <div class="status-row" :class="{ compact: !shouldShowWearInfo(order) }">
              <span class="status-pill" :class="getOrderPrimaryBadgeClass(order)">{{ getOrderPrimaryBadgeText(order) }}</span>
              <span
                v-if="getOrderSecondaryBadgeText(order)"
                class="status-pill status-pill-type"
                :class="getOrderSecondaryBadgeClass(order)"
              >
                {{ getOrderSecondaryBadgeText(order) }}
              </span>
              <span class="status-pill" :class="{ warn: getOrderStatus(order).tone === 'warn' }">
                {{ getOrderStatus(order).text }}
              </span>
              <span v-if="selectedOrderId === order.id" class="status-pill current">{{ text.currentViewing }}</span>
            </div>
          </div>

          <div class="table-cell seller-cell">
            <el-avatar :size="44" :src="order.user?.avatar || ''">
              {{ getAvatarFallback(order.user) }}
            </el-avatar>
            <div class="seller-info">
              <strong>{{ order.user?.username || text.unknownSeller }}</strong>
            </div>
          </div>

          <div class="table-cell price-cell">
            <strong>{{ text.currency }} {{ formatPrice(order.price) }}</strong>
            <span>{{ formatDate(order.createdAt) }}</span>
          </div>

          <div class="table-cell action-cell">
            <el-button
              plain
              :disabled="order.userId === currentUserId || !canBargain(order)"
              @click="$emit('bargain', order)"
            >
              {{ text.bargain }}
            </el-button>
            <el-button type="primary" :disabled="order.userId === currentUserId" @click="$emit('buy', order)">
              {{ text.buy }}
            </el-button>
          </div>
        </article>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  compact: {
    type: Boolean,
    default: false
  },
  hideHeader: {
    type: Boolean,
    default: false
  },
  orders: {
    type: Array,
    default: () => []
  },
  selectedOrderId: {
    type: [Number, String],
    default: null
  },
  currentUserId: {
    type: [Number, String],
    default: null
  },
  text: {
    type: Object,
    required: true
  },
  canBargain: {
    type: Function,
    required: true
  },
  formatDate: {
    type: Function,
    required: true
  },
  formatPrice: {
    type: Function,
    required: true
  },
  getAvatarFallback: {
    type: Function,
    required: true
  },
  getCollectionTypeLabel: {
    type: Function,
    required: true
  },
  getExteriorText: {
    type: Function,
    required: true
  },
  getItemExterior: {
    type: Function,
    required: true
  },
  getItemIcon: {
    type: Function,
    required: true
  },
  getItemName: {
    type: Function,
    required: true
  },
  getOrderStatus: {
    type: Function,
    required: true
  },
  getWearDisplay: {
    type: Function,
    required: true
  },
  getWearPercent: {
    type: Function,
    required: true
  },
  hasWearVisual: {
    type: Function,
    required: true
  }
})

defineEmits(['select-order', 'bargain', 'buy'])

const filterState = reactive({
  exterior: '',
  paintSeed: '',
  minPrice: '',
  maxPrice: '',
  sort: 'default'
})

const popoverState = reactive({
  exterior: false,
  paintSeed: false,
  price: false,
  sort: false
})

const paintSeedDraft = ref('')
const priceDraft = reactive({
  min: '',
  max: ''
})
const filterPopoverStyle = {
  padding: '0',
  border: '1px solid #2f3448',
  borderRadius: '4px',
  background: '#2c3144',
  color: '#f8fafc',
  overflow: 'hidden',
  boxShadow: '0 18px 34px rgba(2, 6, 23, 0.5)'
}

const getOrderDisplayModel = (order) => getItemDisplayModel(order)
const shouldShowWearInfo = (order) => getOrderDisplayModel(order).showWearModule
const getOrderSubtitle = (order) => getOrderDisplayModel(order).subtitle
const getOrderPrimaryBadgeText = (order) => getOrderDisplayModel(order).primaryBadge.text
const getOrderPrimaryBadgeClass = (order) => {
  const badge = getOrderDisplayModel(order).primaryBadge
  if (!badge) return ''
  if (badge.kind === 'quality') return `status-pill-quality-${badge.code}`
  if (badge.kind === 'category') return `status-pill-category-${badge.code || 'other'}`
  return `status-pill-exterior-${badge.code || 'UN'}`
}
const getOrderSecondaryBadgeText = (order) => getOrderDisplayModel(order).secondaryBadge?.text || ''
const getOrderSecondaryBadgeClass = (order) => {
  const badge = getOrderDisplayModel(order).secondaryBadge
  return badge ? `status-pill-type-${badge.code}` : ''
}

const normalizeText = (value) => String(value || '').trim()
const getOrderWearValue = (order) => {
  const candidates = [
    order?.inventory?.paintWear,
    order?.inventory?.paintwear,
    order?.paintWear,
    getOrderDisplayModel(order).wearValue
  ]

  for (const value of candidates) {
    const numeric = Number(value)
    if (Number.isFinite(numeric)) return numeric
  }
  return null
}
const getOrderPaintSeed = (order) => {
  const seed = order?.inventory?.paintSeed ?? order?.inventory?.paint_seed ?? order?.paintSeed
  return seed === null || seed === undefined || seed === '' ? '' : String(seed)
}
const uniqueOptions = (items) => {
  const seen = new Set()
  return items.filter((item) => {
    if (!item?.value || seen.has(item.value)) return false
    seen.add(item.value)
    return true
  })
}

const EXTERIOR_RANGE_LABELS = {
  FN: '0.00-0.07',
  MW: '0.07-0.15',
  FT: '0.15-0.38',
  WW: '0.38-0.45',
  BS: '0.45-1.00'
}
const EXTERIOR_ORDER = ['FN', 'MW', 'FT', 'WW', 'BS']

const exteriorOptions = computed(() => {
  const mapped = uniqueOptions(props.orders
    .map((order) => {
      const model = getOrderDisplayModel(order)
      if (!model.showWearModule || !model.filterExterior || model.filterExterior === 'NoPaint') return null
      return {
        value: model.filterExterior,
        label: EXTERIOR_RANGE_LABELS[model.filterExterior] || model.exteriorText
      }
    })
    .filter(Boolean))

  return mapped.sort((left, right) => EXTERIOR_ORDER.indexOf(left.value) - EXTERIOR_ORDER.indexOf(right.value))
})

const sortOptions = [
  { value: 'default', label: '默认' },
  { value: 'timeDesc', label: '最新上架' },
  { value: 'priceAsc', label: '价格 ↑' },
  { value: 'priceDesc', label: '价格 ↓' },
  { value: 'wearAsc', label: '磨损 ↑' },
  { value: 'wearDesc', label: '磨损 ↓' }
]

const exteriorTriggerLabel = computed(() => {
  const current = exteriorOptions.value.find((option) => option.value === filterState.exterior)
  return current?.label || '磨损区间'
})

const paintSeedTriggerLabel = computed(() => (
  filterState.paintSeed || '图案模板'
))

const sortTriggerLabel = computed(() => (
  sortOptions.find((option) => option.value === filterState.sort)?.label || '默认'
))

const hasPriceFilter = computed(() => !!filterState.minPrice || !!filterState.maxPrice)
const priceTriggerMinLabel = computed(() => filterState.minPrice || '最低价')
const priceTriggerMaxLabel = computed(() => filterState.maxPrice || '最高价')

const normalizeNumericInput = (value) => {
  if (value === null || value === undefined || value === '') return ''
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric >= 0 ? numeric.toFixed(2).replace(/\.00$/, '') : ''
}

const selectExterior = (value) => {
  filterState.exterior = value
  popoverState.exterior = false
}

const syncPaintSeedDraft = () => {
  paintSeedDraft.value = filterState.paintSeed
}

const applyPaintSeed = () => {
  filterState.paintSeed = normalizeText(paintSeedDraft.value)
  popoverState.paintSeed = false
}

const syncPriceDraft = () => {
  priceDraft.min = filterState.minPrice
  priceDraft.max = filterState.maxPrice
}

const applyPriceRange = () => {
  filterState.minPrice = normalizeNumericInput(priceDraft.min)
  filterState.maxPrice = normalizeNumericInput(priceDraft.max)
  popoverState.price = false
}

const clearPriceRange = () => {
  priceDraft.min = ''
  priceDraft.max = ''
  filterState.minPrice = ''
  filterState.maxPrice = ''
  popoverState.price = false
}

const selectSort = (value) => {
  filterState.sort = value
  popoverState.sort = false
}

const matchesPriceRange = (order) => {
  const price = Number(order?.price)
  if (!Number.isFinite(price)) return false

  const minPrice = Number(filterState.minPrice)
  const maxPrice = Number(filterState.maxPrice)

  if (Number.isFinite(minPrice) && minPrice > 0 && price < minPrice) return false
  if (Number.isFinite(maxPrice) && maxPrice > 0 && price > maxPrice) return false
  return true
}

const sortOrders = (orders) => [...orders].sort((left, right) => {
  if (filterState.sort === 'default') return 0
  if (filterState.sort === 'priceDesc') return Number(right?.price || 0) - Number(left?.price || 0)
  if (filterState.sort === 'wearAsc' || filterState.sort === 'wearDesc') {
    const leftWear = getOrderWearValue(left)
    const rightWear = getOrderWearValue(right)
    const leftValue = leftWear === null ? Number.POSITIVE_INFINITY : leftWear
    const rightValue = rightWear === null ? Number.POSITIVE_INFINITY : rightWear
    return filterState.sort === 'wearAsc' ? leftValue - rightValue : rightValue - leftValue
  }
  if (filterState.sort === 'timeDesc') {
    return new Date(right?.createdAt || 0).getTime() - new Date(left?.createdAt || 0).getTime()
  }
  return Number(left?.price || 0) - Number(right?.price || 0)
})

const displayOrders = computed(() => sortOrders(props.orders.filter((order) => {
  if (filterState.exterior && getOrderDisplayModel(order).filterExterior !== filterState.exterior) return false
  if (filterState.paintSeed && getOrderPaintSeed(order) !== filterState.paintSeed) return false
  if (!matchesPriceRange(order)) return false
  return true
})))

const hasActiveFilters = computed(() => (
  !!filterState.exterior ||
  !!filterState.paintSeed ||
  !!filterState.minPrice ||
  !!filterState.maxPrice ||
  filterState.sort !== 'default'
))

const resetFilters = () => {
  filterState.exterior = ''
  filterState.paintSeed = ''
  filterState.minPrice = ''
  filterState.maxPrice = ''
  filterState.sort = 'default'
  paintSeedDraft.value = ''
  priceDraft.min = ''
  priceDraft.max = ''
}
</script>

<style scoped>
.sell-section {
  margin-top: 18px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(180deg, #181d27 0%, #141822 100%);
  border-bottom: 1px solid rgba(96, 165, 250, 0.18);
}

.section-tab {
  position: relative;
  color: rgba(226, 232, 240, 0.74);
  font-size: 18px;
  font-weight: 700;
}

.section-tab.active {
  color: #ffffff;
}

.section-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -18px;
  width: 100%;
  height: 3px;
  border-radius: 999px;
  background: #60a5fa;
}

.section-count {
  color: #cbd5e1;
  font-size: 14px;
}

.sell-section :deep(.el-empty) {
  background: #ffffff;
  padding: 54px 0;
}

.sell-table {
  background: #ffffff;
}

.sell-filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 16px;
  padding: 18px 24px;
  background: #10111c;
  border-bottom: 1px solid rgba(96, 165, 250, 0.9);
}

.sell-filter-main,
.sell-filter-side {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.sell-filter-side {
  margin-left: auto;
  justify-content: flex-end;
}

.filter-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  position: relative;
  min-width: 118px;
  height: 42px;
  padding: 0 34px 0 14px;
  border: 1px solid rgba(84, 94, 121, 0.72);
  background: #2f3448;
  color: rgba(226, 232, 240, 0.92);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  outline: none;
  transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.filter-trigger:hover {
  background: #353b52;
  border-color: rgba(114, 129, 170, 0.82);
}

.filter-trigger:focus,
.filter-trigger:focus-visible,
.filter-reset:focus,
.filter-reset:focus-visible,
.seed-search-button:focus,
.seed-search-button:focus-visible,
.panel-action:focus,
.panel-action:focus-visible {
  outline: none;
  box-shadow: none;
}

.filter-trigger.active,
.filter-trigger.selected {
  border-color: rgba(96, 165, 250, 0.6);
}

.filter-trigger::after {
  content: '';
  position: absolute;
  right: 10px;
  bottom: 8px;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-bottom: 6px solid rgba(148, 163, 184, 0.88);
}

.filter-trigger.active::after,
.filter-trigger.selected::after {
  border-bottom-color: #f4b319;
}

.price-trigger {
  min-width: 252px;
  justify-content: flex-start;
}

.price-trigger-currency,
.price-trigger em {
  color: rgba(203, 213, 225, 0.78);
  font-style: normal;
}

.sort-trigger {
  min-width: 110px;
}

.filter-reset {
  height: 42px;
  border: 1px solid rgba(84, 94, 121, 0.72);
  padding: 0 16px;
  background: rgba(96, 165, 250, 0.14);
  color: #dbeafe;
  cursor: pointer;
  font-weight: 700;
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

.filter-reset:hover {
  background: rgba(96, 165, 250, 0.2);
  border-color: rgba(96, 165, 250, 0.5);
}

.filter-result {
  color: rgba(203, 213, 225, 0.72);
  font-size: 13px;
  white-space: nowrap;
}

:deep(.sell-filter-popper),
:deep(.sell-filter-popper.el-popover),
:deep(.sell-filter-popper.el-popper) {
  padding: 0;
  border: 1px solid #2f3448;
  border-radius: 4px;
  background: #2c3144 !important;
  color: #f8fafc !important;
  overflow: hidden;
  --el-bg-color-overlay: #2c3144;
  --el-popover-bg-color: #2c3144;
  --el-text-color-primary: #f8fafc;
  --el-text-color-regular: rgba(241, 245, 249, 0.96);
  box-shadow: 0 18px 34px rgba(2, 6, 23, 0.5);
}

:deep(.sell-filter-popper .el-popper__arrow) {
  display: none !important;
}

:deep(.sell-filter-popper .el-popper__arrow::before) {
  background: #2c3144 !important;
  border: 1px solid #2f3448;
}

.filter-menu {
  display: flex;
  flex-direction: column;
  background: #2c3144;
}

.filter-menu-item {
  min-height: 44px;
  border: 0;
  border-top: 1px solid rgba(84, 94, 121, 0.3);
  background: transparent;
  padding: 0 18px;
  color: rgba(241, 245, 249, 0.96);
  font-size: 15px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.filter-menu-item:first-child {
  border-top: 0;
}

.filter-menu-item:hover,
.filter-menu-item.active {
  background: rgba(255, 255, 255, 0.05);
}

.seed-panel {
  padding: 12px;
  background: #2c3144;
}

.seed-search-row {
  display: flex;
  align-items: center;
  height: 40px;
  border: 1px solid rgba(84, 94, 121, 0.5);
  background: #3a425b;
}

.seed-search-row input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: none;
  background: transparent;
  padding: 0 12px;
  color: #f8fafc;
  font-size: 14px;
}

.seed-search-row input::placeholder,
.price-panel-row input::placeholder {
  color: rgba(203, 213, 225, 0.58);
}

.seed-search-button {
  width: 42px;
  height: 100%;
  border: 0;
  background: #465374;
  color: #dbeafe;
  cursor: pointer;
}

.price-panel {
  padding: 14px;
  background: #2c3144;
}

.price-panel-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(226, 232, 240, 0.82);
}

.price-panel-row span,
.price-panel-row em {
  white-space: nowrap;
  font-style: normal;
}

.price-panel-row input {
  width: 94px;
  height: 40px;
  border: 1px solid rgba(84, 94, 121, 0.5);
  outline: none;
  background: transparent;
  padding: 0 12px;
  color: #f8fafc;
  font-size: 14px;
}

.price-panel-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.panel-action {
  flex: 1;
  height: 40px;
  border: 0;
  background: #5b6580;
  color: #f8fafc;
  font-size: 14px;
  cursor: pointer;
}

.panel-action.primary {
  background: #587fd6;
}

.sell-table-head,
.sell-row {
  display: grid;
  grid-template-columns: 150px minmax(220px, 1fr) minmax(190px, 0.9fr) 128px 168px;
  gap: 14px;
  align-items: center;
  padding: 0 20px;
}

.sell-table-head {
  min-height: 56px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.sell-row {
  min-height: 132px;
  border-bottom: 1px solid #e5e7eb;
  transition: background-color 0.2s ease;
}

.sell-row:hover {
  background: #f8fafc;
}

.sell-row.active {
  background: #fffaf0;
}

.table-cell {
  min-width: 0;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-thumb {
  width: 110px;
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef2f7 100%);
}

.item-thumb img {
  max-width: 92px;
  max-height: 60px;
  object-fit: contain;
}

.item-copy {
  display: none;
  min-width: 0;
}

.item-copy strong {
  display: block;
  margin-bottom: 6px;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.4;
}

.item-copy span {
  color: #64748b;
  font-size: 13px;
}

.wear-cell {
  cursor: pointer;
}

.wear-value {
  margin: 0 0 12px;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.wear-bar {
  position: relative;
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, #22c55e 0%, #facc15 45%, #ef4444 100%);
}

.wear-marker {
  position: absolute;
  top: -5px;
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 10px solid #475569;
  transform: translateX(-50%);
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-row.compact {
  margin-top: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.status-pill.warn {
  background: rgba(248, 113, 113, 0.12);
  color: #dc2626;
}

.status-pill.current {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}

.status-pill-type {
  background: rgba(15, 23, 42, 0.94);
  color: #ff8f1f;
}

.status-pill-type-Souvenir {
  color: #fbbf24;
}

.status-pill-type-Star {
  color: #ffffff;
}

.status-pill-type-StarStatTrak {
  color: #ffb347;
}

.status-pill-exterior-FN {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-pill-exterior-MW {
  background: rgba(132, 204, 22, 0.14);
  color: #65a30d;
}

.status-pill-exterior-FT {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.status-pill-exterior-WW {
  background: rgba(100, 116, 139, 0.16);
  color: #475569;
}

.status-pill-exterior-BS {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.status-pill-quality-contraband {
  background: rgba(245, 223, 77, 0.26);
  color: #854d0e;
}

.status-pill-quality-covert {
  background: rgba(244, 63, 94, 0.14);
  color: #be123c;
}

.status-pill-quality-classified {
  background: rgba(192, 38, 211, 0.14);
  color: #a21caf;
}

.status-pill-quality-restricted {
  background: rgba(139, 92, 246, 0.14);
  color: #7c3aed;
}

.status-pill-quality-mil-spec {
  background: rgba(59, 130, 246, 0.14);
  color: #2563eb;
}

.status-pill-quality-industrial {
  background: rgba(96, 165, 250, 0.18);
  color: #2563eb;
}

.status-pill-quality-consumer {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.status-pill-quality-extraordinary {
  background: rgba(249, 115, 22, 0.16);
  color: #c2410c;
}

.status-pill-quality-exotic {
  background: rgba(168, 85, 247, 0.16);
  color: #9333ea;
}

.status-pill-quality-remarkable {
  background: rgba(236, 72, 153, 0.14);
  color: #db2777;
}

.status-pill-quality-high-grade {
  background: rgba(79, 142, 247, 0.16);
  color: #2563eb;
}

.status-pill-quality-normal-grade {
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
}

.status-pill-quality-agent-grade,
.status-pill-category-agent {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-pill-category-sticker,
.status-pill-category-music {
  background: rgba(37, 99, 235, 0.14);
  color: #1d4ed8;
}

.status-pill-category-graffiti,
.status-pill-category-collectible {
  background: rgba(124, 58, 237, 0.14);
  color: #7c3aed;
}

.status-pill-category-charm,
.status-pill-category-tool {
  background: rgba(15, 118, 110, 0.14);
  color: #0f766e;
}

.status-pill-category-case {
  background: rgba(71, 85, 105, 0.16);
  color: #475569;
}

.status-pill-category-pass {
  background: rgba(124, 45, 18, 0.12);
  color: #9a3412;
}

.seller-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-info strong {
  display: block;
  color: #334155;
  font-size: 15px;
}

.price-cell strong {
  display: block;
  margin-bottom: 8px;
  color: #f59e0b;
  font-size: 26px;
  line-height: 1;
}

.price-cell span {
  color: #94a3b8;
  font-size: 13px;
}

.action-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.action-cell :deep(.el-button) {
  width: 100%;
  height: 34px;
  margin-left: 0;
  border-radius: 10px;
  font-weight: 700;
}

.sell-section.compact .sell-table-head {
  display: none;
}

.sell-section.compact .sell-row {
  grid-template-columns: 1fr;
  gap: 14px;
  padding: 18px 20px;
}

.sell-section.compact .item-copy {
  display: block;
}

.sell-section.compact .item-thumb {
  width: 120px;
  height: 86px;
}

.sell-section.compact .item-thumb img {
  max-width: 98px;
  max-height: 64px;
}

.sell-section.compact .action-cell {
  flex-direction: row;
}

@media (max-width: 1120px) {
  .sell-filter-bar {
    padding: 14px 18px;
  }

  .sell-filter-main,
  .sell-filter-side {
    width: 100%;
  }

  .sell-filter-side {
    margin-left: 0;
    justify-content: flex-start;
  }

  .filter-result {
    margin-left: auto;
  }

  .sell-table-head {
    display: none;
  }

  .sell-row {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 18px 20px;
  }

  .item-copy {
    display: block;
  }

  .item-thumb {
    width: 120px;
    height: 86px;
  }

  .item-thumb img {
    max-width: 98px;
    max-height: 64px;
  }

  .action-cell {
    flex-direction: row;
  }
}

@media (max-width: 768px) {
  .section-toolbar {
    padding: 16px 18px;
  }

  .sell-row {
    padding: 16px;
  }

  .sell-filter-main,
  .sell-filter-side {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-trigger,
  .price-trigger,
  .sort-trigger,
  .filter-reset {
    width: 100%;
  }

  .item-cell {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-thumb {
    width: 100%;
    height: 110px;
  }

  .item-thumb img {
    max-width: 130px;
    max-height: 82px;
  }

  .action-cell {
    flex-direction: column;
  }
}
</style>
