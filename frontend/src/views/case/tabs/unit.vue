<template>
  <div class="case-unit">
    <el-tabs v-model="activeTab" type="card">
      <!-- 财产保全 -->
      <el-tab-pane label="财产保全" name="preservation">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddPreservation">
              <el-icon><Plus /></el-icon>
              添加保全
            </el-button>
          </div>

          <el-table :data="preservationList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="target" label="被申请人" width="150" />
            <el-table-column prop="subject" label="保全标的" width="200" />
            <el-table-column prop="amount" label="金额(元)" width="150">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="date" label="保全日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditPreservation(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeletePreservation(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 执行情况 -->
      <el-tab-pane label="执行情况" name="execution">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddExecution">
              <el-icon><Plus /></el-icon>
              添加执行
            </el-button>
          </div>

          <el-table :data="executionList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="caseNumber" label="执行案号" width="180" />
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="respondent" label="被执行人" width="150" />
            <el-table-column prop="subject" label="执行标的" width="150" />
            <el-table-column prop="amount" label="金额(元)" width="120">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="date" label="执行日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditExecution(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteExecution(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 庭审记录 -->
      <el-tab-pane label="庭审记录" name="hearing">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddHearing">
              <el-icon><Plus /></el-icon>
              添加庭审
            </el-button>
          </div>

          <el-table :data="hearingList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="date" label="日期" width="120" sortable />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getHearingTypeTagType(row.type)">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="courtroom" label="法庭号" width="120" />
            <el-table-column prop="judge" label="审判员" width="120" />
            <el-table-column prop="clerk" label="书记员" width="120" />
            <el-table-column label="笔录" width="200">
              <template #default="{ row }">
                <div v-if="row.transcript" class="transcript-files">
                  <el-tag
                    v-for="file in row.transcript"
                    :key="file.id"
                    closable
                    @close="handleDeleteTranscript(row, file)"
                  >
                    {{ file.name }}
                  </el-tag>
                </div>
                <span v-else class="text-muted">暂无笔录</span>
              </template>
            </el-table-column>
            <el-table-column label="附件" width="200">
              <template #default="{ row }">
                <div v-if="row.attachments && row.attachments.length > 0" class="attachment-list">
                  <el-tag
                    v-for="file in row.attachments"
                    :key="file.id"
                    closable
                    @close="handleDeleteAttachment(row, file)"
                  >
                    {{ file.name }}
                  </el-tag>
                </div>
                <span v-else class="text-muted">暂无附件</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditHearing(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteHearing(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 承办人员 -->
      <el-tab-pane label="承办人员" name="personnel">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddPersonnel">
              <el-icon><Plus /></el-icon>
              添加人员
            </el-button>
          </div>

          <el-table :data="personnelList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="position" label="职位" width="120">
              <template #default="{ row }">
                <el-tag :type="getPositionTagType(row.position)">
                  {{ row.position }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="150" />
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="department" label="部门" width="150" />
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditPersonnel(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeletePersonnel(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 财产保全对话框 -->
    <el-dialog v-model="preservationDialogVisible" :title="preservationForm.id ? '编辑财产保全' : '添加财产保全'" width="700px">
      <el-form :model="preservationForm" label-width="120px">
        <el-form-item label="被申请人" required>
          <el-input v-model="preservationForm.targetPerson" placeholder="请输入被申请人姓名" />
        </el-form-item>
        <el-form-item label="保全标的" required>
          <el-input v-model="preservationForm.preservationTarget" placeholder="请输入保全标的" />
        </el-form-item>
        <el-form-item label="保全金额">
          <el-input-number v-model="preservationForm.preservationAmount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保全法院">
          <el-input v-model="preservationForm.preservationCourt" placeholder="请输入保全法院" />
        </el-form-item>
        <el-form-item label="保全日期">
          <el-date-picker
            v-model="preservationForm.preservationDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保全状态">
          <el-select v-model="preservationForm.preservationStatus" placeholder="请选择状态" style="width: 100%">
            <el-option label="待办理" value="待办理" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已解除" value="已解除" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="preservationForm.notes" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="preservationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitPreservation">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行情况对话框 -->
    <el-dialog v-model="executionDialogVisible" :title="executionForm.id ? '编辑执行情况' : '添加执行情况'" width="700px">
      <el-form :model="executionForm" label-width="120px">
        <el-form-item label="执行案号" required>
          <el-input v-model="executionForm.caseNumber" placeholder="请输入执行案号" />
        </el-form-item>
        <el-form-item label="法院">
          <el-input v-model="executionForm.court" placeholder="请输入执行法院" />
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="executionForm.applicant" placeholder="请输入申请人" />
        </el-form-item>
        <el-form-item label="被执行人">
          <el-input v-model="executionForm.respondent" placeholder="请输入被执行人" />
        </el-form-item>
        <el-form-item label="执行标的">
          <el-input v-model="executionForm.subject" placeholder="请输入执行标的" />
        </el-form-item>
        <el-form-item label="执行金额">
          <el-input-number v-model="executionForm.amount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执行日期">
          <el-date-picker
            v-model="executionForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select v-model="executionForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已完毕" value="已完毕" />
            <el-option label="已终止" value="已终止" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="executionForm.notes" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitExecution">确定</el-button>
      </template>
    </el-dialog>

    <!-- 庭审记录对话框 -->
    <el-dialog v-model="hearingDialogVisible" :title="hearingForm.id ? '编辑庭审记录' : '添加庭审记录'" width="700px">
      <el-form :model="hearingForm" label-width="120px">
        <el-form-item label="庭审日期" required>
          <el-date-picker
            v-model="hearingForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="庭审类型" required>
          <el-select v-model="hearingForm.type" placeholder="请选择庭审类型" style="width: 100%">
            <el-option label="开庭" value="开庭" />
            <el-option label="谈话" value="谈话" />
            <el-option label="调解" value="调解" />
            <el-option label="证据交换" value="证据交换" />
          </el-select>
        </el-form-item>
        <el-form-item label="法庭号">
          <el-input v-model="hearingForm.courtroom" placeholder="请输入法庭号" />
        </el-form-item>
        <el-form-item label="审判员">
          <el-input v-model="hearingForm.judge" placeholder="请输入审判员姓名" />
        </el-form-item>
        <el-form-item label="书记员">
          <el-input v-model="hearingForm.clerk" placeholder="请输入书记员姓名" />
        </el-form-item>
        <el-form-item label="庭审状态">
          <el-select v-model="hearingForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
            <el-option label="已改期" value="已改期" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="hearingForm.notes" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hearingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitHearing">确定</el-button>
      </template>
    </el-dialog>

    <!-- 承办人员对话框 -->
    <el-dialog v-model="personnelDialogVisible" :title="personnelForm.id ? '编辑承办人员' : '添加承办人员'" width="600px">
      <el-form :model="personnelForm" label-width="120px">
        <el-form-item label="姓名" required>
          <el-input v-model="personnelForm.name" placeholder="请输入人员姓名" />
        </el-form-item>
        <el-form-item label="职位" required>
          <el-select v-model="personnelForm.position" placeholder="请选择职位" style="width: 100%">
            <el-option label="审判员" value="审判员" />
            <el-option label="书记员" value="书记员" />
            <el-option label="法官助理" value="法官助理" />
            <el-option label="执行员" value="执行员" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="personnelForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="法院">
          <el-input v-model="personnelForm.court" placeholder="请输入法院名称" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="personnelForm.department" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="personnelForm.notes" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="personnelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitPersonnel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'

// 监听案件数据变化，加载受理单位数据
watch(() => props.caseData.id, (newId) => {
  if (newId) {
    loadUnitData()
  }
}, { immediate: true })
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getPreservations,
  createPreservation,
  updatePreservation,
  deletePreservation,
  getExecutions,
  createExecution,
  updateExecution,
  deleteExecution,
  getHearings,
  createHearing,
  updateHearing,
  deleteHearing,
  getPersonnel,
  createPersonnel,
  updatePersonnel,
  deletePersonnel
} from '@/api/case'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

const activeTab = ref('preservation')
const loading = ref(false)

// 加载受理单位数据
const loadUnitData = async () => {
  if (!props.caseData.id) return

  try {
    loading.value = true

    // 根据当前Tab加载对应数据
    if (activeTab.value === 'preservation') {
      const res = await getPreservations(props.caseData.id)
      // 映射后端字段到前端显示字段
      preservationList.value = (res.data || []).map(item => ({
        id: item.id,
        target: item.targetPerson || item.target || '待填写',
        subject: item.preservationTarget || item.subject || '',
        amount: item.preservationAmount || item.amount || 0,
        court: item.preservationCourt || item.court || '待填写',
        date: item.preservationDate || item.date || '',
        status: item.preservationStatus || item.status || '待办理'
      }))
    } else if (activeTab.value === 'execution') {
      const res = await getExecutions(props.caseData.id)
      executionList.value = res.data || []
    } else if (activeTab.value === 'hearing') {
      const res = await getHearings(props.caseData.id)
      hearingList.value = res.data || []
    } else if (activeTab.value === 'personnel') {
      const res = await getPersonnel(props.caseData.id)
      personnelList.value = res.data || []
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    // 如果是404错误，说明后端API未实现，使用空列表
    if (error.response?.status === 404) {
      if (activeTab.value === 'preservation') preservationList.value = []
      else if (activeTab.value === 'execution') executionList.value = []
      else if (activeTab.value === 'hearing') hearingList.value = []
      else if (activeTab.value === 'personnel') personnelList.value = []
    } else {
      ElMessage.error('加载数据失败')
    }
  } finally {
    loading.value = false
  }
}

// 监听Tab切换
const handleTabChange = () => {
  loadUnitData()
}

// 组件挂载时加载数据
onMounted(() => {
  loadUnitData()
})

// 财产保全列表
const preservationList = ref([])

// 执行情况列表
const executionList = ref([])

// 庭审记录列表
const hearingList = ref([])

// 承办人员列表
const personnelList = ref([])

// 监听activeTab变化，自动加载数据
const watchActiveTab = () => {
  loadUnitData()
}

// 添加watch监听
watch(() => activeTab.value, () => {
  watchActiveTab()
})

// 获取状态标签颜色
const getStatusTagType = (status) => {
  const typeMap = {
    '进行中': 'primary',
    '已完成': 'success',
    '已终止': 'info',
    '失败': 'danger'
  }
  return typeMap[status] || ''
}

// 获取庭审类型标签颜色
const getHearingTypeTagType = (type) => {
  const typeMap = {
    '开庭': 'danger',
    '谈话': 'warning',
    '调解': 'success',
    '证据交换': 'info'
  }
  return typeMap[type] || ''
}

// 获取职位标签颜色
const getPositionTagType = (position) => {
  const typeMap = {
    '审判员': 'danger',
    '书记员': 'primary',
    '法官助理': 'success',
    '执行员': 'warning'
  }
  return typeMap[position] || ''
}

// 财产保全对话框
const preservationDialogVisible = ref(false)
const preservationForm = ref({
  targetPerson: '',
  preservationTarget: '',
  preservationAmount: null,
  preservationCourt: '',
  preservationDate: '',
  preservationStatus: '待办理',
  notes: ''
})

// 财产保全操作
const handleAddPreservation = () => {
  preservationForm.value = {
    targetPerson: '',
    preservationTarget: '',
    preservationAmount: null,
    preservationCourt: '',
    preservationDate: new Date().toISOString().split('T')[0],
    preservationStatus: '待办理',
    notes: ''
  }
  preservationDialogVisible.value = true
}

const handleEditPreservation = (row) => {
  preservationForm.value = {
    id: row.id,
    targetPerson: row.target,
    preservationTarget: row.subject,
    preservationAmount: row.amount,
    preservationCourt: row.court,
    preservationDate: row.date,
    preservationStatus: row.status,
    notes: row.notes || ''
  }
  preservationDialogVisible.value = true
}

const handleSubmitPreservation = async () => {
  if (!preservationForm.value.preservationTarget) {
    ElMessage.warning('请输入保全标的')
    return
  }
  if (!preservationForm.value.targetPerson) {
    ElMessage.warning('请输入被申请人')
    return
  }

  try {
    const data = {
      targetPerson: preservationForm.value.targetPerson,
      preservationTarget: preservationForm.value.preservationTarget,
      preservationAmount: preservationForm.value.preservationAmount || 0,
      preservationCourt: preservationForm.value.preservationCourt,
      preservationDate: preservationForm.value.preservationDate,
      preservationStatus: preservationForm.value.preservationStatus,
      notes: preservationForm.value.notes
    }

    if (preservationForm.value.id) {
      await updatePreservation(props.caseData.id, preservationForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createPreservation(props.caseData.id, data)
      ElMessage.success('添加成功')
    }

    preservationDialogVisible.value = false
    await loadUnitData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeletePreservation = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除保全标的"${row.subject}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deletePreservation(props.caseData.id, row.id)
    await loadUnitData()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除保全失败:', error)
      ElMessage.error('删除保全失败')
    }
  }
}

// 执行情况对话框
const executionDialogVisible = ref(false)
const executionForm = ref({
  caseNumber: '',
  court: '',
  applicant: '',
  respondent: '',
  subject: '',
  amount: null,
  date: '',
  status: '进行中',
  notes: ''
})

// 执行情况操作
const handleAddExecution = () => {
  executionForm.value = {
    caseNumber: '',
    court: '',
    applicant: '',
    respondent: '',
    subject: '',
    amount: null,
    date: new Date().toISOString().split('T')[0],
    status: '进行中',
    notes: ''
  }
  executionDialogVisible.value = true
}

const handleEditExecution = (row) => {
  executionForm.value = {
    id: row.id,
    caseNumber: row.caseNumber,
    court: row.court,
    applicant: row.applicant,
    respondent: row.respondent,
    subject: row.subject,
    amount: row.amount,
    date: row.date,
    status: row.status,
    notes: row.notes || ''
  }
  executionDialogVisible.value = true
}

const handleSubmitExecution = async () => {
  if (!executionForm.value.caseNumber) {
    ElMessage.warning('请输入执行案号')
    return
  }

  try {
    const data = {
      caseNumber: executionForm.value.caseNumber,
      court: executionForm.value.court,
      applicant: executionForm.value.applicant,
      respondent: executionForm.value.respondent,
      subject: executionForm.value.subject,
      amount: executionForm.value.amount || 0,
      date: executionForm.value.date,
      status: executionForm.value.status,
      notes: executionForm.value.notes
    }

    if (executionForm.value.id) {
      await updateExecution(props.caseData.id, executionForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createExecution(props.caseData.id, data)
      ElMessage.success('添加成功')
    }

    executionDialogVisible.value = false
    await loadUnitData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeleteExecution = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除执行案号"${row.caseNumber}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteExecution(props.caseData.id, row.id)
    await loadUnitData()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除执行失败:', error)
      ElMessage.error('删除执行失败')
    }
  }
}

// 庭审记录对话框
const hearingDialogVisible = ref(false)
const hearingForm = ref({
  date: '',
  type: '开庭',
  courtroom: '',
  judge: '',
  clerk: '',
  status: '进行中',
  notes: ''
})

// 庭审记录操作
const handleAddHearing = () => {
  hearingForm.value = {
    date: new Date().toISOString().split('T')[0],
    type: '开庭',
    courtroom: '',
    judge: '',
    clerk: '',
    status: '进行中',
    notes: ''
  }
  hearingDialogVisible.value = true
}

const handleEditHearing = (row) => {
  hearingForm.value = {
    id: row.id,
    date: row.date,
    type: row.type,
    courtroom: row.courtroom,
    judge: row.judge,
    clerk: row.clerk,
    status: row.status,
    notes: row.notes || ''
  }
  hearingDialogVisible.value = true
}

const handleSubmitHearing = async () => {
  if (!hearingForm.value.date) {
    ElMessage.warning('请选择庭审日期')
    return
  }

  try {
    const data = {
      date: hearingForm.value.date,
      type: hearingForm.value.type,
      courtroom: hearingForm.value.courtroom,
      judge: hearingForm.value.judge,
      clerk: hearingForm.value.clerk,
      status: hearingForm.value.status,
      notes: hearingForm.value.notes
    }

    if (hearingForm.value.id) {
      await updateHearing(props.caseData.id, hearingForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createHearing(props.caseData.id, data)
      ElMessage.success('添加成功')
    }

    hearingDialogVisible.value = false
    await loadUnitData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeleteHearing = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除${row.type}记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteHearing(props.caseData.id, row.id)
    await loadUnitData()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除庭审失败:', error)
      ElMessage.error('删除庭审失败')
    }
  }
}

const handleDeleteTranscript = async (row, file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除笔录"${file.name}"吗？`,
      '删除笔录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从庭审记录中移除笔录
    const index = row.transcripts?.indexOf(file)
    if (index > -1) {
      row.transcripts.splice(index, 1)
    }

    ElMessage.success('笔录删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除笔录失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleDeleteAttachment = async (row, file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除附件"${file.name}"吗？`,
      '删除附件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从庭审记录中移除附件
    const index = row.attachments?.indexOf(file)
    if (index > -1) {
      row.attachments.splice(index, 1)
    }

    ElMessage.success('附件删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除附件失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 承办人员对话框
const personnelDialogVisible = ref(false)
const personnelForm = ref({
  name: '',
  position: '',
  phone: '',
  court: '',
  department: '',
  notes: ''
})

// 承办人员操作
const handleAddPersonnel = () => {
  personnelForm.value = {
    name: '',
    position: '',
    phone: '',
    court: '',
    department: '',
    notes: ''
  }
  personnelDialogVisible.value = true
}

const handleEditPersonnel = (row) => {
  personnelForm.value = {
    id: row.id,
    name: row.name,
    position: row.position,
    phone: row.phone,
    court: row.court,
    department: row.department,
    notes: row.notes || ''
  }
  personnelDialogVisible.value = true
}

const handleSubmitPersonnel = async () => {
  if (!personnelForm.value.name) {
    ElMessage.warning('请输入人员姓名')
    return
  }

  try {
    const data = {
      name: personnelForm.value.name,
      position: personnelForm.value.position,
      phone: personnelForm.value.phone,
      court: personnelForm.value.court,
      department: personnelForm.value.department,
      notes: personnelForm.value.notes
    }

    if (personnelForm.value.id) {
      await updatePersonnel(props.caseData.id, personnelForm.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createPersonnel(props.caseData.id, data)
      ElMessage.success('添加成功')
    }

    personnelDialogVisible.value = false
    await loadUnitData()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeletePersonnel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除人员"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deletePersonnel(props.caseData.id, row.id)
    await loadUnitData()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除人员失败:', error)
      ElMessage.error('删除人员失败')
    }
  }
}
</script>

<style scoped lang="scss">
.case-unit {
  padding: 30px;

  .tab-content {
    .toolbar {
      margin-bottom: 20px;
    }

    .transcript-files,
    .attachment-list {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .text-muted {
      color: #909399;
      font-size: 13px;
    }
  }
}
</style>
