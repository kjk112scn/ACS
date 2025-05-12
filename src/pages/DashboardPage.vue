<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- 상단 부분: 실시간 ICD 데이터 표시 (3축으로 구분) -->
    <q-card class="icd-data-section">
      <q-card-section>
        <div class="header-section">
          <!-- 명령 시간 (좌측 최상단으로 이동) -->
          <div class="cmd-time">
            <span class="adaptive-text time-value">{{ formattedServerTime }}</span>
          </div>
        </div>

        <div class="axis-grid">
          <!-- Azimuth 축 데이터 -->
          <q-card class="axis-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Azimuth</div>
              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdAzimuthAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.azimuthAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.azimuthSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Elevation 축 데이터 -->
          <q-card class="axis-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Elevation</div>
              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdElevationAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.elevationAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.elevationSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Tilt 축 데이터 -->
          <q-card class="axis-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Tilt</div>
              <div class="axis-data-row">
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">CMD</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.cmdTiltAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Actual</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.tiltAngle)
                  }}</q-item-label>
                </div>
                <div class="axis-data-item">
                  <q-item-label class="adaptive-caption">Speed</q-item-label>
                  <q-item-label class="adaptive-text">{{
                    displayValue(icdStore.tiltSpeed)
                  }}</q-item-label>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div class="status-messages q-mt-md">
          <p v-if="icdStore.error" class="text-negative">오류 발생: {{ icdStore.error }}</p>
          <p v-if="!icdStore.isConnected" class="text-warning">WebSocket 연결 중...</p>
        </div>
        <q-btn class="q-mt-md" color="negative" label="비상 정지" @click="icdStore.sendEmergency" />
      </q-card-section>
    </q-card>

    <!-- 모드 선택 탭 -->
    <q-card class="mode-selection-section q-mt-md">
      <q-card-section>
        <q-tabs
          v-model="currentMode"
          class="text-primary"
          active-color="primary"
          indicator-color="primary"
          align="left"
          narrow-indicator
        >
          <q-tab name="ephemeris" label="Ephemeris" @click="navigateToMode('ephemeris')" />
          <q-tab name="pedestal" label="Pedestal Position" @click="navigateToMode('pedestal')" />
          <q-tab name="suntrack" label="Sun Track" @click="navigateToMode('suntrack')" />
          <q-tab name="feed" label="Feed" @click="navigateToMode('feed')" />
        </q-tabs>
      </q-card-section>
    </q-card>

    <!-- 모드 컨텐츠 섹션 -->
    <q-card class="mode-content-section q-mt-md">
      <q-card-section>
        <!-- 라우터 뷰를 사용하여 현재 모드에 맞는 컴포넌트 표시 -->
        <router-view />
      </q-card-section>
    </q-card>
  </q-page>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useICDStore } from '../stores/ICD'
import { useRouter, useRoute } from 'vue-router'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()
const router = useRouter()
const route = useRoute()

// 값 표시 헬퍼 함수
const displayValue = (value: string | number | null | undefined) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }

  // 숫자로 변환 가능한지 확인
  const num = Number(value)
  if (!isNaN(num)) {
    return num.toFixed(2)
  }

  return value
}


// 서버 시간 포맷팅 (기본 Date 메서드 사용)
const formattedServerTime = computed(() => {


  if (!icdStore.serverTime) return '';

  try {
    // ISO 문자열을 Date 객체로 변환









    const dateObj = new Date(icdStore.serverTime);

    // 시간 문자열 생성 (toISOString은 항상 UTC 시간을 반환)
    // 예: "2023-05-12T16:49:59.928Z" -> "2023-05-12 16:49:59.928"
    const isoString = dateObj.toISOString();
    return isoString.replace('T', ' ').replace('Z', '');
  } catch (error) {


    console.error('날짜 포맷팅 오류:', error);
    return icdStore.serverTime; // 오류 발생 시 원본 문자열 반환
  }

});

// 현재 모드 상태
const currentMode = ref('ephemeris')

// 모드 변경 시 해당 라우트로 이동
const navigateToMode = (mode: string) => {
  // void 연산자를 사용하여 Promise를 명시적으로 무시
  void router.push(`/dashboard/${mode}`)
}

// 디버깅을 위한 watch 추가
watch(
  () => icdStore.tiltAngle,
  (newValue) => {
    console.log('DashboardPage에서 감지된 tiltAngle 변경:', newValue)
  },
)

// 다른 값들도 감시
watch(
  () => icdStore.azimuthAngle,
  (newValue) => {
    console.log('DashboardPage에서 감지된 azimuthAngle 변경:', newValue)
  },
)

watch(
  () => icdStore.elevationAngle,
  (newValue) => {
    console.log('DashboardPage에서 감지된 elevationAngle 변경:', newValue)
  },
)

// 라우트 변경 감지
watch(
  () => route.path,
  (newPath) => {
    const pathParts = newPath.split('/')
    // 배열의 마지막 요소가 존재하는지 확인 후 사용
    const currentPathMode = pathParts.length > 0 ? pathParts[pathParts.length - 1] : ''

    if (
      currentPathMode &&
      ['ephemeris', 'pedestal', 'suntrack', 'feed'].includes(currentPathMode)
    ) {
      currentMode.value = currentPathMode
    }
  },
)

// 컴포넌트 마운트 시 현재 라우트에 따라 모드 설정
onMounted(() => {
  // 현재 라우트 경로에서 모드 추출
  const pathParts = route.path.split('/')
  const currentPathMode = pathParts[pathParts.length - 1]

  // 유효한 모드인 경우 currentMode 업데이트
  if (currentPathMode && ['ephemeris', 'pedestal', 'suntrack', 'feed'].includes(currentPathMode)) {
    currentMode.value = currentPathMode
  } else {
    // 기본 모드로 리다이렉트 (void 연산자를 사용하여 Promise를 명시적으로 무시)
    void router.push('/dashboard/ephemeris')
  }

  // WebSocket 연결 초기화
  icdStore.initialize()

  // 디버깅용 타이머 - 스토어 값 주기적으로 로깅
  const debugInterval = setInterval(() => {
    console.log('현재 스토어 값:', {
      azimuthAngle: icdStore.azimuthAngle,
      elevationAngle: icdStore.elevationAngle,
      tiltAngle: icdStore.tiltAngle,
    })
  }, 5000)

  // 컴포넌트 언마운트 시 타이머 정리를 위한 변수 저장
  onUnmounted(() => {
    clearInterval(debugInterval)
  })
})

// 컴포넌트가 언마운트될 때 WebSocket 연결 정리
onUnmounted(() => {
  icdStore.cleanup()
})
</script>

<style>
/* 전역 스타일: 다크 모드와 라이트 모드에 따른 텍스트 색상 조정 */
.body--dark .adaptive-text {
  color: white !important;
}

.body--light .adaptive-text {
  color: black !important;
}

/* 다크 모드와 라이트 모드에 따른 caption 텍스트 색상 조정 */
.body--dark .adaptive-caption {
  color: rgba(255, 255, 255, 0.7) !important;
}

.body--light .adaptive-caption {
  color: rgba(0, 0, 0, 0.6) !important;
}
</style>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 1rem;
}

.cmd-time {
  display: flex;
  align-items: center;
}

.time-label {
  font-weight: 500;
  font-size: 1rem;
  margin-right: 0.25rem;
}

.time-value {
  font-weight: 500;
  font-size: 1rem;
}

.axis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-top: 1rem;
}

.axis-card {
  border: 1px solid var(--q-primary);
}

.axis-data-row {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
}

.axis-data-item {
  flex: 1;
  text-align: center;
  padding: 0.5rem;
}

.mode-toggle {
  width: 100%;
  max-width: 500px;
}

/* 모바일 화면에서는 축 데이터를 세로로 배치 */
@media (max-width: 768px) {
  .axis-grid {
    grid-template-columns: 1fr;
  }

  .axis-data-row {
    flex-direction: column;
  }

  .axis-data-item {
    text-align: left;
    padding: 0.25rem 0;
  }
}
</style>
