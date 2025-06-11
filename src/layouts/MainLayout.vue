<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <!-- 좌측 섹션: 메뉴 버튼과 GTL ACS 로고 -->

        <div class="row items-center no-wrap">
          <q-btn
            flat
            dense
            round
            icon="menu"
            aria-label="Menu"
            @click="toggleLeftDrawer"
            class="q-mr-sm"
          />
          <div class="text-h6 no-ellipsis">GTL ACS</div>
        </div>

        <!-- 우측으로 밀어내기 위한 공간 -->
        <q-space />

        <!-- 우측 섹션: 설정 버튼과 테마 변경 버튼 -->
        <div class="row items-center">
          <q-btn
            flat
            dense
            round
            icon="settings"
            aria-label="Settings"
            @click="settingsModal = true"
            class="q-mr-sm"
          />
          <q-btn
            flat
            dense
            round
            icon="brightness_4"
            aria-label="Toggle Dark Mode"
            @click="toggleDarkMode"
          />
        </div>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" bordered>
      <q-list>
        <q-item-label header> Essential Links </q-item-label>

        <EssentialLink v-for="link in linksList" :key="link.title" v-bind="link" />
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>

    <!-- 설정 모달 컴포넌트 사용 -->
    <SettingsModal
      v-model="settingsModal"
      :dark-mode="isDarkMode"
      :server-address="serverAddress"
      @save="handleSettingsSave"
    />
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import EssentialLink, { type EssentialLinkProps } from 'components/EssentialLink.vue'
import SettingsModal from 'src/components/modal/Settings/SettingsModal.vue'

import { useQuasar } from 'quasar'

const $q = useQuasar()
const linksList: EssentialLinkProps[] = [
  {
    title: 'Docs',
    caption: 'quasar.dev',
    icon: 'school',
    link: 'https://quasar.dev',
  },
  {
    title: 'Github',
    caption: 'github.com/quasarframework',
    icon: 'code',
    link: 'https://github.com/quasarframework',
  },
  {
    title: 'Discord Chat Channel',
    caption: 'chat.quasar.dev',
    icon: 'chat',
    link: 'https://chat.quasar.dev',
  },
  {
    title: 'Forum',
    caption: 'forum.quasar.dev',
    icon: 'record_voice_over',
    link: 'https://forum.quasar.dev',
  },
  {
    title: 'Twitter',
    caption: '@quasarframework',
    icon: 'rss_feed',
    link: 'https://twitter.quasar.dev',
  },
  {
    title: 'Facebook',
    caption: '@QuasarFramework',
    icon: 'public',
    link: 'https://facebook.quasar.dev',
  },
  {
    title: 'Quasar Awesome',
    caption: 'Community Quasar projects',
    icon: 'favorite',
    link: 'https://awesome.quasar.dev',
  },
]

// 명시적으로 false로 설정하고 show-if-above 속성 제거
const leftDrawerOpen = ref(false)

// 설정 모달 상태
const settingsModal = ref(false)

// 다크 모드 상태
const isDarkMode = ref(false)

// 서버 주소 설정
const serverAddress = ref('ws://localhost:8080/ws/push-data')

// 다크 모드 토글
const toggleDarkMode = () => {
  const newState = !$q.dark.isActive
  $q.dark.set(newState)
  localStorage.setItem('isDarkMode', String(newState))
}

// 설정 저장 핸들러
const handleSettingsSave = (settings: { darkMode: boolean; serverAddress: string }) => {
  // 다크 모드 설정 적용
  if (settings.darkMode !== isDarkMode.value) {
    $q.dark.set(settings.darkMode)
    isDarkMode.value = settings.darkMode
    localStorage.setItem('isDarkMode', String(settings.darkMode))
  }

  // 서버 주소 설정 적용
  serverAddress.value = settings.serverAddress
  localStorage.setItem('serverAddress', settings.serverAddress)

  // 여기에 필요한 경우 서버 연결 재설정 로직 추가
}

// 왼쪽 drawer 토글
function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

// 컴포넌트가 마운트될 때 로컬 스토리지에서 다크 모드 설정 불러오기
onMounted(() => {
  leftDrawerOpen.value = false

  // 로컬 스토리지에서 다크 모드 설정 불러오기
  const savedDarkMode = localStorage.getItem('isDarkMode')
  if (savedDarkMode !== null) {
    const isDarkMode = savedDarkMode === 'true'
    $q.dark.set(isDarkMode)
  }
})
</script>
