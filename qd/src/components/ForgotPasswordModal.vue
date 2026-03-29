<template>
  <el-dialog
    v-model="visible"
    title="忘记密码"
    width="400px"
    :close-on-click-modal="false"
    class="forgot-password-dialog"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="注册邮箱" prop="email">
        <el-input
          v-model="form.email"
          placeholder="请输入注册时使用的邮箱"
          :prefix-icon="Message"
          size="large"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          发送重置邮件
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Message } from '@element-plus/icons-vue'
import { forgotPassword } from '@/api/auth'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref(null)
const loading = ref(false)
const form = ref({
  email: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await forgotPassword(form.value.email)
      ElMessage.success(result || '重置邮件已发送，请查收')
      handleClose()
    } catch (error) {
      ElMessage.error(error?.message || '发送失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const handleClose = () => {
  form.value.email = ''
  formRef.value?.resetFields()
  visible.value = false
}
</script>

<style scoped>
.forgot-password-dialog :deep(.el-dialog__header) {
  text-align: center;
}

.forgot-password-dialog :deep(.el-dialog__body) {
  padding: 20px 30px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
