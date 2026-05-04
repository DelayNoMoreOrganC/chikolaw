import request from '@/utils/request'

// 获取办公用品列表
export function getOfficeSupplies(params) {
  return request({
    url: '/office-supplies',
    method: 'get',
    params
  })
}

// 创建办公用品
export function createOfficeSupply(data) {
  return request({
    url: '/office-supplies',
    method: 'post',
    data
  })
}

// 更新办公用品
export function updateOfficeSupply(id, data) {
  return request({
    url: `/office-supplies/${id}`,
    method: 'put',
    data
  })
}

// 删除办公用品
export function deleteOfficeSupply(id) {
  return request({
    url: `/office-supplies/${id}`,
    method: 'delete'
  })
}

// 获取办公用品详情
export function getOfficeSupplyDetail(id) {
  return request({
    url: `/office-supplies/${id}`,
    method: 'get'
  })
}

// 获取库存不足的物品
export function getLowStockItems() {
  return request({
    url: '/office-supplies/low-stock',
    method: 'get'
  })
}

// 入库
export function stockIn(id, data) {
  return request({
    url: `/office-supplies/${id}/inbound`,
    method: 'post',
    data
  })
}

// 出库
export function stockOut(id, data) {
  return request({
    url: `/office-supplies/${id}/outbound`,
    method: 'post',
    data
  })
}
