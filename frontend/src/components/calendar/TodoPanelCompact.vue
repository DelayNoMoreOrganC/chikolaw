<template>
  <div class="todo-panel-compact" :class="{ 'with-actions': showActions }">
    <div class="panel-header">
      <div class="header-left">
        <h3>{{ title }}</h3>
        <slot name="header-extra" />
      </div>
      <el-button v-if="showViewAll" text type="primary" size="small" @click="$emit('view-all')">
        查看全部
      </el-button>
    </div>

    <slot name="toolbar" />

    <div v-if="!displayList.length" class="empty">
      <slot name="empty">
        <span>暂无待办</span>
      </slot>
    </div>
    <div v-else class="todo-list">
      <div
        v-for="todo in displayList"
        :key="todo.id"
        class="todo-item"
        :class="resolveItemClass(todo)"
      >
        <el-checkbox
          :model-value="todo.completed || todo.status === 'COMPLETED'"
          @change="(v) => $emit('complete', todo, v)"
        />
        <div class="todo-body" @click="$emit('edit', todo)">
          <div class="todo-title">{{ todo.title }}</div>
          <div class="todo-meta">
            <PriorityDot :priority="todo.priority" />
            <span class="deadline-text">{{ formatDeadline(todo.deadline) }}</span>
            <el-tag
              v-if="todo.caseName"
              size="small"
              type="info"
              class="case-tag"
              @click.stop="$emit('go-case', todo.caseId)"
            >
              {{ todo.caseName }}
            </el-tag>
          </div>
        </div>
        <div v-if="showActions" class="todo-actions" @click.stop>
          <el-button text type="primary" size="small" @click="$emit('edit', todo)">编辑</el-button>
          <el-button text type="danger" size="small" @click="$emit('delete', todo)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PriorityDot from '@/components/PriorityDot.vue'
import { formatTodoDeadline, getTodoVisualState } from '@/utils/calendarUi'

const props = defineProps({
  todos: { type: Array, default: () => [] },
  /** 0 表示不截断 */
  limit: { type: Number, default: 5 },
  title: { type: String, default: '待办事项' },
  showViewAll: { type: Boolean, default: true },
  showActions: { type: Boolean, default: false },
  /** 自定义行样式类（日程页 todo-overdue 等） */
  itemClassFn: { type: Function, default: null },
  /** 是否对列表排序（日程页侧栏自行排序时可关闭） */
  sort: { type: Boolean, default: true }
})

defineEmits(['complete', 'edit', 'delete', 'go-case', 'view-all'])

const sortedTodos = computed(() => {
  const list = [...props.todos]
  if (!props.sort) {
    return list
  }
  return list.sort((a, b) => {
    const now = new Date()
    const aOver = new Date(a.deadline) < now
    const bOver = new Date(b.deadline) < now
    if (aOver && !bOver) return -1
    if (!aOver && bOver) return 1
    const pmap = { high: 3, URGENT: 3, medium: 2, NORMAL: 2, low: 1 }
    return (pmap[b.priority] || 1) - (pmap[a.priority] || 1)
  })
})

const displayList = computed(() => {
  if (!props.limit || props.limit <= 0) {
    return sortedTodos.value
  }
  return sortedTodos.value.slice(0, props.limit)
})

function resolveItemClass(todo) {
  if (props.itemClassFn) {
    return props.itemClassFn(todo)
  }
  return getTodoVisualState(todo)
}

function formatDeadline(deadline) {
  if (!deadline) return '-'
  return formatTodoDeadline(deadline)
}
</script>

<style scoped lang="scss">
.todo-panel-compact {
  display: flex;
  flex-direction: column;
  min-height: 0;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    h3 {
      margin: 0;
      font-size: 16px;
    }
  }

  .empty {
    color: #909399;
    font-size: 13px;
    padding: 12px 0;
    text-align: center;
  }

  .todo-list {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
  }

  .todo-item {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    padding: 10px 0;
    border-bottom: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.06));

    &.overdue,
    &.todo-overdue {
      background-color: #fff1f0;
      border-left: 3px solid #f56c6c;
      padding-left: 8px;
      .todo-title { color: #e5484d; font-weight: 600; }
    }
    &.urgent,
    &.todo-urgent .todo-title { color: #f76b15; }
    &.warning,
    &.todo-warning .todo-title { color: #e6a23c; }
    &.done,
    &.todo-completed {
      opacity: 0.65;
      .todo-title { text-decoration: line-through; color: #909399; }
    }

    .todo-body {
      flex: 1;
      cursor: pointer;
      min-width: 0;
    }

    .todo-title {
      font-size: 14px;
      margin-bottom: 4px;
      word-break: break-word;
    }

    .todo-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 12px;
      color: #909399;

      .case-tag { cursor: pointer; }
    }

    .todo-actions {
      display: flex;
      flex-shrink: 0;
      gap: 4px;
    }
  }

  &.with-actions .todo-item {
    align-items: center;
  }
}
</style>
