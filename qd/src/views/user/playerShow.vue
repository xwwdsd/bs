<template>
  <div class="player-show-manage">
    <div class="manage-header">
      <h2>玩家秀管理</h2>
      <el-button type="primary" @click="showUploadDialog = true">
        <el-icon><Plus /></el-icon>
        发布玩家秀
      </el-button>
    </div>

    <div class="content-area" v-loading="loading">
      <div class="show-grid" v-if="shows.length > 0">
        <div class="show-card" v-for="show in shows" :key="show.id">
          <div class="show-image">
            <img :src="show.imageUrl" @click="previewImage(show.imageUrl)" />
          </div>
          <div class="show-info">
            <div class="show-desc">{{ show.description || '暂无描述' }}</div>
            <div class="show-meta">
              <span class="likes">
                <el-icon><Star /></el-icon>
                {{ show.likes || 0 }}
              </span>
              <span class="date">{{ formatDate(show.createdAt) }}</span>
            </div>
          </div>
          <div class="show-actions">
            <el-button size="small" type="danger" @click="handleDelete(show)">删除</el-button>
          </div>
        </div>
      </div>

      <div class="empty-state" v-else>
        <el-icon class="empty-icon"><Picture /></el-icon>
        <div class="empty-text">暂无玩家秀</div>
        <el-button type="primary" @click="showUploadDialog = true">发布玩家秀</el-button>
      </div>
    </div>

    <!-- 发布玩家秀弹窗 -->
    <el-dialog v-model="showUploadDialog" title="发布玩家秀" width="500px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="80px" ref="uploadFormRef">
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="image-uploader"
            action=""
            :http-request="uploadImage"
            :show-file-list="false"
            accept="image/*"
          >
            <img v-if="uploadForm.imageUrl" :src="uploadForm.imageUrl" class="image-preview" />
            <div v-else class="image-placeholder">
              <el-icon><Plus /></el-icon>
              <span>上传图片</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="分享你的饰品搭配心得（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">发布</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="800px">
      <img :src="previewUrl" style="width: 100%;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyPlayerShows, createPlayerShow, deletePlayerShow } from '@/api/playerShow'
import { Plus, Picture, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const shows = ref([])
const loading = ref(false)
const showUploadDialog = ref(false)
const submitLoading = ref(false)
const uploadFormRef = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')

const uploadForm = ref({
  imageUrl: '',
  description: ''
})

const fetchShows = async () => {
  loading.value = true
  try {
    const res = await getMyPlayerShows()
    shows.value = res || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const uploadImage = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const data = await request.post('/v1/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    uploadForm.value.imageUrl = data
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const handleSubmit = async () => {
  if (!uploadForm.value.imageUrl) {
    ElMessage.warning('请上传图片')
    return
  }
  
  submitLoading.value = true
  try {
    await createPlayerShow(uploadForm.value)
    ElMessage.success('发布成功')
    showUploadDialog.value = false
    uploadForm.value = { imageUrl: '', description: '' }
    fetchShows()
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (show) => {
  try {
    await ElMessageBox.confirm('确定要删除这条玩家秀吗？', '提示', { type: 'warning' })
    await deletePlayerShow(show.id)
    shows.value = shows.value.filter(s => s.id !== show.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const previewImage = (url) => {
  previewUrl.value = url
  previewVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchShows()
})
</script>

<style scoped>
.player-show-manage {
  background: #fff;
  min-height: 100%;
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
}

.manage-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.content-area {
  padding: 20px 25px;
  min-height: 400px;
}

.show-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.show-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  transition: all 0.2s;
}

.show-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.show-image {
  width: 100%;
  height: 200px;
  background: #f5f5f5;
  cursor: pointer;
}

.show-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.show-info {
  padding: 12px;
}

.show-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.show-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.show-meta .likes {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f56c6c;
}

.show-actions {
  display: flex;
  justify-content: flex-end;
  padding: 0 12px 12px;
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

/* 图片上传 */
.image-uploader {
  width: 100%;
}

.image-uploader :deep(.el-upload) {
  width: 100%;
  height: 200px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-uploader :deep(.el-upload:hover) {
  border-color: #4b89dc;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  gap: 8px;
}

.image-placeholder .el-icon {
  font-size: 32px;
}
</style>
