<template>
  <div>
    <h5 class="q-mt-none q-mb-md">{{ $t('settings.admin.servoAlarmReset') }}</h5>

    <div class="row q-col-gutter-md">
      <!-- Azimuth -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.azimuth') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.azimuthDesc') }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.azimuthButton')"
                class="full-width" :loading="isLoading.azimuth" @click="handleReset('azimuth')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.elevation') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.elevationDesc') }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.elevationButton')"
                class="full-width" :loading="isLoading.elevation" @click="handleReset('elevation')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ $t('settings.admin.axes.tilt') }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.servoAlarmResetDetails.tiltDesc') }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="$t('settings.admin.servoAlarmResetDetails.tiltButton')" class="full-width"
                :loading="isLoading.tilt" @click="handleReset('tilt')" />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useDialog } from '@/composables/useDialog'
import { useErrorHandler } from '@/composables/useErrorHandler'

type AxisType = 'azimuth' | 'elevation' | 'tilt'

const { t } = useI18n()
const icdStore = useICDStore()
const { success } = useNotification()
const { confirm } = useDialog()
const { handleApiError } = useErrorHandler()

const isLoading = reactive({ azimuth: false, elevation: false, tilt: false })

const handleReset = async (axis: AxisType) => {
  const axisName = t(`settings.admin.axes.${axis}`)
  const message = t('settings.admin.servoAlarmResetDetails.confirmMessage', { axis: axisName })

  const confirmed = await confirm(message, {
    title: t('settings.admin.servoAlarmResetDetails.confirmTitle'),
    ok: { label: t('buttons.yes'), color: 'positive' },
    cancel: { label: t('buttons.no'), color: 'negative' },
  })

  if (!confirmed) return

  isLoading[axis] = true
  try {
    await icdStore.sendServoAlarmResetCommand(
      axis === 'azimuth',
      axis === 'elevation',
      axis === 'tilt'
    )
    success(t('settings.admin.success'))
  } catch (error) {
    handleApiError(error, `Servo Alarm Reset (${axis})`)
  } finally {
    isLoading[axis] = false
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
