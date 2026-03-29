<template>
  <div class="user-messages">
    <!-- 消息类型Tab -->
    <div class="message-tabs">
      <div 
        class="tab" 
        :class="{ active: activeTab === 1 }" 
        @click="switchTab(1)"
      >
        交易消息
        <span class="unread-badge" v-if="unreadCount.trade > 0">{{ unreadCount.trade > 99 ? '99+' : unreadCount.trade }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 2 }" 
        @click="switchTab(2)"
      >
        系统消息
        <span class="unread-badge" v-if="unreadCount.system > 0">{{ unreadCount.system > 99 ? '99+' : unreadCount.system }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 3 }" 
        @click="switchTab(3)"
      >
        还价留言
        <span class="unread-badge" v-if="unreadCount.bargain > 0">{{ unreadCount.bargain > 99 ? '99+' : unreadCount.bargain }}</span>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="message-toolbar">
      <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
      <div class="toolbar-actions">
        <el-button type="primary" link @click="markSelectedRead" :disabled="selectedIds.length === 0">标记已读</el-button>
        <el-button type="primary" link @click="deleteSelected" :disabled="selectedIds.length === 0">删除</el-button>
        <el-button type="primary" link @click="markAllRead">全部已读</el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" v-loading="loading">
      <!-- 交易消息 -->
      <template v-if="activeTab === 1">
        <div 
          class="message-item" 
          v-for="msg in messages" 
          :key="msg.id"
          :class="{ unread: msg.status === 0 }"
        >
          <el-checkbox v-model="selectedIds" :value="msg.id" />
          <div class="message-icon" :class="getTradeIconClass(msg.subType)">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="message-content" @click="viewMessage(msg)">
            <div class="message-title">
              <span class="title-text">{{ msg.title }}</span>
              <el-tag size="small" :type="getTradeTagType(msg.subType)">{{ getTradeTagText(msg.subType) }}</el-tag>
            </div>
            <div class="message-desc">{{ msg.content }}</div>
            <div class="message-meta">
              <span class="order-no" v-if="msg.relatedOrderNo">订单号：{{ msg.relatedOrderNo }}</span>
              <span class="time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 系统消息 -->
      <template v-if="activeTab === 2">
        <div 
          class="message-item system" 
          v-for="msg in messages" 
          :key="msg.id"
          :class="{ unread: msg.status === 0 }"
        >
          <el-checkbox v-model="selectedIds" :value="msg.id" />
          <div class="message-icon system-icon">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="message-content" @click="viewMessage(msg)">
            <div class="message-title">
              <span class="title-text">{{ msg.title }}</span>
              <el-tag size="small" :type="getSystemTagType(msg.subType)">{{ getSystemTagText(msg.subType) }}</el-tag>
            </div>
            <div class="message-desc">{{ msg.content }}</div>
            <div class="message-meta">
              <span class="time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 还价留言 -->
      <template v-if="activeTab === 3">
        <div 
          class="message-item bargain" 
          v-for="msg in messages" 
          :key="msg.id"
          :class="{ unread: msg.status === 0 }"
        >
          <el-checkbox v-model="selectedIds" :value="msg.id" />
          <div class="message-icon bargain-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="message-content" @click="viewMessage(msg)">
            <div class="message-title">
              <span class="title-text">{{ msg.title }}</span>
              <span class="bargain-price">还价：¥{{ msg.bargainPrice }}</span>
            </div>
            <div class="message-desc">{{ msg.content }}</div>
            <div class="message-meta">
              <span class="item-name" v-if="msg.itemName">商品：{{ msg.itemName }}</span>
              <span class="time">{{ formatTime(msg.createdAt) }}</span>
            </div>
            <div class="bargain-actions" v-if="msg.status === 0">
              <el-button type="primary" size="small" @click.stop="acceptBargain(msg)">接受</el-button>
              <el-button size="small" @click.stop="rejectBargain(msg)">拒绝</el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div class="empty-state" v-if="!loading && messages.length === 0">
        <el-icon class="empty-icon"><Message /></el-icon>
        <div class="empty-text">暂无消息</div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination 
        v-model:current-page="currentPage" 
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchMessages"
      />
    </div>

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="showDetailDialog" :title="currentMessage?.title" width="500px">
      <div class="message-detail">
        <div class="detail-time">{{ formatTime(currentMessage?.createdAt) }}</div>
        <div class="detail-content">{{ currentMessage?.content }}</div>
        <div class="detail-order" v-if="currentMessage?.relatedOrderNo">
          <span>关联订单：</span>
          <el-link type="primary">{{ currentMessage?.relatedOrderNo }}</el-link>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getMessages, markAsRead, markAllRead as markAllReadApi, deleteMessages } from '@/api/message'
import { ShoppingCart, Bell, ChatDotRound, Message } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const messages = ref([])
const activeTab = ref(1)
const loading = ref(false)
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

const switchTab = (tab) => {
  activeTab.value = tab
  selectedIds.value = []
  selectAll.value = false
  currentPage.value = 1
  fetchMessages()
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const res = await getMessages(activeTab.value, currentPage.value, pageSize.value)
    messages.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await getMessages('unread-count')
    if (res.data) {
      unreadCount.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const handleSelectAll = (val) => {
  if (val) {
    selectedIds.value = messages.value.map(m => m.id)
  } else {
    selectedIds.value = []
  }
}

const markSelectedRead = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await markAsRead(selectedIds.value)
    messages.value.forEach(m => {
      if (selectedIds.value.includes(m.id)) {
        m.status = 1
      }
    })
    ElMessage.success('已标记为已读')
    fetchUnreadCount()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm('确定要删除选中的消息吗？', '提示', { type: 'warning' })
    await deleteMessages(selectedIds.value)
    messages.value = messages.value.filter(m => !selectedIds.value.includes(m.id))
    selectedIds.value = []
    selectAll.value = false
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const markAllRead = async () => {
  try {
    await markAllReadApi(activeTab.value)
    messages.value.forEach(m => m.status = 1)
    ElMessage.success('已全部标记为已读')
    fetchUnreadCount()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const viewMessage = (msg) => {
  currentMessage.value = msg
  showDetailDialog.value = true
  if (msg.status === 0) {
    markAsRead([msg.id])
    msg.status = 1
    fetchUnreadCount()
  }
}

const acceptBargain = async (msg) => {
  try {
    await ElMessageBox.confirm('确定接受此还价吗？', '提示')
    ElMessage.success('已接受还价')
    msg.status = 1
  } catch (error) {
    // cancelled
  }
}

const rejectBargain = async (msg) => {
  try {
    await ElMessageBox.confirm('确定拒绝此还价吗？', '提示')
    ElMessage.success('已拒绝还价')
    msg.status = 1
  } catch (error) {
    // cancelled
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  const diff = now.diff(date, 'day')
  
  if (diff === 0) {
    return date.format('HH:mm')
  } else if (diff === 1) {
    return '昨天 ' + date.format('HH:mm')
  } else if (diff < 7) {
    return diff + '天前'
  } else {
    return date.format('MM-DD HH:mm')
  }
}

const getTradeIconClass = (subType) => {
  const classMap = {
    1: 'icon-buy',
    2: 'icon-sell',
    3: 'icon-complete',
    4: 'icon-cancel',
    5: 'icon-refund'
  }
  return classMap[subType] || 'icon-default'
}

const getTradeTagType = (subType) => {
  const typeMap = {
    1: 'success',
    2: 'primary',
    3: 'success',
    4: 'info',
    5: 'warning'
  }
  return typeMap[subType] || ''
}

const getTradeTagText = (subType) => {
  const textMap = {
    1: '购买',
    2: '出售',
    3: '完成',
    4: '取消',
    5: '退款'
  }
  return textMap[subType] || '交易'
}

const getSystemTagType = (subType) => {
  const typeMap = {
    1: 'danger',
    2: 'warning',
    3: 'success',
    4: 'info'
  }
  return typeMap[subType] || ''
}

const getSystemTagText = (subType) => {
  const textMap = {
    1: '重要',
    2: '活动',
    3: '通知',
    4: '公告'
  }
  return textMap[subType] || '系统'
}

onMounted(() => {
  fetchMessages()
  fetchUnreadCount()
})
</script>

<style scoped>
.user-messages {
  background: #fff;
  min-height: 100%;
}

.message-tabs {
  background: #232631;
  display: flex;
  padding: 0 20px;
}

.tab {
  padding: 15px 30px;
  color: #868a9f;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.tab.active {
  color: #fff;
  background: #2a3040;
  border-top: 2px solid #4b89dc;
}

.tab:hover {
  color: #fff;
}

.unread-badge {
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.message-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f7f8fa;
  border-bottom: 1px solid #eee;
}

.toolbar-actions {
  display: flex;
  gap: 15px;
}

.message-list {
  min-height: 400px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
  transition: background 0.2s;
}

.message-item:hover {
  background: #fafafa;
}

.message-item.unread {
  background: #f0f9ff;
}

.message-item.unread:hover {
  background: #e6f4ff;
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.message-icon.icon-buy {
  background: #e1f3d8;
  color: #67c23a;
}

.message-icon.icon-sell {
  background: #d9ecff;
  color: #409eff;
}

.message-icon.icon-complete {
  background: #e1f3d8;
  color: #67c23a;
}

.message-icon.icon-cancel {
  background: #f4f4f5;
  color: #909399;
}

.message-icon.icon-refund {
  background: #fdf6ec;
  color: #e6a23c;
}

.message-icon.system-icon {
  background: #fef0f0;
  color: #f56c6c;
}

.message-icon.bargain-icon {
  background: #f0f9ff;
  color: #409eff;
}

.message-content {
  flex: 1;
  cursor: pointer;
}

.message-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.title-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.message-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 12px;
  color: #999;
}

.order-no {
  color: #409eff;
}

.bargain-price {
  color: #f56c6c;
  font-weight: 600;
}

.item-name {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bargain-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: center;
}

/* 消息详情弹窗 */
.message-detail {
  padding: 10px 0;
}

.detail-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 16px;
}

.detail-content {
  font-size: 14px;
  color: #333;
  line-height: 1.8;
}

.detail-order {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  font-size: 13px;
  color: #666;
}
</style>
