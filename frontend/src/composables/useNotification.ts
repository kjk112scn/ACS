import { Notify } from 'quasar'

export interface NotificationOptions {
  type?: 'positive' | 'negative' | 'warning' | 'info'
  position?:
    | 'top-left'
    | 'top-right'
    | 'bottom-left'
    | 'bottom-right'
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'center'
  timeout?: number
  icon?: string
  caption?: string
  actions?: Array<{
    label: string
    color?: string
    handler?: () => void
  }>
}

export const useNotification = () => {
  // 성공 알림
  const success = (message: string, options: NotificationOptions = {}) => {
    return Notify.create({
      type: 'positive',
      icon: 'check_circle',
      message,
      position: 'top-right',
      timeout: 3000,
      ...options,
    })
  }

  // 에러 알림
  const error = (message: string, options: NotificationOptions = {}) => {
    return Notify.create({
      type: 'negative',
      icon: 'error',
      message,
      position: 'top-right',
      timeout: 5000,
      ...options,
    })
  }

  // 경고 알림
  const warning = (message: string, options: NotificationOptions = {}) => {
    return Notify.create({
      type: 'warning',
      icon: 'warning',
      message,
      position: 'top-right',
      timeout: 4000,
      ...options,
    })
  }

  // 정보 알림
  const info = (message: string, options: NotificationOptions = {}) => {
    return Notify.create({
      type: 'info',
      icon: 'info',
      message,
      position: 'top-right',
      timeout: 3000,
      ...options,
    })
  }

  // 커스텀 알림
  const custom = (message: string, options: NotificationOptions = {}) => {
    return Notify.create({
      message,
      position: 'top-right',
      timeout: 3000,
      ...options,
    })
  }

  // 모든 알림 닫기
  const dismissAll = () => {
    Notify.create({
      type: 'info',
      message: '모든 알림을 닫습니다.',
      timeout: 0,
      actions: [
        {
          label: '닫기',
          color: 'white',
          handler: () => {
            // 모든 알림 닫기
            document.querySelectorAll('.q-notification').forEach((el) => {
              el.remove()
            })
          },
        },
      ],
    })
  }

  return {
    success,
    error,
    warning,
    info,
    custom,
    dismissAll,
  }
}


