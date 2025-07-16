<template>
    <div class="axis-transform-calculator">
        <!-- 헤더 -->
        <div class="header-section">
            <div class="text-h6 text-primary">3축 변환 계산기</div>
            <div class="text-caption text-grey-5">입력 좌표를 3축 변환하여 새로운 좌표를 계산합니다</div>
        </div>

        <!-- 입력 섹션 -->
        <div class="input-section">
            <div class="text-subtitle2 text-weight-bold q-mb-md">입력 좌표</div>

            <div class="input-grid">
                <!-- Azimuth 입력 -->
                <div class="input-item">
                    <label class="input-label">Azimuth (°)</label>
                    <q-input v-model.number="inputData.azimuth" type="number" outlined dense step="0.01" min="0"
                        max="360" :rules="[val => (val >= 0 && val <= 360) || '0-360도 범위여야 합니다']" class="q-mb-sm" />
                </div>

                <!-- Elevation 입력 -->
                <div class="input-item">
                    <label class="input-label">Elevation (°)</label>
                    <q-input v-model.number="inputData.elevation" type="number" outlined dense step="0.01" min="0"
                        max="180" :rules="[val => (val >= 0 && val <= 180) || '0-180도 범위여야 합니다']" class="q-mb-sm" />
                </div>

                <!-- Tilt 입력 -->
                <div class="input-item">
                    <label class="input-label">Tilt (°)</label>
                    <q-input v-model.number="inputData.tilt" type="number" outlined dense step="0.01" min="-90" max="90"
                        :rules="[val => (val >= -90 && val <= 90) || '-90-90도 범위여야 합니다']" class="q-mb-sm" />
                </div>

                <!-- Rotator 입력 -->
                <div class="input-item">
                    <label class="input-label">Rotator (°)</label>
                    <q-input v-model.number="inputData.rotator" type="number" outlined dense step="0.01" min="0"
                        max="360" :rules="[val => (val >= 0 && val <= 360) || '0-360도 범위여야 합니다']" class="q-mb-sm" />
                </div>
            </div>

            <!-- 계산 버튼 -->
            <div class="button-section">
                <q-btn color="primary" label="계산" icon="calculate" @click="calculateTransform" :loading="isCalculating"
                    class="q-mr-sm" />
                <q-btn color="grey-7" label="초기화" icon="refresh" @click="resetInputs" class="q-mr-sm" />
                <q-btn color="negative" label="창 닫기" icon="close" @click="handleCloseWindow" />
            </div>
        </div>

        <!-- 출력 섹션 -->
        <div class="output-section">
            <div class="text-subtitle2 text-weight-bold q-mb-md">변환 결과</div>

            <div class="output-grid">
                <!-- 변환된 Azimuth -->
                <div class="output-item">
                    <label class="output-label">변환된 Azimuth (°)</label>
                    <q-input v-model="outputData.azimuth" outlined dense readonly class="q-mb-sm" />
                </div>

                <!-- 변환된 Elevation -->
                <div class="output-item">
                    <label class="output-label">변환된 Elevation (°)</label>
                    <q-input v-model="outputData.elevation" outlined dense readonly class="q-mb-sm" />
                </div>
            </div>

            <!-- 결과 메시지 -->
            <div v-if="resultMessage" class="result-message q-mt-md">
                <q-banner :class="resultMessage.type === 'success' ? 'bg-positive' : 'bg-negative'" class="text-white">
                    {{ resultMessage.text }}
                </q-banner>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useQuasar } from 'quasar'
import { closeWindow } from '../../utils/windowUtils'
import { ephemerisTrackService } from '../../services/mode/ephemerisTrackService'

const $q = useQuasar()

// 입력 데이터
const inputData = reactive({
    azimuth: 0,
    elevation: 0,
    tilt: 0,
    rotator: 0
})

// 출력 데이터
const outputData = reactive({
    azimuth: '',
    elevation: ''
})

// 상태
const isCalculating = ref(false)
const resultMessage = ref<{ type: 'success' | 'error'; text: string } | null>(null)

// 계산 함수
const calculateTransform = async () => {
    try {
        isCalculating.value = true
        resultMessage.value = null

        // 입력값 검증
        if (inputData.azimuth < 0 || inputData.azimuth > 360) {
            throw new Error('Azimuth는 0-360도 범위여야 합니다')
        }
        if (inputData.elevation < 0 || inputData.elevation > 180) {
            throw new Error('Elevation은 0-180도 범위여야 합니다')
        }
        if (inputData.tilt < -90 || inputData.tilt > 90) {
            throw new Error('Tilt는 -90-90도 범위여야 합니다')
        }
        if (inputData.rotator < 0 || inputData.rotator > 360) {
            throw new Error('Rotator는 0-360도 범위여야 합니다')
        }

        // API 호출
        const response = await ephemerisTrackService.calculateAxisTransform({
            azimuth: inputData.azimuth,
            elevation: inputData.elevation,
            tilt: inputData.tilt,
            rotator: inputData.rotator
        })

        if (response.success) {
            // 결과 출력
            outputData.azimuth = response.output.azimuth.toFixed(6)
            outputData.elevation = response.output.elevation.toFixed(6)

            resultMessage.value = {
                type: 'success',
                text: '3축 변환 계산이 완료되었습니다'
            }
        } else {
            throw new Error(response.message || '계산에 실패했습니다')
        }

    } catch (error) {
        console.error('3축 변환 계산 실패:', error)

        resultMessage.value = {
            type: 'error',
            text: error instanceof Error ? error.message : '계산에 실패했습니다'
        }

        $q.notify({
            type: 'negative',
            message: '3축 변환 계산에 실패했습니다',
            timeout: 3000
        })
    } finally {
        isCalculating.value = false
    }
}

// 입력값 초기화
const resetInputs = () => {
    inputData.azimuth = 0
    inputData.elevation = 0
    inputData.tilt = 0
    inputData.rotator = 0

    outputData.azimuth = ''
    outputData.elevation = ''

    resultMessage.value = null
}

// 창 닫기
const handleCloseWindow = () => {
  closeWindow()
}
</script>

<style scoped>
.axis-transform-calculator {
    padding: 1rem;
    height: 100%;
    display: flex;
    flex-direction: column;
    background: var(--q-page-background);
}

.header-section {
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.input-section {
    margin-bottom: 2rem;
}

.input-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-bottom: 1.5rem;
}

.input-item {
    display: flex;
    flex-direction: column;
}

.input-label {
    font-size: 0.9rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: var(--q-primary);
}

.button-section {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.output-section {
    flex: 1;
}

.output-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-bottom: 1rem;
}

.output-item {
    display: flex;
    flex-direction: column;
}

.output-label {
    font-size: 0.9rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: var(--q-primary);
}

.result-message {
    margin-top: 1rem;
}

/* 다크 모드 지원 */
.body--dark .axis-transform-calculator {
    background: var(--q-dark-page);
}

.body--dark .header-section {
    border-bottom-color: rgba(255, 255, 255, 0.12);
}

/* 반응형 */
@media (max-width: 600px) {

    .input-grid,
    .output-grid {
        grid-template-columns: 1fr;
    }

    .button-section {
        flex-direction: column;
    }
}
</style>
