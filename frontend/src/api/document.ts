import api from './index'

export interface DocumentQuery {
  page?: number
  size?: number
  periodId?: number
  activityId?: number
  userId?: number
}

export const getDocumentList = (params: DocumentQuery) => {
  return api.get('/documents', { params })
}

export const uploadDocument = (formData: FormData) => {
  return api.post('/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const deleteDocument = (id: number) => {
  return api.delete(`/documents/${id}`)
}

export const downloadDocument = (id: number) => {
  return api.get(`/documents/${id}/download`, {
    responseType: 'blob'
  })
}