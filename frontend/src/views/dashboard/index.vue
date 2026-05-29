<template>
  <div class="dashboard">
    <!-- 核心：卷宗智能录入（传文件→AI分析→登记备注→案件档案夹） -->
    <CaseFileIntakePanel />

    <div class="welcome-strip">
      <div class="welcome-text">
        <h2>欢迎回来，{{ userDisplayName }}</h2>
        <p>{{ greetingText }} · 本周办案一览</p>
      </div>
      <el-button text type="primary" size="small" @click="router.push('/ai-hub')">
        文书识别 / 待办自动化 → AI 智能中心
      </el-button>
    </div>

    <!-- 今日横条 -->
    <div class="today-bar">
      <span class="today-label">今日</span>
      <button type="button" class="today-chip hearing" @click="goCalendarFilter('HEARING')">
        开庭 {{ todaySummary.hearings }}
      </button>
      <button type="button" class="today-chip deadline" @click="goCalendarFilter('DEADLINE')">
        审限 {{ todaySummary.deadlines }}
      </button>
      <button type="button" class="today-chip overdue" @click="router.push('/calendar?filter=overdue')">
        逾期待办 {{ todaySummary.overdueTodos }}
      </button>
    </div>

    <!-- 统计卡片区（可折叠） -->
    <div class="stats-toggle">
      <el-button text size="small" @click="statsCollapsed = !statsCollapsed">
        {{ statsCollapsed ? '展开统计卡片' : '收起统计卡片' }}
      </el-button>
    </div>
    <div v-show="!statsCollapsed" class="stats-cards">
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
            <el-radio-group v-model="calendarView" size="small" @change="handleCalendarViewChange">
              <el-radio-button value="week">周视图</el-radio-button>
              <el-radio-button value="month">月视图</el-radio-button>
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
        <div class="calendar-with-copilot">
          <div class="calendar-main">
            <div class="calendar-view">
              <CalendarWeekView
                v-if="calendarView === 'week'"
                v-model="calendarDate"
                :events="filteredCalendarEvents"
                @event-click="handleEventClick"
              />
              <div v-else class="month-fallback">
                <p>完整月视图请前往日程页查看与编辑。</p>
                <el-button type="primary" plain size="small" @click="router.push('/calendar?view=month')">
                  打开日程 · 月视图
                </el-button>
              </div>
            </div>
          </div>
          <CalendarCopilotStrip
            v-if="calendarView === 'week'"
            :events="filteredCalendarEvents"
            :todos="todos"
            :selected-event="selectedEventUi"
            :week-date="calendarDate"
          />
        </div>
      </div>

      <!-- 待办事项区 -->
      <div class="todo-section">
        <TodoPanelCompact
          :todos="todos"
          :limit="5"
          @complete="handleTodoCompleteCompact"
          @go-case="goToCaseDetail"
          @view-all="router.push('/calendar?filter=overdue')"
        />
        <div class="todo-actions-row">
          <el-button type="primary" size="small" @click="handleCreateTodo">
            <el-icon><Plus /></el-icon>
            新建待办
          </el-button>
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
      <div class="action-item" @click="handleQuickAction('intake')">
        <el-icon :size="32" color="#3b6fd9"><UploadFilled /></el-icon>
        <span>卷宗录入</span>
      </div>
    </div>

    <CalendarEventDrawer
      v-model:visible="eventDrawerVisible"
      :event="selectedEventUi"
      @go-case="goToCaseDetail"
      @open-calendar="router.push('/calendar')"
    />

    <!-- AI助手 -->
    <AIAssistant v-model:visible="showAIAssistant" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { Plus, FolderAdd, UserFilled, MagicStick, UploadFilled, Bell } from '@element-plus/icons-vue'
import CaseFileIntakePanel from '@/components/CaseFileIntakePanel.vue'
import CalendarWeekView from '@/components/calendar/CalendarWeekView.vue'
import CalendarEventDrawer from '@/components/calendar/CalendarEventDrawer.vue'
import CalendarCopilotStrip from '@/components/calendar/CalendarCopilotStrip.vue'
import TodoPanelCompact from '@/components/calendar/TodoPanelCompact.vue'
import { getDashboardStats } from '@/api/dashboard'
import { getTodoList, updateTodo } from '@/api/todo'
import { getCalendarList } from '@/api/calendar'
import { useUserStore } from '@/stores'
import AIAssistant from '@/views/ai/assistant.vue'
import {
  formatDateToString,
  mapApiEventToUi,
  startOfWeek,
  endOfWeek,
  countTodayByType,
  isTodoOverdue
} from '@/utils/calendarUi'
const userStore = useUserStore()
const router = useRouter()

const userDisplayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

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

// 统计卡默认折叠，让位周视图
const statsCollapsed = ref(true)

// 日历相关（默认周视图）
const calendarView = ref('week')
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

const filteredCalendarEvents = computed(() =>
  calendarEvents.value.filter((event) => matchesCalendarFilters(event))
)

const todaySummary = computed(() => {
  const events = filteredCalendarEvents.value
  const pending = todos.value.filter((t) => !t.completed && t.status !== 'COMPLETED')
  return {
    hearings: countTodayByType(events, ['hearing', 'HEARING']),
    deadlines: countTodayByType(events, ['deadline', 'DEADLINE']),
    overdueTodos: pending.filter((t) => new Date(t.deadline) < new Date()).length
  }
})

const goCalendarFilter = (type) => {
  calendarFilters.value.calendarType = type
  calendarView.value = 'week'
}

// 待办事项
const todos = ref([])

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

// 获取日程事件（本周范围）
const fetchCalendarEvents = async () => {
  try {
    const base = calendarView.value === 'week' ? calendarDate.value : new Date()
    const start = startOfWeek(base)
    const end = endOfWeek(base)
    const response = await getCalendarList({
      userId: userStore.userId,
      startDate: formatDateToString(start),
      endDate: formatDateToString(end)
    })

    if (response.code === 200 || response.success) {
      const events = Array.isArray(response.data)
        ? response.data
        : (response.data?.list || response.data?.records || [])
      calendarEvents.value = events.map(mapApiEventToUi)
    }
  } catch (error) {
    console.error('获取日程失败:', error)
    ElMessage.error('获取日程数据失败')
  }
}

const handleCalendarViewChange = (view) => {
  if (view === 'month') {
    router.push('/calendar?view=month')
    calendarView.value = 'week'
  } else {
    fetchCalendarEvents()
  }
}

watch(calendarDate, () => {
  if (calendarView.value === 'week') {
    fetchCalendarEvents()
  }
})

// 跳转到案件详情
const goToCaseDetail = (caseId) => {
  eventDrawerVisible.value = false
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

// 处理事件点击
const selectedEventUi = ref(null)
const eventDrawerVisible = ref(false)

const handleEventClick = (event) => {
  selectedEventUi.value = event
  eventDrawerVisible.value = true
}

const handleTodoCompleteCompact = async (todo, completed) => {
  todo.completed = completed
  await handleTodoComplete(todo)
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
  router.push('/calendar')
}

// 快捷操作
const handleQuickAction = (action) => {
  if (action === 'aiAssistant') {
    showAIAssistant.value = true
    return
  }
  if (action === 'intake') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  const actionMap = {
    createCase: '/case/create',
    createClient: '/client/create'
  }
  if (actionMap[action]) {
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
    return isTodoOverdue(todo.deadline)
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
  .welcome-strip {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 12px;
    padding: 20px 24px;
    background: linear-gradient(135deg, #eef3fc 0%, #f8fafc 100%);
    border: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.08));
    border-radius: var(--lawos-radius-lg, 12px);
    box-shadow: var(--lawos-shadow-sm);

    .welcome-text {
      h2 {
        margin: 0 0 6px;
        font-size: 20px;
        font-weight: 600;
        color: var(--lawos-text, #1c1c1e);
      }

      p {
        margin: 0;
        font-size: 14px;
        color: var(--lawos-text-secondary, #6b7280);
      }
    }
  }

  .today-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: var(--lawos-surface-solid, #fff);
    border: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.08));
    border-radius: var(--lawos-radius-md, 8px);

    .today-label {
      font-weight: 600;
      font-size: 14px;
      color: var(--lawos-text, #1c1c1e);
    }

    .today-chip {
      border: none;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 13px;
      background: #f3f4f6;

      &.hearing { color: #e5484d; background: #fef2f2; }
      &.deadline { color: #f76b15; background: #fff7ed; }
      &.overdue { color: #dc2626; background: #fee2e2; }
    }
  }

  .stats-toggle {
    margin-bottom: 8px;
  }

  .month-fallback {
    padding: 24px;
    text-align: center;
    color: var(--lawos-text-secondary, #6b7280);
    background: #fafafa;
    border-radius: 8px;
  }

  .todo-actions-row {
    margin-top: 12px;
  }

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
        background-color: var(--lawos-primary-light-9, #eef3fc);
        color: var(--lawos-primary, #3b6fd9);
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

      .calendar-with-copilot {
        display: flex;
        gap: 12px;
        padding: 0 20px 20px;
        align-items: flex-start;

        .calendar-main {
          flex: 1;
          min-width: 0;
        }

        .month-fallback {
          padding: 24px;
          text-align: center;
          color: #606266;
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
}

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
