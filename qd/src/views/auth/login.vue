<template>
  <div class="auth-page">
    <SiteHeader @auth-click="showModal = true" />
    <LoginModal v-model="showModal" @success="handleSuccess" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const router = useRouter()
const userStore = useUserStore()
const showModal = ref(true)

onMounted(() => {
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
</style>
