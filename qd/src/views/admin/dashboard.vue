<template>
  <div class="admin-page">
    <section class="stat-grid">
      <el-card
        v-for="card in statsCards"
        :key="card.label"
        shadow="never"
        class="stat-card"
      >
        <div class="stat-card__head">
          <div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
          <div class="stat-icon" :style="{ background: card.iconBg, color: card.iconColor }">
            <el-icon>
              <component :is="card.icon" />
            </el-icon>
          </div>
        </div>
        <div class="stat-subtitle">{{ card.subtext }}</div>
      </el-card>
    </section>

    <section class="chart-grid">
      <el-card shadow="never" class="panel panel-main">
        <div class="panel-head">
          <div>
            <div class="panel-title">交易趋势</div>
            <div class="panel-subtitle">近 7 天订单数与成交额</div>
          </div>
          <div class="panel-actions">
            <el-tag type="primary" effect="plain">交易看板</el-tag>
            <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          </div>
        </div>
        <v-chart class="chart chart-large" :option="tradeTrendOption" autoresize />
      </el-card>

      <el-card shadow="never" class="panel panel-side">
        <div class="panel-head">
          <div>
            <div class="panel-title">订单状态</div>
            <div class="panel-subtitle">待处理、完成与其他</div>
          </div>
          <el-tag effect="plain">订单分布</el-tag>
        </div>
        <v-chart class="chart chart-ring" :option="orderStatusOption" autoresize />
        <div class="legend-list">
          <div v-for="item in orderStatusList" :key="item.label" class="legend-item">
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <span class="legend-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </el-card>
    </section>

    <section class="bottom-grid">
      <el-card shadow="never" class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">用户增长</div>
            <div class="panel-subtitle">近 7 天新增与累计用户</div>
          </div>
          <el-tag effect="plain">用户趋势</el-tag>
        </div>
        <v-chart class="chart chart-medium" :option="userGrowthOption" autoresize />
      </el-card>

      <el-card shadow="never" class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">Steam 同步状态</div>
            <div class="panel-subtitle">最近一次饰品同步任务进度</div>
          </div>
          <el-tag :type="syncTagType" effect="light">{{ syncStatusText }}</el-tag>
        </div>

        <div class="sync-panel">
          <div class="sync-metrics">
            <div class="sync-metric">
              <span>同步进度</span>
              <strong>{{ syncProgress }}%</strong>
            </div>
            <div class="sync-metric">
              <span>同步饰品</span>
              <strong>{{ formatNumber(latestSync.processedCount || latestSync.totalCount || 0) }}</strong>
            </div>
            <div class="sync-metric">
              <span>更新时间</span>
              <strong>{{ syncFinishedAt }}</strong>
            </div>
          </div>

          <el-progress
            :percentage="syncProgress"
            :stroke-width="12"
            :status="syncProgress === 100 ? 'success' : undefined"
          />

          <div class="sync-note">{{ syncNote }}</div>

          <div class="quick-grid">
            <button type="button" class="quick-card quick-card-primary" @click="handleRebuildMarket">
              <span class="quick-title">重建行情数据</span>
              <span class="quick-text">刷新成交参考和统计结果</span>
            </button>
            <button type="button" class="quick-card" @click="go('/admin/sync')">
              <span class="quick-title">进入同步维护</span>
              <span class="quick-text">查看任务记录和清理入口</span>
            </button>
            <button type="button" class="quick-card" @click="go('/admin/withdrawals')">
              <span class="quick-title">待审提现</span>
              <span class="quick-text">当前 {{ stats.pendingWithdrawals ?? 0 }} 笔</span>
            </button>
            <button type="button" class="quick-card" @click="go('/admin/messages')">
              <span class="quick-title">消息管理</span>
              <span class="quick-text">处理系统消息与站内通知</span>
            </button>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Goods, Money, Refresh, Tickets, User } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import {
  getLatestSteamSyncTask,
  getStatistics,
  getTradeTrend,
  getUserGrowth,
  rebuildMarketData
} from '@/api/admin'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const loading = ref(false)
const stats = ref({})
const tradeTrend = ref({})
const userGrowth = ref({})
const latestSync = ref({})

const toNumber = (value, digits = 0) => {
  const numeric = Number(value || 0)
  return Number.isFinite(numeric)
    ? numeric.toLocaleString('zh-CN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
      })
    : '0'
}

const formatNumber = (value) => toNumber(value, 0)
const formatMoney = (value) => `¥ ${toNumber(value, 2)}`

const statsCards = computed(() => [
  {
    label: '用户总数',
    value: formatNumber(stats.value.totalUsers),
    subtext: `活跃用户 ${formatNumber(stats.value.activeUsers)}`,
    icon: User,
    iconBg: 'rgba(37, 99, 235, 0.12)',
    iconColor: '#2563eb'
  },
  {
    label: '订单总数',
    value: formatNumber(stats.value.totalOrders),
    subtext: `今日订单 ${formatNumber(stats.value.todayOrders)}`,
    icon: Tickets,
    iconBg: 'rgba(59, 130, 246, 0.12)',
    iconColor: '#1d4ed8'
  },
  {
    label: '交易总额',
    value: formatMoney(stats.value.totalTradeAmount),
    subtext: `今日成交 ${formatMoney(stats.value.todayTradeAmount)}`,
    icon: Money,
    iconBg: 'rgba(245, 158, 11, 0.12)',
    iconColor: '#d97706'
  },
  {
    label: '饰品总数',
    value: formatNumber(stats.value.totalItems),
    subtext: `可用饰品 ${formatNumber(stats.value.activeItems)}`,
    icon: Goods,
    iconBg: 'rgba(34, 197, 94, 0.12)',
    iconColor: '#16a34a'
  }
])

const orderStatusList = computed(() => {
  const totalOrders = Number(stats.value.totalOrders || 0)
  const pendingOrders = Number(stats.value.pendingOrders || 0)
  const completedOrders = Number(stats.value.completedOrders || 0)
  const otherOrders = Math.max(totalOrders - pendingOrders - completedOrders, 0)
  return [
    { label: '待处理订单', value: pendingOrders, color: '#f59e0b' },
    { label: '已完成订单', value: completedOrders, color: '#22c55e' },
    { label: '其他订单', value: otherOrders, color: '#64748b' }
  ]
})

const tradeTrendOption = computed(() => ({
  color: ['#2563eb', '#f59e0b'],
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: {
      color: '#64748b'
    }
  },
  grid: {
    left: 12,
    right: 16,
    top: 24,
    bottom: 42,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: tradeTrend.value.dates || [],
    axisLine: {
      lineStyle: {
        color: '#dbe2ee'
      }
    },
    axisLabel: {
      color: '#64748b'
    }
  },
  yAxis: [
    {
      type: 'value',
      name: '订单',
      minInterval: 1,
      axisLabel: {
        color: '#64748b'
      },
      splitLine: {
        lineStyle: {
          color: '#edf2f7'
        }
      }
    },
    {
      type: 'value',
      name: '成交额',
      axisLabel: {
        color: '#64748b',
        formatter: (value) => `¥${Number(value || 0)}`
      },
      splitLine: {
        show: false
      }
    }
  ],
  series: [
    {
      name: '订单数',
      type: 'line',
      smooth: true,
      yAxisIndex: 0,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: {
        width: 3
      },
      areaStyle: {
        color: 'rgba(37, 99, 235, 0.14)'
      },
      data: tradeTrend.value.orderCounts || []
    },
    {
      name: '成交额',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 2
      },
      data: (tradeTrend.value.amounts || []).map((item) => Number(item || 0))
    }
  ]
}))

const orderStatusOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c}'
  },
  series: [
    {
      type: 'pie',
      radius: ['58%', '78%'],
      center: ['50%', '44%'],
      label: {
        formatter: '{b}\n{c}',
        color: '#475569',
        fontSize: 12
      },
      labelLine: {
        length: 12,
        length2: 10
      },
      data: orderStatusList.value.map((item) => ({
        value: item.value,
        name: item.label,
        itemStyle: { color: item.color }
      }))
    }
  ]
}))

const userGrowthOption = computed(() => ({
  color: ['#2563eb', '#22c55e'],
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: {
      color: '#64748b'
    }
  },
  grid: {
    left: 12,
    right: 16,
    top: 20,
    bottom: 42,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: userGrowth.value.dates || [],
    axisLine: {
      lineStyle: {
        color: '#dbe2ee'
      }
    },
    axisLabel: {
      color: '#64748b'
    }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: {
      color: '#64748b'
    },
    splitLine: {
      lineStyle: {
        color: '#edf2f7'
      }
    }
  },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: {
        width: 3
      },
      data: userGrowth.value.newUsers || []
    },
    {
      name: '累计用户',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 2
      },
      areaStyle: {
        color: 'rgba(34, 197, 94, 0.12)'
      },
      data: userGrowth.value.totalUsers || []
    }
  ]
}))

const syncProgress = computed(() => {
  const plannedPages = Number(latestSync.value.plannedPages || latestSync.value.totalPages || 0)
  const syncedPages = Number(latestSync.value.syncedPages || latestSync.value.currentPage || 0)
  if (plannedPages > 0) {
    return Math.min(100, Math.max(0, Math.round((syncedPages / plannedPages) * 100)))
  }
  if (String(latestSync.value.status || '').toUpperCase() === 'SUCCESS') {
    return 100
  }
  return 0
})

const syncStatusText = computed(() => {
  const status = String(latestSync.value.status || '')
  if (!status) return '暂无记录'
  if (status === 'SUCCESS') return '已完成'
  if (status === 'RUNNING') return '同步中'
  if (status === 'FAILED') return '同步失败'
  return status
})

const syncTagType = computed(() => {
  if (syncStatusText.value === '已完成') return 'success'
  if (syncStatusText.value === '同步中') return 'warning'
  if (syncStatusText.value === '同步失败') return 'danger'
  return 'info'
})

const syncFinishedAt = computed(() => {
  const value = latestSync.value.finishedAt || latestSync.value.updatedAt || latestSync.value.createdAt
  return value ? new Date(value).toLocaleString('zh-CN') : '暂无'
})

const syncNote = computed(() => {
  if (!latestSync.value?.id) {
    return '当前还没有同步任务记录，可以先进入同步维护页执行一次饰品同步。'
  }
  const savedCount = formatNumber(latestSync.value.savedCount || 0)
  const updatedCount = formatNumber(latestSync.value.updatedCount || 0)
  if (syncStatusText.value === '同步失败') {
    return latestSync.value.errorMessage || '最近一次同步未完成，请到同步维护页查看异常原因。'
  }
  return `最近一次同步新增 ${savedCount} 条，更新 ${updatedCount} 条。`
})

const loadData = async () => {
  loading.value = true
  try {
    const [statistics, trade, growth, syncTask] = await Promise.all([
      getStatistics(),
      getTradeTrend(7),
      getUserGrowth(7),
      getLatestSteamSyncTask('STEAM_ITEM_SYNC').catch(() => null)
    ])
    stats.value = statistics || {}
    tradeTrend.value = trade || {}
    userGrowth.value = growth || {}
    latestSync.value = syncTask || {}
  } finally {
    loading.value = false
  }
}

const handleRebuildMarket = async () => {
  await rebuildMarketData()
  ElMessage.success('行情数据已重建')
  loadData()
}

const go = (path) => router.push(path)

onMounted(loadData)
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.chart-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(340px, 1fr);
  gap: 16px;
}

.panel,
.stat-card {
  border: 0;
  border-radius: 10px;
}

.stat-card {
  min-height: 120px;
}

.stat-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stat-label,
.panel-subtitle,
.stat-subtitle,
.sync-note,
.sync-metric span,
.legend-label {
  color: #64748b;
}

.stat-label {
  font-size: 14px;
}

.stat-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.stat-subtitle {
  margin-top: 14px;
  font-size: 13px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.panel-title {
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.panel-subtitle {
  margin-top: 6px;
  font-size: 13px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chart {
  width: 100%;
}

.chart-large {
  height: 340px;
}

.chart-medium {
  height: 300px;
}

.chart-ring {
  height: 280px;
}

.legend-list {
  display: grid;
  gap: 12px;
}

.legend-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-item strong {
  color: #111827;
}

.sync-panel {
  display: grid;
  gap: 18px;
}

.sync-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.sync-metric {
  padding: 16px;
  border-radius: 10px;
  background: #f8fafc;
}

.sync-metric strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 18px;
  line-height: 1.4;
}

.sync-note {
  font-size: 13px;
  line-height: 1.7;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 10px 22px rgba(148, 163, 184, 0.14);
}

.quick-card-primary {
  border-color: rgba(37, 99, 235, 0.18);
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.08), rgba(255, 255, 255, 1));
}

.quick-title {
  display: block;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.quick-text {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .stat-grid,
  .chart-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .sync-metrics,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .panel-title {
    font-size: 20px;
  }
}
</style>
