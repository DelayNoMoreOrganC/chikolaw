import request from '@/utils/request'

/** 类案智能检索 POST /api/case-search/similar */
export function searchSimilarCases(payload) {
  return request({
    url: '/case-search/similar',
    method: 'post',
    data: payload
  })
}

/** 按案件 ID 检索相似案例 */
export function searchSimilarByCaseId(caseId, limit = 10) {
  return request({
    url: `/case-search/similar/${caseId}`,
    method: 'get',
    params: { limit }
  })
}
