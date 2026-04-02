import api from './index'

export interface UserQuery {
  page?: number
  size?: number
  role?: string
  keyword?: string
}

export const getUserList = (params: UserQuery) => {
  return api.get('/users', { params })
}

export const createUser = (data: {
  username: string
  password: string
  realName: string
  role: string
  department: string
}) => {
  return api.post('/users', data)
}

export const getTeachers = () => {
  return api.get('/users/teachers')
}