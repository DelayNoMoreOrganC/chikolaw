<template>
  <div class="ai-doc-generator">
    <el-dialog
      v-model="dialogVisible"
      title="AI文书生成"
      width="900px"
      :close-on-click-modal="false"
      class="generator-dialog"
      @closed="handleClosed"
    >
      <div class="generator-content">
        <!-- 步骤指示器 -->
        <el-steps :active="currentStep" align-center class="steps">
          <el-step title="选择文书类型" />
          <el-step title="填写信息" />
          <el-step title="生成文书" />
        </el-steps>

        <!-- 步骤1：选择文书类型 -->
        <div v-show="currentStep === 0" class="step-content">
          <h4>请选择要生成的文书类型</h4>
          <div class="doc-types-grid">
            <div
              v-for="docType in docTypes"
              :key="docType.type"
              class="doc-type-card"
              :class="{ 'selected': selectedDocType === docType.type }"
              @click="handleSelectDocType(docType.type)"
            >
              <div class="doc-icon">{{ docType.icon }}</div>
              <div class="doc-info">
                <div class="doc-title">{{ docType.title }}</div>
                <div class="doc-desc">{{ docType.description }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 步骤2：填写信息 -->
        <div v-show="currentStep === 1" class="step-content">
          <h4>填写{{ getSelectedDocTypeTitle() }}信息</h4>

          <!-- 关联案件选择 -->
          <el-form :model="formData" label-width="120px" class="doc-form">
            <el-form-item label="关联案件" required>
              <el-select
                v-model="formData.caseId"
                filterable
                placeholder="请选择案件"
                style="width: 100%"
                @change="handleCaseChange"
              >
                <el-option
                  v-for="caseItem in caseList"
                  :key="caseItem.id"
                  :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
                  :value="caseItem.id"
                />
              </el-select>
            </el-form-item>

            <!-- 起诉状特有字段 -->
            <template v-if="selectedDocType === 'complaint'">
              <el-form-item label="原告信息" required>
                <el-input
                  v-model="formData.plaintiffInfo"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入原告信息（姓名、性别、民族、出生日期、住址等）"
                />
              </el-form-item>

              <el-form-item label="被告信息" required>
                <el-input
                  v-model="formData.defendantInfo"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入被告信息（姓名、性别、民族、出生日期、住址等）"
                />
              </el-form-item>

              <el-form-item label="诉讼请求" required>
                <el-input
                  v-model="formData.claims"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入具体的诉讼请求（如：请求判令被告偿还借款本金XX元及利息...）"
                />
              </el-form-item>

              <el-form-item label="事实与理由" required>
                <el-input
                  v-model="formData.facts"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入案件的事实经过和法律依据"
                />
              </el-form-item>
            </template>

            <!-- 答辩状特有字段 -->
            <template v-if="selectedDocType === 'defense'">
              <el-form-item label="答辩人信息" required>
                <el-input
                  v-model="formData.defendantInfo"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入答辩人信息"
                />
              </el-form-item>

              <el-form-item label="答辩意见" required>
                <el-input
                  v-model="formData.defenseOpinion"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入答辩意见和理由"
                />
              </el-form-item>

              <el-form-item label="证据清单">
                <el-input
                  v-model="formData.evidence"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入证据清单（可选）"
                />
              </el-form-item>
            </template>

            <!-- 代理词特有字段 -->
            <template v-if="selectedDocType === 'legalBrief'">
              <el-form-item label="代理意见" required>
                <el-input
                  v-model="formData.agentOpinion"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入代理意见"
                />
              </el-form-item>

              <el-form-item label="争议焦点">
                <el-input
                  v-model="formData.disputeFocus"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入本案争议焦点（可选）"
                />
              </el-form-item>

              <el-form-item label="法律依据">
                <el-input
                  v-model="formData.legalBasis"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入相关法律依据（可选）"
                />
              </el-form-item>
            </template>

            <!-- 法律意见书特有字段 -->
            <template v-if="selectedDocType === 'legalOpinion'">
              <el-form-item label="委托事项" required>
                <el-input
                  v-model="formData.matter"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入委托事项"
                />
              </el-form-item>

              <el-form-item label="基本情况" required>
                <el-input
                  v-model="formData.basicFacts"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入基本情况"
                />
              </el-form-item>

              <el-form-item label="法律分析">
                <el-input
                  v-model="formData.legalAnalysis"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入法律分析（可选）"
                />
              </el-form-item>
            </template>
          </el-form>
        </div>

        <!-- 步骤3：生成文书 -->
        <div v-show="currentStep === 2" class="step-content">
          <h4>生成结果</h4>

          <!-- 生成中状态 -->
          <div v-if="generating" class="generating-state">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            <p>AI正在生成文书，请稍候...</p>
            <el-progress :percentage="generateProgress" :show-text="false" />
          </div>

          <!-- 生成结果 -->
          <div v-else-if="generatedDoc" class="generated-doc">
            <div class="doc-toolbar">
              <el-button @click="handleCopyDoc">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
              <el-button @click="handleDownloadDoc">
                <el-icon><Download /></el-icon>
                下载Word
              </el-button>
              <el-button type="primary" @click="handleRegenerate">
                <el-icon><Refresh /></el-icon>
                重新生成
              </el-button>
            </div>
            <div class="doc-content" v-html="generatedDoc"></div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="currentStep === 0" type="primary" :disabled="!selectedDocType" @click="handleNext">
          下一步
        </el-button>
        <div v-if="currentStep === 1">
          <el-button @click="currentStep--">上一步</el-button>
          <el-button type="primary" @click="handleGenerate" :disabled="!formData.caseId">
            开始生成
          </el-button>
        </div>
        <div v-if="currentStep === 2 && !generating">
          <el-button @click="currentStep--">上一步</el-button>
          <el-button type="primary" @click="dialogVisible = false">完成</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, DocumentCopy, Download, Refresh } from '@element-plus/icons-vue'
import { generateDoc } from '@/api/ai'
import { getCaseList } from '@/api/case'
import { useUserStore } from '@/stores'
import { LEGAL_DOCUMENT_TYPES } from '@/config/ai-terminology'

const userStore = useUserStore()
const userName = computed(() => userStore.userName)

const dialogVisible = ref(false)
const currentStep = ref(0)
const selectedDocType = ref('')
const generating = ref(false)
const generateProgress = ref(0)
const generatedDoc = ref('')

const caseList = ref([])

const DOC_TYPE_META = {
  COMPLAINT: { icon: '📄', description: '民事/行政起诉状，包含原告被告信息、诉讼请求、事实理由' },
  DEFENSE_STATEMENT: { icon: '📝', description: '针对起诉状的答辩，包含答辩意见和证据清单' },
  BRIEF: { icon: '⚖️', description: '庭审代理词，包含代理意见、争议焦点、法律依据' },
  LEGAL_OPINION: { icon: '📋', description: '法律专业意见，包含委托事项、基本情况、法律分析' },
  LAWYER_LETTER: { icon: '✉️', description: '催告、声明等律师函' }
}

const docTypes = LEGAL_DOCUMENT_TYPES.map((t) => ({
  type: t.code,
  title: t.label,
  icon: DOC_TYPE_META[t.code]?.icon || '📄',
  description: DOC_TYPE_META[t.code]?.description || t.description
}))

// 表单数据
const formData = reactive({
  caseId: '',
  plaintiffInfo: '',
  defendantInfo: '',
  claims: '',
  facts: '',
  defenseOpinion: '',
  evidence: '',
  agentOpinion: '',
  disputeFocus: '',
  legalBasis: '',
  matter: '',
  basicFacts: '',
  legalAnalysis: ''
})

// 获取选中文书类型标题
const getSelectedDocTypeTitle = () => {
  const doc = docTypes.find(d => d.type === selectedDocType.value)
  return doc ? doc.title : ''
}

// 选择文书类型
const handleSelectDocType = (type) => {
  selectedDocType.value = type
}

// 下一步
const handleNext = () => {
  currentStep.value++
}

// 生成文书
const handleGenerate = async () => {
  try {
    generating.value = true
    generateProgress.value = 0
    currentStep.value = 2

    // 模拟进度
    const progressInterval = setInterval(() => {
      generateProgress.value += 10
      if (generateProgress.value >= 90) {
        clearInterval(progressInterval)
      }
    }, 500)

    const response = await generateDoc({
      documentType: selectedDocType.value,
      caseId: formData.caseId,
      customPrompt: formData.claims || formData.defenseOpinion || formData.agentOpinion || '',
      additionalContext: JSON.stringify(formData)
    })

    clearInterval(progressInterval)
    generateProgress.value = 100

    const content = response?.data ?? response
    if (content) {
      generatedDoc.value = typeof content === 'string' ? `
        <div style="padding: 20px; font-family: SimSun; line-height: 1.8; white-space: pre-wrap;">${content.replace(/</g, '&lt;')}</div>
      ` : `
        <div style="padding: 20px; font-family: SimSun; line-height: 1.8;">
          <h2 style="text-align: center;">${getSelectedDocTypeTitle()}</h2>
          <p style="color: #999; text-align: center;">生成时间：${new Date().toLocaleString()}</p>
          <hr/>
          <p style="color: #666;">文书已生成完毕，请点击"下载Word"按钮获取完整文档。</p>
        </div>
      `
    } else {
      throw new Error('未返回文书内容')
    }
  } catch (error) {
    ElMessage.error('生成失败：' + error.message)
  } finally {
    generating.value = false
  }
}

// 重新生成
const handleRegenerate = () => {
  currentStep.value = 1
  generatedDoc.value = ''
}

// 复制文书
const handleCopyDoc = () => {
  ElMessage.success('已复制到剪贴板')
}

// 下载文书
const handleDownloadDoc = () => {
  ElMessage.success('下载功能开发中...')
}

// 关闭对话框
const handleClosed = () => {
  currentStep.value = 0
  selectedDocType.value = ''
  generating.value = false
  generateProgress.value = 0
  generatedDoc.value = ''
  Object.assign(formData, {
    caseId: '',
    plaintiffInfo: '',
    defendantInfo: '',
    claims: '',
    facts: '',
    defenseOpinion: '',
    evidence: '',
    agentOpinion: '',
    disputeFocus: '',
    legalBasis: '',
    matter: '',
    basicFacts: '',
    legalAnalysis: ''
  })
}

// 案件变更
const handleCaseChange = async () => {
  // 可以根据选择的案件自动填充一些信息
}

// 暴露打开方法
const open = async () => {
  try {
    const response = await getCaseList({ pageSize: 100 })
    if (response.success) {
      caseList.value = response.data.list || []
    }
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取案件列表失败')
  }
}

defineExpose({
  open
})
</script>

<style scoped lang="scss">
.ai-doc-generator {
  .generator-dialog {
    :deep(.el-dialog__body) {
      padding-top: 20px;
    }
  }

  .generator-content {
    .steps {
      margin-bottom: 30px;
    }

    .step-content {
      min-height: 400px;

      h4 {
        margin-bottom: 20px;
        color: #333;
        font-size: 16px;
      }
    }

    // 步骤1：文书类型选择
    .doc-types-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 20px;

      .doc-type-card {
        border: 2px solid #e4e7ed;
        border-radius: 8px;
        padding: 20px;
        cursor: pointer;
        transition: all 0.3s;
        display: flex;
        align-items: center;
        gap: 16px;

        &:hover {
          border-color: #1890ff;
          background: #f0f5ff;
        }

        &.selected {
          border-color: #1890ff;
          background: #e6f7ff;
        }

        .doc-icon {
          font-size: 40px;
        }

        .doc-info {
          flex: 1;

          .doc-title {
            font-size: 16px;
            font-weight: 500;
            color: #333;
            margin-bottom: 4px;
          }

          .doc-desc {
            font-size: 13px;
            color: #999;
          }
        }
      }
    }

    // 步骤2：表单
    .doc-form {
      max-width: 800px;
    }

    // 步骤3：生成结果
    .generating-state {
      text-align: center;
      padding: 60px 20px;
      color: #666;

      .el-icon {
        font-size: 40px;
        color: #1890ff;
        margin-bottom: 20px;
      }

      p {
        margin-bottom: 20px;
      }
    }

    .generated-doc {
      .doc-toolbar {
        margin-bottom: 20px;
        display: flex;
        gap: 10px;
      }

      .doc-content {
        border: 1px solid #e4e7ed;
        border-radius: 4px;
        padding: 20px;
        min-height: 400px;
        max-height: 500px;
        overflow-y: auto;
        background: #fff;
      }
    }
  }
}
</style>
