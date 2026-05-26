<template>
  <div class="rag-search-page">
    <PageHeader title="AI知识问答" />

    <el-card class="search-card">
      <el-input
        v-model="question"
        placeholder="请输入法律问题，例如：劳动仲裁申请流程、合同违约责任..."
        size="large"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" :loading="loading">
            搜索
          </el-button>
        </template>
      </el-input>

      <div class="example-questions">
        <el-tag
          v-for="q in exampleQuestions"
          :key="q"
          @click="question = q; handleSearch()"
          style="cursor: pointer; margin: 5px;"
        >
          {{ q }}
        </el-tag>
      </div>
    </el-card>

    <el-card v-if="answer" class="answer-card" v-loading="loading">
      <template #header>
        <span>AI回答</span>
      </template>

      <div class="answer-content" v-html="formattedAnswer"></div>

      <el-divider v-if="sources && sources.length > 0" />

      <div v-if="sources && sources.length > 0" class="sources-section">
        <h4>参考文档</h4>
        <el-collapse v-model="activeSources">
          <el-collapse-item
            v-for="(source, index) in sources"
            :key="index"
            :title="source.title"
            :name="String(index)"
          >
            <div class="source-detail">
              <p><strong>分类：</strong>{{ source.category }}</p>
              <p><strong>摘要：</strong>{{ source.summary }}</p>
              <p v-if="source.citationSnippet"><strong>摘录：</strong>{{ source.citationSnippet }}</p>
              <el-button
                size="small"
                @click="viewDocument(source.id)"
                type="primary"
              >
                查看完整文档
              </el-button>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="answer-meta">
        <el-tag :type="hasAnswer ? 'success' : 'warning'">
          {{ hasAnswer ? '找到相关文档' : '未找到相关文档' }}
        </el-tag>
        <el-tag v-if="documentCount" type="info">
          引用 {{ documentCount }} 篇文档
        </el-tag>
        <el-tag v-if="retrievalMode" type="info" effect="plain">
          检索：{{ retrievalModeLabel }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { askAI } from '@/api/knowledge'

const question = ref('')
const answer = ref('')
const sources = ref([])
const hasAnswer = ref(false)
const documentCount = ref(0)
const loading = ref(false)
const activeSources = ref(['0', '1', '2'])
const retrievalMode = ref('')

const RETRIEVAL_LABELS = {
  VECTOR: '向量语义',
  KEYWORD_AFTER_VECTOR_EMPTY: '关键词（向量无结果）',
  KEYWORD_AFTER_VECTOR_MISS: '关键词（向量未解析到文档）',
  KEYWORD_FALLBACK: '关键词（向量服务异常）',
  NO_HITS: '无命中',
  ERROR: '错误'
}

const retrievalModeLabel = computed(() => RETRIEVAL_LABELS[retrievalMode.value] || retrievalMode.value || '')

const exampleQuestions = [
  '劳动仲裁申请流程',
  '合同违约责任认定',
  '刑事案件辩护要点',
  '如何收集证据',
  '诉讼时效计算',
  '离婚案件财产分割',
  '交通事故赔偿标准',
  '借款合同利息计算'
]

const formattedAnswer = computed(() => {
  if (!answer.value) return ''

  // 简单的Markdown格式化
  return answer.value
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.*?)(<br>|$)/g, '• $1$2')
})

const handleSearch = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  loading.value = true
  answer.value = ''
  sources.value = []
  hasAnswer.value = false
  documentCount.value = 0
  retrievalMode.value = ''

  try {
    const aiResponse = await askAI(question.value)
    const d = aiResponse.data || {}
    answer.value = d.answer || '暂无回答'
    hasAnswer.value = !!d.hasAnswer
    documentCount.value = d.documentCount || (d.sources?.length ?? 0)
    sources.value = d.sources || []
    retrievalMode.value = d.retrievalMode || ''

    if (!hasAnswer.value && !answer.value) {
      answer.value = `未找到与「${question.value}」相关的知识库内容，请尝试换关键词。`
    }
  } catch (error) {
    console.error('搜索失败', error)
    ElMessage.error('搜索失败，请稍后再试')
    answer.value = '系统暂时无法回答您的问题，请稍后再试。'
    hasAnswer.value = false
  } finally {
    loading.value = false
  }
}

const viewDocument = (id) => {
  window.open(`#/knowledge/${id}`, '_blank')
}
</script>

<style scoped lang="scss">
.rag-search-page {
  max-width: 900px;
  margin: 0 auto;

  .search-card {
    margin-bottom: 20px;

    .example-questions {
      margin-top: 15px;
    }
  }

  .answer-card {
    .answer-content {
      font-size: 15px;
      line-height: 1.8;
      color: #333;
      white-space: pre-wrap;
    }

    .sources-section {
      margin-top: 20px;

      h4 {
        margin-bottom: 15px;
        color: #606266;
      }

      .source-detail {
        p {
          margin: 8px 0;
          color: #606266;
        }
      }
    }

    .answer-meta {
      margin-top: 20px;
      display: flex;
      gap: 10px;
    }
  }
}
</style>
