<template>
  <div class="work-report">
    <PageHeader title="工作汇报" />

    <div class="work-report-container">
      <!-- 左侧操作区 -->
      <div class="sidebar">
        <el-menu
          :default-active="activeType"
          @select="handleTypeChange"
          class="type-menu"
        >
          <el-menu-item index="">
            <el-icon><Document /></el-icon>
            <span>全部汇报</span>
          </el-menu-item>
          <el-menu-item index="DRAFT">
            <el-icon><Edit /></el-icon>
            <span>草稿箱</span>
          </el-menu-item>
          <el-menu-item index="SUBMITTED">
            <el-icon><Promotion /></el-icon>
            <span>待审核</span>
          </el-menu-item>
          <el-menu-item index="APPROVED">
            <el-icon><CircleCheck /></el-icon>
            <span>已通过</span>
          </el-menu-item>
        </el-menu>

        <el-divider />

        <div class="quick-actions">
          <h4>快捷操作</h4>
          <el-button type="primary" @click="handleCreate" style="width: 100%">
            <el-icon><Plus /></el-icon>
            新建汇报
          </el-button>
          <el-button @click="loadMyReports" style="width: 100%; margin-top: 10px;">
            <el-icon><User /></el-icon>
            我的汇报
          </el-button>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="main-content">
        <!-- 汇报列表 -->
        <div class="report-list">
          <div class="list-header">
            <h3>{{ currentTitle }}</h3>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新建汇报
            </el-button>
          </div>

          <el-empty v-if="reports.length === 0" description="暂无汇报" />

          <div v-else class="report-items">
            <div
              v-for="report in reports"
              :key="report.id"
              class="report-item"
              @click="handleView(report)"
            >
              <div class="item-header">
                <h4 class="title">{{ report.title }}</h4>
                <el-tag :type="getStatusTagType(report.status)" size="small">
                  {{ formatStatus(report.status) }}
                </el-tag>
              </div>

              <div class="item-meta">
                <span class="type">
                  <el-icon><DocumentCopy /></el-icon>
                  {{ formatType(report.reportType) }}
                </span>
                <span class="reporter">
                  <el-icon><User /></el-icon>
                  {{ report.reporterName }}
                </span>
                <span class="department" v-if="report.department">
                  <el-icon><OfficeBuilding /></el-icon>
                  {{ report.department }}
                </span>
                <span class="date">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(report.reportDate) }}
                </span>
              </div>

              <div class="item-summary" v-if="report.workSummary">
                {{ report.workSummary }}
              </div>

              <div class="item-footer">
                <el-button
                  v-if="report.status === 'DRAFT'"
                  type="primary"
                  size="small"
                  @click.stop="handleEdit(report)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="report.status === 'DRAFT'"
                  type="success"
                  size="small"
                  @click.stop="handleSubmit(report)"
                >
                  提交
                </el-button>
                <el-button
                  v-if="report.status === 'SUBMITTED'"
                  type="warning"
                  size="small"
                  @click.stop="handleReview(report)"
                >
                  审核
                </el-button>
                <el-button
                  v-if="report.status === 'DRAFT'"
                  type="danger"
                  size="small"
                  @click.stop="handleDelete(report)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑汇报' : '新建汇报'"
      width="800px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="汇报标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入汇报标题" />
        </el-form-item>

        <el-form-item label="汇报类型" prop="reportType">
          <el-select v-model="formData.reportType" placeholder="请选择汇报类型">
            <el-option label="日报" value="DAILY" />
            <el-option label="周报" value="WEEKLY" />
            <el-option label="月报" value="MONTHLY" />
            <el-option label="项目汇报" value="PROJECT" />
          </el-select>
        </el-form-item>

        <el-form-item label="汇报日期" prop="reportDate">
          <el-date-picker
            v-model="formData.reportDate"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="所属部门">
          <el-input v-model="formData.department" placeholder="请输入部门" />
        </el-form-item>

        <el-form-item label="工作总结">
          <el-input
            v-model="formData.workSummary"
            type="textarea"
            :rows="3"
            placeholder="本周/本月完成的主要工作..."
          />
        </el-form-item>

        <el-form-item label="下步计划">
          <el-input
            v-model="formData.nextPlan"
            type="textarea"
            :rows="3"
            placeholder="下周/下月的工作计划..."
          />
        </el-form-item>

        <el-form-item label="遇到问题">
          <el-input
            v-model="formData.problems"
            type="textarea"
            :rows="3"
            placeholder="工作中遇到的问题和困难..."
          />
        </el-form-item>

        <el-form-item label="建议意见">
          <el-input
            v-model="formData.suggestions"
            type="textarea"
            :rows="3"
            placeholder="对团队或公司的建议..."
          />
        </el-form-item>

        <el-form-item label="详细内容">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="5"
            placeholder="详细汇报内容..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="审核汇报"
      width="600px"
    >
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.status">
            <el-radio label="APPROVED">通过</el-radio>
            <el-radio label="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="审核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReviewSubmit" :loading="reviewing">
          提交审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Edit, Promotion, CircleCheck, Plus, User,
  OfficeBuilding, Calendar, DocumentCopy
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  listWorkReports,
  getMyWorkReports,
  createWorkReport,
  updateWorkReport,
  submitWorkReport,
  reviewWorkReport,
  deleteWorkReport
} from '@/api/workReport'
import { pageRecords, pageTotal } from '@/utils/page'

const router = useRouter()

const activeType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const reports = ref([])

const dialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const reviewing = ref(false)
const currentReportId = ref(null)

const formData = ref({
  title: '',
  reportType: 'WEEKLY',
  reportDate: new Date(),
  department: '',
  workSummary: '',
  nextPlan: '',
  problems: '',
  suggestions: '',
  content: '',
  status: 'DRAFT'
})

const reviewForm = ref({
  status: 'APPROVED',
  comment: ''
})

const rules = {
  title: [{ required: true, message: '请输入汇报标题', trigger: 'blur' }],
  reportType: [{ required: true, message: '请选择汇报类型', trigger: 'change' }],
  reportDate: [{ required: true, message: '请选择汇报日期', trigger: 'change' }]
}

const currentTitle = computed(() => {
  const typeMap = {
    '': '全部汇报',
    'DRAFT': '草稿箱',
    'SUBMITTED': '待审核',
    'APPROVED': '已通过'
  }
  return typeMap[activeType.value] || '全部汇报'
})

// 加载汇报列表
const loadReports = async () => {
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (activeType.value) {
      params.status = activeType.value
    }
    const res = await listWorkReports(params)
    reports.value = pageRecords(res)
    total.value = pageTotal(res)
  } catch (error) {
    console.error('加载汇报列表失败:', error)
    ElMessage.error('加载失败')
  }
}

// 加载我的汇报
const loadMyReports = async () => {
  try {
    activeType.value = ''
    currentPage.value = 1

    const res = await getMyWorkReports({ page: 0, size: 100 })
    reports.value = pageRecords(res)
    total.value = pageTotal(res)
  } catch (error) {
    console.error('加载我的汇报失败:', error)
    ElMessage.error('加载失败')
  }
}

// 类型切换
const handleTypeChange = (type) => {
  activeType.value = type
  currentPage.value = 1
  loadReports()
}

// 分页
const handlePageChange = () => {
  loadReports()
}

// 查看汇报
const handleView = (report) => {
  router.push(`/work-reports/${report.id}`)
}

// 新建汇报
const handleCreate = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑汇报
const handleEdit = (report) => {
  isEdit.value = true
  currentReportId.value = report.id
  Object.assign(formData.value, report)
  dialogVisible.value = true
}

// 保存汇报
const handleSave = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await updateWorkReport(currentReportId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createWorkReport(formData.value)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadReports()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 提交汇报
const handleSubmit = async (report) => {
  try {
    await ElMessageBox.confirm('确认提交此汇报吗？提交后将无法修改。', '提示')

    await submitWorkReport(report.id)

    ElMessage.success('提交成功')
    loadReports()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  }
}

// 删除汇报
const handleDelete = async (report) => {
  try {
    await ElMessageBox.confirm('确认删除此汇报吗？此操作不可恢复。', '警告', {
      type: 'warning'
    })

    await deleteWorkReport(report.id)

    ElMessage.success('删除成功')
    loadReports()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 审核汇报
const handleReview = (report) => {
  currentReportId.value = report.id
  reviewForm.value = {
    status: 'APPROVED',
    comment: ''
  }
  reviewDialogVisible.value = true
}

// 提交审核
const handleReviewSubmit = async () => {
  reviewing.value = true
  try {
    await reviewWorkReport(
      currentReportId.value,
      reviewForm.value.status,
      reviewForm.value.comment
    )

    ElMessage.success('审核完成')
    reviewDialogVisible.value = false
    loadReports()
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  } finally {
    reviewing.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    title: '',
    reportType: 'WEEKLY',
    reportDate: new Date(),
    department: '',
    workSummary: '',
    nextPlan: '',
    problems: '',
    suggestions: '',
    content: '',
    status: 'DRAFT'
  }
}

// 格式化类型
const formatType = (type) => {
  const map = {
    'DAILY': '日报',
    'WEEKLY': '周报',
    'MONTHLY': '月报',
    'PROJECT': '项目汇报'
  }
  return map[type] || type
}

// 格式化状态
const formatStatus = (status) => {
  const map = {
    'DRAFT': '草稿',
    'SUBMITTED': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'REVIEWED': '已审核'
  }
  return map[status] || status
}

// 状态标签颜色
const getStatusTagType = (status) => {
  const map = {
    'DRAFT': 'info',
    'SUBMITTED': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped lang="scss">
.work-report {
  .work-report-container {
    display: flex;
    gap: 20px;
    margin-top: 20px;
  }

  .sidebar {
    width: 200px;
    flex-shrink: 0;

    .type-menu {
      border-right: none;
    }

    .quick-actions {
      padding: 10px 0;

      h4 {
        margin: 0 0 10px;
        font-size: 14px;
        color: #666;
      }

      .el-button {
        width: 100%;
      }
    }
  }

  .main-content {
    flex: 1;
    background: #fff;
    padding: 20px;
    border-radius: 4px;

    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h3 {
        margin: 0;
        color: #333;
      }
    }

    .report-items {
      .report-item {
        padding: 20px;
        border-bottom: 1px solid #e4e7ed;
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: #f9f9f9;
        }

        &:last-child {
          border-bottom: none;
        }

        .item-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .title {
            margin: 0;
            font-size: 16px;
            color: #303133;
            flex: 1;
          }
        }

        .item-meta {
          display: flex;
          gap: 20px;
          font-size: 13px;
          color: #909399;
          margin-bottom: 10px;

          span {
            display: flex;
            align-items: center;
            gap: 4px;
          }
        }

        .item-summary {
          margin: 0 0 15px;
          color: #606266;
          line-height: 1.6;
        }

        .item-footer {
          display: flex;
          gap: 10px;
        }
      }
    }

    .el-pagination {
      margin-top: 20px;
      justify-content: center;
    }
  }
}
</style>
