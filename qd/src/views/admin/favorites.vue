<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.type" placeholder="收藏类型" clearable>
          <el-option label="饰品" :value="1" />
          <el-option label="资讯" :value="2" />
        </el-select>
        <el-input v-model.number="query.targetId" placeholder="目标编号" clearable />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="type" label="类型" width="100"><template #default="{ row }">{{ row.type === 1 ? '饰品' : '资讯' }}</template></el-table-column>
        <el-table-column label="目标" min-width="240">
          <template #default="{ row }">{{ row.item?.nameCn || row.item?.name || row.news?.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="收藏时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
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
import { deleteFavorite, getFavorites } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, userId: null, type: null, targetId: null })
const loadData = async () => {
  loading.value = true
  try {
    const data = await getFavorites(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }
const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除收藏记录 ${row.id} 吗？`, '删除确认', { type: 'warning' })
  await deleteFavorite(row.id)
  ElMessage.success('删除成功')
  loadData()
}
onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: 140px 140px 140px auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar { grid-template-columns: 1fr; } }
</style>
