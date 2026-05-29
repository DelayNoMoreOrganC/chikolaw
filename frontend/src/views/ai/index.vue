<template>
  <div class="ai-hub">
    <PageHeader title="AI智能助手中心" />

    <AiHealthStrip />

    <AiUnifiedWizard
      :initial-intent="wizardIntent"
      :case-id="wizardCaseId"
      @doc-gen="onWizardDocGen"
    />

    <div class="ai-container">
      <!-- 功能导航卡片 -->
      <el-row :gutter="20" class="feature-cards">
        <el-col :xs="24" :sm="12" :md="6" v-for="feature in aiFeatures" :key="feature.id">
          <el-card
            class="feature-card"
            :class="{ active: activeFeature === feature.id }"
            @click="selectFeature(feature.id)"
            shadow="hover"
          >
            <div class="card-icon">{{ feature.icon }}</div>
            <h3>{{ feature.name }}</h3>
            <p>{{ feature.description }}</p>
            <div class="card-badge" v-if="feature.beta">BETA</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 功能区域 -->
      <div class="feature-section">
        <!-- 文书智能识别（统一走顶部向导，避免重复上传） -->
        <div v-show="activeFeature === 'recognition'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>📄 {{ AI_RECOGNITION.featureName }}</span>
                <el-tag type="success">请使用页顶「智能文书向导」</el-tag>
              </div>
            </template>
            <el-alert type="info" show-icon :closable="false" title="识别与卷宗挂接已整合到页顶三步向导">
              <p>支持：归入卷宗、预填建案、创建待办/日程、仅识别要素。从案件文档 Tab 或工作台进入时会自动带上案件上下文。</p>
            </el-alert>
          </el-card>
        </div>

        <!-- 文书生成 -->
        <div v-show="activeFeature === 'docGen'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>📝 {{ AI_DOCUMENT_GEN.featureName }}</span>
                <el-tag type="warning">智能模板</el-tag>
              </div>
            </template>

            <div class="doc-gen-section">
              <AiProgressBanner
                :active="docProgressActive"
                :title="docProgressTitle"
                :hint="docProgressHint"
                :elapsed-sec="docProgressElapsed"
              />
              <el-form :model="docForm" label-width="120px">
                <el-form-item label="关联案件" required>
                  <el-select
                    v-model="docForm.caseId"
                    filterable
                    placeholder="请选择要生成文书的案件"
                    style="width: 100%"
                    :loading="casesLoading"
                  >
                    <el-option
                      v-for="c in caseOptions"
                      :key="c.id"
                      :label="`${c.caseName || '未命名'} (#${c.id})`"
                      :value="c.id"
                    />
                  </el-select>
                  <p v-if="wizardCaseId" class="form-hint">已从案件页带入案件 ID：{{ wizardCaseId }}</p>
                </el-form-item>
                <el-form-item label="文书类型">
                  <el-select v-model="docForm.templateType" placeholder="选择文书模板">
                    <el-option
                      v-for="opt in documentTypeOptions"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="案件类型">
                  <el-select v-model="docForm.caseType" placeholder="选择案件类型">
                    <el-option label="民事纠纷" value="civil" />
                    <el-option label="商事纠纷" value="commercial" />
                    <el-option label="刑事案件" value="criminal" />
                    <el-option label="行政案件" value="administrative" />
                  </el-select>
                </el-form-item>

                <el-form-item label="关键信息">
                  <el-input
                    v-model="docForm.keyInfo"
                    type="textarea"
                    :rows="4"
                    placeholder="输入案件关键信息，AI将自动生成文书..."
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="generateDocument" :loading="isGenerating">
                    <el-icon><Document /></el-icon>
                    生成文书
                  </el-button>
                  <el-button @click="previewDocument" :disabled="!docForm.keyInfo">
                    <el-icon><View /></el-icon>
                    预览
                  </el-button>
                </el-form-item>
              </el-form>

              <!-- 生成结果 -->
              <div v-if="generatedDoc" class="generated-doc">
                <h4>生成结果</h4>
                <el-input
                  v-model="generatedDoc"
                  type="textarea"
                  :rows="15"
                  readonly
                />
                <div class="doc-actions">
                  <el-button type="success" @click="downloadDoc">下载 TXT</el-button>
                  <el-button type="primary" @click="handleExportDocx" :loading="docxDownloading">下载 Word</el-button>
                  <el-button @click="copyDoc">复制内容</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 法律问答 -->
        <div v-show="activeFeature === 'qa'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>🤖 AI法律问答</span>
                <el-tag type="info">基于RAG检索</el-tag>
              </div>
            </template>

            <div class="qa-section">
              <AiProgressBanner
                :active="qaProgressActive"
                :title="qaProgressTitle"
                :hint="qaProgressHint"
                :elapsed-sec="qaProgressElapsed"
              />
              <!-- 对话历史 -->
              <div class="chat-history" ref="chatHistoryRef">
                <div
                  v-for="(message, index) in chatMessages"
                  :key="index"
                  class="message-item"
                  :class="message.role"
                >
                  <div class="message-avatar">
                    <el-avatar v-if="message.role === 'user'" :size="32">
                      {{ userName?.charAt(0) }}
                    </el-avatar>
                    <el-icon v-else class="ai-avatar" :size="32"><ChatDotRound /></el-icon>
                  </div>
                  <div class="message-content">
                    <div v-if="message.pending" class="message-pending">
                      <el-icon class="is-loading"><Loading /></el-icon>
                      AI 思考中，预计 20 秒至 1 分钟…
                    </div>
                    <div v-else class="message-text" v-html="formatMessage(message.content)"></div>
                    <div v-if="!message.pending" class="message-time">{{ message.time }}</div>
                  </div>
                </div>
              </div>

              <!-- 输入区域 -->
              <div class="chat-input">
                <el-input
                  v-model="currentMessage"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入您的法律问题..."
                  @keydown.enter.ctrl="sendMessage"
                />
                <div class="input-actions">
                  <el-button
                    type="primary"
                    @click="sendMessage"
                    :loading="isSending"
                    :disabled="!currentMessage.trim()"
                  >
                    <el-icon><Promotion /></el-icon>
                    发送 (Ctrl+Enter)
                  </el-button>
                  <el-button @click="clearChat">
                    <el-icon><Delete /></el-icon>
                    清空对话
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 使用统计 -->
        <div v-show="activeFeature === 'stats'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>📊 AI使用统计</span>
                <el-tag>本月数据</el-tag>
              </div>
            </template>

            <div class="stats-section">
              <!-- 统计卡片 -->
              <el-row :gutter="20" class="stats-cards">
                <el-col :xs="12" :sm="6">
                  <div class="stat-card">
                    <div class="stat-icon">📄</div>
                    <div class="stat-info">
                      <div class="stat-value">{{ stats.totalDocs }}</div>
                      <div class="stat-label">文书识别</div>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="12" :sm="6">
                  <div class="stat-card">
                    <div class="stat-icon">📝</div>
                    <div class="stat-info">
                      <div class="stat-value">{{ stats.totalGenerated }}</div>
                      <div class="stat-label">文书生成</div>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="12" :sm="6">
                  <div class="stat-card">
                    <div class="stat-icon">💬</div>
                    <div class="stat-info">
                      <div class="stat-value">{{ stats.totalQueries }}</div>
                      <div class="stat-label">AI问答</div>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="12" :sm="6">
                  <div class="stat-card">
                    <div class="stat-icon">⚡</div>
                    <div class="stat-info">
                      <div class="stat-value">{{ stats.totalTokens.toLocaleString() }}</div>
                      <div class="stat-label">Token消耗</div>
                    </div>
                  </div>
                </el-col>
              </el-row>

              <!-- 详细统计表格 -->
              <el-table :data="usageLogs" stripe style="width: 100%; margin-top: 20px;">
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column prop="functionType" label="功能类型" width="120">
                  <template #default="{ row }">
                    <el-tag
                      v-if="row.functionType === 'OCR_RECOGNITION' || row.functionType === 'OCR' || row.functionType === 'DOCUMENT_RECOGNITION'"
                      type="success"
                    >
                      {{ formatAiFunctionType(row.functionType) }}
                    </el-tag>
                    <el-tag v-else-if="row.functionType === 'DOCUMENT_GENERATION' || row.functionType === 'DOC_GEN'" type="warning">
                      文书生成
                    </el-tag>
                    <el-tag
                      v-else-if="row.functionType === 'LEGAL_QA' || row.functionType === 'QA' || row.functionType === 'LEGAL_CHAT'"
                      type="info"
                    >
                      AI问答
                    </el-tag>
                    <el-tag v-else type="">{{ row.functionType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="inputTokens" label="输入Token" width="120" />
                <el-table-column prop="outputTokens" label="输出Token" width="120" />
                <el-table-column prop="duration" label="耗时(秒)" width="100" />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                      {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="docPreviewVisible"
      :title="docPreviewTitle"
      width="720px"
      destroy-on-close
    >
      <pre class="doc-preview-body">{{ docPreviewContent }}</pre>
      <template #footer>
        <el-button @click="docPreviewVisible = false">关闭</el-button>
        <el-button v-if="generatedDoc" type="primary" @click="downloadDoc">下载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import {
  UploadFilled,
  Document,
  View,
  ChatDotRound,
  Promotion,
  Delete,
  Loading
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AiHealthStrip from '@/components/AiHealthStrip.vue'
import AiProgressBanner from '@/components/AiProgressBanner.vue'
import AiUnifiedWizard from '@/components/AiUnifiedWizard.vue'
import * as aiApi from '@/api/ai'
import { getCaseList } from '@/api/case'
import { useUserStore } from '@/stores'
import { useDocumentExport } from '@/composables/useDocumentExport'
import { useAiProgress, AI_PROGRESS_HINTS } from '@/composables/useAiProgress'
import { notifyAiError } from '@/utils/aiError'
import {
  AI_RECOGNITION,
  AI_DOCUMENT_GEN,
  getDocumentTypeOptions,
  formatAiFunctionType
} from '@/config/ai-terminology'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { exporting: docxDownloading, downloadDocx, downloadTxt } = useDocumentExport()
const {
  active: docProgressActive,
  title: docProgressTitle,
  hint: docProgressHint,
  elapsedSec: docProgressElapsed,
  start: docProgressStart,
  stop: docProgressStop
} = useAiProgress()
const {
  active: qaProgressActive,
  title: qaProgressTitle,
  hint: qaProgressHint,
  elapsedSec: qaProgressElapsed,
  start: qaProgressStart,
  stop: qaProgressStop
} = useAiProgress()

const wizardIntent = computed(() => route.query.intent || '')
const wizardCaseId = computed(() => route.query.caseId || null)
const userName = computed(() => userStore.userInfo?.realName || '用户')

const CASE_CREATE_PREFILL_KEY = 'case_create_prefill'

// AI功能列表
const documentTypeOptions = getDocumentTypeOptions(true)

const aiFeatures = ref([
  {
    id: 'recognition',
    name: AI_RECOGNITION.featureName,
    description: 'Vision 识别 + LLM 要素提取（局域网，无需外网 OCR 服务）',
    icon: '📄',
    beta: false
  },
  {
    id: 'docGen',
    name: AI_DOCUMENT_GEN.featureName,
    description: '根据案件信息自动生成各类法律文书',
    icon: '📝',
    beta: false
  },
  {
    id: 'qa',
    name: 'AI法律问答',
    description: '基于知识库的智能法律咨询',
    icon: '🤖',
    beta: true
  },
  {
    id: 'stats',
    name: '使用统计',
    description: '查看AI功能使用情况和Token消耗',
    icon: '📊',
    beta: false
  }
])

// 当前激活的功能
const activeFeature = ref('recognition')

// 文书生成相关
const docForm = reactive({
  caseId: null,
  templateType: '',
  caseType: '',
  keyInfo: ''
})
const caseOptions = ref([])
const casesLoading = ref(false)
const isGenerating = ref(false)
const generatedDoc = ref('')
const docPreviewVisible = ref(false)
const docPreviewContent = ref('')
const docPreviewTitle = ref('文书预览')

const CASE_TYPE_LABELS = {
  civil: '民事纠纷',
  commercial: '商事纠纷',
  criminal: '刑事案件',
  administrative: '行政案件'
}

// 问答相关
const chatMessages = ref([])
const currentMessage = ref('')
const isSending = ref(false)
const chatHistoryRef = ref(null)

// 统计相关
const stats = reactive({
  totalDocs: 0,
  totalGenerated: 0,
  totalQueries: 0,
  totalTokens: 0
})
const usageLogs = ref([])

// 方法
const selectFeature = (featureId) => {
  activeFeature.value = featureId
  if (featureId === 'stats') {
    loadStats()
  }
}

function resolveActiveFeatureFromRoute() {
  const intent = route.query.intent
  if (intent === 'docGen') return 'docGen'
  if (intent === 'recognize' || intent === 'intake' || intent === 'prefill' || intent === 'todo') {
    return 'recognition'
  }
  if (intent === 'qa') return 'qa'
  return activeFeature.value
}

function onWizardDocGen() {
  activeFeature.value = 'docGen'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadCaseOptions() {
  casesLoading.value = true
  try {
    const listRes = await getCaseList({ page: 1, size: 50 })
    const records = listRes.data?.records || listRes.data?.list || []
    caseOptions.value = records
    if (wizardCaseId.value && !docForm.caseId) {
      const id = Number(wizardCaseId.value)
      if (caseOptions.value.some((c) => c.id === id)) {
        docForm.caseId = id
      }
    }
  } catch (e) {
    console.warn('加载案件列表失败', e)
  } finally {
    casesLoading.value = false
  }
}

// 文书生成方法
const generateDocument = async () => {
  if (!docForm.caseId || !docForm.templateType || !docForm.caseType || !docForm.keyInfo?.trim()) {
    ElMessage.warning('请选择关联案件并填写完整的文书信息')
    return
  }

  isGenerating.value = true
  docProgressStart({ title: 'AI 文书生成', hint: AI_PROGRESS_HINTS.docGen })
  try {
    const caseLabel = CASE_TYPE_LABELS[docForm.caseType] || docForm.caseType
    const response = await aiApi.generateDoc({
      caseId: docForm.caseId,
      documentType: docForm.templateType,
      additionalContext: `案件类型：${caseLabel}\n\n关键信息：\n${docForm.keyInfo.trim()}`
    })

    if (response.code === 200 || response.success) {
      generatedDoc.value = response.data
      ElNotification.success({
        title: '生成成功',
        message: '文书已生成，您可以预览或下载'
      })
    } else {
      throw new Error(response.message || '生成失败')
    }
  } catch (error) {
    notifyAiError(error, { fallback: '文书生成失败' })
  } finally {
    isGenerating.value = false
    docProgressStop()
  }
}

const buildDocDraftPreview = () => {
  const typeLabel =
    documentTypeOptions.find((o) => o.value === docForm.templateType)?.label ||
    docForm.templateType ||
    '法律文书'
  const caseLabel = CASE_TYPE_LABELS[docForm.caseType] || docForm.caseType || '未选择'
  return [
    `${typeLabel}`,
    `案件类型：${caseLabel}`,
    '',
    '—— 关键信息 ——',
    '',
    docForm.keyInfo.trim()
  ].join('\n')
}

const previewDocument = () => {
  if (generatedDoc.value) {
    docPreviewTitle.value = '文书预览（已生成）'
    docPreviewContent.value = generatedDoc.value
    docPreviewVisible.value = true
    return
  }
  if (!docForm.keyInfo?.trim()) {
    ElMessage.warning('请先填写关键信息，或先生成文书')
    return
  }
  docPreviewTitle.value = '文书预览（草稿）'
  docPreviewContent.value = buildDocDraftPreview()
  docPreviewVisible.value = true
}

const downloadDoc = () => {
  if (downloadTxt(generatedDoc.value, `法律文书_${Date.now()}.txt`)) {
    ElMessage.success('TXT 已下载')
  }
}

const handleExportDocx = async () => {
  const typeLabel =
    documentTypeOptions.find((o) => o.value === docForm.templateType)?.label || '法律文书'
  await downloadDocx({
    content: generatedDoc.value,
    title: typeLabel,
    fileName: `${typeLabel}_${Date.now()}.docx`
  })
}

const copyDoc = () => {
  navigator.clipboard.writeText(generatedDoc.value)
  ElMessage.success('内容已复制到剪贴板')
}

// 问答方法
const sendMessage = async () => {
  if (!currentMessage.value.trim()) return

  const userMessage = {
    role: 'user',
    content: currentMessage.value,
    time: new Date().toLocaleTimeString()
  }

  chatMessages.value.push(userMessage)
  const userInput = currentMessage.value
  currentMessage.value = ''

  await nextTick()
  scrollToBottom()

  const pendingIdx = chatMessages.value.length
  chatMessages.value.push({
    role: 'assistant',
    content: '',
    pending: true,
    time: new Date().toLocaleTimeString()
  })

  isSending.value = true
  qaProgressStart({ title: 'AI 法律问答', hint: AI_PROGRESS_HINTS.qa })
  try {
    const response = await aiApi.aiChat({ message: userInput })

    chatMessages.value[pendingIdx] = {
      role: 'assistant',
      content: response.data || response.message || '抱歉，AI服务暂时不可用',
      pending: false,
      time: new Date().toLocaleTimeString()
    }

    await nextTick()
    scrollToBottom()
  } catch (error) {
    chatMessages.value.splice(pendingIdx, 1)
    notifyAiError(error, { fallback: 'AI 对话失败' })
  } finally {
    isSending.value = false
    qaProgressStop()
  }
}

const clearChat = () => {
  chatMessages.value = []
  ElMessage.success('对话已清空')
}

const formatMessage = (content) => {
  // 简单的格式化：换行符转<br>
  return content.replace(/\n/g, '<br>')
}

const scrollToBottom = () => {
  if (chatHistoryRef.value) {
    chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
  }
}

// 统计方法
const loadStats = async () => {
  try {
    const response = await aiApi.getAiLogs({ page: 1, size: 20 })
    if (response.success) {
      const logs = response.data?.content || response.data?.records || []

      const isRecognition = (t) =>
        t === 'OCR_RECOGNITION' || t === 'OCR' || t === 'DOCUMENT_RECOGNITION'
      const isDocGen = (t) => t === 'DOCUMENT_GENERATION' || t === 'DOC_GEN'
      const isQa = (t) =>
        t === 'LEGAL_QA' || t === 'QA' || t === 'LEGAL_CHAT' || t === 'GENERAL_CHAT' || t === 'RAG'

      stats.totalDocs = logs.filter((l) => isRecognition(l.functionType)).length
      stats.totalGenerated = logs.filter((l) => isDocGen(l.functionType)).length
      stats.totalQueries = logs.filter((l) => isQa(l.functionType)).length
      stats.totalTokens = logs.reduce((sum, log) => sum + (log.inputTokens || 0) + (log.outputTokens || 0), 0)

      usageLogs.value = logs.map(log => ({
        date: new Date(log.createdAt).toLocaleDateString(),
        functionType: log.functionType,
        inputTokens: log.inputTokens || 0,
        outputTokens: log.outputTokens || 0,
        duration: log.duration || 0,
        status: log.status
      }))
    }
  } catch (error) {
    console.warn('加载统计数据失败', error)
  }
}

// 生命周期
onMounted(() => {
  activeFeature.value = resolveActiveFeatureFromRoute()
  if (wizardCaseId.value) {
    docForm.caseId = Number(wizardCaseId.value) || wizardCaseId.value
  }
  loadCaseOptions()
  loadStats()
})
</script>

<style scoped>
.ai-hub {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

.feature-cards {
  flex-shrink: 0;
}

.feature-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  border: 2px solid transparent;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.feature-card.active {
  border-color: #409EFF;
  background-color: #f0f7ff;
}

.card-icon {
  font-size: 36px;
  margin-bottom: 10px;
}

.card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #E6A23C;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.feature-section {
  flex: 1;
  overflow: hidden;
}

.feature-panel {
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

/* OCR样式 */
.upload-area {
  margin: 20px 0;
}

.upload-content {
  text-align: center;
  padding: 40px 20px;
}

.upload-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 10px;
}

.upload-text {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.upload-hint {
  font-size: 12px;
  color: #999;
}

.ocr-result {
  margin-top: 20px;
}

.result-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
}

/* 问答样式 */
.qa-section {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 10px;
}

.ai-avatar {
  color: #409EFF;
}

.message-content {
  max-width: 70%;
}

.message-text {
  background: white;
  padding: 10px 15px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.message-pending {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 10px 15px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.form-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.message-item.user .message-text {
  background: #409EFF;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.chat-input {
  flex-shrink: 0;
}

.input-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 统计样式 */
.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #eef3fc 0%, #dce8f8 100%);
  color: var(--lawos-text, #1c1c1e);
  border: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.08));
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.stat-icon {
  font-size: 32px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* 文书生成样式 */
.generated-doc {
  margin-top: 20px;
}

.doc-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.doc-preview-body {
  margin: 0;
  padding: 12px;
  max-height: 60vh;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  background: #f5f7fa;
  border-radius: 6px;
}
</style>