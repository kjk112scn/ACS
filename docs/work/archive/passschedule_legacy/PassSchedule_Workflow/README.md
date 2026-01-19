# PassSchedule Workflow Documentation

> PassSchedule 서비스·컨트롤러·프론트엔드 전반의 스케줄 수행 흐름을 분석하고 문서화하는 작업입니다.

---

## 📋 개요

- PassSchedule 스케줄 등록 → 상태 머신 → PassSchedulePage UI 반영까지 **엔드투엔드 흐름**을 단일 문서로 정리합니다.
- 운영 중 반복적으로 질의되는 “현재 추적 대상 판단 기준”과 “WebSocket 데이터 연계 구조”를 빠르게 파악할 수 있도록 합니다.
- `active/README` 가이드에 맞춰 상위 폴더에서 독립적으로 관리합니다.

## 📁 문서

- **[PassSchedule_Workflow.md](./PassSchedule_Workflow.md)**: 백엔드/프론트 상호작용과 API, 데이터 구조, 현안까지 정리한 본문

## ✅ 상태

- **시작일**: 2025-11
- **버전**: 1.0
- **상태**: 진행 중

---

**관련 파일**
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/service/mode/PassScheduleService.kt`
- `ACS_API/src/main/kotlin/com/gtlsystems/acs_api/controller/mode/PassScheduleController.kt`
- `ACS/src/pages/mode/PassSchedulePage.vue`

