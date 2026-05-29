# v2.2 冒烟验收：登录 → AI 诊断 → 卷宗录入（样例 PDF）
# 用法: .\scripts\verify-v2.2.ps1 [-BaseUrl http://localhost:8080/api] [-SkipIntake]
param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$PdfPath = "",
    [switch]$SkipIntake
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

if (-not $PdfPath) {
    $candidates = @(
        (Join-Path $root "tmp-intake-test.pdf"),
        (Join-Path $root "backend\uploads\intake-pending\alloc-1779814950332_tmp-intake-test.pdf")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { $PdfPath = $c; break }
    }
}

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "FAIL: $msg" -ForegroundColor Red; exit 1 }
function Ok($msg) { Write-Host "OK: $msg" -ForegroundColor Green }

Write-Host "ZGAI LawOS v2.2 verify" -ForegroundColor Yellow
Write-Host "BaseUrl: $BaseUrl"

# 1. Login
Write-Step "POST /auth/login"
try {
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 15
} catch {
    Fail "无法登录 ($BaseUrl)。请确认后端已启动。$($_.Exception.Message)"
}
if ($login.code -ne 200 -and -not $login.success) { Fail "登录失败: $($login.message)" }
$token = $login.data.token
if (-not $token) { Fail "登录响应无 token" }
Ok "登录成功 user=$Username"

$headers = @{ Authorization = "Bearer $token" }

# 2. AI diagnostics
Write-Step "GET /ai/diagnostics"
try {
    $diag = Invoke-RestMethod -Uri "$BaseUrl/ai/diagnostics" -Headers $headers -TimeoutSec 15
} catch {
    Fail "AI diagnostics 请求失败: $($_.Exception.Message)"
}
if ($diag.code -ne 200 -and -not $diag.success) { Fail "diagnostics: $($diag.message)" }
$d = $diag.data
if ($null -eq $d.cloudGlm) { Write-Host "WARN: cloudGlm 字段缺失" -ForegroundColor Yellow }
elseif ($d.cloudGlm -ne $true) { Write-Host "WARN: cloudGlm=$($d.cloudGlm)（期望 true）" -ForegroundColor Yellow }
else { Ok "cloudGlm=true mode=$($d.lawfirmAiMode) ocr=$($d.ocrProvider)" }

# 3. Agent runtime
Write-Step "GET /agent/runtime/status"
try {
    $agent = Invoke-RestMethod -Uri "$BaseUrl/agent/runtime/status" -Headers $headers -TimeoutSec 10
} catch {
    Fail "agent status: $($_.Exception.Message)"
}
Ok "activeProvider=$($agent.data.activeProvider)"

# 4. Case intake
if ($SkipIntake) {
    Write-Host "`n跳过卷宗上传 (-SkipIntake)" -ForegroundColor Yellow
} elseif (-not (Test-Path $PdfPath)) {
    Write-Host "`nWARN: 未找到样例 PDF，跳过卷宗上传。可指定 -PdfPath" -ForegroundColor Yellow
} else {
    Write-Step "POST /case-intake/process ($PdfPath)"
  if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
        Fail "需要 curl.exe 上传 multipart"
    }
    $code = curl.exe -s -o "$env:TEMP\intake-resp.json" -w "%{http_code}" `
        -X POST "$BaseUrl/case-intake/process" `
        -H "Authorization: Bearer $token" `
        -F "file=@$PdfPath"
    $body = Get-Content "$env:TEMP\intake-resp.json" -Raw -ErrorAction SilentlyContinue
    if ($code -ne "200") { Fail "卷宗录入 HTTP $code : $body" }
    $json = $body | ConvertFrom-Json
    if ($json.code -ne 200 -and -not $json.success) { Fail "卷宗录入: $($json.message)" }
    $st = $json.data.status
    Ok "卷宗录入 status=$st pendingId=$($json.data.pendingId)"
    if ($st -eq "FAILED") { Fail "卷宗分析失败: $($json.message)" }
}

Write-Host "`n=== v2.2 冒烟通过 ===" -ForegroundColor Green
exit 0
