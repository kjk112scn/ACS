<template>
  <div>
    <h5 class="q-mt-none q-mb-md">Servo Encoder Preset</h5>

    <div class="row q-col-gutter-md">
      <!-- Azimuth -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Azimuth</div>
            <div class="text-caption text-grey-6 q-mb-md">Azimuth 축 서보 인코더 프리셋</div>
            <div class="q-mt-md">
              <q-btn color="primary" label="Azimuth Preset" class="full-width" :loading="isLoading.azimuth"
                @click="handlePreset('azimuth')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Elevation</div>
            <div class="text-caption text-grey-6 q-mb-md">Elevation 축 서보 인코더 프리셋</div>
            <div class="q-mt-md">
              <q-btn color="primary" label="Elevation Preset" class="full-width" :loading="isLoading.elevation"
                @click="handlePreset('elevation')" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt -->
      <div class="col-12 col-md-4">
        <q-card class="command-card">
          <q-card-section>
            <div class="text-h6">Tilt</div>
            <div class="text-caption text-grey-6 q-mb-md">Tilt 축 서보 인코더 프리셋</div>
            <div class="q-mt-md">
              <q-btn color="primary" label="Tilt Preset" class="full-width" :loading="isLoading.tilt"
                @click="handlePreset('tilt')" />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useDialog } from '@/composables/useDialog'
import { useErrorHandler } from '@/composables/useErrorHandler'

type AxisType = 'azimuth' | 'elevation' | 'tilt'

const icdStore = useICDStore()
const { success, error: showError } = useNotification()
const { confirm } = useDialog()
const { handleApiError } = useErrorHandler()

const isLoading = reactive({ azimuth: false, elevation: false, tilt: false })

const handlePreset = async (axis: AxisType) => {
  console.log('🔧 [ServoPreset] 버튼 클릭:', axis)
  const axisTitle = axis.charAt(0).toUpperCase() + axis.slice(1)
  const message = `${axisTitle} 축의 Servo Preset 명령을 실행하시겠습니까?`

  const confirmed = await confirm(message, {
    title: 'Servo Preset 확인',
    ok: { label: '예', color: 'positive' },
    cancel: { label: '아니오', color: 'negative' },
  })

  console.log('🔧 [ServoPreset] 확인 다이얼로그 결과:', confirmed)
  if (!confirmed) return

  isLoading[axis] = true
  try {
    console.log('🔧 [ServoPreset] API 호출 시작:', { azimuth: axis === 'azimuth' ? 1 : 0, elevation: axis === 'elevation' ? 1 : 0, tilt: axis === 'tilt' ? 1 : 0 })
    const result = await icdStore.sendServoPresetCommand(
      axis === 'azimuth' ? 1 : 0,
      axis === 'elevation' ? 1 : 0,
      axis === 'tilt' ? 1 : 0
    )

    console.log('🔧 [ServoPreset] API 호출 결과:', result)
    if (result?.success) {
      success(`${axisTitle} 축 Servo Preset 명령이 성공적으로 실행되었습니다.`)
    } else {
      showError(result?.message || '명령 실행 중 오류가 발생했습니다.')
    }
  } catch (error) {
    console.error('🔧 [ServoPreset] API 호출 실패:', error)
    handleApiError(error, `Servo Preset (${axis})`)
  } finally {
    isLoading[axis] = false
  }
}
</script>

<style scoped>
.command-card {
  min-height: 200px;
}
</style>
