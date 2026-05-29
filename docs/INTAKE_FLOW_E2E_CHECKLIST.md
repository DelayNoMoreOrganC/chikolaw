# 卷宗录入 + 案件流程 验收清单

> 手工冒烟测试（约 15 分钟）。前置：后端 `dev` 配置、`backend/.env` 已配置 `ZHIPU_API_KEY`、`LAWFIRM_AI_MODE=cloud-glm`、前端已登录、至少 1 个民事案件。  
> PRD v2.2 见 [PRD.md](../PRD.md)；自动化测试报告见 [PRD_V2.2_TEST_REPORT.md](./PRD_V2.2_TEST_REPORT.md)。

## 1. 卷宗智能录入（主工作台）

| # | 步骤 | 预期 |
|---|------|------|
| 1.1 | 上传 PDF/图片到「卷宗智能录入」，不选案件 | AI 分析完成；状态 `NEEDS_CASE`；返回 `pendingId` |
| 1.2 | 选择已有案件 →「确认归档」 | 使用 `attach-pending`，无需重传；`SUCCESS`；案件动态有 `FILE_INTAKE` |
| 1.3 | 再次上传未匹配文书 →「发起立案申请」 | 弹窗提交；审批中心出现「立案申请」类型 |
| 1.3b | 主任同意立案审批 | 系统自动创建 `PENDING_FILING` 草稿案件并挂接卷宗；通知/弹窗引导至 `/case/{id}/edit` |
| 1.3c | 完善草稿并保存 | 编辑页展示「卷宗立案草稿」提示；无需重复 `attach-pending` |
| 1.3d | （旧数据无草稿时）申请人点「完善立案」 | 回退 `/case/create?intakePendingId=` 预填后建案并挂接 |
| 1.4 | 上传时预选案件或案号可匹配 | 直接 `SUCCESS`，文档进入对应文件夹 |

## 2. 案件阶段与待办

| # | 步骤 | 预期 |
|---|------|------|
| 2.1 | 新建民事案件 | 仅**首阶段**待办生成（非全阶段） |
| 2.2 | 案件详情推进阶段 | 当前阶段待办创建；可前进/回退（回退走独立 API） |
| 2.3 | 看板视图 | 列名与 `case-lifecycle.js` / 后端 `CaseFlowDefinitionService` 一致 |

## 3. 案件文档 Tab

| # | 步骤 | 预期 |
|---|------|------|
| 3.1 | 同案件同名文件上传两次 | 列表显示 `v1`、`v2`（`versionNo` 递增） |
| 3.2 | 预览 PDF / 图片 | 对话框内 iframe / 图片（带鉴权 blob） |
| 3.2b | 预览 doc/docx/xls/xlsx/ppt/pptx | 服务端转 HTML（`GET .../preview-html`），无需外网 |
| 3.3 | 下载 | `download` 接口正常 |

## 4. Agent 运行时（可选）

| # | 步骤 | 预期 |
|---|------|------|
| 4.1 | `GET /api/agent/runtime/status` | `activeProvider` 为 builtin（或已配置的 OpenClaw/Hermes） |
| 4.2 | 外部 Agent 不可达 | 分析仍完成（回退 builtin） |

## 自动化对应用例（v2.4）

| 清单项 | 自动化 |
|--------|--------|
| 1.1–1.2 | `npm run test:e2e:api`（API）；`npm run test:e2e:ui` 工作台卷宗面板可见 |
| 1.3b+ | 手工 + 可选 `E2E_RUN_INTAKE=1` → `npm run test:e2e:nightly` |
| 全站 API | `npm run test:e2e:regression` |

## 回归注意

- **不自动建案**：未匹配不得创建 `Case` 记录。
- 立案审批通过后：默认自动草稿 + 挂接卷宗；主任同意后可弹窗「完善案件草稿」；申请人在「我发起的」可点「完善立案」。
- 自动化：`mvn test -Dtest=CaseFileIntakeServiceTest,CaseIntakePendingServiceTest`

## 审批 E2E（Sprint 0）

| # | 步骤 | 预期 |
|---|------|------|
| A.1 | 待办 → 转审：选择新审批人 + 原因 | 成功，当前审批人变更 |
| A.2 | 我发起的 → 催办（待审批单） | 成功，审批人收到通知 |
