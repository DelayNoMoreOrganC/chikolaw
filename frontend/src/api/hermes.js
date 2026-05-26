import request from '@/utils/request'

export function getHermesStatus() {
  return request({
    url: '/hermes/status',
    method: 'get'
  })
}

export function hermesChat(body) {
  return request({
    url: '/hermes/chat',
    method: 'post',
    data: body
  })
}
