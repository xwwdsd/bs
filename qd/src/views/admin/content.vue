<template>
  <div class="admin-page">
    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="资讯管理" name="news">
        <el-card shadow="never" class="panel">
          <div class="toolbar toolbar-right">
            <el-button type="primary" :icon="Plus" @click="openNews()">新增资讯</el-button>
          </div>

          <el-table :data="newsRows" v-loading="newsLoading" row-key="id">
            <el-table-column prop="id" label="编号" width="80" />
            <el-table-column label="封面" width="92">
              <template #default="{ row }">
                <el-image
                  v-if="row.coverImage"
                  :src="row.coverImage"
                  fit="cover"
                  class="table-image"
                  :preview-src-list="[row.coverImage]"
                  preview-teleported
                />
                <span v-else class="table-placeholder">无图</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="newsTagType(row.status)">{{ newsStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="发布时间" width="180" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openNews(row)">编辑</el-button>
                <el-button size="small" type="success" @click="audit(row, 1)">通过</el-button>
                <el-button size="small" type="warning" @click="audit(row, 2)">驳回</el-button>
                <el-button size="small" type="danger" @click="removeNews(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="轮播图管理" name="banners">
        <el-card shadow="never" class="panel">
          <div class="toolbar toolbar-right">
            <el-button type="primary" :icon="Plus" @click="openBanner()">新增轮播图</el-button>
          </div>

          <el-table :data="bannerRows" v-loading="bannerLoading" row-key="id">
            <el-table-column prop="id" label="编号" width="80" />
            <el-table-column label="图片" width="110">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  fit="cover"
                  class="table-banner"
                  :preview-src-list="[row.imageUrl]"
                  preview-teleported
                />
                <span v-else class="table-placeholder">无图</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openBanner(row)">编辑</el-button>
                <el-button size="small" @click="changeBannerStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" @click="changeSort(row)">排序</el-button>
                <el-button size="small" type="danger" @click="removeBanner(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="newsVisible" :title="newsForm.id ? '编辑资讯' : '新增资讯'" width="760px">
      <el-form :model="newsForm" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="newsForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="newsForm.summary" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="newsForm.category" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="newsForm.author" />
        </el-form-item>
        <el-form-item label="封面">
          <AdminImageUpload
            v-model="newsForm.coverImage"
            :width="240"
            :height="140"
            tip="建议上传资讯封面，后台列表会直接显示缩略图"
          />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="newsForm.content" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNews">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bannerVisible" :title="bannerForm.id ? '编辑轮播图' : '新增轮播图'" width="680px">
      <el-form :model="bannerForm" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="bannerForm.description" />
        </el-form-item>
        <el-form-item label="图片">
          <AdminImageUpload
            v-model="bannerForm.imageUrl"
            :width="260"
            :height="140"
            tip="建议上传横幅图片，便于首页轮播直接使用"
          />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="bannerForm.linkUrl" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="bannerForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminImageUpload from '@/components/admin/AdminImageUpload.vue'
import {
  addBanner,
  auditNews,
  createNews,
  deleteBanner,
  deleteNews,
  getAllNews,
  getBanners,
  updateBanner,
  updateBannerSort,
  updateBannerStatus,
  updateNews
} from '@/api/admin'

const activeTab = ref('news')
const newsLoading = ref(false)
const bannerLoading = ref(false)
const newsRows = ref([])
const bannerRows = ref([])
const newsVisible = ref(false)
const bannerVisible = ref(false)
const newsForm = reactive({})
const bannerForm = reactive({})

const newsStatus = (status) => ({ 0: '待审核', 1: '已通过', 2: '已驳回', 3: '草稿' }[status] || '未知')
const newsTagType = (status) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[status] || 'info')

const reset = (target, source = {}) => {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

const loadNews = async () => {
  newsLoading.value = true
  try {
    newsRows.value = (await getAllNews()) || []
  } finally {
    newsLoading.value = false
  }
}

const loadBanners = async () => {
  bannerLoading.value = true
  try {
    bannerRows.value = (await getBanners()) || []
  } finally {
    bannerLoading.value = false
  }
}

const openNews = (row = {}) => {
  reset(newsForm, { status: 1, ...row })
  newsVisible.value = true
}

const saveNews = async () => {
  if (newsForm.id) {
    await updateNews(newsForm.id, newsForm)
  } else {
    await createNews(newsForm)
  }
  ElMessage.success('资讯保存成功')
  newsVisible.value = false
  loadNews()
}

const audit = async (row, status) => {
  let reason = ''
  if (status === 2) {
    const result = await ElMessageBox.prompt('请输入驳回原因', '资讯审核')
    reason = result.value
  }
  await auditNews(row.id, status, reason)
  ElMessage.success('审核已完成')
  loadNews()
}

const removeNews = async (row) => {
  await ElMessageBox.confirm(`确定删除资讯“${row.title}”吗？`, '删除确认', { type: 'warning' })
  await deleteNews(row.id)
  ElMessage.success('资讯已删除')
  loadNews()
}

const openBanner = (row = {}) => {
  reset(bannerForm, { status: 1, sortOrder: 0, ...row })
  bannerVisible.value = true
}

const saveBanner = async () => {
  if (bannerForm.id) {
    await updateBanner(bannerForm.id, bannerForm)
  } else {
    await addBanner(bannerForm)
  }
  ElMessage.success('轮播图保存成功')
  bannerVisible.value = false
  loadBanners()
}

const changeBannerStatus = async (row) => {
  await updateBannerStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('轮播图状态已更新')
  loadBanners()
}

const changeSort = async (row) => {
  const result = await ElMessageBox.prompt('请输入新的排序值', '轮播图排序', {
    inputValue: String(row.sortOrder || 0)
  })
  await updateBannerSort(row.id, Number(result.value))
  ElMessage.success('轮播图排序已更新')
  loadBanners()
}

const removeBanner = async (row) => {
  await ElMessageBox.confirm(`确定删除轮播图“${row.title}”吗？`, '删除确认', { type: 'warning' })
  await deleteBanner(row.id)
  ElMessage.success('轮播图已删除')
  loadBanners()
}

onMounted(() => {
  loadNews()
  loadBanners()
})
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 16px;
}

.panel {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-right {
  justify-content: flex-end;
}

.table-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: block;
}

.table-banner {
  width: 76px;
  height: 44px;
  border-radius: 8px;
  display: block;
}

.table-placeholder {
  color: #94a3b8;
  font-size: 12px;
}
</style>
