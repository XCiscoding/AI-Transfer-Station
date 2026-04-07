#Requires -Version 5.1

<#
.SYNOPSIS
    AI调度Center - Stop all services
.DESCRIPTION
    Stop Java process (Spring Boot) and optionally Docker MySQL/Redis containers
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "AI调度Center - Stop Services"

$Green = "Green"
$Yellow = "Yellow"
$Cyan = "Cyan"
$Red = "Red"

Clear-Host
Write-Host ""
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host "  AI调度Center - Stop All Services" -ForegroundColor $Cyan
Write-Host "==================================================" -ForegroundColor $Cyan

Write-Host "`n[1/3] Stopping Java process (Spring Boot)..." -ForegroundColor $Cyan

$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($port8080) {
    $processId = $port8080.OwningProcess
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "  Found process PID: $processId ($($process.ProcessName))" -NoNewline
        try {
            Stop-Process -Id $processId -Force
            Write-Host " [STOPPED]" -ForegroundColor $Green
        }
        catch {
            Write-Host " [FAILED]" -ForegroundColor $Yellow
        }
    }
}
else {
    Write-Host "  No process found on port 8080" -ForegroundColor $Yellow
}

Write-Host "`n[2/3] Checking Docker containers..." -ForegroundColor $Cyan

$dockerReady = $false
try {
    $null = & docker info --format '{{.OSType}}' 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { $dockerReady = $true }
} catch {}

if ($dockerReady) {
    $mysqlContainer = & docker ps --filter "name=aikey-mysql" --filter "status=running" --format "{{.Names}}" 2>$null
    $redisContainer = & docker ps --filter "name=aikey-redis" --filter "status=running" --format "{{.Names}}" 2>$null

    if ($mysqlContainer -or $redisContainer) {
        Write-Host ""
        Write-Host "  Running Docker containers detected:" -ForegroundColor $Cyan
        if ($mysqlContainer) { Write-Host "    - aikey-mysql (MySQL)" -ForegroundColor $Yellow }
        if ($redisContainer) { Write-Host "    - aikey-redis (Redis)" -ForegroundColor $Yellow }

        $stopContainers = Read-Host "`n  Stop Docker containers too? (Y/n)"
        if ($stopContainers -ne "n" -and $stopContainers -ne "N") {
            if ($mysqlContainer) {
                Write-Host "  Stopping aikey-mysql..." -NoNewline
                & docker stop aikey-mysql 2>&1 | Out-Null
                Write-Host " [STOPPED]" -ForegroundColor $Green
            }
            if ($redisContainer) {
                Write-Host "  Stopping aikey-redis..." -NoNewline
                & docker stop aikey-redis 2>&1 | Out-Null
                Write-Host " [STOPPED]" -ForegroundColor $Green
            }
        }
        else {
            Write-Host "  Containers left running (data is persisted in Docker volumes)" -ForegroundColor $Yellow
        }
    }
    else {
        Write-Host "  No running Docker containers for this project" -ForegroundColor $Yellow
    }
}
else {
    Write-Host "  Docker not available or not running" -ForegroundColor $DarkGray
}

Write-Host "`n[3/3] Done." -ForegroundColor $Cyan

Write-Host ""
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host "  All services stopped" -ForegroundColor $Green
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host ""

Read-Host "Press Enter to exit"
exit 0
