<template>
  <el-card class="ai-health-strip" shadow="never">
    <div class="strip-main">
      <div class="strip-left">
        <el-icon class="status-icon" :class="overallClass"><CircleCheck v-if="overallOk" /><Warning v-else /></el-icon>
        <div>
          <div class="strip-title">AI 服务状态</div>
          <div class="strip-summary">{{ summaryText }}</div>
        </div>
      </div>
      <div class="strip-actions">
        <el-button text type="primary" size="small" :loading="loading" @click="load">刷新</el-button>
        <el-button text size="small" @click="expanded = !expanded">{{ expanded ? '收起' : '详情' }}</el-button>
      </div>
    </div>

    <el-collapse-transition>
      <div v-if="expanded && snapshot" class="strip-detail">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="运行模式">{{ snapshot.lawfirmAiMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="云端 GLM">{{ snapshot.cloudGlm ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="文书路由">{{ routeLabel('DOCUMENT') }}</el-descriptions-item>
          <el-descriptions-item label="识别路由">{{ routeLabel('DOCUMENT_RECOGNITION_EXTRACT') }}</el-descriptions-item>
          <el-descriptions-item label="问答路由">{{ routeLabel('GENERAL_CHAT') }}</el-descriptions-item>
          <el-descriptions-item label="最近调用">{{ lastCallText }}</el-descriptions-item>
        </el-descriptions>
        <p v-if="lastCallError" class="last-err">最近失败：{{ lastCallError }}</p>
      </div>
    </el-collapse-transition>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { CircleCheck, Warning } from '@element-plus/icons-vue'
import { getAiDiagnostics } from '@/api/ai'

const loading = ref(false)
const expanded = ref(false)
const snapshot = ref(null)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getAiDiagnostics()
    snapshot.value = res.data || null
  } catch (e) {
    loadError.value = e.message || '无法获取诊断信息'
    snapshot.value = null
  } finally {
    loading.value = false
  }
}

function routeLabel(useCase) {
  const row = snapshot.value?.routing?.[useCase]
  if (!row) return '-'
  if (row.ok) return `${row.providerType} / ${row.modelName || '默认模型'}`
  return `异常：${row.error || '未配置'}`
}

const lastCall = computed(() => {
  const list = snapshot.value?.recentLlmCalls
  return Array.isArray(list) && list.length > 0 ? list[0] : null
})

const lastCallText = computed(() => {
  if (!lastCall.value) return '暂无记录'
  const ok = lastCall.value.success
  const ms = lastCall.value.durationMs
  return `${ok ? '成功' : '失败'} · ${lastCall.value.operation || '-'} · ${ms != null ? `${ms}ms` : '-'}`
})

const lastCallError = computed(() => {
  if (!lastCall.value || lastCall.value.success) return ''
  return lastCall.value.errorHint || ''
})

const overallOk = computed(() => {
  if (loadError.value) return false
  if (!snapshot.value) return false
  const doc = snapshot.value.routing?.DOCUMENT
  const chat = snapshot.value.routing?.GENERAL_CHAT
  return Boolean(snapshot.value.cloudGlm && doc?.ok && chat?.ok)
})

const overallClass = computed(() => (overallOk.value ? 'ok' : 'warn'))

const summaryText = computed(() => {
  if (loading.value) return '正在检查…'
  if (loadError.value) {
    return loadError.value.includes('403')
      ? '当前账号无诊断权限；AI 功能仍可使用，配置问题请联系管理员。'
      : loadError.value
  }
  if (!snapshot.value) return '未获取到诊断数据'
  if (overallOk.value) {
    return `智谱 GLM 已就绪（${snapshot.value.lawfirmAiMode}）· 识别/生成/问答可用`
  }
  return '部分 AI 路由异常，展开详情查看；密钥请检查 backend/.env 中 ZHIPU_API_KEY'
})

onMounted(load)
</script>

<style scoped>
.ai-health-strip {
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
}
.strip-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.strip-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.status-icon {
  font-size: 22px;
  margin-top: 2px;
}
.status-icon.ok {
  color: var(--el-color-success);
}
.status-icon.warn {
  color: var(--el-color-warning);
}
.strip-title {
  font-weight: 600;
  font-size: 14px;
}
.strip-summary {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
.strip-detail {
  margin-top: 12px;
}
.last-err {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-color-danger);
}
</style>
