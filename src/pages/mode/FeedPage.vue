<template>
  <div class="mode-shell feed-mode">
    <div class="mode-shell__content">
      <div class="feed-container">
        <div class="row q-col-gutter-md">
          <!-- S-Band 섹션 -->
          <div class="col-12 col-md-5">
            <q-card class="control-section">
              <q-card-section>
                <div class="text-h6 text-primary q-mb-sm">S-Band</div>

                <!-- S-Band Rx Paths -->
                <div class="feed-path-section">
                  <!-- RHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">RHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentSBandLNARHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="getLNAStatusClass('s', 'rhcp')" @click="toggleLNA('s', 'rhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('s', 'rhcp')"
                                :stroke="getLNAStrokeColor('s', 'rhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM RHCP</div>
                    </div>
                  </div>

                  <!-- LHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">LHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentSBandLNALHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="getLNAStatusClass('s', 'lhcp')" @click="toggleLNA('s', 'lhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('s', 'lhcp')"
                                :stroke="getLNAStrokeColor('s', 'lhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM LHCP</div>
                    </div>
                  </div>
                </div>

                <!-- S-Band Tx Path -->
                <div class="feed-path-section rf-switch-section">
                  <div class="rf-switch-wrapper">
                    <!-- 왼쪽: 입력 라벨 (Rx 경로와 동일한 구조) -->
                    <div class="path-label-group rf-switch-labels">
                      <div class="path-label">RHCP(Tx)</div>
                      <div class="path-label">LHCP(Tx)</div>
                    </div>
                    <!-- 오른쪽: 스위치와 출력 -->
                    <div class="path-content rf-switch-content">
                      <!-- 왼쪽 입력 화살표들 -->
                      <div class="rf-switch-inputs-container">
                        <div class="arrow-container arrow-left rf-switch-arrow">
                          <div class="arrow-line"></div>
                        </div>
                        <div class="arrow-container arrow-left rf-switch-arrow">
                          <div class="arrow-line"></div>
                        </div>
                      </div>
                      <!-- 중앙: 하나의 스위치 -->
                      <div class="rf-switch-container">
                        <div class="rf-switch-icon" :class="getRFSwitchStatusClass()" @click="toggleRFSwitch()">
                          <svg viewBox="0 0 24 24" width="60" height="60">
                            <rect x="2" y="2" width="20" height="20" rx="2" :fill="getRFSwitchFillColor()"
                              :stroke="getRFSwitchStrokeColor()" stroke-width="2" />
                            <!-- RHCP: 위-왼쪽 원 → 중간-오른쪽 원 연결, 아래-왼쪽 원은 연결 안됨 -->
                            <template v-if="icdStore.feedSBoardStatusInfo.rfSwitchStatus.isRHCP">
                              <!-- 위 왼쪽 원(6,6)의 아래쪽(6, 7.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                              <line x1="6" y1="7.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                            </template>
                            <!-- LHCP: 아래-왼쪽 원 → 중간-오른쪽 원 연결, 위-왼쪽 원은 연결 안됨 -->
                            <template v-else>
                              <!-- 아래 왼쪽 원(6,18)의 위쪽(6, 16.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                              <line x1="6" y1="16.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                            </template>
                          </svg>
                        </div>
                      </div>
                      <!-- 오른쪽: 출력 -->
                      <div class="rf-switch-output-group">
                        <div class="arrow-container arrow-left rf-switch-arrow">
                          <div class="arrow-line"></div>
                        </div>
                        <div class="path-output-multiline">
                          <div>Tx (Selective)</div>
                          <div>RHCP or LHCP</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- X-Band 섹션 (FAN 포함) -->
          <div class="col-12 col-md-5">
            <q-card class="control-section">
              <q-card-section>
                <div class="text-h6 text-primary q-mb-sm">X-Band</div>

                <!-- X-Band Rx Paths -->
                <div class="feed-path-section">
                  <!-- RHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">RHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentXBandLNARHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="getLNAStatusClass('x', 'rhcp')" @click="toggleLNA('x', 'rhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('x', 'rhcp')"
                                :stroke="getLNAStrokeColor('x', 'rhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM RHCP</div>
                    </div>
                  </div>

                  <!-- LHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">LHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentXBandLNALHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="getLNAStatusClass('x', 'lhcp')" @click="toggleLNA('x', 'lhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('x', 'lhcp')"
                                :stroke="getLNAStrokeColor('x', 'lhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM LHCP</div>
                    </div>
                  </div>
                </div>

                <!-- FAN 섹션 (X-Band 안에 포함) -->
                <div class="fan-section">
                  <div class="fan-button-container">
                    <q-btn :class="getFanStatusClass()" class="fan-button" :color="getFanButtonColor()"
                      :outline="!icdStore.feedXBoardStatusInfo.fanStatus.isActive && !icdStore.feedXBoardStatusInfo.fanStatus.hasError"
                      :flat="false" @click="toggleFan()">
                      <svg class="fan-icon q-mr-sm" viewBox="0 0 24 24" width="20" height="20">
                        <!-- 팬 외곽 원 -->
                        <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="1.5" />
                        <!-- 팬 블레이드 1 - 3개 블레이드로 선풍기 느낌 -->
                        <path d="M12 12 L12 2 A10 10 0 0 1 19 7 L12 12 Z" fill="currentColor" opacity="0.6" />
                        <!-- 팬 블레이드 2 -->
                        <path d="M12 12 L20 19 A10 10 0 0 1 12 22 L12 12 Z" fill="currentColor" opacity="0.6" />
                        <!-- 팬 블레이드 3 -->
                        <path d="M12 12 L5 19 A10 10 0 0 1 4 7 L12 12 Z" fill="currentColor" opacity="0.6" />
                        <!-- 중앙 원 -->
                        <circle cx="12" cy="12" r="2.5" fill="currentColor" />
                      </svg>
                      <span class="fan-button-text">
                        FAN {{ icdStore.feedXBoardStatusInfo.fanStatus.power }}
                      </span>
                    </q-btn>
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- Legend 섹션 -->
          <div class="col-12 col-md-2">
            <q-card class="control-section">
              <q-card-section>
                <div class="text-h6 text-primary q-mb-xs">Legend</div>
                <div class="legend-grid">
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="#4caf50" stroke="#4caf50" stroke-width="1" />
                    </svg>
                    <span class="legend-text">LNA Power On</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="none" stroke="var(--theme-text-secondary)"
                        stroke-width="2" />
                    </svg>
                    <span class="legend-text">LNA Power Off</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="#f44336" stroke="#f44336" stroke-width="1" />
                    </svg>
                    <span class="legend-text">LNA Error</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#4caf50" stroke="#4caf50"
                        stroke-width="2" />
                      <!-- RHCP: 위 왼쪽 원(6,6)의 아래쪽(6, 7.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                      <line x1="6" y1="7.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    </svg>
                    <span class="legend-text">RHCP Select</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#2196f3" stroke="#2196f3"
                        stroke-width="2" />
                      <!-- LHCP: 아래 왼쪽 원(6,18)의 위쪽(6, 16.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                      <line x1="6" y1="16.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    </svg>
                    <span class="legend-text">LHCP Select</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#f44336" stroke="#f44336"
                        stroke-width="2" />
                      <!-- Error: RHCP와 동일한 형태 -->
                      <line x1="6" y1="7.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                      <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    </svg>
                    <span class="legend-text">RF Switch Error</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#4caf50" stroke="#4caf50"
                        stroke-width="1" />
                    </svg>
                    <span class="legend-text">FAN ON</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="none" stroke="white" stroke-width="2" />
                    </svg>
                    <span class="legend-text">FAN OFF</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#f44336" stroke="#f44336"
                        stroke-width="1" />
                    </svg>
                    <span class="legend-text">FAN Error</span>
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>

        <!-- 상태 메시지 표시 -->
        <div class="status-message q-mt-md" v-if="showStatusMessage">
          <q-banner :class="statusSuccess ? 'bg-positive text-white' : 'bg-negative text-white'">
            {{ statusMessage }}
          </q-banner>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// 알림 composable
const { success, error: notifyError } = useNotification()

// 로딩 상태
const isLoading = ref(false)

// 상태 메시지
const statusMessage = ref('')
const statusSuccess = ref(true)
const statusTimestamp = ref(0)

// 상태 메시지 표시 여부 (최근 3초 이내의 메시지만 표시)
const showStatusMessage = computed(() => {
  const currentTime = Date.now()
  return currentTime - statusTimestamp.value < 3000 && statusMessage.value !== ''
})

/**
 * 전류 값을 포맷팅합니다.
 */
const formatCurrent = (current: string): string => {
  if (!current || current === '') return '0.00'
  const num = parseFloat(current)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

/**
 * LNA 상태 클래스를 반환합니다.
 */
const getLNAStatusClass = (band: 's' | 'x', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo = band === 's' ? icdStore.feedSBoardStatusInfo : icdStore.feedXBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return 'lna-error'
  if (lnaStatus.isActive) return 'lna-on'
  return 'lna-off'
}

/**
 * LNA 채움 색상을 반환합니다.
 */
const getLNAFillColor = (band: 's' | 'x', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo = band === 's' ? icdStore.feedSBoardStatusInfo : icdStore.feedXBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'none' // 채우기 없음 (OFF)
}

/**
 * LNA 윤곽 색상을 반환합니다.
 */
const getLNAStrokeColor = (band: 's' | 'x', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo = band === 's' ? icdStore.feedSBoardStatusInfo : icdStore.feedXBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  // OFF일 때 화살표 색상과 동일하게 설정
  return getComputedStyle(document.documentElement).getPropertyValue('--theme-text-secondary').trim() || '#b0bec5'
}


/**
 * RF Switch 상태 클래스를 반환합니다.
 */
const getRFSwitchStatusClass = (): string => {
  const statusInfo = icdStore.feedSBoardStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return 'rf-switch-error'
  if (statusInfo.rfSwitchStatus.isRHCP) return 'rf-switch-rhcp'
  return 'rf-switch-lhcp'
}

/**
 * RF Switch 채움 색상을 반환합니다.
 */
const getRFSwitchFillColor = (): string => {
  const statusInfo = icdStore.feedSBoardStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (statusInfo.rfSwitchStatus.isRHCP) return '#4caf50' // 녹색 (RHCP)
  return '#2196f3' // 파란색 (LHCP)
}

/**
 * RF Switch 윤곽 색상을 반환합니다.
 */
const getRFSwitchStrokeColor = (): string => {
  const statusInfo = icdStore.feedSBoardStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (statusInfo.rfSwitchStatus.isRHCP) return '#4caf50' // 녹색 (RHCP)
  return '#2196f3' // 파란색 (LHCP)
}

/**
 * RF Switch 선 색상을 반환합니다.
 */
const getRFSwitchLineColor = (): string => {
  return 'white'
}

/**
 * FAN 상태 클래스를 반환합니다.
 */
const getFanStatusClass = (): string => {
  const statusInfo = icdStore.feedXBoardStatusInfo
  if (statusInfo.fanStatus.hasError) return 'fan-error'
  if (statusInfo.fanStatus.isActive) return 'fan-on'
  return 'fan-off'
}

/**
 * FAN 버튼 색상을 반환합니다.
 */
const getFanButtonColor = (): string => {
  const statusInfo = icdStore.feedXBoardStatusInfo
  if (statusInfo.fanStatus.hasError) return 'red'
  if (statusInfo.fanStatus.isActive) return 'green'
  return 'grey-7'
}

/**
 * LNA를 토글하고 즉시 명령을 전송합니다.
 */
const toggleLNA = async (band: 's' | 'x', type: 'lhcp' | 'rhcp') => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo

    // 클릭한 LNA의 현재 상태 확인 및 토글
    let sLHCP = sStatus.sLnaLHCPPower
    let sRHCP = sStatus.sLnaRHCPPower
    let xLHCP = xStatus.xLnaLHCPPower
    let xRHCP = xStatus.xLnaRHCPPower
    const sRFSwitch = sStatus.sRFSwitchMode
    const fan = xStatus.fanPower

    // 클릭한 항목만 토글
    if (band === 's') {
      if (type === 'lhcp') {
        sLHCP = !sLHCP
      } else {
        sRHCP = !sRHCP
      }
    } else {
      if (type === 'lhcp') {
        xLHCP = !xLHCP
      } else {
        xRHCP = !xRHCP
      }
    }

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
    )

    if (result.success) {
      success(`${band.toUpperCase()}-Band ${type.toUpperCase()} LNA 명령이 전송되었습니다.`)
      statusMessage.value = `${band.toUpperCase()}-Band ${type.toUpperCase()} LNA 명령이 성공적으로 전송되었습니다.`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('LNA 토글 중 오류:', error)
    notifyError('LNA 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'LNA 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * RF Switch를 토글하고 즉시 명령을 전송합니다.
 */
const toggleRFSwitch = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo

    // RF Switch만 토글
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = !sStatus.sRFSwitchMode // 토글
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = xStatus.fanPower

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
    )

    if (result.success) {
      success(`RF Switch 명령이 전송되었습니다. (${sRFSwitch ? 'RHCP' : 'LHCP'})`)
      statusMessage.value = `RF Switch 명령이 성공적으로 전송되었습니다. (${sRFSwitch ? 'RHCP' : 'LHCP'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('RF Switch 토글 중 오류:', error)
    notifyError('RF Switch 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'RF Switch 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * FAN을 토글하고 즉시 명령을 전송합니다.
 */
const toggleFan = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo

    // FAN만 토글
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = sStatus.sRFSwitchMode
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = !xStatus.fanPower // 토글

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
    )

    if (result.success) {
      success(`FAN 명령이 전송되었습니다. (${fan ? 'ON' : 'OFF'})`)
      statusMessage.value = `FAN 명령이 성공적으로 전송되었습니다. (${fan ? 'ON' : 'OFF'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('FAN 토글 중 오류:', error)
    notifyError('FAN 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'FAN 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

</script>

<style scoped>
.feed-mode {
  height: 100%;
}

.feed-container {
  padding: 0.5rem 1rem 0.5rem 1rem;
  max-width: 1400px;
  margin: 0 auto;
}

.control-section {
  height: 100%;
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
}

/* q-card-section의 패딩 조정 */
.control-section :deep(.q-card-section) {
  padding-top: 0.75rem;
  padding-bottom: 0.375rem;
}

/* Legend 섹션의 상단 패딩 조정 - X-Band RHCP 테두리 상단에 맞추기 */
.control-section:has(.legend-grid) :deep(.q-card-section) {
  padding-top: 0.75rem;
  padding-bottom: 0.375rem;
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}

.feed-path-section {
  margin-bottom: 0.25rem;
}

/* 마지막 feed-path-section의 하단 여백 제거 */
.feed-path-section:last-of-type {
  margin-bottom: 0;
}

/* S-Band 스위치 섹션과 X-Band FAN 섹션을 같은 선상에 배치 */
.rf-switch-section {
  margin-top: 0;
  margin-bottom: 0;
}

/* rf-switch-section이 feed-path-section 클래스도 가지고 있어서 margin-bottom이 적용되는 것을 방지 */
.rf-switch-section.feed-path-section {
  margin-bottom: 0;
}

.fan-section {
  margin-top: 0;
  margin-bottom: 0;
}

.feed-path {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
  /* LNA 라벨이 위로 25px(약 1.5rem) 나가 있으므로, LNA 위의 공간과 하단 공간을 동일하게 맞춤 */
  /* 하단 패딩을 더 줄여서 아래 공간을 최소화 */
  padding: 2.2rem 1rem 0.375rem 1rem;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  /* 전체 경로가 하나의 배경색으로 통일되도록 */
  position: relative;
  overflow: visible;
  /* 테두리 안의 전체 내용을 가운데 정렬 */
  width: 100%;
}

.path-label {
  min-width: 60px;
  width: 70px;
  max-width: 70px;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: right;
  /* 화살표와 평행하게 배치 - path-content와 같은 높이로 맞춤 */
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 60px;
  padding-top: 0;
  padding-right: 0;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 0;
  /* 화살표와 수평 정렬을 위해 transform 추가 */
  transform: translateY(2px);
}

.path-label-group {
  min-width: 80px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 500;
  color: var(--theme-text);
  text-align: right;
}

.path-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  /* flex: 1을 제거하여 내용에 맞는 너비만 사용 */
  position: relative;
  min-height: 60px;
  height: 60px;
  /* feed-path의 배경색과 통일되도록 배경색 제거 */
  background-color: transparent;
  /* 첫 번째 요소(화살표) 앞의 간격을 feed-path의 gap과 동일하게 */
  margin-left: 0;
  margin-right: 0;
  /* 반응형: 작은 화면에서도 요소들이 함께 줄어들도록 */
  min-width: 0;
  /* 내부 요소들이 가운데 정렬되도록 */
  flex-shrink: 0;
}

/* 화살표와 출력 라벨 사이 간격 추가 - Tx (Selective)의 gap: 0.5rem과 동일하게 */
.path-content>.arrow-container:last-of-type {
  margin-right: 0;
  /* 간격은 출력 라벨의 margin-left로 처리 */
  flex-shrink: 0;
}

/* 마지막 화살표의 화살표 끝 부분 위치 조정 - 출력 라벨과의 간격을 명확히 */
.path-content>.arrow-container:last-of-type .arrow-line::after {
  right: 0;
  /* 화살표 끝이 출력 라벨과 너무 가까워 보이지 않도록 */
}

.arrow-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  min-width: 80px;
  width: 80px;
  max-width: 80px;
  height: 60px;
  /* 삼각형과 화살표를 붙이기 위한 마진 조정 */
  margin: 0;
  /* 화살표를 삼각형과 완벽하게 정렬 - 조금 더 아래로 내림 */
  transform: translateY(2px);
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 1;
}

.arrow-line {
  width: 100%;
  height: 2px;
  background-color: var(--theme-text-secondary);
  position: relative;
  /* 화살표를 삼각형과 붙이기 위한 위치 조정 */
  margin: 0;
}

.arrow-line::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  /* 화살표 화살 부분 - 선의 끝과 약간 겹쳐서 자연스럽게 연결 */
  border-left: 12px solid var(--theme-text-secondary);
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  /* 화살표 끝이 선의 끝과 자연스럽게 연결되도록 - 삼각형과 겹치지 않도록 */
}

.current-display-above {
  position: absolute;
  /* feed-path의 상단 패딩 내부에 위치하도록 조정 - 전류 표시를 화살표에서 더 멀리 */
  top: -0.5rem;
  left: 50%;
  transform: translateX(-50%);
  min-width: 60px;
  padding: 0.25rem 0.5rem;
  /* 배경색 제거 - feed-path의 배경색과 통일 */
  background-color: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  text-align: center;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--theme-text-secondary);
  white-space: nowrap;
  z-index: 2;
}

.lna-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60px;
  /* 삼각형과 화살표를 붙이기 위한 마진 조정 */
  margin: 0;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 0;
  min-width: 0;
}

.lna-label {
  position: absolute;
  /* LNA 라벨을 삼각형 위에 위치하도록 조정 - 조금 내려서 삼각형에 더 가깝게 */
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--theme-text-secondary);
  text-align: center;
  white-space: nowrap;
  z-index: 1;
  pointer-events: none;
}

.lna-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  /* 화살표와 정확히 정렬하기 위한 미세 조정 */
  position: relative;
  /* 삼각형을 화살표와 완벽하게 정렬 - 화살표와 평행하게 맞춤 */
  transform: translateY(1px);
}

.rf-switch-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  /* 내부 콘텐츠 높이(60px) + 위아래 패딩(1rem * 2 = 32px) = 92px */
  min-height: 60px;
  /* LNA 부분(.feed-path)과 동일한 테두리 스타일, 위아래 패딩 증가 */
}

.rf-switch-labels {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.125rem;
  min-width: 70px;
  width: 70px;
  /* Rx 경로의 path-label과 동일한 너비 */
  height: 60px;
  /* 라벨 컨테이너 높이를 화살표와 동일하게 */
  /* RHCP(Tx), LHCP(Tx) 라벨 사이 간격을 더 좁게 조정 */
}

/* Tx 부분의 path-label 스타일 조정 - 화살표와 수평 정렬 */
.rf-switch-labels .path-label {
  height: 30px;
  min-height: 30px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0;
  /* 스위치 아이콘과 평행하게 정렬 */
  transform: translateY(2px);
}

.rf-switch-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 0;
  flex: 1;
  /* 스위치와 화살표를 붙이기 위해 gap 제거 */
  /* path-content의 justify-content: center가 적용되지 않도록 명시적으로 설정 */
}

.rf-switch-inputs-container {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  justify-content: center;
  align-items: center;
  min-width: 80px;
  width: 80px;
  /* Rx 경로의 화살표와 동일한 너비로 정렬, 높이는 두 개의 화살표가 세로로 배치되므로 60px */
  height: 60px;
  /* RHCP(Tx), LHCP(Tx) 화살표 사이 간격을 라벨과 동일하게 더 좁게 조정 */
}

/* Tx 부분의 입력 화살표 컨테이너 높이 조정 - 두 개가 세로로 배치되므로 각각 30px */
.rf-switch-inputs-container .arrow-container {
  height: 30px;
  min-height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 화살표 길이(너비)는 모든 화살표와 동일하게 80px 유지 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}

.rf-switch-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  height: 60px;
  /* 라벨과 화살표 묶음의 중앙과 스위치 아이콘을 평행하게 정렬 */
  /* RHCP(Tx), LHCP(Tx) 라벨 2개 + gap의 중앙에 맞추기 위해 더 내림 */
  transform: translateY(4px);
}

.rf-switch-output-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: 0;
  /* 화살표와 출력 라벨 사이 간격 - RHCP(Rx) 라벨과 화살표 사이 간격과 동일하게 */
  height: 60px;
}

.arrow-left {
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}

.arrow-left .arrow-line::after {
  left: -1px;
  right: auto;
  border-left: none;
  border-right: 12px solid var(--theme-text-secondary);
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  /* 스위치 아이콘과 자연스럽게 연결되도록 */
}

.arrow-right {
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}

.arrow-container.rf-switch-arrow {
  width: 80px;
  min-width: 80px;
  max-width: 80px;
  /* 출력 화살표는 60px 높이 유지 */
  height: 60px;
  /* Rx 경로의 arrow-container와 동일한 너비(길이) */
}

.path-output-multiline {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: left;
  line-height: 1.2;
  min-width: 140px;
  /* 화살표와 수평 정렬을 위해 수직 중앙 정렬 */
  justify-content: center;
  align-items: flex-start;
  /* 화살표와 수평 정렬을 위해 transform 추가 */
  transform: translateY(2px);
}

.lna-icon,
.rf-switch-icon {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  user-select: none;
}

/* Quasar q-btn 컴포넌트에 직접 스타일 적용 */
.fan-button,
.fan-button.q-btn {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  user-select: none;
  /* 텍스트가 안 겹치게 길게 설정 */
  min-width: 200px !important;
  padding: 0.5rem 1rem !important;
  font-weight: 500;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  /* 스위치 컨테이너와 동일한 높이로 설정하여 섹션 높이 통일 */
  height: 60px !important;
  min-height: 60px !important;
  /* .fan-section이 align-items: center로 설정되어 있으므로 버튼이 자동으로 가운데 정렬됨 */
  /* transform 제거하여 자연스러운 가운데 정렬 유지 */
  transform: none !important;
}

.fan-button-text {
  white-space: nowrap;
  font-size: 0.9rem;
}

.fan-icon {
  flex-shrink: 0;
  vertical-align: middle;
}

/* LNA 아이콘 정렬 조정 - 삼각형을 화살표와 정확히 정렬 */
.lna-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
  /* SVG 정렬을 위해 vertical-align 조정 */
  vertical-align: middle;
}

/* SVG 내부 정렬 조정 - 삼각형이 화살표와 정확히 같은 높이에 오도록 */
.lna-icon svg {
  display: block;
  vertical-align: middle;
  /* 삼각형을 화살표와 완벽하게 정렬 - 화살표와 평행하게 맞춤 */
  transform: translateY(1px);
}

.lna-icon:hover,
.rf-switch-icon:hover,
.fan-button:hover {
  opacity: 0.8;
}

.lna-icon:hover {
  transform: scale(1.1);
}

.lna-icon:active,
.rf-switch-icon:active {
  transform: scale(0.95);
}

.fan-button:active,
.fan-button.q-btn:active {
  transform: scale(0.95) !important;
}

.path-output {
  flex: 1;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: left;
  /* 카드 중앙 정렬을 위해 높이와 정렬 명시 */
  display: flex;
  align-items: center;
  height: 60px;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  min-width: 0;
  flex-shrink: 1;
  /* 화살표와 수평 정렬을 위해 transform 추가 */
  transform: translateY(2px);
  /* 화살표와의 간격을 명확히 하기 위해 왼쪽 마진 제거 */
  margin-left: 0 !important;
  padding-left: 0 !important;
}

/* path-content 내부의 path-output에만 간격 적용 */
.path-content>.path-output {
  margin-left: 0.5rem !important;
}

.fan-section {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  /* FAN 라벨 제거 후 여백 조정 */
  margin-top: 0;
  margin-bottom: 0;
  /* 스위치 테두리(.rf-switch-wrapper)와 동일한 높이 및 정렬 */
  min-height: 60px;
  /* 스위치 테두리 기준으로 가운데 수평 정렬 */
  /* 스위치 컨테이너와 동일한 정렬을 위해 align-items: center 사용 */
  /* 스위치 테두리(.rf-switch-wrapper)와 동일한 테두리 스타일 */
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.fan-button-container {
  display: flex;
  justify-content: center;
  align-items: center;
  /* 스위치 테두리 기준으로 가운데 수평 정렬 */
  width: 100%;
  /* 버튼이 컨테이너 중앙에 위치하도록 */
}


.legend-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.5rem 0.5rem 0.25rem 0.75rem;
  /* 범례 항목들을 왼쪽 정렬 */
  align-items: flex-start;
  /* 최소 너비 제한 제거 */
  min-width: 0;
  width: 100%;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  /* 텍스트 줄바꿈 방지 */
  white-space: nowrap;
  /* 최소 너비 제한 제거 */
  min-width: 0;
  width: 100%;
}

.legend-icon {
  flex-shrink: 0;
}

.legend-text {
  color: var(--theme-text);
  font-size: 0.9rem;
  /* 텍스트 줄바꿈 방지 */
  white-space: nowrap;
  flex-shrink: 0;
  /* 텍스트 오버플로우 처리 */
  overflow: visible;
  text-overflow: clip;
}

/* 상태 메시지 스타일 */
.status-message {
  transition: opacity 0.3s;
}

/* 반응형: 작은 화면에서 요소들이 함께 비례적으로 줄어들도록 */
@media (max-width: 1200px) {
  .arrow-container {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
  }

  .path-label {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
    font-size: 0.9rem;
  }

  .lna-container {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
  }

  .lna-icon svg {
    width: 50px;
    height: 50px;
  }

  .path-output {
    min-width: 80px;
    font-size: 0.9rem;
  }
}

@media (max-width: 960px) {
  .arrow-container {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
  }

  .path-label {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
    font-size: 0.85rem;
  }

  .lna-container {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
  }

  .lna-icon svg {
    width: 40px;
    height: 40px;
  }

  .path-output {
    min-width: 70px;
    font-size: 0.85rem;
  }

  .arrow-line::after {
    right: -1px;
    border-left-width: 10px;
    border-top-width: 5px;
    border-bottom-width: 5px;
  }
}

@media (max-width: 768px) {
  .feed-path {
    padding: 2rem 0.75rem 0.375rem 0.75rem;
  }

  .arrow-container {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
  }

  .path-label {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
    font-size: 0.8rem;
  }

  .lna-container {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
  }

  .lna-icon svg {
    width: 35px;
    height: 35px;
  }

  .path-output {
    min-width: 60px;
    font-size: 0.8rem;
  }

  .arrow-line::after {
    right: -1px;
    border-left-width: 8px;
    border-top-width: 4px;
    border-bottom-width: 4px;
  }

  .current-display-above {
    font-size: 0.75rem;
    padding: 0.2rem 0.4rem;
    min-width: 50px;
  }

  .lna-label {
    font-size: 0.7rem;
  }
}
</style>
