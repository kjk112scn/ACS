# PassSchedule Keyhole Display Enhancement

> PassSchedulePage에서 keyhole 위성 정보를 더 명확하게 표시하고, Position View 차트에서 불필요한 경로 표시를 제거

---

## 📋 개요

PassSchedulePage에서 keyhole 위성 정보를 더 명확하게 표시하고, Position View 차트에서 불필요한 경로 표시를 제거하여 성능과 가독성을 개선합니다.

**핵심 문제**:
- Schedule Control 테이블에서 keyhole 위성 여부를 확인하기 어려움
- Schedule Information에 keyhole 정보가 표시되지 않음
- Position View 차트에 여러 스케줄의 경로가 동시에 표시되어 성능 저하 및 가독성 저하

**해결 방안**:
- Schedule Control 테이블에 keyhole 컬럼 추가 및 배지 표시
- Schedule Information에 keyhole 배지 추가
- Position View 차트에서 현재 추적 중인 스케줄 또는 선택된 스케줄의 경로만 표시

## 📁 문서

- **[PassSchedule_Keyhole_Display_Enhancement_plan.md](./PassSchedule_Keyhole_Display_Enhancement_plan.md)**: 원본 계획 문서

## ✅ 상태

- **시작일**: 2025-01
- **버전**: 1.0
- **상태**: 진행 중

---

**관련 파일**: 
- `src/pages/mode/PassSchedulePage.vue`

