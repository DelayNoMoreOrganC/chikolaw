import request, { aiHttp } from '@/utils/request'
import {
  normalizeDocumentTypeCode,
  getDocumentTypeLabel,
  getDocumentTypeOptions
} from '@/config/ai-terminology'

export { normalizeDocumentTypeCode, getDocumentTypeLabel, getDocumentTypeOptions }

/**
 * 文书智能识别（Vision + LLM 要素提取，非独立 OCR 产品名）
 */

/**
 * 智能识别法院文书（新接口）
 * @param {File} file - 文档文件
 * @param {Number} caseId - 关联案件ID（可选）
 * @returns {Promise}
 */
/**
 * @param {File} file
 * @param {number|null} caseId
 * @param {boolean} executeBusinessLogic 是否执行业务自动化（待办/建案）
 */
export function recognizeLegalDocument(file, caseId, executeBusinessLogic = true) {
  const formData = new FormData()
  formData.append('file', file)
  if (caseId) {
    formData.append('caseId', caseId)
  }
  formData.append('executeBusinessLogic', String(executeBusinessLogic))

  return aiHttp({
    url: '/ai/documents/recognize',
    method: 'post',
    data: formData
  })
}

/**
 * 批量识别文档（新接口）
 * @param {File[]} files - 文档文件列表
 * @param {Number} caseId - 关联案件ID（可选）
 * @returns {Promise}
 */
export function recognizeLegalDocumentsBatch(files, caseId) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (caseId) {
    formData.append('caseId', caseId)
  }

  return aiHttp({
    url: '/ai/documents/recognize-batch',
    method: 'post',
    data: formData
  })
}

// AI提取（保留旧接口以兼容）
export function extractInfo(data) {
  return request({
    url: '/ai/extract',
    method: 'post',
    data
  })
}

// 自动填充案件
export function autoFillCase(caseId, data) {
  return request({
    url: `/ai/auto-fill/${caseId}`,
    method: 'post',
    data
  })
}

// 生成文书（兼容旧路径 /ai/generate-doc，documentType 请传 canonical code）
/** 将文书正文导出为 docx（二进制） */
export function exportDocumentDocx({ content, title, fileName }) {
  return request({
    url: '/ai/generate-doc/export-docx',
    method: 'post',
    data: { content, title, fileName },
    responseType: 'blob'
  })
}

export function generateDoc(data) {
  const payload = { ...data }
  if (payload.documentType) {
    payload.documentType = normalizeDocumentTypeCode(payload.documentType)
  }
  if (payload.templateType) {
    payload.documentType = normalizeDocumentTypeCode(payload.templateType)
    delete payload.templateType
  }
  return aiHttp({
    url: '/ai/generate-doc',
    method: 'post',
    data: payload,
    timeout: 300000
  })
}

/** 支持的文书类型（与后端 /ai/documents/types 对齐） */
export function getLegalDocumentTypes() {
  return request({
    url: '/ai/documents/types',
    method: 'get'
  })
}

// AI 通用对话（走后端路由与可选云端降级）
export function aiChat(data) {
  return aiHttp({
    url: '/ai/assist',
    method: 'post',
    data: { message: data.message }
  })
}

// 案件上下文对话
export function caseChat(caseId, data) {
  return aiHttp({
    url: `/ai/case-chat/${caseId}`,
    method: 'post',
    data
  })
}

// 获取AI使用日志
export function getAiLogs(params) {
  return request({
    url: '/ai/logs/user',
    method: 'get',
    params
  })
}

/** AI 可观测性快照（管理员） */
export function getAiDiagnostics() {
  return request({
    url: '/ai/diagnostics',
    method: 'get'
  })
}

// 获取AI配置
export function getAiConfig() {
  return request({
    url: '/ai/config',
    method: 'get'
  })
}

// 更新AI配置
export function updateAiConfig(data) {
  return request({
    url: '/ai/config',
    method: 'put',
    data
  })
}
