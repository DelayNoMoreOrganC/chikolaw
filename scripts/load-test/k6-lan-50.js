/**
 * 50 人局域网压测脚本（k6）
 * 用法:
 *   k6 run -e BASE_URL=http://localhost:8080/api -e USER=admin -e PASS=admin123 scripts/load-test/k6-lan-50.js
 */
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend } from 'k6/metrics'

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080/api'
const username = __ENV.USER || 'admin'
const password = __ENV.PASS || 'admin123'

const loginTrend = new Trend('login_duration')
const dashboardTrend = new Trend('dashboard_duration')
const casesTrend = new Trend('cases_list_duration')

export const options = {
  scenarios: {
    lan_50: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 25 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '20s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    login_duration: ['p(95)<800'],
    dashboard_duration: ['p(95)<500'],
    cases_list_duration: ['p(95)<500']
  }
}

function login() {
  const res = http.post(
    `${baseUrl}/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  )
  loginTrend.add(res.timings.duration)
  check(res, { 'login ok': (r) => r.status === 200 })
  const body = res.json()
  return body.data?.token
}

export default function () {
  const token = login()
  if (!token) {
    sleep(1)
    return
  }
  const headers = { Authorization: `Bearer ${token}` }

  const dash = http.get(`${baseUrl}/dashboard/overview`, { headers, tags: { name: 'dashboard' } })
  dashboardTrend.add(dash.timings.duration)
  check(dash, { 'dashboard ok': (r) => r.status === 200 })

  const cases = http.get(`${baseUrl}/cases?page=0&size=20`, { headers, tags: { name: 'cases' } })
  casesTrend.add(cases.timings.duration)
  check(cases, { 'cases ok': (r) => r.status === 200 })

  sleep(1)
}

export function handleSummary(data) {
  const p95Login = data.metrics.login_duration?.values?.['p(95)'] ?? 'n/a'
  const p95Dash = data.metrics.dashboard_duration?.values?.['p(95)'] ?? 'n/a'
  const p95Cases = data.metrics.cases_list_duration?.values?.['p(95)'] ?? 'n/a'
  return {
    stdout: [
      '=== ZGAI LawOS 50-user load test summary ===',
      `login P95: ${p95Login} ms`,
      `dashboard P95: ${p95Dash} ms`,
      `cases list P95: ${p95Cases} ms`,
      'Record full output: k6 run ... 2>&1 | tee docs/load-test-last-run.txt'
    ].join('\n')
  }
}
