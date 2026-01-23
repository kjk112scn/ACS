---
name: revive
description: 아카이브 문서 부활. 버그/기능 작업 시 관련 과거 문서 검색 및 복구. "부활", "revive", "되살리기" 키워드에 반응.
model: haiku
---

# Revive - 문서 부활 스킬

## 역할

아카이브된 작업 문서를 검색하고 `docs/work/active/`로 복구합니다.

**호출 방식:**
- **자동**: `/bugfix`, `/feature` 실행 시 관련 문서 자동 검색
- **수동**: `/revive {키워드}` 직접 검색

---

## 워크플로우

```
[1. 검색] → [2. 매칭] → [3. 확인] → [4. 부활] → [5. 준비]
     │           │           │           │           │
  키워드      archive/    사용자      active/     상태 변경
  추출        검색        확인 요청   이동        "진행 중"
```

---

## 사용법

### 수동 실행

```
/revive Login           # 키워드 검색
/revive --list          # 전체 아카이브 목록
/revive --recent        # 최근 5개
```

### 자동 실행 (/bugfix, /feature 연계)

```
사용자: "로그인 버그 수정해줘"

→ /bugfix 스킬 시작
→ [자동] archive/에서 "로그인" 검색
→ 관련 문서 발견:
   - Bugfix_Login_Token (2026-01)
→ "과거 문서 있습니다. 참고할까요?" [y/n]
```

---

## 검색 결과 표시

```
🔍 "Login" 검색 결과 (archive/)

| # | 폴더 | 내용 | 아카이브 날짜 |
|---|------|------|--------------|
| 1 | Bugfix_Login_Token | 토큰 만료 버그 | 2026-01-15 |
| 2 | Feature_Login_OAuth | OAuth 연동 | 2026-01-10 |

선택하세요: [1/2/n/참조만]
```

---

## 부활 옵션

| 옵션 | 동작 |
|------|------|
| **부활** | archive/ → active/ 이동, 상태 "진행 중"으로 |
| **참조만** | 이동 안 함, 내용만 요약 표시 |
| **취소** | 새로 시작 |

---

## 부활 실행

```bash
# 폴더 이동
git mv docs/work/archive/{Feature} docs/work/active/{Feature}
```

**부활 후 처리:**
- README.md: 상태 → "🚧 재작업"
- PROGRESS.md: 부활 이력 추가
- CURRENT_STATUS.md: 진행 중 섹션에 추가

---

## 부활 완료 보고

```
✅ 부활 완료

| 항목 | 내용 |
|------|------|
| 폴더 | docs/work/active/Bugfix_Login_Token |
| 상태 | 🚧 재작업 |
| 과거 기록 | ANALYSIS.md, FIX.md 참조 가능 |

💡 과거 원인 분석 참고하여 작업하세요.
```

---

## 관련 스킬

- `/archive` - 완료 후 아카이브
- `/bugfix` - 자동으로 revive 호출
- `/feature` - 자동으로 revive 호출

---

**스킬 버전:** 1.0.0