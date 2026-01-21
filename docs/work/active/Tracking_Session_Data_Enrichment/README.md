# Tracking_Session_Data_Enrichment

## 개요

**목적**: tracking_session 테이블의 데이터 완성도 향상 (빈 컬럼 채우기 + 분석용 메타데이터 확장)
**요청일**: 2026-01-20
**상태**: ✅ 구현 완료, DB 테스트 대기

## 요구사항

### Phase 1: 빈 컬럼 채우기 (완료)
- [x] satellite_id - SatelliteID/SatelliteId 양쪽 키 지원
- [x] duration - ISO String 파싱 + 시간차 계산
- [x] max_azimuth_rate - MaxAzRate/MaxAzimuthRate 양쪽 키 지원
- [x] max_elevation_rate - MaxElRate/MaxElevationRate 양쪽 키 지원
- [x] total_points - DTL 개수 폴백 추가

### Phase 2: 분석용 메타데이터 확장 (완료)
Select Schedule에서 표시하는 모든 변환 단계별 데이터를 DB에 영속화

- [x] 기본 각도 정보 (5개): start/end azimuth/elevation, train_angle
- [x] 가속도/시간 정보 (3개): max_azimuth_accel, max_elevation_accel, max_elevation_time
- [x] Original 2축 (5개): original_start/end_azimuth, original_max_elevation/az_rate/el_rate
- [x] FinalTransformed 3축 T=0 (7개): final_* 컬럼들
- [x] KeyholeAxisTransformed (2개): keyhole_axis_max_az/el_rate
- [x] KeyholeFinalTransformed (7개): keyhole_final_* 컬럼들
- [x] KeyholeOptimizedFinalTransformed (7개): keyhole_opt_* 컬럼들

**총 37개 신규 컬럼 추가 (V004 마이그레이션)**

### Phase 5: 인덱스 최적화 (완료)
조회 성능 향상을 위한 인덱스 추가

- [x] V005 마이그레이션 작성 (인덱스 4개) ✅
- [x] BE 빌드 확인 ✅
- [ ] Flyway 적용 확인 (BE 재시작 필요)

**추가 인덱스 (V005 마이그레이션)**

### Phase 6: Ephemeris TLE 캐시 저장 버그 수정 (완료)
Ephemeris 모드에서 TLE Text 업로드 시 `tle_cache` 테이블에 저장되지 않는 버그

**근본 원인**: `EphemerisService.addSatelliteTle()` 메서드가 존재하지만 호출되지 않음

| 수정 전 | 수정 후 |
|---------|---------|
| FE → generateEphemerisTrack API → 궤도 계산만 | FE → API → **TLE 캐시 저장** → 궤도 계산 |

- [x] EphemerisService.generateEphemerisDesignationTrackSync()에 TLE 캐시 저장 호출 추가 ✅
- [x] BE 빌드 확인 ✅
- [ ] TLE 저장 테스트 (BE 재시작 후)

**추가 인덱스 (V005 마이그레이션)**
| 인덱스명 | 컬럼 | 용도 |
|---------|------|------|
| idx_session_mst_datatype | (mst_id, data_type) | Pass 그룹 조회, View JOIN |
| idx_session_satellite_time | (satellite_id, start_time DESC) | 위성별 히스토리 |
| idx_session_start_time | (start_time DESC) | 시간 범위 조회 |
| idx_session_mode_time | (tracking_mode, start_time DESC) | 모드별 필터링 |

## 변환 파이프라인

```
Original (2축)
  → FinalTransformed (3축, Train=0, ±270°)
    → KeyholeAxisTransformed (3축, Train≠0) [Keyhole 발생 시]
      → KeyholeFinalTransformed (3축, Train≠0, ±270°)
        → KeyholeOptimizedFinalTransformed (최적화 Train, ±270°)
```

## 영향 범위

| 영역 | 파일/컴포넌트 | 변경 내용 |
|------|--------------|----------|
| DB | V004__Add_tracking_session_full_metadata.sql | 37개 컬럼 추가 |
| DB | V005__Add_tracking_session_indexes.sql | 인덱스 4개 추가 |
| Backend | TrackingSessionEntity.kt | 37개 필드 추가 |
| Backend | PassScheduleDataRepository.kt | mapMstToSession() 전체 매핑 |
| Backend | EphemerisDataRepository.kt | mapMstToSession() 키 이름 수정 |
| Backend | TrackingTrajectoryRepository.kt | findBySessionIdAndDetailId/DataType 메서드 추가 |
| Backend | EphemerisService.kt | TLE 캐시 저장 호출 추가 (Phase 6) |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황
