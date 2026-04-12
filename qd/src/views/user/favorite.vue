<template>
  <div class="user-favorites">
    <div class="favorites-header">
      <h2>我的收藏</h2>
      <span class="total">共 {{ itemCount + newsCount }} 件收藏</span>
    </div>

    <div class="favorites-tabs">
      <button
        class="tab"
        :class="{ active: activeTab === FAVORITE_TYPE.ITEM }"
        type="button"
        @click="switchTab(FAVORITE_TYPE.ITEM)"
      >
        饰品收藏
        <span class="badge" v-if="visibleBadgeCount(FAVORITE_TYPE.ITEM) > 0">{{ visibleBadgeCount(FAVORITE_TYPE.ITEM) }}</span>
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === FAVORITE_TYPE.NEWS }"
        type="button"
        @click="switchTab(FAVORITE_TYPE.NEWS)"
      >
        资讯收藏
        <span class="badge" v-if="visibleBadgeCount(FAVORITE_TYPE.NEWS) > 0">{{ visibleBadgeCount(FAVORITE_TYPE.NEWS) }}</span>
      </button>
    </div>

    <div class="content-area" v-loading="loading">
      <template v-if="activeTab === FAVORITE_TYPE.ITEM">
        <div class="items-grid" v-if="items.length > 0">
          <div class="item-card" v-for="fav in items" :key="fav.id">
            <div class="item-image" @click="viewItem(fav.item)">
              <img v-if="fav.item?.iconUrl" :src="fav.item.iconUrl" :alt="getItemDisplayName(fav.item)" />
              <el-icon v-else class="item-placeholder"><ShoppingBag /></el-icon>
              <div class="item-rarity" v-if="getFavoriteBadgeText(fav.item)" :class="getFavoriteBadgeClass(fav.item)">
                {{ getFavoriteBadgeText(fav.item) }}
              </div>
            </div>
            <div class="item-info">
              <div class="item-name" @click="viewItem(fav.item)">{{ getItemDisplayName(fav.item) }}</div>
              <div class="item-subtitle">{{ getFavoriteSubtitle(fav.item) }}</div>
              <div class="item-prices">
                <div class="current-price">
                  <span class="label">参考价</span>
                  <span class="price">¥ {{ formatPrice(getReferencePrice(fav.item)) }}</span>
                </div>
              </div>
            </div>
            <div class="item-actions">
              <el-button type="primary" size="small" @click="viewItem(fav.item)">查看</el-button>
              <el-button size="small" @click="handleRemove(fav)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-else>
          <el-icon class="empty-icon"><ShoppingBag /></el-icon>
          <div class="empty-text">暂无收藏的饰品</div>
          <el-button type="primary" @click="$router.push('/items')">去市场逛逛</el-button>
        </div>
      </template>

      <template v-else>
        <div class="news-list" v-if="newsList.length > 0">
          <div class="news-card" v-for="fav in newsList" :key="fav.id">
            <div class="news-cover" @click="viewNews(fav.news)">
              <img :src="fav.news.coverImage" v-if="fav.news?.coverImage" :alt="fav.news.title" />
              <div class="no-cover" v-else>
                <el-icon><Document /></el-icon>
              </div>
              <div class="news-category">{{ fav.news?.category || '资讯' }}</div>
            </div>
            <div class="news-content">
              <div class="news-title" @click="viewNews(fav.news)">{{ fav.news?.title }}</div>
              <div class="news-summary">{{ fav.news?.summary }}</div>
              <div class="news-meta">
                <span class="author">
                  <el-icon><User /></el-icon>
                  {{ fav.news?.author || '匿名' }}
                </span>
                <span class="views">
                  <el-icon><View /></el-icon>
                  {{ fav.news?.views || 0 }}
                </span>
                <span class="date">{{ formatDate(fav.news?.createdAt) }}</span>
              </div>
            </div>
            <div class="news-actions">
              <el-button type="primary" size="small" @click="viewNews(fav.news)">查看</el-button>
              <el-button size="small" @click="handleRemove(fav)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-else>
          <el-icon class="empty-icon"><Document /></el-icon>
          <div class="empty-text">暂无收藏的资讯</div>
          <el-button type="primary" @click="$router.push('/news')">去资讯看看</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Document, ShoppingBag, User, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getFavorites, removeFavorite } from '@/api/favorite'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const FAVORITE_TYPE = {
  ITEM: 1,
  NEWS: 2
}
const DISMISSED_BADGE_STORAGE_KEY = 'cs2trade.favorite.dismissedBadgeTabs'
const FAVORITE_TYPES = Object.values(FAVORITE_TYPE)

const router = useRouter()
const activeTab = ref(FAVORITE_TYPE.ITEM)
const items = ref([])
const newsList = ref([])
const loading = ref(false)
const itemCount = ref(0)
const newsCount = ref(0)

const readDismissedBadgeTabs = () => {
  const fallback = new Set([FAVORITE_TYPE.ITEM])

  try {
    const raw = window.localStorage.getItem(DISMISSED_BADGE_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    const tabs = Array.isArray(parsed) ? parsed.map(Number).filter((type) => FAVORITE_TYPES.includes(type)) : []
    return new Set([...fallback, ...tabs])
  } catch (error) {
    return fallback
  }
}

const persistDismissedBadgeTabs = () => {
  try {
    window.localStorage.setItem(DISMISSED_BADGE_STORAGE_KEY, JSON.stringify([...dismissedBadgeTabs.value]))
  } catch (error) {
    // Ignore storage errors so the favorites page still works in private mode.
  }
}

const dismissedBadgeTabs = ref(readDismissedBadgeTabs())

const dismissBadgeTab = (tab) => {
  if (!FAVORITE_TYPES.includes(tab)) return
  if (dismissedBadgeTabs.value.has(tab)) {
    persistDismissedBadgeTabs()
    return
  }

  dismissedBadgeTabs.value = new Set([...dismissedBadgeTabs.value, tab])
  persistDismissedBadgeTabs()
}

const hasText = (value) => typeof value === 'string' && value.trim().length > 0

const isUnknownItem = (item) => {
  if (!item) return true
  return String(item.itemId || '').toLowerCase() === 'unknown' ||
    String(item.name || '').toLowerCase() === 'unknown item' ||
    item.nameCn === '未知饰品'
}

const isDisplayableItem = (item) => {
  return Boolean(item?.id) && !isUnknownItem(item) && (hasText(item.nameCn) || hasText(item.name) || hasText(item.iconUrl))
}

const isDisplayableNews = (news) => {
  return Boolean(news?.id) && (hasText(news.title) || hasText(news.summary) || hasText(news.coverImage))
}

const normalizeFavorites = (list, type) => {
  const favorites = Array.isArray(list) ? list : []
  return favorites.filter((fav) => {
    if (type === FAVORITE_TYPE.ITEM) return isDisplayableItem(fav?.item)
    if (type === FAVORITE_TYPE.NEWS) return isDisplayableNews(fav?.news)
    return false
  })
}

const visibleBadgeCount = (type) => {
  if (dismissedBadgeTabs.value.has(type)) return 0
  return type === FAVORITE_TYPE.ITEM ? itemCount.value : newsCount.value
}

const switchTab = (tab) => {
  activeTab.value = tab
  dismissBadgeTab(tab)
  fetchFavorites()
}

const refreshFavoriteCounts = async () => {
  try {
    const [itemFavorites, newsFavorites] = await Promise.all([
      getFavorites(FAVORITE_TYPE.ITEM),
      getFavorites(FAVORITE_TYPE.NEWS)
    ])
    itemCount.value = normalizeFavorites(itemFavorites, FAVORITE_TYPE.ITEM).length
    newsCount.value = normalizeFavorites(newsFavorites, FAVORITE_TYPE.NEWS).length
  } catch (error) {
    console.error(error)
  }
}

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavorites(activeTab.value)
    const list = normalizeFavorites(res, activeTab.value)
    if (activeTab.value === FAVORITE_TYPE.ITEM) {
      items.value = list
      itemCount.value = list.length
    } else {
      newsList.value = list
      newsCount.value = list.length
    }
  } catch (error) {
    ElMessage.error(error?.message || '获取收藏失败')
  } finally {
    loading.value = false
  }
}

const handleRemove = async (fav) => {
  const targetId = activeTab.value === FAVORITE_TYPE.ITEM ? fav.item?.id || fav.itemId : fav.news?.id || fav.newsId
  if (!targetId) {
    ElMessage.warning('收藏目标不存在')
    return
  }

  try {
    await removeFavorite(targetId, activeTab.value)
    if (activeTab.value === FAVORITE_TYPE.ITEM) {
      items.value = items.value.filter((item) => item.id !== fav.id)
      itemCount.value = Math.max(itemCount.value - 1, 0)
    } else {
      newsList.value = newsList.value.filter((news) => news.id !== fav.id)
      newsCount.value = Math.max(newsCount.value - 1, 0)
    }
    ElMessage.success('取消收藏成功')
  } catch (error) {
    ElMessage.error(error?.message || '取消收藏失败')
  }
}

const viewItem = (item) => {
  if (item?.id) {
    router.push(`/items/${item.id}`)
  }
}

const getItemDisplayName = (item) => item?.nameCn || item?.name || '饰品'
const getFavoriteDisplayModel = (item) => getItemDisplayModel(item)
const getFavoriteBadgeText = (item) => getFavoriteDisplayModel(item).primaryBadge.text
const getFavoriteBadgeClass = (item) => {
  const badge = getFavoriteDisplayModel(item).primaryBadge
  if (!badge) return ''
  if (badge.kind === 'quality') return `item-rarity-quality-${badge.code}`
  if (badge.kind === 'category') return `item-rarity-category-${badge.code || 'other'}`
  return `item-rarity-${badge.code || 'UN'}`
}
const getFavoriteSubtitle = (item) => getFavoriteDisplayModel(item).subtitle

const getReferencePrice = (item) => {
  const candidates = [
    item?.steamReferencePrice,
    item?.buffPrice,
    item?.marketPrice
  ]
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0)

  return candidates[0] || null
}

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric.toFixed(2) : '--'
}

const viewNews = (news) => {
  if (news?.id) {
    router.push(`/news/${news.id}`)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  dismissBadgeTab(activeTab.value)
  refreshFavoriteCounts()
  fetchFavorites()
})
</script>

<style scoped>
.user-favorites {
  background: #fff;
  min-height: 100%;
}

.favorites-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
}

.favorites-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.favorites-header .total {
  font-size: 14px;
  color: #999;
}

.favorites-tabs {
  display: flex;
  gap: 10px;
  padding: 15px 25px;
  border-bottom: 1px solid #eee;
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  cursor: pointer;
  border: 0;
  border-radius: 16px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  transition: all 0.2s;
}

.tab.active {
  background: #4b89dc;
  color: #fff;
}

.tab:hover:not(.active) {
  background: #e8e8e8;
}

.tab .badge {
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

.tab.active .badge {
  background: rgba(255, 255, 255, 0.3);
}

.content-area {
  padding: 20px 25px;
  min-height: 400px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.item-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  transition: all 0.2s;
}

.item-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #ddd;
}

.item-image {
  position: relative;
  width: 100%;
  height: 100px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 10px;
}

.item-placeholder {
  width: 100%;
  height: 100%;
  color: rgba(255, 255, 255, 0.45);
  font-size: 34px;
}

.item-rarity {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(71, 85, 105, 0.92);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
}

.item-rarity-FN {
  background: #329545;
}

.item-rarity-MW {
  background: #5d9545;
}

.item-rarity-FT {
  background: #efab3d;
}

.item-rarity-WW {
  background: #8c8f96;
}

.item-rarity-BS {
  background: #c85050;
}

.item-rarity-quality-contraband {
  background: #f5df4d;
  color: #1f2937;
}

.item-rarity-quality-covert {
  background: #d9485f;
}

.item-rarity-quality-classified {
  background: #c84cff;
}

.item-rarity-quality-restricted {
  background: #8b5cf6;
}

.item-rarity-quality-mil-spec {
  background: #3b82f6;
}

.item-rarity-quality-industrial {
  background: #60a5fa;
}

.item-rarity-quality-consumer {
  background: #94a3b8;
}

.item-rarity-quality-extraordinary {
  background: #f97316;
}

.item-rarity-quality-exotic {
  background: #a855f7;
}

.item-rarity-quality-remarkable {
  background: #ec4899;
}

.item-rarity-quality-high-grade {
  background: #4f8ef7;
}

.item-rarity-quality-normal-grade {
  background: #e5e7eb;
  color: #1f2937;
}

.item-rarity-quality-agent-grade,
.item-rarity-category-agent {
  background: #22c55e;
}

.item-rarity-category-sticker,
.item-rarity-category-music {
  background: #2563eb;
}

.item-rarity-category-graffiti,
.item-rarity-category-collectible {
  background: #7c3aed;
}

.item-rarity-category-charm,
.item-rarity-category-tool {
  background: #0f766e;
}

.item-rarity-category-case {
  background: #475569;
}

.item-rarity-category-pass {
  background: #7c2d12;
}

.item-info {
  padding: 10px 12px;
}

.item-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.item-name:hover {
  color: #4b89dc;
}

.item-subtitle {
  margin: -4px 0 8px;
  color: #8a94a6;
  font-size: 12px;
  line-height: 1.4;
}

.item-prices {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.current-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.current-price .label {
  font-size: 11px;
  color: #999;
}

.current-price .price {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

.item-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
}

.item-actions .el-button {
  flex: 1;
  padding: 6px 0;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-card {
  display: flex;
  gap: 15px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  padding: 12px;
  transition: all 0.2s;
}

.news-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #ddd;
}

.news-cover {
  position: relative;
  width: 140px;
  height: 90px;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
}

.news-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 32px;
}

.news-category {
  position: absolute;
  bottom: 6px;
  left: 6px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 3px;
}

.news-content {
  flex: 1;
  min-width: 0;
}

.news-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-title:hover {
  color: #4b89dc;
}

.news-summary {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.news-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.news-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .items-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .items-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .news-card {
    flex-direction: column;
  }

  .news-cover {
    width: 100%;
  }
}
</style>
