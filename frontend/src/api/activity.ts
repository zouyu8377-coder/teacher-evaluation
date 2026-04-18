import api from './index'

export interface Activity {
  id: number
  name: string
  level: 'C' | 'B2' | 'B1' | 'A2' | 'A1'
  description: string
  maxParticipants: number
  status: 'draft' | 'active' | 'closed'
  enrollmentStart: string
  enrollmentEnd: string
  startDate: string
  endDate: string
  reviewerCount: number
  reviewerIds: string
  examPaperId: number
  hasExam: boolean
  examDurationMinutes: number
  createdAt: string
}

export const getActivityList = (activeOnly: boolean = false) => {
  return api.get('/activities', { params: { activeOnly } })
}

export const getActivitiesByPeriod = (periodId: number) => {
  return api.get('/activities')
}

export const getActiveActivitiesByPeriod = (periodId: number) => {
  return api.get('/activities/active')
}

export const getActiveActivities = () => {
  return api.get('/activities/active')
}

export const getActivityById = (id: number) => {
  return api.get(`/activities/${id}`)
}

export const createActivity = (data: Partial<Activity>) => {
  return api.post('/activities', data)
}

export const updateActivity = (id: number, data: Partial<Activity>) => {
  return api.put(`/activities/${id}`, data)
}

export const deleteActivity = (id: number) => {
  return api.delete(`/activities/${id}`)
}

export const getAvailableActivitiesList = () => {
  return api.get('/activities/available')
}

export const getAvailableActivitiesForTeacher = (periodId?: number) => {
  return api.get('/activities/teacher/available')
}

export const canEnrollActivity = (id: number) => {
  return api.get(`/activities/${id}/can-enroll`)
}

export const getEnrollmentInfo = (id: number) => {
  return api.get(`/activities/${id}/enrollment-info`)
}

export const updateReviewerConfig = (id: number, reviewerCount: number, reviewerIds: string) => {
  return api.put(`/activities/${id}/reviewer-config?reviewerCount=${reviewerCount}&reviewerIds=${encodeURIComponent(reviewerIds)}`)
}

export const getActivityEnrollments = (activityId: number) => {
  return api.get(`/activities/${activityId}/enrollments`)
}

export const getReviewProgress = (activityId: number) => {
  return api.get(`/activities/${activityId}/review-progress`)
}

export const enrollActivity = (activityId: number) => {
  return api.post(`/activities/${activityId}/enroll`)
}

export const getMyEnrollments = () => {
  return api.get('/activities/my-enrollments')
}