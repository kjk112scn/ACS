-- V005: tracking_session 테이블 인덱스 추가
-- 목적: 조회 성능 최적화 (데이터 누적 대비)

-- 1. Pass 그룹 조회, View JOIN 용
CREATE INDEX IF NOT EXISTS idx_session_mst_datatype
    ON tracking_session(mst_id, data_type);

-- 2. 위성별 히스토리 조회
CREATE INDEX IF NOT EXISTS idx_session_satellite_time
    ON tracking_session(satellite_id, start_time DESC);

-- 3. 시간 범위 조회 (대시보드, 최근 세션 등)
CREATE INDEX IF NOT EXISTS idx_session_start_time
    ON tracking_session(start_time DESC);

-- 4. 모드별 필터링 (EPHEMERIS/PASS_SCHEDULE)
CREATE INDEX IF NOT EXISTS idx_session_mode_time
    ON tracking_session(tracking_mode, start_time DESC);
