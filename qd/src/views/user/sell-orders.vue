<template>
  <div class="orders-page">
    <SiteHeader active="market" />

    <main class="main-content">
      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-yellow">我的出售</div>
          </div>
          <div class="section-more" @click="$router.push('/user/inventory')">
            发布出售
            <el-icon><Plus /></el-icon>
          </div>
        </div>

        <div class="toolbar">
          <el-radio-group v-model="statusFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="1">在售</el-radio-button>
            <el-radio-button label="2">交易中</el-radio-button>
            <el-radio-button label="3">已售出</el-radio-button>
            <el-radio-button label="0">已取消</el-radio-button>
          </el-radio-group>
        </div>

        <div class="items-grid" v-loading="loading">
          <el-empty v-if="!filteredOrders.length && !loading" description="暂无出售订单" />
          
          <div v-for="order in filteredOrders" :key="order.id" class="item-card" @click="goToDetail(order)">
            <div class="card-image">
              <div class="card-badge-row">
                <div class="wear-tag" :class="getCardBadgeClass(order)">{{ getCardBadgeText(order) }}</div>
                <span v-if="getCardSecondaryBadgeText(order)" class="type-tag" :class="getCardSecondaryBadgeClass(order)">
                  {{ getCardSecondaryBadgeText(order) }}
                </span>
              </div>
              <div class="item-icons">
                <span class="icon-badge">订单 #{{ order.id }}</span>
              </div>
              <img :src="order.inventory?.iconUrl || order.item?.iconUrl || '/default-item.svg'" :alt="getItemName(order)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(order) }}</h4>
              <p class="card-subtitle">{{ getCardSubtitle(order) }}</p>
              <p class="card-price">¥ {{ Number(order.price || 0).toFixed(2) }}</p>
              <div class="card-status">
                <el-tag :type="getStatusType(order.status)" size="small">{{ getStatusText(order.status) }}</el-tag>
                <span v-if="getTradeStageText(order)" class="trade-stage">{{ getTradeStageText(order) }}</span>
              </div>
              <div v-if="shouldShowSellCancel(order) || shouldShowShipAction(order) || shouldShowCheckAction(order) || hasRelatedTradeOrder(order)" class="card-actions">
                <el-button
                  v-if="shouldShowSellCancel(order)"
                  size="small"
                  @click.stop="handleCancel(order)"
                >
                  取消出售
                </el-button>
                <el-button
                  v-if="shouldShowShipAction(order)"
                  type="primary"
                  size="small"
                  :loading="shippingOrderId === getRelatedTradeOrder(order)?.id"
                  @click.stop="handleShip(order)"
                >
                  立即检测
                </el-button>
                <el-button
                  v-if="shouldShowCheckAction(order)"
                  size="small"
                  :loading="checkingOrderId === getRelatedTradeOrder(order)?.id"
                  @click.stop="handleBotCheck(getRelatedTradeOrder(order))"
                >
                  检测报价
                </el-button>
                <el-button
                  v-if="hasRelatedTradeOrder(order)"
                  size="small"
                  @click.stop="viewTradeDetail(order)"
                >
                  交易详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <el-dialog v-model="detailDialogVisible" title="交易详情" width="680px">
      <div v-if="currentTradeOrder" class="detail-grid">
        <div class="detail-block">
          <h4>订单信息</h4>
          <p>订单号：{{ currentTradeOrder.orderNo || currentTradeOrder.id }}</p>
          <p>状态：{{ getTradeOrderStatusText(currentTradeOrder.status) }}</p>
          <p>创建时间：{{ formatDate(currentTradeOrder.createdAt) }}</p>
          <p v-if="currentTradeOrder.paidAt">支付时间：{{ formatDate(currentTradeOrder.paidAt) }}</p>
          <p v-if="currentTradeOrder.sentAt">发货时间：{{ formatDate(currentTradeOrder.sentAt) }}</p>
          <p v-if="currentTradeOrder.completedAt">完成时间：{{ formatDate(currentTradeOrder.completedAt) }}</p>
        </div>

        <div class="detail-block">
          <h4>交易信息</h4>
          <p>饰品：{{ currentTradeOrder.item?.nameCn || currentTradeOrder.item?.name || getItemName(currentTradeOrder) }}</p>
          <p>成交价格：¥ {{ Number(currentTradeOrder.price || 0).toFixed(2) }}</p>
          <p>手续费：¥ {{ Number(currentTradeOrder.fee || 0).toFixed(2) }}</p>
          <p>Steam 报价状态：{{ currentTradeOrder.steamOfferStateText || '-' }}</p>
          <p v-if="currentTradeOrder.monitorErrorMessage">检测备注：{{ currentTradeOrder.monitorErrorMessage }}</p>
          <p v-if="currentTradeOrder.tradeOfferId">报价 ID：{{ currentTradeOrder.tradeOfferId }}</p>
          <p v-if="currentTradeOrder.tradeOfferUrl">
            报价链接：
            <a :href="currentTradeOrder.tradeOfferUrl" target="_blank" rel="noreferrer">打开链接</a>
          </p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { cancelSellOrder, getMySellOrders } from '@/api/sellOrder'
import SiteHeader from '@/components/SiteHeader.vue'
import request from '@/utils/request'
import { getItemDisplayModel } from '@/utils/itemDisplay'
import {
  getExteriorText as formatExteriorText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const tradeOrders = ref([])
const statusFilter = ref('')
const shippingOrderId = ref(null)
const checkingOrderId = ref(null)
const detailDialogVisible = ref(false)
const currentTradeOrder = ref(null)
const getCardDisplayModel = (order) => getItemDisplayModel(order)
const getCardBadgeText = (order) => getCardDisplayModel(order).primaryBadge.text
const getCardBadgeClass = (order) => {
  const badge = getCardDisplayModel(order).primaryBadge
  if (!badge) return ''
  if (badge.kind === 'quality') return `wear-tag-quality-${badge.code}`
  if (badge.kind === 'category') return `wear-tag-category-${badge.code || 'other'}`
  return `wear-tag-${badge.code || 'UN'}`
}
const getCardSecondaryBadgeText = (order) => getCardDisplayModel(order).secondaryBadge?.text || ''
const getCardSecondaryBadgeClass = (order) => {
  const badge = getCardDisplayModel(order).secondaryBadge
  return badge ? `type-tag-${badge.code}` : ''
}
const getCardSubtitle = (order) => getCardDisplayModel(order).subtitle

const filteredOrders = computed(() => {
  if (statusFilter.value === '') return orders.value
  return orders.value.filter((order) => String(order.status) === String(statusFilter.value))
})

const resolveTradeKey = (entry) => {
  if (entry?.inventoryId !== null && entry?.inventoryId !== undefined) return `inventory:${entry.inventoryId}`
  if (entry?.itemId !== null && entry?.itemId !== undefined) return `item:${entry.itemId}`
  return ''
}

const tradeOrderMap = computed(() => {
  const map = new Map()
  tradeOrders.value.forEach((tradeOrder) => {
    const key = resolveTradeKey(tradeOrder)
    if (key && !map.has(key)) {
      map.set(key, tradeOrder)
    }
  })
  return map
})

const getItemName = (order) => order.inventory?.name || order.item?.nameCn || order.item?.name || '未知饰品'

const getOrderExterior = (order) =>
  resolveExteriorCode(
    normalizeWearValue(order?.inventory?.paintWear),
    order?.inventory?.exterior,
    order?.inventory?.wearName,
    order?.item?.exterior,
    order?.inventory?.name,
    order?.item?.nameCn,
    order?.item?.name
  )

const getDisplayExterior = (order) => {
  const exterior = getOrderExterior(order)
  return exterior ? formatExteriorText(exterior) : '-'
}

const getExteriorText = (exterior) => {
  const map = {
    FN: '崭新出厂',
    MW: '略有磨损',
    FT: '久经沙场',
    WW: '破损不堪',
    BS: '战痕累累'
  }
  return map[exterior] || exterior || '-'
}

const getStatusText = (status) => {
  const map = {
    0: '已取消',
    1: '在售',
    2: '交易中',
    3: '已售出'
  }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'primary'
  }
  return map[status] || 'info'
}

const getTradeOrderStatusText = (status) => {
  const map = {
    0: '等待买家付款',
    1: '报价检测中',
    2: '待发货',
    3: '等待买家收货',
    4: '交易完成',
    5: '交易已取消',
    6: '纠纷中'
  }
  return map[status] || '交易处理中'
}

const getRelatedTradeOrder = (order) => tradeOrderMap.value.get(resolveTradeKey(order)) || null
const hasRelatedTradeOrder = (order) => Boolean(getRelatedTradeOrder(order))
const shouldShowSellCancel = (order) => Number(order?.status) === 1
const shouldShowShipAction = (order) => Number(getRelatedTradeOrder(order)?.status) === 2
const shouldShowCheckAction = (order) => Number(getRelatedTradeOrder(order)?.status) === 1

const getTradeStageText = (order) => {
  if (Number(order?.status) !== 2) return ''
  const tradeOrder = getRelatedTradeOrder(order)
  if (!tradeOrder) return '系统自动检测中'
  return getTradeOrderStatusText(tradeOrder.status)
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const [sellOrdersResult, tradeOrdersResult] = await Promise.allSettled([
      getMySellOrders(),
      request.get('/v1/order/my/sell')
    ])

    if (sellOrdersResult.status === 'fulfilled') {
      orders.value = Array.isArray(sellOrdersResult.value) ? sellOrdersResult.value : []
    } else {
      orders.value = []
      ElMessage.error(sellOrdersResult.reason?.message || '获取出售订单失败')
    }

    if (tradeOrdersResult.status === 'fulfilled') {
      tradeOrders.value = Array.isArray(tradeOrdersResult.value) ? tradeOrdersResult.value : []
    } else {
      tradeOrders.value = []
      console.warn('获取交易订单失败', tradeOrdersResult.reason)
    }
  } catch (error) {
    ElMessage.error(error?.message || '获取出售订单失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消这个出售订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelSellOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '取消失败')
    }
  }
}

const handleShip = async (order) => {
  const tradeOrder = getRelatedTradeOrder(order)
  if (!tradeOrder?.id) {
    ElMessage.warning('暂未找到对应的交易订单，请稍后刷新重试')
    return
  }

  shippingOrderId.value = tradeOrder.id
  try {
    await request.post(`/v1/order/${tradeOrder.id}/ship`, {})
    ElMessage.success('已触发系统自动检测卖家发货')
    await fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '自动检测发货失败')
  } finally {
    shippingOrderId.value = null
  }
}

const handleBotCheck = async (tradeOrder) => {
  if (!tradeOrder?.id) return

  checkingOrderId.value = tradeOrder.id
  try {
    await request.post(`/v1/order/${tradeOrder.id}/bot-check`)
    ElMessage.success('报价检测已完成')
    await fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '检测失败')
  } finally {
    checkingOrderId.value = null
  }
}

const viewTradeDetail = async (order) => {
  const tradeOrder = getRelatedTradeOrder(order)
  if (!tradeOrder?.id) {
    ElMessage.warning('暂未找到对应的交易订单')
    return
  }

  try {
    const detail = await request.get(`/v1/order/${tradeOrder.id}`)
    currentTradeOrder.value = detail
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '获取交易详情失败')
  }
}

const goToDetail = (order) => {
  if (hasRelatedTradeOrder(order)) {
    viewTradeDetail(order)
    return
  }
  router.push(`/items/order/${order.id}`)
}

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #e3e3e3;
}

.main-content {
  padding-top: 96px;
}

.market-section {
  width: min(1240px, calc(100% - 40px));
  margin: 24px auto 0;
  border: 1px solid #e6e8ec;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
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
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
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

.section-more {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 24px;
  color: #9299a7;
  cursor: pointer;
}

.toolbar {
  padding: 22px 24px 0;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
  padding: 24px 26px 28px;
}

.item-card {
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  border-color: #d9dde5;
  box-shadow: 0 18px 30px rgba(17, 24, 39, 0.12);
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 18px;
  background: linear-gradient(180deg, #f8f8f8 0%, #efefef 100%);
}

.card-image img {
  max-width: 100%;
  max-height: 132px;
  object-fit: contain;
}

.card-badge-row {
  position: absolute;
  left: 8px;
  right: 8px;
  top: 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.wear-tag {
  padding: 5px 8px;
  border-radius: 999px;
  background: #5d8a42;
  color: #fff;
  font-size: 12px;
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

.type-tag {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 21, 34, 0.95);
  color: #ff8f1f;
  font-size: 12px;
  font-weight: 600;
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

.item-icons {
  position: absolute;
  right: 8px;
  top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.icon-badge {
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  padding: 4px 7px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.card-info {
  padding: 14px 16px 16px;
}

.card-name {
  margin: 0 0 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.card-subtitle {
  margin: -2px 0 10px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
}

.card-price {
  margin: 0;
  color: #f0b321;
  font-size: 18px;
  font-weight: 700;
}

.card-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.trade-stage {
  color: #8b5e1a;
  font-size: 12px;
  line-height: 1.4;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.detail-block h4 {
  margin: 0 0 12px;
}

.detail-block p {
  margin: 0 0 10px;
  color: #4b5563;
}

@media (max-width: 1200px) {
  .items-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .items-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>

