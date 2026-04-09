import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

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
    config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    return config
  },
  error => Promise.reject(error)
)

api.interceptors.response.use(
  response => {
    // 确保响应数据是UTF-8编码的
    return response.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (error.response?.status === 403) {
      // 不显示弹窗提示，只在控制台输出警告信息
      console.warn('无权限访问该API接口:', error.config?.url)
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    }
    return Promise.reject(error)
  }
)

export default api