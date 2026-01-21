# Admin_Panel_Separation 설계 문서

## 1. 설계 의도

### Why (왜 이렇게 설계했는가)

1. **도메인 분리**: Settings = 앱 설정 vs Admin = 하드웨어 제어
2. **안전성**: 실수로 하드웨어 제어 접근 방지
3. **확장성**: 향후 권한 기반 접근 제어 용이
4. **산업 표준**: 안테나 제어 시스템에서 일반적인 패턴

### 대안 분석

| 대안 | 장점 | 단점 | 선택 여부 |
|------|------|------|----------|
| A. Settings 내 유지 + 경고 | 변경 최소 | 근본 해결 안됨 | ❌ |
| B. 별도 모달 (헤더 버튼) | 명확한 분리, 간단 | - | ✅ 선택 |
| C. 전용 페이지 (/admin) | 완전 분리 | 라우팅 복잡 | ❌ (향후) |

## 2. 구현 계획

### 2.1 버튼 위치

```
[Settings] [Admin] [DarkMode] [Info]
    ↑         ↑
  기존      신규 추가
```

### 2.2 AdminPanel.vue 구조

```vue
<template>
  <q-dialog v-model="isOpen" maximized>
    <q-card>
      <!-- 헤더 + 경고 배너 -->
      <q-banner class="bg-warning">
        이 기능들은 하드웨어를 직접 제어합니다.
      </q-banner>

      <!-- 탭 (기존 AdminSettings 재사용) -->
      <q-tabs>
        <q-tab name="servoPreset" />
        <q-tab name="servoAlarmReset" />
        <q-tab name="mcOnOff" />
        <q-tab name="maintenance" />
      </q-tabs>

      <!-- 컴포넌트 렌더링 -->
      <ServoEncoderPresetSettings />
      ...
    </q-card>
  </q-dialog>
</template>
```

### 2.3 MainLayout 변경

```typescript
// 추가
const adminPanelModal = ref(false)

// 템플릿
<q-btn icon="admin_panel_settings" @click="adminPanelModal = true" />
<AdminPanel v-model="adminPanelModal" />
```

### 2.4 SettingsModal 변경

- Admin 탭 관련 코드 제거 (L49-56, L94, import문)

## 3. 파일 변경 목록

| 파일 | 변경 내용 |
|------|----------|
| `layouts/MainLayout.vue` | Admin 버튼 + 모달 추가 |
| `components/Admin/AdminPanel.vue` | 신규 생성 |
| `components/Settings/SettingsModal.vue` | Admin 탭 제거 |

## 4. 테스트 계획

- [x] Settings 모달에서 Admin 탭 사라짐 확인
- [ ] Admin 버튼 클릭 시 AdminPanel 열림
- [ ] 각 Admin 기능 동작 확인 (Servo Preset, Alarm Reset, M/C, Maintenance)
- [ ] 경고 배너 표시 확인

## 5. 롤백 계획

문제 발생 시:
1. AdminPanel.vue 삭제
2. MainLayout.vue에서 Admin 버튼/모달 제거
3. SettingsModal.vue에서 Admin 탭 복원 (git revert)
