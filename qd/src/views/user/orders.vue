<template>
  <div class="orders-page">
    <SiteHeader />

    <main class="page-main">
      <section class="page-panel">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">我的订单</div>
            <div class="tab-item plain">查看买入和卖出的交易订单</div>
          </div>
          <div class="toolbar-right">
            <el-radio-group v-model="orderType" size="large" @change="handleOrderTypeChange">
              <el-radio-button label="buy">我购买的</el-radio-button>
              <el-radio-button label="sell">我卖出的</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="order-list" v-loading="loading">
          <el-empty v-if="orders.length === 0" description="暂无订单" />

          <div v-else class="order-cards">
            <article v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-head">
                <div>
                  <div class="order-no">订单号：{{ order.orderNo || order.id }}</div>
                  <div class="order-time">{{ formatDate(order.createdAt) }}</div>
                </div>
                <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
              </div>

              <div class="order-body">
                <div class="item-info">
                  <img :src="order.item?.iconUrl || '/default-item.png'" class="item-image" />
                  <div>
                    <h3>{{ order.item?.nameCn || order.item?.name || '未知饰品' }}</h3>
                    <p v-if="getOrderExteriorText(order)">
                      {{ getOrderExteriorText(order) }}
                    </p>
                  </div>
                </div>

                <div class="price-info">
                  <span>成交价格：¥ {{ formatPrice(order.price) }}</span>
                  <span>手续费：¥ {{ formatPrice(order.fee) }}</span>
                  <span v-if="orderType === 'sell'">实际到账：¥ {{ formatPrice(order.actualAmount) }}</span>
                </div>

                <div class="party-info">
                  <span v-if="orderType === 'buy'">卖家：{{ order.seller?.username || '未知用户' }}</span>
                  <span v-else>买家：{{ order.buyer?.username || '未知用户' }}</span>
                </div>
              </div>

              <div class="order-footer">
                <el-button
                  v-if="orderType === 'buy' && order.status === 0"
                  type="primary"
                  :loading="paying"
                  @click="handlePay(order)"
                >
                  立即支付
                </el-button>

                <el-button
                  v-if="orderType === 'buy' && order.status === 3"
                  type="success"
                  :loading="confirming"
                  @click="handleConfirm(order)"
                >
                  确认收货
                </el-button>

                <el-button
                  v-if="orderType === 'sell' && order.status === 2"
                  type="primary"
                  :loading="shipping"
                  @click="handleShip(order)"
                >
                  立即检测
                </el-button>

                <el-button v-if="order.status === 1" :loading="loading" @click="handleBotCheck(order)">立即检测</el-button>

                <el-button
                  v-if="order.status === 0 || order.status === 2"
                  :loading="cancelling"
                  @click="handleCancel(order)"
                >
                  取消订单
                </el-button>

                <el-button @click="viewDetail(order)">查看详情</el-button>
              </div>
            </article>
          </div>
        </div>
      </section>
    </main>

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
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import SiteHeader from '@/components/SiteHeader.vue'
import {
  getExteriorText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orderType = ref('buy')
const orders = ref([])
const paying = ref(false)
const confirming = ref(false)
const cancelling = ref(false)
const shipping = ref(false)


const detailDialogVisible = ref(false)
const currentOrder = ref(null)

const getOrderExteriorText = (order) => {
  const exterior = resolveExteriorCode(
    normalizeWearValue(order?.inventory?.paintWear),
    order?.inventory?.exterior,
    order?.inventory?.wearName,
    order?.item?.exterior,
    order?.inventory?.name,
    order?.item?.nameCn,
    order?.item?.name
  )
  return exterior ? getExteriorText(exterior) : ''
}

const normalizeOrderType = (value) => (value === 'sell' ? 'sell' : 'buy')

const updateOrderTypeQuery = async (value) => {
  const normalized = normalizeOrderType(value)
  if (route.query.type === normalized) return
  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      type: normalized
    }
  })
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const url = orderType.value === 'buy' ? '/v1/order/my/buy' : '/v1/order/my/sell'
    const res = await request.get(url)
    orders.value = Array.isArray(res) ? res : res?.list || []
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

    paying.value = true
    await request.post(`/v1/order/${order.id}/pay`)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '支付失败')
    }
  } finally {
    paying.value = false
  }
}

const handleShip = async (order) => {
  if (!order?.id) {
    ElMessage.warning('暂未找到对应的交易订单，请稍后刷新重试')
    return
  }

  shipping.value = true
  try {
    await request.post(`/v1/order/${order.id}/ship`, {})
    ElMessage.success('已触发系统自动检测卖家发货')
    fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '自动检测发货失败')
  } finally {
    shipping.value = false
  }
}

const handleBotCheck = async (order) => {
  loading.value = true
  try {
    await request.post(`/v1/order/${order.id}/bot-check`)
    ElMessage.success('报价检测已完成')
    fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '检测失败')
  } finally {
    loading.value = false
  }
}

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm('确认已经收到饰品了吗？', '确认收货', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    confirming.value = true
    await request.post(`/v1/order/${order.id}/confirm`)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '确认收货失败')
    }
  } finally {
    confirming.value = false
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    cancelling.value = true
    await request.post(`/v1/order/${order.id}/cancel`)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '取消订单失败')
    }
  } finally {
    cancelling.value = false
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

const handleOrderTypeChange = async (value) => {
  try {
    await updateOrderTypeQuery(value)
  } catch (error) {
    ElMessage.error(error?.message || '切换订单类型失败')
  }
}

const getStatusText = (status) => {
  const map = {
    0: '待支付',
    1: '报价中',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '纠纷中'
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

watch(
  () => route.query.type,
  (value) => {
    const normalized = normalizeOrderType(value)
    if (orderType.value !== normalized) {
      orderType.value = normalized
    }
    fetchOrders()
  },
  { immediate: true }
)
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 36px;
}

.page-panel {
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
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

.tab-item.active-blue {
  background: #4e86dc;
  color: #fff;
}

.toolbar-right {
  padding-right: 24px;
}

.order-list {
  padding: 24px;
}

.order-cards {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.order-card {
  border: 1px solid #ececec;
  border-radius: 12px;
  padding: 20px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.order-no {
  color: #111827;
  font-weight: 600;
}

.order-time {
  margin-top: 6px;
  color: #9ca3af;
  font-size: 13px;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: contain;
}

.item-info h3 {
  margin: 0 0 6px;
  color: #111827;
}

.item-info p,
.price-info span,
.party-info span {
  color: #4b5563;
}

.price-info {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.order-footer {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
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

@media (max-width: 900px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>


