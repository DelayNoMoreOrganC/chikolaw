<template>
  <div class="case-record">
    <div class="record-toolbar">
      <div class="toolbar-left">
        <el-select v-model="filterStage" placeholder="选择阶段" clearable style="width: 150px">
          <el-option label="全部阶段" value="" />
          <el-option
            v-for="stage in caseStages"
            :key="stage"
            :label="stage"
            :value="stage"
          />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />

        <el-input
          v-model="keyword"
          placeholder="搜索记录内容"
          clearable
          style="width: 250px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <div class="toolbar-right">
        <el-dropdown @command="handleExportCommand">
          <el-button>
            <el-icon><Download /></el-icon>
            导出
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="excel">
                <el-icon><Document /></el-icon>
                导出为Excel
              </el-dropdown-item>
              <el-dropdown-item command="word">
                <el-icon><Document /></el-icon>
                导出为Word
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" @click="handleAddRecord">
          <el-icon><Plus /></el-icon>
          添加记录
        </el-button>
      </div>
    </div>

    <div class="record-stats">
      <div class="stat-item">
        <span class="stat-label">进度统计：</span>
        <span class="stat-value">{{ completedCount }}/{{ totalCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">总工时：</span>
        <span class="stat-value">{{ totalHours }}h</span>
      </div>
    </div>

    <div class="record-timeline">
      <el-timeline>
        <el-timeline-item
          v-for="record in recordList"
          :key="record.id"
          :timestamp="record.recordDate"
          placement="top"
          :color="getRecordColor(record.stage)"
        >
          <el-card class="record-card" shadow="hover">
            <div class="record-header">
              <div class="record-title-row">
                <h4 class="record-title">{{ record.title }}</h4>
                <div class="record-meta">
                  <el-tag size="small" :type="getStageTagType(record.stage)">
                    {{ record.stage }}
                  </el-tag>
                  <span class="record-hours">{{ record.hours }}h</span>
                </div>
              </div>
              <div class="record-actions">
                <el-button text type="primary" size="small" @click="handleEditRecord(record)">
                  编辑
                </el-button>
                <el-button text type="danger" size="small" @click="handleDeleteRecord(record)">
                  删除
                </el-button>
              </div>
            </div>

            <div class="record-content">
              {{ record.content }}
            </div>

            <div v-if="record.attachments && record.attachments.length > 0" class="record-attachments">
              <div class="attachment-label">
                <el-icon><Paperclip /></el-icon>
                附件：
              </div>
              <div class="attachment-list">
                <el-tag
                  v-for="file in record.attachments"
                  :key="file.id"
                  closable
                  @close="handleDeleteAttachment(record, file)"
                >
                  {{ file.name }}
                </el-tag>
              </div>
            </div>

            <div class="record-footer">
              <span class="record-author">
                <el-icon><User /></el-icon>
                {{ record.authorName }}
              </span>
              <span class="record-time">{{ record.createTime }}</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div v-if="recordList.length === 0" class="empty-state">
      <el-empty description="暂无办案记录" />
    </div>

    <!-- 添加/编辑记录对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑记录' : '添加记录'"
      width="800px"
    >
      <el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="100px">
        <el-form-item label="记录标题" prop="title">
          <el-input v-model="recordForm.title" placeholder="请输入记录标题" />
        </el-form-item>

        <el-form-item label="案件阶段" prop="stage">
          <el-select v-model="recordForm.stage" placeholder="请选择阶段">
            <el-option
              v-for="stage in caseStages"
              :key="stage"
              :label="stage"
              :value="stage"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="记录内容" prop="content">
          <el-input
            v-model="recordForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入记录内容"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工时" prop="hours">
              <el-input-number
                v-model="recordForm.hours"
                :min="0"
                :max="24"
                :step="0.5"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="记录日期" prop="recordDate">
              <el-date-picker
                v-model="recordForm.recordDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="附件">
          <el-upload
            v-model:file-list="recordForm.attachments"
            action="#"
            :auto-upload="false"
            multiple
          >
            <el-button>
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRecord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Download, Plus, Paperclip, User, Upload, ArrowDown, Document
} from '@element-plus/icons-vue'
import {
  getCaseRecords,
  createCaseRecord,
  updateCaseRecord,
  deleteCaseRecord,
  exportCaseRecords,
  exportCaseRecordsWord
} from '@/api/case'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

// 筛选条件
const filterStage = ref('')
const dateRange = ref([])
const keyword = ref('')

// 数据
const recordList = ref([])
const loading = ref(false)

// 统计
const completedCount = ref(0)
const totalCount = ref(0)
const totalHours = ref(0)

// 案件阶段
const caseStages = [
  '咨询', '签约', '起草文书', '待立案', '已立案',
  '一审审理中', '一审结案', '二审', '执行', '结案归档'
]

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const recordFormRef = ref(null)

// 表单数据
const recordForm = reactive({
  id: null,
  title: '',
  stage: '',
  content: '',
  hours: null,
  recordDate: '',
  attachments: []
})

// 表单验证规则
const recordRules = {
  title: [{ required: true, message: '请输入记录标题', trigger: 'blur' }],
  stage: [{ required: true, message: '请选择案件阶段', trigger: 'change' }],
  content: [{ required: true, message: '请输入记录内容', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }]
}

// 获取记录颜色
const getRecordColor = (stage) => {
  const colorMap = {
    '咨询': '#909399',
    '签约': '#409eff',
    '起草文书': '#67c23a',
    '待立案': '#e6a23c',
    '已立案': '#f56c6c',
    '一审审理中': '#ff6600',
    '一审结案': '#303133',
    '二审': '#8c44ad',
    '执行': '#1890ff',
    '结案归档': '#52c41a'
  }
  return colorMap[stage] || '#409eff'
}

// 获取阶段标签颜色
const getStageTagType = (stage) => {
  const typeMap = {
    '咨询': 'info',
    '签约': 'primary',
    '起草文书': 'success',
    '待立案': 'warning',
    '已立案': 'danger',
    '一审审理中': 'danger',
    '一审结案': '',
    '二审': 'warning',
    '执行': 'primary',
    '结案归档': 'success'
  }
  return typeMap[stage] || ''
}

// 获取记录列表
const fetchRecords = async () => {
  try {
    loading.value = true
    const { id } = props.caseData
    const params = {
      stage: filterStage.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      keyword: keyword.value
    }
    const res = await getCaseRecords(id, params)
    recordList.value = res.data || []

    // 计算统计
    completedCount.value = recordList.value.filter(r => r.completed).length
    totalCount.value = recordList.value.length
    totalHours.value = recordList.value.reduce((sum, r) => sum + (r.hours || 0), 0)
  } catch (error) {
    ElMessage.error('获取办案记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  fetchRecords()
}

// 重置
const handleReset = () => {
  filterStage.value = ''
  dateRange.value = []
  keyword.value = ''
  fetchRecords()
}

// 导出办案记录
const handleExportCommand = async (command) => {
  try {
    const { id } = props.caseData
    let response
    let fileName
    let fileType

    if (command === 'word') {
      response = await exportCaseRecordsWord(id, {
        stage: filterStage.value,
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1],
        keyword: keyword.value
      })
      fileName = `办案记录_${id}_${new Date().getTime()}.docx`
      fileType = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    } else {
      response = await exportCaseRecords(id, {
        stage: filterStage.value,
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1],
        keyword: keyword.value
      })
      fileName = `办案记录_${id}_${new Date().getTime()}.xlsx`
      fileType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response], { type: fileType }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 添加记录
const handleAddRecord = () => {
  isEdit.value = false
  Object.assign(recordForm, {
    id: null,
    title: '',
    stage: props.caseData.currentStage || '',
    content: '',
    hours: null,
    recordDate: new Date().toISOString().split('T')[0],
    attachments: []
  })
  dialogVisible.value = true
}

// 编辑记录
const handleEditRecord = (record) => {
  isEdit.value = true
  Object.assign(recordForm, {
    id: record.id,
    title: record.title,
    stage: record.stage,
    content: record.content,
    hours: record.hours,
    recordDate: record.recordDate,
    attachments: record.attachments || []
  })
  dialogVisible.value = true
}

// 删除记录
const handleDeleteRecord = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const { id } = props.caseData
    await deleteCaseRecord(id, record.id)
    ElMessage.success('删除成功')
    fetchRecords()
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 删除附件
const handleDeleteAttachment = async (record, file) => {
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

    // 从记录中移除附件
    const index = record.attachments?.indexOf(file)
    if (index > -1) {
      record.attachments.splice(index, 1)
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

// 提交记录
const handleSubmitRecord = async () => {
  try {
    await recordFormRef.value?.validate()

    const { id } = props.caseData
    const data = { ...recordForm }

    if (isEdit.value) {
      await updateCaseRecord(id, recordForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await createCaseRecord(id, data)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    fetchRecords()
    emit('refresh')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

// 监听案件数据变化，加载记录
watch(() => props.caseData.id, (newId) => {
  if (newId) {
    fetchRecords()
  }
}, { immediate: true })
</script>

<style scoped lang="scss">
.case-record {
  padding: 30px;

  .record-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 20px;
    background-color: #f5f7fa;
    border-radius: 4px;

    .toolbar-left,
    .toolbar-right {
      display: flex;
      gap: 10px;
      align-items: center;
    }
  }

  .record-stats {
    display: flex;
    gap: 30px;
    margin-bottom: 20px;
    padding: 15px 20px;
    background-color: #ecf5ff;
    border-radius: 4px;
    border-left: 4px solid #1890ff;

    .stat-item {
      display: flex;
      gap: 8px;

      .stat-label {
        color: #606266;
        font-size: 14px;
      }

      .stat-value {
        color: #1890ff;
        font-size: 16px;
        font-weight: bold;
      }
    }
  }

  .record-timeline {
    :deep(.el-timeline-item__timestamp) {
      font-weight: 500;
      color: #333;
    }

    .record-card {
      .record-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;

        .record-title-row {
          flex: 1;

          .record-title {
            margin: 0 0 8px;
            font-size: 16px;
            font-weight: 500;
            color: #333;
          }

          .record-meta {
            display: flex;
            gap: 12px;
            align-items: center;

            .record-hours {
              font-size: 14px;
              color: #1890ff;
              font-weight: 500;
            }
          }
        }

        .record-actions {
          display: flex;
          gap: 8px;
        }
      }

      .record-content {
        margin-bottom: 12px;
        color: #606266;
        line-height: 1.6;
        white-space: pre-wrap;
      }

      .record-attachments {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px;
        background-color: #f5f7fa;
        border-radius: 4px;
        margin-bottom: 12px;
        flex-wrap: wrap;

        .attachment-label {
          font-size: 13px;
          color: #909399;
          display: flex;
          align-items: center;
          gap: 4px;
        }

        .attachment-list {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
        }
      }

      .record-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 12px;
        border-top: 1px dashed #e4e7ed;
        font-size: 13px;
        color: #909399;

        .record-author {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }

  .empty-state {
    padding: 60px 0;
  }
}
</style>
