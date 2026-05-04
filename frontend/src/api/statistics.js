import request from '@/utils/request'

// 获取统计卡片数据
export function getStatsCards(params) {
  return request({
    url: '/statistics/cards',
    method: 'get',
    params
  })
}

// 获取案件数量趋势
export function getCaseTrend(params) {
  return request({
    url: '/statistics/case-trends',
    method: 'get',
    params
  })
}

// 获取案件类型分布
export function getCaseTypeDistribution(params) {
  return request({
    url: '/statistics/case-types',
    method: 'get',
    params
  })
}

// 获取收费统计
export function getFeeStatistics(params) {
  return request({
    url: '/statistics/fees',
    method: 'get',
    params
  })
}

// 获取律师业绩排名
export function getLawyerPerformance(params) {
  return request({
    url: '/statistics/lawyer-performance',
    method: 'get',
    params
  })
}

// 获取案件胜诉率
export function getWinRate(params) {
  return request({
    url: '/statistics/win-rate',
    method: 'get',
    params
  })
}

// 获取收款率统计
export function getCollectionRate(params) {
  return request({
    url: '/statistics/collection-rate',
    method: 'get',
    params
  })
}

// 导出Excel
export function exportExcel(data) {
  return request({
    url: '/statistics/export/excel',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

// 导出PDF
export function exportPdf(data) {
  return request({
    url: '/statistics/export/pdf',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
