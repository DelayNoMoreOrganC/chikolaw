import request from '@/utils/request'
import { toApiDateTime, formatDisplayDateTime } from '@/utils/datetime'

const PRIORITY_TO_API = {
  high: 'URGENT',
  medium: 'IMPORTANT',
  low: 'NORMAL',
  urgent: 'URGENT',
  important: 'IMPORTANT',
  normal: 'NORMAL'
}

const PRIORITY_FROM_API = {
  URGENT: 'high',
  HIGH: 'high',
  IMPORTANT: 'medium',
  MEDIUM: 'medium',
  NORMAL: 'low',
  LOW: 'low'
}

function mapPriorityToApi(priority) {
  if (!priority) return 'NORMAL'
  const key = String(priority).toLowerCase()
  return PRIORITY_TO_API[key] || String(priority).toUpperCase()
}

function mapPriorityFromApi(priority) {
  if (!priority) return 'medium'
  return PRIORITY_FROM_API[String(priority).toUpperCase()] || 'medium'
}

/**
 * 数据格式转换工具
 */

/**
 * 将后端TodoDTO转换为前端格式
 */
function convertTodoFromBackend(backendTodo) {
  if (!backendTodo) return null

  return {
    ...backendTodo,
    // 将status转换为completed布尔值（兼容前端组件）
    completed: backendTodo.status === 'COMPLETED',
    // 保留原始status字段
    _status: backendTodo.status,
    priority: mapPriorityFromApi(backendTodo.priority),
    // deadline字段别名（前端使用deadline）
    deadline: formatDisplayDateTime(backendTodo.dueDate) || backendTodo.dueDate,
    // remark字段别名（前端使用remark）
    remark: backendTodo.description,
    // assignee字段别名（前端使用assignee）
    assignee: backendTodo.assigneeId
  }
}

/**
 * 批量转换待办列表
 */
function convertTodoListFromBackend(backendList) {
  if (!Array.isArray(backendList)) return []
  return backendList.map(convertTodoFromBackend)
}

// 获取待办列表（兼容前端调用）
export function getTodoList(params) {
  // 如果传了status参数，转换为后端支持的接口
  if (params && params.status) {
    return request({
      url: '/todos/search',
      method: 'get',
      params: {
        assignee: params.assigneeId,
        status: params.status,
        sort: params.sortBy || 'urgency'
      }
    }).then(res => {
      // 转换返回数据
      if (res.data && Array.isArray(res.data)) {
        res.data = convertTodoListFromBackend(res.data)
      }
      return res
    })
  }

  // 否则使用分页接口
  return request({
    url: '/todos',
    method: 'get',
    params: {
      page: params.page || 0,
      size: params.size || 10,
      assigneeId: params.assigneeId
    }
  }).then(res => {
    // 转换返回数据
    if (res.data && res.data.records) {
      res.data.records = convertTodoListFromBackend(res.data.records)
    }
    return res
  })
}

// 创建待办（数据格式转换）
export function createTodo(data) {
  const assigneeId = data.assignee ?? data.assigneeId
  const caseId = data.caseId != null && data.caseId !== '' ? Number(data.caseId) : null
  const todoData = {
    title: data.title,
    description: data.remark || data.description || '',
    status: 'PENDING',
    priority: mapPriorityToApi(data.priority),
    dueDate: toApiDateTime(data.deadline || data.dueDate),
    assigneeId: assigneeId != null && assigneeId !== '' ? Number(assigneeId) : null,
    caseId: Number.isFinite(caseId) ? caseId : null,
    reminder: !!data.reminder
  }

  if (todoData.assigneeId == null) {
    return Promise.reject(new Error('请选择负责人，或重新登录后再试'))
  }

  Object.keys(todoData).forEach(key => {
    if (todoData[key] === undefined || todoData[key] === null || todoData[key] === '') {
      delete todoData[key]
    }
  })

  return request({
    url: '/todos',
    method: 'post',
    data: todoData
  })
}

// 更新待办（数据格式转换）
export function updateTodo(id, data) {
  // 转换前端数据格式到后端DTO格式
  const todoData = {}

  if (data.title !== undefined) todoData.title = data.title
  if (data.remark !== undefined) todoData.description = data.remark
  if (data.description !== undefined) todoData.description = data.description
  if (data.status !== undefined) todoData.status = data.status.toUpperCase()
  if (data.priority !== undefined) todoData.priority = mapPriorityToApi(data.priority)
  if (data.deadline !== undefined) todoData.dueDate = toApiDateTime(data.deadline)
  if (data.dueDate !== undefined) todoData.dueDate = toApiDateTime(data.dueDate)
  if (data.assignee !== undefined) todoData.assigneeId = Number(data.assignee)
  if (data.assigneeId !== undefined) todoData.assigneeId = Number(data.assigneeId)
  if (data.caseId !== undefined) {
    const cid = data.caseId != null && data.caseId !== '' ? Number(data.caseId) : null
    todoData.caseId = Number.isFinite(cid) ? cid : null
  }
  if (data.reminder !== undefined) todoData.reminder = !!data.reminder
  // 处理completed布尔值转换为status
  if (data.completed !== undefined) {
    todoData.status = data.completed ? 'COMPLETED' : 'PENDING'
  }

  return request({
    url: `/todos/${id}`,
    method: 'put',
    data: todoData
  })
}

// 删除待办
export function deleteTodo(id) {
  return request({
    url: `/todos/${id}`,
    method: 'delete'
  })
}

// 查询待办详情
export function getTodo(id) {
  return request({
    url: `/todos/${id}`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data) {
      res.data = convertTodoFromBackend(res.data)
    }
    return res
  })
}

// 查询用户的待办
export function getTodosByAssignee(assigneeId) {
  return request({
    url: `/todos/assignee/${assigneeId}`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 查询用户的待办（按优先级排序）
export function getTodosByAssigneeWithPrioritySort(assigneeId) {
  return request({
    url: `/todos/assignee/${assigneeId}/priority`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 查询待办的待办
export function getPendingTodos(assigneeId) {
  return request({
    url: `/todos/assignee/${assigneeId}/pending`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 查询已完成的待办
export function getCompletedTodos(assigneeId) {
  return request({
    url: `/todos/assignee/${assigneeId}/completed`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 查询逾期的待办
export function getOverdueTodos(assigneeId) {
  return request({
    url: `/todos/assignee/${assigneeId}/overdue`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 查询案件的待办
export function getTodosByCase(caseId) {
  return request({
    url: `/todos/case/${caseId}`,
    method: 'get'
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 按条件查询待办（PRD要求的格式）
export function getTodosByFilter(
    assignee,
    status,
    sort) {
  return request({
    url: '/todos/search',
    method: 'get',
    params: {
      assignee,
      status,
      sort
    }
  }).then(res => {
    // 转换返回数据
    if (res.data && Array.isArray(res.data)) {
      res.data = convertTodoListFromBackend(res.data)
    }
    return res
  })
}

// 分页查询待办
export function getTodos(
    page = 0,
    size = 10,
    assigneeId) {
  return request({
    url: '/todos',
    method: 'get',
    params: {
      page,
      size,
      assigneeId
    }
  }).then(res => {
    // 转换返回数据
    if (res.data && res.data.records) {
      res.data.records = convertTodoListFromBackend(res.data.records)
    }
    return res
  })
}

// 完成待办（通过更新状态实现）
export function completeTodo(id) {
  return updateTodo(id, { status: 'completed' })
}

