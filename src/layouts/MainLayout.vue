<template>
  <q-layout view="lHh Lpr lFf">
    <q-header class="custom-header">
      <q-toolbar class="header-toolbar">
        <!-- 좌측 섹션: 메뉴 버튼과 로고 -->
        <div class="left-section">
          <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" class="q-mr-sm" />
          <img src="/logo/GTL_LOGO.png" alt="GTL Logo" class="header-logo q-mr-md" />
        </div>

        <!-- 가운데 섹션: Antenna Control System -->
        <div class="center-section">
          <div class="text-h4 no-ellipsis text-center">Antenna Control System</div>
        </div>

        <!-- 우측 섹션: 시간 정보 + 설정 버튼들 (2행) -->
        <div class="right-section">
          <!-- 시간 정보 (1행) -->
          <div class="time-row">
            <div class="time-info">
              <div class="utc-time">UTC: {{ displayUTCTime }}</div>
              <div class="local-time">Local: {{ displayLocalTime }}</div>
            </div>
          </div>

          <!-- 설정 버튼들 (2행) -->
          <div class="buttons-row">
            <!-- 서버 상태 표시 부분 완전 제거 -->
            <div class="server-status">
              <span v-if="icdStore.error" class="text-negative">Server : Error: {{ icdStore.error }}</span>
              <span v-else-if="!icdStore.isConnected" class="text-warning">Server : WebSocket Connecting...</span>
              <span v-else-if="icdStore.isConnected && !icdStore.error" class="text-positive">Server : Connected</span>
            </div>

            <!-- 설정 버튼들만 남기기 -->
            <q-btn flat dense round icon="settings" aria-label="Settings" @click="settingsModal = true"
              class="q-mr-sm" />
            <q-btn flat dense round icon="brightness_4" aria-label="Toggle Dark Mode" @click="toggleDarkMode" />
            <q-btn flat dense round icon="info" aria-label="SystemsInfo" size="md" @click="handleSystemInfo" />
          </div>
        </div>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" bordered>
      <q-list>
        <q-item-label header> Essential Links </q-item-label>

        <EssentialLink v-for="link in linksList" :key="link.title" v-bind="link" />
      </q-list>
    </q-drawer>

    <q-page-container class="no-padding">
      <router-view />
    </q-page-container>

    <!-- 설정 모달 컴포넌트 사용 -->
    <SettingsModal v-model="settingsModal" :dark-mode="isDarkMode" :server-address="serverAddress"
      @save="handleSettingsSave" />
    <!-- 하드웨어 에러 로그 패널 (하단 고정) -->
    <!--     <HardwareErrorLogPanel /> -->

    <!-- 하단 고정 바 - 실시간 에러 상태 표시 -->
    <div class="error-status-bar" v-if="true">
      <div class="error-message">
        <q-icon :name="getSeverityIcon()" :color="getSeverityColor()" class="q-mr-sm" />
        <!-- 실시간 에러 상태 표시 -->
        <span v-if="currentErrorMessage">
          {{ currentErrorMessage }}
        </span>
        <span v-else>시스템 정상</span>
      </div>

      <q-btn icon="bug_report" color="primary" round dense @click="openErrorLogPopup" class="log-button" />
    </div>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import EssentialLink, { type EssentialLinkProps } from '@/components/common/EssentialLink.vue'
import SettingsModal from '@/components/settings/SettingsModal.vue'
import { openComponent } from '@/utils/windowUtils' // ✅ 기존 함수 사용
import { useQuasar } from 'quasar'
import { useICDStore } from '@/stores/icd/icdStore' // ICD Store import 추가
//import HardwareErrorLogPanel from '@/components/HardwareErrorLogPanel.vue'
import { useHardwareErrorLogStore } from '@/stores/hardwareErrorLogStore'

const $q = useQuasar()
const icdStore = useICDStore() // Store 사용
const hardwareErrorLogStore = useHardwareErrorLogStore()

// UTC 시간 표시용 computed (24시간 형식) - Local 시간 기준으로 실시간 업데이트
const displayUTCTime = computed(() => {
  if (!icdStore.serverTime) {
    return '서버 시간 대기 중...'
  }

  // ICD Store에서 가져온 서버 시간을 UTC로 변환
  const serverTime = new Date(icdStore.serverTime)
  const year = serverTime.getUTCFullYear()
  const month = String(serverTime.getUTCMonth() + 1).padStart(2, '0')
  const day = String(serverTime.getUTCDate()).padStart(2, '0')
  const hours = String(serverTime.getUTCHours()).padStart(2, '0')
  const minutes = String(serverTime.getUTCMinutes()).padStart(2, '0')
  const seconds = String(serverTime.getUTCSeconds()).padStart(2, '0')
  const milliseconds = String(serverTime.getUTCMilliseconds()).padStart(3, '0')

  return `${year}. ${month}. ${day}. ${hours}:${minutes}:${seconds}.${milliseconds} UTC`
})

// 로컬 시간 표시용 computed (24시간 형식, ms 포함) - ICD Store 서버 시간 사용
const displayLocalTime = computed(() => {
  if (!icdStore.serverTime) {
    return '서버 시간 대기 중...'
  }

  // ICD Store에서 가져온 서버 시간을 로컬 시간으로 변환
  const serverTime = new Date(icdStore.serverTime)
  const year = serverTime.getFullYear()
  const month = String(serverTime.getMonth() + 1).padStart(2, '0')
  const day = String(serverTime.getDate()).padStart(2, '0')
  const hours = String(serverTime.getHours()).padStart(2, '0')
  const minutes = String(serverTime.getMinutes()).padStart(2, '0')
  const seconds = String(serverTime.getSeconds()).padStart(2, '0')
  const milliseconds = String(serverTime.getMilliseconds()).padStart(3, '0')

  return `${year}. ${month}. ${day}. ${hours}:${minutes}:${seconds}.${milliseconds} KST`
})

// 에러 상태 관련 computed 속성들
// const errorCount = computed(() => {
//   return icdStore.errorStatusBarData?.activeErrorCount || 0
// }) // 더 이상 사용하지 않음

const currentErrorMessage = computed(() => {
  const latestError = icdStore.errorStatusBarData?.latestError
  if (latestError) {
    if (latestError.isResolved) {
      // 해결된 에러의 경우 해결 메시지 표시
      return latestError.resolvedMessage || '에러가 해결되었습니다'
    } else {
      // 활성 에러의 경우 에러 메시지 표시
      return latestError.message || '알 수 없는 에러'
    }
  }
  return null
})

// ✅ severity에 따른 아이콘 결정
const getSeverityIcon = () => {
  const latestError = icdStore.errorStatusBarData?.latestError
  if (!latestError) return 'check_circle'

  const severity = latestError.severity
  switch (severity) {
    case 'ERROR':
    case 'CRITICAL':
      return 'error'
    case 'WARNING':
      return 'warning'
    case 'INFO':
      return 'info'
    default:
      return 'warning'
  }
}

// ✅ severity에 따른 색상 결정
const getSeverityColor = () => {
  const latestError = icdStore.errorStatusBarData?.latestError
  if (!latestError) return 'green'

  const severity = latestError.severity
  switch (severity) {
    case 'ERROR':
    case 'CRITICAL':
      return 'red'
    case 'WARNING':
      return 'orange'
    case 'INFO':
      return 'blue'
    default:
      return 'red'
  }
}

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
// ✅ 시스템 정보 팝업 핸들러 추가
const handleSystemInfo = () => {
  console.log('🔧 시스템 정보 버튼 클릭됨')

  void openComponent('hardware-error-log', {
    mode: 'popup', // 'popup' | 'modal' | 'auto'
    width: 1100,
    height: 900,
    onClose: () => {
      console.log('시스템 정보 창이 닫혔습니다')
    },
    onError: (error) => {
      console.error('시스템 정보 창 오류:', error)
      alert('시스템 정보 창을 열 수 없습니다.')
    },
  })
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

// 에러 로그 팝업 열기
const openErrorLogPopup = async () => {
  try {
    console.log('🔍 에러 로그 팝업 열기 요청')

    // 팝업 상태 설정
    await hardwareErrorLogStore.setPopupOpen(true)

    // 팝업 창 열기
    await openComponent('hardware-error-log', {
      mode: 'popup',
      width: 1200,
      height: 616,
      onClose: () => {
        console.log('🔍 에러 로그 팝업 닫기')
        void hardwareErrorLogStore.setPopupOpen(false)
      },
      onError: (error) => {
        console.error('❌ 에러 로그 팝업 오류:', error)
        $q.notify({
          type: 'negative',
          message: '에러 로그 팝업을 열 수 없습니다.',
          position: 'top'
        })
      }
    })
  } catch (error) {
    console.error('❌ 에러 로그 팝업 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: '에러 로그 팝업 열기에 실패했습니다.',
      position: 'top'
    })
  }
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

onBeforeUnmount(() => {
  // 정리 작업
})
</script>
<style scoped>
.custom-header {
  background-color: var(--theme-primary) !important;
  box-shadow: none !important;
  border-bottom: 1px solid var(--theme-border) !important;
}

/* 다크 테마일 때 */
.body--dark .custom-header {
  background-color: #091d24 !important;
  box-shadow: none !important;
  border-bottom: 1px solid var(--theme-border) !important;
}

/* 라이트 테마일 때 */
.body--light .custom-header {
  background-color: #1976d2 !important;
  box-shadow: none !important;
  border-bottom: 1px solid var(--theme-border) !important;
}

/* 툴바 레이아웃 */
.header-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 64px;
  /* 높이 증가 */
  box-shadow: none !important;
  border-bottom: none !important;
}

/* 좌측 섹션 */
.left-section {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 중앙 섹션 */
.center-section {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 우측 섹션 (2행) */
.right-section {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  flex-shrink: 0;
  gap: 4px;
}

/* 시간 정보 행 */
.time-row {
  display: flex;
  align-items: center;
}

/* 버튼들 행 */
.buttons-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 시간 정보 스타일 */
.time-info {
  color: white;
  font-size: 13px;
  /* 11px에서 13px로 증가 */
  font-weight: 500;
  text-align: right;
  line-height: 1.2;
  /* 줄 간격 통일 */
}

.utc-time {
  margin-bottom: 2px;
  font-size: 13px;
  /* UTC 시간 폰트 크기 명시적으로 설정 */
  font-weight: 500;
  /* 폰트 두께 통일 */
}

.local-time {
  font-size: 13px;
  /* 10px에서 13px로 증가하여 UTC와 동일하게 */
  font-weight: 500;
  /* 폰트 두께 통일 */
  opacity: 0.9;
}

/* 서버 상태 스타일 */
.server-status {
  margin-right: 12px;
  font-size: 12px;
  font-weight: 500;
}

.server-status .text-positive {
  color: #4caf50 !important;
}

.server-status .text-warning {
  color: #ff9800 !important;
}

.server-status .text-negative {
  color: #f44336 !important;
}

/* GTL 로고 스타일 */
.header-logo {
  height: 80px;
  width: auto;
  background-color: transparent;
}

.error-status-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: var(--theme-card-background);
  border-top: 1px solid var(--theme-border);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1000;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
}

.error-message {
  display: flex;
  align-items: center;
  color: var(--theme-text);
  font-size: 14px;
  flex: 1;
}

.log-button {
  margin-left: 12px;
  width: 40px !important;
  height: 40px !important;
  min-width: 40px !important;
  min-height: 40px !important;
  max-width: 40px !important;
  max-height: 40px !important;
}

.log-button .q-btn__wrapper {
  width: 40px !important;
  height: 40px !important;
  min-width: 40px !important;
  min-height: 40px !important;
  max-width: 40px !important;
  max-height: 40px !important;
}

.error-counter {
  font-size: 12px;
  opacity: 0.8;
  margin-left: 8px;
}

.error-navigation {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

/* q-page-container 패딩 제거 */
.no-padding {
  padding: 0 !important;
}
</style>
