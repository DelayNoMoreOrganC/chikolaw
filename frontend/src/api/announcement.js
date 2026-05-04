import request from '@/utils/request'

// 获取公告列表
export function getAnnouncements(params) {
  return request({
    url: '/announcement',
    method: 'get',
    params
  })
}

// 创建公告
export function createAnnouncement(data) {
  return request({
    url: '/announcement',
    method: 'post',
    data
  })
}

// 更新公告
export function updateAnnouncement(id, data) {
  return request({
    url: `/announcement/${id}`,
    method: 'put',
    data
  })
}

// 删除公告
export function deleteAnnouncement(id) {
  return request({
    url: `/announcement/${id}`,
    method: 'delete'
  })
}

// 获取公告详情
export function getAnnouncementDetail(id) {
  return request({
    url: `/announcement/${id}`,
    method: 'get'
  })
}

// 标记为已读
export function markAsRead(id) {
  return request({
    url: `/announcement/${id}/read`,
    method: 'put'
  })
}

// 获取未读公告数量
export function getUnreadCount() {
  return request({
    url: '/announcement/unread-count',
    method: 'get'
  })
}

// 获取目标范围列表
export function getTargetScopes() {
  return request({
    url: '/announcement/target-scopes',
    method: 'get'
  })
}
