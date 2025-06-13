<template>
  <!-- 팝업 창 모드일 때 -->
  <div v-if="isPopupWindow" class="popup-system-info">
    <div class="system-info-header">
      <h1>🔧 System Information</h1>
      <button @click="handleClose" class="close-button">✕</button>
    </div>

    <div class="system-info-content">
      <!-- 1행: 연결 상태 + 메모리 - 2열 그리드 -->
      <div class="status-grid">
        <div class="status-card connection-card">
          <h3>📡 Connection</h3>
          <div class="status-items">
            <div class="status-item">
              <div
                class="led"
                :class="{ 'led-green': icdStore.isConnected, 'led-red': !icdStore.isConnected }"
              ></div>
              <span>{{ icdStore.isConnected ? 'Connected' : 'Disconnected' }}</span>
            </div>
            <div class="status-item">
              <div
                class="led"
                :class="{ 'led-green': icdStore.isUpdating, 'led-red': !icdStore.isUpdating }"
              ></div>
              <span>{{ icdStore.isUpdating ? 'Updating' : 'Stopped' }}</span>
            </div>
          </div>
        </div>

        <div class="status-card memory-card">
          <h3>💾 Memory</h3>
          <div class="memory-info">
            <div class="memory-bar">
              <div
                class="memory-fill"
                :style="{
                  width: (memoryMonitor.current.used / memoryMonitor.current.total) * 100 + '%',
                }"
                :class="getMemoryUsageClass()"
              ></div>
            </div>
            <div class="memory-text">
              {{ memoryMonitor.current.used }}MB / {{ memoryMonitor.current.total }}MB ({{
                ((memoryMonitor.current.used / memoryMonitor.current.total) * 100).toFixed(1)
              }}%)
            </div>
          </div>
        </div>
      </div>

      <!-- 2행: 성능 통계 - 4열 그리드 -->
      <div class="stats-grid">
        <div class="stat-card">
          <h4>⏱️ Update Interval</h4>
          <div class="stat-value" :class="getIntervalClass(icdStore.updateInterval)">
            {{ icdStore.updateInterval.toFixed(2) }}ms
          </div>
          <div class="stat-sub">Avg: {{ icdStore.updateIntervalStats.average.toFixed(2) }}ms</div>
          <div class="stat-range">
            {{
              icdStore.updateIntervalStats.min === Number.MAX_VALUE
                ? '0'
                : icdStore.updateIntervalStats.min.toFixed(1)
            }}
            - {{ icdStore.updateIntervalStats.max.toFixed(1) }}ms
          </div>
        </div>

        <div class="stat-card">
          <h4>🎯 Timer Accuracy</h4>
          <div class="timer-grid">
            <div class="timer-item">
              <span class="timer-label">On Time:</span>
              <span class="timer-value text-positive">{{ icdStore.timerStats?.onTime || 0 }}</span>
            </div>
            <div class="timer-item">
              <span class="timer-label">Early:</span>
              <span class="timer-value text-warning">{{ icdStore.timerStats?.early || 0 }}</span>
            </div>
            <div class="timer-item">
              <span class="timer-label">Late:</span>
              <span class="timer-value text-negative">{{ icdStore.timerStats?.late || 0 }}</span>
            </div>
          </div>
          <div class="stat-sub">Drift: {{ (icdStore.driftCorrection || 0).toFixed(2) }}ms</div>
        </div>

        <div class="stat-card">
          <h4>⚡ Processing</h4>
          <div class="stat-value">{{ icdStore.messageDelay.toFixed(2) }}ms</div>
          <div class="stat-sub">Avg: {{ icdStore.messageDelayStats.average.toFixed(2) }}ms</div>
          <div class="stat-range">
            {{
              icdStore.messageDelayStats.min === Number.MAX_VALUE
                ? '0'
                : icdStore.messageDelayStats.min.toFixed(1)
            }}
            - {{ icdStore.messageDelayStats.max.toFixed(1) }}ms
          </div>
        </div>

        <div class="stat-card">
          <h4>📊 Statistics</h4>
          <div class="detail-items">
            <div class="detail-item">
              <span>Count:</span>
              <span>{{ icdStore.updateCount.toLocaleString() }}</span>
            </div>
            <div class="detail-item">
              <span>Peak:</span>
              <span>{{ memoryMonitor.peak.used }}MB</span>
            </div>
            <div class="detail-item">
              <span>Limit:</span>
              <span>{{ memoryMonitor.current.limit }}MB</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 3행: 액션 버튼 - 전체 너비 -->
      <div class="action-section">
        <button @click="refreshSystemInfo" class="action-btn primary">
          🔄 Refresh System Info
        </button>
        <button @click="resetDelayStats" class="action-btn secondary">📊 Reset Statistics</button>
      </div>
    </div>
  </div>
  <!-- 모달 모드일 때 -->
  <div v-else-if="isModalMode" class="transparent-modal-content">
    <!-- 모달 모드는 기존 코드와 동일하게 유지 -->
    <div class="modal-overlay" @click="handleClose">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>🔧 System Information</h2>
          <button @click="handleClose" class="modal-close-btn">✕</button>
        </div>
        <div class="modal-body">
          <!-- 위의 팝업 내용과 동일한 구조 -->
        </div>
      </div>
    </div>
  </div>

  <!-- 다이얼로그 모드일 때 (기존 Quasar Dialog) -->
  <q-dialog v-else-if="modelValue !== undefined" v-model="isOpen" persistent>
    <q-card class="info-system-modal">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">System Information</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <q-card-section class="q-pt-none">
        <div class="info-grid">
          <!-- Connection Status 카드 -->
          <q-card class="connection-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary q-mb-md row items-center">
                <span>Connection Status</span>
                <q-space />
                <q-btn
                  icon="refresh"
                  size="sm"
                  flat
                  round
                  color="primary"
                  @click="resetDelayStats"
                  class="q-ml-sm"
                >
                  <q-tooltip>Reset Delay Statistics</q-tooltip>
                </q-btn>
              </div>

              <div class="connection-info">
                <div class="connection-item">
                  <div
                    class="connection-led"
                    :class="{
                      'led-green': icdStore.isConnected,
                      'led-red': !icdStore.isConnected,
                    }"
                  ></div>
                  <span class="connection-label">WebSocket</span>
                  <span class="connection-value">
                    {{ icdStore.isConnected ? 'Connected' : 'Disconnected' }}
                  </span>
                </div>

                <div class="connection-item">
                  <div
                    class="connection-led"
                    :class="{
                      'led-green': icdStore.isUpdating,
                      'led-red': !icdStore.isUpdating,
                    }"
                  ></div>
                  <span class="connection-label">Data Update</span>
                  <span class="connection-value">
                    {{ icdStore.isUpdating ? 'Active' : 'Inactive' }}
                  </span>
                </div>

                <!-- 30ms 주기 정보 -->
                <div class="update-interval-section q-mt-md">
                  <div class="interval-title">Update Interval (Target: 30ms):</div>
                  <div class="interval-stats">
                    <div class="interval-stat-item">
                      <span class="interval-stat-label">Current:</span>
                      <span
                        class="interval-stat-value"
                        :class="getIntervalClass(icdStore.updateInterval)"
                      >
                        {{ icdStore.updateInterval.toFixed(2) }}ms
                      </span>
                    </div>
                    <div class="interval-stat-item">
                      <span class="interval-stat-label">Average:</span>
                      <span
                        class="interval-stat-value"
                        :class="getIntervalClass(icdStore.updateIntervalStats.average)"
                      >
                        {{ icdStore.updateIntervalStats.average.toFixed(2) }}ms
                      </span>
                    </div>
                    <div class="interval-stat-item">
                      <span class="interval-stat-label">Min / Max:</span>
                      <span class="interval-stat-value">
                        {{
                          icdStore.updateIntervalStats.min === Number.MAX_VALUE
                            ? '0.00'
                            : icdStore.updateIntervalStats.min.toFixed(2)
                        }}ms / {{ icdStore.updateIntervalStats.max.toFixed(2) }}ms
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Timer Accuracy 섹션 -->
                <div class="timer-accuracy-section q-mt-md">
                  <div class="timer-title">Timer Accuracy:</div>
                  <div class="timer-stats">
                    <div class="timer-stat-item">
                      <span class="timer-stat-label">On Time:</span>
                      <span class="timer-stat-value text-positive">{{
                        icdStore.timerStats?.onTime || 0
                      }}</span>
                    </div>
                    <div class="timer-stat-item">
                      <span class="timer-stat-label">Early:</span>
                      <span class="timer-stat-value text-warning">{{
                        icdStore.timerStats?.early || 0
                      }}</span>
                    </div>
                    <div class="timer-stat-item">
                      <span class="timer-stat-label">Late:</span>
                      <span class="timer-stat-value text-negative">{{
                        icdStore.timerStats?.late || 0
                      }}</span>
                    </div>
                    <div class="timer-stat-item">
                      <span class="timer-stat-label">Drift:</span>
                      <span class="timer-stat-value"
                        >{{ (icdStore.driftCorrection || 0).toFixed(2) }}ms</span
                      >
                    </div>
                  </div>
                </div>
              </div>

              <div class="connection-stats q-mt-md">
                <div class="stat-item">
                  <span class="stat-label">Update Count:</span>
                  <span class="stat-value">{{ icdStore.updateCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Processing Delay:</span>
                  <span class="stat-value">{{ icdStore.messageDelay.toFixed(2) }}ms</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Avg Processing:</span>
                  <span class="stat-value"
                    >{{ icdStore.messageDelayStats.average.toFixed(2) }}ms</span
                  >
                </div>
                <div class="stat-item">
                  <span class="stat-label">Min / Max Processing:</span>
                  <span class="stat-value">
                    {{
                      icdStore.messageDelayStats.min === Number.MAX_VALUE
                        ? '0.00'
                        : icdStore.messageDelayStats.min.toFixed(2)
                    }}ms / {{ icdStore.messageDelayStats.max.toFixed(2) }}ms
                  </span>
                </div>
              </div>

              <!-- Memory Monitor 섹션 -->
              <div class="memory-section q-mt-md">
                <div class="memory-title">Memory Monitor:</div>
                <div class="memory-stats">
                  <div class="memory-stat-item">
                    <span class="memory-stat-label">Current:</span>
                    <span class="memory-stat-value">
                      {{ memoryMonitor.current.used }}MB / {{ memoryMonitor.current.total }}MB
                    </span>
                  </div>
                  <div class="memory-stat-item">
                    <span class="memory-stat-label">Peak:</span>
                    <span class="memory-stat-value">
                      {{ memoryMonitor.peak.used }}MB / {{ memoryMonitor.peak.total }}MB
                    </span>
                  </div>
                  <div class="memory-stat-item">
                    <span class="memory-stat-label">Usage:</span>
                    <span class="memory-stat-value" :class="getMemoryUsageClass()">
                      {{
                        ((memoryMonitor.current.used / memoryMonitor.current.total) * 100).toFixed(
                          1,
                        )
                      }}%
                    </span>
                  </div>
                  <div class="memory-stat-item">
                    <span class="memory-stat-label">Available:</span>
                    <span class="memory-stat-value"> {{ memoryMonitor.current.limit }}MB </span>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="새로고침" color="primary" @click="refreshSystemInfo" />
        <q-btn flat label="닫기" color="grey-7" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import { useICDStore } from '../../stores/icd/icdStore'

// Props
interface Props {
  modelValue?: boolean
  modalId?: string
  modalTitle?: string
  isPopup?: boolean
}

const props = defineProps<Props>()
// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

// Store & Route
const icdStore = useICDStore()
const route = useRoute()
const instance = getCurrentInstance()

// 모드 감지
const isPopupWindow = computed(() => {
  // 1. props로 전달된 isPopup 확인
  if (props.isPopup) return true

  // 2. URL 경로로 팝업 모드 감지
  if (route.path.startsWith('/popup/')) return true

  // 3. window.opener 존재 여부로 팝업 창 감지
  if (window.opener !== null) return true

  return false
})

const isModalMode = computed(() => !!props.modalId)
const isDialogMode = computed(() => props.modelValue !== undefined)

// Computed for template
const isOpen = computed({
  get: () => props.modelValue ?? false,
  set: (value: boolean) => {
    if (props.modelValue !== undefined) {
      emit('update:modelValue', value)
    }
  },
})

// 메모리 정보 타입 정의
interface MemoryInfo {
  usedJSHeapSize: number
  totalJSHeapSize: number
  jsHeapSizeLimit: number
}

interface PerformanceWithMemory extends Performance {
  memory?: MemoryInfo
}

// 실시간 메모리 모니터링
const memoryMonitor = ref({
  current: { used: 0, total: 0, limit: 0 },
  peak: { used: 0, total: 0 },
  history: [] as Array<{ time: number; used: number; total: number }>,
})

// 브라우저 성능 정보
const getBrowserPerformance = () => {
  try {
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
    const gpuAcceleration = gl ? 'ON' : 'OFF'

    const performanceWithMemory = performance as PerformanceWithMemory
    const memoryInfo = performanceWithMemory.memory
    const memoryData = memoryInfo
      ? {
          used: Math.round(memoryInfo.usedJSHeapSize / 1024 / 1024),
          total: Math.round(memoryInfo.totalJSHeapSize / 1024 / 1024),
          limit: Math.round(memoryInfo.jsHeapSizeLimit / 1024 / 1024),
        }
      : null

    console.log('🖥️ 시스템 정보:')
    console.log('GPU 가속:', gpuAcceleration)
    if (memoryData) {
      console.log(
        `메모리: ${memoryData.used}MB / ${memoryData.total}MB (한계: ${memoryData.limit}MB)`,
      )
    }

    return { gpuAcceleration, memoryData }
  } catch (error) {
    console.log('시스템 정보를 가져올 수 없습니다:', error)
    return { gpuAcceleration: 'UNKNOWN', memoryData: null }
  }
}

// 업데이트 간격에 따른 색상 클래스 결정
const getIntervalClass = (interval: number) => {
  if (interval === 0) return ''
  if (interval >= 28 && interval <= 32) return 'interval-good'
  if (interval >= 25 && interval <= 35) return 'interval-warning'
  return 'interval-error'
}

// 메모리 사용량 색상 클래스
const getMemoryUsageClass = () => {
  const usage = (memoryMonitor.value.current.used / memoryMonitor.value.current.total) * 100
  if (usage < 70) return 'text-positive' // 녹색 (정상)
  if (usage < 85) return 'text-warning' // 주황색 (주의)
  return 'text-negative' // 빨간색 (위험)
}

// 범용 닫기 함수
const handleClose = () => {
  console.log('🚪 닫기 요청 - 모드:', {
    isPopupWindow: isPopupWindow.value,
    isModalMode: isModalMode.value,
    isDialogMode: isDialogMode.value,
  })

  try {
    if (isPopupWindow.value) {
      // 팝업 창 모드
      console.log('🪟 팝업 창 닫기 시도')

      // 부모 창에 닫기 알림
      if (window.opener && !window.opener.closed) {
        try {
          window.opener.postMessage(
            {
              type: 'popup-closing',
              timestamp: Date.now(),
            },
            window.location.origin,
          )
        } catch (error) {
          console.warn('⚠️ 부모 창 통신 실패:', error)
        }
      }

      // 창 닫기
      window.close()

      // 브라우저에서 창 닫기가 실패할 경우 대비
      setTimeout(() => {
        if (!window.closed) {
          console.warn('⚠️ 자동 창 닫기 실패 - 사용자 액션 필요')
          alert('창을 수동으로 닫아주세요. (Alt+F4 또는 Ctrl+W)')
        }
      }, 100)
    } else if (isModalMode.value) {
      // 모달 모드
      console.log('📱 모달 닫기')
      const globalProperties = instance?.appContext.config.globalProperties
      if (globalProperties?.$closeModal) {
        globalProperties.$closeModal()
      } else {
        console.error('❌ 전역 closeModal 함수를 찾을 수 없음')
      }
    } else {
      // 다이얼로그 모드
      console.log('🔲 다이얼로그 닫기')
      emit('update:modelValue', false)
      emit('close')
    }
  } catch (error) {
    console.error('❌ 닫기 처리 중 오류:', error)

    // 폴백 처리
    if (isPopupWindow.value) {
      alert('창을 수동으로 닫아주세요.')
    } else {
      emit('update:modelValue', false)
      emit('close')
    }
  }
}

// Methods
const refreshSystemInfo = () => {
  console.log('🔄 시스템 정보 새로고침')
  console.log('Connection Status:', {
    isConnected: icdStore.isConnected,
    isUpdating: icdStore.isUpdating,
    updateCount: icdStore.updateCount,
    messageDelay: icdStore.messageDelay,
  })

  getBrowserPerformance()
  updateMemoryInfo()
}

// 지연 통계 초기화 함수
const resetDelayStats = () => {
  console.log('🔄 지연 통계 초기화')
  icdStore.resetMessageDelayStats()
}

// 메모리 정보 업데이트 함수
const updateMemoryInfo = () => {
  try {
    const performanceWithMemory = performance as PerformanceWithMemory
    const memoryInfo = performanceWithMemory.memory

    if (memoryInfo) {
      const current = {
        used: Math.round(memoryInfo.usedJSHeapSize / 1024 / 1024),
        total: Math.round(memoryInfo.totalJSHeapSize / 1024 / 1024),
        limit: Math.round(memoryInfo.jsHeapSizeLimit / 1024 / 1024),
      }

      memoryMonitor.value.current = current

      // Peak 값 업데이트
      if (current.used > memoryMonitor.value.peak.used) {
        memoryMonitor.value.peak.used = current.used
      }
      if (current.total > memoryMonitor.value.peak.total) {
        memoryMonitor.value.peak.total = current.total
      }

      // 히스토리 추가 (최근 10개만 유지)
      memoryMonitor.value.history.push({
        time: Date.now(),
        used: current.used,
        total: current.total,
      })

      if (memoryMonitor.value.history.length > 10) {
        memoryMonitor.value.history.shift()
      }
    }
  } catch (error) {
    console.error('메모리 정보 업데이트 실패:', error)
  }
}

// 5초마다 메모리 정보 업데이트
let memoryTimer: number | null = null

// 라이프사이클 관리

onMounted(async () => {
  console.log('🔧 SystemInfo 컴포넌트 마운트됨')
  console.log('🔍 실행 환경:', {
    isPopupWindow: isPopupWindow.value,
    isModalMode: isModalMode.value,
    isDialogMode: isDialogMode.value,
    modalId: props.modalId,
    currentUrl: window.location.href,
  })

  // ✅ 팝업 모드에서 icdStore 초기화 추가
  if (isPopupWindow.value) {
    console.log('🚀 팝업 모드 - icdStore 초기화 시작')
    try {
      await icdStore.initialize()
      console.log('✅ 팝업 모드 - icdStore 초기화 완료')
    } catch (error) {
      console.error('❌ 팝업 모드 - icdStore 초기화 실패:', error)
    }
  }

  getBrowserPerformance()
  updateMemoryInfo()

  // 5초마다 메모리 모니터링 시작
  memoryTimer = window.setInterval(updateMemoryInfo, 5000)

  // 팝업 창인 경우 제목 설정
  if (isPopupWindow.value) {
    document.title = '🔧 System Information - GTL ACS'
  }
})

onUnmounted(() => {
  console.log('🧹 SystemInfo 컴포넌트 언마운트됨')

  // 타이머 정리
  if (memoryTimer) {
    clearInterval(memoryTimer)
    memoryTimer = null
  }

  // ✅ 팝업 모드에서 icdStore 정리 추가
  if (isPopupWindow.value) {
    console.log('🧹 팝업 모드 - icdStore 정리')
    icdStore.cleanup()
  }

  // 팝업 창인 경우 부모 창에 종료 알림
  if (isPopupWindow.value && window.opener && !window.opener.closed) {
    try {
      window.opener.postMessage(
        {
          type: 'popup-unmounted',
          timestamp: Date.now(),
        },
        window.location.origin,
      )
    } catch (error) {
      console.warn('⚠️ 부모 창 종료 알림 실패:', error)
    }
  }
})
</script>
<style scoped>
/* 팝업 모드 최적화 스타일 - DashboardPage 스타일 적용 */
.popup-system-info {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--q-page-background, #f5f5f5); /* DashboardPage와 동일한 배경 */
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow: hidden;
}

/* ✅ 헤더를 DashboardPage 스타일로 변경 */
.system-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1.2rem;
  background: var(--q-primary); /* Quasar 기본 primary 색상 */
  color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
  min-height: 42px;
}

.system-info-header h1 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: white;
}

.header-stats {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}

.quick-stat {
  font-size: 0.8rem;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.15);
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  color: white;
}

.close-button {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.25);
}

.system-info-content {
  flex: 1;

  padding: 1rem;
  display: flex;
  flex-direction: column;

  gap: 1.5rem; /* 간격 증가 */
  overflow-y: auto;

  min-height: 0; /* 플렉스 아이템이 축소될 수 있도록 */
}

/* ✅ 1행: 상태 그리드 - DashboardPage 카드 스타일 적용 */
.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  height: 140px;
}

.status-card {
  background: white;
  border-radius: 4px; /* DashboardPage와 동일한 border-radius */
  border: 1px solid var(--q-primary);
  border-top: 3px solid var(--q-primary); /* DashboardPage axis-card 스타일 */

  padding: 1.5rem 2rem; /* 좌우 여백 증가 */
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.12),
    0 1px 2px rgba(0, 0, 0, 0.24);
  display: flex;
  flex-direction: column;
}

.connection-card {
  border-top-color: #1976d2; /* primary 색상 */
}

.memory-card {
  border-top-color: #00acc1; /* info 색상 */
}

.status-card h3 {
  margin: 0 0 1.5rem 0;
  font-size: 1rem;
  font-weight: bold;
  color: var(--q-primary);
}

.status-items {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  flex: 1;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.9rem;
}

/* ✅ LED를 DashboardPage status-led 스타일로 변경 */
.led {
  width: 20px; /* DashboardPage와 동일 */
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.led-green {
  background-color: #4caf50; /* DashboardPage와 동일한 녹색 */
  box-shadow:
    0 0 12px #4caf50,
    0 0 24px #4caf50;
}

.led-red {
  background-color: #f44336; /* DashboardPage와 동일한 빨간색 */
  box-shadow:
    0 0 12px #f44336,
    0 0 24px #f44336;
}

/* ✅ 메모리 카드 스타일 개선 */
.memory-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
}

.memory-bar {
  width: 100%;
  height: 24px;
  background: #e0e0e0; /* 더 부드러운 회색 */
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  border: 1px solid #d0d0d0;
}

.memory-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.3s ease;
}

.memory-fill.text-positive {
  background: linear-gradient(90deg, #4caf50, #66bb6a); /* DashboardPage 녹색 톤 */
}

.memory-fill.text-warning {
  background: linear-gradient(90deg, #ff9800, #ffb74d); /* DashboardPage 주황색 톤 */
}

.memory-fill.text-negative {
  background: linear-gradient(90deg, #f44336, #ef5350); /* DashboardPage 빨간색 톤 */
}

.memory-text {
  font-size: 0.9rem;
  text-align: center;
  color: #424242;
  font-weight: 500;
}

/* ✅ 2행: 통계 그리드 - DashboardPage 스타일 적용 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;

  gap: 1.5rem; /* 간격 확대 */
  height: 160px;
}

.stat-card {
  background: white;
  border-radius: 4px;
  border: 1px solid var(--q-primary);
  border-top: 3px solid var(--q-primary);

  padding: 1rem 1.5rem; /* 전체 여백 증가 */
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.12),
    0 1px 2px rgba(0, 0, 0, 0.24);
  display: flex;
  flex-direction: column;
  text-align: center;
}

/* 각 stat-card에 고유 색상 적용 */
.stat-card:nth-child(1) {
  border-top-color: #ff5722; /* azimuth 색상 */
}

.stat-card:nth-child(2) {
  border-top-color: #2196f3; /* elevation 색상 */
}

.stat-card:nth-child(3) {
  border-top-color: #4caf50; /* tilt 색상 */
}

.stat-card:nth-child(4) {
  border-top-color: #9c27b0; /* statistics 색상 */
}

.stat-card h4 {
  margin: 0 0 0.8rem 0;
  font-size: 0.85rem;
  color: var(--q-primary);
  font-weight: bold; /* DashboardPage와 동일 */
}

.stat-value {
  font-size: 1.3rem;
  font-weight: bold;
  font-family: 'Courier New', monospace;
  margin: 0.5rem 0;
  color: #1976d2; /* primary 색상 */
}

.stat-sub {
  font-size: 0.75rem;
  color: #757575;
  margin: 0.25rem 0;
}

.stat-range {
  font-size: 0.7rem;
  color: #9e9e9e;
  margin-top: auto;
}

/* 타이머 그리드 */
.timer-grid {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  flex: 1;
}

.timer-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
}

.timer-label {
  color: #757575;
}

.timer-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* Statistics 카드의 detail-items */
.detail-items {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  flex: 1;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
}

.detail-item span:first-child {
  color: #757575;
}

.detail-item span:last-child {
  font-weight: 600;
  font-family: 'Courier New', monospace;
  color: #424242;
}

/* ✅ 3행: 액션 섹션 - DashboardPage 버튼 스타일 적용 */
.action-section {
  margin-top: 1.5rem;
  padding: 1.2rem 1rem;
}

.action-btn {
  padding: 0.6rem 1.8rem;
  border: none;
  border-radius: 4px; /* DashboardPage와 동일 */
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  transition: all 0.2s ease;
  min-width: 160px;
  text-transform: none; /* Quasar 기본값과 동일 */
}

.action-btn.primary {
  background: var(--q-primary);
  color: white;
}

.action-btn.primary:hover {
  background: var(--q-primary);
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.3);
}

.action-btn.secondary {
  background: var(--q-warning);
  color: white;
}

.action-btn.secondary:hover {
  background: var(--q-warning);
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(255, 152, 0, 0.3);
}

/* ✅ 상태별 색상 클래스 - DashboardPage와 동일 */
.interval-good {
  color: #4caf50;
}

.interval-warning {
  color: #ff9800;
}

.interval-error {
  color: #f44336;
}

.text-positive {
  color: #4caf50;
}

.text-warning {
  color: #ff9800;
}

.text-negative {
  color: #f44336;
}

/* ✅ 다크 모드 지원 - DashboardPage와 동일한 방식 */
.body--dark .popup-system-info {
  background: var(--q-dark-page);
}

.body--dark .system-info-header {
  background: var(--q-primary);
}

.body--dark .status-card,
.body--dark .stat-card,
.body--dark .action-section {
  background: var(--q-dark);
  color: white;
  border-color: var(--q-primary);
}

.body--dark .status-card h3,
.body--dark .stat-card h4 {
  color: var(--q-primary);
}

.body--dark .timer-label,
.body--dark .stat-sub,
.body--dark .stat-range,
.body--dark .detail-item span:first-child {
  color: rgba(255, 255, 255, 0.7);
}

.body--dark .memory-bar {
  background: #424242;
  border-color: #616161;
}

.body--dark .memory-text {
  color: rgba(255, 255, 255, 0.8);
}

.body--dark .stat-value {
  color: var(--q-primary);
}

.body--dark .detail-item span:last-child {
  color: rgba(255, 255, 255, 0.9);
}

/* 스크롤바 스타일링 - DashboardPage와 유사 */
.system-info-content::-webkit-scrollbar {
  width: 6px;
}

.system-info-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.system-info-content::-webkit-scrollbar-thumb {
  background: var(--q-primary);
  border-radius: 3px;
  opacity: 0.5;
}

.system-info-content::-webkit-scrollbar-thumb:hover {
  opacity: 0.8;
}

/* ✅ 반응형 - DashboardPage와 동일한 breakpoint */
@media (max-width: 1279px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr 1fr;
    height: auto;
  }

  .stat-card:nth-child(4) {
    grid-column: 1 / -1;
    max-width: 300px;
    margin: 0 auto;
  }
}

@media (max-width: 959px) {
  .system-info-content {
    padding: 1.2rem 1.5rem; /* 모바일에서도 충분한 여백 */
  }
}

@media (max-width: 767px) {
  .system-info-header {
    padding: 0.4rem 0.8rem;
  }

  .system-info-header h1 {
    font-size: 1rem;
  }

  .header-stats {
    gap: 0.6rem;
  }

  .quick-stat {
    font-size: 0.75rem;
    padding: 0.15rem 0.3rem;
  }

  .system-info-content {
    padding: 0.6rem;
    gap: 1rem;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    height: auto;
  }

  .stat-card {
    text-align: left;
    padding: 0.8rem;
  }

  .timer-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
  }

  .timer-item {
    flex-direction: column;
    gap: 0.2rem;
  }

  .action-section {
    padding: 0.6rem;
    gap: 0.8rem;
  }

  .action-btn {
    min-width: 140px;
    padding: 0.5rem 1.2rem;
    font-size: 0.85rem;
  }
}

@media (max-width: 599px) {
  .system-info-header {
    padding: 0.3rem 0.6rem;
    min-height: 36px;
  }

  .system-info-header h1 {
    font-size: 0.9rem;
  }

  .header-stats {
    gap: 0.4rem;
  }

  .quick-stat {
    font-size: 0.7rem;
    padding: 0.1rem 0.25rem;
  }

  .close-button {
    padding: 3px 6px;
    font-size: 12px;
  }

  .system-info-content {
    padding: 0.5rem;
    gap: 0.8rem;
  }

  .status-card,
  .stat-card {
    padding: 0.8rem;
  }

  .status-card h3,
  .stat-card h4 {
    font-size: 0.85rem;
    margin-bottom: 0.4rem;
  }

  .stat-value {
    font-size: 1.1rem;
  }

  .memory-bar {
    height: 18px;
  }

  .memory-text {
    font-size: 0.8rem;
  }

  .action-section {
    padding: 0.5rem;
  }

  .action-btn {
    min-width: 120px;
    padding: 0.4rem 1rem;
    font-size: 0.8rem;
  }
}

/* 높이 제한 대응 */
@media (max-height: 750px) {
  .status-grid {
    height: 180px; /* 여전히 충분한 높이 유지 */
  }

  .action-section {
    margin-top: 1rem; /* 간격 유지 */
  }
}

@media (max-height: 650px) {
  .system-info-header {
    min-height: 36px;
    padding: 0.4rem 1rem;
  }

  .system-info-header h1 {
    font-size: 1rem;
  }

  .system-info-content {
    padding: 0.6rem;
    gap: 0.8rem;
  }

  .status-grid {
    height: 100px;
  }

  .stats-grid {
    height: 120px;
  }

  .status-card,
  .stat-card {
    padding: 0.6rem;
  }

  .stat-value {
    font-size: 1.1rem;
  }

  .status-card h3,
  .stat-card h4 {
    font-size: 0.8rem;
    margin-bottom: 0.4rem;
  }

  .action-section {
    padding: 0.5rem;
  }

  .action-btn {
    padding: 0.4rem 1.2rem;
    font-size: 0.85rem;
    min-width: 140px;
  }
}

/* 애니메이션 - DashboardPage와 유사한 효과 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.status-card,
.stat-card,
.action-section {
  animation: fadeInUp 0.4s ease-out;
}

.status-card:nth-child(1) {
  animation-delay: 0.1s;
}
.status-card:nth-child(2) {
  animation-delay: 0.2s;
}
.stat-card:nth-child(1) {
  animation-delay: 0.3s;
}
.stat-card:nth-child(2) {
  animation-delay: 0.4s;
}
.stat-card:nth-child(3) {
  animation-delay: 0.5s;
}
.stat-card:nth-child(4) {
  animation-delay: 0.6s;
}
.action-section {
  animation-delay: 0.7s;
}

/* LED 애니메이션 - DashboardPage와 동일 */
@keyframes ledPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.05);
  }
}

.led-green,
.led-red {
  animation: ledPulse 2s infinite ease-in-out;
}

/* 메모리 바 애니메이션 */
@keyframes memoryFill {
  from {
    width: 0%;
  }
}

.memory-fill {
  animation: memoryFill 1.2s ease-out;
}

/* 호버 효과 - DashboardPage와 유사 */
.status-card:hover,
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.action-section:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.12);
  transition: all 0.3s ease;
}

/* 포커스 스타일 - 접근성 개선 */
.action-btn:focus {
  outline: 2px solid var(--q-primary);
  outline-offset: 2px;
}

.close-button:focus {
  outline: 2px solid white;
  outline-offset: 2px;
}

/* 접근성 개선 */
@media (prefers-reduced-motion: reduce) {
  .status-card,
  .stat-card,
  .action-section {
    animation: none;
  }

  .led-green,
  .led-red {
    animation: none;
  }

  .memory-fill {
    animation: none;
  }

  .action-btn:hover,
  .status-card:hover,
  .stat-card:hover,
  .action-section:hover {
    transform: none;
  }
}

/* 고대비 모드 */
@media (prefers-contrast: high) {
  .status-card,
  .stat-card,
  .action-section {
    border: 2px solid #000;
    border-top-width: 4px;
  }

  .led-green {
    background-color: #00ff00;
    box-shadow: none;
  }

  .led-red {
    background-color: #ff0000;
    box-shadow: none;
  }

  .memory-fill.text-positive {
    background: #00ff00;
  }

  .memory-fill.text-warning {
    background: #ffaa00;
  }

  .memory-fill.text-negative {
    background: #ff0000;
  }
}

/* 인쇄 스타일 */
@media print {
  .popup-system-info {
    background: white !important;
  }

  .system-info-header {
    background: #f5f5f5 !important;
    color: black !important;
    min-height: 30px !important;
    padding: 0.3rem 0.5rem !important;
    border-bottom: 2px solid #000;
  }

  .close-button,
  .action-section {
    display: none !important;
  }

  .status-card,
  .stat-card {
    box-shadow: none;
    border: 1px solid #000;
    border-top: 3px solid #000;
    break-inside: avoid;
    padding: 0.5rem !important;
  }

  .led-green,
  .led-red {
    box-shadow: none;
    border: 2px solid #000;
  }

  .memory-bar {
    border: 1px solid #000;
  }

  .system-info-content {
    gap: 0.5rem !important;
    padding: 0.5rem !important;
  }

  .stat-value {
    color: #000 !important;
  }
}

/* 터치 디바이스 대응 */
@media (hover: none) and (pointer: coarse) {
  .action-btn {
    min-height: 44px; /* 터치 타겟 최소 크기 */
    padding: 0.7rem 1.8rem;
  }

  .close-button {
    min-width: 36px;
    min-height: 36px;
    padding: 6px 8px;
  }

  .status-card:hover,
  .stat-card:hover,
  .action-section:hover {
    transform: none;
  }

  /* 터치 피드백 */
  .action-btn:active {
    transform: scale(0.98);
    transition: transform 0.1s ease;
  }
}

/* 로딩 상태 스타일 */
.loading-shimmer {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 에러 상태 스타일 */
.error-state {
  color: var(--q-negative);
  background: rgba(244, 67, 54, 0.1);
  border-color: var(--q-negative);
}

/* 성공 상태 스타일 */
.success-state {
  color: var(--q-positive);
  background: rgba(76, 175, 80, 0.1);
  border-color: var(--q-positive);
}

/* 경고 상태 스타일 */
.warning-state {
  color: var(--q-warning);
  background: rgba(255, 152, 0, 0.1);
  border-color: var(--q-warning);
}
</style>
