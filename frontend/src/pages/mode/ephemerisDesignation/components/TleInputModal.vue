<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)">
    <q-card class="q-pa-md" style="width: 700px; max-width: 95vw">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">TLE 입력</div>
      </q-card-section>

      <q-card-section class="q-pa-md">
        <div class="text-body2 q-mb-md">
          2줄 또는 3줄 형식의 TLE 데이터를 입력하세요. 3줄 형식인 경우 첫 번째 줄은 위성 이름으로
          처리됩니다.
          <br />예시:
          <pre class="q-mt-sm q-pa-sm bg-grey-9 text-white rounded-borders example-tle">
ISS (ZARYA)
1 25544U 98067A   24054.51736111  .00020125  00000+0  36182-3 0  9999
2 25544  51.6416 142.1133 0003324 324.9821 218.2594 15.49780383446574</pre>
        </div>
        <div class="tle-input-container q-mb-md">
          <q-input
            v-model="tleText"
            type="textarea"
            filled
            autogrow
            class="tle-textarea full-width"
            style="min-height: 100px; font-family: monospace; font-size: 0.9rem"
            placeholder="TLE 데이터를 여기에 붙여넣으세요..."
            :input-style="'white-space: pre;'"
            spellcheck="false"
            autofocus
            :error="error !== null"
            :error-message="error || undefined"
            @keydown.ctrl.enter="handleAdd"
          />
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn
          flat
          label="추가"
          color="primary"
          @click="handleAdd"
          :loading="isProcessing"
          :disable="!tleText.trim()"
        />
        <q-btn
          flat
          label="닫기"
          color="primary"
          v-close-popup
          class="q-ml-sm"
          :disable="isProcessing"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue: boolean
  error?: string | null
  isProcessing?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  error: null,
  isProcessing: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  add: [tleText: string]
}>()

const tleText = ref('')

// 모달이 닫힐 때 입력 초기화
watch(() => props.modelValue, (newVal) => {
  if (!newVal) {
    tleText.value = ''
  }
})

const handleAdd = () => {
  if (tleText.value.trim()) {
    emit('add', tleText.value)
  }
}
</script>

<style scoped>
.example-tle {
  font-size: 0.8rem;
  white-space: pre-wrap;
}
</style>
