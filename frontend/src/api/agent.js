import request from '@/utils/request'

/** 内嵌 Agent 运行时：builtin / openclaw / hermes */
export function getAgentRuntimeStatus() {
  return request({
    url: '/agent/runtime/status',
    method: 'get'
  })
}
