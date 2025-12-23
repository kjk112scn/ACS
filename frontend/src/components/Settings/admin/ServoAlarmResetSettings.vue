<template>
  <div>
    <h5 class="q-mt-none q-mb-md">{{ $t('settings.admin.servoAlarmReset') }}</h5>

    <div class="row q-col-gutter-md">
      <!-- Azimuth -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.azimuth') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.azimuthDesc') }}
            </div>

            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.azimuthButton')"
                class="full-width" :loading="isLoading.azimuth" @click="showConfirmation('azimuth')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.elevation') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.elevationDesc')
              }}</div>

            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.elevationButton')"
                class="full-width" :loading="isLoading.elevation" @click="showConfirmation('elevation')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.tilt') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.tiltDesc') }}
            </div>

            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.tiltButton')" class="full-width"
                :loading="isLoading.tilt" @click="showConfirmation('tilt')" />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 확인 모달 -->
    <q-dialog v-model="confirmationDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center">
          <div class="text-h6">{{ $t('settings.admin.servoAlarmResetDetails.confirmTitle') }}</div>
        </q-card-section>

        <q-card-section>
          <p>{{ $t('settings.admin.servoAlarmResetDetails.confirmMessage', {
            axis:
              $t(`settings.admin.axes.${selectedAxis}`) }) }}</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat :label="$t('buttons.no')" color="negative" v-close-popup @click="cancelConfirmation"
            :disable="isLoading[selectedAxis]" />
          <q-btn flat :label="$t('buttons.yes')" color="positive" @click="confirmExecution"
            :loading="isLoading[selectedAxis]" :disable="isLoading[selectedAxis]" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

const { t } = useI18n()
const icdStore = useICDStore()
const { success, error: showError } = useNotification()

// 로딩 상태
const isLoading = ref({
  azimuth: false,
  elevation: false,
  tilt: false
})

// 확인 모달 상태
const confirmationDialog = ref(false)
const selectedAxis = ref('')

// 확인 모달 표시
const showConfirmation = (axis: string) => {
  selectedAxis.value = axis
  confirmationDialog.value = true
}

// 확인 모달 취소
const cancelConfirmation = () => {
  confirmationDialog.value = false
  selectedAxis.value = ''
}

// 실행 확인
const confirmExecution = async () => {
  if (!selectedAxis.value) return

  isLoading.value[selectedAxis.value as keyof typeof isLoading.value] = true

  try {
    // 각 축에 따라 boolean 값 설정
    const azimuth = selectedAxis.value === 'azimuth'
    const elevation = selectedAxis.value === 'elevation'
    const tilt = selectedAxis.value === 'tilt'

    await icdStore.sendServoAlarmResetCommand(azimuth, elevation, tilt)

    success(t('settings.admin.success'))
    confirmationDialog.value = false
  } catch (error) {
    console.error('Servo Alarm Reset 실행 실패:', error)
    showError(t('settings.admin.error'))
  } finally {
    isLoading.value[selectedAxis.value as keyof typeof isLoading.value] = false
    selectedAxis.value = ''
  }
}
</script>

<style scoped>
.command-card {
  transition: all 0.3s ease;
}

.command-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 다크테마에서 호버 효과 개선 */
body.body--dark .command-card:hover {
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
}
</style>
