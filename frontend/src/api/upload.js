import request from '@/utils/request'

export function initChunkedUpload(fileName, fileSize, mimeType) {
  const formData = new FormData()
  formData.append('fileName', fileName)
  formData.append('fileSize', String(fileSize))
  formData.append('mimeType', mimeType || 'application/octet-stream')
  return request({
    url: '/upload/init',
    method: 'post',
    data: formData
  })
}

export function uploadChunk(uploadId, chunkIndex, chunkBlob) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunk', chunkBlob)
  return request({
    url: '/upload/chunk',
    method: 'post',
    data: formData
  })
}

export function mergeChunks(uploadId) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  return request({
    url: '/upload/merge',
    method: 'post',
    data: formData
  })
}

export function getUploadProgress(uploadId) {
  return request({
    url: `/upload/progress/${uploadId}`,
    method: 'get'
  })
}

export function cancelUpload(uploadId) {
  return request({
    url: `/upload/${uploadId}`,
    method: 'delete'
  })
}
