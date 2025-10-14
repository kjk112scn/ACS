import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // 팝업 전용 라우트들
  {
    path: '/popup',
    children: [
      {
        path: 'all-status',
        component: () => import('../components/content/AllStatusContent.vue'),
        name: 'PopupAllStatus',
      },
      {
        path: 'system-info',
        name: 'PopupSystemInfo',
        component: () => import('../components/content/SystemInfoContent.vue'),
      },
      {
        path: 'axis-transform-calculator',
        name: 'PopupAxisTransformCalculator',
        component: () => import('../components/content/AxisTransformCalculator.vue'),
      },
      {
        path: 'hardware-error-log',
        name: 'hardware-error-log',
        component: () => import('@/components/HardwareErrorLogPanel.vue'),
        meta: {
          isPopup: true,
        },
      },
    ],
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'dashboard',
        component: () => import('@/pages/DashboardPage.vue'),
        children: [
          {
            path: '',
            redirect: '/dashboard/standby',
          },
          {
            path: 'standby',
            component: () => import('@/pages/mode/StandbyPage.vue'),
          },
          {
            path: 'step',
            component: () => import('@/pages/mode/StepPage.vue'),
          },
          {
            path: 'slew',
            component: () => import('@/pages/mode/SlewPage.vue'),
          },
          {
            path: 'pedestal',
            component: () => import('@/pages/mode/PedestalPositionPage.vue'),
          },
          {
            path: 'ephemeris',
            component: () => import('@/pages/mode/EphemerisDesignationPage.vue'),
          },
          {
            path: 'pass-schedule',
            component: () => import('@/pages/mode/PassSchedulePage.vue'),
          },
          {
            path: 'suntrack',
            component: () => import('@/pages/mode/SunTrackPage.vue'),
          },
          {
            path: 'feed',
            component: () => import('@/pages/mode/FeedPage.vue'),
          },
        ],
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/LoginLayout.vue'),
    children: [{ path: 'login', component: () => import('@/pages/LoginPage.vue') }],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue'),
  },
]

export default routes
