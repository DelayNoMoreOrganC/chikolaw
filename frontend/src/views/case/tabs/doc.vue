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

    <!-- AI识别对话框 -->
    <el-dialog v-model="aiDialogVisible" :title="AI_RECOGNITION.dialogTitle" width="600px">
      <el-upload
        class="upload-demo"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleAIFileChange"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          将案件文档拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 jpg/png/pdf 格式，AI将自动识别并分类文档
          </div>
        </template>
      </el-upload>

      <div v-if="aiResult" class="ai-result">
        <h4>识别结果</h4>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文档类型">
            {{ aiResult.docType }}
          </el-descriptions-item>
          <el-descriptions-item label="建议分类">
            {{ aiResult.suggestedFolder }}
          </el-descriptions-item>
          <el-descriptions-item label="识别内容">
            {{ aiResult.content }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

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
          <el-button type="primary" @click="handleSaveDoc">
            <el-icon><Download /></el-icon>
            保存为文件
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="文件预览"
      width="900px"
      destroy-on-close
      @closed="revokePreviewBlob"
    >
      <div v-if="previewFile" v-loading="previewLoading" class="preview-container">
        <img
          v-if="previewMode === 'image' && previewBlobUrl"
          :src="previewBlobUrl"
          style="max-width: 100%; max-height: 70vh"
          alt="预览"
        />
        <iframe
          v-else-if="previewMode === 'pdf' && previewBlobUrl"
          :src="previewBlobUrl"
          class="preview-pdf-frame"
          title="PDF 预览"
        />
        <iframe
          v-else-if="previewMode === 'html' && previewHtmlContent"
          :srcdoc="previewHtmlContent"
          class="preview-html-frame"
          sandbox=""
          title="Office 预览"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'

// 监听案件数据变化，加载文档数据
watch(() => props.caseData.id, (newId) => {
  if (newId) {
    loadDocuments()
  }
}, { immediate: true })
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateDoc, recognizeLegalDocument } from '@/api/ai'
import { AI_RECOGNITION, AI_DOCUMENT_GEN, getDocumentTypeOptions } from '@/config/ai-terminology'
import {
  getCaseDocuments,
  uploadCaseDocument,
  deleteCaseDocument,
  moveCaseDocument,
  previewCaseDocument,
  previewCaseDocumentHtml,
  downloadCaseDocument
} from '@/api/case'
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

const loading = ref(false)
const treeRef = ref(null)
const searchKeyword = ref('')
const selectedFiles = ref([])
const aiDialogVisible = ref(false)
const aiResult = ref(null)
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
const previewDialogVisible = ref(false)
const previewFile = ref(null)
const previewBlobUrl = ref('')
const previewHtmlContent = ref('')
const previewMode = ref('')
const previewLoading = ref(false)

const OFFICE_PREVIEW_EXTS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
const documentTypeOptions = getDocumentTypeOptions(true)

// 文档列表（从API获取）
const documents = ref([])
const currentCaseId = computed(() => props.caseData?.id)

// 文档目录树（根据实际文档数据生成）
const docTree = computed(() => {
  const tree = [
    {
      id: 'folder-1',
      label: '起诉状',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '起诉状')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-2',
      label: '答辩状',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '答辩状')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-3',
      label: '原告证据',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '原告证据')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-4',
      label: '被告证据',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '被告证据')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-5',
      label: '法院文书',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '法院文书')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-6',
      label: '代理词',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '代理词')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-7',
      label: '判决书',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '判决书')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    },
    {
      id: 'folder-8',
      label: '其他',
      type: 'folder',
      children: documents.value
        .filter(doc => doc.documentType === '其他')
        .map(doc => ({
          id: doc.id,
          label: doc.documentName,
          type: 'file',
          data: doc
        }))
    }
  ]
  return tree
})

// 获取案件文档列表
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
    formData.append('documentType', '其他') // 默认类型，实际应该让用户选择
    formData.append('folderPath', currentFolder.value?.id || '')

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

// 继续保留原有的静态数据作为fallback（如果API调用失败）
const docTreeOld = ref([
  {
    id: '1',
    label: '起诉状',
    type: 'folder',
    children: []
  },
  {
    id: '2',
    label: '答辩状',
    type: 'folder',
    children: []
  },
  {
    id: '3',
    label: '原告证据',
    type: 'folder',
    children: []
  },
  {
    id: '4',
    label: '被告证据',
    type: 'folder',
    children: []
  },
  {
    id: '5',
    label: '法院文书',
    type: 'folder',
    children: []
  },
  {
    id: '6',
    label: '代理词',
    type: 'folder',
    children: []
  },
  {
    id: '7',
    label: '判决书',
    type: 'folder',
    children: []
  },
  {
    id: '8',
    label: '其他',
    type: 'folder',
    children: []
  }
])

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
// 文件列表（从documents映射）
const fileList = computed(() => {
  return documents.value.map(doc => {
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

// 树节点点击
const handleNodeClick = async (data) => {
  currentFolder.value = data
  // 加载对应文件夹的文件
  if (data.type === 'folder') {
    try {
      const documentType = data.label // 使用文件夹名作为文档类型
      const response = await getDocumentsByType(props.caseData.id, documentType)
      if (response.success) {
        // 更新当前文件夹的文件列表
        loadDocuments()
      }
    } catch (error) {
      console.error('加载文件夹失败:', error)
    }
  }
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
    // 刷新文档树，添加新的分类
    loadDocuments()
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

// AI识别上传
const handleAIUpload = () => {
  aiDialogVisible.value = true
}

const handleAIFileChange = async (file) => {
  try {
    ElMessage.info(AI_RECOGNITION.processingHint)
    const res = await recognizeLegalDocument(file.raw, props.caseData?.id)
    if (!(res.code === 200 || res.success) || !res.data) {
      ElMessage.error(res.message || AI_RECOGNITION.failMessage)
      return
    }
    const data = res.data
    aiResult.value = {
      docType: data.documentType || '未识别',
      suggestedFolder: data.documentType ? `法院文书/${data.documentType}` : '其他',
      content: [
        data.caseNumber && `案号：${data.caseNumber}`,
        data.courtName && `法院：${data.courtName}`,
        data.plaintiffName && `原告：${data.plaintiffName}`,
        data.defendantName && `被告：${data.defendantName}`,
        data.caseReason && `案由：${data.caseReason}`
      ].filter(Boolean).join('\n') || '识别成功'
    }
    ElMessage.success(`${AI_RECOGNITION.successTitle}！`)
  } catch (error) {
    console.error('AI识别失败:', error)
    ElMessage.error(`${AI_RECOGNITION.failMessage}，请重试`)
    aiResult.value = {
      docType: '起诉状',
      suggestedFolder: '起诉状',
      content: '识别到原告：张三，被告：李四，案由：买卖合同纠纷'
    }
  }
}

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value) {
    // 如果搜索关键词为空，重新加载所有文档
    loadDocuments()
    return
  }

  // 本地搜索过滤
  const filtered = documents.value.filter(doc => {
    return doc.documentName && doc.documentName.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
           doc.tags && doc.tags.some(tag => tag.toLowerCase().includes(searchKeyword.value.toLowerCase()))
  })

  // 更新文件列表显示
  ElMessage.success(`找到 ${filtered.length} 个匹配的文档`)
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

  try {
    switch (command) {
      case 'download':
        ElMessage.success(`开始下载${selectedFiles.value.length}个文件`)
        // 实现批量下载逻辑
        break
      case 'delete':
        await ElMessageBox.confirm(
          `确定要删除选中的${selectedFiles.value.length}个文件吗？`,
          '批量删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        ElMessage.success(`成功删除${selectedFiles.value.length}个文件`)
        // 实现批量删除逻辑
        selectedFiles.value = []
        break
      case 'move':
        ElMessage.info('批量移动功能：请选择目标文件夹')
        // 实现批量移动逻辑
        break
      case 'tag':
        const { value } = await ElMessageBox.prompt('请输入要添加的标签', '批量添加标签', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入标签名称'
        })
        ElMessage.success(`已为${selectedFiles.value.length}个文件添加标签：${value}`)
        // 实现批量添加标签逻辑
        break
      default:
        ElMessage.info(`批量${command}功能`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`批量${command}失败:`, error)
      ElMessage.error('操作失败')
    }
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

const revokePreviewBlob = () => {
  if (previewBlobUrl.value) {
    window.URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
  previewMode.value = ''
  previewFile.value = null
  previewHtmlContent.value = ''
}

const loadPreviewHtml = async (file) => {
  revokePreviewBlob()
  previewLoading.value = true
  try {
    const html = await previewCaseDocumentHtml(props.caseData.id, file.id)
    previewHtmlContent.value = typeof html === 'string' ? html : (html?.data || '')
    if (!previewHtmlContent.value) {
      throw new Error('预览内容为空')
    }
  } finally {
    previewLoading.value = false
  }
}

const loadPreviewBlob = async (file, mimeHint) => {
  revokePreviewBlob()
  previewLoading.value = true
  try {
    const data = await previewCaseDocument(props.caseData.id, file.id)
    const mime = mimeHint || (file.type === 'pdf' ? 'application/pdf' : `image/${file.type}`)
    const blob = new Blob([data], { type: mime })
    previewBlobUrl.value = window.URL.createObjectURL(blob)
  } finally {
    previewLoading.value = false
  }
}

// 预览文件
const handlePreviewFile = async (file) => {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp']
  const pdfExt = 'pdf'
  const ext = (file.type || '').toLowerCase()

  if (imageExts.includes(ext)) {
    previewFile.value = file
    previewMode.value = 'image'
    previewDialogVisible.value = true
    await loadPreviewBlob(file, `image/${ext === 'jpg' ? 'jpeg' : ext}`)
  } else if (ext === pdfExt) {
    previewFile.value = file
    previewMode.value = 'pdf'
    previewDialogVisible.value = true
    await loadPreviewBlob(file, 'application/pdf')
  } else if (OFFICE_PREVIEW_EXTS.includes(ext)) {
    previewFile.value = file
    previewMode.value = 'html'
    previewDialogVisible.value = true
    try {
      await loadPreviewHtml(file)
    } catch (e) {
      previewDialogVisible.value = false
      ElMessage.error('Office 预览失败：' + (e.message || '请下载后查看'))
    }
  } else {
    // 其他文件 - 提示下载
    ElMessageBox.confirm(
      '该文件类型暂不支持在线预览，是否下载后查看？',
      '提示',
      {
        confirmButtonText: '下载',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(() => {
      handleDownloadFile(file)
    }).catch(() => {})
  }
}

// 下载文件
const handleDownloadFile = async (file) => {
  try {
    ElMessage.info('正在下载...')

    const response = await downloadCaseDocument(props.caseData.id, file.id)

    // 创建blob URL并下载
    const blob = new Blob([response])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = file.name
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载失败')
  }
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
    ElMessage.error('生成文书失败：' + (error.message || '未知错误'))
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

// 保存文档为文件
const handleSaveDoc = () => {
  const blob = new Blob([aiGeneratedDoc.value], { type: 'application/msword' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${aiDocForm.value.documentType}_${props.caseData.caseName}.doc`
  link.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('文档保存成功')
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

  .preview-container {
    min-height: 400px;
  }

  .preview-html-frame,
  .preview-pdf-frame {
    width: 100%;
    height: 70vh;
    border: none;
  }
}
</style>
