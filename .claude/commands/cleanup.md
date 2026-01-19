---
name: cleanup
description: 코드 품질 일괄 정리. console.log, print, 광범위 catch, 하드코딩 값 정리. "정리", "cleanup", "console.log", "print", "삭제" 키워드에 반응.
model: opus
---

# Cleanup - 코드 품질 정리 스킬

## 역할

코드베이스의 품질 이슈를 일괄 정리합니다.

**핵심 가치:**
- 자동화된 정리
- 일관된 코드 스타일
- 프로덕션 준비
- 디버깅 효율 향상

## 워크플로우

```
[1. 스캔] → [2. 분류] → [3. 계획] → [4. 정리] → [5. 검증] → [6. 완료]
     │           │           │           │           │           │
  이슈 탐지    우선순위     배치 계획   자동/수동   빌드 확인    /done
              분류          수립        정리
```

## 사용법

| 명령 | 설명 |
|------|------|
| `/cleanup` | 전체 스캔 및 정리 계획 |
| `/cleanup console` | console.log 정리 |
| `/cleanup print` | print/println 정리 |
| `/cleanup catch` | 광범위 catch 개선 |
| `/cleanup colors` | 하드코딩 색상 정리 |
| `/cleanup important` | !important 정리 |

## 정리 대상 (ACS 프로젝트)

### Frontend 이슈
| 이슈 | 건수 | 우선순위 |
|------|------|----------|
| console.log | 1,513개 | 🟠 High |
| 하드코딩 색상 | 520건 | 🟡 Medium |
| !important | 1,690개 | 🟡 Medium |
| as 타입 단언 | 280건 | 🟡 Medium |

### Backend 이슈
| 이슈 | 건수 | 우선순위 |
|------|------|----------|
| print/println | 102건 | 🟠 High |
| 광범위 catch | 180+건 | 🟠 High |
| !! 연산자 | 46건 | 🟡 Medium |
| 매직 넘버 | 40+건 | 🟡 Medium |

## console.log 정리

### 전략
```typescript
// Step 1: devLog 유틸 생성
// utils/devLog.ts
export const devLog = {
  debug: (...args: any[]) => {
    if (import.meta.env.DEV) {
      console.log('[DEBUG]', ...args)
    }
  },
  info: (...args: any[]) => {
    if (import.meta.env.DEV) {
      console.info('[INFO]', ...args)
    }
  }
}

// Step 2: 일괄 변환
// Before
console.log('데이터:', data)

// After (필요한 경우)
devLog.debug('데이터:', data)

// After (불필요한 경우)
// 삭제
```

### 정리 카테고리
```yaml
삭제 대상:
  - 테스트용 console.log
  - 임시 디버깅 로그
  - 주석 처리된 console.log

devLog 변환 대상:
  - 개발 시 유용한 로그
  - 상태 변화 추적

유지 대상:
  - 에러 로깅 (console.error)
  - 중요 경고 (console.warn)
```

## print/println 정리

### 전략
```kotlin
// Before
println("Debug: $value")
System.out.println("Info: $message")

// After
logger.debug("Debug: {}", value)
logger.info("Info: {}", message)
```

### Logger 설정
```kotlin
// 각 클래스에 Logger 추가
companion object {
    private val logger = LoggerFactory.getLogger(ClassName::class.java)
}
```

## 광범위 catch 개선

### 전략
```kotlin
// Before (❌)
try {
    doSomething()
} catch (e: Exception) {
    // 너무 광범위
}

// After (✅)
try {
    doSomething()
} catch (e: IOException) {
    logger.error("IO 오류: {}", e.message, e)
    throw CommunicationException("통신 실패", e)
} catch (e: IllegalArgumentException) {
    logger.warn("잘못된 입력: {}", e.message)
    throw ValidationException("입력값 오류", e)
}
```

### 예외 분류 가이드
| 원본 | 세분화 |
|------|--------|
| Exception | IOException, IllegalArgumentException 등 |
| RuntimeException | NullPointerException, IndexOutOfBoundsException 등 |

## 호출 에이전트

| 에이전트 | 역할 | 호출 시점 |
|---------|------|---------|
| `fe-expert` | FE 정리 | console.log, 색상 |
| `be-expert` | BE 정리 | print, catch |
| `code-counter` | 카운팅 | 정리 전후 비교 |
| `code-reviewer` | 품질 검증 | 완료 후 |

## 사용 예시

### 예시 1: console.log 일괄 정리
```
User: "/cleanup console"

→ cleanup 워크플로우:

[스캔]
  총 1,513개 console.log 발견
  - pages/: 423개
  - stores/: 612개
  - components/: 478개

[분류]
  - 삭제 대상: 1,200개
  - devLog 변환: 250개
  - 유지 (error/warn): 63개

[계획]
  1. devLog 유틸 생성
  2. 파일별 순차 정리
  3. 빌드 확인

[정리]
  fe-expert 호출 → 배치 처리

[검증]
  - console.log: 1,513 → 63 (error/warn만)
  - 빌드: ✅ 성공

[완료]
  /done → 커밋
```

### 예시 2: print 정리
```
User: "/cleanup print"

→ cleanup 워크플로우:

[스캔]
  102개 print/println 발견
  - ElevationCalculator.kt: 45개
  - InitService.kt: 23개
  - ICDService.kt: 34개

[정리]
  be-expert 호출
  - Logger 추가
  - print → logger.debug 변환

[검증]
  - print: 102 → 0
  - 빌드: ✅ 성공
```

### 예시 3: 전체 정리
```
User: "/cleanup"

→ 전체 스캔 결과:

📊 코드 품질 스캔 결과
━━━━━━━━━━━━━━━━━━━━━━

🔴 High (즉시 정리 권장)
├── console.log: 1,513개
├── print/println: 102건
└── 광범위 catch: 180+건

🟡 Medium (점진적 정리)
├── 하드코딩 색상: 520건
├── !important: 1,690개
├── as 타입 단언: 280건
├── !! 연산자: 46건
└── 매직 넘버: 40+건

어떤 항목부터 정리할까요?
1. /cleanup console
2. /cleanup print
3. /cleanup catch
```

## 정리 검증

### 정리 전후 비교
```bash
# Frontend
grep -r "console\." frontend/src --include="*.vue" --include="*.ts" | wc -l

# Backend
grep -rE "print(ln)?\(" backend/src --include="*.kt" | wc -l
```

### 빌드 확인
```bash
cd frontend && npm run build
cd backend && ./gradlew clean build -x test
```

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-14
**호환:** ACS 프로젝트 전용
