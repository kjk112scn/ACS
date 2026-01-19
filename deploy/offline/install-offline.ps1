#Requires -RunAsAdministrator
# ============================================
# ACS 오프라인 설치 스크립트
# 모든 파일이 packages/ 폴더에 있어야 함
# ============================================

param(
    [string]$InstallPath = "C:\ACS",
    [string]$DbPassword = "acs1234"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackagesDir = "$ScriptDir\packages"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  ACS 오프라인 설치" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 패키지 폴더 확인
if (-not (Test-Path $PackagesDir)) {
    Write-Host "오류: packages 폴더가 없습니다!" -ForegroundColor Red
    Write-Host "먼저 download-packages.ps1을 인터넷 환경에서 실행하세요."
    exit 1
}

# ============================================
# 1. Java 설치
# ============================================
Write-Host "[1/7] Java 설치 중..." -ForegroundColor Yellow

$javaInstaller = Get-ChildItem "$PackagesDir\*jdk*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $javaInstaller) {
    $javaInstaller = Get-ChildItem "$PackagesDir\*jdk*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($javaInstaller) {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        if ($javaInstaller.Extension -eq ".msi") {
            Start-Process msiexec.exe -ArgumentList "/i `"$($javaInstaller.FullName)`" /quiet /norestart" -Wait
        } else {
            Start-Process $javaInstaller.FullName -ArgumentList "/s" -Wait
        }
        # 환경변수 새로고침
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        Write-Host "  - Java 설치 완료" -ForegroundColor Green
    } else {
        Write-Host "  - Java 이미 설치됨" -ForegroundColor Green
    }
} else {
    Write-Host "  - 경고: Java 설치 파일 없음" -ForegroundColor Red
}

# ============================================
# 2. PostgreSQL 설치
# ============================================
Write-Host "[2/7] PostgreSQL 설치 중..." -ForegroundColor Yellow

$pgInstaller = Get-ChildItem "$PackagesDir\postgresql*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1

if ($pgInstaller) {
    $pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
    if (-not $pgService) {
        # PostgreSQL 자동 설치 (포트 5433, 비밀번호 postgres)
        Start-Process $pgInstaller.FullName -ArgumentList `
            "--mode unattended",
            "--unattendedmodeui minimal",
            "--superpassword postgres",
            "--serverport 5433",
            "--prefix `"C:\Program Files\PostgreSQL\16`"" -Wait
        Write-Host "  - PostgreSQL 설치 완료 (포트: 5433)" -ForegroundColor Green
        Start-Sleep -Seconds 5
    } else {
        Write-Host "  - PostgreSQL 이미 설치됨" -ForegroundColor Green
    }
} else {
    Write-Host "  - 경고: PostgreSQL 설치 파일 없음" -ForegroundColor Red
}

# ============================================
# 3. NSSM 설치
# ============================================
Write-Host "[3/7] NSSM 설치 중..." -ForegroundColor Yellow

$nssmZip = Get-ChildItem "$PackagesDir\nssm*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
$nssmPath = "C:\tools\nssm"

if ($nssmZip) {
    if (-not (Test-Path "$nssmPath\nssm.exe")) {
        New-Item -ItemType Directory -Path $nssmPath -Force | Out-Null
        Expand-Archive $nssmZip.FullName -DestinationPath "$nssmPath\temp" -Force
        # win64 폴더에서 nssm.exe 복사
        $nssmExe = Get-ChildItem "$nssmPath\temp" -Recurse -Filter "nssm.exe" |
                   Where-Object { $_.DirectoryName -like "*win64*" } |
                   Select-Object -First 1
        if ($nssmExe) {
            Copy-Item $nssmExe.FullName "$nssmPath\nssm.exe" -Force
        }
        Remove-Item "$nssmPath\temp" -Recurse -Force
        # PATH에 추가
        $machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
        if ($machinePath -notlike "*$nssmPath*") {
            [System.Environment]::SetEnvironmentVariable("Path", "$machinePath;$nssmPath", "Machine")
            $env:Path = "$env:Path;$nssmPath"
        }
        Write-Host "  - NSSM 설치 완료" -ForegroundColor Green
    } else {
        Write-Host "  - NSSM 이미 설치됨" -ForegroundColor Green
    }
} else {
    Write-Host "  - 경고: NSSM 파일 없음" -ForegroundColor Red
}

# ============================================
# 4. Nginx 설치
# ============================================
Write-Host "[4/7] Nginx 설치 중..." -ForegroundColor Yellow

$nginxZip = Get-ChildItem "$PackagesDir\nginx*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
$nginxPath = "C:\tools\nginx"

if ($nginxZip) {
    if (-not (Test-Path "$nginxPath\nginx.exe")) {
        New-Item -ItemType Directory -Path "C:\tools" -Force | Out-Null
        Expand-Archive $nginxZip.FullName -DestinationPath "C:\tools" -Force
        # nginx-x.x.x 폴더 이름을 nginx로 변경
        $nginxExtracted = Get-ChildItem "C:\tools\nginx-*" -Directory | Select-Object -First 1
        if ($nginxExtracted) {
            Rename-Item $nginxExtracted.FullName $nginxPath -Force
        }
        Write-Host "  - Nginx 설치 완료" -ForegroundColor Green
    } else {
        Write-Host "  - Nginx 이미 설치됨" -ForegroundColor Green
    }
} else {
    Write-Host "  - 경고: Nginx 파일 없음" -ForegroundColor Red
}

# ============================================
# 5. 디렉토리 구조 생성
# ============================================
Write-Host "[5/7] 디렉토리 구조 생성 중..." -ForegroundColor Yellow

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
    }
}
Write-Host "  - 디렉토리 생성 완료" -ForegroundColor Green

# ============================================
# 6. DB 설정
# ============================================
Write-Host "[6/7] 데이터베이스 설정 중..." -ForegroundColor Yellow

$pgPath = "C:\Program Files\PostgreSQL\16"
$psqlPath = "$pgPath\bin\psql.exe"

if (Test-Path $psqlPath) {
    Start-Sleep -Seconds 3
    $env:PGPASSWORD = "postgres"

    # 사용자 및 DB 생성
    & $psqlPath -h localhost -p 5433 -U postgres -c "CREATE USER acs_user WITH PASSWORD '$DbPassword';" 2>$null
    & $psqlPath -h localhost -p 5433 -U postgres -c "CREATE DATABASE acs OWNER acs_user;" 2>$null
    & $psqlPath -h localhost -p 5433 -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE acs TO acs_user;" 2>$null
    & $psqlPath -h localhost -p 5433 -U postgres -c "ALTER USER acs_user CREATEDB;" 2>$null

    Write-Host "  - DB 설정 완료 (acs_user / $DbPassword)" -ForegroundColor Green
} else {
    Write-Host "  - 경고: psql 경로를 찾을 수 없음" -ForegroundColor Red
}

# ============================================
# 7. Nginx 설정 + 서비스 등록
# ============================================
Write-Host "[7/7] 서비스 등록 중..." -ForegroundColor Yellow

# Nginx 설정 파일 복사
$nginxConfSource = "$ScriptDir\config\nginx.conf"
if (Test-Path $nginxConfSource) {
    Copy-Item $nginxConfSource "$nginxPath\conf\nginx.conf" -Force
    Write-Host "  - Nginx 설정 복사 완료" -ForegroundColor Green
}

# 기존 서비스 제거
$services = @("ACS-Backend", "ACS-Nginx")
foreach ($svc in $services) {
    if (Get-Service -Name $svc -ErrorAction SilentlyContinue) {
        & "$nssmPath\nssm.exe" stop $svc 2>$null
        & "$nssmPath\nssm.exe" remove $svc confirm 2>$null
    }
}

# Java 경로 찾기
$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if (-not $javaExe) {
    $javaExe = "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe"
    $javaExe = (Get-ChildItem $javaExe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
}

if ($javaExe) {
    # Backend 서비스
    & "$nssmPath\nssm.exe" install ACS-Backend "$javaExe"
    & "$nssmPath\nssm.exe" set ACS-Backend AppParameters "-jar `"$InstallPath\backend\acs.jar`""
    & "$nssmPath\nssm.exe" set ACS-Backend AppDirectory "$InstallPath\backend"
    & "$nssmPath\nssm.exe" set ACS-Backend AppStdout "$InstallPath\logs\backend-stdout.log"
    & "$nssmPath\nssm.exe" set ACS-Backend AppStderr "$InstallPath\logs\backend-stderr.log"
    & "$nssmPath\nssm.exe" set ACS-Backend Start SERVICE_AUTO_START
    Write-Host "  - ACS-Backend 서비스 등록 완료" -ForegroundColor Green
}

# Nginx 서비스
& "$nssmPath\nssm.exe" install ACS-Nginx "$nginxPath\nginx.exe"
& "$nssmPath\nssm.exe" set ACS-Nginx AppDirectory "$nginxPath"
& "$nssmPath\nssm.exe" set ACS-Nginx Start SERVICE_AUTO_START
Write-Host "  - ACS-Nginx 서비스 등록 완료" -ForegroundColor Green

# ============================================
# 완료
# ============================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  설치 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Cyan
Write-Host "  1. acs.jar 파일을 $InstallPath\backend\ 에 복사"
Write-Host "  2. frontend 빌드 결과를 $InstallPath\frontend\ 에 복사"
Write-Host "  3. 서비스 시작:"
Write-Host "     net start ACS-Backend"
Write-Host "     net start ACS-Nginx"
Write-Host ""
Write-Host "접속: http://localhost" -ForegroundColor Green
Write-Host ""
