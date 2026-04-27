<template>
  <div class="messages-page">
    <SiteHeader />

    <main class="page-main">
      <section class="page-panel">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">消息中心</div>
            <button class="tab-item plain tab-button" :class="{ current: activeTab === 1 }" @click="switchTab(1)">
              交易消息
            </button>
            <button class="tab-item plain tab-button" :class="{ current: activeTab === 2 }" @click="switchTab(2)">
              系统消息
            </button>
            <button class="tab-item plain tab-button" :class="{ current: activeTab === 3 }" @click="switchTab(3)">
              还价留言
            </button>
          </div>
          <div class="section-more">未读 {{ unreadTotal }}</div>
        </div>

        <div class="toolbar">
          <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
          <div class="toolbar-actions">
            <el-button link type="primary" :disabled="selectedIds.length === 0" @click="markSelectedRead">标记已读</el-button>
            <el-button link type="primary" :disabled="selectedIds.length === 0" @click="deleteSelected">删除</el-button>
            <el-button link type="primary" @click="markEverythingRead">全部已读</el-button>
          </div>
        </div>

        <div class="message-list" v-loading="loading">
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-item"
            :class="{ unread: message.status === 0 }"
          >
            <el-checkbox v-model="selectedIds" :value="message.id" />

            <div class="message-icon" :class="getMessageIconClass(message)">
              <el-icon v-if="activeTab === 1"><ShoppingCart /></el-icon>
              <el-icon v-else-if="activeTab === 2"><Bell /></el-icon>
              <el-icon v-else><ChatDotRound /></el-icon>
            </div>

            <div class="message-body" @click="viewMessage(message)">
              <div class="message-title-row">
                <span class="message-title">{{ message.title || getFallbackTitle(message) }}</span>
                <el-tag size="small" :type="getTagType(message)">{{ getTagText(message) }}</el-tag>
              </div>

              <div class="message-content">{{ message.content || '暂无消息内容' }}</div>

              <div class="message-meta">
                <span v-if="message.itemName">饰品：{{ message.itemName }}</span>
                <span v-if="message.bargainPrice && activeTab === 3">还价：¥{{ formatPrice(message.bargainPrice) }}</span>
                <span v-if="message.relatedOrderNo">订单号：{{ message.relatedOrderNo }}</span>
                <span>{{ formatTime(message.createdAt) }}</span>
              </div>

              <div v-if="activeTab === 3 && isPendingBargain(message)" class="bargain-actions">
                <el-button type="primary" size="small" @click.stop="handleAcceptBargain(message)">接受</el-button>
                <el-button size="small" @click.stop="handleRejectBargain(message)">拒绝</el-button>
              </div>
            </div>
          </div>

          <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
        </div>

        <div v-if="total > pageSize" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="fetchMessages"
          />
        </div>
      </section>
    </main>

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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, ChatDotRound, ShoppingCart } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  acceptBargain as acceptBargainApi,
  deleteMessages,
  getMessages,
  getUnreadCount,
  markAllRead,
  markAsRead,
  rejectBargain as rejectBargainApi
} from '@/api/message'
import SiteHeader from '@/components/SiteHeader.vue'

const route = useRoute()
const router = useRouter()
const activeTab = ref(1)
const loading = ref(false)
const messages = ref([])
const selectAll = ref(false)
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailDialog = ref(false)
const currentMessage = ref(null)

const unreadCount = ref({
  trade: 0,
  system: 0,
  bargain: 0
})

const unreadTotal = computed(
  () => Number(unreadCount.value.trade || 0) + Number(unreadCount.value.system || 0) + Number(unreadCount.value.bargain || 0)
)

const normalizeMessageList = (payload) => {
  if (Array.isArray(payload)) {
    return { list: payload, total: payload.length }
  }
  if (payload?.list) {
    return { list: payload.list, total: Number(payload.total || payload.list.length || 0) }
  }
  if (payload?.data?.list) {
    return { list: payload.data.list, total: Number(payload.data.total || payload.data.list.length || 0) }
  }
  return { list: [], total: 0 }
}

const normalizeMessageTab = (value) => {
  const numeric = Number(value)
  return [1, 2, 3].includes(numeric) ? numeric : 1
}

const applyMessageTab = (tab) => {
  activeTab.value = tab
  selectedIds.value = []
  selectAll.value = false
  currentPage.value = 1
}

const updateMessageTabQuery = async (tab) => {
  const normalized = normalizeMessageTab(tab)
  if (normalizeMessageTab(route.query.tab) === normalized) return
  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: String(normalized)
    }
  })
}

const switchTab = async (tab) => {
  try {
    await updateMessageTabQuery(tab)
  } catch (error) {
    ElMessage.error(error?.message || '切换消息分类失败')
  }
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const payload = await getMessages(activeTab.value, currentPage.value, pageSize.value)
    const { list, total: count } = normalizeMessageList(payload)
    messages.value = list
    total.value = count
  } catch (error) {
    ElMessage.error(error?.message || '获取消息失败')
  } finally {
    loading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const payload = await getUnreadCount()
    unreadCount.value = payload || { trade: 0, system: 0, bargain: 0 }
  } catch (error) {
    unreadCount.value = { trade: 0, system: 0, bargain: 0 }
  }
}

const handleSelectAll = (checked) => {
  selectedIds.value = checked ? messages.value.map((item) => item.id) : []
}

const markSelectedRead = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await markAsRead(selectedIds.value)
    messages.value.forEach((item) => {
      if (selectedIds.value.includes(item.id)) {
        item.status = 1
      }
    })
    ElMessage.success('已标记为已读')
    fetchUnreadCount()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm('确定要删除选中的消息吗？', '提示', { type: 'warning' })
    await deleteMessages(selectedIds.value)
    messages.value = messages.value.filter((item) => !selectedIds.value.includes(item.id))
    selectedIds.value = []
    selectAll.value = false
    ElMessage.success('删除成功')
    fetchUnreadCount()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const markEverythingRead = async () => {
  try {
    await markAllRead(activeTab.value)
    messages.value = messages.value.map((item) => ({ ...item, status: 1 }))
    ElMessage.success('已全部标记为已读')
    fetchUnreadCount()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const viewMessage = async (message) => {
  currentMessage.value = message
  showDetailDialog.value = true
  if (message.status === 0) {
    try {
      await markAsRead([message.id])
      message.status = 1
      fetchUnreadCount()
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
    fetchUnreadCount()
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
    fetchUnreadCount()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '拒绝还价失败')
    }
  }
}

const getMessageIconClass = (message) => {
  if (activeTab.value === 2) return 'system-icon'
  if (activeTab.value === 3) return 'bargain-icon'
  return message.status === 0 ? 'trade-unread' : 'trade-read'
}

const getTagType = (message) => {
  if (activeTab.value === 2) {
    const systemTagTypeMap = {
      1: 'danger',
      2: 'success',
      3: 'info',
      4: 'primary'
    }
    return systemTagTypeMap[Number(message?.subType || 0)] || 'info'
  }
  if (activeTab.value === 3) {
    const bargainTagTypeMap = {
      1: 'warning',
      2: 'success',
      3: 'info'
    }
    return bargainTagTypeMap[Number(message?.subType || 0)] || 'info'
  }
  const tagMap = {
    1: 'primary',
    2: 'success',
    3: 'success',
    4: 'info',
    5: 'warning'
  }
  return tagMap[message.subType] || 'info'
}

const getTagText = (message) => {
  if (activeTab.value === 2) {
    const systemTagTextMap = {
      1: '重要',
      2: '活动',
      3: '通知',
      4: '公告'
    }
    return systemTagTextMap[Number(message?.subType || 0)] || '系统'
  }
  if (activeTab.value === 3) {
    const bargainTagTextMap = {
      1: '待处理',
      2: '已接受',
      3: '已拒绝'
    }
    return bargainTagTextMap[Number(message?.subType || 0)] || '还价'
  }
  const tagMap = {
    1: '购买',
    2: '出售',
    3: '完成',
    4: '取消',
    5: '退款'
  }
  return tagMap[message.subType] || '交易'
}

const getFallbackTitle = (message) => {
  if (activeTab.value === 2) return '系统通知'
  if (activeTab.value === 3) return '还价留言'
  return `交易消息 #${message.id}`
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'day')
  if (diff === 0) return date.format('HH:mm')
  if (diff === 1) return `昨天 ${date.format('HH:mm')}`
  if (diff < 7) return `${diff} 天前`
  return date.format('MM-DD HH:mm')
}

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : '0.00'
}

watch(
  () => route.query.tab,
  (value) => {
    const normalized = normalizeMessageTab(value)
    applyMessageTab(normalized)
    fetchMessages()
  },
  { immediate: true }
)

onMounted(() => {
  fetchUnreadCount()
})
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 36px;
}

.page-panel {
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ececec;
  border-bottom: 1px solid #e2e2e2;
}

.tab-group {
  display: flex;
  align-items: center;
  gap: 36px;
}

.tab-item {
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 0 22px;
  color: #666f7d;
  font-size: 16px;
  font-weight: 600;
}

.tab-item.active-blue {
  background: #4e86dc;
  color: #fff;
}

.tab-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.tab-button.current {
  color: #111827;
}

.section-more {
  padding-right: 24px;
  color: #9299a7;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px;
  border-bottom: 1px solid #f0f1f4;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-list {
  padding: 18px 24px 24px;
}

.message-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 18px 0;
  border-bottom: 1px solid #eff2f6;
}

.message-item.unread {
  background: linear-gradient(90deg, rgba(78, 134, 220, 0.05), transparent);
}

.message-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #fff;
  flex-shrink: 0;
}

.trade-unread,
.trade-read {
  background: #4e86dc;
}

.system-icon {
  background: #f59e0b;
}

.bargain-icon {
  background: #10b981;
}

.message-body {
  flex: 1;
  cursor: pointer;
}

.message-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.message-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.message-content {
  color: #4b5563;
  line-height: 1.65;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 10px;
  color: #9ca3af;
  font-size: 13px;
}

.bargain-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 0 0 28px;
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

@media (max-width: 768px) {
  .section-header,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .tab-group {
    gap: 0;
    flex-wrap: wrap;
  }

  .message-item {
    flex-wrap: wrap;
  }

  .message-title-row {
    flex-wrap: wrap;
  }
}
</style>
