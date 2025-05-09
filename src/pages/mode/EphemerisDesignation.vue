<template>
  <div class="ephemeris-mode">
    <h2 class="text-primary q-mb-md">Ephemeris Designation 모드</h2>
    <div class="ephemeris-form">
      <div class="form-row">
        <q-input v-model.number="ephemerisData.azimuth" label="방위각 (°)" type="number" outlined />
        <q-input
          v-model.number="ephemerisData.elevation"
          label="고도각 (°)"
          type="number"
          outlined
        />
        <q-input v-model.number="ephemerisData.tilt" label="틸트각 (°)" type="number" outlined />
      </div>
      <div class="form-row">
        <q-date v-model="ephemerisData.date" outlined />
        <q-time v-model="ephemerisData.time" outlined />
      </div>
      <q-btn color="primary" label="위치 지정" @click="sendEphemerisCommand" />
    </div>

    <!-- 현재 ICD 값 표시 (스토어에서 가져옴) -->
    <div class="current-values q-mt-lg q-pa-md bg-secondary-subtle">
      <h3 class="q-mb-sm">현재 ICD 값</h3>
      <p>방위각: {{ icdStore.azimuthAngle }}</p>
      <p>고도각: {{ icdStore.elevationAngle }}</p>
      <p>틸트각: {{ icdStore.tiltAngle }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { api } from 'boot/axios'
import { date } from 'quasar'
import { useICDStore } from '../../stores/ICD'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// Ephemeris Designation 모드 데이터
const ephemerisData = ref({
  azimuth: 0,
  elevation: 0,
  tilt: 0,
  date: date.formatDate(new Date(), 'YYYY/MM/DD'),
  time: date.formatDate(new Date(), 'HH:mm'),
})

// Ephemeris Designation 명령 전송
const sendEphemerisCommand = async () => {
  try {
    const datetime = `${ephemerisData.value.date} ${ephemerisData.value.time}`
    const command = {
      azimuthAngle: ephemerisData.value.azimuth,
      elevationAngle: ephemerisData.value.elevation,
      tiltAngle: ephemerisData.value.tilt,
      timestamp: datetime,
    }

    const response = await api.post('/icd/set-position', command)
    console.log('위치 지정 명령 전송 성공:', response.data)
  } catch (error) {
    console.error('위치 지정 명령 전송 실패:', error)
  }
}
</script>

<style scoped>
.ephemeris-form {
  margin-top: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.current-values {
  margin-top: 2rem;
  border-radius: 4px;
}
</style>
