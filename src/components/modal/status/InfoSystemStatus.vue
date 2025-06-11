<template>
  <q-dialog v-model="isOpen" persistent>
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
import { computed, ref, watch } from 'vue'
import { useICDStore } from '../../../stores/icd/icdStore'

// Props
interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// Store
const icdStore = useICDStore()

// 메모리 정보 타입 정의
interface MemoryInfo {
  usedJSHeapSize: number
  totalJSHeapSize: number
  jsHeapSizeLimit: number
}

interface PerformanceWithMemory extends Performance {
  memory?: MemoryInfo
}

// Computed
const isOpen = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
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
}

// 지연 통계 초기화 함수
const resetDelayStats = () => {
  console.log('🔄 지연 통계 초기화')
  icdStore.resetMessageDelayStats()
}

// 실시간 메모리 모니터링
const memoryMonitor = ref({
  current: { used: 0, total: 0, limit: 0 },
  peak: { used: 0, total: 0 },
  history: [] as Array<{ time: number; used: number; total: number }>,
})

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

watch(isOpen, (newValue) => {
  if (newValue) {
    console.log('📊 InfoSystem 모달 열림')
    getBrowserPerformance()
    updateMemoryInfo()

    // 5초마다 메모리 모니터링 시작
    memoryTimer = window.setInterval(updateMemoryInfo, 5000)
  } else {
    // 모달 닫힐 때 타이머 정리
    if (memoryTimer) {
      clearInterval(memoryTimer)
      memoryTimer = null
    }
  }
})

const getMemoryUsageClass = () => {
  const usage = (memoryMonitor.value.current.used / memoryMonitor.value.current.total) * 100
  if (usage < 70) return 'text-positive' // 녹색 (정상)
  if (usage < 85) return 'text-warning' // 주황색 (주의)
  return 'text-negative' // 빨간색 (위험)
}
</script>

<style scoped>
.info-system-modal {
  min-width: 800px;
  max-width: 1000px;
  width: 90vw;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

/* Connection Status Card */
.connection-status-card {
  border: 1px solid var(--q-info);
  border-top: 3px solid var(--q-info);
}

.connection-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.connection-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
}

.connection-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.connection-led {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  position: relative;
}

.connection-led::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.led-green {
  background-color: #4caf50;
  box-shadow:
    0 0 8px #4caf50,
    0 0 16px #4caf50;
}

.led-red {
  background-color: #f44336;
  box-shadow:
    0 0 8px #f44336,
    0 0 16px #f44336;
}

.led-green::before {
  background: radial-gradient(circle, rgba(76, 175, 80, 0.3) 0%, transparent 70%);
  animation: pulse-green 2s infinite;
}

.led-red::before {
  background: radial-gradient(circle, rgba(244, 67, 54, 0.3) 0%, transparent 70%);
  animation: pulse-red 2s infinite;
}

.connection-label {
  font-weight: 500;
  min-width: 100px;
  flex-shrink: 0;
}

.connection-value {
  font-weight: 600;
  color: var(--q-info);
  margin-left: auto;
}

.connection-item:has(.led-green) .connection-value {
  color: #4caf50;
}

.connection-item:has(.led-red) .connection-value {
  color: #f44336;
}

.connection-stats {
  border-top: 1px solid #e0e0e0;
  padding-top: 1rem;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.stat-label {
  font-weight: 500;
  color: #666;
}

.stat-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* Update Interval 섹션 */
.update-interval-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 0.75rem;
}

.interval-title {
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.interval-stats {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.interval-stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.interval-stat-label {
  font-weight: 500;
  color: #777;
}

.interval-stat-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* 간격 상태별 색상 */
.interval-good {
  color: #4caf50;
}

.interval-warning {
  color: #ff9800;
}

.interval-error {
  color: #f44336;
}

/* Timer Accuracy 섹션 */
.timer-accuracy-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 0.75rem;
}

.timer-title {
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.timer-stats {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.timer-stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.timer-stat-label {
  font-weight: 500;
  color: #777;
}

.timer-stat-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* Memory Monitor 섹션 */
.memory-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 0.75rem;
}

.memory-title {
  font-weight: 600;
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.memory-stats {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.memory-stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.memory-stat-label {
  font-weight: 500;
  color: #777;
}

.memory-stat-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* LED 애니메이션 */
@keyframes pulse-green {
  0%,
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.5);
  }
}

@keyframes pulse-red {
  0%,
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.5);
  }
}

/* Connection Status 헤더 스타일 */
.connection-status-card .text-subtitle1 {
  display: flex;
  align-items: center;
}

.connection-status-card .q-btn {
  transition: all 0.2s ease;
}

.connection-status-card .q-btn:hover {
  background-color: rgba(25, 118, 210, 0.1);
}

/* 다크 모드 지원 */
.body--dark .connection-item {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: #444;
}

.body--dark .connection-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.body--dark .connection-stats,
.body--dark .update-interval-section,
.body--dark .timer-accuracy-section,
.body--dark .memory-section {
  border-color: #444;
}

.body--dark .stat-label,
.body--dark .interval-stat-label,
.body--dark .timer-stat-label,
.body--dark .memory-stat-label {
  color: #bbb;
}

.body--dark .interval-title,
.body--dark .timer-title,
.body--dark .memory-title {
  color: #bbb;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .info-system-modal {
    min-width: 95vw;
    width: 95vw;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}

/* 접근성 개선 */
@media (prefers-reduced-motion: reduce) {
  .connection-led::before {
    animation: none;
  }

  .connection-item {
    transition: none;
  }

  .connection-item:hover {
    transform: none;
  }
}
</style>
