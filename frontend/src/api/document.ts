import api from './index'
import type { ApiResponse, PageResponse, DocumentVO } from './types'

export interface DocumentQuery {
  page?: number
  size?: number
  periodId?: number
  activityId?: number
  userId?: number
}

export const getDocumentList = (params: DocumentQuery) => {
  return api.get('/documents', { params }) as Promise<ApiResponse<PageResponse<DocumentVO>>>
}

export const getTeacherDocuments = (userId: number, activityId?: number) => {
  return api.get('/documents', { params: { userId, activityId, page: 1, size: 10 } }) as Promise<ApiResponse<PageResponse<DocumentVO>>>
}

export const uploadDocument = (formData: FormData) => {
  return api.post('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<ApiResponse<DocumentVO>>
}

export const deleteDocument = (id: number) => {
  return api.delete('/documents/' + id) as Promise<ApiResponse<void>>
}

export const confirmMaterial = (activityId: number) => {
  return api.post('/documents/activity/' + activityId + '/confirm') as Promise<ApiResponse<any>>
}

export const cancelMaterialConfirm = (activityId: number) => {
  return api.post('/documents/activity/' + activityId + '/cancel-confirm') as Promise<ApiResponse<any>>
}

export const downloadDocument = (id: number) => {
  return api.get('/documents/' + id + '/download', {
    responseType: 'blob'
  })
}
