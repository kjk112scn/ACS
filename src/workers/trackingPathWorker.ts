// Worker 메시지 타입 정의
interface WorkerMessage {
  azimuth: number
  elevation: number
  currentPath: [number, number][]
  maxPoints: number
  threshold: number
}

interface WorkerResponse {
  updatedPath: [number, number][]
  processingTime: number
  pointsAdded: number
  totalPoints: number
  error?: string
}

// Worker 메인 로직
self.onmessage = (e: MessageEvent<WorkerMessage>) => {
  const startTime = performance.now()

  try {
    const { azimuth, elevation, currentPath, maxPoints, threshold } = e.data

    // 정규화
    const normalizedAz = azimuth < 0 ? azimuth + 360 : azimuth
    const normalizedEl = Math.max(0, Math.min(90, elevation))
    const newPoint: [number, number] = [normalizedEl, normalizedAz]

    // 경로 업데이트
    const updatedPath = [...currentPath]

    // 중복 체크
    if (updatedPath.length > 0) {
      const lastPoint = updatedPath[updatedPath.length - 1]
      if (lastPoint) {
        const azDiff = Math.abs(lastPoint[1] - normalizedAz)
        const elDiff = Math.abs(lastPoint[0] - normalizedEl)

        if (azDiff < threshold && elDiff < threshold) {
          // 변화가 작으면 추가하지 않음
          const processingTime = performance.now() - startTime
          self.postMessage({
            updatedPath,
            processingTime,
            pointsAdded: 0,
            totalPoints: updatedPath.length,
          })
          return
        }
      }
    }

    // 새 포인트 추가
    updatedPath.push(newPoint)

    // 크기 제한
    if (updatedPath.length > maxPoints) {
      updatedPath.splice(0, updatedPath.length - maxPoints)
    }

    const processingTime = performance.now() - startTime

    self.postMessage({
      updatedPath,
      processingTime,
      pointsAdded: 1,
      totalPoints: updatedPath.length,
    })

  } catch (error) {
    const processingTime = performance.now() - startTime
    self.postMessage({
      updatedPath: [],
      processingTime,
      pointsAdded: 0,
      totalPoints: 0,
      error: error instanceof Error ? error.message : 'Unknown error',
    })
  }
}

// ✅ Worker 환경에서의 타입 확장
declare const self: DedicatedWorkerGlobalScope

// ✅ 타입 export (Worker 내부에서만 사용)
export type { WorkerMessage, WorkerResponse }
