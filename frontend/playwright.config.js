import { defineConfig } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:3017'
const apiURL = process.env.E2E_API_URL || 'http://localhost:8080/api'

export default defineConfig({
  testDir: './e2e',
  timeout: 180000,
  expect: { timeout: 15000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  use: {
    baseURL,
    trace: 'on-first-retry'
  },
  projects: [
    { name: 'api', testMatch: /(regression|intake-flow)\.spec\.js/ },
    { name: 'ui', testMatch: /intake-ui\.spec\.js/ }
  ],
  metadata: { apiURL }
})
