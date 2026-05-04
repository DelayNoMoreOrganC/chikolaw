<template>
  <div class="ac-calculator">
    <PageHeader title="债权精算工具" />

    <div class="calculator-container">
      <el-alert
        title="工具说明"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        AC债权精算工具用于金融不良资产的债权计算，支持PDF文档识别、大模型信息提取、复杂计算逻辑和Excel台账导出。
      </el-alert>

      <div class="iframe-wrapper">
        <iframe
          :src="AC_CALC_URL"
          width="100%"
          height="100%"
          frameborder="0"
          sandbox="allow-same-origin allow-scripts allow-forms allow-popups allow-modals"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'

// AC精算工具地址（Streamlit默认端口）
const AC_CALC_URL = ref('http://localhost:8501')
</script>

<style scoped>
.ac-calculator {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.calculator-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.iframe-wrapper {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.iframe-wrapper iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
