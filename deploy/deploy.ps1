# ============================================
# ACS 배포 스크립트
# 빌드 후 설치 폴더에 복사
# ============================================

param(
    [string]$InstallPath = "C:\ACS",
    [switch]$Restart
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  ACS 배포" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 서비스 중지 (Restart 옵션 시)
if ($Restart) {
    Write-Host "[1/4] 서비스 중지 중..." -ForegroundColor Yellow
    Stop-Service ACS-Backend, ACS-Nginx -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# Backend 빌드
Write-Host "[2/4] Backend 빌드 중..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\backend"
& .\gradlew.bat clean bootJar -x test
$jarFile = Get-ChildItem "build\libs\*.jar" -Exclude "*-plain.jar" | Select-Object -First 1
if ($jarFile) {
    Copy-Item $jarFile.FullName "$InstallPath\backend\acs.jar" -Force
    Write-Host "  - JAR 복사 완료: $($jarFile.Name)" -ForegroundColor Green
}
Pop-Location

# Frontend 빌드
Write-Host "[3/4] Frontend 빌드 중..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\frontend"
npm run build
if (Test-Path "dist\spa") {
    Remove-Item "$InstallPath\frontend\*" -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item "dist\spa\*" "$InstallPath\frontend\" -Recurse -Force
    Write-Host "  - Frontend 복사 완료" -ForegroundColor Green
}
Pop-Location

# 서비스 시작 (Restart 옵션 시)
if ($Restart) {
    Write-Host "[4/4] 서비스 시작 중..." -ForegroundColor Yellow
    Start-Service ACS-Backend, ACS-Nginx
    Write-Host "  - 서비스 시작 완료" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  배포 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green