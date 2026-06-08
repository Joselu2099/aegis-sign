<#
.SYNOPSIS
    Deploy aegis-sign complete stack using Docker Engine in WSL2 (no Docker Desktop).

.PARAMETER Action
    up       - Start all services (default)
    down     - Stop and remove containers
    restart  - Down then up
    status   - Show container status
    logs     - Tail logs (use -Service to filter)
    build    - Rebuild app image only

.PARAMETER InfraOnly
    Start only postgres, redis, minio (skip app build/run)

.PARAMETER Build
    Force rebuild of the app Docker image before starting

.PARAMETER Service
    Filter logs to one service: app, postgres, redis, minio

.EXAMPLE
    .\docker-deploy.ps1
    .\docker-deploy.ps1 -Action up -Build
    .\docker-deploy.ps1 -Action up -InfraOnly
    .\docker-deploy.ps1 -Action logs -Service app
    .\docker-deploy.ps1 -Action down
#>
param(
    [ValidateSet("up","down","restart","status","logs","build")]
    [string]$Action = "up",
    [switch]$InfraOnly,
    [switch]$Build,
    [ValidateSet("app","postgres","redis","minio","")]
    [string]$Service = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

$ProjectRoot    = Split-Path $PSScriptRoot -Parent
$ComposeFile    = "$ProjectRoot\docker-compose.yml"
$WSLDistro      = "Ubuntu-22.04"
$DockerContext  = "wsl2-engine"

function Write-Step {
    param([string]$Msg)
    Write-Host ""
    Write-Host "[>] $Msg" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Msg)
    Write-Host "    [OK] $Msg" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Msg)
    Write-Host "    [!] $Msg" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Msg)
    Write-Host "    [X] $Msg" -ForegroundColor Red
}

# Run docker CLI with given args; throw on non-zero exit
function Invoke-Docker {
    param([string[]]$CmdArgs)
    & docker @CmdArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker $($CmdArgs -join ' ') failed (exit $LASTEXITCODE)"
    }
}

# Convert  C:\foo\bar  to  /mnt/c/foo/bar  (WSL2 path)
function ConvertTo-WslPath {
    param([string]$WinPath)
    $driveLetter = $WinPath.Substring(0, 1).ToLower()
    $pathRest    = $WinPath.Substring(2) -replace "\\", "/"
    return "/mnt/$driveLetter$pathRest"
}

# Run docker compose inside WSL2 via Unix socket.
# Used for long builds to avoid TCP EOF disconnect on tcp://127.0.0.1:2375
function Invoke-ComposeInWSL {
    param([string[]]$CmdArgs)
    $wslComposePath = ConvertTo-WslPath $ComposeFile
    $joinedArgs     = $CmdArgs -join " "
    $bashCmd        = "docker compose -f '$wslComposePath' $joinedArgs"
    wsl -d $WSLDistro -- bash -c $bashCmd
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $joinedArgs failed (exit $LASTEXITCODE)"
    }
}

function Ensure-WSL2 {
    Write-Step "Checking WSL2 ($WSLDistro)"

    $wslOut = (wsl --list --verbose 2>&1) -join " "
    if ($wslOut -notmatch "Running") {
        Write-Warn "WSL2 not running - starting $WSLDistro ..."
        wsl -d $WSLDistro -- bash -c "exit" | Out-Null
        Start-Sleep -Seconds 3
    }

    $svcState = (wsl -d $WSLDistro -- bash -c "systemctl is-active docker 2>/dev/null" 2>&1).Trim()
    if ($svcState -ne "active") {
        Write-Warn "Docker Engine not active - starting ..."
        wsl -d $WSLDistro -u root -- bash -c "systemctl start docker" | Out-Null
        Start-Sleep -Seconds 4
    }

    Write-Ok "WSL2 running, Docker Engine active"
}

function Ensure-Context {
    Write-Step "Verifying Docker context ($DockerContext)"

    $current = (docker context show 2>&1).Trim()
    if ($current -ne $DockerContext) {
        Write-Warn "Current context is '$current' - switching to '$DockerContext' ..."
        docker context use $DockerContext | Out-Null
    }

    $ver = (docker info --format '{{.ServerVersion}}' 2>&1).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Cannot reach Docker daemon. Ensure WSL2 is running."
    }
    Write-Ok "Connected to Docker Engine $ver"
}

function Start-Stack {
    Write-Step "Starting services"

    if ($Build -and (-not $InfraOnly)) {
        Write-Warn "Building app image inside WSL2 (prevents TCP EOF on long Maven build)"
        Write-Warn "First run downloads ~300 MB of Maven deps - may take 5-10 min"
        Invoke-ComposeInWSL @("build", "app")
    }

    $upArgs = @("compose", "-f", $ComposeFile, "up", "-d")
    if ($InfraOnly) {
        $upArgs += @("postgres", "redis", "minio")
    }
    Invoke-Docker $upArgs
}

function Stop-Stack {
    Write-Step "Stopping services"
    docker compose -f $ComposeFile down
    Write-Ok "Stack stopped"
}

function Build-App {
    Write-Step "Building app image inside WSL2"
    Write-Warn "First run downloads ~300 MB of Maven deps - may take 5-10 min"
    Invoke-ComposeInWSL @("build", "--no-cache", "app")
    Write-Ok "App image built"
}

function Wait-Healthy {
    param(
        [string[]]$Containers,
        [int]$TimeoutSecs = 180
    )

    Write-Step "Waiting for containers (timeout: ${TimeoutSecs}s)"

    $deadline = [DateTime]::Now.AddSeconds($TimeoutSecs)
    $pending  = [System.Collections.Generic.List[string]]::new()
    foreach ($c in $Containers) { $pending.Add($c) }

    while ($pending.Count -gt 0 -and [DateTime]::Now -lt $deadline) {

        $stillWaiting = [System.Collections.Generic.List[string]]::new()

        foreach ($c in $pending) {
            # Use WSL2 Unix socket for inspect - avoids TCP EOF under daemon load
            $hs = (wsl -d $WSLDistro -- bash -c "docker inspect --format '{{.State.Health.Status}}' $c 2>/dev/null" 2>&1).Trim()
            if ($hs -eq "healthy") {
                Write-Ok "$c  [healthy]"
            }
            elseif ($hs -eq "unhealthy") {
                Write-Fail "$c  [unhealthy] - run: docker logs $c"
                $stillWaiting.Add($c)
            }
            elseif ($hs -eq "starting") {
                $stillWaiting.Add($c)
            }
            else {
                $rs = (wsl -d $WSLDistro -- bash -c "docker inspect --format '{{.State.Running}}' $c 2>/dev/null" 2>&1).Trim()
                if ($rs -eq "true") {
                    Write-Ok "$c  [running]"
                }
                else {
                    $stillWaiting.Add($c)
                }
            }
        }

        $pending = $stillWaiting

        if ($pending.Count -gt 0) {
            Write-Host "    ... waiting for: $($pending -join ', ')" -ForegroundColor DarkGray
            Start-Sleep -Seconds 5
        }
    }

    if ($pending.Count -gt 0) {
        Write-Warn "Timed out waiting for: $($pending -join ', ')"
    }
}

function Show-Summary {
    Write-Step "Stack status"
    docker compose -f $ComposeFile ps

    Write-Host ""
    Write-Host "  Service URLs:" -ForegroundColor White
    Write-Host "    App API      -> http://localhost:8090" -ForegroundColor Green
    Write-Host "    Swagger UI   -> http://localhost:8090/swagger-ui.html" -ForegroundColor Green
    Write-Host "    Health       -> http://localhost:8090/actuator/health" -ForegroundColor Green
    Write-Host "    MinIO UI     -> http://localhost:9001  (aegis_admin / aegis_admin_password)" -ForegroundColor Cyan
    Write-Host "    PostgreSQL   -> localhost:5432  DB=aegis_db  user=aegis_user" -ForegroundColor Cyan
    Write-Host "    Redis        -> localhost:6379" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Useful commands:" -ForegroundColor DarkGray
    Write-Host "    .\docker-deploy.ps1 -Action logs -Service app" -ForegroundColor DarkGray
    Write-Host "    .\docker-deploy.ps1 -Action down" -ForegroundColor DarkGray
    Write-Host "    .\docker-deploy.ps1 -Action up -Build" -ForegroundColor DarkGray
}

# --- Main ---

Write-Host ""
Write-Host "  aegis-sign deploy" -ForegroundColor Magenta

$modeLabel = $Action
if ($InfraOnly) { $modeLabel += " --infra-only" }
if ($Build)     { $modeLabel += " --build" }
Write-Host "  $modeLabel" -ForegroundColor DarkGray

switch ($Action) {
    "up" {
        Ensure-WSL2
        Ensure-Context
        Start-Stack
        $ctrs = @("aegis-postgres", "aegis-redis", "aegis-minio")
        if (-not $InfraOnly) { $ctrs += "aegis-app" }
        Wait-Healthy -Containers $ctrs -TimeoutSecs 180
        Show-Summary
    }
    "down" {
        Ensure-WSL2
        Ensure-Context
        Stop-Stack
    }
    "restart" {
        Ensure-WSL2
        Ensure-Context
        Stop-Stack
        Start-Stack
        $ctrs = @("aegis-postgres", "aegis-redis", "aegis-minio")
        if (-not $InfraOnly) { $ctrs += "aegis-app" }
        Wait-Healthy -Containers $ctrs -TimeoutSecs 180
        Show-Summary
    }
    "status" {
        Ensure-WSL2
        Ensure-Context
        Write-Step "Current stack status"
        docker compose -f $ComposeFile ps
    }
    "logs" {
        Ensure-WSL2
        Ensure-Context
        if ($Service) {
            Write-Step "Tailing logs [$Service]"
            docker compose -f $ComposeFile logs -f $Service
        }
        else {
            Write-Step "Tailing all logs (Ctrl+C to stop)"
            docker compose -f $ComposeFile logs -f
        }
    }
    "build" {
        Ensure-WSL2
        Ensure-Context
        Build-App
    }
}
