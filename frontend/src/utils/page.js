/** 统一解析后端 PageResult（records）与 Spring Page（content） */
export function pageRecords(res) {
  const data = res?.data
  if (!data) return []
  if (Array.isArray(data)) return data
  return data.records || data.content || []
}

export function pageTotal(res) {
  const data = res?.data
  if (!data || Array.isArray(data)) return 0
  return data.total ?? data.totalElements ?? 0
}
