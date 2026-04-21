<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索标题、内容、订单号" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.type" placeholder="消息类型" clearable>
          <el-option label="交易消息" :value="1" />
          <el-option label="系统消息" :value="2" />
          <el-option label="还价消息" :value="3" />
        </el-select>
        <el-select v-model="query.status" placeholder="已读状态" clearable>
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Plus" @click="sendVisible = true">发系统消息</el-button>
        <el-button type="danger" :disabled="!selected.length" @click="batchRemove">批量删除</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" row-key="id" @selection-change="selected = $event">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100"><template #default="{ row }">{{ typeText(row.type) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }">{{ row.status === 1 ? '已读' : '未读' }}</template></el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :disabled="row.status === 1" @click="read(row)">标记已读</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="query.page" :page-size="query.size" :total="total"
        @current-change="changePage" @size-change="changeSize" />
    </el-card>

    <el-dialog v-model="sendVisible" title="发送系统消息" width="520px">
      <el-form :model="messageForm" label-width="90px">
        <el-form-item label="用户编号"><el-input-number v-model="messageForm.userId" :min="1" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="messageForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="messageForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { batchDeleteMessages, getMessages, markMessageRead, sendSystemMessage } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const selected = ref([])
const sendVisible = ref(false)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, type: null, status: null })
const messageForm = reactive({ userId: null, title: '', content: '' })
const typeText = (type) => ({ 1: '交易消息', 2: '系统消息', 3: '还价消息' }[type] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const data = await getMessages(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }
const read = async (row) => { await markMessageRead(row.id); ElMessage.success('已标记'); loadData() }
const batchRemove = async () => {
  await ElMessageBox.confirm(`确定删除 ${selected.value.length} 条消息吗？`, '删除确认', { type: 'warning' })
  await batchDeleteMessages(selected.value.map((item) => item.id))
  ElMessage.success('删除成功')
  loadData()
}
const sendMessage = async () => {
  await sendSystemMessage(messageForm)
  ElMessage.success('发送成功')
  sendVisible.value = false
  Object.assign(messageForm, { userId: null, title: '', content: '' })
  loadData()
}
onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 120px 130px 130px auto auto auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 1100px) { .toolbar { grid-template-columns: 1fr; } }
</style>
