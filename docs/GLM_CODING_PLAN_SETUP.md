# 智谱 GLM Coding Plan 接入说明

律所系统后端通过 **OpenAI 兼容 HTTP API** 调用 GLM，不使用 Cursor 侧的 MCP Server（MCP 仅适用于 Claude Code / Cline 等 IDE）。

## 必配项

1. 复制 `backend/.env.example` 为 `backend/.env`
2. 填入 [智谱开放平台](https://open.bigmodel.cn/) 的 **Coding Plan API Key**
3. 端点必须为 Coding 专属（勿用通用 paas/v4）：

   `https://open.bigmodel.cn/api/coding/paas/v4`

4. 重启后端（`scripts/start-dev.ps1` 会自动加载 `backend/.env`）

## 环境变量

| 变量 | 说明 |
|------|------|
| `ZHIPU_API_KEY` | Coding Plan API Key（必填） |
| `ZHIPU_BASE_URL` | 默认已指向 coding 端点 |
| `GLM_CHAT_MODEL` | 默认 `glm-4.7` |
| `GLM_VISION_MODEL` | 卷宗 PDF/图片 OCR，默认 `glm-4.6v` |
| `LLM_FALLBACK_ENABLED` | 纯线上建议 `false` |
| `AI_OCR_PROVIDER` | 建议 `zhipu` |
| `LAWFIRM_AI_MODE` | v2.2 固定 `cloud-glm`；v3.0 本地用 `hybrid` |

## 能力范围

| 功能 | 实现方式 |
|------|----------|
| 卷宗录入 OCR | `ai.ocr.provider=zhipu` → GLM 视觉 |
| 文书要素抽取 | `llm.routing.*=zhipu` |
| 法律对话 / RAG | 同上 |
| IDE 视觉 MCP | 见 [视觉理解 MCP 文档](https://docs.bigmodel.cn/cn/coding-plan/mcp/vision-mcp-server)（与本系统独立） |

## 验证

登录后访问 `GET /api/agent/runtime/status`，`activeProvider` 应为 `builtin`（内置链路已切到智谱 OCR+LLM）。

上传判决书 PDF 测试卷宗录入；未匹配案件时应出现「未匹配」提示及 `pendingId`（需 `case_intake_pending` 表正常）。

更多 v2.2 策略见 [AI_V2.2_GLM.md](./AI_V2.2_GLM.md)。
