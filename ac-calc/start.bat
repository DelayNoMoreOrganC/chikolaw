@echo off
chcp 65001 >nul
title 债权精算工具
cd /d "%~dp0"

echo ============================================
echo   债权精算工具 v0.3 - 启动脚本
echo ============================================
echo.

echo [1/3] 检查Python环境...
python --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Python，请先安装Python 3.8+
    echo 下载地址: https://www.python.org/downloads/
    pause
    exit /b 1
)
python --version

echo.
echo [2/3] 检查依赖包...
pip show streamlit >nul 2>&1
if errorlevel 1 (
    echo 首次使用，正在安装依赖...
    pip install -r requirements.txt
    if errorlevel 1 (
        echo 安装失败，请检查网络连接
        pause
        exit /b 1
    )
    echo 依赖安装完成
) else (
    echo 依赖包已就绪
)

echo.
echo [3/3] 检查Ollama服务（可选）...
curl -s http://localhost:11434/api/tags >nul 2>&1
if errorlevel 1 (
    echo 提示: Ollama未运行，AI解析补充协议功能将不可用
    echo       不影响其他功能的使用
)

echo.
echo ============================================
echo 正在启动债权精算工具...
echo 浏览器将自动打开 http://localhost:8899
echo ============================================
echo.

start http://localhost:8899
streamlit run app_streamlit_simple.py --server.port 8899 --server.headless true --browser.gatherUsageStats false

pause
