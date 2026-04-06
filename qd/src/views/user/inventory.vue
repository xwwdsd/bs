<template>
  <div class="inventory-page">
    <SiteHeader />

    <main class="page-main">
      <section class="summary-panel">
        <div class="summary-card">
          <span class="summary-label">库存总数</span>
          <strong class="summary-value">{{ inventory.length }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">预估总价</span>
          <strong class="summary-value price">¥ {{ totalValue.toFixed(2) }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">可出售数量</span>
          <strong class="summary-value">{{ sellableCount }}</strong>
        </div>
      </section>

      <section class="page-panel">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-yellow">我的库存</div>
          </div>
          <div class="toolbar-right compact">
            <el-button @click="sortBy = sortBy === 'time' ? 'price' : 'time'">
              {{ sortBy === 'time' ? '按时间排序' : '按价格排序' }}
            </el-button>
            <el-button :loading="syncing" type="primary" @click="handleRefresh">刷新</el-button>
            <el-button type="primary" :disabled="!hasSellableInView" @click="handleTopSell">出售</el-button>
          </div>
        </div>

        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="filters.search" placeholder="搜索饰品名称" clearable class="search-input" />
            <el-select v-model="filters.exterior" placeholder="外观" clearable class="toolbar-select">
              <el-option label="全部外观" value="" />
              <el-option label="崭新出厂" value="FN" />
              <el-option label="略有磨损" value="MW" />
              <el-option label="久经沙场" value="FT" />
              <el-option label="破损不堪" value="WW" />
              <el-option label="战痕累累" value="BS" />
            </el-select>
            <el-select v-model="filters.sellableOnly" class="toolbar-select">
              <el-option label="全部物品" :value="false" />
              <el-option label="仅可出售" :value="true" />
            </el-select>
          </div>
        </div>

        <div class="items-grid" v-loading="loading">
          <el-empty v-if="!filteredInventory.length && !loading" description="暂无库存数据" />

          <div
            v-for="item in filteredInventory"
            v-else
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
                    <span class="wear-tag" :class="`wear-tag-${getExteriorCode(item)}`">{{ getDisplayExterior(item) }}</span>
                    <span v-if="item.paintSeed !== null && item.paintSeed !== undefined" class="seed-tag">{{ item.paintSeed }}</span>
                    <span v-if="isStatTrak(item)" class="stattrak-tag">StatTrak™</span>
                  </div>
                  <img :src="item.iconUrl || item.item?.iconUrl || '/default-item.png'" :alt="item.name || '饰品'" />
                  <span class="note-tag">备注</span>
                  <div class="wear-panel">
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
              <div class="card-price-row">
                <div class="price-meta">
                  <p class="card-price">¥ {{ formatPrice(item.marketPrice) }}</p>
                  <span v-if="getDisplayAge(item)" class="card-time">{{ getDisplayAge(item) }}</span>
                </div>
              </div>
              <span v-if="!canSellItem(item)" class="disabled-reason">{{ getSellDisabledReason(item) }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>

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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createSellOrder } from '@/api/sellOrder'
import { getInventory, syncInventory } from '@/api/inventory'
import SiteHeader from '@/components/SiteHeader.vue'
import {
  EXTERIOR_RANGE_MAP as WEAR_RANGE_MAP,
  extractWearFromText as extractWearFromMetadataText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

const loading = ref(false)
const syncing = ref(false)
const inventory = ref([])
const sortBy = ref('time')
const sellDialogVisible = ref(false)
const currentSellItem = ref(null)
const sellPrice = ref(0)
const sellLoading = ref(false)
const selectedInventoryId = ref(null)

const filters = ref({
  search: '',
  exterior: '',
  sellableOnly: false
})

const totalValue = computed(() => inventory.value.reduce((sum, item) => sum + (Number(item.marketPrice) || 0), 0))
const sellableCount = computed(() => inventory.value.filter((item) => canSellItem(item)).length)

const normalizeRarity = (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  const map = {
    ancient: 'contraband',
    contraband: 'contraband',
    immortal: 'extraordinary',
    extraordinary: 'extraordinary',
    legendary: 'covert',
    covert: 'covert',
    mythical: 'classified',
    classified: 'classified',
    rare: 'restricted',
    restricted: 'restricted',
    uncommon: 'mil-spec',
    'mil-spec': 'mil-spec',
    industrial: 'industrial',
    common: 'consumer',
    consumer: 'consumer',
    remarkable: 'remarkable'
  }
  return map[normalized] || normalized
}

const inferExteriorFromText = (...values) => {
  for (const raw of values) {
    const value = String(raw || '').trim()
    const lower = value.toLowerCase()
    if (!value) continue
    if (value.includes('崭新出厂') || lower.includes('factory new')) return 'FN'
    if (value.includes('略有磨损') || lower.includes('minimal wear')) return 'MW'
    if (value.includes('久经沙场') || lower.includes('field-tested') || lower.includes('field tested')) return 'FT'
    if (value.includes('破损不堪') || lower.includes('well-worn') || lower.includes('well worn')) return 'WW'
    if (value.includes('战痕累累') || lower.includes('battle-scarred') || lower.includes('battle scarred')) return 'BS'
  }
  return ''
}

const normalizeExterior = (value, ...fallbackTexts) => {
  const normalized = String(value || '').trim()
  const map = {
    factory_new: 'FN',
    'factory new': 'FN',
    崭新出厂: 'FN',
    minimal_wear: 'MW',
    'minimal wear': 'MW',
    略有磨损: 'MW',
    'field-tested': 'FT',
    'field tested': 'FT',
    久经沙场: 'FT',
    'well-worn': 'WW',
    'well worn': 'WW',
    破损不堪: 'WW',
    'battle-scarred': 'BS',
    'battle scarred': 'BS',
    战痕累累: 'BS'
  }
  const mapped = map[normalized.toLowerCase()] || map[normalized]
  if (mapped) return mapped
  return inferExteriorFromText(...fallbackTexts) || normalized
}

const getInventoryRarity = (item) => normalizeRarity(item?.rarity || item?.item?.quality || item?.item?.rarity || '')

const getInventoryExterior = (item) =>
  resolveExteriorCode(getWearValue(item), item?.exterior, item?.name, item?.item?.nameCn, item?.item?.name)

const getDisplayRarityText = (rarity) => {
  const map = {
    contraband: '违禁',
    covert: '隐秘',
    classified: '保密',
    restricted: '受限',
    'mil-spec': '军规级',
    industrial: '工业级',
    consumer: '消费级',
    extraordinary: '非凡',
    remarkable: '奇异'
  }
  return map[rarity] || rarity || ''
}

const getDisplayExterior = (item) => {
  const map = {
    FN: '崭新出厂',
    MW: '略有磨损',
    FT: '久经沙场',
    WW: '破损不堪',
    BS: '战痕累累'
  }
  const exterior = getInventoryExterior(item)
  return map[exterior] || exterior || '未知外观'
}
const getExteriorCode = (item) => {
  const code = getInventoryExterior(item)
  return ['FN', 'MW', 'FT', 'WW', 'BS'].includes(code) ? code : 'UN'
}

const formatPrice = (value) => (Number(value) || 0).toFixed(2)
const formatWear = (value, digits = 3) => {
  if (value === null || value === undefined || value === '') return '-'
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(digits) : '-'
}
const extractWearFromText = (text) => {
  const source = String(text || '')
  if (!source) return null
  const cleaned = source.replace(/<[^>]+>/g, ' ')
  const match = cleaned.match(/(?:磨损|float|paint wear)\s*[:：]\s*([0-9]*\.?[0-9]+)/i)
  if (!match) return null
  const value = Number(match[1])
  return Number.isFinite(value) ? value : null
}

const getWearValue = (item) =>
  (() => {
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
  })()

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
const isStatTrak = (item) => {
  const name = `${item?.name || ''} ${item?.item?.name || ''} ${item?.item?.nameCn || ''}`.toLowerCase()
  return name.includes('stattrak')
}
const formatAge = (value) => {
  const timestamp = new Date(value || '').getTime()
  if (!Number.isFinite(timestamp) || timestamp <= 0) return ''
  const diffMs = Date.now() - timestamp
  if (diffMs < 0) return '刚刚'
  const totalHours = Math.floor(diffMs / (1000 * 60 * 60))
  if (totalHours < 1) return '刚刚'
  if (totalHours < 24) return `${totalHours}小时`
  const days = Math.floor(totalHours / 24)
  const remainHours = totalHours % 24
  if (days < 30) return remainHours ? `${days}天${remainHours}小时` : `${days}天`
  const months = Math.floor(days / 30)
  return `${months}个月`
}
const getDisplayAge = (item) => formatAge(item?.acquiredAt || item?.createdAt || item?.updatedAt)

const formatDetailValue = (value) => {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text ? text : '-'
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
  const fraudWarnings = Array.isArray(item?.fraudwarnings) ? item.fraudwarnings : Array.isArray(item?.fraudWarnings) ? item.fraudWarnings : []
  const extraText = [...ownerDescriptions, ...fraudWarnings].map((v) => String(v || '').replace(/<[^>]+>/g, ' ')).join(' ')

  if (hasExplicitTradeProtectionText(extraText)) return true
  if (!reason) return false

  // Old backend snapshots may return this generic sentence for many items.
  // Only trust it when Steam raw metadata also carries an explicit protection hint.
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

const filteredInventory = computed(() => {
  const keyword = filters.value.search.trim().toLowerCase()
  const result = inventory.value.filter((item) => {
    const name = String(item.name || item.item?.nameCn || item.item?.name || '').toLowerCase()
    const exterior = getInventoryExterior(item)
    const matchKeyword = !keyword || name.includes(keyword)
    const matchExterior = !filters.value.exterior || exterior === filters.value.exterior
    const matchSellable = !filters.value.sellableOnly || canSellItem(item)
    return matchKeyword && matchExterior && matchSellable
  })

  return [...result].sort((a, b) => {
    if (sortBy.value === 'price') {
      return Number(b.marketPrice || 0) - Number(a.marketPrice || 0)
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

const fetchInventory = async () => {
  loading.value = true
  try {
    const payload = await getInventory()
    inventory.value = normalizeInventory(payload)
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
  sellPrice.value = Number(item.marketPrice || item.item?.buffPrice || 0) || 0.01
  sellDialogVisible.value = true
}

const confirmSell = async () => {
  if (!currentSellItem.value) return
  sellLoading.value = true
  try {
    await createSellOrder({
      inventoryId: currentSellItem.value.id,
      price: sellPrice.value
    })
    ElMessage.success('出售订单创建成功')
    sellDialogVisible.value = false
    await fetchInventory()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '创建出售订单失败')
  } finally {
    sellLoading.value = false
  }
}

onMounted(() => {
  fetchInventory()
})
</script>

<style scoped>
.inventory-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 36px;
}

.summary-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.summary-card,
.page-panel {
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
}

.summary-card {
  padding: 24px;
}

.summary-label {
  display: block;
  margin-bottom: 12px;
  color: #6b7280;
}

.summary-value {
  color: #111827;
  font-size: 32px;
}

.summary-value.price {
  color: #f0a10a;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ececec;
  border-bottom: 1px solid #e2e2e2;
}

.tab-group {
  display: flex;
  align-items: center;
  gap: 36px;
}

.tab-item {
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 0 22px;
  color: #666f7d;
  font-size: 16px;
  font-weight: 600;
}

.tab-item.active-yellow {
  background: #f0b321;
  color: #fff;
}

.toolbar {
  padding: 22px 24px 0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right.compact {
  padding-right: 24px;
}

.search-input {
  width: 280px;
}

.toolbar-select {
  width: 160px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
  padding: 24px 24px 28px;
}

.item-card {
  overflow: hidden;
  border: 1px solid #d7dce4;
  border-radius: 3px;
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

.wear-tag-UN {
  background: #5d8a42;
}

.seed-tag {
  padding: 4px 7px;
  background: rgba(106, 112, 123, 0.92);
  color: #d5d8de;
  font-size: 12px;
  line-height: 1;
}

.stattrak-tag {
  padding: 4px 8px;
  background: rgba(15, 21, 34, 0.95);
  color: #ff8f1f;
  font-size: 14px;
  line-height: 1;
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
  padding: 10px 12px 10px;
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
  align-items: baseline;
  gap: 8px;
}

.card-price {
  margin: 0;
  color: #ed9b00;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.card-time {
  color: #a9afbb;
  font-size: 12px;
  white-space: nowrap;
}

.disabled-reason {
  display: block;
  margin-top: 6px;
  color: #ef4444;
  font-size: 12px;
  line-height: 1.3;
}

.dialog-name {
  color: #111827;
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

@media (max-width: 1024px) {
  .summary-panel {
    grid-template-columns: 1fr;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-header {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
