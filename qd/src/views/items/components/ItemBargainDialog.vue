<template>
  <el-dialog v-model="visibleProxy" :title="text.bargainTitle" width="440px">
    <div class="bargain-dialog">
      <p class="bargain-dialog__item">{{ targetName }}</p>
      <p class="bargain-dialog__meta">{{ text.currentSalePrice }}: {{ text.currency }} {{ targetPriceText }}</p>
      <p class="bargain-dialog__hint">{{ text.bargainHint }}</p>

      <el-form label-position="top">
        <el-form-item :label="text.bargainPriceLabel">
          <el-input-number
            v-model="priceProxy"
            :min="0.01"
            :max="targetMaxPrice"
            :precision="2"
            :step="0.01"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visibleProxy = false">{{ text.cancel }}</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">
        {{ text.sendBargain }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  price: {
    type: Number,
    default: 0.01
  },
  text: {
    type: Object,
    required: true
  },
  targetName: {
    type: String,
    default: ''
  },
  targetPriceText: {
    type: String,
    default: '0.00'
  },
  targetMaxPrice: {
    type: Number,
    default: undefined
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'update:price', 'submit'])

const visibleProxy = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const priceProxy = computed({
  get: () => props.price,
  set: (value) => emit('update:price', Number(value ?? 0))
})
</script>

<style scoped>
.bargain-dialog__item {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.bargain-dialog__meta {
  margin: 0 0 8px;
  color: #f59e0b;
  font-size: 14px;
  font-weight: 700;
}

.bargain-dialog__hint {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
</style>
