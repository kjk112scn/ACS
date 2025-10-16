<template>
  <div class="ephemeris-mode">
    <div class="ephemeris-container">
      <!-- 1행: Offset Controls -->
      <div class="row q-mb-sm align-center">
        <!-- Azimuth Offset - 개별 테두리 -->
        <div class="col-auto">
          <q-card flat bordered class="control-card">
            <q-card-section class="compact-control purple-1"
              style="padding: 0px 8px !important; padding-top: 0px !important; padding-bottom: 0px !important; margin: 0px !important; min-height: auto !important; height: auto !important; line-height: 1 !important; vertical-align: top !important;">
              <div class="row q-gutter-xs align-center"
                style="margin: 0px !important; padding: 0px !important; min-height: auto !important; height: auto !important;">
                <div class="col-auto position-offset-label">
                  <div class="text-subtitle2 text-weight-bold text-primary text-center">
                    Azimuth<br>Offset
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined
                    type="number" step="0.01" label="Azimuth"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
                <div class="col-auto">
                  <div class="vertical-button-group">
                    <div class="vertical-buttons">
                      <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
                      <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
                    </div>
                    <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="outputs[0]" dense outlined readonly label="Output"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Elevation Offset - 개별 테두리 -->
        <div class="col-auto" style="margin-left: 8px !important;">
          <q-card flat bordered class="control-card">
            <q-card-section class="compact-control purple-1"
              style="padding: 0px 8px !important; padding-top: 0px !important; padding-bottom: 0px !important; margin: 0px !important; min-height: auto !important; height: auto !important; line-height: 1 !important; vertical-align: top !important;">
              <div class="row q-gutter-xs align-center"
                style="margin: 0px !important; padding: 0px !important; min-height: auto !important; height: auto !important;">
                <div class="col-auto position-offset-label">
                  <div class="text-subtitle2 text-weight-bold text-primary text-center">
                    Elevation<br>Offset
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined
                    type="number" step="0.01" label="Elevation"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
                <div class="col-auto">
                  <div class="vertical-button-group">
                    <div class="vertical-buttons">
                      <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
                      <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
                    </div>
                    <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="outputs[1]" dense outlined readonly label="Output"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Tilt Offset - 개별 테두리 -->
        <div class="col-auto" style="margin-left: 8px !important;">
          <q-card flat bordered class="control-card">
            <q-card-section class="compact-control purple-1"
              style="padding: 0px 8px !important; padding-top: 0px !important; padding-bottom: 0px !important; margin: 0px !important; min-height: auto !important; height: auto !important; line-height: 1 !important; vertical-align: top !important;">
              <div class="row q-gutter-xs align-center"
                style="margin: 0px !important; padding: 0px !important; min-height: auto !important; height: auto !important;">
                <div class="col-auto position-offset-label">
                  <div class="text-subtitle2 text-weight-bold text-primary text-center">
                    Tilt<br>Offset
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined
                    type="number" step="0.01" label="Tilt"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
                <div class="col-auto">
                  <div class="vertical-button-group">
                    <div class="vertical-buttons">
                      <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
                      <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
                    </div>
                    <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="outputs[2]" dense outlined readonly label="Output"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Time Offset - 개별 테두리 -->
        <div class="col-auto" style="margin-left: 8px !important;">
          <q-card flat bordered class="control-card">
            <q-card-section class="compact-control purple-1"
              style="padding: 0px 8px !important; padding-top: 0px !important; padding-bottom: 0px !important; margin: 0px !important; min-height: auto !important; height: auto !important; line-height: 1 !important; vertical-align: top !important;">
              <div class="row q-gutter-xs align-center"
                style="margin: 0px !important; padding: 0px !important; min-height: auto !important; height: auto !important;">
                <div class="col-auto position-offset-label">
                  <div class="text-subtitle2 text-weight-bold text-primary text-center">
                    Time<br>Offset
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined
                    type="number" step="0.01" label="Time"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
                <div class="col-auto">
                  <div class="vertical-button-group">
                    <div class="vertical-buttons">
                      <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
                      <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
                    </div>
                    <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
                  </div>
                </div>
                <div class="col-auto">
                  <q-input v-model="outputs[3]" dense outlined readonly label="Result"
                    style="width: 84px !important; min-width: 84px !important; max-width: 84px !important;" />
                </div>
                <div class="col-auto">
                  <q-input v-model="formattedCalTime" dense outlined readonly label="Cal Time"
                    style="width: 190px !important; min-width: 190px !important; max-width: 190px !important;" />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- 2행: Main Content -->
      <div class="row q-col-gutter-md">
        <!-- 1번 영역: 차트가 들어갈 네모난 칸 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Position View</div>
              <div class="chart-area" ref="chartRef"></div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 2번 영역: 계산 정보 표시 영역 수정 -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">위성 추적 상태</div>

              <!-- ✅ 추적 상태 표시 -->
              <div class="info-row">
                <span class="info-label">추적 상태:</span>
                <q-chip :color="icdStore.ephemerisTrackingStateInfo.displayColor" text-color="white"
                  :label="icdStore.ephemerisTrackingStateInfo.displayLabel" size="sm" class="tracking-status-chip" />
              </div>

              <div class="ephemeris-form">
                <div class="form-row">
                  <!-- ✅ 정지궤도 정보 표시 -->
                  <div v-if="selectedScheduleInfo.isGeostationary" class="schedule-info q-mt-md">
                    <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">
                      정지궤도 위성 정보
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 이름/ID:</span>
                      <span class="info-value">{{ selectedScheduleInfo.satelliteName }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">방위각:</span>
                      <span class="info-value">{{ selectedScheduleInfo.startAzimuth.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">고도:</span>
                      <span class="info-value">{{ selectedScheduleInfo.startElevation.toFixed(2) }}°</span>
                    </div>
                  </div>

                  <!-- ✅ 기존 스케줄 정보 표시 (정지궤도가 아닌 경우) -->
                  <div v-else-if="selectedScheduleInfo.satelliteName" class="schedule-info q-mt-md">
                    <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">
                      선택된 스케줄 정보
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 이름:</span>
                      <span class="info-value">{{ selectedScheduleInfo.satelliteName }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 ID:</span>
                      <span class="info-value">{{ selectedScheduleInfo.satelliteId }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 시간:</span>
                      <span class="info-value">{{
                        formatToLocalTime(selectedScheduleInfo.startTime)
                        }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">종료 시간:</span>
                      <span class="info-value">{{
                        formatToLocalTime(selectedScheduleInfo.endTime)
                        }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">지속 시간:</span>
                      <span class="info-value">{{ selectedScheduleInfo.duration }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 방위각/고도:</span>
                      <span class="info-value">{{ selectedScheduleInfo.startAzimuth.toFixed(2) }}° /
                        {{ selectedScheduleInfo.startElevation.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">종료 방위각/고도:</span>
                      <span class="info-value">{{ selectedScheduleInfo.endAzimuth.toFixed(2) }}° /
                        {{ selectedScheduleInfo.endElevation.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">최대 고도:</span>
                      <span class="info-value">{{ selectedScheduleInfo.maxElevation.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">남은 시간:</span>
                      <span class="info-value" :class="{
                        'text-negative': timeRemaining < 0,
                        'text-positive': timeRemaining > 0,
                        'text-grey': timeRemaining === 0,
                      }">
                        {{ formatTimeRemaining(timeRemaining) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 3번 영역: TLE Data -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">TLE Data</div>
              <q-editor v-model="tleData.displayText" readonly flat dense class="tle-display q-mt-sm" :toolbar="[]"
                :definitions="{
                  bold: undefined,
                  italic: undefined,
                  strike: undefined,
                  underline: undefined,
                }" content-class="tle-content" />
              <div class="button-group q-mt-md">
                <q-btn color="primary" label="Text" @click="openTLEModal" class="q-mr-sm" />
                <q-btn color="primary" label="Select Schedule" @click="openScheduleModal" class="q-mr-sm" />
                <!-- ✅ 다운로드 버튼 추가 -->
                <q-btn color="secondary" label="Download" icon="download" @click="downloadRealtimeData"
                  class="q-mr-sm" />
              </div>
              <!-- 버튼 그룹 추가 -->
              <div class="button-group q-mt-md">
                <q-btn color="positive" label="Go" @click="handleEphemerisCommand" class="q-mr-sm" />
                <q-btn color="warning" label="Stop" @click="handleStopCommand" class="q-mr-sm" />
                <q-btn color="negative" label="Stow" @click="handleStowCommand" class="q-mr-sm" />
              </div>
              <!-- 새로운 버튼 그룹 추가 -->
              <div class="button-group q-mt-md">
                <q-btn color="info" label="Axis Calculator" icon="calculate" @click="openAxisTransformCalculator"
                  class="q-mr-sm" />
                <q-btn color="secondary" label="이론치 다운로드" icon="download" @click="exportAllMstDataToCsv"
                  :loading="isExportingCsv" />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </div>

  <!-- TLE 입력 모달 -->
  <q-dialog v-model="showTLEModal" persistent>
    <q-card class="q-pa-md" style="width: 700px; max-width: 95vw">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">TLE 입력</div>
      </q-card-section>

      <q-card-section class="q-pa-md">
        <div class="text-body2 q-mb-md">
          2줄 또는 3줄 형식의 TLE 데이터를 입력하세요. 3줄 형식인 경우 첫 번째 줄은 위성 이름으로
          처리됩니다.
          <br />예시:
          <pre class="q-mt-sm q-pa-sm bg-grey-9 text-white rounded-borders"
            style="font-size: 0.8rem; white-space: pre-wrap">
ISS (ZARYA)
1 25544U 98067A   24054.51736111  .00020125  00000+0  36182-3 0  9999
2 25544  51.6416 142.1133 0003324 324.9821 218.2594 15.49780383446574</pre>
        </div>
        <div class="tle-input-container q-mb-md">
          <q-input v-model="tempTLEData.tleText" type="textarea" filled autogrow class="tle-textarea full-width"
            style="min-height: 200px; font-family: monospace; font-size: 0.9rem" placeholder="TLE 데이터를 여기에 붙여넣으세요..."
            :input-style="'white-space: pre;'" spellcheck="false" autofocus :error="tleError !== null"
            :error-message="tleError || undefined" @keydown.ctrl.enter="addTLEData" />
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn flat label="추가" color="primary" @click="addTLEData" :loading="isProcessingTLE"
          :disable="!tempTLEData.tleText.trim()" />
        <q-btn flat label="닫기" color="primary" v-close-popup class="q-ml-sm" :disable="isProcessingTLE" />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- 스케줄 선택 모달 -->
  <q-dialog v-model="showScheduleModal" persistent maximized>
    <q-card class="q-pa-md" style="width: 1200px; max-width: 98vw; max-height: 70vh">
      <q-card-section class="bg-primary text-white">
        <div class="text-h6">Select Schedule</div>
      </q-card-section>

      <q-card-section class="q-pa-md" style="max-height: 50vh; overflow: auto">
        <q-table :rows="scheduleData" :columns="scheduleColumns" row-key="No" :loading="loadingSchedule"
          :pagination="{ rowsPerPage: 10 }" selection="single" v-model:selected="selectedSchedule"
          class="bg-grey-9 text-white" dark flat bordered>
          <template v-slot:loading>
            <q-inner-loading showing color="primary">
              <q-spinner size="50px" color="primary" />
            </q-inner-loading>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Select" color="primary" @click="selectSchedule" :disable="selectedSchedule.length === 0" />
        <q-btn flat label="Close" color="primary" v-close-popup class="q-ml-sm" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { date } from 'quasar'

import type { QTableProps } from 'quasar'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useEphemerisTrackModeStore } from '@/stores'
import { formatToLocalTime, formatTimeRemaining, getCalTimeTimestamp } from '../../utils/times'
// 인터페이스 정의 - 서비스의 타입과 동일하게 사용
import {
  ephemerisTrackService,
  type ScheduleItem,
  type RealtimeTrackingDataItem,
} from '../../services/mode/ephemerisTrackService'
import { openPopup } from '../../utils/windowUtils'
// ✅ 스토어 연동 추가
const ephemerisStore = useEphemerisTrackModeStore()

// ECharts 데이터 포인트 타입 정의
interface EChartsScatterParam {
  value: [number, number]
  dataIndex: number
  seriesIndex: number
  seriesName: string
  name: string
  color: string
  borderColor: string
  dimensionNames: string[]
  encode: Record<string, number[]>
  marker: string
  data: [number, number]
  dimensionIndex: number
}

// 스토어 인스턴스 생성
const icdStore = useICDStore()

// 차트 데이터용 인터페이스
interface TrajectoryPoint {
  Time: string
  Azimuth: number
  Elevation: number
  [key: string]: string | number | boolean | null | undefined
}

// 차트 관련 변수
const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let updateTimer: number | null = null

// TLE Data 스토어에서 상태 가져오기
const tleData = computed(() => ephemerisStore.tleDisplayData)
// Ephemeris Designation 모드 데이터 - 현재 위치 정보 표시용
const currentPosition = ref({
  azimuth: 0,
  elevation: 0,
  train: 0,
  date: date.formatDate(new Date(), 'YYYY/MM/DD'),
  time: date.formatDate(new Date(), 'HH:mm'),
})

// ✅ 스토어 상태 연동 - 탭 이동 시에도 데이터 유지
const showScheduleModal = ref(false)

const scheduleData = computed(() => ephemerisStore.masterData)
const selectedSchedule = ref<ScheduleItem[]>([])
const loadingSchedule = ref(false)

// TLE 모달 관련 상태
const showTLEModal = ref(false)
const tempTLEData = ref({
  tleText: '', // 전체 TLE 텍스트를 저장
})

// TLE 관련 상태
const tleError = ref<string | null>(null)
const isProcessingTLE = ref(false)

// CSV 내보내기 관련 상태
const isExportingCsv = ref(false)

// QTable 컬럼 타입 정의
type QTableColumn = NonNullable<QTableProps['columns']>[0]

// 스케줄 테이블 컬럼 정의
const scheduleColumns: QTableColumn[] = [
  { name: 'No', label: 'No', field: 'No', align: 'left', sortable: true },
  {
    name: 'SatelliteName',
    label: '위성 이름',
    field: 'SatelliteName',
    align: 'left',
    sortable: true,

    format: (val, row) => val || row.SatelliteID || '이름 없음',
  },
  {
    name: 'StartTime',
    label: '시작 시간',
    field: 'StartTime',
    align: 'left',
    sortable: true,
    format: (val) => formatToLocalTime(val),
  },
  {
    name: 'EndTime',
    label: '종료 시간',
    field: 'EndTime',
    align: 'left',
    sortable: true,
    format: (val) => formatToLocalTime(val),
  },
  { name: 'Duration', label: '지속 시간', field: 'Duration', align: 'left', sortable: true },
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'MaxElevation',
    align: 'left',
    sortable: true,
  },
]

// ✅ 새로운 코드로 교체:
// 스토어에서 상태 가져오기
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0.00'])
// ✅ outputs 계산된 속성 수정 - Time Result 분리
const outputs = computed(() => [
  ephemerisStore.offsetValues.azimuth,
  ephemerisStore.offsetValues.elevation,
  ephemerisStore.offsetValues.train,
  ephemerisStore.offsetValues.timeResult, // ✅ 별도 관리되는 Result 값
])
// Quasar 인스턴스 가져오기
import { useQuasar } from 'quasar'

const $q = useQuasar()

// ✅ 스토어에서 선택된 스케줄 정보 가져오기 - 탭 이동 시에도 유지
const selectedScheduleInfo = computed(() => {
  // ✅ 정지궤도 각도가 설정되어 있으면 정지궤도 정보 표시
  if (ephemerisStore.geostationaryAngles.isSet) {
    // TLE 라인에서 위성 ID 추출
    const tleLine1 = ephemerisStore.geostationaryAngles.tleLine1 || ''
    const satelliteId = tleLine1.length >= 7 ? tleLine1.substring(2, 7).trim() : 'N/A'

    // 위성 이름과 ID 구분
    const satelliteName = ephemerisStore.geostationaryAngles.satelliteName || ''

    // 표시용 이름 생성: 3줄 TLE면 "이름/ID", 2줄 TLE면 "ID"만
    const displayName = satelliteName && satelliteName !== satelliteId
      ? `${satelliteName}/${satelliteId}`
      : satelliteId

    return {
      passId: 0,
      satelliteName: displayName,
      satelliteId: satelliteId,
      originalSatelliteName: satelliteName, // 원본 이름 보존
      startTime: '',
      endTime: '',
      duration: '',
      maxElevation: ephemerisStore.geostationaryAngles.elevation,
      startTimeMs: 0,
      timeRemaining: 0,
      startAzimuth: ephemerisStore.geostationaryAngles.azimuth,
      endAzimuth: ephemerisStore.geostationaryAngles.azimuth,
      startElevation: ephemerisStore.geostationaryAngles.elevation,
      endElevation: ephemerisStore.geostationaryAngles.elevation,
      isGeostationary: true, // ✅ 정지궤도 구분 플래그
    }
  }

  // 기존 스케줄 정보 로직
  const selected = ephemerisStore.selectedSchedule
  if (selected) {
    return {
      passId: selected.No,
      satelliteName: selected.SatelliteName || selected.SatelliteID || '알 수 없음',
      satelliteId: selected.SatelliteID || 'N/A',
      startTime: selected.StartTime,
      endTime: selected.EndTime,
      duration: selected.Duration,
      maxElevation: typeof selected.MaxElevation === 'number' ? selected.MaxElevation : 0,
      startTimeMs: new Date(selected.StartTime).getTime(),
      timeRemaining: 0,
      startAzimuth: typeof selected.StartAzimuth === 'number' ? selected.StartAzimuth : 0,
      endAzimuth: typeof selected.EndAzimuth === 'number' ? selected.EndAzimuth : 0,
      startElevation: typeof selected.StartElevation === 'number' ? selected.StartElevation : 0,
      endElevation: typeof selected.EndElevation === 'number' ? selected.EndElevation : 0,
      isGeostationary: false,
    }
  }

  return {
    passId: 0,
    satelliteName: '',
    satelliteId: '',
    startTime: '',
    endTime: '',
    duration: '',
    maxElevation: 0,
    startTimeMs: 0,
    timeRemaining: 0,
    startAzimuth: 0,
    endAzimuth: 0,
    startElevation: 0,
    endElevation: 0,
    isGeostationary: false,
  }
})

// ✅ 추적 상태 변경 감지 및 경로 초기화
watch(() => icdStore.ephemerisTrackingState, (newState, oldState) => {
  console.log('🔄 추적 상태 변경:', oldState, '→', newState)

  // 추적 시작 또는 완료 시 경로 초기화
  if (newState === 'TRACKING' || newState === 'COMPLETED' || newState === 'IDLE') {
    // ✅ 현재 위치를 기준으로 경로 초기화 (0도에서 시작하는 문제 해결)
    const currentAzimuth = parseFloat(icdStore.azimuthAngle) || 0
    const currentElevation = parseFloat(icdStore.elevationAngle) || 0

    ephemerisStore.clearTrackingPath(currentAzimuth, currentElevation)
    console.log('🧹 추적 경로 초기화 완료 - 현재 위치 기준:', {
      azimuth: currentAzimuth,
      elevation: currentElevation
    })
  }
})
// ✅ 개선된 RealtimeTrackingDataItem 타입을 사용하는 CSV 다운로드 함수
const downloadCSVWithTransformations = (data: RealtimeTrackingDataItem[]) => {
  // 안전한 숫자 포맷팅 함수
  const safeToFixed = (value: number | null | undefined, digits: number = 4): string => {
    if (value === null || value === undefined || isNaN(Number(value))) {
      return '0.0000'
    }
    return Number(value).toFixed(digits)
  }

  // CSV 헤더 정의 - 원본/축변환/최종 데이터 포함
  const headers = [
    'Index', 'TheoreticalIndex', 'Timestamp', 'PassId', 'ElapsedTime(s)',

    // 원본 데이터 (변환 전)
    'OriginalAzimuth(°)', 'OriginalElevation(°)', 'OriginalRange(km)', 'OriginalAltitude(km)',

    // 축변환 데이터 (기울기 변환 적용)
    'AxisTransformedAzimuth(°)', 'AxisTransformedElevation(°)', 'AxisTransformedRange(km)', 'AxisTransformedAltitude(km)',

    // 최종 변환 데이터 (±270도 제한 적용)
    'FinalTransformedAzimuth(°)', 'FinalTransformedElevation(°)', 'FinalTransformedRange(km)', 'FinalTransformedAltitude(km)',

    // 명령 및 실제 추적 데이터
    'CmdAzimuth(°)', 'CmdElevation(°)', 'ActualAzimuth(°)', 'ActualElevation(°)',
    'TrackingAzimuthTime(s)', 'TrackingCMDAzimuth(°)', 'TrackingActualAzimuth(°)',
    'TrackingElevationTime(s)', 'TrackingCMDElevation(°)', 'TrackingActualElevation(°)',
    'TrackingTrainTime(s)', 'TrackingCMDTrain(°)', 'TrackingActualTrain(°)',

    // 오차 분석
    'AzimuthError(°)', 'ElevationError(°)',
    'OriginalToAxisTransformationError(°)', 'AxisToFinalTransformationError(°)', 'TotalTransformationError(°)',

    // 정확도 분석 (새로 추가된 필드들)
    '시간정확도(s)', 'Az_CMD정확도(°)', 'Az_Act정확도(°)', 'Az_최종정확도(°)',
    'El_CMD정확도(°)', 'El_Act정확도(°)', 'El_최종정확도(°)',

    // 변환 정보
    'TrainAngle(°)', 'TransformationType', 'HasTransformation', 'InterpolationMethod', 'InterpolationAccuracy'
  ]

  // CSV 데이터 생성 (안전한 처리 적용)
  const csvContent = [
    headers.join(','),
    ...data.map((item) =>
      [
        item.index || 0,
        item.theoreticalIndex || 0,  // ✅ 이론치 데이터 인덱스 추가
        `"${item.timestamp ? formatToLocalTime(item.timestamp) : new Date().toISOString()}"`,
        item.passId || 0,
        safeToFixed(item.elapsedTimeSeconds, 3),

        // 원본 데이터 (변환 전)
        safeToFixed(item.originalAzimuth, 6),
        safeToFixed(item.originalElevation, 6),
        safeToFixed(item.originalRange, 6),
        safeToFixed(item.originalAltitude, 6),

        // 축변환 데이터 (기울기 변환 적용)
        safeToFixed(item.axisTransformedAzimuth, 6),
        safeToFixed(item.axisTransformedElevation, 6),
        safeToFixed(item.axisTransformedRange, 6),
        safeToFixed(item.axisTransformedAltitude, 6),

        // 최종 변환 데이터 (±270도 제한 적용)
        safeToFixed(item.finalTransformedAzimuth, 6),
        safeToFixed(item.finalTransformedElevation, 6),
        safeToFixed(item.finalTransformedRange, 6),
        safeToFixed(item.finalTransformedAltitude, 6),

        // 명령 및 실제 추적 데이터
        safeToFixed(item.cmdAz, 6),
        safeToFixed(item.cmdEl, 6),
        safeToFixed(item.actualAz, 6),
        safeToFixed(item.actualEl, 6),
        safeToFixed(item.trackingAzimuthTime, 2),
        safeToFixed(item.trackingCMDAzimuthAngle, 6),
        safeToFixed(item.trackingActualAzimuthAngle, 6),
        safeToFixed(item.trackingElevationTime, 2),
        safeToFixed(item.trackingCMDElevationAngle, 6),
        safeToFixed(item.trackingActualElevationAngle, 6),
        safeToFixed(item.trackingTrainTime, 2),
        safeToFixed(item.trackingCMDTrainAngle, 6),
        safeToFixed(item.trackingActualTrainAngle, 6),

        // 오차 분석
        safeToFixed(item.azimuthError, 6),
        safeToFixed(item.elevationError, 6),
        safeToFixed(item.originalToAxisTransformationError, 6),
        safeToFixed(item.axisToFinalTransformationError, 6),
        safeToFixed(item.totalTransformationError, 6),

        // 정확도 분석 (새로 추가된 필드들)
        safeToFixed(item.timeAccuracy, 6),
        safeToFixed(item.azCmdAccuracy, 6),
        safeToFixed(item.azActAccuracy, 6),
        safeToFixed(item.azFinalAccuracy, 6),
        safeToFixed(item.elCmdAccuracy, 6),
        safeToFixed(item.elActAccuracy, 6),
        safeToFixed(item.elFinalAccuracy, 6),

        // 변환 정보
        safeToFixed(item.trainAngle, 6),
        `"${item.transformationType || 'none'}"`,
        item.hasTransformation ? 'true' : 'false',
        `"${item.interpolationMethod || 'linear'}"`,
        safeToFixed(item.interpolationAccuracy, 6)
      ].join(','),
    ),
  ].join('\n')

  // BOM 추가 (한글 깨짐 방지)
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })

  // 파일명 생성 (현재 시간 포함)
  const now = new Date()
  const timestamp = now.toISOString().replace(/[:.]/g, '-').slice(0, 19)
  const filename = `realtime_tracking_data_with_transformations_${timestamp}.csv`

  // 다운로드 실행
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const downloadRealtimeData = async () => {
  try {
    // Loading 대신 notify로 시작 알림
    $q.notify({
      type: 'info',
      message: '실시간 추적 데이터를 조회하고 있습니다...',
      timeout: 2000,
    })

    // ✅ 기존 API 호출 - generateRealtimeTrackingCsv와 연계
    const response = await ephemerisTrackService.fetchRealtimeTrackingData()

    if (response.data && response.data.length > 0) {
      // ✅ 클라이언트에서 CSV 생성 및 다운로드
      downloadCSVWithTransformations(response.data)

      $q.notify({
        type: 'positive',
        message: `${response.totalCount || 0}개의 실시간 추적 데이터를 다운로드했습니다`,
        timeout: 5000,
      })

      console.log('실시간 추적 데이터 다운로드 결과:', response)
    } else {
      $q.notify({
        type: 'warning',
        message: '다운로드할 실시간 추적 데이터가 없습니다',
        timeout: 3000,
      })
    }
  } catch (error) {
    console.error('실시간 추적 데이터 다운로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '실시간 추적 데이터 다운로드에 실패했습니다',
      timeout: 5000,
    })
  }
}


// 남은 시간 계산을 위한 상태
const timeRemaining = ref(0)
let timeUpdateTimer: number | null = null

// ✅ 성능 모니터링 시스템
// ✅ TypeScript 안전한 성능 모니터링 시스템
class PerformanceMonitor {
  private frameTimings: number[] = []
  private gcDetectionThreshold = 50
  private lastFrameTime = 0
  private stats = {
    totalFrames: 0,
    gcSuspectedFrames: 0,
    averageFrameTime: 0,
    maxFrameTime: 0,
    memorySnapshots: [] as Array<{
      timestamp: number
      used: string
      total: string
      frameCount: number
    }>,
  }

  measureFrame(callback: () => void) {
    const startTime = performance.now()

    callback()

    const endTime = performance.now()
    const frameTime = endTime - startTime

    this.frameTimings.push(frameTime)
    if (this.frameTimings.length > 100) {
      this.frameTimings.shift()
    }

    this.stats.totalFrames++
    this.stats.maxFrameTime = Math.max(this.stats.maxFrameTime, frameTime)
    this.stats.averageFrameTime =
      this.frameTimings.reduce((a, b) => a + b, 0) / this.frameTimings.length

    if (frameTime > this.gcDetectionThreshold) {
      this.stats.gcSuspectedFrames++
      console.warn(`🐌 느린 프레임 감지: ${frameTime.toFixed(2)}ms`, {
        메모리: this.getMemoryInfo(),
        프레임비율: `${this.stats.gcSuspectedFrames}/${this.stats.totalFrames}`,
        평균프레임시간: this.stats.averageFrameTime.toFixed(2) + 'ms',
      })
    }

    if (this.stats.totalFrames % 100 === 0) {
      this.takeMemorySnapshot()
    }
  }

  private getMemoryInfo(): { used: string; total: string } | null {
    // ✅ performance.memory 타입 체크
    if ('memory' in performance && performance.memory) {
      const memory = performance.memory as {
        usedJSHeapSize: number
        totalJSHeapSize: number
        jsHeapSizeLimit: number
      }

      return {
        used: Math.round(memory.usedJSHeapSize / 1024 / 1024) + 'MB',
        total: Math.round(memory.totalJSHeapSize / 1024 / 1024) + 'MB',
      }
    }
    return null
  }

  private takeMemorySnapshot() {
    const memInfo = this.getMemoryInfo()
    if (memInfo) {
      this.stats.memorySnapshots.push({
        timestamp: Date.now(),
        ...memInfo,
        frameCount: this.stats.totalFrames,
      })

      if (this.stats.memorySnapshots.length > 10) {
        this.stats.memorySnapshots.shift()
      }

      if (this.stats.memorySnapshots.length >= 3) {
        const recent = this.stats.memorySnapshots.slice(-3)
        const memoryTrend = recent.map((s) => parseInt(s.used))
        const isIncreasing = memoryTrend.every(
          (val, i) => i === 0 || val >= (memoryTrend[i - 1] ?? 0),
        )

        if (isIncreasing) {
          console.warn('📈 메모리 지속 증가 감지:', memoryTrend)
        }
      }
    }
  }

  getReport() {
    return {
      ...this.stats,
      gcSuspectedRatio:
        ((this.stats.gcSuspectedFrames / this.stats.totalFrames) * 100).toFixed(2) + '%',
    }
  }
}

const perfMonitor = new PerformanceMonitor()

// ✅ 객체 풀링으로 GC 압박 최소화
// ✅ TypeScript 안전한 차트 업데이트 풀
class ChartUpdatePool {
  private positionData: [number, number][] = [[0, 0]]
  private trackingData: [number, number][] = []
  private updateOption: {
    series: Array<{ data?: [number, number][] }>
  }

  constructor() {
    this.updateOption = {
      series: [{ data: this.positionData }, {}, { data: this.trackingData }, {}],
    }
  }

  updatePosition(elevation: number, azimuth: number) {
    // ✅ 배열 존재 확인
    if (this.positionData.length > 0 && this.positionData[0]) {
      this.positionData[0][0] = elevation
      this.positionData[0][1] = azimuth
    } else {
      this.positionData = [[elevation, azimuth]]
      // 시리즈 데이터 참조 업데이트
      if (this.updateOption.series[0]) {
        this.updateOption.series[0].data = this.positionData
      }
    }
    return this.updateOption
  }

  updateTrackingPath(newPath: [number, number][]) {
    // ✅ 안전한 배열 업데이트
    this.trackingData.length = 0
    if (Array.isArray(newPath)) {
      this.trackingData.push(...newPath)
    }
    return this.updateOption
  }
}

const chartPool = new ChartUpdatePool()

// ✅ 최적화된 차트 업데이트
// ✅ 안전한 차트 업데이트
const updateChart = () => {
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  perfMonitor.measureFrame(() => {
    try {
      // ✅ 추적 상태에 따라 다른 데이터 소스 사용
      const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive

      const azimuth = isTrackingActive
        ? parseFloat(icdStore.trackingActualAzimuthAngle) || 0
        : parseFloat(icdStore.azimuthAngle) || 0
      const elevation = isTrackingActive
        ? parseFloat(icdStore.trackingActualElevationAngle) || 0
        : parseFloat(icdStore.elevationAngle) || 0

      const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
      const normalizedEl = Math.max(0, Math.min(90, elevation))

      // ✅ 안전한 속성 업데이트 (원본 값 표시로 일관성 유지)
      if (currentPosition.value) {
        currentPosition.value.azimuth = azimuth  // 원본 값 (-180.14°)
        currentPosition.value.elevation = elevation
        currentPosition.value.date = date.formatDate(new Date(), 'YYYY/MM/DD')
        currentPosition.value.time = date.formatDate(new Date(), 'HH:mm:ss')
      }

      // ✅ 안전한 상태 체크 (실제 추적 상태 확인)
      if (icdStore.ephemerisTrackingState === "TRACKING") {
        void ephemerisStore.updateTrackingPath(azimuth, elevation)
      }

      // ✅ 안전한 차트 옵션 업데이트
      const option = chartPool.updatePosition(normalizedEl, normalizedAz)
      if (ephemerisStore.trackingPath?.sampledPath) {
        chartPool.updateTrackingPath(ephemerisStore.trackingPath.sampledPath as [number, number][])
      }

      // ✅ 차트가 여전히 존재하는지 확인
      if (chart && !chart.isDisposed()) {
        chart.setOption(option, false, true)
      }
    } catch (error) {
      console.error('차트 업데이트 중 오류 발생:', error)
    }
  })
}
// ✅ 차트 초기화 함수 수정
const initChart = () => {
  if (!chartRef.value) return

  // 기존 차트 인스턴스가 있으면 제거
  if (chart) {
    chart.dispose()
  }

  // 차트 인스턴스 생성
  chart = echarts.init(chartRef.value)
  console.log('차트 인스턴스 생성됨')

  // 차트 옵션 설정
  const option = {
    backgroundColor: 'transparent',
    grid: {
      containLabel: true,
    },
    polar: {
      radius: ['0%', '80%'],
      center: ['50%', '50%'],
    },
    angleAxis: {
      type: 'value',
      startAngle: 90,
      clockwise: true,
      min: 0,
      max: 360,
      animation: false, // ✅ 애니메이션 완전 비활성화
      axisLine: {
        show: true,
        lineStyle: {
          color: '#555',
        },
      },
      axisTick: {
        show: true,
        interval: 30,
        lineStyle: {
          color: '#555',
        },
      },
      axisLabel: {
        interval: 30,
        formatter: function (value: number) {
          if (value === 0) return 'N (0°)'
          if (value === 90) return 'E (90°)'
          if (value === 180) return 'S (180°)'
          if (value === 270) return 'W (270°)'
          if (value === 45) return 'NE (45°)'
          if (value === 135) return 'SE (135°)'
          if (value === 225) return 'SW (225°)'
          if (value === 315) return 'NW (315°)'
          if (value % 30 === 0) return value + '°'
          return ''
        },
        color: '#999',
        fontSize: 10,
        distance: 10,
      },
      splitLine: {
        show: true,
        interval: 30,
        lineStyle: {
          color: '#555',
          type: 'dashed',
          width: 1,
        },
      },
    },
    radiusAxis: {
      type: 'value',
      min: 0,
      max: 90,
      inverse: true,
      animation: false, // ✅ 애니메이션 완전 비활성화
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        formatter: '{value}°',
        color: '#999',
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#555',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: '실시간 추적 위치',
        type: 'scatter',
        coordinateSystem: 'polar',
        symbol: 'circle',
        symbolSize: 15,
        animation: false, // ✅ 애니메이션 완전 비활성화
        itemStyle: {
          color: '#ff5722',
        },
        data: [[0, 0]],
        emphasis: {
          itemStyle: {
            color: '#ff9800',
            borderColor: '#fff',
            borderWidth: 2,
          },
        },
        label: {
          show: true,
          formatter: function (params: EChartsScatterParam) {
            // ✅ 원본 값 표시 (정규화된 값이 아닌)
            const originalAz = currentPosition.value?.azimuth || params.value[1]
            const originalEl = currentPosition.value?.elevation || params.value[0]
            return `Az: ${originalAz.toFixed(2)}°\nEl: ${originalEl.toFixed(2)}°`
          },
          position: 'top',
          distance: 5,
          color: '#fff',
          backgroundColor: 'rgba(0,0,0,0.7)',
          padding: [4, 8],
          borderRadius: 4,
          fontSize: 10,
        },
        zlevel: 3,
      },
      {
        name: '위치 선',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false, // ✅ 애니메이션 완전 비활성화
        lineStyle: {
          color: '#ff5722',
          width: 2,
          type: 'dashed',
        },
        data: [
          [0, 0],
          [0, 0],
        ],
        zlevel: 2,
      },
      {
        name: '실시간 추적 경로',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false, // ✅ 애니메이션 완전 비활성화
        lineStyle: {
          color: '#ffffff',
          width: 2, // ✅ 3 → 2로 줄여서 렌더링 부하 감소
          opacity: 0.8,
        },
        data: [],
        zlevel: 2,
      },
      {
        name: '위성 궤적',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false, // ✅ 애니메이션 완전 비활성화
        lineStyle: {
          color: '#2196f3',
          width: 2,
        },
        data: [],
        zlevel: 1,
      },
    ],
  }

  // 차트 옵션 적용
  chart.setOption(option)
  console.log('차트 옵션 적용됨')

  // 명시적으로 리사이즈 호출
  setTimeout(() => {
    chart?.resize()
  }, 0)

  // 윈도우 리사이즈 이벤트에 대응
  window.addEventListener('resize', () => {
    chart?.resize()
  })
}

// ✅ 최적화된 차트 업데이트 함수 (완전 교체)
// ✅ 타입 안전한 객체 재사용 변수들로 수정

// ✅ 성능 측정 변수들 추가
/* const performanceStats = {
  updateChartTime: 0,
  trackingPathUpdateTime: 0,
  chartSetOptionTime: 0,
  totalUpdateCount: 0,
  slowUpdateCount: 0,
} */

// ✅ updateChart 함수 - 비동기 Worker 활용
/* const updateChart = () => {
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  try {
    let azimuth = 0
    let elevation = 0

    // ✅ 추적 상태에 따라 다른 데이터 소스 사용
    const isTrackingActive = icdStore.ephemerisTrackingState === "TRACKING" || icdStore.passScheduleStatusInfo.isActive

    azimuth = isTrackingActive
      ? parseFloat(icdStore.trackingActualAzimuthAngle) || 0
      : parseFloat(icdStore.azimuthAngle) || 0
    elevation = isTrackingActive
      ? parseFloat(icdStore.trackingActualElevationAngle) || 0
      : parseFloat(icdStore.elevationAngle) || 0

    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    // 현재 위치 정보 업데이트 (원본 값 표시)
    currentPosition.value.azimuth = azimuth  // 원본 값 (정규화 전)
    currentPosition.value.elevation = elevation
    currentPosition.value.date = date.formatDate(new Date(), 'YYYY/MM/DD')
    currentPosition.value.time = date.formatDate(new Date(), 'HH:mm:ss')

    // ✅ 추적 중일 때 Worker를 통한 비동기 경로 처리
    if (icdStore.ephemerisTrackingState === "TRACKING") {
      // ✅ 비동기 호출이지만 결과를 기다리지 않음 (성능 최적화)
      void ephemerisStore.updateTrackingPath(azimuth, elevation)
    }

    // ✅ 차트 업데이트 (Worker에서 처리된 결과 사용)
    const updateOption = {
      series: [
        {
          data: [[normalizedEl, normalizedAz]],
        },
        {},
        {
          // ✅ Worker가 처리한 최적화된 경로 데이터 사용
          data: [...ephemerisStore.trackingPath.sampledPath],
        },
        {},
      ],
    } as unknown as Parameters<typeof chart.setOption>[0]

    chart.setOption(updateOption)
  } catch (error) {
    console.error('차트 업데이트 중 오류 발생:', error)
  }
}
 */
// 궤적 라인을 차트에 추가하는 함수@
const updateChartWithTrajectory = (data: TrajectoryPoint[]) => {
  if (!chart) {
    console.error('차트가 초기화되지 않았습니다.')
    return
  }

  console.log('궤적 데이터 처리 시작:', data.length, '개의 포인트')

  try {
    const trajectoryPoints = data.map((point) => {
      const az = typeof point.Azimuth === 'number' ? point.Azimuth : 0
      const el = typeof point.Elevation === 'number' ? point.Elevation : 0

      // ✅ DashboardPage와 동일한 정규화 방식
      const normalizedAz = az < 0 ? az + 360 : az
      const normalizedEl = Math.max(0, Math.min(90, el))

      // ✅ [elevation, azimuth] 순서로 반환 (극좌표계: [radius, angle])
      return [normalizedEl, normalizedAz]
    })

    console.log('생성된 궤적 포인트 샘플:', trajectoryPoints.slice(0, 5))

    // 차트 옵션 업데이트 - 네 번째 시리즈(궤적 라인)만 업데이트
    const trajectoryOption = {
      series: [
        {}, // 첫 번째 시리즈는 그대로 유지
        {}, // 두 번째 시리즈는 그대로 유지
        {}, // 세 번째 시리즈는 그대로 유지
        {
          // 네 번째 시리즈(궤적 라인) 업데이트
          data: trajectoryPoints,
        },
      ],
    } as unknown as Parameters<typeof chart.setOption>[0]

    chart.setOption(trajectoryOption)

    console.log('차트 옵션 업데이트 완료')
  } catch (error) {
    console.error('차트 옵션 업데이트 중 오류 발생:', error)
  }
}
// ✅ 개선된 시간 계산 함수 수정
const updateTimeRemaining = () => {
  if (selectedScheduleInfo.value.startTimeMs > 0) {
    try {
      const currentCalTime = getCalTimeTimestamp(icdStore.resultTimeOffsetCalTime)
      const remainingMs = selectedScheduleInfo.value.startTimeMs - currentCalTime
      timeRemaining.value = remainingMs
    } catch (error) {
      console.error('시간 계산 오류:', error)
      const clientTime = Date.now()
      timeRemaining.value = Math.max(0, selectedScheduleInfo.value.startTimeMs - clientTime)
    }
  }
}

// ===== 스토어 연동 메서드들 =====

// ✅ 스케줄 데이터 로드 - 스토어 사용
const loadScheduleData = async () => {
  loadingSchedule.value = true
  try {
    await ephemerisStore.loadMasterData(true)
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 데이터를 불러오는데 실패했습니다',
    })
  } finally {
    loadingSchedule.value = false
  }
}

// ✅ 스케줄 선택 - 스토어에 저장하여 탭 이동 시에도 유지
const selectSchedule = async () => {
  if (selectedSchedule.value.length === 0) return

  try {
    const selectedItem = selectedSchedule.value[0]
    if (!selectedItem) return

    // ✅ 정지궤도 상태 초기화 (스케줄 선택 시)
    ephemerisStore.resetGeostationaryAngles()

    // 스토어에 선택된 스케줄 저장 (탭 이동 시에도 유지됨)
    await ephemerisStore.selectSchedule(selectedItem)

    // 상세 데이터 로드
    // 스토어의 detailData는 selectSchedule 메서드 내에서 이미 로드됨
    const detailData = ephemerisStore.detailData

    // 차트 업데이트
    if (detailData && detailData.length > 0 && chart) {
      updateChartWithTrajectory([...detailData] as TrajectoryPoint[])
    }

    $q.notify({
      type: 'positive',
      message: `${selectedItem.SatelliteName || selectedItem.SatelliteID} 스케줄이 선택되었습니다`,
    })

    showScheduleModal.value = false
  } catch (error) {
    console.error('스케줄 선택 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 선택에 실패했습니다',
    })
  }
}

// ===== 기존 메서드들 유지 =====

// 입력값 업데이트 함수들
// 증가 함수 - 입력된 값만큼 증가
const increment = async (index: number) => {
  // 현재 출력값 (현재 상태)
  const currentOutput = parseFloat(outputs.value[index] || '0')
  console.log('currentOutput:', currentOutput)
  // 입력된 값 (증가량)
  const inputValue = parseFloat(inputs.value[index] || '0')
  console.log('inputValue:', inputValue)
  // 새로운 값 계산 (현재 출력값 + 입력된 값)
  const newValue = (currentOutput + inputValue).toFixed(2)
  console.log('newValue:', newValue)
  // 출력값 업데이트
  outputs.value[index] = newValue

  // 오프셋 업데이트 (서버에 전송)
  await updateOffset(index, newValue)
}

// 감소 함수 - 입력된 값만큼 감소
const decrement = async (index: number) => {
  // 현재 출력값 (현재 상태)
  const currentOutput = parseFloat(outputs.value[index] || '0')

  // 입력된 값 (감소량)
  const inputValue = parseFloat(inputs.value[index] || '0')

  // 새로운 값 계산 (현재 출력값 - 입력된 값)
  const newValue = (currentOutput - inputValue).toFixed(2)

  // 출력값 업데이트
  outputs.value[index] = newValue

  // 오프셋 업데이트 (서버에 전송)
  await updateOffset(index, newValue)
}

// 리셋 함수
const reset = async (index: number) => {
  inputs.value[index] = '0.00'
  await updateOffset(index, '0.00')
}
// ✅ updateOffset 함수 수정 - Time 처리 분리
const updateOffset = async (index: number, value: string) => {
  try {
    // ✅ 디버깅 로그 추가
    console.log('updateOffset 호출됨:', {
      index,
      value,
      valueType: typeof value,
      inputs3: inputs.value[3],
      currentTimeResult: ephemerisStore.offsetValues.timeResult,
    })

    const numValue = Number(parseFloat(value).toFixed(2)) || 0
    console.log('계산된 numValue:', numValue)

    const offsetTypes = ['azimuth', 'elevation', 'train', 'time'] as const
    const offsetType = offsetTypes[index]

    if (!offsetType) {
      console.error('Invalid offset index:', index)
      return
    }

    if (index === 3) {
      const timeInputValue = inputs.value[3] || '0.00'
      ephemerisStore.updateOffsetValues('time', timeInputValue)
      try {
        await ephemerisStore.sendTimeOffset(numValue)
        ephemerisStore.updateOffsetValues('timeResult', numValue.toFixed(2))
        console.log('Time Result 업데이트:', numValue.toFixed(2))
      } catch (error) {
        console.error('Time offset command failed:', error)
      }
      return
    }

    // Position Offset 처리 (azimuth, elevation, train)
    ephemerisStore.updateOffsetValues(offsetType, numValue.toFixed(2))

    const azOffset = Number((parseFloat(ephemerisStore.offsetValues.azimuth) || 0).toFixed(2))
    const elOffset = Number((parseFloat(ephemerisStore.offsetValues.elevation) || 0).toFixed(2))
    const trainOffset = Number((parseFloat(ephemerisStore.offsetValues.train) || 0).toFixed(2))

    await icdStore.sendPositionOffsetCommand(azOffset, elOffset, trainOffset)
  } catch (error) {
    console.error('Error updating offset:', error)
  }
}
// 입력값이 변경될 때 호출되는 함수
const onInputChange = (index: number, value: string) => {
  console.log('onInputChange 호출:', { index, value, inputs: inputs.value })
  inputs.value[index] = value
  void updateOffset(index, value)
}

// 서버 시간 포맷팅을 위한 계산된 속성
const formattedCalTime = computed(() => {
  const calTime = icdStore.resultTimeOffsetCalTime
  if (!calTime) return ''
  try {
    // 서버 시간 파싱
    const dateObj = new Date(calTime)

    // 유효한 날짜인지 확인
    if (isNaN(dateObj.getTime())) {
      return calTime // 유효하지 않은 날짜면 원본 반환
    }

    // UTC 기준으로 시간 형식 지정
    const utcYear = dateObj.getFullYear()
    const utcMonth = String(dateObj.getMonth() + 1).padStart(2, '0')
    const utcDay = String(dateObj.getDate()).padStart(2, '0')
    const utcHours = String(dateObj.getHours()).padStart(2, '0')
    const utcMinutes = String(dateObj.getMinutes()).padStart(2, '0')
    const utcSeconds = String(dateObj.getSeconds()).padStart(2, '0')
    const utcMilliseconds = String(dateObj.getMilliseconds()).padStart(3, '0')

    // YYYY-MM-DD HH:MM:SS.mmm (UTC) 형식
    return `${utcYear}-${utcMonth}-${utcDay} ${utcHours}:${utcMinutes}:${utcSeconds}.${utcMilliseconds} `
  } catch (e) {
    console.error('Error formatting cal time:', e)
    return calTime
  }
})

// TLE 관련 함수들
const openTLEModal = () => {
  showTLEModal.value = true
  tempTLEData.value.tleText = ''
  tleError.value = null
}

// 정지궤도 판별 함수 (평균운동만 사용)
const isGeostationaryOrbit = (tleText: string): boolean => {
  try {
    const lines = tleText.trim().split('\n').filter(line => line.trim() !== '')

    if (lines.length < 2) return false

    // TLE Line 2 추출 (평균운동이 있는 라인)
    const tleLine2 = lines.length >= 3 ? lines[2] : lines[1]

    if (!tleLine2 || tleLine2.length < 63) return false

    // 평균운동(Mean Motion) 추출 (Line 2의 52-63번째 문자)
    const meanMotionStr = tleLine2.substring(52, 63).trim()
    const meanMotion = parseFloat(meanMotionStr)

    // 정지궤도 판별: 평균운동이 1.0027 rev/day에 가까운지 확인
    const isGEO = Math.abs(meanMotion - 1.0027) <= 0.1

    console.log('정지궤도 판별:', {
      meanMotion,
      isGEO,
      tleLine2: tleLine2.substring(52, 63)
    })

    return isGEO
  } catch (error) {
    console.error('정지궤도 판별 중 오류:', error)
    return false
  }
}

const addTLEData = async () => {
  if (!tempTLEData.value.tleText.trim()) {
    tleError.value = 'TLE 데이터를 입력하세요'
    return
  }

  isProcessingTLE.value = true
  tleError.value = null

  try {
    // 정지궤도 여부 확인 - 전체 TLE 텍스트 전달
    const isGEO = isGeostationaryOrbit(tempTLEData.value.tleText)

    if (isGEO) {
      console.log('정지궤도 TLE 감지됨 - 정지궤도 각도 계산')

      // TLE 파싱
      const lines = tempTLEData.value.tleText.trim().split('\n').filter(line => line.trim() !== '')

      // TLE 라인 추출 (3줄 형식인 경우 위성 이름 제외)
      const tleLine1 = lines.length >= 3 ? lines[1] : lines[0]
      const tleLine2 = lines.length >= 3 ? lines[2] : lines[1]

      // TLE 라인이 유효한지 확인
      if (!tleLine1 || !tleLine2) {
        throw new Error('유효하지 않은 TLE 데이터입니다')
      }

      // ✅ 위성 이름과 ID 추출
      let satelliteName = ''
      const satelliteId = tleLine1.substring(2, 7).trim() // TLE Line 1에서 위성 ID 추출

      if (lines.length >= 3 && lines[0]) {
        // 3줄 형식: 첫 번째 줄이 위성 이름
        satelliteName = lines[0].trim()
      } else {
        // 2줄 형식: 위성 이름은 ID와 동일하게 설정 (구분을 위해)
        satelliteName = satelliteId
      }

      // 정지궤도 각도 계산 (추적 시작하지 않음)
      await ephemerisStore.calculateGeostationaryAngles(tleLine1, tleLine2, satelliteName)

      // ✅ TLE 표시 데이터 업데이트
      ephemerisStore.updateTLEDisplayData({
        displayText: tempTLEData.value.tleText,
        tleLine1: tleLine1,
        tleLine2: tleLine2,
        satelliteName: satelliteName,
      })

      // ✅ 정지궤도 TLE 입력 시 스케줄 데이터 완전 초기화
      ephemerisStore.clearScheduleData()
    }
    else {
      await ephemerisStore.processTLEData(tempTLEData.value.tleText)

      // ✅ 저궤도 TLE 입력 시 항상 스케줄 데이터 로드 (강제 새로고침)
      await loadScheduleData()
    }

    $q.notify({
      type: 'positive',
      message: `TLE 데이터가 성공적으로 처리되었습니다${isGEO ? ' (정지궤도)' : ''}`,
    })

    showTLEModal.value = false
  } catch (error) {
    console.error('TLE 처리 실패:', error)
    tleError.value = error instanceof Error ? error.message : 'TLE 데이터 처리에 실패했습니다'
  } finally {
    isProcessingTLE.value = false
  }
}

// 스케줄 모달 관련
const openScheduleModal = async () => {
  showScheduleModal.value = true

  // ✅ 정지궤도 상태가 아닐 때만 스케줄 데이터 로드
  if (!ephemerisStore.geostationaryAngles.isSet) {
    await loadScheduleData()
  }
}

// 명령 실행 함수들
const handleEphemerisCommand = async () => {
  try {
    // ✅ 정지궤도 각도가 설정되어 있으면 정지궤도 추적 시작
    if (ephemerisStore.geostationaryAngles.isSet) {
      // 정지궤도 추적 활성화
      ephemerisStore.activateGeostationaryTracking()

      // 백엔드에 추적 시작 명령 전송
      if (ephemerisStore.geostationaryAngles.tleLine1 && ephemerisStore.geostationaryAngles.tleLine2) {
        await ephemerisStore.startGeostationaryTracking(
          ephemerisStore.geostationaryAngles.tleLine1,
          ephemerisStore.geostationaryAngles.tleLine2
        )
      }

      $q.notify({
        type: 'positive',
        message: `정지궤도 위성(${ephemerisStore.geostationaryAngles.satelliteName}) 추적이 활성화되었습니다`,
      })
      return
    }

    // 기존 스케줄 추적 로직
    if (!selectedScheduleInfo.value.passId) {
      $q.notify({
        type: 'warning',
        message: '먼저 스케줄을 선택하거나 TLE를 입력하세요',
      })
      return
    }

    // ✅ 추적 시작 전 경로 초기화 (현재 위치 기준)
    const currentAzimuth = parseFloat(icdStore.azimuthAngle) || 0
    const currentElevation = parseFloat(icdStore.elevationAngle) || 0

    ephemerisStore.clearTrackingPath(currentAzimuth, currentElevation)
    await ephemerisStore.startTracking()

    $q.notify({
      type: 'positive',
      message: 'Ephemeris 추적이 시작되었습니다',
    })
  } catch (error) {
    console.error('Failed to start tracking:', error)
    $q.notify({
      type: 'negative',
      message: '추적 시작에 실패했습니다',
    })
  }
}

const handleStopCommand = async () => {
  try {
    // ✅ 기존 ephemeris 추적 중지 API 사용 (하드웨어 + 소프트웨어 상태 모두 처리)
    await ephemerisTrackService.stopEphemerisTracking()

    // ✅ 하드웨어 정지 명령도 함께 전송
    await icdStore.stopCommand(true, true, true)

    // ✅ 프론트엔드 상태 업데이트
    await ephemerisStore.stopTracking()

    // ✅ Stop 버튼 클릭 시 실시간 경로 초기화
    ephemerisStore.clearTrackingPath()
    console.log('🛑 Stop 버튼 클릭 - 추적 중지 및 상태 변경')

    $q.notify({
      type: 'positive',
      message: '정지 명령이 전송되었습니다',
    })
  } catch (error) {
    console.error('Failed to send stop command:', error)
    $q.notify({
      type: 'negative',
      message: '정지 명령 전송에 실패했습니다',
    })
  }
}

const handleStowCommand = async () => {
  try {
    await icdStore.stowCommand()

    $q.notify({
      type: 'positive',
      message: 'Stow 명령이 전송되었습니다',
    })
  } catch (error) {
    console.error('Failed to send stow command:', error)
    $q.notify({
      type: 'negative',
      message: 'Stow 명령 전송에 실패했습니다',
    })
  }
}

// ATC 팝업 열기
const openAxisTransformCalculator = () => {
  try {
    openPopup('axis-transform-calculator', {
      width: 800,
      height: 600,
      title: '3축 변환 계산기'
    })
  } catch (error) {
    console.error('ATC 팝업 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: 'ATC 팝업을 열 수 없습니다',
      timeout: 3000
    })
  }
}

// 모든 MST 데이터를 CSV로 내보내기
const exportAllMstDataToCsv = async () => {
  if (isExportingCsv.value) return

  isExportingCsv.value = true

  try {
    $q.notify({
      type: 'info',
      message: '이론치 데이터를 CSV로 내보내는 중...',
      timeout: 2000
    })

    const response = await ephemerisTrackService.exportAllMstDataToCsv()

    if (response.success) {
      $q.notify({
        type: 'positive',
        message: `이론치 데이터 내보내기 완료! 총 ${response.totalMstCount}개 MST, ${response.successCount}개 성공`,
        timeout: 5000
      })

      console.log('CSV 내보내기 결과:', response)
    } else {
      $q.notify({
        type: 'negative',
        message: `이론치 데이터 내보내기 실패: ${response.error || '알 수 없는 오류'}`,
        timeout: 5000
      })
    }
  } catch (error) {
    console.error('CSV 내보내기 실패:', error)
    $q.notify({
      type: 'negative',
      message: '이론치 데이터 내보내기 중 오류가 발생했습니다',
      timeout: 5000
    })
  } finally {
    isExportingCsv.value = false
  }
}
// ✅ 메인 스레드 블로킹 감지
let mainThreadBlockingDetector: number | null = null

const startMainThreadMonitoring = () => {
  let lastCheck = performance.now()

  const checkMainThread = () => {
    const currentTime = performance.now()
    const timeDiff = currentTime - lastCheck

    // ✅ 예상보다 오래 걸렸다면 메인 스레드가 블로킹되었음
    if (timeDiff > 50) {
      // 10ms 체크 간격에서 50ms 이상이면 블로킹
      console.warn(`🚫 메인 스레드 블로킹 감지: ${timeDiff.toFixed(2)}ms`)
    }

    lastCheck = currentTime
    mainThreadBlockingDetector = requestAnimationFrame(checkMainThread)
  }

  mainThreadBlockingDetector = requestAnimationFrame(checkMainThread)
}
let lastTimerExecution = 0
const timerIntervalStats = {
  totalExecutions: 0,
  totalInterval: 0,
  maxInterval: 0,
  minInterval: Infinity,
}

// ===== 라이프사이클 훅 =====

onMounted(async () => {
  console.log('EphemerisDesignation 컴포넌트 마운트됨')
  // ✅ 메인 스레드 모니터링 시작
  startMainThreadMonitoring()
  // 차트 초기화
  setTimeout(() => {
    initChart()
  }, 100)
  // ✅ 스토어에서 오프셋 값 복원
  inputs.value = [
    ephemerisStore.offsetValues.azimuth,
    ephemerisStore.offsetValues.elevation,
    ephemerisStore.offsetValues.train,
    ephemerisStore.offsetValues.time,
  ]

  // ✅ 스토어에 데이터가 없으면 로드 (탭 이동 시에도 데이터 유지)
  if (ephemerisStore.masterData.length === 0) {
    await loadScheduleData()
  }

  // ✅ 이미 선택된 스케줄이 있으면 차트 업데이트
  if (ephemerisStore.selectedSchedule && ephemerisStore.detailData.length > 0) {
    setTimeout(() => {
      if (chart) {
        updateChartWithTrajectory([...ephemerisStore.detailData] as TrajectoryPoint[])
      }
    }, 200)
  }
  // ✅ 추가: 기존 추적 경로가 있으면 차트에 복원
  if (ephemerisStore.trackingPath.sampledPath.length > 0) {
    setTimeout(() => {
      if (chart) {
        const updateOption = {
          series: [
            {},
            {},
            {
              data: [...ephemerisStore.trackingPath.sampledPath],
            },
            {},
          ],
        } as unknown as Parameters<typeof chart.setOption>[0]
        chart.setOption(updateOption)
      }
    }, 300)
  }
  // 차트 업데이트 타이머 시작
  updateTimer = window.setInterval(() => {
    const currentTime = performance.now()

    if (lastTimerExecution > 0) {
      const interval = currentTime - lastTimerExecution
      timerIntervalStats.totalExecutions++
      timerIntervalStats.totalInterval += interval
      timerIntervalStats.maxInterval = Math.max(timerIntervalStats.maxInterval, interval)
      timerIntervalStats.minInterval = Math.min(timerIntervalStats.minInterval, interval)

      // ✅ 타이머 간격이 150ms 이상이면 경고
      if (interval > 150) {
        console.warn(`⏰ 타이머 지연 감지: ${interval.toFixed(2)}ms (목표: 100ms)`)
      }

      // ✅ 100번마다 타이머 통계 출력
      if (timerIntervalStats.totalExecutions % 100 === 0) {
        const avgInterval = timerIntervalStats.totalInterval / timerIntervalStats.totalExecutions
        console.log(`⏰ 타이머 통계:`, {
          평균간격: avgInterval.toFixed(2) + 'ms',
          최대간격: timerIntervalStats.maxInterval.toFixed(2) + 'ms',
          최소간격: timerIntervalStats.minInterval.toFixed(2) + 'ms',
          목표간격: '100ms',
        })
      }
    }

    lastTimerExecution = currentTime
    void updateChart()
    updateTimeRemaining()
  }, 100)

  // 시간 업데이트 타이머 시작
  timeUpdateTimer = window.setInterval(() => {
    updateTimeRemaining()
  }, 1000)
})

onUnmounted(() => {
  console.log('EphemerisDesignation 컴포넌트 언마운트됨')

  // 타이머 정리
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
  }

  if (timeUpdateTimer) {
    clearInterval(timeUpdateTimer)
    timeUpdateTimer = null
  }

  // 차트 정리
  if (chart) {
    chart.dispose()
    chart = null
  }
  // ✅ 메인 스레드 모니터링 정리
  if (mainThreadBlockingDetector) {
    cancelAnimationFrame(mainThreadBlockingDetector)
  }
  // ✅ 추가: 추적 경로 정리 (메모리 절약)
  ephemerisStore.clearTrackingPath()
  // ✅ TypeScript Worker 정리
  ephemerisStore.cleanupWorker()
  // 윈도우 이벤트 리스너 정리
  window.removeEventListener('resize', () => { })
})
</script>

<style scoped>
.ephemeris-mode {
  height: 100%;
  width: 100%;
}

.ephemeris-container {
  padding: 0px !important;
  padding-top: 1px !important;
  /* 상단 패딩 1px로 설정 */
  width: 100%;
  height: 100%;
  margin-top: 0px !important;
  /* 상단 마진 제거 */
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}


.control-section {
  height: 100%;
  max-height: 450px;
  width: 100%;
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.chart-area {
  height: 400px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.5rem;
}

.ephemeris-form {
  margin-top: 0.5rem;
  width: 100%;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.button-group {
  display: flex;
  gap: 0.25rem;
  margin-top: 0.5rem;
  width: 100%;
  justify-content: space-between;
}

.full-width {
  width: 100%;
}

.tle-editor {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

.tle-display {
  font-family: monospace !important;
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  min-height: 80px;
}

/* 스케줄 테이블 스타일 */
.schedule-table {
  background-color: var(--theme-card-background);
  color: white;
}

/* ✅ 스케줄 정보 표시 스타일 */
.schedule-info {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 500;
  color: #90caf9;
  min-width: 120px;
}

.info-value {
  font-weight: 400;
  color: #ffffff;
  text-align: right;
  flex: 1;
}

/* 컴팩트 컨트롤 스타일 */
.compact-control {
  padding: 0.5rem !important;
}

.compact-control .q-input {
  margin-bottom: 0.25rem !important;
}

.compact-control .q-btn {
  min-height: 2rem !important;
  padding: 0.25rem !important;
}

/* 가로 배치를 위한 정렬 스타일 */
.align-center {
  align-items: center !important;
}

.justify-end {
  justify-content: flex-end;
}

.justify-center {
  justify-content: center;
}

/* 입력창과 버튼이 같은 높이에 오도록 */
.compact-control .row.align-center {
  display: flex !important;
  align-items: center !important;
  flex-wrap: nowrap !important;
}

.compact-control .q-field {
  margin-bottom: 0 !important;
}

/* 모든 요소가 한 줄에 배치되도록 강제 */
.compact-control .row {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
}

.compact-control .col-3,
.compact-control .col-6,
.compact-control .col-2,
.compact-control .col-4 {
  flex-shrink: 0 !important;
}

/* 강제 가로 배치 */
.compact-control .row {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  width: 100% !important;
}

.compact-control .col-3,
.compact-control .col-6,
.compact-control .col-2,
.compact-control .col-4 {
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  align-items: stretch !important;
}

/* 입력창과 버튼 컨테이너 */
.compact-control .row .row {
  display: flex !important;
  flex-direction: row !important;
  justify-content: center !important;
  align-items: center !important;
  gap: 0.25rem !important;
}

/* 라벨과 입력창을 같은 높이에 배치 */
.compact-control .text-subtitle2 {
  display: flex !important;
  align-items: center !important;
  height: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 라벨 컨테이너 */
.compact-control .col-1 {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start !important;
  min-width: fit-content !important;
}

/* 라벨 텍스트 크기 조정 */
.compact-control .text-subtitle2 {
  font-size: 0.9rem !important;
  white-space: nowrap !important;
}

/* 간격 제거로 더 타이트하게 */
.compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}

/* ✅ 새로운 세로 버튼 레이아웃 */
.vertical-button-group {
  display: flex !important;
  align-items: center !important;
  gap: 4px !important;
}

.vertical-buttons {
  display: flex !important;
  flex-direction: column !important;
  gap: 2px !important;
}

/* ✅ 방법 1: 왼쪽 세로 라벨 (카드 안) - 높이 최적화 */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  /* 높이 줄임: 8px 12px → 4px 8px */
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  /* 너비도 약간 줄임: 60px → 50px */
  margin-right: 6px;
  /* 간격도 줄임: 8px → 6px */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  /* 텍스트 크기 줄임 */
  line-height: 1.2 !important;
  /* 줄 간격 줄임 */
}

/* ✅ 방법 1: 왼쪽 세로 라벨 (카드 안) - 높이 최적화 */
.position-offset-label {
  background: linear-gradient(135deg, rgba(25, 118, 210, 0.15) 0%, rgba(25, 118, 210, 0.08) 100%);
  padding: 4px 8px;
  /* 높이 줄임: 8px 12px → 4px 8px */
  border-radius: 6px;
  border-right: 3px solid var(--q-primary);
  min-width: 50px;
  /* 너비도 약간 줄임: 60px → 50px */
  margin-right: 6px;
  /* 간격도 줄임: 8px → 6px */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  /* 텍스트 크기 줄임 */
  line-height: 1.2 !important;
  /* 줄 간격 줄임 */
}

/* ✅ 카드 테두리 위아래 패딩 완전 제거 - 더 구체적인 셀렉터 */
.q-card.control-card .q-card-section.compact-control {
  padding: 0px 8px !important;
  /* 상하 패딩을 완전히 제거 */
}

/* ✅ 추가적인 강제 적용 */
.q-card-section.compact-control {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* ✅ 더 강력한 강제 적용 - 모든 가능한 셀렉터 */
.q-card-section.compact-control.purple-1,
.q-card.control-card .q-card-section.compact-control.purple-1,
.q-card-section[class*="compact-control"],
.q-card-section[class*="purple-1"] {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
  margin-top: 0px !important;
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
  display: flex !important;
  align-items: flex-start !important;
}

/* ✅ Quasar 기본 스타일 덮어쓰기 */
.q-card-section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 카드 자체 마진도 제거 */
.q-card.control-card {
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

.q-card.control-card .q-card__section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 추가 높이 줄이기 - 모든 요소의 높이 최소화 */
.q-input {
  min-height: auto !important;
  /* 입력 필드 높이 원래대로 */
}

.q-field__control {
  min-height: auto !important;
  /* 필드 컨트롤 높이 원래대로 */
}

.q-field__native {
  padding: auto !important;
  /* 필드 내부 패딩 원래대로 */
}

.q-btn {
  min-height: auto !important;
  /* 버튼 높이 원래대로 */
}

.q-btn--dense {
  min-height: auto !important;
  /* 조밀한 버튼 높이 원래대로 */
}

/* ✅ 라벨 테두리 높이만 줄이기 - 내부 구성은 유지 */
.position-offset-label {
  padding: 4px 8px !important;
  /* 내부 패딩 원래대로 유지 */
  min-width: 50px !important;
  /* 라벨 너비 원래대로 */
  border-right: 1px solid var(--q-primary) !important;
  /* 테두리 두께만 줄임: 3px → 1px */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
  /* 그림자도 줄임 */
}

.position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  /* 텍스트 크기 원래대로 */
  line-height: 1.2 !important;
  /* 줄 간격 원래대로 */
}

/* ✅ 모든 개별 테두리 스타일 - 통합 테두리와 동일하게 */
.azimuth-control-card,
.elevation-control-card,
.tilt-control-card,
.time-control-card {
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
}

.azimuth-control-section,
.elevation-control-section,
.tilt-control-section,
.time-control-section {
  padding: 0px 8px !important;
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  margin: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

.azimuth-control-section .row,
.elevation-control-section .row,
.tilt-control-section .row,
.time-control-section .row {
  margin: 0px !important;
  padding: 0px !important;
  min-height: auto !important;
  height: auto !important;
}

.azimuth-control-section .row>div,
.elevation-control-section .row>div,
.tilt-control-section .row>div,
.time-control-section .row>div {
  margin: 0px !important;
  padding: 0px !important;
  min-height: auto !important;
  height: auto !important;
}

/* ✅ Quasar 기본 스타일 완전 덮어쓰기 */
.q-row {
  margin: 0px !important;
  padding: 0px !important;
}

.q-col {
  margin: 0px !important;
  padding: 0px !important;
}

/* 간격 제거로 더 타이트하게 */
.compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}
</style>

<style>
/* ✅ 전역 스타일 제거하고 scoped로 변경 */
.q-field__control {
  padding: 0 8px;
}

.q-card__section {
  padding: 16px;
}

/* ✅ 전역 q-card 스타일 제거 - 이것이 문제의 원인 */
/* .q-card {
  background: var(--q-dark);
  box-shadow: 0 1px 5px rgb(0 0 0 / 20%), 0 2px 2px rgb(0 0 0 / 14%), 0 3px 1px -2px rgb(0 0 0 / 12%);
} */

/* ✅ EphemerisDesignationPage 내부의 q-card만 스타일 적용 */
.ephemeris-mode .q-card {
  background: var(--theme-card-background);
  box-shadow: 0 1px 5px rgb(0 0 0 / 20%), 0 2px 2px rgb(0 0 0 / 14%), 0 3px 1px -2px rgb(0 0 0 / 12%);
}


.q-btn {
  flex: 1;
}

/* ✅ 강력한 전역 스타일 오버라이드 */
.ephemeris-mode .q-field.fixed-input,
.ephemeris-mode .q-field.fixed-output {
  width: 30px !important;
  min-width: 30px !important;
  max-width: 30px !important;
  flex: 0 0 30px !important;
}

.ephemeris-mode .q-field.fixed-input .q-field__control,
.ephemeris-mode .q-field.fixed-output .q-field__control {
  width: 30px !important;
  min-width: 30px !important;
  max-width: 30px !important;
  flex: 0 0 30px !important;
}

.ephemeris-mode .q-field.fixed-input .q-field__native,
.ephemeris-mode .q-field.fixed-output .q-field__native {
  width: 30px !important;
  min-width: 30px !important;
  max-width: 30px !important;
  padding: 0 4px !important;
  font-size: 12px !important;
}

/* 나머지 스타일은 그대로 유지... */
</style>
