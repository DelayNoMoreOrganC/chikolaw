<template>
  <div class="ai-hub">
    <PageHeader title="AI智能助手中心" />

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
        <!-- OCR文档识别 -->
        <div v-show="activeFeature === 'ocr'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>📄 OCR文档智能识别</span>
                <el-tag type="success">支持PDF/图片</el-tag>
              </div>
            </template>

            <div class="ocr-section">
              <!-- 上传区域 -->
              <div
                class="upload-area"
                :class="{ 'drag-over': isDragOver, 'uploading': isUploading }"
                @drop.prevent="handleDrop"
                @dragover.prevent="isDragOver = true"
                @dragleave.prevent="isDragOver = false"
              >
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleFileChange"
                  accept=".pdf,.jpg,.jpeg,.png"
                  drag
                >
                  <div class="upload-content">
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <p class="upload-text">拖拽文件到此处或点击上传</p>
                    <p class="upload-hint">支持PDF、JPG、PNG格式，最大10MB</p>
                  </div>
                </el-upload>
              </div>

              <!-- 识别结果 -->
              <div v-if="ocrResult" class="ocr-result">
                <h4>识别结果</h4>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="文档类型">{{ ocrResult.documentType }}</el-descriptions-item>
                  <el-descriptions-item label="法院">{{ ocrResult.court }}</el-descriptions-item>
                  <el-descriptions-item label="案号">{{ ocrResult.caseNumber }}</el-descriptions-item>
                  <el-descriptions-item label="案件性质">{{ ocrResult.caseNature }}</el-descriptions-item>
                  <el-descriptions-item label="当事人" :span="2">{{ ocrResult.parties }}</el-descriptions-item>
                  <el-descriptions-item label="案由" :span="2">{{ ocrResult.caseCause }}</el-descriptions-item>
                </el-descriptions>

                <div class="result-actions">
                  <el-button type="primary" @click="createCaseFromOCR">创建案件</el-button>
                  <el-button @click="clearOCRResult">重新识别</el-button>
                </div>
              </div>

              <!-- 识别进度 -->
              <div v-if="isUploading" class="upload-progress">
                <el-progress :percentage="uploadProgress" :status="uploadStatus" />
                <p>{{ uploadStatusText }}</p>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 文书生成 -->
        <div v-show="activeFeature === 'docGen'" class="feature-panel">
          <el-card>
            <template #header>
              <div class="panel-header">
                <span>📝 AI文书生成</span>
                <el-tag type="warning">智能模板</el-tag>
              </div>
            </template>

            <div class="doc-gen-section">
              <el-form :model="docForm" label-width="120px">
                <el-form-item label="文书类型">
                  <el-select v-model="docForm.templateType" placeholder="选择文书模板">
                    <el-option label="起诉状" value="complaint" />
                    <el-option label="答辩状" value="defense" />
                    <el-option label="代理词" value="opinion" />
                    <el-option label="律师函" value="letter" />
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
                  <el-button type="success" @click="downloadDoc">下载文档</el-button>
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
                    <div class="message-text" v-html="formatMessage(message.content)"></div>
                    <div class="message-time">{{ message.time }}</div>
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
                      <div class="stat-label">OCR识别</div>
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
                    <el-tag v-if="row.functionType === 'OCR'" type="success">OCR识别</el-tag>
                    <el-tag v-else-if="row.functionType === 'DOC_GEN'" type="warning">文书生成</el-tag>
                    <el-tag v-else-if="row.functionType === 'QA'" type="info">AI问答</el-tag>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import {
  UploadFilled,
  Document,
  View,
  ChatDotRound,
  Promotion,
  Delete
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import * as aiApi from '@/api/ai'
import { useUserStore } from '@/stores'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || '用户')

// AI功能列表
const aiFeatures = ref([
  {
    id: 'ocr',
    name: 'OCR文档识别',
    description: '智能识别法院文书、合同等法律文档',
    icon: '📄',
    beta: false
  },
  {
    id: 'docGen',
    name: 'AI文书生成',
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
const activeFeature = ref('ocr')

// OCR相关
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')
const uploadStatusText = ref('')
const ocrResult = ref(null)

// 文书生成相关
const docForm = reactive({
  templateType: '',
  caseType: '',
  keyInfo: ''
})
const isGenerating = ref(false)
const generatedDoc = ref('')

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

// OCR相关方法
const handleDrop = (e) => {
  isDragOver.value = false
  const files = e.dataTransfer.files
  if (files.length > 0) {
    processFile(files[0])
  }
}

const handleFileChange = (file) => {
  if (file.raw) {
    processFile(file.raw)
  }
}

const processFile = async (file) => {
  // 验证文件类型
  const validTypes = ['application/pdf', 'image/jpeg', 'image/png']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持PDF、JPG、PNG格式文件')
    return
  }

  // 验证文件大小（10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }

  isUploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadStatusText.value = '正在上传文件...'

  try {
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += 10
      }
    }, 200)

    const response = await aiApi.recognizeLegalDocument(file)

    clearInterval(progressInterval)
    uploadProgress.value = 100
    uploadStatus.value = 'success'
    uploadStatusText.value = '识别完成！'

    if (response.success) {
      ocrResult.value = response.data
      ElNotification.success({
        title: '识别成功',
        message: '文档识别完成，请查看结果'
      })
    } else {
      throw new Error(response.message || '识别失败')
    }
  } catch (error) {
    uploadStatus.value = 'exception'
    uploadStatusText.value = '识别失败：' + error.message
    ElMessage.error('OCR识别失败：' + error.message)
  } finally {
    isUploading.value = false
  }
}

const createCaseFromOCR = () => {
  // 跳转到案件创建页面并预填充数据
  ElMessage.info('即将跳转到案件创建页面...')
  // TODO: 实现跳转逻辑
}

const clearOCRResult = () => {
  ocrResult.value = null
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadStatusText.value = ''
}

// 文书生成方法
const generateDocument = async () => {
  if (!docForm.templateType || !docForm.caseType || !docForm.keyInfo) {
    ElMessage.warning('请填写完整的文书信息')
    return
  }

  isGenerating.value = true
  try {
    const response = await aiApi.generateDoc({
      templateType: docForm.templateType,
      caseType: docForm.caseType,
      keyInfo: docForm.keyInfo
    })

    if (response.success) {
      generatedDoc.value = response.data
      ElNotification.success({
        title: '生成成功',
        message: '文书已生成，您可以预览或下载'
      })
    } else {
      throw new Error(response.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('文书生成失败：' + error.message)
  } finally {
    isGenerating.value = false
  }
}

const previewDocument = () => {
  ElMessage.info('预览功能开发中...')
}

const downloadDoc = () => {
  // 下载生成的文档
  const blob = new Blob([generatedDoc.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `法律文书_${new Date().getTime()}.txt`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('文档下载成功')
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

  isSending.value = true
  try {
    const response = await aiApi.aiChat({ message: userInput })

    const aiMessage = {
      role: 'assistant',
      content: response.data || response.message || '抱歉，AI服务暂时不可用',
      time: new Date().toLocaleTimeString()
    }

    chatMessages.value.push(aiMessage)

    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('AI对话失败：' + error.message)
  } finally {
    isSending.value = false
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
    const response = await aiApi.getAiLogs({ page: 0, size: 20 })
    if (response.success) {
      const logs = response.data.records || []

      // 计算统计数据
      stats.totalDocs = logs.filter(l => l.functionType === 'OCR').length
      stats.totalGenerated = logs.filter(l => l.functionType === 'DOC_GEN').length
      stats.totalQueries = logs.filter(l => l.functionType === 'QA').length
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
    ElMessage.error('加载统计数据失败：' + error.message)
  }
}

// 生命周期
onMounted(() => {
  // 默认加载统计数据
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
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
</style>