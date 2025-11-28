<template>
  <div class="select-schedule-content">
    <!-- ✅ X 아이콘 추가 -->
    <q-btn flat round dense icon="close" size="sm" @click="handleClose" class="close-btn">
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
      <q-table flat bordered dark :rows="scheduleData" :columns="scheduleColumns" row-key="index" :loading="loading"
        v-model:selected="selectedRows" selection="multiple" class="schedule-table"
        style="height: 500px; background-color: var(--theme-card-background);" virtual-scroll
        :virtual-scroll-sticky-size-start="48" hide-pagination :rows-per-page-options="[0]" :row-class="getRowClass"
        :grid="false" :selected-rows-label="getSelectedLabel" dense>

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

        <!-- ✅ 헤더 체크박스 - 전체 선택/해제 -->
        <template v-slot:header-selection>
          <q-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate"
            @update:model-value="toggleSelectAll" color="primary" class="header-checkbox" />
        </template>

        <!-- ✅ 체크박스 컬럼 - 선택 가능 여부만 제어 -->
        <template v-slot:body-cell-selection="props">
          <q-td>
            <q-checkbox :model-value="isScheduleSelected(props.row)" :disable="!canSelectSchedule(props.row)"
              :color="isScheduleOverlapping(props.row.no) ? 'warning' : 'primary'"
              @update:model-value="(val) => handleCheckboxChange(props.row, val)" class="schedule-checkbox" />
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

        <!-- ✅ 위성 이름 컬럼 템플릿 (KEYHOLE 배지 포함) -->
        <template v-slot:body-cell-satelliteName="props">
          <q-td :props="props" class="text-center satellite-name-cell">
            <div class="satellite-name-container">
              <div class="satellite-name-text">{{ props.value || props.row?.satelliteId || '이름 없음' }}</div>
              <q-badge v-if="props.row?.IsKeyhole || props.row?.isKeyhole" color="red" class="keyhole-badge"
                label="KEYHOLE" />
            </div>
          </q-td>
        </template>

        <!-- ✅ Azimuth 각도 컬럼 템플릿 (Keyhole 여부에 따라 동적 값 표시) -->
        <template v-slot:body-cell-azimuthAngles="props">
          <q-td :props="props" class="angle-cell">
            <div class="angle-container">
              <div class="angle-line start-angle">
                <span class="angle-label">시작:</span>
                <span class="angle-value">{{ formatAngle(props.value?.start) }}</span>
              </div>
              <div class="angle-line end-angle">
                <span class="angle-label">종료:</span>
                <span class="angle-value">{{ formatAngle(props.value?.end) }}</span>
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

        <!-- ✅ Elevation 각도 컬럼 템플릿 (Keyhole 여부에 따라 동적 값 표시) -->
        <template v-slot:body-cell-elevationAngles="props">
          <q-td :props="props" class="angle-cell">
            <div class="angle-container">
              <div class="angle-line start-angle">
                <span class="angle-label">시작:</span>
                <span class="angle-value">{{ formatAngle(props.value?.start) }}</span>
              </div>
              <div class="angle-line end-angle">
                <span class="angle-label">종료:</span>
                <span class="angle-value">{{ formatAngle(props.value?.end) }}</span>
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ Keyhole 정보 컬럼 템플릿 -->
        <template v-slot:body-cell-isKeyhole="props">
          <q-td :props="props" class="keyhole-cell">
            <q-badge v-if="props.value" color="red" label="KEYHOLE" class="keyhole-badge" />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- ✅ RecommendedTrainAngle 컬럼 템플릿 -->
        <template v-slot:body-cell-recommendedTrainAngle="props">
          <q-td :props="props" class="train-angle-cell">
            <span v-if="props.row.IsKeyhole && props.value" class="text-positive text-weight-bold">
              {{ safeToFixed(props.value, 6) }}°
            </span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- ✅ 2축 최대 고도 템플릿 (Original) -->
        <template v-slot:body-cell-OriginalMaxElevation="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-blue-3">
                {{ safeToFixed(props.value, 6) }}°
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ 3축 최대 고도 템플릿 (Train=0, ±270°, 항상 고정) -->
        <template v-slot:body-cell-Train0MaxElevation="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-green-3">
                {{ safeToFixed(props.value, 6) }}°
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ FinalTransformed 최대 고도 템플릿 (Keyhole에 따라 다른 값 표시) -->
        <template v-slot:body-cell-MaxElevation="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold" :class="props.row?.IsKeyhole ? 'text-red' : 'text-green-3'">
                {{ safeToFixed(
                  props.row?.IsKeyhole
                    ? (props.row?.KeyholeFinalTransformedMaxElevation ?? props.value ?? 0)
                    : (props.value ?? 0),
                  6
                ) }}°
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ 2축 최대 Az 속도 템플릿 -->
        <template v-slot:body-cell-OriginalMaxAzRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-blue-3">
                {{ safeToFixed(props.value, 6) }}°/s
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ 3축 최대 Az 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
        <template v-slot:body-cell-Train0MaxAzRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-green-3">
                {{ safeToFixed(props.value, 6) }}°/s
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ FinalTransformed 최대 Az 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
        <template v-slot:body-cell-FinalTransformedMaxAzRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold" :class="props.row?.IsKeyhole ? 'text-red' : 'text-green-3'">
                {{ safeToFixed(
                  props.row?.IsKeyhole
                    ? (props.row?.KeyholeFinalTransformedMaxAzRate ?? props.value ?? 0)
                    : (props.value ?? 0),
                  6
                ) }}°/s
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ 2축 최대 El 속도 템플릿 -->
        <template v-slot:body-cell-OriginalMaxElRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-blue-3">
                {{ safeToFixed(props.value, 6) }}°/s
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ 3축 최대 El 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
        <template v-slot:body-cell-Train0MaxElRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold text-green-3">
                {{ safeToFixed(props.value, 6) }}°/s
              </div>
            </div>
          </q-td>
        </template>

        <!-- ✅ FinalTransformed 최대 El 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
        <template v-slot:body-cell-FinalTransformedMaxElRate="props">
          <q-td :props="props">
            <div class="text-center">
              <div class="text-weight-bold" :class="props.row?.IsKeyhole ? 'text-red' : 'text-green-3'">
                {{ safeToFixed(
                  props.row?.IsKeyhole
                    ? (props.row?.KeyholeFinalTransformedMaxElRate ?? props.value ?? 0)
                    : (props.value ?? 0),
                  6
                ) }}°/s
              </div>
            </div>
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
import { ref, onMounted, computed, getCurrentInstance, onUnmounted, watch, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleModeStore, type ScheduleItem } from '@/stores'
import type { QTableProps } from 'quasar'
import { formatToLocalTime } from '../../utils/times'
import { closeWindow } from '../../utils/windowUtils'

const $q = useQuasar()
const passScheduleStore = usePassScheduleModeStore()




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

  // ✅ no를 원본 그대로 유지하고, index는 정렬된 순서로 설정
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
      // ✅ no는 원본 그대로 유지 (백엔드 원본 No 값)
      // no: item.no, // 이미 spread로 포함됨
      index: sortedIndex + 1 // 정렬된 순서로 1부터 설정 (표시용)
    }
  })

  console.log('🔍 변환된 데이터 (처음 3개):', result.slice(0, 3))
  return result
})

const loading = computed(() => passScheduleStore.loading)

// 선택된 행 레이블 표시 함수
const getSelectedLabel = (count: number) => {
  return `${count}개의 스케줄이 선택되었습니다.`
}

const selectedRows = ref<ScheduleItem[]>([])

// ✅ 선택 상태 변경 시 localStorage에 저장 (Index 기준으로도 저장)
watch(
  () => selectedRows.value,
  (newSelected) => {
    // 선택된 스케줄을 index 순서로 정렬
    const sortedSelected = [...newSelected].sort((a, b) => {
      const indexA = a.index || 0
      const indexB = b.index || 0
      return indexA - indexB
    })

    // Index 목록 저장 (PassSchedulePage의 No와 매칭)
    const selectedIndexes = sortedSelected.map(s => s.index || s.no)
    // no 목록도 함께 저장 (호환성)
    const selectedNos = sortedSelected.map(s => s.no)

    try {
      const storageKey = 'pass-schedule-selected-nos'
      const dataToSave = {
        selectedNos, // no(원본) 저장 (호환성)
        selectedIndexes, // Index 저장 (새로운 방식)
        savedAt: Date.now()
      }
      localStorage.setItem(storageKey, JSON.stringify(dataToSave))
      console.log('💾 선택 상태 저장 (Index 기준):', {
        indexes: selectedIndexes,
        nos: selectedNos,
        count: selectedIndexes.length
      })
    } catch (error) {
      console.error('❌ 선택 상태 저장 실패:', error)
    }
  },
  { deep: true }
)

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

// ✅ 선택 가능 여부 확인 함수 - no 기준으로 수정
const canSelectSchedule = (schedule: ScheduleItem): boolean => {
  // 겹치지 않는 스케줄은 항상 선택 가능
  if (!isScheduleOverlapping(schedule.no)) {
    return true
  }

  // 겹치는 스케줄인 경우, 같은 그룹의 다른 스케줄이 선택되어 있는지 확인
  const overlappingGroup = getOverlappingGroup(schedule.no)

  // selectedRows에서 같은 그룹에 속하는 다른 스케줄이 선택되어 있는지 확인 (Index 기준)
  const otherSelectedInGroup = selectedRows.value.filter(selected => {
    // selectedRows의 스케줄이 scheduleData에서 어떤 스케줄인지 찾기
    const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
    // 같은 그룹에 있고, 다른 스케줄인지 확인
    return selectedSchedule &&
      overlappingGroup.includes(selectedSchedule.no) &&
      selected.index !== schedule.index
  })

  return otherSelectedInGroup.length === 0
}

// ✅ 체크박스 선택 상태 확인 함수 (Index 기준 비교)
const isScheduleSelected = (schedule: ScheduleItem): boolean => {
  return selectedRows.value.some(selected => selected.index === schedule.index)
}

// ✅ 체크박스 변경 핸들러
const handleCheckboxChange = (row: ScheduleItem, value: boolean) => {
  console.log('☑️ 체크박스 변경:', {
    satelliteName: row.satelliteName,
    index: row.index,
    value
  })

  if (value) {
    // 선택 시도
    if (!canSelectSchedule(row)) {
      console.log('❌ 선택 불가능한 스케줄')
      showOverlapWarning(row)
      // 선택 불가능하면 추가하지 않음
      return
    }

    // 겹침 검증
    const wouldOverlap = selectedRows.value.some(selected => {
      const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
      return selectedSchedule && checkTimeOverlap(row, selectedSchedule)
    })

    if (wouldOverlap) {
      console.log('❌ 시간 겹침 검증 실패')
      showOverlapWarning(row)
      return
    }

    // 이미 선택되어 있지 않으면 추가
    if (!selectedRows.value.some(s => s.index === row.index)) {
      selectedRows.value.push({ ...row })
      console.log('✅ 스케줄 선택 추가:', row.satelliteName)
    }
  } else {
    // 선택 해제
    const idx = selectedRows.value.findIndex(s => s.index === row.index)
    if (idx >= 0) {
      selectedRows.value.splice(idx, 1)
      console.log('✅ 스케줄 선택 해제:', row.satelliteName)
    }
  }

  // 선택 후 전체 선택된 항목 로그
  console.log('📋 현재 선택된 항목들:', selectedRows.value.map(s => ({
    index: s.index,
    no: s.no,
    name: s.satelliteName
  })))
}

// ✅ 전체 선택 상태 확인 (Index 기준)
const isAllSelected = computed(() => {
  if (scheduleData.value.length === 0) return false
  if (selectedRows.value.length === 0) return false

  // 겹치지 않고 선택 가능한 스케줄만 카운트
  const selectableSchedules = scheduleData.value.filter(schedule =>
    !isScheduleOverlapping(schedule.no)
  )

  if (selectableSchedules.length === 0) return false

  // 선택 가능한 모든 스케줄이 선택되었는지 확인
  const allSelected = selectableSchedules.every(schedule =>
    selectedRows.value.some(selected => selected.index === schedule.index)
  )

  console.log('🔍 isAllSelected:', {
    allSelected,
    selectableCount: selectableSchedules.length,
    selectedCount: selectedRows.value.length
  })

  return allSelected
})

// ✅ 일부 선택 상태 확인 (indeterminate) - Index 기준
const isIndeterminate = computed(() => {
  if (scheduleData.value.length === 0) return false
  if (selectedRows.value.length === 0) return false

  // 겹치지 않고 선택 가능한 스케줄만 카운트
  const selectableSchedules = scheduleData.value.filter(schedule =>
    !isScheduleOverlapping(schedule.no)
  )

  if (selectableSchedules.length === 0) return false

  const selectedCount = selectableSchedules.filter(schedule =>
    selectedRows.value.some(selected => selected.index === schedule.index)
  ).length

  const isIndeterminate = selectedCount > 0 && selectedCount < selectableSchedules.length

  console.log('🔍 isIndeterminate:', {
    isIndeterminate,
    selectedCount,
    selectableCount: selectableSchedules.length
  })

  return isIndeterminate
})

// ✅ 전체 선택/해제 토글
const toggleSelectAll = (value: boolean) => {
  console.log('🔄 전체 선택/해제:', value, '현재 선택:', selectedRows.value.length)

  if (value) {
    // 전체 선택
    console.log('📋 전체 선택 시작')

    // 선택 가능한 모든 스케줄 찾기 (겹치지 않는 것만)
    const selectableSchedules = scheduleData.value.filter(schedule => {
      // 이미 선택된 항목은 제외
      if (isScheduleSelected(schedule)) {
        console.log('⏭️ 이미 선택됨:', schedule.index, schedule.satelliteName)
        return false
      }

      // 겹치는 스케줄은 제외
      if (isScheduleOverlapping(schedule.no)) {
        console.log('⚠️ 겹침으로 제외:', schedule.index, schedule.satelliteName)
        return false
      }

      return true
    })

    console.log('✅ 선택 가능한 스케줄:', selectableSchedules.length, '개')

    // 시간 겹침 검증을 통과한 스케줄만 추가
    const validSchedules: ScheduleItem[] = []

    selectableSchedules.forEach(schedule => {
      // 현재 선택된 항목들 + 이미 추가하려는 항목들과 시간 겹침 체크
      const wouldOverlap = [...selectedRows.value, ...validSchedules].some(selected =>
        checkTimeOverlap(schedule, selected)
      )

      if (!wouldOverlap) {
        validSchedules.push(schedule)
        console.log('➕ 추가:', schedule.index, schedule.satelliteName)
      } else {
        console.log('⚠️ 시간 겹침으로 제외:', schedule.index, schedule.satelliteName)
      }
    })

    // 추가
    selectedRows.value.push(...validSchedules.map(s => ({ ...s })))
    console.log('✅ 전체 선택 완료:', validSchedules.length, '개 추가, 총', selectedRows.value.length, '개 선택됨')
  } else {
    // 전체 해제
    console.log('🗑️ 전체 해제 실행')
    selectedRows.value = []
    console.log('✅ 전체 해제 완료')
  }
}


// ✅ 겹침 경고 메시지 표시 함수 - no 기준으로 수정
const showOverlapWarning = (row: ScheduleItem) => {
  const overlappingGroup = getOverlappingGroup(row.no)

  // selectedRows의 스케줄이 scheduleData에서 어떤 스케줄인지 찾아서 비교 (Index 기준)
  const selectedInGroup = selectedRows.value.filter(selected => {
    const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
    return selectedSchedule && overlappingGroup.includes(selectedSchedule.no)
  })

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


// 테이블 컬럼 정의
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'index', label: 'Index', field: 'index', align: 'left' as const, sortable: true, style: 'width: 80px' },
  { name: 'no', label: 'No', field: 'no', align: 'left' as const, sortable: true, style: 'width: 70px' },
  { name: 'satelliteId', label: '위성 ID', field: 'satelliteId', align: 'center' as const, sortable: true, style: 'width: 120px' },
  { name: 'satelliteName', label: '위성명', field: 'satelliteName', align: 'center' as const, sortable: true, style: 'min-width: 150px' },
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
    format: (val) => formatDuration(val),
    style: 'width: 100px'
  },
  // ✅ 2축 최대 고도 (Original)
  {
    name: 'OriginalMaxElevation',
    label: '2축 최대 고도 (°)',
    field: 'OriginalMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 130px'
  },
  // ✅ 3축 최대 고도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElevation',
    label: '3축 최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 130px'
  },
  // ✅ FinalTransformed 최대 고도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center' as const,
    sortable: true,
    style: 'width: 120px'
  },
  // ✅ 2축 최대 Az 속도
  {
    name: 'OriginalMaxAzRate',
    label: '2축 최대 Az 속도 (°/s)',
    field: 'OriginalMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 150px'
  },
  // ✅ 3축 최대 Az 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxAzRate',
    label: '3축 최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 150px'
  },
  // ✅ FinalTransformed 최대 Az 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxAzRate',
    label: '최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px'
  },
  // ✅ 2축 최대 El 속도
  {
    name: 'OriginalMaxElRate',
    label: '2축 최대 El 속도 (°/s)',
    field: 'OriginalMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 150px'
  },
  // ✅ 3축 최대 El 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElRate',
    label: '3축 최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 150px'
  },
  // ✅ FinalTransformed 최대 El 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxElRate',
    label: '최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center' as const,
    sortable: true,
    style: 'width: 140px'
  },
  // ✅ Azimuth 각도 컬럼 (Keyhole 여부에 따라 동적 값 표시)
  {
    name: 'azimuthAngles',
    label: 'Azimuth 각도',
    field: (row: ScheduleItem) => {
      // Keyhole일 경우: KeyholeFinalTransformed 값 사용
      // Keyhole 아닐 경우: FinalTransformed 값 사용
      const isKeyhole = row.IsKeyhole || row.isKeyhole || false
      if (isKeyhole) {
        return {
          start: row.KeyholeFinalTransformedStartAzimuth ?? row.FinalTransformedStartAzimuth ?? row.startAzimuthAngle ?? 0,
          end: row.KeyholeFinalTransformedEndAzimuth ?? row.FinalTransformedEndAzimuth ?? row.endAzimuthAngle ?? 0
        }
      } else {
        return {
          start: row.FinalTransformedStartAzimuth ?? row.startAzimuthAngle ?? 0,
          end: row.FinalTransformedEndAzimuth ?? row.endAzimuthAngle ?? 0
        }
      }
    },
    align: 'center' as const,
    sortable: false,
    style: 'width: 140px'
  },
  // ✅ Elevation 각도 컬럼 추가 (Keyhole 여부에 따라 동적 값 표시)
  {
    name: 'elevationAngles',
    label: 'Elevation 각도',
    field: (row: ScheduleItem) => {
      // Keyhole일 경우: KeyholeFinalTransformed 값 사용
      // Keyhole 아닐 경우: FinalTransformed 값 사용
      const isKeyhole = row.IsKeyhole || row.isKeyhole || false
      if (isKeyhole) {
        return {
          start: row.KeyholeFinalTransformedStartElevation ?? row.FinalTransformedStartElevation ?? row.startElevationAngle ?? 0,
          end: row.KeyholeFinalTransformedEndElevation ?? row.FinalTransformedEndElevation ?? row.endElevationAngle ?? 0
        }
      } else {
        return {
          start: row.FinalTransformedStartElevation ?? row.startElevationAngle ?? 0,
          end: row.FinalTransformedEndElevation ?? row.endElevationAngle ?? 0
        }
      }
    },
    align: 'center' as const,
    sortable: false,
    style: 'width: 140px'
  },
  // ✅ Keyhole 정보 컬럼 추가
  {
    name: 'isKeyhole',
    label: 'KEYHOLE',
    field: 'IsKeyhole',
    align: 'center' as const,
    sortable: true,
    style: 'width: 100px'
  },
  {
    name: 'recommendedTrainAngle',
    label: 'Train 각도 (°)',
    field: 'RecommendedTrainAngle',
    align: 'center' as const,
    sortable: true,
    style: 'width: 110px'
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

// ✅ 안전한 숫자 포맷팅 헬퍼 함수 (EphemerisDesignationPage.vue 참고)
const safeToFixed = (value: unknown, decimals: number = 6): string => {
  if (typeof value === 'number' && !isNaN(value)) {
    return value.toFixed(decimals)
  }

  // 문자열이나 숫자 문자열만 파싱 시도
  if (typeof value === 'string' || typeof value === 'number') {
    const parsed = parseFloat(String(value))
    if (!isNaN(parsed)) {
      return parsed.toFixed(decimals)
    }
  }

  return '-'
}

// ✅ Duration 포맷 함수 추가 (ISO 8601 Duration 형식 파싱)
const formatDuration = (duration: string): string => {
  if (!duration) return '0분 0초'

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration // 파싱 실패 시 원본 반환

  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = parseFloat(match[3] || '0')

  const parts: string[] = []
  if (hours > 0) parts.push(`${hours}시간`)
  if (minutes > 0) parts.push(`${minutes}분`)
  if (seconds > 0) parts.push(`${Math.round(seconds)}초`)

  return parts.length > 0 ? parts.join(' ') : '0분 0초'
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

  // ✅ Keyhole 위성 행 스타일 추가
  if (row.IsKeyhole || row.isKeyhole) {
    classes.push('keyhole-row')
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
    console.log('📋 선택된 스케줄 상세:', selectedRows.value.map(s => ({
      no: s.no,
      index: s.index,
      satelliteName: s.satelliteName,
      satelliteId: s.satelliteId,
      startTime: s.startTime,
      endTime: s.endTime
    })))

    // ✅ index를 no로 덮어쓰기 (고유 식별자로 사용)
    const schedulesWithIndexAsNo = selectedRows.value.map(s => ({
      ...s,
      no: s.index || s.no // index를 no로 사용
    }))

    console.log('🔄 index를 no로 변환:', schedulesWithIndexAsNo.map(s => ({
      no: s.no,
      index: s.index,
      satelliteName: s.satelliteName
    })))

    // 🔧 기존 목록 초기화 후 새 스케줄 추가
    const success = await passScheduleStore.replaceSelectedSchedules(schedulesWithIndexAsNo)

    console.log('🔍 replaceSelectedSchedules 결과:', success)
    console.log('🔍 Store 상태 확인:', {
      selectedScheduleListCount: passScheduleStore.selectedScheduleList.length,
      selectedSchedule: passScheduleStore.selectedSchedule?.satelliteName
    })

    if (success) {
      console.log('✅ 스케줄 목록 교체 완료:', {
        count: selectedRows.value.length,
        schedules: selectedRows.value.map(s => ({
          no: s.no, // 서버 원본 No 값
          index: s.index,
          name: s.satelliteName,
          satelliteId: s.satelliteId,
          startTime: s.startTime
        }))
      })

      // ✅ 선택 완료 시 localStorage에 저장 (Index 기준)
      const sortedSelected = [...selectedRows.value].sort((a, b) => {
        const indexA = a.index || 0
        const indexB = b.index || 0
        return indexA - indexB
      })

      const selectedIndexes = sortedSelected.map(s => s.index || s.no)
      const selectedNos = sortedSelected.map(s => s.no)

      try {
        const storageKey = 'pass-schedule-selected-nos'
        const dataToSave = {
          selectedNos, // no(원본) 저장 (호환성)
          selectedIndexes, // Index 저장 (새로운 방식)
          savedAt: Date.now()
        }
        localStorage.setItem(storageKey, JSON.stringify(dataToSave))
        console.log('💾 선택 완료 - 선택 상태 저장 (Index 기준):', {
          indexes: selectedIndexes,
          nos: selectedNos,
          count: selectedIndexes.length
        })
      } catch (error) {
        console.error('❌ 선택 상태 저장 실패:', error)
      }

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

      // ✅ 데이터 로드 후 이전 선택 상태 복원
      await nextTick() // scheduleData가 준비될 때까지 대기

      // ✅ passScheduleStore.selectedScheduleList를 직접 사용하여 복원
      const storeSelectedList = passScheduleStore.selectedScheduleList

      if (storeSelectedList.length > 0 && scheduleData.value.length > 0) {
        console.log('🔄 Store에서 선택된 스케줄 복원 시작:', {
          storeCount: storeSelectedList.length,
          scheduleDataCount: scheduleData.value.length,
          storeNos: storeSelectedList.map(s => ({ no: s.no, index: s.index, name: s.satelliteName })),
          scheduleDataNos: scheduleData.value.slice(0, 5).map(s => ({ no: s.no, index: s.index, name: s.satelliteName }))
        })

        // ✅ Store의 selectedScheduleList를 시간 순으로 정렬 (PassSchedulePage와 동일한 순서)
        const sortedStoreList = [...storeSelectedList].sort((a, b) => {
          try {
            return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
          } catch {
            return 0
          }
        })

        // ✅ Store에 저장된 index 값을 직접 사용하여 매칭
        const restoredSchedules: ScheduleItem[] = []

        sortedStoreList.forEach((storeSchedule) => {
          // ✅ storeSchedule의 index 값을 직접 사용 (순서가 아닌 저장된 값)
          const savedIndex = storeSchedule.index || storeSchedule.no

          // scheduleData에서 같은 index를 가진 스케줄 찾기
          const matchedSchedule = scheduleData.value.find(s => s.index === savedIndex)

          if (matchedSchedule) {
            console.log('✅ Index 기준 복원 매칭:', {
              savedIndex: savedIndex,
              scheduleIndex: matchedSchedule.index,
              scheduleNo: matchedSchedule.no,
              storeNo: storeSchedule.no,
              scheduleName: matchedSchedule.satelliteName
            })
            restoredSchedules.push(matchedSchedule)
          } else {
            console.warn('⚠️ Index 매칭 실패:', {
              savedIndex: savedIndex,
              storeNo: storeSchedule.no,
              scheduleDataIndexes: scheduleData.value.slice(0, 5).map(s => s.index)
            })
          }
        })

        if (restoredSchedules.length > 0) {
          console.log('✅ 복원 가능한 스케줄:', restoredSchedules.length, '개')

          // ✅ 겹침 검증 후 선택 가능한 항목만 복원
          const validSchedules: ScheduleItem[] = []

          restoredSchedules.forEach(schedule => {
            // 이미 선택되지 않은 경우만 확인 (index 기준)
            const alreadySelected = selectedRows.value.some(selected =>
              selected.index === schedule.index
            )

            if (alreadySelected) return

            // 겹침이 없는 경우
            if (!isScheduleOverlapping(schedule.no)) {
              validSchedules.push(schedule)
            } else {
              // 겹치는 항목은 같은 그룹에 다른 선택이 없을 때만 추가
              const overlappingGroup = getOverlappingGroup(schedule.no)
              const hasOtherSelected = selectedRows.value.some(selected => {
                const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
                return selectedSchedule &&
                  overlappingGroup.includes(selectedSchedule.no) &&
                  selected.index !== schedule.index
              })

              if (!hasOtherSelected) {
                // 추가 전에 겹침 체크
                const wouldOverlap = selectedRows.value.some(selected => {
                  const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
                  return selectedSchedule && checkTimeOverlap(schedule, selectedSchedule)
                })

                if (!wouldOverlap) {
                  validSchedules.push(schedule)
                }
              }
            }
          })

          // ✅ 유효한 스케줄들을 selectedRows에 추가 (객체 복사로 참조 분리)
          selectedRows.value.push(...validSchedules.map(s => ({ ...s })))

          console.log('✅ 이전 선택 상태 복원 완료:', {
            count: validSchedules.length,
            restoredNos: validSchedules.map(s => s.no),
            restoredIndexes: validSchedules.map(s => s.index)
          })
        } else {
          console.log('⚠️ 복원 가능한 스케줄 없음')
          console.log('🔍 Store 선택 목록 no (원본):', storeSelectedList.map(s => s.no))
          console.log('🔍 scheduleData의 no (원본):', scheduleData.value.slice(0, 5).map(s => s.no))
        }
      } else {
        // ✅ Store에 선택 목록이 없으면 localStorage에서 복원 시도
        const savedData = passScheduleStore.loadSelectedScheduleNosFromLocalStorage()
        const savedIndexes = passScheduleStore.loadSelectedScheduleIndexesFromLocalStorage()

        // ✅ Index가 있으면 Index 기준으로, 없으면 no 기준으로 복원 (하위 호환성)
        const useIndex = savedIndexes.length > 0 && savedIndexes.length === savedData.length

        if ((savedData.length > 0 || savedIndexes.length > 0) && scheduleData.value.length > 0) {
          console.log('🔄 localStorage에서 선택 상태 복원 시작:', {
            savedNosCount: savedData.length,
            savedIndexesCount: savedIndexes.length,
            useIndex,
            savedData: savedData,
            savedIndexes: savedIndexes,
            scheduleDataCount: scheduleData.value.length,
            scheduleDataNos: scheduleData.value.slice(0, 5).map(s => ({ no: s.no, index: s.index }))
          })

          let restoredSchedules: ScheduleItem[] = []

          if (useIndex && savedIndexes.length > 0) {
            // ✅ Index 기준으로 복원 (새로운 방식)
            savedIndexes.forEach((savedIndex) => {
              const matchedSchedule = scheduleData.value.find(s => s.index === savedIndex)
              if (matchedSchedule) {
                console.log('✅ localStorage Index 기준 복원 매칭:', {
                  savedIndex: savedIndex,
                  scheduleIndex: matchedSchedule.index,
                  scheduleNo: matchedSchedule.no,
                  scheduleName: matchedSchedule.satelliteName
                })
                restoredSchedules.push(matchedSchedule)
              }
            })
          } else {
            // ✅ no 기준으로 복원 (하위 호환성)
            restoredSchedules = scheduleData.value.filter(schedule => {
              const found = savedData.includes(schedule.no)
              if (found) {
                console.log('✅ localStorage no 기준 복원 매칭:', {
                  scheduleNo: schedule.no,
                  scheduleIndex: schedule.index,
                  savedNo: savedData.find(n => n === schedule.no)
                })
              }
              return found
            })
          }

          if (restoredSchedules.length > 0) {
            console.log('✅ 복원 가능한 스케줄:', restoredSchedules.length, '개')

            // ✅ 겹침 검증 후 선택 가능한 항목만 복원
            const validSchedules: ScheduleItem[] = []

            restoredSchedules.forEach(schedule => {
              // 이미 선택되지 않은 경우만 확인 (index 기준)
              const alreadySelected = selectedRows.value.some(selected =>
                selected.index === schedule.index
              )

              if (alreadySelected) return

              // 겹침이 없는 경우
              if (!isScheduleOverlapping(schedule.no)) {
                validSchedules.push(schedule)
              } else {
                // 겹치는 항목은 같은 그룹에 다른 선택이 없을 때만 추가
                const overlappingGroup = getOverlappingGroup(schedule.no)
                const hasOtherSelected = selectedRows.value.some(selected => {
                  const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
                  return selectedSchedule &&
                    overlappingGroup.includes(selectedSchedule.no) &&
                    selected.index !== schedule.index
                })

                if (!hasOtherSelected) {
                  const wouldOverlap = selectedRows.value.some(selected => {
                    const selectedSchedule = scheduleData.value.find(s => s.index === selected.index)
                    return selectedSchedule && checkTimeOverlap(schedule, selectedSchedule)
                  })

                  if (!wouldOverlap) {
                    validSchedules.push(schedule)
                  }
                }
              }
            })

            // ✅ 유효한 스케줄들을 selectedRows에 추가 (객체 복사로 참조 분리)
            selectedRows.value.push(...validSchedules.map(s => ({ ...s })))

            console.log('✅ localStorage에서 선택 상태 복원 완료:', {
              count: validSchedules.length,
              restoredNos: validSchedules.map(s => s.no),
              restoredIndexes: validSchedules.map(s => s.index)
            })
          }
        }
      }
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

  /* ✅ 기본 상태: 회색 배경에 흰색 X */
  background-color: rgba(0, 0, 0, 0.6) !important;
  color: white !important;
  border-radius: 50%;
  width: 32px;
  height: 32px;

  /* ✅ 호버 효과 강화 */
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.close-btn:hover {
  /* ✅ 호버 시: 더 진한 배경에 흰색 X 유지 */
  background-color: rgba(0, 0, 0, 0.8) !important;
  color: white !important;

  /* ✅ 호버 시 테두리 추가 */
  border: 2px solid rgba(255, 255, 255, 0.3);

  /* ✅ 호버 시 약간 확대 효과 */
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* ✅ 포커스 상태도 추가 */
.close-btn:focus {
  background-color: rgba(0, 0, 0, 0.8) !important;
  color: white !important;
  border: 2px solid rgba(255, 255, 255, 0.5);
  outline: none;
}

/* ✅ 활성 상태 (클릭 시) */
.close-btn:active {
  background-color: rgba(0, 0, 0, 0.9) !important;
  transform: scale(0.95);
}

.select-schedule-content {
  position: relative;
  /* X 아이콘 절대 위치를 위해 추가 */
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--theme-card-background) !important;
  color: white !important;
  padding: 1rem;
  overflow: hidden;
  box-sizing: border-box;

  /* ✅ 외곽 테두리 추가 */
  border: 2px solid var(--theme-border);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.content-header {
  flex-shrink: 0;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--theme-border);

  /* ✅ 헤더 배경 강화 */
  background-color: rgba(255, 255, 255, 0.02);
  padding: 1rem;
  border-radius: 6px 6px 0 0;
}

.content-body {
  flex: 1;
  overflow: hidden;
  /* ✅ 자식 요소에서 스크롤 처리 */
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
  border-top: 2px solid var(--theme-border);
  flex-wrap: wrap;
  gap: 1rem;

  /* ✅ 푸터 배경 강화 */
  background-color: rgba(255, 255, 255, 0.02);
  padding: 1rem;
  border-radius: 0 0 6px 6px;
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
  background-color: var(--theme-card-background) !important;
  color: white !important;
  flex: 1;
  width: 100%;

  /* ✅ 테이블 테두리 추가 */
  border: 1px solid var(--theme-border);
  border-radius: 6px;
}

/* ✅ 가상 스크롤 및 고정 헤더 스타일 */
.schedule-table :deep(.q-table__container) {
  background-color: var(--theme-card-background) !important;
  border: 1px solid var(--theme-border);
  border-radius: 6px;
  max-height: 100%;
  overflow: hidden;
  /* ✅ 컨테이너는 스크롤 없음, 하위 요소에서 처리 */
  display: flex;
  flex-direction: column;

  /* ✅ 내부 테두리 강화 */
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* ✅ 테이블 헤더 고정 및 불투명 배경 설정 */
.schedule-table :deep(.q-table thead) {
  background-color: var(--theme-card-background) !important;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.schedule-table :deep(.q-table thead th) {
  background-color: var(--theme-card-background) !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2) !important;
  position: sticky;
  top: 0;
  z-index: 10;
  font-weight: 600 !important;
  /* ✅ 헤더 폰트 굵기 증가 */
  padding: 12px 8px !important;
  /* ✅ 헤더 패딩 증가 */
  font-size: 13px !important;
  /* ✅ 헤더 폰트 크기 증가 */
}

/* ✅ 헤더 호버 효과 */
.schedule-table :deep(.q-table thead th:hover) {
  background-color: #2a2a2a !important;
}

/* ✅ 테이블 바디 스크롤 영역 */
.schedule-table :deep(.q-table tbody) {
  background-color: var(--theme-card-background) !important;
}

.schedule-table :deep(.q-table tbody tr) {
  background-color: var(--theme-card-background) !important;
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

/* ✅ Keyhole 위성 행 스타일 (빨간색 강조) */
.schedule-table :deep(.q-table tbody tr.keyhole-row) {
  background-color: rgba(244, 67, 54, 0.1) !important;
  border-left: 3px solid #f44336 !important;
}

.schedule-table :deep(.q-table tbody tr.keyhole-row:hover) {
  background-color: rgba(244, 67, 54, 0.2) !important;
}

.schedule-table :deep(.q-table tbody tr.keyhole-row.selected) {
  background-color: rgba(244, 67, 54, 0.25) !important;
  border-left: 3px solid #f44336 !important;
}

/* ✅ 위성 이름 셀 스타일 */
.schedule-table :deep(.satellite-name-cell) {
  padding: 8px 10px !important;
  vertical-align: middle;
}

/* ✅ 위성 이름 컨테이너 스타일 (세로 배치) */
.schedule-table :deep(.satellite-name-container) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 50px;
}

/* ✅ 위성 이름 텍스트 스타일 */
.schedule-table :deep(.satellite-name-text) {
  font-size: 13px !important;
  font-weight: 500 !important;
  color: white;
}

/* ✅ Keyhole 배지 스타일 */
.schedule-table :deep(.keyhole-badge) {
  font-weight: 700 !important;
  font-size: 11px !important;
  padding: 4px 8px !important;
  letter-spacing: 0.5px !important;
  margin-top: 2px;
}

/* ✅ Train 각도 셀 스타일 */
.schedule-table :deep(.train-angle-cell) {
  font-weight: 600 !important;
}

.schedule-table :deep(.q-table tbody td) {
  background-color: transparent !important;
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
  padding: 10px 8px;
  /* ✅ 상하 패딩 증가로 가독성 향상 */
  font-size: 13px;
  /* ✅ 폰트 크기 증가 */
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

/* ✅ 체크박스 셀 스타일 (이벤트 허용) */
.schedule-table :deep(.q-table tbody td[data-col="selection"]) {
  padding: 10px 8px;
}

/* ✅ 체크박스 스타일 (이벤트 허용) */
.schedule-table :deep(.schedule-checkbox) {
  color: #1976d2 !important;
}

.schedule-table :deep(.schedule-checkbox .q-checkbox__inner) {
  color: #1976d2 !important;
}

/* ✅ 비활성화된 체크박스 스타일 */
.schedule-table :deep(.q-checkbox.disabled) {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
}

.schedule-table :deep(.q-checkbox.disabled .q-checkbox__inner) {
  color: #666 !important;
  cursor: not-allowed !important;
}

/* ✅ 겹치는 스케줄의 체크박스 스타일 */
.schedule-table :deep(.overlapping-row .q-checkbox:not(.disabled)) {
  color: #ff9800 !important;
}

.schedule-table :deep(.overlapping-row .q-checkbox:not(.disabled) .q-checkbox__inner) {
  color: #ff9800 !important;
}

/* ✅ 겹치는 스케줄의 비활성화된 체크박스 */
.schedule-table :deep(.overlapping-row .q-checkbox.disabled) {
  color: #ff9800 !important;
  opacity: 0.3 !important;
}

.schedule-table :deep(.overlapping-row .q-checkbox.disabled .q-checkbox__inner) {
  color: #ff9800 !important;
  opacity: 0.3 !important;
}

/* ✅ 선택 불가능한 행의 체크박스 */
.schedule-table :deep(.disabled-row .q-checkbox) {
  opacity: 0.3 !important;
  cursor: not-allowed !important;
}

/* ✅ 체크박스 호버 효과 */
.schedule-table :deep(.q-checkbox:not(.disabled):hover) {
  opacity: 0.8;
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
  color: var(--theme-text-secondary);
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
  background-color: var(--theme-card-background) !important;
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
  overflow-x: auto;
  /* ✅ 가로 스크롤바 추가 */
  overflow-y: auto;
  /* ✅ 세로 스크롤바 유지 */
  max-height: 100%;
  flex: 1;
  min-width: 0;
}

/* ✅ 테이블 자체에 최소 너비 설정 (컬럼 총 너비보다 크게) */
.schedule-table :deep(.q-table) {
  min-width: 2000px;
  /* ✅ 컬럼들의 총 너비보다 큰 값 설정 */
  table-layout: auto;
  /* ✅ 컬럼 너비 자동 조정 */
  width: 100%;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
}

/* ✅ 로딩 및 빈 데이터 상태 배경 설정 */
.schedule-table :deep(.q-table__bottom--nodata) {
  background-color: var(--theme-card-background) !important;
  color: white !important;
}

.schedule-table :deep(.q-inner-loading) {
  background-color: rgba(0, 0, 0, 0.7) !important;
}

.schedule-table :deep(.q-spinner) {
  color: #2196f3 !important;
}

.schedule-table :deep(.full-width) {
  background-color: var(--theme-card-background) !important;
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
    background-color: var(--theme-card-background) !important;
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
    background-color: var(--theme-card-background) !important;
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
