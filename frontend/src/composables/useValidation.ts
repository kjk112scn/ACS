import { ref, computed } from 'vue'

export interface ValidationRule {
  required?: boolean
  minLength?: number
  maxLength?: number
  pattern?: RegExp
  email?: boolean
  numeric?: boolean
  min?: number
  max?: number
  custom?: (value: unknown) => string | boolean
}

export interface ValidationResult {
  isValid: boolean
  errors: string[]
}

export const useValidation = () => {
  const validationErrors = ref<Map<string, string[]>>(new Map())

  // 기본 검증 규칙들
  const rules = {
    required: (value: unknown) => {
      if (value === null || value === undefined || value === '') {
        return '필수 입력 항목입니다.'
      }
      return true
    },

    minLength: (value: unknown, min: number) => {
      if (value && typeof value === 'string' && value.length < min) {
        return `최소 ${min}자 이상 입력해주세요.`
      }
      return true
    },

    maxLength: (value: unknown, max: number) => {
      if (value && typeof value === 'string' && value.length > max) {
        return `최대 ${max}자까지 입력 가능합니다.`
      }
      return true
    },

    pattern: (value: unknown, regex: RegExp) => {
      if (value && typeof value === 'string' && !regex.test(value)) {
        return '올바른 형식이 아닙니다.'
      }
      return true
    },

    email: (value: unknown) => {
      if (value && typeof value === 'string' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return '올바른 이메일 형식이 아닙니다.'
      }
      return true
    },

    numeric: (value: unknown) => {
      if (value && isNaN(Number(value))) {
        return '숫자만 입력 가능합니다.'
      }
      return true
    },

    min: (value: unknown, min: number) => {
      if (value && Number(value) < min) {
        return `최소값은 ${min}입니다.`
      }
      return true
    },

    max: (value: unknown, max: number) => {
      if (value && Number(value) > max) {
        return `최대값은 ${max}입니다.`
      }
      return true
    },
  }

  // 단일 필드 검증
  const validateField = (
    fieldName: string,
    value: unknown,
    fieldRules: ValidationRule,
  ): ValidationResult => {
    const errors: string[] = []

    // required 검증
    if (fieldRules.required) {
      const result = rules.required(value)
      if (result !== true) {
        errors.push(result)
      }
    }

    // 값이 있을 때만 다른 검증 수행
    if (value !== null && value !== undefined && value !== '') {
      // minLength 검증
      if (fieldRules.minLength) {
        const result = rules.minLength(value, fieldRules.minLength)
        if (result !== true) {
          errors.push(result)
        }
      }

      // maxLength 검증
      if (fieldRules.maxLength) {
        const result = rules.maxLength(value, fieldRules.maxLength)
        if (result !== true) {
          errors.push(result)
        }
      }

      // pattern 검증
      if (fieldRules.pattern) {
        const result = rules.pattern(value, fieldRules.pattern)
        if (result !== true) {
          errors.push(result)
        }
      }

      // email 검증
      if (fieldRules.email) {
        const result = rules.email(value)
        if (result !== true) {
          errors.push(result)
        }
      }

      // numeric 검증
      if (fieldRules.numeric) {
        const result = rules.numeric(value)
        if (result !== true) {
          errors.push(result)
        }
      }

      // min 검증
      if (fieldRules.min !== undefined) {
        const result = rules.min(value, fieldRules.min)
        if (result !== true) {
          errors.push(result)
        }
      }

      // max 검증
      if (fieldRules.max !== undefined) {
        const result = rules.max(value, fieldRules.max)
        if (result !== true) {
          errors.push(result)
        }
      }

      // custom 검증
      if (fieldRules.custom) {
        const result = fieldRules.custom(value)
        if (typeof result === 'string') {
          errors.push(result)
        }
      }
    }

    // 에러 상태 업데이트
    validationErrors.value.set(fieldName, errors)

    return {
      isValid: errors.length === 0,
      errors,
    }
  }

  // 폼 전체 검증
  const validateForm = (
    formData: Record<string, unknown>,
    formRules: Record<string, ValidationRule>,
  ): ValidationResult => {
    const allErrors: string[] = []
    let isValid = true

    Object.keys(formRules).forEach((fieldName) => {
      const fieldValue = formData[fieldName]
      const fieldRules = formRules[fieldName]
      if (fieldRules) {
        const result = validateField(fieldName, fieldValue, fieldRules)

        if (!result.isValid) {
          isValid = false
          allErrors.push(...result.errors)
        }
      }
    })

    return {
      isValid,
      errors: allErrors,
    }
  }

  // 필드 에러 가져오기
  const getFieldErrors = (fieldName: string) => {
    return validationErrors.value.get(fieldName) || []
  }

  // 필드 에러 제거
  const clearFieldErrors = (fieldName: string) => {
    validationErrors.value.delete(fieldName)
  }

  // 모든 에러 제거
  const clearAllErrors = () => {
    validationErrors.value.clear()
  }

  // 필드가 유효한지 확인
  const isFieldValid = (fieldName: string) => {
    const errors = getFieldErrors(fieldName)
    return errors.length === 0
  }

  // 폼이 유효한지 확인
  const isFormValid = computed(() => {
    return Array.from(validationErrors.value.values()).every((errors) => errors.length === 0)
  })

  return {
    // 상태
    validationErrors: computed(() => validationErrors.value),
    isFormValid,

    // 메서드
    validateField,
    validateForm,
    getFieldErrors,
    clearFieldErrors,
    clearAllErrors,
    isFieldValid,
    rules,
  }
}
