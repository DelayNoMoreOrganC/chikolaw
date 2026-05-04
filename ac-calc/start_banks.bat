@echo off
REM ====================================
REM 债权计算系统 - 银行选择版启动脚本
REM ====================================

echo.
echo ========================================
echo   债权计算系统 - 银行选择版
echo ========================================
echo.

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

echo [提示] 正在启动债权计算系统...
echo.

REM 启动银行选择界面
streamlit run app_bank_selector.py

pause
