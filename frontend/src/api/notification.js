import request from '@/utils/request'

export function getNotificationList(params) {
  return request({
    url: '/notification',
    method: 'get',
    params
  })
}

export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

export function getNotificationSummary() {
  return request({
    url: '/notification/summary',
    method: 'get'
  })
}

export function getUnreadNotifications(limit = 10) {
  return request({
    url: '/notification/unread',
    method: 'get',
    params: { limit }
  })
}

export function markAsRead(id) {
  return request({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

export function markAllAsRead() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

export function deleteNotification(id) {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}

export function getNotificationCategories() {
  return request({
    url: '/notification/categories',
    method: 'get'
  })
}
