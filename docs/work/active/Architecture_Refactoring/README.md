# Architecture Refactoring

> **Last Updated**: 2026-01-15
> **Status**: Active

---

## Quick Links

| Document | Description |
|----------|-------------|
| [PLAN.md](./PLAN.md) | Unified refactoring plan |
| [TRACKER.md](./TRACKER.md) | Execution checklist |
| [IMPROVEMENT_ROADMAP.md](./IMPROVEMENT_ROADMAP.md) | 7-Expert comprehensive roadmap |
| [legacy/](./legacy/) | Previous documents (archived) |

---

## Context Reference

All refactoring decisions are based on:

- [refactoring-hints.md](../../architecture/context/analysis/synthesis/refactoring-hints.md) - Detailed analysis results
- [backend.md](../../architecture/context/architecture/backend.md) - BE architecture context
- [frontend.md](../../architecture/context/architecture/frontend.md) - FE architecture context

---

## Approach

```
Context (Domain Knowledge)
    ↓
PLAN.md (What to do)
    ↓
TRACKER.md (Progress tracking)
    ↓
Execute & Verify
```

---

## Summary (검증 완료)

| Category | Verified Count | Primary Issues |
|----------|:--------------:|----------------|
| 보안 Critical | **6건** | Path Traversal (1), CORS (1), innerHTML XSS (4) |
| BE 안정성 | **36건** | !! (7), Thread.sleep (2), runBlocking (1), GlobalData (18), subscribe (6), Shutdown (2) |
| FE 성능 | **1,022건** | deep watch (34), console.log (988) |
| 대형 파일 | **8개** | BE 4개 (12,318줄), FE 4개 (14,877줄) |

### 일정

| Phase | 목표일 |
|-------|:------:|
| Sprint 0 (보안) | 1/15 (목) |
| Phase 1 (BE) | 1/15 (목) |
| Phase 2 (FE 성능) | 1/16~17 |
| Phase 4 (품질) | 1/17~18 |
| Phase 3 (FE 분리) | 1/18~19 |
| Phase 5 (키보드) | 1/19 (월) |
| Phase 3 (BE 분리) | 1/20~21 |
| **전체 완료** | **1/21 (수)** |

---

**Note**: Legacy documents in `legacy/` folder contain historical decisions and analysis. Refer to them only when needed for context.
