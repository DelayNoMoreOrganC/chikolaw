# 3-Agent 协同开发结构

**设计理念**：主Agent统筹开发，Agent2执行开发，Agent3复核测试，形成开发闭环。

---

## 🏗️ 架构设计

```
    ┌─────────────────────────────────────────────────┐
    │            主Agent (PM/Architect)                │
    │     根据PRD发布任务 → 安排调整 → 最终验收        │
    └──────────────────┬──────────────────────────────┘
                       │
           ┌───────────┴───────────┐
           ▼                       ▼
    ┌─────────────┐         ┌─────────────┐
    │   Agent 2   │ ───────▶│   Agent 3   │
    │  开发执行   │         │  复核测试   │
    └─────────────┘         └─────────────┘
           ▲                       │
           └───────────────────────┘
               反馈与调整循环
```

---

## 📋 Agent 角色定义

### 主Agent (PM/Architect)
**职责**：
- 分析PRD文档，拆解开发任务
- 发布开发任务给Agent 2
- 接收Agent 3的反馈，安排调整任务
- 最终验收与交付

**输入**：PRD.md、用户需求、Agent 3反馈
**输出**：开发任务单、调整指令、验收报告

### Agent 2 (Developer)
**职责**：
- 执行前端开发（Vue 3 + Element Plus）
- 执行后端开发（Spring Boot + JPA）
- 实现业务逻辑
- 完成AI功能集成（LLM API）

**技术栈**：
- 前端：Vue 3.2+ / Element Plus / Vite / Pinia
- 后端：Spring Boot 2.7 / JPA / JWT
- AI：LLM API（DeepSeek/通义千问等）

### Agent 3 (QA/Reviewer)
**职责**：
- 代码审查
- 功能测试（API测试 + 端到端测试）
- 性能验证
- 问题收集与反馈

**输出**：测试报告、问题清单、改进建议

---

## 🔄 协同流程

```
1. 主Agent分析PRD → 生成开发任务
            ↓
2. 主Agent发布任务 → Agent 2执行开发
            ↓
3. Agent 2提交交付 → Agent 3复核测试
            ↓
4. Agent 3反馈问题 → 主Agent分析反馈
            ↓
5a. 有问题 → 主Agent发布调整任务 → Agent 2修正
            ↓
5b. 无问题 → 主Agent最终验收
            ↓
6. 任务完成，进入下一个迭代
```

---

## 📝 任务单模板

```markdown
# 开发任务：[功能名称]

## 背景（来自PRD）
[PRD章节引用]

## 开发目标
[具体的交付目标]

## 技术要求
- 前端：[组件/页面]
- 后端：[API/Service]

## 验收标准
1. [可测试的标准1]
2. [可测试的标准2]

## AI集成
- API端点：[LLM API]
- 功能：[OCR/问答/生成]

## 参考
- PRD章节：[章节号]
- 示例代码：[文件路径]
```

---

## 🔧 AI集成方案（LLM API）

### API选择
- **DeepSeek API**：主力模型，高性价比
- **通义千问 API**：备用方案
- **API配置**：`application.yml`

### 功能映射
| 功能 | API | 说明 |
|------|-----|------|
| OCR识别 | DeepSeek-Vision | 图片→文本提取 |
| 文书生成 | DeepSeek-Chat | 起诉状/答辩状 |
| 法律问答 | DeepSeek-Chat | 基于知识库问答 |
| 案件分析 | DeepSeek-Chat | 案情要素提取 |

---

## 🎯 当前Sprint任务分配

### Sprint 1：案件管理完善

| 任务 | 开发者 | 复核者 |
|------|--------|--------|
| 部门自动关联 | Agent 2 | Agent 3 |
| 风险代理验证 | Agent 2 | Agent 3 |
| 到期提醒服务 | Agent 2 | Agent 3 |
| LLM API集成 | Agent 2 | Agent 3 |

### 开发节奏
```
Day 1: 主Agent发布任务 → Agent 2开发
Day 2: Agent 3复核测试 → 反馈问题
Day 3: 主Agent安排调整 → Agent 2修正
Day 4: Agent 3回归测试 → 主Agent验收
```

---

## 🔧 使用方法

### 主Agent启动
```bash
# 进入主Agent模式
# 自动加载PRD，分析需求
```

### 发布开发任务
```bash
# 主Agent生成任务单
# 调用Agent工具分配给Agent 2
```

### 复核测试
```bash
# Agent 3执行测试
# 生成测试报告
# 反馈给主Agent
```

### 调整与验收
```bash
# 主Agent分析反馈
# 发布调整任务或最终验收
```

---

## 📊 进度跟踪

使用 `TaskList` 工具跟踪：
- 创建任务：`TaskCreate`
- 更新状态：`TaskUpdate`
- 查看列表：`TaskList`

---

## 🚀 快速开始

1. **P9模式**：`/pua:p9` 启动Tech Lead
2. **拆解需求**：P9分析PRD，生成Task Prompts
3. **分配执行**：P9调用Agent分配给P8
4. **验收交付**：P9审查P8交付成果
