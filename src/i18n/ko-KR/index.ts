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

  // 설정
  settings: {
    title: '설정',
    general: '일반',
    system: '시스템',
    connection: '연결',
    language: '언어',
    theme: '테마',
    darkMode: '다크 모드',
    lightMode: '라이트 모드',
  },

  // 에러 메시지
  errors: {
    apiError: 'API 요청 중 오류가 발생했습니다.',
    networkError: '네트워크 연결에 문제가 있습니다.',
    systemError: '시스템 오류가 발생했습니다.',
    validationError: '입력값이 올바르지 않습니다.',
    userError: '사용자 오류가 발생했습니다.',
    requiredField: '필수 입력 항목입니다.',
    invalidFormat: '올바른 형식이 아닙니다.',
    minLength: '최소 {min}자 이상 입력해주세요.',
    maxLength: '최대 {max}자까지 입력 가능합니다.',
    minValue: '최소값은 {min}입니다.',
    maxValue: '최대값은 {max}입니다.',
    emailFormat: '올바른 이메일 형식이 아닙니다.',
    numericOnly: '숫자만 입력 가능합니다.',
  },

  // 알림
  notifications: {
    success: '성공',
    error: '오류',
    warning: '경고',
    info: '정보',
    dismissAll: '모든 알림 닫기',
  },

  // 다이얼로그
  dialogs: {
    confirm: '확인',
    alert: '알림',
    input: '입력',
    inputLabel: '값을 입력하세요',
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

  // 차트
  charts: {
    azimuth: '방위각',
    elevation: '고각',
    train: '트레인',
    time: '시간',
    angle: '각도',
    speed: '속도',
  },
}
