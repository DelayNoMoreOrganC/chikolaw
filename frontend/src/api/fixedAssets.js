import request from '@/utils/request'

// 获取固定资产列表
export function getFixedAssets(params) {
  return request({
    url: '/fixed-assets',
    method: 'get',
    params
  })
}

// 创建固定资产
export function createFixedAsset(data) {
  return request({
    url: '/fixed-assets',
    method: 'post',
    data
  })
}

// 更新固定资产
export function updateFixedAsset(id, data) {
  return request({
    url: `/fixed-assets/${id}`,
    method: 'put',
    data
  })
}

// 删除固定资产
export function deleteFixedAsset(id) {
  return request({
    url: `/fixed-assets/${id}`,
    method: 'delete'
  })
}

// 获取固定资产详情
export function getFixedAssetDetail(id) {
  return request({
    url: `/fixed-assets/${id}`,
    method: 'get'
  })
}

// 资产领用
export function assignAsset(id, data) {
  return request({
    url: `/fixed-assets/${id}/assign`,
    method: 'post',
    data
  })
}

// 资产归还
export function returnAsset(id, data) {
  return request({
    url: `/fixed-assets/${id}/return`,
    method: 'post',
    data
  })
}

// 资产维修
export function repairAsset(id, data) {
  return request({
    url: `/fixed-assets/${id}/repair`,
    method: 'post',
    data
  })
}

// 资产报废
export function scrapAsset(id, data) {
  return request({
    url: `/fixed-assets/${id}/scrap`,
    method: 'post',
    data
  })
}
