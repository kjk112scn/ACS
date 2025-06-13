<template>
  <q-dialog v-model="isOpen" persistent>
    <q-card class="all-status-modal">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">All Status Information</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup @click="handleClose" />
      </q-card-section>

      <q-card-section class="q-pt-none">
        <div class="status-grid">
          <!-- Protocol 카드 -->
          <q-card class="protocol-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-primary q-mb-md">
                Protocol Status
              </div>

              <div class="protocol-grid">
                <!-- Elevation Protocol -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !protocolStatusInfo.elevation,
                      'led-error': protocolStatusInfo.elevation,
                    }"
                  ></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ protocolStatusInfo.elevation ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Azimuth Protocol -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !protocolStatusInfo.azimuth,
                      'led-error': protocolStatusInfo.azimuth,
                    }"
                  ></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ protocolStatusInfo.azimuth ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Tilt Protocol -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !protocolStatusInfo.tilt,
                      'led-error': protocolStatusInfo.tilt,
                    }"
                  ></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ protocolStatusInfo.tilt ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Feed Protocol -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !protocolStatusInfo.feed,
                      'led-error': protocolStatusInfo.feed,
                    }"
                  ></div>
                  <span class="status-label">Feed</span>
                  <span class="status-value">
                    {{ protocolStatusInfo.feed ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>
              </div>

              <!-- Protocol 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Raw Bits:</span>
                  <span class="summary-value">{{ protocolStatusInfo.raw }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Count:</span>
                  <span class="summary-value">{{
                    protocolStatusInfo.summary?.totalActive || 0
                  }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Protocols:</span>
                  <span class="summary-value">
                    {{ protocolStatusInfo.activeProtocols?.join(', ') || 'None' }}
                  </span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>

          <!-- Power Status 카드 -->
          <q-card class="power-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-warning q-mb-md">Power Status</div>

              <div class="power-grid">
                <!-- Power Surge Protector -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !icdStore.mainBoardStatusInfo.powerSurgeProtector,
                      'led-error': icdStore.mainBoardStatusInfo.powerSurgeProtector,
                    }"
                  ></div>
                  <span class="status-label">Surge Protector</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardStatusInfo.powerSurgeProtector ? 'FAULT' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Power Reverse Phase Sensor -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !icdStore.mainBoardStatusInfo.powerReversePhaseSensor,
                      'led-error': icdStore.mainBoardStatusInfo.powerReversePhaseSensor,
                    }"
                  ></div>
                  <span class="status-label">Reverse Phase</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardStatusInfo.powerReversePhaseSensor ? 'FAULT' : 'NORMAL' }}
                  </span>
                </div>
              </div>

              <!-- Power 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Raw Bits:</span>
                  <span class="summary-value">{{ icdStore.mainBoardStatusInfo.raw }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Power Issues:</span>
                  <span
                    class="summary-value"
                    :class="{
                      'text-negative': icdStore.mainBoardStatusInfo.summary?.hasPowerIssue,
                      'text-positive': !icdStore.mainBoardStatusInfo.summary?.hasPowerIssue,
                    }"
                  >
                    {{ icdStore.mainBoardStatusInfo.summary?.hasPowerIssue ? 'DETECTED' : 'NONE' }}
                  </span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>
          <!-- Emergency Stop 카드 -->
          <q-card class="emergency-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-negative q-mb-md">
                Emergency Stop Status
              </div>

              <div class="emergency-grid">
                <!-- Emergency Stop ACU -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !icdStore.mainBoardStatusInfo.emergencyStopACU,
                      'led-error': icdStore.mainBoardStatusInfo.emergencyStopACU,
                    }"
                  ></div>
                  <span class="status-label">ACU</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardStatusInfo.emergencyStopACU ? 'STOP' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Emergency Stop Positioner -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': !icdStore.mainBoardStatusInfo.emergencyStopPositioner,
                      'led-error': icdStore.mainBoardStatusInfo.emergencyStopPositioner,
                    }"
                  ></div>
                  <span class="status-label">Positioner</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardStatusInfo.emergencyStopPositioner ? 'STOP' : 'NORMAL' }}
                  </span>
                </div>
              </div>

              <!-- Emergency 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Emergency Status:</span>
                  <span
                    class="summary-value"
                    :class="{
                      'text-negative': icdStore.mainBoardStatusInfo.summary?.hasEmergencyStop,
                      'text-positive': !icdStore.mainBoardStatusInfo.summary?.hasEmergencyStop,
                    }"
                  >
                    {{ icdStore.mainBoardStatusInfo.summary?.hasEmergencyStop ? 'ACTIVE' : 'NONE' }}
                  </span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>

          <!-- Servo Power 카드 -->
          <q-card class="servo-power-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-info q-mb-md">
                Servo Power Status
              </div>

              <div class="servo-power-grid">
                <!-- Azimuth Servo Power -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.mainBoardMCOnOffInfo.mcAzimuth,
                      'led-off': !icdStore.mainBoardMCOnOffInfo.mcAzimuth,
                    }"
                  ></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardMCOnOffInfo.mcAzimuth ? 'ON' : 'OFF' }}
                  </span>
                </div>

                <!-- Elevation Servo Power -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.mainBoardMCOnOffInfo.mcElevation,
                      'led-off': !icdStore.mainBoardMCOnOffInfo.mcElevation,
                    }"
                  ></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardMCOnOffInfo.mcElevation ? 'ON' : 'OFF' }}
                  </span>
                </div>

                <!-- Tilt Servo Power -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.mainBoardMCOnOffInfo.mcTilt,
                      'led-off': !icdStore.mainBoardMCOnOffInfo.mcTilt,
                    }"
                  ></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ icdStore.mainBoardMCOnOffInfo.mcTilt ? 'ON' : 'OFF' }}
                  </span>
                </div>
              </div>

              <!-- Servo Power 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Raw Bits:</span>
                  <span class="summary-value">{{ icdStore.mainBoardMCOnOffInfo.raw }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Servos:</span>
                  <span class="summary-value">
                    {{
                      [
                        icdStore.mainBoardMCOnOffInfo.mcAzimuth && 'Azimuth',
                        icdStore.mainBoardMCOnOffInfo.mcElevation && 'Elevation',
                        icdStore.mainBoardMCOnOffInfo.mcTilt && 'Tilt',
                      ]
                        .filter(Boolean)
                        .join(', ') || 'None'
                    }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">All Servos:</span>
                  <span
                    class="summary-value"
                    :class="{
                      'text-positive':
                        icdStore.mainBoardMCOnOffInfo.mcAzimuth &&
                        icdStore.mainBoardMCOnOffInfo.mcElevation &&
                        icdStore.mainBoardMCOnOffInfo.mcTilt,
                      'text-warning':
                        (icdStore.mainBoardMCOnOffInfo.mcAzimuth ||
                          icdStore.mainBoardMCOnOffInfo.mcElevation ||
                          icdStore.mainBoardMCOnOffInfo.mcTilt) &&
                        !(
                          icdStore.mainBoardMCOnOffInfo.mcAzimuth &&
                          icdStore.mainBoardMCOnOffInfo.mcElevation &&
                          icdStore.mainBoardMCOnOffInfo.mcTilt
                        ),
                      'text-negative':
                        !icdStore.mainBoardMCOnOffInfo.mcAzimuth &&
                        !icdStore.mainBoardMCOnOffInfo.mcElevation &&
                        !icdStore.mainBoardMCOnOffInfo.mcTilt,
                    }"
                  >
                    {{
                      icdStore.mainBoardMCOnOffInfo.mcAzimuth &&
                      icdStore.mainBoardMCOnOffInfo.mcElevation &&
                      icdStore.mainBoardMCOnOffInfo.mcTilt
                        ? 'ON'
                        : icdStore.mainBoardMCOnOffInfo.mcAzimuth ||
                            icdStore.mainBoardMCOnOffInfo.mcElevation ||
                            icdStore.mainBoardMCOnOffInfo.mcTilt
                          ? 'PARTIAL'
                          : 'OFF'
                    }}
                  </span>
                </div>
              </div>
               -->
            </q-card-section>
          </q-card>

          <!-- Stow Pin Status 카드 -->
          <q-card class="stow-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-purple q-mb-md">Stow Pin Status</div>

              <div class="stow-grid">
                <!-- Azimuth Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.azimuthBoardStatusInfo.stowPin,
                      'led-off': !icdStore.azimuthBoardStatusInfo.stowPin,
                    }"
                  ></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ icdStore.azimuthBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
                  </span>
                </div>

                <!-- Elevation Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.elevationBoardStatusInfo.stowPin,
                      'led-off': !icdStore.elevationBoardStatusInfo.stowPin,
                    }"
                  ></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ icdStore.elevationBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
                  </span>
                </div>

                <!-- Tilt Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': icdStore.tiltBoardStatusInfo.stowPin,
                      'led-off': !icdStore.tiltBoardStatusInfo.stowPin,
                    }"
                  ></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ icdStore.tiltBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
                  </span>
                </div>
              </div>

              <!-- Stow Pin 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Stowed Axes:</span>
                  <span class="summary-value">
                    {{
                      [
                        icdStore.azimuthBoardStatusInfo.stowPin && 'Azimuth',
                        icdStore.elevationBoardStatusInfo.stowPin && 'Elevation',




                        icdStore.tiltBoardStatusInfo.stowPin && 'Tilt'
                      ].filter(Boolean).join(', ') || 'None'
                    }}
                  </span>
                </div>

                <div class="summary-item">
                  <span class="summary-label">All Stowed:</span>
                  <span
                    class="summary-value"
                    :class="{
                      'text-positive':
                        icdStore.azimuthBoardStatusInfo.stowPin &&
                        icdStore.elevationBoardStatusInfo.stowPin &&
                        icdStore.tiltBoardStatusInfo.stowPin,
                      'text-warning':
                        (icdStore.azimuthBoardStatusInfo.stowPin ||
                          icdStore.elevationBoardStatusInfo.stowPin ||
                          icdStore.tiltBoardStatusInfo.stowPin) &&
                        !(
                          icdStore.azimuthBoardStatusInfo.stowPin &&
                          icdStore.elevationBoardStatusInfo.stowPin &&
                          icdStore.tiltBoardStatusInfo.stowPin
                        ),
                      'text-negative':
                        !icdStore.azimuthBoardStatusInfo.stowPin &&
                        !icdStore.elevationBoardStatusInfo.stowPin &&
                        !icdStore.tiltBoardStatusInfo.stowPin,
                    }"
                  >
                    {{
                      icdStore.azimuthBoardStatusInfo.stowPin &&
                      icdStore.elevationBoardStatusInfo.stowPin &&
                      icdStore.tiltBoardStatusInfo.stowPin
                        ? 'YES'
                        : icdStore.azimuthBoardStatusInfo.stowPin ||
                            icdStore.elevationBoardStatusInfo.stowPin ||
                            icdStore.tiltBoardStatusInfo.stowPin
                          ? 'PARTIAL'
                          : 'NO'
                    }}
                  </span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>

          <!-- Stow 카드 -->
          <q-card class="stow-command-status-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-teal q-mb-md">
                Stow Command Status
              </div>

              <div class="stow-command-grid">
                <!-- Azimuth Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': false,
                      'led-off': true,
                    }"
                  ></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value"> READY </span>
                </div>

                <!-- Elevation Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': false,
                      'led-off': true,
                    }"
                  ></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value"> READY </span>
                </div>

                <!-- Tilt Stow -->
                <div class="status-item">
                  <div
                    class="status-led"
                    :class="{
                      'led-on': false,
                      'led-off': true,
                    }"
                  ></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value"> READY </span>
                </div>
              </div>

              <!-- Stow Command 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Command Status:</span>
                  <span class="summary-value">READY</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Commands:</span>
                  <span class="summary-value">None</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">All Ready:</span>
                  <span class="summary-value text-positive">YES</span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>

          <!-- Positioner Status 카드 (전체 행 차지) -->
          <q-card class="positioner-status-card full-width-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-indigo q-mb-md">
                Positioner Status
              </div>

              <div class="positioner-grid">
                <!-- Azimuth Positioner -->
                <div class="positioner-section">
                  <div class="positioner-title">Azimuth</div>

                  <!-- Limit Switch -275˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.azimuthBoardStatusInfo.limitSwitchNegative275,
                        'led-off': !icdStore.azimuthBoardStatusInfo.limitSwitchNegative275,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch -275˚</span>
                    <span class="status-value">
                      {{
                        icdStore.azimuthBoardStatusInfo.limitSwitchNegative275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +275˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.azimuthBoardStatusInfo.limitSwitchPositive275,
                        'led-off': !icdStore.azimuthBoardStatusInfo.limitSwitchPositive275,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch +275˚</span>
                    <span class="status-value">
                      {{
                        icdStore.azimuthBoardStatusInfo.limitSwitchPositive275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.azimuthBoardServoStatusInfo.servoMotor,
                        'led-off': !icdStore.azimuthBoardServoStatusInfo.servoMotor,
                      }"
                    ></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ icdStore.azimuthBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.azimuthBoardServoStatusInfo.servoBrake,
                        'led-off': !icdStore.azimuthBoardServoStatusInfo.servoBrake,
                      }"
                    ></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{ icdStore.azimuthBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'INACTIVE' }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.azimuthBoardStatusInfo.encoder,
                        'led-error': icdStore.azimuthBoardStatusInfo.encoder,
                      }"
                    ></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ icdStore.azimuthBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.azimuthBoardServoStatusInfo.servoAlarm,
                        'led-error': icdStore.azimuthBoardServoStatusInfo.servoAlarm,
                      }"
                    ></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{ icdStore.azimuthBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL' }}
                    </span>
                  </div>
                </div>

                <!-- Elevation Positioner -->
                <div class="positioner-section">
                  <div class="positioner-title">Elevation</div>

                  <!-- Limit Switch -5˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardStatusInfo.limitSwitchNegative5,
                        'led-off': !icdStore.elevationBoardStatusInfo.limitSwitchNegative5,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch -5˚</span>
                    <span class="status-value">
                      {{
                        icdStore.elevationBoardStatusInfo.limitSwitchNegative5
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch -0˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardStatusInfo.limitSwitchNegative0,
                        'led-off': !icdStore.elevationBoardStatusInfo.limitSwitchNegative0,
                      }"
                    ></div>

                    <span class="status-label">Limit Switch -0˚</span>
                    <span class="status-value">
                      {{
                        icdStore.elevationBoardStatusInfo.limitSwitchNegative0
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +180˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardStatusInfo.limitSwitchPositive180,
                        'led-off': !icdStore.elevationBoardStatusInfo.limitSwitchPositive180,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch +180˚</span>
                    <span class="status-value">
                      {{
                        icdStore.elevationBoardStatusInfo.limitSwitchPositive180
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +185˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardStatusInfo.limitSwitchPositive185,
                        'led-off': !icdStore.elevationBoardStatusInfo.limitSwitchPositive185,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch +185˚</span>
                    <span class="status-value">
                      {{
                        icdStore.elevationBoardStatusInfo.limitSwitchPositive185
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardServoStatusInfo.servoMotor,
                        'led-off': !icdStore.elevationBoardServoStatusInfo.servoMotor,
                      }"
                    ></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ icdStore.elevationBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.elevationBoardServoStatusInfo.servoBrake,
                        'led-off': !icdStore.elevationBoardServoStatusInfo.servoBrake,
                      }"
                    ></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{
                        icdStore.elevationBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.elevationBoardStatusInfo.encoder,
                        'led-error': icdStore.elevationBoardStatusInfo.encoder,
                      }"
                    ></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ icdStore.elevationBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.elevationBoardServoStatusInfo.servoAlarm,
                        'led-error': icdStore.elevationBoardServoStatusInfo.servoAlarm,
                      }"
                    ></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{ icdStore.elevationBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL' }}
                    </span>
                  </div>
                </div>

                <!-- Tilt Positioner -->
                <div class="positioner-section">
                  <div class="positioner-title">Tilt</div>

                  <!-- Limit Switch -275˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.tiltBoardStatusInfo.limitSwitchNegative275,
                        'led-off': !icdStore.tiltBoardStatusInfo.limitSwitchNegative275,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch -275˚</span>
                    <span class="status-value">
                      {{
                        icdStore.tiltBoardStatusInfo.limitSwitchNegative275 ? 'ACTIVE' : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +275˚ -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.tiltBoardStatusInfo.limitSwitchPositive275,
                        'led-off': !icdStore.tiltBoardStatusInfo.limitSwitchPositive275,
                      }"
                    ></div>
                    <span class="status-label">Limit Switch +275˚</span>
                    <span class="status-value">
                      {{
                        icdStore.tiltBoardStatusInfo.limitSwitchPositive275 ? 'ACTIVE' : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.tiltBoardServoStatusInfo.servoMotor,
                        'led-off': !icdStore.tiltBoardServoStatusInfo.servoMotor,
                      }"
                    ></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ icdStore.tiltBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.tiltBoardServoStatusInfo.servoBrake,
                        'led-off': !icdStore.tiltBoardServoStatusInfo.servoBrake,
                      }"
                    ></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{ icdStore.tiltBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'INACTIVE' }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.tiltBoardStatusInfo.encoder,
                        'led-error': icdStore.tiltBoardStatusInfo.encoder,
                      }"
                    ></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ icdStore.tiltBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.tiltBoardServoStatusInfo.servoAlarm,
                        'led-error': icdStore.tiltBoardServoStatusInfo.servoAlarm,
                      }"
                    ></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{ icdStore.tiltBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL' }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Positioner 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Position Status:</span>
                  <span class="summary-value">READY</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Axes:</span>
                  <span class="summary-value">None</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">All Ready:</span>
                  <span class="summary-value text-positive">YES</span>
                </div>
              </div>
               -->
            </q-card-section>
          </q-card>

          <!-- Feed Status 카드 (전체 행 차지) -->
          <q-card class="feed-status-card full-width-card">
            <q-card-section>
              <div class="text-subtitle1 text-weight-bold text-deep-orange q-mb-md">
                Feed Status
              </div>

              <div class="feed-grid">
                <!-- 왼쪽 섹션 - Error Status -->
                <div class="feed-section">
                  <div class="feed-section-title">Error Status</div>

                  <!-- FAN ERROR -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedXBoardStatusInfo.fanError,
                        'led-error': icdStore.feedXBoardStatusInfo.fanError,
                      }"
                    ></div>
                    <span class="status-label">FAN ERROR</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.fanError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- X-Band LNA RHCP Error -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedXBoardStatusInfo.xLnaRHCPError,
                        'led-error': icdStore.feedXBoardStatusInfo.xLnaRHCPError,
                      }"
                    ></div>
                    <span class="status-label">X-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.xLnaRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- X-Band LNA LHCP Error -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedXBoardStatusInfo.xLnaLHCPError,
                        'led-error': icdStore.feedXBoardStatusInfo.xLnaLHCPError,
                      }"
                    ></div>
                    <span class="status-label">X-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.xLnaLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band LNA RHCP Error -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedSBoardStatusInfo.sLnaRHCPError,
                        'led-error': icdStore.feedSBoardStatusInfo.sLnaRHCPError,
                      }"
                    ></div>
                    <span class="status-label">S-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sLnaRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band LNA LHCP Error -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedSBoardStatusInfo.sLnaLHCPError,
                        'led-error': icdStore.feedSBoardStatusInfo.sLnaLHCPError,
                      }"
                    ></div>
                    <span class="status-label">S-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sLnaLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band RF Switch Error -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-off': !icdStore.feedSBoardStatusInfo.sRFSwitchError,
                        'led-error': icdStore.feedSBoardStatusInfo.sRFSwitchError,
                      }"
                    ></div>
                    <span class="status-label">S-Band RF Switch</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sRFSwitchError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>
                </div>

                <!-- 오른쪽 섹션 - Power Status -->
                <div class="feed-section">
                  <div class="feed-section-title">Power Status</div>

                  <!-- FAN Power -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedXBoardStatusInfo.fanPower,
                        'led-off': !icdStore.feedXBoardStatusInfo.fanPower,
                      }"
                    ></div>
                    <span class="status-label">FAN Power</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.fanPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- X-Band LNA RHCP Power -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedXBoardStatusInfo.xLnaRHCPPower,
                        'led-off': !icdStore.feedXBoardStatusInfo.xLnaRHCPPower,
                      }"
                    ></div>
                    <span class="status-label">X-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.xLnaRHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- X-Band LNA LHCP Power -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedXBoardStatusInfo.xLnaLHCPPower,
                        'led-off': !icdStore.feedXBoardStatusInfo.xLnaLHCPPower,
                      }"
                    ></div>
                    <span class="status-label">X-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedXBoardStatusInfo.xLnaLHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band LNA RHCP Power -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedSBoardStatusInfo.sLnaRHCPPower,
                        'led-off': !icdStore.feedSBoardStatusInfo.sLnaRHCPPower,
                      }"
                    ></div>
                    <span class="status-label">S-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sLnaRHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band LNA LHCP Power -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedSBoardStatusInfo.sLnaLHCPPower,
                        'led-off': !icdStore.feedSBoardStatusInfo.sLnaLHCPPower,
                      }"
                    ></div>
                    <span class="status-label">S-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sLnaLHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band RF Switch Mode -->
                  <div class="status-item">
                    <div
                      class="status-led"
                      :class="{
                        'led-on': icdStore.feedSBoardStatusInfo.sRFSwitchMode,
                        'led-off': !icdStore.feedSBoardStatusInfo.sRFSwitchMode,
                      }"
                    ></div>
                    <span class="status-label">S-Band RF Switch</span>
                    <span class="status-value">
                      {{ icdStore.feedSBoardStatusInfo.sRFSwitchMode ? 'ON' : 'OFF' }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Feed 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Feed Status:</span>
                  <span class="summary-value">MONITORING</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Feeds:</span>
                  <span class="summary-value">
                    {{
                      [
                        icdStore.feedSBoardStatusInfo.sLnaLHCPPower && 'S-LHCP',
                        icdStore.feedSBoardStatusInfo.sLnaRHCPPower && 'S-RHCP',
                        icdStore.feedXBoardStatusInfo.xLnaLHCPPower && 'X-LHCP',
                        icdStore.feedXBoardStatusInfo.xLnaRHCPPower && 'X-RHCP',
                        icdStore.feedXBoardStatusInfo.fanPower && 'FAN',
                      ]
                        .filter(Boolean)
                        .join(', ') || 'None'
                    }}
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Error Count:</span>
                  <span class="summary-value text-negative">
                    {{
                      [
                        icdStore.feedXBoardStatusInfo.fanError,
                        icdStore.feedXBoardStatusInfo.xLnaRHCPError,
                        icdStore.feedXBoardStatusInfo.xLnaLHCPError,
                        icdStore.feedSBoardStatusInfo.sLnaRHCPError,
                        icdStore.feedSBoardStatusInfo.sLnaLHCPError,
                        icdStore.feedSBoardStatusInfo.sRFSwitchError,
                      ].filter(Boolean).length
                    }}
                  </span>
                </div>
              </div>
 -->
            </q-card-section>
          </q-card>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="닫기" color="grey-7" @click="handleClose" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { closeWindow } from '../../utils/windowUtils'

const icdStore = useICDStore()
// Computed for template
const isOpen = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const protocolStatusInfo = computed(() => icdStore.protocolStatusInfo)

// Props를 선택적으로 만들기// 🎯 Props 정의
const props = withDefaults(
  defineProps<{
    modelValue?: boolean
  }>(),
  {
    modelValue: true,
  },
)

// 🎯 Emits 정의
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

// 🔍 실행 환경 감지 (isPopupWindow만 필요)
const isPopupWindow = computed(() => window.opener !== null)

// 표시 모드
const displayMode = computed(() => (isPopupWindow.value ? '팝업 창 모드' : '모달 모드'))

// 마지막 업데이트 시간
const lastUpdateTime = ref(new Date().toLocaleTimeString())

// 🚪 범용 닫기 함수
const handleClose = () => {
  console.log('🚪 닫기 요청 - 모드:', displayMode.value)
  closeWindow() // 간단하게 한 줄!
  try {
    if (isPopupWindow.value) {
      // 팝업 창 모드
      console.log('🪟 팝업 창 닫기 시도')

      // 부모 창에 닫기 알림
      if (window.opener && !window.opener.closed) {
        try {
          window.opener.postMessage(
            {
              type: 'popup-closing',
              timestamp: Date.now(),
            },
            window.location.origin,
          )
        } catch (error) {
          console.warn('⚠️ 부모 창 통신 실패:', error)
        }
      }

      // 창 닫기
      window.close()

      // 브라우저에서 창 닫기가 실패할 경우 대비
      setTimeout(() => {
        if (!window.closed) {
          console.warn('⚠️ 자동 창 닫기 실패 - 사용자 액션 필요')
          alert('창을 수동으로 닫아주세요. (Alt+F4 또는 Ctrl+W)')
        }
      }, 100)
    } else {
      // 모달 모드
      console.log('📱 모달 닫기')
      emit('update:modelValue', false)
      emit('close')
    }
  } catch (error) {
    console.error('❌ 닫기 처리 중 오류:', error)

    // 폴백 처리
    if (isPopupWindow.value) {
      alert('창을 수동으로 닫아주세요.')
    } else {
      emit('update:modelValue', false)
    }
  }
}

// 🎹 키보드 이벤트 핸들러
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'Escape':
      console.log('⌨️ ESC 키 - 닫기')
      event.preventDefault()
      handleClose()
      break

    case 'F5':
      console.log('🔄 F5 키 - 새로고침')
      event.preventDefault()
      refreshStatus()
      break

    default:
      // Ctrl+W (창 닫기)
      if (event.ctrlKey && event.key === 'w') {
        console.log('⌨️ Ctrl+W - 창 닫기')

        if (isPopupWindow.value) {
          // 팝업 창: 브라우저 기본 동작 허용
          return
        } else {
          // 모달: 기본 동작 방지하고 커스텀 닫기
          event.preventDefault()
          handleClose()
        }
      }
      break
  }
}

// 🔄 새로고침 함수
const refreshStatus = () => {
  console.log('🔄 상태 새로고침')
  lastUpdateTime.value = new Date().toLocaleTimeString()

  // 팝업 창인 경우 제목 업데이트
  if (isPopupWindow.value) {
    document.title = `All Status - ${lastUpdateTime.value}`
  }
}

// 🎯 라이프사이클 관리
onMounted(() => {
  console.log('📱 AllStatusContent 마운트됨')
  console.log('🔍 실행 환경:', displayMode.value)

  // 키보드 이벤트 리스너 추가
  document.addEventListener('keydown', handleKeydown)

  // 환경별 초기화
  if (isPopupWindow.value) {
    // 팝업 창 초기화
    document.title = 'All Status Information'

    // 부모 창 통신 설정
    window.addEventListener('message', (event) => {
      if (event.origin !== window.location.origin) return

      console.log('📨 부모 창 메시지:', event.data)

      switch (event.data.type) {
        case 'refresh':
          refreshStatus()
          break
        case 'close':
          handleClose()
          break
      }
    })

    // 부모 창에 준비 완료 알림
    if (window.opener && !window.opener.closed) {
      try {
        window.opener.postMessage(
          {
            type: 'popup-ready',
            timestamp: Date.now(),
          },
          window.location.origin,
        )
      } catch (error) {
        console.warn('⚠️ 부모 창 통신 설정 실패:', error)
      }
    }
  } else {
    // 모달 초기화
    console.log('📱 모달 모드로 초기화됨')
  }

  // 초기 새로고침
  setTimeout(refreshStatus, 100)
})

onUnmounted(() => {
  console.log('🧹 AllStatusContent 언마운트됨')

  // 이벤트 리스너 정리
  document.removeEventListener('keydown', handleKeydown)

  // 팝업 창인 경우 부모 창에 종료 알림
  if (isPopupWindow.value && window.opener && !window.opener.closed) {
    try {
      window.opener.postMessage(
        {
          type: 'popup-unmounted',
          timestamp: Date.now(),
        },
        window.location.origin,
      )
    } catch (error) {
      console.warn('⚠️ 부모 창 종료 알림 실패:', error)
    }
  }
})
</script>
<style scoped>
.all-status-modal {
  min-width: 1440px;
  max-width: 1680px;
  width: 95vw;
  max-height: 90vh;
  overflow: auto;
  container-type: inline-size;
}

.q-card-section:nth-child(2) {
  overflow-y: auto;
  max-height: calc(90vh - 120px);
  padding-right: 10px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar {
  width: 8px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  grid-template-rows: auto auto;
  gap: 0.6rem;
}

/* 첫 번째 행: 6개 카드 */
.protocol-status-card {
  grid-column: 1;
  grid-row: 1;
}
.power-status-card {
  grid-column: 2;
  grid-row: 1;
}
.emergency-status-card {
  grid-column: 3;
  grid-row: 1;
}
.servo-power-status-card {
  grid-column: 4;
  grid-row: 1;
}
.stow-status-card {
  grid-column: 5;
  grid-row: 1;
}
.stow-command-status-card {
  grid-column: 6;
  grid-row: 1;
}

/* 두 번째 행: Positioner Status (1~3열) + Feed Status (4~5열) + 공백 (6열) */
.positioner-status-card {
  grid-column: 1 / 4;
  grid-row: 2;
  border: 1px solid var(--q-indigo);
  border-top: 3px solid var(--q-indigo);
}

.feed-status-card {
  grid-column: 4 / 6;
  grid-row: 2;
  border: 1px solid var(--q-deep-orange);
  border-top: 3px solid var(--q-deep-orange);
}

.positioner-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.6rem;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.6rem;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3rem;
  border: 1px solid #e0e0e0;
  border-radius: 3px;
  background-color: rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
  min-width: 0;
  overflow: hidden;
}

.status-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.status-led {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  position: relative;
}

.status-led::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.led-on {
  background-color: #4caf50;
  box-shadow:
    0 0 5px #4caf50,
    0 0 10px #4caf50;
}

.led-on::before {
  background: radial-gradient(circle, rgba(76, 175, 80, 0.3) 0%, transparent 70%);
  animation: pulse-green 2s infinite;
}

.led-off {
  background-color: #666;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.3);
}

.led-error {
  background-color: #f44336;
  box-shadow:
    0 0 5px #f44336,
    0 0 10px #f44336;
}

.led-error::before {
  background: radial-gradient(circle, rgba(244, 67, 54, 0.3) 0%, transparent 70%);
  animation: pulse-red 2s infinite;
}

.status-label {
  font-weight: 500;
  min-width: 66px;
  flex-shrink: 0;
  font-size: 0.84rem;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-value {
  font-weight: 600;
  margin-left: auto;
  font-size: 0.84rem;
  line-height: 1.1;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-item:has(.led-on) .status-value {
  color: #4caf50;
}

.status-item:has(.led-off) .status-value {
  color: #666;
}

.status-item:has(.led-error) .status-value {
  color: #f44336;
}

.status-summary {
  border-top: 1px solid #e0e0e0;
  padding-top: 0.5rem;
  margin-top: 0.5rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.25rem;
  font-size: 0.78rem;
}

.summary-label {
  font-weight: 500;
  color: #666;
}

.summary-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

.text-subtitle1 {
  font-size: 1.08rem !important;
  margin-bottom: 0.6rem !important;
}

.positioner-title,
.feed-section-title {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #666;
}

.q-card {
  margin-bottom: 0.36rem;
  min-width: 0;
}

.q-card-section {
  padding: 0.72rem;
  min-width: 0;
}

@keyframes pulse-green {
  0%,
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.5);
  }
}

@keyframes pulse-red {
  0%,
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.5);
  }
}

/* 다크 모드 */
.body--dark .status-item {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: #444;
}

.body--dark .status-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.body--dark .status-summary {
  border-color: #444;
}

.body--dark .summary-label {
  color: #bbb;
}

.body--dark .positioner-title,
.body--dark .feed-section-title {
  color: #bbb;
}

.body--dark .led-off {
  background-color: #444;
}

.body--dark .status-item:has(.led-off) .status-value {
  color: #999;
}

.body--dark .q-card-section:nth-child(2)::-webkit-scrollbar-track {
  background: #333;
}

.body--dark .q-card-section:nth-child(2)::-webkit-scrollbar-thumb {
  background: #666;
}

.body--dark .q-card-section:nth-child(2)::-webkit-scrollbar-thumb:hover {
  background: #777;
}

/* 컨테이너 쿼리 (더 정확한 반응형) */
@container (max-width: 1400px) {
  .status-grid {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: auto auto auto auto;
  }

  .protocol-status-card {
    grid-column: 1;
    grid-row: 1;
  }
  .power-status-card {
    grid-column: 2;
    grid-row: 1;
  }
  .emergency-status-card {
    grid-column: 3;
    grid-row: 1;
  }
  .servo-power-status-card {
    grid-column: 1;
    grid-row: 2;
  }
  .stow-status-card {
    grid-column: 2;
    grid-row: 2;
  }
  .stow-command-status-card {
    grid-column: 3;
    grid-row: 2;
  }
  .positioner-status-card {
    grid-column: 1 / -1;
    grid-row: 3;
  }
  .feed-status-card {
    grid-column: 1 / -1;
    grid-row: 4;
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.5rem;
  }
}

@container (max-width: 900px) {
  .status-grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(8, auto);
  }

  .protocol-status-card {
    grid-column: 1;
    grid-row: 1;
  }
  .power-status-card {
    grid-column: 1;
    grid-row: 2;
  }
  .emergency-status-card {
    grid-column: 1;
    grid-row: 3;
  }
  .servo-power-status-card {
    grid-column: 1;
    grid-row: 4;
  }
  .stow-status-card {
    grid-column: 1;
    grid-row: 5;
  }
  .stow-command-status-card {
    grid-column: 1;
    grid-row: 6;
  }
  .positioner-status-card {
    grid-column: 1;
    grid-row: 7;
  }
  .feed-status-card {
    grid-column: 1;
    grid-row: 8;
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: 1fr;
    gap: 0.36rem;
  }
}

/* 기존 미디어 쿼리 (폴백용) */
@media (max-width: 1600px) {
  .all-status-modal {
    min-width: 1200px;
    max-width: 1400px;
  }
}

@media (max-width: 1440px) {
  .all-status-modal {
    min-width: 95vw;
  }

  .status-grid {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: auto auto auto auto;
  }

  .protocol-status-card {
    grid-column: 1;
    grid-row: 1;
  }
  .power-status-card {
    grid-column: 2;
    grid-row: 1;
  }
  .emergency-status-card {
    grid-column: 3;
    grid-row: 1;
  }
  .servo-power-status-card {
    grid-column: 1;
    grid-row: 2;
  }
  .stow-status-card {
    grid-column: 2;
    grid-row: 2;
  }
  .stow-command-status-card {
    grid-column: 3;
    grid-row: 2;
  }
  .positioner-status-card {
    grid-column: 1 / -1;
    grid-row: 3;
  }
  .feed-status-card {
    grid-column: 1 / -1;
    grid-row: 4;
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.5rem;
  }

  .status-label {
    min-width: 50px;
    font-size: 0.8rem;
  }

  .status-value {
    font-size: 0.8rem;
  }
}

@media (max-width: 1200px) {
  .status-grid {
    gap: 0.4rem;
  }

  .status-item {
    padding: 0.25rem;
    gap: 0.4rem;
  }

  .status-label {
    min-width: 45px;
    font-size: 0.75rem;
  }

  .status-value {
    font-size: 0.75rem;
  }
}

@media (max-width: 1000px) {
  .status-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(5, auto);
  }

  .protocol-status-card {
    grid-column: 1;
    grid-row: 1;
  }
  .power-status-card {
    grid-column: 2;
    grid-row: 1;
  }
  .emergency-status-card {
    grid-column: 1;
    grid-row: 2;
  }
  .servo-power-status-card {
    grid-column: 2;
    grid-row: 2;
  }
  .stow-status-card {
    grid-column: 1;
    grid-row: 3;
  }
  .stow-command-status-card {
    grid-column: 2;
    grid-row: 3;
  }
  .positioner-status-card {
    grid-column: 1 / -1;
    grid-row: 4;
  }
  .feed-status-card {
    grid-column: 1 / -1;
    grid-row: 5;
  }
}

@media (max-width: 922px) {
  .all-status-modal {
    min-width: 95vw;
    width: 95vw;
    max-height: 85vh;
  }
  .status-grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(8, auto);
  }

  .protocol-status-card {
    grid-column: 1;
    grid-row: 1;
  }
  .power-status-card {
    grid-column: 1;
    grid-row: 2;
  }
  .emergency-status-card {
    grid-column: 1;
    grid-row: 3;
  }
  .servo-power-status-card {
    grid-column: 1;
    grid-row: 4;
  }
  .stow-status-card {
    grid-column: 1;
    grid-row: 5;
  }
  .stow-command-status-card {
    grid-column: 1;
    grid-row: 6;
  }
  .positioner-status-card {
    grid-column: 1;
    grid-row: 7;
  }
  .feed-status-card {
    grid-column: 1;
    grid-row: 8;
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: 1fr;
    gap: 0.36rem;
  }
}

@media (max-width: 800px) {
  .status-item {
    padding: 0.2rem;
    gap: 0.3rem;
  }

  .status-label {
    min-width: 40px;
    font-size: 0.7rem;
  }

  .status-value {
    font-size: 0.7rem;
  }

  .text-subtitle1 {
    font-size: 0.95rem !important;
  }

  .positioner-title,
  .feed-section-title {
    font-size: 0.8rem;
  }
}

@media (max-width: 700px) {
  .all-status-modal {
    min-width: 100vw;
    width: 100vw;
  }

  .status-grid {
    gap: 0.3rem;
  }

  .q-card-section {
    padding: 0.5rem;
  }
}

@media (max-width: 576px) {
  .all-status-modal {
    min-width: 100vw;
    width: 100vw;
    height: 100vh;
    max-height: 100vh;
  }

  .status-item {
    padding: 0.5rem;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.36rem;
  }

  .status-led {
    align-self: center;
  }

  .status-label,
  .status-value {
    align-self: center;
    min-width: auto;
    font-size: 0.8rem;
  }

  .summary-item {
    flex-direction: column;
    gap: 0.18rem;
  }

  .text-subtitle1 {
    font-size: 0.9rem !important;
    text-align: center;
  }

  .positioner-title,
  .feed-section-title {
    font-size: 0.75rem;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .q-card-section {
    padding: 0.4rem;
  }

  .status-grid {
    gap: 0.25rem;
  }

  .status-item {
    padding: 0.4rem;
  }

  .status-label,
  .status-value {
    font-size: 0.75rem;
  }

  .text-subtitle1 {
    font-size: 0.85rem !important;
  }
}

/* 접근성 */
@media (prefers-reduced-motion: reduce) {
  .status-led::before {
    animation: none;
  }

  .status-item {
    transition: none;
  }

  .status-item:hover {
    transform: none;
  }
}

/* 고대비 모드 */
@media (prefers-contrast: high) {
  .status-item {
    border-width: 2px;
    border-color: #000;
  }

  .led-on {
    background-color: #00ff00;
    box-shadow: none;
  }

  .led-error {
    background-color: #ff0000;
    box-shadow: none;
  }

  .led-off {
    background-color: #808080;
    box-shadow: none;
  }
}

/* 인쇄 스타일 */
@media print {
  .all-status-modal {
    box-shadow: none;
    border: 1px solid #000;
  }

  .status-led {
    box-shadow: none;
    border: 1px solid #000;
  }

  .led-on {
    background-color: #fff !important;
    border: 2px solid #000;
  }

  .led-error {
    background-color: #000 !important;
  }

  .led-off {
    background-color: #ccc !important;
    border: 1px solid #000;
  }

  .status-item {
    break-inside: avoid;
  }
}
</style>
