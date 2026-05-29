<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="900px"
    destroy-on-close
    @closed="revokePreviewBlob"
  >
    <div v-loading="loading" class="preview-container">
      <img
        v-if="previewMode === 'image' && previewBlobUrl"
        :src="previewBlobUrl"
        class="preview-media"
        alt="预览"
      />
      <iframe
        v-else-if="previewMode === 'pdf' && previewBlobUrl"
        :src="previewBlobUrl"
        class="preview-frame"
        title="PDF 预览"
      />
      <iframe
        v-else-if="previewMode === 'html' && previewHtmlContent"
        :srcdoc="previewHtmlContent"
        class="preview-frame"
        sandbox=""
        title="Office 预览"
      />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useDocumentPreview } from '@/composables/useDocumentPreview'

const api = useDocumentPreview()

const {
  visible,
  loading,
  previewMode,
  previewBlobUrl,
  previewHtmlContent,
  previewFile,
  preview,
  downloadFile,
  revokePreviewBlob
} = api

const dialogTitle = computed(() =>
  previewFile.value?.name ? `预览：${previewFile.value.name}` : '文件预览'
)

defineExpose({ preview, downloadFile })
</script>

<style scoped lang="scss">
.preview-container {
  min-height: 400px;
}

.preview-media {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
}

.preview-frame {
  width: 100%;
  height: 70vh;
  border: none;
}
</style>
