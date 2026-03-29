<template>
  <div class="player-show-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link active">玩家秀</router-link>
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

    <div class="show-container">
      <div class="show-header">
        <h2>玩家秀</h2>
        <el-button type="primary" @click="dialogVisible = true">上传作品</el-button>
      </div>

      <div class="show-grid" v-loading="loading">
        <div class="show-card" v-for="show in showList" :key="show.id">
          <div class="show-image">
            <img :src="show.imageUrl" />
          </div>
          <div class="show-info">
            <div class="show-user">
              <el-avatar :size="24" :src="show.avatar" />
              <span class="show-username">{{ show.username }}</span>
            </div>
            <div class="show-desc">{{ show.description }}</div>
            <div class="show-actions">
              <span 
                class="action-item" 
                :class="{ 'has-liked': show.hasLiked }"
                @click="handleLike(show)"
              >
                <el-icon><Pointer /></el-icon> {{ show.likes || 0 }}
              </span>
              <span class="action-item" @click="toggleComments(show)">
                <el-icon><ChatDotSquare /></el-icon> 评论
              </span>
            </div>
            
            <!-- Comments Section -->
            <div class="comments-box" v-if="show.showComments">
              <div class="comment-list" v-if="show.comments && show.comments.length > 0">
                <div class="comment-item" v-for="comment in show.comments" :key="comment.id">
                  <span class="comment-user">{{ comment.username }}:</span>
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
        </div>
      </div>
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="dialogVisible" title="上传玩家秀" width="500px">
      <el-form :model="uploadForm">
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="upload-demo"
            action="/api/v1/file/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            name="file"
          >
            <img v-if="uploadForm.imageUrl" :src="uploadForm.imageUrl" class="uploaded-img" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpload">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { Collection, Pointer, ChatDotSquare, Plus } from '@element-plus/icons-vue'
import { getPlayerShows, createPlayerShow, likePlayerShow, hasLikedPlayerShow, getPlayerShowComments, addPlayerShowComment } from '@/api/playerShow'
import { ElMessage } from 'element-plus'
import LoginModal from '@/components/LoginModal.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'

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
    showList.value = res.map(item => ({
      ...item,
      showComments: false,
      comments: [],
      newComment: '',
      hasLiked: false
    }))
    if (userStore.isLoggedIn) {
      checkLikeStatus()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const checkLikeStatus = async () => {
  for (const show of showList.value) {
    try {
      const liked = await hasLikedPlayerShow(show.id)
      show.hasLiked = liked
    } catch (error) {
      console.error(error)
    }
  }
}

const uploadHeaders = {
  Authorization: 'Bearer ' + userStore.accessToken
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    uploadForm.value.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res.message || '图片上传失败')
  }
}

const handleUploadError = (err) => {
  ElMessage.error('上传请求失败，请检查网络或登录状态')
  console.error(err)
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

const handleUpload = async () => {
  if (!uploadForm.value.imageUrl) {
    return ElMessage.warning('请先上传图片')
  }
  try {
    await createPlayerShow(uploadForm.value)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    uploadForm.value = { description: '', imageUrl: '' }
    fetchShows()
  } catch (error) {
    console.error(error)
  }
}

const handleLike = async (show) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }
  if (show.hasLiked) {
    ElMessage.warning('已经点赞过了')
    return
  }
  try {
    await likePlayerShow(show.id)
    show.likes++
    show.hasLiked = true
    ElMessage.success('点赞成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('点赞失败')
  }
}

const toggleComments = async (show) => {
  show.showComments = !show.showComments
  if (show.showComments && show.comments.length === 0) {
    const res = await getPlayerShowComments(show.id)
    show.comments = res
  }
}

const submitComment = async (show) => {
  if (!show.newComment) return
  try {
    await addPlayerShowComment(show.id, show.newComment)
    show.newComment = ''
    const res = await getPlayerShowComments(show.id)
    show.comments = res
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchShows()
})
</script>

<style scoped>
.player-show-page {
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

.show-container {
  width: 1200px;
  margin: 94px auto 30px;
}

.show-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.show-grid {
  columns: 4 280px;
  column-gap: 20px;
}

.show-card {
  break-inside: avoid;
  background: #232631;
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
}

.show-image img {
  width: 100%;
  display: block;
}

.show-info {
  padding: 15px;
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

.show-desc {
  margin-bottom: 10px;
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

.action-item.has-liked {
  color: #ff9800;
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
