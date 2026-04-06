<template>
  <div class="market-page">
    <SiteHeader active="market" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="main-content">
      <section class="hero-banner market-banner">
        <div class="banner-overlay">
          <p class="eyebrow">DOTA2 Market</p>
          <h1>DOTA2 市场</h1>
          <p>这里是独立的 DOTA2 市场入口，当前先保留统一的页面结构和切换能力，后续再接入真实数据。</p>
        </div>
      </section>

      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <router-link to="/items" class="tab-item plain link-tab">
              <span class="game-icon cs2"></span>
              CS2
            </router-link>
            <div class="tab-item active-blue">
              <span class="game-icon dota2"></span>
              DOTA2
            </div>
          </div>
          <div class="section-summary">独立市场页面</div>
        </div>

        <div class="filter-bar disabled">
          <el-input model-value="" disabled placeholder="搜索 DOTA2 饰品名称" class="filter-control search-control" />
          <el-select model-value="" disabled placeholder="排序" class="filter-control">
            <el-option label="默认排序" value="" />
          </el-select>
          <el-button class="filter-action" disabled>建设中</el-button>
        </div>

        <div class="placeholder-panel">
          <h3>DOTA2 市场正在建设中</h3>
          <p>当前已经完成顶部导航、独立路由和页面风格统一，后续可以继续接入 DOTA2 专属的分类、筛选和市场数据。</p>
          <el-button type="primary" @click="$router.push('/items')">返回 CS2 市场</el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const showLoginModal = ref(false)
</script>

<style scoped>
.market-page {
  min-height: 100vh;
  background: #e3e3e3;
}

.main-content {
  padding-top: 72px;
}

.hero-banner {
  width: 100%;
  height: 320px;
}

.market-banner {
  background:
    linear-gradient(135deg, rgba(28, 31, 40, 0.72), rgba(53, 32, 71, 0.82)),
    radial-gradient(circle at top right, rgba(196, 58, 240, 0.26), transparent 35%),
    radial-gradient(circle at bottom left, rgba(240, 179, 33, 0.16), transparent 32%),
    #171a21;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-overlay {
  width: min(1240px, calc(100% - 60px));
  color: #fff;
}

.eyebrow {
  margin: 0 0 12px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.banner-overlay h1 {
  margin: 0 0 14px;
  font-size: 42px;
}

.banner-overlay p {
  margin: 0;
  max-width: 620px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
}

.market-section {
  width: min(1240px, calc(100% - 60px));
  margin: -44px auto 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ececec;
  border-bottom: 1px solid #e2e2e2;
}

.tab-group {
  display: flex;
  align-items: center;
  gap: 36px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 0 22px;
  color: #666f7d;
  font-size: 16px;
  font-weight: 600;
}

.tab-item.active-blue {
  background: #4e86dc;
  color: #fff;
}

.link-tab {
  text-decoration: none;
}

.game-icon {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 4px;
}

.game-icon.cs2 {
  background: #f0b321;
}

.game-icon.dota2 {
  background: #c43af0;
}

.section-summary {
  padding-right: 24px;
  color: #9299a7;
}

.filter-bar {
  display: flex;
  gap: 14px;
  padding: 22px 24px;
  border-bottom: 1px solid #eceef3;
}

.filter-control {
  min-width: 180px;
}

.search-control {
  width: 320px;
}

.disabled {
  opacity: 0.72;
}

.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
  padding: 34px 24px 40px;
}

.placeholder-panel h3 {
  margin: 0;
  color: #111827;
  font-size: 24px;
}

.placeholder-panel p {
  margin: 0;
  max-width: 720px;
  color: #6b7280;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .section-header,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
