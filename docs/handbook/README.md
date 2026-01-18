# ACS 팀 핸드북

> ACS 프로젝트 개발에 필요한 기술 참조 및 패턴 가이드

## 목적

- 프로젝트에서 사용하는 기술 스택의 핵심 패턴 정리
- 새 팀원 온보딩 지원
- 코드 리뷰 시 참조 기준

---

## 문서 구조

```
handbook/
├── README.md           ← 현재 문서
├── overview/           ← 시스템 전체 (시작점)
│   ├── tech-stack.md   # 기술 스택 + 선택 이유
│   ├── data-flow.md    # FE ↔ BE ↔ HW 데이터 흐름
│   └── glossary.md     # 용어 사전
├── kotlin/             ← BE 문법/패턴
├── vue/                ← FE 문법/패턴
└── project/            ← ACS 특화 패턴
```

---

## 문서 목록

### Overview (시스템 전체) - 먼저 읽기 권장
| 문서 | 주제 | 상태 |
|------|------|:----:|
| [tech-stack.md](./overview/tech-stack.md) | 기술 스택 전체 + 왜 선택했는지 | ✅ |
| [data-flow.md](./overview/data-flow.md) | FE ↔ BE ↔ 안테나 데이터 흐름 | ✅ |
| [glossary.md](./overview/glossary.md) | 용어 사전 (약어, 도메인 용어) | ✅ |

### Kotlin (Backend)
| 문서 | 주제 | 상태 |
|------|------|:----:|
| [kotlin-null-safety.md](./kotlin/kotlin-null-safety.md) | Null 안전 처리 | ✅ |
| [kotlin-reactive.md](./kotlin/kotlin-reactive.md) | WebFlux/리액티브 | ✅ |
| [spring-annotations.md](./kotlin/spring-annotations.md) | Spring 어노테이션 | ✅ |

### Vue/TypeScript (Frontend)
| 문서 | 주제 | 상태 |
|------|------|:----:|
| [vue-reactivity.md](./vue/vue-reactivity.md) | 반응형 시스템 | ✅ |
| [vue-composables.md](./vue/vue-composables.md) | Composable 패턴 | 📝 |
| [typescript-patterns.md](./vue/typescript-patterns.md) | TS 패턴 | 📝 |

### 프로젝트 특화
| 문서 | 주제 | 상태 |
|------|------|:----:|
| [acs-patterns.md](./project/acs-patterns.md) | ACS 코드 패턴 | ✅ |

---

## Quick Reference

### Kotlin
```kotlin
?.     // null이면 멈춤
?:     // null이면 우측 값
!!     // null 아님 단언 (위험)
?.let  // null 아닐 때만 실행
```

### Vue
```typescript
ref()       // 반응형 단일 값
reactive()  // 반응형 객체
computed()  // 계산된 값 (캐시)
watch()     // 변경 감지
```

---

## 관리 정책

- **업데이트**: 새로운 패턴 발견 시 추가
- **검토**: 분기별 최신화 확인
- **담당**: 개발팀 전체

---

**상태**: ✅ 완료 | 📝 작성 예정
