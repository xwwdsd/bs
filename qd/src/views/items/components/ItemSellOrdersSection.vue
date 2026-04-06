<template>
  <section id="sell-orders" class="sell-section" v-loading="loading">
    <div class="section-toolbar">
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
            <span>{{ getCollectionTypeLabel(order) }} &middot; {{ getExteriorText(getItemExterior(order)) }}</span>
          </div>
        </div>

        <div class="table-cell wear-cell" @click="$emit('select-order', order.id)">
          <p class="wear-value">{{ text.wear }}: {{ getWearDisplay(order) }}</p>
          <div v-if="hasWearVisual(order)" class="wear-bar">
            <span class="wear-marker" :style="{ left: `${getWearPercent(order)}%` }"></span>
          </div>
          <div class="status-row">
            <span class="status-pill">{{ getExteriorText(getItemExterior(order)) }}</span>
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
defineProps({
  loading: {
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
