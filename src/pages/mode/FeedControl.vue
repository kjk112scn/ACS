<template>
  <q-page class="dashboard-container q-pa-md">
    <!-- Feed Control 타이틀 -->
    <div class="section-title text-h5 text-primary q-mb-sm">Feed Control</div>

    <!-- Feed 제어 박스 -->
    <q-card class="feed-control-section q-mb-md">
      <q-card-section>
        <div class="row q-col-gutter-md">
          <!-- S-Band 섹션 -->
          <div class="col-12 col-md-6">
            <q-card class="s-band-section">
              <q-card-section>
                <div class="text-h6 text-primary">S-Band</div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <!-- S-Band LHCP -->
                  <div class="col-12 col-sm-4">
                    <q-toggle
                      v-model="feedControls.sLHCP"
                      label="LHCP"
                      color="primary"
                      :disable="isLoading"
                    />
                  </div>
                  <!-- S-Band RHCP -->
                  <div class="col-12 col-sm-4">
                    <q-toggle
                      v-model="feedControls.sRHCP"
                      label="RHCP"
                      color="primary"
                      :disable="isLoading"
                    />
                  </div>
                  <!-- S-RF Switch -->
                  <div class="col-12 col-sm-4">
                    <q-toggle
                      v-model="feedControls.sRFSwitch"
                      label="RF Switch"
                      color="primary"
                      :disable="isLoading"
                    />
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- X-Band 섹션 -->
          <div class="col-12 col-md-6">
            <q-card class="x-band-section">
              <q-card-section>
                <div class="text-h6 text-primary">X-Band</div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <!-- X-Band LHCP -->
                  <div class="col-12 col-sm-6">
                    <q-toggle
                      v-model="feedControls.xLHCP"
                      label="LHCP"
                      color="primary"
                      :disable="isLoading"
                    />
                  </div>
                  <!-- X-Band RHCP -->
                  <div class="col-12 col-sm-6">
                    <q-toggle
                      v-model="feedControls.xRHCP"
                      label="RHCP"
                      color="primary"
                      :disable="isLoading"
                    />
                  </div>
                </div>
              </q-card-section>
            </q-card>
          </div>

          <!-- FAN 섹션 -->
          <div class="col-12">
            <q-card class="fan-section">
              <q-card-section>
                <div class="row items-center">
                  <div class="text-h6 text-primary">Fan Control</div>
                  <q-space />
                  <q-toggle v-model="feedControls.fan" color="primary" :disable="isLoading" />
                </div>
              </q-card-section>
            </q-card>
          </div>
        </div>

        <!-- 제어 버튼 -->
        <div class="row justify-center q-mt-md">
          <q-btn
            label="Apply"
            color="primary"
            icon="send"
            :loading="isLoading"
            @click="applyFeedControls"
          />
        </div>

        <!-- 상태 메시지 표시 -->
        <div class="status-message q-mt-md" v-if="showStatusMessage">
          <q-banner :class="statusSuccess ? 'bg-positive text-white' : 'bg-negative text-white'">
            {{ statusMessage }}
          </q-banner>
        </div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/ICD'
import { useQuasar } from 'quasar'

// Quasar 인스턴스 생성
const $q = useQuasar()

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// Feed 제어 상태
const feedControls = ref({
  sLHCP: false,
  sRHCP: false,
  sRFSwitch: false,
  xLHCP: false,
  xRHCP: false,
  fan: false,
})

// 로딩 상태
const isLoading = ref(false)

// 상태 메시지
const statusMessage = ref('')
const statusSuccess = ref(true)
const statusTimestamp = ref(0)

// 상태 메시지 표시 여부 (최근 3초 이내의 메시지만 표시)
const showStatusMessage = computed(() => {
  const currentTime = Date.now()
  return currentTime - statusTimestamp.value < 3000 && statusMessage.value !== ''
})

// Feed 제어 적용 함수
const applyFeedControls = async () => {
  try {
    isLoading.value = true

    // Feed On/Off 명령 API 호출
    await icdStore.sendFeedOnOffCommand(
      feedControls.value.sLHCP,
      feedControls.value.sRHCP,
      feedControls.value.sRFSwitch,
      feedControls.value.xLHCP,
      feedControls.value.xRHCP,
      feedControls.value.fan,
    )

    // 성공 메시지 설정
    statusMessage.value = 'Feed 제어 명령이 성공적으로 전송되었습니다.'
    statusSuccess.value = true
    statusTimestamp.value = Date.now()

    // 알림 표시
    $q.notify({
      type: 'positive',
      message: 'Feed 제어 명령이 성공적으로 전송되었습니다.',
      caption: '설정이 적용되었습니다.',
    })
  } catch (error) {
    console.error('Feed 제어 명령 처리 중 오류:', error)

    // 오류 메시지 설정
    statusMessage.value = 'Feed 제어 명령 전송 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()

    // 알림 표시
    $q.notify({
      type: 'negative',
      message: 'Feed 제어 명령 전송 중 오류가 발생했습니다.',
    })
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

/* 상태 메시지 스타일 */
.status-message {
  transition: opacity 0.3s;
}

/* 카드 스타일 */
.s-band-section,
.x-band-section,
.fan-section {
  height: 100%;
}
</style>
