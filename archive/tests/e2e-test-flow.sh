#!/bin/bash
# 端到端测试脚本 - 模拟完整用户操作路径

echo "=== 用户端到端测试 ==="
echo ""

# 1. 登录
echo "步骤1: 用户登录"
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
CODE=$(echo $LOGIN_RESPONSE | grep -o '"code":[0-9]*' | cut -d':' -f2)

if [ "$CODE" = "200" ] && [ -n "$TOKEN" ]; then
  echo "✅ 登录成功"
  echo "   Token: ${TOKEN:0:20}..."
else
  echo "❌ 登录失败"
  echo "   响应: $LOGIN_RESPONSE"
  exit 1
fi
echo ""

# 2. 查看案件列表
echo "步骤2: 查看案件列表"
CASES_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cases)

CASE_COUNT=$(echo $CASES_RESPONSE | grep -o '"total":[0-9]*' | cut -d':' -f2)
if [ -n "$CASE_COUNT" ]; then
  echo "✅ 案件列表可访问"
  echo "   案件数: $CASE_COUNT"
else
  echo "❌ 案件列表访问失败"
  echo "   响应: $CASES_RESPONSE"
fi
echo ""

# 3. 查看案件详情（假设第一个案件ID为1）
echo "步骤3: 查看案件详情"
CASE_DETAIL=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cases/1)

CASE_NAME=$(echo $CASE_DETAIL | grep -o '"caseName":"[^"]*' | cut -d'"' -f4)
if [ -n "$CASE_NAME" ]; then
  echo "✅ 案件详情可访问"
  echo "   案件名: $CASE_NAME"
else
  echo "⚠️  案件详情访问异常（可能没有ID=1的案件）"
fi
echo ""

# 4. 查看知识库
echo "步骤4: 查看知识库"
KNOWLEDGE_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/knowledge)

ARTICLE_COUNT=$(echo $KNOWLEDGE_RESPONSE | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
if [ -n "$ARTICLE_COUNT" ]; then
  echo "✅ 知识库可访问"
  echo "   文章数: $ARTICLE_COUNT"
else
  echo "❌ 知识库访问失败"
fi
echo ""

# 5. 测试AI问答功能
echo "步骤5: 测试AI问答"
RAG_RESPONSE=$(curl -s -X POST \
  http://localhost:8080/api/knowledge/rag/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is civil litigation?"}')

RAG_STATUS=$(echo $RAG_RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)
if [ -n "$RAG_STATUS" ]; then
  echo "✅ AI问答可访问"
  echo "   状态: $RAG_STATUS"
else
  echo "❌ AI问答访问失败"
fi
echo ""

# 6. 查看客户列表
echo "步骤6: 查看客户列表"
CLIENTS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/clients)

if echo "$CLIENTS_RESPONSE" | grep -q "total"; then
  echo "✅ 客户列表可访问"
else
  echo "⚠️  客户列表返回异常"
fi
echo ""

# 7. 查看统计报表
echo "步骤7: 查看统计报表"
STATS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/statistics/dashboard)

if echo "$STATS_RESPONSE" | grep -q "totalCases\|monthCases"; then
  echo "✅ 统计报表可访问"
else
  echo "⚠️  统计报表返回异常"
fi
echo ""

echo "=== 测试完成 ==="
