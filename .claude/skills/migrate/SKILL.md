---
name: migrate
description: 단계별 마이그레이션 실행 및 관리 스킬. Feature Flag 기반 점진적 배포, 롤백 플레이북 제공. "migrate", "마이그레이션", "롤백", "배포" 키워드에 반응.
model: sonnet
---

# Migrate Skill - 마이그레이션 관리

Feature Flag 기반 점진적 배포 및 롤백 관리.

## 커맨드

| 커맨드 | 설명 | 옵션 |
|--------|------|------|
| `/migrate plan` | 마이그레이션 계획 수립 | `--phase=N` |
| `/migrate start` | Feature Flag 활성화 | `--feature=NAME --canary=10` |
| `/migrate status` | 현재 상태 확인 | `--feature=NAME` |
| `/migrate increase` | Canary 비율 증가 | `--feature=NAME --to=50` |
| `/migrate rollback` | 긴급 롤백 | `--to=phaseN --reason="사유"` |
| `/migrate validate` | 마이그레이션 검증 | `--phase=N` |

## 워크플로우

```
[plan] → [start] → [status] → [increase] → [validate]
   │         │          │          │            │
 계획 수립  Canary 10%  모니터링   50% → 100%   검증/완료
                        ↓
                   문제 발생 시
                        ↓
                   [rollback]
```

## Canary Release 단계

```
0% → 10% → 50% → 100%
     ↓      ↓       ↓
   5분    10분    최종
  모니터링 모니터링 완료
```

## 자동 롤백 트리거

| 조건 | 임계값 | 지속시간 |
|------|--------|----------|
| 에러율 | > 5% | 5분 |
| 응답시간 | > 100ms | 10분 |
| WebSocket | < 90% | 3분 |
| Critical 에러 | 발생 | 즉시 |

## plan 생성 문서 구조

```markdown
# Migration Plan: Phase N

## 체크리스트
### Pre-Migration
- [ ] Git 브랜치 생성/백업
- [ ] 테스트 환경 준비

### During Migration
- [ ] 코드 변경
- [ ] 단위/통합 테스트

### Post-Migration
- [ ] 코드 리뷰
- [ ] 문서 업데이트

## 롤백 포인트
1. 변경 전
2. 테스트 전
3. 완료 전

## 리스크 평가
| 리스크 | 가능성 | 영향 | 완화 |
```

## Feature Flag 개념

```kotlin
// Backend 사용 패턴
if (featureFlagService.isEnabled("new-feature")) {
    // 새 코드
} else {
    // 기존 코드
}

// Canary: userId hash % 100 < canaryPercent
```

```typescript
// Frontend 사용 패턴
if (featureFlagStore.isEnabled('new-feature')) {
    // 새 코드
}
```

## 수동 롤백 절차

1. `/migrate rollback --to=phaseN --reason="사유"`
2. `git checkout main && git pull`
3. 서비스 재시작
4. 헬스체크: `/actuator/health`, `/api/health`
5. `/migrate validate --phase=N`

## 호출 에이전트

| 에이전트 | 역할 |
|---------|------|
| `tech-lead` | 마이그레이션 계획 검토 |
| `debugger` | 롤백 시 원인 분석 |
| `test-expert` | 검증 테스트 |

---

**버전:** 1.1.0 | **갱신:** 2026-01-22
