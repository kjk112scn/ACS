# Settings Registry

> **최종 업데이트**: 2026-01-18
> **총 설정 수**: 57개

---

## 현황 요약

```
✅ 정상 연동: 37개 (65%)  ← FE → BE → 로직 모두 연결
⏸️ 보류:      1개 (2%)   ← 역할 불명확, 검토 필요
🔧 미구현:   12개 (21%)  ← FE/BE 존재, 기능 자체가 없음
❌ 미사용:    7개 (12%)  ← FE/BE 존재, 로직에서 안 씀
```

---

## 상태 범례

| 상태 | 의미 | FE UI | BE 저장 | 로직 사용 | 조치 |
|:----:|------|:-----:|:------:|:--------:|------|
| ✅ | 정상 연동 | ✅ | ✅ | ✅ | 유지 |
| ⏸️ | 보류 | ✅ | ✅ | ❓ | 역할 정의 후 결정 |
| 🔧 | 미구현 | ✅ | ✅ | ❌ | **기능 구현 시** 연결 |
| ❌ | 미사용 | ✅ | ✅ | ❌ | 삭제 검토 또는 연결 |
| 🆕 | 신규 추가 | ✅ | ✅ | 🆕 | 연동 필요 |

### 🔧 미구현 vs ❌ 미사용 차이

| 구분 | 🔧 미구현 | ❌ 미사용 |
|------|----------|----------|
| **원인** | 기능 자체가 없음 | 기능은 있으나 연결 안됨 |
| **예시** | Stow 모드, SpeedLimits | stepsizelimit, system.jvm |
| **사용자 영향** | 값 바꿔도 효과 없음 | 값 바꿔도 효과 없음 |
| **조치** | 기능 개발 시 연결 | 삭제하거나 로직에 연결 |

---

## 1. Location (위치) - 3개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `location.latitude` | 위도 | 36.38 | SunTrackService, TrackingService | ✅ |
| `location.longitude` | 경도 | 127.36 | SunTrackService, TrackingService | ✅ |
| `location.altitude` | 고도 | 100.0 | SunTrackService, TrackingService | ✅ |

---

## 2. Tracking (추적) - 3개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `tracking.msInterval` | 추적 간격 (ms) | 100 | TrackingService (WebSocket) | ✅ |
| `tracking.durationDays` | 추적 기간 (일) | 1 | EphemerisService, PassScheduleService | ✅ |
| `tracking.minElevationAngle` | 최소 고도각 | 0.0 | - | ⏸️ |

> **tracking.minElevationAngle 보류 사유**: `sourceMinElevationAngle`과 역할 구분 필요

---

## 3. Stow (스토우) - 6개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `stow.angle.azimuth` | Stow 방위각 | 0.0 | - | 🔧 |
| `stow.angle.elevation` | Stow 고도각 | 90.0 | - | 🔧 |
| `stow.angle.train` | Stow Train각 | 0.0 | - | 🔧 |
| `stow.speed.azimuth` | Stow 방위각 속도 | 1.0 | - | 🔧 |
| `stow.speed.elevation` | Stow 고도각 속도 | 1.0 | - | 🔧 |
| `stow.speed.train` | Stow Train각 속도 | 1.0 | - | 🔧 |

> **미구현**: Stow 모드 개발 시 연결 예정

---

## 4. AntennaSpec (안테나 사양) - 2개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `antennaspec.trueNorthOffsetAngle` | True North Offset | 0.0 | TrackingService | ✅ |
| `antennaspec.tiltAngle` | Tilt 각도 | -7.0 | TrackingService (좌표변환) | ✅ |

---

## 5. AngleLimits (각도 한계) - 6개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `anglelimits.azimuthMin` | Az 최소각 | -270.0 | - | ❌ |
| `anglelimits.azimuthMax` | Az 최대각 | 270.0 | - | ❌ |
| `anglelimits.elevationMin` | El 최소각 | 0.0 | SatelliteService (Pass 필터) | ✅ |
| `anglelimits.elevationMax` | El 최대각 | 90.0 | - | ❌ |
| `anglelimits.trainMin` | Train 최소각 | -180.0 | - | ❌ |
| `anglelimits.trainMax` | Train 최대각 | 180.0 | - | ❌ |

> **권장**: 명령 전송 전 한계 체크 로직에 적용

---

## 6. SpeedLimits (속도 한계) - 6개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `speedlimits.azimuthMin` | Az 최소속도 | 0.1 | - | 🔧 |
| `speedlimits.azimuthMax` | Az 최대속도 | 10.0 | - | 🔧 |
| `speedlimits.elevationMin` | El 최소속도 | 0.1 | - | 🔧 |
| `speedlimits.elevationMax` | El 최대속도 | 10.0 | - | 🔧 |
| `speedlimits.trainMin` | Train 최소속도 | 0.1 | - | 🔧 |
| `speedlimits.trainMax` | Train 최대속도 | 10.0 | - | 🔧 |

> **미구현**: ICD 모터 명령 구현 시 연결 예정

---

## 7. AngleOffsetLimits (오프셋 한계) - 3개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `angleoffsetlimits.azimuth` | Az 오프셋 제한 | 5.0 | - | ❌ |
| `angleoffsetlimits.elevation` | El 오프셋 제한 | 5.0 | - | ❌ |
| `angleoffsetlimits.train` | Train 오프셋 제한 | 5.0 | - | ❌ |

---

## 8. TimeOffsetLimits (시간 오프셋) - 2개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `timeoffsetlimits.min` | 최소값 | -10.0 | - | ❌ |
| `timeoffsetlimits.max` | 최대값 | 10.0 | - | ❌ |

---

## 9. Algorithm (알고리즘) - 1개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `algorithm.geoMinMotion` | Geo Min Motion | 0.001 | - | ❌ |

---

## 10. StepSizeLimit (스텝 크기) - 2개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `stepsizelimit.min` | 스텝 최소값 | 0.01 | - | ❌ |
| `stepsizelimit.max` | 스텝 최대값 | 10.0 | - | ❌ |

---

## 11. System.UDP (UDP 설정) - 6개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.udp.receiveInterval` | 수신 간격 | 100 | IcdUdpService | ✅ |
| `system.udp.sendInterval` | 전송 간격 | 100 | IcdUdpService | ✅ |
| `system.udp.timeout` | 타임아웃 | 5000 | IcdUdpService | ✅ |
| `system.udp.reconnectInterval` | 재연결 간격 | 3000 | IcdUdpService | ✅ |
| `system.udp.maxBufferSize` | 최대 버퍼 | 1024 | IcdUdpService | ✅ |
| `system.udp.commandDelay` | 명령 지연 | 50 | IcdUdpService | ✅ |

---

## 12. System.Tracking (시스템 추적) - 4개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.tracking.interval` | 추적 간격 | 100 | TrackingService | ✅ |
| `system.tracking.fineInterval` | 정밀 간격 | 10 | - | ❌ |
| `system.tracking.coarseInterval` | 일반 간격 | 1000 | - | ❌ |
| `system.tracking.stabilizationTimeout` | 안정화 타임아웃 | 5000 | SunTrackService | ✅ |

---

## 13. System.WebSocket - 1개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.websocket.transmissionInterval` | 전송 간격 | 100 | WebSocketService | ✅ |

---

## 14. System.Performance (성능) - 7개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.performance.threshold` | 임계값 | 100 | - | ❌ |
| `system.performance.ultraCores` | ULTRA 코어 | 16 | PerformanceService | ✅ |
| `system.performance.highCores` | HIGH 코어 | 8 | PerformanceService | ✅ |
| `system.performance.mediumCores` | MEDIUM 코어 | 4 | PerformanceService | ✅ |
| `system.performance.ultraMemory` | ULTRA 메모리 | 32 | PerformanceService | ✅ |
| `system.performance.highMemory` | HIGH 메모리 | 16 | PerformanceService | ✅ |
| `system.performance.mediumMemory` | MEDIUM 메모리 | 8 | PerformanceService | ✅ |

---

## 15. System.Storage (저장) - 3개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.storage.batchSize` | 배치 크기 | 100 | BatchStorageManager | ✅ |
| `system.storage.saveInterval` | 저장 간격 | 1000 | BatchStorageManager | ✅ |
| `system.storage.progressLogInterval` | 로그 간격 | 10 | BatchStorageManager | ✅ |

---

## 16. System.SunTrack (태양 추적) - 4개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.suntrack.highAccuracyThreshold` | 높은 정확도 | 0.1 | SunTrackService | ✅ |
| `system.suntrack.mediumAccuracyThreshold` | 중간 정확도 | 0.5 | SunTrackService | ✅ |
| `system.suntrack.lowAccuracyThreshold` | 낮은 정확도 | 1.0 | SunTrackService | ✅ |
| `system.suntrack.searchHours` | 검색 시간 | 12.0 | SunTrackService | ✅ |

---

## 17. System.JVM (JVM 설정) - 4개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `system.jvm.gcPause` | GC 일시정지 | 200 | - | ❌ |
| `system.jvm.heapRegionSize` | 힙 영역 크기 | 16 | - | ❌ |
| `system.jvm.concurrentThreads` | 동시 스레드 | 4 | - | ❌ |
| `system.jvm.parallelThreads` | 병렬 스레드 | 4 | - | ❌ |

---

## 18. Ephemeris.Tracking - 2개

| 키 | 설명 | 기본값 | 사용 위치 | 상태 |
|----|------|--------|----------|:----:|
| `ephemeris.tracking.sourceMinElevationAngle` | 원본 최소 El | 0.0 | EphemerisService, PassScheduleService | ✅ |
| `ephemeris.tracking.keyholeAzimuthVelocityThreshold` | Keyhole 임계값 | 5.0 | EphemerisService | ✅ |

---

## 통계 요약

### 상태별 분류

| 상태 | 개수 | 비율 | 설명 |
|:----:|:----:|:----:|------|
| ✅ 정상 | 37 | 65% | FE→BE→로직 모두 연결 |
| ⏸️ 보류 | 1 | 2% | 역할 불명확, 검토 필요 |
| 🔧 미구현 | 12 | 21% | 기능 자체가 없음 (Stow, SpeedLimits) |
| ❌ 미사용 | 7 | 12% | 로직에서 안 씀 (삭제 검토) |

### 카테고리별 상세

| 카테고리 | 총 | ✅ | ⏸️ | 🔧 | ❌ | 비고 |
|----------|:--:|:--:|:--:|:--:|:--:|------|
| Location | 3 | 3 | - | - | - | 모두 정상 |
| Tracking | 3 | 2 | 1 | - | - | minElevationAngle 보류 |
| **Stow** | 6 | - | - | **6** | - | 모드 미구현 |
| AntennaSpec | 2 | 2 | - | - | - | 모두 정상 |
| AngleLimits | 6 | 1 | - | - | 5 | 한계 체크 미적용 |
| **SpeedLimits** | 6 | - | - | **6** | - | ICD 명령 미구현 |
| System.UDP | 6 | 6 | - | - | - | 모두 정상 |
| System.Tracking | 4 | 2 | - | - | 2 | interval만 사용 |
| System.WebSocket | 1 | 1 | - | - | - | 정상 |
| System.Performance | 7 | 6 | - | - | 1 | threshold 미사용 |
| System.Storage | 3 | 3 | - | - | - | 모두 정상 |
| System.SunTrack | 4 | 4 | - | - | - | 모두 정상 |
| System.JVM | 4 | - | - | - | 4 | 모두 미사용 |
| Ephemeris | 2 | 2 | - | - | - | 모두 정상 |
| 기타 | 6 | - | - | - | 6 | offset, algorithm 등 |

---

## 변경 이력

| 날짜 | 설정 키 | 변경 내용 | 담당 |
|------|---------|----------|------|
| 2026-01-18 | `tracking.durationDays` | 하드코딩 제거, 설정값 사용 | - |
| 2026-01-18 | `tracking.minElevationAngle` | 보류 처리 (역할 구분 필요) | - |

---

## 다음 조치 항목

### 우선순위 HIGH
- [ ] `tracking.minElevationAngle` 역할 정의 (sourceMinElevationAngle과 구분)

### 우선순위 MEDIUM
- [ ] AngleLimits → 명령 전송 전 한계 체크 로직 추가
- [ ] SpeedLimits → ICD 모터 명령 구현 시 연결
- [ ] Stow → Stow 모드 구현 시 연결

### 우선순위 LOW
- [ ] 미사용 설정 삭제 검토 (❌ 항목)
  - `algorithm.geoMinMotion`
  - `stepsizelimit.*`
  - `system.jvm.*`
  - `timeoffsetlimits.*`

---

**파일 위치**: `docs/architecture/SETTINGS_REGISTRY.md`
