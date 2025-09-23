// i18n 번역 키 타입 정의
export type I18nKey =
  // 공통
  | 'common.save'
  | 'common.cancel'
  | 'common.confirm'
  | 'common.delete'
  | 'common.edit'
  | 'common.add'
  | 'common.search'
  | 'common.reset'
  | 'common.close'
  | 'common.back'
  | 'common.next'
  | 'common.previous'
  | 'common.loading'
  | 'common.success'
  | 'common.error'
  | 'common.warning'
  | 'common.info'
  | 'common.yes'
  | 'common.no'

  // 로그인
  | 'login.title'
  | 'login.username'
  | 'login.password'
  | 'login.loginButton'
  | 'login.invalidCredentials'
  | 'login.loginSuccess'

  // 대시보드
  | 'dashboard.title'
  | 'dashboard.serverStatus'
  | 'dashboard.connected'
  | 'dashboard.connecting'
  | 'dashboard.disconnected'
  | 'dashboard.communication'
  | 'dashboard.commandTime'
  | 'dashboard.azimuth'
  | 'dashboard.elevation'
  | 'dashboard.train'
  | 'dashboard.cmd'
  | 'dashboard.actual'
  | 'dashboard.speed'

  // 모드
  | 'modes.standby'
  | 'modes.step'
  | 'modes.slew'
  | 'modes.pedestal'
  | 'modes.ephemeris'
  | 'modes.passSchedule'
  | 'modes.sunTrack'
  | 'modes.feed'

  // 설정
  | 'settings.title'
  | 'settings.general'
  | 'settings.system'
  | 'settings.connection'
  | 'settings.language'
  | 'settings.theme'
  | 'settings.darkMode'
  | 'settings.lightMode'

  // 에러 메시지
  | 'errors.apiError'
  | 'errors.networkError'
  | 'errors.systemError'
  | 'errors.validationError'
  | 'errors.userError'
  | 'errors.requiredField'
  | 'errors.invalidFormat'
  | 'errors.minLength'
  | 'errors.maxLength'
  | 'errors.minValue'
  | 'errors.maxValue'
  | 'errors.emailFormat'
  | 'errors.numericOnly'

  // 알림
  | 'notifications.success'
  | 'notifications.error'
  | 'notifications.warning'
  | 'notifications.info'
  | 'notifications.dismissAll'

  // 다이얼로그
  | 'dialogs.confirm'
  | 'dialogs.alert'
  | 'dialogs.input'
  | 'dialogs.inputLabel'
  | 'dialogs.ok'
  | 'dialogs.cancel'

  // ICD 관련
  | 'icd.title'
  | 'icd.realTime'
  | 'icd.status'
  | 'icd.data'
  | 'icd.connection'
  | 'icd.error'

  // 차트
  | 'charts.azimuth'
  | 'charts.elevation'
  | 'charts.train'
  | 'charts.time'
  | 'charts.angle'
  | 'charts.speed'

// 타입 안전한 번역 함수
export type TranslateFunction = (key: I18nKey, params?: Record<string, unknown>) => string

// 언어 타입
export type Language = 'ko-KR' | 'en-US'
