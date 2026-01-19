# ============================================
# 오프라인 설치용 패키지 다운로드
# 인터넷 되는 PC에서 실행
# ============================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackagesDir = "$ScriptDir\packages"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  설치 패키지 다운로드" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 폴더 생성
New-Item -ItemType Directory -Path $PackagesDir -Force | Out-Null

# 다운로드 함수
function Download-File {
    param($Url, $FileName)
    $OutPath = "$PackagesDir\$FileName"
    if (Test-Path $OutPath) {
        Write-Host "  - 이미 있음: $FileName" -ForegroundColor Gray
        return
    }
    Write-Host "  - 다운로드 중: $FileName..."
    try {
        Invoke-WebRequest -Uri $Url -OutFile $OutPath -UseBasicParsing
        Write-Host "    완료" -ForegroundColor Green
    } catch {
        Write-Host "    실패: $_" -ForegroundColor Red
    }
}

# ============================================
# 1. OpenJDK 17 (Adoptium/Temurin)
# ============================================
Write-Host "[1/4] OpenJDK 17 다운로드..." -ForegroundColor Yellow
Download-File `
    -Url "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi" `
    -FileName "OpenJDK17U-jdk_x64_windows.msi"

# ============================================
# 2. PostgreSQL 16
# ============================================
Write-Host "[2/4] PostgreSQL 16 다운로드..." -ForegroundColor Yellow
Download-File `
    -Url "https://get.enterprisedb.com/postgresql/postgresql-16.4-1-windows-x64.exe" `
    -FileName "postgresql-16.4-1-windows-x64.exe"

# ============================================
# 3. NSSM
# ============================================
Write-Host "[3/4] NSSM 다운로드..." -ForegroundColor Yellow
Download-File `
    -Url "https://nssm.cc/release/nssm-2.24.zip" `
    -FileName "nssm-2.24.zip"

# ============================================
# 4. Nginx
# ============================================
Write-Host "[4/4] Nginx 다운로드..." -ForegroundColor Yellow
Download-File `
    -Url "https://nginx.org/download/nginx-1.26.2.zip" `
    -FileName "nginx-1.26.2.zip"

# ============================================
# 완료
# ============================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  다운로드 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "다운로드된 파일:" -ForegroundColor Cyan
Get-ChildItem $PackagesDir | ForEach-Object {
    $size = [math]::Round($_.Length / 1MB, 1)
    Write-Host "  - $($_.Name) ($size MB)"
}
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Cyan
Write-Host "  1. offline 폴더 전체를 USB에 복사"
Write-Host "  2. 대상 PC에서 관리자 권한으로 install-offline.ps1 실행"
Write-Host ""