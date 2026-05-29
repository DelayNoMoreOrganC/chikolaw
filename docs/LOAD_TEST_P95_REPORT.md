# 局域网 50 人压测 P95 报告（v2.2）

## 环境

| 项 | 说明 |
|----|------|
| 脚本 | `scripts/load-test/k6-lan-50.js` |
| 目标 | `BASE_URL` 指向 Spring Boot `/api`（生产建议 Nginx 反代） |
| 并发 | 0 → 25（30s）→ 50（1m 保持 2m）→ 0 |
| 不含 | LLM / 卷宗 Vision（避免 API 配额干扰 CRUD 指标） |

## 执行

```powershell
# 安装 k6: https://k6.io/docs/get-started/installation/
cd d:\ZGAI
k6 run -e BASE_URL=http://localhost:8080/api -e USER=admin -e PASS=admin123 scripts/load-test/k6-lan-50.js
```

将控制台输出保存为 `docs/load-test-last-run.txt` 便于归档。

## 验收阈值（与 ARCHITECTURE_LAN_50_USERS.md 对齐）

| 接口 | P95 目标 |
|------|----------|
| POST `/auth/login` | < 800 ms |
| GET `/dashboard/overview` | < 500 ms |
| GET `/cases` 列表 | < 500 ms |
| 错误率 | < 5% |

## 结果记录（填写实测）

| 日期 | 部署 | login P95 | dashboard P95 | cases P95 | 错误率 | 备注 |
|------|------|-----------|---------------|-----------|--------|------|
| _待测_ | dev H2 单实例 | — | — | — | — | 开发环境仅供参考 |
| _待测_ | prod MySQL+Redis | — | — | — | — | 正式验收 |

## 说明

- 开发环境（H2 文件库 + 单 JAR）数值**不能**代表生产。
- 卷宗上传压测需单独场景并 mock LLM，避免计费与超时污染 CRUD 报告。
