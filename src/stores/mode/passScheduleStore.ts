import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useQuasar } from 'quasar'

interface ScheduleItem {
  No: number
  Name: string
  StartTime: string
  EndTime: string
  Status: string
  Azimuth: number
  Elevation: number
  Tilt: number
  Duration: number
}

interface TLEItem {
  No: number
  TLE: string
}

export const usePassScheduleStore = defineStore('passSchedule', () => {
  const $q = useQuasar()




  // 상태
  const scheduleData = ref<ScheduleItem[]>([])
  const selectedSchedule = ref<ScheduleItem | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)



  // TLE 관련 상태
  const tleData = ref<TLEItem[]>([])
  const selectedTLE = ref<TLEItem | null>(null)



  // 스케줄 데이터 가져오기
  const fetchScheduleData = async () => {
    try {
      loading.value = true
      error.value = null







      await new Promise(resolve => setTimeout(resolve, 500))

      const dummyData: ScheduleItem[] = [
        {
          No: 1,
          Name: 'ISS Pass 1',
          StartTime: new Date(Date.now() + 3600000).toISOString(),
          EndTime: new Date(Date.now() + 7200000).toISOString(),
          Status: 'Pending',
          Azimuth: 45.5,
          Elevation: 30.2,
          Tilt: 0,
          Duration: 60
        },
        {
          No: 2,
          Name: 'ISS Pass 2',
          StartTime: new Date(Date.now() + 10800000).toISOString(),
          EndTime: new Date(Date.now() + 14400000).toISOString(),
          Status: 'Pending',
          Azimuth: 135.8,
          Elevation: 45.7,
          Tilt: 0,
          Duration: 60
        },
        {
          No: 3,
          Name: 'ISS Pass 3',
          StartTime: new Date(Date.now() + 18000000).toISOString(),
          EndTime: new Date(Date.now() + 21600000).toISOString(),
          Status: 'Pending',
          Azimuth: 225.3,
          Elevation: 60.1,
          Tilt: 0,
          Duration: 60
        },
      ]


      scheduleData.value = dummyData
    } catch (err) {
      console.error('Failed to fetch schedule data:', err)
      error.value = 'Failed to fetch schedule data'
      $q.notify({
        type: 'negative',
        message: 'Failed to fetch schedule data',
      })
    } finally {
      loading.value = false
    }
  }


  // 스케줄 선택
  const selectSchedule = (schedule: ScheduleItem) => {
    selectedSchedule.value = schedule
  }



  // TLE 데이터 추가
  const addTLEData = (tleContent: string) => {

    const newNo = tleData.value.length > 0
      ? Math.max(...tleData.value.map(item => item.No)) + 1
      : 1

    const newTLE: TLEItem = {
      No: newNo,

      TLE: tleContent
    }

    tleData.value.push(newTLE)
  }


  // TLE 데이터 삭제
  const removeTLEData = (no: number) => {
    const index = tleData.value.findIndex(item => item.No === no)

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

    return tleData.value
      .map(item => item.TLE)
      .join('\n\n') + '\n'
  }

  // 초기화
  const init = async () => {
    await fetchScheduleData()
  }

  return {

    // 상태
    scheduleData,
    selectedSchedule,
    loading,
    error,

    // TLE 상태
    tleData,
    selectedTLE,

    // 액션
    fetchScheduleData,
    selectSchedule,

    // TLE 액션
    addTLEData,
    removeTLEData,
    clearTLEData,
    selectTLE,
    exportTLEData,

    init,
  }
})
