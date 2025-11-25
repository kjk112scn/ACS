<template>
  <div class="ephemeris-mode">
    <!-- 1행: Offset Controls - 원본 형태로 복원하고 반응형 적용 -->
    <div class="row q-col-gutter-md q-mb-sm offset-control-row">
      <div class="col-12">
        <q-card flat bordered class="control-card">
          <q-card-section class="compact-control purple-1">
            <!-- 모든 간격이 동적으로 조정되는 반응형 레이아웃 -->
            <div class="flexible-offset-layout">
              <!-- Azimuth Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Azimuth<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[0]" @input="(val: string) => onInputChange(0, val)" dense outlined
                      type="number" step="0.01" label="Azimuth" class="offset-input" />
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
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Elevation Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Elevation<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[1]" @input="(val: string) => onInputChange(1, val)" dense outlined
                      type="number" step="0.01" label="Elevation"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
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
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Tilt Offset -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Tilt<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[2]" @input="(val: string) => onInputChange(2, val)" dense outlined
                      type="number" step="0.01" label="Tilt"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
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
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                </div>
              </div>

              <!-- Time Offset + Cal Time -->
              <div class="offset-group">
                <div class="row q-gutter-xs align-center">
                  <div class="col-auto position-offset-label">
                    <div class="text-subtitle2 text-weight-bold text-primary text-center">
                      Time<br>Offset
                    </div>
                  </div>
                  <div class="col-auto">
                    <q-input v-model="inputs[3]" @input="(val: string) => onInputChange(3, val)" dense outlined
                      type="number" step="0.01" label="Time"
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
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
                      style="width: 110px !important; min-width: 110px !important; max-width: 110px !important;" />
                  </div>
                  <div class="col-auto cal-time-field">
                    <q-input v-model="formattedCalTime" dense outlined readonly label="Cal Time"
                      style="min-width: 190px !important; max-width: 220px !important;" />
                  </div>
                </div>
              </div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 2행: Main Content -->
    <div class="row q-col-gutter-md main-content-row"
      style="display: flex; flex-wrap: nowrap; align-items: stretch; margin-bottom: 0 !important; padding-bottom: 0 !important;">
      <!-- 1번 영역: 차트가 들어갈 네모난 칸 - 반응형 크기 조정 -->
      <div class="col-12 col-md-3 position-view-col">
        <q-card class="control-section position-view-card"
          style="min-height: 360px !important; height: 100% !important; display: flex !important; flex-direction: column !important;">
          <q-card-section class="position-view-section"
            style="min-height: 360px !important; height: 100% !important; flex: 1 !important; display: flex !important; flex-direction: column !important; padding-top: 16px !important; padding-bottom: 0px !important;">
            <div class="text-subtitle1 text-weight-bold text-primary position-view-title">Position View</div>
            <div class="chart-area" ref="chartRef"
              style="min-height: 340px !important; height: 100% !important; flex: 1 !important; padding-top: 0 !important; padding-bottom: 0 !important; margin-bottom: 0 !important;">
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- 2번 영역: 계산 정보 표시 영역 수정 -->
      <div class="col-12 col-md-3">
        <q-card class="control-section">
          <q-card-section>
            <div class="row justify-between items-center q-mb-xs">
              <div class="text-subtitle1 text-weight-bold text-primary">위성 추적 정보</div>
              <div class="row items-center q-gutter-sm">
                <span class="info-label">추적 상태:</span>
                <q-chip :color="icdStore.ephemerisTrackingStateInfo.displayColor" text-color="white"
                  :label="icdStore.ephemerisTrackingStateInfo.displayLabel" size="sm" class="tracking-status-chip" />
              </div>
            </div>

            <div class="ephemeris-form">
              <div class="form-row">
                <!-- ✅ 정지궤도 정보 표시 -->
                <div v-if="selectedScheduleInfo.isGeostationary" class="schedule-info q-mt-xs">
                  <div class="text-subtitle2 text-weight-bold text-primary q-mb-xs">
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
                <div v-else-if="selectedScheduleInfo.satelliteName" class="schedule-info q-mt-xs">
                  <div class="info-row">
                    <span class="info-label">위성 이름/ID:</span>
                    <span class="info-value">
                      {{ selectedScheduleInfo.satelliteName }} / {{ selectedScheduleInfo.satelliteId }}
                      <!-- KEYHOLE 배지 -->
                      <q-badge v-if="selectedScheduleInfo.isKeyhole" color="red" class="q-ml-sm" label="KEYHOLE" />
                    </span>
                  </div>

                  <div class="info-row">
                    <span class="info-label">시작/종료 시간:</span>
                    <span class="info-value">{{
                      formatToLocalTime(selectedScheduleInfo.startTime)
                    }} / {{
                        formatToLocalTime(selectedScheduleInfo.endTime)
                      }}</span>
                  </div>

                  <div class="info-row">
                    <span class="info-label">지속 시간:</span>
                    <span class="info-value">{{ formatDuration(selectedScheduleInfo.duration) }}</span>
                  </div>

                  <div class="info-row">
                    <span class="info-label">시작/종료 방위각/고도:</span>
                    <span class="info-value">{{ selectedScheduleInfo.startAzimuth.toFixed(6) }}° / {{
                      selectedScheduleInfo.endAzimuth.toFixed(6) }}° / {{
                        selectedScheduleInfo.startElevation.toFixed(6) }}°</span>
                  </div>

                  <div class="info-row">
                    <span class="info-label">최대 고도:</span>
                    <span class="info-value">{{ selectedScheduleInfo.maxElevation.toFixed(6) }}°</span>
                  </div>

                  <!-- KEYHOLE 정보 표시 -->
                  <div v-if="selectedScheduleInfo.isKeyhole" class="keyhole-info q-mt-sm q-pa-sm"
                    style="background-color: rgba(255, 0, 0, 0.1); border-left: 3px solid #f44336;">
                    <div class="text-weight-bold text-red q-mb-xs">🚀 KEYHOLE 위성 정보</div>
                    <div class="info-row">
                      <span class="info-label">권장 Train 각도:</span>
                      <span class="info-value text-positive">{{
                        safeToFixed(selectedScheduleInfo.recommendedTrainAngle, 6)
                      }}°</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">최대 Azimuth 속도:</span>
                      <span class="info-value text-red">
                        {{ safeToFixed(
                          selectedScheduleInfo.isKeyhole
                            ? (selectedScheduleInfo.KeyholeFinalTransformedMaxAzRate ??
                              selectedScheduleInfo.FinalTransformedMaxAzRate ?? 0)
                            : (selectedScheduleInfo.FinalTransformedMaxAzRate ?? 0),
                          6
                        ) }}°/s
                      </span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">최대 Elevation 속도:</span>
                      <span class="info-value text-red">
                        {{ safeToFixed(
                          selectedScheduleInfo.isKeyhole
                            ? (selectedScheduleInfo.KeyholeFinalTransformedMaxElRate ??
                              selectedScheduleInfo.FinalTransformedMaxElRate ?? 0)
                            : (selectedScheduleInfo.FinalTransformedMaxElRate ?? 0),
                          6
                        ) }}°/s
                      </span>
                    </div>
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
      <div class="col-12 col-md-6 schedule-control-col">
        <q-card class="control-section">
          <q-card-section class="schedule-control-section">
            <!-- ✅ TLE Data 헤더 - PassSchedulePage.vue와 동일한 구조 -->
            <div class="schedule-header">
              <div class="text-subtitle1 text-weight-bold text-primary schedule-header-title">TLE Data</div>
            </div>
            <div class="tle-display q-mt-sm">
              <pre class="tle-content">{{ tleData.displayText || 'No TLE data available' }}</pre>
            </div>
            <!-- ✅ 버튼 그룹을 하나로 통합 - PassSchedulePage.vue와 동일한 구조 -->
            <div class="button-group">
              <div class="button-row">
                <q-btn color="primary" label="Text" icon="description" @click="openTLEModal" class="q-mr-sm upload-btn"
                  size="sm" />
                <q-btn color="primary" label="Select Schedule" icon="playlist_add_check" @click="openScheduleModal"
                  class="upload-btn" size="sm" />
              </div>
              <div class="control-button-row">
                <q-btn color="positive" icon="play_arrow" label="Go" @click="handleEphemerisCommand" class="control-btn"
                  size="sm" />
                <q-btn color="negative" icon="stop" label="Stop" @click="handleStopCommand" class="control-btn"
                  size="sm" />
                <q-btn color="warning" icon="home" label="Stow" @click="handleStowCommand" class="control-btn"
                  size="sm" />
              </div>
              <div class="button-row">
                <q-btn color="info" label="Axis Calculator" icon="calculate" @click="openAxisTransformCalculator"
                  class="upload-btn" size="sm" />
                <q-btn color="secondary" label="이론치 다운로드" icon="download" @click="exportAllMstDataToCsv"
                  :loading="isExportingCsv" class="upload-btn" size="sm" />
                <q-btn color="secondary" label="Download" icon="download" @click="downloadRealtimeData"
                  class="upload-btn" size="sm" />
              </div>
            </div>
          </q-card-section>
        </q-card>
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
            style="min-height: 100px; font-family: monospace; font-size: 0.9rem" placeholder="TLE 데이터를 여기에 붙여넣으세요..."
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
        <q-table :rows="ephemerisStore.masterData" :columns="scheduleColumns" row-key="No"
          :loading="isLoadingComparison" :pagination="{ rowsPerPage: 10 }" selection="single"
          v-model:selected="selectedSchedule" class="bg-grey-9 text-white" dark flat bordered>

          <!-- ✅ Azimuth 각도 컬럼 템플릿 (Keyhole 여부에 따라 동적 값 표시) -->
          <template v-slot:body-cell-azimuthAngles="props">
            <q-td :props="props" class="angle-cell">
              <div class="angle-container">
                <div class="angle-line start-angle">
                  <span class="angle-label">시작:</span>
                  <span class="angle-value">{{ formatAngle(props.value?.start) }}</span>
                </div>
                <div class="angle-line end-angle">
                  <span class="angle-label">종료:</span>
                  <span class="angle-value">{{ formatAngle(props.value?.end) }}</span>
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ Elevation 각도 컬럼 템플릿 (Keyhole 여부에 따라 동적 값 표시) -->
          <template v-slot:body-cell-elevationAngles="props">
            <q-td :props="props" class="angle-cell">
              <div class="angle-container">
                <div class="angle-line start-angle">
                  <span class="angle-label">시작:</span>
                  <span class="angle-value">{{ formatAngle(props.value?.start) }}</span>
                </div>
                <div class="angle-line end-angle">
                  <span class="angle-label">종료:</span>
                  <span class="angle-value">{{ formatAngle(props.value?.end) }}</span>
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 2축 최대 고도 템플릿 (Original) -->
          <template v-slot:body-cell-OriginalMaxElevation="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-blue-3">
                  {{ safeToFixed(props.value, 6) }}°
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 3축 최대 고도 템플릿 (Train=0, ±270°, 항상 고정) -->
          <template v-slot:body-cell-Train0MaxElevation="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-green-3">
                  {{ safeToFixed(props.value, 6) }}°
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ FinalTransformed 최대 고도 템플릿 (Keyhole에 따라 다른 값 표시) -->
          <template v-slot:body-cell-MaxElevation="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.row?.KeyholeFinalTransformedMaxElevation ?? props.value ?? 0)
                      : (props.value ?? 0),
                    6
                  ) }}°
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 2축 최대 Az 속도 템플릿 (Select Schedule 테이블용) -->
          <template v-slot:body-cell-OriginalMaxAzRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-blue-3">
                  {{ safeToFixed(props.value, 6) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 3축 최대 Az 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
          <template v-slot:body-cell-Train0MaxAzRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-green-3">
                  {{ safeToFixed(props.value, 6) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ FinalTransformed 최대 Az 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
          <template v-slot:body-cell-FinalTransformedMaxAzRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.row?.KeyholeFinalTransformedMaxAzRate ?? props.value ?? 0)
                      : (props.value ?? 0),
                    6
                  ) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 2축 최대 El 속도 템플릿 (Select Schedule 테이블용) -->
          <template v-slot:body-cell-OriginalMaxElRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-blue-3">
                  {{ safeToFixed(props.value, 6) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 3축 최대 El 속도 템플릿 (Train=0, ±270°, 항상 고정) -->
          <template v-slot:body-cell-Train0MaxElRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-green-3">
                  {{ safeToFixed(props.value, 6) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ FinalTransformed 최대 El 속도 템플릿 (Keyhole에 따라 다른 값 표시) -->
          <template v-slot:body-cell-FinalTransformedMaxElRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-red' : 'text-green-3'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.row?.KeyholeFinalTransformedMaxElRate ?? props.value ?? 0)
                      : (props.value ?? 0),
                    6
                  ) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 중앙차분법 템플릿 (실시간 제어용 - 주석 처리) -->
          <!--
          <template v-slot:body-cell-CentralDiffMaxAzRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-blue-3">
                  {{ safeToFixed(props.value) }}°/s
                </div>
                <div class="text-caption text-grey-6">
                  중앙차분법
                </div>
              </div>
            </q-td>
          </template>

          <template v-slot:body-cell-CentralDiffMaxElRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold text-blue-3">
                  {{ safeToFixed(props.value) }}°/s
                </div>
                <div class="text-caption text-grey-6">
                  중앙차분법
                </div>
              </div>
            </q-td>
          </template>
          -->

          <!-- KEYHOLE 배지 템플릿 -->
          <template v-slot:body-cell-SatelliteName="props">
            <q-td :props="props" class="text-center satellite-name-cell">
              <div class="satellite-name-container">
                <div class="satellite-name-text">{{ props.value || props.row?.SatelliteID || '이름 없음' }}</div>
                <q-badge v-if="props.row?.isKeyhole" color="red" class="keyhole-badge" label="KEYHOLE" />
              </div>
            </q-td>
          </template>

          <!-- Train 각도 템플릿 -->
          <template v-slot:body-cell-recommendedTrainAngle="props">
            <q-td :props="props">
              <span v-if="props.row?.isKeyhole" class="text-positive">
                {{ safeToFixed(props.value) }}°
              </span>
              <span v-else class="text-grey">-</span>
            </q-td>
          </template>

          <!-- ✅ 방법 2 (신규): 최적화 Train 각도 템플릿 -->
          <template v-slot:body-cell-KeyholeOptimizedRecommendedTrainAngle="props">
            <q-td :props="props">
              <span v-if="props.row?.isKeyhole" class="text-info">
                {{ safeToFixed(props.value ?? 0) }}°
              </span>
              <span v-else class="text-grey">-</span>
            </q-td>
          </template>

          <!-- ✅ 방법 2 (신규): 최적화된 최대 Az 속도 템플릿 -->
          <template v-slot:body-cell-KeyholeOptimizedFinalTransformedMaxAzRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-info' : 'text-grey'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.value ?? 0)
                      : 0,
                    6
                  ) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 방법 2 (신규): 최적화된 최대 El 속도 템플릿 -->
          <template v-slot:body-cell-KeyholeOptimizedFinalTransformedMaxElRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold" :class="props.row?.isKeyhole ? 'text-info' : 'text-grey'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.value ?? 0)
                      : 0,
                    6
                  ) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 비교 결과: 개선량 템플릿 -->
          <template v-slot:body-cell-OptimizationImprovement="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold"
                  :class="props.row?.isKeyhole && (props.value ?? 0) > 0 ? 'text-positive' : 'text-grey'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.value ?? 0)
                      : 0,
                    6
                  ) }}°/s
                </div>
              </div>
            </q-td>
          </template>

          <!-- ✅ 비교 결과: 개선율 템플릿 -->
          <template v-slot:body-cell-OptimizationImprovementRate="props">
            <q-td :props="props">
              <div class="text-center">
                <div class="text-weight-bold"
                  :class="props.row?.isKeyhole && (props.value ?? 0) > 0 ? 'text-positive' : 'text-grey'">
                  {{ safeToFixed(
                    props.row?.isKeyhole
                      ? (props.value ?? 0)
                      : 0,
                    2
                  ) }}%
                </div>
              </div>
            </q-td>
          </template>

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
import { ref, onMounted, onUnmounted, onActivated, onDeactivated, computed, watch, nextTick } from 'vue'
import { date, useQuasar } from 'quasar'

import type { QTableProps } from 'quasar'
import { useICDStore } from '../../stores/icd/icdStore'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useEphemerisTrackModeStore } from '@/stores'
import { formatToLocalTime, getCalTimeTimestamp } from '../../utils/times'
// 인터페이스 정의 - 서비스의 타입과 동일하게 사용
import {
  ephemerisTrackService,
  type ScheduleItem,
  type RealtimeTrackingDataItem,
} from '../../services/mode/ephemerisTrackService'
import { openPopup } from '../../utils/windowUtils'
import { useNotification } from '../../composables/useNotification'

// ✅ Quasar 인스턴스
const $q = useQuasar()

// ✅ 알림 시스템 사용
const { success, error, warning, info } = useNotification()

// ✅ Duration 포맷 함수 추가 - 시:분:초 형식
const formatDuration = (duration: string): string => {
  if (!duration) return '00:00:00'

  // ISO 8601 Duration 형식 (PT13M43.6S) 파싱
  const match = duration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/)
  if (!match) return duration // 파싱 실패 시 원본 반환

  const hours = parseInt(match[1] || '0')
  const minutes = parseInt(match[2] || '0')
  const seconds = Math.round(parseFloat(match[3] || '0'))

  // ✅ 시:분:초 형식 (24시간 이상도 표시 가능)
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

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
let chartResizeHandler: (() => void) | null = null

// ✅ 차트 초기화 플래그 (리사이즈 방지용)
const isChartInitialized = ref(false)

// ✅ 차트 크기 상수 통일
const CHART_SIZE = 500

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

// ✅ scheduleData는 더 이상 사용하지 않음 (comparisonData로 대체)
// const scheduleData = computed(() => {
//   const data = ephemerisStore.masterData
//   // 안전한 렌더링을 위해 기본값 보장
//   if (!Array.isArray(data)) {
//     console.warn('⚠️ masterData가 배열이 아닙니다:', data)
//     return []
//   }
//   return data
// })
const selectedSchedule = ref<ScheduleItem[]>([])

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

// ✅ 기존 스케줄 테이블 컬럼 정의 (원래대로 복구)
const scheduleColumns: QTableColumn[] = [
  { name: 'No', label: 'No', field: 'No', align: 'left', sortable: true },
  {
    name: 'SatelliteName',
    label: '위성 이름',
    field: 'SatelliteName',
    align: 'center',
    sortable: true,
    format: (val, row) => {
      const name = val || row.SatelliteID || '이름 없음'
      return name
    },
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
  {
    name: 'Duration',
    label: '지속 시간',
    field: 'Duration',
    align: 'left',
    sortable: true,
    format: (val) => formatDuration(val)
  },
  // ✅ Azimuth 각도 컬럼 (Keyhole 여부에 따라 동적 값 표시)
  {
    name: 'azimuthAngles',
    label: 'Azimuth 각도',
    field: (row) => {
      // Keyhole일 경우: KeyholeFinalTransformed 값 사용
      // Keyhole 아닐 경우: FinalTransformed 값 사용
      const isKeyhole = row.isKeyhole || row.IsKeyhole || false
      if (isKeyhole) {
        return {
          start: row.KeyholeFinalTransformedStartAzimuth ?? row.FinalTransformedStartAzimuth ?? row.StartAzimuth ?? 0,
          end: row.KeyholeFinalTransformedEndAzimuth ?? row.FinalTransformedEndAzimuth ?? row.EndAzimuth ?? 0
        }
      } else {
        return {
          start: row.FinalTransformedStartAzimuth ?? row.StartAzimuth ?? 0,
          end: row.FinalTransformedEndAzimuth ?? row.EndAzimuth ?? 0
        }
      }
    },
    align: 'center',
    sortable: false,
  },
  // ✅ Elevation 각도 컬럼 (Keyhole 여부에 따라 동적 값 표시)
  {
    name: 'elevationAngles',
    label: 'Elevation 각도',
    field: (row) => {
      // Keyhole일 경우: KeyholeFinalTransformed 값 사용
      // Keyhole 아닐 경우: FinalTransformed 값 사용
      const isKeyhole = row.isKeyhole || row.IsKeyhole || false
      if (isKeyhole) {
        return {
          start: row.KeyholeFinalTransformedStartElevation ?? row.FinalTransformedStartElevation ?? row.StartElevation ?? 0,
          end: row.KeyholeFinalTransformedEndElevation ?? row.FinalTransformedEndElevation ?? row.EndElevation ?? 0
        }
      } else {
        return {
          start: row.FinalTransformedStartElevation ?? row.StartElevation ?? 0,
          end: row.FinalTransformedEndElevation ?? row.EndElevation ?? 0
        }
      }
    },
    align: 'center',
    sortable: false,
  },
  // ✅ 2축 최대 고도 (Original)
  {
    name: 'OriginalMaxElevation',
    label: '2축 최대 고도 (°)',
    field: 'OriginalMaxElevation',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '-',
  },
  // ✅ 3축 최대 고도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElevation',
    label: '3축 최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },
  // ✅ FinalTransformed 최대 고도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'MaxElevation',
    label: '최대 고도 (°)',
    field: 'FinalTransformedMaxElevation',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },
  // ✅ 2축 최대 Az 속도 (Select Schedule 테이블용)
  {
    name: 'OriginalMaxAzRate',
    label: '2축 최대 Az 속도 (°/s)',
    field: 'OriginalMaxAzRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '-',
  },
  // ✅ 3축 최대 Az 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxAzRate',
    label: '3축 최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },
  // ✅ FinalTransformed 최대 Az 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxAzRate',
    label: '최대 Az 속도 (°/s)',
    field: 'FinalTransformedMaxAzRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },
  // ✅ 2축 최대 El 속도 (Select Schedule 테이블용)
  {
    name: 'OriginalMaxElRate',
    label: '2축 최대 El 속도 (°/s)',
    field: 'OriginalMaxElRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '-',
  },
  // ✅ 3축 최대 El 속도 (Train=0, ±270°, 항상 고정)
  {
    name: 'Train0MaxElRate',
    label: '3축 최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },
  // ✅ FinalTransformed 최대 El 속도 (Keyhole 여부에 따라 동적 표시)
  {
    name: 'FinalTransformedMaxElRate',
    label: '최대 El 속도 (°/s)',
    field: 'FinalTransformedMaxElRate',
    align: 'center',
    sortable: true,
    format: (val) => val?.toFixed(6) || '0.000000',
  },

  // ✅ 중앙차분법 데이터 (실시간 제어용 - 주석 처리)
  // {
  //   name: 'CentralDiffMaxAzRate',
  //   label: '중앙차분 Az 속도 (°/s)',
  //   field: 'CentralDiffMaxAzRate',
  //   align: 'center',
  //   sortable: true,
  //   format: (val) => val?.toFixed(6) || '0.000000',
  // },
  // {
  //   name: 'CentralDiffMaxElRate',
  //   label: '중앙차분 El 속도 (°/s)',
  //   field: 'CentralDiffMaxElRate',
  //   align: 'center',
  //   sortable: true,
  //   format: (val) => val?.toFixed(6) || '0.000000',
  // },
  // ✅ KEYHOLE 및 Train 각도
  {
    name: 'isKeyhole',
    label: 'KEYHOLE',
    field: 'isKeyhole',
    align: 'center',
    sortable: true,
    format: (val) => val ? 'YES' : 'NO',
  },
  {
    name: 'recommendedTrainAngle',
    label: 'Train 각도 (°)',
    field: 'recommendedTrainAngle',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? val?.toFixed(6) : '-',
  },
  // ✅ 방법 2 (신규): 하이브리드 3단계 그리드 서치로 계산된 Train 각도
  {
    name: 'keyholeOptimizedRecommendedTrainAngle',
    label: '최적화 Train 각도 (°)',
    field: 'KeyholeOptimizedRecommendedTrainAngle',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? (val?.toFixed(6) || '-') : '-',
  },
  // ✅ 방법 2 (신규): 최적화된 최대 Az 속도
  {
    name: 'KeyholeOptimizedFinalTransformedMaxAzRate',
    label: '최적화 최대 Az 속도 (°/s)',
    field: 'KeyholeOptimizedFinalTransformedMaxAzRate',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? (val?.toFixed(6) || '0.000000') : '-',
  },
  // ✅ 방법 2 (신규): 최적화된 최대 El 속도
  {
    name: 'KeyholeOptimizedFinalTransformedMaxElRate',
    label: '최적화 최대 El 속도 (°/s)',
    field: 'KeyholeOptimizedFinalTransformedMaxElRate',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? (val?.toFixed(6) || '0.000000') : '-',
  },
  // ✅ 비교 결과: 개선량
  {
    name: 'OptimizationImprovement',
    label: '개선량 (°/s)',
    field: 'OptimizationImprovement',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? (val?.toFixed(6) || '0.000000') : '-',
  },
  // ✅ 비교 결과: 개선율
  {
    name: 'OptimizationImprovementRate',
    label: '개선율 (%)',
    field: 'OptimizationImprovementRate',
    align: 'center',
    sortable: true,
    format: (val, row) => row.isKeyhole ? (val?.toFixed(2) || '0.00') + '%' : '-',
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

// ✅ 로딩 상태 관리
const isLoadingComparison = ref(false)

// ✅ 기존 스케줄 데이터 로드 함수 (스토어 메서드 사용)
const loadScheduleData = async () => {
  try {
    isLoadingComparison.value = true
    console.log('📊 스케줄 데이터 로드 시작')

    // ✅ 스토어의 loadMasterData 메서드 사용
    await ephemerisStore.loadMasterData(true)

    console.log(`✅ 스케줄 데이터 로드 완료: ${ephemerisStore.masterData.length}개 패스`)

  } catch (err) {
    console.error('❌ 스케줄 데이터 로드 실패:', err)
    error('스케줄 데이터 로드에 실패했습니다')
  } finally {
    isLoadingComparison.value = false
  }
}

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
      // 정지궤도는 KEYHOLE이 아님
      isKeyhole: false,
      recommendedTrainAngle: 0,
      FinalTransformedMaxAzRate: 0,
      FinalTransformedMaxElRate: 0,
      KeyholeAxisTransformedMaxAzRate: undefined,
      KeyholeAxisTransformedMaxElRate: undefined,
      KeyholeFinalTransformedMaxAzRate: undefined,
      KeyholeFinalTransformedMaxElRate: undefined,
      // ✅ 방법 2 (신규): 최적화 데이터 기본값
      KeyholeOptimizedRecommendedTrainAngle: 0,
      KeyholeOptimizedFinalTransformedMaxAzRate: 0,
      KeyholeOptimizedFinalTransformedMaxElRate: 0,
      OptimizationImprovement: 0,
      OptimizationImprovementRate: 0,
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
      maxElevation: selected.isKeyhole
        ? (selected.KeyholeFinalTransformedMaxElevation ?? selected.FinalTransformedMaxElevation ?? (typeof selected.MaxElevation === 'number' ? selected.MaxElevation : 0))
        : (selected.FinalTransformedMaxElevation ?? (typeof selected.MaxElevation === 'number' ? selected.MaxElevation : 0)),
      startTimeMs: new Date(selected.StartTime).getTime(),
      timeRemaining: 0,
      startAzimuth: selected.isKeyhole
        ? (selected.KeyholeFinalTransformedStartAzimuth ?? selected.FinalTransformedStartAzimuth ?? (typeof selected.StartAzimuth === 'number' ? selected.StartAzimuth : 0))
        : (selected.FinalTransformedStartAzimuth ?? (typeof selected.StartAzimuth === 'number' ? selected.StartAzimuth : 0)),
      endAzimuth: selected.isKeyhole
        ? (selected.KeyholeFinalTransformedEndAzimuth ?? selected.FinalTransformedEndAzimuth ?? (typeof selected.EndAzimuth === 'number' ? selected.EndAzimuth : 0))
        : (selected.FinalTransformedEndAzimuth ?? (typeof selected.EndAzimuth === 'number' ? selected.EndAzimuth : 0)),
      startElevation: selected.isKeyhole
        ? (selected.KeyholeFinalTransformedStartElevation ?? selected.FinalTransformedStartElevation ?? (typeof selected.StartElevation === 'number' ? selected.StartElevation : 0))
        : (selected.FinalTransformedStartElevation ?? (typeof selected.StartElevation === 'number' ? selected.StartElevation : 0)),
      endElevation: selected.isKeyhole
        ? (selected.KeyholeFinalTransformedEndElevation ?? selected.FinalTransformedEndElevation ?? (typeof selected.EndElevation === 'number' ? selected.EndElevation : 0))
        : (selected.FinalTransformedEndElevation ?? (typeof selected.EndElevation === 'number' ? selected.EndElevation : 0)),
      isGeostationary: false,
      // KEYHOLE 정보 추가
      isKeyhole: selected.IsKeyhole || false,
      recommendedTrainAngle: selected.RecommendedTrainAngle || 0,
      FinalTransformedMaxAzRate: selected.FinalTransformedMaxAzRate || 0,
      FinalTransformedMaxElRate: selected.FinalTransformedMaxElRate || 0,
      KeyholeAxisTransformedMaxAzRate: selected.KeyholeAxisTransformedMaxAzRate,
      KeyholeAxisTransformedMaxElRate: selected.KeyholeAxisTransformedMaxElRate,
      KeyholeFinalTransformedMaxAzRate: selected.KeyholeFinalTransformedMaxAzRate,
      KeyholeFinalTransformedMaxElRate: selected.KeyholeFinalTransformedMaxElRate,
      // ✅ 방법 2 (신규): 최적화 데이터 추가
      KeyholeOptimizedRecommendedTrainAngle: selected.KeyholeOptimizedRecommendedTrainAngle || 0,
      KeyholeOptimizedFinalTransformedMaxAzRate: selected.KeyholeOptimizedFinalTransformedMaxAzRate || 0,
      KeyholeOptimizedFinalTransformedMaxElRate: selected.KeyholeOptimizedFinalTransformedMaxElRate || 0,
      OptimizationImprovement: selected.OptimizationImprovement || 0,
      OptimizationImprovementRate: selected.OptimizationImprovementRate || 0,
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
    // KEYHOLE 정보 기본값
    isKeyhole: false,
    recommendedTrainAngle: 0,
    FinalTransformedMaxAzRate: 0,
    FinalTransformedMaxElRate: 0,
    KeyholeAxisTransformedMaxAzRate: undefined,
    KeyholeAxisTransformedMaxElRate: undefined,
    KeyholeFinalTransformedMaxAzRate: undefined,
    KeyholeFinalTransformedMaxElRate: undefined,
    // ✅ 방법 2 (신규): 최적화 데이터 기본값
    KeyholeOptimizedRecommendedTrainAngle: 0,
    KeyholeOptimizedFinalTransformedMaxAzRate: 0,
    KeyholeOptimizedFinalTransformedMaxElRate: 0,
    OptimizationImprovement: 0,
    OptimizationImprovementRate: 0,
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

  // 선택된 스케줄에서 KEYHOLE 정보 가져오기
  const selectedSchedule = ephemerisStore.selectedSchedule
  const isKeyhole = selectedSchedule?.isKeyhole || false
  const recommendedTrainAngle = selectedSchedule?.recommendedTrainAngle || 0
  const maxAzimuthRate = selectedSchedule?.FinalTransformedMaxAzRate || 0
  const maxElevationRate = selectedSchedule?.FinalTransformedMaxElRate || 0

  // CSV 헤더 정의 - 원본/축변환/최종 데이터 포함
  const headers = [
    'Index', 'TheoreticalIndex', 'Timestamp', 'PassId', 'ElapsedTime(s)',

    // 원본 데이터 (변환 전)
    'OriginalAzimuth(°)', 'OriginalElevation(°)', 'OriginalRange(km)', 'OriginalAltitude(km)',

    // 축변환 데이터 (기울기 변환 적용)
    'AxisTransformedAzimuth(°)', 'AxisTransformedElevation(°)', 'AxisTransformedRange(km)', 'AxisTransformedAltitude(km)',

    // 최종 변환 데이터 (±270도 제한 적용, Train=0)
    'FinalTransformedAzimuth(°)', 'FinalTransformedElevation(°)', 'FinalTransformedRange(km)', 'FinalTransformedAltitude(km)',

    // Keyhole Final 변환 데이터 (±270도 제한 적용, Train≠0) [Keyhole 발생 시만]
    'KeyholeFinalTransformedAzimuth(°)', 'KeyholeFinalTransformedElevation(°)', 'KeyholeFinalTransformedRange(km)', 'KeyholeFinalTransformedAltitude(km)',

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
    'TrainAngle(°)', 'TransformationType', 'HasTransformation', 'InterpolationMethod', 'InterpolationAccuracy',

    // KEYHOLE 정보
    'IsKeyhole', 'RecommendedTrainAngle(°)', 'MaxAzimuthRate(°/s)', 'MaxElevationRate(°/s)'
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

        // 최종 변환 데이터 (±270도 제한 적용, Train=0)
        safeToFixed(item.finalTransformedAzimuth, 6),
        safeToFixed(item.finalTransformedElevation, 6),
        safeToFixed(item.finalTransformedRange, 6),
        safeToFixed(item.finalTransformedAltitude, 6),

        // Keyhole Final 변환 데이터 (±270도 제한 적용, Train≠0) [Keyhole 발생 시만]
        safeToFixed(item.keyholeFinalTransformedAzimuth ?? null, 6),
        safeToFixed(item.keyholeFinalTransformedElevation ?? null, 6),
        safeToFixed(item.keyholeFinalTransformedRange ?? null, 6),
        safeToFixed(item.keyholeFinalTransformedAltitude ?? null, 6),

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
        safeToFixed(item.interpolationAccuracy, 6),

        // KEYHOLE 정보
        isKeyhole ? 'true' : 'false',
        safeToFixed(recommendedTrainAngle, 6),
        safeToFixed(maxAzimuthRate, 6),
        safeToFixed(maxElevationRate, 6)
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
    info('실시간 추적 데이터를 조회하고 있습니다...')

    // ✅ 기존 API 호출 - generateRealtimeTrackingCsv와 연계
    const response = await ephemerisTrackService.fetchRealtimeTrackingData()

    if (response.data && response.data.length > 0) {
      // ✅ 클라이언트에서 CSV 생성 및 다운로드
      downloadCSVWithTransformations(response.data)

      success(`${response.totalCount || 0}개의 실시간 추적 데이터를 다운로드했습니다`)

      console.log('실시간 추적 데이터 다운로드 결과:', response)
    } else {
      warning('다운로드할 실시간 추적 데이터가 없습니다')
    }
  } catch (error) {
    console.error('실시간 추적 데이터 다운로드 실패:', error)
    error('실시간 추적 데이터 다운로드에 실패했습니다')
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
    // 성능 모니터링 간소화 - 블로킹 방지
    callback()
    return
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
      series: [{ data: this.positionData }, { data: this.trackingData }, {}],
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

// ✅ 차트 크기 조정 함수 (외부에서도 호출 가능) - DOM 스타일을 먼저 설정하여 깜빡임 방지
const adjustChartSize = async () => {
  await nextTick() // ✅ Vue의 DOM 업데이트 완료 대기

  if (!chart || chart.isDisposed() || !chartRef.value) return

  // ✅ 1단계: DOM 스타일을 먼저 설정 (리사이즈 전에!)
  // 이렇게 하면 차트가 처음부터 올바른 위치에서 렌더링되어 깜빡임이 없음
  const chartElement = chartRef.value.querySelector('div') as HTMLElement | null
  if (chartElement) {
    // ✅ 스타일을 먼저 설정하여 차트가 올바른 위치에서 렌더링되도록 함
    chartElement.style.width = `${CHART_SIZE}px`
    chartElement.style.height = `${CHART_SIZE}px`
    chartElement.style.maxWidth = `${CHART_SIZE}px`
    chartElement.style.maxHeight = `${CHART_SIZE}px`
    chartElement.style.minWidth = `${CHART_SIZE}px`
    chartElement.style.minHeight = `${CHART_SIZE}px`
    chartElement.style.position = 'absolute'
    chartElement.style.top = '50%'
    chartElement.style.left = '50%'
    chartElement.style.transform = 'translate(-50%, -50%)'
  }

  // ✅ 2단계: DOM 스타일 적용 후 리사이즈 (스타일이 적용된 상태에서)
  await nextTick()
  chart.resize({
    width: CHART_SIZE,
    height: CHART_SIZE
  })

  console.log('차트 리사이즈 완료:', CHART_SIZE)
}

// ✅ 차트 초기화 함수 수정 - 반응형 크기
const initChart = () => {
  if (!chartRef.value) return

  // ✅ 이미 초기화되었으면 재초기화하지 않음 (PassSchedulePage와 동일)
  if (isChartInitialized.value && chart && !chart.isDisposed()) {
    console.log('✅ 차트가 이미 초기화되어 있음 - 재초기화 건너뜀')
    return
  }

  // 기존 차트 인스턴스가 있으면 제거
  if (chart) {
    chart.dispose()
    isChartInitialized.value = false // ✅ 플래그 리셋
  }

  // ✅ 차트 크기 설정 (차트를 더 크게, Position View 구역 크기와 독립적) - PassSchedulePage와 동일
  // 차트 인스턴스 생성
  chart = echarts.init(chartRef.value, null, {
    width: CHART_SIZE,
    height: CHART_SIZE
  })
  console.log('EphemerisDesignation 차트 인스턴스 생성됨, 크기:', CHART_SIZE)

  // 차트 옵션 설정
  const option = {
    backgroundColor: 'transparent',
    grid: {
      left: '10%', /* ✅ 균등한 여백 확보 (PassSchedulePage와 동일) */
      right: '10%',
      top: '10%',
      bottom: '10%',
      containLabel: false
    },
    polar: {
      radius: ['0%', '50%'],
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
        interval: 60,
        length: 3,
        lineStyle: {
          color: '#555',
        },
      },
      axisLabel: {
        interval: 60,
        formatter: function (value: number) {
          if (value === 0) return 'N (0°)'
          if (value === 90) return 'E (90°)'
          if (value === 180) return 'S (180°)'
          if (value === 270) return 'W (270°)'
          if (value === 45) return 'NE (45°)'
          if (value === 135) return 'SE (135°)'
          if (value === 225) return 'SW (225°)'
          if (value === 315) return 'NW (315°)'
          if (value % 60 === 0) return value + '°'
          return ''
        },
        color: '#999',
        fontSize: 8,
        distance: -8,
      },
      splitLine: {
        show: true,
        interval: 60,
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
        fontSize: 8,
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
  chart.setOption(option, true)
  console.log('EphemerisDesignation 차트 옵션 적용됨')

  // ✅ 초기화 시에만 차트 크기 조정 (DOM 스타일 설정 및 리사이즈)
  // 이미 초기화된 차트는 리사이즈하지 않음
  if (!isChartInitialized.value) {
    void adjustChartSize()
    isChartInitialized.value = true
    console.log('✅ 차트 초기화 및 리사이즈 완료')
  } else {
    console.log('⏸️ 차트가 이미 초기화됨 - 리사이즈 스킵')
  }

  // ✅ 윈도우 리사이즈 핸들러 제거 (고정 크기 차트이므로 불필요)
  // 차트가 고정 크기(500px)이고 CSS로도 고정되어 있으므로 윈도우 리사이즈 시 리사이즈 불필요
  // chartResizeHandler = null (등록하지 않음)
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

    // 차트 옵션 업데이트 - 세 번째 시리즈(궤적 라인)만 업데이트
    const trajectoryOption = {
      series: [
        {}, // 첫 번째 시리즈(실시간 위치)는 그대로 유지
        {}, // 두 번째 시리즈(실시간 추적 경로)는 그대로 유지
        {
          // 세 번째 시리즈(위성 궤적) 업데이트
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

// ✅ 차트 데이터 복원 함수 (이론 경로 + 실시간 경로 한 번에)
// ⚠️ 현재 사용하지 않음 - 화면 복귀 시 불필요한 리렌더링 방지를 위해 제거
// updateChart()가 100ms마다 업데이트하므로 별도 복원이 필요 없음
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const restoreChartData = () => {
  if (!chart || chart.isDisposed()) return

  const hasTrackingPath = ephemerisStore.trackingPath?.sampledPath &&
    ephemerisStore.trackingPath.sampledPath.length > 0
  const hasTrajectory = ephemerisStore.selectedSchedule &&
    ephemerisStore.detailData.length > 0

  if (!hasTrackingPath && !hasTrajectory) return

  // ✅ 이론 경로 데이터 변환 (updateChartWithTrajectory 로직 재사용)
  let trajectoryPoints: [number, number][] = []
  if (hasTrajectory) {
    trajectoryPoints = ephemerisStore.detailData.map((point) => {
      const az = typeof point.Azimuth === 'number' ? point.Azimuth : 0
      const el = typeof point.Elevation === 'number' ? point.Elevation : 0
      const normalizedAz = az < 0 ? az + 360 : az
      const normalizedEl = Math.max(0, Math.min(90, el))
      return [normalizedEl, normalizedAz] as [number, number]
    })
  }

  // ✅ 두 데이터를 한 번에 복원 (리사이즈 없이)
  const updateOption: Parameters<typeof chart.setOption>[0] = {
    series: [
      {}, // series[0]: 실시간 위치 (updateChart에서 관리)
      hasTrackingPath
        ? { data: [...ephemerisStore.trackingPath.sampledPath] }
        : {}, // series[1]: 실시간 추적 경로
      hasTrajectory
        ? { data: trajectoryPoints }
        : {}, // series[2]: 위성 궤적
    ],
  }

  // ✅ setOption 호출 시 리사이즈 방지 (notMerge: false, lazyUpdate: true)
  // lazyUpdate: true는 다음 프레임에 업데이트하므로 리사이즈가 발생하지 않음
  chart.setOption(updateOption, false, true)

  if (hasTrackingPath) {
    console.log('✅ 추적 경로 복원:', ephemerisStore.trackingPath.sampledPath.length, '개 포인트')
  }
  if (hasTrajectory) {
    console.log('✅ 위성 궤적 복원:', ephemerisStore.detailData.length, '개 포인트')
  }
}

// ✅ 남은 시간을 시:분:초 형식으로 포맷하는 함수 (24시간 이상도 표시 가능)
const formatTimeRemaining = (ms: number): string => {
  if (ms < 0) {
    return 'Delayed' // 지연됨
  }
  if (ms === 0) {
    return 'Completed' // 완료
  }

  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  // ✅ 시:분:초 형식 (24시간 이상도 표시 가능)
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
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

// ✅ 중복된 함수 제거됨 - 위에서 이미 정의됨

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

    // KEYHOLE 정보 로깅
    if (selectedItem.IsKeyhole) {
      console.log('🚀 KEYHOLE 위성 선택됨:', {
        satelliteName: selectedItem.SatelliteName || selectedItem.SatelliteID,
        recommendedTrainAngle: selectedItem.RecommendedTrainAngle,
        FinalTransformedMaxAzRate: selectedItem.FinalTransformedMaxAzRate,
        FinalTransformedMaxElRate: selectedItem.FinalTransformedMaxElRate,
        threshold: 10.0 // 기본 임계값
      })
    }

    // 차트 업데이트
    if (detailData && detailData.length > 0 && chart) {
      updateChartWithTrajectory([...detailData] as TrajectoryPoint[])
    }

    success(`${selectedItem.SatelliteName || selectedItem.SatelliteID} 스케줄이 선택되었습니다`)

    showScheduleModal.value = false
  } catch (error) {
    console.error('스케줄 선택 실패:', error)
    error('스케줄 선택에 실패했습니다')
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

    success(`TLE 데이터가 성공적으로 처리되었습니다${isGEO ? ' (정지궤도)' : ''}`)

    showTLEModal.value = false
  } catch (error) {
    console.error('TLE 처리 실패:', error)
    tleError.value = error instanceof Error ? error.message : 'TLE 데이터 처리에 실패했습니다'
  } finally {
    isProcessingTLE.value = false
  }
}

// 안전한 숫자 포맷팅 헬퍼 함수
const safeToFixed = (value: unknown, decimals: number = 6): string => {
  if (typeof value === 'number' && !isNaN(value)) {
    return value.toFixed(decimals)
  }

  // 문자열이나 숫자 문자열만 파싱 시도
  if (typeof value === 'string' || typeof value === 'number') {
    const parsed = parseFloat(String(value))
    if (!isNaN(parsed)) {
      return parsed.toFixed(decimals)
    }
  }

  return '0.00'
}

// ✅ 각도 포맷팅 헬퍼 함수 (Azimuth/Elevation 각도 컬럼용)
const formatAngle = (angle: number | undefined | null): string => {
  if (angle === undefined || angle === null) return '-'
  return `${Number(angle).toFixed(1)}°`
}

// 스케줄 모달 관련
const openScheduleModal = async () => {
  console.log('🚨🚨🚨 Select Schedule 버튼 클릭됨 - 함수 시작!')
  console.log('📋 현재 showScheduleModal 상태:', showScheduleModal.value)
  console.log('📋 ephemerisStore 상태:', {
    geostationaryAngles: ephemerisStore.geostationaryAngles,
    masterData: ephemerisStore.masterData.length,
    selectedSchedule: ephemerisStore.selectedSchedule
  })

  try {
    showScheduleModal.value = true
    console.log('📋 스케줄 모달 열림 - showScheduleModal:', showScheduleModal.value)

    // ✅ 정지궤도 상태가 아닐 때만 스케줄 데이터 로드
    if (!ephemerisStore.geostationaryAngles.isSet) {
      console.log('🔄 정지궤도가 아니므로 스케줄 데이터 로드 시작')
      await loadScheduleData()
    } else {
      console.log('ℹ️ 정지궤도 상태이므로 스케줄 데이터 로드 건너뜀')
    }

    console.log('✅ openScheduleModal 함수 완료')
  } catch (error) {
    console.error('❌ openScheduleModal 함수에서 에러 발생:', error)
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

      success(`정지궤도 위성(${ephemerisStore.geostationaryAngles.satelliteName}) 추적이 활성화되었습니다`)
      return
    }

    // 기존 스케줄 추적 로직
    if (!selectedScheduleInfo.value.passId) {
      warning('먼저 스케줄을 선택하거나 TLE를 입력하세요')
      return
    }

    // ✅ 추적 시작 전 경로 초기화 (현재 위치 기준)
    const currentAzimuth = parseFloat(icdStore.azimuthAngle) || 0
    const currentElevation = parseFloat(icdStore.elevationAngle) || 0

    ephemerisStore.clearTrackingPath(currentAzimuth, currentElevation)
    await ephemerisStore.startTracking()

    console.log('Ephemeris 추적이 시작되었습니다')
  } catch (error) {
    console.error('Failed to start tracking:', error)
    console.error('추적 시작에 실패했습니다')
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

    console.log('정지 명령이 전송되었습니다')
  } catch (error) {
    console.error('Failed to send stop command:', error)
    console.error('정지 명령 전송에 실패했습니다')
  }
}

const handleStowCommand = async () => {
  try {
    await icdStore.stowCommand()

    console.log('Stow 명령이 전송되었습니다')
  } catch (error) {
    console.error('Failed to send stow command:', error)
    console.error('Stow 명령 전송에 실패했습니다')
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
    error('ATC 팝업을 열 수 없습니다')
  }
}

// 모든 MST 데이터를 CSV로 내보내기
const exportAllMstDataToCsv = async () => {
  if (isExportingCsv.value) {
    warning('이미 CSV 내보내기가 진행 중입니다. 잠시만 기다려주세요.')
    return
  }

  isExportingCsv.value = true

  try {
    info('이론치 데이터를 통합 CSV로 내보내는 중...')

    // ✅ 기존 엔드포인트 사용 (이제 통합 CSV 생성)
    const response = await ephemerisTrackService.exportAllMstDataToCsv()

    if (response.success) {
      console.log(`통합 이론치 데이터 내보내기 완료! 총 ${response.totalMstCount}개 MST, ${response.successCount}개 성공`)

      // ✅ 성공 메시지 개선
      success(`통합 이론치 데이터 내보내기 완료! 총 ${response.totalMstCount}개 MST, ${response.successCount}개 성공`)

      console.log('통합 CSV 내보내기 결과:', response)
    } else {
      console.error(`통합 이론치 데이터 내보내기 실패: ${response.error || '알 수 없는 오류'}`)
      error(`통합 이론치 데이터 내보내기 실패: ${response.error || '알 수 없는 오류'}`)
    }
  } catch (error) {
    console.error('통합 CSV 내보내기 실패:', error)
    error('통합 이론치 데이터 내보내기 중 오류가 발생했습니다')
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

// ✅ 컴포넌트 활성화 시 차트 및 데이터 복원
const handleActivated = () => {
  console.log('🔄 EphemerisDesignationPage 활성화됨')

  // ✅ 차트가 이미 존재하고 유효하면 재초기화하지 않음
  // 차트 컨테이너와 차트 인스턴스가 모두 존재하면 그대로 유지
  if (!chart || !chartRef.value) {
    isChartInitialized.value = false // ✅ 플래그 리셋 (재초기화 필요)
    setTimeout(() => {
      initChart()
      console.log('✅ 차트 재초기화 완료')
    }, 100)
  } else {
    // ✅ 차트가 이미 존재하면 그대로 유지 (추가 리사이즈/스타일 변경 없음)
    //    초기 마운트 시 initChart + adjustChartSize에서 한 번만 리사이즈함
    console.log('✅ 차트가 이미 존재함 - 그대로 유지 (리사이즈/스타일 변경 없음)')
  }

  // ✅ 차트 데이터 복원 제거 - 화면 복귀 시 불필요한 리렌더링 방지
  // 차트는 이미 데이터를 가지고 있고, updateChart()가 100ms마다 업데이트하므로
  // 화면 복귀 시 별도 복원이 필요 없음 (불필요한 setOption 호출로 인한 깜빡임 방지)
  // void nextTick(() => {
  //   if (chart && !chart.isDisposed()) {
  //     restoreChartData()
  //   }
  // })

  // ✅ 타이머 재시작
  if (!updateTimer) {
    updateTimer = window.setInterval(() => {
      updateChart()
    }, 100)
    console.log('✅ 차트 업데이트 타이머 재시작')
  }
}

// ✅ 컴포넌트 비활성화 시 타이머만 정리 (차트와 데이터는 유지)
const handleDeactivated = () => {
  console.log('🔄 EphemerisDesignationPage 비활성화됨')

  // ✅ 타이머만 정리 (차트와 추적 경로는 유지)
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
    console.log('✅ 차트 업데이트 타이머 정리됨')
  }
}

// ✅ Vue 생명주기 훅 등록
onActivated(handleActivated)
onDeactivated(handleDeactivated)

onMounted(() => {
  try {
    console.log('EphemerisDesignation 컴포넌트 마운트됨')
    // ✅ 메인 스레드 모니터링 시작
    startMainThreadMonitoring()

    // ✅ 스토어에서 오프셋 값 복원
    inputs.value = [
      ephemerisStore.offsetValues.azimuth,
      ephemerisStore.offsetValues.elevation,
      ephemerisStore.offsetValues.train,
      ephemerisStore.offsetValues.time,
    ]

    // ✅ 차트는 즉시 초기화 (서버 연결과 무관) - PassSchedulePage와 동일
    void nextTick(() => {
      try {
        initChart()
        console.log('✅ 차트 즉시 초기화 완료')

        // 차트 업데이트 타이머 시작
        if (updateTimer) {
          clearInterval(updateTimer)
        }
        updateTimer = window.setInterval(() => {
          try {
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
          } catch (timerError) {
            console.error('차트 업데이트 타이머 오류:', timerError)
          }
        }, 100)

        // 시간 업데이트 타이머 시작
        if (timeUpdateTimer) {
          clearInterval(timeUpdateTimer)
        }
        timeUpdateTimer = window.setInterval(() => {
          try {
            updateTimeRemaining()
          } catch (timeError) {
            console.error('시간 업데이트 타이머 오류:', timeError)
          }
        }, 1000)
      } catch (chartError) {
        console.error('차트 초기화 오류:', chartError)
      }
    })

    // ✅ 서버 데이터 로딩은 비동기로 처리 (차트와 분리) - PassSchedulePage와 동일
    void loadScheduleData().then(() => {
      console.log('✅ 스케줄 데이터 로드 완료')
    }).catch((error) => {
      console.error('스케줄 데이터 로드 실패:', error)
      $q.notify({
        type: 'warning',
        message: '스케줄 데이터를 불러오는데 실패했습니다',
        caption: '차트는 정상적으로 표시됩니다'
      })
    })
  } catch (error) {
    console.error('EphemerisDesignationPage 마운트 중 오류:', error)
  }

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

  // ✅ 차트는 유지 (dispose하지 않음) - keep-alive나 재마운트 시 재사용
  // 실제로 컴포넌트가 완전히 제거될 때만 dispose (일반적으로 발생하지 않음)
  // if (chart) {
  //   chart.dispose()
  //   chart = null
  // }

  // ✅ 메인 스레드 모니터링 정리
  if (mainThreadBlockingDetector) {
    cancelAnimationFrame(mainThreadBlockingDetector)
  }
  // ✅ 추가: 추적 경로 정리 (메모리 절약)
  // ✅ 추적 경로는 유지 (dispose하지 않음) - keep-alive나 재마운트 시 재사용
  // 실제로 컴포넌트가 완전히 제거될 때만 clear (일반적으로 발생하지 않음)
  // ephemerisStore.clearTrackingPath()
  // ✅ TypeScript Worker 정리
  ephemerisStore.cleanupWorker()
  // 윈도우 이벤트 리스너 정리
  // ✅ 차트 리사이즈 이벤트 리스너 제거
  if (chartResizeHandler) {
    window.removeEventListener('resize', chartResizeHandler)
    chartResizeHandler = null
  }
})
</script>

<style scoped>
/* 모든 간격이 동적으로 조정되는 반응형 레이아웃 */
.flexible-offset-layout {
  display: flex;
  align-items: stretch;
  justify-content: center;
  width: 100%;
  gap: 40px;
  row-gap: 8px;
  flex-wrap: wrap;
}

/* 개별 Offset 그룹 - Elevation, Tilt, Time은 좌측 공간 축소 */
.offset-group {
  flex: none;
  min-width: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.01);
  display: flex;
  align-items: center;
}

/* ✅ 간격 통일 - padding-left 제거하고 gap만으로 간격 관리 */

/* 라벨 스타일 */
.position-offset-label {
  min-width: 80px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: rgba(25, 118, 210, 0.1);
  border: 1px solid rgba(25, 118, 210, 0.3);
}

/* Cal Time 필드 스타일 - 확보된 공간 활용 */
.cal-time-field {
  flex-shrink: 0;
  min-width: 190px;
}

/* 반응형 동작 - 1900px 기준으로 줄바꿈 */
@media (max-width: 1900px) {
  .flexible-offset-layout {
    flex-wrap: wrap;
    gap: 20px;
    row-gap: 8px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
    padding: 8px;
  }

  .position-offset-label {
    min-width: 70px;
    font-size: 0.8rem;
  }

  .cal-time-field {
    min-width: 180px;
    max-width: 200px;
  }
}

@media (min-width: 1901px) {
  .flexible-offset-layout {
    flex-wrap: nowrap;
    gap: 40px;
    justify-content: center;
  }

  .offset-group {
    flex: none;
    min-width: 0;
  }

  .position-offset-label {
    min-width: 80px;
    font-size: 0.875rem;
  }
}

/* ✅ 1단계: ephemeris-mode와 부모 요소의 하단 여백 완전 제거 (PassSchedulePage와 동일) */
/* router-view, q-page-container 내부의 ephemeris-mode 하단 여백 제거 */
router-view .ephemeris-mode,
q-page-container .ephemeris-mode,
q-page .ephemeris-mode,
.ephemeris-mode,
[class*="ephemeris-mode"],
div.ephemeris-mode {
  height: auto !important;
  /* ✅ height: 100% 제거하여 내용에 맞게 조정 */
  width: 100%;
  padding: 0 !important;
  margin: 0 !important;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  /* ✅ min-height는 공통 CSS의 var(--theme-layout-modePageMinHeight, 500px) 사용 */
  max-height: none !important;
  /* ✅ 최대 높이 제거 */
  display: flex !important;
  /* ✅ flexbox로 변경 */
  flex-direction: column !important;
  /* ✅ 세로 방향 */
  gap: 0 !important;
  /* ✅ flex gap 제거 */
  row-gap: 0 !important;
  /* ✅ flex row-gap 제거 */
  column-gap: 0 !important;
  /* ✅ flex column-gap 제거 */
}

/* router-view, q-page-container의 하단 패딩/마진 제거 */
router-view .ephemeris-mode,
q-page-container .ephemeris-mode {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.section-title {
  font-weight: 500;
  padding-left: 0.5rem;
}

/* ✅ 오프셋 컨트롤 행 하단 여백 줄이기 - PassSchedulePage.vue와 동일한 순서로 배치 */
.ephemeris-mode .offset-control-row {
  margin-bottom: 0.5rem !important;
  /* ✅ 기본 q-mb-sm (0.5rem) 유지하되 명시적으로 설정 */
}

/* ✅ ephemeris-mode 내부의 모든 직접 자식 요소 하단 여백 제거 - PassSchedulePage.vue와 동일한 순서 */
.ephemeris-mode>* {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 마지막 div 요소 하단 여백 완전 제거 (더 강력한 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode>div:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 마지막 row 요소 하단 여백 완전 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode>.row:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 모든 직접 자식 row 요소 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode>.row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ ephemeris-mode의 모든 직접 자식 div 요소 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode>div {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row가 ephemeris-mode의 마지막 자식일 때 하단 여백 완전 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode>.main-content-row:last-child,
.ephemeris-mode>.row.main-content-row:last-child,
.ephemeris-mode>div.main-content-row:last-child,
.ephemeris-mode>.main-content-row,
.ephemeris-mode>.row.main-content-row,
.ephemeris-mode>div.main-content-row {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  padding-top: 0 !important;
}


/* ✅ 메인 콘텐츠 행 하단 여백을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.ephemeris-mode .main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
}

/* ✅ Quasar q-col-gutter-md가 행에 추가하는 하단 마진을 EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.ephemeris-mode .main-content-row.q-col-gutter-md,
.ephemeris-mode .row.q-col-gutter-md.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ Quasar row 기본 스타일 오버라이드 (더 강력한 선택자) - EphemerisDesignationPage.vue와 동일하게 설정 (하단 마진 없음) */
.ephemeris-mode .main-content-row.row,
.ephemeris-mode .row.main-content-row {
  margin-bottom: 0 !important;
  /* ✅ EphemerisDesignationPage.vue와 동일하게 하단 마진 없음 */
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 하단 여백 완전 제거 - PassSchedulePage.vue와 동일 (중복 제거됨) */

/* ✅ main-content-row 내부의 마지막 컬럼 하단 여백 완전 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"] .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card 하단 여백 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card-section 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"] .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card-section 하단 여백 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 Quasar q-card 하단 마진/패딩 완전 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ===== 2. 컨트롤 섹션 기본 스타일 ===== */
.control-section {
  height: 100%;
  max-height: 500px;
  /* ✅ PassSchedulePage.vue와 동일하게 설정 */
  width: 100%;
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  /* ✅ EphemerisDesignationPage.vue와 동일한 높이를 위해 flex 추가 - 내부 구성 변경 없음 */
  display: flex;
  flex-direction: column;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
}

/* ✅ main-content-row 내부의 모든 컬럼 하단 여백 완전 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"] {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 하단 여백 완전 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"] .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card 하단 여백 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 컬럼 내부의 q-card-section 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"] .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 마지막 컬럼 내부의 q-card-section 하단 여백 제거 (더 구체적인 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:last-child .q-card-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 Quasar q-card 하단 마진/패딩 완전 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .q-card {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ main-content-row 내부의 모든 control-section 하단 여백 제거 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .control-section {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ Position View 카드 높이 제한 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .control-section.position-view-card,
.ephemeris-mode .control-section.position-view-card.q-card {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ Position View 카드 섹션 높이 조정 */
.ephemeris-mode .control-section.position-view-card .q-card-section.position-view-section {
  min-height: 360px !important;
  /* ✅ 차트 영역 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 (다른 패널과 동일하게) */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

.control-section .q-card-section {
  padding: 16px !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 (상단 공간과 동일하게) */
  /* ✅ 남은 공간을 채우도록 flex 추가 - 내부 구성 변경 없음 */
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  /* ✅ 제목 absolute positioning을 위한 기준점 */
}

.position-view-section {
  padding: 16px 16px 0px 16px !important;
  /* ✅ 상단 패딩을 다른 패널과 동일하게 16px로 맞춤, 하단 패딩 제거 */
}

.position-view-title {
  position: absolute;
  /* ✅ 제목을 absolute로 배치하여 차트 영역이 전체 공간 사용 */
  top: 16px;
  left: 16px;
  z-index: 10;
  margin: 0;
  padding: 0;
}

.chart-area {
  min-height: 340px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  width: 100%;
  display: flex;
  align-items: center;
  /* ✅ 중앙 정렬 */
  justify-content: center;
  margin: 0 auto;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding: 0 !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  box-sizing: border-box;
  overflow: visible !important;
  /* ✅ 차트가 넘쳐도 보이도록 변경 */
  text-align: center;
  position: relative;
}

/* ✅ 차트 컨테이너 - PassSchedulePage와 동일한 크기 및 정렬 */
.chart-area>div {
  position: absolute !important;
  left: 50% !important;
  top: 50% !important;
  /* ✅ 중앙 정렬 - PassSchedulePage와 동일한 위치 */
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box !important;
  /* ✅ 차트를 더 크게 설정 (Position View 구역 크기와 독립적) */
  width: 500px !important;
  height: 500px !important;
  max-width: 500px !important;
  max-height: 500px !important;
  min-width: 500px !important;
  min-height: 500px !important;
  aspect-ratio: 1 !important;
  /* ✅ 정사각형 유지 */
}

/* 반응형 차트 크기 조정 - 차트를 더 크게 (Position View 구역 크기와 독립적) - PassSchedulePage.vue와 동일 */
@media (max-width: 1900px) {
  .chart-area>div {
    width: 500px !important;
    height: 500px !important;
    max-width: 500px !important;
    max-height: 500px !important;
    min-width: 500px !important;
    min-height: 500px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

@media (max-width: 1600px) {
  .chart-area>div {
    width: 470px !important;
    height: 470px !important;
    max-width: 470px !important;
    max-height: 470px !important;
    min-width: 470px !important;
    min-height: 470px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

@media (max-width: 1200px) {
  .chart-area>div {
    width: 420px !important;
    height: 420px !important;
    max-width: 420px !important;
    max-height: 420px !important;
    min-width: 420px !important;
    min-height: 420px !important;
    top: 50% !important;
    /* ✅ 중앙 정렬 */
  }
}

/* ✅ 2번 영역(위성 추적 정보) 카드 높이를 Position View와 동일하게 설정 (360px) - PassSchedulePage와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section,
.ephemeris-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section.q-card {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ 2번 영역(위성 추적 정보) 카드 섹션 높이 조정 - PassSchedulePage와 동일 */
.ephemeris-mode .main-content-row>[class*="col-"]:nth-child(2) .control-section .q-card-section {
  min-height: 360px !important;
  /* ✅ 최소 높이 보장 */
  flex: 1 !important;
  /* ✅ 남은 공간 채우기 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ 2행 control-section 카드는 기본 패딩 유지 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .control-section .q-card-section {
  padding: 16px !important;
}

/* ✅ 3번 영역(TLE Data) 카드 높이를 Position View와 동일하게 설정 (360px) - PassSchedulePage와 동일 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section,
.ephemeris-mode .main-content-row .schedule-control-col .control-section.q-card {
  min-height: 367px !important;
  /* ✅ 최소 높이 증가 (365px → 367px) */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ 3번 영역(TLE Data) 카드 섹션 높이 조정 - PassSchedulePage.vue와 동일한 레이아웃 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section {
  min-height: 367px !important;
  /* ✅ 최소 높이 유지 (다른 카드와 높이 맞추기) */
  flex: 1 1 auto !important;
  /* ✅ 남은 공간 채우기 (flex-grow: 1, flex-shrink: 1, flex-basis: auto) */
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-start !important;
  /* ✅ PassSchedulePage.vue와 동일하게 flex-start 유지 */
  align-items: stretch !important;
  padding: 16px !important;
  /* ✅ PassSchedulePage.vue의 .control-section .q-card-section과 동일하게 padding: 16px */
  padding-bottom: 0 !important;
  /* ✅ PassSchedulePage.vue와 동일하게 하단 패딩 0 */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  overflow: hidden !important;
  /* ✅ 하단 여백 방지 */
}

/* ✅ 3단계: schedule-control-section 내부의 button-group 스타일 - 버튼 위 공간 확대 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group {
  margin-top: calc(0.25rem + 28px) !important;
  /* ✅ PassSchedulePage.vue의 margin-top(0.25rem) + 추가 공간(28px) = 버튼 위 공간 적절히 확대 */
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  flex-shrink: 0 !important;
  /* ✅ 버튼 그룹이 축소되지 않도록 */
  padding-top: 0.25rem !important;
  /* ✅ PassSchedulePage.vue와 동일하게 padding-top 유지 */
  width: 100% !important;
  /* ✅ 전체 너비 유지 */
}

/* ✅ schedule-control-section 내부의 마지막 요소 하단 여백 처리 - button-group과 tle-display는 제외 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section>*:last-child:not(.button-group):not(.tle-display) {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ schedule-control-section 내부의 모든 직접 자식 요소 하단 여백 처리 - button-group과 tle-display는 제외 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section>*:not(.button-group):not(.tle-display) {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.ephemeris-form {
  margin-top: 0.5rem;
  width: 100%;
  margin-bottom: 0;
  /* ✅ 하단 마진 제거 */
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  /* ✅ PassSchedulePage와 동일 (0.5rem → 0.25rem) */
  width: 100%;
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  justify-content: flex-start;
  /* ✅ 상단부터 시작 */
}

/* ✅ 스케줄 헤더 스타일 - PassSchedulePage.vue와 동일 */
.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0 !important;
  /* ✅ 헤더가 축소되지 않도록 */
}

.schedule-header-title {
  line-height: 1.2;
  margin: 0;
  padding: 0;
}

/* ✅ button-group 스타일 - PassSchedulePage.vue와 동일한 스타일 */
.button-group {
  margin-top: 0.25rem !important;
  /* ✅ 기본 margin-top (일반 버튼 그룹용) */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 */
  width: 100%;
  flex-shrink: 0 !important;
  /* ✅ 버튼 그룹이 축소되지 않도록 */
  padding-top: 0.25rem !important;
  /* ✅ PassSchedulePage.vue와 동일하게 padding-top: 0.25rem */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 - PassSchedulePage.vue와 동일 */
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  /* ✅ 상단 테두리 추가 - PassSchedulePage.vue와 동일 */
  box-sizing: border-box !important;
  display: flex !important;
  /* ✅ flex 컨테이너로 명시 */
  flex-direction: column !important;
  /* ✅ 세로 방향 - PassSchedulePage.vue와 동일 */
  gap: 0.5rem !important;
  /* ✅ 버튼 행 사이 간격 명시 (겹침 방지) - PassSchedulePage.vue와 동일 */
}

/* ✅ TLE Data 섹션의 button-group은 별도 규칙 적용 (더 구체적인 선택자로 우선순위 보장) */
.ephemeris-mode .schedule-control-section .button-group,
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group {
  margin-top: calc(0.25rem + 28px) !important;
  /* ✅ 버튼 위 공간 28px 확대 (38px에서 10px 감소) - 더 구체적인 선택자로 일반 .button-group 규칙 오버라이드 */
}


/* ✅ 첫 번째 버튼 행 위쪽 여백 추가 (Text / Select Schedule) - 간격 절반으로 줄임 */
.button-group .button-row:first-child {
  margin-top: 4px !important;
  /* ✅ 간격을 절반으로 줄임 (8px → 4px) */
}

/* ✅ TLE 에디터 높이 조정 - 텍스트 박스 크기 줄이기 (140px 고정) */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .tle-display {
  flex: 0 0 auto !important;
  /* ✅ 고정 높이 - flexbox에서 자동으로 늘어나지 않도록 */
  height: 140px !important;
  /* ✅ 높이 140px로 고정 (사용자 요구사항) */
  max-height: 140px !important;
  /* ✅ 최대 높이 고정 */
  min-height: 140px !important;
  /* ✅ 최소 높이 고정 */
  margin-bottom: 95px !important;
  /* ✅ 테이블(210px)과 tle-display(140px) 차이(70px) + 추가 공간(25px) = 95px로 설정하여 button-group 위 공간 확대 */
  margin-top: 0.5rem !important;
  /* ✅ PassSchedulePage.vue 테이블과 동일하게 q-mt-sm (0.5rem) */
  display: flex !important;
  flex-direction: column !important;
}

/* ✅ pre 태그 스타일 - q-editor 대신 사용 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .tle-display .tle-content {
  flex: 1 1 auto !important;
  /* ✅ 남은 공간을 차지하도록 flex-grow: 1 설정 */
  height: 100% !important;
  /* ✅ 부모 높이에 맞춤 */
  min-height: 100% !important;
  /* ✅ 최소 높이 100% */
  display: block !important;
  font-size: 0.9rem !important;
  line-height: 1.4 !important;
  /* ✅ resize handle 제거 */
  resize: none !important;
  /* ✅ 스크롤바 숨기기 */
  overflow: hidden !important;
}

/* ✅ button-row 스타일 - PassSchedulePage.vue와 동일 */
.button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 (상단 공간과 동일하게) */
  flex-shrink: 0 !important;
  /* ✅ 버튼 행이 축소되지 않도록 */
  justify-content: flex-start;
}

/* ✅ Quasar q-mb-sm 클래스 오버라이드 (button-row에 적용된 경우) */
.button-row.q-mb-sm,
.schedule-control-section .button-row.q-mb-sm {
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 완전 제거 */
}

/* ✅ control-button-row 스타일 - PassSchedulePage.vue와 동일 */
.control-button-row {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  flex-shrink: 0 !important;
  /* ✅ 버튼 행이 축소되지 않도록 */
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
}

/* ✅ 업로드 버튼 스타일 - PassSchedulePage.vue와 동일 */
.upload-btn {
  flex: 1;
  min-width: 0;
  height: 36px !important;
  /* ✅ PassSchedulePage.vue와 동일하게 height: 36px 강제 적용 */
  font-size: 13px !important;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
}

/* ✅ TLE Data 섹션의 upload-btn에 대한 더 구체적인 선택자 - PassSchedulePage.vue와 동일한 크기 강제 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group .upload-btn,
.ephemeris-mode .schedule-control-section .button-group .upload-btn {
  height: 36px !important;
  min-height: 36px !important;
  max-height: 36px !important;
  font-size: 13px !important;
}

/* ✅ Quasar 버튼 내부 요소에도 크기 제한 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.upload-btn),
.ephemeris-mode .schedule-control-section .button-group :deep(.upload-btn) {
  height: 36px !important;
  min-height: 36px !important;
  max-height: 36px !important;
}

.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.upload-btn .q-btn__wrapper) {
  min-height: 36px !important;
  height: 36px !important;
}

/* ✅ 버튼 내부 폰트 크기 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .button-group :deep(.upload-btn .q-btn__content),
.ephemeris-mode .schedule-control-section .button-group :deep(.upload-btn .q-btn__content),
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.upload-btn .q-btn__content) {
  font-size: 13px !important;
  line-height: 1.2;
}

.ephemeris-mode :deep(.control-btn .q-btn__content),
.ephemeris-mode .schedule-control-section .button-group :deep(.control-btn .q-btn__content),
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.control-btn .q-btn__content) {
  font-size: 13px !important;
  line-height: 1.2;
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* ✅ 컨트롤 버튼 스타일 - PassSchedulePage.vue와 동일 */
.control-btn {
  flex: 1;
  min-width: 0;
  height: 32px !important;
  /* ✅ PassSchedulePage.vue와 동일하게 height: 32px 강제 적용 */
  font-size: 13px !important;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s ease;
}

/* ✅ TLE Data 섹션의 control-btn에 대한 더 구체적인 선택자 - PassSchedulePage.vue와 동일한 크기 강제 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group .control-btn,
.ephemeris-mode .schedule-control-section .button-group .control-btn {
  height: 32px !important;
  min-height: 32px !important;
  max-height: 32px !important;
  font-size: 13px !important;
}

/* ✅ Quasar 버튼 내부 요소에도 크기 제한 - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.control-btn),
.ephemeris-mode .schedule-control-section .button-group :deep(.control-btn) {
  height: 32px !important;
  min-height: 32px !important;
  max-height: 32px !important;
}

.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section .button-group :deep(.control-btn .q-btn__wrapper) {
  min-height: 32px !important;
  height: 32px !important;
}

.control-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}

.full-width {
  width: 100%;
}

.tle-editor {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
}

.tle-display {
  background-color: var(--theme-card-background);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  height: 140px;
  max-height: 140px;
  min-height: 140px;
  overflow: hidden;
  position: relative;
  /* ✅ resize handle 제거 */
  resize: none !important;
}

/* ✅ pre 태그 스타일 - 줄바꿈 유지, 스크롤바 없음, 점선 없음 */
.tle-display .tle-content {
  font-family: monospace !important;
  font-size: 0.9rem !important;
  line-height: 1.4 !important;
  padding: 8px !important;
  margin: 0 !important;
  white-space: pre-wrap !important;
  overflow-wrap: break-word !important;
  /* ✅ 스크롤바 완전히 숨기기 */
  overflow: hidden !important;
  /* ✅ resize handle 제거 */
  resize: none !important;
  /* ✅ 점선 제거 */
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  /* ✅ 배경색 상속 */
  background-color: transparent !important;
  /* ✅ 높이 100%로 설정하여 부모 영역 채우기 */
  height: 100% !important;
  width: 100% !important;
  /* ✅ 텍스트 색상 */
  color: var(--theme-text) !important;
  /* ✅ 박스 사이징 */
  box-sizing: border-box !important;
}


/* 스케줄 테이블 스타일 */
.schedule-table {
  background-color: var(--theme-card-background);
  color: white;
}

/* ✅ 스케줄 정보 표시 스타일 (PassSchedulePage와 동일) */
.schedule-info {
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px 16px 8px 16px;
  /* ✅ PassSchedulePage와 동일 */
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex: 1;
  /* ✅ 남은 공간을 채워서 하단 정렬 */
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  /* ✅ 상단부터 시작 */
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  /* ✅ PassSchedulePage와 동일 (8px → 6px) */
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
  margin-bottom: auto;
  /* ✅ 마지막 행 아래에 자동 여백 추가하여 하단 정렬 */
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

/* ✅ 컴팩트 컨트롤 스타일 - 정리 */
.ephemeris-mode .compact-control {
  padding: 0 8px;
  margin: 0;
  min-height: auto;
  height: auto;
  line-height: 1;
  vertical-align: top;
}

.ephemeris-mode .compact-control .q-input {
  margin-bottom: 0.25rem;
}

.ephemeris-mode .compact-control .q-btn {
  min-height: 2rem;
  padding: 0.25rem;
}

/* ✅ 레이아웃 정렬 스타일 - 정리 */
.ephemeris-mode .align-center {
  align-items: center;
}

.ephemeris-mode .justify-end {
  justify-content: flex-end;
}

.ephemeris-mode .justify-center {
  justify-content: center;
}

/* ✅ 컴팩트 컨트롤 레이아웃 */
.ephemeris-mode .compact-control .row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  width: 100%;
}

.ephemeris-mode .compact-control .q-field {
  margin-bottom: 0;
}

.ephemeris-mode .compact-control .col-auto {
  flex-shrink: 0;
}

/* ✅ 세부 레이아웃 스타일 - 정리 */
.ephemeris-mode .compact-control .row .row {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 0.25rem;
}

.ephemeris-mode .compact-control .text-subtitle2 {
  display: flex;
  align-items: center;
  height: 100%;
  margin: 0;
  padding: 0;
  font-size: 0.9rem;
  white-space: nowrap;
}

.ephemeris-mode .compact-control .col-1 {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: fit-content;
}

/* ✅ 입력 필드 스타일 - 통일 */
.ephemeris-mode .offset-input {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
}

.ephemeris-mode .cal-time-field {
  min-width: 190px;
  max-width: 220px;
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

/* ✅ Quasar 기본 스타일 덮어쓰기 - 1행 offset control 카드에만 적용 */
.ephemeris-mode .q-card.control-card .q-card-section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  line-height: 1 !important;
  vertical-align: top !important;
}


/* ✅ 카드 자체 마진도 제거 */
.ephemeris-mode .q-card.control-card {
  margin-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}


.ephemeris-mode .q-card.control-card .q-card__section {
  padding-top: 0px !important;
  padding-bottom: 0px !important;
  min-height: auto !important;
  height: auto !important;
  line-height: 1 !important;
  vertical-align: top !important;
}

/* ✅ 추가 높이 줄이기 - 모든 요소의 높이 최소화 */
.ephemeris-mode .q-input {
  min-height: auto !important;
}

.ephemeris-mode .q-field__control {
  min-height: auto !important;
}

.ephemeris-mode .q-field__native {
  padding: auto !important;
}

.ephemeris-mode .q-btn {
  min-height: auto !important;
}

.ephemeris-mode .q-btn--dense {
  min-height: auto !important;
}

/* ✅ 라벨 테두리 높이만 줄이기 - 내부 구성은 유지 */
.ephemeris-mode .position-offset-label {
  padding: 4px 8px !important;
  min-width: 50px !important;
  border-right: 1px solid var(--q-primary) !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
}

.ephemeris-mode .position-offset-label .text-subtitle2 {
  font-size: 0.8rem !important;
  line-height: 1.2 !important;
}

/* 간격 제거로 더 타이트하게 */
.ephemeris-mode .compact-control .row.q-gutter-none {
  margin: 0 !important;
  padding: 0 !important;
}

.ephemeris-mode .compact-control .row.q-gutter-none>div {
  padding-left: 0.25rem !important;
  padding-right: 0.25rem !important;
}

.ephemeris-mode .compact-control .row.q-gutter-none>div:first-child {
  padding-left: 0 !important;
}

.ephemeris-mode .compact-control .row.q-gutter-none>div:last-child {
  padding-right: 0 !important;
}
</style>

<style>
/* ✅ EphemerisDesignationPage 내부 스타일만 적용 - .ephemeris-mode로 스코프 제한 */
/* ✅ q-field__control padding 제거 - PassSchedulePage.vue와 동일한 좌측 여백을 위해 Quasar 기본 패딩 사용 */
/* .ephemeris-mode .q-field__control {
  padding: 0 8px;
} */

.ephemeris-mode .q-card__section {
  padding: 16px;
}

/* ✅ schedule-control-section의 모든 하단 패딩 완전 제거 (더 강력한 선택자) - PassSchedulePage.vue와 동일 */
.ephemeris-mode .main-content-row .schedule-control-col .control-section.q-card .q-card__section.schedule-control-section,
.ephemeris-mode .main-content-row .schedule-control-col .control-section .q-card-section.schedule-control-section,
.ephemeris-mode .main-content-row .schedule-control-col .q-card.control-section .q-card-section.schedule-control-section,
.ephemeris-mode .main-content-row .schedule-control-col .q-card.control-section .q-card__section.schedule-control-section {
  padding-bottom: 0 !important;
  /* ✅ PassSchedulePage.vue와 동일하게 하단 패딩 0 */
  margin-bottom: 0 !important;
}

/* ✅ schedule-control-col의 q-card 하단 패딩 완전 제거 */
.ephemeris-mode .main-content-row .schedule-control-col .q-card.control-section {
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

/* ✅ EphemerisDesignationPage 내부의 q-card만 스타일 적용 - PassSchedulePage.vue와 동일하게 Quasar 기본 그림자 사용 */
.ephemeris-mode .q-card {
  background: var(--theme-card-background);
  /* ✅ box-shadow 제거 - Quasar 기본 q-card 그림자 사용 (PassSchedulePage.vue와 동일) */
}

/* ✅ Position View 카드에만 overflow: visible 적용 */
.ephemeris-mode .q-card.position-view-card,
.ephemeris-mode .control-section.position-view-card.q-card {
  overflow: visible !important;
  /* ✅ 차트가 넘쳐도 보이도록 */
}

.ephemeris-mode .q-btn {
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

/* ✅ KEYHOLE 관련 스타일 - .ephemeris-mode로 스코프 제한 */
.ephemeris-mode .keyhole-info {
  background-color: rgba(255, 0, 0, 0.1) !important;
  border-left: 3px solid #f44336 !important;
  border-radius: 4px;
  padding: 12px !important;
  margin-top: 8px !important;
}

.ephemeris-mode .keyhole-info .text-weight-bold {
  font-weight: 600 !important;
}

.ephemeris-mode .keyhole-info .text-red {
  color: #f44336 !important;
}

.ephemeris-mode .keyhole-info .text-positive {
  color: #4caf50 !important;
}

/* ✅ KEYHOLE 배지 스타일 - .ephemeris-mode로 스코프 제한 */
.ephemeris-mode .q-badge.keyhole-badge {
  background-color: #f44336 !important;
  color: white !important;
  font-weight: 600 !important;
  font-size: 0.75rem !important;
  padding: 2px 6px !important;
  border-radius: 3px !important;
}

/* ✅ KEYHOLE 테이블 행 하이라이트 - .ephemeris-mode로 스코프 제한 */
.ephemeris-mode .q-table tbody tr.keyhole-row {
  background-color: rgba(255, 0, 0, 0.05) !important;
}

.ephemeris-mode .q-table tbody tr.keyhole-row:hover {
  background-color: rgba(255, 0, 0, 0.1) !important;
}

/* ✅ KEYHOLE 컬럼 스타일 - .ephemeris-mode로 스코프 제한 */
.ephemeris-mode .keyhole-column {
  font-weight: 600 !important;
}

.ephemeris-mode .keyhole-column.text-red {
  color: #f44336 !important;
}

.ephemeris-mode .keyhole-column.text-positive {
  color: #4caf50 !important;
}

/* ✅ 각도 컬럼 스타일 (Azimuth/Elevation 각도) */
.ephemeris-mode .angle-cell {
  padding: 6px 10px !important;
  vertical-align: middle;
}

.ephemeris-mode .angle-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 50px;
  justify-content: center;
}

.ephemeris-mode .angle-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px !important;
  line-height: 1.3;
}

.ephemeris-mode .angle-label {
  color: var(--theme-text-secondary);
  font-weight: 600 !important;
  min-width: 35px;
  font-size: 13px !important;
}

.ephemeris-mode .angle-value {
  color: white;
  font-weight: 700 !important;
  text-align: right;
  font-size: 14px !important;
}

.ephemeris-mode .start-angle .angle-value {
  color: #4caf50;
  font-size: 14px !important;
  font-weight: 700 !important;
}

.ephemeris-mode .end-angle .angle-value {
  color: #ff9800;
  font-size: 14px !important;
  font-weight: 700 !important;
}

/* ✅ 위성 이름 셀 스타일 */
.ephemeris-mode .satellite-name-cell {
  padding: 8px 10px !important;
  vertical-align: middle;
}

/* ✅ 위성 이름 컨테이너 스타일 (세로 배치) */
.ephemeris-mode .satellite-name-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 50px;
}

/* ✅ 위성 이름 텍스트 스타일 */
.ephemeris-mode .satellite-name-text {
  font-size: 13px !important;
  font-weight: 500 !important;
  color: white;
}

/* ✅ Keyhole 배지 스타일 */
.ephemeris-mode .keyhole-badge {
  font-weight: 700 !important;
  font-size: 11px !important;
  padding: 4px 8px !important;
  letter-spacing: 0.5px !important;
  margin-top: 2px;
}

/* ✅ Schedule Control이 남은 공간을 차지하도록 설정 (PassSchedulePage와 동일) */
.schedule-control-col {
  flex: 1 1 auto;
  min-width: 0;
}
</style>
