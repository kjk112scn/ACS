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
