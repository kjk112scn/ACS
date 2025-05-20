import { api } from 'boot/axios'
import { API_ENDPOINTS } from '../config/apiEndpoints'

export interface MessageData {
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
  tilt?: boolean
  stow?: boolean
  azAngle?: number
  azSpeed?: number
  elAngle?: number
  elSpeed?: number
  tiAngle?: number
  tiSpeed?: number
}

export class CommandError extends Error {
  status: CommandStatus;
  originalError?: unknown;

  constructor(message: string, status: CommandStatus, originalError?: unknown) {
    super(message);
    this.name = 'CommandError';
    this.status = status;
    this.originalError = originalError;

    // Error 객체의 프로토타입 체인 유지를 위한 설정
    Object.setPrototypeOf(this, CommandError.prototype);

    // 스택 트레이스 보존
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, CommandError);
    }
  }
}

class WebSocketService {
  private websocket: WebSocket | null = null
  private messageHandler: WebSocketMessageHandler | null = null
  private pingInterval: ReturnType<typeof setInterval> | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

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
        
        this.websocket = new WebSocket(url)

        this.websocket.onopen = () => {
          console.log('WebSocket 연결 성공')
          this.reconnectAttempts = 0
          this.setupPing()
          resolve()
        }

        this.websocket.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data) as MessageData
            this.messageHandler?.(message)
          } catch (error) {
            console.error('WebSocket 메시지 파싱 오류:', error)
          }
        }

        this.websocket.onclose = () => {
          console.log('WebSocket 연결 종료')
          this.cleanup()
          this.attemptReconnect()
        }

        this.websocket.onerror = (event) => {
          const error = new Error(`WebSocket error: ${event.type}`)
          console.error('WebSocket 오류:', error)
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
    if (this.reconnectAttempts < this.maxReconnectAttempts && this.messageHandler) {
      this.reconnectAttempts++
      console.log(`재연결 시도 중... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        if (this.websocket?.url) {
          this.connect(this.websocket.url, this.messageHandler!)
            .catch((error: Error) => console.error('재연결 실패:', error))
        }
      }, this.reconnectDelay)
    } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('최대 재연결 시도 횟수 초과')
    }
  }

  /**
   * 주기적인 핑 전송 설정
   */
  private setupPing(): void {
    this.cleanupPing()
    
    this.pingInterval = setInterval(() => {
      if (this.websocket?.readyState === WebSocket.OPEN) {
        this.websocket.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }))
      }
    }, 30000) // 30초마다 핑 전송
  }

  /**
   * 핑 인터벌 정리
   */
  private cleanupPing(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval)
      this.pingInterval = null
    }
  }

  /**
   * 리소스 정리
   */
  private cleanup(): void {
    this.cleanupPing()
  }
}

export const icdService = {
  webSocketService: new WebSocketService(),
  
  /**
   * WebSocket 연결 설정
   * @param url WebSocket 서버 URL
   * @param onMessage 메시지 수신 핸들러
   */
  async connectWebSocket(url: string, onMessage: WebSocketMessageHandler): Promise<void> {
    return this.webSocketService.connect(url, onMessage)
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
      const response = await api.post(API_ENDPOINTS.ICD.EMERGENCY_STOP, null, {
        params: {
          commandType
        }
      })
      console.log('비상 정지 명령 전송 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('비상 정지 명령 전송 실패:', error)
      throw error
    }
  },

  /**
   * Sun Track 시작 명령 전송
   * @param interval 간격
   * @param azimuthSpeed 방위각 속도
   * @param elevationSpeed 고도각 속도
   * @param tiltSpeed 틸트각 속도
   */
  async startSunTrack(
    interval: number,
    azimuthSpeed: number,
    elevationSpeed: number,
    tiltSpeed: number,
  ) {
    try {
      const response = await api.post(API_ENDPOINTS.SUN_TRACK.START, null, {
        params: {
          interval,
          cmdAzimuthSpeed: azimuthSpeed,
          cmdElevationSpeed: elevationSpeed,
          cmdTiltSpeed: tiltSpeed,
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
      console.log('Multi control command sent:', response.data)
      return {
        data: response.data,
        status: {
          message: '멀티 컨트롤 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        }
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
          tiOffest: tiOffset, // 백엔드 파라미터 이름에 맞춤 (tiOffest)
        },
      })
      console.log('Position offset command sent:', response.data)
      return {
        data: response.data,
        status: {
          message: '오프셋 명령이 성공적으로 전송되었습니다.',
          success: true,
          timestamp: Date.now(),
        } as CommandStatus
      }
    } catch (error) {
      console.error('Position offset command failed:', error)
      const errorStatus = {
        message: '오프셋 명령 전송 중 오류가 발생했습니다.',
        success: false,
        timestamp: Date.now(),
      } as CommandStatus

      throw new CommandError('오프셋 명령 전송 실패', errorStatus, error);
    }
  },

  /**
   * Feed On/Off 명령 전송
   * @param sLHCP LHCP 설정
   * @param sRHCP RHCP 설정
   * @param sRFSwitch RF 스위치 설정
   * @param xLHCP X-LHCP 설정
   * @param xRHCP X-RHCP 설정
   * @param fan 팬 설정
   */
  async sendFeedOnOffCommand(
    sLHCP: boolean = false,
    sRHCP: boolean = false,
    sRFSwitch: boolean = false,
    xLHCP: boolean = false,
    xRHCP: boolean = false,
    fan: boolean = false,
  ) {
    try {
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
        } as CommandStatus
      }
    } catch (error) {
      console.error('Time offset command failed:', error)
      const errorStatus = {
        message: '시간 오프셋 명령 전송 중 오류가 발생했습니다.',
        success: false,
        timestamp: Date.now(),
      } as CommandStatus

      throw new CommandError('시간 오프셋 명령 전송 실패', errorStatus, error);
    }
  },

  /**
   * 서보 프리셋 명령 전송
   * @param azimuth 방위각 프리셋 여부
   * @param elevation 고도각 프리셋 여부
   * @param tilt 틸트각 프리셋 여부
   */
  async sendServoPresetCommand(azimuth: boolean = false, elevation: boolean = false, tilt: boolean = false) {
    try {
      const response = await api.post('/icd/servo-preset-command', null, {
        params: {
          azimuth,
          elevation,
          tilt,
        },
      })
      console.log('Servo preset command sent:', response.data)
      return {
        success: true,
        message: response.data,
        data: response.data
      }
    } catch (error) {
      console.error('Servo preset command failed:', error)
      return {
        success: false,
        message: '서보 프리셋 명령 전송 중 오류가 발생했습니다.',
        error
      }
    }
  },

  /**
   * 위치 설정 명령 전송
   * @param command 위치 설정 명령 객체
   */
  async setPosition(command: {
    azimuthAngle: number;
    elevationAngle: number;
    tiltAngle: number;
    timestamp: string;
  }) {
    try {
      const response = await api.post('/icd/set-position', command)
      console.log('위치 지정 명령 전송 성공:', response.data)
      return response.data
    } catch (error) {
      console.error('위치 지정 명령 전송 실패:', error)
      throw error
    }
  }
}
