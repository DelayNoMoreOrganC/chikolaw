<template>
  <div class="case-timeline">
    <!-- 分类Tab -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="动态" name="activity" />
      <el-tab-pane label="评论" name="comment" />
    </el-tabs>

    <!-- 评论输入区 -->
    <div class="comment-input-section">
      <div class="input-header">
        <h4>添加评论</h4>
        <el-button text type="primary" @click="handleToggleMention">
          <el-icon><Promotion /></el-icon>
          @提及成员
        </el-button>
      </div>

      <div v-if="showMention" class="mention-panel">
        <el-checkbox-group v-model="mentionedUsers">
          <el-checkbox
            v-for="user in teamMembers"
            :key="user.id"
            :label="user.id"
          >
            {{ user.name }}
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <el-input
        v-model="newComment"
        type="textarea"
        :rows="3"
        placeholder="输入评论内容，支持@提及成员"
        @keyup.ctrl.enter="handleSubmitComment"
      />

      <div class="input-footer">
        <span class="tip">Ctrl + Enter 快速发送</span>
        <el-button type="primary" @click="handleSubmitComment">
          发送评论
        </el-button>
      </div>
    </div>

    <!-- 时间线 -->
    <div class="timeline-section">
      <el-timeline v-loading="loading">
        <el-timeline-item
          v-for="item in filteredTimeline"
          :key="item.id"
          :timestamp="item.timestamp"
          placement="top"
          :color="getItemColor(item.type)"
          :size="item.type === 'comment' ? 'large' : 'normal'"
        >
          <div class="timeline-card" :class="`type-${item.type}`">
            <!-- 动态类型 -->
            <div v-if="item.type === 'activity'" class="activity-item">
              <div class="activity-header">
                <div class="activity-user">
                  <el-avatar :size="32" :src="item.userAvatar">
                    {{ item.userName?.charAt(0) }}
                  </el-avatar>
                  <div class="user-info">
                    <div class="user-name">{{ item.userName }}</div>
                    <div class="action-text">{{ item.action }}</div>
                  </div>
                </div>
                <div class="activity-time">{{ item.timestamp }}</div>
              </div>

              <div v-if="item.details" class="activity-details">
                <div v-for="(detail, index) in item.details" :key="index" class="detail-item">
                  <span class="detail-label">{{ detail.label }}：</span>
                  <span class="detail-value">{{ detail.value }}</span>
                </div>
              </div>
            </div>

            <!-- 评论类型 -->
            <div v-if="item.type === 'comment'" class="comment-item">
              <div class="comment-header">
                <div class="comment-user">
                  <el-avatar :size="36" :src="item.userAvatar">
                    {{ item.userName?.charAt(0) }}
                  </el-avatar>
                  <div class="user-info">
                    <div class="user-name">{{ item.userName }}</div>
                    <div class="comment-time">{{ item.timestamp }}</div>
                  </div>
                </div>
                <div class="comment-actions">
                  <el-button
                    v-if="canEdit(item)"
                    text
                    type="primary"
                    size="small"
                    @click="handleEditComment(item)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="canDelete(item)"
                    text
                    type="danger"
                    size="small"
                    @click="handleDeleteComment(item)"
                  >
                    删除
                  </el-button>
                </div>
              </div>

              <div class="comment-content">
                {{ item.content }}
              </div>

              <div v-if="item.mentions && item.mentions.length > 0" class="comment-mentions">
                <el-tag
                  v-for="user in item.mentions"
                  :key="user.id"
                  size="small"
                  type="info"
                >
                  @{{ user.name }}
                </el-tag>
              </div>

              <!-- 回复列表 -->
              <div v-if="item.replies && item.replies.length > 0" class="comment-replies">
                <div
                  v-for="reply in item.replies"
                  :key="reply.id"
                  class="reply-item"
                >
                  <el-avatar :size="24" :src="reply.userAvatar">
                    {{ reply.userName?.charAt(0) }}
                  </el-avatar>
                  <div class="reply-content">
                    <span class="reply-user">{{ reply.userName }}</span>
                    <span class="reply-text">{{ reply.content }}</span>
                    <span class="reply-time">{{ reply.timestamp }}</span>
                  </div>
                </div>
              </div>

              <!-- 回复输入 -->
              <div class="reply-input">
                <el-input
                  v-model="item.replyText"
                  placeholder="回复评论..."
                  size="small"
                  @keyup.enter="handleReply(item)"
                >
                  <template #append>
                    <el-button @click="handleReply(item)">回复</el-button>
                  </template>
                </el-input>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div v-if="filteredTimeline.length === 0" class="empty-state">
        <el-empty description="暂无动态" />
      </div>
    </div>

    <!-- 编辑评论对话框 -->
    <el-dialog v-model="editCommentDialogVisible" title="编辑评论" width="500px">
      <el-input
        v-model="editingCommentText"
        type="textarea"
        :rows="6"
        placeholder="请输入评论内容"
      />
      <template #footer>
        <el-button @click="editCommentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEditComment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

// 监听案件数据变化，加载动态数据
watch(() => props.caseData.id, (newId) => {
  if (newId) {
    fetchTimeline()
  }
}, { immediate: true })
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { getCaseTimeline, createTimelineComment, deleteTimelineComment } from '@/api/case'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

const activeTab = ref('all')
const loading = ref(false)
const showMention = ref(false)
const newComment = ref('')
const mentionedUsers = ref([])
const currentUserId = ref('1') // 当前用户ID

// ==================== 编辑评论对话框 ====================
const editCommentDialogVisible = ref(false)
const editingComment = ref(null)
const editingCommentText = ref('')

// 时间线数据
const timelineData = ref([
  {
    id: '1',
    type: 'activity',
    timestamp: '2024-03-20 15:30',
    userName: '张律师',
    userAvatar: '',
    action: '创建了案件',
    details: [
      { label: '案件类型', value: '民事' },
      { label: '案由', value: '买卖合同纠纷' }
    ]
  },
  {
    id: '2',
    type: 'comment',
    timestamp: '2024-03-21 10:15',
    userName: '李律师',
    userAvatar: '',
    content: '案件已立案，注意关注举证期限',
    mentions: [],
    replies: [
      {
        id: '2-1',
        userName: '小张',
        userAvatar: '',
        content: '收到，会及时提醒',
        timestamp: '2024-03-21 10:20'
      }
    ]
  },
  {
    id: '3',
    type: 'activity',
    timestamp: '2024-03-22 14:00',
    userName: '小李',
    userAvatar: '',
    action: '上传了文档',
    details: [
      { label: '文档名称', value: '证据清单.xlsx' },
      { label: '文件夹', value: '原告证据' }
    ]
  }
])

// 团队成员
const teamMembers = ref([
  { id: '1', name: '张律师' },
  { id: '2', name: '李律师' },
  { id: '3', name: '小张' },
  { id: '4', name: '小李' }
])

// 过滤后的时间线
const filteredTimeline = computed(() => {
  if (activeTab.value === 'all') {
    return timelineData.value
  }
  return timelineData.value.filter(item => item.type === activeTab.value)
})

// 获取项目颜色
const getItemColor = (type) => {
  return type === 'comment' ? '#1890ff' : '#52c41a'
}

// 判断是否可以编辑
const canEdit = (item) => {
  return item.userId === currentUserId.value
}

// 判断是否可以删除
const canDelete = (item) => {
  return item.userId === currentUserId.value
}

// Tab切换
const handleTabChange = (tab) => {
  // removed debug log
}

// 切换@提及面板
const handleToggleMention = () => {
  showMention.value = !showMention.value
}

// 提交评论
const handleSubmitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const { id } = props.caseData
    const data = {
      content: newComment.value,
      mentionIds: mentionedUsers.value,
      parentId: null
    }

    await createTimelineComment(id, data)

    ElMessage.success('评论已添加')

    // 添加到本地列表
    timelineData.value.unshift({
      id: Date.now().toString(),
      type: 'comment',
      timestamp: new Date().toLocaleString('zh-CN'),
      userName: '当前用户',
      userAvatar: '',
      content: newComment.value,
      mentions: mentionedUsers.value.map(id => ({
        id,
        name: teamMembers.value.find(u => u.id === id)?.name
      })),
      replies: []
    })

    // 清空输入
    newComment.value = ''
    mentionedUsers.value = []
    showMention.value = false
  } catch (error) {
    ElMessage.error('添加评论失败')
    console.error(error)
  }
}

// 编辑评论
const handleEditComment = (item) => {
  editingComment.value = item
  editingCommentText.value = item.content || ''
  editCommentDialogVisible.value = true
}

const handleSubmitEditComment = async () => {
  if (!editingCommentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  try {
    // 更新评论
    const index = timelineData.value.findIndex(i => i.id === editingComment.value.id)
    if (index > -1) {
      timelineData.value[index].content = editingCommentText.value
      timelineData.value[index].edited = true
      timelineData.value[index].editTime = new Date().toISOString()
    }

    ElMessage.success('评论更新成功')
    editCommentDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新评论失败:', error)
    ElMessage.error('更新失败')
  }
}

// 删除评论
const handleDeleteComment = async (item) => {
  try {
    await deleteTimelineComment(props.caseData.id, item.id)
    timelineData.value = timelineData.value.filter(i => i.id !== item.id)
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 回复评论
const handleReply = (item) => {
  if (!item.replyText?.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  // 添加回复
  if (!item.replies) {
    item.replies = []
  }

  item.replies.push({
    id: `${item.id}-${Date.now()}`,
    userName: '当前用户',
    userAvatar: '',
    content: item.replyText,
    timestamp: new Date().toLocaleString('zh-CN')
  })

  item.replyText = ''
  ElMessage.success('回复已添加')
}

// 获取时间线数据
const fetchTimeline = async () => {
  try {
    loading.value = true
    const { id } = props.caseData
    const res = await getCaseTimeline(id)
    timelineData.value = res.data || []
  } catch (error) {
    console.error('获取案件动态失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化
fetchTimeline()
</script>

<style scoped lang="scss">
.case-timeline {
  padding: 30px;

  .comment-input-section {
    background-color: #f5f7fa;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 30px;

    .input-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;

      h4 {
        margin: 0;
        font-size: 14px;
        color: #333;
      }
    }

    .mention-panel {
      padding: 10px;
      background-color: #fff;
      border-radius: 4px;
      margin-bottom: 10px;
    }

    .input-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 10px;

      .tip {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .timeline-section {
    :deep(.el-timeline-item__timestamp) {
      font-weight: 500;
      color: #333;
    }

    .timeline-card {
      padding: 15px;
      background-color: #fff;
      border-radius: 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

      &.type-activity {
        border-left: 3px solid #52c41a;
      }

      &.type-comment {
        border-left: 3px solid #1890ff;
      }

      .activity-item {
        .activity-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .activity-user {
            display: flex;
            gap: 10px;
            align-items: center;

            .user-info {
              .user-name {
                font-size: 14px;
                font-weight: 500;
                color: #333;
              }

              .action-text {
                font-size: 13px;
                color: #606266;
                margin-top: 2px;
              }
            }
          }

          .activity-time {
            font-size: 12px;
            color: #909399;
          }
        }

        .activity-details {
          padding-top: 10px;
          border-top: 1px dashed #e4e7ed;

          .detail-item {
            display: inline-block;
            margin-right: 20px;
            font-size: 13px;
            color: #606266;

            .detail-label {
              color: #909399;
            }

            .detail-value {
              color: #333;
            }
          }
        }
      }

      .comment-item {
        .comment-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 10px;

          .comment-user {
            display: flex;
            gap: 10px;
            align-items: center;

            .user-info {
              .user-name {
                font-size: 14px;
                font-weight: 500;
                color: #333;
              }

              .comment-time {
                font-size: 12px;
                color: #909399;
                margin-top: 2px;
              }
            }
          }

          .comment-actions {
            display: flex;
            gap: 8px;
          }
        }

        .comment-content {
          margin-bottom: 10px;
          color: #333;
          line-height: 1.6;
          white-space: pre-wrap;
        }

        .comment-mentions {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
          margin-bottom: 10px;
        }

        .comment-replies {
          padding: 10px;
          background-color: #f5f7fa;
          border-radius: 4px;
          margin-bottom: 10px;

          .reply-item {
            display: flex;
            gap: 8px;
            margin-bottom: 8px;

            &:last-child {
              margin-bottom: 0;
            }

            .reply-content {
              flex: 1;

              .reply-user {
                font-size: 13px;
                font-weight: 500;
                color: #1890ff;
                margin-right: 8px;
              }

              .reply-text {
                font-size: 13px;
                color: #333;
              }

              .reply-time {
                font-size: 12px;
                color: #909399;
                margin-left: 8px;
              }
            }
          }
        }

        .reply-input {
          margin-top: 10px;
        }
      }
    }
  }

  .empty-state {
    padding: 60px 0;
  }
}
</style>
