<template>
  <div class="user-submissions">
    <div class="submissions-header">
      <h2>我的投稿</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        发布文章
      </el-button>
    </div>

    <div class="submissions-tabs">
      <div 
        class="tab" 
        :class="{ active: activeTab === 'all' }" 
        @click="activeTab = 'all'"
      >
        全部
        <span class="count" v-if="totalCount > 0">({{ totalCount }})</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'pending' }" 
        @click="activeTab = 'pending'"
      >
        待审核
        <span class="badge warning" v-if="pendingCount > 0">{{ pendingCount }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'approved' }" 
        @click="activeTab = 'approved'"
      >
        已通过
        <span class="badge success" v-if="approvedCount > 0">{{ approvedCount }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'rejected' }" 
        @click="activeTab = 'rejected'"
      >
        已拒绝
        <span class="badge danger" v-if="rejectedCount > 0">{{ rejectedCount }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'draft' }" 
        @click="activeTab = 'draft'"
      >
        草稿箱
        <span class="badge info" v-if="draftCount > 0">{{ draftCount }}</span>
      </div>
    </div>

    <div class="content-area" v-loading="loading">
      <div class="submission-list" v-if="filteredSubmissions.length > 0">
        <div class="submission-card" v-for="news in filteredSubmissions" :key="news.id">
          <div class="submission-cover">
            <img :src="news.coverImage" v-if="news.coverImage" />
            <div class="no-cover" v-else>
              <el-icon><Document /></el-icon>
            </div>
          </div>
          <div class="submission-content">
            <div class="submission-title">{{ news.title }}</div>
            <div class="submission-summary">{{ news.summary || '暂无摘要' }}</div>
            <div class="submission-meta">
              <span class="category">
                <el-icon><Folder /></el-icon>
                {{ news.category || '未分类' }}
              </span>
              <span class="views">
                <el-icon><View /></el-icon>
                {{ news.views || 0 }} 阅读
              </span>
              <span class="date">
                <el-icon><Clock /></el-icon>
                {{ formatDate(news.createdAt) }}
              </span>
            </div>
          </div>
          <div class="submission-status">
            <el-tag :type="getStatusType(news.status)" size="small">
              {{ getStatusText(news.status) }}
            </el-tag>
            <div class="status-reason" v-if="news.status === 2 && news.rejectReason">
              拒绝原因：{{ news.rejectReason }}
            </div>
          </div>
          <div class="submission-actions">
            <el-button type="primary" size="small" @click="editNews(news)" v-if="news.status === 0 || news.status === 2">
              编辑
            </el-button>
            <el-button size="small" @click="viewNews(news)" v-if="news.status === 1">
              查看
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(news)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div class="empty-state" v-else>
        <el-icon class="empty-icon"><EditPen /></el-icon>
        <div class="empty-text">
          {{ activeTab === 'pending' ? '暂无待审核稿件' : 
             activeTab === 'approved' ? '暂无已通过稿件' : 
             activeTab === 'rejected' ? '暂无被拒绝稿件' : 
             activeTab === 'draft' ? '草稿箱为空' : '暂无投稿' }}
        </div>
        <el-button type="primary" @click="showCreateDialog = true">发布文章</el-button>
      </div>
    </div>

    <!-- 发布/编辑文章弹窗 -->
    <el-dialog 
      v-model="showCreateDialog" 
      :title="editingNews ? '编辑文章' : '发布文章'" 
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="newsForm" label-width="80px" :rules="newsRules" ref="newsFormRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="newsForm.title" placeholder="请输入文章标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="newsForm.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="游戏资讯" value="游戏资讯" />
            <el-option label="饰品攻略" value="饰品攻略" />
            <el-option label="市场分析" value="市场分析" />
            <el-option label="玩家心得" value="玩家心得" />
            <el-option label="官方公告" value="官方公告" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            class="cover-uploader"
            action=""
            :http-request="uploadCover"
            :show-file-list="false"
            accept="image/*"
          >
            <img v-if="newsForm.coverImage" :src="newsForm.coverImage" class="cover-preview" />
            <div v-else class="cover-placeholder">
              <el-icon><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input 
            v-model="newsForm.summary" 
            type="textarea" 
            :rows="2"
            placeholder="请输入文章摘要（选填）" 
            maxlength="200" 
            show-word-limit 
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input 
            v-model="newsForm.content" 
            type="textarea" 
            :rows="8"
            placeholder="请输入文章内容" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button @click="saveDraft" v-if="!editingNews">保存草稿</el-button>
        <el-button type="primary" @click="submitNews" :loading="submitLoading">
          {{ editingNews ? '保存' : '提交审核' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getMyNews, createNews, updateNews, deleteNews as deleteNewsApi } from '@/api/news'
import { Plus, Document, Folder, View, Clock, EditPen } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('all')
const submissions = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const submitLoading = ref(false)
const editingNews = ref(null)
const newsFormRef = ref(null)

const newsForm = ref({
  title: '',
  category: '',
  coverImage: '',
  summary: '',
  content: ''
})

const newsRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}

const totalCount = computed(() => submissions.value.length)
const pendingCount = computed(() => submissions.value.filter(n => n.status === 0).length)
const approvedCount = computed(() => submissions.value.filter(n => n.status === 1).length)
const rejectedCount = computed(() => submissions.value.filter(n => n.status === 2).length)
const draftCount = computed(() => submissions.value.filter(n => n.status === 3).length)

const filteredSubmissions = computed(() => {
  if (activeTab.value === 'all') return submissions.value
  if (activeTab.value === 'pending') return submissions.value.filter(n => n.status === 0)
  if (activeTab.value === 'approved') return submissions.value.filter(n => n.status === 1)
  if (activeTab.value === 'rejected') return submissions.value.filter(n => n.status === 2)
  if (activeTab.value === 'draft') return submissions.value.filter(n => n.status === 3)
  return submissions.value
})

const fetchSubmissions = async () => {
  loading.value = true
  try {
    const res = await getMyNews()
    submissions.value = res || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝',
    3: '草稿'
  }
  return texts[status] || '未知'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const uploadCover = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const data = await request.post('/v1/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    newsForm.value.coverImage = data
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const submitNews = async () => {
  if (!newsFormRef.value) return
  
  await newsFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (editingNews.value) {
        const data = { ...newsForm.value, status: 0 }
        await updateNews(editingNews.value.id, data)
        ElMessage.success('保存成功')
      } else {
        const data = { ...newsForm.value, status: 0 }
        await createNews(data)
        ElMessage.success('提交成功，等待审核')
      }
      showCreateDialog.value = false
      resetForm()
      fetchSubmissions()
    } catch (error) {
      ElMessage.error('提交失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const saveDraft = async () => {
  submitLoading.value = true
  try {
    const data = { ...newsForm.value, status: 3 }
    await createNews(data)
    ElMessage.success('草稿保存成功')
    showCreateDialog.value = false
    resetForm()
    fetchSubmissions()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const editNews = (news) => {
  editingNews.value = news
  newsForm.value = {
    title: news.title,
    category: news.category,
    coverImage: news.coverImage,
    summary: news.summary,
    content: news.content
  }
  showCreateDialog.value = true
}

const viewNews = (news) => {
  router.push(`/news/${news.id}`)
}

const handleDelete = async (news) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', { type: 'warning' })
    await deleteNewsApi(news.id)
    submissions.value = submissions.value.filter(n => n.id !== news.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetForm = () => {
  editingNews.value = null
  newsForm.value = {
    title: '',
    category: '',
    coverImage: '',
    summary: '',
    content: ''
  }
}

onMounted(() => {
  fetchSubmissions()
})
</script>

<style scoped>
.user-submissions {
  background: #fff;
  min-height: 100%;
}

.submissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
}

.submissions-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.submissions-tabs {
  display: flex;
  gap: 10px;
  padding: 15px 25px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 16px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  transition: all 0.2s;
}

.tab.active {
  background: #4b89dc;
  color: #fff;
}

.tab:hover:not(.active) {
  background: #e8e8e8;
}

.tab .count {
  font-size: 12px;
  opacity: 0.8;
}

.tab .badge {
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

.tab .badge.warning { background: #e6a23c; }
.tab .badge.success { background: #67c23a; }
.tab .badge.danger { background: #f56c6c; }
.tab .badge.info { background: #909399; }

.content-area {
  padding: 20px 25px;
  min-height: 400px;
}

.submission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submission-card {
  display: flex;
  gap: 15px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
  padding: 15px;
  transition: all 0.2s;
}

.submission-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #ddd;
}

.submission-cover {
  width: 120px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.submission-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 32px;
}

.submission-content {
  flex: 1;
  min-width: 0;
}

.submission-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submission-summary {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.submission-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 12px;
  color: #999;
}

.submission-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.submission-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.status-reason {
  font-size: 11px;
  color: #f56c6c;
  max-width: 150px;
  text-align: right;
}

.submission-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

/* 封面上传 */
.cover-uploader {
  width: 200px;
  height: 120px;
}

.cover-uploader :deep(.el-upload) {
  width: 100%;
  height: 100%;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-uploader :deep(.el-upload:hover) {
  border-color: #4b89dc;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  gap: 8px;
}

.cover-placeholder .el-icon {
  font-size: 24px;
}
</style>
