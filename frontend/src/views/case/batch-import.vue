<template>
  <div class="case-batch-import">
    <PageHeader title="批量收案" :show-back="true" @back="$router.back()" />

    <el-card>
      <el-alert
        title="Excel列顺序：委托银行、债务人/被告、资产批次号、业务子类、案件编号、借款合同号、转让协议号、本金余额、利息余额、担保方式、抵押物状态、案件程序、案由、联系电话、身份证号、地址"
        type="info"
        :closable="false"
        show-icon
      />

      <el-upload
        class="import-upload"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-remove="handleRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽Excel到此处，或点击选择文件</div>
      </el-upload>

      <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleImport">
        开始导入
      </el-button>
    </el-card>

    <el-card v-if="importResult" class="result-card">
      <template #header>导入结果</template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="总行数">{{ importResult.totalRows }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ importResult.successRows }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ importResult.failedRows }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="importResult.rows || []" style="margin-top: 16px">
        <el-table-column prop="rowNumber" label="行号" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="caseNumber" label="案件编号" />
        <el-table-column prop="caseId" label="案件ID" width="120" />
        <el-table-column prop="error" label="失败原因" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { importNpaCases } from '@/api/case'

const selectedFile = ref(null)
const uploading = ref(false)
const importResult = ref(null)

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const handleRemove = () => {
  selectedFile.value = null
}

const handleImport = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const res = await importNpaCases(selectedFile.value)
    if (res.success) {
      importResult.value = res.data
      ElMessage.success('导入完成')
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.import-upload {
  margin: 20px 0;
}

.result-card {
  margin-top: 20px;
}
</style>
