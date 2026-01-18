/**
 * ECharts 테마 색상 캐싱 Composable
 *
 * ECharts는 CSS 변수를 직접 사용할 수 없으므로
 * getComputedStyle로 색상을 가져와 캐싱하여 성능을 최적화합니다.
 *
 * @example
 * ```typescript
 * const { colors, refreshColors } = useChartTheme()
 *
 * const chartOption = computed(() => ({
 *   xAxis: { axisLine: { lineStyle: { color: colors.value.axis } } },
 *   series: [{ lineStyle: { color: colors.value.azimuth } }]
 * }))
 * ```
 *
 * @created 2026-01-18
 */
import { ref, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'

export interface ChartThemeColors {
  // 차트 기본 색상
  line: string
  grid: string
  label: string
  axis: string
  tooltipBg: string

  // 상태 색상
  positive: string
  negative: string
  warning: string
  info: string

  // ACS 전용 색상
  azimuth: string
  elevation: string
  tilt: string

  // 테이블 색상
  tableRowHover: string
  tableRowSelected: string
  tableRowEven: string
  tableHeaderBg: string

  // 텍스트 색상
  text: string
  textSecondary: string
  textMuted: string

  // 배경 색상
  background: string
  surface: string
}

/**
 * ECharts 테마 색상 Composable
 *
 * 다크/라이트 모드 전환 시 자동으로 색상을 업데이트합니다.
 */
export function useChartTheme() {
  const $q = useQuasar()

  const colors = ref<ChartThemeColors>({
    // 차트 기본 색상 (기본값)
    line: '#555',
    grid: '#333',
    label: '#999',
    axis: '#666',
    tooltipBg: 'rgba(0, 0, 0, 0.8)',

    // 상태 색상
    positive: '#4caf50',
    negative: '#f44336',
    warning: '#ff9800',
    info: '#2196f3',

    // ACS 전용 색상
    azimuth: '#ff5722',
    elevation: '#4fc3f7',
    tilt: '#4caf50',

    // 테이블 색상
    tableRowHover: 'rgba(255, 255, 255, 0.05)',
    tableRowSelected: 'rgba(33, 150, 243, 0.2)',
    tableRowEven: 'rgba(255, 255, 255, 0.02)',
    tableHeaderBg: '#1a1a2e',

    // 텍스트 색상
    text: '#ffffff',
    textSecondary: '#b0bec5',
    textMuted: '#78909c',

    // 배경 색상
    background: '#15282f',
    surface: '#091d24',
  })

  /**
   * CSS 변수에서 색상 값을 가져옵니다.
   */
  function getVar(name: string, fallback: string): string {
    const value = getComputedStyle(document.documentElement)
      .getPropertyValue(name)
      .trim()
    return value || fallback
  }

  /**
   * 모든 테마 색상을 새로고침합니다.
   * 다크/라이트 모드 전환 시 호출됩니다.
   */
  function refreshColors(): void {
    colors.value = {
      // 차트 기본 색상
      line: getVar('--theme-chart-line', '#555'),
      grid: getVar('--theme-chart-grid', '#333'),
      label: getVar('--theme-chart-label', '#999'),
      axis: getVar('--theme-chart-axis', '#666'),
      tooltipBg: getVar('--theme-chart-tooltip-bg', 'rgba(0, 0, 0, 0.8)'),

      // 상태 색상
      positive: getVar('--theme-positive', '#4caf50'),
      negative: getVar('--theme-negative', '#f44336'),
      warning: getVar('--theme-warning', '#ff9800'),
      info: getVar('--theme-info', '#2196f3'),

      // ACS 전용 색상
      azimuth: getVar('--theme-azimuth-color', '#ff5722'),
      elevation: getVar('--theme-elevation-color', '#4fc3f7'),
      tilt: getVar('--theme-tilt-color', '#4caf50'),

      // 테이블 색상
      tableRowHover: getVar('--theme-table-row-hover', 'rgba(255, 255, 255, 0.05)'),
      tableRowSelected: getVar('--theme-table-row-selected', 'rgba(33, 150, 243, 0.2)'),
      tableRowEven: getVar('--theme-table-row-even', 'rgba(255, 255, 255, 0.02)'),
      tableHeaderBg: getVar('--theme-table-header-bg', '#1a1a2e'),

      // 텍스트 색상
      text: getVar('--theme-text', '#ffffff'),
      textSecondary: getVar('--theme-text-secondary', '#b0bec5'),
      textMuted: getVar('--theme-text-muted', '#78909c'),

      // 배경 색상
      background: getVar('--theme-background', '#15282f'),
      surface: getVar('--theme-surface', '#091d24'),
    }
  }

  // 다크/라이트 모드 전환 감지
  watch(
    () => $q.dark.isActive,
    () => {
      // DOM 업데이트 후 색상 새로고침 (CSS 변수 적용 대기)
      setTimeout(refreshColors, 0)
    }
  )

  // 컴포넌트 마운트 시 초기 색상 로드
  onMounted(refreshColors)

  return {
    /** 캐싱된 테마 색상 */
    colors,
    /** 색상 수동 새로고침 (테마 변경 후 호출 가능) */
    refreshColors,
  }
}
