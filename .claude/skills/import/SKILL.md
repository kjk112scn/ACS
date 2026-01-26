---
name: import
description: claude-dev-kit에서 공통 규칙/스킬 가져오기. "import", "가져오기", "sync from devkit" 키워드에 반응.
model: haiku
---

# Import - 공통 규칙 가져오기 스킬

## 역할

`claude-dev-kit` GitHub 레포에서 공통 규칙/스킬을 현재 프로젝트로 가져옵니다.

## 사용법

```
/import --common      # 공통 규칙만 가져오기
/import bugfix        # 특정 스킬 가져오기
/import --all         # 모든 스킬 가져오기
```

## 워크플로우

```
[1. Pull] → [2. 복사/병합] → [3. 검증] → [4. 완료]
```

## 실행 단계

### Step 1: DevKit 최신화

```bash
cd c:/Users/NG2/source/repos/claude-dev-kit
git pull origin main
```

### Step 2: 파일 복사/병합

**--common 옵션 (공통 규칙):**
| 소스 | 대상 |
|------|------|
| `claude-dev-kit/.claude/rules/common-rules.md` | 현재 프로젝트 `CLAUDE.md` 내 `@common-rules` 섹션 |

> **병합 방식:** `@common-rules` 마커 사이 내용만 교체

**스킬 가져오기:**
| 소스 | 대상 |
|------|------|
| `claude-dev-kit/.claude/skills/{name}/` | 현재 프로젝트 `.claude/skills/{name}/` |

### Step 3: 검증

- 마커 무결성 확인 (`@common-rules: start/end`)
- 문법 오류 없음 확인

### Step 4: 결과 보고

**--common 옵션:**
```
✅ 공통 규칙 가져오기 완료
   소스: claude-dev-kit/.claude/rules/common-rules.md
   대상: CLAUDE.md @common-rules 섹션
   변경: 15줄 업데이트
```

**스킬 가져오기:**
```
✅ bugfix 스킬 가져오기 완료
   소스: claude-dev-kit
   파일: 3개 복사됨
```

## 경로

| 항목 | 경로 |
|------|------|
| DevKit | `C:\Users\NG2\source\repos\claude-dev-kit\` |
| 공통 규칙 | `.claude/rules/common-rules.md` |
| 스킬 | `.claude/skills/` |

---

**스킬 버전:** 1.0.0