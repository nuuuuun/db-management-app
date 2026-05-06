# DB管理アプリ ローカル起動スクリプト（Docker不要）
# 使い方: PowerShell で .\scripts\start-local.ps1 を実行

$ErrorActionPreference = "Stop"
$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DB管理アプリ ローカル起動スクリプト" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- PostgreSQL 接続確認 ---
Write-Host "[1/4] PostgreSQL 接続確認中..." -ForegroundColor Yellow

$pgReady = $false
try {
    $result = & pg_isready -h localhost -p 5432 2>&1
    if ($LASTEXITCODE -eq 0) { $pgReady = $true }
} catch {}

if (-not $pgReady) {
    # pg_isready がない場合は psql で試行
    try {
        $null = & psql -U postgres -c "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) { $pgReady = $true }
    } catch {}
}

if (-not $pgReady) {
    Write-Host ""
    Write-Host "[エラー] PostgreSQL に接続できませんでした。" -ForegroundColor Red
    Write-Host "  以下を確認してください:" -ForegroundColor Red
    Write-Host "  1. PostgreSQL がインストールされているか" -ForegroundColor Red
    Write-Host "  2. PostgreSQL サービスが起動しているか" -ForegroundColor Red
    Write-Host "     > services.msc → 'postgresql' を検索して開始" -ForegroundColor Red
    Write-Host ""
    exit 1
}
Write-Host "  PostgreSQL OK" -ForegroundColor Green

# --- データベース作成（存在しない場合のみ） ---
Write-Host "[2/4] データベース 'dbmanagement' を確認中..." -ForegroundColor Yellow

$dbExists = $false
try {
    $dbCheck = & psql -U postgres -lqt 2>&1 | Select-String "dbmanagement"
    if ($dbCheck) { $dbExists = $true }
} catch {}

if (-not $dbExists) {
    Write-Host "  データベースが存在しません。作成します..." -ForegroundColor Yellow
    try {
        & psql -U postgres -c "CREATE DATABASE dbmanagement;" 2>&1
        Write-Host "  データベース作成完了" -ForegroundColor Green
    } catch {
        Write-Host ""
        Write-Host "[警告] データベースの自動作成に失敗しました。" -ForegroundColor Yellow
        Write-Host "  手動で以下を実行してください:" -ForegroundColor Yellow
        Write-Host "  > psql -U postgres -c `"CREATE DATABASE dbmanagement;`"" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  続行しますか？ (Y/N): " -NoNewline
        $confirm = Read-Host
        if ($confirm -ne "Y" -and $confirm -ne "y") { exit 1 }
    }
} else {
    Write-Host "  データベース OK（既存）" -ForegroundColor Green
}

# --- フロントエンドビルド ---
Write-Host "[3/4] フロントエンドをビルド中..." -ForegroundColor Yellow

$frontendDir = Join-Path $ROOT "frontend"
$staticDir   = Join-Path $ROOT "db-management\src\main\resources\static"

Push-Location $frontendDir
try {
    if (-not (Test-Path "node_modules")) {
        Write-Host "  npm install 中..." -ForegroundColor Yellow
        & npm install --silent
    }
    Write-Host "  npm run build 中..." -ForegroundColor Yellow
    & npm run build -- --silent
    if ($LASTEXITCODE -ne 0) { throw "フロントエンドビルド失敗" }
} finally {
    Pop-Location
}

# dist/ を static/ にコピー
if (Test-Path $staticDir) {
    Remove-Item "$staticDir\*" -Recurse -Force
}
Copy-Item "$frontendDir\dist\*" $staticDir -Recurse -Force
Write-Host "  フロントエンドビルド完了" -ForegroundColor Green

# --- Spring Boot 起動 ---
Write-Host "[4/4] Spring Boot を起動します..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  アクセスURL: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  停止するには Ctrl+C を押してください" -ForegroundColor Gray
Write-Host ""

$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw spring-boot:run
} finally {
    Pop-Location
}
