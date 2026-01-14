# ACS 문서 동기화 규칙

## 코드 → 문서 매핑 테이블

### Backend 매핑

| 코드 경로 | 문서 경로 | 동기화 내용 |
|----------|----------|------------|
| `controller/icd/` | `docs/api/ICD_API.md` | ICD 통신 API |
| `controller/mode/` | `docs/api/Mode_API.md` | 모드 제어 API |
| `controller/system/` | `docs/api/System_API.md` | 시스템 API |
| `controller/websocket/` | `docs/api/WebSocket_API.md` | WebSocket API |
| `service/mode/EphemerisService.kt` | `docs/architecture/algorithms/Satellite_Tracking_Overview.md` | 위성 추적 알고리즘 |
| `service/mode/PassScheduleService.kt` | `docs/architecture/algorithms/Pass_Schedule_Logic.md` | 패스 스케줄 로직 |
| `service/mode/SunTrackService.kt` | `docs/architecture/algorithms/Sun_Tracking_Overview.md` | 태양 추적 알고리즘 |
| `service/hardware/` | `docs/guides/Hardware_Error_Messages.md` | 에러 메시지 |
| `algorithm/**/*.kt` | `docs/architecture/algorithms/*.md` | 계산 알고리즘 |

### Frontend 매핑

| 코드 경로 | 문서 경로 | 동기화 내용 |
|----------|----------|------------|
| `pages/mode/*.vue` | `docs/architecture/UI_Architecture.md` | 모드별 페이지 |
| `stores/**/*.ts` | `docs/architecture/Store_Architecture.md` | Pinia 스토어 |
| `composables/**/*.ts` | `docs/guides/Composables_Guide.md` | Vue Composables |
| `components/**/*.vue` | `docs/architecture/Component_Structure.md` | 컴포넌트 구조 |

### 상태 문서 매핑

| 이벤트 | 대상 문서 | 작업 |
|-------|---------|------|
| Controller 추가/삭제 | `PROJECT_STATUS_CURRENT.md` | 통계 업데이트 |
| Service 추가/삭제 | `PROJECT_STATUS_CURRENT.md` | 통계 업데이트 |
| 기능 완료 | `docs/work/active/` → `archive/` | 폴더 이동 |
| 아키텍처 변경 | `SYSTEM_OVERVIEW.md` | 구조 업데이트 |

---

## 추출 규칙

### KDoc 주석 → Markdown

```kotlin
/**
 * 위성 추적 서비스
 * 위성의 위치를 계산하고 추적 정보를 제공합니다.
 */
@Service
class EphemerisService
```

→ 변환:

```markdown
## EphemerisService

**역할:** 위성 추적 서비스

위성의 위치를 계산하고 추적 정보를 제공합니다.

**파일:** `service/mode/EphemerisService.kt`
```

### enum class → 상태 다이어그램

```kotlin
enum class TrackingState {
    IDLE,       // 대기
    PREPARING,  // 준비
    TRACKING,   // 추적 중
    COMPLETED   // 완료
}
```

→ 변환:

```markdown
## 추적 상태 머신

```
IDLE (대기)
  ↓
PREPARING (준비)
  ↓
TRACKING (추적 중)
  ↓
COMPLETED (완료)
```
```

### import 문 → 의존성 그래프

```kotlin
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.service.icd.ICDService
```

→ 변환:

```markdown
## 의존성

- `OrekitCalculator` (algorithm/satellitetracker)
- `ICDService` (service/icd)
```

---

## 파일 크기 기준

| 크기 | 분류 | 문서화 수준 |
|------|------|------------|
| < 300줄 | 소형 | 기본 설명만 |
| 300-1000줄 | 중형 | 주요 메서드 설명 |
| > 1000줄 | 대형 | **상세 문서 필수** |

### 대형 파일 (1000줄+) - 필수 문서화

| 파일 | 줄 수 | 필수 문서 |
|------|-------|----------|
| EphemerisService.kt | 4,986 | Satellite_Tracking_Overview.md |
| PassScheduleService.kt | 2,896 | Pass_Schedule_Logic.md |
| ICDService.kt | 2,788 | ICD_Communication.md |
| icdStore.ts | 2,971 | Store_Architecture.md |
| PassSchedulePage.vue | 4,841 | UI_Architecture.md |

---

## 마커 규칙

### 자동 생성 영역

```markdown
<!-- AUTO-GENERATED: START - 파일 목록 -->
| 파일 | 역할 |
|------|------|
| EphemerisService.kt | 위성 추적 |
<!-- AUTO-GENERATED: END -->
```

### 수동 작성 영역 (보존)

```markdown
<!-- MANUAL: 설계 의도 -->
Train 각도를 사용하는 이유는...
<!-- MANUAL END -->
```

### 혼합 영역

```markdown
## Controller 목록

<!-- AUTO-GENERATED: START -->
...자동 생성...
<!-- AUTO-GENERATED: END -->

### 주의사항
<!-- MANUAL: 주의사항 -->
ICDController는 UDP 통신 전용...
<!-- MANUAL END -->
```

---

## 검증 규칙

### 1. 링크 검증

```bash
# 깨진 링크 검사
grep -r "\[.*\](.*\.md)" docs/ | while read line; do
  # 링크 대상 파일 존재 확인
done
```

### 2. 통계 검증

```bash
# 문서의 통계와 실제 파일 수 비교
doc_controllers=$(grep -c "Controller" docs/PROJECT_STATUS_SUMMARY.md)
actual_controllers=$(find backend -name "*Controller.kt" | wc -l)
```

### 3. 버전 검증

```markdown
문서 버전: 2.0.0
마지막 동기화: 2026-01-05
코드 커밋: abc1234
```

---

## 동기화 주기

| 이벤트 | 동기화 범위 | 자동/수동 |
|-------|-----------|----------|
| `/sync` 명령 | 전체 | 수동 |
| `/done` 명령 | 해당 기능 문서 | 자동 |
| Controller 추가 | API 문서 | 제안 |
| Service 추가 | 아키텍처 문서 | 제안 |
| 주간 리뷰 | 전체 통계 | 권장 |

---

## 충돌 해결

### 우선순위

1. **코드가 항상 우선** (Single Source of Truth)
2. 문서는 코드를 반영하는 View
3. 수동 작성 영역은 보존

### 충돌 시나리오

**시나리오 1: 문서에 있지만 코드에 없음**
```
문서: ConfigurationController.kt
코드: (없음)

→ 조치: "삭제된 파일" 섹션에 기록, 문서에서 제거 제안
```

**시나리오 2: 코드에 있지만 문서에 없음**
```
문서: (없음)
코드: PerformanceController.kt

→ 조치: "신규 파일" 섹션에 추가, 문서 생성 제안
```

**시나리오 3: 내용 불일치**
```
문서: "6개 Controller"
코드: 9개 실제 파일

→ 조치: AUTO-GENERATED 영역 자동 업데이트
```
