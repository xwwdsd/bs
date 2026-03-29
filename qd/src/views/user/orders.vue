<template>
  <div class="orders-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>

      <div class="user-nav-links">
        <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
        <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
        <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        <div class="nav-user-section">
          <div class="user-profile" @click="$router.push('/user/profile')">
            <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
            <span class="username">{{ userStore.userInfo?.username }}</span>
          </div>
        </div>
      </div>
    </nav>

    <div class="main-container">
      <div class="page-header">
        <div>
          <h1>我的订单</h1>
          <p>查看买入和卖出的交易订单，并在这里完成支付、发货和确认收货。</p>
        </div>
        <el-radio-group v-model="orderType" size="large" @change="fetchOrders">
          <el-radio-button label="buy">我购买的</el-radio-button>
          <el-radio-button label="sell">我卖出的</el-radio-button>
        </el-radio-group>
      </div>

      <div class="orders-list" v-loading="loading">
        <el-empty v-if="orders.length === 0" description="暂无订单" />

        <div v-else class="order-cards">
          <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
            <div class="order-header">
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
                  <p v-if="order.item?.exterior">{{ order.item.exterior }}</p>
                </div>
              </div>

              <div class="price-info">
                <span>成交价格：￥{{ formatPrice(order.price) }}</span>
                <span>手续费：￥{{ formatPrice(order.fee) }}</span>
                <span v-if="orderType === 'sell'">实际到账：￥{{ formatPrice(order.actualAmount) }}</span>
              </div>

              <div class="party-info">
                <span v-if="orderType === 'buy'">卖家：{{ order.seller?.username || '未知用户' }}</span>
                <span v-else>买家：{{ order.buyer?.username || '未知用户' }}</span>
              </div>
            </div>

            <div class="order-footer">
              <el-button v-if="orderType === 'buy' && order.status === 0" type="primary" :loading="paying" @click="handlePay(order)">
                立即支付
              </el-button>
              <el-button v-if="orderType === 'buy' && order.status === 3" type="success" :loading="confirming" @click="handleConfirm(order)">
                确认收货
              </el-button>
              <el-button v-if="orderType === 'sell' && order.status === 2" type="primary" :loading="shipping" @click="showShipDialog(order)">
                发货
              </el-button>
              <el-button v-if="order.status === 0 || order.status === 2" :loading="cancelling" @click="handleCancel(order)">
                取消订单
              </el-button>
              <el-button @click="viewDetail(order)">查看详情</el-button>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <el-dialog v-model="shipDialogVisible" title="发货" width="500px">
      <el-form :model="shipForm" label-position="top">
        <el-form-item label="Steam 交易报价 ID">
          <el-input v-model="shipForm.tradeOfferId" placeholder="请输入 Steam 交易报价 ID" />
        </el-form-item>
        <el-form-item label="交易报价链接">
          <el-input v-model="shipForm.tradeOfferUrl" placeholder="https://steamcommunity.com/tradeoffer/..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
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
          <h4>商品信息</h4>
          <p>名称：{{ currentOrder.item?.nameCn || currentOrder.item?.name || '未知饰品' }}</p>
          <p>价格：￥{{ formatPrice(currentOrder.price) }}</p>
          <p>手续费：￥{{ formatPrice(currentOrder.fee) }}</p>
          <p v-if="currentOrder.tradeOfferId">交易报价 ID：{{ currentOrder.tradeOfferId }}</p>
          <p v-if="currentOrder.tradeOfferUrl">
            交易链接：
            <a :href="currentOrder.tradeOfferUrl" target="_blank" rel="noreferrer">打开链接</a>
          </p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import websocketService from '@/utils/websocket'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const orderType = ref('buy')
const orders = ref([])
const paying = ref(false)
const confirming = ref(false)
const cancelling = ref(false)
const shipping = ref(false)

const shipDialogVisible = ref(false)
const shipForm = ref({
  orderId: null,
  tradeOfferId: '',
  tradeOfferUrl: ''
})

const detailDialogVisible = ref(false)
const currentOrder = ref(null)

const fetchOrders = async () => {
  loading.value = true
  try {
    const url = orderType.value === 'buy' ? '/v1/order/my/buy' : '/v1/order/my/sell'
    const res = await request.get(url)
    orders.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (error) {
    ElMessage.error(error?.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确认支付 ￥${formatPrice(order.price)} 购买 ${order.item?.nameCn || order.item?.name || '该商品'} 吗？`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    paying.value = true
    await request.post(`/v1/order/${order.id}/pay`)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '支付失败')
    }
  } finally {
    paying.value = false
  }
}

const showShipDialog = (order) => {
  shipForm.value = {
    orderId: order.id,
    tradeOfferId: '',
    tradeOfferUrl: ''
  }
  shipDialogVisible.value = true
}

const handleShip = async () => {
  if (!shipForm.value.tradeOfferId) {
    ElMessage.warning('请输入 Steam 交易报价 ID')
    return
  }

  shipping.value = true
  try {
    await request.post(`/v1/order/${shipForm.value.orderId}/ship`, {
      tradeOfferId: shipForm.value.tradeOfferId,
      tradeOfferUrl: shipForm.value.tradeOfferUrl
    })
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    fetchOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '发货失败')
  } finally {
    shipping.value = false
  }
}

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确认已经收到商品吗？确认后款项会打给卖家。',
      '确认收货',
      {
        confirmButtonText: '确认收货',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    confirming.value = true
    await request.post(`/v1/order/${order.id}/confirm`)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '确认收货失败')
    }
  } finally {
    confirming.value = false
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？',
      '取消订单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    cancelling.value = true
    await request.post(`/v1/order/${order.id}/cancel`)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '取消订单失败')
    }
  } finally {
    cancelling.value = false
  }
}

const viewDetail = (order) => {
  currentOrder.value = order
  detailDialogVisible.value = true
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待确认',
    1: '报价中',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '纠纷中'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'info',
    6: 'danger'
  }
  return typeMap[status] || 'info'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatPrice = (value) => {
  return Number(value || 0).toFixed(2)
}

const handleWebSocketMessage = (event) => {
  const payload = event.detail
  fetchOrders()
  if (payload?.message) {
    ElMessage.info(payload.message)
  }
}

onMounted(() => {
  fetchOrders()
  websocketService.connect()
  window.addEventListener('ws-order-update', handleWebSocketMessage)
})

onUnmounted(() => {
  window.removeEventListener('ws-order-update', handleWebSocketMessage)
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #111927 0%, #152032 100%);
  color: #eef3ff;
}

.navbar-dark {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: rgba(13, 20, 31, 0.92);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
}

.nav-links-left,
.user-nav-links,
.nav-user-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  color: rgba(238, 243, 255, 0.76);
  text-decoration: none;
}

.nav-link:hover {
  color: #fff;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: rgba(238, 243, 255, 0.7);
}

.order-cards {
  display: grid;
  gap: 16px;
}

.order-card {
  border-radius: 20px;
}

.order-header,
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.order-body {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.item-info {
  display: flex;
  gap: 14px;
  align-items: center;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 16px;
  background: #f5f7fb;
}

.price-info,
.party-info {
  display: grid;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-block {
  padding: 16px;
  border-radius: 16px;
  background: #f7f9fc;
}

.detail-block h4 {
  margin-top: 0;
}

@media (max-width: 900px) {
  .order-body,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .page-header,
  .order-header,
  .order-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
