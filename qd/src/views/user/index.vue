<template>
  <div class="user-layout-page">
    <SiteHeader />

    <div class="user-container">
      <UserSidebar />
      <div class="user-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import SiteHeader from '@/components/SiteHeader.vue'
import UserSidebar from './components/UserSidebar.vue'
</script>

<style scoped>
.user-layout-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.user-container {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 36px;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.user-content {
  flex: 1;
  min-height: 640px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
  overflow: hidden;
}

@media (max-width: 960px) {
  .user-container {
    flex-direction: column;
  }

  :deep(.user-sidebar) {
    width: 100%;
    min-height: auto;
  }
}
</style>
