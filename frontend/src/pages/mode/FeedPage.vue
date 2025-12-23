<template>
  <div class="feed-mode">
    <div class="feed-container">
      <!-- 첫 번째 행: S-Band, X-Band, Ka-Band 가로 배치 -->
      <div class="row q-col-gutter-md feed-main-row" :class="getFeedRowClass()">
        <!-- S-Band 섹션 -->
        <div v-if="feedSettingsStore.isBandEnabled('s')" class="col-12" :class="getSBandColumnClass()">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-h6 text-primary q-mb-sm">S-Band</div>
              <div class="feed-path-wrapper">
                <!-- S-Band Rx Paths -->
                <div class="feed-path-section">
                  <!-- RHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">RHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentSBandLNARHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('s', 'rhcp'), { 'lna-disabled': isLoading }]" @click="!isLoading && toggleLNA('s', 'rhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('s', 'rhcp')"
                                :stroke="getLNAStrokeColor('s', 'rhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM RHCP</div>
                    </div>
                  </div>

                  <!-- LHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">LHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentSBandLNALHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('s', 'lhcp'), { 'lna-disabled': isLoading }]" @click="!isLoading && toggleLNA('s', 'lhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('s', 'lhcp')"
                                :stroke="getLNAStrokeColor('s', 'lhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM LHCP</div>
                    </div>
                  </div>
                </div>

                <!-- S-Band Tx Path -->
                <div class="feed-path-section rf-switch-section">
                  <div class="feed-path rf-switch-path">
                    <div class="rf-switch-wrapper">
                      <!-- 왼쪽: 입력 라벨 (Rx 경로와 동일한 구조) -->
                      <div class="path-label-group rf-switch-labels">
                        <div class="path-label">RHCP(Tx)</div>
                        <div class="path-label">LHCP(Tx)</div>
                      </div>
                      <!-- 오른쪽: 스위치와 출력 -->
                      <div class="path-content rf-switch-content">
                        <!-- 왼쪽 입력 화살표들 -->
                        <div class="rf-switch-inputs-container">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                        </div>
                        <!-- 중앙: 하나의 스위치 -->
                        <div class="rf-switch-container">
                          <div class="rf-switch-icon" :class="[getRFSwitchStatusClass(), { 'lna-disabled': isLoading }]" @click="!isLoading && toggleRFSwitch()">
                            <svg viewBox="0 0 24 24" width="80" height="80">
                              <rect x="2" y="2" width="20" height="20" rx="2" :fill="getRFSwitchFillColor()"
                                :stroke="getRFSwitchStrokeColor()" stroke-width="2" />
                              <!-- RHCP 경로 -->
                              <template v-if="icdStore.feedBoardETCStatusInfo.rfSwitchStatus.isRHCP">
                                <line x1="6" y1="7.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                              <!-- LHCP 경로 -->
                              <template v-else>
                                <line x1="6" y1="16.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                            </svg>
                          </div>
                        </div>
                        <!-- 오른쪽: 출력 -->
                        <div class="rf-switch-output-group">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="path-output-multiline">
                            <div>Tx (Selective)</div>
                            <div>RHCP or LHCP</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- X-Band 섹션 -->
        <div v-if="feedSettingsStore.isBandEnabled('x')" class="col-12" :class="getXBandColumnClass()">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-h6 text-primary q-mb-sm">X-Band</div>
              <div class="feed-path-wrapper">
                <!-- X-Band Rx Paths -->
                <div class="feed-path-section">
                  <!-- RHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">RHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentXBandLNARHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('x', 'rhcp'), { 'lna-disabled': isLoading }]" @click="!isLoading && toggleLNA('x', 'rhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('x', 'rhcp')"
                                :stroke="getLNAStrokeColor('x', 'rhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM RHCP</div>
                    </div>
                  </div>

                  <!-- LHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">LHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentXBandLNALHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('x', 'lhcp'), { 'lna-disabled': isLoading }]" @click="!isLoading && toggleLNA('x', 'lhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('x', 'lhcp')"
                                :stroke="getLNAStrokeColor('x', 'lhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM LHCP</div>
                    </div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Ka-Band 섹션 -->
        <div v-if="feedSettingsStore.isBandEnabled('ka')" class="col-12" :class="getKaBandColumnClass()">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-h6 text-primary q-mb-sm">Ka-Band</div>
              <div class="feed-path-wrapper">
                <!-- Ka-Band Rx Paths -->
                <div class="feed-path-section">
                  <!-- RHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">RHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentKaBandLNARHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('ka', 'rhcp'), { 'lna-disabled': isLoading }]"
                            @click="!isLoading && toggleLNA('ka', 'rhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('ka', 'rhcp')"
                                :stroke="getLNAStrokeColor('ka', 'rhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM RHCP</div>
                    </div>
                  </div>

                  <!-- LHCP(Rx) Path -->
                  <div class="feed-path">
                    <div class="path-label">LHCP(Rx)</div>
                    <div class="path-content">
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                        <div class="current-display-above">{{ formatCurrent(icdStore.currentKaBandLNALHCP) }} A</div>
                      </div>
                      <div class="lna-wrapper">
                        <div class="lna-label">LNA</div>
                        <div class="lna-container">
                          <div class="lna-icon" :class="[getLNAStatusClass('ka', 'lhcp'), { 'lna-disabled': isLoading }]"
                            @click="!isLoading && toggleLNA('ka', 'lhcp')">
                            <svg viewBox="0 0 24 24" width="60" height="60">
                              <polygon points="22,12 2,2 2,22" :fill="getLNAFillColor('ka', 'lhcp')"
                                :stroke="getLNAStrokeColor('ka', 'lhcp')" stroke-width="2" />
                            </svg>
                          </div>
                        </div>
                      </div>
                      <div class="arrow-container">
                        <div class="arrow-line"></div>
                      </div>
                      <div class="path-output">TM LHCP</div>
                    </div>
                  </div>
                </div>

                <!-- Ka-Band Selection RHCP Path (Band1/Band2 선택) -->
                <div class="feed-path-section rf-switch-section">
                  <div class="feed-path rf-switch-path">
                    <div class="rf-switch-wrapper">
                      <!-- 왼쪽: 입력 라벨 (S-Band와 동일한 구조) -->
                      <div class="path-label-group rf-switch-labels">
                        <div class="path-label">Band1</div>
                        <div class="path-label">Band2</div>
                      </div>
                      <!-- 오른쪽: 스위치와 출력 -->
                      <div class="path-content rf-switch-content">
                        <!-- 왼쪽 입력 화살표들 -->
                        <div class="rf-switch-inputs-container">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                        </div>
                        <!-- 중앙: 스위치 (Band1/Band2 선택) -->
                        <div class="rf-switch-container">
                          <div class="rf-switch-icon" :class="[getKaSelectionRHCPStatusClass(), { 'lna-disabled': isLoading }]"
                            @click="!isLoading && toggleKaSelectionRHCP()">
                            <svg viewBox="0 0 24 24" width="80" height="80">
                              <rect x="2" y="2" width="20" height="20" rx="2" :fill="getKaSelectionRHCPFillColor()"
                                :stroke="getKaSelectionRHCPStrokeColor()" stroke-width="2" />
                              <!-- Band1 경로 -->
                              <template v-if="icdStore.feedKaBoardStatusInfo.selectionStatus.rhcp.band === 'Band1'">
                                <line x1="6" y1="7.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                              <!-- Band2 경로 -->
                              <template v-else>
                                <line x1="6" y1="16.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                            </svg>
                          </div>
                        </div>
                        <!-- 오른쪽: 출력 -->
                        <div class="rf-switch-output-group">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="path-output-multiline">
                            <div>Selection RHCP</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Ka-Band Selection LHCP Path (Band1/Band2 선택) -->
                <div class="feed-path-section rf-switch-section">
                  <div class="feed-path rf-switch-path">
                    <div class="rf-switch-wrapper">
                      <!-- 왼쪽: 입력 라벨 (S-Band와 동일한 구조) -->
                      <div class="path-label-group rf-switch-labels">
                        <div class="path-label">Band1</div>
                        <div class="path-label">Band2</div>
                      </div>
                      <!-- 오른쪽: 스위치와 출력 -->
                      <div class="path-content rf-switch-content">
                        <!-- 왼쪽 입력 화살표들 -->
                        <div class="rf-switch-inputs-container">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                        </div>
                        <!-- 중앙: 스위치 (Band1/Band2 선택) -->
                        <div class="rf-switch-container">
                          <div class="rf-switch-icon" :class="[getKaSelectionLHCPStatusClass(), { 'lna-disabled': isLoading }]"
                            @click="!isLoading && toggleKaSelectionLHCP()">
                            <svg viewBox="0 0 24 24" width="80" height="80">
                              <rect x="2" y="2" width="20" height="20" rx="2" :fill="getKaSelectionLHCPFillColor()"
                                :stroke="getKaSelectionLHCPStrokeColor()" stroke-width="2" />
                              <!-- Band1 경로 -->
                              <template v-if="icdStore.feedKaBoardStatusInfo.selectionStatus.lhcp.band === 'Band1'">
                                <line x1="6" y1="7.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                              <!-- Band2 경로 -->
                              <template v-else>
                                <line x1="6" y1="16.5" x2="16.5" y2="12" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="18" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="18" cy="12" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                                <circle cx="6" cy="6" r="1.5" fill="none" :stroke="getRFSwitchLineColor()"
                                  stroke-width="1" />
                              </template>
                            </svg>
                          </div>
                        </div>
                        <!-- 오른쪽: 출력 -->
                        <div class="rf-switch-output-group">
                          <div class="arrow-container arrow-left rf-switch-arrow">
                            <div class="arrow-line"></div>
                          </div>
                          <div class="path-output-multiline">
                            <div>Selection LHCP</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- FAN 섹션 -->
        <div class="col-12" :class="getFanColumnClass()">
          <q-card class="control-section fan-section-card">
            <q-card-section>
              <div class="text-h6 text-primary q-mb-sm">FAN</div>
              <div class="fan-panel">
                <div class="fan-button-container">
                  <q-btn :class="getFanStatusClass()" class="fan-button" :color="getFanButtonColor()"
                    :outline="!icdStore.feedBoardETCStatusInfo.fanStatus.isActive && !icdStore.feedBoardETCStatusInfo.fanStatus.hasError"
                    :flat="false" :disable="isLoading" @click="toggleFan()">
                    <svg class="fan-icon q-mr-sm" viewBox="0 0 24 24" width="20" height="20">
                      <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="1.5" />
                      <path d="M12 12 L12 2 A10 10 0 0 1 19 7 L12 12 Z" fill="currentColor" opacity="0.6" />
                      <path d="M12 12 L20 19 A10 10 0 0 1 12 22 L12 12 Z" fill="currentColor" opacity="0.6" />
                      <path d="M12 12 L5 19 A10 10 0 0 1 4 7 L12 12 Z" fill="currentColor" opacity="0.6" />
                      <circle cx="12" cy="12" r="2.5" fill="currentColor" />
                    </svg>
                    <span class="fan-button-text">
                      FAN {{ icdStore.feedBoardETCStatusInfo.fanStatus.power }}
                    </span>
                  </q-btn>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Legend 섹션 -->
        <div v-if="getLegendColumnClass() !== 'col-md-0'" class="col-12" :class="getLegendColumnClass()">
          <q-card class="control-section">
            <q-card-section>
              <div class="text-h6 text-primary q-mb-xs">Legend</div>
              <div class="legend-grid">
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <polygon points="22,12 2,2 2,22" fill="#4caf50" stroke="#4caf50" stroke-width="1" />
                  </svg>
                  <span class="legend-text">LNA Power On</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <polygon points="22,12 2,2 2,22" fill="none" stroke="var(--theme-text-secondary)"
                      stroke-width="2" />
                  </svg>
                  <span class="legend-text">LNA Power Off</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <polygon points="22,12 2,2 2,22" fill="#f44336" stroke="#f44336" stroke-width="1" />
                  </svg>
                  <span class="legend-text">LNA Error</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="#4caf50" stroke="#4caf50" stroke-width="2" />
                    <!-- RHCP: 위 왼쪽 원(6,6)의 아래쪽(6, 7.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                    <line x1="6" y1="7.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                  </svg>
                  <span class="legend-text">RHCP Select</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="#2196f3" stroke="#2196f3" stroke-width="2" />
                    <!-- LHCP: 아래 왼쪽 원(6,18)의 위쪽(6, 16.5)에서 우측 가운데 원(18,12)의 왼쪽 중앙(16.5, 12)으로 연결 -->
                    <line x1="6" y1="16.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                  </svg>
                  <span class="legend-text">LHCP Select</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="#f44336" stroke="#f44336" stroke-width="2" />
                    <!-- Error: RHCP와 동일한 형태 -->
                    <line x1="6" y1="7.5" x2="16.5" y2="12" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="6" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="18" cy="12" r="1.5" fill="none" stroke="white" stroke-width="1" />
                    <circle cx="6" cy="18" r="1.5" fill="none" stroke="white" stroke-width="1" />
                  </svg>
                  <span class="legend-text">RF Switch Error</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="#4caf50" stroke="#4caf50" stroke-width="1" />
                  </svg>
                  <span class="legend-text">FAN ON</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="none" stroke="white" stroke-width="2" />
                  </svg>
                  <span class="legend-text">FAN OFF</span>
                </div>
                <div class="legend-item">
                  <svg viewBox="0 0 24 24" width="24" height="24" class="legend-icon">
                    <rect x="2" y="2" width="20" height="20" rx="2" fill="#f44336" stroke="#f44336" stroke-width="1" />
                  </svg>
                  <span class="legend-text">FAN Error</span>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- 상태 메시지 표시 -->
      <div class="status-message q-mt-md" v-if="showStatusMessage">
        <q-banner :class="statusSuccess ? 'bg-positive text-white' : 'bg-negative text-white'">
          {{ statusMessage }}
        </q-banner>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useICDStore } from '../../stores/icd/icdStore'
import { useNotification } from '@/composables/useNotification'
import { useFeedSettingsStore } from '@/stores/ui/feedSettingsStore'

// ICD 스토어 인스턴스 생성
const icdStore = useICDStore()

// 피드 설정 스토어 인스턴스 생성
const feedSettingsStore = useFeedSettingsStore()

// 알림 composable
const { success, error: notifyError } = useNotification()

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

/**
 * S-Band 컬럼 클래스를 반환합니다.
 * S-Band 너비는 유지하되, Legend 확대로 인해 조정됩니다.
 */
const getSBandColumnClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'col-md-6' // 1개: S-Band 6 (7 → 6으로 조정, Legend 확대 반영)
  if (count === 2) return 'col-md-4' // 2개: S-Band 4 (5 → 4로 조정, X-Band 확대 반영)
  return 'col-md-3'                   // 3개: S-Band 3 (유지)
}

/**
 * X-Band 컬럼 클래스를 반환합니다.
 * X-Band 너비를 20% 정도 넓힙니다.
 */
const getXBandColumnClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'col-md-7' // 1개: X-Band 7 (S-Band와 동일)
  if (count === 2) return 'col-md-4' // 2개: X-Band 4 (3 → 4로 20% 증가)
  return 'col-md-3'                   // 3개: X-Band 3 (2 → 3로 증가)
}

/**
 * Ka-Band 컬럼 클래스를 반환합니다.
 * Ka-Band 너비는 S-Band와 동일하게 유지합니다.
 */
const getKaBandColumnClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'col-md-7' // 1개: Ka-Band 7
  if (count === 2) return 'col-md-5' // 2개: Ka-Band 5
  return 'col-md-3'                   // 3개: Ka-Band 3
}


/**
 * FAN 섹션의 컬럼 클래스를 반환합니다.
 * FAN 너비를 조정합니다.
 */
const getFanColumnClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'col-md-1' // 1개: FAN 1 - S-Band 6, FAN 1, Legend 3 = 10 (여유 있음)
  if (count === 2) return 'col-md-1' // 2개: FAN 1 - S-Band 4, X-Band 4, FAN 1, Legend 3 = 12
  return 'col-md-1'                   // 3개: FAN 1 - S-Band 3, X-Band 3, Ka-Band 3, FAN 1, Legend 2 = 12
}

/**
 * Legend 섹션의 컬럼 클래스를 반환합니다.
 * Legend 너비를 조정합니다.
 */
const getLegendColumnClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'col-md-3' // 1개: Legend 3 - S-Band 6, FAN 1, Legend 3 = 10 (여유 있음)
  if (count === 2) return 'col-md-3' // 2개: Legend 3 - S-Band 4, X-Band 4, FAN 1, Legend 3 = 12
  return 'col-md-2'                   // 3개: Legend 2 - S-Band 3, X-Band 3, Ka-Band 3, FAN 1, Legend 2 = 12
}

/**
 * 피드 행의 클래스를 반환합니다 (CSS 최적화용)
 */
const getFeedRowClass = (): string => {
  const count = feedSettingsStore.enabledBandCount
  if (count === 1) return 'feed-row-single'
  if (count === 2) return 'feed-row-double'
  return 'feed-row-triple' // 3개
}

/**
 * 전류 값을 포맷팅합니다.
 */
const formatCurrent = (current: string): string => {
  if (!current || current === '') return '0.00'
  const num = parseFloat(current)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

/**
 * LNA 상태 클래스를 반환합니다.
 */
const getLNAStatusClass = (band: 's' | 'x' | 'ka', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo =
    band === 's'
      ? icdStore.feedSBoardStatusInfo
      : band === 'x'
        ? icdStore.feedXBoardStatusInfo
        : icdStore.feedKaBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return 'lna-error'
  if (lnaStatus.isActive) return 'lna-on'
  return 'lna-off'
}

/**
 * LNA 채움 색상을 반환합니다.
 */
const getLNAFillColor = (band: 's' | 'x' | 'ka', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo =
    band === 's'
      ? icdStore.feedSBoardStatusInfo
      : band === 'x'
        ? icdStore.feedXBoardStatusInfo
        : icdStore.feedKaBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  return 'none' // 채우기 없음 (OFF)
}

/**
 * LNA 윤곽 색상을 반환합니다.
 */
const getLNAStrokeColor = (band: 's' | 'x' | 'ka', type: 'lhcp' | 'rhcp'): string => {
  const statusInfo =
    band === 's'
      ? icdStore.feedSBoardStatusInfo
      : band === 'x'
        ? icdStore.feedXBoardStatusInfo
        : icdStore.feedKaBoardStatusInfo
  const lnaStatus = type === 'lhcp' ? statusInfo.lnaStatus.lhcp : statusInfo.lnaStatus.rhcp

  if (lnaStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (lnaStatus.isActive) return '#4caf50' // 녹색 (ON)
  // OFF일 때 화살표 색상과 동일하게 설정
  return getComputedStyle(document.documentElement).getPropertyValue('--theme-text-secondary').trim() || '#b0bec5'
}


/**
 * RF Switch 상태 클래스를 반환합니다.
 */
const getRFSwitchStatusClass = (): string => {
  const statusInfo = icdStore.feedBoardETCStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return 'rf-switch-error'
  if (statusInfo.rfSwitchStatus.isRHCP) return 'rf-switch-rhcp'
  return 'rf-switch-lhcp'
}

/**
 * RF Switch 채움 색상을 반환합니다.
 */
const getRFSwitchFillColor = (): string => {
  const statusInfo = icdStore.feedBoardETCStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (statusInfo.rfSwitchStatus.isRHCP) return '#4caf50' // 녹색 (RHCP)
  return '#2196f3' // 파란색 (LHCP)
}

/**
 * RF Switch 윤곽 색상을 반환합니다.
 */
const getRFSwitchStrokeColor = (): string => {
  const statusInfo = icdStore.feedBoardETCStatusInfo
  if (statusInfo.rfSwitchStatus.hasError) return '#f44336' // 빨간색 (Error)
  if (statusInfo.rfSwitchStatus.isRHCP) return '#4caf50' // 녹색 (RHCP)
  return '#2196f3' // 파란색 (LHCP)
}

/**
 * RF Switch 선 색상을 반환합니다.
 */
const getRFSwitchLineColor = (): string => {
  return 'white'
}

/**
 * Ka-Band Selection RHCP 상태 클래스를 반환합니다.
 * Band1 = 녹색, Band2 = 파란색
 */
const getKaSelectionRHCPStatusClass = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.rhcp.error) {
    return 'rf-switch-error'
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.rhcp.band === 'Band1') {
    return 'rf-switch-rhcp'
  }
  return 'rf-switch-lhcp'
}

/**
 * Ka-Band Selection RHCP 채움 색상을 반환합니다.
 */
const getKaSelectionRHCPFillColor = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.rhcp.error) {
    return '#f44336' // 빨간색 (Error)
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.rhcp.band === 'Band1') {
    return '#4caf50' // 녹색 (Band1)
  }
  return '#2196f3' // 파란색 (Band2)
}

/**
 * Ka-Band Selection RHCP 윤곽 색상을 반환합니다.
 */
const getKaSelectionRHCPStrokeColor = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.rhcp.error) {
    return '#f44336' // 빨간색 (Error)
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.rhcp.band === 'Band1') {
    return '#4caf50' // 녹색 (Band1)
  }
  return '#2196f3' // 파란색 (Band2)
}

/**
 * Ka-Band Selection LHCP 상태 클래스를 반환합니다.
 * Band1 = 녹색, Band2 = 파란색
 */
const getKaSelectionLHCPStatusClass = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.lhcp.error) {
    return 'rf-switch-error'
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.lhcp.band === 'Band1') {
    return 'rf-switch-rhcp'
  }
  return 'rf-switch-lhcp'
}

/**
 * Ka-Band Selection LHCP 채움 색상을 반환합니다.
 */
const getKaSelectionLHCPFillColor = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.lhcp.error) {
    return '#f44336' // 빨간색 (Error)
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.lhcp.band === 'Band1') {
    return '#4caf50' // 녹색 (Band1)
  }
  return '#2196f3' // 파란색 (Band2)
}

/**
 * Ka-Band Selection LHCP 윤곽 색상을 반환합니다.
 */
const getKaSelectionLHCPStrokeColor = (): string => {
  const statusInfo = icdStore.feedKaBoardStatusInfo
  if (statusInfo.selectionStatus.lhcp.error) {
    return '#f44336' // 빨간색 (Error)
  }
  // Band1 = 녹색, Band2 = 파란색
  if (statusInfo.selectionStatus.lhcp.band === 'Band1') {
    return '#4caf50' // 녹색 (Band1)
  }
  return '#2196f3' // 파란색 (Band2)
}

/**
 * Ka-Band Selection RHCP를 토글합니다.
 * Band1 <-> Band2 전환
 */
const toggleKaSelectionRHCP = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo
    const kaStatus = icdStore.feedKaBoardStatusInfo
    const etcStatus = icdStore.feedBoardETCStatusInfo

    // Ka-Band Selection RHCP만 토글 (Band1 <-> Band2)
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = etcStatus.rfSwitchStatus.isLHCP
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = etcStatus.fanPower
    const kaLHCP = kaStatus.kaLnaLHCPPower
    const kaRHCP = kaStatus.kaLnaRHCPPower
    const kaSelectionRHCP = kaStatus.selectionStatus.rhcp.band === 'Band1' // 토글 (현재가 Band1이면 Band2로)
    const kaSelectionLHCP = kaStatus.selectionStatus.lhcp.band === 'Band2'

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
      kaLHCP,
      kaRHCP,
      kaSelectionRHCP, // 토글된 값
      kaSelectionLHCP,
    )

    if (result.success) {
      success(`Ka-Band Selection RHCP 명령이 전송되었습니다. (${kaSelectionRHCP ? 'Band2' : 'Band1'})`)
      statusMessage.value = `Ka-Band Selection RHCP 명령이 성공적으로 전송되었습니다. (${kaSelectionRHCP ? 'Band2' : 'Band1'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('Ka-Band Selection RHCP 토글 중 오류:', error)
    notifyError('Ka-Band Selection RHCP 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'Ka-Band Selection RHCP 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * Ka-Band Selection LHCP를 토글합니다.
 * Band1 <-> Band2 전환
 */
const toggleKaSelectionLHCP = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo
    const kaStatus = icdStore.feedKaBoardStatusInfo
    const etcStatus = icdStore.feedBoardETCStatusInfo

    // Ka-Band Selection LHCP만 토글 (Band1 <-> Band2)
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = etcStatus.rfSwitchStatus.isLHCP
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = etcStatus.fanPower
    const kaLHCP = kaStatus.kaLnaLHCPPower
    const kaRHCP = kaStatus.kaLnaRHCPPower
    const kaSelectionRHCP = kaStatus.selectionStatus.rhcp.band === 'Band2'
    const kaSelectionLHCP = kaStatus.selectionStatus.lhcp.band === 'Band1' // 토글 (현재가 Band1이면 Band2로)

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
      kaLHCP,
      kaRHCP,
      kaSelectionRHCP,
      kaSelectionLHCP, // 토글된 값
    )

    if (result.success) {
      success(`Ka-Band Selection LHCP 명령이 전송되었습니다. (${kaSelectionLHCP ? 'Band2' : 'Band1'})`)
      statusMessage.value = `Ka-Band Selection LHCP 명령이 성공적으로 전송되었습니다. (${kaSelectionLHCP ? 'Band2' : 'Band1'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('Ka-Band Selection LHCP 토글 중 오류:', error)
    notifyError('Ka-Band Selection LHCP 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'Ka-Band Selection LHCP 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * FAN 상태 클래스를 반환합니다.
 */
const getFanStatusClass = (): string => {
  const statusInfo = icdStore.feedBoardETCStatusInfo
  if (statusInfo.fanStatus.hasError) return 'fan-error'
  if (statusInfo.fanStatus.isActive) return 'fan-on'
  return 'fan-off'
}

/**
 * FAN 버튼 색상을 반환합니다.
 */
const getFanButtonColor = (): string => {
  const statusInfo = icdStore.feedBoardETCStatusInfo
  if (statusInfo.fanStatus.hasError) return 'red'
  if (statusInfo.fanStatus.isActive) return 'green'
  return 'grey-7'
}

/**
 * LNA를 토글하고 즉시 명령을 전송합니다.
 */
const toggleLNA = async (band: 's' | 'x' | 'ka', type: 'lhcp' | 'rhcp') => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo
    const kaStatus = icdStore.feedKaBoardStatusInfo
    const etcStatus = icdStore.feedBoardETCStatusInfo

    // 클릭한 LNA의 현재 상태 확인 및 토글
    let sLHCP = sStatus.sLnaLHCPPower
    let sRHCP = sStatus.sLnaRHCPPower
    let xLHCP = xStatus.xLnaLHCPPower
    let xRHCP = xStatus.xLnaRHCPPower
    let kaLHCP = kaStatus.kaLnaLHCPPower
    let kaRHCP = kaStatus.kaLnaRHCPPower
    const sRFSwitch = etcStatus.rfSwitchStatus.isLHCP
    const fan = etcStatus.fanPower
    const kaSelectionRHCP = kaStatus.selectionStatus.rhcp.band === 'Band2'
    const kaSelectionLHCP = kaStatus.selectionStatus.lhcp.band === 'Band2'

    // 디버깅: 토글 전 현재 상태 출력
    console.log('=== 토글 전 현재 상태 ===')
    console.log(`클릭한 항목: ${band.toUpperCase()}-Band ${type.toUpperCase()}`)
    console.log('Store에서 읽은 상태:', {
      'S-LHCP': sLHCP,
      'S-RHCP': sRHCP,
      'X-LHCP': xLHCP,
      'X-RHCP': xRHCP,
      'Ka-LHCP': kaLHCP,
      'Ka-RHCP': kaRHCP,
    })

    // 클릭한 항목만 토글
    if (band === 's') {
      if (type === 'lhcp') {
        console.log(`S-LHCP 토글: ${sLHCP} → ${!sLHCP}`)
        sLHCP = !sLHCP
      } else {
        console.log(`S-RHCP 토글: ${sRHCP} → ${!sRHCP}`)
        sRHCP = !sRHCP
      }
    } else if (band === 'x') {
      if (type === 'lhcp') {
        console.log(`X-LHCP 토글: ${xLHCP} → ${!xLHCP}`)
        xLHCP = !xLHCP
      } else {
        console.log(`X-RHCP 토글: ${xRHCP} → ${!xRHCP}`)
        xRHCP = !xRHCP
      }
    } else if (band === 'ka') {
      // Ka-Band LNA 토글 지원
      if (type === 'lhcp') {
        console.log(`Ka-LHCP 토글: ${kaLHCP} → ${!kaLHCP}`)
        kaLHCP = !kaLHCP
      } else {
        console.log(`Ka-RHCP 토글: ${kaRHCP} → ${!kaRHCP}`)
        kaRHCP = !kaRHCP
      }
    }

    // 현재 상태를 콘솔에 출력 (디버깅용)
    console.log('=== Feed On/Off 명령 전송 ===')
    console.log(`토글 대상: ${band.toUpperCase()}-Band ${type.toUpperCase()}`)
    console.log('전송할 상태:', {
      'S-Band LHCP': sLHCP,
      'S-Band RHCP': sRHCP,
      'S-Band RF Switch': sRFSwitch,
      'X-Band LHCP': xLHCP,
      'X-Band RHCP': xRHCP,
      'FAN': fan,
      'Ka-Band LHCP': kaLHCP,
      'Ka-Band RHCP': kaRHCP,
      'Ka-Band Selection RHCP': kaSelectionRHCP,
      'Ka-Band Selection LHCP': kaSelectionLHCP,
    })

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
      kaLHCP,
      kaRHCP,
      kaSelectionRHCP,
      kaSelectionLHCP,
    )

    // 토글된 값 확인
    const toggledValue = band === 's'
      ? (type === 'lhcp' ? sLHCP : sRHCP)
      : band === 'x'
        ? (type === 'lhcp' ? xLHCP : xRHCP)
        : (type === 'lhcp' ? kaLHCP : kaRHCP)
    const toggledState = toggledValue ? 'ON' : 'OFF'

    if (result.success) {
      success(`${band.toUpperCase()}-Band ${type.toUpperCase()} LNA ${toggledState} 명령이 전송되었습니다.`)
      statusMessage.value = `${band.toUpperCase()}-Band ${type.toUpperCase()} LNA ${toggledState} 명령이 성공적으로 전송되었습니다.`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('LNA 토글 중 오류:', error)
    notifyError('LNA 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'LNA 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * RF Switch를 토글하고 즉시 명령을 전송합니다.
 */
const toggleRFSwitch = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo
    const kaStatus = icdStore.feedKaBoardStatusInfo
    const etcStatus = icdStore.feedBoardETCStatusInfo

    // RF Switch만 토글
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = !etcStatus.rfSwitchStatus.isLHCP // 토글 (현재가 LHCP면 RHCP로)
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = etcStatus.fanPower
    const kaLHCP = kaStatus.kaLnaLHCPPower
    const kaRHCP = kaStatus.kaLnaRHCPPower
    const kaSelectionRHCP = kaStatus.selectionStatus.rhcp.band === 'Band2'
    const kaSelectionLHCP = kaStatus.selectionStatus.lhcp.band === 'Band2'

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
      kaLHCP,
      kaRHCP,
      kaSelectionRHCP,
      kaSelectionLHCP,
    )

    if (result.success) {
      success(`RF Switch 명령이 전송되었습니다. (${sRFSwitch ? 'LHCP' : 'RHCP'})`)
      statusMessage.value = `RF Switch 명령이 성공적으로 전송되었습니다. (${sRFSwitch ? 'LHCP' : 'RHCP'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('RF Switch 토글 중 오류:', error)
    notifyError('RF Switch 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'RF Switch 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

/**
 * FAN을 토글하고 즉시 명령을 전송합니다.
 */
const toggleFan = async () => {
  try {
    isLoading.value = true

    // 현재 상태 읽기
    const sStatus = icdStore.feedSBoardStatusInfo
    const xStatus = icdStore.feedXBoardStatusInfo
    const kaStatus = icdStore.feedKaBoardStatusInfo
    const etcStatus = icdStore.feedBoardETCStatusInfo

    // FAN만 토글
    const sLHCP = sStatus.sLnaLHCPPower
    const sRHCP = sStatus.sLnaRHCPPower
    const sRFSwitch = etcStatus.rfSwitchStatus.isLHCP
    const xLHCP = xStatus.xLnaLHCPPower
    const xRHCP = xStatus.xLnaRHCPPower
    const fan = !etcStatus.fanPower // 토글
    const kaLHCP = kaStatus.kaLnaLHCPPower
    const kaRHCP = kaStatus.kaLnaRHCPPower
    const kaSelectionRHCP = kaStatus.selectionStatus.rhcp.band === 'Band2'
    const kaSelectionLHCP = kaStatus.selectionStatus.lhcp.band === 'Band2'

    // 즉시 명령 전송
    const result = await icdStore.sendFeedOnOffCommand(
      sLHCP,
      sRHCP,
      sRFSwitch,
      xLHCP,
      xRHCP,
      fan,
      kaLHCP,
      kaRHCP,
      kaSelectionRHCP,
      kaSelectionLHCP,
    )

    if (result.success) {
      success(`FAN 명령이 전송되었습니다. (${fan ? 'ON' : 'OFF'})`)
      statusMessage.value = `FAN 명령이 성공적으로 전송되었습니다. (${fan ? 'ON' : 'OFF'})`
      statusSuccess.value = true
    } else {
      notifyError(result.message || '명령 전송에 실패했습니다.')
      statusMessage.value = result.message || '명령 전송에 실패했습니다.'
      statusSuccess.value = false
    }
    statusTimestamp.value = Date.now()
  } catch (error) {
    console.error('FAN 토글 중 오류:', error)
    notifyError('FAN 토글 중 오류가 발생했습니다.')
    statusMessage.value = 'FAN 토글 중 오류가 발생했습니다.'
    statusSuccess.value = false
    statusTimestamp.value = Date.now()
  } finally {
    isLoading.value = false
  }
}

</script>

<style scoped>
/* ✅ router-view, q-page-container 내부의 feed-mode 하단 여백 제거 - 다른 페이지들과 동일 */
/* ✅ mode-common.scss의 높이 제한을 따르도록 설정 (500px - 34px - 16px = 450px) */
router-view .feed-mode,
q-page-container .feed-mode,
q-page .feed-mode,
.feed-mode,
[class*="feed-mode"],
div.feed-mode {
  /* ✅ mode-common.scss의 높이 제한을 따르도록 height: auto 제거 */
  width: 100%;
  padding: 0 !important;
  margin: 0 !important;
  margin-bottom: 0 !important;
  /* ✅ 하단 마진 제거 */
  padding-bottom: 0 !important;
  /* ✅ 하단 패딩 제거 */
  /* ✅ mode-common.scss에서 높이 제한 설정 (450px) */
  overflow: visible !important;
  /* ✅ 테두리가 보이도록 overflow: visible로 변경 */
  display: flex !important;
  /* ✅ flexbox로 변경 */
  flex-direction: column !important;
  /* ✅ 세로 방향 */
  justify-content: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  align-items: center;
  /* ✅ 컨텐츠를 가운데 정렬 */
  gap: 0 !important;
  /* ✅ flex gap 제거 */
  row-gap: 0 !important;
  /* ✅ flex row-gap 제거 */
  column-gap: 0 !important;
  /* ✅ flex column-gap 제거 */
}

/* router-view, q-page-container의 하단 패딩/마진 제거 */
router-view .feed-mode,
q-page-container .feed-mode {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* ✅ feed-mode 하단 여백 제거 - SunTrackPage와 동일한 구조 */

.feed-container {
  padding: 0.5rem 0.5rem 0.5rem 0.5rem !important;
  /* ✅ 좌우 패딩 더 감소 (0.75rem → 0.5rem) - S-Band를 좌측으로 더 붙임 */
  max-width: 100% !important;
  /* ✅ 최대 너비 제한 제거 - 전체 화면 너비 사용 */
  width: 100% !important;
  margin: 0 auto;
  margin-bottom: 0 !important;
  padding-bottom: 0.5rem !important;
  /* ✅ 한 화면에 나오도록 패딩 최소화 */
  box-sizing: border-box !important;
}

/* ✅ row stretch - S-Band, X-Band+FAN, Legend 섹션이 동일한 높이를 가지도록 */
.feed-container .row {
  display: flex !important;
  flex-wrap: wrap !important;
  align-items: stretch !important;
  /* ✅ flex container로 명시적으로 설정하고 stretch로 동일한 높이 유지 */
}

/* ✅ col 높이를 100%로 설정하여 모든 섹션이 동일한 높이를 가지도록 */
.feed-container .row>[class*="col-"] {
  display: flex !important;
  flex-direction: column !important;
  align-items: stretch !important;
  /* ✅ col의 높이는 row의 stretch로 자동 결정됨 */
}

/* ✅ q-col-gutter-md가 추가하는 padding을 고려하여 높이 조정 */
.feed-container .row.q-col-gutter-md>[class*="col-"] {
  display: flex !important;
  flex-direction: column !important;
  align-items: stretch !important;
  /* ✅ gutter padding이 있어도 높이는 stretch로 자동 결정됨 */
}

/* ✅ 밴드 개수에 따른 반응형 레이아웃 */
/* 1개 밴드: 전체 너비 */
.feed-container .feed-main-row.feed-row-single .q-card {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  min-height: 0 !important;
  align-self: stretch !important;
}

/* 2개 밴드: 각각 넓게 (50%) */
.feed-container .feed-main-row.feed-row-double .q-card {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  min-height: 0 !important;
  align-self: stretch !important;
}

/* 3개 밴드: 가로 배치, 내부 컨텐츠 축소 */
.feed-container .feed-main-row.feed-row-triple .q-card {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  min-height: 0 !important;
  align-self: stretch !important;
}

/* ✅ 3개 밴드 선택 시 내부 컨텐츠 축소 */
.feed-container .feed-main-row.feed-row-triple .feed-path {
  padding: 0.75rem 0.75rem !important;
  height: 75px !important;
  min-height: 75px !important;
  max-height: 75px !important;
}

.feed-container .feed-main-row.feed-row-triple .feed-path-wrapper {
  padding: 0.375rem 0.75rem !important;
  gap: 0.15rem !important;
}

.feed-container .feed-main-row.feed-row-triple .lna-icon svg {
  width: 50px !important;
  height: 50px !important;
}

.feed-container .feed-main-row.feed-row-triple .rf-switch-icon svg {
  width: 70px !important;
  height: 70px !important;
}

.feed-container .feed-main-row.feed-row-triple .path-label {
  min-width: 50px !important;
  width: 50px !important;
  max-width: 50px !important;
  font-size: 0.85rem !important;
}

.feed-container .feed-main-row.feed-row-triple .arrow-container {
  min-width: 60px !important;
  width: 60px !important;
  max-width: 60px !important;
}

.feed-container .feed-main-row.feed-row-triple .lna-container {
  min-width: 50px !important;
  width: 50px !important;
  max-width: 50px !important;
}

.feed-container .feed-main-row.feed-row-triple .current-display-above {
  font-size: 0.75rem !important;
  padding: 0.2rem 0.4rem !important;
  min-width: 50px !important;
}

.feed-container .feed-main-row.feed-row-triple .path-output {
  font-size: 0.85rem !important;
  min-width: 70px !important;
  /* ✅ 최소 너비 증가 */
}

.feed-container .feed-main-row.feed-row-triple .rf-switch-path {
  padding: 0.75rem 0.75rem !important;
  height: 75px !important;
  min-height: 75px !important;
  max-height: 75px !important;
}

.feed-container .feed-main-row.feed-row-triple .rf-switch-labels {
  min-width: 60px !important;
  width: 60px !important;
}

.feed-container .feed-main-row.feed-row-triple .rf-switch-inputs-container {
  min-width: 60px !important;
  width: 60px !important;
}

.feed-container .feed-main-row.feed-row-triple .path-output-multiline {
  min-width: 100px !important;
  font-size: 0.8rem !important;
}

/* ✅ 두 번째 행 (FAN, Legend) - 높이 최소화 */
.feed-container .feed-second-row {
  margin-top: 0.5rem !important;
}

.feed-container .feed-second-row .fan-section-card {
  height: auto !important;
  min-height: auto !important;
}

/* ✅ col 내부의 q-card도 높이를 100%로 설정 - S-Band 높이 기준 */
.feed-container .row>[class*="col-"]:first-child .q-card,
.feed-container .row>[class*="col-"]:first-child .q-card.control-section {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  min-height: 0 !important;
  align-self: stretch !important;
  /* ✅ S-Band 높이 기준 */
}

/* ✅ Legend 카드도 S-Band 높이에 맞춤 */
.feed-container .row>[class*="col-md-2"] .q-card {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  min-height: 0 !important;
  align-self: stretch !important;
}

/* ✅ q-card-section도 높이를 100%로 설정 */
.feed-container .row>[class*="col-"] .q-card .q-card-section,
.feed-container .row>[class*="col-"] .q-card .q-card__section {
  flex: 1 1 auto !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-start !important;
  align-items: stretch !important;
  min-height: auto !important;
  /* ✅ min-height: auto로 설정하여 flex 컨테이너가 내용에 맞게 크기를 조정하도록 */
  /* ✅ flex: 1 1 auto로 남은 공간을 채우도록 설정 */
  /* ✅ justify-content: center로 행들을 가운데 정렬 */
  /* ✅ align-items: stretch로 자식 요소들이 전체 너비를 사용하도록 */
}

/* ✅ LEGEND 섹션은 상단 정렬로 오버라이드 */
.feed-container .row>[class*="col-"] .control-section:has(.legend-grid) .q-card-section,
.feed-container .row>[class*="col-"] .control-section:has(.legend-grid) .q-card__section {
  justify-content: flex-start !important;
  /* ✅ Legend를 X-BAND 라벨과 동일한 선상에 위치 (상단 정렬) */
}

/* ✅ 마지막 row 하단 여백 제거 */
.feed-container .row:last-child {
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

.control-section {
  display: flex !important;
  flex-direction: column !important;
  flex: 1 1 auto !important;
  justify-content: center !important;
  min-height: 0 !important;
  /* ✅ flex: 1 1 auto로 남은 공간을 채우고, min-height: 0으로 축소 가능하도록 설정 */
  /* ✅ justify-content: center로 행들을 가운데 정렬 */
  background-color: var(--theme-card-background);
  /* ✅ border, border-radius, box-shadow는 mode-common.scss에서 통일 관리 */
  /* ✅ col의 높이에 맞춰 늘어나도록 flex 설정 */
}

/* q-card-section의 패딩 조정 */
.control-section :deep(.q-card-section) {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 0.75rem !important;
  /* ✅ 좌측 패딩 감소 - 더 넓게 사용 */
  padding-right: 0.75rem !important;
  /* ✅ 우측 패딩 감소 - 더 넓게 사용 */
  /* ✅ 한 화면에 나오도록 패딩 감소 */
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-start !important;
}

/* S-Band와 X-Band의 행을 수평으로 정렬하기 위한 스타일 */
.feed-container .row>[class*="col-"] .control-section :deep(.q-card-section) {
  padding-top: 0.5rem;
  padding-left: 0.75rem !important;
  /* ✅ 좌측 패딩 감소 - 더 넓게 사용 */
  padding-right: 0.75rem !important;
  /* ✅ 우측 패딩 감소 - 더 넓게 사용 */
  /* ✅ 한 화면에 나오도록 패딩 감소 */
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-start !important;
  /* ✅ 행들을 가운데 정렬 */
}

/* ✅ feed-path-section 설정 */
.feed-container .row>[class*="col-"] .feed-path-section {
  flex-shrink: 0;
  /* ✅ flex item이 축소되지 않도록 설정 */
  margin-bottom: 0 !important;
  /* ✅ 섹션들 사이 간격은 feed-path-wrapper의 gap으로 제어 */
}

/* Legend 섹션의 상단 패딩 조정 - X-Band RHCP 테두리 상단에 맞추기 */
.control-section:has(.legend-grid) {
  flex: 1 1 auto !important;
  min-height: 0 !important;
  /* ✅ Legend 섹션이 S-Band와 동일한 높이를 유지하도록 flex 설정 */
}

.control-section:has(.legend-grid) :deep(.q-card-section) {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  /* ✅ 한 화면에 나오도록 패딩 감소 */
  padding-left: 1.25rem !important;
  /* ✅ 좌측 패딩 증가 (0.75rem → 1.25rem) - 내부 컨텐츠 넓이보다 여유 있게 */
  padding-right: 1.25rem !important;
  /* ✅ 우측 패딩 증가 (calc(1.05rem + 0.3125rem) → 1.25rem) - 내부 컨텐츠 넓이보다 여유 있게 */
  display: flex;
  flex-direction: column;
  justify-content: flex-start !important;
  /* ✅ Legend를 X-BAND 라벨과 동일한 선상에 위치 (상단 정렬) - !important로 오버라이드 */
  flex: 1 1 auto !important;
  min-height: 0 !important;
  /* ✅ Legend 섹션이 S-Band와 동일한 높이를 유지하도록 flex 설정 */
}

/* ✅ feed-path-section을 flex container로 변경하여 gap으로 간격 제어 */
.feed-path-section {
  display: flex !important;
  flex-direction: column !important;
  row-gap: 0.2rem !important;
  column-gap: 0 !important;
  gap: 0.2rem !important;
  /* ✅ 한 화면에 나오도록 간격 감소: 0.25rem → 0.2rem */
  margin-bottom: 0 !important;
  /* ✅ 섹션들 사이 간격은 feed-path-wrapper의 gap으로 제어 */
}

/* ✅ 더 구체적인 선택자로 강제 적용 */
.control-section .q-card-section .feed-path-wrapper .feed-path-section {
  display: flex !important;
  flex-direction: column !important;
  row-gap: 0.2rem !important;
  gap: 0.2rem !important;
  margin-bottom: 0 !important;
  /* ✅ 한 화면에 나오도록 간격 감소 */
}

/* S-Band 스위치 섹션과 X-Band FAN 섹션을 같은 선상에 배치 */
.rf-switch-section {
  margin-top: 0;
  margin-bottom: 0 !important;
  /* ✅ RF Switch 섹션 간격은 feed-path-wrapper의 gap으로 제어 */
}

/* rf-switch-section이 feed-path-section 클래스도 가지고 있어서 margin-bottom이 적용되도록 수정 */
.rf-switch-section.feed-path-section {
  margin-bottom: 0 !important;
  /* ✅ RF Switch 섹션 간격은 feed-path-wrapper의 gap으로 제어 */
}

.fan-section {
  margin-top: 0;
  margin-bottom: 0;
}

@media (max-width: 1200px) {
  .rf-switch-wrapper {
    width: 100%;
    flex-wrap: wrap;
    justify-content: center;
    text-align: center;
  }

  .rf-switch-wrapper>* {
    justify-content: center;
  }
}

.feed-path {
  display: flex;
  align-items: center !important;
  justify-content: center !important;
  gap: 0.5rem;
  margin-bottom: 0 !important;
  /* ✅ margin-bottom 제거 - feed-path-section의 gap으로 제어 */
  /* ✅ 복잡한 패딩 계산 대신 단순하고 명확한 패딩 사용 */
  /* ✅ 상하 패딩을 동일하게 설정하고 flex 정렬에 의존 */
  padding: 1.5rem 1rem !important;
  /* ✅ 좌우 패딩 조정 (1.5rem → 1rem) - 더 넓게 사용하면서도 라벨이 들어오도록 */
  height: 115px !important;
  min-height: 115px !important;
  max-height: 115px !important;
  box-sizing: border-box !important;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  /* 전체 경로가 하나의 배경색으로 통일되도록 */
  position: relative;
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  width: 100%;
  /* ✅ 내부 컨텐츠를 완전히 가운데 정렬하기 위한 추가 설정 */
  /* ✅ 좌우 균형을 맞추기 위해 내부 요소들의 flex 속성 조정 */
  /* ✅ 수평 정렬을 위해 내부 요소들을 감싸는 wrapper처럼 동작하도록 설정 */
  flex-wrap: nowrap !important;
  /* ✅ 줄바꿈 방지 */
  align-content: center;
}

/* ✅ Rx 경로와 Tx 경로(스위치)의 가운데 정렬을 위한 추가 스타일 */
.feed-path-section:has(.rf-switch-path) {
  display: flex !important;
  justify-content: center !important;
  /* ✅ 스위치 경로를 가운데 정렬 */
}

.feed-path-section:has(.feed-path:not(.rf-switch-path)) {
  display: flex !important;
  justify-content: center !important;
  /* ✅ LNA 경로를 가운데 정렬 */
}

/* ✅ feed-path-section 내부의 feed-path들 사이 간격은 feed-path-section의 gap으로 제어됨 */

.feed-path-wrapper {
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.5rem 1rem !important;
  /* ✅ 좌우 패딩 조정 (1.5rem → 1rem) - 더 넓게 사용하면서도 라벨이 들어오도록 */
  display: flex !important;
  flex-direction: column !important;
  row-gap: 0.2rem !important;
  column-gap: 0 !important;
  gap: 0.2rem !important;
  /* ✅ 한 화면에 나오도록 패딩과 간격 감소 */
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  width: 100% !important;
  /* ✅ 전체 너비 사용 */
  box-sizing: border-box !important;
}

/* ✅ feed-path-wrapper 내부 간격 명시적으로 설정 */
.feed-container .row>[class*="col-"] .feed-path-wrapper {
  display: flex !important;
  flex-direction: column !important;
  row-gap: 0.25rem !important;
  gap: 0.25rem !important;
  /* ✅ 한 화면에 나오도록 간격 감소: 0.375rem → 0.25rem */
}

/* ✅ 더 구체적인 선택자로 강제 적용 */
.control-section .q-card-section .feed-path-wrapper {
  display: flex !important;
  flex-direction: column !important;
  row-gap: 0.375rem !important;
  gap: 0.375rem !important;
}

.feed-path-wrapper .feed-path,
.feed-path-wrapper .rf-switch-path {
  background-color: transparent;
  border: none;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
  margin-bottom: 0 !important;
  /* ✅ feed-path들 사이 간격은 feed-path-section 내부에서 제어 */
}

.feed-path-wrapper .fan-section {
  background-color: transparent;
  border: none;
  padding: 1.5rem 0.5rem;
  margin: 0;
  height: 115px !important;
  min-height: 115px !important;
  max-height: 115px !important;
  box-sizing: border-box !important;
}

.feed-path-wrapper .fan-button-container {
  margin: 0;
}

.fan-panel {
  background-color: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  /* ✅ FAN 섹션 내부에서 중앙 정렬 */
}

/* ✅ FAN 섹션 카드 스타일 */
.fan-section-card {
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  min-height: 0 !important;
  height: auto !important;
  /* ✅ 높이를 자동으로 설정하여 내용에 맞게 조정 */
  max-height: 120px !important;
  /* ✅ 최대 높이 증가 (100px → 120px) - 버튼과 간섭 없도록 */
}

/* ✅ FAN 섹션 테두리 높이 증가 - 버튼과 간섭 없도록 */
.fan-section-card .q-card-section {
  padding: 1rem 0.5rem !important;
  /* ✅ 상하 패딩 증가 (0.75rem → 1rem) - 버튼과 간섭 없도록 */
  /* ✅ 좌우 패딩 유지 (0.5rem) */
  min-height: auto !important;
  height: auto !important;
  max-height: 120px !important;
  /* ✅ 최대 높이 증가 (100px → 120px) - 버튼과 간섭 없도록 */
}

.rf-switch-path {
  /* ✅ Rx 경로의 feed-path와 동일한 패딩 및 높이 설정 */
  padding: 1.5rem 1rem !important;
  /* ✅ Rx 경로의 feed-path와 동일한 패딩 (1.5rem 1rem) */
  height: 115px !important;
  min-height: 115px !important;
  max-height: 115px !important;
  /* ✅ Rx 경로의 feed-path와 동일한 높이 (115px) */
  box-sizing: border-box !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  /* ✅ 스위치 행을 상하좌우 가운데 정렬 */
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  width: 100% !important;
  /* ✅ 전체 너비 사용 */
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  /* ✅ Rx 경로의 feed-path와 동일한 배경 및 테두리 */
}


.path-label {
  min-width: 120px !important;
  /* ✅ 최소 너비 더 증가 (100px → 120px) - RHCP/LHCP/Band1/Band2 라벨이 겹치지 않도록 */
  width: 120px !important;
  max-width: 120px !important;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: right;
  /* 화살표와 평행하게 배치 - path-content와 같은 높이로 맞춤 */
  display: flex;
  align-items: center !important;
  justify-content: flex-end;
  /* ✅ 고정 높이 제거, min-height만 사용하여 유연성 확보 */
  /* ✅ 하지만 align-items: center가 제대로 작동하도록 height도 설정 */
  min-height: 60px;
  height: 60px;
  padding-top: 0;
  padding-right: 1.25rem !important;
  /* ✅ 오른쪽 패딩 더 증가 (1rem → 1.25rem) - 라벨이 겹치지 않도록 */
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 0;
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
  /* ✅ 좌우 균형을 맞추기 위해 margin 제거 */
  margin: 0;
  /* ✅ 수평 정렬을 위해 flex 속성 조정 */
  flex-grow: 0;
  flex-basis: auto;
  /* ✅ 수직 정렬을 강제하기 위해 추가 설정 */
  align-self: center;
  white-space: nowrap !important;
  /* ✅ 라벨이 줄바꿈되지 않도록 */
}

.path-label-group {
  min-width: 80px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 500;
  color: var(--theme-text);
  text-align: right;
  /* ✅ 좌우 균형을 맞추기 위해 margin 초기화 */
  margin: 0;
}

.path-content {
  display: flex;
  align-items: center !important;
  justify-content: center;
  gap: 0;
  /* flex: 1을 제거하여 내용에 맞는 너비만 사용 */
  position: relative;
  /* ✅ LNA와 스위치가 같은 높이에 정렬되도록 높이를 80px로 변경 */
  min-height: 80px;
  height: 80px;
  /* feed-path의 배경색과 통일되도록 배경색 제거 */
  background-color: transparent;
  /* 첫 번째 요소(화살표) 앞의 간격을 feed-path의 gap과 동일하게 */
  margin: 0;
  /* ✅ 좌우 균형을 맞추기 위해 margin 초기화 */
  /* 반응형: 작은 화면에서도 요소들이 함께 줄어들도록 */
  min-width: 0;
  /* 내부 요소들이 가운데 정렬되도록 */
  flex-shrink: 1;
  /* ✅ flex item이 축소 가능하도록 */
  width: auto;
  max-width: 100%;
  /* ✅ 수직 정렬을 강제하기 위해 추가 설정 */
  align-self: center;
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
}

/* 화살표와 출력 라벨 사이 간격 추가 - Tx (Selective)의 gap: 0.5rem과 동일하게 */
.path-content>.arrow-container:last-of-type {
  margin-right: 0;
  /* 간격은 출력 라벨의 margin-left로 처리 */
  flex-shrink: 0;
}

/* 마지막 화살표의 화살표 끝 부분 위치 조정 - 출력 라벨과의 간격을 명확히 */
.path-content>.arrow-container:last-of-type .arrow-line::after {
  right: 0;
  /* 화살표 끝이 출력 라벨과 너무 가까워 보이지 않도록 */
}

.arrow-container {
  position: relative;
  display: flex;
  align-items: center !important;
  /* ✅ 세로 정렬 강제 */
  justify-content: center;
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  min-width: 80px;
  width: 80px;
  max-width: 80px;
  /* ✅ 고정 높이 제거, min-height만 사용하여 유연성 확보 */
  /* ✅ 하지만 align-items: center가 제대로 작동하도록 height도 설정 */
  min-height: 60px;
  height: 60px;
  /* 삼각형과 화살표를 붙이기 위한 마진 조정 */
  margin: 0;
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 1;
}

.arrow-line {
  width: 100%;
  height: 2px;
  background-color: var(--theme-text-secondary);
  position: relative;
  /* 화살표를 삼각형과 붙이기 위한 위치 조정 */
  margin: 0;
}

.arrow-line::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  /* 화살표 화살 부분 - 선의 끝과 약간 겹쳐서 자연스럽게 연결 */
  border-left: 12px solid var(--theme-text-secondary);
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  /* 화살표 끝이 선의 끝과 자연스럽게 연결되도록 - 삼각형과 겹치지 않도록 */
}

.current-display-above {
  position: absolute;
  /* feed-path의 상단 패딩 내부에 위치하도록 조정 - 전류 표시를 화살표에서 더 멀리 */
  top: -0.5rem;
  left: 50%;
  transform: translateX(-50%);
  min-width: 60px;
  padding: 0.25rem 0.5rem;
  /* 배경색 제거 - feed-path의 배경색과 통일 */
  background-color: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  text-align: center;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--theme-text-secondary);
  white-space: nowrap;
  z-index: 2;
}

.lna-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  /* ✅ 중앙 정렬로 변경 - RF Switch와 동일한 수직 정렬 */
  /* ✅ 고정 높이 유지 */
  height: 60px;
  /* ✅ 마진 제거 */
  margin: 0;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  flex-shrink: 0;
  min-width: 0;
}

.lna-label {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--theme-text-secondary);
  text-align: center;
  white-space: nowrap;
  z-index: 1;
  pointer-events: none;
}

.lna-container {
  display: flex;
  align-items: center !important;
  /* ✅ 세로 정렬 강제 */
  justify-content: center;
  /* ✅ 고정 높이 제거, min-height만 사용하여 유연성 확보 */
  /* ✅ 하지만 align-items: center가 제대로 작동하도록 height도 설정 */
  min-height: 60px;
  height: 60px;
  /* 화살표와 정확히 정렬하기 위한 미세 조정 */
  position: relative !important;
  /* LNA 라벨의 기준점 - !important로 우선순위 확보 */
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
  /* flex 컨테이너이지만 absolute 자식 요소를 위한 설정 */
  flex-wrap: nowrap;
}

.rf-switch-wrapper {
  display: flex;
  align-items: center !important;
  /* ✅ 중앙 정렬로 변경 - Rx 경로의 feed-path와 동일하게 */
  gap: 0.5rem;
  /* ✅ Rx 경로의 feed-path와 동일한 gap (0.5rem) */
  padding: 0;
  border-radius: 0;
  background-color: transparent;
  border: none;
  min-height: 80px !important;
  /* ✅ 모든 요소가 80px이므로 최소 높이도 80px */
  height: 80px !important;
  /* ✅ 고정 높이로 변경 - 라벨과 화살표 컨테이너가 정확히 정렬되도록 */
  justify-content: center !important;
  /* ✅ 중앙 정렬로 변경 - Rx 경로의 feed-path와 동일하게 */
  flex-wrap: nowrap !important;
  /* ✅ 줄바꿈 방지 */
  margin: 0 !important;
  /* ✅ margin 초기화 - Rx 경로의 feed-path와 동일하게 */
  width: 100% !important;
  /* ✅ 전체 너비 사용 - Rx 경로의 feed-path와 동일하게 */
  max-width: 100% !important;
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  text-align: left;
  /* ✅ 좌측 정렬로 변경 */
}

.rf-switch-wrapper>* {
  /* ✅ flex-shrink 제거 - 각 요소가 자체 flex-shrink 속성을 사용하도록 */
}

.rf-switch-labels {
  display: flex;
  flex-direction: column;
  justify-content: center !important;
  /* ✅ 중앙 정렬로 변경 - 각 라벨이 화살표와 같은 선상에 정렬되도록 */
  gap: 0;
  /* ✅ gap 제거 - 각 라벨이 화살표와 정확히 정렬되도록 */
  min-width: 120px !important;
  /* ✅ 최소 너비 더 증가 (100px → 120px) - RHCP/LHCP/Band1/Band2 라벨이 겹치지 않도록 */
  width: 120px !important;
  /* Rx 경로의 path-label과 동일한 너비 */
  height: 80px;
  /* ✅ 스위치 아이콘과 동일한 높이로 맞춤 */
  /* 라벨 컨테이너 높이를 스위치 아이콘과 동일하게 */
  /* RHCP(Tx), LHCP(Tx) 라벨 사이 간격을 더 좁게 조정 */
  padding-right: 0 !important;
  /* ✅ 패딩 제거 - Rx 경로의 path-label과 동일하게 */
  align-items: flex-end !important;
  /* ✅ flex-end로 변경 - Rx 경로의 path-label과 동일하게 우측 정렬 */
  text-align: right !important;
  /* ✅ Rx 경로의 path-label-group과 동일하게 우측 정렬 */
  flex-shrink: 0 !important;
  /* ✅ Rx 경로의 path-label과 동일하게 flex-shrink: 0 */
  flex-grow: 0 !important;
  /* ✅ Rx 경로의 path-label과 동일하게 flex-grow: 0 */
  flex-basis: auto !important;
  /* ✅ Rx 경로의 path-label과 동일하게 flex-basis: auto */
}

/* Tx 부분의 path-label 스타일 조정 - 화살표와 같은 선상에 정렬 */
.rf-switch-labels .path-label {
  height: 40px;
  min-height: 40px;
  display: flex;
  align-items: center !important;
  /* ✅ 세로 가운데 정렬 - 화살표 라인에 맞춤 */
  justify-content: flex-end !important;
  /* ✅ Rx 경로의 path-label과 동일하게 flex-end */
  padding: 0;
  padding-right: 1.25rem !important;
  /* ✅ Rx 경로의 path-label과 동일한 패딩 (1.25rem) */
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
  /* ✅ 좌우 균형을 맞추기 위해 margin 초기화 */
  margin: 0;
  white-space: nowrap !important;
  /* ✅ 라벨이 줄바꿈되지 않도록 */
  min-width: 120px !important;
  /* ✅ 최소 너비 설정 - Band1/Band2 라벨이 겹치지 않도록 */
  width: 120px !important;
  max-width: 120px !important;
  flex-shrink: 0 !important;
  /* ✅ 축소 방지 */
  font-weight: 500;
  /* ✅ Rx 경로의 path-label과 동일한 폰트 굵기 */
  color: var(--theme-text-secondary);
  /* ✅ Rx 경로의 path-label과 동일한 색상 */
  text-align: right;
  /* ✅ Rx 경로의 path-label과 동일한 텍스트 정렬 */
}

/* 첫 번째 라벨 (RHCP(Tx)) - 상단 화살표와 정렬 */
.rf-switch-labels .path-label:first-child {
  margin-top: 0;
  /* ✅ 상단 마진 제거 */
}

/* 두 번째 라벨 (LHCP(Tx)) - 하단 화살표와 정렬 */
.rf-switch-labels .path-label:last-child {
  margin-bottom: 0;
  /* ✅ 하단 마진 제거 */
}


.rf-switch-content {
  display: flex;
  align-items: center;
  justify-content: center !important;
  /* ✅ Rx 경로의 path-content와 동일하게 center 정렬 - 스위치가 LNA와 같은 수직선에 정렬되도록 */
  gap: 0;
  flex-shrink: 1;
  /* ✅ Rx 경로의 path-content와 동일하게 flex-shrink: 1 사용 */
  flex-grow: 0;
  /* ✅ Rx 경로의 path-content와 동일하게 flex-grow: 0 */
  flex-basis: auto;
  /* ✅ Rx 경로의 path-content와 동일하게 flex-basis: auto */
  width: auto;
  /* ✅ Rx 경로의 path-content와 동일하게 width: auto */
  /* 스위치와 화살표를 붙이기 위해 gap 제거 */
  /* ✅ 좌우 균형을 맞추기 위해 margin 초기화 */
  margin: 0;
  min-width: 0 !important;
  /* ✅ flex item이 축소 가능하도록 */
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
}

.rf-switch-inputs-container {
  display: flex;
  flex-direction: column;
  gap: 0;
  /* ✅ gap 제거 - 각 화살표가 라벨과 정확히 정렬되도록 */
  justify-content: center !important;
  /* ✅ 중앙 정렬로 변경 - 각 화살표가 해당 라벨과 같은 선상에 정렬되도록 */
  align-items: center;
  min-width: 80px;
  width: 80px;
  /* Rx 경로의 화살표와 동일한 너비로 정렬 */
  height: 80px;
  /* ✅ 스위치 아이콘과 동일한 높이로 맞춤 */
  /* RHCP(Tx), LHCP(Tx) 화살표 사이 간격을 라벨과 동일하게 더 좁게 조정 */
  flex-shrink: 0;
  /* ✅ 축소 방지 - 화살표 컨테이너가 고정 너비 유지 */
  /* ✅ margin-right: auto 제거 - 중앙 정렬을 위해 */
}


/* Tx 부분의 입력 화살표 컨테이너 높이 조정 - 두 개가 세로로 배치되므로 각각 30px */
.rf-switch-inputs-container .arrow-container {
  height: 30px;
  min-height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 화살표 길이(너비)는 모든 화살표와 동일하게 80px 유지 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}


.rf-switch-container {
  display: flex;
  flex-direction: column;
  align-items: center !important;
  /* ✅ 세로 정렬 강제 */
  justify-content: center;
  flex-shrink: 0;
  height: 80px;
  /* ✅ 스위치 아이콘과 동일한 높이로 맞춤 */
  /* ✅ 모든 요소(라벨, 화살표, 스위치)를 80px로 통일하여 정렬 문제 해결 */
  /* ✅ 절대 위치 제거 - 화살표, 스위치, 출력이 함께 움직이도록 */
  /* ✅ margin 제거 - 중앙 정렬을 위해 */
}

.rf-switch-output-group {
  display: flex;
  align-items: center !important;
  /* ✅ 세로 정렬 강제 */
  gap: 0.5rem;
  margin-left: 0;
  /* 화살표와 출력 라벨 사이 간격 - RHCP(Rx) 라벨과 화살표 사이 간격과 동일하게 */
  height: 80px;
  /* ✅ 스위치 아이콘과 동일한 높이로 맞춤 */
  flex-shrink: 0;
  /* ✅ 축소 방지 - 출력 그룹이 고정 너비 유지 */
}

.arrow-left {
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}

.arrow-left .arrow-line::after {
  left: -1px;
  right: auto;
  border-left: none;
  border-right: 12px solid var(--theme-text-secondary);
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  /* 스위치 아이콘과 자연스럽게 연결되도록 */
}

.arrow-right {
  /* 모든 화살표의 길이(너비)를 80px로 통일 */
  width: 80px;
  min-width: 80px;
  max-width: 80px;
}

.arrow-container.rf-switch-arrow {
  width: 80px;
  min-width: 80px;
  max-width: 80px;
  /* 출력 화살표는 60px 높이 유지 */
  height: 60px;
  /* Rx 경로의 arrow-container와 동일한 너비(길이) */
}

.path-output-multiline {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: left;
  line-height: 1.2;
  min-width: 180px !important;
  /* ✅ 최소 너비 더 증가 (150px → 180px) - Selection RHCP/LHCP 라벨이 겹치지 않도록 */
  width: auto !important;
  /* ✅ 내용에 맞게 너비 조정 */
  flex-shrink: 0 !important;
  /* ✅ 축소 방지 */
  /* 화살표와 수평 정렬을 위해 수직 중앙 정렬 */
  justify-content: center;
  align-items: flex-start;
  /* 화살표와 수평 정렬을 위해 transform 추가 */
  transform: translateY(2px);
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  padding-right: 1rem !important;
  /* ✅ 오른쪽 패딩 더 증가 (0.75rem → 1rem) */
  padding-left: 0.5rem !important;
  /* ✅ 왼쪽 패딩 추가 - 라벨이 겹치지 않도록 */
}

.lna-icon,
.rf-switch-icon {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ✅ 스위치 아이콘 - 80px 컨테이너에 80px 아이콘이므로 정렬이 자동으로 맞춰짐 */

/* ✅ 스위치 아이콘 SVG 크기 증가 */
.rf-switch-icon svg {
  width: 80px !important;
  height: 80px !important;
  min-width: 80px !important;
  min-height: 80px !important;
  max-width: 80px !important;
  max-height: 80px !important;
}

/* Quasar q-btn 컴포넌트에 직접 스타일 적용 */
.fan-button,
.fan-button.q-btn {
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  user-select: none;
  /* ✅ 버튼 너비 조정 - FEED 테두리 내부에 맞게 */
  min-width: 120px !important;
  max-width: 120px !important;
  width: 120px !important;
  padding: 0.5rem 0.5rem !important;
  /* ✅ 상하 패딩 유지 (0.5rem) */
  /* ✅ 좌우 패딩 감소 (0.75rem → 0.5rem) - 버튼 너비 감소에 맞춤 */
  font-weight: 500;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  /* ✅ FAN 버튼 높이 유지 (50px) */
  height: 50px !important;
  min-height: 50px !important;
  max-height: 50px !important;
  /* .fan-section이 align-items: center로 설정되어 있으므로 버튼이 자동으로 가운데 정렬됨 */
  /* transform 제거하여 자연스러운 가운데 정렬 유지 */
  transform: none !important;
}

.fan-button-text {
  white-space: nowrap;
  font-size: 0.9rem;
}

.fan-icon {
  flex-shrink: 0;
  vertical-align: middle;
}

/* LNA 아이콘 정렬 조정 - 삼각형을 화살표와 정확히 정렬 */
.lna-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
  /* SVG 정렬을 위해 vertical-align 조정 */
  vertical-align: middle;
}

/* SVG 내부 정렬 조정 - 삼각형이 화살표와 정확히 같은 높이에 오도록 */
.lna-icon svg {
  display: block;
  vertical-align: middle;
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
}

.lna-icon:hover,
.rf-switch-icon:hover,
.fan-button:hover {
  opacity: 0.8;
}

/* 로딩 중 비활성화 스타일 */
.lna-icon.lna-disabled,
.rf-switch-icon.lna-disabled {
  cursor: not-allowed !important;
  opacity: 0.5 !important;
  pointer-events: none !important;
}

.lna-icon:hover {
  transform: scale(1.1);
}

.lna-icon:active,
.rf-switch-icon:active {
  transform: scale(0.95) !important;
}

.rf-switch-icon:hover {
  transform: scale(1.05) !important;
}

.fan-button:active,
.fan-button.q-btn:active {
  transform: scale(0.95) !important;
}

.path-output {
  flex: 1;
  font-weight: 500;
  color: var(--theme-text-secondary);
  text-align: left;
  /* 카드 중앙 정렬을 위해 높이와 정렬 명시 */
  display: flex;
  align-items: center;
  /* ✅ 고정 높이 제거, min-height만 사용하여 유연성 확보 */
  /* ✅ 하지만 align-items: center가 제대로 작동하도록 height도 설정 */
  min-height: 60px;
  height: 60px;
  /* 반응형: 작은 화면에서도 비례적으로 줄어들도록 */
  min-width: 80px !important;
  /* ✅ 최소 너비 증가 (라벨이 잘리지 않도록) */
  flex-shrink: 0 !important;
  /* ✅ 축소 방지 */
  /* ✅ 수직 가운데 정렬을 위해 transform 제거 */
  transform: none;
  padding-right: 0.5rem !important;
  /* ✅ 오른쪽 패딩 추가 */
  overflow: visible !important;
  /* ✅ 오른쪽이 잘리지 않도록 */
  /* 화살표와의 간격을 명확히 하기 위해 왼쪽 마진 제거 */
  margin-left: 0 !important;
  padding-left: 0 !important;
}

/* path-content 내부의 path-output에만 간격 적용 */
.path-content>.path-output {
  margin-left: 0.5rem !important;
}

.fan-section {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  /* ✅ 복잡한 패딩 계산 대신 단순하고 명확한 패딩 사용 */
  /* ✅ 상하 패딩을 동일하게 설정하고 flex 정렬에 의존 */
  padding: 1.5rem 1rem;
  /* ✅ 상하 패딩 동일: 1.5rem (24px) */
  height: 115px !important;
  min-height: 115px !important;
  max-height: 115px !important;
  box-sizing: border-box !important;
  margin-top: 0;
  margin-bottom: 0.5rem;
  /* ✅ 1·2행과 동일한 간격 */
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  /* ✅ S-Band 3행과 동일한 테두리 스타일 */
}

.fan-button-container {
  display: flex;
  justify-content: center;
  align-items: center;
  /* 스위치 테두리 기준으로 가운데 수평 정렬 */
  /* ✅ 좌우 균형을 맞추기 위해 margin 초기화 */
  margin: 0;
  width: 100%;
  /* 버튼이 컨테이너 중앙에 위치하도록 */
}


.legend-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.5rem calc(0.65rem + 0.3125rem + 0.3125rem) 0.25rem 0.75rem;
  /* ✅ 우측 패딩 추가 5px 증가: 0.65rem + 0.3125rem → 0.65rem + 0.3125rem + 0.3125rem (추가 5px) */
  /* 범례 항목들을 왼쪽 정렬 */
  align-items: flex-start;
  /* 최소 너비 제한 제거 */
  min-width: 0;
  width: 100%;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  /* 텍스트 줄바꿈 방지 */
  white-space: nowrap;
  /* 최소 너비 제한 제거 */
  min-width: 0;
  width: 100%;
}

.legend-icon {
  flex-shrink: 0;
  width: 32px !important;
  /* ✅ 아이콘 크기 증가: 24px → 32px */
  height: 32px !important;
  /* ✅ 아이콘 크기 증가: 24px → 32px */
}

.legend-text {
  color: var(--theme-text);
  font-size: 0.9rem;
  /* 텍스트 줄바꿈 방지 */
  white-space: nowrap;
  flex-shrink: 0;
  /* 텍스트 오버플로우 처리 */
  overflow: visible;
  text-overflow: clip;
}

/* 상태 메시지 스타일 */
.status-message {
  transition: opacity 0.3s;
  margin-bottom: 0 !important;
  padding-bottom: 0 !important;
}

/* 반응형: 작은 화면에서 요소들이 함께 비례적으로 줄어들도록 */
@media (max-width: 1200px) {
  .arrow-container {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
  }

  .path-label {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
    font-size: 0.9rem;
  }

  .lna-container {
    min-width: 50px;
    width: 60px;
    max-width: 60px;
  }

  .lna-icon svg {
    width: 50px;
    height: 50px;
  }

  .path-output {
    min-width: 80px;
    font-size: 0.9rem;
  }
}

@media (max-width: 960px) {
  .arrow-container {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
  }

  .path-label {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
    font-size: 0.85rem;
  }

  .lna-container {
    min-width: 40px;
    width: 50px;
    max-width: 50px;
  }

  .lna-icon svg {
    width: 40px;
    height: 40px;
  }

  .path-output {
    min-width: 70px;
    font-size: 0.85rem;
  }

  .arrow-line::after {
    right: -1px;
    border-left-width: 10px;
    border-top-width: 5px;
    border-bottom-width: 5px;
  }
}

@media (max-width: 768px) {
  .feed-path {
    padding: calc(2rem + 0.3125rem) 0.75rem calc(0.375rem + 0.3125rem) 0.75rem;
    /* ✅ 높이를 5px씩 증가: padding-top과 padding-bottom에 각각 0.3125rem(5px) 추가 */
  }

  .arrow-container {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
  }

  .path-label {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
    font-size: 0.8rem;
  }

  .lna-container {
    min-width: 30px;
    width: 40px;
    max-width: 40px;
  }

  .lna-icon svg {
    width: 35px;
    height: 35px;
  }

  .path-output {
    min-width: 60px;
    font-size: 0.8rem;
  }

  .arrow-line::after {
    right: -1px;
    border-left-width: 8px;
    border-top-width: 4px;
    border-bottom-width: 4px;
  }

  .current-display-above {
    font-size: 0.75rem;
    padding: 0.2rem 0.4rem;
    min-width: 50px;
  }

  .lna-label {
    font-size: 0.7rem;
  }
}
</style>
