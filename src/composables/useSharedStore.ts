import { useICDStore } from '../stores/icd/icdStore'

// ✅ 타입 정의
type ICDStoreType = ReturnType<typeof useICDStore>

// ✅ 타입 가드 함수
const hasSharedStore = (win: Window): win is Window & { sharedICDStore: ICDStoreType } => {
  return 'sharedICDStore' in win && win.sharedICDStore !== undefined
}

export const useSharedICDStore = (): ICDStoreType => {
  console.log('🔍 useSharedICDStore 호출됨')

  // 1. 현재 창의 공유 store 확인
  if (hasSharedStore(window)) {
    console.log('🔄 기존 공유 store 사용')
    return window.sharedICDStore
  }

  // 2. 팝업창인 경우 부모창의 store 확인

  if (window.opener && hasSharedStore(window.opener)) {
    console.log('🔗 부모창 store 연결됨')
    return window.opener.sharedICDStore
  }

  // 3. 새 store 생성 및 전역 등록
  console.log('🌍 새 공유 store 생성')
  const store = useICDStore()

  // Window 객체에 속성 추가
  Object.defineProperty(window, 'sharedICDStore', {
    value: store,
    writable: true,
    configurable: true
  })

  return store
}
