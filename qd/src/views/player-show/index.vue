<template>
  <div class="player-show-page">
    <SiteHeader active="player-shows" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="page-main">
      <section class="page-panel">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">玩家秀</div>
            <div class="tab-item plain">社区作品展示</div>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="dialogVisible = true">上传作品</el-button>
          </div>
        </div>

        <div class="show-grid" v-loading="loading">
          <article v-for="show in showList" :key="show.id" class="show-card">
            <div class="show-image">
              <img :src="show.imageUrl || defaultImage" :alt="show.description || '玩家秀作品'" />
            </div>

            <div class="show-info">
              <div class="show-user">
                <el-avatar :size="24" :src="show.avatar" />
                <span class="show-username">{{ show.username || '匿名玩家' }}</span>
              </div>

              <div class="show-desc">{{ show.description || '这位玩家还没有填写描述。' }}</div>

              <div class="show-actions">
                <span class="action-item" :class="{ liked: show.hasLiked }" @click="handleLike(show)">
                  👍 {{ show.likes || 0 }}
                </span>
                <span class="action-item" @click="toggleComments(show)">
                  评论
                </span>
              </div>

              <div v-if="show.showComments" class="comments-box">
                <div v-if="show.comments?.length" class="comment-list">
                  <div v-for="comment in show.comments" :key="comment.id" class="comment-item">
                    <span class="comment-user">{{ comment.username }}：</span>
                    <span class="comment-content">{{ comment.content }}</span>
                  </div>
                </div>

                <div class="add-comment">
                  <el-input v-model="show.newComment" placeholder="说点什么..." size="small">
                    <template #append>
                      <el-button @click="submitComment(show)">发送</el-button>
                    </template>
                  </el-input>
                </div>
              </div>
            </div>
          </article>

          <el-empty v-if="!loading && !showList.length" description="暂无玩家秀内容" />
        </div>
      </section>
    </main>

    <el-dialog v-model="dialogVisible" title="上传玩家秀" width="500px">
      <el-form :model="uploadForm">
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" />
        </el-form-item>

        <el-form-item label="图片链接">
          <el-input v-model="uploadForm.imageUrl" placeholder="请输入图片链接" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  addPlayerShowComment,
  createPlayerShow,
  getPlayerShowComments,
  getPlayerShows,
  hasLikedPlayerShow,
  likePlayerShow
} from '@/api/playerShow'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const defaultImage = 'https://dummyimage.com/400x300/1c1f28/ffffff&text=SHOW'
const userStore = useUserStore()

const showLoginModal = ref(false)
const showList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const uploadForm = ref({
  description: '',
  imageUrl: ''
})

const fetchShows = async () => {
  loading.value = true
  try {
    const res = await getPlayerShows()
    const list = Array.isArray(res) ? res : []
    showList.value = await Promise.all(
      list.map(async (item) => {
        let liked = false
        if (userStore.isLoggedIn) {
          try {
            const likeRes = await hasLikedPlayerShow(item.id)
            liked = likeRes === true || likeRes?.liked
          } catch (error) {
            void error
          }
        }
        return {
          ...item,
          hasLiked: liked,
          showComments: false,
          comments: [],
          newComment: ''
        }
      })
    )
  } catch (error) {
    ElMessage.error(error?.message || '获取玩家秀失败')
  } finally {
    loading.value = false
  }
}

const handleLike = async (show) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  try {
    await likePlayerShow(show.id)
    show.hasLiked = true
    show.likes = Number(show.likes || 0) + 1
  } catch (error) {
    ElMessage.error(error?.message || '点赞失败')
  }
}

const toggleComments = async (show) => {
  show.showComments = !show.showComments
  if (show.showComments && !show.comments.length) {
    try {
      const comments = await getPlayerShowComments(show.id)
      show.comments = Array.isArray(comments) ? comments : []
    } catch (error) {
      ElMessage.error(error?.message || '获取评论失败')
    }
  }
}

const submitComment = async (show) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!show.newComment?.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    await addPlayerShowComment(show.id, show.newComment.trim())
    show.comments.push({
      id: Date.now(),
      username: userStore.userInfo?.username || '我',
      content: show.newComment.trim()
    })
    show.newComment = ''
    ElMessage.success('评论成功')
  } catch (error) {
    ElMessage.error(error?.message || '评论失败')
  }
}

const handleUpload = async () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!uploadForm.value.imageUrl) {
    ElMessage.warning('请先填写图片链接')
    return
  }

  try {
    await createPlayerShow(uploadForm.value)
    ElMessage.success('上传成功')
    dialogVisible.value = false
    uploadForm.value = { description: '', imageUrl: '' }
    fetchShows()
  } catch (error) {
    ElMessage.error(error?.message || '上传失败')
  }
}

onMounted(() => {
  fetchShows()
})
</script>

<style scoped>
.player-show-page {
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

.toolbar-right {
  padding-right: 24px;
}

.show-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  padding: 24px;
}

.show-card {
  border: 1px solid #ececec;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.show-image img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.show-info {
  padding: 16px;
}

.show-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.show-username {
  color: #111827;
  font-weight: 600;
}

.show-desc {
  color: #4b5563;
  line-height: 1.75;
}

.show-actions {
  display: flex;
  gap: 16px;
  margin-top: 14px;
}

.action-item {
  color: #6b7280;
  cursor: pointer;
}

.action-item.liked {
  color: #f0a10a;
}

.comments-box {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #ececec;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.comment-user {
  color: #111827;
  font-weight: 600;
}

.comment-content {
  color: #4b5563;
}

@media (max-width: 960px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .show-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .show-grid {
    grid-template-columns: 1fr;
  }
}
</style>
