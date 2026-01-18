<template>
  <div class="mode-layout" :class="layoutClass">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  mode?: string // 'sun-track', 'ephemeris', 'pass-schedule', etc.
}

const props = defineProps<Props>()

const layoutClass = computed(() => {
  const classes: string[] = []
  if (props.mode) {
    classes.push(`${props.mode}-mode`)
  }
  return classes
})
</script>

<style scoped>
.mode-layout {
  height: auto;
  width: 100%;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  box-sizing: border-box;
}

/* 하위 요소 마진/패딩 제거 */
.mode-layout > :deep(*) {
  margin-bottom: 0;
  padding-bottom: 0;
}

.mode-layout > :deep(*:last-child) {
  margin-bottom: 0;
  padding-bottom: 0;
}
</style>
