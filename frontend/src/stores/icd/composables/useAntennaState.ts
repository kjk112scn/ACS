/**
 * 안테나 상태 업데이트 로직
 * @description 안테나 데이터 업데이트를 위한 순수 함수 및 헬퍼
 */

// ============================================
// 타입 정의
// ============================================

/**
 * 안테나 데이터 입력 (WebSocket에서 수신)
 */
export interface AntennaDataInput {
  // 기본 안테나 데이터
  modeStatusBits?: unknown
  azimuthAngle?: unknown
  elevationAngle?: unknown
  trainAngle?: unknown
  azimuthSpeed?: unknown
  elevationSpeed?: unknown
  trainSpeed?: unknown

  // 서보 드라이버 데이터
  servoDriverAzimuthAngle?: unknown
  servoDriverElevationAngle?: unknown
  servoDriverTrainAngle?: unknown

  // 토크 데이터
  torqueAzimuth?: unknown
  torqueElevation?: unknown
  torqueTrain?: unknown

  // 환경 데이터
  windSpeed?: unknown
  windDirection?: unknown
  rtdOne?: unknown
  rtdTwo?: unknown

  // 메인 보드 상태 비트
  mainBoardProtocolStatusBits?: unknown
  mainBoardStatusBits?: unknown
  mainBoardMCOnOffBits?: unknown
  mainBoardReserveBits?: unknown

  // Azimuth 보드 상태
  azimuthBoardServoStatusBits?: unknown
  azimuthBoardStatusBits?: unknown

  // Elevation 보드 상태
  elevationBoardServoStatusBits?: unknown
  elevationBoardStatusBits?: unknown

  // Train 보드 상태
  trainBoardServoStatusBits?: unknown
  trainBoardStatusBits?: unknown

  // Feed 보드 상태
  feedBoardETCStatusBits?: unknown
  feedSBoardStatusBits?: unknown
  feedXBoardStatusBits?: unknown
  feedKaBoardStatusBits?: unknown

  // LNA 전류 데이터
  currentSBandLNALHCP?: unknown
  currentSBandLNARHCP?: unknown
  currentXBandLNALHCP?: unknown
  currentXBandLNARHCP?: unknown
  currentKaBandLNALHCP?: unknown
  currentKaBandLNARHCP?: unknown

  // RSSI 데이터
  rssiSBandLNALHCP?: unknown
  rssiSBandLNARHCP?: unknown
  rssiXBandLNALHCP?: unknown
  rssiXBandLNARHCP?: unknown
  rssiKaBandLNALHCP?: unknown
  rssiKaBandLNARHCP?: unknown

  // 가속도 데이터
  azimuthAcceleration?: unknown
  elevationAcceleration?: unknown
  trainAcceleration?: unknown
  azimuthMaxAcceleration?: unknown
  elevationMaxAcceleration?: unknown
  trainMaxAcceleration?: unknown

  // 추적 데이터
  trackingAzimuthTime?: unknown
  trackingCMDAzimuthAngle?: unknown
  trackingActualAzimuthAngle?: unknown
  trackingElevationTime?: unknown
  trackingCMDElevationAngle?: unknown
  trackingActualElevationAngle?: unknown
  trackingTrainTime?: unknown
  trackingCMDTrainAngle?: unknown
  trackingActualTrainAngle?: unknown
}

/**
 * 문자열 필드 업데이트 결과
 */
export interface AntennaStringFieldsUpdate {
  modeStatusBits?: string
  azimuthAngle?: string
  elevationAngle?: string
  trainAngle?: string
  azimuthSpeed?: string
  elevationSpeed?: string
  trainSpeed?: string
  servoDriverAzimuthAngle?: string
  servoDriverElevationAngle?: string
  servoDriverTrainAngle?: string
  torqueAzimuth?: string
  torqueElevation?: string
  torqueTrain?: string
  windSpeed?: string
  windDirection?: string
  rtdOne?: string
  rtdTwo?: string
  mainBoardReserveBits?: string
  currentSBandLNALHCP?: string
  currentSBandLNARHCP?: string
  currentXBandLNALHCP?: string
  currentXBandLNARHCP?: string
  currentKaBandLNALHCP?: string
  currentKaBandLNARHCP?: string
  rssiSBandLNALHCP?: string
  rssiSBandLNARHCP?: string
  rssiXBandLNALHCP?: string
  rssiXBandLNARHCP?: string
  rssiKaBandLNALHCP?: string
  rssiKaBandLNARHCP?: string
  azimuthAcceleration?: string
  elevationAcceleration?: string
  trainAcceleration?: string
  azimuthMaxAcceleration?: string
  elevationMaxAcceleration?: string
  trainMaxAcceleration?: string
  trackingAzimuthTime?: string
  trackingCMDAzimuthAngle?: string
  trackingActualAzimuthAngle?: string
  trackingElevationTime?: string
  trackingCMDElevationAngle?: string
  trackingActualElevationAngle?: string
  trackingTrainTime?: string
  trackingCMDTrainAngle?: string
  trackingActualTrainAngle?: string
}

/**
 * 비트 문자열 필드 업데이트 결과 (파싱 필요)
 */
export interface AntennaBitFieldsUpdate {
  mainBoardProtocolStatusBits?: string
  mainBoardStatusBits?: string
  mainBoardMCOnOffBits?: string
  azimuthBoardServoStatusBits?: string
  azimuthBoardStatusBits?: string
  elevationBoardServoStatusBits?: string
  elevationBoardStatusBits?: string
  trainBoardServoStatusBits?: string
  trainBoardStatusBits?: string
  feedBoardETCStatusBits?: string
  feedSBoardStatusBits?: string
  feedXBoardStatusBits?: string
  feedKaBoardStatusBits?: string
}

/**
 * 안테나 위치 정보 (그룹화)
 */
export interface AntennaPositionInfo {
  azimuth: string
  elevation: string
  train: string
}

/**
 * 안테나 속도 정보 (그룹화)
 */
export interface AntennaSpeedInfo {
  azimuth: string
  elevation: string
  train: string
}

/**
 * 안테나 토크 정보 (그룹화)
 */
export interface AntennaTorqueInfo {
  azimuth: string
  elevation: string
  train: string
}

/**
 * 환경 데이터 정보 (그룹화)
 */
export interface EnvironmentInfo {
  windSpeed: string
  windDirection: string
  rtdOne: string
  rtdTwo: string
}

// ============================================
// 헬퍼 함수
// ============================================

/**
 * 값을 안전하게 문자열로 변환
 * @param value - 변환할 값
 * @returns 문자열
 */
export function safeToString(value: unknown): string {
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
      if (value && typeof value === 'object' && 'toString' in value) {
        const toStringResult = (value as { toString(): string }).toString()
        if (toStringResult !== '[object Object]') {
          return toStringResult
        }
      }
      return `[${typeof value}]`
    }
  }
  return `[${typeof value}]`
}

/**
 * 유효한 값인지 확인
 * @param value - 확인할 값
 * @returns 유효 여부
 */
export function isValidValue(value: unknown): boolean {
  return value !== undefined && value !== null
}

// ============================================
// 데이터 파싱 함수
// ============================================

/**
 * 단순 문자열 필드들 파싱
 * @description 비트 파싱이 필요 없는 단순 값들을 추출
 * @param input - 안테나 데이터 입력
 * @returns 문자열 필드 업데이트 객체
 */
export function parseAntennaStringFields(input: AntennaDataInput): AntennaStringFieldsUpdate {
  const updates: AntennaStringFieldsUpdate = {}

  // 기본 안테나 데이터
  if (isValidValue(input.modeStatusBits)) {
    updates.modeStatusBits = safeToString(input.modeStatusBits)
  }
  if (isValidValue(input.azimuthAngle)) {
    updates.azimuthAngle = safeToString(input.azimuthAngle)
  }
  if (isValidValue(input.elevationAngle)) {
    updates.elevationAngle = safeToString(input.elevationAngle)
  }
  if (isValidValue(input.trainAngle)) {
    updates.trainAngle = safeToString(input.trainAngle)
  }
  if (isValidValue(input.azimuthSpeed)) {
    updates.azimuthSpeed = safeToString(input.azimuthSpeed)
  }
  if (isValidValue(input.elevationSpeed)) {
    updates.elevationSpeed = safeToString(input.elevationSpeed)
  }
  if (isValidValue(input.trainSpeed)) {
    updates.trainSpeed = safeToString(input.trainSpeed)
  }

  // 서보 드라이버 데이터
  if (isValidValue(input.servoDriverAzimuthAngle)) {
    updates.servoDriverAzimuthAngle = safeToString(input.servoDriverAzimuthAngle)
  }
  if (isValidValue(input.servoDriverElevationAngle)) {
    updates.servoDriverElevationAngle = safeToString(input.servoDriverElevationAngle)
  }
  if (isValidValue(input.servoDriverTrainAngle)) {
    updates.servoDriverTrainAngle = safeToString(input.servoDriverTrainAngle)
  }

  // 토크 데이터
  if (isValidValue(input.torqueAzimuth)) {
    updates.torqueAzimuth = safeToString(input.torqueAzimuth)
  }
  if (isValidValue(input.torqueElevation)) {
    updates.torqueElevation = safeToString(input.torqueElevation)
  }
  if (isValidValue(input.torqueTrain)) {
    updates.torqueTrain = safeToString(input.torqueTrain)
  }

  // 환경 데이터
  if (isValidValue(input.windSpeed)) {
    updates.windSpeed = safeToString(input.windSpeed)
  }
  if (isValidValue(input.windDirection)) {
    updates.windDirection = safeToString(input.windDirection)
  }
  if (isValidValue(input.rtdOne)) {
    updates.rtdOne = safeToString(input.rtdOne)
  }
  if (isValidValue(input.rtdTwo)) {
    updates.rtdTwo = safeToString(input.rtdTwo)
  }

  // 메인 보드 예약 비트
  if (isValidValue(input.mainBoardReserveBits)) {
    updates.mainBoardReserveBits = safeToString(input.mainBoardReserveBits)
  }

  // LNA 전류 데이터
  if (isValidValue(input.currentSBandLNALHCP)) {
    updates.currentSBandLNALHCP = safeToString(input.currentSBandLNALHCP)
  }
  if (isValidValue(input.currentSBandLNARHCP)) {
    updates.currentSBandLNARHCP = safeToString(input.currentSBandLNARHCP)
  }
  if (isValidValue(input.currentXBandLNALHCP)) {
    updates.currentXBandLNALHCP = safeToString(input.currentXBandLNALHCP)
  }
  if (isValidValue(input.currentXBandLNARHCP)) {
    updates.currentXBandLNARHCP = safeToString(input.currentXBandLNARHCP)
  }
  if (isValidValue(input.currentKaBandLNALHCP)) {
    updates.currentKaBandLNALHCP = safeToString(input.currentKaBandLNALHCP)
  }
  if (isValidValue(input.currentKaBandLNARHCP)) {
    updates.currentKaBandLNARHCP = safeToString(input.currentKaBandLNARHCP)
  }

  // RSSI 데이터
  if (isValidValue(input.rssiSBandLNALHCP)) {
    updates.rssiSBandLNALHCP = safeToString(input.rssiSBandLNALHCP)
  }
  if (isValidValue(input.rssiSBandLNARHCP)) {
    updates.rssiSBandLNARHCP = safeToString(input.rssiSBandLNARHCP)
  }
  if (isValidValue(input.rssiXBandLNALHCP)) {
    updates.rssiXBandLNALHCP = safeToString(input.rssiXBandLNALHCP)
  }
  if (isValidValue(input.rssiXBandLNARHCP)) {
    updates.rssiXBandLNARHCP = safeToString(input.rssiXBandLNARHCP)
  }
  if (isValidValue(input.rssiKaBandLNALHCP)) {
    updates.rssiKaBandLNALHCP = safeToString(input.rssiKaBandLNALHCP)
  }
  if (isValidValue(input.rssiKaBandLNARHCP)) {
    updates.rssiKaBandLNARHCP = safeToString(input.rssiKaBandLNARHCP)
  }

  // 가속도 데이터
  if (isValidValue(input.azimuthAcceleration)) {
    updates.azimuthAcceleration = safeToString(input.azimuthAcceleration)
  }
  if (isValidValue(input.elevationAcceleration)) {
    updates.elevationAcceleration = safeToString(input.elevationAcceleration)
  }
  if (isValidValue(input.trainAcceleration)) {
    updates.trainAcceleration = safeToString(input.trainAcceleration)
  }
  if (isValidValue(input.azimuthMaxAcceleration)) {
    updates.azimuthMaxAcceleration = safeToString(input.azimuthMaxAcceleration)
  }
  if (isValidValue(input.elevationMaxAcceleration)) {
    updates.elevationMaxAcceleration = safeToString(input.elevationMaxAcceleration)
  }
  if (isValidValue(input.trainMaxAcceleration)) {
    updates.trainMaxAcceleration = safeToString(input.trainMaxAcceleration)
  }

  // 추적 데이터
  if (isValidValue(input.trackingAzimuthTime)) {
    updates.trackingAzimuthTime = safeToString(input.trackingAzimuthTime)
  }
  if (isValidValue(input.trackingCMDAzimuthAngle)) {
    updates.trackingCMDAzimuthAngle = safeToString(input.trackingCMDAzimuthAngle)
  }
  if (isValidValue(input.trackingActualAzimuthAngle)) {
    updates.trackingActualAzimuthAngle = safeToString(input.trackingActualAzimuthAngle)
  }
  if (isValidValue(input.trackingElevationTime)) {
    updates.trackingElevationTime = safeToString(input.trackingElevationTime)
  }
  if (isValidValue(input.trackingCMDElevationAngle)) {
    updates.trackingCMDElevationAngle = safeToString(input.trackingCMDElevationAngle)
  }
  if (isValidValue(input.trackingActualElevationAngle)) {
    updates.trackingActualElevationAngle = safeToString(input.trackingActualElevationAngle)
  }
  if (isValidValue(input.trackingTrainTime)) {
    updates.trackingTrainTime = safeToString(input.trackingTrainTime)
  }
  if (isValidValue(input.trackingCMDTrainAngle)) {
    updates.trackingCMDTrainAngle = safeToString(input.trackingCMDTrainAngle)
  }
  if (isValidValue(input.trackingActualTrainAngle)) {
    updates.trackingActualTrainAngle = safeToString(input.trackingActualTrainAngle)
  }

  return updates
}

/**
 * 비트 문자열 필드들 파싱
 * @description 비트 파싱이 필요한 필드들을 추출
 * @param input - 안테나 데이터 입력
 * @returns 비트 필드 업데이트 객체
 */
export function parseAntennaBitFields(input: AntennaDataInput): AntennaBitFieldsUpdate {
  const updates: AntennaBitFieldsUpdate = {}

  // 메인 보드 상태 비트
  if (isValidValue(input.mainBoardProtocolStatusBits)) {
    updates.mainBoardProtocolStatusBits = safeToString(input.mainBoardProtocolStatusBits)
  }
  if (isValidValue(input.mainBoardStatusBits)) {
    updates.mainBoardStatusBits = safeToString(input.mainBoardStatusBits)
  }
  if (isValidValue(input.mainBoardMCOnOffBits)) {
    updates.mainBoardMCOnOffBits = safeToString(input.mainBoardMCOnOffBits)
  }

  // Azimuth 보드 상태
  if (isValidValue(input.azimuthBoardServoStatusBits)) {
    updates.azimuthBoardServoStatusBits = safeToString(input.azimuthBoardServoStatusBits)
  }
  if (isValidValue(input.azimuthBoardStatusBits)) {
    updates.azimuthBoardStatusBits = safeToString(input.azimuthBoardStatusBits)
  }

  // Elevation 보드 상태
  if (isValidValue(input.elevationBoardServoStatusBits)) {
    updates.elevationBoardServoStatusBits = safeToString(input.elevationBoardServoStatusBits)
  }
  if (isValidValue(input.elevationBoardStatusBits)) {
    updates.elevationBoardStatusBits = safeToString(input.elevationBoardStatusBits)
  }

  // Train 보드 상태
  if (isValidValue(input.trainBoardServoStatusBits)) {
    updates.trainBoardServoStatusBits = safeToString(input.trainBoardServoStatusBits)
  }
  if (isValidValue(input.trainBoardStatusBits)) {
    updates.trainBoardStatusBits = safeToString(input.trainBoardStatusBits)
  }

  // Feed 보드 상태
  if (isValidValue(input.feedBoardETCStatusBits)) {
    updates.feedBoardETCStatusBits = safeToString(input.feedBoardETCStatusBits)
  }
  if (isValidValue(input.feedSBoardStatusBits)) {
    updates.feedSBoardStatusBits = safeToString(input.feedSBoardStatusBits)
  }
  if (isValidValue(input.feedXBoardStatusBits)) {
    updates.feedXBoardStatusBits = safeToString(input.feedXBoardStatusBits)
  }
  if (isValidValue(input.feedKaBoardStatusBits)) {
    updates.feedKaBoardStatusBits = safeToString(input.feedKaBoardStatusBits)
  }

  return updates
}

// ============================================
// 그룹화 헬퍼 함수
// ============================================

/**
 * 안테나 위치 정보 생성
 * @param updates - 문자열 필드 업데이트
 * @param current - 현재 값들
 * @returns 안테나 위치 정보
 */
export function createAntennaPosition(
  updates: AntennaStringFieldsUpdate,
  current: { azimuth: string; elevation: string; train: string },
): AntennaPositionInfo {
  return {
    azimuth: updates.azimuthAngle ?? current.azimuth,
    elevation: updates.elevationAngle ?? current.elevation,
    train: updates.trainAngle ?? current.train,
  }
}

/**
 * 안테나 속도 정보 생성
 * @param updates - 문자열 필드 업데이트
 * @param current - 현재 값들
 * @returns 안테나 속도 정보
 */
export function createAntennaSpeed(
  updates: AntennaStringFieldsUpdate,
  current: { azimuth: string; elevation: string; train: string },
): AntennaSpeedInfo {
  return {
    azimuth: updates.azimuthSpeed ?? current.azimuth,
    elevation: updates.elevationSpeed ?? current.elevation,
    train: updates.trainSpeed ?? current.train,
  }
}

/**
 * 안테나 토크 정보 생성
 * @param updates - 문자열 필드 업데이트
 * @param current - 현재 값들
 * @returns 안테나 토크 정보
 */
export function createAntennaTorque(
  updates: AntennaStringFieldsUpdate,
  current: { azimuth: string; elevation: string; train: string },
): AntennaTorqueInfo {
  return {
    azimuth: updates.torqueAzimuth ?? current.azimuth,
    elevation: updates.torqueElevation ?? current.elevation,
    train: updates.torqueTrain ?? current.train,
  }
}

/**
 * 환경 데이터 정보 생성
 * @param updates - 문자열 필드 업데이트
 * @param current - 현재 값들
 * @returns 환경 데이터 정보
 */
export function createEnvironmentInfo(
  updates: AntennaStringFieldsUpdate,
  current: { windSpeed: string; windDirection: string; rtdOne: string; rtdTwo: string },
): EnvironmentInfo {
  return {
    windSpeed: updates.windSpeed ?? current.windSpeed,
    windDirection: updates.windDirection ?? current.windDirection,
    rtdOne: updates.rtdOne ?? current.rtdOne,
    rtdTwo: updates.rtdTwo ?? current.rtdTwo,
  }
}
