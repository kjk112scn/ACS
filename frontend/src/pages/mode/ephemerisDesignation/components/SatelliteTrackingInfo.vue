<template>
  <q-card class="control-section">
    <q-card-section>
      <div class="row justify-between items-center q-mb-xs">
        <div class="text-subtitle1 text-weight-bold text-primary">위성 추적 정보</div>
        <div class="row items-center q-gutter-sm">
          <span class="info-label">추적 상태:</span>
          <q-chip
            :color="trackingStateInfo.displayColor"
            text-color="white"
            :label="trackingStateInfo.displayLabel"
            size="sm"
            class="tracking-status-chip"
          />
        </div>
      </div>

      <div class="ephemeris-form">
        <div class="form-row">
          <!-- 정지궤도 정보 표시 -->
          <div v-if="scheduleInfo.isGeostationary" class="schedule-info q-mt-xs">
            <div class="text-subtitle2 text-weight-bold text-primary q-mb-xs">
              정지궤도 위성 정보
            </div>

            <div class="info-row">
              <span class="info-label">위성 이름/ID:</span>
              <span class="info-value">{{ scheduleInfo.satelliteName }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">방위각:</span>
              <span class="info-value">{{ safeToFixed(scheduleInfo.startAzimuth, 2) }}°</span>
            </div>

            <div class="info-row">
              <span class="info-label">고도:</span>
              <span class="info-value">{{ safeToFixed(scheduleInfo.startElevation, 2) }}°</span>
            </div>
          </div>

          <!-- 기존 스케줄 정보 표시 (정지궤도가 아닌 경우) -->
          <div v-else-if="scheduleInfo.satelliteName" class="schedule-info q-mt-xs">
            <div class="info-row">
              <span class="info-label">위성 이름/ID:</span>
              <span class="info-value">
                {{ scheduleInfo.satelliteName }} / {{ scheduleInfo.satelliteId }}
                <q-badge v-if="scheduleInfo.isKeyhole" color="red" class="q-ml-sm" label="KEYHOLE" />
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">시작/종료 시간:</span>
              <span class="info-value">{{
                formatToLocalTime(scheduleInfo.startTime)
              }} / {{
                formatToLocalTime(scheduleInfo.endTime)
              }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">지속 시간:</span>
              <span class="info-value">{{ formatDuration(scheduleInfo.duration) }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">시작/종료 방위각/고도:</span>
              <span class="info-value">{{ safeToFixed(scheduleInfo.startAzimuth, 6) }}° / {{
                safeToFixed(scheduleInfo.endAzimuth, 6) }}° / {{
                safeToFixed(scheduleInfo.startElevation, 6) }}°</span>
            </div>

            <div class="info-row">
              <span class="info-label">최대 고도:</span>
              <span class="info-value">{{ safeToFixed(scheduleInfo.maxElevation, 6) }}°</span>
            </div>

            <!-- KEYHOLE 정보 표시 -->
            <div v-if="scheduleInfo.isKeyhole" class="keyhole-info q-mt-sm q-pa-sm">
              <div class="text-weight-bold text-red q-mb-xs">🚀 KEYHOLE 위성 정보</div>
              <div class="info-row">
                <span class="info-label">권장 Train 각도:</span>
                <span class="info-value text-positive">{{
                  safeToFixed(scheduleInfo.recommendedTrainAngle, 6)
                }}°</span>
              </div>
              <div class="info-row">
                <span class="info-label">2축/3축/최적화 Azimuth 속도:</span>
                <span class="info-value text-red">
                  {{ safeToFixed(scheduleInfo.OriginalMaxAzRate ?? 0, 2) }} /
                  {{ safeToFixed(scheduleInfo.FinalTransformedMaxAzRate ?? 0, 2) }} /
                  {{ safeToFixed(scheduleInfo.KeyholeOptimizedFinalTransformedMaxAzRate ?? 0, 2) }}°/s
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">2축/3축/최적화 Elevation 속도:</span>
                <span class="info-value text-red">
                  {{ safeToFixed(scheduleInfo.OriginalMaxElRate ?? 0, 2) }} /
                  {{ safeToFixed(scheduleInfo.FinalTransformedMaxElRate ?? 0, 2) }} /
                  {{ safeToFixed(scheduleInfo.KeyholeOptimizedFinalTransformedMaxElRate ?? 0, 2) }}°/s
                </span>
              </div>
            </div>

            <div class="info-row">
              <span class="info-label">남은 시간:</span>
              <span class="info-value" :class="{
                'text-negative': timeRemainingMs < 0,
                'text-positive': timeRemainingMs > 0,
                'text-grey': timeRemainingMs === 0,
              }">
                {{ formatTimeRemaining(timeRemainingMs) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { formatToLocalTime } from '@/utils/times'

interface TrackingStateInfo {
  displayColor: string
  displayLabel: string
}

interface ScheduleInfo {
  satelliteName: string
  satelliteId?: string
  isGeostationary?: boolean
  isKeyhole?: boolean
  startTime?: string
  endTime?: string
  duration?: string
  startAzimuth?: number
  endAzimuth?: number
  startElevation?: number
  endElevation?: number
  maxElevation?: number
  recommendedTrainAngle?: number
  OriginalMaxAzRate?: number
  OriginalMaxElRate?: number
  FinalTransformedMaxAzRate?: number
  FinalTransformedMaxElRate?: number
  KeyholeOptimizedFinalTransformedMaxAzRate?: number
  KeyholeOptimizedFinalTransformedMaxElRate?: number
}

interface Props {
  trackingStateInfo: TrackingStateInfo
  scheduleInfo: ScheduleInfo
  timeRemainingMs: number  // 밀리초 단위
}

defineProps<Props>()

const formatDuration = (duration?: string): string => {
  if (!duration) return '00:00:00'
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration
  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = Math.round(parseFloat(match[3] || '0'))
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const safeToFixed = (value: number | null | undefined, digits: number = 2): string => {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '0.' + '0'.repeat(digits)
  }
  return Number(value).toFixed(digits)
}

const formatTimeRemaining = (ms: number): string => {
  if (ms === 0) return '-'
  const absMs = Math.abs(ms)
  const hours = Math.floor(absMs / 3600000)
  const minutes = Math.floor((absMs % 3600000) / 60000)
  const secs = Math.floor((absMs % 60000) / 1000)
  const millis = absMs % 1000
  const sign = ms < 0 ? '-' : ''
  // 개발 단계: ms까지 표시 (실시간 업데이트 확인용)
  if (hours > 0) return `${sign}${hours}h ${minutes}m ${secs}.${String(millis).padStart(3, '0')}s`
  if (minutes > 0) return `${sign}${minutes}m ${secs}.${String(millis).padStart(3, '0')}s`
  return `${sign}${secs}.${String(millis).padStart(3, '0')}s`
}
</script>

<style scoped>
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--theme-text-secondary);
  font-weight: 500;
}

.info-value {
  color: var(--theme-text-primary);
  font-weight: 600;
}

.keyhole-info {
  background-color: rgba(255, 0, 0, 0.1);
  border-left: 3px solid #f44336;
  border-radius: 4px;
}

.tracking-status-chip {
  font-weight: 600;
}
</style>
