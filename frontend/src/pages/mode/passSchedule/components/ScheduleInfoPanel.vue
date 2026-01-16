<template>
  <q-card class="control-section">
    <q-card-section>
      <div class="text-subtitle1 text-weight-bold text-primary q-mb-xs">Schedule Information</div>
      <div class="schedule-form">
        <div class="form-row">
          <!-- 자동/수동 선택된 스케줄 정보 표시 -->
          <div v-if="schedule" class="schedule-info q-mt-xs">
            <div class="info-row">
              <span class="info-label">스케줄 ID / 상태:</span>
              <div class="info-value-with-badge">
                <span class="info-value">{{ schedule.no }}</span>
                <q-badge
                  v-if="scheduleStatus"
                  :color="scheduleStatus.color"
                  :label="scheduleStatus.label"
                  class="q-ml-sm"
                />
                <q-badge
                  v-if="schedule.isKeyhole || schedule.IsKeyhole"
                  color="red"
                  label="KEYHOLE"
                  class="q-ml-sm"
                />
              </div>
            </div>
            <div class="info-row">
              <span class="info-label">위성 이름:</span>
              <span class="info-value">{{ schedule.satelliteName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">시작 시간:</span>
              <span class="info-value">{{ formatTime(schedule.startTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">종료 시간:</span>
              <span class="info-value">{{ formatTime(schedule.endTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">지속 시간:</span>
              <span class="info-value">{{ formatDurationValue(schedule.duration) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">시작 방위각:</span>
              <span class="info-value">{{ schedule.startAzimuthAngle.toFixed(2) }}°</span>
            </div>
            <div class="info-row">
              <span class="info-label">최대 고도각:</span>
              <span class="info-value">{{ schedule.maxElevation?.toFixed(2) }}°</span>
            </div>
            <div class="info-row">
              <span class="info-label">남은 시간:</span>
              <span
                class="info-value"
                :class="{
                  'text-negative': timeRemaining < 0,
                  'text-positive': timeRemaining > 0,
                  'text-grey': timeRemaining === 0,
                }"
              >
                {{ formatRemainingTime(timeRemaining) }}
              </span>
            </div>
          </div>
          <!-- 스케줄이 선택되지 않은 경우 -->
          <div v-else class="no-schedule-selected">
            <div class="text-grey-5">추적 중인 스케줄이 없습니다</div>
          </div>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { formatToLocalTime, formatTimeRemaining } from '@/utils/times'
import type { ScheduleItem } from '@/stores'

interface ScheduleStatus {
  color: string
  label: string
}

interface Props {
  schedule: ScheduleItem | null
  scheduleStatus: ScheduleStatus | null
  timeRemaining: number
}

defineProps<Props>()

// 유틸리티 함수들
const formatTime = (dateString: string): string => {
  return formatToLocalTime(dateString)
}

const formatRemainingTime = (ms: number): string => {
  return formatTimeRemaining(ms)
}

const formatDurationValue = (duration: string): string => {
  if (!duration) return '0분 0초'

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration

  const hours = parseInt(match[1] || '0', 10)
  const minutes = parseInt(match[2] || '0', 10)
  const seconds = parseFloat(match[3] || '0')

  const parts: string[] = []
  if (hours > 0) parts.push(`${hours}시간`)
  if (minutes > 0) parts.push(`${minutes}분`)
  if (seconds > 0) parts.push(`${Math.round(seconds)}초`)

  return parts.length > 0 ? parts.join(' ') : '0초'
}
</script>

<style scoped>
.schedule-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.info-label {
  font-weight: 500;
  color: var(--theme-text-secondary, #666);
  min-width: 90px;
  font-size: 0.85rem;
}

.info-value {
  font-weight: 600;
  color: var(--theme-text-primary, #333);
  font-size: 0.85rem;
}

.info-value-with-badge {
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-schedule-selected {
  padding: 16px;
  text-align: center;
}

.control-section {
  height: 100%;
}
</style>
