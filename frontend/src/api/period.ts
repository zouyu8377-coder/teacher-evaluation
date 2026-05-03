import api from './index'
import type { ApiResponse, PageResponse } from './types'

export interface Period {
  id: number
  name: string
  startDate: string
  endDate: string
  description: string
  status: string
  createdAt: string
}

export const getPeriodList = () => {
  return api.get('/periods') as Promise<ApiResponse<Period[]>>
}

export const getActivePeriod = () => {
  return api.get('/periods/active') as Promise<ApiResponse<Period>>
}

export const createPeriod = (data: {
  name: string
  startDate: string
  endDate: string
  description: string
}) => {
  return api.post('/periods', data) as Promise<ApiResponse<Period>>
}

export const updatePeriod = (id: number, data: {
  name?: string
  startDate?: string
  endDate?: string
  description?: string
  status?: string
}) => {
  return api.put(`/periods/${id}`, data) as Promise<ApiResponse<Period>>
}

export const deletePeriod = (id: number) => {
  return api.delete(`/periods/${id}`) as Promise<ApiResponse<void>>
}

export const enrollPeriod = (id: number) => {
  return api.post(`/periods/${id}/enroll`) as Promise<ApiResponse<void>>
}

export const getAvailablePeriods = () => {
  return api.get('/periods/available') as Promise<ApiResponse<Period[]>>
}

export const getMyEnrollments = () => {
  return api.get('/periods/my-enrollments') as Promise<ApiResponse<any[]>>
}

export const getPeriodEnrollments = (id: number) => {
  return api.get(`/periods/${id}/enrollments`) as Promise<ApiResponse<any[]>>
}

export const removeEnrollment = (periodId: number, teacherId: number) => {
  return api.delete(`/periods/${periodId}/enrollments/${teacherId}`) as Promise<ApiResponse<void>>
}

export const getEnrolledTeachers = () => {
  return api.get('/periods/enrolled-teachers') as Promise<ApiResponse<any[]>>
}