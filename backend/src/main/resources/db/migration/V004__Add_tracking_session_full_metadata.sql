-- =============================================================================
-- V004: tracking_session에 전체 메타데이터 컬럼 추가
-- =============================================================================
-- 목적: Select Schedule에서 표시하는 모든 변환 단계별 데이터를 DB에 저장
-- 배경: 2축/3축/Keyhole 변환 결과를 분석용으로 영속화
--
-- 변환 파이프라인:
--   Original (2축)
--     → AxisTransformed (3축, Train=0)
--       → FinalTransformed (3축, Train=0, ±270°)
--         → KeyholeAxisTransformed (3축, Train≠0) [Keyhole 발생 시]
--           → KeyholeFinalTransformed (3축, Train≠0, ±270°)
--             → KeyholeOptimizedFinalTransformed (최적화 Train, ±270°)
-- =============================================================================

-- ===== 1. 기본 각도 정보 (최종 사용값) =====
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS start_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS end_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS start_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS end_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS train_angle DOUBLE PRECISION;

-- ===== 2. 가속도 정보 =====
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS max_azimuth_accel DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS max_elevation_accel DOUBLE PRECISION;

-- ===== 3. 추가 시간 정보 =====
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS max_elevation_time TIMESTAMPTZ;

-- ===== 4. Original (2축) 메타데이터 =====
-- 원본 궤도 데이터 (좌표 변환 전)
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS original_start_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS original_end_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS original_max_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS original_max_az_rate DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS original_max_el_rate DOUBLE PRECISION;

-- ===== 5. FinalTransformed (3축, Train=0, ±270°) =====
-- 3축 변환 + 각도 제한 적용 (Train=0 기준)
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_start_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_end_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_start_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_end_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_max_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_max_az_rate DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS final_max_el_rate DOUBLE PRECISION;

-- ===== 6. KeyholeAxisTransformed (3축, Train≠0) =====
-- Keyhole 발생 시 Train 각도 적용 (각도 제한 전)
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_axis_max_az_rate DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_axis_max_el_rate DOUBLE PRECISION;

-- ===== 7. KeyholeFinalTransformed (3축, Train≠0, ±270°) =====
-- Keyhole Train 적용 + 각도 제한
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_start_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_end_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_start_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_end_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_max_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_max_az_rate DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_final_max_el_rate DOUBLE PRECISION;

-- ===== 8. KeyholeOptimizedFinalTransformed (최적화 Train, ±270°) =====
-- 최적화된 Train 각도로 최종 변환 (실제 추적에 사용)
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_start_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_end_azimuth DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_start_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_end_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_max_elevation DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_max_az_rate DOUBLE PRECISION;
ALTER TABLE tracking_session ADD COLUMN IF NOT EXISTS keyhole_opt_max_el_rate DOUBLE PRECISION;

-- ===== 컬럼 코멘트 =====
COMMENT ON COLUMN tracking_session.start_azimuth IS '시작 방위각 (최종 사용값, 도)';
COMMENT ON COLUMN tracking_session.end_azimuth IS '종료 방위각 (최종 사용값, 도)';
COMMENT ON COLUMN tracking_session.start_elevation IS '시작 고각 (최종 사용값, 도)';
COMMENT ON COLUMN tracking_session.end_elevation IS '종료 고각 (최종 사용값, 도)';
COMMENT ON COLUMN tracking_session.train_angle IS '사용된 Train 각도 (도)';
COMMENT ON COLUMN tracking_session.max_azimuth_accel IS '최대 방위각 가속도 (도/s²)';
COMMENT ON COLUMN tracking_session.max_elevation_accel IS '최대 고각 가속도 (도/s²)';
COMMENT ON COLUMN tracking_session.max_elevation_time IS '최대 고각 도달 시간';

COMMENT ON COLUMN tracking_session.original_start_azimuth IS '[2축] 원본 시작 방위각';
COMMENT ON COLUMN tracking_session.original_end_azimuth IS '[2축] 원본 종료 방위각';
COMMENT ON COLUMN tracking_session.original_max_elevation IS '[2축] 원본 최대 고각';
COMMENT ON COLUMN tracking_session.original_max_az_rate IS '[2축] 원본 최대 방위각 속도';
COMMENT ON COLUMN tracking_session.original_max_el_rate IS '[2축] 원본 최대 고각 속도';

COMMENT ON COLUMN tracking_session.final_start_azimuth IS '[3축 T=0] 시작 방위각 (±270° 제한)';
COMMENT ON COLUMN tracking_session.final_end_azimuth IS '[3축 T=0] 종료 방위각 (±270° 제한)';
COMMENT ON COLUMN tracking_session.final_start_elevation IS '[3축 T=0] 시작 고각';
COMMENT ON COLUMN tracking_session.final_end_elevation IS '[3축 T=0] 종료 고각';
COMMENT ON COLUMN tracking_session.final_max_elevation IS '[3축 T=0] 최대 고각';
COMMENT ON COLUMN tracking_session.final_max_az_rate IS '[3축 T=0] 최대 방위각 속도';
COMMENT ON COLUMN tracking_session.final_max_el_rate IS '[3축 T=0] 최대 고각 속도';

COMMENT ON COLUMN tracking_session.keyhole_axis_max_az_rate IS '[Keyhole 3축] 최대 방위각 속도 (각도 제한 전)';
COMMENT ON COLUMN tracking_session.keyhole_axis_max_el_rate IS '[Keyhole 3축] 최대 고각 속도 (각도 제한 전)';

COMMENT ON COLUMN tracking_session.keyhole_final_start_azimuth IS '[Keyhole ±270°] 시작 방위각';
COMMENT ON COLUMN tracking_session.keyhole_final_end_azimuth IS '[Keyhole ±270°] 종료 방위각';
COMMENT ON COLUMN tracking_session.keyhole_final_start_elevation IS '[Keyhole ±270°] 시작 고각';
COMMENT ON COLUMN tracking_session.keyhole_final_end_elevation IS '[Keyhole ±270°] 종료 고각';
COMMENT ON COLUMN tracking_session.keyhole_final_max_elevation IS '[Keyhole ±270°] 최대 고각';
COMMENT ON COLUMN tracking_session.keyhole_final_max_az_rate IS '[Keyhole ±270°] 최대 방위각 속도';
COMMENT ON COLUMN tracking_session.keyhole_final_max_el_rate IS '[Keyhole ±270°] 최대 고각 속도';

COMMENT ON COLUMN tracking_session.keyhole_opt_start_azimuth IS '[최적화] 시작 방위각 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_end_azimuth IS '[최적화] 종료 방위각 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_start_elevation IS '[최적화] 시작 고각 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_end_elevation IS '[최적화] 종료 고각 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_max_elevation IS '[최적화] 최대 고각 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_max_az_rate IS '[최적화] 최대 방위각 속도 (실제 추적용)';
COMMENT ON COLUMN tracking_session.keyhole_opt_max_el_rate IS '[최적화] 최대 고각 속도 (실제 추적용)';

-- ===== 인덱스 (분석 쿼리용) =====
CREATE INDEX IF NOT EXISTS idx_ts_keyhole ON tracking_session(keyhole_detected) WHERE keyhole_detected = true;
CREATE INDEX IF NOT EXISTS idx_ts_max_elevation ON tracking_session(max_elevation DESC);