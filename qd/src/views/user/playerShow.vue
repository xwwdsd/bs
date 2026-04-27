<template>
  <div class="player-show-manage">
    <div class="manage-header">
      <h2>玩家秀管理</h2>
      <el-button type="primary" @click="openCreateDialog">
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
            <el-button size="small" @click="openEditDialog(show)">修改</el-button>
            <el-button size="small" type="danger" @click="handleDelete(show)">删除</el-button>
          </div>
        </div>
      </div>

      <div class="empty-state" v-else>
        <el-icon class="empty-icon"><Picture /></el-icon>
        <div class="empty-text">暂无玩家秀</div>
        <el-button type="primary" @click="openCreateDialog">发布玩家秀</el-button>
      </div>
    </div>

    <el-dialog
      v-model="showEditorDialog"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetUploadForm"
    >
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
        <el-button @click="showEditorDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ submitButtonText }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="图片预览" width="800px">
      <img :src="previewUrl" style="width: 100%;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  createPlayerShow,
  deletePlayerShow,
  getMyPlayerShows,
  updatePlayerShow
} from '@/api/playerShow'
import { Plus, Picture, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const shows = ref([])
const loading = ref(false)
const showEditorDialog = ref(false)
const submitLoading = ref(false)
const uploadFormRef = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const dialogMode = ref('create')
const editingShowId = ref(null)

const uploadForm = ref({
  imageUrl: '',
  description: ''
})

const dialogTitle = computed(() => (dialogMode.value === 'edit' ? '修改玩家秀' : '发布玩家秀'))
const submitButtonText = computed(() => (dialogMode.value === 'edit' ? '保存修改' : '发布'))

const resetUploadForm = () => {
  uploadForm.value = {
    imageUrl: '',
    description: ''
  }
  dialogMode.value = 'create'
  editingShowId.value = null
}

const openCreateDialog = () => {
  resetUploadForm()
  showEditorDialog.value = true
}

const openEditDialog = (show) => {
  dialogMode.value = 'edit'
  editingShowId.value = show.id
  uploadForm.value = {
    imageUrl: show.imageUrl || '',
    description: show.description || ''
  }
  showEditorDialog.value = true
}

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
    if (dialogMode.value === 'edit' && editingShowId.value) {
      await updatePlayerShow(editingShowId.value, uploadForm.value)
      ElMessage.success('修改成功')
    } else {
      await createPlayerShow(uploadForm.value)
      ElMessage.success('发布成功')
    }
    showEditorDialog.value = false
    resetUploadForm()
    fetchShows()
  } catch (error) {
    ElMessage.error(dialogMode.value === 'edit' ? '修改失败' : '发布失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (show) => {
  try {
    await ElMessageBox.confirm('确定要删除这条玩家秀吗？', '提示', { type: 'warning' })
    await deletePlayerShow(show.id)
    shows.value = shows.value.filter((item) => item.id !== show.id)
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
  margin: 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
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
  overflow: hidden;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
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
  display: -webkit-box;
  overflow: hidden;
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.show-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 12px;
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
  gap: 8px;
  padding: 0 12px 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  margin-bottom: 12px;
  color: #ddd;
  font-size: 48px;
}

.empty-text {
  margin-bottom: 20px;
  color: #999;
  font-size: 14px;
}

.image-uploader {
  width: 100%;
}

.image-uploader :deep(.el-upload) {
  display: flex;
  width: 100%;
  height: 200px;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
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
  gap: 8px;
  color: #8c939d;
}

.image-placeholder .el-icon {
  font-size: 32px;
}
</style>
