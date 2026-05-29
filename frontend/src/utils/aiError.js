/**
 * 统一解析 AI / 卷宗 / 文书生成错误文案
 */
export function formatAiError(error) {
  if (!error) return '未知错误'
  const data = error.response?.data
  if (typeof data === 'string' && data) return data
  if (data?.message) return data.message
  if (error.message) return error.message
  return String(error)
}

/** 是否像配置/密钥类错误（提示检查 ZHIPU_API_KEY） */
export function isAiConfigError(message) {
  if (!message) return false
  const m = message.toLowerCase()
  return (
    m.includes('zhipu') ||
    m.includes('api_key') ||
    m.includes('api key') ||
    m.includes('401') ||
    m.includes('unauthorized') ||
    m.includes('未配置') ||
    m.includes('glm') ||
    m.includes('智谱')
  )
}

export function buildAiErrorHint(message, { showDiagnosticsLink = false } = {}) {
  const base = formatAiError(message)
  if (isAiConfigError(base)) {
    return `${base}\n请检查 backend/.env 中的 ZHIPU_API_KEY 与 LAWFIRM_AI_MODE=cloud-glm。`
  }
  if (showDiagnosticsLink) {
    return `${base}\n可展开页顶「AI 服务状态」查看路由与最近调用。`
  }
  return base
}

/** 页面内展示 AI 错误（单行摘要，避免与拦截器重复弹窗） */
export function notifyAiError(error, { fallback = 'AI 处理失败' } = {}) {
  const detail = buildAiErrorHint(formatAiError(error), { showDiagnosticsLink: true })
  const firstLine = detail.split('\n')[0] || fallback
  ElMessage.error(firstLine)
  return detail
}
