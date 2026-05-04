#!/bin/bash
# ====================================
# 债权计算系统 - 银行选择版启动脚本
# ====================================

echo ""
echo "========================================"
echo "  债权计算系统 - 银行选择版"
echo "========================================"
echo ""

# 检查Python是否安装
if ! command -v python3 &> /dev/null; then
    echo "[错误] 未检测到Python，请先安装Python 3.8+"
    exit 1
fi

echo "[提示] 正在启动债权计算系统..."
echo ""

# 启动银行选择界面
streamlit run app_bank_selector.py
