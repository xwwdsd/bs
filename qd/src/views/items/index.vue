<template>
  <div class="market-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link active">市场</router-link>
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
      <section class="hero-banner market-banner">
        <div class="banner-overlay">
          <p class="eyebrow">CS2 Market</p>
          <h1>饰品市场</h1>
          <p>在与首页一致的视觉风格下，快速筛选分类、外观和价格，浏览当前在售饰品。</p>
        </div>
      </section>

      <section class="market-section market-board">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-yellow">市场列表</div>
            <div class="tab-item plain"><span class="game-icon cs2"></span> CS2</div>
            <div class="tab-item plain"><span class="game-icon dota2"></span> DOTA2</div>
          </div>
          <div class="section-summary">共 {{ total }} 条在售记录</div>
        </div>

        <div class="filter-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索饰品名称"
            clearable
            class="filter-control search-control"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select v-model="filters.category" clearable placeholder="分类" class="filter-control" @change="handleSearch">
            <el-option label="全部分类" value="" />
            <el-option label="步枪" value="rifle" />
            <el-option label="手枪" value="pistol" />
            <el-option label="冲锋枪" value="smg" />
            <el-option label="霰弹枪" value="shotgun" />
            <el-option label="狙击步枪" value="sniper_rifle" />
            <el-option label="机枪" value="machinegun" />
            <el-option label="刀" value="knife" />
            <el-option label="手套" value="glove" />
            <el-option label="贴纸" value="sticker" />
            <el-option label="武器箱" value="case" />
            <el-option label="其他" value="other" />
          </el-select>

          <el-select v-model="filters.exterior" clearable placeholder="外观" class="filter-control" @change="handleSearch">
            <el-option label="全部外观" value="" />
            <el-option label="崭新出厂" value="FN" />
            <el-option label="略有磨损" value="MW" />
            <el-option label="久经沙场" value="FT" />
            <el-option label="破损不堪" value="WW" />
            <el-option label="战痕累累" value="BS" />
            <el-option label="无涂装" value="NoPaint" />
          </el-select>

          <el-select v-model="sortValue" placeholder="排序" class="filter-control" @change="handleSearch">
            <el-option label="默认排序" value="default" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="最新发布" value="newest" />
          </el-select>

          <el-input
            v-model="minPrice"
            placeholder="最低价"
            class="filter-control price-control"
            @keyup.enter="handleSearch"
          />

          <el-input
            v-model="maxPrice"
            placeholder="最高价"
            class="filter-control price-control"
            @keyup.enter="handleSearch"
          />

          <el-button type="warning" class="filter-action primary-action" @click="handleSearch">筛选</el-button>
          <el-button class="filter-action" @click="resetFilters">重置</el-button>
        </div>

        <div class="items-grid" v-loading="loading">
          <el-empty v-if="!items.length" description="暂无符合条件的在售饰品" />

          <div v-for="entry in items" v-else :key="entry.id" class="item-card" @click="goToDetail(entry)">
            <div class="card-image">
              <div class="wear-tag">{{ getExteriorText(getItemExterior(entry)) }}</div>
              <div class="item-icons">
                <span class="icon-3d">在售</span>
              </div>
              <img :src="getItemIcon(entry)" :alt="getItemName(entry)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(entry) }}</h4>
              <p class="card-meta">{{ getCategoryText(entry.item?.category || entry.inventory?.category) }}</p>
              <div class="card-footer">
                <p class="card-price">¥ {{ formatPrice(entry.price) }}</p>
                <span class="seller-text">{{ entry.seller?.username || '查看详情' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getMarketList } from '@/api/sellOrder'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'

const router = useRouter()
const userStore = useUserStore()
const showLoginModal = ref(false)

const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const searchKeyword = ref('')
const minPrice = ref('')
const maxPrice = ref('')
const sortValue = ref('default')

const filters = reactive({
  category: '',
  exterior: ''
})

const buildParams = () => {
  const params = {
    page: page.value,
    size: pageSize,
    keyword: searchKeyword.value?.trim() || undefined,
    category: filters.category || undefined,
    exterior: filters.exterior || undefined,
    minPrice: minPrice.value || undefined,
    maxPrice: maxPrice.value || undefined
  }

  if (sortValue.value === 'price_asc') {
    params.sortField = 'price'
    params.sortOrder = 'ASC'
  } else if (sortValue.value === 'price_desc') {
    params.sortField = 'price'
    params.sortOrder = 'DESC'
  } else if (sortValue.value === 'newest') {
    params.sortField = 'created_at'
    params.sortOrder = 'DESC'
  }

  return params
}

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await getMarketList(buildParams())
    items.value = res?.list || []
    total.value = Number(res?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '获取市场列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchItems()
}

const handlePageChange = (nextPage) => {
  page.value = nextPage
  fetchItems()
}

const resetFilters = () => {
  searchKeyword.value = ''
  minPrice.value = ''
  maxPrice.value = ''
  sortValue.value = 'default'
  filters.category = ''
  filters.exterior = ''
  handleSearch()
}

const getItemName = (entry) => {
  return entry.inventory?.name || entry.item?.nameCn || entry.item?.name || '未知饰品'
}

const getItemIcon = (entry) => {
  return entry.inventory?.iconUrl || entry.item?.iconUrl || '/default-item.png'
}

const getItemExterior = (entry) => {
  return entry.inventory?.exterior || entry.item?.exterior || ''
}

const getCategoryText = (category) => {
  const map = {
    rifle: '步枪',
    pistol: '手枪',
    smg: '冲锋枪',
    shotgun: '霰弹枪',
    sniper_rifle: '狙击步枪',
    machinegun: '机枪',
    knife: '刀',
    glove: '手套',
    sticker: '贴纸',
    souvenir: '纪念品',
    music: '音乐盒',
    agent: '探员',
    graffiti: '涂鸦',
    case: '武器箱',
    other: '其他'
  }
  return map[category] || category || '未分类'
}

const getExteriorText = (exterior) => {
  const map = {
    FN: '崭新出厂',
    MW: '略有磨损',
    FT: '久经沙场',
    WW: '破损不堪',
    BS: '战痕累累',
    NoPaint: '无涂装'
  }
  return map[exterior] || exterior || '未知外观'
}

const formatPrice = (price) => {
  return Number(price || 0).toFixed(2)
}

const goToDetail = (entry) => {
  const itemId = entry.itemId || entry.item?.id || entry.inventory?.itemId
  if (!itemId) {
    ElMessage.error('物品信息不完整')
    return
  }
  router.push(`/items/${itemId}`)
}

onMounted(() => {
  fetchItems()
})
</script>

<style scoped>
.market-page {
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

.main-content {
  padding-top: 60px;
  background: #e3e3e3;
}

.hero-banner {
  position: relative;
  height: 360px;
}

.market-banner {
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.18), rgba(0, 0, 0, 0.08)),
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

.eyebrow {
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.banner-overlay h1 {
  margin: 0 0 14px;
  font-size: 42px;
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

.section-summary {
  padding-right: 18px;
  color: #8f96a3;
  font-size: 14px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 18px 22px;
  background: #fff;
  border-bottom: 1px solid #efefef;
}

.filter-control {
  width: 150px;
}

.search-control {
  width: 280px;
}

.price-control {
  width: 110px;
}

.filter-action {
  min-width: 88px;
}

.primary-action {
  border-color: #f0b321;
  background: #f0b321;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  min-height: 320px;
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
  background: #5d8a42;
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
  margin: 0 0 8px;
  overflow: hidden;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-meta {
  margin: 0 0 12px;
  color: #9aa0aa;
  font-size: 13px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.card-price {
  margin: 0;
  color: #f0a10a;
  font-size: 18px;
  font-weight: 700;
}

.seller-text {
  color: #9aa0aa;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 0 0 26px;
  background: #fff;
  border-radius: 0 0 10px 10px;
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
    height: 280px;
  }

  .banner-overlay h1 {
    font-size: 32px;
  }

  .banner-overlay p {
    font-size: 15px;
  }

  .filter-control,
  .search-control,
  .price-control {
    width: 100%;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>
