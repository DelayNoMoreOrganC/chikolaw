# AI文书生成功能配置指南

## 配置DeepSeek API

### 方法1：环境变量配置（推荐）

在系统环境变量中设置：

```bash
# Windows
setx DEEPSEEK_API_KEY "your-deepseek-api-key"

# Linux/Mac
export DEEPSEEK_API_KEY="your-deepseek-api-key"
```

### 方法2：application.yml配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
llm:
  deepseek:
    api-key: your-deepseek-api-key
    base-url: https://api.deepseek.com
    chat-model: deepseek-chat
    vision-model: deepseek-vl
    max-tokens: 4000
    temperature: 0.7
  timeout: 30000
  retry: 3
```

### 方法3：运行时配置

通过系统设置的AI配置管理接口设置：

1. 登录系统
2. 进入"系统设置" → "AI配置"
3. 配置DeepSeek API密钥
4. 测试连接

## 获取DeepSeek API密钥

1. 访问 DeepSeek 官网：https://platform.deepseek.com/
2. 注册账号并登录
3. 进入"API Keys"页面
4. 创建新的API密钥
5. 复制密钥并保存到安全位置

## 启动后端服务

```bash
cd D:\ZGAI\backend
mvn spring-boot:run
```

## 测试API

### 1. 测试获取文书类型列表

```bash
curl -X GET http://localhost:8080/api/ai/documents/types \
  -H "Authorization: Bearer <your-token>"
```

### 2. 测试生成起诉状

```bash
curl -X POST http://localhost:8080/api/ai/documents/generate \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "caseId": 1,
    "documentType": "COMPLAINT",
    "plaintiff": {
      "name": "张三",
      "type": "PERSON",
      "gender": "男",
      "idCard": "110101198001011234",
      "address": "北京市朝阳区xx街道xx号",
      "phone": "13800138000"
    },
    "defendant": {
      "name": "李四",
      "type": "PERSON",
      "address": "北京市海淀区xx街道xx号",
      "phone": "13900139000"
    },
    "claims": "请求判令被告偿还借款本金人民币10万元",
    "factsAndReasons": "被告于2023年1月1日向原告借款10万元，约定年利率6%，借款期限一年。借款到期后，被告未能按期归还。",
    "evidenceList": "1. 借条原件；2. 银行转账记录；3. 微信聊天记录"
  }'
```

## 常见问题

### Q1: API密钥未配置错误

**错误信息**: "DeepSeek API密钥未配置"

**解决方案**:
1. 检查环境变量是否正确设置
2. 检查application.yml配置
3. 通过系统设置配置API密钥

### Q2: 案件不存在错误

**错误信息**: "案件不存在"

**解决方案**:
1. 确认caseId是否正确
2. 检查数据库中是否存在该案件
3. 使用GET /api/cases/{id}确认案件存在

### Q3: 必填字段缺失错误

**错误信息**: "起诉状必须提供原告信息"

**解决方案**:
1. 检查请求参数是否完整
2. 根据文书类型添加必填字段
3. 参考API文档确认必填字段

### Q4: API调用超时

**错误信息**: "API调用超时"

**解决方案**:
1. 检查网络连接
2. 增加超时时间配置
3. 检查DeepSeek API状态

## 性能调优

### 1. 调整温度参数

```yaml
llm:
  deepseek:
    temperature: 0.3  # 降低温度，输出更确定
```

### 2. 调整最大token数

```yaml
llm:
  deepseek:
    max-tokens: 2000  # 减少token数，加快响应
```

### 3. 调整重试次数

```yaml
llm:
  retry: 2  # 减少重试次数
```

## 日志查看

查看AI操作日志：

```sql
SELECT * FROM ai_log
WHERE function_type = '文书生成'
ORDER BY created_at DESC
LIMIT 10;
```

## 成本估算

- **起诉状生成**: 约500-1000 tokens ≈ ¥0.01-0.02
- **答辩状生成**: 约500-1000 tokens ≈ ¥0.01-0.02
- **代理词生成**: 约800-1500 tokens ≈ ¥0.02-0.03
- **法律意见书生成**: 约1000-2000 tokens ≈ ¥0.02-0.04

**月度估算** (假设每月生成100份文书): ¥1-2

## 监控指标

建议监控以下指标：

1. **生成成功率**: 成功数 / 总请求数
2. **平均响应时间**: 从请求到返回的时间
3. **API调用量**: 每日/每月API调用次数
4. **错误率**: 失败数 / 总请求数
5. **用户满意度**: 生成质量评分

## 安全建议

1. **API密钥保护**:
   - 不要在代码中硬编码API密钥
   - 使用环境变量或配置管理工具
   - 定期轮换API密钥

2. **访问控制**:
   - 限制API访问权限
   - 记录所有API调用
   - 实施速率限制

3. **数据安全**:
   - 不要在Prompt中包含敏感信息
   - 加密传输数据
   - 定期审查AI日志
