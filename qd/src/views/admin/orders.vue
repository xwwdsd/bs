<template>
  <div class="admin-common-page">
    <div class="page-header">
      <h1>订单管理</h1>
      <p>管理订单记录和交易状态</p>
    </div>

    <el-card class="content-card">
      <div class="card-header">
        <div class="header-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索订单号/用户名"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="statusFilter" placeholder="订单状态" clearable class="filter-select" @change="handleSearch">
            <el-option label="待支付" value="pending" />
            <el-option label="待发货" value="paid" />
            <el-option label="待收货" value="shipped" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </div>
        <div class="header-right">
          <el-button @click="refreshData" :loading="loading">刷新</el-button>
        </div>
      </div>

      <el-table :data="orders" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="订单号" width="180" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="itemName" label="商品" min-width="200" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price-text">¥ {{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleView(row)">详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAllOrders, cancelOrder } from '@/api/admin'

const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const orders = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getAllOrders()
    orders.value = res || []
    total.value = orders.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchOrders()
}

const refreshData = () => {
  fetchOrders()
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchOrders()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  page.value = 1
  fetchOrders()
}

const handleView = (row) => {
  ElMessage.info(`查看订单详情：${row.id}`)
  // TODO: 打开订单详情对话框
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除订单 "${row.id}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelOrder(row.id)
    ElMessage.success('取消成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.admin-common-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.content-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.header-left {
  flex: 1;
  display: flex;
  gap: 12px;
}

.search-input {
  width: 300px;
}

.filter-select {
  width: 150px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.price-text {
  color: #f59e0b;
  font-weight: 600;
}
</style>
