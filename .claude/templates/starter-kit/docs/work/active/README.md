# Active Work - 진행 중 작업

> 현재 진행 중인 작업 폴더들

---

## 폴더 구조

```
active/
└── {Category}_{Name}/
    ├── README.md      # 개요, 요구사항
    ├── PROGRESS.md    # 진행률, 체크리스트
    ├── DESIGN.md      # (선택) 설계 문서
    └── FIX.md         # (Bugfix만) 수정 계획
```

---

## 폴더명 규칙

| 접두어 | 용도 |
|--------|------|
| `Feature_` | 새 기능 |
| `Bugfix_` | 버그 수정 |
| `Refactor_` | 리팩토링 |
| `Optimize_` | 성능 최적화 |

**예시:** `Feature_User_Login`, `Bugfix_Token_Expire`

---

## 워크플로우

1. `/feature` 또는 `/bugfix` 실행 → 폴더 자동 생성
2. 작업 진행 → PROGRESS.md 업데이트
3. 완료 → `/done` 실행
4. 100% 완료 → `/archive`로 이동

---

## 현재 진행 중

*(자동 업데이트됨 - CURRENT_STATUS.md 참조)*