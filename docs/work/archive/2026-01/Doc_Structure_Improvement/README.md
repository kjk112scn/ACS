# 문서 구조 개선 (Doc Structure Improvement)

> **상태**: 진행 예정 | **우선순위**: P1 | **예상 소요**: 1-2일

## 개요

Claude Code와 최적 연계를 위한 문서 구조 개선 프로젝트.

- **ADR**: [ADR-004-claude-code-optimized-doc-structure](../../decisions/ADR-004-claude-code-optimized-doc-structure.md)
- **목표**: 토큰 절감 53%, 문서 수 54% 감소, 계층적 컨텍스트

## 현재 문제

| 문제 | 현황 | 목표 |
|------|------|------|
| CLAUDE.md 크기 | 169줄 | 80줄 |
| 문서 총 개수 | 129개 | ~60개 |
| 중복 문서 | 5쌍 이상 | 0 |
| 하위 CLAUDE.md | 0개 | 2개 |

## 구현 계획

### Phase 1: 계층적 CLAUDE.md (2시간)

- [ ] `frontend/CLAUDE.md` 생성
- [ ] `backend/CLAUDE.md` 생성
- [ ] 루트 `CLAUDE.md` 축소 (169줄 -> 80줄)

### Phase 2: docs/core/ 통합 (4시간)

- [ ] `docs/core/` 폴더 생성
- [ ] `SYSTEM.md` - SYSTEM_OVERVIEW 핵심 추출
- [ ] `DATA_FLOW.md` - 데이터 흐름 통합
- [ ] `DOMAIN.md` - 도메인 개념 통합
- [ ] `CONVENTIONS.md` - 코딩 규칙 통합
- [ ] `llms.txt` 생성

### Phase 3: legacy 정리 (1일)

- [ ] `work/active/Architecture_Refactoring/legacy/` 압축 보관
- [ ] `architecture/context/analysis/` 정리
- [ ] 깨진 링크 수정

### Phase 4: 가드레일 (지속)

- [ ] `/sync` 스킬에 가드레일 추가
- [ ] 월간 문서 건강도 점검 프로세스

## 파일 목록

### 생성 예정

| 파일 | 목적 |
|------|------|
| `frontend/CLAUDE.md` | FE 전용 규칙 |
| `backend/CLAUDE.md` | BE 전용 규칙 |
| `docs/core/SYSTEM.md` | 시스템 개요 축소판 |
| `docs/core/DATA_FLOW.md` | 데이터 흐름 |
| `docs/core/DOMAIN.md` | 도메인 개념 |
| `docs/core/CONVENTIONS.md` | 규칙 통합 |
| `llms.txt` | LLM 탐색용 인덱스 |

### 제거/이동 예정

| 파일/폴더 | 조치 |
|----------|------|
| `work/active/*/legacy/` | 압축 보관 |
| `context/analysis/backend/` | 삭제 |
| `context/analysis/frontend/` | 삭제 |
| `context/analysis/domain-logic/` | 삭제 |

## 참조

- [ADR-004](../../decisions/ADR-004-claude-code-optimized-doc-structure.md)
- [현재 CLAUDE.md](/CLAUDE.md)
- [문서 README](../../README.md)

---

**생성일**: 2026-01-17
**담당**: architect
