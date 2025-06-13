<template>
  <div class="popup-container" :style="containerStyle">
    <!-- 팝업 헤더 (모달이 아닐 때만 표시) -->
    <div v-if="showHeader && !isModal" class="popup-header">
      <h1>{{ pageTitle }}</h1>
      <div class="header-actions">
        <button @click="refreshPage" class="btn-refresh" title="새로고침 (F5)">🔄</button>
        <button @click="closeWindow" class="btn-close" title="닫기 (ESC)">✕</button>
      </div>
    </div>

    <!-- 로딩 화면 -->
    <div v-if="isLoading" class="loading-screen">
      <div class="loading-content">
        <div class="spinner">⏳</div>
        <h3>인증 상태 확인 중...</h3>
        <p>{{ loadingMessage }}</p>
      </div>
    </div>

    <!-- 메인 컨텐츠 (인증된 경우에만 표시) -->
    <div v-else-if="isAuthenticated" class="popup-content" :class="{ 'modal-content': isModal }">
      <router-view />
    </div>

    <!-- 리다이렉트 중 표시 (인증되지 않은 경우) -->
    <div v-else class="redirect-screen">
      <div class="redirect-content">
        <div class="spinner">🔄</div>
        <h3>로그인 페이지로 이동 중...</h3>
        <p>인증이 필요합니다. 잠시만 기다려주세요.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useQuasar } from 'quasar'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const $q = useQuasar()
const showHeader = ref(true)

// 상태 관리
const isLoading = ref(true)
const isAuthenticated = ref(false)
const loadingMessage = ref('초기화 중...')

// 다크 모드에 따른 동적 스타일
const containerStyle = computed(() => ({
  background: $q.dark.isActive ? '#121212' : '#fafafa',
}))

// 모달인지 확인 (부모 창이 같은 origin인지 체크)
const isModal = computed(() => {
  try {
    // 모달인 경우 window.opener가 없거나 같은 origin
    return !window.opener || window.opener === window.parent
  } catch {
    // 크로스 오리진 에러가 발생하면 팝업
    return false
  }
})

const pageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    'all-status': '📊 All Status Information',
    'system-info': '🖥️ System Information',
    'error-log': '📋 Error Log',
  }

  const componentName = route.params.component as string
  return titleMap[componentName] || 'Popup Window'
})

// 창 닫기
const closeWindow = () => {
  if (window.opener && !window.opener.closed) {
    try {
      window.opener.postMessage({ type: 'popup-closing' }, window.location.origin)
    } catch (error) {
      console.warn('부모 창 통신 실패:', error)
    }
  }
  window.close()
}

// 페이지 새로고침
const refreshPage = () => {
  window.location.reload()
}

// 로그인 페이지로 리다이렉트
const redirectToLogin = () => {
  console.log('🔄 로그인 페이지로 리다이렉트')

  // 현재 페이지 정보를 쿼리 파라미터로 저장 (로그인 후 돌아올 수 있도록)
  const returnUrl = encodeURIComponent(route.fullPath)

  // 로그인 페이지로 이동
  void router.push({
    path: '/login',
    query: {
      returnUrl: returnUrl,
      popup: isModal.value ? 'modal' : 'popup', // 팝업/모달 정보도 전달
    },
  })
}

// 초기 인증 확인
const checkAuth = () => {
  console.log('🔍 인증 상태 확인 시작')

  // localStorage 확인
  const localAuth = localStorage.getItem('auth-status') === 'logged-in'

  // 스토어 상태 확인 및 복원
  const storeAuth = authStore.restoreAuthState()

  // 최종 인증 상태 결정
  const finalAuth = localAuth || storeAuth || authStore.isLoggedIn

  console.log('📊 인증 확인 결과:', {
    localStorage: localAuth,
    store: storeAuth,
    final: finalAuth,
  })

  isAuthenticated.value = finalAuth
  isLoading.value = false

  if (finalAuth) {
    loadingMessage.value = '인증 완료!'
    console.log('✅ 인증됨 - 컨텐츠 표시')
  } else {
    loadingMessage.value = '로그인이 필요합니다'
    console.log('❌ 인증되지 않음 - 로그인 페이지로 리다이렉트')

    // 1초 후 리다이렉트 (사용자가 메시지를 볼 수 있도록)
    setTimeout(() => {
      redirectToLogin()
    }, 1000)
  }
}

onMounted(() => {
  console.log('🪟 PopupRouter 마운트됨:', route.params.component)
  console.log('🎭 모달 모드:', isModal.value)

  // 인증 확인 (약간의 지연 후)
  setTimeout(() => {
    checkAuth()
  }, 300)

  // 키보드 단축키
  const handleKeydown = (event: KeyboardEvent) => {
    switch (event.key) {
      case 'Escape':
        event.preventDefault()
        closeWindow()
        break
      case 'F5':
        event.preventDefault()
        refreshPage()
        break
    }
  }

  window.addEventListener('keydown', handleKeydown)

  // 정리
  return () => {
    window.removeEventListener('keydown', handleKeydown)
  }
})
</script>

<style scoped>
.popup-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  /* background는 동적 스타일로 처리 */
  color: white;
  font-family: Arial, sans-serif;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: #2196f3;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.popup-header h1 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-refresh,
.btn-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-refresh:hover,
.btn-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.loading-screen,
.redirect-screen {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.loading-content,
.redirect-content {
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  padding: 2rem;
  border-radius: 12px;
  min-width: 400px;
}

.spinner {
  font-size: 3rem;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.popup-content {
  flex: 1;
  overflow: hidden;
}

/* 모달일 때 컨텐츠 스타일 */
.modal-content {
  overflow: auto; /* 모달에서는 스크롤 허용 */
  padding: 0; /* 모달에서는 패딩 제거 */
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .loading-content,
  .redirect-content {
    min-width: 90%;
    margin: 0 5%;
    padding: 1.5rem;
  }

  .popup-header {
    padding: 1rem;
  }

  .popup-header h1 {
    font-size: 1.2rem;
  }
}
</style>
