<template>
  <div class="orders-page">
    <SiteHeader active="market" />

    <main class="main-content">
      <section class="market-section">
        <div class="section-header">
          <div class="tab-group">
            <div class="tab-item active-blue">我的求购</div>
          </div>
          <div class="section-more" @click="showCreateDialog = true">
            发布求购
            <el-icon><Plus /></el-icon>
          </div>
        </div>

        <div class="toolbar">
          <el-radio-group v-model="statusFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="1">进行中</el-radio-button>
            <el-radio-button label="2">已完成</el-radio-button>
            <el-radio-button label="0">已取消</el-radio-button>
          </el-radio-group>
        </div>

        <div class="placeholder-wrap">
          <el-empty description="当前求购功能正在整理中，页面入口和样式已独立出来，后续会继续接入完整数据。" />
        </div>
      </section>
    </main>

    <el-dialog v-model="showCreateDialog" title="发布求购" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="饰品" prop="itemId">
          <el-select v-model="createForm.itemId" placeholder="请选择饰品" filterable style="width: 100%">
            <el-option v-for="item in itemList" :key="item.id" :label="item.nameCn || item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="求购价格" prop="price">
          <el-input-number v-model="createForm.price" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>

        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="createForm.quantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getItemList } from '@/api/item'
import SiteHeader from '@/components/SiteHeader.vue'

const statusFilter = ref('')
const showCreateDialog = ref(false)
const creating = ref(false)
const itemList = ref([])
const createFormRef = ref(null)

const createForm = ref({
  itemId: null,
  price: 0,
  quantity: 1
})

const createRules = {
  itemId: [{ required: true, message: '请选择饰品', trigger: 'change' }],
  price: [{ required: true, message: '请输入求购价格', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入求购数量', trigger: 'blur' }]
}

const fetchItems = async () => {
  try {
    const res = await getItemList({ page: 1, size: 100 })
    itemList.value = res?.list || []
  } catch (error) {
    ElMessage.error(error?.message || '获取饰品列表失败')
  }
}

const handleCreate = async () => {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
    creating.value = true
    setTimeout(() => {
      creating.value = false
      showCreateDialog.value = false
      ElMessage.success('求购需求已暂存，后续将继续接入完整发布接口')
    }, 400)
  } catch (error) {
    void error
  }
}

onMounted(() => {
  fetchItems()
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #e3e3e3;
}

.main-content {
  padding-top: 96px;
}

.market-section {
  width: min(1240px, calc(100% - 40px));
  margin: 24px auto 0;
  border: 1px solid #e6e8ec;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
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
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
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

.section-more {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 24px;
  color: #9299a7;
  cursor: pointer;
}

.toolbar {
  padding: 22px 24px 0;
}

.placeholder-wrap {
  padding: 60px 24px 64px;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
