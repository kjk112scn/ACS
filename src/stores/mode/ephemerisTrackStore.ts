import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import {
  ephemerisTrackService,
  type ScheduleItem,
  type ScheduleDetailItem,
  type EphemerisTrackRequest,
} from '../../services/mode/ephemerisTrackService'

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

export const useEphemerisTrackStore = defineStore('ephemerisTrack', () => {
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
    tilt: '0.00',
    time: '0.00',
    timeResult: '0.00', // ✅ timeResult 추가
  })

  // ✅ Worker 관련 상태
  let trackingWorker: Worker | null = null
  let workerInitialized = false
  let pendingUpdates = 0
  const maxPendingUpdates = 5


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

  // ===== 계산된 속성 =====
  const hasValidData = computed(() => masterData.value.length > 0)
  const isTrackingActive = computed(() => trackingStatus.value === 'active')

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

   * ✅ Worker 초기화 (올바른 경로 사용)
   */
  const initTrackingWorker = async (): Promise<void> => {
    if (workerInitialized) return

    try {

      // ✅ 올바른 Worker 경로
      trackingWorker = new Worker(

        new URL('../../workers/trackingPathWorker.ts', import.meta.url),
        { type: 'module' }
      )


      // ✅ Worker 준비 완료 대기
      await new Promise<void>((resolve, reject) => {
        const initTimeout = setTimeout(() => {

          reject(new Error('Worker 초기화 타임아웃'))
        }, 5000)

        let isInitialized = false

        trackingWorker!.onmessage = (e: MessageEvent<WorkerResponse>) => {

          if (!isInitialized) {
            clearTimeout(initTimeout)
            isInitialized = true
            resolve()
          }


          // 메시지 처리 로직
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



          // ✅ totalPoints 활용 - 통계에 추가
          workerStats.value.totalUpdates++
          workerStats.value.totalProcessingTime += processingTime
          workerStats.value.averageProcessingTime =
            workerStats.value.totalProcessingTime / workerStats.value.totalUpdates
          workerStats.value.pointsAdded += pointsAdded
          workerStats.value.currentPathPoints = totalPoints
          workerStats.value.lastUpdateTime = Date.now()




          // ✅ 100번마다 통계 출력 시 totalPoints 포함
          if (workerStats.value.totalUpdates % 100 === 0) {
            console.log('📊 Worker 성능 통계:', {
              평균처리시간: workerStats.value.averageProcessingTime.toFixed(2) + 'ms',
              총업데이트: workerStats.value.totalUpdates,
              추가된포인트: workerStats.value.pointsAdded,
              현재포인트수: totalPoints, // ✅ totalPoints 사용
              대기중업데이트: pendingUpdates,
              오류수: workerStats.value.errors,
            })
          }












        }

        trackingWorker!.onerror = (error: ErrorEvent) => {
          clearTimeout(initTimeout)

          console.error('🚫 Worker 오류:', error.message)
          workerStats.value.errors++
          workerInitialized = false
          trackingWorker = null
          reject(new Error(`Worker 오류: ${error.message}`))
        }


        // 초기화 테스트 메시지
        trackingWorker!.postMessage({
          azimuth: 0,
          elevation: 0,
          currentPath: [],
          maxPoints: 1,
          threshold: 0.1,

        })
      })

      workerInitialized = true

      console.log('✅ Worker 초기화 완료')
    } catch (error) {
      console.error('🚫 Worker 생성 실패:', error)
      workerInitialized = false
      throw error
    }
  }

  /**
   * ✅ 추적 경로 업데이트 (비동기 최적화의 핵심)
   */
  const updateTrackingPath = async (azimuth: number, elevation: number): Promise<void> => {
    // ✅ 입력 검증
    if (typeof azimuth !== 'number' || typeof elevation !== 'number') {
      console.warn('🚫 잘못된 입력 타입:', { azimuth, elevation })
      return
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

    // ✅ Worker에 비동기 처리 요청
    const message: WorkerMessage = {
      azimuth,
      elevation,
      currentPath: [...currentPath], // 깊은 복사로 안전성 보장
      maxPoints: 150,
      threshold: 0.3,
    }

    pendingUpdates++
    trackingWorker.postMessage(message)
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
      if (currentPath.length > 150) {
        currentPath.splice(0, currentPath.length - 150)
      }

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
   * ✅ Worker 정리
   */
  const cleanupTrackingWorker = (): void => {
    if (trackingWorker) {
      trackingWorker.terminate()
      trackingWorker = null
      workerInitialized = false
      pendingUpdates = 0
      console.log('🧹 TypeScript Tracking Worker 정리 완료')
    }
  }

  /**
   * ✅ 추적 경로 초기화
   */
  const clearTrackingPath = (): void => {
    trackingPath.value.rawPath = []
    trackingPath.value.sampledPath = []
    trackingPath.value.lastUpdateTime = 0
    pendingUpdates = 0

    // ✅ 통계 초기화
    workerStats.value = {
      totalUpdates: 0,
      totalProcessingTime: 0,
      averageProcessingTime: 0,
      pointsAdded: 0,
      currentPathPoints: 0, // ✅ 모든 초기화에 추가
      lastUpdateTime: 0,
      errors: 0,
    }
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
   * 스케줄 선택 및 세부 데이터 로드
   */
  const selectSchedule = async (schedule: ScheduleItem) => {
    selectedSchedule.value = schedule
    currentTrackingPassId.value = schedule.No

    try {
      await ephemerisTrackService.setCurrentTrackingPassId(schedule.No)
      const details = await ephemerisTrackService.fetchEphemerisDetailData(schedule.No)
      detailData.value = details
      return details
    } catch (err) {
      error.value = 'Failed to select schedule'
      throw err
    }
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
    type: 'azimuth' | 'elevation' | 'tilt' | 'time' | 'timeResult',
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
   * 상태 초기화 (전체 리셋)
   */
  const reset = () => {
    masterData.value = []
    detailData.value = []
    selectedSchedule.value = null
    currentTrackingPassId.value = null
    trackingStatus.value = 'idle'
    tleData.value = null
    error.value = null
    lastFetchTime.value = 0

    // ✅ 새로 추가된 상태들도 초기화
    tleDisplayData.value = {
      displayText: 'No TLE data available',
      tleLine1: undefined,
      tleLine2: undefined,
      satelliteName: undefined,
    }
    clearTrackingPath()
    offsetValues.value = {
      azimuth: '0.00',
      elevation: '0.00',
      tilt: '0.00',
      time: '0.00',
      timeResult: '0.00',
    }

    // ✅ Worker도 정리
    cleanupTrackingWorker()
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

    // 계산된 속성
    hasValidData,
    isTrackingActive,
    currentScheduleInfo,

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

    // ✅ Worker-related 액션들
    updateTrackingPath,
    clearTrackingPath,
    cleanupTrackingWorker,
    updateOffsetValues,
    updateTLEDisplayData,
  }
})

// ✅ 타입 export
export type {
  ScheduleItem,
  ScheduleDetailItem,
  EphemerisTrackRequest,
  TLEData,
  TrackingPath,
  WorkerMessage,
  WorkerResponse,
}
