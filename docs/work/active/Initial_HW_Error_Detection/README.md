# Initial_HW_Error_Detection

## 개요

**목적**: BE 시작 시 이미 존재하는 HW 에러 상태를 감지하여 FE에 표시
**요청일**: 2026-01-20
**상태**: 🚧 진행중

## 문제 상황

| 상황 | 현재 동작 | 기대 동작 |
|------|----------|----------|
| BE 실행 중 HW 에러 발생 | ✅ 에러 감지 + DB 저장 + FE 표시 | - |
| BE 시작 시 이미 HW 에러 상태 | ❌ 무시됨 | 에러 감지 + DB 저장 + FE 표시 |

## 요구사항

- [ ] BE 시작 시 첫 UDP 수신에서 초기 에러 상태 감지
- [ ] 초기 에러와 런타임 에러 구분 마킹
- [ ] DB에 초기 에러 저장 (HardwareErrorLog)
- [ ] WebSocket을 통해 FE에 전달
- [ ] FE 에러 상태바에 표시

## 영향 범위

| 영역 | 파일/컴포넌트 | 변경 내용 |
|------|--------------|----------|
| BE Service | `HardwareErrorLogService.kt` | 초기 에러 감지 로직 추가 |
| BE Entity | `HardwareErrorLog.kt` | `isInitialError` 필드 추가 (선택) |
| BE Repository | `HardwareErrorLogRepository.kt` | (변경 없음) |
| DB | `hardware_error_logs` 테이블 | `is_initial_error` 컬럼 추가 (선택) |
| FE Store | `hardwareErrorLogStore.ts` | 초기 에러 처리 |
| FE Component | 에러 상태바 | 초기 에러 표시 구분 (선택) |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황

## 관련 코드

- 문제 위치: `HardwareErrorLogService.kt:97-113`
- 현재 로직: `previousBits == null` 시 저장만, 에러 체크 안 함