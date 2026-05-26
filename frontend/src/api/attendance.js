import request from '@/utils/request'

export function getMyAttendanceRecords() {
  return request({ url: '/attendance/my', method: 'get' })
}

export function getPendingAttendance() {
  return request({ url: '/attendance/pending', method: 'get' })
}

export function createAttendance(data) {
  return request({ url: '/attendance', method: 'post', data })
}

export function updateAttendance(id, data) {
  return request({ url: `/attendance/${id}`, method: 'put', data })
}

export function deleteAttendance(id) {
  return request({ url: `/attendance/${id}`, method: 'delete' })
}

export function approveAttendance(id, status, comment) {
  return request({
    url: `/attendance/${id}/approve`,
    method: 'put',
    params: { status, comment }
  })
}

export function getAttendanceMonthlyStats(month) {
  return request({ url: '/attendance/stats', method: 'get', params: { month } })
}
