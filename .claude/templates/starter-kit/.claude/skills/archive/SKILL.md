---
name: archive
description: 완료된 작업 폴더 아카이브. /done에서 자동 호출 또는 수동 실행. "아카이브", "archive", "정리" 키워드에 반응.
model: haiku
---

# Archive - 문서 아카이브 스킬

## 역할

완료된 작업 폴더를 `docs/work/archive/`로 이동합니다.

**호출 방식:**
- **자동**: `/done` 실행 시 완료 감지되면 제안
- **수동**: `/archive {폴더명}` 직접 실행

---

## 완료 감지 조건

PROGRESS.md 또는 README.md 파싱:

| 조건 | 체크 방법 |
|------|----------|
| 진행률 95%+ | `진행률: (\d+)%` 파싱 |
| "완료" 키워드 | `✅ 완료`, `상태: 완료`, `Completed` |

---

## 워크플로우

```
[1. 스캔] → [2. 감지] → [3. 확인] → [4. 이동] → [5. 업데이트]
     │           │           │           │           │
  active/    PROGRESS.md   사용자      archive/    README.md
  폴더 목록   파싱         확인 요청   이동        업데이트
```

---

## 실행

### 자동 모드 (/done에서 호출)

```
📦 아카이브 후보 발견

| 폴더 | 진행률 | 마지막 수정 |
|------|--------|------------|
| Feature_Login | 100% | 2026-01-22 |

아카이브할까요? [y/n]
```

### 수동 모드

```
사용자: /archive Feature_Login

→ Feature_Login 폴더 확인
→ archive/로 이동
→ 완료 보고
```

---

## 아카이브 실행

```bash
# 폴더 이동
git mv docs/work/active/{Feature} docs/work/archive/{Feature}
```

**아카이브 후 처리:**
- CURRENT_STATUS.md에서 해당 작업 제거
- archive/README.md에 기록 추가

---

## 완료 보고

```
✅ 아카이브 완료

| 항목 | 결과 |
|------|------|
| 이동 | docs/work/archive/Feature_Login |
| CURRENT_STATUS | 업데이트됨 |

💡 부활 필요 시: /revive Login
```

---

## 관련 스킬

- `/done` - 아카이브 자동 호출
- `/revive` - 아카이브 부활

---

**스킬 버전:** 1.0.0