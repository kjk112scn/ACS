<template>
  <div class="language-settings">
    <h5 class="q-mt-none q-mb-md">{{ T.settings.general.language }}</h5>

    <div class="language-options">
      <q-card v-for="lang in availableLanguages" :key="lang.code" class="q-mb-md language-card"
        :class="{ 'selected': selectedLanguage === lang.code }" flat bordered clickable
        @click="selectLanguageHandler(lang.code)">
        <q-card-section class="q-pa-md">
          <div class="row items-center">
            <div class="col">
              <div class="text-h6">{{ lang.name }}</div>
              <div class="text-caption text-grey-6">{{ lang.description }}</div>
            </div>
            <div class="col-auto">
              <q-icon v-if="selectedLanguage === lang.code" name="check_circle" color="primary" size="24px" />
            </div>
          </div>
        </q-card-section>
      </q-card>
    </div>

    <!-- 현재 언어 정보 -->
    <div class="current-language-info q-mt-md">
      <q-banner class="bg-primary-1 text-primary">
        <template v-slot:avatar>
          <q-icon name="info" color="primary" />
        </template>
        {{ T.settings.language.current }}: <strong>{{ currentLanguageName }}</strong>
      </q-banner>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { T, setLanguage, getCurrentLanguage, type Language } from '@/texts'
import { useNotification } from '@/composables/useNotification'

const { success } = useNotification()

// 사용 가능한 언어 목록
const availableLanguages = ref([
  {
    code: 'ko',
    name: '한국어',
    description: 'Korean',
    flag: '🇰🇷'
  },
  {
    code: 'en',
    name: 'English',
    description: 'English (US)',
    flag: '🇺🇸'
  }
])

const selectedLanguage = ref<Language>(getCurrentLanguage())

// 현재 선택된 언어의 이름
const currentLanguageName = computed(() => {
  const lang = availableLanguages.value.find(l => l.code === selectedLanguage.value)
  return lang ? lang.name : 'Unknown'
})

// 언어 선택
const selectLanguageHandler = (langCode: string) => {
  const lang = langCode as Language
  selectedLanguage.value = lang
  setLanguage(lang)

  // 성공 메시지 (선택된 언어로)
  const langName = availableLanguages.value.find(l => l.code === langCode)?.name || langCode
  success(T.value.settings.language.changed(langName))
}

// 컴포넌트 마운트 시 저장된 언어 불러오기
onMounted(() => {
  selectedLanguage.value = getCurrentLanguage()
})
</script>

<style scoped>
.language-settings {
  max-width: 600px;
}

.language-options {
  display: grid;
  gap: 12px;
}

.language-card {
  transition: all 0.3s ease;
  cursor: pointer;
}

.language-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

/* 다크테마 지원을 위한 CSS 변수 사용 */
.language-card.selected {
  border: 2px solid var(--q-primary);
  background-color: var(--q-primary-1);
}

.language-card.selected:hover {
  background-color: var(--q-primary-2);
}

/* 다크테마에서 호버 효과 개선 */
body.body--dark .language-card:hover {
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
}

.current-language-info {
  margin-top: 16px;
}
</style>