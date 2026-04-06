<template>
  <div class="user-favorites">
    <div class="favorites-header">
      <h2>我的收藏</h2>
      <span class="total">共 {{ itemCount + newsCount }} 件收藏</span>
    </div>

    <div class="favorites-tabs">
      <div 
        class="tab" 
        :class="{ active: activeTab === 1 }" 
        @click="switchTab(1)"
      >
        饰品收藏
        <span class="badge" v-if="itemCount > 0">{{ itemCount }}</span>
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 2 }" 
        @click="switchTab(2)"
      >
        资讯收藏
        <span class="badge" v-if="newsCount > 0">{{ newsCount }}</span>
      </div>
    </div>

    <div class="content-area" v-loading="loading">
      <!-- 饰品收藏 -->
      <template v-if="activeTab === 1">
        <div class="items-grid" v-if="items.length > 0">
          <div class="item-card" v-for="fav in items" :key="fav.id">
            <div class="item-image" @click="viewItem(fav.item)">
              <img :src="fav.item?.iconUrl" :alt="getItemDisplayName(fav.item)" />
              <div class="item-rarity" v-if="fav.item?.rarity">{{ fav.item?.rarity }}</div>
            </div>
            <div class="item-info">
              <div class="item-name" @click="viewItem(fav.item)">{{ getItemDisplayName(fav.item) }}</div>
              <div class="item-prices">
                <div class="current-price">
                  <span class="label">最低价</span>
                  <span class="price">¥ {{ formatPrice(fav.item?.buffPrice) }}</span>
                </div>
              </div>
            </div>
            <div class="item-actions">
              <el-button type="primary" size="small" @click="viewItem(fav.item)">购买</el-button>
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

      <!-- 资讯收藏 -->
      <template v-if="activeTab === 2">
        <div class="news-list" v-if="newsList.length > 0">
          <div class="news-card" v-for="fav in newsList" :key="fav.id">
            <div class="news-cover" @click="viewNews(fav.news)">
              <img :src="fav.news?.coverImage" v-if="fav.news?.coverImage" />
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
import { getFavorites, removeFavorite } from '@/api/favorite'
import { Delete, ShoppingBag, Document, User, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref(1)
const items = ref([])
const newsList = ref([])
const loading = ref(false)
const itemCount = ref(0)
const newsCount = ref(0)

const switchTab = (tab) => {
  activeTab.value = tab
  fetchFavorites()
}

const refreshFavoriteCounts = async () => {
  try {
    const [itemFavorites, newsFavorites] = await Promise.all([
      getFavorites(1),
      getFavorites(2)
    ])
    itemCount.value = Array.isArray(itemFavorites) ? itemFavorites.length : 0
    newsCount.value = Array.isArray(newsFavorites) ? newsFavorites.length : 0
  } catch (error) {
    console.error(error)
  }
}

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavorites(activeTab.value)
    const list = Array.isArray(res) ? res : []
    if (activeTab.value === 1) {
      items.value = list
      itemCount.value = list.length
    } else {
      newsList.value = list
      newsCount.value = list.length
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleRemove = async (fav) => {
  try {
    const targetId = activeTab.value === 1 ? fav.itemId : fav.newsId
    await removeFavorite(targetId, activeTab.value)
    if (activeTab.value === 1) {
      items.value = items.value.filter(i => i.id !== fav.id)
      itemCount.value--
    } else {
      newsList.value = newsList.value.filter(n => n.id !== fav.id)
      newsCount.value--
    }
    ElMessage.success('取消收藏成功')
  } catch (error) {
    ElMessage.error('取消失败')
  }
}

const viewItem = (item) => {
  if (item?.id) {
    router.push(`/items/${item.id}`)
  }
}

const getItemDisplayName = (item) => item?.nameCn || item?.name || '未知饰品'

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
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
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

/* 饰品卡片网格 */
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

.item-rarity {
  position: absolute;
  top: 6px;
  right: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
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

/* 资讯列表 */
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

/* 空状态 */
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
</style>
