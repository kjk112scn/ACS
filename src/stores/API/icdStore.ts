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
const WEBSOCKET_URL = 'ws://localhost:8080/ws/push-data'

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
  const lastMessageTime = ref(0)

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
  const hasActiveConnection = computed(() => isConnected.value)
  const lastUpdateTime = computed(() => new Date(lastMessageTime.value).toLocaleTimeString())
  const connectionStatus = computed(() => ({
    isConnected: isConnected.value,
    lastUpdate: lastUpdateTime.value,
    messageDelay: messageDelay.value,
  }))

  // 메시지 처리 함수
  const processDirectData = (message: MessageData) => {
    try {
      const now = Date.now()
      lastMessageTime.value = now
      messageDelay.value = now - (message.timestamp ? Number(message.timestamp) : now)

      // 메시지에서 데이터 추출하여 상태 업데이트
      if (message.azimuthAngle !== undefined)
        azimuthAngle.value = safeToString(message.azimuthAngle)
      if (message.elevationAngle !== undefined)
        elevationAngle.value = safeToString(message.elevationAngle)
      if (message.tiltAngle !== undefined) tiltAngle.value = safeToString(message.tiltAngle)
      if (message.azimuthSpeed !== undefined)
        azimuthSpeed.value = safeToString(message.azimuthSpeed)
      if (message.elevationSpeed !== undefined)
        elevationSpeed.value = safeToString(message.elevationSpeed)
      if (message.tiltSpeed !== undefined) tiltSpeed.value = safeToString(message.tiltSpeed)
      if (message.modeStatusBits !== undefined)
        modeStatusBits.value = safeToString(message.modeStatusBits)
      if (message.cmdAzimuthAngle !== undefined)
        cmdAzimuthAngle.value = safeToString(message.cmdAzimuthAngle)
      if (message.cmdElevationAngle !== undefined)
        cmdElevationAngle.value = safeToString(message.cmdElevationAngle)
      if (message.cmdTiltAngle !== undefined)
        cmdTiltAngle.value = safeToString(message.cmdTiltAngle)
      if (message.cmdTime !== undefined) cmdTime.value = safeToString(message.cmdTime)
      if (message.serverTime !== undefined) serverTime.value = safeToString(message.serverTime)
      if (message.resultTimeOffsetCalTime !== undefined) {
        resultTimeOffsetCalTime.value = safeToString(message.resultTimeOffsetCalTime)
      }
    } catch (e) {
      console.error('데이터 처리 중 오류 발생:', e)
      error.value = '데이터 처리 중 오류가 발생했습니다.'
    }
  }

  // WebSocket 메시지 핸들러
  const handleWebSocketMessage = (message: MessageData) => {
    try {
      // 토픽과 상관없이 최상위 레벨의 중요 필드들을 항상 처리
      if (message.serverTime !== undefined) {
        serverTime.value = safeToString(message.serverTime)
        console.log('서버 시간 업데이트:', serverTime.value)
      }

      if (message.resultTimeOffsetCalTime !== undefined)
        resultTimeOffsetCalTime.value = safeToString(message.resultTimeOffsetCalTime)

      if (message.cmdAzimuthAngle !== undefined)
        cmdAzimuthAngle.value = safeToString(message.cmdAzimuthAngle)
      if (message.cmdElevationAngle !== undefined)
        cmdElevationAngle.value = safeToString(message.cmdElevationAngle)
      if (message.cmdTiltAngle !== undefined)
        cmdTiltAngle.value = safeToString(message.cmdTiltAngle)

      // 이후 메시지 구조에 따라 데이터 처리
      if (message.data) {
        // data 필드가 있는 경우 처리 (토픽 상관없이)
        if (typeof message.data === 'object' && message.data !== null) {
          processDirectData(message.data as MessageData)
        } else {
          console.warn('지원하지 않는 data 필드 형식:', message.data)
        }
      } else if (
        'azimuthAngle' in message ||
        'elevationAngle' in message ||
        'tiltAngle' in message
      ) {
        // 직접 데이터 필드가 있는 경우 처리
        processDirectData(message)
      } else if (!message.data && message.topic !== 'read') {
        // data 필드가 없고 read 토픽이 아닌 경우 경고
        console.warn('데이터 필드가 없는 메시지:', message)
      }
    } catch (e) {
      console.error('메시지 처리 중 오류 발생:', e, message)
      error.value = '메시지 처리 중 오류가 발생했습니다.'
    }
  }

  // WebSocket 연결 설정
  const connectWebSocket = async () => {
    try {
      error.value = ''
      await icdService.connectWebSocket(WEBSOCKET_URL, handleWebSocketMessage)
      isConnected.value = true
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
      error.value = ''
    } catch (e) {
      console.error('WebSocket 연결 해제 중 오류:', e)
      error.value = 'WebSocket 연결 해제 중 오류가 발생했습니다.'
    }
  }

  // 컴포넌트가 언마운트될 때 정리
  onScopeDispose(() => {
    disconnectWebSocket()
  })

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
  const initialize = () => {
    connectWebSocket().catch(console.error)
  }

  // 정리
  const cleanup = () => {
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
    messageDelay,
    lastMessageTime,
    lastOffsetCommandStatus,
    lastTimeOffsetCommandStatus,
    lastMultiControlCommandStatus,

    // 계산된 속성
    hasActiveConnection,
    lastUpdateTime,
    connectionStatus,

    // 메서드
    connectWebSocket,
    disconnectWebSocket,
    sendEmergency,
    sendMultiControlCommand,
    initialize,
    cleanup,
    sendServoPresetCommand,
    stopCommand,
    stowCommand,
    sendFeedOnOffCommand,
    startSunTrack,
    sendPositionOffsetCommand,
    sendTimeOffsetCommand,
  }
})
