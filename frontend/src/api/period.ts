import api from './index'

export const getPeriodList = () => {
  return api.get('/periods')
}

export const getActivePeriod = () => {
  return api.get('/periods/active')
}

export const createPeriod = (data: {
  name: string
  startDate: string
  endDate: string
  description: string
}) => {
  return api.post('/periods', data)
}

export const updatePeriod = (id: number, data: {
  name?: string
  startDate?: string
  endDate?: string
  description?: string
  status?: string
}) => {
  return api.put(`/periods/${id}`, data)
}

export const deletePeriod = (id: number) => {
  return api.delete(`/periods/${id}`)
}