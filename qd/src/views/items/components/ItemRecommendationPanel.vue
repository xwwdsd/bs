<template>
  <section class="recommend-panel" v-loading="loading">
    <div class="panel-head">
      <div>
        <h3>推荐</h3>
        <p>优先展示同类、同价位或近期热度更高的饰品。</p>
      </div>
      <div class="head-actions">
        <span class="panel-tag">{{ panelTag }}</span>
        <el-button link class="icon-button" :loading="loading" @click="fetchRecommendations">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !visibleRecommendations.length" :description="emptyDescription">
      <el-button type="primary" plain class="icon-button" @click="fetchRecommendations">
        <el-icon><Refresh /></el-icon>
        <span>重新加载</span>
      </el-button>
    </el-empty>

    <div v-else class="recommend-grid">
      <article
        v-for="item in visibleRecommendations"
        :key="item.itemId"
        class="recommend-card"
        :class="getScoreTone(item)"
        @click="goToItem(item)"
      >
        <div class="card-image">
          <img :src="getRecommendationIcon(item)" :alt="getRecommendationName(item)" />
        </div>

        <div class="card-body">
          <div class="card-copy">
            <strong>{{ getRecommendationName(item) }}</strong>
            <span>{{ getCategoryText(item) }}</span>
          </div>

          <div class="score-row">
            <span>
              <el-icon><TrendCharts /></el-icon>
              推荐 {{ getRecommendScore(item) }}
            </span>
            <span>热度 {{ getHeatScore(item) }}</span>
          </div>
        </div>

        <div class="card-footer">
          <div>
            <span>参考价</span>
            <b>¥ {{ formatPrice(item.referencePrice) }}</b>
          </div>
          <button type="button" class="detail-action" @click.stop="goToItem(item)">
            <span>查看</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { getRecommendations } from '@/api/item'
import { useUserStore } from '@/stores/user'
import { resolveItemImageUrl } from '@/utils/itemImage'
import { getItemDisplayModel } from '@/utils/itemDisplay'

const props = defineProps({
  currentItemId: {
    type: [Number, String],
    default: null
  },
  limit: {
    type: Number,
    default: 6
  }
})

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const hasLoaded = ref(false)
const loadError = ref('')
const recommendations = ref([])
let recommendationRequestId = 0

const normalizeRecommendations = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.list)) return payload.list
  if (Array.isArray(payload?.data)) return payload.data
  return []
}

const visibleRecommendations = computed(() => {
  const currentItemId = Number(props.currentItemId)
  const filtered = recommendations.value.filter((item) => {
    if (!props.currentItemId || !Number.isFinite(currentItemId)) return true
    return Number(item?.itemId) !== currentItemId
  })

  return filtered.slice(0, props.limit)
})

const panelTag = computed(() => {
  if (loading.value) return '推荐中'
  return visibleRecommendations.value.length ? `${visibleRecommendations.value.length} 个推荐` : '暂无推荐'
})

const emptyDescription = computed(() => (
  loadError.value || (hasLoaded.value ? '暂无推荐饰品' : '点击重新加载推荐饰品')
))

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric.toFixed(2) : '--'
}

const getScore = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? Math.max(0, Math.min(100, Math.round(numeric))) : 0
}

const getRecommendationModel = (item) => getItemDisplayModel(item || {})
const getRecommendationName = (item) => item?.nameCn || item?.name || '推荐饰品'
const getRecommendationIcon = (item) => resolveItemImageUrl(item?.iconUrl)
const getRecommendScore = (item) => getScore(item?.recommendScore || item?.heatScore)
const getHeatScore = (item) => getScore(item?.heatScore)
const getScoreTone = (item) => {
  const score = getRecommendScore(item)
  if (score >= 80) return 'is-strong'
  if (score >= 60) return 'is-good'
  return ''
}
const getCategoryText = (item) => {
  const model = getRecommendationModel(item)
  const parts = [model.categoryLabel]

  if (model.qualityText && model.qualityText !== model.categoryLabel) {
    parts.push(model.qualityText)
  }

  if (model.showWearModule && model.exteriorText && !parts.includes(model.exteriorText)) {
    parts.push(model.exteriorText)
  }

  return parts.filter(Boolean).join(' · ') || '饰品推荐'
}

const currentRecommendationViewer = computed(() => (
  userStore.userInfo?.userId || userStore.userInfo?.id || 'guest'
))

const fetchRecommendations = async () => {
  const requestId = ++recommendationRequestId
  loading.value = true
  loadError.value = ''
  try {
    const payload = await getRecommendations(12, props.currentItemId)
    if (requestId !== recommendationRequestId) return
    recommendations.value = normalizeRecommendations(payload)
    hasLoaded.value = true
  } catch (error) {
    if (requestId !== recommendationRequestId) return
    recommendations.value = []
    loadError.value = '推荐加载失败，请稍后重试'
    ElMessage.error(error?.message || '获取推荐饰品失败')
  } finally {
    if (requestId === recommendationRequestId) {
      loading.value = false
    }
  }
}

const goToItem = (item) => {
  if (!item?.itemId) return
  router.push(`/items/${item.itemId}`)
}

watch([() => props.currentItemId, currentRecommendationViewer], fetchRecommendations, { immediate: true })
</script>

<style scoped>
.recommend-panel {
  min-height: 420px;
  padding: 34px 40px 40px;
  background: #ffffff;
}

.panel-head,
.head-actions,
.card-footer,
.score-row,
.detail-action {
  display: flex;
  align-items: center;
}

.panel-head {
  justify-content: space-between;
  gap: 18px;
}

.panel-head h3 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.25;
}

.panel-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.head-actions {
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.icon-button :deep(.el-icon) {
  margin-right: 5px;
}

.panel-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.recommend-card {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.recommend-card:hover {
  border-color: rgba(37, 99, 235, 0.26);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.recommend-card.is-strong {
  border-color: rgba(245, 158, 11, 0.32);
}

.recommend-card.is-good {
  border-color: rgba(37, 99, 235, 0.22);
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 152px;
  padding: 20px 18px 16px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.04), transparent 42%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

.card-image img {
  max-width: 100%;
  max-height: 102px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(15, 23, 42, 0.14));
}

.card-body {
  padding: 16px 18px 12px;
}

.card-copy strong {
  display: -webkit-box;
  overflow: hidden;
  color: #172033;
  font-size: 17px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-copy span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.score-row {
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
}

.score-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-footer {
  justify-content: space-between;
  gap: 14px;
  min-height: 68px;
  padding: 0 18px 18px;
}

.card-footer span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.card-footer b {
  display: block;
  margin-top: 4px;
  color: #f59e0b;
  font-size: 24px;
  line-height: 1;
}

.detail-action {
  justify-content: center;
  gap: 4px;
  min-width: 74px;
  height: 34px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

.detail-action:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

@media (max-width: 1200px) {
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .recommend-panel {
    padding: 18px;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .card-image {
    min-height: 142px;
  }
}
</style>
