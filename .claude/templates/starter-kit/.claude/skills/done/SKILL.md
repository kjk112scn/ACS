---
name: done
description: 작업 완료 + 커밋. "완료", "done", "커밋", "마무리" 키워드에 반응.
model: opus
---

# Done - 작업 완료

## 역할

작업 완료 후 커밋까지 자동화합니다.

## 워크플로우

```
[1] 변경사항 확인
    └── git status, git diff

[2] 빌드 확인
    └── npm run build 또는 ./gradlew build

[3] 커밋 메시지 생성
    └── 변경 내용 분석 → 메시지 작성

[4] 커밋 실행
    └── git add + git commit
```

## 커밋 메시지 형식

```
{type}({scope}): {subject}

{body - 변경 내용 요약}

Co-Authored-By: Claude <noreply@anthropic.com>
```

### type 종류

| type | 용도 |
|------|------|
| feat | 새 기능 |
| fix | 버그 수정 |
| refactor | 리팩토링 |
| docs | 문서 |
| chore | 기타 |

## 실행 조건

- 변경사항이 있을 때만 실행
- 빌드 실패 시 커밋 중단
- 사용자 확인 후 진행

## 사용 예시

```
사용자: "/done"

AI:
1. 변경사항 확인... 3개 파일 수정됨
2. 빌드 확인... ✅ 성공
3. 커밋 메시지:
   feat(auth): 로그인 기능 추가

   - LoginForm 컴포넌트 생성
   - useAuth composable 추가
   - API 연동

4. 커밋하시겠습니까? [Y/n]
```
