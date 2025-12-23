<template>
  <q-page class="flex flex-center bg-dark text-white">
    <q-card class="q-pa-lg" style="width: 300px">
      <q-form @submit.prevent="login">
        <q-input filled v-model="username" label="Username" class="q-mb-md" dark />
        <q-input filled v-model="password" label="Password" type="password" class="q-mb-md" dark />
        <q-btn label="Login" type="submit" color="primary" class="full-width" />
      </q-form>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'

const username = ref('')
const password = ref('')
const router = useRouter()
const auth = useAuthStore()

const login = () => {
  if (username.value === 'de' && password.value === 'de') {
    auth.login() // localStorage에 저장됨

    // 팝업 창들에게 인증 상태 변경 알림
    broadcastAuthChange(true)

    console.log('✅ 메인 창에서 로그인 성공')

    router.push('/dashboard').catch((err) => {
      console.error('Navigation error:', err)
    })
  } else {
    alert('Invalid credentials')
  }
}

// 다른 창들에게 인증 상태 변경 알림
const broadcastAuthChange = (isLoggedIn: boolean) => {
  try {
    const channel = new BroadcastChannel('auth-channel')
    channel.postMessage({
      type: 'auth-status-changed',
      isLoggedIn: isLoggedIn,
      timestamp: Date.now()
    })
    channel.close()
    console.log('📡 인증 상태 브로드캐스트:', isLoggedIn)
  } catch (error) {
    console.warn('브로드캐스트 실패:', error)
  }
}
</script>
