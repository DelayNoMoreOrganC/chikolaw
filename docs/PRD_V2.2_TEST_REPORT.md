# PRD v2.2 功能测试报告

**测试日期**：2026-05-27  
**版本**：v2.2.0  
**PRD**：[PRD.md](../PRD.md) v2.2  
**测试人**：自动化 + 代码审查（开发环境）

---

## 1. 测试范围（v2.2 已完成功能）

### 1.1 AI 全链路 GLM

| # | 功能 | 验收标准 | 结果 |
|---|------|----------|------|
| T-AI-01 | `cloud-glm` 模式 | diagnostics 返回 `cloudGlm=true`、`lawfirmAiMode=cloud-glm` | 见 §2 |
| T-AI-02 | 七场景路由 | 各场景 `providerType=zhipu` | 见 §2 |
| T-AI-03 | OCR 提供商 | `ocrProvider=zhipu` | 见 §2 |
| T-AI-04 | Agent 运行时 | `activeProvider=builtin` | 见 §2 |
| T-AI-05 | 路由单测 | `AIModelRoutingServiceTest` 含 cloud-glm 强制用例 | ✅ 通过 |
| T-AI-06 | fallback 关闭 | `cloud-glm` 下 `LLMApiService` 不降级 | ✅ 代码审查 |

### 1.2 卷宗智能录入

| # | 功能 | 验收标准 | 结果 |
|---|------|----------|------|
| T-INT-01 | 服务单测 | `CaseFileIntakeServiceTest` | ✅ 通过 |
| T-INT-02 | 暂存/挂接 | `CaseIntakePendingServiceTest` | ✅ 通过 |
| T-INT-03 | 失败透出 | `AIServiceException` 不吞掉 → `FAILED` | ✅ 代码审查 |
| T-INT-04 | 前端 FAILED 区 | `CaseFileIntakePanel` 红色 alert | ✅ 代码审查 |
| T-INT-05 | E2E 手工 | [INTAKE_FLOW_E2E_CHECKLIST.md](./INTAKE_FLOW_E2E_CHECKLIST.md) | ⏳ 需人工 + 有效 `ZHIPU_API_KEY` |

### 1.3 工作台与入口

| # | 功能 | 验收标准 | 结果 |
|---|------|----------|------|
| T-UI-01 | 卷宗录入首屏 | Dashboard 顶部 `CaseFileIntakePanel` | ✅ 代码审查 |
| T-UI-02 | 去除重复上传 | 无「AI 智能创建」拖拽区 | ✅ 代码审查 |
| T-UI-03 | 引导 AI 中心 | 待办区按钮 → `/ai-hub` | ✅ 代码审查 |

### 1.4 文书识别 API

| # | 功能 | 验收标准 | 结果 |
|---|------|----------|------|
| T-REC-01 | 格式 | PDF/图/docx/txt | ✅ `AiDocumentFileSupport` |
| T-REC-02 | 大小 | ≤50MB | ✅ 控制器校验 |
| T-REC-03 | docx 文本路径 | 先 `DocumentTextExtractService` | ✅ `AIDocumentService` |

### 1.5 核心 P0/P1（回归抽样）

| 模块 | 自动化 | 结果 |
|------|--------|------|
| 认证/用户密码策略 | `UserServicePasswordTest` 等 | ⚠️ H2 文件锁（后端进程占用 DB） |
| 审批工作流 | `ApprovalWorkflowServiceTest` | ⚠️ 同上 |
| 案件流程模板 | `CaseFlowDefinitionServiceTest` | ⚠️ 同上 |
| 前端构建 | `npm run test`（= build） | ✅ 7.4s 构建成功 |

---

## 2. 运行时冒烟（若后端已启动）

在 backend 运行且已配置 `ZHIPU_API_KEY` 时执行：

```http
POST /api/auth/login  {"username":"admin","password":"admin123"}
GET  /api/ai/diagnostics        Authorization: Bearer {token}
GET  /api/agent/runtime/status
```

**期望**：`cloudGlm=true`，`ocrProvider=zhipu`，`activeProvider=builtin`。

---

## 3. 测试结论

| 类别 | 结论 |
|------|------|
| v2.2 AI GLM 改造 | **通过**（单测 + 代码审查 + 配置一致性） |
| 卷宗录入链路 | **通过**（单测）；**待**真实 PDF + API Key 手工 E2E |
| 工作台去重 | **通过**（UI 审查） |
| 全量 `mvn test` | **阻塞**：开发库 H2 被运行中后端锁定；需停服后重跑 |

---

## 4. 下一步优化方案（建议排期）

### P0 — 稳定性与可验收（1 周）

1. **卷宗录入 E2E**：Playwright 覆盖 NEEDS_CASE → attach-pending → 立案审批（mock GLM 或测试 Key）。
2. **测试环境 H2**：`application-test.yml` 使用内存库 `jdbc:h2:mem:test`，避免与 dev 进程锁库冲突。
3. **重启验收脚本**：`scripts/verify-v2.2.ps1`（login → diagnostics → 上传样例 PDF → 断言 status）。

### P1 — 体验与交付物（2–3 周）

4. **AI 统一向导**：`/ai-hub` 上传后选择「归入卷宗 / 预填建案 / 仅识别 / 创建待办」单流程。
5. **文书 `.docx` 导出**：对接 `python-docx` 或 POI，满足 PRD 起诉状/代理词交付形态。
6. **业务自动化反馈**：识别 API 返回真实 `businessLogic` 字段，前端不再硬编码「待办已创建」。

### P2 — v3.0 铺垫（并行设计）

7. **hybrid 模式验收**：`LAWFIRM_AI_MODE=hybrid` + LM Studio 集成测试矩阵。
8. **智谱 Embedding**：评估替换阿里云向量，统一供应商账单与合规叙事。
9. **50 人压测报告**：按 [ARCHITECTURE_LAN_50_USERS.md](./ARCHITECTURE_LAN_50_USERS.md) 出正式数据。

### P3 — 技术债

10. 移除 `@Deprecated` `ocr-upload` 调用方；合并 orphan 组件 `AIDocGenerator` / `AIAgentSidebar`。
11. WebSocket 通知；分片上传前端对接 `ChunkedUploadController`。
12. 全站 macOS Token 扫尾（附录 A.6 体验项）。

---

## 5. 参考

- [AI_V2.2_GLM.md](./AI_V2.2_GLM.md)
- [GLM_CODING_PLAN_SETUP.md](./GLM_CODING_PLAN_SETUP.md)
- [INTAKE_FLOW_E2E_CHECKLIST.md](./INTAKE_FLOW_E2E_CHECKLIST.md)
- [CURRENT_STATUS.md](../CURRENT_STATUS.md)
