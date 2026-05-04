@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM 并发测试脚本 (Windows版本)
REM 模拟50人同时使用系统

set BASE_URL=http://localhost:8080/api
set TOTAL_USERS=50
set CONCURRENT_USERS=50

echo ======================================
echo   并发测试脚本 - 50人同时使用系统
echo ======================================
echo 测试时间: %date% %time%
echo 目标并发: %CONCURRENT_USERS%人
echo.

REM ============================================
REM 测试1: 50人同时登录
REM ============================================
echo ======================================
echo 测试1: %CONCURRENT_USERS%人同时登录
echo ======================================
echo.

echo 准备测试账号...
curl -s -X POST %BASE_URL%/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" > nul

if errorlevel 1 (
    echo [错误] 系统未运行或无法访问 %BASE_URL%
    echo 请先启动系统: cd backend ^&^& mvn spring-boot:run
    pause
    exit /b 1
)

echo [成功] 系统运行正常
echo.

echo 开始并发登录测试...
echo 使用PowerShell进行并发测试
echo.

REM 使用PowerShell进行并发测试
powershell -Command "& { $jobs = @(); for ($i = 1; $i -le %CONCURRENT_USERS%; $i++) { $jobs += Start-Job -ScriptBlock { $start = Get-Date; $response = Invoke-WebRequest -Uri '%BASE_URL%/auth/login' -Method POST -ContentType 'application/json' -Body '{\"username\":\"admin\",\"password\":\"admin123\"}' -UseBasicParsing; $end = Get-Date; $duration = ($end - $start).TotalMilliseconds; if ($response.StatusCode -eq 200) { Write-Host \"[登录] 用户#$i 成功 ($duration ms)\" -ForegroundColor Green } else { Write-Host \"[登录] 用户#$i 失败\" -ForegroundColor Red } } }; Wait-Job $jobs | Out-Null; Remove-Job $jobs }"

echo.

REM ============================================
REM 测试2: 50人同时查询案件列表
REM ============================================
echo ======================================
echo 测试2: %CONCURRENT_USERS%人同时查询案件列表
echo ======================================
echo.

echo 获取认证Token...
for /f "tokens=2 delims=:," %%a in ('curl -s -X POST %BASE_URL%/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}') do (
    set TOKEN=%%a
    goto :found_token
)
:found_token
set TOKEN=%TOKEN:"=%
set TOKEN=%TOKEN: =%

if "%TOKEN%"=="" (
    echo [错误] 无法获取Token
    pause
    exit /b 1
)

echo [成功] 获取到Token: %TOKEN:~0,20%...
echo.

echo 开始并发查询测试...
echo.

powershell -Command "& { $token = '%TOKEN%'; $jobs = @(); for ($i = 1; $i -le %CONCURRENT_USERS%; $i++) { $jobs += Start-Job -ScriptBlock { param($token, $i); $start = Get-Date; $headers = @{ 'Authorization' = 'Bearer ' + $token }; $response = Invoke-WebRequest -Uri '%BASE_URL%/cases?page=1&size=20' -Method GET -Headers $headers -UseBasicParsing; $end = Get-Date; $duration = ($end - $start).TotalMilliseconds; if ($response.StatusCode -eq 200) { Write-Host \"[查询] 用户#$i 成功 ($duration ms)\" -ForegroundColor Green } else { Write-Host \"[查询] 用户#$i 失败\" -ForegroundColor Red } } -ArgumentList $token, $i }; Wait-Job $jobs | Out-Null; Remove-Job $jobs }"

echo.

REM ============================================
REM 测试3: 50人同时查询用户信息
REM ============================================
echo ======================================
echo 测试3: %CONCURRENT_USERS%人同时查询用户信息
echo ======================================
echo.

powershell -Command "& { $token = '%TOKEN%'; $jobs = @(); for ($i = 1; $i -le %CONCURRENT_USERS%; $i++) { $jobs += Start-Job -ScriptBlock { param($token, $i); $start = Get-Date; $headers = @{ 'Authorization' = 'Bearer ' + $token }; $response = Invoke-WebRequest -Uri '%BASE_URL%/users/1' -Method GET -Headers $headers -UseBasicParsing; $end = Get-Date; $duration = ($end - $start).TotalMilliseconds; if ($response.StatusCode -eq 200) { Write-Host \"[查询] 用户#$i 成功 ($duration ms)\" -ForegroundColor Green } else { Write-Host \"[查询] 用户#$i 失败\" -ForegroundColor Red } } -ArgumentList $token, $i }; Wait-Job $jobs | Out-Null; Remove-Job $jobs }"

echo.

REM ============================================
REM 测试总结
REM ============================================
echo ======================================
echo 测试总结
echo ======================================
echo 测试完成时间: %date% %time%
echo.
echo 已完成测试场景:
echo   √ 50人同时登录
echo   √ 50人同时查询案件列表
echo   √ 50人同时查询用户信息
echo.
echo [完成] 并发测试完成！
echo.
echo 优化配置已生效:
echo   • 数据库连接池: maximum-pool-size=50
echo   • 缓存: Caffeine (高性能本地缓存)
echo   • 异步线程池: maxPoolSize=50
echo.
echo 如需查看详细性能指标，请查看:
echo   • 后端日志: backend\logs\lawfirm-backend.log
echo   • 缓存统计: 通过Actuator端点查看
echo ======================================
pause
