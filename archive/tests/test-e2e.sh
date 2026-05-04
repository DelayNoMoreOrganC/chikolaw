#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "=== 律所案件管理系统 E2E 测试 ==="
echo ""

echo "1. 测试统计卡片..."
curl -s -X GET "http://localhost:8080/api/dashboard/stats?userId=1" \
  -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"monthlyCases":[0-9]*'

echo ""
echo "2. 测试案件列表..."
curl -s -X GET "http://localhost:8080/api/cases" \
  -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"total":[0-9]*'

echo ""
echo "3. 测试待办事项..."
curl -s -X GET "http://localhost:8080/api/todos/assignee/1/priority" \
  -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"title":"[^"]*"' | wc -l

echo ""
echo "4. 测试全局搜索..."
curl -s -X GET "http://localhost:8080/api/search?q=%E6%B5%8B%E8%AF%95" \
  -H "X-User-Id: 1" -H "Authorization: Bearer $TOKEN" \
  | grep -o '"type":"[^"]*"' | wc -l

echo ""
echo "=== 测试完成 ==="
