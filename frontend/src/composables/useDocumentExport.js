import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportDocumentDocx } from '@/api/ai'

/**
 * 统一法律文书 TXT / DOCX 本地下载（AI 中心、案件文档 Tab 共用）
 */
export function useDocumentExport() {
  const exporting = ref(false)

  function downloadTxt(content, fileName = `法律文书_${Date.now()}.txt`) {
    if (!content?.trim()) {
      ElMessage.warning('没有可下载的内容')
      return false
    }
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName.endsWith('.txt') ? fileName : `${fileName}.txt`
    link.click()
    URL.revokeObjectURL(url)
    return true
  }

  async function downloadDocx({ content, title, fileName }) {
    if (!content?.trim()) {
      ElMessage.warning('没有可导出的内容')
      return false
    }
    exporting.value = true
    try {
      const blob = await exportDocumentDocx({
        content,
        title: title || '法律文书',
        fileName: fileName || `${title || '法律文书'}_${Date.now()}.docx`
      })
      const name =
        fileName && fileName.toLowerCase().endsWith('.docx')
          ? fileName
          : `${(fileName || title || '法律文书').replace(/\.docx$/i, '')}_${Date.now()}.docx`
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = name
      link.click()
      URL.revokeObjectURL(url)
      ElMessage.success('Word 文档已下载')
      return true
    } catch (e) {
      ElMessage.error('Word 导出失败：' + (e.message || e))
      return false
    } finally {
      exporting.value = false
    }
  }

  return { exporting, downloadTxt, downloadDocx }
}
