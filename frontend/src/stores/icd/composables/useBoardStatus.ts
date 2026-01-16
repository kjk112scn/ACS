/**
 * 보드 상태 비트 파싱 함수들
 * @description ICD 프로토콜의 비트 문자열을 파싱하는 순수 함수들
 */

import type {
  ProtocolStatusBits,
  MainBoardStatusBits,
  MainBoardMCOnOffBits,
  ServoStatusBits,
  AzimuthBoardStatusBits,
  ElevationBoardStatusBits,
  TrainBoardStatusBits,
  FeedSBoardStatusBits,
  FeedXBoardStatusBits,
  FeedBoardETCStatusBits,
  FeedKaBoardStatusBits,
} from '../types/icdTypes'

/**
 * 비트 문자열을 배열로 변환하는 헬퍼 함수
 * @param bitString - 비트 문자열 (예: "00001000")
 * @returns 오른쪽부터 시작하는 비트 배열
 */
const parseBitString = (bitString: string): string[] => {
  return bitString.padStart(8, '0').split('').reverse()
}

/**
 * 프로토콜 상태 비트 파싱
 * @description 메인보드 프로토콜 상태 비트를 파싱
 */
export function parseProtocolStatusBits(bitString: string): ProtocolStatusBits {
  const bits = parseBitString(bitString)
  return {
    elevation: bits[0] === '1',
    azimuth: bits[1] === '1',
    train: bits[2] === '1',
    feed: bits[3] === '1',
    reserve1: bits[4] === '1',
    reserve2: bits[5] === '1',
    reserve3: bits[6] === '1',
    defaultReceive: bits[7] === '1',
  }
}

/**
 * 메인보드 상태 비트 파싱
 */
export function parseMainBoardStatusBits(bitString: string): MainBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    powerSurgeProtector: bits[0] === '1',
    powerReversePhaseSensor: bits[1] === '1',
    emergencyStopACU: bits[2] === '1',
    emergencyStopPositioner: bits[3] === '1',
    reserve1: bits[4] === '1',
    reserve2: bits[5] === '1',
    reserve3: bits[6] === '1',
    reserve4: bits[7] === '1',
  }
}

/**
 * 메인보드 MC On/Off 비트 파싱
 */
export function parseMainBoardMCOnOffBits(bitString: string): MainBoardMCOnOffBits {
  const bits = parseBitString(bitString)
  return {
    mcTrain: bits[0] === '1',
    mcElevation: bits[1] === '1',
    mcAzimuth: bits[2] === '1',
    reserve1: bits[3] === '1',
    reserve2: bits[4] === '1',
    reserve3: bits[5] === '1',
    reserve4: bits[6] === '1',
    reserve5: bits[7] === '1',
  }
}

/**
 * 서보 상태 비트 파싱 (공통)
 * @description Azimuth, Elevation, Train 서보 상태 비트에 공통으로 사용
 */
export function parseServoStatusBits(bitString: string): ServoStatusBits {
  const bits = parseBitString(bitString)
  return {
    servoAlarmCode1: bits[0] === '1',
    servoAlarmCode2: bits[1] === '1',
    servoAlarmCode3: bits[2] === '1',
    servoAlarmCode4: bits[3] === '1',
    servoAlarmCode5: bits[4] === '1',
    servoAlarm: bits[5] === '1',
    servoBrake: bits[6] === '1',
    servoMotor: bits[7] === '1',
  }
}

/**
 * Azimuth 보드 상태 비트 파싱
 */
export function parseAzimuthBoardStatusBits(bitString: string): AzimuthBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    limitSwitchPositive275: bits[0] === '1',
    limitSwitchNegative275: bits[1] === '1',
    reserve1: bits[2] === '1',
    reserve2: bits[3] === '1',
    stowPin: bits[4] === '1',
    reserve3: bits[5] === '1',
    reserve4: bits[6] === '1',
    encoder: bits[7] === '1',
  }
}

/**
 * Elevation 보드 상태 비트 파싱
 */
export function parseElevationBoardStatusBits(bitString: string): ElevationBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    limitSwitchPositive180: bits[0] === '1',
    limitSwitchPositive185: bits[1] === '1',
    limitSwitchNegative0: bits[2] === '1',
    limitSwitchNegative5: bits[3] === '1',
    stowPin: bits[4] === '1',
    reserve1: bits[5] === '1',
    reserve2: bits[6] === '1',
    encoder: bits[7] === '1',
  }
}

/**
 * Train 보드 상태 비트 파싱
 */
export function parseTrainBoardStatusBits(bitString: string): TrainBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    limitSwitchPositive275: bits[0] === '1',
    limitSwitchNegative275: bits[1] === '1',
    reserve1: bits[2] === '1',
    reserve2: bits[3] === '1',
    stowPin: bits[4] === '1',
    reserve3: bits[5] === '1',
    reserve4: bits[6] === '1',
    encoder: bits[7] === '1',
  }
}

/**
 * Feed S-Band 상태 비트 파싱
 * @description ICD 문서: Bits 15-8
 */
export function parseFeedSBoardStatusBits(bitString: string): FeedSBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    lnaLHCPPower: bits[0] === '1',
    lnaLHCPError: bits[1] === '1',
    lnaRHCPPower: bits[2] === '1',
    lnaRHCPError: bits[3] === '1',
    reserve1: false,
    reserve2: false,
    rfSwitchMode: false,
    rfSwitchError: false,
  }
}

/**
 * Feed X-Band 상태 비트 파싱
 * @description ICD 문서: Bits 23-16
 */
export function parseFeedXBoardStatusBits(bitString: string): FeedXBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    lnaLHCPPower: bits[0] === '1',
    lnaLHCPError: bits[1] === '1',
    lnaRHCPPower: bits[2] === '1',
    lnaRHCPError: bits[3] === '1',
    fanPower: false,
    fanError: false,
  }
}

/**
 * Feed ETC 상태 비트 파싱
 * @description ICD 문서: Bits 7-0
 */
export function parseFeedBoardETCStatusBits(bitString: string): FeedBoardETCStatusBits {
  const bits = parseBitString(bitString)
  return {
    rfSwitchMode: bits[0] === '1',
    rfSwitchError: bits[1] === '1',
    fanPower: bits[2] === '1',
    fanError: bits[3] === '1',
  }
}

/**
 * Feed Ka-Band 상태 비트 파싱
 * @description ICD 문서: Bits 31-24
 */
export function parseFeedKaBoardStatusBits(bitString: string): FeedKaBoardStatusBits {
  const bits = parseBitString(bitString)
  return {
    lnaLHCPPower: bits[0] === '1',
    lnaLHCPError: bits[1] === '1',
    lnaRHCPPower: bits[2] === '1',
    lnaRHCPError: bits[3] === '1',
    selectionLHCPBand: bits[4] === '1',
    selectionLHCPError: bits[5] === '1',
    selectionRHCPBand: bits[6] === '1',
    selectionRHCPError: bits[7] === '1',
  }
}
