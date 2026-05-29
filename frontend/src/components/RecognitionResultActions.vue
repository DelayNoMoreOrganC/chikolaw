<template>
  <div class="recognition-actions">
    <el-descriptions :column="2" border size="small" class="recognition-desc">
      <el-descriptions-item
        v-for="row in fields"
        :key="row.label"
        :label="row.label"
      >
        {{ row.value }}
      </el-descriptions-item>
    </el-descriptions>

    <p v-if="businessLogicText" class="bl-line">
      业务处理：{{ businessLogicText }}
    </p>

    <div class="action-bar">
      <el-button size="small" @click="copyElements">
        <el-icon><DocumentCopy /></el-icon>
        复制要素
      </el-button>
      <el-button size="small" :loading="exporting" @click="exportWord">
        <el-icon><Download /></el-icon>
        导出 Word
      </el-button>
      <el-button
        v-if="showAutomation"
        size="small"
        type="primary"
        :loading="automationRunning"
        :disabled="automationDone"
        @click="runAutomation"
      >
        <el-icon><Calendar /></el-icon>
        {{ automationDone ? '已创建待办/日程' : '创建待办/日程' }}
      </el-button>
      <el-button
        v-if="caseId"
        size="small"
        text
        type="primary"
        @click="goCase"
      >
        查看关联案件
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Download, Calendar } from '@element-plus/icons-vue'
import { recognizeLegalDocument } from '@/api/ai'
import { createTodo } from '@/api/todo'
import { createCalendar } from '@/api/calendar'
import { useDocumentExport } from '@/composables/useDocumentExport'
import { useUserStore } from '@/stores'
import { notifyAiError } from '@/utils/aiError'
import {
  getRecognitionFields,
  formatRecognitionText,
  recognitionDocTitle,
  formatBusinessLogicSummary,
  hasAutomationDone,
  buildHearingTodoPayload,
  buildHearingCalendarPayload
} from '@/utils/recognitionResult'

const props = defineProps({
  recognition: { type: Object, required: true },
  caseId: { type: [Number, String], default: null },
  sourceFile: { type: Object, default: null },
  showAutomation: { type: Boolean, default: true }
})

const emit = defineEmits(['automation-done'])

const router = useRouter()
const userStore = useUserStore()
const { exporting, downloadDocx } = useDocumentExport()
const automationRunning = ref(false)

const fields = computed(() => getRecognitionFields(props.recognition))

const businessLogicText = computed(() =>
  formatBusinessLogicSummary(props.recognition?.businessLogic)
)

const automationDone = computed(() => hasAutomationDone(props.recognition))

const resolvedCaseId = computed(() => {
  const id = props.caseId ?? props.recognition?.businessLogic?.caseId
  if (id == null || id === '') return null
  const n = Number(id)
  return Number.isFinite(n) ? n : null
})

async function copyElements() {
  const text = formatRecognitionText(props.recognition)
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('要素已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请检查浏览器权限')
  }
}

async function exportWord() {
  const content = formatRecognitionText(props.recognition)
  const title = recognitionDocTitle(props.recognition)
  await downloadDocx({
    content,
    title,
    fileName: `${title}.docx`
  })
}

async function runAutomation() {
  if (props.sourceFile) {
    automationRunning.value = true
    try {
      const res = await recognizeLegalDocument(
        props.sourceFile,
        resolvedCaseId.value,
        true
      )
      if (res.code === 200 || res.success) {
        emit('automation-done', res.data)
        const bl = res.data?.businessLogic
        if (bl?.skipped) {
          ElMessage.warning(`该文书类型暂不支持全自动待办：${bl.reason || ''}`)
          await createQuickHearingItems()
        } else if (bl?.success === false) {
          ElMessage.warning(bl.error || '自动化未完全成功，已尝试创建开庭提醒')
          await createQuickHearingItems()
        } else {
          ElMessage.success('已创建待办/日程（可在工作台或日历查看）')
        }
      } else {
        throw new Error(res.message || '创建失败')
      }
    } catch (e) {
      notifyAiError(e, { fallback: '创建待办/日程失败' })
    } finally {
      automationRunning.value = false
    }
    return
  }

  automationRunning.value = true
  try {
    await createQuickHearingItems()
  } finally {
    automationRunning.value = false
  }
}

async function createQuickHearingItems() {
  const assigneeId = userStore.userId
  if (!assigneeId) {
    ElMessage.warning('无法获取当前用户，请重新登录')
    return
  }
  const r = props.recognition
  const todoPayload = buildHearingTodoPayload(r, {
    caseId: resolvedCaseId.value,
    assigneeId
  })
  await createTodo(todoPayload)

  if (r.hearingDate) {
    const calPayload = buildHearingCalendarPayload(r, {
      caseId: resolvedCaseId.value
    })
    await createCalendar(calPayload)
    ElMessage.success('已创建开庭待办与日程')
  } else {
    ElMessage.success('已创建跟进待办（未识别到开庭时间，未建日程）')
  }
  emit('automation-done', {
    ...r,
    businessLogic: {
      success: true,
      manual: true,
      message: '已手动创建开庭提醒'
    }
  })
}

function goCase() {
  if (!resolvedCaseId.value) return
  router.push(`/case/detail/${resolvedCaseId.value}`)
}
</script>

<style scoped>
.recognition-actions {
  text-align: left;
  margin-top: 8px;
}
.recognition-desc {
  margin-bottom: 12px;
}
.bl-line {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 12px;
}
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
</style>
