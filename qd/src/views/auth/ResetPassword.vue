<template>
  <div class="reset-password-page">
    <div class="reset-card">
      <div class="logo">
        <div class="logo-mark">CS2</div>
        <h1>重置密码</h1>
      </div>

      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>正在验证链接...</p>
      </div>

      <div v-else-if="!tokenValid" class="error-container">
        <el-icon :size="60" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <h2>链接无效或已过期</h2>
        <p>该密码重置链接已经失效，请重新申请。</p>
        <el-button type="primary" @click="goToLogin">返回登录</el-button>
      </div>

      <div v-else class="form-container">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="handleSubmit"
              style="width: 100%"
            >
              重置密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="back-link">
        <router-link to="/">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleCloseFilled, Lock } from '@element-plus/icons-vue'
import { validateResetToken, resetPassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(true)
const submitting = ref(false)
const tokenValid = ref(false)
const token = ref('')

const form = ref({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6-20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(async () => {
  const encodedToken = route.query.token
  if (!encodedToken) {
    loading.value = false
    return
  }

  try {
    token.value = atob(encodedToken)
    const valid = await validateResetToken(token.value)
    tokenValid.value = valid === true
  } catch (error) {
    tokenValid.value = false
  } finally {
    loading.value = false
  }
})

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await resetPassword(token.value, form.value.newPassword)
      ElMessage.success('密码重置成功，请重新登录')
      router.push('/?showLogin=true')
    } catch (error) {
      ElMessage.error(error?.message || '重置失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/?showLogin=true')
}
</script>

<style scoped>
.reset-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 20px;
}

.reset-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo-mark {
  width: 60px;
  height: 60px;
  margin: 0 auto 10px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffc857, #ff7b54);
  color: #16213e;
  font-size: 20px;
  font-weight: 800;
}

.logo h1 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.loading-container,
.error-container {
  text-align: center;
  padding: 40px 0;
}

.loading-container p,
.error-container p {
  color: #666;
  margin: 15px 0 20px;
}

.error-container h2 {
  color: #333;
  margin: 15px 0;
}

.form-container {
  margin-top: 20px;
}

.back-link {
  text-align: center;
  margin-top: 20px;
}

.back-link a {
  color: #409eff;
  text-decoration: none;
}

.back-link a:hover {
  text-decoration: underline;
}
</style>
