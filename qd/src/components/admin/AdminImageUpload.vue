<template>
  <div class="image-upload" :style="panelStyle">
    <el-upload
      class="upload-trigger"
      action=""
      :show-file-list="false"
      :http-request="uploadImage"
      accept="image/*"
      :disabled="uploading"
    >
      <div v-if="modelValue" class="preview-shell">
        <el-image :src="modelValue" fit="cover" class="preview-image" />
        <div class="preview-mask">
          <el-icon class="preview-icon"><RefreshRight /></el-icon>
          <span>{{ uploading ? '上传中...' : buttonText }}</span>
        </div>
      </div>
      <div v-else class="placeholder">
        <el-icon class="placeholder-icon">
          <Plus />
        </el-icon>
        <span>{{ uploading ? '上传中...' : buttonText }}</span>
      </div>
    </el-upload>

    <div class="upload-footer">
      <div class="upload-tip">{{ tip }}</div>
      <el-button v-if="modelValue" text type="danger" @click="clearImage">清除</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  width: {
    type: [String, Number],
    default: 220
  },
  height: {
    type: [String, Number],
    default: 132
  },
  tip: {
    type: String,
    default: '支持 JPG、PNG、WEBP'
  },
  buttonText: {
    type: String,
    default: '点击上传图片'
  }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const normalizeSize = (value) => (typeof value === 'number' ? `${value}px` : value)

const panelStyle = computed(() => ({
  '--upload-width': normalizeSize(props.width),
  '--upload-height': normalizeSize(props.height)
}))

const uploadImage = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  uploading.value = true
  try {
    const imageUrl = await request.post('/v1/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('update:modelValue', imageUrl)
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

const clearImage = () => {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  width: var(--upload-width);
  max-width: 100%;
}

.upload-trigger {
  width: 100%;
}

.upload-trigger :deep(.el-upload) {
  width: 100%;
}

.preview-shell,
.placeholder {
  width: 100%;
  height: var(--upload-height);
  border-radius: 8px;
  overflow: hidden;
}

.preview-shell {
  position: relative;
  border: 1px solid #dbe2ee;
  background: #f8fafc;
}

.preview-image {
  width: 100%;
  height: 100%;
  display: block;
}

.preview-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #ffffff;
  background: rgba(15, 23, 42, 0.42);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.preview-shell:hover .preview-mask {
  opacity: 1;
}

.preview-icon,
.placeholder-icon {
  font-size: 24px;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  color: #64748b;
  transition: border-color 0.2s ease, color 0.2s ease;
}

.upload-trigger :deep(.el-upload:hover) .placeholder {
  border-color: #2563eb;
  color: #2563eb;
}

.upload-footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.upload-tip {
  min-width: 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}
</style>
