<template>
  <div class="records-tab">
    <div class="toolbar-card">
      <div class="toolbar-top toolbar-top-right">
        <div class="toolbar-actions">
          <el-button @click="fetchOrders">刷新</el-button>
          <el-tooltip content="导出功能后续开放">
            <span>
              <el-button disabled>导出记录</el-button>
            </span>
          </el-tooltip>
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
          <el-select v-model="filters.status" placeholder="订单状态" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-picker"
            unlink-panels
          />
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
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="pagedOrders" class="records-table" empty-text="暂无购买记录">
        <el-table-column label="饰品" min-width="300">
          <template #default="{ row }">
            <div class="item-cell">
              <img :src="row.item?.iconUrl || '/default-item.svg'" :alt="getItemName(row)" class="item-image" />
              <div class="item-copy">
                <div class="item-name">{{ getItemName(row) }}</div>
                <div class="item-subtitle">{{ getOrderSubtitle(row) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <div class="price-copy">¥ {{ formatPrice(row.price) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="卖家" min-width="210">
          <template #default="{ row }">
            <div class="seller-cell">
              <el-avatar :size="40" :src="row.seller?.avatar || row.seller?.avatarUrl">
                {{ getUserInitial(row.seller?.username) }}
              </el-avatar>
              <div class="seller-copy">
                <div class="seller-name">{{ row.seller?.username || '未知用户' }}</div>
                <div class="seller-meta">订单角色：卖家</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="time-copy">{{ formatDate(row.createdAt) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="详情" min-width="280">
          <template #default="{ row }">
            <div class="detail-cell">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              <div class="detail-meta">订单ID：{{ row.orderNo || row.id }}</div>
              <div class="detail-actions">
                <el-button
                  v-if="row.status === 0"
                  type="primary"
                  size="small"
                  :loading="payingOrderId === row.id"
                  @click="handlePay(row)"
                >
                  立即支付
                </el-button>
                <el-button
                  v-if="row.status === 3"
                  type="success"
                  size="small"
                  :loading="confirmingOrderId === row.id"
                  @click="handleConfirm(row)"
                >
                  确认收货
                </el-button>
                <el-button
                  v-if="row.status === 2 && row.seller?.steamTradeUrl"
                  type="primary"
                  size="small"
                  @click="openOfferGuide(row)"
                >
                  发送报价
                </el-button>
                <el-button
                  v-if="row.status === 2"
                  size="small"
                  :loading="checkingOfferOrderId === row.id"
                  @click="handleDetectOffer(row)"
                >
                  检测报价
                </el-button>
                <el-button
                  v-if="row.status === 1"
                  size="small"
                  :loading="checkingOfferOrderId === row.id"
                  @click="handleBotCheck(row)"
                >
                  刷新状态
                </el-button>
                <el-button
                  v-if="row.status === 0 || row.status === 2"
                  size="small"
                  :loading="cancellingOrderId === row.id"
                  @click="handleCancel(row)"
                >
                  取消订单
                </el-button>
                <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
              </div>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无购买记录" />
        </template>
      </el-table>

      <div v-if="filteredOrders.length > pageSize" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredOrders.length"
          layout="prev, pager, next"
        />
      </div>
    </div>

    <el-dialog v-model="detailDialogVisible" title="订单详情" width="680px">
      <div v-if="currentOrder" class="detail-grid">
        <div class="detail-block">
          <h4>订单信息</h4>
          <p>订单号：{{ currentOrder.orderNo || currentOrder.id }}</p>
          <p>状态：{{ getStatusText(currentOrder.status) }}</p>
          <p>创建时间：{{ formatDate(currentOrder.createdAt) }}</p>
          <p v-if="currentOrder.paidAt">支付时间：{{ formatDate(currentOrder.paidAt) }}</p>
          <p v-if="currentOrder.sentAt">报价检测时间：{{ formatDate(currentOrder.sentAt) }}</p>
          <p v-if="currentOrder.completedAt">完成时间：{{ formatDate(currentOrder.completedAt) }}</p>
        </div>

        <div class="detail-block">
          <h4>交易信息</h4>
          <p>饰品：{{ currentOrder.item?.nameCn || currentOrder.item?.name || '未知饰品' }}</p>
          <p>价格：¥ {{ formatPrice(currentOrder.price) }}</p>
          <p>手续费：¥ {{ formatPrice(currentOrder.fee) }}</p>
          <p>Steam 报价状态：{{ currentOrder.steamOfferStateText || '-' }}</p>
          <p v-if="currentOrder.monitorErrorMessage">检测备注：{{ currentOrder.monitorErrorMessage }}</p>
          <p v-if="currentOrder.status === 2 && currentOrder.seller?.steamTradeUrl">
            卖家报价链接：
            <a :href="buildPrefilledTradeUrl(currentOrder)" target="_blank" rel="noreferrer">打开链接</a>
          </p>
          <p v-if="currentOrder.tradeOfferId">报价 ID：{{ currentOrder.tradeOfferId }}</p>
          <p v-if="currentOrder.tradeOfferUrl">
            报价链接：
            <a :href="currentOrder.tradeOfferUrl" target="_blank" rel="noreferrer">打开链接</a>
          </p>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="offerGuideVisible" title="发送 Steam 报价" width="720px">
      <div v-if="offerGuideOrder" class="offer-guide">
        <section class="offer-summary">
          <img :src="offerGuideOrder.item?.iconUrl || '/default-item.svg'" :alt="getItemName(offerGuideOrder)" />
          <div>
            <h4>{{ getItemName(offerGuideOrder) }}</h4>
            <p>成交价：¥ {{ formatPrice(offerGuideOrder.price) }}</p>
            <p>卖家：{{ offerGuideOrder.seller?.username || '未知用户' }}</p>
          </div>
        </section>

        <div class="offer-steps">
          <article>
            <strong>1</strong>
            <div>
              <h5>打开已带入饰品的 Steam 报价页</h5>
              <p>系统会把本订单饰品参数带到 Steam 官方报价页面。</p>
            </div>
          </article>
          <article>
            <strong>2</strong>
            <div>
              <h5>核对后发送报价</h5>
              <p>确认收取方、饰品和数量无误后，在 Steam 页面点击发送。</p>
            </div>
          </article>
          <article>
            <strong>3</strong>
            <div>
              <h5>回到本站检测报价</h5>
              <p>系统会查你的已发送报价，匹配后继续跟踪 Steam 状态和库存到账。</p>
            </div>
          </article>
        </div>

        <div class="offer-actions">
          <el-button
            type="primary"
            :disabled="!offerGuideOrder.seller?.steamTradeUrl"
            @click="openSellerTradeUrl(offerGuideOrder)"
          >
            打开并自动选择饰品
          </el-button>
          <el-button
            :loading="checkingOfferOrderId === offerGuideOrder.id"
            @click="handleDetectOffer(offerGuideOrder)"
          >
            我已发送，检测报价
          </el-button>
        </div>

        <div class="manual-offer-box">
          <el-input
            v-model="manualTradeOfferInput"
            clearable
            placeholder="检测不到时粘贴 Steam 报价链接或 ID"
          >
            <template #append>
              <el-button
                :loading="checkingOfferOrderId === offerGuideOrder.id"
                @click="handleSubmitManualOffer(offerGuideOrder)"
              >
                提交报价
              </el-button>
            </template>
          </el-input>
        </div>

        <el-alert
          v-if="!offerGuideOrder.seller?.steamTradeUrl"
          type="warning"
          show-icon
          :closable="false"
          title="卖家暂未配置 Steam 报价链接，请联系卖家补充后再发送报价。"
        />
        <el-alert
          v-else-if="!getTradeOfferAssetId(offerGuideOrder)"
          type="warning"
          show-icon
          :closable="false"
          title="当前订单缺少 Steam Asset ID，将打开普通报价链接，需要手动选择饰品。"
        />
        <el-alert
          v-else
          type="info"
          show-icon
          :closable="false"
          title="安装本地 Steam 报价助手后，发送成功会自动登记报价；如果未登记，可粘贴报价链接兜底提交。"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { QUALITY_TEXT_MAP, TYPE_TEXT_MAP, normalizeQualityKey } from '@/utils/itemClassification'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const loading = ref(false)
const hasLoaded = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = 10
const payingOrderId = ref(null)
const confirmingOrderId = ref(null)
const cancellingOrderId = ref(null)
const checkingOfferOrderId = ref(null)
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const offerGuideVisible = ref(false)
const offerGuideOrder = ref(null)
const manualTradeOfferInput = ref('')

const filters = ref({
  quality: '',
  type: '',
  exterior: '',
  status: '',
  dateRange: [],
  search: ''
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
const statusOptions = [
  { value: '0', label: '待支付' },
  { value: '1', label: '报价中' },
  { value: '2', label: '待发送报价' },
  { value: '3', label: '待收货' },
  { value: '4', label: '已完成' },
  { value: '5', label: '已取消' }
]

const getDisplayModel = (order) => getItemDisplayModel(order)

const filteredOrders = computed(() => {
  const keyword = filters.value.search.trim().toLowerCase()
  const [start, end] = Array.isArray(filters.value.dateRange) ? filters.value.dateRange : []

  return orders.value.filter((order) => {
    const displayModel = getDisplayModel(order)
    const name = getItemName(order).toLowerCase()
    const sellerName = String(order?.seller?.username || '').toLowerCase()
    const orderNo = String(order?.orderNo || order?.id || '').toLowerCase()
    const createdAt = order?.createdAt ? new Date(order.createdAt).getTime() : 0
    const startTime = start ? new Date(start).getTime() : 0
    const endTime = end ? new Date(end).setHours(23, 59, 59, 999) : 0

    const matchKeyword = !keyword || name.includes(keyword) || sellerName.includes(keyword) || orderNo.includes(keyword)
    const matchQuality = !filters.value.quality || displayModel.resolvedQuality === normalizeQualityKey(filters.value.quality)
    const matchType = !filters.value.type || displayModel.resolvedType === filters.value.type
    const matchExterior = !filters.value.exterior || displayModel.filterExterior === filters.value.exterior
    const matchStatus = !filters.value.status || String(order.status) === filters.value.status
    const matchDate =
      !start ||
      !end ||
      (createdAt >= startTime && createdAt <= endTime)

    return matchKeyword && matchQuality && matchType && matchExterior && matchStatus && matchDate
  })
})

const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredOrders.value.slice(start, start + pageSize)
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/v1/order/my/buy')
    orders.value = Array.isArray(res) ? res : res?.list || []
    hasLoaded.value = true
  } catch (error) {
    ElMessage.error(error?.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${formatPrice(order.price)} 购买这件饰品吗？`, '确认支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning'
    })

    payingOrderId.value = order.id
    await request.post(`/v1/order/${order.id}/pay`)
    ElMessage.success('支付成功')
    await fetchOrders()
    const paidOrder = orders.value.find((entry) => entry.id === order.id)
    if (paidOrder?.status === 2) {
      openOfferGuide(paidOrder)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '支付失败')
    }
  } finally {
    payingOrderId.value = null
  }
}

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm('确认已经收到饰品了吗？', '确认收货', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    confirmingOrderId.value = order.id
    await request.post(`/v1/order/${order.id}/confirm`)
    ElMessage.success('确认收货成功')
    await fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '确认收货失败')
    }
  } finally {
    confirmingOrderId.value = null
  }
}

const openOfferGuide = (order) => {
  offerGuideOrder.value = order
  manualTradeOfferInput.value = ''
  offerGuideVisible.value = true
}

const STEAM_CSGO_APP_ID = '730'
const STEAM_CSGO_CONTEXT_ID = '2'

const getTradeOfferAssetId = (order) => String(order?.inventory?.assetId || order?.assetId || '').trim()

const buildPrefilledTradeUrl = (order) => {
  const baseUrl = order?.seller?.steamTradeUrl
  const assetId = getTradeOfferAssetId(order)
  if (!baseUrl || !assetId) {
    return baseUrl
  }

  const forItem = `${STEAM_CSGO_APP_ID}_${STEAM_CSGO_CONTEXT_ID}_${assetId}`
  try {
    const url = new URL(baseUrl)
    url.searchParams.set('for_item', forItem)
    url.searchParams.set('cs2trade_order_id', order.id)
    return url.toString()
  } catch (error) {
    const [withoutHash, hash = ''] = String(baseUrl).split('#')
    const separator = withoutHash.includes('?') ? '&' : '?'
    const extraParams = new URLSearchParams({
      for_item: forItem,
      cs2trade_order_id: String(order.id)
    })
    return `${withoutHash}${separator}${extraParams.toString()}${hash ? `#${hash}` : ''}`
  }
}

const openSellerTradeUrl = (order) => {
  const url = buildPrefilledTradeUrl(order)
  if (!url) {
    ElMessage.warning('卖家暂未配置 Steam 报价链接')
    return
  }
  if (!getTradeOfferAssetId(order)) {
    ElMessage.warning('当前订单缺少 Steam Asset ID，将打开普通报价链接')
  }
  window.open(url, '_blank', 'noreferrer')
}

const extractTradeOfferId = (value) => {
  const text = String(value || '').trim()
  if (/^\d+$/.test(text)) {
    return text
  }
  return text.match(/\/tradeoffer\/(\d+)/)?.[1] || ''
}

const handleSubmitManualOffer = async (order) => {
  const rawValue = manualTradeOfferInput.value.trim()
  const tradeOfferId = extractTradeOfferId(rawValue)
  if (!tradeOfferId) {
    ElMessage.warning('请粘贴 Steam 报价链接或报价 ID')
    return
  }

  checkingOfferOrderId.value = order.id
  try {
    await request.post(`/v1/order/${order.id}/buyer-offer/detect`, {
      tradeOfferId,
      tradeOfferUrl: rawValue.startsWith('http') ? rawValue : `https://steamcommunity.com/tradeoffer/${tradeOfferId}/`
    })
    ElMessage.success('Steam 报价已登记')
    await fetchOrders()
    const updatedOrder = orders.value.find((entry) => entry.id === order.id)
    if (updatedOrder) {
      offerGuideOrder.value = updatedOrder
    }
    offerGuideVisible.value = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '提交报价失败')
  } finally {
    checkingOfferOrderId.value = null
  }
}

const resolveSentOfferWithExtension = (order) => {
  const requestId = `offer-${order.id}-${Date.now()}-${Math.random().toString(16).slice(2)}`

  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => {
      window.removeEventListener('message', onMessage)
      reject(new Error('本地 Steam 报价助手未响应，请确认扩展已刷新并启用'))
    }, 12000)

    function onMessage(event) {
      if (
        event.source !== window ||
        event.data?.source !== 'cs2trade_steam_offer_helper' ||
        event.data?.type !== 'resolve-sent-offer-result' ||
        event.data?.requestId !== requestId
      ) {
        return
      }

      window.clearTimeout(timer)
      window.removeEventListener('message', onMessage)
      const response = event.data.payload
      if (response?.ok) {
        resolve(response.result)
      } else {
        reject(new Error(response?.error || '本地 Steam 报价助手读取失败'))
      }
    }

    window.addEventListener('message', onMessage)
    window.postMessage({
      source: 'cs2trade_site',
      type: 'resolve-sent-offer',
      requestId,
      payload: {
        orderId: order.id
      }
    }, '*')
  })
}

const handleDetectOffer = async (order) => {
  if (!order?.id) return

  checkingOfferOrderId.value = order.id
  try {
    await request.post(`/v1/order/${order.id}/buyer-offer/detect`, {})
    ElMessage.success('已触发系统检测你发出的报价')
    await fetchOrders()
    const updatedOrder = orders.value.find((entry) => entry.id === order.id)
    if (updatedOrder) {
      offerGuideOrder.value = updatedOrder
      if (updatedOrder.status !== 2) {
        offerGuideVisible.value = false
      }
    }
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || '检测报价失败'
    try {
      ElMessage.info('正在通过本地 Steam 报价助手读取已发送请求')
      await resolveSentOfferWithExtension(order)
      ElMessage.success('已从 Steam 已发送请求登记报价')
      await fetchOrders()
      const updatedOrder = orders.value.find((entry) => entry.id === order.id)
      if (updatedOrder) {
        offerGuideOrder.value = updatedOrder
        if (updatedOrder.status !== 2) {
          offerGuideVisible.value = false
        }
      }
    } catch (extensionError) {
      ElMessage.error(extensionError?.message || message)
    }
  } finally {
    checkingOfferOrderId.value = null
  }
}

const handleBotCheck = async (order) => {
  if (!order?.id) return

  checkingOfferOrderId.value = order.id
  try {
    await request.post(`/v1/order/${order.id}/bot-check`)
    ElMessage.success('报价状态已刷新')
    await fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '检测报价失败')
  } finally {
    checkingOfferOrderId.value = null
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    cancellingOrderId.value = order.id
    await request.post(`/v1/order/${order.id}/cancel`)
    ElMessage.success('订单已取消')
    await fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '取消订单失败')
    }
  } finally {
    cancellingOrderId.value = null
  }
}

const viewDetail = async (order) => {
  try {
    const detail = await request.get(`/v1/order/${order.id}`)
    currentOrder.value = detail
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '获取订单详情失败')
  }
}

const getItemName = (order) => order?.item?.nameCn || order?.item?.name || '未知饰品'
const getOrderSubtitle = (order) => getDisplayModel(order).subtitle || getDisplayModel(order).exteriorText || '-'
const getUserInitial = (username) => String(username || '?').slice(0, 1).toUpperCase()

const getStatusText = (status) => {
  const map = {
    0: '待支付',
    1: '报价中',
    2: '待发送报价',
    3: '待收货',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知状态'
}

const getStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'warning',
    3: 'success',
    4: 'success',
    5: 'info'
  }
  return map[status] || 'info'
}

const formatPrice = (value) => Number(value || 0).toFixed(2)
const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')

const handleSearch = () => {
  filters.value.search = filters.value.search.trim()
  currentPage.value = 1
}

watch(filteredOrders, () => {
  if ((currentPage.value - 1) * pageSize >= filteredOrders.value.length && currentPage.value > 1) {
    currentPage.value = 1
  }
})

watch(
  () => props.active,
  (active) => {
    if (active && !hasLoaded.value) {
      fetchOrders()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.records-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.toolbar-card,
.table-card {
  border: 1px solid #e4e8f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.toolbar-card {
  padding: 24px;
}

.toolbar-top,
.toolbar-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-filters {
  margin-top: 18px;
}

.toolbar-top-right {
  justify-content: flex-end;
}

.toolbar-actions,
.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 150px;
}

.date-picker {
  width: 260px;
}

.search-group {
  min-width: 300px;
  flex: 1;
  max-width: 420px;
}

.search-input {
  width: 100%;
}

.table-card {
  overflow: hidden;
}

:deep(.records-table th) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}

:deep(.records-table td),
:deep(.records-table th) {
  padding: 18px 0;
}

.item-cell,
.seller-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 10px;
  background: linear-gradient(180deg, #eef2f7 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}

.item-name,
.seller-name,
.price-copy {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.item-subtitle,
.seller-meta,
.time-copy,
.detail-meta {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.detail-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 18px 0 24px;
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

.offer-guide {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.offer-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.offer-summary img {
  width: 82px;
  height: 82px;
  object-fit: contain;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.offer-summary h4,
.offer-steps h5 {
  margin: 0;
  color: #111827;
}

.offer-summary p,
.offer-steps p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.offer-steps {
  display: grid;
  gap: 12px;
}

.offer-steps article {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.offer-steps strong {
  display: grid;
  place-items: center;
  flex: none;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
}

.offer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.manual-offer-box {
  margin-top: 12px;
}

@media (max-width: 1024px) {
  .toolbar-top,
  .toolbar-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .search-group {
    max-width: none;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
