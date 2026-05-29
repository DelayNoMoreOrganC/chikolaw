import { test, expect } from '@playwright/test'

const apiURL = process.env.E2E_API_URL || 'http://localhost:8080/api'

async function loginViaApi(request) {
  const res = await request.post(`${apiURL}/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
    headers: { 'Content-Type': 'application/json' }
  })
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  return body.data?.token
}

test.describe('卷宗与 AI Hub UI', () => {
  test.beforeEach(async ({ page, request }) => {
    let token
    try {
      token = await loginViaApi(request)
    } catch {
      test.skip(true, '后端不可用')
    }
    await page.goto('/login')
    await page.evaluate((t) => {
      localStorage.setItem('token', t)
      localStorage.setItem(
        'userInfo',
        JSON.stringify({ id: 1, username: 'admin', realName: 'admin' })
      )
    }, token)
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')
  })

  test('工作台展示卷宗智能录入面板', async ({ page }) => {
    await expect(page.getByText('卷宗智能录入').first()).toBeVisible({ timeout: 15000 })
  })

  test('AI 中心展示三步向导', async ({ page }) => {
    await page.goto('/ai-hub')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.ai-unified-wizard').getByText('智能文书向导', { exact: true })).toBeVisible({ timeout: 15000 })
    const stepTitle = (name) => page.locator('.el-step__title').filter({ hasText: name })
    await expect(stepTitle('上传文件')).toBeVisible()
    await expect(stepTitle('选择意图')).toBeVisible()
    await expect(stepTitle('查看结果')).toBeVisible()
  })

  test('带 caseId 打开 AI 中心', async ({ page, request }) => {
    const listRes = await request.get(`${apiURL}/cases?page=1&size=1`, {
      headers: { Authorization: `Bearer ${await loginViaApi(request)}` }
    })
    const listBody = await listRes.json()
    const caseId = listBody.data?.records?.[0]?.id
    test.skip(!caseId, '无案件数据')

    await page.goto(`/ai-hub?intent=recognize&caseId=${caseId}`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('.ai-unified-wizard').getByText('智能文书向导', { exact: true })).toBeVisible()
    await expect(page.getByText(`关联案件 ID：${caseId}`)).toBeVisible({ timeout: 10000 })
  })
})
