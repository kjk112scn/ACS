import { createApp, type App, type Component } from 'vue'
import { Quasar } from 'quasar' // ✅ Quasar import 추가
import { POPUP_COMPONENTS, type ComponentName, type ComponentConfig } from '../config/components'

// 기존 인터페이스들...
export interface PopupOptions {
  width?: number
  height?: number
  centered?: boolean
  resizable?: boolean
  scrollbars?: boolean
  menubar?: boolean
  toolbar?: boolean
  location?: boolean
  status?: boolean
  relativeTo?: 'screen' | 'window'
}

export interface DisplayOptions extends PopupOptions {
  mode?: 'popup' | 'modal' | 'auto'
  modalContainer?: string
  modalClass?: string
  onClose?: () => void
  onError?: (error: Error) => void
  props?: Record<string, unknown>
  title?: string
}

// 기존 함수들 (getScreenBounds, openCenteredPopup, openSmartCenteredPopup)...
// 🛡️ 확장된 Screen 인터페이스 정의
interface ExtendedScreen extends Screen {
  availLeft?: number
  availTop?: number
}
// 🔧 타입 안전한 화면 정보 가져오기
const getScreenBounds = () => {
  const screen = window.screen as ExtendedScreen

  return {
    left: screen.availLeft ?? 0,
    top: screen.availTop ?? 0,
    width: screen.availWidth ?? 1920,
    height: screen.availHeight ?? 1080,
  }
}
// 🎯 전역 모달 관리자 클래스 (더 안전한 버전)
class ModalManager {
  private static instance: ModalManager
  private activeModals: Map<string, () => void> = new Map()

  static getInstance(): ModalManager {
    if (!ModalManager.instance) {
      ModalManager.instance = new ModalManager()
    }
    return ModalManager.instance
  }

  // 모달 등록 (디버깅 강화)
  registerModal(id: string, closeFunction: () => void) {
    this.activeModals.set(id, closeFunction)
    console.log(`📝 모달 등록: ${id}`)
    console.log(`📊 현재 활성 모달 수: ${this.activeModals.size}`)
    console.log(`📋 활성 모달 목록:`, Array.from(this.activeModals.keys()))
  }

  // 모달 닫기 (디버깅 강화)
  closeModal(id?: string): boolean {
    console.log(`🔍 closeModal 호출됨 - ID: ${id || '최근 모달'}`)
    console.log(`📊 현재 활성 모달 수: ${this.activeModals.size}`)
    console.log(`📋 활성 모달 목록:`, Array.from(this.activeModals.keys()))

    if (id) {
      // 특정 모달 닫기
      const closeFunction = this.activeModals.get(id)
      if (closeFunction) {
        try {
          console.log(`🎯 특정 모달 닫기 실행: ${id}`)
          closeFunction()
          this.activeModals.delete(id)

          console.log(`✅ 모달 닫기 성공: ${id}`)
          return true
        } catch (error) {
          console.error(`❌ 모달 닫기 실패: ${id}`, error)

          this.activeModals.delete(id)
          return false
        }
      } else {
        console.warn(`⚠️ 지정된 모달을 찾을 수 없음: ${id}`)
      }
    } else {
      // 가장 최근 모달 닫기
      const entries = Array.from(this.activeModals.entries())

      if (entries.length === 0) {
        console.warn('⚠️ 닫을 모달이 없습니다')
        return false
      }

      const lastIndex = entries.length - 1
      const lastEntry = entries[lastIndex]

      if (!lastEntry || lastEntry.length !== 2) {
        console.error('❌ 잘못된 모달 엔트리 형식')
        return false
      }

      const [lastId, closeFunction] = lastEntry

      try {
        console.log(`🎯 최근 모달 닫기 실행: ${lastId}`)
        closeFunction()
        this.activeModals.delete(lastId)

        console.log(`✅ 최근 모달 닫기 성공: ${lastId}`)
        return true
      } catch (error) {
        console.error(`❌ 최근 모달 닫기 실패: ${lastId}`, error)

        this.activeModals.delete(lastId)
        return false
      }
    }

    return false
  }

  // 모든 모달 닫기
  closeAllModals(): number {
    let closedCount = 0

    this.activeModals.forEach((closeFunction, id) => {
      try {
        closeFunction()
        console.log(`🚪 모달 닫기: ${id}`)
        closedCount++
      } catch (error) {
        console.error(`❌ 모달 닫기 실패: ${id}`, error)
      }
    })

    this.activeModals.clear()
    console.log(`🚪 총 ${closedCount}개 모달 닫기 완료`)

    return closedCount
  }

  // 모달 해제 (디버깅 강화)
  unregisterModal(id: string): boolean {
    const existed = this.activeModals.has(id)
    this.activeModals.delete(id)

    console.log(`🗑️ 모달 해제: ${id} (존재했음: ${existed})`)
    console.log(`📊 해제 후 활성 모달 수: ${this.activeModals.size}`)

    return existed
  }

  // 활성 모달 수 확인
  getActiveModalCount(): number {
    return this.activeModals.size
  }

  // 모든 활성 모달 ID 목록
  getActiveModalIds(): string[] {
    return Array.from(this.activeModals.keys())
  }

  // 특정 모달 존재 확인
  hasModal(id: string): boolean {
    return this.activeModals.has(id)
  }

  // 디버깅용 - 모든 모달 정보 출력
  debugPrintModals(): void {
    console.log('🔍 현재 활성 모달 목록:')
    if (this.activeModals.size === 0) {
      console.log('  - 활성 모달 없음')
    } else {
      this.activeModals.forEach((_, id) => {
        console.log(`  - ${id}`)
      })
    }
  }
}
export { ModalManager }
export const openCenteredPopup = (
  url: string,
  name: string,
  options: PopupOptions = {},
): Window | null => {
  const {
    width = 1400,
    height = 900,
    centered = true,
    resizable = true,
    scrollbars = true,
    menubar = false,
    toolbar = false,
    location = false,
    status = false,
    relativeTo = 'window',
  } = options

  let left = 0
  let top = 0

  if (centered) {
    if (relativeTo === 'window') {
      // 현재 창 정보
      const currentWindow = {
        left: window.screenX || window.screenLeft || 0,
        top: window.screenY || window.screenTop || 0,
        width: window.outerWidth,
        height: window.outerHeight,
      }

      // 현재 창 기준 중앙 위치 계산
      left = currentWindow.left + Math.round((currentWindow.width - width) / 2)
      top = currentWindow.top + Math.round((currentWindow.height - height) / 2)

      // 화면 경계 정보 가져오기
      const screenBounds = getScreenBounds()

      // 팝업이 화면 밖으로 나가지 않도록 조정
      left = Math.max(
        screenBounds.left,
        Math.min(left, screenBounds.left + screenBounds.width - width),
      )
      top = Math.max(
        screenBounds.top,
        Math.min(top, screenBounds.top + screenBounds.height - height),
      )

      console.log('🖥️ 듀얼 모니터 고려 중앙 배치:', {
        currentWindow,
        screenBounds,
        final: { left, top },
      })
    } else {
      // 전체 화면 기준 중앙 배치
      const screenBounds = getScreenBounds()
      left = screenBounds.left + Math.round((screenBounds.width - width) / 2)
      top = screenBounds.top + Math.round((screenBounds.height - height) / 2)

      console.log('📺 전체 화면 기준 중앙 배치:', {
        screenBounds,
        position: { left, top },
      })
    }
  }

  const features = [
    `width=${width}`,
    `height=${height}`,
    `left=${left}`,
    `top=${top}`,
    `scrollbars=${scrollbars ? 'yes' : 'no'}`,
    `resizable=${resizable ? 'yes' : 'no'}`,
    `menubar=${menubar ? 'yes' : 'no'}`,
    `toolbar=${toolbar ? 'yes' : 'no'}`,
    `location=${location ? 'yes' : 'no'}`,
    `status=${status ? 'yes' : 'no'}`,
  ].join(',')

  console.log('🪟 팝업 열기:', { url, name, features })

  try {
    const popup = window.open(url, name, features)

    if (popup) {
      popup.focus()
      console.log('✅ 팝업 창 열기 성공')
    } else {
      console.error('❌ 팝업 창 열기 실패')
    }

    return popup
  } catch (error) {
    console.error('❌ 팝업 열기 중 오류:', error)
    return null
  }
}

// 🎯 스마트 중앙 배치 (자동 감지)
export const openSmartCenteredPopup = (
  url: string,
  name: string,
  options: Omit<PopupOptions, 'relativeTo'> = {},
): Window | null => {
  // 멀티 모니터 환경 감지
  const currentX = window.screenX || window.screenLeft || 0
  const isDualMonitor = Math.abs(currentX) > 100 || window.screen.availWidth > 1920

  console.log('🔍 모니터 환경 감지:', {
    currentX,
    screenWidth: window.screen.availWidth,

    isDualMonitor,
  })

  return openCenteredPopup(url, name, {
    ...options,

    relativeTo: isDualMonitor ? 'window' : 'screen',
  })
}

// 🖥️ 모니터 정보 확인 함수
export const getMonitorInfo = () => {
  const screenBounds = getScreenBounds()

  const info = {
    screen: {
      total: {
        width: window.screen.width,
        height: window.screen.height,
      },
      available: {
        width: window.screen.availWidth,
        height: window.screen.availHeight,

        left: screenBounds.left,
        top: screenBounds.top,
      },
    },

    window: {
      outer: {
        width: window.outerWidth,
        height: window.outerHeight,
      },
      position: {
        x: window.screenX || window.screenLeft || 0,
        y: window.screenY || window.screenTop || 0,
      },
    },

    estimated: {
      isDualMonitor:
        window.screen.availWidth > 1920 || Math.abs(window.screenX || window.screenLeft || 0) > 100,
      currentMonitor:
        (window.screenX || window.screenLeft || 0) > window.screen.availWidth / 2
          ? 'right'
          : 'left',
    },
  }

  console.log('🖥️ 모니터 정보:', info)
  return info
}

// 🆕 컴포넌트 설정 가져오기
export const getComponentConfig = (componentName: ComponentName): ComponentConfig => {
  const config = POPUP_COMPONENTS[componentName]
  if (!config) {
    throw new Error(`알 수 없는 컴포넌트: ${componentName}`)
  }
  return config
}

// 🆕 Vue 모달 생성 (개선된 버전) - 투명 배경 버전
export const createVueModal = async (
  componentName: ComponentName,
  options: DisplayOptions = {},
): Promise<{ element: HTMLElement; app: App; close: () => void }> => {
  const config = getComponentConfig(componentName)
  const {
    width = config.defaultWidth,
    height = config.defaultHeight,
    modalContainer = 'body',
    modalClass = 'vue-modal',
    onClose,
    onError,
    props = {},
    title = config.title,
  } = options

  // 🎯 고유 모달 ID 생성
  const modalId = `${componentName}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  console.log(`🎭 모달 ID 생성: ${modalId}`)

  try {
    // 컴포넌트 로딩
    console.log(`📦 컴포넌트 로딩 시작: ${componentName}`)
    const componentModule = await config.component()
    const component: Component =
      'default' in componentModule ? componentModule.default : componentModule
    console.log(`✅ 컴포넌트 로딩 완료: ${componentName}`)

    // 🎨 투명 모달 오버레이 생성 (배경 투명)
    const overlay = document.createElement('div')
    overlay.className = `${modalClass}-overlay`
    overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;

      background: transparent;
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 2000;

      animation: modalFadeIn 0.3s ease-out;
      pointer-events: none;
    `

    // 🎨 투명 모달 컨테이너 생성 (헤더 제거, 투명 배경)
    const modal = document.createElement('div')
    modal.className = `${modalClass}-container`
    modal.style.cssText = `
      width: ${Math.min(width, window.innerWidth * 0.95)}px;
      height: ${Math.min(height, window.innerHeight * 0.95)}px;
      max-width: 95vw;
      max-height: 95vh;





      background: transparent;
      border-radius: 0;
      box-shadow: none;
      overflow: hidden;
      position: relative;
      animation: modalSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
      display: flex;
      flex-direction: column;
      z-index: 2001;
      pointer-events: auto;
    `

    // Vue 컴포넌트를 마운트할 컨테이너 (전체 영역 사용)
    const componentContainer = document.createElement('div')
    componentContainer.style.cssText = `
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      background: transparent;
    `

    // 🎯 모달 닫기 함수 정의
    const closeModal = () => {
      console.log(`🚪 closeModal 함수 실행됨: ${modalId}`)

      overlay.style.animation = 'modalFadeOut 0.3s ease-in'
      modal.style.animation = 'modalSlideOut 0.3s ease-in'

      setTimeout(() => {
        try {
          const container = document.querySelector(modalContainer)
          if (container && overlay.parentNode === container) {
            container.removeChild(overlay)
            console.log(`🗑️ DOM 요소 제거 완료: ${modalId}`)
          }

          app.unmount()
          console.log(`🔌 Vue 앱 언마운트 완료: ${modalId}`)

          // 🎯 모달 매니저에서 해제
          const unregistered = ModalManager.getInstance().unregisterModal(modalId)
          console.log(`📝 모달 매니저 해제 결과: ${unregistered}`)

          onClose?.()
        } catch (error) {
          console.error(`❌ 모달 정리 중 오류: ${modalId}`, error)
        }
      }, 300)
    }

    // ✅ 🎯 모달 매니저에 등록
    console.log(`📝 모달 매니저에 등록 시도: ${modalId}`)

    ModalManager.getInstance().registerModal(modalId, closeModal)
    console.log(`✅ 모달 매니저 등록 완료: ${modalId}`)

    // Vue 앱 생성
    const app = createApp(component, {
      ...props,
      modalId: modalId,
      modalTitle: title,
    })

    // Quasar 플러그인 추가
    app.use(Quasar, {
      plugins: {},
    })

    // 전역 속성으로 닫기 함수 제공
    app.config.globalProperties.$closeModal = closeModal
    app.config.globalProperties.$modalId = modalId

    // 🎨 이벤트 리스너들 (오버레이 클릭 비활성화)
    // ESC 키로만 닫기 가능
    const handleKeydown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        console.log(`⌨️ ESC 키로 닫기: ${modalId}`)
        closeModal()
        document.removeEventListener('keydown', handleKeydown)
      }
    }
    document.addEventListener('keydown', handleKeydown)

    // DOM 구성 (헤더 없이 컨텐츠만)
    modal.appendChild(componentContainer)
    overlay.appendChild(modal)

    // 컨테이너에 추가
    const container = document.querySelector(modalContainer)
    if (!container) {
      throw new Error(`모달 컨테이너를 찾을 수 없습니다: ${modalContainer}`)
    }
    container.appendChild(overlay)

    // 🎨 CSS 애니메이션 추가 (투명 배경용)
    if (!document.querySelector('#transparent-modal-animations')) {
      const style = document.createElement('style')

      style.id = 'transparent-modal-animations'
      style.textContent = `
        @keyframes modalFadeIn {


          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes modalFadeOut {


          from { opacity: 1; }
          to { opacity: 0; }
        }
        @keyframes modalSlideIn {

          from { opacity: 0; transform: scale(0.95) translateY(-20px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        @keyframes modalSlideOut {
          from { opacity: 1; transform: scale(1) translateY(0); }

          to { opacity: 0; transform: scale(0.95) translateY(-20px); }
        }
      `
      document.head.appendChild(style)
    }

    // Vue 컴포넌트 마운트
    console.log(`🔧 Vue 컴포넌트 마운트 시작: ${modalId}`)
    app.mount(componentContainer)
    console.log(`✅ Vue 컴포넌트 마운트 완료: ${modalId}`)

    // 최종 확인
    const finalActiveCount = ModalManager.getInstance().getActiveModalCount()

    console.log(`🎭 투명 모달 생성 완료: ${componentName} (ID: ${modalId})`)
    console.log(`📊 최종 활성 모달 수: ${finalActiveCount}`)

    return { element: overlay, app, close: closeModal }
  } catch (error) {
    console.error(`❌ 모달 생성 실패: ${componentName}`, error)
    onError?.(error as Error)
    throw error
  }
}

// 🆕 팝업 열기
export const openPopup = (
  componentName: ComponentName,
  options: DisplayOptions = {},
): Window | null => {
  const config = getComponentConfig(componentName)
  const baseUrl = window.location.origin
  const popupUrl = `${baseUrl}/#/popup/${componentName}`

  return openSmartCenteredPopup(popupUrl, `${componentName}Popup`, {
    width: config.defaultWidth,
    height: config.defaultHeight,
    ...options,
  })
}

// 🆕 모달 열기
export const openModal = async (
  componentName: ComponentName,
  options: DisplayOptions = {},
): Promise<{ element: HTMLElement; app: App; close: () => void } | null> => {
  try {
    return await createVueModal(componentName, options)
  } catch (error) {
    console.error(`모달 열기 실패: ${componentName}`, error)
    options.onError?.(error as Error)
    return null
  }
}

// 🆕 자동 모드 (팝업 시도 후 실패시 모달)
export const openComponent = async (
  componentName: ComponentName,
  options: DisplayOptions = {},
): Promise<Window | { element: HTMLElement; app: App; close: () => void } | null> => {
  const { mode = 'auto' } = options

  if (mode === 'popup') {
    return openPopup(componentName, options)
  }

  if (mode === 'modal') {
    return await openModal(componentName, options)
  }

  // auto 모드: 팝업 시도 후 실패시 모달
  const popup = openPopup(componentName, options)
  if (!popup) {
    console.log('🚫 팝업이 차단됨, 모달로 전환')
    return await openModal(componentName, options)
  }
  return popup
}

// 🆕 편의 함수들
export const openAllStatus = (options: DisplayOptions = {}) => openComponent('all-status', options)

export const openSystemInfo = (options: DisplayOptions = {}) =>
  openComponent('system-info', options)

export const openErrorLog = (options: DisplayOptions = {}) => openComponent('error-log', options)

export const openNetworkStatus = (options: DisplayOptions = {}) =>
  openComponent('network-status', options)

export const openTrackingInfo = (options: DisplayOptions = {}) =>
  openComponent('tracking-info', options)

export const openCommandHistory = (options: DisplayOptions = {}) =>
  openComponent('command-history', options)
// TLE Upload 편의 함수 추가
export const openTLEUpload = (options: DisplayOptions = {}) => openComponent('tle-upload', options)
// 🆕 모든 컴포넌트 목록 가져오기
export const getAvailableComponents = () => {
  return Object.values(POPUP_COMPONENTS)
}

// 🆕 선택 다이얼로그 (여러 컴포넌트 중 선택) - 계속
export const showComponentSelector = (): Promise<ComponentName | null> => {
  return new Promise((resolve) => {
    const dialog = document.createElement('div')
    dialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0, 0, 0, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 10000;
      backdrop-filter: blur(4px);
    `

    const selector = document.createElement('div')
    selector.style.cssText = `
      background: #2a2a2a;
      border-radius: 12px;
      padding: 2rem;
      max-width: 600px;
      width: 90%;
      max-height: 80vh;
      overflow-y: auto;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
    `

    const title = document.createElement('h2')
    title.textContent = '📋 컴포넌트 선택'
    title.style.cssText = `
      color: white;
      margin: 0 0 1.5rem 0;
      text-align: center;
      font-size: 1.5rem;
    `

    const grid = document.createElement('div')
    grid.style.cssText = `
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    `

    // 컴포넌트 버튼들 생성
    Object.values(POPUP_COMPONENTS).forEach((config) => {
      const button = document.createElement('button')
      button.style.cssText = `
        background: #3a3a3a;
        border: 2px solid #555;
        border-radius: 8px;
        padding: 1rem;
        color: white;
        cursor: pointer;
        transition: all 0.2s;
        text-align: left;
      `

      button.innerHTML = `
        <div style="font-size: 2rem; margin-bottom: 0.5rem;">${config.icon}</div>
        <div style="font-weight: bold; margin-bottom: 0.25rem;">${config.title}</div>
        <div style="font-size: 0.9rem; opacity: 0.7;">${config.description || ''}</div>
      `

      button.addEventListener('mouseenter', () => {
        button.style.background = '#4a4a4a'
        button.style.borderColor = '#2196f3'
        button.style.transform = 'translateY(-2px)'
      })

      button.addEventListener('mouseleave', () => {
        button.style.background = '#3a3a3a'
        button.style.borderColor = '#555'
        button.style.transform = 'translateY(0)'
      })

      button.addEventListener('click', () => {
        document.body.removeChild(dialog)
        resolve(config.name)
      })

      grid.appendChild(button)
    })

    const cancelButton = document.createElement('button')
    cancelButton.textContent = '취소'
    cancelButton.style.cssText = `
      background: #666;
      border: none;
      border-radius: 6px;
      padding: 0.75rem 2rem;
      color: white;
      cursor: pointer;
      font-size: 1rem;
      display: block;
      margin: 0 auto;
      transition: all 0.2s;
    `

    cancelButton.addEventListener('mouseenter', () => {
      cancelButton.style.background = '#777'
    })

    cancelButton.addEventListener('mouseleave', () => {
      cancelButton.style.background = '#666'
    })

    cancelButton.addEventListener('click', () => {
      document.body.removeChild(dialog)
      resolve(null)
    })

    // ESC 키로 취소
    const handleKeydown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        document.body.removeChild(dialog)
        document.removeEventListener('keydown', handleKeydown)
        resolve(null)
      }
    }
    document.addEventListener('keydown', handleKeydown)

    // 오버레이 클릭으로 취소
    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) {
        document.body.removeChild(dialog)
        resolve(null)
      }
    })

    selector.appendChild(title)
    selector.appendChild(grid)
    selector.appendChild(cancelButton)
    dialog.appendChild(selector)
    document.body.appendChild(dialog)
  })
}

// 🆕 표시 모드 선택 다이얼로그
export const showDisplayModeDialog = (
  componentName: ComponentName,
): Promise<'popup' | 'modal' | null> => {
  return new Promise((resolve) => {
    const config = getComponentConfig(componentName)

    const dialog = document.createElement('div')
    dialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0, 0, 0, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 10000;
      backdrop-filter: blur(4px);
    `

    const selector = document.createElement('div')
    selector.style.cssText = `
      background: #2a2a2a;
      border-radius: 12px;
      padding: 2rem;
      width: 400px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
    `

    const title = document.createElement('h2')
    title.innerHTML = `${config.icon} ${config.title}`
    title.style.cssText = `
      color: white;
      margin: 0 0 1rem 0;
      text-align: center;
      font-size: 1.3rem;
    `

    const subtitle = document.createElement('p')
    subtitle.textContent = '표시 방식을 선택하세요:'
    subtitle.style.cssText = `
      color: #ccc;
      margin: 0 0 1.5rem 0;
      text-align: center;
    `

    const buttonContainer = document.createElement('div')
    buttonContainer.style.cssText = `
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-bottom: 1rem;
    `

    // 팝업 버튼
    const popupButton = document.createElement('button')
    popupButton.innerHTML = `
      <div style="display: flex; align-items: center; gap: 1rem;">
        <span style="font-size: 1.5rem;">🪟</span>
        <div style="text-align: left;">
          <div style="font-weight: bold;">새 창으로 열기</div>
          <div style="font-size: 0.9rem; opacity: 0.7;">별도의 브라우저 창에서 열림</div>
        </div>
      </div>
    `
    popupButton.style.cssText = `
      background: #3a3a3a;
      border: 2px solid #555;
      border-radius: 8px;
      padding: 1rem;
      color: white;
      cursor: pointer;
      transition: all 0.2s;
      width: 100%;
    `

    // 모달 버튼
    const modalButton = document.createElement('button')
    modalButton.innerHTML = `
      <div style="display: flex; align-items: center; gap: 1rem;">
        <span style="font-size: 1.5rem;">🎭</span>
        <div style="text-align: left;">
          <div style="font-weight: bold;">모달로 열기</div>
          <div style="font-size: 0.9rem; opacity: 0.7;">현재 창 위에 오버레이로 표시</div>
        </div>
      </div>
    `
    modalButton.style.cssText = `
      background: #3a3a3a;
      border: 2px solid #555;
      border-radius: 8px;
      padding: 1rem;
      color: white;
      cursor: pointer;
      transition: all 0.2s;
      width: 100%;
    `

    // 취소 버튼
    const cancelButton = document.createElement('button')
    cancelButton.textContent = '취소'
    cancelButton.style.cssText = `
      background: #666;
      border: none;
      border-radius: 6px;
      padding: 0.75rem 2rem;
      color: white;
      cursor: pointer;
      font-size: 1rem;
      display: block;
      margin: 0 auto;
      transition: all 0.2s;
    `

    // 이벤트 리스너들
    const addHoverEffect = (button: HTMLElement, hoverColor: string) => {
      button.addEventListener('mouseenter', () => {
        button.style.borderColor = hoverColor
        button.style.transform = 'translateY(-2px)'
      })
      button.addEventListener('mouseleave', () => {
        button.style.borderColor = '#555'
        button.style.transform = 'translateY(0)'
      })
    }

    addHoverEffect(popupButton, '#2196f3')
    addHoverEffect(modalButton, '#9c27b0')

    popupButton.addEventListener('click', () => {
      document.body.removeChild(dialog)
      resolve('popup')
    })

    modalButton.addEventListener('click', () => {
      document.body.removeChild(dialog)
      resolve('modal')
    })

    cancelButton.addEventListener('click', () => {
      document.body.removeChild(dialog)
      resolve(null)
    })

    // ESC 키로 취소
    const handleKeydown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        document.body.removeChild(dialog)
        document.removeEventListener('keydown', handleKeydown)
        resolve(null)
      }
    }
    document.addEventListener('keydown', handleKeydown)

    // 오버레이 클릭으로 취소
    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) {
        document.body.removeChild(dialog)
        resolve(null)
      }
    })

    buttonContainer.appendChild(popupButton)
    buttonContainer.appendChild(modalButton)

    selector.appendChild(title)
    selector.appendChild(subtitle)
    selector.appendChild(buttonContainer)
    selector.appendChild(cancelButton)
    dialog.appendChild(selector)
    document.body.appendChild(dialog)
  })
}

// 🆕 컴포넌트 선택 후 열기
export const selectAndOpenComponent = async (): Promise<void> => {
  const componentName = await showComponentSelector()
  if (!componentName) return

  const mode = await showDisplayModeDialog(componentName)
  if (!mode) return

  await openComponent(componentName, { mode })
}
// 🚪 범용 닫기 함수 - 팝업/모달 자동 감지하여 닫기 (더 안전한 버전)
export const closeWindow = (): boolean => {
  console.log('🚪 범용 닫기 함수 호출')

  try {
    const isPopupWindow = window.opener !== null

    console.log(`🔍 팝업 창 여부: ${isPopupWindow}`)

    if (isPopupWindow) {
      // 팝업 창 모드
      console.log('🪟 팝업 창 닫기 시도')

      // 부모 창에 알림
      if (window.opener && !window.opener.closed) {
        try {
          window.opener.postMessage(
            { type: 'popup-closing', timestamp: Date.now() },
            window.location.origin,
          )
        } catch (error) {
          console.warn('⚠️ 부모 창 통신 실패:', error)
        }
      }

      // 창 닫기
      window.close()

      // 브라우저에서 창 닫기가 실패할 경우 대비
      setTimeout(() => {
        if (!window.closed) {
          console.warn('⚠️ 자동 창 닫기 실패')
          alert('창을 수동으로 닫아주세요. (Alt+F4 또는 Ctrl+W)')
        }
      }, 100)

      return true
    } else {
      // 모달 모드
      console.log('📱 모달 닫기 시도')

      const modalManager = ModalManager.getInstance()
      console.log('🎯 ModalManager 인스턴스 획득')

      const success = modalManager.closeModal()

      console.log(`🎯 모달 닫기 결과: ${success}`)

      if (!success) {
        console.warn('⚠️ 닫을 모달이 없습니다')

        modalManager.debugPrintModals()
      }

      return success
    }
  } catch (error) {
    console.error('❌ 창 닫기 중 오류:', error)

    return false
  }
}

// 🚪 특정 모달 닫기 (ID로)
export const closeModalWindow = (modalId?: string) => {
  console.log(`🚪 모달 닫기 함수 호출: ${modalId || '최근 모달'}`)

  if (modalId) {
    return ModalManager.getInstance().closeModal(modalId)
  } else {
    return ModalManager.getInstance().closeModal()
  }
}

// 🚪 모든 모달 닫기
export const closeAllModalWindows = () => {
  console.log('🚪 모든 모달 닫기')
  ModalManager.getInstance().closeAllModals()
}

// 하드웨어 에러 로그 팝업
export const openHardwareErrorLog = (options: DisplayOptions = {}) =>
  openComponent('hardware-error-log', options)
