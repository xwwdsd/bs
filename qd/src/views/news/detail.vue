<template>
  <div class="news-detail-page">
    <SiteHeader active="news" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="page-main">
      <div v-if="loading" class="detail-loading" v-loading="loading"></div>

      <template v-else-if="news">
        <nav class="breadcrumb">
          <router-link to="/news" class="breadcrumb-link">资讯中心</router-link>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-current">{{ news.title }}</span>
        </nav>

        <article class="detail-card">
          <div class="detail-header">
            <div class="detail-tag-row">
              <span v-if="news.category" class="detail-tag">{{ news.category }}</span>
            </div>
            <h1 class="detail-title">{{ news.title }}</h1>
            <div class="detail-meta">
              <span>作者：{{ news.author || '匿名用户' }}</span>
              <span>来源：{{ news.source || '用户投稿' }}</span>
              <span>{{ formatDate(news.createdAt) }}</span>
              <span>{{ news.views || 0 }} 阅读</span>
            </div>
            <p v-if="news.summary" class="detail-summary">{{ news.summary }}</p>
          </div>

          <div v-if="news.coverImage" class="detail-cover">
            <img :src="news.coverImage" :alt="news.title" />
          </div>

          <div class="detail-content">{{ news.content || '暂无正文内容' }}</div>

          <div class="detail-footer">
            <el-button @click="router.push('/news')">返回资讯列表</el-button>
          </div>
        </article>
      </template>

      <div v-else class="detail-empty">
        <el-empty description="未找到这篇资讯" />
        <el-button type="primary" @click="router.push('/news')">返回资讯列表</el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNewsById } from '@/api/news'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const route = useRoute()
const router = useRouter()

const showLoginModal = ref(false)
const loading = ref(false)
const news = ref(null)

const fetchNewsDetail = async (id) => {
  if (!id) {
    news.value = null
    return
  }

  loading.value = true
  try {
    const res = await getNewsById(id)
    news.value = res || null
    document.title = news.value?.title ? `${news.value.title} - 资讯中心` : '资讯详情'
  } catch (error) {
    news.value = null
    ElMessage.error(error?.message || '获取资讯详情失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
}

watch(
  () => route.params.id,
  (id) => {
    fetchNewsDetail(id)
  },
  { immediate: true }
)
</script>

<style scoped>
.news-detail-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1100px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 40px;
}

.detail-loading {
  min-height: 300px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.08);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  color: #94a3b8;
  font-size: 14px;
}

.breadcrumb-link {
  color: #4e86dc;
  text-decoration: none;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.detail-card {
  overflow: hidden;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
}

.detail-header {
  padding: 32px 36px 20px;
}

.detail-tag-row {
  margin-bottom: 14px;
}

.detail-tag {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 14px;
  border-radius: 999px;
  background: #f0b321;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.detail-title {
  margin: 0;
  color: #111827;
  font-size: 36px;
  line-height: 1.35;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 16px;
  color: #94a3b8;
  font-size: 14px;
}

.detail-summary {
  margin: 18px 0 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.8;
}

.detail-cover {
  padding: 0 36px;
}

.detail-cover img {
  width: 100%;
  max-height: 440px;
  border-radius: 16px;
  object-fit: cover;
}

.detail-content {
  padding: 28px 36px 32px;
  color: #1f2937;
  font-size: 16px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0 36px 32px;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 24px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
}

@media (max-width: 768px) {
  .page-main {
    width: min(100%, calc(100% - 24px));
    padding-top: 92px;
  }

  .detail-header,
  .detail-content,
  .detail-footer {
    padding-left: 20px;
    padding-right: 20px;
  }

  .detail-cover {
    padding: 0 20px;
  }

  .detail-title {
    font-size: 28px;
  }
}
</style>
