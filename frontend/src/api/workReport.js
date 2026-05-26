import request from '@/utils/request'

export function listWorkReports(params) {
  return request({
    url: '/work-reports',
    method: 'get',
    params
  })
}

export function getMyWorkReports(params) {
  return request({
    url: '/work-reports/my',
    method: 'get',
    params
  })
}

export function getPendingWorkReports(params) {
  return request({
    url: '/work-reports/pending',
    method: 'get',
    params
  })
}

export function getWorkReport(id) {
  return request({ url: `/work-reports/${id}`, method: 'get' })
}

export function createWorkReport(data) {
  return request({ url: '/work-reports', method: 'post', data })
}

export function updateWorkReport(id, data) {
  return request({ url: `/work-reports/${id}`, method: 'put', data })
}

export function submitWorkReport(id) {
  return request({ url: `/work-reports/${id}/submit`, method: 'put' })
}

export function reviewWorkReport(id, status, comment) {
  return request({
    url: `/work-reports/${id}/review`,
    method: 'put',
    params: { status, comment }
  })
}

export function deleteWorkReport(id) {
  return request({ url: `/work-reports/${id}`, method: 'delete' })
}
