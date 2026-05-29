import { test, expect } from '@playwright/test'
import { login, authHeaders } from './helpers/auth.js'

const apiURL = process.env.E2E_API_URL || 'http://localhost:8080/api'

test.describe('v2.3 API 回归', () => {
  let token

  test.beforeAll(async ({ request }) => {
    try {
      token = await login(request, apiURL)
    } catch (e) {
      test.skip(true, `后端不可用: ${e.message}`)
    }
  })

  test('案件列表 quickFilter=pending_approval', async ({ request }) => {
    const res = await request.get(`${apiURL}/cases`, {
      headers: authHeaders(token),
      params: { page: 1, size: 10, quickFilter: 'pending_approval' }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    const records = body.data?.records || []
    for (const row of records) {
      expect(row.status).toBe('PENDING_FILING')
      expect(['WAIVER_PENDING', 'CONFLICT']).toContain(row.conflictCheckStatus)
    }
  })

  test('案件列表 quickFilter=pending_intake', async ({ request }) => {
    const res = await request.get(`${apiURL}/cases`, {
      headers: authHeaders(token),
      params: { page: 1, size: 10, quickFilter: 'pending_intake' }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    const records = body.data?.records || []
    for (const row of records) {
      expect(row.status).toBe('PENDING_FILING')
      expect(row.stageFoldersInitialized).not.toBe(true)
    }
  })

  test('日程 events 接口可用', async ({ request }) => {
    const start = '2026-01-01'
    const end = '2026-12-31'
    const res = await request.get(`${apiURL}/calendar/events`, {
      headers: authHeaders(token),
      params: { start, end }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    expect(Array.isArray(body.data)).toBeTruthy()
  })

  test('待办列表接口可用', async ({ request }) => {
    const res = await request.get(`${apiURL}/todos`, {
      headers: authHeaders(token),
      params: { page: 1, size: 5 }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
  })
})
