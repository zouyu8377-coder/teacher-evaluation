import api from './index'

export interface EvaluationQuery {
  page?: number
  size?: number
  activityId?: number
  teacherId?: number
}

export const getEvaluationList = (params: EvaluationQuery) => {
  return api.get('/evaluations', { params })
}

export const submitEvaluation = (data: {
  teacherId: number
  activityId: number
  score: number
  comment: string
}) => {
  return api.post('/evaluations', data)
}

export const getMyScores = () => {
  return api.get('/evaluations/teacher/me')
}

export const getTeacherActivityEvaluations = (activityId: number, teacherId: number) => {
  return api.get(`/evaluations/activity/${activityId}/teacher/${teacherId}`)
}

export const getActivityEvaluationSummary = (activityId: number, teacherId?: number) => {
  return api.get(`/evaluations/activity/${activityId}/summary`, { params: { teacherId } })
}

export const publishEvaluationScores = (activityId: number, teacherId?: number) => {
  return api.post('/evaluations/publish', null, { params: { activityId, teacherId } })
}