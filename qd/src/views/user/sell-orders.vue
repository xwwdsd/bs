<template>
  <div class="sell-orders-page-dark">
    <!-- 顶部导航 -->
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>
      <div class="user-nav-links">
        <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
        <router-link to="/user/sell-orders" class="nav-link active">我的出售</router-link>
        <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        <UserNavDropdown />
      </div>
    </nav>

    <!-- 主要内容 -->
    <div class="main-container">
      <div class="content-wrapper">
        <div class="page-header">
          <h1 class="page-title">我的出售</h1>
          <el-button type="primary" class="action-btn" @click="$router.push('/user/inventory')">
            <el-icon><Plus /></el-icon> 发布出售
          </el-button>
        </div>

        <!-- 状态筛选 -->
        <div class="toolbar">
          <div class="filter-tabs">
            <el-radio-group v-model="statusFilter" @change="handleFilterChange" class="dark-radio-group">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="1">在售</el-radio-button>
              <el-radio-button label="2">交易中</el-radio-button>
              <el-radio-button label="3">已售出</el-radio-button>
              <el-radio-button label="0">已取消</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 出售列表 -->
        <div class="orders-container" v-loading="loading" element-loading-background="rgba(28, 31, 40, 0.8)">
          <div v-if="orders.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无出售订单">
              <el-button type="primary" class="action-btn" @click="$router.push('/user/inventory')">去发布出售</el-button>
            </el-empty>
          </div>

          <div v-else class="orders-list">
            <el-table :data="orders" style="width: 100%" class="dark-table" :header-cell-style="{background:'#252834', color:'#868a9f'}" :row-style="{background:'#1c1f28', color:'#ccc'}">
              <el-table-column label="饰品信息" width="400">
                <template #default="scope">
                  <div class="order-item-info">
                    <img :src="scope.row.inventory?.iconUrl || scope.row.item?.iconUrl || '/default-item.png'" class="order-img" />
                    <div class="item-details">
                      <div class="item-name">{{ scope.row.inventory?.name || scope.row.item?.nameCn || scope.row.item?.name || '未知饰品' }}</div>
                      <div class="item-exterior">{{ getExteriorText(scope.row.inventory?.exterior || scope.row.item?.exterior) }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="price" label="价格">
                <template #default="scope">
                  <span class="price-text">¥ {{ scope.row.price }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="发布时间">
                <template #default="scope">
                  {{ formatTime(scope.row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)" effect="dark">{{ getStatusText(scope.row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" fixed="right" width="150">
                <template #default="scope">
                  <el-button v-if="scope.row.status === 1" type="danger" size="small" plain @click="handleCancel(scope.row)">
                    取消
                  </el-button>
                  <el-button v-if="scope.row.status === 2" type="primary" size="small" @click="handleConfirm(scope.row)">
                    确认发货
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="totalPages > 1">
            <el-pagination 
              v-model:current-page="page" 
              v-model:page-size="size" 
              :total="total"
              :page-sizes="[10, 20, 50]" 
              layout="total, sizes, prev, pager, next" 
              @size-change="handleSizeChange"
              @current-change="handlePageChange" 
              background
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import UserNavDropdown from '@/components/UserNavDropdown.vue'
import { getMySellOrders, cancelSellOrder } from '@/api/sellOrder'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')

const getExteriorText = (exterior) => {
  const map = {
    'FN': '崭新出厂',
    'MW': '略有磨损',
    'FT': '久经沙场',
    'WW': '破损不堪',
    'BS': '战痕累累'
  }
  return map[exterior] || exterior
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

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getMySellOrders()
    orders.value = res || []
    total.value = res?.length || 0
    totalPages.value = Math.ceil(total.value / size.value)
  } catch (error) {
    console.error('获取出售订单失败:', error)
    ElMessage.error('获取出售订单失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  page.value = 1
  fetchOrders()
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 1
  fetchOrders()
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchOrders()
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消这个出售订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      background: '#252834'
    })
    await cancelSellOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '取消失败')
    }
  }
}

const handleConfirm = (order) => {
  ElMessageBox.confirm('确认已发送Steam交易报价？', '确认发货', {
    confirmButtonText: '已发送',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    ElMessage.success('发货确认成功')
    fetchOrders()
  })
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
/* 深色主题样式 */
.sell-orders-page-dark {
  min-height: 100vh;
  background: #171a21;
  color: #c7d5e0;
  font-family: 'Microsoft YaHei', sans-serif;
}

/* 导航栏深色适配 */
.navbar-dark {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 50px;
  height: 64px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
}

.nav-links-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.nav-links-left .nav-link {
  color: #c7d5e0;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
}

.nav-links-left .nav-link:hover {
  color: #fff;
}

.user-nav-links {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 40px;
}

.user-nav-links .nav-link {
  color: #868a9f !important;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
}

.user-nav-links .nav-link:visited {
  color: #868a9f !important;
}

.user-nav-links .nav-link:hover,
.user-nav-links .nav-link.active,
.user-nav-links .router-link-active {
  color: #fff !important;
}

.nav-user-section {
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: background 0.3s;
}

.user-profile:hover {
  background: #2a2e3b;
}

.username {
  color: #fff;
  font-size: 14px;
}

/* 主要内容区 */
.main-container {
  max-width: 1200px;
  margin: 80px auto 20px;
  padding: 0 20px;
}

.content-wrapper {
  background: #1c1f28;
  min-height: 600px;
  border-radius: 4px;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #2a2e3b;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.action-btn {
  background-color: #4b89dc;
  border-color: #4b89dc;
}

.action-btn:hover {
  background-color: #5da0ff;
  border-color: #5da0ff;
}

/* 工具栏 */
.toolbar {
  margin-bottom: 20px;
}

.filter-tabs {
  background: #232631;
  padding: 5px;
  border-radius: 4px;
  display: inline-block;
}

/* 列表样式 */
.orders-list {
  background: #1c1f28;
}

.order-item-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.order-img {
  width: 80px;
  height: 60px;
  object-fit: contain;
  background: #252834;
  border-radius: 4px;
}

.item-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item-name {
  color: #c7d5e0;
  font-size: 14px;
}

.item-exterior {
  color: #868a9f;
  font-size: 12px;
}

.price-text {
  color: #eeb425;
  font-weight: bold;
  font-size: 16px;
}

.empty-state {
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #2a2e3b;
}

/* 深色组件覆盖 */
:deep(.dark-radio-group .el-radio-button__inner) {
  background-color: #232631;
  color: #868a9f;
  border-color: #3a3f50;
  box-shadow: none;
}

:deep(.dark-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #4b89dc;
  border-color: #4b89dc;
  color: #fff;
  box-shadow: none;
}

:deep(.dark-table) {
  --el-table-border-color: #2a2e3b;
  --el-table-bg-color: #1c1f28;
  --el-table-tr-bg-color: #1c1f28;
  --el-table-header-bg-color: #252834;
  color: #c7d5e0;
}

:deep(.el-table tr) {
  background-color: #1c1f28 !important;
}

:deep(.el-table th.el-table__cell) {
  background-color: #252834 !important;
  color: #868a9f;
  border-bottom: 1px solid #2a2e3b;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #2a2e3b;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled)) {
  background-color: #252834;
  color: #868a9f;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #4b89dc;
  color: #fff;
}

:deep(.el-pagination.is-background .btn-prev),
:deep(.el-pagination.is-background .btn-next) {
  background-color: #252834;
  color: #868a9f;
}
</style>
