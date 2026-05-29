import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { initChunkedUpload, uploadChunk, mergeChunks } from '@/api/upload'

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024

/**
 * 大文件分片上传（>50MB 或弱网场景）
 */
export function useChunkedUpload(options = {}) {
  const chunkSize = options.chunkSize || DEFAULT_CHUNK_SIZE
  const uploading = ref(false)
  const progress = ref(0)

  async function uploadFile(file, { onProgress } = {}) {
    if (!file) {
      throw new Error('未选择文件')
    }
    uploading.value = true
    progress.value = 0
    try {
      const initRes = await initChunkedUpload(file.name, file.size, file.type)
      const uploadId = initRes.data
      if (!uploadId) {
        throw new Error(initRes.message || '初始化上传失败')
      }
      const totalChunks = Math.ceil(file.size / chunkSize)
      for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        const blob = file.slice(start, end)
        const chunkRes = await uploadChunk(uploadId, i, blob)
        const pct = chunkRes.data?.progressPercent
        progress.value = pct != null ? pct : Math.round(((i + 1) / totalChunks) * 100)
        onProgress?.(progress.value)
      }
      const mergeRes = await mergeChunks(uploadId)
      ElMessage.success('文件上传完成')
      return mergeRes.data
    } finally {
      uploading.value = false
    }
  }

  return { uploading, progress, uploadFile }
}
