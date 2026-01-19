<template>
  <div class="version-info-settings">
    <h5 class="q-mt-none q-mb-sm">{{ T.settings.version.title }}</h5>

    <!-- 새로고침 버튼 -->
    <div class="q-mb-sm">
      <q-btn color="primary" icon="refresh" :label="T.settings.version.refresh" :loading="loading"
        @click="loadVersionInfo" size="sm" />
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
              <div class="text-weight-bold text-grey-8 q-mb-xs">{{ T.settings.version.firmware }}</div>
              <q-chip :color="board.firmwareVersion && board.firmwareVersion !== 'N/A' ? 'primary' : 'grey-5'"
                text-color="white" size="md">
                {{ board.firmwareVersion || 'N/A' }}
              </q-chip>
            </div>

            <!-- 제품번호 -->
            <div class="col-12 col-md-6">
              <div class="text-weight-bold text-grey-8 q-mb-xs">{{ T.settings.version.serial }}</div>
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
      <div class="q-mt-sm">{{ T.settings.version.loading }}</div>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="text-center q-pa-md">
      <q-icon name="error" size="32px" color="negative" />
      <div class="q-mt-sm text-negative">{{ error }}</div>
      <q-btn color="primary" :label="T.settings.version.retry" class="q-mt-sm" size="sm" @click="loadVersionInfo" />
    </div>

    <!-- 데이터 없음 -->
    <div v-else class="text-center q-pa-md">
      <q-icon name="info" size="32px" color="grey-5" />
      <div class="q-mt-sm text-grey-6">{{ T.settings.version.noData }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { T } from '@/texts'
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

// 펌웨어 버전 포맷팅 함수 (one.two.three 형식)
const formatVersion = (one: number, two: number, three: number): string => {
  if (one === undefined || two === undefined || three === undefined) {
    return 'N/A'
  }
  return `${one}.${two}.${three}`
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
      console.log('📡 펌웨어 데이터:', firmwareData)

      // 펌웨어 버전 및 제품번호 데이터 파싱
      versionData.value = {
        mainboard: {
          name: T.value.settings.version.boards.mainboard,
          description: T.value.settings.version.boards.mainboardDesc,
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
          name: T.value.settings.version.boards.azimuth,
          description: T.value.settings.version.boards.azimuthDesc,
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
          name: T.value.settings.version.boards.elevation,
          description: T.value.settings.version.boards.elevationDesc,
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
          name: T.value.settings.version.boards.tilt,
          description: T.value.settings.version.boards.tiltDesc,
          firmwareVersion: formatVersion(
            firmwareData.trainFwVerOne,
            firmwareData.trainFwVerTwo,
            firmwareData.trainFwVerThree
          ),
          serialNumber: formatSerialNumber(
            firmwareData.tiltSerialYear,
            firmwareData.tiltSerialMonth,
            firmwareData.tiltSerialNumber
          )
        },
        feed: {
          name: T.value.settings.version.boards.feed,
          description: T.value.settings.version.boards.feedDesc,
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

      console.log('📡 파싱된 버전 데이터:', versionData.value)
      success(T.value.settings.version.success)
    } else {
      throw new Error('Invalid response format')
    }
  } catch (err: unknown) {
    console.error('❌ 버전 정보 로드 실패:', err)
    const errorMessage = err instanceof Error ? err.message : 'Unknown error'
    error.value = errorMessage
    showError(T.value.settings.version.error)
  } finally {
    loading.value = false
  }
}

// 컴포넌트 마운트 시 자동 로드하지 않음 (사용자가 버튼을 클릭해야 로드)
onMounted(() => {
  // 자동 로드 비활성화
})
</script>

<style scoped>
.version-info-settings {
  max-width: 800px;
}

.version-cards {
  display: grid;
  gap: 8px;
}

.version-card {
  transition: all 0.3s ease;
}

.version-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 다크테마에서 호버 효과 개선 */
body.body--dark .version-card:hover {
  box-shadow: 0 2px 8px rgba(255, 255, 255, 0.1);
}

/* 컴팩트한 레이아웃을 위한 스타일 조정 */
.version-card .q-card__section {
  padding: 8px 12px;
}

.version-card .text-subtitle1 {
  font-size: 0.9rem;
  font-weight: 600;
}

.version-card .text-caption {
  font-size: 0.75rem;
}

.version-card .q-chip {
  font-size: 0.8rem;
  height: 24px;
}

/* 반응형 레이아웃 */
@media (max-width: 768px) {
  .version-cards {
    gap: 6px;
  }

  .version-card .q-card__section {
    padding: 6px 8px;
  }
}
</style>