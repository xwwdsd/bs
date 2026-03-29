<template>
  <div class="detail-page">
    <nav class="navbar-dark">
      <div class="nav-links-left">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/items" class="nav-link active">市场</router-link>
        <router-link to="/news" class="nav-link">资讯</router-link>
        <router-link to="/player-shows" class="nav-link">玩家秀</router-link>
      </div>
      <div v-if="userStore.isLoggedIn" class="nav-user-section">
        <div class="user-nav-links">
          <router-link to="/user/inventory" class="nav-link">我的库存</router-link>
          <router-link to="/user/sell-orders" class="nav-link">我的出售</router-link>
          <router-link to="/user/buy-orders" class="nav-link">我的求购</router-link>
        </div>
        <UserNavDropdown />
      </div>
      <div v-else class="nav-user">
        <span class="nav-link" @click="showLoginModal = true">登录/注册</span>
      </div>
    </nav>

    <LoginModal v-model="showLoginModal" />

    <div class="detail-shell" v-loading="loading">
      <template v-if="item">
        <section class="detail-hero">
          <div class="media-panel">
            <img :src="item.iconUrl || '/default-item.png'" :alt="item.name" />
          </div>
          <div class="info-panel">
            <p class="eyebrow">{{ getCategoryText(item.category) }}</p>
            <h1>{{ item.nameCn || item.name }}</h1>
            <p class="meta-line">外观：{{ getExteriorText(item.exterior) || '未知' }}</p>
            <p class="meta-line">品质：{{ getQualityText(item.quality) || '未知' }}</p>
            <p class="price-line">参考价格：￥{{ formatPrice(item.buffPrice) }}</p>

            <div class="hero-actions">
              <el-button type="primary" @click="scrollToSellOrders">查看卖家</el-button>
              <el-button v-if="userStore.isLoggedIn" @click="showSellDialog">我要出售</el-button>
            </div>
          </div>
        </section>

        <section id="sell-orders" class="sell-section" v-loading="sellOrdersLoading">
          <div class="section-head">
            <h2>在售卖家</h2>
            <span>{{ sellOrders.length }} 条在售记录</span>
          </div>

          <el-empty v-if="!sellOrders.length" description="暂无在售记录" />

          <div v-else class="sell-list">
            <article v-for="order in sellOrders" :key="order.id" class="sell-card">
              <div>
                <h3>{{ order.seller?.username || '未知卖家' }}</h3>
                <p>价格：￥{{ formatPrice(order.price) }}</p>
                <p>磨损：{{ order.paintWear ? order.paintWear.toFixed(6) : '-' }}</p>
                <p>上架时间：{{ formatDate(order.createdAt) }}</p>
              </div>
              <el-button
                type="primary"
                :disabled="order.userId === userStore.userInfo?.id"
                @click="handleBuy(order)"
              >
                购买
              </el-button>
            </article>
          </div>
        </section>
      </template>

      <el-empty v-else-if="!loading" description="饰品不存在" />
    </div>

    <el-dialog v-model="sellDialogVisible" title="创建出售订单" width="480px">
      <div v-if="userInventory.length">
        <el-form :model="sellForm" label-position="top">
          <el-form-item label="选择库存饰品">
            <el-select v-model="sellForm.inventoryId" placeholder="请选择库存饰品" style="width: 100%">
              <el-option
                v-for="inventory in userInventory"
                :key="inventory.id"
                :label="inventory.name"
                :value="inventory.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="出售价格">
            <el-input-number v-model="sellForm.price" :min="0.01" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-else description="你当前没有可出售的同类饰品" />

      <template #footer>
        <el-button @click="sellDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="selling" :disabled="!userInventory.length" @click="confirmSell">
          确认出售
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getItemById } from '@/api/item'
import { getSellOrdersByItemId, createSellOrder } from '@/api/sellOrder'
import { getUserInventory } from '@/api/inventory'
import { useUserStore } from '@/stores/user'
import LoginModal from '@/components/LoginModal.vue'
import UserNavDropdown from '@/components/UserNavDropdown.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showLoginModal = ref(false)
const loading = ref(false)
const sellOrdersLoading = ref(false)
const item = ref(null)
const sellOrders = ref([])
const sellDialogVisible = ref(false)
const selling = ref(false)
const userInventory = ref([])

const sellForm = ref({
  inventoryId: null,
  price: 0
})

const fetchItemDetail = async () => {
  const id = route.params.id
  if (!id) {
    router.push('/items')
    return
  }

  loading.value = true
  try {
    item.value = await getItemById(id)
    await fetchSellOrders(id)
  } catch (error) {
    ElMessage.error(error?.message || '获取饰品详情失败')
  } finally {
    loading.value = false
  }
}

const fetchSellOrders = async (itemId) => {
  sellOrdersLoading.value = true
  try {
    sellOrders.value = await getSellOrdersByItemId(itemId) || []
  } catch (error) {
    ElMessage.error(error?.message || '获取在售列表失败')
  } finally {
    sellOrdersLoading.value = false
  }
}

const scrollToSellOrders = () => {
  document.getElementById('sell-orders')?.scrollIntoView({ behavior: 'smooth' })
}

const handleBuy = async (sellOrder) => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }

  if (sellOrder.userId === userStore.userInfo?.id) {
    ElMessage.warning('不能购买自己的商品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认以 ￥${formatPrice(sellOrder.price)} 购买该饰品吗？`,
      '确认购买',
      {
        confirmButtonText: '确认购买',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request.post('/v1/order/create', {
      sellOrderId: sellOrder.id
    })

    ElMessage.success('订单创建成功，请前往订单页面支付')
    router.push('/user/orders')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || error?.message || '创建订单失败')
    }
  }
}

const showSellDialog = async () => {
  try {
    const res = await getUserInventory()
    userInventory.value = (res || []).filter((inventory) => inventory.itemId === item.value?.id && inventory.status === 0)
    sellForm.value = {
      inventoryId: null,
      price: Number(item.value?.buffPrice || 0)
    }
    sellDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '获取库存失败')
  }
}

const confirmSell = async () => {
  if (!sellForm.value.inventoryId) {
    ElMessage.warning('请选择要出售的饰品')
    return
  }

  if (!sellForm.value.price || sellForm.value.price <= 0) {
    ElMessage.warning('请输入有效的价格')
    return
  }

  selling.value = true
  try {
    await createSellOrder({
      inventoryId: sellForm.value.inventoryId,
      price: sellForm.value.price
    })
    ElMessage.success('出售订单创建成功')
    sellDialogVisible.value = false
    fetchSellOrders(item.value?.id)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '创建出售订单失败')
  } finally {
    selling.value = false
  }
}

const getQualityText = (quality) => {
  const map = {
    contraband: '违禁',
    covert: '隐秘',
    classified: '保密',
    restricted: '受限',
    'mil-spec': '军规级',
    industrial: '工业级',
    consumer: '消费级'
  }
  return map[quality] || quality
}

const getExteriorText = (exterior) => {
  const map = {
    FN: '崭新出厂',
    MW: '略有磨损',
    FT: '久经沙场',
    WW: '破损不堪',
    BS: '战痕累累',
    NoPaint: '无涂装'
  }
  return map[exterior] || exterior
}

const getCategoryText = (category) => {
  const map = {
    rifle: '步枪',
    pistol: '手枪',
    smg: '冲锋枪',
    shotgun: '霰弹枪',
    sniper_rifle: '狙击步枪',
    machinegun: '机枪',
    knife: '刀',
    glove: '手套',
    sticker: '贴纸',
    case: '武器箱',
    other: '其他'
  }
  return map[category] || category || '未分类'
}

const formatPrice = (price) => {
  return Number(price || 0).toFixed(2)
}

const formatDate = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchItemDetail()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #101722 0%, #131d2d 100%);
  color: #e9eef7;
}

.navbar-dark {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: rgba(13, 20, 31, 0.92);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
}

.nav-links-left,
.user-nav-links,
.nav-user-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  color: rgba(233, 238, 247, 0.76);
  text-decoration: none;
  cursor: pointer;
}

.nav-link.active,
.nav-link:hover {
  color: #ffffff;
}

.detail-shell {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.detail-hero {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.media-panel,
.info-panel,
.sell-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
}

.media-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  padding: 24px;
}

.media-panel img {
  width: 100%;
  max-width: 280px;
  object-fit: contain;
}

.info-panel {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #ffcf6e;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
}

.info-panel h1 {
  margin: 0 0 16px;
  font-size: 34px;
}

.meta-line {
  margin: 0 0 10px;
  color: rgba(233, 238, 247, 0.72);
}

.price-line {
  margin: 22px 0;
  color: #ffcf6e;
  font-size: 26px;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sell-list {
  display: grid;
  gap: 16px;
}

.sell-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.sell-card h3 {
  margin: 0 0 12px;
}

.sell-card p {
  margin: 6px 0;
  color: rgba(233, 238, 247, 0.72);
}

@media (max-width: 900px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .sell-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
