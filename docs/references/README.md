# ACS References - 참조 문서

> 프로젝트 참조 문서 및 기술 문서 모음

## 문서 구조

```
references/
├── architecture/      # 시스템 아키텍처
├── algorithms/        # 알고리즘 상세
├── protocols/         # 통신 프로토콜
├── api/              # API 명세
├── development/      # 개발 가이드
├── deployment/       # 배포 가이드
└── user-guide/       # 사용자 가이드
```

## 핵심 문서

### 아키텍처

| 문서 | 설명 |
|------|------|
| [SYSTEM_OVERVIEW.md](architecture/SYSTEM_OVERVIEW.md) | 시스템 통합 개요 (FW↔BE↔FE) |
| [UI_Architecture.md](architecture/UI_Architecture.md) | 프론트엔드 UI 아키텍처 |

### 알고리즘

| 문서 | 설명 |
|------|------|
| [EphemerisService.md](algorithms/EphemerisService.md) | 위성 궤도 추적 서비스 (5,000줄) |
| [Train_Angle_Algorithm.md](algorithms/Train_Angle_Algorithm.md) | Train 각도 계산 알고리즘 |
| [Antenna_Structure_And_Train_Angle_Concept.md](algorithms/Antenna_Structure_And_Train_Angle_Concept.md) | 안테나 구조 및 Train 각도 개념 |

### 프로토콜

| 문서 | 설명 |
|------|------|
| [ICDService.md](protocols/ICDService.md) | 펌웨어 ICD 프로토콜 (2,800줄) |

### 개발

| 문서 | 설명 |
|------|------|
| [Development_Guide.md](development/Development_Guide.md) | 개발 가이드라인 |
| [Coding_Standards.md](development/Coding_Standards.md) | 코딩 표준 |
| [Settings_Development_Guide.md](development/Settings_Development_Guide.md) | 설정 개발 가이드 |
| [Keyhole_Processing_Logic.md](development/Keyhole_Processing_Logic.md) | Keyhole 처리 로직 |
| [ACS_Development_Lifecycle_System.md](development/ACS_Development_Lifecycle_System.md) | 개발 라이프사이클 |

### 프로젝트 현황

| 문서 | 설명 |
|------|------|
| [PROJECT_STATUS_CURRENT.md](PROJECT_STATUS_CURRENT.md) | **현재 프로젝트 상태 (최신)** - Claude Code v2.0.0 반영 |
| [PROJECT_STATUS_SUMMARY.md](PROJECT_STATUS_SUMMARY.md) | 프로젝트 현황 요약 (레거시) |
| [Hardware_Error_Messages.md](Hardware_Error_Messages.md) | 하드웨어 에러 메시지 관리 |

## 하위 폴더

- [architecture/](architecture/README.md) - 시스템 아키텍처 문서
- [algorithms/](algorithms/README.md) - 알고리즘 상세 문서
- [protocols/](protocols/README.md) - 통신 프로토콜 문서
- [api/](api/README.md) - API 명세
- [deployment/](deployment/README.md) - 배포 가이드
- [development/](development/Development_Guide.md) - 개발 가이드

## 관련 문서

- [docs/README.md](../README.md) - 문서 시스템 개요
- [docs/guides/](../guides/) - 실용 가이드
- [docs/daily/](../daily/) - 일일 작업 로그

---

**최종 업데이트**: 2026-01-07
