# EphemerisDesignationPage Position View 성능 최적화

> **상태**: ✅ 코드 완료 (테스트 필요)
> **시작일**: 2026-01-27
> **완료일**: 2026-01-27
> **Review ID**: #R002

---

## 문제

위성 추적 시간이 길어질수록 프론트엔드가 점점 느려지고 끊김 발생.

**원인**: 100ms마다 차트 업데이트 시 포인트가 무한 증가

---

## 적용된 최적화

| # | 최적화 | 파일 | 상태 |
|:-:|--------|------|:----:|
| 1 | Worker 제거 → O(1) push | ephemerisTrackStore.ts | ✅ |
| 2 | Store 중복 타이머 제거 | ephemerisTrackStore.ts | ✅ |
| 3 | LTTB 다운샘플링 (1,500개 제한) | utils/sampling.ts | ✅ |
| 4 | ChartUpdatePool 전체 교체 방식 | EphemerisDesignationPage.vue | ✅ |
| 5 | ECharts sampling 옵션 제거 (polar 미지원) | EphemerisDesignationPage.vue | ✅ |

---

## 변경된 파일

```
frontend/src/
├── utils/
│   └── sampling.ts              # NEW: LTTB 알고리즘 + IncrementalLTTB 클래스
├── stores/mode/
│   └── ephemerisTrackStore.ts   # LTTB 샘플러 적용
└── pages/mode/
    └── EphemerisDesignationPage.vue  # ChartUpdatePool 수정
```

---

## 현재 상태 (내일 확인 필요)

### 테스트 필요

1. **콘솔 로그 확인** (F12)
   ```
   📊 LTTB: raw=1500 → sampled=1500 (2.3ms)
   ```
   - 이 로그가 나오면 LTTB 작동 중
   - 안 나오면 아직 1000개 미만

2. **끊김 현상**
   - 1000개 이하에서도 끊기면 → 다른 병목 존재
   - 1000개 이상에서 끊기면 → LTTB 계산 시간 문제

### 의심되는 추가 병목

| 항목 | 설명 |
|------|------|
| Vue 반응성 | `trackingPath`가 deep ref로 매번 트리거 |
| chart.setOption | 100ms마다 전체 option 처리 |
| detailData (궤적) | 매번 캐싱 체크 연산 |

---

## 다음 단계

1. **테스트**: 위성 추적하면서 콘솔 로그 확인
2. **성능 측정**: performance.now()로 updateChart 시간 측정
3. **추가 최적화** (필요시):
   - shallowRef 전환
   - requestAnimationFrame 사용
   - 업데이트 주기 조정 (100ms → 200ms)

---

## 참고 문서

- [REVIEW.md](REVIEW.md) - 초기 분석 결과

---

**마지막 업데이트**: 2026-01-27
