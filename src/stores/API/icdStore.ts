import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose } from 'vue'
import {
  icdService,
  type MessageData,
  type CommandStatus,
  type MultiControlCommand,
} from '../../services/icdService'

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
  // 상태 정의
  const serverTime = ref('')
  const resultTimeOffsetCalTime = ref('')
  const modeStatusBits = ref('')
  const azimuthAngle = ref('')
  const azimuthSpeed = ref('')
  const elevationAngle = ref('')
  const elevationSpeed = ref('')
  const tiltAngle = ref('')
  const tiltSpeed = ref('')
  const cmdAzimuthAngle = ref('')
  const cmdElevationAngle = ref('')
  const cmdTiltAngle = ref('')
  const cmdTime = ref('')
  const error = ref('')
  const isConnected = ref(false)
  const messageDelay = ref(0)

  // 타이머 관련 상태

  const updateTimer = ref<NodeJS.Timeout | null>(null)
  const isUpdating = ref(false)
  const updateCount = ref(0)
  const lastUpdateTime = ref(0)

  // 최신 데이터 버퍼 (WebSocket에서 받은 데이터 임시 저장)
  const latestDataBuffer = ref<MessageData | null>(null)
  const bufferUpdateTime = ref(0)

  // 명령 상태
  const lastOffsetCommandStatus = ref<CommandStatus>({
    message: '',
    success: true,
    timestamp: 0,
  })

  const lastTimeOffsetCommandStatus = ref<CommandStatus>({
    message: '',
    success: true,
    timestamp: 0,
  })

  const lastMultiControlCommandStatus = ref<CommandStatus>({
    message: '',
    success: true,
    timestamp: 0,
  })

  // 계산된 속성

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

      // 센서 데이터 업데이트

      if (message.data && typeof message.data === 'object') {
        updateSensorData(message.data)
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

  // 센서 데이터 업데이트 함수

  const updateSensorData = (sensorData: Record<string, unknown>) => {
    try {
      if (sensorData.azimuthAngle !== undefined && sensorData.azimuthAngle !== null) {
        azimuthAngle.value = safeToString(sensorData.azimuthAngle)
      }
      if (sensorData.elevationAngle !== undefined && sensorData.elevationAngle !== null) {
        elevationAngle.value = safeToString(sensorData.elevationAngle)
      }
      if (sensorData.tiltAngle !== undefined && sensorData.tiltAngle !== null) {
        tiltAngle.value = safeToString(sensorData.tiltAngle)
      }
      if (sensorData.azimuthSpeed !== undefined && sensorData.azimuthSpeed !== null) {
        azimuthSpeed.value = safeToString(sensorData.azimuthSpeed)
      }
      if (sensorData.elevationSpeed !== undefined && sensorData.elevationSpeed !== null) {
        elevationSpeed.value = safeToString(sensorData.elevationSpeed)
      }
      if (sensorData.tiltSpeed !== undefined && sensorData.tiltSpeed !== null) {
        tiltSpeed.value = safeToString(sensorData.tiltSpeed)
      }
      if (sensorData.modeStatusBits !== undefined && sensorData.modeStatusBits !== null) {
        modeStatusBits.value = safeToString(sensorData.modeStatusBits)
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

  const sendMultiControlCommand = async (command: MultiControlCommand) => {
    try {
      error.value = ''
      const result = await icdService.sendMultiControlCommand(command)
      lastMultiControlCommandStatus.value = {
        message: '명령이 성공적으로 전송되었습니다.',
        success: true,
        timestamp: Date.now(),
      }
      return result
    } catch (e) {
      const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
      lastMultiControlCommandStatus.value = {
        message: `명령 전송 실패: ${errorMessage}`,
        success: false,
        timestamp: Date.now(),
      }
      error.value = `명령 전송 실패: ${errorMessage}`
      throw e
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
    error,
    isConnected,

    isUpdating,
    updateCount,
    messageDelay,

    lastUpdateTime,
    lastOffsetCommandStatus,
    lastTimeOffsetCommandStatus,
    lastMultiControlCommandStatus,

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
