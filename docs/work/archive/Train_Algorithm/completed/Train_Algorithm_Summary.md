# Train 각도 알고리즘 요약

---

**완료일**: 2024-12  
**작성자**: GTL Systems  
**상태**: ✅ 구현 완료

---

## 목표

위성 추적 시 Keyhole 영역(Azimuth ±270° 근처) 회피를 위한 Train 각도 최적화 알고리즘 구현

**달성한 목표**:
- ✅ 2축 원본 데이터 → 3축 변환 (Train 각도 적용)
- ✅ Keyhole 판단 및 Train 각도 계산
- ✅ 각도 제한 (±270° 범위) 적용
- ✅ 6가지 DataType별 데이터 관리
- ✅ 동적 CSV 출력 (Keyhole 여부에 따라 헤더/데이터 변경)
- ✅ 프론트엔드 API 연동

---

## 주요 변경 사항

### 1. 데이터 구조
- 6가지 DataType 정의: `original`, `axis_transformed`, `final_transformed`, `keyhole_axis_transformed`, `keyhole_final_transformed`
- Train=0과 Train≠0 데이터 분리 관리
- Keyhole 발생 시에만 Train≠0 데이터 생성

### 2. Keyhole 판단 로직
- `final_transformed` (Train=0)의 MaxAzRate 기준으로 판단
- 임계값: 10.0°/s (공격적 운영, 권장)
- Keyhole 발생 시 Train≠0으로 재계산 진행

### 3. Train 각도 계산
- 방법: 최종 최대 각속도 시점 기준 (방법 B)
- 공식: `trainAngle = -azimuthAtMaxRate`
- 최단 거리로 Train 각도 계산

### 4. 각도 제한
- ±270° 범위로 제한하여 포지셔너 물리적 제한 준수
- `final_transformed`: Train=0 + 각도 제한 ✅
- `keyhole_final_transformed`: Train≠0 + 각도 제한 ✅

### 5. 각속도 계산
- 방법: 10-point cumulative sum method
- 단위: °/s (도/초)
- 정밀도: 10개 포인트 평활화

---

## 최종 상태

### 구현 완료 기능
✓ 6가지 DataType 처리 (original, axis, final, keyhole_axis, keyhole_final)
✓ Keyhole 판단 (final_transformed 기준)
✓ Train 각도 계산 (방법 B)
✓ `forcedTrainAngle` 파라미터로 Train=0/≠0 분리
✓ KeyholeAxis/Final DB 저장
✓ CSV 동적 헤더/데이터 출력
✓ 프론트엔드 API 병합

### 설정
- **임계값**: 기본 3.0°/s, 현재 10.0°/s 사용
- **각속도 계산**: 10-point cumulative sum method
- **각도 제한**: ±270°
- **Train 각도**: 최단 거리 계산

### 주요 파일
- `ProcessedTrackingData.kt`: 데이터 구조 정의
- `SatelliteTrackingProcessor.kt`: 전체 변환 로직
- `EphemerisService.kt`: CSV 생성 및 API 연동
- `CoordinateTransformer.kt`: 3D 변환
- `LimitAngleCalculator.kt`: 각도 제한
- `SettingsService.kt`: 임계값 설정

### API 엔드포인트
```
GET /api/ephemeris/tracking/mst/merged
```

**기능**: Original + FinalTransformed + KeyholeFinalTransformed 병합

---

## 핵심 개념

### Train 각도의 목적
- ✅ **Keyhole 회피**: ±270° 영역 통과 방지
- ❌ **각속도 최소화 아님** (부차적 효과)

### Keyhole 판단
- 기준: `final_transformed` (Train=0)의 MaxAzRate
- 임계값: 10.0°/s (설정 가능)
- 판단 후 Keyhole 발생 시에만 Train≠0 적용

### 데이터 저장
- Train=0: 항상 저장 (original, axis, final)
- Train≠0: Keyhole 발생 시만 저장 (keyhole_axis, keyhole_final)

---

## 참조

- 원본 계획: [Train_Algorithm_Original_Plan.md](./Train_Algorithm_Original_Plan.md)
- 최종 결과: [Train_Algorithm_Final_Result.md](./Train_Algorithm_Final_Result.md)

---

**문서 버전**: 1.0.0  
**최종 업데이트**: 2024-12

