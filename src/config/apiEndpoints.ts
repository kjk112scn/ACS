/**
 * API 엔드포인트 상수 모음
 * API 경로는 이 파일에서 중앙 집중식으로 관리합니다.
 */

export const API_ENDPOINTS = {
  // ICD 관련 엔드포인트
  ICD: {
    EMERGENCY_STOP: '/icd/on-emergency-stop-command',
    REALTIME_DATA: '/icd/realtime-data',
    // 기타 ICD 관련 엔드포인트들...
  },

  // Sun Track 관련 엔드포인트
  SUN_TRACK: {
    START: '/sun-track/start-sun-track',
    STOP: '/sun-track/stop-sun-track',
    // 기타 Sun Track 관련 엔드포인트들...
  },

  // Ephemeris 관련 엔드포인트
  EPHEMERIS: {
    GENERATE: '/satellite/ephemeris/generate',
    MASTER: '/satellite/ephemeris/master',
    DETAIL: '/satellite/ephemeris/detail',
    TLE_CALCULATE: '/satellite/tle/calculate',
    // 기타 Ephemeris 관련 엔드포인트들...
  },

  // 기타 API 엔드포인트들...
} as const

// 타입 추출을 위한 유틸리티 타입
export type ApiEndpoints = typeof API_ENDPOINTS
export type EndpointPath = {
  [K in keyof ApiEndpoints]: {
    [P in keyof ApiEndpoints[K]]: ApiEndpoints[K][P]
  }[keyof ApiEndpoints[K]]
}[keyof ApiEndpoints]

/**
 * API 엔드포인트 전체 경로를 생성하는 헬퍼 함수
 * @param path 엔드포인트 경로 (예: API_ENDPOINTS.ICD.EMERGENCY_STOP)
 * @returns 전체 API URL
 */
export const getApiUrl = (path: string): string => {
  return path // baseURL은 axios 인스턴스에서 이미 설정되어 있으므로 상대 경로만 반환
}
