@echo off
chcp 65001 >nul
title 债权精算工具 - 打包为EXE
echo ============================================
echo   债权精算工具 v0.3 - 打包为独立EXE
echo ============================================
echo.
echo 正在打包，请等待...（约5-10分钟）
echo.

cd /d "%~dp0"

REM 清理旧的构建缓存
if exist "build" rmdir /s /q build
if exist "dist" rmdir /s /q dist

REM 执行打包
pyinstaller --clean 债权精算工具.spec

echo.
echo ============================================
if %ERRORLEVEL% EQU 0 (
    echo  打包成功！
    echo.
    echo  输出目录: %~dp0dist\债权精算工具\
    echo  启动文件: 债权精算工具.exe
    echo.
    echo  提示：将整个 dist\债权精算工具 文件夹
    echo  复制到其他电脑即可使用。
    echo.
    echo  注意：AI解析功能需要Ollama环境，
    echo  其他功能完全独立运行。
    echo ============================================
) else (
    echo  打包失败！请检查错误信息。
    echo ============================================
)

pause
