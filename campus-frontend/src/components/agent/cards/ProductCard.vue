<template>
  <div class="product-list">
    <div
      v-for="item in items"
      :key="item.id"
      class="product-card"
      @click="goDetail(item.id)"
    >
      <div class="product-info">
        <div class="product-title">{{ item.title }}</div>
        <div class="product-meta">
          <el-tag size="small" type="info">{{ item.category }}</el-tag>
          <el-tag size="small" type="success" v-if="item.condition">{{ conditionText(item.condition) }}</el-tag>
        </div>
        <div class="product-reason" v-if="item.recommendReason">{{ item.recommendReason }}</div>
      </div>
      <div class="product-price">
        <span class="price-num">¥{{ item.price }}</span>
        <el-button type="primary" size="small" link>查看</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const props = defineProps({
  data: { type: Object, required: true }
})

const items = props.data?.items || []

function conditionText(c) {
  return { NEW: '全新', LIKE_NEW: '九成新', GOOD: '良好', FAIR: '一般' }[c] || c
}
function goDetail(id) {
  router.push('/secondhand/' + id)
}
</script>

<style scoped>
.product-list { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.product-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow .2s;
}
.product-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.12); }
.product-info { flex: 1; }
.product-title { font-size: 14px; font-weight: 500; color: #303133; }
.product-meta { display: flex; gap: 4px; margin: 4px 0; }
.product-reason { font-size: 12px; color: #909399; }
.product-price { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.price-num { font-size: 16px; font-weight: 700; color: #f56c6c; }
</style>
