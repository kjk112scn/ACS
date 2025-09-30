<template>
  <div class="hardware-error-log-popup">
    <!-- 팝업 헤더 -->
    <div class="popup-header">
      <div class="header-title">
        <q-icon name="bug_report" class="q-mr-sm" />
        하드웨어 에러 로그
      </div>
      <q-btn icon="close" flat round dense @click="closePopup" class="close-button" />
    </div>

    <!-- 에러 로그 내용 -->
    <div class="popup-content">
      <!-- 필터 및 조회 버튼 -->
      <div class="filter-section">
        <!-- 카테고리 필터 -->
        <q-select v-model="selectedCategory" :options="categoryOptions" label="카테고리" dense outlined
          style="min-width: 150px" clearable display-value="전체" />

        <!-- 심각도 필터 -->
        <q-select v-model="selectedSeverity" :options="severityOptions" label="심각도" dense outlined
          style="min-width: 120px" clearable display-value="전체" />

        <!-- 날짜 범위 필터 -->
        <q-input v-model="startDate" label="시작 날짜" type="date" dense outlined style="min-width: 150px" clearable />

        <q-input v-model="endDate" label="종료 날짜" type="date" dense outlined style="min-width: 150px" clearable />

        <!-- 해결 상태 필터 -->
        <q-select v-model="selectedResolvedStatus" :options="resolvedStatusOptions" label="해결 상태" dense outlined
          style="min-width: 120px" clearable display-value="전체" />

        <!-- 조회 버튼 -->
        <q-btn icon="search" label="조회" color="primary" dense @click="applyFilters" />

        <!-- 필터 초기화 버튼 -->
        <q-btn icon="refresh" label="초기화" color="grey" dense @click="resetFilters" />
      </div>

      <!-- 통계 정보 -->
      <div class="stats-section">
        <q-chip color="primary" text-color="white" icon="info">
          전체: {{ filteredLogs.length }}개
        </q-chip>
        <q-chip color="negative" text-color="white" icon="error">
          미해결: {{ unresolvedCount }}개
        </q-chip>
        <q-chip color="positive" text-color="white" icon="check_circle">
          해결됨: {{ resolvedCount }}개
        </q-chip>
      </div>

      <!-- 에러 로그 목록 -->
      <div class="error-log-list">
        <q-list separator>
          <q-item v-for="log in filteredLogs" :key="log.id" class="error-log-item" :class="{
            'error-log-resolved': log.isResolved,
            'error-log-critical': log.severity === 'CRITICAL',
            'error-log-error': log.severity === 'ERROR',
            'error-log-warning': log.severity === 'WARNING',
            'error-log-info': log.severity === 'INFO'
          }">
            <q-item-section avatar>
              <q-icon :name="getSeverityIcon(log.severity)" :color="getSeverityColor(log.severity)" size="sm" />
            </q-item-section>

            <q-item-section>
              <q-item-label class="error-log-message">
                {{ getCurrentMessage(log.message) }}
              </q-item-label>
              <q-item-label caption class="error-log-details">
                <span class="error-log-category">{{ getCategoryName(log.category) }}</span>
                <span class="error-log-severity">{{ getSeverityName(log.severity) }}</span>
                <span class="error-log-component">{{ log.component }}</span>
                <span class="error-log-time">{{ formatTime(log.timestamp) }}</span>
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-chip v-if="log.isResolved" color="positive" text-color="white" size="sm" icon="check_circle">
                해결됨
              </q-chip>
              <q-chip v-else color="negative" text-color="white" size="sm" icon="error">
                미해결
              </q-chip>
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useHardwareErrorLogStore } from '@/stores/hardwareErrorLogStore'
import { useTheme } from '@/composables/useTheme'

const hardwareErrorLogStore = useHardwareErrorLogStore()
const { initializeTheme } = useTheme()

// 상태
const selectedCategory = ref<string | null>(null)
const selectedSeverity = ref<string | null>(null)
const selectedResolvedStatus = ref<string | null>(null)
const startDate = ref<string | null>(null)
const endDate = ref<string | null>(null)

// 계산된 속성
const {
  errorLogs,
  getCurrentMessage,
  getCategoryName,
  getSeverityName
} = hardwareErrorLogStore

// 팝업 닫기
const closePopup = () => {
  if (window.opener) {
    window.close()
  } else {
    // 모달인 경우 - 타입 안전하게 수정
    const closeModal = (window as Window & { $closeModal?: () => void }).$closeModal
    if (closeModal) {
      closeModal()
    }
  }
}

// 필터 옵션
const categoryOptions = [
  { label: '전체', value: null },
  { label: 'Power Status', value: 'POWER' },
  { label: 'Protocol Status', value: 'PROTOCOL' },
  { label: 'Emergency Stop Status', value: 'EMERGENCY' },
  { label: 'Servo Power Status', value: 'SERVO_POWER' },
  { label: 'Stow Pin Status', value: 'STOW' },
  { label: 'Positioner Status', value: 'POSITIONER' },
  { label: 'Feed Status', value: 'FEED' }
]

const severityOptions = [
  { label: '전체', value: null },
  { label: '정보', value: 'INFO' },
  { label: '경고', value: 'WARNING' },
  { label: '오류', value: 'ERROR' },
  { label: '심각', value: 'CRITICAL' }
]

const resolvedStatusOptions = [
  { label: '전체', value: null },
  { label: '미해결', value: 'unresolved' },
  { label: '해결됨', value: 'resolved' }
]

// 필터링된 로그
const filteredLogs = computed(() => {
  let filtered = errorLogs

  // 카테고리 필터
  if (selectedCategory.value) {
    filtered = filtered.filter(log => log.category === selectedCategory.value)
  }

  // 심각도 필터
  if (selectedSeverity.value) {
    filtered = filtered.filter(log => log.severity === selectedSeverity.value)
  }

  // 해결 상태 필터
  if (selectedResolvedStatus.value) {
    if (selectedResolvedStatus.value === 'resolved') {
      filtered = filtered.filter(log => log.isResolved)
    } else if (selectedResolvedStatus.value === 'unresolved') {
      filtered = filtered.filter(log => !log.isResolved)
    }
  }

  // 날짜 범위 필터
  if (startDate.value) {
    const start = new Date(startDate.value)
    filtered = filtered.filter(log => {
      const logDate = new Date(log.timestamp)
      return logDate >= start
    })
  }

  if (endDate.value) {
    const end = new Date(endDate.value)
    end.setHours(23, 59, 59, 999) // 하루 끝까지 포함
    filtered = filtered.filter(log => {
      const logDate = new Date(log.timestamp)
      return logDate <= end
    })
  }

  return filtered
})

// 통계 정보
const unresolvedCount = computed(() =>
  filteredLogs.value.filter(log => !log.isResolved).length
)

const resolvedCount = computed(() =>
  filteredLogs.value.filter(log => log.isResolved).length
)

// 조회 버튼 클릭
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

// 필터 초기화
const resetFilters = () => {
  // 기본값으로 설정 (전체)
  selectedCategory.value = null
  selectedSeverity.value = null
  selectedResolvedStatus.value = null

  // 기본 날짜 설정 (한 달 전 ~ 현재)
  const today = new Date()
  const oneMonthAgo = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate())

  startDate.value = oneMonthAgo.toISOString().split('T')[0]
  endDate.value = today.toISOString().split('T')[0]
}

// 컴포넌트 마운트 시 기본 필터 설정
onMounted(() => {
  // ✅ 간단한 테마 초기화
  initializeTheme()

  // 기본 날짜 설정 (한 달 전 ~ 현재)
  const today = new Date()
  const oneMonthAgo = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate())

  startDate.value = oneMonthAgo.toISOString().split('T')[0]
  endDate.value = today.toISOString().split('T')[0]
})

// 심각도 아이콘
const getSeverityIcon = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'error'
    case 'ERROR': return 'warning'
    case 'WARNING': return 'info'
    case 'INFO': return 'info_outline'
    default: return 'help'
  }
}

// 심각도 색상 - Quasar 색상 사용
const getSeverityColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'negative'
    case 'ERROR': return 'warning'
    case 'WARNING': return 'info'
    case 'INFO': return 'primary'
    default: return 'grey'
  }
}

// 시간 포맷팅
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
/* 간단한 테마 적용 */
.hardware-error-log-popup {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--theme-background, #ffffff);
  color: var(--theme-text, #212121);
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background-color: var(--theme-card-background, #f5f5f5);
  border-bottom: 1px solid var(--theme-border, #e0e0e0);
}

.header-title {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  color: var(--theme-text, #212121);
}

.close-button {
  color: var(--theme-text-secondary, rgba(0, 0, 0, 0.6));
}

.popup-content {
  flex: 1;
  padding: 20px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: var(--theme-background, #ffffff);
}

.filter-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.stats-section {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.error-log-list {
  flex: 1;
  overflow-y: auto;
}

.error-log-item {
  border-left: 4px solid transparent;
  transition: all 0.3s ease;
  background-color: var(--theme-surface, #f5f5f5);
}

.error-log-item:hover {
  background-color: var(--theme-card-background, #ffffff);
}

.error-log-resolved {
  opacity: 0.6;
}

.error-log-critical {
  border-left-color: var(--q-negative, #C10015);
}

.error-log-error {
  border-left-color: var(--q-warning, #F2C037);
}

.error-log-warning {
  border-left-color: var(--q-info, #31CCEC);
}

.error-log-info {
  border-left-color: var(--q-primary, #1976D2);
}

.error-log-message {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--theme-text, #212121);
}

.error-log-details {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.error-log-category,
.error-log-severity,
.error-log-component,
.error-log-time {
  font-size: 0.75rem;
  color: var(--theme-text-secondary, rgba(0, 0, 0, 0.6));
}

.error-log-category {
  font-weight: 600;
  color: var(--theme-primary, #1976D2);
}

.error-log-severity {
  font-weight: 500;
}

.error-log-time {
  margin-left: auto;
}
</style>
