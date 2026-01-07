# Migrate Skill (마이그레이션 관리 스킬)

단계별 마이그레이션 실행 및 관리 스킬. Feature Flag 기반 점진적 배포, 롤백 플레이북 제공.

## 개요

리팩토링 또는 새 기능 배포 시 안전한 마이그레이션을 위한 자동화 스킬입니다. Phase별 체크리스트 생성, Feature Flag 관리, 모니터링, 롤백 플레이북을 제공합니다.

## 사용 시점

- Architecture Refactoring Phase 2-4 실행 시
- 새 기능 프로덕션 배포 시
- 대규모 리팩토링 실행 시
- 문제 발생 시 긴급 롤백 필요 시

## 주요 기능

### 1. 마이그레이션 계획 수립
- Phase별 체크리스트 자동 생성
- 의존성 분석 및 순서 결정
- 롤백 포인트 정의
- 리스크 평가

### 2. Feature Flag 관리
- 새 기능 활성화/비활성화
- Canary Release 비율 조정 (0% → 10% → 50% → 100%)
- 사용자 그룹별 제어

### 3. 모니터링 대시보드
- 에러율 실시간 추적
- API 응답 시간 측정
- WebSocket 연결 상태 확인
- 알람 자동 트리거

### 4. 자동 롤백
- 에러율 임계값 초과 시 자동 롤백
- 데이터 정합성 검증
- 롤백 성공 여부 확인

## 커맨드

### `/migrate plan`
마이그레이션 계획 수립

```bash
# 사용법
/migrate plan --phase=2

# 옵션
--phase: Phase 번호 (0, 1, 2, 3, 4)
--output: 출력 파일 경로 (기본: docs/migration/phase-{N}-plan.md)
```

**생성되는 문서 예시**:
```markdown
# Migration Plan: Phase 2 - Backend Refactoring

## 개요
- Phase: 2
- 예상 기간: 5일
- 담당: Backend Team
- 목표: SatelliteTrackingEngine 추출

## 체크리스트

### Pre-Migration (마이그레이션 전)
- [ ] 현재 코드 백업 (Git 브랜치 생성)
- [ ] 테스트 환경 준비
- [ ] 의존성 패키지 버전 확인
- [ ] 데이터베이스 백업 (해당 시)

### During Migration (마이그레이션 중)
- [ ] SatelliteTrackingEngine.kt 파일 생성
- [ ] EphemerisService 중복 코드 제거
- [ ] PassScheduleService 중복 코드 제거
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 실행

### Post-Migration (마이그레이션 후)
- [ ] 코드 리뷰 완료
- [ ] 성능 테스트 통과
- [ ] 문서 업데이트
- [ ] PR 병합

## 롤백 포인트
1. **Rollback Point 1**: SatelliteTrackingEngine 생성 전
2. **Rollback Point 2**: 중복 코드 제거 전
3. **Rollback Point 3**: 통합 완료 전

## 리스크 평가
| 리스크 | 발생 가능성 | 영향도 | 완화 방안 |
|--------|------------|--------|----------|
| 컴파일 에러 | 중간 | 높음 | 단계별 커밋, 테스트 |
| 성능 저하 | 낮음 | 높음 | 성능 테스트 |
| 기존 기능 영향 | 낮음 | 매우 높음 | 회귀 테스트 |

## 의존성 분석
- EphemerisService → SatelliteTrackingEngine (새 의존성)
- PassScheduleService → SatelliteTrackingEngine (새 의존성)
- WebSocketHandler → SatelliteTrackingEngine (간접 의존성)
```

---

### `/migrate start`
마이그레이션 시작 (Feature Flag 활성화)

```bash
# 사용법
/migrate start --feature=tracking-engine --canary=10

# 옵션
--feature: Feature Flag 이름
--canary: Canary Release 비율 (0-100, 기본: 0)
--user-group: 특정 사용자 그룹만 활성화 (선택)
```

**동작**:
1. Feature Flag 활성화
2. Canary 비율 설정 (예: 10% 사용자에게만 적용)
3. 모니터링 시작
4. 알람 활성화

**예시 출력**:
```
✅ Feature Flag 'tracking-engine' 활성화 완료
📊 Canary Release: 10% 사용자에게 적용 중
🔍 모니터링 시작: http://localhost:3000/monitoring
⏰ 알람 활성화: 에러율 > 5% 시 자동 알림
```

---

### `/migrate status`
현재 마이그레이션 상태 확인

```bash
# 사용법
/migrate status --feature=tracking-engine
```

**출력 예시**:
```
📊 Migration Status: tracking-engine

상태: ✅ 진행 중 (Canary Release)
활성화 비율: 10%
시작 시간: 2026-01-07 10:00:00
경과 시간: 2시간 30분

모니터링 지표:
- 에러율: 0.2% ✅ (임계값: 5%)
- 평균 응답 시간: 45ms ✅ (임계값: 100ms)
- WebSocket 연결: 98% ✅ (임계값: 90%)

최근 에러:
- 없음

권장 조치:
✅ 모든 지표 정상. Canary 비율을 50%로 증가해도 좋습니다.
```

---

### `/migrate increase`
Canary Release 비율 증가

```bash
# 사용법
/migrate increase --feature=tracking-engine --to=50

# 옵션
--feature: Feature Flag 이름
--to: 증가할 비율 (0-100)
```

**예시**:
```
📈 Canary Release 비율 증가 중...
  10% → 50%

⏳ 5분간 모니터링 후 자동 확인...

✅ 증가 완료!
   - 에러율: 0.3% (정상)
   - 응답 시간: 47ms (정상)
   - WebSocket: 97% (정상)
```

---

### `/migrate rollback`
긴급 롤백 실행

```bash
# 사용법
/migrate rollback --to=phase1

# 옵션
--to: 롤백할 Phase 또는 Commit
--reason: 롤백 사유 (필수)
```

**동작**:
1. Feature Flag 즉시 비활성화
2. Git 브랜치 전환 또는 Revert
3. 데이터 정합성 검증
4. 서비스 재시작 (필요 시)
5. 롤백 보고서 생성

**예시 출력**:
```
🚨 긴급 롤백 실행 중...

1. Feature Flag 비활성화: tracking-engine ✅
2. Git 브랜치 전환: feature/phase2-tracking-engine → main ✅
3. 의존성 복원 ✅
4. 서비스 재시작 ✅
5. 데이터 정합성 검증 ✅

롤백 완료!
소요 시간: 3분 20초

롤백 보고서: docs/migration/rollback-report-20260107.md
```

---

### `/migrate validate`
마이그레이션 검증

```bash
# 사용법
/migrate validate --phase=2
```

**검증 항목**:
- ✅ 단위 테스트 통과
- ✅ 통합 테스트 통과
- ✅ 성능 테스트 통과 (응답 시간 < 100ms)
- ✅ 에러율 < 1%
- ✅ 코드 커버리지 > 40% (BE), > 30% (FE)

---

## Feature Flag 구현 예시

### Backend (Kotlin)
```kotlin
// FeatureFlagService.kt
@Service
class FeatureFlagService {
    private val flags = ConcurrentHashMap<String, FeatureFlag>()

    fun isEnabled(flagName: String, userId: String? = null): Boolean {
        val flag = flags[flagName] ?: return false

        if (!flag.enabled) return false

        // Canary Release
        if (flag.canaryPercent < 100) {
            val hash = userId?.hashCode() ?: 0
            return (hash % 100) < flag.canaryPercent
        }

        return true
    }

    fun setCanaryPercent(flagName: String, percent: Int) {
        flags[flagName]?.let {
            it.canaryPercent = percent
        }
    }
}

data class FeatureFlag(
    val name: String,
    var enabled: Boolean,
    var canaryPercent: Int = 0
)

// 사용 예시
@Service
class EphemerisService(
    private val featureFlagService: FeatureFlagService
) {
    fun calculatePosition(...): Position {
        return if (featureFlagService.isEnabled("tracking-engine")) {
            // 새 코드: SatelliteTrackingEngine 사용
            trackingEngine.calculatePosition(...)
        } else {
            // 기존 코드: 레거시 로직
            calculatePositionLegacy(...)
        }
    }
}
```

### Frontend (TypeScript)
```typescript
// featureFlagStore.ts
export const useFeatureFlagStore = defineStore('featureFlag', () => {
    const flags = ref<Map<string, boolean>>(new Map())

    const isEnabled = (flagName: string): boolean => {
        return flags.value.get(flagName) ?? false
    }

    const fetchFlags = async () => {
        const response = await axios.get('/api/feature-flags')
        flags.value = new Map(Object.entries(response.data))
    }

    return { flags, isEnabled, fetchFlags }
})

// 사용 예시
const featureFlagStore = useFeatureFlagStore()

if (featureFlagStore.isEnabled('tracking-engine')) {
    // 새 코드: trackingStateStore 사용
    trackingStateStore.start()
} else {
    // 기존 코드: ephemerisTrackingState 사용
    ephemerisTrackingState.start()
}
```

---

## 모니터링 설정

### Prometheus + Grafana 예시
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'acs-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'

# 알람 규칙 (alerts.yml)
groups:
  - name: migration_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_errors_total[5m]) > 0.05
        for: 2m
        annotations:
          summary: "에러율 5% 초과"
          description: "마이그레이션 롤백 고려 필요"

      - alert: SlowResponse
        expr: histogram_quantile(0.95, http_server_requests_duration_seconds) > 0.1
        for: 5m
        annotations:
          summary: "응답 시간 100ms 초과"
```

### 대시보드 URL
- Grafana: http://localhost:3000/d/migration
- Prometheus: http://localhost:9090/graph

---

## 롤백 플레이북

### 자동 롤백 트리거 조건
1. 에러율 > 5% (5분 지속)
2. 평균 응답 시간 > 100ms (10분 지속)
3. WebSocket 연결 < 90% (3분 지속)
4. Critical 에러 발생 (즉시)

### 수동 롤백 절차
```bash
# 1. Feature Flag 비활성화
/migrate rollback --to=phase1 --reason="에러율 10% 초과"

# 2. Git 브랜치 전환
git checkout main
git pull origin main

# 3. 서비스 재시작
./gradlew bootRun  # Backend
npm run dev        # Frontend

# 4. 검증
curl http://localhost:8080/actuator/health
curl http://localhost:9000/api/health

# 5. 보고서 작성
/migrate validate --phase=1
```

---

## 참고 문서

- [Architecture_Refactoring_plan.md](../../docs/features/active/Architecture_Refactoring/Architecture_Refactoring_plan.md)
- [RFC_Configuration_Management.md](../../docs/features/active/Architecture_Refactoring/RFC_Configuration_Management.md)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0.0 | 2026-01-07 | 최초 작성 |
