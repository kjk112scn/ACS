# Logs - 일일 작업 로그

> 날짜별 작업 기록

---

## 파일 구조

```
logs/
├── README.md
└── YYYY-MM-DD.md    # 일일 로그
```

---

## 로그 생성 시점

`/done` 실행 시 자동 생성/업데이트

---

## 로그 형식

```markdown
# YYYY-MM-DD 작업 로그

## 완료된 작업

### {작업명}
- **유형:** Feature / Bugfix / Refactor
- **영향:** Frontend / Backend / Both
- **변경 파일:**
  - `path/to/file.ts` - {변경 내용}

### 상세 내용
{작업 상세 설명}
```

---

## 용도

- **세션 연속성:** 이전 작업 빠른 파악
- **CHANGELOG 참조:** 버전 기록 시 참고
- **디버깅:** 언제 무엇이 변경되었는지 추적