<template>
  <div class="user-settings">
    <h2 class="page-title">账号设置</h2>
    
    <div class="settings-group">
      <div class="group-title">基本设置</div>
      
      <div class="setting-item">
        <div class="label">头像</div>
        <div class="content">
          <el-avatar :size="48" :src="userStore.userInfo?.avatar" />
        </div>
        <div class="action">
          <el-button type="primary" link @click="openEditDialog">修改头像</el-button>
        </div>
      </div>

      <div class="setting-item">
        <div class="label">昵称</div>
        <div class="content">{{ userStore.userInfo?.username }}</div>
        <div class="action">
          <el-button type="primary" link @click="openEditDialog">修改昵称</el-button>
        </div>
      </div>
    </div>

    <div class="settings-group">
      <div class="group-title">账号安全</div>

      <div class="setting-item">
        <div class="label">邮箱</div>
        <div class="content">
          <span v-if="userStore.userInfo?.email">{{ userStore.userInfo.email }}</span>
          <span v-else class="text-gray">未绑定</span>
        </div>
        <div class="action">
          <el-button type="primary" class="small-btn" @click="openEditDialog">
            {{ userStore.userInfo?.email ? '修改邮箱' : '补充邮箱' }}
          </el-button>
        </div>
      </div>

      <!-- 手机账号模块 -->
      <div class="setting-item">
        <div class="label">手机账号</div>
        <div class="content">
          <div class="security-status" v-if="userPhone">
            <div class="status-icon-wrapper success">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-text">已绑定</div>
              <div class="status-detail">{{ maskPhone(userPhone) }}</div>
            </div>
          </div>
          <div class="security-status" v-else>
            <div class="status-icon-wrapper warning">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-text warning">未绑定</div>
              <div class="status-detail">绑定手机号可提高账号安全性</div>
            </div>
          </div>
        </div>
        <div class="action">
          <el-button type="primary" class="small-btn" @click="showPhoneDialog = true">
            {{ userPhone ? '更换手机' : '绑定手机' }}
          </el-button>
        </div>
      </div>

      <!-- 密码设置模块 -->
      <div class="setting-item">
        <div class="label">登录密码</div>
        <div class="content">
          <div class="security-status">
            <div class="status-icon-wrapper success">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-text">已设置</div>
              <div class="status-detail">定期修改密码可保护账号安全</div>
            </div>
          </div>
        </div>
        <div class="action">
          <el-button type="primary" class="small-btn" @click="showPasswordDialog = true">修改密码</el-button>
        </div>
      </div>

      <!-- 实名认证模块 -->
      <div class="setting-item">
        <div class="label">实名认证</div>
        <div class="content">
          <div class="security-status" v-if="isRealNameVerified">
            <div class="status-icon-wrapper success">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-text">已认证</div>
              <div class="status-detail">{{ realNameInfo }}</div>
            </div>
          </div>
          <div class="security-status" v-else>
            <div class="status-icon-wrapper warning">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="status-info">
              <div class="status-text warning">未认证</div>
              <div class="status-detail">实名认证后可进行提现操作</div>
            </div>
          </div>
        </div>
        <div class="action">
          <el-button v-if="!isRealNameVerified" type="primary" class="small-btn" @click="showRealNameDialog = true">立即认证</el-button>
          <el-button v-else type="primary" class="small-btn" disabled>已认证</el-button>
        </div>
      </div>
    </div>

    <div class="settings-group">
      <div class="group-title">支付账号</div>

      <div class="setting-item">
        <div class="label">支付宝绑定</div>
        <div class="content">
          <div class="payment-account" v-if="userAlipay">
            <div class="payment-icon alipay-icon">支</div>
            <span>{{ userAlipay }}</span>
          </div>
          <span v-else class="text-gray">未绑定</span>
        </div>
        <div class="action">
          <el-button type="primary" class="small-btn" @click="showAlipayDialog = true">
            {{ userAlipay ? '修改' : '绑定' }}
          </el-button>
        </div>
      </div>

      <div class="setting-item">
        <div class="label">银行卡绑定</div>
        <div class="content">
          <div class="payment-account" v-if="userBankCard">
            <div class="payment-icon bank-icon">卡</div>
            <span>{{ userBankCard }}</span>
          </div>
          <span v-else class="text-gray">未绑定</span>
        </div>
        <div class="action">
          <el-button type="primary" class="small-btn" @click="showBankDialog = true">
            {{ userBankCard ? '修改' : '绑定' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="settings-group">
      <div class="group-title">第三方账号</div>

      <div class="setting-item">
        <div class="label">Steam账号</div>
        <div class="content">
          <span v-if="userStore.userInfo?.steamId">{{ userStore.userInfo.steamId }}</span>
          <span v-else class="text-gray">未绑定</span>
        </div>
        <div class="action">
          <el-button v-if="!userStore.userInfo?.steamId" type="primary" class="small-btn" @click="showSteamBindDialog = true">绑定</el-button>
          <el-button v-else type="danger" plain class="small-btn" @click="showUnbindConfirm">解绑</el-button>
        </div>
      </div>
    </div>

    <!-- 编辑资料 Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="选填，用于登录和找回密码" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload 
            class="avatar-uploader" 
            action="" 
            :http-request="uploadAvatar"
            :show-file-list="false" 
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 手机绑定 Dialog -->
    <el-dialog v-model="showPhoneDialog" title="绑定手机号" width="450px">
      <el-form :model="phoneForm" label-width="100px">
        <el-form-item label="手机号码">
          <el-input v-model="phoneForm.phone" placeholder="请输入手机号码">
            <template #prepend>+86</template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码">
          <div class="verify-code-row">
            <el-input v-model="phoneForm.code" placeholder="请输入验证码" />
            <el-button :disabled="countdown > 0" @click="sendVerifyCode">
              {{ countdown > 0 ? `${countdown}s后重试` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindPhone" :loading="bindLoading">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 Dialog -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="450px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="bindLoading">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 实名认证 Dialog -->
    <el-dialog v-model="showRealNameDialog" title="实名认证" width="450px">
      <div class="realname-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>实名认证信息提交后不可修改，请确保信息准确</span>
      </div>
      <el-form :model="realNameForm" label-width="100px" style="margin-top: 20px;">
        <el-form-item label="真实姓名">
          <el-input v-model="realNameForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="realNameForm.idCard" placeholder="请输入身份证号码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRealNameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRealNameVerify" :loading="bindLoading">提交认证</el-button>
      </template>
    </el-dialog>

    <!-- 支付宝绑定 Dialog -->
    <el-dialog v-model="showAlipayDialog" title="绑定支付宝" width="450px">
      <div class="bind-dialog-content">
        <div class="bind-icon alipay-icon-large">支</div>
        <el-form :model="alipayForm" label-width="100px" style="margin-top: 20px;">
          <el-form-item label="支付宝账号">
            <el-input v-model="alipayForm.account" placeholder="请输入支付宝账号" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="alipayForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
        </el-form>
        <div class="bind-tips">
          <p>• 请确保支付宝账号与真实姓名一致</p>
          <p>• 绑定后可用于提现到支付宝</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAlipayDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindAlipay" :loading="bindLoading">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- 银行卡绑定 Dialog -->
    <el-dialog v-model="showBankDialog" title="绑定银行卡" width="450px">
      <div class="bind-dialog-content">
        <div class="bind-icon bank-icon-large">卡</div>
        <el-form :model="bankForm" label-width="100px" style="margin-top: 20px;">
          <el-form-item label="持卡人姓名">
            <el-input v-model="bankForm.realName" placeholder="请输入持卡人姓名" />
          </el-form-item>
          <el-form-item label="银行卡号">
            <el-input v-model="bankForm.cardNumber" placeholder="请输入银行卡号" />
          </el-form-item>
          <el-form-item label="开户银行">
            <el-select v-model="bankForm.bankName" placeholder="请选择开户银行" style="width: 100%;">
              <el-option label="中国工商银行" value="工商银行" />
              <el-option label="中国农业银行" value="农业银行" />
              <el-option label="中国银行" value="中国银行" />
              <el-option label="中国建设银行" value="建设银行" />
              <el-option label="交通银行" value="交通银行" />
              <el-option label="招商银行" value="招商银行" />
              <el-option label="邮储银行" value="邮储银行" />
              <el-option label="浦发银行" value="浦发银行" />
              <el-option label="民生银行" value="民生银行" />
              <el-option label="兴业银行" value="兴业银行" />
            </el-select>
          </el-form-item>
          <el-form-item label="预留手机">
            <el-input v-model="bankForm.phone" placeholder="请输入银行预留手机号" />
          </el-form-item>
        </el-form>
        <div class="bind-tips">
          <p>• 请确保银行卡信息准确无误</p>
          <p>• 绑定后可用于提现到银行卡</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showBankDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindBank" :loading="bindLoading">确认绑定</el-button>
      </template>
    </el-dialog>

    <!-- Steam Bind Dialog -->
    <el-dialog v-model="showSteamBindDialog" title="绑定Steam账号" width="500px">
      <el-form :model="steamBindForm" label-width="100px">
        <el-form-item label="交易链接">
          <el-input v-model="steamBindForm.tradeUrl" placeholder="Steam交易链接" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="steamBindForm.apiKey" placeholder="Steam API Key" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSteamBindDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSteamBind">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { CircleCheckFilled, Plus, WarningFilled, Lock, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const userStore = useUserStore()

const showEditDialog = ref(false)
const showPasswordDialog = ref(false)
const showSteamBindDialog = ref(false)
const showAlipayDialog = ref(false)
const showBankDialog = ref(false)
const showPhoneDialog = ref(false)
const showRealNameDialog = ref(false)
const bindLoading = ref(false)
const countdown = ref(0)

// 用户信息
const userPhone = ref('')
const userAlipay = ref('')
const userBankCard = ref('')
const isRealNameVerified = ref(false)
const realNameInfo = ref('')

const editForm = ref({
  username: userStore.userInfo?.username || '',
  email: userStore.userInfo?.email || '',
  avatar: userStore.userInfo?.avatar || ''
})

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

watch(
  () => userStore.userInfo,
  (userInfo) => {
    editForm.value.username = userInfo?.username || ''
    editForm.value.email = userInfo?.email || ''
    editForm.value.avatar = userInfo?.avatar || ''
  },
  { immediate: true }
)

const phoneForm = ref({
  phone: '',
  code: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const realNameForm = ref({
  realName: '',
  idCard: ''
})

const steamBindForm = ref({
  tradeUrl: '',
  apiKey: ''
})

const alipayForm = ref({
  account: '',
  realName: ''
})

const bankForm = ref({
  realName: '',
  cardNumber: '',
  bankName: '',
  phone: ''
})

const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const openEditDialog = () => {
  editForm.value.username = userStore.userInfo?.username || ''
  editForm.value.email = userStore.userInfo?.email || ''
  editForm.value.avatar = userStore.userInfo?.avatar || ''
  showEditDialog.value = true
}

const fetchUserAccounts = async () => {
  try {
    const data = await request.get('/v1/user/payment-accounts')
    if (data) {
      userPhone.value = data.phone || ''
      userAlipay.value = data.alipay || ''
      userBankCard.value = data.bankCard || ''
      alipayForm.value.account = data.alipay || ''
      alipayForm.value.realName = data.alipayRealName || ''
      bankForm.value.realName = data.bankRealName || ''
      bankForm.value.cardNumber = data.bankCard || ''
      bankForm.value.bankName = data.bankName || ''
      bankForm.value.phone = data.bankPhone || ''
    }
  } catch (error) {
    console.error(error)
  }
}

const sendVerifyCode = () => {
  if (!phoneForm.value.phone || phoneForm.value.phone.length !== 11) {
    return ElMessage.warning('请输入正确的手机号码')
  }
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  ElMessage.success('验证码已发送')
}

const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPG) ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
  if (!isLt2M) ElMessage.error('上传头像图片大小不能超过 2MB!')
  return isJPG && isLt2M
}

const uploadAvatar = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const data = await request.post('/v1/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    editForm.value.avatar = data
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const handleSaveProfile = async () => {
  const email = editForm.value.email.trim()
  if (email && !emailPattern.test(email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  try {
    await request.post('/v1/user/update', {
      nickname: editForm.value.username,
      avatar: editForm.value.avatar,
      ...(email ? { email } : {})
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    await userStore.getUserInfo()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

const handleBindPhone = async () => {
  if (!phoneForm.value.phone || !phoneForm.value.code) {
    return ElMessage.warning('请填写完整信息')
  }
  bindLoading.value = true
  try {
    await request.post('/v1/user/bind-phone', phoneForm.value)
    ElMessage.success('绑定成功')
    showPhoneDialog.value = false
    userPhone.value = phoneForm.value.phone
  } catch (error) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) {
    return ElMessage.warning('请填写完整信息')
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  if (passwordForm.value.newPassword.length < 6 || passwordForm.value.newPassword.length > 20) {
    return ElMessage.warning('密码长度应为6-20位')
  }
  bindLoading.value = true
  try {
    await request.post('/v1/user/change-password', passwordForm.value)
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    bindLoading.value = false
  }
}

const handleRealNameVerify = async () => {
  if (!realNameForm.value.realName || !realNameForm.value.idCard) {
    return ElMessage.warning('请填写完整信息')
  }
  bindLoading.value = true
  try {
    await request.post('/v1/user/real-name-verify', realNameForm.value)
    ElMessage.success('认证成功')
    showRealNameDialog.value = false
    isRealNameVerified.value = true
    realNameInfo.value = realNameForm.value.realName + ' (' + realNameForm.value.idCard.slice(0, 4) + '****' + realNameForm.value.idCard.slice(-4) + ')'
  } catch (error) {
    ElMessage.error(error.message || '认证失败')
  } finally {
    bindLoading.value = false
  }
}

const handleSteamBind = async () => {
  if (!steamBindForm.value.tradeUrl || !steamBindForm.value.apiKey) {
    return ElMessage.warning('请填写完整')
  }
  try {
    await request.post('/v1/steam/bind', steamBindForm.value)
    ElMessage.success('绑定成功')
    showSteamBindDialog.value = false
    userStore.getUserInfo()
  } catch (error) {
    ElMessage.error('绑定失败')
  }
}

const handleBindAlipay = async () => {
  if (!alipayForm.value.account) {
    return ElMessage.warning('请输入支付宝账号')
  }
  bindLoading.value = true
  try {
    await request.post('/v1/user/bind-alipay', alipayForm.value)
    ElMessage.success('绑定成功')
    showAlipayDialog.value = false
    fetchUserAccounts()
  } catch (error) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const handleBindBank = async () => {
  if (!bankForm.value.cardNumber || !bankForm.value.realName || !bankForm.value.bankName) {
    return ElMessage.warning('请填写完整信息')
  }
  bindLoading.value = true
  try {
    await request.post('/v1/user/bind-bank', bankForm.value)
    ElMessage.success('绑定成功')
    showBankDialog.value = false
    fetchUserAccounts()
  } catch (error) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    bindLoading.value = false
  }
}

const showUnbindConfirm = () => {
  ElMessageBox.confirm('确定要解绑Steam账号吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.post('/v1/steam/unbind')
      ElMessage.success('解绑成功')
      userStore.getUserInfo()
    } catch (error) {
      ElMessage.error('解绑失败')
    }
  })
}

onMounted(() => {
  fetchUserAccounts()
})
</script>

<style scoped>
.user-settings {
  padding: 30px 40px;
  color: #333;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 30px;
  color: #333;
}

.settings-group {
  margin-bottom: 40px;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.label {
  width: 120px;
  color: #999;
  font-size: 14px;
}

.content {
  flex: 1;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.text-gray {
  color: #999;
}

.action {
  width: 100px;
  text-align: right;
}

.small-btn {
  padding: 8px 15px;
  font-size: 12px;
  background-color: #4b89dc;
  border-color: #4b89dc;
}

.small-btn:hover {
  background-color: #5da0ff;
  border-color: #5da0ff;
}

/* 安全状态样式 */
.security-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.status-icon-wrapper.success {
  background: #e1f3d8;
  color: #67c23a;
}

.status-icon-wrapper.warning {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.status-text.warning {
  color: #e6a23c;
}

.status-detail {
  font-size: 12px;
  color: #999;
}

/* 支付账号显示 */
.payment-account {
  display: flex;
  align-items: center;
  gap: 10px;
}

.payment-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 14px;
}

.alipay-icon {
  background: linear-gradient(135deg, #1677ff 0%, #40a9ff 100%);
}

.bank-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 绑定对话框样式 */
.bind-dialog-content {
  text-align: center;
}

.bind-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 28px;
}

.alipay-icon-large {
  background: linear-gradient(135deg, #1677ff 0%, #40a9ff 100%);
}

.bank-icon-large {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.bind-tips {
  text-align: left;
  margin-top: 15px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.bind-tips p {
  margin: 5px 0;
  font-size: 12px;
  color: #909399;
}

/* 验证码行 */
.verify-code-row {
  display: flex;
  gap: 10px;
}

.verify-code-row .el-input {
  flex: 1;
}

/* 实名认证提示 */
.realname-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 13px;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  object-fit: cover;
}
</style>
