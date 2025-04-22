<template>
  <q-page class="flex flex-center bg-dark text-white">
    <q-card class="q-pa-lg" style="width: 300px">
      <q-form @submit.prevent="login">
        <q-input
          filled
          v-model="username"
          label="Username"
          class="q-mb-md"
          dark
        />
        <q-input
          filled
          v-model="password"
          label="Password"
          type="password"
          class="q-mb-md"
          dark
        />
        <q-btn label="Login" type="submit" color="primary" class="full-width" />
      </q-form>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'stores/auth'

const username = ref('')
const password = ref('')
const router = useRouter()
const auth = useAuthStore()

const login = () => {
  if (username.value === 'de' && password.value === 'de') {
    auth.login()
    router.push('/dashboard').catch((err) => {
      console.error('Navigation error:', err)
    })
  } else {
    alert('Invalid credentials')
  }
}
</script>
