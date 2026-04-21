<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索出售单、用户、库存或饰品" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="已取消" :value="0" />
          <el-option label="在售" :value="1" />
          <el-option label="交易中" :value="2" />
          <el-option label="已售出" :value="3" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="user.username" label="卖家" min-width="130" />
        <el-table-column prop="inventory.name" label="库存名称" min-width="220" />
        <el-table-column prop="item.nameCn" label="饰品" min-width="180" />
        <el-table-column prop="price" label="售价" width="110"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="180" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="warning" :disabled="row.status !== 1" @click="cancelOrder(row)">下架</el-button>
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
import { cancelSellOrder, getSellOrders } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, status: null })
const money = (value) => `￥${Number(value || 0).toFixed(2)}`
const statusText = (status) => ({ 0: '已取消', 1: '在售', 2: '交易中', 3: '已售出' }[status] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const data = await getSellOrders(query)
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
  await ElMessageBox.confirm(`确定下架出售单 ${row.id} 吗？`, '下架确认', { type: 'warning' })
  await cancelSellOrder(row.id)
  ElMessage.success('出售单已下架')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 120px 130px auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar { grid-template-columns: 1fr; } }
</style>
