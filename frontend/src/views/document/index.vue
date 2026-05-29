<template>
  <div class="document">
    <PageHeader title="文档中心">
      <template #extra>
        <el-input
          v-model="keyword"
          placeholder="搜索文档名称"
          clearable
          style="width: 200px; margin-right: 10px"
          @keyup.enter="handleFilter"
        />
        <el-select v-model="filterCaseId" placeholder="选择案件" clearable filterable style="width: 200px; margin-right: 10px">
          <el-option
            v-for="caseItem in caseList"
            :key="caseItem.id"
            :label="`${caseItem.caseNumber || ''} ${caseItem.caseName}`"
            :value="caseItem.id"
          />
        </el-select>
        <el-select v-model="filterType" placeholder="文档类型" clearable style="width: 150px; margin-right: 10px">
          <el-option label="起诉状" value="起诉状" />
          <el-option label="答辩状" value="答辩状" />
          <el-option label="原告证据" value="原告证据" />
          <el-option label="被告证据" value="被告证据" />
          <el-option label="法院文书" value="法院文书" />
          <el-option label="判决书" value="判决书" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-button type="primary" @click="handleFilter">搜索</el-button>
      </template>
    </PageHeader>

    <div class="doc-stats">
      <div class="stat-card">
        <div class="stat-icon"><el-icon><Files /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalDocuments }}</div>
          <div class="stat-label">全部文档</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><FolderOpened /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.uniqueCases }}</div>
          <div class="stat-label">涉及案件</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><Coin /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatSize(stats.totalSize) }}</div>
          <div class="stat-label">总大小</div>
        </div>
      </div>
    </div>

    <div v-if="!loading && documentList.length === 0" class="empty-state">
      <el-empty description="暂无文档数据">
        <el-button type="primary" @click="$router.push('/case/list')">前往案件上传</el-button>
      </el-empty>
    </div>

    <el-table v-else :data="documentList" border v-loading="loading" stripe>
      <el-table-column prop="documentName" label="文档名称" min-width="250">
        <template #default="{ row }">
          <div class="file-name" @click="handlePreview(row)">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="name-text">{{ row.documentName }}</span>
            <el-tag v-if="row.versionNo > 1" size="small" type="info">v{{ row.versionNo }}</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="documentType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.documentType }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="caseId" label="所属案件" width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="goToCase(row.caseId)">
            {{ getCaseName(row.caseId) }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>

      <el-table-column prop="folderPath" label="文件夹" width="150">
        <template #default="{ row }">
          {{ row.folderPath || '根目录' }}
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="上传时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDownload(row)">下载</el-button>
          <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <DocumentPreviewDialog ref="previewRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Files, FolderOpened, Coin, Document } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DocumentPreviewDialog from '@/components/DocumentPreviewDialog.vue'
import { getDocumentCenterList, deleteCaseDocument } from '@/api/document'
import { getCaseList } from '@/api/case'
import { useDocumentPreview } from '@/composables/useDocumentPreview'

const router = useRouter()
const previewRef = ref(null)
const { downloadFile } = useDocumentPreview()

const loading = ref(false)
const documentList = ref([])
const caseList = ref([])
const keyword = ref('')
const filterCaseId = ref(null)
const filterType = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const stats = reactive({
  totalDocuments: 0,
  uniqueCases: 0,
  totalSize: 0
})

const fetchDocuments = async () => {
  try {
    loading.value = true
    const res = await getDocumentCenterList({
      page: currentPage.value,
      size: pageSize.value,
      caseId: filterCaseId.value || undefined,
      documentType: filterType.value || undefined,
      keyword: keyword.value?.trim() || undefined
    })
    if (res.code === 200 || res.success) {
      const data = res.data || {}
      const pageData = data.page || {}
      documentList.value = pageData.records || []
      total.value = pageData.total || 0
      if (data.stats) {
        stats.totalDocuments = data.stats.totalDocuments ?? 0
        stats.uniqueCases = data.stats.uniqueCases ?? 0
        stats.totalSize = data.stats.totalSize ?? 0
      }
    }
  } catch (error) {
    console.error('获取文档列表失败:', error)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

const fetchCases = async () => {
  try {
    const res = await getCaseList({ page: 1, size: 500 })
    caseList.value = res.data?.records || res.data?.list || []
  } catch (error) {
    console.error('获取案件列表失败:', error)
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchDocuments()
}

const getCaseName = (caseId) => {
  const caseItem = caseList.value.find((c) => c.id === caseId)
  return caseItem?.caseName || `案件 #${caseId}`
}

const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

const handleDownload = (row) => downloadFile(row.caseId, row)

const handlePreview = (row) => {
  previewRef.value?.preview(row.caseId, row)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档「${row.documentName}」吗？`, '删除确认', { type: 'warning' })
    const res = await deleteCaseDocument(row.caseId, row.id)
    if (res.code === 200 || res.success) {
      ElMessage.success('文档已删除')
      await fetchDocuments()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除文档失败')
    }
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchDocuments()
}

const handleCurrentChange = () => {
  fetchDocuments()
}

onMounted(() => {
  fetchDocuments()
  fetchCases()
})
</script>

<style scoped lang="scss">
.document {
  .doc-stats {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;

    .stat-card {
      flex: 1;
      display: flex;
      align-items: center;
      padding: 20px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .stat-icon {
        font-size: 32px;
        margin-right: 15px;
        color: #409eff;
      }

      .stat-content {
        .stat-value {
          font-size: 24px;
          font-weight: bold;
          color: #333;
          margin-bottom: 5px;
        }

        .stat-label {
          font-size: 14px;
          color: #666;
        }
      }
    }
  }

  .file-name {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    &:hover {
      color: #409eff;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
