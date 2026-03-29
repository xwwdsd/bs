<template>
  <div class="inventory-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>
      <div class="user-nav-links">
        <router-link to="/user/inventory" class="nav-link active">我的库存</router-link>
        <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
        <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        <div class="nav-user-section">
          <UserNavDropdown />
        </div>
      </div>
    </nav>

    <div class="page-shell">
      <section class="hero-panel">
        <div class="hero-stat">
          <span class="hero-label">库存总数</span>
          <strong class="hero-value">{{ inventory.length }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-label">预估总值</span>
          <strong class="hero-value price">¥ {{ totalValue.toFixed(2) }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-label">可出售</span>
          <strong class="hero-value">{{ sellableCount }}</strong>
        </div>
      </section>

      <section class="content-panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="Steam 库存" name="steam">
            <div class="toolbar">
              <div class="toolbar-left">
                <el-input
                  v-model="filters.search"
                  placeholder="搜索饰品名称"
                  clearable
                  class="dark-input search-input"
                />
                <el-select v-model="filters.rarity" placeholder="品质" clearable class="dark-select">
                  <el-option label="全部品质" value="" />
                  <el-option label="违禁" value="contraband" />
                  <el-option label="隐秘" value="covert" />
                  <el-option label="保密" value="classified" />
                  <el-option label="受限" value="restricted" />
                  <el-option label="军规级" value="mil-spec" />
                  <el-option label="消费级" value="consumer" />
                  <el-option label="非凡" value="extraordinary" />
                </el-select>
                <el-select v-model="filters.type" placeholder="类型" clearable class="dark-select">
                  <el-option label="全部类型" value="" />
                  <el-option label="普通" value="normal" />
                  <el-option label="纪念品" value="souvenir" />
                  <el-option label="StatTrak" value="stattrak" />
                  <el-option label="★" value="star" />
                  <el-option label="★ StatTrak" value="star_stattrak" />
                </el-select>
                <el-select v-model="filters.exterior" placeholder="外观" clearable class="dark-select">
                  <el-option label="全部外观" value="" />
                  <el-option label="崭新出厂" value="FN" />
                  <el-option label="略有磨损" value="MW" />
                  <el-option label="久经沙场" value="FT" />
                  <el-option label="破损不堪" value="WW" />
                  <el-option label="战痕累累" value="BS" />
                </el-select>
                <el-select v-model="filters.sellableOnly" class="dark-select">
                  <el-option label="全部物品" :value="false" />
                  <el-option label="仅可出售" :value="true" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <el-button class="dark-btn" @click="sortBy = sortBy === 'time' ? 'price' : 'time'">
                  {{ sortBy === 'time' ? '按时间' : '按价格' }}
                </el-button>
                <el-button class="dark-btn" :loading="syncing" @click="handleSync">同步库存</el-button>
              </div>
            </div>

            <div class="summary-line">共 {{ filteredInventory.length }} 件物品，灰色按钮代表当前不可出售。</div>

            <div v-loading="loading" class="inventory-grid" element-loading-background="rgba(23,26,33,0.75)">
              <div
                v-for="item in filteredInventory"
                :key="item.id || item.assetId"
                class="item-card"
                :class="{ locked: !canSellItem(item), active: hoveredItem?.assetId === item.assetId }"
                @mouseenter="hoveredItem = item"
                @mouseleave="hoveredItem = null"
              >
                <div class="rarity-bar" :style="{ background: getRarityColor(getInventoryRarity(item)) }"></div>
                <div class="card-header">
                  <span class="wear-chip" v-if="item.paintWear">{{ formatWear(item.paintWear, 3) }}</span>
                  <span class="status-chip" :class="{ disabled: !canSellItem(item) }">
                    {{ canSellItem(item) ? '可出售' : '不可出售' }}
                  </span>
                </div>
                <div class="card-image">
                  <img :src="item.iconUrl || '/default-item.png'" :alt="item.name" />
                </div>
                <div class="card-body">
                  <div class="item-name" :title="item.name">{{ item.name }}</div>
                  <div class="item-meta">
                    <span>{{ getDisplayRarityText(getInventoryRarity(item)) || '未分类' }}</span>
                    <span>{{ item.exterior || '-' }}</span>
                  </div>
                  <div class="item-price">¥ {{ formatPrice(item.marketPrice) }}</div>
                  <div class="item-reason">{{ getSellDisabledReason(item) }}</div>
                </div>
                <div class="card-actions">
                  <el-button size="small" type="primary" :disabled="!canSellItem(item)" @click.stop="handleSell(item)">出售</el-button>
                  <el-button size="small" class="dark-btn" @click.stop="handleInspect(item)">检视</el-button>
                </div>
              </div>
            </div>

            <div v-if="!loading && filteredInventory.length === 0" class="empty-state">
              <el-empty description="没有符合条件的库存物品" />
            </div>

            <div v-if="hoveredItem" class="detail-panel">
              <div class="detail-top">
                <img :src="hoveredItem.iconUrl || '/default-item.png'" :alt="hoveredItem.name" />
                <div>
                  <h3>{{ hoveredItem.name }}</h3>
                  <p>{{ getSellDisabledReason(hoveredItem) }}</p>
                </div>
              </div>
              <div class="detail-grid">
                <div class="detail-row">
                  <span>磨损</span>
                  <strong>{{ formatWear(hoveredItem.paintWear, 10) }}</strong>
                </div>
                <div class="detail-row">
                  <span>图案模板</span>
                  <strong>{{ hoveredItem.paintSeed ?? '-' }}</strong>
                </div>
                <div class="detail-row">
                  <span>皮肤编号</span>
                  <strong>{{ hoveredItem.paintIndex ?? '-' }}</strong>
                </div>
                <div class="detail-row">
                  <span>外观</span>
                  <strong>{{ hoveredItem.exterior || '-' }}</strong>
                </div>
              </div>
              <div class="detail-description" v-if="hoveredItem.description">{{ hoveredItem.description }}</div>
              <div v-if="hoveredItem.stickers?.length" class="sticker-list">
                <div class="sticker-title">附加物</div>
                <div v-for="(sticker, index) in hoveredItem.stickers" :key="index" class="sticker-item">
                  <img v-if="sticker.iconUrl" :src="sticker.iconUrl" :alt="sticker.name" />
                  <span>{{ sticker.name }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="购买记录" name="history">
            <div v-loading="historyLoading" class="history-panel" element-loading-background="rgba(23,26,33,0.75)">
              <el-table :data="buyHistory" class="dark-table" :header-cell-style="{ background: '#252834', color: '#aeb8c5' }">
                <el-table-column prop="id" label="订单号" width="180" />
                <el-table-column label="物品">
                  <template #default="{ row }">
                    {{ row.item?.nameCn || row.item?.name || '未知物品' }}
                  </template>
                </el-table-column>
                <el-table-column prop="price" label="价格">
                  <template #default="{ row }">¥ {{ formatPrice(row.price) }}</template>
                </el-table-column>
                <el-table-column prop="createdAt" label="购买时间" />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>

    <el-dialog v-model="sellDialogVisible" title="出售饰品" width="420px" class="dark-dialog">
      <div v-if="currentSellItem" class="sell-dialog">
        <div class="sell-item">
          <img :src="currentSellItem.iconUrl || '/default-item.png'" :alt="currentSellItem.name" />
          <div>
            <div class="sell-item-name">{{ currentSellItem.name }}</div>
            <div class="sell-item-tip">{{ getSellDisabledReason(currentSellItem) }}</div>
          </div>
        </div>
        <el-input-number v-model="sellPrice" :min="0.01" :step="0.1" :precision="2" class="price-input" />
        <div class="fee-line">
          <span>手续费 5%</span>
          <span>¥ {{ (sellPrice * 0.05).toFixed(2) }}</span>
        </div>
        <div class="fee-line total">
          <span>预计到账</span>
          <span>¥ {{ (sellPrice * 0.95).toFixed(2) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="sellDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sellLoading" @click="confirmSell">确认上架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventory, syncInventory } from '@/api/inventory'
import { createSellOrder } from '@/api/sellOrder'
import UserNavDropdown from '@/components/UserNavDropdown.vue'
import request from '@/utils/request'

const activeTab = ref('steam')
const loading = ref(false)
const syncing = ref(false)
const historyLoading = ref(false)
const inventory = ref([])
const buyHistory = ref([])
const hoveredItem = ref(null)

const sellDialogVisible = ref(false)
const currentSellItem = ref(null)
const sellPrice = ref(0)
const sellLoading = ref(false)

const sortBy = ref('time')
const filters = ref({
  search: '',
  rarity: '',
  type: '',
  exterior: '',
  sellableOnly: false
})

const totalValue = computed(() =>
  inventory.value.reduce((sum, item) => sum + (Number(item.marketPrice) || 0), 0)
)

const sellableCount = computed(() => inventory.value.filter((item) => canSellItem(item)).length)

const filteredInventory = computed(() => {
  const keyword = filters.value.search.trim().toLowerCase()
  const result = inventory.value.filter((item) => {
    if (keyword && !String(item.name || '').toLowerCase().includes(keyword)) {
      return false
    }
    if (filters.value.rarity && getInventoryRarity(item) !== filters.value.rarity) {
      return false
    }
    if (filters.value.type && getInventoryType(item) !== filters.value.type) {
      return false
    }
    if (filters.value.exterior && item.exterior !== filters.value.exterior) {
      return false
    }
    if (filters.value.sellableOnly && !canSellItem(item)) {
      return false
    }
    return true
  })

  result.sort((a, b) => {
    if (sortBy.value === 'price') {
      return (Number(b.marketPrice) || 0) - (Number(a.marketPrice) || 0)
    }
    return (b.id || 0) - (a.id || 0)
  })

  return result
})

const getInventoryRarity = (item) => item?.rarity || item?.item?.quality || item?.item?.rarity || ''

const getInventoryType = (item) => item?.type || detectTypeFromName(item?.name || '')

const detectTypeFromName = (name) => {
  const lower = String(name).toLowerCase()
  if (lower.includes('stattrak')) return name.includes('★') ? 'star_stattrak' : 'stattrak'
  if (lower.includes('souvenir')) return 'souvenir'
  if (name.includes('★')) return 'star'
  return 'normal'
}

const getDisplayRarityText = (rarity) => {
  const map = {
    contraband: '违禁',
    covert: '隐秘',
    classified: '保密',
    restricted: '受限',
    'mil-spec': '军规级',
    consumer: '消费级',
    extraordinary: '非凡',
    remarkable: '奇异'
  }
  return map[rarity] || rarity || ''
}

const getRarityColor = (rarity) => {
  const colors = {
    contraband: '#d5aa3b',
    covert: '#eb4b4b',
    classified: '#d32ce6',
    restricted: '#8847ff',
    'mil-spec': '#4b69ff',
    consumer: '#9db2c8',
    extraordinary: '#ff7a2f',
    remarkable: '#8d6dff'
  }
  return colors[rarity] || '#60738a'
}

const formatPrice = (value) => (Number(value) || 0).toFixed(2)

const formatWear = (value, digits = 3) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(digits) : '-'
}

const canSellItem = (item) => item?.isMarketable === 1 && (item?.status ?? 0) === 0

const getSellDisabledReason = (item) => {
  if (!item) return '-'
  if (item.marketableReason && item.marketableReason !== '可出售') {
    return item.marketableReason
  }
  if ((item.status ?? 0) !== 0) {
    const statusMap = {
      1: '该物品已售出',
      2: '该物品正在交易中',
      3: '该物品已在售'
    }
    return statusMap[item.status] || '该物品当前不可出售'
  }
  if (item.isMarketable !== 1) {
    return '该物品当前不可上架出售'
  }
  return '可出售'
}

const fetchInventory = async (syncFromSteam = false) => {
  loading.value = true
  try {
    inventory.value = (syncFromSteam ? await syncInventory() : await getInventory()) || []
  } catch (error) {
    ElMessage.error(error?.message || '获取库存失败')
  } finally {
    loading.value = false
  }
}

const fetchHistory = async () => {
  historyLoading.value = true
  try {
    buyHistory.value = (await request.get('/v1/order/my/buy')) || []
  } catch (error) {
    ElMessage.error('获取购买记录失败')
  } finally {
    historyLoading.value = false
  }
}

const handleSync = async () => {
  syncing.value = true
  try {
    inventory.value = (await syncInventory()) || []
    ElMessage.success('库存同步成功')
  } catch (error) {
    ElMessage.error(error?.message || '库存同步失败')
  } finally {
    syncing.value = false
  }
}

const handleInspect = (item) => {
  if (!item?.inspectUrl) {
    ElMessage.warning('当前物品没有可用的检视链接')
    return
  }
  window.open(item.inspectUrl, '_blank', 'noopener,noreferrer')
}

const handleSell = (item) => {
  if (!canSellItem(item)) {
    ElMessage.warning(getSellDisabledReason(item))
    return
  }
  currentSellItem.value = item
  sellPrice.value = Number(item.marketPrice) || 0.01
  sellDialogVisible.value = true
}

const confirmSell = async () => {
  if (!currentSellItem.value) return
  if (sellPrice.value <= 0) {
    ElMessage.warning('请输入有效价格')
    return
  }

  sellLoading.value = true
  try {
    await createSellOrder({
      assetId: currentSellItem.value.assetId,
      price: Number(sellPrice.value)
    })
    ElMessage.success('上架成功')
    sellDialogVisible.value = false
    await fetchInventory(false)
  } catch (error) {
    ElMessage.error(error?.message || '上架失败')
  } finally {
    sellLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'history') {
    fetchHistory()
  }
})

onMounted(async () => {
  await fetchInventory(false)
})
</script>

<style scoped>
.inventory-page {
  min-height: 100vh;
  background: #171a21;
  color: #c7d5e0;
}

.navbar-dark {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 64px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  position: sticky;
  top: 0;
  z-index: 30;
}

.nav-links-left,
.user-nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-link {
  color: #8d99a7;
  text-decoration: none;
}

.nav-link.active,
.nav-link:hover,
.router-link-active {
  color: #fff;
}

.page-shell {
  width: min(1320px, calc(100vw - 40px));
  margin: 24px auto 40px;
}

.hero-panel,
.content-panel,
.detail-panel {
  background: #1c1f28;
  border: 1px solid #2a2e3b;
  border-radius: 16px;
}

.hero-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 18px;
}

.hero-stat {
  padding: 16px 18px;
  background: linear-gradient(135deg, #222834, #1a212d);
  border-radius: 12px;
}

.hero-label {
  display: block;
  color: #92a0af;
  margin-bottom: 8px;
  font-size: 13px;
}

.hero-value {
  font-size: 28px;
}

.hero-value.price {
  color: #f0bf52;
}

.content-panel {
  padding: 18px 20px 22px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  align-items: center;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  width: 220px;
  flex: 0 0 220px;
}

.toolbar-left {
  flex: 1;
  min-width: 0;
  flex-wrap: nowrap;
}

.toolbar-right {
  flex: 0 0 auto;
  white-space: nowrap;
}

:deep(.dark-select) {
  width: 150px;
  flex: 0 0 150px;
}

.summary-line {
  color: #8d99a7;
  margin-bottom: 16px;
  font-size: 13px;
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.item-card {
  position: relative;
  background: #232a36;
  border: 1px solid #313a48;
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.item-card:hover,
.item-card.active {
  transform: translateY(-2px);
  border-color: #5697f5;
}

.item-card.locked {
  opacity: 0.92;
}

.rarity-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 5px;
}

.card-header,
.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px 0;
}

.wear-chip,
.status-chip {
  background: #171d26;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 12px;
}

.status-chip.disabled {
  color: #f3a44c;
}

.card-image {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.card-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.card-body {
  padding: 0 14px 12px;
}

.item-name {
  color: #fff;
  font-weight: 600;
  line-height: 1.4;
  min-height: 40px;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  color: #93a2b1;
  font-size: 12px;
  margin: 8px 0;
}

.item-price {
  color: #f0bf52;
  font-size: 18px;
  font-weight: 700;
}

.item-status,
.item-reason,
.detail-description {
  color: #92a0af;
  font-size: 12px;
  line-height: 1.5;
}

.card-actions {
  padding: 0 14px 14px;
}

.detail-panel {
  margin-top: 18px;
  padding: 18px;
}

.detail-top {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.detail-top img {
  width: 100px;
  height: 100px;
  object-fit: contain;
  background: #232a36;
  border-radius: 12px;
  padding: 8px;
}

.detail-top h3 {
  margin: 0 0 8px;
  color: #fff;
}

.detail-top p {
  margin: 0;
  color: #96a4b4;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
  margin-bottom: 14px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #232a36;
  border-radius: 10px;
}

.detail-row span {
  color: #96a4b4;
}

.detail-row strong {
  color: #fff;
}

.sticker-list {
  margin-top: 14px;
}

.sticker-title {
  margin-bottom: 10px;
  color: #fff;
}

.sticker-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #232a36;
  border-radius: 10px;
  margin-bottom: 8px;
}

.sticker-item img {
  width: 28px;
  height: 28px;
}

.history-panel {
  padding-top: 8px;
}

.sell-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sell-item {
  display: flex;
  gap: 14px;
  align-items: center;
}

.sell-item img {
  width: 72px;
  height: 72px;
  object-fit: contain;
  background: #232a36;
  border-radius: 10px;
  padding: 8px;
}

.sell-item-name {
  color: #fff;
  font-weight: 600;
  margin-bottom: 4px;
}

.sell-item-tip {
  color: #92a0af;
  font-size: 12px;
}

.price-input {
  width: 100%;
}

.fee-line {
  display: flex;
  justify-content: space-between;
  color: #96a4b4;
}

.fee-line.total {
  color: #f0bf52;
  font-weight: 700;
}

.empty-state {
  padding: 40px 0 16px;
}

:deep(.dark-input .el-input__wrapper),
:deep(.dark-select .el-input__wrapper) {
  background: #252d3a;
  box-shadow: none;
  border: 1px solid #344052;
}

:deep(.dark-input .el-input__inner),
:deep(.dark-select .el-input__inner),
:deep(.el-tabs__item),
:deep(.dark-table) {
  color: #c7d5e0;
}

:deep(.el-tabs__item.is-active) {
  color: #fff;
}

:deep(.dark-table) {
  --el-table-bg-color: #1c1f28;
  --el-table-tr-bg-color: #1c1f28;
  --el-table-border-color: #2a2e3b;
}

:deep(.dark-dialog .el-dialog) {
  background: #1c1f28;
}

@media (max-width: 900px) {
  .hero-panel,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-right {
    flex-wrap: wrap;
  }

  .search-input,
  :deep(.dark-select) {
    width: 100%;
    flex: 1 1 180px;
  }
}
</style>
