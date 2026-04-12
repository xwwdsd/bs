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
              <img :src="row.item?.iconUrl || '/default-item.png'" :alt="getItemName(row)" class="item-image" />
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
          <p v-if="currentOrder.sentAt">发货时间：{{ formatDate(currentOrder.sentAt) }}</p>
          <p v-if="currentOrder.completedAt">完成时间：{{ formatDate(currentOrder.completedAt) }}</p>
        </div>

        <div class="detail-block">
          <h4>交易信息</h4>
          <p>饰品：{{ currentOrder.item?.nameCn || currentOrder.item?.name || '未知饰品' }}</p>
          <p>价格：¥ {{ formatPrice(currentOrder.price) }}</p>
          <p>手续费：¥ {{ formatPrice(currentOrder.fee) }}</p>
          <p>Steam 报价状态：{{ currentOrder.steamOfferStateText || '-' }}</p>
          <p v-if="currentOrder.monitorErrorMessage">检测备注：{{ currentOrder.monitorErrorMessage }}</p>
          <p v-if="currentOrder.tradeOfferId">报价 ID：{{ currentOrder.tradeOfferId }}</p>
          <p v-if="currentOrder.tradeOfferUrl">
            报价链接：
            <a :href="currentOrder.tradeOfferUrl" target="_blank" rel="noreferrer">打开链接</a>
          </p>
        </div>
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
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

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
  { value: '1', label: '待发货' },
  { value: '2', label: '待收货' },
  { value: '3', label: '已发货' },
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
    1: '待发货',
    2: '待收货',
    3: '已发货',
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
