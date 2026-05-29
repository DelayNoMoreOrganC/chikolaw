import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { previewCaseDocument, previewCaseDocumentHtml, downloadCaseDocument } from '@/api/case'

const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
const OFFICE_EXTS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']

export function extFromFilename(name) {
  if (!name) return ''
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

/**
 * 案件文档预览/下载（文档中心、案件文档 Tab 共用）
 */
export function useDocumentPreview() {
  const visible = ref(false)
  const loading = ref(false)
  const previewFile = ref(null)
  const previewMode = ref('')
  const previewBlobUrl = ref('')
  const previewHtmlContent = ref('')

  const revokePreviewBlob = () => {
    if (previewBlobUrl.value) {
      window.URL.revokeObjectURL(previewBlobUrl.value)
      previewBlobUrl.value = ''
    }
    previewMode.value = ''
    previewFile.value = null
    previewHtmlContent.value = ''
  }

  const normalizeFile = (row) => ({
    id: row.id,
    name: row.documentName || row.name,
    type: row.type || extFromFilename(row.documentName || row.name),
    contentType: row.contentType
  })

  const loadPreviewHtml = async (caseId, file) => {
    loading.value = true
    try {
      const html = await previewCaseDocumentHtml(caseId, file.id)
      previewHtmlContent.value = typeof html === 'string' ? html : (html?.data || '')
      if (!previewHtmlContent.value) throw new Error('预览内容为空')
    } finally {
      loading.value = false
    }
  }

  const loadPreviewBlob = async (caseId, file, mimeHint) => {
    loading.value = true
    try {
      const data = await previewCaseDocument(caseId, file.id)
      const ext = file.type
      const mime = mimeHint
        || file.contentType
        || (ext === 'pdf' ? 'application/pdf' : IMAGE_EXTS.includes(ext) ? `image/${ext === 'jpg' ? 'jpeg' : ext}` : 'application/octet-stream')
      const blob = new Blob([data], { type: mime })
      previewBlobUrl.value = window.URL.createObjectURL(blob)
    } finally {
      loading.value = false
    }
  }

  const downloadFile = async (caseId, row) => {
    const file = normalizeFile(row)
    try {
      const data = await downloadCaseDocument(caseId, file.id)
      const blob = new Blob([data])
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = file.name || 'document'
      link.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('下载成功')
    } catch (e) {
      ElMessage.error('下载失败：' + (e.message || '未知错误'))
    }
  }

  const preview = async (caseId, row) => {
    if (!caseId || !row?.id) {
      ElMessage.warning('缺少案件或文档信息')
      return
    }
    const file = normalizeFile(row)
    const ext = file.type

    if (IMAGE_EXTS.includes(ext)) {
      previewFile.value = file
      previewMode.value = 'image'
      visible.value = true
      await loadPreviewBlob(caseId, file, `image/${ext === 'jpg' ? 'jpeg' : ext}`)
    } else if (ext === 'pdf') {
      previewFile.value = file
      previewMode.value = 'pdf'
      visible.value = true
      await loadPreviewBlob(caseId, file, 'application/pdf')
    } else if (OFFICE_EXTS.includes(ext)) {
      previewFile.value = file
      previewMode.value = 'html'
      visible.value = true
      try {
        await loadPreviewHtml(caseId, file)
      } catch (e) {
        visible.value = false
        ElMessage.error('Office 预览失败：' + (e.message || '请下载后查看'))
      }
    } else {
      try {
        await ElMessageBox.confirm('该文件类型暂不支持在线预览，是否下载后查看？', '提示', {
          confirmButtonText: '下载',
          cancelButtonText: '取消',
          type: 'info'
        })
        await downloadFile(caseId, row)
      } catch {
        /* cancel */
      }
    }
  }

  const close = () => {
    visible.value = false
    revokePreviewBlob()
  }

  return {
    visible,
    loading,
    previewFile,
    previewMode,
    previewBlobUrl,
    previewHtmlContent,
    preview,
    downloadFile,
    close,
    revokePreviewBlob
  }
}
