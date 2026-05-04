#!/bin/bash

echo "============================================"
echo "债权计算表自动生成系统 - 启动脚本"
echo "============================================"
echo ""

echo "[1/3] 检查Python环境..."
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到Python，请先安装Python 3.8+"
    exit 1
fi
python3 --version

echo ""
echo "[2/3] 检查依赖包..."
if ! python3 -c "import streamlit" &> /dev/null; then
    echo "依赖包未安装，正在安装..."
    pip3 install -r requirements.txt
fi

echo ""
echo "[3/3] 检查Ollama服务..."
if ! curl -s http://localhost:11434/api/tags &> /dev/null; then
    echo "警告: Ollama服务未运行，请先启动Ollama"
    echo "启动方法: 打开新终端运行 'ollama serve'"
    echo ""
fi

echo ""
echo "============================================"
echo "正在启动应用..."
echo "============================================"
echo ""

streamlit run app.py
