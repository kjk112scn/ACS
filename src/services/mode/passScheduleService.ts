import { api } from '@/boot/axios'

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
/**
 * PassSchedule 마스터 데이터 인터페이스
 *
 * EphemerisService의 ScheduleItem과 동일한 수준의 정보를 포함합니다.
 * Keyhole 정보 및 축 변환 정보를 포함합니다.
 */
export interface PassScheduleMasterData {
  No: number
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string
  MaxElevation: number
  MaxElevationTime: string
  StartAzimuth: number
  StartElevation: number
  EndAzimuth: number
  EndElevation: number
  MaxAzRate: number
  MaxElRate: number
  MaxAzAccel: number
  MaxElAccel: number
  CreationDate: string
  Creator: string
  OriginalStartAzimuth: number
  OriginalEndAzimuth: number

  // ✅ Keyhole 정보 추가
  IsKeyhole: boolean
  RecommendedTrainAngle: number

  // ✅ Original (2축) 메타데이터 추가
  OriginalMaxElevation?: number
  OriginalMaxAzRate?: number
  OriginalMaxElRate?: number

  // ✅ FinalTransformed (3축, Train=0, ±270°) 메타데이터 추가
  FinalTransformedMaxAzRate?: number
  FinalTransformedMaxElRate?: number
  FinalTransformedStartAzimuth?: number
  FinalTransformedEndAzimuth?: number
  FinalTransformedStartElevation?: number
  FinalTransformedEndElevation?: number
  FinalTransformedMaxElevation?: number

  // ✅ KeyholeAxisTransformed (3축, Train≠0) 메타데이터 추가
  KeyholeAxisTransformedMaxAzRate?: number
  KeyholeAxisTransformedMaxElRate?: number

  // ✅ KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터 추가
  KeyholeFinalTransformedMaxAzRate?: number
  KeyholeFinalTransformedMaxElRate?: number
  KeyholeFinalTransformedStartAzimuth?: number
  KeyholeFinalTransformedEndAzimuth?: number
  KeyholeFinalTransformedStartElevation?: number
  KeyholeFinalTransformedEndElevation?: number
  KeyholeFinalTransformedMaxElevation?: number
}
export interface TrackingTarget {
  no: number
  mstId: number
  satelliteId: string
  satelliteName: string
  startTime: string
  endTime: string
  maxElevation: number
}

export interface SetTrackingTargetsRequest {
  targets: TrackingTarget[]
}

export interface SetTrackingTargetsResponse {
  success: boolean
  message: string
  data?: {
    totalTargets: number
    uniqueSatellites: number
    targets: TrackingTarget[]
  }
  errors?: string[]
  timestamp: number
}

export interface GetAllTrackingMasterResponse {
  satelliteCount: number
  totalPassCount: number
  satellites: Record<string, PassScheduleMasterData[]>
}

// 추적 경로 데이터 인터페이스 추가
export interface TrackingDetailItem {
  Time: string
  Azimuth: number
  Elevation: number
  [key: string]: string | number | boolean | null | undefined
}

export interface TrackingDetailResponse {
  success: boolean
  message: string
  data?: {
    satelliteId: string
    passId: number
    trackingPointCount: number
    trackingPoints: TrackingDetailItem[]
  }
  timestamp?: number
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
   * 모든 위성의 패스 스케줄 마스터 데이터 조회 (디버깅 강화)
   */
  async getAllTrackingMasterData(): Promise<{
    success: boolean
    data?: GetAllTrackingMasterResponse
    message: string
  }> {
    try {
      console.log('📡 API 호출: /pass-schedule/tracking/master')

      const response = await api.get('/pass-schedule/tracking/master')

      // 🔍 상세 디버깅
      console.log('=== Service 상세 디버깅 ===')
      console.log('1. HTTP Status:', response.status)
      console.log('2. Raw Response:', response.data)
      console.log('3. Response Type:', typeof response.data)

      if (response.data) {
        console.log('4. Response Keys:', Object.keys(response.data))
        console.log('5. response.data.success:', response.data.success)
        console.log('6. response.data.message:', response.data.message)
        console.log('7. response.data.data:', response.data.data)

        if (response.data.data) {
          console.log('8. data.data Type:', typeof response.data.data)
          console.log('9. data.data Keys:', Object.keys(response.data.data))
          console.log('10. data.data.satelliteCount:', response.data.data.satelliteCount)
          console.log('11. data.data.totalPassCount:', response.data.data.totalPassCount)
          console.log('12. data.data.satellites:', response.data.data.satellites)
          console.log('13. satellites Type:', typeof response.data.data.satellites)

          if (response.data.data.satellites) {
            console.log('14. satellites Keys:', Object.keys(response.data.data.satellites))
          } else {
            console.log('14. satellites is null/undefined/empty')
          }
        } else {
          console.log('8. data.data is null/undefined')
        }
      }
      console.log('=== Service 디버깅 끝 ===')

      // 🔧 응답이 없는 경우
      if (!response.data) {
        console.error('❌ API 응답에 data가 없음')
        return {
          success: false,
          message: 'API 응답에 데이터가 없습니다',
        }
      }

      // 🔧 성공 응답 처리
      if (response.data.success === true) {
        console.log('✅ API 성공 응답 확인됨')

        if (response.data.data) {
          console.log('✅ response.data.data 존재 확인됨')
          return {
            success: true,
            data: response.data.data,
            message: response.data.message || '데이터 조회 완료',
          }
        } else {
          console.warn('⚠️ response.data.data가 없음')
          return {
            success: false,
            message: 'API 응답에 실제 데이터가 없습니다',
          }
        }
      } else {
        console.warn('⚠️ API 응답이 성공이 아님:', response.data.success)
        return {
          success: false,
          message: response.data.message || '서버에서 실패 응답을 받았습니다',
        }
      }
    } catch (error) {
      console.error('❌ Service API 호출 실패:', error)

      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as {
          response?: {
            status?: number
            statusText?: string
            data?: unknown
          }
          message?: string
        }

        console.error('Service Axios 에러 상세:', {
          status: axiosError.response?.status,
          statusText: axiosError.response?.statusText,
          responseData: axiosError.response?.data,
          message: axiosError.message,
        })
      }

      return {
        success: false,
        message: '서버 연결에 실패했습니다',
      }
    }
  }
  /**
   * 추적 모니터링 시작 (100ms 주기)
   */
  async startScheduleTracking(): Promise<{
    success: boolean
    message: string
    data?: {
      monitoringInterval: string
      timeReference: string
      threadName: string
      isRunning: boolean
    }
    timestamp: number
  }> {
    try {
      console.log('🚀 추적 모니터링 시작 API 호출')

      const response = await api.post('/pass-schedule/tracking/start')

      console.log('✅ 추적 모니터링 시작 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 추적 모니터링 시작 실패:', error)
      return this.handleApiError(error, '추적 모니터링 시작에 실패했습니다') as Promise<{
        success: boolean
        message: string
        data?: {
          monitoringInterval: string
          timeReference: string
          threadName: string
          isRunning: boolean
        }
        timestamp: number
      }>
    }
  }

  /**
   * 추적 모니터링 중지
   */
  async stopScheduleTracking(): Promise<{
    success: boolean
    message: string
    data?: {
      isRunning: boolean
      stoppedAt: number
      resourcesCleaned: boolean
    }
    timestamp: number
  }> {
    try {
      console.log('🛑 추적 모니터링 중지 API 호출')

      const response = await api.post('/pass-schedule/tracking/stop')

      console.log('✅ 추적 모니터링 중지 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 추적 모니터링 중지 실패:', error)
      return this.handleApiError(error, '추적 모니터링 중지에 실패했습니다') as Promise<{
        success: boolean
        message: string
        data?: {
          isRunning: boolean
          stoppedAt: number
          resourcesCleaned: boolean
        }
        timestamp: number
      }>
    }
  }
  async sendTimeOffsetCommand(timeOffset: number) {
    if (typeof timeOffset !== 'number' || isNaN(timeOffset)) {
      throw new Error('유효하지 않은 timeOffset 값입니다.')
    }
    try {
      const response = await api.post('/pass-schedule/time-offset-command', null, {
        params: { inputTimeOffset: timeOffset },
      })
      return response.data
    } catch (error) {
      this.handleApiError(error, '시간 오프셋 명령 전송에 실패했습니다.')
    }
  }
  /**
   * 추적 모니터링 상태 조회
   */
  async getTrackingMonitorStatus(): Promise<{
    success: boolean
    message: string
    data?: {
      isRunning: boolean
      monitoringInterval?: string
      timeReference?: string
      threadName?: string
      startedAt?: number
      uptime?: number
    }
    timestamp: number
  }> {
    try {
      console.log('📊 추적 모니터링 상태 조회 API 호출')

      const response = await api.get('/pass-schedule/tracking/status')

      console.log('✅ 추적 모니터링 상태 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 추적 모니터링 상태 조회 실패:', error)
      return this.handleApiError(error, '추적 모니터링 상태 조회에 실패했습니다') as Promise<{
        success: boolean
        message: string
        data?: {
          isRunning: boolean
          monitoringInterval?: string
          timeReference?: string
          threadName?: string
          startedAt?: number
          uptime?: number
        }
        timestamp: number
      }>
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

  // 추적 대상 설정 관련 타입 추가

  /**
   * 위성 추적 스케줄 대상 목록을 설정합니다
   */
  async setTrackingTargets(
    request: SetTrackingTargetsRequest,
  ): Promise<SetTrackingTargetsResponse> {
    try {
      if (!request.targets || request.targets.length === 0) {
        throw new Error('추적 대상 목록이 비어있습니다')
      }

      console.log('🚀 추적 대상 설정 API 호출:', {
        targetCount: request.targets.length,
        targets: request.targets.map((t) => ({
          mstId: t.mstId,
          satelliteId: t.satelliteId,
          satelliteName: t.satelliteName,
          startTime: t.startTime,
          endTime: t.endTime,
          maxElevation: t.maxElevation,
        })),
      })

      const response = await api.post('/pass-schedule/tracking-targets', request)

      console.log('✅ 추적 대상 설정 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 추적 대상 설정 실패:', error)
      return this.handleApiError(
        error,
        '추적 대상 설정에 실패했습니다',
      ) as Promise<SetTrackingTargetsResponse>
    }
  }

  /**
   * 전체 추적 데이터 삭제
   */
  async deleteAllTrackingData(): Promise<{
    success: boolean
    message: string
    data?: {
      deletedSatelliteCount: number
      deletedPassCount: number
      deletedTrackingPointCount: number
      remainingSatelliteCount: number
      remainingPassCount: number
      remainingTrackingPointCount: number
    }
    timestamp: number
  }> {
    try {
      console.log('🗑️ 전체 추적 데이터 삭제 API 호출')

      const response = await api.delete('/pass-schedule/tracking')

      console.log('✅ 전체 추적 데이터 삭제 응답:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 전체 추적 데이터 삭제 실패:', error)
      return this.handleApiError(error, '전체 추적 데이터 삭제에 실패했습니다') as Promise<{
        success: boolean
        message: string
        data?: {
          deletedSatelliteCount: number
          deletedPassCount: number
          deletedTrackingPointCount: number
          remainingSatelliteCount: number
          remainingPassCount: number
          remainingTrackingPointCount: number
        }
        timestamp: number
      }>
    }
  }

  /**
   * 특정 위성의 특정 패스에 대한 추적 경로 세부 데이터 조회
   */
  async fetchTrackingDetailData(
    satelliteId: string,
    passId: number,
  ): Promise<TrackingDetailResponse> {
    try {
      console.log(`🛰️ 추적 경로 세부 데이터 조회 - 위성: ${satelliteId}, 패스: ${passId}`)

      const response = await api.get<TrackingDetailResponse>(
        `/pass-schedule/tracking/detail/${satelliteId}/pass/${passId}`,
      )

      console.log('✅ 추적 경로 세부 데이터 조회 성공:', response.data)

      return response.data
    } catch (error) {
      console.error('❌ 추적 경로 세부 데이터 조회 실패:', error)

      return {
        success: false,
        message: '추적 경로 세부 데이터 조회에 실패했습니다',
        timestamp: Date.now(),
      }
    }
  }

  // ===== Pass Schedule 추적 경로 API 메서드들 =====

  /**
   * 특정 위성의 특정 패스에 대한 세부 추적 데이터 조회
   * 백엔드 API: GET /tracking/detail/{satelliteId}/pass/{passId}
   */
  async getTrackingDetailByPass(
    satelliteId: string,
    passId: number,
  ): Promise<{
    success: boolean
    message: string
    data?: {
      satelliteId: string
      passId: number
      trackingPointCount: number
      trackingPoints: TrackingDetailItem[]
    }
    timestamp?: number
  }> {
    try {
      console.log(`📡 추적 세부 데이터 조회 요청: satelliteId=${satelliteId}, passId=${passId}`)

      const response = await api.get(`/pass-schedule/tracking/detail/${satelliteId}/pass/${passId}`)

      console.log('✅ 추적 세부 데이터 응답:', {
        success: response.data.success,
        pointCount: response.data.data?.trackingPointCount,
        message: response.data.message,
      })

      return response.data
    } catch (error) {
      console.error('❌ 추적 세부 데이터 조회 실패:', error)
      return this.handleApiError(error, '추적 세부 데이터 조회에 실패했습니다')
    }
  }

  /**
   * 추적 경로 데이터를 Position View 차트용 좌표로 변환
   */
  convertToChartData(trackingPoints: TrackingDetailItem[]): [number, number][] {
    try {
      if (!Array.isArray(trackingPoints) || trackingPoints.length === 0) {
        console.warn('⚠️ 변환할 추적 포인트가 없음')
        return []
      }

      const chartData: [number, number][] = trackingPoints
        .filter((point) => {
          // 유효한 데이터만 필터링
          return (
            point.Azimuth !== null &&
            point.Azimuth !== undefined &&
            point.Elevation !== null &&
            point.Elevation !== undefined &&
            !isNaN(Number(point.Azimuth)) &&
            !isNaN(Number(point.Elevation))
          )
        })
        .map((point) => {
          // [elevation, azimuth] 순서로 변환 (polar 차트 좌표계)
          const elevation = Math.max(0, Math.min(90, Number(point.Elevation)))
          const azimuth =
            Number(point.Azimuth) < 0 ? Number(point.Azimuth) + 360 : Number(point.Azimuth)
          return [elevation, azimuth] as [number, number]
        })

      console.log(`✅ 차트 데이터 변환 완료: ${chartData.length}개 포인트`)

      // 샘플링 (성능 최적화)
      if (chartData.length > 200) {
        const step = Math.ceil(chartData.length / 200)
        const sampledData = chartData.filter((_, index) => index % step === 0)
        console.log(`📊 데이터 샘플링: ${chartData.length} → ${sampledData.length}개 포인트`)
        return sampledData
      }

      return chartData
    } catch (error) {
      console.error('❌ 차트 데이터 변환 실패:', error)
      return []
    }
  }
}

export const passScheduleService = new PassScheduleService()
