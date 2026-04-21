<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索饰品名称或市场名称"
          clearable
          @keyup.enter="loadData"
        />
        <el-input v-model="query.category" placeholder="分类" clearable @keyup.enter="loadData" />
        <el-select v-model="query.isActive" placeholder="启用状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Plus" @click="openEditor()">新增</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column label="图标" width="92">
          <template #default="{ row }">
            <el-image
              v-if="row.iconUrl"
              class="item-icon"
              :src="row.iconUrl"
              fit="contain"
              :preview-src-list="[row.iconUrl]"
              preview-teleported
            />
            <span v-else class="empty-image">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="nameCn" label="中文名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="name" label="英文名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="exterior" label="外观" width="110" />
        <el-table-column prop="steamReferencePrice" label="参考价" width="120">
          <template #default="{ row }">{{ money(row.steamReferencePrice || row.buffPrice) }}</template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
              {{ row.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditor(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeItem(row)">删除</el-button>
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

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑饰品' : '新增饰品'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="物品编号">
          <el-input v-model="form.itemId" />
        </el-form-item>
        <el-form-item label="中文名">
          <el-input v-model="form.nameCn" />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="子分类">
          <el-input v-model="form.subCategory" />
        </el-form-item>
        <el-form-item label="品质">
          <el-input v-model="form.quality" />
        </el-form-item>
        <el-form-item label="外观">
          <el-input v-model="form.exterior" />
        </el-form-item>
        <el-form-item label="图标上传">
          <AdminImageUpload
            v-model="form.iconUrl"
            :width="220"
            :height="220"
            tip="饰品图标会在后台列表和前台饰品卡片中复用"
          />
        </el-form-item>
        <el-form-item label="参考价">
          <el-input-number v-model="form.steamReferencePrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import AdminImageUpload from '@/components/admin/AdminImageUpload.vue'
import { addItem, deleteItem, getAllItems, updateItem } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const editorVisible = ref(false)
const query = reactive({ page: 1, size: 20, keyword: '', category: '', isActive: null })
const form = reactive({})

const money = (value) => {
  if (value == null || value === '') return '-'
  return `¥ ${Number(value).toFixed(2)}`
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await getAllItems(query)
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

const openEditor = (row = {}) => {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, { isActive: 1 }, row)
  editorVisible.value = true
}

const saveItem = async () => {
  if (form.id) {
    await updateItem(form.id, form)
  } else {
    await addItem(form)
  }
  ElMessage.success('饰品保存成功')
  editorVisible.value = false
  loadData()
}

const removeItem = async (row) => {
  await ElMessageBox.confirm(`确定删除饰品“${row.nameCn || row.name}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteItem(row.id)
  ElMessage.success('饰品已删除')
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
  grid-template-columns: minmax(220px, 1fr) 140px 140px auto auto;
  gap: 12px;
  margin-bottom: 16px;
}

.item-icon {
  width: 52px;
  height: 52px;
  display: block;
}

.empty-image {
  color: #94a3b8;
  font-size: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 1000px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
