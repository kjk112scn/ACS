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
      <!-- ✅ FIX: row-key를 uid 문자열로 변경 (함수 형태 → 문자열) - 단일 선택 문제 해결 -->
      <q-table flat bordered dark :rows="scheduleData" :columns="scheduleColumns" row-key="uid" :loading="loading"
        :selected="selectedRows" @update:selected="handleSelectionUpdate" selection="multiple" class="schedule-table"
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
              :color="isScheduleOverlapping(props.row) ? 'warning' : 'primary'"
              @update:model-value="(val) => handleCheckboxChange(props.row, val)" class="schedule-checkbox" />
            <q-tooltip v-if="!canSelectSchedule(props.row)" class="bg-warning text-black">
              시간이 겹치는 다른 스케줄이 이미 선택되어 있습니다
            </q-tooltip>
          </q-td>
        </template>

        <template v-slot:body-cell-startTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.value) }}
            <q-icon v-if="isScheduleOverlapping(props.row)" name="warning" color="warning" size="xs" class="q-ml-xs">
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

        <!-- ✅ MstId 컬럼 템플릿 추가 -->
        <template v-slot:body-cell-mstId="props">
          <q-td :props="props" class="mstid-cell">
            <span class="mstid-value">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- ✅ DetailId 컬럼 템플릿 추가 -->
        <template v-slot:body-cell-detailId="props">
          <q-td :props="props" class="detailid-cell">
            <span class="detailid-value">{{ props.value ?? 0 }}</span>
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

  // ✅ PassSchedule 데이터 구조 리팩토링: mstId, detailId 매핑 (index 필드 제거)
  const result = sortedData.map((item, sortedIndex) => {
    // 디버깅: 원본 item의 구조 확인
    if (sortedIndex < 3) {
      console.log(`🔍 Item ${sortedIndex}:`, {
        mstId: item.mstId,
        originalNo: item.no,
        satelliteName: item.satelliteName,
        allKeys: Object.keys(item)
      })
    }

    // ✅ index 필드 제거, mstId만 사용
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { index: _index, ...itemWithoutIndex } = item as ScheduleItem & { index?: number } // index 필드 제거 (타입 안전)

    return {
      ...itemWithoutIndex,
      // ✅ FIX: row-key용 고유 ID (원본 유지 - mstId_detailId 조합)
      uid: item.uid || `${item.mstId}_${item.detailId ?? 0}`,
      // ✅ 전역 고유 ID (필수) - fallback 제거, null이면 오류
      mstId: item.mstId,
      // ✅ Detail 구분자 (필수) - mstId와 함께 고유 식별
      detailId: item.detailId ?? 0,
      // ✅ UI 표시용 재순번 (1, 2, 3...)
      no: sortedIndex + 1
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

/**
 * 선택 상태 변경 시 localStorage에 저장
 *
 * PassSchedule 데이터 구조 리팩토링에 따라 selectedMstIds 저장.
 */
watch(
  () => selectedRows.value,
  (newSelected) => {
    // ✅ 선택된 스케줄을 mstId 순서로 정렬
    const sortedSelected = [...newSelected].sort((a, b) => {
      const mstIdA = a.mstId || 0
      const mstIdB = b.mstId || 0
      return mstIdA - mstIdB
    })

    // ✅ mstId와 detailId 조합으로 저장 (필수)
    const selectedMstIds = sortedSelected.map(s => s.mstId)
    const selectedDetailIds = sortedSelected.map(s => s.detailId ?? 0)
    // ✅ 하위 호환성을 위해 no도 함께 저장
    const selectedNos = sortedSelected.map(s => s.no)

    try {
      const storageKey = 'pass-schedule-selected-nos'
      const dataToSave = {
        selectedMstIds, // ✅ 전역 고유 ID (필수)
        selectedDetailIds, // ✅ Detail 구분자 (필수)
        selectedNos, // ✅ 하위 호환성
        savedAt: Date.now()
      }
      localStorage.setItem(storageKey, JSON.stringify(dataToSave))
      console.log('💾 선택 상태 저장 (mstId 기준):', {
        mstIds: selectedMstIds,
        nos: selectedNos,
        count: selectedMstIds.length
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

// ✅ 겹치는 스케줄 그룹 계산 - mstId + detailId 조합으로 고유 식별
const overlappingGroups = computed(() => {
  const data = scheduleData.value
  const groups: string[][] = []  // 스케줄 키: `${mstId}_${detailId}`
  const processed = new Set<string>()

  const getScheduleKey = (s: ScheduleItem) => `${s.mstId}_${s.detailId}`

  console.log('🔍 겹침 검사 시작 - 총', data.length, '개 스케줄')

  data.forEach((schedule, index) => {
    const scheduleKey = getScheduleKey(schedule)
    if (processed.has(scheduleKey)) return

    const overlappingSchedules = [scheduleKey]

    data.forEach((otherSchedule, otherIndex) => {
      const otherKey = getScheduleKey(otherSchedule)
      if (index !== otherIndex && !processed.has(otherKey)) {
        if (checkTimeOverlap(schedule, otherSchedule)) {
          overlappingSchedules.push(otherKey)
          console.log('🔍 겹침 발견:', {
            schedule1: `${schedule.satelliteName} (${scheduleKey}, ${schedule.startTime} ~ ${schedule.endTime})`,
            schedule2: `${otherSchedule.satelliteName} (${otherKey}, ${otherSchedule.startTime} ~ ${otherSchedule.endTime})`
          })
        }
      }
    })

    if (overlappingSchedules.length > 1) {
      groups.push(overlappingSchedules)
      overlappingSchedules.forEach(key => processed.add(key))
      console.log('✅ 겹침 그룹 생성:', overlappingSchedules)
    }
  })

  console.log('🔍 최종 겹침 그룹:', groups)
  return groups
})

// ✅ 특정 스케줄이 겹치는지 확인 - 직접 비교 방식 (mstId + detailId로 정확히 식별)
const isScheduleOverlapping = (schedule: ScheduleItem): boolean => {
  return scheduleData.value.some(other =>
    (other.mstId !== schedule.mstId || other.detailId !== schedule.detailId) &&
    checkTimeOverlap(schedule, other)
  )
}

// ✅ 특정 스케줄과 겹치는 모든 스케줄 가져오기 (직접 비교 방식)
const getOverlappingSchedules = (schedule: ScheduleItem): ScheduleItem[] => {
  return scheduleData.value.filter(other =>
    (other.mstId !== schedule.mstId || other.detailId !== schedule.detailId) &&
    checkTimeOverlap(schedule, other)
  )
}

// ✅ 선택 가능 여부 확인 함수 - 직접 비교 방식 (그룹화 의존 제거)
const canSelectSchedule = (schedule: ScheduleItem): boolean => {
  // 선택된 모든 스케줄과 직접 시간 겹침 검사
  return !selectedRows.value.some(selected => {
    // 자기 자신은 제외 (mstId + detailId 조합으로 비교)
    if (selected.mstId === schedule.mstId && selected.detailId === schedule.detailId) {
      return false
    }
    // 시간 겹침 직접 검사
    return checkTimeOverlap(schedule, selected)
  })
}

/**
 * 체크박스 선택 상태 확인 함수
 *
 * PassSchedule 데이터 구조 리팩토링에 따라 mstId와 detailId 조합으로 비교.
 *
 * @param schedule 확인할 스케줄
 * @returns 선택 여부
 */
const isScheduleSelected = (schedule: ScheduleItem): boolean => {
  // ✅ mstId와 detailId 조합으로 비교 (고유 식별)
  return selectedRows.value.some(selected =>
    selected.mstId === schedule.mstId && selected.detailId === schedule.detailId
  )
}

// ✅ q-table 선택 변경 핸들러 (행 클릭 시에도 검증 수행)
const handleSelectionUpdate = (newSelection: ScheduleItem[]) => {
  // 새로 추가된 항목 찾기
  const newItems = newSelection.filter(newItem =>
    !selectedRows.value.some(existing =>
      existing.mstId === newItem.mstId && existing.detailId === newItem.detailId
    )
  )

  // 제거된 항목 찾기
  const removedItems = selectedRows.value.filter(existing =>
    !newSelection.some(newItem =>
      newItem.mstId === existing.mstId && newItem.detailId === existing.detailId
    )
  )

  console.log('🔄 선택 변경 감지:', {
    새로추가: newItems.map(i => ({ name: i.satelliteName, mstId: i.mstId })),
    제거됨: removedItems.map(i => ({ name: i.satelliteName, mstId: i.mstId }))
  })

  // 새로 추가된 항목 각각에 대해 검증
  for (const item of newItems) {
    // canSelectSchedule 검증
    if (!canSelectSchedule(item)) {
      console.log('❌ 선택 불가 (겹침 그룹에서 이미 선택됨):', item.satelliteName)
      showOverlapWarning(item)
      continue // 건너뜀 - selectedRows에 추가하지 않음
    }

    // 현재까지 selectedRows + 이미 추가된 항목들과의 겹침 검증
    const wouldOverlap = selectedRows.value.some(selected => {
      const selectedSchedule = scheduleData.value.find(s =>
        s.mstId === selected.mstId && s.detailId === selected.detailId
      )
      return selectedSchedule && checkTimeOverlap(item, selectedSchedule)
    })

    if (wouldOverlap) {
      console.log('❌ 시간 겹침으로 선택 불가:', item.satelliteName)
      showOverlapWarning(item)
      continue // 건너뜀
    }

    // 검증 통과 - 추가
    if (!selectedRows.value.some(s => s.mstId === item.mstId && s.detailId === item.detailId)) {
      selectedRows.value.push({ ...item })
      console.log('✅ 스케줄 선택 추가:', item.satelliteName, `mstId=${item.mstId}`)
    }
  }

  // 제거된 항목 처리
  for (const item of removedItems) {
    const idx = selectedRows.value.findIndex(s =>
      s.mstId === item.mstId && s.detailId === item.detailId
    )
    if (idx >= 0) {
      selectedRows.value.splice(idx, 1)
      console.log('✅ 스케줄 선택 해제:', item.satelliteName)
    }
  }

  console.log('📋 현재 선택된 항목들:', selectedRows.value.map(s => ({
    mstId: s.mstId,
    name: s.satelliteName
  })))
}

// ✅ 체크박스 변경 핸들러
const handleCheckboxChange = (row: ScheduleItem, value: boolean) => {
    console.log('☑️ 체크박스 변경:', {
      satelliteName: row.satelliteName,
      mstId: row.mstId,
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
      // ✅ mstId와 detailId 조합으로 매칭
      const selectedSchedule = scheduleData.value.find(s =>
        s.mstId === selected.mstId && s.detailId === selected.detailId
      )
      return selectedSchedule && checkTimeOverlap(row, selectedSchedule)
    })

    if (wouldOverlap) {
      console.log('❌ 시간 겹침 검증 실패')
      showOverlapWarning(row)
      return
    }

    // 이미 선택되어 있지 않으면 추가
    if (!selectedRows.value.some(s => s.mstId === row.mstId && s.detailId === row.detailId)) {
      selectedRows.value.push({ ...row })
      console.log('✅ 스케줄 선택 추가:', row.satelliteName, `mstId=${row.mstId}, detailId=${row.detailId}`)
    }
  } else {
    // 선택 해제
    // ✅ mstId와 detailId 조합으로 찾기
    const idx = selectedRows.value.findIndex(s => s.mstId === row.mstId && s.detailId === row.detailId)
    if (idx >= 0) {
      selectedRows.value.splice(idx, 1)
      console.log('✅ 스케줄 선택 해제:', row.satelliteName)
    }
  }

  // 선택 후 전체 선택된 항목 로그
  console.log('📋 현재 선택된 항목들:', selectedRows.value.map(s => ({
    mstId: s.mstId,
    no: s.no,
    name: s.satelliteName
  })))
}

// ✅ 전체 선택 상태 확인 (mstId 기준)
const isAllSelected = computed(() => {
  if (scheduleData.value.length === 0) return false
  if (selectedRows.value.length === 0) return false

  // 겹치지 않고 선택 가능한 스케줄만 카운트 (mstId 기준)
  const selectableSchedules = scheduleData.value.filter(schedule =>
    !isScheduleOverlapping(schedule)
  )

  if (selectableSchedules.length === 0) return false

  // ✅ 선택 가능한 모든 스케줄이 선택되었는지 확인 (mstId와 detailId 조합)
  const allSelected = selectableSchedules.every(schedule =>
    selectedRows.value.some(selected =>
      selected.mstId === schedule.mstId && selected.detailId === schedule.detailId
    )
  )

  console.log('🔍 isAllSelected:', {
    allSelected,
    selectableCount: selectableSchedules.length,
    selectedCount: selectedRows.value.length
  })

  return allSelected
})

// ✅ 일부 선택 상태 확인 (indeterminate) - mstId 기준
const isIndeterminate = computed(() => {
  if (scheduleData.value.length === 0) return false
  if (selectedRows.value.length === 0) return false

  // 겹치지 않고 선택 가능한 스케줄만 카운트 (mstId 기준)
  const selectableSchedules = scheduleData.value.filter(schedule =>
    !isScheduleOverlapping(schedule)
  )

  if (selectableSchedules.length === 0) return false

  // ✅ mstId 기준으로 선택된 개수 확인
  const selectedCount = selectableSchedules.filter(schedule =>
    selectedRows.value.some(selected =>
      selected.mstId === schedule.mstId && selected.detailId === schedule.detailId
    )
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
        console.log('⏭️ 이미 선택됨:', schedule.mstId, schedule.satelliteName)
        return false
      }

      // 겹치는 스케줄은 제외 (mstId 기준)
      if (isScheduleOverlapping(schedule)) {
        console.log('⚠️ 겹침으로 제외:', schedule.mstId, schedule.satelliteName)
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
        console.log('➕ 추가:', schedule.mstId, schedule.satelliteName)
      } else {
        console.log('⚠️ 시간 겹침으로 제외:', schedule.mstId, schedule.satelliteName)
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


// ✅ 겹침 경고 메시지 표시 함수 - 직접 비교 방식
const showOverlapWarning = (row: ScheduleItem) => {
  // ✅ 직접 비교로 겹치는 스케줄 찾기
  const overlappingSchedules = getOverlappingSchedules(row)

  // ✅ selectedRows에서 겹치는 스케줄 찾기 (직접 비교)
  const selectedInGroup = selectedRows.value.filter(selected =>
    checkTimeOverlap(row, selected)
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
  { name: 'mstId', label: 'MstId', field: 'mstId', align: 'left' as const, sortable: true, style: 'width: 80px' },
  { name: 'detailId', label: 'DetailId', field: 'detailId', align: 'left' as const, sortable: true, style: 'width: 80px' },
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

// ✅ Duration 포맷 함수 추가 (V006 Fix: 숫자/ISO 8601 모두 처리)
const formatDuration = (duration: string | number | null | undefined): string => {
  if (duration === null || duration === undefined) return '0분 0초'

  // ✅ 숫자(초 단위)인 경우 직접 변환
  if (typeof duration === 'number') {
    const totalSeconds = Math.round(duration)
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60
    const parts: string[] = []
    if (hours > 0) parts.push(`${hours}시간`)
    if (minutes > 0) parts.push(`${minutes}분`)
    if (seconds > 0 || parts.length === 0) parts.push(`${seconds}초`)
    return parts.join(' ')
  }

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return String(duration) // 파싱 실패 시 원본 반환

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

  if (isScheduleOverlapping(row)) {
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
      mstId: s.mstId,
      no: s.no,
      satelliteName: s.satelliteName,
      satelliteId: s.satelliteId,
      startTime: s.startTime,
      endTime: s.endTime
    })))

    // ✅ PassSchedule 데이터 구조 리팩토링: mstId 사용
    const schedulesWithMstId = selectedRows.value.map(s => ({
      ...s,
      mstId: s.mstId, // ✅ 전역 고유 ID (필수)
      detailId: s.detailId ?? 0, // ✅ Detail 구분자
    }))

    console.log('🔄 mstId 기준 변환:', schedulesWithMstId.map(s => ({
      mstId: s.mstId,
      no: s.no,
      satelliteName: s.satelliteName
    })))

    // 🔧 기존 목록 초기화 후 새 스케줄 추가
    const success = await passScheduleStore.replaceSelectedSchedules(schedulesWithMstId)

    console.log('🔍 replaceSelectedSchedules 결과:', success)
    console.log('🔍 Store 상태 확인:', {
      selectedScheduleListCount: passScheduleStore.selectedScheduleList.length,
      selectedSchedule: passScheduleStore.selectedSchedule?.satelliteName
    })

    if (success) {
      console.log('✅ 스케줄 목록 교체 완료:', {
        count: selectedRows.value.length,
        schedules: selectedRows.value.map(s => ({
          mstId: s.mstId, // 전역 고유 ID
          no: s.no, // UI 표시용 재순번
          name: s.satelliteName,
          satelliteId: s.satelliteId,
          startTime: s.startTime
        }))
      })

      // ✅ 선택 완료 시 localStorage에 저장 (mstId와 detailId 조합)
      const sortedSelected = [...selectedRows.value].sort((a, b) => {
        const mstIdA = a.mstId || 0
        const mstIdB = b.mstId || 0
        if (mstIdA !== mstIdB) return mstIdA - mstIdB
        // mstId가 같으면 detailId로 정렬
        const detailIdA = a.detailId ?? 0
        const detailIdB = b.detailId ?? 0
        return detailIdA - detailIdB
      })

      // ✅ mstId와 detailId 조합으로 저장
      const selectedMstIds = sortedSelected.map(s => s.mstId)
      const selectedDetailIds = sortedSelected.map(s => s.detailId ?? 0)
      const selectedNos = sortedSelected.map(s => s.no)

      console.log('💾 localStorage 저장:', {
        selectedMstIds,
        selectedDetailIds,
        selectedNos,
        count: selectedMstIds.length
      })

      try {
        const storageKey = 'pass-schedule-selected-nos'
        const dataToSave = {
          selectedMstIds, // ✅ 전역 고유 ID (필수)
          selectedDetailIds, // ✅ Detail 구분자 (필수)
          selectedNos, // ✅ 하위 호환성
          savedAt: Date.now()
        }
        localStorage.setItem(storageKey, JSON.stringify(dataToSave))
        console.log('💾 선택 완료 - 선택 상태 저장 (mstId 기준):', {
          mstIds: selectedMstIds,
          nos: selectedNos,
          count: selectedMstIds.length
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

      // ✅ PassSchedule 데이터 구조 리팩토링: localStorage에서 selectedMstIds와 selectedDetailIds 복원
      const savedMstIds = passScheduleStore.loadSelectedScheduleNosFromLocalStorage()
      const savedDetailIds = passScheduleStore.loadSelectedScheduleDetailIdsFromLocalStorage()

      if (savedMstIds.length > 0 && scheduleData.value.length > 0) {
        console.log('🔄 localStorage에서 선택된 스케줄 복원 시작:', {
          savedMstIdsCount: savedMstIds.length,
          savedDetailIdsCount: savedDetailIds.length,
          scheduleDataCount: scheduleData.value.length,
          savedMstIds: savedMstIds.slice(0, 5),
          savedDetailIds: savedDetailIds.slice(0, 5)
        })

        // ✅ mstId와 detailId 조합으로 복원
        savedMstIds.forEach((mstId, index) => {
          const savedDetailId = savedDetailIds[index] ?? 0
          // ✅ mstId와 detailId 조합으로 매칭
          const matchedSchedule = scheduleData.value.find(s =>
            s.mstId === mstId && (s.detailId ?? 0) === savedDetailId
          )
          if (matchedSchedule) {
            console.log('🔍 복원 시도:', {
              savedMstId: mstId,
              matchedMstId: matchedSchedule.mstId,
              matchedDetailId: matchedSchedule.detailId,
              matchedNo: matchedSchedule.no,
              satelliteName: matchedSchedule.satelliteName,
              canSelect: canSelectSchedule(matchedSchedule)
            })

            if (canSelectSchedule(matchedSchedule)) {
              if (!selectedRows.value.some(s =>
                s.mstId === matchedSchedule.mstId && s.detailId === matchedSchedule.detailId
              )) {
                selectedRows.value.push({ ...matchedSchedule })
                console.log('✅ 스케줄 복원:', matchedSchedule.satelliteName, `mstId=${matchedSchedule.mstId}, detailId=${matchedSchedule.detailId}`)
              } else {
                console.log('⚠️ 이미 복원된 스케줄:', matchedSchedule.satelliteName, `mstId=${matchedSchedule.mstId}, detailId=${matchedSchedule.detailId}`)
              }
            } else {
              console.log('❌ 선택 불가능한 스케줄 (겹침):', matchedSchedule.satelliteName, `mstId=${matchedSchedule.mstId}, detailId=${matchedSchedule.detailId}`)
            }
          } else {
            console.warn('⚠️ mstId 매칭 실패:', {
              savedMstId: mstId,
              availableMstIds: scheduleData.value.slice(0, 5).map(s => ({ mstId: s.mstId, detailId: s.detailId, no: s.no, name: s.satelliteName }))
            })
          }
        })
      }

      // ✅ passScheduleStore.selectedScheduleList를 직접 사용하여 복원 (추가 복원)
      const storeSelectedList = passScheduleStore.selectedScheduleList

      if (storeSelectedList.length > 0 && scheduleData.value.length > 0) {
        console.log('🔄 Store에서 선택된 스케줄 복원 시작:', {
          storeCount: storeSelectedList.length,
          scheduleDataCount: scheduleData.value.length,
          storeNos: storeSelectedList.map(s => ({ no: s.no, mstId: s.mstId, name: s.satelliteName })),
          scheduleDataNos: scheduleData.value.slice(0, 5).map(s => ({ no: s.no, mstId: s.mstId, name: s.satelliteName }))
        })

        // ✅ Store의 selectedScheduleList를 시간 순으로 정렬 (PassSchedulePage와 동일한 순서)
        const sortedStoreList = [...storeSelectedList].sort((a, b) => {
          try {
            return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
          } catch {
            return 0
          }
        })

        // ✅ PassSchedule 데이터 구조 리팩토링: mstId 기준 복원
        const restoredSchedules: ScheduleItem[] = []

        sortedStoreList.forEach((storeSchedule) => {
          // ✅ mstId와 detailId 조합으로 매칭 (전역 고유 ID)
          const savedMstId = storeSchedule.mstId ?? storeSchedule.no
          const savedDetailId = storeSchedule.detailId ?? 0

          // scheduleData에서 같은 mstId와 detailId를 가진 스케줄 찾기
          const matchedSchedule = scheduleData.value.find(s =>
            s.mstId === savedMstId && (s.detailId ?? 0) === savedDetailId
          )

          if (matchedSchedule) {
            console.log('✅ mstId와 detailId 조합 복원 매칭:', {
              savedMstId: savedMstId,
              savedDetailId: savedDetailId,
              scheduleMstId: matchedSchedule.mstId,
              scheduleDetailId: matchedSchedule.detailId,
              scheduleNo: matchedSchedule.no,
              storeNo: storeSchedule.no,
              scheduleName: matchedSchedule.satelliteName
            })
            restoredSchedules.push(matchedSchedule)
          } else {
            console.warn('⚠️ mstId 매칭 실패:', {
              savedMstId: savedMstId,
              storeNo: storeSchedule.no,
              scheduleDataMstIds: scheduleData.value.slice(0, 5).map(s => s.mstId)
            })
          }
        })

        if (restoredSchedules.length > 0) {
          console.log('✅ 복원 가능한 스케줄:', restoredSchedules.length, '개')

          // ✅ 겹침 검증 후 선택 가능한 항목만 복원
          const validSchedules: ScheduleItem[] = []

          restoredSchedules.forEach(schedule => {
            // ✅ 이미 선택되지 않은 경우만 확인 (mstId와 detailId 조합)
            const alreadySelected = selectedRows.value.some(selected =>
              selected.mstId === schedule.mstId && selected.detailId === schedule.detailId
            )

            if (alreadySelected) return

            // ✅ 직접 비교 방식: 이미 선택된 스케줄과 겹치지 않으면 추가
            const wouldOverlap = selectedRows.value.some(selected =>
              checkTimeOverlap(schedule, selected)
            )

            if (!wouldOverlap) {
              validSchedules.push(schedule)
            }
          })

          // ✅ 유효한 스케줄들을 selectedRows에 추가 (객체 복사로 참조 분리)
          selectedRows.value.push(...validSchedules.map(s => ({ ...s })))

          console.log('✅ 이전 선택 상태 복원 완료:', {
            count: validSchedules.length,
            restoredNos: validSchedules.map(s => s.no),
            restoredMstIds: validSchedules.map(s => s.mstId)
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
            scheduleDataNos: scheduleData.value.slice(0, 5).map(s => ({ no: s.no, mstId: s.mstId }))
          })

          let restoredSchedules: ScheduleItem[] = []

          if (useIndex && savedIndexes.length > 0) {
            // ✅ mstId 기준으로 복원 (index는 mstId와 동일)
            savedIndexes.forEach((savedIndex) => {
              // ✅ index는 mstId와 연계되어 있으므로 mstId로 매칭
              const matchedSchedule = scheduleData.value.find(s => s.mstId === savedIndex)
              if (matchedSchedule) {
                console.log('✅ localStorage mstId 기준 복원 매칭:', {
                  savedIndex: savedIndex,
                  scheduleMstId: matchedSchedule.mstId,
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
                  scheduleMstId: schedule.mstId,
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
              // ✅ 이미 선택되지 않은 경우만 확인 (mstId와 detailId 조합)
              const alreadySelected = selectedRows.value.some(selected =>
                selected.mstId === schedule.mstId && selected.detailId === schedule.detailId
              )

              if (alreadySelected) return

              // ✅ 직접 비교 방식: 이미 선택된 스케줄과 겹치지 않으면 추가
              const wouldOverlap = selectedRows.value.some(selected =>
                checkTimeOverlap(schedule, selected)
              )

              if (!wouldOverlap) {
                validSchedules.push(schedule)
              }
            })

            // ✅ 유효한 스케줄들을 selectedRows에 추가 (객체 복사로 참조 분리)
            selectedRows.value.push(...validSchedules.map(s => ({ ...s })))

            console.log('✅ localStorage에서 선택 상태 복원 완료:', {
              count: validSchedules.length,
              restoredNos: validSchedules.map(s => s.no),
              restoredMstIds: validSchedules.map(s => s.mstId)
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
  border-left: 3px solid var(--theme-warning);
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
  background-color: var(--theme-surface-elevated) !important;
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
  border-left: 3px solid var(--theme-button-primary) !important;
}

/* ✅ 겹치는 스케줄 행 스타일 (주황색) */
.schedule-table :deep(.q-table tbody tr.overlapping-row) {
  background-color: rgba(255, 152, 0, 0.15) !important;
  border-left: 3px solid var(--theme-warning) !important;
}

.schedule-table :deep(.q-table tbody tr.overlapping-row:hover) {
  background-color: rgba(255, 152, 0, 0.25) !important;
}

/* ✅ 겹치는 스케줄이 선택된 경우 */
.schedule-table :deep(.q-table tbody tr.overlapping-row.selected) {
  background-color: rgba(255, 152, 0, 0.3) !important;
  border-left: 3px solid var(--theme-warning) !important;
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
  border-left: 3px solid var(--theme-negative) !important;
}

.schedule-table :deep(.q-table tbody tr.keyhole-row:hover) {
  background-color: rgba(244, 67, 54, 0.2) !important;
}

.schedule-table :deep(.q-table tbody tr.keyhole-row.selected) {
  background-color: rgba(244, 67, 54, 0.25) !important;
  border-left: 3px solid var(--theme-negative) !important;
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
  color: var(--theme-button-primary) !important;
}

.schedule-table :deep(.schedule-checkbox .q-checkbox__inner) {
  color: var(--theme-button-primary) !important;
}

/* ✅ 비활성화된 체크박스 스타일 */
.schedule-table :deep(.q-checkbox.disabled) {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
}

.schedule-table :deep(.q-checkbox.disabled .q-checkbox__inner) {
  color: var(--theme-chart-axis) !important;
  cursor: not-allowed !important;
}

/* ✅ 겹치는 스케줄의 체크박스 스타일 */
.schedule-table :deep(.overlapping-row .q-checkbox:not(.disabled)) {
  color: var(--theme-warning) !important;
}

.schedule-table :deep(.overlapping-row .q-checkbox:not(.disabled) .q-checkbox__inner) {
  color: var(--theme-warning) !important;
}

/* ✅ 겹치는 스케줄의 비활성화된 체크박스 */
.schedule-table :deep(.overlapping-row .q-checkbox.disabled) {
  color: var(--theme-warning) !important;
  opacity: 0.3 !important;
}

.schedule-table :deep(.overlapping-row .q-checkbox.disabled .q-checkbox__inner) {
  color: var(--theme-warning) !important;
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
  color: var(--theme-positive);
  font-size: 14px !important;
  font-weight: 700 !important;
}

.end-angle .angle-value {
  color: var(--theme-warning);
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
  color: var(--theme-info) !important;
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
    background-color: var(--theme-dark) !important;
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
    background-color: var(--theme-dark) !important;
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
