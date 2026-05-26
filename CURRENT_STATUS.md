# 律所管理系统 - 当前状态

**更新时间**：2026-05-26  
**版本**：v2.2.0  
**状态**：🟡 功能完备，待生产验收

> 需求基准：[PRD.md](PRD.md) v2.0（附录 A 实现状态矩阵）  
> 卷宗录入验收：[docs/INTAKE_FLOW_E2E_CHECKLIST.md](docs/INTAKE_FLOW_E2E_CHECKLIST.md)  
> 行政合规映射：[docs/ADMIN_CASE_CLIENT_REQUIREMENTS.md](docs/ADMIN_CASE_CLIENT_REQUIREMENTS.md)

---

## 总体完成度

| 层级 | 状态 |
|------|------|
| P0/P1 核心（案件/客户/财务/审批/行政/系统） | 附录 A：**已实现** |
| P2 扩展（17 个一级导航） | **已上线** |
| 卷宗智能录入 + 立案审批桥接 | **已实现** + 单元测试 |
| 客户 Excel 导入 + 利冲审查 | **已实现** |
| AI 模型路由 / 诊断 / Agent 运行时 | **已实现** |
| 类案语义检索 / 案件 LLM 分析 | **可配置启用**（`case-search.semantic.enabled`） |

---

## 近期交付（Sprint 0~2）

### 卷宗智能录入
- 工作台 `CaseFileIntakePanel` → `POST /api/case-intake/process`
- 未匹配：`NEEDS_CASE` + `pendingId`；挂接 `attach-pending`；立案 `CASE_FILING` 审批通过 → **自动待立案草稿** + 卷宗挂接 → 引导完善
- **不自动建案**（回归约束）

### 审批与工作流
- 自定义流程：`ApprovalWorkflowService` + 设置页
- 发起类型与后端枚举对齐（含立案/开票/采购/证照）

### 客户行政合规
- `POST /api/clients/import` Excel 批量导入（导入前逐行利冲）
- 非管理员不可改客户名称、不可改案源人/负责人

### AI 能力
- 7 场景模型路由 + `GET /api/ai/diagnostics`
- `GET /api/cases/{id}/ai-analysis` LLM 案件分析
- 类案检索：文本加权 + 可选 Embedding 语义增强
- Agent：`GET /api/agent/runtime/status`；生产配置见 `application-prod.yml`
- OCR 遗留路径已标注 `@Deprecated`，统一 `/api/ai/documents/recognize`
- 案件详情页「案件分析」按钮 → `GET /api/cases/{id}/ai-analysis`

---

## 已知差距（按优先级）

| 优先级 | 项 |
|--------|-----|
| P1 | 立案审批通过后自动草稿案件（`PENDING_FILING`）+ 完善引导 |
| P2 | 审批转审/催办 | 转审选人对话框 + 催办按钮已接入 |
| P2 | Office 文档在线预览 |
| — | **消息通知中心** | 顶栏铃铛 + 抽屉分类（待办/案件/审批）+ 审批待办/结果/催办推送 |
| P3 | AI 文书术语与 UI 别名统一 |

---

## 快速启动

```bash
# 后端
cd backend && mvn spring-boot:run

# 前端
cd frontend && npm run dev
```

- 后端：http://localhost:8080/api  
- 前端：http://localhost:3017  
- 默认账号：`admin` / `admin123`

**生产环境变量**：`JWT_SECRET`、`CRYPTO_SECRET_KEY`、`LAWFIRM_DB_*`、`QDRANT_*`、`AGENT_PROVIDER`、`HERMES_*`、`CASE_SEARCH_SEMANTIC_ENABLED`

---

## 测试

```bash
cd backend && mvn test
```

含 `CaseFileIntakeServiceTest`、`CaseIntakePendingServiceTest`、`ApprovalWorkflowServiceTest`、`CaseFlowDefinitionServiceTest` 等。

---

*与 [PRD.md](PRD.md) 附录 A 同步；历史进度见 [archive/reports/](archive/reports/)*
