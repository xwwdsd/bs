<template>
  <nav class="site-header">
    <div class="nav-links-left">
      <span class="nav-link" :class="{ active: active === 'home' }" @click="navigateTo('/')">首页</span>
      <span class="nav-link" :class="{ active: active === 'market' }" @click="navigateTo('/items')">市场</span>
      <span class="nav-link" :class="{ active: active === 'news' }" @click="navigateTo('/news')">资讯</span>
      <span class="nav-link" :class="{ active: active === 'player-shows' }" @click="navigateTo('/player-shows')">玩家秀</span>
    </div>

    <div v-if="userStore.isLoggedIn" class="nav-user-section">
      <TopUserLinks />
    </div>

    <div v-else class="nav-user">
      <span class="nav-link auth-link" @click="handleAuthClick">登录 / 注册</span>
    </div>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import TopUserLinks from '@/components/TopUserLinks.vue'

const props = defineProps({
  active: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['auth-click'])

const router = useRouter()
const userStore = useUserStore()

const handleAuthClick = () => {
  if (userStore.isLoggedIn) return
  emit('auth-click')
  if (!props.active) {
    router.push('/login')
  }
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  background: rgba(16, 19, 27, 0.96);
  border-bottom: 1px solid #2a2e3b;
  backdrop-filter: blur(14px);
}

.nav-links-left,
.nav-user-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-link {
  color: #d8e0ec;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.2s ease;
  cursor: pointer;
  display: inline-block;
}

.nav-link:hover,
.nav-link.active {
  color: #fff;
}

.nav-link:active {
  opacity: 0.8;
}

.auth-link {
  user-select: none;
}

@media (max-width: 1024px) {
  .site-header {
    padding: 0 16px;
  }

  .nav-links-left,
  .nav-user-section {
    gap: 18px;
  }
}

@media (max-width: 768px) {
  .site-header {
    overflow-x: auto;
  }

  .nav-links-left,
  .nav-user-section {
    gap: 16px;
  }
}
</style>
