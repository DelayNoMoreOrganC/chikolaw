#!/bin/bash
echo "=== 关键路径链接检查 ==="
echo ""

# 1. 案件列表页是否存在
if [ -f "frontend/src/views/case/list.vue" ]; then
  echo "✅ 案件列表页存在"
else
  echo "❌ 案件列表页缺失"
fi

# 2. 案件详情页是否存在
if [ -f "frontend/src/views/case/detail.vue" ]; then
  echo "✅ 案件详情页存在"
else
  echo "❌ 案件详情页缺失"
fi

# 3. 客户详情页是否存在
if [ -f "frontend/src/views/client/detail.vue" ]; then
  echo "✅ 客户详情页存在"
else
  echo "❌ 客户详情页缺失"
fi

# 4. 知识库详情页是否存在
if [ -f "frontend/src/views/knowledge/detail.vue" ]; then
  echo "✅ 知识库详情页存在"
else
  echo "❌ 知识库详情页缺失"
fi

echo ""
echo "=== 路由配置检查 ==="
if grep -q "path.*:id.*edit" frontend/src/router/index.js; then
  echo "✅ 编辑路由已配置"
else
  echo "❌ 编辑路由缺失"
fi

