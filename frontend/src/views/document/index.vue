<template>
  <div class="document">
    <PageHeader title="文档中心">
      <template #extra>
        <el-select v-model="filterCaseId" placeholder="选择案件" clearable style="width: 200px; margin-right: 10px">
          <el-option
            v-for="caseItem in caseList"
            :key="caseItem.id"
            :label="caseItem.caseName"
            :value="caseItem.id"
          />
        </el-select>
        <el-select v-model="filterType" placeholder="文档类型" clearable style="width: 150px; margin-right: 10px">
          <el-option label="起诉状" value="起诉状" />
          <el-option label="证据" value="证据" />
          <el-option label="答辩状" value="答辩状" />
          <el-option label="判决书" value="判决书" />
          <el-option label="调解书" value="调解书" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-button type="primary" @click="handleFilter">搜索</el-button>
      </template>
    </PageHeader>

    <!-- 文档统计 -->
    <div class="doc-stats">
      <div class="stat-card">
        <div class="stat-icon"><el-icon><Files /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ totalDocuments }}</div>
          <div class="stat-label">全部文档</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><FolderOpened /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ uniqueCases }}</div>
          <div class="stat-label">涉及案件</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon"><el-icon><Coin /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatSize(totalSize) }}</div>
          <div class="stat-label">总大小</div>
        </div>
      </div>
    </div>

    <!-- 文档列表 -->
    <div v-if="!loading && documentList.length === 0" class="empty-state">
      <el-empty description="暂无文档数据">
        <el-button type="primary" @click="$router.push('/case/create')">创建新案件</el-button>
      </el-empty>
    </div>

    <el-table v-else :data="documentList" border v-loading="loading" stripe>
      <el-table-column prop="documentName" label="文档名称" min-width="250">
        <template #default="{ row }">
          <div class="file-name" @click="handlePreview(row)">
            <el-icon class="file-icon"><component :is="getFileIcon(row.documentType)" /></el-icon>
            <span class="name-text">{{ row.documentName }}</span>
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
          <el-link @click="goToCase(row.caseId)" type="primary">
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

      <el-table-column prop="createdAt" label="上传时间" width="160" sortable>
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDownload(row)">
            下载
          </el-button>
          <el-button link type="primary" size="small" @click="handlePreview(row)">
            预览
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalDocuments"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Files, FolderOpened, Coin, Document, Paperclip } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getAllDocuments } from '@/api/document'
import { getCaseList } from '@/api/case'
import request from '@/utils/request'

// Emoji到Element Plus图标的映射
const emojiToIcon = {
  '起诉状': Document,
  '证据': Paperclip,
  '答辩状': Document,
  '判决书': Scale,
  '调解书': Document
}

const getFileIcon = (type) => {
  return emojiToIcon[type] || Document
}

const router = useRouter()
const loading = ref(false)
const documentList = ref([])
const caseList = ref([])

const filterCaseId = ref(null)
const filterType = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)

// 统计数据
const totalDocuments = computed(() => documentList.value.length)
const uniqueCases = computed(() => {
  const cases = new Set(documentList.value.map(doc => doc.caseId))
  return cases.size
})
const totalSize = computed(() => {
  return documentList.value.reduce((sum, doc) => sum + (doc.fileSize || 0), 0)
})

// 获取全部文档
const fetchDocuments = async () => {
  try {
    loading.value = true
    const params = {}
    if (filterCaseId.value) params.caseId = filterCaseId.value
    if (filterType.value) params.documentType = filterType.value

    const res = await getAllDocuments(params)
    documentList.value = res.data || []
  } catch (error) {
    console.error('获取文档列表失败:', error)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

// 获取案件列表（用于筛选）
const fetchCases = async () => {
  try {
    const res = await getCaseList({ page: 1, size: 1000 })
    caseList.value = res.data?.records || []
  } catch (error) {
    console.error('获取案件列表失败:', error)
  }
}

// 筛选
const handleFilter = () => {
  currentPage.value = 1
  fetchDocuments()
}

// 获取案件名称
const getCaseName = (caseId) => {
  const caseItem = caseList.value.find(c => c.id === caseId)
  return caseItem?.caseName || `案件${caseId}`
}

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

// 下载文档
const handleDownload = (row) => {
  ElMessage.info('下载功能需要后端提供文件下载接口')
}

// 预览文档
const handlePreview = (row) => {
  ElMessage.info('预览功能需要集成文档预览组件')
}

// 删除文档
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档"${row.documentName}"吗？`,
      '删除确认',
      { type: 'warning' }
    )

    // 调用后端删除API: DELETE /api/cases/{caseId}/documents/{id}
    const response = await request.delete(`/cases/${row.caseId}/documents/${row.id}`)

    if (response.data.code === 200) {
      ElMessage.success('文档删除成功')
      await fetchDocuments()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除文档失败')
      console.error(error)
    }
  }
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 分页
const handleSizeChange = (size) => {
  pageSize.value = size
  fetchDocuments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
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
    cursor: pointer;

    .file-icon {
      margin-right: 8px;
    }

    .name-text {
      flex: 1;
    }

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
