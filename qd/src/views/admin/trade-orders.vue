<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索订单号或报价编号" clearable @keyup.enter="loadData" />
        <el-input v-model.number="query.userId" placeholder="用户编号" clearable />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="待支付" :value="0" />
          <el-option label="报价中" :value="1" />
      <el-option label="待买家报价" :value="2" />
          <el-option label="待收货" :value="3" />
          <el-option label="已完成" :value="4" />
          <el-option label="已取消" :value="5" />
          <el-option label="纠纷中" :value="6" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="buyerId" label="买家" width="90" />
        <el-table-column prop="sellerId" label="卖家" width="90" />
        <el-table-column prop="price" label="金额" width="110"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        <el-table-column prop="tradeOfferId" label="报价编号" min-width="150" />
    <el-table-column prop="deliveryStage" label="交付阶段" min-width="150">
          <template #default="{ row }">{{ deliveryStageText(row.deliveryStage) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="warning" :disabled="!canCancel(row)" @click="cancelTrade(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="query.page" :page-size="query.size" :total="total"
        @current-change="changePage" @size-change="changeSize" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="交易订单详情" size="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ current.buyerId }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ current.sellerId }}</el-descriptions-item>
        <el-descriptions-item label="报价编号">{{ current.tradeOfferId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="交付阶段">{{ deliveryStageText(current.deliveryStage) }}</el-descriptions-item>
        <el-descriptions-item label="监控错误">{{ current.monitorErrorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { cancelOrder, getAllOrders, getTradeOrderById } from '@/api/admin'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const current = ref(null)
const drawerVisible = ref(false)
const query = reactive({ page: 1, size: 20, keyword: '', userId: null, status: null })
const money = (value) => `￥${Number(value || 0).toFixed(2)}`
const statusText = (status) => ({ 0: '待支付', 1: '报价中', 2: '待买家报价', 3: '待收货', 4: '已完成', 5: '已取消', 6: '纠纷中' }[status] || '未知')
const deliveryStageText = (stage) => ({
  NONE: '未进入交付',
  BUYER_OFFER_SENT: '买家已发送报价',
  SELLER_OFFER_SENT: '报价已发送',
  SELLER_CONFIRMED: '卖家确认报价',
  OFFER_ACCEPTED: '报价已接受',
  BUYER_RECEIVED: '买家库存已匹配'
}[stage] || stage || '-')
const canCancel = (row) => row.status === 0 || row.status === 2

const loadData = async () => {
  loading.value = true
  try {
    const data = await getAllOrders(query)
    rows.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; loadData() }
const changePage = (page) => { query.page = page; loadData() }
const changeSize = (size) => { query.size = size; query.page = 1; loadData() }

const openDetail = async (row) => {
  current.value = await getTradeOrderById(row.id)
  drawerVisible.value = true
}

const cancelTrade = async (row) => {
  await ElMessageBox.confirm(`确定取消交易订单 ${row.orderNo || row.id} 吗？`, '取消确认', { type: 'warning' })
  await cancelOrder(row.id)
  ElMessage.success('交易订单已取消')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 120px 130px auto; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar { grid-template-columns: 1fr; } }
</style>
