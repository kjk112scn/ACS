# Fonts & Icons 라이선스 상세

프로젝트에서 사용하는 폰트, 아이콘, 이미지 리소스의 라이선스 정보입니다.

**검토일**: 2026-01-19

---

## 폰트 (Fonts)

### Roboto Font

| 항목 | 내용 |
|------|------|
| **이름** | Roboto |
| **라이선스** | Apache License 2.0 |
| **저작권** | Copyright (c) Google |
| **출처** | Google Fonts |
| **상업적 사용** | ✅ 허용 |
| **패키지** | `@quasar/extras` |

#### 파일 위치
```
node_modules/@quasar/extras/roboto-font/
├── LICENSE
├── roboto-font.css
└── web-font/
    ├── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWubEbFmUiAw.woff (weight: 100)
    ├── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWuaabVmUiAw.woff (weight: 300)
    ├── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWubEbVmUiAw.woff (weight: 400)
    ├── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWub2bVmUiAw.woff (weight: 500)
    ├── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWuYjalmUiAw.woff (weight: 700)
    └── KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWuZtalmUiAw.woff (weight: 900)
```

#### 프로젝트 설정
```typescript
// quasar.config.ts
extras: [
  'roboto-font',  // Apache 2.0
]
```

#### 의무사항
- 라이선스 텍스트 포함
- 저작권 표시

---

## 아이콘 (Icons)

### Material Icons

| 항목 | 내용 |
|------|------|
| **이름** | Material Icons |
| **라이선스** | Apache License 2.0 |
| **저작권** | Copyright (c) Google |
| **출처** | Google Material Design |
| **상업적 사용** | ✅ 허용 |
| **패키지** | `@quasar/extras` |

#### 파일 위치
```
node_modules/@quasar/extras/material-icons/
├── LICENSE
├── material-icons.css
└── web-font/
    ├── flUhRq6tzZclQEJ-Vdg-IuiaDsNcIhQ8tQ.woff2
    └── flUhRq6tzZclQEJ-Vdg-IuiaDsNa.woff
```

#### 프로젝트 설정
```typescript
// quasar.config.ts
extras: [
  'material-icons',  // Apache 2.0
]
```

#### 프로젝트에서 사용 중인 아이콘 목록

| 아이콘 | 용도 | 사용 위치 |
|--------|------|----------|
| satellite_alt | 위성 추적 | DashboardPage |
| satellite | 위성 | 여러 페이지 |
| warning | 경고 | 알림 컴포넌트 |
| check_circle | 확인 | 상태 표시 |
| error | 오류 | 에러 표시 |
| info | 정보 | 정보 표시 |
| settings | 설정 | 설정 페이지 |
| play_arrow | 재생 | 제어 버튼 |
| schedule | 일정 | 스케줄 페이지 |
| wifi | WiFi | 연결 상태 |
| engineering | 엔지니어링 | 유지보수 |
| language | 언어 | 언어 설정 |
| admin_panel_settings | 관리자 | 관리 페이지 |

---

## 미사용 아이콘 라이브러리 (설정만 존재)

`quasar.config.ts`에 주석 처리된 라이브러리들:

| 라이브러리 | 라이선스 | 상태 |
|-----------|---------|------|
| FontAwesome v5/v6 | CC BY 4.0 (무료) / Pro (유료) | 미사용 |
| Ionicons v4-v7 | MIT | 미사용 |
| MDI v3-v7 | Apache 2.0 | 미사용 |
| EVA Icons | MIT | 미사용 |
| Themify | MIT / OFL | 미사용 |
| Line Awesome | MIT / OFL | 미사용 |
| Bootstrap Icons | MIT | 미사용 |
| Material Symbols | Apache 2.0 | 미사용 |

**참고**: 이 라이브러리들은 현재 사용되지 않으므로 라이선스 의무가 없습니다.

---

## 이미지 리소스

### 프로젝트 자체 리소스

| 파일 | 위치 | 용도 | 라이선스 |
|------|------|------|---------|
| GTL_LOGO.png | `/frontend/public/logo/` | 회사 로고 | 프로젝트 소유 |
| favicon.ico | `/frontend/public/` | 파비콘 | 프로젝트 소유 |
| favicon-16x16.png | `/frontend/public/icons/` | 파비콘 16x16 | 프로젝트 소유 |
| favicon-32x32.png | `/frontend/public/icons/` | 파비콘 32x32 | 프로젝트 소유 |
| favicon-96x96.png | `/frontend/public/icons/` | 파비콘 96x96 | 프로젝트 소유 |

### 프레임워크 리소스

| 파일 | 위치 | 라이선스 |
|------|------|---------|
| quasar-logo-vertical.svg | `/frontend/src/assets/` | MIT (Quasar) |

---

## 라이선스 요약

| 리소스 유형 | 라이선스 | 상업적 사용 |
|------------|---------|-------------|
| Roboto Font | Apache 2.0 | ✅ |
| Material Icons | Apache 2.0 | ✅ |
| 프로젝트 로고/아이콘 | 자체 소유 | ✅ |
| Quasar 로고 | MIT | ✅ |

---

## 의무사항 체크리스트

### Roboto Font (Apache 2.0)
- [x] 라이선스 텍스트 포함
- [x] 저작권 표시
- [ ] NOTICE 포함 (배포 시)

### Material Icons (Apache 2.0)
- [x] 라이선스 텍스트 포함
- [x] 저작권 표시
- [ ] NOTICE 포함 (배포 시)

---

## 참고 링크

- [Google Fonts - Roboto](https://fonts.google.com/specimen/Roboto)
- [Roboto GitHub License](https://github.com/googlefonts/roboto-2/blob/main/LICENSE)
- [Material Icons Guide](https://developers.google.com/fonts/docs/material_icons)
- [Material Icons License](https://github.com/google/material-design-icons/blob/master/LICENSE)
- [Google Fonts FAQ](https://developers.google.com/fonts/faq)

---

## 업데이트 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-19 | 최초 작성 |
