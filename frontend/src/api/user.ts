import api from './index'
import type { ApiResponse, PageResponse, UserVO } from './types'

export interface UserQuery {
  page?: number
  size?: number
  role?: string
  keyword?: string
}

export const getUserList = (params: UserQuery) => {
  return api.get('/users', { params }) as Promise<ApiResponse<PageResponse<UserVO>>>
}

export const createUser = (data: {
  username: string
  password: string
  realName: string
  role: string
  department: string
}) => {
  return api.post('/users', data) as Promise<ApiResponse<UserVO>>
}

export const getTeachers = () => {
  return api.get('/users/teachers') as Promise<ApiResponse<UserVO[]>>
}

export const getEvaluators = () => {
  return api.get('/users/evaluators') as Promise<ApiResponse<UserVO[]>>
}

export const updateUser = (id: number, data: {
  username?: string
  realName?: string
  role?: string
  department?: string
  status?: number
  password?: string
}) => {
  return api.put(`/users/${id}`, data) as Promise<ApiResponse<UserVO>>
}

export const updateTeacherLevel = (id: number, level: string) => {
  return api.put(`/users/${id}/level`, { level }) as Promise<ApiResponse<void>>
}

export const deleteUser = (id: number) => {
  return api.delete(`/users/${id}`) as Promise<ApiResponse<void>>
}