import api from './index'
import type { ApiResponse, PageResponse } from './types'

export interface ExamQuestion {
  id: number
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
  finalScore?: number
  startedAt: string
  submittedAt: string
}

// ========== 题库相关 ==========

export const getQuestions = (params: {
  type?: 'single' | 'multiple'
  difficulty?: number
  page?: number
  size?: number
}) => {
  return api.get('/exam/questions', { params }) as Promise<ApiResponse<PageResponse<ExamQuestion>>>
}

export const getQuestionById = (id: number) => {
  return api.get(`/exam/questions/${id}`) as Promise<ApiResponse<ExamQuestion>>
}

export const createQuestion = (data: Partial<ExamQuestion>) => {
  return api.post('/exam/questions', data) as Promise<ApiResponse<ExamQuestion>>
}

export const updateQuestion = (id: number, data: Partial<ExamQuestion>) => {
  return api.put(`/exam/questions/${id}`, data) as Promise<ApiResponse<ExamQuestion>>
}

export const deleteQuestion = (id: number) => {
  return api.delete(`/exam/questions/${id}`) as Promise<ApiResponse<void>>
}

export const importQuestions = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/exam/questions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<ApiResponse<any>>
}

export const downloadQuestionTemplate = () => {
  return api.get('/exam/questions/template', { responseType: 'blob' })
}

// ========== 试卷相关 ==========

export const getPapers = (params: { page?: number; size?: number }) => {
  return api.get('/exam/papers', { params }) as Promise<ApiResponse<PageResponse<ExamPaper>>>
}

export const getPapersByPeriod = () => {
  return api.get('/exam/papers', { params: { page: 1, size: 100 } }) as Promise<ApiResponse<PageResponse<ExamPaper>>>
}

export const getPaperById = (id: number) => {
  return api.get(`/exam/papers/${id}`) as Promise<ApiResponse<ExamPaper>>
}

export const getPaperQuestions = (id: number) => {
  return api.get(`/exam/papers/${id}/questions`) as Promise<ApiResponse<ExamQuestion[]>>
}

export const createPaper = (data: Partial<ExamPaper>) => {
  return api.post('/exam/papers', data) as Promise<ApiResponse<ExamPaper>>
}

export const updatePaper = (id: number, data: Partial<ExamPaper>) => {
  return api.put(`/exam/papers/${id}`, data) as Promise<ApiResponse<ExamPaper>>
}

export const deletePaper = (id: number) => {
  return api.delete(`/exam/papers/${id}`) as Promise<ApiResponse<void>>
}

export const setPaperQuestions = (id: number, questionIds: number[]) => {
  return api.put(`/exam/papers/${id}/questions`, questionIds) as Promise<ApiResponse<void>>
}

export const generatePaper = (id: number, singleCount: number, multiCount: number) => {
  return api.post(`/exam/papers/${id}/generate`, null, {
    params: { singleCount, multiCount }
  }) as Promise<ApiResponse<ExamPaper>>
}

export const bindPaperToActivity = (paperId: number, activityId: number) => {
  return api.post(`/exam/papers/${paperId}/bind/${activityId}`) as Promise<ApiResponse<void>>
}

// ========== 考试相关 ==========

export const startExam = (activityId: number) => {
  return api.post('/exam/records/start', null, { params: { activityId } }) as Promise<ApiResponse<ExamRecord>>
}

export const getExamRecord = (id: number) => {
  return api.get(`/exam/records/${id}`) as Promise<ApiResponse<ExamRecord>>
}

export const saveAnswer = (id: number, answers: Record<string, string>) => {
  return api.put(`/exam/records/${id}/answer`, answers) as Promise<ApiResponse<ExamRecord>>
}

export const submitExam = (id: number) => {
  return api.post(`/exam/records/${id}/submit`) as Promise<ApiResponse<ExamRecord>>
}

export const getMyExamRecords = () => {
  return api.get('/exam/records/my') as Promise<ApiResponse<ExamRecord[]>>
}

export const getExamRecordsByActivity = (activityId: number, page = 1, size = 20) => {
  return api.get(`/exam/records/activity/${activityId}`, { params: { page, size } }) as Promise<ApiResponse<PageResponse<ExamRecord>>>
}

export const getExamRecordDetail = (id: number) => {
  return api.get(`/exam/records/${id}/detail`) as Promise<ApiResponse<any>>
}

export const adjustScore = (id: number, adjust: number) => {
  return api.post(`/exam/records/${id}/adjust`, null, { params: { adjust } }) as Promise<ApiResponse<void>>
}