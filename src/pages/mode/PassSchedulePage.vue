<template>
  <div class="pass-schedule-mode">
    <div class="section-title text-h5 text-primary q-mb-sm">Pass Schedule</div>

    <!-- Control Section -->
    <div class="row q-col-gutter-md q-mb-md">
      <!-- Azimuth Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-blue-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Azimuth</div>
          </q-card-section>
          <q-card-section>
            <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined type="number"
              step="0.01" class="q-mb-sm" />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(0)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(0)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(0)" />
            </div>
            <q-input v-model="outputs[0]" dense outlined readonly label="Output" class="q-mt-sm" />
          </q-card-section>
        </q-card>
      </div>

      <!-- Elevation Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-green-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Elevation</div>
          </q-card-section>
          <q-card-section>
            <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined type="number"
              step="0.01" class="q-mb-sm" />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(1)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(1)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(1)" />
            </div>
            <q-input v-model="outputs[1]" dense outlined readonly label="Output" class="q-mt-sm" />
          </q-card-section>
        </q-card>
      </div>

      <!-- Tilt Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="bg-orange-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Tilt</div>
          </q-card-section>
          <q-card-section>
            <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined type="number"
              step="0.01" class="q-mb-sm" />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(2)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(2)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(2)" />
            </div>
            <q-input v-model="outputs[2]" dense outlined readonly label="Output" class="q-mt-sm" />
          </q-card-section>
        </q-card>
      </div>

      <!-- Time Control -->
      <div class="col-6 col-sm-3">
        <q-card flat bordered class="control-card">
          <q-card-section class="purple-1">
            <div class="text-subtitle2 text-weight-bold text-primary">Time</div>
          </q-card-section>
          <q-card-section>
            <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined type="number"
              step="1" class="q-mb-sm" />
            <div class="row q-gutter-xs">
              <q-btn icon="add" size="sm" color="primary" dense flat @click="increment(3)" />
              <q-btn icon="remove" size="sm" color="primary" dense flat @click="decrement(3)" />
              <q-space />
              <q-btn icon="refresh" size="sm" color="grey-7" dense flat @click="reset(3)" />
            </div>
            <q-input v-model="outputs[3]" dense outlined readonly label="Output" class="q-mt-sm" />
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- Main Content Section - 2행 구조로 변경 -->
    <div class="schedule-container">
      <div class="row q-col-gutter-sm">
        <!-- 1행: Position View -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Position View</div>
              <div ref="chartRef" class="chart-area"></div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 1행: Schedule Information -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Schedule Information</div>
              <div class="schedule-form">
                <div class="form-row">
                  <!-- 선택된 스케줄 정보 표시 -->
                  <div v-if="selectedSchedule" class="schedule-info q-mt-md">
                    <div class="text-subtitle2 text-weight-bold text-primary q-mb-sm">
                      선택된 스케줄 정보
                    </div>

                    <div class="info-row">
                      <span class="info-label">스케줄 ID:</span>
                      <span class="info-value">{{ selectedSchedule.no }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">위성 이름:</span>
                      <span class="info-value">{{ selectedSchedule.satelliteName }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 시간:</span>
                      <span class="info-value">{{ new Date(selectedSchedule.startTime).toLocaleString('ko-KR') }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">종료 시간:</span>
                      <span class="info-value">{{ new Date(selectedSchedule.endTime).toLocaleString('ko-KR') }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">지속 시간:</span>
                      <span class="info-value">{{ selectedSchedule.duration }}</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 방위각:</span>
                      <span class="info-value">{{ selectedSchedule.startAzimuthAngle.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">시작 고도각:</span>
                      <span class="info-value">{{ selectedSchedule.startElevationAngle.toFixed(2) }}°</span>
                    </div>

                    <div class="info-row">
                      <span class="info-label">최대 고도각:</span>
                      <span class="info-value">{{ selectedSchedule.maxElevation?.toFixed(2) }}°</span>
                    </div>
                  </div>

                  <!-- 스케줄이 선택되지 않은 경우 -->
                  <div v-else class="no-schedule-selected">
                    <div class="text-grey-5">스케줄을 선택하세요</div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- 1행: Schedule Control -->
        <div class="col-12 col-md-4">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary">Schedule Control</div>

              <!-- 스케줄 테이블 -->
              <q-table flat bordered :rows="scheduleData" :columns="scheduleColumns" row-key="no"
                :pagination="pagination" :loading="loading" selection="single" @row-click="onRowClick"
                class="schedule-table q-mt-sm" style="height: 300px" :no-data-label="'선택된 스케줄이 없습니다'">
                <template v-slot:loading>
                  <q-inner-loading showing color="primary">
                    <q-spinner size="50px" color="primary" />
                  </q-inner-loading>
                </template>

                <!-- 🆕 삭제 버튼 컬럼 -->
                <template v-slot:body-cell-actions="props">
                  <q-td :props="props">
                    <q-btn icon="delete" color="negative" size="sm" flat round>
                      <q-tooltip>목록에서 제거</q-tooltip>
                    </q-btn>
                  </q-td>
                </template>
              </q-table>


              <!-- 버튼 그룹 섹션 -->
              <div class="button-group q-mt-md">
                <div class="button-row q-mb-md">
                  <q-btn color="info" label="TLE Upload" icon="upload_file" @click="handleTLEUpload"
                    class="q-mr-sm upload-btn" size="md" />



                  <q-btn color="primary" label="Select Schedule" icon="playlist_add_check" @click="selectScheduleData"
                    class="upload-btn" size="md">
                    <q-tooltip>스케줄 목록을 불러와서 선택할 수 있습니다</q-tooltip>
                  </q-btn>
                </div>

                <div class="control-button-row">
                  <q-btn color="positive" label="Start" @click="handleStartCommand" :disable="!selectedSchedule"
                    class="control-btn" size="md" />
                  <q-btn color="warning" label="Stop" @click="handleStopCommand" class="control-btn" size="md" />
                  <q-btn color="negative" label="Stow" @click="handleStowCommand" class="control-btn" size="md" />
                </div>

                <!-- 🆕 선택된 스케줄 개수 표시 -->
                <div class="q-mt-sm text-caption text-grey-5">
                  선택된 스케줄: {{ scheduleData.length }}개
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useQuasar } from 'quasar'
import { usePassScheduleStore, type ScheduleItem } from '../../stores/mode/passScheduleStore'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import type { QTableProps } from 'quasar'
import { openModal } from '../../utils/windowUtils'

const $q = useQuasar()
const passScheduleStore = usePassScheduleStore()
const icdStore = useICDStore()

// 차트 관련 변수
const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let updateTimer: number | null = null

// ECharts 매개변수 타입 정의
interface EChartsScatterParam {
  value: [number, number]
  dataIndex: number
  seriesIndex: number
  seriesName: string
  name: string
  color: string
}

// 스케줄 데이터
const scheduleData = passScheduleStore.selectedScheduleList
const selectedSchedule = ref<ScheduleItem | null>(null)
const loading = passScheduleStore.loading

// 입력값과 출력값 - PassSchedule 독립적 상태
const inputs = ref<string[]>(['0.00', '0.00', '0.00', '0'])
const outputs = ref<string[]>(['0.00', '0.00', '0.00', '0'])

// 테이블 컬럼 정의 - Store의 실제 필드명에 맞춤
type QTableColumn = NonNullable<QTableProps['columns']>[0]

const scheduleColumns: QTableColumn[] = [
  { name: 'no', label: 'No', field: 'no', align: 'left' as const, sortable: true },
  { name: 'satelliteName', label: '위성 이름', field: 'satelliteName', align: 'left' as const, sortable: true },
  {
    name: 'startTime',
    label: '시작 시간',
    field: 'startTime',
    align: 'left' as const,
    sortable: true,
    format: (val: string) => {
      try {
        return new Date(val).toLocaleString('ko-KR')
      } catch {
        return val
      }
    }
  },
  {
    name: 'endTime',
    label: '종료 시간',
    field: 'endTime',
    align: 'left' as const,
    sortable: true,
    format: (val: string) => {
      try {
        return new Date(val).toLocaleString('ko-KR')
      } catch {
        return val
      }
    }
  },
  {
    name: 'duration',
    label: '지속 시간',
    field: 'duration',
    align: 'left' as const,
    sortable: true,
  },
  // 🆕 삭제 버튼 컬럼 추가
  { name: 'actions', label: '작업', field: 'actions', align: 'center' as const, sortable: false },
]
// TLE 업로드 핸들러
const handleTLEUpload = async () => {
  try {
    console.log('TLE 업로드 모달 열기')

    const modal = await openModal('tle-upload', {
      width: 1000,
      height: 860,
      modalClass: 'tle-upload-modal',
      onClose: () => {
        console.log('TLE 업로드 모달 닫힘')
        // 모달 닫힌 후 스케줄 데이터 새로고침
      },
      onError: (error) => {
        console.error('TLE 업로드 모달 오류:', error)
        $q.notify({
          type: 'negative',
          message: 'TLE 업로드 창을 열 수 없습니다',
        })
      },
    })

    if (modal) {
      console.log('TLE 업로드 모달 열기 성공')
    }
  } catch (error) {
    console.error('TLE 업로드 모달 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: 'TLE 업로드 창을 열 수 없습니다',
    })
  }
}

// 페이지네이션 설정
const pagination = {
  sortBy: 'no',
  descending: false,
  page: 1,
  rowsPerPage: 10,
}

// 차트 초기화
const initChart = () => {
  if (!chartRef.value) return

  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  const option = {
    backgroundColor: 'transparent',
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
      axisLine: { show: true, lineStyle: { color: '#555' } },
      axisTick: { show: true, interval: 30, lineStyle: { color: '#555' } },
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
      },
      splitLine: {
        show: true,
        interval: 30,
        lineStyle: { color: '#555', type: 'dashed', width: 1 },
      },
    },
    radiusAxis: {
      type: 'value',
      min: 0,
      max: 90,
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { formatter: '{value}°', color: '#999' },
      splitLine: { show: true, lineStyle: { color: '#555', type: 'dashed' } },
    },
    series: [
      {
        name: '현재 위치',
        type: 'scatter',
        coordinateSystem: 'polar',
        symbol: 'circle',
        symbolSize: 15,
        animation: false,
        itemStyle: { color: '#ff5722' },
        data: [[0, 0]],
        label: {
          show: true,
          formatter: function (params: EChartsScatterParam) {
            return `Az: ${params.value[1].toFixed(2)}°\nEl: ${params.value[0].toFixed(2)}°`
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
        name: '스케줄 경로',
        type: 'line',
        coordinateSystem: 'polar',
        symbol: 'none',
        animation: false,
        lineStyle: { color: '#2196f3', width: 2 },
        data: [],
        zlevel: 1,
      },
    ],
  }

  chart.setOption(option)

  setTimeout(() => {
    chart?.resize()
  }, 0)
}

// 차트 업데이트
const updateChart = () => {
  if (!chart) return

  try {
    const azimuth = parseFloat(icdStore.azimuthAngle) || 0
    const elevation = parseFloat(icdStore.elevationAngle) || 0

    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))

    const updateOption = {
      series: [
        {
          data: [[normalizedEl, normalizedAz]],
        },
        {},
      ],
    }

    chart.setOption(updateOption)
  } catch (error) {
    console.error('차트 업데이트 중 오류 발생:', error)
  }
}
const selectScheduleData = async () => {
  try {
    console.log('스케줄 선택 모달 열기')

    const modal = await openModal('select-schedule', {
      width: 1200,
      height: 700,
      modalClass: 'select-schedule-modal',
      onClose: (selectedData?: ScheduleItem) => {
        console.log('스케줄 선택 모달 닫힘', selectedData)
        if (selectedData) {
          // 🔧 선택된 스케줄을 목록에 추가
          passScheduleStore.addSelectedSchedule(selectedData)

          $q.notify({
            type: 'positive',
            message: `스케줄 "${selectedData.satelliteName}"이 목록에 추가되었습니다`,
          })
        }
      },
      onError: (error) => {
        console.error('스케줄 선택 모달 오류:', error)
        $q.notify({
          type: 'negative',
          message: '스케줄 선택 창을 열 수 없습니다',
        })
      },
    })

    if (modal) {
      console.log('스케줄 선택 모달 열기 성공')
    }
  } catch (error) {
    console.error('스케줄 선택 모달 열기 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 선택 창을 열 수 없습니다',
    })
  }
}

// 테이블 행 클릭 이벤트 핸들러
const onRowClick = (evt: Event, row: ScheduleItem) => {
  selectedSchedule.value = row
  passScheduleStore.selectSchedule(row) // Store에도 선택 상태 저장
  updateScheduleChart()

  console.log('스케줄 선택됨:', {
    no: row.no,
    satelliteName: row.satelliteName,
    startTime: row.startTime,
  })
}

// 선택된 스케줄에 따른 차트 업데이트
const updateScheduleChart = () => {
  if (!chart || !selectedSchedule.value) return

  // 여기에 선택된 스케줄의 궤적 데이터를 차트에 표시하는 로직 추가
  console.log('스케줄 차트 업데이트:', selectedSchedule.value)
}

// 입력값 변경 핸들러
const onInputChange = (index: number, value: string) => {
  inputs.value[index] = value
  updateOutputs()
}

// 증가 함수
const increment = async (index: number) => {
  const currentOutput = parseFloat(outputs.value[index] || '0')
  const inputValue = parseFloat(inputs.value[index] || '0')
  const newValue = (currentOutput + inputValue).toFixed(index === 3 ? 0 : 2)

  outputs.value[index] = newValue
  await updateOffset(index, newValue)
}

// 감소 함수
const decrement = async (index: number) => {
  const currentOutput = parseFloat(outputs.value[index] || '0')
  const inputValue = parseFloat(inputs.value[index] || '0')
  const newValue = (currentOutput - inputValue).toFixed(index === 3 ? 0 : 2)

  outputs.value[index] = newValue
  await updateOffset(index, newValue)
}

// 리셋 함수
const reset = async (index: number) => {
  inputs.value[index] = index === 3 ? '0' : '0.00'
  outputs.value[index] = index === 3 ? '0' : '0.00'
  await updateOffset(index, outputs.value[index])
}

// 출력값 업데이트
const updateOutputs = () => {
  outputs.value = [...inputs.value]
}

// 오프셋 업데이트 (PassSchedule 독립적 처리)
const updateOffset = async (index: number, value: string) => {
  try {
    const numValue = Number(parseFloat(value).toFixed(2)) || 0

    if (index === 3) {
      // Time offset 처리 (PassSchedule 전용)
      console.log('PassSchedule Time offset:', numValue)
      return
    }

    // Position Offset 처리
    const azOffset = Number(parseFloat(outputs.value[0] || '0').toFixed(2))
    const elOffset = Number(parseFloat(outputs.value[1] || '0').toFixed(2))
    const tiOffset = Number(parseFloat(outputs.value[2] || '0').toFixed(2))

    await icdStore.sendPositionOffsetCommand(azOffset, elOffset, tiOffset)
  } catch (error) {
    console.error('Error updating offset:', error)
  }
}

// 명령 핸들러들 - async 제거하고 동기 처리
const handleStartCommand = () => {
  if (!selectedSchedule.value) {
    $q.notify({
      type: 'warning',
      message: '먼저 스케줄을 선택하세요',
    })
    return
  }

  try {
    // PassSchedule 시작 로직 - selectSchedule 메서드 사용 (동기 처리)
    passScheduleStore.selectSchedule(selectedSchedule.value)

    $q.notify({
      type: 'positive',
      message: `스케줄 ${selectedSchedule.value.satelliteName} 시작됨`,
    })
  } catch (error) {
    console.error('Failed to start schedule:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 시작에 실패했습니다',
    })
  }
}

const handleStopCommand = async () => {
  try {
    await icdStore.stopCommand(true, true, true)

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
    selectedSchedule.value = null
    inputs.value = ['0.00', '0.00', '0.00', '0']
    outputs.value = ['0.00', '0.00', '0.00', '0']

    // 🔧 선택된 스케줄 목록도 초기화
    passScheduleStore.clearSelectedSchedules()

    // 모든 오프셋 리셋
    await icdStore.sendPositionOffsetCommand(0, 0, 0)

    $q.notify({
      type: 'info',
      message: 'PassSchedule이 리셋되었습니다',
    })
  } catch (error) {
    console.error('Failed to reset:', error)
    $q.notify({
      type: 'negative',
      message: '리셋에 실패했습니다',
    })
  }
}

// 초기화
const init = async () => {
  console.log('PassSchedulePage 초기화 시작')

  setTimeout(() => {
    initChart()
  }, 100)

  // Store 초기화 호출
  try {
    await passScheduleStore.init() // 🔧 Store의 init 메서드 직접 호출
    console.log('✅ 스케줄 데이터 로드 완료:', passScheduleStore.scheduleData.length, '개')
  } catch (error) {
    console.error('스케줄 데이터 로드 실패:', error)
    $q.notify({
      type: 'negative',
      message: '스케줄 데이터를 불러오는데 실패했습니다',
    })
  }
}
// 컴포넌트 마운트
onMounted(async () => {
  console.log('PassSchedulePage 컴포넌트 마운트됨')
  await init()

  // 차트 업데이트 타이머 시작 (PassSchedule 독립적)
  updateTimer = window.setInterval(() => {
    updateChart()
  }, 100)
})

// 컴포넌트 언마운트
onUnmounted(() => {
  console.log('PassSchedulePage 컴포넌트 언마운트됨')

  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
  }

  if (chart) {
    chart.dispose()
    chart = null
  }

  window.removeEventListener('resize', () => { })
})
</script>

<style scoped>
.pass-schedule-mode {
  height: 100%;
  width: 100%;
}

.schedule-container {
  padding: 1rem;
  width: 100%;
  height: 100%;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

.control-section {
  height: 500px;
  width: 100%;
  background-color: var(--q-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
}

.control-section .q-card-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-area {
  height: 400px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.5rem;
}

.schedule-form {
  margin-top: 0.5rem;
  width: 100%;
  flex: 1;
  overflow-y: auto;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
  height: 100%;
}

.button-group {
  margin-top: 0.5rem;
  width: 100%;
  flex-shrink: 0;
}

.button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  margin-bottom: 1rem;
}

.control-button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
}

.upload-btn {
  flex: 1;
  min-width: 0;
  height: 48px;
  font-size: 14px;
  font-weight: 500;
}

.control-btn {
  flex: 1;
  min-width: 0;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
}

.schedule-info {
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.05);
  flex: 1;
  overflow-y: auto;
}

.no-schedule-selected {
  padding: 2rem;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.02);
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  padding: 0.25rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}

.info-value {
  font-weight: 600;
  color: white;
}

.schedule-table {
  background-color: var(--q-dark);
  color: white;
  flex: 1;
}

.control-card {
  height: 100%;
}

.control-card .q-card-section {
  padding: 0.5rem;
}

/* 반응형 디자인 */
@media (max-width: 1023px) {
  .control-section {
    height: auto;
    min-height: 400px;
  }

  /* 태블릿에서는 오프셋 컨트롤을 2x2로 배치 */
  .row:first-of-type .col-3 {
    width: 50%;
  }
}

@media (max-width: 767px) {

  /* 모바일에서는 오프셋 컨트롤을 세로로 배치 */
  .row:first-of-type .col-3 {
    width: 100%;
  }
}
</style>

<style>
/* 전역 스타일 */
.col-md-4 {
  width: 33.3333%;
  padding: 4px;
}

.q-btn {
  flex: 1;
}
</style>
