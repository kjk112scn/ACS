# ACS 프로젝트 리팩토링 계획 V3 (백엔드/프론트엔드 분리)

## 📋 프로젝트 개요

- **프로젝트명**: ACS (안테나 제어 시스템)
- **기술스택**: Vue 3 + Quasar + TypeScript + Kotlin Spring Boot
- **현재 상태**: 기능적으로는 작동하지만 구조적으로 리팩토링 필요
- **목표**: 실용적이고 단순한 구조로 개선, UI 재사용성 향상

## 🎯 핵심 요구사항

1. **한글 인코딩 문제** 해결 (최우선)
2. **에러 관리 기능** 통합
3. **UI 재사용성** 개선 (notify, dialog, loading 등)
4. **한영 호환** 시스템 구축
5. **폴더 구조** 정리 (기능별 분리)
6. **과도한 분할** 피하기 (기존 기능 유지)

## ⚠️ 핵심 원칙

- **기존 기능 유지** - 동작하는 코드는 건드리지 않음
- **점진적 개선** - 한 번에 하나씩만 수정
- **실용적 접근** - 복잡한 분할보다는 통합에 집중
- **테스트 우선** - 각 단계마다 빌드 및 기능 테스트

## �� 한글 인코딩 문제 해결 방안

### **문제 원인 분석**

1. **파일 인코딩 불일치** - UTF-8과 CP949 혼재
2. **Windows 명령어 한글 처리** - echo 명령어 한글 깨짐
3. **빌드 시스템 인코딩** - Vite/Quasar 빌드 시 한글 처리 문제
4. **Git 설정** - Git LFS 또는 인코딩 설정 문제

### **해결 방안**

1. **파일 인코딩 통일**

   - 모든 `.vue`, `.ts`, `.js` 파일을 UTF-8 BOM 없이 저장
   - VS Code 설정에서 `"files.encoding": "utf8"` 설정
   - `.editorconfig` 파일 생성하여 인코딩 규칙 정의

2. **빌드 환경 설정**

   - `quasar.config.ts`에서 Vite 인코딩 설정 추가
   - `tsconfig.json`에서 컴파일러 옵션 조정
   - `package.json` 스크립트에 인코딩 옵션 추가

3. **Git 설정 최적화**

   - `.gitattributes` 파일 생성하여 텍스트 파일 인코딩 규칙 정의
   - Git LFS 설정 확인 및 조정
   - `core.autocrlf` 설정 조정

4. **개발 도구 설정**
   - VS Code 확장 프로그램 설치 (Korean Language Pack)
   - 터미널 인코딩 설정 (chcp 65001)
   - 파일 탐색기에서 한글 파일명 처리 확인

---

# 🖥️ 프론트엔드 리팩토링 (Vue 3 + Quasar + TypeScript)

## 📊 프론트엔드 진행사항 (2025.01.23 기준)

### ✅ 완료된 단계 (Stage 0-4)

#### **Stage 0: 한국어 인코딩 문제 해결** ✅

- [x] `.editorconfig` 파일 생성 및 설정
- [x] `.gitattributes` 파일 생성 및 설정
- [x] UTF-8 인코딩 통일
- [x] 빌드 환경 설정 완료
- [x] 터미널 인코딩 설정 (chcp 65001)
- [x] VS Code 인코딩 설정 확인 및 수정
- [x] `quasar.config.ts`에 Vite 인코딩 설정 추가
- [x] `tsconfig.json` 컴파일러 옵션 조정
- [x] `package.json` 스크립트에 인코딩 옵션 추가
- [x] 모든 `.vue` 파일을 UTF-8로 재저장
- [x] 모든 `.ts` 파일을 UTF-8로 재저장
- [x] 모든 `.js` 파일을 UTF-8로 재저장
- [x] `????` 패턴 검색 및 수정
- [x] TrajectoryPoint 인터페이스 구문 오류 수정
- [x] 기타 구문 오류 수정
- [x] 빌드 성공 확인

#### **Stage 1: 긴급 문제 해결** ✅

- [x] `npm run build` 성공 확인
- [x] `npm run dev` 정상 실행 확인
- [x] 주요 페이지 접근 테스트
- [x] 로그인 기능 테스트
- [x] 대시보드 로딩 테스트
- [x] 주요 모드 페이지 테스트

#### **Stage 2: 통합 관리 시스템 구축** ✅

- [x] `src/composables/useErrorHandler.ts` 생성
- [x] `src/composables/useNotification.ts` 생성
- [x] `src/composables/useDialog.ts` 생성
- [x] `src/composables/useLoading.ts` 생성
- [x] `src/composables/useValidation.ts` 생성
- [x] 통합된 에러 처리 로직 구현
- [x] API 에러, 유효성 검사 에러, 일반 에러 분류
- [x] 에러 로깅 시스템 구축
- [x] Quasar Notify 통합 관리
- [x] 성공, 경고, 에러, 정보 알림 타입별 처리
- [x] 기존 개별 notify 코드 통합
- [x] 확인, 취소, 입력 다이얼로그 통합
- [x] 기존 개별 dialog 코드 통합
- [x] 전역 로딩 상태 관리
- [x] 개별 컴포넌트 로딩 상태 통합
- [x] 공통 유효성 검사 규칙 정의
- [x] 에러 메시지 통합 관리

#### **Stage 3: 한영 호환 시스템 구축** ✅

- [x] `src/i18n/ko-KR/index.ts` 생성
- [x] `src/i18n/en-US/index.ts` 생성
- [x] `src/composables/useI18n.ts` 생성
- [x] 언어 변경 기능 구현
- [x] VS Code i18n Ally 확장 설정
- [x] 타입 안전한 번역 시스템
- [x] 하드코딩된 한글 메시지를 i18n 키로 변환
- [x] 에러 메시지 번역화
- [x] UI 텍스트 번역화

#### **Stage 4: 폴더 구조 정리** ✅

- [x] `src/types/common.ts` - 공통 타입들
- [x] `src/types/settings.ts` - 설정 관련 타입들
- [x] `src/types/icd.ts` - ICD 관련 타입들
- [x] `src/types/mode.ts` - 모드 관련 타입들
- [x] `src/types/index.ts` - 모든 타입 export
- [x] `src/services/api/` 디렉토리 생성
- [x] `src/services/common/` 디렉토리 생성
- [x] `src/services/mode/` 디렉토리 생성
- [x] API 관련 서비스들 통합
- [x] 공통 API 클라이언트 구축
- [x] `src/stores/api/` 디렉토리 생성
- [x] `src/stores/common/` 디렉토리 생성
- [x] `src/stores/mode/` 디렉토리 생성
- [x] 관련 스토어들 그룹화
- [x] 공통 스토어 로직 통합
- [x] `src/components/common/` 디렉토리 생성
- [x] `src/components/content/` 디렉토리 생성
- [x] `src/components/settings/` 디렉토리 생성
- [x] alias 경로 설정 (`@/` 경로 통일)
- [x] 네이밍 규칙 통일 (kebab-case 폴더, PascalCase 파일)
- [x] 인덱스 파일 생성 및 통합 export 관리
- [x] import 경로 수정 및 통일

### �� 현재 진행 중 (Stage 5: 성능 최적화)

#### **5.1 WebSocket 최적화** (진행 예정)

- [ ] 프론트엔드 싱글톤 패턴 구현
- [ ] 구독 관리 시스템 구축
- [ ] 브로드캐스트 시스템 구현
- [ ] 기존 코드 마이그레이션
- **예상 효과**: 메모리 70% 감소, 연결 수 90% 감소

#### **5.2 메모리 최적화** (진행 예정)

- [ ] 컴포넌트 언마운트 시 정리
- [ ] 가상 스크롤링 적용
- [ ] 메모리 누수 방지
- **예상 효과**: 메모리 사용량 50% 감소

#### **5.3 번들 최적화** (진행 예정)

- [ ] 동적 import 적용
- [ ] 불필요한 라이브러리 제거
- [ ] 코드 분할 최적화
- **예상 효과**: 번들 크기 30% 감소

#### **5.4 렌더링 성능 최적화** (진행 예정)

- [ ] computed/watch 최적화
- [ ] v-memo 적용
- [ ] 성능 모니터링 추가
- **예상 효과**: 렌더링 속도 40% 향상

### 📋 프론트엔드 다음 단계 (Stage 6-8)

#### **Stage 6: 테스트 및 검증** (예상 소요시간: 2-3시간)

- [ ] **6.1 기능 테스트**: 모든 페이지 및 API 연동 테스트
- [ ] **6.2 성능 테스트**: 로딩 속도, 메모리, 번들 크기 테스트

#### **Stage 7: 공통 UI 컴포넌트 구축** (예상 소요시간: 4-5시간)

- [ ] **7.1 공통 UI 컴포넌트**: BaseButton, BaseInput, BaseSelect, BaseCard, BaseModal, BaseForm
- [ ] **7.2 공통 차트 컴포넌트**: BaseChart, BaseGauge, BaseStatus
- [ ] **7.3 기존 컴포넌트 통합**: 중복 코드 제거 및 일관성 적용

#### **Stage 8: 문서화** (예상 소요시간: 1-2시간)

- [ ] **8.1 코드 문서화**: JSDoc 추가 및 타입 문서화
- [ ] **8.2 사용법 문서화**: README 업데이트 및 API 문서 작성

---

# 🖥️ 백엔드 리팩토링 (Kotlin Spring Boot)

## 📊 백엔드 진행사항 (2025.01.23 기준)

### ✅ 완료된 단계

#### **백엔드 WebSocket 분석 완료** ✅

- [x] **현재 구현 상태 분석**:

  - PushDataController 아키텍처 분석
  - PushDataService 데이터 생성 로직 분석
  - 세션별 전용 스레드 관리 방식 분석
  - 30ms 주기 실시간 데이터 전송 분석

- [x] **성능 문제점 식별**:

  - 사용자당 개별 WebSocket 연결
  - 세션별 전용 스레드 생성 (메모리 낭비)
  - 중복 데이터 전송 (CPU 낭비)
  - 스레드 리소스 비효율적 사용

- [x] **최적화 방안 설계**:
  - 브로드캐스트 시스템 도입
  - 연결 풀링 및 스레드 최적화
  - 메모리 사용량 70-80% 감소 예상

### �� 백엔드 최적화 계획 (시스템 안정화 후 진행)

#### **백엔드 Stage 1: 브로드캐스트 시스템 구현** (예상 소요시간: 1일)

- [ ] **1.1 단일 데이터 생성기 구현**

  - [ ] 공유 데이터 버퍼 생성
  - [ ] 30ms마다 한 번만 데이터 생성
  - [ ] 데이터 변경 감지 로직 구현

- [ ] **1.2 구독자 관리 시스템**

  - [ ] ConcurrentHashMap으로 구독자 관리
  - [ ] 구독자 추가/제거 로직
  - [ ] 구독자 상태 모니터링

- [ ] **1.3 브로드캐스트 엔진**
  - [ ] 단일 스케줄러로 브로드캐스트
  - [ ] 모든 구독자에게 동시 전송
  - [ ] 전송 실패 처리 로직

#### **백엔드 Stage 2: 연결 풀링 및 스레드 최적화** (예상 소요시간: 1일)

- [ ] **2.1 스레드 풀 최적화**

  - [ ] 고정 크기 스레드 풀 생성
  - [ ] CPU 코어 수 기반 스레드 수 계산
  - [ ] 스레드 풀 모니터링 시스템

- [ ] **2.2 연결 풀링 시스템**

  - [ ] WebSocket 연결 풀 관리
  - [ ] 연결 재사용 로직
  - [ ] 연결 상태 체크 및 정리

- [ ] **2.3 리소스 관리 최적화**
  - [ ] 메모리 사용량 모니터링
  - [ ] 자동 리소스 정리
  - [ ] 성능 메트릭 수집

#### **백엔드 Stage 3: 메모리 최적화 및 성능 테스트** (예상 소요시간: 1일)

- [ ] **3.1 메모리 최적화**

  - [ ] 공유 데이터 버퍼 최적화
  - [ ] 불필요한 객체 생성 방지
  - [ ] 메모리 누수 방지

- [ ] **3.2 성능 테스트**

  - [ ] 다중 사용자 연결 테스트
  - [ ] 메모리 사용량 측정
  - [ ] CPU 사용량 측정
  - [ ] 네트워크 대역폭 측정

- [ ] **3.3 모니터링 시스템**
  - [ ] 실시간 성능 모니터링
  - [ ] 알림 시스템 구축
  - [ ] 로그 시스템 최적화

## 🔍 백엔드 WebSocket 분석 결과

### **현재 구현 상태**

#### **아키텍처 구조**

PushDataController (WebSocket 연결 관리)
├── 세션별 전용 스레드 생성
├── 30ms 주기 실시간 데이터 전송
└── 세션별 독립적 스케줄러
PushDataService (데이터 생성)
├── 클라이언트 카운트 관리
├── 실시간 데이터 생성
└── DataStoreService 연동

#### **현재 성능 특성**

- **연결 방식**: 사용자당 개별 WebSocket 연결
- **전송 주기**: 30ms (약 33.3 msg/s)
- **스레드 관리**: 세션별 전용 스레드 (`websocket-{sessionId8}`)
- **메모리 사용**: 연결당 독립적인 스케줄러와 리소스

### **🚨 발견된 최적화 포인트**

#### **1. 메모리 사용량 문제**

```kotlin
// 현재: 각 사용자마다 개별 스케줄러 생성
val sessionExecutor = Executors.newSingleThreadScheduledExecutor(sessionThreadFactory)

// 문제점:
// - 사용자 10명 = 10개 스케줄러
// - 사용자 100명 = 100개 스케줄러
// - 메모리 사용량 선형 증가
```

#### **2. 중복 데이터 전송**

```kotlin
// 현재: 모든 사용자에게 동일한 데이터를 개별 전송
session.send(Mono.just(session.textMessage(realtimeData)))

// 문제점:
// - 동일한 데이터를 N번 생성
// - 네트워크 대역폭 낭비
// - CPU 사용량 증가
```

#### **3. 스레드 리소스 낭비**

```kotlin
// 현재: 세션별 전용 스레드
val threadName = "websocket-$shortSessionId"

// 문제점:
// - 사용자 수만큼 스레드 생성
// - 컨텍스트 스위칭 오버헤드
// - 스레드 풀 미활용
```

### **💡 최적화 방안**

#### **1. 브로드캐스트 시스템 도입**

```kotlin
// 최적화 후: 단일 데이터 생성 + 브로드캐스트
class OptimizedPushDataController {
    private val subscribers = ConcurrentHashMap<String, WebSocketSession>()
    private val singleScheduler = Executors.newSingleThreadScheduledExecutor()

    // 30ms마다 한 번만 데이터 생성
    private fun startBroadcast() {
        singleScheduler.scheduleAtFixedRate({
            val data = generateSingleData()
            broadcastToAllSubscribers(data)
        }, 0, 30, TimeUnit.MILLISECONDS)
    }
}
```

#### **2. 연결 풀링 및 스레드 최적화**

```kotlin
// 최적화 후: 스레드 풀 활용
private val threadPool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() * 2
)

// 브로드캐스트 시 스레드 풀 사용
private fun broadcastToAllSubscribers(data: String) {
    subscribers.values.forEach { session ->
        threadPool.submit {
            session.send(Mono.just(session.textMessage(data)))
        }
    }
}
```

#### **3. 메모리 최적화**

```kotlin
// 최적화 후: 공유 데이터 버퍼
private val sharedDataBuffer = AtomicReference<String>()
private val lastUpdateTime = AtomicLong(0)

// 데이터 변경 시에만 새로 생성
private fun updateSharedData() {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastUpdateTime.get() > 30) {
        sharedDataBuffer.set(generateRealtimeData())
        lastUpdateTime.set(currentTime)
    }
}
```

### **�� 예상 성능 개선 효과**

#### **메모리 사용량**

- **현재**: 사용자 수 × 스케줄러 메모리
- **최적화 후**: 고정 스레드 풀 + 공유 버퍼
- **개선율**: **70-80% 감소**

#### **CPU 사용량**

- **현재**: 사용자 수 × 데이터 생성 비용
- **최적화 후**: 단일 데이터 생성 + 브로드캐스트
- **개선율**: **60-70% 감소**

#### **네트워크 효율성**

- **현재**: 개별 전송으로 중복 데이터
- **최적화 후**: 브로드캐스트로 효율적 전송
- **개선율**: **50-60% 감소**

#### **확장성**

- **현재**: 사용자 수에 선형 증가
- **최적화 후**: 사용자 수에 무관한 고정 리소스
- **개선율**: **10배 이상 확장성 향상**

---

# 📅 전체 일정 및 우선순위

## ��️ 프론트엔드 일정

### **즉시 시작 가능 (Stage 5)**

1일차: WebSocket 싱글톤 패턴 구현
2일차: 메모리 최적화 및 가상 스크롤링
3일차: 번들 최적화 및 동적 import
4일차: 렌더링 성능 최적화

### **시스템 안정화 후 (Stage 6)**

5일차: 기능 테스트 및 성능 테스트

### **마지막 단계 (Stage 7-8)**

6-7일차: 공통 UI 컴포넌트 구축
8일차: 문서화 및 최종 검증

## 🖥️ 백엔드 일정

### **시스템 안정화 후 (백엔드 최적화)**

1일차: 브로드캐스트 시스템 구현
2일차: 연결 풀링 및 스레드 최적화
3일차: 메모리 최적화 및 성능 테스트

## 📈 예상 효과

### **단기 효과 (1-2주)**

- 성능 최적화로 사용자 경험 크게 개선
- 메모리 사용량 70% 감소
- 로딩 속도 50% 단축
- WebSocket 연결 효율성 90% 향상

### **중기 효과 (1-3개월)**

- 개발 속도 30-40% 향상
- 버그 발생률 50% 감소
- 유지보수 비용 40% 감소
- 백엔드 확장성 10배 향상

### **장기 효과 (6개월+)**

- 팀 생산성 2배 향상
- 기술 부채 대폭 감소
- 확장성 크게 향상
- 다국어 지원 완성

## 🚀 다음 진행 방향

### **우선순위 1: 프론트엔드 WebSocket 최적화** (즉시 시작 가능)

1. **싱글톤 WebSocket 서비스 구현**
2. **구독 관리 시스템 구축**
3. **브로드캐스트 시스템 구현**
4. **기존 코드 마이그레이션**

### **우선순위 2: 프론트엔드 메모리 최적화**

- 컴포넌트 언마운트 시 정리
- 가상 스크롤링 적용
- 메모리 누수 방지

### **우선순위 3: 프론트엔드 번들 최적화**

- 동적 import 적용
- 불필요한 라이브러리 제거
- 번들 크기 30% 감소

### **우선순위 4: 프론트엔드 렌더링 성능 최적화**

- computed/watch 최적화
- v-memo 적용
- 성능 모니터링 추가

### **우선순위 5: 백엔드 최적화** (시스템 안정화 후)

- 브로드캐스트 시스템 구현
- 연결 풀링 및 스레드 최적화
- 메모리 최적화 및 성능 테스트

## 🚀 시작 방법

1. **현재 상태 백업** (Git commit)
2. **Stage 5부터 순차적으로 진행** (프론트엔드 성능 최적화)
3. **각 단계 완료 후 테스트** 필수
4. **문제 발생 시 즉시 중단**하고 이전 단계로 롤백

## 🔧 새 PC에서 이어서 진행하는 방법

### **1. 프로젝트 복사**

```bash
git clone [repository-url]
cd ACS
```

### **2. 의존성 설치**

```bash
npm install
```

### **3. 현재 진행 상황 확인**

- Git 히스토리에서 완료된 단계 확인
- 다음 단계부터 진행

### **4. 새 대화에서 이어서 진행**

## ⚠️ 주의사항

- **각 단계는 순차적으로 진행**해야 함
- **프론트엔드 Stage 5(성능 최적화)를 먼저 완료**해야 함
- **백엔드 최적화는 시스템 안정화 후** 진행
- **각 단계 완료 후 테스트** 필수
- **문제 발생 시 즉시 중단**하고 이전 단계로 롤백
- **백업을 자주 생성**할 것
- **과도한 분할 피하기** - 기존 기능 유지 우선
- **공통 UI 컴포넌트는 마지막 단계**에서 구축

---

**생성일**: 2024년 12월 19일  
**업데이트**: 2025년 1월 23일  
**버전**: 3.0  
**상태**: 프론트엔드 Stage 5 진행 중, 백엔드 분석 완료  
**특징**: 프론트엔드/백엔드 분리, 실용적 접근, UI 재사용성 중심, 한영 호환 시스템, 공통 컴포넌트 마지막 단계, 백엔드 최적화 계획 포함, 완전한 진행사항 기록
