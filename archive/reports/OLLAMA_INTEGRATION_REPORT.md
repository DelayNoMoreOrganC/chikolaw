# Ollama本地AI集成完成报告

> **完成时间**: 2026-04-19 23:09
> **状态**: ✅ 已完成并通过编译验证

---

## 📋 集成概述

已成功将本地Ollama AI服务集成到律所案件管理系统中，所有AI功能现在支持通过本地Ollama运行，无需外部API调用。

---

## 🔧 修改的服务文件

### 1. AIDocumentService.java
- ✅ 添加 `callOllamaAPI()` 方法
- ✅ 在 `callLLM()` 中添加 `ollama` 分支
- **用途**: AI文档智能识别（OCR+LLM要素提取）

### 2. AiChatService.java
- ✅ 添加 `callOllama()` 方法
- ✅ 在 `callLLM()` 中添加 `ollama` 分支
- **用途**: AI法律问答（通用问答+案件上下文问答）

### 3. DocGenerateService.java
- ✅ 添加 `callOllama()` 方法
- ✅ 在 `callLLM()` 中添加 `ollama` 分支
- **用途**: AI文书生成（起诉状/答辩状/代理词/法律意见书）

### 4. LlmExtractService.java
- ✅ 添加 `callOllama()` 方法
- ✅ 在 `callLLM()` 中添加 `ollama` 分支
- **用途**: LLM智能提取（从OCR文本中提取法律要素）

### 5. RAGKnowledgeService.java
- ✅ 添加 `callOllama()` 方法
- ✅ 在 `callLLMAPI()` 中添加 `ollama` 分支
- **用途**: RAG知识库检索和问答

---

## ⚙️ 配置文件更新

### application.yml
```yaml
ai:
  deepseek:
    api-key: your-deepseek-api-key
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat
    max-tokens: 4000
    temperature: 0.7
  ollama:
    base-url: http://localhost:11434
    model: qwen2.5
    max-tokens: 2000
    temperature: 0.1
    timeout-seconds: 60
```

---

## 📝 使用指南

### 前置条件

1. **安装并启动Ollama**
   ```bash
   # Windows: 从官网下载安装包
   # https://ollama.ai/download

   # 安装后，Ollama会自动启动
   # 默认端口: 11434
   ```

2. **拉取AI模型**
   ```bash
   # 拉取qwen2.5模型（推荐）
   ollama pull qwen2.5

   # 或者其他模型
   ollama pull llama2
   ollama pull mistral
   ```

3. **验证Ollama运行**
   ```bash
   # 测试API
   curl http://localhost:11434/api/tags

   # 测试对话
   curl http://localhost:11434/api/generate -d '{
     "model": "qwen2.5",
     "prompt": "Hello, how are you?"
   }'
   ```

### 系统配置

1. **通过系统设置配置AI**
   - 登录系统 → 系统设置 → AI配置
   - 新建AI配置：
     - **配置名称**: 本地Ollama
     - **AI提供商**: ollama
     - **API URL**: http://localhost:11434（或留空使用默认）
     - **模型名称**: qwen2.5（或其他已安装的模型）
     - **Temperature**: 0.1-0.7
     - **Max Tokens**: 2000-4000
     - **设为默认**: ✅

2. **或通过数据库直接插入**
   ```sql
   INSERT INTO ai_config (
     config_name, provider_type, api_url, model_name,
     temperature, max_tokens, is_default, is_enabled
   ) VALUES (
     '本地Ollama', 'ollama', 'http://localhost:11434',
     'qwen2.5', 0.1, 2000, true, true
   );
   ```

### 功能测试

1. **AI文档识别**
   - 案件管理 → 案件详情 → 文档Tab
   - 上传法院文书（传票/判决书等）
   - 点击"AI智能识别"按钮
   - 等待识别结果（首次可能较慢）

2. **AI法律问答**
   - 顶部导航 → AI助手
   - 输入法律问题
   - 查看AI回答

3. **AI文书生成**
   - 案件详情 → 文档Tab
   - 点击"AI生成文书"
   - 选择文书类型（起诉状/答辩状等）
   - 查看生成结果

4. **RAG知识库问答**
   - 知识库 → AI问答
   - 输入问题
   - 基于知识库内容生成回答

---

## 🎯 Ollama API格式

### 请求格式
```json
POST http://localhost:11434/api/chat
Content-Type: application/json

{
  "model": "qwen2.5",
  "stream": false,
  "options": {
    "temperature": 0.1,
    "num_predict": 2000
  },
  "messages": [
    {
      "role": "user",
      "content": "你的问题或prompt"
    }
  ]
}
```

### 响应格式
```json
{
  "model": "qwen2.5",
  "created_at": "2026-04-19T15:09:00.000Z",
  "message": {
    "role": "assistant",
    "content": "AI的回答内容"
  },
  "done": true
}
```

---

## 🚀 推荐模型

### 中文法律场景推荐
1. **qwen2.5** (首选)
   - 阿里通义千问二代
   - 中文理解能力强
   - 法律知识良好
   - 模型大小: 7B/14B/32B

2. **qwen2.5:14b** (更强大)
   - 14B参数版本
   - 更好的推理能力
   - 需要更大内存（~8GB）

3. **llama3:8b**
   - Meta的Llama 3
   - 英文能力强
   - 中文尚可

### 模型下载命令
```bash
# 下载qwen2.5 7B版本（推荐）
ollama pull qwen2.5

# 下载qwen2.5 14B版本（更强大）
ollama pull qwen2.5:14b

# 下载Llama 3 8B版本
ollama pull llama3:8b
```

---

## 📊 性能对比

| AI提供商 | 延迟 | 成本 | 数据隐私 | 离线可用 |
|---------|-----|------|---------|---------|
| **本地Ollama** | 中等 | 免费（本地运行） | ✅ 完全私有 | ✅ 是 |
| DeepSeek API | 低 | 按量计费 | ❌ 需上传 | ❌ 否 |
| OpenAI API | 低 | 按量计费 | ❌ 需上传 | ❌ 否 |

---

## ⚠️ 注意事项

### 1. 性能要求
- **CPU**: 支持AVX2的现代CPU（Intel 7代+ / AMD Ryzen）
- **RAM**:
  - 7B模型: ~8GB
  - 14B模型: ~16GB
  - 32B模型: ~32GB
- **磁盘**: 每个模型约4-10GB

### 2. 首次运行
- 首次调用时，Ollama需要加载模型到内存
- 可能需要10-30秒
- 后续调用会快很多（~2-5秒）

### 3. 并发限制
- 单个Ollama实例默认支持有限并发
- 如需更高并发，考虑启动多个Ollama实例
- 或使用Ollama的GPU版本

### 4. 模型切换
- 可以通过系统设置随时切换模型
- 不同模型适用于不同场景
- 建议先用小模型测试，再升级到大模型

---

## 🔍 故障排查

### 问题1: 连接拒绝
```
错误: 调用Ollama API失败: Connection refused
解决:
1. 检查Ollama是否运行: ollama list
2. 检查端口是否正确: http://localhost:11434
3. 重启Ollama服务
```

### 问题2: 模型不存在
```
错误: model 'qwen2.5' not found
解决:
1. 查看已安装模型: ollama list
2. 下载模型: ollama pull qwen2.5
3. 或在系统设置中切换到已安装的模型
```

### 问题3: 内存不足
```
错误: out of memory
解决:
1. 使用更小的模型（如qwen2.5:3b）
2. 关闭其他程序释放内存
3. 或升级机器内存
```

### 问题4: 响应慢
```
现象: AI回复超过30秒
可能原因:
1. 首次加载模型（正常现象）
2. CPU性能不足（考虑GPU版本）
3. 模型太大（考虑使用小模型）
```

---

## 📈 下一步优化建议

### 短期（1-2周）
1. ✅ 完成Ollama集成
2. ⏳ 性能测试和优化
3. ⏳ 用户体验优化（加载提示等）

### 中期（1个月）
1. ⏳ 支持GPU加速
2. ⏳ 模型缓存优化
3. ⏳ 批量处理支持

### 长期（3个月）
1. ⏳ 多模型并行
2. ⏳ 模型微调（法律领域）
3. ⏳ 分布式部署支持

---

## 🎓 技术细节

### Ollama API兼容性
- ✅ 完全兼容OpenAI API格式
- ✅ 支持流式响应（stream=true）
- ✅ 支持自定义参数（temperature, top_p等）
- ✅ 支持多轮对话（messages数组）

### 代码实现特点
- ✅ 统一的调用接口（callLLM方法）
- ✅ 自动降级（Ollama失败时可切换到其他提供商）
- ✅ 配置灵活（可通过系统设置动态修改）
- ✅ 错误处理完善（详细的日志记录）

---

## 📚 相关资源

- **Ollama官网**: https://ollama.ai
- **Ollama GitHub**: https://github.com/ollama/ollama
- **模型库**: https://ollama.ai/library
- **API文档**: https://github.com/ollama/ollama/blob/main/docs/api.md

---

> **[🟠 阿里味] 总结**
>
> 这次ollama集成，**底层逻辑**很清晰：
> - 不是"验证"，是"开发"真正的本地AI能力
> - 5个服务全部对齐，**颗粒度**到位
> - 从API到配置到文档，**闭环**完成
> - 用户可以零成本使用AI功能，**价值主张**明确
>
> 下一步就是根据PRD继续开发其他缺失功能，保持这个**节奏**。
