#!/bin/bash

echo "========================================"
echo "PRD功能完成度验证"
echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

TOKEN=$(cat D:/ZGAI/backend/token.txt 2>/dev/null)

echo "【P0核心功能】"
echo ""

echo "1. 认证系统"
AUTH=$(curl -s "http://localhost:8080/api/user/info" -H "Authorization: Bearer $TOKEN")
echo "   状态: $(echo "$AUTH" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "2. 工作台"
DASH=$(curl -s "http://localhost:8080/api/dashboard/stats" -H "Authorization: Bearer $TOKEN")
echo "   状态: $(echo "$DASH" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "3. 案件管理"
CASE=$(curl -s "http://localhost:8080/api/cases?page=1&size=1" -H "Authorization: Bearer $TOKEN")
CASE_TOTAL=$(echo "$CASE" | grep -o '"total":[0-9]*' | cut -d':' -f2)
echo "   案件数: $CASE_TOTAL"
echo "   基本案情: $(curl -s "http://localhost:8080/api/cases/1" -H "Authorization: Bearer $TOKEN" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo "   办案记录: $(curl -s "http://localhost:8080/api/cases/1/records?page=1&size=1" -H "Authorization: Bearer $TOKEN" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo "   案件文档: $(curl -s "http://localhost:8080/api/cases/1/documents" -H "Authorization: Bearer $TOKEN" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo "   案件动态: $(curl -s "http://localhost:8080/api/cases/1/timeline" -H "Authorization: Bearer $TOKEN" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "4. 日程待办"
CAL=$(curl -s "http://localhost:8080/api/calendar/events?start=2026-04-01&end=2026-04-30" -H "Authorization: Bearer $TOKEN")
CAL_COUNT=$(echo "$CAL" | grep -o '"id":[0-9]*' | wc -l)
echo "   日程数: $CAL_COUNT"
echo ""

echo "5. AI 诊断（v2.3 替代遗留 /ocr/health）"
AI_DIAG=$(curl -s "http://localhost:8080/api/ai/diagnostics" -H "Authorization: Bearer $TOKEN")
echo "   状态: $(echo "$AI_DIAG" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "【P1重要功能】"
echo ""

echo "6. 客户管理"
CLIENT=$(curl -s "http://localhost:8080/api/clients?page=1&size=1" -H "Authorization: Bearer $TOKEN")
echo "   状态: $(echo "$CLIENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "7. 财务管理"
FIN=$(curl -s "http://localhost:8080/api/finance/expenses?page=1&size=1" -H "Authorization: Bearer $TOKEN")
echo "   状态: $(echo "$FIN" | grep -o '"code":[0-9]*' | cut -d':' -f2)"
echo ""

echo "8. 审批管理"
APPR=$(curl -s "http://localhost:8080/api/approval?page=1&size=1" -H "Authorization: Bearer $TOKEN")
APPR_TOTAL=$(echo "$APPR" | grep -o '"total":[0-9]*' | cut -d':' -f2)
echo "   审批数: $APPR_TOTAL"
echo ""

echo "9. AI文书生成"
DOC=$(curl -s -X POST "http://localhost:8080/api/ai/generate-doc" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"caseId":1,"documentType":"PETITION"}')
echo "   状态: $(echo "$DOC" | grep -o '"code":[0-9]*' | cut -d':' -f2) (401为测试密钥预期)"
echo ""

echo "【P2增强功能】"
echo ""

echo "10. RAG知识库"
KNOW=$(curl -s "http://localhost:8080/api/knowledge/list?page=0&size=1" -H "Authorization: Bearer $TOKEN")
KNOW_TOTAL=$(echo "$KNOW" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
echo "   文档数: $KNOW_TOTAL"
echo ""

echo "11. 工作汇报"
WORK=$(curl -s "http://localhost:8080/api/work-reports?page=0&size=1" -H "Authorization: Bearer $TOKEN")
WORK_TOTAL=$(echo "$WORK" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
echo "   汇报数: $WORK_TOTAL"
echo ""

echo "12. 工具集"
echo "   前端: http://localhost:3017/tools"
echo ""

echo "13. 公文流转"
echo "   复用审批系统，审批数: $APPR_TOTAL"
echo ""

echo "========================================"
echo "前端访问: http://localhost:3017"
echo "后端API: http://localhost:8080/api"
echo "========================================"
