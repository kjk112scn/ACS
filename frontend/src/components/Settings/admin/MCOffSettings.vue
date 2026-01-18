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
          ]" color="primary" class="full-width" :loading="isLoading" @click="handleToggle" />
        </div>
      </q-card-section>
    </q-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useDialog } from '@/composables/useDialog'
import { useErrorHandler } from '@/composables/useErrorHandler'

const { t } = useI18n()
const icdStore = useICDStore()
const { success } = useNotification()
const { confirm } = useDialog()
const { handleApiError } = useErrorHandler()

const isLoading = ref(false)
const mcState = ref(false)

const handleToggle = async () => {
  const stateText = t(`settings.admin.states.${mcState.value ? 'on' : 'off'}`)
  const message = t('settings.admin.mcOnOffDetails.confirmMessage', { state: stateText })

  const confirmed = await confirm(message, {
    title: t('settings.admin.mcOnOffDetails.confirmTitle'),
    ok: { label: t('buttons.yes'), color: 'positive' },
    cancel: { label: t('buttons.no'), color: 'negative' },
  })

  if (!confirmed) return

  isLoading.value = true
  try {
    await icdStore.sendMCOnOffCommand(mcState.value)
    success(t('settings.admin.success'))
  } catch (error) {
    handleApiError(error, 'M/C On/Off')
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
