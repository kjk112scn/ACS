# Hardware Error Messages (하드웨어 에러 메시지 관리)

> ACS 시스템의 하드웨어 에러 메시지 정의 및 관리 가이드

---

## 개요

ACS 시스템은 실시간으로 하드웨어 상태를 모니터링하고, 비정상 상태 발생 시 에러 로그와 알림을 생성합니다. 이 문서는 에러 메시지의 정의 위치, 구조, 그리고 활성화/비활성화 방법을 설명합니다.

---

## 에러 메시지 정의 위치

### Backend (메시지 생성)

**파일**: `backend/src/main/kotlin/com/gtlsystems/acs_api/service/hardware/HardwareErrorLogService.kt`

이 파일에서 모든 하드웨어 에러 메시지를 정의하고 관리합니다.

### Frontend (메시지 번역)

**파일**: `frontend/src/i18n/ko-KR/index.ts`, `frontend/src/i18n/en-US/index.ts`

`hardwareErrors` 섹션에서 에러 키에 대한 다국어 메시지를 정의합니다.

---

## 에러 메시지 구조

### ErrorConfig 데이터 클래스

```kotlin
data class ErrorConfig(
    val category: String,      // 카테고리 (예: SERVO_POWER, POSITIONER, STOW)
    val severity: String,      // 심각도 (ERROR, WARNING, INFO)
    val errorKey: String,      // 에러 키 (i18n 키와 매핑)
    val description: String    // 설명
)
```

### 비트 매핑 구조

각 하드웨어 보드의 상태는 비트 플래그로 전달되며, `HardwareErrorLogService.kt`의 `bitFieldMappings`에서 비트 인덱스와 에러 설정을 매핑합니다.

**예시**:
```kotlin
"azimuthBoardServoStatusBits" -> {
    mapOf(
        0 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM_CODE1", "Azimuth Servo Alarm Code 1"),
        5 to ErrorConfig("SERVO_POWER", "ERROR", "AZIMUTH_SERVO_ALARM", "Azimuth Servo Alarm"),
        6 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_BRAKE_ENGAGED", "Azimuth Servo Brake")
    )
}
```

---

## 카테고리 및 심각도

### 카테고리 (Category)

| 카테고리 | 설명 |
|---------|------|
| `POWER` | 전원 관련 (메인 전원, 서보 전원) |
| `SERVO_POWER` | 서보 모터 전원 및 알람 |
| `POSITIONER` | 포지셔너 (리미트 스위치, 엔코더) |
| `STOW` | 스토우 핀 |
| `EMERGENCY` | 비상 정지 |
| `PROTOCOL` | 통신 프로토콜 에러 |
| `FEED` | 피드 관련 |
| `SYSTEM` | 시스템 예약 |
| `TEST` | 테스트용 |

### 심각도 (Severity)

| 심각도 | 색상 | 설명 |
|--------|------|------|
| `CRITICAL` | 빨강 | 즉시 조치 필요 (시스템 정지) |
| `ERROR` | 주황 | 에러 (기능 동작 불가) |
| `WARNING` | 노랑 | 경고 (주의 필요) |
| `INFO` | 파랑 | 정보성 메시지 |

---

## 메시지 활성화/비활성화

### 알림 비활성화 방법

특정 에러 메시지의 알림을 비활성화하려면 `HardwareErrorLogService.kt`에서 해당 라인을 **주석처리**합니다.

**예시**: 서보 모터 켜짐/꺼짐 알림 비활성화

```kotlin
"azimuthBoardServoStatusBits" -> {
    mapOf(
        // ... 다른 에러들 ...
        6 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_BRAKE_ENGAGED", "Azimuth Servo Brake")
        // 7 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_MOTOR_ON", "Azimuth Servo Motor") // 알림 비활성화
    )
}
```

### 알림 재활성화 방법

주석을 제거하고 다시 빌드합니다.

```kotlin
7 to ErrorConfig("SERVO_POWER", "INFO", "AZIMUTH_SERVO_MOTOR_ON", "Azimuth Servo Motor")
```

---

## 현재 비활성화된 메시지

다음 메시지들은 너무 자주 발생하여 알림이 비활성화되었습니다:

| 에러 키 | 위치 | 사유 |
|---------|------|------|
| `AZIMUTH_SERVO_MOTOR_ON` | `azimuthBoardServoStatusBits` 비트 7 | 서보 모터 켜짐/꺼짐 알림 과다 |
| `ELEVATION_SERVO_MOTOR_ON` | `elevationBoardServoStatusBits` 비트 7 | 서보 모터 켜짐/꺼짐 알림 과다 |
| `TRAIN_SERVO_MOTOR_ON` | `trainBoardServoStatusBits` 비트 7 | 서보 모터 켜짐/꺼짐 알림 과다 |

**파일**: `HardwareErrorLogService.kt` 라인 201, 224, 252

---

## 다국어 메시지 추가

### 1. 백엔드에 에러 정의 추가

`HardwareErrorLogService.kt`에 새 에러 설정 추가:

```kotlin
8 to ErrorConfig("SERVO_POWER", "WARNING", "AZIMUTH_SERVO_OVERHEATING", "Azimuth Servo Overheating")
```

### 2. 프론트엔드 i18n에 번역 추가

`frontend/src/i18n/ko-KR/index.ts`:

```typescript
hardwareErrors: {
  // ... 기존 메시지들 ...
  AZIMUTH_SERVO_OVERHEATING: 'Azimuth 서보 과열',
  AZIMUTH_SERVO_OVERHEATING_RESOLVED: 'Azimuth 서보 과열 해제',
}
```

`frontend/src/i18n/en-US/index.ts`:

```typescript
hardwareErrors: {
  // ... 기존 메시지들 ...
  AZIMUTH_SERVO_OVERHEATING: 'Azimuth Servo Overheating',
  AZIMUTH_SERVO_OVERHEATING_RESOLVED: 'Azimuth Servo Overheating Resolved',
}
```

---

## 에러 로그 흐름

```
[펌웨어]
  ↓ 비트 플래그 (UDP)
[Backend - ICDService]
  ↓ 비트 파싱
[Backend - HardwareErrorLogService]
  ↓ ErrorConfig 매핑
[Backend - WebSocket]
  ↓ 에러 로그 전송
[Frontend - icdStore]
  ↓ 에러 수신
[Frontend - hardwareErrorLogStore]
  ↓ i18n 번역
[Frontend - UI]
  ↓ 알림 표시 + 로그 패널
[사용자]
```

---

## 주요 비트 필드 목록

| 필드명 | 축 | 설명 |
|--------|-----|------|
| `mainBoardStatusBits` | 공통 | 메인보드 상태 (전원, 비상정지) |
| `azimuthBoardServoStatusBits` | Azimuth | 서보 알람, 브레이크, 모터 |
| `azimuthBoardStatusBits` | Azimuth | 리미트 스위치, 스토우 핀, 엔코더 |
| `elevationBoardServoStatusBits` | Elevation | 서보 알람, 브레이크, 모터 |
| `elevationBoardStatusBits` | Elevation | 리미트 스위치, 스토우 핀, 엔코더 |
| `trainBoardServoStatusBits` | Train | 서보 알람, 브레이크, 모터 |
| `trainBoardStatusBits` | Train | 리미트 스위치, 스토우 핀, 엔코더 |
| `feedBoardStatusBits` | Feed | 피드 상태 |

---

## 디버깅 팁

### 에러 메시지가 표시되지 않는 경우

1. **백엔드 로그 확인**:
   ```bash
   # HardwareErrorLogService에서 에러 생성 확인
   grep "Hardware error detected" backend/logs/application.log
   ```

2. **i18n 키 확인**:
   - 브라우저 개발자 도구 콘솔에서 번역 실패 경고 확인
   - `🚨 에러 메시지 번역 실패` 메시지 검색

3. **비트 매핑 확인**:
   - `HardwareErrorLogService.kt`에서 올바른 비트 인덱스 매핑 확인
   - ICDService의 enum 순서와 일치하는지 확인

### 알림이 과도하게 발생하는 경우

- 해당 메시지를 주석처리하여 비활성화
- 또는 심각도를 `INFO`에서 다른 레벨로 변경 고려

---

## 참고 문서

- [System Overview](../architecture/SYSTEM_OVERVIEW.md) - 전체 시스템 구조
- [API Reference](./README.md) - WebSocket API 명세
- [Development Guide](../guides/Development_Guide.md) - 개발 가이드

---

**문서 버전**: 1.0.0
**작성일**: 2024-12-23
**최종 수정**: 2024-12-23
