# Tracking_Session_Data_Enrichment 진행 상황

## 진행률: 98% ⏳

## 작업 체크리스트

### Phase 1: 빈 컬럼 채우기
- [x] 요구사항 분석
- [x] 근본 원인 분석 (키 이름 불일치 발견)
- [x] EphemerisDataRepository mapMstToSession() 수정
- [x] PassScheduleDataRepository mapMstToSession() 수정
- [x] parseDurationToSeconds() 함수 추가

### Phase 2: 분석용 메타데이터 확장
- [x] 전문가 분석 (FE 51필드 vs DB 17컬럼 불일치 확인)
- [x] V004 마이그레이션 작성 (37개 컬럼 추가)
- [x] TrackingSessionEntity 필드 추가 (37개)
- [x] PassScheduleDataRepository 저장 로직 수정 (전체 매핑)

### Phase 3: Repository 개선
- [x] TrackingTrajectoryRepository.findBySessionIdAndDetailId() 추가
- [x] TrackingTrajectoryRepository.findBySessionIdAndDataType() 추가
- [x] mapRowToEntity() 헬퍼 함수 추가

### Phase 4: 검증
- [x] BE 빌드 확인 ✅
- [ ] Flyway V004, V005 적용 확인 (BE 재시작 필요)
- [ ] DB 저장 테스트

### Phase 5: 인덱스 최적화
- [x] V005 마이그레이션 작성 (인덱스 4개) ✅
- [x] BE 빌드 확인 ✅

### Phase 6: Ephemeris TLE 캐시 저장 버그
- [x] 근본 원인 분석 (addSatelliteTle() 미호출)
- [x] EphemerisService에 TLE 캐시 저장 호출 추가 ✅
- [x] BE 빌드 확인 ✅
- [ ] TLE 저장 테스트 (BE 재시작 후)

## 일일 로그

### 2026-01-21
- **Phase 6 완료**: Ephemeris TLE 캐시 저장 버그 수정
  - 근본 원인: `addSatelliteTle()` 메서드 미호출
  - generateEphemerisDesignationTrackSync()에서 TLE 캐시 저장 로직 누락
  - **수정**: `generateEphemerisDesignationTrackSync()` 시작 부분에 TLE 캐시 저장 호출 추가
  - BE 빌드 성공
- **Phase 5 추가**: 인덱스 최적화 요구사항
- V005__Add_tracking_session_indexes.sql 생성 (인덱스 4개)
  - idx_session_mst_datatype (mst_id, data_type)
  - idx_session_satellite_time (satellite_id, start_time DESC)
  - idx_session_start_time (start_time DESC)
  - idx_session_mode_time (tracking_mode, start_time DESC)
- BE 빌드 성공
- **Phase 2 추가**: Select Schedule 데이터 전체 저장 요구사항
- 전문가 분석 수행:
  - DB tracking_session: 17개 컬럼
  - FE ScheduleItem: 51개 필드
  - 매칭: 10개, 미저장: 41개
- V004__Add_tracking_session_full_metadata.sql 생성 (37개 컬럼)
- TrackingSessionEntity.kt 37개 필드 추가
- PassScheduleDataRepository.kt 전체 매핑 로직 작성
- **Phase 3 추가**: Repository 개선
- TrackingTrajectoryRepository에 조회 메서드 2개 추가:
  - `findBySessionIdAndDetailId(sessionId, detailId)`
  - `findBySessionIdAndDataType(sessionId, dataType)`
- `mapRowToEntity()` 헬퍼 함수 추가
- BE 빌드 성공

### 2026-01-20
- Feature 폴더 생성
- README.md, DESIGN.md, PROGRESS.md 작성
- 빈 컬럼 5개 식별: satellite_id, duration, max_azimuth_rate, max_elevation_rate, total_points
- **근본 원인 발견**: MST 데이터와 Repository 매핑 간 키 이름 불일치
  - `SatelliteID` vs `SatelliteId`
  - `MaxAzRate` vs `MaxAzimuthRate`
  - `MaxElRate` vs `MaxElevationRate`
  - `IsKeyhole` vs `KeyholeDetected`
  - `Duration` (ISO String) vs (Number)
- EphemerisDataRepository.kt 수정 완료
- PassScheduleDataRepository.kt 수정 완료
- BE 빌드 성공
