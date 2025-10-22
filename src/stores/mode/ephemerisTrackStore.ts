import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import {
  ephemerisTrackService,
  type ScheduleItem,
  type ScheduleDetailItem,
  type EphemerisTrackRequest,
  type GeostationaryTrackingRequest,
  type GeostationaryTrackingResponse,
} from '../../services/mode/ephemerisTrackService'

// ✅ 기본값 상수 정의 (파일 상단에 추가)
const DEFAULT_WORKER_STATS = {
  totalUpdates: 0,
  totalProcessingTime: 0,
  averageProcessingTime: 0,
  pointsAdded: 0,
  currentPathPoints: 0,
  lastUpdateTime: 0,
  errors: 0,
} as const

// ✅ Worker 타입 정의 (Worker 파일과 동일하게 유지)
interface WorkerMessage {
  azimuth: number
  elevation: number
  currentPath: [number, number][]
  maxPoints?: number
  threshold?: number
}

interface WorkerResponse {
  updatedPath: [number, number][]
  processingTime: number
  pointsAdded: number
  totalPoints: number
  error?: string
}

// ✅ TLE 데이터 인터페이스
interface TLEData {
  displayText: string
  tleLine1: string | undefined
  tleLine2: string | undefined
  satelliteName: string | null | undefined
  startTime?: string
  endTime?: string
  stepSize?: number
}

// ✅ 추적 경로 데이터 인터페이스
interface TrackingPath {
  rawPath: [number, number][]
  sampledPath: [number, number][]
  lastUpdateTime: number
}
export const useEphemerisTrackModeStore = defineStore('ephemerisTrack', () => {
  // ===== 상태 정의 =====
  const masterData = ref<ScheduleItem[]>([])
  const detailData = ref<ScheduleDetailItem[]>([])
  const selectedSchedule = ref<ScheduleItem | null>(null)
  const currentTrackingPassId = ref<number | null>(null)
  const trackingStatus = ref<'idle' | 'active' | 'paused' | 'error'>('idle')
  const tleData = ref<EphemerisTrackRequest | null>(null)

  // ✅ 새로 추가된 상태들
  const tleDisplayData = ref<TLEData>({
    displayText: 'No TLE data available',
    tleLine1: undefined,
    tleLine2: undefined,
    satelliteName: undefined,
  })

  const trackingPath = ref<TrackingPath>({
    rawPath: [],
    sampledPath: [],
    lastUpdateTime: 0,
  })

  // ✅ 오프셋 값들 저장
  const offsetValues = ref({
    azimuth: '0.00',
    elevation: '0.00',
    train: '0.00',
    time: '0.00',
    timeResult: '0.00', // ✅ timeResult 추가
  })

  // ✅ 정지궤도 각도 정보 저장
  const geostationaryAngles = ref({
    azimuth: 0,
    elevation: 0,
    satelliteName: '',
    tleLine1: '', // ✅ TLE 라인 저장 추가
    tleLine2: '', // ✅ TLE 라인 저장 추가
    isSet: false,
  })

  // ✅ Worker 관련 상태
  let trackingWorker: Worker | null = null
  let workerInitialized = false
  let pendingUpdates = 0
  const maxPendingUpdates = 5

  // ✅ 추적 시작 지연을 위한 상태
  const trackingStartTime = ref<number | null>(null)
  const isInitialDelayActive = ref(false)
  const INITIAL_DELAY_MS = 10000 // 5초 지연

  // ✅ Worker 통계 상태에 currentPathPoints 추가
  const workerStats = ref({
    totalUpdates: 0,
    totalProcessingTime: 0,
    averageProcessingTime: 0,
    pointsAdded: 0,

    currentPathPoints: 0, // ✅ 이 필드 추가
    lastUpdateTime: 0,
    errors: 0,
  })

  // 로딩 및 에러 상태
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // 캐시 관리
  const lastFetchTime = ref<number>(0)
  const cacheTimeout = 5 * 60 * 1000 // 5분

  // ===== 새로운 상태: 전체 데이터 저장 및 필터링 =====

  /**
   * 전체 스케줄 상세 데이터 (필터링 전)
   * 백엔드에서 받은 모든 데이터 저장 (음수 Elevation 포함)
   */
  const rawDetailData = ref<ScheduleDetailItem[]>([])

  /**
   * 화면 표시용 최소 Elevation 각도 (도)
   * SettingsService.displayMinElevationAngle 값
   */
  const displayMinElevation = ref<number>(0.0)

  // ===== 계산된 속성 =====
  const hasValidData = computed(() => masterData.value.length > 0)
  const isTrackingActive = computed(() => trackingStatus.value === 'active')

  /**
   * 화면에 표시할 필터링된 상세 데이터
   * displayMinElevation 기준으로 필터링
   */
  const filteredDetailData = computed(() => {
    return rawDetailData.value.filter((item) => item.Elevation >= displayMinElevation.value)
  })

  /**
   * KEYHOLE 위성 스케줄들만 필터링
   */
  const keyholeSchedules = computed(() => {
    return masterData.value.filter((schedule) => schedule.IsKeyhole)
  })

  const currentScheduleInfo = computed(() => {
    if (!selectedSchedule.value) return null

    const now = Date.now()
    const startTime = new Date(selectedSchedule.value.StartTime).getTime()
    const endTime = new Date(selectedSchedule.value.EndTime).getTime()

    return {
      ...selectedSchedule.value,
      timeRemaining: Math.max(0, endTime - now),
      progress: Math.min(100, ((now - startTime) / (endTime - startTime)) * 100),
      isActive: now >= startTime && now <= endTime,
    }
  })

  // ===== Worker-related 메서드들 =====

  /**


   * ✅ 인라인 Worker 생성 (파일 로딩 문제 해결)
   */
  const createInlineWorker = (): Worker => {
    const workerScript = `
      // Worker 메시지 타입 정의
      self.onmessage = function(e) {
        const startTime = performance.now()

        try {
          const { azimuth, elevation, currentPath, maxPoints, threshold } = e.data

          // ✅ 입력 데이터 검증 강화
          if (typeof azimuth !== 'number' || isNaN(azimuth) || !isFinite(azimuth)) {
            throw new Error('Invalid azimuth value: ' + azimuth)
          }

          if (typeof elevation !== 'number' || isNaN(elevation) || !isFinite(elevation)) {
            throw new Error('Invalid elevation value: ' + elevation)
          }

          if (!Array.isArray(currentPath)) {
            throw new Error('currentPath is not an array: ' + typeof currentPath)
          }

          // ✅ 배열 데이터 정제
          const safePath = currentPath.filter(point => {
            return Array.isArray(point) &&
                   point.length === 2 &&
                   typeof point[0] === 'number' &&
                   typeof point[1] === 'number' &&
                   !isNaN(point[0]) && !isNaN(point[1]) &&
                   isFinite(point[0]) && isFinite(point[1])
          })

          // 정규화
          const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
          const normalizedEl = Math.max(0, Math.min(90, elevation))
          const newPoint = [normalizedEl, normalizedAz]

          // 경로 업데이트
          const updatedPath = [...safePath]

          // 중복 체크
          if (updatedPath.length > 0) {
            const lastPoint = updatedPath[updatedPath.length - 1]
            if (lastPoint) {
              const azDiff = Math.abs(lastPoint[1] - normalizedAz)
              const elDiff = Math.abs(lastPoint[0] - normalizedEl)

              if (azDiff < threshold && elDiff < threshold) {
                // 변화가 작으면 추가하지 않음
                const processingTime = performance.now() - startTime
                self.postMessage({
                  updatedPath,
                  processingTime,
                  pointsAdded: 0,
                  totalPoints: updatedPath.length,
                })
                return
              }
            }
          }

          // 새 포인트 추가
          updatedPath.push(newPoint)

          // 크기 제한
          //if (updatedPath.length > maxPoints) {
          //  updatedPath.splice(0, updatedPath.length - maxPoints)
          //}

          const processingTime = performance.now() - startTime

          self.postMessage({
            updatedPath,
            processingTime,
            pointsAdded: 1,
            totalPoints: updatedPath.length,
          })

        } catch (error) {
          const processingTime = performance.now() - startTime
          self.postMessage({
            updatedPath: [],
            processingTime,
            pointsAdded: 0,
            totalPoints: 0,
            error: error.message || 'Unknown error',
          })
        }
      }
    `

    const blob = new Blob([workerScript], { type: 'application/javascript' })
    return new Worker(URL.createObjectURL(blob))
  }

  /**
   * ✅ Worker 초기화 (인라인 Worker 사용)
   */
  const initTrackingWorker = async (): Promise<void> => {
    if (workerInitialized) return

    try {
      // ✅ 인라인 Worker 생성
      trackingWorker = createInlineWorker()

      // ✅ Worker 준비 완료 대기
      await new Promise<void>((resolve, reject) => {
        const initTimeout = setTimeout(() => {
          reject(new Error('Worker 초기화 타임아웃'))
        }, 5000)

        let isInitialized = false

        trackingWorker.onmessage = (e: MessageEvent<WorkerResponse>) => {
          if (!isInitialized) {
            clearTimeout(initTimeout)
            isInitialized = true
            console.log('✅ 인라인 Worker 초기화 완료')
            resolve()
          }

          const { updatedPath, processingTime, pointsAdded, totalPoints, error } = e.data

          pendingUpdates = Math.max(0, pendingUpdates - 1)

          if (error) {
            console.error('🚫 Worker 오류:', error)
            workerStats.value.errors++
            return
          }

          // 상태 업데이트
          trackingPath.value.rawPath = updatedPath
          trackingPath.value.sampledPath = updatedPath
          trackingPath.value.lastUpdateTime = Date.now()

          // 통계 업데이트
          workerStats.value.totalUpdates++
          workerStats.value.totalProcessingTime += processingTime
          workerStats.value.averageProcessingTime =
            workerStats.value.totalProcessingTime / workerStats.value.totalUpdates
          workerStats.value.pointsAdded += pointsAdded
          workerStats.value.currentPathPoints = totalPoints
          workerStats.value.lastUpdateTime = Date.now()

          // 100번마다 통계 출력
          if (workerStats.value.totalUpdates % 100 === 0) {
            console.log('📊 Worker 성능 통계:', {
              평균처리시간: workerStats.value.averageProcessingTime.toFixed(2) + 'ms',
              총업데이트: workerStats.value.totalUpdates,
              추가된포인트: workerStats.value.pointsAdded,

              현재포인트수: totalPoints,
              대기중업데이트: pendingUpdates,
              오류수: workerStats.value.errors,
            })
          }
        }

        trackingWorker.onerror = (error: ErrorEvent) => {
          clearTimeout(initTimeout)

          console.error('🚫 Worker 오류:', error.message)
          workerStats.value.errors++
          workerInitialized = false
          trackingWorker = null
          reject(new Error(`Worker 오류: ${error.message}`))
        }

        // 초기화 테스트 메시지
        trackingWorker.postMessage({
          azimuth: 0,
          elevation: 0,
          currentPath: [],
          maxPoints: Number.MAX_SAFE_INTEGER,
          threshold: 0.1,
        })
      })

      workerInitialized = true

      console.log('✅ 인라인 Worker 초기화 완료')
    } catch (error) {
      console.error('🚫 Worker 생성 실패:', error)
      workerInitialized = false
      throw error
    }
  }

  /**

   * ✅ 추적 경로 업데이트 (비동기 최적화의 핵심) - 수정된 버전
   */
  const updateTrackingPath = async (azimuth: number, elevation: number): Promise<void> => {
    // ✅ 입력 검증
    if (typeof azimuth !== 'number' || typeof elevation !== 'number') {
      console.warn('🚫 잘못된 입력 타입:', { azimuth, elevation })
      return
    }

    // ✅ NaN 체크 추가
    if (isNaN(azimuth) || isNaN(elevation)) {
      console.warn('🚫 NaN 값 감지:', { azimuth, elevation })
      return
    }

    // ✅ 추적 시작 후 5초 지연 체크
    if (isInitialDelayActive.value && trackingStartTime.value) {
      const elapsedTime = Date.now() - trackingStartTime.value
      if (elapsedTime < INITIAL_DELAY_MS) {
        // console.log(`⏸️ 추적 시작 지연 중... (${elapsedTime}ms / ${INITIAL_DELAY_MS}ms)`)
        return // 경로 업데이트 무시
      } else {
        // ✅ 지연 시간 완료
        isInitialDelayActive.value = false
        console.log('✅ 추적 시작 지연 완료 - 경로 그리기 시작')
      }
    }

    // ✅ Worker 초기화 (비동기)
    if (!workerInitialized) {
      try {
        await initTrackingWorker()
      } catch (error) {
        console.error('Worker 초기화 실패, 폴백 처리:', error)
        // ✅ Worker 실패 시 폴백: 직접 처리
        fallbackUpdatePath(azimuth, elevation)
        return
      }
    }

    // ✅ Worker 과부하 방지
    if (!trackingWorker || pendingUpdates >= maxPendingUpdates) {
      // console.log('⚠️ Worker 과부하, 업데이트 스킵')
      return
    }

    // ✅ 중복 데이터 필터링 (성능 최적화)
    const currentPath = trackingPath.value.rawPath
    if (currentPath.length > 0) {
      const lastPoint = currentPath[currentPath.length - 1]
      if (lastPoint) {
        const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
        const normalizedEl = Math.max(0, Math.min(90, elevation))

        const azDiff = Math.abs(lastPoint[1] - normalizedAz)
        const elDiff = Math.abs(lastPoint[0] - normalizedEl)

        // ✅ 임계값 이하 변화는 무시 (성능 최적화)
        if (azDiff < 0.3 && elDiff < 0.3) {
          return
        }
      }
    }

    try {
      // ✅ 안전한 데이터 준비 (직렬화 가능한 형태로 변환)
      const safeCurrentPath: [number, number][] = currentPath
        .filter((point) => Array.isArray(point) && point.length === 2)
        .map((point) => [Number(point[0]) || 0, Number(point[1]) || 0] as [number, number])
        .filter((point) => !isNaN(point[0]) && !isNaN(point[1]))

      // ✅ Worker에 비동기 처리 요청 - 안전한 메시지 생성
      const message: WorkerMessage = {
        azimuth: Number(azimuth),
        elevation: Number(elevation),
        currentPath: safeCurrentPath, // 정제된 안전한 데이터
        maxPoints: 150,
        threshold: 0.3,
      }

      // ✅ 메시지 직렬화 테스트
      try {
        JSON.stringify(message)
      } catch (serializeError) {
        console.error('🚫 메시지 직렬화 실패:', serializeError)
        fallbackUpdatePath(azimuth, elevation)
        return
      }

      pendingUpdates++
      trackingWorker.postMessage(message)
    } catch (error) {
      console.error('🚫 Worker 메시지 전송 실패:', error)
      pendingUpdates = Math.max(0, pendingUpdates - 1)
      fallbackUpdatePath(azimuth, elevation)
    }
  }

  /**
   * ✅ Worker 실패 시 폴백 함수 개선
   */
  const fallbackUpdatePath = (azimuth: number, elevation: number): void => {
    try {
      const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
      const normalizedEl = Math.max(0, Math.min(90, elevation))
      const currentPoint: [number, number] = [normalizedEl, normalizedAz]

      const currentPath = [...trackingPath.value.rawPath]

      // 중복 체크
      if (currentPath.length > 0) {
        const lastPoint = currentPath[currentPath.length - 1]
        if (lastPoint) {
          const azDiff = Math.abs(lastPoint[1] - normalizedAz)
          const elDiff = Math.abs(lastPoint[0] - normalizedEl)
          if (azDiff < 0.3 && elDiff < 0.3) {
            return
          }
        }
      }

      currentPath.push(currentPoint)

      // 크기 제한
      /*      if (currentPath.length > 150) {
        currentPath.splice(0, currentPath.length - 150)
      } */

      // 상태 업데이트
      trackingPath.value.rawPath = currentPath
      trackingPath.value.sampledPath = currentPath
      trackingPath.value.lastUpdateTime = Date.now()

      // ✅ 폴백 모드 표시 (너무 자주 출력되지 않도록)
      if (currentPath.length % 50 === 0) {
        console.log('📍 폴백 모드로 경로 업데이트 중...')
      }
    } catch (error) {
      console.error('폴백 처리 실패:', error)
    }
  }

  /**

   * ✅ Worker 정리 및 통계 초기화 (Blob URL 해제 포함)
   */

  const cleanupWorker = () => {
    if (trackingWorker) {
      // ✅ Blob URL 해제 (메모리 누수 방지)
      try {
        trackingWorker.terminate()
      } catch (error) {
        console.warn('Worker 종료 중 오류:', error)
      }
      trackingWorker = null
    }
    workerInitialized = false
    pendingUpdates = 0

    // 통계 초기화
    workerStats.value = { ...DEFAULT_WORKER_STATS }
  }

  /**
   * ✅ 추적 경로 초기화 (현재 위치로 시작)
   */
  const clearTrackingPath = (currentAzimuth?: number, currentElevation?: number): void => {
    // ✅ 현재 위치를 첫 번째 포인트로 설정 (0에서 시작하는 문제 해결)
    const azimuth = currentAzimuth ?? 0
    const elevation = currentElevation ?? 0

    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    const initialPoint: [number, number] = [normalizedEl, normalizedAz]

    trackingPath.value.rawPath = [initialPoint]
    trackingPath.value.sampledPath = [initialPoint]
    trackingPath.value.lastUpdateTime = Date.now()
    pendingUpdates = 0

    // ✅ 지연 관련 상태 초기화
    trackingStartTime.value = null
    isInitialDelayActive.value = false

    // ✅ 통계 초기화
    workerStats.value = {
      totalUpdates: 0,
      totalProcessingTime: 0,
      averageProcessingTime: 0,
      pointsAdded: 0,
      currentPathPoints: 0,
      lastUpdateTime: 0,
      errors: 0,
    }

    console.log('🧹 추적 경로 초기화 완료 - 현재 위치 기준:', {
      azimuth: normalizedAz,
      elevation: normalizedEl,
    })
  }

  // ===== 기존 액션 메서드들 =====

  /**
   * 마스터 데이터 로드 (캐시 고려)
   */
  const loadMasterData = async (forceRefresh = false) => {
    const now = Date.now()
    const shouldRefresh =
      forceRefresh || now - lastFetchTime.value > cacheTimeout || masterData.value.length === 0

    if (!shouldRefresh) {
      return masterData.value
    }

    isLoading.value = true
    error.value = null

    try {
      const data = await ephemerisTrackService.fetchEphemerisMasterData()
      masterData.value = data
      lastFetchTime.value = now
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load data'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 스케줄 선택 및 상세 데이터 로드
   *
   * @param schedule 선택된 스케줄 아이템
   */
  const selectSchedule = async (schedule: ScheduleItem) => {
    selectedSchedule.value = schedule
    currentTrackingPassId.value = schedule.No

    try {
      await ephemerisTrackService.setCurrentTrackingPassId(schedule.No)

      // 1. 백엔드에서 전체 데이터 조회 (필터링 없음)
      const allData = await ephemerisTrackService.fetchEphemerisDetailData(schedule.No)

      // 2. 전체 데이터 저장
      rawDetailData.value = allData

      // 3. displayMinElevation 설정값 조회 및 저장
      displayMinElevation.value = await ephemerisTrackService.getDisplayMinElevationAngle()

      // 4. 기존 detailData도 업데이트 (호환성 유지)
      detailData.value = filteredDetailData.value

      console.log(`✅ 스케줄 데이터 로드 완료:
        - 전체 데이터: ${rawDetailData.value.length}개
        - 표시 데이터: ${filteredDetailData.value.length}개
        - 필터 기준: ${displayMinElevation.value}°
        - KEYHOLE: ${schedule.IsKeyhole ? 'YES' : 'NO'}
        - Train 각도: ${schedule.RecommendedTrainAngle}°
      `)

      return filteredDetailData.value
    } catch (err) {
      error.value = 'Failed to select schedule'
      throw err
    }
  }

  /**
   * displayMinElevation 설정값 업데이트
   * 설정 변경 시 호출하여 즉시 필터링 반영
   *
   * @param newValue 새로운 최소 Elevation 값 (도)
   */
  const updateDisplayMinElevation = (newValue: number) => {
    displayMinElevation.value = newValue
    // 기존 detailData도 업데이트 (호환성 유지)
    detailData.value = filteredDetailData.value
    console.log(
      `🔄 표시 필터 업데이트: ${newValue}° (표시 데이터: ${filteredDetailData.value.length}개)`,
    )
  }

  /**
   * 추적 시작
   */
  const startTracking = async () => {
    if (!currentTrackingPassId.value) {
      throw new Error('No schedule selected')
    }

    try {
      await ephemerisTrackService.startEphemerisTracking(currentTrackingPassId.value)
      trackingStatus.value = 'active'
      trackingStartTime.value = Date.now() // 추적 시작 시간 기록
      isInitialDelayActive.value = true // 지연 시작 활성화
    } catch (err) {
      trackingStatus.value = 'error'
      error.value = 'Failed to start tracking'
      throw err
    }
  }

  /**
   * 추적 중지
   */
  const stopTracking = async () => {
    try {
      await ephemerisTrackService.stopEphemerisTracking()
      trackingStatus.value = 'idle'
      //currentTrackingPassId.value = null
    } catch (err) {
      error.value = 'Failed to stop tracking'
      throw err
    }
  }

  /**
   * TLE 데이터 처리
   */
  const processTLEData = async (tleText: string) => {
    try {
      const parsed = ephemerisTrackService.parseTLEData(tleText)

      const request: EphemerisTrackRequest = {
        tleLine1: parsed.tleLine1,
        tleLine2: parsed.tleLine2,
        satelliteName: parsed.satelliteName || 'Unknown',
        startTime: new Date().toISOString(),
        endTime: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        stepSize: 60,
      }

      await ephemerisTrackService.generateEphemerisTrack(request)
      tleData.value = request

      // ✅ TLE 표시 데이터도 저장
      tleDisplayData.value = {
        displayText: tleText,
        tleLine1: parsed.tleLine1,
        tleLine2: parsed.tleLine2,
        satelliteName: parsed.satelliteName,
        startTime: request.startTime,
        endTime: request.endTime,
        stepSize: request.stepSize,
      }

      // 새 데이터 생성 후 마스터 데이터 새로고침
      await loadMasterData(true)

      return request
    } catch (err) {
      error.value = 'Failed to process TLE data'
      throw err
    }
  }

  /**
   * 오프셋 값 업데이트
   */
  const updateOffsetValues = (
    type: 'azimuth' | 'elevation' | 'train' | 'time' | 'timeResult',
    value: string,
  ) => {
    offsetValues.value[type] = value
  }

  /**
   * TLE 표시 데이터 업데이트
   */
  const updateTLEDisplayData = (data: Partial<TLEData>) => {
    tleDisplayData.value = { ...tleDisplayData.value, ...data }
  }

  /**
   * 데이터 삭제
   */
  const deleteSchedule = async (mstId: number) => {
    try {
      await ephemerisTrackService.deleteEphemerisData(mstId)
      // 삭제 후 마스터 데이터 새로고침
      await loadMasterData(true)

      // 삭제된 스케줄이 현재 선택된 스케줄이면 선택 해제
      if (selectedSchedule.value?.No === mstId) {
        clearSelection()
      }
    } catch (err) {
      error.value = 'Failed to delete schedule'
      throw err
    }
  }

  /**
   * 시간 오프셋 명령 전송
   */
  const sendTimeOffset = async (timeOffset: number) => {
    try {
      return await ephemerisTrackService.sendTimeOffsetCommand(timeOffset)
    } catch (err) {
      error.value = 'Failed to send time offset'
      throw err
    }
  }

  /**
   * 정지궤도 각도 계산 (TLE 입력 시)
   */
  const calculateGeostationaryAngles = async (
    tleLine1: string,
    tleLine2: string,
    satelliteName?: string,
  ) => {
    try {
      const request = { tleLine1, tleLine2 }
      const response = await ephemerisTrackService.calculateGeostationaryAngles(request)

      // ✅ 정지궤도 각도 정보 설정 (백엔드에서 계산된 값 사용)
      geostationaryAngles.value = {
        azimuth: response.azimuth,
        elevation: response.elevation,
        satelliteName: satelliteName || response.satelliteId,
        tleLine1: tleLine1,
        tleLine2: tleLine2,
        isSet: true,
      }

      // 성공 시 알림
      console.log('정지궤도 각도 계산 완료:', response)

      return response
    } catch (err) {
      error.value = '정지궤도 각도 계산에 실패했습니다'
      throw err
    }
  }

  /**
   * 정지궤도 위성 추적 시작 (GO 버튼 클릭 시)
   */
  const startGeostationaryTracking = async (tleLine1: string, tleLine2: string) => {
    try {
      const request = { tleLine1, tleLine2 }
      const response = await ephemerisTrackService.startGeostationaryTracking(request)

      // 성공 시 알림
      console.log('정지궤도 추적 시작:', response)

      return response
    } catch (err) {
      error.value = '정지궤도 위성 추적 시작에 실패했습니다'
      throw err
    }
  }

  /**
   * 정지궤도 추적 활성화 (GO 버튼 클릭 시)
   */
  const activateGeostationaryTracking = () => {
    try {
      if (!geostationaryAngles.value.isSet) {
        throw new Error('정지궤도 각도 정보가 설정되지 않았습니다')
      }

      // 추적 상태를 활성화
      trackingStatus.value = 'active'
      currentTrackingPassId.value = 0 // 정지궤도는 passId가 없음

      console.log('정지궤도 추적 활성화됨')
      return { success: true }
    } catch (err) {
      error.value = '정지궤도 추적 활성화에 실패했습니다'
      throw err
    }
  }

  /**
   * 정지궤도 각도만 초기화하는 메서드
   */
  const resetGeostationaryAngles = () => {
    geostationaryAngles.value = {
      azimuth: 0,
      elevation: 0,
      satelliteName: '',
      tleLine1: '',
      tleLine2: '',
      isSet: false,
    }
  }

  /**
   * 상태 초기화 (전체 리셋)
   */
  const reset = () => {
    // ✅ 기본 상태 초기화
    masterData.value = []
    detailData.value = []
    selectedSchedule.value = null
    currentTrackingPassId.value = null
    trackingStatus.value = 'idle'
    tleData.value = null

    // ✅ TLE 표시 데이터 초기화
    tleDisplayData.value = {
      displayText: 'No TLE data available',
      tleLine1: undefined,
      tleLine2: undefined,
      satelliteName: undefined,
    }

    // ✅ 추적 경로 초기화
    trackingPath.value = {
      rawPath: [],
      sampledPath: [],
      lastUpdateTime: 0,
    }

    // ✅ 지연 관련 상태 초기화
    trackingStartTime.value = null
    isInitialDelayActive.value = false

    // ✅ 오프셋 값 초기화
    offsetValues.value = {
      azimuth: '0.00',
      elevation: '0.00',
      train: '0.00',
      time: '0.00',
      timeResult: '0.00',
    }

    // ✅ 정지궤도 각도 초기화
    geostationaryAngles.value = {
      azimuth: 0,
      elevation: 0,
      satelliteName: '',
      tleLine1: '',
      tleLine2: '',
      isSet: false,
    }

    // ✅ Worker 통계 초기화
    workerStats.value = {
      totalUpdates: 0,
      totalProcessingTime: 0,
      averageProcessingTime: 0,
      pointsAdded: 0,
      currentPathPoints: 0,
      lastUpdateTime: 0,
      errors: 0,
    }

    // ✅ Worker 정리
    cleanupWorker()

    console.log('🔄 Ephemeris Track Store 초기화 완료')
  }

  /**
   * 선택 상태 클리어
   */
  const clearSelection = () => {
    selectedSchedule.value = null
    currentTrackingPassId.value = null
    detailData.value = []
  }

  /**
   * 에러 상태 클리어
   */
  const clearError = () => {
    error.value = null
  }

  /**
   * 스케줄 데이터만 초기화
   */
  const clearScheduleData = () => {
    masterData.value = []
    detailData.value = []
    selectedSchedule.value = null
    currentTrackingPassId.value = null
  }

  return {
    // 상태 (readonly로 외부 수정 방지)
    masterData: readonly(masterData),
    detailData: readonly(detailData),
    selectedSchedule: readonly(selectedSchedule),
    trackingStatus: readonly(trackingStatus),
    tleData: readonly(tleData),
    isLoading: readonly(isLoading),
    error: readonly(error),
    currentTrackingPassId: readonly(currentTrackingPassId),

    // ✅ 새로 추가된 상태들
    tleDisplayData: readonly(tleDisplayData),
    trackingPath: readonly(trackingPath),
    offsetValues: readonly(offsetValues),
    workerStats: readonly(workerStats),
    geostationaryAngles: readonly(geostationaryAngles),

    // ✅ 새로운 필터링 관련 상태
    rawDetailData: readonly(rawDetailData),
    displayMinElevation: readonly(displayMinElevation),

    // 계산된 속성
    hasValidData,
    isTrackingActive,
    currentScheduleInfo,
    filteredDetailData, // 필터링된 데이터
    keyholeSchedules, // KEYHOLE 위성들

    // 기존 액션
    loadMasterData,
    selectSchedule,
    startTracking,
    stopTracking,
    processTLEData,
    deleteSchedule,
    sendTimeOffset,
    reset,
    clearSelection,
    clearError,
    clearScheduleData,

    // ✅ Worker-related 액션들
    updateTrackingPath,
    clearTrackingPath,

    cleanupWorker,
    updateOffsetValues,
    updateTLEDisplayData,
    calculateGeostationaryAngles,
    startGeostationaryTracking,
    activateGeostationaryTracking,
    resetGeostationaryAngles,

    // ✅ 새로운 필터링 관련 액션
    updateDisplayMinElevation,
  }
})

// ✅ 타입 export
export type {
  ScheduleItem,
  ScheduleDetailItem,
  EphemerisTrackRequest,
  GeostationaryTrackingRequest,
  GeostationaryTrackingResponse,
  TLEData,
  TrackingPath,
  WorkerMessage,
  WorkerResponse,
}
