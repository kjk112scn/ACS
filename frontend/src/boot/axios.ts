import axios, { type AxiosInstance } from 'axios'
import { getApiBaseUrl } from '@/utils/api-config'
import { getCurrentLanguage } from '@/texts'

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance
    $api: AxiosInstance
  }
}

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside of the
// "export default () => {}" function below (which runs individually
// for each client)
const api = axios.create({
  baseURL: getApiBaseUrl(),
  // 타임아웃 설정 추가
  timeout: 10000,
  // 요청 헤더 추가
  headers: {
    'Content-Type': 'application/json',
  },
})

// 요청 인터셉터 - Accept-Language 헤더 추가
api.interceptors.request.use(
  (config) => {
    config.headers['Accept-Language'] = getCurrentLanguage()
    return config
  },
  (error: unknown) =>
    Promise.reject(error instanceof Error ? error : new Error('Request interceptor error'))
)

// 응답 인터셉터 - 에러 처리
api.interceptors.response.use(
  (response) => {
    // 성공 응답도 백엔드 에러 상태 체크
    if (response.data?.status === 'error') {
      const error = new Error(response.data.message || 'API 요청 실패')
      console.error('API 에러 응답:', response.data)
      return Promise.reject(error)
    }
    return response
  },
  (error) => {
    // 네트워크 에러 또는 HTTP 에러
    let errorMessage = '네트워크 연결에 문제가 있습니다.'
    let errorDetails = '인터넷 연결을 확인해주세요.'

    if (error.response) {
      // 서버에서 응답을 받았지만 에러 상태
      const status = error.response.status
      const data = error.response.data

      if (data?.status === 'error') {
        errorMessage = data.message || '서버 오류가 발생했습니다.'
        errorDetails = `HTTP ${status}: ${error.response.statusText}`
      } else {
        errorMessage = `서버 오류 (${status})`
        errorDetails = error.response.statusText || '알 수 없는 서버 오류'
      }
    } else if (error.request) {
      // 요청은 보냈지만 응답을 받지 못함
      errorMessage = '서버에 연결할 수 없습니다.'
      errorDetails = '백엔드 서버가 실행 중인지 확인해주세요.'
    } else {
      // 요청 설정 중 오류
      errorMessage = '요청 설정 중 오류가 발생했습니다.'
      errorDetails = error.message
    }

    console.error('API 요청 실패:', {
      url: error.config?.url,
      method: error.config?.method,
      message: errorMessage,
      details: errorDetails,
      error: error,
    })

    // Error 객체로 변환하여 Promise.reject에 전달
    const apiError = new Error(errorMessage)
    return Promise.reject(apiError) // Error 객체로 변환
  },
)

export default ({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$api = api
  // ^ ^ ^ this will allow you to use this.$api (for Vue Options API form)
  //       so you can easily perform requests against your app's API
}

export { api }
