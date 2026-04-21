<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <template #header>玩家秀帖子</template>
      <div class="toolbar">
        <el-input v-model="showQuery.keyword" placeholder="搜索描述或用户" clearable @keyup.enter="loadShows" />
        <el-input v-model.number="showQuery.userId" placeholder="用户编号" clearable />
        <el-button type="primary" :icon="Search" @click="searchShows">查询</el-button>
      </div>
      <el-table :data="shows" v-loading="showLoading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="likes" label="点赞" width="90" />
        <el-table-column prop="createdAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeShow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="showQuery.page" :page-size="showQuery.size" :total="showTotal"
        @current-change="changeShowPage" @size-change="changeShowSize" />
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header>评论管理</template>
      <div class="toolbar">
        <el-input v-model="commentQuery.keyword" placeholder="搜索评论内容" clearable @keyup.enter="loadComments" />
        <el-input v-model.number="commentQuery.showId" placeholder="帖子编号" clearable />
        <el-input v-model.number="commentQuery.userId" placeholder="用户编号" clearable />
        <el-button type="primary" @click="searchComments">查询评论</el-button>
      </div>
      <el-table :data="comments" v-loading="commentLoading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="showId" label="帖子" width="90" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="removeComment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="commentQuery.page" :page-size="commentQuery.size" :total="commentTotal"
        @current-change="changeCommentPage" @size-change="changeCommentSize" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { deletePlayerShow, deletePlayerShowComment, getAllPlayerShows, getPlayerShowComments } from '@/api/admin'

const showLoading = ref(false)
const commentLoading = ref(false)
const shows = ref([])
const comments = ref([])
const showTotal = ref(0)
const commentTotal = ref(0)
const showQuery = reactive({ page: 1, size: 10, keyword: '', userId: null })
const commentQuery = reactive({ page: 1, size: 10, keyword: '', showId: null, userId: null })

const loadShows = async () => {
  showLoading.value = true
  try {
    const data = await getAllPlayerShows(showQuery)
    shows.value = data?.list || []
    showTotal.value = data?.total || 0
  } finally {
    showLoading.value = false
  }
}
const loadComments = async () => {
  commentLoading.value = true
  try {
    const data = await getPlayerShowComments(commentQuery)
    comments.value = data?.list || []
    commentTotal.value = data?.total || 0
  } finally {
    commentLoading.value = false
  }
}
const searchShows = () => { showQuery.page = 1; loadShows() }
const searchComments = () => { commentQuery.page = 1; loadComments() }
const changeShowPage = (page) => { showQuery.page = page; loadShows() }
const changeShowSize = (size) => { showQuery.size = size; showQuery.page = 1; loadShows() }
const changeCommentPage = (page) => { commentQuery.page = page; loadComments() }
const changeCommentSize = (size) => { commentQuery.size = size; commentQuery.page = 1; loadComments() }
const removeShow = async (row) => {
  await ElMessageBox.confirm(`确定删除玩家秀 ${row.id} 吗？`, '删除确认', { type: 'warning' })
  await deletePlayerShow(row.id)
  ElMessage.success('删除成功')
  loadShows()
}
const removeComment = async (row) => {
  await ElMessageBox.confirm(`确定删除评论 ${row.id} 吗？`, '删除确认', { type: 'warning' })
  await deletePlayerShowComment(row.id)
  ElMessage.success('删除成功')
  loadComments()
}
onMounted(() => {
  loadShows()
  loadComments()
})
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 120px 120px auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar { grid-template-columns: 1fr; } }
</style>
