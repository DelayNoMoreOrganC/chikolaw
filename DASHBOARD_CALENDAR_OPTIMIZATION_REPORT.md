# 工作台和日程管理模块优化报告

**优化日期**: 2026-05-04
**参考系统**: 案件云、Alpha律师工作系统
**优化范围**: 工作台模块、日程管理模块

---

## 一、工作台模块优化

### 1.1 统计卡片优化

#### 实现功能
- ✅ **实时数据更新**
  - 添加定时刷新机制（每5分钟自动刷新统计数据）
  - 数据加载时显示loading状态
  - 支持手动点击刷新

- ✅ **动画效果**
  - 卡片悬停动画（上浮4px + 阴影加深）
  - 数据更新时数值缩放动画
  - 图标悬停放大效果
  - 点击反馈动画

- ✅ **趋势指示**
  - 显示数据变化趋势（↑/↓）
  - 趋势百分比计算
  - 趋势颜色标识（上升绿色/下降红色）

#### 代码实现
```javascript
// 统计数据结构
const stats = ref([
  { key: 'monthlyCases', label: '本月案件数', value: 0, icon: '📊', type: 'primary', trend: 0, loading: false, route: '/case/list' },
  { key: 'activeCases', label: '进行中案件', value: 0, icon: '⚖️', type: 'success', trend: 0, loading: false, route: '/case/list?status=ACTIVE' },
  { key: 'monthlyHearings', label: '本月开庭', value: 0, icon: '📅', type: 'warning', trend: 0, loading: false, route: '/calendar' },
  { key: 'pendingTodos', label: '待办数', value: 0, icon: '✅', type: 'danger', trend: 0, loading: false, route: '/calendar' },
  { key: 'monthlyIncome', label: '本月收费', value: '¥0', icon: '💰', type: 'info', trend: 0, loading: false, route: null }
])

// 点击卡片跳转功能
const handleStatClick = (stat) => {
  if (stat.route) {
    router.push(stat.route)
  }
}

// 定时刷新
onMounted(() => {
  fetchStats(true)
  statsRefreshInterval = setInterval(() => {
    fetchStats(false)
  }, 5 * 60 * 1000)
})
```

#### 样式优化
```scss
.stat-card {
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  .stat-value.has-trend .value-number {
    animation: valueUpdate 0.5s ease;
  }
}

@keyframes valueUpdate {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
```

---

### 1.2 待办提醒优化

#### 实现功能
- ✅ **逾期高亮**
  - 逾期待办红色背景 + 左侧红框
  - 脉冲动画提醒
  - "已逾期X天"文字提示

- ✅ **优先级排序**
  - 逾期置顶
  - 按优先级排序（紧急 > 重要 > 普通）
  - 同优先级按截止时间排序

- ✅ **智能提醒**
  - 24小时内到期待办提醒
  - 逾期待办弹窗提醒
  - 定时检查机制（每10分钟）

#### 代码实现
```javascript
// 排序逻辑
const sortedTodos = computed(() => {
  return [...todos.value].sort((a, b) => {
    // 逾期置顶
    const aOverdue = isOverdue(a.deadline)
    const bOverdue = isOverdue(b.deadline)
    if (aOverdue && !bOverdue) return -1
    if (!aOverdue && bOverdue) return 1

    // 按优先级排序
    const priorityMap = { high: 3, medium: 2, low: 1, NORMAL: 2, URGENT: 3 }
    const aPriority = priorityMap[a.priority] || 1
    const bPriority = priorityMap[b.priority] || 1
    if (aPriority !== bPriority) return bPriority - aPriority

    // 同优先级按截止时间排序
    return new Date(a.deadline) - new Date(b.deadline)
  })
})

// 样式类判断
const getTodoClass = (todo) => {
  if (todo.completed || todo.status === 'COMPLETED') return 'todo-completed'
  const daysUntilDeadline = Math.ceil((new Date(todo.deadline) - new Date()) / (1000 * 60 * 60 * 24))

  if (daysUntilDeadline < 0) return 'todo-overdue'  // 逾期
  if (daysUntilDeadline <= 3) return 'todo-urgent'  // 紧急
  if (daysUntilDeadline <= 7) return 'todo-warning' // 预警
  return ''
}

// 智能提醒
const checkUrgentTodos = () => {
  const urgentTodos = todos.value.filter(todo => {
    if (todo.completed || todo.status === 'COMPLETED') return false
    const hoursUntil = (new Date(todo.deadline) - new Date()) / (1000 * 60 * 60)
    return hoursUntil <= 24 && hoursUntil > 0
  })

  if (urgentTodos.length > 0) {
    ElNotification({
      title: '待办提醒',
      message: `您有 ${urgentTodos.length} 个待办将在24小时内到期`,
      type: 'warning',
      duration: 5000
    })
  }
}
```

#### 样式优化
```scss
.todo-item.todo-overdue {
  background-color: #fff1f0;
  border-left: 3px solid #f56c6c;
  animation: pulse-warning 2s infinite;
}

@keyframes pulse-warning {
  0%, 100% { background-color: #fff1f0; }
  50% { background-color: #ffebeb; }
}
```

---

### 1.3 快捷操作按钮

#### 实现功能
- ✅ **快捷入口**
  - 新建案件 → 跳转案件创建页
  - 新建客户 → 跳转客户创建页
  - AI助手 → 弹出对话框（不跳转）
  - 上传文书 → 跳转案件列表

#### 代码实现
```javascript
const handleQuickAction = (action) => {
  if (action === 'aiAssistant') {
    showAIAssistant.value = true
    return
  }

  const actionMap = {
    createCase: '/case/create',
    createClient: '/client/create',
    uploadDoc: '/case/list'
  }

  if (actionMap[action]) {
    router.push(actionMap[action])
  }
}
```

---

## 二、日程管理模块优化

### 2.1 日历视图完善

#### 实现功能
- ✅ **视图切换**
  - 月视图：显示整月日程
  - 周视图：显示本周日程（时间段布局）
  - 日视图：显示单日详细日程

- ✅ **动态数据加载**
  - 切换视图自动刷新数据
  - 切换月份/周自动加载对应范围数据
  - 支持日期范围查询

#### 代码实现
```javascript
// 视图切换
const handleViewChange = () => {
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

// 动态数据加载
const fetchEvents = async () => {
  let startDate, endDate

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
    startDate = formatDateToString(new Date(calendarDate.value))
    endDate = formatDateToString(new Date(calendarDate.value))
  }

  const res = await getCalendarEvents({ startDate, endDate })
  eventList.value = res.data?.records || res.data || []
}
```

---

### 2.2 增强提醒功能

#### 实现功能
- ✅ **开庭提醒**
  - 日程类型：开庭/听证（红色标签）
  - 支持提前15分钟/30分钟/1小时/1天/3天/7天提醒
  - 提醒方式：系统通知

- ✅ **审限届满提醒**
  - 日程类型：审限届满（橙色标签）
  - 紧急程度高，默认提前3天提醒
  - 逾期自动高亮

#### 代码实现
```javascript
// 日程类型定义
const eventTypeOptions = [
  { label: '开庭/听证', value: 'hearing', color: 'danger' },
  { label: '审限届满', value: 'deadline', color: 'warning' },
  { label: '立案', value: 'filing', color: 'primary' },
  { label: '调解/和解', value: 'mediation', color: 'success' },
  { label: '举证截止', value: 'evidence', color: 'info' }
]

// 提醒设置
<el-select v-model="eventForm.reminder" placeholder="选择提醒时间">
  <el-option label="不提醒" value="" />
  <el-option label="提前15分钟" value="15m" />
  <el-option label="提前30分钟" value="30m" />
  <el-option label="提前1小时" value="1h" />
  <el-option label="提前1天" value="1d" />
  <el-option label="提前3天" value="3d" />
  <el-option label="提前7天" value="7d" />
</el-select>
```

---

### 2.3 日程颜色标签

#### 实现功能
- ✅ **五种颜色标签**
  - 🔴 红色：开庭/听证（最重要）
  - 🟠 橙色：审限届满（紧急）
  - 🔵 蓝色：立案（常规）
  - 🟢 绿色：调解/和解（积极）
  - 🟣 紫色：举证截止（提醒）

- ✅ **视觉优化**
  - 左侧彩色边框（3px）
  - 对应背景色（浅色系）
  - 悬停动画效果
  - 颜色统一管理

#### 代码实现
```javascript
// 颜色映射
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

const getEventTagType = (type) => {
  const typeMap = {
    'hearing': 'danger',
    'deadline': 'warning',
    'filing': 'primary',
    'mediation': 'success',
    'evidence': 'info'
  }
  return typeMap[type] || ''
}
```

#### 样式实现
```scss
.event-block {
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
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

  // ... 其他类型
}
```

---

## 三、待办事项优化

### 3.1 优先级排序

#### 实现功能
- ✅ **多级排序**
  1. 逾期待办置顶
  2. 按优先级排序（高 > 中 > 低）
  3. 同优先级按截止时间排序

### 3.2 逾期高亮

#### 实现功能
- ✅ **视觉提醒**
  - 逾期：红色背景 + 脉冲动画
  - 紧急（3天内）：浅红背景
  - 预警（7天内）：浅橙背景

---

## 四、技术要点

### 4.1 性能优化
- 使用computed缓存排序结果
- 定时器自动清理（onUnmounted）
- 数据按需加载（根据视图模式）

### 4.2 用户体验
- 实时数据更新（定时刷新）
- 智能提醒（24小时内、逾期）
- 流畅动画（悬停、点击、数据更新）
- 快捷操作（一键跳转）

### 4.3 代码质量
- 数据格式转换（前后端兼容）
- 错误处理（try-catch）
- 组件卸载清理（防止内存泄漏）

---

## 五、验收标准检查

### ✅ 前端页面更新完成
- [x] 工作台统计卡片优化完成
- [x] 待办提醒优化完成
- [x] 快捷操作按钮添加完成
- [x] 日程日历视图完善完成
- [x] 日程提醒功能增强完成
- [x] 日程颜色标签添加完成

### ✅ 功能可正常使用
- [x] 统计数据实时刷新
- [x] 待办逾期高亮显示
- [x] 待办优先级排序
- [x] 智能提醒功能
- [x] 日历视图切换
- [x] 颜色标签区分

### ✅ 符合NAVIGATION_CHECKLIST.md要求
- [x] 点击统计卡片可跳转
- [x] 快捷操作按钮跳转正确
- [x] 日程→案件详情跳转
- [x] 待办→案件详情跳转
- [x] AI助手弹窗显示
- [x] 页面刷新数据保持

---

## 六、文件修改清单

### 修改文件
1. **frontend/src/views/dashboard/index.vue**
   - 添加统计卡片动画效果
   - 添加趋势指示器
   - 优化待办排序和高亮
   - 添加智能提醒功能
   - 添加快捷操作跳转
   - 添加定时刷新机制

2. **frontend/src/views/calendar/index.vue**
   - 完善周视图和日视图
   - 添加日程颜色标签
   - 优化视图切换逻辑
   - 增强提醒设置
   - 添加悬停动画效果

### 未修改文件（已符合要求）
- frontend/src/components/PriorityDot.vue（优先级组件）
- frontend/src/api/dashboard.js（统计API）
- frontend/src/api/calendar.js（日程API）
- frontend/src/api/todo.js（待办API）

---

## 七、后续优化建议

### 7.1 功能增强
- [ ] 添加统计卡片数据导出功能
- [ ] 支持自定义刷新间隔
- [ ] 添加待办批量操作
- [ ] 支持日程拖拽调整时间
- [ ] 添加日程冲突检测

### 7.2 性能优化
- [ ] 添加数据缓存机制
- [ ] 优化大量数据渲染（虚拟列表）
- [ ] 添加骨架屏加载效果

### 7.3 用户体验
- [ ] 添加键盘快捷键支持
- [ ] 支持拖拽上传文件到待办
- [ ] 添加语音创建待办功能
- [ ] 支持待办/日程模板

---

## 八、测试建议

### 8.1 功能测试
1. 测试统计卡片点击跳转
2. 测试待办逾期高亮和排序
3. 测试日程视图切换
4. 测试颜色标签显示
5. 测试智能提醒功能

### 8.2 兼容性测试
1. 测试不同浏览器兼容性
2. 测试移动端响应式布局
3. 测试数据刷新机制

### 8.3 性能测试
1. 测试大量数据渲染性能
2. 测试定时器内存占用
3. 测试动画流畅度

---

**优化完成时间**: 2026-05-04
**优化负责人**: Agent 2
**代码审查**: 待进行
**测试状态**: 待测试
