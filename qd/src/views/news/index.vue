<template>
  <div class="news-page">
    <SiteHeader active="news" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="page-main">
      <section class="page-panel">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">资讯中心</div>
            <button class="tab-item plain tab-button" :class="{ current: currentCategory === 'all' }" @click="setCategory('all')">
              全部
            </button>
            <button class="tab-item plain tab-button" :class="{ current: currentCategory === 'CS2' }" @click="setCategory('CS2')">
              CS2
            </button>
            <button class="tab-item plain tab-button" :class="{ current: currentCategory === 'DOTA2' }" @click="setCategory('DOTA2')">
              DOTA2
            </button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="postDialogVisible = true">文章投稿</el-button>
          </div>
        </div>

        <div class="news-list" v-loading="loading">
          <article v-for="item in filteredNews" :key="item.id" class="news-item">
            <div class="news-cover">
              <img :src="item.coverImage || defaultCover" :alt="item.title" />
            </div>

            <div class="news-content">
              <div class="news-tags">
                <span v-if="item.category === 'CS2'" class="tag-cs2">CS2</span>
                <span v-if="item.category === 'DOTA2'" class="tag-dota2">DOTA2</span>
              </div>
              <h3 class="news-title">{{ item.title }}</h3>
              <p class="news-summary">{{ item.summary || item.content }}</p>
              <div class="news-meta">
                <span>作者：{{ item.author || '未知作者' }}</span>
                <span>来源：{{ item.source || '平台投稿' }}</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
            </div>

            <div class="news-action">
              <el-button link type="primary" @click="handleFavorite(item)">
                {{ favoriteIds.has(item.id) ? '取消收藏' : '收藏' }}
              </el-button>
            </div>
          </article>

          <el-empty v-if="!loading && filteredNews.length === 0" description="暂无资讯内容" />
        </div>
      </section>
    </main>

    <el-dialog v-model="postDialogVisible" title="文章投稿" width="600px">
      <el-form :model="postForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="postForm.title" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="摘要">
          <el-input v-model="postForm.summary" type="textarea" :rows="2" placeholder="请输入文章摘要" />
        </el-form-item>

        <el-form-item label="内容">
          <el-input v-model="postForm.content" type="textarea" :rows="8" placeholder="请输入文章内容" />
        </el-form-item>

        <el-form-item label="分类">
          <el-radio-group v-model="postForm.category">
            <el-radio label="CS2">CS2</el-radio>
            <el-radio label="DOTA2">DOTA2</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="postDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitPost">投稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getNews, createNews } from '@/api/news'
import { addFavorite, checkFavorite, removeFavorite } from '@/api/favorite'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const defaultCover = 'https://dummyimage.com/320x180/e5e7eb/6b7280&text=NEWS'
const userStore = useUserStore()

const showLoginModal = ref(false)
const loading = ref(false)
const currentCategory = ref('all')
const newsList = ref([])
const favoriteIds = ref(new Set())
const postDialogVisible = ref(false)
const submitting = ref(false)

const postForm = ref({
  title: '',
  summary: '',
  content: '',
  category: 'CS2'
})

const filteredNews = computed(() => {
  if (currentCategory.value === 'all') return newsList.value
  return newsList.value.filter((item) => item.category === currentCategory.value)
})

const fetchNews = async () => {
  loading.value = true
  try {
    const res = await getNews(currentCategory.value === 'all' ? undefined : currentCategory.value)
    newsList.value = Array.isArray(res) ? res : []
    await refreshFavorites()
  } catch (error) {
    ElMessage.error(error?.message || '获取资讯失败')
  } finally {
    loading.value = false
  }
}

const refreshFavorites = async () => {
  if (!userStore.isLoggedIn) {
    favoriteIds.value = new Set()
    return
  }

  const next = new Set()
  for (const item of newsList.value) {
    try {
      const res = await checkFavorite(item.id, 2)
      if (res === true || res?.favorited) {
        next.add(item.id)
      }
    } catch (error) {
      void error
    }
  }
  favoriteIds.value = next
}

const setCategory = (category) => {
  currentCategory.value = category
  fetchNews()
}

const handleFavorite = async (item) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  try {
    if (favoriteIds.value.has(item.id)) {
      await removeFavorite(item.id, 2)
      favoriteIds.value.delete(item.id)
      favoriteIds.value = new Set(favoriteIds.value)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(item.id, 2)
      favoriteIds.value.add(item.id)
      favoriteIds.value = new Set(favoriteIds.value)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const handleSubmitPost = async () => {
  if (!postForm.value.title || !postForm.value.content) {
    ElMessage.warning('请先填写标题和内容')
    return
  }

  submitting.value = true
  try {
    await createNews(postForm.value)
    ElMessage.success('投稿成功')
    postDialogVisible.value = false
    postForm.value = {
      title: '',
      summary: '',
      content: '',
      category: 'CS2'
    }
    fetchNews()
  } catch (error) {
    ElMessage.error(error?.message || '投稿失败')
  } finally {
    submitting.value = false
  }
}

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '-')

onMounted(() => {
  fetchNews()
})
</script>

<style scoped>
.news-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 36px;
}

.page-panel {
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
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

.tab-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.tab-button.current {
  color: #111827;
}

.toolbar-right {
  padding-right: 24px;
}

.news-list {
  padding: 24px;
}

.news-item {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 100px;
  gap: 20px;
  padding: 18px 0;
  border-bottom: 1px solid #eff2f6;
}

.news-cover img {
  width: 100%;
  height: 150px;
  border-radius: 10px;
  object-fit: cover;
}

.news-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.tag-cs2,
.tag-dota2 {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
}

.tag-cs2 {
  background: #f0b321;
}

.tag-dota2 {
  background: #4e86dc;
}

.news-title {
  margin: 0 0 10px;
  color: #111827;
}

.news-summary {
  margin: 0 0 12px;
  color: #4b5563;
  line-height: 1.75;
}

.news-meta {
  display: flex;
  gap: 18px;
  color: #9ca3af;
  font-size: 13px;
  flex-wrap: wrap;
}

.news-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .news-item {
    grid-template-columns: 1fr;
  }

  .news-action {
    justify-content: flex-start;
  }
}
</style>
