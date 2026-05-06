import api from './index'
import type { ApiResponse, PageResponse, EvaluationVO, EvaluationListVO, EvaluationSummaryVO } from './types'

export interface EvaluationQuery {
  page?: number
  size?: number
  activityId?: number
  teacherId?: number
}

export const getEvaluationList = (params: EvaluationQuery) => {
  return api.get('/evaluations', { params }) as Promise<ApiResponse<PageResponse<EvaluationVO>>>
}

export const submitEvaluation = (data: {
  teacherId: number
  activityId: number
  score: number
  comment: string
}) => {
  return api.post('/evaluations', data) as Promise<ApiResponse<EvaluationVO>>
}

export const getMyScores = () => {
  return api.get('/evaluations/teacher/me') as Promise<ApiResponse<EvaluationVO[]>>
}

export const getTeacherActivityEvaluations = (activityId: number, teacherId: number) => {
  return api.get(`/evaluations/activity/${activityId}/teacher/${teacherId}`) as Promise<ApiResponse<EvaluationListVO>>
}

export const getActivityEvaluationSummary = (activityId: number, teacherId?: number) => {
  return api.get(`/evaluations/activity/${activityId}/summary`, { params: { teacherId } }) as Promise<ApiResponse<EvaluationSummaryVO>>
}

export const publishEvaluationScores = (activityId: number, passingScore: number, teacherId?: number) => {
  return api.post('/evaluations/publish', null, { params: { activityId, passingScore, teacherId } }) as Promise<ApiResponse<number>>
}