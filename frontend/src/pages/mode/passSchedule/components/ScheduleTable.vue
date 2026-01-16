<template>
  <q-table
    flat
    bordered
    :row-class="getRowClass"
    :row-style="getRowStyleDirect"
    :rows="scheduleList"
    :columns="columns"
    row-key="no"
    :pagination="{ rowsPerPage: 0 }"
    hide-pagination
    :loading="loading && scheduleList.length === 0"
    @row-click="handleRowClick"
    class="schedule-table q-mt-sm"
    style="height: 210px; max-height: 210px;"
    :no-data-label="'등록된 스케줄이 없습니다'"
    :virtual-scroll="false"
  >
    <template v-slot:loading>
      <q-inner-loading showing color="primary">
        <q-spinner size="50px" color="primary" />
      </q-inner-loading>
    </template>

    <!-- Keyhole 배지 컬럼 -->
    <template v-slot:body-cell-keyhole="props">
      <q-td :props="props">
        <q-badge v-if="props.row.isKeyhole || props.row.IsKeyhole" color="red" label="KEYHOLE" />
      </q-td>
    </template>

    <!-- 위성 정보 컬럼 템플릿 -->
    <template v-slot:body-cell-satelliteInfo="props">
      <q-td :props="props" class="satellite-info-cell">
        <div class="satellite-container">
          <div class="satellite-id">{{ props.row.satelliteId || '-' }}</div>
          <div class="satellite-name">{{ props.row.satelliteName }}</div>
        </div>
      </q-td>
    </template>

    <!-- 시간 범위 컬럼 템플릿 -->
    <template v-slot:body-cell-timeRange="props">
      <q-td :props="props" class="time-range-cell">
        <div class="time-container">
          <div class="start-time">{{ formatDateTime(props.row.startTime) }}</div>
          <div class="end-time">{{ formatDateTime(props.row.endTime) }}</div>
        </div>
      </q-td>
    </template>

    <!-- Azimuth 범위 컬럼 템플릿 -->
    <template v-slot:body-cell-azimuthRange="props">
      <q-td :props="props" class="azimuth-range-cell">
        <div class="azimuth-container">
          <div class="start-az">{{ formatAngle(props.row.startAzimuthAngle) }}</div>
          <div class="end-az">{{ formatAngle(props.row.endAzimuthAngle) }}</div>
        </div>
      </q-td>
    </template>

    <!-- Elevation 정보 컬럼 템플릿 -->
    <template v-slot:body-cell-elevationInfo="props">
      <q-td :props="props" class="elevation-info-cell">
        <div class="elevation-container">
          <div class="max-elevation">{{ formatAngle(props.row.maxElevation) }}</div>
          <div class="train">{{ formatAngle(props.row.train) }}</div>
        </div>
      </q-td>
    </template>
  </q-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QTableProps } from 'quasar'
import type { ScheduleItem } from '@/stores'
import { formatToLocalTime } from '@/utils/times'

type QTableColumn = NonNullable<QTableProps['columns']>[0]

interface HighlightInfo {
  currentMstId: number | null
  currentDetailId: number | null
  nextMstId: number | null
  nextDetailId: number | null
}

interface Props {
  scheduleList: ScheduleItem[]
  columns: QTableColumn[]
  loading: boolean
  highlightInfo: HighlightInfo
}

const props = defineProps<Props>()

const emit = defineEmits<{
  rowClick: [event: Event, row: ScheduleItem]
}>()

// 유틸리티 함수들
const formatDateTime = (dateString: string): string => {
  try {
    return formatToLocalTime(dateString)
  } catch (error) {
    console.error('시간 포맷팅 오류:', error)
    return dateString
  }
}

const formatAngle = (angle: number | undefined | null): string => {
  if (angle === undefined || angle === null) return '-'
  return `${angle.toFixed(1)}°`
}

// 행 스타일링 로직
const highlightedRows = computed(() => {
  return {
    current: props.highlightInfo.currentMstId,
    currentDetailId: props.highlightInfo.currentDetailId,
    next: props.highlightInfo.nextMstId,
    nextDetailId: props.highlightInfo.nextDetailId,
  }
})

const getRowStyleDirect = (rowProps: { row: ScheduleItem }) => {
  try {
    const schedule = rowProps.row
    const scheduleMstId = schedule.mstId ?? schedule.no
    const scheduleDetailId = schedule.detailId ?? null
    const { current, currentDetailId, next, nextDetailId } = highlightedRows.value

    if (current !== null || next !== null) {
      const currentMatch =
        current !== null &&
        Number(scheduleMstId) === Number(current) &&
        (currentDetailId !== null &&
          scheduleDetailId !== null &&
          Number(scheduleDetailId) === Number(currentDetailId))

      const nextMatch =
        next !== null &&
        Number(scheduleMstId) === Number(next) &&
        (nextDetailId !== null &&
          scheduleDetailId !== null &&
          Number(scheduleDetailId) === Number(nextDetailId))

      if (currentMatch) {
        return {
          backgroundColor: 'rgba(27, 94, 32, 0.92)',
          borderLeft: '4px solid #66bb6a',
          color: '#e8f5e9',
          fontWeight: '600',
        }
      }
      if (nextMatch) {
        return {
          backgroundColor: 'rgba(13, 71, 161, 0.9)',
          borderLeft: '4px solid #42a5f5',
          color: '#e3f2fd',
          fontWeight: '600',
        }
      }
    }
    return {}
  } catch (error) {
    console.error('❌ getRowStyleDirect 에러:', error)
    return {}
  }
}

const getRowClass = (rowProps: { row: ScheduleItem }) => {
  try {
    const schedule = rowProps.row
    const scheduleMstId = schedule.mstId ?? schedule.no
    const scheduleDetailId = schedule.detailId ?? null
    const { current, currentDetailId, next, nextDetailId } = highlightedRows.value

    if (current !== null || next !== null) {
      const currentMatch =
        current !== null &&
        Number(scheduleMstId) === Number(current) &&
        (currentDetailId !== null &&
          scheduleDetailId !== null &&
          Number(scheduleDetailId) === Number(currentDetailId))

      const nextMatch =
        next !== null &&
        Number(scheduleMstId) === Number(next) &&
        (nextDetailId !== null &&
          scheduleDetailId !== null &&
          Number(scheduleDetailId) === Number(nextDetailId))

      if (currentMatch) {
        return 'highlight-current-schedule'
      }
      if (nextMatch) {
        return 'highlight-next-schedule'
      }
    }
    return ''
  } catch (error) {
    console.error('❌ getRowClass 에러:', error)
    return ''
  }
}

const handleRowClick = (evt: Event, row: ScheduleItem) => {
  emit('rowClick', evt, row)
}
</script>

<style scoped>
/* ===== 테이블 기본 스타일 ===== */
.schedule-table {
  background-color: var(--theme-card-background);
  color: white;
  border-radius: 6px;
  overflow: hidden;
  height: 210px !important;
  max-height: 210px !important;
}

.schedule-table :deep(.q-table__container) {
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  height: 210px !important;
  max-height: 210px !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.schedule-table :deep(.q-table__middle) {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  min-height: 0;
}

.schedule-table :deep(.q-table__bottom) {
  display: none !important;
}

.schedule-table :deep(.q-table__control) {
  display: none !important;
}

/* 테이블 헤더 고정 */
.schedule-table :deep(.q-table thead) {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: var(--theme-card-background);
}

.schedule-table :deep(.q-table thead th) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  font-size: 12px;
  padding: 8px 20px 8px 6px;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
  text-align: center !important;
  vertical-align: middle !important;
  white-space: pre-line;
  line-height: 1.2;
  height: 50px !important;
  position: sticky;
  top: 0;
  z-index: 10;
}

/* 테이블 행 스타일 */
.schedule-table :deep(.q-table tbody tr) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.2s ease;
  cursor: pointer;
}

.schedule-table :deep(.q-table tbody tr:hover) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.schedule-table :deep(.q-table tbody td) {
  padding: 8px 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  border-right: 1px solid rgba(255, 255, 255, 0.04);
  vertical-align: middle;
  text-align: center;
}

/* ===== 하이라이트 스타일 ===== */
.schedule-table tbody tr.highlight-current-schedule {
  background-color: rgba(27, 94, 32, 0.92) !important;
  border-left: 4px solid #66bb6a !important;
  color: #e8f5e9 !important;
}

.schedule-table tbody tr.highlight-current-schedule td {
  background-color: transparent !important;
  color: inherit !important;
  font-weight: 600 !important;
}

.schedule-table tbody tr.highlight-next-schedule {
  background-color: rgba(13, 71, 161, 0.9) !important;
  border-left: 4px solid #42a5f5 !important;
  color: #e3f2fd !important;
}

.schedule-table tbody tr.highlight-next-schedule td {
  background-color: transparent !important;
  color: inherit !important;
  font-weight: 600 !important;
}

/* ===== 컬럼별 스타일 ===== */
.satellite-info-cell {
  padding: 8px 6px !important;
  min-width: 100px;
  text-align: center;
}

.satellite-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}

.satellite-id {
  font-weight: 600;
  font-size: 14px;
  color: #2196f3;
  line-height: 1.2;
}

.satellite-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
  line-height: 1.2;
  word-break: break-word;
}

.time-range-cell {
  padding: 8px 6px !important;
  min-width: 130px;
  text-align: center;
}

.time-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}

.start-time,
.end-time {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.start-time {
  color: #4caf50;
}

.end-time {
  color: #ff9800;
}

.azimuth-range-cell {
  padding: 8px 6px !important;
  vertical-align: middle !important;
  min-width: 80px;
  text-align: center;
}

.azimuth-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  min-height: 35px;
}

.start-az,
.end-az {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
  font-family: 'Courier New', monospace;
}

.start-az {
  color: #4caf50;
}

.end-az {
  color: #ff9800;
}

.elevation-info-cell {
  padding: 8px 6px !important;
  vertical-align: middle !important;
  min-width: 70px;
  text-align: center;
}

.elevation-container {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
  justify-content: center;
  min-height: 35px;
}

.max-elevation {
  font-size: 14px;
  font-weight: 600;
  color: #4caf50;
}

.train {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}
</style>
