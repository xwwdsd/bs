<template>
  <div class="sell-advice-tab">
    <section class="advice-card" v-loading="loading">
      <div class="advice-head">
        <div class="advice-copy">
          <h3>建议出售</h3>
          <p>优先展示当前热度高、流动性好的库存饰品</p>
        </div>
        <div class="advice-side">
          <el-button class="refresh-button" :loading="loading" @click="handleRefresh">刷新</el-button>
          <div class="advice-meta">
            <span>建议数量</span>
            <strong>{{ recommendedCount }}</strong>
          </div>
        </div>
      </div>

      <el-empty
        v-if="!loading && !recommendations.length"
        description="暂无建议出售饰品"
      />

      <div v-else class="advice-list">
        <article
          v-for="item in recommendations"
          :key="item.inventoryId || item.itemId || item.name"
          class="advice-item"
        >
          <img
            :src="getRecommendationIcon(item)"
            :alt="item.name || '饰品'"
            class="advice-image"
          />

          <div class="advice-item-copy">
            <strong>{{ item.name || '未知饰品' }}</strong>
            <span>{{ getPriorityText(item) }}</span>
            <p>{{ getReasonText(item) }}</p>
          </div>

          <div class="advice-price">
            <span>建议卖价</span>
            <strong>¥ {{ formatPrice(item.suggestedSellPrice || item.referencePrice) }}</strong>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventoryAnalysis, syncInventory } from '@/api/inventory'
import { resolveItemImageUrl } from '@/utils/itemImage'

const props = defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const loading = ref(false)
const hasLoaded = ref(false)
const analysis = ref(null)

const normalizeAnalysis = (payload) => payload?.data || payload || {}

const recommendations = computed(() => {
  const current = normalizeAnalysis(analysis.value)
  return Array.isArray(current?.recommendations) ? current.recommendations : []
})

const recommendedCount = computed(() => {
  return recommendations.value.length
})

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric >= 0 ? numeric.toFixed(2) : '--'
}

const getRecommendationIcon = (item) => resolveItemImageUrl(item?.iconUrl)

const getPriorityText = (item) => {
  const score = Number(item?.sellPriorityScore)
  const actionLabel = item?.actionLabel || '建议持有观察'
  return `优先级 ${Number.isFinite(score) ? score : 0} · ${actionLabel}`
}

const getReasonText = (item) => {
  const reasonTags = Array.isArray(item?.reasonTags) ? item.reasonTags.filter(Boolean) : []
  if (reasonTags.length) {
    return reasonTags.join('，')
  }
  return '行情稳定，可关注卖出窗口'
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

.advice-card {
  padding: 28px 32px 30px;
  border: 1px solid #e4e8f1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.advice-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.advice-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
}

.advice-copy h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
}

.advice-copy p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.advice-meta {
  text-align: right;
}

.refresh-button {
  min-width: 96px;
}

.advice-meta span,
.advice-price span {
  display: block;
  color: #94a3b8;
  font-size: 14px;
}

.advice-meta strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 32px;
  line-height: 1;
}

.advice-list {
  display: grid;
  gap: 18px;
}

.advice-item {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  padding: 24px 28px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.advice-item:hover {
  transform: translateY(-1px);
  border-color: #cbd5e1;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.advice-image {
  width: 112px;
  height: 72px;
  object-fit: contain;
}

.advice-item-copy {
  min-width: 0;
}

.advice-item-copy strong {
  display: block;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.advice-item-copy span,
.advice-item-copy p {
  display: block;
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.advice-price {
  min-width: 164px;
  text-align: right;
}

.advice-price strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
}

@media (max-width: 1024px) {
  .advice-card {
    padding: 22px 20px 24px;
  }

  .advice-head {
    flex-direction: column;
    align-items: stretch;
  }

  .advice-meta {
    text-align: left;
  }

  .advice-side {
    align-items: stretch;
  }

  .advice-item {
    grid-template-columns: 96px minmax(0, 1fr);
  }

  .advice-price {
    grid-column: 1 / -1;
    padding-top: 4px;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .advice-item {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .advice-image {
    width: 96px;
    height: 64px;
  }

  .advice-item-copy strong {
    font-size: 18px;
  }

  .advice-price strong {
    font-size: 26px;
  }
}
</style>
