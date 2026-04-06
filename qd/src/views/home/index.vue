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
              <div class="wear-tag">{{ getItemExterior(item) || '未知外观' }}</div>
              <img :src="getItemIcon(item)" :alt="getItemName(item)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(item) }}</h4>
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
              <div class="wear-tag">{{ getItemExterior(item) || '未知外观' }}</div>
              <img :src="getItemIcon(item)" :alt="getItemName(item)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(item) }}</h4>
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
import {
  getExteriorText,
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

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
const getItemExterior = (item) =>
  getExteriorText(
    resolveExteriorCode(
      normalizeWearValue(item?.inventory?.paintWear ?? item?.paintWear),
      item?.inventory?.exterior,
      item?.inventory?.wearName,
      item?.item?.exterior,
      item?.inventory?.name,
      item?.item?.nameCn,
      item?.item?.name
    )
  )
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

.wear-tag {
  position: absolute;
  left: 8px;
  top: 8px;
  padding: 5px 8px;
  border-radius: 999px;
  background: #5d8a42;
  color: #fff;
  font-size: 12px;
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
