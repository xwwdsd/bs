<template>
  <section id="sell-orders" class="sell-section" :class="{ compact }" v-loading="loading">
    <div v-if="!hideHeader" class="section-toolbar">
      <div class="section-tab active">{{ text.currentOnSale }}</div>
      <span class="section-count">{{ orders.length }} {{ text.saleRecordSuffix }}</span>
    </div>

    <el-empty v-if="!orders.length && !loading" :description="text.emptyListings" />

    <div v-else class="sell-table">
      <div class="sell-table-head">
        <span>{{ text.item }}</span>
        <span>{{ text.wearLevel }}</span>
        <span>{{ text.seller }}</span>
        <span>{{ text.price }}</span>
        <span>{{ text.action }}</span>
      </div>

      <article
        v-for="order in orders"
        :key="order.id"
        class="sell-row"
        :class="{ active: selectedOrderId === order.id }"
      >
        <div class="table-cell item-cell">
          <div class="item-thumb">
            <img :src="getItemIcon(order)" :alt="getItemName(order)" />
          </div>
          <div class="item-copy">
            <strong>{{ getItemName(order) }}</strong>
            <span>{{ getOrderSubtitle(order) }}</span>
          </div>
        </div>

        <div class="table-cell wear-cell" @click="$emit('select-order', order.id)">
          <p v-if="shouldShowWearInfo(order)" class="wear-value">{{ text.wear }}: {{ getWearDisplay(order) }}</p>
          <div v-if="shouldShowWearInfo(order) && hasWearVisual(order)" class="wear-bar">
            <span class="wear-marker" :style="{ left: `${getWearPercent(order)}%` }"></span>
          </div>
          <div class="status-row" :class="{ compact: !shouldShowWearInfo(order) }">
            <span class="status-pill" :class="getOrderPrimaryBadgeClass(order)">{{ getOrderPrimaryBadgeText(order) }}</span>
            <span
              v-if="getOrderSecondaryBadgeText(order)"
              class="status-pill status-pill-type"
              :class="getOrderSecondaryBadgeClass(order)"
            >
              {{ getOrderSecondaryBadgeText(order) }}
            </span>
            <span class="status-pill" :class="{ warn: getOrderStatus(order).tone === 'warn' }">
              {{ getOrderStatus(order).text }}
            </span>
            <span v-if="selectedOrderId === order.id" class="status-pill current">{{ text.currentViewing }}</span>
          </div>
        </div>

        <div class="table-cell seller-cell">
          <el-avatar :size="44" :src="order.user?.avatar || ''">
            {{ getAvatarFallback(order.user) }}
          </el-avatar>
          <div class="seller-info">
            <strong>{{ order.user?.username || text.unknownSeller }}</strong>
          </div>
        </div>

        <div class="table-cell price-cell">
          <strong>{{ text.currency }} {{ formatPrice(order.price) }}</strong>
          <span>{{ formatDate(order.createdAt) }}</span>
        </div>

        <div class="table-cell action-cell">
          <el-button
            plain
            :disabled="order.userId === currentUserId || !canBargain(order)"
            @click="$emit('bargain', order)"
          >
            {{ text.bargain }}
          </el-button>
          <el-button type="primary" :disabled="order.userId === currentUserId" @click="$emit('buy', order)">
            {{ text.buy }}
          </el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { getItemDisplayModel } from '@/utils/itemDisplay'

defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  compact: {
    type: Boolean,
    default: false
  },
  hideHeader: {
    type: Boolean,
    default: false
  },
  orders: {
    type: Array,
    default: () => []
  },
  selectedOrderId: {
    type: [Number, String],
    default: null
  },
  currentUserId: {
    type: [Number, String],
    default: null
  },
  text: {
    type: Object,
    required: true
  },
  canBargain: {
    type: Function,
    required: true
  },
  formatDate: {
    type: Function,
    required: true
  },
  formatPrice: {
    type: Function,
    required: true
  },
  getAvatarFallback: {
    type: Function,
    required: true
  },
  getCollectionTypeLabel: {
    type: Function,
    required: true
  },
  getExteriorText: {
    type: Function,
    required: true
  },
  getItemExterior: {
    type: Function,
    required: true
  },
  getItemIcon: {
    type: Function,
    required: true
  },
  getItemName: {
    type: Function,
    required: true
  },
  getOrderStatus: {
    type: Function,
    required: true
  },
  getWearDisplay: {
    type: Function,
    required: true
  },
  getWearPercent: {
    type: Function,
    required: true
  },
  hasWearVisual: {
    type: Function,
    required: true
  }
})

defineEmits(['select-order', 'bargain', 'buy'])

const getOrderDisplayModel = (order) => getItemDisplayModel(order)
const shouldShowWearInfo = (order) => getOrderDisplayModel(order).showWearModule
const getOrderSubtitle = (order) => getOrderDisplayModel(order).subtitle
const getOrderPrimaryBadgeText = (order) => getOrderDisplayModel(order).primaryBadge.text
const getOrderPrimaryBadgeClass = (order) => {
  const badge = getOrderDisplayModel(order).primaryBadge
  if (!badge) return ''
  if (badge.kind === 'quality') return `status-pill-quality-${badge.code}`
  if (badge.kind === 'category') return `status-pill-category-${badge.code || 'other'}`
  return `status-pill-exterior-${badge.code || 'UN'}`
}
const getOrderSecondaryBadgeText = (order) => getOrderDisplayModel(order).secondaryBadge?.text || ''
const getOrderSecondaryBadgeClass = (order) => {
  const badge = getOrderDisplayModel(order).secondaryBadge
  return badge ? `status-pill-type-${badge.code}` : ''
}
</script>

<style scoped>
.sell-section {
  margin-top: 18px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(180deg, #181d27 0%, #141822 100%);
  border-bottom: 1px solid rgba(96, 165, 250, 0.18);
}

.section-tab {
  position: relative;
  color: rgba(226, 232, 240, 0.74);
  font-size: 18px;
  font-weight: 700;
}

.section-tab.active {
  color: #ffffff;
}

.section-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -18px;
  width: 100%;
  height: 3px;
  border-radius: 999px;
  background: #60a5fa;
}

.section-count {
  color: #cbd5e1;
  font-size: 14px;
}

.sell-section :deep(.el-empty) {
  background: #ffffff;
  padding: 54px 0;
}

.sell-table {
  background: #ffffff;
}

.sell-table-head,
.sell-row {
  display: grid;
  grid-template-columns: 150px minmax(220px, 1fr) minmax(190px, 0.9fr) 128px 168px;
  gap: 14px;
  align-items: center;
  padding: 0 20px;
}

.sell-table-head {
  min-height: 56px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.sell-row {
  min-height: 132px;
  border-bottom: 1px solid #e5e7eb;
  transition: background-color 0.2s ease;
}

.sell-row:hover {
  background: #f8fafc;
}

.sell-row.active {
  background: #fffaf0;
}

.table-cell {
  min-width: 0;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-thumb {
  width: 110px;
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef2f7 100%);
}

.item-thumb img {
  max-width: 92px;
  max-height: 60px;
  object-fit: contain;
}

.item-copy {
  display: none;
  min-width: 0;
}

.item-copy strong {
  display: block;
  margin-bottom: 6px;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.4;
}

.item-copy span {
  color: #64748b;
  font-size: 13px;
}

.wear-cell {
  cursor: pointer;
}

.wear-value {
  margin: 0 0 12px;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.wear-bar {
  position: relative;
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, #22c55e 0%, #facc15 45%, #ef4444 100%);
}

.wear-marker {
  position: absolute;
  top: -5px;
  width: 0;
  height: 0;
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 10px solid #475569;
  transform: translateX(-50%);
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-row.compact {
  margin-top: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.status-pill.warn {
  background: rgba(248, 113, 113, 0.12);
  color: #dc2626;
}

.status-pill.current {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
}

.status-pill-type {
  background: rgba(15, 23, 42, 0.94);
  color: #ff8f1f;
}

.status-pill-type-Souvenir {
  color: #fbbf24;
}

.status-pill-type-Star {
  color: #ffffff;
}

.status-pill-type-StarStatTrak {
  color: #ffb347;
}

.status-pill-exterior-FN {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-pill-exterior-MW {
  background: rgba(132, 204, 22, 0.14);
  color: #65a30d;
}

.status-pill-exterior-FT {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.status-pill-exterior-WW {
  background: rgba(100, 116, 139, 0.16);
  color: #475569;
}

.status-pill-exterior-BS {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.status-pill-quality-contraband {
  background: rgba(245, 223, 77, 0.26);
  color: #854d0e;
}

.status-pill-quality-covert {
  background: rgba(244, 63, 94, 0.14);
  color: #be123c;
}

.status-pill-quality-classified {
  background: rgba(192, 38, 211, 0.14);
  color: #a21caf;
}

.status-pill-quality-restricted {
  background: rgba(139, 92, 246, 0.14);
  color: #7c3aed;
}

.status-pill-quality-mil-spec {
  background: rgba(59, 130, 246, 0.14);
  color: #2563eb;
}

.status-pill-quality-industrial {
  background: rgba(96, 165, 250, 0.18);
  color: #2563eb;
}

.status-pill-quality-consumer {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.status-pill-quality-extraordinary {
  background: rgba(249, 115, 22, 0.16);
  color: #c2410c;
}

.status-pill-quality-exotic {
  background: rgba(168, 85, 247, 0.16);
  color: #9333ea;
}

.status-pill-quality-remarkable {
  background: rgba(236, 72, 153, 0.14);
  color: #db2777;
}

.status-pill-quality-high-grade {
  background: rgba(79, 142, 247, 0.16);
  color: #2563eb;
}

.status-pill-quality-normal-grade {
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
}

.status-pill-quality-agent-grade,
.status-pill-category-agent {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-pill-category-sticker,
.status-pill-category-music {
  background: rgba(37, 99, 235, 0.14);
  color: #1d4ed8;
}

.status-pill-category-graffiti,
.status-pill-category-collectible {
  background: rgba(124, 58, 237, 0.14);
  color: #7c3aed;
}

.status-pill-category-charm,
.status-pill-category-tool {
  background: rgba(15, 118, 110, 0.14);
  color: #0f766e;
}

.status-pill-category-case {
  background: rgba(71, 85, 105, 0.16);
  color: #475569;
}

.status-pill-category-pass {
  background: rgba(124, 45, 18, 0.12);
  color: #9a3412;
}

.seller-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-info strong {
  display: block;
  color: #334155;
  font-size: 15px;
}

.price-cell strong {
  display: block;
  margin-bottom: 8px;
  color: #f59e0b;
  font-size: 26px;
  line-height: 1;
}

.price-cell span {
  color: #94a3b8;
  font-size: 13px;
}

.action-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}

.action-cell :deep(.el-button) {
  width: 100%;
  height: 34px;
  margin-left: 0;
  border-radius: 10px;
  font-weight: 700;
}

.sell-section.compact .sell-table-head {
  display: none;
}

.sell-section.compact .sell-row {
  grid-template-columns: 1fr;
  gap: 14px;
  padding: 18px 20px;
}

.sell-section.compact .item-copy {
  display: block;
}

.sell-section.compact .item-thumb {
  width: 120px;
  height: 86px;
}

.sell-section.compact .item-thumb img {
  max-width: 98px;
  max-height: 64px;
}

.sell-section.compact .action-cell {
  flex-direction: row;
}

@media (max-width: 1120px) {
  .sell-table-head {
    display: none;
  }

  .sell-row {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 18px 20px;
  }

  .item-copy {
    display: block;
  }

  .item-thumb {
    width: 120px;
    height: 86px;
  }

  .item-thumb img {
    max-width: 98px;
    max-height: 64px;
  }

  .action-cell {
    flex-direction: row;
  }
}

@media (max-width: 768px) {
  .section-toolbar {
    padding: 16px 18px;
  }

  .sell-row {
    padding: 16px;
  }

  .item-cell {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-thumb {
    width: 100%;
    height: 110px;
  }

  .item-thumb img {
    max-width: 130px;
    max-height: 82px;
  }

  .action-cell {
    flex-direction: column;
  }
}
</style>
