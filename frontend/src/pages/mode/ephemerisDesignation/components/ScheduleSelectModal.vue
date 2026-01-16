<template>
  <q-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    maximized
  >
    <q-card class="q-pa-md" style="width: 1200px; max-width: 98vw; max-height: 70vh">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">Select Schedule</div>
      </q-card-section>

      <q-card-section class="q-pa-md" style="max-height: 50vh; overflow: auto">
        <q-table
          :rows="scheduleData"
          :columns="columns"
          row-key="No"
          :loading="loading"
          :pagination="{ rowsPerPage: 10 }"
          selection="single"
          v-model:selected="selectedItems"
          class="bg-grey-9 text-white"
          dark
          flat
          bordered
        >
          <!-- 슬롯은 부모 컴포넌트에서 전달받음 -->
          <slot name="body-cells" />

          <template v-slot:loading>
            <q-inner-loading showing color="primary">
              <q-spinner size="50px" color="primary" />
            </q-inner-loading>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          label="Select"
          color="primary"
          @click="handleSelect"
          :disable="selectedItems.length === 0"
        />
        <q-btn flat label="Close" color="primary" v-close-popup class="q-ml-sm" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { QTableProps } from 'quasar'

type QTableColumn = NonNullable<QTableProps['columns']>[0]

interface ScheduleItem {
  No: number
  [key: string]: unknown
}

interface Props {
  modelValue: boolean
  scheduleData: ScheduleItem[]
  columns: QTableColumn[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [item: ScheduleItem]
}>()

const selectedItems = ref<ScheduleItem[]>([])

// 모달이 닫힐 때 선택 초기화
watch(() => props.modelValue, (newVal) => {
  if (!newVal) {
    selectedItems.value = []
  }
})

const handleSelect = () => {
  if (selectedItems.value.length > 0) {
    emit('select', selectedItems.value[0])
    emit('update:modelValue', false)
  }
}
</script>

<style scoped>
/* 테이블 스타일은 부모의 전역 스타일을 상속 */
</style>
