<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">
        <div class="brand-title">后台管理</div>
        <div class="brand-subtitle">业务数据控制台</div>
      </div>
      <el-menu :default-active="activePath" router class="admin-menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <div class="page-title">{{ currentTitle }}</div>
          <div class="page-subtitle">查看、筛选和处理前台产生的主要业务数据</div>
        </div>
        <el-button :icon="Refresh" @click="router.go(0)">刷新</el-button>
      </header>
      <section class="admin-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Box,
  ChatDotRound,
  Collection,
  Document,
  Goods,
  Grid,
  HomeFilled,
  Money,
  Refresh,
  Sell,
  ShoppingCart,
  Switch,
  Tickets,
  User,
  Wallet
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/admin/dashboard', label: '控制台', icon: HomeFilled },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/items', label: '饰品管理', icon: Goods },
  { path: '/admin/inventory', label: '库存管理', icon: Box },
  { path: '/admin/buy-orders', label: '求购单', icon: ShoppingCart },
  { path: '/admin/sell-orders', label: '出售单', icon: Sell },
  { path: '/admin/trade-orders', label: '交易订单', icon: Tickets },
  { path: '/admin/wallets', label: '钱包流水', icon: Wallet },
  { path: '/admin/withdrawals', label: '提现审核', icon: Money },
  { path: '/admin/favorites', label: '收藏管理', icon: Collection },
  { path: '/admin/messages', label: '消息管理', icon: ChatDotRound },
  { path: '/admin/content', label: '内容运营', icon: Document },
  { path: '/admin/player-shows', label: '玩家秀', icon: Grid },
  { path: '/admin/sync', label: '同步维护', icon: Switch }
]

const activePath = computed(() => route.path)
const currentTitle = computed(() => menus.find((item) => item.path === route.path)?.label || '后台管理')
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  background: #f5f7fb;
  color: #1f2937;
}

.admin-sidebar {
  width: 232px;
  background: #111827;
  color: #fff;
  flex-shrink: 0;
}

.brand {
  padding: 22px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 6px;
  color: #9ca3af;
  font-size: 12px;
}

.admin-menu {
  border-right: 0;
  background: transparent;
}

.admin-menu :deep(.el-menu-item) {
  color: #d1d5db;
  height: 44px;
}

.admin-menu :deep(.el-menu-item.is-active),
.admin-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: #2563eb;
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
}

.page-subtitle {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.admin-content {
  padding: 20px;
}

@media (max-width: 900px) {
  .admin-shell {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
  }

  .admin-menu {
    display: flex;
    overflow-x: auto;
  }

  .admin-menu :deep(.el-menu-item) {
    flex: 0 0 auto;
  }
}
</style>
