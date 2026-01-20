-- =============================================================================
-- ACS Default Settings V002
-- SettingsService.kt의 settingDefinitions와 동기화됨
-- =============================================================================

-- 기존 데이터 정리 (필요시)
DELETE FROM settings;

-- =============================================================================
-- 위치 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('location.latitude', '35.317540', 'DOUBLE', '위도', false),
    ('location.longitude', '128.608510', 'DOUBLE', '경도', false),
    ('location.altitude', '0.0', 'DOUBLE', '고도', false);

-- =============================================================================
-- 추적 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('tracking.msInterval', '100', 'INTEGER', '추적 간격', false),
    ('tracking.durationDays', '1', 'LONG', '추적 기간(일)', false),
    ('tracking.minElevationAngle', '0', 'FLOAT', '최소 고도각', false),
    ('tracking.preparationTimeMinutes', '4', 'LONG', '추적 준비 시간(분) - Train+Az 이동 시간', false);

-- =============================================================================
-- Stow Angle 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('stow.angle.azimuth', '0.0', 'DOUBLE', 'Stow 방위각', false),
    ('stow.angle.elevation', '90.0', 'DOUBLE', 'Stow 고도각', false),
    ('stow.angle.train', '0.0', 'DOUBLE', 'Stow Train각', false);

-- =============================================================================
-- Stow Speed 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('stow.speed.azimuth', '5.0', 'DOUBLE', 'Stow 방위각 속도', false),
    ('stow.speed.elevation', '5.0', 'DOUBLE', 'Stow 고도각 속도', false),
    ('stow.speed.train', '5.0', 'DOUBLE', 'Stow Train각 속도', false);

-- =============================================================================
-- AntennaSpec 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('antennaspec.trueNorthOffsetAngle', '0.0', 'DOUBLE', 'True North Offset Angle', false),
    ('antennaspec.tiltAngle', '-7.0', 'DOUBLE', 'Tilt Angle', false);

-- =============================================================================
-- Angle Limits 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('anglelimits.azimuthMin', '-270.0', 'DOUBLE', 'Azimuth 최소각', false),
    ('anglelimits.azimuthMax', '270.0', 'DOUBLE', 'Azimuth 최대각', false),
    ('anglelimits.elevationMin', '0.0', 'DOUBLE', 'Elevation 최소각', false),
    ('anglelimits.elevationMax', '180.0', 'DOUBLE', 'Elevation 최대각', false),
    ('anglelimits.trainMin', '-270.0', 'DOUBLE', 'Train 최소각', false),
    ('anglelimits.trainMax', '270.0', 'DOUBLE', 'Train 최대각', false);

-- =============================================================================
-- Speed Limits 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('speedlimits.azimuthMin', '0.1', 'DOUBLE', 'Azimuth 최소속도', false),
    ('speedlimits.azimuthMax', '15.0', 'DOUBLE', 'Azimuth 최대속도', false),
    ('speedlimits.elevationMin', '0.1', 'DOUBLE', 'Elevation 최소속도', false),
    ('speedlimits.elevationMax', '10.0', 'DOUBLE', 'Elevation 최대속도', false),
    ('speedlimits.trainMin', '0.1', 'DOUBLE', 'Train 최소속도', false),
    ('speedlimits.trainMax', '5.0', 'DOUBLE', 'Train 최대속도', false);

-- =============================================================================
-- Angle Offset Limits 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('angleoffsetlimits.azimuth', '50.0', 'DOUBLE', 'Azimuth 오프셋 제한', false),
    ('angleoffsetlimits.elevation', '50.0', 'DOUBLE', 'Elevation 오프셋 제한', false),
    ('angleoffsetlimits.train', '50.0', 'DOUBLE', 'Train 오프셋 제한', false);

-- =============================================================================
-- Time Offset Limits 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('timeoffsetlimits.min', '0.1', 'DOUBLE', '시간 오프셋 최소값', false),
    ('timeoffsetlimits.max', '99999', 'DOUBLE', '시간 오프셋 최대값', false);

-- =============================================================================
-- Algorithm 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('algorithm.geoMinMotion', '1.1', 'DOUBLE', 'Geo Min Motion', false);

-- =============================================================================
-- StepSizeLimit 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('stepsizelimit.min', '50', 'DOUBLE', '스텝 사이즈 최소값', false),
    ('stepsizelimit.max', '50', 'DOUBLE', '스텝 사이즈 최대값', false);

-- =============================================================================
-- Feed 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('feed.enabledBands', '["s","x"]', 'STRING', '피드 밴드 표시 설정 (S-Band, X-Band, Ka-Band)', false);

-- =============================================================================
-- 시스템 UDP 통신 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.udp.receiveInterval', '10', 'LONG', 'UDP 수신 간격', true),
    ('system.udp.sendInterval', '10', 'LONG', 'UDP 전송 간격', true),
    ('system.udp.timeout', '25', 'LONG', 'UDP 타임아웃', true),
    ('system.udp.reconnectInterval', '1000', 'LONG', 'UDP 재연결 간격', true),
    ('system.udp.maxBufferSize', '1024', 'INTEGER', 'UDP 최대 버퍼 크기', true),
    ('system.udp.commandDelay', '100', 'LONG', 'UDP 명령 지연', true);

-- =============================================================================
-- 시스템 추적 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.tracking.interval', '100', 'LONG', '추적 간격', true),
    ('system.tracking.transmissionInterval', '100', 'LONG', '전송 간격', true),
    ('system.tracking.fineInterval', '100', 'LONG', '정밀 계산 간격', true),
    ('system.tracking.coarseInterval', '1000', 'LONG', '일반 계산 간격', true),
    ('system.tracking.stabilizationTimeout', '5000', 'LONG', '안정화 타임아웃', true);

-- =============================================================================
-- 시스템 데이터 저장 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.storage.batchSize', '1000', 'INTEGER', '배치 크기', true),
    ('system.storage.saveInterval', '100', 'LONG', '저장 간격', true),
    ('system.storage.progressLogInterval', '1000', 'INTEGER', '진행률 로깅 간격', true);

-- =============================================================================
-- 시스템 태양 추적 정확도 임계값 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.suntrack.highAccuracyThreshold', '0.000278', 'DOUBLE', '태양 추적 높은 정확도 임계값', true),
    ('system.suntrack.mediumAccuracyThreshold', '0.002778', 'DOUBLE', '태양 추적 중간 정확도 임계값', true),
    ('system.suntrack.lowAccuracyThreshold', '0.016667', 'DOUBLE', '태양 추적 낮은 정확도 임계값', true),
    ('system.suntrack.searchHours', '48.0', 'DOUBLE', '태양 추적 검색 시간', true);

-- =============================================================================
-- 시스템 WebSocket 및 성능 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.websocket.transmissionInterval', '30', 'LONG', 'WebSocket 전송 간격', true),
    ('system.performance.threshold', '100', 'LONG', '성능 임계값', true);

-- =============================================================================
-- 시스템 성능 등급 기준 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.performance.ultraCores', '8', 'INTEGER', 'ULTRA 등급 최소 CPU 코어 수', true),
    ('system.performance.highCores', '6', 'INTEGER', 'HIGH 등급 최소 CPU 코어 수', true),
    ('system.performance.mediumCores', '4', 'INTEGER', 'MEDIUM 등급 최소 CPU 코어 수', true),
    ('system.performance.ultraMemory', '8', 'LONG', 'ULTRA 등급 최소 메모리(GB)', true),
    ('system.performance.highMemory', '4', 'LONG', 'HIGH 등급 최소 메모리(GB)', true),
    ('system.performance.mediumMemory', '2', 'LONG', 'MEDIUM 등급 최소 메모리(GB)', true);

-- =============================================================================
-- 시스템 JVM 튜닝 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('system.jvm.gcPause', '10', 'LONG', 'GC 일시정지 시간 (ms)', true),
    ('system.jvm.heapRegionSize', '16', 'LONG', '힙 영역 크기 (MB)', true),
    ('system.jvm.concurrentThreads', '4', 'LONG', '동시 스레드 수', true),
    ('system.jvm.parallelThreads', '8', 'LONG', '병렬 스레드 수', true);

-- =============================================================================
-- Ephemeris Tracking 설정
-- =============================================================================
INSERT INTO settings (key, value, type, description, is_system_setting) VALUES
    ('ephemeris.tracking.sourceMinElevationAngle', '0.0', 'DOUBLE', '원본 2축 위성 추적 데이터 생성 시 최소 Elevation 각도 (도)', true),
    ('ephemeris.tracking.keyholeAzimuthVelocityThreshold', '10.0', 'DOUBLE', 'KEYHOLE 위성 판단을 위한 Azimuth 각속도 임계값 (도/초)', true);

-- =============================================================================
-- 설정값 삽입 완료: 총 63개 설정
-- =============================================================================
