# 설정 시스템 개발 가이드

## 개요
설정 시스템은 애플리케이션의 모든 설정 값을 관리하는 시스템입니다.

## 아키텍처

### 계층 구조
```
settings/
├── entity/                # 엔티티 클래스
├── storage/              # 저장소 구현체
└── service/              # 비즈니스 로직
```

### 주요 컴포넌트
1. **엔티티**
   - `Setting`: 설정 값 엔티티
   - `SettingHistory`: 설정 변경 이력 엔티티

2. **저장소**
   - 메모리 캐시
   - 데이터베이스 저장

3. **서비스**
   - 설정 값 CRUD
   - 이력 관리
   - 캐시 관리

## 설정 값 추가 방법

### 1. 기본값 상수 추가
```kotlin
companion object {
    private const val DEFAULT_NEW_SETTING = "default"
}
```

### 2. Getter/Setter 함수 추가
```kotlin
fun getNewSetting(): String = getSetting("new.setting", DEFAULT_NEW_SETTING)
fun setNewSetting(value: String, accountId: String, reason: String? = null) =
    updateSetting("new.setting", value, SettingType.STRING, accountId, reason)
```

### 3. 데이터베이스 마이그레이션
```sql
INSERT INTO settings (setting_key, setting_value, setting_type, description)
VALUES ('new.setting', 'default', 'STRING', '새로운 설정 설명');
```

## 캐시 관리
- 메모리 캐시는 `ConcurrentHashMap` 사용
- 캐시 미스 시 데이터베이스 조회
- 주기적인 캐시 새로고침 권장

## 이력 관리
- 모든 설정 변경은 이력 테이블에 기록
- 변경자 정보와 변경 사유 포함
- 이력 조회 API 제공

## 테스트
- 단위 테스트: `SettingsServiceTest`
- 통합 테스트: `SettingsIntegrationTest`
- API 테스트: `SettingsControllerTest`

## 모범 사례
1. 설정 키는 점(.) 구분자로 계층화
2. 설정 값은 항상 문자열로 저장
3. 타입 변환은 서비스 계층에서 처리
4. 캐시 일관성 유지

## 주의사항
1. 설정 키 중복 방지
2. 타입 안전성 확보
3. 동시성 고려
4. 캐시 무효화 시점 관리 