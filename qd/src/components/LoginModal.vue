<template>
  <el-dialog
    v-model="visible"
    width="420px"
    :show-close="true"
    :close-on-click-modal="false"
    class="login-modal"
    destroy-on-close
  >
    <div class="login-content">
      <h2 class="login-title">{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
      <p class="login-subtitle">{{ isLogin ? '登录您的 CS2Trade 账号' : '注册新的 CS2Trade 账号' }}</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleSubmit"
      >
        <el-form-item v-if="isLogin" prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入用户名或邮箱"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item v-else prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="!isLogin" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            size="large"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item v-if="!isLogin" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <div v-if="isLogin" class="form-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <a href="#" class="forgot-link" @click.prevent="handleForgot">忘记密码？</a>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            {{ isLogin ? '登录' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <a href="#" class="toggle-link" @click.prevent="toggleMode">
          {{ isLogin ? '立即注册' : '立即登录' }}
        </a>
      </div>
    </div>

    <ForgotPasswordModal v-model="showForgotPassword" />
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import ForgotPasswordModal from './ForgotPasswordModal.vue'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'success'])

const userStore = useUserStore()
const visible = ref(props.modelValue)
const isLogin = ref(true)
const loading = ref(false)
const rememberMe = ref(false)
const formRef = ref(null)
const showForgotPassword = ref(false)

const form = reactive({
  account: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = {
  account: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    { min: 3, max: 50, message: '长度需在 3-50 个字符之间', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需在 3-20 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6-20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(() => visible.value, (val) => {
  emit('update:modelValue', val)
  if (!val) {
    resetForm()
  }
})

const resetForm = () => {
  form.account = ''
  form.username = ''
  form.email = ''
  form.password = ''
  form.confirmPassword = ''
  isLogin.value = true
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      if (isLogin.value) {
        const data = await login({
          account: form.account,
          password: form.password
        })
        userStore.setLoginInfo(data)
        ElMessage.success('登录成功')
        visible.value = false
        emit('success')
        return
      }

      await register({
        username: form.username,
        email: form.email,
        password: form.password
      })
      ElMessage.success('注册成功，请使用新账号登录')
      isLogin.value = true
      form.account = form.username
      form.password = ''
      form.confirmPassword = ''
    } catch (error) {
      console.error(isLogin.value ? '登录失败:' : '注册失败:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleForgot = () => {
  showForgotPassword.value = true
}
</script>

<style scoped>
.login-content {
  padding: 20px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
  text-align: center;
}

.login-subtitle {
  font-size: 14px;
  color: #666;
  margin-bottom: 30px;
  text-align: center;
}

.login-form :deep(.el-input__inner) {
  height: 44px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forgot-link {
  color: #4b89dc;
  font-size: 14px;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  background: #4b89dc;
  border: none;
}

.submit-btn:hover {
  background: #3a78cb;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.toggle-link {
  color: #4b89dc;
  font-weight: 500;
  text-decoration: none;
  margin-left: 5px;
}

.toggle-link:hover {
  text-decoration: underline;
}
</style>

<style>
.login-modal .el-dialog__header {
  display: none;
}

.login-modal .el-dialog__body {
  padding: 0;
}
</style>
