import { defineStore } from 'pinia'
import { ref, computed, readonly } from 'vue'
import {
  ephemerisTrackService,
  type ScheduleItem,
  type ScheduleDetailItem,
  type EphemerisTrackRequest,
} from '../../services/mode/ephemerisTrackService'

export const useEphemerisTrackStore = defineStore('ephemerisTrack', () => {
  // ===== 상태 정의 =====
  const masterData = ref<ScheduleItem[]>([])
  const detailData = ref<ScheduleDetailItem[]>([])
  const selectedSchedule = ref<ScheduleItem | null>(null)
  const currentTrackingPassId = ref<number | null>(null)
  const trackingStatus = ref<'idle' | 'active' | 'paused' | 'error'>('idle')
  const tleData = ref<EphemerisTrackRequest | null>(null)

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

  // ===== 액션 메서드들 =====

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

      // 새 데이터 생성 후 마스터 데이터 새로고침
      await loadMasterData(true)

      return request
    } catch (err) {
      error.value = 'Failed to process TLE data'
      throw err
    }
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
   * 상태 초기화
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

    // 계산된 속성
    hasValidData,
    isTrackingActive,
    currentScheduleInfo,

    // 액션
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
  }
})

// 타입 export
export type { ScheduleItem, ScheduleDetailItem, EphemerisTrackRequest }
