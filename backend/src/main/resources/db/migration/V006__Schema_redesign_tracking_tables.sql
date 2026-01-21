-- =============================================================================
-- V006__Schema_redesign_tracking_tables.sql
-- Tracking 테이블 스키마 재설계
-- =============================================================================
-- 문제:
--   1. tracking_session: 1 Pass = 7 Sessions (data_type별 분리) → 1 Pass = 1 Session
--   2. tracking_trajectory: range, altitude, velocity, train_rate 누락
--   3. tracking_result: 이론치가 잘못 저장됨 → 실측치만 저장
--   4. detail_id: 항상 0 → Pass 구분자로 활용
--
-- 접근법: 클린 리셋 (개발 환경 - 기존 데이터 삭제 후 재구성)
-- =============================================================================

-- =============================================================================
-- Phase 0: 클린 리셋 (데이터 삭제)
-- =============================================================================
-- 참조 무결성 순서: result → trajectory → session
TRUNCATE TABLE tracking_result CASCADE;
TRUNCATE TABLE tracking_trajectory CASCADE;
TRUNCATE TABLE tracking_session CASCADE;

-- =============================================================================
-- Phase 1: tracking_trajectory 확장
-- =============================================================================
-- 누락된 컬럼 추가: 위성 정보 + train_rate
ALTER TABLE tracking_trajectory
    ADD COLUMN IF NOT EXISTS train_rate DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS satellite_range DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS satellite_altitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS satellite_velocity DOUBLE PRECISION;

-- 컬럼 코멘트
COMMENT ON COLUMN tracking_trajectory.train_rate IS 'Train 각속도 (°/s)';
COMMENT ON COLUMN tracking_trajectory.satellite_range IS '위성까지 거리 (km)';
COMMENT ON COLUMN tracking_trajectory.satellite_altitude IS '위성 고도 (km)';
COMMENT ON COLUMN tracking_trajectory.satellite_velocity IS '위성 속도 (km/s)';

-- =============================================================================
-- Phase 2: tracking_session UNIQUE 제약 변경
-- =============================================================================
-- 현재: UNIQUE (mst_id, data_type, tracking_mode) → 1 Pass = 7 Sessions
-- 변경: UNIQUE (mst_id, detail_id, tracking_mode) → 1 Pass = 1 Session

ALTER TABLE tracking_session
    DROP CONSTRAINT IF EXISTS uk_tracking_session;

ALTER TABLE tracking_session
    ADD CONSTRAINT uk_tracking_session UNIQUE (mst_id, detail_id, tracking_mode);

-- data_type 컬럼은 호환성을 위해 유지 (nullable로 변경)
ALTER TABLE tracking_session
    ALTER COLUMN data_type DROP NOT NULL;

-- TLE 연동 컬럼 추가 (FK + 스냅샷)
ALTER TABLE tracking_session
    ADD COLUMN IF NOT EXISTS tle_cache_id BIGINT REFERENCES tle_cache(id),
    ADD COLUMN IF NOT EXISTS tle_line_1 VARCHAR(70),
    ADD COLUMN IF NOT EXISTS tle_line_2 VARCHAR(70),
    ADD COLUMN IF NOT EXISTS tle_epoch TIMESTAMPTZ;

COMMENT ON COLUMN tracking_session.data_type IS '(Deprecated) 데이터 유형 - trajectory에서 관리';
COMMENT ON COLUMN tracking_session.detail_id IS 'Pass 구분자 (동일 위성의 여러 Pass 구분)';
COMMENT ON COLUMN tracking_session.tle_cache_id IS 'TLE 캐시 참조 (FK)';
COMMENT ON COLUMN tracking_session.tle_line_1 IS 'TLE Line 1 스냅샷 (계산 시점)';
COMMENT ON COLUMN tracking_session.tle_line_2 IS 'TLE Line 2 스냅샷 (계산 시점)';
COMMENT ON COLUMN tracking_session.tle_epoch IS 'TLE 에포크 스냅샷';

-- =============================================================================
-- Phase 3: tracking_result 재구성
-- =============================================================================
-- 이론치 컬럼 제거 (trajectory에서 JOIN으로 조회)
ALTER TABLE tracking_result
    DROP COLUMN IF EXISTS original_azimuth,
    DROP COLUMN IF EXISTS original_elevation,
    DROP COLUMN IF EXISTS transformed_azimuth,
    DROP COLUMN IF EXISTS transformed_elevation,
    DROP COLUMN IF EXISTS transformed_train,
    DROP COLUMN IF EXISTS final_azimuth,
    DROP COLUMN IF EXISTS final_elevation,
    DROP COLUMN IF EXISTS final_train,
    DROP COLUMN IF EXISTS azimuth_rate,
    DROP COLUMN IF EXISTS elevation_rate,
    DROP COLUMN IF EXISTS train_rate,
    DROP COLUMN IF EXISTS azimuth_acceleration,
    DROP COLUMN IF EXISTS elevation_acceleration,
    DROP COLUMN IF EXISTS train_acceleration,
    DROP COLUMN IF EXISTS satellite_range,
    DROP COLUMN IF EXISTS satellite_altitude,
    DROP COLUMN IF EXISTS satellite_velocity,
    DROP COLUMN IF EXISTS interpolation_type;

-- 정밀 추적 컬럼 추가 (선형 보간 메타데이터)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS theoretical_timestamp TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS time_offset_ms DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS interpolation_fraction DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS lower_theoretical_index INTEGER,
    ADD COLUMN IF NOT EXISTS upper_theoretical_index INTEGER;

-- ICD 추적 데이터 컬럼 추가 (실측치)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS tracking_azimuth_time REAL,
    ADD COLUMN IF NOT EXISTS tracking_cmd_azimuth REAL,
    ADD COLUMN IF NOT EXISTS tracking_actual_azimuth REAL,
    ADD COLUMN IF NOT EXISTS tracking_elevation_time REAL,
    ADD COLUMN IF NOT EXISTS tracking_cmd_elevation REAL,
    ADD COLUMN IF NOT EXISTS tracking_actual_elevation REAL,
    ADD COLUMN IF NOT EXISTS tracking_train_time REAL,
    ADD COLUMN IF NOT EXISTS tracking_cmd_train REAL,
    ADD COLUMN IF NOT EXISTS tracking_actual_train REAL;

-- 칼만 필터 컬럼 (향후 확장)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS kalman_azimuth DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS kalman_elevation DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS kalman_gain DOUBLE PRECISION;

-- 컬럼 코멘트
COMMENT ON COLUMN tracking_result.theoretical_timestamp IS '매칭된 이론치의 정확한 타임스탬프';
COMMENT ON COLUMN tracking_result.time_offset_ms IS '이론치와 실측치 간 시간 차이 (ms)';
COMMENT ON COLUMN tracking_result.interpolation_fraction IS '선형 보간 비율 (0.0=하한, 1.0=상한)';
COMMENT ON COLUMN tracking_result.interpolation_accuracy IS '보간 정확도 (0.5가 최저)';
COMMENT ON COLUMN tracking_result.lower_theoretical_index IS '보간에 사용된 하한 이론치 인덱스';
COMMENT ON COLUMN tracking_result.upper_theoretical_index IS '보간에 사용된 상한 이론치 인덱스';

COMMENT ON COLUMN tracking_result.tracking_azimuth_time IS 'ICD 방위각 시간 (s)';
COMMENT ON COLUMN tracking_result.tracking_cmd_azimuth IS 'ICD 방위각 명령값 (°)';
COMMENT ON COLUMN tracking_result.tracking_actual_azimuth IS 'ICD 방위각 실측값 (°)';
COMMENT ON COLUMN tracking_result.tracking_elevation_time IS 'ICD 고도각 시간 (s)';
COMMENT ON COLUMN tracking_result.tracking_cmd_elevation IS 'ICD 고도각 명령값 (°)';
COMMENT ON COLUMN tracking_result.tracking_actual_elevation IS 'ICD 고도각 실측값 (°)';
COMMENT ON COLUMN tracking_result.tracking_train_time IS 'ICD Train 시간 (s)';
COMMENT ON COLUMN tracking_result.tracking_cmd_train IS 'ICD Train 명령값 (°)';
COMMENT ON COLUMN tracking_result.tracking_actual_train IS 'ICD Train 실측값 (°)';

COMMENT ON COLUMN tracking_result.kalman_azimuth IS '칼만 필터로 보정된 방위각 추정값 (향후)';
COMMENT ON COLUMN tracking_result.kalman_elevation IS '칼만 필터로 보정된 고도각 추정값 (향후)';
COMMENT ON COLUMN tracking_result.kalman_gain IS '칼만 이득 - 측정값 신뢰도 (0=예측, 1=측정)';

-- =============================================================================
-- Phase 4: 인덱스 추가
-- =============================================================================
-- 정밀 추적 조회용
CREATE INDEX IF NOT EXISTS idx_tr_theoretical_ts
    ON tracking_result(session_id, theoretical_timestamp);

-- trajectory data_type 조회용
CREATE INDEX IF NOT EXISTS idx_tt_data_type
    ON tracking_trajectory(session_id, data_type);

-- =============================================================================
-- 스키마 변경 요약
-- =============================================================================
-- | 테이블             | 변경 내용                                    |
-- |--------------------|---------------------------------------------|
-- | tracking_session   | UNIQUE 변경: data_type → detail_id          |
-- | tracking_trajectory| +4컬럼: train_rate, range, altitude, velocity|
-- | tracking_result    | -17컬럼(이론치), +15컬럼(ICD+정밀추적+칼만)  |
-- =============================================================================