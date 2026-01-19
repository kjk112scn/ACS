<template>
  <div>
    <h5 class="q-mt-none q-mb-md">{{ T.settings.admin.servoAlarmReset }}</h5>

    <div class="row q-col-gutter-md">
      <!-- Azimuth -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ T.settings.admin.axes.azimuth }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ T.settings.admin.servoAlarmResetDetails.azimuthDesc }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="T.settings.admin.servoAlarmResetDetails.azimuthButton"
                class="full-width" :loading="isLoading.azimuth" @click="handleReset('azimuth')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ T.settings.admin.axes.elevation }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ T.settings.admin.servoAlarmResetDetails.elevationDesc }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="T.settings.admin.servoAlarmResetDetails.elevationButton"
                class="full-width" :loading="isLoading.elevation" @click="handleReset('elevation')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">{{ T.settings.admin.axes.tilt }}</div>
            <div class="text-caption text-grey-6 q-mb-md">{{ T.settings.admin.servoAlarmResetDetails.tiltDesc }}</div>
            <div class="q-mt-md">
              <q-btn color="primary" :label="T.settings.admin.servoAlarmResetDetails.tiltButton" class="full-width"
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
import { T } from '@/texts'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useDialog } from '@/composables/useDialog'
import { useErrorHandler } from '@/composables/useErrorHandler'

type AxisType = 'azimuth' | 'elevation' | 'tilt'

const icdStore = useICDStore()
const { success } = useNotification()
const { confirm } = useDialog()
const { handleApiError } = useErrorHandler()

const isLoading = reactive({ azimuth: false, elevation: false, tilt: false })

const handleReset = async (axis: AxisType) => {
  const axisName = T.value.settings.admin.axes[axis]
  const message = T.value.settings.admin.servoAlarmResetDetails.confirmMessage(axisName)

  const confirmed = await confirm(message, {
    title: T.value.settings.admin.servoAlarmResetDetails.confirmTitle,
    ok: { label: T.value.buttons.yes, color: 'positive' },
    cancel: { label: T.value.buttons.no, color: 'negative' },
  })

  if (!confirmed) return

  isLoading[axis] = true
  try {
    await icdStore.sendServoAlarmResetCommand(
      axis === 'azimuth',
      axis === 'elevation',
      axis === 'tilt'
    )
    success(T.value.settings.admin.success)
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
