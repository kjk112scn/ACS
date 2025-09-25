<template>
  <div>
    <h5 class="q-mt-none q-mb-md">{{ $t('settings.admin.mcOnOff') }}</h5>

    <q-card class="command-card">
      <q-card-section>
        <div class="text-h6">{{ $t('settings.admin.mcOnOffDetails.title') }}</div>
        <div class="text-caption text-grey-6 q-mb-md">{{ $t('settings.admin.mcOnOffDetails.description') }}</div>

        <div class="q-mt-md">
          <q-btn-toggle v-model="mcState" :options="[
            { label: $t('settings.admin.states.off'), value: false },
            { label: $t('settings.admin.states.on'), value: true }
          ]" color="primary" class="full-width" :loading="isLoading" @click="showConfirmation" />
        </div>
      </q-card-section>
    </q-card>

    <!-- 확인 모달 -->
    <q-dialog v-model="confirmationDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section class="row items-center">
          <div class="text-h6">{{ $t('settings.admin.mcOnOffDetails.confirmTitle') }}</div>
        </q-card-section>

        <q-card-section>
          <p>{{ $t('settings.admin.mcOnOffDetails.confirmMessage', {
            state: $t(`settings.admin.states.${mcState ? 'on' : 'off'}`)
          }) }}</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat :label="$t('buttons.no')" color="negative" v-close-popup @click="cancelConfirmation"
            :disable="isLoading" />
          <q-btn flat :label="$t('buttons.yes')" color="positive" @click="confirmExecution" :loading="isLoading"
            :disable="isLoading" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'

const { t } = useI18n()
const icdStore = useICDStore()
const { success, error: showError } = useNotification()

// 로딩 상태
const isLoading = ref(false)
const mcState = ref(false)

// 확인 모달 상태
const confirmationDialog = ref(false)

// 확인 모달 표시
const showConfirmation = () => {
  confirmationDialog.value = true
}

// 확인 모달 취소
const cancelConfirmation = () => {
  confirmationDialog.value = false
}

// 실행 확인
const confirmExecution = async () => {
  isLoading.value = true

  try {
    await icdStore.sendMCOnOffCommand(mcState.value)

    success(t('settings.admin.success'))
    confirmationDialog.value = false
  } catch (error) {
    console.error('M/C On/Off 실행 실패:', error)
    showError(t('settings.admin.error'))
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.command-card {
  transition: all 0.3s ease;
}

.command-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 다크테마에서 호버 효과 개선 */
body.body--dark .command-card:hover {
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
}
</style>
