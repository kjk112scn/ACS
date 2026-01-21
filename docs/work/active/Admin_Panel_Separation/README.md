# Admin_Panel_Separation

## 개요

**목적**: 관리자 설정(하드웨어 제어 기능)을 Settings 모달에서 분리하여 별도 Admin Panel로 제공
**요청일**: 2026-01-21
**상태**: ✅ 완료

## 배경

Settings 모달에 포함된 Admin 탭은 하드웨어를 직접 제어하는 위험 기능들을 포함:
- Servo Encoder Preset
- Servo Alarm Reset
- M/C On/Off
- Maintenance (리미트 스위치 포함)

일반 설정(다크모드, 언어 등)과 성격이 다르므로 분리 필요.

## 요구사항

- [x] 헤더에 Admin 버튼 추가 (Settings 버튼 우측)
- [x] AdminPanel.vue 모달 컴포넌트 생성
- [x] 기존 admin 컴포넌트 재사용
- [x] SettingsModal에서 Admin 탭 제거

## 영향 범위

| 영역 | 파일/컴포넌트 |
|------|--------------|
| Frontend | MainLayout.vue, SettingsModal.vue |
| 신규 | AdminPanel.vue |
| 재사용 | admin/*.vue (변경 없음) |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황