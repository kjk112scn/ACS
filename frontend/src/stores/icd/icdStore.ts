import { defineStore } from 'pinia'
import { ref, shallowRef, computed, onScopeDispose, readonly } from 'vue'
import { icdService, type MessageData, type MultiControlCommand } from '@/services'
import type { HardwareErrorLog } from '@/types/hardwareError'
import { T } from '@/texts'
import { getWebSocketUrl } from '@/utils/api-config'

// Composables - 순수 파싱 함수들
import {
  parseProtocolStatusBits as parseProtocolBits,
  parseMainBoardStatusBits as parseMainBoardBits,
  parseMainBoardMCOnOffBits as parseMCOnOffBits,
  parseServoStatusBits,
  parseAzimuthBoardStatusBits as parseAzimuthBits,
  parseElevationBoardStatusBits as parseElevationBits,
  parseTrainBoardStatusBits as parseTrainBits,
  parseFeedSBoardStatusBits as parseFeedSBits,
  parseFeedXBoardStatusBits as parseFeedXBits,
  parseFeedBoardETCStatusBits as parseFeedETCBits,
  parseFeedKaBoardStatusBits as parseFeedKaBits,
} from './composables/useBoardStatus'
import {
  parseTrackingStatusUpdate,
  type CurrentTrackingState,
} from './composables/useTrackingState'

// 값을 안전하게 문자열로 변환하는 헬퍼 함수
const safeToString = (value: unknown): string => {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      // JSON.stringify 실패 시 객체의 toString() 메서드 사용
      if (value && typeof value === 'object' && 'toString' in value) {
        const toStringResult = (value as { toString(): string }).toString()
        // [object Object]가 아닌 경우에만 반환
        if (toStringResult !== '[object Object]') {
          return toStringResult
        }
      }
      // 그 외의 경우 타입 정보와 함께 반환
      return `[${typeof value}]`
    }
  }
  // 기본 타입의 경우 타입을 명시적으로 체크
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  // 알 수 없는 타입의 경우 타입 정보 반환
  return `[${typeof value}]`
}

const WEBSOCKET_URL = getWebSocketUrl()

const UPDATE_INTERVAL = 30 // 30ms 주기

export const useICDStore = defineStore('icd', () => {
  // 기본 상태 정의
  const serverTime = ref('')
  const resultTimeOffsetCalTime = ref('')
  const cmdAzimuthAngle = ref('')
  const cmdElevationAngle = ref('')
  const cmdTrainAngle = ref('')
  const cmdTime = ref('')
  const error = ref('')
  const isConnected = ref(false)
  const messageDelay = ref(0)

  /**
   * 하드웨어 에러 키를 다국어 메시지로 변환하는 함수
   * @param errorKey - 에러 키 (예: 'ELEVATION_SERVO_ALARM')
   * @param isResolved - 에러가 해결되었는지 여부
   * @returns 변환된 메시지
   */
  const translateHardwareError = (errorKey: string, isResolved: boolean): string => {
    try {
      // 해결된 에러인 경우 _RESOLVED 접미사 추가
      const key = isResolved ? `${errorKey}_RESOLVED` : errorKey

      // T.value.hardwareErrors에서 동적으로 키 조회
      const hardwareErrors = T.value.hardwareErrors as Record<string, string>
      const translatedMessage = hardwareErrors[key]

      // 번역이 없는 경우 원본 키 반환
      if (!translatedMessage) {
        console.warn(`🚨 하드웨어 에러 메시지 번역 실패: hardwareErrors.${key}`)
        return errorKey
      }

      return translatedMessage
    } catch (error) {
      console.error('🚨 에러 메시지 번역 중 오류:', error)
      return errorKey // 오류 시 원본 키 반환
    }
  }

  /**
   * HardwareErrorLog 객체에 다국어 메시지를 추가하는 함수
   * @param errorLog - 하드웨어 에러 로그 객체
   * @returns 메시지가 추가된 에러 로그 객체
   */
  const addLocalizedMessage = (errorLog: HardwareErrorLog): HardwareErrorLog => {
    const translatedMessage = translateHardwareError(errorLog.errorKey, errorLog.isResolved)

    return {
      ...errorLog,
      message: translatedMessage,
      resolvedMessage: errorLog.isResolved ? translatedMessage : undefined,
    }
  }

  // 안테나 데이터 전체 필드 추가
  const modeStatusBits = ref('')
  const azimuthAngle = ref('')
  const elevationAngle = ref('')
  const trainAngle = ref('')
  const azimuthSpeed = ref('')
  const elevationSpeed = ref('')
  const trainSpeed = ref('')
  const servoDriverAzimuthAngle = ref('')
  const servoDriverElevationAngle = ref('')
  const servoDriverTrainAngle = ref('')
  const torqueAzimuth = ref('')
  const torqueElevation = ref('')
  const torqueTrain = ref('')
  const windSpeed = ref('')
  const windDirection = ref('')
  const rtdOne = ref('')
  const rtdTwo = ref('')
  const mainBoardProtocolStatusBits = ref('')
  // mainBoardProtocolStatusBits 관련 개별 상태들
  const protocolElevationStatus = ref<boolean>(false)
  const protocolAzimuthStatus = ref<boolean>(false)
  const protocolTrainStatus = ref<boolean>(false)
  const protocolFeedStatus = ref<boolean>(false)
  const mainBoardProtocolStatusBitsReserve1 = ref<boolean>(false)
  const mainBoardProtocolStatusBitsReserve2 = ref<boolean>(false)
  const mainBoardProtocolStatusBitsReserve3 = ref<boolean>(false)
  const defaultReceiveStatus = ref<boolean>(false)

  const mainBoardStatusBits = ref('')
  // mainBoardStatusBits 관련 개별 상태들 (기존 코드 뒤에 추가)
  const powerSurgeProtector = ref<boolean>(false)
  const powerReversePhaseSensor = ref<boolean>(false)
  const emergencyStopACU = ref<boolean>(false)
  const emergencyStopPositioner = ref<boolean>(false)
  const mainBoardStatusBitsReserve1 = ref<boolean>(false)
  const mainBoardStatusBitsReserve2 = ref<boolean>(false)
  const mainBoardStatusBitsReserve3 = ref<boolean>(false)
  const mainBoardStatusBitsReserve4 = ref<boolean>(false)

  const mainBoardMCOnOffBits = ref('')
  // mainBoardMCOnOffBits 관련 개별 상태들 (기존 mainBoardStatusBits 관련 상태들 뒤에 추가)
  const mcTrain = ref<boolean>(false)
  const mcElevation = ref<boolean>(false)
  const mcAzimuth = ref<boolean>(false)
  const mainBoardMCOnOffBitsReserve1 = ref<boolean>(false)
  const mainBoardMCOnOffBitsReserve2 = ref<boolean>(false)
  const mainBoardMCOnOffBitsReserve3 = ref<boolean>(false)
  const mainBoardMCOnOffBitsReserve4 = ref<boolean>(false)
  const mainBoardMCOnOffBitsReserve5 = ref<boolean>(false)

  const mainBoardReserveBits = ref('')

  const azimuthBoardServoStatusBits = ref('')
  // azimuthBoardServoStatusBits 관련 개별 상태들 (기존 mainBoardMCOnOffBits 관련 상태들 뒤에 추가)
  const azimuthBoardServoStatusServoAlarmCode1 = ref<boolean>(false)
  const azimuthBoardServoStatusServoAlarmCode2 = ref<boolean>(false)
  const azimuthBoardServoStatusServoAlarmCode3 = ref<boolean>(false)
  const azimuthBoardServoStatusServoAlarmCode4 = ref<boolean>(false)
  const azimuthBoardServoStatusServoAlarmCode5 = ref<boolean>(false)
  const azimuthBoardServoStatusServoAlarm = ref<boolean>(false)
  const azimuthBoardServoStatusServoBrake = ref<boolean>(false)
  const azimuthBoardServoStatusServoMotor = ref<boolean>(false)

  const azimuthBoardStatusBits = ref('')
  // azimuthBoardStatusBits 관련 개별 상태들 (기존 azimuthBoardServoStatusBits 관련 상태들 뒤에 추가)
  const azimuthBoardStatusLimitSwitchPositive275 = ref<boolean>(false) // +275도 리미트 스위치
  const azimuthBoardStatusLimitSwitchNegative275 = ref<boolean>(false) // -275도 리미트 스위치
  const azimuthBoardStatusReserve1 = ref<boolean>(false)
  const azimuthBoardStatusReserve2 = ref<boolean>(false)
  const azimuthBoardStatusStowPin = ref<boolean>(false)
  const azimuthBoardStatusReserve3 = ref<boolean>(false)
  const azimuthBoardStatusReserve4 = ref<boolean>(false)
  const azimuthBoardStatusEncoder = ref<boolean>(false)

  const elevationBoardServoStatusBits = ref('')
  // elevationBoardServoStatusBits 관련 개별 상태들 (기존 azimuthBoardStatusBits 관련 상태들 뒤에 추가)
  const elevationBoardServoStatusServoAlarmCode1 = ref<boolean>(false)
  const elevationBoardServoStatusServoAlarmCode2 = ref<boolean>(false)
  const elevationBoardServoStatusServoAlarmCode3 = ref<boolean>(false)
  const elevationBoardServoStatusServoAlarmCode4 = ref<boolean>(false)
  const elevationBoardServoStatusServoAlarmCode5 = ref<boolean>(false)
  const elevationBoardServoStatusServoAlarm = ref<boolean>(false)
  const elevationBoardServoStatusServoBrake = ref<boolean>(false)
  const elevationBoardServoStatusServoMotor = ref<boolean>(false)

  const elevationBoardStatusBits = ref('')
  // elevationBoardStatusBits 관련 개별 상태들 (기존 elevationBoardServoStatusBits 관련 상태들 뒤에 추가)
  const elevationBoardStatusLimitSwitchPositive180 = ref<boolean>(false) // +180도 리미트 스위치
  const elevationBoardStatusLimitSwitchPositive185 = ref<boolean>(false) // +95도 리미트 스위치
  const elevationBoardStatusLimitSwitchNegative0 = ref<boolean>(false) // -90도 리미트 스위치
  const elevationBoardStatusLimitSwitchNegative5 = ref<boolean>(false) // -95도 리미트 스위치
  const elevationBoardStatusStowPin = ref<boolean>(false)
  const elevationBoardStatusReserve1 = ref<boolean>(false)
  const elevationBoardStatusReserve2 = ref<boolean>(false)
  const elevationBoardStatusEncoder = ref<boolean>(false)

  const trainBoardServoStatusBits = ref('')
  // trainBoardServoStatusBits 관련 개별 상태들 (기존 elevationBoardStatusBits 관련 상태들 뒤에 추가)
  const trainBoardServoStatusServoAlarmCode1 = ref<boolean>(false)
  const trainBoardServoStatusServoAlarmCode2 = ref<boolean>(false)
  const trainBoardServoStatusServoAlarmCode3 = ref<boolean>(false)
  const trainBoardServoStatusServoAlarmCode4 = ref<boolean>(false)
  const trainBoardServoStatusServoAlarmCode5 = ref<boolean>(false)
  const trainBoardServoStatusServoAlarm = ref<boolean>(false)
  const trainBoardServoStatusServoBrake = ref<boolean>(false)
  const trainBoardServoStatusServoMotor = ref<boolean>(false)

  const trainBoardStatusBits = ref('')
  // trainBoardStatusBits 관련 개별 상태들 (기존 trainBoardServoStatusBits 관련 상태들 뒤에 추가)
  const trainBoardStatusLimitSwitchPositive275 = ref<boolean>(false) // +275도 리미트 스위치
  const trainBoardStatusLimitSwitchNegative275 = ref<boolean>(false) // -275도 리미트 스위치
  const trainBoardStatusReserve1 = ref<boolean>(false)
  const trainBoardStatusReserve2 = ref<boolean>(false)
  const trainBoardStatusStowPin = ref<boolean>(false)
  const trainBoardStatusReserve3 = ref<boolean>(false)
  const trainBoardStatusReserve4 = ref<boolean>(false)
  const trainBoardStatusEncoder = ref<boolean>(false)

  const feedSBoardStatusBits = ref('')
  // feedSBoardStatusBits 관련 개별 상태들 (기존 trainBoardStatusBits 관련 상태들 뒤에 추가)
  const feedSBoardStatusLNALHCPPower = ref<boolean>(false) // S-Band LNA LHCP ON/OFF (1=ON, 0=OFF)
  const feedSBoardStatusLNALHCPError = ref<boolean>(false) // S-Band LNA LHCP Error/Normal (1=Error, 0=Normal)
  const feedSBoardStatusLNARHCPPower = ref<boolean>(false) // S-Band LNA RHCP ON/OFF (1=ON, 0=OFF)
  const feedSBoardStatusLNARHCPError = ref<boolean>(false) // S-Band LNA RHCP Error/Normal (1=Error, 0=Normal)
  const feedSBoardStatusRFSwitchMode = ref<boolean>(false) // S-Band RF Switch RHCP/LHCP (1=RHCP, 0=LHCP)
  const feedSBoardStatusRFSwitchError = ref<boolean>(false) // S-Band RF Switch Error/Normal (1=Error, 0=Normal)
  const feedSBoardStatusBitsReserve1 = ref<boolean>(false)
  const feedSBoardStatusBitsReserve2 = ref<boolean>(false)

  const feedXBoardStatusBits = ref('')
  // feedXBoardStatusBits 관련 개별 상태들 (ICD 문서: Bits 23-16, 실제 사용: Bit 16-19)
  const feedXBoardStatusLNALHCPPower = ref<boolean>(false) // X-Band LNA LHCP ON/OFF (Bit 16: 1=ON, 0=OFF)
  const feedXBoardStatusLNALHCPError = ref<boolean>(false) // X-Band LNA LHCP Error/Normal (Bit 17: 1=Error, 0=Normal)
  const feedXBoardStatusLNARHCPPower = ref<boolean>(false) // X-Band LNA RHCP ON/OFF (Bit 18: 1=ON, 0=OFF)
  const feedXBoardStatusLNARHCPError = ref<boolean>(false) // X-Band LNA RHCP Error/Normal (Bit 19: 1=Error, 0=Normal)
  // Bit 20-23: Reserved

  const feedBoardETCStatusBits = ref('')
  // feedBoardETCStatusBits 관련 개별 상태들 (ICD 문서: Bits 7-0)
  const feedBoardETCStatusRFSwitchMode = ref<boolean>(false) // S-Band TX RF Switch Mode (Bit 0: 0=RHCP, 1=LHCP)
  const feedBoardETCStatusRFSwitchError = ref<boolean>(false) // S-Band TX RF Switch Error/Normal (Bit 1: 1=Error, 0=Normal)
  const feedBoardETCStatusFanPower = ref<boolean>(false) // Fan Power ON/OFF (Bit 2: 1=ON, 0=OFF)
  const feedBoardETCStatusFanError = ref<boolean>(false) // Fan Error/Normal (Bit 3: 1=Error, 0=Normal)
  // Bit 4-7: Reserved

  const feedKaBoardStatusBits = ref('')
  // feedKaBoardStatusBits 관련 개별 상태들 (ICD 문서: Bits 31-24)
  const feedKaBoardStatusLNALHCPPower = ref<boolean>(false) // Ka-Band RX LNA LHCP ON/OFF (Bit 24: 1=ON, 0=OFF)
  const feedKaBoardStatusLNALHCPError = ref<boolean>(false) // Ka-Band RX LNA LHCP Error/Normal (Bit 25: 1=Error, 0=Normal)
  const feedKaBoardStatusLNARHCPPower = ref<boolean>(false) // Ka-Band RX LNA RHCP ON/OFF (Bit 26: 1=ON, 0=OFF)
  const feedKaBoardStatusLNARHCPError = ref<boolean>(false) // Ka-Band RX LNA RHCP Error/Normal (Bit 27: 1=Error, 0=Normal)
  const feedKaBoardStatusSelectionLHCPBand = ref<boolean>(false) // Ka-Band Selection LHCP Band (Bit 28: 0=Band1, 1=Band2)
  const feedKaBoardStatusSelectionLHCPError = ref<boolean>(false) // Ka-Band Selection LHCP Error/Normal (Bit 29: 1=Error, 0=Normal)
  const feedKaBoardStatusSelectionRHCPBand = ref<boolean>(false) // Ka-Band Selection RHCP Band (Bit 30: 0=Band1, 1=Band2)
  const feedKaBoardStatusSelectionRHCPError = ref<boolean>(false) // Ka-Band Selection RHCP Error/Normal (Bit 31: 1=Error, 0=Normal)

  const currentSBandLNALHCP = ref('')
  const currentSBandLNARHCP = ref('')
  const currentXBandLNALHCP = ref('')
  const currentXBandLNARHCP = ref('')
  const currentKaBandLNALHCP = ref('')
  const currentKaBandLNARHCP = ref('')
  const rssiSBandLNALHCP = ref('')
  const rssiSBandLNARHCP = ref('')
  const rssiXBandLNALHCP = ref('')
  const rssiXBandLNARHCP = ref('')
  const rssiKaBandLNALHCP = ref('')
  const rssiKaBandLNARHCP = ref('')
  const azimuthAcceleration = ref('')
  const elevationAcceleration = ref('')
  const trainAcceleration = ref('')
  const azimuthMaxAcceleration = ref('')
  const elevationMaxAcceleration = ref('')
  const trainMaxAcceleration = ref('')
  const trackingAzimuthTime = ref('')
  const trackingCMDAzimuthAngle = ref('')
  const trackingActualAzimuthAngle = ref('')
  const trackingElevationTime = ref('')
  const trackingCMDElevationAngle = ref('')
  const trackingActualElevationAngle = ref('')
  const trackingTrainTime = ref('')
  const trackingCMDTrainAngle = ref('')
  const trackingActualTrainAngle = ref('')
  // 96-98번째 줄 근처 - Boolean 타입으로 변경
  const ephemerisStatus = ref<boolean | null>(null)
  const ephemerisTrackingState = ref<string | null>(null) // ✅ 추가
  const passScheduleStatus = ref<boolean | null>(null)
  const passScheduleTrackingState = ref<string | null>(null) // ✅ 추가 (패스 스케줄 상세 상태)
  const sunTrackStatus = ref<boolean | null>(null)
  const sunTrackTrackingState = ref<string | null>(null) // ✅ 추가
  const communicationStatus = ref('')
  const currentTrackingMstId = ref<number | null>(null)
  const currentTrackingDetailId = ref<number | null>(null) // ✅ detailId 추가
  const nextTrackingMstId = ref<number | null>(null)
  const nextTrackingDetailId = ref<number | null>(null) // ✅ detailId 추가
  const udpConnected = ref<boolean>(false)
  const lastUdpUpdateTime = ref<string>('')

  // 에러 데이터 상태 (shallowRef: 전체 객체 교체로 업데이트)
  const errorStatusBarData = shallowRef<{
    activeErrorCount: number
    latestError: {
      id: string
      timestamp: string
      category: string
      severity: string
      errorKey: string
      message: string
      component: string
      isResolved: boolean
      resolvedAt: string | null
      resolvedMessage: string | null
    } | null
    hasNewErrors: boolean
  } | null>(null)
  const errorPopupData = shallowRef<{
    isInitialLoad: boolean
    newLogs: {
      id: string
      timestamp: string
      category: string
      severity: string
      errorKey: string
      message: string
      component: string
      isResolved: boolean
      resolvedAt: string | null
      resolvedMessage: string | null
    }[]
    totalLogCount: number
    lastUpdateTime: number
  } | null>(null)
  const clientId = ref<string>('')

  // 클라이언트 ID 생성
  const generateClientId = () => {
    if (!clientId.value) {
      clientId.value = `client-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    }
    return clientId.value
  }

  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (순수 함수 사용)
  const parseProtocolStatusBits = (bitString: string) => {
    const parsed = parseProtocolBits(bitString)
    protocolElevationStatus.value = parsed.elevation
    protocolAzimuthStatus.value = parsed.azimuth
    protocolTrainStatus.value = parsed.train
    protocolFeedStatus.value = parsed.feed
    mainBoardProtocolStatusBitsReserve1.value = parsed.reserve1
    mainBoardProtocolStatusBitsReserve2.value = parsed.reserve2
    mainBoardProtocolStatusBitsReserve3.value = parsed.reserve3
    defaultReceiveStatus.value = parsed.defaultReceive
  }
  // 메인보드 상태 비트 파싱 (순수 함수 사용)
  const parseMainBoardStatusBits = (bitString: string) => {
    const parsed = parseMainBoardBits(bitString)
    powerSurgeProtector.value = parsed.powerSurgeProtector
    powerReversePhaseSensor.value = parsed.powerReversePhaseSensor
    emergencyStopACU.value = parsed.emergencyStopACU
    emergencyStopPositioner.value = parsed.emergencyStopPositioner
    mainBoardStatusBitsReserve1.value = parsed.reserve1
    mainBoardStatusBitsReserve2.value = parsed.reserve2
    mainBoardStatusBitsReserve3.value = parsed.reserve3
    mainBoardStatusBitsReserve4.value = parsed.reserve4
  }

  // MC On/Off 비트 파싱 (순수 함수 사용)
  const parseMainBoardMCOnOffBits = (bitString: string) => {
    const parsed = parseMCOnOffBits(bitString)
    mcTrain.value = parsed.mcTrain
    mcElevation.value = parsed.mcElevation
    mcAzimuth.value = parsed.mcAzimuth
    mainBoardMCOnOffBitsReserve1.value = parsed.reserve1
    mainBoardMCOnOffBitsReserve2.value = parsed.reserve2
    mainBoardMCOnOffBitsReserve3.value = parsed.reserve3
    mainBoardMCOnOffBitsReserve4.value = parsed.reserve4
    mainBoardMCOnOffBitsReserve5.value = parsed.reserve5
  }

  // Azimuth 서보 상태 비트 파싱 (순수 함수 사용)
  const parseAzimuthBoardServoStatusBits = (bitString: string) => {
    const parsed = parseServoStatusBits(bitString)
    azimuthBoardServoStatusServoAlarmCode1.value = parsed.servoAlarmCode1
    azimuthBoardServoStatusServoAlarmCode2.value = parsed.servoAlarmCode2
    azimuthBoardServoStatusServoAlarmCode3.value = parsed.servoAlarmCode3
    azimuthBoardServoStatusServoAlarmCode4.value = parsed.servoAlarmCode4
    azimuthBoardServoStatusServoAlarmCode5.value = parsed.servoAlarmCode5
    azimuthBoardServoStatusServoAlarm.value = parsed.servoAlarm
    azimuthBoardServoStatusServoBrake.value = parsed.servoBrake
    azimuthBoardServoStatusServoMotor.value = parsed.servoMotor
  }

  // Azimuth 보드 상태 비트 파싱 (순수 함수 사용)
  const parseAzimuthBoardStatusBits = (bitString: string) => {
    const parsed = parseAzimuthBits(bitString)
    azimuthBoardStatusLimitSwitchPositive275.value = parsed.limitSwitchPositive275
    azimuthBoardStatusLimitSwitchNegative275.value = parsed.limitSwitchNegative275
    azimuthBoardStatusReserve1.value = parsed.reserve1
    azimuthBoardStatusReserve2.value = parsed.reserve2
    azimuthBoardStatusStowPin.value = parsed.stowPin
    azimuthBoardStatusReserve3.value = parsed.reserve3
    azimuthBoardStatusReserve4.value = parsed.reserve4
    azimuthBoardStatusEncoder.value = parsed.encoder
  }

  // Elevation 서보 상태 비트 파싱 (순수 함수 사용)
  const parseElevationBoardServoStatusBits = (bitString: string) => {
    const parsed = parseServoStatusBits(bitString)
    elevationBoardServoStatusServoAlarmCode1.value = parsed.servoAlarmCode1
    elevationBoardServoStatusServoAlarmCode2.value = parsed.servoAlarmCode2
    elevationBoardServoStatusServoAlarmCode3.value = parsed.servoAlarmCode3
    elevationBoardServoStatusServoAlarmCode4.value = parsed.servoAlarmCode4
    elevationBoardServoStatusServoAlarmCode5.value = parsed.servoAlarmCode5
    elevationBoardServoStatusServoAlarm.value = parsed.servoAlarm
    elevationBoardServoStatusServoBrake.value = parsed.servoBrake
    elevationBoardServoStatusServoMotor.value = parsed.servoMotor
  }

  // Elevation 보드 상태 비트 파싱 (순수 함수 사용)
  const parseElevationBoardStatusBits = (bitString: string) => {
    const parsed = parseElevationBits(bitString)
    elevationBoardStatusLimitSwitchPositive180.value = parsed.limitSwitchPositive180
    elevationBoardStatusLimitSwitchPositive185.value = parsed.limitSwitchPositive185
    elevationBoardStatusLimitSwitchNegative0.value = parsed.limitSwitchNegative0
    elevationBoardStatusLimitSwitchNegative5.value = parsed.limitSwitchNegative5
    elevationBoardStatusStowPin.value = parsed.stowPin
    elevationBoardStatusReserve1.value = parsed.reserve1
    elevationBoardStatusReserve2.value = parsed.reserve2
    elevationBoardStatusEncoder.value = parsed.encoder
  }

  // Train 서보 상태 비트 파싱 (순수 함수 사용)
  const parseTrainBoardServoStatusBits = (bitString: string) => {
    const parsed = parseServoStatusBits(bitString)
    trainBoardServoStatusServoAlarmCode1.value = parsed.servoAlarmCode1
    trainBoardServoStatusServoAlarmCode2.value = parsed.servoAlarmCode2
    trainBoardServoStatusServoAlarmCode3.value = parsed.servoAlarmCode3
    trainBoardServoStatusServoAlarmCode4.value = parsed.servoAlarmCode4
    trainBoardServoStatusServoAlarmCode5.value = parsed.servoAlarmCode5
    trainBoardServoStatusServoAlarm.value = parsed.servoAlarm
    trainBoardServoStatusServoBrake.value = parsed.servoBrake
    trainBoardServoStatusServoMotor.value = parsed.servoMotor
  }

  // Train 보드 상태 비트 파싱 (순수 함수 사용)
  const parseTrainBoardStatusBits = (bitString: string) => {
    const parsed = parseTrainBits(bitString)
    trainBoardStatusLimitSwitchPositive275.value = parsed.limitSwitchPositive275
    trainBoardStatusLimitSwitchNegative275.value = parsed.limitSwitchNegative275
    trainBoardStatusReserve1.value = parsed.reserve1
    trainBoardStatusReserve2.value = parsed.reserve2
    trainBoardStatusStowPin.value = parsed.stowPin
    trainBoardStatusReserve3.value = parsed.reserve3
    trainBoardStatusReserve4.value = parsed.reserve4
    trainBoardStatusEncoder.value = parsed.encoder
  }
  /**
   * S-Band Status Bits 파싱 (순수 함수 사용)
   * ICD 문서: Bits 15-8
   */
  const parseFeedSBoardStatusBits = (bitString: string) => {
    const parsed = parseFeedSBits(bitString)
    feedSBoardStatusLNALHCPPower.value = parsed.lnaLHCPPower
    feedSBoardStatusLNALHCPError.value = parsed.lnaLHCPError
    feedSBoardStatusLNARHCPPower.value = parsed.lnaRHCPPower
    feedSBoardStatusLNARHCPError.value = parsed.lnaRHCPError
    feedSBoardStatusBitsReserve1.value = parsed.reserve1
    feedSBoardStatusBitsReserve2.value = parsed.reserve2
    feedSBoardStatusRFSwitchMode.value = parsed.rfSwitchMode
    feedSBoardStatusRFSwitchError.value = parsed.rfSwitchError
  }

  /**
   * X-Band Status Bits 파싱 (순수 함수 사용)
   * ICD 문서: Bits 23-16
   */
  const parseFeedXBoardStatusBits = (bitString: string) => {
    const parsed = parseFeedXBits(bitString)
    feedXBoardStatusLNALHCPPower.value = parsed.lnaLHCPPower
    feedXBoardStatusLNALHCPError.value = parsed.lnaLHCPError
    feedXBoardStatusLNARHCPPower.value = parsed.lnaRHCPPower
    feedXBoardStatusLNARHCPError.value = parsed.lnaRHCPError
    feedBoardETCStatusFanPower.value = parsed.fanPower
    feedBoardETCStatusFanError.value = parsed.fanError
  }

  /**
   * ETC Status Bits 파싱 (순수 함수 사용)
   * ICD 문서: Bits 7-0
   */
  const parseFeedBoardETCStatusBits = (bitString: string) => {
    const parsed = parseFeedETCBits(bitString)
    feedBoardETCStatusRFSwitchMode.value = parsed.rfSwitchMode
    feedBoardETCStatusRFSwitchError.value = parsed.rfSwitchError
    feedBoardETCStatusFanPower.value = parsed.fanPower
    feedBoardETCStatusFanError.value = parsed.fanError
  }

  /**
   * Ka-Band Status Bits 파싱 (순수 함수 사용)
   * ICD 문서: Bits 31-24
   */
  const parseFeedKaBoardStatusBits = (bitString: string) => {
    const parsed = parseFeedKaBits(bitString)
    feedKaBoardStatusLNALHCPPower.value = parsed.lnaLHCPPower
    feedKaBoardStatusLNALHCPError.value = parsed.lnaLHCPError
    feedKaBoardStatusLNARHCPPower.value = parsed.lnaRHCPPower
    feedKaBoardStatusLNARHCPError.value = parsed.lnaRHCPError
    feedKaBoardStatusSelectionLHCPBand.value = parsed.selectionLHCPBand
    feedKaBoardStatusSelectionLHCPError.value = parsed.selectionLHCPError
    feedKaBoardStatusSelectionRHCPBand.value = parsed.selectionRHCPBand
    feedKaBoardStatusSelectionRHCPError.value = parsed.selectionRHCPError
  }

  // 전체 프로토콜 상태 정보를 제공하는 computed
  const protocolStatusInfo = computed(() => ({
    raw: mainBoardProtocolStatusBits.value,
    elevation: protocolElevationStatus.value,
    azimuth: protocolAzimuthStatus.value,
    train: protocolTrainStatus.value,
    feed: protocolFeedStatus.value,
    reserve1: mainBoardProtocolStatusBitsReserve1.value,
    reserve2: mainBoardProtocolStatusBitsReserve2.value,
    reserve3: mainBoardProtocolStatusBitsReserve3.value,
    defaultReceive: defaultReceiveStatus.value,
    // 활성화된 프로토콜 목록
    activeProtocols: [
      protocolElevationStatus.value && 'elevation',
      protocolAzimuthStatus.value && 'azimuth',
      protocolTrainStatus.value && 'train',
      protocolFeedStatus.value && 'feed',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActive: [
        protocolElevationStatus.value,
        protocolAzimuthStatus.value,
        protocolTrainStatus.value,
        protocolFeedStatus.value,
      ].filter(Boolean).length,
      hasAnyActive:
        protocolElevationStatus.value ||
        protocolAzimuthStatus.value ||
        protocolTrainStatus.value ||
        protocolFeedStatus.value,
    },
  }))

  // 전체 메인보드 상태 정보를 제공하는 computed (기존 protocolStatusInfo computed 뒤에 추가)
  const mainBoardStatusInfo = computed(() => ({
    raw: mainBoardStatusBits.value,
    powerSurgeProtector: powerSurgeProtector.value,
    powerReversePhaseSensor: powerReversePhaseSensor.value,
    emergencyStopACU: emergencyStopACU.value,
    emergencyStopPositioner: emergencyStopPositioner.value,
    reserve1: mainBoardStatusBitsReserve1.value,
    reserve2: mainBoardStatusBitsReserve2.value,
    reserve3: mainBoardStatusBitsReserve3.value,
    reserve4: mainBoardStatusBitsReserve4.value,
    // 활성화된 상태 목록
    activeStatuses: [
      powerSurgeProtector.value && 'powerSurgeProtector',
      powerReversePhaseSensor.value && 'powerReversePhaseSensor',
      emergencyStopACU.value && 'emergencyStopACU',
      emergencyStopPositioner.value && 'emergencyStopPositioner',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActive: [
        powerSurgeProtector.value,
        powerReversePhaseSensor.value,
        emergencyStopACU.value,
        emergencyStopPositioner.value,
      ].filter(Boolean).length,
      hasAnyActive:
        powerSurgeProtector.value ||
        powerReversePhaseSensor.value ||
        emergencyStopACU.value ||
        emergencyStopPositioner.value,
      hasEmergencyStop: emergencyStopACU.value || emergencyStopPositioner.value,
      hasPowerIssue: powerSurgeProtector.value || powerReversePhaseSensor.value,
    },
  }))
  // 전체 MC On/Off 상태 정보를 제공하는 computed (기존 mainBoardStatusInfo computed 뒤에 추가)
  const mainBoardMCOnOffInfo = computed(() => ({
    raw: mainBoardMCOnOffBits.value,
    mcTrain: mcTrain.value,
    mcElevation: mcElevation.value,
    mcAzimuth: mcAzimuth.value,
    reserve1: mainBoardMCOnOffBitsReserve1.value,
    reserve2: mainBoardMCOnOffBitsReserve2.value,
    reserve3: mainBoardMCOnOffBitsReserve3.value,
    reserve4: mainBoardMCOnOffBitsReserve4.value,
    reserve5: mainBoardMCOnOffBitsReserve5.value,
    // 활성화된 MC 목록
    activeMCs: [
      mcTrain.value && 'train',
      mcElevation.value && 'elevation',
      mcAzimuth.value && 'azimuth',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActive: [mcTrain.value, mcElevation.value, mcAzimuth.value].filter(Boolean).length,
      hasAnyActive: mcTrain.value || mcElevation.value || mcAzimuth.value,
      allAxesActive: mcTrain.value && mcElevation.value && mcAzimuth.value,
      // 축별 상태
      axesStatus: {
        train: mcTrain.value ? 'ON' : 'OFF',
        elevation: mcElevation.value ? 'ON' : 'OFF',
        azimuth: mcAzimuth.value ? 'ON' : 'OFF',
      },
    },
  }))
  // 전체 Azimuth Board Servo 상태 정보를 제공하는 computed (기존 mainBoardMCOnOffInfo computed 뒤에 추가)
  const azimuthBoardServoStatusInfo = computed(() => ({
    raw: azimuthBoardServoStatusBits.value,
    servoAlarmCode1: azimuthBoardServoStatusServoAlarmCode1.value,
    servoAlarmCode2: azimuthBoardServoStatusServoAlarmCode2.value,
    servoAlarmCode3: azimuthBoardServoStatusServoAlarmCode3.value,
    servoAlarmCode4: azimuthBoardServoStatusServoAlarmCode4.value,
    servoAlarmCode5: azimuthBoardServoStatusServoAlarmCode5.value,
    servoAlarm: azimuthBoardServoStatusServoAlarm.value,
    servoBrake: azimuthBoardServoStatusServoBrake.value,
    servoMotor: azimuthBoardServoStatusServoMotor.value,
    // 활성화된 알람 코드 목록
    activeAlarmCodes: [
      azimuthBoardServoStatusServoAlarmCode1.value && 'AlarmCode1',
      azimuthBoardServoStatusServoAlarmCode2.value && 'AlarmCode2',
      azimuthBoardServoStatusServoAlarmCode3.value && 'AlarmCode3',
      azimuthBoardServoStatusServoAlarmCode4.value && 'AlarmCode4',
      azimuthBoardServoStatusServoAlarmCode5.value && 'AlarmCode5',
    ].filter(Boolean),
    // 활성화된 서보 상태 목록
    activeServoStatuses: [
      azimuthBoardServoStatusServoAlarm.value && 'Alarm',
      azimuthBoardServoStatusServoBrake.value && 'Brake',
      azimuthBoardServoStatusServoMotor.value && 'Motor',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalAlarmCodes: [
        azimuthBoardServoStatusServoAlarmCode1.value,
        azimuthBoardServoStatusServoAlarmCode2.value,
        azimuthBoardServoStatusServoAlarmCode3.value,
        azimuthBoardServoStatusServoAlarmCode4.value,
        azimuthBoardServoStatusServoAlarmCode5.value,
      ].filter(Boolean).length,
      hasAnyAlarmCode:
        azimuthBoardServoStatusServoAlarmCode1.value ||
        azimuthBoardServoStatusServoAlarmCode2.value ||
        azimuthBoardServoStatusServoAlarmCode3.value ||
        azimuthBoardServoStatusServoAlarmCode4.value ||
        azimuthBoardServoStatusServoAlarmCode5.value,
      hasServoAlarm: azimuthBoardServoStatusServoAlarm.value,
      isBrakeActive: azimuthBoardServoStatusServoBrake.value,
      isMotorActive: azimuthBoardServoStatusServoMotor.value,
      // 서보 상태
      servoStatus: {
        alarm: azimuthBoardServoStatusServoAlarm.value ? 'ALARM' : 'NORMAL',
        brake: azimuthBoardServoStatusServoBrake.value ? 'ON' : 'OFF',
        motor: azimuthBoardServoStatusServoMotor.value ? 'ON' : 'OFF',
      },
      // 전체 상태 판단
      overallStatus: azimuthBoardServoStatusServoAlarm.value
        ? 'ALARM'
        : azimuthBoardServoStatusServoMotor.value
          ? 'ACTIVE'
          : 'STANDBY',
    },
  }))

  // 전체 Azimuth Board 상태 정보를 제공하는 computed (기존 azimuthBoardServoStatusInfo computed 뒤에 추가)
  const azimuthBoardStatusInfo = computed(() => ({
    raw: azimuthBoardStatusBits.value,
    limitSwitchPositive275: azimuthBoardStatusLimitSwitchPositive275.value,
    limitSwitchNegative275: azimuthBoardStatusLimitSwitchNegative275.value,
    reserve1: azimuthBoardStatusReserve1.value,
    reserve2: azimuthBoardStatusReserve2.value,
    stowPin: azimuthBoardStatusStowPin.value,
    reserve3: azimuthBoardStatusReserve3.value,
    reserve4: azimuthBoardStatusReserve4.value,
    encoder: azimuthBoardStatusEncoder.value,
    // 활성화된 리미트 스위치 목록
    activeLimitSwitches: [
      azimuthBoardStatusLimitSwitchPositive275.value && '+275°',
      azimuthBoardStatusLimitSwitchNegative275.value && '-275°',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      azimuthBoardStatusLimitSwitchPositive275.value && 'LimitSwitch+275',
      azimuthBoardStatusLimitSwitchNegative275.value && 'LimitSwitch-275',
      azimuthBoardStatusStowPin.value && 'StowPin',
      azimuthBoardStatusEncoder.value && 'Encoder',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      hasLimitSwitchActive:
        azimuthBoardStatusLimitSwitchPositive275.value ||
        azimuthBoardStatusLimitSwitchNegative275.value,
      isStowPinActive: azimuthBoardStatusStowPin.value,
      isEncoderActive: azimuthBoardStatusEncoder.value,
      totalActiveStatuses: [
        azimuthBoardStatusLimitSwitchPositive275.value,
        azimuthBoardStatusLimitSwitchNegative275.value,
        azimuthBoardStatusStowPin.value,
        azimuthBoardStatusEncoder.value,
      ].filter(Boolean).length,
      // 리미트 스위치 상태
      limitSwitchStatus: {
        positive275: azimuthBoardStatusLimitSwitchPositive275.value ? 'ACTIVE' : 'NORMAL',
        negative275: azimuthBoardStatusLimitSwitchNegative275.value ? 'ACTIVE' : 'NORMAL',
        anyActive:
          azimuthBoardStatusLimitSwitchPositive275.value ||
          azimuthBoardStatusLimitSwitchNegative275.value,
      },
      // 전체 상태 판단
      overallStatus:
        azimuthBoardStatusLimitSwitchPositive275.value ||
        azimuthBoardStatusLimitSwitchNegative275.value
          ? 'LIMIT_REACHED'
          : azimuthBoardStatusStowPin.value
            ? 'STOWED'
            : azimuthBoardStatusEncoder.value
              ? 'ENCODER_ACTIVE'
              : 'NORMAL',
    },
  }))

  // 전체 Elevation Board Servo 상태 정보를 제공하는 computed (기존 azimuthBoardStatusInfo computed 뒤에 추가)
  const elevationBoardServoStatusInfo = computed(() => ({
    raw: elevationBoardServoStatusBits.value,
    servoAlarmCode1: elevationBoardServoStatusServoAlarmCode1.value,
    servoAlarmCode2: elevationBoardServoStatusServoAlarmCode2.value,
    servoAlarmCode3: elevationBoardServoStatusServoAlarmCode3.value,
    servoAlarmCode4: elevationBoardServoStatusServoAlarmCode4.value,
    servoAlarmCode5: elevationBoardServoStatusServoAlarmCode5.value,
    servoAlarm: elevationBoardServoStatusServoAlarm.value,
    servoBrake: elevationBoardServoStatusServoBrake.value,
    servoMotor: elevationBoardServoStatusServoMotor.value,
    // 활성화된 알람 코드 목록
    activeAlarmCodes: [
      elevationBoardServoStatusServoAlarmCode1.value && 'AlarmCode1',
      elevationBoardServoStatusServoAlarmCode2.value && 'AlarmCode2',
      elevationBoardServoStatusServoAlarmCode3.value && 'AlarmCode3',
      elevationBoardServoStatusServoAlarmCode4.value && 'AlarmCode4',
      elevationBoardServoStatusServoAlarmCode5.value && 'AlarmCode5',
    ].filter(Boolean),
    // 활성화된 서보 상태 목록
    activeServoStatuses: [
      elevationBoardServoStatusServoAlarm.value && 'Alarm',
      elevationBoardServoStatusServoBrake.value && 'Brake',
      elevationBoardServoStatusServoMotor.value && 'Motor',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalAlarmCodes: [
        elevationBoardServoStatusServoAlarmCode1.value,
        elevationBoardServoStatusServoAlarmCode2.value,
        elevationBoardServoStatusServoAlarmCode3.value,
        elevationBoardServoStatusServoAlarmCode4.value,
        elevationBoardServoStatusServoAlarmCode5.value,
      ].filter(Boolean).length,
      hasAnyAlarmCode:
        elevationBoardServoStatusServoAlarmCode1.value ||
        elevationBoardServoStatusServoAlarmCode2.value ||
        elevationBoardServoStatusServoAlarmCode3.value ||
        elevationBoardServoStatusServoAlarmCode4.value ||
        elevationBoardServoStatusServoAlarmCode5.value,
      hasServoAlarm: elevationBoardServoStatusServoAlarm.value,
      isBrakeActive: elevationBoardServoStatusServoBrake.value,
      isMotorActive: elevationBoardServoStatusServoMotor.value,
      // 서보 상태
      servoStatus: {
        alarm: elevationBoardServoStatusServoAlarm.value ? 'ALARM' : 'NORMAL',
        brake: elevationBoardServoStatusServoBrake.value ? 'ON' : 'OFF',
        motor: elevationBoardServoStatusServoMotor.value ? 'ON' : 'OFF',
      },
      // 전체 상태 판단
      overallStatus: elevationBoardServoStatusServoAlarm.value
        ? 'ALARM'
        : elevationBoardServoStatusServoMotor.value
          ? 'ACTIVE'
          : 'STANDBY',
    },
  }))
  // 전체 Elevation Board 상태 정보를 제공하는 computed (기존 elevationBoardServoStatusInfo computed 뒤에 추가)
  const elevationBoardStatusInfo = computed(() => ({
    raw: elevationBoardStatusBits.value,
    limitSwitchPositive180: elevationBoardStatusLimitSwitchPositive180.value,
    limitSwitchPositive185: elevationBoardStatusLimitSwitchPositive185.value,
    limitSwitchNegative0: elevationBoardStatusLimitSwitchNegative0.value,
    limitSwitchNegative5: elevationBoardStatusLimitSwitchNegative5.value,
    stowPin: elevationBoardStatusStowPin.value,
    reserve1: elevationBoardStatusReserve1.value,
    reserve2: elevationBoardStatusReserve2.value,
    encoder: elevationBoardStatusEncoder.value,
    // 활성화된 리미트 스위치 목록
    activeLimitSwitches: [
      elevationBoardStatusLimitSwitchPositive180.value && '+180°',
      elevationBoardStatusLimitSwitchPositive185.value && '+185',
      elevationBoardStatusLimitSwitchNegative0.value && '-0°',
      elevationBoardStatusLimitSwitchNegative5.value && '-5°',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      elevationBoardStatusLimitSwitchPositive180.value && 'LimitSwitch+180',
      elevationBoardStatusLimitSwitchPositive185.value && 'LimitSwitch+185',
      elevationBoardStatusLimitSwitchNegative0.value && 'LimitSwitch-0',
      elevationBoardStatusLimitSwitchNegative5.value && 'LimitSwitch-5',
      elevationBoardStatusStowPin.value && 'StowPin',
      elevationBoardStatusEncoder.value && 'Encoder',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      hasLimitSwitchActive:
        elevationBoardStatusLimitSwitchPositive180.value ||
        elevationBoardStatusLimitSwitchPositive185.value ||
        elevationBoardStatusLimitSwitchNegative0.value ||
        elevationBoardStatusLimitSwitchNegative5.value,
      hasPositiveLimitActive:
        elevationBoardStatusLimitSwitchPositive180.value ||
        elevationBoardStatusLimitSwitchPositive185.value,
      hasNegativeLimitActive:
        elevationBoardStatusLimitSwitchNegative0.value ||
        elevationBoardStatusLimitSwitchNegative5.value,
      isStowPinActive: elevationBoardStatusStowPin.value,
      isEncoderActive: elevationBoardStatusEncoder.value,
      totalActiveStatuses: [
        elevationBoardStatusLimitSwitchPositive180.value,
        elevationBoardStatusLimitSwitchPositive185.value,
        elevationBoardStatusLimitSwitchNegative0.value,
        elevationBoardStatusLimitSwitchNegative5.value,
        elevationBoardStatusStowPin.value,
        elevationBoardStatusEncoder.value,
      ].filter(Boolean).length,
      // 리미트 스위치 상태 세부 정보
      limitSwitchStatus: {
        positive90: elevationBoardStatusLimitSwitchPositive180.value ? 'ACTIVE' : 'NORMAL',
        positive95: elevationBoardStatusLimitSwitchPositive185.value ? 'ACTIVE' : 'NORMAL',
        negative90: elevationBoardStatusLimitSwitchNegative0.value ? 'ACTIVE' : 'NORMAL',
        negative95: elevationBoardStatusLimitSwitchNegative5.value ? 'ACTIVE' : 'NORMAL',
        anyActive:
          elevationBoardStatusLimitSwitchPositive180.value ||
          elevationBoardStatusLimitSwitchPositive185.value ||
          elevationBoardStatusLimitSwitchNegative0.value ||
          elevationBoardStatusLimitSwitchNegative5.value,
        positiveDirection:
          elevationBoardStatusLimitSwitchPositive180.value ||
          elevationBoardStatusLimitSwitchPositive185.value,
        negativeDirection:
          elevationBoardStatusLimitSwitchNegative0.value ||
          elevationBoardStatusLimitSwitchNegative5.value,
      },
      // 전체 상태 판단
      overallStatus:
        elevationBoardStatusLimitSwitchPositive180.value ||
        elevationBoardStatusLimitSwitchPositive185.value ||
        elevationBoardStatusLimitSwitchNegative0.value ||
        elevationBoardStatusLimitSwitchNegative5.value
          ? 'LIMIT_REACHED'
          : elevationBoardStatusStowPin.value
            ? 'STOWED'
            : elevationBoardStatusEncoder.value
              ? 'ENCODER_ACTIVE'
              : 'NORMAL',
    },
  }))

  // 전체 Train Board Servo 상태 정보를 제공하는 computed (기존 elevationBoardStatusInfo computed 뒤에 추가)
  const trainBoardServoStatusInfo = computed(() => ({
    raw: trainBoardServoStatusBits.value,
    servoAlarmCode1: trainBoardServoStatusServoAlarmCode1.value,
    servoAlarmCode2: trainBoardServoStatusServoAlarmCode2.value,
    servoAlarmCode3: trainBoardServoStatusServoAlarmCode3.value,
    servoAlarmCode4: trainBoardServoStatusServoAlarmCode4.value,
    servoAlarmCode5: trainBoardServoStatusServoAlarmCode5.value,
    servoAlarm: trainBoardServoStatusServoAlarm.value,
    servoBrake: trainBoardServoStatusServoBrake.value,
    servoMotor: trainBoardServoStatusServoMotor.value,
    // 활성화된 알람 코드 목록
    activeAlarmCodes: [
      trainBoardServoStatusServoAlarmCode1.value && 'AlarmCode1',
      trainBoardServoStatusServoAlarmCode2.value && 'AlarmCode2',
      trainBoardServoStatusServoAlarmCode3.value && 'AlarmCode3',
      trainBoardServoStatusServoAlarmCode4.value && 'AlarmCode4',
      trainBoardServoStatusServoAlarmCode5.value && 'AlarmCode5',
    ].filter(Boolean),
    // 활성화된 서보 상태 목록
    activeServoStatuses: [
      trainBoardServoStatusServoAlarm.value && 'Alarm',
      trainBoardServoStatusServoBrake.value && 'Brake',
      trainBoardServoStatusServoMotor.value && 'Motor',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalAlarmCodes: [
        trainBoardServoStatusServoAlarmCode1.value,
        trainBoardServoStatusServoAlarmCode2.value,
        trainBoardServoStatusServoAlarmCode3.value,
        trainBoardServoStatusServoAlarmCode4.value,
        trainBoardServoStatusServoAlarmCode5.value,
      ].filter(Boolean).length,
      hasAnyAlarmCode:
        trainBoardServoStatusServoAlarmCode1.value ||
        trainBoardServoStatusServoAlarmCode2.value ||
        trainBoardServoStatusServoAlarmCode3.value ||
        trainBoardServoStatusServoAlarmCode4.value ||
        trainBoardServoStatusServoAlarmCode5.value,
      hasServoAlarm: trainBoardServoStatusServoAlarm.value,
      isBrakeActive: trainBoardServoStatusServoBrake.value,
      isMotorActive: trainBoardServoStatusServoMotor.value,
      // 서보 상태
      servoStatus: {
        alarm: trainBoardServoStatusServoAlarm.value ? 'ALARM' : 'NORMAL',
        brake: trainBoardServoStatusServoBrake.value ? 'ON' : 'OFF',
        motor: trainBoardServoStatusServoMotor.value ? 'ON' : 'OFF',
      },
      // 전체 상태 판단
      overallStatus: trainBoardServoStatusServoAlarm.value
        ? 'ALARM'
        : trainBoardServoStatusServoMotor.value
          ? 'ACTIVE'
          : 'STANDBY',
    },
  }))
  // 전체 Train Board 상태 정보를 제공하는 computed (기존 trainBoardServoStatusInfo computed 뒤에 추가)
  const trainBoardStatusInfo = computed(() => ({
    raw: trainBoardStatusBits.value,
    limitSwitchPositive275: trainBoardStatusLimitSwitchPositive275.value,
    limitSwitchNegative275: trainBoardStatusLimitSwitchNegative275.value,
    reserve1: trainBoardStatusReserve1.value,
    reserve2: trainBoardStatusReserve2.value,
    stowPin: trainBoardStatusStowPin.value,
    reserve3: trainBoardStatusReserve3.value,
    reserve4: trainBoardStatusReserve4.value,
    encoder: trainBoardStatusEncoder.value,
    // 활성화된 리미트 스위치 목록
    activeLimitSwitches: [
      trainBoardStatusLimitSwitchPositive275.value && '+275°',
      trainBoardStatusLimitSwitchNegative275.value && '-275°',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      trainBoardStatusLimitSwitchPositive275.value && 'LimitSwitch+275',
      trainBoardStatusLimitSwitchNegative275.value && 'LimitSwitch-275',
      trainBoardStatusStowPin.value && 'StowPin',
      trainBoardStatusEncoder.value && 'Encoder',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      hasLimitSwitchActive:
        trainBoardStatusLimitSwitchPositive275.value ||
        trainBoardStatusLimitSwitchNegative275.value,
      isStowPinActive: trainBoardStatusStowPin.value,
      isEncoderActive: trainBoardStatusEncoder.value,
      totalActiveStatuses: [
        trainBoardStatusLimitSwitchPositive275.value,
        trainBoardStatusLimitSwitchNegative275.value,
        trainBoardStatusStowPin.value,
        trainBoardStatusEncoder.value,
      ].filter(Boolean).length,
      // 리미트 스위치 상태
      limitSwitchStatus: {
        positive275: trainBoardStatusLimitSwitchPositive275.value ? 'ACTIVE' : 'NORMAL',
        negative275: trainBoardStatusLimitSwitchNegative275.value ? 'ACTIVE' : 'NORMAL',
        anyActive:
          trainBoardStatusLimitSwitchPositive275.value ||
          trainBoardStatusLimitSwitchNegative275.value,
      },
      // 전체 상태 판단
      overallStatus:
        trainBoardStatusLimitSwitchPositive275.value || trainBoardStatusLimitSwitchNegative275.value
          ? 'LIMIT_REACHED'
          : trainBoardStatusStowPin.value
            ? 'STOWED'
            : trainBoardStatusEncoder.value
              ? 'ENCODER_ACTIVE'
              : 'NORMAL',
    },
  }))
  // 전체 Feed S-Band Board 상태 정보를 제공하는 computed (ICD 문서: Bits 15-8)
  const feedSBoardStatusInfo = computed(() => ({
    raw: feedSBoardStatusBits.value,
    sLnaLHCPPower: feedSBoardStatusLNALHCPPower.value,
    sLnaLHCPError: feedSBoardStatusLNALHCPError.value,
    sLnaRHCPPower: feedSBoardStatusLNARHCPPower.value,
    sLnaRHCPError: feedSBoardStatusLNARHCPError.value,
    // RF Switch는 ETC 바이트에 있음 (feedBoardETCStatusInfo 참조)
    // LNA 상태 정보
    lnaStatus: {
      lhcp: {
        power: feedSBoardStatusLNALHCPPower.value ? 'ON' : 'OFF',
        status: feedSBoardStatusLNALHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedSBoardStatusLNALHCPPower.value,
        hasError: feedSBoardStatusLNALHCPError.value,
      },
      rhcp: {
        power: feedSBoardStatusLNARHCPPower.value ? 'ON' : 'OFF',
        status: feedSBoardStatusLNARHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedSBoardStatusLNARHCPPower.value,
        hasError: feedSBoardStatusLNARHCPError.value,
      },
    },
    // 활성화된 LNA 목록
    activeLNAs: [
      feedSBoardStatusLNALHCPPower.value && 'LHCP',
      feedSBoardStatusLNARHCPPower.value && 'RHCP',
    ].filter(Boolean),
    // 에러가 있는 컴포넌트 목록
    errorComponents: [
      feedSBoardStatusLNALHCPError.value && 'LNA_LHCP',
      feedSBoardStatusLNARHCPError.value && 'LNA_RHCP',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      feedSBoardStatusLNALHCPPower.value && 'LNA_LHCP_ON',
      feedSBoardStatusLNARHCPPower.value && 'LNA_RHCP_ON',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActiveLNAs: [
        feedSBoardStatusLNALHCPPower.value,
        feedSBoardStatusLNARHCPPower.value,
      ].filter(Boolean).length,
      totalErrors: [feedSBoardStatusLNALHCPError.value, feedSBoardStatusLNARHCPError.value].filter(
        Boolean,
      ).length,
      hasAnyLNAActive: feedSBoardStatusLNALHCPPower.value || feedSBoardStatusLNARHCPPower.value,
      hasAnyError: feedSBoardStatusLNALHCPError.value || feedSBoardStatusLNARHCPError.value,
      // 전체 상태 판단
      overallStatus:
        feedSBoardStatusLNALHCPError.value || feedSBoardStatusLNARHCPError.value
          ? 'ERROR'
          : feedSBoardStatusLNALHCPPower.value || feedSBoardStatusLNARHCPPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 전체 Feed X-Band Board 상태 정보를 제공하는 computed (ICD 문서: Bits 23-16)
  const feedXBoardStatusInfo = computed(() => ({
    raw: feedXBoardStatusBits.value,
    xLnaLHCPPower: feedXBoardStatusLNALHCPPower.value,
    xLnaLHCPError: feedXBoardStatusLNALHCPError.value,
    xLnaRHCPPower: feedXBoardStatusLNARHCPPower.value,
    xLnaRHCPError: feedXBoardStatusLNARHCPError.value,
    // Fan은 ETC 바이트에 있음 (feedBoardETCStatusInfo 참조)
    // LNA 상태 정보
    lnaStatus: {
      lhcp: {
        power: feedXBoardStatusLNALHCPPower.value ? 'ON' : 'OFF',
        status: feedXBoardStatusLNALHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedXBoardStatusLNALHCPPower.value,
        hasError: feedXBoardStatusLNALHCPError.value,
      },
      rhcp: {
        power: feedXBoardStatusLNARHCPPower.value ? 'ON' : 'OFF',
        status: feedXBoardStatusLNARHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedXBoardStatusLNARHCPPower.value,
        hasError: feedXBoardStatusLNARHCPError.value,
      },
    },
    // 활성화된 LNA 목록
    activeLNAs: [
      feedXBoardStatusLNALHCPPower.value && 'LHCP',
      feedXBoardStatusLNARHCPPower.value && 'RHCP',
    ].filter(Boolean),
    // 에러가 있는 컴포넌트 목록
    errorComponents: [
      feedXBoardStatusLNALHCPError.value && 'LNA_LHCP',
      feedXBoardStatusLNARHCPError.value && 'LNA_RHCP',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      feedXBoardStatusLNALHCPPower.value && 'LNA_LHCP_ON',
      feedXBoardStatusLNARHCPPower.value && 'LNA_RHCP_ON',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActiveLNAs: [
        feedXBoardStatusLNALHCPPower.value,
        feedXBoardStatusLNARHCPPower.value,
      ].filter(Boolean).length,
      totalErrors: [feedXBoardStatusLNALHCPError.value, feedXBoardStatusLNARHCPError.value].filter(
        Boolean,
      ).length,
      hasAnyLNAActive: feedXBoardStatusLNALHCPPower.value || feedXBoardStatusLNARHCPPower.value,
      hasAnyError: feedXBoardStatusLNALHCPError.value || feedXBoardStatusLNARHCPError.value,
      // 전체 상태 판단
      overallStatus:
        feedXBoardStatusLNALHCPError.value || feedXBoardStatusLNARHCPError.value
          ? 'ERROR'
          : feedXBoardStatusLNALHCPPower.value || feedXBoardStatusLNARHCPPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 전체 Feed ETC Board 상태 정보를 제공하는 computed (ICD 문서: Bits 7-0)
  const feedBoardETCStatusInfo = computed(() => ({
    raw: feedBoardETCStatusBits.value,
    rfSwitchMode: feedBoardETCStatusRFSwitchMode.value, // 0=RHCP, 1=LHCP
    rfSwitchError: feedBoardETCStatusRFSwitchError.value,
    fanPower: feedBoardETCStatusFanPower.value,
    fanError: feedBoardETCStatusFanError.value,
    // RF Switch 상태 정보
    rfSwitchStatus: {
      mode: feedBoardETCStatusRFSwitchMode.value ? 'LHCP' : 'RHCP',
      status: feedBoardETCStatusRFSwitchError.value ? 'ERROR' : 'NORMAL',
      isRHCP: !feedBoardETCStatusRFSwitchMode.value,
      isLHCP: feedBoardETCStatusRFSwitchMode.value,
      hasError: feedBoardETCStatusRFSwitchError.value,
    },
    // Fan 상태 정보
    fanStatus: {
      power: feedBoardETCStatusFanPower.value ? 'ON' : 'OFF',
      status: feedBoardETCStatusFanError.value ? 'ERROR' : 'NORMAL',
      isActive: feedBoardETCStatusFanPower.value,
      hasError: feedBoardETCStatusFanError.value,
    },
    // 전체 상태 요약
    summary: {
      hasFanActive: feedBoardETCStatusFanPower.value,
      hasAnyError: feedBoardETCStatusRFSwitchError.value || feedBoardETCStatusFanError.value,
      currentRFSwitchMode: feedBoardETCStatusRFSwitchMode.value ? 'LHCP' : 'RHCP',
      overallStatus:
        feedBoardETCStatusRFSwitchError.value || feedBoardETCStatusFanError.value
          ? 'ERROR'
          : feedBoardETCStatusFanPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 전체 Feed Ka-Band Board 상태 정보를 제공하는 computed (ICD 문서: Bits 31-24)
  const feedKaBoardStatusInfo = computed(() => ({
    raw: feedKaBoardStatusBits.value,
    kaLnaLHCPPower: feedKaBoardStatusLNALHCPPower.value,
    kaLnaLHCPError: feedKaBoardStatusLNALHCPError.value,
    kaLnaRHCPPower: feedKaBoardStatusLNARHCPPower.value,
    kaLnaRHCPError: feedKaBoardStatusLNARHCPError.value,
    kaSelectionLHCPBand: feedKaBoardStatusSelectionLHCPBand.value ? 'Band2' : 'Band1',
    kaSelectionLHCPError: feedKaBoardStatusSelectionLHCPError.value,
    kaSelectionRHCPBand: feedKaBoardStatusSelectionRHCPBand.value ? 'Band2' : 'Band1',
    kaSelectionRHCPError: feedKaBoardStatusSelectionRHCPError.value,
    // LNA 상태 정보
    lnaStatus: {
      lhcp: {
        power: feedKaBoardStatusLNALHCPPower.value ? 'ON' : 'OFF',
        status: feedKaBoardStatusLNALHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedKaBoardStatusLNALHCPPower.value,
        hasError: feedKaBoardStatusLNALHCPError.value,
      },
      rhcp: {
        power: feedKaBoardStatusLNARHCPPower.value ? 'ON' : 'OFF',
        status: feedKaBoardStatusLNARHCPError.value ? 'ERROR' : 'NORMAL',
        isActive: feedKaBoardStatusLNARHCPPower.value,
        hasError: feedKaBoardStatusLNARHCPError.value,
      },
    },
    // Selection 상태 정보
    selectionStatus: {
      lhcp: {
        band: feedKaBoardStatusSelectionLHCPBand.value ? 'Band2' : 'Band1',
        error: feedKaBoardStatusSelectionLHCPError.value,
      },
      rhcp: {
        band: feedKaBoardStatusSelectionRHCPBand.value ? 'Band2' : 'Band1',
        error: feedKaBoardStatusSelectionRHCPError.value,
      },
    },
    // 활성화된 LNA 목록
    activeLNAs: [
      feedKaBoardStatusLNALHCPPower.value && 'LHCP',
      feedKaBoardStatusLNARHCPPower.value && 'RHCP',
    ].filter(Boolean),
    // 에러가 있는 컴포넌트 목록
    errorComponents: [
      feedKaBoardStatusLNALHCPError.value && 'LNA_LHCP',
      feedKaBoardStatusLNARHCPError.value && 'LNA_RHCP',
      feedKaBoardStatusSelectionLHCPError.value && 'SELECTION_LHCP',
      feedKaBoardStatusSelectionRHCPError.value && 'SELECTION_RHCP',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActiveLNAs: [
        feedKaBoardStatusLNALHCPPower.value,
        feedKaBoardStatusLNARHCPPower.value,
      ].filter(Boolean).length,
      totalErrors: [
        feedKaBoardStatusLNALHCPError.value,
        feedKaBoardStatusLNARHCPError.value,
        feedKaBoardStatusSelectionLHCPError.value,
        feedKaBoardStatusSelectionRHCPError.value,
      ].filter(Boolean).length,
      hasAnyLNAActive: feedKaBoardStatusLNALHCPPower.value || feedKaBoardStatusLNARHCPPower.value,
      hasAnyError:
        feedKaBoardStatusLNALHCPError.value ||
        feedKaBoardStatusLNARHCPError.value ||
        feedKaBoardStatusSelectionLHCPError.value ||
        feedKaBoardStatusSelectionRHCPError.value,
      // 전체 상태 판단
      overallStatus:
        feedKaBoardStatusLNALHCPError.value ||
        feedKaBoardStatusLNARHCPError.value ||
        feedKaBoardStatusSelectionLHCPError.value ||
        feedKaBoardStatusSelectionRHCPError.value
          ? 'ERROR'
          : feedKaBoardStatusLNALHCPPower.value || feedKaBoardStatusLNARHCPPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 타이머 관련 상태

  const isUpdating = ref(false)
  const updateCount = ref(0)
  const lastUpdateTime = ref(0)

  // 최신 데이터 버퍼 (WebSocket에서 받은 데이터 임시 저장, shallowRef: 30ms마다 전체 교체)
  const latestDataBuffer = shallowRef<MessageData | null>(null)
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
  // 🆕 추적 스케줄 정보를 위한 computed 속성
  const trackingScheduleInfo = computed(() => ({
    currentMstId: currentTrackingMstId.value,
    nextMstId: nextTrackingMstId.value,
    hasCurrentSchedule: currentTrackingMstId.value !== null,
    hasNextSchedule: nextTrackingMstId.value !== null,
    udpConnectionStatus: udpConnected.value,
    lastUdpUpdate: lastUdpUpdateTime.value,
    // 스케줄 상태 요약
    scheduleStatus: {
      isTracking: currentTrackingMstId.value !== null,
      hasUpcoming: nextTrackingMstId.value !== null,
      statusText:
        currentTrackingMstId.value !== null
          ? `현재: ${currentTrackingMstId.value}${nextTrackingMstId.value ? `, 다음: ${nextTrackingMstId.value}` : ''}`
          : '대기 중',
    },
  }))
  // 메시지 지연 통계 관련 상태 추가
  const messageDelayStats = ref({
    min: Number.MAX_VALUE,
    max: 0,
    total: 0,
    count: 0,
    average: 0,
  })

  // 업데이트 간격 측정 관련 상태 추가
  const lastUpdateTimestamp = ref(0)
  const updateInterval = ref(0)
  const updateIntervalStats = ref({
    min: Number.MAX_VALUE,
    max: 0,
    total: 0,
    count: 0,
    average: 0,
  })

  // 업데이트 간격 통계 초기화 함수
  const resetUpdateIntervalStats = () => {
    updateIntervalStats.value = {
      min: Number.MAX_VALUE,
      max: 0,
      total: 0,
      count: 0,
      average: 0,
    }
    lastUpdateTimestamp.value = 0
    console.log('📊 업데이트 간격 통계 초기화됨')
  }

  // 통계 초기화 함수 수정 (기존 함수에 업데이트 간격 통계도 포함)
  const resetMessageDelayStats = () => {
    messageDelayStats.value = {
      min: Number.MAX_VALUE,
      max: 0,
      total: 0,
      count: 0,
      average: 0,
    }
    updateCount.value = 0
    resetUpdateIntervalStats() // 업데이트 간격 통계도 함께 초기화
    console.log('📊 메시지 지연 통계 및 업데이트 카운트 초기화됨')
  }
  // ✅ 디버깅: WebSocket 메시지 수신 카운터
  const wsMessageCount = ref(0)

  // WebSocket 메시지 핸들러 - 데이터를 버퍼에만 저장
  const handleWebSocketMessage = async (message: MessageData) => {
    try {
      // ✅ 디버깅: WebSocket 메시지 수신 확인 (100번마다)
      wsMessageCount.value++

      // ✅ 디버깅 로그 비활성화 (Position View 점프 문제 디버깅 시에만 활성화)
      // if (wsMessageCount.value % 100 === 0) {
      //   console.log('🔍 [디버깅] WebSocket 메시지 수신 (handleWebSocketMessage):', {
      //     messageCount: wsMessageCount.value,
      //     hasData: !!message.data,
      //     dataType: typeof message.data,
      //     dataKeys:
      //       message.data && typeof message.data === 'object'
      //         ? Object.keys(message.data)
      //         : 'no data',
      //     hasCurrentTrackingMstId:
      //       message.data &&
      //       typeof message.data === 'object' &&
      //       'currentTrackingMstId' in message.data,
      //     hasNextTrackingMstId:
      //       message.data && typeof message.data === 'object' && 'nextTrackingMstId' in message.data,
      //     currentTrackingMstId:
      //       message.data && typeof message.data === 'object'
      //         ? (message.data as Record<string, unknown>).currentTrackingMstId
      //         : undefined,
      //     currentTrackingDetailId:
      //       message.data && typeof message.data === 'object'
      //         ? (message.data as Record<string, unknown>).currentTrackingDetailId
      //         : undefined,
      //     nextTrackingMstId:
      //       message.data && typeof message.data === 'object'
      //         ? (message.data as Record<string, unknown>).nextTrackingMstId
      //         : undefined,
      //     nextTrackingDetailId:
      //       message.data && typeof message.data === 'object'
      //         ? (message.data as Record<string, unknown>).nextTrackingDetailId
      //         : undefined,
      //   })
      // }

      // 받은 데이터를 버퍼에 저장만 하고 즉시 UI 업데이트하지 않음
      latestDataBuffer.value = message
      bufferUpdateTime.value = Date.now()

      // ✅ 에러 데이터 처리 (새로운 구조)
      try {
        if (message.data && typeof message.data === 'object' && 'errorData' in message.data) {
          const errorData = (message.data as Record<string, unknown>).errorData
          // console.log('🔍 에러 데이터 수신:', errorData)

          if (errorData && typeof errorData === 'object') {
            const errorDataObj = errorData as Record<string, unknown>
            // console.log('🔍 WebSocket 에러 데이터 수신:', errorDataObj)

            // 현재 언어 설정 가져오기 (사용하지 않으므로 제거)

            // 상태바 데이터 업데이트 (항상)
            if ('statusBarData' in errorDataObj) {
              // console.log('🔍 상태바 데이터 업데이트:', errorDataObj.statusBarData)

              const rawStatusBarData = errorDataObj.statusBarData as {
                activeErrorCount: number
                latestError: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  errorKey: string // ✅ 에러 키만 받음
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                } | null
                hasNewErrors: boolean
              }

              // latestError가 있으면 다국어 변환 적용
              if (rawStatusBarData.latestError) {
                const translatedMessage = translateHardwareError(
                  rawStatusBarData.latestError.errorKey,
                  rawStatusBarData.latestError.isResolved,
                )

                // 변환된 메시지로 상태바 데이터 구성
                errorStatusBarData.value = {
                  ...rawStatusBarData,
                  latestError: {
                    ...rawStatusBarData.latestError,
                    message: translatedMessage,
                    resolvedMessage: rawStatusBarData.latestError.isResolved
                      ? translatedMessage
                      : undefined,
                  },
                }
              } else {
                errorStatusBarData.value = {
                  ...rawStatusBarData,
                  latestError: rawStatusBarData.latestError
                    ? {
                        ...rawStatusBarData.latestError,
                        message: translateHardwareError(
                          rawStatusBarData.latestError.errorKey,
                          rawStatusBarData.latestError.isResolved,
                        ),
                        resolvedMessage: rawStatusBarData.latestError.isResolved
                          ? translateHardwareError(
                              rawStatusBarData.latestError.errorKey,
                              rawStatusBarData.latestError.isResolved,
                            )
                          : undefined,
                      }
                    : null,
                }
              }

              // 하드웨어 에러 로그 스토어에 상태바 데이터 반영
              const { useHardwareErrorLogStore } = await import('@/stores/hardwareErrorLogStore')
              const hardwareErrorLogStore = useHardwareErrorLogStore()

              if (rawStatusBarData?.hasNewErrors && rawStatusBarData?.latestError) {
                // 다국어 변환된 에러 로그를 스토어에 추가
                const localizedErrorLog = addLocalizedMessage(
                  rawStatusBarData.latestError as HardwareErrorLog,
                )
                // 로그 제거 (addErrorLog 내부에서 상태 변경 시에만 로그 출력)
                hardwareErrorLogStore.addErrorLog(localizedErrorLog)
              }
            }

            // 팝업 데이터 업데이트 (팝업이 열려있을 때만)
            if ('popupData' in errorDataObj) {
              // console.log('🔍 팝업 데이터 업데이트:', errorDataObj.popupData)

              const rawPopupData = errorDataObj.popupData as {
                isInitialLoad: boolean
                newLogs: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  errorKey: string // ✅ 에러 키만 받음
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                }[]
                totalLogCount: number
                lastUpdateTime: number
              }

              // rawPopupData null 체크 추가
              if (!rawPopupData || !rawPopupData.newLogs) {
                // console.warn('⚠️ rawPopupData 또는 newLogs가 null/undefined입니다:', rawPopupData)
                return
              }

              // newLogs 배열 유효성 체크
              if (!Array.isArray(rawPopupData.newLogs)) {
                console.warn('⚠️ newLogs가 배열이 아닙니다:', rawPopupData.newLogs)
                return
              }

              // newLogs에 다국어 변환 적용 (안전한 처리)
              let localizedNewLogs: HardwareErrorLog[] = []
              try {
                localizedNewLogs = rawPopupData.newLogs.map((log) =>
                  addLocalizedMessage(log as HardwareErrorLog),
                )
              } catch (mapError) {
                console.error('❌ newLogs 매핑 중 오류:', mapError)
                console.log('❌ 문제가 된 rawPopupData.newLogs:', rawPopupData.newLogs)
                return
              }

              errorPopupData.value = {
                ...rawPopupData,
                newLogs: localizedNewLogs.map((log) => ({
                  ...log,
                  resolvedAt: log.resolvedAt || '',
                  resolvedMessage: log.resolvedMessage || '',
                })),
              }

              const { useHardwareErrorLogStore } = await import('@/stores/hardwareErrorLogStore')
              const hardwareErrorLogStore = useHardwareErrorLogStore()

              if (rawPopupData?.newLogs && Array.isArray(rawPopupData.newLogs)) {
                // 다국어 변환된 로그들을 스토어에 추가
                console.log('🔍 icdStore - localizedNewLogs:', localizedNewLogs)
                hardwareErrorLogStore.addNewLogs(localizedNewLogs)
              }
            }
          }
        }
      } catch (errorDataError) {
        console.error('❌ 에러 데이터 처리 실패:', errorDataError)
      }

      // 디버깅용 (가끔씩만 로그)
      if (Math.random() < 0.01) {
        // 1% 확률
        console.log('📨 WebSocket 데이터 버퍼 업데이트:', new Date().toLocaleTimeString())
      }
    } catch (e) {
      console.error('❌ WebSocket 메시지 처리 오류:', e)
    }
  }

  // 고정밀 타이머 관련 상태 추가
  // ✅ targetInterval을 adjustInterval 함수에서 사용하도록 수정
  const targetInterval = UPDATE_INTERVAL // 30ms 목표 간격
  const adaptiveInterval = ref(targetInterval)
  const performanceHistory = ref<number[]>([])

  const adjustInterval = () => {
    const recentPerformance = performanceHistory.value.slice(-10)

    if (recentPerformance.length === 0) return

    const avgProcessingTime =
      recentPerformance.reduce((a, b) => a + b, 0) / recentPerformance.length

    // ✅ targetInterval 사용
    if (avgProcessingTime > targetInterval * 0.7) {
      // 목표의 70% 이상이면
      // 처리 시간이 길면 간격을 늘림

      adaptiveInterval.value = Math.min(targetInterval * 1.7, adaptiveInterval.value + 2)
    } else if (
      avgProcessingTime < targetInterval * 0.2 &&
      adaptiveInterval.value > targetInterval
    ) {
      // 처리 시간이 목표의 20% 미만이면 간격을 줄임
      adaptiveInterval.value = Math.max(targetInterval, adaptiveInterval.value - 1)
    }

    // ✅ 디버깅 로그 비활성화
    // if (Math.random() < 0.1) {
    //   console.log(
    //     `🔧 간격 조정: 평균처리시간 ${avgProcessingTime.toFixed(2)}ms, 목표 ${targetInterval}ms, 적응간격 ${adaptiveInterval.value}ms`,
    //   )
    // }
  }

  // ✅ 디버깅 로그 비활성화
  // const lastDebugLogTime = ref(0)
  // const DEBUG_LOG_INTERVAL = 10000 // 10초

  // 30ms 타이머로 실행되는 UI 업데이트 함수
  const updateUIFromBuffer = () => {
    try {
      const startTime = performance.now()
      // const currentTime = Date.now() // 디버깅 로그용 (비활성화)

      // 업데이트 간격 측정 (더 정확하게)
      if (lastUpdateTimestamp.value > 0) {
        const currentInterval = startTime - lastUpdateTimestamp.value
        updateInterval.value = currentInterval

        // 성능 히스토리 업데이트
        performanceHistory.value.push(currentInterval)
        if (performanceHistory.value.length > 20) {
          performanceHistory.value.shift()
        }

        // 간격이 너무 불규칙하면 건너뛰기
        if (currentInterval < UPDATE_INTERVAL * 0.5) {
          //console.warn(`⚠️ 너무 빠른 업데이트 건너뛰기: ${currentInterval.toFixed(2)}ms`)
          return
        }

        // 업데이트 간격 통계 업데이트
        updateIntervalStats.value.min = Math.min(updateIntervalStats.value.min, currentInterval)
        updateIntervalStats.value.max = Math.max(updateIntervalStats.value.max, currentInterval)
        updateIntervalStats.value.total += currentInterval
        updateIntervalStats.value.count++

        updateIntervalStats.value.average =
          updateIntervalStats.value.total / updateIntervalStats.value.count
      }
      lastUpdateTimestamp.value = startTime

      // 버퍼에 새 데이터가 있는지 확인
      if (!latestDataBuffer.value) {
        return
      }

      const message = latestDataBuffer.value
      updateCount.value++
      lastUpdateTime.value = Date.now()

      // serverTime 업데이트 (최우선)
      if (message.data && typeof message.data === 'object' && 'serverTime' in message.data) {
        const dataServerTime = (message.data as Record<string, unknown>).serverTime
        if (dataServerTime !== undefined && dataServerTime !== null) {
          // ✅ 디버깅 로그 비활성화
          // const oldTime = serverTime.value
          serverTime.value = safeToString(dataServerTime)
          // if (updateCount.value % 100 === 0) {
          //   console.log(`🕐 [${updateCount.value}] serverTime: ${oldTime} → ${serverTime.value}`)
          // }
        }
      } else {
        // ✅ 디버깅 로그 비활성화
        // console.log('❌ [Frontend] serverTime을 찾을 수 없습니다:', {
        //   messageServerTime: message.serverTime,
        //   messageData: message.data,
        //   hasData: !!message.data,
        //   dataKeys: message.data ? Object.keys(message.data) : 'no data',
        // })
      }

      // resultTimeOffsetCalTime 업데이트 - data 객체 안에서 찾기
      if (
        message.data &&
        typeof message.data === 'object' &&
        'resultTimeOffsetCalTime' in message.data
      ) {
        const dataResultTime = (message.data as Record<string, unknown>).resultTimeOffsetCalTime
        if (dataResultTime !== undefined && dataResultTime !== null) {
          resultTimeOffsetCalTime.value = safeToString(dataResultTime)
        }
      }

      // 명령 데이터 업데이트 - data 객체 안에서 찾기
      if (message.data && typeof message.data === 'object' && 'cmdAzimuthAngle' in message.data) {
        const dataCmdAzimuth = (message.data as Record<string, unknown>).cmdAzimuthAngle
        if (dataCmdAzimuth !== undefined && dataCmdAzimuth !== null) {
          cmdAzimuthAngle.value = safeToString(dataCmdAzimuth)
        }
      }

      if (message.data && typeof message.data === 'object' && 'cmdElevationAngle' in message.data) {
        const dataCmdElevation = (message.data as Record<string, unknown>).cmdElevationAngle
        if (dataCmdElevation !== undefined && dataCmdElevation !== null) {
          cmdElevationAngle.value = safeToString(dataCmdElevation)
        }
      }

      if (message.data && typeof message.data === 'object' && 'cmdTrainAngle' in message.data) {
        const dataCmdTrain = (message.data as Record<string, unknown>).cmdTrainAngle
        if (dataCmdTrain !== undefined && dataCmdTrain !== null) {
          cmdTrainAngle.value = safeToString(dataCmdTrain)
        }
      }

      // ✅ 디버깅 로그 비활성화
      // if (currentTime - lastDebugLogTime.value >= DEBUG_LOG_INTERVAL) {
      //   console.log('🔍 [디버깅] WebSocket 메시지 구조 확인:', { ... })
      // }

      // 🆕 추적 스케줄 정보 업데이트 - data 객체 안에서 찾기 (mstId와 detailId)
      if (
        message.data &&
        typeof message.data === 'object' &&
        'currentTrackingMstId' in message.data
      ) {
        const dataCurrentMstId = (message.data as Record<string, unknown>).currentTrackingMstId
        const dataCurrentDetailId = (message.data as Record<string, unknown>)
          .currentTrackingDetailId

        // ✅ 디버깅 로그 비활성화
        // if (currentTime - lastDebugLogTime.value >= DEBUG_LOG_INTERVAL) {
        //   console.log('🔍 [디버깅] WebSocket currentTrackingDetailId:', { ... })
        //   lastDebugLogTime.value = currentTime
        // }

        if (dataCurrentMstId !== undefined) {
          const newCurrentMstId = dataCurrentMstId as number | null
          const newCurrentDetailId =
            dataCurrentDetailId !== undefined ? (dataCurrentDetailId as number | null) : null
          if (
            currentTrackingMstId.value !== newCurrentMstId ||
            currentTrackingDetailId.value !== newCurrentDetailId
          ) {
            // ✅ 디버깅 로그 비활성화
            // console.log(
            //   `📋 현재 추적 MstId/DetailId 변경: ${currentTrackingMstId.value}/${currentTrackingDetailId.value} → ${newCurrentMstId}/${newCurrentDetailId}`,
            // )
            currentTrackingMstId.value = newCurrentMstId
            currentTrackingDetailId.value = newCurrentDetailId
          }
        }
      }

      if (message.data && typeof message.data === 'object' && 'nextTrackingMstId' in message.data) {
        const dataNextMstId = (message.data as Record<string, unknown>).nextTrackingMstId
        const dataNextDetailId = (message.data as Record<string, unknown>).nextTrackingDetailId

        // ✅ 디버깅 로그 비활성화
        // if (currentTime - lastDebugLogTime.value >= DEBUG_LOG_INTERVAL) {
        //   console.log('🔍 [디버깅] WebSocket nextTrackingDetailId:', { ... })
        //   lastDebugLogTime.value = currentTime
        // }

        if (dataNextMstId !== undefined) {
          const newNextMstId = dataNextMstId as number | null
          const newNextDetailId =
            dataNextDetailId !== undefined ? (dataNextDetailId as number | null) : null
          if (
            nextTrackingMstId.value !== newNextMstId ||
            nextTrackingDetailId.value !== newNextDetailId
          ) {
            // ✅ 디버깅 로그 비활성화
            // console.log(
            //   `📋 다음 추적 MstId/DetailId 변경: ${nextTrackingMstId.value}/${nextTrackingDetailId.value} → ${newNextMstId}/${newNextDetailId}`,
            // )
            nextTrackingMstId.value = newNextMstId
            nextTrackingDetailId.value = newNextDetailId
          }
        }
      }

      // ✅ 안테나 데이터 업데이트 - 타입 안전한 수정
      if (message.data && typeof message.data === 'object' && 'data' in message.data) {
        const messageData = message.data as Record<string, unknown>
        const antennaData = messageData.data

        if (antennaData && typeof antennaData === 'object') {
          updataAntennaData(antennaData as Record<string, unknown>)
        }
      }

      // 추적 상태 데이터 업데이트 - data 객체 안에서 찾기
      if (message.data && typeof message.data === 'object' && 'trackingStatus' in message.data) {
        const dataTrackingStatus = (message.data as Record<string, unknown>).trackingStatus
        if (dataTrackingStatus && typeof dataTrackingStatus === 'object') {
          updataTrackingStatus(dataTrackingStatus as Record<string, unknown>)
        }
      }
      if (message.communicationStatus !== undefined) {
        communicationStatus.value = safeToString(message.communicationStatus)
      }

      // 성능 측정
      const endTime = performance.now()

      messageDelay.value = endTime - startTime

      // 메시지 처리 지연 통계 업데이트

      messageDelayStats.value.min = Math.min(messageDelayStats.value.min, messageDelay.value)
      messageDelayStats.value.max = Math.max(messageDelayStats.value.max, messageDelay.value)
      messageDelayStats.value.total += messageDelay.value
      messageDelayStats.value.count++

      messageDelayStats.value.average =
        messageDelayStats.value.total / messageDelayStats.value.count

      // ✅ adjustInterval 함수 호출 (100회마다)
      if (updateCount.value % 100 === 0) {
        adjustInterval()
      }

      // ✅ 디버깅 로그 비활성화
      // if (updateCount.value % Math.floor(1000 / UPDATE_INTERVAL) === 0) {
      //   console.log(
      //     `📊 UI 업데이트 통계: ${updateCount.value}회, 처리시간: ${messageDelay.value.toFixed(2)}ms, 간격: ${updateInterval.value.toFixed(2)}ms, 적응간격: ${adaptiveInterval.value}ms`,
      //   )
      // }
    } catch (e) {
      console.error('❌ UI 업데이트 오류:', e)
    }
  }
  // 추적 상태 업데이트 함수 (순수 함수 사용)
  const updataTrackingStatus = (trackingStatusData: Record<string, unknown>) => {
    try {
      // 현재 상태
      const currentState: CurrentTrackingState = {
        ephemerisStatus: ephemerisStatus.value,
        ephemerisTrackingState: ephemerisTrackingState.value,
        passScheduleStatus: passScheduleStatus.value,
        passScheduleTrackingState: passScheduleTrackingState.value,
        sunTrackStatus: sunTrackStatus.value,
        sunTrackTrackingState: sunTrackTrackingState.value,
      }

      // 순수 함수로 업데이트할 필드 계산
      const updates = parseTrackingStatusUpdate(trackingStatusData, currentState)

      // 변경된 필드만 업데이트
      if (updates.ephemerisStatus !== undefined) {
        ephemerisStatus.value = updates.ephemerisStatus
      }
      if (updates.ephemerisTrackingState !== undefined) {
        ephemerisTrackingState.value = updates.ephemerisTrackingState
      }
      if (updates.passScheduleStatus !== undefined) {
        passScheduleStatus.value = updates.passScheduleStatus
      }
      if (updates.passScheduleTrackingState !== undefined) {
        passScheduleTrackingState.value = updates.passScheduleTrackingState
      }
      if (updates.sunTrackStatus !== undefined) {
        sunTrackStatus.value = updates.sunTrackStatus
      }
      if (updates.sunTrackTrackingState !== undefined) {
        sunTrackTrackingState.value = updates.sunTrackTrackingState
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
      if (antennaData.trainAngle !== undefined && antennaData.trainAngle !== null) {
        trainAngle.value = safeToString(antennaData.trainAngle)
      }
      if (antennaData.azimuthSpeed !== undefined && antennaData.azimuthSpeed !== null) {
        azimuthSpeed.value = safeToString(antennaData.azimuthSpeed)
      }
      if (antennaData.elevationSpeed !== undefined && antennaData.elevationSpeed !== null) {
        elevationSpeed.value = safeToString(antennaData.elevationSpeed)
      }
      if (antennaData.trainSpeed !== undefined && antennaData.trainSpeed !== null) {
        trainSpeed.value = safeToString(antennaData.trainSpeed)
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
        antennaData.servoDriverTrainAngle !== undefined &&
        antennaData.servoDriverTrainAngle !== null
      ) {
        servoDriverTrainAngle.value = safeToString(antennaData.servoDriverTrainAngle)
      }

      // 토크 데이터
      if (antennaData.torqueAzimuth !== undefined && antennaData.torqueAzimuth !== null) {
        torqueAzimuth.value = safeToString(antennaData.torqueAzimuth)
      }
      if (antennaData.torqueElevation !== undefined && antennaData.torqueElevation !== null) {
        torqueElevation.value = safeToString(antennaData.torqueElevation)
      }
      if (antennaData.torqueTrain !== undefined && antennaData.torqueTrain !== null) {
        torqueTrain.value = safeToString(antennaData.torqueTrain)
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
        const newBitString = safeToString(antennaData.mainBoardProtocolStatusBits)
        mainBoardProtocolStatusBits.value = newBitString
        parseProtocolStatusBits(newBitString)
      }
      if (
        antennaData.mainBoardStatusBits !== undefined &&
        antennaData.mainBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.mainBoardStatusBits)
        mainBoardStatusBits.value = newBitString
        parseMainBoardStatusBits(newBitString)
      }
      if (
        antennaData.mainBoardMCOnOffBits !== undefined &&
        antennaData.mainBoardMCOnOffBits !== null
      ) {
        const newBitString = safeToString(antennaData.mainBoardMCOnOffBits)
        mainBoardMCOnOffBits.value = newBitString
        parseMainBoardMCOnOffBits(newBitString)
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
        const newBitString = safeToString(antennaData.azimuthBoardServoStatusBits)
        azimuthBoardServoStatusBits.value = newBitString
        parseAzimuthBoardServoStatusBits(newBitString)
      }
      if (
        antennaData.azimuthBoardStatusBits !== undefined &&
        antennaData.azimuthBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.azimuthBoardStatusBits)
        azimuthBoardStatusBits.value = newBitString
        parseAzimuthBoardStatusBits(newBitString)
      }
      if (
        antennaData.elevationBoardServoStatusBits !== undefined &&
        antennaData.elevationBoardServoStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.elevationBoardServoStatusBits)
        elevationBoardServoStatusBits.value = newBitString
        parseElevationBoardServoStatusBits(newBitString)
      }
      if (
        antennaData.elevationBoardStatusBits !== undefined &&
        antennaData.elevationBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.elevationBoardStatusBits)
        elevationBoardStatusBits.value = newBitString
        parseElevationBoardStatusBits(newBitString)
      }
      if (
        antennaData.trainBoardServoStatusBits !== undefined &&
        antennaData.trainBoardServoStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.trainBoardServoStatusBits)
        trainBoardServoStatusBits.value = newBitString
        parseTrainBoardServoStatusBits(newBitString)
      }
      if (
        antennaData.trainBoardStatusBits !== undefined &&
        antennaData.trainBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.trainBoardStatusBits)
        trainBoardStatusBits.value = newBitString
        parseTrainBoardStatusBits(newBitString)
      }

      // Feed 보드 상태 (ICD 문서: 4바이트 Unsigned Long으로 전송, 백엔드에서 8비트씩 분리하여 전송)
      if (
        antennaData.feedBoardETCStatusBits !== undefined &&
        antennaData.feedBoardETCStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.feedBoardETCStatusBits)
        feedBoardETCStatusBits.value = newBitString
        parseFeedBoardETCStatusBits(newBitString)
      }
      if (
        antennaData.feedSBoardStatusBits !== undefined &&
        antennaData.feedSBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.feedSBoardStatusBits)
        // ✅ 디버깅 로그 비활성화
        // console.log('🔍 [WebSocket] feedSBoardStatusBits 수신:', newBitString, '(binary:', newBitString.padStart(8, '0'), ')')
        feedSBoardStatusBits.value = newBitString
        parseFeedSBoardStatusBits(newBitString)
      }
      if (
        antennaData.feedXBoardStatusBits !== undefined &&
        antennaData.feedXBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.feedXBoardStatusBits)
        feedXBoardStatusBits.value = newBitString
        parseFeedXBoardStatusBits(newBitString)
      }
      if (
        antennaData.feedKaBoardStatusBits !== undefined &&
        antennaData.feedKaBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.feedKaBoardStatusBits)
        feedKaBoardStatusBits.value = newBitString
        parseFeedKaBoardStatusBits(newBitString)
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
      if (
        antennaData.currentKaBandLNALHCP !== undefined &&
        antennaData.currentKaBandLNALHCP !== null
      ) {
        currentKaBandLNALHCP.value = safeToString(antennaData.currentKaBandLNALHCP)
      }
      if (
        antennaData.currentKaBandLNARHCP !== undefined &&
        antennaData.currentKaBandLNARHCP !== null
      ) {
        currentKaBandLNARHCP.value = safeToString(antennaData.currentKaBandLNARHCP)
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
      if (antennaData.rssiKaBandLNALHCP !== undefined && antennaData.rssiKaBandLNALHCP !== null) {
        rssiKaBandLNALHCP.value = safeToString(antennaData.rssiKaBandLNALHCP)
      }
      if (antennaData.rssiKaBandLNARHCP !== undefined && antennaData.rssiKaBandLNARHCP !== null) {
        rssiKaBandLNARHCP.value = safeToString(antennaData.rssiKaBandLNARHCP)
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
      if (antennaData.trainAcceleration !== undefined && antennaData.trainAcceleration !== null) {
        trainAcceleration.value = safeToString(antennaData.trainAcceleration)
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
        antennaData.trainMaxAcceleration !== undefined &&
        antennaData.trainMaxAcceleration !== null
      ) {
        trainMaxAcceleration.value = safeToString(antennaData.trainMaxAcceleration)
      }

      // 트래킹 데이터
      if (
        antennaData.trackingAzimuthTime !== undefined &&
        antennaData.trackingAzimuthTime !== null
      ) {
        trackingAzimuthTime.value = safeToString(antennaData.trackingAzimuthTime)
      }
      // ✅ 추적 데이터 수신 (디버깅 로그 비활성화)
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
      if (antennaData.trackingTrainTime !== undefined && antennaData.trackingTrainTime !== null) {
        trackingTrainTime.value = safeToString(antennaData.trackingTrainTime)
      }
      if (
        antennaData.trackingCMDTrainAngle !== undefined &&
        antennaData.trackingCMDTrainAngle !== null
      ) {
        trackingCMDTrainAngle.value = safeToString(antennaData.trackingCMDTrainAngle)
      }
      if (
        antennaData.trackingActualTrainAngle !== undefined &&
        antennaData.trackingActualTrainAngle !== null
      ) {
        trackingActualTrainAngle.value = safeToString(antennaData.trackingActualTrainAngle)
      }
    } catch (e) {
      console.error('❌ 센서 데이터 업데이트 오류:', e)
    }
  }

  const driftCorrection = ref(0)
  const timerStats = ref({
    onTime: 0, // 정시 실행 횟수
    early: 0, // 빠른 실행 횟수
    late: 0, // 늦은 실행 횟수
    totalDrift: 0, // 총 편차
  })

  // ✅ 정밀 타이머 타입 정의
  interface PreciseTimer {
    clear: () => void
    getStats: () => {
      onTime: number
      early: number
      late: number
      totalDrift: number
      targetInterval: number
      currentInterval: number
      driftCorrection: number
      accuracy: number
    }
  }

  // 정밀 타이머 함수 (반환 타입 명시)
  const preciseSetInterval = (callback: () => void, interval: number): PreciseTimer => {
    let expected = Date.now() + interval
    let timeout: NodeJS.Timeout

    const step = () => {
      const now = Date.now()
      const drift = now - expected

      // 드리프트 보정
      driftCorrection.value = drift

      // 통계 업데이트
      const tolerance = targetInterval * 0.1
      if (Math.abs(drift) <= tolerance) {
        timerStats.value.onTime++
      } else if (drift < 0) {
        timerStats.value.early++
      } else {
        timerStats.value.late++
      }
      timerStats.value.totalDrift += Math.abs(drift)

      // 콜백 실행
      callback()

      // 다음 실행 시간 계산 (드리프트 보정)
      expected += interval
      const nextDelay = Math.max(0, interval - drift)

      timeout = setTimeout(step, nextDelay)
    }

    timeout = setTimeout(step, interval)

    return {
      clear: () => clearTimeout(timeout),
      getStats: () => ({
        onTime: timerStats.value.onTime,
        early: timerStats.value.early,
        late: timerStats.value.late,
        totalDrift: timerStats.value.totalDrift,
        targetInterval,
        currentInterval: adaptiveInterval.value,
        driftCorrection: driftCorrection.value,
        accuracy:
          timerStats.value.onTime + timerStats.value.early + timerStats.value.late > 0
            ? (timerStats.value.onTime /
                (timerStats.value.onTime + timerStats.value.early + timerStats.value.late)) *
              100
            : 0,
      }),
    }
  }

  // ✅ 정밀 타이머 변수 타입 명시
  let preciseTimer: PreciseTimer | null = null

  // 타이머 시작
  const startUIUpdates = () => {
    if (preciseTimer) {
      preciseTimer.clear()
    }

    console.log(`🚀 정밀 UI 업데이트 타이머 시작 (목표: ${targetInterval}ms 주기)`)
    isUpdating.value = true
    updateCount.value = 0

    // 통계 초기화
    timerStats.value = { onTime: 0, early: 0, late: 0, totalDrift: 0 }
    adaptiveInterval.value = targetInterval

    // ✅ 정밀 타이머 시작 (타입 안전)
    preciseTimer = preciseSetInterval(() => {
      updateUIFromBuffer()
    }, targetInterval)
  }

  // ✅ 타이머 중지 (타입 안전)
  const stopUIUpdates = () => {
    if (preciseTimer) {
      try {
        const stats = preciseTimer.getStats() // 이제 타입 오류 없음
        console.log('📊 타이머 정확도 통계:', stats)
        preciseTimer.clear()
      } catch (error) {
        console.warn('⚠️ 타이머 통계 수집 중 오류:', error)
      } finally {
        preciseTimer = null
      }
    }

    isUpdating.value = false
    console.log('⏹️ 정밀 UI 업데이트 타이머 중지')
  }

  // WebSocket 연결 설정 - 구독 시스템 추가
  const connectWebSocket = async () => {
    try {
      error.value = ''

      console.log('🔌 WebSocket 연결 시작')

      // 클라이언트 ID 생성
      generateClientId()
      console.log('🆔 클라이언트 ID 생성:', clientId.value)

      // WebSocket 연결 (메시지는 버퍼에만 저장)
      await icdService.connectWebSocket(
        WEBSOCKET_URL,
        handleWebSocketMessage as (message: MessageData) => void,
      )
      isConnected.value = true

      console.log('✅ WebSocket 연결 성공')
    } catch (e) {
      const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.'
      error.value = `WebSocket 연결 실패: ${errorMessage}`
      isConnected.value = false
      throw e
    }
  }

  // WebSocket 구독자 추가 함수
  const subscribeWebSocket = (key: string, handler: (message: MessageData) => void) => {
    icdService.subscribeWebSocket(key, handler)
    console.log(`📡 WebSocket 구독 추가: ${key}`)
  }

  // WebSocket 구독자 제거 함수
  const unsubscribeWebSocket = (key: string, handler: (message: MessageData) => void) => {
    icdService.unsubscribeWebSocket(key, handler)
    console.log(`📡 WebSocket 구독 제거: ${key}`)
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
  }))

  // ✅ 새로운 computed 속성 추가
  const ephemerisTrackingStateInfo = computed(() => {
    const state = ephemerisTrackingState.value
    switch (state) {
      case 'IDLE':
        return { displayLabel: '정지', displayColor: 'grey' }
      // ✅ 새로운 상태 (6개 상태 체계)
      case 'PREPARING':
        return { displayLabel: '준비 중', displayColor: 'orange' }
      case 'WAITING':
        return { displayLabel: '시작 대기', displayColor: 'cyan' }
      case 'TRACKING':
        return { displayLabel: '추적 중', displayColor: 'green' }
      case 'COMPLETED':
        return { displayLabel: '완료', displayColor: 'purple' }
      case 'ERROR':
        return { displayLabel: '오류', displayColor: 'red' }
      // ✅ 기존 상태 (호환성 유지)
      case 'TRAIN_MOVING_TO_ZERO':
        return { displayLabel: 'Train 이동 중', displayColor: 'deep-orange' }
      case 'TRAIN_STABILIZING':
        return { displayLabel: 'Train 안정화', displayColor: 'amber-7' }
      case 'MOVING_TO_START':
        return { displayLabel: '시작 위치 이동', displayColor: 'blue' }
      case 'WAITING_FOR_TRACKING':
        return { displayLabel: '추적 대기', displayColor: 'cyan' }
      case 'IN_PROGRESS':
        return { displayLabel: '추적 중', displayColor: 'green' }
      default:
        return { displayLabel: '알 수 없음', displayColor: 'grey' }
    }
  })

  // ✅ Sun Track 추적 상태 정보 computed 속성 추가
  const sunTrackTrackingStateInfo = computed(() => {
    const state = sunTrackTrackingState.value

    // ✅ 디버깅 로그 추가
    console.log('☀️ sunTrackTrackingStateInfo computed 실행:', {
      현재상태: state,
      타입: typeof state,
      null여부: state === null,
      undefined여부: state === undefined,
    })

    switch (state) {
      case 'IDLE':
        return { displayLabel: '정지', displayColor: 'grey' }
      case 'TRAIN_MOVING_TO_ZERO':
        return { displayLabel: 'Train 이동', displayColor: 'deep-orange' }
      case 'TRAIN_STABILIZING':
        return { displayLabel: 'Train 안정화', displayColor: 'amber-7' }
      case 'TRACKING':
        return { displayLabel: '추적 중', displayColor: 'green' }
      default:
        console.log('☀️ 알 수 없는 상태 감지:', state)
        return { displayLabel: '알 수 없음', displayColor: 'grey' }
    }
  })

  // ✅ PassSchedule 추적 상태 정보 computed 속성 추가
  const passScheduleTrackingStateInfo = computed(() => {
    const state = passScheduleTrackingState.value
    switch (state) {
      // V2 상태 (11개)
      case 'IDLE':
        return { displayLabel: '정지', displayColor: 'grey' }
      case 'STOWING':
        return { displayLabel: 'Stow 이동', displayColor: 'blue' }
      case 'STOWED':
        return { displayLabel: 'Stow 대기', displayColor: 'blue-grey' }
      case 'MOVING_TRAIN':
        return { displayLabel: 'Train 이동', displayColor: 'deep-orange' }
      case 'TRAIN_STABILIZING':
        return { displayLabel: 'Train 안정화', displayColor: 'amber-7' }
      case 'MOVING_TO_START':
        return { displayLabel: '시작위치 이동', displayColor: 'cyan' }
      case 'READY':
        return { displayLabel: '추적 준비완료', displayColor: 'light-green' }
      case 'TRACKING':
        return { displayLabel: '추적 중', displayColor: 'green' }
      case 'POST_TRACKING':
        return { displayLabel: '추적 후 처리', displayColor: 'teal' }
      case 'COMPLETED':
        return { displayLabel: '완료', displayColor: 'purple' }
      case 'ERROR':
        return { displayLabel: '오류', displayColor: 'red' }
      // V1 호환 상태
      case 'WAITING':
        return { displayLabel: '대기 중', displayColor: 'blue-grey' }
      case 'PREPARING':
        return { displayLabel: '준비 중', displayColor: 'orange' }
      default:
        return { displayLabel: '알 수 없음', displayColor: 'grey' }
    }
  })

  // Standby 명령 전송
  const standbyCommand = async (azimuth: boolean, elevation: boolean, train: boolean) => {
    try {
      const response = await icdService.standbyCommand(azimuth, elevation, train)
      return {
        success: true,
        data: response,
        message: 'Standby 명령이 전송되었습니다.',
        axes: response.axes || '', // 백엔드에서 반환하는 축 정보
      }
    } catch (error) {
      console.error('Standby 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: 'Standby 명령 전송에 실패했습니다.',
      }
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
  const sendServoPresetCommand = async (azimuth: number, elevation: number, train: number) => {
    try {
      const response = await icdService.sendServoPresetCommand(
        azimuth > 0,
        elevation > 0,
        train > 0,
      )
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
  const sendMCOnOffCommand = async (onOff: boolean = true) => {
    try {
      const result = await icdService.sendMCOnOffCommand(onOff)

      // 성공 시 UI 업데이트
      const status = onOff ? 'ON' : 'OFF'
      console.log(`M/C 전원 ${status} 명령 완료`)

      return result
    } catch (error) {
      console.error('M/C On/Off 명령 오류:', error)
      throw error
    }
  }
  const sendServoAlarmResetCommand = async (
    azimuth: boolean = false,
    elevation: boolean = false,
    train: boolean = false,
  ) => {
    try {
      const result = await icdService.sendServoAlarmResetCommand(azimuth, elevation, train)

      // 성공 시 UI 업데이트
      const axes = []
      if (azimuth) axes.push('AZIMUTH')
      if (elevation) axes.push('ELEVATION')
      if (train) axes.push('TRAIN')

      console.log(`Servo Alarm Reset 명령 완료: ${axes.join(', ')}`)

      return result
    } catch (error) {
      console.error('Servo Alarm Reset 명령 오류:', error)
      throw error
    }
  }

  // 정지 명령 전송
  const stopCommand = async (azimuth: boolean, elevation: boolean, train: boolean) => {
    try {
      const response = await icdService.stopCommand(azimuth, elevation, train)
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
  const sendReadFwVerSerialNoStatusCommand = async () => {
    try {
      const result = await icdService.sendReadFwVerSerialNoStatusCommand()

      console.log('Firmware Version/Serial Number 조회 완료')

      return result
    } catch (error) {
      console.error('Firmware Version/Serial Number 조회 오류:', error)
      throw error
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
    kaLHCP = false,
    kaRHCP = false,
    kaSelectionRHCP = false,
    kaSelectionLHCP = false,
  ) => {
    try {
      const response = await icdService.sendFeedOnOffCommand(
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
      )

      // 명령 전송 성공 시 즉시 상태 업데이트 (Optimistic Update)
      // WebSocket으로 실제 상태가 돌아올 때까지 UI가 즉시 반영되도록 함
      feedSBoardStatusLNALHCPPower.value = sLHCP
      feedSBoardStatusLNARHCPPower.value = sRHCP
      feedBoardETCStatusRFSwitchMode.value = sRFSwitch
      feedXBoardStatusLNALHCPPower.value = xLHCP
      feedXBoardStatusLNARHCPPower.value = xRHCP
      feedBoardETCStatusFanPower.value = fan
      feedKaBoardStatusLNALHCPPower.value = kaLHCP
      feedKaBoardStatusLNARHCPPower.value = kaRHCP
      feedKaBoardStatusSelectionRHCPBand.value = kaSelectionRHCP
      feedKaBoardStatusSelectionLHCPBand.value = kaSelectionLHCP

      console.log('✅ Feed 상태 Optimistic Update 완료')

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
    trainSpeed: number,
  ) => {
    try {
      const response = await icdService.startSunTrack(interval, azSpeed, elSpeed, trainSpeed)
      return { success: true, data: response, message: 'Sun Track이 시작되었습니다.' }
    } catch (error) {
      console.error('Sun Track 시작 실패:', error)
      return { success: false, error: String(error), message: 'Sun Track 시작에 실패했습니다.' }
    }
  }
  // Sun Track 중지
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
        azimuthOffset: azOffset, // 실제 응답 구조에 맞게 수정 필요
        elevationOffset: elOffset, // 실제 응답 구조에 맞게 수정 필요
        trainOffset: tiOffset, // 실제 응답 구조에 맞게 수정 필요
      }
    } catch (error) {
      console.error('위치 오프셋 명령 전송 실패:', error)
      return {
        success: false,
        error: String(error),
        message: '위치 오프셋 명령 전송에 실패했습니다.',
        azimuthOffset: 0,
        elevationOffset: 0,
        trainOffset: 0,
      }
    }
  }

  // 시간 오프셋 명령 전송
  async function sendTimeOffsetCommand(timeOffset: number) {
    try {
      const response = await icdService.sendTimeOffsetCommand(timeOffset)
      return {
        success: true,
        data: response,
        message: '시간 오프셋 명령이 전송되었습니다.',
        inputTimeoffset: timeOffset, // 실제 응답 구조에 맞게 수정 필요
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
    // 기본 상태
    serverTime,
    resultTimeOffsetCalTime,
    cmdAzimuthAngle,
    cmdElevationAngle,
    cmdTrainAngle: cmdTrainAngle,
    cmdTime,
    error,
    isConnected,
    messageDelay,
    lastUpdateTime,

    // 안테나 데이터
    modeStatusBits,
    azimuthAngle,
    elevationAngle,
    trainAngle: trainAngle,
    azimuthSpeed,
    elevationSpeed,
    trainSpeed: trainSpeed,
    servoDriverAzimuthAngle,
    servoDriverElevationAngle,
    servoDriverTrainAngle: servoDriverTrainAngle,
    torqueAzimuth,
    torqueElevation,
    torqueTrain: torqueTrain,
    windSpeed,
    windDirection,
    rtdOne,
    rtdTwo,

    // 보드 상태 비트
    mainBoardProtocolStatusBits,
    mainBoardStatusBits,
    mainBoardMCOnOffBits,
    mainBoardReserveBits,
    azimuthBoardServoStatusBits,
    azimuthBoardStatusBits,
    elevationBoardServoStatusBits,
    elevationBoardStatusBits,
    trainBoardServoStatusBits: trainBoardServoStatusBits,
    trainBoardStatusBits: trainBoardStatusBits,
    feedBoardETCStatusBits,
    feedSBoardStatusBits,
    feedXBoardStatusBits,
    feedKaBoardStatusBits,

    // LNA 및 RSSI 데이터
    currentSBandLNALHCP,
    currentSBandLNARHCP,
    currentXBandLNALHCP,
    currentXBandLNARHCP,
    currentKaBandLNALHCP,
    currentKaBandLNARHCP,
    rssiSBandLNALHCP,
    rssiSBandLNARHCP,
    rssiXBandLNALHCP,
    rssiXBandLNARHCP,
    rssiKaBandLNALHCP,
    rssiKaBandLNARHCP,

    // 가속도 데이터
    azimuthAcceleration,
    elevationAcceleration,
    trainAcceleration: trainAcceleration,
    azimuthMaxAcceleration,
    elevationMaxAcceleration,
    trainMaxAcceleration: trainMaxAcceleration,

    // 추적 데이터
    trackingAzimuthTime,
    trackingCMDAzimuthAngle,
    trackingActualAzimuthAngle,
    trackingElevationTime,
    trackingCMDElevationAngle,
    trackingActualElevationAngle,
    trackingTrainTime: trackingTrainTime,
    trackingCMDTrainAngle: trackingCMDTrainAngle,
    trackingActualTrainAngle: trackingActualTrainAngle,

    // 업데이트 관련
    isUpdating,
    updateCount,
    messageDelayStats,
    updateInterval,
    updateIntervalStats,

    // 계산된 속성
    hasActiveConnection,
    lastUpdateTimeFormatted,
    connectionStatus,
    trackingScheduleInfo,

    // 비트 처리 정보
    mainBoardStatusInfo,
    protocolStatusInfo,
    mainBoardMCOnOffInfo,
    azimuthBoardServoStatusInfo,
    azimuthBoardStatusInfo,
    elevationBoardServoStatusInfo,
    elevationBoardStatusInfo,
    trainBoardServoStatusInfo: trainBoardServoStatusInfo,
    trainBoardStatusInfo: trainBoardStatusInfo,
    feedBoardETCStatusInfo,
    feedSBoardStatusInfo,
    feedXBoardStatusInfo,
    feedKaBoardStatusInfo,

    // 모드 상태 정보
    ephemerisStatus,
    ephemerisStatusInfo,
    ephemerisTrackingState,
    ephemerisTrackingStateInfo,
    passScheduleStatus,
    passScheduleStatusInfo,
    passScheduleTrackingState,
    passScheduleTrackingStateInfo,
    sunTrackStatus,
    sunTrackStatusInfo,
    sunTrackTrackingState,
    sunTrackTrackingStateInfo,

    // 펌웨어 UDP 상태
    communicationStatus,
    adaptiveInterval,
    driftCorrection,
    timerStats,

    // 추적 스케줄 정보
    currentTrackingMstId: readonly(currentTrackingMstId),
    currentTrackingDetailId: readonly(currentTrackingDetailId), // ✅ detailId 추가
    nextTrackingMstId: readonly(nextTrackingMstId),
    nextTrackingDetailId: readonly(nextTrackingDetailId), // ✅ detailId 추가
    udpConnected: readonly(udpConnected),
    lastUdpUpdateTime: readonly(lastUdpUpdateTime),

    // 에러 데이터
    errorStatusBarData: readonly(errorStatusBarData),
    errorPopupData: readonly(errorPopupData),
    clientId: readonly(clientId),

    // 메서드
    initialize,
    cleanup,
    startUIUpdates,
    stopUIUpdates,
    connectWebSocket,
    disconnectWebSocket,
    subscribeWebSocket,
    unsubscribeWebSocket,
    getDebugInfo,
    sendEmergency,
    sendMultiControlCommand,
    sendServoPresetCommand,
    stopCommand,
    standbyCommand,
    stowCommand,
    sendFeedOnOffCommand,
    startSunTrack,
    sendPositionOffsetCommand,
    sendTimeOffsetCommand,
    resetMessageDelayStats,
    resetUpdateIntervalStats,
    sendMCOnOffCommand,
    sendServoAlarmResetCommand,
    sendReadFwVerSerialNoStatusCommand,

    // 하드웨어 에러 변환 함수들
    translateHardwareError,
    addLocalizedMessage,
  }
})
