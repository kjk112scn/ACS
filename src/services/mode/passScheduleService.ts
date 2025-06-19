import { api } from 'boot/axios'

// 타입 정의 (기존 타입들과 함께)
export interface TLEItem {
  satelliteId: string
  satelliteName?: string
  tleLine1: string
  tleLine2: string
}

export interface AddTLERequest {
  satelliteId?: string
  tleLine1: string
  tleLine2: string
}

export interface UpdateTLERequest {
  tleLine1: string
  tleLine2: string
}

export interface TLEResponse {
  success: boolean
  message: string
  data?: {
    satelliteId?: string
    tleLine1?: string
    tleLine2?: string
    added?: boolean
    deleted?: boolean
    isUpdate?: boolean
    operation?: string
    satelliteIdSource?: string
  }
  timestamp: number
}

export interface AllTLEResponse {
  success: boolean
  message: string
  data: {
    totalCount: number
    tleList: TLEItem[]
  }
  timestamp: number
}

export interface CacheStatusResponse {
  success: boolean
  message: string
  data: {
    totalCount: number
    satelliteIds: string[]
    isEmpty: boolean
    cacheInfo: {
      type: string
      description: string
    }
  }
  timestamp: number
}

export interface AddTleAndTrackingRequest {
  satelliteId?: string
  satelliteName?: string
  tleLine1: string
  tleLine2: string
}

export interface PassInfo {
  passId: string
  startTime: string
  endTime: string
  duration: string
  maxElevation: number
}

export interface TleAndTrackingResponse {
  success: boolean
  message: string
  data?: {
    satelliteId: string
    satelliteName: string
    tleLine1: string
    tleLine2: string
    passCount: number
    trackingPointCount: number
    satelliteIdSource: string
    passes: PassInfo[]
  }
  timestamp: number
}

// 🆕 패스 스케줄 관련 타입들 추가
export interface PassScheduleMasterData {
  id: string
  satelliteId: string
  satelliteName: string
  passNumber: number
  startTime: string
  endTime: string
  maxElevation: number
  duration: number
  status: string
  azimuthStart: number
  azimuthEnd: number
  elevationStart: number
  elevationEnd: number
}

export interface GetAllTrackingMasterResponse {
  satelliteCount: number
  totalPassCount: number
  satellites: Record<string, PassScheduleMasterData[]>
}

// 에러 클래스들
export class TLEApiError extends Error {
  status: number
  code: string | undefined

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'TLEApiError'
    this.status = status
    this.code = code
    Object.setPrototypeOf(this, TLEApiError.prototype)
  }
}

/**
 * Pass Schedule TLE 관리 API 서비스
 */
class PassScheduleService {
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

      throw new TLEApiError(errorMessage, statusCode, errorCode)
    }

    throw new TLEApiError('알 수 없는 오류가 발생했습니다', 500)
  }

  // ===== TLE 관리 API 메서드들 =====

  /**
   * TLE 데이터 추가
   */
  async addTLE(request: AddTLERequest): Promise<TLEResponse> {
    try {
      if (!request.tleLine1 || !request.tleLine2) {
        throw new Error('TLE Line1과 Line2는 필수입니다')
      }
      const response = await api.post('/pass-schedule/tle', request)
      return response.data
    } catch (error) {
      return this.handleApiError(error, 'TLE 데이터 추가에 실패했습니다')
    }
  }

  /**
   * 특정 위성의 TLE 데이터 조회
   */
  async getTLE(satelliteId: string): Promise<TLEResponse> {
    try {
      if (!satelliteId) {
        throw new Error('위성 ID가 필요합니다')
      }
      const response = await api.get(`/pass-schedule/tle/${satelliteId}`)
      return response.data
    } catch (error) {
      return this.handleApiError(error, 'TLE 데이터 조회에 실패했습니다')
    }
  }

  /**
   * 전체 TLE 데이터 조회
   */
  async getAllTLEs(): Promise<AllTLEResponse> {
    try {
      const response = await api.get('/pass-schedule/tle')
      return response.data
    } catch (error) {
      return this.handleApiError(
        error,
        '전체 TLE 데이터 조회에 실패했습니다',
      ) as Promise<AllTLEResponse>
    }
  }

  /**
   * 특정 위성의 TLE 데이터 삭제
   */
  async deleteTLE(satelliteId: string): Promise<TLEResponse> {
    try {
      if (!satelliteId) {
        throw new Error('위성 ID가 필요합니다')
      }
      const response = await api.delete(`/pass-schedule/tle/${satelliteId}`)
      return response.data
    } catch (error) {
      return this.handleApiError(error, 'TLE 데이터 삭제에 실패했습니다')
    }
  }

  /**
   * 전체 TLE 데이터 삭제
   */
  async deleteAllTLEs(): Promise<TLEResponse> {
    try {
      const response = await api.delete('/pass-schedule/tle')
      return response.data
    } catch (error) {
      return this.handleApiError(error, '전체 TLE 데이터 삭제에 실패했습니다')
    }
  }

  /**
   * TLE 데이터 업데이트
   */
  async updateTLE(satelliteId: string, request: UpdateTLERequest): Promise<TLEResponse> {
    try {
      if (!satelliteId) {
        throw new Error('위성 ID가 필요합니다')
      }
      if (!request.tleLine1 || !request.tleLine2) {
        throw new Error('TLE Line1과 Line2는 필수입니다')
      }
      const response = await api.put(`/pass-schedule/tle/${satelliteId}`, request)
      return response.data
    } catch (error) {
      return this.handleApiError(error, 'TLE 데이터 업데이트에 실패했습니다')
    }
  }

  /**
   * TLE 캐시 상태 조회
   */
  async getCacheStatus(): Promise<CacheStatusResponse> {
    try {
      const response = await api.get('/pass-schedule/status')
      return response.data
    } catch (error) {
      return this.handleApiError(
        error,
        'TLE 캐시 상태 조회에 실패했습니다',
      ) as Promise<CacheStatusResponse>
    }
  }

  /**
   * TLE 텍스트 파싱 (순서 보장 및 위성명 처리 개선)
   */
  parseTLEText(tleText: string): TLEItem[] {
    if (!tleText || typeof tleText !== 'string') {
      throw new Error('TLE 데이터가 유효하지 않습니다')
    }

    const normalizedText = tleText.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

    const lines = normalizedText
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)

    console.log('🔍 Service 파싱 - 라인 수:', lines.length)

    const tleItems: TLEItem[] = []
    let i = 0

    while (i < lines.length) {
      // 3줄 형식 우선 처리 (위성명 + TLE Line1 + TLE Line2)
      if (
        i + 2 < lines.length &&
        !lines[i]?.startsWith('1 ') &&
        !lines[i]?.startsWith('2 ') &&
        lines[i + 1]?.startsWith('1 ') &&
        lines[i + 2]?.startsWith('2 ')
      ) {
        const satelliteName = lines[i]?.trim() || ''
        const tleLine1 = lines[i + 1]?.trim() || ''
        const tleLine2 = lines[i + 2]?.trim() || ''

        const satelliteId = tleLine1.substring(2, 7).trim()

        console.log(`✅ Service 3줄 형식: "${satelliteName}" (ID: ${satelliteId})`)

        const tleItem: TLEItem = {
          satelliteId: satelliteId,
          tleLine1,
          tleLine2,
        }

        if (satelliteName && satelliteName.length > 0) {
          tleItem.satelliteName = satelliteName
        }

        tleItems.push(tleItem)
        i += 3
      }
      // 2줄 형식 처리 (TLE Line1 + TLE Line2)
      else if (
        i + 1 < lines.length &&
        lines[i]?.startsWith('1 ') &&
        lines[i + 1]?.startsWith('2 ')
      ) {
        const tleLine1 = lines[i]?.trim() || ''
        const tleLine2 = lines[i + 1]?.trim() || ''

        const satelliteId = tleLine1.substring(2, 7).trim()

        console.log(`✅ Service 2줄 형식: ID ${satelliteId}`)

        tleItems.push({
          satelliteId,
          tleLine1,
          tleLine2,
        })

        i += 2
      } else {
        console.log(`⚠️ Service 건너뛴 라인: "${lines[i]}"`)
        i++
      }
    }

    console.log(`🎯 Service 파싱 완료: ${tleItems.length}개`)
    return tleItems
  }

  /**
   * TLE 데이터를 텍스트로 변환
   */
  convertTLEsToText(tleItems: TLEItem[]): string {
    return tleItems
      .map((item) => {
        return `${item.satelliteId}\n${item.tleLine1}\n${item.tleLine2}`
      })
      .join('\n\n')
  }

  /**
   * TLE 형식 검증
   */
  validateTLE(tleLine1: string, tleLine2: string): boolean {
    // 기본 길이 검증
    if (tleLine1.length !== 69 || tleLine2.length !== 69) {
      return false
    }

    // Line 1 검증
    if (!tleLine1.startsWith('1 ')) {
      return false
    }

    // Line 2 검증
    if (!tleLine2.startsWith('2 ')) {
      return false
    }

    // 위성 번호 일치 검증
    const satNum1 = tleLine1.substring(2, 7).trim()
    const satNum2 = tleLine2.substring(2, 7).trim()

    if (satNum1 !== satNum2) {
      return false
    }

    return true
  }

  /**
   * 파일에서 TLE 데이터 읽기
   */
  async readTLEFromFile(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()

      reader.onload = (event) => {
        const content = event.target?.result as string
        resolve(content)
      }

      reader.onerror = () => {
        reject(new Error('파일 읽기에 실패했습니다'))
      }

      reader.readAsText(file)
    })
  }

  /**
   * TLE 데이터를 파일로 다운로드
   */
  downloadTLEAsFile(content: string, filename: string = 'tle_data.txt'): void {
    const blob = new Blob([content], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    URL.revokeObjectURL(url)
  }

  /**
   * TLE 데이터 추가와 동시에 추적 데이터를 생성합니다 (원스톱 API)
   */
  async addTleAndGenerateTracking(
    request: AddTleAndTrackingRequest,
  ): Promise<TleAndTrackingResponse> {
    try {
      if (!request.tleLine1 || !request.tleLine2) {
        throw new Error('TLE Line1과 Line2는 필수입니다')
      }

      console.log('🚀 TLE 추가 및 추적 데이터 생성 API 호출:', {
        satelliteId: request.satelliteId,
        tleLine1Length: request.tleLine1.length,
        tleLine2Length: request.tleLine2.length,
      })

      const response = await api.post('/pass-schedule/tle-and-tracking', request, {
        timeout: 600000, // 10분 타임아웃
      })

      console.log('✅ TLE 추가 및 추적 데이터 생성 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ TLE 추가 및 추적 데이터 생성 실패:', error)
      return this.handleApiError(
        error,
        'TLE 데이터 추가 및 추적 데이터 생성에 실패했습니다',
      ) as Promise<TleAndTrackingResponse>
    }
  }

  /**
   * 모든 위성의 패스 스케줄 마스터 데이터 조회
   */
  async getAllTrackingMasterData(): Promise<{
    success: boolean
    data?: GetAllTrackingMasterResponse
    message: string
  }> {
    try {
      console.log('📡 모든 패스 스케줄 마스터 데이터 조회')

      const response = await api.get('/pass-schedule/tracking/master')

      return {
        success: true,
        data: response.data.data,
        message: response.data.message || '패스 스케줄 마스터 데이터 조회 완료',
      }
    } catch (error) {
      console.error('❌ 패스 스케줄 마스터 데이터 조회 실패:', error)
      return {
        success: false,
        message: '패스 스케줄 마스터 데이터 조회에 실패했습니다',
      }
    }
  }

  /**
   * 특정 위성의 패스 스케줄 마스터 데이터 조회
   */
  async getTrackingMasterDataBySatellite(satelliteId: string): Promise<{
    success: boolean
    data?: PassScheduleMasterData[]
    message: string
  }> {
    try {
      console.log('🛰️ 위성별 패스 스케줄 마스터 데이터 조회:', satelliteId)

      const response = await api.get(`/pass-schedule/tracking/master/${satelliteId}`)

      return {
        success: true,
        data: response.data.data,
        message: '위성별 패스 스케줄 마스터 데이터 조회 완료',
      }
    } catch (error) {
      console.error('❌ 위성별 패스 스케줄 마스터 데이터 조회 실패:', error)
      return {
        success: false,
        message: '위성별 패스 스케줄 마스터 데이터 조회에 실패했습니다',
      }
    }
  }
}

export const passScheduleService = new PassScheduleService()
