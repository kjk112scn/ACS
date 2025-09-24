<template>
  <div>
    <h5 class="q-mt-none q-mb-md">M/C On/Off</h5>

    <q-card class="command-card">
      <q-card-section>
        <div class="text-h6">M/C 상태 제어</div>
        <div class="text-caption text-grey-6 q-mb-md">M/C On/Off 명령 실행</div>

        <div class="q-mt-md">
          <q-btn-toggle v-model="mcState" :options="[
            { label: 'OFF', value: false },
            { label: 'ON', value: true }
          ]" color="primary" class="full-width" :loading="isLoading" @click="showConfirmation" />
        </div>
      </q-card-section>
    </q-card>

    <!-- 확인 모달 -->
    <q-dialog v-model="confirmationDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center">
          <div class="text-h6">M/C On/Off 확인</div>
        </q-card-section>

        <q-card-section>
          <p>M/C {{ mcState ? 'ON' : 'OFF' }} 명령을 실행하시겠습니까?</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="아니오" color="negative" v-close-popup @click="cancelConfirmation" :disable="isLoading" />
          <q-btn flat label="예" color="positive" @click="confirmExecution" :loading="isLoading" :disable="isLoading" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

const icdStore = useICDStore()
const { success, error: showError } = useNotification()

// 로딩 상태
const isLoading = ref(false)
const mcState = ref(false)

// 확인 모달 관련 상태
const confirmationDialog = ref(false)

// 확인 모달 표시
const showConfirmation = () => {
  confirmationDialog.value = true
}

// 확인 취소
const cancelConfirmation = () => {
  confirmationDialog.value = false
}

// 확인 및 명령 실행
const confirmExecution = async () => {
  try {
    isLoading.value = true

    // M/C On/Off 명령 실행
    const result = await icdStore.sendMCOnOffCommand(mcState.value)

    console.log('🔍 M/C 명령 결과:', result) // 디버깅용

    // ✅ status 필드로 확인
    if (result && result.status === 'success') {
      success(`M/C ${mcState.value ? 'ON' : 'OFF'} 명령이 성공적으로 실행되었습니다.`)

      // 모달 닫기
      confirmationDialog.value = false
    } else {
      showError(result?.message || '명령 실행 중 오류가 발생했습니다.')
    }
  } catch (error) {
    console.error('M/C On/Off error:', error)
    showError('명령 처리 중 오류가 발생했습니다.')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.command-card {
  min-height: 200px;
}
</style>
