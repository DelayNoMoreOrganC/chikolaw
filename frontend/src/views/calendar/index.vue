<template>
  <div class="calendar">
    <PageHeader title="日程管理">
      <template #extra>
        <el-button type="primary" @click="handleCreateEvent">
          <el-icon><Plus /></el-icon>
          新建日程
        </el-button>
        <el-button @click="handleCreateTodo">
          <el-icon><Plus /></el-icon>
          新建待办
        </el-button>
      </template>
    </PageHeader>

    <div class="calendar-layout">
      <!-- 左侧日历视图 -->
      <div class="calendar-main">
        <!-- 视图切换 -->
        <div class="view-switcher">
          <el-radio-group v-model="viewMode" @change="handleViewChange">
            <el-radio-button value="month">月视图</el-radio-button>
            <el-radio-button value="week">周视图</el-radio-button>
            <el-radio-button value="day">日视图</el-radio-button>
          </el-radio-group>

          <div class="date-navigation">
            <el-button circle @click="handlePrev">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <span class="current-date">{{ currentMonth }}</span>
            <el-button circle @click="handleNext">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button @click="handleToday">今天</el-button>
          </div>
        </div>

        <!-- 月视图 -->
        <div v-show="viewMode === 'month'" class="month-view">
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="calendar-day" @click="handleDayClick(data.day)">
                <span class="date-number">{{ data.day.split('-')[2] }}</span>
                <div class="event-tags">
                  <el-tag
                    v-for="event in getEventsForDate(data.day)"
                    :key="event.id"
                    :type="getEventTagType(event.type)"
                    size="small"
                    class="event-tag"
                    @click.stop="handleEventClick(event)"
                  >
                    <el-icon class="tag-icon"><component :is="getEventIcon(event.type)" /></el-icon>
                    {{ event.title }}
                  </el-tag>
                </div>
              </div>
            </template>
          </el-calendar>
        </div>

        <!-- 周视图（共享组件） -->
        <div v-show="viewMode === 'week'" class="week-view-shared">
          <CalendarWeekView
            v-model="calendarDate"
            :events="weekEventsUi"
            :show-toolbar="false"
            @event-click="handleEventClick"
          />
        </div>

        <!-- 日视图 -->
        <div v-show="viewMode === 'day'" class="day-view">
          <div class="day-header">
            <h3>{{ currentDayTitle }}</h3>
          </div>

          <div class="day-body">
            <div
              v-for="hour in dayHours"
              :key="hour"
              class="hour-row"
            >
              <div class="hour-label">{{ hour }}:00</div>
              <div class="hour-content">
                <div
                  v-for="event in getEventsForHour(hour)"
                  :key="event.id"
                  class="event-detail"
                  :class="`type-${event.type}`"
                  @click="handleEventClick(event)"
                >
                  <div class="event-header">
                    <el-icon class="event-icon"><component :is="getEventIcon(event.type)" /></el-icon>
                    <span class="event-time">{{ event.startTime }} - {{ event.endTime }}</span>
                  </div>
                  <div class="event-title">{{ event.title }}</div>
                  <div v-if="event.location" class="event-location">
                    <el-icon><Location /></el-icon>
                    {{ event.location }}
                  </div>
                  <div v-if="event.caseName" class="event-case">
                    <el-icon><Briefcase /></el-icon>
                    {{ event.caseName }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧待办清单（共享 TodoPanelCompact） -->
      <div class="todo-sidebar">
        <TodoPanelCompact
          class="calendar-todo-panel"
          :todos="filteredTodos"
          :limit="0"
          :sort="false"
          title="待办事项"
          :show-view-all="false"
          show-actions
          :item-class-fn="calendarTodoClass"
          @complete="onTodoCompleteFromPanel"
          @edit="handleEditTodo"
          @delete="handleDeleteTodo"
        >
          <template #header-extra>
            <el-badge :value="urgentCount" :hidden="urgentCount === 0" class="urgent-badge">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </template>
          <template #toolbar>
            <div class="todo-filters">
              <el-radio-group v-model="todoFilter" size="small">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="pending">待办</el-radio-button>
                <el-radio-button label="overdue">逾期</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <template #empty>
            <el-empty description="暂无待办" :image-size="64" />
          </template>
        </TodoPanelCompact>
      </div>
    </div>

    <!-- 新建/编辑日程对话框 -->
    <el-dialog
      v-model="eventDialogVisible"
      :title="isEditEvent ? '编辑日程' : '新建日程'"
      width="600px"
    >
      <el-form :model="eventForm" :rules="eventRules" ref="eventFormRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="eventForm.title" placeholder="请输入日程标题" />
        </el-form-item>

        <el-form-item label="类型" prop="type">
          <el-select v-model="eventForm.type" placeholder="请选择类型">
            <el-option label="开庭/听证" value="hearing" />
            <el-option label="审限届满" value="deadline" />
            <el-option label="立案" value="filing" />
            <el-option label="调解/和解" value="mediation" />
            <el-option label="举证截止" value="evidence" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="eventForm.startTime"
                type="datetime"
                placeholder="输入或选择开始时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
                :editable="true"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="eventForm.endTime"
                type="datetime"
                placeholder="输入或选择结束时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
                :editable="true"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地点" prop="location">
          <el-input v-model="eventForm.location" placeholder="请输入地点" />
        </el-form-item>

        <el-form-item label="关联案件" prop="caseId">
          <el-select
            v-model="eventForm.caseId"
            filterable
            placeholder="选择关联案件"
            style="width: 100%"
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="caseItem.caseName || caseItem.name"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="参与人员">
          <el-select
            v-model="eventForm.participants"
            multiple
            filterable
            placeholder="选择参与人员"
            style="width: 100%"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName || user.name || user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="提醒设置">
          <el-select v-model="eventForm.reminder" placeholder="选择提醒时间">
            <el-option label="不提醒" value="" />
            <el-option label="提前15分钟" value="15m" />
            <el-option label="提前30分钟" value="30m" />
            <el-option label="提前1小时" value="1h" />
            <el-option label="提前1天" value="1d" />
            <el-option label="提前3天" value="3d" />
            <el-option label="提前7天" value="7d" />
          </el-select>
        </el-form-item>

        <el-form-item label="重复">
          <el-select v-model="eventForm.repeat" placeholder="选择重复规则">
            <el-option label="不重复" value="" />
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
            <el-option label="每年" value="yearly" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="eventForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="eventDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEvent">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑待办对话框 -->
    <el-dialog
      v-model="todoDialogVisible"
      :title="isEditTodo ? '编辑待办' : '新建待办'"
      width="500px"
    >
      <el-form :model="todoForm" :rules="todoRules" ref="todoFormRef" label-width="100px">
        <el-form-item label="待办标题" prop="title">
          <el-input v-model="todoForm.title" placeholder="请输入待办标题" />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="todoForm.priority">
            <el-radio label="high">紧急</el-radio>
            <el-radio label="medium">重要</el-radio>
            <el-radio label="low">普通</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="todoForm.deadline"
            type="datetime"
            placeholder="输入或选择截止时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :editable="true"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="todoForm.caseId"
            filterable
            placeholder="选择关联案件"
            style="width: 100%"
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="caseItem.caseName || caseItem.name"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="负责人" prop="assignee">
          <el-select
            v-model="todoForm.assignee"
            filterable
            placeholder="选择负责人"
            style="width: 100%"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName || user.name || user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="提醒设置">
          <el-select v-model="todoForm.reminder" placeholder="选择提醒时间">
            <el-option label="不提醒" value="" />
            <el-option label="提前1小时" value="1h" />
            <el-option label="提前1天" value="1d" />
            <el-option label="提前3天" value="3d" />
            <el-option label="提前7天" value="7d" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="todoForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="todoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTodo">确定</el-button>
      </template>
    </el-dialog>

    <CalendarEventDrawer
      v-model:visible="eventDetailVisible"
      :event="selectedEventUi"
      :editable="true"
      @edit="handleEditFromDetail"
      @delete="handleDeleteFromDetail"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaseList } from '@/api/case'
import { getUserList } from '@/api/user'
import { formatLocalDateTime } from '@/utils/datetime'
import {
  Plus, ArrowLeft, ArrowRight, Bell, Location, Briefcase, Clock, Document, Files, Reading
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import CalendarWeekView from '@/components/calendar/CalendarWeekView.vue'
import TodoPanelCompact from '@/components/calendar/TodoPanelCompact.vue'
import CalendarEventDrawer from '@/components/calendar/CalendarEventDrawer.vue'
import { getCalendarEvents, createEvent, updateEvent, deleteEvent } from '@/api/calendar'
import { getTodos, createTodo, updateTodo, deleteTodo } from '@/api/todo'
import { toCalendarPayload, calendarToForm, normalizeCalendarEvent } from '@/api/calendarPayload'
import {
  mapApiEventToUi,
  getEventTagType,
  formatDateToString,
  startOfWeek,
  endOfWeek,
  getEventsForDate as filterEventsByDate,
  calendarTodoClass,
  isTodoOverdue
} from '@/utils/calendarUi'

const route = useRoute()

const userStore = useUserStore()

// 视图模式
const viewMode = ref('month')
const calendarDate = ref(new Date())

// 日程事件
const eventList = ref([])

const weekEventsUi = computed(() =>
  eventList.value.map((e) => mapApiEventToUi({
    ...e,
    calendarType: e.calendarType || (e.type || '').toUpperCase()
  }))
)

// 待办事项
const todoList = ref([])
const todoFilter = ref('all')

// 对话框
const eventDialogVisible = ref(false)
const eventDetailVisible = ref(false)
const selectedEventUi = ref(null)
const todoDialogVisible = ref(false)
const isEditEvent = ref(false)
const isEditTodo = ref(false)
const eventFormRef = ref(null)
const todoFormRef = ref(null)

// 表单数据
const eventForm = reactive({
  id: null,
  title: '',
  type: '',
  startTime: '',
  endTime: '',
  location: '',
  caseId: '',
  participants: [],
  reminder: '',
  repeat: '',
  remark: ''
})

const todoForm = reactive({
  id: null,
  title: '',
  priority: 'medium',
  deadline: '',
  caseId: '',
  assignee: '',
  reminder: '',
  remark: ''
})

// 表单验证规则
const eventRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const todoRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  assignee: [{ required: true, message: '请选择负责人', trigger: 'change' }]
}

const caseList = ref([])
const userList = ref([])

const loadLookupData = async () => {
  try {
    const [caseRes, userRes] = await Promise.all([
      getCaseList({ page: 1, size: 200 }),
      getUserList({ page: 1, size: 200 })
    ])
    caseList.value = caseRes.data?.records || caseRes.data?.list || []
    const rawUsers = userRes.data
    userList.value = rawUsers?.records || rawUsers?.content || rawUsers?.list || (Array.isArray(rawUsers) ? rawUsers : [])
  } catch (e) {
    console.warn('加载案件/用户列表失败', e)
  }
}

// 当前月份
const currentMonth = computed(() => {
  const date = calendarDate.value
  return `${date.getFullYear()}年 ${date.getMonth() + 1}月`
})

// 当前日期标题
const currentDayTitle = computed(() => {
  const date = calendarDate.value
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  return `${date.getMonth() + 1}月${date.getDate()}日 星期${weekDays[date.getDay()]}`
})

// 一天的小时数
const dayHours = Array.from({ length: 12 }, (_, i) => i + 8) // 8:00 - 19:00

// 过滤待办
const filteredTodos = computed(() => {
  let filtered = todoList.value

  if (todoFilter.value === 'pending') {
    filtered = filtered.filter(todo => !todo.completed)
  } else if (todoFilter.value === 'overdue') {
    filtered = filtered.filter(todo => isTodoOverdue(todo.deadline) && !todo.completed)
  }

  return filtered.sort((a, b) => {
    const aOverdue = isTodoOverdue(a.deadline)
    const bOverdue = isTodoOverdue(b.deadline)
    if (aOverdue && !bOverdue) return -1
    if (!aOverdue && bOverdue) return 1

    // 按优先级排序
    const priorityMap = { high: 3, medium: 2, low: 1 }
    return priorityMap[b.priority] - priorityMap[a.priority]
  })
})

// 紧急待办数量
const urgentCount = computed(() => {
  return todoList.value.filter(todo => {
    const days = Math.ceil((new Date(todo.deadline) - new Date()) / (1000 * 60 * 60 * 24))
    return !todo.completed && days <= 3
  }).length
})

// 获取指定日期的事件
const getEventsForDate = (date) => filterEventsByDate(eventList.value, date)

// 获取指定小时的事件
const getEventsForHour = (hour) => {
  return eventList.value.filter(event => {
    const eventHour = parseInt(event.startTime.split(' ')[1].split(':')[0])
    return eventHour === hour
  })
}

// 获取事件图标
const getEventIcon = (type) => {
  const iconMap = {
    'hearing': Reading,
    'deadline': Clock,
    'filing': Document,
    'mediation': Bell,
    'evidence': Files
  }
  return iconMap[type] || Bell
}

// 视图切换
const handleViewChange = () => {
  // 视图切换逻辑
  fetchEvents() // 切换视图时刷新数据
}

// 日期导航
const handlePrev = () => {
  const date = new Date(calendarDate.value)
  if (viewMode.value === 'month') {
    date.setMonth(date.getMonth() - 1)
  } else if (viewMode.value === 'week') {
    date.setDate(date.getDate() - 7)
  } else {
    date.setDate(date.getDate() - 1)
  }
  calendarDate.value = date
  fetchEvents() // 切换日期时刷新事件
}

const handleNext = () => {
  const date = new Date(calendarDate.value)
  if (viewMode.value === 'month') {
    date.setMonth(date.getMonth() + 1)
  } else if (viewMode.value === 'week') {
    date.setDate(date.getDate() + 7)
  } else {
    date.setDate(date.getDate() + 1)
  }
  calendarDate.value = date
  fetchEvents() // 切换日期时刷新事件
}

const handleToday = () => {
  calendarDate.value = new Date()
}

// 点击日期
const handleDayClick = (date) => {
  calendarDate.value = new Date(date)
  viewMode.value = 'day'
}

// 点击事件
const handleEventClick = (event) => {
  selectedEventUi.value = event?.data ? event : mapApiEventToUi(event)
  eventDetailVisible.value = true
}

const getSelectedEventRaw = () => selectedEventUi.value?.data || selectedEventUi.value

const handleEditFromDetail = () => {
  const raw = getSelectedEventRaw()
  if (!raw) return
  isEditEvent.value = true
  Object.assign(eventForm, calendarToForm(raw))
  eventDetailVisible.value = false
  eventDialogVisible.value = true
}

const handleDeleteFromDetail = async () => {
  const raw = getSelectedEventRaw()
  if (!raw?.id) return
  try {
    await ElMessageBox.confirm('确定删除该日程吗？', '提示', { type: 'warning' })
    await deleteEvent(raw.id)
    ElMessage.success('已删除')
    eventDetailVisible.value = false
    await fetchEvents()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 新建日程
const handleCreateEvent = () => {
  isEditEvent.value = false
  const now = new Date()
  const end = new Date(now)
  end.setHours(end.getHours() + 1)
  Object.assign(eventForm, {
    id: null,
    title: '',
    type: 'other',
    startTime: formatLocalDateTime(now),
    endTime: formatLocalDateTime(end),
    location: '',
    caseId: '',
    participants: [],
    reminder: '',
    repeat: '',
    remark: ''
  })
  eventDialogVisible.value = true
}

// 新建待办
const handleCreateTodo = () => {
  isEditTodo.value = false
  const due = new Date()
  due.setDate(due.getDate() + 3)
  due.setHours(18, 0, 0, 0)
  Object.assign(todoForm, {
    id: null,
    title: '',
    priority: 'medium',
    deadline: formatLocalDateTime(due),
    caseId: '',
    assignee: userStore.userId || '',
    reminder: '',
    remark: ''
  })
  todoDialogVisible.value = true
}

// 编辑待办
const handleEditTodo = (todo) => {
  isEditTodo.value = true
  Object.assign(todoForm, todo)
  todoDialogVisible.value = true
}

// 删除待办
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

    await deleteTodo(todo.id)
    ElMessage.success('删除成功')
    // 从待办列表中移除已删除的项
    const index = todoList.value.findIndex(t => t.id === todo.id)
    if (index > -1) {
      todoList.value.splice(index, 1)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除待办失败:', error)
      ElMessage.error('删除待办失败')
    }
  }
}

// 完成待办
const onTodoCompleteFromPanel = (todo, completed) => {
  todo.completed = completed
  handleTodoComplete(todo)
}

const handleTodoComplete = (todo) => {
  ElMessage.success('待办状态已更新')
}

// 提交日程
const handleSubmitEvent = async () => {
  try {
    await eventFormRef.value?.validate()
    if (!eventForm.endTime && eventForm.startTime) {
      const end = new Date(eventForm.startTime.replace(' ', 'T'))
      end.setHours(end.getHours() + 1)
      eventForm.endTime = formatLocalDateTime(end)
    }

    const payload = toCalendarPayload(eventForm)
    if (isEditEvent.value) {
      await updateEvent(eventForm.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createEvent(payload)
      ElMessage.success('创建成功')
    }

    eventDialogVisible.value = false
    await fetchEvents()
  } catch (error) {
    if (error !== false && error?.message && !error?.response) {
      ElMessage.error(error.message)
    }
  }
}

// 提交待办
const handleSubmitTodo = async () => {
  try {
    await todoFormRef.value?.validate()
    const assigneeId = todoForm.assignee || userStore.userId
    const payload = { ...todoForm, assignee: assigneeId }

    if (isEditTodo.value) {
      await updateTodo(todoForm.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createTodo(payload)
      ElMessage.success('创建成功')
    }

    todoDialogVisible.value = false
    await fetchTodos()
  } catch (error) {
    if (error !== false && error?.message && !error?.response) {
      ElMessage.error(error.message)
    }
  }
}

// 初始化数据
const fetchEvents = async () => {
  try {
    let startDate, endDate

    // 根据视图模式计算日期范围
    if (viewMode.value === 'month') {
      startDate = getCurrentMonthStart()
      endDate = getCurrentMonthEnd()
    } else if (viewMode.value === 'week') {
      startDate = formatDateToString(startOfWeek(calendarDate.value))
      endDate = formatDateToString(endOfWeek(calendarDate.value))
    } else {
      // 日视图
      startDate = formatDateToString(new Date(calendarDate.value))
      endDate = formatDateToString(new Date(calendarDate.value))
    }

    const res = await getCalendarEvents({
      startDate,
      endDate
    })
    const raw = res.data?.records || res.data || []
    eventList.value = raw.map(normalizeCalendarEvent)
  } catch (error) {
    console.error('获取日程事件失败:', error)
    ElMessage.error('获取日程事件失败')
  }
}

const fetchTodos = async () => {
  try {
    const res = await getTodos(0, 100)
    todoList.value = res.data?.records || res.data || []
  } catch (error) {
    console.error('获取待办事项失败:', error)
    ElMessage.error('获取待办事项失败')
  }
}

// 获取当前月份的开始和结束日期
const getCurrentMonthStart = () => {
  const date = new Date(calendarDate.value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-01`
}

const getCurrentMonthEnd = () => {
  const date = new Date(calendarDate.value)
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  return `${lastDay.getFullYear()}-${String(lastDay.getMonth() + 1).padStart(2, '0')}-${String(lastDay.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  const qView = route.query.view
  if (qView === 'week' || qView === 'month' || qView === 'day') {
    viewMode.value = qView
  }
  if (route.query.filter === 'overdue') {
    todoFilter.value = 'overdue'
  }
  loadLookupData()
  fetchEvents()
  fetchTodos()
})
</script>

<style scoped lang="scss">
.calendar {
  .calendar-layout {
    display: grid;
    grid-template-columns: 1fr 350px;
    gap: 20px;
  }

  .calendar-main {
    .view-switcher {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding: 15px 20px;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .date-navigation {
        display: flex;
        gap: 10px;
        align-items: center;

        .current-date {
          font-size: 16px;
          font-weight: 500;
          color: #333;
          min-width: 150px;
          text-align: center;
        }
      }
    }

    .month-view {
      background-color: #fff;
      padding: 20px;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

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
          cursor: pointer;
          transition: background-color 0.3s;

          &:hover {
            background-color: #f5f7fa;
          }

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

              .tag-icon {
                margin-right: 4px;
              }
            }
          }
        }
      }
    }

    .week-view-shared {
      background-color: #fff;
      padding: 12px 20px 20px;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

    .day-view {
      background-color: #fff;
      padding: 20px;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .day-header {
        margin-bottom: 20px;
        padding-bottom: 15px;
        border-bottom: 1px solid #e4e7ed;

        h3 {
          margin: 0;
          font-size: 18px;
          color: #333;
        }
      }

      .day-body {
        .hour-row {
          display: grid;
          grid-template-columns: 60px 1fr;
          gap: 10px;
          min-height: 60px;
          border-bottom: 1px solid #f0f0f0;

          .hour-label {
            font-size: 12px;
            color: #909399;
            text-align: center;
            padding-top: 10px;
          }

          .hour-content {
            .event-detail {
              padding: 10px;
              border-radius: 4px;
              margin-bottom: 8px;
              cursor: pointer;
              border-left: 3px solid;
              transition: all 0.3s;

              &:hover {
                transform: translateX(2px);
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
              }

              &.type-hearing {
                background-color: #fef0f0;
                border-left-color: #f56c6c;
              }

              &.type-deadline {
                background-color: #fdf6ec;
                border-left-color: #e6a23c;
              }

              &.type-filing {
                background-color: #ecf5ff;
                border-left-color: #409eff;
              }

              &.type-mediation {
                background-color: #f0f9ff;
                border-left-color: #67c23a;
              }

              &.type-evidence {
                background-color: #f4f4f5;
                border-left-color: #909399;
              }

              .event-header {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 5px;

                .event-icon {
                  font-size: 16px;
                }

                .event-time {
                  font-size: 13px;
                  color: #606266;
                }
              }

              .event-title {
                font-size: 14px;
                font-weight: 500;
                color: #333;
                margin-bottom: 5px;
              }

              .event-location,
              .event-case {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 12px;
                color: #909399;
              }
            }
          }
        }
      }
    }
  }

  .todo-sidebar {
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 200px);
    padding: 12px 16px;

    .calendar-todo-panel {
      flex: 1;
      min-height: 0;
    }

    .todo-filters {
      margin-bottom: 8px;
    }

    .urgent-badge {
      font-size: 18px;
      color: #f56c6c;
    }
  }
}
</style>
