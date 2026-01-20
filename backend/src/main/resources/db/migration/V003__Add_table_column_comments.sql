-- =============================================================================
-- ACS Database Comments V003
-- 모든 테이블과 컬럼에 대한 한글 주석
-- =============================================================================

-- =============================================================================
-- 1. settings (시스템 설정)
-- =============================================================================
COMMENT ON TABLE settings IS '시스템 설정 테이블 - 서버 재시작 시 복원되는 설정값 영속 저장';

COMMENT ON COLUMN settings.id IS '설정 고유 식별자 (자동 생성)';
COMMENT ON COLUMN settings.key IS '설정 키 (고유값) - 예: antenna.azimuth.offset';
COMMENT ON COLUMN settings.value IS '설정 값 (TEXT로 저장, type에 따라 파싱)';
COMMENT ON COLUMN settings.type IS '값 타입: STRING, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN';
COMMENT ON COLUMN settings.description IS '설정에 대한 설명';
COMMENT ON COLUMN settings.is_system_setting IS '시스템 설정 여부 (TRUE=수정 불가, FALSE=사용자 수정 가능)';
COMMENT ON COLUMN settings.created_at IS '설정 생성 일시 (UTC)';
COMMENT ON COLUMN settings.updated_at IS '설정 최종 수정 일시 (UTC)';

-- =============================================================================
-- 2. setting_history (설정 변경 이력)
-- =============================================================================
COMMENT ON TABLE setting_history IS '설정 변경 감사 로그 - 누가 언제 어떤 설정을 변경했는지 추적';

COMMENT ON COLUMN setting_history.id IS '이력 고유 식별자 (자동 생성)';
COMMENT ON COLUMN setting_history.setting_key IS '변경된 설정의 키';
COMMENT ON COLUMN setting_history.old_value IS '변경 전 값 (신규 생성 시 NULL)';
COMMENT ON COLUMN setting_history.new_value IS '변경 후 값';
COMMENT ON COLUMN setting_history.changed_by IS '변경한 사용자 (인증 구현 전까지 system)';
COMMENT ON COLUMN setting_history.change_reason IS '변경 사유';
COMMENT ON COLUMN setting_history.created_at IS '변경 일시 (UTC)';

-- =============================================================================
-- 3. tracking_session (추적 세션)
-- =============================================================================
COMMENT ON TABLE tracking_session IS '위성 추적 세션 정보 - 한 번의 위성 추적 작업 단위';

COMMENT ON COLUMN tracking_session.id IS '세션 고유 식별자 (자동 생성)';
COMMENT ON COLUMN tracking_session.mst_id IS 'Master ID - passSchedule mstId 또는 ephemeris 식별자';
COMMENT ON COLUMN tracking_session.detail_id IS '상세 ID - passSchedule detailId';
COMMENT ON COLUMN tracking_session.satellite_id IS '위성 식별자 (NORAD ID 문자열)';
COMMENT ON COLUMN tracking_session.satellite_name IS '위성 이름 (표시용)';
COMMENT ON COLUMN tracking_session.tracking_mode IS '추적 모드: EPHEMERIS (실시간 계산) | PASS_SCHEDULE (사전 계산)';
COMMENT ON COLUMN tracking_session.data_type IS '데이터 타입: original, axisTransformed, keyholeOptimized 등';
COMMENT ON COLUMN tracking_session.start_time IS '추적 시작 시간 (UTC)';
COMMENT ON COLUMN tracking_session.end_time IS '추적 종료 시간 (UTC)';
COMMENT ON COLUMN tracking_session.duration IS '추적 지속 시간 (초 단위)';
COMMENT ON COLUMN tracking_session.max_elevation IS '최대 고도각 (도, degrees)';
COMMENT ON COLUMN tracking_session.max_azimuth_rate IS '최대 방위각 변화율 (도/초)';
COMMENT ON COLUMN tracking_session.max_elevation_rate IS '최대 고도각 변화율 (도/초)';
COMMENT ON COLUMN tracking_session.keyhole_detected IS 'Keyhole 영역 감지 여부 (고도 85도 이상)';
COMMENT ON COLUMN tracking_session.recommended_train_angle IS 'Keyhole 회피를 위한 권장 Train(Tilt) 각도 (도)';
COMMENT ON COLUMN tracking_session.total_points IS '궤적 데이터 포인트 총 개수';
COMMENT ON COLUMN tracking_session.created_at IS '레코드 생성 일시 (UTC)';

-- =============================================================================
-- 4. tracking_trajectory (이론 궤적) - Hypertable
-- =============================================================================
COMMENT ON TABLE tracking_trajectory IS '이론 궤적 데이터 (Hypertable) - Orekit/SGP4로 계산된 예측 위치';

COMMENT ON COLUMN tracking_trajectory.timestamp IS '궤적 시점 (UTC) - Hypertable 파티션 키';
COMMENT ON COLUMN tracking_trajectory.session_id IS '소속 추적 세션 ID (FK → tracking_session)';
COMMENT ON COLUMN tracking_trajectory.detail_id IS 'Pass Schedule detail ID';
COMMENT ON COLUMN tracking_trajectory.data_type IS '데이터 타입 (세션과 동일)';
COMMENT ON COLUMN tracking_trajectory.index IS '궤적 내 순서 인덱스';
COMMENT ON COLUMN tracking_trajectory.azimuth IS '방위각 (도, 0~360)';
COMMENT ON COLUMN tracking_trajectory.elevation IS '고도각 (도, 0~90)';
COMMENT ON COLUMN tracking_trajectory.train IS 'Train(Tilt) 각도 (도) - Keyhole 회피용';
COMMENT ON COLUMN tracking_trajectory.azimuth_rate IS '방위각 변화율 (도/초)';
COMMENT ON COLUMN tracking_trajectory.elevation_rate IS '고도각 변화율 (도/초)';
COMMENT ON COLUMN tracking_trajectory.created_at IS '레코드 생성 일시 (UTC)';

-- =============================================================================
-- 5. tracking_result (실측 추적 결과) - Hypertable
-- =============================================================================
COMMENT ON TABLE tracking_result IS '실측 추적 결과 (Hypertable) - 안테나 실제 동작 데이터';

COMMENT ON COLUMN tracking_result.timestamp IS '측정 시점 (UTC) - Hypertable 파티션 키';
COMMENT ON COLUMN tracking_result.session_id IS '소속 추적 세션 ID (FK → tracking_session)';
COMMENT ON COLUMN tracking_result.index IS '측정 순서 인덱스';
COMMENT ON COLUMN tracking_result.theoretical_index IS '대응되는 이론 궤적 인덱스 (보간 시 NULL 가능)';
COMMENT ON COLUMN tracking_result.original_azimuth IS '원본 계산 방위각 (도)';
COMMENT ON COLUMN tracking_result.original_elevation IS '원본 계산 고도각 (도)';
COMMENT ON COLUMN tracking_result.transformed_azimuth IS '축 변환 후 방위각 (도)';
COMMENT ON COLUMN tracking_result.transformed_elevation IS '축 변환 후 고도각 (도)';
COMMENT ON COLUMN tracking_result.transformed_train IS '축 변환 후 Train 각도 (도)';
COMMENT ON COLUMN tracking_result.final_azimuth IS '최종 명령 방위각 (Keyhole 최적화 적용, 도)';
COMMENT ON COLUMN tracking_result.final_elevation IS '최종 명령 고도각 (도)';
COMMENT ON COLUMN tracking_result.final_train IS '최종 명령 Train 각도 (도)';
COMMENT ON COLUMN tracking_result.actual_azimuth IS '안테나 실측 방위각 (도)';
COMMENT ON COLUMN tracking_result.actual_elevation IS '안테나 실측 고도각 (도)';
COMMENT ON COLUMN tracking_result.actual_train IS '안테나 실측 Train 각도 (도)';
COMMENT ON COLUMN tracking_result.azimuth_error IS '방위각 오차 (명령 - 실측, 도)';
COMMENT ON COLUMN tracking_result.elevation_error IS '고도각 오차 (명령 - 실측, 도)';
COMMENT ON COLUMN tracking_result.train_error IS 'Train 각도 오차 (도)';
COMMENT ON COLUMN tracking_result.total_error IS '총 오차 (RSS 계산, 도)';
COMMENT ON COLUMN tracking_result.azimuth_rate IS '방위각 변화율 (도/초)';
COMMENT ON COLUMN tracking_result.elevation_rate IS '고도각 변화율 (도/초)';
COMMENT ON COLUMN tracking_result.train_rate IS 'Train 변화율 (도/초)';
COMMENT ON COLUMN tracking_result.azimuth_acceleration IS '방위각 가속도 (도/초²)';
COMMENT ON COLUMN tracking_result.elevation_acceleration IS '고도각 가속도 (도/초²)';
COMMENT ON COLUMN tracking_result.train_acceleration IS 'Train 가속도 (도/초²)';
COMMENT ON COLUMN tracking_result.keyhole_active IS 'Keyhole 영역 진입 상태';
COMMENT ON COLUMN tracking_result.keyhole_optimized IS 'Keyhole 최적화 적용 여부';
COMMENT ON COLUMN tracking_result.tracking_quality IS '추적 품질: EXCELLENT, GOOD, FAIR, POOR, LOST';
COMMENT ON COLUMN tracking_result.interpolation_type IS '보간 방식: LINEAR, SPLINE 등';
COMMENT ON COLUMN tracking_result.interpolation_accuracy IS '보간 정확도 (0~1)';
COMMENT ON COLUMN tracking_result.satellite_range IS '위성까지 거리 (km)';
COMMENT ON COLUMN tracking_result.satellite_altitude IS '위성 고도 (km)';
COMMENT ON COLUMN tracking_result.satellite_velocity IS '위성 속도 (km/s)';
COMMENT ON COLUMN tracking_result.cmd_azimuth IS 'ICD 명령 방위각 (도)';
COMMENT ON COLUMN tracking_result.cmd_elevation IS 'ICD 명령 고도각 (도)';
COMMENT ON COLUMN tracking_result.cmd_train IS 'ICD 명령 Train 각도 (도)';
COMMENT ON COLUMN tracking_result.position_azimuth IS 'ICD 응답 방위각 (도)';
COMMENT ON COLUMN tracking_result.position_elevation IS 'ICD 응답 고도각 (도)';
COMMENT ON COLUMN tracking_result.position_train IS 'ICD 응답 Train 각도 (도)';
COMMENT ON COLUMN tracking_result.created_at IS '레코드 생성 일시 (UTC)';

-- =============================================================================
-- 6. icd_status (ICD 제어 상태) - Hypertable
-- =============================================================================
COMMENT ON TABLE icd_status IS 'ICD 안테나 제어 상태 (Hypertable) - UDP 통신으로 수신한 실시간 상태';

COMMENT ON COLUMN icd_status.timestamp IS '상태 수신 시점 (UTC) - Hypertable 파티션 키';

-- 각도 (6개)
COMMENT ON COLUMN icd_status.azimuth_angle IS '안테나 방위각 (도, 0~360)';
COMMENT ON COLUMN icd_status.elevation_angle IS '안테나 고도각 (도, 0~90)';
COMMENT ON COLUMN icd_status.train_angle IS '안테나 Train(Tilt) 각도 (도)';
COMMENT ON COLUMN icd_status.servo_driver_azimuth_angle IS '서보 드라이버 보고 방위각 (도)';
COMMENT ON COLUMN icd_status.servo_driver_elevation_angle IS '서보 드라이버 보고 고도각 (도)';
COMMENT ON COLUMN icd_status.servo_driver_train_angle IS '서보 드라이버 보고 Train 각도 (도)';

-- 속도 (3개)
COMMENT ON COLUMN icd_status.azimuth_speed IS '방위각 회전 속도 (도/초)';
COMMENT ON COLUMN icd_status.elevation_speed IS '고도각 회전 속도 (도/초)';
COMMENT ON COLUMN icd_status.train_speed IS 'Train 회전 속도 (도/초)';

-- 토크 (3개)
COMMENT ON COLUMN icd_status.torque_azimuth IS '방위축 토크 (%)';
COMMENT ON COLUMN icd_status.torque_elevation IS '고도축 토크 (%)';
COMMENT ON COLUMN icd_status.torque_train IS 'Train축 토크 (%)';

-- 가속도 (6개)
COMMENT ON COLUMN icd_status.azimuth_acceleration IS '방위각 가속도 (도/초²)';
COMMENT ON COLUMN icd_status.elevation_acceleration IS '고도각 가속도 (도/초²)';
COMMENT ON COLUMN icd_status.train_acceleration IS 'Train 가속도 (도/초²)';
COMMENT ON COLUMN icd_status.azimuth_max_acceleration IS '방위각 최대 가속도 설정값';
COMMENT ON COLUMN icd_status.elevation_max_acceleration IS '고도각 최대 가속도 설정값';
COMMENT ON COLUMN icd_status.train_max_acceleration IS 'Train 최대 가속도 설정값';

-- 환경 (4개)
COMMENT ON COLUMN icd_status.wind_speed IS '풍속 (m/s)';
COMMENT ON COLUMN icd_status.wind_direction IS '풍향 (도, 0~360)';
COMMENT ON COLUMN icd_status.rtd_one IS 'RTD 온도 센서 1 (°C)';
COMMENT ON COLUMN icd_status.rtd_two IS 'RTD 온도 센서 2 (°C)';

-- 상태 비트 (12개)
COMMENT ON COLUMN icd_status.mode_status_bits IS '운영 모드 상태 비트 (8비트)';
COMMENT ON COLUMN icd_status.main_board_protocol_status IS '메인보드 프로토콜 상태 비트';
COMMENT ON COLUMN icd_status.main_board_status IS '메인보드 상태 비트';
COMMENT ON COLUMN icd_status.main_board_mc_onoff IS '메인보드 MC 온오프 상태';
COMMENT ON COLUMN icd_status.main_board_reserve IS '메인보드 예비 상태 비트';
COMMENT ON COLUMN icd_status.azimuth_servo_status IS '방위축 서보 상태 비트 (에러 플래그 포함)';
COMMENT ON COLUMN icd_status.azimuth_board_status IS '방위축 보드 상태 비트';
COMMENT ON COLUMN icd_status.elevation_servo_status IS '고도축 서보 상태 비트 (에러 플래그 포함)';
COMMENT ON COLUMN icd_status.elevation_board_status IS '고도축 보드 상태 비트';
COMMENT ON COLUMN icd_status.train_servo_status IS 'Train축 서보 상태 비트 (에러 플래그 포함)';
COMMENT ON COLUMN icd_status.train_board_status IS 'Train축 보드 상태 비트';
COMMENT ON COLUMN icd_status.feed_board_etc_status IS '피드 보드 기타 상태 비트';

-- Feed 상태 (3개)
COMMENT ON COLUMN icd_status.feed_s_board_status IS 'S밴드 피드 보드 상태';
COMMENT ON COLUMN icd_status.feed_x_board_status IS 'X밴드 피드 보드 상태';
COMMENT ON COLUMN icd_status.feed_ka_board_status IS 'Ka밴드 피드 보드 상태';

-- LNA 전류 (6개)
COMMENT ON COLUMN icd_status.current_sband_lna_lhcp IS 'S밴드 LNA LHCP 전류 (mA)';
COMMENT ON COLUMN icd_status.current_sband_lna_rhcp IS 'S밴드 LNA RHCP 전류 (mA)';
COMMENT ON COLUMN icd_status.current_xband_lna_lhcp IS 'X밴드 LNA LHCP 전류 (mA)';
COMMENT ON COLUMN icd_status.current_xband_lna_rhcp IS 'X밴드 LNA RHCP 전류 (mA)';
COMMENT ON COLUMN icd_status.current_kaband_lna_lhcp IS 'Ka밴드 LNA LHCP 전류 (mA)';
COMMENT ON COLUMN icd_status.current_kaband_lna_rhcp IS 'Ka밴드 LNA RHCP 전류 (mA)';

-- RSSI (6개)
COMMENT ON COLUMN icd_status.rssi_sband_lna_lhcp IS 'S밴드 LNA LHCP RSSI (dBm)';
COMMENT ON COLUMN icd_status.rssi_sband_lna_rhcp IS 'S밴드 LNA RHCP RSSI (dBm)';
COMMENT ON COLUMN icd_status.rssi_xband_lna_lhcp IS 'X밴드 LNA LHCP RSSI (dBm)';
COMMENT ON COLUMN icd_status.rssi_xband_lna_rhcp IS 'X밴드 LNA RHCP RSSI (dBm)';
COMMENT ON COLUMN icd_status.rssi_kaband_lna_lhcp IS 'Ka밴드 LNA LHCP RSSI (dBm)';
COMMENT ON COLUMN icd_status.rssi_kaband_lna_rhcp IS 'Ka밴드 LNA RHCP RSSI (dBm)';

-- 추적 CMD/실측 (9개)
COMMENT ON COLUMN icd_status.tracking_azimuth_time IS '방위각 명령 시간 오프셋 (초)';
COMMENT ON COLUMN icd_status.tracking_cmd_azimuth IS '추적 명령 방위각 (도)';
COMMENT ON COLUMN icd_status.tracking_actual_azimuth IS '추적 실측 방위각 (도)';
COMMENT ON COLUMN icd_status.tracking_elevation_time IS '고도각 명령 시간 오프셋 (초)';
COMMENT ON COLUMN icd_status.tracking_cmd_elevation IS '추적 명령 고도각 (도)';
COMMENT ON COLUMN icd_status.tracking_actual_elevation IS '추적 실측 고도각 (도)';
COMMENT ON COLUMN icd_status.tracking_train_time IS 'Train 명령 시간 오프셋 (초)';
COMMENT ON COLUMN icd_status.tracking_cmd_train IS '추적 명령 Train 각도 (도)';
COMMENT ON COLUMN icd_status.tracking_actual_train IS '추적 실측 Train 각도 (도)';

-- =============================================================================
-- 7. tle_cache (TLE 캐시)
-- =============================================================================
COMMENT ON TABLE tle_cache IS 'TLE(Two-Line Element) 캐시 - 위성 궤도 정보 저장, Soft Delete 적용';

COMMENT ON COLUMN tle_cache.id IS 'TLE 캐시 고유 식별자 (자동 생성)';
COMMENT ON COLUMN tle_cache.satellite_id IS '위성 식별자 (NORAD ID 문자열)';
COMMENT ON COLUMN tle_cache.norad_id IS 'NORAD 카탈로그 번호 (정수)';
COMMENT ON COLUMN tle_cache.satellite_name IS '위성 이름';
COMMENT ON COLUMN tle_cache.tle_line_1 IS 'TLE 첫 번째 라인 (69자)';
COMMENT ON COLUMN tle_cache.tle_line_2 IS 'TLE 두 번째 라인 (69자)';
COMMENT ON COLUMN tle_cache.epoch_date IS 'TLE 에포크 시간 (UTC)';
COMMENT ON COLUMN tle_cache.mode IS '사용 모드: EPHEMERIS (실시간) | PASS_SCHEDULE (사전 계산)';
COMMENT ON COLUMN tle_cache.is_active IS '활성 상태 (FALSE=비활성화, Soft Delete)';
COMMENT ON COLUMN tle_cache.source IS 'TLE 출처: MANUAL, CELESTRAK, SPACE_TRACK';
COMMENT ON COLUMN tle_cache.created_at IS '레코드 생성 일시 (UTC)';
COMMENT ON COLUMN tle_cache.updated_at IS '레코드 수정 일시 (UTC)';
COMMENT ON COLUMN tle_cache.deactivated_at IS '비활성화 일시 (Soft Delete 시점)';

-- =============================================================================
-- 8. hardware_error_log (하드웨어 에러 로그) - Hypertable
-- =============================================================================
COMMENT ON TABLE hardware_error_log IS '하드웨어 에러 로그 (Hypertable) - ICD 비트 에러 감지 및 해결 추적';

COMMENT ON COLUMN hardware_error_log.id IS '에러 로그 고유 식별자 (자동 생성)';
COMMENT ON COLUMN hardware_error_log.timestamp IS '에러 발생 시점 (UTC) - Hypertable 파티션 키';
COMMENT ON COLUMN hardware_error_log.error_code IS '에러 코드 (예: AZ_SERVO_BIT_5, PROTOCOL_ERROR)';
COMMENT ON COLUMN hardware_error_log.error_type IS '에러 타입: PROTOCOL, SERVO, EMERGENCY, INTERLOCK, SYSTEM';
COMMENT ON COLUMN hardware_error_log.error_message IS '에러 상세 메시지 (한글)';
COMMENT ON COLUMN hardware_error_log.source IS '에러 발생 소스 (예: azimuthBoardServoStatusBits)';
COMMENT ON COLUMN hardware_error_log.axis IS '관련 축: AZIMUTH, ELEVATION, TRAIN, FEED, MAIN, NULL';
COMMENT ON COLUMN hardware_error_log.severity IS '심각도: CRITICAL, ERROR, WARNING, INFO';
COMMENT ON COLUMN hardware_error_log.tracking_mode IS '에러 발생 시 추적 모드: ephemeris, passSchedule, sunTrack, NULL(대기)';
COMMENT ON COLUMN hardware_error_log.session_id IS '관련 추적 세션 ID (FK → tracking_session, 현재 미사용)';
COMMENT ON COLUMN hardware_error_log.raw_data IS '에러 발생 시 원시 데이터 (JSON) - 비트값, 안테나 상태 등';
COMMENT ON COLUMN hardware_error_log.correlation_id IS '동시 발생 에러 그룹 ID (같은 UDP 패킷에서 2개 이상 에러 시)';
COMMENT ON COLUMN hardware_error_log.occurrence_count IS '동일 에러 연속 발생 횟수';
COMMENT ON COLUMN hardware_error_log.resolved IS '해결 여부';
COMMENT ON COLUMN hardware_error_log.resolved_at IS '해결 시점 (UTC)';
COMMENT ON COLUMN hardware_error_log.resolved_by IS '해결자 (사용자 ID 또는 SYSTEM)';
COMMENT ON COLUMN hardware_error_log.resolution_note IS '해결 메모';
COMMENT ON COLUMN hardware_error_log.is_initial_error IS 'BE 시작 시 기존 에러 여부 (TRUE=시작 시 이미 존재, FALSE=새로 발생)';

-- =============================================================================
-- 완료
-- =============================================================================
