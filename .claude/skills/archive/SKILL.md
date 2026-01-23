---
name: archive
description: 완료된 작업 폴더 아카이브. 수동 실행 또는 /done에서 자동 호출. "아카이브", "archive", "정리", "보관" 키워드에 반응.
model: haiku
---

# Archive - 문서 아카이브 스킬

> 완료된 작업 폴더를 `docs/work/archive/`로 이동

## 역할

- **수동 모드**: `/archive {폴더명}` - 지정 폴더 즉시 아카이브
- **자동 모드**: `/done` 실행 시 완료 감지되면 자동 호출

## 완료 감지 조건

PROGRESS.md 파싱하여 다음 조건 확인:

| 조건 | 체크 방법 |
|------|----------|
| 진행률 95% 이상 | `진행률: (\d+)%` 파싱 |
| "완료" 키워드 | `✅ 완료`, `상태: 완료`, `Completed` |
| 대기 상태 없음 | `⏳`, `대기`, `보류` 없음 |

## 워크플로우

```
[1. 스캔] → [2. 감지] → [3. 확인] → [4. 이동] → [5. 업데이트]
     │           │           │           │           │
  active/    PROGRESS.md   사용자      archive/    README.md
  폴더 목록   파싱         확인 요청   이동        업데이트
```

## 실행 단계

### Step 1: 아카이브 후보 스캔

```bash
# docs/work/active/ 하위 폴더 순회
for dir in docs/work/active/*/; do
  # PROGRESS.md 존재 확인
  if [ -f "$dir/PROGRESS.md" ]; then
    # 완료 상태 체크
    check_completion "$dir"
  fi
done
```

### Step 2: 사용자 확인

```
📦 아카이브 후보 발견

| 폴더 | 진행률 | 마지막 수정 |
|------|--------|------------|
| Admin_Panel_Separation | 100% | 2026-01-20 |

아카이브하시겠습니까? [y/n/선택]
```

### Step 3: 아카이브 실행

```bash
# 1. 폴더 이동
mv docs/work/active/{폴더명} docs/work/archive/{폴더명}

# 2. archive/README.md 업데이트 (목록에 추가)

# 3. active/README.md 업데이트 (목록에서 제거)
```

### Step 4: 완료 보고

```
✅ 아카이브 완료

| 작업 | 결과 |
|------|------|
| 폴더 이동 | docs/work/archive/Admin_Panel_Separation |
| README 업데이트 | ✅ |

💡 부활 필요 시: /revive Admin_Panel
```

## 수동 사용 예시

```
사용자: /archive Admin_Panel_Separation

→ Admin_Panel_Separation 폴더 확인
→ archive/로 이동
→ README.md 업데이트
→ 완료 보고
```

## /done 연계

`/done` 스킬 Step 5에서 자동 호출:

```yaml
# done 스킬 내부
Step 5: 아카이브 검토
  - archive 스킬 호출 (자동 모드)
  - 완료된 작업 감지 시 제안
```

## 참조

- **부활 스킬**: [../revive/SKILL.md](../revive/SKILL.md)
- **완료 스킬**: [../done/SKILL.md](../done/SKILL.md)
- **아카이브 폴더**: `docs/work/archive/README.md`

---

**스킬 버전:** 1.0.0
**작성일:** 2026-01-22
