import request from '@/utils/request'

// 获取客户列表
export function getClientList(params) {
  return request({
    url: '/clients',
    method: 'get',
    params
  })
}

// 获取客户详情
export function getClientDetail(id) {
  return request({
    url: `/clients/${id}`,
    method: 'get'
  })
}

// 创建客户
export function createClient(data) {
  return request({
    url: '/clients',
    method: 'post',
    data
  })
}

// 更新客户
export function updateClient(id, data) {
  return request({
    url: `/clients/${id}`,
    method: 'put',
    data
  })
}

// 删除客户
export function deleteClient(id) {
  return request({
    url: `/clients/${id}`,
    method: 'delete'
  })
}

// 搜索客户
export function searchClients(keyword) {
  return request({
    url: '/clients/search',
    method: 'get',
    params: { keyword }
  })
}

// 获取客户的案件
export function getClientCases(id) {
  return request({
    url: `/clients/${id}/cases`,
    method: 'get'
  })
}

// 获取沟通记录
export function getCommunications(clientId, params) {
  return request({
    url: `/clients/${clientId}/communications`,
    method: 'get',
    params
  })
}

// 创建沟通记录
export function createCommunication(clientId, data) {
  return request({
    url: `/clients/${clientId}/communications`,
    method: 'post',
    data
  })
}

// 更新沟通记录
export function updateCommunication(clientId, communicationId, data) {
  return request({
    url: `/clients/${clientId}/communications/${communicationId}`,
    method: 'put',
    data
  })
}

// 删除沟通记录
export function deleteCommunication(clientId, communicationId) {
  return request({
    url: `/clients/${clientId}/communications/${communicationId}`,
    method: 'delete'
  })
}

/** Excel 批量导入客户 */
export function importClients(file, skipConflictRows = true) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/clients/import',
    method: 'post',
    params: { skipConflictRows },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 新建客户前：按名称检查利冲（含相似名称提示） */
export function checkClientNameConflict(clientName) {
  return request({
    url: '/conflict-check/check-client',
    method: 'post',
    params: { clientName }
  })
}

// 利益冲突检索
export function conflictCheck(id) {
  return request({
    url: `/clients/${id}/conflict-check`,
    method: 'get'
  })
}

// 别名函数，保持向后兼容
export function checkConflict(id) {
  return conflictCheck(id)
}
