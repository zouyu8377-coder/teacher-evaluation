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

export const enrollPeriod = (id: number) => {
  return api.post(`/periods/${id}/enroll`)
}

export const getAvailablePeriods = () => {
  return api.get('/periods/available')
}

export const getMyEnrollments = () => {
  return api.get('/periods/my-enrollments')
}

export const getPeriodEnrollments = (id: number) => {
  return api.get(`/periods/${id}/enrollments`)
}

export const removeEnrollment = (periodId: number, teacherId: number) => {
  return api.delete(`/periods/${periodId}/enrollments/${teacherId}`)
}

export const getEnrolledTeachers = () => {
  return api.get('/periods/enrolled-teachers')
}