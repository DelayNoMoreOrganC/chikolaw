/**
 * 开发环境自动以管理员登录（可选关闭：.env.development 中 VITE_DEV_AUTO_LOGIN=false）
 */

import { ElMessage } from 'element-plus'
import axios from 'axios'

const DEV_CREDENTIALS = {
  username: 'admin',
  password: 'admin123'
}

const isDev = () => import.meta.env.DEV === true

/** 与 stores/user.js 中 login 成功后的 userInfo 结构一致 */
function persistSession(payload) {
  const d = payload
  if (!d?.token) return false
  localStorage.setItem('token', d.token)
  localStorage.setItem(
    'userInfo',
    JSON.stringify({
      id: d.userId,
      username: d.username,
      realName: d.realName,
      email: d.email,
      phone: d.phone,
      avatar: d.avatar
    })
  )
  return true
}

export async function autoLogin() {
  if (!isDev()) return false
  if (import.meta.env.VITE_DEV_AUTO_LOGIN === 'false') return false

  const existing = localStorage.getItem('token')
  if (existing) {
    return true
  }

  try {
    const { data: body } = await axios.post('/api/auth/login', DEV_CREDENTIALS, {
      timeout: 10000,
      headers: { 'Content-Type': 'application/json;charset=UTF-8' }
    })

    if (body?.code === 200 && body.data && persistSession(body.data)) {
      return true
    }

    console.warn('[dev-auto-login] 响应异常', body)
    return false
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '网络错误'
    console.warn('[dev-auto-login] 失败:', msg)
    ElMessage.warning(`开发环境自动登录失败：${msg}，请手动登录或检查后端是否已启动`)
    return false
  }
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  window.location.reload()
}

export default { autoLogin, clearAuth, isDev }
