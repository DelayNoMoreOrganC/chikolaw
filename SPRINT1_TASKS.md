# Sprint 1 任务清单

**时间**：2026-05-04 ~ 2026-05-18（2周）
**目标**：完善案件管理核心功能 + LLM API集成

---

## 📋 任务列表

### Task 1: 主办/协办部门自动关联
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：2天

**需求**：
- 根据选择的律师ID，自动查询其所属部门
- 支持多律师对应多部门
- 自动填充hostDepartment和coDepartments

**API设计**：
```
GET /api/departments/by-lawyer/{lawyerId}
Response: ["诉讼一部", "行政部"]
```

**验收**：
- [ ] Service层实现
- [ ] Controller接口
- [ ] 单元测试通过

---

### Task 2: 部门关联UI集成
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：1天
**依赖**：Task 1

**需求**：
- 选择律师后，自动调用部门API
- 自动填充部门下拉框
- 支持手动修改

**验收**：
- [ ] 前端调用API
- [ ] 自动填充逻辑
- [ ] 用户可覆盖

---

### Task 3: 风险代理必填验证
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：1天

**需求**：
- 当feeTypes包含"风险代理"时
- disputedAmount字段必填
- 自定义验证器@RequireIfRiskProxy

**验收**：
- [ ] 验证器实现
- [ ] 集成到Case实体
- [ ] 测试用例通过

---

### Task 4: 合同到期提醒服务
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：2天

**需求**：
- 顾问类案件到期前1个月提醒
- 定时任务扫描
- 创建待办事项

**实现**：
```java
@Scheduled(cron = "0 0 9 * * ?")
public void checkContractExpiry()
```

**验收**：
- [ ] 定时任务
- [ ] 提醒创建逻辑
- [ ] 测试数据验证

---

### Task 5: 开庭日期提醒
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：1天

**需求**：
- 开庭前3天提醒
- 开庭当天提醒
- 创建待办事项

**验收**：
- [ ] 提醒逻辑
- [ ] 测试用例通过

---

### Task 6: 提醒通知UI
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：1天
**依赖**：Task 4, 5

**需求**：
- 待办列表显示提醒
- 高亮显示紧急项
- 支持标记完成

**验收**：
- [ ] 待办列表更新
- [ ] 高亮样式
- [ ] 完成操作

---

### Task 7: LLM API集成（替代Ollama）
**开发者**：Agent 2
**复核者**：Agent 3
**优先级**：P1
**估时**：2天

**需求**：
- 集成DeepSeek API（或通义千问）
- 实现OCR识别（Vision API）
- 实现AI文书生成
- 实现AI法律问答

**API配置**：
```yaml
llm:
  api-key: ${DEEPSEEK_API_KEY}
  base-url: https://api.deepseek.com
  model: deepseek-chat
  vision-model: deepseek-vl
```

**验收**：
- [ ] API调用封装
- [ ] OCR识别功能
- [ ] 文书生成功能
- [ ] 法律问答功能
- [ ] 测试用例通过

---

## 🔄 任务依赖图

```
Task 1 (部门API) ────────┐
    ↓                    │
Task 2 (UI集成)          │
                         │
Task 4 (合同提醒) ───────┤
    ↓                    │
Task 6 (提醒UI) ←────────┘
    ↑
Task 5 (开庭提醒)

Task 3 (验证器) [独立]

Task 7 (LLM API) [独立]
```

---

## 📅 进度安排

| 日期 | 任务 | 执行 | 复核 |
|------|------|------|------|
| Day 1-2 | Task 1 | Agent 2 | Agent 3 |
| Day 1 | Task 3 | Agent 2 | Agent 3 |
| Day 2-3 | Task 4 | Agent 2 | Agent 3 |
| Day 3 | Task 5 | Agent 2 | Agent 3 |
| Day 2-3 | Task 2 | Agent 2 | Agent 3 |
| Day 3-4 | Task 6 | Agent 2 | Agent 3 |
| Day 1-3 | Task 7 | Agent 2 | Agent 3 |
| Day 4 | 主Agent验收 | 主Agent | - |

---

## ✅ 验收标准

### 功能验收
- [ ] 部门自动关联工作正常
- [ ] 风险代理验证生效
- [ ] 到期提醒正确创建
- [ ] UI显示正确
- [ ] LLM API调用成功

### 技术验收
- [ ] 单元测试覆盖率 > 80%
- [ ] API响应时间 < 500ms（非AI接口）
- [ ] AI API响应时间 < 5s
- [ ] 无明显Bug
- [ ] 代码审查通过

---

## 🚀 启动流程

### 1. 主Agent发布任务
```markdown
# 任务发布示例
主Agent分析PRD → 生成任务单 → 分配给Agent 2
```

### 2. Agent 2执行开发
```markdown
# 开发执行示例
接收任务 → 编写代码 → 自测 → 提交交付
```

### 3. Agent 3复核测试
```markdown
# 测试复核示例
代码审查 → API测试 → 端到端测试 → 反馈报告
```

### 4. 主Agent安排调整
```markdown
# 调整示例
分析反馈 → 发布调整任务 → Agent 2修正
```

### 5. 最终验收
```markdown
# 验收示例
回归测试 → 功能验收 → 交付完成
```
