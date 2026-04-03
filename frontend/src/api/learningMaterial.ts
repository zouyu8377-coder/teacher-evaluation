import api from './index'

export interface MaterialQuery {
  page?: number
  size?: number
  periodId?: number
}

export const getMaterialList = (params: MaterialQuery) => {
  return api.get('/learning-materials', { params })
}

export const uploadMaterial = (formData: FormData) => {
  return api.post('/learning-materials', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateMaterial = (id: number, data: { title?: string; description?: string }) => {
  return api.put(`/learning-materials/${id}`, data)
}

export const deleteMaterial = (id: number) => {
  return api.delete(`/learning-materials/${id}`)
}

export const downloadMaterial = (id: number) => {
  return api.get(`/learning-materials/${id}/download`, {
    responseType: 'blob'
  })
}