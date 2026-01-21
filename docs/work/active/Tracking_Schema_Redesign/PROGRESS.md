# Tracking Schema Redesign 진행 상황

## 진행률: 80% (BE 코드 완료, DB 적용 대기)

## 작업 체크리스트

### Phase 0: V006 마이그레이션 작성 ✅
- [x] tracking_trajectory 컬럼 추가 설계
- [x] tracking_session UNIQUE 제약 변경 설계
- [x] tracking_session TLE 연동 컬럼 추가 (FK + 스냅샷)
- [x] tracking_result 컬럼 재구성 설계
- [x] V006__Schema_redesign_tracking_tables.sql 작성

### Phase 1: Entity 업데이트 ✅
- [x] TrackingSessionEntity.kt (+4 필드: tle_cache_id, tle_line_1, tle_line_2, tle_epoch)
- [x] TrackingTrajectoryEntity.kt (+4 필드: train_rate, satellite_range, satellite_altitude, satellite_velocity)
- [x] TrackingResultEntity.kt (-17 필드, +15 필드)

### Phase 2: Repository 매핑 수정 ✅
- [x] TrackingResultRepository.kt - SQL INSERT/SELECT 재작성
- [x] TrackingResultRepository.kt - mapRowToEntity() 재작성
- [x] BatchStorageManager.kt - mapToTrackingResult() 재작성

### Phase 3: 검증 ✅
- [x] BE 빌드 확인 (BUILD SUCCESSFUL)

### Phase 4: DB 적용 (대기) ⏳
- [ ] BE 재시작 (Flyway V006 적용)
- [ ] DB 스키마 검증
- [ ] 데이터 저장 테스트

## 변경 파일 요약

| 파일 | 변경 내용 |
|------|----------|
| V006__Schema_redesign_tracking_tables.sql | +TLE 컬럼, UNIQUE 변경 |
| TrackingSessionEntity.kt | +4 TLE 필드 |
| TrackingTrajectoryEntity.kt | +4 satellite 필드 |
| TrackingResultEntity.kt | -17 이론치, +15 ICD/정밀추적 |
| TrackingResultRepository.kt | SQL/매핑 전체 재작성 |
| BatchStorageManager.kt | mapToTrackingResult() 재작성 |

## 일일 로그

### 2026-01-21
- V006 마이그레이션 작성 완료
- TLE 연동 방식 결정: FK + 스냅샷 (하이브리드)
  - tle_cache_id: FK로 tle_cache 참조
  - tle_line_1, tle_line_2, tle_epoch: 계산 시점 스냅샷
- 문서 정리: planned → active 이동
- README.md, PROGRESS.md 업데이트
- **Phase 1 완료**: Entity 3개 업데이트
- **Phase 2 완료**: Repository/BatchStorageManager 매핑 수정
- **BE 빌드 성공**: 컴파일 에러 0개