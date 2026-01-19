#Requires -RunAsAdministrator
# ============================================
# ACS 전체 설치 스크립트
# 실행: 관리자 권한 PowerShell에서 .\install.ps1
# ============================================

param(
    [string]$InstallPath = "C:\ACS",
    [string]$JdkVersion = "21",
    [string]$DbPassword = "acs1234",
    [switch]$SkipChocolatey
)

$ErrorActionPreference = "Stop"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  ACS 설치 시작" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================
# 1. Chocolatey 설치 (없으면)
# ============================================
if (-not $SkipChocolatey) {
    if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
        Write-Host "[1/6] Chocolatey 설치 중..." -ForegroundColor Yellow
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    } else {
        Write-Host "[1/6] Chocolatey 이미 설치됨" -ForegroundColor Green
    }
}

# ============================================
# 2. 필수 소프트웨어 설치
# ============================================
Write-Host "[2/6] 필수 소프트웨어 설치 중..." -ForegroundColor Yellow

# Java 설치
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "  - OpenJDK $JdkVersion 설치 중..."
    choco install openjdk$JdkVersion -y
    refreshenv
} else {
    Write-Host "  - Java 이미 설치됨" -ForegroundColor Green
}

# NSSM 설치 (서비스 관리)
if (-not (Get-Command nssm -ErrorAction SilentlyContinue)) {
    Write-Host "  - NSSM 설치 중..."
    choco install nssm -y
    refreshenv
} else {
    Write-Host "  - NSSM 이미 설치됨" -ForegroundColor Green
}

# Nginx 설치
if (-not (Test-Path "C:\tools\nginx*")) {
    Write-Host "  - Nginx 설치 중..."
    choco install nginx -y
} else {
    Write-Host "  - Nginx 이미 설치됨" -ForegroundColor Green
}

# PostgreSQL 설치 (TimescaleDB는 별도)
$pgInstalled = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if (-not $pgInstalled) {
    Write-Host "  - PostgreSQL 16 설치 중..."
    choco install postgresql16 --params '/Password:postgres' -y
    refreshenv
} else {
    Write-Host "  - PostgreSQL 이미 설치됨" -ForegroundColor Green
}

# ============================================
# 3. 디렉토리 구조 생성
# ============================================
Write-Host "[3/6] 디렉토리 구조 생성 중..." -ForegroundColor Yellow

$directories = @(
    "$InstallPath",
    "$InstallPath\backend",
    "$InstallPath\frontend",
    "$InstallPath\logs",
    "$InstallPath\config"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "  - 생성: $dir"
    }
}

# ============================================
# 4. TimescaleDB 확장 설치 및 DB 설정
# ============================================
Write-Host "[4/6] TimescaleDB 설정 중..." -ForegroundColor Yellow

# PostgreSQL bin 경로 찾기
$pgPath = (Get-ChildItem "C:\Program Files\PostgreSQL" -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
$psqlPath = "$pgPath\bin\psql.exe"

if (Test-Path $psqlPath) {
    # PostgreSQL 포트를 5433으로 변경 (application.properties와 일치)
    $pgConfPath = "$pgPath\data\postgresql.conf"
    if (Test-Path $pgConfPath) {
        $content = Get-Content $pgConfPath -Raw
        if ($content -notmatch "port = 5433") {
            $content = $content -replace "port = 5432", "port = 5433"
            Set-Content $pgConfPath $content
            Write-Host "  - PostgreSQL 포트를 5433으로 변경"
            Restart-Service postgresql* -Force
            Start-Sleep -Seconds 5
        }
    }

    # DB 및 사용자 생성
    $env:PGPASSWORD = "postgres"

    Write-Host "  - 데이터베이스 생성 중..."
    & $psqlPath -h localhost -p 5433 -U postgres -c "CREATE USER acs_user WITH PASSWORD '$DbPassword';" 2>$null
    & $psqlPath -h localhost -p 5433 -U postgres -c "CREATE DATABASE acs OWNER acs_user;" 2>$null
    & $psqlPath -h localhost -p 5433 -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE acs TO acs_user;" 2>$null

    # TimescaleDB 확장 (설치되어 있으면)
    & $psqlPath -h localhost -p 5433 -U postgres -d acs -c "CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;" 2>$null

    Write-Host "  - DB 설정 완료" -ForegroundColor Green
} else {
    Write-Host "  - 경고: PostgreSQL 경로를 찾을 수 없음" -ForegroundColor Red
}

# ============================================
# 5. Nginx 설정
# ============================================
Write-Host "[5/6] Nginx 설정 중..." -ForegroundColor Yellow

$nginxPath = (Get-ChildItem "C:\tools\nginx*" -Directory | Select-Object -First 1).FullName
$nginxConf = @"
worker_processes 1;

events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;

    # Gzip 압축
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    server {
        listen 80;
        server_name localhost;

        # 프론트엔드 정적 파일
        location / {
            root   $($InstallPath -replace '\\','/')/frontend;
            index  index.html;
            try_files `$uri `$uri/ /index.html;
        }

        # 백엔드 API 프록시
        location /api/ {
            proxy_pass http://127.0.0.1:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host `$host;
            proxy_set_header X-Real-IP `$remote_addr;
        }

        # WebSocket 프록시
        location /ws/ {
            proxy_pass http://127.0.0.1:8080/ws/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host `$host;
        }

        # Swagger UI
        location /swagger-ui/ {
            proxy_pass http://127.0.0.1:8080/swagger-ui/;
        }

        location /v3/api-docs {
            proxy_pass http://127.0.0.1:8080/v3/api-docs;
        }
    }
}
"@

if ($nginxPath) {
    $nginxConf | Out-File -FilePath "$nginxPath\conf\nginx.conf" -Encoding UTF8 -Force
    Write-Host "  - Nginx 설정 완료" -ForegroundColor Green
}

# ============================================
# 6. Windows 서비스 등록
# ============================================
Write-Host "[6/6] Windows 서비스 등록 중..." -ForegroundColor Yellow

# 기존 서비스 제거 (있으면)
$services = @("ACS-Backend", "ACS-Nginx")
foreach ($svc in $services) {
    if (Get-Service -Name $svc -ErrorAction SilentlyContinue) {
        nssm stop $svc 2>$null
        nssm remove $svc confirm 2>$null
    }
}

# Java 경로 찾기
$javaPath = (Get-Command java).Source

# Backend 서비스 등록
Write-Host "  - ACS-Backend 서비스 등록..."
nssm install ACS-Backend $javaPath
nssm set ACS-Backend AppParameters "-jar `"$InstallPath\backend\acs.jar`""
nssm set ACS-Backend AppDirectory "$InstallPath\backend"
nssm set ACS-Backend AppStdout "$InstallPath\logs\backend-stdout.log"
nssm set ACS-Backend AppStderr "$InstallPath\logs\backend-stderr.log"
nssm set ACS-Backend Start SERVICE_AUTO_START
nssm set ACS-Backend AppRestartDelay 5000

# Nginx 서비스 등록
Write-Host "  - ACS-Nginx 서비스 등록..."
nssm install ACS-Nginx "$nginxPath\nginx.exe"
nssm set ACS-Nginx AppDirectory "$nginxPath"
nssm set ACS-Nginx Start SERVICE_AUTO_START

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  설치 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Cyan
Write-Host "  1. JAR 파일을 $InstallPath\backend\acs.jar 에 복사"
Write-Host "  2. 프론트엔드 빌드 결과를 $InstallPath\frontend\ 에 복사"
Write-Host "  3. 서비스 시작: .\start-services.ps1"
Write-Host ""
Write-Host "서비스 관리:" -ForegroundColor Cyan
Write-Host "  시작: net start ACS-Backend && net start ACS-Nginx"
Write-Host "  중지: net stop ACS-Backend && net stop ACS-Nginx"
Write-Host "  상태: Get-Service ACS-*"
Write-Host ""
