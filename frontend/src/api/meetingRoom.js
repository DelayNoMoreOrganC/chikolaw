import request from '@/utils/request'

// 获取会议室列表
export function getMeetingRooms(params) {
  return request({
    url: '/meeting-room',
    method: 'get',
    params
  })
}

// 创建会议室
export function createMeetingRoom(data) {
  return request({
    url: '/meeting-room',
    method: 'post',
    data
  })
}

// 更新会议室
export function updateMeetingRoom(id, data) {
  return request({
    url: `/meeting-room/${id}`,
    method: 'put',
    data
  })
}

// 删除会议室
export function deleteMeetingRoom(id) {
  return request({
    url: `/meeting-room/${id}`,
    method: 'delete'
  })
}

// 获取会议室详情
export function getMeetingRoomDetail(id) {
  return request({
    url: `/meeting-room/${id}`,
    method: 'get'
  })
}

// 启用/禁用会议室
export function toggleMeetingRoomStatus(id, status) {
  return request({
    url: `/meeting-room/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 获取会议室预定列表
export function getMeetingBookings(params) {
  return request({
    url: '/meeting-booking',
    method: 'get',
    params
  })
}

// 创建会议室预定
export function createMeetingBooking(data) {
  return request({
    url: '/meeting-booking',
    method: 'post',
    data
  })
}

// 更新会议室预定
export function updateMeetingBooking(id, data) {
  return request({
    url: `/meeting-booking/${id}`,
    method: 'put',
    data
  })
}

// 取消会议室预定
export function cancelMeetingBooking(id) {
  return request({
    url: `/meeting-booking/${id}/cancel`,
    method: 'put'
  })
}

// 获取会议室预定详情
export function getMeetingBookingDetail(id) {
  return request({
    url: `/meeting-booking/${id}`,
    method: 'get'
  })
}
