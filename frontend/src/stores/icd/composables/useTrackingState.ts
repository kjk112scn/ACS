/**
 * 추적 상태 업데이트 로직
 * @description 추적 모드(Ephemeris, PassSchedule, SunTrack)의 상태 업데이트 순수 함수
 */

// ============================================
// 타입 정의
// ============================================

/**
 * 추적 상태 입력 데이터 (WebSocket에서 수신)
 */
export interface TrackingStatusInput {
  ephemerisStatus?: boolean | null
  ephemerisTrackingState?: string | null
  passScheduleStatus?: boolean | null
  passScheduleTrackingState?: string | null
  sunTrackStatus?: boolean | null
  sunTrackTrackingState?: string | null
}

/**
 * 추적 상태 업데이트 결과
 */
export interface TrackingStatusUpdate {
  ephemerisStatus?: boolean | null
  ephemerisTrackingState?: string | null
  passScheduleStatus?: boolean | null
  passScheduleTrackingState?: string | null
  sunTrackStatus?: boolean | null
  sunTrackTrackingState?: string | null
}

/**
 * 현재 추적 상태 (비교용)
 */
export interface CurrentTrackingState {
  ephemerisStatus: boolean | null
  ephemerisTrackingState: string | null
  passScheduleStatus: boolean | null
  passScheduleTrackingState: string | null
  sunTrackStatus: boolean | null
  sunTrackTrackingState: string | null
}

// ============================================
// 순수 함수
// ============================================

/**
 * 추적 상태 데이터를 파싱하여 업데이트할 필드만 반환
 * @description 변경된 값만 반환하여 불필요한 반응성 트리거 방지
 * @param input - WebSocket에서 수신한 추적 상태 데이터
 * @param current - 현재 스토어 상태
 * @returns 업데이트할 필드만 포함된 객체
 */
export function parseTrackingStatusUpdate(
  input: TrackingStatusInput,
  current: CurrentTrackingState,
): TrackingStatusUpdate {
  const updates: TrackingStatusUpdate = {}

  // Ephemeris 상태 업데이트
  if (input.ephemerisStatus !== undefined) {
    if (current.ephemerisStatus !== input.ephemerisStatus) {
      updates.ephemerisStatus = input.ephemerisStatus
    }
  }

  // Ephemeris 추적 상태 업데이트
  if (input.ephemerisTrackingState !== undefined) {
    if (current.ephemerisTrackingState !== input.ephemerisTrackingState) {
      updates.ephemerisTrackingState = input.ephemerisTrackingState
    }
  }

  // Pass Schedule 상태 업데이트
  if (input.passScheduleStatus !== undefined) {
    if (current.passScheduleStatus !== input.passScheduleStatus) {
      updates.passScheduleStatus = input.passScheduleStatus
    }
  }

  // Pass Schedule 추적 상태 업데이트
  if (input.passScheduleTrackingState !== undefined) {
    if (current.passScheduleTrackingState !== input.passScheduleTrackingState) {
      updates.passScheduleTrackingState = input.passScheduleTrackingState
    }
  }

  // Sun Track 상태 업데이트
  if (input.sunTrackStatus !== undefined) {
    if (current.sunTrackStatus !== input.sunTrackStatus) {
      updates.sunTrackStatus = input.sunTrackStatus
    }
  }

  // Sun Track 추적 상태 업데이트
  if (input.sunTrackTrackingState !== undefined) {
    if (current.sunTrackTrackingState !== input.sunTrackTrackingState) {
      updates.sunTrackTrackingState = input.sunTrackTrackingState
    }
  }

  return updates
}

/**
 * 추적 상태가 활성화되어 있는지 확인
 * @param state - 현재 추적 상태
 * @returns 활성화 여부
 */
export function isAnyTrackingActive(state: CurrentTrackingState): boolean {
  return (
    state.ephemerisStatus === true ||
    state.passScheduleStatus === true ||
    state.sunTrackStatus === true
  )
}

/**
 * 활성화된 추적 모드 이름 반환
 * @param state - 현재 추적 상태
 * @returns 활성화된 모드 이름 (없으면 null)
 */
export function getActiveTrackingMode(
  state: CurrentTrackingState,
): 'ephemeris' | 'passSchedule' | 'sunTrack' | null {
  if (state.ephemerisStatus === true) return 'ephemeris'
  if (state.passScheduleStatus === true) return 'passSchedule'
  if (state.sunTrackStatus === true) return 'sunTrack'
  return null
}

/**
 * 현재 추적 상태의 상세 정보 반환
 * @param state - 현재 추적 상태
 * @returns 활성 모드와 상세 상태
 */
export function getTrackingStateInfo(state: CurrentTrackingState): {
  activeMode: 'ephemeris' | 'passSchedule' | 'sunTrack' | null
  trackingState: string | null
} {
  if (state.ephemerisStatus === true) {
    return { activeMode: 'ephemeris', trackingState: state.ephemerisTrackingState }
  }
  if (state.passScheduleStatus === true) {
    return { activeMode: 'passSchedule', trackingState: state.passScheduleTrackingState }
  }
  if (state.sunTrackStatus === true) {
    return { activeMode: 'sunTrack', trackingState: state.sunTrackTrackingState }
  }
  return { activeMode: null, trackingState: null }
}
