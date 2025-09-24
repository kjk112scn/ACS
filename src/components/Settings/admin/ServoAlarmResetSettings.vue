<template>
  <div>
    <h5 class="q-mt-none q-mb-md">Servo Alarm Reset</h5>

    <div class="row q-col-gutter-md">
      <!-- Azimuth -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Azimuth</div>
            <div class="text-caption text-grey-6 q-mb-md">Azimuth 축 서보 알람 리셋</div>

            <div class="q-mt-md">
              <q-btn color="primary" label="Azimuth Reset" class="full-width" :loading="isLoading.azimuth"
                @click="showConfirmation('azimuth')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Elevation</div>
            <div class="text-caption text-grey-6 q-mb-md">Elevation 축 서보 알람 리셋</div>

            <div class="q-mt-md">
              <q-btn color="primary" label="Elevation Reset" class="full-width" :loading="isLoading.elevation"
                @click="showConfirmation('elevation')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Tilt</div>
            <div class="text-caption text-grey-6 q-mb-md">Tilt 축 서보 알람 리셋</div>

            <div class="q-mt-md">
              <q-btn color="primary" label="Tilt Reset" class="full-width" :loading="isLoading.tilt"
                @click="showConfirmation('tilt')" />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 확인 모달 -->
    <q-dialog v-model="confirmationDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center">
          <div class="text-h6">Servo Alarm Reset 확인</div>
        </q-card-section>

        <q-card-section>
          <p>{{ getAxisTitle(selectedAxis) }} 축의 Servo Alarm Reset 명령을 실행하시겠습니까?</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="아니오" color="negative" v-close-popup @click="cancelConfirmation" :disable="isAnyLoading" />
          <q-btn flat label="예" color="positive" @click="confirmExecution" :loading="isAnyLoading"
            :disable="isAnyLoading" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

const icdStore = useICDStore()
const { success, error: showError } = useNotification()

// 로딩 상태 (각 축별)
const isLoading = ref({
  azimuth: false,
  elevation: false,
  tilt: false
})

// 확인 모달 관련 상태
const confirmationDialog = ref(false)
const selectedAxis = ref<'azimuth' | 'elevation' | 'tilt' | null>(null)

// 전체 로딩 상태 확인
const isAnyLoading = computed(() => {
  return isLoading.value.azimuth || isLoading.value.elevation || isLoading.value.tilt
})

// 축 제목 가져오기
const getAxisTitle = (axis: string | null) => {
  if (!axis) return ''

  switch (axis) {
    case 'azimuth':
      return 'Azimuth'
    case 'elevation':
      return 'Elevation'
    case 'tilt':
      return 'Tilt'
    default:
      return ''
  }
}

// 확인 모달 표시
const showConfirmation = (axis: 'azimuth' | 'elevation' | 'tilt') => {
  selectedAxis.value = axis
  confirmationDialog.value = true
}

// 확인 취소
const cancelConfirmation = () => {
  selectedAxis.value = null
  confirmationDialog.value = false
}

// 확인 및 명령 실행
const confirmExecution = async () => {
  if (!selectedAxis.value) return

  try {
    isLoading.value[selectedAxis.value] = true

    // ✅ 올바른 방식으로 Servo Alarm Reset 명령 실행
    const azimuth = selectedAxis.value === 'azimuth'
    const elevation = selectedAxis.value === 'elevation'
    const tilt = selectedAxis.value === 'tilt'

    const result = await icdStore.sendServoAlarmResetCommand(azimuth, elevation, tilt)

    if (result && result.success) {
      // ✅ useNotification 사용
      success(`${getAxisTitle(selectedAxis.value)} 축 Servo Alarm Reset 명령이 성공적으로 실행되었습니다.`)

      // 모달 닫기
      confirmationDialog.value = false
    } else {
      // ✅ useNotification 사용
      showError(result?.message || '명령 실행 중 오류가 발생했습니다.')
    }
  } catch (error) {
    console.error('Servo Alarm Reset error:', error)
    // ✅ useNotification 사용
    showError('명령 처리 중 오류가 발생했습니다.')
  } finally {
    if (selectedAxis.value) {
      isLoading.value[selectedAxis.value] = false
    }
    selectedAxis.value = null
  }
}
</script>

<style scoped>
.command-card {
  min-height: 200px;
}
</style>
