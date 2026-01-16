/**
 * ICD Store 타입 정의
 * @description 안테나 제어 시스템의 ICD 프로토콜 관련 타입들
 */

// ============================================
// 프로토콜 상태 비트
// ============================================

export interface ProtocolStatusBits {
  elevation: boolean
  azimuth: boolean
  train: boolean
  feed: boolean
  reserve1: boolean
  reserve2: boolean
  reserve3: boolean
  defaultReceive: boolean
}

// ============================================
// 메인보드 상태 비트
// ============================================

export interface MainBoardStatusBits {
  powerSurgeProtector: boolean
  powerReversePhaseSensor: boolean
  emergencyStopACU: boolean
  emergencyStopPositioner: boolean
  reserve1: boolean
  reserve2: boolean
  reserve3: boolean
  reserve4: boolean
}

export interface MainBoardMCOnOffBits {
  mcTrain: boolean
  mcElevation: boolean
  mcAzimuth: boolean
  reserve1: boolean
  reserve2: boolean
  reserve3: boolean
  reserve4: boolean
  reserve5: boolean
}

// ============================================
// 서보 상태 비트 (공통 구조)
// ============================================

export interface ServoStatusBits {
  servoAlarmCode1: boolean
  servoAlarmCode2: boolean
  servoAlarmCode3: boolean
  servoAlarmCode4: boolean
  servoAlarmCode5: boolean
  servoAlarm: boolean
  servoBrake: boolean
  servoMotor: boolean
}

// ============================================
// Azimuth 보드 상태 비트
// ============================================

export interface AzimuthBoardStatusBits {
  limitSwitchPositive275: boolean
  limitSwitchNegative275: boolean
  reserve1: boolean
  reserve2: boolean
  stowPin: boolean
  reserve3: boolean
  reserve4: boolean
  encoder: boolean
}

// ============================================
// Elevation 보드 상태 비트
// ============================================

export interface ElevationBoardStatusBits {
  limitSwitchPositive180: boolean
  limitSwitchPositive185: boolean
  limitSwitchNegative0: boolean
  limitSwitchNegative5: boolean
  stowPin: boolean
  reserve1: boolean
  reserve2: boolean
  encoder: boolean
}

// ============================================
// Train 보드 상태 비트
// ============================================

export interface TrainBoardStatusBits {
  limitSwitchPositive275: boolean
  limitSwitchNegative275: boolean
  reserve1: boolean
  reserve2: boolean
  stowPin: boolean
  reserve3: boolean
  reserve4: boolean
  encoder: boolean
}

// ============================================
// Feed 보드 상태 비트
// ============================================

export interface FeedSBoardStatusBits {
  lnaLHCPPower: boolean
  lnaLHCPError: boolean
  lnaRHCPPower: boolean
  lnaRHCPError: boolean
  reserve1: boolean
  reserve2: boolean
  rfSwitchMode: boolean
  rfSwitchError: boolean
}

export interface FeedXBoardStatusBits {
  lnaLHCPPower: boolean
  lnaLHCPError: boolean
  lnaRHCPPower: boolean
  lnaRHCPError: boolean
  fanPower: boolean
  fanError: boolean
}

export interface FeedBoardETCStatusBits {
  rfSwitchMode: boolean
  rfSwitchError: boolean
  fanPower: boolean
  fanError: boolean
}

export interface FeedKaBoardStatusBits {
  lnaLHCPPower: boolean
  lnaLHCPError: boolean
  lnaRHCPPower: boolean
  lnaRHCPError: boolean
  selectionLHCPBand: boolean
  selectionLHCPError: boolean
  selectionRHCPBand: boolean
  selectionRHCPError: boolean
}

// ============================================
// 안테나 상태 (그룹화)
// ============================================

export interface AntennaPosition {
  azimuth: string
  elevation: string
  train: string
}

export interface AntennaSpeed {
  azimuth: string
  elevation: string
  train: string
}

export interface AntennaTorque {
  azimuth: string
  elevation: string
  train: string
}

export interface AntennaCommand {
  azimuth: string
  elevation: string
  train: string
  time: string
}

// ============================================
// 추적 상태
// ============================================

export interface TrackingStateInfo {
  currentMstId: string
  currentDetailId: string
  currentSatelliteName: string
  nextMstId: string
  nextDetailId: string
  nextSatelliteName: string
}

// ============================================
// 환경 데이터
// ============================================

export interface EnvironmentData {
  windSpeed: string
  windDirection: string
  rtdOne: string
  rtdTwo: string
}
