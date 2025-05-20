<template>
  <div>
    <h5 class="q-mt-none q-mb-md">Servo Encoder Preset</h5>

    <div class="row q-col-gutter-md">
      <div class="col-12 col-md-4">
        <q-card class="preset-card">
          <q-card-section>
            <div class="text-h6">Azimuth</div>
            <div class="q-mt-md">
              <q-btn
                color="primary"
                label="Preset"
                class="full-width"
                @click="showPresetConfirmation('azimuth')"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-md-4">
        <q-card class="preset-card">
          <q-card-section>
            <div class="text-h6">Elevation</div>
            <div class="q-mt-md">
              <q-btn
                color="primary"
                label="Preset"
                class="full-width"
                @click="showPresetConfirmation('elevation')"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <div class="col-12 col-md-4">
        <q-card class="preset-card">
          <q-card-section>
            <div class="text-h6">Tilt</div>
            <div class="q-mt-md">
              <q-btn
                color="primary"
                label="Preset"
                class="full-width"
                @click="showPresetConfirmation('tilt')"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 프리셋 확인 모달 -->
    <q-dialog v-model="confirmationDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center">
          <div class="text-h6">Encoder Preset 확인</div>
        </q-card-section>

        <q-card-section>
          <p>{{ getPresetTitle(presetToConfirm) }}를 Encoder Preset 하시겠습니까?</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="아니오" color="negative" v-close-popup @click="cancelPresetConfirmation" :disable="isLoading" />
          <q-btn flat label="예" color="positive" @click="confirmPreset" :loading="isLoading" :disable="isLoading" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { useICDStore } from '../../../stores/ICD'

const $q = useQuasar()
const icdStore = useICDStore() // ICD 스토어 사용

// 로딩 상태 추가
const isLoading = ref(false)

// 확인 모달 관련 상태
const confirmationDialog = ref(false)
const presetToConfirm = ref<'azimuth' | 'elevation' | 'tilt' | null>(null)

// 프리셋 확인 모달 표시
const showPresetConfirmation = (preset: 'azimuth' | 'elevation' | 'tilt') => {
  presetToConfirm.value = preset
  confirmationDialog.value = true
}

// 프리셋 확인 취소
const cancelPresetConfirmation = () => {
  presetToConfirm.value = null
}

// 프리셋 확인 및 명령 전송
const confirmPreset = async () => {
  if (!presetToConfirm.value) return

  try {
    isLoading.value = true

    // 선택된 축에 따라 파라미터 설정
    const azimuth = presetToConfirm.value === 'azimuth'
    const elevation = presetToConfirm.value === 'elevation'
    const tilt = presetToConfirm.value === 'tilt'

    // 서보 프리셋 명령 전송
    const result = await icdStore.sendServoPresetCommand(azimuth, elevation, tilt)

    if (result.success) {
      // 성공 알림
      $q.notify({
        color: 'positive',
        message: `${getPresetTitle(presetToConfirm.value)} Encoder Preset 명령이 성공적으로 전송되었습니다.`,
        icon: 'check',
        position: 'top',
      })

      // 모달 닫기
      confirmationDialog.value = false
    } else {
      // 오류 알림
      $q.notify({
        color: 'negative',
        message: result.message || '명령 전송 중 오류가 발생했습니다.',
        icon: 'error',
        position: 'top',
      })
    }
  } catch (error) {
    console.error('Preset command error:', error)
    $q.notify({
      color: 'negative',
      message: '명령 처리 중 오류가 발생했습니다.',
      icon: 'error',
      position: 'top',
    })
  } finally {
    isLoading.value = false
    presetToConfirm.value = null
  }
}

// 프리셋 제목 가져오기
const getPresetTitle = (preset: string | null) => {
  if (!preset) return ''

  switch (preset) {
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
</script>

<style scoped>
.preset-card {
  height: 100%;
  transition: all 0.3s;
}

.preset-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.body--dark .preset-card:hover {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}
</style>
