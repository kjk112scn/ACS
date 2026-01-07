# PassSchedule_ApplyRowColors_Infinite_Loop 원인 분석

## 1. 증상 요약

PassSchedulePage에서 `applyRowColors()` 함수가 값 변경 없이도 무한 반복 호출되어 성능 저하 발생.

## 2. 분석 과정

### 2.1 로그 분석
```
🎨 DOM 직접 조작으로 색상 적용 시작
현재 Store 상태: {current: null, currentDetailId: null, next: null, nextDetailId: null}
총 57개 행 처리
✅ DOM 직접 조작 완료
(위 패턴 무한 반복)
```

**특이사항**: Store 상태가 모두 `null`로 변경되지 않는데도 계속 실행됨.

### 2.2 코드 추적

| 순서 | 파일 | 위치 | 설명 |
|------|------|------|------|
| 1 | PassSchedulePage.vue | Line 528-534 | watch: currentTrackingMstId/DetailId → applyRowColors() |
| 2 | PassSchedulePage.vue | Line 675-682 | watch: nextTrackingMstId/DetailId → applyRowColors() |
| 3 | PassSchedulePage.vue | Line 1163-1186 | watch: 4개 값 동시 감시 + `deep: true` → applyRowColors() |
| 4 | PassSchedulePage.vue | Line 976-1068 | applyRowColors() 함수 본체 |

### 2.3 데이터 흐름
```
WebSocket 데이터 수신
    ↓
icdStore 업데이트 (같은 값이어도 참조 변경 가능)
    ↓
watch #1 트리거 → applyRowColors() + setTimeout(100ms)
watch #2 트리거 → applyRowColors() + setTimeout(100ms)
watch #3 트리거 (deep: true) → applyRowColors() + setTimeout(100ms)
    ↓
3개의 setTimeout이 거의 동시에 실행
    ↓
그 사이 WebSocket 데이터 또 수신 → 반복
```

## 3. 근본 원인 (Root Cause)

**원인**: 동일한 함수를 3개의 watch에서 중복 호출 + 값 변경 체크 없음

**Why 분석 (5 Whys):**
1. Why? → applyRowColors()가 계속 호출됨
2. Why? → 3개의 watch가 모두 이 함수를 호출함
3. Why? → watch #3이 이미 watch #1, #2가 감시하는 값을 또 감시 (중복)
4. Why? → watch #3에 `deep: true` 옵션이 있어 참조 변경만으로도 트리거
5. Why? → **applyRowColors()에 값 변경 체크 로직이 없어서 매번 DOM 조작 실행**

## 4. 영향 범위

| 영역 | 영향 여부 | 설명 |
|------|----------|------|
| Backend | ❌ | 영향 없음 |
| Frontend | ✅ | PassSchedulePage 성능 저하, 콘솔 스팸 |
| Algorithm | ❌ | 영향 없음 |
| 다른 기능 | ❌ | 해당 페이지에만 국한 |

## 5. 관련 파일

| 파일 | 역할 | 수정 필요 |
|------|------|----------|
| frontend/src/pages/mode/PassSchedulePage.vue | 스케줄 페이지 | ✅ |
