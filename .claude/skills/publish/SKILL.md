---
name: publish
description: 스킬/템플릿을 claude-dev-kit 레포에 동기화. "publish", "동기화", "sync devkit" 키워드에 반응.
model: haiku
---

# Publish - 스킬 배포 스킬

## 역할

ACS 프로젝트의 스킬/템플릿을 `claude-dev-kit` GitHub 레포에 동기화합니다.

## 사용법

```
/publish bugfix       # 특정 스킬만
/publish --all        # 모든 스킬
```

## 워크플로우

```
[1. 복사] → [2. 커밋] → [3. 푸시] → [4. 완료]
```

## 실행 단계

### Step 1: 파일 복사

| 소스 | 대상 |
|------|------|
| `ACS/.claude/skills/{name}/` | `claude-dev-kit/.claude/skills/{name}/` |

### Step 2: Git 커밋 & 푸시

```bash
cd c:/Users/NG2/source/repos/claude-dev-kit
git add -A
git commit -m "sync: {skill-name} skill update"
git push origin main
```

### Step 3: 결과 보고

```
✅ bugfix 스킬 동기화 완료
   커밋: abc1234
   URL: https://github.com/kjk112scn/claude-dev-kit
```

## 경로

| 항목 | 경로 |
|------|------|
| ACS 스킬 | `C:\Users\NG2\source\repos\VueQuasar\ACS\.claude\skills\` |
| DevKit | `C:\Users\NG2\source\repos\claude-dev-kit\.claude\skills\` |

---

**스킬 버전:** 1.0.0