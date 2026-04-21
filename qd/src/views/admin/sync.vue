<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <template #header>数据维护</template>
      <div class="action-grid">
        <el-button type="primary" @click="handleRebuild">重建全部行情数据</el-button>
        <el-button @click="clean('cancelledSell')">清理已取消出售单</el-button>
        <el-button @click="clean('cancelledTrade')">清理已取消交易单</el-button>
        <el-button @click="clean('expiredSell')">清理过期出售单</el-button>
        <el-button @click="clean('soldInventory')">清理已售出库存</el-button>
        <el-button type="warning" @click="clean('all')">一键清理无用数据</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header>同步任务记录</template>
      <div class="toolbar">
        <el-input v-model="query.taskType" placeholder="任务类型" clearable @keyup.enter="loadData" />
        <el-input v-model="query.status" placeholder="任务状态" clearable @keyup.enter="loadData" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="taskType" label="任务类型" min-width="150" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="processedCount" label="处理数" width="100" />
        <el-table-column prop="savedCount" label="新增数" width="100" />
        <el-table-column prop="updatedCount" label="更新数" width="100" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="240" show-overflow-tooltip />
        <el-table-column prop="startedAt" label="开始时间" width="180" />
        <el-table-column prop="finishedAt" label="结束时间" width="180" />
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
import {
  cleanAllUselessData,
  cleanCancelledSellOrders,
  cleanCancelledTradeOrders,
  cleanExpiredSellOrders,
  cleanSoldInventory,
  getSteamSyncTasks,
  rebuildMarketData
} from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, taskType: '', status: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getSteamSyncTasks(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }
const handleRebuild = async () => {
  await ElMessageBox.confirm('确定重建全部行情数据吗？', '维护确认', { type: 'warning' })
  await rebuildMarketData()
  ElMessage.success('行情数据已重建')
}
const cleanActions = {
  cancelledSell: cleanCancelledSellOrders,
  cancelledTrade: cleanCancelledTradeOrders,
  expiredSell: cleanExpiredSellOrders,
  soldInventory: cleanSoldInventory,
  all: cleanAllUselessData
}
const clean = async (key) => {
  await ElMessageBox.confirm('确定执行该清理操作吗？', '清理确认', { type: 'warning' })
  await cleanActions[key]()
  ElMessage.success('清理完成')
  loadData()
}
onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.action-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.toolbar { display: grid; grid-template-columns: 180px 180px auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 800px) { .toolbar { grid-template-columns: 1fr; } }
</style>
