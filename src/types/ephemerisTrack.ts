export interface EphemerisTrackRequest {
  tleLine1: string
  tleLine2: string
  startTime: string
  endTime: string
  stepSize: number
}

export interface ScheduleItem {
  mstId: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string

  // 2축 (Original) 속도
  OriginalMaxAzRate?: number
  OriginalMaxElRate?: number
  OriginalMaxElevation?: number

  // Train=0 (FinalTransformed) 속도
  FinalTransformedMaxAzRate: number
  FinalTransformedMaxElRate: number

  // TrainOK (KeyholeFinalTransformed) 속도
  KeyholeFinalTransformedMaxAzRate: number
  KeyholeFinalTransformedMaxElRate: number

  /**
   * KEYHOLE 위성 여부
   * MaxAzRate가 임계값 이상인 경우 true
   */
  isKeyhole: boolean

  /**
   * KEYHOLE 위성일 경우 권장 Train 각도 (도)
   * 최대 Elevation 지점의 Azimuth 각도
   */
  recommendedTrainAngle: number

  // Legacy fields (deprecated - use new fields above)
  IsKeyhole?: boolean
  RecommendedTrainAngle?: number
  MaxAzRate?: number
  MaxElRate?: number
}

export interface ScheduleDetailItem {
  id: number
  mstId: number
  timestamp: string
  azimuth: number
  elevation: number
  range: number
  rangeRate: number
  x: number
  y: number
  z: number
  vx: number
  vy: number
  vz: number

  /**
   * 필터링 기준으로 사용되는 Elevation 값
   * displayMinElevationAngle과 비교하여 표시 여부 결정
   */
  Elevation: number
}

/**
 * 실시간 추적 데이터 아이템
 */
export interface RealtimeTrackingDataItem {
  timestamp: string
  azimuth: number
  elevation: number
  range: number
  altitude: number

  /**
   * KEYHOLE 위성 여부
   */
  IsKeyhole?: boolean

  /**
   * 권장 Train 각도 (도)
   */
  RecommendedTrainAngle?: number

  /**
   * 최대 Azimuth 각속도 (도/초)
   */
  MaxAzRate?: number

  /**
   * 최대 Elevation 각속도 (도/초)
   */
  MaxElRate?: number
}

/**
 * KEYHOLE 위성 정보 인터페이스
 */
export interface KeyholeInfo {
  /**
   * KEYHOLE 위성 여부
   */
  IsKeyhole: boolean

  /**
   * 권장 Train 각도 (도)
   */
  RecommendedTrainAngle: number

  /**
   * 최대 Azimuth 각속도 (도/초)
   */
  MaxAzRate: number

  /**
   * 최대 Elevation 각속도 (도/초)
   */
  MaxElRate: number

  /**
   * KEYHOLE 판단 임계값 (도/초)
   */
  threshold: number
}

export interface TLEData {
  line1: string
  line2: string
  name: string
}

export interface TLEParseError {
  message: string
  code: string
  details?: string
}

export interface ApiError {
  message: string
  status?: number
  details?: string
}

export interface ParseTLEResult {
  name: string
  line1: string
  line2: string
  noradId: string
  classification: string
  launchYear: string
  launchNumber: string
  launchPiece: string
  epochYear: string
  epochDay: string
  meanMotionFirstDerivative: string
  meanMotionSecondDerivative: string
  bstarDragTerm: string
  ephemerisType: string
  elementSetNumber: string
  inclination: string
  rightAscensionOfAscendingNode: string
  eccentricity: string
  argumentOfPerigee: string
  meanAnomaly: string
  meanMotion: string
  revolutionNumberAtEpoch: string
}
