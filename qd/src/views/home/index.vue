<template>
  <div class="home-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link active">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>

      <div class="nav-user-section" v-if="userStore.isLoggedIn">
        <div class="user-nav-links">
          <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
          <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
          <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        </div>
        <UserNavDropdown />
      </div>

      <div class="nav-user" v-else>
        <span class="nav-link" @click="showLoginModal = true">登录/注册</span>
      </div>
    </nav>

    <LoginModal v-model="showLoginModal" />

    <main class="main-content">
      <section class="hero-banner" v-if="banners.length > 0">
        <el-carousel height="400px" trigger="click" arrow="hover">
          <el-carousel-item v-for="banner in banners" :key="banner.id">
            <div class="banner-slide" :style="{ backgroundImage: `url(${banner.imageUrl})` }">
              <div class="banner-overlay banner-overlay-rich">
                <h1>{{ banner.title }}</h1>
                <p>{{ banner.description }}</p>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </section>

      <section class="hero-banner fallback-banner" v-else>
        <div class="banner-overlay">
          <p class="eyebrow">CS2 Trading</p>
          <h1>更清晰地浏览，更稳定地交易</h1>
          <p>在市场中浏览饰品、查看库存、发布出售和求购，一站完成你的交易流程。</p>
        </div>
      </section>

      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-yellow">热门饰品</div>
            <div class="tab-item plain"><span class="game-icon cs2"></span> CS2</div>
            <div class="tab-item plain"><span class="game-icon dota2"></span> DOTA2</div>
          </div>
          <div class="section-more" @click="router.push('/items')">
            进入市场 <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="items-grid">
          <div v-for="item in hotItems" :key="item.id" class="item-card" @click="goToDetail(item)">
            <div class="card-image">
              <div class="wear-tag">{{ getItemExterior(item) || '未知外观' }}</div>
              <div class="item-icons">
                <span class="icon-3d">3D</span>
              </div>
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
            <div class="tab-item plain"><span class="game-icon cs2"></span> CS2</div>
            <div class="tab-item plain"><span class="game-icon dota2"></span> DOTA2</div>
          </div>
          <div class="section-more" @click="router.push('/items')">
            进入市场 <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="items-grid">
          <div v-for="item in newItems" :key="item.id" class="item-card" @click="goToDetail(item)">
            <div class="card-image">
              <div class="wear-tag">{{ getItemExterior(item) || '未知外观' }}</div>
              <div class="item-icons">
                <span class="icon-3d">3D</span>
              </div>
              <img :src="getItemIcon(item)" :alt="getItemName(item)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(item) }}</h4>
              <p class="card-price">¥ {{ formatPrice(item.price) }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="market-section" v-if="buyItems.length > 0">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-gold">最新求购</div>
            <div class="tab-item plain"><span class="game-icon cs2"></span> CS2</div>
            <div class="tab-item plain"><span class="game-icon dota2"></span> DOTA2</div>
          </div>
          <div class="section-more" @click="router.push('/user/buy-orders')">
            进入求购 <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="items-grid">
          <div v-for="item in buyItems" :key="item.id" class="item-card" @click="goToDetail(item)">
            <div class="card-image">
              <div class="wear-tag">求购</div>
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

    <div class="fixed-sidebar">
      <div class="sidebar-item">
        <el-icon><Cellphone /></el-icon>
        <span>App</span>
      </div>
      <div class="sidebar-item">
        <el-icon><Star /></el-icon>
        <span>收藏</span>
      </div>
      <div class="sidebar-item">
        <el-icon><ChatDotRound /></el-icon>
        <span>公众号</span>
      </div>
      <div class="sidebar-item">
        <el-icon><ChatLineRound /></el-icon>
        <span>微博</span>
      </div>
      <div class="sidebar-item">
        <el-icon><QuestionFilled /></el-icon>
        <span>帮助</span>
      </div>
      <div class="sidebar-item">
        <el-icon><Headset /></el-icon>
        <span>客服</span>
      </div>
      <div class="sidebar-item to-top" @click="scrollToTop">
        <el-icon><CaretTop /></el-icon>
        <span>TOP</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Cellphone,
  Star,
  ChatDotRound,
  ChatLineRound,
  QuestionFilled,
  Headset,
  CaretTop
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMarketList } from '@/api/sellOrder'
import { getActiveBanners } from '@/api/banner'
import LoginModal from '@/components/LoginModal.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'

const userStore = useUserStore()
const router = useRouter()
const showLoginModal = ref(false)

const hotItems = ref([])
const newItems = ref([])
const buyItems = ref([])
const banners = ref([])

const fetchBanners = async () => {
  try {
    const res = await getActiveBanners()
    banners.value = Array.isArray(res) ? res : []
  } catch (error) {
    ElMessage.error('获取 Banner 失败')
  }
}

const getItemName = (item) => {
  return item.item?.nameCn || item.item?.name || item.inventory?.name || '未知饰品'
}

const getItemIcon = (item) => {
  return item.inventory?.iconUrl || item.item?.iconUrl || '/default-item.png'
}

const getItemExterior = (item) => {
  return item.inventory?.exterior || item.item?.exterior || ''
}

const formatPrice = (price) => {
  return Number(price || 0).toFixed(2)
}

const fetchItems = async () => {
  try {
    const hotResult = await getMarketList({ page: 1, size: 5, sortField: 'price', sortOrder: 'DESC' })
    hotItems.value = hotResult?.list || []

    const newResult = await getMarketList({ page: 1, size: 5, sortField: 'created_at', sortOrder: 'DESC' })
    newItems.value = newResult?.list || []

    buyItems.value = []
  } catch (error) {
    ElMessage.error('获取首页饰品失败')
  }
}

const goToDetail = (item) => {
  const itemId = item.itemId || item.item?.id || item.id
  if (!itemId) {
    ElMessage.error('物品信息不完整')
    return
  }
  router.push(`/items/${itemId}`)
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
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

.navbar-dark {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 50px;
  height: 60px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  color: #c7d5e0;
}

.main-content {
  padding-top: 60px;
  background: #e3e3e3;
}

.nav-links-left,
.user-nav-links {
  display: flex;
  align-items: center;
  gap: 40px;
}

.nav-link {
  color: #c7d5e0;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
  cursor: pointer;
}

.nav-link:hover,
.nav-link.active {
  color: #fff;
}

.nav-user-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.hero-banner {
  position: relative;
  height: 400px;
  background: #d9d9d9;
}

.banner-slide {
  width: 100%;
  height: 400px;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.fallback-banner {
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.04)),
    linear-gradient(120deg, #d1d1d1 0%, #171717 42%, #e88708 72%, #f8ce76 100%);
}

.banner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 70px;
  color: #fff;
}

.banner-overlay-rich {
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.32), rgba(0, 0, 0, 0.06) 55%, rgba(0, 0, 0, 0));
}

.eyebrow {
  margin-bottom: 10px;
  font-size: 14px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.82);
}

.banner-overlay h1 {
  margin: 0 0 14px;
  font-size: 46px;
}

.banner-overlay p {
  max-width: 620px;
  margin: 0;
  font-size: 18px;
  line-height: 1.7;
}

.market-section {
  max-width: 1240px;
  margin: 36px auto 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #dcdcdc;
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
  height: 58px;
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
  background: #4a86df;
  color: #fff;
}

.tab-item.active-gold {
  background: #d69b1f;
  color: #fff;
}

.game-icon {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.game-icon.cs2 {
  background: #f0b321;
}

.game-icon.dota2 {
  background: #c43af0;
}

.section-more {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-right: 18px;
  color: #8f96a3;
  font-size: 14px;
  cursor: pointer;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.item-card {
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
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
  border-radius: 3px;
  background: #6f7782;
  color: #fff;
  font-size: 12px;
}

.item-icons {
  position: absolute;
  right: 8px;
  top: 8px;
}

.icon-3d {
  padding: 4px 8px;
  border-radius: 3px;
  background: #8e8e8e;
  color: #fff;
  font-size: 12px;
}

.card-info {
  padding: 14px;
}

.card-name {
  display: -webkit-box;
  min-height: 50px;
  margin: 0 0 10px;
  overflow: hidden;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-price {
  margin: 0;
  color: #f0a10a;
  font-size: 18px;
  font-weight: 700;
}

.fixed-sidebar {
  position: fixed;
  right: 16px;
  top: 50%;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transform: translateY(-50%);
}

.sidebar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 60px;
  min-height: 60px;
  border-radius: 8px;
  background: #2e3546;
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.sidebar-item span {
  margin-top: 6px;
}

.to-top {
  background: #262d3c;
}

@media (max-width: 1024px) {
  .navbar-dark {
    padding: 0 20px;
  }

  .banner-overlay {
    padding: 0 30px;
  }

  .market-section {
    margin-left: 16px;
    margin-right: 16px;
  }
}

@media (max-width: 768px) {
  .navbar-dark {
    overflow-x: auto;
  }

  .nav-links-left,
  .user-nav-links {
    gap: 18px;
  }

  .hero-banner {
    height: 300px;
  }

  .banner-slide {
    height: 300px;
  }

  .banner-overlay h1 {
    font-size: 34px;
  }

  .banner-overlay p {
    font-size: 15px;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .fixed-sidebar {
    display: none;
  }
}

@media (max-width: 560px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
