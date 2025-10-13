<template>
  <div class="hardware-error-log-panel">
    <!-- 헤더 -->
    <div class="header-section">
      <h5 class="q-mt-none q-mb-md">하드웨어 에러 로그</h5>

      <!-- 통계 정보 -->
      <div class="stats-section">
        <q-chip color="red" text-color="white" :label="`활성 에러: ${activeErrorCount}`" />
        <q-chip color="green" text-color="white" :label="`해결됨: ${resolvedErrorCount}`" />

        <!-- 실시간 업데이트 상태 표시 -->
        <q-chip v-if="isRealtimeUpdating" color="blue" text-color="white" icon="sync" :label="'실시간 업데이트'" />

        <!-- 초기 로딩 상태 표시 -->
        <q-spinner v-if="!hardwareErrorLogStore.isInitialLoad" color="primary" size="20px" class="q-ml-sm" />
        <span v-if="!hardwareErrorLogStore.isInitialLoad" class="text-caption q-ml-sm">초기 로딩 중...</span>
      </div>
    </div>

    <!-- 필터 섹션 -->
    <div class="filter-section">
      <!-- 카테고리 필터 -->
      <q-select v-model="selectedCategory" :options="categoryOptions" label="카테고리" dense outlined
        style="min-width: 150px" clearable emit-value map-options />

      <!-- 심각도 필터 -->
      <q-select v-model="selectedSeverity" :options="severityOptions" label="심각도" dense outlined
        style="min-width: 120px" clearable emit-value map-options />

      <!-- 날짜 범위 필터 -->
      <q-input v-model="startDate" label="시작 날짜" type="date" dense outlined style="min-width: 150px"
        class="date-input" />

      <q-input v-model="endDate" label="종료 날짜" type="date" dense outlined style="min-width: 150px" class="date-input" />

      <!-- 해결 상태 필터 -->
      <q-select v-model="selectedResolvedStatus" :options="resolvedStatusOptions" label="해결 상태" dense outlined
        style="min-width: 150px" clearable emit-value map-options />

      <!-- 조회 버튼 -->
      <q-btn color="primary" label="조회" @click="applyFilters" />

      <!-- 필터 초기화 -->
      <q-btn color="grey" label="초기화" @click="resetFilters" />
    </div>

    <!-- 에러 로그 목록 -->
    <q-list v-if="filteredErrorLogs.length > 0" class="error-log-list">
      <q-item v-for="log in filteredErrorLogs" :key="log.id" class="error-log-item">
        <q-item-section>
          <q-item-label class="error-message">
            {{ getCurrentMessage(log.message) }}
          </q-item-label>
          <q-item-label caption class="error-details">
            {{ getCategoryName(log.category) }} • {{ getSeverityName(log.severity) }} • {{
              formatTimestamp(log.timestamp) }}
          </q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-chip :color="getSeverityColor(log.severity)" :text-color="getSeverityTextColor(log.severity)"
            :label="log.isResolved ? '해결됨' : '활성'" size="sm" />
        </q-item-section>
      </q-item>
    </q-list>

    <!-- 로그가 없을 때 -->
    <div v-else class="no-logs">
      <q-icon name="info" size="48px" color="grey" />
      <p>표시할 에러 로그가 없습니다.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useHardwareErrorLogStore } from '@/stores/hardwareErrorLogStore'
import { useI18n } from 'vue-i18n'
import { useTheme } from '@/composables/useTheme'

const hardwareErrorLogStore = useHardwareErrorLogStore()
const { locale } = useI18n()
const { initializeTheme } = useTheme()

// ✅ hardwareErrorLogStore에서 직접 데이터 가져오기
const errorLogs = computed(() => hardwareErrorLogStore.errorLogs)
const activeErrorCount = computed(() => hardwareErrorLogStore.activeErrorCount)
const resolvedErrorCount = computed(() => hardwareErrorLogStore.resolvedErrorCount)

// ✅ 필터 변수들 정의
const selectedCategory = ref<string | null>(null)
const selectedSeverity = ref<string | null>(null)
const selectedResolvedStatus = ref<string | null>(null)
const startDate = ref<string>('')
const endDate = ref<string>('')

// ✅ 옵션들 정의 - 실제 데이터에 맞게 수정
const categoryOptions = [
  { label: '전체', value: null },
  { label: '전원', value: 'POWER' },
  { label: '프로토콜', value: 'PROTOCOL' },
  { label: '비상', value: 'EMERGENCY' },
  { label: '서보 전원', value: 'SERVO_POWER' },
  { label: 'Stow', value: 'STOW' },
  { label: '포지셔너', value: 'POSITIONER' },
  { label: '피드', value: 'FEED' }
]

const severityOptions = [
  { label: '전체', value: null },
  { label: '정보', value: 'INFO' },
  { label: '경고', value: 'WARNING' },
  { label: '오류', value: 'ERROR' },
  { label: '치명적', value: 'CRITICAL' }
]

const resolvedStatusOptions = [
  { label: '전체', value: null },
  { label: '해결됨', value: 'resolved' },
  { label: '미해결', value: 'unresolved' }
]

// ✅ 다국어 함수들 정의
const getCurrentMessage = (message: { ko: string; en: string }) => {
  return locale.value === 'ko-KR' ? message.ko : message.en
}

// 실시간 업데이트 상태
const isRealtimeUpdating = computed(() => {
  return hardwareErrorLogStore.isPopupOpen && hardwareErrorLogStore.isInitialLoad
})

// ✅ 필터 값 변경 감지
watch([selectedCategory, selectedSeverity, selectedResolvedStatus, startDate, endDate], () => {
  console.log('🔍 필터 값 변경 감지:', {
    category: selectedCategory.value,
    severity: selectedSeverity.value,
    resolvedStatus: selectedResolvedStatus.value,
    startDate: startDate.value,
    endDate: endDate.value
  })
}, { deep: true })

// 컴포넌트 마운트 시 팝업 상태 설정
onMounted(async () => {
  console.log('🔍 HardwareErrorLogPanel 마운트됨')
  console.log('🔍 현재 errorLogs:', hardwareErrorLogStore.errorLogs)
  console.log('🔍 현재 isInitialLoad:', hardwareErrorLogStore.isInitialLoad)

  await hardwareErrorLogStore.setPopupOpen(true)

  console.log('🔍 팝업 열기 후 errorLogs:', hardwareErrorLogStore.errorLogs)
  console.log('🔍 팝업 열기 후 isInitialLoad:', hardwareErrorLogStore.isInitialLoad)
})

// 컴포넌트 언마운트 시 팝업 상태 해제
onUnmounted(async () => {
  await hardwareErrorLogStore.setPopupOpen(false)
})

const getCategoryName = (category: string) => {
  const categoryNames = {
    'ko-KR': {
      'POWER': '전원',
      'PROTOCOL': '프로토콜',
      'EMERGENCY': '비상',
      'SERVO_POWER': '서보 전원',
      'STOW': 'Stow',
      'POSITIONER': '포지셔너',
      'FEED': '피드'
    },
    'en-US': {
      'POWER': 'Power',
      'PROTOCOL': 'Protocol',
      'EMERGENCY': 'Emergency',
      'SERVO_POWER': 'Servo Power',
      'STOW': 'Stow',
      'POSITIONER': 'Positioner',
      'FEED': 'Feed'
    }
  }

  return categoryNames[locale.value]?.[category] || category
}

const getSeverityName = (severity: string) => {
  const severityNames = {
    'ko-KR': {
      'INFO': '정보',
      'WARNING': '경고',
      'ERROR': '오류',
      'CRITICAL': '치명적'
    },
    'en-US': {
      'INFO': 'Info',
      'WARNING': 'Warning',
      'ERROR': 'Error',
      'CRITICAL': 'Critical'
    }
  }

  return severityNames[locale.value]?.[severity] || severity
}

// ✅ 해결된 에러 개수 계산 - 이 부분을 완전히 제거하세요
// const resolvedErrorCount = computed(() => {
//   return errorLogs.value.filter(log => log.isResolved).length
// })

// ✅ 필터링된 에러 로그
const filteredErrorLogs = computed(() => {
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
  filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())

  console.log('🔍 필터링 완료 - 최종 로그 개수:', filtered.length)
  return filtered
})

// ✅ 필터 적용
const applyFilters = () => {
  // 필터가 변경되면 computed 속성이 자동으로 업데이트됨
  console.log('조회 실행:', {
    category: selectedCategory.value,
    severity: selectedSeverity.value,
    resolvedStatus: selectedResolvedStatus.value,
    startDate: startDate.value,
    endDate: endDate.value
  })
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

// ✅ 심각도별 색상
const getSeverityColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'red'
    case 'ERROR': return 'orange'
    case 'WARNING': return 'yellow'
    case 'INFO': return 'blue'
    default: return 'grey'
  }
}

const getSeverityTextColor = (severity: string) => {
  switch (severity) {
    case 'WARNING': return 'black'
    default: return 'white'
  }
}

// ✅ 시간 포맷팅
const formatTimestamp = (timestamp: string) => {
  return new Date(timestamp).toLocaleString()
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
  min-height: 100vh;
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

.no-logs {
  text-align: center;
  padding: 40px;
  color: var(--theme-text-secondary);
}

.no-logs p {
  margin-top: 16px;
  font-size: 1.1em;
}

/* 달력 아이콘 스타일링은 공통 CSS 파일에서 관리 */
</style>
