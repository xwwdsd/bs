<template>
  <div class="news-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link active">资讯</router-link>
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

    <!-- 登录弹窗 -->
    <LoginModal v-model="showLoginModal" />

    <div class="news-container">
      <!-- Left: News List -->
      <div class="news-section">
        <div class="news-header">
          <div class="breadcrumb">游戏资讯 <span class="divider">></span> 全部</div>
        </div>
        
        <div class="tabs-bar">
          <div class="tab-item" :class="{ active: currentCategory === 'all' }" @click="setCategory('all')">全部</div>
          <div class="tab-item" :class="{ active: currentCategory === 'CS2' }" @click="setCategory('CS2')">
            <span class="game-icon cs2"></span> CS2
          </div>
          <div class="tab-item" :class="{ active: currentCategory === 'DOTA2' }" @click="setCategory('DOTA2')">
            <span class="game-icon dota2"></span> DOTA2
          </div>
          <el-button type="primary" size="small" class="post-btn" @click="postDialogVisible = true">文章投稿</el-button>
        </div>

        <div class="news-list" v-loading="loading">
          <div class="news-item" v-for="item in newsList" :key="item.id">
            <div class="news-cover">
              <img :src="item.coverImage" :alt="item.title" />
            </div>
            <div class="news-content">
              <h3 class="news-title">{{ item.title }}</h3>
              <div class="news-tags">
                <span class="tag-cs2" v-if="item.category === 'CS2'">CS2</span>
                <span class="tag-dota2" v-if="item.category === 'DOTA2'">DOTA2</span>
              </div>
              <div class="news-meta">
                <span class="author">作者: {{ item.author }}</span>
                <span class="source">来源: {{ item.source }}</span>
                <span class="time"><el-icon><Clock /></el-icon> {{ formatDate(item.createdAt) }}</span>
              </div>
            </div>
            <div class="news-action">
              <span 
                class="collect-btn" 
                :class="{ 'is-favorite': favoriteIds.has(item.id) }"
                @click="handleFavorite(item)"
              >
                收藏 <el-icon><StarFilled v-if="favoriteIds.has(item.id)" /><Star v-else /></el-icon>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="postDialogVisible" title="文章投稿" width="600px">
      <el-form :model="postForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="postForm.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="postForm.summary" type="textarea" rows="2" placeholder="请输入文章摘要" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="postForm.content" type="textarea" rows="10" placeholder="请输入文章内容" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
            class="upload-demo"
            action="/api/v1/file/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            name="file"
          >
            <img v-if="postForm.coverImage" :src="postForm.coverImage" class="uploaded-img" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-radio-group v-model="postForm.category">
            <el-radio label="CS2">CS2</el-radio>
            <el-radio label="DOTA2">DOTA2</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="postDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitPost" :loading="submitting">投稿</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { Collection, Clock, Star, StarFilled, Pointer, ChatDotSquare, Plus } from '@element-plus/icons-vue'
import { getNews, createNews } from '@/api/news'
import { addFavorite, removeFavorite, checkFavorite } from '@/api/favorite'
import { ElMessage } from 'element-plus'
import LoginModal from '@/components/LoginModal.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'

const userStore = useUserStore()
const showLoginModal = ref(false)
const currentCategory = ref('all')
const newsList = ref([])
const favoriteIds = ref(new Set())
const loading = ref(false)
const submitting = ref(false)
const postDialogVisible = ref(false)
const postForm = ref({
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  category: 'CS2'
})

const uploadHeaders = {
  Authorization: 'Bearer ' + userStore.accessToken
}

const handleCoverSuccess = (res) => {
  if (res.code === 200) {
    postForm.value.coverImage = res.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(res.message || '封面上传失败')
  }
}

const handleUploadError = (err) => {
  ElMessage.error('上传请求失败，请检查网络或登录状态')
  console.error(err)
}

const handleFavorite = async (news) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }
  try {
    if (favoriteIds.value.has(news.id)) {
      await removeFavorite(news.id, 2)
      favoriteIds.value.delete(news.id)
      ElMessage.success('取消收藏成功')
    } else {
      await addFavorite(news.id, 2)
      favoriteIds.value.add(news.id)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

const checkFavorites = async () => {
  if (!userStore.isLoggedIn) return
  for (const news of newsList.value) {
    try {
      const res = await checkFavorite(news.id, 2)
      if (res) {
        favoriteIds.value.add(news.id)
      }
    } catch (error) {
      console.error(error)
    }
  }
}

const beforeUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const setCategory = (cat) => {
  currentCategory.value = cat
  fetchNews()
}

const fetchNews = async () => {
  loading.value = true
  try {
    const res = await getNews(currentCategory.value === 'all' ? '' : currentCategory.value)
    newsList.value = res
    checkFavorites()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

const handleSubmitPost = async () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }
  
  if (!postForm.value.title || !postForm.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  
  submitting.value = true
  try {
    await createNews({
      title: postForm.value.title,
      summary: postForm.value.summary || postForm.value.content.substring(0, 100),
      content: postForm.value.content,
      coverImage: postForm.value.coverImage,
      category: postForm.value.category,
      author: userStore.userInfo?.username || '匿名用户',
      source: '用户投稿'
    })
    ElMessage.success('投稿成功，等待审核')
    postDialogVisible.value = false
    postForm.value = {
      title: '',
      summary: '',
      content: '',
      coverImage: '',
      category: 'CS2'
    }
    fetchNews()
  } catch (error) {
    ElMessage.error(error.message || '投稿失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchNews()
})
</script>

<style scoped>
.news-page {
  background: #171a21;
  min-height: 100vh;
  color: #c7d5e0;
}

.navbar-dark {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 50px;
  height: 64px;
  background: #1c1f28;
  border-bottom: 1px solid #2a2e3b;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
}

.nav-links-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.nav-links-left .nav-link {
  color: #c7d5e0;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
}

.nav-links-left .nav-link:hover,
.nav-links-left .nav-link.active {
  color: #fff;
}

.nav-user-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-nav-links {
  display: flex;
  align-items: center;
  gap: 40px;
}

.user-nav-links .nav-link {
  color: #868a9f !important;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
}

.user-nav-links .nav-link:visited {
  color: #868a9f !important;
}

.user-nav-links .nav-link:hover,
.user-nav-links .nav-link.active,
.user-nav-links .router-link-active {
  color: #fff !important;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #fff;
  font-size: 14px;
}

/* Layout */
.news-container {
  width: 1200px;
  margin: 94px auto 30px;
  display: flex;
  gap: 20px;
}

.news-section {
  width: 100%;
  background: #1c1f28; /* 暗色背景 */
  padding: 20px;
  border-radius: 4px;
}

/* News Styles */
.news-header {
  margin-bottom: 20px;
  color: #868a9f;
  font-size: 14px;
}

.divider {
  margin: 0 5px;
}

.tabs-bar {
  display: flex;
  align-items: center;
  background: #232631;
  padding: 0 20px;
  height: 50px;
  border-bottom: 1px solid #2a2e3b;
}

.tab-item {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: #868a9f;
  cursor: pointer;
  position: relative;
}

.tab-item.active {
  color: #fff;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: #fff; /* 默认白色 */
}

.tab-item:nth-child(2).active::after { background: #eeb425; } /* CS2 黄色 */
.tab-item:nth-child(3).active::after { background: #d32ce6; } /* DOTA2 紫色 */

.game-icon {
  width: 16px;
  height: 16px;
  display: inline-block;
  margin-right: 5px;
  border-radius: 2px;
}
.game-icon.cs2 { background: #eeb425; }
.game-icon.dota2 { background: #d32ce6; }

.post-btn {
  margin-left: auto;
}

.news-item {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid #2a2e3b;
  background: #fff; /* 白色卡片背景 */
  margin-bottom: 10px;
}

/* 适配暗色背景 */
.news-section {
  background: transparent;
  padding: 0;
}

.news-list {
  background: #fff;
}

.news-item {
  border-bottom: 1px solid #f0f0f0;
}

.news-cover {
  width: 200px;
  height: 120px;
  margin-right: 20px;
  overflow: hidden;
  background: #eee;
}

.news-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.news-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.news-title {
  font-size: 18px;
  color: #333;
  margin: 0 0 10px;
  font-weight: 600;
}

.news-tags span {
  display: inline-block;
  padding: 2px 6px;
  font-size: 12px;
  color: #fff;
  border-radius: 2px;
  margin-right: 10px;
}

.tag-cs2 { background: #eeb425; }
.tag-dota2 { background: #d32ce6; }

.news-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 20px;
  align-items: center;
}

.news-action {
  width: 60px;
  display: flex;
  justify-content: flex-end;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.collect-btn {
  display: flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  transition: color 0.3s;
}

.collect-btn.is-favorite {
  color: #ff9800;
}

.collect-btn.is-favorite .el-icon {
  color: #ff9800;
}

/* Player Show Styles */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  color: #fff;
}

.show-item {
  background: #232631;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
}

.show-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.show-username {
  color: #868a9f;
  font-size: 13px;
}

.show-image img {
  width: 100%;
  border-radius: 4px;
}

.show-desc {
  margin: 10px 0;
  font-size: 14px;
  color: #c7d5e0;
}

.show-actions {
  display: flex;
  gap: 20px;
  color: #868a9f;
  font-size: 13px;
  cursor: pointer;
}

.action-item:hover {
  color: #fff;
}

.comments-box {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #3a3f50;
}

.comment-item {
  font-size: 12px;
  margin-bottom: 5px;
  color: #868a9f;
}

.comment-user {
  color: #4b89dc;
  margin-right: 5px;
}

.add-comment {
  margin-top: 10px;
}

.upload-demo {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.upload-demo:hover {
  border-color: #409EFF;
}

.upload-icon {
  font-size: 28px;
  color: #8c939d;
}

.uploaded-img {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}
</style>
