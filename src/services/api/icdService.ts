import { api } from '@/boot/axios'
import { handleConnectionChange } from '@/utils/connectionManager'
export interface TrackingStatus {
  ephemerisStatus?: boolean | null
  ephemerisTrackingState?: string | null
  passScheduleStatus?: boolean | null
  sunTrackStatus?: boolean | null
  sunTrackTrackingState?: string | null
}

export interface MessageData {
  [key: string]: unknown
  topic?: string
  data?: string | Record<string, unknown> | MessageData
  azimuthAngle?: number | string
  elevationAngle?: number | string
  trainAngle?: number | string
  azimuthSpeed?: number | string
  elevationSpeed?: number | string
  trainSpeed?: number | string
  modeStatusBits?: string
  cmdAzimuthAngle?: number | string
  cmdElevationAngle?: number | string
  cmdTrainAngle?: number | string
  cmdTime?: string
  serverTime?: string
  resultTimeOffsetCalTime?: string

  // ✅ 추적 상태 추가
  trackingStatus?: TrackingStatus
}

type WebSocketMessageHandler = (message: MessageData) => void

// 명령 상태를 위한 인터페이스 정의
export interface CommandStatus {
  message: string
  success: boolean
  timestamp: number
}

// 멀티 컨트롤 명령을 위한 인터페이스 정의
export interface MultiControlCommand {
  azimuth?: boolean
  elevation?: boolean
  train?: boolean
  stow?: boolean
  azAngle?: number
  azSpeed?: number
  elAngle?: number
  elSpeed?: number
  trainAngle?: number
  trainSpeed?: number
}

export class CommandError extends Error {
  status: CommandStatus
  originalError?: unknown

  constructor(message: string, status: CommandStatus, originalError?: unknown) {
    super(message)
    this.name = 'CommandError'
    this.status = status
    this.originalError = originalError

    // Error 객체의 프로토타입 체인 유지를 위한 설정
    Object.setPrototypeOf(this, CommandError.prototype)

    // 스택 트레이스 보존
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, CommandError)
    }
  }
}

class WebSocketService {
  private static instance: WebSocketService | null = null
  private websocket: WebSocket | null = null
  private messageHandler: WebSocketMessageHandler | null = null
  private pingInterval: ReturnType<typeof setInterval> | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private currentUrl: string = '' // 현재 연결된 WebSocket URL 저장
  private subscribers = new Map<string, WebSocketMessageHandler[]>() // 구독자 관리

  /**
   * 싱글톤 인스턴스 반환
   */
  static getInstance(): WebSocketService {
    if (!WebSocketService.instance) {
      WebSocketService.instance = new WebSocketService()
    }
    return WebSocketService.instance
  }

  /**
   * 구독자 추가
   */
  subscribe(key: string, handler: WebSocketMessageHandler): void {
    if (!this.subscribers.has(key)) {
      this.subscribers.set(key, [])
    }
    this.subscribers.get(key)?.push(handler)
    console.log(`[WebSocket] 구독자 추가: ${key}, 총 구독자: ${this.subscribers.get(key)?.length}`)
  }

  /**
   * 구독자 제거
   */
  unsubscribe(key: string, handler: WebSocketMessageHandler): void {
    const handlers = this.subscribers.get(key)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
        console.log(`[WebSocket] 구독자 제거: ${key}, 남은 구독자: ${handlers.length}`)
      }
    }
  }

  /**
   * 모든 구독자에게 메시지 브로드캐스트
   */
  private broadcastMessage(message: MessageData): void {
    this.subscribers.forEach((handlers, key) => {
      handlers.forEach((handler) => {
        try {
          handler(message)
        } catch (error) {
          console.error(`[WebSocket] 구독자 ${key} 메시지 처리 오류:`, error)
        }
      })
    })
  }

  /**
   * WebSocket 연결 설정
   * @param url WebSocket 서버 URL
   * @param onMessage 메시지 수신 핸들러
   */
  connect(url: string, onMessage: WebSocketMessageHandler): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.disconnect()
        this.messageHandler = onMessage
        this.currentUrl = url // 현재 URL 저장

        console.log(`[WebSocket] 새로운 연결 시도: ${url}`)
        this.websocket = new WebSocket(url)

        // WebSocket 버퍼링 상태 로깅
        console.log(`[WebSocket] 버퍼링 상태: ${this.websocket.bufferedAmount} bytes`)

        // WebSocket 이벤트 리스너 추가
        this.websocket.onopen = () => {
          console.log(
            `[WebSocket] 연결 열림 - readyState: ${this.getReadyStateString(this.websocket.readyState)}`,
          )
          console.log('WebSocket 연결 성공')
          this.reconnectAttempts = 0

          // ✅ 연결 상태 업데이트 및 재연결 감지
          handleConnectionChange(true, {
            minDisconnectDuration: 5000, // 5초 이상 끊겼으면 백엔드 재시작으로 간주
            onReconnected: () => {
              console.log('🔄 백엔드 재연결 감지 - localStorage 초기화됨')
            },
          })

          resolve()
        }

        this.websocket.onmessage = (event) => {
          try {
            //console.info('WebSocket 메시지:', event.data)
            const message = JSON.parse(event.data) as MessageData

            // 기존 단일 핸들러 호출 (호환성 유지)
            this.messageHandler?.(message)

            // 모든 구독자에게 브로드캐스트
            this.broadcastMessage(message)
          } catch (error) {
            console.error('WebSocket 메시지 파싱 오류:', error)
          }
        }

        this.websocket.onclose = (event) => {
          const closeInfo = {
            code: event.code,
            reason: event.reason || '알 수 없음',
            wasClean: event.wasClean,

            readyState: this.websocket?.readyState,
            timestamp: new Date().toISOString(),

            stack: new Error().stack,
          }

          console.log('[WebSocket] 연결 종료 - 상세 정보:', JSON.stringify(closeInfo, null, 2))

          // 종료 코드에 따른 처리
          switch (event.code) {
            case 1000:
              console.log('정상 종료')
              break
            case 1001:
              console.log('서버 종료 또는 브라우저 탭 닫힘')
              break
            case 1005:
              console.warn('상태 코드 없이 연결 종료됨 (코드 1005) - 네트워크 문제 가능성 높음')
              // 네트워크 상태 확인
              if (navigator.onLine) {
                console.log('브라우저는 온라인 상태입니다. 서버 문제일 가능성이 있습니다.')
              } else {
                console.warn('브라우저가 오프라인 상태입니다. 네트워크 연결을 확인하세요.')
              }
              break
            case 1006:
              console.error('비정상 종료 (서버 응답 없음)')
              break
            // 기타 코드 처리...
          }

          // ✅ 연결 끊김 상태 업데이트
          handleConnectionChange(false)

          this.cleanup()

          // 정상 종료가 아닌 경우에만 재연결 시도
          if (event.code !== 1000 && event.code !== 1001) {
            // 코드 1005의 경우 네트워크 상태 확인 후 재연결
            if (event.code === 1005) {
              // 네트워크 상태 확인 후 재연결
              if (navigator.onLine) {
                console.log('네트워크 연결 확인됨, 재연결 시도...')
                this.attemptReconnect()
              } else {
                // 네트워크 연결이 복구되면 재연결 시도
                const onlineHandler = () => {
                  console.log('네트워크 연결 복구됨, 재연결 시도...')
                  window.removeEventListener('online', onlineHandler)
                  this.attemptReconnect()
                }
                window.addEventListener('online', onlineHandler)
              }
            } else {
              this.attemptReconnect()
            }
          }
        }

        this.websocket.onerror = (event) => {
          const error = new Error(`WebSocket error: ${event.type}`)
          console.error('WebSocket 오류:', error)

          // ✅ 에러 발생 시 연결 끊김 상태 업데이트
          handleConnectionChange(false)

          reject(error)
        }
      } catch (error) {
        const wsError = error instanceof Error ? error : new Error(String(error))
        console.error('WebSocket 연결 실패:', wsError)
        reject(wsError)
      }
    })
  }

  /**
   * WebSocket 연결 해제
   */
  disconnect(): void {
    this.cleanup()

    if (this.websocket) {
      this.websocket.close()
      this.websocket = null
    }
  }

  /**
   * 재연결 시도
   */
  private attemptReconnect(): void {
    if (
      this.reconnectAttempts < this.maxReconnectAttempts &&
      this.messageHandler &&
      this.currentUrl
    ) {
      this.reconnectAttempts++
      console.log(`재연결 시도 중... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

      setTimeout(() => {
        this.connect(this.currentUrl, this.messageHandler).catch((error) =>
          console.error('재연결 실패:', error),
        )
      }, this.reconnectDelay)
    } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('최대 재연결 시도 횟수 초과')
      // 일정 시간 후 재시도 카운터 초기화
      setTimeout(() => {
        this.reconnectAttempts = 0
      }, 60000) // 1분 후 재시도
    }
  }

  /**
   * 리소스 정리
   */
  /**
   * WebSocket readyState를 문자열로 변환
   */
  private getReadyStateString(state: number): string {
    const states = {
      0: 'CONNECTING',
      1: 'OPEN',
      2: 'CLOSING',
      3: 'CLOSED',
    }
    return states[state as keyof typeof states] || `UNKNOWN(${state})`
  }

  /**
   * WebSocket 종료 코드를 메시지로 변환
   */
  private getCloseStatusMessage(code: number): string {
    const statusMessages: { [key: number]: string } = {
      1000: '정상 종료',
      1001: '서버가 사라짐 또는 브라우저가 페이지를 벗어남',
      1002: '프로토콜 오류',
      1003: '지원하지 않는 데이터 유형',
      1005: '상태 코드 없음',
      1006: '비정상 종료',
      1007: '일관성 없는 데이터',
      1008: '정책 위반',
      1009: '메시지가 너무 큼',
      1010: '클라이언트가 확장을 요구함',
      1011: '예기치 않은 조건',
      1012: '서비스 재시작 중',
      1013: '일시적인 서버 상태',
      1015: 'TLS 핸드셰이크 실패',
    }
    return statusMessages[code] || `알 수 없는 상태 코드 (${code})`
  }

  private cleanup(): void {
    console.log('[WebSocket] 리소스 정리 시작')

    // 핑 인터벌 정리
    if (this.pingInterval) {
      console.log('[WebSocket] 핑 인터벌 정리')
      clearInterval(this.pingInterval)
      this.pingInterval = null
    }

    // WebSocket 정리
    if (this.websocket) {
      const state = this.getReadyStateString(this.websocket.readyState)
      console.log(`[WebSocket] WebSocket 정리 (현재 상태: ${state})`)

      // 이벤트 리스너 제거
      this.websocket.onopen = null
      this.websocket.onclose = null
      this.websocket.onerror = null
      this.websocket.onmessage = null

      // 연결이 열려있으면 닫기
      if (this.websocket.readyState === WebSocket.OPEN) {
        console.log('[WebSocket] WebSocket 연결 닫는 중...')
        this.websocket.close(1000, '클라이언트에서 연결 종료')
      }

      // WebSocket 객체는 유지하되 참조는 유지 (재연결을 위해)
    } else {
      console.log('[WebSocket] 정리할 WebSocket 인스턴스가 없음')
    }

    console.log('[WebSocket] 리소스 정리 완료')
  }
}

export const icdService = {
  webSocketService: WebSocketService.getInstance(),

  /**
   * WebSocket 연결 설정
   * @param url WebSocket 서버 URL
   * @param onMessage 메시지 수신 핸들러
   */
  async connectWebSocket(url: string, onMessage: WebSocketMessageHandler): Promise<void> {
    return await this.webSocketService.connect(url, onMessage)
  },

  /**
   * WebSocket 구독자 추가
   * @param key 구독자 키
   * @param handler 메시지 핸들러
   */
  subscribeWebSocket(key: string, handler: WebSocketMessageHandler): void {
    this.webSocketService.subscribe(key, handler)
  },

  /**
   * WebSocket 구독자 제거
   * @param key 구독자 키
   * @param handler 메시지 핸들러
   */
  unsubscribeWebSocket(key: string, handler: WebSocketMessageHandler): void {
    this.webSocketService.unsubscribe(key, handler)
  },

  /**
   * WebSocket 연결 해제
   */
  disconnectWebSocket(): void {
    this.webSocketService.disconnect()
  },
  /**
   * 비상 정지 명령 전송
   * @param commandType 'E' 또는 'S' 값
   */
  async sendEmergency(commandType: 'E' | 'S' = 'E') {
    try {
      const response = await api.post('/icd/on-emergency-stop-command', null, {
        params: {
          commandType,
        },
      })
      console.log('비상 정지 명령 전송 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('비상 정지 명령 전송 실패:', error)
      throw error
    }
  },
  /**
   * 비상 정지 명령 전송
   * @param commandType 'E' 또는 'S' 값
   */
  async sendWriteNTP() {
    try {
      const response = await api.post('/icd/write-ntp-command')

      console.log('sendWriteNTP 명령 전송 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('sendWriteNTP 명령 전송 실패:', error)
      throw error
    }
  },
  /**
   * Sun Track 시작 명령 전송
   * @param interval 간격
   * @param azimuthSpeed 방위각 속도
   * @param elevationSpeed 고도각 속도
   * @param trainSpeed 틸트각 속도
   */
  async startSunTrack(
    interval: number,
    azimuthSpeed: number,
    elevationSpeed: number,
    trainSpeed: number,
  ) {
    try {
      const response = await api.post('/sun-track/start-sun-track', null, {
        params: {
          interval,
          cmdAzimuthSpeed: azimuthSpeed,
          cmdElevationSpeed: elevationSpeed,
          cmdTrainSpeed: trainSpeed,
        },
      })
      console.log('Start Sun Track command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Start Sun Track command failed:', error)
      throw error
    }
  },

  /**
   * Sun Track 중지 명령 전송
   */
  async stopSunTrack() {
    try {
      const response = await api.post('/sun-track/stop-sun-track')
      console.log('Stop Sun Track command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Stop Sun Track command failed:', error)
      throw error
    }
  },

  /**
   * 모든 명령 중지
   */
  async stopAllCommand() {
    return await this.stopSunTrack()
  },
  async standbyCommand(azStandby: boolean, elStandby: boolean, tiStandby: boolean) {
    try {
      const response = await api.post('/icd/standby-command', null, {
        params: {
          azStandby,
          elStandby,
          tiStandby,
        },
      })
      console.log('Standby command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Standby command failed:', error)
      throw error
    }
  },
  /**
   * Stop 명령 전송
   * @param azStop 방위각 정지 여부
   * @param elStop 고도각 정지 여부
   * @param tiStop 틸트각 정지 여부
   */
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

      const response = await api.post('/icd/stop-command', null, {
        params: {
          azStop,
          elStop,
          tiStop,
        },
      })
      console.log('Stop command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Stop command failed:', error)
      throw error
    }
  },

  /**
   * Stow 명령 전송
   */
  async stowCommand() {
    try {
      const response = await api.post('/icd/stow-command')
      console.log('Stow command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Stow command failed:', error)
      throw error
    }
  },

  /**
   * 멀티 컨트롤 명령 전송
   * @param command 멀티 컨트롤 명령 객체
   */
  async sendMultiControlCommand(command: MultiControlCommand) {
    try {
      const response = await api.post('/icd/multi-control-command', null, {
        params: {
          azimuth: command.azimuth || false,
          elevation: command.elevation || false,
          train: command.train || false,
          stow: command.stow || false,
          azAngle: command.azAngle || 0,
          azSpeed: command.azSpeed || 0,
          elAngle: command.elAngle || 0,
          elSpeed: command.elSpeed || 0,
          trainAngle: command.trainAngle || 0,
          trainSpeed: command.trainSpeed || 0,
        },
      })
      console.log('Multi control command sent:', response.data)
      return {
        data: response.data,
        status: {
          message: '멀티 컨트롤 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        },
      }
    } catch (error) {
      console.error('Multi control command failed:', error)
      throw error
    }
  },

  /**
   * 위치 오프셋 명령 전송
   * @param azOffset 방위각 오프셋
   * @param elOffset 고도각 오프셋
   * @param tiOffset 틸트각 오프셋
   */
  async sendPositionOffsetCommand(azOffset: number, elOffset: number, tiOffset: number) {
    try {
      const response = await api.post('/icd/position-offset-command', null, {
        params: {
          azOffset,
          elOffset,
          tiOffset: tiOffset, // 백엔드 파라미터 이름에 맞춤 (tiOffest)
        },
      })
      console.log('Position offset command sent:', response.data)
      return {
        data: response.data,
        status: {
          message: '오프셋 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        } as CommandStatus,
      }
    } catch (error) {
      console.error('Position offset command failed:', error)
      const errorStatus = {
        message: '오프셋 명령 전송 중 오류가 발생했습니다.',
        success: false,
        timestamp: Date.now(),
      } as CommandStatus

      throw new CommandError('오프셋 명령 전송 실패', errorStatus, error)
    }
  },

  /**
   * Feed On/Off 명령 전송
   * @param sLHCP S-Band LHCP 설정
   * @param sRHCP S-Band RHCP 설정
   * @param sRFSwitch S-Band RF 스위치 설정 (false: RHCP, true: LHCP)
   * @param xLHCP X-Band LHCP 설정
   * @param xRHCP X-Band RHCP 설정
   * @param fan 팬 설정
   * @param kaLHCP Ka-Band LHCP 설정 (추가)
   * @param kaRHCP Ka-Band RHCP 설정 (추가)
   * @param kaSelectionRHCP Ka-Band RHCP Selection 설정 (false: Band1, true: Band2) (추가)
   * @param kaSelectionLHCP Ka-Band LHCP Selection 설정 (false: Band1, true: Band2) (추가)
   */
  async sendFeedOnOffCommand(
    sLHCP: boolean = false,
    sRHCP: boolean = false,
    sRFSwitch: boolean = false,
    xLHCP: boolean = false,
    xRHCP: boolean = false,
    fan: boolean = false,
    kaLHCP: boolean = false,
    kaRHCP: boolean = false,
    kaSelectionRHCP: boolean = false,
    kaSelectionLHCP: boolean = false,
  ) {
    try {
      // 모든 파라미터가 false인지 확인 (Ka-Band 포함)
      const allParamsFalse =
        !sLHCP &&
        !sRHCP &&
        !sRFSwitch &&
        !xLHCP &&
        !xRHCP &&
        !fan &&
        !kaLHCP &&
        !kaRHCP &&
        !kaSelectionRHCP &&
        !kaSelectionLHCP

      let params

      if (allParamsFalse) {
        // 모든 파라미터가 false면 0x00 전송
        params = {
          feedCommand: 0x00,
        }
      } else {
        // 하나라도 true면 개별 파라미터 전송
        params = {
          sLHCP,
          sRHCP,
          sRFSwitch,
          xLHCP,
          xRHCP,
          fan,
          kaLHCP,
          kaRHCP,
          kaSelectionRHCP,
          kaSelectionLHCP,
        }
      }

      const response = await api.post('/icd/feed-on-off-command', null, {
        params,
      })

      console.log('Feed On/Off command sent:', response.data)
      return response.data
    } catch (error) {
      console.error('Feed On/Off command failed:', error)
      throw error
    }
  },

  /**
   * 시간 오프셋 명령 전송
   * @param timeOffset 시간 오프셋
   */
  async sendTimeOffsetCommand(timeOffset: number) {
    try {
      const response = await api.post('/icd/time-offset-command', null, {
        params: {
          inputTimeOffset: timeOffset,
        },
      })
      console.log('Time offset command sent:', response.data)
      return {
        data: response.data,
        status: {
          message: '시간 오프셋 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        } as CommandStatus,
      }
    } catch (error) {
      console.error('Time offset command failed:', error)
      const errorStatus = {
        message: '시간 오프셋 명령 전송 중 오류가 발생했습니다.',
        success: false,
        timestamp: Date.now(),
      } as CommandStatus

      throw new CommandError('시간 오프셋 명령 전송 실패', errorStatus, error)
    }
  },

  /**
   * 서보 프리셋 명령 전송
   * @param azimuth 방위각 프리셋 여부
   * @param elevation 고도각 프리셋 여부
   * @param train 틸트각 프리셋 여부
   */
  async sendServoPresetCommand(
    azimuth: boolean = false,
    elevation: boolean = false,
    train: boolean = false,
  ) {
    try {
      const response = await api.post('/icd/servo-preset-command', null, {
        params: {
          azimuth,
          elevation,
          train: train,
        },
      })
      console.log('Servo preset command sent:', response.data)
      return {
        success: true,
        message: response.data,
        data: response.data,
      }
    } catch (error) {
      console.error('Servo preset command failed:', error)
      return {
        success: false,
        message: '서보 프리셋 명령 전송 중 오류가 발생했습니다.',
        error,
      }
    }
  },

  /**
   * 위치 설정 명령 전송
   * @param command 위치 설정 명령 객체
   */
  async setPosition(command: {
    azimuthAngle: number
    elevationAngle: number
    trainAngle: number
    timestamp: string
  }) {
    try {
      const response = await api.post('/icd/set-position', command)
      console.log('위치 지정 명령 전송 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('위치 지정 명령 전송 실패:', error)
      throw error
    }
  },

  // 실시간 데이터 요청 메서드 추가
  async getRealtimeData() {
    try {
      const response = await api.get('/icd/realtime-data', {
        timeout: 25, // 30ms 주기보다 짧게 설정
      })

      return {
        data: response.data,
        timestamp: Date.now(),
      }
    } catch (error) {
      // 타이머 방식에서는 에러를 던지지 않고 로그만
      if (Math.random() < 0.01) {
        // 1% 확률로만 로그 (너무 많은 로그 방지)
        console.warn('실시간 데이터 요청 실패:', error)
      }
      return null
    }
  },

  async sendMCOnOffCommand(onOff: boolean = true) {
    try {
      const response = await api.post('/icd/mc-on-off-command', null, {
        params: {
          onOff: onOff.toString(),
        },
      })

      console.log('M/C On/Off 명령 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('M/C On/Off 명령 오류:', error)
      throw error
    }
  },

  async sendServoAlarmResetCommand(
    azimuth: boolean = false,
    elevation: boolean = false,
    train: boolean = false,
  ) {
    try {
      const response = await api.post('/icd/servo-alarm-reset', null, {
        params: {
          azimuth: azimuth.toString(),
          elevation: elevation.toString(),
          train: train.toString(),
        },
      })

      console.log('Servo Alarm Reset 명령 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('Servo Alarm Reset 명령 오류:', error)
      throw error
    }
  },

  /**
   * 2.3 Read Positioner Status
   * 3-Axis의 각도 정보를 수신 받기 위한 프로토콜이다.
   * 주요 정보: 3-Axis 각도 정보(Azimuth / Elevation / Tilt)
   * 주요 사용처: 상시
   */
  async sendReadPositionerStatusCommand() {
    try {
      const response = await api.post('/icd/read-positioner-status')

      console.log('Read Positioner Status 명령 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('Read Positioner Status 명령 오류:', error)
      throw error
    }
  },

  /**
   * 2.4 Read Firmware Version/Serial Number Info
   * 각 축의 Board F/W Version, Serial Number 정보를 수신 받기 위한 프로토콜이다.
   * 주요 정보: Board F/W Version, Serial Number
   * 주요 사용처: 설정모드
   */
  async sendReadFwVerSerialNoStatusCommand() {
    try {
      const response = await api.get('/icd/firmware-version-serial-no')

      console.log('Read Firmware Version/Serial Number Info 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('Read Firmware Version/Serial Number Info 오류:', error)
      throw error
    }
  },
}
