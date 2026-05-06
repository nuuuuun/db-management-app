# DB管理アプリ 開発者向け起動スクリプト（Docker不要）
# バックエンドとフロントエンドを別プロセスで起動する
# フロントエンド: http://localhost:5173 (HMR有効)
# バックエンド:   http://localhost:8080

$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DB管理アプリ 開発者モード起動" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  バックエンド: http://localhost:8080" -ForegroundColor Green
Write-Host "  フロントエンド: http://localhost:5173" -ForegroundColor Green
Write-Host "  ※ ブラウザは http://localhost:5173 を使ってください" -ForegroundColor Yellow
Write-Host ""

# フロントエンドを別ウィンドウで起動
$frontendDir = Join-Path $ROOT "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendDir'; npm run dev"

# バックエンドを現在のウィンドウで起動
$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw spring-boot:run
} finally {
    Pop-Location
}
