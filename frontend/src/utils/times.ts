/**
 * 날짜/시간 관련 유틸리티 함수들
 */

/**
 * 로컬 시간으로 포맷팅 (밀리초 포함)
 */
export const formatToLocalTime = (dateString: string): string => {
  try {
    const date = new Date(dateString)

    if (isNaN(date.getTime())) {
      return `원시 데이터: ${dateString}`
    }

    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch (error) {
    console.error('로컬 시간 포맷팅 오류:', error)
    return dateString
  }
}

/**
 * 로컬 시간으로 포맷팅 (밀리초 포함)
 */
export const formatToLocalTimeWithMs = (dateString: string): string => {
  try {
    const date = new Date(dateString)

    if (isNaN(date.getTime())) {
      return `원시 데이터: ${dateString}`
    }

    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    const milliseconds = String(date.getMilliseconds()).padStart(3, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${milliseconds}`
  } catch (error) {
    console.error('로컬 시간 포맷팅 오류:', error)
    return dateString
  }
}

/**
 * UTC 시간을 로컬 시간으로 변환
 */
export const formatUTCToLocal = (utcDateString: string): string => {
  try {
    const date = new Date(utcDateString + 'Z')

    return formatToLocalTime(date.toISOString())
  } catch (error) {
    console.error('UTC to Local 변환 오류:', error)
    return utcDateString
  }
}

/**
 * 로컬 시간을 UTC로 변환
 */
export const formatToUTC = (dateString: string): string => {
  try {
    const date = new Date(dateString)

    if (isNaN(date.getTime())) {
      return `원시 데이터: ${dateString}`
    }

    const year = date.getUTCFullYear()
    const month = String(date.getUTCMonth() + 1).padStart(2, '0')
    const day = String(date.getUTCDate()).padStart(2, '0')
    const hours = String(date.getUTCHours()).padStart(2, '0')
    const minutes = String(date.getUTCMinutes()).padStart(2, '0')
    const seconds = String(date.getUTCSeconds()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds} UTC`
  } catch (error) {
    console.error('UTC 시간 포맷팅 오류:', error)
    return dateString
  }
}

/**
 * 시간대 정보와 함께 포맷팅
 */
export const formatWithTimezone = (dateString: string): string => {
  try {
    const date = new Date(dateString)

    return new Intl.DateTimeFormat('ko-KR', {
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      timeZoneName: 'short',
    }).format(date)
  } catch (error) {
    console.error('시간대 포맷팅 오류:', error)
    return dateString
  }
}

/**
 * 남은 시간 계산
 */
export const formatTimeRemaining = (milliseconds: number): string => {
  const absMs = Math.abs(milliseconds)
  const seconds = Math.floor((absMs / 1000) % 60)
  const minutes = Math.floor((absMs / (1000 * 60)) % 60)
  const hours = Math.floor(absMs / (1000 * 60 * 60))

  const formattedTime = [
    hours.toString().padStart(2, '0'),
    minutes.toString().padStart(2, '0'),
    seconds.toString().padStart(2, '0'),
  ].join(':')

  if (milliseconds <= 0) {
    return `진행 중 (+${formattedTime})`
  }
  return `${formattedTime} 후 시작`
}
/**
 * Cal Time을 타임스탬프로 변환하는 함수
 */
export const getCalTimeTimestamp = (calTime: string | null | undefined): number => {
  if (!calTime) return Date.now()

  try {
    // Cal Time이 시간만 있는 경우 (예: "13:00:00.000")
    if (calTime.includes(':') && !calTime.includes('T') && !calTime.includes('-')) {
      // 오늘 날짜와 결합하여 UTC 기준으로 생성 (9시간 추가 없이)
      const today = new Date()
      const year = today.getUTCFullYear()
      const month = String(today.getUTCMonth() + 1).padStart(2, '0')
      const day = String(today.getUTCDate()).padStart(2, '0')

      // ✅ UTC 기준으로 그대로 생성 (9시간 추가 안함)
      const fullDateTime = `${year}-${month}-${day}T${calTime}Z`
      const dateObj = new Date(fullDateTime)

      if (isNaN(dateObj.getTime())) {
        console.warn('Cal Time 파싱 실패:', fullDateTime)
        return Date.now()
      }

      // ✅ UTC 타임스탬프 그대로 반환 (9시간 추가 없이)
      return dateObj.getTime()
    } else {
      // 완전한 날짜 문자열인 경우
      const dateObj = new Date(calTime)
      if (isNaN(dateObj.getTime())) {
        console.warn('유효하지 않은 Cal Time:', calTime)
        return Date.now()
      }
      // ✅ 원본 타임스탬프 그대로 반환
      return dateObj.getTime()
    }
  } catch (e) {
    console.error('Cal Time 변환 오류:', e)
    return Date.now()
  }
}
