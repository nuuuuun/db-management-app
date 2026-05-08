$ErrorActionPreference = "Stop"
$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  DB Management App - Local Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# --- Find PostgreSQL binaries ---
$pgBin = $null

# Search common install locations (newest first)
$pgSearchPaths = @(
    "C:\Program Files\PostgreSQL\17\bin",
    "C:\Program Files\PostgreSQL\16\bin",
    "C:\Program Files\PostgreSQL\15\bin",
    "C:\Program Files\PostgreSQL\14\bin"
)

foreach ($path in $pgSearchPaths) {
    if (Test-Path (Join-Path $path "pg_isready.exe")) {
        $pgBin = $path
        break
    }
}

# Also check if already in PATH
if (-not $pgBin) {
    try {
        $null = Get-Command pg_isready -ErrorAction Stop
        $pgBin = "PATH"
    } catch {}
}

# --- Check PostgreSQL ---
Write-Host "[1/4] Checking PostgreSQL..." -ForegroundColor Yellow

$pgReady = $false

if ($pgBin -and $pgBin -ne "PATH") {
    $pgIsReady = Join-Path $pgBin "pg_isready.exe"
    try {
        $null = & $pgIsReady -h localhost -p 5432 2>&1
        if ($LASTEXITCODE -eq 0) { $pgReady = $true }
    } catch {}
} elseif ($pgBin -eq "PATH") {
    try {
        $null = & pg_isready -h localhost -p 5432 2>&1
        if ($LASTEXITCODE -eq 0) { $pgReady = $true }
    } catch {}
}

if (-not $pgReady) {
    Write-Host ""
    Write-Host "[ERROR] Cannot connect to PostgreSQL on localhost:5432." -ForegroundColor Red
    if (-not $pgBin) {
        Write-Host "  PostgreSQL not found in common locations." -ForegroundColor Red
        Write-Host "  Searched: C:\Program Files\PostgreSQL\{14-17}\bin" -ForegroundColor Red
        Write-Host "  Please install PostgreSQL or add its bin folder to PATH." -ForegroundColor Red
    } else {
        Write-Host "  PostgreSQL binary found but service may not be running." -ForegroundColor Red
        Write-Host "  Open services.msc and start: postgresql-x64-17 (or similar)" -ForegroundColor Red
    }
    Write-Host ""
    exit 1
}
Write-Host "  PostgreSQL OK" -ForegroundColor Green

# --- Resolve psql path ---
if ($pgBin -and $pgBin -ne "PATH") {
    $psql = Join-Path $pgBin "psql.exe"
} else {
    $psql = "psql"
}

# Use default postgres password to avoid interactive prompt
$env:PGPASSWORD = "postgres"

# --- Create database if not exists ---
Write-Host "[2/4] Checking database 'dbmanagement'..." -ForegroundColor Yellow

$dbExists = $false
try {
    $dbCheck = & $psql -U postgres -h localhost -lqt 2>&1
    if ($dbCheck -match "dbmanagement") { $dbExists = $true }
} catch {}

if (-not $dbExists) {
    Write-Host "  Database not found. Creating..." -ForegroundColor Yellow
    try {
        & $psql -U postgres -h localhost -c "CREATE DATABASE dbmanagement;" 2>&1
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

Push-Location $frontendDir
try {
    if (-not (Test-Path "node_modules")) {
        Write-Host "  Running npm install..." -ForegroundColor Yellow
        & npm install --silent
    }
    Write-Host "  Running npm run build..." -ForegroundColor Yellow
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
    Write-Host "  Frontend build OK" -ForegroundColor Green
} finally {
    Pop-Location
}

# --- Check Maven cache before running --offline ---
$parentPom = Join-Path $env:USERPROFILE ".m2\repository\org\springframework\boot\spring-boot-starter-parent\3.4.5\spring-boot-starter-parent-3.4.5.pom"
if (-not (Test-Path $parentPom)) {
    $jar = Get-ChildItem (Join-Path $ROOT "db-management\target") -Filter "*.jar" -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notlike "*sources*" } |
           Select-Object -First 1
    Write-Host ""
    Write-Host "[ERROR] Maven cache (.m2) is empty on this machine." -ForegroundColor Red
    Write-Host "  start-local.ps1 requires Maven dependencies cached locally." -ForegroundColor Red
    if ($jar) {
        Write-Host ""
        Write-Host "  A pre-built JAR was found. Use this instead:" -ForegroundColor Yellow
        Write-Host "  .\scripts\start-jar.ps1" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "  Run on an internet-connected PC first:" -ForegroundColor Yellow
        Write-Host "  .\scripts\build-jar.ps1" -ForegroundColor Cyan
        Write-Host "  Then use: .\scripts\start-jar.ps1" -ForegroundColor Cyan
    }
    Write-Host ""
    exit 1
}

# --- Start Spring Boot ---
Write-Host "[4/4] Starting Spring Boot..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  URL: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Press Ctrl+C to stop." -ForegroundColor Gray
Write-Host ""

$backendDir = Join-Path $ROOT "db-management"
Push-Location $backendDir
try {
    & .\mvnw --offline spring-boot:run
} finally {
    Pop-Location
}
