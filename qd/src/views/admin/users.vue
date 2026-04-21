<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名、邮箱、手机或 Steam ID" clearable @keyup.enter="loadData" />
        <el-select v-model="query.role" placeholder="角色" clearable>
          <el-option label="普通用户" value="user" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="禁用" :value="0" />
          <el-option label="正常" :value="1" />
          <el-option label="待验证" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="steamId" label="Steam ID" min-width="150" />
        <el-table-column prop="userLevel" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.userLevel >= 2 ? 'danger' : 'info'">{{ row.userLevel >= 2 ? '管理员' : '普通用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="prev, pager, next, sizes, total"
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        @current-change="changePage"
        @size-change="changeSize"
      />
    </el-card>

    <el-drawer v-model="drawerVisible" title="用户业务概览" size="520px">
      <el-descriptions v-if="currentUser" :column="1" border>
        <el-descriptions-item label="用户">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Steam ID">{{ currentUser.steamId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="库存数量">{{ overview.inventoryCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="未读消息">{{ overview.unreadMessages ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="钱包余额">{{ money(overview.wallet?.balance) }}</el-descriptions-item>
        <el-descriptions-item label="冻结金额">{{ money(overview.wallet?.frozenAmount) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { deleteUser, getUserOverview, getUsers, updateUserStatus } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const currentUser = ref(null)
const overview = ref({})
const query = reactive({ page: 1, size: 20, keyword: '', role: '', status: null })

const statusText = (status) => ({ 0: '禁用', 1: '正常', 2: '待验证' }[status] || '未知')
const money = (value) => `￥${Number(value || 0).toFixed(2)}`

const loadData = async () => {
  loading.value = true
  try {
    const data = await getUsers(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  loadData()
}

const changePage = (page) => {
  query.page = page
  loadData()
}

const changeSize = (size) => {
  query.size = size
  query.page = 1
  loadData()
}

const openDetail = async (row) => {
  currentUser.value = row
  overview.value = await getUserOverview(row.id)
  drawerVisible.value = true
}

const toggleStatus = async (row) => {
  await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('状态已更新')
  loadData()
}

const removeUser = async (row) => {
  await ElMessageBox.confirm(`确定删除用户“${row.username}”吗？`, '删除确认', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 16px;
}

.panel {
  border-radius: 8px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 140px 140px auto;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
