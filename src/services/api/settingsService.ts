import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api'

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

class SettingsService {
  // 위치 설정
  async getLocationSettings(): Promise<LocationSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/location`)
    return response.data
  }

  async setLocationSettings(settings: LocationSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/location`, settings)
  }

  // 추적 설정
  async getTrackingSettings(): Promise<TrackingSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/tracking`)
    return response.data
  }

  async setTrackingSettings(settings: TrackingSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/tracking`, settings)
  }

  // Stow 각도 설정
  async getStowAngleSettings(): Promise<StowAngleSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/stow/angle`)
    return response.data
  }

  async setStowAngleSettings(settings: StowAngleSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/stow/angle`, settings)
  }

  // Stow 속도 설정
  async getStowSpeedSettings(): Promise<StowSpeedSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/stow/speed`)
    return response.data
  }

  async setStowSpeedSettings(settings: StowSpeedSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/stow/speed`, settings)
  }

  // Stow 전체 설정
  async getStowAllSettings(): Promise<{ angle: StowAngleSettings; speed: StowSpeedSettings }> {
    const response = await axios.get(`${API_BASE_URL}/settings/stow/all`)
    return response.data
  }

  async setStowAllSettings(
    angleSettings: StowAngleSettings,
    speedSettings: StowSpeedSettings,
  ): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/stow/all`, {
      angleAzimuth: angleSettings.azimuth,
      angleElevation: angleSettings.elevation,
      angleTrain: angleSettings.train,
      speedAzimuth: speedSettings.azimuth,
      speedElevation: speedSettings.elevation,
      speedTrain: speedSettings.train,
    })
  }

  // 안테나 사양 설정
  async getAntennaSpecSettings(): Promise<AntennaSpecSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/antennaspec`)
    return response.data
  }

  async setAntennaSpecSettings(settings: AntennaSpecSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/antennaspec`, settings)
  }

  // 각도 제한 설정
  async getAngleLimitsSettings(): Promise<AngleLimitsSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/anglelimits`)
    return response.data
  }

  async setAngleLimitsSettings(settings: AngleLimitsSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/anglelimits`, settings)
  }

  // 속도 제한 설정
  async getSpeedLimitsSettings(): Promise<SpeedLimitsSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/speedlimits`)
    return response.data
  }

  async setSpeedLimitsSettings(settings: SpeedLimitsSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/speedlimits`, settings)
  }

  // 각도 오프셋 제한 설정
  async getAngleOffsetLimitsSettings(): Promise<AngleOffsetLimitsSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/angleoffsetlimits`)
    return response.data
  }

  async setAngleOffsetLimitsSettings(settings: AngleOffsetLimitsSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/angleoffsetlimits`, settings)
  }

  // 시간 오프셋 제한 설정
  async getTimeOffsetLimitsSettings(): Promise<TimeOffsetLimitsSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/timeoffsetlimits`)
    return response.data
  }

  async setTimeOffsetLimitsSettings(settings: TimeOffsetLimitsSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/timeoffsetlimits`, settings)
  }

  // 알고리즘 설정
  async getAlgorithmSettings(): Promise<AlgorithmSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/algorithm`)
    return response.data
  }

  async setAlgorithmSettings(settings: AlgorithmSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/algorithm`, settings)
  }

  // 스텝 사이즈 제한 설정
  async getStepSizeLimitSettings(): Promise<StepSizeLimitSettings> {
    const response = await axios.get(`${API_BASE_URL}/settings/stepsizelimit`)
    return response.data
  }

  async setStepSizeLimitSettings(settings: StepSizeLimitSettings): Promise<void> {
    await axios.post(`${API_BASE_URL}/settings/stepsizelimit`, settings)
  }

  // 전체 설정 조회
  async getAllSettings(): Promise<Record<string, unknown>> {
    const response = await axios.get(`${API_BASE_URL}/settings`)
    return response.data
  }
}

export const settingsService = new SettingsService()
