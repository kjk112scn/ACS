import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import {
  passScheduleService,
  type AddTleAndTrackingRequest,
  type TleAndTrackingResponse,
  type PassScheduleMasterData,
} from '../../services/mode/passScheduleService'

export interface ScheduleItem {
  no: number
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

  // 스케줄 데이터 가져오기
  const fetchScheduleData = async () => {
    // 서버에서 실제 데이터를 가져오도록 변경
    return await fetchScheduleDataFromServer()
  }

  // 🆕 선택된 스케줄을 목록에 추가
  const addSelectedSchedule = (schedule: ScheduleItem) => {
    // 중복 체크
    const exists = selectedScheduleList.value.find((item) => item.no === schedule.no)
    if (!exists) {
      selectedScheduleList.value.push(schedule)
      console.log('✅ 스케줄이 선택 목록에 추가됨:', schedule.satelliteName)
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
        let scheduleNo = 1

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
                no: scheduleNo++,
                satelliteId: pass.SatelliteID || satelliteId,
                satelliteName: pass.SatelliteName || `Satellite-${satelliteId}`,
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

    // 액션
    fetchScheduleData,
    fetchScheduleDataFromServer,
    selectSchedule,

    // 🆕 선택된 스케줄 관리 액션
    addSelectedSchedule,
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
  }
})
