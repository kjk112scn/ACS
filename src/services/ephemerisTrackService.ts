import { api } from 'boot/axios'
import type { EphemerisTrackRequest, ScheduleItem, ScheduleDetailItem } from '../types/ephemerisTrack'
import { API_ENDPOINTS } from '../config/apiEndpoints'

/**
 * TLE 파싱 관련 오류 클래스
 */
export class TLEParseError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'TLEParseError'
    Object.setPrototypeOf(this, TLEParseError.prototype)
  }
}

/**
 * API 호출 관련 오류 클래스
 */
export class ApiError extends Error {
  status: number
  code: string | undefined  // 명시적으로 undefined 허용

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code  // optional로 전달된 code는 undefined가 될 수 있음
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

/**
 * 위성 궤도 추적 관련 API 서비스
 */
class EphemerisTrackService {
  /**
   * TLE(Two-Line Element) 텍스트를 파싱하여 표준화된 형식으로 변환합니다.
   * @param tleText - 파싱할 TLE 텍스트 (2줄 또는 3줄 형식)
   * @returns 파싱된 TLE 데이터
   * @throws {TLEParseError} TLE 데이터 파싱 및 검증
   */
  parseTLEData(tleText: string): EphemerisTrackRequest {
    if (!tleText || typeof tleText !== 'string') {
      throw new TLEParseError('TLE 데이터가 유효하지 않습니다')
    }

    const lines = tleText
      .split('\n')
      .map(line => line.trim())
      .filter(line => line !== '')

    if (lines.length < 2) {
      throw new TLEParseError('TLE 형식이 올바르지 않습니다. 최소 2줄이 필요합니다.')
    }

    let tleLine1 = ''
    let tleLine2 = ''

    // 3줄 형식 (이름 + TLE 2줄) 또는 2줄 형식 (TLE 2줄) 처리
    if (lines.length >= 3 && lines[1]?.startsWith('1 ') && lines[2]?.startsWith('2 ')) {
      // 첫 번째 줄은 위성 이름이지만 현재 요청에서는 사용하지 않음
      tleLine1 = lines[1]
      tleLine2 = lines[2]
    } else if (lines[0]?.startsWith('1 ') && lines[1]?.startsWith('2 ')) {
      tleLine1 = lines[0]
      tleLine2 = lines[1]
    } else {
      throw new TLEParseError('TLE 형식이 올바르지 않습니다. 라인은 "1 " 또는 "2 "로 시작해야 합니다.')
    }

    // TLE 라인 유효성 검사
    if (!tleLine1.startsWith('1 ') || !tleLine2.startsWith('2 ')) {
      throw new TLEParseError('TLE 형식이 올바르지 않습니다. 첫 번째 라인은 "1 "로, 두 번째 라인은 "2 "로 시작해야 합니다.')
    }

    // 현재 날짜를 기본값으로 사용
    const now = new Date()
    const tomorrow = new Date(now)
    tomorrow.setDate(now.getDate() + 1)
    
    return {
      tleLine1,
      tleLine2,
      startTime: now.toISOString(),
      endTime: tomorrow.toISOString(),
      stepSize: 60 // 기본값으로 60초 간격 사용
    }
  }

  /**
   * API 호출을 위한 공통 에러 처리 함수
   */
  private handleApiError(error: unknown, defaultMessage: string): never {
    let errorMessage = defaultMessage
    let statusCode = 500
    let errorCode: string | undefined

    if (error instanceof Error) {
      console.error(`API 오류: ${error.message}`, error)
      
      if ('isAxiosError' in error) {
        // Axios 에러 타입 안전하게 처리
        const axiosError = error as {
          response?: { status?: number; data?: { message?: string } }
          code?: string
        }
        statusCode = axiosError.response?.status || 500
        errorCode = axiosError.code
        errorMessage = axiosError.response?.data?.message || errorMessage
      }
      
      throw new ApiError(errorMessage, statusCode, errorCode)
    }
    
    const unknownError = new Error('알 수 없는 오류가 발생했습니다')
    throw new ApiError(unknownError.message, 500)
  }

  /**
   * 위성 궤도 추적 데이터를 생성합니다.
   * @param request - TLE 데이터를 포함한 요청 객체
   * @returns 생성된 추적 데이터
   * @throws {ApiError} API 호출 실패 시
   */
  async generateEphemerisTrack(request: EphemerisTrackRequest): Promise<unknown> {
    try {
      // 요청 유효성 검사
      if (!request.tleLine1 || !request.tleLine2) {
        throw new Error('TLE 데이터가 유효하지 않습니다')
      }

      const response = await api.post(API_ENDPOINTS.EPHEMERIS.GENERATE, request)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위성 궤도 추적 데이터 생성에 실패했습니다')
    }
  }

  /**
   * 모든 마스터 데이터를 조회합니다.
   * @returns 마스터 데이터 배열
   * @throws {ApiError} API 호출 실패 시
   */
  async fetchEphemerisMasterData(): Promise<ScheduleItem[]> {
    try {
      const response = await api.get('/ephemeris/master')
      return response.data
    } catch (error) {
      return this.handleApiError(error, '마스터 데이터 조회에 실패했습니다')
    }
  }

  /**
   * 특정 마스터 ID에 대한 세부 데이터를 조회합니다.
   * @param mstId - 조회할 마스터 ID
   * @returns 세부 데이터 배열
   * @throws {ApiError} API 호출 실패 시
   */
  async fetchEphemerisDetailData(mstId: number): Promise<ScheduleDetailItem[]> {
    try {
      const response = await api.get(`/ephemeris/detail/${mstId}`)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '세부 데이터 조회에 실패했습니다')
    }
  }

  /**
   * 특정 마스터 ID의 데이터를 삭제합니다.
   * @param mstId - 삭제할 마스터 ID
   * @returns 삭제 성공 여부
   * @throws {ApiError} API 호출 실패 시
   */
  async deleteEphemerisData(mstId: number): Promise<boolean> {
    try {
      await api.delete(`/ephemeris/${mstId}`)
      return true
    } catch (error) {
      return this.handleApiError(error, '데이터 삭제에 실패했습니다')
    }
  }
}

export const ephemerisTrackService = new EphemerisTrackService()

export type { EphemerisTrackRequest, ScheduleItem, ScheduleDetailItem }
