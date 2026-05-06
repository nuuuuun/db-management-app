$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  DB Management App - Dev Mode" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Backend:  http://localhost:8080" -ForegroundColor Green
Write-Host "  Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "  Use http://localhost:5173 in browser." -ForegroundColor Yellow
Write-Host ""

$frontendDir = Join-Path $ROOT "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendDir'; npm run dev"

$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw spring-boot:run
} finally {
    Pop-Location
}
