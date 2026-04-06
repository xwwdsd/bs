<template>
  <div class="top-user-links">
    <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
    <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
    <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
    <router-link to="/messages" class="nav-link">消息中心</router-link>

    <el-dropdown trigger="click" placement="bottom-end" popper-class="market-switch-dropdown" @command="handleSwitchMarket">
      <span class="nav-link switch-link">
        切换
        <el-icon class="switch-icon"><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <div class="market-dropdown-content">
          <div class="search-box">
            <el-input
              v-model="searchQuery"
              placeholder="搜索游戏市场"
              prefix-icon="Search"
              clearable
              @input="filterMarkets"
            />
          </div>
          <el-dropdown-menu>
            <el-dropdown-item 
              v-for="market in filteredMarkets" 
              :key="market.path" 
              :command="market.path" 
              :disabled="isCurrentRoute(market.path)"
            >
              {{ market.name }}
            </el-dropdown-item>
          </el-dropdown-menu>
          <div v-if="filteredMarkets.length === 0" class="no-results">
            暂无匹配的游戏市场
          </div>
        </div>
      </template>
    </el-dropdown>

    <div class="nav-user-section">
      <UserNavDropdown />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'

const route = useRoute()
const router = useRouter()

const searchQuery = ref('')

const markets = [
  { path: '/items', name: 'CS2市场' },
  { path: '/items/dota2', name: 'DOTA2 市场' }
]

const filteredMarkets = ref(markets)

const filterMarkets = () => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) {
    filteredMarkets.value = markets
  } else {
    filteredMarkets.value = markets.filter(market => 
      market.name.toLowerCase().includes(query)
    )
  }
}

const isCurrentRoute = (path) => route.path === path

const handleSwitchMarket = (path) => {
  if (path && path !== route.path) {
    router.push(path)
  }
}
</script>

<style scoped>
.top-user-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.top-user-links :deep(.nav-link) {
  color: #d8e0ec !important;
  text-decoration: none !important;
  font-size: 15px;
  transition: color 0.2s ease;
}

.top-user-links :deep(.nav-link:visited) {
  color: #d8e0ec !important;
  text-decoration: none !important;
}

.top-user-links :deep(.nav-link:hover),
.top-user-links :deep(.router-link-active),
.top-user-links :deep(.router-link-exact-active) {
  color: #fff !important;
  text-decoration: none !important;
}

.top-user-links :deep(.market-switch-dropdown) {
  margin-top: 14px !important;
  padding: 0 !important;
  border: 1px solid #2f3545 !important;
  border-radius: 12px !important;
  background: #171b24 !important;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.35) !important;
  z-index: 10000 !important;
}

.top-user-links :deep(.market-switch-dropdown .el-popper__arrow) {
  display: none !important;
}

/* 下拉菜单容器样式 */
.market-dropdown-content {
  min-width: 220px;
  max-height: 400px;
  overflow-y: auto;
  border-radius: 12px;
  background: #171b24 !important;
}

/* 强制覆盖 Element Plus 的白色背景 */
.top-user-links :deep(.market-switch-dropdown),
.top-user-links :deep(.market-switch-dropdown .el-dropdown-menu),
.top-user-links :deep(.el-popper),
.top-user-links :deep(.el-popper__content) {
  background: #171b24 !important;
  border: none !important;
}

.market-dropdown-content::-webkit-scrollbar {
  width: 6px;
}

.market-dropdown-content::-webkit-scrollbar-track {
  background: transparent;
}

.market-dropdown-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.market-dropdown-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.search-box {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 12px;
  background: #171b24;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.search-box :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
  border-radius: 8px;
  padding: 8px 12px;
  transition: background 0.2s ease;
}

.search-box :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.12) !important;
}

.search-box :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3) !important;
}

.search-box :deep(.el-input__inner) {
  color: #f3f4f6 !important;
  font-size: 14px;
}

.search-box :deep(.el-input__inner::placeholder) {
  color: #8c93a3;
}

.search-box :deep(.el-input__prefix-inner) {
  color: #8c93a3;
}

.search-box :deep(.el-input__suffix-inner) {
  color: #8c93a3;
}

.top-user-links :deep(.market-switch-dropdown .el-dropdown-menu) {
  border: 0 !important;
  background: #171b24 !important;
  padding: 4px !important;
}

.top-user-links :deep(.market-switch-dropdown .el-dropdown-menu__item) {
  min-width: 156px;
  border-radius: 8px;
  color: #f3f4f6 !important;
  background: #171b24 !important;
  padding: 10px 16px;
  transition: all 0.2s ease;
}

.top-user-links :deep(.market-switch-dropdown .el-dropdown-menu__item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

.top-user-links :deep(.market-switch-dropdown .el-dropdown-menu__item.is-disabled) {
  color: #8c93a3 !important;
  background: #171b24 !important;
  opacity: 0.5;
}

/* 确保无结果提示也是深色背景 */
.no-results {
  padding: 12px 16px;
  color: #8c93a3;
  font-size: 14px;
  text-align: center;
  background: #171b24 !important;
}

.switch-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #fff;
  text-decoration: none;
}

.switch-icon {
  font-size: 12px;
}

.nav-user-section {
  display: flex;
  align-items: center;
}

@media (max-width: 1200px) {
  .top-user-links {
    gap: 18px;
  }
}

@media (max-width: 1024px) {
  .top-user-links {
    gap: 16px;
  }
}

@media (max-width: 960px) {
  .top-user-links {
    flex-wrap: wrap;
    justify-content: flex-end;
    row-gap: 10px;
  }
}
</style>
