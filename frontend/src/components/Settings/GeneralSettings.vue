<template>
  <div>
    <h5 class="q-mt-none q-mb-md">일반 설정</h5>

    <q-list>
      <q-item>
        <q-item-section>
          <q-item-label>다크 모드</q-item-label>
          <q-item-label caption>어두운 테마로 전환합니다</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-toggle v-model="localDarkMode" @update:model-value="updateDarkMode" />
        </q-item-section>
      </q-item>
    </q-list>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, defineProps, defineEmits, onMounted } from 'vue'
import { useQuasar } from 'quasar'

const $q = useQuasar()

// Props 정의
const props = defineProps({
  darkMode: {
    type: Boolean,
    default: false,
  },
})

// Emits 정의
const emit = defineEmits(['update:darkMode'])

// 로컬 상태 관리
const localDarkMode = ref(props.darkMode)

// 컴포넌트 마운트 시 현재 다크 모드 상태 동기화
onMounted(() => {
  localDarkMode.value = $q.dark.isActive
})

// props 변경 감지
watch(
  () => props.darkMode,
  (newVal) => {
    localDarkMode.value = newVal
    // props가 변경되면 Quasar 다크 모드도 함께 변경
    $q.dark.set(newVal)
  },
)

// 다크 모드 업데이트 함수
const updateDarkMode = (value: boolean) => {
  // Quasar 다크 모드 설정 직접 변경
  $q.dark.set(value)

  // 로컬 스토리지에 설정 저장 (선택사항)
  localStorage.setItem('isDarkMode', String(value))

  // 부모 컴포넌트에 변경 알림
  emit('update:darkMode', value)
}
</script>
