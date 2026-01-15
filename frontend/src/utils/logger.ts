/**
 * 환경별 로깅 유틸리티
 *
 * 사용법:
 *   import { logger } from '@/utils/logger'
 *
 *   logger.debug('디버그 메시지', data)      // 개발환경에서만 출력
 *   logger.info('정보 메시지', data)         // 개발환경에서만 출력
 *   logger.warn('경고 메시지', data)         // 항상 출력
 *   logger.error('에러 메시지', error)       // 항상 출력
 *
 * 특징:
 *   - 개발환경: 모든 로그 출력 (색상 + 타임스탬프)
 *   - Production: warn, error만 출력
 *   - 카테고리별 필터링 가능
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error'

interface LoggerOptions {
  category?: string
  showTimestamp?: boolean
}

const isDev = import.meta.env.DEV

// 로그 레벨별 색상 (콘솔용)
const LOG_COLORS = {
  debug: '#9E9E9E', // gray
  info: '#2196F3', // blue
  warn: '#FF9800', // orange
  error: '#F44336', // red
} as const

// 로그 레벨별 이모지
const LOG_EMOJI = {
  debug: '🔍',
  info: 'ℹ️',
  warn: '⚠️',
  error: '❌',
} as const

/**
 * 타임스탬프 생성
 */
const getTimestamp = (): string => {
  const now = new Date()
  const time = now.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
  const ms = now.getMilliseconds().toString().padStart(3, '0')
  return `${time}.${ms}`
}

/**
 * 로그 출력 함수
 */
const log = (
  level: LogLevel,
  message: string,
  data?: unknown,
  options: LoggerOptions = {}
): void => {
  const { category, showTimestamp = true } = options

  // Production에서는 warn, error만 출력
  if (!isDev && level !== 'warn' && level !== 'error') {
    return
  }

  const timestamp = showTimestamp ? `[${getTimestamp()}]` : ''
  const categoryTag = category ? `[${category}]` : ''
  const emoji = LOG_EMOJI[level]
  const color = LOG_COLORS[level]

  const prefix = `${emoji} ${timestamp}${categoryTag}`

  // 콘솔 메서드 선택
  const consoleFn = level === 'error' ? console.error
    : level === 'warn' ? console.warn
    : console.log

  // 개발환경: 색상 적용
  if (isDev) {
    if (data !== undefined) {
      consoleFn(`%c${prefix} ${message}`, `color: ${color}`, data)
    } else {
      consoleFn(`%c${prefix} ${message}`, `color: ${color}`)
    }
  } else {
    // Production: 색상 없이
    if (data !== undefined) {
      consoleFn(`${prefix} ${message}`, data)
    } else {
      consoleFn(`${prefix} ${message}`)
    }
  }
}

/**
 * 메인 로거 객체
 */
export const logger = {
  /**
   * 디버그 로그 (개발환경에서만 출력)
   */
  debug: (message: string, data?: unknown, options?: LoggerOptions): void => {
    log('debug', message, data, options)
  },

  /**
   * 정보 로그 (개발환경에서만 출력)
   */
  info: (message: string, data?: unknown, options?: LoggerOptions): void => {
    log('info', message, data, options)
  },

  /**
   * 경고 로그 (항상 출력)
   */
  warn: (message: string, data?: unknown, options?: LoggerOptions): void => {
    log('warn', message, data, options)
  },

  /**
   * 에러 로그 (항상 출력)
   */
  error: (message: string, data?: unknown, options?: LoggerOptions): void => {
    log('error', message, data, options)
  },

  /**
   * 카테고리별 로거 생성
   *
   * 사용법:
   *   const log = logger.create('ICD')
   *   log.debug('WebSocket 연결')
   *   log.error('연결 실패', error)
   */
  create: (category: string) => ({
    debug: (message: string, data?: unknown): void => {
      log('debug', message, data, { category })
    },
    info: (message: string, data?: unknown): void => {
      log('info', message, data, { category })
    },
    warn: (message: string, data?: unknown): void => {
      log('warn', message, data, { category })
    },
    error: (message: string, data?: unknown): void => {
      log('error', message, data, { category })
    },
  }),
}

export default logger
