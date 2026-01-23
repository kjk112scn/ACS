---
name: api-sync
description: Backend-Frontend API 자동 동기화 스킬. OpenAPI 스펙 생성, TypeScript 타입 자동 생성, 불일치 검증. "api-sync", "타입 동기화", "OpenAPI" 키워드에 반응.
model: sonnet
---

# API Sync - FE-BE 타입 동기화 스킬

> Controller/DTO 변경 시 TypeScript 타입 자동 동기화

## 핵심 가치

- OpenAPI 스펙 자동 생성 (SpringDoc 기반)
- TypeScript 타입 자동 생성 (openapi-typescript)
- Breaking Change 자동 검출
- FE-BE 타입 불일치 예방

## 커맨드

| 커맨드 | 설명 | 옵션 |
|--------|------|------|
| `/api-sync generate` | OpenAPI + TS 타입 생성 | `--controller`, `--all` |
| `/api-sync validate` | API 계약 검증 | `--breaking-only` |
| `/api-sync update` | FE 코드 자동 업데이트 | `--auto-fix`, `--dry-run` |
| `/api-sync diff` | 버전 간 비교 | `--from`, `--to` |

## 워크플로우

```
[1. 스캔] → [2. 생성] → [3. 검증] → [4. 업데이트]
     │           │           │           │
 Controller   openapi.yaml  Breaking   FE 코드
 DTO 분석    generated.ts   Change     자동 수정
```

## 출력 예시

```
🔍 Controller 스캔: 5개 (36 엔드포인트)
📝 OpenAPI 스펙: backend/.../openapi.yaml ✅
🔧 TypeScript: frontend/.../generated.ts ✅
⚠️ Breaking Change: LocationSettings.altitude (optional → required)
```

## 검증 항목

| 항목 | 설명 |
|------|------|
| 타입 일치성 | DTO ↔ TypeScript interface |
| 필수 필드 | required 속성 검증 |
| Breaking Change | 호환성 깨지는 변경 검출 |

## 사용 시점

- Controller/DTO 수정 시
- 새 API 엔드포인트 추가 시
- 배포 전 검증
- CI/CD 파이프라인 (자동)

## 연계

- **에이전트**: `api-contract-manager`
- **문서**: `docs/guides/api-sync-implementation.md` (상세 구현)

---

**버전:** 2.0.0 | **모델:** sonnet
