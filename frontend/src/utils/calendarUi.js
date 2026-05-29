/** 日历 UI 共享：五色标签、日期工具（工作台 / 日程页共用） */

export const CALENDAR_TYPE_LABELS = {
  HEARING: '开庭/听证',
  hearing: '开庭/听证',
  DEADLINE: '审限届满',
  deadline: '审限届满',
  FILING: '立案',
  filing: '立案',
  MEDIATION: '调解/和解',
  mediation: '调解/和解',
  EVIDENCE: '举证截止',
  evidence: '举证截止',
  OTHER: '其他',
  other: '其他'
}

export function calendarTypeLabel(t) {
  return CALENDAR_TYPE_LABELS[t] || t || '-'
}

/** Element Plus tag type：开庭红/审限橙/立案蓝/调解绿/举证紫 */
export function getEventTagType(type) {
  const key = (type || '').toLowerCase()
  const map = {
    hearing: 'danger',
    deadline: 'warning',
    filing: 'primary',
    mediation: 'success',
    evidence: 'info',
    danger: 'danger',
    warning: 'warning',
    primary: 'primary',
    success: 'success',
    info: 'info'
  }
  return map[key] || ''
}

export function formatDateToString(date) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function startOfWeek(date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - d.getDay())
  return d
}

export function endOfWeek(date) {
  const s = startOfWeek(date)
  const e = new Date(s)
  e.setDate(s.getDate() + 6)
  e.setHours(23, 59, 59, 999)
  return e
}

/** @returns {{ name: string, date: string, iso: string, isToday: boolean }[]} */
export function buildWeekDays(baseDate = new Date()) {
  const today = new Date()
  const start = startOfWeek(baseDate)
  const weekNames = ['日', '一', '二', '三', '四', '五', '六']
  const days = []
  for (let i = 0; i < 7; i++) {
    const day = new Date(start)
    day.setDate(start.getDate() + i)
    days.push({
      name: weekNames[day.getDay()],
      date: `${day.getMonth() + 1}/${day.getDate()}`,
      iso: formatDateToString(day),
      isToday: day.toDateString() === today.toDateString()
    })
  }
  return days
}

export function normalizeEventType(calendarType, color) {
  if (color) return color
  const map = {
    HEARING: 'hearing',
    DEADLINE: 'deadline',
    FILING: 'filing',
    MEDIATION: 'mediation',
    EVIDENCE: 'evidence'
  }
  return map[calendarType] || calendarType?.toLowerCase() || 'other'
}

export function mapApiEventToUi(event) {
  const start = event.startTime || ''
  return {
    id: event.id,
    date: formatDateToString(new Date(start)),
    title: event.title || calendarTypeLabel(event.calendarType),
    type: normalizeEventType(event.calendarType, event.color),
    startTime: start.replace('T', ' '),
    endTime: event.endTime ? event.endTime.replace('T', ' ') : '',
    data: event
  }
}

export function getEventsForDate(events, dateStr) {
  return (events || []).filter((e) => {
    const s = (e.startTime || e.date || '').replace('T', ' ')
    return s.startsWith(dateStr)
  })
}

export function countTodayByType(events, typeKeys) {
  const today = formatDateToString(new Date())
  const keys = new Set(typeKeys.map((k) => k.toLowerCase()))
  return getEventsForDate(events, today).filter((e) => {
    const t = (e.type || e.data?.calendarType || '').toLowerCase()
    return keys.has(t)
  }).length
}

/** 待办：是否已完成 */
export function isTodoCompleted(todo) {
  return todo.completed || todo.status === 'COMPLETED'
}

/** 待办：是否已逾期 */
export function isTodoOverdue(deadline) {
  return new Date(deadline) < new Date()
}

/** 待办视觉状态：done | overdue | urgent | warning | '' */
export function getTodoVisualState(todo) {
  if (isTodoCompleted(todo)) return 'done'
  if (isTodoOverdue(todo.deadline)) return 'overdue'
  const days = Math.ceil((new Date(todo.deadline) - new Date()) / 86400000)
  if (days <= 3) return 'urgent'
  if (days <= 7) return 'warning'
  return ''
}

/** 日程页侧栏待办 CSS 类名 */
export function calendarTodoClass(todo) {
  const state = getTodoVisualState(todo)
  if (!state) return ''
  if (state === 'done') return 'todo-completed'
  return `todo-${state}`
}

/** 人性化截止时间文案 */
export function formatTodoDeadline(deadline) {
  const date = new Date(deadline)
  const days = Math.ceil((date - new Date()) / 86400000)
  if (days < 0) return `已逾期${Math.abs(days)}天`
  if (days === 0) return '今天'
  if (days === 1) return '明天'
  if (days <= 7) return `${days}天后`
  return deadline
}
