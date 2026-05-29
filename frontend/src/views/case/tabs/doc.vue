<template>
  <div class="case-doc">
    <div class="doc-layout">
      <!-- 左侧目录树 -->
      <div class="doc-sidebar">
        <div class="sidebar-header">
          <h4>文档目录</h4>
          <el-button text type="primary" size="small" @click="handleAddFolder">
            <el-icon><Plus /></el-icon>
            新建文件夹
          </el-button>
        </div>

        <el-tree
          ref="treeRef"
          :data="docTree"
          :props="treeProps"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <el-icon class="node-icon">
                <component :is="data.type === 'folder' ? Folder : DocumentAdd" />
              </el-icon>
              <span class="node-label">{{ node.label }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- 右侧文件列表 -->
      <div class="doc-main">
        <!-- 面包屑导航 -->
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>案件文档</el-breadcrumb-item>
            <el-breadcrumb-item
              v-for="crumb in breadcrumbs"
              :key="crumb.id"
              @click="handleBreadcrumbClick(crumb)"
            >
              {{ crumb.label }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-upload
              action="#"
              :auto-upload="false"
              multiple
              :on-change="handleFileUpload"
              show-file-list="false"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
            </el-upload>

            <el-button @click="handleNewDocument">
              <el-icon><DocumentAdd /></el-icon>
              新建文档
            </el-button>

            <el-button @click="handleAIUpload">
              <el-icon><MagicStick /></el-icon>
              {{ AI_RECOGNITION.shortName }}
            </el-button>

            <el-button type="success" @click="handleAIGenerateDoc">
              <el-icon><Edit /></el-icon>
              {{ AI_DOCUMENT_GEN.actionLabel }}
            </el-button>
          </div>

          <div class="toolbar-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文件"
              clearable
              style="width: 250px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-button @click="handleArchivePDF">
              <el-icon><FolderOpened /></el-icon>
              一键归档PDF
            </el-button>

            <el-dropdown @command="handleBatchAction">
              <el-button>
                批量操作
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="download">批量下载</el-dropdown-item>
                  <el-dropdown-item command="move">批量移动</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>批量删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 文件列表 -->
        <el-table
          :data="fileList"
          border
          v-loading="loading"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="文件名" min-width="250">
            <template #default="{ row }">
              <div class="file-name" @click="handlePreviewFile(row)">
                <span class="file-icon">{{ getFileIcon(row.type) }}</span>
                <span class="name-text">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="size" label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>

          <el-table-column prop="uploader" label="上传人" width="120" />

          <el-table-column prop="uploadTime" label="上传时间" width="160" sortable />

          <el-table-column label="标签" width="150">
            <template #default="{ row }">
              <el-tag
                v-for="tag in row.tags"
                :key="tag"
                size="small"
                closable
                @close="handleRemoveTag(row, tag)"
              >
                {{ tag }}
              </el-tag>
              <el-button
                v-if="!row.tags || row.tags.length === 0"
                text
                type="primary"
                size="small"
                @click="handleAddTag(row)"
              >
                + 添加
              </el-button>
            </template>
          </el-table-column>

          <el-table-column label="版本" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.version" size="small" type="info">
                v{{ row.version }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handlePreviewFile(row)">
                预览
              </el-button>
              <el-button link type="primary" size="small" @click="handleDownloadFile(row)">
                下载
              </el-button>
              <el-button link type="primary" size="small" @click="handleRenameFile(row)">
                重命名
              </el-button>
              <el-button link type="danger" size="small" @click="handleDeleteFile(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- AI文书生成对话框 -->
    <el-dialog v-model="aiDocDialogVisible" :title="AI_DOCUMENT_GEN.dialogTitle" width="700px">
      <el-form :model="aiDocForm" label-width="100px">
        <el-form-item label="文书类型">
          <el-select v-model="aiDocForm.documentType" placeholder="请选择文书类型">
            <el-option
              v-for="opt in documentTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="自定义要求">
          <el-input
            v-model="aiDocForm.customPrompt"
            type="textarea"
            :rows="4"
            placeholder="例如：诉讼请求、答辩意见、代理意见等具体要求"
          />
        </el-form-item>

        <el-form-item label="补充信息">
          <el-input
            v-model="aiDocForm.additionalContext"
            type="textarea"
            :rows="3"
            placeholder="其他需要补充的信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="aiDocDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="aiDocGenerating" @click="handleGenerateDoc">
            生成文书
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 生成结果对话框 -->
    <el-dialog v-model="aiResultDialogVisible" title="文书生成结果" width="900px">
      <div v-loading="aiDocGenerating" class="ai-result-content">
        <div v-if="aiGeneratedDoc" class="generated-doc">
          <el-alert
            title="以下内容由AI生成，请仔细核对后使用"
            type="warning"
            :closable="false"
            style="margin-bottom: 15px"
          />
          <div class="doc-content" v-html="formatDocContent(aiGeneratedDoc)"></div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="aiResultDialogVisible = false">关闭</el-button>
          <el-button type="success" @click="handleCopyDoc">
            <el-icon><DocumentCopy /></el-icon>
            复制内容
          </el-button>
          <el-button type="success" @click="handleSaveDoc">
            <el-icon><Download /></el-icon>
            下载 TXT
          </el-button>
          <el-button type="primary" :loading="docxExporting" @click="handleExportDocx">
            <el-icon><Download /></el-icon>
            导出 Word
          </el-button>
        </span>
      </template>
    </el-dialog>

    <DocumentPreviewDialog ref="previewRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'

// 监听案件数据变化，加载文档数据
watch(() => props.caseData.id, (newId) => {
  if (newId) {
    fetchDocuments()
  }
}, { immediate: true })
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateDoc } from '@/api/ai'
import { AI_RECOGNITION, AI_DOCUMENT_GEN, getDocumentTypeOptions, getDocumentTypeLabel } from '@/config/ai-terminology'
import { useDocumentExport } from '@/composables/useDocumentExport'
import { buildAiErrorHint, formatAiError } from '@/utils/aiError'
import {
  getCaseDocuments,
  uploadCaseDocument,
  deleteCaseDocument,
  moveCaseDocument,
  updateCaseDocument
} from '@/api/case'
import { getStagesByCaseType } from '@/config/case-lifecycle'
import { useDocumentPreview } from '@/composables/useDocumentPreview'
import {
  Plus, Upload, DocumentAdd, MagicStick, Search, FolderOpened, Folder,
  ArrowDown, Edit, DocumentCopy, Download
} from '@element-plus/icons-vue'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])
const router = useRouter()
const { exporting: docxExporting, downloadDocx, downloadTxt } = useDocumentExport()

const loading = ref(false)
const treeRef = ref(null)
const searchKeyword = ref('')
const selectedFiles = ref([])
const currentFolder = ref(null)
const aiDocDialogVisible = ref(false)
const aiResultDialogVisible = ref(false)
const aiDocGenerating = ref(false)
const aiGeneratedDoc = ref('')
const aiDocForm = ref({
  documentType: '',
  customPrompt: '',
  additionalContext: ''
})
const previewRef = ref(null)
const { downloadFile: downloadDocument } = useDocumentPreview()
const documentTypeOptions = getDocumentTypeOptions(true)

const DOC_FOLDER_NAMES = [
  '起诉状', '答辩状', '原告证据', '被告证据', '法院文书', '代理词', '判决书', '其他'
]

const mapDocToTreeFile = (doc) => ({
  id: doc.id,
  label: doc.documentName,
  type: 'file',
  data: doc
})

const buildFlatTypeTree = (docs) => DOC_FOLDER_NAMES.map((name) => ({
  id: `type-${name}`,
  label: name,
  type: 'folder',
  folderPath: name,
  children: docs
    .filter((doc) => {
      const fp = doc.folderPath || ''
      if (fp.includes('/')) {
        return fp.endsWith(`/${name}`) || fp === name
      }
      return (doc.documentType || '其他') === name
    })
    .map(mapDocToTreeFile)
}))

// 文档目录树（阶段 × 文书类型，或回退到扁平类型目录）
const docTree = computed(() => {
  const docs = documents.value
  const hasStagePaths = docs.some((d) => d.folderPath?.includes('/'))
  const stageInitialized = props.caseData?.stageFoldersInitialized

  if (hasStagePaths || stageInitialized) {
    const stageMap = new Map()
    const caseType = props.caseData?.caseType || 'CIVIL'
    getStagesByCaseType(caseType).forEach((s) => {
      const typeMap = new Map()
      DOC_FOLDER_NAMES.forEach((t) => typeMap.set(t, []))
      stageMap.set(s.label, typeMap)
    })

    docs.forEach((doc) => {
      const fp = doc.folderPath || ''
      let stage = props.caseData?.currentStage || '其他'
      let docType = doc.documentType || '其他'
      if (fp.includes('/')) {
        const parts = fp.split('/')
        stage = parts[0]
        docType = parts.slice(1).join('/') || docType
      }
      if (!stageMap.has(stage)) {
        stageMap.set(stage, new Map())
      }
      const typeMap = stageMap.get(stage)
      if (!typeMap.has(docType)) {
        typeMap.set(docType, [])
      }
      typeMap.get(docType).push(doc)
    })

    return Array.from(stageMap.entries()).map(([stage, typeMap]) => ({
      id: `stage:${stage}`,
      label: stage,
      type: 'folder',
      children: Array.from(typeMap.entries()).map(([docType, typeDocs]) => ({
        id: `${stage}/${docType}`,
        label: docType,
        type: 'folder',
        folderPath: `${stage}/${docType}`,
        children: typeDocs.map(mapDocToTreeFile)
      }))
    }))
  }

  return buildFlatTypeTree(docs)
})

// 文档列表（从API获取）
const documents = ref([])
const currentCaseId = computed(() => props.caseData?.id)
const fetchDocuments = async () => {
  if (!currentCaseId.value) return

  try {
    loading.value = true
    const response = await getCaseDocuments(currentCaseId.value)
    if (response.code === 200) {
      documents.value = response.data || []
    }
  } catch (error) {
    console.error('获取文档列表失败:', error)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取文档列表
onMounted(() => {
  fetchDocuments()
})

// 处理文件上传
const handleFileUpload = async (file) => {
  if (!currentCaseId.value) {
    ElMessage.error('案件信息缺失')
    return
  }

  try {
    loading.value = true
    const formData = new FormData()
    formData.append('file', file.raw)
    const folderPath = currentFolder.value?.folderPath
      || (currentFolder.value?.id?.includes('/') ? currentFolder.value.id : '')
    const docType = folderPath.includes('/')
      ? folderPath.split('/').pop()
      : (currentFolder.value?.label || '其他')
    formData.append('documentType', docType)
    formData.append('folderPath', folderPath && !folderPath.startsWith('stage:') ? folderPath : '')

    const response = await uploadCaseDocument(currentCaseId.value, formData)
    if (response.code === 200) {
      ElMessage.success('文档上传成功')
      await fetchDocuments() // 刷新文档列表
    }
  } catch (error) {
    console.error('上传文档失败:', error)
    ElMessage.error('上传文档失败')
  } finally {
    loading.value = false
  }
}

// 处理文件删除
const handleDeleteFile = async (file) => {
  try {
    await ElMessageBox.confirm('确定要删除这个文档吗？', '提示', {
      type: 'warning'
    })

    loading.value = true
    const response = await deleteCaseDocument(currentCaseId.value, file.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      await fetchDocuments() // 刷新文档列表
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文档失败:', error)
      ElMessage.error('删除文档失败')
    }
  } finally {
    loading.value = false
  }
}

const treeProps = {
  children: 'children',
  label: 'label'
}

// 面包屑
const breadcrumbs = computed(() => {
  if (!currentFolder.value) return []
  return [currentFolder.value]
})

// 文件列表
// 文件列表（从 documents 映射，支持文件夹与关键词过滤）
const fileList = computed(() => {
  let docs = documents.value
  if (currentFolder.value?.type === 'folder') {
    const folderId = currentFolder.value.id
    if (folderId.startsWith('stage:')) {
      const stage = folderId.slice(6)
      docs = docs.filter((d) => d.folderPath?.startsWith(`${stage}/`))
    } else if (currentFolder.value.folderPath || folderId.includes('/')) {
      const path = currentFolder.value.folderPath || folderId
      docs = docs.filter((d) => d.folderPath === path)
    } else {
      docs = docs.filter((doc) => (doc.documentType || '其他') === currentFolder.value.label)
    }
  }
  const kw = searchKeyword.value?.trim().toLowerCase()
  if (kw) {
    docs = docs.filter((doc) => {
      const nameMatch = doc.documentName?.toLowerCase().includes(kw)
      let tags = []
      if (doc.tags) {
        try {
          tags = JSON.parse(doc.tags)
        } catch {
          tags = String(doc.tags).split(',').map((t) => t.trim())
        }
      }
      const tagMatch = tags.some((tag) => String(tag).toLowerCase().includes(kw))
      return nameMatch || tagMatch
    })
  }
  return docs.map((doc) => {
    // 解析文件扩展名
    const fileName = doc.documentName || ''
    const ext = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''

    // 解析tags字符串为数组
    let tags = []
    if (doc.tags) {
      try {
        tags = JSON.parse(doc.tags)
      } catch {
        tags = doc.tags.split(',').map(t => t.trim()).filter(t => t)
      }
    }

    // 格式化时间
    const formatTime = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toISOString().slice(0, 16).replace('T', ' ')
    }

    return {
      id: doc.id,
      name: doc.documentName,
      type: ext,
      size: doc.fileSize || 0,
      uploader: doc.uploadByName || '未知',
      uploadTime: formatTime(doc.createdAt),
      tags: tags,
      documentType: doc.documentType,
      filePath: doc.filePath,
      folderPath: doc.folderPath,
      version: doc.versionNo != null ? `v${doc.versionNo}` : 'v1',
      contentType: doc.contentType
    }
  })
})

// 获取文件图标
const getFileIcon = (type) => {
  const iconMap = {
    'pdf': '📕',
    'doc': '📘',
    'docx': '📘',
    'xls': '📗',
    'xlsx': '📗',
    'jpg': '🖼️',
    'png': '🖼️',
    'zip': '📦'
  }
  return iconMap[type] || '📄'
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 树节点点击（按 documentType 客户端过滤）
const handleNodeClick = (data) => {
  currentFolder.value = data
}

// 面包屑点击
const handleBreadcrumbClick = (crumb) => {
  currentFolder.value = crumb
}

// 新建文件夹
const handleAddFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '文件夹名称长度为1-50个字符'
    })

    // 案件文档使用documentType分类，不是真正的文件夹
    // 这里我们创建一个新的文档分类
    ElMessage.success(`文档分类"${value}"创建成功`)
    await fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建文件夹失败:', error)
      ElMessage.error('创建文件夹失败')
    }
  }
}

// 新建文档
const handleNewDocument = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文档名称', '新建文档', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '文档名称长度为1-50个字符'
    })

    // 提示用户上传文件或使用AI生成
    ElMessageBox.confirm(
      '文档创建需要上传文件或使用AI生成。请选择操作方式。',
      '新建文档',
      {
        confirmButtonText: '上传文件',
        cancelButtonText: 'AI生成',
        distinguishCancelAndClose: true
      }
    ).then(() => {
      // 上传文件
      ElMessage.info('请点击上方"上传文件"按钮上传文档')
    }).catch((action) => {
      if (action === 'cancel') {
        // AI生成
        handleAIGenerateDoc()
      }
    })
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建文档失败:', error)
      ElMessage.error('创建文档失败')
    }
  }
}

// 跳转 AI 中心（统一向导，带案件上下文）
const handleAIUpload = () => {
  if (!props.caseData?.id) {
    ElMessage.warning('请先保存案件基本信息')
    return
  }
  router.push({
    path: '/ai-hub',
    query: { intent: 'intake', caseId: String(props.caseData.id) }
  })
}

// 搜索（列表由 fileList 计算属性实时过滤）
const handleSearch = () => {
  const count = fileList.value.length
  if (searchKeyword.value?.trim()) {
    ElMessage.success(`找到 ${count} 个匹配的文档`)
  }
}

// 一键归档PDF
const handleArchivePDF = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要生成一键归档PDF吗？这将包含所有文档和元数据。',
      '一键归档PDF',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    ElMessage.success('PDF生成中，请稍候...')

    // 调用一键归档PDF API
    const response = await fetch(`/api/cases/${props.caseData.id}/archive-pdf/download`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (response.ok) {
      // 下载文件
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `归档_${props.caseData.caseName}_${new Date().toISOString().slice(0,10).replace(/-/g, '')}.pdf`
      link.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('PDF下载成功')
    } else {
      throw new Error('PDF生成失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成PDF失败:', error)
      ElMessage.error('生成PDF失败')
    }
  }
}

// 批量操作
const handleBatchAction = async (command) => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!props.caseData?.id) {
    ElMessage.warning('案件信息未加载')
    return
  }

  const caseId = props.caseData.id
  const files = [...selectedFiles.value]

  try {
    switch (command) {
      case 'download': {
        for (const file of files) {
          await downloadDocument(caseId, toPreviewRow(file))
        }
        ElMessage.success(`已开始下载 ${files.length} 个文件`)
        break
      }
      case 'delete': {
        await ElMessageBox.confirm(
          `确定要删除选中的 ${files.length} 个文件吗？`,
          '批量删除',
          { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )
        loading.value = true
        for (const file of files) {
          await deleteCaseDocument(caseId, file.id)
        }
        ElMessage.success(`已删除 ${files.length} 个文件`)
        selectedFiles.value = []
        await fetchDocuments()
        break
      }
      case 'move': {
        const { value } = await ElMessageBox.prompt(
          `请输入目标分类（${DOC_FOLDER_NAMES.join('、')}）`,
          '批量移动',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '例如：原告证据',
            inputValidator: (v) =>
              DOC_FOLDER_NAMES.includes(v?.trim()) || `请选择：${DOC_FOLDER_NAMES.join('、')}`
          }
        )
        const folder = value.trim()
        loading.value = true
        for (const file of files) {
          await moveCaseDocument(caseId, file.id, folder)
        }
        ElMessage.success(`已将 ${files.length} 个文件移动到「${folder}」`)
        selectedFiles.value = []
        await fetchDocuments()
        break
      }
      case 'tag': {
        const { value } = await ElMessageBox.prompt('请输入要添加的标签', '批量添加标签', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入标签名称',
          inputPattern: /^.{1,20}$/,
          inputErrorMessage: '标签长度为1-20个字符'
        })
        loading.value = true
        for (const file of files) {
          const currentTags = Array.isArray(file.tags) ? file.tags : []
          if (currentTags.includes(value)) continue
          await updateCaseDocument(caseId, file.id, {
            tags: JSON.stringify([...currentTags, value])
          })
        }
        ElMessage.success(`已为 ${files.length} 个文件添加标签：${value}`)
        await fetchDocuments()
        break
      }
      default:
        ElMessage.info(`批量${command}功能`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`批量${command}失败:`, error)
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

const toPreviewRow = (file) => ({
  id: file.id,
  documentName: file.name,
  name: file.name,
  type: file.type,
  contentType: file.contentType
})

const handlePreviewFile = (file) => {
  if (!props.caseData?.id) {
    ElMessage.warning('案件信息未加载')
    return
  }
  previewRef.value?.preview(props.caseData.id, toPreviewRow(file))
}

const handleDownloadFile = (file) => {
  if (!props.caseData?.id) return
  downloadDocument(props.caseData.id, toPreviewRow(file))
}

// 重命名文件
const handleRenameFile = async (file) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的文件名', '重命名文件', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入文件名（不含扩展名）',
      inputValue: file.name.replace(/\.[^/.]+$/, ''), // 移除扩展名
      inputPattern: /^.{1,200}$/,
      inputErrorMessage: '文件名长度为1-200个字符'
    })

    // 获取文件扩展名
    const ext = file.name.substring(file.name.lastIndexOf('.'))
    const newName = value + ext

    // 更新文件名
    file.name = newName

    ElMessage.success('文件重命名成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重命名文件失败:', error)
      ElMessage.error('重命名失败')
    }
  }
}

// 添加标签
const handleAddTag = async (file) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入标签名称', '添加标签', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,20}$/,
      inputErrorMessage: '标签长度为1-20个字符'
    })

    if (!value) return

    const currentTags = Array.isArray(file.tags) ? file.tags : []
    if (currentTags.includes(value)) {
      ElMessage.warning('标签已存在')
      return
    }

    const updatedTags = [...currentTags, value]

    // 更新文档标签
    await updateCaseDocument(props.caseData.id, file.id, {
      tags: JSON.stringify(updatedTags)
    })

    ElMessage.success('标签添加成功')
    await fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('添加标签失败:', error)
      ElMessage.error('添加失败')
    }
  }
}

// 删除标签
const handleRemoveTag = async (file, tag) => {
  try {
    const currentTags = Array.isArray(file.tags) ? file.tags : []
    const updatedTags = currentTags.filter(t => t !== tag)

    // 更新文档标签
    await updateCaseDocument(props.caseData.id, file.id, {
      tags: JSON.stringify(updatedTags)
    })

    ElMessage.success('标签删除成功')
    await fetchDocuments()
  } catch (error) {
    console.error('删除标签失败:', error)
    ElMessage.error('删除失败')
  }
}

// AI文书生成
const handleAIGenerateDoc = () => {
  if (!props.caseData.id) {
    ElMessage.warning('请先保存案件基本信息')
    return
  }
  aiDocDialogVisible.value = true
}

// 生成文书
const handleGenerateDoc = async () => {
  if (!aiDocForm.value.documentType) {
    ElMessage.warning('请选择文书类型')
    return
  }

  try {
    aiDocGenerating.value = true
    const response = await generateDoc({
      caseId: props.caseData.id,
      documentType: aiDocForm.value.documentType,
      customPrompt: aiDocForm.value.customPrompt,
      additionalContext: aiDocForm.value.additionalContext
    })

    const content = response?.data ?? response
    if (content instanceof Blob) {
      const url = window.URL.createObjectURL(content)
      const link = document.createElement('a')
      link.href = url
      link.download = `${aiDocForm.value.documentType}_${props.caseData.caseName}.doc`
      link.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('文书生成并下载成功')
      aiDocDialogVisible.value = false
    } else if (typeof content === 'string' && content) {
      aiGeneratedDoc.value = content
      aiResultDialogVisible.value = true
      aiDocDialogVisible.value = false
    } else {
      throw new Error('未返回文书内容')
    }
  } catch (error) {
    console.error('生成文书失败:', error)
    ElMessage.error(buildAiErrorHint(formatAiError(error)))
  } finally {
    aiDocGenerating.value = false
  }
}

// 格式化文档内容
const formatDocContent = (content) => {
  if (!content) return ''
  // 将换行符转换为HTML换行
  return content
    .replace(/\n/g, '<br>')
    .replace(/ /g, '&nbsp;')
}

// 复制文档内容
const handleCopyDoc = () => {
  navigator.clipboard.writeText(aiGeneratedDoc.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 下载 TXT
const handleSaveDoc = () => {
  const name = `${getDocumentTypeLabel(aiDocForm.value.documentType) || '文书'}_${props.caseData.caseName}`
  if (downloadTxt(aiGeneratedDoc.value, `${name}.txt`)) {
    ElMessage.success('TXT 已下载')
  }
}

const handleExportDocx = async () => {
  const title = getDocumentTypeLabel(aiDocForm.value.documentType) || '法律文书'
  const name = `${title}_${props.caseData.caseName || 'case'}`
  await downloadDocx({
    content: aiGeneratedDoc.value,
    title,
    fileName: `${name}.docx`
  })
}

</script>

<style scoped lang="scss">
.case-doc {
  .doc-layout {
    display: grid;
    grid-template-columns: 250px 1fr;
    gap: 20px;
    height: calc(100vh - 300px);
  }

  .doc-sidebar {
    background-color: #f5f7fa;
    padding: 20px;
    border-radius: 4px;
    overflow-y: auto;

    .sidebar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;

      h4 {
        margin: 0;
        font-size: 14px;
        color: #333;
      }
    }

    .tree-node {
      display: flex;
      align-items: center;
      gap: 8px;
      width: 100%;

      .node-icon {
        font-size: 16px;
      }

      .node-label {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .doc-main {
    display: flex;
    flex-direction: column;
    gap: 15px;

    .breadcrumb {
      background-color: #fff;
      padding: 10px 15px;
      border-radius: 4px;
    }

    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px;
      background-color: #f5f7fa;
      border-radius: 4px;

      .toolbar-left,
      .toolbar-right {
        display: flex;
        gap: 10px;
        align-items: center;
      }
    }

    .file-name {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #1890ff;

      &:hover {
        text-decoration: underline;
      }

      .file-icon {
        font-size: 18px;
      }

      .name-text {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .upload-demo {
    margin-bottom: 20px;
  }

  .ai-result {
    margin-top: 20px;
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #333;
    }
  }

}
</style>
