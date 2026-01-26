# PostgreSQL Data Migration 진행 로그

## 완료일: 2026-01-26

## 실행 명령어 기록

---

### Step 1: 백업 ✅

**명령어:**
```cmd
mkdir D:\PostgreSQL_Backup
pg_dumpall -U postgres > D:\PostgreSQL_Backup\full_backup.sql
```

**결과:** 백업 완료 (158MB)

---

### Step 2: 서비스 중지 ✅

**명령어:**
```cmd
net stop postgresql-x64-16
```

**결과:** services.msc에서 수동 중지 (관리자 권한 필요)

---

### Step 3: 데이터 복사 ✅

**명령어:**
```bash
cp -r "C:/Program Files/PostgreSQL/16/data/"* "D:/PostgreSQL/data/"
```

**결과:** 836MB 복사 완료

---

### Step 4: 권한 설정 ✅

**명령어:**
```cmd
icacls "D:\PostgreSQL" /grant "NETWORK SERVICE":(OI)(CI)F /T
```

**결과:** 관리자 CMD에서 실행 완료

---

### Step 5: 서비스 설정 변경 ✅

**레지스트리 위치:**
```
HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\postgresql-x64-16
```

**변경 후 ImagePath:**
```
"C:\Program Files\PostgreSQL\16\bin\pg_ctl.exe" runservice -N "postgresql-x64-16" -D "D:\PostgreSQL\data" -w
```

---

### Step 6: 서비스 시작 및 검증 ✅

**명령어:**
```cmd
net start postgresql-x64-16
psql -U postgres -c "SHOW data_directory;"
```

**결과:**
```
   data_directory
--------------------
 D:/PostgreSQL/data
```

---

## 최종 상태

| 항목 | 값 |
|------|-----|
| 데이터 위치 | `D:\PostgreSQL\data` |
| 백업 위치 | `D:\PostgreSQL_Backup\full_backup.sql` |
| 원본 (보관) | `C:\Program Files\PostgreSQL\16\data` |

## 롤백 방법 (필요시)

```cmd
net stop postgresql-x64-16
# 레지스트리 ImagePath를 원래 경로로 변경:
# "C:\Program Files\PostgreSQL\16\bin\pg_ctl.exe" runservice -N "postgresql-x64-16" -D "C:\Program Files\PostgreSQL\16\data" -w
net start postgresql-x64-16
```
