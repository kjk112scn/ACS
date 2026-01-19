# ACS 배포 가이드

## 새 PC 설치 (한 번만)

```powershell
# 관리자 권한 PowerShell에서 실행
.\install.ps1
```

**설치되는 것:**
- OpenJDK 21
- PostgreSQL 16 (포트 5433)
- Nginx
- NSSM (서비스 관리)
- Windows 서비스 등록 (ACS-Backend, ACS-Nginx)

## 배포 (빌드 후 복사)

```powershell
# 빌드 + 복사
.\deploy.ps1

# 빌드 + 복사 + 서비스 재시작
.\deploy.ps1 -Restart
```

## 서비스 관리

```powershell
# 시작
.\start-services.ps1

# 중지
.\stop-services.ps1

# 상태 확인
Get-Service ACS-*, postgresql*
```

## 폴더 구조

```
C:\ACS\
├── backend\
│   └── acs.jar          # Spring Boot JAR
├── frontend\
│   └── (Quasar 빌드 결과)
├── logs\
│   ├── backend-stdout.log
│   └── backend-stderr.log
└── config\
```

## 포트

| 서비스 | 포트 |
|--------|------|
| Nginx (HTTP) | 80 |
| Backend API | 8080 |
| PostgreSQL | 5433 |

## 접속

- 웹: http://localhost
- API: http://localhost/api/
- Swagger: http://localhost/swagger-ui/