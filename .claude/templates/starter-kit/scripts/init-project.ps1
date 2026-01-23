# Claude Dev Kit - Project Initializer (Windows PowerShell)
# Usage: .\init-project.ps1 -ProjectName "MyProject" -TargetPath "C:\path\to\project"

param(
    [Parameter(Mandatory=$true)]
    [string]$ProjectName,

    [Parameter(Mandatory=$false)]
    [string]$TargetPath = ".",

    [Parameter(Mandatory=$false)]
    [string]$Description = "프로젝트 설명을 입력하세요"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TemplateDir = Split-Path -Parent $ScriptDir

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "🚀 Claude Dev Kit Initializer" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

# Resolve target path
$TargetPath = Resolve-Path $TargetPath -ErrorAction SilentlyContinue
if (-not $TargetPath) {
    $TargetPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($TargetPath)
}

Write-Host ""
Write-Host "프로젝트: $ProjectName" -ForegroundColor Yellow
Write-Host "대상 경로: $TargetPath" -ForegroundColor Yellow
Write-Host ""

# Create directories
Write-Host "[1/4] 폴더 구조 생성..." -ForegroundColor Green

$folders = @(
    ".claude\agents",
    ".claude\skills",
    "docs\architecture\context",
    "docs\guides",
    "docs\work\active",
    "docs\work\archive",
    "docs\logs",
    "docs\templates"
)

foreach ($folder in $folders) {
    $path = Join-Path $TargetPath $folder
    if (-not (Test-Path $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
        Write-Host "  ✓ $folder" -ForegroundColor DarkGray
    }
}

# Copy .claude folder
Write-Host "[2/4] 스킬 & 에이전트 복사..." -ForegroundColor Green

Copy-Item -Path "$TemplateDir\.claude\*" -Destination "$TargetPath\.claude" -Recurse -Force
Write-Host "  ✓ .claude/agents/ (9개)" -ForegroundColor DarkGray
Write-Host "  ✓ .claude/skills/ (10개)" -ForegroundColor DarkGray

# Copy docs folder
Write-Host "[3/4] 문서 템플릿 복사..." -ForegroundColor Green

Copy-Item -Path "$TemplateDir\docs\*" -Destination "$TargetPath\docs" -Recurse -Force
Write-Host "  ✓ docs/work/ (CURRENT_STATUS, active, archive)" -ForegroundColor DarkGray
Write-Host "  ✓ docs/logs/" -ForegroundColor DarkGray
Write-Host "  ✓ docs/guides/UNIFIED_WORKFLOW.md" -ForegroundColor DarkGray
Write-Host "  ✓ docs/templates/" -ForegroundColor DarkGray

# Copy and customize root files
Write-Host "[4/4] 루트 파일 생성 & 커스터마이징..." -ForegroundColor Green

# CLAUDE.md
$claudeMd = Get-Content "$TemplateDir\CLAUDE.md" -Raw -Encoding UTF8
$claudeMd = $claudeMd -replace '\{프로젝트명\}', $ProjectName
$claudeMd = $claudeMd -replace '\{한 줄 설명\}', $Description
Set-Content -Path "$TargetPath\CLAUDE.md" -Value $claudeMd -Encoding UTF8
Write-Host "  ✓ CLAUDE.md" -ForegroundColor DarkGray

# CHANGELOG.md
Copy-Item -Path "$TemplateDir\CHANGELOG.md" -Destination "$TargetPath\CHANGELOG.md" -Force
Write-Host "  ✓ CHANGELOG.md" -ForegroundColor DarkGray

# Update CURRENT_STATUS.md with project name
$statusMd = Get-Content "$TargetPath\docs\work\CURRENT_STATUS.md" -Raw -Encoding UTF8
$today = Get-Date -Format "yyyy-MM-dd"
$statusMd = $statusMd -replace 'YYYY-MM-DD', $today
Set-Content -Path "$TargetPath\docs\work\CURRENT_STATUS.md" -Value $statusMd -Encoding UTF8

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "✅ 초기화 완료!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host ""
Write-Host "📁 생성된 구조:" -ForegroundColor Cyan
Write-Host "   $TargetPath"
Write-Host "   ├── CLAUDE.md"
Write-Host "   ├── CHANGELOG.md"
Write-Host "   ├── .claude/"
Write-Host "   │   ├── agents/ (9개)"
Write-Host "   │   └── skills/ (10개)"
Write-Host "   └── docs/"
Write-Host "       ├── work/CURRENT_STATUS.md"
Write-Host "       ├── guides/UNIFIED_WORKFLOW.md"
Write-Host "       └── ..."
Write-Host ""
Write-Host "📋 다음 단계:" -ForegroundColor Yellow
Write-Host "   1. CLAUDE.md 열어서 기술스택/규칙 수정"
Write-Host "   2. Claude Code에서 /status 실행"
Write-Host "   3. /feature 또는 /bugfix로 작업 시작"
Write-Host ""
