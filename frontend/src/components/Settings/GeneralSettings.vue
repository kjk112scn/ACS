<template>
  <div>
    <h5 class="q-mt-none q-mb-md">일반 설정</h5>

    <q-list>
      <!-- 다크 모드 설정 -->
      <q-item>
        <q-item-section>
          <q-item-label>다크 모드</q-item-label>
          <q-item-label caption>어두운 테마로 전환합니다</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-toggle v-model="localDarkMode" @update:model-value="updateDarkMode" />
        </q-item-section>
      </q-item>

      <q-separator spaced />

      <!-- Timezone 설정 -->
      <q-item-label header>시간대 설정</q-item-label>

      <q-item>
        <q-item-section>
          <q-item-label>자동 감지 사용</q-item-label>
          <q-item-label caption>브라우저 시간대를 자동으로 사용합니다</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-toggle v-model="useAutoDetect" @update:model-value="onAutoDetectChange" />
        </q-item-section>
      </q-item>

      <q-item v-if="!useAutoDetect">
        <q-item-section>
          <q-item-label>시간대 선택</q-item-label>
          <q-select
            v-model="selectedTimezone"
            :options="filteredTimezones"
            option-value="value"
            option-label="label"
            emit-value
            map-options
            outlined
            dense
            use-input
            input-debounce="200"
            @filter="onFilterTimezone"
            @update:model-value="onTimezoneChange"
            class="q-mt-sm"
          >
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                <q-item-section v-if="scope.opt.header" class="text-weight-bold text-grey-7">
                  {{ scope.opt.label }}
                </q-item-section>
                <template v-else>
                  <q-item-section>
                    <q-item-label>{{ scope.opt.label }}</q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-badge :label="scope.opt.offset" color="grey-7" />
                  </q-item-section>
                </template>
              </q-item>
            </template>
          </q-select>
        </q-item-section>
      </q-item>

      <!-- 현재 시간대 정보 -->
      <q-item>
        <q-item-section>
          <q-banner class="bg-blue-1 text-blue-9" rounded dense>
            <template v-slot:avatar>
              <q-icon name="schedule" color="blue" />
            </template>
            현재 시간대: <strong>{{ timezoneStore.displayString }}</strong>
            <span v-if="useAutoDetect" class="text-caption q-ml-sm">(자동 감지)</span>
          </q-banner>
        </q-item-section>
      </q-item>
    </q-list>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useTimezoneStore } from '@/stores/common/timezoneStore'
import { useTimezone, type TimezoneOption } from '@/composables/useTimezone'

const $q = useQuasar()
const timezoneStore = useTimezoneStore()
const { groupedTimezones } = useTimezone()

// Props 정의
const props = defineProps({
  darkMode: {
    type: Boolean,
    default: false,
  },
})

// Emits 정의
const emit = defineEmits(['update:darkMode'])

// ========================================
// 다크 모드 상태
// ========================================
const localDarkMode = ref(props.darkMode)

// ========================================
// Timezone 상태
// ========================================
const useAutoDetect = ref(timezoneStore.useAutoDetect)
const selectedTimezone = ref(timezoneStore.manualTimezone)
const filteredTimezones = ref<TimezoneOption[]>(groupedTimezones.value)

// ========================================
// 다크 모드 함수
// ========================================
onMounted(() => {
  localDarkMode.value = $q.dark.isActive
})

watch(
  () => props.darkMode,
  (newVal) => {
    localDarkMode.value = newVal
    $q.dark.set(newVal)
  },
)

const updateDarkMode = (value: boolean) => {
  $q.dark.set(value)
  localStorage.setItem('isDarkMode', String(value))
  emit('update:darkMode', value)
}

// ========================================
// Timezone 함수
// ========================================
const onAutoDetectChange = (value: boolean) => {
  timezoneStore.setAutoDetect(value)
}

const onTimezoneChange = (value: string) => {
  timezoneStore.setTimezone(value)
}

const onFilterTimezone = (
  val: string,
  update: (callback: () => void) => void
) => {
  if (val === '') {
    update(() => {
      filteredTimezones.value = groupedTimezones.value
    })
    return
  }

  update(() => {
    const needle = val.toLowerCase()
    filteredTimezones.value = groupedTimezones.value.filter(
      (tz) =>
        !tz.header &&
        (tz.value.toLowerCase().includes(needle) ||
          tz.offset.toLowerCase().includes(needle))
    )
  })
}
</script>
