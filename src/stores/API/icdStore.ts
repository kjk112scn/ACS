import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose } from 'vue'
import { icdService, type MessageData, type MultiControlCommand } from '../../services/icdService'

// 값을 안전하게 문자열로 변환하는 헬퍼 함수
const safeToString = (value: unknown): string => {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'string') return value
  if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') {
    return String(value)
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return '[복잡한 객체]'
    }
  }
  if (typeof value === 'function') return '[함수]'
  if (typeof value === 'symbol') return value.toString()
  return `[알 수 없는 타입: ${typeof value}]`
}

// WebSocket 서버 URL
const WEBSOCKET_URL = 'ws://localhost:8080/ws'

const UPDATE_INTERVAL = 30 // 30ms 주기

export const useICDStore = defineStore('icd', () => {
  // 기본 상태 정의
  const serverTime = ref('')
  const resultTimeOffsetCalTime = ref('')
  const cmdAzimuthAngle = ref('')
  const cmdElevationAngle = ref('')
  const cmdTiltAngle = ref('')
  const cmdTime = ref('')
  const error = ref('')
  const isConnected = ref(false)
  const messageDelay = ref(0)

  // 안테나 데이터 전체 필드 추가
  const modeStatusBits = ref('')
  const azimuthAngle = ref('')
  const elevationAngle = ref('')
  const tiltAngle = ref('')
  const azimuthSpeed = ref('')
  const elevationSpeed = ref('')
  const tiltSpeed = ref('')
  const servoDriverAzimuthAngle = ref('')
  const servoDriverElevationAngle = ref('')
  const servoDriverTiltAngle = ref('')
  const torqueAzimuth = ref('')
  const torqueElevation = ref('')
  const torqueTilt = ref('')
  const windSpeed = ref('')
  const windDirection = ref('')
  const rtdOne = ref('')
  const rtdTwo = ref('')
  const mainBoardProtocolStatusBits = ref('')
  const mainBoardStatusBits = ref('')
  const mainBoardMCOnOffBits = ref('')
  const mainBoardReserveBits = ref('')
  const azimuthBoardServoStatusBits = ref('')
  const azimuthBoardStatusBits = ref('')
  const elevationBoardServoStatusBits = ref('')
  const elevationBoardStatusBits = ref('')
  const tiltBoardServoStatusBits = ref('')
  const tiltBoardStatusBits = ref('')
  const feedSBoardStatusBits = ref('')
  const feedXBoardStatusBits = ref('')
  const currentSBandLNALHCP = ref('')
  const currentSBandLNARHCP = ref('')
  const currentXBandLNALHCP = ref('')
  const currentXBandLNARHCP = ref('')
  const rssiSBandLNALHCP = ref('')
  const rssiSBandLNARHCP = ref('')
  const rssiXBandLNALHCP = ref('')
  const rssiXBandLNARHCP = ref('')
  const azimuthAcceleration = ref('')
  const elevationAcceleration = ref('')
  const tiltAcceleration = ref('')
  const azimuthMaxAcceleration = ref('')
  const elevationMaxAcceleration = ref('')
  const tiltMaxAcceleration = ref('')
  const trackingAzimuthTime = ref('')
  const trackingCMDAzimuthAngle = ref('')
  const trackingActualAzimuthAngle = ref('')
  const trackingElevationTime = ref('')
  const trackingCMDElevationAngle = ref('')
  const trackingActualElevationAngle = ref('')
  const trackingTiltTime = ref('')
  const trackingCMDTiltAngle = ref('')
  const trackingActualTiltAngle = ref('')
  // 96-98번째 줄 근처 - Boolean 타입으로 변경
  const ephemerisStatus = ref<boolean | null>(null)
  const passScheduleStatus = ref<boolean | null>(null)
  const sunTrackStatus = ref<boolean | null>(null)

  // 타이머 관련 상태

  const updateTimer = ref<NodeJS.Timeout | null>(null)
  const isUpdating = ref(false)
  const updateCount = ref(0)
  const lastUpdateTime = ref(0)

  // 최신 데이터 버퍼 (WebSocket에서 받은 데이터 임시 저장)
  const latestDataBuffer = ref<MessageData | null>(null)
  const bufferUpdateTime = ref(0)

  const hasActiveConnection = computed(() => isConnected.value && isUpdating.value)
  const lastUpdateTimeFormatted = computed(() =>
    new Date(lastUpdateTime.value).toLocaleTimeString(),
  )
  const connectionStatus = computed(() => ({
    isConnected: isConnected.value,

    isUpdating: isUpdating.value,
    lastUpdate: lastUpdateTimeFormatted.value,
    updateCount: updateCount.value,
    messageDelay: messageDelay.value,
    bufferAge: bufferUpdateTime.value ? Date.now() - bufferUpdateTime.value : 0,
  }))

  // WebSocket 메시지 핸들러 - 데이터를 버퍼에만 저장
  const handleWebSocketMessage = (message: MessageData) => {
    try {
      // 받은 데이터를 버퍼에 저장만 하고 즉시 UI 업데이트하지 않음
      latestDataBuffer.value = message.data as MessageData
      bufferUpdateTime.value = Date.now()

      // 디버깅용 (가끔씩만 로그)
      if (Math.random() < 0.01) {
        // 1% 확률
        console.log('📨 WebSocket 데이터 버퍼 업데이트:', new Date().toLocaleTimeString())
      }
    } catch (e) {
      console.error('❌ WebSocket 메시지 처리 오류:', e)
    }
  }

  // 30ms 타이머로 실행되는 UI 업데이트 함수
  const updateUIFromBuffer = () => {
    try {
      const startTime = performance.now()

      // 버퍼에 새 데이터가 있는지 확인
      if (!latestDataBuffer.value) {
        return
      }

      const message = latestDataBuffer.value
      updateCount.value++
      lastUpdateTime.value = Date.now()

      // serverTime 업데이트 (최우선)
      if (message.serverTime !== undefined) {
        const oldTime = serverTime.value

        serverTime.value = safeToString(message.serverTime)

        // 100번마다 로그
        if (updateCount.value % 100 === 0) {
          console.log(`🕐 [${updateCount.value}] serverTime: ${oldTime} → ${serverTime.value}`)
        }
      }

      // resultTimeOffsetCalTime 업데이트

      if (message.resultTimeOffsetCalTime !== undefined) {
        resultTimeOffsetCalTime.value = safeToString(message.resultTimeOffsetCalTime)
      }

      // 명령 데이터 업데이트

      if (message.cmdAzimuthAngle !== undefined) {
        cmdAzimuthAngle.value = safeToString(message.cmdAzimuthAngle)
      }

      if (message.cmdElevationAngle !== undefined) {
        cmdElevationAngle.value = safeToString(message.cmdElevationAngle)
      }

      if (message.cmdTiltAngle !== undefined) {
        cmdTiltAngle.value = safeToString(message.cmdTiltAngle)
      }

      // 안테나나 데이터 업데이트
      if (message.data && typeof message.data === 'object') {
        updataAntennaData(message.data)
      }
      // 안테나나 데이터 업데이트
      if (message.trackingStatus && typeof message.trackingStatus === 'object') {
        updataTrackingStatus(message.trackingStatus)
      }

      // 성능 측정
      const endTime = performance.now()
      messageDelay.value = endTime - startTime

      // 성능 통계 (1초마다)
      if (updateCount.value % Math.floor(1000 / UPDATE_INTERVAL) === 0) {
        console.log(
          `📊 UI 업데이트 통계: ${updateCount.value}회, 처리시간: ${messageDelay.value.toFixed(2)}ms`,
        )
      }
    } catch (e) {
      console.error('❌ UI 업데이트 오류:', e)
    }
  }
  // 추적 상태 업데이트 함수 수정
  const updataTrackingStatus = (trackingStatusData: Record<string, unknown>) => {
    try {
      // Ephemeris 상태 업데이트 (Boolean)
      if (trackingStatusData.ephemerisStatus !== undefined) {
        const newStatus = trackingStatusData.ephemerisStatus as boolean | null
        if (ephemerisStatus.value !== newStatus) {
          console.log(`📡 Ephemeris 상태 변경: ${ephemerisStatus.value} → ${newStatus}`)
          ephemerisStatus.value = newStatus
        }
      }

      // Pass Schedule 상태 업데이트 (Boolean)
      if (trackingStatusData.passScheduleStatus !== undefined) {
        const newStatus = trackingStatusData.passScheduleStatus as boolean | null
        if (passScheduleStatus.value !== newStatus) {
          console.log(`📅 Pass Schedule 상태 변경: ${passScheduleStatus.value} → ${newStatus}`)
          passScheduleStatus.value = newStatus
        }
      }

      // Sun Track 상태 업데이트 (Boolean)
      if (trackingStatusData.sunTrackStatus !== undefined) {
        const newStatus = trackingStatusData.sunTrackStatus as boolean | null
        if (sunTrackStatus.value !== newStatus) {
          console.log(`☀️ Sun Track 상태 변경: ${sunTrackStatus.value} → ${newStatus}`)
          sunTrackStatus.value = newStatus
        }
      }
    } catch (e) {
      console.error('❌ 추적 상태 업데이트 오류:', e)
    }
  }
  // 모든 안테나 데이터를 업데이트하는 함수
  const updataAntennaData = (antennaData: Record<string, unknown>) => {
    try {
      // 기본 안테나 데이터
      if (antennaData.modeStatusBits !== undefined && antennaData.modeStatusBits !== null) {
        modeStatusBits.value = safeToString(antennaData.modeStatusBits)
      }
      if (antennaData.azimuthAngle !== undefined && antennaData.azimuthAngle !== null) {
        azimuthAngle.value = safeToString(antennaData.azimuthAngle)
      }
      if (antennaData.elevationAngle !== undefined && antennaData.elevationAngle !== null) {
        elevationAngle.value = safeToString(antennaData.elevationAngle)
      }
      if (antennaData.tiltAngle !== undefined && antennaData.tiltAngle !== null) {
        tiltAngle.value = safeToString(antennaData.tiltAngle)
      }
      if (antennaData.azimuthSpeed !== undefined && antennaData.azimuthSpeed !== null) {
        azimuthSpeed.value = safeToString(antennaData.azimuthSpeed)
      }
      if (antennaData.elevationSpeed !== undefined && antennaData.elevationSpeed !== null) {
        elevationSpeed.value = safeToString(antennaData.elevationSpeed)
      }
      if (antennaData.tiltSpeed !== undefined && antennaData.tiltSpeed !== null) {
        tiltSpeed.value = safeToString(antennaData.tiltSpeed)
      }

      // 서보 드라이버 데이터
      if (
        antennaData.servoDriverAzimuthAngle !== undefined &&
        antennaData.servoDriverAzimuthAngle !== null
      ) {
        servoDriverAzimuthAngle.value = safeToString(antennaData.servoDriverAzimuthAngle)
      }
      if (
        antennaData.servoDriverElevationAngle !== undefined &&
        antennaData.servoDriverElevationAngle !== null
      ) {
        servoDriverElevationAngle.value = safeToString(antennaData.servoDriverElevationAngle)
      }
      if (
        antennaData.servoDriverTiltAngle !== undefined &&
        antennaData.servoDriverTiltAngle !== null
      ) {
        servoDriverTiltAngle.value = safeToString(antennaData.servoDriverTiltAngle)
      }

      // 토크 데이터
      if (antennaData.torqueAzimuth !== undefined && antennaData.torqueAzimuth !== null) {
        torqueAzimuth.value = safeToString(antennaData.torqueAzimuth)
      }
      if (antennaData.torqueElevation !== undefined && antennaData.torqueElevation !== null) {
        torqueElevation.value = safeToString(antennaData.torqueElevation)
      }
      if (antennaData.torqueTilt !== undefined && antennaData.torqueTilt !== null) {
        torqueTilt.value = safeToString(antennaData.torqueTilt)
      }

      // 환경 데이터
      if (antennaData.windSpeed !== undefined && antennaData.windSpeed !== null) {
        windSpeed.value = safeToString(antennaData.windSpeed)
      }
      if (antennaData.windDirection !== undefined && antennaData.windDirection !== null) {
        windDirection.value = safeToString(antennaData.windDirection)
      }
      if (antennaData.rtdOne !== undefined && antennaData.rtdOne !== null) {
        rtdOne.value = safeToString(antennaData.rtdOne)
      }
      if (antennaData.rtdTwo !== undefined && antennaData.rtdTwo !== null) {
        rtdTwo.value = safeToString(antennaData.rtdTwo)
      }

      // 메인 보드 상태
      if (
        antennaData.mainBoardProtocolStatusBits !== undefined &&
        antennaData.mainBoardProtocolStatusBits !== null
      ) {
        mainBoardProtocolStatusBits.value = safeToString(antennaData.mainBoardProtocolStatusBits)
      }
      if (
        antennaData.mainBoardStatusBits !== undefined &&
        antennaData.mainBoardStatusBits !== null
      ) {
        mainBoardStatusBits.value = safeToString(antennaData.mainBoardStatusBits)
      }
      if (
        antennaData.mainBoardMCOnOffBits !== undefined &&
        antennaData.mainBoardMCOnOffBits !== null
      ) {
        mainBoardMCOnOffBits.value = safeToString(antennaData.mainBoardMCOnOffBits)
      }
      if (
        antennaData.mainBoardReserveBits !== undefined &&
        antennaData.mainBoardReserveBits !== null
      ) {
        mainBoardReserveBits.value = safeToString(antennaData.mainBoardReserveBits)
      }
      // 축별 보드 상태
      if (
        antennaData.azimuthBoardServoStatusBits !== undefined &&
        antennaData.azimuthBoardServoStatusBits !== null
      ) {
        azimuthBoardServoStatusBits.value = safeToString(antennaData.azimuthBoardServoStatusBits)
      }
      if (
        antennaData.azimuthBoardStatusBits !== undefined &&
        antennaData.azimuthBoardStatusBits !== null
      ) {
        azimuthBoardStatusBits.value = safeToString(antennaData.azimuthBoardStatusBits)
      }
      if (
        antennaData.elevationBoardServoStatusBits !== undefined &&
        antennaData.elevationBoardServoStatusBits !== null
      ) {
        elevationBoardServoStatusBits.value = safeToString(
          antennaData.elevationBoardServoStatusBits,
        )
      }
      if (
        antennaData.elevationBoardStatusBits !== undefined &&
        antennaData.elevationBoardStatusBits !== null
      ) {
        elevationBoardStatusBits.value = safeToString(antennaData.elevationBoardStatusBits)
      }
      if (
        antennaData.tiltBoardServoStatusBits !== undefined &&
        antennaData.tiltBoardServoStatusBits !== null
      ) {
        tiltBoardServoStatusBits.value = safeToString(antennaData.tiltBoardServoStatusBits)
      }
      if (
        antennaData.tiltBoardStatusBits !== undefined &&
        antennaData.tiltBoardStatusBits !== null
      ) {
        tiltBoardStatusBits.value = safeToString(antennaData.tiltBoardStatusBits)
      }

      // Feed 보드 상태
      if (
        antennaData.feedSBoardStatusBits !== undefined &&
        antennaData.feedSBoardStatusBits !== null
      ) {
        feedSBoardStatusBits.value = safeToString(antennaData.feedSBoardStatusBits)
      }
      if (
        antennaData.feedXBoardStatusBits !== undefined &&
        antennaData.feedXBoardStatusBits !== null
      ) {
        feedXBoardStatusBits.value = safeToString(antennaData.feedXBoardStatusBits)
      }

      // LNA 전류 데이터
      if (
        antennaData.currentSBandLNALHCP !== undefined &&
        antennaData.currentSBandLNALHCP !== null
      ) {
        currentSBandLNALHCP.value = safeToString(antennaData.currentSBandLNALHCP)
      }
      if (
        antennaData.currentSBandLNARHCP !== undefined &&
        antennaData.currentSBandLNARHCP !== null
      ) {
        currentSBandLNARHCP.value = safeToString(antennaData.currentSBandLNARHCP)
      }
      if (
        antennaData.currentXBandLNALHCP !== undefined &&
        antennaData.currentXBandLNALHCP !== null
      ) {
        currentXBandLNALHCP.value = safeToString(antennaData.currentXBandLNALHCP)
      }
      if (
        antennaData.currentXBandLNARHCP !== undefined &&
        antennaData.currentXBandLNARHCP !== null
      ) {
        currentXBandLNARHCP.value = safeToString(antennaData.currentXBandLNARHCP)
      }

      // RSSI 데이터
      if (antennaData.rssiSBandLNALHCP !== undefined && antennaData.rssiSBandLNALHCP !== null) {
        rssiSBandLNALHCP.value = safeToString(antennaData.rssiSBandLNALHCP)
      }
      if (antennaData.rssiSBandLNARHCP !== undefined && antennaData.rssiSBandLNARHCP !== null) {
        rssiSBandLNARHCP.value = safeToString(antennaData.rssiSBandLNARHCP)
      }
      if (antennaData.rssiXBandLNALHCP !== undefined && antennaData.rssiXBandLNALHCP !== null) {
        rssiXBandLNALHCP.value = safeToString(antennaData.rssiXBandLNALHCP)
      }
      if (antennaData.rssiXBandLNARHCP !== undefined && antennaData.rssiXBandLNARHCP !== null) {
        rssiXBandLNARHCP.value = safeToString(antennaData.rssiXBandLNARHCP)
      }

      // 가속도 데이터
      if (
        antennaData.azimuthAcceleration !== undefined &&
        antennaData.azimuthAcceleration !== null
      ) {
        azimuthAcceleration.value = safeToString(antennaData.azimuthAcceleration)
      }
      if (
        antennaData.elevationAcceleration !== undefined &&
        antennaData.elevationAcceleration !== null
      ) {
        elevationAcceleration.value = safeToString(antennaData.elevationAcceleration)
      }
      if (antennaData.tiltAcceleration !== undefined && antennaData.tiltAcceleration !== null) {
        tiltAcceleration.value = safeToString(antennaData.tiltAcceleration)
      }
      if (
        antennaData.azimuthMaxAcceleration !== undefined &&
        antennaData.azimuthMaxAcceleration !== null
      ) {
        azimuthMaxAcceleration.value = safeToString(antennaData.azimuthMaxAcceleration)
      }
      if (
        antennaData.elevationMaxAcceleration !== undefined &&
        antennaData.elevationMaxAcceleration !== null
      ) {
        elevationMaxAcceleration.value = safeToString(antennaData.elevationMaxAcceleration)
      }
      if (
        antennaData.tiltMaxAcceleration !== undefined &&
        antennaData.tiltMaxAcceleration !== null
      ) {
        tiltMaxAcceleration.value = safeToString(antennaData.tiltMaxAcceleration)
      }

      // 트래킹 데이터
      if (
        antennaData.trackingAzimuthTime !== undefined &&
        antennaData.trackingAzimuthTime !== null
      ) {
        trackingAzimuthTime.value = safeToString(antennaData.trackingAzimuthTime)
      }
      if (
        antennaData.trackingCMDAzimuthAngle !== undefined &&
        antennaData.trackingCMDAzimuthAngle !== null
      ) {
        trackingCMDAzimuthAngle.value = safeToString(antennaData.trackingCMDAzimuthAngle)
      }
      if (
        antennaData.trackingActualAzimuthAngle !== undefined &&
        antennaData.trackingActualAzimuthAngle !== null
      ) {
        trackingActualAzimuthAngle.value = safeToString(antennaData.trackingActualAzimuthAngle)
      }
      if (
        antennaData.trackingElevationTime !== undefined &&
        antennaData.trackingElevationTime !== null
      ) {
        trackingElevationTime.value = safeToString(antennaData.trackingElevationTime)
      }
      if (
        antennaData.trackingCMDElevationAngle !== undefined &&
        antennaData.trackingCMDElevationAngle !== null
      ) {
        trackingCMDElevationAngle.value = safeToString(antennaData.trackingCMDElevationAngle)
      }
      if (
        antennaData.trackingActualElevationAngle !== undefined &&
        antennaData.trackingActualElevationAngle !== null
      ) {
        trackingActualElevationAngle.value = safeToString(antennaData.trackingActualElevationAngle)
      }
      if (antennaData.trackingTiltTime !== undefined && antennaData.trackingTiltTime !== null) {
        trackingTiltTime.value = safeToString(antennaData.trackingTiltTime)
      }
      if (
        antennaData.trackingCMDTiltAngle !== undefined &&
        antennaData.trackingCMDTiltAngle !== null
      ) {
        trackingCMDTiltAngle.value = safeToString(antennaData.trackingCMDTiltAngle)
      }
      if (
        antennaData.trackingActualTiltAngle !== undefined &&
        antennaData.trackingActualTiltAngle !== null
      ) {
        trackingActualTiltAngle.value = safeToString(antennaData.trackingActualTiltAngle)
      }
    } catch (e) {
      console.error('❌ 센서 데이터 업데이트 오류:', e)
    }
  }

  // 30ms 타이머 시작
  const startUIUpdates = () => {
    if (updateTimer.value) {
      clearInterval(updateTimer.value)
    }

    console.log(`🚀 UI 업데이트 타이머 시작 (${UPDATE_INTERVAL}ms 주기)`)
    isUpdating.value = true
    updateCount.value = 0

    // 30ms마다 UI 업데이트
    updateTimer.value = setInterval(() => {
      updateUIFromBuffer()
    }, UPDATE_INTERVAL)
  }

  // 타이머 중지

  const stopUIUpdates = () => {
    if (updateTimer.value) {
      clearInterval(updateTimer.value)
      updateTimer.value = null
    }

    isUpdating.value = false
    console.log('⏹️ UI 업데이트 타이머 중지')
  }

  // WebSocket 연결 설정
  const connectWebSocket = async () => {
    try {
      error.value = ''

      console.log('🔌 WebSocket 연결 시작')

      // WebSocket 연결 (메시지는 버퍼에만 저장)
      await icdService.connectWebSocket(WEBSOCKET_URL, handleWebSocketMessage)
      isConnected.value = true

      console.log('✅ WebSocket 연결 성공')
    } catch (e) {
      const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
      error.value = `WebSocket 연결 실패: ${errorMessage}`
      isConnected.value = false
      throw e
    }
  }

  // WebSocket 연결 해제
  const disconnectWebSocket = () => {
    try {
      icdService.disconnectWebSocket()
      isConnected.value = false
      latestDataBuffer.value = null
      bufferUpdateTime.value = 0
    } catch (e) {
      console.error('WebSocket 연결 해제 중 오류:', e)
    }
  }

  // 초기화
  const initialize = async () => {
    try {
      console.log('🎬 icdStore 초기화 (WebSocket + 30ms 타이머)')

      // WebSocket 연결
      await connectWebSocket()

      // UI 업데이트 타이머 시작
      startUIUpdates()

      console.log('✅ 초기화 완료')
    } catch (e) {
      console.error('❌ 초기화 실패:', e)
    }
  }

  // 정리
  const cleanup = () => {
    stopUIUpdates()
    disconnectWebSocket()
  }
/*
  // ✅ Boolean 기반 computed 속성들 수정
  const ephemerisStatusInfo = computed(() => ({
    status: ephemerisStatus.value,
    isActive: ephemerisStatus.value === true,
    isInactive: ephemerisStatus.value === false,
    isUnknown: ephemerisStatus.value === null,
    displayText:
      ephemerisStatus.value === true
        ? 'ACTIVE'
        : ephemerisStatus.value === false
          ? 'INACTIVE'
          : 'UNKNOWN',
  }))

  const passScheduleStatusInfo = computed(() => ({
    status: passScheduleStatus.value,
    isActive: passScheduleStatus.value === true,
    isInactive: passScheduleStatus.value === false,
    isUnknown: passScheduleStatus.value === null,
    displayText:
      passScheduleStatus.value === true
        ? 'ACTIVE'
        : passScheduleStatus.value === false
          ? 'INACTIVE'
          : 'UNKNOWN',
  }))

  const sunTrackStatusInfo = computed(() => ({
    status: sunTrackStatus.value,
    isActive: sunTrackStatus.value === true,
    isInactive: sunTrackStatus.value === false,
    isUnknown: sunTrackStatus.value === null,
    displayText:
      sunTrackStatus.value === true
        ? 'ACTIVE'
        : sunTrackStatus.value === false
          ? 'INACTIVE'
          : 'UNKNOWN',
  })) */
  // 명령 전송 메서드들
  const sendEmergency = async (commandType: 'E' | 'S' = 'E') => {
    try {
      error.value = ''
      return await icdService.sendEmergency(commandType)
    } catch (e) {
      const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
      error.value = `비상 정지 명령 실패: ${errorMessage}`
      throw e
    }
  }

  // 멀티 컨트롤 명령 전송
  const sendMultiControlCommand = async (command: MultiControlCommand) => {
    try {
      error.value = ''
      const result = await icdService.sendMultiControlCommand(command)
      return {
        success: true,
        data: result,
        message: '멀티 컨트롤 명령이 성공적으로 전송되었습니다.',
      }
    } catch (e) {
      const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
      error.value = `멀티 컨트롤 명령 전송 실패: ${errorMessage}`
      return {
        success: false,
        error: errorMessage,
        message: `멀티 컨트롤 명령 전송에 실패했습니다.`,
      }
    }
  }
  // 서보 프리셋 명령 전송
  const sendServoPresetCommand = async (azimuth: number, elevation: number, tilt: number) => {
    try {
      const response = await icdService.sendServoPresetCommand(azimuth > 0, elevation > 0, tilt > 0)
      return { success: true, data: response, message: '서보 프리셋 명령이 전송되었습니다.' }
    } catch (error) {
      console.error('서보 프리셋 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: '서보 프리셋 명령 전송에 실패했습니다.',
      }
    }
  }

  // 정지 명령 전송
  const stopCommand = async (azimuth: boolean, elevation: boolean, tilt: boolean) => {
    try {
      const response = await icdService.stopCommand(azimuth, elevation, tilt)
      return { success: true, data: response, message: '정지 명령이 전송되었습니다.' }
    } catch (error) {
      console.error('정지 명령 전송 실패:', error)
      return { success: false, error: String(error), message: '정지 명령 전송에 실패했습니다.' }
    }
  }

  // Stow 명령 전송
  const stowCommand = async () => {
    try {
      const response = await icdService.stowCommand()
      return { success: true, data: response, message: 'Stow 명령이 전송되었습니다.' }
    } catch (error) {
      console.error('Stow 명령 전송 실패:', error)
      return { success: false, error: String(error), message: 'Stow 명령 전송에 실패했습니다.' }
    }
  }

  // Feed On/Off 명령 전송
  const sendFeedOnOffCommand = async (
    sLHCP: boolean,
    sRHCP: boolean,
    sRFSwitch: boolean,
    xLHCP = false,
    xRHCP = false,
    fan = false,
  ) => {
    try {
      const response = await icdService.sendFeedOnOffCommand(
        sLHCP,
        sRHCP,
        sRFSwitch,
        xLHCP,
        xRHCP,
        fan,
      )
      return { success: true, data: response, message: 'Feed On/Off 명령이 전송되었습니다.' }
    } catch (error) {
      console.error('Feed On/Off 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: 'Feed On/Off 명령 전송에 실패했습니다.',
      }
    }
  }

  // Sun Track 시작
  const startSunTrack = async (
    interval: number,
    azSpeed: number,
    elSpeed: number,
    tiltSpeed: number,
  ) => {
    try {
      const response = await icdService.startSunTrack(interval, azSpeed, elSpeed, tiltSpeed)
      return { success: true, data: response, message: 'Sun Track이 시작되었습니다.' }
    } catch (error) {
      console.error('Sun Track 시작 실패:', error)
      return { success: false, error: String(error), message: 'Sun Track 시작에 실패했습니다.' }
    }
  }
  // Sun Track 중지지
  /* const stopSunTrack = async () => {
    try {
      const response = await icdService.stopSunTrack()
      return { success: true, data: response, message: 'Sun Track이 중지지되었습니다.' }
    } catch (error) {
      console.error('Sun Track 시작 실패:', error)
      return { success: false, error: String(error), message: 'Sun Track 중지지에 실패했습니다.' }
    }
  } */

  // 위치 오프셋 명령 전송
  const sendPositionOffsetCommand = async (
    azOffset: number,
    elOffset: number,
    tiOffset: number,
  ) => {
    try {
      const response = await icdService.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)
      return {
        success: true,
        data: response,
        message: '위치 오프셋 명령이 전송되었습니다.',
        azimuthResult: 0, // 실제 응답 구조에 맞게 수정 필요
        elevationResult: 0, // 실제 응답 구조에 맞게 수정 필요
        tiltResult: 0, // 실제 응답 구조에 맞게 수정 필요
      }
    } catch (error) {
      console.error('위치 오프셋 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: '위치 오프셋 명령 전송에 실패했습니다.',
        azimuthResult: 0,
        elevationResult: 0,
        tiltResult: 0,
      }
    }
  }

  // 시간 오프셋 명령 전송
  const sendTimeOffsetCommand = async (timeOffset: number) => {
    try {
      const response = await icdService.sendTimeOffsetCommand(timeOffset)
      return {
        success: true,
        data: response,
        message: '시간 오프셋 명령이 전송되었습니다.',
        inputTimeoffset: 0, // 실제 응답 구조에 맞게 수정 필요
        resultTimeOffset: 0, // 실제 응답 구조에 맞게 수정 필요
      }
    } catch (error) {
      console.error('시간 오프셋 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: '시간 오프셋 명령 전송에 실패했습니다.',
        inputTimeoffset: 0,
        resultTimeOffset: 0,
      }
    }
  }

  // 디버깅 함수
  const getDebugInfo = () => {
    return {
      isConnected: isConnected.value,
      isUpdating: isUpdating.value,
      updateCount: updateCount.value,
      bufferAge: bufferUpdateTime.value ? Date.now() - bufferUpdateTime.value : 0,
      hasBufferData: !!latestDataBuffer.value,
      lastServerTime: serverTime.value,
      lastUpdateTime: lastUpdateTimeFormatted.value,
    }
  }

  // 컴포넌트가 언마운트될 때 정리
  onScopeDispose(() => {
    cleanup()
  })

  // 공개할 상태와 메서드 반환
  return {
    // 상태
    serverTime,
    resultTimeOffsetCalTime,
    modeStatusBits,
    azimuthAngle,
    azimuthSpeed,
    elevationAngle,
    elevationSpeed,
    tiltAngle,
    tiltSpeed,
    cmdAzimuthAngle,
    cmdElevationAngle,
    cmdTiltAngle,
    cmdTime,
    servoDriverAzimuthAngle,
    servoDriverElevationAngle,
    servoDriverTiltAngle,
    torqueAzimuth,
    torqueElevation,
    torqueTilt,
    windSpeed,
    windDirection,
    rtdOne,
    rtdTwo,
    mainBoardProtocolStatusBits,
    mainBoardStatusBits,
    mainBoardMCOnOffBits,
    mainBoardReserveBits,
    azimuthBoardServoStatusBits,
    azimuthBoardStatusBits,
    elevationBoardServoStatusBits,
    elevationBoardStatusBits,
    tiltBoardServoStatusBits,
    tiltBoardStatusBits,
    feedSBoardStatusBits,
    feedXBoardStatusBits,
    currentSBandLNALHCP,
    currentSBandLNARHCP,
    currentXBandLNALHCP,
    currentXBandLNARHCP,
    rssiSBandLNALHCP,
    rssiSBandLNARHCP,
    rssiXBandLNALHCP,
    rssiXBandLNARHCP,
    azimuthAcceleration,
    elevationAcceleration,
    tiltAcceleration,
    azimuthMaxAcceleration,
    elevationMaxAcceleration,
    tiltMaxAcceleration,
    trackingAzimuthTime,
    trackingCMDAzimuthAngle,
    trackingActualAzimuthAngle,
    trackingElevationTime,
    trackingCMDElevationAngle,
    trackingActualElevationAngle,
    trackingTiltTime,
    trackingCMDTiltAngle,
    trackingActualTiltAngle,

    error,
    isConnected,

    isUpdating,
    updateCount,
    messageDelay,

    // 계산된 속성
    hasActiveConnection,
    lastUpdateTimeFormatted,
    connectionStatus,

    // 메서드
    initialize,
    cleanup,

    startUIUpdates,
    stopUIUpdates,
    connectWebSocket,
    disconnectWebSocket,
    getDebugInfo,
    sendEmergency,
    sendMultiControlCommand,
    sendServoPresetCommand,
    stopCommand,
    stowCommand,
    sendFeedOnOffCommand,
    startSunTrack,
    sendPositionOffsetCommand,
    sendTimeOffsetCommand,
  }
})
