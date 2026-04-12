<template>
  <div class="bargain-tab">
    <div class="toolbar-card">
      <div class="toolbar-top toolbar-top-right">
        <div class="toolbar-actions">
          <span class="unread-indicator">未读 {{ unreadCount }}</span>
          <el-button @click="fetchMessages">刷新</el-button>
          <el-button link type="primary" :disabled="selectedIds.length === 0" @click="markSelectedRead">标记已读</el-button>
          <el-button link type="primary" :disabled="selectedIds.length === 0" @click="deleteSelected">删除</el-button>
          <el-button link type="primary" @click="markEverythingRead">全部已读</el-button>
        </div>
      </div>

      <div class="toolbar-filters">
        <div class="filter-group">
          <el-select v-model="filters.quality" placeholder="品质" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in qualityOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.type" placeholder="类别" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in typeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.exterior" placeholder="外观" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in exteriorOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="filters.status" placeholder="还价状态" clearable class="toolbar-select">
            <el-option label="不限" value="" />
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="search-group">
          <el-input
            v-model="filters.search"
            placeholder="输入物品名称"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="pagedMessages"
        class="bargain-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />

        <el-table-column label="饰品" min-width="260">
          <template #default="{ row }">
            <div class="item-cell">
              <img :src="getMessageImage(row)" :alt="getItemName(row)" class="item-image" />
              <div class="item-copy">
                <div class="item-name">{{ getItemName(row) }}</div>
                <div class="item-subtitle">{{ getMessageSubtitle(row) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="还价" width="140">
          <template #default="{ row }">
            <div class="price-copy">¥ {{ formatPrice(row.bargainPrice) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="还价信息" min-width="280">
          <template #default="{ row }">
            <div class="info-copy">
              <div class="info-title">{{ row.title || '还价留言' }}</div>
              <div class="info-meta" v-if="row.relatedOrderNo">订单号：{{ row.relatedOrderNo }}</div>
              <div class="info-summary">{{ row.content || '暂无留言内容' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <div class="time-copy">{{ formatTime(row.createdAt) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" min-width="260">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="getStatusType(row)">{{ getStatusText(row) }}</el-tag>
              <div class="status-meta" :class="{ unread: Number(row.status) === 0 }">
                {{ Number(row.status) === 0 ? '未读' : '已读' }}
              </div>
              <div class="status-actions">
                <el-button
                  v-if="isPendingBargain(row)"
                  type="primary"
                  size="small"
                  @click="handleAcceptBargain(row)"
                >
                  接受
                </el-button>
                <el-button
                  v-if="isPendingBargain(row)"
                  size="small"
                  @click="handleRejectBargain(row)"
                >
                  拒绝
                </el-button>
                <el-button size="small" @click="viewMessage(row)">查看详情</el-button>
              </div>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-wrap">
            <div class="empty-mark">暂无数据</div>
          </div>
        </template>
      </el-table>

      <div v-if="filteredMessages.length > pageSize" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredMessages.length"
          layout="prev, pager, next"
        />
      </div>
    </div>

    <el-dialog v-model="showDetailDialog" :title="currentMessage?.title || '消息详情'" width="520px">
      <div class="message-detail">
        <div class="detail-time">{{ formatTime(currentMessage?.createdAt) }}</div>
        <div class="detail-content">{{ currentMessage?.content || '暂无详情内容' }}</div>
        <div v-if="currentMessage?.itemName" class="detail-order">饰品：{{ currentMessage.itemName }}</div>
        <div v-if="currentMessage?.relatedOrderNo" class="detail-order">订单号：{{ currentMessage.relatedOrderNo }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  acceptBargain as acceptBargainApi,
  deleteMessages,
  getMessages,
  markAllRead,
  markAsRead,
  rejectBargain as rejectBargainApi
} from '@/api/message'
import { QUALITY_TEXT_MAP, TYPE_TEXT_MAP, normalizeQualityKey } from '@/utils/itemClassification'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const loading = ref(false)
const hasLoaded = ref(false)
const messages = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = 10
const showDetailDialog = ref(false)
const currentMessage = ref(null)

const filters = ref({
  quality: '',
  type: '',
  exterior: '',
  status: '',
  search: ''
})

const qualityOptions = Object.entries(QUALITY_TEXT_MAP).map(([value, label]) => ({ value, label }))
const typeOptions = Object.entries(TYPE_TEXT_MAP).map(([value, label]) => ({ value, label }))
const exteriorOptions = [
  { value: 'FN', label: '崭新出厂' },
  { value: 'MW', label: '略有磨损' },
  { value: 'FT', label: '久经沙场' },
  { value: 'WW', label: '破损不堪' },
  { value: 'BS', label: '战痕累累' },
  { value: 'NoPaint', label: '无涂装' }
]
const statusOptions = [
  { value: '1', label: '待处理' },
  { value: '2', label: '已接受' },
  { value: '3', label: '已拒绝' }
]

const getMessageDisplayModel = (message) => getItemDisplayModel({ name: message?.itemName || '', description: message?.content || '' })

const unreadCount = computed(() => messages.value.filter((message) => Number(message.status) === 0).length)

const filteredMessages = computed(() => {
  const keyword = filters.value.search.trim().toLowerCase()

  return messages.value.filter((message) => {
    const displayModel = getMessageDisplayModel(message)
    const itemName = String(message.itemName || '').toLowerCase()
    const title = String(message.title || '').toLowerCase()
    const content = String(message.content || '').toLowerCase()
    const orderNo = String(message.relatedOrderNo || '').toLowerCase()

    const matchKeyword =
      !keyword ||
      itemName.includes(keyword) ||
      title.includes(keyword) ||
      content.includes(keyword) ||
      orderNo.includes(keyword)
    const matchQuality =
      !filters.value.quality || displayModel.resolvedQuality === normalizeQualityKey(filters.value.quality)
    const matchType = !filters.value.type || displayModel.resolvedType === filters.value.type
    const matchExterior = !filters.value.exterior || displayModel.filterExterior === filters.value.exterior
    const matchStatus = !filters.value.status || String(message.subType || '') === filters.value.status

    return matchKeyword && matchQuality && matchType && matchExterior && matchStatus
  })
})

const pagedMessages = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMessages.value.slice(start, start + pageSize)
})

const normalizeMessageList = (payload) => {
  if (Array.isArray(payload)) {
    return payload
  }
  if (payload?.list) {
    return payload.list
  }
  if (payload?.data?.list) {
    return payload.data.list
  }
  return []
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const payload = await getMessages(3, 1, 500)
    messages.value = normalizeMessageList(payload)
    hasLoaded.value = true
  } catch (error) {
    ElMessage.error(error?.message || '获取还价留言失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

const markSelectedRead = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await markAsRead(selectedIds.value)
    messages.value = messages.value.map((message) =>
      selectedIds.value.includes(message.id) ? { ...message, status: 1 } : message
    )
    ElMessage.success('已标记为已读')
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm('确定要删除选中的消息吗？', '提示', { type: 'warning' })
    await deleteMessages(selectedIds.value)
    messages.value = messages.value.filter((message) => !selectedIds.value.includes(message.id))
    selectedIds.value = []
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const markEverythingRead = async () => {
  try {
    await markAllRead(3)
    messages.value = messages.value.map((message) => ({ ...message, status: 1 }))
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const viewMessage = async (message) => {
  currentMessage.value = message
  showDetailDialog.value = true
  if (Number(message.status) === 0) {
    try {
      await markAsRead([message.id])
      message.status = 1
    } catch (error) {
      void error
    }
  }
}

const isPendingBargain = (message) => Number(message?.subType || 0) === 1

const handleAcceptBargain = async (message) => {
  try {
    await ElMessageBox.confirm('确定接受这条还价留言吗？', '提示')
    const result = await acceptBargainApi(message.id)
    message.status = 1
    message.subType = 2
    message.relatedOrderNo = result?.orderNo || message.relatedOrderNo
    ElMessage.success('已接受还价，订单已创建')
    await fetchMessages()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '接受还价失败')
    }
  }
}

const handleRejectBargain = async (message) => {
  try {
    await ElMessageBox.confirm('确定拒绝这条还价留言吗？', '提示')
    await rejectBargainApi(message.id)
    message.status = 1
    message.subType = 3
    ElMessage.success('已拒绝还价')
    await fetchMessages()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '拒绝还价失败')
    }
  }
}

const getStatusType = (message) => {
  const typeMap = {
    1: 'warning',
    2: 'success',
    3: 'info'
  }
  return typeMap[Number(message?.subType || 0)] || 'info'
}

const getStatusText = (message) => {
  const textMap = {
    1: '待处理',
    2: '已接受',
    3: '已拒绝'
  }
  return textMap[Number(message?.subType || 0)] || '还价'
}

const getItemName = (message) => message?.itemName || '未知饰品'
const getMessageImage = (message) => message?.itemIconUrl || message?.item?.iconUrl || '/default-item.png'
const getMessageSubtitle = (message) => getMessageDisplayModel(message).subtitle || '还价留言'

const formatTime = (time) => {
  if (!time) return '-'
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'day')
  if (diff === 0) return date.format('HH:mm')
  if (diff === 1) return `昨天 ${date.format('HH:mm')}`
  if (diff < 7) return `${diff} 天前`
  return date.format('YYYY-MM-DD HH:mm:ss')
}

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : '0.00'
}

const handleSearch = () => {
  filters.value.search = filters.value.search.trim()
  currentPage.value = 1
}

watch(filteredMessages, () => {
  if ((currentPage.value - 1) * pageSize >= filteredMessages.value.length && currentPage.value > 1) {
    currentPage.value = 1
  }
})

watch(
  () => props.active,
  (active) => {
    if (active && !hasLoaded.value) {
      fetchMessages()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.bargain-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.toolbar-card,
.table-card {
  border: 1px solid #e4e8f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.toolbar-card {
  padding: 24px;
}

.toolbar-top,
.toolbar-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-filters {
  margin-top: 18px;
}

.toolbar-top-right {
  justify-content: flex-end;
}

.toolbar-actions,
.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 150px;
}

.unread-indicator {
  color: #475569;
  font-size: 14px;
}

.search-group {
  min-width: 300px;
  flex: 1;
  max-width: 420px;
}

.search-input {
  width: 100%;
}

.table-card {
  overflow: hidden;
}

:deep(.bargain-table th) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}

:deep(.bargain-table td),
:deep(.bargain-table th) {
  padding: 18px 0;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-image {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 10px;
  background: linear-gradient(180deg, #eef2f7 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}

.item-name,
.price-copy,
.info-title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.item-subtitle,
.info-meta,
.info-summary,
.time-copy,
.status-meta {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.info-summary {
  line-height: 1.5;
}

.status-meta.unread {
  color: #2563eb;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 18px 0 24px;
}

.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  color: #cbd5e1;
  font-size: 32px;
  font-weight: 700;
}

.message-detail {
  color: #374151;
}

.detail-time {
  margin-bottom: 14px;
  color: #9ca3af;
}

.detail-content {
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-order {
  margin-top: 18px;
  color: #4e86dc;
}

@media (max-width: 1024px) {
  .toolbar-top,
  .toolbar-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .search-group {
    max-width: none;
  }
}
</style>
