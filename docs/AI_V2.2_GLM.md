# v2.2 AI 策略：全量智谱 GLM

## 运行模式

| 模式 | 环境变量 | 说明 |
|------|----------|------|
| **cloud-glm**（默认） | `LAWFIRM_AI_MODE=cloud-glm` | 所有 `llm.routing.*` 强制走 `zhipu`；禁用 LLM 降级到 DeepSeek |
| **hybrid**（v3.0 预留） | `LAWFIRM_AI_MODE=hybrid` | 按 `llm.routing.*` 与 `ai_config` 使用 lmstudio/ollama 等，可开 `LLM_FALLBACK_ENABLED` |

## v2.2 已统一 GLM 的能力

- 卷宗智能录入（Vision OCR + 要素抽取）
- 文书智能识别（`/ai/documents/recognize`）
- AI 对话、案件分析、文书生成、法律聊天、RAG 问答生成

## 仍为阿里云（非 GLM Chat）

- **向量嵌入** `text-embedding-v3`：类案检索 / 知识库 RAG 召回（v3.0 可评估智谱 embedding）

## 前端入口（v2.2）

| 入口 | 用途 |
|------|------|
| 工作台 · 卷宗智能录入 | 归入已有案件卷宗 |
| AI 智能中心 `/ai-hub` | 识别、建案预填、文书生成、待办自动化 |
| 新建案件 · 文书智能识别填充 | 预填表单 |

工作台已移除重复的「AI 智能创建」上传区，避免与卷宗录入混淆。

## v3.0 切换本地模型示例

```env
LAWFIRM_AI_MODE=hybrid
LLM_FALLBACK_ENABLED=true
LLM_FALLBACK_PROVIDER=zhipu
LLM_ROUTE_DOCUMENT=lmstudio
LMSTUDIO_BASE_URL=http://127.0.0.1:1234
LMSTUDIO_MODEL=your-local-model
```

## 诊断

`GET /api/ai/diagnostics` 返回 `lawfirmAiMode`、`cloudGlm`、`ocrProvider`、各场景路由解析结果。
