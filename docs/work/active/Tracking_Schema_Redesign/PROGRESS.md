# Tracking Schema Redesign 진행 상황

## 진행률: 90% (V006 완료, P2+P3 수정 완료, 검증 대기)

## ✅ 수정 완료된 이슈

| 이슈 | 심각도 | 상태 |
|------|--------|------|
| **sessionId 연동 버그** - tracking_result에 session_id=0 저장 | 🔴 CRITICAL | ✅ 수정 완료 |
| **trackingMode 불일치** - "ephemeris_designation" vs "EPHEMERIS" | 🔴 CRITICAL | ✅ 수정 완료 |
| **PassSchedule V006 미반영** - 1 Pass = 7 Sessions | 🟡 HIGH | ✅ 수정 완료 |
| **PassSchedule sessionId 조회** - 메서드 누락 | 🟡 HIGH | ✅ 수정 완료 |

## ✅ 신규 발견 이슈 수정 완료 (2026-01-22 오후)

| 이슈 | 심각도 | 상태 |
|------|--------|------|
| **P2: mst_id = detail_id** - startMstId 미전달 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-1: mst_id 의미 오류** - 동일 위성에 다른 mstId | 🔴 CRITICAL | ✅ 수정 완료 |
| **P2-2: LimitAngleCalculator 그룹화** - MstId만으로 그룹화 | 🟡 HIGH | ✅ 수정 완료 |
| **P2-3: calculateMaxAzRate() DetailId 누락** | 🟡 HIGH | ✅ 수정 완료 |
| **P2-4: validateConversion() 그룹화 오류** | 🟡 HIGH | ✅ 수정 완료 |
| **P2-5: No → Index 필드명 불일치** | 🟡 MEDIUM | ✅ 수정 완료 |
| **P3: TLE 컬럼 NULL** - MST 데이터에 TLE 미포함 | 🟡 HIGH | ✅ 수정 완료 |
| **P4: 7가지 DataType 누락** - original만 저장됨 | 🔴 CRITICAL | ✅ 오인 (정상) |

상세: [FIX.md](FIX.md) §P2-P4

상세: [FIX.md](FIX.md), [DEEP_REVIEW_V007.md §11](DEEP_REVIEW_V007.md#11-발견된-이슈-critical)

## 작업 체크리스트

### Phase 0: V006 마이그레이션 작성 ✅
- [x] tracking_trajectory 컬럼 추가 설계
- [x] tracking_session UNIQUE 제약 변경 설계
- [x] tracking_session TLE 연동 컬럼 추가 (FK + 스냅샷)
- [x] tracking_result 컬럼 재구성 설계
- [x] V006__Schema_redesign_tracking_tables.sql 작성

### Phase 1: Entity 업데이트 ✅
- [x] TrackingSessionEntity.kt (+4 필드: tle_cache_id, tle_line_1, tle_line_2, tle_epoch)
- [x] TrackingTrajectoryEntity.kt (+4 필드: train_rate, satellite_range, satellite_altitude, satellite_velocity)
- [x] TrackingResultEntity.kt (-17 필드, +15 필드)

### Phase 2: Repository 매핑 수정 ✅
- [x] TrackingResultRepository.kt - SQL INSERT/SELECT 재작성
- [x] TrackingResultRepository.kt - mapRowToEntity() 재작성
- [x] BatchStorageManager.kt - mapToTrackingResult() 재작성

### Phase 3: 검증 ✅
- [x] BE 빌드 확인 (BUILD SUCCESSFUL)

### Phase 4: 심층 검토 ✅
- [x] 3개 테이블 연계 구조 검토
- [x] 7가지 DataType 설계 확정
- [x] TimescaleDB 호환성 검증
- [x] PassSchedule 다중 위성/스케줄 시나리오 검증
- [x] [DEEP_REVIEW_V007.md](DEEP_REVIEW_V007.md) 문서화

### Phase 5: 버그 수정 ✅
- [x] **[P0] sessionId 연동 버그**
  - [x] EphemerisDataRepository: getSessionIdByMstAndDetail() 추가
  - [x] EphemerisService: createRealtimeTrackingData()에 sessionId 조회/추가
  - [x] trackingMode 기본값 수정 ("ephemeris_designation" → "EPHEMERIS")
- [x] **[P1] PassSchedule V006 정책 적용**
  - [x] PassScheduleDataRepository: (mstId, detailId) 그룹화 로직 추가
  - [x] PassScheduleDataRepository: saveOrUpdateSession() UPSERT 로직
  - [x] PassScheduleDataRepository: getSessionIdByMstAndDetail() 추가

### Phase 6: V007 마이그레이션 (대기) ⏳
- [ ] position 컬럼 제거 (중복)
- [ ] error 컬럼 제거 (파생값)
- [ ] used_data_type 컬럼 추가

### Phase 7: BE 수정 (대기) ⏳
- [ ] TrackingResultEntity에 usedDataType 추가
- [ ] Repository SQL 수정
- [ ] BatchStorageManager usedDataType 매핑 추가

### Phase 8: DB 적용 (대기) ⏳
- [ ] BE 재시작 (Flyway V006+V007 적용)
- [ ] DB 스키마 검증
- [ ] 데이터 저장 테스트

## 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| V006__Schema_redesign_tracking_tables.sql | +TLE 컬럼, UNIQUE 변경 |
| TrackingSessionEntity.kt | +4 TLE 필드 |
| TrackingTrajectoryEntity.kt | +4 satellite 필드 |
| TrackingResultEntity.kt | -17 이론치, +15 ICD/정밀추적 |
| TrackingResultRepository.kt | SQL/매핑 전체 재작성 |
| BatchStorageManager.kt | mapToTrackingResult() 재작성 |
| **EphemerisDataRepository.kt** | +getSessionIdByMstAndDetail(), trackingMode 수정 |
| **EphemerisService.kt** | createRealtimeTrackingData()에 sessionId 추가, **P2: mstIdCounter + startMstId**, **P2-1: getAndAdd(1)** |
| **PassScheduleDataRepository.kt** | V006 그룹화 로직, saveOrUpdateSession, getSessionIdByMstAndDetail |
| **PassScheduleService.kt** | **P2-1: mstIdCounter.getAndAdd(1) 수정 (2곳)** |
| **SatelliteTrackingProcessor.kt** | **P3: TleLine1, TleLine2 추가**, **P2-1: mstId = startMstId**, **P2-3: DetailId + Index 추가** |
| **LimitAngleCalculator.kt** | **P2-2: (MstId, DetailId) 그룹화**, **P2-4: validateConversion 그룹화**, **P2-5: No→Index** |

## 일일 로그

### 2026-01-22 (오후 - P2/P3/P2-1~P2-5 수정)
- **P2 수정 완료**: mst_id = detail_id 문제
  - EphemerisService.kt에 mstIdCounter(AtomicLong) 추가
  - processFullTransformation() 호출 시 startMstId 전달
- **P2-1 수정 완료**: mst_id 의미 오류 (🔴 CRITICAL)
  - 증상: 동일 위성(AQUA)의 각 패스가 다른 mst_id를 가짐 (1,2,3,4,5,6)
  - 원인: mst_id를 "각 패스의 전역 고유 ID"로 잘못 이해
  - 올바른 설계: mst_id=위성별 그룹 ID, detail_id=Pass 구분자
  - 수정 1: SatelliteTrackingProcessor.kt - `val mstId = startMstId` (index 제거)
  - 수정 2: EphemerisService.kt - `mstIdCounter.getAndAdd(1)` (passCount 대신 1)
  - 수정 3: PassScheduleService.kt - 동일 수정 (2곳)
- **P2-2 수정 완료**: LimitAngleCalculator "큰 회전 감지" 대량 발생 (🟡 HIGH)
  - 원인: P2-1 수정 후 동일 MstId를 가진 여러 패스의 데이터가 하나로 병합됨
  - 수정: LimitAngleCalculator.kt - `(MstId, DetailId)` 쌍으로 그룹화
- **P2-3 수정 완료**: calculateMaxAzRateForTrainAngle() DetailId 누락 (🟡 HIGH)
  - 원인: LimitAngleCalculator에 데이터 전달 시 DetailId 누락
  - 수정: SatelliteTrackingProcessor.kt - DetailId 추가, "No" → "Index" 변경
- **P2-4 수정 완료**: validateConversion() 그룹화 오류 (🟡 HIGH)
  - 원인: validateConversion()에서 MstId만으로 그룹화
  - 수정: LimitAngleCalculator.kt L510 - (MstId, DetailId) 쌍으로 그룹화
- **P2-5 수정 완료**: No → Index 필드명 불일치 (🟡 MEDIUM)
  - 수정: LimitAngleCalculator.kt L467, L713 - "No" → "Index" 변경
- **P3 수정 완료**: TLE 컬럼 NULL 문제
  - SatelliteTrackingProcessor.kt의 structureOriginalData()에 TleLine1, TleLine2 추가
- **P4 해결**: 7가지 DataType 누락 → DB 검증 결과 정상 동작 확인
  - original: 33,314개, axis_transformed: 33,314개, final_transformed: 33,314개
- BE 빌드 성공 ✅

### 2026-01-22 (오전)
- **Phase 5 완료**: 버그 수정 ✅
  - **P0 sessionId 연동**: EphemerisDataRepository.getSessionIdByMstAndDetail() 추가
  - **P0-1 trackingMode 불일치**: 기본값 "EPHEMERIS"로 수정
  - **P1 PassSchedule 그룹화**: (mstId, detailId) 그룹화 + saveOrUpdateSession UPSERT
  - **P1-1 PassSchedule sessionId**: getSessionIdByMstAndDetail() 추가
  - BE 빌드 성공
- **Phase 4 완료**: 심층 검토
  - 3개 테이블 연계 구조 검증
  - 7가지 DataType 정의 및 파이프라인 문서화
  - TimescaleDB 호환성 확인
- **🔴 CRITICAL 버그 발견 및 수정**: sessionId 연동
  - tracking_result.session_id = 0 으로 저장됨 → 수정 완료
  - createRealtimeTrackingData()에서 sessionId 누락 → sessionId 추가
  - trackingMode 불일치 발견 → "EPHEMERIS"로 수정
- **🟡 HIGH 이슈 발견 및 수정**: PassSchedule V006 미반영
  - EphemerisDataRepository: V006 정책 적용됨
  - PassScheduleDataRepository: 그룹화 로직 추가 완료
- DEEP_REVIEW_V007.md 작성 (§11-13 이슈 추가)
- FIX.md 작성 (버그 수정 계획 및 완료 내역)

### 2026-01-21
- V006 마이그레이션 작성 완료
- TLE 연동 방식 결정: FK + 스냅샷 (하이브리드)
  - tle_cache_id: FK로 tle_cache 참조
  - tle_line_1, tle_line_2, tle_epoch: 계산 시점 스냅샷
- 문서 정리: planned → active 이동
- README.md, PROGRESS.md 업데이트
- **Phase 1 완료**: Entity 3개 업데이트
- **Phase 2 완료**: Repository/BatchStorageManager 매핑 수정
- **BE 빌드 성공**: 컴파일 에러 0개