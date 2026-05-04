# AI文书生成功能测试用例

## 测试环境
- 后端服务运行状态: 需要启动
- DeepSeek API配置: 需要配置API密钥
- 测试案件数据: 需要准备测试案件

## 测试用例

### 1. 起诉状生成测试

**请求示例：**
```json
POST /ai/documents/generate

{
  "caseId": 1,
  "documentType": "COMPLAINT",
  "plaintiff": {
    "name": "张三",
    "type": "PERSON",
    "gender": "男",
    "birthDate": "1980-01-01",
    "nationality": "汉族",
    "idCard": "110101198001011234",
    "address": "北京市朝阳区xx街道xx号",
    "phone": "13800138000"
  },
  "defendant": {
    "name": "李四",
    "type": "PERSON",
    "gender": "女",
    "address": "北京市海淀区xx街道xx号",
    "phone": "13900139000"
  },
  "claims": "1. 请求判令被告偿还借款本金人民币10万元；\n2. 请求判令被告支付利息（按照年利率6%计算）；\n3. 请求判令被告承担诉讼费用。",
  "factsAndReasons": "2023年1月1日，被告因资金周转困难向原告借款10万元，约定借款期限为一年，年利率为6%。借款到期后，被告未能按期归还本息。原告多次催要未果，故诉至法院。",
  "evidenceList": "1. 借条原件一份；\n2. 银行转账记录；\n3. 微信聊天记录。"
}
```

**预期结果：**
- 返回完整的起诉状文书
- 包含：文书标题、当事人信息、诉讼请求、事实与理由、证据清单、致送法院、原告签名、日期
- 格式规范，语言专业

### 2. 答辩状生成测试

**请求示例：**
```json
POST /ai/documents/generate

{
  "caseId": 1,
  "documentType": "DEFENSE_STATEMENT",
  "defendant": {
    "name": "李四",
    "type": "PERSON",
    "gender": "女",
    "address": "北京市海淀区xx街道xx号",
    "phone": "13900139000"
  },
  "defenseOpinion": "1. 原被告之间不存在真实的借款关系；\n2. 所谓的借款实际上是原告之前的还款；\n3. 请求法院驳回原告全部诉讼请求。",
  "factsAndReasons": "原告主张的借款实际上是被告之前借给原告的还款，有转账记录为证。原告恶意诉讼，企图通过合法形式侵害被告合法权益。",
  "evidenceList": "1. 银行转账记录；\n2. 证人证言。"
}
```

**预期结果：**
- 返回完整的答辩状文书
- 针对原告请求逐一答辩
- 答辩理由充分

### 3. 代理词生成测试

**请求示例：**
```json
POST /ai/documents/generate

{
  "caseId": 1,
  "documentType": "BRIEF",
  "briefPoints": "1. 借款关系明确，证据充分；\n2. 被告应当承担还款责任；\n3. 利息计算符合法律规定。",
  "factsAndReasons": "原被告之间的借款关系有借条和转账记录为证，事实清楚。被告应当按照约定履行还款义务。",
  "evidenceList": "1. 借条；\n2. 转账记录；\n3. 相关法律条文：《民法典》第667条、第679条"
}
```

**预期结果：**
- 返回规范的代理词
- 尊称使用正确
- 争议焦点归纳准确
- 代理意见有理有据

### 4. 法律意见书生成测试

**请求示例：**
```json
POST /ai/documents/generate

{
  "caseId": 1,
  "documentType": "LEGAL_OPINION",
  "consultationQuestions": "1. 本案借款关系是否有效？\n2. 原告能否主张利息？\n3. 诉讼时效是否已过？\n4. 应当采取哪些法律措施？",
  "factsAndReasons": "2023年1月1日，张三借给李四10万元，约定借款期限一年，年利率6%。现借款已到期，李四未归还。",
  "additionalContext": "双方是朋友关系，没有书面合同，只有转账记录和微信聊天记录。"
}
```

**预期结果：**
- 返回专业的法律意见书
- 包含：前言、事实背景、法律分析、风险评估、结论与建议、免责声明
- 分析全面，建议具体

## API接口测试

### 1. 获取文书类型列表

**请求：**
```
GET /ai/documents/types
```

**预期结果：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"code": "COMPLAINT", "name": "起诉状", "description": "..."},
    {"code": "DEFENSE_STATEMENT", "name": "答辩状", "description": "..."},
    {"code": "BRIEF", "name": "代理词", "description": "..."},
    {"code": "LEGAL_OPINION", "name": "法律意见书", "description": "..."}
  ]
}
```

## 验收标准

### 功能验收
- [x] DocumentGenerationService成功调用DeepSeek API
- [x] 支持4种文书类型生成（起诉状、答辩状、代理词、法律意见书）
- [x] Controller接口正确接收请求并返回结果
- [x] 有基本的错误处理

### 技术验收
- [x] 使用已创建的LLMApiService
- [x] Prompt模板规范、专业
- [x] 生成结果格式化（Markdown格式）
- [x] 有完整的日志记录
- [x] 有请求验证
- [x] 编译通过

## 错误处理测试

### 1. 案件不存在
```json
{
  "caseId": 99999,
  "documentType": "COMPLAINT"
}
```
**预期：** 返回错误信息"案件不存在"

### 2. 文书类型为空
```json
{
  "caseId": 1
}
```
**预期：** 返回错误信息"文书类型不能为空"

### 3. 起诉状缺少原告信息
```json
{
  "caseId": 1,
  "documentType": "COMPLAINT",
  "defendant": {...}
}
```
**预期：** 返回错误信息"起诉状必须提供原告信息"

### 4. AI服务未配置
**预期：** 返回错误信息"未配置AI服务，请先在系统设置中配置AI"

## 性能测试

- 平均响应时间: < 10秒
- 成功率: > 95%
- 并发支持: 10个请求/分钟

## 集成测试

- AI日志正确记录
- 案件信息正确关联
- 用户权限正确验证
