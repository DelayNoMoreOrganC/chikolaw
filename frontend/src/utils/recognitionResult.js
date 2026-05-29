/**
 * 文书识别结果 → 展示文案 / 待办日程 / 导出
 */

const FIELD_LABELS = [
  ['documentType', '文书类型'],
  ['caseNumber', '案号'],
  ['courtName', '法院'],
  ['court', '法院'],
  ['caseReason', '案由'],
  ['caseCause', '案由'],
  ['plaintiffName', '原告'],
  ['defendantName', '被告'],
  ['hearingDate', '开庭时间'],
  ['hearingPlace', '开庭地点'],
  ['judgeName', '承办法官'],
  ['clerkName', '书记员'],
  ['judgmentDate', '判决/文书日期'],
  ['appealDeadline', '上诉期届满'],
  ['contactPhone', '联系电话']
]

export function getRecognitionFields(recognition) {
  if (!recognition) return []
  const rows = []
  const seen = new Set()
  for (const [key, label] of FIELD_LABELS) {
    const val = recognition[key]
    if (val == null || val === '' || seen.has(label)) continue
    seen.add(label)
    rows.push({ label, value: String(val) })
  }
  if (recognition.confidence != null) {
    rows.push({ label: '置信度', value: `${Math.round(recognition.confidence * 100)}%` })
  }
  return rows
}

export function formatRecognitionText(recognition) {
  const lines = ['—— 文书识别要素 ——', '']
  for (const { label, value } of getRecognitionFields(recognition)) {
    lines.push(`${label}：${value}`)
  }
  if (recognition?.ocrText?.trim()) {
    lines.push('', '—— OCR 原文摘要 ——', recognition.ocrText.trim().slice(0, 2000))
  }
  return lines.join('\n').trim()
}

export function recognitionDocTitle(recognition) {
  const type = recognition?.documentType || '文书识别'
  const caseNo = recognition?.caseNumber ? `_${recognition.caseNumber}` : ''
  return `${type}${caseNo}`
}

/** 解析开庭时间字符串为 Date */
export function parseHearingDateTime(hearingDate) {
  if (!hearingDate) return null
  const s = String(hearingDate).trim().replace('T', ' ')
  const normalized = s.replace(/\//g, '-')
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

function formatLocalDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

/** 根据识别结果构建开庭待办（无后端自动化时的兜底） */
export function buildHearingTodoPayload(recognition, { caseId, assigneeId }) {
  const caseNo = recognition.caseNumber || '案件'
  const hearing = parseHearingDateTime(recognition.hearingDate)
  const due = hearing || new Date(Date.now() + 7 * 24 * 3600 * 1000)
  return {
    title: `${caseNo} 开庭提醒`,
    description: [
      `文书类型：${recognition.documentType || '-'}`,
      `法院：${recognition.courtName || recognition.court || '-'}`,
      `开庭地点：${recognition.hearingPlace || '-'}`,
      `开庭时间：${recognition.hearingDate || '-'}`
    ].join('\n'),
    priority: 'high',
    deadline: formatLocalDateTime(due),
    assigneeId,
    caseId: caseId || undefined,
    reminder: true
  }
}

/** 开庭日程 DTO */
export function buildHearingCalendarPayload(recognition, { caseId }) {
  const caseNo = recognition.caseNumber || '案件'
  const start = parseHearingDateTime(recognition.hearingDate) || new Date(Date.now() + 7 * 24 * 3600 * 1000)
  const end = new Date(start.getTime() + 2 * 3600 * 1000)
  return {
    title: `${caseNo} 开庭`,
    calendarType: 'HEARING',
    startTime: formatLocalDateTime(start),
    endTime: formatLocalDateTime(end),
    location: recognition.hearingPlace || recognition.courtName || recognition.court || null,
    caseId: caseId || null,
    reminder: true,
    reminderMinutes: 1440
  }
}

export function formatBusinessLogicSummary(bl) {
  if (!bl) return ''
  if (bl.skipped) return `已跳过（${bl.reason || '不支持类型'}）`
  if (bl.success === false) return bl.error || bl.message || '自动化失败'
  const parts = []
  if (bl.caseId) parts.push(`案件#${bl.caseId}`)
  if (bl.todoId) parts.push(`待办#${bl.todoId}`)
  if (bl.calendarId) parts.push(`日程#${bl.calendarId}`)
  if (bl.answerTodoId) parts.push(`答辩待办#${bl.answerTodoId}`)
  if (bl.message) parts.push(bl.message)
  return parts.length ? parts.join('；') : '已执行'
}

export function hasAutomationDone(recognition) {
  const bl = recognition?.businessLogic
  if (!bl || bl.skipped) return false
  return bl.success !== false && (bl.todoId || bl.calendarId || bl.answerTodoId || bl.trialTodoId)
}
