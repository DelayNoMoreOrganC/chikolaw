<template>
  <div class="document-flow-page">
    <PageHeader title="公文流转" />

    <div class="flow-container">
      <el-row :gutter="20">
        <el-col :span="4">
          <el-card class="menu-card">
            <el-menu :default-active="activeType" @select="handleTypeChange">
              <el-menu-item index="">全部公文</el-menu-item>
              <el-menu-item index="PENDING">待审批</el-menu-item>
              <el-menu-item index="APPROVED">已通过</el-menu-item>
              <el-menu-item index="REJECTED">已驳回</el-menu-item>
            </el-menu>
          </el-card>
        </el-col>

        <el-col :span="20">
          <el-card v-loading="loading">
            <template #header>
              <div style="display: flex; justify-content: space-between">
                <span>{{ currentTitle }}</span>
                <el-button type="primary" @click="dialogVisible = true">新建公文</el-button>
              </div>
            </template>

            <el-empty v-if="documents.length === 0" description="暂无公文" />

            <div v-else>
              <div v-for="doc in documents" :key="doc.id" class="doc-item">
                <div style="display: flex; justify-content: space-between">
                  <h4>{{ doc.title }}</h4>
                  <el-tag :type="getStatusType(doc.status)" size="small">
                    {{ doc.statusDesc || formatStatus(doc.status) }}
                  </el-tag>
                </div>
                <div style="margin: 10px 0; color: #909399; font-size: 13px">
                  <span>类型：公文流转</span>
                  <span style="margin-left: 20px">申请人：{{ doc.applicantName }}</span>
                  <span style="margin-left: 20px">时间：{{ formatDate(doc.applyTime) }}</span>
                </div>
                <div style="margin-top: 10px">
                  <el-button
                    v-if="doc.status === 'PENDING'"
                    size="small"
                    type="primary"
                    @click="handleUrge(doc)"
                  >
                    催办
                  </el-button>
                  <el-button size="small" @click="handleView(doc)">查看详情</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="dialogVisible" title="新建公文" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="公文标题" prop="title">
          <el-input v-model="formData.title" />
        </el-form-item>
        <el-form-item label="公文类型">
          <el-select v-model="formData.docType" style="width: 100%">
            <el-option label="通知" value="通知" />
            <el-option label="公告" value="公告" />
            <el-option label="请示" value="请示" />
            <el-option label="报告" value="报告" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="formData.urgency">
            <el-radio label="普通">普通</el-radio>
            <el-radio label="紧急">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主送机关">
          <el-input v-model="formData.mainReceiver" />
        </el-form-item>
        <el-form-item label="公文内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">提交审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="公文详情" width="700px">
      <div v-if="selectedDoc">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ selectedDoc.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedDoc.status)">
              {{ selectedDoc.statusDesc || formatStatus(selectedDoc.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ selectedDoc.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div style="white-space: pre-wrap">{{ selectedDoc.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="flowSteps.length" style="margin-top: 16px">
          <h4>审批流转</h4>
          <el-timeline>
            <el-timeline-item
              v-for="step in flowSteps"
              :key="step.id"
              :timestamp="formatDate(step.actionTime)"
            >
              {{ step.action }} — {{ step.comments || '无备注' }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getApprovalList, createApproval, getApprovalDetail, urgeApproval } from '@/api/approval'
import request from '@/utils/request'
import { pageRecords } from '@/utils/page'

const OFFICIAL_DOC = 'OFFICIAL_DOC'

const activeType = ref('')
const documents = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedDoc = ref(null)
const flowSteps = ref([])
const saving = ref(false)
const formRef = ref(null)

const formData = ref({
  title: '',
  docType: '通知',
  urgency: '普通',
  mainReceiver: '',
  content: ''
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const currentTitle = computed(() => {
  const map = { '': '全部公文', PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[activeType.value] || '全部公文'
})

const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getApprovalList({
      page: 1,
      size: 50,
      approvalType: OFFICIAL_DOC,
      status: activeType.value || undefined
    })
    documents.value = pageRecords(res)
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type) => {
  activeType.value = type
  loadDocuments()
}

const resetForm = () => {
  formData.value = { title: '', docType: '通知', urgency: '普通', mainReceiver: '', content: '' }
}

const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const body = [
      `公文类型：${formData.value.docType}`,
      `紧急程度：${formData.value.urgency}`,
      `主送机关：${formData.value.mainReceiver || '无'}`,
      '',
      formData.value.content
    ].join('\n')
    await createApproval({
      approvalType: OFFICIAL_DOC,
      title: formData.value.title,
      content: body
    })
    ElMessage.success('公文已提交审批')
    dialogVisible.value = false
    loadDocuments()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    saving.value = false
  }
}

const handleUrge = async (doc) => {
  try {
    await urgeApproval(doc.id)
    ElMessage.success('已催办')
  } catch (e) {
    ElMessage.error(e.message || '催办失败')
  }
}

const handleView = async (doc) => {
  try {
    const res = await getApprovalDetail(doc.id)
    selectedDoc.value = res.data
    const flowRes = await request({ url: `/approval/${doc.id}/flow`, method: 'get' })
    flowSteps.value = flowRes.data || []
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

const formatStatus = (status) => {
  const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', TRANSFERRED: '已转审' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(loadDocuments)
</script>

<style scoped lang="scss">
.doc-item {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}
.flow-container {
  padding: 20px;
}
</style>
