<template>
  <div class="approval-workflow-panel">
    <el-form inline>
      <el-form-item label="审批类型">
        <el-select v-model="selectedType" style="width: 200px" @change="loadSteps">
          <el-option v-for="t in typeOptions" :key="t.code" :label="t.name" :value="t.code" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存流程</el-button>
        <el-button @click="loadSteps">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="steps" border>
      <el-table-column label="顺序" width="80">
        <template #default="{ row }">
          <el-input-number v-model="row.stepOrder" :min="1" :max="10" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="步骤名称">
        <template #default="{ row }">
          <el-input v-model="row.stepName" placeholder="如：部门主管审批" />
        </template>
      </el-table-column>
      <el-table-column label="审批人用户ID" width="140">
        <template #default="{ row }">
          <el-input-number v-model="row.approverUserId" :min="1" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="自动通过" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.autoApprove" />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" @click="steps.splice($index, 1)">删</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button style="margin-top: 12px" @click="addStep">添加步骤</el-button>
    <p class="hint">PRD 自定义流程：按审批类型配置多级审批人；首步「自动通过」则提交后直接通过。</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getApprovalTypes } from '@/api/approval'
import { getApprovalWorkflow, saveApprovalWorkflow } from '@/api/approvalWorkflow'

const selectedType = ref('SEAL')
const typeOptions = ref([])
const steps = ref([])
const saving = ref(false)

const loadTypes = async () => {
  try {
    const res = await getApprovalTypes()
    typeOptions.value = res.data || []
    if (typeOptions.value.length && !selectedType.value) {
      selectedType.value = typeOptions.value[0].code
    }
  } catch {
    typeOptions.value = [{ code: 'SEAL', name: '用印申请' }]
  }
}

const loadSteps = async () => {
  if (!selectedType.value) return
  try {
    const res = await getApprovalWorkflow(selectedType.value)
    steps.value = (res.data || []).map((s) => ({
      ...s,
      enabled: s.enabled !== false,
      autoApprove: !!s.autoApprove
    }))
    if (steps.value.length === 0) {
      addStep()
    }
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

const addStep = () => {
  steps.value.push({
    stepOrder: steps.value.length + 1,
    stepName: '审批步骤',
    approverUserId: 1,
    autoApprove: false,
    enabled: true
  })
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveApprovalWorkflow(selectedType.value, steps.value)
    ElMessage.success('流程已保存')
    loadSteps()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTypes()
  await loadSteps()
})
</script>

<style scoped>
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
