<template>
  <div class="select-schedule-content">
    <!-- 헤더 -->
    <div class="content-header q-pa-md">
      <div class="text-h6 text-primary">스케줄 선택</div>
      <div class="text-caption text-grey-5">
        총 {{ scheduleData.length }}개의 스케줄이 있습니다
      </div>
    </div>

    <!-- 스케줄 테이블 -->
    <div class="table-container q-pa-md">
      <q-table flat bordered dark :rows="scheduleData" :columns="columns" row-key="no" :pagination="pagination"
        :loading="loading" selection="single" v-model:selected="selected" @row-click="onRowClick" class="schedule-table"
        :no-data-label="'스케줄 데이터가 없습니다'">
        <template v-slot:loading>
          <q-inner-loading showing color="primary">
            <q-spinner size="50px" color="primary" />
          </q-inner-loading>
        </template>

        <!-- 시간 포맷팅 -->
        <template v-slot:body-cell-startTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.row.startTime) }}
          </q-td>
        </template>

        <template v-slot:body-cell-endTime="props">
          <q-td :props="props">
            {{ formatDateTime(props.row.endTime) }}
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- 선택된 스케줄 정보 -->
    <div v-if="selectedSchedule" class="selected-info q-pa-md bg-grey-9">
      <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">
        선택된 스케줄 정보
      </div>

      <div class="row q-col-gutter-md">
        <div class="col-6">
          <div class="info-item">
            <span class="info-label">스케줄 ID:</span>
            <span class="info-value">{{ selectedSchedule.no }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">위성 이름:</span>
            <span class="info-value">{{ selectedSchedule.satelliteName }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">시작 시간:</span>
            <span class="info-value">{{ formatDateTime(selectedSchedule.startTime) }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">종료 시간:</span>
            <span class="info-value">{{ formatDateTime(selectedSchedule.endTime) }}</span>
          </div>
        </div>

        <div class="col-6">
          <div class="info-item">
            <span class="info-label">지속 시간:</span>
            <span class="info-value">{{ selectedSchedule.duration }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">최대 고도각:</span>
            <span class="info-value">{{ selectedSchedule.maxElevation?.toFixed(2) || '0.00' }}°</span>
          </div>

          <div class="info-item">
            <span class="info-label">시작 방위각:</span>
            <span class="info-value">{{ selectedSchedule.startAzimuthAngle?.toFixed(2) || '0.00' }}°</span>
          </div>

          <div class="info-item">
            <span class="info-label">종료 방위각:</span>
            <span class="info-value">{{ selectedSchedule.endAzimuthAngle?.toFixed(2) || '0.00' }}°</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 버튼 영역 -->
    <div class="button-area q-pa-md">
      <div class="row q-gutter-md justify-end">
        <q-btn color="grey-7" label="Close" @click="handleClose" class="q-px-lg" size="md" />
        <q-btn color="primary" label="Select" @click="handleSelect" :disable="!selectedSchedule" class="q-px-lg"
          size="md" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, getCurrentInstance, onUnmounted } from 'vue'
import { usePassScheduleStore, type ScheduleItem } from '../../stores/mode/passScheduleStore'
import { closeWindow } from '../../utils/windowUtils'

// Props
interface Props {
  modalId?: string
  modalTitle?: string
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  close: [selectedData?: ScheduleItem]
  error: [error: Error]
}>()

// Store
const passScheduleStore = usePassScheduleStore()

// 🔧 TLEUploadContent.vue 참고 - 현재 인스턴스 가져오기
const instance = getCurrentInstance()

// 🔧 모드 감지
const isPopupWindow = ref(false)
const isModalMode = ref(false)

// 상태
const loading = ref(false)
const selected = ref<ScheduleItem[]>([])
const selectedSchedule = computed(() => selected.value[0] || null)

// Store의 전체 스케줄 데이터 사용
const scheduleData = computed(() => {
  const data = passScheduleStore.scheduleData
  console.log('🔍 computed scheduleData 호출됨:', data.length, '개')

  if (data.length > 0) {
    console.log('📋 첫 번째 데이터 전체:', data[0])
    //console.log('📋 데이터 필드들:', Object.keys(data[0]))
    console.log('📋 row-key (no):', data[0]?.no)
    console.log('📋 satelliteName:', data[0]?.satelliteName)
  }

  return data
})

// 테이블 설정
const pagination = ref({
  sortBy: 'startTime',
  descending: false,
  page: 1,
  rowsPerPage: 10,
})

const columns = [
  { name: 'no', label: 'No', field: 'no', align: 'left' as const, sortable: true },
  { name: 'satelliteName', label: '위성 이름', field: 'satelliteName', align: 'left' as const, sortable: true },
  {
    name: 'startTime',
    label: '시작 시간',
    field: 'startTime',
    align: 'left' as const,
    sortable: true
  },
  {
    name: 'endTime',
    label: '종료 시간',
    field: 'endTime',
    align: 'left' as const,
    sortable: true
  },
  { name: 'duration', label: '지속 시간', field: 'duration', align: 'left' as const, sortable: true },
  {
    name: 'maxElevation',
    label: '최대 고도각',
    field: 'maxElevation',
    align: 'right' as const,
    sortable: true,
    format: (val: number) => val ? `${val.toFixed(2)}°` : '0.00°'
  },
  {
    name: 'startAzimuthAngle',
    label: '시작 방위각',
    field: 'startAzimuthAngle',
    align: 'right' as const,
    sortable: true,
    format: (val: number) => val ? `${val.toFixed(2)}°` : '0.00°'
  },
]

// 유틸리티 함수
const formatDateTime = (dateString: string): string => {
  if (!dateString) return ''

  try {
    return new Date(dateString).toLocaleString('ko-KR', {
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

// 이벤트 핸들러
const onRowClick = (evt: Event, row: ScheduleItem) => {
  selected.value = [row]
  console.log('✅ 스케줄 선택됨:', row.satelliteName)
}

// 🔧 TLEUploadContent.vue 참고 - Select 버튼 핸들러
const handleSelect = () => {
  if (!selectedSchedule.value) {
    console.warn('⚠️ 선택된 스케줄이 없음')
    return
  }

  console.log('✅ 스케줄 확정 선택:', selectedSchedule.value.satelliteName)

  try {
    // emit으로 선택된 스케줄 데이터 전달
    emit('close', selectedSchedule.value)

    // 창 닫기 처리
    performClose()
  } catch (error) {
    console.error('❌ 스케줄 선택 처리 중 오류:', error)
    emit('error', error instanceof Error ? error : new Error('스케줄 선택 실패'))
  }
}

// 🔧 TLEUploadContent.vue 참고 - Close 버튼 핸들러
const handleClose = () => {
  console.log('🚪 닫기 요청 - Select Schedule')

  try {
    // emit으로 닫기 알림 (선택된 데이터 없이)
    emit('close')

    // 창 닫기 처리
    performClose()
  } catch (error) {
    console.error('❌ 닫기 처리 중 오류:', error)
    performClose() // 에러가 있어도 강제로 닫기
  }
}

// 🔧 TLEUploadContent.vue 참고 - 실제 닫기 수행
const performClose = () => {
  console.log('🚪 실제 닫기 수행 - Select Schedule')

  try {
    if (isPopupWindow.value) {
      // 팝업 창 모드
      console.log('🪟 팝업 창 닫기')
      window.close()
    } else if (isModalMode.value) {
      // 모달 모드 - closeWindow가 ModalManager를 통해 처리
      console.log('📱 모달 닫기 - ID:', props.modalId)

      // 전역 closeModal 함수 사용 (있는 경우)
      const globalProperties = instance?.appContext.config.globalProperties
      if (globalProperties?.$closeModal) {
        console.log('🎯 전역 closeModal 함수 사용')
        globalProperties.$closeModal()
      } else {
        console.log('🎯 closeWindow 함수 사용')
        closeWindow()
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

// 초기화
const init = async () => {
  try {
    loading.value = true
    console.log('🔄 SelectScheduleContent 초기화 시작')

    // Store에 데이터가 없으면 로드
    if (scheduleData.value.length === 0) {
      console.log('📡 스케줄 데이터 로드 시작')
      await passScheduleStore.fetchScheduleDataFromServer()
    }

    console.log('✅ SelectScheduleContent 초기화 완료, 스케줄 수:', scheduleData.value.length)

  } catch (error) {
    console.error('❌ SelectScheduleContent 초기화 실패:', error)
    emit('error', error instanceof Error ? error : new Error('초기화 실패'))
  } finally {
    loading.value = false
  }
}

// 🔧 TLEUploadContent.vue 참고 - 컴포넌트 마운트 시 설정
onMounted(async () => {
  console.log('🔧 Select Schedule 컴포넌트 마운트')
  console.log('🆔 모달 ID:', props.modalId)
  console.log('📋 모달 제목:', props.modalTitle)

  // 모드 감지
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

  try {
    await init()
  } catch (error) {
    console.error('❌ 마운트 중 초기화 실패:', error)
    emit('error', error instanceof Error ? error : new Error('마운트 실패'))
  }
})

// 🔧 TLEUploadContent.vue 참고 - 컴포넌트 언마운트
onUnmounted(() => {
  console.log('🧹 Select Schedule 컴포넌트 언마운트')
  console.log('🆔 정리할 모달 ID:', props.modalId)

  try {
    // 선택된 항목 초기화
    selected.value = []

    // 모달 모드인 경우 추가 정리 작업
    if (isModalMode.value && props.modalId) {
      console.log('🗑️ 모달 정리 작업 수행')

      const globalProperties = instance?.appContext.config.globalProperties
      if (globalProperties?.$modalId === props.modalId) {
        console.log('🧹 전역 모달 ID 정리')
        delete globalProperties.$modalId
      }
    }

    console.log('✅ Select Schedule 정리 완료')

  } catch (error) {
    console.error('❌ 언마운트 정리 중 오류:', error)
  }
})
</script>

<style scoped>
.select-schedule-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.content-header {
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.table-container {
  flex: 1;
  overflow: hidden;
}

.schedule-table {
  height: 100%;
}

.selected-info {
  flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.button-area {
  flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  min-width: 100px;
}

.info-value {
  font-weight: 600;
  color: white;
  text-align: right;
}
</style>
