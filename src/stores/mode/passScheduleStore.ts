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

export const usePassScheduleStore = defineStore('passSchedule', () => {
  const $q = useQuasar()
  
  // 상태
  const scheduleData = ref<ScheduleItem[]>([])
  const selectedSchedule = ref<ScheduleItem | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  
  // 스케줄 데이터 가져오기 (API 호출 대신 더미 데이터 사용)
  const fetchScheduleData = async () => {
    try {
      loading.value = true
      error.value = null
      
      // 실제로는 API 호출이 여기서 이루어집니다.
      // 예시를 위해 더미 데이터를 반환합니다.
      await new Promise(resolve => setTimeout(resolve, 500)) // API 호출 지연 시뮬레이션
      
      // 더미 데이터 생성
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
    
    // 액션
    fetchScheduleData,
    selectSchedule,
    init,
  }
})
