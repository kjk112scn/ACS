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
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.protocolStatusInfo.elevation,
                    'led-error': realtimeData.protocolStatusInfo.elevation,
                  }"></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ realtimeData.protocolStatusInfo.elevation ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Azimuth Protocol -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.protocolStatusInfo.azimuth,
                    'led-error': realtimeData.protocolStatusInfo.azimuth,
                  }"></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ realtimeData.protocolStatusInfo.azimuth ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Train Protocol -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.protocolStatusInfo.train,
                    'led-error': realtimeData.protocolStatusInfo.train,
                  }"></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ realtimeData.protocolStatusInfo.train ? 'ERROR' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Feed Protocol -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.protocolStatusInfo.feed,
                    'led-error': realtimeData.protocolStatusInfo.feed,
                  }"></div>
                  <span class="status-label">Feed</span>
                  <span class="status-value">
                    {{ realtimeData.protocolStatusInfo.feed ? 'ERROR' : 'NORMAL' }}
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
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.mainBoardStatusInfo.powerSurgeProtector,
                    'led-error': realtimeData.mainBoardStatusInfo.powerSurgeProtector,
                  }"></div>
                  <span class="status-label">Surge Protector</span>
                  <span class="status-value">
                    {{ realtimeData.mainBoardStatusInfo.powerSurgeProtector ? 'FAULT' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Power Reverse Phase Sensor -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.mainBoardStatusInfo.powerReversePhaseSensor,
                    'led-error': realtimeData.mainBoardStatusInfo.powerReversePhaseSensor,
                  }"></div>
                  <span class="status-label">Reverse Phase</span>
                  <span class="status-value">
                    {{
                      realtimeData.mainBoardStatusInfo.powerReversePhaseSensor ? 'FAULT' : 'NORMAL'
                    }}
                  </span>
                </div>
              </div>

              <!-- Power 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Raw Bits:</span>
                  <span class="summary-value">{{ icdStore.realtimeData.mainBoardStatusInfo.raw }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Power Issues:</span>
                  <span
                    class="summary-value"
                    :class="{
                      'text-negative': icdStore.realtimeData.mainBoardStatusInfo.summary?.hasPowerIssue,
                      'text-positive': !icdStore.realtimeData.mainBoardStatusInfo.summary?.hasPowerIssue,
                    }"
                  >
                    {{ icdStore.realtimeData.mainBoardStatusInfo.summary?.hasPowerIssue ? 'DETECTED' : 'NONE' }}
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
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.mainBoardStatusInfo.emergencyStopACU,
                    'led-error': realtimeData.mainBoardStatusInfo.emergencyStopACU,
                  }"></div>
                  <span class="status-label">ACU</span>
                  <span class="status-value">
                    {{ realtimeData.mainBoardStatusInfo.emergencyStopACU ? 'STOP' : 'NORMAL' }}
                  </span>
                </div>

                <!-- Emergency Stop Positioner -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': !realtimeData.mainBoardStatusInfo.emergencyStopPositioner,
                    'led-error': realtimeData.mainBoardStatusInfo.emergencyStopPositioner,
                  }"></div>
                  <span class="status-label">Positioner</span>
                  <span class="status-value">
                    {{
                      realtimeData.mainBoardStatusInfo.emergencyStopPositioner ? 'STOP' : 'NORMAL'
                    }}
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
                      'text-negative': icdStore.realtimeData.mainBoardStatusInfo.summary?.hasEmergencyStop,
                      'text-positive': !icdStore.realtimeData.mainBoardStatusInfo.summary?.hasEmergencyStop,
                    }"
                  >
                    {{ icdStore.realtimeData.mainBoardStatusInfo.summary?.hasEmergencyStop ? 'ACTIVE' : 'NONE' }}
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
                  <div class="status-led" :class="{
                    'led-on': realtimeData.mainBoardMCOnOffInfo.mcAzimuth,
                    'led-off': !realtimeData.mainBoardMCOnOffInfo.mcAzimuth,
                  }"></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ realtimeData.mainBoardMCOnOffInfo.mcAzimuth ? 'ON' : 'OFF' }}
                  </span>
                </div>

                <!-- Elevation Servo Power -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': realtimeData.mainBoardMCOnOffInfo.mcElevation,
                    'led-off': !realtimeData.mainBoardMCOnOffInfo.mcElevation,
                  }"></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ realtimeData.mainBoardMCOnOffInfo.mcElevation ? 'ON' : 'OFF' }}
                  </span>
                </div>

                <!-- Train Servo Power -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': realtimeData.mainBoardMCOnOffInfo.mcTrain,
                    'led-off': !realtimeData.mainBoardMCOnOffInfo.mcTrain,
                  }"></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ realtimeData.mainBoardMCOnOffInfo.mcTrain ? 'ON' : 'OFF' }}
                  </span>
                </div>
              </div>

              <!-- Servo Power 요약 정보 -->
              <!--
              <div class="status-summary q-mt-md">
                <div class="summary-item">
                  <span class="summary-label">Raw Bits:</span>
                  <span class="summary-value">{{ icdStore.realtimeData.mainBoardMCOnOffInfo.raw }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Active Servos:</span>
                  <span class="summary-value">
                    {{
                      [
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcAzimuth && 'Azimuth',
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcElevation && 'Elevation',
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcTrain && 'Train',
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
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcAzimuth &&
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcElevation &&
                        icdStore.realtimeData.mainBoardMCOnOffInfo.mcTrain,
                      'text-warning':
                        (icdStore.realtimeData.mainBoardMCOnOffInfo.mcAzimuth ||
                          icdStore.realtimeData.mainBoardMCOnOffInfo.mcElevation ||
                          icdStore.realtimeData.mainBoardMCOnOffInfo.mcTrain) &&
                        !(
                          realtimeData.mainBoardMCOnOffInfo.mcAzimuth &&
                          realtimeData.mainBoardMCOnOffInfo.mcElevation &&
                          realtimeData.mainBoardMCOnOffInfo.mcTrain
                        ),
                      'text-negative':
                        !realtimeData.mainBoardMCOnOffInfo.mcAzimuth &&
                        !realtimeData.mainBoardMCOnOffInfo.mcElevation &&
                        !realtimeData.mainBoardMCOnOffInfo.mcTrain,
                    }"
                  >
                    {{
                      realtimeData.mainBoardMCOnOffInfo.mcAzimuth &&
                      realtimeData.mainBoardMCOnOffInfo.mcElevation &&
                      realtimeData.mainBoardMCOnOffInfo.mcTrain
                        ? 'ON'
                        : realtimeData.mainBoardMCOnOffInfo.mcAzimuth ||
                            realtimeData.mainBoardMCOnOffInfo.mcElevation ||
                            realtimeData.mainBoardMCOnOffInfo.mcTrain
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
                  <div class="status-led" :class="{
                    'led-on': realtimeData.azimuthBoardStatusInfo.stowPin,
                    'led-off': !realtimeData.azimuthBoardStatusInfo.stowPin,
                  }"></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value">
                    {{ realtimeData.azimuthBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
                  </span>
                </div>

                <!-- Elevation Stow -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': realtimeData.elevationBoardStatusInfo.stowPin,
                    'led-off': !realtimeData.elevationBoardStatusInfo.stowPin,
                  }"></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value">
                    {{ realtimeData.elevationBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
                  </span>
                </div>

                <!-- Train Stow -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': realtimeData.trainBoardStatusInfo.stowPin,
                    'led-off': !realtimeData.trainBoardStatusInfo.stowPin,
                  }"></div>
                  <span class="status-label">Tilt</span>
                  <span class="status-value">
                    {{ realtimeData.trainBoardStatusInfo.stowPin ? 'STOWED' : 'UNSTOWED' }}
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




                        icdStore.trainBoardStatusInfo.stowPin && 'Train'
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
                        icdStore.trainBoardStatusInfo.stowPin,
                      'text-warning':
                        (icdStore.azimuthBoardStatusInfo.stowPin ||
                          icdStore.elevationBoardStatusInfo.stowPin ||
                          icdStore.trainBoardStatusInfo.stowPin) &&
                        !(
                          icdStore.azimuthBoardStatusInfo.stowPin &&
                          icdStore.elevationBoardStatusInfo.stowPin &&
                          icdStore.trainBoardStatusInfo.stowPin
                        ),
                      'text-negative':
                        !icdStore.azimuthBoardStatusInfo.stowPin &&
                        !icdStore.elevationBoardStatusInfo.stowPin &&
                        !icdStore.trainBoardStatusInfo.stowPin,
                    }"
                  >
                    {{
                      icdStore.azimuthBoardStatusInfo.stowPin &&
                      icdStore.elevationBoardStatusInfo.stowPin &&
                      icdStore.trainBoardStatusInfo.stowPin
                        ? 'YES'
                        : icdStore.azimuthBoardStatusInfo.stowPin ||
                            icdStore.elevationBoardStatusInfo.stowPin ||
                            icdStore.trainBoardStatusInfo.stowPin
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
                  <div class="status-led" :class="{
                    'led-on': false,
                    'led-off': true,
                  }"></div>
                  <span class="status-label">Azimuth</span>
                  <span class="status-value"> READY </span>
                </div>

                <!-- Elevation Stow -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': false,
                    'led-off': true,
                  }"></div>
                  <span class="status-label">Elevation</span>
                  <span class="status-value"> READY </span>
                </div>

                <!-- Train Stow -->
                <div class="status-item">
                  <div class="status-led" :class="{
                    'led-on': false,
                    'led-off': true,
                  }"></div>
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
                    <div class="status-led" :class="{
                      'led-on': realtimeData.azimuthBoardStatusInfo.limitSwitchNegative275,
                      'led-off': !realtimeData.azimuthBoardStatusInfo.limitSwitchNegative275,
                    }"></div>
                    <span class="status-label">Limit Switch -275˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.azimuthBoardStatusInfo.limitSwitchNegative275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +275˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.azimuthBoardStatusInfo.limitSwitchPositive275,
                      'led-off': !realtimeData.azimuthBoardStatusInfo.limitSwitchPositive275,
                    }"></div>
                    <span class="status-label">Limit Switch +275˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.azimuthBoardStatusInfo.limitSwitchPositive275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.azimuthBoardServoStatusInfo.servoMotor,
                      'led-off': !realtimeData.azimuthBoardServoStatusInfo.servoMotor,
                    }"></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ realtimeData.azimuthBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.azimuthBoardServoStatusInfo.servoBrake,
                      'led-off': !realtimeData.azimuthBoardServoStatusInfo.servoBrake,
                    }"></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{
                        realtimeData.azimuthBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.azimuthBoardStatusInfo.encoder,
                      'led-error': realtimeData.azimuthBoardStatusInfo.encoder,
                    }"></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ realtimeData.azimuthBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.azimuthBoardServoStatusInfo.servoAlarm,
                      'led-error': realtimeData.azimuthBoardServoStatusInfo.servoAlarm,
                    }"></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{ realtimeData.azimuthBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL' }}
                    </span>
                  </div>
                </div>

                <!-- Elevation Positioner -->
                <div class="positioner-section">
                  <div class="positioner-title">Elevation</div>

                  <!-- Limit Switch -5˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardStatusInfo.limitSwitchNegative5,
                      'led-off': !realtimeData.elevationBoardStatusInfo.limitSwitchNegative5,
                    }"></div>
                    <span class="status-label">Limit Switch -5˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardStatusInfo.limitSwitchNegative5
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch -0˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardStatusInfo.limitSwitchNegative0,
                      'led-off': !realtimeData.elevationBoardStatusInfo.limitSwitchNegative0,
                    }"></div>

                    <span class="status-label">Limit Switch -0˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardStatusInfo.limitSwitchNegative0
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +180˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardStatusInfo.limitSwitchPositive180,
                      'led-off': !realtimeData.elevationBoardStatusInfo.limitSwitchPositive180,
                    }"></div>
                    <span class="status-label">Limit Switch +180˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardStatusInfo.limitSwitchPositive180
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +185˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardStatusInfo.limitSwitchPositive185,
                      'led-off': !realtimeData.elevationBoardStatusInfo.limitSwitchPositive185,
                    }"></div>
                    <span class="status-label">Limit Switch +185˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardStatusInfo.limitSwitchPositive185
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardServoStatusInfo.servoMotor,
                      'led-off': !realtimeData.elevationBoardServoStatusInfo.servoMotor,
                    }"></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ realtimeData.elevationBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.elevationBoardServoStatusInfo.servoBrake,
                      'led-off': !realtimeData.elevationBoardServoStatusInfo.servoBrake,
                    }"></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardServoStatusInfo.servoBrake
                          ? 'ENGAGED'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.elevationBoardStatusInfo.encoder,
                      'led-error': realtimeData.elevationBoardStatusInfo.encoder,
                    }"></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ realtimeData.elevationBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.elevationBoardServoStatusInfo.servoAlarm,
                      'led-error': realtimeData.elevationBoardServoStatusInfo.servoAlarm,
                    }"></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{
                        realtimeData.elevationBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL'
                      }}
                    </span>
                  </div>
                </div>

                <!-- T Positioner -->
                <div class="positioner-section">
                  <div class="positioner-title">Tilt</div>

                  <!-- Limit Switch -275˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.trainBoardStatusInfo.limitSwitchNegative275,
                      'led-off': !realtimeData.trainBoardStatusInfo.limitSwitchNegative275,
                    }"></div>
                    <span class="status-label">Limit Switch -275˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.trainBoardStatusInfo.limitSwitchNegative275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Limit Switch +275˚ -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.trainBoardStatusInfo.limitSwitchPositive275,
                      'led-off': !realtimeData.trainBoardStatusInfo.limitSwitchPositive275,
                    }"></div>
                    <span class="status-label">Limit Switch +275˚</span>
                    <span class="status-value">
                      {{
                        realtimeData.trainBoardStatusInfo.limitSwitchPositive275
                          ? 'ACTIVE'
                          : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Motor -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.trainBoardServoStatusInfo.servoMotor,
                      'led-off': !realtimeData.trainBoardServoStatusInfo.servoMotor,
                    }"></div>
                    <span class="status-label">Servo Motor</span>
                    <span class="status-value">
                      {{ realtimeData.trainBoardServoStatusInfo.servoMotor ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Servo Brake -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.trainBoardServoStatusInfo.servoBrake,
                      'led-off': !realtimeData.trainBoardServoStatusInfo.servoBrake,
                    }"></div>
                    <span class="status-label">Servo Brake</span>
                    <span class="status-value">
                      {{
                        realtimeData.trainBoardServoStatusInfo.servoBrake ? 'ENGAGED' : 'INACTIVE'
                      }}
                    </span>
                  </div>

                  <!-- Servo Encoder -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.trainBoardStatusInfo.encoder,
                      'led-error': realtimeData.trainBoardStatusInfo.encoder,
                    }"></div>
                    <span class="status-label">Servo Encoder</span>
                    <span class="status-value">
                      {{ realtimeData.trainBoardStatusInfo.encoder ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Servo Alarm -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.trainBoardServoStatusInfo.servoAlarm,
                      'led-error': realtimeData.trainBoardServoStatusInfo.servoAlarm,
                    }"></div>
                    <span class="status-label">Servo Alarm</span>
                    <span class="status-value">
                      {{ realtimeData.trainBoardServoStatusInfo.servoAlarm ? 'ALARM' : 'NORMAL' }}
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
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedBoardETCStatusInfo.fanError,
                      'led-error': realtimeData.feedBoardETCStatusInfo.fanError,
                    }"></div>
                    <span class="status-label">FAN ERROR</span>
                    <span class="status-value">
                      {{ realtimeData.feedBoardETCStatusInfo.fanError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Ka-Band LNA RHCP Error -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaLnaRHCPError,
                      'led-error': realtimeData.feedKaBoardStatusInfo.kaLnaRHCPError,
                    }"></div>
                    <span class="status-label">Ka-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaLnaRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Ka-Band LNA LHCP Error -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaLnaLHCPError,
                      'led-error': realtimeData.feedKaBoardStatusInfo.kaLnaLHCPError,
                    }"></div>
                    <span class="status-label">Ka-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaLnaLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Ka-Band Selection RHCP Error -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaSelectionRHCPError,
                      'led-error': realtimeData.feedKaBoardStatusInfo.kaSelectionRHCPError,
                    }"></div>
                    <span class="status-label">Ka Selection RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaSelectionRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- Ka-Band Selection LHCP Error -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaSelectionLHCPError,
                      'led-error': realtimeData.feedKaBoardStatusInfo.kaSelectionLHCPError,
                    }"></div>
                    <span class="status-label">Ka Selection LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaSelectionLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- X-Band LNA RHCP Error -->
                  <div v-if="isXBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedXBoardStatusInfo.xLnaRHCPError,
                      'led-error': realtimeData.feedXBoardStatusInfo.xLnaRHCPError,
                    }"></div>
                    <span class="status-label">X-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedXBoardStatusInfo.xLnaRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- X-Band LNA LHCP Error -->
                  <div v-if="isXBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedXBoardStatusInfo.xLnaLHCPError,
                      'led-error': realtimeData.feedXBoardStatusInfo.xLnaLHCPError,
                    }"></div>
                    <span class="status-label">X-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedXBoardStatusInfo.xLnaLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band LNA RHCP Error -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedSBoardStatusInfo.sLnaRHCPError,
                      'led-error': realtimeData.feedSBoardStatusInfo.sLnaRHCPError,
                    }"></div>
                    <span class="status-label">S-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedSBoardStatusInfo.sLnaRHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band LNA LHCP Error -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedSBoardStatusInfo.sLnaLHCPError,
                      'led-error': realtimeData.feedSBoardStatusInfo.sLnaLHCPError,
                    }"></div>
                    <span class="status-label">S-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedSBoardStatusInfo.sLnaLHCPError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>

                  <!-- S-Band RF Switch Error -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-off': !realtimeData.feedBoardETCStatusInfo.rfSwitchError,
                      'led-error': realtimeData.feedBoardETCStatusInfo.rfSwitchError,
                    }"></div>
                    <span class="status-label">S-Band RF Switch</span>
                    <span class="status-value">
                      {{ realtimeData.feedBoardETCStatusInfo.rfSwitchError ? 'ERROR' : 'NORMAL' }}
                    </span>
                  </div>
                </div>

                <!-- 오른쪽 섹션 - Power Status -->
                <div class="feed-section">
                  <div class="feed-section-title">Power Status</div>

                  <!-- FAN Power -->
                  <div class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedBoardETCStatusInfo.fanPower,
                      'led-off': !realtimeData.feedBoardETCStatusInfo.fanPower,
                    }"></div>
                    <span class="status-label">FAN Power</span>
                    <span class="status-value">
                      {{ realtimeData.feedBoardETCStatusInfo.fanPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Ka-Band LNA RHCP Power -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedKaBoardStatusInfo.kaLnaRHCPPower,
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaLnaRHCPPower,
                    }"></div>
                    <span class="status-label">Ka-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaLnaRHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- Ka-Band LNA LHCP Power -->
                  <div v-if="isKaBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedKaBoardStatusInfo.kaLnaLHCPPower,
                      'led-off': !realtimeData.feedKaBoardStatusInfo.kaLnaLHCPPower,
                    }"></div>
                    <span class="status-label">Ka-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedKaBoardStatusInfo.kaLnaLHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- X-Band LNA RHCP Power -->
                  <div v-if="isXBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedXBoardStatusInfo.xLnaRHCPPower,
                      'led-off': !realtimeData.feedXBoardStatusInfo.xLnaRHCPPower,
                    }"></div>
                    <span class="status-label">X-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedXBoardStatusInfo.xLnaRHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- X-Band LNA LHCP Power -->
                  <div v-if="isXBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedXBoardStatusInfo.xLnaLHCPPower,
                      'led-off': !realtimeData.feedXBoardStatusInfo.xLnaLHCPPower,
                    }"></div>
                    <span class="status-label">X-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedXBoardStatusInfo.xLnaLHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band LNA RHCP Power -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedSBoardStatusInfo.sLnaRHCPPower,
                      'led-off': !realtimeData.feedSBoardStatusInfo.sLnaRHCPPower,
                    }"></div>
                    <span class="status-label">S-Band LNA RHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedSBoardStatusInfo.sLnaRHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band LNA LHCP Power -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedSBoardStatusInfo.sLnaLHCPPower,
                      'led-off': !realtimeData.feedSBoardStatusInfo.sLnaLHCPPower,
                    }"></div>
                    <span class="status-label">S-Band LNA LHCP</span>
                    <span class="status-value">
                      {{ realtimeData.feedSBoardStatusInfo.sLnaLHCPPower ? 'ON' : 'OFF' }}
                    </span>
                  </div>

                  <!-- S-Band RF Switch Mode -->
                  <div v-if="isSBandEnabled" class="status-item">
                    <div class="status-led" :class="{
                      'led-on': realtimeData.feedBoardETCStatusInfo.rfSwitchMode,
                      'led-off': !realtimeData.feedBoardETCStatusInfo.rfSwitchMode,
                    }"></div>
                    <span class="status-label">S-Band RF Switch</span>
                    <span class="status-value">
                      {{ realtimeData.feedSBoardStatusInfo.sRFSwitchMode ? 'ON' : 'OFF' }}
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useSharedICDStore } from '../../composables/useSharedStore'
import { closeWindow } from '../../utils/windowUtils'
import { useFeedSettingsStore } from '@/stores/ui/feedSettingsStore'

const icdStore = useSharedICDStore()
const feedSettingsStore = useFeedSettingsStore()

// ✅ Pinia 스토어의 반응형 값 추출 (반응형 보장)
const { enabledBands, isSBandEnabled, isXBandEnabled, isKaBandEnabled } = storeToRefs(feedSettingsStore)

// ✅ 피드 설정 변경 감지 (반응형 보장)
watch(
  enabledBands,
  (newBands) => {
    console.log('🔄 피드 설정 변경 감지:', newBands)
    console.log('📊 밴드 상태:', {
      s: isSBandEnabled.value,
      x: isXBandEnabled.value,
      ka: isKaBandEnabled.value,
    })
  },
  { deep: true, immediate: true }
)

// ✅ 각 밴드별 computed 변경 감지 (추가 보장)
watch(
  isKaBandEnabled,
  (isEnabled) => {
    console.log('🔄 Ka-Band 상태 변경:', isEnabled)
  }
)
// Computed for template
const isOpen = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

// ✅ 실시간 업데이트를 위한 반응형 상태
const realtimeData = ref({
  updateCount: 0,
  serverTime: '',
  isConnected: false,

  // Protocol Status
  protocolStatusInfo: {
    elevation: false,
    azimuth: false,
    train: false,
    feed: false,
  },

  // Main Board Status
  mainBoardStatusInfo: {
    powerSurgeProtector: false,
    powerReversePhaseSensor: false,
    emergencyStopACU: false,
    emergencyStopPositioner: false,
  },

  // MC On/Off Status
  mainBoardMCOnOffInfo: {
    mcAzimuth: false,
    mcElevation: false,
    mcTrain: false,
  },

  // Azimuth Board Status
  azimuthBoardStatusInfo: {
    stowPin: false,
    limitSwitchNegative275: false,
    limitSwitchPositive275: false,
    encoder: false,
  },

  // Elevation Board Status
  elevationBoardStatusInfo: {
    stowPin: false,
    limitSwitchNegative5: false,
    limitSwitchNegative0: false,
    limitSwitchPositive180: false,
    limitSwitchPositive185: false,
    encoder: false,
  },

  // Train Board Status
  trainBoardStatusInfo: {
    stowPin: false,
    limitSwitchNegative275: false,
    limitSwitchPositive275: false,
    encoder: false,
  },

  // Azimuth Servo Status
  azimuthBoardServoStatusInfo: {
    servoMotor: false,
    servoBrake: false,
    servoAlarm: false,
  },

  // Elevation Servo Status
  elevationBoardServoStatusInfo: {
    servoMotor: false,
    servoBrake: false,
    servoAlarm: false,
  },

  // Train Servo Status
  trainBoardServoStatusInfo: {
    servoMotor: false,
    servoBrake: false,
    servoAlarm: false,
  },

  // Feed X Board Status
  feedXBoardStatusInfo: {
    xLnaRHCPError: false,
    xLnaRHCPPower: false,
    xLnaLHCPError: false,
    xLnaLHCPPower: false,
  },

  // Feed S Board Status
  feedSBoardStatusInfo: {
    sLnaRHCPError: false,
    sLnaRHCPPower: false,
    sLnaLHCPError: false,
    sLnaLHCPPower: false,
    sRFSwitchError: false,
    sRFSwitchMode: false,
  },

  // Feed ETC Board Status
  feedBoardETCStatusInfo: {
    rfSwitchMode: false,
    rfSwitchError: false,
    fanPower: false,
    fanError: false,
  },

  // Feed Ka Board Status
  feedKaBoardStatusInfo: {
    kaLnaRHCPError: false,
    kaLnaRHCPPower: false,
    kaLnaLHCPError: false,
    kaLnaLHCPPower: false,
    kaSelectionRHCPError: false,
    kaSelectionRHCPBand: 'Band1',
    kaSelectionLHCPError: false,
    kaSelectionLHCPBand: 'Band1',
  },
})

// ✅ 실시간 데이터 업데이트 함수
const updateRealtimeData = () => {
  realtimeData.value = {
    updateCount: icdStore.updateCount,
    serverTime: icdStore.serverTime,
    isConnected: icdStore.isConnected,

    // Protocol Status
    protocolStatusInfo: {
      elevation: icdStore.protocolStatusInfo.elevation,
      azimuth: icdStore.protocolStatusInfo.azimuth,
      train: icdStore.protocolStatusInfo.train,
      feed: icdStore.protocolStatusInfo.feed,
    },

    // Main Board Status
    mainBoardStatusInfo: {
      powerSurgeProtector: icdStore.mainBoardStatusInfo.powerSurgeProtector,
      powerReversePhaseSensor: icdStore.mainBoardStatusInfo.powerReversePhaseSensor,
      emergencyStopACU: icdStore.mainBoardStatusInfo.emergencyStopACU,
      emergencyStopPositioner: icdStore.mainBoardStatusInfo.emergencyStopPositioner,
    },

    // MC On/Off Status
    mainBoardMCOnOffInfo: {
      mcAzimuth: icdStore.mainBoardMCOnOffInfo.mcAzimuth,
      mcElevation: icdStore.mainBoardMCOnOffInfo.mcElevation,
      mcTrain: icdStore.mainBoardMCOnOffInfo.mcTrain,
    },

    // Azimuth Board Status
    azimuthBoardStatusInfo: {
      stowPin: icdStore.azimuthBoardStatusInfo.stowPin,
      limitSwitchNegative275: icdStore.azimuthBoardStatusInfo.limitSwitchNegative275,
      limitSwitchPositive275: icdStore.azimuthBoardStatusInfo.limitSwitchPositive275,
      encoder: icdStore.azimuthBoardStatusInfo.encoder,
    },

    // Elevation Board Status
    elevationBoardStatusInfo: {
      stowPin: icdStore.elevationBoardStatusInfo.stowPin,
      limitSwitchNegative5: icdStore.elevationBoardStatusInfo.limitSwitchNegative5,
      limitSwitchNegative0: icdStore.elevationBoardStatusInfo.limitSwitchNegative0,
      limitSwitchPositive180: icdStore.elevationBoardStatusInfo.limitSwitchPositive180,
      limitSwitchPositive185: icdStore.elevationBoardStatusInfo.limitSwitchPositive185,
      encoder: icdStore.elevationBoardStatusInfo.encoder,
    },

    // Train Board Status
    trainBoardStatusInfo: {
      stowPin: icdStore.trainBoardStatusInfo.stowPin,
      limitSwitchNegative275: icdStore.trainBoardStatusInfo.limitSwitchNegative275,
      limitSwitchPositive275: icdStore.trainBoardStatusInfo.limitSwitchPositive275,
      encoder: icdStore.trainBoardStatusInfo.encoder,
    },

    // Azimuth Servo Status
    azimuthBoardServoStatusInfo: {
      servoMotor: icdStore.azimuthBoardServoStatusInfo.servoMotor,
      servoBrake: icdStore.azimuthBoardServoStatusInfo.servoBrake,
      servoAlarm: icdStore.azimuthBoardServoStatusInfo.servoAlarm,
    },

    // Elevation Servo Status
    elevationBoardServoStatusInfo: {
      servoMotor: icdStore.elevationBoardServoStatusInfo.servoMotor,
      servoBrake: icdStore.elevationBoardServoStatusInfo.servoBrake,
      servoAlarm: icdStore.elevationBoardServoStatusInfo.servoAlarm,
    },

    // Train Servo Status
    trainBoardServoStatusInfo: {
      servoMotor: icdStore.trainBoardServoStatusInfo.servoMotor,
      servoBrake: icdStore.trainBoardServoStatusInfo.servoBrake,
      servoAlarm: icdStore.trainBoardServoStatusInfo.servoAlarm,
    },

    // Feed X Board Status
    feedXBoardStatusInfo: {
      xLnaRHCPError: icdStore.feedXBoardStatusInfo.xLnaRHCPError,
      xLnaRHCPPower: icdStore.feedXBoardStatusInfo.xLnaRHCPPower,
      xLnaLHCPError: icdStore.feedXBoardStatusInfo.xLnaLHCPError,
      xLnaLHCPPower: icdStore.feedXBoardStatusInfo.xLnaLHCPPower,
    },

    // Feed S Board Status
    feedSBoardStatusInfo: {
      sLnaRHCPError: icdStore.feedSBoardStatusInfo.sLnaRHCPError,
      sLnaRHCPPower: icdStore.feedSBoardStatusInfo.sLnaRHCPPower,
      sLnaLHCPError: icdStore.feedSBoardStatusInfo.sLnaLHCPError,
      sLnaLHCPPower: icdStore.feedSBoardStatusInfo.sLnaLHCPPower,
      sRFSwitchError: icdStore.feedBoardETCStatusInfo.rfSwitchError,
      sRFSwitchMode: icdStore.feedBoardETCStatusInfo.rfSwitchStatus.isLHCP,
    },

    // Feed ETC Board Status
    feedBoardETCStatusInfo: {
      rfSwitchMode: icdStore.feedBoardETCStatusInfo.rfSwitchStatus.isLHCP,
      rfSwitchError: icdStore.feedBoardETCStatusInfo.rfSwitchError,
      fanPower: icdStore.feedBoardETCStatusInfo.fanPower,
      fanError: icdStore.feedBoardETCStatusInfo.fanError,
    },

    // Feed Ka Board Status
    feedKaBoardStatusInfo: {
      kaLnaRHCPError: icdStore.feedKaBoardStatusInfo.kaLnaRHCPError,
      kaLnaRHCPPower: icdStore.feedKaBoardStatusInfo.kaLnaRHCPPower,
      kaLnaLHCPError: icdStore.feedKaBoardStatusInfo.kaLnaLHCPError,
      kaLnaLHCPPower: icdStore.feedKaBoardStatusInfo.kaLnaLHCPPower,
      kaSelectionRHCPError: icdStore.feedKaBoardStatusInfo.kaSelectionRHCPError,
      kaSelectionRHCPBand: icdStore.feedKaBoardStatusInfo.kaSelectionRHCPBand,
      kaSelectionLHCPError: icdStore.feedKaBoardStatusInfo.kaSelectionLHCPError,
      kaSelectionLHCPBand: icdStore.feedKaBoardStatusInfo.kaSelectionLHCPBand,
    },
  }
} // ✅ 타이머 관리
let updateTimer: NodeJS.Timeout | null = null

onMounted(() => {
  console.log('📱 AllStatusContent 마운트됨')
  console.log('🔍 실행 환경:', isPopupWindow.value ? '팝업창' : '모달')

  // ✅ 피드 설정 로드 (초기화 보장)
  feedSettingsStore.loadSettings().then(() => {
    console.log('✅ 피드 설정 로드 완료:', enabledBands.value)
    console.log('📊 밴드 상태 확인:', {
      s: isSBandEnabled.value,
      x: isXBandEnabled.value,
      ka: isKaBandEnabled.value,
    })
  }).catch((error) => {
    console.error('⚠️ 피드 설정 로드 실패:', error)
  })

  // ✅ 초기 데이터 로드
  updateRealtimeData()

  // ✅ 실시간 업데이트 타이머 시작 (100ms 간격)
  updateTimer = setInterval(() => {
    updateRealtimeData()
  }, 100)

  console.log('🔄 실시간 업데이트 타이머 시작 (100ms 간격)')

  document.addEventListener('keydown', handleKeydown)

  if (isPopupWindow.value) {
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

  // ✅ 타이머 정리
  if (updateTimer) {
    clearInterval(updateTimer)
    updateTimer = null
    console.log('⏹️ 실시간 업데이트 타이머 중지')
  }

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
  background: var(--theme-scrollbar-track);
  border-radius: 4px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar-thumb {
  background: var(--theme-scrollbar-thumb);
  border-radius: 4px;
}

.q-card-section:nth-child(2)::-webkit-scrollbar-thumb:hover {
  background: var(--theme-scrollbar-thumb-hover);
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
  border: 1px solid var(--theme-border);
  border-radius: 3px;
  background-color: var(--theme-table-row-even);
  transition: all 0.2s ease;
  min-width: 0;
  overflow: hidden;
}

.status-item:hover {
  background-color: var(--theme-table-row-hover);
  transform: translateY(-1px);
  box-shadow: 0 2px 5px var(--theme-shadow);
}

.status-led {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 0 3px var(--theme-shadow);
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
  background-color: var(--theme-led-normal);
  box-shadow:
    0 0 5px var(--theme-led-normal),
    0 0 10px var(--theme-led-normal);
}

.led-on::before {
  background: radial-gradient(circle, rgba(76, 175, 80, 0.3) 0%, transparent 70%);
  animation: pulse-green 2s infinite;
}

.led-off {
  background-color: var(--theme-led-inactive);
  box-shadow: 0 0 3px var(--theme-shadow);
}

.led-error {
  background-color: var(--theme-led-error);
  box-shadow:
    0 0 5px var(--theme-led-error),
    0 0 10px var(--theme-led-error);
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
  color: var(--theme-positive);
}

.status-item:has(.led-off) .status-value {
  color: var(--theme-text-muted);
}

.status-item:has(.led-error) .status-value {
  color: var(--theme-negative);
}

.status-summary {
  border-top: 1px solid var(--theme-border);
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
  color: var(--theme-text-muted);
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
  color: var(--theme-text-muted);
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

/* 다크 모드 - CSS 변수가 자동으로 전환되므로 대부분 오버라이드 불필요 */
/* 필요한 경우만 추가 스타일 지정 */

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
