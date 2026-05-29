<template>
  <el-alert
    v-if="active"
    class="ai-progress-banner"
    type="info"
    :closable="false"
    show-icon
  >
    <template #title>{{ title }}</template>
    <p class="hint">{{ hint }}</p>
    <p class="elapsed">已等待 {{ elapsedSec }} 秒 · 请勿关闭页面或重复点击</p>
    <el-progress :percentage="100" :indeterminate="true" :duration="1.4" :show-text="false" />
  </el-alert>
</template>

<script setup>
defineProps({
  active: { type: Boolean, default: false },
  title: { type: String, default: 'AI 处理中' },
  hint: { type: String, default: '' },
  elapsedSec: { type: Number, default: 0 }
})
</script>

<style scoped>
.ai-progress-banner {
  margin-bottom: 16px;
  text-align: left;
}
.hint,
.elapsed {
  margin: 4px 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.el-progress {
  margin-top: 10px;
}
</style>
