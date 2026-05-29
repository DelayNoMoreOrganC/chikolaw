import request, { aiHttp } from '@/utils/request'

/**
 * 核心卷宗录入：传文件 → AI 分析 → 登记备注 → 归入案件档案夹
 */
export function processCaseIntake(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  if (options.caseId) {
    formData.append('caseId', options.caseId)
  }
  if (options.remark) {
    formData.append('remark', options.remark)
  }

  return aiHttp({
    url: '/case-intake/process',
    method: 'post',
    data: formData,
    timeout: 180000
  })
}

export function attachCaseIntake(file, caseId, remark) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('caseId', caseId)
  if (remark) {
    formData.append('remark', remark)
  }
  return aiHttp({
    url: '/case-intake/attach',
    method: 'post',
    data: formData,
    timeout: 120000
  })
}

/** 从服务端暂存记录挂接到案件（NEEDS_CASE 后无需重传） */
export function attachCaseIntakeFromPending(pendingId, caseId, remark) {
  const formData = new FormData()
  formData.append('pendingId', pendingId)
  formData.append('caseId', caseId)
  if (remark) {
    formData.append('remark', remark)
  }
  return aiHttp({
    url: '/case-intake/attach-pending',
    method: 'post',
    data: formData,
    timeout: 120000
  })
}

/** 未匹配案件时发起立案审批 */
export function createCaseFilingApplication(pendingId, options = {}) {
  const formData = new FormData()
  formData.append('pendingId', pendingId)
  if (options.title) formData.append('title', options.title)
  if (options.content) formData.append('content', options.content)
  if (options.approverId) formData.append('approverId', options.approverId)
  return aiHttp({
    url: '/case-intake/filing-application',
    method: 'post',
    data: formData
  })
}

export function listIntakePending() {
  return request({
    url: '/case-intake/pending',
    method: 'get'
  })
}

/** 立案审批通过后预填新建案件 */
export function getIntakePrefill(pendingId) {
  return request({
    url: `/case-intake/pending/${pendingId}/prefill`,
    method: 'get'
  })
}

export function searchCasesForIntake(q, limit = 20) {
  return request({
    url: '/case-intake/cases/search',
    method: 'get',
    params: { q, limit }
  })
}
