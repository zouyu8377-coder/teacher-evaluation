import api from './index'
import type { ApiResponse, PageResponse, LearningMaterialVO } from './types'

export interface MaterialQuery {
  page?: number
  size?: number
  activityId?: number | null
}

export const getMaterialList = (params: MaterialQuery) => {
  return api.get('/learning-materials', { params }) as Promise<ApiResponse<PageResponse<LearningMaterialVO>>>
}

export const uploadMaterial = (formData: FormData) => {
  return api.post('/learning-materials', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<ApiResponse<LearningMaterialVO>>
}

export const updateMaterial = (id: number, data: { title?: string; description?: string }) => {
  return api.put(`/learning-materials/${id}`, data) as Promise<ApiResponse<LearningMaterialVO>>
}

export const deleteMaterial = (id: number) => {
  return api.delete(`/learning-materials/${id}`) as Promise<ApiResponse<void>>
}

export const downloadMaterial = (id: number) => {
  return api.get(`/learning-materials/${id}/download`, {
    responseType: 'blob'
  })
}
