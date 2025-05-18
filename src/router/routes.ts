import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      {
        path: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        children: [
          {
            path: '',
            redirect: '/dashboard/standby',
          },
          {
            path: 'standby',
            component: () => import('pages/mode/Standby.vue'),
          },
          {
            path: 'step',
            component: () => import('pages/mode/Step.vue'),
          },
          {
            path: 'slew',
            component: () => import('pages/mode/Slew.vue'),
          },
          {
            path: 'pedestal',
            component: () => import('pages/mode/PedestalPosition.vue'),
          },
          {
            path: 'ephemeris',
            component: () => import('pages/mode/EphemerisDesignation.vue'),
          },

          {
            path: 'suntrack',
            component: () => import('pages/mode/SunTrack.vue'),
          },
          {
            path: 'feed',
            component: () => import('pages/mode/FeedControl.vue'),
          },
        ],
      },
      { path: 'test', component: () => import('pages/DashboardPage_Test.vue') },
    ],
  },
  {
    path: '/',
    component: () => import('layouts/LoginLayout.vue'),
    children: [{ path: 'login', component: () => import('pages/LoginPage.vue') }],
  },
  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
