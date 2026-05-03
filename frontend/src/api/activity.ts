import api from './index'
import type { ApiResponse, PageResponse, Activity, MyEnrollmentVO, EnrollmentInfoVO, ReviewProgressVO, EnrollmentTeacherVO } from './types'

export const getActivityList = (activeOnly: boolean = false) => {
  return api.get('/activities', { params: { activeOnly } }) as Promise<ApiResponse<Activity[]>>
}

export const getActivitiesByPeriod = (periodId: number) => {
  return api.get('/activities') as Promise<ApiResponse<Activity[]>>
}

export const getActiveActivitiesByPeriod = (periodId: number) => {
  return api.get('/activities/active') as Promise<ApiResponse<Activity[]>>
}

export const getActiveActivities = () => {
  return api.get('/activities/active') as Promise<ApiResponse<Activity[]>>
}

export const getActivityById = (id: number) => {
  return api.get(`/activities/${id}`) as Promise<ApiResponse<Activity>>
}

export const createActivity = (data: Partial<Activity>) => {
  return api.post('/activities', data) as Promise<ApiResponse<Activity>>
}

export const updateActivity = (id: number, data: Partial<Activity>) => {
  return api.put(`/activities/${id}`, data) as Promise<ApiResponse<Activity>>
}

export const deleteActivity = (id: number) => {
  return api.delete(`/activities/${id}`) as Promise<ApiResponse<void>>
}

export const getAvailableActivitiesList = () => {
  return api.get('/activities/available') as Promise<ApiResponse<Activity[]>>
}

export const getAvailableActivitiesForTeacher = (periodId?: number) => {
  return api.get('/activities/teacher/available') as Promise<ApiResponse<Activity[]>>
}

export const canEnrollActivity = (id: number) => {
  return api.get(`/activities/${id}/can-enroll`) as Promise<ApiResponse<boolean>>
}

export const getEnrollmentInfo = (id: number) => {
  return api.get(`/activities/${id}/enrollment-info`) as Promise<ApiResponse<EnrollmentInfoVO>>
}

export const updateReviewerConfig = (id: number, reviewerCount: number, reviewerIds: string) => {
  return api.put(`/activities/${id}/reviewer-config?reviewerCount=${reviewerCount}&reviewerIds=${encodeURIComponent(reviewerIds)}`) as Promise<ApiResponse<Activity>>
}

export const getActivityEnrollments = (activityId: number) => {
  return api.get(`/activities/${activityId}/enrollments`) as Promise<ApiResponse<EnrollmentTeacherVO[]>>
}

export const getReviewProgress = (activityId: number) => {
  return api.get(`/activities/${activityId}/review-progress`) as Promise<ApiResponse<ReviewProgressVO>>
}

export const enrollActivity = (activityId: number) => {
  return api.post(`/activities/${activityId}/enroll`) as Promise<ApiResponse<void>>
}

export const getMyEnrollments = () => {
  return api.get('/activities/my-enrollments') as Promise<ApiResponse<MyEnrollmentVO[]>>
}
