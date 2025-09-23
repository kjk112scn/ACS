import { createPinia } from 'pinia'

// Re-export
export { useAuthStore } from './common/auth'
export { useModeStore } from './common/modeStore'
export { useSettingsStore } from './api/settingsStore'
export { useEphemerisTrackModeStore } from './mode/ephemerisTrackStore'
export { usePassScheduleModeStore } from './mode/passScheduleStore'
export { usePedestalPositionModeStore } from './mode/pedestalPositionStore'
export { useSlewModeStore } from './mode/slewStore'
export { useStandbyModeStore } from './mode/standbyStore'
export { useStepStore } from './mode/stepStore'

// 타입들도 export
export type { ScheduleItem } from './mode/passScheduleStore'

// Pinia store instance를 default export
export default createPinia()
