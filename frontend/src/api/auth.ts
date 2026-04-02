import api from './index'

export const login = (username: string, password: string) => {
  return api.post('/auth/login', { username, password })
}

export const getCurrentUser = () => {
  return api.get('/auth/current')
}

export const logout = () => {
  return api.post('/auth/logout')
}