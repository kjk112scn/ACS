<template>
  <q-card flat bordered class="control-card">
    <q-card-section class="compact-control purple-1">
      <div class="flexible-offset-layout">
        <!-- Azimuth Offset -->
        <div class="offset-group">
          <div class="row q-gutter-xs align-center">
            <div class="col-auto position-offset-label">
              <div class="text-subtitle2 text-weight-bold text-primary text-center">
                Azimuth<br>Offset
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="inputs[0]"
                @update:model-value="(val: string | number | null) => onInputChange(0, String(val ?? ''))"
                dense
                outlined
                type="number"
                step="0.01"
                label="Azimuth"
                class="offset-input"
              />
            </div>
            <div class="col-auto">
              <div class="vertical-button-group">
                <div class="vertical-buttons">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
                </div>
                <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="outputs[0]"
                dense
                outlined
                readonly
                label="Output"
                class="offset-input"
              />
            </div>
          </div>
        </div>

        <!-- Elevation Offset -->
        <div class="offset-group">
          <div class="row q-gutter-xs align-center">
            <div class="col-auto position-offset-label">
              <div class="text-subtitle2 text-weight-bold text-primary text-center">
                Elevation<br>Offset
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="inputs[1]"
                @update:model-value="(val: string | number | null) => onInputChange(1, String(val ?? ''))"
                dense
                outlined
                type="number"
                step="0.01"
                label="Elevation"
                class="offset-input"
              />
            </div>
            <div class="col-auto">
              <div class="vertical-button-group">
                <div class="vertical-buttons">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
                </div>
                <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="outputs[1]"
                dense
                outlined
                readonly
                label="Output"
                class="offset-input"
              />
            </div>
          </div>
        </div>

        <!-- Tilt Offset -->
        <div class="offset-group">
          <div class="row q-gutter-xs align-center">
            <div class="col-auto position-offset-label">
              <div class="text-subtitle2 text-weight-bold text-primary text-center">
                Tilt<br>Offset
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="inputs[2]"
                @update:model-value="(val: string | number | null) => onInputChange(2, String(val ?? ''))"
                dense
                outlined
                type="number"
                step="0.01"
                label="Tilt"
                class="offset-input"
              />
            </div>
            <div class="col-auto">
              <div class="vertical-button-group">
                <div class="vertical-buttons">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
                </div>
                <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="outputs[2]"
                dense
                outlined
                readonly
                label="Output"
                class="offset-input"
              />
            </div>
          </div>
        </div>

        <!-- Time Offset + Cal Time -->
        <div class="offset-group">
          <div class="row q-gutter-xs align-center">
            <div class="col-auto position-offset-label">
              <div class="text-subtitle2 text-weight-bold text-primary text-center">
                Time<br>Offset
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="inputs[3]"
                @update:model-value="(val: string | number | null) => onInputChange(3, String(val ?? ''))"
                dense
                outlined
                type="number"
                step="0.01"
                label="Time"
                class="offset-input"
              />
            </div>
            <div class="col-auto">
              <div class="vertical-button-group">
                <div class="vertical-buttons">
                  <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                  <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
                </div>
                <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
              </div>
            </div>
            <div class="col-auto">
              <q-input
                :model-value="outputs[3]"
                dense
                outlined
                readonly
                label="Result"
                class="offset-input"
              />
            </div>
            <div class="col-auto cal-time-field">
              <q-input
                :model-value="calTime"
                dense
                outlined
                readonly
                label="Cal Time"
                class="cal-time-input"
              />
            </div>
          </div>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
interface Props {
  inputs: readonly string[] | string[]
  outputs: readonly string[] | string[]
  calTime: string
}

defineProps<Props>()

const emit = defineEmits<{
  inputChange: [index: number, value: string]
  increment: [index: number]
  decrement: [index: number]
  reset: [index: number]
}>()

const onInputChange = (index: number, value: string) => {
  emit('inputChange', index, value)
}

const increment = (index: number) => {
  emit('increment', index)
}

const decrement = (index: number) => {
  emit('decrement', index)
}

const reset = (index: number) => {
  emit('reset', index)
}
</script>

<style scoped>
/* 반응형 레이아웃 */
.flexible-offset-layout {
  display: flex;
  align-items: stretch;
  justify-content: center;
  width: 100%;
  gap: 40px;
  row-gap: 8px;
  flex-wrap: wrap;
}

.offset-group {
  flex: none;
  min-width: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.01);
  display: flex;
  align-items: center;
}

.position-offset-label {
  min-width: 80px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(25, 118, 210, 0.1);
  border: 1px solid rgba(25, 118, 210, 0.3);
}

.cal-time-field {
  flex-shrink: 0;
  min-width: 190px;
}

.vertical-button-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 4px;
}

.vertical-buttons {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.vertical-buttons .q-btn {
  min-height: 18px;
  height: 18px;
  padding: 0 4px;
}

.offset-input {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
}

.cal-time-input {
  min-width: 190px;
  max-width: 220px;
}

/* 반응형 동작 */
@media (max-width: 1900px) {
  .flexible-offset-layout {
    flex-wrap: wrap;
    gap: 20px;
    row-gap: 8px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
    padding: 8px;
  }

  .position-offset-label {
    min-width: 70px;
    font-size: 0.8rem;
  }

  .cal-time-field {
    min-width: 180px;
    max-width: 200px;
  }
}

@media (min-width: 1901px) {
  .flexible-offset-layout {
    flex-wrap: nowrap;
    gap: 40px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
  }

  .position-offset-label {
    min-width: 80px;
    font-size: 0.875rem;
  }
}
</style>
