#Requires -RunAsAdministrator
# ACS 서비스 중지

Write-Host "ACS 서비스 중지 중..." -ForegroundColor Yellow

Stop-Service ACS-Nginx -ErrorAction SilentlyContinue
Write-Host "  - ACS-Nginx 중지됨"

Stop-Service ACS-Backend -ErrorAction SilentlyContinue
Write-Host "  - ACS-Backend 중지됨"

Write-Host ""
Write-Host "서비스 상태:" -ForegroundColor Cyan
Get-Service ACS-* | Format-Table Name, Status