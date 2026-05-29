<template>
  <el-drawer
    :model-value="visible"
    title="日程详情"
    size="420px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <template v-if="event">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ event.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ calendarTypeLabel(event.data?.calendarType || event.type) }}
        </el-descriptions-item>
        <el-descriptions-item label="开始">{{ event.startTime || event.data?.startTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ event.endTime || event.data?.endTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ event.data?.location || '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联案件">{{ event.data?.caseName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="主办律师">{{ event.data?.ownerName || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div class="drawer-actions">
        <el-button v-if="event.data?.caseId" type="primary" @click="$emit('go-case', event.data.caseId)">
          打开案件
        </el-button>
        <el-button @click="$emit('open-calendar')">在日程页编辑</el-button>
        <template v-if="editable">
          <el-button type="primary" @click="$emit('edit')">编辑</el-button>
          <el-button type="danger" plain @click="$emit('delete')">删除</el-button>
        </template>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { calendarTypeLabel } from '@/utils/calendarUi'

defineProps({
  visible: { type: Boolean, default: false },
  event: { type: Object, default: null },
  editable: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'go-case', 'open-calendar', 'edit', 'delete'])
</script>

<style scoped>
.drawer-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
