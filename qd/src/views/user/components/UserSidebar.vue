<template>
  <div class="user-sidebar">
    <!-- User Info Card -->
    <div class="user-info-card">
      <div class="avatar-wrapper">
        <el-avatar :size="64" :src="userStore.userInfo?.avatar" />
      </div>
      <div class="user-name">
        {{ userStore.userInfo?.username || '用户' }}
      </div>
    </div>

    <!-- Navigation Menu -->
    <div class="sidebar-menu">
      <router-link to="/user/wallet" class="menu-item" active-class="active">
        <el-icon><Wallet /></el-icon>
        <span>我的资金</span>
      </router-link>
      <router-link to="/user/profile" class="menu-item" active-class="active">
        <el-icon><Setting /></el-icon>
        <span>账号设置</span>
      </router-link>
      <router-link to="/user/messages" class="menu-item" active-class="active">
        <el-icon><Message /></el-icon>
        <span>消息中心</span>
      </router-link>
      <router-link to="/user/favorites" class="menu-item" active-class="active">
        <el-icon><Star /></el-icon>
        <span>我的收藏</span>
      </router-link>
      <router-link to="/user/submissions" class="menu-item" active-class="active">
        <el-icon><Document /></el-icon>
        <span>我的投稿</span>
      </router-link>
      <router-link to="/user/player-shows" class="menu-item" active-class="active">
        <el-icon><Picture /></el-icon>
        <span>玩家秀管理</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { Wallet, Setting, Message, Star, Document, Picture } from '@element-plus/icons-vue'

const userStore = useUserStore()
</script>

<style scoped>
.user-sidebar {
  width: 240px;
  background: linear-gradient(180deg, #181b24 0%, #13151a 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  min-height: 600px;
}

.user-info-card {
  padding: 30px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: radial-gradient(circle at 50% 0%, #2a3040 0%, #181b24 70%);
  border-bottom: 1px solid #252834;
}

.avatar-wrapper {
  margin-bottom: 15px;
  border: 2px solid #2a3040;
  border-radius: 50%;
  padding: 2px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.level-tag {
  font-size: 10px;
  background: #4b89dc;
  padding: 1px 4px;
  border-radius: 2px;
}

.sidebar-menu {
  padding: 10px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 30px;
  color: #868a9f;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;
  cursor: pointer;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.03);
}

.menu-item.active {
  color: #fff;
  background: linear-gradient(90deg, rgba(75, 137, 220, 0.1) 0%, rgba(75, 137, 220, 0) 100%);
  border-left-color: #4b89dc;
  text-shadow: 0 0 10px rgba(75, 137, 220, 0.5);
}

.menu-item .el-icon {
  font-size: 18px;
}
</style>
