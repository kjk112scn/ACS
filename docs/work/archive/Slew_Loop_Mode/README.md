# Slew Loop Mode

## 개요

**목적**: Slew 모드에서 선택한 축들이 최소/최대 각도 사이를 자동으로 반복 이동
**요청일**: 2026-01-06
**상태**: 🚧 진행중

## 요구사항

- [ ] Loop 체크박스 UI 추가 (전체 테두리 위, Azimuth 축 위)
- [ ] 선택된 축별 독립적 왕복 이동
- [ ] 시스템 설정의 min/max 각도 범위 사용
- [ ] 도달 판정: ±0.5° 연속 3초 OR 모터 OFF
- [ ] Stop 버튼으로 Loop 중단

## 영향 범위

| 영역 | 파일/컴포넌트 |
|------|--------------|
| Frontend | SlewPage.vue, slewModeStore.ts |
| Frontend | angleLimitsSettingsStore.ts (참조) |
| Frontend | icdStore.ts (모터 상태 참조) |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황
