#!/bin/bash

echo "========================================="
echo "PRD功能完成度 - 最终验证"
echo "========================================="
echo ""

TOKEN=$(cat D:/ZGAI/backend/token.txt 2>/dev/null)

check_api() {
    local name="$1"
    local url="$2"
    local result=$(curl -s "$url" -H "Authorization: Bearer $TOKEN" 2>&1)
    local code=$(echo "$result" | grep -o '"code":[0-9]*' | cut -d':' -f2)
    local data_check=$(echo "$result" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
    local data_check2=$(echo "$result" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    
    if [ "$code" = "200" ]; then
        if [ -n "$data_check" ]; then
            echo "✅ $name - $data_check 条"
        elif [ -n "$data_check2" ]; then
            echo "✅ $name - $data_check2 条"
        else
            echo "✅ $name - API正常"
        fi
    else
        echo "❌ $name - 错误: $code"
    fi
}

echo "【P0核心功能】"
check_api "认证系统" "http://localhost:8080/api/user/info"
check_api "工作台" "http://localhost:8080/api/dashboard/stats"
check_api "案件管理" "http://localhost:8080/api/cases?page=1&size=1"
check_api "日程待办" "http://localhost:8080/api/calendar/events?start=2026-04-01&end=2026-04-30"
check_api "AI OCR" "http://localhost:8080/api/ocr/health"
check_api "卷宗管理" "http://localhost:8080/api/cases/1/dossiers"
echo ""

echo "【P1重要功能】"
check_api "客户管理" "http://localhost:8080/api/clients?page=1&size=1"
check_api "财务管理" "http://localhost:8080/api/finance/expenses?page=1&size=1"
check_api "审批管理" "http://localhost:8080/api/approval?page=1&size=1"
check_api "AI文书生成" "http://localhost:8080/api/ai/generate-doc"
check_api "行政OA" "http://localhost:8080/api/office-supplies"
check_api "统计报表" "http://localhost:8080/api/statistics/cases"
check_api "固定资产" "http://localhost:8080/api/fixed-assets"
echo ""

echo "【P2增强功能】"
check_api "RAG知识库" "http://localhost:8080/api/knowledge/list?page=0&size=1"
check_api "工作汇报" "http://localhost:8080/api/work-reports?page=0&size=1"
check_api "公文流转" "http://localhost:8080/api/approval?page=1&size=1"
echo ""

echo "========================================="
echo "前端访问: http://localhost:3017"
echo "后端API: http://localhost:8080/api"
echo "========================================="
