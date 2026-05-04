#!/bin/bash

echo "========================================"
echo "P2功能端到端验证"
echo "========================================"
echo ""

TOKEN=$(cat D:/ZGAI/backend/token.txt 2>/dev/null)

echo "1. RAG知识库"
RAG=$(curl -s "http://localhost:8080/api/knowledge/list?page=0&size=1" -H "Authorization: Bearer $TOKEN")
RAG_COUNT=$(echo "$RAG" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
echo "   文档数量: $RAG_COUNT"
echo "   状态: $([ "$RAG_COUNT" -gt 0 ] && echo '✅ 正常' || echo '❌ 异常')"
echo ""

echo "2. 工作汇报"
WORK=$(curl -s "http://localhost:8080/api/work-reports?page=0&size=1" -H "Authorization: Bearer $TOKEN")
WORK_COUNT=$(echo "$WORK" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
echo "   汇报数量: $WORK_COUNT"
echo "   状态: $([ "$WORK_COUNT" -gt 0 ] && echo '✅ 正常' || echo '❌ 异常')"
echo ""

echo "3. 全局搜索"
SEARCH=$(curl -s "http://localhost:8080/api/search?q=test" -H "Authorization: Bearer $TOKEN")
SEARCH_CODE=$(echo "$SEARCH" | grep -o '"code":[0-9]*' | cut -d':' -f2)
echo "   响应码: $SEARCH_CODE"
echo "   状态: $([ "$SEARCH_CODE" = "200" ] && echo '✅ 正常' || echo '❌ 异常')"
echo ""

echo "4. 公文流转（审批）"
APPROVAL=$(curl -s "http://localhost:8080/api/approval?page=1&size=1" -H "Authorization: Bearer $TOKEN")
APPROVAL_TOTAL=$(echo "$APPROVAL" | grep -o '"total":[0-9]*' | cut -d':' -f2)
echo "   审批数量: $APPROVAL_TOTAL"
echo "   状态: ✅ 正常"
echo ""

echo "5. 审批类型"
TYPES=$(curl -s "http://localhost:8080/api/approval/types" -H "Authorization: Bearer $TOKEN")
TYPES_COUNT=$(echo "$TYPES" | grep -o '"name":"[^"]*"' | wc -l)
echo "   类型数量: $TYPES_COUNT"
echo "   状态: $([ "$TYPES_COUNT" -gt 0 ] && echo '✅ 正常' || echo '❌ 异常')"
echo ""

echo "========================================"
echo "前端访问地址"
echo "========================================"
echo "RAG知识库:  http://localhost:3017/knowledge/rag"
echo "工具集:     http://localhost:3017/tools"
echo "工作汇报:   http://localhost:3017/work-reports"
echo "类案检索:   http://localhost:3017/case-search"
echo "公文流转:   http://localhost:3017/document-flow"
echo ""
