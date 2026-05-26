<template>
  <div class="legal-search">
    <PageHeader title="法律检索" />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="法规库" name="regulations">
        <el-card>
          <el-form inline>
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" placeholder="法条、案例、执行、保全" clearable />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="query.category" clearable placeholder="全部分类">
                <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="loadRegulations">检索</el-button>
          </el-form>

          <el-table :data="articles" style="margin-top: 16px">
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="articleType" label="类型" width="120" />
            <el-table-column prop="category" label="分类" width="160" />
            <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="AI法律问答" name="qa">
        <el-card>
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            placeholder="请输入金融借款、保全、执行、终本等法律问题"
          />
          <el-button type="primary" :loading="asking" style="margin-top: 12px" @click="handleAsk">
            提问
          </el-button>
          <el-card v-if="answer" class="answer-card">
            <pre>{{ answer }}</pre>
          </el-card>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="类案推送" name="similar">
        <el-card v-loading="similarLoading">
          <el-form inline>
            <el-form-item label="案由">
              <el-input v-model="similarForm.caseReason" placeholder="案由关键词" clearable style="width: 220px" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="similarForm.caseType" style="width: 120px">
                <el-option label="民事" value="CIVIL" />
                <el-option label="刑事" value="CRIMINAL" />
                <el-option label="行政" value="ADMINISTRATIVE" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="runSimilarSearch">检索</el-button>
            <el-button @click="goCaseSearch">完整类案页</el-button>
          </el-form>
          <el-table :data="similarResults" style="margin-top: 12px" empty-text="输入案由后检索">
            <el-table-column prop="caseName" label="案件" min-width="180" />
            <el-table-column prop="caseReason" label="案由" width="140" />
            <el-table-column prop="similarityPercent" label="相似度" width="90" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="router.push(`/case/${row.caseId}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { askLegalQuestion, getLegalCategories, searchRegulations } from '@/api/legalSearch'
import { searchSimilarCases } from '@/api/caseSearch'

const router = useRouter()
const activeTab = ref('regulations')
const similarForm = reactive({ caseReason: '', caseType: 'CIVIL' })
const similarResults = ref([])
const similarLoading = ref(false)
const categories = ref([])
const articles = ref([])
const query = reactive({ keyword: '', category: '' })
const question = ref('')
const answer = ref('')
const asking = ref(false)

const loadRegulations = async () => {
  const res = await searchRegulations({ ...query, page: 0, size: 20 })
  if (res.success) {
    articles.value = res.data?.content || []
  }
}

const handleAsk = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  asking.value = true
  try {
    const res = await askLegalQuestion(question.value)
    if (res.success) {
      answer.value = res.data?.answer || res.data?.content || JSON.stringify(res.data, null, 2)
    }
  } finally {
    asking.value = false
  }
}

const runSimilarSearch = async () => {
  if (!similarForm.caseReason?.trim()) {
    ElMessage.warning('请输入案由')
    return
  }
  similarLoading.value = true
  try {
    const res = await searchSimilarCases({
      caseReason: similarForm.caseReason.trim(),
      caseType: similarForm.caseType,
      limit: 10
    })
    similarResults.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    ElMessage.error(e.message || '检索失败')
  } finally {
    similarLoading.value = false
  }
}

const goCaseSearch = () => {
  router.push({
    path: '/case-search',
    query: { caseReason: similarForm.caseReason, caseType: similarForm.caseType }
  })
}

onMounted(async () => {
  const res = await getLegalCategories()
  categories.value = res.data || []
  loadRegulations()
})
</script>

<style scoped>
.answer-card {
  margin-top: 16px;
}

pre {
  white-space: pre-wrap;
}
</style>
