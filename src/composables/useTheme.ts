import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'

export interface ThemeConfig {
  name: string
  displayName: string
  colors: {
    // 기본 Quasar 색상
    primary: string
    secondary: string
    accent: string
    dark: string
    darkPage: string
    positive: string
    negative: string
    info: string
    warning: string

    // 사진 기반 배경 및 표면 색상
    background: string
    surface: string
    surfaceElevated: string
    cardBackground: string

    // 텍스트 색상
    text: string
    textSecondary: string
    textMuted: string

    // 테두리 및 구분선
    border: string
    borderLight: string
    divider: string

    // 그림자
    shadow: string
    shadowLight: string

    // DashboardPage 전용 색상 (사진 분석 기반)
    azimuthColor: string
    elevationColor: string
    tiltColor: string

    // LED 색상 (사진 분석 기반)
    ledNormal: string
    ledError: string
    ledStowActive: string
    ledInactive: string

    // 버튼 색상 (사진 분석 기반)
    buttonPrimary: string
    buttonSecondary: string
    buttonWarning: string
    buttonSuccess: string
    buttonDanger: string

    // 상태 색상
    statusActive: string
    statusInactive: string
    statusWarning: string
    statusError: string

    // 테마 시스템 전용 색상
    borderColor: string
    borderColorEmergency: string
    chartBackground: string
  }

  // 간격 시스템
  spacing: {
    xs: string // 0.25rem (4px)
    sm: string // 0.5rem (8px)
    md: string // 1rem (16px)
    lg: string // 1.5rem (24px)
    xl: string // 2rem (32px)
  }

  // 테두리 시스템
  borders: {
    width: string
    widthThick: string
    radius: string
    radiusSm: string
  }

  // 그림자 시스템
  shadows: {
    sm: string
    md: string
    lg: string
  }

  // ✅ DashboardPage 전용 설정 (선택적 속성으로 변경)
  dashboard?: {
    // 패널 배경색
    panelBackground: string
    panelBackgroundDark: string

    // 테두리 색상
    panelBorder: string
    panelBorderThick: string
    panelBorderEmergency: string

    // 차트 관련
    chartBackground: string
    chartBorder: string

    // LED 크기 및 스타일
    ledSize: string
    ledBorderWidth: string

    // 텍스트 크기
    titleFontSize: string
    labelFontSize: string

    // 간격
    panelGap: string
    contentPadding: string
  }

  // ✅ 새로 추가할 공통 컴포넌트 스타일
  components: {
    // 카드 스타일
    card: {
      background: string
      border: string
      borderRadius: string
      shadow: string
      padding: string
    }

    // 섹션 스타일
    section: {
      background: string
      border: string
      borderRadius: string
      shadow: string
      padding: string
    }
    title: {
      fontSize: string
      fontWeight: string
      color: string
      margin: string
    }

    // 입력 필드 스타일
    input: {
      background: string
      border: string
      textColor: string
      placeholderColor: string
      borderRadius: string
    }

    // 버튼 스타일
    button: {
      primary: {
        background: string
        color: string
        border: string
        borderRadius: string
        padding: string
      }
      secondary: {
        background: string
        color: string
        border: string
        borderRadius: string
        padding: string
      }
    }

    // 컨트롤 섹션 전용
    controlSection: {
      background: string
      border: string
      borderRadius: string
      shadow: string
      padding: string
    }
  }
}

export const THEME_PRESETS: Record<string, ThemeConfig> = {
  // 사진 기반 다크 테마 (정확한 색상 매칭)
  dark: {
    name: 'dark',
    displayName: '다크 테마 (ACS 스타일)',
    colors: {
      // 기본 Quasar 색상
      primary: '#1976D2', // 파란색 (헤더, 버튼)
      secondary: '#26A69A', // 청록색
      accent: '#9C27B0', // 보라색
      dark: '#0F1419', // 매우 어두운 청록색 (주 배경)
      darkPage: '#0A0E13', // 더 어두운 배경
      positive: '#00E676', // 밝은 녹색 (Active, LED)
      negative: '#F44336', // 빨간색 (에러)
      info: '#00BCD4', // 청록색
      warning: '#FFC107', // 노란색 (Standby)

      // 사진 기반 배경 색상 (정확한 매칭)
      background: '#0F1419', // 메인 배경 (어두운 청록색)
      surface: '#1A2B3C', // 카드/패널 배경 (사진의 패널 색상)
      surfaceElevated: '#243447', // 상위 표면
      cardBackground: '#1A2B3C', // 카드 배경

      // 텍스트 색상 (사진 기반)
      text: '#FFFFFF', // 주 텍스트 (흰색)
      textSecondary: '#B0BEC5', // 보조 텍스트 (밝은 회색)
      textMuted: '#78909C', // 비활성 텍스트 (회색)

      // 테두리 및 구분선 (사진의 밝은 회색 라인)
      border: '#37474F', // 카드 테두리 (밝은 회색)
      borderLight: '#455A64', // 밝은 테두리
      divider: '#263238', // 구분선

      // 그림자
      shadow: 'rgba(0, 0, 0, 0.3)',
      shadowLight: 'rgba(0, 0, 0, 0.1)',

      // DashboardPage 전용 색상 (사진 기반)
      azimuthColor: '#FF5722', // 주황색 (Azimuth)
      elevationColor: '#2196F3', // 파란색 (Elevation)
      tiltColor: '#4CAF50', // 녹색 (Tilt)

      // LED 색상 (사진 기반)
      ledNormal: '#00E676', // 밝은 녹색 (활성 상태)
      ledError: '#F44336', // 빨간색 (에러 상태)
      ledStowActive: '#00E676', // 밝은 녹색 (Stow 활성)
      ledInactive: '#78909C', // 회색 (비활성)

      // 버튼 색상 (사진 기반)
      buttonPrimary: '#1976D2', // 진한 파란색
      buttonSecondary: '#455A64', // 어두운 회색
      buttonWarning: '#FFC107', // 노란색
      buttonSuccess: '#00E676', // 밝은 녹색
      buttonDanger: '#F44336', // 빨간색

      // 상태 색상
      statusActive: '#00E676', // 활성 상태
      statusInactive: '#78909C', // 비활성 상태
      statusWarning: '#FFC107', // 경고 상태
      statusError: '#F44336', // 에러 상태

      // 테마 시스템 전용 색상
      borderColor: '#5c6a67', // 통일된 테두리 색상
      borderColorEmergency: '#f44336', // 긴급 테두리 색상
      chartBackground: '#091d24', // 차트 배경 색상
    },

    // ✅ 새로 추가할 components
    components: {
      card: {
        background: '#091d24',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      section: {
        background: '#091d24',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      title: {
        fontSize: '1.25rem',
        fontWeight: '500',
        color: '#1976D2',
        margin: '0 0 0.5rem 0',
      },
      input: {
        background: '#1A2B3C',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        textColor: '#FFFFFF',
        placeholderColor: 'rgba(255, 255, 255, 0.5)',
        borderRadius: '4px',
      },
      button: {
        primary: {
          background: '#1976D2',
          color: '#FFFFFF',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
        secondary: {
          background: 'transparent',
          color: '#1976D2',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
      },
      controlSection: {
        background: '#1A2B3C',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
    },

    // 간격 시스템
    spacing: {
      xs: '0.25rem', // 4px
      sm: '0.5rem', // 8px
      md: '1rem', // 16px
      lg: '1.5rem', // 24px
      xl: '2rem', // 32px
    },

    // 테두리 시스템
    borders: {
      width: '1px',
      widthThick: '3px',
      radius: '8px',
      radiusSm: '4px',
    },

    // 그림자 시스템
    shadows: {
      sm: '0 1px 2px rgba(0, 0, 0, 0.1)',
      md: '0 2px 4px rgba(0, 0, 0, 0.1)',
      lg: '0 4px 8px rgba(0, 0, 0, 0.1)',
    },

    // ✅ DashboardPage 전용 설정 추가
    dashboard: {
      // 패널 배경색
      panelBackground: '#1A2B3C',
      panelBackgroundDark: '#091d24',

      // 테두리 색상
      panelBorder: '#37474F',
      panelBorderThick: '#37474F',
      panelBorderEmergency: '#f44336',

      // 차트 관련
      chartBackground: '#091d24',
      chartBorder: '#455A64',

      // LED 크기 및 스타일
      ledSize: '24px',
      ledBorderWidth: '4px',

      // 텍스트 크기
      titleFontSize: '1rem',
      labelFontSize: '1.3rem',

      // 간격
      panelGap: '1rem',
      contentPadding: '0.5rem',
    },
  },

  // 라이트 테마 (기존 유지)
  light: {
    name: 'light',
    displayName: '라이트 테마',
    colors: {
      primary: '#1976D2',
      secondary: '#26A69A',
      accent: '#9C27B0',
      dark: '#1D1D1D',
      darkPage: '#121212',
      positive: '#21BA45',
      negative: '#C10015',
      info: '#31CCEC',
      warning: '#F2C037',
      background: '#ffffff',
      surface: '#f5f5f5',
      surfaceElevated: '#ffffff',
      cardBackground: '#ffffff',
      text: '#212121',
      textSecondary: 'rgba(0, 0, 0, 0.6)',
      textMuted: 'rgba(0, 0, 0, 0.4)',
      border: '#e0e0e0',
      borderLight: '#f0f0f0',
      divider: '#e0e0e0',
      shadow: 'rgba(0, 0, 0, 0.1)',
      shadowLight: 'rgba(0, 0, 0, 0.05)',
      azimuthColor: '#ff5722',
      elevationColor: '#2196f3',
      tiltColor: '#4caf50',
      ledNormal: '#4caf50',
      ledError: '#f44336',
      ledStowActive: '#4caf50',
      ledInactive: '#666',
      buttonPrimary: '#1976D2',
      buttonSecondary: '#757575',
      buttonWarning: '#F2C037',
      buttonSuccess: '#21BA45',
      buttonDanger: '#C10015',
      statusActive: '#21BA45',
      statusInactive: '#757575',
      statusWarning: '#F2C037',
      statusError: '#C10015',
      borderColor: '#5c6a67',
      borderColorEmergency: '#f44336',
      chartBackground: '#f5f5f5',
    },

    // ✅ 새로 추가할 components
    components: {
      card: {
        background: '#ffffff',
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      section: {
        background: '#ffffff',
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      title: {
        fontSize: '1.25rem',
        fontWeight: '500',
        color: '#1976D2',
        margin: '0 0 0.5rem 0',
      },
      input: {
        background: '#ffffff',
        border: '1px solid #e0e0e0',
        textColor: '#212121',
        placeholderColor: 'rgba(0, 0, 0, 0.5)',
        borderRadius: '4px',
      },
      button: {
        primary: {
          background: '#1976D2',
          color: '#FFFFFF',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
        secondary: {
          background: 'transparent',
          color: '#1976D2',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
      },
      controlSection: {
        background: '#ffffff',
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
    },

    // 간격 시스템
    spacing: {
      xs: '0.25rem',
      sm: '0.5rem',
      md: '1rem',
      lg: '1.5rem',
      xl: '2rem',
    },

    // 테두리 시스템
    borders: {
      width: '1px',
      widthThick: '3px',
      radius: '8px',
      radiusSm: '4px',
    },

    // 그림자 시스템
    shadows: {
      sm: '0 1px 2px rgba(0, 0, 0, 0.1)',
      md: '0 2px 4px rgba(0, 0, 0, 0.1)',
      lg: '0 4px 8px rgba(0, 0, 0, 0.1)',
    },

    // ✅ DashboardPage 전용 설정 추가
    dashboard: {
      // 패널 배경색
      panelBackground: '#ffffff',
      panelBackgroundDark: '#f5f5f5',

      // 테두리 색상
      panelBorder: '#e0e0e0',
      panelBorderThick: '#e0e0e0',
      panelBorderEmergency: '#C10015',

      // 차트 관련
      chartBackground: '#f5f5f5',
      chartBorder: '#e0e0e0',

      // LED 크기 및 스타일
      ledSize: '24px',
      ledBorderWidth: '4px',

      // 텍스트 크기
      titleFontSize: '1rem',
      labelFontSize: '1.3rem',

      // 간격
      panelGap: '1rem',
      contentPadding: '0.5rem',
    },
  },

  // 새로운 테마 추가
  acs: {
    name: 'acs',
    displayName: 'ACS 전용 테마',
    colors: {
      // 기본 Quasar 색상
      primary: '#1976D2', // 파란색 (헤더, 버튼)
      secondary: '#26A69A', // 청록색
      accent: '#9C27B0', // 보라색
      dark: '#0F1419', // 매우 어두운 청록색 (주 배경)
      darkPage: '#0A0E13', // 더 어두운 배경
      positive: '#00E676', // 밝은 녹색 (Active, LED)
      negative: '#F44336', // 빨간색 (에러)
      info: '#00BCD4', // 청록색
      warning: '#FFC107', // 노란색 (Standby)

      // 사진 기반 배경 색상 (정확한 매칭)
      background: '#0F1419', // 메인 배경 (어두운 청록색)
      surface: '#1A2B3C', // 카드/패널 배경 (사진의 패널 색상)
      surfaceElevated: '#243447', // 상위 표면
      cardBackground: '#091d24', // 통일된 카드 배경

      // 텍스트 색상 (사진 기반)
      text: '#FFFFFF', // 주 텍스트 (흰색)
      textSecondary: '#B0BEC5', // 보조 텍스트 (밝은 회색)
      textMuted: '#78909C', // 비활성 텍스트 (회색)

      // 테두리 및 구분선 (사진의 밝은 회색 라인)
      border: '#37474F', // 카드 테두리 (밝은 회색)
      borderLight: '#455A64', // 밝은 테두리
      divider: '#263238', // 구분선

      // 그림자
      shadow: 'rgba(0, 0, 0, 0.3)',
      shadowLight: 'rgba(0, 0, 0, 0.1)',

      // DashboardPage 전용 색상 (사진 기반)
      azimuthColor: '#FF5722', // 주황색 (Azimuth)
      elevationColor: '#2196F3', // 파란색 (Elevation)
      tiltColor: '#4CAF50', // 녹색 (Tilt)

      // LED 색상 (사진 기반)
      ledNormal: '#00E676', // 밝은 녹색 (활성 상태)
      ledError: '#F44336', // 빨간색 (에러 상태)
      ledStowActive: '#00E676', // 밝은 녹색 (Stow 활성)
      ledInactive: '#78909C', // 회색 (비활성)

      // 버튼 색상 (사진 기반)
      buttonPrimary: '#1976D2', // 진한 파란색
      buttonSecondary: '#455A64', // 어두운 회색
      buttonWarning: '#FFC107', // 노란색
      buttonSuccess: '#00E676', // 밝은 녹색
      buttonDanger: '#F44336', // 빨간색

      // 상태 색상
      statusActive: '#00E676', // 활성 상태
      statusInactive: '#78909C', // 비활성 상태
      statusWarning: '#FFC107', // 경고 상태
      statusError: '#F44336', // 에러 상태

      // 테마 시스템 전용 색상
      borderColor: '#5c6a67', // 통일된 테두리 색상
      borderColorEmergency: '#f44336', // 긴급 테두리 색상
      chartBackground: '#091d24', // 차트 배경 색상
    },

    // ✅ 새로 추가할 components
    components: {
      card: {
        background: '#091d24',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      section: {
        background: '#091d24',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
      title: {
        fontSize: '1.25rem',
        fontWeight: '500',
        color: '#1976D2',
        margin: '0 0 0.5rem 0',
      },
      input: {
        background: '#1A2B3C',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        textColor: '#FFFFFF',
        placeholderColor: 'rgba(255, 255, 255, 0.5)',
        borderRadius: '4px',
      },
      button: {
        primary: {
          background: '#1976D2',
          color: '#FFFFFF',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
        secondary: {
          background: 'transparent',
          color: '#1976D2',
          border: '1px solid #1976D2',
          borderRadius: '4px',
          padding: '0.5rem 1rem',
        },
      },
      controlSection: {
        background: '#1A2B3C',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        borderRadius: '8px',
        shadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        padding: '1rem',
      },
    },

    // 간격 시스템
    spacing: {
      xs: '0.25rem', // 4px
      sm: '0.5rem', // 8px
      md: '1rem', // 16px
      lg: '1.5rem', // 24px
      xl: '2rem', // 32px
    },

    // 테두리 시스템
    borders: {
      width: '1px',
      widthThick: '3px',
      radius: '8px',
      radiusSm: '4px',
    },

    // 그림자 시스템
    shadows: {
      sm: '0 1px 2px rgba(0, 0, 0, 0.1)',
      md: '0 2px 4px rgba(0, 0, 0, 0.1)',
      lg: '0 4px 8px rgba(0, 0, 0, 0.1)',
    },

    // ✅ DashboardPage 전용 설정 추가
    dashboard: {
      // 패널 배경색
      panelBackground: '#1A2B3C',
      panelBackgroundDark: '#091d24',

      // 테두리 색상
      panelBorder: '#37474F',
      panelBorderThick: '#37474F',
      panelBorderEmergency: '#f44336',

      // 차트 관련
      chartBackground: '#091d24',
      chartBorder: '#455A64',

      // LED 크기 및 스타일
      ledSize: '24px',
      ledBorderWidth: '4px',

      // 텍스트 크기
      titleFontSize: '1rem',
      labelFontSize: '1.3rem',

      // 간격
      panelGap: '1rem',
      contentPadding: '0.5rem',
    },
  },
}

export function useTheme() {
  const $q = useQuasar()

  // 현재 테마 상태
  const currentTheme = ref<string>('dark')
  const isDarkMode = computed(() => currentTheme.value === 'dark')

  // 테마 설정 가져오기
  const getThemeConfig = (themeName: string): ThemeConfig => {
    return THEME_PRESETS[themeName] || THEME_PRESETS.dark
  }

  // 현재 테마 설정 가져오기
  const themeConfig = computed(() => getThemeConfig(currentTheme.value))

  // CSS 변수 업데이트
  const updateCSSVariables = (config: ThemeConfig) => {
    const root = document.documentElement

    // 모든 색상 변수를 CSS 변수로 설정
    Object.entries(config.colors).forEach(([key, value]) => {
      root.style.setProperty(`--theme-${key}`, value)
    })

    // ✅ 추가: 기존 사용 중인 변수명과 호환성 유지
    root.style.setProperty('--theme-card-background', config.colors.cardBackground)
    root.style.setProperty('--theme-text', config.colors.text)
    root.style.setProperty('--theme-text-secondary', config.colors.textSecondary)

    // ✅ 새로 추가할 components 변수 설정
    if (config.components) {
      // 카드 변수
      Object.entries(config.components.card).forEach(([key, value]) => {
        root.style.setProperty(`--theme-card-${key}`, value)
      })

      // 섹션 변수
      Object.entries(config.components.section).forEach(([key, value]) => {
        root.style.setProperty(`--theme-section-${key}`, value)
      })

      // 제목 변수
      Object.entries(config.components.title).forEach(([key, value]) => {
        root.style.setProperty(`--theme-title-${key}`, value)
      })

      // 입력 필드 변수
      Object.entries(config.components.input).forEach(([key, value]) => {
        root.style.setProperty(`--theme-input-${key}`, value)
      })

      // 버튼 변수
      Object.entries(config.components.button.primary).forEach(([key, value]) => {
        root.style.setProperty(`--theme-button-primary-${key}`, value)
      })
      Object.entries(config.components.button.secondary).forEach(([key, value]) => {
        root.style.setProperty(`--theme-button-secondary-${key}`, value)
      })

      // 컨트롤 섹션 변수
      Object.entries(config.components.controlSection).forEach(([key, value]) => {
        root.style.setProperty(`--theme-control-section-${key}`, value)
      })
    }

    // ✅ DashboardPage 전용 변수 설정 (선택적 속성 체크)
    if (config.dashboard) {
      Object.entries(config.dashboard).forEach(([key, value]) => {
        root.style.setProperty(`--theme-dashboard-${key}`, value)
      })
    }

    // 간격 변수 설정
    Object.entries(config.spacing).forEach(([key, value]) => {
      root.style.setProperty(`--theme-spacing-${key}`, value)
    })

    // 테두리 변수 설정
    Object.entries(config.borders).forEach(([key, value]) => {
      root.style.setProperty(`--theme-border-${key}`, value)
    })

    // 그림자 변수 설정
    Object.entries(config.shadows).forEach(([key, value]) => {
      root.style.setProperty(`--theme-shadow-${key}`, value)
    })

    // Quasar 색상 변수도 업데이트
    root.style.setProperty('--q-primary', config.colors.primary)
    root.style.setProperty('--q-secondary', config.colors.secondary)
    root.style.setProperty('--q-accent', config.colors.accent)
    root.style.setProperty('--q-positive', config.colors.positive)
    root.style.setProperty('--q-negative', config.colors.negative)
    root.style.setProperty('--q-info', config.colors.info)
    root.style.setProperty('--q-warning', config.colors.warning)
    root.style.setProperty('--q-dark', config.colors.dark)
    root.style.setProperty('--q-dark-page', config.colors.darkPage)
  }

  // 테마 변경
  const setTheme = (themeName: string) => {
    if (!THEME_PRESETS[themeName]) {
      console.warn(`테마 '${themeName}'를 찾을 수 없습니다.`)
      return
    }

    currentTheme.value = themeName
    const config = getThemeConfig(themeName)

    // Quasar 다크모드 설정
    $q.dark.set(themeName === 'dark')

    // CSS 변수 업데이트
    updateCSSVariables(config)

    // 로컬 스토리지에 저장
    localStorage.setItem('selectedTheme', themeName)

    console.log(`테마가 '${config.displayName}'로 변경되었습니다.`)
  }

  // 테마 초기화
  const initializeTheme = () => {
    const savedTheme = localStorage.getItem('selectedTheme')
    const themeToUse = savedTheme && THEME_PRESETS[savedTheme] ? savedTheme : 'dark'

    setTheme(themeToUse)
  }

  // 사용 가능한 테마 목록
  const availableThemes = computed(() => {
    return Object.values(THEME_PRESETS).map((theme) => ({
      value: theme.name,
      label: theme.displayName,
    }))
  })

  // 테마 변경 감시
  watch(currentTheme, (newTheme) => {
    console.log(`테마 변경 감지: ${newTheme}`)
  })

  return {
    // 상태
    currentTheme,
    isDarkMode,
    themeConfig,
    availableThemes,

    // 메서드
    setTheme,
    initializeTheme,
    getThemeConfig,
  }
}
