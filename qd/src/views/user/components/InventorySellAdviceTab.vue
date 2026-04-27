<template>
  <div class="sell-advice-tab">
    <section class="advice-panel" v-loading="loading">
      <div class="advice-head">
        <div class="advice-title">
          <h3>建议出售</h3>
          <p>按热度、流动性和波动风险排序，优先处理更容易成交的库存。</p>
        </div>

        <div class="head-actions">
          <el-button class="icon-button" :loading="loading" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
          <el-button type="primary" class="icon-button" @click="emit('open-inventory')">
            <el-icon><Sell /></el-icon>
            <span>去库存出售</span>
          </el-button>
        </div>
      </div>

      <div class="summary-grid">
        <article v-for="card in summaryCards" :key="card.label" class="summary-item" :class="card.tone">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <em>{{ card.hint }}</em>
        </article>
      </div>

      <el-empty
        v-if="!loading && !recommendations.length"
        description="暂无建议出售饰品"
      />

      <div v-else class="advice-list">
        <article
          v-for="(item, index) in recommendations"
          :key="item.inventoryId || item.itemId || item.name"
          class="advice-item"
        >
          <div class="rank-block" :class="getScoreTone(item.sellPriorityScore)">
            <span>{{ getRankLabel(index) }}</span>
            <strong>{{ safeScore(item.sellPriorityScore) }}</strong>
          </div>

          <img
            :src="getRecommendationIcon(item)"
            :alt="item.name || '饰品'"
            class="advice-image"
          />

          <div class="advice-item-copy">
            <div class="name-row">
              <strong>{{ item.name || '未知饰品' }}</strong>
              <span>{{ item.actionLabel || getScoreLabel(item.sellPriorityScore) }}</span>
            </div>

            <div class="reason-row">
              <em v-for="tag in getReasonTags(item)" :key="tag">{{ tag }}</em>
            </div>
          </div>

          <div class="price-stack">
            <span>建议卖价</span>
            <strong>¥ {{ formatPrice(item.suggestedSellPrice || item.referencePrice) }}</strong>
            <em>参考价 ¥ {{ formatPrice(item.referencePrice) }}</em>
          </div>

          <div class="row-actions">
            <el-button type="primary" link class="icon-button" @click="emit('open-inventory')">
              <el-icon><Sell /></el-icon>
              <span>出售</span>
            </el-button>
            <el-button link class="icon-button" :disabled="!item.itemId" @click="goToItem(item)">
              <el-icon><View /></el-icon>
              <span>行情</span>
            </el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Sell, View } from '@element-plus/icons-vue'
import { getInventoryAnalysis, syncInventory } from '@/api/inventory'
import { resolveItemImageUrl } from '@/utils/itemImage'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['open-inventory'])
const router = useRouter()

const loading = ref(false)
const hasLoaded = ref(false)
const analysis = ref(null)

const normalizeAnalysis = (payload) => payload?.data || payload || {}
const currentAnalysis = computed(() => normalizeAnalysis(analysis.value))

const recommendations = computed(() => {
  const list = Array.isArray(currentAnalysis.value?.recommendations) ? currentAnalysis.value.recommendations : []
  return [...list].sort((a, b) => safeScore(b?.sellPriorityScore) - safeScore(a?.sellPriorityScore))
})

const recommendedCount = computed(() => Number(currentAnalysis.value?.recommendedSellCount ?? recommendations.value.length) || 0)
const sellableCount = computed(() => Number(currentAnalysis.value?.sellableCount) || 0)
const restrictedCount = computed(() => Number(currentAnalysis.value?.restrictedCount) || 0)
const totalValue = computed(() => toNumber(currentAnalysis.value?.totalValue))

const summaryCards = computed(() => [
  {
    label: '建议数量',
    value: recommendedCount.value,
    hint: `${sellableCount.value} 件可出售`,
    tone: 'accent'
  },
  {
    label: '资产估值',
    value: `¥ ${formatPrice(totalValue.value, true)}`,
    hint: `${restrictedCount.value} 件受限`,
    tone: ''
  }
])

const toNumber = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

const safeScore = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? Math.max(0, Math.min(100, Math.round(numeric))) : 0
}

const formatPrice = (value, allowZero = false) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && (numeric > 0 || allowZero) ? numeric.toFixed(2) : '--'
}

const getRecommendationIcon = (item) => resolveItemImageUrl(item?.iconUrl)

const getReasonTags = (item) => {
  const tags = Array.isArray(item?.reasonTags) ? item.reasonTags.filter(Boolean) : []
  return tags.length ? tags.slice(0, 3) : ['行情稳定', '可关注卖出窗口']
}

const getRankLabel = (index) => (index < 3 ? `TOP ${index + 1}` : `#${index + 1}`)

const getScoreTone = (value) => {
  const score = safeScore(value)
  if (score >= 85) return 'hot'
  if (score >= 75) return 'good'
  return 'watch'
}

const getScoreLabel = (value) => {
  const tone = getScoreTone(value)
  if (tone === 'hot') return '适合优先出售'
  if (tone === 'good') return '可挂单试探'
  return '建议继续观察'
}

const goToItem = (item) => {
  if (!item?.itemId) return
  router.push(`/items/${item.itemId}`)
}

const fetchInventoryAnalysis = async (manageLoading = true) => {
  if (manageLoading) {
    loading.value = true
  }
  try {
    analysis.value = await getInventoryAnalysis()
    hasLoaded.value = true
  } catch (error) {
    analysis.value = null
    ElMessage.error(error?.message || '获取出售建议失败')
  } finally {
    if (manageLoading) {
      loading.value = false
    }
  }
}

const handleRefresh = async () => {
  loading.value = true
  try {
    await syncInventory()
    await fetchInventoryAnalysis(false)
    ElMessage.success(`已同步库存并刷新 ${recommendations.value.length} 条出售建议`)
  } catch (error) {
    ElMessage.error(error?.message || '刷新出售建议失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.active,
  (active) => {
    if (active && !hasLoaded.value) {
      fetchInventoryAnalysis()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.sell-advice-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.advice-panel {
  padding: 30px 32px 34px;
  border: 1px solid #e4e8f1;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.advice-head,
.name-row,
.row-actions {
  display: flex;
  align-items: center;
}

.advice-head {
  justify-content: space-between;
  gap: 24px;
}

.advice-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.2;
}

.advice-title p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.head-actions,
.reason-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.icon-button :deep(.el-icon) {
  margin-right: 5px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.summary-item {
  min-height: 96px;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.summary-item.accent {
  border-color: rgba(37, 99, 235, 0.22);
  background: #eff6ff;
}

.summary-item span,
.price-stack span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
}

.summary-item em {
  display: block;
  margin-top: 9px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.advice-list {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.advice-item {
  display: grid;
  grid-template-columns: 72px 104px minmax(0, 1fr) 174px 118px;
  align-items: center;
  gap: 18px;
  min-height: 118px;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.advice-item:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.rank-block {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 5px;
  width: 64px;
  height: 72px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
}

.rank-block.hot {
  background: #fff7ed;
  color: #c2410c;
}

.rank-block.good {
  background: #ecfdf5;
  color: #047857;
}

.rank-block span {
  font-size: 11px;
  font-weight: 800;
}

.rank-block strong {
  font-size: 26px;
  line-height: 1;
}

.advice-image {
  width: 104px;
  height: 72px;
  object-fit: contain;
  filter: drop-shadow(0 10px 16px rgba(15, 23, 42, 0.12));
}

.advice-item-copy {
  min-width: 0;
}

.name-row {
  justify-content: flex-start;
  gap: 10px;
}

.name-row strong {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-row span {
  flex: none;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.reason-row {
  margin-top: 10px;
}

.reason-row em {
  padding: 5px 9px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.price-stack {
  text-align: right;
}

.price-stack strong {
  display: block;
  margin-top: 7px;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.05;
}

.price-stack em {
  display: block;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.row-actions {
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: wrap;
}

@media (max-width: 1180px) {
  .advice-item {
    grid-template-columns: 72px 104px minmax(0, 1fr) 164px;
  }

  .row-actions {
    grid-column: 3 / -1;
  }
}

@media (max-width: 820px) {
  .advice-panel {
    padding: 22px 18px 24px;
  }

  .advice-head {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .advice-item {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 14px;
  }

  .advice-image {
    grid-column: 1;
    width: 84px;
  }

  .advice-item-copy,
  .price-stack,
  .row-actions {
    grid-column: 1 / -1;
  }

  .price-stack {
    text-align: left;
  }

  .row-actions {
    justify-content: flex-start;
  }

  .name-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .name-row strong {
    white-space: normal;
  }
}
</style>
