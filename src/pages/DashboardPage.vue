<template>
  <div>
    <h1>실시간 센서 데이터</h1>
    <p>mode bit : {{ modeStatusBits }}</p>
    <p>azimuthAngle: {{ azimuthAngle }}</p>
    <p>azimuthSpeed : {{ azimuthSpeed }}</p>
    <p>azimuthAngle: {{ elevationAngle }}</p>
    <p>elevationSpeed : {{ elevationSpeed }}</p>
    <p>azimuthAngle: {{ tiltAngle }}</p>
    <p>azimuthAngle: {{ tiltSpeed }}</p>
    <p v-if="error" style="color: red">오류 발생: {{ error }}</p>
    <p v-if="!isConnected">WebSocket 연결 중...</p>
    <q-btn label="Emergency Command 전송" @click="sendEmergency" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { api } from 'boot/axios' // 또는 import axios from 'axios';
const modeStatusBits = ref('')
const azimuthAngle = ref('')
const azimuthSpeed = ref('')
const elevationAngle = ref('')
const elevationSpeed = ref('')
const tiltAngle = ref('')
const tiltSpeed = ref('')
const error = ref('')
const isConnected = ref(false)
let websocket: WebSocket | undefined // 명시적인 타입 지정

const data = ref(null)
const sendEmergency = async () => {
  try {
    const response = await api.post('/icd/on-emergency-stop-command') // POST 요청으로 변경
    data.value = response.data
    console.log('API 응답:', response.data)
  } catch (error) {
    console.error('API 호출 실패:', error)
  }
}

const connectWebSocket = () => {
  websocket = new WebSocket('ws://localhost:8080/ws/sensor-data') // 백엔드 WebSocket 엔드포인트 주소

  websocket.onopen = () => {
    console.log('WebSocket 연결 성공.')
    isConnected.value = true
    error.value = ''
  }

  websocket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      modeStatusBits.value = data.modeStatusBits
      azimuthAngle.value = data.azimuthAngle
      azimuthSpeed.value = data.azimuthSpeed
      elevationAngle.value = data.elevationAngle
      elevationSpeed.value = data.elevationSpeed
      tiltAngle.value = data.tiltAngle
      tiltSpeed.value = data.tiltSpeed
      console.log('WebSocket 메시지 수신:', data)
    } catch (e) {
      console.error('JSON 파싱 오류:', e)
      error.value = '데이터 파싱 오류 발생'
    }
  }

  websocket.onclose = () => {
    console.log('WebSocket 연결 종료.')
    isConnected.value = false
    // 재연결 시도 (선택 사항)
    setTimeout(connectWebSocket, 3000) // 3초 후 재시도
  }

  websocket.onerror = (err) => {
    console.error('WebSocket 오류:', err)
    error.value = 'WebSocket 연결 오류 발생'
    isConnected.value = false
  }
}

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  if (websocket) {
    websocket.close()
  }
})
</script>

<style scoped>
h1 {
  margin-bottom: 1rem;
}
p {
  margin-bottom: 0.5rem;
}
</style>
