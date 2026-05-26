import request, { aiHttp } from '@/utils/request'

// 获取知识库列表
export function getKnowledgeList(params) {
  return request({
    url: '/knowledge/list',
    method: 'get',
    params
  })
}

// 搜索知识库
export function searchKnowledge(keyword, params = {}) {
  return request({
    url: '/knowledge/search',
    method: 'get',
    params: {
      q: encodeURIComponent(keyword),
      ...params
    }
  })
}

// 获取知识库详情
export function getKnowledgeDetail(id) {
  return request({
    url: `/knowledge/${id}`,
    method: 'get'
  })
}

// 创建知识库文章
export function createKnowledge(data) {
  return request({
    url: '/knowledge',
    method: 'post',
    data
  })
}

// 更新知识库文章
export function updateKnowledge(id, data) {
  return request({
    url: `/knowledge/${id}`,
    method: 'put',
    data
  })
}

// 删除知识库文章
export function deleteKnowledge(id) {
  return request({
    url: `/knowledge/${id}`,
    method: 'delete'
  })
}

// 获取知识库分类
export function getKnowledgeCategories() {
  return request({
    url: '/knowledge/categories',
    method: 'get'
  })
}

// AI知识问答（后端向量/关键词检索 + LLM，与 RAGKnowledgeController 对齐）
export function askAI(question) {
  return aiHttp({
    url: '/knowledge/rag/search',
    method: 'post',
    data: { question }
  })
}
