import { ref, onUnmounted } from 'vue'

/**
 * AI 长耗时任务进度（文案 + 已等待秒数）
 */
export function useAiProgress() {
  const active = ref(false)
  const title = ref('')
  const hint = ref('')
  const elapsedSec = ref(0)

  let timer = null

  function start({ title: t, hint: h }) {
    stop()
    active.value = true
    title.value = t || 'AI 处理中'
    hint.value = h || '预计需要 30 秒至 2 分钟，请勿关闭页面或重复点击。'
    elapsedSec.value = 0
    timer = setInterval(() => {
      elapsedSec.value += 1
    }, 1000)
  }

  function stop() {
    active.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onUnmounted(stop)

  return { active, title, hint, elapsedSec, start, stop }
}

/** 按场景返回预计耗时文案 */
export const AI_PROGRESS_HINTS = {
  recognize: '正在识别文书要素（Vision + 提取），预计 30 秒至 2 分钟。',
  intake: '正在分析并归入卷宗，预计 30 秒至 2 分钟。',
  docGen: '正在生成法律文书，预计 1 至 2 分钟。',
  qa: '正在检索并生成回答，预计 20 秒至 1 分钟。'
}
