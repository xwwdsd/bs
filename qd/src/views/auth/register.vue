<template>
  <div class="auth-page">
    <!-- 顶部导航 -->
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>
      <div class="nav-user" v-if="!userStore.isLoggedIn">
        <span class="nav-link active">登录/注册</span>
      </div>
    </nav>

    <!-- 登录弹窗（注册模式） -->
    <LoginModal v-model="showModal" @success="handleSuccess" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Collection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'

const router = useRouter()
const userStore = useUserStore()
const showModal = ref(true)

onMounted(() => {
  // 如果已登录，跳转到首页
  if (userStore.isLoggedIn) {
    router.push('/')
  }
})

const handleSuccess = () => {
  router.push('/')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #171a21;
}

.navbar-dark {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 50px;
  height: 64px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
}

.nav-links-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.nav-links-left .nav-link {
  color: #c7d5e0;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
  cursor: pointer;
}

.nav-links-left .nav-link:hover,
.nav-links-left .nav-link.active {
  color: #fff;
}
</style>
