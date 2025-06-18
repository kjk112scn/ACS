<template>
  <div class="tle-upload-content">
    <!-- 헤더 -->
    <div class="header-section">
      <div class="text-h6 text-primary">TLE Upload</div>
      <div class="text-caption text-grey-5">위성 궤도 요소(TLE) 데이터를 업로드하고 관리합니다</div>
    </div>

    <!-- 툴바 -->
    <div class="toolbar-section">
      <q-btn icon="upload_file" color="primary" size="md" class="toolbar-btn" @click="handleFileUpload"
        title="파일 업로드" />
      <q-btn icon="download" color="info" size="md" class="toolbar-btn" @click="handleExportTXT"
        :disable="tleData.length === 0" title="TXT로 내보내기" />
      <q-btn icon="delete" color="negative" size="md" class="toolbar-btn" @click="handleDelete"
        :disable="selected.length === 0" title="선택 항목 삭제" />


      <q-btn icon="clear_all" color="warning" size="md" class="toolbar-btn" @click="handleClearAll"
        :disable="tleData.length === 0" title="전체 삭제" />
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

    <!-- 하단 버튼 -->
    <div class="footer-section">
      <div class="footer-info">
        <span class="text-caption text-grey-5">
          총 {{ tleData.length }}개의 TLE 데이터 ({{ selected.length }}개 선택됨)
        </span>
      </div>
      <div class="footer-buttons">
        <q-btn color="positive" label="Save & Close" @click="handleSaveAndClose" size="md" />
        <q-btn color="grey-7" label="Close" @click="handleClose" size="md" />
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
import { usePassScheduleStore } from '../../stores/mode/passScheduleStore'
import { closeWindow } from '../../utils/windowUtils'
import type { QTableProps } from 'quasar'

// Props 정의 (모달 ID 관리용)
interface Props {
  modalId?: string
  modalTitle?: string
}

const props = defineProps<Props>()

const $q = useQuasar()
const passScheduleStore = usePassScheduleStore()

// 현재 인스턴스 가져오기
const instance = getCurrentInstance()

// 모드 감지
const isPopupWindow = ref(false)
const isModalMode = ref(false)


// 로컬 상태에 confirmDialog 추가
const fileInput = ref<HTMLInputElement | null>(null)
const selected = ref<TLEItem[]>([])
const confirmDialog = ref(false) // 추가
const confirmAllDialog = ref(false) // 전체 삭제용 추가
const confirmCloseDialog = ref(false) // 닫기 확인용 추가


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
onMounted(() => {
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

  // 기존 저장된 TLE 데이터를 임시 데이터로 복사
  tempTleData.value = [...passScheduleStore.tleData]

  console.log('📦 기존 TLE 데이터 로드:', tempTleData.value.length, '개')
})

// TLE 이름 추출 (첫 번째 줄)
const getTLEName = (tleContent: string): string => {
  if (!tleContent) return ''

  const lines = tleContent.split('\n').filter((line) => line.trim())
  if (lines.length === 0) return ''

  const firstLine = lines[0]?.trim() || ''

  // 첫 번째 줄이 TLE Line1이 아닌 경우 (위성명)
  if (!firstLine.startsWith('1 ')) {
    return firstLine
  }

  // TLE Line1에서 위성 ID 추출
  const satelliteId = firstLine.substring(2, 7).trim()
  return `Satellite ${satelliteId}`
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

// TLE 텍스트 파싱 (개선된 버전)
const parseTLEText = (content: string): string[] => {
  if (!content) return []

  // 줄바꿈 정규화
  const normalizedContent = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const lines = normalizedContent.split('\n')

  const tleBlocks: string[] = []
  let i = 0

  while (i < lines.length) {
    const currentLine = lines[i]?.trim()

    if (!currentLine) {
      i++
      continue
    }

    // 3줄 형식 감지: 위성명 + TLE Line1 + TLE Line2
    if (i + 2 < lines.length) {
      const line1 = lines[i + 1]?.trim()
      const line2 = lines[i + 2]?.trim()

      if (line1?.startsWith('1 ') && line2?.startsWith('2 ')) {
        // 위성명이 있는 3줄 형식
        const satelliteName = currentLine
        const tleBlock = `${satelliteName}\n${line1}\n${line2}`
        tleBlocks.push(tleBlock)
        i += 3
        continue
      }
    }

    // 2줄 형식 감지: TLE Line1 + TLE Line2
    if (currentLine.startsWith('1 ') && i + 1 < lines.length) {
      const line2 = lines[i + 1]?.trim()

      if (line2?.startsWith('2 ')) {
        const tleBlock = `${currentLine}\n${line2}`
        tleBlocks.push(tleBlock)
        i += 2
        continue
      }
    }

    i++
  }

  return tleBlocks
}

// 파일 업로드 핸들러
const handleFileUpload = () => {
  fileInput.value?.click()
}

// 파일 업로드 핸들러 - 임시 데이터에 추가
const onFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  try {
    const content = await readFileContent(file)
    const tleBlocks = parseTLEText(content)

    if (tleBlocks.length === 0) {
      $q.notify({
        type: 'warning',
        message: '유효한 TLE 데이터를 찾을 수 없습니다',
      })
      return
    }

    // 임시 데이터에 추가 (기존 데이터에 이어서 추가)
    tleBlocks.forEach((block) => {
      const newNo = tempTleData.value.length > 0
        ? Math.max(...tempTleData.value.map(item => item.No)) + 1
        : 1

      tempTleData.value.push({
        No: newNo,
        TLE: block
      })
    })

    $q.notify({
      type: 'positive',
      message: `${tleBlocks.length}개의 TLE 데이터가 추가되었습니다 (임시)`,
    })
  } catch (error) {
    console.error('파일 처리 오류:', error)
    $q.notify({
      type: 'negative',
      message: '파일 처리 중 오류가 발생했습니다',
    })
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

  // v-close-popup이 자동으로 처리하므로 수동 닫기 제거
  // confirmDialog.value = false // 이 줄 제거
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
      .join('\n\n')

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


// Save & Close - 임시 데이터를 실제 store에 저장하고 닫기
const handleSaveAndClose = () => {
  console.log('💾 Save & Close 버튼 클릭')
  console.log('📊 저장할 임시 데이터 개수:', tempTleData.value.length)
  console.log('📊 현재 store 데이터 개수:', passScheduleStore.tleData.length)

  try {

    // 1. 기존 store 데이터 모두 삭제
    console.log('🗑️ 기존 store 데이터 삭제')
    passScheduleStore.clearTLEData()




    // 2. 임시 데이터를 store에 저장
    console.log('💾 임시 데이터를 store에 저장 시작')
    tempTleData.value.forEach((item, index) => {
      console.log(`💾 저장 중: ${index + 1}/${tempTleData.value.length} - ${item.TLE.substring(0, 30)}...`)
      passScheduleStore.addTLEData(item.TLE)
    })


    console.log('✅ 모든 데이터 저장 완료')
    console.log('📊 저장 후 store 데이터 개수:', passScheduleStore.tleData.length)

    // 3. 성공 알림
    $q.notify({
      type: 'positive',
      message: `${tempTleData.value.length}개의 TLE 데이터가 저장되었습니다`,
      timeout: 2000
    })



    // 4. 잠시 후 창 닫기 (사용자가 알림을 볼 수 있도록)
    setTimeout(() => {
      console.log('🚪 저장 완료 후 창 닫기')
      performClose()
    }, 1000)

  } catch (error) {

    console.error('❌ 저장 중 오류 발생:', error)

    $q.notify({
      type: 'negative',
      message: '저장 중 오류가 발생했습니다',
      timeout: 3000
    })
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

// 컴포넌트 마운트/언마운트
onUnmounted(() => {

  console.log('🧹 TLE Upload 컴포넌트 언마운트')
  console.log('🆔 정리할 모달 ID:', props.modalId)

  try {
    // 선택된 항목 초기화
    selected.value = []

    // 임시 데이터 초기화 (메모리 정리)
    tempTleData.value = []

    // 모달 모드인 경우 추가 정리 작업
    if (isModalMode.value && props.modalId) {
      console.log('🗑️ 모달 정리 작업 수행')

      // ModalManager에서 모달 해제 (이미 닫혔을 수도 있지만 안전하게 정리)
      // closeWindow 함수가 이미 처리했을 수도 있지만, 혹시 모르니 정리
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
.tle-upload-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--q-dark);
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
  background-color: var(--q-dark);
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
  background-color: var(--q-dark);
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
  min-height: 80px !important;





  padding: 8px !important;
  box-sizing: border-box !important;


}

.tle-preview {







  display: block !important;
  width: 100% !important;
  height: 100% !important;
}

.tle-name {
  font-weight: bold;
  color: #64b5f6;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: center;

  margin-bottom: 8px;
}

.tle-lines {
  font-family: 'Courier New', monospace;

  font-size: 10px;
  color: #e0e0e0;
  white-space: pre-wrap;
  word-break: break-all;


  line-height: 1.3;
  max-height: 60px;
  overflow: hidden;
  background-color: rgba(255, 255, 255, 0.05);


  padding: 6px 8px;
  border-radius: 4px;
  border-left: 3px solid #64b5f6;
  width: 100%;

  text-align: left;

  box-sizing: border-box;
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
  padding: 16px 8px !important;
  height: 60px !important;
  min-height: 60px !important;
  box-sizing: border-box !important;





  /* 테이블 셀 기본 속성 유지하면서 가운데 정렬 */
  display: table-cell !important;
  text-align: center !important;
  vertical-align: middle !important;

  /* 텍스트 줄바꿈 방지 */
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
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
  background-color: var(--q-dark);
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
  background-color: rgba(33, 150, 243, 0.2);
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
  color: #64b5f6 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

.tle-table :deep(.q-checkbox__inner) {
  color: #64b5f6;
}

.tle-table :deep(.q-checkbox__bg) {
  border-color: #64b5f6;
}

.tle-table :deep(.q-checkbox__bg--active) {
  background-color: #64b5f6;
  border-color: #64b5f6;
}

/* 선택 상태 강조 */
.tle-table :deep(.q-table tbody tr.selected .tle-name) {
  color: #90caf9;
}

.tle-table :deep(.q-table tbody tr.selected .tle-lines) {
  border-left-color: #90caf9;
  background-color: rgba(33, 150, 243, 0.1);
}

.tle-table :deep(.q-table tbody tr.selected .q-checkbox) {
  color: #90caf9;
}

.tle-table :deep(.q-table tbody tr.selected .q-checkbox__bg--active) {
  background-color: #90caf9;
  border-color: #90caf9;
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
  background-color: var(--q-dark);
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
</style>
