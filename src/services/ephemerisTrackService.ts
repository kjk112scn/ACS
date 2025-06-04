import { api } from 'boot/axios'

// 타입 정의
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

export interface EphemerisTrackRequest {
  tleLine1: string
  tleLine2: string
  startTime: string
  endTime: string
  stepSize: number
  satelliteName?: string
}

// 기존 인터페이스들 뒤에 추가
export interface RealtimeTrackingDataItem {
  index: number
  timestamp: string
  cmdAz: number
  cmdEl: number
  elapsedTimeSeconds: number
  trackingAzimuthTime: number
  trackingCMDAzimuthAngle: number
  trackingActualAzimuthAngle: number
  trackingElevationTime: number
  trackingCMDElevationAngle: number
  trackingActualElevationAngle: number
  trackingTiltTime: number
  trackingCMDTiltAngle: number
  trackingActualTiltAngle: number
  passId: number
  azimuthError: number
  elevationError: number
}

export interface RealtimeTrackingResponse {
  message: string
  totalCount: number
  data: RealtimeTrackingDataItem[]
  statistics: Record<string, unknown>
}

// 에러 클래스들
export class TLEParseError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'TLEParseError'
    Object.setPrototypeOf(this, TLEParseError.prototype)
  }
}

export class ApiError extends Error {
  status: number
  code: string | undefined

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

/**
 * 위성 궤도 추적 관련 API 서비스 (순수 API 호출만 담당)
 */
class EphemerisTrackService {
  /**
   * TLE 텍스트 파싱
   */
  parseTLEData(tleText: string): {
    tleLine1: string
    tleLine2: string
    satelliteName: string | null
  } {
    if (!tleText || typeof tleText !== 'string') {
      throw new TLEParseError('TLE 데이터가 유효하지 않습니다')
    }

    const normalizedText = tleText.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
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
    }

    if (tleLine1 && !satelliteName) {
      const satelliteIdMatch = tleLine1.match(/^1\s+(\d+[A-Z])\s+/)
      if (satelliteIdMatch && satelliteIdMatch[1]) {
        satelliteName = satelliteIdMatch[1]
      }
    }

    return { tleLine1, tleLine2, satelliteName }
  }

  /**
   * API 에러 처리
   */
  private handleApiError(error: unknown, defaultMessage: string): never {
    let errorMessage = defaultMessage
    let statusCode = 500
    let errorCode: string | undefined

    if (error instanceof Error) {
      console.error(`API 오류: ${error.message}`, error)

      if ('isAxiosError' in error) {
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

    throw new ApiError('알 수 없는 오류가 발생했습니다', 500)
  }

  // ===== 순수 API 호출 메서드들 =====

  async generateEphemerisTrack(request: EphemerisTrackRequest): Promise<unknown> {
    try {
      if (!request.tleLine1 || !request.tleLine2) {
        throw new Error('TLE 데이터가 유효하지 않습니다')
      }
      const response = await api.post('/ephemeris/generate', request)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위성 궤도 추적 데이터 생성에 실패했습니다')
    }
  }

  async setCurrentTrackingPassId(passId: number) {
    if (typeof passId !== 'number' || isNaN(passId)) {
      throw new Error('유효하지 않은 passId 값입니다.')
    }
    try {
      const response = await api.post('/ephemeris/set-current-tracking-pass-id', null, {
        params: { passId: passId },
      })
      return response.data
    } catch (error) {
      this.handleApiError(error, '위성 추적 대상 No 설정 명령 전송에 실패했습니다.')
    }
  }

  async sendTimeOffsetCommand(timeOffset: number) {
    if (typeof timeOffset !== 'number' || isNaN(timeOffset)) {
      throw new Error('유효하지 않은 timeOffset 값입니다.')
    }
    try {
      const response = await api.post('/ephemeris/time-offset-command', null, {
        params: { inputTimeOffset: timeOffset },
      })
      return response.data
    } catch (error) {
      this.handleApiError(error, '시간 오프셋 명령 전송에 실패했습니다.')
    }
  }

  async fetchEphemerisMasterData(): Promise<ScheduleItem[]> {
    try {
      const response = await api.get('/ephemeris/master')
      return response.data || []
    } catch (error) {
      return this.handleApiError(error, '마스터 데이터 조회에 실패했습니다') as Promise<ScheduleItem[]>
    }
  }

  async fetchEphemerisDetailData(mstId: number): Promise<ScheduleDetailItem[]> {
    try {
      const response = await api.get<ScheduleDetailItem[]>(`/ephemeris/detail/${mstId}`)
      return response.data || []
    } catch (error) {
      return this.handleApiError(error, '세부 데이터 조회에 실패했습니다') as Promise<ScheduleDetailItem[]>
    }
  }

  async deleteEphemerisData(mstId: number): Promise<boolean> {
    try {
      await api.delete(`/ephemeris/${mstId}`)
      return true
    } catch (error) {
      return this.handleApiError(error, '데이터 삭제에 실패했습니다') as Promise<boolean>
    }
  }

  async startEphemerisTracking(passId: number): Promise<unknown> {
    try {
      const response = await api.post(`/ephemeris/start/${passId}`)
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위성 추적 시작에 실패했습니다')
    }
  }

  async stopEphemerisTracking(): Promise<unknown> {
    try {
      const response = await api.post('/ephemeris/tracking/stop')
      return response.data
    } catch (error) {
      return this.handleApiError(error, '위성 추적 중지에 실패했습니다')
    }
  }

  async fetchRealtimeTrackingData(): Promise<RealtimeTrackingResponse> {
    try {
      const response = await api.get<RealtimeTrackingResponse>('/ephemeris/tracking/realtime-data')
      return response.data
    } catch (error) {
      return this.handleApiError(error, '실시간 추적 데이터 조회에 실패했습니다') as Promise<RealtimeTrackingResponse>
    }
  }
}

export const ephemerisTrackService = new EphemerisTrackService()
