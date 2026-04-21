<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索求购单、用户或饰品" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="已取消" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="expireOrders">处理过期</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userId" label="用户" width="90" />
        <el-table-column prop="item.nameCn" label="求购饰品" min-width="220" />
        <el-table-column prop="price" label="单价" width="110"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="filledQuantity" label="已成交" width="90" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" :disabled="row.status !== 1" @click="cancelOrder(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="query.page" :page-size="query.size" :total="total"
        @current-change="changePage" @size-change="changeSize" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { cancelBuyOrder, expireBuyOrders, getBuyOrders } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, status: null })
const money = (value) => `￥${Number(value || 0).toFixed(2)}`
const statusText = (status) => ({ 0: '已取消', 1: '进行中', 2: '已完成' }[status] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const data = await getBuyOrders(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }

const cancelOrder = async (row) => {
  await ElMessageBox.confirm(`确定取消求购单 ${row.id} 吗？`, '取消确认', { type: 'warning' })
  await cancelBuyOrder(row.id)
  ElMessage.success('求购单已取消')
  loadData()
}

const expireOrders = async () => {
  const count = await expireBuyOrders()
  ElMessage.success(`已处理 ${count || 0} 条过期求购`)
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 120px 130px auto auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar { grid-template-columns: 1fr; } }
</style>
