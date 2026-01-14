# PassSchedule_ApplyRowColors_Infinite_Loop

## 버그 정보

| 항목 | 내용 |
|------|------|
| **보고일** | 2026-01-06 |
| **심각도** | 🟠 High |
| **상태** | ✅ 수정완료 |
| **영향 범위** | PassSchedulePage - 스케줄 테이블 하이라이트 |

## 증상

PassSchedulePage에서 `applyRowColors()` 함수가 무한 반복 호출됨.
콘솔에 아래 로그가 끊임없이 반복 출력:
```
🎨 DOM 직접 조작으로 색상 적용 시작
현재 Store 상태: {current: null, currentDetailId: null, next: null, nextDetailId: null}
총 57개 행 처리
✅ DOM 직접 조작 완료
```

## 재현 단계

1. Pass Schedule 페이지 진입
2. 콘솔 로그 확인
3. 위 로그가 계속 반복됨 (1초에 여러 번)

## 예상 결과 vs 실제 결과

- **예상**: 스케줄 테이블의 현재/다음 스케줄 하이라이트가 값 변경 시에만 업데이트
- **실제**: 값이 변경되지 않아도 계속 DOM 조작 반복 실행

## 관련 문서

- [ANALYSIS.md](ANALYSIS.md) - 원인 분석
- [FIX.md](FIX.md) - 수정 내용
