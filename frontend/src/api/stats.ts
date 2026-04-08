import request from './index'

export interface LevelPassedCount {
  C: number
  B2: number
  B1: number
  A2: number
  A1: number
}

export const getLevelPassedCount = () => {
  return request.get<{ code: number; data: LevelPassedCount }>('/api/stats/level-passed')
}

export interface ActivityInfo {
  id: number
  name: string
  level: string
  status: string
  startDate: string
  endDate: string
  description?: string
}

export const getActiveActivities = () => {
  return request.get<{ code: number; data: ActivityInfo[] }>('/api/stats/active-activities')
}