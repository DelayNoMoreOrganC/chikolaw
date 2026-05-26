<template>
  <div class="ai-document-fill">
    <el-dialog
      v-model="visible"
      title="文书智能识别填充"
      width="600px"
      :close-on-click-modal="false"
    >
      <!-- 上传区域 -->
      <div v-if="!recognitionResult" class="upload-section">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept="image/*,.pdf"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持jpg/png图片或PDF文件，文件大小不超过10MB
            </div>
          </template>
        </el-upload>

        <div v-if="selectedFile" class="file-info">
          <el-tag type="success">已选择：{{ selectedFile.name }}</el-tag>
        </div>
      </div>

      <!-- 识别结果展示 -->
      <div v-else class="result-section">
        <el-alert
          title="识别完成"
          type="success"
          :closable="false"
          show-icon
        >
          <template #default>
            <div>已从文档中提取以下信息，请核对后确认填充</div>
          </template>
        </el-alert>

        <el-form :model="recognitionResult" label-width="100px" class="result-form">
          <el-form-item label="案号">
            <el-input v-model="recognitionResult.caseNumber" placeholder="案号" />
          </el-form-item>

          <el-form-item label="法院名称">
            <el-input v-model="recognitionResult.courtName" placeholder="法院名称" />
          </el-form-item>

          <el-form-item label="文书类型">
            <el-select v-model="recognitionResult.documentType" placeholder="文书类型">
              <el-option label="传票" value="传票" />
              <el-option label="判决书" value="判决书" />
              <el-option label="裁定书" value="裁定书" />
              <el-option label="通知书" value="通知书" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="案由">
            <el-input v-model="recognitionResult.caseReason" placeholder="案由" />
          </el-form-item>

          <el-form-item label="原告">
            <el-input v-model="recognitionResult.plaintiffName" placeholder="原告姓名" />
          </el-form-item>

          <el-form-item label="被告">
            <el-input v-model="recognitionResult.defendantName" placeholder="被告姓名" />
          </el-form-item>

          <el-form-item label="开庭时间">
            <el-date-picker
              v-model="hearingDate"
              type="datetime"
              placeholder="开庭时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
          </el-form-item>

          <el-form-item label="开庭地点">
            <el-input v-model="recognitionResult.hearingPlace" placeholder="开庭地点" />
          </el-form-item>

          <el-form-item label="审判长">
            <el-input v-model="recognitionResult.judgeName" placeholder="审判长" />
          </el-form-item>

          <el-form-item label="书记员">
            <el-input v-model="recognitionResult.clerkName" placeholder="书记员" />
          </el-form-item>

          <el-form-item label="联系电话">
            <el-input v-model="recognitionResult.contactPhone" placeholder="联系电话" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作按钮 -->
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button
            v-if="!recognitionResult"
            type="primary"
            :loading="recognizing"
            @click="handleRecognize"
            :disabled="!selectedFile"
          >
            开始识别
          </el-button>
          <el-button
            v-else
            type="success"
            @click="handleConfirm"
          >
            确认填充
          </el-button>
          <el-button
            v-if="recognitionResult"
            @click="handleReset"
          >
            重新识别
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { recognizeLegalDocument } from '@/api/ai'

const props = defineProps({
  modelValue: Boolean,
  caseId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const uploadRef = ref(null)
const selectedFile = ref(null)
const recognizing = ref(false)
const recognitionResult = ref(null)

const hearingDate = computed({
  get: () => recognitionResult.value?.hearingDate || null,
  set: (val) => {
    if (recognitionResult.value) {
      recognitionResult.value.hearingDate = val
    }
  }
})

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

// 开始识别
const handleRecognize = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    recognizing.value = true
    ElMessage.info('正在识别文档，请稍候...')

    const res = await recognizeLegalDocument(selectedFile.value, props.caseId)

    if (res.code === 200) {
      recognitionResult.value = res.data
      ElMessage.success('识别成功！')
    } else {
      ElMessage.error(res.message || '识别失败')
    }
  } catch (error) {
    console.error('识别失败:', error)
    ElMessage.error('识别失败: ' + (error.message || '未知错误'))
  } finally {
    recognizing.value = false
  }
}

// 确认填充
const handleConfirm = () => {
  emit('confirm', recognitionResult.value)
  handleCancel()
}

// 取消
const handleCancel = () => {
  visible.value = false
  handleReset()
}

// 重置
const handleReset = () => {
  selectedFile.value = null
  recognitionResult.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}
</script>

<style scoped lang="scss">
.ai-document-fill {
  .upload-section {
    .upload-demo {
      width: 100%;
    }

    .file-info {
      margin-top: 20px;
      text-align: center;
    }
  }

  .result-section {
    .el-alert {
      margin-bottom: 20px;
    }

    .result-form {
      margin-top: 20px;
      max-height: 400px;
      overflow-y: auto;
    }
  }
}
</style>