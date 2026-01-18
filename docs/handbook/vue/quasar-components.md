# Quasar 컴포넌트 가이드

> ACS 프로젝트에서 자주 사용하는 Quasar UI 컴포넌트

## Quasar란?

Vue 3 기반의 UI 프레임워크입니다.
Material Design 스타일의 컴포넌트를 제공하며, **q-** 접두사로 시작합니다.

```vue
<!-- Quasar 컴포넌트 예시 -->
<q-btn label="클릭" color="primary" />
<q-input v-model="text" label="입력" />
<q-card>카드 내용</q-card>
```

---

## 레이아웃 컴포넌트

### q-layout
페이지 전체 레이아웃 구조

```vue
<q-layout view="hHh lpR fFf">
  <!-- h: header, l: left drawer, p: page, R: right drawer, f: footer -->

  <q-header>
    <q-toolbar>
      <q-toolbar-title>ACS</q-toolbar-title>
    </q-toolbar>
  </q-header>

  <q-drawer side="left" v-model="leftDrawerOpen">
    <!-- 사이드 메뉴 -->
  </q-drawer>

  <q-page-container>
    <router-view />  <!-- 페이지 내용 -->
  </q-page-container>
</q-layout>
```

### q-page
페이지 컨테이너 (q-page-container 안에서 사용)

```vue
<q-page padding>
  <!-- padding: 기본 패딩 적용 -->
  <div class="content">페이지 내용</div>
</q-page>
```

### q-card
카드 레이아웃

```vue
<q-card class="my-card">
  <q-card-section>
    <div class="text-h6">제목</div>
    <div class="text-subtitle2">부제목</div>
  </q-card-section>

  <q-separator />

  <q-card-section>
    본문 내용
  </q-card-section>

  <q-card-actions align="right">
    <q-btn flat label="취소" />
    <q-btn color="primary" label="확인" />
  </q-card-actions>
</q-card>
```

---

## 폼 컴포넌트

### q-input
텍스트 입력

```vue
<!-- 기본 -->
<q-input v-model="text" label="이름" />

<!-- 숫자 입력 -->
<q-input
  v-model.number="value"
  type="number"
  label="방위각"
  suffix="°"
/>

<!-- 비밀번호 -->
<q-input
  v-model="password"
  :type="showPassword ? 'text' : 'password'"
  label="비밀번호"
>
  <template #append>
    <q-icon
      :name="showPassword ? 'visibility_off' : 'visibility'"
      @click="showPassword = !showPassword"
      class="cursor-pointer"
    />
  </template>
</q-input>

<!-- 검증 -->
<q-input
  v-model="email"
  label="이메일"
  :rules="[
    val => !!val || '필수 입력',
    val => val.includes('@') || '이메일 형식 오류'
  ]"
/>
```

### q-select
드롭다운 선택

```vue
<!-- 기본 -->
<q-select
  v-model="selected"
  :options="['옵션1', '옵션2', '옵션3']"
  label="선택"
/>

<!-- 객체 옵션 -->
<q-select
  v-model="satellite"
  :options="satellites"
  option-label="name"
  option-value="id"
  label="위성 선택"
  emit-value
  map-options
/>

<!-- 검색 가능 -->
<q-select
  v-model="selected"
  :options="filteredOptions"
  use-input
  input-debounce="300"
  @filter="filterFn"
  label="검색"
>
  <template #no-option>
    <q-item>
      <q-item-section>결과 없음</q-item-section>
    </q-item>
  </template>
</q-select>
```

### q-checkbox / q-toggle
체크박스와 토글

```vue
<!-- 체크박스 -->
<q-checkbox v-model="agreed" label="동의합니다" />

<!-- 토글 스위치 -->
<q-toggle v-model="enabled" label="활성화" />

<!-- 색상 지정 -->
<q-toggle
  v-model="tracking"
  label="추적 모드"
  color="green"
  checked-icon="check"
  unchecked-icon="clear"
/>
```

### q-slider
슬라이더

```vue
<!-- 기본 -->
<q-slider v-model="value" :min="0" :max="360" />

<!-- 라벨 표시 -->
<q-slider
  v-model="azimuth"
  :min="0"
  :max="360"
  :step="0.1"
  label
  label-always
  :label-value="azimuth + '°'"
/>

<!-- 범위 슬라이더 -->
<q-range
  v-model="range"
  :min="0"
  :max="100"
  label
/>
```

### q-btn-toggle
버튼 그룹 토글

```vue
<q-btn-toggle
  v-model="mode"
  toggle-color="primary"
  :options="[
    { label: 'Standby', value: 'standby' },
    { label: 'Step', value: 'step' },
    { label: 'Slew', value: 'slew' }
  ]"
/>
```

---

## 버튼

### q-btn
버튼 컴포넌트

```vue
<!-- 기본 버튼 -->
<q-btn label="클릭" color="primary" />

<!-- 아이콘 버튼 -->
<q-btn icon="play_arrow" color="green" />

<!-- 아이콘 + 라벨 -->
<q-btn icon="save" label="저장" color="primary" />

<!-- 로딩 상태 -->
<q-btn
  label="저장"
  color="primary"
  :loading="isLoading"
  @click="save"
/>

<!-- 비활성화 -->
<q-btn label="전송" :disable="!isValid" />

<!-- 스타일 변형 -->
<q-btn flat label="Flat" />       <!-- 배경 없음 -->
<q-btn outline label="Outline" /> <!-- 테두리만 -->
<q-btn round icon="add" />        <!-- 원형 -->
<q-btn fab icon="add" />          <!-- Floating Action Button -->
```

### q-btn-dropdown
드롭다운 버튼

```vue
<q-btn-dropdown color="primary" label="메뉴">
  <q-list>
    <q-item clickable v-close-popup @click="action1">
      <q-item-section>옵션 1</q-item-section>
    </q-item>
    <q-item clickable v-close-popup @click="action2">
      <q-item-section>옵션 2</q-item-section>
    </q-item>
  </q-list>
</q-btn-dropdown>
```

---

## 데이터 표시

### q-table
테이블

```vue
<q-table
  :rows="passes"
  :columns="columns"
  row-key="id"
  :pagination="{ rowsPerPage: 10 }"
>
  <!-- 컬럼 정의 -->
  <!-- columns: [
    { name: 'satellite', label: '위성', field: 'satelliteName', sortable: true },
    { name: 'aos', label: 'AOS', field: 'aos', format: val => formatTime(val) },
    { name: 'maxEl', label: '최대 El', field: 'maxElevation', format: val => val.toFixed(1) + '°' }
  ] -->

  <!-- 커스텀 셀 -->
  <template #body-cell-actions="props">
    <q-td :props="props">
      <q-btn flat icon="edit" @click="edit(props.row)" />
      <q-btn flat icon="delete" color="negative" @click="remove(props.row)" />
    </q-td>
  </template>
</q-table>
```

### q-list
리스트

```vue
<q-list bordered separator>
  <q-item v-for="item in items" :key="item.id" clickable v-ripple>
    <q-item-section avatar>
      <q-icon :name="item.icon" />
    </q-item-section>

    <q-item-section>
      <q-item-label>{{ item.title }}</q-item-label>
      <q-item-label caption>{{ item.subtitle }}</q-item-label>
    </q-item-section>

    <q-item-section side>
      <q-icon name="chevron_right" />
    </q-item-section>
  </q-item>
</q-list>
```

### q-badge
뱃지

```vue
<!-- 기본 -->
<q-badge color="primary">NEW</q-badge>

<!-- 아이콘에 뱃지 -->
<q-icon name="mail">
  <q-badge color="red" floating>5</q-badge>
</q-icon>

<!-- 상태 표시 -->
<q-badge :color="status === 'active' ? 'green' : 'grey'">
  {{ status }}
</q-badge>
```

### q-chip
칩 (태그)

```vue
<!-- 기본 -->
<q-chip>태그</q-chip>

<!-- 삭제 가능 -->
<q-chip
  v-for="tag in tags"
  :key="tag"
  removable
  @remove="removeTag(tag)"
>
  {{ tag }}
</q-chip>

<!-- 선택 가능 -->
<q-chip
  clickable
  :color="selected ? 'primary' : 'grey'"
  @click="selected = !selected"
>
  필터
</q-chip>
```

---

## 피드백 컴포넌트

### q-dialog
다이얼로그 (모달)

```vue
<template>
  <q-btn label="열기" @click="dialogOpen = true" />

  <q-dialog v-model="dialogOpen">
    <q-card style="min-width: 350px">
      <q-card-section>
        <div class="text-h6">제목</div>
      </q-card-section>

      <q-card-section>
        다이얼로그 내용
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="취소" v-close-popup />
        <q-btn color="primary" label="확인" @click="confirm" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
```

### useQuasar - Notify
알림 메시지

```typescript
import { useQuasar } from 'quasar'

const $q = useQuasar()

// 성공
$q.notify({
  type: 'positive',
  message: '저장 완료',
  position: 'top'
})

// 에러
$q.notify({
  type: 'negative',
  message: '저장 실패',
  position: 'top',
  timeout: 3000
})

// 경고
$q.notify({
  type: 'warning',
  message: '주의가 필요합니다',
  position: 'top-right'
})

// 커스텀
$q.notify({
  message: '새 메시지',
  color: 'purple',
  icon: 'mail',
  actions: [
    { label: '보기', color: 'white', handler: () => goToMessages() }
  ]
})
```

### useQuasar - Dialog
프로그래매틱 다이얼로그

```typescript
const $q = useQuasar()

// 확인 다이얼로그
$q.dialog({
  title: '삭제 확인',
  message: '정말 삭제하시겠습니까?',
  cancel: true,
  persistent: true
}).onOk(() => {
  deleteItem()
}).onCancel(() => {
  console.log('취소됨')
})

// 입력 다이얼로그
$q.dialog({
  title: '이름 입력',
  message: '새 이름을 입력하세요',
  prompt: {
    model: '',
    type: 'text'
  },
  cancel: true
}).onOk((name) => {
  rename(name)
})
```

### q-spinner
로딩 스피너

```vue
<!-- 기본 -->
<q-spinner v-if="loading" />

<!-- 크기/색상 -->
<q-spinner color="primary" size="3em" />

<!-- 다양한 스타일 -->
<q-spinner-dots />
<q-spinner-bars />
<q-spinner-gears />
<q-spinner-hourglass />

<!-- 오버레이 로딩 -->
<q-inner-loading :showing="loading">
  <q-spinner-gears size="50px" color="primary" />
</q-inner-loading>
```

### q-linear-progress
진행률 바

```vue
<!-- 기본 -->
<q-linear-progress :value="0.6" />

<!-- 불확정 (로딩) -->
<q-linear-progress indeterminate />

<!-- 스트라이프 -->
<q-linear-progress
  :value="progress"
  stripe
  color="primary"
/>

<!-- 버퍼링 -->
<q-linear-progress
  :value="downloaded"
  :buffer="buffered"
/>
```

---

## 네비게이션

### q-tabs
탭 네비게이션

```vue
<q-tabs v-model="tab" class="text-primary">
  <q-tab name="info" label="정보" />
  <q-tab name="settings" label="설정" />
  <q-tab name="logs" label="로그" />
</q-tabs>

<q-tab-panels v-model="tab" animated>
  <q-tab-panel name="info">
    정보 내용
  </q-tab-panel>
  <q-tab-panel name="settings">
    설정 내용
  </q-tab-panel>
  <q-tab-panel name="logs">
    로그 내용
  </q-tab-panel>
</q-tab-panels>
```

### q-expansion-item
확장 패널

```vue
<q-list>
  <q-expansion-item
    expand-separator
    icon="settings"
    label="고급 설정"
    caption="상세 옵션"
  >
    <q-card>
      <q-card-section>
        설정 내용...
      </q-card-section>
    </q-card>
  </q-expansion-item>

  <q-expansion-item
    icon="info"
    label="시스템 정보"
    default-opened
  >
    <q-card>
      <q-card-section>
        시스템 정보...
      </q-card-section>
    </q-card>
  </q-expansion-item>
</q-list>
```

---

## 유틸리티

### q-tooltip
툴팁

```vue
<q-btn icon="help">
  <q-tooltip>도움말 보기</q-tooltip>
</q-btn>

<!-- 위치 지정 -->
<q-btn icon="info">
  <q-tooltip anchor="top middle" self="bottom middle">
    위쪽에 표시
  </q-tooltip>
</q-btn>
```

### q-separator
구분선

```vue
<!-- 가로 -->
<q-separator />

<!-- 세로 (인라인 요소 사이) -->
<q-separator vertical />

<!-- 여백 포함 -->
<q-separator spaced />

<!-- 색상 -->
<q-separator color="primary" />
```

### q-space
플렉스 공간

```vue
<q-toolbar>
  <q-btn icon="menu" />
  <q-toolbar-title>제목</q-toolbar-title>
  <q-space />  <!-- 나머지 공간 차지 -->
  <q-btn icon="settings" />
</q-toolbar>
```

### q-scroll-area
스크롤 영역

```vue
<q-scroll-area style="height: 300px;">
  <!-- 긴 내용 -->
</q-scroll-area>

<!-- 커스텀 스크롤바 -->
<q-scroll-area
  :thumb-style="{ width: '5px', opacity: 0.5 }"
  style="height: 400px;"
>
  ...
</q-scroll-area>
```

---

## ACS 프로젝트 예시

### 안테나 상태 카드
```vue
<q-card class="antenna-status-card">
  <q-card-section class="row items-center">
    <q-icon name="satellite_alt" size="md" class="q-mr-sm" />
    <span class="text-h6">안테나 상태</span>
    <q-space />
    <q-badge :color="isConnected ? 'green' : 'red'">
      {{ isConnected ? '연결됨' : '연결 끊김' }}
    </q-badge>
  </q-card-section>

  <q-separator />

  <q-card-section>
    <div class="row q-gutter-md">
      <div class="col">
        <div class="text-caption">방위각 (Az)</div>
        <div class="text-h5">{{ azimuth.toFixed(2) }}°</div>
      </div>
      <div class="col">
        <div class="text-caption">고도각 (El)</div>
        <div class="text-h5">{{ elevation.toFixed(2) }}°</div>
      </div>
      <div class="col">
        <div class="text-caption">틸트 (Tilt)</div>
        <div class="text-h5">{{ tilt.toFixed(2) }}°</div>
      </div>
    </div>
  </q-card-section>
</q-card>
```

### 모드 선택 버튼
```vue
<q-btn-toggle
  v-model="currentMode"
  toggle-color="primary"
  spread
  :options="[
    { label: 'Standby', value: 'STANDBY', icon: 'pause' },
    { label: 'Step', value: 'STEP', icon: 'my_location' },
    { label: 'Slew', value: 'SLEW', icon: 'swap_horiz' },
    { label: 'Track', value: 'TRACK', icon: 'track_changes' }
  ]"
  @update:model-value="changeMode"
/>
```

### 패스 스케줄 테이블
```vue
<q-table
  :rows="passes"
  :columns="passColumns"
  row-key="id"
  :pagination="{ rowsPerPage: 5 }"
  selection="single"
  v-model:selected="selectedPass"
>
  <template #body-cell-status="props">
    <q-td :props="props">
      <q-chip
        :color="getStatusColor(props.row.status)"
        text-color="white"
        dense
      >
        {{ props.row.status }}
      </q-chip>
    </q-td>
  </template>

  <template #body-cell-actions="props">
    <q-td :props="props">
      <q-btn
        flat
        round
        icon="play_arrow"
        color="primary"
        :disable="props.row.status !== 'PENDING'"
        @click="startPass(props.row)"
      >
        <q-tooltip>추적 시작</q-tooltip>
      </q-btn>
      <q-btn
        flat
        round
        icon="delete"
        color="negative"
        @click="deletePass(props.row)"
      >
        <q-tooltip>삭제</q-tooltip>
      </q-btn>
    </q-td>
  </template>
</q-table>
```

---

## 자주 쓰는 CSS 클래스

### 여백
```html
<!-- margin -->
<div class="q-ma-md">전체 margin medium</div>
<div class="q-mt-lg">margin-top large</div>
<div class="q-px-sm">padding 좌우 small</div>

<!-- 크기: none, xs, sm, md, lg, xl -->
```

### 텍스트
```html
<div class="text-h1">Heading 1</div>
<div class="text-h6">Heading 6</div>
<div class="text-subtitle1">Subtitle</div>
<div class="text-body1">Body text</div>
<div class="text-caption">Caption</div>

<div class="text-bold">Bold</div>
<div class="text-italic">Italic</div>
<div class="text-center">Center</div>
<div class="text-right">Right</div>
```

### 색상
```html
<div class="text-primary">Primary 색상 텍스트</div>
<div class="text-negative">에러 색상</div>
<div class="text-positive">성공 색상</div>
<div class="text-warning">경고 색상</div>
<div class="text-grey-6">회색 텍스트</div>

<div class="bg-primary">Primary 배경</div>
<div class="bg-grey-2">밝은 회색 배경</div>
```

### Flexbox
```html
<div class="row">가로 배치</div>
<div class="column">세로 배치</div>
<div class="row items-center">세로 중앙</div>
<div class="row justify-center">가로 중앙</div>
<div class="row justify-between">양쪽 정렬</div>

<div class="col">균등 분배</div>
<div class="col-6">50% 너비</div>
<div class="col-auto">컨텐츠 크기</div>
```

---

## 테마 커스터마이징

### quasar.variables.scss
```scss
// src/css/quasar.variables.scss
$primary   : #1976D2;
$secondary : #26A69A;
$accent    : #9C27B0;
$positive  : #21BA45;
$negative  : #C10015;
$info      : #31CCEC;
$warning   : #F2C037;

// 커스텀 테마 변수 (ACS)
$theme-background: #1a1a2e;
$theme-surface: #16213e;
$theme-text: #e8e8e8;
```

### 다크 모드
```typescript
// main.ts 또는 컴포넌트에서
import { useQuasar } from 'quasar'

const $q = useQuasar()

// 다크 모드 토글
$q.dark.toggle()

// 다크 모드 설정
$q.dark.set(true)  // 다크 모드 ON
$q.dark.set(false) // 라이트 모드

// 현재 상태 확인
const isDark = $q.dark.isActive
```

---

## Quick Reference

```vue
<!-- 레이아웃 -->
<q-layout>  <q-header>  <q-drawer>  <q-page>  <q-card>

<!-- 폼 -->
<q-input>  <q-select>  <q-checkbox>  <q-toggle>  <q-slider>

<!-- 버튼 -->
<q-btn>  <q-btn-toggle>  <q-btn-dropdown>

<!-- 데이터 -->
<q-table>  <q-list>  <q-item>  <q-badge>  <q-chip>

<!-- 피드백 -->
<q-dialog>  <q-spinner>  <q-linear-progress>
$q.notify()  $q.dialog()

<!-- 네비게이션 -->
<q-tabs>  <q-tab-panels>  <q-expansion-item>

<!-- 유틸리티 -->
<q-tooltip>  <q-separator>  <q-space>  <q-scroll-area>
```

---

**이전**: [typescript-patterns.md](./typescript-patterns.md) - TypeScript 패턴
**다음**: Kotlin 섹션 - [../kotlin/kotlin-basics.md](../kotlin/kotlin-basics.md)
