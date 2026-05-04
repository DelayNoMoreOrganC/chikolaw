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

        <!-- 周视图 -->
        <div v-show="viewMode === 'week'" class="week-view">
          <div class="week-header">
            <div
              v-for="day in weekDays"
              :key="day.date"
              class="day-header"
              :class="{ 'is-today': day.isToday }"
            >
              <div class="day-name">{{ day.name }}</div>
              <div class="day-date">{{ day.date }}</div>
            </div>
          </div>

          <div class="week-body">
            <div
              v-for="hour in dayHours"
              :key="hour"
              class="hour-row"
            >
              <div class="hour-label">{{ hour }}:00</div>
              <div class="hour-events">
                <div
                  v-for="event in getEventsForHour(hour)"
                  :key="event.id"
                  class="event-block"
                  :class="`type-${event.type}`"
                  @click="handleEventClick(event)"
                >
                  <span class="event-time">{{ event.startTime }}</span>
                  <span class="event-title">{{ event.title }}</span>
                </div>
              </div>
            </div>
          </div>
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

      <!-- 右侧待办清单 -->
      <div class="todo-sidebar">
        <div class="sidebar-header">
          <h4>待办事项</h4>
          <el-badge :value="urgentCount" :hidden="urgentCount === 0" class="badge">
            <el-icon><Bell /></el-icon>
          </el-badge>
        </div>

        <div class="todo-filters">
          <el-radio-group v-model="todoFilter" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">待办</el-radio-button>
            <el-radio-button label="overdue">逾期</el-radio-button>
          </el-radio-group>
        </div>

        <div class="todo-list">
          <div
            v-for="todo in filteredTodos"
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
                  <span class="todo-deadline">{{ formatDeadline(todo.deadline) }}</span>
                  <el-tag v-if="todo.caseName" size="small" type="info">
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

        <div v-if="filteredTodos.length === 0" class="empty-state">
          <el-empty description="暂无待办" />
        </div>
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
                placeholder="选择开始时间"
                value-format="YYYY-MM-DD HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="eventForm.endTime"
                type="datetime"
                placeholder="选择结束时间"
                value-format="YYYY-MM-DD HH:mm"
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
              :label="caseItem.name"
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
              :label="user.name"
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
            placeholder="选择截止时间"
            value-format="YYYY-MM-DD HH:mm"
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
              :label="caseItem.name"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="负责人">
          <el-select
            v-model="todoForm.assignee"
            filterable
            placeholder="选择负责人"
            style="width: 100%"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.name"
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
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, ArrowLeft, ArrowRight, Bell, Location, Briefcase, Clock, Document, Files, Scale
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import PriorityDot from '@/components/PriorityDot.vue'
import { getCalendarEvents, createEvent, updateEvent, deleteEvent } from '@/api/calendar'
import { getTodos, createTodo, updateTodo, deleteTodo } from '@/api/todo'

// 视图模式
const viewMode = ref('month')
const calendarDate = ref(new Date())

// 日程事件
const eventList = ref([])

// 待办事项
const todoList = ref([])
const todoFilter = ref('all')

// 对话框
const eventDialogVisible = ref(false)
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
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
}

// 预置数据
const caseList = ref([
  { id: '1', name: '张三诉李四买卖合同纠纷' },
  { id: '2', name: '王五离婚纠纷' }
])

const userList = ref([
  { id: '1', name: '张律师' },
  { id: '2', name: '李律师' },
  { id: '3', name: '小张' }
])

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

// 周视图日期
const weekDays = computed(() => {
  const days = []
  const today = new Date()
  const baseDate = new Date(calendarDate.value)  // 使用 calendarDate 而不是 today
  const startOfWeek = new Date(baseDate)
  startOfWeek.setDate(baseDate.getDate() - baseDate.getDay())

  for (let i = 0; i < 7; i++) {
    const day = new Date(startOfWeek)
    day.setDate(startOfWeek.getDate() + i)
    days.push({
      name: ['日', '一', '二', '三', '四', '五', '六'][day.getDay()],
      date: `${day.getMonth() + 1}/${day.getDate()}`,
      isToday: day.toDateString() === today.toDateString()
    })
  }
  return days
})

// 一天的小时数
const dayHours = Array.from({ length: 12 }, (_, i) => i + 8) // 8:00 - 19:00

// 过滤待办
const filteredTodos = computed(() => {
  let filtered = todoList.value

  if (todoFilter.value === 'pending') {
    filtered = filtered.filter(todo => !todo.completed)
  } else if (todoFilter.value === 'overdue') {
    filtered = filtered.filter(todo => new Date(todo.deadline) < new Date() && !todo.completed)
  }

  return filtered.sort((a, b) => {
    // 逾期置顶
    const aOverdue = isOverdue(a.deadline)
    const bOverdue = isOverdue(b.deadline)
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

// 判断是否逾期
const isOverdue = (deadline) => {
  return new Date(deadline) < new Date()
}

// 获取指定日期的事件
const getEventsForDate = (date) => {
  return eventList.value.filter(event => event.startTime.startsWith(date))
}

// 获取指定小时的事件
const getEventsForHour = (hour) => {
  return eventList.value.filter(event => {
    const eventHour = parseInt(event.startTime.split(' ')[1].split(':')[0])
    return eventHour === hour
  })
}

// 获取事件标签类型（红/橙/蓝/绿/紫）
const getEventTagType = (type) => {
  const typeMap = {
    'hearing': 'danger',      // 开庭/听证 → 红色
    'deadline': 'warning',    // 审限届满 → 橙色
    'filing': 'primary',      // 立案 → 蓝色
    'mediation': 'success',   // 调解/和解 → 绿色
    'evidence': 'info'        // 举证截止 → 紫色
  }
  return typeMap[type] || ''
}

// 获取事件颜色（用于边框和背景）
const getEventColor = (type) => {
  const colorMap = {
    'hearing': '#f56c6c',     // 红色
    'deadline': '#e6a23c',    // 橙色
    'filing': '#409eff',      // 蓝色
    'mediation': '#67c23a',   // 绿色
    'evidence': '#909399'     // 紫色
  }
  return colorMap[type] || '#909399'
}

// 获取事件图标
const getEventIcon = (type) => {
  const iconMap = {
    'hearing': Scale,
    'deadline': Clock,
    'filing': Document,
    'mediation': Bell,
    'evidence': Files
  }
  return iconMap[type] || Bell
}

// 获取待办样式类
const getTodoClass = (todo) => {
  if (todo.completed) return 'todo-completed'
  if (isOverdue(todo.deadline)) return 'todo-overdue'
  const days = Math.ceil((new Date(todo.deadline) - new Date()) / (1000 * 60 * 60 * 24))
  if (days <= 3) return 'todo-urgent'
  if (days <= 7) return 'todo-warning'
  return ''
}

// 格式化截止时间
const formatDeadline = (deadline) => {
  const date = new Date(deadline)
  const now = new Date()
  const days = Math.ceil((date - now) / (1000 * 60 * 60 * 24))

  if (days < 0) return `已逾期${Math.abs(days)}天`
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  if (days <= 7) return `${days}天后`
  return deadline
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
  ElMessage.info('查看事件详情')
}

// 新建日程
const handleCreateEvent = () => {
  isEditEvent.value = false
  Object.assign(eventForm, {
    id: null,
    title: '',
    type: '',
    startTime: new Date().toISOString().slice(0, 16).replace('T', ' '),
    endTime: '',
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
  Object.assign(todoForm, {
    id: null,
    title: '',
    priority: 'medium',
    deadline: new Date().toISOString().slice(0, 16).replace('T', ' '),
    caseId: '',
    assignee: '',
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
const handleTodoComplete = (todo) => {
  ElMessage.success('待办状态已更新')
}

// 提交日程
const handleSubmitEvent = async () => {
  try {
    await eventFormRef.value?.validate()

    if (isEditEvent.value) {
      await updateEvent(eventForm.id, eventForm)
      ElMessage.success('更新成功')
    } else {
      await createEvent(eventForm)
      ElMessage.success('创建成功')
    }

    eventDialogVisible.value = false
    // 重新加载数据
    await fetchEvents()
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

// 提交待办
const handleSubmitTodo = async () => {
  try {
    await todoFormRef.value?.validate()

    if (isEditTodo.value) {
      await updateTodo(todoForm.id, todoForm)
      ElMessage.success('更新成功')
    } else {
      await createTodo(todoForm)
      ElMessage.success('创建成功')
    }

    todoDialogVisible.value = false
    // 重新加载数据
    await fetchTodos()
  } catch (error) {
    if (error.message) {
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
      const today = new Date(calendarDate.value)
      const startOfWeek = new Date(today)
      startOfWeek.setDate(today.getDate() - today.getDay())
      const endOfWeek = new Date(startOfWeek)
      endOfWeek.setDate(startOfWeek.getDate() + 6)
      startDate = formatDateToString(startOfWeek)
      endDate = formatDateToString(endOfWeek)
    } else {
      // 日视图
      startDate = formatDateToString(new Date(calendarDate.value))
      endDate = formatDateToString(new Date(calendarDate.value))
    }

    const res = await getCalendarEvents({
      startDate,
      endDate
    })
    eventList.value = res.data?.records || res.data || []
  } catch (error) {
    console.error('获取日程事件失败:', error)
    ElMessage.error('获取日程事件失败')
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

// 初始化
fetchEvents()
fetchTodos()
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

    .week-view {
      background-color: #fff;
      padding: 20px;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .week-header {
        display: grid;
        grid-template-columns: 60px repeat(7, 1fr);
        gap: 10px;
        margin-bottom: 10px;
        padding-bottom: 10px;
        border-bottom: 1px solid #e4e7ed;

        .day-header {
          text-align: center;

          &.is-today {
            .day-name {
              color: #1890ff;
              font-weight: bold;
            }

            .day-date {
              color: #1890ff;
              font-weight: bold;
            }
          }

          .day-name {
            font-size: 12px;
            color: #909399;
          }

          .day-date {
            font-size: 14px;
            color: #333;
            margin-top: 4px;
          }
        }
      }

      .week-body {
        .hour-row {
          display: grid;
          grid-template-columns: 60px 1fr;
          gap: 10px;
          min-height: 50px;
          border-bottom: 1px solid #f0f0f0;

          .hour-label {
            font-size: 12px;
            color: #909399;
            text-align: center;
            padding-top: 10px;
          }

          .hour-events {
            display: flex;
            flex-direction: column;
            gap: 5px;

            .event-block {
              padding: 5px 10px;
              border-radius: 4px;
              cursor: pointer;
              font-size: 12px;
              border-left: 3px solid;
              transition: all 0.3s;

              &:hover {
                transform: translateX(2px);
                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
              }

              &.type-hearing {
                background-color: #fef0f0;
                color: #f56c6c;
                border-left-color: #f56c6c;
              }

              &.type-deadline {
                background-color: #fdf6ec;
                color: #e6a23c;
                border-left-color: #e6a23c;
              }

              &.type-filing {
                background-color: #ecf5ff;
                color: #409eff;
                border-left-color: #409eff;
              }

              &.type-mediation {
                background-color: #f0f9ff;
                color: #67c23a;
                border-left-color: #67c23a;
              }

              &.type-evidence {
                background-color: #f4f4f5;
                color: #909399;
                border-left-color: #909399;
              }
            }
          }
        }
      }
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

    .sidebar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px 20px;
      border-bottom: 1px solid #e4e7ed;

      h4 {
        margin: 0;
        font-size: 14px;
        color: #333;
      }

      .badge {
        font-size: 18px;
        color: #f56c6c;
      }
    }

    .todo-filters {
      padding: 10px 20px;
      border-bottom: 1px solid #e4e7ed;
    }

    .todo-list {
      flex: 1;
      overflow-y: auto;
      padding: 10px 20px;

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
        }

        &.todo-urgent {
          .todo-title {
            color: #f56c6c;
          }
        }

        &.todo-warning {
          .todo-title {
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

              .todo-deadline {
                color: #f56c6c;
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

    .empty-state {
      padding: 40px 20px;
      text-align: center;
    }
  }
}
</style>
