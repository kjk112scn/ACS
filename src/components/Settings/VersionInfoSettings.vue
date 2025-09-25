<template>
  <div class="version-info-settings">
    <h5 class="q-mt-none q-mb-sm">펌웨어 버전/ 제품 번호 정보</h5>

    <!-- 새로고침 버튼 -->
    <div class="q-mb-sm">
      <q-btn color="primary" icon="refresh" label="버전 정보 새로고침" :loading="loading" @click="loadVersionInfo" size="sm" />
    </div>

    <!-- 펌웨어 및 제품번호 정보 표시 -->
    <div v-if="Object.keys(versionData).length > 0" class="version-cards">
      <q-card v-for="(board, key) in versionData" :key="key" class="q-mb-sm version-card" flat bordered>
        <q-card-section class="q-pa-sm">
          <div class="row items-center">
            <div class="col">
              <div class="text-subtitle1">{{ board.name }}</div>
              <div class="text-caption text-grey-6">{{ board.description }}</div>
            </div>
          </div>
        </q-card-section>

        <q-separator />

        <q-card-section class="q-pa-sm">
          <div class="row q-col-gutter-sm">
            <!-- 펌웨어 버전 -->
            <div class="col-12 col-md-6">
              <div class="text-weight-bold text-grey-8 q-mb-xs">펌웨어 버전</div>
              <q-chip :color="board.firmwareVersion && board.firmwareVersion !== 'N/A' ? 'primary' : 'grey-5'"
                text-color="white" size="md">
                {{ board.firmwareVersion || 'N/A' }}
              </q-chip>
            </div>

            <!-- 제품번호 -->
            <div class="col-12 col-md-6">
              <div class="text-weight-bold text-grey-8 q-mb-xs">제품번호</div>
              <q-chip :color="board.serialNumber && board.serialNumber !== 'N/A' ? 'secondary' : 'grey-5'"
                text-color="white" size="md">
                {{ board.serialNumber || 'N/A' }}
              </q-chip>
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>

    <!-- 로딩 상태 -->
    <div v-else-if="loading" class="text-center q-pa-md">
      <q-spinner size="32px" color="primary" />
      <div class="q-mt-sm">버전 정보를 불러오는 중...</div>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="text-center q-pa-md">
      <q-icon name="error" size="32px" color="negative" />
      <div class="q-mt-sm text-negative">{{ error }}</div>
      <q-btn color="primary" label="다시 시도" class="q-mt-sm" size="sm" @click="loadVersionInfo" />
    </div>

    <!-- 데이터 없음 -->
    <div v-else class="text-center q-pa-md">
      <q-icon name="info" size="32px" color="grey-5" />
      <div class="q-mt-sm text-grey-6">버전 정보를 불러오려면 새로고침 버튼을 클릭하세요</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

// 버전 데이터 타입 정의
interface VersionBoard {
  name: string
  description: string
  firmwareVersion: string
  serialNumber: string
}

const icdStore = useICDStore()
const { success, error: showError } = useNotification()

const loading = ref(false)
const error = ref('')
const versionData = ref<Record<string, VersionBoard>>({})

// 제품번호 포맷팅 함수 (YYMMNNNNN 형식)
const formatSerialNumber = (year: number, month: number, number: number): string => {
  if (year === undefined || month === undefined || number === undefined) {
    return 'N/A'
  }

  // Year: 2자리, Month: 2자리, Number: 5자리 (0으로 패딩)
  const yearStr = String(year).padStart(2, '0')
  const monthStr = String(month).padStart(2, '0')
  const numberStr = String(number).padStart(5, '0')

  return `${yearStr}${monthStr}${numberStr}`
}

// 버전 정보 로드
const loadVersionInfo = async () => {
  loading.value = true
  error.value = ''

  try {
    console.log(' 버전 정보 로드 시작...')
    const result = await icdStore.sendReadFwVerSerialNoStatusCommand()
    console.log('📡 API 응답:', result)

    if (result && result.success && result.data?.firmwareData) {
      const firmwareData = result.data.firmwareData
      console.log(' 펌웨어 데이터:', firmwareData)

      // 펌웨어 버전 및 제품번호 데이터 파싱
      versionData.value = {
        mainboard: {
          name: 'Mainboard',
          description: '메인보드 펌웨어 및 제품번호',
          firmwareVersion: formatVersion(
            firmwareData.mainFwVerOne,
            firmwareData.mainFwVerTwo,
            firmwareData.mainFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.mainSerialYear,
            firmwareData.mainSerialMonth,
            firmwareData.mainSerialNumber
          )
        },
        azimuth: {
          name: 'Azimuth',
          description: '방위각 축 펌웨어 및 제품번호',
          firmwareVersion: formatVersion(
            firmwareData.azimuthFwVerOne,
            firmwareData.azimuthFwVerTwo,
            firmwareData.azimuthFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.azimuthSerialYear,
            firmwareData.azimuthSerialMonth,
            firmwareData.azimuthSerialNumber
          )
        },
        elevation: {
          name: 'Elevation',
          description: '고도각 축 펌웨어 및 제품번호',
          firmwareVersion: formatVersion(
            firmwareData.elevationFwVerOne,
            firmwareData.elevationFwVerTwo,
            firmwareData.elevationFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.elevationSerialYear,
            firmwareData.elevationSerialMonth,
            firmwareData.elevationSerialNumber
          )
        },
        tilt: {
          name: 'Tilt',
          description: '기울기 축 펌웨어 및 제품번호',
          firmwareVersion: formatVersion(
            firmwareData.trainFwVerOne,
            firmwareData.trainFwVerTwo,
            firmwareData.trainFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.trainSerialYear,
            firmwareData.trainSerialMonth,
            firmwareData.trainSerialNumber
          )
        },
        feed: {
          name: 'Feed',
          description: '피드 펌웨어 및 제품번호',
          firmwareVersion: formatVersion(
            firmwareData.feedFwVerOne,
            firmwareData.feedFwVerTwo,
            firmwareData.feedFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.feedSerialYear,
            firmwareData.feedSerialMonth,
            firmwareData.feedSerialNumber
          )
        }
      }

      console.log('✅ 버전 데이터 파싱 완료:', versionData.value)
      success('버전 정보를 성공적으로 불러왔습니다')
    } else {
      console.warn('⚠️ 응답 데이터 구조가 예상과 다름:', result)
      throw new Error('버전 정보를 불러올 수 없습니다')
    }
  } catch (err: unknown) {
    const errorMessage = err instanceof Error ? err.message : '버전 정보 로드 중 오류가 발생했습니다'
    console.error('❌ 버전 정보 로드 실패:', err)
    error.value = errorMessage
    showError(errorMessage)
  } finally {
    loading.value = false
  }
}

// 버전 포맷팅 (one.two.three 형태)
const formatVersion = (one: number, two: number, three: number): string => {
  if (one === undefined || two === undefined || three === undefined) {
    return 'N/A'
  }
  return `${one}.${two}.${three}`
}

// 컴포넌트 마운트 시 자동 로드
onMounted(() => {
  void loadVersionInfo()
})
</script>

<style scoped>
.version-info-settings {
  max-width: 800px;
}

.version-cards {
  display: grid;
  gap: 12px;
  /* 간격 줄임 */
}

.version-card {
  transition: all 0.3s ease;
}

.version-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 카드 섹션 패딩 줄임 */
.version-card .q-card-section {
  padding: 12px 16px;
  /* 기존 20px에서 줄임 */
}

/* 제목과 설명 간격 줄임 */
.version-card .text-h6 {
  margin-bottom: 4px;
}

.version-card .text-caption {
  margin-bottom: 8px;
}

/* 구분선과 섹션 간격 줄임 */
.version-card .q-separator {
  margin: 8px 0;
}

/* 칩 크기 줄임 */
.version-card .q-chip {
  font-size: 0.875rem;
  /* 약간 작게 */
}

/* 행 간격 줄임 */
.version-card .row.q-col-gutter-md {
  --q-col-gutter-md: 8px;
  /* 16px에서 8px로 줄임 */
}

/* 라벨과 칩 간격 줄임 */
.version-card .text-weight-bold {
  margin-bottom: 4px;
  /* 8px에서 4px로 줄임 */
}
</style>
