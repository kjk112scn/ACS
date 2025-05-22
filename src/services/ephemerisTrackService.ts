import { api } from 'boot/axios'
import { API_ENDPOINTS } from '../config/apiEndpoints'

// 타입 정의를 직접 선언하여 충돌 방지
export interface ScheduleItem {
  No: number
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string
  MaxElevation: number
  CreationDate: string
  Creator: string
  [key: string]: string | number | boolean | null | undefined
}

export interface ScheduleDetailItem {
  Time: string
  Azimuth: number
  Elevation: number
  [key: string]: string | number | boolean | null | undefined
}

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
  code: string | undefined // 명시적으로 undefined 허용

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code // optional로 전달된 code는 undefined가 될 수 있음
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
  parseTLEData(tleText: string): {
    tleLine1: string
    tleLine2: string
    satelliteName: string | null
  } {
    if (!tleText || typeof tleText !== 'string') {
      throw new TLEParseError('TLE 데이터가 유효하지 않습니다')
    }

    // 줄바꿈 문자 정규화
    const normalizedText = tleText
      .replace(/\r\n/g, '\n') // Windows 줄바꿈을 \n으로 통일
      .replace(/\r/g, '\n') // Mac 이전 버전 줄바꿈 처리

    // 줄바꿈 문자로 분리하고 빈 줄 제거
    const lines = normalizedText.split('\n').filter((line) => line.trim() !== '')

    if (lines.length < 2) {
      throw new TLEParseError('TLE 형식이 올바르지 않습니다. 최소 2줄이 필요합니다.')
    }

    let tleLine1 = ''
    let tleLine2 = ''
    let satelliteName = null

    if (lines.length >= 3) {
      satelliteName = lines[0]?.trim() || ''
      tleLine1 = lines[1]?.trim() || ''
      tleLine2 = lines[2]?.trim() || ''
    } else if (lines.length >= 2) {
      tleLine1 = lines[0]?.trim() || ''
      tleLine2 = lines[1]?.trim() || ''
    } else {
      tleLine1 = lines[0]?.trim() || ''
      tleLine2 = ''
    }

    // TLE 첫 번째 줄에서 위성 ID 추출 (27424U와 같은 형식)
    if (tleLine1 && !satelliteName) {
      // TLE 첫 번째 줄의 형식: 1 NNNNNX ... (여기서 NNNNN은 위성 번호, X는 등록 문자)
      const satelliteIdMatch = tleLine1.match(/^1\s+(\d+[A-Z])\s+/)
      if (satelliteIdMatch && satelliteIdMatch[1]) {
        satelliteName = satelliteIdMatch[1] // 예: 27424U
      }
    }

    return { tleLine1, tleLine2, satelliteName }
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
   * Ephemeris Designation 명령을 전송합니다.
   * @param command - 방위각, 고도각, 틸트각을 포함한 명령 객체
   * @returns API 응답 데이터
   * @throws {ApiError} API 호출 실패 시
   */
  async sendEphemerisCommand(command: {
    azimuthAngle: number
    elevationAngle: number
    tiltAngle: number
    timestamp: string
  }): Promise<unknown> {
    try {
      const response = await api.post('/icd/set-position', command)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위치 지정 명령 전송에 실패했습니다')
    }
  }

  /**
   * 정지 명령을 전송합니다.
   * @param azimuth - 방위축 정지 여부
   * @param elevation - 고도축 정지 여부
   * @param tilt - 틸트축 정지 여부
   * @returns API 응답 데이터
   * @throws {ApiError} API 호출 실패 시
   */
  async stopCommand(azimuth: boolean, elevation: boolean, tilt: boolean): Promise<unknown> {
    try {
      const response = await api.post('/icd/stop', { azimuth, elevation, tilt })
      return response.data
    } catch (error) {
      return this.handleApiError(error, '정지 명령 전송에 실패했습니다')
    }
  }

  /**
   * Stow 명령을 전송합니다.
   * @returns API 응답 데이터
   * @throws {ApiError} API 호출 실패 시
   */
  async stowCommand(): Promise<unknown> {
    try {
      const response = await api.post('/icd/stow')
      return response.data
    } catch (error) {
      return this.handleApiError(error, 'Stow 명령 전송에 실패했습니다')
    }
  }

  /**
   * 모든 마스터 데이터를 조회합니다.
   * @returns 마스터 데이터 배열
   * @throws {ApiError} API 호출 실패 시
   */
  async fetchEphemerisMasterData(): Promise<ScheduleItem[]> {
    try {
      const response = await api.get('/satellite/ephemeris/master')

      // 개발자 도구 콘솔에 응답 데이터 출력
      console.log('[fetchEphemerisMasterData] 응답 데이터:', response)
      console.log('[fetchEphemerisMasterData] 응답 데이터 (data):', response.data)
      console.log(
        '[fetchEphemerisMasterData] 응답 데이터 형식:',
        Array.isArray(response.data) ? '배열' : typeof response.data,
      )

      if (Array.isArray(response.data)) {
        console.log('[fetchEphemerisMasterData] 데이터 개수:', response.data.length)
        if (response.data.length > 0) {
          console.log('[fetchEphemerisMasterData] 첫 번째 항목 예시:', response.data[0])
        }
      }

      return response.data || []
    } catch (error) {
      console.error('[fetchEphemerisMasterData] 오류 발생:', error)
      return this.handleApiError(error, '마스터 데이터 조회에 실패했습니다') as Promise<
        ScheduleItem[]
      >
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
      const response = await api.get<ScheduleDetailItem[]>(`/satellite/ephemeris/detail/${mstId}`)
      return response.data || []
    } catch (error) {
      return this.handleApiError(error, '세부 데이터 조회에 실패했습니다') as Promise<
        ScheduleDetailItem[]
      >
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
      await api.delete(`/satellite/ephemeris/${mstId}`)
      return true
    } catch (error) {
      return this.handleApiError(error, '데이터 삭제에 실패했습니다') as Promise<boolean>
    }
  }

  /**
   * 위성 추적을 시작합니다.
   * @param passId - 시작할 패스의 ID
   * @returns 추적 시작 결과
   * @throws {ApiError} API 호출 실패 시
   */
  async startEphemerisTracking(passId: number): Promise<unknown> {
    try {
      const response = await api.post(`/satellite/ephemeris/start/${passId}`)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위성 추적 시작에 실패했습니다')
    }
  }
}

export const ephemerisTrackService = new EphemerisTrackService()

export interface EphemerisTrackRequest {
  tleLine1: string
  tleLine2: string
  startTime: string
  endTime: string
  stepSize: number
  satelliteName?: string
}

// 이미 위에서 export interface로 내보냈으므로 중복 내보내기 제거
