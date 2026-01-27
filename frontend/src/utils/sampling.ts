/**
 * LTTB (Largest-Triangle-Three-Buckets) 다운샘플링 알고리즘
 * Polar 좌표계 [elevation, azimuth] 형식에 최적화
 *
 * @description
 * - 시각적으로 중요한 포인트를 유지하면서 데이터 크기 감소
 * - ECharts polar 좌표계에서 sampling 옵션이 작동하지 않아 수동 구현
 * - Polar → Cartesian 변환 후 삼각형 면적 계산 (정확도 향상)
 */

// ============================================================
// 상수
// ============================================================

/** 기본 목표 샘플 수 */
export const DEFAULT_TARGET_SIZE = 1500

/** 샘플링 적용 임계값 (이 이하면 샘플링 불필요) */
export const SAMPLE_THRESHOLD = 1000

/** 리샘플링 간격 (포인트 수) */
export const RESAMPLE_INTERVAL = 50

// ============================================================
// 유틸리티 함수
// ============================================================

/**
 * Polar 좌표를 Cartesian 좌표로 변환
 * @param elevation - 고도 (0-90도, 반지름으로 사용)
 * @param azimuth - 방위각 (0-360도)
 * @returns [x, y] Cartesian 좌표
 */
function polarToCartesian(elevation: number, azimuth: number): [number, number] {
  const azRad = (azimuth * Math.PI) / 180
  return [elevation * Math.cos(azRad), elevation * Math.sin(azRad)]
}

/**
 * 세 점으로 이루어진 삼각형 면적 계산 (Cartesian 좌표)
 * @param p1 - 첫 번째 점 [x, y]
 * @param p2 - 두 번째 점 [x, y]
 * @param p3 - 세 번째 점 [x, y]
 * @returns 삼각형 면적
 */
function triangleArea(
  p1: [number, number],
  p2: [number, number],
  p3: [number, number]
): number {
  return Math.abs(
    (p1[0] * (p2[1] - p3[1]) + p2[0] * (p3[1] - p1[1]) + p3[0] * (p1[1] - p2[1])) / 2
  )
}

// ============================================================
// LTTB 알고리즘
// ============================================================

/**
 * LTTB 다운샘플링 메인 함수
 *
 * @param data - 원본 데이터 [elevation, azimuth][]
 * @param targetSize - 목표 샘플 수 (기본값: 1500)
 * @returns 다운샘플링된 데이터
 *
 * @example
 * ```ts
 * const rawPath = [[45, 120], [46, 121], ...] // 35,000개
 * const displayPath = lttbDownsample(rawPath, 1500) // 1,500개
 * ```
 */
export function lttbDownsample(
  data: [number, number][],
  targetSize: number = DEFAULT_TARGET_SIZE
): [number, number][] {
  const dataLength = data.length

  // 이미 목표 크기 이하면 그대로 반환 (복사본)
  if (dataLength <= targetSize || targetSize < 3) {
    return [...data]
  }

  const sampled: [number, number][] = []

  // 첫 번째 포인트는 항상 포함
  sampled.push(data[0])

  // 버킷 크기 계산 (첫/마지막 제외)
  const bucketSize = (dataLength - 2) / (targetSize - 2)

  let prevSelectedIndex = 0

  for (let i = 0; i < targetSize - 2; i++) {
    // 현재 버킷 범위
    const bucketStart = Math.floor(i * bucketSize) + 1
    const bucketEnd = Math.min(Math.floor((i + 1) * bucketSize) + 1, dataLength - 1)

    // 다음 버킷의 평균점 계산 (Point C)
    const nextBucketStart = Math.floor((i + 1) * bucketSize) + 1
    const nextBucketEnd = Math.min(Math.floor((i + 2) * bucketSize) + 1, dataLength - 1)

    let avgEl = 0
    let sinSum = 0
    let cosSum = 0
    let avgCount = 0

    for (let j = nextBucketStart; j < nextBucketEnd; j++) {
      avgEl += data[j][0]
      // Azimuth 평균: 각도 wraparound 처리를 위해 sin/cos 사용
      const azRad = (data[j][1] * Math.PI) / 180
      sinSum += Math.sin(azRad)
      cosSum += Math.cos(azRad)
      avgCount++
    }

    let avgAz = 0
    if (avgCount > 0) {
      avgEl /= avgCount
      // 각도 평균: atan2 사용
      avgAz = (Math.atan2(sinSum / avgCount, cosSum / avgCount) * 180) / Math.PI
      if (avgAz < 0) avgAz += 360
    }

    // Polar → Cartesian 변환
    const pointC = polarToCartesian(avgEl, avgAz)

    // 현재 버킷에서 최대 면적 포인트 찾기
    let maxArea = -1
    let maxAreaIndex = bucketStart

    const pointA = polarToCartesian(data[prevSelectedIndex][0], data[prevSelectedIndex][1])

    for (let j = bucketStart; j < bucketEnd; j++) {
      const pointB = polarToCartesian(data[j][0], data[j][1])
      const area = triangleArea(pointA, pointB, pointC)

      if (area > maxArea) {
        maxArea = area
        maxAreaIndex = j
      }
    }

    sampled.push(data[maxAreaIndex])
    prevSelectedIndex = maxAreaIndex
  }

  // 마지막 포인트는 항상 포함
  sampled.push(data[dataLength - 1])

  return sampled
}

// ============================================================
// 증분 LTTB 클래스 (실시간 데이터용)
// ============================================================

/**
 * 증분 LTTB 샘플러
 *
 * @description
 * 실시간으로 포인트가 추가될 때 효율적으로 샘플링
 * - rawPath: 전체 원본 데이터 보관
 * - sampledPath: 렌더링용 다운샘플링된 데이터
 * - 일정 간격(batchSize)마다 전체 재계산
 *
 * @example
 * ```ts
 * const sampler = new IncrementalLTTB(1500, 50)
 * sampler.addPoint([45, 120])
 * sampler.addPoint([46, 121])
 * const displayPath = sampler.getSampledPath()
 * ```
 */
export class IncrementalLTTB {
  private rawPath: [number, number][] = []
  private sampledPath: [number, number][] = []
  private lastSampledLength = 0

  constructor(
    private targetSize: number = DEFAULT_TARGET_SIZE,
    private batchSize: number = RESAMPLE_INTERVAL
  ) {}

  /**
   * 새 포인트 추가
   * @param point - [elevation, azimuth]
   * @returns 현재 샘플링된 경로
   */
  addPoint(point: [number, number]): [number, number][] {
    this.rawPath.push(point)

    const currentLength = this.rawPath.length

    // 리샘플링 조건 체크
    if (currentLength - this.lastSampledLength >= this.batchSize) {
      if (currentLength > SAMPLE_THRESHOLD) {
        // LTTB 샘플링 적용
        const start = performance.now()
        this.sampledPath = lttbDownsample(this.rawPath, this.targetSize)
        const elapsed = performance.now() - start
        // ✅ 디버깅: LTTB 샘플링 성능 로그
        if (currentLength % 500 === 0) {
          console.log(`📊 LTTB: raw=${currentLength} → sampled=${this.sampledPath.length} (${elapsed.toFixed(1)}ms)`)
        }
      } else {
        // 임계값 이하: 전체 표시
        this.sampledPath = [...this.rawPath]
      }
      this.lastSampledLength = currentLength
    } else if (currentLength <= SAMPLE_THRESHOLD) {
      // ✅ 임계값 이하: 새 포인트 추가 (샘플링 불필요)
      this.sampledPath.push(point)
    }
    // ✅ 임계값 초과 + 배치 사이: sampledPath 유지 (다음 배치에서 재계산)

    return this.sampledPath
  }

  /**
   * 전체 초기화
   */
  clear(): void {
    this.rawPath = []
    this.sampledPath = []
    this.lastSampledLength = 0
  }

  /**
   * 초기 데이터로 설정 (복원용)
   * @param data - 초기 데이터
   */
  setInitialData(data: [number, number][]): void {
    this.rawPath = [...data]
    if (data.length > SAMPLE_THRESHOLD) {
      this.sampledPath = lttbDownsample(data, this.targetSize)
    } else {
      this.sampledPath = [...data]
    }
    this.lastSampledLength = data.length
  }

  /**
   * 강제 전체 재계산
   */
  forceResample(): [number, number][] {
    if (this.rawPath.length > SAMPLE_THRESHOLD) {
      this.sampledPath = lttbDownsample(this.rawPath, this.targetSize)
    } else {
      this.sampledPath = [...this.rawPath]
    }
    this.lastSampledLength = this.rawPath.length
    return this.sampledPath
  }

  /**
   * 샘플링된 경로 반환
   */
  getSampledPath(): [number, number][] {
    return this.sampledPath
  }

  /**
   * 원본 경로 반환
   */
  getRawPath(): [number, number][] {
    return this.rawPath
  }

  /**
   * 현재 포인트 수
   */
  get length(): number {
    return this.rawPath.length
  }

  /**
   * 샘플링된 포인트 수
   */
  get sampledLength(): number {
    return this.sampledPath.length
  }
}
