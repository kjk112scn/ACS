-- =============================================================================
-- ACS Database Schema
-- PostgreSQL 16 + TimescaleDB
-- =============================================================================

-- TimescaleDB 확장 활성화
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- =============================================================================
-- 1. tracking_session (추적 세션) - 일반 테이블
-- =============================================================================
-- FK 부모 테이블이므로 Hypertable 불가, 압축 없음
-- 삭제: Spring Scheduler로 365일 후 삭제

CREATE TABLE tracking_session (
    id                      BIGSERIAL PRIMARY KEY,
    mst_id                  BIGINT NOT NULL,
    detail_id               INTEGER NOT NULL,
    satellite_id            VARCHAR(20) NOT NULL,
    satellite_name          VARCHAR(100),
    tracking_mode           VARCHAR(20) NOT NULL,      -- 'EPHEMERIS' | 'PASS_SCHEDULE'
    data_type               VARCHAR(50) NOT NULL,      -- 'original', 'axisTransformed', etc.

    -- 시간 정보
    start_time              TIMESTAMPTZ NOT NULL,
    end_time                TIMESTAMPTZ NOT NULL,
    duration                INTEGER,                   -- 초 단위

    -- 각도 정보 (Peak 값)
    max_elevation           DOUBLE PRECISION,
    max_azimuth_rate        DOUBLE PRECISION,
    max_elevation_rate      DOUBLE PRECISION,
    keyhole_detected        BOOLEAN DEFAULT FALSE,

    -- 추가 메타데이터
    recommended_train_angle DOUBLE PRECISION,
    total_points            INTEGER,

    -- 시스템 필드
    created_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uk_tracking_session UNIQUE (mst_id, data_type, tracking_mode)
);

-- 인덱스
CREATE INDEX idx_ts_satellite ON tracking_session(satellite_id);
CREATE INDEX idx_ts_start_time ON tracking_session(start_time DESC);
CREATE INDEX idx_ts_mode ON tracking_session(tracking_mode);
CREATE INDEX idx_ts_mst_datatype ON tracking_session(mst_id, data_type);
CREATE INDEX idx_ts_mode_satellite_time ON tracking_session(tracking_mode, satellite_id, start_time DESC);

-- =============================================================================
-- 2. tracking_trajectory (이론 궤적) - Hypertable
-- =============================================================================
-- 압축: 7일 후, 삭제: tracking_session CASCADE

CREATE TABLE tracking_trajectory (
    timestamp               TIMESTAMPTZ NOT NULL,
    session_id              BIGINT NOT NULL REFERENCES tracking_session(id) ON DELETE CASCADE,
    detail_id               INTEGER NOT NULL,
    data_type               VARCHAR(50) NOT NULL,

    -- 인덱스
    index                   INTEGER NOT NULL,

    -- 각도
    azimuth                 DOUBLE PRECISION NOT NULL,
    elevation               DOUBLE PRECISION NOT NULL,
    train                   DOUBLE PRECISION,

    -- 속도
    azimuth_rate            DOUBLE PRECISION,
    elevation_rate          DOUBLE PRECISION,

    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환 (PRIMARY KEY 없이)
SELECT create_hypertable('tracking_trajectory', 'timestamp');
SELECT set_chunk_time_interval('tracking_trajectory', INTERVAL '7 days');

-- 압축 정책
ALTER TABLE tracking_trajectory SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'session_id'
);
SELECT add_compression_policy('tracking_trajectory', INTERVAL '7 days');

-- 인덱스
CREATE INDEX idx_tt_session ON tracking_trajectory(session_id);
CREATE INDEX idx_tt_session_timestamp ON tracking_trajectory(session_id, timestamp);

-- =============================================================================
-- 3. tracking_result (실측 추적 결과) - Hypertable
-- =============================================================================
-- 압축: 7일 후, 삭제: tracking_session CASCADE

CREATE TABLE tracking_result (
    timestamp               TIMESTAMPTZ NOT NULL,
    session_id              BIGINT NOT NULL REFERENCES tracking_session(id) ON DELETE CASCADE,

    -- 인덱스/시간
    index                   INTEGER NOT NULL,
    theoretical_index       INTEGER,

    -- 원본 각도 (2축)
    original_azimuth        DOUBLE PRECISION,
    original_elevation      DOUBLE PRECISION,

    -- 변환된 각도 (3축)
    transformed_azimuth     DOUBLE PRECISION,
    transformed_elevation   DOUBLE PRECISION,
    transformed_train       DOUBLE PRECISION,

    -- 최종 각도 (제한 적용)
    final_azimuth           DOUBLE PRECISION,
    final_elevation         DOUBLE PRECISION,
    final_train             DOUBLE PRECISION,

    -- 실제 측정값
    actual_azimuth          DOUBLE PRECISION,
    actual_elevation        DOUBLE PRECISION,
    actual_train            DOUBLE PRECISION,

    -- 오차
    azimuth_error           DOUBLE PRECISION,
    elevation_error         DOUBLE PRECISION,
    train_error             DOUBLE PRECISION,
    total_error             DOUBLE PRECISION,

    -- 속도
    azimuth_rate            DOUBLE PRECISION,
    elevation_rate          DOUBLE PRECISION,
    train_rate              DOUBLE PRECISION,

    -- 가속도
    azimuth_acceleration    DOUBLE PRECISION,
    elevation_acceleration  DOUBLE PRECISION,
    train_acceleration      DOUBLE PRECISION,

    -- 상태
    keyhole_active          BOOLEAN DEFAULT FALSE,
    keyhole_optimized       BOOLEAN DEFAULT FALSE,
    tracking_quality        VARCHAR(20),

    -- 보간 정보
    interpolation_type      VARCHAR(20),
    interpolation_accuracy  DOUBLE PRECISION,

    -- 위성 위치 (참조)
    satellite_range         DOUBLE PRECISION,
    satellite_altitude      DOUBLE PRECISION,
    satellite_velocity      DOUBLE PRECISION,

    -- CMD/Position
    cmd_azimuth             DOUBLE PRECISION,
    cmd_elevation           DOUBLE PRECISION,
    cmd_train               DOUBLE PRECISION,
    position_azimuth        DOUBLE PRECISION,
    position_elevation      DOUBLE PRECISION,
    position_train          DOUBLE PRECISION,

    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Hypertable 변환 (PRIMARY KEY 없이)
SELECT create_hypertable('tracking_result', 'timestamp');
SELECT set_chunk_time_interval('tracking_result', INTERVAL '1 day');

-- 압축 정책
ALTER TABLE tracking_result SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'session_id'
);
SELECT add_compression_policy('tracking_result', INTERVAL '7 days');

-- 인덱스
CREATE INDEX idx_tr_session ON tracking_result(session_id);
CREATE INDEX idx_tr_timestamp ON tracking_result(timestamp DESC);
CREATE INDEX idx_tr_quality ON tracking_result(tracking_quality);

-- =============================================================================
-- 4. icd_status (ICD 제어 상태 - 100ms) - Hypertable
-- =============================================================================
-- 압축: 7일 후, 삭제: 90일 후 (TimescaleDB retention)

CREATE TABLE icd_status (
    timestamp                       TIMESTAMPTZ NOT NULL,

    -- 각도 (6개)
    azimuth_angle                   REAL,
    elevation_angle                 REAL,
    train_angle                     REAL,
    servo_driver_azimuth_angle      REAL,
    servo_driver_elevation_angle    REAL,
    servo_driver_train_angle        REAL,

    -- 속도 (3개)
    azimuth_speed                   REAL,
    elevation_speed                 REAL,
    train_speed                     REAL,

    -- 토크 (3개)
    torque_azimuth                  REAL,
    torque_elevation                REAL,
    torque_train                    REAL,

    -- 가속도 (6개)
    azimuth_acceleration            REAL,
    elevation_acceleration          REAL,
    train_acceleration              REAL,
    azimuth_max_acceleration        REAL,
    elevation_max_acceleration      REAL,
    train_max_acceleration          REAL,

    -- 환경 (4개)
    wind_speed                      REAL,
    wind_direction                  SMALLINT,
    rtd_one                         REAL,
    rtd_two                         REAL,

    -- 상태 비트 (12개)
    mode_status_bits                VARCHAR(32),
    main_board_protocol_status      VARCHAR(32),
    main_board_status               VARCHAR(32),
    main_board_mc_onoff             VARCHAR(32),
    main_board_reserve              VARCHAR(32),
    azimuth_servo_status            VARCHAR(32),
    azimuth_board_status            VARCHAR(32),
    elevation_servo_status          VARCHAR(32),
    elevation_board_status          VARCHAR(32),
    train_servo_status              VARCHAR(32),
    train_board_status              VARCHAR(32),
    feed_board_etc_status           VARCHAR(32),

    -- Feed 상태 (3개)
    feed_s_board_status             VARCHAR(32),
    feed_x_board_status             VARCHAR(32),
    feed_ka_board_status            VARCHAR(32),

    -- LNA 전류 (6개)
    current_sband_lna_lhcp          REAL,
    current_sband_lna_rhcp          REAL,
    current_xband_lna_lhcp          REAL,
    current_xband_lna_rhcp          REAL,
    current_kaband_lna_lhcp         REAL,
    current_kaband_lna_rhcp         REAL,

    -- RSSI (6개)
    rssi_sband_lna_lhcp             REAL,
    rssi_sband_lna_rhcp             REAL,
    rssi_xband_lna_lhcp             REAL,
    rssi_xband_lna_rhcp             REAL,
    rssi_kaband_lna_lhcp            REAL,
    rssi_kaband_lna_rhcp            REAL,

    -- 추적 CMD/실측 (9개)
    tracking_azimuth_time           REAL,
    tracking_cmd_azimuth            REAL,
    tracking_actual_azimuth         REAL,
    tracking_elevation_time         REAL,
    tracking_cmd_elevation          REAL,
    tracking_actual_elevation       REAL,
    tracking_train_time             REAL,
    tracking_cmd_train              REAL,
    tracking_actual_train           REAL
);

-- Hypertable 변환
SELECT create_hypertable('icd_status', 'timestamp');
SELECT set_chunk_time_interval('icd_status', INTERVAL '1 day');

-- 압축 정책
ALTER TABLE icd_status SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = ''
);
SELECT add_compression_policy('icd_status', INTERVAL '7 days');

-- 보관 정책 (90일)
SELECT add_retention_policy('icd_status', INTERVAL '90 days');

-- 인덱스
CREATE INDEX idx_is_timestamp ON icd_status(timestamp DESC);

-- =============================================================================
-- 5. settings (시스템 설정) - 일반 테이블
-- =============================================================================
-- 설정값 영속 저장, 서버 재시작 시 복원

CREATE TABLE settings (
    id                      BIGSERIAL PRIMARY KEY,
    key                     VARCHAR(100) NOT NULL UNIQUE,
    value                   TEXT NOT NULL,
    type                    VARCHAR(20) NOT NULL,          -- STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN
    description             VARCHAR(500),
    is_system_setting       BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_settings_key ON settings(key);
CREATE INDEX idx_settings_system ON settings(is_system_setting);

-- =============================================================================
-- 6. setting_history (설정 변경 이력) - 일반 테이블
-- =============================================================================
-- 감사 로그: 누가 언제 어떤 설정을 변경했는지 추적

CREATE TABLE setting_history (
    id                      BIGSERIAL PRIMARY KEY,
    setting_key             VARCHAR(100) NOT NULL,
    old_value               TEXT,
    new_value               TEXT NOT NULL,
    changed_by              VARCHAR(100),
    change_reason           VARCHAR(500),
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_sh_setting_key ON setting_history(setting_key);
CREATE INDEX idx_sh_created_at ON setting_history(created_at DESC);
CREATE INDEX idx_sh_changed_by ON setting_history(changed_by);

-- =============================================================================
-- 7. tle_cache (TLE 캐시) - 일반 테이블
-- =============================================================================
-- Ephemeris/PassSchedule TLE 저장
-- Soft Delete: is_active=FALSE로 비활성화 (이력 보관)

CREATE TABLE tle_cache (
    id                      BIGSERIAL PRIMARY KEY,

    -- 위성 식별
    satellite_id            VARCHAR(20) NOT NULL,
    norad_id                INTEGER,                    -- NORAD ID 숫자형 (조회 최적화)
    satellite_name          VARCHAR(100),

    -- TLE 데이터
    tle_line_1              VARCHAR(70) NOT NULL,
    tle_line_2              VARCHAR(70) NOT NULL,
    epoch_date              TIMESTAMPTZ NOT NULL,       -- TLE 에포크 (정밀도 판단용)

    -- 운영 정보
    mode                    VARCHAR(20) NOT NULL,       -- 'EPHEMERIS' | 'PASS_SCHEDULE'
    is_active               BOOLEAN DEFAULT TRUE,
    source                  VARCHAR(50) DEFAULT 'MANUAL', -- 'MANUAL' | 'CELESTRAK' | 'SPACE_TRACK'

    -- 시스템 필드
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    deactivated_at          TIMESTAMPTZ,                -- 비활성화 시점

    -- 제약조건
    CONSTRAINT chk_tle_mode CHECK (mode IN ('EPHEMERIS', 'PASS_SCHEDULE')),
    CONSTRAINT chk_tle_source CHECK (source IN ('MANUAL', 'CELESTRAK', 'SPACE_TRACK'))
);

-- 인덱스
CREATE INDEX idx_tle_satellite_mode ON tle_cache(satellite_id, mode);
CREATE INDEX idx_tle_norad ON tle_cache(norad_id);
CREATE INDEX idx_tle_epoch ON tle_cache(epoch_date DESC);
CREATE INDEX idx_tle_active ON tle_cache(mode, is_active) WHERE is_active = TRUE;

-- Ephemeris는 활성 TLE 1개만 허용 (부분 UNIQUE)
CREATE UNIQUE INDEX idx_tle_ephemeris_unique
ON tle_cache(mode)
WHERE mode = 'EPHEMERIS' AND is_active = TRUE;

-- =============================================================================
-- 8. hardware_error_log (하드웨어 에러 로그) - Hypertable
-- =============================================================================
-- ICD 통신 에러, 서보 에러, 비상 정지 등 로깅
-- 압축: 30일 후, 삭제: 365일 후

CREATE TABLE hardware_error_log (
    id                      BIGSERIAL,
    timestamp               TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 에러 식별
    error_code              VARCHAR(50) NOT NULL,       -- PROTOCOL_AZIMUTH_ERROR, SERVO_TRAIN_ALARM 등
    error_type              VARCHAR(30) NOT NULL,       -- 'PROTOCOL' | 'SERVO' | 'EMERGENCY' | 'INTERLOCK' | 'SYSTEM'
    error_message           TEXT,

    -- 소스 정보 (ErrorMessageConfig.kt 기반)
    source                  VARCHAR(30) NOT NULL,       -- 'ACU' | 'AZIMUTH' | 'ELEVATION' | 'TRAIN' | 'FEED'
    axis                    VARCHAR(20),                -- 'AZIMUTH' | 'ELEVATION' | 'TRAIN' | NULL

    -- 심각도
    severity                VARCHAR(20) NOT NULL DEFAULT 'WARNING',

    -- 컨텍스트
    tracking_mode           VARCHAR(20),                -- 현재 추적 모드
    session_id              BIGINT REFERENCES tracking_session(id) ON DELETE SET NULL,

    -- 에러 상세
    raw_data                JSONB,                      -- ICD 원본 데이터 (디버깅용)

    -- 연속 에러 그룹화
    correlation_id          UUID,                       -- 동일 원인 에러 묶음
    occurrence_count        INTEGER DEFAULT 1,

    -- 해결 상태
    resolved                BOOLEAN DEFAULT FALSE,
    resolved_at             TIMESTAMPTZ,
    resolved_by             VARCHAR(100),
    resolution_note         TEXT,

    -- Hypertable용 복합 PK
    PRIMARY KEY (timestamp, id),

    -- 제약조건
    CONSTRAINT chk_hel_severity CHECK (severity IN ('CRITICAL', 'ERROR', 'WARNING', 'INFO')),
    CONSTRAINT chk_hel_type CHECK (error_type IN ('PROTOCOL', 'SERVO', 'EMERGENCY', 'INTERLOCK', 'SYSTEM'))
);

-- Hypertable 변환
SELECT create_hypertable('hardware_error_log', 'timestamp');
SELECT set_chunk_time_interval('hardware_error_log', INTERVAL '7 days');

-- 압축 정책 (30일 후)
ALTER TABLE hardware_error_log SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'error_type'
);
SELECT add_compression_policy('hardware_error_log', INTERVAL '30 days');

-- 보관 정책 (365일)
SELECT add_retention_policy('hardware_error_log', INTERVAL '365 days');

-- 인덱스
CREATE INDEX idx_hel_timestamp ON hardware_error_log(timestamp DESC);
CREATE INDEX idx_hel_error_code ON hardware_error_log(error_code);
CREATE INDEX idx_hel_severity ON hardware_error_log(severity, timestamp DESC);
CREATE INDEX idx_hel_unresolved ON hardware_error_log(resolved, timestamp DESC) WHERE resolved = FALSE;
CREATE INDEX idx_hel_session ON hardware_error_log(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_hel_correlation ON hardware_error_log(correlation_id) WHERE correlation_id IS NOT NULL;

-- =============================================================================
-- 9. CHECK 제약조건 추가 (기존 테이블)
-- =============================================================================

-- tracking_session
ALTER TABLE tracking_session
ADD CONSTRAINT chk_ts_mode CHECK (tracking_mode IN ('EPHEMERIS', 'PASS_SCHEDULE'));

ALTER TABLE tracking_session
ADD CONSTRAINT chk_ts_time CHECK (end_time > start_time);

-- tracking_result
ALTER TABLE tracking_result
ADD CONSTRAINT chk_tr_quality CHECK (
    tracking_quality IS NULL OR
    tracking_quality IN ('EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'LOST')
);

-- settings
ALTER TABLE settings
ADD CONSTRAINT chk_settings_type CHECK (type IN ('STRING', 'INTEGER', 'LONG', 'FLOAT', 'DOUBLE', 'BOOLEAN'));

-- =============================================================================
-- 테이블 설정 요약
-- =============================================================================
-- | 테이블              | 타입       | 압축   | 삭제 정책              |
-- |---------------------|------------|--------|------------------------|
-- | tracking_session    | 일반 테이블 | 없음   | 365일 (Spring Scheduler)|
-- | tracking_trajectory | Hypertable | 7일 후 | CASCADE (session 삭제시)|
-- | tracking_result     | Hypertable | 7일 후 | CASCADE (session 삭제시)|
-- | icd_status          | Hypertable | 7일 후 | 90일 (TimescaleDB)     |
-- | settings            | 일반 테이블 | 없음   | 영구 보관              |
-- | setting_history     | 일반 테이블 | 없음   | 영구 보관 (감사 로그)   |
-- | tle_cache           | 일반 테이블 | 없음   | Soft Delete (이력 보관) |
-- | hardware_error_log  | Hypertable | 30일후 | 365일 (TimescaleDB)    |
-- =============================================================================
