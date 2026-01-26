# Task Management Integration

## 개요

**목적**: 스킬 실행 시 문서 기반 Task Management System 자동 연동
**요청일**: 2026-01-26
**상태**: ✅ 완료

## 요구사항

- [x] PROGRESS.md 체크리스트 → Claude Code TodoWrite 자동 동기화
- [ ] 의존성 태그 파싱 (`[depends: T001, T002]`)
- [ ] 병렬 실행 가능 태그 (`[parallel: true]`)
- [ ] 백그라운드 에이전트 자동 실행
- [ ] 기존 스킬 (feature, bugfix, plan) 통합

## 핵심 가치

| 기존 | 개선 후 |
|------|---------|
| 마크다운 체크박스 수동 관리 | Task System 자동 추적 |
| 순차 실행만 가능 | 병렬 에이전트 실행 |
| 의존성 암묵적 | 명시적 의존성 그래프 |
| tasks.json 별도 관리 | 문서 기반 (추가 파일 불필요) |

## 영향 범위

| 영역 | 파일/컴포넌트 |
|------|--------------|
| Skills | feature, bugfix, plan, done |
| New Files | TASK_SYSTEM.md (규칙), task-parser 로직 |
| Templates | PROGRESS.md 템플릿 확장 |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 상세 설계
- [PROGRESS.md](PROGRESS.md) - 진행 상황
