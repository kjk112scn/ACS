import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose, readonly } from 'vue'
import { icdService, type MessageData, type MultiControlCommand } from '@/services'
import type { HardwareErrorLog } from '@/types/hardwareError'

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

// WebSocket 서버 URL
const WEBSOCKET_URL = 'ws://localhost:8080/ws'

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
  // feedXBoardStatusBits 관련 개별 상태들 (기존 feedSBoardStatusBits 관련 상태들 뒤에 추가)
  const feedXBoardStatusLNALHCPPower = ref<boolean>(false) // X-Band LNA LHCP ON/OFF (1=ON, 0=OFF)
  const feedXBoardStatusLNALHCPError = ref<boolean>(false) // X-Band LNA LHCP Error/Normal (1=Error, 0=Normal)
  const feedXBoardStatusLNARHCPPower = ref<boolean>(false) // X-Band LNA RHCP ON/OFF (1=ON, 0=OFF)
  const feedXBoardStatusLNARHCPError = ref<boolean>(false) // X-Band LNA RHCP Error/Normal (1=Error, 0=Normal)
  const feedXBoardStatusBitsReserve1 = ref<boolean>(false)
  const feedXBoardStatusBitsReserve2 = ref<boolean>(false)
  const feedBoardStatusBitsFanPower = ref<boolean>(false) // Fan Power ON/OFF (1=ON, 0=OFF)
  const feedBoardStatusBitsFanError = ref<boolean>(false) // Fan Error/Normal (1=Error, 0=Normal)

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
  const sunTrackStatus = ref<boolean | null>(null)
  const sunTrackTrackingState = ref<string | null>(null) // ✅ 추가
  const communicationStatus = ref('')
  const currentTrackingMstId = ref<number | null>(null)
  const nextTrackingMstId = ref<number | null>(null)
  const udpConnected = ref<boolean>(false)
  const lastUdpUpdateTime = ref<string>('')
  
  // 에러 데이터 상태
  const errorStatusBarData = ref<{
    activeErrorCount: number
    latestError: {
      id: string
      timestamp: string
      category: string
      severity: string
      message: { ko: string; en: string }
      component: string
      isResolved: boolean
      resolvedAt: string | null
      resolvedMessage: { ko: string; en: string } | null
    } | null
    hasNewErrors: boolean
  } | null>(null)
  const errorPopupData = ref<{
    isInitialLoad: boolean
    newLogs: {
      id: string
      timestamp: string
      category: string
      severity: string
      message: { ko: string; en: string }
      component: string
      isResolved: boolean
      resolvedAt: string | null
      resolvedMessage: { ko: string; en: string } | null
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

  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수
  const parseProtocolStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    protocolElevationStatus.value = bits[0] === '1' // 1번째 비트
    protocolAzimuthStatus.value = bits[1] === '1' // 2번째 비트
    protocolTrainStatus.value = bits[2] === '1' // 3번째 비트
    protocolFeedStatus.value = bits[3] === '1' // 4번째 비트
    mainBoardProtocolStatusBitsReserve1.value = bits[4] === '1' // 5번째 비트
    mainBoardProtocolStatusBitsReserve2.value = bits[5] === '1' // 6번째 비트
    mainBoardProtocolStatusBitsReserve3.value = bits[6] === '1' // 7번째 비트
    defaultReceiveStatus.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseProtocolStatusBits 함수 뒤에 추가)
  const parseMainBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    powerSurgeProtector.value = bits[0] === '1' // 1번째 비트
    powerReversePhaseSensor.value = bits[1] === '1' // 2번째 비트
    emergencyStopACU.value = bits[2] === '1' // 3번째 비트
    emergencyStopPositioner.value = bits[3] === '1' // 4번째 비트
    mainBoardStatusBitsReserve1.value = bits[4] === '1' // 5번째 비트
    mainBoardStatusBitsReserve2.value = bits[5] === '1' // 6번째 비트
    mainBoardStatusBitsReserve3.value = bits[6] === '1' // 7번째 비트
    mainBoardStatusBitsReserve4.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseMainBoardStatusBits 함수 뒤에 추가)
  const parseMainBoardMCOnOffBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    mcTrain.value = bits[0] === '1' // 1번째 비트
    mcElevation.value = bits[1] === '1' // 2번째 비트
    mcAzimuth.value = bits[2] === '1' // 3번째 비트
    mainBoardMCOnOffBitsReserve1.value = bits[3] === '1' // 4번째 비트
    mainBoardMCOnOffBitsReserve2.value = bits[4] === '1' // 5번째 비트
    mainBoardMCOnOffBitsReserve3.value = bits[5] === '1' // 6번째 비트
    mainBoardMCOnOffBitsReserve4.value = bits[6] === '1' // 7번째 비트
    mainBoardMCOnOffBitsReserve5.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseMainBoardMCOnOffBits 함수 뒤에 추가)
  const parseAzimuthBoardServoStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    azimuthBoardServoStatusServoAlarmCode1.value = bits[0] === '1' // 1번째 비트
    azimuthBoardServoStatusServoAlarmCode2.value = bits[1] === '1' // 2번째 비트
    azimuthBoardServoStatusServoAlarmCode3.value = bits[2] === '1' // 3번째 비트
    azimuthBoardServoStatusServoAlarmCode4.value = bits[3] === '1' // 4번째 비트
    azimuthBoardServoStatusServoAlarmCode5.value = bits[4] === '1' // 5번째 비트
    azimuthBoardServoStatusServoAlarm.value = bits[5] === '1' // 6번째 비트
    azimuthBoardServoStatusServoBrake.value = bits[6] === '1' // 7번째 비트
    azimuthBoardServoStatusServoMotor.value = bits[7] === '1' // 8번째 비트
  }

  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseAzimuthBoardServoStatusBits 함수 뒤에 추가)
  const parseAzimuthBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    azimuthBoardStatusLimitSwitchPositive275.value = bits[0] === '1' // 1번째 비트
    azimuthBoardStatusLimitSwitchNegative275.value = bits[1] === '1' // 2번째 비트
    azimuthBoardStatusReserve1.value = bits[2] === '1' // 3번째 비트
    azimuthBoardStatusReserve2.value = bits[3] === '1' // 4번째 비트
    azimuthBoardStatusStowPin.value = bits[4] === '1' // 5번째 비트
    azimuthBoardStatusReserve3.value = bits[5] === '1' // 6번째 비트
    azimuthBoardStatusReserve4.value = bits[6] === '1' // 7번째 비트
    azimuthBoardStatusEncoder.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseAzimuthBoardStatusBits 함수 뒤에 추가)
  const parseElevationBoardServoStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    elevationBoardServoStatusServoAlarmCode1.value = bits[0] === '1' // 1번째 비트
    elevationBoardServoStatusServoAlarmCode2.value = bits[1] === '1' // 2번째 비트
    elevationBoardServoStatusServoAlarmCode3.value = bits[2] === '1' // 3번째 비트
    elevationBoardServoStatusServoAlarmCode4.value = bits[3] === '1' // 4번째 비트
    elevationBoardServoStatusServoAlarmCode5.value = bits[4] === '1' // 5번째 비트
    elevationBoardServoStatusServoAlarm.value = bits[5] === '1' // 6번째 비트
    elevationBoardServoStatusServoBrake.value = bits[6] === '1' // 7번째 비트
    elevationBoardServoStatusServoMotor.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseElevationBoardServoStatusBits 함수 뒤에 추가)
  const parseElevationBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    elevationBoardStatusLimitSwitchPositive180.value = bits[0] === '1' // 1번째 비트
    elevationBoardStatusLimitSwitchPositive185.value = bits[1] === '1' // 2번째 비트
    elevationBoardStatusLimitSwitchNegative0.value = bits[2] === '1' // 3번째 비트
    elevationBoardStatusLimitSwitchNegative5.value = bits[3] === '1' // 4번째 비트
    elevationBoardStatusStowPin.value = bits[4] === '1' // 5번째 비트
    elevationBoardStatusReserve1.value = bits[5] === '1' // 6번째 비트
    elevationBoardStatusReserve2.value = bits[6] === '1' // 7번째 비트
    elevationBoardStatusEncoder.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseElevationBoardStatusBits 함수 뒤에 추가)
  const parseTrainBoardServoStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    trainBoardServoStatusServoAlarmCode1.value = bits[0] === '1' // 1번째 비트
    trainBoardServoStatusServoAlarmCode2.value = bits[1] === '1' // 2번째 비트
    trainBoardServoStatusServoAlarmCode3.value = bits[2] === '1' // 3번째 비트
    trainBoardServoStatusServoAlarmCode4.value = bits[3] === '1' // 4번째 비트
    trainBoardServoStatusServoAlarmCode5.value = bits[4] === '1' // 5번째 비트
    trainBoardServoStatusServoAlarm.value = bits[5] === '1' // 6번째 비트
    trainBoardServoStatusServoBrake.value = bits[6] === '1' // 7번째 비트
    trainBoardServoStatusServoMotor.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseTrainBoardServoStatusBits 함수 뒤에 추가)
  const parseTrainBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    trainBoardStatusLimitSwitchPositive275.value = bits[0] === '1' // 1번째 비트
    trainBoardStatusLimitSwitchNegative275.value = bits[1] === '1' // 2번째 비트
    trainBoardStatusReserve1.value = bits[2] === '1' // 3번째 비트
    trainBoardStatusReserve2.value = bits[3] === '1' // 4번째 비트
    trainBoardStatusStowPin.value = bits[4] === '1' // 5번째 비트
    trainBoardStatusReserve3.value = bits[5] === '1' // 6번째 비트
    trainBoardStatusReserve4.value = bits[6] === '1' // 7번째 비트
    trainBoardStatusEncoder.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseTrainBoardStatusBits 함수 뒤에 추가)
  const parseFeedSBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    feedSBoardStatusLNALHCPPower.value = bits[0] === '1' // 1번째 비트
    feedSBoardStatusLNALHCPError.value = bits[1] === '1' // 2번째 비트
    feedSBoardStatusLNARHCPPower.value = bits[2] === '1' // 3번째 비트
    feedSBoardStatusLNARHCPError.value = bits[3] === '1' // 4번째 비트
    feedSBoardStatusRFSwitchMode.value = bits[4] === '1' // 5번째 비트
    feedSBoardStatusRFSwitchError.value = bits[5] === '1' // 6번째 비트
    feedSBoardStatusBitsReserve1.value = bits[6] === '1' // 7번째 비트
    feedSBoardStatusBitsReserve2.value = bits[7] === '1' // 8번째 비트
  }
  // 비트 문자열을 개별 boolean으로 파싱하는 헬퍼 함수 (기존 parseFeedSBoardStatusBits 함수 뒤에 추가)
  const parseFeedXBoardStatusBits = (bitString: string) => {
    // "00001000" -> ['0','0','0','0','1','0','0','0']
    const bits = bitString.padStart(8, '0').split('').reverse() // 오른쪽부터 1번째 비트

    feedXBoardStatusLNALHCPPower.value = bits[0] === '1' // 1번째 비트
    feedXBoardStatusLNALHCPError.value = bits[1] === '1' // 2번째 비트
    feedXBoardStatusLNARHCPPower.value = bits[2] === '1' // 3번째 비트
    feedXBoardStatusLNARHCPError.value = bits[3] === '1' // 4번째 비트
    feedXBoardStatusBitsReserve1.value = bits[4] === '1' // 5번째 비트
    feedXBoardStatusBitsReserve2.value = bits[5] === '1' // 6번째 비트
    feedBoardStatusBitsFanPower.value = bits[6] === '1' // 7번째 비트
    feedBoardStatusBitsFanError.value = bits[7] === '1' // 8번째 비트
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
  // 전체 Feed S-Band Board 상태 정보를 제공하는 computed (기존 trainBoardStatusInfo computed 뒤에 추가)
  const feedSBoardStatusInfo = computed(() => ({
    raw: feedSBoardStatusBits.value,
    sLnaLHCPPower: feedSBoardStatusLNALHCPPower.value,
    sLnaLHCPError: feedSBoardStatusLNALHCPError.value,
    sLnaRHCPPower: feedSBoardStatusLNARHCPPower.value,
    sLnaRHCPError: feedSBoardStatusLNARHCPError.value,
    sRFSwitchMode: feedSBoardStatusRFSwitchMode.value,
    sRFSwitchError: feedSBoardStatusRFSwitchError.value,
    sReserve1: feedSBoardStatusBitsReserve1.value,
    sReserve2: feedSBoardStatusBitsReserve2.value,
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
    // RF Switch 상태 정보
    rfSwitchStatus: {
      mode: feedSBoardStatusRFSwitchMode.value ? 'RHCP' : 'LHCP',
      status: feedSBoardStatusRFSwitchError.value ? 'ERROR' : 'NORMAL',
      isRHCP: feedSBoardStatusRFSwitchMode.value,
      isLHCP: !feedSBoardStatusRFSwitchMode.value,
      hasError: feedSBoardStatusRFSwitchError.value,
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
      feedSBoardStatusRFSwitchError.value && 'RF_SWITCH',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      feedSBoardStatusLNALHCPPower.value && 'LNA_LHCP_ON',
      feedSBoardStatusLNARHCPPower.value && 'LNA_RHCP_ON',
      feedSBoardStatusRFSwitchMode.value && 'RF_SWITCH_RHCP',
      !feedSBoardStatusRFSwitchMode.value && 'RF_SWITCH_LHCP',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActiveLNAs: [
        feedSBoardStatusLNALHCPPower.value,
        feedSBoardStatusLNARHCPPower.value,
      ].filter(Boolean).length,
      totalErrors: [
        feedSBoardStatusLNALHCPError.value,
        feedSBoardStatusLNARHCPError.value,
        feedSBoardStatusRFSwitchError.value,
      ].filter(Boolean).length,
      hasAnyLNAActive: feedSBoardStatusLNALHCPPower.value || feedSBoardStatusLNARHCPPower.value,
      hasAnyError:
        feedSBoardStatusLNALHCPError.value ||
        feedSBoardStatusLNARHCPError.value ||
        feedSBoardStatusRFSwitchError.value,
      currentRFSwitchMode: feedSBoardStatusRFSwitchMode.value ? 'RHCP' : 'LHCP',
      // 전체 상태 판단
      overallStatus:
        feedSBoardStatusLNALHCPError.value ||
        feedSBoardStatusLNARHCPError.value ||
        feedSBoardStatusRFSwitchError.value
          ? 'ERROR'
          : feedSBoardStatusLNALHCPPower.value || feedSBoardStatusLNARHCPPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 전체 Feed X-Band Board 상태 정보를 제공하는 computed (기존 feedSBoardStatusInfo computed 뒤에 추가)
  const feedXBoardStatusInfo = computed(() => ({
    raw: feedXBoardStatusBits.value,
    xLnaLHCPPower: feedXBoardStatusLNALHCPPower.value,
    xLnaLHCPError: feedXBoardStatusLNALHCPError.value,
    xLnaRHCPPower: feedXBoardStatusLNARHCPPower.value,
    xLnaRHCPError: feedXBoardStatusLNARHCPError.value,
    xReserve1: feedXBoardStatusBitsReserve1.value,
    xReserve2: feedXBoardStatusBitsReserve2.value,
    fanPower: feedBoardStatusBitsFanPower.value,
    fanError: feedBoardStatusBitsFanError.value,
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
    // Fan 상태 정보
    fanStatus: {
      power: feedBoardStatusBitsFanPower.value ? 'ON' : 'OFF',
      status: feedBoardStatusBitsFanError.value ? 'ERROR' : 'NORMAL',
      isActive: feedBoardStatusBitsFanPower.value,
      hasError: feedBoardStatusBitsFanError.value,
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
      feedBoardStatusBitsFanError.value && 'FAN',
    ].filter(Boolean),
    // 활성화된 상태 목록
    activeStatuses: [
      feedXBoardStatusLNALHCPPower.value && 'LNA_LHCP_ON',
      feedXBoardStatusLNARHCPPower.value && 'LNA_RHCP_ON',
      feedBoardStatusBitsFanPower.value && 'FAN_ON',
    ].filter(Boolean),
    // 전체 상태 요약
    summary: {
      totalActiveLNAs: [
        feedXBoardStatusLNALHCPPower.value,
        feedXBoardStatusLNARHCPPower.value,
      ].filter(Boolean).length,
      totalErrors: [
        feedXBoardStatusLNALHCPError.value,
        feedXBoardStatusLNARHCPError.value,
        feedBoardStatusBitsFanError.value,
      ].filter(Boolean).length,
      hasAnyLNAActive: feedXBoardStatusLNALHCPPower.value || feedXBoardStatusLNARHCPPower.value,
      isFanActive: feedBoardStatusBitsFanPower.value,
      hasAnyError:
        feedXBoardStatusLNALHCPError.value ||
        feedXBoardStatusLNARHCPError.value ||
        feedBoardStatusBitsFanError.value,
      // 전체 상태 판단
      overallStatus:
        feedXBoardStatusLNALHCPError.value ||
        feedXBoardStatusLNARHCPError.value ||
        feedBoardStatusBitsFanError.value
          ? 'ERROR'
          : feedXBoardStatusLNALHCPPower.value ||
              feedXBoardStatusLNARHCPPower.value ||
              feedBoardStatusBitsFanPower.value
            ? 'ACTIVE'
            : 'STANDBY',
    },
  }))
  // 타이머 관련 상태

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
  // WebSocket 메시지 핸들러 - 데이터를 버퍼에만 저장
  const handleWebSocketMessage = async (message: MessageData) => {
    try {
      console.log('🔍 [Frontend] WebSocket 메시지 수신:', message)
      console.log('🔍 [Frontend] 메시지 구조 분석:', {
        hasServerTime: 'serverTime' in message,
        serverTimeValue: message.serverTime,
        serverTimeType: typeof message.serverTime,
        messageType: typeof message,
        messageKeys: Object.keys(message),
      })

      // 받은 데이터를 버퍼에 저장만 하고 즉시 UI 업데이트하지 않음
      latestDataBuffer.value = message
      bufferUpdateTime.value = Date.now()

      // ✅ 에러 데이터 처리 (새로운 구조)
      try {
        if (message.data && typeof message.data === 'object' && 'errorData' in message.data) {
          const errorData = (message.data as Record<string, unknown>).errorData
          console.log('🔍 에러 데이터 수신:', errorData)

          if (errorData && typeof errorData === 'object') {
            const errorDataObj = errorData as Record<string, unknown>
            
            // 상태바 데이터 업데이트 (항상)
            if ('statusBarData' in errorDataObj) {
              errorStatusBarData.value = errorDataObj.statusBarData as {
                activeErrorCount: number
                latestError: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  message: { ko: string; en: string }
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                  resolvedMessage: { ko: string; en: string } | null
                } | null
                hasNewErrors: boolean
              }
              
              // 하드웨어 에러 로그 스토어에 상태바 데이터 반영
            const { useHardwareErrorLogStore } = await import('@/stores/hardwareErrorLogStore')
            const hardwareErrorLogStore = useHardwareErrorLogStore()

              const statusBarData = errorDataObj.statusBarData as {
                activeErrorCount: number
                latestError: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  message: { ko: string; en: string }
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                  resolvedMessage: { ko: string; en: string } | null
                } | null
                hasNewErrors: boolean
              }
              if (statusBarData?.hasNewErrors && statusBarData?.latestError) {
                hardwareErrorLogStore.addErrorLog(statusBarData.latestError as unknown as HardwareErrorLog)
              }
            }
            
            // 팝업 데이터 업데이트 (팝업이 열려있을 때만)
            if ('popupData' in errorDataObj) {
              errorPopupData.value = errorDataObj.popupData as {
                isInitialLoad: boolean
                newLogs: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  message: { ko: string; en: string }
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                  resolvedMessage: { ko: string; en: string } | null
                }[]
                totalLogCount: number
                lastUpdateTime: number
              }
              
              const { useHardwareErrorLogStore } = await import('@/stores/hardwareErrorLogStore')
              const hardwareErrorLogStore = useHardwareErrorLogStore()
              
              const popupData = errorDataObj.popupData as {
                isInitialLoad: boolean
                newLogs: {
                  id: string
                  timestamp: string
                  category: string
                  severity: string
                  message: { ko: string; en: string }
                  component: string
                  isResolved: boolean
                  resolvedAt: string | null
                  resolvedMessage: { ko: string; en: string } | null
                }[]
                totalLogCount: number
                lastUpdateTime: number
              }
              if (popupData?.newLogs && Array.isArray(popupData.newLogs)) {
                hardwareErrorLogStore.addNewLogs(popupData.newLogs as unknown as HardwareErrorLog[])
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

    // 디버깅 로그 (가끔씩만)
    if (Math.random() < 0.1) {
      // 10% 확률
      console.log(
        `🔧 간격 조정: 평균처리시간 ${avgProcessingTime.toFixed(2)}ms, 목표 ${targetInterval}ms, 적응간격 ${adaptiveInterval.value}ms`,
      )
    }
  }

  // 30ms 타이머로 실행되는 UI 업데이트 함수
  const updateUIFromBuffer = () => {
    try {
      const startTime = performance.now()

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
          const oldTime = serverTime.value
          serverTime.value = safeToString(dataServerTime)

          // 100번마다 로그
          if (updateCount.value % 100 === 0) {
            console.log(`🕐 [${updateCount.value}] serverTime: ${oldTime} → ${serverTime.value}`)
          }
        }
      } else {
        console.log('❌ [Frontend] serverTime을 찾을 수 없습니다:', {
          messageServerTime: message.serverTime,
          messageData: message.data,
          hasData: !!message.data,
          dataKeys: message.data ? Object.keys(message.data) : 'no data',
        })
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

      // 명령 데이터 업데이트
      if (message.cmdAzimuthAngle !== undefined) {
        cmdAzimuthAngle.value = safeToString(message.cmdAzimuthAngle)
      }

      if (message.cmdElevationAngle !== undefined) {
        cmdElevationAngle.value = safeToString(message.cmdElevationAngle)
      }

      if (message.cmdTrainAngle !== undefined) {
        cmdTrainAngle.value = safeToString(message.cmdTrainAngle)
      }

      // 🆕 추적 스케줄 정보 업데이트
      if (message.currentTrackingMstId !== undefined) {
        const newCurrentMstId = message.currentTrackingMstId as number | null
        if (currentTrackingMstId.value !== newCurrentMstId) {
          console.log(`📋 현재 추적 MstId 변경: ${currentTrackingMstId.value} → ${newCurrentMstId}`)
          currentTrackingMstId.value = newCurrentMstId
        }
      }

      if (message.nextTrackingMstId !== undefined) {
        const newNextMstId = message.nextTrackingMstId as number | null
        if (nextTrackingMstId.value !== newNextMstId) {
          console.log(`📋 다음 추적 MstId 변경: ${nextTrackingMstId.value} → ${newNextMstId}`)
          nextTrackingMstId.value = newNextMstId
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

      // 추적 상태 데이터 업데이트
      if (message.trackingStatus && typeof message.trackingStatus === 'object') {
        updataTrackingStatus(message.trackingStatus)
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

      // 성능 통계 (1초마다)
      if (updateCount.value % Math.floor(1000 / UPDATE_INTERVAL) === 0) {
        console.log(
          `📊 UI 업데이트 통계: ${updateCount.value}회, 처리시간: ${messageDelay.value.toFixed(2)}ms, 간격: ${updateInterval.value.toFixed(2)}ms, 적응간격: ${adaptiveInterval.value}ms`,
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

      // ✅ 새로 추가: Ephemeris 추적 상태 업데이트
      if (trackingStatusData.ephemerisTrackingState !== undefined) {
        const newState = trackingStatusData.ephemerisTrackingState as string | null
        if (ephemerisTrackingState.value !== newState) {
          ephemerisTrackingState.value = newState
          console.log(' Ephemeris 추적 상태 업데이트:', newState)
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

      // ✅ 새로 추가: Sun Track 추적 상태 업데이트
      if (trackingStatusData.sunTrackTrackingState !== undefined) {
        const newState = trackingStatusData.sunTrackTrackingState as string | null
        if (sunTrackTrackingState.value !== newState) {
          console.log('☀️ Sun Track 추적 상태 변경 감지:', {
            이전상태: sunTrackTrackingState.value,
            새상태: newState,
            전체데이터: trackingStatusData,
          })
          sunTrackTrackingState.value = newState
          console.log('☀️ Sun Track 추적 상태 업데이트 완료:', newState)
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

      // Feed 보드 상태
      if (
        antennaData.feedSBoardStatusBits !== undefined &&
        antennaData.feedSBoardStatusBits !== null
      ) {
        const newBitString = safeToString(antennaData.feedSBoardStatusBits)
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

  // WebSocket 연결 설정
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
        return { displayLabel: '대기(위성 추적 정지)', displayColor: 'grey' }
      case 'TRAIN_MOVING_TO_ZERO':
        return { displayLabel: 'Train 시작 위치로 이동', displayColor: 'deep-orange' }
      case 'TRAIN_STABILIZING':
        return { displayLabel: 'Train 안정화 대기', displayColor: 'amber-7' }
      case 'MOVING_TO_START':
        return { displayLabel: '시작 위치 이동', displayColor: 'blue' }
      case 'WAITING_FOR_TRACKING':
        return { displayLabel: '위성 추적 대기', displayColor: 'cyan' }
      case 'TRACKING':
        return { displayLabel: '추적 중', displayColor: 'green' }
      case 'COMPLETED':
        return { displayLabel: '완료', displayColor: 'purple' }
      case 'ERROR':
        return { displayLabel: '오류', displayColor: 'red' }
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
        return { displayLabel: '대기', displayColor: 'grey' }
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
    feedSBoardStatusBits,
    feedXBoardStatusBits,

    // LNA 및 RSSI 데이터
    currentSBandLNALHCP,
    currentSBandLNARHCP,
    currentXBandLNALHCP,
    currentXBandLNARHCP,
    rssiSBandLNALHCP,
    rssiSBandLNARHCP,
    rssiXBandLNALHCP,
    rssiXBandLNARHCP,

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
    feedSBoardStatusInfo,
    feedXBoardStatusInfo,

    // 모드 상태 정보
    ephemerisStatus,
    ephemerisStatusInfo,
    ephemerisTrackingState,
    ephemerisTrackingStateInfo,
    passScheduleStatus,
    passScheduleStatusInfo,
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
    nextTrackingMstId: readonly(nextTrackingMstId),
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
  }
})
