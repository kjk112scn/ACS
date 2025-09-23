import { route } from 'quasar/wrappers'
import {
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory,
} from 'vue-router'

import routes from './routes'
import { useAuthStore } from '../stores/common/auth'

export default route(function (/* { store, ssrContext } */) {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : process.env.VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  })

  // 라우터 가드 수정 - 팝업 경로 우선 처리
  Router.beforeEach((to, from, next) => {
    console.log('🛣️ 라우터 가드 실행:', {
      to_path: to.path,
      to_name: to.name,
      from_path: from.path,
      requiresAuth: to.meta.requiresAuth,
      isPopup: to.meta.isPopup,
      fullPath: to.fullPath,
    })

    // 팝업 경로는 무조건 통과 (인증 체크 안함)
    if (to.path.startsWith('/popup/') || to.meta.isPopup) {
      console.log('🪟 팝업 경로 감지 - 인증 체크 건너뛰기')
      next()
      return
    }

    // 루트 경로 처리 - 팝업이 아닌 경우만 로그인으로 리다이렉트
    if (to.path === '/') {
      console.log('🏠 루트 경로 - 로그인으로 리다이렉트')
      next('/login')
      return
    }

    // 인증이 필요한 페이지 체크
    if (to.meta.requiresAuth) {
      const authStore = useAuthStore()
      const isAuthenticated = authStore.restoreAuthState()

      console.log('🔐 인증 체크:', {
        isAuthenticated,
        storeLoggedIn: authStore.isLoggedIn,
        targetPath: to.path,
      })

      if (!isAuthenticated) {
        console.log('❌ 인증 실패 - 로그인 페이지로 리다이렉트')
        next('/login')
        return
      }
    }

    console.log('✅ 라우터 가드 통과')
    next()
  })

  return Router
})
