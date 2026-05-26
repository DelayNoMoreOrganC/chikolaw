<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="420px"
    :before-close="handleClose"
  >
    <template #header>
      <div class="notification-header">
        <span class="title">通知中心</span>
        <div class="actions">
          <el-button v-if="unreadCount > 0" type="primary" size="small" link @click="markAllRead">
            全部已读
          </el-button>
        </div>
      </div>
    </template>

    <div class="notification-content">
      <div v-if="summary.unreadByGroup && Object.keys(summary.unreadByGroup).length" class="group-badges">
        <el-tag
          v-for="(count, group) in summary.unreadByGroup"
          :key="group"
          size="small"
          type="danger"
          effect="plain"
        >
          {{ groupLabel(group) }} {{ count }}
        </el-tag>
      </div>

      <el-tabs v-model="activeCategory" @tab-change="onCategoryChange">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待办" name="TODO" />
        <el-tab-pane label="案件" name="CASE" />
        <el-tab-pane label="审批" name="APPROVAL" />
        <el-tab-pane label="系统" name="SYSTEM" />
      </el-tabs>

      <div v-loading="loading" class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.isRead }"
          @click="onItemClick(item)"
        >
          <div class="notification-icon">{{ iconFor(item.categoryGroup) }}</div>
          <div class="notification-body">
            <div class="notification-title-row">
              <span class="notification-title">{{ item.title }}</span>
              <el-tag size="small" type="info" effect="plain">{{ item.categoryLabel }}</el-tag>
            </div>
            <div class="notification-content-text">{{ item.content }}</div>
            <div class="notification-time">{{ formatTime(item.createdAt) }}</div>
          </div>
          <div class="notification-actions" @click.stop>
            <el-button v-if="!item.isRead" link type="primary" size="small" @click="markOneRead(item)">
              已读
            </el-button>
            <el-button link type="danger" size="small" @click="removeOne(item)">删除</el-button>
          </div>
        </div>

        <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" :image-size="80" />

        <div v-if="hasMore && !loading" class="load-more">
          <el-button text @click="loadMore">加载更多</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getNotificationList,
  getUnreadCount,
  getNotificationSummary,
  markAsRead,
  markAllAsRead,
  deleteNotification
} from '@/api/notification'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  unreadCount: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue', 'update:unreadCount'])

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const notifications = ref([])
const activeCategory = ref('')
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const summary = ref({ unreadCount: 0, unreadByGroup: {} })

const hasMore = computed(() => notifications.value.length < total.value)

const groupLabels = {
  TODO: '待办',
  CASE: '案件',
  CALENDAR: '日程',
  APPROVAL: '审批',
  SYSTEM: '系统'
}

const groupLabel = (g) => groupLabels[g] || g

const iconFor = (group) => {
  const map = { TODO: '⏰', CASE: '⚖️', APPROVAL: '✅', CALENDAR: '📅', SYSTEM: '🔔' }
  return map[group] || '📢'
}

const refreshUnread = async () => {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      emit('update:unreadCount', res.data ?? 0)
    }
  } catch {
    /* ignore */
  }
}

const loadSummary = async () => {
  try {
    const res = await getNotificationSummary()
    if (res.code === 200 && res.data) {
      summary.value = res.data
    }
  } catch {
    summary.value = { unreadCount: 0, unreadByGroup: {} }
  }
}

const fetchList = async (page = 1, append = false) => {
  loading.value = true
  try {
    const params = { page, size: pageSize }
    if (activeCategory.value) {
      params.categoryGroup = activeCategory.value
    }
    const res = await getNotificationList(params)
    if (res.code === 200 && res.data) {
      const list = res.data.records || []
      notifications.value = append ? [...notifications.value, ...list] : list
      total.value = res.data.total ?? list.length
      currentPage.value = page
    }
  } catch (e) {
    ElMessage.error(e.message || '获取通知失败')
  } finally {
    loading.value = false
  }
}

const onCategoryChange = () => {
  currentPage.value = 1
  fetchList(1, false)
}

const loadMore = () => {
  fetchList(currentPage.value + 1, true)
}

const markOneRead = async (item) => {
  try {
    const res = await markAsRead(item.id)
    if (res.code === 200) {
      item.isRead = true
      await refreshUnread()
      await loadSummary()
    }
  } catch {
    ElMessage.error('标记失败')
  }
}

const markAllRead = async () => {
  try {
    const res = await markAllAsRead()
    if (res.code === 200) {
      notifications.value.forEach((n) => { n.isRead = true })
      ElMessage.success('已全部标记为已读')
      await refreshUnread()
      await loadSummary()
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const removeOne = async (item) => {
  try {
    const res = await deleteNotification(item.id)
    if (res.code === 200) {
      notifications.value = notifications.value.filter((n) => n.id !== item.id)
      total.value = Math.max(0, total.value - 1)
      if (!item.isRead) {
        await refreshUnread()
        await loadSummary()
      }
      ElMessage.success('已删除')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

const onItemClick = async (item) => {
  if (!item.isRead) {
    await markOneRead(item)
  }
  if (item.routePath) {
    visible.value = false
    router.push(item.routePath)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const diff = Date.now() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return date.toLocaleString('zh-CN')
}

const handleClose = () => {
  visible.value = false
}

watch(visible, (open) => {
  if (open) {
    currentPage.value = 1
    loadSummary()
    fetchList(1, false)
  }
})

defineExpose({ refreshUnread, loadSummary })
</script>

<style scoped lang="scss">
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  .title {
    font-size: 16px;
    font-weight: 600;
  }
}

.notification-content {
  display: flex;
  flex-direction: column;
  height: 100%;

  .group-badges {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 8px;
  }

  .notification-list {
    flex: 1;
    overflow-y: auto;
    margin-top: 8px;
    padding-right: 4px;
  }

  .notification-item {
    display: flex;
    gap: 10px;
    padding: 12px;
    margin-bottom: 8px;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    cursor: pointer;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

    &.unread {
      background: #ecf5ff;
      border-color: #b3d8ff;
    }

    .notification-icon {
      font-size: 22px;
      line-height: 1;
    }

    .notification-body {
      flex: 1;
      min-width: 0;

      .notification-title-row {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;
      }

      .notification-title {
        font-weight: 500;
        font-size: 14px;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .notification-content-text {
        font-size: 13px;
        color: #606266;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .notification-time {
        font-size: 12px;
        color: #909399;
        margin-top: 6px;
      }
    }

    .notification-actions {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
  }

  .load-more {
    text-align: center;
    padding: 8px 0;
  }
}
</style>
