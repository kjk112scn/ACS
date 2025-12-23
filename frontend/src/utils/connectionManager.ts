/**
 * 백엔드 연결 상태 관리 유틸리티
 * 백엔드 재시작 감지 시 localStorage 초기화
 */

const CONNECTION_STATE_KEY = 'acs-connection-state'
const LAST_DISCONNECT_TIME_KEY = 'acs-last-disconnect-time'
const INITIAL_CONNECTION_KEY = 'acs-initial-connection'

type ConnectionState = 'connected' | 'disconnected' | 'reconnecting' | 'initial'

// 안전한 localStorage 접근
const safeSetItem = (key: string, value: string): boolean => {
  try {
    localStorage.setItem(key, value)
    return true
  } catch (error) {
    console.error(`❌ localStorage 저장 실패 (${key}):`, error)
    return false
  }
}

const safeGetItem = (key: string): string | null => {
  try {
    return localStorage.getItem(key)
  } catch (error) {
    console.error(`❌ localStorage 조회 실패 (${key}):`, error)
    return null
  }
}

const safeRemoveItem = (key: string): boolean => {
  try {
    localStorage.removeItem(key)
    return true
  } catch (error) {
    console.error(`❌ localStorage 삭제 실패 (${key}):`, error)
    return false
  }
}

/**
 * 연결 상태 저장
 */
export const saveConnectionState = (state: ConnectionState): void => {
  safeSetItem(CONNECTION_STATE_KEY, state)

  if (state === 'disconnected') {
    safeSetItem(LAST_DISCONNECT_TIME_KEY, String(Date.now()))
  } else if (state === 'connected') {
    const isInitial = safeGetItem(INITIAL_CONNECTION_KEY) === null
    if (isInitial) {
      safeSetItem(INITIAL_CONNECTION_KEY, 'true')
    }
  }

  console.log(`✅ 연결 상태 저장: ${state}`)
}

/**
 * 마지막 연결 끊김 시간 조회
 */
export const getLastDisconnectTime = (): number | null => {
  const saved = safeGetItem(LAST_DISCONNECT_TIME_KEY)
  return saved ? parseInt(saved, 10) : null
}

/**
 * 저장된 연결 상태 조회
 */
export const getSavedConnectionState = (): ConnectionState | null => {
  const saved = safeGetItem(CONNECTION_STATE_KEY)
  return (saved as ConnectionState) || null
}

/**
 * 초기 접속 여부 확인
 */
export const isInitialConnection = (): boolean => {
  return safeGetItem(INITIAL_CONNECTION_KEY) === null
}

/**
 * 모든 ACS 관련 localStorage 데이터 초기화
 */
export const clearACSLocalStorage = (): void => {
  const keysToRemove = ['pass-schedule-data', 'ephemeris-designation-data', 'hardware-error-logs']

  let clearedCount = 0
  keysToRemove.forEach((key) => {
    if (safeGetItem(key)) {
      safeRemoveItem(key)
      clearedCount++
      console.log(`✅ localStorage 초기화: ${key}`)
    }
  })

  if (clearedCount > 0) {
    console.log(`✅ 총 ${clearedCount}개의 localStorage 데이터 초기화 완료`)
  } else {
    console.log('ℹ️ 초기화할 localStorage 데이터 없음')
  }
}

/**
 * 연결 재설정 감지 및 처리
 * ✅ 백엔드 재시작만 감지 (브라우저 재시작은 무시)
 *
 * @param isConnected - 현재 연결 상태
 * @param options - 추가 옵션
 * @returns 재연결되었으면 true
 */
let lastProcessedTime = 0
const PROCESS_THROTTLE = 100 // 100ms 내 중복 호출 방지

export const handleConnectionChange = (
  isConnected: boolean,
  options: {
    onReconnected?: () => void
    minDisconnectDuration?: number // 최소 끊김 시간 (ms) - 기본 5초
  } = {},
): boolean => {
  try {
    // 중복 호출 방지
    const now = Date.now()
    if (now - lastProcessedTime < PROCESS_THROTTLE) {
      console.log('⚠️ handleConnectionChange 중복 호출 방지')
      return false
    }
    lastProcessedTime = now

    const { onReconnected, minDisconnectDuration = 5000 } = options

    const previousState = getSavedConnectionState()
    const lastDisconnectTime = getLastDisconnectTime()
    const isInitial = isInitialConnection()

    if (isConnected) {
      // 첫 접속 처리
      if (isInitial) {
        console.log('🆕 첫 접속 - localStorage 초기화 안 함 (초기화할 데이터 없음)')
        saveConnectionState('connected')
        return false
      }

      // 백엔드 재시작 후 재접속 감지
      if (previousState === 'disconnected' || previousState === 'reconnecting') {
        const disconnectDuration = lastDisconnectTime ? Date.now() - lastDisconnectTime : 0

        // ✅ 5초 이상 끊어졌으면 백엔드 재시작으로 간주
        const isServerRestart = disconnectDuration >= minDisconnectDuration

        if (isServerRestart) {
          console.log('🔄 백엔드 재시작 감지 - localStorage 초기화:', {
            disconnectDuration: `${Math.round(disconnectDuration / 1000)}초`,
            lastDisconnectTime: lastDisconnectTime
              ? new Date(lastDisconnectTime).toISOString()
              : null,
          })

          // localStorage 초기화
          clearACSLocalStorage()

          saveConnectionState('connected')

          if (onReconnected) {
            onReconnected()
          }

          return true // 재연결됨
        } else {
          // 짧은 시간 내 재연결 = 백엔드가 살아있었음
          console.log('ℹ️ 짧은 시간 내 재연결 - 백엔드가 살아있었음 (초기화 안 함)', {
            disconnectDuration: `${Math.round(disconnectDuration / 1000)}초`,
          })
          saveConnectionState('connected')
          return false
        }
      } else {
        // 이미 연결된 상태
        saveConnectionState('connected')
        return false
      }
    } else {
      // 연결 끊김
      saveConnectionState('disconnected')
      return false
    }
  } catch (error) {
    console.error('❌ 연결 상태 처리 실패:', error)
    return false
  }
}
