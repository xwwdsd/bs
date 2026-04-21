<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索库存名称、资产编号" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-input v-model.number="query.itemId" placeholder="饰品编号" clearable />
        <el-select v-model="query.status" placeholder="库存状态" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="已售出" :value="1" />
          <el-option label="交易中" :value="2" />
          <el-option label="在售" :value="3" />
        </el-select>
        <el-select v-model="query.isMarketable" placeholder="可交易" clearable>
          <el-option label="可交易" :value="1" />
          <el-option label="不可交易" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button type="warning" @click="cleanAbnormal">清理异常</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userId" label="用户" width="90" />
        <el-table-column prop="assetId" label="资产编号" min-width="150" />
        <el-table-column prop="name" label="库存名称" min-width="220" />
        <el-table-column prop="item.nameCn" label="映射饰品" min-width="180" />
        <el-table-column prop="isMarketable" label="可交易" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isMarketable === 1 ? 'success' : 'info'">{{ row.isMarketable === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="marketPrice" label="市场价" width="110">
          <template #default="{ row }">{{ money(row.marketPrice) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" @click="openFix(row)">修复映射</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="query.page" :page-size="query.size" :total="total"
        @current-change="changePage" @size-change="changeSize" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="库存详情" size="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="库存编号">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="用户编号">{{ current.userId }}</el-descriptions-item>
        <el-descriptions-item label="资产编号">{{ current.assetId }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="检查链接">{{ current.inspectUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="不可交易原因">{{ current.marketableReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { cleanAbnormalInventories, fixInventoryItemMapping, getInventories } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const current = ref(null)
const drawerVisible = ref(false)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, itemId: null, status: null, isMarketable: null })

const statusText = (status) => ({ 0: '正常', 1: '已售出', 2: '交易中', 3: '在售' }[status] || '未知')
const money = (value) => value == null ? '-' : `￥${Number(value).toFixed(2)}`

const loadData = async () => {
  loading.value = true
  try {
    const data = await getInventories(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }
const openDetail = (row) => { current.value = row; drawerVisible.value = true }

const openFix = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入新的饰品编号', '修复库存映射', { inputValue: row.itemId || '' })
  await fixInventoryItemMapping(row.id, Number(value))
  ElMessage.success('映射已修复')
  loadData()
}

const cleanAbnormal = async () => {
  await ElMessageBox.confirm('确定清理 item_id 为空的异常库存记录吗？', '清理确认', { type: 'warning' })
  const count = await cleanAbnormalInventories()
  ElMessage.success(`已清理 ${count || 0} 条记录`)
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 120px 120px 130px 120px auto auto;
  gap: 12px;
  margin-bottom: 16px;
}
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 1100px) { .toolbar { grid-template-columns: 1fr; } }
</style>
