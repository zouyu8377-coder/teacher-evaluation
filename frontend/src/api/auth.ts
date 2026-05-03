import api from './index'
import type { ApiResponse, UserVO } from './types'

export interface LoginResponse {
  token: string
  user: UserVO
}

export const login = (username: string, password: string) => {
  return api.post('/auth/login', { username, password }) as Promise<ApiResponse<LoginResponse>>
}

export const getCurrentUser = () => {
  return api.get('/auth/current') as Promise<ApiResponse<UserVO>>
}

export const logout = () => {
  return api.post('/auth/logout') as Promise<ApiResponse<void>>
}