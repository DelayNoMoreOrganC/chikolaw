#!/bin/bash

# AI文书生成API测试脚本
# 使用前请确保：
# 1. 后端服务已启动
# 2. DeepSeek API密钥已配置
# 3. 有有效的JWT token

# 配置
BASE_URL="http://localhost:8080/api"
TOKEN="your-jwt-token-here"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4

    echo -e "\n${YELLOW}测试: ${name}${NC}"
    echo "请求: ${method} ${url}"

    if [ -z "$data" ]; then
        response=$(curl -s -X ${method} \
            -H "Authorization: Bearer ${TOKEN}" \
            -H "Content-Type: application/json" \
            "${BASE_URL}${url}")
    else
        echo "数据: ${data}"
        response=$(curl -s -X ${method} \
            -H "Authorization: Bearer ${TOKEN}" \
            -H "Content-Type: application/json" \
            -d "${data}" \
            "${BASE_URL}${url}")
    fi

    echo "响应:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"

    # 检查是否成功
    if echo "$response" | grep -q '"code":200'; then
        echo -e "${GREEN}✓ 测试通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 测试失败${NC}"
        return 1
    fi
}

# 开始测试
echo "========================================="
echo "AI文书生成API测试"
echo "========================================="

# 1. 获取文书类型列表
test_api \
    "获取文书类型列表" \
    "GET" \
    "/ai/documents/types"

# 2. 生成起诉状
test_api \
    "生成起诉状" \
    "POST" \
    "/ai/documents/generate" \
    '{
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
    }'

# 3. 生成答辩状
test_api \
    "生成答辩状" \
    "POST" \
    "/ai/documents/generate" \
    '{
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
    }'

# 4. 生成代理词
test_api \
    "生成代理词" \
    "POST" \
    "/ai/documents/generate" \
    '{
      "caseId": 1,
      "documentType": "BRIEF",
      "briefPoints": "1. 借款关系明确，证据充分；\n2. 被告应当承担还款责任；\n3. 利息计算符合法律规定。",
      "factsAndReasons": "原被告之间的借款关系有借条和转账记录为证，事实清楚。被告应当按照约定履行还款义务。",
      "evidenceList": "1. 借条；\n2. 转账记录；\n3. 相关法律条文：《民法典》第667条、第679条"
    }'

# 5. 生成法律意见书
test_api \
    "生成法律意见书" \
    "POST" \
    "/ai/documents/generate" \
    '{
      "caseId": 1,
      "documentType": "LEGAL_OPINION",
      "consultationQuestions": "1. 本案借款关系是否有效？\n2. 原告能否主张利息？\n3. 诉讼时效是否已过？\n4. 应当采取哪些法律措施？",
      "factsAndReasons": "2023年1月1日，张三借给李四10万元，约定借款期限一年，年利率6%。现借款已到期，李四未归还。",
      "additionalContext": "双方是朋友关系，没有书面合同，只有转账记录和微信聊天记录。"
    }'

# 测试错误处理
echo -e "\n${YELLOW}========================================="
echo "错误处理测试"
echo "=========================================${NC}"

# 测试：案件不存在
test_api \
    "错误处理-案件不存在" \
    "POST" \
    "/ai/documents/generate" \
    '{
      "caseId": 99999,
      "documentType": "COMPLAINT"
    }'

# 测试：缺少必填字段
test_api \
    "错误处理-缺少原告信息" \
    "POST" \
    "/ai/documents/generate" \
    '{
      "caseId": 1,
      "documentType": "COMPLAINT",
      "defendant": {
        "name": "李四",
        "type": "PERSON"
      }
    }'

echo -e "\n${GREEN}========================================="
echo "测试完成"
echo "=========================================${NC}"
