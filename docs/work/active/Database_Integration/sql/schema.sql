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
-- =============================================================================
