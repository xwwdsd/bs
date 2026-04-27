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
            <el-button type="primary" @click="openUploadDialog">上传作品</el-button>
          </div>
        </div>

        <div class="show-grid" v-loading="loading">
          <article
            v-for="show in showList"
            :key="show.id"
            class="show-card"
            role="button"
            tabindex="0"
            @click="openShowDetail(show.id)"
            @keydown.enter.prevent="openShowDetail(show.id)"
            @keydown.space.prevent="openShowDetail(show.id)"
          >
            <div class="show-image">
              <img :src="show.imageUrl || defaultImage" :alt="show.description || '玩家秀作品'" />
              <div class="show-image-overlay">
                <span>查看详情</span>
              </div>
            </div>

            <div class="show-info">
              <div class="show-user">
                <el-avatar :size="28" :src="show.avatar" />
                <div class="show-user-meta">
                  <span class="show-username">{{ show.username || '匿名玩家' }}</span>
                  <span class="show-time">{{ formatDate(show.createdAt) }}</span>
                </div>
              </div>

              <div class="show-desc">{{ show.description || '这位玩家还没有填写描述。' }}</div>

              <div class="show-actions">
                <button
                  type="button"
                  class="action-item"
                  :class="{ liked: show.hasLiked }"
                  @click.stop="handleLike(show)"
                >
                  点赞 {{ show.likes || 0 }}
                </button>
                <button type="button" class="action-item" @click.stop="openShowDetail(show.id)">
                  评论
                </button>
              </div>
            </div>
          </article>

          <el-empty v-if="!loading && !showList.length" description="暂无玩家秀内容" />
        </div>
      </section>
    </main>

    <el-dialog
      v-model="dialogVisible"
      title="上传玩家秀"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetUploadForm"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="简单介绍一下你的作品（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="图片">
          <AdminImageUpload
            v-model="uploadForm.imageUrl"
            width="100%"
            :height="220"
            tip="支持 JPG、PNG、WEBP，上传后会直接作为玩家秀封面"
            button-text="上传玩家秀图片"
          />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  createPlayerShow,
  getPlayerShows,
  hasLikedPlayerShow,
  likePlayerShow
} from '@/api/playerShow'
import AdminImageUpload from '@/components/admin/AdminImageUpload.vue'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'

const defaultImage = 'https://dummyimage.com/400x300/1c1f28/ffffff&text=SHOW'

const router = useRouter()
const userStore = useUserStore()

const showLoginModal = ref(false)
const showList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const uploadForm = ref({
  description: '',
  imageUrl: ''
})

const resetUploadForm = () => {
  uploadForm.value = {
    description: '',
    imageUrl: ''
  }
}

const formatDate = (value) => {
  if (!value) return '刚刚发布'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '刚刚发布' : date.toLocaleString('zh-CN')
}

const openShowDetail = (id) => {
  router.push(`/player-shows/${id}`)
}

const openUploadDialog = () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  dialogVisible.value = true
}

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
          hasLiked: liked
        }
      })
    )
  } catch (error) {
    showList.value = []
  } finally {
    loading.value = false
  }
}

const handleLike = async (show) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (show.hasLiked) {
    ElMessage.info('你已经点过赞了')
    return
  }

  try {
    await likePlayerShow(show.id)
    show.hasLiked = true
    show.likes = Number(show.likes || 0) + 1
  } catch (error) {
    void error
  }
}

const handleUpload = async () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (!uploadForm.value.imageUrl) {
    ElMessage.warning('请先上传图片')
    return
  }

  try {
    await createPlayerShow(uploadForm.value)
    ElMessage.success('上传成功')
    dialogVisible.value = false
    resetUploadForm()
    fetchShows()
  } catch (error) {
    void error
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
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.show-card:hover,
.show-card:focus-visible {
  border-color: #c9d8f6;
  box-shadow: 0 18px 36px rgba(78, 134, 220, 0.14);
  transform: translateY(-4px);
  outline: none;
}

.show-image {
  position: relative;
}

.show-image img {
  display: block;
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.show-image-overlay {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
  background: linear-gradient(180deg, transparent, rgba(15, 23, 42, 0.55));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.show-info {
  padding: 16px;
}

.show-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.show-user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.show-username {
  color: #111827;
  font-weight: 600;
}

.show-time {
  color: #94a3b8;
  font-size: 12px;
}

.show-desc {
  min-height: 56px;
  color: #4b5563;
  line-height: 1.75;
  word-break: break-word;
}

.show-actions {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.action-item {
  padding: 0;
  border: 0;
  background: transparent;
  color: #6b7280;
  font: inherit;
  cursor: pointer;
}

.action-item:hover {
  color: #4e86dc;
}

.action-item.liked {
  color: #f0a10a;
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
  .page-main {
    width: min(100%, calc(100% - 24px));
    padding-top: 92px;
  }

  .show-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}
</style>
