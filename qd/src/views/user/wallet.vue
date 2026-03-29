<template>
  <div class="user-wallet">
    <!-- Top Stats -->
    <div class="wallet-header">
      <div class="stat-box">
        <div class="label">总资金 ></div>
        <div class="value">¥ {{ ((wallet.balance || 0) + (wallet.frozenAmount || 0)).toFixed(2) }}</div>
      </div>
      <div class="stat-box">
        <div class="label">余额</div>
        <div class="value highlight">¥ {{ wallet.balance?.toFixed(2) || '0.00' }}</div>
      </div>
      <div class="stat-box">
        <div class="label">求购金</div>
        <div class="value highlight">¥ {{ wallet.buyOrderAmount?.toFixed(2) || '0.00' }}</div>
      </div>
      <div class="stat-box">
        <div class="label">锁定中</div>
        <div class="value highlight">¥ {{ wallet.frozenAmount?.toFixed(2) || '0.00' }}</div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="wallet-tabs">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'settle' }"
        @click="activeTab = 'settle'"
      >待结算金额</div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'recharge' }"
        @click="activeTab = 'recharge'"
      >充值</div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'withdraw' }"
        @click="activeTab = 'withdraw'"
      >提现</div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'buyorder' }"
        @click="activeTab = 'buyorder'"
      >求购金</div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'transactions' }"
        @click="activeTab = 'transactions'; fetchTransactions()"
      >资金流水</div>
    </div>

    <!-- Content Area -->
    <div class="wallet-content">
      <!-- 待结算金额 Tab -->
      <div v-if="activeTab === 'settle'" class="tab-content">
        <div class="notice-bar">
          因Steam交易保护机制更新，饰品出售成功后，订单收入将在7天后结算到账。
        </div>
        <div class="settle-info">
          <div class="info-block">
            <div class="sub-label">待结算金额</div>
            <div class="sub-value">¥ 0.00</div>
          </div>
          <div class="info-block right">
            <div class="sub-label">到余额</div>
            <div class="sub-value green">¥ 0.00 <span class="tag">¥ 0.00 可用于购买</span></div>
          </div>
          <div class="info-block right">
            <div class="sub-label">已使用 ></div>
            <div class="sub-value">¥ 0.00</div>
          </div>
        </div>
        <div class="transaction-table">
          <div class="table-header">
            <span>结算时间</span>
            <span>待结算总金额 (元)</span>
            <span>结算到支付宝 (元)</span>
            <span>结算到余额 (元)</span>
            <span>订单数量 (单)</span>
          </div>
          <div class="empty-data">
            <div class="logo-text">CS2Trade</div>
            <div>暂无待结算数据</div>
          </div>
        </div>
      </div>

      <!-- 充值 Tab -->
      <div v-if="activeTab === 'recharge'" class="tab-content">
        <div class="recharge-page">
          <div class="page-title">充值到余额</div>
          <div class="recharge-form">
            <div class="form-row">
              <div class="form-label">充值金额</div>
              <div class="form-input">
                <div class="amount-display">
                  <span class="currency">¥</span>
                  <span class="amount">{{ rechargeForm.amount.toFixed(2) }}</span>
                </div>
                <div class="amount-tips">
                  <span>单次充值限额 ¥10.00-¥50000.00</span>
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-label"></div>
              <div class="quick-amounts">
                <div 
                  v-for="amt in [100, 500, 1000, 5000]" 
                  :key="amt"
                  class="amount-btn"
                  :class="{ active: rechargeForm.amount === amt }"
                  @click="rechargeForm.amount = amt"
                >
                  {{ amt }}
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-label">支付方式</div>
              <div class="pay-methods">
                <div 
                  class="pay-method"
                  :class="{ active: rechargeForm.channel === 'alipay' }"
                  @click="rechargeForm.channel = 'alipay'"
                >
                  <div class="pay-icon alipay">支</div>
                  <div class="pay-name">支付宝</div>
                  <div class="pay-check" v-if="rechargeForm.channel === 'alipay'">✓</div>
                </div>
                <div 
                  class="pay-method"
                  :class="{ active: rechargeForm.channel === 'wechat' }"
                  @click="rechargeForm.channel = 'wechat'"
                >
                  <div class="pay-icon wechat">微</div>
                  <div class="pay-name">微信支付</div>
                  <div class="pay-check" v-if="rechargeForm.channel === 'wechat'">✓</div>
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-label"></div>
              <div class="form-actions">
                <el-button 
                  type="primary" 
                  size="large" 
                  @click="handleRecharge" 
                  :loading="rechargeLoading"
                  class="submit-btn"
                >
                  立即充值
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 提现 Tab -->
      <div v-if="activeTab === 'withdraw'" class="tab-content">
        <div class="withdraw-page">
          <div class="page-title">提现</div>
          <div class="withdraw-form">
            <!-- 提现金额 -->
            <div class="form-row">
              <div class="form-label">提现金额</div>
              <div class="form-input">
                <div class="amount-input-box">
                  <el-icon class="amount-icon"><Wallet /></el-icon>
                  <el-input-number 
                    v-model="withdrawForm.amount" 
                    :min="1" 
                    :max="wallet.balance || 0"
                    :precision="2"
                    :controls="false"
                    placeholder="最多可提¥0"
                    class="amount-input-box-inner"
                  />
                </div>
              </div>
            </div>

            <!-- 收款账号 -->
            <div class="form-row">
              <div class="form-label">收款账号</div>
              <div class="form-input">
                <div 
                  class="account-select-box bank"
                  :class="{ active: withdrawForm.channel === 'bank' }"
                  @click="withdrawForm.channel = 'bank'"
                >
                  <div class="account-icon-wrapper bank-icon">
                    <span class="icon-text">卡</span>
                  </div>
                  <div class="account-info">
                    <div class="account-type">银行卡</div>
                    <div class="account-number" v-if="userBankAccount">{{ userBankAccount }}</div>
                    <div class="account-placeholder" v-else>请绑定银行卡</div>
                  </div>
                  <div class="check-icon" v-if="withdrawForm.channel === 'bank'">✓</div>
                </div>
                <div 
                  class="account-select-box alipay"
                  :class="{ active: withdrawForm.channel === 'alipay' }"
                  @click="withdrawForm.channel = 'alipay'"
                >
                  <div class="account-icon-wrapper alipay-icon">
                    <span class="icon-text">支</span>
                  </div>
                  <div class="account-info">
                    <div class="account-type">支付宝</div>
                    <div class="account-number" v-if="userAlipayAccount">{{ userAlipayAccount }}</div>
                    <div class="account-placeholder" v-else>请绑定支付宝</div>
                  </div>
                  <div class="check-icon" v-if="withdrawForm.channel === 'alipay'">✓</div>
                </div>
              </div>
            </div>

            <!-- 服务费 -->
            <div class="form-row">
              <div class="form-label">服务费</div>
              <div class="form-input">
                <div class="service-fee">
                  <span class="fee-currency">¥</span>
                  <span class="fee-amount">0</span>
                </div>
              </div>
            </div>

            <!-- 确认按钮 -->
            <div class="form-row">
              <div class="form-label"></div>
              <div class="form-actions">
                <el-button 
                  type="info" 
                  size="large" 
                  class="submit-btn"
                  :disabled="!withdrawForm.amount || withdrawForm.amount <= 0"
                  @click="handleWithdraw"
                  :loading="withdrawLoading"
                >
                  确认提现
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 求购金 Tab -->
      <div v-if="activeTab === 'buyorder'" class="tab-content">
        <div class="buyorder-info">
          <div class="info-card">
            <div class="card-title">求购金说明</div>
            <div class="card-content">
              <p>求购金是用于发布求购订单的专项资金。</p>
              <p>发布求购订单时，系统会冻结相应的求购金。</p>
              <p>求购订单取消或完成后，求购金将自动解冻。</p>
            </div>
          </div>
          <div class="info-stats">
            <div class="stat-item">
              <div class="stat-label">当前求购金</div>
              <div class="stat-value">¥ {{ wallet.buyOrderAmount?.toFixed(2) || '0.00' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">冻结中求购金</div>
              <div class="stat-value">¥ {{ wallet.frozenBuyOrderAmount?.toFixed(2) || '0.00' }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">可用求购金</div>
              <div class="stat-value highlight">¥ {{ ((wallet.buyOrderAmount || 0) - (wallet.frozenBuyOrderAmount || 0)).toFixed(2) }}</div>
            </div>
          </div>
        </div>
        <div class="transaction-table">
          <div class="table-header">
            <span>时间</span>
            <span>类型</span>
            <span>金额 (元)</span>
            <span>关联订单</span>
            <span>备注</span>
          </div>
          <div v-if="buyOrderTransactions.length > 0" class="table-body">
            <div v-for="item in buyOrderTransactions" :key="item.id" class="table-row">
              <span>{{ formatDate(item.createdAt) }}</span>
              <span>{{ item.typeText }}</span>
              <span :class="{ 'amount-positive': item.amount > 0, 'amount-negative': item.amount < 0 }">
                {{ item.amount > 0 ? '+' : '' }}{{ item.amount?.toFixed(2) }}
              </span>
              <span>{{ item.relatedOrderId || '-' }}</span>
              <span>{{ item.description || '-' }}</span>
            </div>
          </div>
          <div v-else class="empty-data">
            <div class="logo-text">CS2Trade</div>
            <div>暂无求购金记录</div>
          </div>
        </div>
      </div>

      <!-- 资金流水 Tab -->
      <div v-if="activeTab === 'transactions'" class="tab-content">
        <div class="transaction-filters">
          <el-radio-group v-model="transactionType" @change="fetchTransactions">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="1">充值</el-radio-button>
            <el-radio-button :label="2">提现</el-radio-button>
            <el-radio-button :label="3">收入</el-radio-button>
            <el-radio-button :label="4">支出</el-radio-button>
          </el-radio-group>
        </div>
        <div class="transaction-table">
          <div class="table-header">
            <span>时间</span>
            <span>类型</span>
            <span>金额 (元)</span>
            <span>余额 (元)</span>
            <span>描述</span>
          </div>
          <div v-if="transactions.length > 0" class="table-body">
            <div v-for="item in transactions" :key="item.id" class="table-row">
              <span>{{ formatDate(item.createdAt) }}</span>
              <span>{{ getTransactionTypeText(item.type) }}</span>
              <span :class="{ 'amount-positive': isIncomeType(item.type), 'amount-negative': isExpenseType(item.type) }">
                {{ isIncomeType(item.type) ? '+' : '-' }}{{ item.amount?.toFixed(2) }}
              </span>
              <span>{{ item.balanceAfter?.toFixed(2) }}</span>
              <span>{{ item.description || '-' }}</span>
            </div>
          </div>
          <div v-else class="empty-data">
            <div class="logo-text">CS2Trade</div>
            <div>暂无资金流水记录</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Wallet, WalletFilled, UserFilled, ArrowDown } from '@element-plus/icons-vue'

const wallet = ref({
  balance: 0,
  frozenAmount: 0,
  buyOrderAmount: 0,
  frozenBuyOrderAmount: 0
})

const activeTab = ref('settle')
const transactions = ref([])
const buyOrderTransactions = ref([])
const transactionType = ref(null)

// 用户绑定的账号
const userAlipayAccount = ref('')
const userBankAccount = ref('')

// 充值相关
const rechargeLoading = ref(false)
const rechargeForm = ref({ 
  amount: 100, 
  channel: 'alipay' 
})

// 提现相关
const withdrawLoading = ref(false)
const withdrawForm = ref({ 
  amount: undefined,
  channel: 'alipay',
  account: ''
})

const fetchWallet = async () => {
  try {
    const data = await request.get('/v1/wallet/my')
    wallet.value = data
  } catch (error) {
    console.error(error)
  }
}

const fetchUserAccounts = async () => {
  try {
    const data = await request.get('/v1/user/payment-accounts')
    if (data) {
      userAlipayAccount.value = data.alipay || ''
      userBankAccount.value = data.bankCard || ''
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchTransactions = async () => {
  try {
    const params = transactionType.value !== null ? { type: transactionType.value } : {}
    const data = await request.get('/v1/wallet/transactions', { params })
    transactions.value = data || []
  } catch (error) {
    console.error(error)
  }
}

const handleRecharge = async () => {
  if (!rechargeForm.value.amount || rechargeForm.value.amount <= 0) {
    ElMessage.error('请输入充值金额')
    return
  }
  
  rechargeLoading.value = true
  try {
    await request.post('/v1/wallet/recharge', rechargeForm.value)
    ElMessage.success('充值成功')
    rechargeForm.value = { amount: 100, channel: 'alipay' }
    await fetchWallet()
  } catch (error) {
    ElMessage.error(error.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
}

const handleWithdraw = async () => {
  if (!withdrawForm.value.amount || withdrawForm.value.amount <= 0) {
    ElMessage.error('请输入提现金额')
    return
  }
  
  if (withdrawForm.value.amount > wallet.value.balance) {
    ElMessage.error('提现金额不能超过可用余额')
    return
  }
  
  withdrawLoading.value = true
  try {
    await request.post('/v1/wallet/withdraw', withdrawForm.value)
    ElMessage.success('提现申请已提交')
    withdrawForm.value = { amount: undefined, channel: 'alipay', account: '' }
    await fetchWallet()
  } catch (error) {
    ElMessage.error(error.message || '提现失败')
  } finally {
    withdrawLoading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getTransactionTypeText = (type) => {
  const typeMap = {
    1: '充值',
    2: '提现',
    3: '收入',
    4: '支出',
    5: '冻结',
    6: '解冻'
  }
  return typeMap[type] || '未知'
}

const isIncomeType = (type) => {
  return [1, 3, 6].includes(type)
}

const isExpenseType = (type) => {
  return [2, 4, 5].includes(type)
}

onMounted(() => {
  fetchWallet()
  fetchUserAccounts()
})
</script>

<style scoped>
.user-wallet {
  background: #171a21;
}

.wallet-header {
  display: flex;
  background: #171a21;
  padding: 30px 40px;
  background-size: cover;
  color: #fff;
}

.stat-box {
  flex: 1;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  padding-left: 20px;
}

.stat-box:last-child {
  border-right: none;
}

.stat-box .label {
  color: #868a9f;
  font-size: 14px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-box .value {
  font-size: 24px;
  font-weight: 600;
}

.stat-box .value.highlight {
  color: #eeb425;
}

.wallet-tabs {
  background: #1c1f28;
  display: flex;
  padding: 0 20px;
}

.tab-item {
  padding: 15px 30px;
  color: #868a9f;
  cursor: pointer;
  font-size: 16px;
}

.tab-item.active {
  color: #fff;
  font-weight: 600;
  border-bottom: 3px solid #4b89dc;
}

.tab-item:hover {
  color: #fff;
}

/* White content area starts here */
.wallet-content {
  background: #fff;
  padding: 30px 40px;
  min-height: 500px;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.notice-bar {
  background: #fff8e6;
  color: #e6a23c;
  padding: 10px 40px;
  font-size: 12px;
  text-align: center;
  border-bottom: 1px solid #faecd8;
}

.settle-info {
  background: #f7f8fa;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.info-block .sub-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.info-block .sub-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.info-block.right {
  text-align: right;
}

.info-block .green {
  color: #67c23a;
}

.tag {
  font-size: 12px;
  color: #67c23a;
  background: #e1f3d8;
  padding: 2px 5px;
  border-radius: 2px;
  margin-left: 5px;
}

/* 充值页面样式 */
.recharge-page,
.withdraw-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.form-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 25px;
}

.form-label {
  width: 100px;
  color: #666;
  font-size: 14px;
  padding-top: 10px;
}

.form-input {
  flex: 1;
}

.amount-display {
  display: flex;
  align-items: baseline;
  gap: 5px;
  padding: 10px 0;
  border-bottom: 2px solid #409eff;
}

.amount-display .currency {
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.amount-display .amount {
  font-size: 32px;
  color: #333;
  font-weight: 600;
}

.amount-tips {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.quick-amounts {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.amount-btn {
  padding: 8px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
}

.amount-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.amount-btn.active {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
}

.pay-methods {
  display: flex;
  gap: 15px;
}

.pay-method {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 150px;
  position: relative;
}

.pay-method:hover {
  border-color: #409eff;
}

.pay-method.active {
  border-color: #409eff;
  background: #f0f9ff;
}

.pay-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.pay-icon.alipay {
  background: #1677ff;
}

.pay-icon.wechat {
  background: #07c160;
}

.pay-icon.bank {
  background: #ff6b6b;
}

.pay-name {
  font-size: 14px;
  color: #333;
}

.pay-check {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.form-actions {
  margin-top: 20px;
}

.submit-btn {
  width: 200px;
  height: 44px;
  font-size: 16px;
}

/* 提现页面样式 */
.amount-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 2px solid #409eff;
}

.amount-input-wrapper .currency {
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.amount-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0;
}

.amount-input :deep(.el-input__inner) {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  height: 40px;
}

.balance-info {
  margin-top: 10px;
  font-size: 14px;
  color: #666;
}

.balance-info .highlight {
  color: #f56c6c;
  font-weight: 600;
}

.balance-info .link {
  color: #409eff;
  margin-left: 15px;
  cursor: pointer;
}

.balance-info .link:hover {
  text-decoration: underline;
}

.account-input {
  width: 100%;
  max-width: 400px;
}

.account-input :deep(.el-input__inner) {
  height: 44px;
}

/* 提现页面新样式 */
.amount-input-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 10px;
  background: #fff;
}

.amount-input-box .amount-icon {
  font-size: 20px;
  color: #909399;
}

.amount-input-box-inner {
  flex: 1;
}

.amount-input-box-inner :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0;
}

.amount-input-box-inner :deep(.el-input__inner) {
  font-size: 16px;
  color: #333;
  text-align: left;
}

.account-select-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s;
}

.account-select-box:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.account-select-box.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.account-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.account-icon-wrapper .icon-text {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
}

.bank-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.alipay-icon {
  background: linear-gradient(135deg, #1677ff 0%, #40a9ff 100%);
}

.account-info {
  flex: 1;
}

.account-type {
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.account-number {
  font-size: 13px;
  color: #67c23a;
  margin-top: 4px;
}

.account-placeholder {
  font-size: 13px;
  color: #f56c6c;
  margin-top: 4px;
}

.check-icon {
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.service-fee {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 10px 0;
}

.fee-currency {
  font-size: 16px;
  color: #409eff;
  font-weight: 600;
}

.fee-amount {
  font-size: 24px;
  color: #409eff;
  font-weight: 600;
}

/* 求购金样式 */
.buyorder-info {
  margin-bottom: 20px;
}

.info-card {
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 10px;
}

.card-content p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.info-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  background: #f7f8fa;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-value.highlight {
  color: #67c23a;
}

/* 资金流水筛选 */
.transaction-filters {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

/* 表格样式 */
.transaction-table {
  margin-top: 20px;
}

.table-header {
  display: flex;
  background: #f7f8fa;
  padding: 12px 0;
  color: #999;
  font-size: 12px;
  font-weight: 600;
}

.table-header span {
  flex: 1;
  text-align: center;
}

.table-body {
  background: #fff;
}

.table-row {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #333;
}

.table-row:hover {
  background: #fafafa;
}

.table-row span {
  flex: 1;
  text-align: center;
}

.amount-positive {
  color: #67c23a;
  font-weight: 600;
}

.amount-negative {
  color: #f56c6c;
  font-weight: 600;
}

.empty-data {
  text-align: center;
  padding: 100px 0;
  color: #ccc;
}

.logo-text {
  font-size: 40px;
  font-weight: bold;
  color: #e0e0e0;
  margin-bottom: 10px;
}
</style>
