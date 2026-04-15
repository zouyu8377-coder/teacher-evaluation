import api from './index'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface UserQuery {
  page?: number
  size?: number
  role?: string
  keyword?: string
}

export const getUserList = (params: UserQuery): Promise<ApiResponse<{ records: any[], total: number }>> => {
  return api.get('/users', { params })
}

export const createUser = (data: {
  username: string
  password: string
  realName: string
  role: string
  department: string
}): Promise<ApiResponse<any>> => {
  return api.post('/users', data)
}

export const getTeachers = (): Promise<ApiResponse<any[]>> => {
  return api.get('/users/teachers')
}

export const getEvaluators = (): Promise<ApiResponse<any[]>> => {
  return api.get('/users/evaluators')
}

export const updateUser = (id: number, data: {
  username?: string
  realName?: string
  role?: string
  department?: string
  status?: number
}): Promise<ApiResponse<any>> => {
  return api.put(`/users/${id}`, data)
}

export const deleteUser = (id: number): Promise<ApiResponse<any>> => {
  return api.delete(`/users/${id}`)
}