<template>
  <div class="case-file-intake-panel">
    <div class="panel-header">
      <div class="title-block">
        <h2>卷宗智能录入</h2>
        <p class="subtitle">传入文件 → AI 分析 → 登记备注 → 归入已有案件卷宗（不自动建案）</p>
      </div>
      <el-tag :type="agentTagType" size="small">{{ agentStatusText }}</el-tag>
    </div>

    <el-row :gutter="16" class="panel-body">
      <el-col :xs="24" :md="14">
        <el-upload
          class="intake-upload"
          drag
          :show-file-list="false"
          :http-request="handleUpload"
          :disabled="processing"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽任意文书到此处，或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 PDF、图片、Word(docx)、TXT 等（≤50MB）。引擎：内置 AI / 本机 OpenClaw / Hermes（按后端配置自动选择）
            </div>
          </template>
        </el-upload>
        <div v-if="processing" class="processing-row">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在分析，请稍候…</span>
        </div>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-form label-position="top" class="intake-form">
          <el-form-item label="关联案件（建议先选或依赖案号自动匹配）">
            <el-select
              v-model="selectedCaseId"
              filterable
              remote
              clearable
              placeholder="搜索案号/案件名称"
              :remote-method="remoteSearchCases"
              :loading="caseSearchLoading"
              style="width: 100%"
            >
              <el-option
                v-for="c in caseOptions"
                :key="c.id"
                :label="`${c.caseNumber || '无案号'} - ${c.caseName}`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="登记备注">
            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              placeholder="如：客户送来传票原件、需3日内答辩等"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>

    <el-alert
      v-if="lastResult && lastResult.status === 'NEEDS_CASE'"
      type="warning"
      :closable="false"
      class="result-alert"
      show-icon
    >
      <template #title>未匹配到案件</template>
      <p class="needs-case-msg">{{ lastResult.message }}</p>
      <div class="needs-case-actions">
        <el-button
          type="primary"
          :loading="processing"
          :disabled="!selectedCaseId || (!lastResult?.pendingId && !pendingFile)"
          @click="confirmAttach"
        >
          确认归档到所选案件
        </el-button>
        <el-button :disabled="!lastResult?.pendingId" @click="openFilingDialog">发起立案申请</el-button>
        <el-button @click="goApproval">查看审批</el-button>
      </div>
    </el-alert>

    <el-card v-if="lastResult && lastResult.status === 'SUCCESS'" class="result-card" shadow="never">
      <template #header>
        <span>已归入卷宗</span>
      </template>
      <div class="result-grid">
        <div><span class="label">案件：</span>{{ lastResult.caseName }}</div>
        <div><span class="label">案号：</span>{{ lastResult.caseNumber || '—' }}</div>
        <div><span class="label">文件夹：</span>{{ lastResult.folderPath }}</div>
        <div><span class="label">分析引擎：</span>{{ lastResult.analysisProvider || 'builtin' }}</div>
      </div>
      <p class="registration-note">{{ lastResult.registrationNote }}</p>
      <div class="result-actions">
        <el-button type="primary" @click="goCaseDoc(lastResult.caseId)">打开案件卷宗</el-button>
        <el-button @click="goCaseDetail(lastResult.caseId)">案件详情</el-button>
      </div>
    </el-card>

    <el-dialog v-model="filingDialogVisible" title="立案申请" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="申请标题">
          <el-input v-model="filingTitle" placeholder="如：立案申请 - 传票原件" maxlength="100" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="filingContent"
            type="textarea"
            :rows="4"
            placeholder="补充案情、当事人、案由等（可选，系统会附带 AI 识别摘要）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          审批通过后将提示前往「新建案件」（自动预填识别信息并挂接卷宗）；暂存编号 {{ lastResult?.pendingId }}。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="filingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="filingSubmitting" @click="submitFilingApplication">
          提交审批
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading } from '@element-plus/icons-vue'
import {
  processCaseIntake,
  attachCaseIntake,
  attachCaseIntakeFromPending,
  createCaseFilingApplication,
  searchCasesForIntake
} from '@/api/caseIntake'
import { getAgentRuntimeStatus } from '@/api/agent'

const router = useRouter()

const processing = ref(false)
const remark = ref('')
const selectedCaseId = ref(null)
const lastResult = ref(null)
const pendingFile = ref(null)
const caseOptions = ref([])
const caseSearchLoading = ref(false)
const agentRuntime = ref(null)
const filingDialogVisible = ref(false)
const filingTitle = ref('')
const filingContent = ref('')
const filingSubmitting = ref(false)

const agentStatusText = computed(() => {
  const r = agentRuntime.value
  if (!r) return 'Agent 检测中…'
  const active = r.activeProvider || 'builtin'
  const labels = { builtin: '内置 AI', openclaw: 'OpenClaw', hermes: 'Hermes' }
  return `当前：${labels[active] || active}`
})

const agentTagType = computed(() => {
  const r = agentRuntime.value
  if (!r) return 'info'
  if (r.activeProvider === 'openclaw' && !r.openclawReachable) return 'warning'
  if (r.activeProvider === 'hermes' && r.hermesEnabled && !(r.hermes?.reachable)) return 'warning'
  return 'success'
})

onMounted(async () => {
  try {
    const res = await getAgentRuntimeStatus()
    if (res.code === 200 || res.success) {
      agentRuntime.value = res.data
    }
  } catch {
    agentRuntime.value = { activeProvider: 'builtin' }
  }
  remoteSearchCases('')
})

const remoteSearchCases = async (q) => {
  caseSearchLoading.value = true
  try {
    const res = await searchCasesForIntake(q, 20)
    if (res.code === 200 || res.success) {
      caseOptions.value = res.data || []
      if (lastResult.value?.caseCandidates?.length) {
        const merged = [...(res.data || [])]
        lastResult.value.caseCandidates.forEach((c) => {
          if (!merged.find((m) => m.id === c.id)) merged.push(c)
        })
        caseOptions.value = merged
      }
    }
  } finally {
    caseSearchLoading.value = false
  }
}

const handleUpload = async ({ file }) => {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件不能超过 50MB')
    return
  }
  pendingFile.value = file
  processing.value = true
  lastResult.value = null
  try {
    const res = await processCaseIntake(file, {
      caseId: selectedCaseId.value,
      remark: remark.value
    })
    if (res.code === 200 || res.success) {
      lastResult.value = res.data
      if (res.data?.caseId) {
        selectedCaseId.value = res.data.caseId
      }
      if (res.data?.status === 'SUCCESS') {
        ElMessage.success(res.data.message || '已归入案件卷宗')
        pendingFile.value = null
      } else if (res.data?.status === 'NEEDS_CASE') {
        if (res.data.caseCandidates?.length) {
          caseOptions.value = res.data.caseCandidates
        }
        ElMessage.warning('未匹配到案件，请选择案件或发起立案申请')
      }
    } else {
      ElMessage.error(res.message || '录入失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '录入失败')
  } finally {
    processing.value = false
  }
}

const confirmAttach = async () => {
  if (!selectedCaseId.value) return
  const pendingId = lastResult.value?.pendingId
  if (!pendingId && !pendingFile.value) return
  processing.value = true
  try {
    const res = pendingId
      ? await attachCaseIntakeFromPending(pendingId, selectedCaseId.value, remark.value)
      : await attachCaseIntake(pendingFile.value, selectedCaseId.value, remark.value)
    if (res.code === 200 || res.success) {
      lastResult.value = res.data
      ElMessage.success('已归档到案件卷宗')
      pendingFile.value = null
    } else {
      ElMessage.error(res.message || '归档失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '归档失败')
  } finally {
    processing.value = false
  }
}

const buildFilingDefaults = () => {
  const rec = lastResult.value?.recognition
  const label = rec?.caseReason
    || [rec?.plaintiffName, rec?.defendantName].filter(Boolean).join(' 诉 ')
    || ''
  filingTitle.value = label
    ? `立案申请 - ${label}`
    : `立案申请 - 卷宗暂存 #${lastResult.value?.pendingId || ''}`
  const lines = []
  if (rec?.caseNumber) lines.push(`识别案号：${rec.caseNumber}`)
  if (rec?.caseReason) lines.push(`案由：${rec.caseReason}`)
  if (rec?.plaintiffName || rec?.defendantName) {
    lines.push(`当事人：${rec?.plaintiffName || '—'} / ${rec?.defendantName || '—'}`)
  }
  if (rec?.documentType) lines.push(`文书类型：${rec.documentType}`)
  if (lastResult.value?.message) lines.push(lastResult.value.message)
  filingContent.value = lines.join('\n')
}

const openFilingDialog = () => {
  if (!lastResult.value?.pendingId) {
    ElMessage.warning('请先完成文件分析以生成暂存记录')
    return
  }
  buildFilingDefaults()
  filingDialogVisible.value = true
}

const submitFilingApplication = async () => {
  const pendingId = lastResult.value?.pendingId
  if (!pendingId) return
  filingSubmitting.value = true
  try {
    const res = await createCaseFilingApplication(pendingId, {
      title: filingTitle.value,
      content: filingContent.value
    })
    if (res.code === 200 || res.success) {
      ElMessage.success('立案申请已提交，请在审批中心跟进')
      filingDialogVisible.value = false
      router.push('/approval')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    filingSubmitting.value = false
  }
}

const goCaseDoc = (caseId) => router.push(`/case/${caseId}/doc`)
const goCaseDetail = (caseId) => router.push(`/case/${caseId}`)
const goApproval = () => router.push('/approval')
</script>

<style scoped>
.case-file-intake-panel {
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f0f7ff 0%, #fafbff 100%);
  border: 1px solid #d6e4ff;
  border-radius: 12px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.panel-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1d39c4;
}
.subtitle {
  margin: 0;
  font-size: 13px;
  color: #597ef7;
}
.intake-upload :deep(.el-upload-dragger) {
  padding: 28px 16px;
  background: #fff;
}
.upload-icon {
  font-size: 48px;
  color: #597ef7;
  margin-bottom: 8px;
}
.processing-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #597ef7;
}
.result-alert {
  margin-top: 16px;
}
.needs-case-msg {
  margin: 8px 0 12px;
  font-size: 14px;
  line-height: 1.6;
}
.needs-case-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.result-card {
  margin-top: 16px;
}
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  font-size: 14px;
}
.result-grid .label {
  color: #8c8c8c;
}
.registration-note {
  margin: 12px 0;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 6px;
  font-size: 13px;
  white-space: pre-wrap;
}
.result-actions {
  display: flex;
  gap: 8px;
}
</style>
