<template>
  <div class="admin-common-page">
    <div class="page-header">
      <h1>饰品管理</h1>
      <p>管理饰品数据、分类和价格</p>
    </div>

    <el-card class="content-card">
      <div class="card-header">
        <div class="header-left">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索饰品名称"
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="categoryFilter" placeholder="分类" clearable class="filter-select" @change="handleSearch">
            <el-option label="步枪" value="rifle" />
            <el-option label="手枪" value="pistol" />
            <el-option label="冲锋枪" value="smg" />
            <el-option label="狙击步枪" value="sniper_rifle" />
            <el-option label="刀" value="knife" />
            <el-option label="手套" value="glove" />
          </el-select>
        </div>
        <div class="header-right">
          <el-button @click="refreshData" :loading="loading">刷新</el-button>
        </div>
      </div>

      <el-table :data="items" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="250" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="exterior" label="外观" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price-text">¥ {{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'onsale' ? 'success' : 'info'">
              {{ row.status === 'onsale' ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
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
import { getAllItems, deleteItem } from '@/api/admin'

const loading = ref(false)
const searchKeyword = ref('')
const categoryFilter = ref('')
const items = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await getAllItems()
    items.value = res || []
    total.value = items.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取饰品列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchItems()
}

const refreshData = () => {
  fetchItems()
}

const handlePageChange = (newPage) => {
  page.value = newPage
  fetchItems()
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  page.value = 1
  fetchItems()
}

const handleEdit = (row) => {
  ElMessage.info(`编辑饰品：${row.name}`)
  // TODO: 打开编辑对话框
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除饰品 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteItem(row.id)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchItems()
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
