<template>
  <div class="calendar-copilot" :class="{ collapsed }">
    <div class="copilot-header">
      <div v-if="!collapsed" class="header-title">
        <el-icon><MagicStick /></el-icon>
        <span>AI 副驾</span>
      </div>
      <el-button text size="small" @click="collapsed = !collapsed">
        {{ collapsed ? 'AI' : '收起' }}
      </el-button>
    </div>

    <template v-if="!collapsed">
      <p class="copilot-hint">基于本周日程与待办，快捷提问</p>

      <div v-if="selectedEvent?.data?.caseName" class="context-chip">
        已选：{{ selectedEvent.data.caseName }}
      </div>

      <div class="quick-actions">
        <el-button
          v-for="action in quickActions"
          :key="action.key"
          size="small"
          plain
          :disabled="action.disabled || loading"
          @click="runQuickAction(action)"
        >
          {{ action.label }}
        </el-button>
      </div>

      <div class="chat-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="例如：本周有哪些开庭要准备？"
          maxlength="500"
          show-word-limit
          @keydown.ctrl.enter="sendMessage"
        />
        <el-button
          type="primary"
          size="small"
          class="send-btn"
          :loading="loading"
          :disabled="!userInput.trim()"
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>

      <div v-if="lastReply" class="reply-box">
        <div class="reply-label">AI 回复</div>
        <div class="reply-content">{{ lastReply }}</div>
      </div>
      <div v-else-if="errorMsg" class="reply-box error">
        {{ errorMsg }}
      </div>

      <el-button text type="primary" size="small" class="hub-link" @click="router.push('/ai-hub')">
        深度文书生成 → AI 智能中心
      </el-button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick } from '@element-plus/icons-vue'
import { aiChat, caseChat } from '@/api/ai'
import { calendarTypeLabel, formatDateToString, startOfWeek, endOfWeek } from '@/utils/calendarUi'

const props = defineProps({
  events: { type: Array, default: () => [] },
  todos: { type: Array, default: () => [] },
  selectedEvent: { type: Object, default: null },
  weekDate: { type: Date, default: () => new Date() }
})

const router = useRouter()
const collapsed = ref(false)
const userInput = ref('')
const loading = ref(false)
const lastReply = ref('')
const errorMsg = ref('')

const quickActions = computed(() => {
  const hasCase = !!props.selectedEvent?.data?.caseId
  return [
    { key: 'summary', label: '本周办案摘要', prompt: '请根据以下本周日程与待办，生成简洁的办案摘要（3-5条要点）。' },
    { key: 'overdue', label: '逾期待办建议', prompt: '请分析以下逾期待办，给出优先级排序与处理建议。' },
    { key: 'hearing', label: '开庭准备要点', prompt: '请针对以下本周开庭日程，列出庭前准备 checklist。' },
    {
      key: 'materials',
      label: '案件缺什么材料',
      prompt: '请结合该案件信息与本周相关日程，分析卷宗可能缺少的材料清单。',
      disabled: !hasCase,
      useCaseChat: hasCase
    }
  ]
})

function buildContextBlock() {
  const start = formatDateToString(startOfWeek(props.weekDate))
  const end = formatDateToString(endOfWeek(props.weekDate))
  const lines = [`【时间范围】${start} 至 ${end}`]

  const evLines = (props.events || []).slice(0, 30).map((e) => {
    const d = e.data || e
    return `- ${e.startTime || d.startTime || ''} ${e.title || d.title}（${calendarTypeLabel(d.calendarType || e.type)}）${d.caseName ? ` 案件：${d.caseName}` : ''}`
  })
  lines.push('【本周日程】')
  lines.push(evLines.length ? evLines.join('\n') : '（无）')

  const pending = (props.todos || []).filter((t) => !t.completed && t.status !== 'COMPLETED')
  const overdue = pending.filter((t) => new Date(t.deadline) < new Date())
  lines.push('【待办（未完成）】')
  if (pending.length) {
    pending.slice(0, 15).forEach((t) => {
      lines.push(`- ${t.title} 截止：${t.deadline}${t.caseName ? ` 案件：${t.caseName}` : ''}`)
    })
  } else {
    lines.push('（无）')
  }
  if (overdue.length) {
    lines.push(`【逾期待办 ${overdue.length} 项】`)
    overdue.forEach((t) => lines.push(`- ${t.title}`))
  }

  if (props.selectedEvent?.data) {
    const s = props.selectedEvent.data
    lines.push('【当前选中日程】')
    lines.push(`${s.title} | ${calendarTypeLabel(s.calendarType)} | 案件：${s.caseName || '无'}`)
  }

  return lines.join('\n')
}

async function askAi(prompt, useCaseChat = false) {
  loading.value = true
  errorMsg.value = ''
  lastReply.value = ''
  try {
    const fullMessage = `${buildContextBlock()}\n\n【请回答】\n${prompt}`
    let res
    const caseId = props.selectedEvent?.data?.caseId
    if (useCaseChat && caseId) {
      res = await caseChat(caseId, { message: fullMessage })
    } else {
      res = await aiChat({ message: fullMessage })
    }
    const text = res.data?.content || res.data?.reply || res.data || res.message
    if (typeof text === 'string' && text.trim()) {
      lastReply.value = text.trim()
    } else if (res.code === 200 || res.success) {
      lastReply.value = typeof res.data === 'string' ? res.data : JSON.stringify(res.data)
    } else {
      throw new Error(res.message || 'AI 未返回有效内容')
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || e.message || 'AI 请求失败，请检查模型配置'
  } finally {
    loading.value = false
  }
}

function runQuickAction(action) {
  askAi(action.prompt, action.useCaseChat)
}

function sendMessage() {
  const msg = userInput.value.trim()
  if (!msg || loading.value) return
  askAi(msg, !!props.selectedEvent?.data?.caseId && msg.includes('材料'))
  userInput.value = ''
}
</script>

<style scoped lang="scss">
.calendar-copilot {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.08));
  border-radius: var(--lawos-radius-md, 8px);
  background: linear-gradient(180deg, #faf8ff 0%, #fff 40%);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 520px;
  overflow-y: auto;

  &.collapsed {
    width: 48px;
    padding: 8px 4px;
    align-items: center;

    .copilot-header {
      flex-direction: column;
    }
  }

  .copilot-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 600;
      font-size: 14px;
      color: #531dab;
    }
  }

  .copilot-hint {
    margin: 0;
    font-size: 12px;
    color: #8c8c8c;
    line-height: 1.5;
  }

  .context-chip {
    font-size: 12px;
    padding: 4px 8px;
    background: #f0f5ff;
    border-radius: 4px;
    color: #1d39c4;
  }

  .quick-actions {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .el-button {
      margin: 0;
      justify-content: flex-start;
    }
  }

  .chat-area {
    .send-btn {
      margin-top: 8px;
      width: 100%;
    }
  }

  .reply-box {
    padding: 10px;
    background: #f5f5f5;
    border-radius: 6px;
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    max-height: 200px;
    overflow-y: auto;

    &.error {
      background: #fff2f0;
      color: #cf1322;
    }

    .reply-label {
      font-size: 11px;
      color: #8c8c8c;
      margin-bottom: 4px;
    }
  }

  .hub-link {
    align-self: flex-start;
    padding: 0;
  }
}

@media (max-width: 1100px) {
  .calendar-copilot:not(.collapsed) {
    width: 100%;
    max-height: none;
  }
}
</style>
