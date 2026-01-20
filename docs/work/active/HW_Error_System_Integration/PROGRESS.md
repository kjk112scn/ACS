# HW_Error_System_Integration 진행 상황

## 진행률: 90%

## 작업 체크리스트

### Phase 1: 준비
- [x] 전문가 검토 (architect, tech-lead, debugger)
- [x] 설계 문서 작성
- [x] 폴더 및 문서 생성

### Phase 2: 구현
- [x] HardwareErrorLog data class에 필드 추가 (rawData, correlationId, trackingMode)
- [x] HardwareErrorLogService에 DataStoreService 의존성 추가
- [x] processAntennaData()에서 correlationId 생성
- [x] processAntennaData()에서 trackingMode 조회
- [x] analyzeBitChanges()에서 rawData JSON 생성
- [x] analyzeInitialErrors()에서 rawData JSON 생성
- [x] buildRawDataJson() 함수 구현
- [x] mapErrorLogToEntity() 매핑 업데이트
- [x] mapEntityToErrorLog() 매핑 업데이트

### Phase 3: 검증
- [x] BE 빌드 확인 (BUILD SUCCESSFUL)
- [ ] 에러 발생 테스트 (비트 변화)
- [ ] DB 저장 데이터 확인 (tracking_mode, correlation_id, raw_data 값 확인)
- [ ] isInitialError 동작 확인

## 구현 상세

### 변경된 파일
| 파일 | 변경 내용 |
|------|----------|
| HardwareErrorLogService.kt | DataStoreService 의존성 추가, correlationId/trackingMode/rawData 처리 |
| HardwareErrorLog data class | rawData, correlationId, trackingMode 필드 추가 |

### raw_data JSON 스키마
```json
{
  "version": "1.0",
  "bitType": "elevationBoardServoStatusBits",
  "currentBits": "00100000",
  "previousBits": "00000000",
  "changedPosition": 5,
  "changeType": "0→1",
  "antennaState": {
    "azimuth": 45.5,
    "elevation": 30.2,
    "train": 0.0
  }
}
```

### correlation_id 동작
- 동일 processAntennaData() 호출에서 2개 이상 에러 발생 시 동일 UUID 공유
- 단독 에러는 null 유지

### tracking_mode 값
- `"ephemeris"` - 위성 추적 중
- `"passSchedule"` - 패스 스케줄 실행 중
- `"sunTrack"` - 태양 추적 중
- `null` - 대기 상태

## 일일 로그

### 2026-01-20
- 전문가 검토 완료 (architect, tech-lead, debugger)
- feature 폴더 생성: `docs/work/active/HW_Error_System_Integration/`
- README.md, DESIGN.md, PROGRESS.md 작성
- 구현 우선순위 결정: tracking_mode > correlation_id > raw_data > session_id(보류)
- **구현 완료:**
  - DataStoreService 의존성 주입
  - buildRawDataJson() 함수 추가
  - HardwareErrorLog data class 필드 추가
  - mapErrorLogToEntity() / mapEntityToErrorLog() 매핑 업데이트
- BE 빌드 성공 확인
