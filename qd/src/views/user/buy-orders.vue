<template>
  <div class="orders-page">
    <SiteHeader active="market" />

    <main class="main-content">
      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">我的求购</div>
          </div>
          <div class="section-more" @click="router.push('/items')">
            发布求购
            <el-icon><Plus /></el-icon>
          </div>
        </div>

        <div class="toolbar">
          <el-radio-group v-model="statusFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="1">进行中</el-radio-button>
            <el-radio-button label="2">已完成</el-radio-button>
            <el-radio-button label="0">已取消</el-radio-button>
          </el-radio-group>
        </div>

        <div class="items-grid" v-loading="loading">
          <el-empty v-if="!filteredOrders.length && !loading" description="暂无求购订单" />

          <div v-for="order in filteredOrders" :key="order.id" class="item-card" @click="goToDetail(order)">
            <div class="card-image">
              <div class="item-icons">
                <span class="icon-badge">求购 #{{ order.id }}</span>
              </div>
              <img :src="order.item?.iconUrl || '/default-item.svg'" :alt="getItemName(order)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(order) }}</h4>
              <p class="card-price">¥ {{ formatPrice(order.price) }}</p>
              <div class="card-meta">
                <span>数量 {{ getRemainingQuantity(order) }}/{{ order.quantity || 1 }}</span>
                <span>{{ formatDate(order.createdAt) }}</span>
              </div>
              <div class="card-actions" @click.stop>
                <el-tag :type="getStatusType(order.status)" size="small">{{ getStatusText(order.status) }}</el-tag>
                <el-button
                  v-if="order.status === 1"
                  type="danger"
                  link
                  @click="handleCancel(order)"
                >
                  取消
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { cancelBuyOrder, getMyBuyOrders } from '@/api/buyOrder'
import SiteHeader from '@/components/SiteHeader.vue'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const statusFilter = ref('')

const filteredOrders = computed(() => {
  if (statusFilter.value === '') return orders.value
  return orders.value.filter((order) => String(order.status) === String(statusFilter.value))
})

const getItemName = (order) => order.item?.nameCn || order.item?.name || '未知饰品'
const formatPrice = (price) => Number(price || 0).toFixed(2)
const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '--')

const getRemainingQuantity = (order) => {
  const quantity = Number(order?.quantity || 0)
  const filled = Number(order?.filledQuantity || 0)
  return Math.max(quantity - filled, 0)
}

const getStatusText = (status) => {
  const map = {
    0: '已取消',
    1: '进行中',
    2: '已完成'
  }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'success',
    2: 'primary'
  }
  return map[status] || 'info'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getMyBuyOrders()
    orders.value = Array.isArray(res) ? res : []
  } catch (error) {
    ElMessage.error(error?.message || '获取求购订单失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消这条求购单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelBuyOrder(order.id)
    ElMessage.success('求购单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.message || error?.message || '取消失败')
    }
  }
}

const goToDetail = (order) => {
  if (order?.itemId) {
    router.push(`/items/${order.itemId}`)
  }
}

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
  overflow: hidden;
  border: 1px solid #e6e8ec;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e2e2;
  background: #ececec;
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

.tab-item.active-blue {
  background: #4e86dc;
  color: #fff;
}

.section-more {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 24px;
  color: #5b6f92;
  cursor: pointer;
  font-weight: 700;
}

.toolbar {
  padding: 22px 24px 0;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
  min-height: 260px;
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

.item-icons {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}

.icon-badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(78, 134, 220, 0.14);
  color: #315d9d;
  font-size: 12px;
  font-weight: 700;
}

.card-info {
  padding: 14px 14px 16px;
}

.card-name {
  min-height: 42px;
  margin: 0 0 10px;
  color: #222;
  font-size: 15px;
  line-height: 1.4;
}

.card-price {
  margin: 0 0 8px;
  color: #f0b321;
  font-size: 20px;
  font-weight: 800;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #8a94a6;
  font-size: 12px;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .items-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .section-more {
    padding: 14px 22px;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
