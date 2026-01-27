# Ephemeris DB 로드 타이밍 문제 수정

> **Review ID:** #R001
> **생성일:** 2026-01-27
> **상태:** ⏳ 대기 (내일 작업 예정)

## 문제 요약

**증상:** 서버 끄고 킨 후 Select Schedule 화면에서 최근 등록한 위성이 아닌 예전 것이 표시됨

**원인:** 백엔드 `initFromDatabase()`가 비동기(`subscribe()`)로 실행되어 서버 Ready 상태가 되어도 DB 데이터가 메모리에 로드되지 않음

## 영향 범위

| 구분 | 파일 |
|------|------|
| **BE (수정 필요)** | `EphemerisDataRepository.kt` |
| **FE (영향 없음)** | `EphemerisDesignationPage.vue` |

## 수정 방안

### 1단계: 동기 로드로 변경 (1줄 수정)

**파일:** `backend/src/main/kotlin/.../ephemeris/EphemerisDataRepository.kt`

**위치:** Line 87 부근

```kotlin
// 변경 전
.subscribe()  // 비동기 - 완료 대기 없음

// 변경 후
.block(Duration.ofSeconds(30))  // 동기 대기
```

### 2단계: 테스트

1. 위성 TLE 등록
2. 서버 재시작
3. Select Schedule 모달 열기
4. 등록한 위성이 목록에 표시되는지 확인

## 관련 이슈

| ID | 심각도 | 설명 |
|----|:------:|------|
| #R001-C1 | 🔴 Critical | 비동기 DB 로드 타이밍 문제 |

## 참고

- SWR 적용은 이 문제를 해결하지 못함 (프론트엔드 캐싱 vs 백엔드 초기화 문제)
- PassSchedule도 동일한 구조인지 확인 필요
