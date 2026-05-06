$ErrorActionPreference = "Stop"
$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  DB Management App - Local Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# --- Check PostgreSQL ---
Write-Host "[1/4] Checking PostgreSQL..." -ForegroundColor Yellow

$pgReady = $false
try {
    $null = & pg_isready -h localhost -p 5432 2>&1
    if ($LASTEXITCODE -eq 0) { $pgReady = $true }
} catch {}

if (-not $pgReady) {
    try {
        $null = & psql -U postgres -c "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) { $pgReady = $true }
    } catch {}
}

if (-not $pgReady) {
    Write-Host ""
    Write-Host "[ERROR] Cannot connect to PostgreSQL." -ForegroundColor Red
    Write-Host "  1. Make sure PostgreSQL is installed." -ForegroundColor Red
    Write-Host "  2. Start PostgreSQL service: services.msc -> postgresql -> Start" -ForegroundColor Red
    Write-Host ""
    exit 1
}
Write-Host "  PostgreSQL OK" -ForegroundColor Green

# --- Create database if not exists ---
Write-Host "[2/4] Checking database 'dbmanagement'..." -ForegroundColor Yellow

$dbExists = $false
try {
    $dbCheck = & psql -U postgres -lqt 2>&1
    if ($dbCheck -match "dbmanagement") { $dbExists = $true }
} catch {}

if (-not $dbExists) {
    Write-Host "  Database not found. Creating..." -ForegroundColor Yellow
    try {
        & psql -U postgres -c "CREATE DATABASE dbmanagement;" 2>&1
        Write-Host "  Database created." -ForegroundColor Green
    } catch {
        Write-Host ""
        Write-Host "[WARNING] Could not auto-create database." -ForegroundColor Yellow
        Write-Host "  Run manually: psql -U postgres -c `"CREATE DATABASE dbmanagement;`"" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Continue anyway? (Y/N): " -NoNewline
        $ans = Read-Host
        if ($ans -ne "Y" -and $ans -ne "y") { exit 1 }
    }
} else {
    Write-Host "  Database OK (exists)" -ForegroundColor Green
}

# --- Build frontend ---
Write-Host "[3/4] Building frontend..." -ForegroundColor Yellow

$frontendDir = Join-Path $ROOT "frontend"
$staticDir   = Join-Path $ROOT "db-management\src\main\resources\static"

Push-Location $frontendDir
try {
    if (-not (Test-Path "node_modules")) {
        Write-Host "  Running npm install..." -ForegroundColor Yellow
        & npm install --silent
    }
    Write-Host "  Running npm run build..." -ForegroundColor Yellow
    & npm run build -- --silent
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
} finally {
    Pop-Location
}

if (Test-Path $staticDir) {
    Remove-Item "$staticDir\*" -Recurse -Force
}
New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
Copy-Item "$frontendDir\dist\*" $staticDir -Recurse -Force
Write-Host "  Frontend build OK" -ForegroundColor Green

# --- Start Spring Boot ---
Write-Host "[4/4] Starting Spring Boot..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  URL: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Press Ctrl+C to stop." -ForegroundColor Gray
Write-Host ""

$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw spring-boot:run
} finally {
    Pop-Location
}
