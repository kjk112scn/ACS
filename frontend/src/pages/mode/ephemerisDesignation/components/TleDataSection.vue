<template>
  <q-card class="control-section">
    <q-card-section class="schedule-control-section">
      <div class="schedule-header">
        <div class="text-subtitle1 text-weight-bold text-primary schedule-header-title">TLE Data</div>
      </div>
      <div class="tle-display q-mt-sm">
        <pre class="tle-content">{{ tleDisplayText || 'No TLE data available' }}</pre>
      </div>
      <div class="button-group">
        <div class="button-row">
          <q-btn
            color="primary"
            label="Text"
            icon="description"
            @click="emit('openTleModal')"
            class="q-mr-sm upload-btn"
            size="sm"
          />
          <q-btn
            color="primary"
            label="Select Schedule"
            icon="playlist_add_check"
            @click="emit('openScheduleModal')"
            class="upload-btn"
            size="sm"
          />
        </div>
        <div class="control-button-row">
          <q-btn
            color="positive"
            icon="play_arrow"
            label="Go"
            @click="emit('goCommand')"
            class="control-btn"
            size="sm"
          />
          <q-btn
            color="negative"
            icon="stop"
            label="Stop"
            @click="emit('stopCommand')"
            class="control-btn"
            size="sm"
          />
          <q-btn
            color="warning"
            icon="home"
            label="Stow"
            @click="emit('stowCommand')"
            class="control-btn"
            size="sm"
          />
        </div>
        <div class="button-row">
          <q-btn
            color="info"
            label="Axis Calculator"
            icon="calculate"
            @click="emit('openAxisCalculator')"
            class="upload-btn"
            size="sm"
          />
          <q-btn
            color="secondary"
            label="이론치 다운로드"
            icon="download"
            @click="emit('exportCsv')"
            :loading="isExportingCsv"
            class="upload-btn"
            size="sm"
          />
          <q-btn
            color="secondary"
            label="Download"
            icon="download"
            @click="emit('downloadRealtimeData')"
            class="upload-btn"
            size="sm"
          />
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
interface Props {
  tleDisplayText: string
  isExportingCsv: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  openTleModal: []
  openScheduleModal: []
  goCommand: []
  stopCommand: []
  stowCommand: []
  openAxisCalculator: []
  exportCsv: []
  downloadRealtimeData: []
}>()
</script>

<style scoped>
.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tle-display {
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 12px;
}

.tle-content {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.8rem;
  color: #4fc3f7;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.button-row {
  display: flex;
  gap: 8px;
}

.control-button-row {
  display: flex;
  gap: 8px;
}

.upload-btn {
  flex: 1;
}

.control-btn {
  flex: 1;
}
</style>
