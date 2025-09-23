import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: false,
  }),
  actions: {
    login() {
      this.isLoggedIn = true
      // localStorage에 인증 상태 저장 (타임스탬프는 마지막 활동 시간으로 사용)
      localStorage.setItem('auth-status', 'logged-in')
      localStorage.setItem('auth-last-activity', Date.now().toString())
      console.log('✅ 로그인 상태 저장됨:', localStorage.getItem('auth-status'))
    },
    logout() {
      this.isLoggedIn = false
      // localStorage에서 인증 상태 제거
      localStorage.removeItem('auth-status')
      localStorage.removeItem('auth-last-activity')
      console.log('❌ 로그아웃 상태 저장됨')
    },

    // 사용자 활동 업데이트 (선택적으로 사용)
    updateActivity() {
      if (this.isLoggedIn) {
        localStorage.setItem('auth-last-activity', Date.now().toString())
      }
    },

    // 인증 상태 복원 (24시간 체크 제거)
    restoreAuthState() {
      const authStatus = localStorage.getItem('auth-status')
      const lastActivity = localStorage.getItem('auth-last-activity')

      console.log('🔍 인증 상태 복원 시도:', { authStatus, lastActivity })

      if (authStatus === 'logged-in') {
        this.isLoggedIn = true
        // 마지막 활동 시간 업데이트
        if (lastActivity) {
          localStorage.setItem('auth-last-activity', Date.now().toString())
        }
        console.log('✅ 인증 상태 복원 성공 (무제한)')
        return true
      }

      console.log('❌ 인증 상태 없음')
      return false
    },

    // 인증 상태 확인
    checkAuthStatus() {
      const status = localStorage.getItem('auth-status') === 'logged-in'
      console.log('🔍 인증 상태 확인:', status)
      return status
    },

    // 수동 로그아웃 (관리자가 필요시 사용)
    forceLogout() {
      console.log('🔒 강제 로그아웃 실행')
      this.logout()
    }
  },
})
