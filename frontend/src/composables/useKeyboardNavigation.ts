import { onMounted, onUnmounted, ref, watch, type Ref, isRef } from 'vue'

/**
 * 키보드 단축키 핸들러 타입
 */
export type KeyHandler = (event: KeyboardEvent) => void | boolean

/**
 * 키보드 바인딩 옵션
 */
export interface KeyBinding {
  /** 키 이름 (e.g., 'Escape', 'Enter', 'F5') */
  key: string
  /** Ctrl 키 조합 */
  ctrl?: boolean
  /** Shift 키 조합 */
  shift?: boolean
  /** Alt 키 조합 */
  alt?: boolean
  /** 핸들러 함수 */
  handler: KeyHandler
  /** 기본 동작 방지 여부 (기본: true) */
  preventDefault?: boolean
  /** 이벤트 전파 중단 여부 */
  stopPropagation?: boolean
}

/**
 * useKeyboardNavigation 옵션
 */
export interface UseKeyboardNavigationOptions {
  /** 활성화 여부 (ref로 동적 제어 가능) */
  enabled?: Ref<boolean> | boolean
  /** 이벤트 대상 (기본: window) */
  target?: Window | Document | HTMLElement
  /** 자동 마운트 (기본: true) */
  autoMount?: boolean
}

/**
 * 키보드 네비게이션 composable
 *
 * 모달, 다이얼로그, 페이지 등에서 키보드 단축키를 관리합니다.
 *
 * @example
 * // 기본 사용법 - ESC로 닫기
 * const { onEscape } = useKeyboardNavigation()
 * onEscape(() => {
 *   emit('close')
 * })
 *
 * @example
 * // 여러 키 바인딩
 * const { bind, onEscape, onEnter } = useKeyboardNavigation()
 * onEscape(() => closeModal())
 * onEnter(() => submitForm())
 * bind({ key: 'F5', handler: () => refresh(), preventDefault: true })
 *
 * @example
 * // 동적 활성화 제어
 * const isModalOpen = ref(false)
 * const { onEscape } = useKeyboardNavigation({ enabled: isModalOpen })
 */
export const useKeyboardNavigation = (options: UseKeyboardNavigationOptions = {}) => {
  const {
    enabled = true,
    target = window,
    autoMount = true
  } = options

  const bindings = ref<KeyBinding[]>([])
  const isEnabled = ref(isRef(enabled) ? enabled.value : enabled)

  // enabled가 ref인 경우 watch로 동기화
  if (isRef(enabled)) {
    watch(enabled, (newVal) => {
      isEnabled.value = newVal
    }, { immediate: true })
  }

  /**
   * 키보드 이벤트 핸들러
   */
  const handleKeyDown = (event: KeyboardEvent) => {
    // 비활성화 상태면 무시
    if (!isEnabled.value) return

    // 입력 필드에서는 일부 키만 허용
    const activeElement = document.activeElement
    const isInputFocused = activeElement instanceof HTMLInputElement ||
      activeElement instanceof HTMLTextAreaElement ||
      activeElement instanceof HTMLSelectElement ||
      (activeElement as HTMLElement)?.isContentEditable

    for (const binding of bindings.value) {
      // 키 매칭 확인
      if (event.key !== binding.key) continue

      // 수정자 키 확인
      if (binding.ctrl && !event.ctrlKey) continue
      if (binding.shift && !event.shiftKey) continue
      if (binding.alt && !event.altKey) continue
      if (!binding.ctrl && event.ctrlKey && binding.key !== 'Control') continue
      if (!binding.shift && event.shiftKey && binding.key !== 'Shift') continue
      if (!binding.alt && event.altKey && binding.key !== 'Alt') continue

      // 입력 필드 포커스 시 Escape와 특수 조합만 허용
      if (isInputFocused && binding.key !== 'Escape' && !binding.ctrl && !binding.alt) {
        continue
      }

      // 기본 동작 방지
      if (binding.preventDefault !== false) {
        event.preventDefault()
      }

      // 이벤트 전파 중단
      if (binding.stopPropagation) {
        event.stopPropagation()
      }

      // 핸들러 실행
      const result = binding.handler(event)

      // false 반환 시 다음 바인딩으로 계속
      if (result === false) continue

      // 처리 완료
      break
    }
  }

  /**
   * 키보드 바인딩 추가
   */
  const bind = (binding: KeyBinding) => {
    bindings.value.push(binding)
    return () => unbind(binding)
  }

  /**
   * 키보드 바인딩 제거
   */
  const unbind = (binding: KeyBinding) => {
    const index = bindings.value.indexOf(binding)
    if (index !== -1) {
      bindings.value.splice(index, 1)
    }
  }

  /**
   * 모든 바인딩 제거
   */
  const unbindAll = () => {
    bindings.value = []
  }

  /**
   * ESC 키 바인딩 헬퍼
   */
  const onEscape = (handler: KeyHandler, options: Partial<KeyBinding> = {}) => {
    return bind({
      key: 'Escape',
      handler,
      ...options
    })
  }

  /**
   * Enter 키 바인딩 헬퍼
   */
  const onEnter = (handler: KeyHandler, options: Partial<KeyBinding> = {}) => {
    return bind({
      key: 'Enter',
      handler,
      ...options
    })
  }

  /**
   * Ctrl+Enter 키 바인딩 헬퍼
   */
  const onCtrlEnter = (handler: KeyHandler, options: Partial<KeyBinding> = {}) => {
    return bind({
      key: 'Enter',
      ctrl: true,
      handler,
      ...options
    })
  }

  /**
   * F5 키 바인딩 헬퍼 (새로고침)
   */
  const onF5 = (handler: KeyHandler, options: Partial<KeyBinding> = {}) => {
    return bind({
      key: 'F5',
      handler,
      preventDefault: true,
      ...options
    })
  }

  /**
   * 활성화/비활성화 설정
   */
  const setEnabled = (value: boolean) => {
    isEnabled.value = value
  }

  /**
   * 이벤트 리스너 등록
   */
  const mount = () => {
    target.addEventListener('keydown', handleKeyDown as EventListener)
  }

  /**
   * 이벤트 리스너 해제
   */
  const unmount = () => {
    target.removeEventListener('keydown', handleKeyDown as EventListener)
  }

  // 자동 마운트
  if (autoMount) {
    onMounted(mount)
    onUnmounted(() => {
      unmount()
      unbindAll()
    })
  }

  return {
    // 바인딩 메서드
    bind,
    unbind,
    unbindAll,

    // 헬퍼 메서드
    onEscape,
    onEnter,
    onCtrlEnter,
    onF5,

    // 제어 메서드
    setEnabled,
    mount,
    unmount,

    // 상태
    isEnabled,
    bindings
  }
}
