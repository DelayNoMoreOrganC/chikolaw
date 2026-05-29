/**
 * @param {import('@playwright/test').APIRequestContext} request
 */
export async function login(request, apiURL) {
  const username = process.env.E2E_USER || 'admin'
  const password = process.env.E2E_PASSWORD || 'admin123'
  const res = await request.post(`${apiURL}/auth/login`, {
    data: { username, password },
    headers: { 'Content-Type': 'application/json' }
  })
  if (!res.ok()) {
    throw new Error(`登录失败 HTTP ${res.status()}`)
  }
  const body = await res.json()
  const token = body.data?.token
  if (!token) {
    throw new Error('登录响应无 token')
  }
  return token
}

export function authHeaders(token) {
  return { Authorization: `Bearer ${token}` }
}
