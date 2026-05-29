/** 表单展示：YYYY-MM-DD HH:mm（本地时区，不含秒） */
export function formatLocalDateTime(date = new Date()) {
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${mo}-${day} ${h}:${min}`
}

/** 表单值 → 后端 LocalDateTime（ISO，秒固定为 00） */
export function toApiDateTime(value) {
  if (value == null || value === '') return null
  const s = String(value).trim().replace('T', ' ')
  const m = s.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?$/)
  if (m) {
    const h = m[2].padStart(2, '0')
    const sec = m[4] || '00'
    return `${m[1]}T${h}:${m[3]}:${sec}`
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s.replace(' ', 'T'))) {
    return `${s.replace(' ', 'T')}:00`
  }
  return String(value).trim()
}

/** 展示：去掉秒与 T */
export function formatDisplayDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}
