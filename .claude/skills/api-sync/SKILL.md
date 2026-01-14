# API Sync Skill (API 자동 동기화 스킬)

Backend-Frontend API 자동 동기화 스킬. OpenAPI 스펙 생성, TypeScript 타입 자동 생성, 불일치 검증 자동화.

## 개요

Backend Controller 변경 시 자동으로 OpenAPI 스펙을 생성하고, TypeScript 타입을 갱신하여 FE-BE 타입 불일치를 예방합니다. CI/CD 파이프라인에 통합 가능합니다.

## 사용 시점

- Controller 또는 DTO 수정 시
- 새 API 엔드포인트 추가 시
- API 변경사항 배포 전 검증
- 타입 불일치 버그 발생 시
- CI/CD 파이프라인 자동 실행

## 주요 기능

### 1. OpenAPI 스펙 자동 생성
- Spring Boot Controller 분석
- SpringDoc 어노테이션 기반 스펙 생성
- 검증 규칙 (Bean Validation) 반영
- 예제 데이터 포함

### 2. TypeScript 타입 자동 생성
- openapi-typescript 실행
- TypeScript interface 생성
- API 클라이언트 타입 안전성 보장
- 기존 타입과 Diff 비교

### 3. 불일치 검증
- DTO vs TypeScript 타입 비교
- Breaking Change 자동 검출
- 에러 발생 시 CI/CD 실패

### 4. 자동 커밋
- API 변경사항 자동 커밋
- PR 자동 생성 (선택)
- Conventional Commits 준수

## 커맨드

### `/api-sync generate`
OpenAPI 스펙 및 TypeScript 타입 생성

```bash
# 사용법
/api-sync generate --controller=SettingsController

# 옵션
--controller: 특정 Controller만 생성 (선택)
--all: 전체 Controller 생성 (기본값)
--output-format: 출력 형식 (yaml, json)
```

**동작**:
1. Backend Controller 스캔
2. OpenAPI 스펙 생성 (openapi.yaml)
3. TypeScript 타입 생성 (generated.ts)
4. Zod 스키마 생성 (schemas.ts)

**예시 출력**:
```
🔍 Controller 스캔 중...
   찾은 Controller: 5개
   - SettingsController (10개 엔드포인트)
   - SatelliteController (8개 엔드포인트)
   - PassScheduleController (6개 엔드포인트)
   - EphemerisController (5개 엔드포인트)
   - HardwareController (7개 엔드포인트)

📝 OpenAPI 스펙 생성 중...
   ✅ backend/src/main/resources/openapi/openapi.yaml

🔧 TypeScript 타입 생성 중...
   ✅ frontend/src/types/api/generated.ts

🛡️ Zod 스키마 생성 중...
   ✅ frontend/src/types/api/schemas.ts

✅ API 동기화 완료!
   - 총 엔드포인트: 36개
   - 생성된 타입: 28개
   - 스키마: 15개
```

---

### `/api-sync validate`
API 계약 검증

```bash
# 사용법
/api-sync validate --all

# 옵션
--all: 전체 검증 (기본값)
--breaking-only: Breaking Change만 검증
--controller: 특정 Controller만 검증
```

**검증 항목**:
1. DTO ↔ TypeScript 타입 일치성
2. Breaking Change 검출
3. 필수 필드 누락 검사
4. 타입 호환성 검사

**예시 출력**:
```
🔍 API 계약 검증 중...

✅ 타입 일치성: 28/28 통과
✅ 필수 필드: 모두 정의됨
⚠️ Breaking Change 발견: 1건

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔴 Breaking Changes:

1. LocationSettings.altitude 타입 변경
   - Before: number (optional)
   - After: number (required, min: 0)
   - 영향: frontend/src/stores/api/settings/locationSettingsStore.ts
   - 수정 필요: altitude 기본값 설정

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

권장 조치:
1. altitude 필드에 기본값 추가: altitude: 0
2. 또는 Backend에서 optional로 변경
```

---

### `/api-sync update`
OpenAPI 스펙 기반 Frontend 코드 업데이트

```bash
# 사용법
/api-sync update --auto-fix

# 옵션
--auto-fix: 자동 수정 (기본: false)
--force: Breaking Change 무시하고 강제 업데이트
--dry-run: 변경사항만 미리보기
```

**동작**:
1. OpenAPI 스펙 로드
2. TypeScript 타입 재생성
3. 기존 코드와 비교
4. 자동 수정 가능한 항목 수정
5. 수동 수정 필요한 항목 리스트 출력

**예시 출력**:
```
🔧 Frontend 코드 업데이트 중...

자동 수정 완료:
✅ locationSettingsStore.ts: latitude 초기값 0 → 35.317540
✅ trackingSettingsStore.ts: maxVelocity 타입 number → number | null

수동 수정 필요:
⚠️ settingsService.ts:45
   - setLocationSettings(settings: LocationSettings)
   - altitude 필수 필드 추가 필요

⚠️ LocationSettings.vue:120
   - v-model="locationSettings.altitude"
   - null 체크 추가 권장

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

변경 파일: 2개
   - frontend/src/stores/api/settings/locationSettingsStore.ts
   - frontend/src/stores/api/settings/trackingSettingsStore.ts

수동 수정 필요: 2곳
   - frontend/src/services/api/settingsService.ts:45
   - frontend/src/components/Settings/system/LocationSettings.vue:120
```

---

### `/api-sync watch`
파일 변경 감지 및 자동 동기화

```bash
# 사용법
/api-sync watch

# 옵션
--interval: 감지 간격 (ms, 기본: 1000)
--debounce: debounce 시간 (ms, 기본: 500)
```

**동작**:
- Controller, DTO 파일 변경 감지
- 자동으로 OpenAPI 스펙 재생성
- TypeScript 타입 자동 갱신
- Hot Reload 트리거

**예시 출력**:
```
👀 파일 변경 감지 중...
   대상: backend/src/main/kotlin/**/*Controller.kt
   대상: backend/src/main/kotlin/**/dto/**/*.kt

[10:30:15] 파일 변경 감지: SettingsController.kt
[10:30:15] OpenAPI 스펙 재생성 중...
[10:30:16] ✅ 스펙 생성 완료
[10:30:16] TypeScript 타입 생성 중...
[10:30:17] ✅ 타입 생성 완료

[10:30:17] 변경사항:
   + LocationSettings.category 필드 추가

계속 감시 중... (Ctrl+C로 종료)
```

---

### `/api-sync diff`
이전 버전과 현재 버전 비교

```bash
# 사용법
/api-sync diff --from=v1.0.0 --to=v1.1.0

# 옵션
--from: 이전 버전 (Git 태그 또는 브랜치)
--to: 현재 버전 (기본: HEAD)
--format: 출력 형식 (markdown, json, html)
```

**예시 출력**:
```markdown
# API Diff: v1.0.0 → v1.1.0

## 🟢 New Endpoints (3개)

### POST /api/settings/import
- Description: 설정 가져오기
- Request: multipart/form-data
- Response: 200 OK

### POST /api/settings/export
- Description: 설정 내보내기
- Response: 200 OK, application/json

### GET /api/settings/history
- Description: 설정 변경 이력 조회
- Query: key (optional), limit (default: 50)
- Response: 200 OK, SettingHistory[]

## 🔴 Breaking Changes (1개)

### PUT /api/settings/location
- **LocationRequest.altitude**: number (optional) → number (required, min: 0)
- 영향도: 높음
- 마이그레이션: altitude 기본값 추가 필요

## 🟡 Deprecated (1개)

### GET /api/settings/all
- Deprecated: v1.1.0에서 deprecated
- 대체: GET /api/settings/groups
- 제거 예정: v2.0.0
```

---

### `/api-sync commit`
API 변경사항 자동 커밋

```bash
# 사용법
/api-sync commit --message="Add settings import/export API"

# 옵션
--message: 커밋 메시지 (필수)
--pr: PR 자동 생성 (true/false)
--branch: 브랜치 이름 (기본: api-sync/{timestamp})
```

**동작**:
1. OpenAPI 스펙 변경사항 스테이징
2. TypeScript 타입 변경사항 스테이징
3. Conventional Commits 형식으로 커밋
4. PR 생성 (선택)

**예시 출력**:
```
📝 커밋 메시지 생성 중...

feat(api): Add settings import/export API

- POST /api/settings/import
- POST /api/settings/export
- GET /api/settings/history

BREAKING CHANGE: LocationRequest.altitude is now required

🚀 커밋 완료!
   Branch: api-sync/20260107-103045
   Commit: a1b2c3d "feat(api): Add settings import/export API"

🔗 PR 생성 중...
   ✅ PR #123: feat(api): Add settings import/export API
   URL: https://github.com/user/acs/pull/123
```

---

## 자동화 설정

### package.json 스크립트
```json
{
  "scripts": {
    "api:generate": "cd ../backend && ./gradlew generateOpenApiDocs && cd ../frontend && openapi-typescript ../backend/src/main/resources/openapi/openapi.yaml -o src/types/api/generated.ts",
    "api:validate": "node scripts/validate-api-contract.js",
    "api:watch": "nodemon --watch ../backend/src/main/kotlin --ext kt --exec \"npm run api:generate\"",
    "api:diff": "node scripts/api-diff.js"
  }
}
```

### CI/CD 파이프라인 예시
```yaml
# .github/workflows/api-sync.yml
name: API Sync

on:
  push:
    paths:
      - 'backend/src/main/kotlin/**/*Controller.kt'
      - 'backend/src/main/kotlin/**/dto/**/*.kt'

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Generate OpenAPI Spec
        run: cd backend && ./gradlew generateOpenApiDocs

      - name: Generate TypeScript Types
        run: cd frontend && npm run api:generate

      - name: Validate API Contract
        run: cd frontend && npm run api:validate

      - name: Commit Changes
        if: ${{ success() }}
        run: |
          git config user.name "API Sync Bot"
          git config user.email "bot@acs.com"
          git add backend/src/main/resources/openapi/openapi.yaml
          git add frontend/src/types/api/generated.ts
          git commit -m "chore(api): Auto-sync API types"
          git push

      - name: Create PR
        if: ${{ success() }}
        uses: peter-evans/create-pull-request@v5
        with:
          title: "chore(api): Auto-sync API types"
          body: "Automatically generated by API Sync workflow"
          branch: api-sync/${{ github.run_number }}
```

---

## 스크립트 구현 예시

### validate-api-contract.js
```javascript
// scripts/validate-api-contract.js
const fs = require('fs')
const yaml = require('js-yaml')

// OpenAPI 스펙 로드
const openapi = yaml.load(fs.readFileSync('../backend/src/main/resources/openapi/openapi.yaml', 'utf8'))

// TypeScript 타입 로드
const generated = fs.readFileSync('src/types/api/generated.ts', 'utf8')

// 검증 로직
let breakingChanges = []
let errors = []

// DTO vs TypeScript 타입 비교
for (const [path, methods] of Object.entries(openapi.paths)) {
  for (const [method, spec] of Object.entries(methods)) {
    if (spec.requestBody) {
      const schemaRef = spec.requestBody.content['application/json']?.schema?.$ref
      if (schemaRef) {
        const schemaName = schemaRef.split('/').pop()

        // TypeScript에 해당 타입 존재 확인
        if (!generated.includes(`interface ${schemaName}`)) {
          errors.push(`Missing TypeScript type: ${schemaName}`)
        }
      }
    }
  }
}

// 결과 출력
if (errors.length > 0) {
  console.error('❌ API Contract Validation Failed')
  errors.forEach(err => console.error(`  - ${err}`))
  process.exit(1)
} else {
  console.log('✅ API Contract Validation Passed')
  if (breakingChanges.length > 0) {
    console.warn('⚠️ Breaking Changes Detected:')
    breakingChanges.forEach(change => console.warn(`  - ${change}`))
  }
}
```

---

## Zod 스키마 자동 생성

### zod-schema-generator.js
```javascript
// scripts/zod-schema-generator.js
const fs = require('fs')
const yaml = require('js-yaml')

const openapi = yaml.load(fs.readFileSync('../backend/src/main/resources/openapi/openapi.yaml', 'utf8'))

let zodSchemas = `import { z } from 'zod'\n\n`

for (const [schemaName, schema] of Object.entries(openapi.components.schemas)) {
  zodSchemas += `export const ${schemaName}Schema = z.object({\n`

  for (const [propName, prop] of Object.entries(schema.properties)) {
    let zodType = ''

    switch (prop.type) {
      case 'string':
        zodType = 'z.string()'
        break
      case 'number':
        zodType = 'z.number()'
        if (prop.minimum) zodType += `.min(${prop.minimum})`
        if (prop.maximum) zodType += `.max(${prop.maximum})`
        break
      case 'boolean':
        zodType = 'z.boolean()'
        break
      case 'integer':
        zodType = 'z.number().int()'
        break
    }

    if (!schema.required?.includes(propName)) {
      zodType += '.optional()'
    }

    zodSchemas += `  ${propName}: ${zodType},\n`
  }

  zodSchemas += `})\n\n`
  zodSchemas += `export type ${schemaName} = z.infer<typeof ${schemaName}Schema>\n\n`
}

fs.writeFileSync('src/types/api/schemas.ts', zodSchemas)
console.log('✅ Zod schemas generated')
```

---

## 참고 문서

- [RFC_Configuration_Management.md](../../docs/work/active/Architecture_Refactoring/RFC_Configuration_Management.md)
- [api-contract-manager 에이전트](../../.claude/agents/api-contract-manager.md)
- [OpenAPI Specification](https://swagger.io/specification/)
- [openapi-typescript](https://github.com/drwpow/openapi-typescript)

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0.0 | 2026-01-07 | 최초 작성 |
