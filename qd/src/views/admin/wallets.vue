<template>
  <div class="admin-page">
    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-input v-model="walletQuery.keyword" placeholder="搜索用户编号、用户名或邮箱" clearable @keyup.enter="loadWallets" />
        <el-input v-model.number="walletQuery.userId" placeholder="用户编号" clearable />
        <el-button type="primary" :icon="Search" @click="searchWallets">查询钱包</el-button>
      </div>
      <el-table :data="wallets" v-loading="walletLoading" row-key="id" @row-click="selectWallet">
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="balance" label="可用余额"><template #default="{ row }">{{ money(row.balance) }}</template></el-table-column>
        <el-table-column prop="frozenAmount" label="冻结金额"><template #default="{ row }">{{ money(row.frozenAmount) }}</template></el-table-column>
        <el-table-column prop="buyOrderAmount" label="求购金"><template #default="{ row }">{{ money(row.buyOrderAmount) }}</template></el-table-column>
        <el-table-column prop="totalRecharge" label="累计充值"><template #default="{ row }">{{ money(row.totalRecharge) }}</template></el-table-column>
        <el-table-column prop="totalWithdraw" label="累计提现"><template #default="{ row }">{{ money(row.totalWithdraw) }}</template></el-table-column>
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="walletQuery.page" :page-size="walletQuery.size" :total="walletTotal"
        @current-change="changeWalletPage" @size-change="changeWalletSize" />
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header>钱包流水</template>
      <div class="toolbar compact">
        <el-input v-model.number="txQuery.userId" placeholder="用户编号" clearable />
        <el-select v-model="txQuery.type" placeholder="流水类型" clearable>
          <el-option label="充值" :value="1" />
          <el-option label="提现" :value="2" />
          <el-option label="收入" :value="3" />
          <el-option label="支出" :value="4" />
          <el-option label="冻结" :value="5" />
          <el-option label="解冻" :value="6" />
        </el-select>
        <el-input v-model="txQuery.orderNo" placeholder="订单号" clearable />
        <el-button type="primary" @click="searchTransactions">查询流水</el-button>
      </div>
      <el-table :data="transactions" v-loading="txLoading" row-key="id">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="type" label="类型" width="90"><template #default="{ row }">{{ txType(row.type) }}</template></el-table-column>
        <el-table-column prop="amount" label="金额"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column prop="balanceAfter" label="变动后余额"><template #default="{ row }">{{ money(row.balanceAfter) }}</template></el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
      <el-pagination class="pager" background layout="prev, pager, next, sizes, total"
        :current-page="txQuery.page" :page-size="txQuery.size" :total="txTotal"
        @current-change="changeTxPage" @size-change="changeTxSize" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getWallets, getWalletTransactions } from '@/api/admin'

const walletLoading = ref(false)
const txLoading = ref(false)
const wallets = ref([])
const transactions = ref([])
const walletTotal = ref(0)
const txTotal = ref(0)
const walletQuery = reactive({ page: 1, size: 10, keyword: '', userId: null })
const txQuery = reactive({ page: 1, size: 10, userId: null, type: null, orderNo: '' })

const money = (value) => `￥${Number(value || 0).toFixed(2)}`
const txType = (type) => ({ 1: '充值', 2: '提现', 3: '收入', 4: '支出', 5: '冻结', 6: '解冻' }[type] || '未知')

const loadWallets = async () => {
  walletLoading.value = true
  try {
    const data = await getWallets(walletQuery)
    wallets.value = data?.list || []
    walletTotal.value = data?.total || 0
  } finally {
    walletLoading.value = false
  }
}
const loadTransactions = async () => {
  txLoading.value = true
  try {
    const data = await getWalletTransactions(txQuery)
    transactions.value = data?.list || []
    txTotal.value = data?.total || 0
  } finally {
    txLoading.value = false
  }
}
const searchWallets = () => { walletQuery.page = 1; loadWallets() }
const searchTransactions = () => { txQuery.page = 1; loadTransactions() }
const changeWalletPage = (page) => { walletQuery.page = page; loadWallets() }
const changeWalletSize = (size) => { walletQuery.size = size; walletQuery.page = 1; loadWallets() }
const changeTxPage = (page) => { txQuery.page = page; loadTransactions() }
const changeTxSize = (size) => { txQuery.size = size; txQuery.page = 1; loadTransactions() }
const selectWallet = (row) => {
  txQuery.userId = row.userId
  txQuery.page = 1
  loadTransactions()
}

onMounted(() => {
  loadWallets()
  loadTransactions()
})
</script>

<style scoped>
.admin-page { display: grid; gap: 16px; }
.panel { border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 140px auto; gap: 12px; margin-bottom: 16px; }
.toolbar.compact { grid-template-columns: 140px 140px minmax(180px, 1fr) auto; }
.pager { margin-top: 16px; justify-content: flex-end; }
@media (max-width: 900px) { .toolbar, .toolbar.compact { grid-template-columns: 1fr; } }
</style>
