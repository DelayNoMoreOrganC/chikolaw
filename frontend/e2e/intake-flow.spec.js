import { test, expect } from '@playwright/test'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { login, authHeaders } from './helpers/auth.js'

const apiURL = process.env.E2E_API_URL || 'http://localhost:8080/api'
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '../..')

function resolveSamplePdf() {
  const candidates = [
    path.join(rootDir, 'tmp-intake-test.pdf'),
    path.join(rootDir, 'backend/uploads/intake-pending/alloc-1779814950332_tmp-intake-test.pdf')
  ]
  return candidates.find((p) => fs.existsSync(p))
}

test.describe('卷宗录入 API E2E', () => {
  let token

  test.beforeAll(async ({ request }) => {
    try {
      token = await login(request, apiURL)
    } catch (e) {
      test.skip(true, `后端不可用: ${e.message}`)
    }
  })

  test('登录后可查询暂存列表', async ({ request }) => {
    const res = await request.get(`${apiURL}/case-intake/pending`, {
      headers: authHeaders(token)
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    expect(Array.isArray(body.data)).toBeTruthy()
  })

  test('可搜索案件', async ({ request }) => {
    const res = await request.get(`${apiURL}/case-intake/cases/search`, {
      headers: authHeaders(token),
      params: { q: '', limit: 5 }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
  })

  test('attach-pending 挂接暂存卷宗', async ({ request }) => {
    const pendingId = process.env.E2E_PENDING_ID
    const caseId = process.env.E2E_CASE_ID
    test.skip(!pendingId || !caseId, '设置 E2E_PENDING_ID 与 E2E_CASE_ID 后运行完整挂接测试')

    const form = new FormData()
    form.append('pendingId', pendingId)
    form.append('caseId', caseId)

    const res = await request.post(`${apiURL}/case-intake/attach-pending`, {
      headers: authHeaders(token),
      multipart: {
        pendingId,
        caseId
      }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    expect(['SUCCESS', 'NEEDS_CASE']).toContain(body.data?.status)
  })

  test('NEEDS_CASE：上传样例 PDF（需 GLM，E2E_RUN_INTAKE=1）', async ({ request }) => {
    test.skip(process.env.E2E_RUN_INTAKE !== '1', '设置 E2E_RUN_INTAKE=1 启用（依赖 ZHIPU_API_KEY）')
    const pdfPath = resolveSamplePdf()
    test.skip(!pdfPath, '未找到 tmp-intake-test.pdf')

    const res = await request.post(`${apiURL}/case-intake/process`, {
      headers: authHeaders(token),
      multipart: {
        file: {
          name: 'tmp-intake-test.pdf',
          mimeType: 'application/pdf',
          buffer: fs.readFileSync(pdfPath)
        }
      },
      timeout: 180000
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
    expect(['SUCCESS', 'NEEDS_CASE']).toContain(body.data?.status)
    if (body.data?.status === 'NEEDS_CASE') {
      expect(body.data.pendingId).toBeTruthy()
    }
  })

  test('立案审批申请（E2E_PENDING_ID）', async ({ request }) => {
    const pendingId = process.env.E2E_PENDING_ID
    test.skip(!pendingId, '设置 E2E_PENDING_ID 后运行')

    const res = await request.post(`${apiURL}/case-intake/filing-application`, {
      headers: authHeaders(token),
      multipart: {
        pendingId,
        title: 'E2E 立案申请',
        content: 'Playwright 自动化测试'
      }
    })
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code === 200 || body.success).toBeTruthy()
  })
})
