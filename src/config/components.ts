import type { Component } from 'vue'

export interface ComponentConfig {
  name: string
  title: string
  icon: string
  defaultWidth: number
  defaultHeight: number
  component: () => Promise<{ default: Component } | Component>
  description?: string
}

export const POPUP_COMPONENTS: Record<string, ComponentConfig> = {
  'all-status': {
    name: 'all-status',
    title: 'All Status Information',
    icon: '📊',
    defaultWidth: 1700,
    defaultHeight: 700,
    component: () => import('../components/content/AllStatusContent.vue'),
    description: '전체 시스템 상태 정보',
  },
  'system-info': {
    name: 'system-info',
    title: 'System Information',
    icon: '🖥️',
    defaultWidth: 1400,
    defaultHeight: 800,
    component: () => import('../components/content/SystemInfoContent.vue'),
    description: '시스템 정보 및 성능',
  },
  'tle-upload': {
    name: 'tle-upload',
    title: 'TLE Upload',
    icon: '📡',
    description: 'TLE 데이터 업로드',
    defaultWidth: 800,
    defaultHeight: 600,
    component: () => import('../components/content/TLEUploadContent.vue'),
  },
  'select-schedule': {
    name: 'select-schedule',
    title: 'Select Schedule',
    icon: '📡',
    description: 'Select Schedule',
    defaultWidth: 800,
    defaultHeight: 600,
    component: () => import('../components/content/SelectScheduleContent.vue'),
  },
}

export type ComponentName = keyof typeof POPUP_COMPONENTS
