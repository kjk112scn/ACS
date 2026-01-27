# Ephemeris 추적 버그 분석 리뷰 (#R001)

> **리뷰 날짜:** 2026-01-27
> **분석 범위:** Ephemeris 모드 추적 실패 문제
> **심각도:** Critical

## 1. 문제 현상

사용자 보고:
- Ephemeris 모드에서 위성 스케줄 추가 후 추적 시작
- **"이상한 각도로 이동하더니 추적하지 않음"**

### FE 로그 분석

```
🔄 추적 상태 변경: WAITING → IDLE           ← 초기화 (이전 세션)
🧹 IDLE 상태 전환 - 경로 초기화 완료
Position offset command sent
Ephemeris 추적이 시작되었습니다               ← startTracking 호출
🔄 Ephemeris 상태 변경: true
📊 Azimuth Actual 값: 0                      ← ⚠️ 모든 값이 0
📊 Elevation Actual 값: 0
📊 Train Actual 값: 0
📊 Azimuth CMD 값: 0                         ← ⚠️ CMD도 0
📊 Elevation CMD 값: 0
📊 Train CMD 값: 0
🔄 추적 상태 변경: IDLE → PREPARING
🔄 추적 상태 변경: PREPARING → WAITING
🔄 추적 상태 변경: WAITING → TRACKING        ← TRACKING 도달
🧹 추적 시작 - 경로 초기화 완료
Stop Sun Track command sent                   ← ⚠️ SunTrack 정지 명령?
```

---

## 2. 발견된 이슈

| ID | 심각도 | 문제 | 위치 | 상태 |
|----|:------:|------|------|:----:|
| #R001-C1 | 🔴 Critical | **createRealtimeTrackingData 빈 Map 반환** | EphemerisService.kt:1714-1787 | ⏳ |
| #R001-H1 | 🟠 High | **ephemerisStatus와 CMD 값 설정 타이밍 불일치** | EphemerisService.kt:1099 vs 1387 | ⏳ |
| #R001-M1 | 🟡 Medium | Dashboard에서 0 값 표시 (fallback 없음) | DashboardPage.vue:1502-1523 | ⏳ |
| #R001-L1 | 🔵 Low | SunTrack 정지 명령이 Ephemeris 추적 시 전송됨 | 로그 분석 필요 | ⏳ |

---

## 3. 상세 분석

### #R001-C1: createRealtimeTrackingData 빈 Map 반환 (Critical)

**파일:** [EphemerisService.kt:1714-1787](../../../backend/src/main/kotlin/com/gtlsystems/acs_api/service/mode/EphemerisService.kt#L1714-L1787)

**문제:**
createRealtimeTrackingData 함수에서 여러 조건에서 빈 Map을 반환:

```kotlin
// 조건 1: originalPassDetails.isEmpty()
if (originalPassDetails.isEmpty()) {
    logger.error("❌ 원본 이론치 데이터가 없습니다...")
    return emptyMap()  // ⚠️ 빈 Map 반환
}

// 조건 2: allPassDetails.isEmpty()
if (allPassDetails.isEmpty()) {
    logger.error("❌ 최종 변환 데이터가 없습니다...")
    return emptyMap()  // ⚠️ 빈 Map 반환
}

// 조건 3: finalMst 없음
if (finalMst == null) {
    logger.warn("⚠️ final_transformed MST 데이터를 찾을 수 없습니다...")
    return emptyMap()  // ⚠️ 빈 Map 반환
}

// 조건 4: filteredFinalTransformed.isEmpty()
if (filteredFinalTransformed.isEmpty()) {
    logger.warn("⚠️ 필터링 결과 데이터가 없습니다.")
    return emptyMap()  // ⚠️ 빈 Map 반환
}
```

**영향:**
1. TRACKING 상태에서 CMD 값 업데이트 안 됨
2. 안테나가 위성 위치로 이동하지 않음
3. 사용자가 "추적하지 않음" 경험

**가설:**
- mstId/detailId가 올바르게 전달되지 않음
- ephemerisTrackMstStorage에 데이터가 없음
- 스케줄 선택 시 데이터가 제대로 로드되지 않음

**확인 필요:**
```
BE 로그에서 다음 패턴 확인:
- "❌ [createRealtimeTrackingData] 원본 이론치 데이터가 없습니다"
- "❌ [createRealtimeTrackingData] 최종 변환 데이터가 없습니다"
- "⚠️ [createRealtimeTrackingData] final_transformed MST 데이터를 찾을 수 없습니다"
```

---

### #R001-H1: ephemerisStatus와 CMD 값 설정 타이밍 불일치 (High)

**문제:**
```
1. startModeTimer() 호출 시: ephemerisStatus = true (Line 1099)
2. FE Dashboard watch 트리거 → trackingCMD 값 읽기
3. WAITING → TRACKING 전환 시: trackingCMD 값 설정 (Line 1387-1395)
```

**타임라인:**
```
Time 0: startEphemerisTracking() 호출
Time 1: startModeTimer() → ephemerisStatus = true ✓
Time 2: FE watch 트리거 → trackingCMD 읽기 = 0 ⚠️
Time 3: PREPARING 단계 시작 (Train 이동)
...
Time N: WAITING → TRACKING 전환 → trackingCMD 설정 (너무 늦음)
```

**영향:**
- Dashboard에서 초기에 모든 값이 0으로 표시
- 사용자 혼란 유발

**해결 방향:**
1. ephemerisStatus를 true로 설정하기 전에 trackingCMD 초기값 설정
2. 또는 FE에서 trackingCMD가 0이면 일반 CMD 값 사용 (fallback)

---

### #R001-M1: Dashboard fallback 없음 (Medium)

**파일:** [DashboardPage.vue:1502-1523](../../../frontend/src/pages/DashboardPage.vue#L1502-L1523)

**현재 로직:**
```typescript
watch(() => icdStore.ephemerisStatusInfo.isActive, (newVal) => {
  console.log('📊 Azimuth CMD 값:',
    newVal ? icdStore.trackingCMDAzimuthAngle : icdStore.cmdAzimuthAngle)
})
```

**문제:**
- ephemerisStatusInfo.isActive가 true면 trackingCMD 값 사용
- trackingCMD가 0 또는 빈 문자열이어도 그대로 사용
- fallback 로직 없음

**해결 방향:**
```typescript
// 제안: fallback 로직 추가
const effectiveAzCMD = icdStore.trackingCMDAzimuthAngle &&
                        parseFloat(icdStore.trackingCMDAzimuthAngle) !== 0
  ? icdStore.trackingCMDAzimuthAngle
  : icdStore.cmdAzimuthAngle
```

---

### #R001-L1: SunTrack 정지 명령 (Low)

**로그:**
```
Stop Sun Track command sent: Sun Track UDP 명령어 전송 요청 완료 (Command:)
```

**질문:**
- Ephemeris 추적 시작 시 SunTrack 정지 명령이 왜 전송되는가?
- 이것이 정상 동작인지 확인 필요

---

## 4. 데이터 흐름 분석

```
[스케줄 선택]
     │
     ▼
[selectSchedule()] ─────────────────────┐
  FE: ephemerisStore                    │
  BE: setCurrentTrackingPassId(mstId)   │
     │                                  │
     ▼                                  │
[추적 시작 버튼 클릭]                      │
     │                                  │
     ▼                                  │
[handleEphemerisCommand()]              │
  FE: ephemerisStore.startTracking()    │
     │                                  │
     ▼                                  │
[BE: startEphemerisTracking(mstId, detailId)]
     │
     ├─► ephemerisStatus = true (즉시)
     │       │
     │       ▼
     │   [FE: Dashboard watch 트리거]
     │       trackingCMD = 0 ⚠️
     │
     ├─► PREPARING: Train 이동
     │
     ├─► PREPARING: Az/El 이동
     │
     ├─► WAITING: 시작 시간 대기
     │
     └─► TRACKING:
           │
           ▼
         [createRealtimeTrackingData(mstId, detailId)]
           │
           ├─► 데이터 있음 → CMD 값 설정 ✓
           │
           └─► 데이터 없음 → 빈 Map 반환 ⚠️
                              │
                              ▼
                           CMD 업데이트 안 됨
                              │
                              ▼
                           안테나 이동 안 함
```

---

## 5. 확인 필요 사항

### BE 로그 확인 (Critical)

다음 BE 로그를 확인해주세요:

1. **MstId 관련:**
   ```
   "🚀 위성 추적 시작: mstId = X, detailId = Y"
   ```

2. **데이터 조회 관련:**
   ```
   "❌ [createRealtimeTrackingData] 원본 이론치 데이터가 없습니다"
   "⚠️ [createRealtimeTrackingData] final_transformed MST 데이터를 찾을 수 없습니다"
   "⚠️ [createRealtimeTrackingData] 사용 가능한 MstId 목록: [...]"
   ```

3. **ephemerisTrackMstStorage 상태:**
   - 저장소에 데이터가 있는지
   - mstId가 일치하는지

---

## 6. 권장 조치

| 우선순위 | 이슈 ID | 조치 | 연계 스킬 |
|:--------:|---------|------|----------|
| 1 | #R001-C1 | BE 로그 확인 후 근본 원인 파악 | `/bugfix` |
| 2 | #R001-H1 | ephemerisStatus 설정 전 CMD 초기화 | `/bugfix` |
| 3 | #R001-M1 | Dashboard fallback 로직 추가 | `/bugfix` |
| 4 | #R001-L1 | SunTrack 정지 명령 의도 확인 | - |

---

## 7. 다음 단계

**BE 로그 확인이 가장 중요합니다.**

추적 실패 시점의 BE 로그를 공유해주시면:
1. 근본 원인 확정
2. `/bugfix #R001-C1` 실행으로 수정

---

**리뷰 완료:** 2026-01-27
**리뷰어:** Claude Code
