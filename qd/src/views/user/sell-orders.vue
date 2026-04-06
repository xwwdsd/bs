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
              <div class="wear-tag">{{ getDisplayExterior(order) }}</div>
              <div class="item-icons">
                <span class="icon-badge">订单 #{{ order.id }}</span>
              </div>
              <img :src="order.inventory?.iconUrl || order.item?.iconUrl || '/default-item.png'" :alt="getItemName(order)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(order) }}</h4>
              <p class="card-price">¥ {{ Number(order.price || 0).toFixed(2) }}</p>
              <div class="card-status">
                <el-tag :type="getStatusType(order.status)" size="small">{{ getStatusText(order.status) }}</el-tag>
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
import { cancelSellOrder, getMySellOrders } from '@/api/sellOrder'
import SiteHeader from '@/components/SiteHeader.vue'
import {
  getExteriorText as formatExteriorText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const statusFilter = ref('')

const filteredOrders = computed(() => {
  if (statusFilter.value === '') return orders.value
  return orders.value.filter((order) => String(order.status) === String(statusFilter.value))
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

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getMySellOrders()
    orders.value = Array.isArray(res) ? res : []
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

const goToDetail = (order) => {
  router.push(`/items/order/${order.id}`)
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

.wear-tag {
  position: absolute;
  left: 8px;
  top: 8px;
  padding: 5px 8px;
  border-radius: 999px;
  background: #5d8a42;
  color: #fff;
  font-size: 12px;
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

.card-price {
  margin: 0;
  color: #f0b321;
  font-size: 18px;
  font-weight: 700;
}

.card-status {
  margin-top: 10px;
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
}

@media (max-width: 560px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
