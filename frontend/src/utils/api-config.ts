/**
 * API 설정 유틸리티
 * 모든 API 및 WebSocket URL을 동적으로 생성하는 중앙 관리 파일
 */

/**
 * 현재 브라우저가 접속한 호스트를 기반으로 API 베이스 URL 생성
 * @returns API 베이스 URL (예: http://192.168.0.201:8080/api)
 */
export const getApiBaseUrl = (): string => {
  const protocol = window.location.protocol // http: or https:
  const hostname = window.location.hostname
  const port = '8080'
  return `${protocol}//${hostname}:${port}/api`
}

/**
 * 현재 브라우저가 접속한 호스트를 기반으로 WebSocket URL 생성
 * @returns WebSocket URL (예: ws://192.168.0.201:8080/ws)
 */
export const getWebSocketUrl = (): string => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const hostname = window.location.hostname
  const port = '8080'
  return `${protocol}//${hostname}:${port}/ws`
}

/**
 * API 및 WebSocket 서버 포트 (중앙 관리)
 */
export const API_PORT = '8080'

/**
 * 프론트엔드 서버 포트
 */
export const FRONTEND_PORT = '8083'
