import request from '@/utils/request'

// 获取审批列表
export function getApprovalList(params) {
  return request({
    url: '/approval',
    method: 'get',
    params
  })
}

// 获取审批详情
export function getApprovalDetail(id) {
  return request({
    url: `/approval/${id}`,
    method: 'get'
  })
}

// 创建审批
export function createApproval(data) {
  return request({
    url: '/approval',
    method: 'post',
    data
  })
}

// 同意审批
export function approveApproval(id, data) {
  return request({
    url: `/approval/${id}/approve`,
    method: 'put',
    data
  })
}

// 驳回审批
export function rejectApproval(id, data) {
  return request({
    url: `/approval/${id}/reject`,
    method: 'put',
    data
  })
}

// 转审
export function transferApproval(id, data) {
  return request({
    url: `/approval/${id}/transfer`,
    method: 'put',
    data
  })
}

// 撤回审批
export function withdrawApproval(id) {
  return request({
    url: `/approval/${id}/withdraw`,
    method: 'put'
  })
}

// 催办
export function urgeApproval(id) {
  return request({
    url: `/approval/${id}/urge`,
    method: 'put'
  })
}

// 审批类型（与后端枚举一致）
export function getApprovalTypes() {
  return request({
    url: '/approval/types',
    method: 'get'
  })
}
