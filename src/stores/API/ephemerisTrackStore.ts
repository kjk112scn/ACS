import { defineStore } from 'pinia'
import { ref, computed, onMounted } from 'vue'
import { ephemerisTrackService } from '../../services/ephemerisTrackService'
import type { EphemerisTrackRequest, TLEParseError, ApiError } from '../../types/ephemerisTrack'
import type { ScheduleItem, ScheduleDetailItem } from '../../types/ephemerisTrack'
// 타입 정의를 임포트하지 않고 서비스의 타입을 사용하도록 변경
import type {
  ScheduleItem as ServiceScheduleItem,
  ScheduleDetailItem as ServiceScheduleDetailItem,
} from '../../services/ephemerisTrackService'

// 서비스의 타입을 types의 타입으로 변환하는 타입 매핑 함수
function mapToScheduleItem(item: ServiceScheduleItem): ScheduleItem {
  return {
    mstId: item.No,
    name: item.SatelliteName,
    description: item.SatelliteID,
    createdAt: item.CreationDate,
    updatedAt: item.CreationDate,
  }
}

function mapToScheduleDetailItem(item: ServiceScheduleDetailItem): ScheduleDetailItem {
  return {
    id: 0, // 임의 ID 사용
    mstId: 0, // 임의 mstId 사용
    timestamp: item.Time,
    azimuth: item.Azimuth,
    elevation: item.Elevation,
    range: 0,
    rangeRate: 0,
    x: 0,
    y: 0,
    z: 0,
    vx: 0,
    vy: 0,
    vz: 0,
  }
}

export const useEphemerisTrackStore = defineStore('ephemerisTrack', () => {
  // 상태
  const ephemerisMasterData = ref<ScheduleItem[]>([])
  const ephemerisDetailData = ref<ScheduleDetailItem[]>([])
  const isLoading = ref(false)
  const error = ref<Error | null>(null)

  // 에러 상태 설정 헬퍼 함수
  const setError = (err: unknown) => {
    if (err instanceof Error) {
      error.value = err
    } else {
      error.value = new Error(String(err))
    }
    return error.value
  }

  // 계산된 속성
  const hasMasterData = computed(() => ephemerisMasterData.value.length > 0)
  const hasDetailData = computed(() => ephemerisDetailData.value.length > 0)
  const latestMasterData = computed<ScheduleItem | undefined>(() => {
    if (ephemerisMasterData.value.length === 0) return undefined

    return [...ephemerisMasterData.value].sort((a, b) => {
      const dateA = new Date(a.createdAt || 0).getTime()
      const dateB = new Date(b.createdAt || 0).getTime()
      return dateB - dateA
    })[0]
  })

  /**
   * 위성 궤도 추적 데이터를 생성합니다.
   * @param request - TLE 데이터를 포함한 요청 객체
   * @returns 생성된 추적 데이터
   */
  const generateEphemerisTrack = async (request: EphemerisTrackRequest): Promise<unknown> => {
    isLoading.value = true
    error.value = null

    try {
      const result = await ephemerisTrackService.generateEphemerisTrack(request)
      // 생성 후 마스터 데이터 자동 갱신
      await fetchEphemerisMasterData()
      return result
    } catch (err) {
      if (err instanceof Error) {
        error.value = err
      } else {
        error.value = new Error(String(err))
      }
      throw error.value
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 모든 마스터 데이터를 조회합니다.
   * @returns 마스터 데이터 배열
   */
  const fetchEphemerisMasterData = async (): Promise<ScheduleItem[]> => {
    isLoading.value = true
    error.value = null

    try {
      const serviceData = await ephemerisTrackService.fetchEphemerisMasterData()
      // 서비스 데이터를 스토어 타입으로 변환
      const mappedData = serviceData.map((item) => mapToScheduleItem(item))
      ephemerisMasterData.value = mappedData
      return mappedData
    } catch (err) {
      if (err instanceof Error) {
        error.value = err
      } else {
        error.value = new Error(String(err))
      }
      throw error.value
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 특정 마스터 ID에 대한 세부 데이터를 조회합니다.
   * @param mstId - 조회할 마스터 ID
   * @returns 세부 데이터 배열
   */
  const fetchEphemerisDetailData = async (mstId: number): Promise<ScheduleDetailItem[]> => {
    // 유효성 검사
    if (!mstId || typeof mstId !== 'number' || mstId <= 0) {
      throw setError('유효하지 않은 마스터 ID입니다')
    }

    isLoading.value = true
    error.value = null

    try {
      const serviceData = await ephemerisTrackService.fetchEphemerisDetailData(mstId)
      // 서비스 데이터를 스토어 타입으로 변환
      const mappedData = serviceData.map((item) => mapToScheduleDetailItem(item))
      ephemerisDetailData.value = mappedData
      return mappedData
    } catch (err) {
      if (err instanceof Error) {
        error.value = err
      } else {
        error.value = new Error(String(err))
      }
      throw error.value
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 특정 마스터 ID의 데이터를 삭제합니다.
   * @param mstId - 삭제할 마스터 ID
   * @returns 삭제 성공 여부
   */
  const deleteEphemerisData = async (mstId: number): Promise<boolean> => {
    if (!mstId || typeof mstId !== 'number' || mstId <= 0) {
      const err = new Error('유효하지 않은 마스터 ID입니다')
      error.value = err
      throw err
    }

    isLoading.value = true
    error.value = null

    try {
      const success = await ephemerisTrackService.deleteEphemerisData(mstId)
      if (success) {
        // 삭제 성공 시 마스터 데이터 갱신
        await fetchEphemerisMasterData()
        // 현재 상세 데이터가 삭제된 데이터라면 초기화
        if (ephemerisDetailData.value[0]?.mstId === mstId) {
          ephemerisDetailData.value = []
        }
      }
      return success
    } catch (err) {
      if (err instanceof Error) {
        error.value = err
      } else {
        error.value = new Error(String(err))
      }
      throw error.value
    } finally {
      isLoading.value = false
    }
  }

  /**
   * TLE 텍스트를 파싱합니다.
   * @param tleText - 파싱할 TLE 텍스트
   * @returns 파싱된 TLE 데이터
   */
  const parseTLEData = (tleText: string) => {
    try {
      const parsedData = ephemerisTrackService.parseTLEData(tleText)
      // 기본 값 추가하여 EphemerisTrackRequest 형식으로 변환
      const now = new Date()
      const tomorrow = new Date(now)
      tomorrow.setDate(now.getDate() + 1)

      return {
        tleLine1: parsedData.tleLine1,
        tleLine2: parsedData.tleLine2,
        satelliteName: parsedData.satelliteName || undefined,
        startTime: now.toISOString(),
        endTime: tomorrow.toISOString(),
        stepSize: 60, // 기본값으로 60초 간격 사용
      } as EphemerisTrackRequest
    } catch (err) {
      throw setError(err)
    }
  }

  // 스토어 초기화 시 마스터 데이터 자동 로드
  const initialize = () => {
    fetchEphemerisMasterData().catch((err) => {
      console.error('Failed to initialize ephemeris track store:', err)
      setError(err)
    })
  }

  // 컴포넌트 마운트 시 초기화
  onMounted(() => {
    initialize()
  })

  // 컴포넌트에서 사용할 수 있는 속성 및 메서드 노출
  return {
    // 상태
    ephemerisMasterData,
    ephemerisDetailData,
    isLoading,
    error,

    // 계산된 속성
    hasMasterData,
    hasDetailData,
    latestMasterData,

    // 메서드
    initialize,
    parseTLEData,
    generateEphemerisTrack,
    fetchEphemerisMasterData,
    fetchEphemerisDetailData,
    deleteEphemerisData,
  }
})

export type { EphemerisTrackRequest, ScheduleItem, ScheduleDetailItem, TLEParseError, ApiError }
