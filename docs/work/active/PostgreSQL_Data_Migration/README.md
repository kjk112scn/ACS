# PostgreSQL Data Migration

## 개요

| 항목 | 값 |
|------|-----|
| **목적** | TimescaleDB 데이터를 C → D 드라이브로 이전 |
| **요청일** | 2026-01-26 |
| **상태** | ✅ 완료 |
| **이전 위치** | `C:\Program Files\PostgreSQL\16\data` |
| **새 위치** | `D:\PostgreSQL\data` |
| **데이터 크기** | 836 MB |
| **백업 위치** | `D:\PostgreSQL_Backup\full_backup.sql` (158MB) |

## 작업 단계

| 단계 | 설명 | 상태 |
|------|------|------|
| 1 | 백업 폴더 생성 + pg_dumpall | ✅ |
| 2 | PostgreSQL 서비스 중지 | ✅ |
| 3 | D드라이브로 데이터 복사 | ✅ |
| 4 | 권한 설정 | ✅ |
| 5 | 서비스 설정 변경 (레지스트리) | ✅ |
| 6 | 서비스 시작 + 검증 | ✅ |

## 롤백 방법

문제 발생 시:
1. 서비스 중지
2. 레지스트리에서 ImagePath 원복
3. 서비스 시작

원본 데이터: `C:\Program Files\PostgreSQL\16\data` (삭제하지 않음)

## 관련 문서

- [PROGRESS.md](PROGRESS.md) - 상세 진행 로그