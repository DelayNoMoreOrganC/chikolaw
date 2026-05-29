# 律所系统开发环境快速启动（先打包后端 JAR，避免每次 mvn spring-boot:run 冷启动过慢）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$jar = Join-Path $root "backend\target\lawfirm-backend-2.0.0.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Building backend JAR (first time may take 1-2 min)..."
    Set-Location (Join-Path $root "backend")
    mvn -q -DskipTests package
    Set-Location $root
}

$envFile = Join-Path $root "backend\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $val = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $val, 'Process')
        }
    }
    Write-Host "Loaded backend/.env (ZHIPU_API_KEY etc.)"
}

Write-Host "Starting backend on http://localhost:8080/api"
Start-Process powershell -ArgumentList "-NoExit", "-Command", @"
cd '$root\backend'
if (Test-Path '.env') {
  Get-Content '.env' | ForEach-Object {
    if (`$_ -match '^\s*([^#][^=]+)=(.*)$') {
      [Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim(), 'Process')
    }
  }
}
java -jar target\lawfirm-backend-2.0.0.jar --spring.profiles.active=dev
"@

Start-Sleep -Seconds 3
Write-Host "Starting frontend on http://localhost:3017/"
Set-Location (Join-Path $root "frontend")
npm run dev
