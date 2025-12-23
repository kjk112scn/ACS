# PassSchedule 차트 최적화 리팩토링

## 개요

PassSchedulePage의 Position View 차트를 최적화하여 성능을 개선하고, keyhole 여부에 따라 올바른 경로를 표시하며, 스케줄 전환 시 메모리 누적 문제를 해결합니다.

## 목표

1. **차트 업데이트 최적화**: ECharts의 `appendData` API 활용으로 증분 업데이트 구현
2. **메모리 누적 문제 해결**: 스케줄 전환 시 이전 스케줄의 경로만 초기화
3. **경로 데이터 정확성**: keyhole 여부에 따라 올바른 경로 데이터 사용
4. **차트 시리즈 구조 일치**: PassChartUpdatePool을 차트 시리즈 구조와 일치하도록 수정

## 주요 개선 사항

- 실시간 추적 경로 증분 업데이트 (appendData 활용)
- 스케줄 전환 시 경로 초기화 로직 추가
- 차트 시리즈 구조 일치 (4개 시리즈 관리)
- 메모리 보호 (50,000개 포인트 초과 시 자동 정리)

## 관련 문서

- [상세 계획서](./PassSchedule_Chart_Optimization_plan.md)
- [관련 기능: PassSchedule Keyhole 표시 개선](../PassSchedule_Keyhole_Display_Enhancement/PassSchedule_Keyhole_Display_Enhancement_plan.md)

## 상태

🔄 진행 중

