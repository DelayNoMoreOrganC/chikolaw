# LLM API集成功能说明

## 概述

本项目已成功实现从Ollama迁移到LLM API（DeepSeek/通义千问）的功能，支持多种AI服务提供商。

## 已实现功能

### 1. LLM API客户端服务 (LLMApiService)

**位置**: `backend/src/main/java/com/lawfirm/service/LLMApiService.java`

**功能特性**:
- 支持DeepSeek API（聊天+视觉）
- 支持通义千问API
- 支持OpenAI API（预留）
- 支持Ollama本地模型
- 自动重试机制（指数退避）
- 超时配置
- 错误处理和日志记录

**核心方法**:
```java
// DeepSeek聊天接口
String chatWithDeepSeek(String prompt, String systemPrompt)

// DeepSeek视觉接口（图片识别）
String visionWithDeepSeek(String prompt, String imageBase64)

// 通义千问聊天接口
String chatWithQwen(String prompt, String systemPrompt)

// 使用配置的AI服务
String chatWithConfig(String prompt, AIConfig config)

// 测试API连接
boolean testConnection(String provider, AIConfig config)
```

### 2. LLM配置管理

**配置文件**: `backend/src/main/resources/application.yml`

```yaml
llm:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:your-deepseek-api-key}
    base-url: https://api.deepseek.com
    chat-model: deepseek-chat
    vision-model: deepseek-vl
    max-tokens: 4000
    temperature: 0.7
  qwen:
    api-key: ${QWEN_API_KEY:your-qwen-api-key}
    base-url: https://dashscope.aliyuncs.com
    model: qwen-plus
  timeout: 30000
  retry: 3
```

**配置属性类**: `backend/src/main/java/com/lawfirm/config/LLMProperties.java`

### 3. OCR识别功能迁移

**位置**: `backend/src/main/java/com/lawfirm/service/AIDocumentService.java`

**新增功能**:
- `performDeepSeekVisionOCR()`: 使用DeepSeek Vision API进行图片OCR识别
- 支持PDF文本层提取
- 支持图片格式OCR
- 自动选择OCR提供商（tesseract/deepseek/baidu/aliyun）

**使用示例**:
```java
// 在application.yml中配置OCR提供商
ai:
  ocr:
    provider: deepseek  # 使用DeepSeek Vision API
```

### 4. AI配置管理接口

**位置**: `backend/src/main/java/com/lawfirm/controller/AIConfigController.java`

**新增接口**:

| 接口 | 方法 | 说明 |
|------|------|------|
| `/ai/config/test/{id}` | POST | 测试AI API连接 |
| `/ai/config/providers` | GET | 获取可用的AI提供商列表 |
| `/ai/config/recommendations` | GET | 获取AI功能配置建议 |
| `/ai/config/batch` | POST | 批量导入AI配置 |
| `/ai/config/setDefault/{id}` | PUT | 设置默认AI配置 |
| `/ai/config/toggle/{id}` | PUT | 启用/禁用AI配置 |

**可用的AI提供商**:
1. **DeepSeek**: 性价比高，中文支持好
   - 模型: deepseek-chat, deepseek-coder, deepseek-vl
2. **通义千问**: 企业级AI服务
   - 模型: qwen-turbo, qwen-plus, qwen-max, qwen-vl-max
3. **OpenAI**: GPT系列
   - 模型: gpt-4, gpt-4-turbo, gpt-3.5-turbo, gpt-4-vision-preview
4. **Ollama**: 本地部署的开源大模型
   - 模型: qwen2.5, llama3, mistral, deepseek-coder

### 5. RestTemplate配置

**位置**: `backend/src/main/java/com/lawfirm/config/RestTemplateConfig.java`

**功能**:
- 配置HTTP连接超时
- 配置读取超时
- 支持自定义超时时间

## 配置指南

### 方法1: 使用环境变量（推荐）

1. 复制 `backend/.env.example` 为 `backend/.env`
2. 填入真实的API密钥
3. 启动应用前加载环境变量

**Windows**:
```cmd
set DEEPSEEK_API_KEY=your-actual-api-key
cd backend
mvn spring-boot:run
```

**Linux/Mac**:
```bash
export DEEPSEEK_API_KEY=your-actual-api-key
cd backend
mvn spring-boot:run
```

### 方法2: 直接修改配置文件

直接修改 `application.yml` 中的API密钥（不推荐，有安全风险）

```yaml
llm:
  deepseek:
    api-key: sk-xxxxxxxxxxxxxxxxxxxxx  # 替换为真实密钥
```

### 方法3: 通过数据库配置

1. 启动应用
2. 调用 `/ai/config` 接口创建AI配置
3. 设置为默认配置

## API密钥获取

### DeepSeek API
1. 访问: https://platform.deepseek.com/
2. 注册/登录账号
3. 进入API Keys页面
4. 创建新的API密钥

### 通义千问API
1. 访问: https://dashscope.console.aliyun.com/
2. 登录阿里云账号
3. 开通灵积平台服务
4. 创建API-KEY

## 使用示例

### OCR识别示例

```bash
# 上传图片进行OCR识别
curl -X POST http://localhost:8080/api/ai/document/recognize \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@legal_document.jpg" \
  -F "userId=1" \
  -F "caseId=1"
```

### 测试API连接

```bash
# 测试AI配置连接
curl -X POST http://localhost:8080/api/ai/config/test/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 获取推荐配置

```bash
curl -X GET http://localhost:8080/api/ai/config/recommendations \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 技术实现细节

### 超时和重试机制

- **连接超时**: 10秒（可配置）
- **读取超时**: 30秒（可配置）
- **重试次数**: 3次（可配置）
- **退避策略**: 指数退避（1s, 2s, 4s）

### API调用流程

1. 获取API密钥（优先级：环境变量 > 配置文件 > 数据库）
2. 构建请求体
3. 设置请求头
4. 发送HTTP请求
5. 解析响应
6. 失败重试（指数退避）
7. 返回结果

### 错误处理

- API密钥未配置
- 网络连接失败
- API调用失败
- 响应解析失败
- 超时

所有错误都会记录详细日志，便于排查问题。

## 验收标准

✅ **LLMApiService可以成功调用DeepSeek API**
- 已实现聊天接口和视觉接口
- 支持系统提示词
- 正确解析API响应

✅ **配置文件正确读取API密钥**
- 支持环境变量
- 支持配置文件
- 支持数据库配置
- 优先级正确

✅ **OCR识别功能使用新API**
- 新增DeepSeek Vision OCR
- 保持原有接口兼容
- 支持PDF和图片格式

✅ **有基本的错误处理和日志**
- 完整的异常处理
- 详细的日志记录
- 重试机制
- 超时配置

✅ **RestTemplate配置超时**
- 连接超时配置
- 读取超时配置
- 可自定义

✅ **API密钥不硬编码**
- 使用环境变量
- 配置外部化
- 安全性保障

## 后续优化建议

1. **API密钥加密存储**
   - 使用Jasypt加密敏感配置
   - 密钥轮换机制

2. **缓存机制**
   - 缓存API响应
   - 减少重复调用
   - 降低成本

3. **监控和告警**
   - API调用次数统计
   - 成本监控
   - 失败率告警

4. **更多模型支持**
   - OpenAI GPT-4
   - Claude API
   - 文心一言

5. **流式响应**
   - 支持SSE流式输出
   - 提升用户体验

## 相关文档

- [AI功能清单](AI_FEATURES_LIST.md)
- [DeepSeek API文档](https://platform.deepseek.com/api-docs/)
- [通义千问API文档](https://help.aliyun.com/zh/dashscope/developer-reference/api-details)

## 技术支持

如有问题，请查看：
1. 应用日志: `logs/lawfirm-backend.log`
2. 控制台输出
3. API响应错误信息

---

**实现时间**: 2026-05-04
**实现人员**: Agent 2 (Developer)
**状态**: ✅ 已完成并编译通过
