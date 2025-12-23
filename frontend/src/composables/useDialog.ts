import { Dialog } from 'quasar'
import type { Component } from 'vue'

export interface DialogOptions {
  title?: string
  message?: string
  ok?: {
    label?: string
    color?: string
    handler?: () => void
  }
  cancel?: {
    label?: string
    color?: string
    handler?: () => void
  }
  persistent?: boolean
  maximized?: boolean
  fullWidth?: boolean
  fullHeight?: boolean
}

export const useDialog = () => {
  // 확인 다이얼로그
  const confirm = (message: string, options: DialogOptions = {}) => {
    return new Promise<boolean>((resolve) => {
      Dialog.create({
        title: options.title || '확인',
        message,
        ok: {
          label: options.ok?.label || '확인',
          color: options.ok?.color || 'primary',
          handler: () => {
            options.ok?.handler?.()
            resolve(true)
          },
        },
        cancel: {
          label: options.cancel?.label || '취소',
          color: options.cancel?.color || 'grey',
          handler: () => {
            options.cancel?.handler?.()
            resolve(false)
          },
        },
        persistent: options.persistent || false,
      })
    })
  }

  // 경고 다이얼로그
  const alert = (message: string, options: DialogOptions = {}) => {
    return new Promise<void>((resolve) => {
      Dialog.create({
        title: options.title || '알림',
        message,
        ok: {
          label: options.ok?.label || '확인',
          color: options.ok?.color || 'primary',
          handler: () => {
            options.ok?.handler?.()
            resolve()
          },
        },
        persistent: options.persistent || false,
      })
    })
  }

  // 입력 다이얼로그
  const prompt = (
    message: string,
    options: DialogOptions & {
      defaultValue?: string
      inputType?: 'text' | 'password' | 'email' | 'number' | 'tel' | 'url'
      inputLabel?: string
    } = {},
  ) => {
    return new Promise<string | null>((resolve) => {
      Dialog.create({
        title: options.title || '입력',
        message,
        prompt: {
          model: options.defaultValue || '',
          type: options.inputType || 'text',
          label: options.inputLabel || '값을 입력하세요',
        },
        ok: {
          label: options.ok?.label || '확인',
          color: options.ok?.color || 'primary',
          handler: (value: string) => {
            options.ok?.handler?.()
            resolve(value)
          },
        },
        cancel: {
          label: options.cancel?.label || '취소',
          color: options.cancel?.color || 'grey',
          handler: () => {
            options.cancel?.handler?.()
            resolve(null)
          },
        },
        persistent: options.persistent || false,
      })
    })
  }

  // 커스텀 다이얼로그
  const custom = (component: string | Component, options: DialogOptions = {}) => {
    return new Promise<unknown>((resolve) => {
      Dialog.create({
        component,
        ...options,
        ok: {
          label: options.ok?.label || '확인',
          color: options.ok?.color || 'primary',
          handler: (value: unknown) => {
            options.ok?.handler?.()
            resolve(value)
          },
        },
        cancel: {
          label: options.cancel?.label || '취소',
          color: options.cancel?.color || 'grey',
          handler: () => {
            options.cancel?.handler?.()
            resolve(null)
          },
        },
      })
    })
  }

  return {
    confirm,
    alert,
    prompt,
    custom,
  }
}
