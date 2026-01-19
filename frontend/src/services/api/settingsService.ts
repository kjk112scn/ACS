import axios from 'axios'
import { getApiBaseUrl } from '@/utils/api-config'

const API_BASE_URL = getApiBaseUrl()

// 타입 정의들
export interface LocationSettings {
  latitude: number
  longitude: number
  altitude: number
}

export interface TrackingSettings {
  msInterval: number
  durationDays: number
  minElevationAngle: number
  preparationTimeMinutes: number  // 추적 준비 시간 (분) - Train+Az 이동 시간
}

export interface StowAngleSettings {
  azimuth: number
  elevation: number
  train: number
}

export interface StowSpeedSettings {
  azimuth: number
  elevation: number
  train: number
}

export interface AntennaSpecSettings {
  trueNorthOffsetAngle: number
  tiltAngle: number
}

export interface AngleLimitsSettings {
  azimuthMin: number
  azimuthMax: number
  elevationMin: number
  elevationMax: number
  trainMin: number
  trainMax: number
}

export interface SpeedLimitsSettings {
  azimuthMin: number
  azimuthMax: number
  elevationMin: number
  elevationMax: number
  trainMin: number
  trainMax: number
}

export interface AngleOffsetLimitsSettings {
  azimuth: number
  elevation: number
  train: number
}

export interface TimeOffsetLimitsSettings {
  min: number
  max: number
}

export interface AlgorithmSettings {
  geoMinMotion: number
}

export interface StepSizeLimitSettings {
  min: number
  max: number
}

export interface FeedSettings {
  enabledBands: string[]
}

class SettingsService {
  // 위치 설정
  async getLocationSettings(): Promise<LocationSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/location`)
      const data = response.data

      return {
        latitude: data['location.latitude'],
        longitude: data['location.longitude'],
        altitude: data['location.altitude'],
      }
    } catch (error) {
      console.error('위치 설정 조회 실패:', error)
      throw error
    }
  }

  async setLocationSettings(settings: LocationSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/location`, settings)
    } catch (error) {
      console.error('위치 설정 저장 실패:', error)
      throw error
    }
  }

  // 추적 설정
  async getTrackingSettings(): Promise<TrackingSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/tracking`)
      const data = response.data
      return {
        msInterval: data['tracking.msInterval'],
        durationDays: data['tracking.durationDays'],
        minElevationAngle: data['tracking.minElevationAngle'],
        preparationTimeMinutes: data['tracking.preparationTimeMinutes'] ?? 4,
      }
    } catch (error) {
      console.error('추적 설정 조회 실패:', error)
      throw error
    }
  }

  async setTrackingSettings(settings: TrackingSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/tracking`, settings)
    } catch (error) {
      console.error('추적 설정 저장 실패:', error)
      throw error
    }
  }

  // Stow 각도 설정
  async getStowAngleSettings(): Promise<StowAngleSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/stow/angle`)
      const data = response.data
      return {
        azimuth: data['stow.angle.azimuth'],
        elevation: data['stow.angle.elevation'],
        train: data['stow.angle.train'],
      }
    } catch (error) {
      console.error('Stow 각도 설정 조회 실패:', error)
      throw error
    }
  }

  async setStowAngleSettings(settings: StowAngleSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/stow/angle`, settings)
    } catch (error) {
      console.error('Stow 각도 설정 저장 실패:', error)
      throw error
    }
  }

  // Stow 속도 설정
  async getStowSpeedSettings(): Promise<StowSpeedSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/stow/speed`)
      const data = response.data
      return {
        azimuth: data['stow.speed.azimuth'],
        elevation: data['stow.speed.elevation'],
        train: data['stow.speed.train'],
      }
    } catch (error) {
      console.error('Stow 속도 설정 조회 실패:', error)
      throw error
    }
  }

  async setStowSpeedSettings(settings: StowSpeedSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/stow/speed`, settings)
    } catch (error) {
      console.error('Stow 속도 설정 저장 실패:', error)
      throw error
    }
  }

  // Stow 전체 설정
  async getStowAllSettings(): Promise<{ angle: StowAngleSettings; speed: StowSpeedSettings }> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/stow/all`)
      const data = response.data
      return {
        angle: {
          azimuth: data.angle['stow.angle.azimuth'],
          elevation: data.angle['stow.angle.elevation'],
          train: data.angle['stow.angle.train'],
        },
        speed: {
          azimuth: data.speed['stow.speed.azimuth'],
          elevation: data.speed['stow.speed.elevation'],
          train: data.speed['stow.speed.train'],
        },
      }
    } catch (error) {
      console.error('Stow 전체 설정 조회 실패:', error)
      throw error
    }
  }

  async setStowAllSettings(
    angleSettings: StowAngleSettings,
    speedSettings: StowSpeedSettings,
  ): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/stow/all`, {
        angleAzimuth: angleSettings.azimuth,
        angleElevation: angleSettings.elevation,
        angleTrain: angleSettings.train,
        speedAzimuth: speedSettings.azimuth,
        speedElevation: speedSettings.elevation,
        speedTrain: speedSettings.train,
      })
    } catch (error) {
      console.error('Stow 전체 설정 저장 실패:', error)
      throw error
    }
  }

  // 안테나 사양 설정
  async getAntennaSpecSettings(): Promise<AntennaSpecSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/antennaspec`)
      const data = response.data
      return {
        trueNorthOffsetAngle: data['antennaspec.trueNorthOffsetAngle'],
        tiltAngle: data['antennaspec.tiltAngle'],
      }
    } catch (error) {
      console.error('안테나 사양 설정 조회 실패:', error)
      throw error
    }
  }

  async setAntennaSpecSettings(settings: AntennaSpecSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/antennaspec`, settings)
    } catch (error) {
      console.error('안테나 사양 설정 저장 실패:', error)
      throw error
    }
  }

  // 각도 제한 설정
  async getAngleLimitsSettings(): Promise<AngleLimitsSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/anglelimits`)
      const data = response.data
      return {
        azimuthMin: data['anglelimits.azimuthMin'],
        azimuthMax: data['anglelimits.azimuthMax'],
        elevationMin: data['anglelimits.elevationMin'],
        elevationMax: data['anglelimits.elevationMax'],
        trainMin: data['anglelimits.trainMin'],
        trainMax: data['anglelimits.trainMax'],
      }
    } catch (error) {
      console.error('각도 제한 설정 조회 실패:', error)
      throw error
    }
  }

  async setAngleLimitsSettings(settings: AngleLimitsSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/anglelimits`, settings)
    } catch (error) {
      console.error('각도 제한 설정 저장 실패:', error)
      throw error
    }
  }

  // 속도 제한 설정
  async getSpeedLimitsSettings(): Promise<SpeedLimitsSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/speedlimits`)
      const data = response.data
      return {
        azimuthMin: data['speedlimits.azimuthMin'],
        azimuthMax: data['speedlimits.azimuthMax'],
        elevationMin: data['speedlimits.elevationMin'],
        elevationMax: data['speedlimits.elevationMax'],
        trainMin: data['speedlimits.trainMin'],
        trainMax: data['speedlimits.trainMax'],
      }
    } catch (error) {
      console.error('속도 제한 설정 조회 실패:', error)
      throw error
    }
  }

  async setSpeedLimitsSettings(settings: SpeedLimitsSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/speedlimits`, settings)
    } catch (error) {
      console.error('속도 제한 설정 저장 실패:', error)
      throw error
    }
  }

  // 각도 오프셋 제한 설정
  async getAngleOffsetLimitsSettings(): Promise<AngleOffsetLimitsSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/angleoffsetlimits`)
      const data = response.data
      return {
        azimuth: data['angleoffsetlimits.azimuth'],
        elevation: data['angleoffsetlimits.elevation'],
        train: data['angleoffsetlimits.train'],
      }
    } catch (error) {
      console.error('각도 오프셋 제한 설정 조회 실패:', error)
      throw error
    }
  }

  async setAngleOffsetLimitsSettings(settings: AngleOffsetLimitsSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/angleoffsetlimits`, settings)
    } catch (error) {
      console.error('각도 오프셋 제한 설정 저장 실패:', error)
      throw error
    }
  }

  // 시간 오프셋 제한 설정
  async getTimeOffsetLimitsSettings(): Promise<TimeOffsetLimitsSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/timeoffsetlimits`)
      const data = response.data
      return {
        min: data['timeoffsetlimits.min'],
        max: data['timeoffsetlimits.max'],
      }
    } catch (error) {
      console.error('시간 오프셋 제한 설정 조회 실패:', error)
      throw error
    }
  }

  async setTimeOffsetLimitsSettings(settings: TimeOffsetLimitsSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/timeoffsetlimits`, settings)
    } catch (error) {
      console.error('시간 오프셋 제한 설정 저장 실패:', error)
      throw error
    }
  }

  // 알고리즘 설정
  async getAlgorithmSettings(): Promise<AlgorithmSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/algorithm`)
      const data = response.data
      return {
        geoMinMotion: data['algorithm.geoMinMotion'],
      }
    } catch (error) {
      console.error('알고리즘 설정 조회 실패:', error)
      throw error
    }
  }

  async setAlgorithmSettings(settings: AlgorithmSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/algorithm`, settings)
    } catch (error) {
      console.error('알고리즘 설정 저장 실패:', error)
      throw error
    }
  }

  // 스텝 사이즈 제한 설정
  async getStepSizeLimitSettings(): Promise<StepSizeLimitSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/stepsizelimit`)
      const data = response.data
      return {
        min: data['stepsizelimit.min'],
        max: data['stepsizelimit.max'],
      }
    } catch (error) {
      console.error('스텝 사이즈 제한 설정 조회 실패:', error)
      throw error
    }
  }

  async setStepSizeLimitSettings(settings: StepSizeLimitSettings): Promise<void> {
    try {
      await axios.post(`${API_BASE_URL}/settings/stepsizelimit`, settings)
    } catch (error) {
      console.error('스텝 사이즈 제한 설정 저장 실패:', error)
      throw error
    }
  }

  // Feed 설정
  async getFeedSettings(): Promise<FeedSettings> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings/feed`)
      const data = response.data

      // 응답 데이터 구조 확인
      console.log('📡 Feed 설정 API 응답:', data)

      // JSON 문자열을 파싱하여 배열로 변환
      const enabledBandsStr = data['feed.enabledBands'] as string
      let enabledBands: string[] = []

      if (enabledBandsStr) {
        try {
          enabledBands = JSON.parse(enabledBandsStr) as string[]
        } catch (parseError) {
          console.warn('Feed 설정 파싱 실패, 기본값 사용:', parseError)
          enabledBands = ['s', 'x']
        }
      } else {
        // feed.enabledBands가 없으면 기본값 사용
        console.warn('Feed 설정 데이터가 없음, 기본값 사용')
        enabledBands = ['s', 'x']
      }

      return {
        enabledBands,
      }
    } catch (error) {
      // 404 에러는 백엔드 API가 없을 때 발생 (정상적인 경우일 수 있음)
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        console.warn('⚠️ Feed 설정 API 엔드포인트를 찾을 수 없음 (404) - 로컬 스토리지 사용')
      } else {
        console.error('Feed 설정 조회 실패:', error)
      }
      // 에러 발생 시 빈 배열 반환 (로컬 스토리지에서 로드하도록)
      throw error
    }
  }

  async setFeedSettings(settings: FeedSettings): Promise<void> {
    try {
      console.log('📤 Feed 설정 저장 요청:', settings)
      const response = await axios.post(`${API_BASE_URL}/settings/feed`, {
        enabledBands: settings.enabledBands,
      })
      console.log('✅ Feed 설정 저장 성공:', response.data)
    } catch (error) {
      if (axios.isAxiosError(error)) {
        console.error('❌ Feed 설정 저장 실패:', {
          status: error.response?.status,
          statusText: error.response?.statusText,
          data: error.response?.data,
          message: error.message,
        })
      } else {
        console.error('❌ Feed 설정 저장 실패:', error)
      }
      throw error
    }
  }

  // 전체 설정 조회
  async getAllSettings(): Promise<Record<string, unknown>> {
    try {
      const response = await axios.get(`${API_BASE_URL}/settings`)
      return response.data
    } catch (error) {
      console.error('전체 설정 조회 실패:', error)
      throw error
    }
  }
}

export const settingsService = new SettingsService()
