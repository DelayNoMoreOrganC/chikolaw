<template>
  <div class="dashboard">
    <!-- 核心：卷宗智能录入（传文件→AI分析→登记备注→案件档案夹） -->
    <CaseFileIntakePanel />

    <!-- 统计卡片区 -->
    <div class="stats-cards">
      <div
        v-for="stat in stats"
        :key="stat.key"
        class="stat-card"
        :class="`stat-${stat.type}`"
        @click="handleStatClick(stat)"
      >
        <div class="stat-icon">
          <el-icon><component :is="getIconName(stat.icon)" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value" :class="{ 'has-trend': stat.trend }">
            <span class="value-number">{{ stat.value }}</span>
            <span v-if="stat.trend" class="trend-indicator" :class="stat.trend > 0 ? 'trend-up' : 'trend-down'">
              {{ stat.trend > 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}%
            </span>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div v-if="stat.loading" class="stat-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
      </div>
    </div>

    <div class="dashboard-content">
      <!-- 日历视图区 -->
      <div class="calendar-section">
        <div class="section-header">
          <h3>日程安排</h3>
          <div class="header-actions">
            <el-radio-group v-model="calendarView" size="small">
              <el-radio-button value="month">月视图</el-radio-button>
              <el-radio-button value="week">周视图</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" @click="handleCreateEvent">
              <el-icon><Plus /></el-icon>
              新建日程
            </el-button>
          </div>
        </div>
        <div class="calendar-filters">
          <el-select v-model="calendarFilters.calendarType" placeholder="日程类型" clearable size="small" style="width: 120px">
            <el-option label="开庭" value="HEARING" />
            <el-option label="审限" value="DEADLINE" />
            <el-option label="立案" value="FILING" />
            <el-option label="调解" value="MEDIATION" />
            <el-option label="举证" value="EVIDENCE" />
          </el-select>
          <el-select v-model="calendarFilters.caseType" placeholder="案件类型" clearable size="small" style="width: 110px">
            <el-option label="民事" value="CIVIL" />
            <el-option label="刑事" value="CRIMINAL" />
            <el-option label="行政" value="ADMINISTRATIVE" />
            <el-option label="仲裁" value="ARBITRATION" />
            <el-option label="非诉" value="NON_LITIGATION" />
          </el-select>
          <el-select v-model="calendarFilters.caseStatus" placeholder="案件状态" clearable size="small" style="width: 110px">
            <el-option label="咨询中" value="CONSULTATION" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已结案" value="CLOSED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
          <el-select
            v-model="calendarFilters.ownerId"
            placeholder="主办律师"
            clearable
            filterable
            size="small"
            style="width: 130px"
          >
            <el-option
              v-for="o in calendarOwnerOptions"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
          <el-input
            v-model="calendarFilters.court"
            placeholder="法院关键词"
            clearable
            size="small"
            style="width: 140px"
          />
          <el-button size="small" @click="resetCalendarFilters">重置</el-button>
        </div>
        <div class="calendar-view">
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="calendar-day">
                <span class="date-number">{{ data.day.split('-')[2] }}</span>
                <div class="event-tags">
                  <el-tag
                    v-for="event in getEventsForDate(data.day)"
                    :key="event.id"
                    :type="event.type"
                    size="small"
                    class="event-tag"
                    @click="handleEventClick(event)"
                  >
                    {{ event.title }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-calendar>
        </div>
      </div>

      <!-- 待办事项区 -->
      <div class="todo-section">
        <!-- AI智能创建上传框 -->
        <div class="ai-upload-section">
          <div class="section-header">
            <h3>🤖 AI智能创建</h3>
            <el-tag size="small" type="success">支持创建待办/任务/日程/日志</el-tag>
          </div>
          <el-upload
            ref="uploadRef"
            class="upload-dragger"
            drag
            :http-request="handleCustomUpload"
            :show-file-list="false"
            accept=".pdf,.doc,.docx,.txt,.jpg,.png"
            :auto-upload="true"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、TXT、图片格式，AI将自动识别内容并创建待办事项、任务、日程或工作日志
              </div>
            </template>
          </el-upload>
          <div v-if="aiProcessing" class="ai-processing">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>AI正在分析文档，请稍候...</span>
          </div>
          <div v-if="aiResult" class="ai-result">
            <el-alert type="success" :closable="false" class="result-alert">
              <template #title>
                <div class="result-title">
                  <span>✅ {{ AI_RECOGNITION.successTitle }}（{{ aiResult.documentType || '未知文书' }}）</span>
                  <el-button text type="primary" size="small" @click="aiResult = null">关闭</el-button>
                </div>
              </template>
              <div class="result-content">
                <!-- 识别结果 -->
                <div class="result-section">
                  <h4>📄 识别信息</h4>
                  <div class="result-grid">
                    <div v-if="aiResult.caseNumber" class="result-item">
                      <span class="label">案号：</span>
                      <span class="value">{{ aiResult.caseNumber }}</span>
                    </div>
                    <div v-if="aiResult.courtName" class="result-item">
                      <span class="label">法院：</span>
                      <span class="value">{{ aiResult.courtName }}</span>
                    </div>
                    <div v-if="aiResult.plaintiffName" class="result-item">
                      <span class="label">原告：</span>
                      <span class="value">{{ aiResult.plaintiffName }}</span>
                    </div>
                    <div v-if="aiResult.defendantName" class="result-item">
                      <span class="label">被告：</span>
                      <span class="value">{{ aiResult.defendantName }}</span>
                    </div>
                    <div v-if="aiResult.caseReason" class="result-item">
                      <span class="label">案由：</span>
                      <span class="value">{{ aiResult.caseReason }}</span>
                    </div>
                    <div v-if="aiResult.judgmentDate" class="result-item">
                      <span class="label">判决日期：</span>
                      <span class="value">{{ aiResult.judgmentDate }}</span>
                    </div>
                    <div v-if="aiResult.hearingDate" class="result-item">
                      <span class="label">开庭时间：</span>
                      <span class="value">{{ aiResult.hearingDate }}</span>
                    </div>
                    <div v-if="aiResult.processingTime" class="result-item">
                      <span class="label">处理时间：</span>
                      <span class="value">{{ aiResult.processingTime }}ms</span>
                    </div>
                  </div>
                </div>

                <!-- 业务逻辑执行结果 -->
                <div class="result-section" v-if="aiResult.businessLogic">
                  <h4>🚀 自动执行</h4>
                  <div class="business-logic">
                    <el-tag v-if="aiResult.businessLogic.caseCreated" type="success" size="small">
                      ✅ 案件已创建
                    </el-tag>
                    <el-tag v-if="aiResult.businessLogic.todoCreated" type="success" size="small">
                      ✅ 待办已创建
                    </el-tag>
                    <el-tag v-if="aiResult.businessLogic.calendarCreated" type="success" size="small">
                      ✅ 日程已创建
                    </el-tag>
                    <el-tag v-if="aiResult.businessLogic.workReportCreated" type="success" size="small">
                      ✅ 工作日志已创建
                    </el-tag>
                  </div>
                  <div class="quick-links" v-if="aiResult.caseNumber">
                    <el-button type="primary" size="small" @click="goToCaseDetail(aiResult.caseId)">
                      查看案件详情
                    </el-button>
                    <el-button type="info" size="small" @click="fetchTodos()">
                      刷新待办列表
                    </el-button>
                  </div>
                </div>
              </div>
            </el-alert>
          </div>
        </div>

        <!-- 原待办事项列表 -->
        <div class="section-header">
          <h3>待办事项</h3>
          <el-button type="primary" size="small" @click="handleCreateTodo">
            <el-icon><Plus /></el-icon>
            新建待办
          </el-button>
        </div>
        <div class="todo-list">
          <div
            v-for="todo in sortedTodos"
            :key="todo.id"
            class="todo-item"
            :class="getTodoClass(todo)"
          >
            <div class="todo-left">
              <el-checkbox v-model="todo.completed" @change="handleTodoComplete(todo)" />
              <div class="todo-content">
                <div class="todo-title">{{ todo.title }}</div>
                <div class="todo-meta">
                  <PriorityDot :priority="todo.priority" />
                  <span class="todo-deadline">{{ todo.deadline }}</span>
                  <el-tag v-if="todo.caseName" size="small" type="info" @click="goToCaseDetail(todo.caseId)" class="clickable-tag">
                    {{ todo.caseName }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div class="todo-actions">
              <el-button text type="primary" size="small" @click="handleEditTodo(todo)">
                编辑
              </el-button>
              <el-button text type="danger" size="small" @click="handleDeleteTodo(todo)">
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div class="action-item" @click="handleQuickAction('createCase')">
        <el-icon :size="32" color="#1890ff"><FolderAdd /></el-icon>
        <span>新建案件</span>
      </div>
      <div class="action-item" @click="handleQuickAction('createClient')">
        <el-icon :size="32" color="#52c41a"><UserFilled /></el-icon>
        <span>新建客户</span>
      </div>
      <div class="action-item" @click="handleQuickAction('aiAssistant')">
        <el-icon :size="32" color="#722ed1"><MagicStick /></el-icon>
        <span>AI助手</span>
      </div>
      <div class="action-item" @click="handleQuickAction('uploadDoc')">
        <el-icon :size="32" color="#faad14"><UploadFilled /></el-icon>
        <span>上传文书</span>
      </div>
    </div>

    <!-- 案件详情浮层对话框 -->
    <el-dialog
      v-model="eventDialogVisible"
      title="日程详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-row">
          <span class="label">案件名称：</span>
          <span class="value">{{ selectedEvent.caseName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">案号：</span>
          <span class="value">{{ selectedEvent.caseNumber || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">当事人：</span>
          <span class="value">{{ selectedEvent.parties || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">时间：</span>
          <span class="value">{{ formatEventTime(selectedEvent.startTime, selectedEvent.endTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">地点：</span>
          <span class="value">{{ selectedEvent.location || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">主办律师：</span>
          <span class="value">{{ selectedEvent.ownerName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">类型：</span>
          <el-tag :type="selectedEvent.color || getEventType(selectedEvent.calendarType)" size="small">
            {{ selectedEvent.calendarType }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="eventDialogVisible = false">关闭</el-button>
        <el-button
          v-if="selectedEvent?.caseId"
          type="primary"
          @click="goToCaseDetail(selectedEvent.caseId)"
        >
          查看案件详情
        </el-button>
      </template>
    </el-dialog>

    <!-- AI助手 -->
    <AIAssistant v-model:visible="showAIAssistant" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Plus, FolderAdd, UserFilled, MagicStick, UploadFilled, Loading, Bell } from '@element-plus/icons-vue'
import PriorityDot from '@/components/PriorityDot.vue'
import CaseFileIntakePanel from '@/components/CaseFileIntakePanel.vue'
import { getDashboardStats } from '@/api/dashboard'
import { getTodoList, deleteTodo, updateTodo } from '@/api/todo'
import { uploadDocForAIRecognition } from '@/api/ai'
import { AI_RECOGNITION } from '@/config/ai-terminology'
import { getCalendarList } from '@/api/calendar'
import { useUserStore } from '@/stores'
import AIAssistant from '@/views/ai/assistant.vue'
const userStore = useUserStore()
const router = useRouter()

// 组件挂载状态标记，防止内存泄漏
const isMounted = ref(true)

// AI助手控制
const showAIAssistant = ref(false)

// Emoji到Element Plus图标组件名称的映射
const emojiToIconName = {
  '📊': 'DataAnalysis',
  '⚖️': 'Files',
  '📅': 'Calendar',
  '✅': 'CircleCheck',
  '💰': 'Finance'
}

const getIconName = (emoji) => {
  return emojiToIconName[emoji] || 'DataAnalysis'
}

// 统计数据（带趋势和动画效果）
const stats = ref([
  { key: 'monthlyCases', label: '本月案件数', value: 0, icon: '📊', type: 'primary', trend: 0, loading: false, route: '/case/list' },
  { key: 'activeCases', label: '进行中案件', value: 0, icon: '⚖️', type: 'success', trend: 0, loading: false, route: '/case/list?status=ACTIVE' },
  { key: 'monthlyHearings', label: '本月开庭', value: 0, icon: '📅', type: 'warning', trend: 0, loading: false, route: '/calendar' },
  { key: 'pendingTodos', label: '待办数', value: 0, icon: '✅', type: 'danger', trend: 0, loading: false, route: '/calendar' },
  { key: 'monthlyIncome', label: '本月收费', value: '¥0', icon: '💰', type: 'info', trend: 0, loading: false, route: null }
])

// 定时刷新统计数据的定时器
let statsRefreshInterval = null
let reminderCheckInterval = null

// 上一次的统计数据（用于计算趋势）
const previousStats = ref({})

// 日历相关
const calendarView = ref('month')
const calendarDate = ref(new Date())
const calendarEvents = ref([])
const calendarFilters = ref({
  calendarType: '',
  caseType: '',
  caseStatus: '',
  court: '',
  ownerId: null
})

const calendarOwnerOptions = computed(() => {
  const map = new Map()
  calendarEvents.value.forEach((e) => {
    const d = e.data
    if (d?.ownerId && d?.ownerName) {
      map.set(d.ownerId, { id: d.ownerId, name: d.ownerName })
    }
  })
  return [...map.values()]
})

const matchesCalendarFilters = (event) => {
  const d = event.data || {}
  const f = calendarFilters.value
  if (f.calendarType && d.calendarType !== f.calendarType) return false
  if (f.caseType && d.caseType !== f.caseType) return false
  if (f.caseStatus && d.caseStatus !== f.caseStatus) return false
  if (f.ownerId && d.ownerId !== f.ownerId) return false
  if (f.court && !(d.court || '').includes(f.court)) return false
  return true
}

const resetCalendarFilters = () => {
  calendarFilters.value = {
    calendarType: '',
    caseType: '',
    caseStatus: '',
    court: '',
    ownerId: null
  }
}

// 待办事项
const todos = ref([])

// AI智能上传相关
const uploadRef = ref(null)
const aiProcessing = ref(false)
const aiResult = ref(null)

// 自定义上传处理
const handleCustomUpload = async (options) => {
  const { file } = options

  // 校验文件类型
  const isValidType = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain', 'image/jpeg', 'image/png'].includes(file.type)
  const isValidSize = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只支持PDF、Word、TXT、图片格式文件')
    return
  }
  if (!isValidSize) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }

  aiProcessing.value = true
  aiResult.value = null

  try {
    ElMessage.info(AI_RECOGNITION.processingHint)
    const response = await uploadDocForAIRecognition(file)
    aiProcessing.value = false

    if (response.success || response.code === 200) {
      const data = response.data
      // 保存完整的识别结果
      aiResult.value = {
        success: true,
        documentType: data.documentType,
        caseNumber: data.caseNumber,
        courtName: data.courtName,
        plaintiffName: data.plaintiffName,
        defendantName: data.defendantName,
        caseReason: data.caseReason,
        judgmentDate: data.judgmentDate,
        hearingDate: data.hearingDate,
        processingTime: data.processingTime,
        // 业务逻辑执行结果（如果有）
        businessLogic: {
          caseCreated: !!data.caseNumber, // 案件是否创建
          todoCreated: true, // 待办是否创建
          calendarCreated: true, // 日程是否创建
          workReportCreated: true // 工作日志是否创建
        }
      }

      ElMessage.success(`${AI_RECOGNITION.successTitle}！文书类型：${data.documentType || '未知'}\n案号：${data.caseNumber || '无'}\n处理时间：${data.processingTime || 0}ms`)
      // 刷新待办列表
      fetchTodos()
      // 刷新日程列表
      fetchCalendarEvents()
      // 刷新统计数据
      fetchStats()
    } else {
      ElMessage.error(response.message || AI_RECOGNITION.failMessage)
    }
  } catch (error) {
    aiProcessing.value = false
    console.error('上传失败:', error)

    // 更友好的超时错误提示
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      ElMessage.error(`${AI_RECOGNITION.failMessage}（文档可能较大），请稍后在待办事项中查看结果`)
    } else {
      ElMessage.error('文档上传失败：' + (error.message || '未知错误'))
    }
  }
}

// 获取统计数据（带动画效果和趋势计算）
const fetchStats = async (showLoading = false) => {
  try {
    // 如果需要显示加载状态
    if (showLoading) {
      stats.value.forEach(stat => stat.loading = true)
    }

    const response = await getDashboardStats(userStore.userId)
    if (response.code === 200 || response.success) {
      const data = response.data

      // 保存旧值用于计算趋势
      const oldValues = { ...previousStats.value }

      // 更新统计数据
      stats.value[0].value = data.monthlyCases || 0
      stats.value[1].value = data.activeCases || 0
      stats.value[2].value = data.monthlyHearings || 0
      stats.value[3].value = data.pendingTodos || 0
      stats.value[4].value = data.monthlyIncome ? `¥${data.monthlyIncome.toLocaleString()}` : '¥0'

      // 计算趋势（与上次数据对比）
      stats.value.forEach(stat => {
        if (oldValues[stat.key] !== undefined && oldValues[stat.key] !== 0) {
          const oldValue = oldValues[stat.key]
          const newValue = typeof stat.value === 'number' ? stat.value : parseInt(stat.value.replace(/[^\d]/g, '')) || 0
          const trendValue = typeof data.monthlyIncome === 'number' ? data.monthlyIncome : 0

          if (typeof stat.value === 'number' && stat.key !== 'monthlyIncome') {
            stat.trend = oldValue > 0 ? Math.round(((stat.value - oldValue) / oldValue) * 100) : 0
          }
        }
        // 保存当前值
        previousStats.value[stat.key] = stat.value
        stat.loading = false
      })
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    // 添加组件卸载检查
    if (isMounted.value) {
      stats.value.forEach(stat => stat.loading = false)
    }
  }
}

// 点击统计卡片跳转
const handleStatClick = (stat) => {
  if (stat.route) {
    router.push(stat.route)
  }
}

// 获取日程事件
const fetchCalendarEvents = async () => {
  try {
    const response = await getCalendarList({
      userId: userStore.userId,
      startDate: formatDateToString(new Date(new Date().getFullYear(), new Date().getMonth(), 1)),
      endDate: formatDateToString(new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0))
    })

    if (response.code === 200 || response.success) {
      const events = Array.isArray(response.data)
        ? response.data
        : (response.data?.list || response.data?.records || [])
      calendarEvents.value = events.map(event => ({
        id: event.id,
        date: formatDateToString(new Date(event.startTime)),
        title: event.title || event.calendarType,
        type: event.color || getEventType(event.calendarType),
        data: event
      }))
    }
  } catch (error) {
    console.error('获取日程失败:', error)
    ElMessage.error('获取日程数据失败')
  }
}

// 格式化日期为YYYY-MM-DD
const formatDateToString = (date) => {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 根据日程类型返回Element Plus的tag类型
const getEventType = (calendarType) => {
  const typeMap = {
    'HEARING': 'danger',      // 开庭/听证 → 红色
    'DEADLINE': 'warning',    // 审限届满 → 橙色
    'FILING': 'primary',      // 立案 → 蓝色
    'MEDIATION': 'success',   // 调解/和解 → 绿色
    'EVIDENCE': 'info'        // 举证截止 → 紫色
  }
  return typeMap[calendarType] || 'default'
}

// 格式化事件时间显示
const formatEventTime = (startTime, endTime) => {
  if (!startTime) return '-'
  const start = new Date(startTime)
  const end = endTime ? new Date(endTime) : null

  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  }

  if (end) {
    return `${formatDate(start)} ~ ${formatDate(end)}`
  }
  return formatDate(start)
}

// 跳转到案件详情
const goToCaseDetail = (caseId) => {
  eventDialogVisible.value = false
  router.push(`/case/${caseId}`)
}

// 获取待办事项
const fetchTodos = async () => {
  try {
    const response = await getTodoList({
      assigneeId: userStore.userId,
      status: 'PENDING',
      sortBy: 'dueDate',
      sortOrder: 'ASC'
    })
    if (response.code === 200 || response.success) {
      todos.value = Array.isArray(response.data)
        ? response.data
        : (response.data?.list || response.data?.records || [])
    }
  } catch (error) {
    console.error('获取待办失败:', error)
  }
}

// 获取指定日期的事件（含 PRD 多维筛选）
const getEventsForDate = (date) => {
  return calendarEvents.value.filter(
    (event) => event.date === date && matchesCalendarFilters(event)
  )
}

// 处理事件点击 - 显示案件详情浮层
const selectedEvent = ref(null)
const eventDialogVisible = ref(false)

const handleEventClick = (event) => {
  selectedEvent.value = event.data
  eventDialogVisible.value = true
}

// 排序待办事项（逾期高亮、优先级排序）
const sortedTodos = computed(() => {
  return [...todos.value].sort((a, b) => {
    // 逾期置顶
    const aOverdue = isOverdue(a.deadline)
    const bOverdue = isOverdue(b.deadline)
    if (aOverdue && !bOverdue) return -1
    if (!aOverdue && bOverdue) return 1

    // 按优先级排序（高 > 中 > 低）
    const priorityMap = { high: 3, medium: 2, low: 1, NORMAL: 2, URGENT: 3 }
    const aPriority = priorityMap[a.priority] || 1
    const bPriority = priorityMap[b.priority] || 1
    if (aPriority !== bPriority) return bPriority - aPriority

    // 同优先级按截止时间排序
    return new Date(a.deadline) - new Date(b.deadline)
  })
})

// 判断是否逾期
const isOverdue = (deadline) => {
  return new Date(deadline) < new Date()
}

// 获取待办样式类（逾期高亮）
const getTodoClass = (todo) => {
  if (todo.completed || todo.status === 'COMPLETED') return 'todo-completed'

  const now = new Date()
  const deadline = new Date(todo.deadline)
  const daysUntilDeadline = Math.ceil((deadline - now) / (1000 * 60 * 60 * 24))

  // 逾期高亮
  if (daysUntilDeadline < 0) return 'todo-overdue'
  // 紧急高亮（3天内）
  if (daysUntilDeadline <= 3) return 'todo-urgent'
  // 预警（7天内）
  if (daysUntilDeadline <= 7) return 'todo-warning'
  return ''
}

// 待办操作
const handleTodoComplete = async (todo) => {
  try {
    const response = await updateTodo(todo.id, { completed: todo.completed })
    if (response.code === 200 || response.success) {
      ElMessage.success(todo.completed ? '待办已完成' : '待办已恢复')
      // 刷新列表
      await fetchTodos()
      // 刷新统计
      await fetchStats()
    }
  } catch (error) {
    console.error('更新待办状态失败:', error)
    ElMessage.error('更新失败')
    // 恢复状态
    todo.completed = !todo.completed
  }
}

const handleCreateTodo = () => {
  // 直接跳转到日程页面的新建功能，因为待办功能集成在日程管理中
  router.push('/calendar')
}

const handleCreateEvent = () => {
  // 直接跳转到日程管理页面
  router.push('/calendar')
}

const handleEditTodo = (todo) => {
  // removed debug log
}

const handleDeleteTodo = async (todo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除待办"${todo.title}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用删除API
    const response = await deleteTodo(todo.id)
    if (response.code === 200 || response.success) {
      ElMessage.success('删除成功')
      // 重新加载待办列表
      await fetchTodos()
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 快捷操作（新建案件、AI助手、上传文书）
const handleQuickAction = (action) => {
  if (action === 'aiAssistant') {
    // 触发AI助手对话框，不跳转页面
    showAIAssistant.value = true
    return
  }

  const actionMap = {
    createCase: '/case/create',
    createClient: '/client/create',
    uploadDoc: '/case/list'
  }

  if (action === 'uploadDoc') {
    ElMessage.info('请选择案件以管理案件文书')
  } else if (actionMap[action]) {
    router.push(actionMap[action])
  }
}

// 检查紧急待办并提醒
const checkUrgentTodos = () => {
  const now = new Date()
  const urgentTodos = todos.value.filter(todo => {
    if (todo.completed || todo.status === 'COMPLETED') return false
    const deadline = new Date(todo.deadline)
    const hoursUntil = (deadline - now) / (1000 * 60 * 60)
    // 24小时内逾期或即将到期的待办
    return hoursUntil <= 24 && hoursUntil > 0
  })

  if (urgentTodos.length > 0) {
    ElNotification({
      title: '待办提醒',
      message: `您有 ${urgentTodos.length} 个待办将在24小时内到期`,
      type: 'warning',
      icon: Bell,
      duration: 5000
    })
  }

  // 检查逾期待办
  const overdueTodos = todos.value.filter(todo => {
    if (todo.completed || todo.status === 'COMPLETED') return false
    return isOverdue(todo.deadline)
  })

  if (overdueTodos.length > 0) {
    ElNotification({
      title: '逾期提醒',
      message: `您有 ${overdueTodos.length} 个待办已逾期`,
      type: 'error',
      icon: Bell,
      duration: 8000
    })
  }
}

onMounted(() => {
  // 初始化数据
  fetchStats(true)
  fetchCalendarEvents()
  fetchTodos()

  // 检查紧急待办
  checkUrgentTodos()

  // 定时刷新统计数据（每5分钟）
  statsRefreshInterval = setInterval(() => {
    fetchStats(false)
  }, 5 * 60 * 1000)

  // 定时检查待办提醒（每10分钟）
  reminderCheckInterval = setInterval(() => {
    checkUrgentTodos()
    fetchTodos()
  }, 10 * 60 * 1000)
})

// 组件卸载时清除定时器和标记状态
onUnmounted(() => {
  // 标记组件已卸载，防止异步操作更新已卸载的组件
  isMounted.value = false

  if (statsRefreshInterval) {
    clearInterval(statsRefreshInterval)
  }
  if (reminderCheckInterval) {
    clearInterval(reminderCheckInterval)
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  .stats-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 20px;

    .stat-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transition: all 0.3s ease;
      cursor: pointer;
      position: relative;
      overflow: hidden;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
      }

      &:active {
        transform: translateY(-2px);
      }

      .stat-icon {
        font-size: 40px;
        width: 60px;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 12px;
        transition: transform 0.3s ease;
      }

      &:hover .stat-icon {
        transform: scale(1.1);
      }

      .stat-content {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
          margin-bottom: 4px;
          display: flex;
          align-items: center;
          gap: 8px;

          .value-number {
            transition: transform 0.3s ease;
          }

          &.has-trend .value-number {
            animation: valueUpdate 0.5s ease;
          }

          .trend-indicator {
            font-size: 14px;
            font-weight: normal;
            padding: 2px 6px;
            border-radius: 4px;

            &.trend-up {
              color: #67c23a;
              background-color: #f0f9ff;
            }

            &.trend-down {
              color: #f56c6c;
              background-color: #fef0f0;
            }
          }
        }

        .stat-label {
          font-size: 14px;
          color: #999;
        }
      }

      .stat-loading {
        position: absolute;
        top: 10px;
        right: 10px;
        color: #409eff;
        font-size: 16px;
      }

      &.stat-primary .stat-icon {
        background-color: #e6f7ff;
        color: #1890ff;
      }

      &.stat-success .stat-icon {
        background-color: #f6ffed;
        color: #52c41a;
      }

      &.stat-warning .stat-icon {
        background-color: #fffbe6;
        color: #faad14;
      }

      &.stat-danger .stat-icon {
        background-color: #fff1f0;
        color: #f5222d;
      }

      &.stat-info .stat-icon {
        background-color: #f0f5ff;
        color: #722ed1;
      }
    }
  }

  .dashboard-content {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 20px;
    margin-bottom: 20px;

    .calendar-section,
    .todo-section {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .ai-upload-section {
      background: linear-gradient(135deg, #8e9eab 0%, #eef2f3 100%);
      border-radius: 8px;
      padding: 20px;

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        flex-wrap: wrap;
        gap: 10px;

        h3 {
          margin: 0;
          color: #303133;
          font-size: 16px;
        }

        .header-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }
      }

      .upload-dragger {
        :deep(.el-upload-dragger) {
          background-color: rgba(255, 255, 255, 0.95);
          border: 2px dashed rgba(108, 117, 125, 0.3);

          &:hover {
            border-color: #606266;
          }

          .el-icon--upload {
            font-size: 48px;
            color: #606266;
            margin-bottom: 16px;
          }

          .el-upload__text {
            color: #303133;

            em {
              color: #606266;
              font-style: normal;
            }
          }

          .el-upload__tip {
            color: #909399;
            font-size: 12px;
            line-height: 1.5;
            margin-top: 12px;
          }
        }
      }

      .ai-processing {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 12px;
        background-color: rgba(255, 255, 255, 0.9);
        border-radius: 4px;
        margin-top: 12px;
        color: #606266;
        font-size: 14px;

        .el-icon {
          font-size: 18px;
        }
      }

      .ai-result {
        margin-top: 12px;

        :deep(.el-alert) {
          background-color: rgba(255, 255, 255, 0.95);
          border: none;

          .el-alert__title {
            color: #67c23a;
            font-weight: 500;
          }
        }

        .result-alert {
          .result-title {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-weight: 500;
          }

          .result-content {
            margin-top: 12px;

            .result-section {
              margin-bottom: 16px;

              &:last-child {
                margin-bottom: 0;
              }

              h4 {
                margin: 0 0 8px 0;
                font-size: 14px;
                font-weight: 500;
                color: #333;
              }

              .result-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 8px;

                .result-item {
                  font-size: 13px;

                  .label {
                    color: #666;
                    font-weight: 500;
                  }

                  .value {
                    color: #333;
                    margin-left: 4px;
                  }
                }
              }

              .business-logic {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                margin-bottom: 12px;
              }

              .quick-links {
                display: flex;
                gap: 8px;
              }
            }
          }
        }
      }
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
      background-color: #fff;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        flex-wrap: wrap;
        gap: 10px;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
          color: #333;
        }

        .header-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }
      }

      .calendar-filters {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        padding: 0 20px 12px;
        align-items: center;
      }
    }

    .calendar-view {
      :deep(.el-calendar) {
        .el-calendar__header {
          padding: 12px 20px;
          border-bottom: 1px solid #f0f0f0;
        }

        .el-calendar__body {
          padding: 12px 20px 20px;
        }

        .calendar-day {
          height: 80px;
          padding: 4px;
          display: flex;
          flex-direction: column;
          gap: 4px;

          .date-number {
            font-size: 14px;
            font-weight: 500;
            color: #333;
          }

          .event-tags {
            display: flex;
            flex-direction: column;
            gap: 2px;
            overflow: hidden;

            .event-tag {
              font-size: 12px;
              cursor: pointer;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }
      }
    }

    .todo-list {
      .todo-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px;
        border-bottom: 1px solid #f0f0f0;
        transition: background-color 0.3s;

        &:hover {
          background-color: #f5f5f5;
        }

        &:last-child {
          border-bottom: none;
        }

        &.todo-overdue {
          background-color: #fff1f0;
          border-left: 3px solid #f56c6c;
          animation: pulse-warning 2s infinite;

          .todo-title {
            color: #f56c6c;
            font-weight: 500;
          }
        }

        &.todo-urgent {
          background-color: #fef0f0;
          border-left: 3px solid #f56c6c;

          .todo-title {
            color: #f56c6c;
            font-weight: 500;
          }

          .todo-deadline {
            color: #f56c6c;
            font-weight: 500;
          }
        }

        &.todo-warning {
          background-color: #fdf6ec;
          border-left: 3px solid #e6a23c;

          .todo-title {
            color: #e6a23c;
          }

          .todo-deadline {
            color: #e6a23c;
          }
        }

        &.todo-completed {
          opacity: 0.6;
          text-decoration: line-through;
        }

        .todo-left {
          display: flex;
          align-items: center;
          gap: 12px;
          flex: 1;

          .todo-content {
            .todo-title {
              font-size: 14px;
              color: #333;
              margin-bottom: 4px;
            }

            .todo-meta {
              display: flex;
              align-items: center;
              gap: 8px;
              font-size: 12px;
              color: #999;

              .clickable-tag {
                cursor: pointer;
                transition: all 0.3s;

                &:hover {
                  opacity: 0.8;
                  transform: translateY(-1px);
                }
              }
            }
          }
        }

        .todo-actions {
          display: flex;
          gap: 8px;
        }
      }
    }
  }

  .quick-actions {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 20px;
    background-color: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 20px;
      border: 1px dashed #d9d9d9;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: #1890ff;
        background-color: #f0f5ff;
      }

      span {
        font-size: 14px;
        color: #333;
      }
    }
  }

  // 案件详情浮层样式
  .event-detail {
    .detail-row {
      display: flex;
      margin-bottom: 16px;
      align-items: center;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        font-weight: 500;
        color: #606266;
        min-width: 100px;
        font-size: 14px;
      }

      .value {
        color: #303133;
        font-size: 14px;
        flex: 1;
      }
    }
  }
}

// 添加动画关键帧
@keyframes valueUpdate {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes pulse-warning {
  0%, 100% {
    background-color: #fff1f0;
  }
  50% {
    background-color: #ffebeb;
  }
}
</style>
