<template>
  <q-dialog v-model="isOpen" persistent>
    <q-card class="all-status-modal">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">All Status Information</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
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
                      {{ icdStore.azimuthBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'RELEASED' }}
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
                        icdStore.elevationBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'RELEASED'
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
                      {{ icdStore.tiltBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'RELEASED' }}
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
        <q-btn flat label="새로고침" color="primary" @click="refreshStatus" />
        <q-btn flat label="닫기" color="grey-7" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useICDStore } from '../../../stores/icd/icdStore'

// Props
interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

// Store
const icdStore = useICDStore()

// 메모리 정보 타입 정의
interface MemoryInfo {
  usedJSHeapSize: number
  totalJSHeapSize: number
  jsHeapSizeLimit: number
}

interface PerformanceWithMemory extends Performance {
  memory?: MemoryInfo
}

// Computed
const isOpen = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const protocolStatusInfo = computed(() => icdStore.protocolStatusInfo)

// 브라우저 성능 정보 (타입 안전하게)
const getBrowserPerformance = () => {
  try {
    // GPU 가속 확인
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
    const gpuAcceleration = gl ? 'ON' : 'OFF'

    // 메모리 정보 (타입 안전하게)
    const performanceWithMemory = performance as PerformanceWithMemory
    const memoryInfo = performanceWithMemory.memory
    const memoryData = memoryInfo
      ? {
          used: Math.round(memoryInfo.usedJSHeapSize / 1024 / 1024),
          total: Math.round(memoryInfo.totalJSHeapSize / 1024 / 1024),
          limit: Math.round(memoryInfo.jsHeapSizeLimit / 1024 / 1024),
        }
      : null

    console.log('🖥️ 브라우저 성능 정보:')
    console.log('GPU 가속:', gpuAcceleration)
    if (memoryData) {
      console.log(
        `메모리: ${memoryData.used}MB / ${memoryData.total}MB (한계: ${memoryData.limit}MB)`,
      )
    }

    return { gpuAcceleration, memoryData }
  } catch (error) {
    console.log('성능 정보를 가져올 수 없습니다:', error)
    return { gpuAcceleration: 'UNKNOWN', memoryData: null }
  }
}

// Methods
const refreshStatus = () => {
  console.log('🔄 상태 새로고침')
  console.log('Protocol Status:', protocolStatusInfo.value)
  console.log('Power Status:', {
    powerSurgeProtector: icdStore.mainBoardStatusInfo.powerSurgeProtector,
    powerReversePhaseSensor: icdStore.mainBoardStatusInfo.powerReversePhaseSensor,
    hasPowerIssue: icdStore.mainBoardStatusInfo.summary?.hasPowerIssue,
  })
  console.log('Emergency Status:', {
    emergencyStopACU: icdStore.mainBoardStatusInfo.emergencyStopACU,
    emergencyStopPositioner: icdStore.mainBoardStatusInfo.emergencyStopPositioner,
    hasEmergencyStop: icdStore.mainBoardStatusInfo.summary?.hasEmergencyStop,
  })
  console.log('Stow Status:', {
    azimuthStowed: icdStore.azimuthBoardStatusInfo.stowPin,
    elevationStowed: icdStore.elevationBoardStatusInfo.stowPin,
    tiltStowed: icdStore.tiltBoardStatusInfo.stowPin,
    stowedAxes: [
      icdStore.azimuthBoardStatusInfo.stowPin && 'Azimuth',
      icdStore.elevationBoardStatusInfo.stowPin && 'Elevation',
      icdStore.tiltBoardStatusInfo.stowPin && 'Tilt',
    ].filter(Boolean),
    allStowed:
      icdStore.azimuthBoardStatusInfo.stowPin &&
      icdStore.elevationBoardStatusInfo.stowPin &&
      icdStore.tiltBoardStatusInfo.stowPin,
  })
  console.log('Connection Status:', {
    isConnected: icdStore.isConnected,
    isUpdating: icdStore.isUpdating,
    updateCount: icdStore.updateCount,
    messageDelay: icdStore.messageDelay,
  })

  getBrowserPerformance()
}
</script>
<style scoped>
.all-status-modal {
  min-width: 1200px; /* 더 넓게 조정 */
  max-width: 1400px;
  width: 95vw;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 3열 그리드 */
  gap: 1rem;
}

/* 전체 행을 차지하는 카드 */
.full-width-card {
  grid-column: 1 / -1; /* 첫 번째 열부터 마지막 열까지 */
}

/* Positioner Status Card */
.positioner-status-card {
  border: 1px solid var(--q-indigo);
  border-top: 3px solid var(--q-indigo);
}

.positioner-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 3열로 배치 */
  gap: 1rem;
}

/* Feed Status Card */
.feed-status-card {
  border: 1px solid var(--q-deep-orange);
  border-top: 3px solid var(--q-deep-orange);
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 3열로 배치 */
  gap: 1rem;
}

/* 통합된 Status Item 스타일 */
.status-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
}

.status-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 통합된 Status LED 스타일 */
.status-led {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
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

/* LED 상태별 색상 - ON (정상/활성) */
.led-on {
  background-color: #4caf50;
  box-shadow:
    0 0 8px #4caf50,
    0 0 16px #4caf50;
}

.led-on::before {
  background: radial-gradient(circle, rgba(76, 175, 80, 0.3) 0%, transparent 70%);
  animation: pulse-green 2s infinite;
}

/* LED 상태별 색상 - OFF (비활성) */
.led-off {
  background-color: #666;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

/* LED 상태별 색상 - ERROR (에러) */
.led-error {
  background-color: #f44336;
  box-shadow:
    0 0 8px #f44336,
    0 0 16px #f44336;
}

.led-error::before {
  background: radial-gradient(circle, rgba(244, 67, 54, 0.3) 0%, transparent 70%);
  animation: pulse-red 2s infinite;
}

/* 통합된 Status Label & Value */
.status-label {
  font-weight: 500;
  min-width: 80px;
  flex-shrink: 0;
}

.status-value {
  font-weight: 600;
  margin-left: auto;
}

/* 상태별 텍스트 색상 */
.status-item:has(.led-on) .status-value {
  color: #4caf50;
}

.status-item:has(.led-off) .status-value {
  color: #666;
}

.status-item:has(.led-error) .status-value {
  color: #f44336;
}

/* 통합된 Status Summary */
.status-summary {
  border-top: 1px solid #e0e0e0;
  padding-top: 1rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.summary-label {
  font-weight: 500;
  color: #666;
}

.summary-value {
  font-weight: 600;
  font-family: 'Courier New', monospace;
}

/* LED 애니메이션 */
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

/* 다크 모드 지원 */
.body--dark .status-item {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: #444;
}

.body--dark .status-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.body--dark .status-summary,
.body--dark .connection-stats,
.body--dark .update-interval-section,
.body--dark .timer-accuracy-section,
.body--dark .memory-section {
  border-color: #444;
}

.body--dark .summary-label,
.body--dark .stat-label,
.body--dark .interval-stat-label,
.body--dark .timer-stat-label,
.body--dark .memory-stat-label {
  color: #bbb;
}

.body--dark .interval-title,
.body--dark .timer-title,
.body--dark .memory-title {
  color: #bbb;
}

.body--dark .led-off {
  background-color: #444;
}

.body--dark .status-item:has(.led-off) .status-value {
  color: #999;
}

.body--dark .connection-item {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: #444;
}

.body--dark .connection-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

/* 반응형 디자인 */
@media (max-width: 1200px) {
  .status-grid {
    grid-template-columns: repeat(2, 1fr); /* 2열로 변경 */
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: repeat(2, 1fr); /* 2열로 변경 */
  }
}

@media (max-width: 768px) {
  .all-status-modal {
    min-width: 95vw;
    width: 95vw;
  }

  .status-grid {
    grid-template-columns: 1fr; /* 1열로 변경 */
  }

  .positioner-grid,
  .feed-grid {
    grid-template-columns: 1fr; /* 1열로 변경 */
  }
}

@media (max-width: 480px) {
  .all-status-modal {
    min-width: 100vw;
    width: 100vw;
    height: 100vh;
    max-height: 100vh;
  }

  .status-item {
    padding: 0.75rem;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .status-led {
    align-self: center;
  }

  .status-label,
  .status-value {
    align-self: center;
    min-width: auto;
  }

  .summary-item,
  .stat-item,
  .interval-stat-item,
  .timer-stat-item,
  .memory-stat-item {
    flex-direction: column;
    gap: 0.25rem;
  }

  .connection-item {
    padding: 0.75rem;
  }

  .connection-label {
    min-width: auto;
  }
}

/* 접근성 개선 */
@media (prefers-reduced-motion: reduce) {
  .status-led::before,
  .connection-led::before {
    animation: none;
  }

  .status-item,
  .connection-item {
    transition: none;
  }

  .status-item:hover,
  .connection-item:hover {
    transform: none;
  }
}

/* 고대비 모드 지원 */
@media (prefers-contrast: high) {
  .status-item,
  .connection-item {
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

  .led-green {
    background-color: #00ff00;
    box-shadow: none;
  }

  .led-red {
    background-color: #ff0000;
    box-shadow: none;
  }
}

/* 인쇄 스타일 */
@media print {
  .all-status-modal {
    box-shadow: none;
    border: 1px solid #000;
  }

  .status-led,
  .connection-led {
    box-shadow: none;
    border: 1px solid #000;
  }

  .led-on,
  .led-green {
    background-color: #fff !important;
    border: 2px solid #000;
  }

  .led-error,
  .led-red {
    background-color: #000 !important;
  }

  .led-off {
    background-color: #ccc !important;
    border: 1px solid #000;
  }

  .status-item,
  .connection-item {
    break-inside: avoid;
  }
}
</style>
