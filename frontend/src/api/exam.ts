import api from './index'

export interface ExamQuestion {
  id: number
  periodId: number
  questionText: string
  questionType: 'single' | 'multiple'
  options: { id: string; text: string }[]
  correctAnswer: string
  score: number
  explanation: string
  difficulty: number
  status: boolean
}

export interface ExamPaper {
  id: number
  periodId: number
  name: string
  description: string
  totalScore: number
  durationMinutes: number
  questionCount: number
  status: 'draft' | 'active' | 'closed'
}

export interface ExamRecord {
  id: number
  paperId: number
  activityId: number
  teacherId: number
  answers: Record<string, string>
  score: number
  autoScore: number
  manualAdjust: number
  correctCount: number
  wrongCount: number
  status: 'not_started' | 'in_progress' | 'submitted'
  startedAt: string
  submittedAt: string
}

// ========== 题库相关 ==========

export const getQuestions = (params: {
  periodId: number
  type?: 'single' | 'multiple'
  status?: boolean
  page?: number
  size?: number
}) => {
  return api.get('/exam/questions', { params })
}

export const getQuestionById = (id: number) => {
  return api.get(`/exam/questions/${id}`)
}

export const createQuestion = (data: Partial<ExamQuestion>) => {
  return api.post('/exam/questions', data)
}

export const updateQuestion = (id: number, data: Partial<ExamQuestion>) => {
  return api.put(`/exam/questions/${id}`, data)
}

export const deleteQuestion = (id: number) => {
  return api.delete(`/exam/questions/${id}`)
}

export const importQuestions = (file: File, periodId: number) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('periodId', String(periodId))
  return api.post('/exam/questions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const downloadQuestionTemplate = () => {
  return api.get('/exam/questions/template', { responseType: 'blob' })
}

// ========== 试卷相关 ==========

export const getPapers = (params: { periodId: number; page?: number; size?: number }) => {
  return api.get('/exam/papers', { params })
}

export const getPapersByPeriod = () => {
  return api.get('/exam/papers', { params: { page: 1, size: 100 } })
}

export const getPaperById = (id: number) => {
  return api.get(`/exam/papers/${id}`)
}

export const getPaperQuestions = (id: number) => {
  return api.get(`/exam/papers/${id}/questions`)
}

export const createPaper = (data: Partial<ExamPaper>) => {
  return api.post('/exam/papers', data)
}

export const updatePaper = (id: number, data: Partial<ExamPaper>) => {
  return api.put(`/exam/papers/${id}`, data)
}

export const deletePaper = (id: number) => {
  return api.delete(`/exam/papers/${id}`)
}

export const setPaperQuestions = (id: number, questionIds: number[]) => {
  return api.put(`/exam/papers/${id}/questions`, questionIds)
}

export const generatePaper = (id: number, singleCount: number, multiCount: number) => {
  return api.post(`/exam/papers/${id}/generate`, null, {
    params: { singleCount, multiCount }
  })
}

export const bindPaperToActivity = (paperId: number, activityId: number) => {
  return api.post(`/exam/papers/${paperId}/bind/${activityId}`)
}

// ========== 考试相关 ==========

export const startExam = (activityId: number) => {
  return api.post('/exam/records/start', null, { params: { activityId } })
}

export const getExamRecord = (id: number) => {
  return api.get(`/exam/records/${id}`)
}

export const saveAnswer = (id: number, answers: Record<string, string>) => {
  return api.put(`/exam/records/${id}/answer`, answers)
}

export const submitExam = (id: number) => {
  return api.post(`/exam/records/${id}/submit`)
}

export const getMyExamRecords = () => {
  return api.get('/exam/records/my')
}

export const getExamRecordsByActivity = (activityId: number, page = 1, size = 20) => {
  return api.get(`/exam/records/activity/${activityId}`, { params: { page, size } })
}

export const getExamRecordDetail = (id: number) => {
  return api.get(`/exam/records/${id}/detail`)
}

export const adjustScore = (id: number, adjust: number) => {
  return api.post(`/exam/records/${id}/adjust`, null, { params: { adjust } })
}