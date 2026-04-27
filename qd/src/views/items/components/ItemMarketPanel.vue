<template>
  <aside class="market-panel" v-loading="loading">
    <div class="panel-head">
      <div>
        <h3>{{ panelTitle }}</h3>
        <p v-if="panelSubtitle">{{ panelSubtitle }}</p>
      </div>
      <span v-if="activeView !== 'trades'" class="source-tag" :class="`source-tag-${trendSource}`">{{ trendSourceText }}</span>
    </div>
    <p v-if="marketNote && activeView !== 'pricing'" class="panel-note">{{ marketNote }}</p>

    <template v-if="activeView === 'trend'">
      <div class="range-switch">
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

      <div class="chart-panel">
        <div v-if="currentTrend.length" class="trend-summary">
          <span>{{ trendSummaryLabel }}</span>
          <strong>{{ moneyText(latestTrendPoint?.price) }}</strong>
        </div>
        <v-chart v-if="currentTrend.length" class="trend-chart" :option="chartOption" autoresize />
        <el-empty v-else description="暂无价格走势数据" />
      </div>
    </template>

    <template v-if="activeView === 'pricing'">
      <div class="pricing-board">
        <section class="pricing-lead">
          <div class="lead-top">
            <span class="lead-icon">
              <el-icon><Money /></el-icon>
            </span>
            <span>{{ pricingConfidenceText }}</span>
          </div>
          <span class="lead-label">建议卖价</span>
          <strong>{{ moneyText(suggestedSellPrice) }}</strong>
          <p>{{ pricingActionText }}</p>
          <div class="lead-meta">
            <span>{{ currentReferenceLabel }} {{ moneyText(currentReferencePrice) }}</span>
            <span>{{ priceGapText }}</span>
          </div>
        </section>

        <section class="pricing-side">
          <article>
            <span>建议买价</span>
            <strong>{{ moneyText(suggestedBuyPrice) }}</strong>
            <em>{{ buyDiscountText }}</em>
          </article>
          <article>
            <span>最低在售</span>
            <strong>{{ moneyText(lowestSellPrice) }}</strong>
            <em>{{ lowestSellPrice ? '当前卖盘底价' : '暂无站内卖盘' }}</em>
          </article>
        </section>
      </div>

      <div class="metric-strip">
        <article
          v-for="metric in pricingMetrics"
          :key="metric.label"
          class="metric-item"
          :class="metric.tone"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <em>{{ metric.detail }}</em>
        </article>
      </div>

      <div class="score-grid">
        <article v-for="score in scoreMetrics" :key="score.label" class="score-item">
          <div>
            <span>{{ score.label }}</span>
            <strong>{{ score.value }}</strong>
          </div>
          <div class="score-track">
            <i :style="{ width: `${score.value}%` }"></i>
          </div>
          <em>{{ score.hint }}</em>
        </article>
      </div>
    </template>

    <section v-if="activeView === 'trades'" class="trade-section">
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
          <strong>{{ moneyText(trade.price) }}</strong>
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
import { Money } from '@element-plus/icons-vue'
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
const currentReferenceLabel = computed(() => {
  if (referencePriceSource.value === 'steam') return 'Steam售价'
  if (referencePriceSource.value === 'buff') return 'Buff售价'
  if (referencePriceSource.value === 'local') return '站内售价'
  return '当前售价'
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
const panelSubtitle = computed(() => {
  if (props.activeView === 'pricing') return '先看建议价，再用盘口和分数判断挂单策略。'
  if (props.activeView === 'trades') return ''
  return ''
})
const trendSourceText = computed(() => {
  if (trendSource.value === 'steam') return 'Steam 走势'
  if (trendSource.value === 'reference') return '参考价线'
  return '本站成交'
})
const trendSummaryLabel = computed(() => {
  if (trendSource.value === 'steam') return '最新 Steam 走势价'
  if (trendSource.value === 'reference') return '当前参考价保底线'
  return '最新本站成交均价'
})
const marketNote = computed(() => {
  if (props.activeView === 'trades') {
    return ''
  }
  if (props.activeView === 'pricing') {
    return ''
  }
  if (panelData.value?.dataNote) return stripRecentTradeNote(panelData.value.dataNote)
  if (trendSource.value === 'reference') {
    return `暂无可用 Steam 历史和本站成交趋势，已按${currentReferenceLabel.value}绘制参考价线。`
  }
  return ''
})
const pricingConfidenceText = computed(() => {
  if (recentTrades.value.length >= 3) return '成交样本较足'
  if (trendSource.value === 'steam') return 'Steam 数据参考'
  if (trendSource.value === 'reference') return '参考价估算'
  return '本站样本参考'
})
const pricingActionText = computed(() => {
  if (!suggestedSellPrice.value) return '缺少可用价格，建议先同步行情后再定价。'
  if (safeScore(panelData.value?.volatilityScore) >= 65) return '波动偏高，建议低于最低在售小幅挂单，避免价格回落。'
  if (highestBuyPriceValue.value && suggestedSellPrice.value) {
    const gap = (suggestedSellPrice.value - highestBuyPriceValue.value) / suggestedSellPrice.value
    if (gap >= 0 && gap <= 0.03) return '最高求购接近建议卖价，可优先考虑快速成交。'
  }
  if (safeScore(panelData.value?.heatScore) >= 60 && safeScore(panelData.value?.liquidityScore) >= 60) {
    return '热度和流动性较好，可按建议价挂单并观察买盘。'
  }
  return '行情相对平稳，建议按参考价挂单，后续根据浏览和求购变化调整。'
})
const buyDiscountText = computed(() => {
  if (!suggestedSellPrice.value || !suggestedBuyPrice.value) return '暂无买入估算'
  const ratio = (suggestedBuyPrice.value / suggestedSellPrice.value) * 100
  return `约为卖价 ${ratio.toFixed(0)}%`
})
const priceGapText = computed(() => {
  if (!suggestedSellPrice.value || !lowestSellPrice.value) return '暂无卖盘差距'
  const gap = suggestedSellPrice.value - lowestSellPrice.value
  if (Math.abs(gap) < 0.01) return '与最低在售持平'
  const percent = (gap / lowestSellPrice.value) * 100
  return `${gap > 0 ? '高于' : '低于'}最低在售 ${Math.abs(percent).toFixed(1)}%`
})
const pricingMetrics = computed(() => [
  {
    label: '近7日均价',
    value: moneyText(panelData.value?.avgTradePrice7d),
    detail: recentTrades.value.length ? `${recentTrades.value.length} 条成交样本` : '暂无成交样本',
    tone: ''
  },
  {
    label: '最高求购',
    value: moneyText(highestBuyPriceValue.value),
    detail: highestBuyPriceValue.value ? '买盘上限' : '暂无求购',
    tone: ''
  },
  {
    label: '7日涨跌',
    value: formatPercent(panelData.value?.priceChange7d),
    detail: '短期变化',
    tone: getChangeClass(panelData.value?.priceChange7d)
  },
  {
    label: '30日涨跌',
    value: formatPercent(panelData.value?.priceChange30d),
    detail: '月度变化',
    tone: getChangeClass(panelData.value?.priceChange30d)
  }
])
const scoreMetrics = computed(() => [
  {
    label: '热度',
    value: safeScore(panelData.value?.heatScore),
    hint: getScoreHint('heat', panelData.value?.heatScore)
  },
  {
    label: '流动性',
    value: safeScore(panelData.value?.liquidityScore),
    hint: getScoreHint('liquidity', panelData.value?.liquidityScore)
  },
  {
    label: '波动性',
    value: safeScore(panelData.value?.volatilityScore),
    hint: getScoreHint('volatility', panelData.value?.volatilityScore)
  }
])

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
      return `${point.axisValue}<br/>价格: ${moneyText(point.data)}`
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
      formatter: (value) => moneyText(value)
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
        color: '#2563eb'
      },
      itemStyle: {
        color: '#f59e0b'
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

const moneyText = (value) => {
  const price = formatPrice(value)
  return price === '--' ? '--' : `¥ ${price}`
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
  return Number.isFinite(numeric) ? Math.max(0, Math.min(100, Math.round(numeric))) : 0
}

const getScoreHint = (type, value) => {
  const score = safeScore(value)
  if (type === 'volatility') {
    if (score >= 65) return '波动偏高'
    if (score >= 35) return '波动适中'
    return '价格较稳'
  }
  if (score >= 70) return '表现较强'
  if (score >= 40) return '表现一般'
  return '样本偏少'
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
watch(() => props.activeView, (activeView, previousView) => {
  if (activeView === 'pricing' && previousView !== 'pricing') {
    fetchPanel()
  }
})
</script>

<style scoped>
.market-panel {
  min-height: 420px;
  padding: 34px 48px 46px;
  background: #ffffff;
}

.panel-head,
.trade-section-head,
.trend-summary,
.lead-top,
.lead-meta,
.metric-strip,
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

.panel-head p,
.panel-note,
.trade-section-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.source-tag {
  display: inline-flex;
  align-items: center;
  flex: none;
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
  border-radius: 8px;
  background: #ffffff;
}

.trend-summary {
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

.pricing-board {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.75fr);
  gap: 16px;
  margin-top: 24px;
}

.pricing-lead,
.pricing-side article,
.metric-item,
.score-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.pricing-lead {
  min-height: 220px;
  padding: 24px 26px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent 36%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.lead-top {
  justify-content: flex-start;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.lead-icon {
  display: inline-grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #172033;
  color: #ffffff;
}

.lead-label {
  display: block;
  margin-top: 24px;
  color: #64748b;
  font-size: 13px;
}

.pricing-lead strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1;
}

.pricing-lead p {
  max-width: 660px;
  margin: 16px 0 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
}

.lead-meta {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin-top: 18px;
}

.lead-meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.pricing-side {
  display: grid;
  gap: 14px;
}

.pricing-side article {
  min-height: 103px;
  padding: 18px 20px;
}

.pricing-side span,
.metric-item span,
.score-item span,
.trade-row span,
.trade-row em {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.pricing-side strong {
  display: block;
  margin-top: 8px;
  color: #172033;
  font-size: 26px;
  line-height: 1;
}

.pricing-side em,
.metric-item em,
.score-item em {
  display: block;
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.metric-strip {
  margin-top: 14px;
  align-items: stretch;
}

.metric-item {
  flex: 1;
  min-width: 0;
  padding: 16px;
  background: #f8fafc;
}

.metric-item strong {
  display: block;
  margin-top: 8px;
  color: #172033;
  font-size: 20px;
  line-height: 1;
}

.metric-item.positive strong {
  color: #15803d;
}

.metric-item.negative strong {
  color: #b91c1c;
}

.score-grid {
  margin-top: 14px;
  align-items: stretch;
}

.score-item {
  flex: 1;
  min-width: 0;
  padding: 16px;
}

.score-item > div:first-child {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.score-item strong {
  color: #172033;
  font-size: 24px;
  line-height: 1;
}

.score-track {
  height: 7px;
  margin-top: 13px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.score-track i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #f59e0b);
}

.trade-section {
  margin-top: 24px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.trade-section-head {
  min-height: 70px;
  padding: 14px 22px;
  background: #f8fafc;
  border-bottom: 1px solid #eef2f7;
}

.trade-section-head span {
  flex: none;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
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

.trade-row {
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
}

.trade-row:last-child {
  border-bottom: 0;
}

.trade-row strong {
  color: #f59e0b;
  font-size: 20px;
}

.trade-row em {
  font-style: normal;
}

@media (max-width: 1024px) {
  .market-panel {
    padding: 28px 24px 32px;
  }

  .pricing-board {
    grid-template-columns: 1fr;
  }

  .metric-strip,
  .score-grid {
    flex-wrap: wrap;
  }

  .metric-item,
  .score-item {
    flex-basis: calc(50% - 8px);
  }
}

@media (max-width: 768px) {
  .market-panel {
    padding: 18px;
  }

  .panel-head,
  .trade-section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .metric-item,
  .score-item {
    flex-basis: 100%;
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
