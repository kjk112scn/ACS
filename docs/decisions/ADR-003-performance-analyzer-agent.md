# ADR-003: Performance Analyzer 에이전트 도입

## 상태

**승인됨 (Accepted)** - 2026-01-07

## 컨텍스트

ACS 프로젝트에는 대형 파일들이 다수 존재합니다:
- EphemerisService.kt (4,986줄)
- PassSchedulePage.vue (4,841줄)
- icdStore.ts (2,971줄)
- PassScheduleService.kt (2,896줄)
- ICDService.kt (2,788줄)

이러한 대형 파일들의 성능 분석과 최적화를 체계적으로 수행할 전문 에이전트가 필요했습니다.

### 기존 에이전트의 한계

| 에이전트 | 성능 분석 가능 여부 | 한계 |
|---------|-------------------|------|
| refactorer | 부분적 | 구조 개선 중심, 성능 측정 부족 |
| code-reviewer | 부분적 | 품질 중심, 성능 지표 없음 |
| fullstack-helper | 제한적 | 개발 중심, 분석 도구 부족 |

## 결정

**성능 분석 전문 에이전트 `performance-analyzer`를 도입합니다.**

### 핵심 기능

1. **Frontend 성능 분석**
   - Vue 렌더링 최적화 (shallowRef, computed, v-memo)
   - ECharts 대용량 데이터 처리
   - 번들 크기 분석

2. **Backend 성능 분석**
   - WebFlux blocking 호출 감지
   - Coroutine 병렬화 분석
   - 대형 서비스 분리 제안

3. **통신 성능 분석**
   - WebSocket 쓰로틀링
   - UDP(ICD) 폴링 간격 최적화

### 도구 권한

```yaml
tools: Read, Grep, Glob, Bash
model: opus
```

- Read/Grep/Glob: 코드 분석
- Bash: 성능 측정 명령 실행
- Edit 권한 없음: 분석만 수행, 수정은 다른 에이전트에게 위임

## 대안

### 대안 1: refactorer 에이전트 확장

**장점:**
- 기존 에이전트 활용
- 추가 설정 불필요

**단점:**
- 역할 과부하
- 단일 책임 원칙 위반
- 성능 분석 전문성 부족

### 대안 2: 외부 도구 연동 (Lighthouse, profiler)

**장점:**
- 정확한 성능 측정
- 업계 표준 도구

**단점:**
- 설정 복잡
- 자동화 어려움
- 프로젝트 특화 분석 한계

### 대안 3: 에이전트 없이 /health 스킬만 사용

**장점:**
- 간단한 구조
- 빠른 점검

**단점:**
- 심층 분석 불가
- 개선 방안 제시 한계

## 결과

### 긍정적 결과

1. **전문화된 성능 분석**
   - 대형 파일 전문 분석
   - 구체적인 병목 지점 식별
   - 측정 기반 개선안 제시

2. **역할 분리 명확화**
   - performance-analyzer: 분석
   - refactorer: 구조 개선
   - code-reviewer: 품질 검토

3. **연계 워크플로우**
   ```
   /health (빠른 체크)
     ↓ 문제 발견
   performance-analyzer (심층 분석)
     ↓ 개선안 도출
   refactorer (실제 수정)
   ```

### 부정적 결과

1. **에이전트 증가**
   - 12개 → 13개로 증가
   - 관리 포인트 추가

2. **중복 가능성**
   - refactorer와 일부 역할 중복
   - 명확한 사용 가이드 필요

## 참고

- [refactorer 에이전트](.claude/agents/refactorer.md)
- [/health 스킬](.claude/skills/health/SKILL.md)
- [SYSTEM_OVERVIEW.md](../references/architecture/SYSTEM_OVERVIEW.md)

---

**결정자**: 개발팀
**날짜**: 2026-01-07
