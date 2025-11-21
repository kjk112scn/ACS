<template>
  <div class="mode-shell feed-mode">
    <div class="mode-shell__content">
      <div class="feed-container">
        <div class="row q-col-gutter-md">
          <!-- S-Band 섹션 -->
          <div class="col-12 col-md-5">
            <q-card class="control-section">
              <q-card-section>
                <div class="text-h6 text-primary q-mb-md">S-Band</div>

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
                <div class="feed-path-section q-mt-md">
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
                          <svg viewBox="0 0 24 24" width="78" height="78">
                            <rect x="2" y="2" width="20" height="20" rx="2" :fill="getRFSwitchFillColor()"
                              :stroke="getRFSwitchStrokeColor()" stroke-width="2" />
                            <!-- RHCP: 위-왼쪽 원 → 중간-오른쪽 원 연결, 아래-왼쪽 원은 연결 안됨 -->
                            <template v-if="icdStore.feedSBoardStatusInfo.rfSwitchStatus.isRHCP">
                              <line x1="6" y1="6" x2="18" y2="12" :stroke="getRFSwitchLineColor()" stroke-width="1.5" />
                              <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                              <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                stroke-width="1" />
                            </template>
                            <!-- LHCP: 아래-왼쪽 원 → 중간-오른쪽 원 연결, 위-왼쪽 원은 연결 안됨 -->
                            <template v-else>
                              <line x1="6" y1="18" x2="18" y2="12" :stroke="getRFSwitchLineColor()"
                                stroke-width="1.5" />
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
                <div class="text-h6 text-primary q-mb-md">X-Band</div>

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
                <div class="fan-section q-mt-md">
                  <div class="text-subtitle2 text-primary q-mb-sm">FAN</div>
                  <div class="fan-button-container">
                    <div class="fan-button" :class="getFanStatusClass()" @click="toggleFan()">
                      <svg viewBox="0 0 24 24" width="60" height="60">
                        <rect x="2" y="2" width="20" height="20" rx="2" :fill="getFanFillColor()"
                          :stroke="getFanStrokeColor()" stroke-width="2" />
                        <text x="12" y="16" text-anchor="middle" font-size="10" :fill="getFanTextColor()"
                          font-weight="bold">FAN</text>
                        <text x="12" y="20" text-anchor="middle" font-size="8" :fill="getFanTextColor()"
                          font-weight="bold">
                          {{ icdStore.feedXBoardStatusInfo.fanStatus.power }}
                        </text>
                      </svg>
                    </div>
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- Legend 섹션 -->
          <div class="col-12 col-md-2">
            <q-card class="control-section">
              <q-card-section>
                <div class="text-h6 text-primary q-mb-md">Legend</div>
                <div class="legend-grid">
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="#4caf50" stroke="#4caf50" stroke-width="1" />
                    </svg>
                    <span class="legend-text">LNA Power On</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="none" stroke="white" stroke-width="2" />
                    </svg>
                    <span class="legend-text">LNA Power Off</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <polygon points="22,12 2,2 2,22" fill="#ff9800" stroke="#ff9800" stroke-width="1" />
                    </svg>
                    <span class="legend-text">LNA Error</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#4caf50" stroke="#4caf50"
                        stroke-width="1" />
                      <!-- RHCP: 위-왼쪽 → 아래-오른쪽, 아래-왼쪽에 원 하나 -->
                      <line x1="6" y1="6" x2="18" y2="18" stroke="white" stroke-width="2" />
                      <circle cx="6" cy="6" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="18" cy="18" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="6" cy="18" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                    </svg>
                    <span class="legend-text">RHCP Select</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#2196f3" stroke="#2196f3"
                        stroke-width="1" />
                      <!-- LHCP: 아래-왼쪽 → 위-오른쪽, 위-왼쪽에 원 하나 -->
                      <line x1="6" y1="18" x2="18" y2="6" stroke="white" stroke-width="2" />
                      <circle cx="6" cy="18" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="18" cy="6" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="6" cy="6" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                    </svg>
                    <span class="legend-text">LHCP Select</span>
                  </div>
                  <div class="legend-item">
                    <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#ff9800" stroke="#ff9800"
                        stroke-width="1" />
                      <!-- Error: RHCP와 동일한 형태 -->
                      <line x1="6" y1="6" x2="18" y2="18" stroke="white" stroke-width="2" />
                      <circle cx="6" cy="6" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="18" cy="18" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
                      <circle cx="6" cy="18" r="2.5" fill="none" stroke="white" stroke-width="1.5" />
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
                      <rect x="2" y="2" width="20" height="20" rx="2" fill="#ff9800" stroke="#ff9800"
                        stroke-width="1" />
                    </svg>
                    <span class="legend-text">FAN Error</span>
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>

        <!-- 제어 버튼 (테스트용 유지) -->
        <div class="row justify-center q-mt-md mode-button-bar">
          <q-btn label="Apply" color="primary" icon="send" :loading="isLoading" @click="applyFeedControls" />
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

  if (lnaStatus.hasError) return '#ff9800' // 주황색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'none' // 채우기 없음 (OFF)
}

/**
 * LNA 윤곽 색상을 반환합니다.
 */
const getLNAStrokeColor = (band: 's' | 'x', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo = band === 's' ? icdStore.feedSBoardStatusInfo : icdStore.feedXBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return '#ff9800' // 주황색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'white' // 흰색 (OFF)
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
  if (statusInfo.rfSwitchStatus.hasError) return '#ff9800' // 주황색 (Error)
  if (statusInfo.rfSwitchStatus.isRHCP) return '#4caf50' // 녹색 (RHCP)
  return '#2196f3' // 파란색 (LHCP)
}

/**
 * RF Switch 윤곽 색상을 반환합니다.
 */
const getRFSwitchStrokeColor = (): string => {
  const statusInfo = icdStore.feedSBoardStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return '#ff9800' // 주황색 (Error)
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
 * FAN 채움 색상을 반환합니다.
 */
const getFanFillColor = (): string => {
  const statusInfo = icdStore.feedXBoardStatusInfo
  if (statusInfo.fanStatus.hasError) return '#ff9800' // 주황색 (Error)
  if (statusInfo.fanStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'none' // 채우기 없음 (OFF)
}

/**
 * FAN 윤곽 색상을 반환합니다.
 */
const getFanStrokeColor = (): string => {
  const statusInfo = icdStore.feedXBoardStatusInfo
  if (statusInfo.fanStatus.hasError) return '#ff9800' // 주황색 (Error)
  if (statusInfo.fanStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'white' // 흰색 (OFF)
}

/**
 * FAN 텍스트 색상을 반환합니다.
 */
const getFanTextColor = (): string => {
  const statusInfo = icdStore.feedXBoardStatusInfo
  if (statusInfo.fanStatus.hasError) return 'white'
  if (statusInfo.fanStatus.isActive) return 'white'
  return 'white'
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

/**
 * Feed 제어 적용 함수 (Apply 버튼용 - 테스트용)
 */
const applyFeedControls = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo

    // Feed On/Off 명령 API 호출
    await icdStore.sendFeedOnOffCommand(
      sStatus.sLnaLHCPPower,
      sStatus.sLnaRHCPPower,
      sStatus.sRFSwitchMode,
      xStatus.xLnaLHCPPower,
      xStatus.xLnaRHCPPower,
      xStatus.fanPower,
    )

    // 성공 메시지 설정
    success('Feed 제어 명령이 성공적으로 전송되었습니다.')
    statusMessage.value = 'Feed 제어 명령이 성공적으로 전송되었습니다.'
    statusSuccess.value = true
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('Feed 제어 명령 처리 중 오류:', error)

    // 오류 메시지 설정
    notifyError('Feed 제어 명령 전송 중 오류가 발생했습니다.')
    statusMessage.value = 'Feed 제어 명령 전송 중 오류가 발생했습니다.'
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
  padding: 1rem;
  max-width: 1400px;
  margin: 0 auto;
}

.control-section {
  height: 100%;
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border);
}

.feed-path-section {
  margin-bottom: 1rem;
}

.feed-path {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 0.5rem;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.02);
}

.path-label {
  min-width: 80px;
  font-weight: 500;
  color: var(--theme-text);
  text-align: right;
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
  gap: 0.5rem;
  flex: 1;
  position: relative;
  min-height: 60px;
}

.arrow-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
  width: 80px;
  height: 60px;
}

.arrow-line {
  width: 100%;
  height: 2px;
  background-color: var(--theme-text-secondary);
  position: relative;
}

.arrow-line::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid var(--theme-text-secondary);
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
}

.current-display-above {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  min-width: 60px;
  padding: 0.25rem 0.5rem;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--theme-border);
  border-radius: 4px;
  text-align: center;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--theme-text);
  white-space: nowrap;
}

.lna-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60px;
}

.lna-label {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--theme-text);
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
}

.rf-switch-wrapper {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.02);
  min-height: 60px;
}

.rf-switch-labels {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.75rem;
}

.rf-switch-content {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.rf-switch-inputs-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  justify-content: center;
  align-items: center;
  min-width: 120px;
  width: 120px;
}

.rf-switch-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  height: 60px;
}

.rf-switch-output-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-left: 0.5rem;
}

.arrow-left {
  width: 80px;
}

.arrow-left .arrow-line::after {
  left: 0;
  right: auto;
  border-left: none;
  border-right: 8px solid var(--theme-text-secondary);
}

.arrow-right {
  width: 80px;
}

.arrow-container.rf-switch-arrow {
  width: 140px;
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
}

.lna-icon,
.rf-switch-icon,
.fan-button {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  user-select: none;
}

.lna-icon:hover,
.rf-switch-icon:hover,
.fan-button:hover {
  transform: scale(1.1);
  opacity: 0.8;
}

.lna-icon:active,
.rf-switch-icon:active,
.fan-button:active {
  transform: scale(0.95);
}

.path-output {
  flex: 1;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: left;
}

.fan-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 0;
}

.fan-button-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.legend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  padding: 1rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.legend-icon {
  flex-shrink: 0;
}

.legend-text {
  color: var(--theme-text);
  font-size: 0.9rem;
}

/* 상태 메시지 스타일 */
.status-message {
  transition: opacity 0.3s;
}
</style>
