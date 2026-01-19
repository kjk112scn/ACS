#Requires -RunAsAdministrator
# ACS 서비스 시작

Write-Host "ACS 서비스 시작 중..." -ForegroundColor Cyan

# PostgreSQL 먼저
Start-Service postgresql* -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Backend
Start-Service ACS-Backend -ErrorAction SilentlyContinue
Write-Host "  - ACS-Backend 시작됨" -ForegroundColor Green

# Nginx
Start-Service ACS-Nginx -ErrorAction SilentlyContinue
Write-Host "  - ACS-Nginx 시작됨" -ForegroundColor Green

Write-Host ""
Write-Host "서비스 상태:" -ForegroundColor Cyan
Get-Service ACS-*, postgresql* | Format-Table Name, Status, StartType

Write-Host ""
Write-Host "접속 URL: http://localhost" -ForegroundColor Green
