<template>
  <div class="control-button-bar mode-button-bar">
    <div class="row q-col-gutter-md justify-center">
      <!-- Go Button -->
      <div v-if="showGo" class="col-auto">
        <q-btn
          :label="goLabel"
          color="positive"
          icon="play_arrow"
          size="lg"
          :loading="goLoading"
          :disable="goDisabled"
          @click="$emit('go')"
        />
      </div>

      <!-- Stop Button -->
      <div v-if="showStop" class="col-auto">
        <q-btn
          :label="stopLabel"
          color="negative"
          icon="stop"
          size="lg"
          :loading="stopLoading"
          :disable="stopDisabled"
          @click="$emit('stop')"
        />
      </div>

      <!-- Stow Button -->
      <div v-if="showStow" class="col-auto">
        <q-btn
          :label="stowLabel"
          color="warning"
          icon="home"
          size="lg"
          :loading="stowLoading"
          :disable="stowDisabled"
          @click="$emit('stow')"
        />
      </div>

      <!-- Custom Slot for additional buttons -->
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  // Visibility
  showGo?: boolean
  showStop?: boolean
  showStow?: boolean
  // Labels
  goLabel?: string
  stopLabel?: string
  stowLabel?: string
  // Loading states
  goLoading?: boolean
  stopLoading?: boolean
  stowLoading?: boolean
  // Disabled states
  goDisabled?: boolean
  stopDisabled?: boolean
  stowDisabled?: boolean
}

withDefaults(defineProps<Props>(), {
  showGo: true,
  showStop: true,
  showStow: true,
  goLabel: 'Go',
  stopLabel: 'Stop',
  stowLabel: 'Stow',
  goLoading: false,
  stopLoading: false,
  stowLoading: false,
  goDisabled: false,
  stopDisabled: false,
  stowDisabled: false,
})

defineEmits<{
  go: []
  stop: []
  stow: []
}>()
</script>

<style scoped>
.control-button-bar {
  margin-top: 1rem;
  padding-top: 0;
  flex-shrink: 0;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* mode-button-bar 스타일은 mode-common.scss에서 상속 */
</style>
