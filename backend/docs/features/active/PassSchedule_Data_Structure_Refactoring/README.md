# PassSchedule 데이터 구조 리팩토링

## 개요

PassSchedule의 MST/DTL 데이터 구조를 전역 고유 ID 기반으로 재설계하여 데이터 식별 및 조회의 일관성과 확장성을 확보합니다.

## 목표

1. **전역 고유 ID 도입**: 모든 위성의 모든 패스를 구분하는 전역 넘버링 시스템 구현
2. **명확한 필드 구분**: MstId, DetailId, Index의 역할 명확화
3. **데이터 일관성**: 백엔드와 프론트엔드 간 데이터 식별자 일관성 보장
4. **확장성 확보**: 향후 DetailId 확장 가능한 구조 설계

## 주요 변경 사항

- 전역 고유 MstId 도입 (기존 위성별 인덱스 → 전역 넘버링)
- DetailId 필드 명시화 (현재는 항상 0, 향후 확장 가능)
- Index 필드 명확화 (100ms 포인트 순번)
- PassNo 제거 (필요 시 계산 가능)
- SatelliteID/SatelliteName 구분 명확화

## 관련 문서

- [상세 계획서](./PassSchedule_Data_Structure_Refactoring_plan.md)

## 상태

🔄 진행 중


