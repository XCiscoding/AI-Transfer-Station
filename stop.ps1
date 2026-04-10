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
$DarkGray = "DarkGray"

$BackendPort = 8080
$FrontendPort = 5173
$MySqlContainerName = "aikey-mysql"
$RedisContainerName = "aikey-redis"
$BackendContainerName = "aikey-backend"
$AutoStopContainers = $env:AIKEY_STOP_DOCKER_CONTAINERS

Clear-Host
Write-Host ""
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host "  AI调度Center - Stop All Services" -ForegroundColor $Cyan
Write-Host "==================================================" -ForegroundColor $Cyan

Write-Host "`n[1/4] Stopping frontend service..." -ForegroundColor $Cyan

$frontendConnection = Get-NetTCPConnection -LocalPort $FrontendPort -ErrorAction SilentlyContinue | Select-Object -First 1
if ($frontendConnection) {
    $processId = $frontendConnection.OwningProcess
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "  Found frontend PID: $processId ($($process.ProcessName))" -NoNewline
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
    Write-Host "  No process found on port $FrontendPort" -ForegroundColor $Yellow
}

Write-Host "`n[2/4] Stopping backend service..." -ForegroundColor $Cyan

$backendConnection = Get-NetTCPConnection -LocalPort $BackendPort -ErrorAction SilentlyContinue | Select-Object -First 1
if ($backendConnection) {
    $processId = $backendConnection.OwningProcess
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "  Found backend PID: $processId ($($process.ProcessName))" -NoNewline
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
    Write-Host "  No process found on port $BackendPort" -ForegroundColor $Yellow
}

Write-Host "`n[3/4] Checking Docker containers..." -ForegroundColor $Cyan

$dockerReady = $false
try {
    $null = & docker info --format '{{.OSType}}' 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { $dockerReady = $true }
} catch {}

if ($dockerReady) {
    $mysqlContainer = & docker ps --filter "name=$MySqlContainerName" --filter "status=running" --format "{{.Names}}" 2>$null
    $redisContainer = & docker ps --filter "name=$RedisContainerName" --filter "status=running" --format "{{.Names}}" 2>$null
    $backendContainer = & docker ps --filter "name=$BackendContainerName" --filter "status=running" --format "{{.Names}}" 2>$null

    if ($mysqlContainer -or $redisContainer -or $backendContainer) {
        Write-Host ""
        Write-Host "  Running Docker containers detected:" -ForegroundColor $Cyan
        if ($mysqlContainer) { Write-Host "    - $MySqlContainerName (MySQL)" -ForegroundColor $Yellow }
        if ($redisContainer) { Write-Host "    - $RedisContainerName (Redis)" -ForegroundColor $Yellow }
        if ($backendContainer) { Write-Host "    - $BackendContainerName (Backend)" -ForegroundColor $Yellow }

        $stopContainers = $AutoStopContainers
        if (-not $stopContainers) {
            $stopContainers = Read-Host "`n  Stop Docker containers too? (Y/n)"
        }
        if ($stopContainers -ne "n" -and $stopContainers -ne "N") {
            if ($backendContainer) {
                Write-Host "  Stopping $BackendContainerName..." -NoNewline
                & docker stop $BackendContainerName 2>&1 | Out-Null
                Write-Host " [STOPPED]" -ForegroundColor $Green
            }
            if ($mysqlContainer) {
                Write-Host "  Stopping $MySqlContainerName..." -NoNewline
                & docker stop $MySqlContainerName 2>&1 | Out-Null
                Write-Host " [STOPPED]" -ForegroundColor $Green
            }
            if ($redisContainer) {
                Write-Host "  Stopping $RedisContainerName..." -NoNewline
                & docker stop $RedisContainerName 2>&1 | Out-Null
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

Write-Host "`n[4/4] Done." -ForegroundColor $Cyan

Write-Host ""
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host "  All services stopped" -ForegroundColor $Green
Write-Host "==================================================" -ForegroundColor $Cyan
Write-Host ""

Read-Host "Press Enter to exit"
exit 0
