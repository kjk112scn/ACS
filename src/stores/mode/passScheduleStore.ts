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

  // 🆕 추적 모니터링 시작 - 타입 안전성 개선
  const startTrackingMonitor = async (): Promise<boolean> => {
    try {
      loading.value = true
      console.log('🚀 Store: 추적 모니터링 시작')

      const response = await passScheduleService.startScheduleTracking()

      if (response.success) {
        isTrackingMonitorActive.value = true

        // 🔧 타입 안전한 할당
        trackingMonitorStatus.value = {
          monitoringInterval: response.data?.monitoringInterval || undefined,
          timeReference: response.data?.timeReference || undefined,
          threadName: response.data?.threadName || undefined,
          startedAt: Date.now(),
          uptime: undefined,
        }

        $q.notify({
          type: 'positive',
          message: '추적 모니터링이 시작되었습니다',
          caption: `주기: ${response.data?.monitoringInterval || '100ms'}`,
        })

        console.log('✅ Store: 추적 모니터링 시작 성공')
        return true
      } else {
        throw new Error(response.message || '추적 모니터링 시작 실패')
      }
    } catch (error) {
      console.error('❌ Store: 추적 모니터링 시작 실패:', error)

      $q.notify({
        type: 'negative',
        message: '추적 모니터링 시작에 실패했습니다',
        caption: error instanceof Error ? error.message : '알 수 없는 오류',
      })

      return false
    } finally {
      loading.value = false
    }
  }

  // 🆕 추적 모니터링 중지
  const stopTrackingMonitor = async (): Promise<boolean> => {
    try {
      loading.value = true
      console.log('🛑 Store: 추적 모니터링 중지')

      const response = await passScheduleService.stopScheduleTracking()

      if (response.success) {
        isTrackingMonitorActive.value = false
        trackingMonitorStatus.value = {}

        $q.notify({
          type: 'positive',
          message: '추적 모니터링이 중지되었습니다',
          caption: '리소스가 정리되었습니다',
        })

        console.log('✅ Store: 추적 모니터링 중지 성공')
        return true
      } else {
        throw new Error(response.message || '추적 모니터링 중지 실패')
      }
    } catch (error) {
      console.error('❌ Store: 추적 모니터링 중지 실패:', error)

      $q.notify({
        type: 'negative',
        message: '추적 모니터링 중지에 실패했습니다',
        caption: error instanceof Error ? error.message : '알 수 없는 오류',
      })

      return false
    } finally {
      loading.value = false
    }
  }

  // 🆕 추적 모니터링 토글
  const toggleTrackingMonitor = async (): Promise<boolean> => {
    if (isTrackingMonitorActive.value) {
      return await stopTrackingMonitor()
    } else {
      return await startTrackingMonitor()
    }
  }

  // 🆕 추적 모니터링 상태 조회 - 타입 안전성 개선
  const getTrackingMonitorStatus = async (): Promise<boolean> => {
    try {
      console.log('📊 Store: 추적 모니터링 상태 조회')

      const response = await passScheduleService.getTrackingMonitorStatus()

      if (response.success && response.data) {
        isTrackingMonitorActive.value = response.data.isRunning || false

        // 🔧 타입 안전한 할당
        trackingMonitorStatus.value = {
          monitoringInterval: response.data.monitoringInterval || undefined,
          timeReference: response.data.timeReference || undefined,
          threadName: response.data.threadName || undefined,
          startedAt: response.data.startedAt || undefined,
          uptime: response.data.uptime || undefined,
        }

        console.log('✅ Store: 추적 모니터링 상태 조회 성공')
        return true
      } else {
        throw new Error(response.message || '상태 조회 실패')
      }
    } catch (error) {
      console.error('❌ Store: 추적 모니터링 상태 조회 실패:', error)

      // 에러 시 기본값으로 설정
      isTrackingMonitorActive.value = false
      trackingMonitorStatus.value = {}

      return false
    }
  }

  // 🆕 추적 모니터링 재시작
  const restartTrackingMonitor = async (): Promise<boolean> => {
    try {
      console.log('🔄 Store: 추적 모니터링 재시작 시작')

      // 1. 중지
      const stopSuccess = await stopTrackingMonitor()
      if (!stopSuccess) {
        throw new Error('중지 단계에서 실패')
      }

      // 2. 잠시 대기 (리소스 정리)
      await new Promise((resolve) => setTimeout(resolve, 1000))

      // 3. 시작
      const startSuccess = await startTrackingMonitor()
      if (!startSuccess) {
        throw new Error('시작 단계에서 실패')
      }

      $q.notify({
        type: 'positive',
        message: '추적 모니터링이 재시작되었습니다',
        caption: '시스템이 초기화되었습니다',
      })

      console.log('✅ Store: 추적 모니터링 재시작 성공')
      return true
    } catch (error) {
      console.error('❌ Store: 추적 모니터링 재시작 실패:', error)

      $q.notify({
        type: 'negative',
        message: '추적 모니터링 재시작에 실패했습니다',
        caption: error instanceof Error ? error.message : '알 수 없는 오류',
      })

      return false
    }
  }

  // 🆕 computed - 추적 모니터링 정보
  const trackingMonitorInfo = computed(() => {
    const startedAt = trackingMonitorStatus.value.startedAt
    const uptime = startedAt ? Date.now() - startedAt : 0

    return {
      isActive: isTrackingMonitorActive.value,
      status: trackingMonitorStatus.value,
      uptime,
      formattedUptime: formatDuration(uptime),
    }
  })

  // 🆕 유틸리티 함수 - 시간 포맷팅
  const formatDuration = (ms: number): string => {
    const seconds = Math.floor(ms / 1000)
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const remainingSeconds = seconds % 60

    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

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

    // 🆕 추적 모니터링 상태
    isTrackingMonitorActive: readonly(isTrackingMonitorActive),
    trackingMonitorStatus: readonly(trackingMonitorStatus),
    trackingMonitorInfo,

    // 액션
    fetchScheduleData,
    fetchScheduleDataFromServer,
    selectSchedule,
    addSelectedSchedule, // 🔧 API 호출 포함
    addSelectedSchedules, // 🔧 새로 추가
    addSelectedScheduleLocal, // 🔧 로컬만 (기존 로
    addSchedulesToSelection,
    replaceSelectedSchedules,

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
  }
})
