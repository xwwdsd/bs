<template>
  <div class="home-page">
    <SiteHeader active="home" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="main-content">
      <section v-if="banners.length" class="hero-banner">
        <el-carousel height="400px" trigger="click" arrow="hover">
          <el-carousel-item v-for="banner in banners" :key="banner.id">
            <div class="banner-slide" :style="{ backgroundImage: `url(${banner.imageUrl})` }">
              <div class="banner-overlay">
                <p class="eyebrow">CS2 Trading</p>
                <h1>{{ banner.title }}</h1>
                <p>{{ banner.description }}</p>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </section>

      <section v-else class="hero-banner">
        <div class="banner-slide fallback-slide">
          <div class="banner-overlay">
            <p class="eyebrow">CS2 Trading</p>
            <h1>更清晰地浏览，更稳定地交易</h1>
            <p>在首页浏览热门在售与最新上架，点击任意卡片即可进入对应订单的独立详情页。</p>
          </div>
        </div>
      </section>

      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-yellow">热门饰品</div>
          </div>
          <div class="section-more" @click="router.push('/items')">
            进入市场
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="items-grid">
          <div v-for="item in hotItems" :key="item.id" class="item-card" @click="goToDetail(item)">
            <div class="card-image">
              <div class="card-badge-row">
                <div class="wear-tag" :class="getCardBadgeClass(item)">{{ getCardBadgeText(item) }}</div>
                <span v-if="getCardSecondaryBadgeText(item)" class="type-tag" :class="getCardSecondaryBadgeClass(item)">
                  {{ getCardSecondaryBadgeText(item) }}
                </span>
              </div>
              <img :src="getItemIcon(item)" :alt="getItemName(item)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(item) }}</h4>
              <p class="card-subtitle">{{ getCardSubtitle(item) }}</p>
              <p class="card-price">¥ {{ formatPrice(item.price) }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">最新上架</div>
          </div>
          <div class="section-more" @click="router.push('/items')">
            进入市场
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="items-grid">
          <div v-for="item in newItems" :key="item.id" class="item-card" @click="goToDetail(item)">
            <div class="card-image">
              <div class="card-badge-row">
                <div class="wear-tag" :class="getCardBadgeClass(item)">{{ getCardBadgeText(item) }}</div>
                <span v-if="getCardSecondaryBadgeText(item)" class="type-tag" :class="getCardSecondaryBadgeClass(item)">
                  {{ getCardSecondaryBadgeText(item) }}
                </span>
              </div>
              <img :src="getItemIcon(item)" :alt="getItemName(item)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(item) }}</h4>
              <p class="card-subtitle">{{ getCardSubtitle(item) }}</p>
              <p class="card-price">¥ {{ formatPrice(item.price) }}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { getMarketList } from '@/api/sellOrder'
import { getActiveBanners } from '@/api/banner'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const router = useRouter()
const showLoginModal = ref(false)
const hotItems = ref([])
const newItems = ref([])
const banners = ref([])

const fetchBanners = async () => {
  try {
    const res = await getActiveBanners()
    banners.value = Array.isArray(res) ? res : []
  } catch (error) {
    ElMessage.error(error?.message || '获取首页 Banner 失败')
  }
}

const fetchItems = async () => {
  try {
    const [hotResult, newResult] = await Promise.all([
      getMarketList({ page: 1, size: 10, sortField: 'price', sortOrder: 'DESC' }),
      getMarketList({ page: 1, size: 10, sortField: 'created_at', sortOrder: 'DESC' })
    ])
    hotItems.value = hotResult?.list || []
    newItems.value = newResult?.list || []
  } catch (error) {
    ElMessage.error(error?.message || '获取首页市场数据失败')
  }
}

const getItemName = (item) => item.item?.nameCn || item.item?.name || item.inventory?.name || '未知饰品'
const getItemIcon = (item) => item.inventory?.iconUrl || item.item?.iconUrl || '/default-item.png'
const getCardDisplayModel = (item) => getItemDisplayModel(item)
const getCardBadgeText = (item) => getCardDisplayModel(item).primaryBadge.text
const getCardBadgeClass = (item) => {
  const badge = getCardDisplayModel(item).primaryBadge
  if (!badge) return ''
  if (badge.kind === 'quality') return `wear-tag-quality-${badge.code}`
  if (badge.kind === 'category') return `wear-tag-category-${badge.code || 'other'}`
  return `wear-tag-${badge.code || 'UN'}`
}
const getCardSecondaryBadgeText = (item) => getCardDisplayModel(item).secondaryBadge?.text || ''
const getCardSecondaryBadgeClass = (item) => {
  const badge = getCardDisplayModel(item).secondaryBadge
  return badge ? `type-tag-${badge.code}` : ''
}
const getCardSubtitle = (item) => getCardDisplayModel(item).subtitle
const formatPrice = (price) => Number(price || 0).toFixed(2)

const goToDetail = (item) => {
  router.push(`/items/order/${item.id}`)
}

onMounted(() => {
  fetchBanners()
  fetchItems()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #e3e3e3;
}

.main-content {
  padding-top: 96px;
}

.hero-banner {
  width: min(1240px, calc(100% - 40px));
  height: 400px;
  margin: 0 auto;
  border-radius: 18px;
  overflow: hidden;
}

.banner-slide {
  height: 100%;
  background-position: center;
  background-size: cover;
}

.fallback-slide {
  background:
    linear-gradient(135deg, rgba(18, 22, 31, 0.88), rgba(31, 48, 67, 0.84)),
    radial-gradient(circle at top right, rgba(240, 179, 33, 0.18), transparent 35%),
    radial-gradient(circle at bottom left, rgba(94, 134, 204, 0.16), transparent 32%),
    #171a21;
}

.banner-overlay {
  width: min(1120px, calc(100% - 60px));
  height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.eyebrow {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 12px;
}

.banner-overlay h1 {
  margin: 0 0 14px;
  font-size: 48px;
  line-height: 1.1;
}

.banner-overlay p {
  max-width: 640px;
  margin: 0;
  font-size: 16px;
  line-height: 1.7;
}

.market-section {
  width: min(1240px, calc(100% - 40px));
  margin: 24px auto 0;
  border: 1px solid #e6e8ec;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
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

.tab-item.active-yellow {
  background: #f0b321;
  color: #fff;
}

.tab-item.active-blue {
  background: #4e86dc;
  color: #fff;
}

.section-more {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 24px;
  color: #9299a7;
  cursor: pointer;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
  padding: 24px 26px 28px;
}

.item-card {
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  border-color: #d9dde5;
  box-shadow: 0 18px 30px rgba(17, 24, 39, 0.12);
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 18px;
  background: linear-gradient(180deg, #f8f8f8 0%, #efefef 100%);
}

.card-image img {
  max-width: 100%;
  max-height: 132px;
  object-fit: contain;
}

.card-badge-row {
  position: absolute;
  left: 8px;
  right: 8px;
  top: 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.wear-tag {
  padding: 5px 8px;
  border-radius: 999px;
  background: #5d8a42;
  color: #fff;
  font-size: 12px;
}

.wear-tag-FN {
  background: #329545;
}

.wear-tag-MW {
  background: #5d9545;
}

.wear-tag-FT {
  background: #efab3d;
}

.wear-tag-WW {
  background: #8c8f96;
}

.wear-tag-BS {
  background: #c85050;
}

.wear-tag-category-sticker,
.wear-tag-category-music {
  background: #2563eb;
}

.wear-tag-category-graffiti {
  background: #7c3aed;
}

.wear-tag-category-charm {
  background: #0f766e;
}

.wear-tag-category-agent {
  background: #16a34a;
}

.wear-tag-category-case {
  background: #475569;
}

.wear-tag-category-tool {
  background: #0f766e;
}

.wear-tag-category-pass {
  background: #7c2d12;
}

.wear-tag-category-collectible {
  background: #7c3aed;
}

.wear-tag-quality-contraband {
  background: #f5df4d;
  color: #1f2937;
}

.wear-tag-quality-covert {
  background: #d9485f;
}

.wear-tag-quality-classified {
  background: #c84cff;
}

.wear-tag-quality-restricted {
  background: #8b5cf6;
}

.wear-tag-quality-mil-spec {
  background: #3b82f6;
}

.wear-tag-quality-industrial {
  background: #60a5fa;
}

.wear-tag-quality-consumer {
  background: #94a3b8;
}

.wear-tag-quality-extraordinary {
  background: #f97316;
}

.wear-tag-quality-exotic {
  background: #a855f7;
}

.wear-tag-quality-remarkable {
  background: #ec4899;
}

.wear-tag-quality-high-grade {
  background: #4f8ef7;
}

.wear-tag-quality-normal-grade {
  background: #e5e7eb;
  color: #1f2937;
}

.wear-tag-quality-agent-grade {
  background: #22c55e;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 21, 34, 0.95);
  color: #ff8f1f;
  font-size: 12px;
  font-weight: 600;
}

.type-tag-Souvenir {
  color: #fbbf24;
}

.type-tag-Star {
  color: #f8fafc;
}

.type-tag-StarStatTrak {
  color: #ffb347;
}

.card-info {
  padding: 14px;
}

.card-name {
  display: -webkit-box;
  min-height: 50px;
  margin: 0 0 8px;
  overflow: hidden;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-subtitle {
  margin: 0 0 8px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.4;
}

.card-price {
  margin: 0;
  color: #f0b321;
  font-size: 18px;
  font-weight: 700;
}

@media (max-width: 1024px) {
  .items-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .banner-overlay h1 {
    font-size: 34px;
  }
}

@media (max-width: 560px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
