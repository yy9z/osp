<template>
  <div class="knowledge-card">
    <div class="knowledge-header">
      <div class="header-title">
        <el-icon><Reading /></el-icon>
        <span>RAG 知识检索</span>
      </div>
      <el-tag v-if="hit" size="small" type="success">已命中</el-tag>
      <el-tag v-else size="small" type="info">未命中</el-tag>
    </div>

    <div class="knowledge-body">
      <template v-if="hit">
        <div v-if="firstContent" class="knowledge-excerpt">{{ firstContent }}</div>

        <div v-if="citations.length" class="source-section">
          <div class="section-title">知识来源</div>
          <div v-for="citation in citations" :key="citation.chunkId || citation.index" class="source-row">
            <div class="source-index">{{ citation.index }}</div>
            <div class="source-main">
              <div class="source-title">{{ citation.title || '平台知识库' }}</div>
              <div class="source-path">{{ citation.source || 'rag/campus_knowledge.md' }}</div>
              <div v-if="citation.tags?.length" class="source-tags">
                <el-tag v-for="tag in citation.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="empty-state">知识库中暂未找到足够相关的内容，可以换一种问法再试。</div>
    </div>

    <div v-if="hit && scoreText" class="knowledge-footer">
      检索相关度 {{ scoreText }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Reading } from '@element-plus/icons-vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const safeData = computed(() => props.data || {})
const hit = computed(() => Boolean(safeData.value.hit))
const citations = computed(() => Array.isArray(safeData.value.citations) ? safeData.value.citations : [])
const chunks = computed(() => Array.isArray(safeData.value.chunks) ? safeData.value.chunks : [])
const firstContent = computed(() => {
  const content = String(chunks.value[0]?.content || '').trim()
  if (!content) return ''
  return content.length > 260 ? `${content.slice(0, 260)}…` : content
})
const scoreText = computed(() => {
  const score = Number(safeData.value.topScore)
  return Number.isFinite(score) ? score.toFixed(2) : ''
})
</script>

<style scoped>
.knowledge-card {
  margin-top: 8px;
  overflow: hidden;
  border: 1px solid #d9e8ff;
  border-radius: 10px;
  background: #fff;
}

.knowledge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f2f0ff 100%);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #315b91;
  font-size: 14px;
  font-weight: 600;
}

.knowledge-body { padding: 12px; }
.knowledge-excerpt {
  color: #3f4752;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.source-section { margin-top: 12px; }
.section-title {
  margin-bottom: 7px;
  color: #909399;
  font-size: 12px;
}

.source-row {
  display: flex;
  gap: 9px;
  padding: 8px;
  border-radius: 7px;
  background: #f7f9fc;
}
.source-row + .source-row { margin-top: 6px; }
.source-index {
  display: flex;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.source-main { min-width: 0; flex: 1; }
.source-title { color: #303133; font-size: 13px; font-weight: 600; }
.source-path { margin-top: 2px; color: #909399; font-size: 11px; word-break: break-all; }
.source-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 5px; }
.empty-state { padding: 8px 0; color: #909399; font-size: 13px; text-align: center; }
.knowledge-footer {
  padding: 7px 12px;
  border-top: 1px solid #edf1f7;
  color: #909399;
  font-size: 11px;
  text-align: right;
}
</style>
