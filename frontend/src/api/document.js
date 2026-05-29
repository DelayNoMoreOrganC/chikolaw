import request from '@/utils/request'

/** 文档中心分页列表（含 stats） */
export function getDocumentCenterList(params) {
  return request({
    url: '/documents',
    method: 'get',
    params
  })
}

/** 兼容：全量列表 */
export function getAllDocuments(params) {
  return request({
    url: '/documents/all',
    method: 'get',
    params
  })
}

export function getDocument(id) {
  return request({
    url: `/documents/${id}`,
    method: 'get'
  })
}

export function deleteCaseDocument(caseId, docId) {
  return request({
    url: `/cases/${caseId}/documents/${docId}`,
    method: 'delete'
  })
}
