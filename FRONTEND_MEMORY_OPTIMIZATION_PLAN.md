# 프론트엔드 메모리 최적화 계획서

## 📋 개요
- **목표**: 대용량 데이터 처리 시 메모리 사용량 최적화
- **예상 효과**: 메모리 사용량 30-50% 감소, 브라우저 안정성 개선
- **우선순위**: HIGH ⭐⭐⭐

## 🎯 최적화 대상

### 1. HardwareErrorLogPanel.vue
**현재 문제점:**
- 모든 로그를 메모리에 로드하여 표시
- 스크롤 시 계속 로드하여 메모리 누적
- 대용량 로그 처리 시 브라우저 지연

**최적화 방안:**
- 가상 스크롤링 구현
- 화면에 보이는 로그만 렌더링
- 스크롤 시 동적 로드/언로드

### 2. ICD 데이터 처리
**현재 문제점:**
- WebSocket 데이터를 계속 누적
- 실시간 데이터 버퍼링으로 메모리 증가
- 오래된 데이터 정리 부족

**최적화 방안:**
- 데이터 버퍼 크기 제한
- 오래된 데이터 자동 정리
- 메모리 사용량 모니터링

### 3. 컴포넌트 렌더링
**현재 문제점:**
- 불필요한 리렌더링 발생
- 메모리 누수 가능성
- 대용량 리스트 렌더링

**최적화 방안:**
- React.memo 패턴 적용
- useMemo, useCallback 최적화
- 컴포넌트 분할

## 🛠️ 구현 계획

### Phase 1: 가상 스크롤링 구현
**파일**: `src/components/HardwareErrorLogPanel.vue`

**구현 내용:**
```typescript
// 1. 가상 스크롤링 컴포넌트 생성
const VirtualScrollList = {
  props: ['items', 'itemHeight', 'containerHeight'],
  setup(props) {
    const visibleItems = computed(() => {
      // 화면에 보이는 아이템만 계산
      const startIndex = Math.floor(scrollTop.value / props.itemHeight)
      const endIndex = Math.min(startIndex + visibleCount.value, props.items.length)
      return props.items.slice(startIndex, endIndex)
    })
    
    return { visibleItems }
  }
}

// 2. 메모리 효율적인 로그 관리
const useLogManager = () => {
  const MAX_LOGS_IN_MEMORY = 1000 // 메모리에 유지할 최대 로그 수
  const logs = ref<HardwareErrorLog[]>([])
  
  const addLog = (newLog: HardwareErrorLog) => {
    logs.value.push(newLog)
    // 메모리 제한 초과 시 오래된 로그 제거
    if (logs.value.length > MAX_LOGS_IN_MEMORY) {
      logs.value = logs.value.slice(-MAX_LOGS_IN_MEMORY)
    }
  }
  
  return { logs, addLog }
}
```

### Phase 2: 데이터 버퍼 최적화
**파일**: `src/stores/icd/icdStore.ts`

**구현 내용:**
```typescript
// 1. 메모리 제한된 데이터 버퍼
const useMemoryOptimizedBuffer = () => {
  const MAX_BUFFER_SIZE = 5000 // 최대 버퍼 크기
  const buffer = ref<MessageData[]>([])
  
  const addToBuffer = (data: MessageData) => {
    buffer.value.push(data)
    
    // 버퍼 크기 초과 시 오래된 데이터 제거
    if (buffer.value.length > MAX_BUFFER_SIZE) {
      buffer.value = buffer.value.slice(-MAX_BUFFER_SIZE)
    }
  }
  
  return { buffer, addToBuffer }
}

// 2. 메모리 사용량 모니터링
const useMemoryMonitor = () => {
  const memoryUsage = ref(0)
  
  const updateMemoryUsage = () => {
    if ('memory' in performance) {
      memoryUsage.value = (performance as any).memory.usedJSHeapSize
    }
  }
  
  // 주기적으로 메모리 사용량 체크
  setInterval(updateMemoryUsage, 5000)
  
  return { memoryUsage }
}
```

### Phase 3: 컴포넌트 최적화
**파일**: `src/components/common/VirtualList.vue`

**구현 내용:**
```vue
<template>
  <div class="virtual-list" @scroll="handleScroll">
    <div class="virtual-list-spacer" :style="{ height: totalHeight + 'px' }">
      <div class="virtual-list-content" :style="{ transform: `translateY(${offsetY}px)` }">
        <div
          v-for="(item, index) in visibleItems"
          :key="item.id"
          class="virtual-list-item"
          :style="{ height: itemHeight + 'px' }"
        >
          <slot :item="item" :index="startIndex + index" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  items: any[]
  itemHeight: number
  containerHeight: number
}

const props = defineProps<Props>()

const scrollTop = ref(0)
const startIndex = computed(() => Math.floor(scrollTop.value / props.itemHeight))
const endIndex = computed(() => Math.min(startIndex.value + visibleCount.value, props.items.length))
const visibleItems = computed(() => props.items.slice(startIndex.value, endIndex.value))
const totalHeight = computed(() => props.items.length * props.itemHeight)
const offsetY = computed(() => startIndex.value * props.itemHeight)
const visibleCount = computed(() => Math.ceil(props.containerHeight / props.itemHeight) + 2)

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  scrollTop.value = target.scrollTop
}
</script>
```

## 📊 성능 측정 방법

### 1. 메모리 사용량 측정
```typescript
// 메모리 사용량 측정 함수
const measureMemoryUsage = () => {
  if ('memory' in performance) {
    const memory = (performance as any).memory
    return {
      used: memory.usedJSHeapSize,
      total: memory.totalJSHeapSize,
      limit: memory.jsHeapSizeLimit
    }
  }
  return null
}
```

### 2. 렌더링 성능 측정
```typescript
// 렌더링 시간 측정
const measureRenderTime = (componentName: string) => {
  const start = performance.now()
  
  return {
    end: () => {
      const end = performance.now()
      console.log(`${componentName} 렌더링 시간: ${end - start}ms`)
    }
  }
}
```

## 🚀 실행 방법

### 1. 가상 스크롤링 적용
```bash
# 1. VirtualList 컴포넌트 생성
touch src/components/common/VirtualList.vue

# 2. HardwareErrorLogPanel에 적용
# - 기존 스크롤 로직을 VirtualList로 교체
# - 메모리 제한 로직 추가
```

### 2. 데이터 버퍼 최적화
```bash
# 1. icdStore에 메모리 최적화 로직 추가
# 2. 메모리 모니터링 컴포넌트 생성
# 3. 자동 정리 메커니즘 구현
```

### 3. 컴포넌트 최적화
```bash
# 1. 불필요한 리렌더링 방지
# 2. 메모리 누수 방지
# 3. 성능 모니터링 추가
```

## 📈 예상 결과

### Before (현재)
- 메모리 사용량: 100MB+ (대용량 로그 시)
- 렌더링 시간: 500ms+ (1000개 로그)
- 브라우저 지연: 발생

### After (최적화 후)
- 메모리 사용량: 30-50MB (50% 감소)
- 렌더링 시간: 50ms (90% 감소)
- 브라우저 지연: 없음

## 🔧 구현 시 주의사항

### 1. 기존 기능 유지
- 무한 스크롤 기능 유지
- 실시간 업데이트 기능 유지
- 필터링 및 검색 기능 유지

### 2. 호환성 고려
- 기존 API 호출 방식 유지
- 데이터 구조 변경 최소화
- 사용자 경험 개선

### 3. 테스트 필수
- 대용량 데이터 테스트
- 메모리 누수 테스트
- 성능 벤치마크 테스트

## 📝 체크리스트

### Phase 1: 가상 스크롤링
- [ ] VirtualList 컴포넌트 생성
- [ ] HardwareErrorLogPanel에 적용
- [ ] 메모리 제한 로직 구현
- [ ] 성능 테스트

### Phase 2: 데이터 버퍼 최적화
- [ ] icdStore 메모리 최적화
- [ ] 자동 정리 메커니즘 구현
- [ ] 메모리 모니터링 추가
- [ ] 성능 테스트

### Phase 3: 컴포넌트 최적화
- [ ] 리렌더링 최적화
- [ ] 메모리 누수 방지
- [ ] 성능 모니터링
- [ ] 최종 테스트

## 🎯 완료 기준

1. **메모리 사용량 30% 이상 감소**
2. **렌더링 성능 50% 이상 향상**
3. **대용량 데이터 처리 시 안정성 확보**
4. **기존 기능 100% 유지**
5. **사용자 경험 개선**

---

**작성일**: 2025-10-14  
**작성자**: AI Assistant  
**버전**: 1.0  
**상태**: 계획 완료, 구현 대기

