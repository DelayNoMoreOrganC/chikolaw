$ErrorActionPreference = 'Continue'
$base = "http://localhost:8080/api"
$results = @()

function Add-Result($name, $ok, $detail) {
  $script:results += [PSCustomObject]@{ Test = $name; OK = $ok; Detail = $detail }
}

$login = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' -TimeoutSec 10
$token = $login.data.token
Add-Result "登录" $true "OK"
$headers = @{ Authorization = "Bearer $token" }

try {
  $d = Invoke-RestMethod -Uri "$base/ai/diagnostics" -Headers $headers -TimeoutSec 10
  Add-Result "AI 诊断" $true "mode=$($d.data.lawfirmAiMode)"
} catch { Add-Result "AI 诊断" $false $_.Exception.Message }

try {
  $logs = Invoke-RestMethod -Uri "$base/ai/logs/user?page=1&size=5" -Headers $headers -TimeoutSec 10
  $items = $logs.data.content
  if (-not $items) { $items = $logs.data.records }
  if (-not $items) { $items = @() }
  Add-Result "使用统计 /ai/logs/user" $true "records=$($items.Count)"
} catch { Add-Result "使用统计 /ai/logs/user" $false $_.Exception.Message }

try {
  Invoke-RestMethod -Uri "$base/ai/logs?page=1&size=5" -Headers $headers -TimeoutSec 5 | Out-Null
  Add-Result "旧路径 /ai/logs" $false "unexpected 200"
} catch {
  $code = $_.Exception.Response.StatusCode.value__
  Add-Result "旧路径 /ai/logs (应404)" ($code -eq 404) "status=$code"
}

try {
  $body = '{"title":"测试","content":"正文"}'
  $r = Invoke-WebRequest -Uri "$base/ai/generate-doc/export-docx" -Method POST -Headers (@{ Authorization = "Bearer $token"; "Content-Type"="application/json" }) -Body $body -UseBasicParsing -TimeoutSec 15
  Add-Result "Word 导出" ($r.StatusCode -eq 200) "size=$($r.RawContentLength)"
} catch { Add-Result "Word 导出" $false $_.Exception.Message }

try {
  $cases = Invoke-RestMethod -Uri "$base/cases?page=1&size=1" -Headers $headers -TimeoutSec 10
  $caseId = $cases.data.records[0].id
  $genBody = (@{ caseId = $caseId; documentType = "起诉状"; additionalContext = "测试生成" } | ConvertTo-Json -Compress)
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $gen = Invoke-RestMethod -Uri "$base/ai/generate-doc" -Method POST -Headers (@{ Authorization = "Bearer $token"; "Content-Type"="application/json" }) -Body $genBody -TimeoutSec 300
  $sw.Stop()
  Add-Result "文书生成" ($gen.data.Length -gt 50) "$($sw.ElapsedMilliseconds)ms len=$($gen.data.Length)"
} catch { Add-Result "文书生成" $false $_.Exception.Message }

try {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $chat = Invoke-RestMethod -Uri "$base/ai/assist" -Method POST -Headers (@{ Authorization = "Bearer $token"; "Content-Type"="application/json" }) -Body '{"message":"用一句话介绍民事起诉状"}' -TimeoutSec 120
  $sw.Stop()
  Add-Result "法律问答" ($chat.data.Length -gt 5) "$($sw.ElapsedMilliseconds)ms"
} catch { Add-Result "法律问答" $false $_.Exception.Message }

$testFile = "d:\ZGAI\backend\uploads\intake-pending\_ai_smoke.txt"
if (-not (Test-Path $testFile)) { "test" | Out-File -Encoding utf8 $testFile }
$code = curl.exe -s -o NUL -w "%{http_code}" -X POST "$base/case-intake/process" -H "Authorization: Bearer $token" -F "file=@$testFile"
Add-Result "卷宗录入 multipart" ($code -eq "200") "http=$code"

$results | Format-Table -AutoSize -Wrap
$fail = @($results | Where-Object { -not $_.OK }).Count
Write-Host "TOTAL=$($results.Count) PASS=$($results.Count - $fail) FAIL=$fail"
if ($fail -gt 0) { exit 1 }
