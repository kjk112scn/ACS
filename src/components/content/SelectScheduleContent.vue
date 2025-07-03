<template>
  <div class="select-schedule-content">
    <!-- ✅ X 아이콘 추가 -->
    <q-btn flat round dense icon="close" color="grey-5" size="sm" @click="handleClose" class="close-btn">
      <q-tooltip>닫기</q-tooltip>
    </q-btn>

    <div class="content-header">
      <div class="text-h6 text-primary">스케줄 선택</div>
      <div class="text-caption text-grey-5">
        총 {{ scheduleData.length }}개의 패스 스케줄 ({{ selectedRows.length }}개 선택됨)
        <span v-if="overlappingGroups.length > 0" class="text-warning q-ml-sm">
          ⚠️ {{ overlappingGroups.flat().length }}개 시간 겹침
        </span>
      </div>
    </div>

    <div class="content-body">
      <!-- 스케줄 테이블 -->
      <q-table flat bordered dark :rows="scheduleData" :columns="scheduleColumns" row-key="no" :loading="loading"
        selection="multiple" v-model:selected="selectedRows" @row-click="onRowClick" class="schedule-table"
        style="height: 400px; background-color: var(--q-dark);" virtual-scroll :virtual-scroll-sticky-size-start="48"
        hide-pagination :rows-per-page-options="[0]" :row-class="getRowClass">

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

        <!-- ✅ 체크박스 컬럼 완전 차단 처리 -->
        <template v-slot:body-cell-selection="props">
          <q-td :props="props" @click.stop.prevent="handleCheckboxInteraction(props.row, $event)"
            @mousedown.stop.prevent="handleCheckboxInteraction(props.row, $event)"
            @touchstart.stop.prevent="handleCheckboxInteraction(props.row, $event)">
            <q-checkbox :model-value="isScheduleSelected(props.row)" :disable="!canSelectSchedule(props.row)"
              :color="isScheduleOverlapping(props.row.no) ? 'warning' : 'primary'"
              @click.stop.prevent="handleCheckboxInteraction(props.row, $event)"
              @update:model-value="handleCheckboxInteraction(props.row, $event)"
              @mousedown.stop.prevent="handleCheckboxInteraction(props.row, $event)"
              @touchstart.stop.prevent="handleCheckboxInteraction(props.row, $event)" class="schedule-checkbox"
              :class="{ 'checkbox-blocked': !canSelectSchedule(props.row) }" />
            <q-tooltip v-if="!canSelectSchedule(props.row)" class="bg-warning text-black">
              시간이 겹치는 다른 스케줄이 이미 선택되어 있습니다
            </q-tooltip>
          </q-td>
        </template>

        <template v-slot:body-cell-startTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.value) }}
            <q-icon v-if="isScheduleOverlapping(props.row.no)" name="warning" color="warning" size="xs" class="q-ml-xs">
              <q-tooltip class="bg-warning text-black">
                시간이 겹치는 스케줄입니다
              </q-tooltip>
            </q-icon>
          </q-td>
        </template>

        <template v-slot:body-cell-endTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.value) }}
          </q-td>
        </template>

        <template v-slot:body-cell-maxElevation="props">
          <q-td :props="props">
            {{ props.value ? props.value.toFixed(1) + '°' : '-' }}
          </q-td>
        </template>

        <template v-slot:body-cell-satelliteId="props">
          <q-td :props="props">
            <q-chip :label="props.value" color="info" text-color="white" size="md" class="satellite-id-chip"
              v-if="props.value" />
          </q-td>
        </template>

        <!-- ✅ Azimuth 각도 컬럼 템플릿 -->
        <template v-slot:body-cell-azimuthAngles="props">
          <q-td :props="props" class="angle-cell">
            <div class="angle-container">
              <div class="angle-line start-angle">
                <span class="angle-label">시작:</span>
                <span class="angle-value">{{ formatAngle(props.row.startAzimuthAngle) }}</span>
              </div>
              <div class="angle-line end-angle">
                <span class="angle-label">종료:</span>
                <span class="angle-value">{{ formatAngle(props.row.endAzimuthAngle) }}</span>
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ Index 컬럼 템플릿 추가 -->
        <template v-slot:body-cell-index="props">
          <q-td :props="props" class="index-cell">
            <span class="index-value">{{ props.value }}</span>
          </q-td>
        </template>
      </q-table>
    </div>

    <div class="content-footer">
      <div class="selection-info" v-if="selectedRows.length > 0">
        <div class="text-body2 text-primary">
          {{ selectedRows.length }}개의 스케줄이 선택되었습니다
        </div>
        <q-btn flat dense color="grey-5" label="전체 해제" @click="clearSelection" size="sm" />
      </div>

      <!-- ✅ 겹침 경고 정보 -->
      <div class="overlap-warning" v-if="overlappingGroups.length > 0">
        <q-icon name="info" color="warning" size="sm" />
        <span class="text-caption text-warning q-ml-xs">
          주황색 행들은 시간이 겹치므로 동시 선택할 수 없습니다
        </span>
      </div>

      <div class="button-group">
        <q-btn color="primary" label="Select" icon="check" @click="handleSelect" :disable="selectedRows.length === 0"
          class="action-btn" />
        <q-btn color="grey-7" label="Close" icon="close" @click="handleClose" class="action-btn" />
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, computed, getCurrentInstance, onUnmounted } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleStore, type ScheduleItem } from '../../stores/mode/passScheduleStore'
import type { QTableProps } from 'quasar'
import { formatToLocalTime } from '../../utils/times'
import { closeWindow } from '../../utils/windowUtils'

const $q = useQuasar()
const passScheduleStore = usePassScheduleStore()




// ✅ 디버깅을 위한 로그 추가 및 안전한 데이터 처리
const scheduleData = computed(() => {
  const rawData = passScheduleStore.scheduleData
  console.log('🔍 원본 데이터 확인:', rawData.slice(0, 3)) // 처음 3개 항목 로그

  if (rawData.length === 0) return []

  // 시간 순으로 정렬
  const sortedData = [...rawData].sort((a, b) => {
    try {
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
    } catch {
      return 0
    }
  })

  // 원본 no를 index로 보존하고, no를 1부터 순서대로 재생성
  const result = sortedData.map((item, sortedIndex) => {
    // 디버깅: 원본 item의 구조 확인
    if (sortedIndex < 3) {
      console.log(`🔍 Item ${sortedIndex}:`, {
        originalNo: item.no,
        satelliteName: item.satelliteName,
        allKeys: Object.keys(item)
      })
    }

    return {
      ...item,
      index: item.no, // 원본 no 값을 index로 보존
      no: sortedIndex + 1 // 정렬된 순서로 1부터 재생성
    }
  })

  console.log('🔍 변환된 데이터 (처음 3개):', result.slice(0, 3))
  return result
})

const loading = computed(() => passScheduleStore.loading)


const selectedRows = ref<ScheduleItem[]>([])

// ✅ 시간 겹침 검사 함수 수정 - 더 엄격한 겹침 검사
const checkTimeOverlap = (schedule1: ScheduleItem, schedule2: ScheduleItem): boolean => {
  try {
    const start1 = new Date(schedule1.startTime).getTime()
    const end1 = new Date(schedule1.endTime).getTime()
    const start2 = new Date(schedule2.startTime).getTime()
    const end2 = new Date(schedule2.endTime).getTime()

    // 🔧 더 엄격한 겹침 검사 - 시작/종료 시간이 조금이라도 겹치면 true
    const isOverlapping = (start1 < end2) && (end1 > start2)

    // 🔧 디버깅 로그 추가
    if (isOverlapping) {
      console.log('⚠️ 시간 겹침 감지:', {
        schedule1: {
          name: schedule1.satelliteName,
          start: schedule1.startTime,
          end: schedule1.endTime,
          startMs: start1,
          endMs: end1
        },
        schedule2: {
          name: schedule2.satelliteName,
          start: schedule2.startTime,
          end: schedule2.endTime,
          startMs: start2,
          endMs: end2
        },
        overlap: {
          condition1: `start1(${start1}) < end2(${end2})`,
          condition2: `end1(${end1}) > start2(${start2})`,
          result1: start1 < end2,
          result2: end1 > start2
        }
      })
    }

    return isOverlapping
  } catch (error) {
    console.error('시간 겹침 검사 오류:', error)
    return false
  }
}

// ✅ 겹치는 스케줄 그룹 계산 - 디버깅 강화
const overlappingGroups = computed(() => {
  const data = scheduleData.value
  const groups: number[][] = []
  const processed = new Set<number>()

  console.log('🔍 겹침 검사 시작 - 총', data.length, '개 스케줄')

  data.forEach((schedule, index) => {
    if (processed.has(schedule.no)) return

    const overlappingSchedules = [schedule.no]

    data.forEach((otherSchedule, otherIndex) => {
      if (index !== otherIndex && !processed.has(otherSchedule.no)) {
        if (checkTimeOverlap(schedule, otherSchedule)) {
          overlappingSchedules.push(otherSchedule.no)
          console.log('🔍 겹침 발견:', {
            schedule1: `${schedule.satelliteName} (${schedule.startTime} ~ ${schedule.endTime})`,
            schedule2: `${otherSchedule.satelliteName} (${otherSchedule.startTime} ~ ${otherSchedule.endTime})`
          })
        }
      }
    })

    if (overlappingSchedules.length > 1) {
      groups.push(overlappingSchedules)
      overlappingSchedules.forEach(no => processed.add(no))
      console.log('✅ 겹침 그룹 생성:', overlappingSchedules.map(no => {
        const item = data.find(s => s.no === no)
        return `${item?.satelliteName}(${no})`
      }))
    }
  })

  console.log('🔍 최종 겹침 그룹:', groups)
  return groups
})

// ✅ 특정 스케줄이 겹치는지 확인
const isScheduleOverlapping = (scheduleNo: number): boolean => {
  return overlappingGroups.value.some(group => group.includes(scheduleNo))
}

// ✅ 특정 스케줄의 겹치는 그룹 가져오기
const getOverlappingGroup = (scheduleNo: number): number[] => {
  const group = overlappingGroups.value.find(group => group.includes(scheduleNo))
  return group || []
}

// ✅ 선택 가능 여부 확인 함수 - 로직 강화
const canSelectSchedule = (schedule: ScheduleItem): boolean => {
  // 겹치지 않는 스케줄은 항상 선택 가능
  if (!isScheduleOverlapping(schedule.no)) {
    console.log('✅ 겹치지 않는 스케줄 - 선택 가능:', schedule.satelliteName)
    return true
  }

  // 겹치는 스케줄인 경우, 같은 그룹의 다른 스케줄이 선택되어 있는지 확인
  const overlappingGroup = getOverlappingGroup(schedule.no)
  const otherSelectedInGroup = selectedRows.value.filter(selected =>
    overlappingGroup.includes(selected.no) && selected.no !== schedule.no
  )

  const canSelect = otherSelectedInGroup.length === 0

  console.log('🔍 겹치는 스케줄 선택 가능 여부:', {
    scheduleName: schedule.satelliteName,
    scheduleNo: schedule.no,
    overlappingGroup,
    otherSelectedInGroup: otherSelectedInGroup.map(s => `${s.satelliteName}(${s.no})`),
    canSelect
  })

  return canSelect
}

// ✅ 체크박스 상태 확인 함수 (통합)
const isScheduleSelected = (schedule: ScheduleItem): boolean => {
  return selectedRows.value.some(selected => selected.no === schedule.no)
}

// ✅ 스케줄 선택 토글 함수 - 검증 강화
const toggleScheduleSelection = (row: ScheduleItem) => {
  console.log('🔄 스케줄 선택 토글 시도:', {
    scheduleName: row.satelliteName,
    scheduleNo: row.no,
    startTime: row.startTime,
    endTime: row.endTime
  })

  if (!canSelectSchedule(row)) {
    console.log('❌ 선택 불가능한 스케줄')
    showOverlapWarning(row)
    return
  }

  const index = selectedRows.value.findIndex(item => item.no === row.no)

  if (index >= 0) {
    // 선택 해제
    selectedRows.value.splice(index, 1)
    console.log('✅ 스케줄 선택 해제:', row.satelliteName)
  } else {
    // 선택 추가 - 추가 검증
    const wouldOverlap = selectedRows.value.some(selected =>
      checkTimeOverlap(row, selected)
    )

    if (wouldOverlap) {
      console.log('❌ 추가 겹침 검증 실패')
      showOverlapWarning(row)
      return
    }

    selectedRows.value.push(row)
    console.log('✅ 스케줄 선택 추가:', row.satelliteName)
  }
}

// ✅ 겹침 경고 메시지 표시 함수 - 더 상세한 정보 제공
const showOverlapWarning = (row: ScheduleItem) => {
  const overlappingGroup = getOverlappingGroup(row.no)
  const selectedInGroup = selectedRows.value.filter(s => overlappingGroup.includes(s.no))

  // 🔧 겹치는 다른 스케줄들의 시간 정보도 표시
  const overlappingSchedules = scheduleData.value.filter(s =>
    overlappingGroup.includes(s.no) && s.no !== row.no
  )

  let message = `시간이 겹치는 스케줄이 이미 선택되어 있습니다.\n\n`
  message += `선택하려는 스케줄: ${row.satelliteName}\n`
  message += `시간: ${formatDateTime(row.startTime)} ~ ${formatDateTime(row.endTime)}\n\n`

  if (selectedInGroup.length > 0) {
    message += `이미 선택된 겹치는 스케줄:\n`
    selectedInGroup.forEach(s => {
      message += `• ${s.satelliteName}: ${formatDateTime(s.startTime)} ~ ${formatDateTime(s.endTime)}\n`
    })
  } else {
    message += `겹치는 다른 스케줄들:\n`
    overlappingSchedules.forEach(s => {
      message += `• ${s.satelliteName}: ${formatDateTime(s.startTime)} ~ ${formatDateTime(s.endTime)}\n`
    })
  }

  if ($q && typeof $q.notify === 'function') {
    $q.notify({
      type: 'warning',
      message,
      timeout: 5000,
      position: 'top',
      multiLine: true,
      actions: [
        {
          label: '확인',
          color: 'white',
          handler: () => { }
        }
      ]
    })
  } else {
    console.warn('$q.notify is not available:', message)
  }
}

// ✅ 체크박스 관련 모든 이벤트 통합 처리 (완전 차단)
const handleCheckboxInteraction = (row: ScheduleItem, event: Event) => {
  event.stopPropagation()
  event.preventDefault()

  console.log('☑️ 체크박스 상호작용:', row.satelliteName, '선택 가능:', canSelectSchedule(row))

  if (!canSelectSchedule(row)) {
    console.log('❌ 선택 불가능한 체크박스 상호작용 완전 차단')
    showOverlapWarning(row)
    return false
  }

  // 선택 가능한 경우에도 직접 체크박스 조작은 차단
  console.log('✅ 체크박스 직접 조작 차단, 토글 처리')
  toggleScheduleSelection(row)
  return false
}

// ✅ 행 클릭 이벤트 핸들러 (체크박스 영역 완전 제외)
const onRowClick = (evt: Event, row: ScheduleItem) => {
  console.log('🖱️ 행 클릭:', row.satelliteName)

  // 체크박스 영역 클릭 감지 및 완전 차단
  const target = evt.target as HTMLElement
  const isCheckboxArea = target.closest('.q-checkbox') ||
    target.closest('[data-col="selection"]') ||
    target.classList.contains('q-checkbox__inner') ||
    target.classList.contains('q-checkbox__bg') ||
    target.classList.contains('schedule-checkbox') ||
    target.closest('td[data-col="selection"]')

  if (isCheckboxArea) {
    console.log('☑️ 체크박스 영역 클릭 감지, 행 클릭 이벤트 무시')
    evt.stopPropagation()
    evt.preventDefault()
    return
  }

  // 선택 가능 여부 확인 후 토글
  toggleScheduleSelection(row)
}

// 테이블 컬럼 정의
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'index', label: 'Index', field: 'index', align: 'left' as const, sortable: true, style: 'width: 70px' },
  { name: 'no', label: 'No', field: 'no', align: 'left' as const, sortable: true, style: 'width: 60px' },
  { name: 'satelliteId', label: '위성 ID', field: 'satelliteId', align: 'center' as const, sortable: true, style: 'width: 100px' },
  { name: 'satelliteName', label: '위성명', field: 'satelliteName', align: 'left' as const, sortable: true },
  {
    name: 'startTime',
    label: '시작 시간',
    field: 'startTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px'
  },
  {
    name: 'endTime',
    label: '종료 시간',
    field: 'endTime',
    align: 'left' as const,
    sortable: true,
    style: 'width: 150px'
  },
  {
    name: 'duration',
    label: '지속 시간',
    field: 'duration',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px'
  },
  {
    name: 'maxElevation',
    label: '최대 고도',
    field: 'maxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px'
  },
  {
    name: 'azimuthAngles',
    label: 'Azimuth 각도',
    field: (row: ScheduleItem) => ({ start: row.startAzimuthAngle, end: row.endAzimuthAngle }),
    align: 'center' as const,
    sortable: false,
    style: 'width: 120px'
  },
]

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

const clearSelection = () => {
  selectedRows.value = []
  console.log('🗑️ 모든 선택 해제됨')
}

const getRowClass = (row: ScheduleItem): string => {
  const classes = []

  if (isScheduleOverlapping(row.no)) {
    classes.push('overlapping-row')
  }

  if (!canSelectSchedule(row)) {

    classes.push('disabled-row')
  }
  return classes.join(' ')
}

// 🔧 대안: Store의 addSelectedSchedules 활용
// 🔧 handleSelect 함수 - 초기화 후 추가
const handleSelect = async () => {
  try {
    if (selectedRows.value.length === 0) {
      if ($q && typeof $q.notify === 'function') {
        $q.notify({
          type: 'warning',
          message: '패스 스케줄을 선택하세요',
        })
      } else {
        console.warn('패스 스케줄을 선택하세요')
      }
      return
    }

    console.log('🚀 스케줄 선택 처리 시작 (기존 목록 초기화):', selectedRows.value.length, '개')

    // 🔧 기존 목록 초기화 후 새 스케줄 추가
    const success = await passScheduleStore.replaceSelectedSchedules(selectedRows.value)

    if (success) {
      console.log('✅ 스케줄 목록 교체 완료:', {
        count: selectedRows.value.length,
        schedules: selectedRows.value.map(s => ({
          no: s.no, // 서버 원본 No 값
          name: s.satelliteName,
          satelliteId: s.satelliteId,
          startTime: s.startTime
        }))
      })

      if ($q && typeof $q.notify === 'function') {
        $q.notify({
          type: 'positive',
          message: `기존 목록을 초기화하고 ${selectedRows.value.length}개의 새 스케줄이 추적 대상으로 설정되었습니다`,
        })
      } else {
        console.log(`기존 목록을 초기화하고 ${selectedRows.value.length}개의 새 스케줄이 추적 대상으로 설정되었습니다`)
      }

      // 선택 완료 후 창 닫기
      setTimeout(() => {
        try {
          handleClose()
        } catch (closeError) {
          console.error('❌ 창 닫기 중 오류:', closeError)
        }
      }, 100) // 성공 메시지를 볼 시간 제공

    } else {
      if ($q && typeof $q.notify === 'function') {
        $q.notify({
          type: 'negative',
          message: '스케줄 선택에 실패했습니다',
        })
      } else {
        console.error('스케줄 선택에 실패했습니다')
      }
    }

  } catch (error) {
    console.error('❌ 스케줄 선택 처리 중 오류:', error)

    if ($q && typeof $q.notify === 'function') {
      $q.notify({
        type: 'negative',
        message: '스케줄 선택 처리 중 오류가 발생했습니다',
      })
    } else {
      console.error('스케줄 선택 처리 중 오류가 발생했습니다')
    }
  }
}

interface Props {
  modalId?: string
  modalTitle?: string
}
const props = defineProps<Props>()
const instance = getCurrentInstance()

const isPopupWindow = ref(false)
const isModalMode = ref(false)

// 실제 닫기 수행
const performClose = () => {
  console.log('🚪 실제 닫기 수행')

  try {
    if (isPopupWindow.value) {
      // 팝업 창 모드
      console.log('🪟 팝업 창 닫기')
      window.close()
    } else if (isModalMode.value && props.modalId) {
      // 모달 모드 - closeWindow가 ModalManager를 통해 처리
      console.log('📱 모달 닫기 - ID:', props.modalId)

      // 전역 closeModal 함수 사용 (있는 경우)
      const globalProperties = instance?.appContext.config.globalProperties
      if (globalProperties?.$closeModal) {
        console.log('🎯 전역 closeModal 함수 사용')
        globalProperties.$closeModal()
      } else {
        console.log('🎯 closeWindow 함수 사용 (모달 ID 포함)')
        // 특정 모달 ID로 닫기 시도
        import('../../utils/windowUtils').then(({ closeModalWindow }) => {
          const success = closeModalWindow(props.modalId)
          console.log('🎯 특정 모달 닫기 결과:', success)
          if (!success) {
            console.log('🔄 일반 closeWindow 시도')
            closeWindow()
          }
        }).catch(error => {
          console.error('❌ 모달 닫기 import 실패:', error)
          closeWindow()
        })
      }
    } else {
      // 일반 모드
      console.log('🔲 일반 창 닫기')
      closeWindow()
    }
  } catch (error) {
    console.error('❌ 닫기 처리 중 오류:', error)
    closeWindow()
  }
}
const handleClose = () => {
  performClose()
}

onMounted(async () => {
  console.log('SelectScheduleContent 마운트됨')

  // 🆕 모드 감지 로직 추가
  isPopupWindow.value = window.opener !== null ||
    window.location.search.includes('popup=true') ||
    window.location.pathname.includes('/popup/')

  isModalMode.value = !!props.modalId ||
    window.location.search.includes('modal=true') ||
    window.location.pathname.includes('/modal/')

  console.log('🔍 모드 감지:', {
    isPopupWindow: isPopupWindow.value,
    isModalMode: isModalMode.value,
    modalId: props.modalId
  })

  // 🆕 모달 모드인 경우 ModalManager에 등록
  if (isModalMode.value && props.modalId) {
    console.log('📝 ModalManager에 모달 등록 시도:', props.modalId)

    try {
      // ModalManager import
      const { ModalManager } = await import('../../utils/windowUtils')

      ModalManager.getInstance().registerModal(props.modalId, () => {
        console.log('🚪 ModalManager를 통한 닫기 실행:', props.modalId)
        performClose()
      })

      console.log('✅ ModalManager 등록 완료:', props.modalId)
    } catch (error) {
      console.error('❌ ModalManager 등록 실패:', error)
    }
  }

  // 기존 데이터 로드 로직
  try {
    console.log('🚀 서버에서 패스 스케줄 데이터 로드 시작')
    const success = await passScheduleStore.fetchScheduleDataFromServer()

    if (success) {
      console.log('✅ 패스 스케줄 데이터 로드 성공:', scheduleData.value.length, '개')
      console.log('🔍 겹치는 스케줄 그룹:', overlappingGroups.value)
    } else {
      console.log('⚠️ 패스 스케줄 데이터 없음')
    }
  } catch (error) {
    console.error('❌ 패스 스케줄 데이터 로드 실패:', error)

    // 🔧 $q 존재 확인 후 알림 처리
    if ($q && $q.notify) {
      $q.notify({
        type: 'negative',
        message: '패스 스케줄 데이터 로드에 실패했습니다',
      })
    }
  }
})

// 🆕 컴포넌트 언마운트 시 모달 해제 추가
onUnmounted(async () => {
  console.log('🧹 SelectScheduleContent 언마운트')

  if (isModalMode.value && props.modalId) {
    try {
      console.log('🗑️ ModalManager에서 모달 해제 시도:', props.modalId)

      const { ModalManager } = await import('../../utils/windowUtils')
      const unregistered = ModalManager.getInstance().unregisterModal(props.modalId)
      console.log('🗑️ ModalManager 해제 결과:', unregistered)
    } catch (error) {
      console.error('❌ 모달 해제 중 오류:', error)
    }
  }
})


</script>
<style scoped>
/* ✅ X 아이콘 위치 및 스타일 */
.close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 100;
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 50%;
  width: 32px;
  height: 32px;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.select-schedule-content {
  position: relative;
  /* X 아이콘 절대 위치를 위해 추가 */
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
  flex-wrap: wrap;
  gap: 1rem;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 200px;
}

/* ✅ 겹침 경고 정보 스타일 */
.overlap-warning {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: rgba(255, 152, 0, 0.1);
  border-radius: 4px;
  border-left: 3px solid #ff9800;
  flex-shrink: 0;
}

.button-group {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  flex-shrink: 0;
}

/* ✅ 버튼 너비 통일 및 확대 */
.action-btn {

  min-width: 120px;
  width: 120px;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.schedule-table {
  background-color: var(--q-dark) !important;
  color: white !important;
  flex: 1;
  width: 100%;
}

/* ✅ 가상 스크롤 및 고정 헤더 스타일 */
.schedule-table :deep(.q-table__container) {
  background-color: var(--q-dark) !important;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  max-height: 100%;
}

/* ✅ 테이블 헤더 고정 및 불투명 배경 설정 */
.schedule-table :deep(.q-table thead) {
  background-color: #1d1d1d !important;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.schedule-table :deep(.q-table thead th) {
  background-color: #1d1d1d !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important;
  position: sticky;
  top: 0;
  z-index: 10;
  font-weight: 600;
  padding: 12px 8px;
}

/* ✅ 헤더 호버 효과 */
.schedule-table :deep(.q-table thead th:hover) {
  background-color: #2a2a2a !important;
}

/* ✅ 테이블 바디 스크롤 영역 */
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
  background-color: rgba(25, 118, 210, 0.2) !important;
  border-left: 3px solid #1976d2 !important;
}

/* ✅ 겹치는 스케줄 행 스타일 (주황색) */
.schedule-table :deep(.q-table tbody tr.overlapping-row) {
  background-color: rgba(255, 152, 0, 0.15) !important;
  border-left: 3px solid #ff9800 !important;
}

.schedule-table :deep(.q-table tbody tr.overlapping-row:hover) {
  background-color: rgba(255, 152, 0, 0.25) !important;
}

/* ✅ 겹치는 스케줄이 선택된 경우 */
.schedule-table :deep(.q-table tbody tr.overlapping-row.selected) {
  background-color: rgba(255, 152, 0, 0.3) !important;
  border-left: 3px solid #ff9800 !important;
}

/* ✅ 선택 불가능한 행 스타일 */
.schedule-table :deep(.q-table tbody tr.disabled-row) {
  opacity: 0.6;
  background-color: rgba(255, 152, 0, 0.1) !important;
}

.schedule-table :deep(.q-table tbody tr.disabled-row:hover) {
  background-color: rgba(255, 152, 0, 0.15) !important;
  cursor: not-allowed;
}

.schedule-table :deep(.q-table tbody td) {
  background-color: transparent !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
  padding: 8px;
}

/* ✅ 시간 관련 셀 내용 폰트 크기 증가 */
.schedule-table :deep(.q-table tbody td[data-col="startTime"]),
.schedule-table :deep(.q-table tbody td[data-col="endTime"]) {
  font-size: 14px !important;
  font-weight: 600 !important;
  padding: 10px 8px !important;
}

/* ✅ 지속시간 셀 내용 폰트 크기 증가 */
.schedule-table :deep(.q-table tbody td[data-col="duration"]) {
  font-size: 14px !important;
  font-weight: 600 !important;
  padding: 10px 8px !important;
}

/* ✅ 최대고도 셀 내용 폰트 크기 증가 */
.schedule-table :deep(.q-table tbody td[data-col="maxElevation"]) {
  font-size: 14px !important;
  font-weight: 600 !important;
  padding: 10px 8px !important;
}

/* ✅ 체크박스 영역 완전 차단 스타일 */
.schedule-table :deep(.q-table tbody td[data-col="selection"]) {
  pointer-events: none !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}

/* ✅ 체크박스 자체도 완전 차단 */
.schedule-table :deep(.schedule-checkbox) {
  pointer-events: none !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
  color: #1976d2 !important;
}

.schedule-table :deep(.schedule-checkbox .q-checkbox__inner) {
  pointer-events: none !important;
  user-select: none !important;
  color: #1976d2 !important;
}

.schedule-table :deep(.schedule-checkbox .q-checkbox__bg) {
  pointer-events: none !important;
  user-select: none !important;
}

/* ✅ 비활성화된 체크박스 스타일 강화 */
.schedule-table :deep(.schedule-checkbox.disabled) {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}

.schedule-table :deep(.schedule-checkbox.disabled .q-checkbox__inner) {
  color: #666 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}

.schedule-table :deep(.schedule-checkbox.disabled:hover) {
  opacity: 0.4 !important;
}

/* ✅ 겹치는 스케줄의 체크박스 스타일 */
.schedule-table :deep(.overlapping-row .schedule-checkbox) {
  color: #ff9800 !important;
  pointer-events: none !important;
}

.schedule-table :deep(.overlapping-row .schedule-checkbox .q-checkbox__inner) {
  color: #ff9800 !important;
  pointer-events: none !important;
}

/* ✅ 겹치는 스케줄의 비활성화된 체크박스 */
.schedule-table :deep(.overlapping-row .schedule-checkbox.disabled) {
  color: #ff9800 !important;
  opacity: 0.3 !important;
  pointer-events: none !important;
}

.schedule-table :deep(.overlapping-row .schedule-checkbox.disabled .q-checkbox__inner) {
  color: #ff9800 !important;
  opacity: 0.3 !important;
  pointer-events: none !important;
}

/* ✅ 선택 불가능한 행의 체크박스 영역 완전 차단 */
.schedule-table :deep(.disabled-row .schedule-checkbox) {
  pointer-events: none !important;
  opacity: 0.3 !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}

.schedule-table :deep(.disabled-row td[data-col="selection"]) {
  pointer-events: none !important;
  cursor: not-allowed !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}

/* ✅ 체크박스 차단 표시 */
.schedule-table :deep(.checkbox-blocked) {
  position: relative;
}

.schedule-table :deep(.checkbox-blocked::after) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 0, 0, 0.1);
  pointer-events: none;
  border-radius: 2px;
}

/* ✅ 모든 체크박스 관련 요소 터치/마우스 이벤트 완전 차단 */
.schedule-table :deep(.q-checkbox),
.schedule-table :deep(.q-checkbox *),
.schedule-table :deep(.q-checkbox__inner),
.schedule-table :deep(.q-checkbox__bg),
.schedule-table :deep(.q-checkbox__svg),
.schedule-table :deep(.q-checkbox__truthy),
.schedule-table :deep(.q-checkbox__falsy) {
  pointer-events: none !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
  -webkit-touch-callout: none !important;
  -webkit-tap-highlight-color: transparent !important;
}

/* ✅ 체크박스 셀 전체 터치/마우스 이벤트 차단 */
.schedule-table :deep(td[data-col="selection"]) {
  pointer-events: none !important;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
  -webkit-touch-callout: none !important;
  -webkit-tap-highlight-color: transparent !important;
}

/* ✅ 모바일에서 터치 이벤트 완전 차단 */
@media (max-width: 768px) {

  .schedule-table :deep(.q-checkbox),
  .schedule-table :deep(.q-checkbox *),
  .schedule-table :deep(td[data-col="selection"]) {
    -webkit-touch-callout: none !important;
    -webkit-tap-highlight-color: transparent !important;
    touch-action: none !important;
    pointer-events: none !important;
  }
}

/* ✅ 위성 ID 칩 스타일 */
.schedule-table :deep(.satellite-id-chip) {
  font-size: 14px !important;
  font-weight: 600 !important;
  padding: 8px 12px !important;
  min-height: 32px !important;
  border-radius: 6px !important;
}

.schedule-table :deep(.satellite-id-chip .q-chip__content) {
  padding: 0 !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px;
}

/* ✅ 위성 ID 칩 호버 효과 */
.schedule-table :deep(.satellite-id-chip:hover) {
  transform: scale(1.05);
  transition: transform 0.2s ease;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.3);
}

/* ✅ Azimuth 각도 셀 스타일 */
.schedule-table :deep(.angle-cell) {
  padding: 6px 10px !important;
  vertical-align: middle;
}

.angle-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 50px;
  justify-content: center;
}

.angle-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px !important;
  line-height: 1.3;
}

.angle-label {
  color: rgba(255, 255, 255, 0.7);
  font-weight: 600 !important;
  min-width: 35px;
  font-size: 13px !important;
}

.angle-value {
  color: white;
  font-weight: 700 !important;
  text-align: right;
  font-size: 14px !important;
}

.start-angle .angle-value {
  color: #4caf50;
  font-size: 14px !important;
  font-weight: 700 !important;
}

.end-angle .angle-value {
  color: #ff9800;
  font-size: 14px !important;
  font-weight: 700 !important;
}

/* ✅ 가상 스크롤 컨테이너 스타일 */
.schedule-table :deep(.q-virtual-scroll) {
  max-height: 100%;
}

.schedule-table :deep(.q-virtual-scroll__content) {
  background-color: var(--q-dark) !important;
}

/* ✅ 스크롤바 스타일링 */
.schedule-table :deep(.q-scrollarea__thumb) {
  background-color: rgba(255, 255, 255, 0.3) !important;
  border-radius: 4px;
}

.schedule-table :deep(.q-scrollarea__bar) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

/* ✅ 테이블 전체 스크롤 영역 스타일 */
.schedule-table :deep(.q-table__middle) {
  overflow-y: auto;
  max-height: 100%;
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

/* ✅ 페이지네이션 숨기기 */
.schedule-table :deep(.q-table__bottom) {
  display: none !important;
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

  .content-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 0.5rem;
  }

  .overlap-warning {
    order: -1;
    margin-bottom: 0.5rem;
    width: 100%;
  }

  .selection-info {
    justify-content: center;
    min-width: unset;
    width: 100%;
  }

  .button-group {
    justify-content: center;
    width: 100%;
  }

  .action-btn {
    min-width: 100px;
    width: 100px;
    height: 38px;
    font-size: 13px;
  }

  .schedule-table :deep(.q-table thead th) {
    font-size: 12px;
    padding: 8px 4px;
    background-color: #1d1d1d !important;
  }

  .schedule-table :deep(.q-table tbody td) {
    font-size: 12px;
    padding: 8px 4px;
  }

  .schedule-table :deep(.q-table tbody td[data-col="startTime"]),
  .schedule-table :deep(.q-table tbody td[data-col="endTime"]),
  .schedule-table :deep(.q-table tbody td[data-col="duration"]),
  .schedule-table :deep(.q-table tbody td[data-col="maxElevation"]) {
    font-size: 13px !important;
    font-weight: 600 !important;
  }

  .angle-container {
    gap: 2px;
    min-height: 40px;
  }

  .angle-line {
    font-size: 12px !important;
  }

  .angle-label {
    min-width: 30px;
    font-size: 12px !important;
  }

  .angle-value {
    font-size: 13px !important;
  }

  /* ✅ Index 컬럼 스타일 */
  .schedule-table :deep(.q-table tbody td[data-col="index"]) {
    font-size: 13px !important;
    font-weight: 600 !important;
  }
}

@media (max-width: 480px) {
  .select-schedule-content {
    background-color: var(--q-dark) !important;
    padding: 0.25rem;
  }

  .content-header {
    margin-bottom: 0.5rem;
  }

  .content-footer {
    margin-top: 0.5rem;
    padding-top: 0.5rem;
  }

  .action-btn {

    min-width: 90px;
    width: 90px;
    height: 36px;
    font-size: 12px;
  }

  .schedule-table :deep(.q-table thead th) {
    background-color: #1d1d1d !important;
  }

  .schedule-table :deep(.q-table tbody td[data-col="startTime"]),
  .schedule-table :deep(.q-table tbody td[data-col="endTime"]),
  .schedule-table :deep(.q-table tbody td[data-col="duration"]),
  .schedule-table :deep(.q-table tbody td[data-col="maxElevation"]) {
    font-size: 12px !important;
    font-weight: 600 !important;
  }

  .angle-line {
    font-size: 11px !important;
  }

  .angle-label {
    font-size: 11px !important;
  }

  .angle-value {
    font-size: 12px !important;
  }

  .overlap-warning {
    padding: 6px 8px;
    font-size: 12px;
  }

  /* ✅ Index 컬럼 스타일 */
  .schedule-table :deep(.q-table tbody td[data-col="index"]) {
    font-size: 12px !important;
    font-weight: 600 !important;
  }

  .index-value {
    padding: 2px 6px;
    font-size: 12px;
  }
}

/* ✅ 큰 화면에서 더 넓은 버튼 */
@media (min-width: 1200px) {
  .action-btn {
    min-width: 140px;
    width: 140px;
    height: 42px;
    font-size: 15px;
  }

  .button-group {
    gap: 1.5rem;
  }
}
</style>
