<template>
  <div class="admin-oa-page">
    <PageHeader title="行政OA管理" />

    <el-tabs v-model="activeTab" type="card" class="admin-oa-tabs"
      :class="'tab-' + activeTab">
      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="announcement">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddAnnouncement" class="add-btn">
              <el-icon><Plus /></el-icon>
              发布公告
            </el-button>
            <el-select v-model="announcementSearchForm.targetScope" placeholder="目标范围" clearable>
              <el-option label="全部" value="" />
              <el-option label="全员" value="ALL" />
              <el-option label="律师" value="LAWYER" />
              <el-option label="行政" value="ADMIN" />
            </el-select>
          </div>

          <el-table :data="announcementList" border class="admin-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="公告标题" width="250" />
            <el-table-column prop="publisherName" label="发布人" width="100" />
            <el-table-column prop="publishDate" label="发布时间" width="160" />
            <el-table-column prop="targetScope" label="目标范围" width="100">
              <template #default="{ row }">
                <el-tag>{{ getTargetScopeLabel(row.targetScope) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.priority > 0" type="danger">重要</el-tag>
                <el-tag v-else type="info">普通</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewAnnouncement(row)">查看</el-button>
                <el-button link type="primary" size="small" @click="handleEditAnnouncement(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteAnnouncement(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 会议室预定 -->
      <el-tab-pane label="会议室预定" name="meeting">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleBookMeeting" class="add-btn">
              <el-icon><Plus /></el-icon>
              预定会议室
            </el-button>
            <el-date-picker
              v-model="meetingDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
            />
          </div>

          <el-table :data="meetingBookingList" border class="admin-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="roomName" label="会议室" width="150" />
            <el-table-column prop="bookingDate" label="预定日期" width="120" />
            <el-table-column prop="startTime" label="开始时间" width="100" />
            <el-table-column prop="endTime" label="结束时间" width="100" />
            <el-table-column prop="bookerName" label="预定人" width="100" />
            <el-table-column prop="meetingTitle" label="会议主题" min-width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getBookingStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewMeeting(row)">查看</el-button>
                <el-button link type="warning" size="small" @click="handleCancelBooking(row)"
                  :disabled="row.status !== '已预定'">
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 综合概览 -->
      <el-tab-pane label="综合概览" name="overview">
        <div class="tab-content">
          <el-row :gutter="20" class="overview-cards">
            <el-col :span="6">
              <div class="stat-card">
                <div class="icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
                  <el-icon><Bell /></el-icon>
                </div>
                <div class="content">
                  <div class="label">本月公告</div>
                  <div class="value">12</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
                  <el-icon><Location /></el-icon>
                </div>
                <div class="content">
                  <div class="label">今日会议</div>
                  <div class="value">5</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
                  <el-icon><Box /></el-icon>
                </div>
                <div class="content">
                  <div class="label">低库存物品</div>
                  <div class="value">8</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
                  <el-icon><Monitor /></el-icon>
                </div>
                <div class="content">
                  <div class="label">待维修资产</div>
                  <div class="value">3</div>
                </div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="12">
              <el-card class="quick-access-card">
                <template #header>
                  <div class="card-header">
                    <span>快捷入口</span>
                  </div>
                </template>
                <div class="quick-access-grid">
                  <div class="quick-access-item" @click="$router.push('/office-supplies')">
                    <el-icon class="icon"><Box /></el-icon>
                    <span>办公用品</span>
                  </div>
                  <div class="quick-access-item" @click="$router.push('/fixed-assets')">
                    <el-icon class="icon"><Monitor /></el-icon>
                    <span>固定资产</span>
                  </div>
                  <div class="quick-access-item" @click="activeTab = 'meeting'">
                    <el-icon class="icon"><Location /></el-icon>
                    <span>会议室预定</span>
                  </div>
                  <div class="quick-access-item" @click="activeTab = 'announcement'">
                    <el-icon class="icon"><Bell /></el-icon>
                    <span>发布公告</span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="notice-card">
                <template #header>
                  <div class="card-header">
                    <span>最新公告</span>
                  </div>
                </template>
                <div class="notice-list">
                  <div class="notice-item">
                    <el-tag type="danger" size="small">重要</el-tag>
                    <span class="title">关于端午节放假安排的通知</span>
                    <span class="date">2025-06-20</span>
                  </div>
                  <div class="notice-item">
                    <el-tag size="small">普通</el-tag>
                    <span class="title">新入职员工欢迎通知</span>
                    <span class="date">2025-06-18</span>
                  </div>
                  <div class="notice-item">
                    <el-tag size="small">普通</el-tag>
                    <span class="title">办公用品申领流程说明</span>
                    <span class="date">2025-06-15</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="announcementDialogVisible" title="发布公告" width="700px">
      <el-form :model="announcementForm" ref="announcementFormRef" label-width="100px" @submit.native.prevent>
        <el-form-item label="公告标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>

        <el-form-item label="目标范围" required>
          <el-select v-model="announcementForm.targetScope" placeholder="请选择目标范围" style="width: 100%">
            <el-option label="全员" value="ALL" />
            <el-option label="律师" value="LAWYER" />
            <el-option label="行政" value="ADMIN" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="announcementForm.priority">
            <el-radio :label="0">普通</el-radio>
            <el-radio :label="1">重要</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="公告内容" required>
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="announcementForm.fileList"
          >
            <el-button>选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAnnouncement">发布</el-button>
      </template>
    </el-dialog>

    <!-- 预定会议室对话框 -->
    <el-dialog v-model="meetingDialogVisible" title="预定会议室" width="600px">
      <el-form :model="meetingForm" ref="meetingFormRef" label-width="100px" @submit.native.prevent>
        <el-form-item label="会议室" required>
          <el-select v-model="meetingForm.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option
              v-for="room in meetingRoomList"
              :key="room.id"
              :label="`${room.roomName} (容量: ${room.capacity}人)`"
              :value="room.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="预定日期" required>
          <el-date-picker
            v-model="meetingForm.bookingDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="meetingForm.startTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="meetingForm.endTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="会议主题" required>
          <el-input v-model="meetingForm.meetingTitle" placeholder="请输入会议主题" />
        </el-form-item>

        <el-form-item label="参会人员">
          <el-input
            v-model="meetingForm.attendees"
            type="textarea"
            :rows="3"
            placeholder="请输入参会人员"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="meetingForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="meetingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitMeeting">预定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Bell, Location, Box, Monitor } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getAnnouncements,
  createAnnouncement
} from '@/api/announcement'
import {
  getMeetingRooms,
  getMeetingBookings,
  createMeetingBooking
} from '@/api/meetingRoom'

const activeTab = ref('overview')
const loading = ref(false)

// 表单ref
const announcementFormRef = ref(null)
const meetingFormRef = ref(null)

// 公告相关
const announcementList = ref([])
const announcementSearchForm = ref({
  targetScope: ''
})
const announcementDialogVisible = ref(false)
const announcementForm = ref({
  title: '',
  targetScope: 'ALL',
  priority: 0,
  content: '',
  fileList: []
})

// 会议室相关
const meetingDate = ref(new Date().toISOString().split('T')[0])
const meetingBookingList = ref([])
const meetingDialogVisible = ref(false)
const meetingRoomList = ref([])
const meetingForm = ref({
  roomId: null,
  bookingDate: new Date().toISOString().split('T')[0],
  startTime: null,
  endTime: null,
  meetingTitle: '',
  attendees: '',
  remark: ''
})

// 获取目标范围标签
const getTargetScopeLabel = (scope) => {
  const scopeMap = {
    'ALL': '全员',
    'LAWYER': '律师',
    'ADMIN': '行政'
  }
  return scopeMap[scope] || scope
}

// 获取预定状态标签颜色
const getBookingStatusTagType = (status) => {
  const typeMap = {
    '已预定': 'success',
    '进行中': 'primary',
    '已取消': 'info',
    '已结束': 'warning'
  }
  return typeMap[status] || ''
}

// 获取类别标签颜色
const getCategoryTagType = (category) => {
  const typeMap = {
    '电子设备': 'primary',
    '办公家具': 'success',
    '车辆': 'warning',
    '其他': 'info'
  }
  return typeMap[category] || ''
}

// 获取状态标签颜色
const getStatusTagType = (status) => {
  const typeMap = {
    '在用': 'success',
    '闲置': 'info',
    '报废': 'danger',
    '维修中': 'warning'
  }
  return typeMap[status] || ''
}

// 加载公告列表
const loadAnnouncements = async () => {
  try {
    loading.value = true
    const res = await getAnnouncements({
      page: 1,
      size: 100,
      ...announcementSearchForm.value
    })
    announcementList.value = res.data?.content || res.data?.records || []
  } catch (error) {
    ElMessage.error('获取公告列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 添加公告
const handleAddAnnouncement = () => {
  announcementForm.value = {
    id: null,
    title: '',
    targetScope: 'ALL',
    priority: 0,
    content: '',
    fileList: []
  }
  announcementDialogVisible.value = true
}

// 查看公告
const handleViewAnnouncement = (row) => {
  ElMessage.info(`查看公告：${row.title}`)
  // TODO: 显示公告详情对话框
}

// 编辑公告
const handleEditAnnouncement = (row) => {
  Object.assign(announcementForm.value, row)
  announcementDialogVisible.value = true
}

// 删除公告
const handleDeleteAnnouncement = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除公告"${row.title}"吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    // TODO: 调用删除API
    ElMessage.success('删除成功')
    loadAnnouncements()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 查看会议预定
const handleViewMeeting = (row) => {
  ElMessage.info(`查看会议预定：${row.meetingTitle}`)
  // TODO: 显示会议详情对话框
}

// 取消会议预定
const handleCancelBooking = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要取消会议预定"${row.meetingTitle}"吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    // TODO: 调用取消API
    ElMessage.success('取消成功')
    loadMeetingBookings()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 提交公告
const handleSubmitAnnouncement = async () => {
  if (!announcementForm.value.title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!announcementForm.value.content) {
    ElMessage.warning('请输入公告内容')
    return
  }

  try {
    await createAnnouncement(announcementForm.value)
    ElMessage.success('公告发布成功')
    announcementDialogVisible.value = false
    loadAnnouncements()
  } catch (error) {
    console.error('发布公告失败:', error)
    ElMessage.error('发布公告失败')
  }
}

// 文件变化
const handleFileChange = (file, fileList) => {
  announcementForm.value.fileList = fileList
}

// 加载会议室列表
const loadMeetingRooms = async () => {
  try {
    const res = await getMeetingRooms({ onlyEnabled: true })
    meetingRoomList.value = res.data || []
  } catch (error) {
    console.error('加载会议室列表失败:', error)
  }
}

// 加载会议室预定列表
const loadMeetingBookings = async () => {
  try {
    loading.value = true
    const res = await getMeetingBookings({
      bookingDate: meetingDate.value,
      page: 1,
      size: 100
    })
    meetingBookingList.value = res.data?.content || res.data?.records || []
  } catch (error) {
    ElMessage.error('获取会议预定列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 预定会议室
const handleBookMeeting = async () => {
  await loadMeetingRooms()

  meetingForm.value = {
    roomId: null,
    bookingDate: new Date().toISOString().split('T')[0],
    startTime: null,
    endTime: null,
    meetingTitle: '',
    attendees: '',
    remark: ''
  }
  meetingDialogVisible.value = true
}

// 提交会议预定
const handleSubmitMeeting = async () => {
  if (!meetingForm.value.roomId) {
    ElMessage.warning('请选择会议室')
    return
  }
  if (!meetingForm.value.bookingDate) {
    ElMessage.warning('请选择预定日期')
    return
  }
  if (!meetingForm.value.startTime || !meetingForm.value.endTime) {
    ElMessage.warning('请选择开始和结束时间')
    return
  }
  if (!meetingForm.value.meetingTitle) {
    ElMessage.warning('请输入会议主题')
    return
  }

  try {
    await createMeetingBooking(meetingForm.value)
    ElMessage.success('会议室预定成功')
    meetingDialogVisible.value = false
    loadMeetingBookings()
  } catch (error) {
    console.error('预定会议室失败:', error)
    ElMessage.error('预定会议室失败')
  }
}

onMounted(() => {
  loadAnnouncements()
  loadMeetingBookings()
  loadMeetingRooms()
})

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped lang="scss">
.admin-oa-page {
  .admin-oa-tabs {
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

  .add-btn {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border: none;
    border-radius: 8px;
    padding: 10px 20px;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
  }

  .admin-table {
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
  }

  .tab-content {
    .toolbar {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      align-items: center;
    }

    .overview-cards {
      .stat-card {
        background: #fff;
        border-radius: 12px;
        padding: 24px;
        display: flex;
        align-items: center;
        gap: 20px;
        box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
        border: 1px solid #e6f7ff;
        transition: all 0.3s;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(24, 144, 255, 0.15);
        }

        .icon {
          width: 60px;
          height: 60px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          font-size: 28px;
        }

        .content {
          flex: 1;

          .label {
            font-size: 14px;
            color: #666;
            margin-bottom: 8px;
          }

          .value {
            font-size: 28px;
            font-weight: bold;
            color: #1890ff;
          }
        }
      }
    }

    .quick-access-card,
    .notice-card {
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
      border: 1px solid #e6f7ff;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: 600;
        color: #333;
      }

      .quick-access-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;

        .quick-access-item {
          background: linear-gradient(135deg, #f0f5ff 0%, #ffffff 100%);
          border: 1px solid #e6f7ff;
          border-radius: 12px;
          padding: 20px;
          text-align: center;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
          }

          .icon {
            font-size: 32px;
            color: #1890ff;
            margin-bottom: 8px;
          }

          span {
            display: block;
            font-size: 14px;
            color: #333;
            font-weight: 500;
          }
        }
      }

      .notice-list {
        .notice-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 0;
          border-bottom: 1px solid #f0f0f0;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            background: #f0f5ff;
            border-radius: 8px;
            padding-left: 12px;
            padding-right: 12px;
          }

          &:last-child {
            border-bottom: none;
          }

          .title {
            flex: 1;
            font-size: 14px;
            color: #333;
          }

          .date {
            font-size: 12px;
            color: #999;
          }
        }
      }
    }
  }
}
</style>
