<template>
  <div class="player-show-detail-page">
    <SiteHeader active="player-shows" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="page-main">
      <div v-if="loading" class="detail-loading" v-loading="loading"></div>

      <template v-else-if="show">
        <nav class="breadcrumb">
          <router-link to="/player-shows" class="breadcrumb-link">玩家秀</router-link>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-current">{{ show.username || '匿名玩家' }} 的作品</span>
        </nav>

        <section class="detail-layout">
          <article class="detail-card">
            <div class="detail-image">
              <img :src="show.imageUrl || defaultImage" :alt="show.description || '玩家秀作品'" />
            </div>

            <div class="detail-body">
              <div class="author-row">
                <div class="author-info">
                  <el-avatar :size="44" :src="show.avatar" />
                  <div class="author-meta">
                    <div class="author-name">{{ show.username || '匿名玩家' }}</div>
                    <div class="author-time">发布于 {{ formatDate(show.createdAt) }}</div>
                  </div>
                </div>

                <el-button
                  :type="show.hasLiked ? 'warning' : 'primary'"
                  plain
                  @click="handleLike"
                >
                  点赞 {{ show.likes || 0 }}
                </el-button>
              </div>

              <div class="detail-description">
                {{ show.description || '这位玩家还没有填写描述。' }}
              </div>

              <div class="detail-footer">
                <span>{{ comments.length }} 条评论</span>
                <el-button @click="router.push('/player-shows')">返回列表</el-button>
              </div>
            </div>
          </article>

          <section class="comment-card">
            <div class="comment-header">
              <div>
                <h2>评论区</h2>
                <p>欢迎交流想法、夸夸作品，或者聊聊这张图背后的故事。</p>
              </div>
            </div>

            <div class="comment-editor">
              <el-input
                v-model="newComment"
                type="textarea"
                :rows="4"
                resize="none"
                maxlength="300"
                show-word-limit
                placeholder="写下你的评论..."
              />
              <div class="comment-editor-footer">
                <span class="editor-tip">
                  {{ userStore.isLoggedIn ? '文明发言，让评论区更友好。' : '登录后可以发表评论。' }}
                </span>
                <el-button type="primary" @click="submitComment">发表评论</el-button>
              </div>
            </div>

            <div class="comment-list" v-loading="commentsLoading">
              <template v-if="comments.length">
                <article v-for="comment in comments" :key="comment.id" class="comment-item">
                  <el-avatar :size="36" :src="comment.avatar" />
                  <div class="comment-content">
                    <div class="comment-meta">
                      <span class="comment-user">{{ comment.username || '匿名用户' }}</span>
                      <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                    </div>
                    <div class="comment-text">{{ comment.content }}</div>
                  </div>
                </article>
              </template>

              <el-empty v-else description="还没有评论，来抢沙发吧" />
            </div>
          </section>
        </section>
      </template>

      <div v-else class="detail-empty">
        <el-empty description="未找到这条玩家秀" />
        <el-button type="primary" @click="router.push('/player-shows')">返回玩家秀</el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  addPlayerShowComment,
  getPlayerShowComments,
  getPlayerShows,
  hasLikedPlayerShow,
  likePlayerShow
} from '@/api/playerShow'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const defaultImage = 'https://dummyimage.com/900x700/1c1f28/ffffff&text=SHOW'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showLoginModal = ref(false)
const loading = ref(false)
const commentsLoading = ref(false)
const show = ref(null)
const comments = ref([])
const newComment = ref('')

const resolveShowFromList = async (id) => {
  const list = await getPlayerShows()
  const numericId = Number(id)
  return (Array.isArray(list) ? list : []).find((item) => Number(item?.id) === numericId) || null
}

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
}

const fetchComments = async (id) => {
  if (!id) {
    comments.value = []
    return
  }

  commentsLoading.value = true
  try {
    const res = await getPlayerShowComments(id)
    comments.value = Array.isArray(res) ? res : []
  } catch (error) {
    comments.value = []
    void error
  } finally {
    commentsLoading.value = false
  }
}

const fetchShowDetail = async (id) => {
  if (!id) {
    show.value = null
    comments.value = []
    return
  }

  loading.value = true
  try {
    const res = await resolveShowFromList(id)
    if (!res) {
      show.value = null
      comments.value = []
      return
    }
    let liked = false
    if (userStore.isLoggedIn) {
      try {
        const likeRes = await hasLikedPlayerShow(id)
        liked = likeRes === true || likeRes?.liked
      } catch (error) {
        void error
      }
    }

    show.value = {
      ...res,
      hasLiked: liked
    }
    await fetchComments(id)
    document.title = `${show.value.username || '玩家'}的作品 - 玩家秀`
  } catch (error) {
    show.value = null
    comments.value = []
    void error
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!show.value) return

  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (show.value.hasLiked) {
    ElMessage.info('你已经点过赞了')
    return
  }

  try {
    await likePlayerShow(show.value.id)
    show.value.hasLiked = true
    show.value.likes = Number(show.value.likes || 0) + 1
  } catch (error) {
    void error
  }
}

const submitComment = async () => {
  if (!show.value) return

  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    await addPlayerShowComment(show.value.id, newComment.value.trim())
    newComment.value = ''
    ElMessage.success('评论成功')
    await fetchComments(show.value.id)
  } catch (error) {
    void error
  }
}

watch(
  () => route.params.id,
  (id) => {
    newComment.value = ''
    fetchShowDetail(id)
  },
  { immediate: true }
)
</script>

<style scoped>
.player-show-detail-page {
  min-height: 100vh;
  background: #f3f4f6;
}

.page-main {
  width: min(1160px, calc(100% - 40px));
  margin: 0 auto;
  padding: 104px 0 40px;
}

.detail-loading {
  min-height: 320px;
  border-radius: 18px;
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

.detail-layout {
  display: grid;
  gap: 20px;
}

.detail-card,
.comment-card,
.detail-empty {
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(17, 24, 39, 0.08);
}

.detail-card {
  overflow: hidden;
}

.detail-image img {
  display: block;
  width: 100%;
  max-height: 620px;
  object-fit: cover;
}

.detail-body {
  padding: 24px 28px 28px;
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  color: #111827;
  font-size: 20px;
  font-weight: 700;
}

.author-time {
  color: #94a3b8;
  font-size: 13px;
}

.detail-description {
  margin-top: 18px;
  color: #374151;
  font-size: 16px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  color: #94a3b8;
  font-size: 14px;
}

.comment-card {
  padding: 24px 28px 28px;
}

.comment-header h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
}

.comment-header p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
}

.comment-editor {
  margin-top: 22px;
  padding: 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.comment-editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 14px;
}

.editor-tip {
  color: #94a3b8;
  font-size: 13px;
}

.comment-list {
  margin-top: 24px;
}

.comment-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid #eef2f7;
}

.comment-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.comment-user {
  color: #111827;
  font-weight: 600;
}

.comment-time {
  color: #94a3b8;
  font-size: 13px;
}

.comment-text {
  margin-top: 8px;
  color: #475569;
  line-height: 1.8;
  word-break: break-word;
  white-space: pre-wrap;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 24px;
}

@media (max-width: 768px) {
  .page-main {
    width: min(100%, calc(100% - 24px));
    padding-top: 92px;
  }

  .detail-body,
  .comment-card {
    padding-left: 20px;
    padding-right: 20px;
  }

  .author-row,
  .comment-editor-footer,
  .detail-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
