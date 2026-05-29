import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 错误码映射表
 */
const ERROR_CODE_MAP = {
  400: '请求参数错误',
  401: '登录状态已过期，请重新登录',
  403: '权限不足，禁止访问',
  404: '请求的资源不存在',
  405: '请求方法不允许',
  408: '请求超时',
  409: '资源冲突',
  422: '请求参数验证失败',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
  // 业务错误码
  1001: '用户名或密码错误',
  1002: '账号已被禁用',
  1003: '账号已被锁定',
  2001: '资源不存在',
  2002: '资源已存在',
  2003: '资源已被删除',
  3001: '操作失败',
  3002: '数据验证失败'
}

/**
 * 获取错误信息
 * @param {number} code - 错误码
 * @param {string} message - 原始错误信息
 * @returns {string} - 格式化后的错误信息
 */
const getErrorMessage = (code, message) => {
  // 优先使用服务器返回的错误信息
  if (message) {
    return message
  }

  // 使用错误码映射表
  if (ERROR_CODE_MAP[code]) {
    return ERROR_CODE_MAP[code]
  }

  // 默认错误信息
  return '操作失败，请稍后重试'
}

/**
 * 显示错误提示（2.5s 内相同文案去重）
 */
let lastErrorToast = { message: '', at: 0 }

const showError = (message, code, type = 'error') => {
  const msg = message || '操作失败，请稍后重试'
  const now = Date.now()
  if (msg === lastErrorToast.message && now - lastErrorToast.at < 2500) {
    return
  }
  lastErrorToast = { message: msg, at: now }
  ElMessage({
    message: msg,
    type,
    duration: 5000,
    showClose: true
  })
}

const shouldSkipErrorToast = config => Boolean(config?.skipErrorToast)

/**
 * 处理401未授权错误
 */
const handleUnauthorized = () => {
  // 清除登录信息
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')

  // 显示提示并跳转
  ElMessage.warning('登录状态已过期，请重新登录')

  // 延迟跳转，让用户看到提示
  setTimeout(() => {
    window.location.replace('/login')
  }, 1000)
}

/**
 * 处理403禁止访问错误
 */
const handleForbidden = () => {
  showError('权限不足，禁止访问', 403)
}

/**
 * 处理404资源不存在错误
 */
const handleNotFound = () => {
  showError('请求的资源不存在', 404)
}

/**
 * 处理500服务器错误
 */
const handleServerError = message => {
  showError(message || '服务器内部错误，请稍后重试', 500)
}

// 创建axios实例
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 30000, // 普通请求30秒超时
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 创建长超时实例（用于AI上传等耗时操作）
const longTimeoutService = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 120000, // AI上传120秒超时
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

const attachAuthHeader = config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
}

/** FormData 上传须去掉 application/json，否则后端报 not a multipart request */
const prepareRequestConfig = config => {
  attachAuthHeader(config)
  if (config.data instanceof FormData) {
    config.headers = config.headers || {}
    delete config.headers['Content-Type']
    delete config.headers['content-type']
  }
  return config
}

longTimeoutService.interceptors.request.use(prepareRequestConfig, error => Promise.reject(error))

// 请求拦截器
service.interceptors.request.use(
  config => {
    prepareRequestConfig(config)

    if (config.method === 'get') {
      // 默认允许浏览器/网关缓存；仅显式 noCache 时加时间戳（减少重复导航的请求量）
      if (config.noCache) {
        config.params = {
          ...config.params,
          _t: Date.now()
        }
      }

      if (config.params?.page && typeof config.params.page === 'number') {
        config.params.page = config.params.page - 1
      }
    }

    return config
  },
  error => {
    console.error('请求错误:', error)

    // 网络错误处理
    if (error.message.includes('Network Error')) {
      showError('网络连接失败，请检查网络设置')
    } else if (error.message.includes('timeout')) {
      showError('请求超时，请稍后重试', 408)
    }

    return Promise.reject(error)
  }
)

const onResponseSuccess = response => {
    const responseType = response.config?.responseType
    if (responseType === 'blob' || responseType === 'arraybuffer' || responseType === 'text') {
      return response.data
    }

    const res = response.data

    // 如果返回的状态码不是200,则判断为错误
    if (res.code !== 200) {
      const errorMessage = getErrorMessage(res.code, res.message)
      const config = response.config

      if (!shouldSkipErrorToast(config)) {
        if (res.code === 401) {
          handleUnauthorized()
        } else {
          showError(errorMessage, res.code)
        }
      }

      return Promise.reject(new Error(errorMessage))
    } else {
      return res
    }
}

const onResponseError = error => {
  console.error('响应错误:', error)
  const config = error.config

  if (error.response) {
    const { status } = error.response
    const errorMessage = getErrorMessage(status, error.response.data?.message)

    if (!shouldSkipErrorToast(config)) {
      if (status === 401) {
        handleUnauthorized()
      } else {
        showError(errorMessage, status)
      }
    }
  } else if (error.request) {
    if (!shouldSkipErrorToast(config)) {
      if (error.message.includes('timeout')) {
        showError('请求超时，请稍后重试', 408)
      } else {
        showError('网络连接失败，请检查网络设置')
      }
    }
  } else if (!shouldSkipErrorToast(config)) {
    showError(error.message || '请求配置错误')
  }

  return Promise.reject(error)
}

// 响应拦截器（普通请求 + AI 长超时请求共用，避免 aiHttp 404 无提示）
service.interceptors.response.use(onResponseSuccess, onResponseError)
longTimeoutService.interceptors.response.use(onResponseSuccess, onResponseError)

/**
 * 封装请求方法，自动处理错误
 * @param {Function} requestFn - 请求函数
 * @param {Object} options - 配置选项
 * @returns {Promise} - 返回请求结果
 */
export const request = async (requestFn, options = {}) => {
  const {
    // 是否显示错误提示
    showError = true,
    // 是否显示加载提示
    showLoading = false,
    // 加载提示文本
    loadingText = '加载中...',
    // 成功回调
    onSuccess = null,
    // 错误回调
    onError = null,
    // 完成回调
    onFinally = null
  } = options

  let loadingInstance = null

  try {
    // 显示加载提示
    if (showLoading) {
      loadingInstance = ElMessage({
        message: loadingText,
        type: 'info',
        duration: 0
      })
    }

    // 执行请求
    const result = await requestFn()

    // 执行成功回调
    if (onSuccess && typeof onSuccess === 'function') {
      onSuccess(result)
    }

    return result
  } catch (error) {
    // 执行错误回调
    if (onError && typeof onError === 'function') {
      onError(error)
    }

    throw error
  } finally {
    // 关闭加载提示
    if (loadingInstance) {
      loadingInstance.close()
    }

    // 执行完成回调
    if (onFinally && typeof onFinally === 'function') {
      onFinally()
    }
  }
}

/**
 * AI / LLM 相关 HTTP 请求（默认 120s 超时；默认由页面内联展示错误，避免与 ElMessage 重复）
 */
export function aiHttp(config) {
  return longTimeoutService({
    skipErrorToast: true,
    ...config
  })
}

export default service
export { longTimeoutService }
