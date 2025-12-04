<template>
    <div class="feed-settings">
        <h5 class="q-mt-none q-mb-md">
            피드 설정
            <q-badge v-if="hasUnsavedChanges" color="orange" class="q-ml-sm">
                변경됨
            </q-badge>
        </h5>

        <div class="text-body2 text-grey-7 q-mb-lg">
            표시할 밴드를 선택하세요. 선택하지 않은 밴드는 Feed 페이지와 All Status에서 숨겨집니다.
        </div>

        <q-list bordered separator>
            <q-item>
                <q-item-section avatar>
                    <q-checkbox v-model="sBandEnabled" />
                </q-item-section>
                <q-item-section>
                    <q-item-label>S-Band</q-item-label>
                    <q-item-label caption>S-Band LNA 및 RF Switch 제어</q-item-label>
                </q-item-section>
            </q-item>

            <q-item>
                <q-item-section avatar>
                    <q-checkbox v-model="xBandEnabled" />
                </q-item-section>
                <q-item-section>
                    <q-item-label>X-Band</q-item-label>
                    <q-item-label caption>X-Band LNA 제어</q-item-label>
                </q-item-section>
            </q-item>

            <q-item>
                <q-item-section avatar>
                    <q-checkbox v-model="kaBandEnabled" />
                </q-item-section>
                <q-item-section>
                    <q-item-label>Ka-Band</q-item-label>
                    <q-item-label caption>Ka-Band LNA 및 Selection 제어</q-item-label>
                </q-item-section>
            </q-item>
        </q-list>

        <q-banner v-if="enabledBandCount === 0" class="bg-warning q-mt-md">
            <template v-slot:avatar>
                <q-icon name="warning" />
            </template>
            최소 하나의 밴드를 선택해야 합니다.
        </q-banner>

        <!-- 저장 버튼 -->
        <div class="row q-gutter-sm q-mt-md">
            <q-btn color="primary" label="저장" @click="onSave" :disable="!hasUnsavedChanges" icon="save" />
            <q-btn color="secondary" label="초기화" @click="onReset" icon="refresh" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useFeedSettingsStore } from '@/stores/ui/feedSettingsStore'

const $q = useQuasar()
const feedSettingsStore = useFeedSettingsStore()

// 원본 설정 (초기값)
const originalBands = ref<('s' | 'x' | 'ka')[]>([...feedSettingsStore.enabledBands])

// 각 밴드의 활성화 상태
const sBandEnabled = computed({
    get: () => feedSettingsStore.isSBandEnabled,
    set: (value: boolean) => {
        if (value) {
            if (!feedSettingsStore.enabledBands.includes('s')) {
                feedSettingsStore.enabledBands.push('s')
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().catch((error) => {
                    console.error('S-Band 설정 저장 실패:', error)
                })
            }
        } else {
            const index = feedSettingsStore.enabledBands.indexOf('s')
            if (index > -1 && feedSettingsStore.enabledBandCount > 1) {
                feedSettingsStore.enabledBands.splice(index, 1)
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().catch((error) => {
                    console.error('S-Band 설정 저장 실패:', error)
                })
            } else if (feedSettingsStore.enabledBandCount === 1) {
                $q.notify({
                    color: 'warning',
                    message: '최소 하나의 밴드는 선택되어 있어야 합니다.',
                    icon: 'warning',
                    position: 'top',
                })
                return
            }
        }
    },
})

const xBandEnabled = computed({
    get: () => feedSettingsStore.isXBandEnabled,
    set: (value: boolean) => {
        if (value) {
            if (!feedSettingsStore.enabledBands.includes('x')) {
                feedSettingsStore.enabledBands.push('x')
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().catch((error) => {
                    console.error('X-Band 설정 저장 실패:', error)
                })
            }
        } else {
            const index = feedSettingsStore.enabledBands.indexOf('x')
            if (index > -1 && feedSettingsStore.enabledBandCount > 1) {
                feedSettingsStore.enabledBands.splice(index, 1)
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().catch((error) => {
                    console.error('X-Band 설정 저장 실패:', error)
                })
            } else if (feedSettingsStore.enabledBandCount === 1) {
                $q.notify({
                    color: 'warning',
                    message: '최소 하나의 밴드는 선택되어 있어야 합니다.',
                    icon: 'warning',
                    position: 'top',
                })
                return
            }
        }
    },
})

const kaBandEnabled = computed({
    get: () => feedSettingsStore.isKaBandEnabled,
    set: (value: boolean) => {
        if (value) {
            if (!feedSettingsStore.enabledBands.includes('ka')) {
                feedSettingsStore.enabledBands.push('ka')
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().then(() => {
                    console.log('✅ Ka-Band 활성화, 백엔드 저장 완료:', feedSettingsStore.enabledBands)
                }).catch((error) => {
                    console.error('Ka-Band 설정 저장 실패:', error)
                })
            }
        } else {
            const index = feedSettingsStore.enabledBands.indexOf('ka')
            if (index > -1 && feedSettingsStore.enabledBandCount > 1) {
                feedSettingsStore.enabledBands.splice(index, 1)
                // 백엔드 API에 저장 (로컬 스토리지는 saveSettings에서 처리)
                feedSettingsStore.saveSettings().then(() => {
                    console.log('✅ Ka-Band 비활성화, 백엔드 저장 완료:', feedSettingsStore.enabledBands)
                }).catch((error) => {
                    console.error('Ka-Band 설정 저장 실패:', error)
                })
            } else if (feedSettingsStore.enabledBandCount === 1) {
                $q.notify({
                    color: 'warning',
                    message: '최소 하나의 밴드는 선택되어 있어야 합니다.',
                    icon: 'warning',
                    position: 'top',
                })
                return
            }
        }
    },
})

const enabledBandCount = computed(() => feedSettingsStore.enabledBandCount)

// 변경사항 확인
const hasUnsavedChanges = computed(() => {
    const current = [...feedSettingsStore.enabledBands].sort()
    const original = [...originalBands.value].sort()
    return JSON.stringify(current) !== JSON.stringify(original)
})

// 저장 함수
const onSave = async () => {
    try {
        await feedSettingsStore.saveSettings()
        originalBands.value = [...feedSettingsStore.enabledBands]

        $q.notify({
            color: 'positive',
            message: '피드 설정이 저장되었습니다',
            icon: 'check',
            position: 'top',
        })
    } catch (error) {
        console.error('피드 설정 저장 실패:', error)
        $q.notify({
            color: 'negative',
            message: '피드 설정 저장에 실패했습니다',
            icon: 'error',
            position: 'top',
        })
    }
}

// 초기화 함수
const onReset = () => {
    feedSettingsStore.enabledBands = [...originalBands.value]

    $q.notify({
        color: 'info',
        message: '피드 설정이 초기화되었습니다',
        icon: 'refresh',
        position: 'top',
    })
}

// 초기 로드 시 원본 값 저장
onMounted(async () => {
    await feedSettingsStore.loadSettings()
    originalBands.value = [...feedSettingsStore.enabledBands]
})

watch(
    () => feedSettingsStore.enabledBands,
    () => {
        if (originalBands.value.length === 0) {
            originalBands.value = [...feedSettingsStore.enabledBands]
        }
    },
    { immediate: true }
)
</script>

<style scoped>
.feed-settings {
    max-width: 500px;
}

.q-banner {
    border-radius: 4px;
}
</style>
