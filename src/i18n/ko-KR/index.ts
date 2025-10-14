export default {
  // 공통
  common: {
    save: '저장',
    cancel: '취소',
    confirm: '확인',
    delete: '삭제',
    edit: '편집',
    add: '추가',
    search: '검색',
    reset: '초기화',
    close: '닫기',
    back: '뒤로',
    next: '다음',
    previous: '이전',
    loading: '로딩 중...',
    success: '성공',
    error: '오류',
    warning: '경고',
    info: '정보',
    yes: '예',
    no: '아니오',
  },

  // 로그인
  login: {
    title: '로그인',
    username: '사용자명',
    password: '비밀번호',
    loginButton: '로그인',
    invalidCredentials: '잘못된 인증 정보입니다.',
    loginSuccess: '로그인에 성공했습니다.',
  },

  // 대시보드
  dashboard: {
    title: '대시보드',
    serverStatus: '서버 상태',
    connected: '연결됨',
    connecting: '연결 중...',
    disconnected: '연결 끊김',
    communication: '통신',
    commandTime: '명령 시간',
    azimuth: '방위각',
    elevation: '고각',
    train: '트레인',
    cmd: '명령',
    actual: '실제',
    speed: '속도',
  },

  // 모드
  modes: {
    standby: '대기',
    step: '스텝',
    slew: '슬루',
    pedestal: '페더스탈',
    ephemeris: '에피메리스',
    passSchedule: '패스 스케줄',
    sunTrack: '태양 추적',
    feed: '피드',
  },

  // 설정 관련 확장
  settings: {
    title: '설정',
    theme: '테마',
    darkMode: '다크 모드',
    lightMode: '라이트 모드',

    // 언어 설정
    'language.current': '현재 언어',
    'language.changed': '{language}로 변경되었습니다',
    'language.select': '언어를 선택하세요',

    // 버전 정보
    version: {
      title: '펌웨어 버전 정보',
      refresh: '버전 정보 새로고침',
      loading: '버전 정보를 불러오는 중...',
      error: '버전 정보 로드 중 오류가 발생했습니다',
      retry: '다시 시도',
      noData: '버전 정보를 불러오려면 새로고침 버튼을 클릭하세요',
      success: '버전 정보를 성공적으로 불러왔습니다',
      firmware: '펌웨어 버전',
      serial: '제품번호',
      boards: {
        mainboard: 'Mainboard',
        mainboardDesc: '메인보드 펌웨어 및 제품번호',
        azimuth: 'Azimuth',
        azimuthDesc: '방위각 축 펌웨어 및 제품번호',
        elevation: 'Elevation',
        elevationDesc: '고도각 축 펌웨어 및 제품번호',
        tilt: 'Tilt',
        tiltDesc: '기울기 축 펌웨어 및 제품번호',
        feed: 'Feed',
        feedDesc: '피드 펌웨어 및 제품번호',
      },
    },

    // 관리자 설정
    admin: {
      title: '관리자 설정',
      description: '시스템 관리자 전용 설정',
      servoPreset: {
        title: '서보 프리셋',
        description: '서보 인코더 프리셋 설정',
        azimuthDesc: '방위각 축 서보 인코더 프리셋',
        elevationDesc: '고도각 축 서보 인코더 프리셋',
        tiltDesc: '기울기 축 서보 인코더 프리셋',
        azimuthButton: 'Azimuth Preset',
        elevationButton: 'Elevation Preset',
        tiltButton: 'Tilt Preset',
        confirmTitle: '서보 프리셋 확인',
        confirmMessage: '{axis} 축 서보 프리셋을 실행하시겠습니까?',
      },
      servoAlarmReset: '서보 알람 리셋',
      mcOnOff: 'M/C On/Off',
      confirm: '실행하시겠습니까?',
      success: '명령이 성공적으로 실행되었습니다',
      error: '명령 실행 중 오류가 발생했습니다',
      axes: {
        azimuth: '방위각',
        elevation: '고도각',
        tilt: '기울기',
      },
      states: {
        on: 'ON',
        off: 'OFF',
      },

      // 서보 프리셋 상세
      servoPresetDetails: {
        azimuthDesc: '방위각 축 서보 인코더 프리셋',
        elevationDesc: '고도각 축 서보 인코더 프리셋',
        tiltDesc: '기울기 축 서보 인코더 프리셋',
        azimuthButton: 'Azimuth Preset',
        elevationButton: 'Elevation Preset',
        tiltButton: 'Tilt Preset',
        confirmTitle: '서보 프리셋 확인',
        confirmMessage: '{axis} 축 서보 프리셋을 실행하시겠습니까?',
      },

      // 서보 알람 리셋 상세
      servoAlarmResetDetails: {
        azimuthDesc: '방위각 축 서보 알람 리셋',
        elevationDesc: '고도각 축 서보 알람 리셋',
        tiltDesc: '기울기 축 서보 알람 리셋',
        azimuthButton: 'Azimuth Reset',
        elevationButton: 'Elevation Reset',
        tiltButton: 'Tilt Reset',
        confirmTitle: '서보 알람 리셋 확인',
        confirmMessage: '{axis} 축 서보 알람을 리셋하시겠습니까?',
      },

      // M/C On/Off 상세
      mcOnOffDetails: {
        title: 'M/C 상태 제어',
        description: 'M/C On/Off 명령 실행',
        confirmTitle: 'M/C On/Off 확인',
        confirmMessage: 'M/C {state} 명령을 실행하시겠습니까?',
      },
    },

    // 일반 설정
    general: {
      title: '일반 설정',
      theme: '테마 설정',
      language: '언어 설정',
    },

    // 연결 설정
    connection: {
      title: '연결 설정',
      websocket: 'WebSocket 서버 주소',
      api: 'API 기본 URL',
      autoReconnect: '연결 끊김 시 자동 재연결',
    },

    // 탭 제목들 (별도로 정의)
    generalTab: '일반 설정',
    systemTab: '시스템 설정',
    connectionTab: '연결 설정',
    languageTab: '언어 설정',
    versionTab: '버전 정보',
    adminTab: '관리자 설정',
  },

  // 공통 버튼들
  buttons: {
    save: '저장',
    cancel: '취소',
    confirm: '확인',
    yes: '예',
    no: '아니오',
    refresh: '새로고침',
    retry: '다시 시도',
    close: '닫기',
  },

  // 에러 메시지
  errors: {
    apiError: 'API 요청 중 오류가 발생했습니다.',
    networkError: '네트워크 연결 문제가 발생했습니다.',
    systemError: '시스템 오류가 발생했습니다.',
    validationError: '잘못된 입력 값입니다.',
    userError: '사용자 오류가 발생했습니다.',
    requiredField: '이 필드는 필수입니다.',
    invalidFormat: '잘못된 형식입니다.',
    minLength: '최소 {min}자 이상 입력해주세요.',
    maxLength: '최대 {max}자까지 입력 가능합니다.',
    minValue: '최소값은 {min}입니다.',
    maxValue: '최대값은 {max}입니다.',
    emailFormat: '잘못된 이메일 형식입니다.',
    numericOnly: '숫자만 입력 가능합니다.',
  },

  // 알림
  notifications: {
    success: '성공',
    error: '오류',
    warning: '경고',
    info: '정보',
    dismissAll: '모든 알림을 닫습니다.',
  },

  // 다이얼로그
  dialogs: {
    confirm: '확인',
    alert: '알림',
    input: '입력',
    inputLabel: '값을 입력해주세요',
    ok: '확인',
    cancel: '취소',
  },

  // ICD 관련
  icd: {
    title: 'ICD 데이터',
    realTime: '실시간',
    status: '상태',
    data: '데이터',
    connection: '연결',
    error: '오류',
  },

  // 하드웨어 에러 메시지
  hardwareErrors: {
    // 서보 알람
    ELEVATION_SERVO_ALARM: 'Elevation 서보 알람',
    ELEVATION_SERVO_ALARM_CODE1: 'Elevation 서보 알람 코드 1',
    ELEVATION_SERVO_ALARM_CODE2: 'Elevation 서보 알람 코드 2',
    ELEVATION_SERVO_ALARM_CODE3: 'Elevation 서보 알람 코드 3',
    ELEVATION_SERVO_ALARM_CODE4: 'Elevation 서보 알람 코드 4',
    ELEVATION_SERVO_ALARM_CODE5: 'Elevation 서보 알람 코드 5',
    AZIMUTH_SERVO_ALARM: 'Azimuth 서보 알람',
    AZIMUTH_SERVO_ALARM_CODE1: 'Azimuth 서보 알람 코드 1',
    AZIMUTH_SERVO_ALARM_CODE2: 'Azimuth 서보 알람 코드 2',
    AZIMUTH_SERVO_ALARM_CODE3: 'Azimuth 서보 알람 코드 3',
    AZIMUTH_SERVO_ALARM_CODE4: 'Azimuth 서보 알람 코드 4',
    AZIMUTH_SERVO_ALARM_CODE5: 'Azimuth 서보 알람 코드 5',
    TRAIN_SERVO_ALARM: 'Train 서보 알람',
    TRAIN_SERVO_ALARM_CODE1: 'Train 서보 알람 코드 1',
    TRAIN_SERVO_ALARM_CODE2: 'Train 서보 알람 코드 2',
    TRAIN_SERVO_ALARM_CODE3: 'Train 서보 알람 코드 3',
    TRAIN_SERVO_ALARM_CODE4: 'Train 서보 알람 코드 4',
    TRAIN_SERVO_ALARM_CODE5: 'Train 서보 알람 코드 5',

    // 인코더 에러
    ELEVATION_ENCODER_ERROR: 'Elevation 인코더 에러',
    AZIMUTH_ENCODER_ERROR: 'Azimuth 인코더 에러',
    TRAIN_ENCODER_ERROR: 'Train 인코더 에러',

    // 전력 관련
    POWER_SURGE_PROTECTOR_FAULT: '전력 서지 보호기 고장',
    POWER_REVERSE_PHASE_FAULT: '전력 역상 고장',

    // 비상 정지
    EMERGENCY_STOP_ACU: '비상 정지 ACU',
    EMERGENCY_STOP_POSITIONER: '비상 정지 포지셔너',

    // 서보 브레이크
    ELEVATION_SERVO_BRAKE_ENGAGED: 'Elevation 서보 브레이크 작동',
    AZIMUTH_SERVO_BRAKE_ENGAGED: 'Azimuth 서보 브레이크 작동',
    TRAIN_SERVO_BRAKE_ENGAGED: 'Train 서보 브레이크 작동',

    // 서보 모터
    ELEVATION_SERVO_MOTOR_ON: 'Elevation 서보 모터 켜짐',
    AZIMUTH_SERVO_MOTOR_ON: 'Azimuth 서보 모터 켜짐',
    TRAIN_SERVO_MOTOR_ON: 'Train 서보 모터 켜짐',

    // 한계 스위치
    ELEVATION_LIMIT_SWITCH_POSITIVE_180: 'Elevation 한계 스위치 +180°',
    ELEVATION_LIMIT_SWITCH_POSITIVE_185: 'Elevation 한계 스위치 +185°',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_0: 'Elevation 한계 스위치 -0°',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_5: 'Elevation 한계 스위치 -5°',
    AZIMUTH_LIMIT_SWITCH_NEGATIVE_275: 'Azimuth 한계 스위치 -275°',
    AZIMUTH_LIMIT_SWITCH_POSITIVE_275: 'Azimuth 한계 스위치 +275°',
    TRAIN_LIMIT_SWITCH_NEGATIVE_275: 'Train 한계 스위치 -275°',
    TRAIN_LIMIT_SWITCH_POSITIVE_275: 'Train 한계 스위치 +275°',

    // 스토우 핀
    ELEVATION_STOW_PIN_ACTIVE: 'Elevation 스토우 핀 활성',
    AZIMUTH_STOW_PIN_ACTIVE: 'Azimuth 스토우 핀 활성',
    TRAIN_STOW_PIN_ACTIVE: 'Train 스토우 핀 활성',

    // 서보 전원
    SERVO_TRAIN_POWER_OFF: 'Train 서보 전원 꺼짐',
    SERVO_ELEVATION_POWER_OFF: 'Elevation 서보 전원 꺼짐',
    SERVO_AZIMUTH_POWER_OFF: 'Azimuth 서보 전원 꺼짐',

    // 프로토콜 에러
    PROTOCOL_ELEVATION_ERROR: 'Elevation 프로토콜 에러',
    PROTOCOL_AZIMUTH_ERROR: 'Azimuth 프로토콜 에러',
    PROTOCOL_TRAIN_ERROR: 'Train 프로토콜 에러',
    PROTOCOL_FEED_ERROR: 'Feed 프로토콜 에러',

    // S-Band 에러
    S_BAND_LNA_LHCP_ERROR: 'S-Band LNA LHCP 에러',
    S_BAND_LNA_RHCP_ERROR: 'S-Band LNA RHCP 에러',
    S_BAND_RF_SWITCH_ERROR: 'S-Band RF 스위치 에러',

    // X-Band 에러
    X_BAND_LNA_LHCP_ERROR: 'X-Band LNA LHCP 에러',
    X_BAND_LNA_RHCP_ERROR: 'X-Band LNA RHCP 에러',

    // 팬 에러
    FAN_ERROR: '팬 에러',

    // 전원 켜짐 상태
    S_BAND_LNA_LHCP_POWER_ON: 'S-Band LNA LHCP 전원 켜짐',
    S_BAND_LNA_RHCP_POWER_ON: 'S-Band LNA RHCP 전원 켜짐',
    S_BAND_RF_SWITCH_RHCP: 'S-Band RF 스위치 RHCP',
    X_BAND_LNA_LHCP_POWER_ON: 'X-Band LNA LHCP 전원 켜짐',
    X_BAND_LNA_RHCP_POWER_ON: 'X-Band LNA RHCP 전원 켜짐',
    FAN_POWER_ON: '팬 전원 켜짐',

    // 해결된 메시지
    ELEVATION_SERVO_ALARM_RESOLVED: 'Elevation 서보 알람 해제',
    ELEVATION_SERVO_ALARM_CODE1_RESOLVED: 'Elevation 서보 알람 코드 1 해제',
    ELEVATION_SERVO_ALARM_CODE2_RESOLVED: 'Elevation 서보 알람 코드 2 해제',
    ELEVATION_SERVO_ALARM_CODE3_RESOLVED: 'Elevation 서보 알람 코드 3 해제',
    ELEVATION_SERVO_ALARM_CODE4_RESOLVED: 'Elevation 서보 알람 코드 4 해제',
    ELEVATION_SERVO_ALARM_CODE5_RESOLVED: 'Elevation 서보 알람 코드 5 해제',
    AZIMUTH_SERVO_ALARM_RESOLVED: 'Azimuth 서보 알람 해제',
    AZIMUTH_SERVO_ALARM_CODE1_RESOLVED: 'Azimuth 서보 알람 코드 1 해제',
    AZIMUTH_SERVO_ALARM_CODE2_RESOLVED: 'Azimuth 서보 알람 코드 2 해제',
    AZIMUTH_SERVO_ALARM_CODE3_RESOLVED: 'Azimuth 서보 알람 코드 3 해제',
    AZIMUTH_SERVO_ALARM_CODE4_RESOLVED: 'Azimuth 서보 알람 코드 4 해제',
    AZIMUTH_SERVO_ALARM_CODE5_RESOLVED: 'Azimuth 서보 알람 코드 5 해제',
    TRAIN_SERVO_ALARM_RESOLVED: 'Train 서보 알람 해제',
    TRAIN_SERVO_ALARM_CODE1_RESOLVED: 'Train 서보 알람 코드 1 해제',
    TRAIN_SERVO_ALARM_CODE2_RESOLVED: 'Train 서보 알람 코드 2 해제',
    TRAIN_SERVO_ALARM_CODE3_RESOLVED: 'Train 서보 알람 코드 3 해제',
    TRAIN_SERVO_ALARM_CODE4_RESOLVED: 'Train 서보 알람 코드 4 해제',
    TRAIN_SERVO_ALARM_CODE5_RESOLVED: 'Train 서보 알람 코드 5 해제',
    ELEVATION_ENCODER_ERROR_RESOLVED: 'Elevation 인코더 에러 해결됨',
    AZIMUTH_ENCODER_ERROR_RESOLVED: 'Azimuth 인코더 에러 해결됨',
    TRAIN_ENCODER_ERROR_RESOLVED: 'Train 인코더 에러 해결됨',
    POWER_SURGE_PROTECTOR_FAULT_RESOLVED: '전력 서지 보호기 정상',
    POWER_REVERSE_PHASE_FAULT_RESOLVED: '전력 역상 정상',
    EMERGENCY_STOP_ACU_RESOLVED: '비상 정지 ACU 해제됨',
    EMERGENCY_STOP_POSITIONER_RESOLVED: '비상 정지 포지셔너 해제됨',
    ELEVATION_SERVO_BRAKE_ENGAGED_RESOLVED: 'Elevation 서보 브레이크 해제됨',
    AZIMUTH_SERVO_BRAKE_ENGAGED_RESOLVED: 'Azimuth 서보 브레이크 해제됨',
    TRAIN_SERVO_BRAKE_ENGAGED_RESOLVED: 'Train 서보 브레이크 해제됨',
    ELEVATION_SERVO_MOTOR_ON_RESOLVED: 'Elevation 서보 모터 꺼짐',
    AZIMUTH_SERVO_MOTOR_ON_RESOLVED: 'Azimuth 서보 모터 꺼짐',
    TRAIN_SERVO_MOTOR_ON_RESOLVED: 'Train 서보 모터 꺼짐',
    ELEVATION_LIMIT_SWITCH_POSITIVE_180_RESOLVED: 'Elevation 한계 스위치 +180° 비활성',
    ELEVATION_LIMIT_SWITCH_POSITIVE_185_RESOLVED: 'Elevation 한계 스위치 +185° 비활성',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_0_RESOLVED: 'Elevation 한계 스위치 -0° 비활성',
    ELEVATION_LIMIT_SWITCH_NEGATIVE_5_RESOLVED: 'Elevation 한계 스위치 -5° 비활성',
    AZIMUTH_LIMIT_SWITCH_NEGATIVE_275_RESOLVED: 'Azimuth 한계 스위치 -275° 비활성',
    AZIMUTH_LIMIT_SWITCH_POSITIVE_275_RESOLVED: 'Azimuth 한계 스위치 +275° 비활성',
    TRAIN_LIMIT_SWITCH_NEGATIVE_275_RESOLVED: 'Train 한계 스위치 -275° 비활성',
    TRAIN_LIMIT_SWITCH_POSITIVE_275_RESOLVED: 'Train 한계 스위치 +275° 비활성',
    ELEVATION_STOW_PIN_ACTIVE_RESOLVED: 'Elevation 스토우 핀 비활성',
    AZIMUTH_STOW_PIN_ACTIVE_RESOLVED: 'Azimuth 스토우 핀 비활성',
    TRAIN_STOW_PIN_ACTIVE_RESOLVED: 'Train 스토우 핀 비활성',
    SERVO_TRAIN_POWER_OFF_RESOLVED: 'Train 서보 전원 켜짐',
    SERVO_ELEVATION_POWER_OFF_RESOLVED: 'Elevation 서보 전원 켜짐',
    SERVO_AZIMUTH_POWER_OFF_RESOLVED: 'Azimuth 서보 전원 켜짐',
    PROTOCOL_ELEVATION_ERROR_RESOLVED: 'Elevation 프로토콜 정상',
    PROTOCOL_AZIMUTH_ERROR_RESOLVED: 'Azimuth 프로토콜 정상',
    PROTOCOL_TRAIN_ERROR_RESOLVED: 'Train 프로토콜 정상',
    PROTOCOL_FEED_ERROR_RESOLVED: 'Feed 프로토콜 정상',
    S_BAND_LNA_LHCP_ERROR_RESOLVED: 'S-Band LNA LHCP 정상',
    S_BAND_LNA_RHCP_ERROR_RESOLVED: 'S-Band LNA RHCP 정상',
    S_BAND_RF_SWITCH_ERROR_RESOLVED: 'S-Band RF 스위치 정상',
    X_BAND_LNA_LHCP_ERROR_RESOLVED: 'X-Band LNA LHCP 정상',
    X_BAND_LNA_RHCP_ERROR_RESOLVED: 'X-Band LNA RHCP 정상',
    FAN_ERROR_RESOLVED: '팬 정상',
    S_BAND_LNA_LHCP_POWER_ON_RESOLVED: 'S-Band LNA LHCP 전원 꺼짐',
    S_BAND_LNA_RHCP_POWER_ON_RESOLVED: 'S-Band LNA RHCP 전원 꺼짐',
    S_BAND_RF_SWITCH_RHCP_RESOLVED: 'S-Band RF 스위치 LHCP',
    X_BAND_LNA_LHCP_POWER_ON_RESOLVED: 'X-Band LNA LHCP 전원 꺼짐',
    X_BAND_LNA_RHCP_POWER_ON_RESOLVED: 'X-Band LNA RHCP 전원 꺼짐',
    FAN_POWER_ON_RESOLVED: '팬 전원 꺼짐',

    // 테스트 에러
    TEST_ERROR: '테스트 에러 발생',
    TEST_ERROR_RESOLVED: '테스트 에러 해결됨',
  },

  // 차트
  charts: {
    azimuth: '방위각',
    elevation: '고각',
    train: '트레인',
    time: '시간',
    angle: '각도',
    speed: '속도',
  },

  // 시스템 설정
  system: {
    title: '시스템 설정',
    categories: {
      location: {
        name: '위치 설정',
        description: '위도, 경도, 고도 설정',
      },
      tracking: {
        name: '추적 설정',
        description: '추적 간격, 기간, 최소고도각',
      },
      stow: {
        name: 'Stow 설정',
        description: 'Stow 각도 및 속도 설정',
      },
      antennaSpec: {
        name: '안테나 사양',
        description: 'True North Offset, Tilt Angle',
      },
      angleLimits: {
        name: '각도 제한',
        description: 'Azimuth, Elevation, Train 제한',
      },
      speedLimits: {
        name: '속도 제한',
        description: 'Azimuth, Elevation, Train 속도 제한',
      },
      offsetLimits: {
        name: '오프셋 제한',
        description: '각도 오프셋, 시간 오프셋 제한',
      },
      algorithm: {
        name: '알고리즘 설정',
        description: 'Geo Min Motion 등 알고리즘 파라미터',
      },
      stepSize: {
        name: '스텝 사이즈 제한',
        description: '안테나 이동 스텝 사이즈 제한',
      },
    },

    // 공통 메시지
    common: {
      changed: '변경됨',
      saved: '저장됨',
      loading: '로딩 중...',
      save: '저장',
      reset: '초기화',
      cancel: '취소',
      apply: '적용',
      success: '설정이 성공적으로 저장되었습니다',
      error: '설정 저장 중 오류가 발생했습니다',
      validationError: '입력값을 확인해주세요',
      applyRecommended: '권장값 적용',
      getCurrentLocation: '현재 위치 가져오기',
    },

    // 위치 설정
    location: {
      title: '위치 설정',
      latitude: '위도 (도)',
      longitude: '경도 (도)',
      altitude: '고도 (미터)',
      latitudeHint: '위도 범위: -90도 ~ 90도',
      longitudeHint: '경도 범위: -180도 ~ 180도',
      altitudeHint: '고도는 0미터 이상이어야 합니다',
      latitudeRequired: '위도를 입력해주세요',
      longitudeRequired: '경도를 입력해주세요',
      altitudeRequired: '고도를 입력해주세요',
      latitudeRange: '위도는 -90도에서 90도 사이여야 합니다',
      longitudeRange: '경도는 -180도에서 180도 사이여야 합니다',
      altitudeMin: '고도는 0미터 이상이어야 합니다',
    },

    // 추적 설정
    tracking: {
      title: '추적 설정',
      interval: '추적 간격 (초)',
      duration: '추적 기간 (분)',
      minElevation: '최소 고도각 (도)',
      intervalHint: '추적 간격은 1초 이상이어야 합니다',
      durationHint: '추적 기간은 1분 이상이어야 합니다',
      minElevationHint: '최소 고도각은 0도에서 90도 사이여야 합니다',
      intervalRequired: '추적 간격을 입력해주세요',
      durationRequired: '추적 기간을 입력해주세요',
      minElevationRequired: '최소 고도각을 입력해주세요',
      intervalMin: '추적 간격은 1초 이상이어야 합니다',
      durationMin: '추적 기간은 1분 이상이어야 합니다',
      minElevationRange: '최소 고도각은 0도에서 90도 사이여야 합니다',
    },

    // Stow 설정
    stow: {
      title: 'Stow 설정',
      azimuthAngle: 'Azimuth Stow 각도 (도)',
      elevationAngle: 'Elevation Stow 각도 (도)',
      trainAngle: 'Train Stow 각도 (도)',
      azimuthSpeed: 'Azimuth Stow 속도 (도/초)',
      elevationSpeed: 'Elevation Stow 속도 (도/초)',
      trainSpeed: 'Train Stow 속도 (도/초)',
      azimuthAngleHint: 'Azimuth Stow 각도 범위: -180도 ~ 180도',
      elevationAngleHint: 'Elevation Stow 각도 범위: 0도 ~ 90도',
      trainAngleHint: 'Train Stow 각도 범위: -180도 ~ 180도',
      speedHint: '속도는 0.1도/초 이상이어야 합니다',
      azimuthAngleRequired: 'Azimuth Stow 각도를 입력해주세요',
      elevationAngleRequired: 'Elevation Stow 각도를 입력해주세요',
      trainAngleRequired: 'Train Stow 각도를 입력해주세요',
      azimuthSpeedRequired: 'Azimuth Stow 속도를 입력해주세요',
      elevationSpeedRequired: 'Elevation Stow 속도를 입력해주세요',
      trainSpeedRequired: 'Train Stow 속도를 입력해주세요',
      speedMin: '속도는 0.1도/초 이상이어야 합니다',
    },

    // 안테나 사양 설정
    antennaSpec: {
      title: '안테나 사양 설정',
      trueNorthOffset: 'True North Offset (도)',
      tiltAngle: 'Tilt Angle (도)',
      trueNorthOffsetHint: 'True North Offset 범위: -180도 ~ 180도',
      tiltAngleHint: 'Tilt Angle 범위: -90도 ~ 90도',
      trueNorthOffsetRequired: 'True North Offset을 입력해주세요',
      tiltAngleRequired: 'Tilt Angle을 입력해주세요',
      trueNorthOffsetRange: 'True North Offset은 -180도에서 180도 사이여야 합니다',
      tiltAngleRange: 'Tilt Angle은 -90도에서 90도 사이여야 합니다',
    },

    // 각도 제한 설정
    angleLimits: {
      title: '각도 제한 설정',
      azimuthMin: 'Azimuth 최소 각도 (도)',
      azimuthMax: 'Azimuth 최대 각도 (도)',
      elevationMin: 'Elevation 최소 각도 (도)',
      elevationMax: 'Elevation 최대 각도 (도)',
      trainMin: 'Train 최소 각도 (도)',
      trainMax: 'Train 최대 각도 (도)',
      azimuthHint: 'Azimuth 범위: -180도 ~ 180도',
      elevationHint: 'Elevation 범위: 0도 ~ 90도',
      trainHint: 'Train 범위: -180도 ~ 180도',
      azimuthMinRequired: 'Azimuth 최소 각도를 입력해주세요',
      azimuthMaxRequired: 'Azimuth 최대 각도를 입력해주세요',
      elevationMinRequired: 'Elevation 최소 각도를 입력해주세요',
      elevationMaxRequired: 'Elevation 최대 각도를 입력해주세요',
      trainMinRequired: 'Train 최소 각도를 입력해주세요',
      trainMaxRequired: 'Train 최대 각도를 입력해주세요',
      azimuthRange: 'Azimuth는 -180도에서 180도 사이여야 합니다',
      elevationRange: 'Elevation는 0도에서 90도 사이여야 합니다',
      trainRange: 'Train는 -180도에서 180도 사이여야 합니다',
    },

    // 속도 제한 설정
    speedLimits: {
      title: '속도 제한 설정',
      azimuthMax: 'Azimuth 최대 속도 (도/초)',
      elevationMax: 'Elevation 최대 속도 (도/초)',
      trainMax: 'Train 최대 속도 (도/초)',
      speedHint: '속도는 0.1도/초 이상이어야 합니다',
      azimuthRequired: 'Azimuth 최대 속도를 입력해주세요',
      elevationRequired: 'Elevation 최대 속도를 입력해주세요',
      trainRequired: 'Train 최대 속도를 입력해주세요',
      speedMin: '속도는 0.1도/초 이상이어야 합니다',
    },

    // 오프셋 제한 설정
    offsetLimits: {
      title: '오프셋 제한 설정',
      angleOffset: '각도 오프셋 (도)',
      timeOffset: '시간 오프셋 (초)',
      angleOffsetHint: '각도 오프셋 범위: -180도 ~ 180도',
      timeOffsetHint: '시간 오프셋 범위: -3600초 ~ 3600초',
      angleOffsetRequired: '각도 오프셋을 입력해주세요',
      timeOffsetRequired: '시간 오프셋을 입력해주세요',
      angleOffsetRange: '각도 오프셋은 -180도에서 180도 사이여야 합니다',
      timeOffsetRange: '시간 오프셋은 -3600초에서 3600초 사이여야 합니다',
    },

    // 알고리즘 설정
    algorithm: {
      title: '알고리즘 설정',
      geoMinMotion: 'Geo Min Motion (도)',
      trackingAccuracy: '추적 정확도 (도)',
      predictionTime: '예측 시간 (초)',
      geoMinMotionHint: 'Geo Min Motion 범위: 0.001도 ~ 1도',
      trackingAccuracyHint: '추적 정확도 범위: 0.001도 ~ 1도',
      predictionTimeHint: '예측 시간 범위: 1초 ~ 3600초',
      geoMinMotionRequired: 'Geo Min Motion을 입력해주세요',
      trackingAccuracyRequired: '추적 정확도를 입력해주세요',
      predictionTimeRequired: '예측 시간을 입력해주세요',
      geoMinMotionRange: 'Geo Min Motion은 0.001도에서 1도 사이여야 합니다',
      trackingAccuracyRange: '추적 정확도는 0.001도에서 1도 사이여야 합니다',
      predictionTimeRange: '예측 시간은 1초에서 3600초 사이여야 합니다',
    },

    // 스텝 사이즈 제한 설정
    stepSize: {
      title: '스텝 사이즈 제한 설정',
      minStepSize: '스텝 사이즈 최소값 (도)',
      maxStepSize: '스텝 사이즈 최대값 (도)',
      minStepSizeHint: '스텝 사이즈의 최소 제한값',
      maxStepSizeHint: '스텝 사이즈의 최대 제한값',
      minStepSizeRequired: '최소 스텝 사이즈를 입력해주세요',
      maxStepSizeRequired: '최대 스텝 사이즈를 입력해주세요',
      minStepSizeMin: '최소 스텝 사이즈는 0.001도 이상이어야 합니다',
      maxStepSizeMin: '최대 스텝 사이즈는 0.001도 이상이어야 합니다',
      minStepSizeMax: '최소 스텝 사이즈는 180도 이하여야 합니다',
      maxStepSizeMax: '최대 스텝 사이즈는 180도 이하여야 합니다',
      applyRecommended: '권장값 적용',
      descriptionTitle: '스텝 사이즈 제한 설명',
      description1: '스텝 사이즈 제한:',
      description2: '안테나 이동 시 한 번에 움직일 수 있는 최소/최대 각도를 설정합니다.',
      description3: '이 값들은 안테나의 정밀한 제어와 안전한 이동을 위해 사용됩니다.',
      minValueTitle: '최소값',
      minValueDesc: '너무 작은 움직임을 방지하여 시스템 안정성 확보',
      maxValueTitle: '최대값',
      maxValueDesc: '너무 큰 움직임을 방지하여 안테나 보호',
      recommendedRange: '권장 범위: 0.1도 ~ 180도',
    },
  },
}
