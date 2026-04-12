<template>
  <div class="inventory-workspace-page">
    <SiteHeader />

    <main class="workspace-main">
      <section class="workspace-shell">
        <div class="workspace-tabs" role="tablist" aria-label="库存工作台导航">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="workspace-tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span class="workspace-tab-label">{{ tab.label }}</span>
          </button>
        </div>

        <div class="workspace-content">
          <InventorySteamTab v-show="activeTab === 'steam'" :active="activeTab === 'steam'" />
          <InventoryPurchaseRecordsTab
            v-show="activeTab === 'purchase'"
            :active="activeTab === 'purchase'"
          />
          <InventoryBargainTab v-show="activeTab === 'bargain'" :active="activeTab === 'bargain'" />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SiteHeader from '@/components/SiteHeader.vue'
import InventorySteamTab from '@/views/user/components/InventorySteamTab.vue'
import InventoryPurchaseRecordsTab from '@/views/user/components/InventoryPurchaseRecordsTab.vue'
import InventoryBargainTab from '@/views/user/components/InventoryBargainTab.vue'

const tabs = [
  { key: 'steam', label: 'Steam库存' },
  { key: 'purchase', label: '购买记录' },
  { key: 'bargain', label: '我的还价' }
]

const activeTab = ref('steam')
</script>

<style scoped>
.inventory-workspace-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.12), transparent 34%),
    linear-gradient(180deg, #f8fbff 0%, #f3f4f6 22%, #eef2f7 100%);
}

.workspace-main {
  padding: 92px 0 56px;
}

.workspace-shell {
  width: min(1320px, calc(100% - 40px));
  margin: 0 auto;
}

.workspace-tabs {
  display: flex;
  align-items: flex-end;
  gap: 28px;
  min-height: 76px;
  padding: 0 28px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.05);
  overflow-x: auto;
}

.workspace-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 76px;
  padding: 0 2px;
  flex: none;
  border: none;
  background: transparent;
  color: #64748b !important;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s ease;
  opacity: 1;
  z-index: 1;
}

.workspace-tab::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.2s ease;
}

.workspace-tab:hover {
  color: #1e293b;
}

.workspace-tab.active {
  color: #0f172a !important;
}

.workspace-tab.active::after {
  background: #2563eb;
}

.workspace-tab-label {
  display: inline-block;
  color: inherit;
}

.workspace-content {
  padding-top: 16px;
}

@media (max-width: 900px) {
  .workspace-main {
    padding: 84px 0 40px;
  }

  .workspace-shell {
    width: calc(100% - 24px);
  }

  .workspace-tabs {
    flex-wrap: wrap;
    gap: 0;
    min-height: auto;
    padding: 0 14px;
  }

  .workspace-tab {
    min-height: 58px;
    padding: 0 10px;
    font-size: 18px;
  }
}
</style>
