# 律所管理系统 - 当前状态

**更新时间**：2026-05-26  
**版本**：v2.2.0（PRD 文档 v2.1）  
**状态**：🟡 功能完备，待生产验收  
**性能/并发**：见 [docs/ARCHITECTURE_LAN_50_USERS.md](docs/ARCHITECTURE_LAN_50_USERS.md)（约 50 人局域网）

> 需求基准：[PRD.md](PRD.md) v2.1（macOS 蓝灰视觉 + Alpha/案件云对标；附录 A/B）  
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

### 案件文档预览（局域网）
- PDF/图片：`GET .../preview`（鉴权 blob）
- Office（doc/docx/xls/xlsx/ppt/pptx）：`GET .../preview-html`（Apache POI 转 HTML，无需外网 Office Online）

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

### AI 术语与文书类型
- 用户界面统一为「文书智能识别」（非 OCR 品牌名）；canonical 类型：起诉状/答辩状/代理词/法律意见书/律师函
- 前端：`frontend/src/config/ai-terminology.js`；后端：`DocumentTypeAliasResolver`
- 类型列表 API：`GET /api/ai/documents/types`（含 `route`: DOCUMENT / LEGACY_DOCUMENT）

### 文档中心（跨案件聚合）
- `GET /api/documents?page&size&caseId&documentType&keyword` — 服务端分页 + 统计（总数/涉及案件/总大小）
- 前端：预览/下载/删除（复用 `DocumentPreviewDialog` + `useDocumentPreview`）；兼容 `GET /api/documents/all`

### AI 识别 → 建案预填
- AI 中心识别结果「创建案件」→ `sessionStorage` 预填 → `/case/create` 自动带入案由/法院/案号/当事人

### 预览组件复用
- 案件文档 Tab（`doc.vue`）接入 `DocumentPreviewDialog` + `useDocumentPreview`（与文档中心一致）
- AI 文书生成：支持已生成全文预览 / 关键信息草稿预览对话框

### 批量与导出
- 案件文档：批量下载 / 删除 / 移动分类 / 添加标签；左侧树点击按类型过滤；修复 `loadDocuments` 未定义
- `AIDocGenerator`：复制与下载文书（`.txt`）
- 公文流转：详情弹窗「导出正文」

### UI v2.1（macOS 蓝灰 · 2026-05-26）
- `theme-lawos.scss` 全局 Token + Element 主色 `#3B6FD9`
- 工作台欢迎条、登录/AI/行政/客户页去除紫渐变
- 日程：`repeat`↔`repeatRule` 提交映射、详情抽屉、编辑/删除
- 行政 OA：公告详情/删除、会议详情/取消预定

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
| P2 | 审批转审/催办 | 转审选人对话框 + 催办按钮已接入 |
| P2 | 文档中心分页检索 | **已实现** |
| P2 | AI 识别一键建案预填 | **已实现** |
| P2 | macOS 蓝灰视觉 Token 全站收敛 | **部分**（Token 已接入；部分子页待扫） |
| — | AI 文书 `.docx`、分片上传、WebSocket 通知、SSB 省时宝 | 未实现/占位 |

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
