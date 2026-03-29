<template>
  <el-popover
    placement="bottom-end"
    :width="300"
    trigger="hover"
    popper-class="user-dropdown-popover"
    :show-arrow="false"
    :offset="10"
  >
    <template #reference>
      <div class="user-profile-trigger" @click="$router.push('/user/profile')">
        <el-avatar :size="36" :src="userStore.userInfo?.avatar" />
      </div>
    </template>

    <div class="user-dropdown-content">
      <!-- User Header -->
      <div class="dropdown-header">
        <el-avatar :size="50" :src="userStore.userInfo?.avatar" />
        <div class="user-info">
          <div class="name-row">
            <span class="username">{{ userStore.userInfo?.username }}</span>
          </div>
          <div class="logout-link" @click="handleLogout">退出登录</div>
        </div>
      </div>

      <!-- Wallet Section -->
      <div class="wallet-section">
        <div class="wallet-info">
          <span class="label">资金</span>
          <span class="amount">¥ {{ wallet.balance?.toFixed(2) || '0.00' }}</span>
        </div>
        <el-button type="primary" size="small" class="withdraw-btn" @click="$router.push('/user/wallet')">提现</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const userStore = useUserStore()
const router = useRouter()
const wallet = ref({ balance: 0 })

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const fetchWallet = async () => {
  try {
    const res = await request.get('/v1/wallet/my')
    if (res) {
      wallet.value = res
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchWallet()
  }
})
</script>

<style scoped>
.user-profile-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  padding: 0;
  height: 60px;
  line-height: 60px;
}

.level-badge {
  position: absolute;
  bottom: 0;
  right: -5px;
  background: #4b89dc;
  color: white;
  font-size: 10px;
  padding: 0 3px;
  border-radius: 2px;
  line-height: 1.2;
}

.user-dropdown-content {
  color: #fff;
}

.dropdown-header {
  display: flex;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 15px;
}

.user-info {
  margin-left: 15px;
}

.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.username {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  margin-right: 8px;
}

.level-tag {
  background: #4b89dc;
  color: white;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 2px;
}

.logout-link {
  font-size: 12px;
  color: #4b89dc;
  cursor: pointer;
}

.logout-link:hover {
  text-decoration: underline;
}

.wallet-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  padding: 10px 15px;
  border-radius: 4px;
}

.wallet-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.label {
  color: #868a9f;
  font-size: 14px;
}

.amount {
  color: #eeb425;
  font-size: 18px;
  font-weight: bold;
}

.withdraw-btn {
  background: #3a3f50;
  border-color: #3a3f50;
  color: #fff;
}

.withdraw-btn:hover {
  background: #4b4f60;
  border-color: #4b4f60;
}
</style>

<style>
.user-dropdown-popover.el-popover {
  background: #232631 !important;
  border: 1px solid #3a3f50 !important;
  padding: 20px !important;
  color: #fff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5) !important;
}
</style>
