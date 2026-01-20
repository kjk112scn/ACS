# HW_Error_System_Integration

## 개요

**목적**: hardware_error_log 테이블의 빈 컬럼들을 시스템과 연계하여 의미있는 데이터로 채움
**요청일**: 2026-01-20
**상태**: 🚧 진행중

## 요구사항

- [x] 전문가 검토 완료 (architect, tech-lead, debugger)
- [x] tracking_mode - 에러 발생 시 현재 추적 모드 저장
- [x] correlation_id - 동시 발생 에러 그룹화
- [x] raw_data - 에러 관련 비트 데이터 JSON 저장
- [ ] session_id - 추적 세션 ID 연계 (보류 - 복잡도 높음)

## 영향 범위

| 영역 | 파일/컴포넌트 | 변경 내용 |
|------|--------------|----------|
| Backend | HardwareErrorLogService.kt | DataStoreService 의존성 추가, 컬럼 연계 로직 |
| Backend | HardwareErrorLog (data class) | rawData, correlationId 필드 추가 |
| DB | hardware_error_log | 변경 없음 (컬럼 이미 존재) |
| Frontend | - | 변경 없음 |

## 전문가 검토 결과

### architect
- DataStoreService 의존성 추가 권장 (순환 참조 없음)
- raw_data JSON 스키마 정의 필요
- correlation_id: processAntennaData() 호출 단위 적절

### tech-lead
- 구현 우선순위: tracking_mode > correlation_id > raw_data
- session_id는 복잡도 높아 보류 권장
- 성능 영향 미미 (AtomicReference 읽기, UUID 생성)

### debugger
- isInitialError 정상 동작 확인
- raw_data 캡처 시점: analyzeBitChanges() 내에서 currentBits, previousBits 모두 available
- correlation_id: analyzeInitialErrors()도 동일 ID 공유 필요

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황