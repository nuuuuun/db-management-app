$ErrorActionPreference = "Stop"
$ROOT = Split-Path $PSScriptRoot -Parent

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  DB Management App - Offline Startup" -ForegroundColor Cyan
Write-Host "  (No Maven / npm required)" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# --- Find pre-built JAR ---
Write-Host "[1/3] Locating JAR..." -ForegroundColor Yellow

$targetDir = Join-Path $ROOT "db-management\target"
$jar = $null
if (Test-Path $targetDir) {
    $jar = Get-ChildItem $targetDir -Filter "*.jar" |
           Where-Object { $_.Name -notlike "*sources*" } |
           Sort-Object LastWriteTime -Descending |
           Select-Object -First 1
}

if (-not $jar) {
    Write-Host ""
    Write-Host "[ERROR] No JAR found in db-management\target\" -ForegroundColor Red
    Write-Host "  Run .\scripts\build-jar.ps1 on an internet-connected PC first," -ForegroundColor Red
    Write-Host "  then copy the entire repository (including target/) here." -ForegroundColor Red
    Write-Host ""
    exit 1
}
Write-Host "  JAR: $($jar.Name)" -ForegroundColor Green

# --- Find PostgreSQL binaries ---
$pgBin = $null
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
if (-not $pgBin) {
    try {
        $null = Get-Command pg_isready -ErrorAction Stop
        $pgBin = "PATH"
    } catch {}
}

# --- Check PostgreSQL ---
Write-Host "[2/3] Checking PostgreSQL..." -ForegroundColor Yellow

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
        Write-Host "  PostgreSQL not found. Install PostgreSQL 14-17." -ForegroundColor Red
    } else {
        Write-Host "  PostgreSQL found but service not running." -ForegroundColor Red
        Write-Host "  Open services.msc and start postgresql-x64-17 (or similar)." -ForegroundColor Red
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
$env:PGPASSWORD = "postgres"

# --- Create database if not exists ---
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
        Write-Host "[WARNING] Could not auto-create database." -ForegroundColor Yellow
        Write-Host "  Run manually: psql -U postgres -c `"CREATE DATABASE dbmanagement;`"" -ForegroundColor Yellow
        Write-Host "Continue anyway? (Y/N): " -NoNewline
        $ans = Read-Host
        if ($ans -ne "Y" -and $ans -ne "y") { exit 1 }
    }
} else {
    Write-Host "  Database OK (exists)" -ForegroundColor Green
}

# --- Start app ---
Write-Host "[3/3] Starting application..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  URL: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Press Ctrl+C to stop." -ForegroundColor Gray
Write-Host ""

& java -jar $jar.FullName
