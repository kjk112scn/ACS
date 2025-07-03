import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue' // computed, readonly import 추가
import { useQuasar } from 'quasar'
import {
  passScheduleService,
  type AddTleAndTrackingRequest,
  type TleAndTrackingResponse,
  type PassScheduleMasterData,
  type SetTrackingTargetsRequest,
  type TrackingTarget,
  type TrackingDetailItem,
} from '../../services/mode/passScheduleService'

export interface ScheduleItem {
  no: number
  index?: number // 🔧 index 필드 확인/추가
  satelliteId?: string
  satelliteName: string
  startTime: string
  endTime: string
  startAzimuthAngle: number
  endAzimuthAngle: number
  startElevationAngle: number
  endElevationAngle: number
  tilt: number
  duration: string
  maxAzimuthRate?: number
  maxElevationRate?: number
  maxAzimuthAccel?: number
  maxElevationAccel?: number
  originalStartAzimuth?: number
  originalEndAzimuth?: number
  maxElevation?: number
  maxElevationTime?: string
}

// 🔧 타입들을 export하여 다른 파일에서 사용 가능하게 함
export interface TLEUploadResult {
  success: boolean
  successCount: number
  failedCount: number
  totalCount: number
  totalPasses: number
  totalTrackingPoints: number
  processingTime: number
  completedSatellites: string[]
  failedSatellites: string[]
  results: Array<{
    satelliteId: string
    success: boolean
    passCount?: number
    trackingPointCount?: number
    error?: string
  }>
}

export interface ProgressCallback {
  onProgress?: (completed: number, total: number, currentSatellite: string) => void
  onSuccess?: (satelliteId: string, response: TleAndTrackingResponse) => void
  onError?: (satelliteId: string, error: string) => void
  onComplete?: (result: TLEUploadResult) => void
}

// TLE 아이템 타입도 export
export interface TLEItem {
  No: number
  TLE: string
}

// ✅ 오프셋 값들 저장
const offsetValues = ref({
  azimuth: '0.00',
  elevation: '0.00',
  tilt: '0.00',
  time: '0.00',
  timeResult: '0.00', // ✅ timeResult 추가
})
export const usePassScheduleStore = defineStore('passSchedule', () => {
  const $q = useQuasar()

  // 상태
  const scheduleData = ref<ScheduleItem[]>([]) // 🔧 서버에서 가져온 전체 스케줄 (모달용)
  const selectedScheduleList = ref<ScheduleItem[]>([]) // 🆕 사용자가 선택한 스케줄 목록 (테이블용)
  const selectedSchedule = ref<ScheduleItem | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // TLE 관련 상태
  const tleData = ref<TLEItem[]>([])
  const selectedTLE = ref<TLEItem | null>(null)

  // 🆕 TLE 업로드 진행 상태
  const isUploading = ref(false)
  const uploadProgress = ref(0)
  const uploadStatus = ref('')

  // 🆕 추적 모니터링 관련 상태 - 타입 수정
  const isTrackingMonitorActive = ref(false)
  const trackingMonitorStatus = ref<{
    monitoringInterval?: string | undefined
    timeReference?: string | undefined
    threadName?: string | undefined
    startedAt?: number | undefined
    uptime?: number | undefined
  }>({})

  // 🆕 추적 경로 관련 상태 추가
  const trackingDetailData = ref<TrackingDetailItem[]>([])
  const predictedTrackingPath = ref<[number, number][]>([])
  const actualTrackingPath = ref<[number, number][]>([])
  const currentTrackingPosition = ref<{ azimuth: number; elevation: number }>({
    azimuth: 0,
    elevation: 0,
  })
  const trackingPathLoading = ref(false)

  // 🆕 현재 로드된 추적 경로 정보
  const currentTrackingPathInfo = ref<{
    satelliteId: string | null
    passId: number | null
    pointCount: number
    lastUpdated: number | null
  }>({
    satelliteId: null,
    passId: null,
    pointCount: 0,
    lastUpdated: null,
  })

  // 🆕 Worker 관련 상태 (EphemerisTrackStore와 동일한 성능 최적화)
  let passScheduleWorker: Worker | null = null
  let workerInitialized = false
  const workerStats = ref({
    totalJobs: 0,
    successfulJobs: 0,
    failedJobs: 0,
    averageProcessingTime: 0,
    currentPathPoints: 0,
    lastProcessingTime: 0,
    lastErrorMessage: null as string | null,
    isProcessing: false,
  })

  // 🆕 추적 경로 통계
  const trackingPath = computed(() => ({
    sampledPath: actualTrackingPath.value,
    currentPosition: currentTrackingPosition.value,
    pathLength: actualTrackingPath.value.length,
    isActive: actualTrackingPath.value.length > 0,
    lastUpdated: currentTrackingPathInfo.value.lastUpdated,
  }))

  // 🆕 적응형 해상도 설정
  const ADAPTIVE_CONFIG = {
    maxPoints: 0, // 0 = 제한 없음 (모든 포인트 보존)
    threshold: 0.1, // 중복 포인트 제거 임계값
    memoryLimit: 50000, // 메모리 보호용 최대 포인트 수 (50,000개)
    cleanupThreshold: 40000, // 40,000개 초과 시 오래된 포인트 정리
  }

  // 🆕 메모리 관리 함수
  const cleanupOldPoints = (path: [number, number][]) => {
    if (path.length > ADAPTIVE_CONFIG.cleanupThreshold) {
      // 오래된 포인트 10% 제거 (최신 90% 유지)
      const removeCount = Math.floor(path.length * 0.1)
      console.log(`🧹 메모리 정리: ${path.length} → ${path.length - removeCount} 포인트`)
      return path.slice(removeCount)
    }
    return path
  }

  // 🆕 최적화된 updateActualTrackingPath
  const updateActualTrackingPathOptimized = async (
    azimuth: number,
    elevation: number,
  ): Promise<void> => {
    try {
      if (!workerInitialized || !passScheduleWorker) {
        const initSuccess = initializePassScheduleWorker()
        if (!initSuccess) {
          throw new Error('PassSchedule Worker 초기화 실패')
        }
      }

      // 중복 요청 방지
      if (workerStats.value.isProcessing) {
        return
      }

      workerStats.value.isProcessing = true
      workerStats.value.totalJobs++

      return new Promise<void>((resolve, reject) => {
        if (!passScheduleWorker) {
          workerStats.value.isProcessing = false
          reject(new Error('Worker가 초기화되지 않음'))
          return
        }

        const timeout = setTimeout(() => {
          workerStats.value.failedJobs++
          workerStats.value.lastErrorMessage = 'Worker 응답 시간 초과'
          workerStats.value.isProcessing = false
          reject(new Error('Worker 응답 시간 초과'))
        }, 500)

        passScheduleWorker.onmessage = (event) => {
          clearTimeout(timeout)
          workerStats.value.isProcessing = false

          try {
            const response = event.data

            if (response.success) {
              // 메모리 관리 적용
              let updatedPath = response.updatedPath
              if (updatedPath.length > ADAPTIVE_CONFIG.memoryLimit) {
                updatedPath = cleanupOldPoints(updatedPath)
              }

              // 성공적인 경로 업데이트 - 모든 포인트 보존
              actualTrackingPath.value = updatedPath
              currentTrackingPosition.value = { azimuth, elevation }
              currentTrackingPathInfo.value.lastUpdated = Date.now()

              // 통계 업데이트
              workerStats.value.successfulJobs++
              workerStats.value.currentPathPoints = updatedPath.length
              workerStats.value.lastProcessingTime = response.processingTime

              // 평균 처리 시간 계산
              workerStats.value.averageProcessingTime =
                (workerStats.value.averageProcessingTime * (workerStats.value.successfulJobs - 1) +
                  response.processingTime) /
                workerStats.value.successfulJobs

              resolve()
            } else {
              workerStats.value.failedJobs++
              workerStats.value.lastErrorMessage = response.error || 'Unknown error'
              reject(new Error(response.error || 'Worker 처리 실패'))
            }
          } catch (error) {
            workerStats.value.failedJobs++
            workerStats.value.lastErrorMessage =
              error instanceof Error ? error.message : 'Parse error'
            reject(error instanceof Error ? error : new Error('Parse error'))
          }
        }

        passScheduleWorker.onerror = (error) => {
          clearTimeout(timeout)
          workerStats.value.failedJobs++
          workerStats.value.lastErrorMessage = 'Worker 오류'
          workerStats.value.isProcessing = false
          reject(new Error('Worker 오류: ' + error.message))
        }

        // 데이터 직렬화 처리
        const currentPath = Array.isArray(actualTrackingPath.value)
          ? [...actualTrackingPath.value]
          : []

        const serializedPath = currentPath.map((point) => {
          if (Array.isArray(point) && point.length >= 2) {
            return [Number(point[0]), Number(point[1])] as [number, number]
          }
          return [0, 0] as [number, number]
        })

        // Worker로 작업 전송 - 포인트 제한 없음
        passScheduleWorker.postMessage({
          azimuth: Number(azimuth),
          elevation: Number(elevation),
          currentPath: serializedPath,
          maxPoints: ADAPTIVE_CONFIG.maxPoints, // 0 = 제한 없음
          threshold: ADAPTIVE_CONFIG.threshold,
        })
      })
    } catch (error) {
      workerStats.value.isProcessing = false
      console.error('❌ PassSchedule 추적 경로 업데이트 실패:', error)
      workerStats.value.failedJobs++
      workerStats.value.lastErrorMessage = error instanceof Error ? error.message : 'Unknown error'
    }
  }

  // 🆕 기존 함수를 최적화된 버전으로 교체
  const updateActualTrackingPath = updateActualTrackingPathOptimized

  // 스케줄 데이터 가져오기
  const fetchScheduleData = async () => {
    // 서버에서 실제 데이터를 가져오도록 변경
    return await fetchScheduleDataFromServer()
  }
  // 🔧 addSelectedSchedule 함수 개선 - API 호출 추가
  const addSelectedSchedule = async (schedule: ScheduleItem): Promise<boolean> => {
    try {
      // 중복 체크
      const exists = selectedScheduleList.value.find((item) => item.no === schedule.no)
      if (exists) {
        console.log('⚠️ 이미 선택된 스케줄:', schedule.satelliteName)
        return true // 이미 선택된 경우 성공으로 처리
      }

      console.log('🚀 스케줄 선택 및 추적 대상 설정 시작:', schedule.satelliteName)

      // 🔧 서버에 추적 대상 설정 먼저 수행
      const success = await setTrackingTargets([schedule])

      if (success) {
        // 서버 설정 성공 시에만 로컬 배열에 추가
        selectedScheduleList.value.push(schedule)
        console.log('✅ 스케줄이 선택 목록에 추가됨:', schedule.satelliteName)
        return true
      } else {
        console.error('❌ 서버 추적 대상 설정 실패')
        return false
      }
    } catch (error) {
      console.error('❌ 스케줄 선택 중 오류:', error)

      $q.notify({
        type: 'negative',
        message: '스케줄 선택 중 오류가 발생했습니다',
      })

      return false
    }
  }
  // 🔧 유연한 스케줄 추가 함수 (단일/다중 모두 처리)
  const addSchedulesToSelection = async (
    schedules: ScheduleItem | ScheduleItem[],
  ): Promise<boolean> => {
    try {
      // 단일 스케줄인 경우 배열로 변환
      const scheduleArray = Array.isArray(schedules) ? schedules : [schedules]

      console.log('🚀 스케줄 선택 처리:', scheduleArray.length, '개')

      return await addSelectedSchedules(scheduleArray)
    } catch (error) {
      console.error('❌ 스케줄 선택 처리 중 오류:', error)
      return false
    }
  }
  // 🆕 명시적 초기화 후 추가 함수
  const replaceSelectedSchedules = async (schedules: ScheduleItem[]): Promise<boolean> => {
    try {
      console.log('🔄 선택된 스케줄 목록 교체 시작:', {
        기존개수: selectedScheduleList.value.length,
        새로운개수: schedules.length,
      })

      // 🔧 명시적으로 배열 초기화
      selectedScheduleList.value.splice(0) // 기존 배열 완전 비우기
      selectedSchedule.value = null

      console.log('🗑️ 기존 목록 초기화 완료, 현재 길이:', selectedScheduleList.value.length)

      // 추적 대상 설정
      const success = await setTrackingTargets(schedules)

      if (success) {
        // 🔧 Vue의 반응성을 보장하는 방식으로 추가
        schedules.forEach((schedule) => {
          selectedScheduleList.value.push(schedule)
        })

        // 🔧 또는 한 번에 교체
        // selectedScheduleList.value = [...schedules]

        console.log('✅ 새 스케줄 목록 설정 완료:', {
          설정된개수: selectedScheduleList.value.length,
          목록: selectedScheduleList.value.map((s) => ({
            no: s.no,
            name: s.satelliteName,
          })),
        })

        // 🔧 강제 반응성 트리거 (필요한 경우)
        // nextTick(() => {
        //   console.log('🔄 nextTick 후 selectedScheduleList 길이:', selectedScheduleList.value.length)
        // })

        $q.notify({
          type: 'positive',
          message: `기존 목록을 초기화하고 ${schedules.length}개의 새 스케줄이 추적 대상으로 설정되었습니다`,
        })

        return true
      } else {
        console.error('❌ 추적 대상 설정 실패')
        return false
      }
    } catch (error) {
      console.error('❌ 스케줄 목록 교체 실패:', error)
      return false
    }
  }

  // 🔧 addSelectedSchedules 함수 - 한 번에 처리하도록 개선
  // 🔧 addSelectedSchedules 함수에 초기화 옵션 추가
  const addSelectedSchedules = async (
    schedules: ScheduleItem[],
    clearExisting: boolean = false, // 🆕 기존 목록 초기화 옵션
  ): Promise<boolean> => {
    try {
      if (schedules.length === 0) {
        console.warn('⚠️ 추가할 스케줄이 없음')
        return false
      }

      console.log('🚀 여러 스케줄 선택 처리 시작:', {
        newCount: schedules.length,
        clearExisting,
        currentCount: selectedScheduleList.value.length,
      })

      // 🔧 기존 목록 초기화 (옵션)
      if (clearExisting) {
        console.log('🗑️ 기존 선택된 스케줄 목록 초기화')
        selectedScheduleList.value = []
        selectedSchedule.value = null
      }

      // 🔧 한 번에 모든 스케줄을 추적 대상으로 설정
      const success = await setTrackingTargets(schedules)

      if (success) {
        // 🔧 성공한 경우에만 선택 목록에 추가 (중복 제거)
        schedules.forEach((schedule) => {
          const exists = selectedScheduleList.value.find((item) => item.no === schedule.no)
          if (!exists) {
            selectedScheduleList.value.push(schedule)
            console.log('✅ 스케줄이 선택 목록에 추가됨:', {
              no: schedule.no,
              satelliteName: schedule.satelliteName,
            })
          } else {
            console.log('⚠️ 이미 선택된 스케줄 (건너뜀):', schedule.satelliteName)
          }
        })

        console.log('✅ 모든 스케줄 선택 처리 완료:', {
          requestCount: schedules.length,
          totalSelectedCount: selectedScheduleList.value.length,
          wasCleared: clearExisting,
        })

        $q.notify({
          type: 'positive',
          message: clearExisting
            ? `기존 목록을 초기화하고 ${schedules.length}개의 새 스케줄이 추적 대상으로 설정되었습니다`
            : `${schedules.length}개의 스케줄이 추적 대상으로 설정되었습니다`,
        })

        return true
      } else {
        console.error('❌ 추적 대상 설정 실패')

        $q.notify({
          type: 'negative',
          message: '스케줄을 추적 대상으로 설정하는데 실패했습니다',
        })

        return false
      }
    } catch (error) {
      console.error('❌ 여러 스케줄 선택 처리 실패:', error)

      $q.notify({
        type: 'negative',
        message: '스케줄 선택 처리 중 오류가 발생했습니다',
      })

      return false
    }
  }

  // 🔧 단순 로컬 추가 함수 (API 호출 없이)
  const addSelectedScheduleLocal = (schedule: ScheduleItem) => {
    const exists = selectedScheduleList.value.find((item) => item.no === schedule.no)
    if (!exists) {
      selectedScheduleList.value.push(schedule)
      console.log('✅ 스케줄이 로컬 선택 목록에 추가됨:', schedule.satelliteName)
    } else {
      console.log('⚠️ 이미 선택된 스케줄:', schedule.satelliteName)
    }
  }

  // 🆕 선택된 스케줄을 목록에서 제거
  const removeSelectedSchedule = (scheduleNo: number) => {
    const index = selectedScheduleList.value.findIndex((item) => item.no === scheduleNo)
    if (index >= 0) {
      const removed = selectedScheduleList.value.splice(index, 1)[0]
      console.log('✅ 스케줄이 선택 목록에서 제거됨:', removed?.satelliteName)

      // 현재 선택된 스케줄이 제거된 경우 선택 해제
      if (selectedSchedule.value?.no === scheduleNo) {
        selectedSchedule.value = null
      }
    }
  }

  // 🆕 선택된 스케줄 목록 초기화
  const clearSelectedSchedules = () => {
    selectedScheduleList.value = []
    selectedSchedule.value = null
    console.log('✅ 선택된 스케줄 목록이 초기화됨')
  }

  // 스케줄 선택
  const selectSchedule = (schedule: ScheduleItem) => {
    selectedSchedule.value = schedule
    console.log('✅ 현재 스케줄 선택됨:', schedule.satelliteName)
  }

  // TLE 데이터 추가
  const addTLEData = (tleContent: string) => {
    const newNo =
      tleData.value.length > 0 ? Math.max(...tleData.value.map((item) => item.No)) + 1 : 1

    const newTLE: TLEItem = {
      No: newNo,
      TLE: tleContent,
    }

    tleData.value.push(newTLE)
  }

  // TLE 데이터 삭제
  const removeTLEData = (no: number) => {
    const index = tleData.value.findIndex((item) => item.No === no)

    if (index >= 0) {
      tleData.value.splice(index, 1)

      // 선택된 항목이 삭제된 경우 선택 해제
      if (selectedTLE.value?.No === no) {
        selectedTLE.value = null
      }
    }
  }

  // 모든 TLE 데이터 삭제
  const clearTLEData = () => {
    tleData.value = []
    selectedTLE.value = null
  }

  // TLE 선택
  const selectTLE = (tle: TLEItem) => {
    selectedTLE.value = tle
  }

  // TLE 데이터 내보내기
  const exportTLEData = (): string => {
    if (tleData.value.length === 0) {
      return ''
    }

    return tleData.value.map((item) => item.TLE).join('\n\n') + '\n'
  }

  // 🆕 서버에서 TLE 데이터 로드 (정교한 버전)
  // 🆕 서버에서 TLE 데이터 로드 (순서 보장 및 위성명 처리 개선)
  const loadTLEDataFromServer = async (): Promise<boolean> => {
    try {
      loading.value = true
      console.log('🔄 서버에서 TLE 데이터 로드 시작')

      const response = await passScheduleService.getAllTLEs()

      if (response.success && response.data) {
        // 🔧 서버 데이터를 순서대로 처리하여 로컬 형식으로 변환
        const serverTLEs = response.data.tleList

        console.log('🔍 서버에서 받은 TLE 데이터:', serverTLEs.length, '개')

        tleData.value = serverTLEs.map((item, index) => {
          console.log(`🔍 TLE ${index + 1} 처리:`, {
            satelliteId: item.satelliteId,
            satelliteName: item.satelliteName,
          })

          // 🔧 위성 이름 결정 로직 개선
          let tleContent = ''

          if (
            item.satelliteName &&
            item.satelliteName.trim() !== '' &&
            item.satelliteName !== `Satellite-${item.satelliteId}`
          ) {
            // 실제 위성 이름이 있는 경우 - 3줄 형식
            tleContent = `${item.satelliteName}\n${item.tleLine1}\n${item.tleLine2}`
            console.log(`✅ 3줄 형식으로 구성: "${item.satelliteName}"`)
          } else {
            // 위성 이름이 없거나 기본 형식인 경우 - 2줄 형식
            tleContent = `${item.tleLine1}\n${item.tleLine2}`
            console.log(`✅ 2줄 형식으로 구성: "${item.satelliteId}"`)
          }

          return {
            No: index + 1, // 🔧 순서 보장
            TLE: tleContent,
          }
        })

        console.log('✅ 서버 TLE 데이터 로드 완료:', tleData.value.length, '개')

        // 🔧 변환 결과 확인
        tleData.value.forEach((item, index) => {
          console.log(`\n=== 로드된 TLE ${index + 1} ===`)
          console.log('No:', item.No)
          console.log('TLE:')
          console.log(item.TLE)
          console.log('추출된 이름:', getTLEName(item.TLE))
        })

        return true
      } else {
        console.warn('⚠️ 서버 TLE 데이터 없음')
        return false
      }
    } catch (err) {
      console.error('❌ 서버 TLE 데이터 로드 실패:', err)
      error.value = 'TLE 데이터 로드 실패'
      return false
    } finally {
      loading.value = false
    }
  }
  // TLE 이름 추출 (개선된 버전)
  const getTLEName = (tleContent: string): string => {
    if (!tleContent) return ''

    const lines = tleContent.split('\n').filter((line) => line.trim())
    if (lines.length === 0) return ''

    // 🔧 3줄 형식인 경우 (위성명 + Line1 + Line2)
    if (
      lines.length >= 3 &&
      !lines[0]?.startsWith('1 ') &&
      !lines[0]?.startsWith('2 ') &&
      lines[1]?.startsWith('1 ') &&
      lines[2]?.startsWith('2 ')
    ) {
      const satelliteName = lines[0]?.trim() || ''
      console.log(`🔍 위성명 추출 (3줄): "${satelliteName}"`)
      return satelliteName
    }

    // 🔧 2줄 형식인 경우 - TLE Line1에서 위성 ID 추출
    const line1 = lines.find((line) => line.startsWith('1 '))
    if (line1) {
      const satelliteId = line1.substring(2, 7).trim()
      console.log(`🔍 위성 ID 추출 (2줄): "${satelliteId}"`)
      return `Satellite ${satelliteId}`
    }

    return ''
  }

  // 🔧 위성 정보 추출 함수 (Store용)
  const extractSatelliteInfo = (tleContent: string): { id: string; name?: string } => {
    if (!tleContent) return { id: '' }

    const lines = tleContent.split('\n').filter((line) => line.trim())

    console.log('🔍 위성 정보 추출:', lines.length, '라인')

    // 3줄 형식인 경우 (위성명 + Line1 + Line2)
    if (
      lines.length >= 3 &&
      !lines[0]?.startsWith('1 ') &&
      !lines[0]?.startsWith('2 ') &&
      lines[1]?.startsWith('1 ') &&
      lines[2]?.startsWith('2 ')
    ) {
      const satelliteName = lines[0]?.trim() || ''
      const line1 = lines[1]?.trim() || ''
      const satelliteId = line1.substring(2, 7).trim()

      console.log(`✅ 3줄 형식 - 이름: "${satelliteName}", ID: "${satelliteId}"`)
      return { id: satelliteId, name: satelliteName }
    }

    // 2줄 형식인 경우 (Line1 + Line2)
    const line1 = lines.find((line) => line.startsWith('1 '))
    if (line1) {
      const satelliteId = line1.substring(2, 7).trim()
      console.log(`✅ 2줄 형식 - ID: "${satelliteId}"`)
      return { id: satelliteId }
    }

    return { id: '' }
  }

  // 🔧 TLE 라인 추출 함수들
  const getTLELine1 = (tleContent: string): string => {
    const lines = tleContent.split('\n').filter((line) => line.trim())
    const line1 = lines.find((line) => line.startsWith('1 '))
    return line1?.trim() || ''
  }

  const getTLELine2 = (tleContent: string): string => {
    const lines = tleContent.split('\n').filter((line) => line.trim())
    const line2 = lines.find((line) => line.startsWith('2 '))
    return line2?.trim() || ''
  }

  // 🆕 TLE 데이터를 서버에 업로드하고 추적 데이터 생성 (디버깅 강화)
  const uploadTLEDataToServer = async (
    tleItems: TLEItem[],
    callbacks?: ProgressCallback,
  ): Promise<TLEUploadResult> => {
    console.log('🚀 TLE 데이터 서버 업로드 시작:', tleItems.length, '개')

    isUploading.value = true
    uploadProgress.value = 0
    uploadStatus.value = '업로드 준비 중...'

    const startTime = Date.now()
    let successCount = 0
    let failedCount = 0
    let totalPasses = 0
    let totalTrackingPoints = 0
    const completedSatellites: string[] = []
    const failedSatellites: string[] = []
    const results: TLEUploadResult['results'] = []

    try {
      // 🔧 순서를 보장하기 위해 for 루프 사용 (forEach 대신)
      for (let i = 0; i < tleItems.length; i++) {
        const item = tleItems[i]

        // 🔧 undefined 체크 추가
        if (!item) {
          console.warn(`⚠️ [${i + 1}] TLE 아이템이 undefined`)
          continue
        }

        console.log(`\n🔄 [${i + 1}/${tleItems.length}] TLE 아이템 처리`)
        console.log('TLE 내용 미리보기:', item.TLE.substring(0, 50) + '...')

        // 🔧 위성 정보 추출
        const satelliteInfo = extractSatelliteInfo(item.TLE)
        const satelliteIdForRequest = satelliteInfo.name || satelliteInfo.id || `Unknown-${i + 1}`

        console.log('🛰️ 처리할 위성:', {
          id: satelliteInfo.id,
          name: satelliteInfo.name,
          requestId: satelliteIdForRequest,
        })

        try {
          // 진행률 업데이트
          uploadProgress.value = i / tleItems.length
          uploadStatus.value = `${satelliteIdForRequest} 처리 중...`

          callbacks?.onProgress?.(i, tleItems.length, satelliteIdForRequest)

          // 🔧 API 요청 데이터 구성
          const requestData: AddTleAndTrackingRequest = {
            satelliteId: satelliteInfo.id,
            tleLine1: getTLELine1(item.TLE),
            tleLine2: getTLELine2(item.TLE),
          }

          // 위성 이름이 있는 경우 추가
          if (satelliteInfo.name) {
            requestData.satelliteName = satelliteInfo.name
          }

          console.log('📡 API 요청 데이터:', requestData)

          const response = await passScheduleService.addTleAndGenerateTracking(requestData)

          if (response.success && response.data) {
            successCount++
            totalPasses += response.data.passCount || 0
            totalTrackingPoints += response.data.trackingPointCount || 0
            completedSatellites.push(satelliteIdForRequest)

            results.push({
              satelliteId: satelliteIdForRequest,
              success: true,
              passCount: response.data.passCount,
              trackingPointCount: response.data.trackingPointCount,
            })

            console.log(`✅ [${i + 1}] ${satelliteIdForRequest} 성공:`, {
              passCount: response.data.passCount,
              trackingPointCount: response.data.trackingPointCount,
            })

            callbacks?.onSuccess?.(satelliteIdForRequest, response)
          } else {
            throw new Error(response.message || '서버에서 실패 응답')
          }
        } catch (error) {
          failedCount++
          failedSatellites.push(satelliteIdForRequest)

          let errorMessage = '알 수 없는 오류'
          if (error instanceof Error) {
            errorMessage = error.message
          }

          results.push({
            satelliteId: satelliteIdForRequest,
            success: false,
            error: errorMessage,
          })

          console.error(`❌ [${i + 1}] ${satelliteIdForRequest} 실패:`, errorMessage)
          callbacks?.onError?.(satelliteIdForRequest, errorMessage)
        }
      }

      // 최종 진행률 업데이트
      uploadProgress.value = 1
      uploadStatus.value = '업로드 완료'

      const processingTime = Math.round((Date.now() - startTime) / 1000)
      const uploadResult: TLEUploadResult = {
        success: failedCount === 0,
        successCount,
        failedCount,
        totalCount: tleItems.length,
        totalPasses,
        totalTrackingPoints,
        processingTime,
        completedSatellites,
        failedSatellites,
        results,
      }

      console.log('🎉 TLE 업로드 완료:', uploadResult)
      callbacks?.onComplete?.(uploadResult)

      // 🔧 업로드 성공 후 서버에서 최신 데이터 다시 로드
      if (successCount > 0) {
        console.log('🔄 업로드 완료 후 서버 데이터 재로드')
        await loadTLEDataFromServer()
      }

      return uploadResult
    } catch (error) {
      console.error('❌ TLE 업로드 중 전체 오류:', error)

      const processingTime = Math.round((Date.now() - startTime) / 1000)
      const uploadResult: TLEUploadResult = {
        success: false,
        successCount,
        failedCount: tleItems.length - successCount,
        totalCount: tleItems.length,
        totalPasses,
        totalTrackingPoints,
        processingTime,
        completedSatellites,
        failedSatellites,
        results,
      }

      callbacks?.onComplete?.(uploadResult)
      return uploadResult
    } finally {
      isUploading.value = false
      uploadProgress.value = 0
      uploadStatus.value = ''
    }
  }

  // 🔧 사용하지 않는 함수 제거하고 실제 데이터 처리 로직 수정
  const fetchScheduleDataFromServer = async (): Promise<boolean> => {
    try {
      loading.value = true
      error.value = null

      console.log('🔄 서버에서 패스 스케줄 데이터 로드 시작')

      const response = await passScheduleService.getAllTrackingMasterData()

      console.log('🔍 Store에서 받은 응답:', {
        success: response.success,
        message: response.message,
        hasData: !!response.data,
      })

      if (response.success && response.data) {
        const serverData = response.data

        console.log('📊 서버 데이터 상세:', {
          satelliteCount: serverData.satelliteCount,
          totalPassCount: serverData.totalPassCount,
          hasSatellites: !!serverData.satellites,
          satellitesType: typeof serverData.satellites,
        })

        // 🔧 satellites 안전 검증
        if (!serverData.satellites || typeof serverData.satellites !== 'object') {
          console.warn('⚠️ satellites 데이터가 없거나 올바르지 않음')
          scheduleData.value = []

          $q.notify({
            type: 'info',
            message: '위성 데이터가 없습니다. TLE 데이터를 먼저 업로드해주세요.',
          })

          return false
        }

        // 🔧 빈 객체 확인
        const satelliteKeys = Object.keys(serverData.satellites)
        if (satelliteKeys.length === 0) {
          console.warn('⚠️ satellites 객체가 비어있음')
          scheduleData.value = []

          $q.notify({
            type: 'info',
            message: '등록된 위성이 없습니다.',
          })

          return false
        }

        console.log('✅ satellites 검증 통과:', satelliteKeys)

        const allSchedules: ScheduleItem[] = []

        // 🔧 직접 Object.entries 사용 (안전 검증 후)
        Object.entries(serverData.satellites).forEach(([satelliteId, passes]) => {
          console.log(`🛰️ 위성 ${satelliteId} 처리:`, {
            isArray: Array.isArray(passes),
            passCount: Array.isArray(passes) ? passes.length : 'Not Array',
          })

          if (!Array.isArray(passes)) {
            console.warn(`⚠️ 위성 ${satelliteId}의 패스 데이터가 배열이 아님`)
            return
          }

          passes.forEach((pass: PassScheduleMasterData) => {
            try {
              const scheduleItem: ScheduleItem = {
                no: pass.No,
                satelliteId: pass.SatelliteID || satelliteId,
                satelliteName: pass.SatelliteName || satelliteId,
                startTime: pass.StartTime || '',
                endTime: pass.EndTime || '',
                duration: pass.Duration || '00:00:00',
                startAzimuthAngle: pass.StartAzimuth || 0,
                endAzimuthAngle: pass.EndAzimuth || 0,
                startElevationAngle: pass.StartElevation || 0,
                endElevationAngle: pass.EndElevation || 0,
                tilt: 0,
                maxElevation: pass.MaxElevation || 0,
                maxElevationTime: pass.MaxElevationTime || '',
                maxAzimuthRate: pass.MaxAzRate || 0,
                maxElevationRate: pass.MaxElRate || 0,
                maxAzimuthAccel: pass.MaxAzAccel || 0,
                maxElevationAccel: pass.MaxElAccel || 0,
                originalStartAzimuth: pass.OriginalStartAzimuth || 0,
                originalEndAzimuth: pass.OriginalEndAzimuth || 0,
              }

              allSchedules.push(scheduleItem)
              console.log(
                `✅ 스케줄 생성: ${scheduleItem.satelliteName} - ${scheduleItem.startTime}`,
              )
            } catch (itemError) {
              console.error(`❌ 스케줄 아이템 생성 실패:`, itemError)
            }
          })
        })

        if (allSchedules.length === 0) {
          console.warn('⚠️ 생성된 스케줄이 없음')
          scheduleData.value = []

          $q.notify({
            type: 'info',
            message: '유효한 패스 스케줄이 없습니다.',
          })

          return false
        }

        // 시간 순 정렬
        allSchedules.sort((a, b) => {
          try {
            return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
          } catch {
            return 0
          }
        })

        scheduleData.value = allSchedules

        console.log('✅ 패스 스케줄 데이터 로드 완료:', allSchedules.length, '개')

        $q.notify({
          type: 'positive',
          message: `${allSchedules.length}개의 패스 스케줄을 로드했습니다.`,
        })

        return true
      } else {
        console.warn('⚠️ 서버 응답 실패:', response)
        scheduleData.value = []

        $q.notify({
          type: 'info',
          message: response.message || '추적 데이터가 없습니다.',
        })

        return false
      }
    } catch (err) {
      console.error('❌ 서버 패스 스케줄 데이터 로드 실패:', err)
      scheduleData.value = []
      error.value = 'Failed to fetch schedule data from server'

      $q.notify({
        type: 'negative',
        message: '패스 스케줄 데이터 로드에 실패했습니다',
      })

      return false
    } finally {
      loading.value = false
    }
  }

  // 🔧 초기화 함수 수정 (fetchScheduleData 대신 fetchScheduleDataFromServer 직접 호출)
  const init = async () => {
    console.log('🚀 PassScheduleStore 초기화 시작')

    try {
      // 🔧 서버에서 직접 데이터 로드
      const scheduleResult = await fetchScheduleDataFromServer()
      const tleResult = await loadTLEDataFromServer()

      console.log('✅ PassScheduleStore 초기화 완료:', {
        scheduleLoaded: scheduleResult,
        tleLoaded: tleResult,
        scheduleCount: scheduleData.value.length,
        tleCount: tleData.value.length,
      })

      return { scheduleResult, tleResult }
    } catch (error) {
      console.error('❌ PassScheduleStore 초기화 실패:', error)
      throw error
    }
  }

  // 추적 대상 설정 함수 추가
  // 🔧 간단한 버전 - 기본값 보장
  const setTrackingTargets = async (schedules: ScheduleItem[]): Promise<boolean> => {
    try {
      loading.value = true
      console.log('🚀 추적 대상 설정 시작:', schedules.length, '개')

      const trackingTargets: TrackingTarget[] = schedules.map((schedule, arrayIndex) => {
        // 🔧 안전한 mstId 결정 - 항상 유효한 number 반환
        const mstId = schedule.index || schedule.no || arrayIndex + 1

        console.log(
          `🔍 스케줄 ${arrayIndex}: mstId=${mstId}, index=${schedule.index}, no=${schedule.no}`,
        )

        return {
          mstId: Number(mstId), // 🔧 명시적 number 변환
          no: schedule.no,
          satelliteId: schedule.satelliteId || '',
          satelliteName: schedule.satelliteName,
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          maxElevation: schedule.maxElevation || 0,
        }
      })

      console.log(
        '🔄 변환된 추적 대상:',
        trackingTargets.map((t) => ({
          mstId: t.mstId,
          no: t.no,
          satelliteId: t.satelliteId,
          satelliteName: t.satelliteName,
        })),
      )

      const request: SetTrackingTargetsRequest = {
        targets: trackingTargets,
      }

      const response = await passScheduleService.setTrackingTargets(request)

      if (response.success) {
        console.log('✅ 추적 대상 설정 성공:', response.data)

        $q.notify({
          type: 'positive',
          message: `${response.data?.totalTargets || trackingTargets.length}개의 추적 대상이 설정되었습니다`,
        })

        return true
      } else {
        console.error('❌ 추적 대상 설정 실패:', response.message)

        $q.notify({
          type: 'negative',
          message: response.message || '추적 대상 설정에 실패했습니다',
        })

        return false
      }
    } catch (error) {
      console.error('❌ 추적 대상 설정 중 오류:', error)

      $q.notify({
        type: 'negative',
        message: '추적 대상 설정 중 오류가 발생했습니다',
      })

      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 전체 추적 데이터 삭제
   */
  const deleteAllTrackingData = async (): Promise<boolean> => {
    try {
      loading.value = true
      console.log('🗑️ 전체 추적 데이터 삭제 시작')

      const response = await passScheduleService.deleteAllTrackingData()

      if (response.success) {
        console.log('✅ 전체 추적 데이터 삭제 성공:', response.data)

        // 로컬 데이터도 초기화
        scheduleData.value = []
        selectedScheduleList.value = []
        selectedSchedule.value = null

        $q.notify({
          type: 'positive',
          message: `전체 추적 데이터가 삭제되었습니다. (${response.data?.deletedSatelliteCount || 0}개 위성, ${response.data?.deletedPassCount || 0}개 패스)`,
          timeout: 3000,
        })

        return true
      } else {
        console.error('❌ 전체 추적 데이터 삭제 실패:', response.message)

        $q.notify({
          type: 'negative',
          message: response.message || '전체 추적 데이터 삭제에 실패했습니다',
        })

        return false
      }
    } catch (error) {
      console.error('❌ 전체 추적 데이터 삭제 중 오류:', error)

      $q.notify({
        type: 'negative',
        message: '전체 추적 데이터 삭제 중 오류가 발생했습니다',
      })

      return false
    } finally {
      loading.value = false
    }
  }

  // 🆕 추적 경로 세부 데이터 조회 (개선된 버전)
  async function loadTrackingDetailData(satelliteId: string, passId: number): Promise<boolean> {
    try {
      // 이미 같은 데이터가 로드되어 있는지 확인
      if (
        currentTrackingPathInfo.value.satelliteId === satelliteId &&
        currentTrackingPathInfo.value.passId === passId &&
        predictedTrackingPath.value.length > 0
      ) {
        console.log('✅ 이미 로드된 추적 경로 데이터 사용')
        return true
      }

      trackingPathLoading.value = true
      console.log(`📡 Store: 추적 경로 세부 데이터 조회 - 위성: ${satelliteId}, 패스: ${passId}`)

      // 🔧 새로운 API 사용
      const response = await passScheduleService.getTrackingDetailByPass(satelliteId, passId)

      if (response.success && response.data?.trackingPoints) {
        const trackingPoints = response.data.trackingPoints

        // 원본 상세 데이터 저장
        trackingDetailData.value = trackingPoints

        // 차트용 좌표 데이터 변환 (서비스의 변환 함수 사용)
        const chartData = passScheduleService.convertToChartData(trackingPoints)
        predictedTrackingPath.value = chartData

        // 추적 경로 정보 업데이트
        currentTrackingPathInfo.value = {
          satelliteId,
          passId,
          pointCount: trackingPoints.length,
          lastUpdated: Date.now(),
        }

        console.log(`✅ Store: 추적 경로 데이터 로드 완료:`, {
          rawPointCount: trackingPoints.length,
          chartPointCount: chartData.length,
          satelliteId,
          passId,
        })

        $q.notify({
          type: 'positive',
          message: '추적 경로를 로드했습니다',
          caption: `${trackingPoints.length}개 포인트`,
        })

        return true
      } else {
        console.warn('❌ Store: 추적 경로 데이터 조회 실패:', response.message)

        // 데이터 초기화
        clearTrackingPaths()

        $q.notify({
          type: 'warning',
          message: '추적 경로 데이터가 없습니다',
          caption: response.message,
        })

        return false
      }
    } catch (error) {
      console.error('❌ Store: 추적 경로 데이터 조회 중 오류:', error)

      // 오류 시 데이터 초기화
      clearTrackingPaths()

      $q.notify({
        type: 'negative',
        message: '추적 경로 로드에 실패했습니다',
        caption: error instanceof Error ? error.message : '알 수 없는 오류',
      })

      return false
    } finally {
      trackingPathLoading.value = false
    }
  }

  // 🆕 예상 경로 설정
  function setPredictedTrackingPath(path: [number, number][]) {
    predictedTrackingPath.value = [...path]
    console.log(`📍 Store: 예상 경로 설정 완료 - ${path.length}개 포인트`)
  }

  // 🆕 추적 경로 데이터 정리
  const clearTrackingPaths = () => {
    actualTrackingPath.value = []
    predictedTrackingPath.value = []
    trackingDetailData.value = []
    currentTrackingPosition.value = { azimuth: 0, elevation: 0 }
    currentTrackingPathInfo.value = {
      satelliteId: null,
      passId: null,
      pointCount: 0,
      lastUpdated: null,
    }
    console.log('✅ PassSchedule 추적 경로 데이터 정리 완료')
  }

  // 🆕 현재 위치 업데이트
  function updateCurrentPosition(azimuth: number, elevation: number) {
    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))
    currentTrackingPosition.value = { azimuth: normalizedAz, elevation: normalizedEl }
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

  // ===== Worker-related 메서드들 (EphemerisTrackStore와 동일한 성능 최적화) =====

  /**
   * 🆕 인라인 Worker 생성 (PassSchedule용)
   */
  const createPassScheduleWorker = (): Worker => {
    const workerScript = `
      self.onmessage = function(e) {
        const startTime = performance.now()

        try {
          const { azimuth, elevation, currentPath, maxPoints, threshold } = e.data

          // 입력 데이터 검증
          if (typeof azimuth !== 'number' || isNaN(azimuth) || !isFinite(azimuth)) {
            throw new Error('Invalid azimuth value: ' + azimuth)
          }

          if (typeof elevation !== 'number' || isNaN(elevation) || !isFinite(elevation)) {
            throw new Error('Invalid elevation value: ' + elevation)
          }

          if (!Array.isArray(currentPath)) {
            throw new Error('currentPath is not an array: ' + typeof currentPath)
          }

          // 배열 데이터 정제
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
            const distance = Math.sqrt(
              Math.pow(newPoint[0] - lastPoint[0], 2) +
              Math.pow(newPoint[1] - lastPoint[1], 2)
            )

            if (distance < (threshold || 0.1)) {
              // 중복 포인트는 추가하지 않음
              const endTime = performance.now()
              self.postMessage({
                success: true,
                updatedPath: updatedPath,
                pointsAdded: 0,
                processingTime: endTime - startTime,
                pathLength: updatedPath.length,
                reason: 'duplicate_point_skipped'
              })
              return
            }
          }

          // 새 포인트 추가
          updatedPath.push(newPoint)

          // 🎯 최대 포인트 수 제한 - maxPoints가 0이면 제한 없음
          if (maxPoints > 0 && updatedPath.length > maxPoints) {
            const removeCount = updatedPath.length - maxPoints
            updatedPath.splice(0, removeCount)
          }

          const endTime = performance.now()

          self.postMessage({
            success: true,
            updatedPath: updatedPath,
            pointsAdded: 1,
            processingTime: endTime - startTime,
            pathLength: updatedPath.length,
            reason: 'point_added'
          })

        } catch (error) {
          const endTime = performance.now()
          self.postMessage({
            success: false,
            error: error.message || 'Unknown worker error',
            processingTime: endTime - startTime,
            pathLength: 0
          })
        }
      }
    `

    const blob = new Blob([workerScript], { type: 'application/javascript' })
    const workerUrl = URL.createObjectURL(blob)

    try {
      const worker = new Worker(workerUrl)
      console.log('✅ PassSchedule 인라인 Worker 생성 성공')
      return worker
    } finally {
      URL.revokeObjectURL(workerUrl)
    }
  }

  /**
   * 🆕 Worker 초기화
   */
  const initializePassScheduleWorker = (): boolean => {
    try {
      if (passScheduleWorker) {
        passScheduleWorker.terminate()
      }

      passScheduleWorker = createPassScheduleWorker()
      workerInitialized = true

      console.log('✅ PassSchedule Worker 초기화 완료')
      return true
    } catch (error) {
      console.error('❌ PassSchedule Worker 초기화 실패:', error)
      workerInitialized = false
      return false
    }
  }

  /**
   * 🆕 Worker 정리
   */
  const cleanupPassScheduleWorker = () => {
    if (passScheduleWorker) {
      passScheduleWorker.terminate()
      passScheduleWorker = null
    }
    workerInitialized = false
    console.log('✅ PassSchedule Worker 정리 완료')
  }

  // ===== 누락된 함수들 추가 =====

  /**
   * 🆕 추적 모니터링 정보
   */
  const trackingMonitorInfo = computed(() => ({
    isActive: isTrackingMonitorActive.value,
    status: trackingMonitorStatus.value,
    scheduleCount: selectedScheduleList.value.length,
    currentPosition: currentTrackingPosition.value,
    pathLength: actualTrackingPath.value.length,
  }))

  /**
   * 🆕 시간 오프셋 전송
   */
  const sendTimeOffset = async (timeOffset: number): Promise<boolean> => {
    try {
      console.log('⏰ 시간 오프셋 전송:', timeOffset)
      // TODO: 실제 시간 오프셋 API 호출 구현
      await new Promise((resolve) => setTimeout(resolve, 100)) // 임시 대기
      return true
    } catch (error) {
      console.error('❌ 시간 오프셋 전송 실패:', error)
      return false
    }
  }

  /**
   * 🆕 추적 모니터링 시작
   */
  const startTrackingMonitor = async (): Promise<boolean> => {
    try {
      if (isTrackingMonitorActive.value) {
        console.log('⚠️ 추적 모니터링이 이미 활성화되어 있습니다')
        return true
      }

      console.log('🚀 추적 모니터링 시작')
      isTrackingMonitorActive.value = true
      trackingMonitorStatus.value = {
        monitoringInterval: '100ms',
        timeReference: 'UTC',
        threadName: 'PassScheduleMonitor',
        startedAt: Date.now(),
        uptime: 0,
      }

      // Worker 초기화
      await new Promise((resolve) => setTimeout(resolve, 10)) // 임시 대기
      initializePassScheduleWorker()

      return true
    } catch (error) {
      console.error('❌ 추적 모니터링 시작 실패:', error)
      isTrackingMonitorActive.value = false
      trackingMonitorStatus.value = {
        monitoringInterval: 'error',
        timeReference: 'UTC',
        threadName: 'PassScheduleMonitor',
        startedAt: undefined,
        uptime: 0,
      }
      return false
    }
  }

  /**
   * 🆕 추적 모니터링 중지
   */
  const stopTrackingMonitor = async (): Promise<boolean> => {
    try {
      if (!isTrackingMonitorActive.value) {
        console.log('⚠️ 추적 모니터링이 이미 비활성화되어 있습니다')
        return true
      }

      console.log('🛑 추적 모니터링 중지')
      isTrackingMonitorActive.value = false
      trackingMonitorStatus.value = {
        monitoringInterval: 'stopped',
        timeReference: 'UTC',
        threadName: 'PassScheduleMonitor',
        startedAt: undefined,
        uptime: 0,
      }

      // Worker 정리
      await new Promise((resolve) => setTimeout(resolve, 10)) // 임시 대기
      cleanupPassScheduleWorker()

      return true
    } catch (error) {
      console.error('❌ 추적 모니터링 중지 실패:', error)
      return false
    }
  }

  /**
   * 🆕 추적 모니터링 토글
   */
  const toggleTrackingMonitor = async (): Promise<boolean> => {
    if (isTrackingMonitorActive.value) {
      return await stopTrackingMonitor()
    } else {
      return await startTrackingMonitor()
    }
  }

  /**
   * 🆕 추적 모니터링 상태 조회
   */
  const getTrackingMonitorStatus = () => {
    return {
      isActive: isTrackingMonitorActive.value,
      status: trackingMonitorStatus.value,
      scheduleCount: selectedScheduleList.value.length,
      currentPosition: currentTrackingPosition.value,
      pathLength: actualTrackingPath.value.length,
    }
  }

  /**
   * 🆕 추적 모니터링 재시작
   */
  const restartTrackingMonitor = async (): Promise<boolean> => {
    try {
      console.log('🔄 추적 모니터링 재시작')
      await stopTrackingMonitor()
      await new Promise((resolve) => setTimeout(resolve, 100)) // 잠시 대기
      return await startTrackingMonitor()
    } catch (error) {
      console.error('❌ 추적 모니터링 재시작 실패:', error)
      return false
    }
  }

  return {
    // 상태
    scheduleData, // 전체 스케줄 (모달용)
    selectedScheduleList, // 🆕 선택된 스케줄 목록 (테이블용)
    selectedSchedule,
    loading,
    error,

    // TLE 상태
    tleData,
    selectedTLE,

    // 업로드 상태
    isUploading,
    uploadProgress,
    uploadStatus,

    offsetValues: readonly(offsetValues),

    // 🆕 추적 모니터링 상태
    isTrackingMonitorActive: readonly(isTrackingMonitorActive),
    trackingMonitorStatus: readonly(trackingMonitorStatus),
    trackingMonitorInfo,

    // 🆕 추적 경로 상태
    trackingDetailData: readonly(trackingDetailData),
    predictedTrackingPath: readonly(predictedTrackingPath),
    actualTrackingPath: readonly(actualTrackingPath),
    currentTrackingPosition: readonly(currentTrackingPosition),
    trackingPathLoading: readonly(trackingPathLoading),
    currentTrackingPathInfo: readonly(currentTrackingPathInfo),

    // 액션
    fetchScheduleData,
    fetchScheduleDataFromServer,
    selectSchedule,
    addSelectedSchedule, // 🔧 API 호출 포함
    addSelectedSchedules, // 🔧 새로 추가
    addSelectedScheduleLocal, // 🔧 로컬만 (기존 로
    addSchedulesToSelection,
    replaceSelectedSchedules,
    sendTimeOffset,
    // 🆕 선택된 스케줄 관리 액션
    removeSelectedSchedule,
    clearSelectedSchedules,

    // TLE 액션
    addTLEData,
    removeTLEData,
    clearTLEData,
    selectTLE,
    exportTLEData,

    // 서버 연동 액션
    loadTLEDataFromServer,
    uploadTLEDataToServer,

    init,
    setTrackingTargets,
    deleteAllTrackingData, // 🆕 추가

    // 🆕 추적 모니터링 액션들
    startTrackingMonitor,
    stopTrackingMonitor,
    toggleTrackingMonitor,
    getTrackingMonitorStatus,
    restartTrackingMonitor,

    // 🆕 추적 경로 액션들
    loadTrackingDetailData,
    setPredictedTrackingPath,
    updateActualTrackingPath,
    clearTrackingPaths,
    updateCurrentPosition,
    updateOffsetValues,

    // 🆕 Worker 관련 메서드들
    initializePassScheduleWorker,
    cleanupPassScheduleWorker,

    // 🆕 computed 속성들
    trackingPath,
  }
})
