import api from './index'

export interface EvaluationQuery {
  page?: number
  size?: number
  periodId?: number
  teacherId?: number
}

export const getEvaluationList = (params: EvaluationQuery) => {
  return api.get('/evaluations', { params })
}

export const submitEvaluation = (data: {
  teacherId: number
  periodId: number
  score: number
  comment: string
}) => {
  return api.post('/evaluations', data)
}

export const getMyScores = (periodId?: number) => {
  return api.get('/evaluations/teacher/me', { params: { periodId } })
}