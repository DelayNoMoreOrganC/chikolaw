import request from '@/utils/request'

export function getApprovalWorkflow(approvalType) {
  return request({
    url: '/approval/workflow',
    method: 'get',
    params: { approvalType }
  })
}

export function saveApprovalWorkflow(approvalType, steps) {
  return request({
    url: '/approval/workflow',
    method: 'put',
    params: { approvalType },
    data: steps
  })
}
