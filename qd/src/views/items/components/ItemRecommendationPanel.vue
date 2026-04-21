<template>
  <section class="recommend-panel" v-loading="loading">
    <div class="panel-head">
      <div>
        <h3>推荐</h3>
      </div>
      <span class="panel-tag">{{ panelTag }}</span>
    </div>

    <el-empty v-if="!loading && !visibleRecommendations.length" :description="emptyDescription">
      <el-button type="primary" plain @click="fetchRecommendations">重新加载</el-button>
    </el-empty>

    <div v-else class="recommend-grid">
      <button
        v-for="item in visibleRecommendations"
        :key="item.itemId"
        type="button"
        class="recommend-card"
        @click="goToItem(item)"
      >
        <div class="card-image">
          <span class="card-badge">{{ item.recommendReason || '推荐' }}</span>
          <img :src="getRecommendationIcon(item)" :alt="getRecommendationName(item)" />
        </div>

        <div class="card-copy">
          <strong>{{ getRecommendationName(item) }}</strong>
          <span>{{ getCategoryText(item) }}</span>
        </div>

        <div class="card-footer">
          <em>{{ item.recommendReason || '根据当前偏好与热度推荐' }}</em>
          <b>¥ {{ formatPrice(item.referencePrice) }}</b>
        </div>
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRecommendations } from '@/api/item'
import { resolveItemImageUrl } from '@/utils/itemImage'

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

const loading = ref(false)
const hasLoaded = ref(false)
const loadError = ref('')
const recommendations = ref([])

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

const getRecommendationName = (item) => item?.nameCn || item?.name || '推荐饰品'
const getRecommendationIcon = (item) => resolveItemImageUrl(item?.iconUrl)
const getCategoryText = (item) => item?.category || '饰品推荐'

const fetchRecommendations = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const payload = await getRecommendations(12)
    recommendations.value = normalizeRecommendations(payload)
    hasLoaded.value = true
  } catch (error) {
    recommendations.value = []
    loadError.value = '推荐加载失败，请稍后重试'
    ElMessage.error(error?.message || '获取推荐饰品失败')
  } finally {
    loading.value = false
  }
}

const goToItem = (item) => {
  if (!item?.itemId) return
  router.push(`/items/${item.itemId}`)
}

watch(() => props.currentItemId, fetchRecommendations, { immediate: true })
</script>

<style scoped>
.recommend-panel {
  min-height: 420px;
  padding: 34px 40px 40px;
  background: #ffffff;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h3 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.25;
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
  gap: 18px;
  margin-top: 24px;
}

.recommend-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  overflow: hidden;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.recommend-card:hover {
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 192px;
  padding: 24px 18px 18px;
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.14), transparent 30%),
    radial-gradient(circle at bottom right, rgba(37, 99, 235, 0.12), transparent 34%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

.card-image img {
  max-width: 100%;
  max-height: 118px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(15, 23, 42, 0.12));
}

.card-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  max-width: calc(100% - 32px);
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.84);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-copy {
  padding: 18px 18px 10px;
}

.card-copy strong {
  display: -webkit-box;
  overflow: hidden;
  color: #172033;
  font-size: 18px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-copy span,
.card-footer em {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.card-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding: 0 18px 18px;
}

.card-footer em {
  display: -webkit-box;
  flex: 1;
  overflow: hidden;
  font-style: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-footer b {
  flex-shrink: 0;
  color: #f59e0b;
  font-size: 24px;
  line-height: 1;
}

@media (max-width: 1200px) {
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .recommend-panel {
    padding: 16px;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .card-image {
    min-height: 164px;
  }

  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
