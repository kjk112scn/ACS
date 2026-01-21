-- =============================================================================
-- V006__Add_precision_tracking_columns.sql
-- 정밀 추적 DB 확장 (선형 보간, 칼만 필터 지원)
-- =============================================================================
-- 목적: 이론치-실측치 매칭 정밀도 향상 (0.05° → 0.01°)
-- 적용 조건: 시스템 안정화 후 (Tracking_Session_Data_Enrichment 완료 후)
--
-- 주의사항:
-- 1. TimescaleDB Hypertable의 압축된 청크가 있으면 먼저 해제 필요
-- 2. 모든 컬럼은 NULL 허용으로 기존 데이터와 호환
-- 3. resolution_ms는 DEFAULT 1000 (현재 1초 주기 기준)
-- =============================================================================

-- =============================================================================
-- 0. 압축 청크 처리 (필요시 주석 해제)
-- =============================================================================
-- 압축 정책 일시 비활성화
-- SELECT remove_compression_policy('tracking_result');
-- SELECT remove_compression_policy('tracking_trajectory');

-- 압축된 청크 해제 (데이터 유지)
-- DO $$
-- DECLARE
--     chunk_rec RECORD;
-- BEGIN
--     FOR chunk_rec IN
--         SELECT chunk_schema || '.' || chunk_name as chunk_full_name
--         FROM timescaledb_information.chunks
--         WHERE hypertable_name IN ('tracking_result', 'tracking_trajectory')
--           AND is_compressed = true
--     LOOP
--         EXECUTE 'SELECT decompress_chunk(''' || chunk_rec.chunk_full_name || ''')';
--         RAISE NOTICE 'Decompressed: %', chunk_rec.chunk_full_name;
--     END LOOP;
-- END $$;

-- =============================================================================
-- 1. tracking_result 확장 (보간 상세)
-- =============================================================================
-- 이론치 매칭 시간
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS theoretical_timestamp TIMESTAMPTZ;

-- 시간 오프셋 (ms)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS time_offset_ms DOUBLE PRECISION;

-- 보간 비율 (0.0 ~ 1.0)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS interpolation_fraction DOUBLE PRECISION;

-- 보간 인덱스 범위
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS lower_theoretical_index INTEGER;

ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS upper_theoretical_index INTEGER;

-- =============================================================================
-- 2. tracking_result 확장 (칼만 필터 - 향후)
-- =============================================================================
-- 칼만 보정 방위각
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS kalman_azimuth DOUBLE PRECISION;

-- 칼만 보정 고도각
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS kalman_elevation DOUBLE PRECISION;

-- 칼만 이득 (0 ~ 1)
ALTER TABLE tracking_result
    ADD COLUMN IF NOT EXISTS kalman_gain DOUBLE PRECISION;

-- =============================================================================
-- 3. tracking_trajectory 확장 (고해상도 + 위성 정보)
-- =============================================================================
-- 데이터 해상도 (ms) - 기본 1000ms (1초)
ALTER TABLE tracking_trajectory
    ADD COLUMN IF NOT EXISTS resolution_ms INTEGER DEFAULT 1000;

-- 위성까지 거리 (km)
ALTER TABLE tracking_trajectory
    ADD COLUMN IF NOT EXISTS satellite_range DOUBLE PRECISION;

-- 위성 고도 (km)
ALTER TABLE tracking_trajectory
    ADD COLUMN IF NOT EXISTS satellite_altitude DOUBLE PRECISION;

-- =============================================================================
-- 4. 컬럼 코멘트
-- =============================================================================
COMMENT ON COLUMN tracking_result.theoretical_timestamp
    IS '매칭된 이론치의 정확한 타임스탬프';

COMMENT ON COLUMN tracking_result.time_offset_ms
    IS '이론치와 실측치 간 시간 차이 (밀리초)';

COMMENT ON COLUMN tracking_result.interpolation_fraction
    IS '선형 보간 비율 (0.0=하한, 1.0=상한)';

COMMENT ON COLUMN tracking_result.lower_theoretical_index
    IS '보간에 사용된 하한 이론치 인덱스';

COMMENT ON COLUMN tracking_result.upper_theoretical_index
    IS '보간에 사용된 상한 이론치 인덱스';

COMMENT ON COLUMN tracking_result.kalman_azimuth
    IS '칼만 필터로 보정된 방위각 추정값 (향후)';

COMMENT ON COLUMN tracking_result.kalman_elevation
    IS '칼만 필터로 보정된 고도각 추정값 (향후)';

COMMENT ON COLUMN tracking_result.kalman_gain
    IS '칼만 이득 - 측정값 신뢰도 (0=예측 신뢰, 1=측정 신뢰)';

COMMENT ON COLUMN tracking_trajectory.resolution_ms
    IS '이론치 데이터 해상도 (밀리초, 기본 1000ms)';

COMMENT ON COLUMN tracking_trajectory.satellite_range
    IS '위성까지 거리 (km)';

COMMENT ON COLUMN tracking_trajectory.satellite_altitude
    IS '위성 고도 (km)';

-- =============================================================================
-- 5. 인덱스 추가 (선택적)
-- =============================================================================
-- 시간 기반 조회 최적화
CREATE INDEX IF NOT EXISTS idx_tr_theoretical_ts
    ON tracking_result(session_id, theoretical_timestamp);

-- 해상도별 조회
CREATE INDEX IF NOT EXISTS idx_tt_resolution
    ON tracking_trajectory(session_id, resolution_ms);

-- =============================================================================
-- 6. 압축 정책 재활성화 (0번 사용 시)
-- =============================================================================
-- SELECT add_compression_policy('tracking_result', INTERVAL '7 days');
-- SELECT add_compression_policy('tracking_trajectory', INTERVAL '7 days');

-- =============================================================================
-- 검증 쿼리
-- =============================================================================
-- 새 컬럼 확인
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'tracking_result'
--   AND column_name IN ('theoretical_timestamp', 'time_offset_ms',
--                       'interpolation_fraction', 'kalman_azimuth');

-- SELECT column_name, data_type, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'tracking_trajectory'
--   AND column_name IN ('resolution_ms', 'satellite_range', 'satellite_altitude');