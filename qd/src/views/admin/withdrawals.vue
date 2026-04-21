<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户、银行或账户名" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.status" placeholder="审核状态" clearable>
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="username" label="用户" min-width="120" />
        <el-table-column prop="amount" label="金额" width="120"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column prop="bankName" label="银行" min-width="140" />
        <el-table-column prop="bankAccount" label="收款账号" min-width="180" />
        <el-table-column prop="accountName" label="账户名" width="120" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" :disabled="row.status !== 0" @click="audit(row, 1)">通过</el-button>
            <el-button size="small" type="danger" :disabled="row.status !== 0" @click="audit(row, 2)">拒绝</el-button>
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
import { auditWithdrawal, getWithdrawals } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, status: null })
const money = (value) => `￥${Number(value || 0).toFixed(2)}`
const statusText = (status) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[status] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const data = await getWithdrawals(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }

const audit = async (row, status) => {
  let reason = ''
  if (status === 2) {
    const result = await ElMessageBox.prompt('请输入拒绝原因', '提现审核')
    reason = result.value
  } else {
    await ElMessageBox.confirm(`确定通过提现 ${row.id} 吗？`, '提现审核', { type: 'warning' })
  }
  await auditWithdrawal(row.id, status, reason)
  ElMessage.success('审核完成')
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
