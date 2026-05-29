# 律所管理系统 - 当前状态

**更新时间**：2026-05-28  
**版本**：v2.4.0（PRD 文档 v2.4）  
**状态**：🟢 v2.4 AI 主线已交付  

> 需求基准：[PRD.md](PRD.md) v2.4  

---

## v2.4 要点摘要（AI 主线）

| 维度 | 说明 |
|------|------|
| AI Hub 向导 | [`AiUnifiedWizard`](frontend/src/components/AiUnifiedWizard.vue) 三步：上传 → 意图 → 结果 |
| 路由参数 | `/ai-hub?intent=docGen\|intake\|recognize&caseId=` |
| docx 导出 | [`useDocumentExport`](frontend/src/composables/useDocumentExport.js) — AI 中心 + 案件文档 Tab |
| 入口去重 | 案件 doc「智能识别」跳转 AI Hub；中心内重复 OCR 上传区已移除 |
| 错误体验 | [`aiError.js`](frontend/src/utils/aiError.js) 统一 GLM/配置类提示 |
| 测试 | `npm run test:e2e:regression` / `test:e2e:ui`；GLM 全链路 `E2E_RUN_INTAKE=1 npm run test:e2e:nightly` |

---

## v2.3 基线（仍有效）

- 工作台周视图、卷宗四意图、立案审批→确认建案、案件↔日程同步、列表 `quickFilter`

---

## 已知差距（v2.5+）

| 优先级 | 项 |
|--------|-----|
| P2 | 工作台嵌入月/日视图 |
| P2 | 卷宗阶段目录实体化（`initStageFolders`） |
| P2 | 分片上传前端、WebSocket 通知 |
| v3.0 | 智谱 Embedding 统一 |

---

## 生产部署

腾讯云轻量服务器（Ubuntu 24.04，Nginx + systemd）：见 [docs/DEPLOY_TENCENT_CLOUD.md](docs/DEPLOY_TENCENT_CLOUD.md)。  
一键更新：`sudo bash scripts/deploy-native.sh`

---

## 测试

```bash
cd backend && mvn test
cd frontend && npm run build
cd frontend && npm run test:e2e:regression   # 需后端
cd frontend && npm run test:e2e:ui           # 需前后端
# GLM 卷宗全链路（nightly）：
#   $env:E2E_RUN_INTAKE=1; npm run test:e2e:nightly
```

---

*与 [PRD.md](PRD.md) 附录 A 同步*
