# Tracking_Session_Data_Enrichment

## 개요

**목적**: tracking_session 테이블의 빈 컬럼들을 계산/추출하여 의미있는 데이터로 채움
**요청일**: 2026-01-20
**상태**: 🚧 진행중

## 요구사항

- [ ] satellite_id - TLE에서 NORAD ID 추출
- [ ] duration - start_time, end_time으로 계산 (초 단위)
- [ ] max_azimuth_rate - DTL 데이터에서 최대값 계산
- [ ] max_elevation_rate - DTL 데이터에서 최대값 계산
- [ ] total_points - DTL 개수 카운트

## 현재 상태

```
satellite_id     → 빈 문자열 (satellite_name은 "AQUA"로 있음)
duration         → null (start_time, end_time은 있음)
max_azimuth_rate → null
max_elevation_rate → null
total_points     → null
```

## 영향 범위

| 영역 | 파일/컴포넌트 | 변경 내용 |
|------|--------------|----------|
| Backend | EphemerisDataRepository.kt | mapMstToSession() 수정 |
| Backend | PassScheduleDataRepository.kt | mapMstToSession() 수정 |
| Backend | EphemerisDataStore (선택) | MST 데이터 생성 시 값 채우기 |

## 관련 문서

- [DESIGN.md](DESIGN.md) - 설계 문서
- [PROGRESS.md](PROGRESS.md) - 진행 상황
