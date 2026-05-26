/**
 * AI 功能术语与文书类型别名（与 PRD §4.1 / §4.2、后端 DocumentTemplateType 对齐）
 */

/** 用户可见：文档识别（不用「OCR」作为主标签） */
export const AI_RECOGNITION = {
  featureName: '文书智能识别',
  shortName: '智能识别',
  dialogTitle: '文书智能识别',
  actionLabel: '文书智能识别',
  fillTitle: '文书智能识别填充',
  fillButton: '文书智能识别填充',
  processingHint: '正在识别文书（Vision + 要素提取）…',
  successTitle: '识别成功',
  failMessage: '文书识别失败',
  apiHint: 'POST /api/ai/documents/recognize'
}

export const AI_DOCUMENT_GEN = {
  featureName: 'AI 文书生成',
  dialogTitle: 'AI 文书生成',
  actionLabel: 'AI 文书生成'
}

/** 标准四类 + 兼容别名（code 为后端 canonical） */
export const LEGAL_DOCUMENT_TYPES = [
  {
    code: 'COMPLAINT',
    label: '起诉状',
    description: '民事、行政、刑事自诉案件的起诉文书',
    route: 'DOCUMENT',
    aliases: ['complaint', '起诉状', '民事起诉状']
  },
  {
    code: 'DEFENSE_STATEMENT',
    label: '答辩状',
    description: '被告针对起诉状的答辩文书',
    route: 'DOCUMENT',
    aliases: ['defense', 'defense_statement', '答辩状', '民事答辩状']
  },
  {
    code: 'BRIEF',
    label: '代理词',
    description: '庭审代理词',
    route: 'DOCUMENT',
    aliases: ['brief', 'legalbrief', 'agent_brief', 'opinion', '代理词', '庭审代理词']
  },
  {
    code: 'LEGAL_OPINION',
    label: '法律意见书',
    description: '就特定法律问题出具的专业意见',
    route: 'DOCUMENT',
    aliases: ['legal_opinion', 'legalopinion', '法律意见书', '法律意见']
  },
  {
    code: 'LAWYER_LETTER',
    label: '律师函',
    description: '催告、声明等律师函（兼容旧版生成路径）',
    route: 'LEGACY_DOCUMENT',
    aliases: ['letter', 'lawyer_letter', '律师函', '律函']
  }
]

const ALIAS_TO_CODE = new Map()
for (const t of LEGAL_DOCUMENT_TYPES) {
  ALIAS_TO_CODE.set(t.code.toUpperCase(), t.code)
  ALIAS_TO_CODE.set(t.label, t.code)
  for (const a of t.aliases || []) {
    ALIAS_TO_CODE.set(String(a).toUpperCase(), t.code)
    ALIAS_TO_CODE.set(String(a), t.code)
  }
}

/**
 * 将 UI / 旧接口传入的类型归一为后端 canonical code
 */
export function normalizeDocumentTypeCode(raw) {
  if (raw == null || raw === '') return ''
  const trimmed = String(raw).trim()
  const upper = trimmed.toUpperCase()
  if (ALIAS_TO_CODE.has(upper)) return ALIAS_TO_CODE.get(upper)
  if (ALIAS_TO_CODE.has(trimmed)) return ALIAS_TO_CODE.get(trimmed)
  return upper
}

export function getDocumentTypeLabel(code) {
  const normalized = normalizeDocumentTypeCode(code)
  const hit = LEGAL_DOCUMENT_TYPES.find((t) => t.code === normalized)
  return hit ? hit.label : code || '未知文书'
}

export function getDocumentTypeOptions(includeLegacy = true) {
  return LEGAL_DOCUMENT_TYPES.filter(
    (t) => includeLegacy || t.route !== 'LEGACY_DOCUMENT'
  ).map((t) => ({
    value: t.code,
    label: t.label,
    description: t.description,
    route: t.route
  }))
}

/** 功能日志展示名（替代 OCR 字样） */
export function formatAiFunctionType(type) {
  const map = {
    OCR: '文书识别',
    DOCUMENT_RECOGNITION: '文书识别',
    DOCUMENT_GENERATION: '文书生成',
    LEGAL_CHAT: '法律问答',
    GENERAL_CHAT: '智能对话',
    RAG: '知识库问答',
    EXTRACT: '要素提取'
  }
  return map[type] || type || '-'
}
