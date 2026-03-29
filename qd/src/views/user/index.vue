<template>
  <div class="user-layout-page">
    <!-- Top Navbar -->
    <nav class="navbar-dark">

      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>
      <div class="user-nav-links">
        <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
        <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
        <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        <div class="nav-user-section">
          <UserNavDropdown />
        </div>
      </div>
    </nav>

    <div class="user-container">
      <UserSidebar />
      <div class="user-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import UserSidebar from './components/UserSidebar.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'
import { Collection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.user-layout-page {
  min-height: 100vh;
  background: #171a21; /* 整体深色背景，但中间内容区可能是白色，视子页面而定 */
  background-image: url('https://g.fp.ps.netease.com/market/file/5e9d5a7a8b742745a5763521A1Uj7K9Q02?f=png'); /* 模拟背景图 */
  background-size: cover;
  background-position: top center;
  background-attachment: fixed;
  display: flex;
  flex-direction: column;
}

/* Navbar Styles */
.navbar-dark {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 60px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 40px; /* Spacing below navbar */
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
}

.nav-links-left .nav-link:hover,
.nav-links-left .router-link-active {
  color: #fff;
}

.user-nav-links {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 40px;
}

.user-nav-links .nav-link {
  color: #868a9f;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
}

.user-nav-links .nav-link:hover {
  color: #fff;
}

.nav-user-section {
  display: flex;
  align-items: center;
  margin-left: 10px;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  color: #fff;
  margin-left: 10px;
  font-size: 14px;
}

.user-container {
  width: 1200px;
  margin: 0 auto 40px; /* Center and add bottom margin */
  display: flex;
  min-height: 600px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  overflow: hidden;
}

.user-content {
  flex: 1;
  background: #fff; /* 右侧白色背景 */
  min-height: 600px;
}
</style>
