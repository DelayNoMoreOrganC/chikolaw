<template>
  <div class="calendar-week-view">
    <div v-if="showToolbar" class="week-toolbar">
      <el-button circle size="small" @click="shiftWeek(-1)">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="week-label">{{ weekLabel }}</span>
      <el-button circle size="small" @click="shiftWeek(1)">
        <el-icon><ArrowRight /></el-icon>
      </el-button>
      <el-button size="small" @click="goToday">今天</el-button>
    </div>
    <div class="week-grid">
      <div
        v-for="day in weekDays"
        :key="day.iso"
        class="week-day"
        :class="{ 'is-today': day.isToday }"
      >
        <div class="day-head">
          <span class="weekday">周{{ day.name }}</span>
          <span class="day-num">{{ day.date }}</span>
        </div>
        <div class="day-events">
          <el-tag
            v-for="event in eventsForDay(day.iso)"
            :key="event.id"
            :type="getEventTagType(event.type)"
            size="small"
            class="event-tag"
            @click.stop="$emit('event-click', event)"
          >
            {{ event.title }}
          </el-tag>
          <span v-if="!eventsForDay(day.iso).length" class="empty-hint">—</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  buildWeekDays,
  getEventTagType,
  getEventsForDate,
  startOfWeek
} from '@/utils/calendarUi'

const props = defineProps({
  events: { type: Array, default: () => [] },
  modelValue: { type: Date, default: () => new Date() },
  showToolbar: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'event-click'])

const weekDays = computed(() => buildWeekDays(props.modelValue))

const weekLabel = computed(() => {
  const start = startOfWeek(props.modelValue)
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  return `${start.getFullYear()}年${start.getMonth() + 1}月${start.getDate()}日 — ${end.getMonth() + 1}月${end.getDate()}日`
})

const eventsForDay = (iso) => getEventsForDate(props.events, iso)

const shiftWeek = (delta) => {
  const d = new Date(props.modelValue)
  d.setDate(d.getDate() + delta * 7)
  emit('update:modelValue', d)
}

const goToday = () => {
  emit('update:modelValue', new Date())
}
</script>

<style scoped lang="scss">
.calendar-week-view {
  .week-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .week-label {
      flex: 1;
      text-align: center;
      font-size: 14px;
      font-weight: 500;
      color: var(--lawos-text, #1c1c1e);
    }
  }

  .week-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    min-height: 160px;
  }

  .week-day {
    border: 1px solid var(--lawos-border, rgba(15, 23, 42, 0.08));
    border-radius: var(--lawos-radius-md, 8px);
    padding: 8px;
    background: var(--lawos-surface-solid, #fff);
    min-height: 120px;

    &.is-today {
      border-color: var(--lawos-primary, #3b6fd9);
      box-shadow: 0 0 0 1px rgba(59, 111, 217, 0.2);
    }

    .day-head {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: var(--lawos-text-secondary, #6b7280);
      margin-bottom: 6px;

      .day-num {
        font-weight: 600;
        color: var(--lawos-text, #1c1c1e);
      }
    }

    .day-events {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .event-tag {
        cursor: pointer;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .empty-hint {
        color: #c0c4cc;
        font-size: 12px;
      }
    }
  }
}

@media (max-width: 768px) {
  .calendar-week-view .week-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
