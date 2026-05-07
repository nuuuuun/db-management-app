$ErrorActionPreference = "Stop"
$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  DB Management App - Build JAR" -ForegroundColor Cyan
Write-Host "  (Run this on internet-connected PC)" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# --- Build frontend ---
Write-Host "[1/2] Building frontend..." -ForegroundColor Yellow

$frontendDir = Join-Path $ROOT "frontend"
Push-Location $frontendDir
try {
    if (-not (Test-Path "node_modules")) {
        Write-Host "  Running npm install..." -ForegroundColor Yellow
        & npm install
        if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
    }
    Write-Host "  Running npm run build..." -ForegroundColor Yellow
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
    Write-Host "  Frontend build OK" -ForegroundColor Green
} finally {
    Pop-Location
}

# --- Build fat JAR ---
Write-Host "[2/2] Building fat JAR (Spring Boot)..." -ForegroundColor Yellow

$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
} finally {
    Pop-Location
}

$jar = Get-ChildItem (Join-Path $backendDir "target") -Filter "*.jar" |
       Where-Object { $_.Name -notlike "*sources*" } |
       Sort-Object LastWriteTime -Descending |
       Select-Object -First 1

if (-not $jar) {
    Write-Host ""
    Write-Host "[ERROR] JAR file not found in target/" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  Build complete!" -ForegroundColor Green
Write-Host "  JAR: db-management\target\$($jar.Name)" -ForegroundColor Green
Write-Host ""
Write-Host "  To deploy to restricted network:" -ForegroundColor Yellow
Write-Host "  Copy the entire repository (including target/) to the target PC" -ForegroundColor Yellow
Write-Host "  Then run: .\scripts\start-jar.ps1" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
