/**
 * 하드웨어 에러 로그 타입 정의
 */
export interface HardwareErrorLog {
  /** 에러 로그 고유 ID */
  id: string
  /** 발생 시간 */
  timestamp: string
  /** 대분류 카테고리 */
  category:
    | 'POWER'
    | 'PROTOCOL'
    | 'EMERGENCY'
    | 'SERVO_POWER'
    | 'STOW'
    | 'POSITIONER'
    | 'FEED'
    | 'TEST'
  /** 심각도 */
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
  /** 에러 메시지 (다국어) */
  message: {
    ko: string
    en: string
  }
  /** 컴포넌트명 */
  component: string
  /** 해결 여부 */
  isResolved: boolean
  /** 해결 시간 */
  resolvedAt?: string
  /** 해결 메시지 (다국어) */
  resolvedMessage?: {
    ko: string
    en: string
  }
}

/**
 * 에러 카테고리 정의
 */
export const ERROR_CATEGORIES = {
  POWER: 'Power Status',
  PROTOCOL: 'Protocol Status',
  EMERGENCY: 'Emergency Stop Status',
  SERVO_POWER: 'Servo Power Status',
  STOW: 'Stow Pin Status',
  POSITIONER: 'Positioner Status',
  FEED: 'Feed Status',
  TEST: 'Test Status',
} as const

/**
 * 에러 심각도 정의
 */
export const ERROR_SEVERITY = {
  INFO: '정보',
  WARNING: '경고',
  ERROR: '오류',
  CRITICAL: '심각',
} as const
