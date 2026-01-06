# Concepts - 개념 문서

> 시스템의 핵심 개념과 설계를 설명하는 영구 보존 문서입니다.

## 구조

```
concepts/
├── architecture/       # 시스템 아키텍처
│   ├── SYSTEM_OVERVIEW.md
│   ├── UI_Architecture.md
│   └── Data_Flow.md
│
├── algorithms/         # 알고리즘 설계
│   ├── Satellite_Tracking.md
│   ├── Sun_Tracking.md
│   └── Train_Angle_Calculation.md
│
├── protocols/          # 통신 프로토콜
│   ├── ICD_Protocol.md
│   └── WebSocket_Protocol.md
│
└── domain/             # 도메인 지식
    ├── Antenna_Control_Basics.md
    └── Coordinate_Systems.md
```

## 각 영역 설명

### architecture/

시스템 구조와 아키텍처 문서

- **SYSTEM_OVERVIEW.md**: 전체 시스템 구조
- **UI_Architecture.md**: Frontend 아키텍처
- **Data_Flow.md**: 데이터 흐름

### algorithms/

핵심 알고리즘 설계 문서

- **Satellite_Tracking.md**: 위성 추적 알고리즘
- **Sun_Tracking.md**: 태양 추적 알고리즘
- **Train_Angle_Calculation.md**: Train 각도 계산

### protocols/

통신 프로토콜 명세

- **ICD_Protocol.md**: ICD 하드웨어 통신
- **WebSocket_Protocol.md**: 실시간 데이터 전송

### domain/

도메인 지식 문서

- **Antenna_Control_Basics.md**: 안테나 제어 기초
- **Coordinate_Systems.md**: 좌표계 설명

## 업데이트 정책

- 코드 변경 시 관련 concepts/ 문서 **동기화 필수**
- `/sync` 스킬로 자동 감지 및 업데이트
- 수동 작성 영역은 `<!-- MANUAL: -->` 마커로 보존

## 문서 템플릿

개념 문서는 다음 구조를 따릅니다:

```markdown
# {개념명}

## 개요
{1-2문장 설명}

## 핵심 개념
{상세 설명}

## 구현
{코드 위치 및 예시}

## 관련 문서
{링크}
```
