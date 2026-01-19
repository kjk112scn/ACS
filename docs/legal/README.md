# 라이선스 관리 가이드

GTL ACS 프로젝트의 라이선스 관리 정책 및 절차를 정의합니다.

---

## 목적

- 프로젝트에서 사용하는 모든 서드파티 라이브러리의 라이선스 추적
- 기업 상용 사용 적합성 보장
- 라이선스 컴플라이언스 유지

---

## 라이선스 정책

### 허용 라이선스

다음 라이선스는 기업 무료 사용이 가능하며 프로젝트에서 사용 승인됩니다:

| 라이선스 | 특징 | 주의사항 |
|----------|------|----------|
| **MIT** | 가장 관대함 | 라이선스 텍스트 포함 |
| **Apache 2.0** | 특허 보호 포함 | 라이선스 + NOTICE 포함 |
| **BSD (2/3-Clause)** | MIT와 유사 | 라이선스 텍스트 포함 |
| **ISC** | MIT와 동등 | 라이선스 텍스트 포함 |
| **Public Domain / CC0** | 제약 없음 | 없음 |

### 주의 필요 라이선스

다음 라이선스는 사용 전 법무 검토가 필요합니다:

| 라이선스 | 위험 수준 | 이유 |
|----------|----------|------|
| **GPL v2/v3** | 높음 | Copyleft - 소스 공개 요구 |
| **LGPL** | 중간 | 동적 링크 시 허용, 정적 링크 시 주의 |
| **AGPL** | 매우 높음 | 네트워크 사용 시에도 소스 공개 |
| **CC BY-NC** | 높음 | 비상업 용도만 허용 |
| **Proprietary** | 높음 | 유료 라이선스 필요 |

### 금지 라이선스

다음 라이선스는 프로젝트에서 사용이 금지됩니다:

- **상업적 사용 금지** 조항이 있는 라이선스
- **유료 라이선스** (별도 승인 없이)
- 라이선스가 **명확하지 않은** 패키지

---

## 라이선스 검토 프로세스

### 신규 의존성 추가 시

1. **라이선스 확인**
   ```bash
   # npm 패키지
   npm info <package-name> license

   # 또는 package.json 확인
   cat node_modules/<package-name>/package.json | grep license
   ```

2. **허용 목록 대조**
   - 허용 라이선스 목록과 대조
   - 불명확한 경우 법무팀 검토 요청

3. **문서 업데이트**
   - `THIRD_PARTY_LICENSES.md` 업데이트
   - 해당 상세 문서 업데이트

### 정기 검토 (분기별)

1. **의존성 감사**
   ```bash
   # Frontend
   cd frontend && npm audit

   # Backend
   cd backend && ./gradlew dependencyInsight
   ```

2. **라이선스 스캔 (권장)**
   ```bash
   # license-checker 설치 및 실행
   npm install -g license-checker
   cd frontend && license-checker --summary
   ```

3. **문서 동기화**
   - 버전 변경 사항 반영
   - 제거된 패키지 정리

---

## 문서 구조

```
docs/legal/
├── README.md                 # 이 문서 (관리 가이드)
├── LICENSES_SUMMARY.md       # 전체 라이선스 요약표
├── frontend-dependencies.md  # 프론트엔드 패키지 상세
├── backend-dependencies.md   # 백엔드 패키지 상세
├── fonts-and-icons.md        # 폰트/아이콘 라이선스
└── license-texts/            # 라이선스 원문
    ├── MIT.txt
    ├── Apache-2.0.txt
    └── BSD-3-Clause.txt
```

---

## 배포 시 체크리스트

### 필수 포함 항목

- [ ] `LICENSE` - 프로젝트 자체 라이선스
- [ ] `THIRD_PARTY_LICENSES.md` - 서드파티 라이선스 요약
- [ ] Apache 2.0 라이브러리의 NOTICE 파일 (해당 시)

### 확인 사항

- [ ] 모든 의존성 라이선스 확인 완료
- [ ] GPL 계열 라이선스 미포함 확인
- [ ] 유료 라이선스 미포함 확인
- [ ] 라이선스 문서 최신화

---

## 담당자

- **라이선스 관리**: 개발팀
- **법무 검토**: 법무팀 (필요시)
- **최종 승인**: 기술 책임자

---

## 참고 자료

- [SPDX License List](https://spdx.org/licenses/)
- [Choose a License](https://choosealicense.com/)
- [FOSSA - Open Source License Compliance](https://fossa.com/)
- [npm license checker](https://www.npmjs.com/package/license-checker)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2026-01-19 | 1.0.0 | 최초 작성 | - |
