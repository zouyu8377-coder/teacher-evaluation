import request from './index'

export interface LevelPassedCount {
  C: number
  B2: number
  B1: number
  A2: number
  A1: number
}

export const getLevelPassedCount = () => {
  return request.get('/stats/level-passed') as Promise<{ code: number; data: LevelPassedCount }>
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
  return request.get('/stats/active-activities') as Promise<{ code: number; data: ActivityInfo[] }>
}