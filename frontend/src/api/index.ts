import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { logger, logRequest, logResponse, logError } from '@/utils/logger'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  responseType: 'json',
  headers: {
    'Accept': 'application/json;charset=UTF-8',
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['Accept'] = 'application/json;charset=UTF-8'
    // 不设置 Content-Type，让浏览器自动处理（特别是 FormData 上传时）
    // 只有当不是 FormData 时才设置 Content-Type
    if (!(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    logRequest(config)
    return config
  },
  error => {
    logger.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    logResponse(response)
    return response.data
  },
  error => {
    logError(error)

    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 403) {
      // 获取当前用户接口403不弹窗，其他接口都弹窗提示
      if (!error.config?.url?.includes('/auth/current')) {
        ElMessage.error('无权限访问该功能，请联系管理员')
      }
      logger.warn('无权限访问该API接口:', error.config?.url)
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (!error.response) {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default api