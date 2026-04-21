<template>
  <aside class="market-panel" v-loading="loading">
    <div class="panel-head">
      <div>
        <h3>{{ panelTitle }}</h3>
      </div>
      <span class="source-tag" :class="`source-tag-${trendSource}`">{{ trendSourceText }}</span>
    </div>
    <p v-if="marketNote" class="panel-note">{{ marketNote }}</p>

    <div v-if="activeView === 'trend'" class="range-switch">
      <button
        v-for="option in rangeOptions"
        :key="option.key"
        type="button"
        class="range-button"
        :class="{ active: activeRange === option.key }"
        @click="activeRange = option.key"
      >
        {{ option.label }}
      </button>
    </div>

    <div v-if="activeView === 'trend'" class="chart-panel">
      <div v-if="currentTrend.length" class="trend-summary">
        <span>{{ trendSummaryLabel }}</span>
        <strong>¥ {{ formatPrice(latestTrendPoint?.price) }}</strong>
      </div>
      <v-chart v-if="currentTrend.length" class="trend-chart" :option="chartOption" autoresize />
      <el-empty v-else description="暂无价格走势数据" />
    </div>

    <div v-if="activeView === 'pricing'" class="metrics-grid">
      <article class="metric-card metric-card-highlight">
        <span>建议卖价</span>
        <strong>¥ {{ formatPrice(suggestedSellPrice) }}</strong>
      </article>
      <article class="metric-card">
        <span>建议买价</span>
        <strong>¥ {{ formatPrice(suggestedBuyPrice) }}</strong>
      </article>
      <article class="metric-card">
        <span>近7日均价</span>
        <strong>¥ {{ formatPrice(panelData?.avgTradePrice7d) }}</strong>
      </article>
      <article class="metric-card">
        <span>当前售价</span>
        <strong>¥ {{ formatPrice(currentReferencePrice) }}</strong>
      </article>
      <article class="metric-card">
        <span>最低在售</span>
        <strong>¥ {{ formatPrice(lowestSellPrice) }}</strong>
      </article>
      <article class="metric-card">
        <span>最高求购</span>
        <strong>¥ {{ formatPrice(highestBuyPriceValue) }}</strong>
      </article>
    </div>

    <p v-if="activeView === 'pricing' && referencePriceSourceText" class="basis-note">
      当前参考价来源：{{ referencePriceSourceText }}
    </p>

    <p v-if="activeView === 'pricing' && pricingBasis" class="basis-note">
      {{ pricingBasis }}
    </p>

    <div v-if="activeView === 'pricing'" class="change-row">
      <div class="change-pill" :class="getChangeClass(panelData?.priceChange7d)">
        7日涨跌 {{ formatPercent(panelData?.priceChange7d) }}
      </div>
      <div class="change-pill" :class="getChangeClass(panelData?.priceChange30d)">
        30日涨跌 {{ formatPercent(panelData?.priceChange30d) }}
      </div>
    </div>

    <div v-if="activeView === 'pricing'" class="score-grid">
      <div class="score-item">
        <span>热度</span>
        <strong>{{ safeScore(panelData?.heatScore) }}</strong>
      </div>
      <div class="score-item">
        <span>流动性</span>
        <strong>{{ safeScore(panelData?.liquidityScore) }}</strong>
      </div>
      <div class="score-item">
        <span>波动性</span>
        <strong>{{ safeScore(panelData?.volatilityScore) }}</strong>
      </div>
    </div>

    <section v-if="activeView === 'trades'" class="trade-section">
      <div class="trade-section-head">
        <h4>最近成交</h4>
        <span>{{ recentTrades.length }} 条</span>
      </div>

      <div v-if="!recentTrades.length && !loading" class="trade-empty">
        <el-empty description="当前饰品暂无完成成交" />
      </div>

      <div v-else class="trade-list">
        <div class="trade-table-head">
          <span>价格</span>
          <span>来源</span>
          <span>时间</span>
        </div>
        <div v-for="trade in recentTrades" :key="`${trade.completedAt}-${trade.price}`" class="trade-row">
          <strong>¥ {{ formatPrice(trade.price) }}</strong>
          <em>本站成交</em>
          <span>{{ formatDate(trade.completedAt) }}</span>
        </div>
      </div>
    </section>
  </aside>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { getItemMarketPanel } from '@/api/item'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const props = defineProps({
  itemId: {
    type: [Number, String],
    default: null
  },
  activeView: {
    type: String,
    default: 'trend'
  },
  fallbackPrice: {
    type: [Number, String],
    default: 0
  },
  highestBuyPrice: {
    type: [Number, String],
    default: 0
  }
})

const rangeOptions = [
  { key: 'sevenDays', label: '7天' },
  { key: 'thirtyDays', label: '30天' },
  { key: 'all', label: '全部' }
]

const loading = ref(false)
const panelData = ref(null)
const activeRange = ref('sevenDays')

const toPositiveNumber = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : 0
}

const buildFallbackTrend = (days) => {
  const price = toPositiveNumber(currentReferencePrice.value)
  if (!price) return []

  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - days + 1)

  return Array.from({ length: days }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return {
      recordedAt: `${date.getFullYear()}-${month}-${day} 00:00`,
      label: `${month}-${day}`,
      price,
      volume: 0
    }
  })
}

const hasBackendTrend = computed(() => {
  const sourceTrend = panelData.value?.priceTrend || {}
  return Object.values(sourceTrend).some((points) => Array.isArray(points) && points.length)
})
const currentReferencePrice = computed(() => toPositiveNumber(
  panelData.value?.referencePrice ||
  props.fallbackPrice ||
  panelData.value?.latestPrice ||
  panelData.value?.lowestSellPrice ||
  panelData.value?.suggestedSellPrice ||
  panelData.value?.highestBuyPrice
))
const referencePriceSource = computed(() => panelData.value?.referencePriceSource || '')
const referencePriceSourceText = computed(() => {
  if (referencePriceSource.value === 'steam') return 'Steam参考价'
  if (referencePriceSource.value === 'buff') return 'Buff回退'
  if (referencePriceSource.value === 'local') return '站内参考'
  return ''
})
const lowestSellPrice = computed(() => toPositiveNumber(panelData.value?.lowestSellPrice || props.fallbackPrice))
const highestBuyPriceValue = computed(() => toPositiveNumber(panelData.value?.highestBuyPrice || props.highestBuyPrice))
const suggestedSellPrice = computed(() => toPositiveNumber(panelData.value?.suggestedSellPrice || currentReferencePrice.value))
const suggestedBuyPrice = computed(() => toPositiveNumber(panelData.value?.suggestedBuyPrice || suggestedSellPrice.value * 0.95))
const trendMap = computed(() => {
  const sourceTrend = panelData.value?.priceTrend || {}
  if (hasBackendTrend.value || !currentReferencePrice.value) return sourceTrend

  return {
    sevenDays: buildFallbackTrend(7),
    thirtyDays: buildFallbackTrend(30),
    all: buildFallbackTrend(30)
  }
})
const currentTrend = computed(() => trendMap.value?.[activeRange.value] || [])
const recentTrades = computed(() => {
  const trades = panelData.value?.recentTrades
  if (!Array.isArray(trades)) return []
  return trades.filter((trade) => trade?.completedAt && trade.source !== 'steam')
})
const trendSource = computed(() => {
  if (!hasBackendTrend.value && currentReferencePrice.value) return 'reference'
  return panelData.value?.trendSource || 'local_trade'
})
const latestTrendPoint = computed(() => {
  const points = currentTrend.value
  return points.length ? points[points.length - 1] : null
})
const panelTitle = computed(() => {
  if (props.activeView === 'trades') return '最近成交记录'
  if (props.activeView === 'pricing') return '定价建议'
  return '价格走势'
})
const trendSourceText = computed(() => {
  if (trendSource.value === 'steam') return 'Steam 走势'
  if (trendSource.value === 'reference') return '参考价线'
  return '本站回退'
})
const trendSummaryLabel = computed(() => {
  if (trendSource.value === 'steam') return '最新 Steam 走势价'
  if (trendSource.value === 'reference') return '当前参考价保底线'
  return '最新本站成交均价'
})
const marketNote = computed(() => {
  if (props.activeView === 'trades') {
    return '最近成交仅统计本站已完成订单。'
  }
  if (props.activeView === 'pricing') {
    return ''
  }
  if (panelData.value?.dataNote) return stripRecentTradeNote(panelData.value.dataNote)
  if (trendSource.value === 'reference') {
    return '暂无可用 Steam 历史和本站成交趋势，已按当前售价绘制参考价线。'
  }
  return ''
})
const pricingBasis = computed(() => {
  if (panelData.value?.pricingBasis) return panelData.value.pricingBasis
  if (trendSource.value === 'reference') {
    return '当前暂无完成成交记录，建议卖价先参考当前售价，建议买价按 95% 估算。'
  }
  return ''
})

const chartOption = computed(() => ({
  grid: {
    left: 18,
    right: 18,
    top: 22,
    bottom: 20,
    containLabel: true
  },
  tooltip: {
    trigger: 'axis',
    formatter: (params) => {
      const point = params?.[0]
      if (!point) return ''
      return `${point.axisValue}<br/>价格: ¥ ${formatPrice(point.data)}`
    }
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    axisLabel: {
      color: '#64748b'
    },
    data: currentTrend.value.map((point) => point.label)
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#64748b',
      formatter: (value) => `¥ ${Number(value || 0).toFixed(2)}`
    },
    splitLine: {
      lineStyle: {
        color: '#e5e7eb'
      }
    }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: {
        width: 3,
        color: '#f59e0b'
      },
      itemStyle: {
        color: '#2563eb'
      },
      areaStyle: {
        color: 'rgba(37, 99, 235, 0.12)'
      },
      data: currentTrend.value.map((point) => Number(point.price || 0))
    }
  ]
}))

const formatPrice = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric.toFixed(2) : '--'
}

const formatPercent = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '--'
  return `${numeric > 0 ? '+' : ''}${numeric.toFixed(2)}%`
}

const formatDate = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const safeScore = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

const getChangeClass = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric === 0) return 'neutral'
  return numeric > 0 ? 'positive' : 'negative'
}

const stripRecentTradeNote = (note) => String(note || '')
  .replace(/最近成交仅统计本站已完成订单[^。]*。/g, '')
  .trim()

const syncActiveRange = () => {
  if (currentTrend.value.length) return
  const fallback = rangeOptions.find((option) => (trendMap.value?.[option.key] || []).length)
  if (fallback) {
    activeRange.value = fallback.key
  }
}

const fetchPanel = async () => {
  if (!props.itemId) {
    panelData.value = null
    return
  }

  loading.value = true
  try {
    panelData.value = await getItemMarketPanel(props.itemId)
    syncActiveRange()
  } catch (error) {
    panelData.value = null
    ElMessage.error(error?.message || '获取行情面板失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.itemId, fetchPanel, { immediate: true })
</script>

<style scoped>
.market-panel {
  min-height: 420px;
  padding: 34px 56px 50px;
  background: #ffffff;
}

.panel-head,
.trade-section-head,
.change-row,
.score-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-head h3,
.trade-section-head h4 {
  margin: 0;
  color: #172033;
}

.panel-head h3 {
  font-size: 24px;
  line-height: 1.25;
}

.panel-note,
.basis-note,
.trade-empty p {
  margin: 12px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.source-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.source-tag-steam {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
}

.source-tag-local_trade {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.source-tag-reference {
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
}

.range-switch {
  display: inline-flex;
  gap: 8px;
  margin-top: 22px;
}

.range-button {
  border: 1px solid #d6deea;
  border-radius: 999px;
  background: #ffffff;
  color: #64748b;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.range-button.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}

.chart-panel {
  margin-top: 20px;
  padding: 24px 30px 18px;
  min-height: 430px;
  border: 1px solid #eef2f7;
  border-radius: 2px;
  background: #ffffff;
}

.trend-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 8px;
}

.trend-summary span {
  color: #64748b;
  font-size: 13px;
}

.trend-summary strong {
  color: #0f172a;
  font-size: 18px;
}

.trend-chart {
  width: 100%;
  height: 380px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.metric-card {
  min-height: 98px;
  padding: 20px 22px;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  background: #ffffff;
}

.metric-card-highlight {
  border-color: rgba(245, 158, 11, 0.35);
  background: linear-gradient(180deg, rgba(245, 158, 11, 0.08), rgba(255, 255, 255, 1));
}

.metric-card span,
.score-item span,
.trade-row span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.metric-card strong,
.score-item strong,
.trade-row strong {
  color: #172033;
  font-size: 20px;
}

.trade-row strong {
  color: #f59e0b;
}

.change-row {
  margin-top: 14px;
}

.change-pill {
  flex: 1;
  padding: 10px 12px;
  border-radius: 2px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.change-pill.positive {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.change-pill.negative {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.change-pill.neutral {
  background: #f1f5f9;
  color: #475569;
}

.score-grid {
  margin-top: 14px;
}

.score-item {
  flex: 1;
  padding: 14px 16px;
  border-radius: 2px;
  background: #111827;
  color: #ffffff;
}

.score-item strong {
  color: #ffffff;
}

.trade-section {
  margin-top: 24px;
  padding: 0;
  border: 1px solid #eef2f7;
  border-radius: 2px;
  background: #ffffff;
}

.trade-section-head {
  min-height: 54px;
  padding: 0 22px;
  background: #f8fafc;
  border-bottom: 1px solid #eef2f7;
}

.trade-section-head span,
.trade-row em {
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.trade-list {
  margin-top: 0;
}

.trade-table-head,
.trade-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.8fr) minmax(120px, 0.7fr) minmax(160px, 1fr);
  align-items: center;
  gap: 18px;
}

.trade-table-head {
  min-height: 46px;
  padding: 0 22px;
  border-bottom: 1px solid #eef2f7;
  color: #94a3b8;
  font-size: 13px;
}

.trade-empty {
  padding-bottom: 8px;
}

.trade-empty p {
  padding: 0 18px;
  text-align: center;
}

.trade-row {
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
}

.trade-row:last-child {
  border-bottom: 0;
}

@media (max-width: 768px) {
  .market-panel {
    padding: 16px;
  }

  .panel-head,
  .trade-section-head,
  .change-row,
  .score-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .trend-chart {
    height: 260px;
  }

  .trade-table-head {
    display: none;
  }

  .trade-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>
