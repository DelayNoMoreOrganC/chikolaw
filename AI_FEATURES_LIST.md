# AI功能清单

**更新时间**：2026-05-04
**方案**：从Ollama迁移到LLM API（DeepSeek/通义千问）

---

## ✅ 已实现AI功能

| 功能 | 状态 | 实现方式 | 完成度 |
|------|------|---------|--------|
| LLM API客户端 | ✅ 完成 | DeepSeek/通义千问 | 100% |
| OCR文书识别 | ✅ 完成 | DeepSeek Vision API | 100% |
| AI文书生成 | ✅ 完成 | DeepSeek Chat API | 100% |
| AI法律问答 | 🔄 迁移中 | Ollama→LLM API | 50% |
| RAG知识库 | ✅ 可用 | TF-IDF检索 | 60% |
| 案件分析 | ⚠️ 基础 | 关键词匹配 | 40% |
| 类案检索 | ⚠️ 基础 | 文本相似度 | 30% |

---

## 🎉 最新完成（2026-05-04）

### ✅ LLM API基础集成（完成）

**实现内容**:
1. ✅ 创建LLM API客户端服务 (LLMApiService)
   - 支持DeepSeek Chat API
   - 支持DeepSeek Vision API
   - 支持通义千问API
   - 支持Ollama本地模型
   - 实现超时和重试机制（指数退避）
   - 完善的错误处理和日志

2. ✅ 配置LLM API参数
   - application.yml中添加LLM配置
   - 支持环境变量存储API密钥
   - 创建LLMProperties配置类
   - 配置RestTemplate超时

3. ✅ 迁移OCR识别功能
   - 使用DeepSeek Vision API替代Ollama
   - 支持图片格式OCR
   - 保持原有接口兼容
   - 自动选择OCR提供商

4. ✅ 创建AI配置管理接口
   - API密钥配置管理
   - 模型参数配置
   - 测试API连接接口
   - 获取可用提供商列表
   - 获取配置建议

**技术特点**:
- 使用RestTemplate调用HTTP API
- 指数退避重试机制（1s, 2s, 4s）
- 连接超时: 10秒（可配置）
- 读取超时: 30秒（可配置）
- 重试次数: 3次（可配置）
- API密钥优先级: 环境变量 > 配置文件 > 数据库
- 不硬编码API密钥

**相关文件**:
- `backend/src/main/java/com/lawfirm/service/LLMApiService.java`
- `backend/src/main/java/com/lawfirm/config/LLMProperties.java`
- `backend/src/main/java/com/lawfirm/config/RestTemplateConfig.java`
- `backend/src/main/java/com/lawfirm/controller/AIConfigController.java`
- `backend/src/main/resources/application.yml`
- `backend/.env.example`
- `LLM_API_INTEGRATION_GUIDE.md`

### ✅ AI文书生成功能迁移（完成）

**实现内容**:
1. ✅ 创建文书生成服务 (DocumentGenerationService)
   - 使用DeepSeek Chat API生成文书
   - 支持4种文书类型：起诉状、答辩状、代理词、法律意见书
   - 为每种文书类型定制专业的系统提示词
   - 动态构建用户消息

2. ✅ 创建文书生成DTO (DocumentGenerateRequest)
   - 支持案件信息
   - 支持当事人信息（个人/公司）
   - 支持文书特定内容（诉讼请求、事实与理由等）

3. ✅ 创建文书生成Controller (DocumentGenerationController)
   - POST /ai/documents/generate - 生成法律文书
   - GET /ai/documents/types - 获取文书类型列表
   - 完善的请求验证

4. ✅ 完善的错误处理和日志
   - AI操作日志记录
   - 错误信息友好化
   - 性能指标记录

**技术特点**:
- 使用LLMApiService统一调用API
- 专业的Prompt模板设计
- 灵活的数据结构
- 完整的请求验证
- 支持自定义Prompt

**相关文件**:
- `backend/src/main/java/com/lawfirm/service/DocumentGenerationService.java`
- `backend/src/main/java/com/lawfirm/dto/DocumentGenerateRequest.java`
- `backend/src/main/java/com/lawfirm/controller/DocumentGenerationController.java`
- `AI_DOCUMENT_GENERATION_MIGRATION_REPORT.md`
- `AI_DOCUMENT_GENERATION_CONFIG_GUIDE.md`

---

## 🔄 正在迁移的功能

### 1. AI法律问答（迁移优先级：P1）
**当前**：Ollama + TF-IDF
**目标**：DeepSeek Chat + RAG

### 3. AI法律问答（迁移优先级：P1）
**当前**：Ollama + TF-IDF
**目标**：DeepSeek Chat + RAG

```yaml
流程:
  1. 用户提问
  2. TF-IDF检索相关法律条文
  3. 结合上下文调用LLM API
  4. 返回答案+引用
```

---

## 📋 待实现AI功能

| 功能 | 优先级 | 预估工期 |
|------|--------|---------|
| 智能提醒 | P1 | 3天 |
| 案件要素提取 | P1 | 2天 |
| 风险评估 | P2 | 5天 |
| 类案推荐 | P2 | 3天 |
| 文档摘要 | P2 | 2天 |
| 争议焦点分析 | P2 | 3天 |

---

## 🔧 LLM API配置方案

```yaml
# application.yml
llm:
  # DeepSeek配置
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com
    chat-model: deepseek-chat
    vision-model: deepseek-vl
    max-tokens: 4000
    temperature: 0.7
  
  # 通义千问备用
  qwen:
    api-key: ${QWEN_API_KEY}
    base-url: https://dashscope.aliyuncs.com
    model: qwen-plus
    
  # 超时配置
  timeout: 30000
  retry: 3
```

---

## 📊 AI功能使用场景

### 场景1：立案阶段
- **OCR识别**：上传起诉状→提取要素
- **信息填充**：自动填入案件信息
- **利益冲突检查**：自动检索客户库

### 场景2：办案阶段
- **文书生成**：根据案情生成各类文书
- **类案检索**：相似案件推荐
- **法规查询**：相关法律条文查询

### 场景3：结案阶段
- **文书整理**：一键生成归档PDF
- **案件总结**：自动生成办案总结
- **数据分析**：案件数据统计分析

---

## 🎯 迁移计划

### 阶段1：API封装（3天）
- [ ] 创建LLM API客户端
- [ ] 实现DeepSeek API调用
- [ ] 添加通义千问备用
- [ ] 统一AI服务接口

### 阶段2：功能迁移（5天）
- [ ] OCR识别迁移到Vision API
- [ ] 文书生成迁移到Chat API
- [ ] 法律问答迁移到Chat API
- [ ] RAG知识库整合

### 阶段3：测试验证（2天）
- [ ] 功能回归测试
- [ ] 性能测试
- [ ] 成本评估

---

## 💰 成本估算

| 功能 | 月调用量 | 单次成本 | 月成本 |
|------|---------|---------|--------|
| OCR识别 | 1000次 | ¥0.02 | ¥20 |
| 文书生成 | 500次 | ¥0.05 | ¥25 |
| 法律问答 | 2000次 | ¥0.01 | ¥20 |
| **合计** | - | - | **¥65/月** |

---

## ✅ 下一步行动

1. 创建LLM API客户端服务
2. 迁移OCR识别功能
3. 迁移文书生成功能
4. 完善法律问答功能
