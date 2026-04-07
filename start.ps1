﻿﻿﻿﻿﻿#Requires -Version 5.1

<#
.SYNOPSIS
    AI调度中心 - 企业API Key管理系统 全能一键启动脚本 v3.0
.DESCRIPTION
    自动检测所有必要环境（Node.js/Java/Maven/Docker），
    智能选择最佳启动方式（本地/Docker混合/纯Docker），
    同时启动前后端服务，自动打开浏览器。
.NOTES
    支持的环境组合：
    - 完整环境：Node.js + Java + Maven + MySQL + Redis（推荐）
    - 开发环境：Node.js + Java + Maven + Docker（MySQL/Redis用Docker补充）
    - 最小环境：仅 Docker Desktop（全部容器化运行）
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "AI调度Center - Full Startup Script v3.0"

$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Cyan = "Cyan"
$White = "White"
$DarkGray = "DarkGray"
$Magenta = "Magenta"

function Write-Step {
    param([int]$Step, [int]$Total, [string]$Message)
    Write-Host "`n[$Step/$Total] $Message" -ForegroundColor $Cyan
}

function Write-SubStep {
    param([string]$Message, [string]$Status = "")
    if ($Status) {
        Write-Host "  $Message ... [$Status]" -ForegroundColor $(if ($Status -eq "OK") { $Green } elseif ($Status -eq "FAIL") { $Red } elseif ($Status -eq "SKIP" -or $Status -eq "WARN") { $Yellow } else { $Cyan })
    } else {
        Write-Host "  $Message" -ForegroundColor $Cyan
    }
}

function Test-Command {
    param([string]$Command)
    return [bool](Get-Command -Name $Command -ErrorAction SilentlyContinue)
}

function Test-Port {
    param([int]$Port)
    try {
        $conn = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue -ErrorAction Stop
        return $conn.TcpTestSucceeded
    } catch {
        return $false
    }
}

function Get-ProcessOnPort {
    param([int]$Port)
    try {
        $output = netstat -ano | findstr ":$Port.*LISTENING"
        if ($output) {
            $pidMatch = [regex]::Match($output, '\s+(\d+)\s*$')
            if ($pidMatch.Success) {
                $pidNum = [int]$pidMatch.Groups[1].Value
                $proc = Get-Process -Id $pidNum -ErrorAction SilentlyContinue
                if ($proc) {
                    return @{ PID = $pidNum; Name = $proc.ProcessName }
                }
            }
        }
    } catch {}
    return $null
}

function Test-DockerReady {
    if (-not (Test-Command "docker")) { return $false }
    try {
        $null = & docker info --format '{{.OSType}}' 2>&1 | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch { return $false }
}

function Start-DockerContainer {
    param(
        [string]$Name,
        [string]$Image,
        [hashtable]$Ports,
        [hashtable]$EnvVars,
        [hashtable]$Volumes,
        [string]$Command = "",
        [int]$ReadyPort = 0,
        [int]$MaxWaitSeconds = 60
    )

    Write-SubStep -Message "Checking container '$Name'"

    $existing = & docker ps -a --filter "name=$Name" --format "{{.Names}}" 2>$null
    if ($existing) {
        Write-SubStep -Message "Found existing '$Name', starting..."
        & docker start $Name 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            if ($ReadyPort -gt 0) {
                Wait-ForPort -Port $ReadyPort -MaxWait $MaxWaitSeconds
            }
            return $true
        }
        Write-SubStep -Message "Failed to start existing container" -Status "FAIL"
        return $false
    }

    $dockerArgs = @("run", "-d", "--name", $Name, "--restart", "unless-stopped")

    foreach ($port in $Ports.GetEnumerator()) {
        $dockerArgs += "-p", "$($port.Key):$($port.Value)"
    }

    foreach ($env in $EnvVars.GetEnumerator()) {
        $dockerArgs += "-e", "$($env.Key)=$($env.Value)"
    }

    foreach ($vol in $Volumes.GetEnumerator()) {
        $dockerArgs += "-v", "$($vol.Key):$($vol.Value)"
    }

    $dockerArgs += $Image

    if ($Command) {
        $dockerArgs += $Command
    }

    Write-SubStep -Message "Creating new container '$Name' from $Image..."
    & @dockerArgs 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        if ($ReadyPort -gt 0) {
            Wait-ForPort -Port $ReadyPort -MaxWait $MaxWaitSeconds
        }
        return $true
    }

    Write-SubStep -Message "Failed to create container" -Status "FAIL"
    return $false
}

function Wait-ForPort {
    param([int]$Port, [int]$MaxWait = 60)
    Write-Host "    Waiting for port $Port..." -NoNewline -ForegroundColor $DarkGray
    for ($i = 0; $i -lt $MaxWait; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port -Port $Port) {
            Write-Host " [OK]" -ForegroundColor $Green
            return $true
        }
        Write-Host "." -NoNewline -ForegroundColor $DarkGray
    }
    Write-Host " [TIMEOUT]" -ForegroundColor $Yellow
    return $false
}

function Invoke-DockerMySQL {
    param([string]$Sql)
    $output = & docker exec aikey-mysql mysql -uroot -proot -e $Sql 2>&1
    if ($output -match "Warning.*password") { return }
    Write-Output $output
}

function Import-DockerSQLFile {
    param([string]$FilePath, [string]$Database)
    $content = Get-Content $FilePath -Raw -Encoding UTF8
    $tempName = "import_$([guid]::NewGuid().ToString('N').Substring(0,8)).sql"
    $tempPath = Join-Path $env:TEMP $tempName
    Set-Content -Path $tempPath -Value $content -Encoding UTF8
    & docker cp $tempPath aikey-mysql:/tmp/import.sql 2>&1 | Out-Null
    & docker exec aikey-mysql mysql -uroot -proot $Database -e "SOURCE /tmp/import.sql" 2>&1 | Out-Null
    Remove-Item $tempPath -Force -ErrorAction SilentlyContinue
    return $LASTEXITCODE
}

function Install-NodeDependencies {
    param([string]$FrontendDir)

    $nodeModulesPath = Join-Path $FrontendDir "node_modules"

    if (Test-Path $nodeModulesPath) {
        Write-SubStep -Message "node_modules exists, checking updates..."

        Set-Location $FrontendDir
        $packageJsonPath = Join-Path $FrontendDir "package.json"
        $nodeModulesTime = (Get-Item $nodeModulesPath).LastWriteTime
        $packageJsonTime = (Get-Item $packageJsonPath).LastWriteTime

        if ($packageJsonTime -gt $nodeModulesTime) {
            Write-SubStep -Message "package.json is newer, updating dependencies..."
            & npm install 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-SubStep -Message "Dependencies updated" -Status "OK"
            } else {
                Write-SubStep -Message "Update failed, using existing" -Status "WARN"
            }
        } else {
            Write-SubStep -Message "Dependencies up to date" -Status "SKIP"
        }
    } else {
        Write-SubStep -Message "Installing frontend dependencies (first time)..."
        Set-Location $FrontendDir
        Write-Host "    This may take a few minutes on first run..." -ForegroundColor $DarkGray
        & npm install 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-SubStep -Message "Dependencies installed" -Status "OK"
        } else {
            Write-SubStep -Message "npm install failed" -Status "FAIL"
            return $false
        }
    }

    return $true
}

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$DbScriptsDir = Join-Path $BackendDir "src\main\resources\db"

$useDockerForMySQL = $false
$useDockerForRedis = $false
$useDockerForBackend = $false
$frontendProcess = $null
$backendProcess = $null

Clear-Host
Write-Host ""
Write-Host "================================================================" -ForegroundColor $Cyan
Write-Host "  AI调度中心 - 企业API Key管理系统 v3.0" -ForegroundColor $Cyan
Write-Host "  Full Auto Startup (Frontend + Backend)" -ForegroundColor $DarkGray
Write-Host "================================================================" -ForegroundColor $Cyan

$totalSteps = 9
$currentStep = 0

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Detecting environment..."

$envReport = @{}

Write-Host "`n  Checking Node.js/npm..." -NoNewline
if (Test-Command "node") {
    $nodeVersion = (& node -v 2>&1) -replace 'v', ''
    $envReport["NodeJS"] = "v$nodeVersion"
    Write-Host " [OK] v$nodeVersion" -ForegroundColor $Green
} else {
    $envReport["NodeJS"] = "NOT FOUND"
    Write-Host " [MISSING]" -ForegroundColor $Red
}

if (Test-Command "npm") {
    $npmVersion = (& npm -v 2>&1)
    $envReport["NPM"] = "v$npmVersion"
} else {
    $envReport["NPM"] = "NOT FOUND"
}

Write-Host "  Checking Java..." -NoNewline
if (Test-Command "java") {
    $javaVersion = (java -version 2>&1 | Select-String -Pattern '"(\d+\.?\d*).*"' | ForEach-Object { $_.Matches.Groups[1].Value })
    $envReport["Java"] = "v$javaVersion"
    Write-Host " [OK] v$javaVersion" -ForegroundColor $Green
} else {
    $envReport["Java"] = "NOT FOUND"
    Write-Host " [MISSING]" -ForegroundColor $Red
}

Write-Host "  Checking Maven..." -NoNewline
if (Test-Command "mvn") {
    $mavenVersion = (& mvn -v 2>&1 | Select-String -Pattern 'Apache Maven ([\d\.]+)' | ForEach-Object { $_.Matches.Groups[1].Value })
    $envReport["Maven"] = "v$mavenVersion"
    Write-Host " [OK] v$mavenVersion" -ForegroundColor $Green
} else {
    $envReport["Maven"] = "NOT FOUND"
    Write-Host " [MISSING]" -ForegroundColor $Red
}

Write-Host "  Checking Docker..." -NoNewline
$dockerAvailable = Test-DockerReady
if ($dockerAvailable) {
    $envReport["Docker"] = "READY"
    Write-Host " [OK] Ready" -ForegroundColor $Green
} else {
    $envReport["Docker"] = "NOT AVAILABLE"
    Write-Host " [NOT AVAILABLE]" -ForegroundColor $Yellow
}

Write-Host ""
Write-Host "  Environment Summary:" -ForegroundColor $Cyan
foreach ($item in $envReport.GetEnumerator()) {
    $statusColor = if ($item.Value -match "(NOT FOUND|NOT AVAILABLE)") { $Red } elseif ($item.Value -match "v\d") { $Green } else { $Yellow }
    Write-Host "    $($item.Key.PadRight(12)): $($item.Value)" -ForegroundColor $statusColor
}

$hasJavaMaven = ((Test-Command "java") -and (Test-Command "mvn"))
$hasNodeJs = (Test-Command "node") -and (Test-Command "npm")
$canRunLocalBackend = $hasJavaMaven
$canRunFrontend = $hasNodeJs
$canUseDocker = $dockerAvailable

if (-not $canRunLocalBackend -and -not $canUseDocker) {
    Write-Host "`n[ERROR] Cannot start system!" -ForegroundColor $Red
    Write-Host "" -ForegroundColor $Red
    Write-Host "Missing required environment:" -ForegroundColor $Red
    Write-Host "  - Java 17+ and Apache Maven (for local backend)" -ForegroundColor $Yellow
    Write-Host "  - OR Docker Desktop (for containerized backend)" -ForegroundColor $Yellow
    Write-Host ""
    Write-Host "Please install at least one of the following:" -ForegroundColor $Cyan
    Write-Host ""
    Write-Host "  Option 1: Java + Maven (Recommended for development)" -ForegroundColor $White
    Write-Host "    Java:   https://adoptium.net/" -ForegroundColor $Yellow
    Write-Host "    Maven:  https://maven.apache.org/download.cgi" -ForegroundColor $Yellow
    Write-Host ""
    Write-Host "  Option 2: Docker Desktop (Easiest, zero config)" -ForegroundColor $White
    Write-Host "    Docker: https://www.docker.com/products/docker-desktop/" -ForegroundColor $Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

if (-not $hasNodeJs) {
    Write-Host "[WARN] Node.js/npm not found!" -ForegroundColor $Yellow
    Write-Host "  Frontend will not be started." -ForegroundColor $Yellow
    Write-Host "  Download Node.js: https://nodejs.org/ (LTS version recommended)" -ForegroundColor $Cyan
    Write-Host ""
    Read-Host "Press Enter to continue without frontend (or Ctrl+C to exit)"
}

if (-not $canRunLocalBackend -and $canUseDocker) {
    Write-Host "[INFO] Java/Maven not found, will use Docker for backend" -ForegroundColor $Cyan
    $useDockerForBackend = $true
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Checking infrastructure (MySQL on port 3306)..."

if (Test-Port -Port 3306) {
    Write-SubStep -Message "MySQL port 3306" -Status "OK"
} elseif ($canUseDocker) {
    Write-SubStep -Message "MySQL not running, starting via Docker..."

    $mysqlResult = Start-DockerContainer `
        -Name "aikey-mysql" `
        -Image "docker.1ms.run/mysql:8.0" `
        -Ports @{ "3306" = "3306" } `
        -EnvVars @{
            "MYSQL_ROOT_PASSWORD" = "root"
            "MYSQL_DATABASE" = "ai_key_management"
            "CHARACTER_SET_SERVER" = "utf8mb4"
            "COLLATION_SERVER" = "utf8mb4_general_ci"
            "TZ" = "Asia/Shanghai"
        } `
        -Volumes @{ "aikey-mysql-data" = "/var/lib/mysql" } `
        -Command "--character-set-server=utf8mb4", "--collation-server=utf8mb4_general_ci", "--default-authentication-plugin=mysql_native_password" `
        -ReadyPort 3306 `
        -MaxWaitSeconds 90

    if ($mysqlResult) {
        $useDockerForMySQL = $true
        Write-SubStep -Message "MySQL via Docker" -Status "OK"
    } else {
        Write-SubStep -Message "Failed to start MySQL" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }
} else {
    Write-SubStep -Message "MySQL not running and Docker not available" -Status "FAIL"
    Write-Host "`n  Please either:" -ForegroundColor $Cyan
    Write-Host "    1. Start MySQL locally" -ForegroundColor $White
    Write-Host "    2. Install Docker Desktop: https://www.docker.com/products/docker-desktop/" -ForegroundColor $Yellow
    Read-Host "`nPress Enter to exit"
    exit 1
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Checking infrastructure (Redis on port 6379)..."

if (Test-Port -Port 6379) {
    Write-SubStep -Message "Redis port 6379" -Status "OK"
} elseif ($canUseDocker) {
    Write-SubStep -Message "Redis not running, starting via Docker..."

    $redisResult = Start-DockerContainer `
        -Name "aikey-redis" `
        -Image "docker.1ms.run/redis:7-alpine" `
        -Ports @{ "6379" = "6379" } `
        -EnvVars @{} `
        -Volumes @{ "aikey-redis-data" = "/data" } `
        -Command "redis-server", "--appendonly=yes" `
        -ReadyPort 6379 `
        -MaxWaitSeconds 30

    if ($redisResult) {
        $useDockerForRedis = $true
        Write-SubStep -Message "Redis via Docker" -Status "OK"
    } else {
        Write-SubStep -Message "Redis auto-start failed (optional)" -Status "WARN"
    }
} else {
    Write-SubStep -Message "Redis not running (optional, continuing)" -Status "WARN"
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Initializing database..."

$schemaFile = Join-Path $DbScriptsDir "schema.sql"
$dataFile = Join-Path $DbScriptsDir "data.sql"

if (-not (Test-Path $schemaFile)) {
    Write-Host "[ERROR] Schema file not found: $schemaFile" -ForegroundColor $Red
    Read-Host "Press Enter to exit"
    exit 1
}

if ($useDockerForMySQL) {
    Write-SubStep -Message "Creating database (Docker MySQL)..."

    Invoke-DockerMySQL "CREATE DATABASE IF NOT EXISTS ai_key_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

    Write-SubStep -Message "Importing schema..."
    $schemaResult = Import-DockerSQLFile -FilePath $schemaFile -Database "ai_key_management"

    Write-SubStep -Message "Importing initial data..."
    $dataResult = Import-DockerSQLFile -FilePath $dataFile -Database "ai_key_management"

    Write-SubStep -Message "Database ready (admin/admin123)" -Status "OK"
} elseif (Test-Command "mysql") {
    Write-SubStep -Message "Creating database (local MySQL)..."

    & mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS ai_key_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>$null | Out-Null

    Write-SubStep -Message "Importing schema..."
    Get-Content $schemaFile -Raw | & mysql -u root -proot ai_key_management 2>$null | Out-Null

    Write-SubStep -Message "Importing initial data..."
    Get-Content $dataFile -Raw | & mysql -u root -proot ai_key_management 2>$null | Out-Null

    Write-SubStep -Message "Database ready (admin/admin123)" -Status "OK"
} else {
    Write-SubStep -Message "Cannot initialize DB automatically" -Status "WARN"
    Write-Host "  Please run init-db.ps1 manually after MySQL is ready" -ForegroundColor $Cyan
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Checking port availability..."

$portsToCheck = @(8080, 5173)
$portsOccupied = @{}

foreach ($port in $portsToCheck) {
    $procInfo = Get-ProcessOnPort -Port $port
    if ($procInfo) {
        $portsOccupied[$port] = $procInfo
        Write-SubStep -Message "Port $port occupied by $($procInfo.Name) (PID: $($procInfo.PID))" -Status "WARN"
    } else {
        Write-SubStep -Message "Port $port" -Status "OK"
    }
}

if ($portsOccupied.Count -gt 0) {
    Write-Host ""
    Write-Host "  Some ports are in use. Options:" -ForegroundColor $Cyan
    Write-Host "    Y - Terminate conflicting processes automatically" -ForegroundColor $White
    Write-Host "    N - Exit and free ports manually" -ForegroundColor $White

    $choice = Read-Host "  Your choice (Y/N)"

    if ($choice -eq 'Y' -or $choice -eq 'y') {
        foreach ($entry in $portsOccupied.GetEnumerator()) {
            $port = $entry.Key
            $procInfo = $entry.Value
            Write-SubStep -Message "Terminating $($procInfo.Name) (PID: $($procInfo.PID)) on port $port..."
            Stop-Process -Id $procInfo.PID -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
            if (-not (Test-Port -Port $port)) {
                Write-SubStep -Message "Port $port freed" -Status "OK"
            } else {
                Write-SubStep -Message "Failed to free port $port" -Status "FAIL"
                Read-Host "Press Enter to exit"
                exit 1
            }
        }
    } else {
        Write-Host "  Please free the ports and try again." -ForegroundColor $Yellow
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Starting backend service..."

if ($useDockerForBackend) {
    Write-SubStep -Message "Building and starting backend via Docker Compose..."

    Set-Location $ProjectRoot

    $composeFile = Join-Path $ProjectRoot "docker-compose.yml"
    if (-not (Test-Path $composeFile)) {
        Write-SubStep -Message "docker-compose.yml not found" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-Host "    Building images (this may take several minutes on first run)..." -ForegroundColor $DarkGray
    & docker compose build backend 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-SubStep -Message "Docker build failed" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-SubStep -Message "Starting backend container..."
    & docker compose up -d backend 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-SubStep -Message "Failed to start backend container" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-SubStep -Message "Waiting for backend to be ready..."
    $backendStarted = Wait-ForPort -Port 8080 -MaxWait 120

    if ($backendStarted) {
        Write-SubStep -Message "Backend running in Docker (port 8080)" -Status "OK"
    } else {
        Write-SubStep -Message "Backend did not start in time" -Status "FAIL"
        Write-Host "    Check with: docker logs aikey-backend" -ForegroundColor $Yellow
        Read-Host "Press Enter to exit"
        exit 1
    }
} else {
    Set-Location $BackendDir

    Write-SubStep -Message "Compiling with Maven..."
    & mvn clean compile -q 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-SubStep -Message "Compilation failed" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-SubStep -Message "Compile" -Status "OK"

    Write-SubStep -Message "Starting Spring Boot application..."
    $backendProcess = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -NoNewWindow -WorkingDirectory $BackendDir

    Write-SubStep -Message "Waiting for backend to start..."
    $backendStarted = Wait-ForPort -Port 8080 -MaxWait 90

    if ($backendStarted) {
        Write-SubStep -Message "Backend started successfully (port 8080)" -Status "OK"
    } else {
        Write-SubStep -Message "Backend did not start in time" -Status "FAIL"
        if ($backendProcess) { Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue }
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Starting frontend service..."

if ($canRunFrontend) {
    Write-SubStep -Message "Checking/installing Node.js dependencies..."

    if (-not (Install-NodeDependencies -FrontendDir $FrontendDir)) {
        Write-SubStep -Message "Cannot proceed without dependencies" -Status "FAIL"
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-SubStep -Message "Starting Vite dev server..."
    Set-Location $FrontendDir

    $frontendProcess = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" -PassThru -NoNewWindow -WorkingDirectory $FrontendDir -RedirectStandardOutput "$env:TEMP\vite-stdout.log" -RedirectStandardError "$env:TEMP\vite-stderr.log"

    Write-SubStep -Message "Waiting for frontend to start..."
    $frontendStarted = $false
    $maxFrontendWait = 30

    for ($i = 0; $i -lt $maxFrontendWait; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Port -Port 5173) {
            $frontendStarted = $true
            break
        }
        Write-Host "." -NoNewline -ForegroundColor $DarkGray
    }

    Write-Host ""

    if ($frontendStarted) {
        Write-SubStep -Message "Frontend started (http://localhost:5173)" -Status "OK"
    } else {
        Write-SubStep -Message "Frontend may still be starting (check http://localhost:5173)" -Status "WARN"
    }
} else {
    Write-SubStep -Message "Node.js not available, skipping frontend" -Status "SKIP"
}

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "Opening browser..."

Start-Sleep -Seconds 2

if ($canRunFrontend) {
    Start-Process "http://localhost:5173"
    $browserUrl = "http://localhost:5173"
} else {
    Start-Process "http://localhost:8080/swagger-ui/index.html"
    $browserUrl = "http://localhost:8080/swagger-ui/index.html"
}

Write-SubStep -Message "Browser opened: $browserUrl" -Status "OK"

Write-Step -Step (++$currentStep) -Total $totalSteps -Message "System startup complete!"

Write-Host ""
Write-Host "================================================================" -ForegroundColor $Green
Write-Host "  SYSTEM IS RUNNING!" -ForegroundColor $Green
Write-Host "================================================================" -ForegroundColor $Green
Write-Host ""
Write-Host "  Access Information:" -ForegroundColor $Cyan
Write-Host "  -------------------" -ForegroundColor $Cyan
if ($canRunFrontend) {
    Write-Host "  Frontend (Main UI):  http://localhost:5173" -ForegroundColor $Green
}
Write-Host "  Backend API:         http://localhost:8080" -ForegroundColor $Green
Write-Host "  Swagger Docs:         http://localhost:8080/swagger-ui/index.html" -ForegroundColor $Cyan
Write-Host "  Health Check:         http://localhost:8080/api/health" -ForegroundColor $Cyan
Write-Host ""
Write-Host "  Default Account:" -ForegroundColor $Cyan
Write-Host "  Username: admin      Password: admin123" -ForegroundColor $Yellow
Write-Host ""
Write-Host "  Infrastructure Status:" -ForegroundColor $Cyan
Write-Host "  --------------------" -ForegroundColor $Cyan
if ($useDockerForMySQL) {
    Write-Host "  MySQL:   Docker container (aikey-mysql)" -ForegroundColor $Magenta
} else {
    Write-Host "  MySQL:   Local installation" -ForegroundColor $Green
}
if ($useDockerForRedis) {
    Write-Host "  Redis:   Docker container (aikey-redis)" -ForegroundColor $Magenta
} elseif (Test-Port -Port 6379) {
    Write-Host "  Redis:   Local installation" -ForegroundColor $Green
} else {
    Write-Host "  Redis:   Not running (optional)" -ForegroundColor $Yellow
}
if ($useDockerForBackend) {
    Write-Host "  Backend: Docker container (aikey-backend)" -ForegroundColor $Magenta
} else {
    Write-Host "  Backend: Local process (Maven)" -ForegroundColor $Green
}
if ($canRunFrontend) {
    Write-Host "  Frontend: Local process (Vite dev server)" -ForegroundColor $Green
}
Write-Host ""
Write-Host "  Press Ctrl+C to stop all services" -ForegroundColor $Yellow
Write-Host "================================================================" -ForegroundColor $Green
Write-Host ""

try {
    while ($true) {
        Start-Sleep -Seconds 1

        $backendAlive = Test-Port -Port 8080
        $frontendAlive = if ($canRunFrontend) { Test-Port -Port 5173 } else { $true }

        if (-not $backendAlive) {
            Write-Host "`n[WARN] Backend service stopped unexpectedly!" -ForegroundColor $Red
            break
        }

        if ($canRunFrontend -and -not $frontendAlive) {
            Write-Host "`n[WARN] Frontend service stopped unexpectedly!" -ForegroundColor $Red
            break
        }
    }
} catch {
}

Write-Host ""
Write-Host "Stopping services..." -ForegroundColor $Yellow

if ($frontendProcess -and -not $frontendProcess.HasExited) {
    Stop-Process -Id $frontendProcess.Id -Force -ErrorAction SilentlyContinue
    Write-Host "  Frontend stopped" -ForegroundColor $DarkGray
}

if ($backendProcess -and -not $backendProcess.HasExited) {
    Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
    Write-Host "  Backend stopped" -ForegroundColor $DarkGray
}

if ($useDockerForBackend) {
    Set-Location $ProjectRoot
    & docker compose down 2>$null | Out-Null
    Write-Host "  Docker containers stopped" -ForegroundColor $DarkGray
}

Write-Host ""
Write-Host "All services stopped." -ForegroundColor $Yellow
Read-Host "`nPress Enter to exit"
exit 0
