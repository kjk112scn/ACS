<template>
  <div class="hardware-error-log-panel">
    <!-- 헤더 -->
    <div class="header-section">
      <h5 class="q-mt-none q-mb-md">
        Hardware Error Log
        <span v-if="totalLogCount > 0" class="text-caption text-grey-6 q-ml-sm">
          (Total: {{ totalLogCount }} logs)
        </span>
      </h5>

      <!-- 통계 정보 -->
      <div class="stats-section">
        <!-- 수동 실시간 업데이트 버튼 -->
        <q-chip :color="isManualRealtimeUpdate ? 'orange' : 'blue'" text-color="white"
          :icon="isManualRealtimeUpdate ? 'stop' : 'sync'"
          :label="isManualRealtimeUpdate ? T.hardwareErrorLog.updating : T.hardwareErrorLog.realtimeUpdate"
          clickable @click="toggleRealtimeUpdate" />

        <!-- 초기 로딩 상태 표시 -->
        <q-spinner v-if="!hardwareErrorLogStore.isInitialLoad" color="primary" size="20px" class="q-ml-sm" />
        <span v-if="!hardwareErrorLogStore.isInitialLoad" class="text-caption q-ml-sm">Initial Loading...</span>
      </div>
    </div>

    <!-- 필터 섹션 -->
    <div class="filter-section">
      <!-- 카테고리 필터 -->
      <q-select v-model="selectedCategory" :options="categoryOptions" :label="T.hardwareErrorLog.category" dense
        outlined style="min-width: 150px" clearable emit-value map-options />

      <!-- 심각도 필터 -->
      <q-select v-model="selectedSeverity" :options="severityOptions" :label="T.hardwareErrorLog.severity" dense
        outlined style="min-width: 120px" clearable emit-value map-options />

      <!-- 날짜 범위 필터 -->
      <q-input v-model="startDate" :label="T.hardwareErrorLog.startDate" type="date" dense outlined
        style="min-width: 150px" class="date-input" />

      <q-input v-model="endDate" :label="T.hardwareErrorLog.endDate" type="date" dense outlined
        style="min-width: 150px" class="date-input" />

      <!-- 해결 상태 필터 -->
      <q-select v-model="selectedResolvedStatus" :options="resolvedStatusOptions"
        :label="T.hardwareErrorLog.resolutionStatus" dense outlined style="min-width: 150px" clearable emit-value
        map-options />

      <!-- 조회 버튼 -->
      <q-btn color="primary" :label="T.hardwareErrorLog.search" @click="manualSearch" />

      <!-- 필터 초기화 -->
      <q-btn color="grey" :label="T.hardwareErrorLog.reset" @click="resetFilters" />
    </div>

    <!-- 스크롤 가능한 에러 로그 목록 -->
    <div class="log-container" ref="logContainer" @scroll="handleScroll">
      <!-- 에러 로그 목록 -->
      <q-list v-if="errorLogs.length > 0" class="error-log-list">
        <q-item v-for="log in errorLogs" :key="log.id" class="error-log-item">
          <q-item-section>
            <q-item-label class="error-message">
              {{ log.message || `[No Message] ${log.errorKey}` }}
            </q-item-label>
            <q-item-label caption class="error-details">
              {{ getCategoryName(log.category) }} • {{ getSeverityName(log.severity) }} • {{
                formatTimestamp(log.timestamp) }}
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-chip :color="getStatusChipColor(log.severity, log.isResolved)"
              :text-color="getStatusChipTextColor(log.severity, log.isResolved)"
              :label="getStatusChipLabel(log.severity, log.isResolved)" size="sm" />
          </q-item-section>
        </q-item>
      </q-list>

      <!-- 로딩 상태 표시 -->
      <div v-if="isLoadingMore" class="loading-more">
        <q-spinner color="primary" size="20px" />
        <span class="q-ml-sm">{{ T.hardwareErrorLog.loadingMoreLogs }}</span>
      </div>

      <!-- 더 보기 버튼 (스크롤과 버튼 모두 지원) -->
      <div v-else-if="hasMorePages && !isLoadingMore" class="load-more-section">
        <q-btn color="primary" outline :label="T.hardwareErrorLog.loadMoreLogs" icon="expand_more"
          @click="loadMoreLogs" class="load-more-btn" />
        <div class="load-more-info">
          <span class="text-caption text-grey-6">
            {{ T.hardwareErrorLog.showingLogs(allLoadedLogs.length, totalLogCount) }}
          </span>
          <div class="scroll-hint">
            <q-icon name="mouse" size="18px" class="q-mr-sm" />
            <span>{{ T.hardwareErrorLog.scrollHint }}</span>
          </div>
        </div>
      </div>

      <!-- 더 이상 로드할 데이터가 없을 때 -->
      <div v-else-if="!hasMorePages && allLoadedLogs.length > 0" class="no-more-data">
        <q-icon name="check_circle" size="20px" color="green" />
        <span class="q-ml-sm text-grey-6">{{ T.hardwareErrorLog.allLogsLoaded(totalLogCount) }}</span>
      </div>

      <!-- 로그가 없을 때 -->
      <div v-else-if="allLoadedLogs.length === 0" class="no-logs">
        <q-icon name="info" size="48px" color="grey" />
        <p>{{ T.hardwareErrorLog.noLogsToDisplay }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useHardwareErrorLogStore } from '@/stores/hardwareErrorLogStore'
import { useTheme } from '@/composables/useTheme'
import { T } from '@/texts'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { HardwareErrorLog } from '@/types/hardwareError'
import { getApiBaseUrl } from '@/utils/api-config'

const hardwareErrorLogStore = useHardwareErrorLogStore()
const { initializeTheme } = useTheme()
const { handleApiError } = useErrorHandler()

// 직접 번역 함수 테스트 (사용하지 않으므로 제거)
// const testTranslate = (errorKey: string, isResolved: boolean) => {
//   const key = isResolved ? `${errorKey}_RESOLVED` : errorKey
//   const i18nKey = `hardwareErrors.${key}`
//   const translatedMessage = t(i18nKey)
//
//   console.log('🔍 HardwareErrorLogPanel 직접 번역 테스트:', {
//     errorKey,
//     isResolved,
//     key,
//     i18nKey,
//     translatedMessage
//   })
//
//   return translatedMessage
// }

// ✅ 무한 스크롤 관련 변수들
const totalLogCount = ref<number>(0)
const currentPage = ref<number>(0)
const pageSize = ref<number>(5) // 한 번에 로드할 로그 개수
const isLoadingMore = ref<boolean>(false)
const hasMorePages = ref<boolean>(true)
const allLoadedLogs = ref<HardwareErrorLog[]>([]) // 모든 로드된 로그들
const logContainer = ref<HTMLElement>() // 스크롤 컨테이너 참조

// ✅ hardwareErrorLogStore에서 직접 데이터 가져오기
const errorLogs = computed(() => {
  console.log('🔍 HardwareErrorLogPanel - errorLogs computed:', allLoadedLogs.value)
  return allLoadedLogs.value
})
// 활성 에러와 해결됨 카운터 제거 (사용하지 않음)
// const activeErrorCount = computed(() => hardwareErrorLogStore.activeErrorCount)
// const resolvedErrorCount = computed(() => hardwareErrorLogStore.resolvedErrorCount)

// ✅ 필터 변수들 정의
const selectedCategory = ref<string | null>(null)
const selectedSeverity = ref<string | null>(null)
const selectedResolvedStatus = ref<string | null>(null)
const startDate = ref<string>('')
const endDate = ref<string>('')

// ✅ 옵션들 정의 - 실제 데이터에 맞게 수정
const categoryOptions = [
  { label: 'All', value: null },
  { label: 'Power', value: 'POWER' },
  { label: 'Protocol', value: 'PROTOCOL' },
  { label: 'Emergency', value: 'EMERGENCY' },
  { label: 'Servo Power', value: 'SERVO_POWER' },
  { label: 'Stow', value: 'STOW' },
  { label: 'Positioner', value: 'POSITIONER' },
  { label: 'Feed', value: 'FEED' }
]

const severityOptions = [
  { label: 'All', value: null },
  { label: 'Info', value: 'INFO' },
  { label: 'Warning', value: 'WARNING' },
  { label: 'Error', value: 'ERROR' },
  { label: 'Critical', value: 'CRITICAL' }
]

const resolvedStatusOptions = [
  { label: 'All', value: null },
  { label: 'Resolved', value: 'resolved' },
  { label: 'Unresolved', value: 'unresolved' }
]

// ✅ 다국어 함수들 정의

// 실시간 업데이트 상태 (사용하지 않으므로 제거)
// const isRealtimeUpdating = computed(() => {
//   return hardwareErrorLogStore.isPopupOpen && hardwareErrorLogStore.isInitialLoad
// })

// 실시간 업데이트 관련 상태
const isManualRealtimeUpdate = ref(false)
let realtimeUpdateInterval: NodeJS.Timeout | null = null

// 실시간 업데이트 시작/중지 함수
const toggleRealtimeUpdate = () => {
  if (isManualRealtimeUpdate.value) {
    // 실시간 업데이트 중지
    stopRealtimeUpdate()
  } else {
    // 실시간 업데이트 시작
    startRealtimeUpdate()
  }
}

const startRealtimeUpdate = () => {
  console.log('🔄 실시간 업데이트 시작 (5초 간격)')
  isManualRealtimeUpdate.value = true

  // 즉시 한 번 실행
  void refreshErrorLogs()

  // 5초마다 반복
  realtimeUpdateInterval = setInterval(() => {
    void refreshErrorLogs()
  }, 5000)
}

const stopRealtimeUpdate = () => {
  console.log('⏹️ 실시간 업데이트 중지')
  isManualRealtimeUpdate.value = false

  if (realtimeUpdateInterval) {
    clearInterval(realtimeUpdateInterval)
    realtimeUpdateInterval = null
  }
}

// ✅ 페이징 API 호출 함수 - pageSize 변수로 동적 로드
const loadErrorLogsPage = async (page: number, reset: boolean = false) => {
  try {
    console.log('📄 페이지 로드 시작:', {
      page,
      reset,
      pageSize: pageSize.value,
      currentLoadedCount: allLoadedLogs.value.length,
      hasMorePages: hasMorePages.value
    })

    if (reset) {
      isLoadingMore.value = true
      currentPage.value = 0
      allLoadedLogs.value = []
    } else {
      isLoadingMore.value = true
    }

    // API 파라미터 구성 - pageSize 변수 사용
    const params = new URLSearchParams({
      page: page.toString(),
      size: pageSize.value.toString() // pageSize 변수로 동적 설정
    })

    if (selectedCategory.value) params.append('category', selectedCategory.value)
    if (selectedSeverity.value) params.append('severity', selectedSeverity.value)
    if (selectedResolvedStatus.value) params.append('resolvedStatus', selectedResolvedStatus.value)
    if (startDate.value) params.append('startDate', startDate.value)
    if (endDate.value) params.append('endDate', endDate.value)

    console.log('📄 API 요청 URL:', `${getApiBaseUrl()}/hardware-error-logs/paginated?${params}`)
    const response = await fetch(`${getApiBaseUrl()}/hardware-error-logs/paginated?${params}`)

    if (response.ok) {
      const data = await response.json()
      console.log('📄 페이지 로드 응답:', {
        content: data.content,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        last: data.last,
        first: data.first,
        numberOfElements: data.numberOfElements
      })

      // 받은 데이터에 번역 적용
      const translatedLogs = (data.content || []).map((log: HardwareErrorLog) => ({
        ...log,
        message: hardwareErrorLogStore.translateErrorKey(log.errorKey, log.isResolved),
        resolvedMessage: log.isResolved ? hardwareErrorLogStore.translateErrorKey(log.errorKey, log.isResolved) : undefined,
      }))

      if (reset) {
        allLoadedLogs.value = translatedLogs
        totalLogCount.value = data.totalElements || 0
        currentPage.value = 0
      } else {
        allLoadedLogs.value = [...allLoadedLogs.value, ...translatedLogs]
        currentPage.value = page
      }

      hasMorePages.value = !data.last

      // 로드된 데이터에 번역 적용
      hardwareErrorLogStore.updateErrorMessages()

      console.log('📄 로드 완료:', {
        totalCount: totalLogCount.value,
        loadedCount: allLoadedLogs.value.length,
        hasMore: hasMorePages.value,
        loadedItems: translatedLogs.length,
        currentPage: currentPage.value,
        isLastPage: data.last
      })
    } else {
      handleApiError(new Error(`페이지 로드 실패: ${response.status}`), 'Hardware Error Log')
    }
  } catch (error) {
    handleApiError(error, 'Hardware Error Log 페이지 로드')
  } finally {
    isLoadingMore.value = false
  }
}

// 에러 로그 새로고침 함수 - 실시간 업데이트용
const refreshErrorLogs = async () => {
  try {
    console.log('🔄 실시간 에러 로그 새로고침 중...')

    // 스토어에서 전체 데이터 새로고침
    await hardwareErrorLogStore.loadHistoryFromBackend()

    // 현재 로드된 로그 개수와 총 개수 비교
    const storeLogs = hardwareErrorLogStore.errorLogs || []
    const currentTotal = totalLogCount.value
    const newTotal = storeLogs.length

    console.log('🔄 실시간 업데이트 비교:', {
      currentLoaded: allLoadedLogs.value.length,
      currentTotal,
      newTotal,
      hasNewLogs: newTotal > currentTotal
    })

    // 새로운 로그가 있으면 첫 페이지부터 다시 로드
    if (newTotal > currentTotal) {
      console.log('🆕 새로운 로그 발견! 첫 페이지부터 다시 로드')
      totalLogCount.value = newTotal
      await loadErrorLogsPage(0, true) // 첫 페이지부터 리셋하여 로드
    } else {
      console.log('📊 새로운 로그 없음')
    }

    console.log('✅ 실시간 에러 로그 새로고침 완료')
  } catch (error) {
    handleApiError(error, '에러 로그 새로고침')
  }
}

// ✅ 필터 값 변경 감지
watch([selectedCategory, selectedSeverity, selectedResolvedStatus, startDate, endDate], () => {
  console.log('🔍 필터 값 변경 감지:', {
    category: selectedCategory.value,
    severity: selectedSeverity.value,
    resolvedStatus: selectedResolvedStatus.value,
    startDate: startDate.value,
    endDate: endDate.value
  })
  // 필터 변경 시 첫 페이지부터 새로 로드
  void loadErrorLogsPage(0, true)
})

// ✅ 스크롤 이벤트 핸들러 - 무한 스크롤 지원
const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  if (!target) return

  const { scrollTop, scrollHeight, clientHeight } = target
  const isNearBottom = scrollTop + clientHeight >= scrollHeight - 50 // 50px 전에 로드

  console.log('🔄 스크롤 이벤트:', {
    scrollTop,
    scrollHeight,
    clientHeight,
    isNearBottom,
    hasMorePages: hasMorePages.value,
    isLoadingMore: isLoadingMore.value,
    currentPage: currentPage.value,
    loadedCount: allLoadedLogs.value.length,
    totalCount: totalLogCount.value
  })

  if (isNearBottom && hasMorePages.value && !isLoadingMore.value) {
    console.log('📄 스크롤 감지 - 다음 페이지 로드:', currentPage.value + 1, `(${pageSize.value}개씩)`)
    void loadErrorLogsPage(currentPage.value + 1, false)
  } else if (!hasMorePages.value) {
    console.log('📄 더 이상 로드할 페이지가 없음')
  } else if (isLoadingMore.value) {
    console.log('📄 이미 로딩 중...')
  } else {
    console.log('📄 스크롤 위치 부족:', Math.round(((scrollTop + clientHeight) / scrollHeight) * 100), '%')
  }
}

// ✅ 더 보기 버튼 클릭 핸들러
const loadMoreLogs = () => {
  console.log('📄 더 보기 버튼 클릭 - 다음 페이지 로드:', currentPage.value + 1, `(${pageSize.value}개씩)`)
  void loadErrorLogsPage(currentPage.value + 1, false)
}

// 컴포넌트 마운트 시 팝업 상태 설정
onMounted(async () => {
  console.log('🔍 HardwareErrorLogPanel 마운트됨 (페이징 로드 모드)')

  // 초기 데이터 로드
  await loadErrorLogsPage(0, true)

  // 에러 메시지 번역 업데이트
  hardwareErrorLogStore.updateErrorMessages()

  // q-scroll-area는 자동으로 스크롤 이벤트를 처리하므로 별도 리스너 불필요
  await hardwareErrorLogStore.setPopupOpen(true)
})

// 컴포넌트 언마운트 시 팝업 상태 해제 및 타이머 정리
onUnmounted(async () => {
  // 실시간 업데이트 중지
  stopRealtimeUpdate()

  // 팝업 상태 해제
  await hardwareErrorLogStore.setPopupOpen(false)
})

const getCategoryName = (category: string) => {
  const categoryNames = {
    'POWER': 'Power',
    'PROTOCOL': 'Protocol',
    'EMERGENCY': 'Emergency',
    'SERVO_POWER': 'Servo Power',
    'STOW': 'Stow',
    'POSITIONER': 'Positioner',
    'FEED': 'Feed'
  }

  return categoryNames[category] || category
}

const getSeverityName = (severity: string) => {
  const severityNames = {
    'INFO': 'Info',
    'WARNING': 'Warning',
    'ERROR': 'Error',
    'CRITICAL': 'Critical'
  }

  return severityNames[severity] || severity
}

// ✅ 해결된 에러 개수 계산 - 이 부분을 완전히 제거하세요
// const resolvedErrorCount = computed(() => {
//   return errorLogs.value.filter(log => log.isResolved).length
// })

// ✅ 필터링된 에러 로그 (사용하지 않음 - 페이징으로 대체)
// const filteredErrorLogs = computed(() => {
let filtered = [...errorLogs.value] // ✅ .value 추가

console.log('🔍 필터링 시작 - 전체 로그 개수:', filtered.length)
console.log('🔍 현재 필터 조건:', {
  category: selectedCategory.value,
  severity: selectedSeverity.value,
  resolvedStatus: selectedResolvedStatus.value,
  startDate: startDate.value,
  endDate: endDate.value
})

// 카테고리 필터
if (selectedCategory.value) {
  const beforeCount = filtered.length
  filtered = filtered.filter(log => log.category === selectedCategory.value)
  console.log('🔍 카테고리 필터 적용:', selectedCategory.value, `${beforeCount} → ${filtered.length}`)
}

// 심각도 필터
if (selectedSeverity.value) {
  const beforeCount = filtered.length
  filtered = filtered.filter(log => log.severity === selectedSeverity.value)
  console.log('🔍 심각도 필터 적용:', selectedSeverity.value, `${beforeCount} → ${filtered.length}`)
}

// 해결 상태 필터
if (selectedResolvedStatus.value) {
  const beforeCount = filtered.length
  if (selectedResolvedStatus.value === 'resolved') {
    filtered = filtered.filter(log => log.isResolved)
  } else if (selectedResolvedStatus.value === 'unresolved') {
    filtered = filtered.filter(log => !log.isResolved)
  }
  console.log('🔍 해결 상태 필터 적용:', selectedResolvedStatus.value, `${beforeCount} → ${filtered.length}`)
}

// 날짜 범위 필터
if (startDate.value) {
  const beforeCount = filtered.length
  const start = new Date(startDate.value)
  filtered = filtered.filter(log => {
    const logDate = new Date(log.timestamp)
    return logDate >= start
  })
  console.log('🔍 시작 날짜 필터 적용:', startDate.value, `${beforeCount} → ${filtered.length}`)
}

if (endDate.value) {
  const beforeCount = filtered.length
  const end = new Date(endDate.value)
  end.setHours(23, 59, 59, 999) // 하루 끝까지 포함
  filtered = filtered.filter(log => {
    const logDate = new Date(log.timestamp)
    return logDate <= end
  })
  console.log('🔍 종료 날짜 필터 적용:', endDate.value, `${beforeCount} → ${filtered.length}`)
}

// ✅ 최신 로그가 위로 오도록 시간순 정렬 (최신순)
// filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())

//   console.log('🔍 필터링 완료 - 최종 로그 개수:', filtered.length)
//   return filtered
// })

// ✅ 필터 적용 (사용하지 않음 - 자동 필터링으로 대체)
// const applyFilters = () => {
//   // 필터가 변경되면 computed 속성이 자동으로 업데이트됨
//   console.log('조회 실행:', {
//     category: selectedCategory.value,
//     severity: selectedSeverity.value,
//     resolvedStatus: selectedResolvedStatus.value,
//     startDate: startDate.value,
//     endDate: endDate.value
//   })
// }

// ✅ 수동 검색 함수
const manualSearch = () => {
  console.log('🔍 수동 검색 실행')
  void loadErrorLogsPage(0, true)
}

// ✅ 필터 초기화
const resetFilters = () => {
  console.log('🔍 필터 초기화 실행')

  // 기본값으로 설정 (전체)
  selectedCategory.value = null
  selectedSeverity.value = null
  selectedResolvedStatus.value = null

  // 기본 날짜 설정 (한 달 전 ~ 현재)
  const today = new Date()
  const oneMonthAgo = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate())

  startDate.value = oneMonthAgo.toISOString().split('T')[0]
  endDate.value = today.toISOString().split('T')[0]

  console.log('🔍 필터 초기화 완료 - 모든 필터가 전체로 설정됨')
}

// ✅ 심각도별 색상 (사용하지 않으므로 제거)
// const getSeverityColor = (severity: string) => {
//   switch (severity) {
//     case 'CRITICAL': return 'red'
//     case 'ERROR': return 'orange'
//     case 'WARNING': return 'yellow'
//     case 'INFO': return 'blue'
//     default: return 'grey'
//   }
// }

// const getSeverityTextColor = (severity: string) => {
//   switch (severity) {
//     case 'WARNING': return 'black'
//     default: return 'white'
//   }
// }

// ✅ 상태 칩 색상 (심각도 + 해결상태 조합)
const getStatusChipColor = (severity: string, isResolved: boolean) => {
  if (isResolved) {
    // 해결됨은 모두 초록색으로 통일
    return 'green'
  } else {
    // 활성 상태는 심각도별 색상
    switch (severity) {
      case 'CRITICAL': return 'red'  // CRITICAL과 ERROR 모두 빨간색으로 통일
      case 'ERROR': return 'red'
      case 'WARNING': return 'yellow'
      case 'INFO': return 'blue'
      default: return 'grey'
    }
  }
}

// ✅ 상태 칩 텍스트 색상
const getStatusChipTextColor = (severity: string, isResolved: boolean) => {
  if (isResolved) {
    // 해결됨은 흰색 텍스트
    return 'white'
  } else {
    // 활성 상태는 심각도별 텍스트 색상
    switch (severity) {
      case 'WARNING': return 'black'  // 노란색 배경에 검은색 텍스트
      default: return 'white'
    }
  }
}

// ✅ 상태 칩 라벨 (심각도 + 해결상태) - 다국어 지원
const getStatusChipLabel = (severity: string, isResolved: boolean) => {
  const severityText = getSeverityName(severity)
  const statusText = isResolved ? T.value.hardwareErrorLog.resolved : T.value.hardwareErrorLog.active
  return `${severityText} ${statusText}`
}

// ✅ 시간 포맷팅
const formatTimestamp = (timestamp: string) => {
  return new Date(timestamp).toLocaleString('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true
  })
}

// 컴포넌트 마운트 시 기본 필터 설정
onMounted(() => {
  initializeTheme()

  // 기본 날짜 설정 (한 달 전 ~ 현재)
  const today = new Date()
  const oneMonthAgo = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate())

  startDate.value = oneMonthAgo.toISOString().split('T')[0]
  endDate.value = today.toISOString().split('T')[0]
})
</script>

<style scoped>
/* 달력 아이콘 스타일링은 전역 CSS에서 관리 */
.hardware-error-log-panel {
  padding: 20px;
  background-color: var(--theme-card-background);
  color: var(--theme-text);
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 로그 컨테이너 스타일링 */
.log-container {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 50px);
  /* 헤더와 필터 영역 제외 */
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stats-section {
  display: flex;
  gap: 10px;
}

.filter-section {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.error-log-list {
  background-color: var(--theme-card-background);
  border-radius: 8px;
  border: 1px solid var(--theme-border);
}

.error-log-item {
  border-bottom: 1px solid var(--theme-border);
  padding: 12px 16px;
}

.error-log-item:last-child {
  border-bottom: none;
}

.error-message {
  font-weight: 500;
  margin-bottom: 4px;
}

.error-details {
  color: var(--theme-text-secondary);
  font-size: 0.9em;
}

.no-more-data {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: var(--theme-text-secondary);
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: var(--theme-text-secondary);
}

.no-logs {
  text-align: center;
  padding: 40px;
  color: var(--theme-text-secondary);
}

.no-logs p {
  margin-top: 16px;
  font-size: 1.1em;
}

/* 더 보기 버튼 섹션 */
.load-more-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  gap: 10px;
}

.load-more-btn {
  min-width: 200px;
}

.load-more-info {
  text-align: center;
}

.scroll-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  opacity: 1;
  font-size: 0.9em;
  color: var(--theme-text-secondary);
  font-weight: 500;
}

/* 달력 아이콘 스타일링은 공통 CSS 파일에서 관리 */
</style>
