<template>
  <div>
    <h1>실시간 센서 데이터</h1>
    <p>타임스탬프: {{ timestamp }}</p>
    <p>값: {{ value }}</p>
    <p v-if="error" style="color: red">오류 발생: {{ error }}</p>
    <p v-if="!isConnected">WebSocket 연결 중...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const timestamp = ref('')
const value = ref('')
const error = ref('')
const isConnected = ref(false)
let websocket: WebSocket | undefined // 명시적인 타입 지정

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
      timestamp.value = data.timestamp
      value.value = data.value
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
