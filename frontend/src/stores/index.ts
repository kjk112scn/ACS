import { createPinia } from 'pinia'

// === Settings 관련 Store ===
export { useSettingsStore } from './api/settings/settingsStore'
export { useLocationSettingsStore } from './api/settings/locationSettingsStore'
export { useTrackingSettingsStore } from './api/settings/trackingSettingsStore'
export { useStowSettingsStore } from './api/settings/stowSettingsStore'
export { useAntennaSpecSettingsStore } from './api/settings/antennaSpecSettingsStore'
export { useAngleLimitsSettingsStore } from './api/settings/angleLimitsSettingsStore'
export { useSpeedLimitsSettingsStore } from './api/settings/speedLimitsSettingsStore'
export { useOffsetLimitsSettingsStore } from './api/settings/offsetLimitsSettingsStore'
export { useAlgorithmSettingsStore } from './api/settings/algorithmSettingsStore'
export { useStepSizeLimitSettingsStore } from './api/settings/stepSizeLimitSettingsStore'

// === Common 관련 Store ===
export { useAuthStore } from './common/auth'
export { useModeStore } from './common/modeStore'

// === Mode 관련 Store ===
export { useEphemerisTrackModeStore } from './mode/ephemerisTrackStore'
export { usePassScheduleModeStore } from './mode/passScheduleStore'
export { usePedestalPositionModeStore } from './mode/pedestalPositionStore'
export { useSlewModeStore } from './mode/slewStore'
export { useStandbyModeStore } from './mode/standbyStore'
export { useStepStore } from './mode/stepStore'

// === ICD 관련 Store ===
export { useICDStore } from './icd/icdStore'
export { useModeStore as useICDModeStore } from './icd/modeStore'

// === UI 관련 Store ===
export { useFeedSettingsStore } from './ui/feedSettingsStore'

// 타입들도 export
export type { ScheduleItem } from './mode/passScheduleStore'

// Pinia store instance를 default export
export default createPinia()
