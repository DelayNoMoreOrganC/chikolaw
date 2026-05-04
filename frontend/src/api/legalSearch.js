import request from '@/utils/request'

export function searchRegulations(params) {
  return request({
    url: '/legal-search/regulations',
    method: 'get',
    params
  })
}

export function getLegalCategories() {
  return request({
    url: '/legal-search/categories',
    method: 'get'
  })
}

export function askLegalQuestion(question) {
  return request({
    url: '/legal-search/ask',
    method: 'post',
    data: { question }
  })
}
