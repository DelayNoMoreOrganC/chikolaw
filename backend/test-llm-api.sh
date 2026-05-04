#!/bin/bash

# LLM API集成测试脚本
# 用于验证LLM API配置是否正确

echo "========================================="
echo "LLM API集成测试"
echo "========================================="
echo ""

# 检查环境变量
echo "1. 检查环境变量配置..."
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "⚠️  DEEPSEEK_API_KEY 未设置"
    echo "   请使用以下命令设置："
    echo "   export DEEPSEEK_API_KEY=your-api-key"
else
    echo "✅ DEEPSEEK_API_KEY 已设置"
fi

if [ -z "$QWEN_API_KEY" ]; then
    echo "⚠️  QWEN_API_KEY 未设置（可选）"
else
    echo "✅ QWEN_API_KEY 已设置"
fi

echo ""

# 检查应用是否运行
echo "2. 检查应用状态..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/actuator/health 2>/dev/null)

if [ "$HEALTH_CHECK" == "200" ]; then
    echo "✅ 应用正在运行"
else
    echo "⚠️  应用未运行或无法访问"
    echo "   请先启动应用: cd backend && mvn spring-boot:run"
    exit 1
fi

echo ""

# 测试AI配置接口（需要登录token）
echo "3. 测试AI配置接口..."
echo "   注意：以下测试需要有效的认证token"
echo ""

# 获取可用的AI提供商
echo "测试：获取可用的AI提供商列表"
curl -s -X GET http://localhost:8080/api/ai/config/providers \
  -H "Content-Type: application/json" | python -m json.tool 2>/dev/null || echo "需要认证"

echo ""

# 获取AI配置建议
echo "测试：获取AI功能配置建议"
curl -s -X GET http://localhost:8080/api/ai/config/recommendations \
  -H "Content-Type: application/json" | python -m json.tool 2>/dev/null || echo "需要认证"

echo ""

echo "========================================="
echo "测试完成"
echo "========================================="
echo ""
echo "如需完整测试，请："
echo "1. 设置API密钥环境变量"
echo "2. 启动应用"
echo "3. 登录获取token"
echo "4. 使用token调用API接口"
echo ""
echo "示例："
echo "curl -X POST http://localhost:8080/api/auth/login \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"username\":\"admin\",\"password\":\"admin123\"}'"
echo ""
