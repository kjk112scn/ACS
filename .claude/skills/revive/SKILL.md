---
name: revive
description: 아카이브 문서 부활. 버그픽스/기능개발 시 관련 과거 문서 검색 및 재활성화. "부활", "revive", "되살리기", "다시" 키워드에 반응.
model: haiku
---

# Revive - 문서 부활 스킬

> 아카이브된 작업 문서를 검색하고 `docs/work/active/`로 복구

## 역할

- **수동 모드**: `/revive {키워드}` - 키워드로 검색 후 부활
- **자동 모드**: `/bugfix`, `/feature` 실행 시 관련 문서 자동 검색

## 워크플로우

```
[1. 검색] → [2. 매칭] → [3. 확인] → [4. 부활] → [5. 준비]
     │           │           │           │           │
  키워드      archive/    사용자      active/     상태 변경
  추출        검색        확인 요청   이동        "진행 중"
```

## 사용법

### 수동 실행

```
/revive PassSchedule        # 키워드 검색
/revive --list              # 전체 아카이브 목록
/revive --recent            # 최근 아카이브 5개
```

### 자동 실행 (bugfix/feature 연계)

```
사용자: "PassSchedule 차트 버그 수정해줘"

→ /bugfix 스킬 시작
→ [자동] /revive PassSchedule 호출
→ 관련 아카이브 발견:
   - PassSchedule_RowColors_Bugfix (2026-01)
   - PassScheduleService_Improvement (2026-01)
→ "과거 작업 기록 있습니다. 참고할까요?" [y/n]
→ y: 해당 문서 부활 또는 참조용으로 표시
```

## 실행 단계

### Step 1: 아카이브 검색

```bash
# archive/ 폴더에서 키워드 검색
# (deny 규칙 우회 - 스킬 특권)

ls docs/work/archive/ | grep -i "{키워드}"
```

### Step 2: 매칭 결과 표시

```
🔍 "{키워드}" 검색 결과 (archive/)

| 폴더 | 내용 | 아카이브 날짜 |
|------|------|--------------|
| PassSchedule_RowColors_Bugfix | 행 색상 무한루프 수정 | 2026-01-15 |
| PassScheduleService_Improvement | 서비스 로직 개선 | 2026-01-10 |

선택하세요: [1/2/n/참조만]
```

### Step 3: 부활 옵션

| 옵션 | 동작 |
|------|------|
| **부활** | archive/ → active/ 이동, 상태를 "진행 중"으로 변경 |
| **참조만** | archive/에 유지, 관련 내용 요약 표시 |
| **취소** | 새 폴더로 작업 시작 |

### Step 4: 부활 실행

```bash
# 1. 폴더 이동
mv docs/work/archive/{폴더명} docs/work/active/{폴더명}

# 2. README.md 상태 변경
# "상태: ✅ 완료" → "상태: 🚧 재작업"

# 3. PROGRESS.md에 부활 기록 추가
echo "## 부활 이력" >> PROGRESS.md
echo "- 2026-01-22: 관련 버그 수정으로 재활성화" >> PROGRESS.md

# 4. active/README.md 목록에 추가
```

### Step 5: 준비 완료 보고

```
✅ 부활 완료

| 항목 | 내용 |
|------|------|
| 폴더 | docs/work/active/PassSchedule_RowColors_Bugfix |
| 상태 | 🚧 재작업 |
| 과거 기록 | ANALYSIS.md, FIX.md 참조 가능 |

💡 과거 원인 분석 참고하여 작업 진행하세요.
```

## /bugfix 연계

`/bugfix` 스킬 Step 1에서 자동 호출:

```yaml
# bugfix 스킬 내부
Step 0: 관련 아카이브 검색 (NEW)
  - 사용자 요청에서 키워드 추출
  - revive 스킬 호출 (자동 모드)
  - 매칭 시 참조 여부 질문
```

## /feature 연계

`/feature` 스킬 Step 1에서 자동 호출:

```yaml
# feature 스킬 내부
Step 0: 관련 아카이브 검색 (NEW)
  - 유사 기능 과거 작업 검색
  - 재활용 가능한 설계 문서 탐색
```

## 참조

- **아카이브 스킬**: [../archive/SKILL.md](../archive/SKILL.md)
- **버그픽스 스킬**: [../bugfix/SKILL.md](../bugfix/SKILL.md)
- **기능 스킬**: [../feature/SKILL.md](../feature/SKILL.md)
- **아카이브 목록**: `docs/work/archive/README.md`

## 트러블슈팅

### Q: 검색 결과가 없어요
A: 키워드를 짧게 또는 다르게 시도. `/revive --list`로 전체 목록 확인.

### Q: 부활 후 상태가 이상해요
A: PROGRESS.md 확인 후 수동으로 상태 수정.

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-22