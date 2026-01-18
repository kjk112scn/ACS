<template>
  <div class="tle-upload-content">
    <!-- ✅ X 아이콘 추가 -->
    <q-btn flat round dense icon="close" color="grey-5" size="sm" @click="handleClose" :disable="isSaving"
      class="close-btn">
      <q-tooltip>닫기</q-tooltip>
    </q-btn>
    <!-- 헤더 -->
    <div class="header-section">
      <div class="text-h6 text-primary">TLE Upload</div>
      <div class="text-caption text-grey-5">위성 궤도 요소(TLE) 데이터를 업로드하고 관리합니다</div>
    </div>

    <!-- 툴바 -->
    <div class="toolbar-section">
      <q-btn icon="upload_file" color="primary" size="md" class="toolbar-btn" @click="handleFileUpload"
        :disable="isSaving" title="파일 업로드" />
      <q-btn icon="download" color="info" size="md" class="toolbar-btn" @click="handleExportTXT"
        :disable="tleData.length === 0 || isSaving" title="TXT로 내보내기" />
      <q-btn icon="delete" color="negative" size="md" class="toolbar-btn" @click="handleDelete"
        :disable="selected.length === 0 || isSaving" title="선택 항목 삭제" />
      <q-btn icon="clear_all" color="warning" size="md" class="toolbar-btn" @click="handleClearAll"
        :disable="tleData.length === 0 || isSaving" title="전체 삭제" />
    </div>

    <!-- 테이블 (스크롤 가능) -->
    <div class="table-section">
      <q-table flat bordered :rows="tleData" :columns="columns" row-key="No" selection="multiple"
        v-model:selected="selected" @row-click="onRowClick" class="tle-table" :pagination="{ rowsPerPage: 0 }"
        hide-pagination table-style="table-layout: fixed; width: 100%;">

        <template v-slot:top>
          <colgroup>
            <col style="width: 50px;">
            <col style="width: 80px;">
            <col style="width: calc(100% - 130px);">
          </colgroup>
        </template>
        <template v-slot:body-cell-TLE="props">
          <q-td :props="props" class="tle-cell">
            <div class="tle-preview">
              <div class="tle-name">{{ getTLEName(props.value) }}</div>
              <div class="tle-lines">{{ getTLELines(props.value) }}</div>
            </div>
          </q-td>
        </template>

        <template v-slot:no-data>
          <div class="full-width row flex-center text-grey-5 q-gutter-sm">
            <q-icon size="2em" name="inbox" />
            <span>업로드된 TLE 데이터가 없습니다</span>
          </div>
        </template>
      </q-table>
    </div>

    <!-- 🆕 진행바 섹션 (업로드 중일 때만 표시) -->
    <div v-if="isSaving" class="progress-section">
      <div class="progress-header">
        <div class="text-subtitle2 text-white">TLE 데이터 처리 중...</div>
        <div class="text-caption text-grey-4">{{ progressLabel }}</div>
      </div>

      <q-linear-progress :value="saveProgress" color="primary" track-color="grey-8" size="12px" rounded
        class="progress-bar" />

      <div class="progress-stats">
        <div class="stat-item">
          <q-icon name="satellite" color="primary" size="16px" />
          <span>{{ completedCount }}/{{ totalCount }}</span>
        </div>
        <div class="stat-item">
          <q-icon name="check_circle" color="positive" size="16px" />
          <span>{{ completedSatellites.length }}개 완료</span>
        </div>
        <div class="stat-item" v-if="failedSatellites.length > 0">
          <q-icon name="error" color="negative" size="16px" />
          <span>{{ failedSatellites.length }}개 실패</span>
        </div>
      </div>

      <!-- 현재 처리 중인 위성 표시 -->
      <div v-if="currentProcessing.show" class="current-processing">
        <q-spinner-dots color="primary" size="20px" />
        <span class="processing-text">{{ currentProcessing.satelliteId }} 처리 중...</span>
      </div>
    </div>

    <!-- 하단 버튼 -->
    <div class="footer-section">
      <div class="footer-info">
        <span class="text-caption text-grey-5">
          총 {{ tleData.length }}개의 TLE 데이터 ({{ selected.length }}개 선택됨)
        </span>
      </div>
      <div class="footer-buttons">
        <q-btn color="positive" label="Save & Close" @click="handleSaveAndClose" size="md" :disable="isSaving"
          :loading="isSaving" />
        <q-btn color="grey-7" label="Close" @click="handleClose" size="md" :disable="isSaving" />
      </div>
    </div>


    <!-- 삭제 확인 다이얼로그 수정 -->
    <q-dialog v-model="confirmDialog" persistent>
      <q-card>
        <q-card-section class="row items-center">
          <q-avatar icon="delete" color="negative" text-color="white" />
          <span class="q-ml-sm">삭제 확인</span>
        </q-card-section>

        <q-card-section class="row items-center">
          <span class="q-ml-sm">선택된 {{ selected.length }}개의 TLE 데이터를 삭제하시겠습니까?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="취소" color="primary" v-close-popup />

          <q-btn flat label="삭제" color="negative" @click="onConfirmDelete" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>


    <!-- 전체 삭제 확인 다이얼로그도 수정 -->
    <q-dialog v-model="confirmAllDialog" persistent>
      <q-card>
        <q-card-section class="row items-center">
          <q-avatar icon="clear_all" color="warning" text-color="white" />
          <span class="q-ml-sm">전체 삭제 확인</span>
        </q-card-section>

        <q-card-section class="row items-center">
          <span class="q-ml-sm">모든 TLE 데이터 {{ tempTleData.length }}개를 삭제하시겠습니까?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="취소" color="primary" v-close-popup />

          <q-btn flat label="전체 삭제" color="warning" @click="onConfirmClearAll" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>


    <!-- 닫기 확인 다이얼로그도 수정 -->
    <q-dialog v-model="confirmCloseDialog" persistent>
      <q-card>
        <q-card-section class="row items-center">
          <q-avatar icon="warning" color="orange" text-color="white" />
          <span class="q-ml-sm">변경사항 확인</span>
        </q-card-section>

        <q-card-section class="row items-center">
          <span class="q-ml-sm">저장하지 않은 변경사항이 있습니다. 정말 닫으시겠습니까?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="취소" color="primary" v-close-popup />

          <q-btn flat label="닫기" color="negative" @click="onConfirmClose" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- 숨겨진 파일 입력 -->
    <input ref="fileInput" type="file" accept=".txt,.tle" @change="onFileSelected" style="display: none" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleModeStore } from '@/stores'
import { closeWindow } from '../../utils/windowUtils'
import type { QTableProps } from 'quasar'


// Props 정의 (모달 ID 관리용)
interface Props {
  modalId?: string
  modalTitle?: string
}
// TLE 업로드 결과 타입 정의 추가
interface TLEUploadResult {
  success: boolean
  successCount: number
  failedCount: number
  totalPasses: number
  processingTime: number
}

const props = defineProps<Props>()

const $q = useQuasar()
const passScheduleStore = usePassScheduleModeStore()

// 현재 인스턴스 가져오기
const instance = getCurrentInstance()

// 모드 감지
const isPopupWindow = ref(false)
const isModalMode = ref(false)

// 로컬 상태
const fileInput = ref<HTMLInputElement | null>(null)
const selected = ref<TLEItem[]>([])
const confirmDialog = ref(false)
const confirmAllDialog = ref(false)
const confirmCloseDialog = ref(false)

// 진행바 상태 (Store에서 가져옴)
const isSaving = computed(() => passScheduleStore.isUploading)
const saveProgress = computed(() => passScheduleStore.uploadProgress)
const progressLabel = computed(() => passScheduleStore.uploadStatus)

// 진행 상태 추적용 로컬 상태
const completedCount = ref(0)
const totalCount = ref(0)
const completedSatellites = ref<string[]>([])
const failedSatellites = ref<string[]>([])
const currentProcessing = ref({
  show: false,
  satelliteId: ''
})

// 임시 TLE 데이터 (저장 전까지 임시로 관리)
const tempTleData = ref<TLEItem[]>([])

// TLE 아이템 타입 정의
interface TLEItem {
  No: number
  TLE: string
}

// 임시 데이터를 사용하도록 변경
const tleData = computed(() => tempTleData.value)

// 테이블 설정
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const columns: QTableColumn[] = [
  {
    name: 'No',
    label: 'No',
    field: 'No',
    align: 'center' as const,
    sortable: true,
    style: 'width: 80px; max-width: 80px; text-align: center; vertical-align: middle;',
    headerStyle: 'width: 80px; max-width: 80px; text-align: center; vertical-align: middle;',
  },
  {
    name: 'TLE',
    label: 'TLE Data',
    field: 'TLE',
    align: 'left' as const,
    sortable: false,
    style: 'width: auto; text-align: left; vertical-align: middle;',
    headerStyle: 'width: auto; text-align: center; vertical-align: middle;',
  },
]

// 컴포넌트 마운트 시 설정
onMounted(async () => {
  console.log('🔧 TLE Upload 컴포넌트 마운트')
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
    // Store에서 서버 데이터 로드
    await passScheduleStore.loadTLEDataFromServer()

    // 기존 저장된 TLE 데이터를 임시 데이터로 복사
    tempTleData.value = [...passScheduleStore.tleData]

    console.log('📦 기존 TLE 데이터 로드:', tempTleData.value.length, '개')
  } catch (error) {
    console.error('❌ 초기 데이터 로드 실패:', error)
    // 로컬 데이터라도 사용
    tempTleData.value = [...passScheduleStore.tleData]
  }
})

// TLE 이름 추출 (첫 번째 줄)
// TLE 이름 추출 (개선된 버전)
const getTLEName = (tleContent: string): string => {
  if (!tleContent) return ''

  const lines = tleContent.split('\n').filter((line) => line.trim())
  if (lines.length === 0) return ''

  // 🔧 3줄 형식인 경우 (위성명 + Line1 + Line2)
  if (lines.length >= 3 &&
    !lines[0]?.startsWith('1 ') &&
    !lines[0]?.startsWith('2 ') &&
    lines[1]?.startsWith('1 ') &&
    lines[2]?.startsWith('2 ')) {

    const satelliteName = lines[0]?.trim() || ''
    console.log(`🔍 위성명 추출 (3줄): "${satelliteName}"`)
    return satelliteName
  }

  // 🔧 2줄 형식인 경우 - TLE Line1에서 위성 ID 추출
  const line1 = lines.find(line => line.startsWith('1 '))
  if (line1) {
    const satelliteId = line1.substring(2, 7).trim()
    console.log(`🔍 위성 ID 추출 (2줄): "${satelliteId}"`)
    return `${satelliteId}`
  }

  return ''
}

// TLE 라인들 추출 (Line1, Line2)
const getTLELines = (tleContent: string): string => {
  if (!tleContent) return ''

  const lines = tleContent.split('\n').filter((line) => line.trim())
  if (lines.length === 0) return ''

  let line1 = ''
  let line2 = ''

  // 3줄 형식인 경우 (위성명 + Line1 + Line2)
  if (lines.length >= 3 && !lines[0]?.startsWith('1 ')) {
    line1 = lines[1]?.trim() || ''
    line2 = lines[2]?.trim() || ''
  }
  // 2줄 형식인 경우 (Line1 + Line2)
  else if (lines.length >= 2) {
    line1 = lines[0]?.trim() || ''
    line2 = lines[1]?.trim() || ''
  }

  return `${line1}\n${line2}`
}

// 파일 내용 읽기
const readFileContent = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (e) => {
      const content = e.target?.result as string
      resolve(content)
    }

    reader.onerror = () => {
      reject(new Error('파일 읽기에 실패했습니다'))
    }

    reader.readAsText(file)
  })
}

// TLE 텍스트 파싱 (개선된 버전 - undefined 체크 추가)
const parseTLEText = (content: string): string[] => {
  if (!content) return []

  console.log('🔍 TLE 파싱 시작')
  console.log('원본 내용:', content)

  // 줄바꿈 정규화
  const normalizedContent = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const allLines = normalizedContent.split('\n')

  console.log('전체 라인 수:', allLines.length)

  // 빈 줄 제거하되 순서 유지
  const lines = allLines.map(line => line.trim()).filter(line => line.length > 0)

  console.log('필터링된 라인들:')
  lines.forEach((line, index) => {
    console.log(`${index}: "${line}"`)
  })

  const tleBlocks: string[] = []
  let i = 0

  while (i < lines.length) {
    const currentLine = lines[i]

    // 🔧 undefined 체크 추가
    if (!currentLine) {
      i++
      continue
    }

    // 🔧 3줄 형식 우선 체크: 위성명 + TLE Line1 + TLE Line2
    if (i + 2 < lines.length) {
      const line1 = lines[i + 1]
      const line2 = lines[i + 2]

      // 🔧 모든 라인이 존재하는지 확인
      if (currentLine && line1 && line2 &&
        !currentLine.startsWith('1 ') &&
        !currentLine.startsWith('2 ') &&
        line1.startsWith('1 ') &&
        line2.startsWith('2 ')) {

        const satelliteName = currentLine
        const tleBlock = `${satelliteName}\n${line1}\n${line2}`
        tleBlocks.push(tleBlock)

        console.log(`✅ 3줄 형식 TLE 발견: "${satelliteName}"`)

        i += 3
        continue
      }
    }

    // 🔧 2줄 형식 체크: TLE Line1 + TLE Line2
    if (i + 1 < lines.length) {
      const line2 = lines[i + 1]

      // 🔧 라인 존재 확인 추가
      if (currentLine && line2 &&
        currentLine.startsWith('1 ') &&
        line2.startsWith('2 ')) {

        const tleBlock = `${currentLine}\n${line2}`
        tleBlocks.push(tleBlock)

        console.log(`✅ 2줄 형식 TLE 발견`)

        i += 2
        continue
      }
    }

    // 처리되지 않은 라인
    console.log(`⚠️ 건너뛴 라인: "${currentLine}"`)
    i++
  }

  console.log(`🎯 파싱 완료: ${tleBlocks.length}개 TLE 블록`)
  tleBlocks.forEach((block, index) => {
    console.log(`\n=== TLE ${index + 1} ===`)
    console.log(block)
  })

  return tleBlocks
}


// 파일 업로드 핸들러
const handleFileUpload = () => {
  fileInput.value?.click()
}

// 파일 업로드 핸들러 수정 - $q 존재 확인 후 사용
const onFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  try {
    console.log('📁 파일 업로드 시작:', file.name)
    console.log('🧹 기존 데이터 초기화 전 - 현재 개수:', tempTleData.value.length)

    // 🔧 기존 데이터 전체 초기화 (명시적으로)
    tempTleData.value.splice(0, tempTleData.value.length) // 배열 완전 초기화
    selected.value.splice(0, selected.value.length) // 선택된 항목도 초기화

    console.log('🧹 기존 데이터 초기화 완료 - 현재 개수:', tempTleData.value.length)

    const content = await readFileContent(file)
    console.log('📄 파일 내용 길이:', content.length)

    const tleBlocks = parseTLEText(content)
    console.log('🔍 파싱된 TLE 블록 수:', tleBlocks.length)

    if (tleBlocks.length === 0) {
      // 🔧 $q 존재 확인 후 알림 처리
      if ($q && $q.notify) {
        $q.notify({
          type: 'warning',
          message: '유효한 TLE 데이터를 찾을 수 없습니다',
        })
      } else {
        console.warn('⚠️ 유효한 TLE 데이터를 찾을 수 없습니다')
      }
      return
    }

    // 🔧 새로운 데이터를 1번부터 순서대로 추가
    tleBlocks.forEach((block, index) => {
      const newNo = index + 1

      tempTleData.value.push({
        No: newNo,
        TLE: block
      })

      console.log(`➕ TLE ${newNo} 추가:`, getTLEName(block))
    })

    console.log('✅ 새로운 TLE 데이터 총', tempTleData.value.length, '개 추가 완료')

    // 🔧 $q 존재 확인 후 알림 처리
    if ($q && $q.notify) {
      $q.notify({
        type: 'positive',
        message: `기존 데이터를 초기화하고 ${tleBlocks.length}개의 새로운 TLE 데이터를 추가했습니다`,
      })
    } else {
      console.log('✅ 기존 데이터를 초기화하고', tleBlocks.length, '개의 새로운 TLE 데이터를 추가했습니다')
    }
  } catch (error) {
    console.error('❌ 파일 처리 오류:', error)

    // 🔧 $q 존재 확인 후 알림 처리
    if ($q && $q.notify) {
      $q.notify({
        type: 'negative',
        message: '파일 처리 중 오류가 발생했습니다',
      })
    } else {
      console.error('❌ 파일 처리 중 오류가 발생했습니다')
    }
  } finally {
    // 파일 입력 초기화
    if (target) {
      target.value = ''
    }
  }
}


// 삭제 핸들러 - 커스텀 다이얼로그 사용
const handleDelete = () => {
  console.log('🗑️ 삭제 버튼 클릭')
  console.log('📋 선택된 항목들:', selected.value)

  if (selected.value.length === 0) {
    console.warn('⚠️ 선택된 항목이 없음')
    $q.notify({
      type: 'warning',
      message: '삭제할 항목을 선택하세요',
    })
    return
  }

  // 커스텀 다이얼로그 표시
  confirmDialog.value = true
}

// 삭제 확인 처리
const onConfirmDelete = () => {
  console.log('✅ 삭제 확인됨')

  const count = selected.value.length
  performDelete(count)
}

// 실제 삭제 수행 함수
const performDelete = (count: number) => {
  console.log('🗑️ 실제 삭제 수행 시작')
  console.log('📊 삭제 전 데이터 개수:', tempTleData.value.length)

  // 선택된 항목들의 No를 수집
  const selectedNos = selected.value.map(item => {
    console.log('🎯 삭제할 항목 No:', item.No)
    return item.No
  })

  console.log('🎯 삭제할 No 목록:', selectedNos)

  // 선택된 항목들을 임시 데이터에서 제거
  const beforeLength = tempTleData.value.length
  tempTleData.value = tempTleData.value.filter(item => {
    const shouldKeep = !selectedNos.includes(item.No)
    if (!shouldKeep) {
      console.log('🗑️ 삭제:', item.No, item.TLE.substring(0, 50) + '...')
    }
    return shouldKeep
  })

  console.log('📊 삭제 후 데이터 개수:', tempTleData.value.length)
  console.log('📊 실제 삭제된 개수:', beforeLength - tempTleData.value.length)

  // No 재정렬
  tempTleData.value.forEach((item, index) => {
    const oldNo = item.No
    item.No = index + 1
    if (oldNo !== item.No) {
      console.log('🔄 No 변경:', oldNo, '->', item.No)
    }
  })

  // 선택 초기화
  selected.value = []
  console.log('🧹 선택 항목 초기화')

  $q.notify({
    type: 'positive',
    message: `${count}개의 TLE 데이터가 삭제되었습니다`,
  })

  console.log('✅ 삭제 완료')
}

// 전체 삭제 핸들러
const handleClearAll = () => {
  console.log('🗑️ 전체 삭제 버튼 클릭')

  if (tempTleData.value.length === 0) {
    $q.notify({
      type: 'warning',
      message: '삭제할 TLE 데이터가 없습니다',
    })
    return
  }

  confirmAllDialog.value = true
}

// 전체 삭제 확인 처리
const onConfirmClearAll = () => {
  console.log('✅ 전체 삭제 확인됨')

  const deletedCount = tempTleData.value.length
  tempTleData.value = []
  selected.value = []

  $q.notify({
    type: 'positive',
    message: `모든 TLE 데이터 ${deletedCount}개가 삭제되었습니다`,
  })

  confirmAllDialog.value = false
}

// TXT 내보내기 핸들러 - 현재 표시된 데이터 기준
const handleExportTXT = () => {
  try {
    if (tempTleData.value.length === 0) {
      $q.notify({
        type: 'warning',
        message: '내보낼 TLE 데이터가 없습니다',
      })
      return
    }

    // 현재 데이터를 TXT 형식으로 변환
    const tleContent = tempTleData.value
      .map(item => item.TLE)
      .join('\n')

    // 파일 다운로드
    const blob = new Blob([tleContent], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `tle_data_${new Date().toISOString().slice(0, 10)}.txt`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    $q.notify({
      type: 'positive',
      message: 'TLE 데이터가 내보내기되었습니다',
    })
  } catch (error) {
    console.error('내보내기 오류:', error)
    $q.notify({
      type: 'negative',
      message: '내보내기 중 오류가 발생했습니다',
    })
  }
}

// 테이블 행 클릭 핸들러
const onRowClick = (evt: Event, row: TLEItem) => {
  // 다중 선택 토글
  const index = selected.value.findIndex((item) => item.No === row.No)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(row)
  }
}
// TLE 업로드 결과 타입 정의 (Store와 동일하게)
interface TLEUploadResult {
  success: boolean
  successCount: number
  failedCount: number
  totalPasses: number
  processingTime: number
}

// TLE 응답 타입 (Store에서 정의된 것과 동일)
interface TleResponse {
  success: boolean
  message: string
  data?: {
    satelliteId: string
    passCount: number
    [key: string]: unknown
  }
}

// Save & Close - Store를 통한 서버 연동
const handleSaveAndClose = async () => {
  console.log('💾 Save & Close 버튼 클릭 - Store 연동')
  console.log('📊 저장할 임시 데이터 개수:', tempTleData.value.length)

  if (tempTleData.value.length === 0) {
    $q.notify({
      type: 'warning',
      message: '저장할 TLE 데이터가 없습니다',
    })
    return
  }

  try {
    // 진행 상태 초기화
    completedCount.value = 0
    totalCount.value = 0
    completedSatellites.value = []
    failedSatellites.value = []
    currentProcessing.value = { show: false, satelliteId: '' }

    console.log('🚀 스케줄 선택 처리 시작')
    console.log('🚀 TLE 업로드 및 추적 데이터 생성 시작')
    console.log('1️⃣ 전체 추적 데이터 삭제 수행')

    // 🆕 1단계: 전체 추적 데이터 삭제
    const deleteSuccess = await passScheduleStore.deleteAllTrackingData()

    if (!deleteSuccess) {
      console.error('❌ 전체 추적 데이터 삭제 실패')
      if ($q && $q.notify) {
        $q.notify({
          type: 'negative',
          message: '기존 추적 데이터 삭제에 실패했습니다',
        })
      }
      return
    }

    // 🔧 Store를 통해 TLE 데이터 업로드 (타입 안전)
    const result = await passScheduleStore.uploadTLEDataToServer(tempTleData.value, {
      onProgress: (completed: number, total: number, currentSatellite: string) => {
        completedCount.value = completed
        totalCount.value = total
        currentProcessing.value = { show: true, satelliteId: currentSatellite }
        console.log(`🔄 진행: ${completed}/${total} - ${currentSatellite}`)
      },

      onSuccess: (satelliteId: string, response: TleResponse) => {
        completedSatellites.value.push(satelliteId)
        console.log(`✅ 성공: ${satelliteId}`, response.data)

        // 🔧 $q 존재 확인 후 알림 처리
        if ($q && $q.notify) {
          $q.notify({
            type: 'positive',
            message: `${satelliteId} 완료 (${response.data?.passCount || 0}개 패스)`,
            timeout: 1500,
            position: 'top-right'
          })
        }
      },

      onError: (satelliteId: string, error: string) => {
        failedSatellites.value.push(satelliteId)
        console.error(`❌ 실패: ${satelliteId} - ${error}`)

        // 🔧 $q 존재 확인 후 알림 처리
        if ($q && $q.notify) {
          $q.notify({
            type: 'negative',
            message: `${satelliteId} 실패: ${error}`,
            timeout: 2000,
            position: 'top-right'
          })
        }
      },

      onComplete: (uploadResult: TLEUploadResult) => {
        currentProcessing.value.show = false
        console.log('🎉 전체 완료:', uploadResult)

        // 🔧 $q 존재 확인 후 최종 알림 처리
        if ($q && $q.notify) {
          if (uploadResult.success) {
            $q.notify({
              type: 'positive',
              message: `🎉 모든 위성 처리 완료!\n${uploadResult.successCount}개 위성, ${uploadResult.totalPasses}개 패스 생성\n소요시간: ${uploadResult.processingTime}초`,
              timeout: 5000,
              multiLine: true,
              actions: [{ label: '확인', color: 'white' }]
            })
          } else {
            $q.notify({
              type: 'warning',
              message: `처리 완료: ${uploadResult.successCount}개 성공, ${uploadResult.failedCount}개 실패\n총 ${uploadResult.totalPasses}개 패스 생성 (${uploadResult.processingTime}초 소요)`,
              timeout: 5000,
              multiLine: true,
              actions: [{ label: '확인', color: 'white' }]
            })
          }
        }

        // 잠시 후 창 닫기
        setTimeout(() => {
          console.log('🚪 저장 완료 후 창 닫기')
          performClose()
        }, 100)
      }
    })

    console.log('✅ Store를 통한 업로드 완료:', result)

  } catch (error) {
    console.error('❌ 업로드 중 오류:', error)

    // 🔧 에러 처리 개선
    if ($q && $q.notify) {
      $q.notify({
        type: 'negative',
        message: `업로드 실패: ${error instanceof Error ? error.message : '알 수 없는 오류'}`,
      })
    }

    // 진행 상태 초기화
    currentProcessing.value.show = false
  }
}


// 닫기 핸들러
const handleClose = () => {
  console.log('🚪 닫기 요청 - TLE Upload')

  const originalData = passScheduleStore.tleData
  const hasChanges = tempTleData.value.length !== originalData.length ||
    !tempTleData.value.every((item, index) =>
      originalData[index]?.TLE === item.TLE)

  if (hasChanges) {
    confirmCloseDialog.value = true
  } else {
    performClose()
  }
}

// 닫기 확인 처리
const onConfirmClose = () => {
  console.log('✅ 닫기 확인됨')
  performClose()
  confirmCloseDialog.value = false
}

// 실제 닫기 수행
const performClose = () => {
  console.log('🚪 실제 닫기 수행')

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

// 컴포넌트 언마운트
onUnmounted(() => {
  console.log('🧹 TLE Upload 컴포넌트 언마운트')
  console.log('🆔 정리할 모달 ID:', props.modalId)

  try {
    // 선택된 항목 초기화
    selected.value = []

    // 임시 데이터 초기화 (메모리 정리)
    tempTleData.value = []

    // 진행 상태 초기화
    completedCount.value = 0
    totalCount.value = 0
    completedSatellites.value = []
    failedSatellites.value = []
    currentProcessing.value = { show: false, satelliteId: '' }

    // 모달 모드인 경우 추가 정리 작업
    if (isModalMode.value && props.modalId) {
      console.log('🗑️ 모달 정리 작업 수행')

      const globalProperties = instance?.appContext.config.globalProperties
      if (globalProperties?.$modalId === props.modalId) {
        console.log('🧹 전역 모달 ID 정리')
        delete globalProperties.$modalId
      }
    }

    // 파일 입력 정리
    if (fileInput.value) {
      fileInput.value.value = ''
    }

    console.log('✅ TLE Upload 정리 완료')

  } catch (error) {
    console.error('❌ 언마운트 정리 중 오류:', error)
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

.close-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tle-upload-content {
  position: relative;
  /* X 아이콘 절대 위치를 위해 추가 */
  /* 기존 스타일 유지 */
}

.tle-upload-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--theme-card-background);
  color: white;
  padding: 1rem;
  overflow: hidden;
  box-sizing: border-box;
}

.header-section {
  flex-shrink: 0;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  margin-bottom: 1rem;
}

.toolbar-section {
  flex-shrink: 0;
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.toolbar-btn {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.table-section {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
}

.tle-table {
  flex: 1;
  background-color: var(--theme-card-background);
  color: white;
  height: 100%;
  overflow: hidden;
  width: 100%;
}

.footer-section {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  margin-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.footer-info {
  display: flex;
  align-items: center;
}

.footer-buttons {
  display: flex;
  gap: 0.5rem;
}

/* 테이블 기본 구조 강화 */
.tle-table :deep(.q-table__container) {
  flex: 1;
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  width: 100% !important;
}

.tle-table :deep(.q-table__middle) {
  flex: 1;
  overflow: auto;
  min-height: 0;
  width: 100% !important;
}

.tle-table :deep(.q-table table) {
  width: 100% !important;
  table-layout: fixed !important;
  border-collapse: separate;
  border-spacing: 0;
}

/* 강제 컬럼 너비 설정 - 더 구체적인 선택자 사용 */
.tle-table :deep(.q-table thead tr th:first-child) {
  width: 50px !important;
  min-width: 50px !important;
  max-width: 50px !important;
}

.tle-table :deep(.q-table thead tr th:nth-child(2)) {
  width: 80px !important;
  min-width: 80px !important;
  max-width: 80px !important;
}

.tle-table :deep(.q-table thead tr th:nth-child(3)) {
  width: calc(100% - 130px) !important;
  min-width: 0 !important;
}

.tle-table :deep(.q-table tbody tr td:first-child) {
  width: 50px !important;
  min-width: 50px !important;
  max-width: 50px !important;
}

.tle-table :deep(.q-table tbody tr td:nth-child(2)) {
  width: 80px !important;
  min-width: 80px !important;
  max-width: 80px !important;
}

.tle-table :deep(.q-table tbody tr td:nth-child(3)) {
  width: calc(100% - 130px) !important;
  min-width: 0 !important;
}

/* TLE 셀 내용이 전체 너비 사용하도록 */
.tle-cell {
  width: 100% !important;
  height: 100% !important;


  min-height: 120px !important;
  padding: 16px !important;
  box-sizing: border-box !important;
}

.tle-preview {
  display: block !important;
  width: 100% !important;
  height: 100% !important;
  padding-top: 12px;
}

.tle-name {
  font-weight: bold;
  color: var(--theme-info);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: center;


  margin-bottom: 12px;
}

.tle-lines {
  font-family: 'Courier New', monospace;


  font-size: 12px;
  color: var(--theme-text);


  white-space: pre-line;
  word-break: keep-all;




  line-height: 1.4;
  max-height: 100px;
  overflow: hidden;
  background-color: rgba(255, 255, 255, 0.05);



  padding: 10px 12px;
  border-radius: 4px;
  border-left: 3px solid var(--theme-info);
  width: 100%;


  text-align: center;

  box-sizing: border-box;
  margin-top: 4px;
}

/* 테이블 헤더 - 3개 컬럼이 전체 너비를 꽉 채우도록 */
.tle-table :deep(.q-table thead) {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: rgba(255, 255, 255, 0.1);
  width: 100% !important;
}

.tle-table :deep(.q-table thead tr) {
  background-color: rgba(255, 255, 255, 0.1);
  width: 100% !important;

  display: table-row !important;
}

.tle-table :deep(.q-table thead th) {
  color: white !important;
  font-weight: bold !important;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2) !important;
  background-color: rgba(255, 255, 255, 0.1) !important;
  position: sticky;
  top: 0;
  z-index: 11;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);



  padding: 6px 4px !important;
  height: 28px !important;
  min-height: 28px !important;
  box-sizing: border-box !important;


  display: table-cell !important;
  text-align: center !important;
  vertical-align: middle !important;


  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  font-size: 13px !important;
}

/* 체크박스 헤더 컬럼 - 고정 너비 */
.tle-table :deep(.q-table thead th.q-table--col-auto-width) {
  width: 50px !important;
  min-width: 50px !important;
  max-width: 50px !important;
}

/* No 헤더 컬럼 - 고정 너비 */
.tle-table :deep(.q-table thead th:nth-child(2)) {
  width: 80px !important;
  min-width: 80px !important;
  max-width: 80px !important;
}

/* TLE Data 헤더 컬럼 - 나머지 전체 공간 사용 */
.tle-table :deep(.q-table thead th:nth-child(3)) {
  width: calc(100% - 130px) !important;
  min-width: 200px !important;
}

/* 테이블 바디 */
.tle-table :deep(.q-table tbody) {
  background-color: var(--theme-card-background);
  width: 100% !important;
}

.tle-table :deep(.q-table tbody tr) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  width: 100% !important;
  display: table-row;
  height: auto !important;
  min-height: 80px !important;
}

.tle-table :deep(.q-table tbody tr:hover) {
  background-color: rgba(255, 255, 255, 0.05);
}

.tle-table :deep(.q-table tbody tr.selected) {
  background-color: rgba(var(--theme-info-rgb), 0.2);
}

.tle-table :deep(.q-table tbody td) {
  color: white !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
  height: 100% !important;
  min-height: 80px !important;
  box-sizing: border-box !important;

  /* 테이블 셀 기본 속성 유지하면서 가운데 정렬 */
  display: table-cell !important;
  text-align: center !important;
  vertical-align: middle !important;
}

/* 체크박스 바디 컬럼 */
.tle-table :deep(.q-table tbody td.q-table--col-auto-width) {
  width: 50px !important;
  min-width: 50px !important;
  max-width: 50px !important;
  padding: 0 !important;
}

/* No 바디 컬럼 */
.tle-table :deep(.q-table tbody td:nth-child(2)) {
  width: 80px !important;
  min-width: 80px !important;
  max-width: 80px !important;
  font-weight: bold !important;
  font-size: 14px !important;
  padding: 0 !important;
}

/* TLE Data 바디 컬럼 */
.tle-table :deep(.q-table tbody td:nth-child(3)) {
  width: calc(100% - 110px) !important;
  min-width: 300px !important;
  padding: 0 !important;
}

/* 체크박스 스타일 */
.tle-table :deep(.q-checkbox) {
  color: var(--theme-info) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

.tle-table :deep(.q-checkbox__inner) {
  color: var(--theme-info);
}

.tle-table :deep(.q-checkbox__bg) {
  border-color: var(--theme-info);
}

.tle-table :deep(.q-checkbox__bg--active) {
  background-color: var(--theme-info);
  border-color: var(--theme-info);
}

/* 선택 상태 강조 */
.tle-table :deep(.q-table tbody tr.selected .tle-name) {
  color: var(--theme-info-light);
}

.tle-table :deep(.q-table tbody tr.selected .tle-lines) {
  border-left-color: var(--theme-info-light);
  background-color: rgba(var(--theme-info-rgb), 0.1);
}

.tle-table :deep(.q-table tbody tr.selected .q-checkbox) {
  color: var(--theme-info-light);
}

.tle-table :deep(.q-table tbody tr.selected .q-checkbox__bg--active) {
  background-color: var(--theme-info-light);
  border-color: var(--theme-info-light);
}

/* 컬럼 너비 강제 설정 */
.tle-table :deep(.q-table colgroup) {
  display: table-column-group;
  width: 100%;
}

.tle-table :deep(.q-table colgroup col:first-child) {

  width: 50px;
  /* 체크박스 컬럼 */
}

.tle-table :deep(.q-table colgroup col:nth-child(2)) {

  width: 80px;
  /* No 컬럼 */
}

.tle-table :deep(.q-table colgroup col:nth-child(3)) {

  width: calc(100% - 130px);
  /* TLE Data 컬럼 - 나머지 전체 */
}

/* 스크롤바 스타일 */
.tle-table :deep(.q-table__middle)::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.tle-table :deep(.q-table__middle)::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.tle-table :deep(.q-table__middle)::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.tle-table :deep(.q-table__middle)::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.tle-table :deep(.q-table__middle) {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) rgba(255, 255, 255, 0.1);
}

/* 빈 데이터 상태 */
.tle-table :deep(.q-table__bottom--nodata) {
  padding: 2rem;
  background-color: var(--theme-card-background);
}

.tle-table :deep(.q-inner-loading) {
  background-color: rgba(0, 0, 0, 0.7);
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .tle-upload-content {
    padding: 0.5rem;
  }

  .toolbar-section {
    gap: 0.25rem;
  }

  .toolbar-btn {
    width: 36px;
    height: 36px;
  }

  .tle-name {
    font-size: 12px;
  }

  .tle-lines {
    font-size: 10px;
    max-height: 60px;
    padding: 6px 4px;
    line-height: 1.3;
  }

  .footer-section {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .footer-buttons {
    justify-content: center;
  }

  /* 모바일에서 컬럼 너비 조정 */
  .tle-table :deep(.q-table thead th.q-table--col-auto-width),
  .tle-table :deep(.q-table tbody td.q-table--col-auto-width) {



    width: 45px !important;
    min-width: 45px !important;
    max-width: 45px !important;
  }

  .tle-table :deep(.q-table thead th:nth-child(2)),
  .tle-table :deep(.q-table tbody td:nth-child(2)) {



    width: 50px !important;
    min-width: 50px !important;
    max-width: 50px !important;
    font-size: 12px !important;
  }

  .tle-table :deep(.q-table thead th:nth-child(3)),
  .tle-table :deep(.q-table tbody td:nth-child(3)) {


    width: calc(100% - 95px) !important;
    min-width: 200px !important;
  }

  .tle-table :deep(.q-table colgroup col:first-child) {

    width: 45px;
  }

  .tle-table :deep(.q-table colgroup col:nth-child(2)) {

    width: 50px;
  }

  .tle-table :deep(.q-table colgroup col:nth-child(3)) {

    width: calc(100% - 95px);
  }

  .tle-cell {
    min-height: 70px !important;
    padding: 10px !important;
  }

  .tle-table :deep(.q-table tbody tr) {
    min-height: 70px !important;
  }

  .tle-table :deep(.q-table tbody td) {
    min-height: 70px !important;
  }
}

@media (max-width: 480px) {
  .toolbar-section {
    justify-content: center;
  }

  .tle-name {
    font-size: 11px;
  }

  .tle-lines {
    font-size: 9px;
    max-height: 50px;
    padding: 4px;
    line-height: 1.2;
  }

  /* 작은 화면에서 더 작은 컬럼 너비 */
  .tle-table :deep(.q-table thead th.q-table--col-auto-width),
  .tle-table :deep(.q-table tbody td.q-table--col-auto-width) {
    width: 45px !important;
    min-width: 45px !important;
    max-width: 45px !important;
  }

  .tle-table :deep(.q-table thead th:nth-child(2)),
  .tle-table :deep(.q-table tbody td:nth-child(2)) {
    width: 70px !important;
    min-width: 70px !important;
    max-width: 70px !important;
    font-size: 11px !important;
  }

  .tle-table :deep(.q-table thead th:nth-child(3)),
  .tle-table :deep(.q-table tbody td:nth-child(3)) {
    width: calc(100% - 115px) !important;
    min-width: 120px !important;
  }

  .tle-table :deep(.q-table colgroup col:first-child) {
    width: 45px;
  }

  .tle-table :deep(.q-table colgroup col:nth-child(2)) {
    width: 70px;
  }

  .tle-table :deep(.q-table colgroup col:nth-child(3)) {
    width: calc(100% - 115px);
  }

  .tle-cell {
    min-height: 60px !important;
    padding: 8px !important;
  }

  .tle-table :deep(.q-table tbody tr) {
    min-height: 60px !important;
  }

  .tle-table :deep(.q-table tbody td) {
    min-height: 60px !important;
  }

  .tle-table :deep(.q-table thead th) {
    padding: 12px 4px !important;
    min-height: 50px !important;
  }
}

/* 다이얼로그 중앙 정렬 */
:deep(.dialog-center .q-dialog__inner) {
  justify-content: center !important;
  align-items: center !important;
  padding: 0 !important;
}

:deep(.dialog-center .q-card) {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
}

/* 다이얼로그 배경 오버레이 */
:deep(.dialog-center .q-dialog__backdrop) {
  background: rgba(0, 0, 0, 0.6) !important;
}

/* 진행바 섹션 스타일 */
.progress-section {
  flex-shrink: 0;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.progress-header {
  margin-bottom: 0.75rem;
}

.progress-bar {
  margin-bottom: 0.75rem;
}

.progress-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: white;
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.1);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.current-processing {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background-color: rgba(var(--theme-info-rgb), 0.1);
  border-left: 3px solid var(--theme-info);
  border-radius: 4px;
}

.processing-text {
  color: var(--theme-info);
  font-size: 13px;
  font-weight: 500;
}

/* 반응형 진행바 */
@media (max-width: 768px) {
  .progress-section {
    padding: 0.75rem;
  }

  .progress-stats {
    gap: 0.5rem;
  }

  .stat-item {
    font-size: 11px;
    padding: 0.2rem 0.4rem;
  }

  .processing-text {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .progress-section {
    padding: 0.5rem;
  }

  .progress-stats {
    flex-direction: column;
    gap: 0.25rem;
  }

  .current-processing {
    padding: 0.4rem;
  }
}
</style>
