<template>
  <div class="case-search-page">
    <PageHeader title="类案检索" />

    <div class="search-container">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
          <el-form-item label="案由" required>
            <el-input
              v-model="searchForm.caseReason"
              placeholder="输入案由关键词，如：民间借贷纠纷"
              style="width: 400px"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="案件类型" required>
            <el-select v-model="searchForm.caseType" placeholder="请选择" style="width: 150px">
              <el-option label="民事" value="CIVIL" />
              <el-option label="商事" value="COMMERCIAL" />
              <el-option label="刑事" value="CRIMINAL" />
              <el-option label="行政" value="ADMINISTRATIVE" />
              <el-option label="仲裁" value="ARBITRATION" />
            </el-select>
          </el-form-item>

          <el-form-item label="争议金额">
            <el-input-number v-model="searchForm.amount" :min="0" :precision="2" placeholder="可选" style="width: 160px" />
          </el-form-item>

          <el-form-item label="审理法院">
            <el-input v-model="searchForm.court" placeholder="法院名称" clearable style="width: 200px" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading">
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="results-card" v-loading="loading">
        <template #header>
          <span>找到 <strong>{{ total }}</strong> 个相关案例</span>
        </template>

        <el-empty v-if="results.length === 0 && !loading" description="暂无搜索结果" />

        <div v-else class="case-list">
          <div v-for="item in results" :key="item.id" class="case-item" @click="handleView(item)">
            <div class="case-title">
              <h3>{{ item.title || item.caseName || '案号：' + item.caseNumber }}</h3>
              <el-tag size="small" type="success" v-if="item.similarityPercent">{{ item.similarityPercent }}</el-tag>
              <el-tag size="small" v-if="item.caseType">{{ item.caseType }}</el-tag>
            </div>

            <div class="case-meta">
              <span v-if="item.caseNumber">案号：{{ item.caseNumber }}</span>
              <span v-if="item.caseReason">案由：{{ item.caseReason }}</span>
              <span v-if="item.court">法院：{{ item.court }}</span>
              <span v-if="item.amount">标的：{{ item.amount }}</span>
            </div>

            <div class="case-summary" v-if="item.summary || item.caseBrief">
              {{ item.summary || item.caseBrief }}
            </div>

            <div class="case-actions" style="margin-top: 10px;">
              <el-button size="small" type="primary" @click.stop="handleViewDetail(item)">
                查看详情
              </el-button>
              <el-button size="small" @click.stop="handleSimilar(item)">
                相似案例
              </el-button>
            </div>
          </div>
        </div>

      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { searchSimilarCases, searchSimilarByCaseId } from '@/api/caseSearch'

const router = useRouter()
const route = useRoute()

const searchForm = ref({
  caseReason: '',
  caseType: 'CIVIL',
  court: '',
  amount: null
})

const loading = ref(false)
const results = ref([])
const total = ref(0)

function mapResultRow(row) {
  return {
    id: row.caseId,
    caseId: row.caseId,
    title: row.caseName,
    caseName: row.caseName,
    caseNumber: row.caseNumber,
    caseReason: row.caseReason,
    caseType: row.caseType,
    court: row.court,
    amount: row.amount,
    summary: row.summary,
    similarityPercent: row.similarityPercent
  }
}

const handleSearch = async () => {
  if (!searchForm.value.caseReason?.trim()) {
    ElMessage.warning('请输入案由')
    return
  }
  if (!searchForm.value.caseType) {
    ElMessage.warning('请选择案件类型')
    return
  }

  loading.value = true
  try {
    const payload = {
      caseReason: searchForm.value.caseReason.trim(),
      caseType: searchForm.value.caseType,
      court: searchForm.value.court || undefined,
      amount: searchForm.value.amount ?? undefined,
      limit: 20
    }
    const res = await searchSimilarCases(payload)
    const list = Array.isArray(res.data) ? res.data : []
    results.value = list.map(mapResultRow)
    total.value = results.value.length

    if (results.value.length === 0) {
      ElMessage.info('未找到相似案件，可调整案由或类型后重试')
    }
  } catch (error) {
    console.error('类案检索失败:', error)
    ElMessage.error(error.message || '检索失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.value = { caseReason: '', caseType: 'CIVIL', court: '', amount: null }
  results.value = []
  total.value = 0
}

const handleView = (item) => {
  const id = item.caseId || item.id
  if (id) router.push(`/case/${id}`)
}

const handleViewDetail = (item) => handleView(item)

const handleSimilar = async (item) => {
  const caseId = item.caseId || item.id
  if (!caseId) {
    searchForm.value.caseReason = item.caseReason || item.summary || item.title || ''
    handleSearch()
    return
  }
  loading.value = true
  try {
    const res = await searchSimilarByCaseId(caseId, 10)
    const list = Array.isArray(res.data) ? res.data : []
    results.value = list.map(mapResultRow)
    total.value = results.value.length
  } catch (e) {
    ElMessage.error(e.message || '相似案例检索失败')
  } finally {
    loading.value = false
  }
}

if (route.query.caseId) {
  searchSimilarByCaseId(Number(route.query.caseId), 10).then((res) => {
    const list = Array.isArray(res.data) ? res.data : []
    results.value = list.map(mapResultRow)
    total.value = results.value.length
  }).catch(() => {})
}
if (route.query.caseReason) {
  searchForm.value.caseReason = String(route.query.caseReason)
  if (route.query.caseType) searchForm.value.caseType = String(route.query.caseType)
}
</script>

<style scoped lang="scss">
.case-search-page {
  .search-container {
    margin-top: 20px;
  }

  .search-card, .results-card {
    margin-bottom: 20px;
  }

  .case-list {
    .case-item {
      padding: 20px;
      border-bottom: 1px solid #e4e7ed;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background: #f9f9f9;
        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      }

      .case-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        h3 {
          margin: 0;
          font-size: 16px;
          color: #303133;
        }
      }

      .case-meta {
        display: flex;
        gap: 20px;
        font-size: 13px;
        color: #909399;
        margin-bottom: 10px;
        flex-wrap: wrap;
      }

      .case-summary {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 10px;
      }

      .case-actions {
        display: flex;
        gap: 10px;
      }
    }
  }

  // 移动端适配
  @media (max-width: 768px) {
    .search-card {
      :deep(.el-form-item) {
        width: 100% !important;
        margin-bottom: 15px;
      }

      :deep(.el-input),
      :deep(.el-select),
      :deep(.el-date-picker) {
        width: 100% !important;
      }
    }

    .case-item {
      .case-meta {
        gap: 10px;
        font-size: 12px;
      }

      .case-actions {
        flex-direction: column;

        .el-button {
          width: 100%;
        }
      }
    }
  }
}
</style>
