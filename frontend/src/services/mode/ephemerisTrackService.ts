import { api } from '@/boot/axios'

// 설정 타입 정의
interface SettingItem {
  key: string
  value: string
  type: string
  description?: string
}

// ✅ 비교 데이터 타입 정의
export interface ComparisonScheduleItem {
  No: number
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string

  // ✅ Original 데이터 (2축)
  OriginalMaxElevation: number
  OriginalMaxAzRate: number
  OriginalMaxElRate: number

  // ✅ Final Transformed 데이터
  FinalMaxElevation: number
  FinalMaxAzRate: number
  FinalMaxElRate: number

  // ✅ KEYHOLE 정보 (Final 데이터 기준)
  IsKeyhole: boolean
  RecommendedTrainAngle: number

  CreationDate: string
  Creator: string

  [key: string]: string | number | boolean | null | undefined
}

// 타입 정의
export interface ScheduleItem {
  No: number
  // ✅ mstId와 detailId 필드 추가 (PassSchedule과 동일한 구조)
  mstId?: number // 전역 고유 마스터 ID
  detailId?: number // 패스 인덱스
  SatelliteID: string
  SatelliteName: string
  StartTime: string
  EndTime: string
  Duration: string
  MaxElevation: number
  CreationDate: string
  Creator: string

  /**
   * KEYHOLE 위성 여부
   * maxAzimuthRate가 임계값 이상인 경우 true
   */
  isKeyhole: boolean
  IsKeyhole?: boolean // ✅ 백엔드 응답 호환성

  /**
   * KEYHOLE 위성일 경우 권장 Train 각도 (도)
   * 최대 Elevation 지점의 Azimuth 각도
   */
  recommendedTrainAngle: number
  RecommendedTrainAngle?: number // ✅ 백엔드 응답 호환성

  // ✅ 시작/종료 각도 (백엔드 응답 호환성)
  StartAzimuth?: number
  EndAzimuth?: number
  StartElevation?: number
  EndElevation?: number

  // ✅ FinalTransformed 시작/종료 각도 및 최대 고도 (Train=0, ±270°)
  FinalTransformedStartAzimuth?: number
  FinalTransformedEndAzimuth?: number
  FinalTransformedStartElevation?: number
  FinalTransformedEndElevation?: number
  FinalTransformedMaxElevation?: number

  // ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도 (Train≠0, ±270°, Keyhole일 경우만)
  KeyholeFinalTransformedStartAzimuth?: number
  KeyholeFinalTransformedEndAzimuth?: number
  KeyholeFinalTransformedStartElevation?: number
  KeyholeFinalTransformedEndElevation?: number
  KeyholeFinalTransformedMaxElevation?: number

  /**
   * ✅ FinalTransformed 최대 Azimuth 각속도 (도/초) - 합계법
   */
  FinalTransformedMaxAzRate: number

  /**
   * ✅ FinalTransformed 최대 Elevation 각속도 (도/초) - 합계법
   */
  FinalTransformedMaxElRate: number

  /**
   * ✅ KeyholeAxisTransformed 최대 Azimuth 각속도 (도/초) - 합계법
   * Keyhole 발생 시 Train≠0, 각도 제한 ❌
   */
  KeyholeAxisTransformedMaxAzRate?: number

  /**
   * ✅ KeyholeAxisTransformed 최대 Elevation 각속도 (도/초) - 합계법
   * Keyhole 발생 시 Train≠0, 각도 제한 ❌
   */
  KeyholeAxisTransformedMaxElRate?: number

  /**
   * ✅ KeyholeFinalTransformed 최대 Azimuth 각속도 (도/초) - 합계법
   * Keyhole 발생 시 Train≠0, 각도 제한 ✅
   */
  KeyholeFinalTransformedMaxAzRate?: number

  /**
   * ✅ KeyholeFinalTransformed 최대 Elevation 각속도 (도/초) - 합계법
   * Keyhole 발생 시 Train≠0, 각도 제한 ✅
   */
  KeyholeFinalTransformedMaxElRate?: number

  /**
   * ✅ 2축(Original) 최대 고도 (도)
   */
  OriginalMaxElevation?: number

  /**
   * ✅ 2축(Original) 최대 Azimuth 각속도 (도/초)
   */
  OriginalMaxAzRate?: number

  /**
   * ✅ 2축(Original) 최대 Elevation 각속도 (도/초)
   */
  OriginalMaxElRate?: number

  /**
   * ✅ 중앙차분법 최대 Azimuth 각속도 (도/초)
   * 실시간 제어용 - 주석 처리됨
   */
  CentralDiffMaxAzRate?: number

  /**
   * ✅ 중앙차분법 최대 Elevation 각속도 (도/초)
   * 실시간 제어용 - 주석 처리됨
   */
  CentralDiffMaxElRate?: number

  /**
   * ✅ 방법 2 (신규): 하이브리드 3단계 그리드 서치로 계산된 Train 각도
   */
  KeyholeOptimizedRecommendedTrainAngle?: number

  /**
   * ✅ 방법 2 (신규): 최적화된 최대 Azimuth 각속도 (도/초)
   */
  KeyholeOptimizedFinalTransformedMaxAzRate?: number

  /**
   * ✅ 방법 2 (신규): 최적화된 최대 Elevation 각속도 (도/초)
   */
  KeyholeOptimizedFinalTransformedMaxElRate?: number

  /**
   * ✅ 비교 결과: 개선량 (도/초)
   * 방법 1 - 방법 2
   */
  OptimizationImprovement?: number

  /**
   * ✅ 비교 결과: 개선율 (%)
   * (개선량 / 방법 1) * 100
   */
  OptimizationImprovementRate?: number

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
  theoreticalIndex?: number // ✅ 이론치 데이터 인덱스 추가
  timestamp: string
  passId: number
  elapsedTimeSeconds: number

  // 원본 데이터 (변환 전)
  originalAzimuth?: number
  originalElevation?: number
  originalRange?: number
  originalAltitude?: number

  // 축변환 데이터 (기울기 변환 적용)
  axisTransformedAzimuth?: number
  axisTransformedElevation?: number
  axisTransformedRange?: number
  axisTransformedAltitude?: number

  // 최종 변환 데이터 (±270도 제한 적용, Train=0)
  finalTransformedAzimuth?: number
  finalTransformedElevation?: number
  finalTransformedRange?: number
  finalTransformedAltitude?: number

  // Keyhole Final 변환 데이터 (±270도 제한 적용, Train≠0) [Keyhole 발생 시만]
  keyholeFinalTransformedAzimuth?: number
  keyholeFinalTransformedElevation?: number
  keyholeFinalTransformedRange?: number
  keyholeFinalTransformedAltitude?: number

  // 명령 및 실제 추적 데이터
  cmdAz: number
  cmdEl: number
  actualAz?: number
  actualEl?: number
  trackingAzimuthTime: number
  trackingCMDAzimuthAngle: number
  trackingActualAzimuthAngle: number
  trackingElevationTime: number
  trackingCMDElevationAngle: number
  trackingActualElevationAngle: number
  trackingTrainTime: number
  trackingCMDTrainAngle: number
  trackingActualTrainAngle: number

  // 오차 분석
  azimuthError: number
  elevationError: number
  originalToAxisTransformationError?: number
  axisToFinalTransformationError?: number
  totalTransformationError?: number

  // 정확도 분석 (새로 추가된 필드들)
  timeAccuracy?: number
  azCmdAccuracy?: number
  azActAccuracy?: number
  azFinalAccuracy?: number
  elCmdAccuracy?: number
  elActAccuracy?: number
  elFinalAccuracy?: number

  // 변환 정보
  trainAngle?: number
  transformationType?: string
  hasTransformation?: boolean
  interpolationMethod?: string
  interpolationAccuracy?: number
  isKeyhole?: boolean
  finalDataType?: string

  // 데이터 유효성 및 소스
  hasValidData?: boolean
  dataSource?: string
}

export interface RealtimeTrackingResponse {
  message: string
  totalCount: number
  data: RealtimeTrackingDataItem[]
  statistics: Record<string, unknown>
}

export interface GeostationaryTrackingRequest {
  tleLine1: string
  tleLine2: string
}

export interface GeostationaryTrackingResponse {
  message: string
  satelliteId: string
  trackingType: string
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

      console.log('🚀 위성 궤도 추적 데이터 생성 API 호출:', {
        satelliteName: request.satelliteName,
        tleLine1Length: request.tleLine1.length,
        tleLine2Length: request.tleLine2.length,
        startTime: request.startTime,
        endTime: request.endTime,
        stepSize: request.stepSize,
      })

      const response = await api.post('/ephemeris/tracking/generate', request, {
        timeout: 300000, // 5분 타임아웃 (기본값)
      })

      console.log('✅ 위성 궤도 추적 데이터 생성 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('❌ 위성 궤도 추적 데이터 생성 실패:', error)
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
      // ✅ 최적화 데이터를 포함한 병합 API 사용
      console.log('🔍 API 호출 시작: /ephemeris/tracking/mst/merged')
      const response = await api.get('/ephemeris/tracking/mst/merged')
      console.log('✅ API 응답 받음:', response.status, response.data?.data?.length || 0, '개')

      // ✅ 병합 API 응답 구조: { status: 'success', data: [...] } (PassSchedule과 동일)
      let mergedData: Record<string, unknown>[] = []

      if (response.data?.status === 'success' && Array.isArray(response.data.data)) {
        // ✅ 정상 응답: { status: 'success', data: [...] }
        mergedData = response.data.data as Record<string, unknown>[]
      } else if (Array.isArray(response.data)) {
        // ✅ 배열 직접 응답: [...]
        mergedData = response.data as Record<string, unknown>[]
      } else if (response.data?.data && Array.isArray(response.data.data)) {
        // ✅ data 필드에 배열이 있는 경우
        mergedData = response.data.data as Record<string, unknown>[]
      } else {
        console.warn('⚠️ 응답 데이터가 배열이 아님:', typeof response.data, response.data)
        mergedData = []
      }

      // 백엔드가 병합 데이터를 반환하므로 매핑 처리
      if (Array.isArray(mergedData) && mergedData.length > 0) {
        const mappedData = mergedData.map((item: Record<string, unknown>) => ({
          // ✅ 백엔드에서 제공하는 순차 번호 그대로 사용 (row-key용)
          No: item.No as number,
          // ✅ mstId와 detailId 매핑 추가 (PassSchedule과 동일한 구조)
          // ✅ No 필드 제거, MstId만 사용
          mstId: item.MstId as number,
          // Ephemeris는 일반적으로 detailId가 0이지만, 백엔드에서 제공하는 경우 사용
          detailId: (item.DetailId ?? 0) as number,
          SatelliteID: item.SatelliteID as string,
          SatelliteName: item.SatelliteName as string,
          StartTime: item.StartTime as string,
          EndTime: item.EndTime as string,
          Duration: item.Duration as string,
          MaxElevation: item.MaxElevation as number,

          // ✅ FinalTransformed 속도 (풀네임)
          FinalTransformedMaxAzRate: item.FinalTransformedMaxAzRate as number,
          FinalTransformedMaxElRate: item.FinalTransformedMaxElRate as number,

          isKeyhole: (item.IsKeyhole ?? item.isKeyhole) as boolean,
          IsKeyhole: item.IsKeyhole as boolean | undefined,
          recommendedTrainAngle: (item.RecommendedTrainAngle ??
            item.recommendedTrainAngle) as number,
          RecommendedTrainAngle: item.RecommendedTrainAngle as number | undefined,

          // ✅ 시작/종료 각도 (하위 호환성)
          StartAzimuth: item.StartAzimuth as number | undefined,
          EndAzimuth: item.EndAzimuth as number | undefined,
          StartElevation: item.StartElevation as number | undefined,
          EndElevation: item.EndElevation as number | undefined,

          // ✅ FinalTransformed 시작/종료 각도 및 최대 고도
          FinalTransformedStartAzimuth: item.FinalTransformedStartAzimuth as number | undefined,
          FinalTransformedEndAzimuth: item.FinalTransformedEndAzimuth as number | undefined,
          FinalTransformedStartElevation: item.FinalTransformedStartElevation as number | undefined,
          FinalTransformedEndElevation: item.FinalTransformedEndElevation as number | undefined,
          FinalTransformedMaxElevation: item.FinalTransformedMaxElevation as number | undefined,

          // ✅ KeyholeFinalTransformed 시작/종료 각도 및 최대 고도
          KeyholeFinalTransformedStartAzimuth: item.KeyholeFinalTransformedStartAzimuth as
            | number
            | undefined,
          KeyholeFinalTransformedEndAzimuth: item.KeyholeFinalTransformedEndAzimuth as
            | number
            | undefined,
          KeyholeFinalTransformedStartElevation: item.KeyholeFinalTransformedStartElevation as
            | number
            | undefined,
          KeyholeFinalTransformedEndElevation: item.KeyholeFinalTransformedEndElevation as
            | number
            | undefined,
          KeyholeFinalTransformedMaxElevation: item.KeyholeFinalTransformedMaxElevation as
            | number
            | undefined,

          CreationDate: item.CreationDate as string,
          Creator: item.Creator as string,

          // Original (2축) 메타데이터
          OriginalMaxElevation: item.OriginalMaxElevation as number | undefined,
          OriginalMaxAzRate: item.OriginalMaxAzRate as number | undefined,
          OriginalMaxElRate: item.OriginalMaxElRate as number | undefined,

          // ✅ 중앙차분법 데이터 (실시간 제어용 - 주석 처리)
          CentralDiffMaxAzRate: item.CentralDiffMaxAzRate as number | undefined,
          CentralDiffMaxElRate: item.CentralDiffMaxElRate as number | undefined,

          // ✅ Keyhole 관련 속도 데이터
          KeyholeAxisTransformedMaxAzRate: item.KeyholeAxisTransformedMaxAzRate as
            | number
            | undefined,
          KeyholeAxisTransformedMaxElRate: item.KeyholeAxisTransformedMaxElRate as
            | number
            | undefined,
          KeyholeFinalTransformedMaxAzRate: item.KeyholeFinalTransformedMaxAzRate as
            | number
            | undefined,
          KeyholeFinalTransformedMaxElRate: item.KeyholeFinalTransformedMaxElRate as
            | number
            | undefined,

          // ✅ 방법 2 (신규): 최적화 데이터 추가
          KeyholeOptimizedRecommendedTrainAngle: item.KeyholeOptimizedRecommendedTrainAngle as
            | number
            | undefined,
          KeyholeOptimizedFinalTransformedMaxAzRate:
            item.KeyholeOptimizedFinalTransformedMaxAzRate as number | undefined,
          KeyholeOptimizedFinalTransformedMaxElRate:
            item.KeyholeOptimizedFinalTransformedMaxElRate as number | undefined,
          OptimizationImprovement: item.OptimizationImprovement as number | undefined,
          OptimizationImprovementRate: item.OptimizationImprovementRate as number | undefined,
        }))

        console.log(
          '📊 매핑된 데이터:',
          mappedData.length,
          '개, Original 데이터 포함:',
          mappedData[0]?.OriginalMaxElevation !== undefined,
        )

        // 🔍 디버깅: Keyhole이 있는 항목의 최적화 데이터 확인
        mappedData.forEach((item, index) => {
          if (item.isKeyhole) {
            console.log(
              `🔍 [프론트엔드] fetchEphemerisMasterData - Schedule #${index + 1} (MST ID: ${item.No}):`,
            )
            console.log(`   - isKeyhole: ${item.isKeyhole}`)
            console.log(
              `   - KeyholeOptimizedRecommendedTrainAngle:`,
              item.KeyholeOptimizedRecommendedTrainAngle,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxAzRate:`,
              item.KeyholeOptimizedFinalTransformedMaxAzRate,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxElRate:`,
              item.KeyholeOptimizedFinalTransformedMaxElRate,
            )
            console.log(`   - OptimizationImprovement:`, item.OptimizationImprovement)
            console.log(`   - OptimizationImprovementRate:`, item.OptimizationImprovementRate)
          }
        })

        // 첫 번째 데이터의 속도 값 확인
        if (mappedData.length > 0) {
          console.log('🔍 첫 번째 데이터 상세:')
          console.log(
            '  - FinalTransformedMaxAzRate (합계법):',
            mappedData[0].FinalTransformedMaxAzRate,
          )
          console.log(
            '  - FinalTransformedMaxElRate (합계법):',
            mappedData[0].FinalTransformedMaxElRate,
          )
          console.log('  - OriginalMaxAzRate (합계법 - 2축):', mappedData[0].OriginalMaxAzRate)
          console.log('  - OriginalMaxElRate (합계법 - 2축):', mappedData[0].OriginalMaxElRate)
          console.log(
            '  - CentralDiffMaxAzRate (중앙차분법 - 실시간 제어용):',
            mappedData[0].CentralDiffMaxAzRate,
          )
          console.log(
            '  - CentralDiffMaxElRate (중앙차분법 - 실시간 제어용):',
            mappedData[0].CentralDiffMaxElRate,
          )
        }
        return mappedData
      }

      // ✅ mergedData가 비어있거나 매핑되지 않은 경우 빈 배열 반환 (타입 명시)
      console.log(
        '⚠️ 응답 데이터가 배열이 아님 또는 비어있음:',
        typeof mergedData,
        mergedData.length,
      )
      return [] as ScheduleItem[]
    } catch (error) {
      console.error('❌ API 호출 실패:', error)
      console.error('❌ 요청 URL:', '/ephemeris/tracking/mst/merged')
      console.error('❌ 에러 상세:', error.response?.status, error.response?.statusText)
      return this.handleApiError(error, '마스터 데이터 조회에 실패했습니다') as Promise<
        ScheduleItem[]
      >
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

  /**
   * 위성 추적 시작
   * ✅ mstId와 detailId를 사용하여 추적 시작 (PassSchedule과 동일한 구조)
   *
   * @param mstId 추적할 마스터 ID
   * @param detailId 패스 인덱스 (기본값: 0)
   * @returns 추적 시작 응답
   */
  async startEphemerisTracking(mstId: number, detailId: number = 0): Promise<unknown> {
    try {
      const response = await api.post(`/ephemeris/tracking/start/${mstId}/pass/${detailId}`)
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
      return this.handleApiError(
        error,
        '실시간 추적 데이터 조회에 실패했습니다',
      ) as Promise<RealtimeTrackingResponse>
    }
  }

  async startGeostationaryTracking(
    request: GeostationaryTrackingRequest,
  ): Promise<GeostationaryTrackingResponse> {
    try {
      const response = await api.post<GeostationaryTrackingResponse>(
        '/ephemeris/3axis/tracking/geostationary/start',
        request,
      )
      return response.data
    } catch (error) {
      return this.handleApiError(
        error,
        '정지궤도 위성 추적 시작에 실패했습니다',
      ) as Promise<GeostationaryTrackingResponse>
    }
  }

  async calculateGeostationaryAngles(request: GeostationaryTrackingRequest): Promise<{
    message: string
    satelliteId: string
    azimuth: number
    elevation: number
    originalAzimuth: number
    originalElevation: number
    tiltAngle: number
    rotatorAngle: number
    trackingType: string
  }> {
    try {
      const response = await api.post(
        '/ephemeris/3axis/tracking/geostationary/calculate-angles',
        request,
      )
      return response.data
    } catch (error) {
      return this.handleApiError(error, '정지궤도 각도 계산에 실패했습니다') as Promise<{
        message: string
        satelliteId: string
        azimuth: number
        elevation: number
        originalAzimuth: number
        originalElevation: number
        tiltAngle: number
        rotatorAngle: number
        trackingType: string
      }>
    }
  }

  /**
   * 3축 변환 계산 API
   */
  async calculateAxisTransform(params: {
    azimuth: number
    elevation: number
    tilt: number
    train: number
  }): Promise<{
    success: boolean
    input: { azimuth: number; elevation: number; tilt: number; rotator: number }
    output: { azimuth: number; elevation: number }
    message?: string
    error?: string
  }> {
    try {
      const response = await api.post('/ephemeris/calculate-axis-transform', params)
      return response.data
    } catch (error) {
      console.error('3축 변환 계산 API 호출 실패:', error)
      throw error
    }
  }

  /**
   * 모든 MST 데이터를 CSV 파일로 내보내기
   */
  async exportAllMstDataToCsv(outputDirectory: string = 'csv_exports'): Promise<{
    success: boolean
    message: string
    totalMstCount?: number
    successCount?: number
    errorCount?: number
    createdFiles?: string[]
    outputDirectory?: string
    error?: string
  }> {
    try {
      const response = await api.post('/ephemeris/export/csv/all', null, {
        params: { outputDirectory },
      })
      return response.data
    } catch (error) {
      console.error('CSV 내보내기 API 호출 실패:', error)
      throw error
    }
  }

  /**
   * 모든 MST 데이터를 하나의 통합된 CSV 파일로 내보내기
   * 사용자 요구사항: 하나의 파일로 모든 데이터 통합
   */
  async exportAllMstDataToSingleCsv(outputDirectory: string = 'csv_exports'): Promise<{
    success: boolean
    message: string
    filename?: string
    filePath?: string
    totalMstCount?: number
    processedMstCount?: number
    totalRows?: number
    outputDirectory?: string
    error?: string
  }> {
    try {
      const response = await api.post('/ephemeris/export/csv/all', null, {
        params: { outputDirectory },
      })
      return response.data
    } catch (error) {
      console.error('통합 CSV 내보내기 API 호출 실패:', error)
      throw error
    }
  }

  /**
   * 특정 MST 데이터를 CSV 파일로 브라우저에 직접 다운로드
   * 선택된 스케줄의 MST ID만 처리하여 빠른 응답
   */
  async downloadMstDataToCsv(mstId: number, detailId?: number): Promise<void> {
    try {
      const params: { mstId: number; detailId?: number } = { mstId }
      if (detailId !== undefined) {
        params.detailId = detailId
      }

      const response = await api.get('/ephemeris/export/csv/download', {
        params,
        responseType: 'blob',
      })

      // Content-Disposition 헤더에서 파일명 추출
      const contentDisposition = response.headers['content-disposition']
      let filename = `MST_${mstId}_Data.csv`
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?([^";\n]+)"?/)
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1]
        }
      }

      // Blob을 다운로드 링크로 변환
      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('CSV 다운로드 실패:', error)
      throw error
    }
  }
  async exportMstDataToCsv(
    mstId: number,
    outputDirectory: string = 'csv_exports',
  ): Promise<{
    success: boolean
    message: string
    filename?: string
    filePath?: string
    satelliteName?: string
    originalDataCount?: number
    axisTransformedDataCount?: number
    finalTransformedDataCount?: number
    error?: string
  }> {
    try {
      const response = await api.post(`/ephemeris/export/csv/${mstId}`, null, {
        params: { outputDirectory },
      })
      return response.data
    } catch (error) {
      console.error('특정 MST CSV 내보내기 API 호출 실패:', error)
      throw error
    }
  }

  /**
   * ✅ 실시간 추적 데이터를 가져오기 (원본/축변환/최종 데이터 포함)
   */
  async fetchRealtimeTrackingDataWithTransformations(): Promise<{
    success: boolean
    message: string
    data: Array<{
      index: number
      theoreticalIndex?: number // ✅ 이론치 데이터 인덱스 추가
      timestamp: string
      passId: number
      elapsedTimeSeconds: number

      // 원본 데이터 (변환 전)
      originalAzimuth: number
      originalElevation: number
      originalRange: number
      originalAltitude: number

      // 축변환 데이터 (기울기 변환 적용)
      axisTransformedAzimuth: number
      axisTransformedElevation: number
      axisTransformedRange: number
      axisTransformedAltitude: number

      // 최종 변환 데이터 (±270도 제한 적용)
      finalTransformedAzimuth: number
      finalTransformedElevation: number
      finalTransformedRange: number
      finalTransformedAltitude: number

      // 명령 및 실제 추적 데이터
      cmdAz: number
      cmdEl: number
      trackingAzimuthTime: number
      trackingCMDAzimuthAngle: number
      trackingActualAzimuthAngle: number
      trackingElevationTime: number
      trackingCMDElevationAngle: number
      trackingActualElevationAngle: number
      trackingTiltTime: number
      trackingCMDTiltAngle: number
      trackingActualTiltAngle: number

      // 오차 분석
      azimuthError: number
      elevationError: number
      originalToAxisTransformationError: number
      axisToFinalTransformationError: number
      totalTransformationError: number

      // 변환 정보
      tiltAngle: number
      transformationType: string
      hasTransformation: boolean
      interpolationAccuracy: number
    }>
    totalCount?: number
    error?: string
  }> {
    try {
      const response = await api.get('/ephemeris/realtime-tracking-data-with-transformations')
      return response.data
    } catch (error) {
      console.error('실시간 추적 데이터 조회 API 호출 실패:', error)
      throw error
    }
  }

  /**
   * 스케줄 상세 데이터 조회 (필터링 없이 전체 데이터 반환)
   *
   * 백엔드에서 모든 데이터를 가져옵니다 (음수 Elevation 포함).
   * 필터링은 Store의 Computed에서 수행됩니다.
   *
   * @param mstId 스케줄 마스터 ID
   * @returns 전체 상세 데이터 배열
   */
  /**
   * 상세 데이터 조회
   *
   * 백엔드에서 모든 데이터를 가져옵니다 (음수 Elevation 포함).
   * 필터링은 Store의 Computed에서 수행됩니다.
   *
   * ✅ mstId와 detailId를 사용하여 상세 데이터 조회 (PassSchedule과 동일한 구조)
   *
   * @param mstId 스케줄 마스터 ID
   * @param detailId 패스 인덱스 (기본값: 0)
   * @returns 전체 상세 데이터 배열
   */
  async fetchEphemerisDetailData(
    mstId: number,
    detailId: number = 0,
  ): Promise<ScheduleDetailItem[]> {
    try {
      const response = await api.get<ScheduleDetailItem[]>(
        `/ephemeris/detail/${mstId}/pass/${detailId}`,
      )

      console.log(
        `📡 백엔드에서 전체 데이터 수신: ${response.data.length}개 (mstId: ${mstId}, detailId: ${detailId})`,
      )

      return response.data
    } catch (error) {
      console.error('❌ 상세 데이터 조회 실패:', error)
      throw error
    }
  }

  /**
   * sourceMinElevationAngle 설정값 조회
   *
   * @returns sourceMinElevationAngle 값 (도)
   */
  async getSourceMinElevationAngle(): Promise<number> {
    try {
      const response = await api.get('/settings')

      const setting = response.data.find(
        (s: SettingItem) => s.key === 'ephemeris.tracking.sourceMinElevationAngle',
      )

      const value = setting?.value ? parseFloat(setting.value) : -7.0

      console.log(`⚙️ sourceMinElevationAngle 설정값: ${value}°`)

      return value
    } catch (error) {
      console.error('❌ 설정값 조회 실패, 기본값 -7.0 사용:', error)
      return -7.0
    }
  }

  /**
   * keyholeAzimuthVelocityThreshold 설정값 조회
   *
   * @returns KEYHOLE 판단 임계값 (도/초)
   */
  async getKeyholeAzimuthVelocityThreshold(): Promise<number> {
    try {
      const response = await api.get('/settings')

      const setting = response.data.find(
        (s: SettingItem) => s.key === 'ephemeris.tracking.keyholeAzimuthVelocityThreshold',
      )

      const value = setting?.value ? parseFloat(setting.value) : 10.0

      console.log(`⚙️ keyholeAzimuthVelocityThreshold 설정값: ${value}°/s`)

      return value
    } catch (error) {
      console.error('❌ 설정값 조회 실패, 기본값 10.0 사용:', error)
      return 10.0
    }
  }

  /**
   * ✅ Original과 Final Transformed 데이터 비교 조회
   *
   * UI에서 Original(2축)과 Final Transformed 데이터를 동시에 표시하기 위한 API
   *
   * @returns 비교 데이터 (Original과 Final Transformed)
   */
  async getComparisonData(): Promise<ComparisonScheduleItem[]> {
    try {
      console.log('📊 Original과 Final Transformed 데이터 비교 조회 시작')

      const response = await api.get('/api/ephemeris/tracking/mst/comparison')

      if (response.data.status === 'success') {
        const comparisonData = response.data.data

        if (comparisonData.success) {
          const originalMst = comparisonData.originalMst || []
          const finalTransformedMst = comparisonData.finalTransformedMst || []

          // Original과 Final 데이터를 매칭하여 비교 데이터 생성
          const matchedData: ComparisonScheduleItem[] = []

          originalMst.forEach((original: Record<string, unknown>) => {
            const final = finalTransformedMst.find(
              (f: Record<string, unknown>) => f.No === original.No,
            )

            if (final) {
              matchedData.push({
                No: original.No as number,
                SatelliteID: original.SatelliteID as string,
                SatelliteName: original.SatelliteName as string,
                StartTime: original.StartTime as string,
                EndTime: original.EndTime as string,
                Duration: original.Duration as string,

                // Original 데이터 (2축)
                OriginalMaxElevation: (original.MaxElevation as number) || 0,
                OriginalMaxAzRate: (original.MaxAzRate as number) || 0,
                OriginalMaxElRate: (original.MaxElRate as number) || 0,

                // Final Transformed 데이터
                FinalMaxElevation: (final.MaxElevation as number) || 0,
                FinalMaxAzRate: (final.MaxAzRate as number) || 0,
                FinalMaxElRate: (final.MaxElRate as number) || 0,

                // KEYHOLE 정보 (Final 데이터 기준)
                IsKeyhole: (final.IsKeyhole as boolean) || false,
                RecommendedTrainAngle: (final.RecommendedTrainAngle as number) || 0,

                CreationDate: original.CreationDate as string,
                Creator: original.Creator as string,
              })
            }
          })

          console.log(`✅ 비교 데이터 조회 완료: ${matchedData.length}개 패스`)
          return matchedData
        } else {
          console.error('❌ 비교 데이터 조회 실패:', comparisonData.error)
          return []
        }
      } else {
        console.error('❌ API 호출 실패:', response.data.message)
        return []
      }
    } catch (error) {
      console.error('❌ 비교 데이터 조회 중 오류:', error)
      return []
    }
  }

  /**
   * ✅ Original과 FinalTransformed 병합 데이터 조회
   * UI 테이블에서 2축/최종변환 값을 동시에 표시하기 위한 API
   *
   * @returns 병합된 스케줄 데이터 (Original과 FinalTransformed 메타데이터 포함)
   */
  async getMergedScheduleData(): Promise<ScheduleItem[]> {
    try {
      console.log('📊 병합 스케줄 데이터 조회 시작')

      const response = await api.get('/api/ephemeris/tracking/mst/merged')

      if (response.data.status === 'success') {
        const mergedData = response.data.data as Record<string, unknown>[]

        // 🔍 디버깅: 원본 API 응답 확인
        console.log('🔍 [프론트엔드] 원본 API 응답:', response.data)
        console.log('🔍 [프론트엔드] mergedData 크기:', mergedData.length)

        // Keyhole이 있는 항목 찾기
        mergedData.forEach((item, index) => {
          const isKeyhole = item.IsKeyhole as boolean
          if (isKeyhole) {
            const no = item.No as number | undefined
            const keyholeOptimizedRecommendedTrainAngle =
              item.KeyholeOptimizedRecommendedTrainAngle as number | undefined
            const keyholeOptimizedFinalTransformedMaxAzRate =
              item.KeyholeOptimizedFinalTransformedMaxAzRate as number | undefined
            const keyholeOptimizedFinalTransformedMaxElRate =
              item.KeyholeOptimizedFinalTransformedMaxElRate as number | undefined
            const optimizationImprovement = item.OptimizationImprovement as number | undefined
            const optimizationImprovementRate = item.OptimizationImprovementRate as
              | number
              | undefined

            console.log(`🔍 [프론트엔드] 원본 API 응답 - Item #${index + 1}:`)
            console.log(`   - No:`, no)
            console.log(`   - IsKeyhole:`, isKeyhole)
            console.log(
              `   - KeyholeOptimizedRecommendedTrainAngle:`,
              keyholeOptimizedRecommendedTrainAngle,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxAzRate:`,
              keyholeOptimizedFinalTransformedMaxAzRate,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxElRate:`,
              keyholeOptimizedFinalTransformedMaxElRate,
            )
            console.log(`   - OptimizationImprovement:`, optimizationImprovement)
            console.log(`   - OptimizationImprovementRate:`, optimizationImprovementRate)
            console.log(`   - 전체 item:`, item)
          }
        })

        const scheduleItems: ScheduleItem[] = mergedData.map((item) => ({
          No: item.No as number,
          SatelliteID: item.SatelliteID as string,
          SatelliteName: item.SatelliteName as string,
          StartTime: item.StartTime as string,
          EndTime: item.EndTime as string,
          Duration: item.Duration as string,
          MaxElevation: item.MaxElevation as number,

          // ✅ 2축 (Original) 속도
          OriginalMaxAzRate: item.OriginalMaxAzRate as number | undefined,
          OriginalMaxElRate: item.OriginalMaxElRate as number | undefined,
          OriginalMaxElevation: item.OriginalMaxElevation as number | undefined,

          // ✅ Train=0 (FinalTransformed) 속도
          FinalTransformedMaxAzRate: item.FinalTransformedMaxAzRate as number,
          FinalTransformedMaxElRate: item.FinalTransformedMaxElRate as number,

          // ✅ TrainOK (KeyholeFinalTransformed) 속도 (방법 1: 기존)
          KeyholeFinalTransformedMaxAzRate: item.KeyholeFinalTransformedMaxAzRate as number,
          KeyholeFinalTransformedMaxElRate: item.KeyholeFinalTransformedMaxElRate as number,

          // Keyhole 관련 (방법 1: 기존)
          isKeyhole: item.IsKeyhole as boolean,
          recommendedTrainAngle: item.RecommendedTrainAngle as number,

          // ✅ 방법 2 (신규): 최적화 데이터 추가
          KeyholeOptimizedRecommendedTrainAngle: item.KeyholeOptimizedRecommendedTrainAngle as
            | number
            | undefined,
          KeyholeOptimizedFinalTransformedMaxAzRate:
            item.KeyholeOptimizedFinalTransformedMaxAzRate as number | undefined,
          KeyholeOptimizedFinalTransformedMaxElRate:
            item.KeyholeOptimizedFinalTransformedMaxElRate as number | undefined,
          OptimizationImprovement: item.OptimizationImprovement as number | undefined,
          OptimizationImprovementRate: item.OptimizationImprovementRate as number | undefined,

          CreationDate: item.CreationDate as string,
          Creator: item.Creator as string,
        }))

        console.log(`✅ 병합 데이터 조회 완료: ${scheduleItems.length}개 패스`)

        // 🔍 디버깅: Keyhole이 있는 항목의 최적화 데이터 확인
        scheduleItems.forEach((item, index) => {
          if (item.isKeyhole) {
            console.log(`🔍 [프론트엔드] Schedule #${index + 1} (MST ID: ${item.id}):`)
            console.log(`   - isKeyhole: ${item.isKeyhole}`)
            console.log(
              `   - KeyholeOptimizedRecommendedTrainAngle: ${item.KeyholeOptimizedRecommendedTrainAngle}`,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxAzRate: ${item.KeyholeOptimizedFinalTransformedMaxAzRate}`,
            )
            console.log(
              `   - KeyholeOptimizedFinalTransformedMaxElRate: ${item.KeyholeOptimizedFinalTransformedMaxElRate}`,
            )
            console.log(`   - OptimizationImprovement: ${item.OptimizationImprovement}`)
            console.log(`   - OptimizationImprovementRate: ${item.OptimizationImprovementRate}`)
            console.log(`   - 원본 API 응답 데이터:`, response.data.data?.[index])
          }
        })

        return scheduleItems
      } else {
        console.warn('⚠️ 병합 데이터 조회 실패:', response.data)
        return []
      }
    } catch (error) {
      console.error('❌ 병합 데이터 조회 API 호출 실패:', error)
      throw error
    }
  }
}

export const ephemerisTrackService = new EphemerisTrackService()
