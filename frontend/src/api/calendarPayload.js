const REMINDER_MINUTES = {
  '15m': 15,
  '30m': 30,
  '1h': 60,
  '1d': 1440,
  '3d': 4320,
  '7d': 10080
}

const REMINDER_BY_MINUTES = Object.fromEntries(
  Object.entries(REMINDER_MINUTES).map(([k, v]) => [String(v), k])
)

export function formatCalendarDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

/** 表单 → 后端 CalendarDTO */
export function toCalendarPayload(form) {
  const minutes = REMINDER_MINUTES[form.reminder]
  const padTime = (t) => {
    if (!t) return t
    if (t.length === 16) return `${t}:00`
    return t
  }
  return {
    title: form.title,
    calendarType: (form.type || 'other').toUpperCase(),
    startTime: padTime(form.startTime),
    endTime: padTime(form.endTime),
    location: form.location || null,
    caseId: form.caseId || null,
    participantIds: form.participants || [],
    reminder: minutes != null,
    reminderMinutes: minutes ?? 0,
    repeatRule: form.repeat || null
  }
}

/** 后端 DTO → 表单 */
export function calendarToForm(dto) {
  if (!dto) return {}
  const minutes = dto.reminderMinutes != null ? String(dto.reminderMinutes) : ''
  return {
    id: dto.id,
    title: dto.title || '',
    type: (dto.calendarType || '').toLowerCase(),
    startTime: formatCalendarDateTime(dto.startTime),
    endTime: formatCalendarDateTime(dto.endTime),
    location: dto.location || '',
    caseId: dto.caseId || '',
    participants: dto.participantIds || [],
    reminder: REMINDER_BY_MINUTES[minutes] || (dto.reminder ? '1d' : ''),
    repeat: dto.repeatRule || '',
    remark: ''
  }
}

export function normalizeCalendarEvent(dto) {
  return {
    ...dto,
    type: (dto.calendarType || '').toLowerCase(),
    repeat: dto.repeatRule || '',
    startTime: formatCalendarDateTime(dto.startTime) || dto.startTime
  }
}
