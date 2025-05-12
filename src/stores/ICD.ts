import { defineStore } from 'pinia'
import { api } from 'boot/axios'

// 명령 상태를 위한 인터페이스 정의
interface CommandStatus {
  message: string
  success: boolean
  timestamp: number
}

// 멀티 컨트롤 명령을 위한 인터페이스 정의
interface MultiControlCommand {
  azimuth?: boolean
  elevation?: boolean
  tilt?: boolean
  stow?: boolean
  azAngle?: number
  azSpeed?: number
  elAngle?: number
  elSpeed?: number
  tiAngle?: number
  tiSpeed?: number
}

// 메시지 데이터 인터페이스 정의
interface MessageData {
  [key: string]: unknown
  topic?: string
  data?: string | Record<string, unknown> | MessageData
  azimuthAngle?: number | string
  elevationAngle?: number | string
  tiltAngle?: number | string
  azimuthSpeed?: number | string
  elevationSpeed?: number | string
  tiltSpeed?: number | string
  modeStatusBits?: string
  cmdAzimuthAngle?: number | string
  cmdElevationAngle?: number | string
  cmdTiltAngle?: number | string
  cmdTime?: string
  serverTime?: string
  resultTimeOffsetCalTime?: string
}

// 값을 안전하게 문자열로 변환하는 헬퍼 함수
function safeToString(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint') {
    return String(value)
  }

  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return '[복잡한 객체]' // 기본 [object Object] 대신 더 명확한 메시지
    }
  }

  if (typeof value === 'function') {
    return '[함수]'
  }

  if (typeof value === 'symbol') {
    return value.toString()
  }

  // 다른 모든 타입에 대한 안전한 처리 - 이 부분은 실행될 가능성이 거의 없음
  // (위에서 모든 JavaScript 타입을 처리했기 때문)
  return `[알 수 없는 타입: ${typeof value}]`
}
export const useICDStore = defineStore('icd', {
  state: () => ({
    serverTime: '',
    resultTimeOffsetCalTime: '',
    modeStatusBits: '',
    azimuthAngle: '',
    azimuthSpeed: '',
    elevationAngle: '',
    elevationSpeed: '',
    tiltAngle: '',
    tiltSpeed: '',
    cmdAzimuthAngle: '',
    cmdElevationAngle: '',
    cmdTiltAngle: '',
    cmdTime: '',
    error: '',
    isConnected: false,
    websocket: null as WebSocket | null,
    pingInterval: null as ReturnType<typeof setInterval> | null,
    lastMessageTime: 0,
    messageDelay: 0,

    // 명령 상태들
    lastOffsetCommandStatus: {
      message: '',
      success: true,
      timestamp: 0,
    } as CommandStatus,

    lastTimeOffsetCommandStatus: {
      message: '',
      success: true,
      timestamp: 0,
    } as CommandStatus,

    lastMultiControlCommandStatus: {
      message: '',
      success: true,
      timestamp: 0,
    } as CommandStatus,
  }),
  actions: {
    connectWebSocket() {
      console.log('WebSocket 연결 시도...')

      // 기존 연결 정리
      this.disconnectWebSocket()

      this.websocket = new WebSocket('ws://localhost:8080/ws/push-data')

      this.websocket.onopen = () => {
        console.log('WebSocket 연결 성공.')
        this.isConnected = true
        this.error = ''
      }

      this.websocket.onmessage = (event) => {
        //console.log('Raw WebSocket data:', event.data)

        try {
          // 외부 JSON 파싱
          const message = JSON.parse(event.data) as MessageData
          //console.log('Parsed message:', message)

          // 데이터 구조 확인
          if (message.topic === 'read') {
            this.processReadData(message)
          } else if (
            'azimuthAngle' in message ||
            'elevationAngle' in message ||
            'tiltAngle' in message
          ) {
            // 필드가 최상위 레벨에 있는 경우 직접 처리
            this.processDirectData(message)
          } else if (message.data) {
            // data 필드가 있는 경우
            this.processReadData(message)
          } else {
            // 다른 모든 경우
            this.processUnknownData(message)
          }
        } catch (e) {
          console.error('JSON 파싱 오류:', e)
          this.error = '데이터 파싱 오류 발생'
        }
      }

      this.websocket.onclose = () => {
        console.log('WebSocket 연결 종료.')
        this.isConnected = false

        // 핑 인터벌 정리
        if (this.pingInterval) {
          clearInterval(this.pingInterval)
          this.pingInterval = null
        }

        // 화살표 함수를 사용하여 this 컨텍스트 유지
        //setTimeout(() => this.connectWebSocket(), 3000)
      }

      this.websocket.onerror = (err) => {
        console.error('WebSocket 오류:', err)
        this.error = 'WebSocket 연결 오류 발생'
        this.isConnected = false
      }

      // 연결 유지를 위한 주기적 핑 설정
      this.pingInterval = setInterval(() => {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
          this.websocket.send(
            JSON.stringify({
              type: 'ping',
              timestamp: Date.now(),
            }),
          )
        }
      }, 30000) // 30초마다 핑
    },

    // 직접 데이터 처리 메서드 (최상위 레벨 필드용)
    processDirectData(message: MessageData) {
      try {
        //console.log('직접 데이터 처리:', message)

        // 명시적으로 상태 업데이트
        this.$patch({
          cmdAzimuthAngle:
            message.cmdAzimuthAngle !== undefined
              ? safeToString(message.cmdAzimuthAngle)
              : this.cmdAzimuthAngle,
          cmdElevationAngle:
            message.cmdElevationAngle !== undefined
              ? safeToString(message.cmdElevationAngle)
              : this.cmdElevationAngle,
          cmdTiltAngle:
            message.cmdTiltAngle !== undefined
              ? safeToString(message.cmdTiltAngle)
              : this.cmdTiltAngle,
          cmdTime: message.cmdTime !== undefined ? safeToString(message.cmdTime) : this.cmdTime,
          modeStatusBits:
            message.modeStatusBits !== undefined
              ? safeToString(message.modeStatusBits)
              : this.modeStatusBits,
          azimuthAngle:
            message.azimuthAngle !== undefined
              ? safeToString(message.azimuthAngle)
              : this.azimuthAngle,
          azimuthSpeed:
            message.azimuthSpeed !== undefined
              ? safeToString(message.azimuthSpeed)
              : this.azimuthSpeed,
          elevationAngle:
            message.elevationAngle !== undefined
              ? safeToString(message.elevationAngle)
              : this.elevationAngle,
          elevationSpeed:
            message.elevationSpeed !== undefined
              ? safeToString(message.elevationSpeed)
              : this.elevationSpeed,
          tiltAngle:
            message.tiltAngle !== undefined ? safeToString(message.tiltAngle) : this.tiltAngle,
          tiltSpeed:
            message.tiltSpeed !== undefined ? safeToString(message.tiltSpeed) : this.tiltSpeed,
        })
        /*
        // 값 할당 후 확인 로깅
        console.log('Store 상태 업데이트 후:', {
          azimuthAngle: this.azimuthAngle,
          elevationAngle: this.elevationAngle,
          tiltAngle: this.tiltAngle,
          serverTime: this.serverTime,
        })
 */
      } catch (e) {
        console.error('직접 데이터 처리 오류:', e)
      }
    },
    // READ 데이터 처리
    processReadData(message: MessageData) {
      try {
        let readDataObj: MessageData
        if (typeof message.data === 'string') {
          readDataObj = JSON.parse(message.data)
        } else if (message.data !== undefined) {
          readDataObj = message.data as MessageData
        } else {
          readDataObj = { ...message } // message 자체를 복사하여 사용
        }

        // readDataObj에서 data 필드 접근 (중첩된 구조)
        const readData =
          typeof readDataObj.data === 'object' && readDataObj.data !== null
            ? (readDataObj.data as Record<string, unknown>)
            : readDataObj // data가 없으면 readDataObj 자체를 사용

        //console.log('READ 데이터 처리:', readData)

        // 서버 시간 정보 처리
        if (message.serverTime !== undefined) {
          this.serverTime = safeToString(message.serverTime)
        } else if (readDataObj.serverTime !== undefined) {
          this.serverTime = safeToString(readDataObj.serverTime)
        }

        if (message.resultTimeOffsetCalTime !== undefined) {
          this.resultTimeOffsetCalTime = safeToString(message.resultTimeOffsetCalTime)
        } else if (readDataObj.resultTimeOffsetCalTime !== undefined) {
          this.resultTimeOffsetCalTime = safeToString(readDataObj.resultTimeOffsetCalTime)
        }

        // 명시적으로 상태 업데이트 (null 체크 포함)
        this.$patch({
          cmdAzimuthAngle:
            message.cmdAzimuthAngle !== undefined
              ? safeToString(message.cmdAzimuthAngle)
              : this.cmdAzimuthAngle,
          cmdElevationAngle:
            message.cmdElevationAngle !== undefined
              ? safeToString(message.cmdElevationAngle)
              : this.cmdElevationAngle,
          cmdTiltAngle:
            message.cmdTiltAngle !== undefined
              ? safeToString(message.cmdTiltAngle)
              : this.cmdTiltAngle,
          cmdTime: message.cmdTime !== undefined ? safeToString(message.cmdTime) : this.cmdTime,
          modeStatusBits:
            readData.modeStatusBits !== undefined
              ? safeToString(readData.modeStatusBits)
              : this.modeStatusBits,
          azimuthAngle:
            readData.azimuthAngle !== undefined
              ? safeToString(readData.azimuthAngle)
              : this.azimuthAngle,
          azimuthSpeed:
            readData.azimuthSpeed !== undefined
              ? safeToString(readData.azimuthSpeed)
              : this.azimuthSpeed,
          elevationAngle:
            readData.elevationAngle !== undefined
              ? safeToString(readData.elevationAngle)
              : this.elevationAngle,
          elevationSpeed:
            readData.elevationSpeed !== undefined
              ? safeToString(readData.elevationSpeed)
              : this.elevationSpeed,
          tiltAngle:
            readData.tiltAngle !== undefined ? safeToString(readData.tiltAngle) : this.tiltAngle,
          tiltSpeed:
            readData.tiltSpeed !== undefined ? safeToString(readData.tiltSpeed) : this.tiltSpeed,
        })
        /*
        // 값 할당 후 확인 로깅
        console.log('Store 상태 업데이트 후:', {
          azimuthAngle: this.azimuthAngle,
          elevationAngle: this.elevationAngle,
          tiltAngle: this.tiltAngle,
          serverTime: this.serverTime,
        })
         */
      } catch (e) {
        console.error('READ 데이터 처리 오류:', e)
      }
    },

    // 알 수 없는 데이터 구조 처리
    processUnknownData(message: Record<string, unknown>) {
      //console.log('알 수 없는 데이터 구조:', message)

      // 필요한 필드가 있는지 확인
      if ('azimuthAngle' in message || 'elevationAngle' in message || 'tiltAngle' in message) {
        // 필드가 최상위 레벨에 있는 경우
        this.processDirectData(message as MessageData)
      } else if (message.data) {
        // data 필드가 있는 경우
        this.processReadData(message as MessageData)
      } else {
        console.warn('처리할 수 없는 데이터 형식:', message)
      }
    },
    async sendEmergency() {
      try {
        const response = await api.post('/icd/on-emergency-stop-command')
        console.log('비상 정지 명령 전송 성공:', response.data)
        return response.data
      } catch (error) {
        console.error('비상 정지 명령 전송 실패:', error)
        throw error
      }
    },

    // Sun Track 시작 명령 전송 함수
    async startSunTrack(
      interval: number,
      azimuthSpeed: number,
      elevationSpeed: number,
      tiltSpeed: number,
    ) {
      try {
        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/sun-track/start-sun-track', null, {
          params: {
            interval,
            cmdAzimuthSpeed: azimuthSpeed,
            cmdElevationSpeed: elevationSpeed,
            cmdTiltSpeed: tiltSpeed,
          },
        })

        // 응답 처리
        console.log('Start Sun Track command sent:', response.data)
        return response.data
      } catch (error) {
        console.error('Start Sun Track command failed:', error)
        throw error
      }
    },

    // Sun Track 중지 명령 전송 함수
    async stopSunTrack() {
      try {
        // API 호출
        const response = await api.post('/sun-track/stop-sun-track')

        // 응답 처리
        console.log('Stop Sun Track command sent:', response.data)
        return response.data
      } catch (error) {
        console.error('Stop Sun Track command failed:', error)
        throw error
      }
    },
    async stopAllCommand() {
      await this.stopSunTrack()
    },

    // Stop 명령 전송 함수
    async stopCommand(azStop: boolean, elStop: boolean, tiStop: boolean) {
      try {
        // Sun Track 중지 먼저 시도
        try {
          await this.stopAllCommand()
          console.log('Sun Track 중지 성공')
        } catch (sunTrackError) {
          console.warn('Sun Track 중지 실패, 계속 진행:', sunTrackError)
          // Sun Track 중지 실패해도 계속 진행
        }

        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/icd/stop-command', null, {
          params: {
            azStop,
            elStop,
            tiStop,
          },
        })

        // 응답 처리
        console.log('Stop command sent:', response.data)
        return response.data
      } catch (error) {
        console.error('Stop command failed:', error)
        throw error
      }
    },

    // Stow 명령 전송 함수
    async stowCommand() {
      try {
        // API 호출
        const response = await api.post('/icd/stow-command')
        // 응답 처리
        console.log('Stow command sent:', response.data)
        return response.data
      } catch (error) {
        console.error('Stow command failed:', error)
        throw error
      }
    },

    // 멀티 컨트롤 명령 전송 함수
    async sendMultiControlCommand(command: MultiControlCommand) {
      try {
        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/icd/multi-control-command', null, {
          params: {
            azimuth: command.azimuth || false,
            elevation: command.elevation || false,
            tilt: command.tilt || false,
            stow: command.stow || false,
            azAngle: command.azAngle || 0,
            azSpeed: command.azSpeed || 0,
            elAngle: command.elAngle || 0,
            elSpeed: command.elSpeed || 0,
            tiAngle: command.tiAngle || 0,
            tiSpeed: command.tiSpeed || 0,
          },
        })
        // 응답 처리
        console.log('Multi control command sent:', response.data)
        // 상태 업데이트
        this.lastMultiControlCommandStatus = {
          message: '멀티 컨트롤 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        }
        return response.data
      } catch (error) {
        console.error('Multi control command failed:', error)
        // 오류 상태 업데이트
        this.lastMultiControlCommandStatus = {
          message: '멀티 컨트롤 명령 전송 중 오류가 발생했습니다.',
          success: false,
          timestamp: Date.now(),
        }
        throw error
      }
    },

    // 위치 오프셋 명령 전송 함수
    async sendPositionOffsetCommand(azOffset: number, elOffset: number, tiOffset: number) {
      try {
        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/icd/position-offset-command', null, {
          params: {
            azOffset,
            elOffset,
            tiOffest: tiOffset, // 백엔드 파라미터 이름에 맞춤 (tiOffest)
          },
        })
        // 응답 처리
        console.log('Position offset command sent:', response.data)
        // 상태 업데이트
        /*     this.lastOffsetCommandStatus = {
          message: '오프셋 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        } */
        return response.data
      } catch (error) {
        console.error('Position offset command failed:', error)
        // 오류 상태 업데이트
        /*         this.lastOffsetCommandStatus = {
          message: '오프셋 명령 전송 중 오류가 발생했습니다.',
          success: false,
          timestamp: Date.now(),
        }
        throw error */
      }
    },

    // Feed On/Off 명령 전송 함수
    async sendFeedOnOffCommand(
      sLHCP: boolean = false,
      sRHCP: boolean = false,
      sRFSwitch: boolean = false,
      xLHCP: boolean = false,
      xRHCP: boolean = false,
      fan: boolean = false,
    ) {
      try {
        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/icd/feed-on-off-command', null, {
          params: {
            sLHCP,
            sRHCP,
            sRFSwitch,
            xLHCP,
            xRHCP,
            fan,
          },
        })
        // 응답 처리
        console.log('Feed On/Off command sent:', response.data)
        return response.data
      } catch (error) {
        console.error('Feed On/Off command failed:', error)
        throw error
      }
    },

    // 시간 오프셋 명령 전송 함수
    async sendTimeOffsetCommand(timeOffset: number) {
      try {
        // API 호출 (쿼리 파라미터로 전송)
        const response = await api.post('/icd/time-offset-command', null, {
          params: {
            inputTimeOffset: timeOffset,
          },
        })
        // 응답 처리
        console.log('Time offset command sent:', response.data)
        // 상태 업데이트
        /*        this.lastTimeOffsetCommandStatus = {
          message: '시간 오프셋 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        } */
        return response.data
      } catch (error) {
        console.error('Time offset command failed:', error)
        // 오류 상태 업데이트
        /*    this.lastTimeOffsetCommandStatus = {
          message: '시간 오프셋 명령 전송 중 오류가 발생했습니다.',
          success: false,
          timestamp: Date.now(),
        }
        throw error */
      }
    },
    disconnectWebSocket() {
      if (this.websocket) {
        this.websocket.close()
        this.websocket = null
      }

      if (this.pingInterval) {
        clearInterval(this.pingInterval)
        this.pingInterval = null
      }
    },

    initialize() {
      this.connectWebSocket()
    },

    cleanup() {
      this.disconnectWebSocket()
    },
  },
})
