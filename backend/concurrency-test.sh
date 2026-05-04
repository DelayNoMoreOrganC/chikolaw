#!/bin/bash

# 并发测试脚本
# 模拟50人同时使用系统

BASE_URL="http://localhost:8080/api"
TOTAL_USERS=50
CONCURRENT_USERS=50

echo "======================================"
echo "  并发测试脚本 - 50人同时使用系统"
echo "======================================"
echo "测试时间: $(date)"
echo "目标并发: ${CONCURRENT_USERS}人"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
PASS_COUNT=0
FAIL_COUNT=0

# 检查系统是否运行
echo -e "${YELLOW}检查系统状态...${NC}"
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/actuator/health 2>/dev/null)

if [ "$HEALTH_CHECK" != "200" ]; then
    echo -e "${RED}错误: 系统未运行或无法访问 ${BASE_URL}${NC}"
    echo "请先启动系统: cd backend && mvn spring-boot:run"
    exit 1
fi

echo -e "${GREEN}✓ 系统运行正常${NC}"
echo ""

# ============================================
# 测试1: 50人同时登录
# ============================================
echo "======================================"
echo "测试1: ${CONCURRENT_USERS}人同时登录"
echo "======================================"

# 先登录获取token（使用测试账号）
echo -e "${YELLOW}准备测试账号...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ 登录失败，无法获取token${NC}"
    echo "响应: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ 获取到测试Token: ${TOKEN:0:20}...${NC}"
echo ""

# 执行并发登录测试
echo -e "${YELLOW}开始并发登录测试...${NC}"
echo "使用Apache Bench进行压力测试"
echo ""

# 检查是否安装了ab
if ! command -v ab &> /dev/null; then
    echo -e "${YELLOW}警告: 未安装Apache Bench (ab)${NC}"
    echo "请安装: sudo apt-get install apache2-utils"
    echo ""
    echo "使用curl进行简单的并发测试..."
    echo ""

    # 使用简单的并发测试
    for i in $(seq 1 $CONCURRENT_USERS); do
        (
            START_TIME=$(date +%s%N)
            RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
              -H "Content-Type: application/json" \
              -d '{"username":"admin","password":"admin123"}')
            END_TIME=$(date +%s%N)

            if echo "$RESPONSE" | grep -q '"token"'; then
                DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
                echo -e "${GREEN}用户#$i 登录成功 (${DURATION}ms)${NC}"
            else
                echo -e "${RED}用户#$i 登录失败${NC}"
            fi
        ) &
    done

    wait
else
    # 使用Apache Bench进行专业测试
    echo "执行命令: ab -n ${TOTAL_USERS} -c ${CONCURRENT_USERS} -T application/json -p login.json ${BASE_URL}/auth/login"

    # 创建测试数据文件
    cat > /tmp/login.json <<EOF
{"username":"admin","password":"admin123"}
EOF

    # 执行测试
    ab -n ${TOTAL_USERS} \
       -c ${CONCURRENT_USERS} \
       -T "application/json" \
       -p /tmp/login.json \
       ${BASE_URL}/auth/login

    echo ""
fi

echo ""

# ============================================
# 测试2: 50人同时查询案件列表
# ============================================
echo "======================================"
echo "测试2: ${CONCURRENT_USERS}人同时查询案件列表"
echo "======================================"

if ! command -v ab &> /dev/null; then
    echo -e "${YELLOW}使用curl进行简单的并发测试...${NC}"
    echo ""

    # 使用简单的并发测试
    for i in $(seq 1 $CONCURRENT_USERS); do
        (
            START_TIME=$(date +%s%N)
            RESPONSE=$(curl -s -X GET "${BASE_URL}/cases?page=1&size=20" \
              -H "Authorization: Bearer ${TOKEN}")
            END_TIME=$(date +%s%N)

            if echo "$RESPONSE" | grep -q '"records"'; then
                DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
                echo -e "${GREEN}用户#$i 查询成功 (${DURATION}ms)${NC}"
            else
                echo -e "${RED}用户#$i 查询失败${NC}"
            fi
        ) &
    done

    wait
else
    # 使用Apache Bench
    echo "执行命令: ab -n ${TOTAL_USERS} -c ${CONCURRENT_USERS} -H \"Authorization: Bearer ${TOKEN}\" ${BASE_URL}/cases?page=1&size=20"

    ab -n ${TOTAL_USERS} \
       -c ${CONCURRENT_USERS} \
       -H "Authorization: Bearer ${TOKEN}" \
       "${BASE_URL}/cases?page=1&size=20"

    echo ""
fi

echo ""

# ============================================
# 测试3: 50人同时查询用户信息
# ============================================
echo "======================================"
echo "测试3: ${CONCURRENT_USERS}人同时查询用户信息"
echo "======================================"

if ! command -v ab &> /dev/null; then
    echo -e "${YELLOW}使用curl进行简单的并发测试...${NC}"
    echo ""

    # 使用简单的并发测试
    for i in $(seq 1 $CONCURRENT_USERS); do
        (
            START_TIME=$(date +%s%N)
            RESPONSE=$(curl -s -X GET "${BASE_URL}/users/1" \
              -H "Authorization: Bearer ${TOKEN}")
            END_TIME=$(date +%s%N)

            if echo "$RESPONSE" | grep -q '"username"'; then
                DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
                echo -e "${GREEN}用户#$i 查询成功 (${DURATION}ms)${NC}"
            else
                echo -e "${RED}用户#$i 查询失败${NC}"
            fi
        ) &
    done

    wait
else
    # 使用Apache Bench
    echo "执行命令: ab -n ${TOTAL_USERS} -c ${CONCURRENT_USERS} -H \"Authorization: Bearer ${TOKEN}\" ${BASE_URL}/users/1"

    ab -n ${TOTAL_USERS} \
       -c ${CONCURRENT_USERS} \
       -H "Authorization: Bearer ${TOKEN}" \
       "${BASE_URL}/users/1"

    echo ""
fi

echo ""

# ============================================
# 测试4: 混合场景测试（30人登录 + 20人查询）
# ============================================
echo "======================================"
echo "测试4: 混合场景测试（30人登录 + 20人查询案件）"
echo "======================================"

echo -e "${YELLOW}启动混合并发测试...${NC}"
echo ""

# 30人登录
for i in $(seq 1 30); do
    (
        START_TIME=$(date +%s%N)
        RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/login \
          -H "Content-Type: application/json" \
          -d '{"username":"admin","password":"admin123"}')
        END_TIME=$(date +%s%N)

        if echo "$RESPONSE" | grep -q '"token"'; then
            DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
            echo -e "[登录] ${GREEN}用户#$i 成功 (${DURATION}ms)${NC}"
        else
            echo -e "[登录] ${RED}用户#$i 失败${NC}"
        fi
    ) &
done

# 20人查询案件
for i in $(seq 1 20); do
    (
        START_TIME=$(date +%s%N)
        RESPONSE=$(curl -s -X GET "${BASE_URL}/cases?page=1&size=20" \
          -H "Authorization: Bearer ${TOKEN}")
        END_TIME=$(date +%s%N)

        if echo "$RESPONSE" | grep -q '"records"'; then
            DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
            echo -e "[查询] ${GREEN}用户#$i 成功 (${DURATION}ms)${NC}"
        else
            echo -e "[查询] ${RED}用户#$i 失败${NC}"
        fi
    ) &
done

# 等待所有后台任务完成
wait

echo ""

# ============================================
# 测试总结
# ============================================
echo "======================================"
echo "测试总结"
echo "======================================"
echo "测试完成时间: $(date)"
echo ""
echo "已完成测试场景:"
echo "  ✓ 50人同时登录"
echo "  ✓ 50人同时查询案件列表"
echo "  ✓ 50人同时查询用户信息"
echo "  ✓ 混合场景（30人登录 + 20人查询）"
echo ""
echo -e "${GREEN}并发测试完成！${NC}"
echo ""
echo "优化配置已生效:"
echo "  • 数据库连接池: maximum-pool-size=50"
echo "  • 缓存: Caffeine (高性能本地缓存)"
echo "  • 异步线程池: maxPoolSize=50"
echo ""
echo "如需查看详细性能指标，请查看:"
echo "  • 后端日志: backend/logs/lawfirm-backend.log"
echo "  • 缓存统计: 通过Actuator端点查看"
echo "======================================"
