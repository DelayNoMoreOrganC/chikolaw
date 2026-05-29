<template>
  <div class="approval">
    <PageHeader title="审批管理">
      <template #extra>
        <el-button type="primary" @click="handleCreateApproval" class="create-btn">
          <el-icon><Plus /></el-icon>
          发起审批
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选器 -->
    <ApprovalFilter @search="handleSearch" @reset="handleReset" />

    <el-tabs v-model="activeTab" type="card" class="approval-tabs"
      :class="'tab-' + activeTab">
      <!-- 待办 -->
      <el-tab-pane label="待办" name="pending">
        <div class="tab-content">
          <el-table :data="pendingList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ formatApprovalType(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentNode" label="当前节点" width="120" />
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row)">
                  同意
                </el-button>
                <el-button type="warning" size="small" @click="handleReject(row)">
                  驳回
                </el-button>
                <el-button type="primary" size="small" @click="handleTransfer(row)">
                  转审
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 已办 -->
      <el-tab-pane label="已办" name="processed">
        <div class="tab-content">
          <el-table :data="processedList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ formatApprovalType(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="processTime" label="处理时间" width="160" sortable />
            <el-table-column prop="result" label="处理结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === '同意' ? 'success' : row.result === '驳回' ? 'danger' : 'info'">
                  {{ row.result }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="处理意见" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我发起的 -->
      <el-tab-pane label="我发起的" name="my-requests">
        <div class="tab-content">
          <el-table :data="myRequestList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ formatApprovalType(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentNode" label="当前节点" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
                <el-button link type="primary" size="small">进度</el-button>
                <el-button
                  v-if="row.approvalType === 'CASE_FILING' && isApproved(row)"
                  link
                  type="success"
                  size="small"
                  @click="goCreateCaseFromFiling(row)"
                >
                  确认建案
                </el-button>
                <el-button
                  link
                  type="info"
                  size="small"
                  :disabled="!isPendingStatus(row)"
                  @click="handleUrge(row)"
                >
                  催办
                </el-button>
                <el-button
                  link
                  type="warning"
                  size="small"
                  :disabled="!isPendingStatus(row)"
                  @click="handleWithdraw(row)"
                >
                  撤回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 发起审批对话框 -->
    <el-dialog v-model="approvalDialogVisible" title="发起审批" width="700px">
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批标题" required>
          <el-input v-model="approvalForm.title" placeholder="请输入审批标题" />
        </el-form-item>

        <el-form-item label="审批类型" required>
          <el-select v-model="approvalForm.approvalType" placeholder="请选择审批类型" style="width: 100%">
            <el-option
              v-for="t in approvalTypeOptions"
              :key="t.code"
              :label="t.name"
              :value="t.code"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="approvalForm.caseId"
            filterable
            placeholder="请选择案件（可选）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="紧急程度">
          <el-radio-group v-model="approvalForm.urgency">
            <el-radio label="NORMAL">普通</el-radio>
            <el-radio label="HIGH">紧急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="审批内容" required>
          <el-input
            v-model="approvalForm.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述审批事由、金额、时间等关键信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApproval">提交审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDialogVisible" title="转审" width="480px" @closed="resetTransferForm">
      <el-form label-width="100px">
        <el-form-item label="转给" required>
          <el-select
            v-model="transferForm.newApproverId"
            filterable
            placeholder="选择新审批人"
            style="width: 100%"
          >
            <el-option
              v-for="u in approverOptions"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转审原因" required>
          <el-input v-model="transferForm.comments" type="textarea" :rows="3" placeholder="请输入转审原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="submitTransfer">确定转审</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalFilter from './ApprovalFilter.vue'
import {
  getApprovalList,
  createApproval,
  approveApproval,
  rejectApproval,
  transferApproval,
  withdrawApproval,
  urgeApproval,
  getApprovalTypes
} from '@/api/approval'
import { getCaseList } from '@/api/case'
import { getUserList } from '@/api/user'
import { getIntakePrefill } from '@/api/caseIntake'

const router = useRouter()
const route = useRoute()

const activeTab = ref('pending')
const loading = ref(false)

const pendingList = ref([])
const processedList = ref([])
const myRequestList = ref([])

const transferDialogVisible = ref(false)
const transferSubmitting = ref(false)
const transferTarget = ref(null)
const transferForm = ref({ newApproverId: null, comments: '' })
const approverOptions = ref([])

const isPendingStatus = (row) =>
  row.status === '审批中' || row.status === 'PENDING' || row.statusDesc === '待审批'

// 审批对话框
const approvalDialogVisible = ref(false)
const approvalForm = ref({
  title: '',
  approvalType: 'SEAL',
  caseId: null,
  urgency: 'NORMAL',
  description: ''
})

// 案件列表
const caseList = ref([])
const approvalTypeOptions = ref([])

const APPROVAL_TYPE_FALLBACK = {
  SEAL: '用印申请',
  REIMBURSEMENT: '费用报销',
  INVOICE: '开票申请',
  LEAVE: '请假出差',
  PURCHASE: '采购申请',
  LICENSE: '证照借用',
  CASE_FILING: '立案申请'
}

const formatApprovalType = (code) => {
  if (!code) return '-'
  const hit = approvalTypeOptions.value.find((t) => t.code === code)
  return hit?.name || APPROVAL_TYPE_FALLBACK[code] || code
}

const parseIntakePendingId = (attachments) => {
  if (!attachments) return null
  try {
    const obj = typeof attachments === 'string' ? JSON.parse(attachments) : attachments
    return obj?.intakePendingId ?? null
  } catch {
    return null
  }
}

const isApproved = (row) =>
  row.status === 'APPROVED' || row.status === '已同意' || row.statusDesc === '已同意'

const navigateToConfirmEstablishment = (caseId) => {
  router.push({ path: `/case/${caseId}/edit`, query: { action: 'confirmEstablishment' } })
}

const promptAfterFilingApproved = async (row) => {
  const pendingId = parseIntakePendingId(row.attachments)
  let caseId = row.caseId

  if (pendingId) {
    try {
      const res = await getIntakePrefill(pendingId)
      if ((res.code === 200 || res.success) && res.data?.draftCaseId) {
        caseId = res.data.draftCaseId
      }
    } catch {
      /* ignore */
    }
  }

  const message = caseId
    ? '立案审批已通过。请核对案件信息后点击「确认建案」，案件将进入审理中并初始化卷宗目录。'
    : '识别信息将自动预填，完善后提交利冲审查并确认建案。'
  const confirmText = caseId ? '去确认建案' : '完善案件'

  try {
    await ElMessageBox.confirm(message, '立案审批已通过', {
      confirmButtonText: confirmText,
      cancelButtonText: '稍后',
      type: 'success'
    })
    if (caseId) {
      navigateToConfirmEstablishment(caseId)
    } else if (pendingId) {
      router.push({ path: '/case/create', query: { intakePendingId: String(pendingId) } })
    }
  } catch {
    /* 用户选择稍后 */
  }
}

const navigateAfterFilingApproval = async (pendingId) => {
  try {
    const res = await getIntakePrefill(pendingId)
    if ((res.code === 200 || res.success) && res.data?.draftCaseId) {
      navigateToConfirmEstablishment(res.data.draftCaseId)
      return
    }
  } catch {
    /* 回退到新建页预填 */
  }
  router.push({ path: '/case/create', query: { intakePendingId: String(pendingId) } })
}

const goCreateCaseFromFiling = async (row) => {
  if (row.caseId) {
    navigateToConfirmEstablishment(row.caseId)
    return
  }
  const pendingId = parseIntakePendingId(row.attachments)
  if (!pendingId) {
    ElMessage.warning('未找到关联案件或卷宗暂存编号')
    return
  }
  await navigateAfterFilingApproval(pendingId)
}

const loadApprovalTypes = async () => {
  try {
    const res = await getApprovalTypes()
    if (res.code === 200 || res.success) {
      approvalTypeOptions.value = res.data || []
    }
  } catch {
    approvalTypeOptions.value = Object.entries(APPROVAL_TYPE_FALLBACK).map(([code, name]) => ({
      code,
      name
    }))
  }
}

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

// 页面加载时获取数据
onMounted(() => {
  loadApprovalTypes()
  fetchApprovalList()

  // 检查是否需要自动创建审批（从案件页面跳转过来）
  if (route.query.action === 'create' && route.query.caseId) {
    // 自动加载案件列表并打开创建审批对话框
    handleCreateApproval().then(() => {
      // 自动填充案件信息
      approvalForm.value.caseId = route.query.caseId
      approvalForm.value.title = `${route.query.caseName || '案件'}相关审批`
      approvalForm.value.description = `关于案件【${route.query.caseName || '未命名案件'}】的审批申请`

      ElMessage.success('已自动关联案件，请补充审批信息')
    })
  } else if (route.query.caseId) {
    // 只筛选案件的审批流程
    ElMessage.info(`正在查看案件的审批流程`)
  }
})

// 监听Tab切换，自动加载对应数据
watch(activeTab, () => {
  fetchApprovalList()
})

const getStatusTagType = (status) => {
  const typeMap = {
    '审批中': 'primary',
    '已通过': 'success',
    '已驳回': 'danger',
    '已撤回': 'info'
  }
  return typeMap[status] || ''
}

const handleCreateApproval = async () => {
  // 加载案件列表
  try {
    const { data } = await getCaseList({ page: 1, size: 100 })
    caseList.value = data.records || []
  } catch (error) {
    console.error('加载案件列表失败:', error)
  }

  approvalForm.value = {
    title: '',
    approvalType: 'SEAL',
    caseId: null,
    urgency: 'NORMAL',
    description: ''
  }
  approvalDialogVisible.value = true
}

const handleSubmitApproval = async () => {
  if (!approvalForm.value.title) {
    ElMessage.warning('请输入审批标题')
    return
  }
  if (!approvalForm.value.description) {
    ElMessage.warning('请输入审批内容')
    return
  }

  try {
    const { title, approvalType, caseId, urgency, description } = approvalForm.value
    await createApproval({
      title,
      approvalType,
      caseId,
      urgency,
      content: description
    })
    ElMessage.success('审批提交成功')
    approvalDialogVisible.value = false
    fetchApprovalList()
  } catch (error) {
    console.error('提交审批失败:', error)
    ElMessage.error('提交失败')
  }
}

const handleApprove = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入审批意见（可选）',
      `同意审批：${row.title}`,
      {
        confirmButtonText: '确定同意',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入审批意见',
        type: 'success'
      }
    )

    const response = await approveApproval(row.id, { comments: value || '' })
    if (response.success) {
      ElMessage.success('审批已同意')
      if (row.approvalType === 'CASE_FILING') {
        await promptAfterFilingApproved(row)
      }
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('同意审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入驳回原因（必填）',
      `驳回审批：${row.title}`,
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入驳回原因',
        inputPattern: /.+/,
        inputErrorMessage: '驳回原因不能为空',
        type: 'warning'
      }
    )

    const response = await rejectApproval(row.id, { comments: value })
    if (response.success) {
      ElMessage.success('审批已驳回')
      // 刷新列表
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('驳回审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const loadApproverOptions = async () => {
  try {
    const res = await getUserList({ page: 1, size: 200 })
    const raw = res.data
    const list = raw?.records || raw?.content || raw?.list || (Array.isArray(raw) ? raw : [])
    approverOptions.value = Array.isArray(list) ? list : []
  } catch {
    approverOptions.value = []
  }
}

const resetTransferForm = () => {
  transferForm.value = { newApproverId: null, comments: '' }
  transferTarget.value = null
}

const handleTransfer = async (row) => {
  transferTarget.value = row
  await loadApproverOptions()
  transferDialogVisible.value = true
}

const submitTransfer = async () => {
  if (!transferForm.value.newApproverId) {
    ElMessage.warning('请选择新审批人')
    return
  }
  if (!transferForm.value.comments?.trim()) {
    ElMessage.warning('请输入转审原因')
    return
  }
  transferSubmitting.value = true
  try {
    const response = await transferApproval(transferTarget.value.id, {
      newApproverId: transferForm.value.newApproverId,
      comments: transferForm.value.comments.trim()
    })
    if (response.success) {
      ElMessage.success('审批已转审')
      transferDialogVisible.value = false
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('转审审批失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    transferSubmitting.value = false
  }
}

const handleUrge = async (row) => {
  try {
    await ElMessageBox.confirm(`确定催办「${row.title}」？系统将通知当前审批人。`, '催办', {
      type: 'warning',
      confirmButtonText: '催办'
    })
    const response = await urgeApproval(row.id)
    if (response.success) {
      ElMessage.success('已发送催办')
    } else {
      ElMessage.error(response.message || '催办失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '催办失败')
    }
  }
}

const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回审批"${row.title}"吗？`,
      '撤回审批',
      {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await withdrawApproval(row.id)
    if (response.success) {
      ElMessage.success('审批已撤回')
      // 刷新列表
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 刷新审批列表
const fetchApprovalList = async () => {
  try {
    loading.value = true
    const res = await getApprovalList({
      status: searchFilters.value.status
        || (activeTab.value === 'pending' ? 'PENDING'
          : activeTab.value === 'processed' ? 'APPROVED' : 'ALL'),
      approvalType: searchFilters.value.approvalType || undefined,
      keyword: searchFilters.value.keyword || undefined,
      caseId: route.query.caseId || null,
      page: 1,
      size: 100
    })

    // 根据当前Tab分配数据
    if (activeTab.value === 'pending') {
      pendingList.value = res.data?.records || []
    } else if (activeTab.value === 'processed') {
      processedList.value = res.data?.records || []
    } else {
      myRequestList.value = res.data?.records || []
    }
  } catch (error) {
    ElMessage.error('获取审批列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const searchFilters = ref({})

const handleSearch = (filters) => {
  searchFilters.value = filters || {}
  fetchApprovalList()
}

const handleReset = () => {
  // 重置筛选条件
  ElMessage.info('筛选条件已重置')
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped lang="scss">
.approval {
  .approval-tabs {
    margin-top: 20px;
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
    border: 1px solid #e6f7ff;

    :deep(.el-tabs__header) {
      margin-bottom: 24px;
      border-bottom: 2px solid #e6f7ff;
    }

    :deep(.el-tabs__item) {
      color: #666;
      font-weight: 500;
      padding: 0 24px;
      height: 40px;
      line-height: 40px;
      border: none;
      transition: all 0.3s;

      &:hover {
        color: #1890ff;
        background: #f0f5ff;
      }

      &.is-active {
        color: #1890ff;
        background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 100%);
        border-bottom: 2px solid #1890ff;
        font-weight: 600;
      }
    }
  }

  .create-btn {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border: none;
    border-radius: 8px;
    padding: 10px 24px;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
  }

  .approval-table {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);

    :deep(.el-table__header-wrapper) {
      th {
        background: #f0f5ff !important;
        color: #333 !important;
        font-weight: 600;
        border-bottom: 2px solid #1890ff;
      }
    }

    :deep(.el-table__body-wrapper) {
      .el-table__row {
        transition: all 0.3s;

        &.even-row {
          background: #ffffff;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        &.odd-row {
          background: #fafcfe;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        td {
          border-bottom: 1px solid #f0f0f0;
        }
      }
    }

    :deep(.el-table__border) {
      border: 1px solid #e6f7ff;
    }

    // 操作按钮优化
    .el-button {
      border-radius: 6px;
      transition: all 0.3s;

      &.el-button--success {
        background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #73d13d 0%, #52c41a 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
        }
      }

      &.el-button--warning {
        background: linear-gradient(135deg, #faad14 0%, #d46b08 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #ffc53d 0%, #faad14 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(250, 173, 20, 0.3);
        }
      }

      &.el-button--primary {
        background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
        }
      }
    }
  }

  .tab-content {
    min-height: 400px;
  }
}
</style>
