<template>
  <div class="select-schedule-content">
    <div class="content-header">
      <div class="text-h6 text-primary">스케줄 선택</div>
      <div class="text-caption text-grey-5">
        총 {{ scheduleData.length }}개의 패스 스케줄
      </div>
    </div>

    <div class="content-body">
      <!-- 스케줄 테이블 -->
      <q-table flat bordered dark :rows="scheduleData" :columns="scheduleColumns" row-key="No" :pagination="pagination"
        :loading="loading" selection="single" v-model:selected="selectedRows" @row-click="onRowClick"
        class="schedule-table" style="height: 400px; background-color: var(--q-dark);">
        <template v-slot:loading>
          <q-inner-loading showing color="primary">
            <q-spinner size="50px" color="primary" />
          </q-inner-loading>
        </template>

        <template v-slot:no-data>
          <div class="full-width row flex-center text-grey-5 q-gutter-sm">
            <q-icon size="2em" name="satellite_alt" />
            <span>패스 스케줄 데이터가 없습니다</span>
          </div>
        </template>

        <template v-slot:body-cell-Status="props">
          <q-td :props="props">
            <q-badge :color="getStatusColor(props.value)" :label="props.value" class="status-badge" />
          </q-td>
        </template>

        <template v-slot:body-cell-StartTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.value) }}
          </q-td>
        </template>

        <template v-slot:body-cell-EndTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.value) }}
          </q-td>
        </template>

        <template v-slot:body-cell-MaxElevation="props">
          <q-td :props="props">
            {{ props.value ? props.value.toFixed(1) + '°' : '-' }}
          </q-td>
        </template>

        <template v-slot:body-cell-SatelliteId="props">
          <q-td :props="props">
            <q-chip :label="props.value" color="info" text-color="white" size="sm" v-if="props.value" />
          </q-td>
        </template>
      </q-table>
    </div>

    <div class="content-footer">
      <div class="button-group">
        <q-btn color="primary" label="Select" icon="check" @click="handleSelect" :disable="selectedRows.length === 0"
          class="action-btn" />
        <q-btn color="grey-7" label="Close" icon="close" @click="handleClose" class="action-btn" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleStore, type ScheduleItem } from '../../stores/mode/passScheduleStore'
import type { QTableProps } from 'quasar'

const $q = useQuasar()
const passScheduleStore = usePassScheduleStore()

// ✅ 올바른 데이터 참조
const scheduleData = computed(() => passScheduleStore.scheduleData)
const loading = computed(() => passScheduleStore.loading)

// 선택된 행
const selectedRows = ref<ScheduleItem[]>([])

// 테이블 컬럼 정의
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'No', label: 'No', field: 'No', align: 'left' as const, sortable: true, style: 'width: 60px' },
  { name: 'SatelliteId', label: '위성 ID', field: 'SatelliteId', align: 'center' as const, sortable: true, style: 'width: 100px' },
  { name: 'Name', label: '위성명', field: 'Name', align: 'left' as const, sortable: true },
  {
    name: 'StartTime',
    label: '시작 시간',
    field: 'StartTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px'
  },
  {
    name: 'EndTime',
    label: '종료 시간',
    field: 'EndTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px'
  },
  {
    name: 'Duration',
    label: '지속 시간',
    field: 'Duration',
    align: 'center' as const,
    sortable: true,
    format: (val: number) => `${Math.round(val)}분`,
    style: 'width: 80px'
  },
  {
    name: 'MaxElevation',
    label: '최대 고도',
    field: 'MaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px'
  },
  {
    name: 'Status',
    label: '상태',
    field: 'Status',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px'
  },
]

// 페이지네이션 설정
const pagination = {
  sortBy: 'StartTime',
  descending: false,
  page: 1,
  rowsPerPage: 15,
  rowsNumber: 15,
}

// 유틸리티 함수들
const formatDateTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return dateString
  }
}

const getStatusColor = (status: string): string => {
  switch (status.toLowerCase()) {
    case 'running':
    case 'active':
      return 'positive'
    case 'pending':
    case 'scheduled':
      return 'warning'
    case 'completed':
    case 'finished':
      return 'info'
    case 'stopped':
    case 'cancelled':
    case 'failed':
      return 'negative'
    default:
      return 'grey'
  }
}

// 이벤트 핸들러들
const onRowClick = (evt: Event, row: ScheduleItem) => {
  selectedRows.value = [row]
  console.log('📋 패스 스케줄 행 선택:', {
    name: row.Name,
    satelliteId: row.SatelliteId,
    startTime: row.StartTime,
    passNumber: row.PassNumber
  })
}

const handleSelect = () => {
  if (selectedRows.value.length === 0) {
    $q.notify({
      type: 'warning',
      message: '패스 스케줄을 선택하세요',
    })
    return
  }

  const schedule = selectedRows.value[0]
  if (!schedule) return

  // Store에 선택된 스케줄 저장
  passScheduleStore.selectSchedule(schedule)

  console.log('✅ 패스 스케줄 선택됨:', {
    name: schedule.Name,
    satelliteId: schedule.SatelliteId,
    startTime: schedule.StartTime,
    duration: schedule.Duration
  })

  $q.notify({
    type: 'positive',
    message: `패스 스케줄 "${schedule.Name}"이 선택되었습니다`,
  })

  // 모달 닫기
  handleClose()
}

const handleClose = () => {
  // 모달 닫기 (부모 컴포넌트에서 처리)
  window.close()
}

onMounted(async () => {
  console.log('SelectScheduleContent 마운트됨')
  console.log('🔍 초기 스케줄 데이터 상태:', scheduleData.value.length)

  try {
    console.log('🚀 서버에서 패스 스케줄 데이터 로드 시작')

    const success = await passScheduleStore.fetchScheduleDataFromServer()

    if (success) {
      console.log('✅ 패스 스케줄 데이터 로드 성공:', scheduleData.value.length, '개')
      console.log('📋 로드된 데이터 샘플:', scheduleData.value.slice(0, 3))

      // ✅ 테이블에 표시될 데이터 확인
      console.log('🔍 테이블 표시용 데이터:', {
        totalCount: scheduleData.value.length,
        firstItem: scheduleData.value[0],
        columns: scheduleColumns.map(col => col.name)
      })
    } else {
      console.log('⚠️ 패스 스케줄 데이터 없음')
    }
  } catch (error) {
    console.error('❌ 패스 스케줄 데이터 로드 실패:', error)

    $q.notify({
      type: 'negative',
      message: '패스 스케줄 데이터 로드에 실패했습니다',
    })
  }
})
</script>

<style scoped>
.select-schedule-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--q-dark) !important;
  color: white !important;
  padding: 1rem;
  overflow: hidden;
  box-sizing: border-box;
}

.content-header {
  flex-shrink: 0;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.content-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
}

.content-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.button-group {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.action-btn {
  min-width: 100px;
  height: 40px;
}

.schedule-table {
  background-color: var(--q-dark) !important;
  color: white !important;
  flex: 1;
  width: 100%;
}

/* ✅ 테이블 컨테이너 배경 설정 */
.schedule-table :deep(.q-table__container) {
  background-color: var(--q-dark) !important;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

/* ✅ 테이블 헤더 배경 설정 */
.schedule-table :deep(.q-table thead) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.schedule-table :deep(.q-table thead th) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important;
}

/* ✅ 테이블 바디 배경 설정 */
.schedule-table :deep(.q-table tbody) {
  background-color: var(--q-dark) !important;
}

.schedule-table :deep(.q-table tbody tr) {
  background-color: var(--q-dark) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
}

.schedule-table :deep(.q-table tbody tr:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
}

.schedule-table :deep(.q-table tbody tr.selected) {
  background-color: rgba(25, 118, 210, 0.12) !important;
}

.schedule-table :deep(.q-table tbody td) {
  background-color: transparent !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
}

/* ✅ 로딩 및 빈 데이터 상태 배경 설정 */
.schedule-table :deep(.q-table__bottom--nodata) {
  background-color: var(--q-dark) !important;
  color: white !important;
}

.schedule-table :deep(.q-inner-loading) {
  background-color: rgba(0, 0, 0, 0.7) !important;
}

.schedule-table :deep(.q-spinner) {
  color: #2196f3 !important;
}

.schedule-table :deep(.full-width) {
  background-color: var(--q-dark) !important;
  color: white !important;
  padding: 2rem;
}

/* ✅ 반응형 디자인에서도 배경 유지 */
@media (max-width: 768px) {
  .select-schedule-content {
    padding: 0.5rem;
    background-color: var(--q-dark) !important;
  }

  .content-header,
  .content-body,
  .content-footer {
    background-color: transparent;
  }
}

@media (max-width: 480px) {
  .select-schedule-content {
    background-color: var(--q-dark) !important;
  }
}
</style>
