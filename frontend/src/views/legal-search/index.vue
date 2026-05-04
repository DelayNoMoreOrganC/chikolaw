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
        <el-empty description="请在案件详情页根据当前案由推送类案；本页保留为统一入口。" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { askLegalQuestion, getLegalCategories, searchRegulations } from '@/api/legalSearch'

const activeTab = ref('regulations')
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
