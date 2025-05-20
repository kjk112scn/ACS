import { defineStore } from 'pinia'
import { api } from 'boot/axios'
import { ref } from 'vue'

export interface EphemerisTrackRequest {
  tleLine1: string
  tleLine2: string
  satelliteName?: string | null
}

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
  No: number
  MstId: number
  Time: string
  Azimuth: number
  Elevation: number
  Range: number
  Altitude: number

  [key: string]: string | number | boolean | null | undefined
}

export const useEphemerisTrackStore = defineStore('ephemerisTrack', () => {
  const ephemerisMasterData = ref<ScheduleItem[]>([])
  const ephemerisDetailData = ref<ScheduleDetailItem[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // TLE 데이터 파싱 함수
  const parseTLEData = (tleText: string): EphemerisTrackRequest => {
    const lines = tleText.split('\n').filter(line => line.trim() !== '')

    if (lines.length < 2) {
      throw new Error('유효하지 않은 TLE 형식: 최소 2줄이 필요합니다')
    }

    let tleLine1 = ''
    let tleLine2 = ''
    let satelliteName: string | null = null

    if (lines.length >= 3) {
      // 3줄 형식: 첫 번째 줄은 위성 이름



      satelliteName = lines[0]?.trim() ?? ''
      tleLine1 = lines[1]?.trim() ?? ''
      tleLine2 = lines[2]?.trim() ?? ''
    } else {
      // 2줄 형식


      tleLine1 = lines[0]?.trim() ?? ''
      tleLine2 = lines[1]?.trim() ?? ''
    }

    // TLE 라인 유효성 검사
    if (!tleLine1.startsWith('1 ') || !tleLine2.startsWith('2 ')) {
      throw new Error('유효하지 않은 TLE 형식: 라인 1은 "1 "로, 라인 2는 "2 "로 시작해야 합니다')
    }

    return {
      tleLine1,
      tleLine2,
      satelliteName
    }
  }

  // 위성 궤도 추적 데이터 생성
  const generateEphemerisTrack = async (request: EphemerisTrackRequest) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await api.post('/ephemeris/generate', request)
      console.log('위성 궤도 추적 데이터 생성 완료:', response.data)

      // 생성 후 마스터 데이터 로드
      await fetchEphemerisMasterData()

      return response.data
    } catch (err) {
      console.error('위성 궤도 추적 데이터 생성 실패:', err)
      error.value = '위성 궤도 추적 데이터 생성에 실패했습니다'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  // 마스터 데이터 조회
  const fetchEphemerisMasterData = async () => {
    isLoading.value = true
    error.value = null

    try {
      const response = await api.get('/ephemeris/master')
      ephemerisMasterData.value = response.data
      return response.data
    } catch (err) {
      console.error('마스터 데이터 조회 실패:', err)
      error.value = '마스터 데이터 조회에 실패했습니다'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  // 특정 마스터 ID에 대한 세부 데이터 조회
  const fetchEphemerisDetailData = async (mstId: number) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await api.get(`/ephemeris/detail/${mstId}`)
      ephemerisDetailData.value = response.data
      return response.data
    } catch (err) {
      console.error(`마스터 ID ${mstId}에 대한 세부 데이터 조회 실패:`, err)
      error.value = '세부 데이터 조회에 실패했습니다'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return {
    ephemerisMasterData,
    ephemerisDetailData,
    isLoading,
    error,
    parseTLEData,
    generateEphemerisTrack,
    fetchEphemerisMasterData,
    fetchEphemerisDetailData
  }
})
