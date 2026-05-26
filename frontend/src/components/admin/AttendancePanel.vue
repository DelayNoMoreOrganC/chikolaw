<template>
  <div class="attendance-panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建申请
      </el-button>
      <el-button @click="loadMyRecords">刷新</el-button>
      <el-button v-if="showApproval" type="warning" @click="loadPending">待我审批</el-button>
    </div>

    <el-table v-loading="loading" :data="displayList" border class="admin-table">
      <el-table-column prop="userName" label="申请人" width="100" />
      <el-table-column prop="attendanceType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.attendanceType)">{{ formatType(row.attendanceType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始" width="110" />
      <el-table-column prop="endDate" label="结束" width="110" />
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ row.duration }} {{ row.durationUnit === 'HOUR' ? '小时' : '天' }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" show-overflow-tooltip />
      <el-table-column prop="approvalStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.approvalStatus)">{{ formatStatus(row.approvalStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="showApproval && row.approvalStatus === 'PENDING'"
            link
            type="success"
            size="small"
            @click="handleApprove(row, 'APPROVED')"
          >
            通过
          </el-button>
          <el-button
            v-if="showApproval && row.approvalStatus === 'PENDING'"
            link
            type="danger"
            size="small"
            @click="handleApprove(row, 'REJECTED')"
          >
            驳回
          </el-button>
          <el-button
            v-if="row.approvalStatus === 'PENDING' && isMine(row)"
            link
            type="primary"
            size="small"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑考勤' : '新建考勤申请'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="类型" required>
          <el-select v-model="form.attendanceType" style="width: 100%">
            <el-option label="请假" value="LEAVE" />
            <el-option label="出差" value="BUSINESS_TRIP" />
            <el-option label="加班" value="OVERTIME" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="form.duration" :min="0.5" :step="0.5" />
          <el-select v-model="form.durationUnit" style="width: 90px; margin-left: 8px">
            <el-option label="天" value="DAY" />
            <el-option label="小时" value="HOUR" />
          </el-select>
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMyAttendanceRecords,
  getPendingAttendance,
  createAttendance,
  updateAttendance,
  approveAttendance
} from '@/api/attendance'
import { useUserStore } from '@/stores'

const props = defineProps({
  showApproval: { type: Boolean, default: true }
})

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const mode = ref('my')
const myList = ref([])
const pendingList = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({
  attendanceType: 'LEAVE',
  startDate: '',
  endDate: '',
  duration: 1,
  durationUnit: 'DAY',
  reason: ''
})

const displayList = computed(() => (mode.value === 'pending' ? pendingList.value : myList.value))

const typeTag = (t) => ({ LEAVE: 'warning', BUSINESS_TRIP: 'info', OVERTIME: 'success' }[t] || '')
const statusTag = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || '')
const formatType = (t) => ({ LEAVE: '请假', BUSINESS_TRIP: '出差', OVERTIME: '加班' }[t] || t)
const formatStatus = (s) => ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s)

const isMine = (row) => row.userId === userStore.userId

const loadMyRecords = async () => {
  loading.value = true
  mode.value = 'my'
  try {
    const res = await getMyAttendanceRecords()
    if (res.code === 200 || res.success) {
      myList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载考勤记录失败')
  } finally {
    loading.value = false
  }
}

const loadPending = async () => {
  loading.value = true
  mode.value = 'pending'
  try {
    const res = await getPendingAttendance()
    if (res.code === 200 || res.success) {
      pendingList.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载待审批列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  editingId.value = null
  form.value = {
    attendanceType: 'LEAVE',
    startDate: '',
    endDate: '',
    duration: 1,
    durationUnit: 'DAY',
    reason: ''
  }
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  form.value = {
    attendanceType: row.attendanceType,
    startDate: row.startDate,
    endDate: row.endDate,
    duration: row.duration,
    durationUnit: row.durationUnit || 'DAY',
    reason: row.reason
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.value.startDate) {
    ElMessage.warning('请选择开始日期')
    return
  }
  submitting.value = true
  try {
    const payload = {
      ...form.value,
      userId: userStore.userId
    }
    const res = editingId.value
      ? await updateAttendance(editingId.value, payload)
      : await createAttendance(payload)
    if (res.code === 200 || res.success) {
      ElMessage.success(editingId.value ? '已更新' : '已提交申请')
      dialogVisible.value = false
      await loadMyRecords()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const handleApprove = async (row, status) => {
  try {
    const { value } = await ElMessageBox.prompt(
      status === 'APPROVED' ? '审批意见（可选）' : '驳回原因',
      status === 'APPROVED' ? '通过申请' : '驳回申请',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入' }
    )
    const res = await approveAttendance(row.id, status, value || '')
    if (res.code === 200 || res.success) {
      ElMessage.success('审批完成')
      await loadPending()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审批失败')
  }
}

const viewDetail = (row) => {
  ElMessageBox.alert(
    `类型：${formatType(row.attendanceType)}\n${row.startDate} ~ ${row.endDate || '—'}\n事由：${row.reason || '无'}\n状态：${formatStatus(row.approvalStatus)}`,
    '考勤详情'
  )
}

onMounted(() => loadMyRecords())
</script>

<style scoped>
.attendance-panel .toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
