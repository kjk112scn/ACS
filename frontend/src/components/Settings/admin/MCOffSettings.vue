<template>
  <div>
    <h5 class="q-mt-none q-mb-md">{{ T.settings.admin.mcOnOff }}</h5>

    <q-card class="command-card">
      <q-card-section>
        <div class="text-h6">{{ T.settings.admin.mcOnOffDetails.title }}</div>
        <div class="text-caption text-grey-6 q-mb-md">{{ T.settings.admin.mcOnOffDetails.description }}</div>

        <div class="q-mt-md">
          <q-btn-toggle v-model="mcState" :options="[
            { label: T.settings.admin.states.off, value: false },
            { label: T.settings.admin.states.on, value: true }
          ]" color="primary" class="full-width" :loading="isLoading" @click="handleToggle" />
        </div>
      </q-card-section>
    </q-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { T } from '@/texts'
import { useICDStore } from '@/stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useDialog } from '@/composables/useDialog'
import { useErrorHandler } from '@/composables/useErrorHandler'

const icdStore = useICDStore()
const { success } = useNotification()
const { confirm } = useDialog()
const { handleApiError } = useErrorHandler()

const isLoading = ref(false)
const mcState = ref(false)

const handleToggle = async () => {
  const stateText = mcState.value ? T.value.settings.admin.states.on : T.value.settings.admin.states.off
  const message = T.value.settings.admin.mcOnOffDetails.confirmMessage(stateText)

  const confirmed = await confirm(message, {
    title: T.value.settings.admin.mcOnOffDetails.confirmTitle,
    ok: { label: T.value.buttons.yes, color: 'positive' },
    cancel: { label: T.value.buttons.no, color: 'negative' },
  })

  if (!confirmed) return

  isLoading.value = true
  try {
    await icdStore.sendMCOnOffCommand(mcState.value)
    success(T.value.settings.admin.success)
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
