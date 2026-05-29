<template>
  <el-card class="ai-unified-wizard" shadow="never">
    <template #header>
      <div class="wizard-header">
        <span>智能文书向导</span>
        <el-tag type="info" size="small">上传一次 · 选择意图</el-tag>
      </div>
    </template>

    <AiProgressBanner
      :active="progressActive"
      :title="progressTitle"
      :hint="progressHint"
      :elapsed-sec="progressElapsed"
    />

    <el-steps :active="step" finish-status="success" align-center class="wizard-steps">
      <el-step title="上传文件" />
      <el-step title="选择意图" />
      <el-step title="查看结果" />
    </el-steps>

    <div v-if="step === 0" class="wizard-body">
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onFileChange"
        accept=".pdf,.jpg,.jpeg,.png,.docx,.txt"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div>拖拽或点击选择文书（PDF/图片/Word/TXT，≤50MB）</div>
      </el-upload>
      <p v-if="selectedFile" class="file-name">已选：{{ selectedFile.name }}</p>
      <p v-if="resolvedCaseId" class="case-hint">关联案件 ID：{{ resolvedCaseId }}</p>
      <el-button type="primary" :disabled="!selectedFile" @click="step = 1">下一步</el-button>
      <el-button
        v-if="intent === 'docGen'"
        text
        type="primary"
        class="skip-upload"
        @click="goDocGen"
      >
        无需上传，直接生成文书 →
      </el-button>
    </div>

    <div v-else-if="step === 1" class="wizard-body">
      <el-radio-group v-model="intent" class="intent-group">
        <el-radio value="intake">归入卷宗（智能录入案件档案）</el-radio>
        <el-radio value="prefill">预填新建案件</el-radio>
        <el-radio value="todo">识别并创建待办/日程</el-radio>
        <el-radio value="recognize">仅识别要素（不触发自动化）</el-radio>
        <el-radio value="docGen">生成法律文书（跳转文书生成）</el-radio>
      </el-radio-group>
      <div class="wizard-actions">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="running" @click="runIntent">开始处理</el-button>
      </div>
    </div>

    <div v-else class="wizard-body">
      <el-alert
        v-if="resultError"
        type="error"
        :title="resultErrorTitle"
        show-icon
        :closable="false"
      >
        <template #default>
          <pre class="error-detail">{{ resultErrorDetail }}</pre>
        </template>
      </el-alert>
      <template v-else-if="intakeResult">
        <el-alert
          :type="intakeResult.status === 'SUCCESS' ? 'success' : intakeResult.status === 'NEEDS_CASE' ? 'warning' : 'info'"
          :title="intakeResult.message || intakeResult.status"
          show-icon
          :closable="false"
        />
        <p v-if="intakeResult.pendingId">暂存 ID：{{ intakeResult.pendingId }} — 可在工作台卷宗录入中挂接案件或发起立案审批</p>
        <RecognitionResultActions
          v-if="intakeResult.recognition"
          :recognition="intakeResult.recognition"
          :case-id="intakeResult.caseId || resolvedCaseId"
          :source-file="selectedFile"
          @automation-done="onRecognitionAutomationDone"
        />
      </template>
      <template v-else-if="recognition">
        <RecognitionResultActions
          :recognition="recognition"
          :case-id="resolvedCaseId"
          :source-file="selectedFile"
          :show-automation="intent !== 'prefill'"
          @automation-done="onRecognitionAutomationDone"
        />
      </template>
      <div class="wizard-actions">
        <el-button @click="resetWizard">重新上传</el-button>
        <el-button v-if="intent === 'prefill' && recognition" type="primary" @click="goCreateCase">去新建案件</el-button>
        <el-button v-if="resolvedCaseId && intakeResult?.status === 'NEEDS_CASE'" type="primary" @click="goDashboardIntake">
          去工作台挂接
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { recognizeLegalDocument } from '@/api/ai'
import { processCaseIntake } from '@/api/caseIntake'
import { buildAiErrorHint, formatAiError } from '@/utils/aiError'
import { useAiProgress, AI_PROGRESS_HINTS } from '@/composables/useAiProgress'
import AiProgressBanner from '@/components/AiProgressBanner.vue'
import RecognitionResultActions from '@/components/RecognitionResultActions.vue'

const props = defineProps({
  initialIntent: { type: String, default: '' },
  caseId: { type: [String, Number], default: null }
})

const emit = defineEmits(['doc-gen'])

const router = useRouter()
const CASE_CREATE_PREFILL_KEY = 'case_create_prefill'

const step = ref(0)
const selectedFile = ref(null)
const intent = ref('intake')
const running = ref(false)
const recognition = ref(null)
const intakeResult = ref(null)
const resultError = ref('')
const resultErrorDetail = ref('')
const {
  active: progressActive,
  title: progressTitle,
  hint: progressHint,
  elapsedSec: progressElapsed,
  start: progressStart,
  stop: progressStop
} = useAiProgress()

const INTENT_PROGRESS = {
  intake: { title: '卷宗智能录入', hint: AI_PROGRESS_HINTS.intake },
  prefill: { title: '文书识别 · 预填建案', hint: AI_PROGRESS_HINTS.recognize },
  todo: { title: '识别并创建待办/日程', hint: AI_PROGRESS_HINTS.recognize },
  recognize: { title: '文书要素识别', hint: AI_PROGRESS_HINTS.recognize }
}

const resolvedCaseId = computed(() => {
  const id = props.caseId
  if (id === null || id === undefined || id === '') return null
  return Number(id) || id
})

const resultErrorTitle = computed(() => {
  const line = (resultErrorDetail.value || '').split('\n')[0]
  return line || '处理失败'
})

const MAX_SIZE = 50 * 1024 * 1024
const VALID_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain'
]

function applyRouteDefaults() {
  if (props.initialIntent) {
    intent.value = props.initialIntent
    if (props.initialIntent === 'docGen') {
      step.value = 0
    }
  }
}

onMounted(applyRouteDefaults)
watch(() => [props.initialIntent, props.caseId], applyRouteDefaults)

function onFileChange(file) {
  const raw = file.raw
  if (!raw) return
  if (!VALID_TYPES.includes(raw.type) && !raw.name.match(/\.(pdf|jpe?g|png|docx|txt)$/i)) {
    ElMessage.error('不支持的文件格式')
    return
  }
  if (raw.size > MAX_SIZE) {
    ElMessage.error('文件不能超过 50MB')
    return
  }
  selectedFile.value = raw
}

function onRecognitionAutomationDone(updated) {
  if (!updated) return
  if (recognition.value) {
    recognition.value = { ...recognition.value, ...updated }
  } else if (intakeResult.value?.recognition) {
    intakeResult.value = {
      ...intakeResult.value,
      recognition: { ...intakeResult.value.recognition, ...updated }
    }
  }
}

function buildPrefill(r) {
  return {
    source: 'ai_wizard',
    caseReason: r.caseReason || r.caseCause || '',
    court: r.courtName || r.court || '',
    courtCaseNumber: r.caseNumber || '',
    caseName: r.caseReason ? `${r.caseReason}案` : r.documentType || '新案件',
    summary: [r.documentType, r.plaintiffName, r.defendantName].filter(Boolean).join(' / '),
    partiesText: [r.plaintiffName, r.defendantName].filter(Boolean).join(' vs ')
  }
}

function setError(err) {
  const msg = formatAiError(err)
  resultErrorDetail.value = buildAiErrorHint(msg)
  resultError.value = msg
}

async function runIntent() {
  if (intent.value === 'docGen') {
    goDocGen()
    return
  }
  if (!selectedFile.value) return
  running.value = true
  resultError.value = ''
  resultErrorDetail.value = ''
  recognition.value = null
  intakeResult.value = null
  const prog = INTENT_PROGRESS[intent.value] || INTENT_PROGRESS.recognize
  progressStart(prog)
  try {
    if (intent.value === 'intake') {
      const res = await processCaseIntake(selectedFile.value, {
        caseId: resolvedCaseId.value || undefined
      })
      if (res.code === 200 || res.success) {
        intakeResult.value = res.data
        step.value = 2
      } else {
        throw new Error(res.message || '卷宗录入失败')
      }
    } else if (intent.value === 'prefill') {
      const res = await recognizeLegalDocument(selectedFile.value, null, false)
      if (res.code === 200 || res.success) {
        recognition.value = res.data
        sessionStorage.setItem(CASE_CREATE_PREFILL_KEY, JSON.stringify(buildPrefill(res.data)))
        step.value = 2
      } else {
        throw new Error(res.message || '识别失败')
      }
    } else if (intent.value === 'todo') {
      const res = await recognizeLegalDocument(
        selectedFile.value,
        resolvedCaseId.value || null,
        true
      )
      if (res.code === 200 || res.success) {
        recognition.value = res.data
        step.value = 2
        ElMessage.success('识别完成，已尝试创建待办/日程')
      } else {
        throw new Error(res.message || '识别失败')
      }
    } else {
      const res = await recognizeLegalDocument(
        selectedFile.value,
        resolvedCaseId.value || null,
        false
      )
      if (res.code === 200 || res.success) {
        recognition.value = res.data
        step.value = 2
      } else {
        throw new Error(res.message || '识别失败')
      }
    }
  } catch (e) {
    setError(e)
    step.value = 2
  } finally {
    running.value = false
    progressStop()
  }
}

function goDocGen() {
  emit('doc-gen', { caseId: resolvedCaseId.value })
}

function goCreateCase() {
  router.push({ path: '/case/create', query: { from: 'ai_wizard' } })
}

function goDashboardIntake() {
  router.push({ path: '/dashboard' })
}

function resetWizard() {
  step.value = 0
  selectedFile.value = null
  recognition.value = null
  intakeResult.value = null
  resultError.value = ''
  resultErrorDetail.value = ''
  progressStop()
  applyRouteDefaults()
}
</script>

<style scoped>
.ai-unified-wizard {
  margin-bottom: 20px;
}
.wizard-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}
.wizard-steps {
  margin: 16px 0 24px;
}
.wizard-body {
  text-align: center;
}
.upload-icon {
  font-size: 40px;
  color: var(--el-color-primary);
}
.file-name,
.case-hint {
  margin: 12px 0;
  color: #606266;
  font-size: 13px;
}
.skip-upload {
  display: block;
  margin: 8px auto 0;
}
.intent-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  margin: 0 auto;
  max-width: 420px;
}
.wizard-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
.error-detail {
  margin: 8px 0 0;
  white-space: pre-wrap;
  font-size: 12px;
  text-align: left;
}
</style>
